# Minecraft 环境渲染系统三版本对比(1.20.1 / 1.21.1 / 26.1.2)

> 基于 `.local_ref/mc/{version}/sources/` 源码分析。
> 本文是对比摘要,详细代码级分析参见各版本独立文档。

## 目录

1. [架构演进总览](#1-架构演进总览)
2. [天空渲染对比](#2-天空渲染对比)
3. [云渲染对比](#3-云渲染对比)
4. [天气渲染对比](#4-天气渲染对比)
5. [雾系统对比](#5-雾系统对比)
6. [维度特效对比](#6-维度特效对比)
7. [渲染调度对比](#7-渲染调度对比)
8. [API 演进](#8-api-演进)
9. [不变的元素](#9-不变的元素)

---

## 1. 架构演进总览

```
1.20.1 (Forge)
  LevelRenderer.java (~3200行)
    ├── renderSky()          → sky disc/太阳/月亮/星星/dark disc
    ├── renderClouds()       → cloud geometry + draw
    ├── renderSnowAndRain()  → rain/snow column particles
    ├── createStars/LightSky/DarkSky → VBO 预构建
    ├── renderEndSky()       → End 维度天空盒
    └── DimensionSpecialEffects → 维度差异(Forge 扩展)

1.21.1 (NeoForge)
  同 1.20.1 架构,增量变更:
    ├── BufferBuilder API 切换(addVertex/set* 替代 vertex/uv/color)
    ├── MeshData 替代 RenderedBuffer
    ├── 云使用 RenderType.cloudsDepthOnly() / RenderType.clouds()
    ├── renderSky 接受 frustumMatrix(不再外部 PoseStack)
    └── CloudStatus +StringRepresentable + CODEC

26.1.2 (NeoForge)
  完全重建——专用渲染器 + GPU 管线:
    ├── SkyRenderer.java (526行)     → 天空渲染
    ├── CloudRenderer.java (354行)   → 云渲染
    ├── WeatherEffectRenderer.java (308行) → 天气
    ├── LevelRenderer.java (1641行)  → FrameGraph 调度(不再含渲染逻辑)
    ├── RenderPipelines.java         → GPU Pipeline 定义
    ├── SkyRenderState/WeatherRenderState → 渲染状态
    ├── FogRenderer (实例) + FogEnvironment → 雾系统
    └── EnvironmentAttributes → 数据驱动天空参数
```

---

## 2. 天空渲染对比

### 2.1 组件结构

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 渲染器 | `LevelRenderer.renderSky()` | 同 1.20.1 | `SkyRenderer` 独立类 |
| 几何体 | `VertexBuffer`(sky/star/dark) | 同 1.20.1 | `GpuBuffer`(top/bottom/star/endFlash/sun/moon/sunrise) |
| 天体纹理 | `SUN_LOCATION`/`MOON_LOCATION`(独立文件) | 同 1.20.1 | `TextureAtlas`(CELESTIALS atlas, sprite "sun"/"moon/*") |
| 颜色传递 | `RenderSystem.setShaderColor()` | 同 1.20.1 | `DynamicTransforms` Uniform (ARGB vector) |
| 着色器 | `PositionShader`/`PositionTexShader`/`PositionColorShader` | 同 1.20.1 | Named pipelines: `SKY`/`CELESTIAL`/`STARS`/`SUNRISE_SUNSET`/`END_SKY` |

### 2.2 太阳/月亮渲染

```java
// 1.20.1/1.21.1: 直接构建 quad
bufferbuilder.vertex(matrix, -30, 100, -30).uv(0,0).endVertex();
bufferbuilder.vertex(matrix,  30, 100, -30).uv(1,0).endVertex();
// ...
BufferUploader.drawWithShader(bufferbuilder.end());

// 26.1.2: 预构建 GpuBuffer + RenderPass
renderPass.setPipeline(RenderPipelines.CELESTIAL);
renderPass.bindTexture("Sampler0", celestialsAtlas.getTextureView(), ...);
renderPass.setVertexBuffer(0, this.sunBuffer);
renderPass.setIndexBuffer(indexBuffer, ...);
renderPass.drawIndexed(0, 0, 6, 1);
```

### 2.3 星空渲染

| 方面 | 1.20.1/1.21.1 | 26.1.2 |
|------|--------------|--------|
| 构建 | `drawStars()` 即时构建 1500 颗星 | `buildStars()` 构造时构建,永久缓存 |
| 缓冲 | `VertexBuffer` | `GpuBuffer` |
| 管线 | `GameRenderer.getPositionShader()` | `RenderPipelines.STARS`(OVERLAY blend) |
| 星旋转 | 无(相机相对) | `starAngle`(通过 PoseStack 旋转) |

### 2.4 End 天空

```java
// 1.20.1/1.21.1: 每帧构建 6 个 QUADS
for(int i = 0; i < 6; ++i) {
    bufferbuilder.begin(QUADS, POSITION_TEX_COLOR);
    bufferbuilder.vertex(-100,-100,-100).uv(0,0).color(40,40,40,255).endVertex();
    // ...
    tesselator.end();
}

// 26.1.2: 预构建 endSkyBuffer, 一次 draw
renderPass.setPipeline(RenderPipelines.END_SKY);
renderPass.setVertexBuffer(0, this.endSkyBuffer);
renderPass.drawIndexed(0, 0, 36, 1);
```

---

## 3. 云渲染对比

### 3.1 核心差异

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 纹理加载 | 直接绑定 PNG | 同 1.20.1 | `SimplePreparableReloadListener` 生命周期 |
| Cell 编码 | 无(即时构建) | 同 1.20.1 | `packCellData()` 预处理为 `long[]` |
| 几何体构建 | 每帧 `buildClouds()` | 同 1.20.1 | 条件重建(`buildMesh()` → UTB buffer) |
| 顶点格式 | `POSITION_TEX_COLOR_NORMAL` | 同 1.20.1 | `EMPTY`(vertex pulling from texel buffer) |
| Uniform | `levelFogColor()` 内部状态 | 同 1.20.1 | `CloudInfo` UBO + `CloudFaces` texel buffer |
| 双 Pass 实现 | 手动 `colorMask(false,...)` | `RenderType.cloudsDepthOnly()` | 未确认(管线可能内部处理) |
| 重建条件 | camera cellX/Y/Z + cloudType + color | 同 1.20.1 | camera cellX/Z + relativePos + cloudStatus |

### 3.2 云几何体构建(不变的核心算法)

所有版本共享相同的核心参数:
- Cell 大小: 12.0 × 4.0 × 12.0 方块
- 移动速度: 0.03 cell/tick = ~0.36 方块/秒
- FANCY: 每个 cell 为完整 3D 盒体(最多 6 个面)
- FAST: 单层向下平面
- Z 偏移: +0.33 * 12 = +3.96(纹理对齐)

### 3.3 CloudStatus 演进

```
1.20.1: enum CloudStatus implements OptionEnum
         OFF(0) / FAST(1) / FANCY(2)
         +getId(), +getKey()

1.21.1: enum CloudStatus implements OptionEnum, StringRepresentable
         OFF(0,"false") / FAST(1,"fast") / FANCY(2,"true")
         +CODEC, +getSerializedName(), +legacyName

26.1.2: enum CloudStatus implements StringRepresentable
         OFF("false") / FAST("fast") / FANCY("true")
         -OptionEnum, -getId()
         +caption (Component)
```

---

## 4. 天气渲染对比

### 4.1 降雨/降雪

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 渲染器 | `LevelRenderer.renderSnowAndRain()` | 同 1.20.1 | `WeatherEffectRenderer.render()` |
| 雨半径 | 5/10 (fast/fancy) | 同 1.20.1 | `options.weatherRadius().get()`(用户可配) |
| 方向表 | `rainSizeX[1024]`/`rainSizeZ[1024]`(构造时) | 同 1.20.1 | 同,`columnSizeX[1024]`/`columnSizeZ[1024]` |
| 雨动画种子 | `ticks + posHash & 31` | `(ticks & 131071) + (posHash & 0xFF)`(改进) | 通过 `RandomSource` per-column |
| 光照传递 | `bufferbuilder.uv2(packedLight)` | `bufferbuilder.setLight(packedLight)` | `ColumnInstance.lightCoords` |
| 着色器 | `GameRenderer.getParticleShader()` | 同 1.20.1 | `RenderPipelines.WEATHER_DEPTH_WRITE` / `WEATHER_NO_DEPTH_WRITE` |
| 状态管理 | 内联局部变量 | 同 1.20.1 | `WeatherRenderState`(rainColumns/snowColumns 列表) |

### 4.2 状态提取模式(26.1.2 新增)

```java
// 26.1.2: 渲染前提取,渲染时使用
weatherEffectRenderer.extractRenderState(level, ticks, partialTicks, camPos, weatherRenderState);
// ...
weatherEffectRenderer.render(cameraPos, weatherRenderState, levelRenderState);
```

---

## 5. 雾系统对比

| 方面 | 1.20.1 / 1.21.1 | 26.1.2 |
|------|-----------------|--------|
| 类型 | 静态工具类 `FogRenderer` | 实例类 `FogRenderer` + `FogEnvironment` 层次结构 |
| 状态存储 | 全局静态变量 `fogRed/Green/Blue` | `FogData` record(immutable) |
| 设置方式 | `RenderSystem.setShaderFogStart/End/Color/Shape` | `RenderSystem.setShaderFog(gpuBufferSlice)` |
| 扩展点 | Forge/NeoForge event hook | `FogEnvironment` 抽象类继承 |
| 模式 | `FOG_SKY` / `FOG_TERRAIN` | `NONE` / `TERRAIN` / `SKY` |
| Fog 环境 | 内联 switch/if-else | 多态: `Atmospheric`/`Water`/`Lava`/`PowderedSnow`/`Blindness`/`Darkness` |
| GPU 传递 | 全局 OpenGL 状态 | Uniform Buffer Object(UBO) |

---

## 6. 维度特效对比

### 6.1 DimensionSpecialEffects 的移除

```
1.20.1/1.21.1:
  DimensionSpecialEffects (抽象类)
    ├── OverworldEffects: cloudLevel=192, SkyType=NORMAL
    ├── NetherEffects:    cloudLevel=NaN, SkyType=NONE
    ├── EndEffects:       cloudLevel=NaN, SkyType=END
    └── IForgeDimensionSpecialEffects / IDimensionSpecialEffectsExtension
         ├── renderSky(...)
         ├── renderClouds(...)
         ├── renderSnowAndRain(...)
         ├── tickRain(...)
         └── adjustLightmapColors(...)

26.1.2:
  DimensionSpecialEffects 类被**完全移除**。
  替代:
    ├── DimensionType.Skybox 枚举 (NONE/NORMAL/END)
    ├── DimensionType.cloudHeight() → float (NaN=无云)
    ├── EnvironmentAttributes (天空参数数据驱动)
    ├── FogEnvironment 子类 (维度雾行为)
    └── LevelRenderState.custom*Renderer 字段 (模组扩展)
```

### 6.2 扩展点变化

| 1.20.1/1.21.1 扩展方式 | 26.1.2 扩展方式 |
|------------------------|-----------------|
| 继承 `DimensionSpecialEffects` | 设置 `LevelRenderState.customSkyboxRenderer` |
| override `renderSky()` 完全接管 | 实现 `CustomSkyboxRenderer` 接口 |
| override `renderClouds()` | 实现 `CustomCloudsRenderer` 接口 |
| override `renderSnowAndRain()` | 实现 `CustomWeatherEffectRenderer` 接口 |
| Forge/NeoForge event hook | 同,保留 `RenderLevelStageEvent.AfterSky/AfterWeather` |

---

## 7. 渲染调度对比

### 7.1 1.20.1 / 1.21.1: 线性调用

```java
// LevelRenderer.renderLevel()
1. Clear + FOG_SKY setup
2. renderSky(poseStack, ...)
3. FOG_TERRAIN setup
4. renderChunkLayer(SOLID) → renderChunkLayer(CUTOUT_MIPPED) → renderChunkLayer(CUTOUT)
5. Lighting setup + Entities + BlockEntities
6. renderChunkLayer(TRANSLUCENT) + TRIPWIRE
7. Particles
8. renderClouds(poseStack, ...)           // 直接 draw 或到独立 target
9. renderSnowAndRain(lightTexture, ...)   // 直接 draw 或到独立 target
10. renderWorldBorder + Debug
```

### 7.2 26.1.2: FrameGraph

```java
// LevelRenderer: FrameGraphBuilder
FrameGraphBuilder frame = ...;
addSkyPass(frame, cameraState, skyFog, modelViewMatrix);       // → SkyRenderer
// ... (terrain/entities/blockentities/translucent/particles) ...
addCloudsPass(frame, cloudStatus, cameraPos, ...);              // → CloudRenderer
addWeatherPass(frame, terrainFog, modelViewMatrix);             // → WeatherEffectRenderer
addLateDebugPass(frame, cameraState, terrainFog, ...);          // → Gizmos
```

---

## 8. API 演进

### 8.1 顶点构建

```
1.20.1:
  bufferbuilder.begin(mode, format);
  bufferbuilder.vertex(x,y,z).uv(u,v).color(r,g,b,a).endVertex();
  BufferUploader.drawWithShader(bufferbuilder.end());  // → RenderedBuffer

1.21.1:
  BufferBuilder bb = tesselator.begin(mode, format);
  bb.addVertex(x,y,z).setUv(u,v).setColor(r,g,b,a);
  BufferUploader.drawWithShader(bb.buildOrThrow());  // → MeshData

26.1.2:
  // 预构建:
  BufferBuilder bb = new BufferBuilder(byteBuilder, mode, format);
  bb.addVertex(x,y,z).setUv(u,v).setColor(r,g,b,a);
  GpuBuffer buf = device.createBuffer(name, align, mesh.vertexBuffer());
  // 渲染:
  renderPass.setVertexBuffer(0, buf);
  renderPass.draw(startVertex, vertexCount);
```

### 8.2 纹理绑定

```
1.20.1/1.21.1:
  RenderSystem.setShaderTexture(0, SUN_LOCATION);

26.1.2:
  renderPass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
```

### 8.3 着色器

```
1.20.1/1.21.1:
  RenderSystem.setShader(GameRenderer::getPositionTexShader);

26.1.2:
  renderPass.setPipeline(RenderPipelines.CELESTIAL);
```

### 8.4 颜色/变换传递

```
1.20.1/1.21.1:
  RenderSystem.setShaderColor(r, g, b, a);
  PoseStack → RenderSystem.getModelViewStack() → applyModelViewMatrix();

26.1.2:
  GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
      .writeTransform(modelViewMatrix, colorVector, ...);
  renderPass.setUniform("DynamicTransforms", transforms);
```

---

## 9. 不变的元素

以下数值和算法在三版本中保持不变:

### 9.1 核心常量

| 常量 | 值 | 三版本一致性 |
|------|-----|------------|
| Sky disc 半径 | 512.0 | ✅ 一致 |
| 太阳大小 | 30.0 | ✅ 一致 |
| 月亮大小 | 20.0 | ✅ 一致 |
| 星星数量 | 1500 | ✅ 一致 |
| 星星种子 | 10842L | ✅ 一致 |
| 云 cell 大小 | 12.0 × 4.0 × 12.0 | ✅ 一致 |
| 云移动速度 | 0.03 cell/tick | ✅ 一致 |
| 云 wrap 周期 | 2048 cells | ✅ 一致 |
| Overworld 云高度 | 192.0 | ✅ 一致 |
| Nether/End 云 | NaN(无云) | ✅ 一致 |
| 日出日落范围 | cos ∈ [-0.4, 0.4] | ✅ 一致 |

### 9.2 核心算法

- 星空分布算法(球面均匀 + ballboard quads)
- Sky disc 构建(TRIANGLE_FAN, 10 vertices)
- 云几何体形状(FANCY 3D box / FAST flat plane)
- 颜色分层(底部 0.7× / 顶部 1.0× / 侧面 0.8-0.9×)
- 日/月位置计算(timeOfDay * 360° / moonPhase UV)
- 雨/雪粒子列生成
- End 天空盒 6 面构建

### 9.3 纹理资源

所有版本的纹理路径完全一致(26.1.2 中天体纹理迁移到 atlas 但 sprite 名称对应):
- `textures/environment/sun.png` → `"sun"` sprite
- `textures/environment/moon_phases.png` → `"moon/*"` sprites
- `textures/environment/clouds.png`
- `textures/environment/rain.png`
- `textures/environment/snow.png`
- `textures/environment/end_sky.png`
