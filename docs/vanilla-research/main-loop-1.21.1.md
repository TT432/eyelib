# 1.21.1 (NeoForge) Minecraft 渲染主循环分析报告

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [相对于 1.20.1 的关键变更](#1-相对于-1201-的关键变更)
2. [GameRenderer.render — 帧级入口](#2-gamerendererrender--帧级入口)
3. [GameRenderer.renderLevel — 世界渲染准备](#3-gamerendererrenderlevel--世界渲染准备)
4. [LevelRenderer.renderLevel — 世界渲染主流程](#4-levelrendererrenderlevel--世界渲染主流程)
5. [阶段详解](#5-阶段详解)
6. [Framebuffer 链](#6-framebuffer-链)
7. [后处理透明度链](#7-后处理透明度链)
8. [实体冻结与 DeltaTracker](#8-实体冻结与-deltatracker)
9. [区块→区段迁移](#9-区块区段迁移)
10. [特殊路径:无透明度链时的降级路径](#10-特殊路径无透明度链时的降级路径)

---

## 1. 相对于 1.20.1 的关键变更

| 方面 | 1.20.1 | 1.21.1 |
|---|---|---|
| 时间管理 | `float partialTicks` + `long nanoTime` | `DeltaTracker` 对象封装 |
| 区块渲染模型 | Chunk(区块) | Section(16×16×16 区段) |
| 区块调度 | `ChunkRenderDispatcher` / `renderChunksInFrustum` | `SectionRenderDispatcher` / `visibleSections` |
| 方法命名 | `renderChunkLayer` / `compileChunks` | `renderSectionLayer` / `compileSections` |
| 方法签名 | `renderLevel(poseStack, partialTick, ...)` | `renderLevel(DeltaTracker, ...)` |
| view matrix | PoseStack 中维护 | 独立 `Matrix4f frustumMatrix` 参数 |
| 实体冻结 | 无 | `TickRateManager.isEntityFrozen()` 检查 |
| 无透明链粒子路径 | 全部粒子在 translucent 后 | solid 粒子在 translucent 前(修复 MC-161917) |
| NeoForge 事件 | `net.minecraftforge.client.event.*` | `net.neoforged.neoforge.client.event.*` |
| 资源加载检查 | 无 `isGameLoadFinished()` 检查 | `flag = isGameLoadFinished()` 保护 |

---

## 2. GameRenderer.render — 帧级入口

**文件**: `GameRenderer.java:1004`

```java
public void render(DeltaTracker deltaTracker, boolean renderLevel)
```

### 2.1 整体结构

与 1.20.1 逻辑一致,差异点:

1. **资源加载保护** (`:1016`):
   ```java
   boolean flag = this.minecraft.isGameLoadFinished();
   ```
   只有游戏加载完成后才渲染世界(1.20.1 无此检查)。

2. **时间来源** (`:1028:1037`):
   ```java
   this.renderLevel(deltaTracker);
   this.postEffect.process(deltaTracker.getGameTimeDeltaTicks());
   ```

3. **GUI 时间** (`:1065`):
   ```java
   deltaTracker.getGameTimeDeltaPartialTick(false)
   ```

4. **GUI z-depth** (`:1057`):
   ```java
   matrix4fstack.translation(0.0F, 0.0F, 10000F - ...);
   // 1.20.1: translate(0.0D, 0.0D, 1000F - ...)
   ```
   GUI 远平面从 1000 改为 10000。

---

## 3. GameRenderer.renderLevel — 世界渲染准备

**文件**: `GameRenderer.java:1231`

```java
public void renderLevel(DeltaTracker deltaTracker)
```

### 3.1 执行步骤

1. **时间提取** (`:1232`):
   ```java
   float f = deltaTracker.getGameTimeDeltaPartialTick(true);
   ```

2. **拾取** (`:1238`): `pick(f)`

3. **实体冻结检查** (`:1244`):
   ```java
   float f1 = level.tickRateManager().isEntityFrozen(entity) ? 1.0F : f;
   camera.setup(level, entity, ..., f1);
   ```
   若实体被 tick 冻结(如 `/tick freeze`),摄像机使用 1.0 partialTick(无插值)。

4. **投影矩阵计算** (`:1249–1269`):
   - 不再使用 PoseStack 累积旋转,直接在 `matrix4f` 上操作
   - 混乱效果(反胃/传送门)用 `matrix4f.rotate/scale` 替代 PoseStack
   - 眩晕角度从度转为弧度:`(float)(Math.PI / 180.0)`

5. **独立 frustumMatrix** (`:1273`):
   ```java
   Quaternionf quaternionf = camera.rotation().conjugate(new Quaternionf());
   Matrix4f frustumMatrix = new Matrix4f().rotation(quaternionf);
   ```
   不再从 PoseStack 提取,而是直接从 Camera.rotation() 生成。

6. **视锥准备** (`:1274–1276`):
   ```java
   LevelRenderer.prepareCullFrustum(cameraPosition, frustumMatrix, projectionMatrix);
   ```

7. **LevelRenderer** (`:1277`):
   ```java
   renderLevel(deltaTracker, flag, camera, this, lightTexture, frustumMatrix, projectionMatrix);
   ```
   参数多了 `frustumMatrix` 和 `projectionMatrix` 分离。

8. **手部渲染** (`:1281–1283`):
   ```java
   this.renderItemInHand(camera, f, frustumMatrix);
   ```

---

## 4. LevelRenderer.renderLevel — 世界渲染主流程

**文件**: `LevelRenderer.java:921`

```java
public void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline,
    Camera camera, GameRenderer gameRenderer, LightTexture lightTexture,
    Matrix4f frustumMatrix, Matrix4f projectionMatrix)
```

### 4.1 阶段清单

与 1.20.1 的阶段顺序**完全一致**:

```
1. light_update_queue → pollLightUpdates
2. light_updates → runLightUpdates
3. culling → 视锥更新
4. captureFrustum
5. clear → FogRenderer.setupColor + levelFogColor + clear(16640)
6. sky → FOG_SKY → renderSky → NeoForge AFTER_SKY
7. fog → FOG_TERRAIN
8. terrain_setup → setupRender
9. compile_sections → compileSections(camera)       ← 改名
10. terrain → SOLID → CUTOUT_MIPPED → CUTOUT
    ↓ renderSectionLayer 替代 renderChunkLayer      ← 改名
11. entities → 遍历 + 渲染 + endBatch + NeoForge AFTER_ENTITIES
12. blockentities → 遍历 + 渲染 + endBatch + NeoForge AFTER_BLOCK_ENTITIES
13. destroyProgress
14. outline / debug
15. translucent → TRANSLUCENT → tripwire → particles
    └ NeoForge AFTER_PARTICLES
16. clouds → renderClouds
17. weather → renderSnowAndRain → AFTER_WEATHER → renderWorldBorder
    └ [IF transparencyChain] transparencyChain.process()
18. renderDebug
19. FogRenderer.setupNoFog
```

### 4.2 关键差异点

#### renderSectionLayer 替代 renderChunkLayer (`:1280`)

```java
public void renderSectionLayer(RenderType renderType, double x, double y, double z,
    Matrix4f frustrumMatrix, Matrix4f projectionMatrix)
```

- 遍历 `visibleSections` (List\<SectionRenderDispatcher.RenderSection\>) 替代 `renderChunksInFrustum`
- 使用 `setDefaultUniforms` 一次性设置所有标准 uniform:
  ```java
  shaderinstance.setDefaultUniforms(VertexFormat.Mode.QUADS, frustrumMatrix, projectionMatrix, window);
  ```
- 手动设置 `CHUNK_OFFSET` uniform(基于 Section 原点偏移)

#### 实体遍历 (`:1009–1044`)

关键差异:
```java
float f2 = deltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity));
```
冻结实体使用 `!frozen` → `true` → 使用 1.0F partialTick(无插值)。

#### PoseStack 管理 (`:1001–1004`)

```java
Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
matrix4fstack.pushMatrix();
matrix4fstack.mul(frustumMatrix);
RenderSystem.applyModelViewMatrix();
```

View matrix 通过 `Matrix4fStack` 管理,独立于 `PoseStack`。

---

## 5. 阶段详解

### 5.1 Section 编译 (`:970`)

```java
profilerfiller.popPush("compile_sections");   // 1.20.1 是 "compilechunks"
this.compileSections(camera);                 // 1.20.1 是 compileChunks
```

### 5.2 地形渲染 (`:972–976`)

```java
this.renderSectionLayer(RenderType.solid(), d0, d1, d2, frustumMatrix, projectionMatrix);
// 叶片 mipmap 修复(NeoForge)
this.renderSectionLayer(RenderType.cutoutMipped(), d0, d1, d2, frustumMatrix, projectionMatrix);
this.renderSectionLayer(RenderType.cutout(), d0, d1, d2, frustumMatrix, projectionMatrix);
```

### 5.3 实体/块实体渲染

与 1.20.1 几乎相同,差异:
- `visibleSections` 替代 `renderChunksInFrustum`
- `getCompiled()` 替代 `getCompiledChunk()`
- `isSectionCompiled` 替代 `isChunkCompiled`
- NeoForge `isBlockEntityRendererVisible` hook 替代直接 `frustum.isVisible`

### 5.4 无透明度链时的粒子拆分 (`:1195–1210`)

```java
} else {
    // 先渲染不透明粒子(在 translucent 之前)
    profilerfiller.popPush("solid_particles");
    this.minecraft.particleEngine.render(lightTexture, camera, f, frustum,
        type -> !type.isTranslucent());

    profilerfiller.popPush("translucent");
    // translucent terrain
    this.renderSectionLayer(RenderType.translucent(), ...);
    bufferSource.endBatch();
    profilerfiller.popPush("string");
    this.renderSectionLayer(RenderType.tripwire(), ...);

    profilerfiller.popPush("particles");
    // 再渲染半透明粒子
    this.minecraft.particleEngine.render(lightTexture, camera, f, frustum,
        type -> type.isTranslucent());
}
```

这修复了 MC-161917(不透明粒子在水下穿模的问题)。

---

## 6. Framebuffer 链

与 1.20.1 **完全相同**。字段名从 `entityTarget` 改为 `entityTarget`(不变),类型和初始化逻辑一致。

RT 创建 (`:545–549`):
```java
this.translucentTarget = rendertarget1;
this.itemEntityTarget = rendertarget2;
this.particlesTarget = rendertarget3;
this.weatherTarget = rendertarget4;
this.cloudsTarget = rendertarget;
```

---

## 7. 后处理透明度链

**文件**: `LevelRenderer.java:532`

```java
ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("shaders/post/transparency.json");
// 1.20.1: new ResourceLocation("shaders/post/transparency.json")
```

使用新的 `ResourceLocation.withDefaultNamespace` 工厂方法,逻辑行为不变。

### NeoForge hook

`(:1114–1118)`:
```java
if (this.outlineEffectRequested) {
    flag2 |= this.shouldShowEntityOutlines();
    this.outlineEffectRequested = false;
}
```
新增的 outline 效果请求处理。

---

## 8. 实体冻结与 DeltaTracker

### 8.1 DeltaTracker 结构

`DeltaTracker` 替代了 1.20.1 中分离的 `partialTicks` 和 `nanoTime`:

| 方法 | 返回 | 用途 |
|---|---|---|
| `getGameTimeDeltaPartialTick(boolean)` | `float` | partialTick(true=realtime, false=游戏时钟) |
| `getGameTimeDeltaTicks()` | `float` | 前一帧的 partialTick(用于后处理) |

### 8.2 TickRateManager

```java
TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
boolean frozen = tickratemanager.isEntityFrozen(entity);
```

- 通过 `/tick freeze` 命令可冻结实体 tick
- 冻结时 `partialTick = 1.0F`,实体位置不插值(直接使用当前位置)
- Camera 和渲染管线均使用 `!frozen` 对应的 partialTick

---

## 9. 区块→区段迁移

### 9.1 类映射

| 1.20.1 | 1.21.1 |
|---|---|
| `ChunkRenderDispatcher` | `SectionRenderDispatcher` |
| `RenderChunk` (16×W×16, W 可变) | `RenderSection` (16×16×16) |
| `CompiledChunk` | `CompiledSection` |
| `renderChunksInFrustum` | `visibleSections` |
| `renderChunkLayer` | `renderSectionLayer` |
| `compileChunks` | `compileSections` |
| `isChunkCompiled` | `isSectionCompiled` |

### 9.2 影响

- **垂直维度**: 1.20.1 的 Chunk 覆盖整个 Y 轴(高度可变),1.21.1 的 Section 是固定 16×16×16 立方体
- **渲染列表**: `visibleSections` 数量远大于 `renderChunksInFrustum`
- **半透明排序**: `resortTransparency` 在 Section 级别执行,延迟从 15 区块降至 15 Section
- **编译粒度**: 更细粒度的编译 = 更快的增量更新

---

## 10. 特殊路径:无透明度链时的降级路径

与 1.20.1 相同基本逻辑,额外增加了粒子拆分:

```
无 transparencyChain 时:
  1. solid particles (不透明粒子先渲染)
  2. translucent terrain
  3. tripwire
  4. translucent particles (半透明粒子后渲染)
  5. clouds (直接到主 RT)
  6. weather (depthMask=false, 直接到主 RT)
```

---

## 总结

### 1.21.1 相较于 1.20.1 的核心变化

1. **DeltaTracker 统一时间**: 消除 `partialTicks` + `nanoTime` 二重奏
2. **Section 取代 Chunk**: 垂直分区使得编译/渲染更细粒度
3. **独立 frustumMatrix**: view matrix 从 Camera 直接生成,不再耦合 PoseStack
4. **实体冻结**: TickRateManager 支持 tick 冻结,渲染管线适配
5. **粒子拆分**: 无透明链路径中不透明/半透明粒子分层渲染,修复 MC-161917
6. **资源加载保护**: `isGameLoadFinished()` 防止未完成加载时渲染
7. **PoseStack 解耦**: 实体渲染中用独立 PoseStack,不依赖全局 RenderSystem 的 modelViewStack
