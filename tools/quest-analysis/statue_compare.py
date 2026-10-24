#!/usr/bin/env python3
"""只读离线对比已导出的 Mongo Extended JSON；仅使用 Python 标准库。

用法：python statue_compare.py 正常UID 异常UID --snapshot 备份目录
不会连接 Mongo、执行修复或覆盖文件。每次在 snapshot 下新建 compare-* 目录。
缺失字段保留为显式标记，绝不将缺失任务当作 UNSTARTED/FINISHED。
"""
import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

MISSING = {"$missing": True}
PARENTS = (351, 352, 353, 303, 5000)
GROUPS = (133004001, 133504005)


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def dump(path, value):
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)


def source(root, relative, needle):
    path = root / "src/main/java/emu/grasscutter" / relative
    if path.exists():
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if needle in line:
                return f"{relative}:{number} ({needle.strip()})"
    return f"{relative}（未定位行号：{needle}）"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("normal_uid", type=int)
    parser.add_argument("test_uid", type=int)
    parser.add_argument("--snapshot", type=Path, required=True)
    parser.add_argument("--source-root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    if args.normal_uid == args.test_uid:
        parser.error("两个 UID 必须不同")
    snap, root = args.snapshot.resolve(), args.source_root.resolve()
    players = {p["_id"]: p for p in read(snap / "players.json")}
    for uid in (args.normal_uid, args.test_uid):
        if uid not in players:
            parser.error(f"快照中没有 UID {uid}")
    # 先校验导出原件，避免把变更后的文件当作同一次调查的证据。
    manifest_path = snap / "export-manifest.json"
    verification = []
    if manifest_path.exists():
        for entry in read(manifest_path)["collections"]:
            actual = hashlib.sha256((snap / entry["file"]).read_bytes()).hexdigest()
            if actual != entry["sha256"]:
                parser.error(f"快照哈希不匹配：{entry['file']}")
            verification.append(entry["file"])
    quests = read(snap / "quests.json")
    groups = read(snap / "group_instances.json")
    avatars = read(snap / "avatars.json")
    catalog = read(root / "data/admin-catalog/quests.json")["items"]
    by_parent = {q["id"]: q for q in catalog}
    exec_index = {}
    for q in catalog:
        for child in q.get("subQuests", []):
            for phase in ("beginExec", "acceptExec", "finishExec", "failExec"):
                for ex in child.get(phase, []) or []:
                    key = (ex.get("type"), tuple(map(str, ex.get("param", []))))
                    exec_index.setdefault(key, []).append({
                        "parentId": q["id"], "subId": child.get("subId", child.get("id")),
                        "phase": phase, "type": ex.get("type"), "param": ex.get("param"),
                    })

    refs = {
        "quest": source(root, "game/quest/GameQuest.java", "public void finish()"),
        "progress": source(root, "game/quest/GameQuest.java", "public void setFinishProgress("),
        "talks": source(root, "game/talk/TalkManager.java", "public void saveTalkToQuest("),
        "forced": source(root, "game/quest/ForcedQuests.java", "public static int apply("),
        "point": source(root, "game/player/PlayerProgressManager.java", "public boolean unlockTransPoint("),
        "pointLock": source(root, "game/player/PlayerProgressManager.java", "public boolean lockTransPoint("),
        "bulk": source(root, "command/commands/UnlockAllCommand.java", "static MapUnlockResult unlockMap("),
        "area": source(root, "game/player/PlayerProgressManager.java", "public void unlockSceneAreaHierarchy("),
        "open": source(root, "game/player/PlayerProgressManager.java", "public void onPlayerLogin()"),
        "global": source(root, "game/quest/QuestManager.java", "public void setQuestGlobalVarValue("),
        "element": source(root, "game/quest/exec/ExecChangeAvatarElemet.java", "public boolean execute("),
        "group": source(root, "game/world/SceneGroupInstance.java", "public void setCached("),
        "groupRefresh": source(root, "scripts/SceneScriptManager.java", "groupInstance.setLastTimeRefreshed("),
        "sots": source(root, "game/managers/SotSManager.java", "public void refillSpringVolume()"),
    }
    diffs = []

    def difference(path, normal, test, provenance, note=""):
        if normal != test:
            diffs.append({"path": path, "normal": normal, "test": test,
                          "sources": provenance, "note": note})

    normal, test = players[args.normal_uid], players[args.test_uid]
    sets = {}
    for field in ("unlockedScenePoints", "unlockedSceneAreas", "forceLockedScenePoints"):
        a, b = sorted(normal.get(field, {}).get("3", [])), sorted(test.get(field, {}).get("3", []))
        sets[field] = {"normal": a, "test": b, "normalOnly": sorted(set(a)-set(b)),
                       "testOnly": sorted(set(b)-set(a))}
        provenance = ([refs["pointLock"], "35106.finishExec: QUEST_EXEC_LOCK_POINT ['3','1720']"]
                      if field == "forceLockedScenePoints" else [refs["point"], refs["area"], refs["bulk"]])
        difference(f"players.{field}.3", a, b, provenance,
                   "这些是可写入该字段的代码路径；快照不记录历史调用者，不能据此断言执行过某条命令。")
    for field, ref in (("openStates", "open"), ("questGlobalVariables", "global")):
        a, b = normal.get(field, {}), test.get(field, {})
        for key in sorted(set(a) | set(b), key=int):
            candidates = []
            typ = "QUEST_EXEC_SET_OPEN_STATE" if field == "openStates" else "QUEST_EXEC_SET_QUEST_GLOBAL_VAR"
            for (ex_type, params), entries in exec_index.items():
                if ex_type == typ and params and params[0] == key:
                    candidates.extend(entries)
            difference(f"players.{field}.{key}", a.get(key, MISSING), b.get(key, MISSING),
                       [refs[ref], {"candidateQuestExecs": candidates}],
                       "候选 exec 是静态写入来源，不是该账号执行过它的历史证明；缺失不等于数值 0。")
    difference("players.mainCharacterElement", normal.get("mainCharacterElement", MISSING),
               test.get("mainCharacterElement", MISSING),
               ["35205.finishExec: QUEST_EXEC_CHANGE_AVATAR_ELEMET ['7']", refs["element"]])
    difference("players.forcedFinishedQuests", sorted(normal.get("forcedFinishedQuests", [])),
               sorted(test.get("forcedFinishedQuests", [])), [refs["forced"]],
               "仅父任务标记；没有真实子任务 finish/exec/reward 语义。")

    quest_details = {}
    for parent_id in PARENTS:
        pair = [next((q for q in quests if q["ownerUid"] == uid and q["parentQuestId"] == parent_id), None)
                for uid in (args.normal_uid, args.test_uid)]
        a, b = pair
        for field in ("state", "isFinished", "questVars", "questGroupSuites", "talks"):
            difference(f"quests[parentQuestId={parent_id}].{field}",
                       a.get(field, MISSING) if a else MISSING,
                       b.get(field, MISSING) if b else MISSING,
                       [refs["talks"] if field == "talks" else refs["quest"]],
                       "文档缺失意味着整段任务未在本快照实例化；不能从父任务伪完成标记反推执行记录。")
        entries = []
        children = sorted(by_parent.get(parent_id, {}).get("subQuests", []), key=lambda q: q.get("order", 0))
        for spec in children:
            child_id = spec.get("subId", spec.get("id"))
            child_pair = [(q or {}).get("childQuests", {}).get(str(child_id)) for q in pair]
            views = []
            for c in child_pair:
                views.append({k: c.get(k, MISSING) if c else MISSING for k in
                              ("state", "finishProgressList", "acceptProgressList", "failProgressList",
                               "startTime", "acceptTime", "finishTime", "triggers")})
            metadata = {k: spec.get(k, []) for k in
                        ("acceptCond", "finishCond", "acceptExec", "finishExec")}
            entries.append({"subId": child_id, "order": spec.get("order"),
                            "normal": views[0], "test": views[1], "resource": metadata})
            for field in ("state", "finishProgressList", "acceptProgressList"):
                difference(f"quests[{parent_id}].childQuests.{child_id}.{field}",
                           views[0][field], views[1][field],
                           [refs["progress"] if field == "finishProgressList" else refs["quest"],
                            {"subId": child_id, **metadata}])
        quest_details[str(parent_id)] = {"normalDocumentPresent": a is not None,
                                        "testDocumentPresent": b is not None, "children": entries}

    group_details = {}
    for group_id in GROUPS:
        pair = [next((g for g in groups if g["ownerUid"] == uid and g["groupId"] == group_id), None)
                for uid in (args.normal_uid, args.test_uid)]
        group_details[str(group_id)] = {"normal": pair[0], "test": pair[1]}
        for field in ("targetSuiteId", "activeSuiteId", "cachedVariables", "cachedGadgetStates",
                      "deadEntities", "scriptDestroyedGadgets", "isCached", "lastTimeRefreshed"):
            difference(f"group_instances[{group_id}].{field}",
                       pair[0].get(field, MISSING) if pair[0] else MISSING,
                       pair[1].get(field, MISSING) if pair[1] else MISSING,
                       [refs["groupRefresh"] if field == "lastTimeRefreshed" else refs["group"]],
                       "缓存/刷新元数据差异不能直接解释为交互门槛。")
    spring_keys = ("10002", "10003", "10004", "10005")
    sots = {}
    for label, p in (("normal", normal), ("test", test)):
        sots[label] = {"properties": {k: p.get("properties", {}).get(k, MISSING) for k in spring_keys},
                       "springLastUsed": p.get("springLastUsed", MISSING),
                       "cityInfoData": p.get("cityInfoData", MISSING)}
    for key in spring_keys:
        difference(f"players.properties.{key}", sots["normal"]["properties"][key],
                   sots["test"]["properties"][key], [refs["sots"]])
    for key in ("springLastUsed", "cityInfoData"):
        difference(f"players.{key}", sots["normal"][key], sots["test"][key], [refs["sots"]])

    def quest_state(uid, parent, sub):
        q = next((q for q in quests if q["ownerUid"] == uid and q["parentQuestId"] == parent), {})
        return q.get("childQuests", {}).get(str(sub), {}).get("state", MISSING)

    facts = {}
    for uid in (args.normal_uid, args.test_uid):
        p = players[uid]
        points, areas = p.get("unlockedScenePoints", {}).get("3", []), p.get("unlockedSceneAreas", {}).get("3", [])
        q35205, q30341 = quest_state(uid, 352, 35205), quest_state(uid, 303, 30341)
        facts[str(uid)] = {
            "point7": 7 in points, "point1166": 1166 in points,
            "area1": 1 in areas, "area43": 43 in areas,
            "q35205": q35205, "q30341": q30341, "q500006": quest_state(uid, 5000, 500006),
            "var7509701": p.get("questGlobalVariables", {}).get("7509701", MISSING),
            "var502802": p.get("questGlobalVariables", {}).get("502802", MISSING),
            "talk31141StaticGate": q35205 == "QUEST_STATE_FINISHED" and 1 in areas,
            "talk31300StaticGate": q30341 == "QUEST_STATE_FINISHED" and 43 in areas,
            "pointCountScene3": len(points), "areaCountScene3": len(areas),
            "forcedParentCount": len(p.get("forcedFinishedQuests", [])),
            "travelers": [{k: a.get(k, MISSING) for k in ("avatarId", "skillDepotId", "skillLevelMap")}
                          for a in avatars if a.get("ownerId") == uid and a.get("avatarId") in (10000005, 10000007)],
        }
    data = {"createdAt": datetime.now(timezone.utc).isoformat(), "normalUid": args.normal_uid,
            "testUid": args.test_uid, "snapshot": str(snap), "verifiedFiles": verification,
            "limits": ["静态存档对比，不证明原生客户端修复或菜单实测。",
                       "acceptProgressLists 位于 QuestManager 内存中，数据库不能恢复其当前值。",
                       "运行时实体 ID、goddessSuiteNotified、goddessNpcEntityByPoint 不持久化。",
                       "资源中的 beginCond 为静态门槛；未把它宣称为本次客户端抓包结论。"],
            "facts": facts, "scene3Sets": sets, "diffs": diffs, "quests": quest_details,
            "groups": group_details, "sots": sots, "sourceRefs": refs,
            "catalogSha256": hashlib.sha256((root / "data/admin-catalog/quests.json").read_bytes()).hexdigest()}
    out = snap / (f"compare-{args.normal_uid}-{args.test_uid}-" + datetime.now().strftime("%Y%m%d-%H%M%S-%f"))
    out.mkdir()
    dump(out / "差异.json", data)
    lines = ["# 七天神像存档对比摘要", "", f"正常对照 UID {args.normal_uid}；异常对照 UID {args.test_uid}。",
             "", "仅静态调查；完整字段值、差集、任务进度和候选写入来源见同目录 `差异.json`。", "",
             "| 检查项 | 正常号 | 异常号 |", "|---|---|---|"]
    for key in facts[str(args.normal_uid)]:
        if key == "travelers":
            continue
        vals = [json.dumps(facts[str(uid)][key], ensure_ascii=False) for uid in (args.normal_uid, args.test_uid)]
        lines.append(f"| {key} | {vals[0]} | {vals[1]} |")
    lines += ["", "蒙德入口 31141：35205 已完成且区域 3/1 解锁。纳塔入口 31300：30341 已完成且区域 3/43 解锁。",
              "纳塔激活由 30341.finishExec 的 UNLOCK_POINT [3,1166]、UNLOCK_AREA [3,43] 写入；500006 只设置 7509701。",
              "", f"共 {len(diffs)} 条字段差异；以下差集不省略元素。", ""]
    for field, values in sets.items():
        lines += [f"## {field}.3", "", f"正常号独有：{values['normalOnly']}", "", f"异常号独有：{values['testOnly']}", ""]
    lines += ["## openStates 差集和值差异", "", "| 字段 | 正常号 | 异常号 |", "|---|---|---|"]
    for d in diffs:
        if d["path"].startswith("players.openStates."):
            lines.append(f"| {d['path']} | {json.dumps(d['normal'], ensure_ascii=False)} | {json.dumps(d['test'], ensure_ascii=False)} |")
    lines += ["", "缺失用 {\"$missing\": true} 表示；不是已验证的 0 值。", "",
              "SotS 对比：" + ("相关持久化字段完全相同。" if sots["normal"] == sots["test"] else "存在差异，详见 JSON。")]
    with (out / "中文摘要.md").open("x", encoding="utf-8") as stream:
        stream.write("\n".join(lines) + "\n")
    print(json.dumps({"output": str(out), "differenceCount": len(diffs), "facts": facts}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
