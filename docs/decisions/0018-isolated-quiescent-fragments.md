# ADR-0018: 孤立静止片段架构（IQF）

**Status:** Accepted
**Date:** 2026-06-28
**Author:** @TT432
**Related:** 修补 [ADR-0010](0010-hexagonal-architecture.md)（六边形架构 — 扩展片段形状判据）、与 [ADR-0014](0014-flat-merge.md) 协同（包边界内片段的形状约束）、扩展 [ADR-0015](0015-stonecutter-multi-version.md) §4（ArchUnit 规则集）、扩展 [ADR-0016](0016-bridge-extraction-standard.md) §3（ACL 职责边界 + Application 层形状）

## Context

### 问题

[ADR-0010](0010-hexagonal-architecture.md) / [0014](0014-flat-merge.md) / [0015](0015-stonecutter-multi-version.md) / [0016](0016-bridge-extraction-standard.md) 已建立四层架构（Domain / ACL / Application / Infrastructure）+ Stonecutter 多版本 + ArchUnit 隔离 baseline。Domain 层已基本实现"可离线验证"（ADR-0010 §提取进度表）。

但代码层面仍存在违反「孤立静止」原则的反模式，导致：

1. **可测试性受限**：Domain 已是孤立静止片段，但 **Application 层（编排）依赖全局 singleton 与 static wiring**，无法离线验证编排逻辑（EntityRenderOrchestrator 的 setup→tick→render 流程目前必须启动 MC 才能验证）。
2. **时序敏感 bug**：static initializer 与全局可变字段让正确性依赖类加载顺序与调用时机：
   - 8 处 `XxxManager.INSTANCE`（MaterialManager / ModelManager / ClientEntityManager / RenderControllerManager / AnimationManager / BehaviorEntityManager / AttachableManager / ParticleRenderManager）；
   - `EntityRenderPorts.{renderStagePort, renderBufferPort, renderEntityPort, setupClientEntityPort}` 四个 public static 可变字段；
   - `ManagerEventPublishBridge.publisher`（含 `install()` / `reset()` 时序合同）；
   - `EntityRenderSystem.{renderCount, errorCount, lastError}` volatile 全局可变计数器。
3. **副作用的可追溯性差**：`EntityRenderOrchestrator.onRenderStage` 通过两段 `entities().forEach(...)` + 闭包副作用串联 setup / tick / render，时序藏在控制流中，不在数据结构中；`setup` 返回 `List<Runnable>` 延后执行，副作用合同散落在调用点。
4. **跨层泄漏**：Application 直接调用 ACL 静态方法（`EntityRenderSystem.{pushPoseRaw, renderItemDirect, flushBuffer, getEntityTypeId, ...}`），绕过 Port 抽象，违反 [ADR-0016](0016-bridge-extraction-standard.md) §5「Application 通过 ACL 接触 MC」。
5. **懒初始化守卫散落**：`if (cap.getOwner() != entity) cap.init(entity)` 在 4 处重复，状态所有者不唯一。

### 根因（不是个别 bug，而是缺一类约束）

[ADR-0016](0016-bridge-extraction-standard.md) 定义了**四层**与**依赖方向**，但没定义**片段内部的形状**。当前的 ArchUnit 5 条规则只回答"谁可以依赖谁"，没回答"一个片段必须长成什么样才能保证孤立静止"。所以即便分层正确，片段内部仍可以装进全局可变状态、static wiring、隐式时序。

### 目标

把所有代码切分成「孤立静止片段」（**Isolated & Quiescent Fragment，IQF**），让片段形状判据**可机器验证**、**可逐步还债**，并复用 [ADR-0015](0015-stonecutter-multi-version.md) §4 已验证的 freeze baseline 策略。

> **孤立** = 片段的上下文（依赖集合）是有限、显式、可在类型签名中读出的。
> **静止** = 片段的行为不依赖调用时机；时序从控制流转换为数据结构。

### 与现有 ADR 的关系

| 现有 ADR | 本 ADR 与它的关系 |
|---|---|
| [0010](0010-hexagonal-architecture.md) | 把"Domain 可离线验证"扩展到"Application 也可离线验证"——IQF 是 ADR-0010 目标的彻底化 |
| [0014](0014-flat-merge.md) | 包边界仍是物理边界；本 ADR 在包边界之内补充片段形状约束，不重新引入 Gradle subproject |
| [0015](0015-stonecutter-multi-version.md) | 复用 §4 的 ArchUnit freeze baseline 机制；新增 4 条 IQF 规则加入同一 freeze 框架 |
| [0016](0016-bridge-extraction-standard.md) | 补全 §3 表格未覆盖的"Application 层片段形状"；强化 §5「Application 通过 ACL 接触 MC」为可机器验证 |

## Decision

### 1. IQF 定义（可验证判据）

一个代码片段（类 / 方法 / 包）称为 **Isolated & Quiescent Fragment** 当且仅当同时满足全部孤立判据与全部静止判据：

**孤立判据（Isolated）**

