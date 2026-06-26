# 方块实体渲染系统跨版本对比

> 对比 1.20.1 (Forge) / 1.21.1 (NeoForge) / 26.1.2 (NeoForge) 三个版本。

## 1. 架构层级对比

### 1.1 核心渲染模型

| 维度 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| 渲染模式 | 即时渲染（immediate） | 两阶段：extract → submit |
| 数据访问 | 渲染时直接访问 BE 字段 | extract 阶段访问 BE，submit 仅用 RenderState |
| 渲染目标 | MultiBufferSource (VertexConsumer) | SubmitNodeCollector (延迟节点) |
| 线程模型 | 渲染过程与 BE 数据耦合 | extract 与 submit 可分离执行 |

### 1.2 类层次结构

```
1.20.1 / 1.21.1:

BlockEntityRenderer<T>
  ├── SignRenderer<T>
  ├── ChestRenderer<T>
  ├── BeaconRenderer<T>
  ├── ...
  └── 无 RenderState 体系

26.1.2:

BlockEntityRenderer<T, S>
  ├── AbstractSignRenderer<S>            ← 新增抽象基类
  │   └── StandingSignRenderer
  ├── HangingSignRenderer
  ├── ChestRenderer<T, ChestRenderState>
  ├── BeaconRenderer<T, BeaconRenderState>
  ├── AbstractEndPortalRenderer<T, S>    ← 新增抽象基类
  │   ├── TheEndPortalRenderer
  │   └── TheEndGatewayRenderer
  ├── BlockEntityWithBoundingBoxRenderer  ← 新通用渲染器
  └── ...

BlockEntityRenderState (27 个子类)
  ├── SignRenderState
  │   └── StandingSignRenderState
  ├── ChestRenderState
  ├── BeaconRenderState
  └── ...
```

## 2. 接口差异矩阵

| 方法/属性 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 渲染方法 | `render(BE, pt, pose, buf, light, overlay)` | 同 | `createRenderState()` + `extractRenderState()` + `submit()` |
| 泛型参数 | `<T>` | `<T>` | `<T extends BE, S extends BERenderState>` |
| 平台扩展接口 | 无 | `extends IBlockEntityRendererExtension<T>` | 同 |
| shouldRenderOffScreen | `(T be)` | `(T be)` | `()` 无参 |
| getViewDistance | 返回 `64` | 同 | 同（Beacon 用 `getEffectiveRenderDistance * 16`）|
| shouldRender | `(T, Vec3)` | 同 | 同 |
| getRenderBoundingBox | 无（BE 自带） | 同 | 可选 override（用于 frustum culling）|
| Neo: getCustomSprite | 无 | 无 | ChestRenderer 提供 |

## 3. BlockEntityRenderDispatcher 对比

### 3.1 核心方法

| 操作 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| 准备 | `prepare(Level, Camera, HitResult)` | `prepare(Vec3 cameraPos)` |
| 渲染 | `render(BE, pt, poseStack, bufferSource)` | N/A (拆分为 extract + submit) |
| 提取 | N/A | `tryExtractRenderState(BE, pt, breakProgress, frustum)` → RenderState |
| 提交 | N/A | `submit(state, poseStack, collector, camera)` |
| 光照注入 | `setupAndRender()` → `LevelRenderer.getLightColor()` | `extractBase()` → `LevelRenderer.getLightCoords()` → state |
| 物品渲染 | `renderItem(BE, pose, buf, light, overlay)` | 未确认（可能已用 extract+submit 替代） |
| Frustum culling | dispatcher 层无 frustum（LevelRenderer 做） | `tryExtractRenderState` 内直接做 frustum cull |

### 3.2 字段持有

| 字段 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| renderers | `Map<BE, BER<?>>` | `Map<BE, BER<?, ?>>` |
| level | `public Level` | 移除（不再持有）|
| camera | `public Camera` | 改为 `private Vec3 cameraPos` |
| cameraHitResult | `public HitResult` | 移除 |
| entityModelSet | `EntityModelSet` | `Supplier<EntityModelSet>` (Lazy) |
| blockRenderDispatcher | `Supplier<BlockRenderDispatcher>` | `BlockModelResolver` (直接) |
| itemRenderer | `Supplier<ItemRenderer>` | `ItemModelResolver` (直接) |
| entityRenderer | `Supplier<EntityRenderDispatcher>` | `EntityRenderDispatcher` (直接) |
| (新增) sprites | 无 | `SpriteGetter` |
| (新增) playerSkinRenderCache | 无 | `PlayerSkinRenderCache` |

