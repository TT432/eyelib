# ADR-0016: 库隔离标准 — DDD 分层与 ACL 职责边界

**Status:** Accepted
**Date:** 2026-06-25
**Author:** @TT432
**Related:** 补充 [ADR-0010](0010-hexagonal-architecture.md)、[ADR-0015](0015-stonecutter-multi-version.md)

## Context

### 问题:ACL 太扩大化

当前架构是 `MC → bridge(ACL) → eyelib`,但 ACL(防腐层)承担了过多职责。按 DDD 的 Anti-Corruption Layer 模式(Eric Evans《DDD》原定义;Microsoft Azure Architecture Center 转述):

> The anti-corruption layer contains all the logic necessary to translate between the two systems.
> **Avoid placing business rules or orchestration in the layer.** It's important to focus the anti-corruption layer on translation logic.

当前违规:

1. **ACL 包含业务规则** — `bridge/material/BrRenderTypeFactory` 不只翻译 MC RenderType,还内含 Bedrock 材质语义(BrRenderState → RenderType 的映射规则)。映射规则属于 domain,不属于 ACL。
2. **编排逻辑直接接触 MC API** — `client/EntityRenderSystem`(41 处 `//?`)、`client/render/EyelibLivingEntityRenderer` 等编排层文件直接 import MC 类并内嵌版本条件,绕过了 ACL。编排(application 层)应通过 ACL 接触 MC,不直接接触。
3. **domain 被基础设施腐蚀** — domain 模块(`material/`、`molang/` 等)存在 `net.minecraft.*` import,违反 Infrastructure Ignorance 原则(ADR-0010 已识别,ArchUnit 被 ADR-0014 删除后无强制)。
4. **`//?` 散布** — 渲染核心 9 文件 139 处 `//?`,86% 是机械改名(L1),但版本条件散布在业务/编排层,调试时需同时读多版本路径。

### 根因

没有按 DDD 的**限界上下文(BC)**和**层(Layer)**划分库的职责边界。当前包按功能域分(`material/`、`client/`、`bridge/`),但没定义"哪个层允许接触什么"。ACL 既做翻译又做业务,编排层既做编排又做 MC 适配。

## Decision

### 1. 四层模型(DDD Layers as Libraries)

按 DDD 分层模式,eyelib 的包划分为四个**职责层**(每层是逻辑库边界,物理上仍是单 Gradle project):

| 层 | DDD 角色 | 现有包 | 接触 MC? | 允许 `//?`? |
|---|---|---|---|---|
| **Domain** | 领域模型(核心) | `material/` `molang/` `animation/` `model/` `particle/` `behavior/` `importer/` | **否** | **否** |
| **ACL**(bridge) | 防腐层(翻译) | `bridge/` | **是**(唯一翻译入口) | **是**(唯一栖息地) |
| **Application** | 应用层(编排) | `client/` `common/` `network/` `capability/` `track/` `event/` | **否**(通过 ACL) | **否** |
| **Infrastructure** | 基础设施(MC 胶水) | `mixin/` `smoke/` | **是**(字节码/生命周期) | 按需 |

### 2. 依赖方向(单向)

```
Application ──→ Domain
     │            ↑
     ↓            │
   ACL ───────────┘  (ACL 依赖 Domain,翻译用 Domain 概念)
     │
     ↓
  MC Runtime (外部系统)
```

**规则**:
- Domain → 无(只依赖 JDK + 自身)。零 `net.minecraft.*`/`net.minecraftforge.*`/`net.neoforged.*`/`com.mojang.blaze3d.*`(Persistence Ignorance + Infrastructure Ignorance)。
- ACL → Domain + MC。ACL 依赖 domain 概念做翻译,依赖 MC API 做适配。
- Application → Domain + ACL。编排层通过 ACL 的翻译接口接触 MC,不直接 import MC 版本特定包。
- Infrastructure → MC。Mixin 直接操作 MC 字节码,是技术基础设施。

**禁止反向依赖**:Domain 不依赖 ACL/Application;ACL 不依赖 Application。

### 3. ACL 职责边界(核心约束)

ACL(Anti-Corruption Layer)的唯一职责是**翻译**(translation):

| ACL **做** | ACL **不做** |
|---|---|
| MC 概念 ↔ Domain 概念的双向映射 | Bedrock 业务规则(材质继承/动画状态机/Molang 求值) |
| 版本差异封装(`//?` 的唯一栖息地) | 渲染编排/事件调度/加载流程 |
| 类型转换(ResourceLocation↔Identifier、RenderType↔PortRenderPass) | 领域模型的 CODEC 解析 |
| MC API 调用代理(纹理上传、顶点写入、事件注册) | 决定"何时渲染什么"的编排决策 |

