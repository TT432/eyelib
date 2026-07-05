# 1.20.1 (Forge) Minecraft 渲染主循环分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [核心类概述](#1-核心类概述)
2. [GameRenderer.render — 帧级入口](#2-gamerendererrender--帧级入口)
3. [GameRenderer.renderLevel — 世界渲染准备](#3-gamerendererrenderlevel--世界渲染准备)
4. [LevelRenderer.renderLevel — 世界渲染主流程](#4-levelrendererrenderlevel--世界渲染主流程)
5. [阶段详解](#5-阶段详解)
6. [Framebuffer 链](#6-framebuffer-链)
7. [后处理透明度链](#7-后处理透明度链)
8. [摄像机与视锥剔除](#8-摄像机与视锥剔除)
9. [雾系统](#9-雾系统)
10. [区块渲染调度](#10-区块渲染调度)
11. [特殊路径:无透明度链时的降级路径](#11-特殊路径无透明度链时的降级路径)

---

## 1. 核心类概述

| 类 | 路径 | 行数 | 职责 |
|---|---|---|---|
| `GameRenderer` | `net/minecraft/client/renderer/GameRenderer.java` | 1542 | 帧入口,lightmap 绑定,postEffect,hand 渲染 |
| `LevelRenderer` | `net/minecraft/client/renderer/LevelRenderer.java` | 3182 | 世界渲染主流程,区块层渲染,实体/块实体调度 |
| `FogRenderer` | `net/minecraft/client/renderer/FogRenderer.java` | 366 | 雾颜色和距离计算,单一静态方法类 |
| `Camera` | `net/minecraft/client/Camera.java` | 未详读 | 摄像机位置/朝向/投影管理 |
| `RenderTarget` | `com/mojang/blaze3d/pipeline/RenderTarget.java` | 未详读 | OpenGL framebuffer 封装 |
| `MainTarget` | `com/mojang/blaze3d/pipeline/MainTarget.java` | 未详读 | 主 framebuffer(继承 RenderTarget) |
| `PostChain` | `net/minecraft/client/renderer/PostChain.java` | 未详读 | 后处理链(bloom、色调映射等) |
| `ChunkRenderDispatcher` | `net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.java` | 未详读 | 区块渲染编译调度 |

---

## 2. GameRenderer.render — 帧级入口

**文件**: `GameRenderer.java:900`

```java
public void render(float partialTicks, long nanoTime, boolean renderLevel)
```

### 2.1 调用链概览

```
Minecraft.runTick() → GameRenderer.render()
  ├─ 暂停检查 (pauseOnLostFocus)
  ├─ [renderLevel=true] 渲染 3D 世界
  │   ├─ GameRenderer.renderLevel(partialTicks, nanoTime, new PoseStack())
  │   │   ├─ lightTexture.updateLightTexture(partialTicks)
  │   │   ├─ Camera.setup(...)
  │   │   ├─ 视锥准备 + 投影矩阵计算
  │   │   ├─ LevelRenderer.renderLevel(...)  ← 核心
  │   │   ├─ FORGE AfterLevel event
  │   │   └─ renderItemInHand (第一人称手)
  │   ├─ tryTakeScreenshotIfNeeded()
  │   ├─ doEntityOutline() (发光实体轮廓)
  │   ├─ postEffect (后处理着色器,如 creeper、spider 等)
  │   └─ getMainRenderTarget().bindWrite(true)
  └─ 渲染 GUI 层
      ├─ RenderSystem.clear(256) (清 depth buffer)
      ├─ 投影矩阵切为正交投影
      ├─ confusionOverlay (反胃效果)
      ├─ gui.render(...)
      ├─ overlay.render(...) 或 screen.render(...)
      └─ RenderSystem.clear(256)
```

### 2.2 渲染 vs 不渲染判断

- `pauseOnLostFocus` + 窗口失去焦点超过 500ms → 暂停游戏,不更新帧
- `minecraft.noRender`:当窗口最小化时设为 true,跳过整个渲染

---

## 3. GameRenderer.renderLevel — 世界渲染准备

**文件**: `GameRenderer.java:1086`

```java
public void renderLevel(float partialTicks, long finishTimeNano, PoseStack poseStack)
```

### 3.1 执行步骤

1. **Lightmap 更新** (`:1087`): `lightTexture.updateLightTexture(partialTicks)` — 重新计算 16x16 lightmap 纹理(详见 `lighting-1.20.1.md`)。

2. **拾取** (`:1092`): `pick(partialTicks)` — 更新准星指向的方块/实体(MousePicker),用于高亮/破坏进度。

3. **摄像机设置** (`:1096–1121`):
   - 计算 FOV (`getFov`)
   - 生成投影矩阵 (`getProjectionMatrix`)
   - `bobHurt`/`bobView` 偏移
   - 反胃效果(旋转扭曲, CONFUSION=7倍速 / 下界传送门=20倍速)
   - `camera.setup(level, entity, ...)` 设置位置和朝向
   - Forge `ViewportEvent.ComputeCameraAngles` 事件

4. **视锥准备** (`:1130–1131`):
   - `setInverseViewRotationMatrix` 设置逆视图旋转矩阵
   - `LevelRenderer.prepareCullFrustum(poseStack, cameraPos, projectionMatrix)`

5. **世界渲染** (`:1132`): `LevelRenderer.renderLevel(...)`

6. **Forge AfterLevel 事件** (`:1134`): `RenderLevelStageEvent.Stage.AFTER_LEVEL`

7. **手部渲染** (`:1135–1139`): `RenderSystem.clear(256)` → `renderItemInHand`

---

## 4. LevelRenderer.renderLevel — 世界渲染主流程

**文件**: `LevelRenderer.java:1128`

```java
public void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime,
    boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
    LightTexture lightTexture, Matrix4f projectionMatrix)
```

### 4.1 阶段清单(profiler 层级)

按 profiler push/pop 顺序:

```
1. light_update_queue → pollLightUpdates
2. light_updates → getLightEngine().runLightUpdates
3. culling → 视锥更新(capturedFrustum 或 cullingFrustum)
4. captureFrustum → 视锥捕获(调试用)
5. clear → FogRenderer.setupColor + levelFogColor + RenderSystem.clear(16640)
6. sky → FogMode.FOG_SKY → renderSky
   └ [FORGE] AFTER_SKY
7. fog → FogMode.FOG_TERRAIN
8. terrain_setup → setupRender(camera, frustum, ...)
9. compilechunks → compileChunks(camera)
10. terrain → renderChunkLayer(SOLID) → renderChunkLayer(CUTOUT_MIPPED) → renderChunkLayer(CUTOUT)
11. entities → entity 遍历 + 渲染 + endBatch
    └ [FORGE] AFTER_ENTITIES
12. blockentities → 区块内 BE + 全局 BE + endBatch
    └ [FORGE] AFTER_BLOCK_ENTITIES
13. destroyProgress → 方块破坏动画
14. outline → 方块选择高亮
15. translucent → renderChunkLayer(TRANSLUCENT) → tripwire → particles
    └ [FORGE] AFTER_PARTICLES
16. clouds → renderClouds(+ cloudsTarget)
17. weather → renderSnowAndRain(+ WEATHER_TARGET)
    └ [FORGE] AFTER_WEATHER
18. renderWorldBorder
19. [IF transparencyChain] transparencyChain.process()
20. renderDebug
21. FogRenderer.setupNoFog
```

---

## 5. 阶段详解

### 5.1 光照更新 (`:1133–1136`)

```java
this.level.pollLightUpdates();                          // 轮询待处理的光照更新
this.level.getChunkSource().getLightEngine().runLightUpdates();  // 执行光照传播
```

发生在每帧渲染开始,确保光照数据最新。

### 5.2 视锥剔除 (`:1142–1156`)

- 优先使用 `capturedFrustum`(自由视角模式下预先捕获的视锥)
- 否则使用 `cullingFrustum`(prepareCullFrustum 中计算)
- `captureFrustum` 用于调试:在下一帧用上一帧的视锥重新渲染,保持可见性加载一致

### 5.3 清屏与雾色 (`:1158–1161`)

```java
FogRenderer.setupColor(camera, partialTick, level, renderDistance, darkenWorldAmount);
FogRenderer.levelFogColor();
RenderSystem.clear(16640, Minecraft.ON_OSX);
```

`16640 = 16384 (COLOR_BUFFER_BIT) | 256 (DEPTH_BUFFER_BIT)`

### 5.4 天空 (`:1166–1169`)

```
FogRenderer.setupFog(FOG_SKY, ...)
RenderSystem.setShader(GameRenderer::getPositionShader)
renderSky(poseStack, projectionMatrix, partialTick, camera, ...)
```

天空雾比地形雾更近,云层和太阳/月亮在天空 pass 中渲染。

### 5.5 地形设置与编译 (`:1173–1176`)

```
FogRenderer.setupFog(FOG_TERRAIN, ...)   // 地形雾
setupRender(camera, frustum, ...)         // 视锥裁剪,更新 visibleChunks
compileChunks(camera)                     // 触发待编译区块的 build task
```

### 5.6 地形渲染 (`:1178–1187`)

```
renderChunkLayer(RenderType.solid(), ...)        // 不透明层
setBlurMipmap(false, ...)                         // 修复树叶闪烁
renderChunkLayer(RenderType.cutoutMipped(), ...)  // 半透明裁剪+mipmap(树叶)
restoreLastBlurMipmap()
renderChunkLayer(RenderType.cutout(), ...)        // 全透明裁剪(玻璃、铁轨)
Lighting.setupNetherLevel/setupLevel              // 光照方向
```

**`renderChunkLayer` 详解** (`:1459–1536+`):
- 对 `renderChunksInFrustum` 列表迭代
- SOLID/CUTOUT_MIPPED/CUTOUT:正向迭代(近→远)
- TRANSLUCENT:反向迭代(远→近),且先做半透明排序(`resortTransparency`,摄像机移动超过 1m 时触发,最多重排 15 个区块)
- 设置 shader uniforms:Sampler0–11, MODEL_VIEW_MATRIX, PROJECTION_MATRIX, FOG_START/END/COLOR/SHAPE, GAME_TIME, CHUNK_OFFSET
- 通过 VertexBuffer 绘制

### 5.7 实体渲染 (`:1190–1245`)

```java
// 准备 framebuffer
if (itemEntityTarget != null) {
    itemEntityTarget.clear(Minecraft.ON_OSX);
    itemEntityTarget.copyDepthFrom(mainRenderTarget);
    mainRenderTarget.bindWrite(false);          // 切回主 RT 写入
}
if (weatherTarget != null) weatherTarget.clear(Minecraft.ON_OSX);
if (shouldShowEntityOutlines()) {
    entityTarget.clear(Minecraft.ON_OSX);
    mainRenderTarget.bindWrite(false);
}

// 遍历所有实体
for (Entity entity : level.entitiesForRendering()) {
    if (应渲染) {
        renderEntity(entity, d0, d1, d2, partialTick, poseStack, bufferSource);
    }
}

// 结束批次
bufferSource.endBatch(entitySolid/cutout/cutoutNoCull/smoothCutout);
```

- **itemEntityTarget**: 物品实体写入独立 RT,后续 blend 到主 RT(支持半透明排序)
- **entityTarget**: 发光实体轮廓目标(Glowing effect)
- **weatherTarget**: 天气粒子目标

实体渲染条件(逐一判断):
1. `entityRenderDispatcher.shouldRender(entity, frustum, ...)` — 视锥内
2. 或 `entity.hasIndirectPassenger(minecraft.player)` — 骑乘关系
3. `isOutsideBuildHeight` 或 `isChunkCompiled` — 区块已编译
4. 不是摄像机附着实体(或摄像机 detached、实体睡觉)
5. 不是 LocalPlayer(或摄像机附着于自身且非旁观模式)

### 5.8 块实体渲染 (`:1248–1309`)

```
// 区块内 BE
for (RenderChunkInfo : renderChunksInFrustum) {
    for (BlockEntity : chunk.getCompiledChunk().getRenderableBlockEntities()) {
        if (frustum.isVisible(be.getRenderBoundingBox())) {
            // 检查破坏动画叠加
            blockEntityRenderDispatcher.render(be, ...)
        }
    }
}

// 全局 BE
synchronized(globalBlockEntities) {
    for (BlockEntity : globalBlockEntities) {
        // 类似渲染
    }
}

// 结束大量批次
bufferSource.endBatch(solid/endPortal/endGateway/solidBlockSheet/cutoutBlockSheet
    /bedSheet/shulkerBoxSheet/signSheet/hangingSignSheet/chestSheet/...)
outlineBufferSource.endOutlineBatch()

// 发光实体轮廓后处理
if (flag2) {
    entityEffect.process(partialTick);
    mainRenderTarget.bindWrite(false);
}
```

### 5.9 方块破坏动画 (`:1314–1332`)

遍历 `destructionProgress` map,对距离摄像机 32m 内的破坏进度,用 `SheetedDecalTextureGenerator` 渲染对应阶段的破坏纹理。

### 5.10 半透明与粒子 (`:1365–1394`)

**有 transparencyChain 时(高端图形设置)**:
```
translucentTarget.clear + copyDepthFrom(main)
renderChunkLayer(TRANSLUCENT, ...)
renderChunkLayer(TRIPWIRE, ...)
particlesTarget.clear + copyDepthFrom(main)
PARTICLES_TARGET.setupRenderState()  → 绑定 particlesTarget
particleEngine.render(poseStack, bufferSource, lightTexture, camera, partialTick, frustum)
[FORGE] AFTER_PARTICLES
PARTICLES_TARGET.clearRenderState()
```

**无 transparencyChain 时(快速/流畅图形设置)**:粒子直接渲染到主 RT,无独立 translucentTarget。

### 5.11 云 (`:1400–1412`)

```java
if (cloudsType != CloudStatus.OFF) {
    if (transparencyChain != null) {
        cloudsTarget.clear(Minecraft.ON_OSX);
        CLOUDS_TARGET.setupRenderState();  // 绑定 cloudsTarget
        renderClouds(...);
        CLOUDS_TARGET.clearRenderState();
    } else {
        RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
        renderClouds(...);  // 直接写入主 RT
    }
}
```

### 5.12 天气与后处理 (`:1414–1429`)

**有 transparencyChain**:
```
WEATHER_TARGET.setupRenderState()
renderSnowAndRain(lightTexture, partialTick, ...)
[FORGE] AFTER_WEATHER
renderWorldBorder(camera)
WEATHER_TARGET.clearRenderState()
transparencyChain.process(partialTick)   ← 全部子 pass 执行
mainRenderTarget.bindWrite(false)
```

**无 transparencyChain**:
```
RenderSystem.depthMask(false)
renderSnowAndRain(...)
[FORGE] AFTER_WEATHER
renderWorldBorder(camera)
RenderSystem.depthMask(true)
```

### 5.13 调试渲染 (`:1434`)

```java
renderDebug(poseStack, bufferSource, camera);
bufferSource.endLastBatch();
```

---

## 6. Framebuffer 链

### 6.1 LevelRenderer 中的 RenderTarget 字段

| 字段 | 行号 | 条件 | 用途 |
|---|---|---|---|
| `mainTarget` | (Minecraft.getMainRenderTarget) | 始终存在 | 主颜色/深度缓冲区 |
| `entityTarget` | `:199` | `entityEffect != null` | 发光实体轮廓 |
| `translucentTarget` | `:203` | `transparencyChain != null` | 半透明方块层 |
| `itemEntityTarget` | `:205` | `transparencyChain != null` | 物品实体 |
| `particlesTarget` | `:207` | `transparencyChain != null` | 粒子 |
| `weatherTarget` | `:209` | `transparencyChain != null` | 天气效果 |
| `cloudsTarget` | `:211` | `transparencyChain != null` | 云 |

### 6.2 创建与销毁 (`:487–538`)

从 transparency.json 加载 PostChain 时创建这些 RT:
```
translucentTarget = rendertarget1
itemEntityTarget  = rendertarget2
particlesTarget   = rendertarget3
weatherTarget     = rendertarget4
cloudsTarget      = rendertarget
```

它们与 `mainTarget` **共享深度缓冲区**(通过 `copyDepthFrom`),尺寸相同。

### 6.3 写入流程

```
1. mainTarget        ← clear(COLOR|DEPTH) → sky → SOLID/CUTOUT_MIPPED/CUTOUT → entities → block entities
2. translucentTarget ← clear + copyDepthFrom(main) → TRANSLUCENT → tripwire
3. itemEntityTarget  ← clear + copyDepthFrom(main)
4. particlesTarget   ← clear + copyDepthFrom(main)
5. cloudsTarget      ← clear               (无 copyDepthFrom)
6. weatherTarget     ← (由 WEATHER_TARGET shard 绑定)
7. transparencyChain.process() → 将所有 RT 合成到 mainTarget
```

---

## 7. 后处理透明度链

**文件**: `LevelRenderer.java:487`

```java
ResourceLocation resourcelocation = new ResourceLocation("shaders/post/transparency.json");
PostChain postchain = PostChain.load(...);  // 若失败则禁用高端图形
this.transparencyChain = postchain;
```

### 7.1 功能

transparencyChain 包含多个子 pass,将独立的 framebuffer(translucentTarget/particlesTarget/cloudsTarget/weatherTarget)按正确深度顺序混合到 mainTarget。

### 7.2 触发条件

- 用户图形设置不是 FAST/FABULOUS → `transparencyChain != null`
- 窗口 resize 时调用 `transparencyChain.resize(width, height)`

### 7.3 执行

```java
this.transparencyChain.process(partialTick);
this.minecraft.getMainRenderTarget().bindWrite(false);  // 恢复主 RT 绑定
```

在 `process` 调用中,transparency.json 定义的 shader passes 逐个执行,最终输出到 mainTarget。

---

## 8. 摄像机与视锥剔除

### 8.1 Camera 设置

**GameRenderer.renderLevel**:1106`:
```java
camera.setup(level, cameraEntity, !firstPerson, mirrored, partialTicks);
```

Camera 类管理:
- 位置(position),朝向(yaw/pitch)
- 视锥体(cullingFrustum),通过 `getPickRay` 进行射线拾取
- 通过 PoseStack 的旋转矩阵生成 view matrix

### 8.2 视锥准备

**LevelRenderer.prepareCullFrustum** (`:1120-1126`):
```java
Matrix4f matrix4f = poseStack.last().pose();
cullingFrustum = new Frustum(matrix4f, projectionMatrix);
cullingFrustum.prepare(camX, camY, camZ);
```

`Frustum` 使用 view matrix 和 projection matrix 构建视锥体平面(6个平面)。

### 8.3 视锥使用

- `setupRender` (`:1174`): 用视锥裁剪 terrain,确定可见区块
- 实体渲染 (`:1211`): `entityRenderDispatcher.shouldRender(entity, frustum, camX, camY, camZ)`
- 块实体渲染 (`:1253`): `frustum.isVisible(blockentity.getRenderBoundingBox())`

---

## 9. 雾系统

**文件**: `FogRenderer.java`

### 9.1 FogMode 枚举 (`:338`)

```java
public static enum FogMode {
    FOG_SKY,       // 天空雾(较近)
    FOG_TERRAIN;   // 地形雾(较远)
}
```

### 9.2 雾设置流程

1. **清屏时** (`LevelRenderer:1159`)
   ```java
   FogRenderer.setupColor(camera, partialTick, level, renderDistance, darkenWorldAmount);
   FogRenderer.levelFogColor();
   ```
   计算雾颜色(依赖维度、时间、生物群系、水下/熔岩等),并通过 `RenderSystem.clearFogColor` 设置。

2. **天空阶段** (`:1164`)
   ```java
   FogRenderer.setupFog(camera, FOG_SKY, renderDistance, isFoggy, partialTick);
   ```

3. **地形阶段** (`:1172`)
   ```java
   FogRenderer.setupFog(camera, FOG_TERRAIN, Math.max(renderDistance, 32.0F), isFoggy, partialTick);
   ```

4. **渲染结束** (`:1438`)
   ```java
   FogRenderer.setupNoFog();
   ```

### 9.3 setupFog 内部逻辑

`s` (`:217–310)`:
- 计算 `farPlaneDistance`(渲染距离 * 16)
- 水/熔岩雾:使用近距离雾(far=水/熔岩可见度)
- 虚空雾:基岩层以下 darkening
- FOG_SKY:雾从 0 开始,用于天空
- FOG_TERRAIN:雾从 `farPlaneDistance * 0.75` 开始

---

## 10. 区块渲染调度

### 10.1 ChunkRenderDispatcher

**文件**: `net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.java`

1.20.1 使用以**区块(Chunk)**为单位的渲染模型:
- `RenderChunk` 持有已编译的渲染数据
- `renderChunksInFrustum` 是 `List<RenderChunkInfo>` — 视锥内可见区块列表
- `compileChunks(camera)` 提交需要重新编译的区块
- `renderChunkLayer(renderType, ...)` 遍历 `renderChunksInFrustum` 绘制

### 10.2 setupRender (`:1174` 调用的方法)

更新 `renderChunksInFrustum`,判断哪些区块可见并需要显示。

---

## 11. 特殊路径:无透明度链时的降级路径

当 `transparencyChain == null`(用户选择 FAST/FABULOUS 图形)时:

1. **无独立 translucentTarget**:半透明方块直接渲染到主 RT
2. **粒子直接渲染**:`particleEngine.render()` 直接到主 RT,无 particlesTarget
3. **云直接渲染**:无 cloudsTarget
4. **天气渲染**:`RenderSystem.depthMask(false)` → 直接到主 RT → `depthMask(true)`
5. **跳过 transparencyChain.process()**

详见 `:1381–1395` 和 `:1423–1430` 的 else 分支。

---

## 总结

### 完整帧流程图

```
GameRenderer.render(partialTicks, nanoTime, true)
└─ GameRenderer.renderLevel(partialTicks, nanoTime, poseStack)
    ├─ lightTexture.updateLightTexture()
    ├─ pick()
    ├─ Camera setup + FOV + bobHurt/bobView
    ├─ FORGE ComputeCameraAngles event
    ├─ prepareCullFrustum
    └─ LevelRenderer.renderLevel(poseStack, partialTick, ...)
        ├─ light_update_queue → pollLightUpdates
        ├─ light_updates → runLightUpdates
        ├─ culling → 选择或计算视锥
        ├─ clear → FogColor + clear(16640)
        ├─ sky → FOG_SKY → renderSky → FORGE AFTER_SKY
        ├─ fog → FOG_TERRAIN
        ├─ terrain_setup → setupRender
        ├─ compilechunks → compileChunks
        ├─ terrain → SOLID → CUTOUT_MIPPED → CUTOUT
        ├─ Lighting setup
        ├─ entities → [prep targets] → loop entities → endBatch → FORGE AFTER_ENTITIES
        ├─ blockentities → loop BE → endBatch → entityEffect → FORGE AFTER_BLOCK_ENTITIES
        ├─ destroyProgress
        ├─ outline (方块/实体高亮)
        ├─ translucent → TRANSLUCENT → tripwire
        ├─ particles → FORGE AFTER_PARTICLES
        ├─ clouds
        ├─ weather → AFTER_WEATHER
        ├─ renderWorldBorder
        ├─ [IF transparencyChain] transparencyChain.process()
        ├─ renderDebug
        └─ FogRenderer.setupNoFog
    ├─ FORGE AFTER_LEVEL event
    └─ renderItemInHand (第一人称手)
├─ tryTakeScreenshotIfNeeded
├─ doEntityOutline()
├─ postEffect (creeper/spider 等效果)
└─ mainRenderTarget.bindWrite(true)
```

### Framebuffer 关联总结

| RT | 写入时机 | 深度来源 |
|---|---|---|
| `mainTarget` | clear → sky → SOLID/CUTOUT → entities → BE | 自有 |
| `itemEntityTarget` | 实体循环开始前 clear | copyDepthFrom(main) |
| `weatherTarget` | 实体循环开始前 clear | 自有 |
| `entityTarget` | 实体循环开始前 clear(若发光) | 自有 |
| `translucentTarget` | translucent 前 clear | copyDepthFrom(main) |
| `particlesTarget` | particles 前 clear | copyDepthFrom(main) |
| `cloudsTarget` | clouds 前 clear | 自有 |
| → 合成 | transparencyChain.process() | 合并到 mainTarget |