## 4. BlockEntityRendererProvider.Context 对比

| 依赖 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| BlockEntityRenderDispatcher | ✓ | ✓ |
| BlockRenderDispatcher | ✓ | 改为 `BlockModelResolver` |
| ItemRenderer | ✓ | 改为 `ItemModelResolver` |
| EntityRenderDispatcher | ✓ | ✓ |
| EntityModelSet | ✓ | ✓ |
| Font | ✓ | ✓ |
| SpriteGetter | 无 | 新增 |
| PlayerSkinRenderCache | 无 | 新增 |
| 类型 | `class` | `record` |
| Provider 泛型 | `<T>` | `<T, S>` |

## 5. LevelRenderer 调度对比

### 5.1 渲染阶段划分

```
1.20.1 / 1.21.1:
renderLevel()
  ├─ blockEntityRenderDispatcher.prepare(level, camera, hitResult)
  ├─ [light, culling, clear, sky, fog, terrain]
  ├─ [entities]
  ├─ [blockentities] ← 一个阶段完成所有 BE 渲染
  │    ├─ for each section: render(BE, pt, pose, buffer) [即时渲染]
  │    └─ for each global BE: render(BE, pt, pose, buffer)
  └─ [endBatch, destroyProgress, particles, weather]

26.1.2:
extractLevel()                          ← 阶段 1: 提取
  ├─ blockEntityRenderDispatcher.prepare(cameraPos)
  ├─ [light, sky, border, entities, particles, ...]
  ├─ extractVisibleBlockEntities(...)   ← 遍历 BE，收集 RenderState
  └─ levelRenderState.blockEntityRenderStates 填充

addMainPass()                           ← 阶段 2: 提交+渲染
  ├─ submitEntities(...)
  ├─ submitBlockEntities(...)           ← 遍历 RenderState list，提交节点
  ├─ submitParticles(...)
  └─ featureRenderDispatcher.renderSolidFeatures() → 批量渲染
```

## 6. 光照系统对比

| 方面 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| 光照获取 | `LevelRenderer.getLightColor(level, pos)` | `LevelRenderer.getLightCoords(level, pos)` |
| packed 格式工具 | `LightTexture.pack/block/sky` | `LightCoordsUtil.max/block/sky/withBlock` |
| 光照注入点 | `setupAndRender()` 在 dispatch 层 | `extractBase()` 在 extract 层 → 存入 state |
| 自发光检查 | 无 | `state.emissiveRendering()` 返回 FULL_BRIGHT |
| 方块自发光 | 无 | `state.getLightEmission()` 与 block light 比较 |
| overlay | `OverlayTexture.NO_OVERLAY` (dispatcher 注入) | 渲染器内部常量或 state 内字段 |

## 7. 渲染器数量对比

| 版本 | 渲染器数量 | 新增/变化 |
|---|---|---|
| 1.20.1 | 22 | 基线 |
| 1.21.1 | 24 | +TrialSpawnerRenderer, +VaultRenderer |
| 26.1.2 | 27 | +CopperGolemStatueBlockRenderer, +ShelfRenderer, +BlockEntityWithBoundingBoxRenderer(通用化), +TestInstanceRenderer; SignRenderer 拆分为 StandingSign+HangingSign+AbstractSign; EndPortal 拆分为 AbstractEndPortal+TheEndPortal+TheEndGateway; StructureBlockRenderer 移除(被 BoundingBox 替代) |

## 8. RenderState 体系（仅 26.1.2）

### 8.1 继承深度

```
BlockEntityRenderState (基类)
  ├── 直接继承: 13 个 (Banner, Bed, Bell, Campfire, Conduit, DecoratedPot,
  │                     Lectern, PistonHead, ShulkerBox, SkullBlock, etc.)
  ├── SignRenderState
  │   └── StandingSignRenderState
  │   └── HangingSignRenderState
  ├── EndPortalRenderState
  │   └── EndGatewayRenderState
  └── BlockEntityWithBoundingBoxRenderState
```

### 8.2 基类字段

