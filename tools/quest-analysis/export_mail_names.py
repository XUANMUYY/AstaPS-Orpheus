"""从 admin-catalog 生成任务补发邮件用的中文名索引 data/quest-adjust/mail-names.json。

只取邮件需要的两类名字：可发放物品（id → 中文名）和主任务（id → 标题）。
主任务标题缺失时退回「章节名（主任务 N）」，章节名也没有就不写，服务端再退回编号。
可重跑：python tools/quest-analysis/export_mail_names.py
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
CATALOG = ROOT / "data" / "admin-catalog"
OUT = ROOT / "data" / "quest-adjust" / "mail-names.json"


def main():
    items = {}
    for x in json.loads((CATALOG / "items.json").read_text(encoding="utf-8"))["items"]:
        name = x.get("nameZh")
        if not name:
            continue
        # 同一 id 既有材料又有头像时（共 29 个），以可发放的背包物品为准
        if str(x["id"]) in items and not x.get("grantableItem"):
            continue
        items[str(x["id"])] = name

    mains = {}
    for m in json.loads((CATALOG / "quests.json").read_text(encoding="utf-8"))["items"]:
        title = m.get("titleZh")
        if title and "$HIDDEN" not in title and not title.startswith("(test)"):
            mains[str(m["id"])] = title
        elif m.get("chapterTitleZh"):
            # 同一章节有多个主任务，带上编号才分得清
            mains[str(m["id"])] = f"{m['chapterTitleZh']}（主任务 {m['id']}）"

    OUT.write_text(json.dumps({"items": items, "mains": mains}, ensure_ascii=False, sort_keys=True, indent=0),
                   encoding="utf-8")
    print(f"物品 {len(items)}，主任务 {len(mains)} → {OUT}")


if __name__ == "__main__":
    main()