| ID | 判据 | 可验证标准 |
|---|---|---|
| I-1 显式依赖 | 片段依赖的所有对象都出现在构造参数 / 方法签名 / import 中，禁止运行时服务定位 | ArchUnit 规则 + review |
| I-2 无服务定位 | 禁止 `public static final Xxx INSTANCE`、`getInstance()`、副作用型 `Class.forName(...)` | ArchUnit 字符串扫描 |
| I-3 单一所有者 | 每个可变状态有且只有一个写者，写者由类型系统标识 | review + 字段写者计数规则 |
| I-4 有限上下文扇入 | 公开 API 的 fan-in ≤ 30；超过必须拆 | 知识图谱查询（codebase-memory `fan_in`） |
| I-5 仅依赖抽象（ACL 开放契约） | Application 层不得依赖 ACL（bridge）的具体类，只能依赖 ACL 暴露的**接口（Port）与反射调度注解**；MC 原生类型由 ACL 内部 Port 接口封装 | ArchUnit 规则（依赖类型种类检查） |

> 判据 I-5 是 ADR-0016 §3「ACL 职责边界」的可机器验证形态，由本 ADR §3 机制 E 完整定义。

**静止判据（Quiescent）**

| ID | 判据 | 可验证标准 |
|---|---|---|
| Q-1 引用透明 | 同样输入 → 同样输出 / 同样副作用集合 | spec-based 测试（与 ADR-0012 三层测试模型 Layer 2/3 对齐） |
| Q-2 无静态初始化时序 | 禁止 `static {}` 块做业务 wiring；类加载顺序不影响正确性 | ArchUnit 规则 |
| Q-3 显式状态机 | 多阶段流程（load→tick→render）以数据形式编码，不是嵌套闭包副作用 | review |
| Q-4 无懒初始化散落 | "if (not initialized) init()" 只能出现在所有者内部一个位置 | ArchUnit 字符串扫描 + review |
| Q-5 可重入 / 可重放 | 同一片段跑两遍不破坏不变量（除显式标记的副作用 sink） | spec-based 测试 |

### 2. 四个根因与解药

| 根因 | 现状 | 解药（成熟模式） | 命中判据 |
|---|---|---|---|
| R1 服务定位器 + singleton | `Manager.INSTANCE` 全局可变，时序敏感（先 load 后 query） | **依赖注入 + 不可变快照**：`Manager<T>` 改构造注入的 `Registry<T>`，状态用 `AtomicReference<RegistrySnapshot<T>>` 持有 | I-2, I-3, Q-1, Q-5 |
| R2 隐式编排时序 | `forEach` + 闭包副作用把 setup / tick / render 串成顺序依赖 | **Pipeline as Data**：定义 `FramePlan`（每帧计划）数据类型，setup/tick/render 各是 `FramePlan → FramePlan` 的纯函数；副作用集中到唯一的 `EffectCommitStage` | Q-1, Q-3, Q-5 |
| R3 静态初始化 wiring | `static {}` + `Class.forName` 触发跨层装配 | **Composition Root**：所有 wiring 集中在 `EyelibRuntime` 对象的构造函数里，由 `@Mod Eyelib` 创建一次 | I-1, Q-2, Q-4 |
| R4 ACL 具体类泄漏到 Application | Application 直接 import / 调用 bridge 具体类与静态方法（`EntityRenderSystem.{pushPoseRaw, renderItemDirect, flushBuffer, getEntityTypeId, ...}`），绕过 Port 抽象 | **ACL 开放契约（反射调度）**：ACL 仅以接口 + 反射调度注解对 Application 开放；Forge 事件订阅归 ACL adapter；Application 用注解声明处理；adapter 翻译后回调 | I-1, I-5 |

### 3. 五个核心机制

**机制 A：`Registry<T>` 取代 `Manager.INSTANCE`（解决 R1）**

在 `util/registry/` 落地不可变快照型注册中心：状态对外仅暴露只读 `RegistrySnapshot<T>`，写入通过唯一方法 `replaceAll(Map)` 原子替换；事件发布器 `ManagerEventPublisher` 由构造注入，删除 `ManagerEventPublishBridge` 静态桥。保留 `Repository<T>` 接口契约（`Registry` 实现 `Repository`，`get()` 读当前快照），manager / loader / visitor / codec 等现有模式不变。

**机制 B：`FramePlan` 数据化管道（解决 R2）**

把"先 setup 后 tick"的时序从**调用顺序**翻译成**数据结构**。每个阶段（SetupStage / TickStage / EffectCommitStage）是 `(EntityWorldView, FramePlan) → FramePlan` 的纯函数；`FramePipeline` 持有有序的 `List<FrameStage>`，时序藏在列表顺序里，可遍历、可调试、可重放。setup 返回的延迟副作用（当前 `List<Runnable>`）显式记录在 `EntitySetupResult.deferredEffects()` 中，由唯一的 `EffectCommitStage` 在管道末端提交。

**机制 C：Composition Root 取代 static wiring（解决 R3）**

