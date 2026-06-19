# ADR-0008: ItemStack Track & Animation Rendering Design

**Status:** Accepted — **Amended by [ADR-0014](0014-flat-merge.md)** (`eyelib-track` 子项目不再存在;track 改为 `io.github.tt432.eyelib.track` 包;per-ItemStack 追踪设计本身仍适用)  
**Context:** ItemStack instances of the same type need per-instance animation state tracking (e.g. two swords each with independent animation progress). Item animation rendering also needs to integrate with the eyelib model rendering pipeline.  
**Decision:** Create `eyelib-track` subproject for tracking ID infrastructure, with root module owning the cache and renderer. Use a `TrackableItem` interface for item declarations, lazy model loading, and cancellable Mixin-based item renderer integration.  
**Consequences:** Per-ItemStack animation state is possible without server-side awareness of animation. Root-owned `ItemTrackRenderer` bridges the tracking layer and the rendering pipeline.

---

# ItemStack 追踪与物品动画渲染设计

## 问题定义

为 ItemStack 提供逐实例的动画状态追踪，使得：同一个物品类型的不同 ItemStack 实例可以拥有独立的动画状态（如两把剑各自动画进度不同），支持物品在背包、手持、地面上展示不同动画。

## 架构概览

```
┌──────────────────────────────────────────────────────────┐
│ eyelib-track (新子项目)                                   │
│ ┌───────────────┐  ┌──────────────────┐  ┌─────────────┐│
│ │ ItemTrackApi  │  │ItemStackIdCache   │  │Mixin(3个)   ││
│ │ getId/assignId│  │SavedData 单调ID   │  │split/copy/   ││
│ │               │  │                   │  │stack/matches ││
│ └───────┬───────┘  └──────────────────┘  └─────────────┘│
└─────────┼────────────────────────────────────────────────┘
          │ 提供: long id = ItemTrackApi.getId(stack)
          │
┌─────────▼────────────────────────────────────────────────┐
│ 根模块 (io.github.tt432.eyelib.client.track)             │
│ ┌───────────────────┐  ┌──────────────────────────────┐  │
│ │ItemTrackRenderCache│  │ItemTrackRenderer             │  │
│ │Long→RenderData<IS>│  │渲染 ItemStack 的 eyelib 模型  │  │
│ │  + MolangScope    │  │                              │  │
│ │  + AnimComponent  │  │                              │  │
│ └──────────────────┘  └──────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

## 追踪 ID 生命周期

基于 Minecraft 1.20.1 ItemStack 生命周期的分析，追踪 ID 的分配和传播规则如下：

### ID 分配时机（服务端调用 `getOrAssignId`）

ItemStack **首次进入需要追踪的上下文**时分配 ID：
- 物品被拾取到玩家背包（`ItemEntity.playerTouch()` → `Inventory.add()`）
- 合成/熔炼产出（`Recipe.assemble()`, `AbstractFurnaceBlockEntity.burn()`）
- 命令创建（`/give` 等）
- 战利品生成（`LootTable.getRandomItems()`）
- 从存档加载后首次使用（延迟分配策略）

### ID 继承（保留原 ID，视为同一逻辑物品）

以下操作产生的是**同一个**逻辑物品的副本，应继承原 ID：
- `ItemStack.copy()` — 完整复制（如背包快照、网络同步快照）
- `ItemStack.save()` → `ItemStack.of()` — 存档/读档循环
- `FriendlyByteBuf.writeItem()` → `readItem()` — 网络传输
- `Slot.set()` — 引用移动到不同容器位置
- `grow()` / `shrink()` — 数量变化，同一实体
- 耐久度变化 — 同一物品磨损

### ID 移除（产生新逻辑物品，需重新分配）

以下操作产生的 ItemStack 是**不同**的逻辑物品，应移除追踪 ID：
- `split()` — 从一个堆拆出一部分 → 拆出部分移除 ID
- 创造模式 CLONE — 凭空复制 → 副本移除 ID
- 拾取方块（Pick Block）— 客户端请求复制方块 → 服务端返回的栈移除 ID

### ID 比较策略

- **宽松比较**（堆叠判定）：`isSameItemSameTags` 忽略 `eyelib_track_id` → 不同 ID 的同种物品仍可堆叠
- **严格比较**（同步检测）：`triggerSlotListeners` / `equipmentHasChanged` 恢复 ID 感知 → 动画状态正确触发更新
- 实现方式：`ItemStackMixin` 拦截 `Objects.equals(tag, tag)` 在比较时剥离追踪 ID

## 缓存架构

### eyelib-track 层：`ItemTrackStateCache<T>`

```java
// 通用缓存容器，Long → T
public class ItemTrackStateCache<T> {
    Long2ObjectMap<T> store;
    LongFunction<T> factory;
    T getOrCreate(long id);
    void remove(long id);
}
```

不持有业务数据，由根模块创建具体绑定。

### 根模块层：`ItemTrackRenderCache`

```java
// 具体绑定：Long → RenderData<ItemStack>
public final class ItemTrackRenderCache {
    static ItemTrackStateCache<RenderData<ItemStack>> CACHE;
    
