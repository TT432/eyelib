# ADR-0017: 移除 migrate26Renames 文本替换，统一用 `//?` 条件注释

**Status:** Accepted
**Date:** 2026-06-28
**Author:** @TT432
**Related:** 补充 [ADR-0015](0015-stonecutter-multi-version.md) §2、[ADR-0016](0016-bridge-extraction-standard.md) §6

## Context

### 问题

build.gradle 曾有一个 `migrate26Renames` Gradle task，在 Stonecutter 生成 26.1.2 源码后、编译前，对 `build/generated/stonecutter/main/java` 做 30+ 种文本替换（类名、方法名、包名、注解参数）。这是让 26.1.2 node 快速编译通过的临时补丁。

该 task 违反了 ADR-0015 §2 和 ADR-0016 §6 定义的正规机制：

- **绕过 `//?` 条件注释**：ADR-0015 §2 定义 L1 差异（import/方法名/常量）用 `//?` 注释处理。migrate26Renames 的 30+ 替换全是 L1 差异，却用隐蔽的编译期文本替换绕过了它。
- **绕过 Port 抽象**：ADR-0015 §2 定义 L2 差异用 Port 接口 + per-version 实现。migrate26Renames 对全项目（含 application 层）做替换，违反 ADR-0016 §6 的「`//?` 唯一栖息地」约束。
- **降低源码可读性**：源码中读到的名字（如 `ResourceLocation`）与 26.1.2 编译/运行时的实际名字（`Identifier`）不一致。开发者无法从源码直接判断 26.1.2 下的真实 API。

### 约束

- active version = 1.20.1，移除 migrate26Renames 不影响 active node 编译。
- 26.1.2 node 必须全程保持可编译（每批迁移后立即验证）。

## Decision

完全移除 `migrate26Renames` task（含 `compileJava.dependsOn` 和 `upToDateWhen { false }`），所有替换用 ADR-0015 §2 定义的正规机制替代：

| 差异类型 | 替代方式 |
|---|---|
| 类名/常量 rename（ResourceLocation → Identifier 等） | `//?` import 块 + 使用处块 |
| 方法名变化（getTimer → getDeltaTracker 等） | `//?` 单行条件或块 |
| 包名变化（GameRules → gamerules 等） | `//?` import 块 |
| 注解参数差异（EventBusSubscriber） | `//?` 块 else 分支改参数 |
| 已有封装抽象（Codec.unit） | 调用点改方法名 + import（EyelibCodec.unit） |

死规则（26.1.2 生成代码不含目标符号的规则）直接删除。

## Consequences

### 正面

- **源码可读性提升**：所见即所得，源码中的名字就是编译/运行时的名字。
- **架构契约一致**：所有 L1 差异统一走 `//?`，符合 ADR-0015 §2 和 ADR-0016 §6。
- **构建简化**：移除 task + dependsOn + upToDateWhen，26.1.2 不再每次强制重编译。
- **可维护性**：新增 L1 差异直接在源码加 `//?` 块，无需修改 build.gradle。

### 负面

- `//?` 块数量增加（尤其 ResourceLocation 的 44 文件），需 IDEA Stonecutter 插件辅助阅读。
- 一次性迁移脚本（`scripts/migrate_*.py`）仅适用于本轮迁移，后续新差异仍需手动加 `//?`。

## Verification

- `:26.1.2:build` → BUILD SUCCESSFUL（无 migrate26Renames，纯 `//?` 编译通过）。
- `:1.20.1:build` → BUILD SUCCESSFUL（`//?` 块不影响 active version）。
- 迁移脚本幂等性验证：dry-run 0 文件。
