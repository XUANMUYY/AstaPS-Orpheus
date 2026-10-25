#!/usr/bin/env python3
"""只读扫描客户端 AssetBundles 里的卡池 prefab 明文资源名，导出清单、对比上次结果并给出配置建议。

用法（客户端更新后重跑一遍）：
    python tools/resource-export/export_gacha_prefabs.py --client "F:/Fork Reversed/Genshin Impact/ForkClient"

扫描 <client>/YuanShen_Data/StreamingAssets/AssetBundles 与 <client>/YuanShen_Data/Persistent/AssetBundles
下所有 .blk（只读，不启动游戏）。blk 里只有头部是明文，没扫到不等于不存在。

证据类型：P=GachaShowPanel_A###（界面 prefab），T=UI_Tab_GachaShowPanel_A###（页签预览图），
U=UI_Gacha_A###_*（UP 素材）。同一个 blk 里一起出现的 UI_Gacha_AvatarIcon_* 等图标用来推断角色/武器。

输出：
    data/admin-catalog/gacha_prefabs.json   机器可读结果（上一次的结果会被读来做对比）
    docs/卡池prefab清单.md                  标记 GENERATED 之间的正文由本脚本重写
只给建议，不改服务端卡池配置。
"""
from __future__ import annotations

import argparse
import json
import mmap
import re
import sys
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SUBDIRS = [
    ("安装包(Streaming)", "YuanShen_Data/StreamingAssets/AssetBundles"),
    ("热更新(Persistent)", "YuanShen_Data/Persistent/AssetBundles"),
]
PATTERN = re.compile(
    rb"(?:UI_Tab_)?GachaShowPanel_(?:A\d{3}|N\d*|S)(?![0-9A-Za-z])"
    rb"|UI_Gacha_A\d{3}[A-Za-z0-9_]*"
    rb"|UI_Gacha_(?:AvatarIcon|AvatarImg|EquipIcon|EquipImg)_[A-Za-z0-9_]+"
)
NUM = re.compile(r"A(\d{3})")
GROUP_BASE = 280  # A280 起按「角色池、角色池2、武器池」三个一组（A280–A303 在社区表里逐个核对过）
ROLES = ["角色池", "角色池2", "武器池"]
GEN_BEGIN = "<!-- BEGIN GENERATED -->"
GEN_END = "<!-- END GENERATED -->"


def scan_file(path: str):
    """返回该 blk 里命中的全部明文资源名（去重）。"""
    try:
        with open(path, "rb") as f:
            if f.seek(0, 2) == 0:
                return path, []
            with mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ) as mm:
                return path, sorted({m.group(0).decode("ascii") for m in PATTERN.finditer(mm)})
    except OSError as e:
        print(f"跳过 {path}: {e}", file=sys.stderr)
        return path, []


def collect(client: Path, workers: int):
    files = []
    for label, sub in SUBDIRS:
        d = client / sub
        if not d.is_dir():
            print(f"目录不存在，跳过：{d}", file=sys.stderr)
            continue
        files += [(label, str(p)) for p in d.rglob("*.blk")]
    if not files:
        raise SystemExit("没有找到任何 .blk，请检查 --client 路径")
    label_of = dict((p, l) for l, p in files)
    hits = []
    with ProcessPoolExecutor(max_workers=workers) as pool:
        for i, (path, names) in enumerate(pool.map(scan_file, [p for _, p in files], chunksize=8), 1):
            if names:
                hits.append((label_of[path], Path(path).name, names))
            if i % 500 == 0:
                print(f"  已扫描 {i}/{len(files)} 个 blk", file=sys.stderr)
    return len(files), hits