    public static RenderData<ItemStack> getOrCreateRenderData(ItemStack stack) {
        long id = ItemTrackApi.getId(stack);
        return CACHE.getOrCreate(id);
    }
    
    // factory: 创建 RenderData<ItemStack>，初始化 MolangScope 和 AnimationComponent
}
```

### 内存管理

- 使用 `Long2ObjectOpenHashMap`，无自动过期
- 短期方案：客户端重启时自动清空（内存缓存，不持久化）
- 未来可选：基于 WeakHashMap 的超时清理

## 渲染集成

### 1.20.1 物品渲染管线分析

Minecraft 1.20.1 的物品渲染通过 `ItemRenderer` 集中处理，所有渲染路径最终委托给 `BakedModel`：

```
ItemRenderer.renderGuiItem()       ─┐
ItemInHandRenderer.renderItem()    ─┤
ItemRenderer.renderStatic()        ─┼→ BakedModel.getQuads()
ItemFrameRenderer.render()         ─┘
```

### 集成方案选择

| 方案 | 侵入性 | 支持 Context | 复杂度 |
|------|--------|-------------|--------|
| A. 自定义 BakedModel + ItemOverrides | 低 | 仅 ItemStack | 中，需适配 Model→BakedModel 转换 |
| B. Mixin ItemRenderer | 中 | 完整渲染上下文 | 高，需拦截多路径 |
| C. BEWLR（BlockEntityWithoutLevelRenderer）| 无（已废弃） | Model + ItemStack | 高，需要补全之前注释掉的代码 |

**推荐方案 B+C 混合**：在 1.20.1 中，推荐通过 Forge 的 `CustomItemRenderer` 或修改 `ItemRenderer` 的 Mixin 来接入。

### 渲染上下文感知

渲染需要区分以下 `ItemDisplayContext`：
- `GUI` — 背包/容器界面
- `FIRST_PERSON_RIGHT_HAND` / `FIRST_PERSON_LEFT_HAND` — 第一人称手持
- `THIRD_PERSON_RIGHT_HAND` / `THIRD_PERSON_LEFT_HAND` — 第三人称手持
- `GROUND` — 掉落物
- `FIXED` — 物品展示框
- `HEAD` — 头盔槽
- `NONE` — 未指定

动画状态需要感知这些上下文（如 GeckoLib 的 `ContextBasedAnimatableInstanceCache`）。

## API 对物品开发者

### 物品需实现的接口

```java
// eyelib-track 中定义
public interface TrackableItem {
    // 是否需要 per-ItemStack 动画追踪
    default boolean needsItemTracking() { return false; }
    
    // 提供用于此物品类型的动画名称（由 AnimationManager 管理）
    // 返回 null 表示使用默认动画映射
    @Nullable
    default Map<String, String> getAnimationMapping() { return null; }
}
```

### 使用示例

```java
public class MyAnimatedSwordItem extends SwordItem implements TrackableItem {
    public MyAnimatedSwordItem(...) {
        super(...);
    }
    
    @Override
    public boolean needsItemTracking() {
        return true;
    }
    
