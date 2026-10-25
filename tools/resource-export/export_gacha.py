#!/usr/bin/env python3
"""Export AstaPS gacha banner candidates for the admin catalog.

Historical schedules are absent from the bundled ExcelBinOutput. This exporter
therefore records prefab-index candidates with unknown schedule fields left null.
"""
from __future__ import annotations
import argparse
import datetime as dt
import json
import re
from pathlib import Path

from _a6_textnames import SOURCE as NAME_SOURCES, load as load_reference_names
from _a6_textnames import name as reference_name
from _a6_textnames import version as reference_version

DEFAULT_ROOT = Path(__file__).resolve().parents[2]
PREFAB_RE = re.compile(r"^(A\d{3})(?::\s*|\s+)(.*?)\s*$")

def read_json(path: Path):
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--resources", type=Path, help="resources directory (defaults to repository resources/)")
    args = ap.parse_args()
    root = args.resources.resolve().parent if args.resources else DEFAULT_ROOT
    resources = args.resources.resolve() if args.resources else root / "resources"
    data = root / "data"
    out = root / "data" / "admin-catalog" / "gacha.json"
    required = [
        resources / "TextMap" / "TextMapCHS.json",
        resources / "ExcelBinOutput" / "MaterialExcelConfigData.json",
        resources / "ExcelBinOutput" / "ProductGachaTicketDetailConfigData.json",
    ]
    missing = [str(p) for p in required if not p.exists()]
    if missing:
        raise SystemExit("Missing resource files: " + ", ".join(missing))

    # TextMap is the only large input and is loaded exactly once.
    textmap = read_json(required[0])
    def zh(hash_value):
        if hash_value is None:
            return None
        return textmap.get(str(hash_value))

    materials = {r["id"]: r for r in read_json(required[1]) if "id" in r}
    reference = load_reference_names(DEFAULT_ROOT, "materials", "weapons", "characters")
    banner_reference = read_json(DEFAULT_ROOT / NAME_SOURCES["banners"])
    banner_versions = banner_reference.get("version", {})
    def item_name(item_id):
        row = materials.get(item_id, {})
        direct = zh(row.get("nameTextMapHash"))
        if direct:
            return direct, "resources/TextMap/TextMapCHS.json"
        for kind, icon_key in (("materials", "icon"), ("weapons", "weaponIcon"),
                               ("characters", "CardImg")):
            icon = row.get("icon") if kind == "materials" else None
            found = reference_name(reference[kind], item_id, icon, icon_key)
            if found:
                return found, NAME_SOURCES[kind]
        return None, None

    def named_entity(item_id):
        value, source = item_name(item_id)
        return {"id": item_id, "nameZh": value, "nameZhSource": source}
    tickets = read_json(required[2])
    ticket_item_ids = sorted({row.get("materialId") for r in tickets for row in r.get("BHMGCLENMCF", []) if row.get("materialId") is not None})
    prefab_file = data / "GachaBannerPrefab.txt"
    prefab_rows = []
    if prefab_file.exists():
        for line in prefab_file.read_text(encoding="utf-8-sig").splitlines():
            m = PREFAB_RE.match(line)
            if m:
                prefab_rows.append((m.group(1), m.group(2)))

    # Infer only what the prefab label itself explicitly distinguishes.
    def classify(label: str):
        s = label.replace(" ", "")
        if "初行者推荐祈愿" in s:
            return 100, "新手祈愿"
        if "奔行世间" in s:
            return 200, "常驻祈愿"
        if "神铸赋形" in s or "/" in s:
            return 302, "武器活动祈愿（推定）"
        if label:
            return 301, "角色活动祈愿（推定）"
        return None, "无法归类"

    items = []
    for code, label in prefab_rows:
        numeric = int(code[1:])
        gt, type_zh = classify(label)
        item_cost = 224 if gt in (100, 200) else 223 if gt in (301, 302) else None
        items.append({
            "scheduleId": None,
            "gachaId": None,
            "prefabId": code,
            "gachaType": {"id": gt, "nameZh": type_zh} if gt is not None else None,
            "prefabPath": f"GachaShowPanel_{code}",
            "titleZh": None,
            "prefabLabelZh": label or None,
            "beginTime": None,
            "endTime": None,
            "costItemId": item_cost,
            "costItemNameZh": item_name(item_cost)[0] if item_cost else None,
            "costItem": named_entity(item_cost) if item_cost else None,
            "up5": [],
            "up4": [],
            "source": ["data/GachaBannerPrefab.txt (prefab label; gacha type inferred from label)"]
        })

    # Keep the currently shipped server banner visible as a separate configuration record.
    banners_file = root / "src" / "main" / "resources" / "defaults" / "data" / "Banners.json"
    configured = read_json(banners_file) if banners_file.exists() else []
    for b in configured:
        gt = b.get("gachaType")
        type_map = {100: "新手祈愿", 200: "常驻祈愿", 301: "角色活动祈愿", 302: "武器活动祈愿", 400: "角色活动祈愿-2", 500: "集录祈愿"}
        up5 = b.get("rateUpItems5", [])
        up4 = b.get("rateUpItems4", [])
        items.append({
            "scheduleId": b.get("scheduleId"),
            "gachaId": None,
            "gachaType": {"id": gt, "nameZh": type_map.get(gt, "未知")},
            "prefabPath": b.get("prefabPath"),
            "titleZh": None,
            "prefabLabelZh": b.get("comment"),
            "beginTime": b.get("beginTime"),
            "endTime": b.get("endTime"),
            "costItemId": b.get("costItemId"),
            "costItemNameZh": item_name(b.get("costItemId"))[0],
            "costItem": named_entity(b.get("costItemId")),
            "up5": [named_entity(i) for i in up5],
            "up4": [named_entity(i) for i in up4],
            "source": ["src/main/resources/defaults/data/Banners.json"]
        })

    type_counts = {}
    unclassified = 0
    for x in items:
        if x["gachaType"] is None:
            unclassified += 1
        else:
            name = x["gachaType"]["nameZh"]
            type_counts[name] = type_counts.get(name, 0) + 1
    missing_names = sum(x.get("costItemId") is not None and x.get("costItemNameZh") is None for x in items)
    missing_names += sum(1 for x in items for arr in (x.get("up5", []), x.get("up4", [])) for entity in arr if entity.get("nameZh") is None)
    source_files = [
        "data/GachaBannerPrefab.txt",
        "resources/ExcelBinOutput/MaterialExcelConfigData.json",
        "resources/ExcelBinOutput/ProductGachaTicketDetailConfigData.json",
        "src/main/java/emu/grasscutter/game/gacha/GachaBanner.java",
        "resources/TextMap/TextMapCHS.json",
        "src/main/resources/defaults/data/Banners.json",
        NAME_SOURCES["materials"], NAME_SOURCES["weapons"], NAME_SOURCES["characters"],
        NAME_SOURCES["banners"],
        NAME_SOURCES["version"],
    ]
    result = {
        "generatedAt": dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source": source_files,
        "stats": {
            "items": len(items),
            "prefabCandidates": len(prefab_rows),
            "configuredBanners": len(configured),
            "ticketItemIdsFromProductConfig": ticket_item_ids,
            "byType": type_counts,
            "unclassified": unclassified,
            "missingChineseNames": missing_names,
            "referenceNameVersion": reference_version(DEFAULT_ROOT),
            "bannerSnapshotVersionCount": len(banner_versions),
            "bannerSnapshotTitleRecordCount": sum(
                isinstance(row, dict) and bool(row.get("titleZh") or row.get("chsTitle"))
                for row in banner_versions.values()),
            "chineseNamesBySource": dict(sorted({source: sum(
                entity.get("nameZhSource") == source for banner in items
                for entity in ([banner["costItem"]] if banner.get("costItem") else [])
                              + banner.get("up5", []) + banner.get("up4", []))
                for source in ("resources/TextMap/TextMapCHS.json", *(
                    NAME_SOURCES[k] for k in ("materials", "weapons", "characters")))}.items())),
            "unknownScheduleId": sum(x["scheduleId"] is None for x in items),
            "unknownDates": sum(x["beginTime"] is None or x["endTime"] is None for x in items),
            "upListsUnavailable": len(prefab_rows),
        },
        "items": items,
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(out), "stats": result["stats"], "sample": items[-2]}, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()