所有跨层 wiring 集中在 `bridge/EyelibRuntime` 的构造函数中，由 `@Mod Eyelib` 唯一一次创建。`EyelibRuntime` 的职责被严格限定为：
1. 构造 `ManagerEventPublisher`；
2. 实例化所有 `Registry`；
3. 实例化 ACL adapter（订阅 Forge 事件，由 adapter 自己持有 `@SubscribeEvent` 方法）；
4. 触发**注解发现 + 装配**（见机制 E）—— `EyelibRuntime` 不直接 `new` Application 类；注解发现复用 (Neo)Forge 反射系统的预扫描数据（`ModList.get().getAllScanData()` + `ModFileScanData.AnnotationData`），由 bridge `*LifecycleHooks` 在 Forge 生命周期事件（如 `FMLCommonSetupEvent`）中触发一次装配，把发现的注解类注册到 domain 的装配入口；
5. 构造 `RenderPorts` 实例并注入。

`EntityRenderPorts.*` 四个 static 字段改为 `RenderPorts` 实例字段；`static { Class.forName(...) }` wiring 块全部删除。`EyelibRuntime` 自身**只做 wiring，不写业务规则**（与 ADR-0010 §"bridge 不能自行发明抽象"同约束思路）。

**机制 D：ArchUnit 强制 IQF 判据**

在 ADR-0015 §4 已有 5 条规则 + 1 条源码扫描规则基础上，新增 5 条 IQF 规则（详见 §7），全部纳入同一 freeze baseline 框架。

**机制 E：ACL 开放契约（接口 + 反射调度注解，解决 R4；借助 (Neo)Forge 反射系统）**

ACL（bridge）对 Application 暴露的形式**只能是接口（Port）与反射调度注解**，不暴露任何具体类、不暴露任何 MC 原生类型。这是 ADR-0016 §3 ACL 职责边界的彻底化，依赖倒置后由 ACL adapter 实现这些接口、消费这些注解。

**项目已有范本**：`molang/mapping/` 子包完整实现了这一模式——`@MolangMapping` / `@MolangFunction` 注解定义在 `molang/mapping/api/`（domain 层，零 MC 依赖）；`MolangMappingDiscovery` 是 domain 定义的 `@FunctionalInterface`；`bridge/molang/mapping/ForgeMolangMappingDiscovery` 用 (Neo)Forge 反射系统实现发现；`MolangMappingTreeLifecycleHooks` 在 `FMLCommonSetupEvent` 触发装配。**旁证**：`clientsmoke` 框架的 `@ClientSmoke` 注解也用同一 `ModFileScanData` 机制发现（见 `clientsmoke/README.md`、Skill `eyelib-clientsmoke`）——这是项目的成熟实践，不是新引入。**机制 E 是把这个范本推广到所有 ACL↔Application 交互**。

**(Neo)Forge 反射系统的精确入口**：Forge 在 mod 加载阶段已通过 ASM 字节码扫描**预收集**了所有 `@Retention(RUNTIME)` 注解的位置数据，运行时通过 `ModList.get().getAllScanData()` 查询 `ModFileScanData.AnnotationData` 即可——**不需要再次扫描字节码、不需要自实现 classpath 扫描、不引入 Java SPI（`ServiceLoader` + `META-INF/services/`）**。

具体由五条子约束组成：

1. **类型化接口（Outbound Port）**：每个 Application 需要接触的 MC 概念都有对应的 Port 接口（如 `PortPoseStack` / `PortBufferSource` / `PortRenderType` / `PortEntity` / ...）。Application 只能依赖 Port 接口，不能 import `com.mojang.blaze3d.*` / `net.minecraft.*` / `net.minecraftforge.*` / `net.neoforged.*` 或 ACL adapter 具体类。
2. **声明式 Inbound Port（反射调度注解）**：需要由外部刺激（Forge 事件、生命周期钩子）触发的 Application 逻辑，由 Application 在类/方法上标记反射调度注解（如 `@OnRenderStage` / `@OnEntitySetup` / `@OnClientTick`），声明"我处理这类事件"。**注解的参数类型只能是 Application 级类型**（如 `float partialTick, double camX, ...`），不得引用 Forge / MC 事件类（不得出现 `RenderLevelStageEvent` 等）。注解 `@Retention(RUNTIME)` 是硬性要求（否则 Forge ASM 预扫描收集不到）。
3. **注解定义在 domain、发现接口在 domain、Forge 实现在 bridge**（与 MolangMapping 范式严格对齐）：注解本身（如 `@OnRenderStage`）和发现接口（如 `EventSubscriberDiscovery`，`@FunctionalInterface`）定义在 domain 层的 API 子包（与 `molang/mapping/api/` 同形态）；bridge 层提供 `ForgeEventSubscriberDiscovery` 用 `ModList.get().getAllScanData()` + `ModFileScanData.AnnotationData` 实现发现；bridge 层的 `*LifecycleHooks` 用 `@EventBusSubscriber` + `@SubscribeEvent` 监听 Forge 生命周期事件（`FMLCommonSetupEvent` 等），在事件中调用 domain 的装配入口完成一次装配。Application 不知道 Forge 存在。
4. **ACL adapter 翻译并回调**：ACL 内部 adapter 类（如 `bridge/client/adapter/RenderEventAdapter`）订阅 Forge 游戏事件（用 `@SubscribeEvent` / `@EventBusSubscriber`），把 MC 事件**翻译**为反射调度注解声明的 Application 级参数，再回调到 domain 装配入口构建的 `MethodHandle` 路由表对应的 Application 方法。Application 不知道 Forge 事件存在。
5. **Outbound Port 由 Application 定义、ACL 实现**（与 ADR-0010 一致）：Application 模块定义它需要的 Port 接口，ACL adapter 实现它们；依赖方向 Application ← ACL 不变。**注解归属遵循 MolangMapping 范式：注解定义在 domain 层 API 子包**（不是 application 也不是 ACL），因为注解是契约，contract 归 domain。