def load_names():
    names = {}
    f = ROOT / "data" / "GachaBannerPrefab.txt"
    if f.exists():
        for line in f.read_text(encoding="utf-8").splitlines():
            m = re.match(r"(A\d{3})\s*[: ]\s*(.*)", line.strip())
            if m:
                names[m.group(1)] = m.group(2).strip()
    icon_cn = {}
    c = ROOT / "docs" / "features" / "data" / "characters.json"
    if c.exists():
        for v in json.loads(c.read_text(encoding="utf-8")).values():
            for k in ("CardImg", "GachaImg"):
                if v.get(k):
                    icon_cn[v[k]] = v.get("chsName", "")
    w = ROOT / "docs" / "features" / "data" / "weapons.json"
    if w.exists():
        for v in json.loads(w.read_text(encoding="utf-8")).values():
            ic = v.get("weaponIcon", "")
            if ic.startswith("UI_EquipIcon_"):
                tail = ic[len("UI_EquipIcon_"):]
                for pre in ("UI_Gacha_EquipIcon_", "UI_Gacha_EquipImg_"):
                    icon_cn[pre + tail] = v.get("chsName", "")
    return names, icon_cn


def build(hits):
    ev = defaultdict(lambda: {"P": set(), "T": set(), "U": set()})
    blk_nums = defaultdict(set)  # (来源, blk) -> 编号
    blk_icons = defaultdict(set)
    other = defaultdict(set)
    for label, blk, names in hits:
        key = f"{label}:{blk}"
        for n in names:
            m = re.fullmatch(r"(UI_Tab_)?GachaShowPanel_(A\d{3})", n)
            if m:
                ev[m.group(2)]["T" if m.group(1) else "P"].add(key)
                blk_nums[key].add(m.group(2))
                continue
            m = re.match(r"UI_Gacha_(A\d{3})", n)
            if m:
                ev[m.group(1)]["U"].add(key)
                blk_nums[key].add(m.group(1))
                continue
            if n.startswith("UI_Gacha_"):
                blk_icons[key].add(n)
                continue
            other[n].add(key)
    return ev, blk_nums, blk_icons, other


def infer(num_id, ev_item, blk_nums, blk_icons, icon_cn, names):
    """按三连号规律和同块图标推断卡池归属。每条都是推断。"""
    n = int(NUM.fullmatch(num_id).group(1))
    out = {"role": None, "group": None, "icons": [], "basis": []}
    if n >= GROUP_BASE:
        i = (n - GROUP_BASE) % 3
        g0 = n - i
        out["role"] = ROLES[i]
        out["group"] = [f"A{g0 + k:03d}" for k in range(3)]
        out["basis"].append(f"A{GROUP_BASE} 起三个一组，本编号是组内第 {i + 1} 个（{ROLES[i]}），同组 {'、'.join(out['group'])}")
    icons = {}
    for key in set().union(*ev_item.values()):
        # 同块里编号越多，图标越不专属，这里只记录，不当作定论
        for ic in blk_icons.get(key, ()):
            icons[ic] = icon_cn.get(ic, "")
    out["icons"] = sorted(f"{k}{'（' + v + '）' if v else ''}" for k, v in icons.items())
    cn = sorted({v for v in icons.values() if v})
    if cn:
        out["basis"].append("同一 blk 里出现图标：" + "、".join(cn) + "（该 blk 可能同时装着多个卡池的资源）")
    if num_id in names:
        out["basis"].append(f"社区表：{names[num_id]}")
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--client", type=Path, required=True, help="客户端目录（含 YuanShen_Data）")
    ap.add_argument("--out", type=Path, default=ROOT / "data" / "admin-catalog" / "gacha_prefabs.json")
    ap.add_argument("--doc", type=Path, default=ROOT / "docs" / "卡池prefab清单.md")
    ap.add_argument("--available", type=Path,
                    default=ROOT / "src" / "main" / "resources" / "defaults" / "data" / "GachaPrefabAvailable.json")
    ap.add_argument("--workers", type=int, default=8)
    ap.add_argument("--no-doc", action="store_true", help="不改文档")
    args = ap.parse_args()

    total_blk, hits = collect(args.client.resolve(), args.workers)
    ev, blk_nums, blk_icons, other = build(hits)
    names, icon_cn = load_names()
    available = set()
    if args.available.exists():
        available = {x.replace("GachaShowPanel_", "") for x in json.loads(args.available.read_text(encoding="utf-8"))}

    items = {}
    for k in sorted(set(ev) | set(names)):
        e = ev.get(k, {"P": set(), "T": set(), "U": set()})
        flags = "".join(x for x in "PTU" if e[x])
        inf = infer(k, e, blk_nums, blk_icons, icon_cn, names)
        if flags:
            sug = "可加入可用名单" if k not in available else "已在可用名单"
            why = "客户端资源里有明文痕迹（" + flags + "），仍建议实机确认一次" if k not in available else ""
        else:
            sug = "先借用能显示的界面"
            why = "客户端资源里没有明文痕迹（武器池界面常见，不等于不存在），实机确认后再加入名单"
        items[k] = {
            "evidence": flags,
            "sources": sorted(set().union(*e.values()))[:6],
            "communityName": names.get(k),
            "inference": inf,
            "suggestion": sug,
            "suggestionReason": why,
            "inAvailableList": k in available,
        }

    result = {
        "client": str(args.client),
        "blkScanned": total_blk,
        "withEvidence": sorted(ev),
        "max": max(ev) if ev else None,
        "other": {k: sorted(v)[:3] for k, v in sorted(other.items())},
        "items": items,
    }

    prev_ids = set()
    if args.out.exists():
        try:
            prev = json.loads(args.out.read_text(encoding="utf-8"))
            prev_ids = set(prev.get("withEvidence", []))
        except Exception:
            pass
    now_ids = set(ev)
    added, gone = sorted(now_ids - prev_ids), sorted(prev_ids - now_ids)
    result["diff"] = {"added": added, "removed": gone, "hadPrevious": bool(prev_ids)}

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    cnt = {x: sum(1 for k in ev if ev[k][x]) for x in "PTU"}
    summary = [
        f"扫描 {total_blk} 个 blk；有证据的卡池编号 {len(ev)} 个（P {cnt['P']}、T {cnt['T']}、U {cnt['U']}），最大 {result['max']}；社区表 {len(names)} 个。",
    ]
    if prev_ids:
        summary.append(f"与上次对比：新增 {len(added)} 个 {added or ''}；消失 {len(gone)} 个 {gone or ''}。")
    else:
        summary.append("没有上次结果，本次作为基线。")
    newest = [k for k in sorted(ev)[-6:]]
    for k in newest:
        inf = items[k]["inference"]
        summary.append(f"  {k}（推断：{inf['role'] or '未知'}）{items[k]['suggestion']}；" + "；".join(inf["basis"][:2]))
    print("\n".join(summary))

    if not args.no_doc:
        write_doc(args.doc, result, items, summary)
        print(f"已写入 {args.out} 和 {args.doc}")
    else:
        print(f"已写入 {args.out}")


