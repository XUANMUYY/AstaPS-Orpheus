#!/usr/bin/env python3
"""从当前资源推导分类，不读账号导出，不生成账号计划。"""
import json, re, hashlib
from collections import defaultdict, Counter
from pathlib import Path
from full_story_dryrun import ROOT, REGIONS, ICONS, read

def classify():
    rows = read(ROOT / 'data/admin-catalog/quests.json')['items']
    text = read(ROOT / 'resources/TextMap/TextMapCHS.json')
    chapters = {x['id']: x for x in read(ROOT / 'resources/ExcelBinOutput/ChapterExcelConfigData.json')}
    chapter_names = {x['id']: x for x in read(ROOT / 'docs/features/data/quests.json')}
    cities = read(ROOT / 'resources/ExcelBinOutput/CityConfigData.json')
    areas = {a: x['cityId'] for x in cities for a in x.get('areaIdVec', [])}
    wa = read(ROOT / 'resources/ExcelBinOutput/WorldAreaConfigData.json')
    scene_regions = defaultdict(set)
    for x in wa:
        if x.get('areaID1') in areas:
            scene_regions[x.get('sceneID')].add(areas[x['areaID1']])
    scene_regions.pop(3, None)
    scenes = {x['id']: x for x in read(ROOT / 'resources/ExcelBinOutput/SceneExcelConfigData.json')}
    dungeon_regions = defaultdict(set)
    for x in read(ROOT / 'resources/ExcelBinOutput/DungeonExcelConfigData.json'):
        if x.get('cityID') in REGIONS:
            dungeon_regions[x.get('sceneId')].add(x['cityID'])
    groups = {}
    for path in (ROOT / 'resources/Scripts/Scene').glob('*/scene*_block*.lua'):
        for gid, area in re.findall(r'\{\s*id\s*=\s*(\d+)[^\n{}]*?\barea\s*=\s*(\d+)', path.read_text(encoding='utf-8-sig')):
            groups[int(gid)] = int(area)
    # 同一坐标只接受一个城市区域；不按 X/Z 阈值或任务 ID 猜国别。
    polygons = []
    world = read(ROOT / 'resources/BinOutput/Scene/WorldArea/scene3_worldArea.json')
    for row in world['OBKNAJCIHEA']:
        a = row['LKBLPFHOAFB']
        area = a['BBLLBIOLMHP']
        poly = a['JLFEAGHGCLP'].get('BFLLDPOAJGD', [])
        if area in areas and poly:
            points = [(p['_x'], p['_z']) for p in poly]
            polygons.append((area, points, min(p[0] for p in points), max(p[0] for p in points), min(p[1] for p in points), max(p[1] for p in points)))

    def position_area(x, z):
        found = set()
        for area, poly, xmin, xmax, zmin, zmax in polygons:
            if not (xmin <= x <= xmax and zmin <= z <= zmax):
                continue
            inside = False
            j = len(poly) - 1
            for i, (xi, zi) in enumerate(poly):
                xj, zj = poly[j]
                if (zi > z) != (zj > z) and x < (xj - xi) * (z - zi) / (zj - zi) + xi:
                    inside = not inside
                j = i
            if inside:
                found.add(area)
        return sorted(found)

    def chapter_id(m):
        return m.get('chapterId') or (m.get('series') if m.get('series') in chapters else 0)

    def region_evidence(m):
        evidence = []
        def area(a, source):
            if a in areas:
                evidence.append({'regionId': areas[a], 'areaId': a, 'source': source})
        def scene(s, source):
            regions = scene_regions.get(s, set()) or dungeon_regions.get(s, set())
            if len(regions) == 1:
                evidence.append({'regionId': next(iter(regions)), 'sceneId': s, 'source': source})
        def pos_string(s, source):
            for match in re.finditer(r'\[(\d+)_\d+:([-\d.]+),[-\d.]+,([-\d.]+)', s):
                sc, x, z = int(match[1]), float(match[2]), float(match[3])
                if sc == 3:
                    found = position_area(x, z)
                    if len({areas[a] for a in found}) == 1:
                        for a in found:
                            area(a, source + ' → scene3_worldArea多边形')
                else:
                    scene(sc, source)
        for s in m.get('subQuests', []):
            guide = s.get('guide') or {}
            scene(guide.get('guideScene'), 'sub.%s.guideScene' % s['subId'])
            for p in guide.get('param', []):
                pos_string(str(p), 'sub.%s.guide.param' % s['subId'])
            for field in ('beginExec', 'finishExec', 'acceptCond', 'finishCond'):
                for c in s.get(field, []):
                    typ, p = c.get('type', ''), c.get('param', [])
                    if any(a in typ for a in ['UNLOCK_AREA', 'ENTER_AREA', 'CURRENT_AREA']) and len(p) >= 2:
                        try: area(int(p[1]), 'sub.%s.%s.%s' % (s['subId'], field, typ))
                        except (ValueError, TypeError): pass
                    for value in p:
                        for g in re.findall(r'(?<!\d)(\d{9})(?!\d)', str(value)):
                            if int(g) in groups:
                                area(groups[int(g)], 'sub.%s.%s.group%s' % (s['subId'], field, g))
        share = ROOT / ('resources/Scripts/Quest/Share/Q%sShareConfig.lua' % m['id'])
        if share.exists():
            raw = share.read_text(encoding='utf-8-sig')
            pos_string(raw, str(share.relative_to(ROOT)))
            for sc in re.findall(r'\bscene_id\s*=\s*(\d+)', raw):
                scene(int(sc), str(share.relative_to(ROOT)) + '.scene_id')
        ch = chapters.get(chapter_id(m), {})
        icon = ch.get('chapterIcon', '').split('_')[-1]
        # AQ 国别图标是章节所属地区；不套用于角色图标猜角色国籍。
        if icon in ICONS:
            evidence.append({'regionId': ICONS[icon], 'source': 'chapter.%s.chapterIcon=%s' % (ch['id'], icon)})
        distinct = sorted({e['regionId'] for e in evidence if e['regionId'] != 99})
        return (distinct[0] if len(distinct) == 1 else 99), distinct, list({json.dumps(e, sort_keys=True): e for e in evidence}.values())

    info = []
    for m in rows:
        title = m.get('titleZh') or text.get(str(m.get('titleTextMapHash'))) or ''
        ch = chapter_id(m)
        aq = 'CHAPTER_STYLE_TYPE_AQ' in chapters.get(ch, {}).values()
        kind = 'AQ' if aq else m.get('type') or ('AQ' if (m.get('luaPath') or '').split('/')[-1].startswith(('AQ', 'MQ')) else '未标注')
        region, distinct, evidence = region_evidence(m)
        exists = (ROOT / ('resources/BinOutput/Quest/%s.json' % m['id'])).is_file()
        hidden = '(test)' in title.lower() or '$HIDDEN' in title
        cp = chapters.get(ch, {})
        coop = 'CHAPTER_STYLE_TYPE_COOP_QUEST' in cp.values()
        names = chapter_names.get(ch, {})
        info.append({'id': m['id'], 'titleZh': title or '（中文主任务标题缺失）', 'kind': kind, 'chapterId': ch,
                     'chapterNum': names.get('chsChapterName') or m.get('chapterNum') or '',
                     'chapterTitleZh': names.get('chsChapterTitle') or m.get('chapterTitleZh') or '',
                     'regionId': region, 'regionZh': REGIONS[region], 'candidateRegions': distinct, 'evidence': evidence,
                     'resourceExists': exists, 'testExcluded': hidden, 'coop': coop,
                     'timedChapter': bool(cp.get('needBeginTimeStr') or cp.get('needEndTimeStr')),
                     'noChapter': not ch, 'series': m.get('series'), 'luaPath': m.get('luaPath'),
                     'areaIds': sorted({e['areaId'] for e in evidence if 'areaId' in e})})
    by_id = {m['id']: m for m in info}
    # 主任务缺场景时，只有整个真实章节已有唯一地区证据才继承；有跨地区证据的章节不推断。
    chapter_regions = defaultdict(set)
    for m in info:
        if m['chapterId'] and not m['testExcluded']:
            chapter_regions[m['chapterId']].update(m['candidateRegions'])
    for m in info:
        known = chapter_regions[m['chapterId']]
        if m['regionId'] == 99 and not m['candidateRegions'] and len(known) == 1:
            region = next(iter(known))
            m.update(regionId=region, regionZh=REGIONS[region], candidateRegions=[region])
            m['evidence'].append({'regionId': region, 'source': '同一真实chapterId=%s中其余主任务只有唯一地区证据' % m['chapterId']})
    return info