> 机制 E 是「ACL 开放契约」的反射调度化：把 ADR-0016 §3 表格中「ACL 做：类型转换、MC API 调用代理」从"ACL 具体类做"收紧为"ACL adapter 做，且对外只通过 Port + 反射调度注解可见"。当前 `EntityRenderSystem` 中 `pushPoseRaw` / `renderItemDirect` / `flushBuffer` / `getEntityTypeId` / `getLlamaDecorColorIndex` / `getEntityTintColor` / `createPoseStackFromMatrix` / `FULL_BRIGHT` 等 static helper 都属于违反 I-5 的泄漏点，必须迁移为 Port 接口（由 Application 定义）或收到 adapter 内部（不对外可见）。

### 4. 与 ADR-0016 四层的映射

本设计**完全嵌入** ADR-0016 四层模型，不增加层：

| ADR-0016 层 | IQF 机制落位 | 多版本 `//?` | 备注 |
|---|---|---|---|
| **Domain**（material / molang / animation / model / behavior / particle / importer） | 机制 A 的 `Registry<T>` / `RegistrySnapshot<T>` / `ManagerEventPublisher` 接口放 `util/`；其余 domain 已基本是 IQF | 零 | 与 ADR-0016 一致 |
| **ACL**（bridge） | 机制 E 的**接口（Port）+ 反射调度注解 + Forge 发现实现 + adapter** 四件套（对齐 `molang/mapping/` 范式）：注解 + 发现接口在 domain 定义；bridge 用 `ModList.get().getAllScanData()` + `ModFileScanData.AnnotationData` 实现发现；`*LifecycleHooks` 在 Forge 生命周期事件触发装配；adapter 订阅 Forge 游戏事件后翻译为反射调度注解参数再回调 Application；`EyelibRuntime` 在此层做唯一 wiring | 集中（ADR-0015 §3） | `//?` 唯一栖息地不变；机制 E 把 ADR-0016 §3 表「ACL 做」从"具体类做"收紧为"adapter 做，对外仅 Port+注解可见" |
| **Application**（client / common / network / capability / track / event） | 机制 B 的 `FramePlan` / `FramePipeline` / `FrameStage` 放 `client/render/pipeline/`；`EntityRenderOrchestrator` 改实例类，构造注入 Registry 与 Port；**所有外部刺激入口改为反射调度注解方法**（`@OnRenderStage` / `@OnEntitySetup` / ...），不再 import bridge 具体类 | 零 | 与 ADR-0016 §5 一致；判据 I-5 强制 |
| **Infrastructure**（mixin / smoke） | 不变 | 按需 | — |

**Composition Root 落位说明**：`EyelibRuntime` 放 `bridge/`（它要 wire Forge `@Mod` + event bus，是 MC 接触点），符合"bridge 是 ACL 翻译层"的定义——它把「MC 启动事件」翻译成「应用层 wiring 调用」。开放问题 OQ-2 保留备选位置讨论。

### 5. 与 Stonecutter 多版本的兼容

本设计与 [ADR-0015](0015-stonecutter-multi-version.md) 完全兼容：

- 所有新增类型（`Registry` / `RegistrySnapshot` / `FramePlan` / `RenderPorts` / `EyelibRuntime`）放在 domain / application 层 → 零 `//?`；
- `EyelibRuntime` 的 wiring 代码中，Forge `@Mod` 与 event bus 部分用 `//?` 切版本，但只在 `bridge/EyelibRuntime` 一个文件内（满足 ADR-0016 §「`//?` 唯一栖息地」）；
- ArchUnit 新规则按 Stonecutter node 各跑一次，与 ADR-0015 §4 一致；
- 不与 `clientsmoke` 多版本化（ADR-0015 §8 Phase 5）冲突。

### 6. 迁移路线图（freeze + 逐步还债）

采用 ADR-0015 §9 已验证的策略。每阶段产出可独立验证，每阶段走 [ADR-0010](0010-hexagonal-architecture.md) 的 G1→G2→G3 验收闸门。

