#!/usr/bin/env python3
"""Export scene point resources into a Chinese admin catalog."""

from __future__ import annotations

import argparse
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "data" / "admin-catalog" / "scene_points.json"
NATIONS = {
    1: "蒙德",
    2: "璃月",
    3: "稻妻",
    4: "须弥",
    5: "枫丹",
    6: "纳塔",
    7: "挪德卡莱",
}
POINT_KINDS = {"SceneTransPoint", "VirtualTransPoint"}


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8-sig") as handle:
        return json.load(handle)


def text_for(text_map: dict[str, Any], value: Any) -> str | None:
    if value is None:
        return None
    result = text_map.get(str(value))
    return result if isinstance(result, str) and result else None


def normalize_position(value: Any) -> dict[str, Any] | None:
    if not isinstance(value, dict):
        return None
    # Preserve partial vectors: several scene resources omit a component.
    return {axis: value[axis] for axis in ("x", "y", "z") if axis in value}


def area_city(area_id: Any, areas: dict[int, dict[str, Any]], cities: list[dict[str, Any]]) -> list[int]:
    if not isinstance(area_id, int):
        return []
    ancestry: set[int] = set()
    current = area_id
    while current and current not in ancestry:
        ancestry.add(current)
        row = areas.get(current)
        if not row:
            break
        parent = row.get("areaID1")
        if parent is None:
            parent = row.get("areaID2")
        if not isinstance(parent, int) or parent == current:
            break
        current = parent
    matches = []
    for city in cities:
        city_id = city.get("cityId")
        area_ids = city.get("areaIdVec") or []
        if isinstance(city_id, int) and any(item in ancestry for item in area_ids if isinstance(item, int)):
            matches.append(city_id)
    return sorted(set(matches))


