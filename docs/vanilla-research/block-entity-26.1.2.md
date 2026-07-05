# 26.1.2 (NeoForge) 方块实体渲染系统分析

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。所有路径相对于该目录。 源码树由 `scripts/extract-mc-source.py` 重建。

## 目录

1. [架构概览：从即时渲染到两阶段提交](#1-架构概览从即时渲染到两阶段提交)
2. [BlockEntityRenderState 基类](#2-blockentityrenderstate-基类)
3. [BlockEntityRenderer\<T, S\> 接口](#3-blockentityrendererts-接口)
4. [BlockEntityRenderDispatcher 重构](#4-blockentityrenderdispatcher-重构)
5. [BlockEntityRendererProvider.Context — record 化与扩展](#5-blockentityrendererprovidercontext--record-化与扩展)
6. [渲染器注册与新类型](#6-渲染器注册与新类型)
7. [LevelRenderer 两阶段调度](#7-levelrenderer-两阶段调度)
8. [SubmitNodeCollector 提交系统](#8-submitnodecollector-提交系统)
9. [光照变更：getLightColor → getLightCoords](#9-光照变更getlightcolor--getlightcoords)
10. [具体渲染器适配示例](#10-具体渲染器适配示例)
11. [新增/拆分渲染器](#11-新增拆分渲染器)

---

## 1. 架构概览：从即时渲染到两阶段提交

26.1.2 对 BE 渲染进行了**根本架构重构**，与实体渲染的 `EntityRenderState` 重构设计一致：

```
                  1.20.1 / 1.21.1                          26.1.2
                  ─────────────────                        ──────
                  
LevelRenderer    for each BE:                    extractVisibleBlockEntities()
                   └─ render(BE)                      for each BE:
                                                        └─ tryExtractRenderState(BE) → state
                                                            └─ extractRenderState(BE, state)
                                                            → state 加入 list

Dispatcher       getRenderer(BE)                    submitBlockEntities()
                 setupAndRender()                       for each state:
                 └─ renderer.render(BE, ...)             └─ submit(state, ...)

Renderer         render(BE, pt, pose, buf,            createRenderState()  // 新建 state
                  light, overlay)                      extractRenderState(BE, state, ...) // 填充
                  (同步即时渲染)                         submit(state, pose, collector, camera)
                                                       (延迟提交)
```

核心动机：**提取（extract）与提交（submit）分离**，在 extract 阶段完成所有 BE 数据访问（可能在主线程），submit 阶段只处理纯渲染数据，不触碰 BE 实例。这使渲染与游戏逻辑解耦，有利于并行化和 Vulkan 渲染后端。

## 2. BlockEntityRenderState 基类

**文件**: `net/minecraft/client/renderer/blockentity/state/BlockEntityRenderState.java` (36 行)

```java
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    private BlockState blockState = Blocks.AIR.defaultBlockState();
    public BlockEntityType<?> blockEntityType = BlockEntityType.TEST_BLOCK;
    public int lightCoords;                    // ← 替代旧的 packedLight 注入
    public ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress;

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
        state.blockEntityType = blockEntity.getType();
        state.lightCoords = blockEntity.getLevel() != null
            ? LevelRenderer.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos())
            : 15728880;
        state.breakProgress = breakProgress;
    }

    public void fillCrashReportCategory(CrashReportCategory category) { ... }
}
```

- `lightCoords` 替代了 1.21.1 中 `setupAndRender` 计算的 `packedLight`。
- 光照在 extract 阶段计算并存储到 state，submit 阶段直接使用。
- `breakProgress` 存储方块破坏叠加层。
- `extractBase()` 是默认提取逻辑，子类 `extractRenderState` 应先调用 `BlockEntityRenderer.super.extractRenderState(blockEntity, state, ...)` → 自动调用 `extractBase`。

### 2.1 继承体系

所有 render state 都继承自 `BlockEntityRenderState`，例如：

```
BlockEntityRenderState
├── BannerRenderState
├── BeaconRenderState         (+ animationTime, beamRadiusScale, sections[])
├── BedRenderState
├── BellRenderState
├── BlockEntityWithBoundingBoxRenderState
├── BrushableBlockRenderState
├── CampfireRenderState
├── ChestRenderState          (+ type, open, facing, material, customSprite)
├── ConduitRenderState
├── CopperGolemStatueRenderState
├── DecoratedPotRenderState
├── EnchantTableRenderState   (+ flip, open, time, yRot)
├── EndPortalRenderState      (+ facesToShow[])
├── EndGatewayRenderState     (extends EndPortalRenderState)
├── HangingSignRenderState
├── LecternRenderState
├── PistonHeadRenderState
├── ShelfRenderState
├── ShulkerBoxRenderState
├── SignRenderState           (+ woodType, frontText, backText, textLineHeight, ...)
│   └── StandingSignRenderState (+ attachmentType)
├── SkullBlockRenderState
├── SpawnerRenderState
├── TestInstanceRenderState
└── VaultRenderState
```

**共计 27 种 RenderState 类**，存放在独立的 `state/` 子包中。

## 3. BlockEntityRenderer\<T, S\> 接口

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java` (37 行)

```java
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState>
        extends IBlockEntityRendererExtension<T> {
    
    S createRenderState();

    default void extractRenderState(T blockEntity, S state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }

    void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                CameraRenderState camera);

    default boolean shouldRenderOffScreen() { return false; }
    default int getViewDistance() { return 64; }

    default boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
                .closerThan(cameraPosition, this.getViewDistance());
    }
}
```

### 3.1 重大变化

| 变化 | 旧 | 新 |
|---|---|---|
| 泛型参数 | `<T>` | `<T extends BlockEntity, S extends BlockEntityRenderState>` |
| 核心方法 | `render(BE, pt, pose, buffer, light, overlay)` | `createRenderState()` + `extractRenderState()` + `submit()` |
| 渲染目标 | `MultiBufferSource` | `SubmitNodeCollector` |
| 摄像机 | (implicit via dispatcher) | `CameraRenderState camera` (submit 参数) |
| 光照 | `int packedLight` (render 参数) | 合并在 `S.lightCoords` 中（extract 阶段填充） |
| overlay | `int packedOverlay` (render 参数) | 渲染器内部通过 `OverlayTexture.NO_OVERLAY` 常量 |
| shouldRenderOffScreen | `shouldRenderOffScreen(T)` | `shouldRenderOffScreen()` (无参数) |
| getRenderBoundingBox | 无（使用 BE 自带） | 可选的 `getRenderBoundingBox(T)` override（用于 frustum culling） |

### 3.2 `createRenderState()` 模式

每个渲染器必须返回一个全新的 render state 实例：

```java
// 示例 ChestRenderer
public ChestRenderState createRenderState() {
    return new ChestRenderState();
}
```

### 3.3 `extractRenderState()` 模式

```java
// 示例 ChestRenderer
public void extractRenderState(T blockEntity, ChestRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
    // 1. 调用基类提取（填充 blockPos/lightCoords 等）
    BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
    // 2. 从 BE 提取特定数据
    state.type = blockState.getValue(ChestBlock.TYPE);
    state.facing = blockState.getValue(ChestBlock.FACING);
    // 3. 合并 neighbour 数据（亮度、开启动画等）
    state.open = combineResult.apply(opennessCombiner).get(partialTicks);
    // 4. 修正光照（双箱亮度合并）
    if (state.type != ChestType.SINGLE)
        state.lightCoords = combineResult.apply(new BrightnessCombiner<>()).applyAsInt(state.lightCoords);
}
```

### 3.4 `submit()` 模式

```java
// 示例 ChestRenderer
public void submit(ChestRenderState state, PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    poseStack.pushPose();
    poseStack.mulPose(modelTransformation(state.facing));
    float open = 1.0F - state.open;
    open = 1.0F - open * open * open;
    SpriteId spriteId = state.customSprite != null ? state.customSprite : Sheets.chooseSprite(...);
    ChestModel model = this.models.select(state.type);
    submitNodeCollector.submitModel(model, open, poseStack,
        state.lightCoords, OverlayTexture.NO_OVERLAY, -1, spriteId, this.sprites, 0, state.breakProgress);
    poseStack.popPose();
}
```

- `submit()` 不能访问 BE 实例——只能访问 state。
- 使用 `SubmitNodeCollector` 提交渲染节点（代替直接写入 VertexConsumer）。
- 光照通过 `state.lightCoords` 传入。

## 4. BlockEntityRenderDispatcher 重构

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher.java` (130 行)

### 4.1 核心字段变化

```java
// 旧
private Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers;
public Level level;                       // 已移除
public Camera camera;                     // 已移除
public HitResult cameraHitResult;          // 已移除

// 新
private Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers;  // 双泛型
private Vec3 cameraPos;                                                  // 简化
private final BlockModelResolver blockModelResolver;
private final ItemModelResolver itemModelResolver;
private final EntityRenderDispatcher entityRenderer;  // 不再是 Supplier
private final SpriteGetter sprites;
private final PlayerSkinRenderCache playerSkinRenderCache;
```

### 4.2 `tryExtractRenderState()` — 替代 `render()`

```java
@Nullable
public <E extends BlockEntity, S extends BlockEntityRenderState> S tryExtractRenderState(
        E blockEntity, float partialTicks, @Nullable CrumblingOverlay breakProgress,
        @Nullable Frustum frustum) {
    BlockEntityRenderer<E, S> renderer = this.getRenderer(blockEntity);
    if (renderer == null) return null;
    if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) return null;
    if (frustum != null && !frustum.isVisible(renderer.getRenderBoundingBox(blockEntity))) return null;
    if (!renderer.shouldRender(blockEntity, this.cameraPos)) return null;

    Vec3 cameraPosition = this.cameraPos;
    S state = renderer.createRenderState();
    renderer.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
    return state;
}
```

检查链与旧版的 `render()` 一致，但增加了：
- Frustum culling 使用 `renderer.getRenderBoundingBox(blockEntity)` 代替 `blockEntity.getRenderBoundingBox()` — 渲染器可自定义包围盒。
- 成功后返回 state 对象（而非直接渲染）。

### 4.3 `submit()` — 替代 `setupAndRender()`

```java
public <S extends BlockEntityRenderState> void submit(S state, PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    BlockEntityRenderer<?, S> renderer = this.getRenderer(state);
    if (renderer != null) {
        try {
            renderer.submit(state, poseStack, submitNodeCollector, camera);
        } catch (Throwable var9) {
            CrashReport report = CrashReport.forThrowable(var9, "Rendering Block Entity");
            CrashReportCategory category = report.addCategory("Block Entity Details");
            state.fillCrashReportCategory(category);
            throw new ReportedException(report);
        }
    }
}
```

- 通过 `state.blockEntityType` 查找渲染器：`getRenderer(S state)`。
- 异常处理保留 CrashReport 机制。
- 不再需要 `setupAndRender` 注入光照——光照已在 state 中。

### 4.4 `prepare()` 简化

```java
// 旧
public void prepare(Level level, Camera camera, HitResult cameraHitResult) { ... }

// 新
public void prepare(Vec3 cameraPos) {
    this.cameraPos = cameraPos;
}
```

不再持有 Level 和 Camera 引用，只保留相机位置用于 shouldRender 判断。

### 4.5 `onResourceManagerReload()`

```java
public void onResourceManagerReload(ResourceManager resourceManager) {
    BlockEntityRendererProvider.Context context = new BlockEntityRendererProvider.Context(
        this, this.blockModelResolver, this.itemModelResolver, this.entityRenderer,
        this.entityModelSet.get(), this.font, this.sprites, this.playerSkinRenderCache
    );
    this.renderers = BlockEntityRenderers.createEntityRenderers(context);
}
```

Context 构造参数从 6 个扩展到 8 个。

## 5. BlockEntityRendererProvider.Context — record 化与扩展

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRendererProvider.java` (37 行)

```java
@FunctionalInterface
public interface BlockEntityRendererProvider<T extends BlockEntity, S extends BlockEntityRenderState> {
    BlockEntityRenderer<T, S> create(Context context);

    public record Context(
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        BlockModelResolver blockModelResolver,
        ItemModelResolver itemModelResolver,
        EntityRenderDispatcher entityRenderer,
        EntityModelSet entityModelSet,
        Font font,
        SpriteGetter sprites,
        PlayerSkinRenderCache playerSkinRenderCache
    ) {
        public ModelPart bakeLayer(ModelLayerLocation id) {
            return this.entityModelSet.bakeLayer(id);
        }
    }
}
```

**重大变化**：
1. `Provider<T>` → `Provider<T, S>` — 泛型增加 RenderState 类型。
2. `Context` 从 class 变为 `record`（Java 17+）。
3. 新增依赖：
   - `BlockModelResolver` — 替代旧 `BlockRenderDispatcher`（模型解析与渲染分离）。
   - `ItemModelResolver` — 替代旧 `ItemRenderer`（物品模型解析单独抽象）。
   - `SpriteGetter` — 统一 sprite 获取接口。
   - `PlayerSkinRenderCache` — 玩家皮肤缓存。
4. `EntityModelSet` 不再是 `Supplier<>`，而是直接引用。

## 6. 渲染器注册与新类型

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderers.java` (65 行)

```java
private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> PROVIDERS = ...;

public static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
        BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> renderer) {
    PROVIDERS.put(type, renderer);
}
```

### 6.1 注册表变化

| BE 类型 | 旧 | 新 | 说明 |
|---|---|---|---|
| SIGN | SignRenderer::new | StandingSignRenderer::new | 拆分：StandingSign + HangingSign |
| STRUCTURE_BLOCK | StructureBlockRenderer::new | `var0 -> new BlockEntityWithBoundingBoxRenderer()` | 通用化 |
| (新增) TEST_INSTANCE_BLOCK | — | `var0 -> new TestInstanceRenderer()` | 测试实例方块 |
| (新增) COPPER_GOLEM_STATUE | — | CopperGolemStatueBlockRenderer::new | 铜傀儡雕像 |
| (新增) SHELF | — | ShelfRenderer::new | 书架方块 |

一些渲染器不再需要 `Context` 参数，直接用 `var0 -> new XxxRenderer()`。

## 7. LevelRenderer 两阶段调度

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 7.1 新增 `extractLevel()` 方法

整个渲染帧的 extract 阶段集中在一个方法中：

```java
public void extractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick) {
    ProfilerFiller profiler = Profiler.get();
    profiler.push("level");
    Vec3 cameraPos = camera.position();
    Frustum cullFrustum = camera.getCullFrustum();
    
    profiler.push("prepareDispatchers");
    this.blockEntityRenderDispatcher.prepare(cameraPos);        // ← BE 准备
    this.entityRenderDispatcher.prepare(camera, ...);
    
    profiler.popPush("prepareChunkDraws");
    // ... 区块绘制准备
    
    profiler.popPush("entities");
    this.extractVisibleEntities(camera, cullFrustum, deltaTracker, levelRenderState);
    
    profiler.popPush("blockEntities");
    this.extractVisibleBlockEntities(camera, deltaPartialTick, levelRenderState, cullFrustum); // ← BE 提取
    
    // ... blockOutline, blockBreaking, weather, sky, border, particles, cloud, debug
}
```

### 7.2 `extractVisibleBlockEntities()` — 阶段 1: 提取

```java
private void extractVisibleBlockEntities(Camera camera, float deltaPartialTick,
        LevelRenderState levelRenderState, @Nullable Frustum frustum) {
    Vec3 cameraPos = camera.position();
    double camX = cameraPos.x();
    double camY = cameraPos.y();
    double camZ = cameraPos.z();
    PoseStack poseStack = new PoseStack();
    boolean shouldShowEntityOutlines = this.shouldShowEntityOutlines();

    // 遍历可见 section 中的 BE
    for (RenderSection section : this.visibleSections) {
        List<BlockEntity> renderableBlockEntities = section.getSectionMesh().getRenderableBlockEntities();
        if (!renderableBlockEntities.isEmpty() && !(section.getVisibility(getMillis()) < 0.3F)) {
            for (BlockEntity blockEntity : renderableBlockEntities) {
                BlockPos blockPos = blockEntity.getBlockPos();
                // 破坏动画进度提取
                SortedSet<BlockDestructionProgress> progresses = this.destructionProgress.get(blockPos.asLong());
                CrumblingOverlay breakProgress = null;
                if (progresses != null && !progresses.isEmpty()) {
                    poseStack.pushPose();
                    poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
                    breakProgress = new CrumblingOverlay(progresses.last().getProgress(), poseStack.last());
                    poseStack.popPose();
                }
                // 提取 render state
                BlockEntityRenderState state = this.blockEntityRenderDispatcher
                    .tryExtractRenderState(blockEntity, deltaPartialTick, breakProgress, frustum);
                if (state != null) {
                    levelRenderState.blockEntityRenderStates.add(state);
                    // outline 处理
                }
            }
        }
    }

    // 全局 BE（如 Beacon）
    Iterator<BlockEntity> iterator = this.level.getGloballyRenderedBlockEntities().iterator();
    while (iterator.hasNext()) {
        BlockEntity blockEntity = iterator.next();
        if (blockEntity.isRemoved()) { iterator.remove(); continue; }
        BlockEntityRenderState state = this.blockEntityRenderDispatcher
            .tryExtractRenderState(blockEntity, deltaPartialTick, null, frustum);
        if (state != null) {
            levelRenderState.blockEntityRenderStates.add(state);
        }
    }
}
```

关键变化：
- 不再直接在 LevelRenderer 中做相机平移；state 中存储的是世界坐标 `blockPos`，平移在 submit 阶段。
- Frustum culling 使用 `renderer.getRenderBoundingBox()` 代替 `BE.getRenderBoundingBox()`。
- `blockEntity.isRemoved()` 检查替代了旧版 `synchronized(globalBlockEntities)`。

### 7.3 `submitBlockEntities()` — 阶段 2: 提交

```java
private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState,
        SubmitNodeStorage submitNodeStorage) {
    Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
    double camX = cameraPos.x();
    double camY = cameraPos.y();
    double camZ = cameraPos.z();

    for (BlockEntityRenderState renderState : levelRenderState.blockEntityRenderStates) {
        BlockPos blockPos = renderState.blockPos;
        poseStack.pushPose();
        poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
        this.blockEntityRenderDispatcher.submit(renderState, poseStack, submitNodeStorage,
            levelRenderState.cameraRenderState);
        poseStack.popPose();
    }
}
```

- submit 阶段在 extract 之后的渲染帧中执行。
- `LevelRenderState.blockEntityRenderStates` 是 `List<BlockEntityRenderState>`。
- 每帧 `reset()` 清空列表。

### 7.4 渲染帧结构

```
extractLevel() [主线程，准备阶段]
  ├─ prepareDispatchers
  ├─ prepareChunkDraws
  ├─ extractVisibleEntities
  ├─ extractVisibleBlockEntities        ← 创建所有 BE RenderState
  └─ ...

addMainPass() [FrameGraph pass]
  ├─ submitEntities
  ├─ submitBlockEntities                ← 提交所有 BE RenderState
  ├─ submitParticles
  └─ submitBlockDestroyAnimation
```

## 8. SubmitNodeCollector 提交系统

`SubmitNodeCollector` 替代了旧的 `MultiBufferSource`。关键方法：

```java
// 渲染模型
submitNodeCollector.submitModel(model, state, poseStack, lightCoords, overlayCoords, color, spriteId, sprites, seed, breakProgress);

// 渲染自定义几何体
submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> { ... });

// 渲染文字
submitNodeCollector.submitText(poseStack, x, y, text, shadow, displayMode, light, color, bgColor, outlineColor);
```

- 模型渲染通过 `Model.Simple`（替代旧 `Model`）和 `SpriteId` / `SpriteGetter`。
- 自定义几何体仍可使用 `VertexConsumer` 回调。
- 渲染节点被延迟处理（存入 SubmitNodeStorage），由 `ModelFeatureRenderer.renderSolidFeatures()` 等统一执行。

## 9. 光照变更：getLightColor → getLightCoords

### 9.1 API 重命名

```java
// 旧 (1.20.1 / 1.21.1)
LevelRenderer.getLightColor(level, blockPos)  → int packedLight

// 新 (26.1.2)
LevelRenderer.getLightCoords(level, blockPos)  → int lightCoords
```

语义相同，只是重命名以与 `LightCoordsUtil` 保持一致。

### 9.2 LightCoordsUtil

引入了 `net.minecraft.util.LightCoordsUtil` 工具类，替代旧版 `LightTexture` 的部分方法：

```java
// 旧
LightTexture.block(packedLight)
LightTexture.sky(packedLight)
LightTexture.pack(block, sky)

// 新
LightCoordsUtil.block(lightCoords)
LightCoordsUtil.sky(lightCoords)
LightCoordsUtil.max(a, b)       // 直接取两坐标分量的 max
LightCoordsUtil.withBlock(coords, newBlockLight)
```

### 9.3 BrightnessCombiner 适配

```java
// 旧版（手动拆包/打包）
int k = LightTexture.block(i);
int l = LightTexture.block(j);
int i1 = LightTexture.sky(i);
int j1 = LightTexture.sky(j);
return LightTexture.pack(Math.max(k, l), Math.max(i1, j1));

// 新版（使用 LightCoordsUtil）
return LightCoordsUtil.max(
    LevelRenderer.getLightCoords(first.getLevel(), first.getBlockPos()),
    LevelRenderer.getLightCoords(second.getLevel(), second.getBlockPos())
);
```

### 9.4 emissiveRendering 检查

`getLightCoords` 新增了对 `state.emissiveRendering(level, pos)` 的检查——自发光方块直接返回 FULL_BRIGHT。

### 9.5 self-emission 检查

`getLightCoords` 还检查 `state.getLightEmission(level, pos)`，若 block light 低于方块自发光值则提升。

## 10. 具体渲染器适配示例

### 10.1 ChestRenderer

**文件**: `ChestRenderer.java` (140 行)

- 实现了 `createRenderState() → extractRenderState() → submit()`。
- 引入 `getCustomSprite()` 方法允许自定义 chest 纹理（Neo API）。
- 模型拆分：`MultiblockChestResources<ChestModel>` 管理 single/double_left/double_right 三个模型。
- 材质系统重构：使用 `ChestMaterialType` 枚举（ENDER_CHEST, CHRISTMAS, TRAPPED, COPPER_* 等）。
- `modelTransformation()` 使用 `Transformation` record 和 `Matrix4f` 直接变换，替代手动 PoseStack 操作。

### 10.2 BeaconRenderer

**文件**: `BeaconRenderer.java` (225 行)

- 泛型从 `<BeaconBlockEntity>` 变为 `<T extends BlockEntity & BeaconBeamOwner, BeaconRenderState>`。
- `MAX_RENDER_Y` 从 1024 提升到 2048。
- 新增 `beamRadiusScale` 基于距离缩放（超过 96 格时信标光束变宽）。
- 自定义几何体通过 `submitNodeCollector.submitCustomGeometry()` 提交。
- `BeaconRenderState` 存储：`animationTime`, `beamRadiusScale`, `sections[]`（颜色和高度）。
- `getViewDistance()` 使用 `getEffectiveRenderDistance() * 16` 动态计算。
- `getRenderBoundingBox()` 使用 Y 轴延伸到 `MAX_RENDER_Y` 的无限包围盒。

### 10.3 AbstractSignRenderer + StandingSignRenderer

**文件**: `AbstractSignRenderer.java` (162 行), `StandingSignRenderer.java` (171 行)

- 基类 `AbstractSignRenderer<S extends SignRenderState>` 提供文本渲染公共逻辑。
- `StandingSignRenderer` 管理站立/墙面两种变体。
- 变换计算预存到 `SignRenderState.SignTransformations` record（body + frontText + backText 三个 Transformation）。
- `WallAndGroundTransformations<SignTransformations>` 统一管理墙面和地面悬挂的矩阵变换。
- 文本通过 `submitNodeCollector.submitText()` 提交。

### 10.4 EnchantTableRenderer

**文件**: `EnchantTableRenderer.java` (82 行)

- `extractRenderState` 将书旋转的 lerp 计算放在此阶段。
- `submit()` 中构建 `BookModel.State`，通过 `submitNodeCollector.submitModel()` 提交。
- BookModel 使用 `Model.Simple` 类型（26.1.2 的新模型系统）。

### 10.5 TheEndPortalRenderer + AbstractEndPortalRenderer

- 新增 `AbstractEndPortalRenderer<T, S>` 基类和 `EndPortalRenderState`。
- 面渲染通过 `submitCube()` 提交自定义几何体。
- `EndPortalRenderState.facesToShow` 列表在 extract 阶段填充。

### 10.6 SkullBlockRenderer

- 重构为 `WallAndGroundTransformations<Transformation>` 管理朝向。
- RenderState: `SkullBlockRenderState` 包含 skullType, profile, transformation。
- 使用 `PlayerSkinRenderCache` 管理玩家皮肤纹理。

## 11. 新增/拆分渲染器

### 11.1 BlockEntityWithBoundingBoxRenderer

**文件**: `BlockEntityWithBoundingBoxRenderer.java` (185 行)

- 通用化原 `StructureBlockRenderer`。
- 泛型：`<T extends BlockEntity & BoundingBoxRenderable, BlockEntityWithBoundingBoxRenderState>`。
- 渲染结构方块的包围盒、不可见方块（结构空位/屏障/光源）。
- `shouldRenderOffScreen` 返回 `true`。
- `getRenderBoundingBox` 返回 `AABB.INFINITE`。

### 11.2 新方块实体类型

| 渲染器 | BE 类型 | 说明 |
|---|---|---|
| TestInstanceRenderer | TEST_INSTANCE_BLOCK | 测试实例方块 |
| CopperGolemStatueBlockRenderer | COPPER_GOLEM_STATUE | 铜傀儡雕像 |
| ShelfRenderer | SHELF | 书架方块 |
| TrialSpawnerRenderer | TRIAL_SPAWNER | 试炼刷怪笼（1.21 新增）|
| VaultRenderer | VAULT | 宝库（1.21 新增）|

---

## 关键代码路径总结

```
LevelRenderer.extractLevel()                    [帧开始 — 提取阶段]
  ├─ blockEntityRenderDispatcher.prepare(cameraPos)
  └─ extractVisibleBlockEntities()
       ├─ for each visible section:
       │    for each BE:
       │      ├─ 提取 breakProgress
       │      └─ dispatcher.tryExtractRenderState(BE, pt, breakProgress, frustum)
       │           ├─ getRenderer(BE)
       │           ├─ hasLevel && isValid(blockState)?
       │           ├─ frustum cull (renderer.getRenderBoundingBox)?
       │           ├─ shouldRender(BE.pos, cameraPos)?
       │           ├─ state = renderer.createRenderState()
       │           ├─ renderer.extractRenderState(BE, state, pt, cameraPos, breakProgress)
       │           │    └─ BlockEntityRenderState.extractBase(BE, state, breakProgress)
       │           │         ├─ blockPos, blockState, blockEntityType
       │           │         └─ lightCoords = getLightCoords(level, pos) | FULL_BRIGHT
       │           └─ return state
       ├─ for each global BE:
       │    └─ dispatcher.tryExtractRenderState(...) → add to list
       └─ states stored in levelRenderState.blockEntityRenderStates

LevelRenderer.addMainPass() → submitBlockEntities() [渲染阶段 — 提交]
  ├─ for each renderState:
  │    ├─ poseStack.pushPose()
  │    ├─ poseStack.translate(pos - cameraPos)
  │    └─ dispatcher.submit(state, poseStack, submitNodeCollector, camera)
  │         ├─ getRenderer(state.blockEntityType)
  │         └─ renderer.submit(state, poseStack, submitNodeCollector, camera)
  │              └─ submitNodeCollector.submitModel/submitCustomGeometry/submitText(...)
  └─ featureRenderDispatcher.renderSolidFeatures() → 实际渲染
```