| Phase | 工作 | 验证 | 备注 |
|---|---|---|---|
| **P0 落地本 ADR + ArchUnit 骨架** | 5 条 IQF 规则入 freeze baseline（先全部记录违规数） | `:1.20.1:test` 通过，baseline 文件入库 | 仅文档 + 测试，零业务代码改动 |
| **P1 Registry 抽象** | `util/registry/` 落地 `Registry<T>` + `RegistrySnapshot<T>` + `ManagerEventPublisher` 接口 + 单元测试 | round-trip 单元测试覆盖 | 不动现有 `Manager.INSTANCE` |
| **P2 ACL 开放契约反射调度落地** | (a) 定义反射调度注解族（`@OnRenderStage` / `@OnEntitySetup` / `@OnClientTick` / ...）与 Port 接口族（`PortPoseStack` / `PortBufferSource` / `PortRenderType` / ...）；(b) `EntityRenderSystem` 拆分为 `bridge/<feature>/adapter/` 内的 adapter 类（订阅 Forge 事件 + 翻译 + 回调）；(c) `EntityRenderPorts.*` static 字段改 `RenderPorts` 实例 + 反射调度注解路由表；(d) `EyelibRuntime` 落地 wiring 与注解扫描（借助 (Neo)Forge 反射系统） | 客户端启动正常 + clientsmoke 通过 + `applicationMustNotDependOnBridgeConcreteClasses` baseline 收紧 | 多版本编译（1.20.1 / 1.21.1 / 26.1.2）；P2 是最大切片，建议分子 PR |
| **P3 Registry 替换 Manager.INSTANCE** | 8 个 `Manager` 子类逐个迁移到 `Registry`，删除 singleton；删除 `ManagerEventPublishBridge` | ArchUnit `domainMustNotUseSingletonInstance` baseline 收紧至零 | 分子批，每批一 PR；P3.a / P3.b ... |
| **P4 Application helper 调用全部走 Port** | `EntityRenderOrchestrator` 等 Application 类当前对 `EntityRenderSystem.{pushPoseRaw, renderItemDirect, ...}` 的直接调用全部替换为 Port 接口调用；`FULL_BRIGHT` 等常量收 ACL 内 | `applicationMustNotDependOnBridgeConcreteClasses` 零违规 | 与 P2 配套，可并行 |
| **P5 FramePlan 管道化** | `EntityRenderOrchestrator.onRenderStage` 拆 SetupStage / TickStage / EffectCommitStage；移除 `EntityRenderSystem.{renderCount, errorCount, lastError}` 全局状态，改 `RenderStats` 由 orchestrator 持有 | spec-based 测试覆盖：相同实体集合 → 相同 FramePlan | 最大变更，独立成 PR；可在 P2–P4 稳定后启动（OQ-4） |
| **P6 ArchUnit 收尾 + Lazy-init 收敛** | `getOwner() != entity` 守卫收敛到 `RenderData` 内一处；5 条 IQF 规则全部零违规 | ArchUnit 5 条规则零违规 + 三 node 编译 | 收尾 |

Phase 3 / 4 可并行，但 P5 依赖两者完成。P0–P3 是「消除 singleton / static wiring」的主干；P4 是「时序数据化」的深化，可在主干稳定后单独推进（见 OQ-4）。

### 7. ArchUnit 规则扩展（加入 ADR-0015 §4 freeze 框架）

| 规则 | 检查 | 判据 | 预期 baseline |
|---|---|---|---|
| `domainMustNotUseSingletonInstance` | Domain 包不得出现 `public static final Xxx INSTANCE =` | I-2 | 0（domain 已无） |
| `applicationMustNotHaveStaticInitializer` | Application 包（`client/` / `common/` 等）不得出现 `static {` 业务 wiring | Q-2 | baseline 记录（EntityRenderOrchestrator 等） |
| `applicationMustNotDependOnBridgeConcreteClasses` | Application 不得依赖 ACL（bridge）的具体类（仅允许依赖接口与反射调度注解，文件名匹配 `*Port.java` / `*Handler.java` / `On*.java` 等）；同时禁止直接 import `net.minecraft.*` / `net.minecraftforge.*` / `net.neoforged.*` / `com.mojang.blaze3d.*` | **I-5** + ADR-0016 §5 | baseline 记录（EntityRenderOrchestrator 当前直接调 EntityRenderSystem 静态方法；helper 多处） |
| `aclPublicApiMustBeInterfaceOrAnnotation` | `bridge/**` 中除 `adapter/` 子包外的对外可见类（`public` 顶层类）必须是 interface 或 `@interface`；adapter 子包内的具体类不得被 Application import | I-5（反向校验） | baseline 记录 |
| `noLazyInitScatter` | `getOwner() != entity` 等 init 守卫只能出现在状态所有者类内部 | Q-4 | baseline 记录（4 处散落） |

源码扫描规则（与 ADR-0016 §"源码扫描规则"同机制，ArchUnit 字节码层面查不到的部分用 JUnit 文本扫描）。

> 与 ADR-0015 §4 已有 5 条规则 + 1 条源码扫描规则的关系：本节 5 条规则**追加**进同一 `ArchitectureTest.java` freeze 框架，baseline 文件按 node 分别存储（与 ADR-0016 §"ArchUnit 强制" 已有惯例一致）。

### 8. Composition Root 落位与命名约定

