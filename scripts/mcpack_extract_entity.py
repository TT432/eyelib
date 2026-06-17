#!/usr/bin/env python3
"""
mcpack_extract_entity.py — 从 .mcpack 中提取单个 client_entity 及所有引用。

用法:
    python3 scripts/mcpack_extract_entity.py <mcpack_path> <entity_id> [-o output.md]
    python3 scripts/mcpack_extract_entity.py <mcpack_path> <entity_id> --raw  # 只输出 entity JSON 到 stdout

示例:
    python3 scripts/mcpack_extract_entity.py run/resourcepacks/Actions-and-Stuff.mcpack minecraft:creeper
    python3 scripts/mcpack_extract_entity.py run/resourcepacks/Actions-and-Stuff.mcpack minecraft:warden --raw

依赖:
    - Python 3.10+（brarchive 解码为纯 Python 实现，对齐 Java BrArchiveDecoder）
    - repomix >= 1.14  (npm install -g repomix)  — 仅完整模式需要

支持:
    - Marketplace 包 (__brarchive/*.brarchive) — 纯 Python 解码
    - 开发包 (plain JSON)
    - Subpacks (取含 animation_controllers 的最完整层)
"""

import argparse
import io
import json
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path


# ═══════════════════════════════════════════════════════════════
# brarchive 纯 Python 解码（对齐 BrArchiveDecoder.java）
# ═══════════════════════════════════════════════════════════════

BRARCHIVE_MAGIC = 0x267052A0B125277D
BRARCHIVE_HEADER_SIZE = 16  # 8B magic + 4B entryCount + 4B version
BRARCHIVE_RECORD_SIZE = 256


def decode_brarchive(archive_path):
    """解码 .brarchive 文件，返回 {entry_name: json_str} 字典。

    格式对齐 BrArchiveDecoder.java：
      8B magic(LE) + 4B entryCount(LE) + 4B version(LE)
      + entryCount * 256B entry records
      + 合并 JSON body

    entry record 第 1 字节是 nameLen，其后是 UTF-8 name。
    JSON body 是多个 JSON 对象直接拼接。
    """
    data = Path(archive_path).read_bytes()
    if len(data) < BRARCHIVE_HEADER_SIZE:
        raise ValueError(f"brarchive too short: {archive_path} ({len(data)} bytes)")

    magic, entry_count, version = struct.unpack_from("<QII", data, 0)
    if magic != BRARCHIVE_MAGIC:
        raise ValueError(f"Invalid brarchive magic: 0x{magic:X}")

    content_start = BRARCHIVE_HEADER_SIZE + entry_count * BRARCHIVE_RECORD_SIZE
    if content_start >= len(data):
        return {}  # 空内容

    # 提取 entry names（用于调试，实际解析靠 JSON body）
    entries = []
    for i in range(entry_count):
        rec_off = BRARCHIVE_HEADER_SIZE + i * BRARCHIVE_RECORD_SIZE
        name_len = data[rec_off]
        if name_len > 0:
            name = data[rec_off + 1: rec_off + 1 + min(name_len, len(data) - rec_off - 1)].decode("utf-8", errors="replace")
        else:
            name = ""
        entries.append(name)

    json_body = data[content_start:].decode("utf-8", errors="replace")

    # JSON body 是多个顶层对象拼接，用流式解析拆分
    return _parse_concatenated_json(json_body)


def _parse_concatenated_json(text):
    """解析直接拼接的多个顶层 JSON 对象。

    用 raw decoder 逐个解析，对齐 Java 的拼接格式。
    """
    decoder = json.JSONDecoder()
    result = {}
    idx = 0
    text_len = len(text)
    while idx < text_len:
        # 跳过空白
        while idx < text_len and text[idx] in " \t\r\n":
            idx += 1
        if idx >= text_len:
            break
        try:
            obj, end = decoder.raw_decode(text, idx)
        except json.JSONDecodeError:
            # 尝试跳过非 JSON 字符
            idx += 1
            continue
        if isinstance(obj, dict):
            # 判断这是什么类型的对象，提取 key
            _index_object(obj, result)
        idx = end
    return result


