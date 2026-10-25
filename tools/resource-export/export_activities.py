#!/usr/bin/env python3
"""Export AstaPS activity schedules and periodic play schedules for the admin catalog."""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from datetime import datetime
from pathlib import Path

ACTIVITY_FILE = "NewActivityExcelConfigData.json"
QUEST_FILE = "NewActivityMainQuestDataExcelConfigData.json"
TEXT_FILE = "TextMapCHS.json"
ACTIVITY_CONFIG = "ActivityConfig.json"
PERIODIC_FILES = {
    "深境螺旋": ("TowerScheduleExcelConfigData.json", "scheduleId"),
    "幻想真境剧诗": ("RoleCombatScheduleExcelConfigData.json", "scheduleId"),
    "纪行": ("BattlePassScheduleExcelConfigData.json", "id"),
}
SCHEDULE_CANDIDATES = [
    "NewActivityTimeGroupExcelConfigData.json",
    "ActivityExcelConfigData.json",
    "OpActivityExcelConfigData.json",
    "TowerScheduleExcelConfigData.json",
    "RoleCombatScheduleExcelConfigData.json",
    "BattlePassScheduleExcelConfigData.json",
]


def read_json(path: Path):
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def rows(data):
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        return list(data.values())
    return []


def date_value(raw):
    if raw is None or raw == "":
        return None
    text = str(raw)
    parsed = None
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"):
        try:
            parsed = datetime.strptime(text, fmt)
            break
        except ValueError:
            continue
    if parsed is None:
        return {"raw": raw, "readable": None}
    return {"raw": raw, "readable": parsed.strftime("%Y年%m月%d日 %H:%M:%S")}


def map_handlers(project_root: Path):
    enum_path = project_root / "src/main/java/emu/grasscutter/game/props/ActivityType.java"
    enum_text = enum_path.read_text(encoding="utf-8")
    enum_values = {name: int(value) for name, value in re.findall(r"\b([A-Z][A-Z0-9_]*)\((\d+)\)", enum_text)}
    numeric_names = {value: name for name, value in enum_values.items()}
    handlers = {}
    source_files = []
    for java_path in (project_root / "src/main/java/emu/grasscutter/game/activity").rglob("*.java"):
        source = java_path.read_text(encoding="utf-8", errors="replace")
        match = re.search(r"@GameActivity\s*\(\s*ActivityType\.([A-Z0-9_]+)\s*\)\s*public\s+class\s+(\w+)", source)
        if match and match.group(2) != "DefaultActivityHandler":
            handlers[match.group(1)] = match.group(2)
            source_files.append(java_path.relative_to(project_root).as_posix())
    return enum_values, numeric_names, handlers, [enum_path.relative_to(project_root).as_posix()] + source_files


def find_direct_main_quests(row):
    # Only accept explicit quest ID fields; do not infer quest links from nearby IDs or condition groups.
    for key in ("mainQuestId", "mainQuestIdList", "mainQuestIds", "questIdList"):
        value = row.get(key)
        if isinstance(value, int):
            return [value]
        if isinstance(value, list):
            return [x for x in value if isinstance(x, int)]
    return []


def activity_date(row, config, keys):
    for source_row in (row, config or {}):
        for key in keys:
            if source_row.get(key) not in (None, ""):
                return date_value(source_row[key])
    return None


def nested_date(row, keys):
    found = []
    def walk(value):
        if isinstance(value, dict):
            for k, v in value.items():
                if k in keys and v not in (None, ""):
                    found.append(v)
                else:
                    walk(v)
        elif isinstance(value, list):
            for item in value:
                walk(item)
    walk(row)
    return found[0] if found else None