- `bridge/EyelibRuntime.java`：唯一 composition root，由 `@Mod Eyelib` 构造一次。所有跨层 wiring 在此；自身不写业务规则。
- `bridge/client/RenderPorts.java`：实例化 Outbound Port 容器（替代当前 `EntityRenderPorts` static 字段版本），由 adapter 持有并向 Application 注入。命名上保留 `Ports` 后缀以呼应 ADR-0010 Port 词汇。
- `client/render/pipeline/`：机制 B 的 `FramePlan` / `FrameStage` / `FramePipeline` / 各阶段实现。子包新建需补 `package-info.java`（AGENTS.md 注释规则）。
- `util/registry/`：机制 A 的核心抽象。该包已存在 `util/repository/`，新增 `util/registry/` 时需在 `package-info.java` 中明确两包职责差异（repository = 存储契约接口，registry = 注入式快照存储实现）。

**ACL 开放契约包结构（机制 E，对齐 `molang/mapping/` 范式）**：

domain 层的契约定义（参考 `molang/mapping/api/`）：

```
<domain-module>/api/                              # 注解 + 发现接口（零 MC，零 //?）
  ├── On<Name>.java                               # @interface 反射调度注解
  │                                               # （如 @OnRenderStage / @OnEntitySetup，
  │                                               #  参数类型仅 Application 级，不引用 MC；
  │                                               #  @Retention(RUNTIME) 硬性要求）
  ├── <Name>SubscriberDiscovery.java              # @FunctionalInterface 发现接口
  │                                               # （参考 MolangMappingDiscovery）
  └── <Name>SubscriberRegistry.java               # domain 装配入口 + 数据持有
                                                  # （参考 MolangMappingTree；
                                                  #  当前为 INSTANCE singleton，
                                                  #  机制 A 落地后改注入式）
```

bridge 层的实现（参考 `bridge/molang/mapping/`）：

```
bridge/<feature>/                                 # ACL 实现层
  ├── Forge<Name>SubscriberDiscovery.java         # implements <Name>SubscriberDiscovery
  │                                               # 用 ModList.get().getAllScanData()
  │                                               # + ModFileScanData.AnnotationData
  │                                               # 过滤 annotationType == On<Name>.class
  │                                               # Class.forName(memberName) 加载
  ├── <Name>LifecycleHooks.java                   # @EventBusSubscriber + @SubscribeEvent
  │                                               # 监听 FMLCommonSetupEvent 等生命周期事件
  │                                               # 触发 <Name>SubscriberRegistry.setup(...)
  └── adapter/                                    # ACL adapter（包内可见）
      └── <Name>Adapter.java                      # 订阅 Forge 游戏事件（@SubscribeEvent）
                                                  # + 翻译为 Application 级参数
                                                  # + 回调 Registry 路由表
                                                  # 对应的 Application 方法
```

Application 层的使用：

```
application/<module>/                             # 用注解声明处理
  └── <HandlerClass>.java                         # @OnRenderStage 标记类
                                                  # 方法签名用 Application 级类型
                                                  # 不出现 @SubscribeEvent（Forge 注解）
                                                  # 不 import bridge 具体类
```

**强制约束**（与 §7 `aclPublicApiMustBeInterfaceOrAnnotation` 对齐）：
- 注解 + 发现接口 + 装配入口三件套必须先在 domain 定义，bridge 才能实现（与 MolangMapping 范式一致）；
- `bridge/<feature>/` 直接子级可以有 Forge 实现类（`Forge*Discovery`、`*LifecycleHooks`），但**不得被 Application 包 import**（仅 Mod 加载器与 bridge adapter 内部使用）；
- Application 包禁止 import `..bridge..<feature>..`（除 Port 接口）；ArchUnit 规则强制；
- **反射调度注解的发现/调度统一借助 (Neo)Forge 反射系统**（`ModList.get().getAllScanData()` + `ModFileScanData.AnnotationData`），不引入 Java SPI（`ServiceLoader`）机制，不自实现 classpath 扫描。

## Consequences

### 正面

- **编排可离线验证**：Application 层满足 IQF 判据后，`EntityRenderOrchestrator` 的 setup→tick→render 可在 plain JUnit 中用 fake Registry + fake RenderPorts 跑（补全 [ADR-0012](0012-system-testing-strategy.md) Layer 3 的最后一块）。
- **时序 bug 大幅减少**：消除 8 处 singleton + 4 处 static Port 字段 + volatile 全局计数器后，正确性不再依赖类加载顺序与调用时机。
- **副作用的可追溯性**：FramePlan 让每帧的 setup / tick / effect 顺序成为可遍历的数据，调试与渲染诊断（Skill `eyelib-debug` Phase 分析）有了结构化抓手。
- **跨层泄漏可机器捕获**：`applicationMustNotDependOnBridgeConcreteClasses` + `aclPublicApiMustBeInterfaceOrAnnotation` 双向规则把 ADR-0016 §3 + §5 从文档约定升级为 ArchUnit 强制。
- **Application 与 Forge 解耦**：机制 E 让 Application 不再知道 Forge 事件存在；未来切换事件系统（Forge → NeoForge → Fabric）只影响 ACL adapter，Application 不动。
- **类型化的 ACL 接口族**：机制 E 强制每个 MC 概念都有对应 Port 接口，让"eyelib 渲染引擎需要哪些 MC 能力"成为可枚举的类型集合（用于审计、未来移植到非 MC 平台）。
- **不破坏现有架构**：嵌入四层模型，保留 manager / loader / visitor / codec 模式，保留 Stonecutter `//?` 唯一栖息地。

