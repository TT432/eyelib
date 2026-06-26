# 1.20.1 (Forge) 方块实体渲染系统分析

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [BlockEntityRenderer 接口](#1-blockentityrenderer-接口)
2. [BlockEntityRenderDispatcher 调度器](#2-blockentityrenderdispatcher-调度器)
3. [BlockEntityRendererProvider.Context 工厂上下文](#3-blockentityrendererprovidercontext-工厂上下文)
4. [渲染器注册与实例](#4-渲染器注册与实例)
5. [LevelRenderer 中的 BE 调度](#5-levelrenderer-中的-be-调度)
6. [光照交互](#6-光照交互)
7. [具体渲染器示例](#7-具体渲染器示例)
8. [Global Block Entities](#8-global-block-entities)

---

## 1. BlockEntityRenderer 接口

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java` (25 行)

```java
public interface BlockEntityRenderer<T extends BlockEntity> {
    void render(T blockEntity, float partialTick, PoseStack poseStack,
                MultiBufferSource buffer, int packedLight, int packedOverlay);

    default boolean shouldRenderOffScreen(T blockEntity) { return false; }
    default int getViewDistance() { return 64; }

    default boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
                .closerThan(cameraPos, (double)this.getViewDistance());
    }
}
```

- 唯一必须实现的方法：`render(T, partialTick, PoseStack, MultiBufferSource, packedLight, packedOverlay)`。
- `shouldRender(T, Vec3)` 默认以方块实体中心到摄像机位置的欧氏距离与 `getViewDistance()` 比较。
- `shouldRenderOffScreen(T)` 返回 `true` 时实体会被加入全局渲染列表 `globalBlockEntities`（后文详述）。
- 无 `extractRenderState` / `createRenderState` —— 渲染是直接即时模式。

## 2. BlockEntityRenderDispatcher 调度器

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher.java` (126 行)

### 2.1 核心字段

```java
private Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers = ImmutableMap.of();
public final Font font;
private final EntityModelSet entityModelSet;
public Level level;
public Camera camera;
public HitResult cameraHitResult;
private final Supplier<BlockRenderDispatcher> blockRenderDispatcher;
private final Supplier<ItemRenderer> itemRenderer;
private final Supplier<EntityRenderDispatcher> entityRenderer;
```

### 2.2 `render()` 方法 — dispatch 主流程

```java
public <E extends BlockEntity> void render(E blockEntity, float partialTick,
        PoseStack poseStack, MultiBufferSource bufferSource) {
    BlockEntityRenderer<E> renderer = this.getRenderer(blockEntity);
    if (renderer != null) {
        if (blockEntity.hasLevel() && blockEntity.getType().isValid(blockEntity.getBlockState())) {
            if (renderer.shouldRender(blockEntity, this.camera.getPosition())) {
                tryRender(blockEntity, () -> {
                    setupAndRender(renderer, blockEntity, partialTick, poseStack, bufferSource);
                });
            }
        }
    }
}
```

检查链：
1. 查找对应 `BlockEntityType` 的渲染器
2. 实体必须有 level 且 type 对当前 blockState 有效
3. 距离裁剪 `shouldRender()`
4. `tryRender()` 包裹异常处理（CrashReport）

### 2.3 `setupAndRender()` — 光照准备

```java
private static <T extends BlockEntity> void setupAndRender(
        BlockEntityRenderer<T> renderer, T blockEntity, float partialTick,
        PoseStack poseStack, MultiBufferSource bufferSource) {
    Level level = blockEntity.getLevel();
    int i;
    if (level != null) {
        i = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
    } else {
        i = 15728880;  // FULL_BRIGHT
    }
    renderer.render(blockEntity, partialTick, poseStack, bufferSource, i, OverlayTexture.NO_OVERLAY);
}
```

- 光照值来自 `LevelRenderer.getLightColor(level, blockPos)` — 返回 packed int (4-bit block 低 4 位 + 4-bit sky 高 4 位)。
- overlay 始终传 `OverlayTexture.NO_OVERLAY` (值为 `packOverlay(0, 10)`)。
- 若 level 为 null（如物品渲染中）则使用 `FULL_BRIGHT = 15728880`。

### 2.4 `renderItem()` — 物品/UI 渲染

```java
public <E extends BlockEntity> boolean renderItem(E blockEntity, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    BlockEntityRenderer<E> renderer = this.getRenderer(blockEntity);
    if (renderer == null) return true;
    tryRender(blockEntity, () -> renderer.render(
        blockEntity, 0.0F, poseStack, bufferSource, packedLight, packedOverlay));
    return false;
}
```

- 用于物品展示/UI 中的 BE 渲染（如 BlockEntityWithoutLevelRenderer）。
- partialTick 固定为 0.0F，光照和 overlay 由调用方传入。

### 2.5 `onResourceManagerReload()` — 工厂构造

每次资源重载时通过 `BlockEntityRenderers.createEntityRenderers(context)` 重建整个渲染器 map。

## 3. BlockEntityRendererProvider.Context 工厂上下文

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRendererProvider.java` (65 行)

```java
@FunctionalInterface
public interface BlockEntityRendererProvider<T extends BlockEntity> {
    BlockEntityRenderer<T> create(Context context);

    public static class Context {
        private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
        private final BlockRenderDispatcher blockRenderDispatcher;
        private final ItemRenderer itemRenderer;
        private final EntityRenderDispatcher entityRenderer;
        private final EntityModelSet modelSet;
        private final Font font;

        public ModelPart bakeLayer(ModelLayerLocation layerLocation) {
            return this.modelSet.bakeLayer(layerLocation);
        }
        // ... getters
    }
}
```

- `Context` 是一个普通类（非 record），持有 6 个依赖。
- `bakeLayer()` 是到 `EntityModelSet` 的便捷代理。

## 4. 渲染器注册与实例

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderers.java` (56 行)

```java
private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?>> PROVIDERS
        = new ConcurrentHashMap<>();

public static <T extends BlockEntity> void register(
        BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> renderProvider) {
    PROVIDERS.put(type, renderProvider);
}
```

静态初始化块注册了所有原版渲染器（22 个类型，22 个渲染器实例）：

| BlockEntityType | 渲染器 | 备注 |
|---|---|---|
| SIGN | SignRenderer::new | |
| HANGING_SIGN | HangingSignRenderer::new | |
| MOB_SPAWNER | SpawnerRenderer::new | |
| PISTON | PistonHeadRenderer::new | getViewDistance=68 |
| CHEST/ENDER_CHEST/TRAPPED_CHEST | ChestRenderer::new | 共享同一渲染器 |
| ENCHANTING_TABLE | EnchantTableRenderer::new | |
| LECTERN | LecternRenderer::new | |
| END_PORTAL | TheEndPortalRenderer::new | |
| END_GATEWAY | TheEndGatewayRenderer::new | getViewDistance=256, extends TheEndPortalRenderer |
| BEACON | BeaconRenderer::new | shouldRenderOffScreen=true, getViewDistance=256 |
| SKULL | SkullBlockRenderer::new | |
| BANNER | BannerRenderer::new | |
| STRUCTURE_BLOCK | StructureBlockRenderer::new | |
| SHULKER_BOX | ShulkerBoxRenderer::new | |
| BED | BedRenderer::new | |
| CONDUIT | ConduitRenderer::new | |
| BELL | BellRenderer::new | |
| CAMPFIRE | CampfireRenderer::new | |
| BRUSHABLE_BLOCK | BrushableBlockRenderer::new | |
| DECORATED_POT | DecoratedPotRenderer::new | |

## 5. LevelRenderer 中的 BE 调度

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 5.1 渲染阶段顺序

`renderLevel()` 中的 profiler push/pop 顺序（简化）：

```
light_update_queue → light_updates → culling → captureFrustum → clear
→ sky → fog → terrain_setup → compilechunks → terrain (solid → cutoutMipped → cutout)
→ entities → AFTER_ENTITIES (Forge event)
→ blockentities       ← 这里
→ endBatch (solid, endPortal, endGateway, Sheets...)
→ destroyProgress → particles → weather → debug
```

### 5.2 "blockentities" 阶段

```java
profilerfiller.popPush("blockentities");

// 普通方块实体 — 从已编译的 chunk section 列表中获取
for (RenderChunkInfo chunkInfo : this.renderChunksInFrustum) {
    List<BlockEntity> list = chunkInfo.chunk.getCompiledChunk().getRenderableBlockEntities();
    if (!list.isEmpty()) {
        for (BlockEntity blockEntity : list) {
            if (!frustum.isVisible(blockEntity.getRenderBoundingBox())) continue;
            BlockPos pos = blockEntity.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - d0, pos.getY() - d1, pos.getZ() - d2);
            // 破坏进度处理 …
            this.blockEntityRenderDispatcher.render(blockEntity, partialTick, poseStack, multibuffersource);
            poseStack.popPose();
        }
    }
}

// Global block entities — 需要离屏渲染的实体（如信标光束）
synchronized (this.globalBlockEntities) {
    for (BlockEntity blockEntity : this.globalBlockEntities) {
        if (!frustum.isVisible(blockEntity.getRenderBoundingBox())) continue;
        BlockPos pos = blockEntity.getBlockPos();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - d0, pos.getY() - d1, pos.getZ() - d2);
        this.blockEntityRenderDispatcher.render(blockEntity, partialTick, poseStack, multibuffersource);
        poseStack.popPose();
    }
}
```

关键点：
- PoseStack 在 LevelRenderer 层做 camera-relative 平移（`pos - cameraPos`），然后传给渲染器。
- 渲染器内部不需要关心相机位置。
- Frustum culling 基于 `blockEntity.getRenderBoundingBox()`。
- global block entities 通过 `updateGlobalBlockEntities()` 维护（add/remove 两阶段）。

### 5.3 破坏动画处理

BE 渲染前检查 `destructionProgress` map，若该位置有破坏进度则创建 `SheetedDecalTextureGenerator` 并包装 `MultiBufferSource`。使用 `VertexMultiConsumer` 同时写入 crumbling 和正常 buffer。

## 6. 光照交互

### 6.1 light 计算

- `LevelRenderer.getLightColor(level, blockPos)` → packed int。
- `LightTexture.pack(block, sky)` 编码为 `sky << 20 | block << 4`。
- `FULL_BRIGHT = 15728880 = 0xF000F0`（block=15, sky=15）。

### 6.2 overlay

一律使用 `OverlayTexture.NO_OVERLAY`（值为 10，表示无红石充能叠加）。仅在物品渲染中由调用方传入。

### 6.3 双方块实体的亮度合并（BrightnessCombiner）

**文件**: `net/minecraft/client/renderer/blockentity/BrightnessCombiner.java` (36 行)

```java
public Int2IntFunction acceptDouble(S first, S second) {
    return packedLight -> {
        int i = LevelRenderer.getLightColor(first.getLevel(), first.getBlockPos());
        int j = LevelRenderer.getLightColor(second.getLevel(), second.getBlockPos());
        int k = LightTexture.block(i);
        int l = LightTexture.block(j);
        int i1 = LightTexture.sky(i);
        int j1 = LightTexture.sky(j);
        return LightTexture.pack(Math.max(k, l), Math.max(i1, j1));
    };
}
```

- 用于双箱子/双床的亮度计算：取两半的 max block light 和 max sky light。
- 通过 `DoubleBlockCombiner` 模式集成到 ChestRenderer 和 BedRenderer。

### 6.4 BeaconRenderer 的特殊光照

- 信标光束顶点直接写入 `uv2(15728880)` — 恒定全亮度，不受环境光影响。
- `shouldRenderOffScreen` 返回 `true`，因此信标始终在 globalBlockEntities 中。

## 7. 具体渲染器示例

### 7.1 SignRenderer — 即时模式渲染

**文件**: `SignRenderer.java` (218 行)

流程：
1. `render()` 根据 WoodType 查模型→调用 `renderSignWithText()`
2. `renderSignWithText()` 做 `pushPose→translateSign→renderSign→renderSignText×2→popPose`
3. `renderSign()` 通过 `material.buffer(buffer, renderType)` 获取 VertexConsumer
4. `renderSignText()` 内发光文字使用 `FULL_BRIGHT(15728880)`，普通文字使用传入的 `packedLight`

### 7.2 ChestRenderer — DoubleBlockCombiner 模式

**文件**: `ChestRenderer.java` (147 行)

```java
public void render(T blockEntity, float partialTick, PoseStack poseStack,
        MultiBufferSource buffer, int packedLight, int packedOverlay) {
    // ...
    // 读取 ChestType (SINGLE/LEFT/RIGHT)
    // 获取 neighbor combine result
    float open = combineResult.apply(ChestBlock.opennessCombiner(blockEntity)).get(partialTick);
    // 亮度合并
    int i = combineResult.apply(new BrightnessCombiner<>()).applyAsInt(packedLight);
    // 根据 single/double 选择对应的 model part 渲染
}
```

- 利用 `DoubleBlockCombiner` 获取邻居信息。
- 开启动画使用 `opennessCombiner`。
- 亮度使用 `BrightnessCombiner` 合并双箱亮度。

### 7.3 BeaconRenderer — 离屏 + 全亮度

- `shouldRenderOffScreen` 返回 `true` → 加入 globalBlockEntities。
- `getViewDistance` 返回 256。
- `shouldRender` 仅比较水平距离（忽略 Y）。
- 光束顶点 `uv2(15728880)` 恒全亮。

### 7.4 PistonHeadRenderer — 移动方块

- 调用 `BlockRenderDispatcher.getModelRenderer().tesselateBlock()` 渲染被推动的方块。
- `getViewDistance` 返回 68（比默认 64 稍大）。

### 7.5 TheEndPortalRenderer / TheEndGatewayRenderer

- EndPortalRenderer 直接构造 6 面顶点。
- EndGatewayRenderer 继承 EndPortalRenderer，额外绘制光束（复用 `BeaconRenderer.renderBeaconBeam`）。
- `getViewDistance` 在 EndGateway 为 256。

### 7.6 BellRenderer — ModelPart 直接渲染

- 构造阶段 bake `ModelLayers.BELL`，取出 `bell_body` child。
- `render()` 直接操作 model part 的 `xRot/zRot` 实现摇晃动画，然后 `bellBody.render()`。

### 7.7 EnchantTableRenderer — BookModel

- 构造阶段 bake `ModelLayers.BOOK`。
- `render()` 计算书的旋转、翻页、打开动画 → 调用 `bookModel.setupAnim()` → `bookModel.render()`。

### 7.8 BedRenderer — 双部件

- 分别 bake `BED_HEAD` 和 `BED_FOOT`。
- 根据 `BedPart` 选择渲染头部或脚部。
- 使用 `BrightnessCombiner` 合并双床亮度。

### 7.9 ConduitRenderer — 多模型复合

- 分别 bake eye/wind/shell/cage 四个 model part。
- 激活状态渲染 cage + wind (3 个旋转阶段) + eye (朝向相机)。
- 非激活状态只渲染 shell。

## 8. Global Block Entities

`LevelRenderer.globalBlockEntities` 是一个 `Set<BlockEntity>`，通过 `updateGlobalBlockEntities(remove, add)` 维护。当 BE 的 `shouldRenderOffScreen()` 返回 true 时被加入。

使用全局渲染列表的 BE：
- **Beacon**：光束可能延伸到高空（MAX_RENDER_Y=1024），需要离屏可见。
- 其他所有默认渲染器的 `shouldRenderOffScreen` 返回 `false`，因此只通过 chunk section 列表渲染。

### 8.1 两个列表的区别

| 特性 | chunk section 列表 | globalBlockEntities |
|---|---|---|
| 来源 | `chunk.getCompiledChunk().getRenderableBlockEntities()` | `updateGlobalBlockEntities()` 添加/移除 |
| 破坏动画 | 支持（检查 destructionProgress） | 不支持 |
| 时机 | terrain 渲染之后、entities 之后 | 紧跟 chunk section 列表 |
| Frustum culling | 基于 `getRenderBoundingBox()` | 基于 `getRenderBoundingBox()` |

---

## 关键代码路径总结

```
LevelRenderer.renderLevel()
  ├─ prepare(cameraPos, cameraHitResult)
  ├─ [terrain rendering]
  ├─ [entity rendering]
  ├─ for each chunk section:
  │    for each renderable block entity:
  │      ├─ frustum cull
  │      ├─ poseStack.translate(pos - camera)
  │      ├─ destruction progress wrapping
  │      └─ BlockEntityRenderDispatcher.render(BE, pt, poseStack, buffer)
  │           ├─ getRenderer(BE)
  │           ├─ hasLevel && isValid(blockState)?
  │           ├─ shouldRender(pos, cameraPos)?
  │           └─ setupAndRender(renderer, BE, pt, poseStack, buffer)
  │                ├─ LevelRenderer.getLightColor(level, pos)
  │                └─ renderer.render(BE, pt, poseStack, buffer, light, NO_OVERLAY)
  ├─ for each global block entity:
  │    └─ same as above (minus destruction progress)
  └─ endBatch for various RenderTypes
```
