#!/usr/bin/env python3
"""生成 data/quest-adjust/instanced-scenes.json：副本场景清单，及完成条件事件的来源场景。

副本场景每次进入都重建，组状态不跨次保留；完成条件的事件来源若全部落在副本里，
剧情跳过时不加载该组也不会丢失持久世界状态。来源查不到的条件不写入，规划仍阻断。
只读资源，不访问数据库。
"""
import glob
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'resources'
OUT = ROOT / 'data/quest-adjust/instanced-scenes.json'
# 来源以「机关 id / 组 id / Lua 通知键」索引，与条件参数对应。
GADGET_TYPES = ('QUEST_CONTENT_DESTROY_GADGET', 'QUEST_CONTENT_INTERACT_GADGET')
GROUP_TYPES = ('QUEST_CONTENT_CLEAR_GROUP_MONSTER',)
LUA_TYPES = ('QUEST_CONTENT_LUA_NOTIFY',)


def quest_gadget_sources(text):
    """读取任务 Share 配置中 gadgets 表的字面量 id/scene_id；缺失场景保持未知。"""
    # 只做静态来源识别，不执行任务 Lua；字符串和注释里的花括号不参与配对。
    text = re.sub(r'--\[\[.*?\]\]|--[^\n]*', '', text, flags=re.S)
    tokens = list(re.finditer(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[{}]', text))
    pairs, stack = {}, []
    for token in tokens:
        if token.group() == '{': stack.append(token.start())
        elif token.group() == '}' and stack: pairs[stack.pop()] = token.start()
    sources, unknown = defaultdict(set), set()
    for match in re.finditer(r'\bgadgets\s*=\s*\{', text):
        start = match.end() - 1
        if start not in pairs: continue
        limit = pairs[start]; cursor = start + 1
        while cursor < limit:
            child = text.find('{', cursor, limit)
            if child < 0 or child not in pairs: break
            end = pairs[child]; block = text[child + 1:end]
            gid = re.search(r'\bid\s*=\s*(\d+)(?=\s*(?:,|$))', block)
            scene = re.search(r'\bscene_id\s*=\s*(\d+)(?=\s*(?:,|$))', block)
            if gid:
                if scene: sources[gid.group(1)].add(int(scene.group(1)))
                else: unknown.add(gid.group(1))
            cursor = end + 1
    return sources, unknown


def main():
    scenes = json.load(open(RES / 'ExcelBinOutput/SceneExcelConfigData.json', encoding='utf-8'))
    dungeons = sorted(s['id'] for s in scenes if s.get('type') == 'SCENE_DUNGEON')
    wanted = defaultdict(set)
    for f in sorted((RES / 'BinOutput/Quest').glob('*.json')):
        data = json.load(open(f, encoding='utf-8'))
        for sub in data.get('subQuests', []):
            for c in sub.get('finishCond', []):
                t, p = c.get('type'), c.get('param', [])
                if t in GADGET_TYPES + GROUP_TYPES and p and p[0]: wanted[t].add(str(p[0]))
                elif t in LUA_TYPES and c.get('param_str'): wanted[t].add(c['param_str'])
    gadgets, groups, notifies = defaultdict(set), {}, defaultdict(set)
    gadget_ids = wanted['QUEST_CONTENT_DESTROY_GADGET'] | wanted['QUEST_CONTENT_INTERACT_GADGET']
    notify_re = re.compile(r'AddQuestProgress\s*\(\s*context\s*,\s*"([^"]+)"')
    gadget_re = re.compile(r'gadget_id\s*=\s*(\d+)')
    for f in glob.glob(str(RES / 'Scripts/Scene/*/scene*_group*.lua')):
        path = Path(f); scene = int(path.parent.name)
        m = re.match(r'scene\d+_group(\d+)\.lua$', path.name)
        if not m: continue
        groups[m.group(1)] = scene
        text = path.read_text(encoding='utf-8', errors='ignore')
        for g in gadget_re.findall(text):
            if g in gadget_ids: gadgets[g].add(scene)
        for key in notify_re.findall(text): notifies[key].add(scene)
    # 任务生成的机关不在 scene group 表中；Share 配置显式记录其场景，不能从任务编号猜场景。
    unknown_gadgets = set()
    for path in sorted((RES / 'Scripts/Quest/Share').glob('*.lua')):
        quest_sources, unknown = quest_gadget_sources(path.read_text(encoding='utf-8-sig'))
        unknown_gadgets.update(unknown)
        for gid, scenes in quest_sources.items():
            if gid in gadget_ids: gadgets[gid].update(scenes)
    sources = {}
    for t in GADGET_TYPES:
        for k in wanted[t]:
            if gadgets.get(k) and k not in unknown_gadgets: sources[t + ':' + k] = sorted(gadgets[k])
    for t in GROUP_TYPES:
        for k in wanted[t]:
            if k in groups: sources[t + ':' + k] = [groups[k]]
    for t in LUA_TYPES:
        for k in wanted[t]:
            if notifies.get(k): sources[t + ':' + k] = sorted(notifies[k])
    dungeon_set = set(dungeons)
    instanced = {k: v for k, v in sorted(sources.items()) if all(s in dungeon_set for s in v)}
    out = {
        'note': '由 tools/quest-analysis/instanced_scenes.py 从资源生成，勿手改。instancedScenes 为 SCENE_DUNGEON；'
                'condSources 只列来源全部位于副本场景的完成条件（键为 类型:参数），其余条件不在此列、规划照旧阻断。',
        'instancedScenes': dungeons,
        'condSources': instanced,
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(f'副本场景 {len(dungeons)}；有来源的条件 {len(sources)}，其中全在副本 {len(instanced)}')


if __name__ == '__main__':
    main()
