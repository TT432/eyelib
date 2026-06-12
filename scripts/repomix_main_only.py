#!/usr/bin/env python3
"""
将 eyelib 全项目 main-only 源码打包为单一 markdown 文件。

用法:
    python repomix_main_only.py [output_path]

    output_path 默认: 脚本所在目录/eyelib-all-main.md

依赖:
    Node.js + npx（repomix 通过 npx 自动获取，无需全局安装）
"""

import shutil
import subprocess
import sys
from pathlib import Path


PROJECT_ROOT = Path(r"E:\_ideaProjects\qylEyelib")
if not PROJECT_ROOT.exists():
    PROJECT_ROOT = Path("/mnt/e/_ideaProjects/qylEyelib")

DEFAULT_OUTPUT = Path(__file__).resolve().parent / "eyelib-all-main.md"


def find_repomix() -> list[str]:
    """查找 repomix 可执行文件，优先 npx（跨平台通用）。"""
    # npx 通用方案
    npx = "npx.cmd" if sys.platform == "win32" else "npx"
    if shutil.which(npx):
        return [npx, "repomix"]

    # 回退：全局安装的 repomix
    for name in ("repomix", "repomix.cmd"):
        if shutil.which(name):
            return [name]

    print("错误: 未找到 npx 或 repomix。请安装 Node.js 后重试。", file=sys.stderr)
    sys.exit(1)


def discover_modules() -> list[str]:
    """自动发现所有含 src/main/ 的模块目录，返回相对路径列表。"""
    modules = []
    if (PROJECT_ROOT / "src" / "main").is_dir():
        modules.append("src/main")
    for d in sorted(PROJECT_ROOT.iterdir()):
        if not d.is_dir():
            continue
        if (d / "src" / "main").is_dir():
            modules.append(f"{d.name}/src/main")
    return modules


def build_include(modules: list[str]) -> str:
    """构建单个 --include pattern。"""
    module_paths = ",".join(modules)
    return f"{{{module_paths}}}/**,*.gradle,MODULES.md,AGENTS.md,gradle.properties"


def build_ignore() -> str:
    """构建 --ignore pattern。"""
    return (
        "{build,.gradle,run,.idea}/**,"
        "**/src/test/**,"
        "*.{lock,log,mca,dat},"
        "*.log.gz"
    )


def main():
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUTPUT

    modules = discover_modules()
    if not modules:
        print("错误: 未发现任何含 src/main/ 的模块目录", file=sys.stderr)
        sys.exit(1)

    print(f"发现 {len(modules)} 个模块:")
    for m in modules:
        print(f"  - {m}")

    include = build_include(modules)
    ignore = build_ignore()
    repomix = find_repomix()

    cmd = [
        *repomix,
        "--style", "markdown",
        "--output", str(output),
        "--include", include,
        "--ignore", ignore,
        str(PROJECT_ROOT),
    ]

    print(f"\nexec: {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=str(PROJECT_ROOT))

    if result.returncode != 0:
        print(f"\nrepomix 失败 (exit code {result.returncode})", file=sys.stderr)
        sys.exit(result.returncode)

    if output.exists():
        size_kb = output.stat().st_size / 1024
        size_mb = size_kb / 1024
        if size_mb >= 0.9:
            print(f"\n✓ 输出: {output} ({size_mb:.1f} MB)")
        else:
            print(f"\n✓ 输出: {output} ({size_kb:.0f} KB)")
    else:
        print(f"\n警告: 输出文件 {output} 未生成", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
