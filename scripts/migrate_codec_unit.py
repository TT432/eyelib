"""
migrate_codec_unit — 把源码里裸用的 Codec.unit( 改为 EyelibCodec.unit(，
并在 import 处补上 EyelibCodec 的 import。

EyelibCodec.unit() 内部已用 //? 条件注释封装了 1.20.1 与 26.1.2 的差异，
因此调用点不需要再加 //? 块。

用法: python scripts/migrate_codec_unit.py [--dry-run]
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

SRC_ROOT = Path("src/main/java")
EXCLUDE = "EyelibCodec.java"
EYELIB_CODEC_IMPORT = "io.github.tt432.eyelib.util.codec.EyelibCodec"
CODEC_IMPORT = "com.mojang.serialization.Codec"

# 负向后视: 排除已经是 EyelibCodec.unit( 的情况
CALL_PATTERN = re.compile(r"(?<!Eyelib)Codec\.unit\(")


def migrate_file(path: Path, dry_run: bool) -> bool:
    text = path.read_text(encoding="utf-8")
    if not CALL_PATTERN.search(text):
        return False

    changed = text
    # 1. 替换调用点
    changed = CALL_PATTERN.sub("EyelibCodec.unit(", changed)

    # 2. 补 import（幂等）
    if f"import {EYELIB_CODEC_IMPORT};" not in changed:
        codec_import_line = f"import {CODEC_IMPORT};"
        if codec_import_line in changed:
            changed = changed.replace(
                codec_import_line,
                f"{codec_import_line}\nimport {EYELIB_CODEC_IMPORT};",
                1,
            )
        else:
            # 没有 Codec import 行（罕见），在 package 行后插入
            pkg_match = re.search(r"(^package [\w.]+;\s*\n)", changed, re.MULTILINE)
            if pkg_match:
                insert_at = pkg_match.end()
                changed = (
                    changed[:insert_at]
                    + f"\nimport {EYELIB_CODEC_IMPORT};\n"
                    + changed[insert_at:]
                )

    if changed == text:
        return False

    if not dry_run:
        path.write_text(changed, encoding="utf-8")
    return True


def main() -> int:
    dry_run = "--dry-run" in sys.argv
    files = [p for p in SRC_ROOT.rglob("*.java") if p.name != EXCLUDE]
    modified: list[str] = []
    for f in files:
        if migrate_file(f, dry_run):
            modified.append(str(f).replace("\\", "/"))

    print(f"{'[DRY-RUN] ' if dry_run else ''}Modified {len(modified)} file(s):")
    for m in sorted(modified):
        print(f"  {m}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