def export(resources: Path, project_root: Path):
    excel = resources / "ExcelBinOutput"
    activity_path = excel / ACTIVITY_FILE
    quest_path = excel / QUEST_FILE
    text_path = resources / "TextMap" / TEXT_FILE
    config_path = project_root / "data" / ACTIVITY_CONFIG
    activities = rows(read_json(activity_path))
    quest_rows = rows(read_json(quest_path)) if quest_path.exists() else []
    text_map = read_json(text_path)  # One load per run; this file is about 49 MB.
    config_rows = rows(read_json(config_path)) if config_path.exists() else []
    config_by_id = {x.get("activityId"): x for x in config_rows if isinstance(x, dict)}
    enum_values, numeric_names, handlers, handler_sources = map_handlers(project_root)

    source_files = [
        "resources/ExcelBinOutput/" + ACTIVITY_FILE,
        "resources/ExcelBinOutput/" + QUEST_FILE,
        "resources/TextMap/" + TEXT_FILE,
        "data/" + ACTIVITY_CONFIG,
        *handler_sources,
    ]
    schedule_sources = []
    schedule_tables = {}
    for filename in SCHEDULE_CANDIDATES:
        path = excel / filename
        if path.exists():
            schedule_tables[filename] = rows(read_json(path))
            schedule_sources.append("resources/ExcelBinOutput/" + filename)
    source_files.extend(schedule_sources)

    type_counts = Counter()
    handler_counts = Counter()
    missing_names = 0
    unknown_types = 0
    missing_schedule = 0
    items = []
    for row in activities:
        activity_id = row.get("activityId")
        raw_type = row.get("activityType")
        type_name = raw_type if isinstance(raw_type, str) else numeric_names.get(raw_type)
        handler = handlers.get(type_name) if type_name else None
        type_counts[str(raw_type)] += 1
        if type_name is None or (type_name not in enum_values and type_name != "NONE"):
            unknown_types += 1
        if handler:
            handler_counts[handler] += 1
        config = config_by_id.get(activity_id)
        name_hash = row.get("nameTextMapHash")
        name = text_map.get(str(name_hash)) if name_hash is not None else None
        if name is None:
            missing_names += 1
        schedule_id = row.get("scheduleId")
        if schedule_id is None and config:
            schedule_id = config.get("scheduleId")
        begin = activity_date(row, config, ("beginTime", "beginTimeStr", "beginDateStr"))
        end = activity_date(row, config, ("endTime", "endTimeStr", "endDateStr"))
        if not schedule_id and not begin and not end:
            missing_schedule += 1
        main_quests = find_direct_main_quests(row)
        items.append({
            "activityId": activity_id,
            "nameZh": name,
            "nameZhSource": "resources/TextMap/" + TEXT_FILE if name else None,
            "activityType": raw_type,
            "scheduleId": schedule_id,
            "beginTime": begin,
            "endTime": end,
            "mainQuestId": main_quests,
            "implementedHandler": handler,
            "inActivityConfig": config is not None,
            "source": ["resources/ExcelBinOutput/" + ACTIVITY_FILE, "resources/TextMap/" + TEXT_FILE]
                       + (["data/" + ACTIVITY_CONFIG] if config is not None else [])
                       + (["resources/ExcelBinOutput/" + QUEST_FILE] if main_quests else []),
        })

    periodic = []
    periodic_missing_names = 0
    periodic_count = Counter()
    for label, (filename, id_key) in PERIODIC_FILES.items():
        path = excel / filename
        if not path.exists():
            continue
        for row in rows(read_json(path)):
            if label == "深境螺旋":
                begin_raw = nested_date(row, ("openTime", "beginTime", "beginTimeStr"))
                end_raw = row.get("closeTime")
                name_hash = row.get("buffnameTextMapHash")
            elif label == "幻想真境剧诗":
                begin_raw = row.get("beginTimeStr")
                end_raw = row.get("endTimeStr")
                name_hash = row.get("nameTextMapHash")
            else:
                begin_raw = row.get("beginDateStr")
                end_raw = row.get("endDateStr")
                name_hash = row.get("titleNameTextMapHash")
            name = text_map.get(str(name_hash)) if name_hash is not None else None
            if name is None:
                periodic_missing_names += 1
            periodic_count[label] += 1
            periodic.append({
                "category": label,
                "scheduleId": row.get(id_key),
                "nameZh": name,
                "nameZhSource": "resources/TextMap/" + TEXT_FILE if name else None,
                "beginTime": date_value(begin_raw),
                "endTime": date_value(end_raw),
                "source": ["resources/ExcelBinOutput/" + filename, "resources/TextMap/" + TEXT_FILE],
            })

    output = {
        "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "source": list(dict.fromkeys(source_files)),
        "stats": {
            "activityCount": len(items),
            "byActivityType": dict(sorted(type_counts.items())),
            "implementedHandlerCount": sum(handler_counts.values()),
            "byImplementedHandler": dict(sorted(handler_counts.items())),
            "unknownActivityTypeCount": unknown_types,
            "missingScheduleCount": missing_schedule,
            "missingActivityNameZhCount": missing_names,
            "activityNameZhBySource": {
                "resources/TextMap/" + TEXT_FILE: len(items) - missing_names,
                "missing": missing_names,
            },
            "periodicScheduleCount": len(periodic),
            "byPeriodicCategory": dict(sorted(periodic_count.items())),
            "missingPeriodicNameZhCount": periodic_missing_names,
            "periodicNameZhBySource": {
                "resources/TextMap/" + TEXT_FILE: len(periodic) - periodic_missing_names,
                "missing": periodic_missing_names,
            },
            "questLinkageNote": "仅采用活动主表明确提供的 mainQuestId/mainQuestIdList/questIdList；关联表没有明确 activityId 外键时不推断。",
        },
        "items": items,
        "periodicSchedules": periodic,
    }
    destination = project_root / "data" / "admin-catalog" / "activities.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
        f.write("\n")

    samples = {x.get("activityId"): x for x in items}
    seven_x = max(
        (x for x in items if x.get("inActivityConfig") and isinstance(x.get("activityId"), int)),
        key=lambda x: x["activityId"],
        default=None,
    )
    print(json.dumps({
        "output": str(destination),
        "stats": output["stats"],
        "snowBallFight": samples.get(5344),
        "sample7x": seven_x,
    }, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--resources", type=Path, help="资源目录；默认使用项目根目录下的 resources")
    args = parser.parse_args()
    script = Path(__file__).resolve()
    project_root = script.parents[2]
    resources = args.resources.resolve() if args.resources else project_root / "resources"
    export(resources, project_root)


if __name__ == "__main__":
    main()