def classify(source_type: Any, gadget_name: str | None, gadget_source_name: str | None) -> str:
    source_marks_statue = gadget_source_name and (
        "Goddess" in gadget_source_name or "Goddness" in gadget_source_name
    )
    if source_type in POINT_KINDS and (
        (gadget_name and "七天神像" in gadget_name) or source_marks_statue
    ):
        return "statue"
    if source_type in POINT_KINDS or source_type == "DungeonWayPoint":
        return "waypoint"
    if source_type == "DungeonEntry":
        return "dungeon_entry"
    if source_type == "DungeonExit":
        return "dungeon_exit"
    return "other"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--resources",
        type=Path,
        default=ROOT / "resources",
        help="资源目录（默认是 AstaPS/resources）",
    )
    args = parser.parse_args()
    resources = args.resources.resolve()

    def load(relative: str) -> Any:
        return read_json(resources / relative)

    text_map = load("TextMap/TextMapCHS.json")
    world_areas = load("ExcelBinOutput/WorldAreaConfigData.json")
    cities = load("ExcelBinOutput/CityConfigData.json")
    gadgets = load("ExcelBinOutput/GadgetExcelConfigData.json")
    dungeons = load("ExcelBinOutput/DungeonExcelConfigData.json")
    scenes = load("ExcelBinOutput/SceneExcelConfigData.json")
    # Kept as source context for dungeon entry metadata; point dungeonIds link to DungeonExcelConfigData.
    dungeon_entries = load("ExcelBinOutput/DungeonEntryExcelConfigData.json")

    areas_by_id = {
        row.get("ID"): row for row in world_areas if isinstance(row.get("ID"), int)
    }
    for row in world_areas:
        for key in ("areaID1", "areaID2"):
            value = row.get(key)
            if isinstance(value, int):
                areas_by_id.setdefault(value, row)
    # PointData.areaId is the top-level areaID1. The Excel table's `ID` is a
    # separate composed key, so index display rows by scene and areaID1 too.
    areas_by_scene_parent: dict[tuple[int, int], dict[str, Any]] = {}
    for row in world_areas:
        scene_id = row.get("sceneID")
        parent_area = row.get("areaID1")
        if not isinstance(scene_id, int) or not isinstance(parent_area, int):
            continue
        key = (scene_id, parent_area)
        current = areas_by_scene_parent.get(key)
        if current is None or row.get("MBOHIOJBBHO") == "LEVEL_1":
            areas_by_scene_parent[key] = row
    gadgets_by_id = {
        row.get("id"): row for row in gadgets if isinstance(row.get("id"), int)
    }
    dungeons_by_id = {
        row.get("id"): row for row in dungeons if isinstance(row.get("id"), int)
    }
    scenes_by_id = {
        row.get("id"): row for row in scenes if isinstance(row.get("id"), int)
    }
    cities_by_id = {
        row.get("cityId"): row for row in cities if isinstance(row.get("cityId"), int)
    }

    point_files = sorted((resources / "BinOutput/Scene/Point").glob("*_point.json"))
    world_area_bin_files = sorted((resources / "BinOutput/Scene/WorldArea").glob("*.json"))
    world_area_bin_valid = 0
    for path in world_area_bin_files:
        # These files hold scene boundary geometry, while Excel supplies the
        # point area IDs and display hashes. Parse them as a consistency/source check.
        read_json(path)
        world_area_bin_valid += 1
    items: list[dict[str, Any]] = []
    scene_counts: Counter[str] = Counter()
    type_counts: Counter[str] = Counter()
    country_counts: Counter[str] = Counter()
    missing_coords = 0
    missing_area_id = 0
    unmapped_area_id = 0
    missing_area_name = 0
    missing_scene_name = 0
    missing_point_name = 0
    no_city = 0
    source_files = {
        "TextMap/TextMapCHS.json",
        "ExcelBinOutput/WorldAreaConfigData.json",
        "ExcelBinOutput/CityConfigData.json",
        "ExcelBinOutput/GadgetExcelConfigData.json",
        "ExcelBinOutput/DungeonEntryExcelConfigData.json",
        "ExcelBinOutput/DungeonExcelConfigData.json",
        "ExcelBinOutput/SceneExcelConfigData.json",
    }
    source_files.update(
        path.relative_to(resources).as_posix() for path in world_area_bin_files
    )

    for path in point_files:
        source_files.add(path.relative_to(resources).as_posix())
        stem = path.name.removesuffix("_point.json")
        try:
            scene_id = int(stem.removeprefix("scene"))
        except ValueError:
            continue
        payload = read_json(path)
        points = payload.get("points", {}) if isinstance(payload, dict) else {}
        if not isinstance(points, dict):
            continue
        scene = scenes_by_id.get(scene_id, {})
        scene_name = text_for(
            text_map,
            scene.get("sceneNameTextMapHash", scene.get("nameTextMapHash")),
        )
        if scene_name is None:
            missing_scene_name += len(points)

        for point_key, raw in points.items():
            if not isinstance(raw, dict):
                raw = {}
            source_type = raw.get("$type")
            try:
                point_id: int | str = int(point_key)
            except (TypeError, ValueError):
                point_id = str(point_key)
            gadget_id = raw.get("gadgetId")
            gadget = gadgets_by_id.get(gadget_id, {})
            gadget_name = text_for(text_map, gadget.get("nameTextMapHash"))
            name_zh = gadget_name
            name_source = "TextMap/TextMapCHS.json" if gadget_name else None
            kind = classify(source_type, gadget_name, gadget.get("jsonName"))
            area_id = raw.get("areaId")
            area = (
                areas_by_scene_parent.get((scene_id, area_id), {})
                if isinstance(area_id, int)
                else {}
            )
            area_name = text_for(text_map, area.get("areaNameTextMapHash"))
            if area_id is None:
                missing_area_id += 1
            elif not area:
                unmapped_area_id += 1
            if area_name is None:
                missing_area_name += 1

            city_ids = area_city(area_id, areas_by_id, cities)
            city_id = city_ids[0] if len(city_ids) == 1 else None
            city = cities_by_id.get(city_id, {}) if city_id is not None else {}
            country = NATIONS.get(city_id)
            if country is None:
                no_city += 1

            raw_dungeon_ids = raw.get("dungeonIds")
            if raw_dungeon_ids is None:
                raw_dungeon_ids = raw.get("JHHFPGJNMIN")
            dungeon_ids = raw_dungeon_ids if isinstance(raw_dungeon_ids, list) else []
            dungeon_names = []
            for dungeon_id in dungeon_ids:
                dungeon = dungeons_by_id.get(dungeon_id, {})
                dungeon_names.append(
                    text_for(
                        text_map,
                        dungeon.get("displayNameTextMapHash", dungeon.get("nameTextMapHash")),
                    )
                )
            if name_zh is None and kind == "dungeon_entry" and dungeon_names:
                name_zh = next((name for name in dungeon_names if name), None)
                if name_zh:
                    name_source = "TextMap/TextMapCHS.json"

            pos = normalize_position(raw.get("pos"))
            if pos is None or not all(axis in pos for axis in ("x", "y", "z")):
                missing_coords += 1
            item = {
                "sceneId": scene_id,
                "sceneNameZh": scene_name,
                "sceneNameZhSource": "TextMap/TextMapCHS.json" if scene_name else None,
                "pointId": point_id,
                "type": kind,
                "sourceType": source_type,
                "gadgetId": gadget_id,
                "nameZh": name_zh,
                "nameZhSource": name_source,
                "pos": pos,
                "tranPos": normalize_position(raw.get("tranPos")),
                "areaId": area_id,
                "areaNameZh": area_name,
                "areaNameZhSource": "TextMap/TextMapCHS.json" if area_name else None,
                "cityId": city_id,
                "cityIds": city_ids,
                "cityNameZh": text_for(text_map, city.get("cityNameTextMapHash")),
                "country": country,
                "dungeonIds": dungeon_ids,
                "dungeonNamesZh": dungeon_names,
                "isDefaultUnlocked": raw.get("unlocked"),
                "unlocked": raw.get("unlocked"),
                "forbidSimpleUnlock": raw.get("forbidSimpleUnlock"),
                "groupLimit": raw.get("groupLimit"),
                "mapVisibility": raw.get("mapVisibility"),
                "nativePointType": raw.get("type"),
                "dungeonEntryType": raw.get("dungeonEntryType"),
                "entryPointId": raw.get("entryPointId"),
                "tranSceneId": raw.get("tranSceneId", raw.get("JHBICGBAPIH")),
                "dungeonRandomList": raw.get("dungeonRandomList", raw.get("GLEKJMEEOMH")),
                "size": normalize_position(raw.get("size")),
            }
            items.append(item)
            scene_counts[str(scene_id)] += 1
            type_counts[kind] += 1
            country_counts[country or "unknown"] += 1
            if name_zh is None:
                missing_point_name += 1

    stats = {
        "total": len(items),
        "byScene": dict(sorted(scene_counts.items(), key=lambda pair: int(pair[0]))),
        "byType": dict(sorted(type_counts.items())),
        "byCountry": dict(sorted(country_counts.items())),
        "mainWorldStatuesScene3": sum(
            item["sceneId"] == 3 and item["type"] == "statue" for item in items
        ),
        "missingCoordinates": missing_coords,
        "missingAreaId": missing_area_id,
        "unmappedAreaId": unmapped_area_id,
        "missingAreaNameZh": missing_area_name,
        "missingSceneNameZhItems": missing_scene_name,
        "missingPointNameZh": missing_point_name,
        "pointNameZhBySource": {
            "TextMap/TextMapCHS.json": len(items) - missing_point_name,
            "missing": missing_point_name,
        },
        "areaNameZhBySource": {
            "TextMap/TextMapCHS.json": len(items) - missing_area_name,
            "missing": missing_area_name,
        },
        "unmappedCountry": no_city,
        "worldAreaConfigRows": len(world_areas),
        "worldAreaBinFilesRead": world_area_bin_valid,
        "dungeonEntryConfigRows": len(dungeon_entries),
        "textMapRows": len(text_map),
    }
    result = {
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "source": sorted(source_files),
        "stats": stats,
        "items": items,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        json.dumps(
            {
                "output": str(OUTPUT),
                "total": stats["total"],
                "byType": stats["byType"],
                "byCountry": stats["byCountry"],
                "mainWorldStatuesScene3": stats["mainWorldStatuesScene3"],
                "missingCoordinates": stats["missingCoordinates"],
                "unmappedAreaId": stats["unmappedAreaId"],
                "missingAreaNameZh": stats["missingAreaNameZh"],
                "missingPointNameZh": stats["missingPointNameZh"],
                "worldAreaBinFilesRead": stats["worldAreaBinFilesRead"],
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
