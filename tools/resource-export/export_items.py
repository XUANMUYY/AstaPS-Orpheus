#!/usr/bin/env python3
"""Export a searchable item catalog from the installed Genshin resource tables.

Only rows with an inventory item ID are marked grantable. Profile pictures are
included for browsing, but their IDs are profile IDs, not inventory item IDs.
"""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import tempfile

from _a6_textnames import SOURCE as NAME_SOURCES, description as reference_description
from _a6_textnames import artifact_name as reference_artifact_name
from _a6_textnames import character_name as reference_character_name
from _a6_textnames import load as load_reference_names, name as reference_name
from _a6_textnames import version as reference_version


ROOT = Path(__file__).resolve().parents[2]
TABLES = (
    "MaterialExcelConfigData",
    "WeaponExcelConfigData",
    "ReliquaryExcelConfigData",
    "ReliquarySetExcelConfigData",
    "HomeWorldFurnitureExcelConfigData",
    "ProfilePictureExcelConfigData",
    "AvatarCostumeExcelConfigData",
    "AvatarPromoteExcelConfigData",
    "ProudSkillExcelConfigData",
    "WeaponPromoteExcelConfigData",
    "MaterialSourceDataExcelConfigData",
    "CookRecipeExcelConfigData",
    "CombineExcelConfigData",
)

SPECIAL_ITEMS = {
    201: ("货币", "祈愿货币"),
    202: ("货币", "通用货币"),
    203: ("货币", "付费货币"),
    221: ("货币", "祈愿兑换货币"),
    222: ("货币", "祈愿兑换货币"),
    223: ("祈愿道具", "限定祈愿"),
    224: ("祈愿道具", "常驻祈愿"),
}

WEAPON_TYPES = {
    "WEAPON_SWORD_ONE_HAND": "单手剑",
    "WEAPON_CLAYMORE": "双手剑",
    "WEAPON_POLE": "长柄武器",
    "WEAPON_CATALYST": "法器",
    "WEAPON_BOW": "弓",
}
RELIC_SLOTS = {
    "EQUIP_BRACER": "生之花",
    "EQUIP_NECKLACE": "死之羽",
    "EQUIP_SHOES": "时之沙",
    "EQUIP_RING": "空之杯",
    "EQUIP_DRESS": "理之冠",
}


def read_json(path: Path):
    with path.open("r", encoding="utf-8-sig") as stream:
        return json.load(stream)


def localized(text_map: dict[str, str], value: object) -> str | None:
    if value in (None, 0, "0", ""):
        return None
    result = text_map.get(str(value))
    return result if isinstance(result, str) and result.strip() else None


def item_rank(row: dict) -> int | None:
    # `rank` is a sort weight in these tables; rankLevel is the star rating.
    value = row.get("rankLevel")
    return value if isinstance(value, int) and 1 <= value <= 5 else None


def costs_used_by(rows: list[dict]) -> set[int]:
    return {
        cost["id"]
        for row in rows
        for cost in row.get("costItems", [])
        if isinstance(cost, dict) and isinstance(cost.get("id"), int) and cost["id"] > 0
    }


