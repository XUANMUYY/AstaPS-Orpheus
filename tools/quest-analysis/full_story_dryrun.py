#!/usr/bin/env python3
"""C5 文件准备与报告。只读取资源/导出文件；Java 批量入口负责真实规划，脚本不调用数据库或重放。"""
import argparse
from collections import Counter, defaultdict
from datetime import datetime
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/quest-adjust'
REGIONS = {1: '蒙德', 2: '璃月', 3: '稻妻', 4: '须弥', 5: '枫丹', 6: '纳塔', 7: '挪德卡莱', 8: '至冬', 9: '至冬特殊场景',
           99: '未归类', 100: '海岛（1.6）', 101: '三界路飨祭', 102: '海岛（2.8）', 103: '琉形蜃境', 104: '希穆兰卡', 105: '空之域'}
ICONS = {'Mengde': 1, 'Liyue': 2, 'Inazuma': 3, 'Sumeru': 4, 'Fontaine': 5, 'Natlan': 6, 'NodKrai': 7, 'Snezhnaya': 8}


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def write(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write('\n')


def prepare(export, run, replace_goal=False):
    base = OUT / run
    base.mkdir(exist_ok=False)
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
    segments = []
    def goal(key, ids, source=None):
        data = {'version': 1, 'source': source or {'description': 'C5离线分段试算；不执行', 'date': '2026-10-25'}, 'completeParents': sorted(ids)}
        path = base / 'goals' / (key + '.json')
        write(path, data)
        return str(path.relative_to(ROOT))
    def add(key, ids, baseline=None, save=None, label='', input='new', goalfile=None):
        s = {'key': key, 'label': label, 'input': input, 'goalFile': goalfile or goal(key, ids), 'parentIds': sorted(ids)}
        if baseline: s['baseline'] = baseline
        if save: s['saveAs'] = save
        segments.append(s)
    aq_ids = []
    ordered = sorted((x for x in chapters.values() if 'CHAPTER_STYLE_TYPE_AQ' in x.values()), key=lambda x: (x.get('tag', x['id']), x['id']))
    for i, ch in enumerate(ordered):
        ids = [x['id'] for x in info if x['chapterId'] == ch['id'] and not x['testExcluded']]
        aq_ids.extend(ids)
        name = chapter_names.get(ch['id'], {})
        add('aq-%s' % ch['id'], ids, baseline='aq' if i else None, save='aq',
            label='%s %s %s' % (name.get('chsChapterImageTitle', '未归类'), name.get('chsChapterName', str(ch['id'])), name.get('chsChapterTitle', '')))
    unassigned_aq = [x['id'] for x in info if x['kind'] == 'AQ' and not x['testExcluded'] and x['id'] not in aq_ids]
    if unassigned_aq:
        add('aq-unassigned', unassigned_aq, baseline='aq', label='未归类魔神（无有效章节归属）')
    for kind in ('LQ', 'WQ'):
        for region in sorted({x['regionId'] for x in info if x['kind'] == kind and not x['testExcluded']}):
            ids = [x['id'] for x in info if x['kind'] == kind and x['regionId'] == region and not x['testExcluded']]
            add('%s-%s' % (kind.lower(), region), ids, baseline='aq', label='%s %s' % (REGIONS[region], kind))
    # 口述默认：不包含晚出间章1004；含1307；覆盖按地区可唯一判定的普通 LQ/WQ。分支/缺失仍阻断。
    personal_chapters = [x['id'] for x in ordered if x.get('tag', 99999) <= chapters[1308]['tag']]
    personal_ids = sorted({x['id'] for x in info if not x['testExcluded'] and (
        x['chapterId'] in personal_chapters or (x['kind'] in ('LQ', 'WQ') and x['regionId'] in (1, 2, 3, 4) and not x['coop']))})
    source = {'description': '主会话转述用户口述，2026-10-25，待用户确认', 'date': '2026-10-25',
              'interpretation': '序章至第三章第六幕及此前间章；四地区唯一可归类的LQ/WQ；邀约、每日、EQ、跨地区与未归类暂不选；扩展区域按CityConfigData归属纳入。并非最终用户授权。',
              'chapterIds': personal_chapters, 'chapterNamesZh': {str(c): chapter_names.get(c, {}).get('chsChapterTitle', '') for c in personal_chapters},
              'parentNamesZh': {str(i): by_id[i]['titleZh'] for i in personal_ids},
              'boundary': {'parentId': 4004, 'titleZh': by_id[4004]['titleZh'], 'required': '枫丹首任务可接且未开始；枫丹及以后保持导出原状态，需单独核验'}}
    personal_goal = ROOT / 'data/quest-adjust/goals/example-sumeru-complete-candidate.json'
    personal_data = {'version': 1, 'source': source, 'completeParents': personal_ids}
    if replace_goal:
        previous = read(personal_goal)
        if previous.get('source', {}).get('description') != source['description']:
            raise ValueError('已有目标来源不同，拒绝覆盖')
        personal_goal.write_text(json.dumps(personal_data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        write(personal_goal, personal_data)
    add('personal-100324832', personal_ids, label='个人号口述候选目标', input=str(Path(export).resolve()), goalfile=str(personal_goal.relative_to(ROOT)))
    segments[-1]['boundaryParents'] = [4004]
    write(base / 'manifest.json', {'segments': segments})
    write(base / 'classification.json', {'parents': info, 'archonOrder': [x['id'] for x in ordered], 'personalParentIds': personal_ids,
        'classificationRule': 'CityConfig.areaIdVec → WorldArea/Scene group area/guide坐标区域多边形/Quest Share scene或副本cityID/章节国别图标。多地区或无证据均未归类。角色图标、任务ID、lua文件数字不用于猜地区。',
        'inputsSha256': {p: hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in ['data/admin-catalog/quests.json', 'resources/ExcelBinOutput/ChapterExcelConfigData.json', 'resources/ExcelBinOutput/CityConfigData.json', 'resources/BinOutput/Scene/WorldArea/scene3_worldArea.json']}})
    print(json.dumps({'run': run, 'segments': len(segments), 'personalParents': len(personal_ids), 'regions': dict(Counter((x['kind'] + '/' + x['regionZh']) for x in info))}, ensure_ascii=False))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=['prepare'])
    p.add_argument('--export', required=True)
    p.add_argument('--run', default='c5-' + datetime.now().strftime('%Y%m%d-%H%M%S'))
    p.add_argument('--replace-goal', action='store_true')
    a = p.parse_args()
    prepare(a.export, a.run, a.replace_goal)
