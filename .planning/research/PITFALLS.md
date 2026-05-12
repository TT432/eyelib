# Domain Pitfalls

**Domain:** Multi-module Forge project structural cleanup
**Researched:** 2026-05-12
**Overall confidence:** HIGH

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: 将 Runtime Adaptation 类型误判为 Schema Duplicate

**What goes wrong:** 看到 root `BrBoneAnimationDefinition` 和 importer `BrBoneAnimationSchema` 有相似字段名，认为它们是重复代码，试图合并。
**Why it happens:** 两套类型在字段名上有重叠（都描述 bone animation），但它们的语义层完全不同。
**Consequences:** 如果将 Definition 合并到 Schema，会破坏运行时预编译的 `ImmutableFloatTreeMap` 缓存、破坏 Catmull-Rom 插值的 setupCurvePoints 预计算、破坏代码执行路径。
**Prevention:** 检查类型的目标消费者——Schema 被 Codec 消费（importer），Definition 被 Sampler/Executor 消费（root runtime）。如果消费者不同，不是 duplicate。
**Detection:** 如果两个类型有不同的包名和不同的字段（Definition 有 sortedKeys、compiled channels），它们不是 duplicate。如果两个类型在同一层做同一件事，且字段列表完全一致，才是 duplicate。

### Pitfall 2: 将活跃的 Br*Executor 误删为"无效代码"

**What goes wrong:** 在 ANIM-01 阶段，不加引用验证就删除 `bedrock/` 下的类。
**Why it happens:** v1.4 的 KeyFrame.java 删除成功，产生"bedrock 里都是遗留代码"的错误印象。
**Consequences:** 删除 `BrClipExecutor` 或 `BrControllerExecutor` 会直接导致动画系统停止工作——EntityRenderSystem → BrAnimator → Animation.tickAnimationUntyped() → BrClipExecutor.tick() 是关键运行时路径。
**Prevention:** 对 `client/animation/bedrock/` 中**每个** .java 文件运行 IDE "Find References"（scope: project_production_files），只删除零引用的文件。
**Detection:** 活跃指示器——文件被 BrAnimator、EntityRenderSystem、BrAnimationEntry、BrAnimationController 或任何 manager/loader 引用。

### Pitfall 3: 破坏 EyelibAttachableData 注册中心

**What goes wrong:** 在 CAP-01 阶段，试图将 `EyelibAttachableData.java` 移动到 `:eyelib-attachment`。
**Why it happens:** 看到它引用了许多 attachment 类型，直觉上认为它"属于" attachment。
**Consequences:** Forge `@Mod.EventBusSubscriber` 注册不能在 attachment 子模块中完成——它依赖 root 的 `Eyelib.MOD_ID`、`DeferredRegister` 注册到 Forge MOD 事件总线、以及 `mc/impl/bootstrap/EyelibMod` 调用 `EyelibAttachableData.DATA_ATTACHMENTS.register(bus)`。移动它会导致 Forge 启动崩溃。
**Prevention:** `EyelibAttachableData` 是 **registry hub**，不是 data type。判断标准：它持有 `RegistryObject<DataAttachmentType<...>>` 常量 → 留在 root。
**Detection:** 警告标志——文件包含 `@Mod.EventBusSubscriber`、`DeferredRegister`、`RegistryObject`、`IForgeRegistry` ——这些都是 root-only Forge 启动关注点。

## Moderate Pitfalls

### Pitfall 4: 将 EntityBehaviorData 的 codec 部分错误提取到 attachment

**What goes wrong:** 看到 `EntityBehaviorData` 有 `CODEC` 和 `STREAM_CODEC` 静态字段，认为整个 codec 应该移入 attachment。
**Why it happens:** 应用"data/codec → attachment"规则时过于机械。
**Consequences:** `EntityBehaviorData` 的 codec 序列化包含 behavior-specific 字段（与 `MolangQuery` 的 variant/markVariant 查询紧密耦合）。如果提取到 attachment，需要引入 attachment → Molang 的显式依赖（可接受）或者 codec 部分不能再使用 behavior-specific 序列化（可能导致破坏性重构）。
**Prevention:** 分析 codec 中字段的消费者——如果所有消费者在 root 中，且 codec 深度绑定 root 行为语义，则保留在 root。只有当 codec 是"纯数据结构序列化"且被多个模块消费时才提取。
**Detection:** 如果 codec 的 `RecordCodecBuilder` 包含对 root 类型的引用或依赖 root behavior 方法 → 不能提取。

