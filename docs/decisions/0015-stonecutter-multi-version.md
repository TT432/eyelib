# ADR-0015: Stonecutter 多版本改造与 ArchUnit 恢复

**Status:** Accepted (Phase 1 implemented — 1.20.1 node live; Phase 2+ pending; §4 ArchUnit freeze framework extended by [ADR-0018](0018-isolated-quiescent-fragments.md) with 4 new IQF rules joining the same freeze baseline)
**Date:** 2026-06-17
**Author:** @TT432
**Amends:** ADR-0010（恢复 ArchUnit 强制；`//?` 注释栖息地从「bridge 唯一」扩展为「L1 散布 + L2/L3 集中 bridge」）、ADR-0014（单 Gradle project 单版本 → 单 Gradle project + Stonecutter 多 node）
**Related:** ADR-0002（模块边界仍按包名约定，新增 bridge 版本目录结构规则）
**Implementation:** Stonecutter **0.7.11**（非 0.5.x placeholder；0.8+ 要求 Gradle 9，但 ModDevGradle legacyforge 不支持 Gradle 9，故锁 0.7.x + Gradle 8.12.1）。详见 `docs/stonecutter-migration-handoff.md`。

## Context

### 问题

eyelib 当前只支持 MC 1.20.1（Forge 47 / Java 17）。需要支持三个版本：

- **1.20.1**（Forge 47 / Java 17 / `net.neoforged.moddev.legacyforge` 插件）
- **1.21.1**（NeoForge 21.1 / Java 21 / `net.neoforged.moddev` 插件）
- **26.1.2**（NeoForge / Java 25 / `net.neoforged.moddev` 更新版；游戏类不再混淆）

ADR-0014 合并了 12 个 Gradle 子项目为单 project，降低了模块隔离的物理约束。ADR-0010 删除了 ArchUnit 强制，domain 隔离改为文档约定，已出现 MC import 渗透（`material/material/BrMaterial` 用 `RenderStateShard`、`molang/mapping/MolangQuery` 直接 import `net.minecraft.world.entity.*` 等）。

### 多版本的技术挑战

1. **跨度极大**：1.21.1 → 26.1.2 累积了 1.21.4（物品模型重写）、1.21.5（items→components、核心着色器→`RenderPipeline`）、1.21.8（renderable data 抽取）、1.21.9（render states 重写）、1.21.11（Jspecify 原生可空、`ResourceLocation`→`Identifier`）、26.1（Java 25、不混淆、`ItemStack`+注册表、`GuiGraphics`→`GuiGraphicsExtractor`）、26.2（渲染后端 OpenGL/Vulkan 可切换）
2. **对渲染库尤其严重**：eyelib 核心是渲染，上述变化直接击中 `material`/`client/render` 主路径
3. **`//?` 注释不够用**：单 `src/` 共享 + Stonecutter 注释条件适合小差异，但 render states 重写、`RenderPipeline` 架构变化需要并行实现

### 目标

> 一套共享源码产三个版本的可用 jar（1.20.1 / 1.21.1 / 26.1.2），架构支持持续改进、不必一步到位，同时填补 ADR-0014 留下的 domain 隔离约束真空。

### 方案对比

| 方案 | 描述 | 否决理由 |
|---|---|---|
| A. 单 src + 纯 `//?` | 所有差异靠注释切 | 1.21.1 → 26.1.2 渲染路径差异超出注释承载能力 |
| **B. Stonecutter + 渐进式 bridge 抽取** | 单 src + 多 node + 分层差异处理（L1 `//?` / L2 Port / L3 per-version） | **采纳** — 契合「持续改进、不必一步到位」 |
| C. 多 Gradle project | 每版本独立 project + shared common | 用错隔离维度（版本不是模块，bridge 包已吸收 MC 接触点）；与 ADR-0014 单 project 决策冲突 |

## Decision

### 1. Stonecutter 项目模型

采用 Stonecutter（`dev.kikugie.stonecutter`）Tree → Branch → Node 模型：

- **单 root branch 三 node**（root branch 隐式 `""`），不引入多 branch
- versions: `"1.20.1"`, `"1.21.1"`, `"26.1.2"`（不加 loader 后缀，单 loader 系）
- 默认 active = `"1.21.1"`
- `src/` 共享源码 = active version，靠 `//?` 注释切版本

### 2. 差异分级标准

| 档 | 判定 | 处理方式 |
|---|---|---|
| **L1 小差异** | import / 方法名 / 单行 API / 常量值变化 | `//?` 注释条件 |
| **L2 中差异** | 单方法体逻辑分歧 / 类签名变化；或 `//?` 块超过 ~20 行；或同一逻辑在 3+ node 本质不同 | Port 接口 + per-version 实现（bridge 抽取） |
| **L3 大差异** | 整个渲染路径重写 / 管线架构变化；或 L2 Port 接口本身都不稳定 | per-version 独立模块 / 包（subsystem 并行实现） |

升级规则：L1 的 `//?` 块超过 ~20 行 → 升级 L2；L2 Port 接口本身不稳定 → 升级 L3。

### 3. bridge 多版本包结构

- `bridge/<feature>/PortName.java`：版本无关接口，**禁止 import MC**
- `bridge/<feature>/v<ver>/ImplName.java`：版本特定实现，整文件级 `//?` 条件包裹
- 现有 bridge（仅 molang/material）需扩展：新增 `render/`（RenderTypePort、GuiGraphicsPort、ResourceLocationPort）、`itemstack/`（ItemStackPort，26.1+）