def write_doc(path: Path, result, items, summary):
    lines = [GEN_BEGIN, "", "（本段由 `tools/resource-export/export_gacha_prefabs.py` 生成，请勿手改；重跑脚本会覆盖。）", ""]
    lines += ["**本次扫描摘要**：", ""] + [f"- {s.strip()}" for s in summary] + [""]
    lines += ["## 全部编号", "",
              "| 编号 | 社区表中的卡池/UP | 证据 | 推断的归属 | 建议 | 来源 blk（示例） |", "|---|---|---|---|---|---|"]
    for k, v in items.items():
        inf = v["inference"]
        role = f"{inf['role']}（推断）" if inf["role"] else "—"
        lines.append(
            f"| {k} | {v['communityName'] or '（表中无）'} | {v['evidence'] or '—'} | {role} | "
            f"{v['suggestion']} | {'；'.join(v['sources'][:2])} |"
        )
    lines += ["", "## 非 A 系列界面", "", "| 资源名 | 来源 |", "|---|---|"]
    for k, v in result["other"].items():
        lines.append(f"| {k} | {'；'.join(v)} |")
    lines += ["", GEN_END, ""]
    block = "\n".join(lines)
    old = path.read_text(encoding="utf-8") if path.exists() else ""
    if GEN_BEGIN in old and GEN_END in old:
        a, b = old.index(GEN_BEGIN), old.index(GEN_END) + len(GEN_END)
        new = old[:a] + block.rstrip("\n") + old[b:]
    else:
        new = old.rstrip("\n") + "\n\n" + block if old else block
    path.write_text(new, encoding="utf-8")


if __name__ == "__main__":
    main()