def classify_material(row: dict, uses: set[str], cooking_inputs: set[int],
                      cooking_outputs: set[int], combine_inputs: set[int],
                      type_description: str | None) -> tuple[str, str | None]:
    item_id = row["id"]
    material_type = row.get("materialType") or ""
    if item_id in SPECIAL_ITEMS:
        return SPECIAL_ITEMS[item_id]
    if material_type == "MATERIAL_NAMECARD":
        return "名片", None
    if material_type == "MATERIAL_COSTUME":
        return "服装", None
    if material_type == "MATERIAL_AVATAR":
        return "角色卡", None
    if material_type == "MATERIAL_FLYCLOAK":
        return "风之翼", None
    if material_type == "MATERIAL_EXP_FRUIT":
        return "经验书", "角色经验书"
    if material_type == "MATERIAL_WEAPON_EXP_STONE":
        return "经验书", "武器精锻矿"
    if material_type == "MATERIAL_RELIQUARY_MATERIAL":
        return "圣遗物经验", None
    if 104100 <= item_id <= 104199 and "avatar" in uses:
        return "角色突破", "突破宝石"
    if 104300 <= item_id <= 104399 and "talent" in uses:
        return "天赋", "天赋书"
    if 113000 <= item_id <= 113999 and "talent" in uses:
        return "周本", "天赋材料"
    if 113000 <= item_id <= 113999 and "avatar" in uses:
        return "BOSS 掉落", "角色突破材料"
    if 114000 <= item_id <= 114999 and "weapon" in uses:
        return "武器突破", "武器突破材料"
    if 112000 <= item_id <= 112999 and uses:
        return "普通怪掉落", "培养材料"
    if "avatar" in uses and "talent" not in uses and "weapon" not in uses:
        if 100000 <= item_id <= 103999:
            return "特产", "角色突破采集物"
        return "角色突破", "突破材料"
    if "talent" in uses:
        return "天赋", "天赋材料"
    if "weapon" in uses:
        return "武器突破", "突破材料"
    if item_id in range(101001, 101010) and material_type == "MATERIAL_EXCHANGE":
        return "矿石", "锻造矿石"
    if item_id in cooking_outputs or material_type in {
        "MATERIAL_FOOD", "MATERIAL_SPICE_FOOD", "MATERIAL_NOTICE_ADD_HP"
    }:
        return "食物", None
    if item_id in cooking_inputs:
        return "烹饪原料", None
    if material_type == "MATERIAL_QUEST":
        return "任务道具", None
    if material_type.startswith("MATERIAL_QUEST_"):
        return "任务道具", None
    if material_type.startswith("MATERIAL_GCG_"):
        return "七圣召唤", material_type.removeprefix("MATERIAL_GCG_")
    if material_type in {"MATERIAL_FURNITURE_FORMULA", "MATERIAL_FURNITURE_SUITE_FORMULA"}:
        return "家具图纸", None
    if material_type == "MATERIAL_WOOD":
        return "木材", None
    if material_type in {"MATERIAL_HOME_SEED", "MATERIAL_FISH_BAIT", "MATERIAL_FISH_ROD"}:
        return "家园/钓鱼", None
    if material_type == "MATERIAL_WIDGET":
        return "小道具/装置", None
    if material_type in {"MATERIAL_RENAME_ITEM", "MATERIAL_CRICKET"}:
        return "小道具/装置", None
    if material_type in {"MATERIAL_CHEST", "MATERIAL_SELECTABLE_CHEST",
                         "MATERIAL_CHEST_BATCH_USE", "MATERIAL_CHEST_BATCH_USE_WITH_GROUP"}:
        return "宝箱/自选包", None
    if material_type == "MATERIAL_PROFILE_PICTURE":
        return "头像", None
    if material_type == "MATERIAL_PROFILE_FRAME":
        return "头像框", None
    if material_type == "MATERIAL_AVATAR_TRACE":
        return "角色培养", None
    if material_type == "MATERIAL_TPS_ACCESSORY":
        return "武器配件", None
    if material_type in {"MATERIAL_BGM", "MATERIAL_PHOTOGRAPH_POSE", "MATERIAL_WEAPON_SKIN"}:
        return "收藏/外观", None
    if material_type == "MATERIAL_NATURALIST_CODEX":
        return "收藏/外观", None
    if material_type in {"MATERIAL_ADSORBATE", "MATERIAL_FAKE_ABSORBATE", "MATERIAL_ELEM_CRYSTAL"}:
        return "元素能量", None
    if row.get("itemType") == "ITEM_VIRTUAL":
        return "货币", None
    if material_type.startswith("MATERIAL_ACTIVITY_") or material_type in {
        "MATERIAL_CHANNELLER_SLAB_BUFF", "MATERIAL_SUMO_BUFF", "MATERIAL_FIREWORKS",
        "MATERIAL_MUSIC_GAME_BOOK_THEME", "MATERIAL_ARANARA", "MATERIAL_DESHRET_MANUAL",
        "MATERIAL_NATLAN_RACE_ALBUM", "MATERIAL_NATLAN_RACE_ENVELOPE"
    }:
        return "活动道具", None
    if material_type in {"MATERIAL_TALENT", "MATERIAL_AVATAR_TALENT_MATERIAL"}:
        return "天赋", None
    if item_id in combine_inputs:
        return "合成原料", None
    if material_type in {"MATERIAL_CONSUME", "MATERIAL_CONSUME_BATCH_USE"}:
        return "小道具/装置", None
    if type_description:
        # Preserve native localized type text when available, without inferring
        # semantics from an unmatched or foreign TextMap hash.
        return "其他", type_description[:80]
    return "其他", None


