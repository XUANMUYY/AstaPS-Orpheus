#!/usr/bin/env python3
"""Export the quest tree, preserving raw actions and their source differences."""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sys

from _a6_textnames import SOURCE as NAME_SOURCES, chapter as reference_chapter
from _a6_textnames import load as load_reference_names
from _a6_textnames import version as reference_version


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "data" / "admin-catalog" / "quests.json"
TYPE_ZH = {
    "AQ": "魔神任务",
    "LQ": "传说任务",
    "WQ": "世界任务",
    "EQ": "活动任务",
    "IQ": "中文类别未确认（IQ）",
    "FQ": "中文类别未确认（FQ）",
    "DQ": "中文类别未确认（DQ）",
    "VQ": "中文类别未确认（VQ）",
}
ACTION_FIELDS = ("beginExec", "finishExec", "failExec")
CONDITION_FIELDS = ("acceptCond", "finishCond", "failCond")
UNLOCK_ACTIONS = {"QUEST_EXEC_UNLOCK_POINT", "QUEST_EXEC_UNLOCK_AREA"}


def read_json(path: Path):
    with path.open("r", encoding="utf-8") as stream:
        return json.load(stream)


def name_of(text_map: dict, text_hash):
    if not text_hash:
        return None
    return text_map.get(str(text_hash))


def valid_typed(values):
    return [value for value in (values or []) if isinstance(value, dict)
            and value.get("type", value.get("_type")) is not None]


def valid_gain_items(values):
    return [value for value in (values or []) if isinstance(value, dict)
            and value.get("id", 0) > 0 and value.get("count", 0) > 0]


def source_variants(excel: dict | None, binout: dict | None, merged: dict,
                    excluded=()):
    """Keep original values only where sources disagree or the effective value changed."""
    variants = {}
    keys = set(excel or ()) | set(binout or ())
    for key in sorted(keys - set(excluded)):
        raw = {}
        if excel is not None and key in excel:
            raw["excel"] = excel[key]
        if binout is not None and key in binout:
            raw["binout"] = binout[key]
        if (len(raw) == 2 and raw["excel"] != raw["binout"]) or any(
                value != merged.get(key) for value in raw.values()):
            variants[key] = raw
    return variants


def effective_comb(excel, binout, conditions):
    if (len(conditions or []) > 1 and excel in (None, "LOGIC_NONE")
            and binout not in (None, "LOGIC_NONE")):
        return binout
    return excel if excel is not None else "LOGIC_NONE"


def merge_sub(excel: dict | None, binout: dict | None, text_map: dict):
    merged = dict(binout or {})
    merged.update(excel or {})
    merged["source"] = "both" if excel and binout else "excel" if excel else "binout"
    main_id = merged.get("mainId")

    # Keep every typed resource action, even if the present server enum cannot
    # deserialize it. The stats explicitly count those compatibility gaps.
    # MainQuestData.onLoad calls QuestData.applyFrom for matching subquests.
    for field in CONDITION_FIELDS:
        merged[field] = valid_typed((excel or {}).get(field, (binout or {}).get(field)))
    for field in ACTION_FIELDS:
        excel_actions = valid_typed((excel or {}).get(field))
        merged[field] = excel_actions or valid_typed((binout or {}).get(field))
    excel_items = valid_gain_items((excel or {}).get("gainItems"))
    merged["gainItems"] = excel_items or valid_gain_items((binout or {}).get("gainItems"))
    if excel:
        # QuestData.onLoad sets this default before the BinOutput merge. Unlike
        # finish/fail combinators, applyFrom normally leaves acceptCondComb alone.
        merged["acceptCondComb"] = excel.get("acceptCondComb") or "LOGIC_NONE"

    if excel and binout:
        # applyFrom uses the BinOutput boolean values, including Java's false
        # default for absent primitive fields.
        merged["isRewind"] = bool(binout.get("isRewind", False))
        merged["finishParent"] = bool(binout.get("finishParent", False))
        if binout.get("showType") is not None:
            merged["showType"] = binout["showType"]
        merged["finishCondComb"] = effective_comb(
            excel.get("finishCondComb"), binout.get("finishCondComb"), merged["finishCond"])
        merged["failCondComb"] = effective_comb(
            excel.get("failCondComb"), binout.get("failCondComb"), merged["failCond"])
        if main_id in (351, 363):
            native_accept = [item for item in valid_typed(binout.get("acceptCond"))
                             if item.get("param", item.get("_param"))]
            if native_accept:
                merged["acceptCond"] = native_accept
            if binout.get("acceptCondComb") is not None:
                merged["acceptCondComb"] = binout["acceptCondComb"]

    for field in ("acceptCondComb", "finishCondComb", "failCondComb"):
        merged.setdefault(field, "LOGIC_NONE")
    merged.setdefault("versionBegin", None)
    merged["descZh"] = name_of(text_map, merged.get("descTextMapHash"))
    merged["stepDescZh"] = name_of(text_map, merged.get("stepDescTextMapHash"))
    variants = source_variants(excel, binout, merged)
    if variants:
        merged["sourceVariants"] = variants
    return merged


