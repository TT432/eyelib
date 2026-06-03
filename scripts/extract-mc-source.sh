#!/usr/bin/env bash
# 从 NeoForm 缓存提取反编译的 Minecraft + Forge 源码到项目级本地目录。
#
# 用法:
#   ./scripts/extract-mc-source.sh              # 提取到 .mc-source/
#   ./scripts/extract-mc-source.sh /path/to/dir  # 提取到指定目录
#
# 来源: $GRADLE_CACHE/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_*_output.jar
# 该 jar 包含 Forge 补丁已应用的 MCP 命名源码。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

TARGET="${1:-"$PROJECT_ROOT/.mc-source"}"
mkdir -p "$TARGET"

# Gradle 缓存路径（不受 $HOME 覆盖影响）
GRADLE_CACHE="$(realpath "$(dirname "$(find /root/.gradle -name 'neoformruntime' -type d 2>/dev/null | head -1)")" 2>/dev/null || echo '/root/.gradle/caches')"
NEOFORM_CACHE="$GRADLE_CACHE/neoformruntime/intermediate_results"

# 查找最新的 sourcesAndCompiledWithNeoForge jar
JAR=""
for f in "$NEOFORM_CACHE"/sourcesAndCompiledWithNeoForge_*_output.jar; do
    if [ -f "$f" ]; then
        JAR="$f"
    fi
done

if [ -z "$JAR" ]; then
    echo "错误: 未找到 sourcesAndCompiledWithNeoForge jar"
    echo "搜索路径: $NEOFORM_CACHE"
    echo ""
    echo "请先运行 Gradle 构建（如 runClient）后再运行此脚本。"
    exit 1
fi

echo "源文件: $(basename "$JAR")"
echo "目标目录: $TARGET"

echo "正在解压..."
python3 -c "
import zipfile, os, sys
jar = sys.argv[1]
dst = sys.argv[2]
os.makedirs(dst, exist_ok=True)
count = 0
with zipfile.ZipFile(jar) as z:
    for name in z.namelist():
        if name.endswith('.java'):
            z.extract(name, dst)
            count += 1
print(f'已提取: {count} 个 Java 文件')
" "$JAR" "$TARGET"

echo "完成: $TARGET"
