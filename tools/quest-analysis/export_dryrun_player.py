#!/usr/bin/env python3
"""只读导出一个玩家及其任务；不执行任何数据库写入，不覆盖历史导出。"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sys


def main():
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--uid', type=int, required=True)
    args = parser.parse_args()
    sys.path.insert(0, str(root.parent / 'backups/quest-adjust-20261025-012425/_tools'))
    from pymongo import MongoClient
    from bson import json_util

    with MongoClient('mongodb://127.0.0.1:27018/?directConnection=true', serverSelectionTimeoutMS=5000,
                     appname='quest-adjust-readonly-export') as client:
        db = client['grasscutter']
        players = list(db['players'].find({'_id': args.uid}))
        if len(players) != 1:
            raise ValueError('未找到唯一玩家文档，停止导出')
        quests = list(db['quests'].find({'ownerUid': args.uid}).sort('parentQuestId', 1))
        # 再读一次检测导出期间的存档变化；没有写操作，也不尝试锁库。
        if players != list(db['players'].find({'_id': args.uid})) or quests != list(db['quests'].find({'ownerUid': args.uid}).sort('parentQuestId', 1)):
            raise RuntimeError('导出期间玩家或任务已变化，请在存档稳定时重试')
    now = datetime.now().astimezone()
    directory = root.parent / 'backups' / ('quest-adjust-dryrun-' + now.strftime('%Y%m%d-%H%M%S-%f'))
    directory.mkdir()
    files = {}
    for name, docs in [('players', players), ('quests', quests)]:
        raw = (json_util.dumps(docs, ensure_ascii=False, indent=2, json_options=json_util.CANONICAL_JSON_OPTIONS) + '\n').encode('utf-8')
        with (directory / (name + '.json')).open('xb') as handle:
            handle.write(raw)
        files[name] = {'count': len(docs), 'sha256': hashlib.sha256(raw).hexdigest()}
    manifest = {'uid': args.uid, 'exportedAt': datetime.now(timezone.utc).isoformat(),
                'source': '127.0.0.1:27018/grasscutter', 'readOnly': True,
                'consistency': '连续两次只读查询结果一致；非事务快照', 'files': files}
    with (directory / 'manifest.json').open('x', encoding='utf-8') as handle:
        json.dump(manifest, handle, ensure_ascii=False, indent=2)
    print(json.dumps({'directory': str(directory), 'uid': args.uid,
                      'players': len(players), 'quests': len(quests)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