def _index_object(obj, result):
    """识别对象类型并索引到 result。

    Bedrock client_entity 格式: {"minecraft:client_entity": {"description": {"identifier": "minecraft:xxx", ...}}}
    动画格式: {"format_version": "...", "animations": {"id": {...}}}
    AC 格式: {"format_version": "...", "animation_controllers": {"id": {...}}}
    RC 格式: {"format_version": "...", "render_controllers": {"id": {...}}}
    几何格式: {"minecraft:geometry": [{"description": {"identifier": "..."}}]}
    """
    # client_entity
    if "minecraft:client_entity" in obj:
        desc = obj["minecraft:client_entity"].get("description", {})
        ident = desc.get("identifier")
        if ident:
            result.setdefault("_entities", {})[ident] = obj
        return

    # geometry（数组形式）
    if "minecraft:geometry" in obj:
        for geom in obj["minecraft:geometry"]:
            gid = geom.get("description", {}).get("identifier")
            if gid:
                result.setdefault("_geometry", {})[gid] = geom
        return

    # format_version 系（animations / animation_controllers / render_controllers / particles）
    for top_key in ("animations", "animation_controllers", "render_controllers", "particle_effects", "particles"):
        if top_key in obj and isinstance(obj[top_key], dict):
            bucket = "_" + top_key
            for kid, v in obj[top_key].items():
                result.setdefault(bucket, {})[kid] = v
            return

    # 兜底：无法识别的对象，按可能的 identifier 字段索引
    ident = obj.get("identifier") or obj.get("description", {}).get("identifier") if isinstance(obj.get("description"), dict) else None
    if ident:
        result.setdefault("_unknown", {})[ident] = obj


# ═══════════════════════════════════════════════════════════════
# 工具函数
# ═══════════════════════════════════════════════════════════════

def run(cmd, **kwargs):
    """运行命令，失败抛异常。"""
    result = subprocess.run(cmd, capture_output=True, text=True, **kwargs)
    if result.returncode != 0:
        raise RuntimeError(f"命令失败: {' '.join(cmd)}\n{result.stderr}")
    return result.stdout


def has_brarchive(mcpack_dir):
    """判断包是否使用 Marketplace brarchive 格式。"""
    return (Path(mcpack_dir) / "__brarchive").is_dir()


# ═══════════════════════════════════════════════════════════════
# 引用提取（从 client_entity JSON）
# ═══════════════════════════════════════════════════════════════

def extract_refs_from_entity(entity_json):
    """从 client_entity JSON 中提取所有引用 ID。

    返回:
        refs: {animations, animation_controllers, render_controllers, particles, sounds, geometry, textures, materials}
        anim_map: {short_name: full_id}
    """
    ce = entity_json["minecraft:client_entity"]["description"]

    refs = {
        "animations": [], "animation_controllers": [], "render_controllers": [],
        "particles": [], "sounds": [], "geometry": [], "textures": [], "materials": [],
    }

    anim_map = ce.get("animations", {})
    for short_name, full_id in anim_map.items():
        if full_id.startswith("controller."):
            refs["animation_controllers"].append(full_id)
        else:
            refs["animations"].append(full_id)

    for rc in ce.get("render_controllers", []):
        if isinstance(rc, str):
            refs["render_controllers"].append(rc)
        elif isinstance(rc, dict):
            refs["render_controllers"].extend(rc.keys())

    refs["geometry"].extend(
        v if isinstance(v, str) else str(v)
        for v in ce.get("geometry", {}).values()
    )
    refs["textures"].extend(
        v if isinstance(v, str) else str(v)
        for v in ce.get("textures", {}).values()
    )
    refs["materials"].extend(ce.get("materials", {}).values())

    for section in ["particle_effects", "particle_emitters"]:
        refs["particles"].extend(v for v in ce.get(section, {}).values())
    refs["sounds"].extend(v for v in ce.get("sound_effects", {}).values())

    return refs, anim_map


def resolve_ac_animations(ac_indexed, ac_ids, anim_map, entity_anims):
    """从已解码的 AC 中递归提取被引用的 animation short names，展开为 full_id。"""
    extra = set()
    for cid in ac_ids:
        cdata = ac_indexed.get(cid)
        if not cdata:
            continue
        for sdata in cdata.get("states", {}).values():
            for a in sdata.get("animations", []):
                short = a if isinstance(a, str) else next(iter(a))
                full = anim_map.get(short, short)
                if full not in entity_anims:
                    extra.add(full)
    return entity_anims | extra


# ═══════════════════════════════════════════════════════════════
# 主流程
# ═══════════════════════════════════════════════════════════════