def prepare():
    info = classify()
    rules = read(ROOT / 'data/quest-adjust/classification-rules.json')
    rows = {m['id']: m for m in read(ROOT / 'data/admin-catalog/quests.json')['items']}
    sub_parents = {s['subId']:s['mainId'] for s in read(ROOT / 'resources/ExcelBinOutput/QuestExcelConfigData.json')}
    daily_parents = {sub_parents[d['questId']] for d in read(ROOT / 'resources/ExcelBinOutput/DailyTaskExcelConfigData.json') if d.get('questId') in sub_parents}
    activity = read(ROOT / 'resources/ExcelBinOutput/ActivityExcelConfigData.json')
    # 只使用有语义的字段，不拿任意数字碰撞任务ID。
    activity_parents, activity_chapters = set(), set()
    for a in activity:
        for k, v in a.items():
            if k in ('questId', 'questIdList', 'mainQuestId', 'mainQuestIdList'):
                activity_parents.update(v if isinstance(v, list) else [v])
            if k in ('chapterId', 'chapterIdList'):
                activity_chapters.update(v if isinstance(v, list) else [v])
    for m in info:
        m['categories'] = []
        if m['id'] in daily_parents: m['categories'].append('每日委托')
        if m['kind'] == 'AQ': m['categories'].append('魔神任务')
        if m['kind'] == 'WQ': m['categories'].append('世界任务')
        if m['kind'] == 'EQ': m['categories'].append('活动任务')
        if rows[m['id']].get('activityId') and '活动任务' not in m['categories']: m['categories'].append('活动任务')
        if m['coop']: m['categories'].append('邀约任务')
        elif m['kind'] == 'LQ':
            m['categories'].append('传说任务')
            if m['timedChapter'] or rows[m['id']].get('activityId') or m['id'] in activity_parents or m['chapterId'] in activity_chapters:
                m['categories'].append('限时或已下线的传说任务')
        # scene 从原始 guide / ShareConfig / evidence 中取；区域证据复用上一轮算法。
        scenes = {int(s.get('guide', {}).get('guideScene') or 0) for s in rows[m['id']].get('subQuests', [])}
        share = ROOT / ('resources/Scripts/Quest/Share/Q%sShareConfig.lua' % m['id'])
        if share.exists(): scenes.update(map(int, re.findall(r'\bscene_id\s*=\s*(\d+)', share.read_text(encoding='utf-8-sig'))))
        m['expansions'] = [x['name'] for x in rules['expansions'] if set(x['areas']) & set(m['areaIds']) or set(x['scenes']) & scenes]
        if m['kind'] == 'WQ' and m['expansions']: m['categories'].append('扩展区域世界任务')
        m['areaKnown'] = bool(m['areaIds'] or (scenes - {0, 3}))
        if m['id'] in daily_parents: m['categories'] = ['每日委托']
    inputs = ['data/admin-catalog/quests.json', 'data/quest-adjust/classification-rules.json',
              'resources/ExcelBinOutput/ChapterExcelConfigData.json', 'resources/ExcelBinOutput/CityConfigData.json',
              'resources/ExcelBinOutput/WorldAreaConfigData.json', 'resources/ExcelBinOutput/ActivityExcelConfigData.json',
              'resources/ExcelBinOutput/QuestExcelConfigData.json', 'resources/ExcelBinOutput/MainQuestExcelConfigData.json',
              'resources/ExcelBinOutput/DailyTaskExcelConfigData.json',
              'resources/ExcelBinOutput/SceneExcelConfigData.json', 'resources/ExcelBinOutput/DungeonExcelConfigData.json',
              'resources/BinOutput/Scene/WorldArea/scene3_worldArea.json', 'resources/TextMap/TextMapCHS.json',
              'docs/features/data/quests.json', 'tools/quest-analysis/category_dryrun.py']
    for pattern in ('resources/BinOutput/Quest/*.json', 'resources/Scripts/Quest/Share/*ShareConfig.lua', 'resources/Scripts/Scene/*/scene*_block*.lua'):
        inputs += [p.relative_to(ROOT).as_posix() for p in sorted(ROOT.glob(pattern))]
    data = {'rules': rules, 'parents': info, 'inputsSha256': {p: hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in inputs}}
    (ROOT / 'data/quest-adjust/classification.json').write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    out = ROOT / 'build/quest-adjust/category-input'
    out.mkdir(parents=True, exist_ok=True)
    segments = []
    choices = [('events', '活动任务', None), ('timed-legend', '限时或已下线的传说任务', None), ('hangout', '邀约任务', None)]
    choices += [('expansion-%02d' % i, '扩展区域世界任务', x['name']) for i, x in enumerate(rules['expansions'])]
    for key, category, expansion in choices:
        selector = {'categories': [category]}
        if expansion: selector['expansions'] = [expansion]
        goal = {'version': 2, 'source': {'description': '空白新号类别静态试算；不绑定账号、不执行'}, 'include': [selector]}
        file = out / (key+'.json'); file.write_text(json.dumps(goal, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
        segments.append({'key': key, 'label': expansion or category, 'input': 'new', 'goalFile': str(file.relative_to(ROOT))})
    (out/'manifest.json').write_text(json.dumps({'segments': segments},ensure_ascii=False,indent=2),encoding='utf-8')
    print('分类主任务',len(info),'类别',Counter(c for m in info for c in m['categories']))

def summarize(plans='build/quest-adjust/category-plans-delivery'):
    out = ROOT / plans
    lines = ['# 类别离线试算（空白新号，未执行）', '', '所有输入均为 new / UID 0 占位；不读取数据库或账号导出。步数、邮件仅为可解析可排序部分；每步按 1.3 秒粗估，不含备份、Lua等待、发信、重登。各行独立，前置可能重复，不能相加当成同一账号计划。', '', '| 类别 / 区域 | 选中主任务 | 步数 | 阻断（按类型） | 邮件 | 估时秒 | 能否跳 |', '|---|---:|---:|---|---:|---:|---|']
    summary=[]
    for s in read(ROOT/'build/quest-adjust/category-input/manifest.json')['segments']:
        d=read(out/(s['key']+'.json')); p=d.get('plan') or {}; issues=list({(i['subId'],i['type'],i['reason']):i for i in d['resolutionIssues']+p.get('blockers',[])}.values())
        counts=Counter(i['type'] for i in issues)
        summary.append({'label':s['label'],'parents':len(d['resolvedGoal']['parentIds']),'steps':len(p.get('steps',[])),'blockers':dict(counts),'mail':len(p.get('expectedMail',[])),'seconds':d['estimatedSeconds']})
        lines.append('| %s | %s | %s | %s | %s | %.1f | %s |' % (s['label'],len(d['resolvedGoal']['parentIds']),len(p.get('steps',[])),'；'.join('%s×%s'%x for x in sorted(counts.items())) or '0',len(p.get('expectedMail',[])),d['estimatedSeconds'],'阻断，不能跳' if issues else '仅静态无阻断，待运行验收'))
    lines += ['', '详细卡点、主任务结算分支候选和未覆盖目标见 %s/*.json。目标解析失败不会自动选择结算分支；未知 exec、未补偿持久条件与顺带接取 beginExec 保持阻断。' % plans, '', '分类规则及资源依据见 data/quest-adjust/classification-rules.json、classification.json；限时类别表示资源中的时间/活动关联，无法据此证明当前官方已下线。当前没有符合该规则的 LQ 条目，不能把零目标算成可跳。无区域证据的世界任务在排除扩展区域时保守省略。']
    (ROOT/'build/quest-adjust/category-dryrun-summary.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    (ROOT/'build/quest-adjust/category-dryrun-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')

if __name__ == '__main__':
    import argparse
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--summarize',action='store_true')
    parser.add_argument('--plans',default='build/quest-adjust/category-plans-delivery')
    args=parser.parse_args()
    summarize(args.plans) if args.summarize else prepare()