def base_item(row: dict, kind: str, category: str, text_map: dict[str, str],
              sub_category: str | None = None, reference: dict | None = None) -> dict:
    text_name = localized(text_map, row.get("nameTextMapHash"))
    reference_kind = ("materials" if kind in ("material", "namecard", "costume")
                      else "weapons" if kind == "weapon" else None)
    icon = row.get("icon") or row.get("itemIcon")
    ref_name = (reference_name(reference[reference_kind], row["id"], icon,
                               "weaponIcon" if reference_kind == "weapons" else "icon")
                if reference_kind and reference else None)
    item = {
        "catalogKey": f"{kind}:{row['id']}",
        "id": row["id"],
        "nameTextMapHash": row.get("nameTextMapHash"),
        "nameZh": text_name or ref_name,
        "nameZhSource": ("TextMap/TextMapCHS.json" if text_name else
                         NAME_SOURCES[reference_kind] if ref_name else None),
        "rank": item_rank(row),
        "icon": icon,
        "kind": kind,
        "category": category,
        "subCategory": sub_category,
        "materialType": row.get("materialType"),
        "itemType": row.get("itemType"),
        "grantableItem": kind != "profile_picture",
    }
    desc = localized(text_map, row.get("descTextMapHash"))
    if not desc and reference_kind == "materials" and reference:
        desc = reference_description(reference["materials"], row["id"], icon)
    if desc:
        item["descZh"] = desc[:80]
    return item