    // 动画会在根模块的 ItemTrackRenderer 中通过 AnimationComponent.setup() 配置
}
```

## 文件清单

### eyelib-track（已完成）
- `EyelibTrack.java` — 常量
- `api/ItemTrackApi.java` — 公开 API
- `cache/ItemStackIdCache.java` — 服务端 ID 生成
- `cache/ItemTrackStateCache.java` — 通用缓存容器
- `mixin/common/ItemStackMixin.java` — split + isSameItemSameTags
- `mixin/common/AbstractContainerMenuMixin.java` — clone + triggerSlotListeners
- `mixin/common/LivingEntityMixin.java` — equipmentHasChanged

### 根模块（待实现）
- `client/track/ItemTrackRenderCache.java` — Long→RenderData<ItemStack> 绑定
- `client/track/ItemTrackRenderer.java` — 物品 eyelib 渲染器
- `client/track/ItemTrackRenderUtils.java` — 渲染辅助
- `mixin/ItemRendererMixin.java` — 接入 ItemRenderer 渲染管线
- `mixin/ItemInHandRendererMixin.java` — 手持物品的 Context 注入

### 文档
- `docs/decisions/0008-item-track-design.md` — 本文档

## 边界与约束

1. eyelfib-track 不能依赖根模块 → 通过接口注入实现解耦
2. 动画跳变：split/merge 时动画可能不连续，这是可接受的折中（与 GeckoLib 相同）
3. 客户端 ID 来源：服务端同步的 NBT 标签 → 客户端 `getId()` 直接读取
4. 兜底：无追踪 ID 时回退 `stack.hashCode()` 作为临时 ID
5. 服务端状态：`AnimationComponent` 仅在客户端有意义，服务端仅负责 ID 分配

---

## 物品模型与动画绑定设计

### 现有架构分析

#### 渲染数据模型

`RenderData<T>` 持有完整的 eyelib 渲染所需数据（`src/main/java/io/github/tt432/eyelib/capability/RenderData.java`）：

- `List<ModelComponent> modelComponents` — 每个元素对应一个渲染控制器槽位，通过 `ModelComponentInfo`（`String model`、`ResourceLocation texture`、`ResourceLocation renderType`）绑定模型和纹理
- `AnimationComponent` — 管理动画控制器绑定（`Map<String, String> animations` + `Map<String, MolangValue> animate`），通过 `BrAnimator.tickAnimation()` 产出 `ModelRuntimeData`（逐骨骼位置/旋转/缩放变换）
- `ClientEntityComponent` — 持有 `BrClientEntity`，通过 `ClientEntityRuntimeData.sync()` 将 geometry 条目解析为 `Model` 实例（`ModelLookup.get(geometryName)`）
- `RenderControllerComponent` — 管理渲染控制器槽位，`RenderControllerEntry.setupModel()` 负责创建 `ModelComponent` 并输出纹理（可含多层纹理合成）

#### 实体渲染管线

```
EntityRenderSystem.setupClientEntity(entityId, cap)
  ├─ ClientEntityLookup.get(entityId) → BrClientEntity
  ├─ ClientEntityComponent.setClientEntity() → ClientEntityRuntimeData.sync()
  │    └─ 遍历 geometry 条目，ModelLookup.get() 加载 Model
  ├─ 遍历 render_controllers → RenderControllerEntry.setupModel()
  │    └─ 创建 ModelComponent，设置 ModelComponentInfo(model, texture, renderType)
  └─ AnimationComponent.setup(animations, animate)

EntityRenderSystem (RenderLevelStageEvent)
  ├─ BrAnimator.tickAnimation(animationComponent, scope, effects, ticks, ...) → ModelRuntimeData
  └─ renderComponents()
       ├─ 迭代 modelComponents，过滤 readyForRendering()
       ├─ 构建 RenderParams (poseStack + texture + renderType + vertexConsumer)
       └─ RenderHelper.start().render(params, model, tickedInfos)
            → DFSModel 遍历 + ModelVisitor (applyBoneTranslate 应用骨骼变换 + 绘制面片)
```

关键：`ModelVisitor.applyBoneTranslate()` 将 `ModelRuntimeData` 的 position/rotation/scale 应用到 PoseStack，实现动画驱动的骨骼变形（`src/main/java/io/github/tt432/eyelib/client/render/visitor/ModelVisitor.java:134`）。

#### 当前物品渲染现状

- `ItemRendererMixin` 在 `ItemRenderer.render()` 头部调用 `ItemTrackRenderer.prepareRenderData()` 准备 `RenderData<ItemStack>`，设置 Molang 上下文（如 `context.item_slot`）
- `prepareRenderData()` 仅绑定 Molang 上下文，不绑定模型 — `RenderData.modelComponents` 为空列表
- `AnimationComponent` 已存在但未配置动画映射，`BrAnimator.tickAnimation()` 产出 `ModelRuntimeData.EMPTY`
- **根本问题**：没有 `ModelComponent` → 没有 `Model` → `ModelRuntimeData` 无处应用

### 设计方案

#### 1. 物品如何声明 geo 模型

物品通过 `TrackableItem` 接口直接提供 `ModelComponentInfo`（模型名称、纹理、渲染类型）和 `AnimationComponentInfo`（动画绑定）。不走 `BrClientEntity` + `RenderControllerEntry` 的完整流程。

**理由**：
- 物品通常是单模型单纹理的简单场景，不需要多个渲染控制器槽位和多层纹理合成
- `ModelComponentInfo` 与 `ModelComponent.setInfo()` 的输入格式完全一致，零转换成本
- `ModelComponentInfo` 和 `AnimationComponentInfo` 均定义在 `eyelib-attachment` 子项目中，该子项目已在 `eyelib-track` 的依赖图中

```java
// eyelib-track/.../TrackableItem.java 新增方法