### 负面 / 风险

- **Application 重构面广**：`EntityRenderOrchestrator` 是渲染主入口，P2 / P5 改动敏感。缓解：分 P2（ACL 开放契约反射调度 + RenderPorts 实例化）与 P5（FramePlan 管道化）两步，每步独立 PR + G1→G2→G3 闸门；P2 完成后渲染行为不变（仅 wiring 改），P5 再做行为可验证的重构。
- **间接层性能**：Registry 快照读 + FramePlan 数据化 + 反射调度注解 `MethodHandle` 回调会引入额外开销。缓解：热路径（每帧每实体）评估；注解扫描在 `EyelibRuntime` 启动时一次性完成，运行期仅 `MethodHandle.invokeExact`（与 Forge `@SubscribeEvent` 同等开销，不是每次反射）；`RegistrySnapshot` 复用、`FramePlan` 对象池化作为后续优化（不在本 ADR 范围）。
- **Port 接口族爆炸**：机制 E 要求"每个 MC 概念都有 Port"，可能导致接口数量激增。缓解：ADR-0010 §"过度抽象"缓解措施仍适用——Port 只在至少两个代码路径调用时才创建；单一 use case 直接暴露具体 MC 类型给 adapter 内部。
- **ArchUnit 规则误报**：`noLazyInitScatter` 字符串匹配可能误伤合理用法；`aclPublicApiMustBeInterfaceOrAnnotation` 在过渡期会捕获大量当前违规。缓解：用 baseline 容忍历史违规，新违规 review；规则字符串在 P0 可调。
- **`EyelibRuntime` 成为新 dumping ground 风险**：缓解——它只做 wiring 与注解扫描，不写业务规则；review 时检查它不出现 `if` / `for` 业务逻辑（与 ADR-0010 §"bridge 不能自行发明抽象" 同约束思路）。
- **反射调度的可调试性**：注解驱动的隐式调用比显式 `port.bind(...)` 更难追踪。缓解：`EyelibRuntime` 启动时日志输出扫描到的路由表（注解方法 → MethodHandle）；ADR-0012 Layer 3 测试覆盖每条反射调度路由。
- **(Neo)Forge 反射系统跨版本差异**：`ModFileScanData` 在 Forge 1.20.1 是 `net.minecraftforge.forgespi.language.ModFileScanData`，NeoForge 1.21.1+ 是 `net.neoforged.neoforgespi.language.ModFileScanData`；`EventBusSubscriber` 注解也跨版本差异（Forge 用 `@Mod.EventBusSubscriber(bus = ...)`，NeoForge 用 `@EventBusSubscriber(modid = ...)`）。eyelib adapter 内部需 `//?` 切版本（与 `ForgeMolangMappingDiscovery` / `MolangMappingTreeLifecycleHooks` 现有写法一致）。缓解：差异封装在 bridge 层（ADR-0016 §"//? 唯一栖息地"），Application 看不到；P2 多版本编译验证。

### 不做的

- **不重新引入 Gradle subproject**（与 ADR-0014 冲突）。片段形状由 ArchUnit + 包边界维护。
- **不重写 manager / loader / visitor / codec 模式**（AGENTS.md「核心模式」要求保留）。`Registry<T>` 是 `Manager<T>` 的内部实现演进，对外 `Repository<T>` 接口不变。
- **不一次性重构全部 Application**（与 ADR-0016 §"不做的"同思路）。按 Phase 推进，每批可独立验证。
- **不修改 Domain 层**（Domain 已基本是 IQF；本 ADR 的 P1–P6 主要在 Application / ACL / Composition Root）。
- **不在本 ADR 写 implementation details**（具体代码重构留给 P1–P6 切片 PR；本 ADR 只定义形状约束与验收）。
- **不引入运行时 IoC 容器**（如 Guice / Spring）。`EyelibRuntime` 手写 wiring + 借助 (Neo)Forge 反射系统一次性扫描，不引入第三方框架。
- **不引入 Java SPI（`ServiceLoader`）机制**：注解发现/调度统一走 (Neo)Forge 反射系统，不使用 `META-INF/services/` 描述符。
- **不让 Application 出现 Forge 注解**（`@SubscribeEvent` / `@EventBusSubscriber` 只能在 ACL adapter 内，机制 E 子约束 3 强制）。

## Confirmed Sub-Decisions

以下子决策在 ADR 接受时一并确认（@TT432 2026-06-28）：