**判定标准**:一个类属于 ACL 当且仅当它的核心职责是**在两种语言之间翻译**。如果它定义了"Bedrock 规范说什么"→ Domain;如果它决定了"什么时候执行"→ Application;如果它做了"MC 概念 ↔ Domain 概念的转换"→ ACL。

### 4. 通用语言(Ubiquitous Language)分离

不同 BC 的同名概念允许有不同形态(DDD 原则:不强行统一术语):

- **Bedrock Domain** 说:`BrMaterial`、`BrRenderState`、`BrAnimation`、`MolangValue`
- **MC Runtime** 说:`RenderType`、`VertexConsumer`、`PoseStack`、`DynamicTexture`
- **ACL** 的职责:在两者之间翻译,不消除差异,不统一术语

### 5. Application 层约束

Application 层(编排)通过 ACL 接触 MC,自身保持版本无关:

- **禁止** import 版本特定 MC 包(`net.minecraftforge.*`、`net.neoforged.*`、`com.mojang.blaze3d.{pipeline,platform}.*`)
- **禁止** `//?`(版本条件只在 ACL)
- **允许** import 三版本完全一致的 MC 类(Entity、Level、ItemStack 等,需白名单验证)
- **必须** 通过 ACL 的翻译接口访问版本差异的 MC API

### 6. `//?` 的唯一栖息地

`//?` Stonecutter 条件化是版本差异的载体。按本标准:
- **Domain** 层:零 `//?`(Bedrock 规范版本无关)
- **ACL** 层:所有 `//?` 集中在此(版本翻译)
- **Application** 层:零 `//?`(编排逻辑版本无关)
- **Infrastructure** 层:按需(Mixin 可能需要版本条件)

## Consequences

### 正面

- **Domain 可离线验证**:零 MC 依赖 → JUnit 独立验证,oracle 来自 Bedrock 规范(ADR-0010 目标落地)。
- **MC 版本升级可控**:版本差异只在 ACL,升级时扫描 `bridge/` 一个包。
- **调试聚焦**:编排层单一代码路径,版本差异不再干扰业务逻辑阅读。
- **ACL 职责纯粹**:翻译层只翻译,不腐化 domain,不承担编排负担。
- **机械约束可恢复**:ArchUnit 可按层检查 import 规则(ADR-0015 §4 恢复)。

### 负面 / 风险

- **翻译层间接调用**:Application 调 ACL 调 MC,多一层间接。对渲染热路径需评估性能,但可维护性优先。
- **职责分离成本**:现有混合代码(BrRenderTypeFactory = 翻译 + 业务规则)需要拆分:翻译部分留 ACL,业务规则移 domain。
- **白名单维护**:三版本一致的 MC 类白名单需随版本更新验证。
- **ACL 粒度选择**:翻译粒度太粗会泄漏 MC 概念到 Application;太细会接口爆炸。按 ADR-0010 缓解措施:"Port 只在被至少两个代码路径调用时才创建"。

### 不做的

- 不一次性重构全部 100+ 文件。按 BC/功能域逐步推进,每批可独立编译验证。
- 不移除 L1 `//?`(import/方法名机械替换)—— 它们在 ACL 内是合理的翻译手段。
- 不改变 ADR-0015 的 Stonecutter 项目模型(单 src + `//?` + 版本门)。
- 不强行统一 BC 间的术语(DDD 原则:接受差异)。

## Verification

### ArchUnit 强制(已落地)

`src/test/java/io/github/tt432/eyelib/architecture/ArchitectureTest.java` 用 freeze 模式维护多条规则,baseline 存于 `build/archunit_store/<node>/`:

