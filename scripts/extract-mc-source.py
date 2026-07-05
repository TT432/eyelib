#!/usr/bin/env python3
"""从各 MC 版本节点的 ModDevGradle 产物提取反编译 Minecraft 源码。

输出: .local_ref/mc/{version}/sources/
用途: 供 docs/vanilla-research/*.md 溯源 —— 这些文档内的源码路径均相对此目录。

前置条件: 目标版本节点已执行过 Gradle 构建，ModDevGradle 会在
versions/{version}/build/moddev/artifacts/ 下生成 *-sources.jar。
命名因平台而异: forge-*-sources.jar / neoforge-*-sources.jar / minecraft-patched-*-sources.jar。
若 artifacts 缺失，先运行 gradlew :{version}:compileJava 或切换 active 版本后构建。

用法:
    python scripts/extract-mc-source.py             # 提取所有已构建版本
    python scripts/extract-mc-source.py 1.20.1      # 只提取指定版本
"""
import sys
import zipfile
from pathlib import Path

# Stonecutter 注册的版本节点
ALL_VERSIONS = ("1.20.1", "1.21.1", "26.1.2")


def extract_version(project_root: Path, version: str) -> None:
    artifact_dir = project_root / "versions" / version / "build" / "moddev" / "artifacts"
    jars = sorted(artifact_dir.glob("*-sources.jar"))

    if not jars:
        print(f"[{version}] 跳过: 未找到 *-sources.jar")
        print(f"         搜索路径: versions/{version}/build/moddev/artifacts/")
        print(f"         请先构建该版本（gradlew :{version}:compileJava 或切换 active 后构建）")
        return

    jar = jars[0]
    target = project_root / ".local_ref" / "mc" / version / "sources"
    target.mkdir(parents=True, exist_ok=True)

    count = 0
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.endswith(".java"):
                z.extract(name, target)
                count += 1

    print(f"[{version}] {jar.name} -> .local_ref/mc/{version}/sources/ ({count} 个 Java 文件)")


def main() -> None:
    project_root = Path(__file__).resolve().parent.parent
    versions = sys.argv[1:] or list(ALL_VERSIONS)

    for v in versions:
        extract_version(project_root, v)

    print("完成")


if __name__ == "__main__":
    main()
