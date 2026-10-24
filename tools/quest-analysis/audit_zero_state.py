#!/usr/bin/env python3
"""只统计当前资源，不加载账号或数据库。"""
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    quests = read(ROOT/'resources/ExcelBinOutput/QuestExcelConfigData.json')
    mains = {int(p.stem):read(p) for p in (ROOT/'resources/BinOutput/Quest').glob('*.json')}
    first = {min(m['subQuests'],key=lambda q:q.get('order',0))['subId'] for m in mains.values() if m.get('subQuests')}
    zero = [{'subId':q['subId'],'mainId':q.get('mainId'),'param':c['param']} for q in quests for c in q.get('acceptCond',[]) if c.get('type')=='QUEST_COND_STATE_EQUAL' and c.get('param',[1])[0]==0]
    incoming = {p:[i for i,m in mains.items() if p in m.get('suggestTrackMainQuestList',[])] for p in {q['mainId'] for q in zero}}
    data = {'acceptSubCount':len({q['subId'] for q in zero}), 'params':dict(Counter(str(q['param']) for q in zero)),
            'openingSubCount':sum(q['subId'] in first for q in zero),'nonOpeningSubCount':sum(q['subId'] not in first for q in zero),
            'parentsWithUniqueSuggestSource':sum(len(v)==1 for v in incoming.values()),'parentsWithNoSuggestSource':sum(not v for v in incoming.values()),
            'parentsWithMultipleSuggestSources':sum(len(v)>1 for v in incoming.values()),
            'fontaine':{'parent':4004,'sub':next(q for q in quests if q['subId']==400401),'suggestSources':incoming.get(4004)},'rows':zero}
    out=ROOT/'build/quest-adjust/zero-state-audit.json';out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print({k:v for k,v in data.items() if k not in ('rows','fontaine')})
if __name__=='__main__': main()
