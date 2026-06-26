# Minecraft 渲染主循环跨版本对比

> 基于 `main-loop-1.20.1.md`、`main-loop-1.21.1.md`、`main-loop-26.1.2.md` 的分析,提取跨版本关键差异。
> 所有信息均可追溯到对应版本的源码,不确定处标注"未确认"。

## 目录

1. [架构演进概览](#1-架构演进概览)
2. [帧生命周期](#2-帧生命周期)
3. [渲染阶段顺序对比](#3-渲染阶段顺序对比)
4. [Framebuffer 管理演进](#4-framebuffer-管理演进)
5. [雾系统演进](#5-雾系统演进)
6. [GPU 抽象层演进](#6-gpu-抽象层演进)
7. [区块/区段渲染调度演进](#7-区块区段渲染调度演进)
8. [实体/块实体渲染演进](#8-实体块实体渲染演进)
9. [后处理演进](#9-后处理演进)
10. [Forge/NeoForge 事件体系](#10-forgeneoforge-事件体系)
11. [粒子渲染路径](#11-粒子渲染路径)
12. [API 签名变化速查](#12-api-签名变化速查)

---

## 1. 架构演进概览

```
1.20.1:  命令式渲染,一步到位
  GameRenderer.render → renderLevel → LevelRenderer.renderLevel
  所有逻辑在一个调用链中直接执行,RenderSystem 全局状态驱动

1.21.1:  命令式渲染,增量改进
  DeltaTracker 统一时间, Section 替代 Chunk, 渲染逻辑不变
  独立 frustumMatrix 解耦 view matrix

26.1.2:  声明式渲染,提取-渲染分离
  GameRenderer: update → extract → render 三阶段
  FrameGraph 依赖声明替代命令式资源绑定
  RenderState 快照替代实时状态查询
  GPU buffer (UBO) 替代 OpenGL uniform
  FeatureRenderDispatcher 替代直接渲染
```

---

## 2. 帧生命周期

| 阶段 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| GameTick | 游戏逻辑 tick(Level.update 等) | 同 | GameRenderer.update() + LevelRenderer.update() |
| 数据提取 | 无独立阶段,在 render 中现场查询 | 无独立阶段 | GameRenderer.extract() + LevelRenderer.extractLevel() |
| 渲染 | GameRenderer.render(partialTicks, nanoTime, renderLevel) | GameRenderer.render(DeltaTracker, renderLevel) | GameRenderer.render(DeltaTracker, advanceGameTime) |
| 类型 | 混合了提取和渲染 | 混合了提取和渲染 | 纯粹提交 GPU 指令 |

### 2.1 时间表示

| 版本 | 参数 | 说明 |
|---|---|---|
| 1.20.1 | `float partialTicks` + `long nanoTime` | 分别传递,渲染用 `partialTicks` |
| 1.21.1 | `DeltaTracker deltaTracker` | 统一 `getGameTimeDeltaPartialTick(realtime)` |
| 26.1.2 | `DeltaTracker deltaTracker` | 同上,但 extract 和 render 可能用不同的 partialTick |

---

## 3. 渲染阶段顺序对比

### 3.1 1.20.1 / 1.21.1 阶段顺序(命令式)

```
1. 光照更新(light_update_queue → light_updates)
2. 视锥剔除(culling → captureFrustum)
3. 清屏(clear + FogColor)
4. 天空(sky, FOG_SKY)           → Forge AFTER_SKY
5. 地形雾(fog, FOG_TERRAIN)
6. 地形设置(terrain_setup)
7. 区块编译(compileChunks / compileSections)
8. SOLID → CUTOUT_MIPPED → CUTOUT
9. 光照方向(Lighting)
10. 实体渲染(entities)          → Forge AFTER_ENTITIES
11. 块实体渲染(blockentities)   → Forge AFTER_BLOCK_ENTITIES
12. 方块破坏动画(destroyProgress)
13. 方块高亮(outline)
14. 半透明(TRANSLUCENT) → tripwire
15. 粒子(particles)             → Forge AFTER_PARTICLES
16. 云(clouds)
17. 天气(weather)               → Forge AFTER_WEATHER
18. 后处理合成(transparencyChain.process)
19. 调试渲染(debug)
20. 关闭雾(FogRenderer.setupNoFog)
```

### 3.2 26.1.2 阶段顺序(声明式 FrameGraph)

```
Pass 1: clear(main RT 清屏,用雾颜色)
Pass 2: sky(天空,条件)                     → 无独立 Forge event,融合到 main pass
Pass 3: main {
    ├─ OPAQUE terrain(SOLID + CUTOUT)      → NeoForge AfterOpaqueBlocks
    ├─ 不透明实体 + 块实体(renderSolidFeatures) → AfterOpaqueFeatures
    ├─ TRANSLUCENT terrain                 → NeoForge AfterTranslucentBlocks
    ├─ 半透明实体 + 块实体(renderTranslucentFeatures) → AfterTranslucentFeatures
    └─ 半透明粒子(renderTranslucentParticles)  → AfterTranslucentParticles
}
Pass 4: entity outline(发光实体,条件)
Pass 5: clouds(云,条件)
Pass 6: weather(天气)
Pass 7: transparency chain(后处理合成)
Pass 8: late debug(调试)
```

### 3.3 阶段分组变化

| 1.20.1/1.21.1 | 26.1.2 | 说明 |
|---|---|---|
| SOLID/CUTOUT_MIPPED/CUTOUT 分开三次调用 | OPAQUE 组一次 `renderGroup` | 合并不透明层 |
| 实体 → BE → translucent → particles → clouds → weather | main pass(实体+BE+半透明) → clouds → weather | 云/天气提到 main pass 之外 |
| weather 中含 renderWorldBorder | world border 已移入 main pass | 分离职责 |

---

## 4. Framebuffer 管理演进

### 4.1 三版对比

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 类型 | `RenderTarget` 直接引用 | `RenderTarget` 直接引用 | `ResourceHandle<RenderTarget>` |
| 管理 | 手动 `clear` + `copyDepthFrom` + `bindWrite` | 同 1.20.1 | FrameGraph 声明 `readsAndWrites` |
| 列表 | 6 个字段(entity/translucent/itemEntity/particles/weather/clouds) | 6 个字段(同) | `LevelTargetBundle` + FrameGraph internal |
| entityTarget | `RenderTarget entityTarget` | `RenderTarget entityTarget` | `entityOutlineTarget`(LevelRenderer 字段) + LevelTargetBundle.entityOutline |
| 创建 | 由 transparencyChain 的 PostChain 创建 | 同 | `frame.createInternal(name, descriptor)` |
| 销毁 | `destroyBuffers()` → `= null` | 同 | FrameGraph.execute 后自动释放内部 RT |
| copyDepthFrom | 显式调用 `itemEntityTarget.copyDepthFrom(mainTarget)` | 同 | FrameGraph 内部管理 depth attachment sharing |
| entity outline | `entityEffect.process(partialTick)` | 同 + `outlineEffectRequested` | PostChain `addToFrame` 声明式 |

### 4.2 LevelTargetBundle 结构(26.1.2)

```java
ResourceHandle<RenderTarget> main;          // 始终有效,外部 import
ResourceHandle<RenderTarget> translucent;   // nullable
ResourceHandle<RenderTarget> itemEntity;    // nullable
ResourceHandle<RenderTarget> particles;     // nullable
ResourceHandle<RenderTarget> weather;       // nullable
ResourceHandle<RenderTarget> clouds;        // nullable
ResourceHandle<RenderTarget> entityOutline; // nullable,外部 import
```

---

## 5. 雾系统演进

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| FogMode | `FOG_SKY`, `FOG_TERRAIN` | 同 | `NONE`, `WORLD`(简化) |
| 存储 | OpenGL uniform(每 draw call) | 同 | GPU UBO buffer(`MappableRingBuffer`) |
| 时机 | render 阶段现场计算 | render 阶段现场计算 | extract 阶段计算 → render 阶段上传 |
| 上传 | `FogRenderer.setupFog()` → 设置 shader uniform | 同 | `fogRenderer.updateBuffer(fogData)` → UBO |
| 绑定 | `RenderSystem.setShaderFog(...)` | 同 | `RenderSystem.setShaderFog(gpuBufferSlice)` |
| 帧管理 | 无 | 无 | `endFrame()` 轮转环形 buffer |

### 5.1 FogData 结构变化

**1.20.1/1.21.1**:
```java
class FogData {
    FogMode mode;
    float start, end;         // 雾起始/结束距离
    // 通过 shader uniform FOG_START, FOG_END, FOG_COLOR, FOG_SHAPE 绑定
}
```

**26.1.2** (推断,未逐行确认):
```java
// UBO 布局: fogColor(vec4) + fogStart + fogEnd + skyStart + skyEnd + endClouds
// 通过 RenderSystem.setShaderFog(bufferSlice) 绑定
```

---

## 6. GPU 抽象层演进

| 操作 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| 清屏 | `RenderSystem.clear(16640, ON_OSX)` | `CommandEncoder.clearColorAndDepthTextures(colorTex, color, depthTex, depth)` |
| 深度清 | `RenderSystem.clear(256, ON_OSX)` | `CommandEncoder.clearDepthTexture(depthTex, 1.0)` |
| Viewport | `RenderSystem.viewport(0, 0, w, h)` | 未出现在 render 中(FrameGraph 管理) |
| Shader 绑定 | `RenderSystem.setShader(...)` | `RenderPass.setPipeline(renderPipeline)` |
| 纹理绑定 | `RenderSystem.setShaderTexture(unit, loc)` | `RenderPass.setColorTexture(name, textureView)` |
| Sampler | 隐式(GL 纹理单元) | `RenderSystem.getDevice().createSampler(...)` → `RenderPass.setSampler(name, sampler)` |
| 投影矩阵 | `RenderSystem.setProjectionMatrix(matrix4f, ORTHOGRAPHIC_Z)` | `RenderSystem.setProjectionMatrix(bufferSlice, PERSPECTIVE)` → 通过 buffer 上传 |

### 6.1 RenderPass 类(26.1.2)

替代了旧的 `RenderSystem` + framebuffer 绑定的所有功能:
- 每个 FramePass 内部创建一个 RenderPass,pass 之间自动 barrier
- `setPipeline`, `setVertexBuffer`, `setUniform`, `setSampler`, `setColorTexture`, `drawIndexed` 等
- 必须通过 `AutoCloseable` 模式使用

---

## 7. 区块/区段渲染调度演进

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 单位 | Chunk (16×W×16, W 可变) | Section (16×16×16) | Section (16×16×16) |
| 调度器 | `ChunkRenderDispatcher` | `SectionRenderDispatcher` | `SectionRenderDispatcher` |
| 资源名称 | `renderChunksInFrustum` | `visibleSections` | `chunkSectionsToRender`(ChunkSectionsToRender) |
| 层渲染 | `renderChunkLayer(RenderType)` | `renderSectionLayer(RenderType)` | `chunkSectionsToRender.renderGroup(LayerGroup, sampler)` |
| 层分组 | 无(逐个 RenderType 调用) | 无(逐个 RenderType 调用) | `OPAQUE`(SOLID+CUTOUT) / `TRANSLUCENT` |
| 编译方法 | `compileChunks(camera)` | `compileSections(camera)` | `compileSections(camera)`(LevelRenderer.update 中) |
| 编译触发 | render 阶段 | render 阶段 | update 阶段 |
| 半透明排序 | `resortTransparency`(renderChunkLayer 内) | `resortTransparency`(renderSectionLayer 内) | ChunkSectionsToRender 内部管理 |
| CUTOUT_MIPPED | 独立调用,手动设置 blurMipmap | 独立调用,手动设置 blurMipmap | 合并到 OPAQUE 组(未确认独立存在) |

### 7.1 层对应关系

| 1.20.1/1.21.1 RenderType | 26.1.2 ChunkSectionLayer |
|---|---|
| `RenderType.solid()` | `ChunkSectionLayer.SOLID` |
| `RenderType.cutoutMipped()` | 合并到 CUTOUT?(未确认) |
| `RenderType.cutout()` | `ChunkSectionLayer.CUTOUT` |
| `RenderType.translucent()` | `ChunkSectionLayer.TRANSLUCENT` |
| `RenderType.tripwire()` | 合并到 TRANSLUCENT(未确认独立存在) |

---

## 8. 实体/块实体渲染演进

| 方面 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| 渲染模式 | 直接调用 `EntityRenderer.render()` | 先 `submit()` 到 `SubmitNodeStorage`,再 `renderSolidFeatures/TranslucentFeatures` |
| 批次管理 | `BufferSource.endBatch(RenderType)` 逐个类型 | `featureRenderDispatcher.renderSolidFeatures()` 统一批次 |
| 实体冻结 | 1.20.1 无,1.21.1 有 `TickRateManager` | `DeltaTracker` partialTick 已隐含 |
| 实体提取 | render 阶段实时遍历 `entitiesForRendering()` | extract 阶段 `extractVisibleEntities()` |
| BE 提取 | render 阶段实时遍历 `renderChunksInFrustum` | extract 阶段 `extractVisibleBlockEntities()` |
| 破坏动画 | render 阶段实时渲染 | `submitBlockDestroyAnimation()` + FeatureRenderDispatcher |
| 方块高亮 | render 阶段实时渲染 | `extractBlockOutline()` → FeatureRenderDispatcher |
| 半透明分层 | 通过 BufferSource endBatch 区分 | `renderSolidFeatures` vs `renderTranslucentFeatures` 直接分层 |

### 8.1 FeatureRenderDispatcher 三层

```
renderSolidFeatures()          → 不透明实体 + 不透明 BE
renderTranslucentFeatures()    → 半透明实体 + 半透明 BE(在 TRANSLUCENT terrain 之后)
renderTranslucentParticles()   → 半透明粒子(在 TRANSLUCENT terrain 之后,半透明实体之后)
```

---

## 9. 后处理演进

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 资源 ID | `new ResourceLocation("shaders/post/transparency.json")` | `withDefaultNamespace("shaders/post/transparency.json")` | `Identifier.withDefaultNamespace("transparency")`(常量) |
| 资源 ID(entity outline) | 散落在 `entityEffect` 中 | 同 | `Identifier.withDefaultNamespace("entity_outline")`(常量) |
| 资源 ID(post effect) | 散落在 `postEffect` 中 | 同 | `postEffectId` + `LevelTargetBundle.MAIN_TARGETS` |
| PostChain 执行 | `transparencyChain.process(partialTick)` | `process(deltaTracker.getGameTimeDeltaTicks())` | `addToFrame(frame, width, height, targets)` → FrameGraph 执行 |
| Target 参数 | PostChain 内部管理 main RT 引用 | 同 | `TargetBundle` 接口(LevelTargetBundle 实现) |
| PostChain 获取 | `PostChain.load(...)` | 同 | `shaderManager.getPostChain(id, targetSet)` |

---

## 10. Forge/NeoForge 事件体系

### 10.1 事件名映射

| 1.20.1 (Forge) | 1.21.1 (NeoForge) | 26.1.2 (NeoForge) |
|---|---|---|
| `RenderLevelStageEvent.Stage.AFTER_SKY` | 同名 | **无**(sky 融合到 main pass,不独立触发) |
| `RenderLevelStageEvent.Stage.AFTER_ENTITIES` | 同名 | `AfterOpaqueFeatures` + `AfterTranslucentFeatures` |
| `RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES` | 同名 | 部分融入 `AfterOpaqueFeatures` + `AfterTranslucentFeatures` |
| `RenderLevelStageEvent.Stage.AFTER_PARTICLES` | 同名 | `AfterTranslucentParticles` |
| `RenderLevelStageEvent.Stage.AFTER_WEATHER` | 同名 | **未确认**(weather 为独立 FramePass,可能有独立事件) |
| `RenderLevelStageEvent.Stage.AFTER_LEVEL` | 同名 | `RenderLevelStageEvent.AfterLevel` |
| — | — | `AfterOpaqueBlocks`(新增) |
| — | — | `AfterTranslucentBlocks`(新增) |
| — | — | `SubmitCustomGeometryEvent`(新增) |
| — | — | `ExtractLevelRenderStateEvent`(新增) |
| `ViewportEvent.ComputeCameraAngles` | 同名 | **未确认**(Camera extract 可能触发不同事件) |

### 10.2 事件调用方式

| 版本 | 调用方式 |
|---|---|
| 1.20.1 | `ForgeHooksClient.dispatchRenderStage(stage, ...)` |
| 1.21.1 | `ClientHooks.dispatchRenderStage(stage, ...)` |
| 26.1.2 | `NeoForge.EVENT_BUS.post(new Event(...))` |

---

## 11. 粒子渲染路径

### 11.1 三版对比

| 版本 | 透明链存在时 | 透明链不存在时 |
|---|---|---|
| 1.20.1 | 全部粒子在 translucent terrain 后一起渲染 | 全部粒子在 translucent terrain 后一起渲染 |
| 1.21.1 | 全部粒子在 translucent terrain 后一起渲染 | **拆分**:不透明粒子在 translucent 前,半透明粒子在 translucent 后 |
| 26.1.2 | FeatureRenderDispatcher 分层: `renderSolidFeatures()` → TRANSLUCENT terrain → `renderTranslucentFeatures()` → `renderTranslucentParticles()` | 未确认无透明链路径是否保留 |

### 11.2 1.21.1 粒子拆分详情

```java
// 无 transparencyChain 时的路径 (1.21.1 新增)
profiler.push("solid_particles");
particleEngine.render(lightTexture, camera, f, frustum, type -> !type.isTranslucent());
profiler.push("translucent");
renderSectionLayer(RenderType.translucent(), ...);
profiler.push("particles");
particleEngine.render(lightTexture, camera, f, frustum, type -> type.isTranslucent());
```

修复 MC-161917(不透明粒子在水下穿模)。

---

## 12. API 签名变化速查

### 12.1 GameRenderer.renderLevel

| 版本 | 签名 | 行号 |
|---|---|---|
| 1.20.1 | `renderLevel(float partialTicks, long finishTimeNano, PoseStack poseStack)` | :1086 |
| 1.21.1 | `renderLevel(DeltaTracker deltaTracker)` | :1231 |
| 26.1.2 | `renderLevel(DeltaTracker deltaTracker)` | :688 |

### 12.2 LevelRenderer.renderLevel

| 版本 | 签名 | 行号 |
|---|---|---|
| 1.20.1 | `renderLevel(PoseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera, GameRenderer, LightTexture, Matrix4f projectionMatrix)` | :1128 |
| 1.21.1 | `renderLevel(DeltaTracker, boolean, Camera, GameRenderer, LightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix)` | :921 |
| 26.1.2 | `renderLevel(GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender)` | :465 |

### 12.3 GameRenderer.render

| 版本 | 签名 | 行号 |
|---|---|---|
| 1.20.1 | `render(float partialTicks, long nanoTime, boolean renderLevel)` | :900 |
| 1.21.1 | `render(DeltaTracker deltaTracker, boolean renderLevel)` | :1004 |
| 26.1.2 | `render(DeltaTracker deltaTracker, boolean advanceGameTime)` | :427 |

### 12.4 新增方法(26.1.2)

| 方法 | 类 | 行号 | 说明 |
|---|---|---|---|
| `update(DeltaTracker, boolean)` | `GameRenderer` | :399 | GameTick 阶段,摄像机+区块更新 |
| `extract(DeltaTracker, boolean)` | `GameRenderer` | :411 | 提取 RenderState |
| `extractLevel(DeltaTracker, Camera, float)` | `LevelRenderer` | :573 | 提取世界 RenderState |
| `extractCamera(...)` | `GameRenderer` | :799 | 提取摄像机 RenderState |
| `extractWindow()` | `GameRenderer` | :764 | 提取窗口 RenderState |
| `extractOptions()` | `GameRenderer` | :775 | 提取选项 RenderState |
| `extractGui(...)` | `GameRenderer` | :484 | 提取 GUI RenderState |
| `addMainPass(...)` | `LevelRenderer` | :628 | 声明 main FramePass |
| `addSkyPass(...)` | `LevelRenderer` | :1286 | 声明 sky FramePass |
| `addCloudsPass(...)` | `LevelRenderer` | :778 | 声明 clouds FramePass |
| `addWeatherPass(...)` | `LevelRenderer` | :811 | 声明 weather FramePass |

---

## 总结

### 演进趋势

1. **从命令式到声明式**: `RenderSystem.setShader(...)` → `FramePass` + `RenderPass.setPipeline(...)`
2. **从实时计算到预提取**: RenderState 快照机制分离数据准备和渲染提交
3. **从 CPU 状态到 GPU buffer**: 雾 UBO、投影矩阵 buffer、全局 uniform buffer
4. **从直接渲染到调度器**: `EntityRenderer.render()` → `SubmitNodeStorage` + `FeatureRenderDispatcher`
5. **从管理资源到声明依赖**: 手动 `clear`/`copyDepthFrom`/`bindWrite` → FrameGraph `readsAndWrites`
6. **从大区块到小区段**: Chunk(16×W×16) → Section(16×16×16),更细粒度的编译和剔除
7. **从单 pass 到多层 pass**: 不透明/半透明/粒子三层分离,优化 draw order

### 兼容性影响

- **1.20.1 → 1.21.1**: 区块→区段迁移是最大 breaking change,渲染逻辑保持命令式
- **1.21.1 → 26.1.2**: 完全重写渲染管线,mod 渲染注入需要新 API(SubmitCustomGeometryEvent、ExtractLevelRenderStateEvent 等)
- **事件变化**: Forge AFTER_SKY/AFTER_ENTITIES/AFTER_BLOCK_ENTITIES/AFTER_PARTICLES 事件在 26.1.2 中被细粒度事件替代
