# 26.1.2 (NeoForge) Minecraft 渲染主循环分析报告

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [架构总览: Extract-Then-Render](#1-架构总览-extract-then-render)
2. [GameRenderer — 生命周期方法](#2-gamerenderer--生命周期方法)
3. [GameRenderer.update — GameTick 阶段](#3-gamerendererupdate--gametick-阶段)
4. [GameRenderer.extract — 提取 RenderState](#4-gamerendererextract--提取-renderstate)
5. [GameRenderer.render — 帧提交阶段](#5-gamerendererrender--帧提交阶段)
6. [GameRenderer.renderLevel — 世界层渲染](#6-gamerendererrenderlevel--世界层渲染)
7. [LevelRenderer.extractLevel — 提取世界 RenderState](#7-levelrendererextractlevel--提取世界-renderstate)
8. [LevelRenderer.renderLevel — 世界渲染(FrameGraph)](#8-levelrendererrenderlevel--世界渲染framegraph)
9. [FrameGraph 架构](#9-framegraph-架构)
10. [RenderPass 与 GPU 抽象](#10-renderpass-与-gpu-抽象)
11. [雾系统(GPU Buffer)](#11-雾系统gpu-buffer)
12. [Framebuffer 管理(LevelTargetBundle)](#12-framebuffer-管理leveltargetbundle)
13. [区块渲染:ChunkSectionLayerGroup](#13-区块渲染chunksectionlayergroup)
14. [FeatureRenderDispatcher 实体/块实体渲染](#14-featurerenderdispatcher-实体块实体渲染)

---

## 1. 架构总览: Extract-Then-Render

26.1.2 引入了**提取-渲染分离**模式:

```
每帧流程:
  GameRenderer.update(deltaTracker, advanceGameTime)  ← GameTick(更新逻辑)
  GameRenderer.extract(deltaTracker, advanceGameTime)  ← 提取 RenderState(数据准备)
  GameRenderer.render(deltaTracker, advanceGameTime)   ← 提交 GPU 指令(渲染)
```

**关键概念**:
- **RenderState**: 不可变数据快照,在 `extract` 中填充,在 `render` 中使用
- **FrameGraph**: 声明式渲染通道图,替代命令式的 RenderSystem 状态调用
- **RenderPass**: GPU 渲染通道抽象,替代原始 OpenGL framebuffer 绑定
- **GpuBufferSlice**: GPU 端的 uniform buffer slice(如雾参数)

---

## 2. GameRenderer — 生命周期方法

**文件**: `GameRenderer.java`

### 2.1 核心状态对象

```java
private final GameRenderState gameRenderState;       // 单帧渲染状态聚合
    ├── windowRenderState: WindowRenderState
    ├── optionsRenderState: OptionsRenderState
    ├── guiRenderState: GuiRenderState
    ├── levelRenderState: LevelRenderState
    │   ├── cameraRenderState: CameraRenderState
    │   ├── weatherRenderState: WeatherRenderState
    │   ├── skyRenderState: SkyRenderState
    │   ├── particlesRenderState: QuadParticleRenderState
    │   └── chunkSectionsToRender: ChunkSectionsToRender
    └── lightmapRenderState: LightmapRenderState
```

---

## 3. GameRenderer.update — GameTick 阶段

**文件**: `GameRenderer.java:399`

```java
public void update(DeltaTracker deltaTracker, boolean advanceGameTime)
```

```java
profiler.push("camera");
mainCamera.update(deltaTracker);                       // 更新摄像机位置
profiler.pop();
if (shouldRenderLevel) {
    minecraft.levelRenderer.update(mainCamera);        // GPU 内存管理、Section 上传
}
```

在 `update` 阶段:
- Camera 更新视角插值
- LevelRenderer.update 执行每帧一次的 GPU buffer 上传

---

## 4. GameRenderer.extract — 提取 RenderState

**文件**: `GameRenderer.java:411`

```java
public void extract(DeltaTracker deltaTracker, boolean advanceGameTime)
```

### 4.1 提取流程

```
1. extractWindow()          → WindowRenderState (宽/高/GUI缩放/线宽)
2. extractOptions()         → OptionsRenderState (云/树叶/AO/模糊等)
3. [if renderLevel]
   ├─ lightmapRenderStateExtractor.extract()  → LightmapRenderState
   ├─ extractCamera(deltaTracker, ...)         → CameraRenderState + fogData
   └─ levelRenderer.extractLevel(deltaTracker, camera, partialTick)
       ├─ prepareDispatchers (BE/实体调度器)
       ├─ prepareChunkRenders → chunkSectionsToRender
       ├─ extractVisibleEntities → entityRenderStateList
       ├─ extractVisibleBlockEntities
       ├─ extractBlockOutline + extractBlockDestroyAnimation
       ├─ weatherEffectRenderer.extractRenderState
       ├─ skyRenderer.extractRenderState
       ├─ worldBorderRenderer.extract
       ├─ particleEngine.extract → particlesRenderState
       ├─ cloud color/height
       ├─ debugRenderer.emitGizmos
       └─ NeoForge ExtractLevelRenderStateEvent
4. extractGui(deltaTracker, ...)
```

### 4.2 extractCamera (`:799–811`)

```java
mainCamera.extractRenderState(cameraState, cameraEntityPartialTicks);
cameraState.fogType = mainCamera.getFluidInCamera();
cameraState.fogData = fogRenderer.setupFog(mainCamera, renderDistance, deltaTracker, ...);
```

雾在 extract 阶段计算好(FogData),render 阶段只使用。

---

## 5. GameRenderer.render — 帧提交阶段

**文件**: `GameRenderer.java:427`

```java
public void render(DeltaTracker deltaTracker, boolean advanceGameTime)
```

### 5.1 渲染流程

```java
profiler.push("render");
// 1. 窗口 resize 检测
if (windowRenderState.isResized) resize(width, height);

// 2. 帧开始:清除主 RT 颜色+深度
CommandEncoder.clearColorAndDepthTextures(
    mainRenderTarget.colorTexture, clearColorOverride,
    mainRenderTarget.depthTexture, 1.0
);

// 3. 全局 uniform 更新（GlobalSettingsUniform）
globalSettingsUniform.update(width, height, glintStrength, gameTime, ...);

// 4. [if renderLevel] Lightmap + 世界渲染
if (shouldRenderLevel) {
    lightmap.render(gameRenderState.lightmapRenderState);  // lightmap 纹理上传
    profiler.push("world");
    renderLevel(deltaTracker);                // 世界渲染
    tryTakeScreenshotIfNeeded();
    doEntityOutline();
    // 后处理(post effect)
    if (postEffectId != null && effectActive) {
        PostChain postChain = shaderManager.getPostChain(postEffectId, LevelTargetBundle.MAIN_TARGETS);
        if (postChain != null) postChain.process(mainRenderTarget, resourcePool);
    }
    profiler.pop();
}

// 5. 帧结束
fogRenderer.endFrame();                                    // 雾 buffer 轮转
CommandEncoder.clearDepthTexture(mainRenderTarget.depthTexture, 1.0);  // 清深度
lighting.setupFor(Lighting.Entry.ITEMS_3D);                // GUI 光照

// 6. GUI 渲染
profiler.push("gui");
guiRenderer.render(fogRenderer.getBuffer(FogMode.NONE));
guiRenderer.endFrame();
profiler.pop();

// 7. 资源清理
submitNodeStorage.endFrame();
featureRenderDispatcher.endFrame();
resourcePool.endFrame();
profiler.pop();
```

### 5.2 关键差异(相对于 1.21.1)

- **无 RenderSystem.clear**: 用 `CommandEncoder.clearColorAndDepthTextures` 替代
- **无 RenderSystem.viewport**: viewport 由 FrameGraph pass 内部管理
- **雾 GPU 化**: `fogRenderer.getBuffer(FogMode)` 返回 `GpuBufferSlice`,通过 `RenderSystem.setShaderFog` 绑定
- **Lightmap 独立**: `lightmap.render(state)` 封装纹理上传
- **GlobalSettingsUniform**: 每帧更新全局 shader uniform buffer
- **GUI 独立渲染器**: `guiRenderer` 替代内联的 GUI 代码
- **ResourcePool**: 帧级资源生命周期管理

---

## 6. GameRenderer.renderLevel — 世界层渲染

**文件**: `GameRenderer.java:688`

### 6.1 执行步骤

1. **提取 partialTick**:
   - `worldPartialTicks = deltaTracker.getGameTimeDeltaPartialTick(false)`
   - `cameraEntityPartialTicks = mainCamera.getCameraEntityPartialTicks(deltaTracker)`

2. **投影矩阵** (`:697–718`):
   - 从 CameraRenderState 获取 projectionMatrix
   - `bobHurt`/`bobView` 构建 PoseStack
   - 反胃/传送门效果(`projectionMatrix.rotate/scale`)

3. **投影矩阵上传** (`:720`):
   ```java
   RenderSystem.setProjectionMatrix(levelProjectionMatrixBuffer.getBuffer(projectionMatrix), PERSPECTIVE);
   ```

4. **雾绑定** (`:722–723`):
   ```java
   fogRenderer.updateBuffer(cameraState.fogData);
   GpuBufferSlice terrainFog = fogRenderer.getBuffer(FogRenderer.FogMode.WORLD);
   ```

5. **LevelRenderer.renderLevel** (`:726–738`):
   ```java
   levelRenderer.renderLevel(resourcePool, deltaTracker, renderOutline, cameraState,
       modelViewMatrix, terrainFog, fogColor, !shouldCreateBossFog, chunkSectionsToRender);
   ```

6. **NeoForge AfterLevel** (`:739–741`):
   ```java
   NeoForge.EVENT_BUS.post(new RenderLevelStageEvent.AfterLevel(...));
   ```

7. **手部渲染** (`:742–748`):
   ```java
   hudProjection.setupPerspective(0.05F, 100.0F, hudFov, width, height);
   RenderSystem.setProjectionMatrix(hud3dProjectionMatrixBuffer.getBuffer(hudProjection), PERSPECTIVE);
   CommandEncoder.clearDepthTexture(mainRenderTarget.depthTexture, 1.0);
   renderItemInHand(cameraState, cameraEntityPartialTicks, modelViewMatrix);
   ```

---

## 7. LevelRenderer.extractLevel — 提取世界 RenderState

**文件**: `LevelRenderer.java:573`

```java
public void extractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick)
```

完全替代了 render 阶段中的准备逻辑,移到独立的 extract 阶段:

```
1. prepareDispatchers → BE/实体调度器准备
2. prepareChunkRenders → chunkSectionsToRender (OPAQUE + TRANSLUCENT layer groups)
3. extractVisibleEntities → 可见实体提取
4. extractVisibleBlockEntities → 可见 BE 提取
5. extractBlockOutline → 方块高亮数据
6. extractBlockDestroyAnimation → 方块破坏动画数据
7. weatherEffectRenderer.extractRenderState → 天气渲染状态
8. skyRenderer.extractRenderState → 天空渲染状态
9. worldBorderRenderer.extract → 世界边界渲染状态
10. particleEngine.extract → 粒子渲染状态
11. cloud color/height
12. debugRenderer.emitGizmos / gameTestBlockHighlightRenderer.emitGizmos
13. NeoForge 自定义环境效果 + ExtractLevelRenderStateEvent
```

---

## 8. LevelRenderer.renderLevel — 世界渲染(FrameGraph)

**文件**: `LevelRenderer.java:465`

```java
public void renderLevel(
    GraphicsResourceAllocator resourceAllocator,
    DeltaTracker deltaTracker,
    boolean renderOutline,
    CameraRenderState cameraState,
    Matrix4fc modelViewMatrix,
    GpuBufferSlice terrainFog,
    Vector4f fogColor,
    boolean shouldRenderSky,
    ChunkSectionsToRender chunkSectionsToRender
)
```

### 8.1 FrameGraph 构建

```java
// 1. 导入外部资源
FrameGraphBuilder frame = new FrameGraphBuilder();
targets.main = frame.importExternal("main", mainRenderTarget);

// 2. 创建内部 render target (若有 transparencyChain)
if (transparencyChain != null) {
    targets.translucent = frame.createInternal("translucent", screenSizeTargetDescriptor);
    targets.itemEntity   = frame.createInternal("item_entity", screenSizeTargetDescriptor);
    targets.particles    = frame.createInternal("particles", screenSizeTargetDescriptor);
    targets.weather      = frame.createInternal("weather", screenSizeTargetDescriptor);
    targets.clouds       = frame.createInternal("clouds", screenSizeTargetDescriptor);
}

// 3. NeoForge FrameGraph setup event
ClientHooks.fireFrameGraphSetup(frame, targets, ...);
```

### 8.2 Pass 声明顺序

```java
// Pass 1: clear
FramePass clearPass = frame.addPass("clear");
targets.main = clearPass.readsAndWrites(targets.main);
clearPass.executes(() -> {
    CommandEncoder.clearColorAndDepthTextures(colorTexture, fogColor, depthTexture, 1.0);
});

// Pass 2: sky(条件)
if (shouldRenderSky) addSkyPass(frame, cameraState, terrainFog, modelViewMatrix);

// Pass 3: main(实体+块实体+地形+半透明)
addMainPass(frame, frustum, modelViewMatrix, terrainFog, ...);

// Pass 4: entity outline(条件)
if (haveGlowingEntities && entityOutlineChain != null) {
    entityOutlineChain.addToFrame(frame, width, height, targets);
}

// Pass 5: clouds(条件)
if (cloudStatus != OFF && cloudColor.alpha > 0) {
    addCloudsPass(frame, cloudStatus, cameraPos, gameTime, ...);
}

// Pass 6: weather
addWeatherPass(frame, terrainFog, modelViewMatrix);

// Pass 7: transparency post-processing
if (transparencyChain != null) {
    transparencyChain.addToFrame(frame, width, height, targets);
}

// Pass 8: late debug
addLateDebugPass(frame, cameraState, terrainFog, modelViewMatrix);
```

### 8.3 执行

```java
profiler.popPush("executeFrameGraph");
frame.execute(resourceAllocator, new FrameGraphBuilder.Inspector() {
    public void beforeExecutePass(String name) { profiler.push(name); }
    public void afterExecutePass(String name)  { profiler.pop(); }
});
```

FrameGraph 在 `execute` 中:
1. 解析依赖关系
2. 分配/复用 GPU 资源(texture, buffer)
3. 按依赖顺序(拓扑排序)执行各 pass
4. 管理 barrier/transition(纹理布局转换)

### 8.4 关键差异:云与天气顺序

| 1.20.1/1.21.1 | 26.1.2 |
|---|---|
| translucent → particles → clouds → weather | main(entities+BEs+translucent) → outline → clouds → weather |
| 云在粒子后 | 云在 main pass 之后,天气之前 |
| 天气中含有 renderWorldBorder | renderWorldBorder 已移入 main pass |

---

## 9. FrameGraph 架构

**文件**: `com/mojang/blaze3d/framegraph/FrameGraphBuilder.java` (未详读完整实现)

### 9.1 核心概念

```
FrameGraphBuilder  ← 声明式图构建器
  ├─ addPass(name) → FramePass
  ├─ createInternal(name, descriptor) → ResourceHandle<RenderTarget>
  ├─ importExternal(name, existingTarget) → ResourceHandle<RenderTarget>
  └─ execute(resourceAllocator, inspector) → void

FramePass  ← 单个渲染通道
  ├─ reads(resource)          → 只读依赖
  ├─ writes(resource)         → 只写输出
  ├─ readsAndWrites(resource) → 读写(in-place)
  └─ executes(runnable)       → 通道执行逻辑(Runnable)

ResourceHandle<RenderTarget>  ← 资源句柄
  ├─ get() → RenderTarget (仅在 execute 期间可用)
  └─ 支持依赖跟踪、生命周期管理
```

### 9.2 优势

1. **自动屏障**: FrameGraph 自动在 pass 间插入 GPU barrier
2. **资源复用**: 内部 RT 在不需要时自动回收
3. **依赖排序**: 拓扑排序保证执行顺序正确
4. **声明式**: 先声明所有 pass,再一次性执行,优化空间

---

## 10. RenderPass 与 GPU 抽象

**文件**: `com/mojang/blaze3d/systems/RenderPass.java`

### 10.1 RenderPass 功能

```java
public class RenderPass implements AutoCloseable {
    void pushDebugGroup(Supplier<String> label);
    void popDebugGroup();
    void setPipeline(RenderPipeline pipeline);
    void setVertexBuffer(int slot, GpuBuffer buffer);
    void setIndexBuffer(GpuBuffer buffer, IndexType type);
    void setUniform(String name, GpuBufferSlice buffer);
    void setSampler(String name, GpuSampler sampler);
    void setColorTexture(String name, GpuTextureView texture);
    void setDepthTexture(String name, GpuTextureView texture);
    void draw(int vertexCount, int firstVertex);
    void drawIndexed(int indexCount, int firstIndex, int vertexOffset);
}
```

### 10.2 替代旧的 RenderSystem

| 旧方式(1.21.1-) | 新方式(26.1.2) |
|---|---|
| `RenderSystem.setShaderTexture(...)` | `renderPass.setColorTexture(name, texture)` |
| `RenderSystem.setShader(...)` | `renderPass.setPipeline(pipeline)` |
| `RenderTarget.bindWrite(true)` | frame pass 中的 `readsAndWrites(resource)` |
| `RenderSystem.clear(...)` | `CommandEncoder.clearColorAndDepthTextures(...)` |
| `RenderSystem.enableDepthTest()` 等 | RenderPipeline 中声明 |

---

## 11. 雾系统(GPU Buffer)

**文件**: `net/minecraft/client/renderer/fog/FogRenderer.java`

### 11.1 FogMode 简化

```java
public static enum FogMode {
    NONE,     // 无雾(GUI 等)
    WORLD;    // 世界雾(统一了 FOG_SKY + FOG_TERRAIN)
}
```

1.20.1/1.21.1 有 `FOG_SKY` 和 `FOG_TERRAIN` 两种模式,26.1.2 合并为 `WORLD`,天空雾用距离 0 区分。

### 11.2 GPU Buffer 实现

```java
private final MappableRingBuffer regularBuffer;  // 环形 GPU buffer
private GpuBuffer emptyBuffer;                   // 空雾(无效果)

// 上传雾数据到 GPU buffer
public void updateBuffer(FogData fogData) {
    ByteBuffer buffer = ...;
    // 打包: fogColor(rgba) + fogStart + fogEnd + skyStart + skyEnd + endClouds
    regularBuffer.putBytes(fogData);
}

// 获取当前帧的雾 buffer slice
public GpuBufferSlice getBuffer(FogMode mode) {
    if (!fogEnabled) return emptyBuffer.slice(0, FOG_UBO_SIZE);
    // 返回 regularBuffer 的当前写入位置
}

// 每帧结束轮转
public void endFrame() { regularBuffer.rotate(); }
```

**优势**: 雾参数通过 UBO (Uniform Buffer Object) 上传,shader 直接 bind buffer,无需逐帧设置 uniform。

### 11.3 setupFog 在 extract 阶段

```java
// GameRenderer.extractCamera (line 803-810)
cameraState.fogData = fogRenderer.setupFog(
    mainCamera, renderDistance, deltaTracker, bossOverlayDarkening, level
);
```

雾计算在 extract 阶段完成,render 阶段直接使用 `fogRenderer.updateBuffer(fogData)` 上传。

---

## 12. Framebuffer 管理(LevelTargetBundle)

**文件**: `LevelRenderer.java:154`, `LevelTargetBundle.java`

### 12.1 结构

```java
public class LevelTargetBundle implements PostChain.TargetBundle {
    ResourceHandle<RenderTarget> main;          // 始终有效
    ResourceHandle<RenderTarget> translucent;   // nullable, transparencyChain 时创建
    ResourceHandle<RenderTarget> itemEntity;    // nullable
    ResourceHandle<RenderTarget> particles;     // nullable
    ResourceHandle<RenderTarget> weather;       // nullable
    ResourceHandle<RenderTarget> clouds;        // nullable
    ResourceHandle<RenderTarget> entityOutline; // nullable (外部 target)
}
```

### 12.2 Target ID

```java
public static final Identifier MAIN_TARGET_ID = PostChain.MAIN_TARGET_ID;
public static final Identifier TRANSLUCENT_TARGET_ID = id("translucent");
public static final Identifier ITEM_ENTITY_TARGET_ID = id("item_entity");
public static final Identifier PARTICLES_TARGET_ID = id("particles");
public static final Identifier WEATHER_TARGET_ID = id("weather");
public static final Identifier CLOUDS_TARGET_ID = id("clouds");
public static final Identifier ENTITY_OUTLINE_TARGET_ID = id("entity_outline");
```

### 12.3 与旧版的差异

| 1.20.1/1.21.1 | 26.1.2 |
|---|---|
| `RenderTarget entityTarget` (直接字段) | `ResourceHandle<RenderTarget> entityOutline` (handle) |
| `transparencyChain.process(partialTick)` | `transparencyChain.addToFrame(frame, ...)` (声明式) |
| 手动 `copyDepthFrom` | FrameGraph 内部管理 |
| RT 销毁在 LevelRenderer.close | FrameGraph 自动管理 internal RT |

---

## 13. 区块渲染:ChunkSectionLayerGroup

**文件**: `net/minecraft/client/renderer/chunk/ChunkSectionLayerGroup.java`

### 13.1 分组

```java
public enum ChunkSectionLayerGroup {
    OPAQUE(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT),
    TRANSLUCENT(ChunkSectionLayer.TRANSLUCENT);
}
```

1.20.1/1.21.1 分别调用 SOLID → CUTOUT_MIPPED → CUTOUT → TRANSLUCENT,26.1.2 合并为 OPAQUE 组内一次绘制。

### 13.2 渲染方式

```java
// addMainPass 内部 (:684)
chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE, chunkLayerSampler);
// ... 实体/块实体/半透明特性 ...
chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, chunkLayerSampler);
```

**Sampler**: 使用 `RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, ..., maxAnisotropy, ...)`,支持各向异性过滤。

### 13.3 CUTOUT_MIPPED 处理

在 26.1.2 中,CUTOUT_MIPPED 不再作为独立层渲染,可能已合并到 CUTOUT(未确认: 需要深入 ChunkSectionLayer 枚举和 CompiledSectionMesh 验证)。

---

## 14. FeatureRenderDispatcher 实体/块实体渲染

**文件**: `LevelRenderer.java addMainPass (:698–753)`

26.1.2 使用 `FeatureRenderDispatcher` + `SubmitNodeStorage` 替代旧版的直接 EntityRenderer 调用:

```java
// 提交阶段(collect)
this.submitEntities(poseStack, levelRenderState, submitNodeStorage);
this.submitBlockEntities(poseStack, levelRenderState, submitNodeStorage);
levelRenderState.particlesRenderState.submit(submitNodeStorage, cameraState);
this.submitBlockDestroyAnimation(poseStack, submitNodeStorage, levelRenderState);

// 渲染阶段(dispatch)
this.featureRenderDispatcher.renderSolidFeatures();
bufferSource.endBatch();

// ... translucent terrain ...

this.featureRenderDispatcher.renderTranslucentFeatures();
bufferSource.endBatch();

// 半透明粒子(在 translucent terrain 之后)
this.featureRenderDispatcher.renderTranslucentParticles();
bufferSource.endBatch();

this.featureRenderDispatcher.clearSubmitNodes();
```

### 14.1 三层渲染

| 阶段 | 内容 |
|---|---|
| `renderSolidFeatures()` | 不透明实体 + 不透明块实体 + 破坏动画 |
| `renderTranslucentFeatures()` | 半透明实体 + 半透明块实体 |
| `renderTranslucentParticles()` | 半透明粒子 |

这种分层替代了旧版中通过 `BufferSource.endBatch(renderType)` 的多批次管理。

### 14.2 SubmitNode 模式

实体/块实体不再直接调用 `render()` 方法,而是通过 `submit()` 提交到 `SubmitNodeStorage`,由 `FeatureRenderDispatcher` 统一调度:
- 支持按 material 排序
- 支持 GPU culling / indirect draw (未在源码中确认完整实现)
- 分离了"收集绘制命令"和"执行绘制命令"

---

## 总结

### 26.1.2 核心架构革新

| 维度 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| 帧模型 | 单次 render 调用 | update → extract → render 三阶段 |
| 状态管理 | RenderSystem 全局状态(thread-local) | RenderState 快照 + FrameGraph |
| Framebuffer | 手动 RenderTarget 绑定 | FrameGraph ResourceHandle 依赖声明 |
| 雾 | OpenGL uniform(逐帧设置) | GPU UBO buffer(MappableRingBuffer) |
| 清屏 | RenderSystem.clear(位掩码) | CommandEncoder.clearColorAndDepthTextures |
| 后处理 | PostChain.process(partialTick) | PostChain.addToFrame(frame, ...) |
| 着色器 | RenderSystem.setShader | RenderPass.setPipeline(RenderPipeline) |
| 实体渲染 | 直接 EntityRenderer.render() | Submit → FeatureRenderDispatcher → renderSolidFeatures/TranslucentFeatures |
| GUI | 内联在 GameRenderer.render | 独立 GuiRenderer |
| 半透明排序 | translucent→particles→clouds→weather | main(内含 translucent) → clouds → weather |

### 渲染阶段顺序(26.1.2)

```
extract 阶段(数据准备):
  ├─ Camera + Fog extract
  ├─ LevelRenderState extract(entities, BEs, particles, weather, sky, ...)
  └─ Gui extract

render 阶段(FrameGraph 执行):
  Pass 1: clear (主 RT 清屏,使用雾颜色)
  Pass 2: sky (天空渲染)
  Pass 3: main (OPAQUE terrain → entities → BEs → translucent terrain → translucent particles)
  Pass 4: entity outline (发光实体后处理,条件)
  Pass 5: clouds (云渲染,条件)
  Pass 6: weather (天气效果)
  Pass 7: transparency chain (后处理合成)
  Pass 8: late debug (调试渲染)
  └─ gui renderer (UI 层)
```