### Pitfall 5: 扫描 preprocessing 归属时误移仍被 root 深度依赖的代码

**What goes wrong:** PREP-01 发现 root 中有 parse/bake 模式的代码，自动移动到 preprocessing 后破坏编译。
**Why it happens:** 不是所有 parse/bake 代码都属于 preprocessing——有些是 root runtime 专有的预处理步骤。
**Consequences:** 如果移动的代码依赖 root runtime 类型（如 `RenderData`、`AnimationComponent`、`MolangScope`），会在 preprocessing 模块中创建反向依赖。
**Prevention:** 移动前验证——代码是否使用 `import io.github.tt432.eyelib.*`（root 包）？如果是，不能移动。移动后运行 `jetbrain_build_project` 验证全量编译。
**Detection:** 硬性规则——如果文件 import 任何 `io.github.tt432.eyelib.capability`、`io.github.tt432.eyelib.client.animation`、`io.github.tt432.eyelib.mc.impl` → 不能移入 preprocessing。

### Pitfall 6: git 中残留已删除源文件的 stale .class 文件

**What goes wrong:** v1.4 已删除的源文件（如 root 中的 ExtraEntityData.java）的 .class 文件仍在 bin/ 目录中。
**Why it happens:** 增量编译不会删除不再存在的源文件对应的 .class 文件。
**Consequences:** IDE 引用分析可能出现误报（bin/ 中的 .class 被索引），clean build 前 IDE 可能报告不存在的引用。
**Prevention:** 在 ANIM-01/CAP-01 之前运行 `jetbrain_run_gradle_tasks task: [":clean"]` 然后 `jetbrain_build_project`，确保 bin/ 与 src/ 一致。
**Detection:** 检查 `bin/main/` 中是否有 .class 文件在 `src/main/java/` 中没有对应的 .java 文件。

## Minor Pitfalls

### Pitfall 7: README.md 引用旧模块名 `eyelib-processor`

**What goes wrong:** README 中仍有 `eyelib-processor` 字符串（v1.4 重命名前的旧名）。
**Why it happens:** grep 替换可能遗漏了 README 文件。
**Consequences:** 新开发者看到过时的模块名产生困惑。
**Prevention:** 在 DOCS-01 阶段，用 `jetbrain_search_in_files_by_text` 搜索整个项目的 `eyelib-processor` 字符串并替换为 `eyelib-preprocessing`。
**Detection:** 搜索 `eyelib-processor`（不含 `eyelib-preprocessing` 的完整匹配）。

### Pitfall 8: 删除了遗留 compatibility pointer README

**What goes wrong:** 在 DOCS-01 看到 `mixin/README.md` 内容简短，认为是无效文档而删除。
**Why it happens:** 未意识到这是 intentional legacy compatibility pointer。
**Consequences:** 破坏指向新位置的指引链。
**Prevention:** 阅读 README 内容——如果明确说明"this file remains as compatibility pointer"，不要删除。
**Detection:** README 包含 "legacy"、"compatibility pointer"、"phased out" 等关键词。

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| ANIM-01: bedrock/ 清理 | Pitfall #2 (误删 active executor) | 逐文件 IDE Find References → 仅删除零引用 |
| CAP-01: capability 审计 | Pitfall #3 (移动 registry hub) | 检查 @Mod.EventBusSubscriber 标记 → 识别 registry hub |
| CAP-01: EntityBehaviorData | Pitfall #4 (错误提取 codec) | 分析 codec 字段消费者 → 仅纯数据提取 |
| PREP-01: 归属扫描 | Pitfall #5 (误移 runtime 依赖代码) | 检查 import 语句 → 禁止移动到 preprocessing |
| DUP-01: 重复检测 | Pitfall #1 (误判 adaptation layer) | 比较消费者和字段列表 → 不同层不是 duplicate |
| DOCS-01: README | Pitfall #7 (旧名残留) | 全文搜索 `eyelib-processor` |
| 所有阶段 | Pitfall #6 (stale .class) | 阶段开始前 clean build |

## Sources

- v1.0–v1.4 多轮模块分离的实践模式（MODULES.md、01/02 boundary docs）
- PROJECT.md v1.5 active requirements
- IDE reference analysis on active integration points
- 对 bin/ stale artifacts 的源码树检查
- 所有 confidence: HIGH（来自已完成的 v1.0–v1.4 里程碑中验证的 pitfall 模式）
