# 架构文档审计清单 (2026-06-09)

> 变更后验证文档与代码的一致性。入口：`docs/README.md`（Diátaxis 四象限结构）。

## 审计项目

在完成重大架构变更后，逐项对照实际代码验证以下文档：

1. **docs/README.md**:
   - ADR 索引是否与 `docs/decisions/` 一致
   - 快速导航表的所有路径有效

2. **docs/concepts/architecture.md**:
   - C4 分层图是否反映当前模块结构
   - 提取进度表与 ArchUnit 实际排除规则一致

3. **docs/concepts/module-map.md**:
   - 模块列表与 `settings.gradle` 一致
   - 分层依赖描述与 `build.gradle` `project(:)` 一致

4. **docs/decisions/**:
   - ADR-0010 (hexagonal) Verification checklist 反映最新状态
   - ADR-0011 (doc baseline) 文件变更表与实际一致
   - 所有 ADR 路径引用有效

5. **docs/architecture/acceptance-gates.md**:
   - 闸门总览表（测试数量、G1/G2/G3 状态）
   - ArchUnit 代码模板与 `ArchitectureRules.java` 一致

6. **AGENTS.md**:
   - 子模块列表是否包含所有当前模块
   - 交叉引用路径有效

7. **SKILL.md**:
   - §8 指针指向的 docs/ 文件存在
   - references/ 列表无重复文件

8. **package-info.java**:
   - 每个 domain 模块 port/ 目录有一句话 package-info.java
   - bridge 子目录都有 package-info.java

## 地真数据采集命令
```bash
# Port 接口位置
find . -path "*/port/*.java" -not -path "*/build/*" -not -path "*/test/*"

# Spec 测试计数
for mod in eyelib-*; do
  count=$(grep -c "@Test" $mod/src/test/java --include="*Spec*.java" -r 2>/dev/null)
  echo "$mod: $count"
done

# MC import 文件数
for mod in eyelib-*; do
  files=$(grep -rl "import net\\.minecraft" $mod/src/main/java 2>/dev/null | wc -l)
  echo "$mod: $files"
done

# 缺失 package-info
for dir in <module>/port <bridge>/material <bridge>/molang; do
  [ -f "$dir/package-info.java" ] || echo "MISSING: $dir"
done

# docs/ 死链接检测
for ref in $(grep -roP '(?<=\\]\()\.\.?/[^)]+' docs/); do
  target=$(realpath --relative-to="$(pwd)" "docs/$ref" 2>/dev/null)
  [ -f "$target" ] || echo "DEAD: $ref"
done
```
