# Feature Landscape: eyelib-util 模块

**Domain:** Forge 渲染库共享工具模块
**Researched:** 2026-05-10
**Source files mapped:** root/util/* (32 Java files), core/util/* (5 files), eyelib-attachment/codec/stream/* (5 files)
**Overall confidence:** HIGH

## 目标包结构（eyelib-util 内）

```
io.github.tt432.eyelibutil/
├── collection/     ← 容器、集合、流辅助工具
├── codec/          ← Codec/编解码基础设施
│   └── stream/     ← 字节流编解码（来自 attachment 的 pull-up）
├── math/           ← 数学、变换、颜色辅助工具
├── time/           ← 定时器和时间步进状态
├── texture/        ← 纹理路径推导
├── color/          ← 颜色通道编解码
├── resource/       ← ResourceLocation 辅助工具
├── search/         ← 搜索/索引结果辅助工具
└── loader/         ← 原生库加载
```

---

## 表头功能（Table Stakes）

一旦缺失，用户会立即感受到产品功能不完整。

| 功能 | 为何是表头功能 | 复杂度 | 现有源码 | MC依赖 | 备注 |
|------|---------------|--------|---------|--------|------|
| **collection: 集合辅助工具** | 整个代码库中频繁使用；Blackboard 是 Molang/行为逻辑中的核心数据结构 | 低 | `util/Blackboard.java`, `util/Lists.java`, `util/Collectors.java`, `util/EntryStreams.java`, `util/ListHelper.java`, `util/ImmutableFloatTreeMap.java`, `core/util/collection/ListAccessors.java` | 无（ImmutableFloatTreeMap 除外） | ImmutableFloatTreeMap 有码 codec 依赖（`com.mojang.serialization.Codec`），是 MC 传递依赖，非直接 MC 类型 |
| **codec: 编解码基础设施** | EyelibCodec 提供 JOML 向量 + AABB 的 codec；CodecHelper 提供通用 codec 辅助工具；TupleCodec 提供元组 codec 支持。行为事件过滤、动画关键帧、粒子定义全部依赖这些 codec | 中 | `util/codec/EyelibCodec.java`, `CodecHelper.java`, `TupleCodec.java`, `Tuple.java`, `ChinExtraCodecs.java`, `EitherHelper.java`, `DispatchedMapCodec.java`, `KeyDispatchMapCodec.java`, `core/util/codec/Eithers.java` | 是：EyelibCodec 导入 `net.minecraft.util.ExtraCodecs`、`net.minecraft.world.phys.AABB`；CodecHelper 也导入 MC 类 | 这是 MC 耦合最密集的类别之一，但属于 eyelib-util 作为 Forge 模块的范围 |
| **math: 数学辅助工具** | EyeMath（lerp、钳位）、MathHelper（角度/弧度）、Curves（曲线插值）、FastColorHelper（颜色通道数学）在整个动画、渲染和 UI 系统中使用 | 低 | `util/math/EyeMath.java`, `MathHelper.java`, `Curves.java`, `Shapes.java`, `FastColorHelper.java` | 无 | 纯 Java + 第三方数学库依赖 |
| **time: 定时器** | SimpleTimer 用于暂停/恢复计时；FixedStepTimerState 提供固定步长时间步进状态数学。动画时间轴、粒子生命周期全部依赖 | 低 | `util/SimpleTimer.java`, `core/util/time/FixedStepTimerState.java` | 无 | 简单的 System.nanoTime() 封装，无外部依赖 |
| **resource: 资源路径** | `ResourceLocations.of()` / `mod()` 是创建 ResourceLocation 的事实标准方式。跨 Molang 查询、渲染同步、动画定义使用 | 低 | `util/ResourceLocations.java` | 是：直接使用 `net.minecraft.resources.ResourceLocation` | 1 个文件，但有 **高进口站点影响** — 10+ 个消费者 |

---

## 差异化功能（Differentiators）

非必需品，但一旦存在，能显著提升模块价值。

| 功能 | 价值主张 | 复杂度 | 现有源码 | MC依赖 | 备注 |
|------|---------|--------|---------|--------|------|
| **codec/stream: 流编解码器** | 当前 `eyelib-attachment` 拥有 `StreamCodec`、`StreamDecoder`、`StreamEncoder`、`EyelibStreamCodecs` — 但这些是其他子模块（particle、network）需要的通用基础设施。将其拉入 eyelib-util 供所有模块使用。 | 中 | `eyelib-attachment/.../codec/stream/*.java` (5 个文件) | 是：`FriendlyByteBuf`、`Nbt*`、`ResourceLocation` | 从子模块拉取的代码；attachment 保留其 `DataAttachment*` 和 `network/*` 合约 |
| **texture: 纹理路径** | 平台无关的纹理路径推导（TexturePaths）+ MC 感知的兼容适配器（TexturePathHelper）。渲染参数使用 TexturePathHelper；Material 模块也可受益。 | 低 | `core/util/texture/TexturePaths.java`, `util/client/texture/TexturePathHelper.java` | 是（TexturePathHelper） | 2 个文件，低风险合并 |
| **color: 颜色编码** | 平台无关的颜色通道变换。用于纹理处理流水线。 | 低 | `core/util/color/ColorEncodings.java` | 无 | 1 个文件，现已在 core，直接迁移 |
| **search: 搜索辅助工具** | `Searchable` 接口 + `SearchResults`。加载器和 UI 中使用，用于资源发现过滤。 | 低 | `util/search/Searchable.java`, `SearchResults.java` | 无 | 2 个文件，低影响，在适当时机提取 |
| **loader: 原生库加载** | `SharedLibraryLoader` 处理各平台的 .dll/.so/.dylib 解包和加载。非所有模块都需要，但若不存在对某些平台至关重要。 | 低 | `util/SharedLibraryLoader.java` | 无 | 1 个文件，自包含 |

---

## 反功能（Anti-Features）

必须显式排除的功能 — 绝不应出现在 eyelib-util 中。

| 反功能 | 排除原因 | 应去往何处 |
|--------|---------|-----------|
| **业务逻辑辅助工具** (`AnimationApplier`) | `AnimationApplier` 将动画骨骼变换应用到模型渲染数据 — 这不是通用工具，是特定于动画运行时的业务逻辑 | → `client/animation/`（动画运行时所有者） |
| **领域特定模型合并** (`Models.merge()`) | `Models` 执行多模型骨骼合并，使用 importer 拥有的 `Model` 类型和 `GlobalBoneIdHandler` — 这是**单一消费者**模型逻辑 | → `client/model/`（模型领域所有者） |
| **外部工具桥接** (`ModBridgeServer`, `BBModelSink`) | 套接字服务器/Blockbench 模型接收器 — 仅用于开发工具工作流，不是通用工具代码 | → `mc/impl/modbridge/`（modbridge 所有者） |
| **兼容层 shim**（纯委托的伪工具） | `ListHelper` 和 `EitherHelper` 仅作为 `core/util` 对等体的兼容性适配器存在 — 这些是提取过程的产物，不应在新模块中永久保留 | 一旦所有调用方改用 `core/util/*` 直接等效项，则删除 shim |
| **功能特定数据存储**（`DataAttachment*`、`DataAttachmentContainer` 等） | 数据附加（data-attachment）合约、存储、容器、类型注册 — 是独立的功能领域，属于 `eyelib-attachment` | 保留在 `eyelib-attachment` 模块中 |
| **厂商特定的 MC 包配置**（`mc/impl/**` 类） | `InventoryModelResourceLocations`、`ModBridgeModelUpdateEvent` 等类存在于 `mc/impl` 下，作为 MC/Forge 运行时适配器 — 这些是平台胶水，不是可共享的工具 | 保留在 `mc/impl/` 下各自的功能所有者处 |

---

## 类别依赖关系图

提取的关键约束：**类别 B 不能在自己的消费者完成迁移之前移入 eyelib-util，除非 B 的消费者也被同时移入或已经迁出。**

```
collection ─────────────────────────────────────────────┐
   (ImmutableFloatTreeMap 依赖 codec)                   │
                                                        ▼
math ───────────────────────────────► codec ◄── resource (ResourceLocation)
  (无依赖)                           (MC ExtraCodecs,    (无内部依赖，
                                      AABB, JOML)        MC ResourceLocation)

time ───────────────────────────────► codec/stream
  (无依赖)                           (FriendlyByteBuf, MC NBT)

color ───► texture
  (无依赖)  (TexturePathHelper 适配 TexturePaths)

search ──► collection
  (使用集合类型)

loader ──► (无依赖，完全独立)
```

**关键见解：**
- `collection`（ImmutableFloatTreeMap 除外）、`math`、`time`、`color`、`loader` — **零内部依赖**。可按任意顺序提取。
- `codec` 是重依赖项 — `ImmutableFloatTreeMap` 和 `resource` 都依赖它。但 `codec` 自身依赖 MC 类型，因此可以在 eyelib-util 的生命周期中更晚迁移。
- `codec/stream` 同时依赖 `codec`（概念上）和 MC 网络类型 — 应最后处理。

---

## 迁移复杂度矩阵（由易到难）

### 第 1 层：零依赖、零 MC — 纯 Java 类别
**适用于提取的第一阶段。** 可以逐个类别移动，也可以批量移动。

| 类别 | 文件数 | 消费者估算 | 每个消费者的改动 | 风险 |
|------|--------|------------|-----------------|------|
| `time` | 2（SimpleTimer, FixedStepTimerState） | ~3-5 | 仅包变更 | 极低 |
| `color` | 1（ColorEncodings） | ~1-2（纹理流水线） | 仅包变更 | 极低 |
| `loader` | 1（SharedLibraryLoader） | ~1（启动） | 仅包变更 | 极低 |
| `math` | 5 | ~10-15（动画、UI、渲染访问器） | 仅包变更 | 低 |
| `collection` (ImmutableFloatTreeMap 除外) | 5-6 | ~15-20 | 仅包变更 | 低 |
| `search` | 2 | ~3-5（加载器、UI） | 仅包变更 | 极低 |

### 第 2 层：有 MC 依赖，但仍然是叶子类别的类别
**在基础架构就绪后提取。** 需要 eyelib-util 已经拥有其依赖项。

| 类别 | 文件数 | 消费者估算 | MC 耦合 | 风险 |
|------|--------|------------|---------|------|
| `resource` | 1 | **高** — 10+ 个消费者（Molang 查询、渲染同步、动画定义） | ResourceLocation | 中 — 由于消费者数量 |
| `texture` | 2 | ~2-3（渲染参数、材质） | ResourceLocation（在 TexturePathHelper 中） | 低 |
| `ImmutableFloatTreeMap` (collection) | 1 | ~4-5（动画关键帧、采样器、通道） | Codec（传递地通过 codec 类别） | 低 — 但需要先迁移 codec |

### 第 3 层：基础设施类别（高内部 fan-in）
**codec 类别在依赖它的其他工具之前进行迁移。** 这是整个 eyelib-util 价值主张中变化最密集的类别。

| 类别 | 文件数 | 消费者估算 | MC 耦合 | 风险 |
|------|--------|------------|---------|------|
| `codec` | 9（来自 root/util） + 1（core/util） = 10 | **高** — 20+ 个消费者（动画、行为、粒子、导入器） | 很高：ExtraCodecs、AABB、Codec、Either、JOML | **高** — 由于导入站点数量和 MC 传播 |

### 第 4 层：从子模块拉取的代码
**在基础 root/util 迁移完成且 eyelib-util 稳定后处理。**

| 类别 | 文件数 | 消费者估算 | 来源模块 | 风险 |
|------|--------|------------|---------|------|
| `codec/stream` | 5 | ~3-5（attachment、particle、network） | `eyelib-attachment` | 中 — 跨模块协调 |

---

## 进口站点影响摘要

| 现有包 | 影响等级 | 调用站点模式 |
|--------|---------|-------------|
| `util/codec/*` | **高** (20+ 个文件) | `BrAnimationEntry.*`、`BrBoneKeyFrame.*`、`LogicNode`、`ComplexFilter`、`ComponentGroup`、`BrParticle*` 类 — 少数顶级消费者，分布广泛 |
| `util/math/*` | **中** (10-15 个文件) | `BrClipExecutor`、`ModelVisitor`、`AnimationView`、`EyelibManagerScreen` — 集中在动画和 UI |
| `util/ResourceLocations` | **中** (10+ 个文件) | `ClientRenderSyncService`、`BrAnimationEntryDefinition`、`MolangQuery` — 横跨渲染、Molang、动画 |
| `util/collection/*` | **中** (10+ 个文件) | `ImmutableFloatTreeMap` 在 BrAnimationChannel/Entry/Sampler 中；`Lists`、`EntryStreams`、`Blackboard` 在各种行为/渲染消费者中 |
| `util/client/*` | **低** (移入领域所有者，非 eyelib-util) | AnimationApplier/Models → 动画/模型所有者 |
| `util/modbridge/*` | **低** (移入 mc/impl) | ModBridgeServer/BBModelSink → modbridge 所有者 |
| `util/search/*` | **低** (3-5 个文件) | BrAttachableLoader 和其他加载器 |
| `core/util/*` | **低** (当前通过 shim 调用) | 当前调用方使用 `ListHelper` / `EitherHelper`；将迁移为直接使用 eyelib-util |

---

## 阶段提取建议

基于 "哪些文件的所有者可以被**干净**拉出，而不会造成级联引入断裂"：

### 阶段 1：将单消费者工具移入其功能所有者（非 eyelib-util）
```
util/client/AnimationApplier.java  →  client/animation/
util/client/Models.java            →  client/model/
util/modbridge/ModBridgeServer.java → mc/impl/modbridge/
util/modbridge/BBModelSink.java    →  mc/impl/modbridge/
```
**理由：** 这些清除了 util/ 混乱，同时不增加 eyelib-util API 表面积。零回归风险 — 相同代码，不同包。

### 阶段 2：提取 "第 1 层" 类别 — time、color、loader、math、search
```
eyelib-util:
  time/    ← util/SimpleTimer.java + core/util/time/FixedStepTimerState.java
  color/   ← core/util/color/ColorEncodings.java
  loader/  ← util/SharedLibraryLoader.java
  math/    ← util/math/*.java (5 个文件)
  search/  ← util/search/*.java (2 个文件)
```
**理由：** 零内部依赖，零 MC 导入（math 可能有 fastutil/joml，但无 MC）。低风险，快速胜利，从头建立 eyelib-util 构建基础设施。

### 阶段 3：提取 collection（ImmutableFloatTreeMap 除外）
```
eyelib-util:
  collection/ ← util/Blackboard.java, Lists.java, Collectors.java, EntryStreams.java +
                core/util/collection/ListAccessors.java
                
删除 shim: util/ListHelper.java（所有调用方使用 eyelib-util 直接等价项）
```
**理由：** 仍然无 MC 依赖，但消费者数量更高。

### 阶段 4：提取 resource + texture + color 整合
```
eyelib-util:
  resource/ ← util/ResourceLocations.java
  texture/ ← core/util/texture/TexturePaths.java + util/client/texture/TexturePathHelper.java
```
**理由：** 有 MC 依赖，但有可控的消费者数量。ResourceLocations 有最多的消费者，但只有 1 个文件。

### 阶段 5：提取 codec 基础设施（最复杂）
```
eyelib-util:
  codec/ ← util/codec/*.java (9 个文件) + core/util/codec/Eithers.java (1 个文件)
  collection/ImmutableFloatTreeMap.java ← 由于 codec 依赖，最后迁移
删除 shim: util/codec/EitherHelper.java
```
**理由：** 这是变动最密集的类别（20+ 个消费者，MC 耦合）。需要周全规划 — 可能需要在根端进行兼容性导入重写或 IDE 重构自动化。

### 阶段 6：从子模块拉取代码
```
eyelib-util:
  codec/stream/ ← eyelib-attachment/.../codec/stream/*.java (5 个文件)
```
将 `eyelib-attachment` 依赖更新为使用 `eyelib-util` 作为流编解码器。

---

## 特殊注意事项

### ImmutableFloatTreeMap 的归属
`ImmutableFloatTreeMap` 名义上是集合（作为密封的数据结构），但**在构建时需要 codec**（使用 `CodecHelper` 和 `com.mojang.serialization.Codec` 进行序列化）。选项：
- **推荐：** 将其放入 `eyelib-util` 的 `collection/`，并声明 codec 作为传递依赖。理想情况下，codec 特定的部分可以被提取到 `codec/` 中，其他部分保留在 `collection/` 中。
- **备选：** 整体放入 `codec/` — 但违背了领域逻辑（它是一个数据容器，不仅仅是一个编解码器）。

### StreamCodec 与已有的 codec 命名空间
`eyelib-attachment` 中的 `StreamCodec`/`StreamDecoder`/`StreamEncoder`/`EyelibStreamCodecs` 是通用的字节流编解码器（`FriendlyByteBuf` 读取/写入），与 com.mojang.serialization 的 `Codec`（JSON/NBT 序列化）不同。它们应当被放在 eyelib-util 中自己的 `codec/stream/` 包下，以明确区分关注点。

### 可删除的 Shim
提取后，以下文件应被**删除**（非迁移）：
- `util/ListHelper.java` — → 调用方直接使用 `ListAccessors.first/last`
- `util/codec/EitherHelper.java` — → 调用方直接使用 `Eithers.unwrap`
- `util/package-info.java` — 如果 util/ 完全不为空，删除包级注解

---

## MVP 推荐

优先考虑：
1. **阶段 1**: 重新安置单消费者工具（AnimationApplier、Models、ModBridge*）→ `eyelib-util` 清零，零 MR 风险
2. **阶段 2**: 提取 time + color + loader + math + search → 在 eyelib-util 中建立构建 + 测试基础设施
3. **阶段 3**: 提取 collection（无 ImmutableFloatTreeMap）→ 展示完整的工具模块
4. **阶段 4**: 提取 resource + texture → 达到大多数消费者受益的程度

延期：
- **阶段 5 (codec)**: 需要精细的重构规划 — 可能使用 IDE 批量重命名/移动自动化
- **阶段 6 (stream codec)**: 需要与 attachment 模块协调

---

## 来源

- **MODULES.md** (第 132-148 行): 完整清点 Shared Utility Modules — HIGH 信心
- **util/README.md** + **core/README.md**: 实际提取规则和目标 — HIGH 信心
- **IDE 文件搜索**: 对 root/util/* (32 文件)、core/util/* (5 文件)、attachment/codec/stream/* (5 文件) 进行了物理清点 — HIGH 信心
- **IDE 文本搜索**: 通过消费者文件验证 import-site 模式 — HIGH 信心
- **PROJECT.md** (v1.3 里程碑): 确认范围和约束 — HIGH 信心
