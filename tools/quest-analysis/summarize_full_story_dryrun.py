#!/usr/bin/env python3
"""汇总 C5 静态计划、分类证据及只读导出校验；没有数据库或服务端调用。"""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from full_story_dryrun import ROOT, OUT, REGIONS, ICONS, read, write


def seconds(n):
    return '%s分%.1f秒' % (int(n // 60), n % 60)


def short_type(t):
    for prefix, label in [('QUEST_EXEC_', 'E:'), ('QUEST_CONTENT_', 'C:'), ('QUEST_COND_', 'A:')]:
        if t.startswith(prefix):
            return label + t[len(prefix):]
    return {'goal_resolution': '目标分支', 'cycle': '阶段环路', 'state_conflict': '状态冲突'}.get(t, t)


def type_counts(issues):
    return dict(sorted(Counter(x['type'] for x in issues).items()))


def format_counts(counts):
    return '；'.join('%s×%s' % (short_type(t), n) for t, n in counts.items()) or '0'


def table(headers, rows):
    return '\n'.join(['| ' + ' | '.join(headers) + ' |', '|' + '|'.join('---' for _ in headers) + '|'] +
                     ['| ' + ' | '.join(str(v).replace('|', '\\|').replace('\n', ' ') for v in row) + ' |' for row in rows])


def main(run, export):
    base = OUT / run
    manifest = read(base / 'manifest.json')
    classification = read(base / 'classification.json')
    parents = classification['parents']
    by_id = {x['id']: x for x in parents}
    raw = {x['id']: x for x in read(ROOT / 'data/admin-catalog/quests.json')['items']}
    sub_main = {s['subId']: m['id'] for m in raw.values() for s in m.get('subQuests', [])}
    chapters = {x['id']: x for x in read(ROOT / 'resources/ExcelBinOutput/ChapterExcelConfigData.json')}
    source_handlers = set()
    handler_files = defaultdict(list)
    for path in (ROOT / 'src/main/java/emu/grasscutter/game/quest').rglob('*.java'):
        for t in re.findall(r'@QuestValue(?:Exec|Content|Cond)\s*\(\s*(?:Quest(?:Exec|Content|Cond)\.)?(QUEST_\w+)', path.read_text(encoding='utf-8')):
            source_handlers.add(t)
            handler_files[t].append(str(path.relative_to(ROOT)))
    plans = [read(OUT / x['outputFile']) for x in manifest['segments']]
    full, personal = plans[:-1], plans[-1]
    summaries = []
    engine, server = defaultdict(list), defaultdict(list)
    for data in full:
        plan = data.get('plan') or {'steps': [], 'blockers': [], 'expectedMail': []}
        segment = data['segment']
        issues = plan['blockers'] + data['resolutionIssues']
        prefix = segment['key'].split('-')[0]
        region = '未归类'
        if prefix == 'aq' and segment['key'] != 'aq-unassigned':
            cid = int(segment['key'].split('-')[1])
            icon = chapters[cid].get('chapterIcon', '').split('_')[-1]
            if icon in ICONS:
                region = REGIONS[ICONS[icon]]
            else:
                regions = sorted({r for i in segment['parentIds'] for r in by_id[i]['candidateRegions'] if r != 99})
                region = '/'.join(REGIONS[r] for r in regions) + ('（跨区）' if len(regions) > 1 else '') if regions else '未归类'
        elif prefix in ('lq', 'wq'):
            region = REGIONS[int(segment['key'].split('-')[1])]
        row = {'key': segment['key'], 'region': region, 'label': segment['label'], 'parents': len(segment['parentIds']),
               'steps': len(plan['steps']), 'starts': sum(s['action'] == 'start' for s in plan['steps']),
               'blockers': len(issues), 'blockersByType': type_counts(issues), 'mail': len(plan['expectedMail']),
               'estimatedSeconds': data['estimatedSeconds'], 'resolutionParents': len(data['resolutionIssues']),
               'unplannedTargets': len(data['unplannedTargetSubIds']), 'inheritedResolutionIssues': len(data['inheritedResolutionIssues']),
               'file': segment['outputFile']}
        summaries.append(row)
        for issue in issues:
            parent = issue['subId'] if issue['type'] == 'goal_resolution' else sub_main.get(issue['subId'], 0)
            item = {**issue, 'mainId': parent, 'segment': segment['key']}
            if issue['type'].startswith('QUEST_EXEC_') and issue['type'] not in source_handlers:
                server[issue['type']].append(item)
            elif issue['type'].startswith(('QUEST_CONTENT_', 'QUEST_COND_')) and issue['type'] not in source_handlers:
                server[issue['type']].append(item)
            elif issue['type'] == 'QUEST_EXEC_NOTIFY_GROUP_LUA' and '未实现' in issue['reason']:
                server['LuaAPI:UnlockScenePoint'].append(item)
            else:
                engine[issue['type']].append(item)
        # 正常流程 handler 缺失也单独统计，包含规划器可独立补偿而未阻断的类型。
        for gap in data['missingHandlers']:
            server[gap['type']].append({**gap, 'reason': '源码未找到handler；正常任务推进缺口', 'segment': segment['key']})

    def inventory(groups):
        result = []
        for typ, rows in sorted(groups.items()):
            mains = sorted({r['mainId'] for r in rows if r.get('mainId')})
            subs = sorted({r['subId'] for r in rows})
            result.append({'type': typ, 'mainCount': len(mains), 'mainIds': mains, 'subCount': len(subs), 'subIds': subs,
                           'occurrencesAcrossSegments': len(rows), 'examples': rows[:4], 'handlerSources': handler_files.get(typ, [])})
        return result
    engine_list, server_list = inventory(engine), inventory(server)
    total = {k: sum(r[k] for r in summaries) for k in ('steps', 'starts', 'blockers', 'mail', 'estimatedSeconds', 'resolutionParents', 'unplannedTargets')}
    total['blockersByType'] = dict(sum((Counter(r['blockersByType']) for r in summaries), Counter()))
    tests = [ET.parse(p).getroot() for p in (ROOT / 'build/test-results/test').glob('*quest.adjust*.xml')]
    validation = {k: sum(int(t.attrib.get(k, '0')) for t in tests) for k in ('tests', 'failures', 'errors', 'skipped')}
    validation['suites'] = len(tests)
    export = Path(export).resolve()
    export_manifest = read(export / 'manifest.json')
    backup_hashes = {key: hashlib.sha256((export / (key + '.json')).read_bytes()).hexdigest() == meta['sha256'] for key, meta in export_manifest['files'].items()}
    if not all(backup_hashes.values()):
        raise ValueError('只读导出哈希不一致')
    # 保存当前可复查的源码指纹；不能替代运行时注册或 Lua 验收。
    source_paths = list((ROOT / 'src/main/java/emu/grasscutter/game/quest/adjust').glob('*.java'))
    source_paths += [ROOT / p for p in sorted({p for paths in handler_files.values() for p in paths})]
    provenance = {'files': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in source_paths},
                  'catalogHashes': sorted({d['plan']['catalogHash'] for d in plans if d.get('plan')}),
                  'policyHashes': sorted({d['plan']['policyHash'] for d in plans if d.get('plan')}),
                  'planFiles': {s['outputFile']: hashlib.sha256((OUT/s['outputFile']).read_bytes()).hexdigest() for s in manifest['segments']},
                  'exportDirectory': str(export), 'exportManifest': export_manifest, 'exportHashVerified': backup_hashes, 'tests': validation}
    write(OUT / 'full-story-provenance.json', provenance)
    write(OUT / 'full-story-engine-blockers.json', engine_list)
    write(OUT / 'full-story-server-gaps.json', server_list)
    write(OUT / 'full-story-dryrun-summary.json', {'segments': summaries, 'total': total, 'tests': validation})

    # 歧义统计只计非(test)/$HIDDEN主任务；每条主任务集合单独去重，条目之间允许交叉。
    eligible = [m for m in parents if not m['testExcluded']]
    selected = set(personal['segment']['parentIds'])
    ambiguities = []
    def ambiguous(key, title, ids, decision):
        ids = sorted(set(ids))
        ambiguities.append({'key': key, 'title': title, 'mainCount': len(ids), 'mainIds': ids,
                            'selectedCount': len(selected.intersection(ids)), 'decision': decision})
    ambiguous('daily', '“全部”是否包含每日委托（IQ及luaPath=IQ）',
              [m['id'] for m in eligible if m['kind'] == 'IQ' or (m['luaPath'] or '').split('/')[-1].startswith('IQ')],
              '普通IQ暂不选；资源标成WQ但路径IQ的候选仍列出，需确认是否逐项排除。')
    ambiguous('events', '是否包含活动任务/EQ/有起止时间的章节',
              [m['id'] for m in eligible if m['kind'] == 'EQ' or (m['luaPath'] or '').split('/')[-1].startswith('EQ') or m['timedChapter']],
              'EQ暂不选；WQ/LQ的活动关联不自动视作永久内容。')
    ambiguous('limited-story', '是否包含限时、废弃、已下线或未发布的传说任务',
              [m['id'] for m in eligible if m['kind'] == 'LQ' and (m['timedChapter'] or any(x in m['titleZh'] for x in ('$UNRELEASED', '废弃', '弃用', '限时')))],
              '这是数据能识别的下界；资源没有可靠的“已下线”总表，不能据此声称其余全部永久开放。')
    ambiguous('hangout', '是否包含邀约任务及不同结局/存档点', [m['id'] for m in eligible if m['coop']], '候选个人目标暂不选邀约；全剧情LQ报告保留资源中的邀约任务。')
    for key, title, area_ids in [('chasm', '层岩巨渊地表及地下是否归璃月', {19, 300}),
                                  ('enkanomiya', '渊下宫是否归稻妻', {200}),
                                  ('desert', '须弥沙漠/千壑沙地/后续沙漠扩展是否归须弥', {27, 28, 29, 30, 31, 32})]:
        ambiguous(key, title, [m['id'] for m in eligible if m['kind'] == 'WQ' and set(m['areaIds']).intersection(area_ids)],
                  '能唯一归入对应城市的已纳入；缺证据或跨地区部分仍待确认。数量按明确区域证据计，不猜未归类任务。')
    ambiguous('islands', '金苹果群岛、琉形蜃境、希穆兰卡等海岛/活动场景是否算对应地区',
              [m['id'] for m in eligible if m['kind'] == 'WQ' and any(r in (100, 101, 102, 103, 104, 105) for r in m['candidateRegions'])],
              '按资源独立场景分组，不默认并入蒙德/璃月。空之域亦单列。')
    activity_dependent = []
    for m in eligible:
        if m['kind'] != 'WQ': continue
        if any('ACTIVITY' in c.get('type', '') for s in raw[m['id']].get('subQuests', []) for field in ('acceptCond', 'finishCond', 'beginExec', 'finishExec') for c in s.get(field, [])):
            activity_dependent.append(m['id'])
    ambiguous('wq-event-deps', '世界任务中依赖活动或版本限定内容的是否包含', activity_dependent,
              '不伪造活动状态；有明确地区的可能已进入候选，但相关缺口保留阻断/接取门槛warning。')
    ambiguous('cross-region', '同一主任务跨多个地区，按起点、主体还是全任务归属',
              [m['id'] for m in eligible if m['kind'] in ('WQ', 'LQ') and len(m['candidateRegions']) > 1],
              '多地区证据一律未归类，暂不作为个人完成目标；全剧情未归类段仍试算。')
    series_groups = defaultdict(list)
    for m in eligible:
        if m['kind'] in ('WQ', 'LQ') and (m['series'] or m['chapterId']):
            series_groups[(m['kind'], m['series'] or m['chapterId'])].append(m)
    cross_series = [v for v in series_groups.values() if len(v) > 1 and len({r for m in v for r in m['candidateRegions']}) > 1]
    ambiguous('cross-series', '同一系列的不同主任务跨地区，是否整系列纳入',
              [m['id'] for group in cross_series for m in group],
              '按主任务证据选取会拆分系列；候选中属于四地区的部分需确认，未自动补完其他地区。')
    ambiguous('no-region', '主任务完全缺地区/场景信息应如何纳入',
              [m['id'] for m in eligible if m['kind'] in ('WQ', 'LQ') and not m['candidateRegions']],
              '不凭ID或角色国籍猜地区；暂不作为个人目标。')
    late_mondstadt = [m['id'] for m in eligible if m['chapterId'] == 1004]
    ambiguous('late-interlude', '“序章/蒙德全部”是否包括纳塔阶段之后的间章第四幕“悖理”', late_mondstadt,
              '按章节tag晚于枫丹，个人目标暂不选；此前间章1205/1207/1307纳入。')
    ambiguous('new-old-regions', '新版本追加到四个旧地区的WQ是否也算已玩过',
              [m['id'] for m in eligible if m['kind'] == 'WQ' and m['regionId'] in (1, 2, 3, 4)],
              '当前按地区全部纳入，不能从“魔神停在枫丹前”推断这些支线在官服也已完成；本条是待核对的最大范围。')
    ambiguous('unreleased', '保留$UNRELEASED/废弃内容是否真要完成',
              [m['id'] for m in eligible if m['kind'] in ('LQ', 'WQ') and any(x in m['titleZh'] for x in ('$UNRELEASED', '废弃', '弃用'))],
              '用户仅明确排除(test)/$HIDDEN；未擅自多排除，但候选绝不可直接执行。')
    ambiguous('finish-branch', '多个或缺失finishParent时选择哪个真实结算分支',
              [x['subId'] for x in personal['resolutionIssues']], '严格目标解析保留错误，未挑最后一个ID或把多个结局都完成。')
    ambiguous('missing-title', '中文标题缺失，能否只按ID确认目标',
              [m['id'] for m in eligible if m['titleZh'] == '（中文主任务标题缺失）'], '目标source中保留缺失说明，不编造中文名。')
    write(OUT / 'personal-100324832-ambiguities.json', ambiguities)

    all_issues = personal['plan']['blockers'] + personal['resolutionIssues']
    completed = {s['mainId'] for s in personal['plan']['steps'] if s['action'] == 'finish'}
    forbidden = sorted(i for i in completed if by_id.get(i, {}).get('regionId') in (5, 6, 7, 8, 9, 105))
    first = next(x for x in personal['boundary']['4004'] if x['subId'] == 400401)
    boundary = {'parentId': 4004, 'subId': 400401, 'before': first['before'], 'projected': first['projected'],
                'rawAcceptCondition': first['accept'], 'rawAcceptConditionSatisfied': False,
                'reason': 'ConditionStateEqual缺少任务0时仅期望状态0才返回真；[0,3]返回假。源码搜索没有找到400401替代接取条件；因此不能证明可接，按原始条件计算不可接。',
                'laterRegionParentsWithFinishSteps': forbidden,
                'worldBoundaryVerified': False, 'worldBoundaryReason': '未执行且未完成Lua/全局变量/场景组效果审计，不能证明枫丹以后全部世界状态保持原样。'}
    personal_stats = {'goalParents': len(selected), 'steps': len(personal['plan']['steps']), 'starts': sum(s['action'] == 'start' for s in personal['plan']['steps']),
                      'blockers': len(all_issues), 'blockersByType': type_counts(all_issues), 'mail': len(personal['plan']['expectedMail']),
                      'estimatedSeconds': personal['estimatedSeconds'], 'resolutionParents': len(personal['resolutionIssues']),
                      'unplannedTargetSubIds': personal['unplannedTargetSubIds'], 'boundary': boundary, 'export': export_manifest}
    write(OUT / 'personal-100324832-dryrun-stats.json', personal_stats)

    files_added = [
        'src/main/java/emu/grasscutter/game/quest/adjust/AcceptLogic.java',
        'src/main/java/emu/grasscutter/game/quest/adjust/QuestAdjustBatchDryRun.java',
        'src/test/java/emu/grasscutter/game/quest/adjust/PlannerPhasesTest.java',
        'data/quest-adjust/goals/example-sumeru-complete-candidate.json',
        'tools/quest-analysis/full_story_dryrun.py', 'tools/quest-analysis/summarize_full_story_dryrun.py',
        'tools/quest-analysis/run_c5_locked.ps1']
    files_modified = ['src/main/java/emu/grasscutter/game/quest/adjust/' + x for x in ('QuestPlanner.java', 'CondCompensation.java', 'SideAcceptPreview.java')]
    files_modified += ['tools/quest-analysis/quest-adjust.gradle', 'tools/quest-analysis/export_dryrun_player.py']
    change_lines = ['新增：'] + ['- `' + p + '`' for p in files_added] + ['', '修改：'] + ['- `' + p + '`' for p in files_modified]
    change_lines += ['', '产物：本目录75份`full-story-*.json`、个人计划/统计/歧义JSON、两份Markdown汇总、汇总/缺口/指纹JSON；`' + run + '/`保存分类证据、分段目标、manifest和最终日志。',
                     '早期`c5-20261025/`、`c5-final-input/plans/`及`c5-final-input/pre-variable-fix/`保留为诊断历史，不作为正式统计。',
                     '工作根之外仅新增只读导出目录：`' + str(export) + '`（players.json、quests.json、manifest.json）。']
    changes = '\n'.join(change_lines)
    fixes = '''1. 用(子任务,接取/完成阶段)建图，处理进行中依赖和负向门槛的时序；相邻接取/完成合并，穿插时保留start，beginExec只预检一次，奖励只计finish。
2. 展开LogicType的正向复合组合，沿确定性正常分支规划；接取预览共用真值逻辑。失败必经分支继续排除。
3. 完成条件有handler也不等于世界状态已补齐：未有补偿或审计的持久效果一律阻断，已核实的瞬时动作单列。缺handler且无补偿不再只是warning。
4. 修正变量NOT_EQUAL补偿被误当EQUAL的问题，保留已有合法值，相等时选择安全的不同值，并覆盖整数边界。
5. 按主任务、建议前置、全局变量写入建立索引，用优先队列拓扑排序；批量诊断复用资源。目标歧义逐项列出，在线严格resolver不放宽。

尚未解决：finishParent按order纳入所有早期步骤时会包含互斥/废弃路线，需通用分支可达性与世界效果审计，不能只删除报错节点；多/无结算分支需用户进度或经过验证的分支语义；Lua未审计/刷怪/跨组效果不自动放行；回滚exec、随机持久写入、全局变量多写入者保留阻断。已有handler但欠缺重放补偿的cond（Lua进度、机关、任务状态、邀约存档等）也保留阻断。本卡未实现正常服务端处理器或Lua API。'''
    commands = r'''```powershell
# 两条命令都由tools/quest-analysis/run_c5_locked.ps1创建CreateNew锁，finally仅释放自己的锁。
.\gradlew.bat test --tests "*quest.adjust*" -PskipHandbook=1 --no-daemon
.\gradlew.bat -I tools/quest-analysis/quest-adjust.gradle questAdjustBatchDryRun `
  -Pmanifest=build/quest-adjust/c5-final-input/manifest.json `
  -Pout=build/quest-adjust -PskipHandbook=1 --no-daemon --no-configuration-cache
```
'''
    boundary_text = f'''枫丹首任务为4004「独舞者的序幕」、入口子任务400401，属于1401「白露与黑潮的序诗」。原始接取条件为`QUEST_COND_STATE_EQUAL [0,3]`，逻辑LOGIC_NONE。输入状态{first['before']}，计划后条件投影状态{first['projected']}（0=不存在/未开始）。

按`ConditionStateEqual`实现，任务0不存在且期望3时条件为假，因此**未开始可以确认；可接不能确认，按原始条件推算不可接**。未找到替代接取条件，不能把占位条件当成已满足。未来需要修通用入口依赖/接取支持，不能给个人号伪造接取标记。计划中枫丹及以后地区主任务的finish步骤涉及{len(forbidden)}个主任务；全局变量、Lua与世界状态边界仍未验收。当前计划不是“无缝接着玩”的已达成结果。'''
    personal_md = f'''# C5 UID 100324832 离线试算

仅生成文件。数据库写入、apply/undo、部署、服务启停、实际发邮件均为0。全部结论来自只读导出与静态规划，未实例化在线玩家或执行Lua。

## 目标与输入

目标：`data/quest-adjust/goals/example-sumeru-complete-candidate.json`，version=1，source.description=「主会话转述用户口述，2026-10-25，待用户确认」。共{len(selected)}个显式主任务ID；中文名注释位于source.parentNamesZh，缺名明确标记。序章至第三章第六幕，以及此前间章1205/1207/1307纳入；晚出的1004暂不纳入。四地区可唯一归类的普通LQ/WQ纳入，邀约、普通IQ/EQ、跨地区和无地区证据的任务暂不纳入。这是待确认候选，不是对口述的最终消歧。

导出：`{export}`。时间`{export_manifest['exportedAt']}`（UTC），1份players、{export_manifest['files']['quests']['count']}份quests，固定127.0.0.1:27018/grasscutter。两轮find一致（共四次find），非事务快照；players/quests的SHA256均已复核。强制完成标记未用于判断完成。

## 结果

{table(['候选主任务', '步骤（含start）', '阻断', '预计邮件', '1.3秒/步'], [[len(selected), len(personal['plan']['steps']), len(all_issues), len(personal['plan']['expectedMail']), seconds(personal['estimatedSeconds'])]])}

其中显式start={personal_stats['starts']}步；严格目标解析失败{len(personal['resolutionIssues'])}个主任务；另有{len(personal['unplannedTargetSubIds'])}个可解析目标因阶段环路未进入排序结果。两者不能用现有步数补齐，总量是可解析、可排序部分的理论值。存在阻断，执行与实际发信均为0，预计耗时不含Lua等待、备份、重登或修复时间。

阻断按类型：{format_counts(personal_stats['blockersByType'])}。`E:`=QUEST_EXEC_，`C:`=QUEST_CONTENT_，`A:`=QUEST_COND_；逐项子任务/原因见`personal-100324832-dryrun.json`的plan.blockers与resolutionIssues。

{boundary_text}

## 待用户确认的歧义

影响数是全资源非测试主任务的去重数量，“候选已选”是与本目标交集；不同条目可以重叠。没有地区证据的数量只是潜在影响，不能断言全部属于四地区。下线信息缺失时只给可识别下界。

{table(['歧义', '影响主任务', '候选已选', '当前处理'], [[a['title'], a['mainCount'], a['selectedCount'], a['decision']] for a in ambiguities])}

每条完整ID集合见`personal-100324832-ambiguities.json`；地区依据逐主任务见`c5-final-input/classification.json`。多结算分支的具体候选见个人计划resolutionIssues，没有擅自选最后一个ID。

## 引擎改动与剩余问题

{fixes}

## 验证

{commands}
最终针对性测试{validation['tests']}项，失败{validation['failures']}、错误{validation['errors']}；日志`c5-final-input/tests-final-verified.log`。正式批量试算日志`c5-final-input/batch-final.log`。构建均Java21、--no-daemon并持锁；未打包部署。`full-story-provenance.json`保存计划、源码和导出指纹。

## 文件清单

{changes}
'''
    (OUT / 'personal-100324832-dryrun-summary.md').write_text(personal_md, encoding='utf-8')
    table_rows = [[r['region'] + ' · ' + r['label'] + ' (' + r['key'] + ')', r['steps'], format_counts(r['blockersByType']), r['mail'], seconds(r['estimatedSeconds']), '%s/%s' % (r['resolutionParents'], r['unplannedTargets'])] for r in summaries]
    table_rows.append(['合计（各独立计划算术相加）', total['steps'], total['blockers'], total['mail'], seconds(total['estimatedSeconds']), '%s/%s' % (total['resolutionParents'], total['unplannedTargets'])])
    segment_table = table(['地区/章节或分组', '步骤', '阻断按类型（条）', '邮件', '理论耗时', '解析失败/未排序目标'], table_rows)
    simple_table = table(['分段', '步骤', '阻断', '邮件', '理论耗时'], [[r['key'], r['steps'], r['blockers'], r['mail'], seconds(r['estimatedSeconds'])] for r in summaries] + [['合计', total['steps'], total['blockers'], total['mail'], seconds(total['estimatedSeconds'])]])
    (base / 'final-table.md').write_text(simple_table, encoding='utf-8')
    server_table = table(['缺口类型', '影响主任务', '例子（主/子）'], [[x['type'], x['mainCount'], ', '.join('%s/%s' % (e['mainId'], e['subId']) for e in x['examples'][:2])] for x in server_list])
    engine_table = table(['引擎阻断类型', '影响主任务', '示例ID（目标分支项为主任务ID）'], [[x['type'], x['mainCount'], ','.join(map(str, x['subIds'][:4]))] for x in engine_list])
    excluded = sum(m['testExcluded'] for m in parents)
    missing = [m for m in parents if not m['resourceExists']]
    full_md = f'''# C5 全剧情分段离线试算

**只试算，未执行。** 本地资源共{len(parents)}个主任务，标题含(test)/$HIDDEN的{excluded}个不作目标；BinOutput主任务资源缺失{len(missing)}个（完整列表见classification.json）；QuestExcel引用的mainId亦无额外缺失。保留正常隐藏子步骤作为真实依赖。非ASCII全角（test）与$UNRELEASED没有被擅自按用户未给出的规则排除。

## 范围和证据

按资源ChapterExcel中CHAPTER_STYLE_TYPE_AQ与tag顺序出{sum(s['key'].startswith('aq-') and s['key'] != 'aq-unassigned' for s in summaries)}个魔神章节，最新1703「往冥府的安魂歌」；另{sum(s['parents'] for s in summaries if s['key'] == 'aq-unassigned')}个无有效章节的AQ/MQ单列。章节顺序以tag为准，例如1306在1302之后、1004在1504之后、1602在1600之前，不按chapterId数值猜。这里描述本地7.1资源，不能据此证明官服实际开放状态。

LQ/WQ各按地区单独规划；资源LQ包含邀约，个人目标另行排除。CityConfig.areaIdVec关联WorldArea；任务guide坐标落入WorldArea多边形、场景组area、副本cityID、Quest Share配置的场景/位置以及章节国别图标提供证据。缺场景时只在同一真实章节已有唯一地区证据时继承；角色国籍/数字ID不作为依据。冲突或缺证据归未归类，活动海岛等独立分组。逐主任务证据、区域列表、候选地区、是否测试/资源存在均在`c5-final-input/classification.json`。

魔神前段状态按顺序累积，**明确假定前段可解析目标终点已经完成**，包括前段被环路卡住的终点，仅用于下一章离线假设；不写导出文件，不抹掉原段阻断。多/无finishParent的前段无法猜终点，`inheritedResolutionIssues`保留这些不完整假设。LQ/WQ从同一份“魔神前置已完成”的内存假设分别计算，彼此不累积。Lua与未知效果未执行，完整世界状态没有被模拟。

所有计划只读，均无override；数据/服务/后台/部署未改动。工具的handler核验来自源码注解，不代表运行时注册。`executionAvailable`是既有基础设施标志，不能当作验收；本次计划均不可直接apply。

## 分段汇总

{segment_table}

类型缩写：`E:`=QUEST_EXEC_，`C:`=QUEST_CONTENT_，`A:`=QUEST_COND_。“目标分支”=goal_resolution，“阶段环路”=cycle。计数为阻断条目（同一子任务多个exec/cond可产生多条），不是受影响主任务数。每段JSON包含plan.blockers（类型、子任务、原因）、resolutionIssues（主任务ID、候选结算分支）、未排序目标、邮件来源、逐步exec/cond和1.3秒/步估时。

每段都存在剩余问题或继承的不完整假设。步数含显式start={total['starts']}；{total['resolutionParents']}次目标解析失败、{total['unplannedTargets']}个目标未排序（跨独立段可能重复）。**这些步数/邮件仅为可解析可排序部分，不能称为全部任务完成所需总量。** 总表是独立方案相加，跨组前置/邮件可能重复，不是一个合并账号计划。估时不含阻断修复、备份、Lua异步等待、发信与重登；实际重放和发信均为0。

## 引擎已修与尚未修复

{fixes}

{engine_table}

以上类型按跨段主任务ID去重计影响数；详细原因与子任务列表在`full-story-engine-blockers.json`。保留Lua旧审计结果（16个已审计子任务放9阻7），没有扩大放行范围或跳过刷怪/跨组。未补偿的cond现在显式阻断，所以阻断增加是纠正漏报，不是世界效果已实现。

## 正常服务端应实现的缺口（只列出）

{server_table}

缺口数覆盖当前计划步骤涉及的缺失handler，以及有补偿而未阻断的正常流程handler缺口；没有声称穷尽未解析分支或未执行Lua中的所有API。主任务影响数跨段去重，类型之间不能相加当成不同任务数。LuaAPI:UnlockScenePoint来自既有39301审计，当前未实现；未实施处理器/API。本地源码路径、子任务列表及例子见`full-story-server-gaps.json`。

## 个人号

候选目标{len(selected)}个主任务，{personal_stats['steps']}步，{personal_stats['blockers']}条阻断，{personal_stats['mail']}封理论邮件，约{seconds(personal_stats['estimatedSeconds'])}；未执行。{len(personal['resolutionIssues'])}个目标分支未定，{len(personal['unplannedTargetSubIds'])}个可解析目标未排序。

{boundary_text}

完整歧义及逐项数量见`personal-100324832-dryrun-summary.md`与`personal-100324832-ambiguities.json`。

## 验证与复现

{commands}
最终针对性测试{validation['tests']}项，失败{validation['failures']}、错误{validation['errors']}、跳过{validation['skipped']}；{validation['suites']}个测试类。新测试覆盖分阶段时序、beginExec去重、负向条件先后、复合逻辑穷举真值、缺handler阻断和handler存在但持久Lua进度未补偿仍阻断。日志`c5-final-input/tests-final-verified.log`。正式批量入口退出0，日志`c5-final-input/batch-final.log`。两者均Java21、--no-daemon、CreateNew持锁并finally释放。没有启动服务端、没有全量测试或部署。

Windows PowerShell5.1将原始javac stderr转成NativeCommandError曾使早期日志截断；已改为隐藏cmd子进程合并原始stdout/stderr并检查退出码，ps1以UTF-8 BOM保存。早期重试产生的FileAlreadyExists错误未覆盖旧计划；只有本次正式命名JSON计入汇总。

SHA256见`full-story-provenance.json`；所有正式计划catalogHash数量={len(provenance['catalogHashes'])}、policyHash数量={len(provenance['policyHashes'])}。只读导出两个文件SHA256均匹配manifest。个人最新导出时间{export_manifest['exportedAt']}（UTC）。

## 新增/修改文件

{changes}

未执行git add/commit/stash/reset/checkout，未修改git元数据。未碰tools/resource-export、data/admin-catalog、gacha、PushTips；未改范围外已有源码。build输出及用户指定的独立只读备份是本卡产物。
'''
    (OUT / 'full-story-dryrun-summary.md').write_text(full_md, encoding='utf-8')
    print(json.dumps({'full': total, 'personal': {k: v for k, v in personal_stats.items() if k not in ('export', 'boundary', 'unplannedTargetSubIds')},
                      'serverTypes': len(server_list), 'engineTypes': len(engine_list), 'tests': validation,
                      'ambiguities': [(a['key'], a['mainCount'], a['selectedCount']) for a in ambiguities]}, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--export', required=True)
    args = parser.parse_args()
    main(args.run, args.export)
