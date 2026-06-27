#!/usr/bin/env python3
"""把裸 import net.minecraft.client.renderer.RenderType; 替换为 //? 块。
跳过已在 //? 块内的 import（如 RenderPassAdapter.java）。幂等。"""
import re, sys, pathlib

SRC = pathlib.Path("src/main/java")
DRY = "--dry-run" in sys.argv

OLD = "import net.minecraft.client.renderer.RenderType;\n"
NEW = (
    "//? if <26.1 {\n"
    "import net.minecraft.client.renderer.RenderType;\n"
    "//?} else {\n"
    "import net.minecraft.client.renderer.rendertype.RenderType;\n"
    "//?}\n"
)

changed = []
for f in SRC.rglob("*.java"):
    lines = f.read_text(encoding="utf-8").splitlines(keepends=True)
    modified = False
    for i, line in enumerate(lines):
        if line != OLD:
            continue
        prev = lines[i - 1].strip() if i > 0 else ""
        if prev.startswith("//?") and not prev.startswith("//?}"):
            continue
        lines[i] = NEW
        modified = True
    if modified:
        changed.append(str(f))
        if not DRY:
            f.write_text("".join(lines), encoding="utf-8")

print(f"{'DRY-RUN: ' if DRY else ''}{len(changed)} files:")
for c in changed:
    print(f"  {c}")
