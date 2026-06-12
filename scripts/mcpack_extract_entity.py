#!/usr/bin/env python3
"""
mcpack_extract_entity.py — 从 .mcpack 中提取单个 client_entity 及所有引用。

用法:
    python3 scripts/mcpack_extract_entity.py <mcpack_path> <entity_id> [-o output.md]

示例:
    python3 scripts/mcpack_extract_entity.py run/resourcepacks/Actions-and-Stuff.mcpack minecraft:slime

依赖:
    - br-ar >= 1.21.124 (https://github.com/Torrekie/br-ar)
    - repomix >= 1.14  (npm install -g repomix)
    - Python 3.10+

支持:
    - Marketplace 包 (__brarchive/*.brarchive)
    - 开发包 (plain JSON)
    - Subpacks (取含 animation_controllers 的最完整层)
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path


# ═══════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════

BRARCHIVE_CLI = "/tmp/usr/bin/brarchive-cli"  # br-ar 解码工具路径


def find_brarchive_cli():
    """查找 brarchive-cli，先检查配置路径，再搜 PATH。"""
    if os.path.isfile(BRARCHIVE_CLI):
        return BRARCHIVE_CLI
    for name in ["brarchive-cli", "br-ar"]:
        p = shutil.which(name)
        if p:
            return p
    return None


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


def decode_brarchive(archive_path, output_dir):
    """解码单个 .brarchive 文件。"""
    cli = find_brarchive_cli()
    if not cli:
        raise RuntimeError("br-ar 未安装。请从 https://github.com/Torrekie/br-ar/releases 下载。")
    os.makedirs(output_dir, exist_ok=True)
    run([cli, "decode", str(archive_path), str(output_dir)])


def find_entity_file(entity_dir, entity_id):
    """在 entity JSON 目录中搜索目标 entity_id。
    
    返回 (file_path, description_dict) 或 None。
    """
    # 尝试直接文件名匹配（开发包）
    direct = Path(entity_dir) / f"{entity_id.replace(':', '_')}.json"
    if direct.is_file():
        return direct

    # brarchive 后文件名是 hash 的，需要 grep
    for f in Path(entity_dir).glob("*.json"):
        try:
            data = json.loads(f.read_text())
            ce = data.get("minecraft:client_entity", {})
            desc = ce.get("description", {})
            if desc.get("identifier") == entity_id:
                return f
        except (json.JSONDecodeError, KeyError):
            continue
    return None


# ═══════════════════════════════════════════════════════════════
# 引用提取
# ═══════════════════════════════════════════════════════════════

def extract_refs_from_entity(entity_json_path):
    """从 client_entity JSON 中提取所有引用 ID。
    
    返回:
        refs: {
            'animations': [full_id, ...],       # "animation.xxx.yyy"
            'animation_controllers': [full_id, ...],  # "controller.animation.xxx.yyy"
            'render_controllers': [full_id, ...],
            'particles': [full_id, ...],
            'sounds': [full_id, ...],
            'geometry': [full_id, ...],
            'textures': [path, ...],
            'materials': [name, ...],
        }
        anim_map: {short_name: full_id}  # 用于从 AC short names 反查
    """
    data = json.loads(Path(entity_json_path).read_text())
    ce = data["minecraft:client_entity"]["description"]

    refs = {
        "animations": [],
        "animation_controllers": [],
        "render_controllers": [],
        "particles": [],
        "sounds": [],
        "geometry": [],
        "textures": [],
        "materials": [],
    }

    anim_map = ce.get("animations", {})
    for short_name, full_id in anim_map.items():
        if full_id.startswith("controller."):
            refs["animation_controllers"].append(full_id)
        else:
            refs["animations"].append(full_id)

    # render_controllers
    for rc in ce.get("render_controllers", []):
        if isinstance(rc, str):
            refs["render_controllers"].append(rc)
        elif isinstance(rc, dict):
            refs["render_controllers"].extend(rc.keys())

    # geometry
    refs["geometry"].extend(
        v if isinstance(v, str) else str(v)
        for v in ce.get("geometry", {}).values()
    )

    # textures
    refs["textures"].extend(
        v if isinstance(v, str) else str(v)
        for v in ce.get("textures", {}).values()
    )

    # materials
    refs["materials"].extend(ce.get("materials", {}).values())

    # particles
    for section in ["particle_effects", "particle_emitters"]:
        refs["particles"].extend(
            v for v in ce.get(section, {}).values()
        )

    # sounds
    refs["sounds"].extend(
        v for v in ce.get("sound_effects", {}).values()
    )

    return refs, anim_map


def resolve_ac_animations(ac_dir, ac_ids, anim_map, entity_anims):
    """从 AC 文件中递归提取被引用的 animation short names，
    通过 anim_map 展开为 full_id，加回 entity_anims 集合。
    """
    extra_anims = set()
    for f in Path(ac_dir).glob("*.json"):
        data = json.loads(f.read_text())
        controllers = data.get("animation_controllers", {})
        for cid, cdata in controllers.items():
            if cid not in ac_ids:
                continue
            for sdata in cdata.get("states", {}).values():
                for a in sdata.get("animations", []):
                    short = a if isinstance(a, str) else next(iter(a))
                    full = anim_map.get(short, short)
                    if full not in entity_anims:
                        extra_anims.add(full)
    return entity_anims | extra_anims


# ═══════════════════════════════════════════════════════════════
# JSON 字段级裁剪
# ═══════════════════════════════════════════════════════════════

def filter_json_file(input_pattern, target_ids, key_name, format_version, output_path):
    """从一组 JSON 文件中提取匹配 target_ids 的条目。
    
    Args:
        input_pattern: glob 模式或单文件路径
        target_ids: set of full IDs to keep
        key_name: 顶层 key 名 (如 'animations', 'animation_controllers')
        format_version: 输出的 format_version 值
        output_path: 输出路径
    """
    result = {}
    pattern = Path(input_pattern)
    if "*" in str(input_pattern):
        files = list(pattern.parent.glob(pattern.name)) if pattern.is_absolute() else list(Path().glob(str(pattern)))
    else:
        files = [pattern]
    for f in files:
        if not f.is_file():
            continue
        data = json.loads(f.read_text())
        for k, v in data.get(key_name, {}).items():
            if k in target_ids:
                result[k] = v

    if result:
        out = {"format_version": format_version, key_name: result}
        Path(output_path).write_text(json.dumps(out, indent=2, ensure_ascii=False))
        return len(result)
    return 0


def find_brarchive_path(pack_root, tmpdir_root, br_name):
    """在 pack_root 和 tmpdir_root(根包) 之间查找 .brarchive 文件。
    优先 pack_root，不存在或不完整时回退到 root。
    """
    archive = pack_root / "__brarchive" / br_name
    if archive.is_file() and archive.stat().st_size > 100:
        return archive
    fallback = tmpdir_root / "__brarchive" / br_name
    if fallback.is_file():
        return fallback
    return None


# ═══════════════════════════════════════════════════════════════
# 主流程
# ═══════════════════════════════════════════════════════════════

def main():
    parser = argparse.ArgumentParser(
        description="从 .mcpack 中提取单个 client_entity 及所有引用，输出为单一 markdown"
    )
    parser.add_argument("mcpack", help=".mcpack 文件路径")
    parser.add_argument("entity_id", help="entity identifier，如 minecraft:slime")
    parser.add_argument("-o", "--output", default=None,
                        help="输出 markdown 路径（默认: <entity_id>.md）")
    parser.add_argument("--subpack", default=None,
                        help="手动指定子包 (如 SP0, SP1, SP2) 或 'root'；默认自动选择含 animation_controllers 的最完整层")
    parser.add_argument("--keep-temp", action="store_true",
                        help="保留临时文件")
    args = parser.parse_args()

    mcpack_path = Path(args.mcpack).resolve()
    entity_id = args.entity_id
    output = Path(args.output) if args.output else Path(f"{entity_id.replace(':', '_')}.md")
    output = output.resolve()

    if not mcpack_path.is_file():
        print(f"❌ 文件不存在: {mcpack_path}", file=sys.stderr)
        sys.exit(1)

    # ── 1. 解包 ──
    tmpdir = Path(tempfile.mkdtemp(prefix="mcpack-extract-"))
    print(f"📦 解压 {mcpack_path.name} → {tmpdir}")

    with zipfile.ZipFile(mcpack_path) as zf:
        zf.extractall(tmpdir)

    # ── 2. 读取 manifest，确定子包 ──
    manifest = json.loads((tmpdir / "manifest.json").read_text())
    print(f"   包名: {manifest['header']['name']}")
    print(f"   版本: {manifest['header']['version']}")

    # 选子包
    pack_root = tmpdir
    subpacks = manifest.get("subpacks", [])

    if args.subpack:
        if args.subpack.lower() == "root":
            pack_root = tmpdir
            print(f"   子包: root (手动指定)")
        else:
            sp_dir = tmpdir / "subpacks" / args.subpack
            if sp_dir.is_dir():
                pack_root = sp_dir
                print(f"   子包: {args.subpack} (手动指定)")
            else:
                print(f"⚠ 子包 {args.subpack} 不存在，使用 root")
    elif subpacks:
        # 优先选含 __brarchive/animation_controllers.brarchive 的子包
        for sp in subpacks:
            sp_dir = tmpdir / "subpacks" / sp["folder_name"]
            if (sp_dir / "__brarchive" / "animation_controllers.brarchive").is_file():
                pack_root = sp_dir
                print(f"   子包: {sp['folder_name']} ({sp.get('name', '')})")
                break
        else:
            # fallback: 用 root
            print(f"   子包: 无动画层，使用 root")

    # ── 3. 定位 entity ──
    if has_brarchive(pack_root):
        print("🔓 解码 brarchive...")
        entity_dir = tmpdir / "_extracted_entity"
        decode_brarchive(pack_root / "__brarchive" / "entity.brarchive", entity_dir)
    else:
        entity_dir = pack_root / "entity"

    entity_file = find_entity_file(entity_dir, entity_id)
    if not entity_file:
        print(f"❌ 未找到 entity: {entity_id}", file=sys.stderr)
        sys.exit(1)
    print(f"🎯 找到 entity: {entity_file.name} → {entity_id}")

    # ── 4. 提取引用 ──
    refs, anim_map = extract_refs_from_entity(entity_file)
    print(f"   动画: {len(refs['animations'])}")
    print(f"   动画控制器: {len(refs['animation_controllers'])}")
    print(f"   渲染控制器: {len(refs['render_controllers'])}")
    print(f"   粒子: {len(refs['particles'])}")
    print(f"   声音: {len(refs['sounds'])}")

    # ── 5. 递归解析 AC 中的动画引用 ──
    if refs["animation_controllers"]:
        ac_archive = find_brarchive_path(pack_root, tmpdir, "animation_controllers.brarchive")
        if ac_archive:
            ac_dir = tmpdir / "_extracted_ac"
            decode_brarchive(ac_archive, ac_dir)
            all_anims = resolve_ac_animations(
                ac_dir, set(refs["animation_controllers"]), anim_map, set(refs["animations"])
            )
            refs["animations"] = sorted(all_anims)
            print(f"   AC 递归后动画: {len(refs['animations'])}")

    # ── 6. 解码并裁剪各 brarchive ──
    collected = tmpdir / "collected"
    collected.mkdir(exist_ok=True)

    # entity — pretty-print 以便子代理可读
    entity_data = json.loads(Path(entity_file).read_text())
    Path(collected / "entity.json").write_text(json.dumps(entity_data, indent=2, ensure_ascii=False))
    # manifest
    shutil.copy(tmpdir / "manifest.json", collected / "manifest.json")

    brarchive_map = {
        "animations": ("animations.brarchive", "animations", "1.8.0", "animations.json"),
        "animation_controllers": ("animation_controllers.brarchive", "animation_controllers", "1.19.60", "animation_controllers.json"),
        "render_controllers": ("render_controllers.brarchive", "render_controllers", "1.10.0", "render_controllers.json"),
    }

    for ref_key, (br_name, key_name, fmt_ver, out_name) in brarchive_map.items():
        ids = set(refs[ref_key])
        if not ids:
            continue

        # 先尝试子包，再回退到 root
        count = 0
        for source_root in [pack_root, tmpdir]:  # pack_root = 子包, tmpdir = 根包
            archive = source_root / "__brarchive" / br_name
            if not archive.is_file() or archive.stat().st_size <= 100:
                continue
            extract_dir = tmpdir / f"_extracted_{ref_key}"
            if extract_dir.exists():
                shutil.rmtree(extract_dir)
            decode_brarchive(archive, extract_dir)
            count += filter_json_file(
                str(extract_dir / "*.json"), ids, key_name, fmt_ver,
                collected / out_name
            )
            if count >= len(ids):
                break  # 全部找到，不需要继续

        if count > 0:
            print(f"   裁剪 {br_name}: {count}/{len(ids)} 匹配")
        else:
            # 开发包：直接读目录
            src_dir = pack_root / ref_key.rstrip("s")
            if src_dir.is_dir():
                count = filter_json_file(
                    str(src_dir / "*.json"), ids, key_name, fmt_ver,
                    collected / out_name
                )
                print(f"   直接读取 {src_dir.name}/: {count}/{len(ids)} 匹配")
            else:
                print(f"   ⚠ {br_name}: 未找到匹配 ({len(ids)} 个 ID)")

    # particles
    if refs["particles"]:
        archive = find_brarchive_path(pack_root, tmpdir, "particles.brarchive")
        if archive:
            extract_dir = tmpdir / "_extracted_particles"
            decode_brarchive(archive, extract_dir)
            count = filter_json_file(
                str(extract_dir / "*.json"), set(refs["particles"]),
                "particle_effects", "1.10.0", collected / "particles.json"
            )
            print(f"   裁剪 particles.brarchive: {count}")

    # models / geometry — 注意: 实体几何在 models/entity.brarchive，不是 models.brarchive
    if refs["geometry"]:
        count = 0
        result = {}
        for br_file in ["models/entity.brarchive", "models.brarchive"]:
            for source_root in [pack_root, tmpdir]:
                archive = source_root / "__brarchive" / br_file
                if not archive.is_file() or archive.stat().st_size <= 100:
                    continue
                extract_dir = tmpdir / "_extracted_models"
                if extract_dir.exists():
                    shutil.rmtree(extract_dir)
                decode_brarchive(archive, extract_dir)
                for f in Path(extract_dir).glob("*.json"):
                    data = json.loads(f.read_text())
                    # 支持两种格式: {"minecraft:geometry": [...]} 和 {"格式版本": [...]}
                    for k, v in data.items():
                        if isinstance(v, list):
                            for geom in v:
                                gid = geom.get("description", {}).get("identifier", "")
                                if gid in set(refs["geometry"]):
                                    result.setdefault(k, []).append(geom)
                                    count += 1
                if count >= len(refs["geometry"]):
                    break
            if count >= len(refs["geometry"]):
                break

        if result:
            Path(collected / "geometry.json").write_text(
                json.dumps(result, indent=2, ensure_ascii=False)
            )
        print(f"   裁剪 geometry: {count}/{len(refs['geometry'])} 匹配")

    # materials
    mat_archive = find_brarchive_path(pack_root, tmpdir, "materials.brarchive")
    if mat_archive:
        mat_dir = tmpdir / "_extracted_materials"
        decode_brarchive(mat_archive, mat_dir)
        for f in Path(mat_dir).glob("*.json"):
            shutil.copy(f, collected / f.name)
        print(f"   材质: {len(list(Path(mat_dir).glob('*.json')))} 个文件")
    else:
        mat_dir = pack_root / "materials"
        if mat_dir.is_dir():
            for f in mat_dir.glob("*.json"):
                shutil.copy(f, collected / f"material_{f.name}")
            print(f"   材质: 已复制")

    # ── 7. repomix 打包 ──
    print(f"\n📝 repomix 打包...")
    run(["repomix", "--style", "markdown", "--output", str(output),
         "--include", "**", "--no-default-patterns", str(collected)])

    # ── 8. 统计 ──
    size_kb = output.stat().st_size / 1024
    print(f"\n✅ 完成!")
    print(f"   输出: {output}")
    print(f"   大小: {size_kb:.0f} KB")
    print(f"   文件: {len(list(collected.iterdir()))} 个")

    # ── 清理 ──
    if not args.keep_temp:
        shutil.rmtree(tmpdir)
        print(f"   临时文件已清理")


if __name__ == "__main__":
    main()