```java
public BlockPos blockPos;
private BlockState blockState;
public BlockEntityType<?> blockEntityType;
public int lightCoords;
@Nullable CrumblingOverlay breakProgress;
```

## 9. NeoForge API 扩展点对比

| 扩展点 | 1.20.1 (Forge) | 1.21.1 (NeoForge) | 26.1.2 (NeoForge) |
|---|---|---|---|
| BER 扩展接口 | 无 | `IBlockEntityRendererExtension<T>` | 同 |
| Frustum culling hook | 直接 `frustum.isVisible()` | `ClientHooks.isBlockEntityRendererVisible()` | dispatcher 内部做 `renderer.getRenderBoundingBox()` |
| Chest 自定义纹理 | 无 | 无 | `getCustomSprite()` Bean 方法 |
| BE RenderState 事件 | 无 | 无 | `ExtractLevelRenderStateEvent` |
| 自定义几何体事件 | 无 | `RenderLevelStageEvent` (不同阶段) | `SubmitCustomGeometryEvent` |

## 10. 破坏动画处理对比

| 方面 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| 处理位置 | LevelRenderer 渲染循环内 | extractVisibleBlockEntities 内 |
| 表现形式 | SheetedDecalTextureGenerator 包装 MultiBufferSource | CrumblingOverlay(PoseStack.Pose) 存入 state.breakProgress |
| submit 层 | 不涉及 | submitModel/submitCustomGeometry 接收 breakProgress 参数 |

## 11. 全局 BE (globalBlockEntities) 对比

| 方面 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| 存储 | `Set<BlockEntity> globalBlockEntities` (LevelRenderer字段) | `level.getGloballyRenderedBlockEntities()` (Level 管理) |
| 更新 | `updateGlobalBlockEntities(remove, add)` | BE 自身加入 Level 的全局集合 |
| 遍历 | `synchronized(globalBlockEntities)` | Iterator + `isRemoved()` 自清理 |
| 触发条件 | `shouldRenderOffScreen(T)` 返回 true | 同 (但签名变为无参) |
| 破坏动画 | 不支持 | 不支持 (breakProgress 传 null) |

## 12. 未确认项

1. **26.1.2 renderItem**：旧版 `BlockEntityRenderDispatcher.renderItem()` 方法在 26.1.2 中是否仍然存在或以 extract+submit 替代 — 未确认。26.1.2 的 `BlockEntityRenderDispatcher` 中未找到 `renderItem` 方法，可能已迁移到 `ItemStackRenderState` 体系。

2. **26.1.2 BlockEntityWithoutLevelRenderer**：与旧版的对应关系 — 未确认，未包含在本次调查范围内。

3. **26.1.2 中的 `IterateVisibleBlockEntities`**：在 1.21.1 的 LevelRenderer 中出现但 26.1.2 中似乎已被提取阶段替代 — 未确认。

4. **SubmitNodeCollector 内部批处理机制**：具体如何将 submitModel / submitCustomGeometry 等调用转换为 GPU draw calls — 未确认（涉及 ModelFeatureRenderer 和 SubmitNodeStorage 的内部实现）。

5. **26.1.2 的 Vulkan 后端兼容性**：两阶段设计是否与 Vulkan 渲染后端直接相关 — 未确认（推测相关但未验证）。

## 13. 版本演进总结

```
1.20.1                    1.21.1                     26.1.2
──────                    ──────                     ──────
即时渲染                  即时渲染 (端口)             两阶段 Extract/Submit
Forge API               NeoForge API               NeoForge API (全新)

核心不变                  + Neo hooks               + RenderState 体系
LightTexture             LightTexture               + LightCoordsUtil
MultiBufferSource        MultiBufferSource          + SubmitNodeCollector
PoseStack                PoseStack                  + CameraRenderState
ModelPart                ModelPart                  + Model.Simple
BlockRenderDispatcher    BlockRenderDispatcher      + BlockModelResolver
ItemRenderer             ItemRenderer               + ItemModelResolver
22 渲染器                 24 渲染器                   27 渲染器 + 27 RenderState
```

**核心演化方向**：渲染数据与方块实体数据分离（RenderState），渲染目标统一为节点提交器（SubmitNodeCollector），整套系统向延迟渲染 + 批量处理架构演进。