def main():
    parser = argparse.ArgumentParser(
        description="从 .mcpack 中提取单个 client_entity 及所有引用"
    )
    parser.add_argument("mcpack", help=".mcpack 文件路径")
    parser.add_argument("entity_id", help="entity identifier，如 minecraft:creeper")
    parser.add_argument("-o", "--output", default=None,
                        help="输出 markdown 路径（默认: <entity_id>.md）")
    parser.add_argument("--subpack", default=None,
                        help="手动指定子包 (SP0/SP1/SP2/root)；默认自动选择含 animation_controllers 的最完整层")
    parser.add_argument("--keep-temp", action="store_true", help="保留临时文件")
    parser.add_argument("--raw", action="store_true",
                        help="只输出 client_entity JSON 到 stdout（不做引用裁剪、不调 repomix）")
    args = parser.parse_args()

    mcpack_path = Path(args.mcpack).resolve()
    entity_id = args.entity_id

    if not mcpack_path.is_file():
        print(f"ERROR: 文件不存在: {mcpack_path}", file=sys.stderr)
        sys.exit(1)

    # ── 1. 解包 ──
    tmpdir = Path(tempfile.mkdtemp(prefix="mcpack-extract-"))
    print(f"解压 {mcpack_path.name} -> {tmpdir}", file=sys.stderr)

    with zipfile.ZipFile(mcpack_path) as zf:
        zf.extractall(tmpdir)

    # ── 2. 读取 manifest，确定子包 ──
    manifest = json.loads((tmpdir / "manifest.json").read_text())
    print(f"   包名: {manifest['header']['name']}", file=sys.stderr)

    pack_root = tmpdir
    subpacks = manifest.get("subpacks", [])

    if args.subpack:
        if args.subpack.lower() == "root":
            pack_root = tmpdir
            print(f"   子包: root (手动)", file=sys.stderr)
        else:
            sp_dir = tmpdir / "subpacks" / args.subpack
            if sp_dir.is_dir():
                pack_root = sp_dir
                print(f"   子包: {args.subpack} (手动)", file=sys.stderr)
            else:
                print(f"   WARN 子包 {args.subpack} 不存在，使用 root", file=sys.stderr)
    elif subpacks:
        for sp in subpacks:
            sp_dir = tmpdir / "subpacks" / sp["folder_name"]
            if (sp_dir / "__brarchive" / "animation_controllers.brarchive").is_file():
                pack_root = sp_dir
                print(f"   子包: {sp['folder_name']} ({sp.get('name', '')})", file=sys.stderr)
                break
        else:
            print(f"   子包: 无动画层，使用 root", file=sys.stderr)

    # ── 3. 解码 entity ──
    entity_indexed = {}  # {identifier: json_obj}

    if has_brarchive(pack_root):
        entity_br = pack_root / "__brarchive" / "entity.brarchive"
        if not entity_br.is_file():
            # fallback root
            entity_br = tmpdir / "__brarchive" / "entity.brarchive"
        if entity_br.is_file():
            print(f"解码 {entity_br.name}...", file=sys.stderr)
            decoded = decode_brarchive(entity_br)
            entity_indexed = decoded.get("_entities", {})
    else:
        # 开发包：读 entity/*.json
        entity_dir = pack_root / "entity"
        for f in entity_dir.glob("*.json"):
            data = json.loads(f.read_text())
            ce = data.get("minecraft:client_entity", {})
            ident = ce.get("description", {}).get("identifier")
            if ident:
                entity_indexed[ident] = data

    if entity_id not in entity_indexed:
        print(f"ERROR: 未找到 entity: {entity_id}", file=sys.stderr)
        print(f"   已知实体 ({len(entity_indexed)}): {', '.join(sorted(entity_indexed)[:20])}...", file=sys.stderr)
        sys.exit(1)

    entity_json = entity_indexed[entity_id]
    print(f"找到 entity: {entity_id}", file=sys.stderr)

    # ── raw 模式：直接输出 ──
    if args.raw:
        print(json.dumps(entity_json, indent=2, ensure_ascii=False))
        if not args.keep_temp:
            shutil.rmtree(tmpdir)
        return

    # ── 4. 提取引用 ──
    refs, anim_map = extract_refs_from_entity(entity_json)
    print(f"   动画: {len(refs['animations'])}", file=sys.stderr)
    print(f"   动画控制器: {len(refs['animation_controllers'])}", file=sys.stderr)
    print(f"   渲染控制器: {len(refs['render_controllers'])}", file=sys.stderr)
    print(f"   粒子: {len(refs['particles'])}", file=sys.stderr)

    # ── 5. 递归解析 AC 中的动画引用 ──
    ac_indexed = {}
    if refs["animation_controllers"]:
        ac_br = pack_root / "__brarchive" / "animation_controllers.brarchive"
        if not ac_br.is_file():
            ac_br = tmpdir / "__brarchive" / "animation_controllers.brarchive"
        if ac_br.is_file():
            ac_decoded = decode_brarchive(ac_br)
            ac_indexed = ac_decoded.get("_animation_controllers", {})
            all_anims = resolve_ac_animations(
                ac_indexed, set(refs["animation_controllers"]), anim_map, set(refs["animations"])
            )
            refs["animations"] = sorted(all_anims)
            print(f"   AC 递归后动画: {len(refs['animations'])}", file=sys.stderr)

    # ── 6. 解码并裁剪各 brarchive ──
    collected = tmpdir / "collected"
    collected.mkdir(exist_ok=True)

    Path(collected / "entity.json").write_text(json.dumps(entity_json, indent=2, ensure_ascii=False))
    shutil.copy(tmpdir / "manifest.json", collected / "manifest.json")

    def decode_br_safe(br_name):
        """优先子包，回退 root，返回 _bucket 字典。"""
        for root in [pack_root, tmpdir]:
            p = root / "__brarchive" / br_name
            if p.is_file() and p.stat().st_size > 100:
                return decode_brarchive(p)
        return {}

    # render_controllers
    if refs["render_controllers"]:
        rc_decoded = decode_br_safe("render_controllers.brarchive")
        rc_bucket = rc_decoded.get("_render_controllers", {})
        rc_ids = set(refs["render_controllers"])
        filtered = {k: v for k, v in rc_bucket.items() if k in rc_ids}
        if filtered:
            Path(collected / "render_controllers.json").write_text(
                json.dumps({"format_version": "1.10.0", "render_controllers": filtered}, indent=2, ensure_ascii=False))
            print(f"   render_controllers: {len(filtered)}/{len(rc_ids)}", file=sys.stderr)

    # animation_controllers
    if ac_indexed:
        ac_ids = set(refs["animation_controllers"])
        filtered = {k: v for k, v in ac_indexed.items() if k in ac_ids}
        if filtered:
            Path(collected / "animation_controllers.json").write_text(
                json.dumps({"format_version": "1.19.60", "animation_controllers": filtered}, indent=2, ensure_ascii=False))
            print(f"   animation_controllers: {len(filtered)}/{len(ac_ids)}", file=sys.stderr)

    # animations
    if refs["animations"]:
        anim_decoded = decode_br_safe("animations.brarchive")
        anim_bucket = anim_decoded.get("_animations", {})
        anim_ids = set(refs["animations"])
        filtered = {k: v for k, v in anim_bucket.items() if k in anim_ids}
        if filtered:
            Path(collected / "animations.json").write_text(
                json.dumps({"format_version": "1.8.0", "animations": filtered}, indent=2, ensure_ascii=False))
            print(f"   animations: {len(filtered)}/{len(anim_ids)}", file=sys.stderr)

    # geometry
    if refs["geometry"]:
        geo_ids = set(refs["geometry"])
        geo_result = []
        for br_file in ["models/entity.brarchive", "models.brarchive"]:
            decoded = decode_br_safe(br_file)
            geo_bucket = decoded.get("_geometry", {})
            for gid in geo_ids:
                if gid in geo_bucket and geo_bucket[gid] not in geo_result:
                    geo_result.append(geo_bucket[gid])
            if len(geo_result) >= len(geo_ids):
                break
        if geo_result:
            Path(collected / "geometry.json").write_text(
                json.dumps({"minecraft:geometry": geo_result}, indent=2, ensure_ascii=False))
            print(f"   geometry: {len(geo_result)}/{len(geo_ids)}", file=sys.stderr)

    # particles
    if refs["particles"]:
        pt_decoded = decode_br_safe("particles.brarchive")
        pt_bucket = pt_decoded.get("_particle_effects", {}) or pt_decoded.get("_particles", {})
        pt_ids = set(refs["particles"])
        filtered = {k: v for k, v in pt_bucket.items() if k in pt_ids}
        if filtered:
            Path(collected / "particles.json").write_text(
                json.dumps({"format_version": "1.10.0", "particle_effects": filtered}, indent=2, ensure_ascii=False))
            print(f"   particles: {len(filtered)}/{len(pt_ids)}", file=sys.stderr)

    # ── 7. repomix 打包 ──
    output = Path(args.output).resolve() if args.output else Path(f"{entity_id.replace(':', '_')}.md").resolve()
    print(f"\nrepomix 打包...", file=sys.stderr)
    run(["repomix", "--style", "markdown", "--output", str(output),
         "--include", "**", "--no-default-patterns", str(collected)])

    size_kb = output.stat().st_size / 1024
    print(f"\n完成! 输出: {output} ({size_kb:.0f} KB)", file=sys.stderr)

    if not args.keep_temp:
        shutil.rmtree(tmpdir)


if __name__ == "__main__":
    main()