/**
 * 提供 eyelib geo 模型绑定信息。
 * model 格式："my_mod:geo/item/my_sword"
 * 返回 null 表示不使用 eyelib 模型渲染。
 */
@Nullable
default ModelComponentInfo getModelComponentInfo() { return null; }

/**
 * 提供动画控制器绑定。
 * animations: 短名 → 动画资源全名
 * animate: 短名 → Molang 触发条件表达式
 * 返回 null 表示不播放动画。
 */
@Nullable
default AnimationComponentInfo getAnimationComponentInfo() { return null; }
```

#### 2. 模型如何加载并绑定到 RenderData

`ItemTrackRenderCache.getOrCreateRenderData()` 在创建 `RenderData` 的 factory 中检查物品的 `TrackableItem` 信息并设置：

```
ItemTrackRenderCache.getOrCreateRenderData(stack)
  ├─ ItemTrackApi.getId(stack) → id
  ├─ CACHE.getOrCreate(id) → RenderData<ItemStack>
  │   └─ factory: new RenderData<>() + init(stack)
  │       ├─ 已有逻辑：MolangScope + HostContext
  │       └─ 新增逻辑：
  │           ├─ 若 stack.getItem() instanceof TrackableItem ti:
  │           │   ├─ ti.getModelComponentInfo() → modelInfo
  │           │   │   └─ 创建 ModelComponent，调用 setInfo(modelInfo)
  │           │   │       添加至 rd.getModelComponents()
  │           │   └─ ti.getAnimationComponentInfo() → animInfo
  │           │       └─ rd.getAnimationComponent().setInfo(animInfo)
  │           │           → 触发 AnimationComponent.setup() 解析动画绑定
  │           └─ 否则：保持 modelComponents 为空（不渲染 eyelib 模型）
  └─ 返回 RenderData
```

模型的实际加载是**懒加载**的：`ModelComponent.getModel()` 调用 `ModelLookup.get(modelName)`，在首次渲染时才从 `ModelManager` 获取 `Model` 实例。与实体渲染行为一致。

`getOrCreateRenderData()` 中已有 `rd.getOwner() != stack` 检查会重新调用 `init()`，当同一 ID 对应的 ItemStack 替换时（如不同物品替换到同一槽位），模型信息也会随之更新。

**eyelib-track 依赖检查**：`TrackableItem` 引入 `ModelComponentInfo` 和 `AnimationComponentInfo` 类型。这两个类型定义在 `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/capability/`。`eyelib-track` 的 `build.gradle` 已包含 `project(':eyelib-attachment')` 依赖（因为 `AnimationComponent` 位于 `eyelib-animation`，但 `AnimationComponentInfo` 位于 `eyelib-attachment`），需确认该依赖关系是否完整。

#### 3. 渲染器如何应用 ModelRuntimeData 并渲染模型

在 `ItemTrackRenderer` 中新增 `render()` 静态方法，复用实体渲染管线已有的 `renderComponents()` 模式：

```
ItemTrackRenderer.render(stack, displayContext, poseStack, buffer, light, overlay) → boolean
  ├─ prepareRenderData(stack, displayContext) → RenderData<ItemStack> rd
  │   └─ 已设置的 Molang 上下文：context.item_slot、owner(ItemStack)
  │   └─ 建议新增：scope.set("variable.item_use_time", ...) — 使用进度 0→1
  ├─ 若 rd 为 null 或 modelComponents 为空 → 返回 false
  ├─ partialTick = Minecraft.getInstance().getFrameTime()
  ├─ AnimationEffects effects = new AnimationEffects()
  ├─ tickedInfos = BrAnimator.tickAnimation(
  │       rd.getAnimationComponent(), scope, effects,
  │       (ClientTickHandler.getTick() + partialTick) / 20,
  │       () -> {} )
  │   → ModelRuntimeData（逐骨骼变换）
  ├─ 迭代 rd.getModelComponents().filter(ModelComponent::readyForRendering)
  │   ├─ Model model = modelComponent.getModel()  ← ModelLookup.get()
  │   ├─ RenderParams params = RenderParams.builder(poseStack, buffer, modelComponent)
  │   │       .light(light).overlay(overlay).build()
  │   └─ RenderHelper.start().render(params, model, tickedInfos)
  │       → DFSModel.visit() + ModelVisitor 遍历骨骼树
  │       → applyBoneTranslate() 将 ModelRuntimeData.Entry 应用于 PoseStack
  │       → HighSpeedRenderModelVisitor 通过 BakedModel 绘制面片
  └─ 返回 true（至少有一个 model component 被渲染）