- **D-1 `Registry<T>` 命名**：保留 `Registry<T>`。`Repository<T>` 接口契约不变（`Registry` 实现 `Repository`）。
- **D-2 Composition Root 位置**：`bridge/EyelibRuntime`。符合 ADR-0016「bridge 是 MC 翻译层」——它需要 wire Forge `@Mod` + event bus。
- **D-3 `FramePipeline` 引入成本**：接受空间结构扩张（机制 B 的 5 个新数据类型）。
- **D-4 P5 范围**：路线图 P0–P6 本轮全做。允许在 P4 完成后重新评估 P5（FramePlan 时序数据化）。
- **D-5 `noLazyInitScatter` 通用化**：当前用具体字符串 `getOwner() != entity`，P6 再升级为通用「每个 mutable 字段只能在一处 init 守卫」模式检测。
- **D-6 反射调度注解与发现接口的 domain 归属**：注解就近放所属业务域的 `api/` 子包（与 MolangMapping 一致，如 `client/render/api/`）；跨模块共用的放 `util/event/api/`。
- **D-7 反射调度事件类型的覆盖范围**：实现机制已定——`ModList.get().getAllScanData()` + `ModFileScanData.AnnotationData`。**本轮覆盖范围**：先迁移 `EntityRenderSystem` 已订阅的 4 类事件（`EntityJoinLevelEvent` / `RenderLevelStageEvent` / `EntityTickEvent` / `RenderLivingEvent.Pre`），P2 落地时按需扩展。**注解方法签名编码**：Forge 事件字段（如 `RenderLevelStageEvent.Stage`）用 domain 枚举编码（不用字符串）。**注解策略**：一次性事件（setup）与每帧事件（render）先用同一注解策略，不区分；P6 再按性能数据决定是否分策略。
- **D-8 Port 接口粒度**：按概念聚类（如 `PoseStackPort` + `ItemRenderPort` + `EntityQueryPort`），不按方法一对一。

## Verification

每 Phase 走 [ADR-0010](0010-hexagonal-architecture.md) G1→G2→G3 闸门（Skill `eyelib-hexagonal-gates`）：

- **G1（ArchUnit 隔离）**：§7 表中 5 条新规则在对应 Phase 末尾达到预期状态（baseline 收紧或零违规）。
- **G2（spec-based 测试）**：
  - P1：`util/registry/` 单元测试 round-trip；
  - P2：每个反射调度注解至少有一条契约测试（事件 → adapter 翻译 → Application 方法被调用，参数正确）；
  - P5：`FramePipeline.run(world, FramePlan.initial(...))` 重放测试（同输入 → 同输出，命中 Q-1 / Q-5）。
- **G3（RenderDoc / 运行时）**：P2 / P3 / P4 / P5 每阶段完成后，相同 .mcpack 数据在迁移前后产生相同 RenderDoc 截帧（与 ADR-0010 §Verification 同标准）。
- **多版本编译**：每个 Phase 完成时 `:1.20.1` / `:1.21.1` / `:26.1.2` 三 node `jetbrain_build_project` 退出码 0（ADR-0015 §Verification）。

**成功标准**（与 ADR-0010 / 0016 §"成功标准" 同风格）：

- I-2 零违规：domain 层无 `public static final Xxx INSTANCE`（ArchUnit 强制）；
- Q-2 零违规：application 层无 `static {}` 业务 wiring（ArchUnit 强制）；
- **I-5 零违规**：application 层不依赖 bridge 具体类、不 import MC / Forge 原生类型（ArchUnit 强制，机制 E 落地后达成）；
- Q-4 零违规：`getOwner() != entity` 守卫仅在 `RenderData` 内一处；
- I-1 强制：application 调 ACL 只通过 Port 接口与反射调度注解（ArchUnit 强制）；
- Q-1 / Q-5 证明：`FramePipeline.run` 重放测试在 domain + application 模块单元测试中通过；
- 多版本不变：1.20.1 / 1.21.1 / 26.1.2 三 node 编译通过 + RenderDoc 截帧等价。

## Related

- [ADR-0010](0010-hexagonal-architecture.md) — 六边形架构（本 ADR 的目标扩展到 Application 层）
- [ADR-0012](0012-system-testing-strategy.md) — 三层测试策略（本 ADR 的 P5 补全其 Layer 3 缺失部分）
- [ADR-0014](0014-flat-merge.md) — flat-merge（本 ADR 不重新引入 subproject）
- [ADR-0015](0015-stonecutter-multi-version.md) — Stonecutter 多版本 + ArchUnit freeze（本 ADR 复用其 §4 框架）
- [ADR-0016](0016-bridge-extraction-standard.md) — 库隔离标准（本 ADR 补全 Application 层形状判据；§3 已加补强小注）
- [docs/concepts/architecture.md](../concepts/architecture.md) — 系统架构总览（更新引用本 ADR）
- **机制 E 范本**：
  - `src/main/java/io/github/tt432/eyelib/molang/mapping/api/`（domain 注解 + 发现接口 + 装配入口）+ `src/main/java/io/github/tt432/eyelib/bridge/molang/mapping/`（Forge 发现实现 + 生命周期 hook）+ `src/main/java/io/github/tt432/eyelib/client/molang/MolangQuery.java`（Application 层用注解声明）；
  - `clientsmoke/`（`@ClientSmoke` 注解 + `ModFileScanData` 发现，Skill `eyelib-clientsmoke` 文档）。
  - 本 ADR 的所有 ACL 开放契约实现都必须对齐此范式。
- Skill `eyelib-hexagonal-gates` — G1→G2→G3 验收闸门（本 ADR 每 Phase 走此流程）
- Skill `eyelib-domain-extraction` — Port 提取操作手册（本 ADR 的机制 B / C 复用其 wiring 模式）
