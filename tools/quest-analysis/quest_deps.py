#!/usr/bin/env python3
"""任务牵连静态分析：仅读取目录和 Java 源码，不连接服务端或数据库。"""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict, deque
import hashlib
import heapq
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
CONDS = ('acceptCond', 'finishCond', 'failCond')
EXECS = ('beginExec', 'finishExec', 'failExec')
CATS = ('quest_state', 'global_var', 'open_state', 'trans_point', 'area', 'scene_group', 'item_reward', 'other')

def typ(a):
    return str(a.get('type', a.get('_type', 'UNKNOWN')))

def params(a):
    return a.get('param', a.get('_param', [])) or []

def integer(v):
    try:
        return int(v)
    except (ValueError, TypeError):
        return None

def numbers(v):
    return [int(x) for x in re.findall(r'-?\d+', str(v))]

def compact(v):
    return json.dumps(v, ensure_ascii=False, separators=(',', ':'))

def strip_comments(s):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', s, flags=re.S)

class Analysis:
    def __init__(self, root):
        self.root = root
        self.inputs = {}
        self.enums, self.handlers = {}, {}
        self.counts = {k: defaultdict(Counter) for k in ('cond', 'content', 'exec')}
        self.readers = defaultdict(list)
        self.unresolved = []
        self.edges = []
        self.incoming = defaultdict(list)
        self.adj = defaultdict(list)
        self.catalog = json.loads(self.read(root / 'data/admin-catalog/quests.json'))
        self.mains = {m['id']: m for m in self.catalog['items']}
        self.subs = {}
        self.nodes = {}
        for m in self.mains.values():
            self.nodes[f"main:{m['id']}"] = self.node(m, True)
            for s in m.get('subQuests', []):
                if s['subId'] in self.subs:
                    raise ValueError(f"重复子任务 {s['subId']}")
                self.subs[s['subId']] = s
                self.nodes[f"sub:{s['subId']}"] = self.node(s, False)
        qroot = root / 'src/main/java/emu/grasscutter/game/quest'
        for kind, name, folder, annotation in [('cond','QuestCond','conditions','QuestValueCond'), ('content','QuestContent','content','QuestValueContent'), ('exec','QuestExec','exec','QuestValueExec')]:
            ep = qroot / 'enums' / (name + '.java')
            source = strip_comments(self.read(ep)) if ep.exists() else ''
            self.enums[kind] = set(re.findall(r'\b(QUEST_[A-Z0-9_]+)\s*\(', source))
            self.handlers[kind] = defaultdict(list)
            for p in sorted((qroot / folder).rglob('*.java')):
                source = strip_comments(self.read(p))
                for match in re.finditer(r'@' + annotation + r'\s*\((.*?)\)', source, re.S):
                    for t in re.findall(r'\bQUEST_[A-Z0-9_]+', match[1]):
                        self.handlers[kind][t].append(str(p.relative_to(root)).replace('\\','/'))
        rp = root / 'resources/ExcelBinOutput/RewardExcelConfigData.json'
        self.rewards = {r['rewardId']: r for r in json.loads(self.read(rp))} if rp.exists() else {}
        self.reward_path = str(rp) if rp.exists() else None
        self.build()

    def read(self, path):
        raw = path.read_bytes()
        self.inputs[str(path.relative_to(self.root)).replace('\\','/')] = hashlib.sha256(raw).hexdigest()
        return raw.decode('utf-8-sig')

    def node(self, q, main):
        return {'id': q['id'] if main else q['subId'], 'kind': 'main' if main else 'sub',
                'mainId': q['id'] if main else q['mainId'], 'name': q.get('titleZh') if main else q.get('descZh'),
                'order': q.get('order'), 'chapterId': q.get('chapterId'),
                'finishParent': q.get('finishParent', False),
                'conditions': {f: q.get(f, []) for f in CONDS},
                'combinators': {f: q.get(f+'Comb') for f in CONDS},
                'out': {c: [] for c in CATS}}

    def status(self, t, kind=None):
        kind = kind or ('exec' if t.startswith('QUEST_EXEC_') else 'cond' if t.startswith('QUEST_COND_') else 'content')
        if t not in self.enums[kind]:
            return 'missing'
        return 'handled' if t in self.handlers[kind] else 'enum_only'

    def record(self, node, field, i, a):
        return {'node': node, 'field': field, 'index': i, 'type': typ(a), 'param': params(a),
                'raw': a, 'status': self.status(typ(a)),
                'comb': self.nodes[node]['combinators'].get(field)}

    def edge(self, src, dst, cat, evidence, **extra):
        e = {'source': src, 'target': dst, 'category': cat, 'evidence': evidence, **extra}
        self.edges.append(e)
        self.adj[src].append(e)
        self.incoming[dst].append(e)
        if src in self.nodes:
            self.nodes[src]['out'][cat].append(e)
        return e

    def ref(self, node, rec, qid, parent=False, semantic='state'):
        if qid is None:
            return
        src = f"{'main' if parent else 'sub'}:{qid}"
        if src not in self.nodes:
            self.unresolved.append({'reference': src, 'evidence': rec})
        self.edge(src, node, 'quest_state', rec, semantic=semantic, resolved=src in self.nodes)

    def reader_keys(self, t, p):
        if not p:
            return []
        if 'QUEST_GLOBAL_VAR' in t:
            return [('global_var', str(p[0]))]
        if 'OPEN_STATE' in t:
            return [('open_state', str(p[0]))]
        if t.endswith(('UNLOCK_TRANS_POINT', 'SCENE_POINT_UNLOCK')) and len(p)>1:
            return [('trans_point', f'{p[0]}:{p[1]}')]
        if t.endswith('UNLOCK_ANY_TRANS_POINT'):
            return [('trans_point', f'{p[0]}:*')]
        if any(x in t for x in ('UNLOCK_AREA','SCENE_AREA_UNLOCKED','ENTER_AREA','CURRENT_AREA')):
            return [('area', ':'.join(map(str,p[:2])))]
        return []

    def classify(self, t):
        if 'QUEST_GLOBAL_VAR' in t: return 'global_var'
        if 'OPEN_STATE' in t: return 'open_state'
        if any(x in t for x in ('UNLOCK_POINT','LOCK_POINT','SCENE_POINT')): return 'trans_point'
        if any(x in t for x in ('UNLOCK_AREA','MAP_AREA','MAP_LAYER')): return 'area'
        if any(x in t for x in ('GROUP','GADGET','SCENE_TAG','SCENE_LEVEL_TAG','SCENE_VAR','ROLLBACK')): return 'scene_group'
        if re.search(r'ADD_(?:PACK_)?ITEM|GRANT_ITEM', t): return 'item_reward'
        return 'other'

    def write_keys(self, t, p, cat):
        if not p: return []
        if cat in ('global_var','open_state'):
            if 'BATCH' in t:
                return [(cat,str(x)) for x in numbers(p[0])]
            return [(cat,str(p[0]))]
        if cat in ('trans_point','area') and len(p)>1:
            return [(cat,f'{p[0]}:{p[1]}')]
        return []

    def build(self):
        # 先登记读取者；保留条件类型、参数位置、逻辑组合与原始记录。
        for node, n in self.nodes.items():
            q = self.mains[n['id']] if n['kind']=='main' else self.subs[n['id']]
            for field in CONDS:
                for i,a in enumerate(q.get(field, []) or []):
                    t,p = typ(a),params(a)
                    kind = 'cond' if t.startswith('QUEST_COND_') else 'content'
                    self.counts[kind][t][field] += 1
                    rec = self.record(node,field,i,a)
                    if t in ('QUEST_COND_STATE_EQUAL','QUEST_COND_STATE_NOT_EQUAL','QUEST_CONTENT_QUEST_STATE_EQUAL','QUEST_CONTENT_QUEST_STATE_NOT_EQUAL'):
                        if p: self.ref(node,rec,integer(p[0]))
                    elif 'PARENT_QUEST_STATE' in t or 'MAIN_QUEST_STATE' in t:
                        if p: self.ref(node,rec,integer(p[0]),True)
                    elif 'COMPLETE_ANY_QUEST' in t or 'COMPLETE_ALL_QUEST' in t:
                        for x in p:
                            for qid in numbers(x):
                                if qid: self.ref(node,rec,qid,False,'list_candidate')
                    elif t.endswith('QUEST_NOT_RECEIVE') or t.endswith('ITEM_GIVING_FINISHED'):
                        if p: self.ref(node,rec,integer(p[0]),False,'event_or_not_received')
                    elif 'QUEST_CHECK' in t:
                        self.unresolved.append({'reason':'QUEST_CHECK 参数不是已确认的任务 ID，不按数值碰撞连边', 'evidence':rec})
                    for key in self.reader_keys(t,p): self.readers[key].append(rec)
            if n['kind']=='sub' and q.get('finishParent'):
                self.edge(node,f"main:{q['mainId']}",'quest_state',{'finishParent':True},semantic='finish_parent')
        # 再建立写入到读取者的牵连边；不声称比较条件因此为真。
        for node,n in self.nodes.items():
            q = self.mains[n['id']] if n['kind']=='main' else self.subs[n['id']]
            for field in EXECS:
                for i,a in enumerate(q.get(field,[]) or []):
                    t,p = typ(a),params(a)
                    self.counts['exec'][t][field] += 1
                    rec = self.record(node,field,i,a)
                    cat = self.classify(t)
                    effect = {'effect':rec,'keys':self.write_keys(t,p,cat)}
                    if t == 'QUEST_EXEC_REFRESH_GROUP_SUITE' and len(p)>1:
                        effect['sceneId'] = integer(p[0])
                        effect['groupSuites'] = [numbers(pair) for pair in str(p[1]).split(';')]
                    if cat == 'item_reward':
                        effect['itemDecode'] = '保留原始参数；未确认的批量/版本格式不猜测数量'
                    n['out'][cat].append(effect)
                    for key in self.write_keys(t,p,cat):
                        readers = list(self.readers[key])
                        if cat=='trans_point': readers += self.readers[(cat,key[1].split(':')[0]+':*')]
                        for reader in readers:
                            self.edge(node,reader['node'],cat,{'write':rec,'read':reader},resource=key[1],semantic='may_affect')
                    if 'ROLLBACK' in t and p:
                        target = f"{'main' if 'PARENT' in t else 'sub'}:{p[0]}"
                        self.edge(node,target,'scene_group',rec,semantic='rollback_candidate')
            for item in q.get('gainItems',[]) or []:
                n['out']['item_reward'].append({'source':'gainItems','item':item})
            if n['kind']=='main':
                for rid in q.get('rewardIdList',[]) or []:
                    r = self.rewards.get(rid)
                    n['out']['item_reward'].append({'source':'rewardIdList','rewardId':rid,'resolved':r is not None,
                       'items':[x for x in (r or {}).get('rewardItemList',[]) if x.get('itemId')], 'raw':r,
                       'grant':'父任务奖励仅列一次；不能对每个子任务重复发放'})
            elif q.get('finishParent'):
                n['out']['item_reward'].append({'source':'parent_reward_reference','main':f"main:{q['mainId']}",'grant':'是否已领取及奖励索引需运行态确认'})

    def stats(self):
        tables = {}
        summary = {}
        for kind,counts in self.counts.items():
            rows = []
            for t,c in sorted(counts.items(),key=lambda kv:(-sum(kv[1].values()),kv[0])):
                rows.append({'type':t,'count':sum(c.values()),'fields':dict(c),'status':self.status(t,kind),'handlers':self.handlers[kind].get(t,[])})
            tables[kind]=rows
            summary[kind]={'total':sum(r['count'] for r in rows),'types':len(rows),
                'statusTypes':dict(Counter(r['status'] for r in rows)),
                'statusOccurrences':dict(sum((Counter({r['status']:r['count']}) for r in rows),Counter()))}
        missing=[r for r in tables['exec'] if r['status']=='missing']
        return {'summary':summary,'types':tables,'mainCount':len(self.mains),'subCount':len(self.subs),
                'baseline':{'expectedMissingExecOccurrences':2017,'expectedMissingExecTypes':42,
                'actualMissingExecOccurrences':sum(r['count'] for r in missing),'actualMissingExecTypes':len(missing),
                'catalogStats':self.catalog.get('stats',{}),
                'explanation':'2017 是未知 exec 出现次数而非全部 exec。按有效合并字段计数，不重复统计 sourceVariants；当前 Java 快照与旧导出或并发修改可能不同。'},
                'inputSha256':self.inputs}

    def topo(self, selected):
        # 只将等于已完成的正向条件用于拓扑；负条件和失败边不能冒充先决顺序。
        selected=set(selected); adj=defaultdict(set); indeg={n:0 for n in selected}
        for e in self.edges:
            a,b=e['source'],e['target']; r=e['evidence']; t=r.get('type',''); p=r.get('param',[])
            positive=(e.get('semantic')=='finish_parent' or
                      (r.get('field') in ('acceptCond','finishCond') and t.endswith('STATE_EQUAL') and len(p)>1 and integer(p[1])==3))
            if positive and a in selected and b in selected and b not in adj[a]:
                adj[a].add(b);indeg[b]+=1
        def key(n): return (self.nodes[n]['order'] or 0,self.nodes[n]['id'],n)
        ready=[key(n) for n,d in indeg.items() if d==0];heapq.heapify(ready); result=[]
        while ready:
            _,_,n=heapq.heappop(ready);result.append(n)
            for b in sorted(adj[n]):
                indeg[b]-=1
                if indeg[b]==0:heapq.heappush(ready,key(b))
        return result,sorted(selected-set(result),key=key)

    def tree(self, roots, depth):
        lines=[];seen=set();queue=deque((r,0) for r in roots)
        while queue:
            n,d=queue.popleft()
            if n in seen:continue
            seen.add(n); lines.append('  '*d+n+' '+str(self.nodes.get(n,{}).get('name') or ''))
            if d>=depth:continue
            for e in self.adj[n]:
                lines.append('  '*(d+1)+f"[{e['category']}/{e.get('semantic')}] -> {e['target']} {compact(e['evidence'])}")
                if e['target'] not in seen:queue.append((e['target'],d+1))
        return '\n'.join(lines)

    def example(self, title, mids):
        selected=[n for n,v in self.nodes.items() if v['mainId'] in mids]
        ordered,cycles=self.topo(selected)
        lines=[f'## {title}', '', f'主任务范围：{compact(sorted(mids))}。', '',
               '子任务正向状态依赖拓扑（同层用 order/ID 稳定排序，不代表剧情时间顺序；OR 分支只表示依赖覆盖，不要求全做）：',
               ' → '.join(n for n in ordered if n.startswith('sub:')), '',
               f'有环或被环阻塞、不能伪造拓扑顺序：{compact(cycles)}。', '']
        for node in ordered+cycles:
            n=self.nodes[node]; q=self.mains[n['id']] if n['kind']=='main' else self.subs[n['id']]
            lines += [f"### {node} {n['name'] or '（无中文名称）'}", '',
                      f"chapterId={self.mains[n['mainId']].get('chapterId')}；order={n['order']}；finishParent={n['finishParent']}。"]
            for field in CONDS+EXECS:
                records=q.get(field,[]) or []
                lines.append(f"- {field}（{q.get(field+'Comb','按数组顺序')}）：" + ('；'.join(f"`{typ(a)} {compact(params(a))}` [{self.status(typ(a))}]" for a in records) or '无'))
            for cat in CATS[1:]:
                effects=[e for e in n['out'][cat] if 'target' not in e]
                if effects: lines.append(f'- {cat}：`{compact(effects)}`')
            incoming=self.incoming[node]
            if incoming:
                lines.append('- 前置/读取牵连：'+ '；'.join(f"{e['source']} [{e['category']}] `{compact(e['evidence'])}`" for e in incoming))
            outgoing=self.adj[node]
            lines.append('- 下游（含接取候选、完成/失败观察者；不等于立即可接取）：'+('；'.join(f"{e['target']} [{e['category']}] `{compact(e['evidence'])}`" for e in outgoing) or '无已识别静态引用'))
            lines.append('')
        return '\n'.join(lines)

    def report(self):
        intro='''# 强制完成父任务的连带处理清单

这是离线检查清单，不是可执行强制完成方案；handled 只证明存在注解处理器，不证明实现完整或客户端已验证。

1. 固定任务目录、奖励表和源码版本，备份运行态；确定父任务、子任务当前状态、已领取奖励、互斥分支及回滚点。不要无差别完成所有子任务。
2. quest_state：按正向状态依赖拓扑检查 acceptCond/finishCond/failCond 及 AND/OR，单独处理 NOT_EQUAL、未开始状态和失败条件。外部前置不应被悄悄完成。
3. beginExec：若确实需要开始子任务，先检查按原数组顺序产生的副作用、临时角色/道具和场景准备；不要对已经执行的动作重复重放。
4. trans_point / area：核验点位和区域的真实解锁状态。finishCond 是观察条件，不是解锁命令；强行置完成不会自动产生相应点位、神像或区域副作用。
5. finishExec：以每个任务原数组顺序逐条审计 global_var、open_state、scene_group 和 other。不能先把同类操作统一执行，因为可能存在顺序依赖。SET 与 INC/DEC 的重放风险不同。
6. scene_group：确认场景、group、suite、变量、gadget、tag、动态组、Lua 通知的实际效果；ROLLBACK 单独审计，不把回滚边当解锁边。failExec 仅属于失败路径，不跟随正常完成执行。
7. item_reward：核对子任务 gainItems、加物品 exec、消耗物品动作；父任务 rewardIdList 只在正确结算点处理一次，检查奖励索引与已领取标记，避免重复发奖。ADD_QUEST_PROGRESS 不是物品。
8. 父任务：确认 finishParent 与父任务完成条件、通知、存档和持久化；重新计算后续 acceptCond，检查其余等级、物品、变量、活动等限制，才判断实际可接取。
9. 对 missing / enum_only 逐项列为缺口，不用状态置位代替功能。检查失败观察者、互斥任务、重登录、场景重载、客户端同步和奖励幂等；这些运行态验证本脚本未执行。

## 例子范围与关键事实

蒙德 35205 的 finishExec 解锁 [3,7] 和区域 [3,1]，并切换元素 [7]；纳塔 500006 的 finishCond 观察 [3,1166]，finishExec 仅写全局变量 [7509701,1]，不能据此推断它解锁神像。
351 的 chapterId 为空；以 352 的 chapterId=1001 确认序章第一幕范围，另纳入 351 前置。同章旧测试任务也保留并标明，不能凭编号连续推断属于实际剧情主线。

'''
        chapter=self.mains[352].get('chapterId')
        scope={m['id'] for m in self.mains.values() if chapter is not None and m.get('chapterId')==chapter}|{351}
        return intro+self.example('例一 A：蒙德神像 35205 前后链（主任务 351、352、353）',{351,352,353})+'\n'+self.example('例一 B：纳塔神像 500006（主任务 5000；边界列外部读写者）',{5000})+'\n'+self.example('例二：蒙德序章第一幕（chapterId=1001，加 351 前置）',scope)

    def export(self):
        # 并行会话可能修改输入；拒绝交付读取期间发生变化的混合快照。
        changed = [name for name, digest in self.inputs.items()
                   if hashlib.sha256((self.root/name).read_bytes()).hexdigest()!=digest]
        if changed:
            raise RuntimeError('读取期间输入已变化，请重跑：'+', '.join(changed))
        out=HERE/'out';out.mkdir(exist_ok=True)
        stats=self.stats()
        data={'schemaVersion':1,'sourceGeneratedAt':self.catalog.get('generatedAt'),'inputSha256':self.inputs,
              'semantics':'出边方向为被引用任务/写入者 -> 读取者；may_affect 不保证条件满足；原始条件与组合保留。',
              'nodes':self.nodes,'edges':self.edges,
              'resourceReaders':[{'category':k[0],'key':k[1],'readers':v} for k,v in sorted(self.readers.items())],
              'unresolved':self.unresolved,'rewardSource':self.reward_path}
        for name,value in [('deps.json',data),('stats.json',stats)]:
            self.save(out/name,json.dumps(value,ensure_ascii=False,indent=2)+'\n')
        md=['# 类型统计','',f"主任务 {len(self.mains)}；子任务 {len(self.subs)}。",'',
            'handled：枚举存在且找到注解 handler；enum_only：只有枚举；missing：枚举没有。按源码扫描，不承诺运行注册成功或实现完整。',
            '统计有效字段，不统计 sourceVariants；cond/content 按类型前缀区分，不按字段名混算。','',
            f"参考核对：未知 exec {stats['baseline']['actualMissingExecOccurrences']} 次 / {stats['baseline']['actualMissingExecTypes']} 种（参考 2017 / 42）。",
            stats['baseline']['explanation'],'',f"汇总：`{compact(stats['summary'])}`",'']
        for kind,rows in stats['types'].items():
            md += [f'## {kind}','', '| 类型 | 次数 | 阶段次数 | 状态 |','|---|---:|---|---|']
            md += [f"| {r['type']} | {r['count']} | {compact(r['fields'])} | {r['status']} |" for r in rows]
            md.append('')
        self.save(out/'stats.md','\n'.join(md)+'\n')
        self.save(out/'forced_complete_checklist.md',self.report())
        return stats

    @staticmethod
    def save(path, text):
        tmp=path.with_suffix(path.suffix+'.tmp');tmp.write_text(text,encoding='utf-8',newline='\n');tmp.replace(path)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    group=parser.add_mutually_exclusive_group()
    group.add_argument('--main',type=int,help='父任务 ID')
    group.add_argument('--sub',type=int,help='子任务 ID')
    parser.add_argument('--depth',type=int,default=2,help='下游展开层数，默认 2')
    args=parser.parse_args()
    if args.depth<0:parser.error('--depth 不能为负数')
    if hasattr(sys.stdout,'reconfigure'):sys.stdout.reconfigure(encoding='utf-8')
    analysis=Analysis(ROOT)
    if args.main is not None or args.sub is not None:
        root=f'main:{args.main}' if args.main is not None else f'sub:{args.sub}'
        if root not in analysis.nodes:parser.error('任务不存在：'+root)
        roots=[root]
        if args.main is not None:roots += [f"sub:{s['subId']}" for s in analysis.mains[args.main].get('subQuests',[])]
        print(analysis.tree(roots,args.depth))
    else:
        stats=analysis.export();print(compact({'out':str(HERE/'out'),'summary':stats['summary']}))

if __name__=='__main__':
    main()