def merge_main(excel: dict | None, binout: dict | None, chapter: dict | None,
               chapter_change: dict | None, text_map: dict, chapter_reference: dict):
    merged = {k: v for k, v in (binout or {}).items() if k != "subQuests"}
    merged.update(excel or {})
    merged["source"] = "both" if excel and binout else "excel" if excel else "binout"
    quest_type = merged.get("type")
    merged["typeZh"] = TYPE_ZH.get(quest_type, "资源未标注类型" if quest_type is None
                                    else f"中文类别未确认（{quest_type}）")
    merged["titleZh"] = name_of(text_map, merged.get("titleTextMapHash"))
    merged["titleZhSource"] = "TextMap/TextMapCHS.json" if merged["titleZh"] else None
    merged["descZh"] = name_of(text_map, merged.get("descTextMapHash"))
    merged.setdefault("series", None)
    merged.setdefault("luaPath", None)
    merged.setdefault("rewardIdList", [])
    merged.setdefault("suggestTrackMainQuestList", [])
    merged.setdefault("chapterId", None)

    # ChapterChange contains newer, resolvable hashes for select 7.x chapters.
    chapter_data = dict(chapter or {})
    if chapter_change:
        for key in ("chapterNumTextMapHash", "chapterTitleTextMapHash"):
            if name_of(text_map, chapter_change.get(key)):
                chapter_data[key] = chapter_change[key]
    merged["chapterNumTextMapHash"] = chapter_data.get("chapterNumTextMapHash")
    merged["chapterTitleTextMapHash"] = chapter_data.get("chapterTitleTextMapHash")
    merged["chapterNum"] = name_of(text_map, merged["chapterNumTextMapHash"])
    merged["chapterTitleZh"] = name_of(text_map, merged["chapterTitleTextMapHash"])
    merged["chapterTitleZhSource"] = "TextMap/TextMapCHS.json" if merged["chapterTitleZh"] else None
    if merged["chapterNum"] is None:
        merged["chapterNum"] = reference_chapter(chapter_reference, merged.get("chapterId"), "chsChapterName")
    if merged["chapterTitleZh"] is None:
        merged["chapterTitleZh"] = reference_chapter(
            chapter_reference, merged.get("chapterId"), "chsChapterTitle")
        if merged["chapterTitleZh"]:
            merged["chapterTitleZhSource"] = NAME_SOURCES["chapters"]
    variants = source_variants(excel, binout, merged, excluded=("subQuests",))
    if variants:
        merged["sourceVariants"] = variants
    return merged