| 规则 | 检查 | 当前状态 |
|---|---|---|
| `domainMustNotDependOnMinecraft` | DOMAIN_CLASSES(`material/`、`molang/`、`animation/`、`model/`、`particle/`、`behavior/`、`importer/`、`util/`) 不依赖 `BANNED_MC`(`net.minecraft.*`/`net.minecraftforge.*`/`net.neoforged.*`/`com.mojang.blaze3d.*`,扣除 MC_WHITELIST=DFU/NBT/FriendlyByteBuf/ExtraCodecs) | ✅ 零违规 |
| `domainMustNotDependOnOrchestration` | DOMAIN_CLASSES 不依赖 ORCHESTRATION(`io.github.tt432.eyelib.{client,common,network,capability,attachment,track,event,bridge,mixin,smoke,debug}..`) | ✅ 零违规 |
| `versionSpecificMcOnlyInBridgeOrInfrastructure` | 非 `io.github.tt432.eyelib.{bridge,mixin,smoke,debug}..` 的类不依赖 VERSION_SPECIFIC_MC(`net.minecraftforge.*`/`net.neoforged.*`/`com.mojang.blaze3d.{pipeline,platform,systems}.*`) | ✅ 零违规 |
| `aclMustNotDependOnApplication` | `io.github.tt432.eyelib.bridge..` 的类不依赖 APPLICATION_CLASSES(`io.github.tt432.eyelib.{client,common,network,capability,attachment,track,event}..`) | ⚠️ baseline 360 行(1.20.1)/355 行(1.21.1) —— 过渡期债务,见下 |
| `applicationMustNotDependOnDomainInternals` | APPLICATION_CLASSES 不依赖 `io.github.tt432.eyelib.<module>.internal..`(Domain 内部实现子包) | ✅ 零违规(前瞻性:Domain 尚未划分 internal/,规则一旦就位未来加 internal/ 时自动捕获) |

### 源码扫描规则(非 ArchUnit)

`src/test/java/io/github/tt432/eyelib/architecture/StonecutterCommentPlacementTest.java`(JUnit 文本扫描,因 ArchUnit 字节码层面查不到 `//?` 注释):

| 检查 | 当前状态 |
|---|---|
| `//?` Stonecutter 条件化注释只出现在 ACL(bridge) 和 Infrastructure(mixin/smoke/debug) | ⚠️ baseline 43 文件 —— 过渡期债务,见下 |

baseline 存于 `build/archunit_store/stonecutter-comment-baseline.txt`(不分 node,因 `src/main/java/` 是 Stonecutter 处理前的模板源,各版本一致)。

### 迁移原则

1. **先定义 BC 边界**(本 ADR),再抽 Port 接口,再迁移代码。
2. **每批迁移**:① 判定每个类的归属层 ② 翻译类移 ACL,业务规则移 domain,编排留 application ③ 三版本编译 + 渲染验证。
3. **ArchUnit 恢复**(ADR-0015 §4):按层 freeze baseline → 逐批收紧。

### 成功标准

- Domain 包零 `net.minecraft.*`/`net.minecraftforge.*`/`net.neoforged.*` import(ArchUnit 强制)。✅
- Application 包零 `//?` + 零版本特定 MC import(白名单除外)。规则 3 已强制版本特定 MC 部分;`//?` 散布已纳入 `StonecutterCommentPlacementTest` 源码扫描(baseline 待收紧)。
- `//?` 只出现在 `bridge/` 和 `mixin/` 包。已纳入 `StonecutterCommentPlacementTest` 强制。
- ACL(bridge/)只含翻译逻辑,不含业务规则或编排。**过渡期未达成**(见下)。

### 过渡期已知债务(待阶段 2 抽 Port)

当前 ArchUnit 五条规则冻结为 0 违规(规则 4 baseline 已记录)或前瞻性 0 违规(规则 6),但 §3 理想的"ACL 只翻译"尚未完全落地:

- **`*Hooks`/`*LifecycleHooks`/Forge 注册类整体迁 `bridge/`**:这些类同时包含 Forge 事件订阅注册(§3 表归 ACL)和订阅后的编排逻辑(§3 标准归 Application)。本阶段按 §5 强约束(application 禁止版本特定 MC)整体迁 bridge/,阶段 2 需将编排部分抽回 application,bridge 只留 Port 实现 + Forge 注册胶水。
- **`bridge/` 反向引用 application 层运行时数据类**:例如 `bridge/capability/*` 引用 `capability/component/*`(application 数据容器),违反 §"禁止反向依赖:ACL 不依赖 Application",由规则 4 `aclMustNotDependOnApplication` baseline 记录(360+ 行违规,32 个 bridge 源文件)。这类数据类本质是 Port 的运行时实体实现,阶段 2 抽 Port 后归位。
- **`//?` 散布在 application/domain**:由 `StonecutterCommentPlacementTest` baseline 记录(43 文件,含 model/Model.java、importer/* 等 domain 文件,以及 client/render/、client/loader/ 等 application 文件)。阶段 2 应将版本差异逻辑迁 bridge/ 或抽 Port。
- **规则 6 是前瞻性占位**:Domain 当前未划分 `internal/` 子包,规则 6 baseline 为 0。未来 Domain 抽内部实现到 `<module>/internal/` 时,规则 6 自动捕获 Application 的越界引用。
