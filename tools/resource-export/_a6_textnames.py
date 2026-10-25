"""Chinese display names from the repository's versioned reference data.

The 7.1.53 Lunaris snapshots are a fallback for hashes missing from the
bundled TextMap. Match native IDs and icon names to avoid a silent ID reuse.
"""

import json
from pathlib import Path


SOURCE = {
    "materials": "docs/features/data/materials.json",
    "weapons": "docs/features/data/weapons.json",
    "artifacts": "docs/features/data/artifacts.json",
    "characters": "docs/features/data/characters.json",
    "banners": "docs/features/data/banners.json",
    "chapters": "docs/features/data/quests.json",
    "version": "docs/features/data/version.json",
}

RELIC_SLOTS_ZH = {
    "EQUIP_BRACER": "生之花",
    "EQUIP_NECKLACE": "死之羽",
    "EQUIP_SHOES": "时之沙",
    "EQUIP_RING": "空之杯",
    "EQUIP_DRESS": "理之冠",
}


def load(root: Path, *kinds: str) -> dict:
    result = {}
    for kind in kinds:
        with (root / SOURCE[kind]).open(encoding="utf-8") as stream:
            raw = json.load(stream)
        result[kind] = {str(row["id"]): row for row in raw} if kind == "chapters" else raw
    return result


def version(root: Path) -> str:
    with (root / SOURCE["version"]).open(encoding="utf-8") as stream:
        return str(json.load(stream)["version"])


def name(rows: dict, item_id: object, icon: str | None = None,
         icon_field: str = "icon") -> str | None:
    row = rows.get(str(item_id)) if item_id is not None else None
    if not isinstance(row, dict):
        return None
    reference_icon = row.get(icon_field)
    if icon and reference_icon and icon != reference_icon:
        return None
    value = row.get("chsName")
    return value if isinstance(value, str) and value.strip() else None


def description(rows: dict, item_id: object, icon: str | None = None) -> str | None:
    row = rows.get(str(item_id)) if item_id is not None else None
    if not isinstance(row, dict):
        return None
    if icon and row.get("icon") and icon != row["icon"]:
        return None
    value = row.get("chsDescription")
    return value if isinstance(value, str) and value.strip() else None


def chapter(rows: dict, chapter_id: object, field: str) -> str | None:
    row = rows.get(str(chapter_id)) if chapter_id is not None else None
    if not isinstance(row, dict):
        return None
    value = row.get(field)
    return value if isinstance(value, str) and value.strip() else None


def character_name(rows: dict, character_id: object) -> str | None:
    return name(rows, character_id)


def artifact_name(rows: dict, set_id: object, equip_type: object,
                  icon: str | None = None) -> str | None:
    # Most rows expose setId. The earliest two rows omit it, but their native
    # relic icon encodes the set ID exactly (UI_RelicIcon_<setId>_<slot>). 
    key = str(set_id) if set_id is not None else None
    if key not in rows and icon:
        import re
        match = re.fullmatch(r"UI_RelicIcon_(\d+)_\d+", icon)
        key = match.group(1) if match else None
    row = rows.get(key) if key is not None else None
    if not isinstance(row, dict):
        return None
    set_name = row.get("chsName")
    slot_name = RELIC_SLOTS_ZH.get(equip_type)
    if not isinstance(set_name, str) or not set_name.strip() or not slot_name:
        return None
    # The snapshot names sets, not individual pieces. Mark the piece slot in
    # the display name so same-set relics remain distinguishable in the catalog.
    return f"{set_name}（{slot_name}）"
