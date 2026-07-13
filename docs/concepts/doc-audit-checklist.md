# 架构文档审计清单 (2026-07-13)

> 变更后验证文档与代码的一致性。入口：`docs/README.md`（Diátaxis 四象限结构）。
> 项目结构基线：ADR-0014（单 Gradle project）、ADR-0015（Stonecutter 多版本 + ArchUnit freeze 恢复）、ADR-0016（四层模型）、ADR-0018（IQF）。

## 审计项目

在完成重大架构变更后，逐项对照实际代码验证以下文档：

1. **docs/README.md**:
   - ADR 索引是否与 `docs/decisions/` 一致（0013a 作为 0013 附录，非独立 ADR）
   - Skill 索引是否与 `.opencode/skills/` 实际目录一致
   - 快速导航表的所有路径有效

2. **docs/concepts/architecture.md**:
   - 模块分层图是否反映当前 18 个模块包结构（见 `MODULES.md`）
   - ArchUnit 状态描述准确（已落地，freeze 模式，见 `ArchitectureTest.java`）
   - ADR 索引表与 `docs/decisions/` 一致

3. **docs/architecture/domain-module-map.md**:
   - 模块列表与 `MODULES.md` 一致
   - Port 清单与实际 `*/port/*.java` 文件一致
   - ArchUnit 状态描述准确
   - 提取进度表中的 MC 文件数 / Spec 测试数为快照值，不需要实时同步

4. **docs/decisions/**:
   - ADR 编号连续，0013a 为 0013 附录（共享编号，非独立 ADR）
   - 所有 ADR 中的相对路径引用有效
   - ADR Status 字段反映当前实现状态（如 ADR-0015 的 Phase 进度）

5. **ArchUnit 测试**（`src/test/java/io/github/tt432/eyelib/architecture/`）:
   - `ArchitectureTest.java` 的 DOMAIN_CLASSES 谓词与 ADR-0016 四层模型一致
   - `IqfSourceScanRulesTest.java` 的规则与 ADR-0018 IQF 判据一致
   - `StonecutterCommentPlacementTest.java` 规则有效
   - freeze baseline 存在于 `build/archunit_store/`（首次运行自动生成）

6. **AGENTS.md**:
   - 模块描述与 `MODULES.md` 一致
   - 交叉引用路径有效

7. **.opencode/skills/**:
   - 各 SKILL.md 中 references/ 指向的文件存在
   - Skill 索引与 `docs/README.md` 的 Skill 索引表一致
   - references/ 列表无重复文件

8. **package-info.java**:
   - 每个顶层模块包（`src/main/java/io/github/tt432/eyelib/<module>/`）有 package-info.java
   - port/ 子包有 package-info.java（当前：`molang/port/`、`material/port/`）
   - bridge/ 及其关键子包有 package-info.java

9. **MODULES.md**:
   - 顶层模块列表与 `src/main/java/io/github/tt432/eyelib/*/package-info.java` 一致
   - 由 `:1.20.1:generateModulesMd` 生成，不应手动编辑

## 地真数据采集命令

```bash
# Port 接口位置（单 project，src/ 下直接查找）
find src/main/java -path "*/port/*.java" -not -path "*/build/*"

# Spec 测试计数（单 project）
grep -rc "@Test" src/test/java --include="*Spec*.java" | grep -v ":0$"

# MC import 文件数（按模块包统计）
for mod in src/main/java/io/github/tt432/eyelib/*/; do
  modname=$(basename "$mod")
  files=$(grep -rl "import net\.minecraft" "$mod" 2>/dev/null | wc -l)
  [ "$files" -gt 0 ] && echo "$modname: $files"
done

# 缺失 package-info（顶层模块包）
for dir in src/main/java/io/github/tt432/eyelib/*/; do
  [ -f "$dir/package-info.java" ] || echo "MISSING: $dir"
done

# ArchUnit baseline 是否存在
ls -la build/archunit_store/ 2>/dev/null || echo "MISSING: build/archunit_store/ (run tests first)"

# docs/ 死链接检测
for ref in $(grep -roP '(?<=\]\()\.\.?/[^)]+' docs/); do
  target=$(realpath --relative-to="$(pwd)" "docs/$ref" 2>/dev/null)
  [ -f "$target" ] || echo "DEAD: $ref"
done
```