### 4. ArchUnit 恢复（反转 ADR-0014 的删除决策）

恢复 ArchUnit 测试，规则：

- **domain 白名单**：`importer`/`molang`/`material`/`model`/`animation`/`behavior`/`util`/`particle` 不得依赖 `client`/`network`/`capability`/`common`/`bridge`
- **MC import 分级**：`com.mojang.serialization.*`/`com.mojang.datafixers.*`（DFU）白名单；`com.mojang.blaze3d.*`/`net.minecraft.*`/`net.minecraftforge.*`/`net.neoforged.*` 仅限 `bridge`/`client`/`network`/`capability`/`common`/`mixin`
- **bridge 包结构**：`bridge/**/v*/` 允许 import MC（实现层），`bridge/**/Port*.java` 禁止（接口层）
- **每个 node 跑**：多版本每个 Stonecutter node 跑 ArchUnit

**策略：freeze + 逐步还债**。Phase 0 以 freeze 模式记录现有违规生成 baseline 文件，新违规直接 fail，老违规按 phase 还债。

### 5. 版本范围常量

`stonecutter.gradle` 定义（具体语法以 Stonecutter DSL 为准，实现时校准）：

- `V_FORGE = it < "1.20.6"`
- `V_NEOFORGE = it >= "1.20.6" && it < "26.1"`
- `V_MODERN = it >= "26.1"`

源码用 `//? if V_FORGE {` 替代裸范围条件，提升可读性。

### 6. build.gradle 共享脚本策略

用 `apply plugin:` 运行时条件（`plugins {}` block 不支持运行时条件），按 `sc.current.parsed` 切 `legacyForge { }` vs `neoForge { }` 块、Java toolchain、依赖。

若后期 26.1.x 的 ModDevGradle DSL 差异过大，可拆 `build-legacy.gradle`/`build-neoforge.gradle` 并用 `mapBuilds` 切。

### 7. Mixin 跨版本

三 node 共用 Spongepowered Mixin 0.8.5 + MixinExtras 0.5.0。**26.1.2 游戏类不混淆，无 refmap**：26.1.2 node 的 `eyelib.mixins.json` 移除 `"refmap"` 字段，`mixin { add sourceSets.main, "${mod_id}.refmap.json" }` 配置跳过。Mixin 运行时字节码修改本身仍支持。

### 8. clientsmoke 跨版本

Phase 1-4 用临时门控（`if (sc.current.version == "1.20.1")` 仅 1.20.1 node 依赖 clientsmoke）。Phase 5 升级 clientsmoke 为独立 Stonecutter tree，三 node 都支持。

### 9. Phase 路线图（概要）

| Phase | 成果 | 前置 |
|---|---|---|
| 0 清理与准备 | 清理反射残留 + 旧 modid；ArchUnit 骨架 freeze | — |
| 1 Stonecutter 脚手架 + 1.20.1 回归 | 1.20.1 node 产可用 jar；1.21.1/26.1.2 声明但允许失败 | 0 |
| 2 1.21.1 node 编译通过 | 1.21.1 node 产可用 jar | 1 |
| 3 ArchUnit 收紧 | domain 模块逐个清零违规（与 2/4 并行） | 0 |
| 4 26.1.2 node 编译通过 | 26.1.2 node 产可用 jar（最高难度） | 2 |
| 5 clientsmoke 多版本化 | 三 node 都支持 smoke 测试 | 4 |
| 6+ 持续改进 | ArchUnit 最终 domain 零 MC import；新版本增量加入 | 5 |

Phase 3 可与 Phase 2/4 并行；Phase 5 依赖 Phase 4。

## Consequences

### 正面

- **多版本可发布**：一套源码产三个版本 jar，覆盖 1.20.1 LTS + 1.21.1 LTS + 最新 26.1.x
- **渐进改进**：Phase 划分让每个阶段都有可用产出，不必一步到位
- **ArchUnit 恢复**：填补 ADR-0014 留下的约束真空，阻止 domain 继续腐化
- **bridge 清晰化**：L2/L3 差异倒逼 MC 接触点集中到 bridge，六边形架构真正落地

### 负面

- **学习曲线**：Stonecutter `//?` 语法、IDEA 插件、active/VCS version 概念需要团队熟悉
- **源码可读性下降**：`//?` 注释条件增加阅读负担（L1），需 IDEA 插件辅助
- **构建复杂度上升**：build.gradle 要用 `apply plugin:` 运行时条件（不能再用 `plugins {}` block），调试体验差
- **26.1.x 工作量大**：L3 渲染路径并行实现、Jspecify 与 NullAway 冲突、Java 25 toolchain
- **clientsmoke 复杂化**：独立 tree 后维护成本上升

### 中性

- **新增依赖**：Stonecutter Gradle 插件 + IDEA 插件
- **目录结构**：新增 `versions/<ver>/gradle.properties`、`stonecutter.gradle`

## Verification

- Phase 0：清理后 grep 无反射残留；ArchUnit baseline 文件生成
- Phase 1：1.20.1 node `jetbrain_build_project` 退出码 0；runClient 冒烟通过
- Phase 2：1.21.1 node 同上
- Phase 4：26.1.2 node 同上
- Phase 3：ArchUnit 规则按 domain 模块逐个从 freeze 升级为 fail
- 每个 Phase 完成时：对应 node 产可用 jar
