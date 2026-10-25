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
    sources = {}
    for t in GADGET_TYPES:
        for k in wanted[t]:
            if gadgets.get(k): sources[t + ':' + k] = sorted(gadgets[k])
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