def export(resources: Path, output: Path):
    excel_dir = resources / "ExcelBinOutput"
    bin_dir = resources / "BinOutput" / "Quest"
    text_map = read_json(resources / "TextMap" / "TextMapCHS.json")
    chapter_reference = load_reference_names(ROOT, "chapters")["chapters"]
    excel_mains = {row["id"]: row for row in read_json(excel_dir / "MainQuestExcelConfigData.json")}
    excel_subs = {row["subId"]: row for row in read_json(excel_dir / "QuestExcelConfigData.json")}
    chapters = {row["id"]: row for row in read_json(excel_dir / "ChapterExcelConfigData.json")}
    changes = {row["chapterId"]: row for row in read_json(
        excel_dir / "ChapterChangeExcelConfigData.json")}

    bin_mains = {}
    bin_subs = {}
    for path in sorted(bin_dir.glob("*.json")):
        row = read_json(path)
        main_id = row["id"]
        if main_id in bin_mains:
            raise ValueError(f"duplicate BinOutput main ID: {main_id}")
        bin_mains[main_id] = row
        for sub in row.get("subQuests") or []:
            sub_id = sub["subId"]
            if sub_id in bin_subs:
                raise ValueError(f"duplicate BinOutput sub ID: {sub_id}")
            bin_subs[sub_id] = sub

    all_main_ids = set(bin_mains) | set(excel_mains)
    all_sub_ids = set(bin_subs) | set(excel_subs)
    sub_by_main = defaultdict(list)
    stats = {
        "binoutMainCount": len(bin_mains), "excelMainCount": len(excel_mains),
        "mainIntersection": len(set(bin_mains) & set(excel_mains)),
        "mainBinoutOnly": len(set(bin_mains) - set(excel_mains)),
        "mainExcelOnly": len(set(excel_mains) - set(bin_mains)),
        "binoutSubCount": len(bin_subs), "excelSubCount": len(excel_subs),
        "subIntersection": len(set(bin_subs) & set(excel_subs)),
        "subBinoutOnly": len(set(bin_subs) - set(excel_subs)),
        "subExcelOnly": len(set(excel_subs) - set(bin_subs)),
    }
    unlock = Counter()
    exec_counts = Counter()
    binout_exec_fallback = Counter()
    exec_enum_path = (ROOT / "src" / "main" / "java" / "emu" / "grasscutter"
                      / "game" / "quest" / "enums" / "QuestExec.java")
    exec_enum_names = set(re.findall(r"^\s*(QUEST_EXEC_[A-Z0-9_]+)\s*\(",
                                     exec_enum_path.read_text(encoding="utf-8"), re.MULTILINE))
    unknown_exec_types = Counter()
    missing_sub_desc = missing_step_desc = 0
    for sub_id in sorted(all_sub_ids):
        sub = merge_sub(excel_subs.get(sub_id), bin_subs.get(sub_id), text_map)
        if sub["descZh"] is None:
            missing_sub_desc += 1
        if sub["stepDescZh"] is None and sub.get("stepDescTextMapHash"):
            missing_step_desc += 1
        for field in ACTION_FIELDS:
            exec_counts[field] += len(sub[field])
            if (not valid_typed((excel_subs.get(sub_id) or {}).get(field))
                    and valid_typed((bin_subs.get(sub_id) or {}).get(field))):
                binout_exec_fallback[field] += 1
            for action in sub[field]:
                action_type = action.get("type", action.get("_type"))
                if action_type not in exec_enum_names:
                    unknown_exec_types[action_type] += 1
                if action_type in UNLOCK_ACTIONS:
                    unlock[action_type] += 1
        sub_by_main[sub["mainId"]].append(sub)

    # Include an otherwise orphaned subquest rather than losing a usable ID.
    all_main_ids |= set(sub_by_main)
    items = []
    type_counts = Counter()
    missing_title = missing_chapter_num = missing_chapter_title = 0
    for main_id in sorted(all_main_ids):
        excel = excel_mains.get(main_id)
        binout = bin_mains.get(main_id)
        chapter_id = (excel or binout or {}).get("chapterId")
        main = merge_main(excel, binout, chapters.get(chapter_id),
                          changes.get(chapter_id), text_map, chapter_reference)
        main["id"] = main_id
        main["subQuests"] = sorted(sub_by_main.get(main_id, []),
                                   key=lambda sub: (sub.get("order") or 0, sub["subId"]))
        type_counts[main.get("type") or "UNCLASSIFIED"] += 1
        missing_title += main["titleZh"] is None
        if chapter_id is not None:
            missing_chapter_num += main["chapterNum"] is None
            missing_chapter_title += main["chapterTitleZh"] is None
        items.append(main)

    stats.update({
        "mainCount": len(items), "subCount": len(all_sub_ids),
        "typeCounts": dict(sorted(type_counts.items())),
        "unclassifiedMainCount": type_counts["UNCLASSIFIED"],
        "missingMainTitleZh": missing_title,
        "missingSubDescZh": missing_sub_desc,
        "missingStepDescZhWithHash": missing_step_desc,
        "missingChapterNumWithId": missing_chapter_num,
        "missingChapterTitleZhWithId": missing_chapter_title,
        "mainTitleZhBySource": dict(sorted(Counter(
            main["titleZhSource"] or "missing" for main in items).items())),
        "chapterTitleZhBySource": dict(sorted(Counter(
            main["chapterTitleZhSource"] or "missing" for main in items).items())),
        "referenceNameVersion": reference_version(ROOT),
        "orphanSubCount": sum(len(subs) for main_id, subs in sub_by_main.items()
                              if main_id not in bin_mains and main_id not in excel_mains),
        "unlockExecCounts": {kind: unlock[kind] for kind in sorted(UNLOCK_ACTIONS)},
        "unlockExecTotal": sum(unlock.values()),
        "execCounts": {field: exec_counts[field] for field in ACTION_FIELDS},
        "binoutExecFallbackSubCounts": {field: binout_exec_fallback[field]
                                        for field in ACTION_FIELDS},
        "serverUnknownExecCount": sum(unknown_exec_types.values()),
        "serverUnknownExecTypeCounts": dict(sorted(unknown_exec_types.items())),
        "versionBeginPresentCount": sum("versionBegin" in (excel_subs.get(i) or {})
                                        or "versionBegin" in (bin_subs.get(i) or {})
                                        for i in all_sub_ids),
    })
    catalog = {
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "source": ["BinOutput/Quest/*.json", "ExcelBinOutput/MainQuestExcelConfigData.json",
                   "ExcelBinOutput/QuestExcelConfigData.json",
                   "ExcelBinOutput/ChapterExcelConfigData.json",
                   "ExcelBinOutput/ChapterChangeExcelConfigData.json",
                   "TextMap/TextMapCHS.json",
                   NAME_SOURCES["chapters"],
                   NAME_SOURCES["version"],
                   "src/main/java/emu/grasscutter/game/quest/enums/QuestExec.java"],
        "stats": stats, "items": items,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    temp = output.with_suffix(output.suffix + ".tmp")
    with temp.open("w", encoding="utf-8", newline="\n") as stream:
        json.dump(catalog, stream, ensure_ascii=False, separators=(",", ":"))
        stream.write("\n")
    temp.replace(output)
    print(json.dumps({"output": str(output), "bytes": output.stat().st_size,
                      "stats": stats}, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--resources", type=Path, default=ROOT / "resources",
                        help="resource directory (default: project resources)")
    args = parser.parse_args()
    if not args.resources.is_dir():
        parser.error(f"resources directory does not exist: {args.resources}")
    export(args.resources, OUTPUT)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"quest export failed: {error}", file=sys.stderr)
        raise SystemExit(1) from error