def export(resources: Path) -> dict:
    excel = resources / "ExcelBinOutput"
    tables = {name: read_json(excel / f"{name}.json") for name in TABLES}
    text_map = read_json(resources / "TextMap" / "TextMapCHS.json")
    reference = load_reference_names(
        ROOT, "materials", "weapons", "artifacts", "characters")
    uses: dict[int, set[str]] = defaultdict(set)
    for table, label in (("AvatarPromoteExcelConfigData", "avatar"),
                         ("ProudSkillExcelConfigData", "talent"),
                         ("WeaponPromoteExcelConfigData", "weapon")):
        for item_id in costs_used_by(tables[table]):
            uses[item_id].add(label)
    cooking_inputs = {
        cost["id"] for row in tables["CookRecipeExcelConfigData"]
        for cost in row.get("inputVec", []) if cost.get("id")
    }
    cooking_outputs = {
        cost["id"] for row in tables["CookRecipeExcelConfigData"]
        for cost in row.get("qualityOutputVec", []) if cost.get("id")
    }
    combine_inputs = {
        cost["id"] for row in tables["CombineExcelConfigData"]
        for cost in row.get("materialItems", []) if cost.get("id")
    }
    source_by_id = {row["id"]: row for row in tables["MaterialSourceDataExcelConfigData"]}
    set_by_id = {row["setId"]: row for row in tables["ReliquarySetExcelConfigData"]}
    costume_by_item = {
        row["itemId"]: row for row in tables["AvatarCostumeExcelConfigData"]
        if row.get("itemId")
    }

    items: list[dict] = []
    for row in tables["MaterialExcelConfigData"]:
        type_desc = localized(text_map, row.get("typeDescTextMapHash"))
        category, sub_category = classify_material(
            row, uses.get(row["id"], set()), cooking_inputs, cooking_outputs,
            combine_inputs, type_desc)
        if type_desc and not sub_category:
            sub_category = type_desc[:80]
        kind = "material"
        if category == "名片":
            kind = "namecard"
        elif category == "服装":
            kind = "costume"
        item = base_item(row, kind, category, text_map, sub_category, reference)
        if type_desc:
            item["typeDescZh"] = type_desc[:80]
        item["typeDescTextMapHash"] = row.get("typeDescTextMapHash")
        item["uses"] = sorted(uses.get(row["id"], set()))
        source = source_by_id.get(row["id"])
        if source:
            dungeon_ids = [value for value in source.get("dungeonList", []) if value]
            if dungeon_ids:
                item["sourceDungeonIds"] = dungeon_ids
            source_text = [localized(text_map, value) for value in source.get("textList", [])]
            source_text = [value[:80] for value in source_text if value]
            if source_text:
                item["sourceTextZh"] = list(dict.fromkeys(source_text))
        costume = costume_by_item.get(row["id"])
        if costume:
            item["skinId"] = costume["skinId"]
            item["characterId"] = costume.get("characterId")
        items.append(item)

    for table, kind, category in (
        ("WeaponExcelConfigData", "weapon", "武器"),
        ("ReliquaryExcelConfigData", "reliquary", "圣遗物"),
        ("HomeWorldFurnitureExcelConfigData", "furniture", "家具"),
    ):
        for row in tables[table]:
            sub_category = None
            if kind == "weapon":
                sub_category = WEAPON_TYPES.get(row.get("weaponType"))
            elif kind == "reliquary":
                sub_category = RELIC_SLOTS.get(row.get("equipType"))
            item = base_item(row, kind, category, text_map, sub_category, reference)
            if kind == "reliquary":
                set_id = row.get("setId") or row.get("setID")
                item["setId"] = set_id
                if set_id in set_by_id:
                    item["setIcon"] = set_by_id[set_id].get("setIcon")
                if item["nameZh"] is None:
                    item["nameZh"] = reference_artifact_name(
                        reference["artifacts"], set_id, row.get("equipType"), item.get("icon"))
                    if item["nameZh"]:
                        item["nameZhSource"] = NAME_SOURCES["artifacts"]
            items.append(item)

    for row in tables["ProfilePictureExcelConfigData"]:
        profile = dict(row)
        profile["icon"] = row.get("iconPath")
        item = base_item(profile, "profile_picture", "头像", text_map, reference=reference)
        if item["nameZh"] is None:
            item["nameZh"] = reference_character_name(
                reference["characters"], row.get("unlockParam"))
            if item["nameZh"]:
                item["nameZhSource"] = NAME_SOURCES["characters"]
        item["unlockType"] = row.get("NLAPNBMECDF")
        items.append(item)

    items.sort(key=lambda item: (item["id"], item["kind"]))
    keys = [item["catalogKey"] for item in items]
    if len(keys) != len(set(keys)):
        raise ValueError("Duplicate catalogKey in source tables")
    id_counts = Counter(item["id"] for item in items)
    stats = {
        "total": len(items),
        "byKind": dict(sorted(Counter(item["kind"] for item in items).items())),
        "byCategory": dict(sorted(Counter(item["category"] for item in items).items())),
        "uncategorized": sum(item["category"] == "其他" for item in items),
        "missingNameZh": sum(item["nameZh"] is None for item in items),
        "nameZhBySource": dict(sorted(Counter(
            item["nameZhSource"] or "missing" for item in items).items())),
        "referenceNameVersion": reference_version(ROOT),
        "artifactSetSlotNamesResolved": sum(
            item["kind"] == "reliquary"
            and item["nameZhSource"] == NAME_SOURCES["artifacts"] for item in items),
        "profilePictureCharacterNamesResolved": sum(
            item["kind"] == "profile_picture"
            and item["nameZhSource"] == NAME_SOURCES["characters"] for item in items),
        "referenceMaterialIconRejected": sum(
            item["kind"] in ("material", "namecard", "costume")
            and item["nameZh"] is None
            and isinstance(reference["materials"].get(str(item["id"])), dict)
            and bool(item.get("icon"))
            and bool(reference["materials"][str(item["id"])].get("icon"))
            and item["icon"] != reference["materials"][str(item["id"])]["icon"]
            for item in items),
        "missingNameZhByKind": dict(sorted(Counter(
            item["kind"] for item in items if item["nameZh"] is None).items())),
        "materialTypeDescResolved": sum("typeDescZh" in item for item in items),
        "profilePictureIdCollisions": sum(
            item["kind"] == "profile_picture" and id_counts[item["id"]] > 1
            for item in items),
        "nonGrantableProfilePictures": len(tables["ProfilePictureExcelConfigData"]),
        "defaultCostumesWithoutItemIdExcluded": sum(
            not row.get("itemId") for row in tables["AvatarCostumeExcelConfigData"]),
        "costumeItemsMatched": sum(
            item["kind"] == "costume" and "skinId" in item for item in items),
    }
    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "source": [f"ExcelBinOutput/{name}.json" for name in TABLES]
                  + ["TextMap/TextMapCHS.json", NAME_SOURCES["materials"],
                     NAME_SOURCES["weapons"], NAME_SOURCES["artifacts"],
                     NAME_SOURCES["characters"], NAME_SOURCES["version"]],
        "stats": stats,
        "items": items,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--resources", type=Path, default=ROOT / "resources")
    args = parser.parse_args()
    resources = args.resources.resolve()
    result = export(resources)
    target = ROOT / "data" / "admin-catalog" / "items.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", newline="\n", suffix=".tmp",
            prefix="items.", dir=target.parent, delete=False,
        ) as stream:
            temporary = Path(stream.name)
            json.dump(result, stream, ensure_ascii=False, separators=(",", ":"))
            stream.write("\n")
        os.replace(temporary, target)
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()
    print(json.dumps({"output": str(target), "stats": result["stats"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