```

**调用点**：修改 `ItemRendererMixin.render()` 注入为 `@Inject(method = "render", at = @At("HEAD"), cancellable = true)`，先调用 `ItemTrackRenderer.render()`，若返回 `true`（已由 eyelib 渲染），则取消原版 BakedModel 渲染（`ci.cancel()`）。

**ItemDisplayContext 适配**：
- `GUI` / `GROUND` / `FIXED` / `HEAD` / `NONE`：标准渲染，poseStack 在 ItemRenderer.render() 中已由调用方建立（物品的位置/旋转/缩放等变换由原版 ItemTransforms 决定，Mixin 注入点可直接复用传入的 poseStack）
- `FIRST_PERSON_*` / `THIRD_PERSON_*`：`ItemInHandRenderer.renderItem()` 路径，已有 `renderHandItem()` 模式（`EntityRenderSystem.java:209`）提供 poseStack 变换参考

**Molang 上下文增强**：
- 已有（`prepareRenderData` 中设置）：`context.item_slot` = `displayContext.getSerializedName()`
- 已有（`RenderData.init` 中设置）：`scope.getHostContext().put(ItemStack.class, stack)` — 通过 owner
- 建议新增：`variable.item_use_duration` — 物品使用进度（0-1），参照实体的 `variable.attack_time`

#### 4. 各文件所需变更

| 文件 | 变更内容 | 理由 |
|------|----------|------|
| `TrackableItem.java` | 新增 `getModelComponentInfo()`、`getAnimationComponentInfo()` 默认方法 | 物品声明模型与动画绑定 |
| `ItemTrackRenderCache.java` | factory 中读取 TrackableItem 信息，创建 ModelComponent 并设置 AnimationComponent | 创建 RenderData 时绑定渲染资源 |
| `ItemTrackRenderer.java` | 新增 `render()` 方法：tickAnimation + 迭代 modelComponents + RenderHelper 渲染 | 执行 eyelib 模型渲染 |
| `ItemRendererMixin.java` | 注入改为 cancellable；调用 `render()` 后若成功则 `ci.cancel()` | 用 eyelib 模型替代原版 BakedModel |
| `ItemInHandRendererMixin.java`（新增） | 注入手持物品渲染路径，调用 `ItemTrackRenderer.render()` | 确保手持视角的 Context 正确传递 |
| `eyelib-track/build.gradle` | 确认 `project(':eyelib-attachment')` 依赖存在 | TrackableItem 引用 ModelComponentInfo 等类型 |

#### 设计权衡

| 考虑点 | 决策 | 理由 |
|--------|------|------|
| 直接 ModelComponentInfo vs 通过 BrClientEntity | 直接 ModelComponentInfo | 物品场景简单，单模型单纹理为主；`RenderControllerEntry` 的多槽位和多层纹理合成对物品是过度设计。未来可通过新增 `getClientEntityIdentifier()` 扩展 |
| 模型加载时机 | 懒加载（首次渲染时） | 与实体一致，避免启动时为不可见的物品预加载模型 |
| 与 BakedModel 的交互 | 替换（cancel 默认渲染） | eyelib 模型完全取代原版 BakedModel，避免两个模型重叠导致 z-fighting |
| AnimationComponent 生命周期 | 绑定到 RenderData（客户端内存缓存） | 与实体一致：服务端仅分配 ID，动画状态纯客户端且在 RenderData 的生命周期内有效 |
| 多 ModelComponent 支持 | 预留但不强制 | `modelComponents` 是 List，当前仅填入一个，但接口设计不限制未来多槽位扩展 |
