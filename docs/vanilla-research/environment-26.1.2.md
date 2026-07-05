# 26.1.2 (NeoForge) Minecraft Vanilla Environment Rendering 分析报告

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [架构总览——从单体到专用渲染器](#1-架构总览从单体到专用渲染器)
2. [FrameGraph 渲染调度](#2-framegraph-渲染调度)
3. [SkyRenderer 全分析](#3-skyrenderer-全分析)
4. [SkyRenderState 渲染状态](#4-skyrenderstate-渲染状态)
5. [CloudRenderer 全分析](#5-cloudrenderer-全分析)
6. [WeatherEffectRenderer 全分析](#6-weathereffectrenderer-全分析)
7. [RenderPipelines 环境相关管线](#7-renderpipelines-环境相关管线)
8. [Fog 系统重构](#8-fog-系统重构)
9. [DimensionType.Skybox 替代 DimensionSpecialEffects.SkyType](#9-dimensiontypeskybox-替代-dimensionspecialeffectsskytype)
10. [EnvironmentAttributes 体系](#10-environmentattributes-体系)
11. [CloudStatus 变更](#11-cloudstatus-变更)

---

## 1. 架构总览——从单体到专用渲染器

26.1.2 对环境渲染进行了 **完全重建**:

| 组件 | 1.20.1/1.21.1 | 26.1.2 |
|------|--------------|--------|
| 天空渲染 | `LevelRenderer.renderSky()` | `SkyRenderer` 独立类 |
| 云渲染 | `LevelRenderer.renderClouds()` | `CloudRenderer` 独立类 |
| 天气渲染 | `LevelRenderer.renderSnowAndRain()` | `WeatherEffectRenderer` 独立类 |
| 渲染状态 | 内联变量 | `SkyRenderState`/`WeatherRenderState`/`LevelRenderState` |
| 渲染管线 | `RenderSystem.setShader()` + manual state | `RenderPipeline` + `RenderPass` |
| Fog | `FogRenderer` 静态方法 | `FogRenderer` 实例 + `FogEnvironment` 子类 |
| 调度 | `renderLevel()` 顺序调用 | `FrameGraphBuilder` pass 图 |
| 天体纹理 | 独立纹理文件 | TextureAtlas (CELESTIALS atlas) |
| 维度控制 | `DimensionSpecialEffects` | `DimensionType.Skybox` + `EnvironmentAttributes` |

### 核心模式: RenderState 提取

```java
// 每帧先提取状态,然后渲染
skyRenderer.extractRenderState(level, partialTicks, camera, skyRenderState);
cloudRenderer.render(color, cloudStatus, height, range, pos, time, partialTicks);
weatherEffectRenderer.extractRenderState(level, ticks, partialTicks, camPos, weatherRenderState);
```

---

## 2. FrameGraph 渲染调度

**文件**: `LevelRenderer.java`

### 2.1 Pass 顺序

```
1. "sky"          → addSkyPass()          → SkyRenderer
2. "terrain"      → 方块渲染
3. "entities"     → 实体渲染
4. "blockentities"→ 方块实体
5. "translucent"  → 半透明
6. "particles"    → 粒子(仅半透明粒子)
7. "clouds"       → addCloudsPass()       → CloudRenderer
8. "weather"      → addWeatherPass()      → WeatherEffectRenderer + WorldBorder
9. "late_debug"   → addLateDebugPass()    → Gizmos + Debug
```

### 2.2 addSkyPass (行 1290–1329)

```java
private void addSkyPass(FrameGraphBuilder frame, CameraRenderState cameraState,
    GpuBufferSlice skyFog, Matrix4fc modelViewMatrix) {
    if (skybox != DimensionType.Skybox.NONE) {
        FramePass pass = frame.addPass("sky");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> {
            // 1. 自定义天空优先
            if (customSkyboxRenderer == null || !customSkyboxRenderer.renderSky(...)) {
                RenderSystem.setShaderFog(skyFog);
                if (state.skybox == Skybox.END) {
                    skyRenderer.renderEndSky();
                    if (endFlashIntensity > 1.0E-5F)
                        skyRenderer.renderEndFlash(...);
                } else {
                    skyRenderer.renderSkyDisc(state.skyColor);
                    skyRenderer.renderSunriseAndSunset(...);
                    skyRenderer.renderSunMoonAndStars(...);
                    if (state.shouldRenderDarkDisc)
                        skyRenderer.renderDarkDisc();
                }
            }
            // 2. NeoForge AFTER_SKY event
        });
    }
}
```

### 2.3 addCloudsPass (行 778–801)

```java
private void addCloudsPass(FrameGraphBuilder frame, CloudStatus cloudStatus,
    Vec3 cameraPosition, long gameTime, float partialTicks,
    int cloudColor, float cloudHeight, int cloudRange, Matrix4fc modelViewMatrix) {
    FramePass pass = frame.addPass("clouds");
    // 有独立 cloud target 时读写它,否则读写 main target
    if (this.targets.clouds != null)
        this.targets.clouds = pass.readsAndWrites(this.targets.clouds);
    else
        this.targets.main = pass.readsAndWrites(this.targets.main);

    pass.executes(() -> {
        if (customCloudsRenderer == null || !customCloudsRenderer.renderClouds(...)) {
            this.cloudRenderer.render(cloudColor, cloudStatus, cloudHeight,
                cloudRange, cameraPosition, gameTime, partialTicks);
        }
    });
}
```

### 2.4 addWeatherPass (行 811–832)

```java
private void addWeatherPass(FrameGraphBuilder frame, GpuBufferSlice fog,
    Matrix4fc modelViewMatrix) {
    FramePass pass = frame.addPass("weather");
    if (this.targets.weather != null)
        this.targets.weather = pass.readsAndWrites(this.targets.weather);
    else
        this.targets.main = pass.readsAndWrites(this.targets.main);

    pass.executes(() -> {
        RenderSystem.setShaderFog(fog);
        this.weatherEffectRenderer.render(cameraState.pos,
            this.levelRenderState.weatherRenderState, this.levelRenderState);
        // AFTER_WEATHER event
        this.worldBorderRenderer.render(...);
    });
}
```

---

## 3. SkyRenderer 全分析

**文件**: `net/minecraft/client/renderer/SkyRenderer.java` (526 行)

### 3.1 构造与预构建缓冲

```java
public class SkyRenderer implements AutoCloseable {
    private final TextureAtlas celestialsAtlas;    // CELESTIALS atlas (天体)
    private final GpuBuffer starBuffer;             // 1500 stars
    private final GpuBuffer topSkyBuffer;           // y=+16 sky disc
    private final GpuBuffer bottomSkyBuffer;        // y=-16 dark disc
    private final GpuBuffer endSkyBuffer;           // End 6-face skybox
    private final GpuBuffer sunBuffer;              // 太阳 quad
    private final GpuBuffer moonBuffer;             // 月相 quads
    private final GpuBuffer sunriseBuffer;          // sunrise fan
    private final GpuBuffer endFlashBuffer;         // End flash quad
}
```

- 所有缓冲使用 `GpuBuffer`(显存缓冲),在构造时一次性构建
- 天体纹理从 `TextureAtlas` (CELESTIALS atlas) 获取,不再是独立文件

### 3.2 星构建 (行 182–214)

```java
private GpuBuffer buildStars() {
    RandomSource random = RandomSource.createThreadLocalInstance(10842L);
    // 1500 颗星, 种子 10842
    // 球面均匀分布, 拒绝 0.01 < length² < 1.0
    // 每颗星: 4 顶点 quad, 朝向相机(ballboard)
    // starSize = 0.15 + random * 0.1
    // 输出: GpuBuffer(40 MB 对齐)
}
```

与 1.20.1/1.21.1 相比: 算法完全一致,但输出为 `GpuBuffer` 而非 `VertexBuffer`。

### 3.3 Sky Disc 构建 (行 216–223)

```java
private void buildSkyDisc(VertexConsumer builder, float yy) {
    float x = Math.signum(yy) * 512.0F;
    builder.addVertex(0.0F, yy, 0.0F);         // 圆心
    for(int i = -180; i <= 180; i += 45) {      // 9 vertices = 10 total
        builder.addVertex(x * cos(i°), yy, 512 * sin(i°));
    }
}
```

- Light disc: yy=16
- Dark disc: yy=-16
- 10 vertices, TRIANGLE_FAN

### 3.4 太阳 quad (行 131–132)

```java
private static GpuBuffer buildSunQuad(TextureAtlas atlas) {
    return buildCelestialQuad("Sun quad", atlas.getSprite(SUN_SPRITE));
}
```

- `SUN_SPRITE` = `Identifier.withDefaultNamespace("sun")`
- 单位 quad (-1→+1),渲染时 scale 30

### 3.5 月相 quads (行 158–180)

```java
private static GpuBuffer buildMoonPhases(TextureAtlas atlas) {
    MoonPhase[] phases = MoonPhase.values();
    // 每个 phase 一个 unit quad,连续排列
    // 纹理坐标从 celestial atlas 的 "moon/<serializedName>" 获取
}
```

- 8 个月相(Full Moon 到 New Moon),共 32 个顶点
- 渲染时通过 `baseVertex` 偏移选择月相

### 3.6 renderSkyDisc (行 263–278)

```java
public void renderSkyDisc(int skyColor) {
    // 使用 RenderPass API
    GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
        .writeTransform(modelViewMatrix, ARGB.vector4fFromARGB32(skyColor), ...);
    try (RenderPass renderPass = device.createCommandEncoder()
            .createRenderPass("Sky disc", colorTex, empty, depthTex, empty)) {
        renderPass.setPipeline(RenderPipelines.SKY);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", dynamicTransforms);
        renderPass.setVertexBuffer(0, this.topSkyBuffer);
        renderPass.draw(0, 10);
    }
}
```

### 3.7 renderSunMoonAndStars (行 331–352)

```java
public void renderSunMoonAndStars(PoseStack poseStack, float sunAngle,
    float moonAngle, float starAngle, MoonPhase moonPhase,
    float rainBrightness, float starBrightness) {
    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
    // 太阳: CELESTIAL pipeline
    poseStack.pushPose(); poseStack.mulPose(Axis.XP.rotation(sunAngle));
    this.renderSun(rainBrightness, poseStack); poseStack.popPose();
    // 月亮: CELESTIAL pipeline
    poseStack.pushPose(); poseStack.mulPose(Axis.XP.rotation(moonAngle));
    this.renderMoon(moonPhase, rainBrightness, poseStack); poseStack.popPose();
    // 星星: STARS pipeline (仅 starBrightness > 0)
    if (starBrightness > 0.0F) {
        poseStack.pushPose(); poseStack.mulPose(Axis.XP.rotation(starAngle));
        this.renderStars(starBrightness, poseStack); poseStack.popPose();
    }
    poseStack.popPose();
}
```

### 3.8 renderSun (行 354–379)

```java
private void renderSun(float rainBrightness, PoseStack poseStack) {
    // Pose: translate(0, 100, 0) + scale(30, 1, 30)
    // Pipeline: RenderPipelines.CELESTIAL
    // Texture: celestialsAtlas (sun sprite)
    // Color modulation: (1,1,1,rainBrightness) — 雨天透明度
    renderPass.drawIndexed(0, 0, 6, 1);  // 1 quad = 6 indices
}
```

### 3.9 其他渲染方法

| 方法 | Pipeline | 说明 |
|------|----------|------|
| `renderMoon` | CELESTIAL | 根据 moonPhase 选择 baseVertex(0/4/8/.../28) |
| `renderStars` | STARS | BlendFunction.OVERLAY, starBrightness 调制颜色 |
| `renderSunriseAndSunset` | SUNRISE_SUNSET | z-scale = alpha, sunriseAndSunsetColor ARGB |
| `renderDarkDisc` | SKY | translate(0,12,0), bottomSkyBuffer, color=(0,0,0,1) |
| `renderEndSky` | END_SKY | 6-face skybox, endSkyBuffer, endSkyTexture |
| `renderEndFlash` | CELESTIAL | translate(0,100,0) + scale(60,1,60), intensity 调制 |

---

## 4. SkyRenderState 渲染状态

**文件**: `net/minecraft/client/renderer/state/level/SkyRenderState.java` (27 行)

```java
public class SkyRenderState {
    public DimensionType.Skybox skybox = Skybox.NONE;
    public boolean shouldRenderDarkDisc;
    public float sunAngle;
    public float moonAngle;
    public float starAngle;
    public float rainBrightness;
    public float starBrightness;
    public int sunriseAndSunsetColor;   // ARGB packed
    public MoonPhase moonPhase;
    public int skyColor;                // ARGB packed
    public float endFlashIntensity;
    public float endFlashXAngle;
    public float endFlashYAngle;
}
```

### 4.1 extractRenderState (行 280–303)

```java
public void extractRenderState(ClientLevel level, float partialTicks,
    Camera camera, SkyRenderState state) {
    state.skybox = level.dimensionType().skybox();
    if (state.skybox != Skybox.NONE) {
        if (state.skybox == Skybox.END) {
            // End flash 状态
            state.endFlashIntensity = endFlashState.getIntensity(partialTicks);
            state.endFlashXAngle = endFlashState.getXAngle();
            state.endFlashYAngle = endFlashState.getYAngle();
        } else {
            // 从 EnvironmentAttributes 获取角度和颜色
            state.sunAngle  = camera.attributeProbe().getValue(SUN_ANGLE, partialTicks);
            state.moonAngle = camera.attributeProbe().getValue(MOON_ANGLE, partialTicks);
            state.starAngle = camera.attributeProbe().getValue(STAR_ANGLE, partialTicks);
            state.rainBrightness = 1.0F - level.getRainLevel(partialTicks);
            state.starBrightness = camera.attributeProbe().getValue(STAR_BRIGHTNESS, partialTicks);
            state.sunriseAndSunsetColor = camera.attributeProbe().getValue(SUNRISE_SUNSET_COLOR, partialTicks);
            state.moonPhase = camera.attributeProbe().getValue(MOON_PHASE, partialTicks);
            state.skyColor = camera.attributeProbe().getValue(SKY_COLOR, partialTicks);
            state.shouldRenderDarkDisc = this.shouldRenderDarkDisc(partialTicks, level);
        }
    }
}
```

---

## 5. CloudRenderer 全分析

**文件**: `net/minecraft/client/renderer/CloudRenderer.java` (354 行)

### 5.1 架构

```java
public class CloudRenderer extends SimplePreparableReloadListener<Optional<TextureData>>
    implements AutoCloseable
```

- **资源加载**: 实现 `prepare/apply` 生命周期,从 `textures/environment/clouds.png` 加载纹理并预处理
- **GPU 缓冲**: `MappableRingBuffer` (UBO 云参数 + UTB 面数据)
- **cell 表示**: 每个纹理像素编码为 `long`(32bit color + 4bit neighbor flags)

### 5.2 纹理预处理 (行 65–99)

```java
protected Optional<TextureData> prepare(ResourceManager manager, ProfilerFiller profiler) {
    NativeImage texture = NativeImage.read(input);
    long[] cells = new long[width * height];
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int color = texture.getPixel(x, y);
            if (isCellEmpty(color)) {
                cells[x + y * width] = 0L;  // 空 cell
            } else {
                // 检查四邻是否为空
                boolean north = isCellEmpty(texture.getPixel(x, (y-1) % height));
                boolean east  = isCellEmpty(texture.getPixel((x+1) % height, y));
                boolean south = isCellEmpty(texture.getPixel(x, (y+1) % height));
                boolean west  = isCellEmpty(texture.getPixel((x-1) % height, y));
                cells[x + y * width] = packCellData(color, north, east, south, west);
            }
        }
    }
    return Optional.of(new TextureData(cells, width, height));
}
```

### 5.3 Cell Data 编码

```java
private static long packCellData(int color, boolean north, boolean east,
    boolean south, boolean west) {
    // bit 0: west empty, bit 1: south empty, bit 2: east empty, bit 3: north empty
    // bits 4-35: ARGB color
    return (long)color << 4 | (north?1:0)<<3 | (east?1:0)<<2
           | (south?1:0)<<1 | (west?1:0)<<0;
}
```

### 5.4 渲染 (行 137–229)

```java
public void render(int color, CloudStatus cloudStatus, float bottomY,
    int range, Vec3 cameraPosition, long gameTime, float partialTicks)
```

#### 相机相对位置判断

```java
float relativeBottomY = bottomY - cameraPosition.y;
float relativeTopY = relativeBottomY + 4.0F;
if (relativeTopY < 0.0F)        relativePos = ABOVE_CLOUDS;
else if (relativeBottomY > 0.0F) relativePos = BELOW_CLOUDS;
else                             relativePos = INSIDE_CLOUDS;
```

#### 云移动

```java
float cloudOffset = (gameTime % (texture.width * 400L)) + partialTicks;
double cloudX = cameraPosition.x + cloudOffset * 0.03;
double cloudZ = cameraPosition.z + 3.96;
// 纹理 wrap
cloudX -= floor(cloudX / (width*12)) * (width*12);
cloudZ -= floor(cloudZ / (height*12)) * (height*12);
```

- 移动速度: 0.03 blocks/tick = 0.6 blocks/s(与老版本相同)
- Z 偏移: +3.96(与 1.20.1 的 +0.33*12 等价)

#### UBO 写入

```java
Std140Builder.intoBuffer(view.data())
    .putVec4(ARGB.vector4fFromARGB32(color))  // 云颜色
    .putVec3(-xInCell, relativeBottomY, -zInCell)  // 偏移
    .putVec3(12.0F, 4.0F, 12.0F);  // cell 大小
```

### 5.5 网格构建(行 231–326)

#### buildFlatCell(FAST 模式)
```java
private void buildFlatCell(ByteBuffer faceBuffer, int x, int z) {
    this.encodeFace(faceBuffer, x, z, Direction.DOWN, 32);  // flag=32 = use top color
}
```

#### buildExtrudedCell(FANCY 模式)
- 根据相机位置(ABOVE/INSIDE/BELOW)决定哪些面可见
- ABOVE: 只渲染 DOWN 面
- BELOW: 只渲染 UP 面
- INSIDE: UP + DOWN + 四侧(仅邻接空 cell)
- 内部面(距相机≤1 cell): 额外添加 6 个带 FLAG_INSIDE_FACE(16) 的面

#### 面编码格式

```java
// 3 bytes per face:
// byte 0: x >> 1 (relative cell X, upper bits)
// byte 1: z >> 1 (relative cell Z, upper bits)
// byte 2: direction(3bit) | flags(3bit) | x_lsb(1bit) | z_lsb(1bit)
```

### 5.6 管线

```java
RenderPipeline renderPipeline = fancyClouds ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
```

#### CLOUDS_SNIPPET
- 顶点着色器: `core/rendertype_clouds`
- Uniform: `CloudInfo`(UBO) + `CloudFaces`(texel buffer R8I)
- 顶点格式: `EMPTY`(vertex pulling from texel buffer)

---

## 6. WeatherEffectRenderer 全分析

**文件**: `net/minecraft/client/renderer/WeatherEffectRenderer.java` (308 行)

### 6.1 架构

```java
public class WeatherEffectRenderer {
    private static final float RAIN_PARTICLES_PER_BLOCK = 0.225F;
    private static final int RAIN_RADIUS = 10;
    // 预计算 32×32 方向表(与老版本相同)
    private final float[] columnSizeX = new float[1024];
    private final float[] columnSizeZ = new float[1024];
}
```

### 6.2 状态提取 (行 81–113)

```java
public void extractRenderState(Level level, int ticks, float partialTicks,
    Vec3 cameraPos, WeatherRenderState renderState) {
    renderState.intensity = level.getRainLevel(partialTicks);
    if (renderState.intensity > 0.0F) {
        renderState.radius = Minecraft.getInstance().options.weatherRadius().get();
        // 遍历 cameraX/Z ± radius 范围
        for (int z = ...; z <= ...; z++) {
            for (int x = ...; x <= ...; x++) {
                int terrainHeight = level.getHeight(MOTION_BLOCKING, x, z);
                int y0 = max(camY - radius, terrainHeight);
                int y1 = max(camY + radius, terrainHeight);
                if (y1 != y0) {
                    Biome.Precipitation prec = getPrecipitationAt(level, pos);
                    if (prec == RAIN)
                        renderState.rainColumns.add(createRainColumnInstance(...));
                    else if (prec == SNOW)
                        renderState.snowColumns.add(createSnowColumnInstance(...));
                }
            }
        }
    }
}
```

### 6.3 ColumnInstance

```java
// 内 record 存储每列数据:
// - lightCoords: packed light
// - x, y0, y1, z: 世界坐标
// - rain: uvOffset, uvPhase
// - snow: uvOffsetX, uvOffsetZ, uvPhase
```

### 6.4 渲染 (行 121 起)

- 使用 `RenderPass` API,分两个 pass:
  1. RAIN columns → `WEATHER_DEPTH_WRITE` / `WEATHER_NO_DEPTH_WRITE`
  2. SNOW columns → 同样的 pipeline
- 雾通过 `RenderSystem.setShaderFog(fogSlice)` 设置
- 深度写入根据 `Minecraft.useShaderTransparency()` 决定

---

## 7. RenderPipelines 环境相关管线

**文件**: `net/minecraft/client/renderer/RenderPipelines.java`

### 7.1 SNIPPET 定义

| Snippet | 基础 | 顶点着色器 | 片元着色器 | 顶点格式 | 特性 |
|---------|------|-----------|-----------|---------|------|
| CLOUDS_SNIPPET | MATRICES_FOG_SNIPPET | core/rendertype_clouds | core/rendertype_clouds | EMPTY/QUADS | CloudInfo UBO + CloudFaces texel buffer |
| WEATHER_SNIPPET | PARTICLE_SNIPPET | core/particle | core/particle | PARTICLE/QUADS | TRANSLUCENT, no cull |

### 7.2 完整 Pipeline

| Pipeline | 位置 | 说明 |
|----------|------|------|
| `SKY` | :608 | MATRICES_FOG_SNIPPET, "core/sky", POSITION/TRIANGLE_FAN |
| `END_SKY` | :616 | MATRICES_PROJECTION_SNIPPET, "core/position_tex_color", TRANSLUCENT |
| `SUNRISE_SUNSET` | :626 | MATRICES_PROJECTION_SNIPPET, "core/position_color", TRANSLUCENT, TRIANGLE_FAN |
| `STARS` | :635 | MATRICES_PROJECTION_SNIPPET, "core/stars", BlendFunction.OVERLAY |
| `CELESTIAL` | :644 | MATRICES_PROJECTION_SNIPPET, "core/position_tex", BlendFunction.OVERLAY |
| `FLAT_CLOUDS` | :533 | CLOUDS_SNIPPET, no cull |
| `CLOUDS` | :536 | CLOUDS_SNIPPET, with cull |
| `WEATHER_DEPTH_WRITE` | :599 | WEATHER_SNIPPET, base depth state |
| `WEATHER_NO_DEPTH_WRITE` | :602 | WEATHER_SNIPPET, depth write disabled |

### 7.3 关键着色器文件

- `core/sky`: SKY pipeline(天空 disc)
- `core/stars`: STARS pipeline(星空)
- `core/position_tex`: CELESTIAL pipeline(太阳/月亮)
- `core/position_color`: SUNRISE_SUNSET pipeline(日出)
- `core/position_tex_color`: END_SKY pipeline(末地天空)
- `core/rendertype_clouds`: CLOUDS/FLAT_CLOUDS(云,vertex pulling)
- `core/particle`: WEATHER_*(天气粒子)

---

## 8. Fog 系统重构

### 8.1 新架构

```
FogRenderer (实例)
  ├── FogEnvironment (抽象)
  │   ├── AtmosphericFogEnvironment  (默认/地面)
  │   ├── WaterFogEnvironment
  │   ├── LavaFogEnvironment
  │   ├── PowderedSnowFogEnvironment
  │   ├── BlindnessFogEnvironment
  │   └── DarknessFogEnvironment
  ├── FogMode: NONE / TERRAIN / SKY
  └── GPU Buffer: getBuffer(FogMode)
```

### 8.2 setupFog (行 162–173)

```java
public FogData setupFog(Camera camera, int renderDistanceInChunks,
    DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level) {
    FogData fog = new FogData();
    FogType fogType = camera.getFluidInCamera();
    // 根据 fogType 选择对应的 FogEnvironment
    FogEnvironment fogEnvironment = switch(fogType) {
        case WATER -> waterFogEnvironment;
        case LAVA -> lavaFogEnvironment;
        case POWDER_SNOW -> powderedSnowFogEnvironment;
        default -> atmosphericFogEnvironment;  // 包括盲/黑效果
    };
    fogEnvironment.setupFog(fog, camera, level, renderDistanceInBlocks, deltaTracker);
    return fog;
}
```

### 8.3 FogData record

```java
public record FogData(float red, float green, float blue,
    float start, float end, FogShape shape) {
    public static final FogData NO_FOG = new FogData(0,0,0, Float.MAX_VALUE, Float.MAX_VALUE, FogShape.SPHERE);
}
```

### 8.4 与老版本对比

| 方面 | 1.20.1/1.21.1 | 26.1.2 |
|------|--------------|--------|
| Fog 类 | 静态 `FogRenderer` | 实例 `FogRenderer` + `FogEnvironment` |
| Fog 存储 | 全局静态变量 (fogRed/Green/Blue) | GpuBuffer(UBO) + `FogData` record |
| Fog 设置 | `RenderSystem.setShaderFogStart/End/Color/Shape` | `RenderSystem.setShaderFog(gpuBufferSlice)` |
| Fog 模式 | FOG_SKY / FOG_TERRAIN | NONE / TERRAIN / SKY |
| 扩展 | Forge hook | FogEnvironment 继承 |

---

## 9. DimensionType.Skybox 替代 DimensionSpecialEffects.SkyType

### 9.1 DimensionType.Skybox 枚举

```java
// 在 DimensionType 中定义
public static enum Skybox {
    NONE,    // 无天空(Nether)
    NORMAL,  // 正常天空
    END;     // 末地天空
}
```

- 旧 `DimensionSpecialEffects.SkyType` 被移除
- `DimensionSpecialEffects` 整个类被移除
- 维度云/雾行为不再通过 DimensionSpecialEffects 控制,改为:
  - **云**: 维度直接提供 `cloudHeight`(NaN=无云)
  - **雾**: `FogEnvironment` 子类
  - **天空类型**: `DimensionType.skybox()`

### 9.2 LevelRenderState 中的云状态

```java
// LevelRenderState.java
public int cloudColor;
public float cloudHeight;
```

这些值在 `extractRenderState` 时从 level 中提取,不再通过 DimensionSpecialEffects。

---

## 10. EnvironmentAttributes 体系

**包**: `net.minecraft.world.attribute.EnvironmentAttributes`

26.1.2 引入 **EnvironmentAttribute** 系统,将天空渲染参数数据驱动化:

| Attribute | 类型 | 含义 |
|-----------|------|------|
| `SUN_ANGLE` | float (度) | 太阳旋转角度 |
| `MOON_ANGLE` | float (度) | 月亮旋转角度 |
| `STAR_ANGLE` | float (度) | 星空旋转角度 |
| `STAR_BRIGHTNESS` | float | 星星亮度 [0,1] |
| `SKY_COLOR` | int (ARGB) | 天空颜色 |
| `SUNRISE_SUNSET_COLOR` | int (ARGB) | 日出日落颜色 |
| `MOON_PHASE` | MoonPhase | 当前月相 |

通过 `Camera.attributeProbe().getValue(attribute, partialTicks)` 获取。

---

## 11. CloudStatus 变更

**文件**: `net/minecraft/client/CloudStatus.java` (32 行)

```java
public enum CloudStatus implements StringRepresentable {
    OFF("false", "options.off"),
    FAST("fast", "options.clouds.fast"),
    FANCY("true", "options.clouds.fancy");

    public static final Codec<CloudStatus> CODEC = StringRepresentable.fromEnum(CloudStatus::values);
    private final String legacyName;
    private final Component caption;  // ← 新增 Component

    public Component caption() { return this.caption; }
}
```

- **移除** `OptionEnum` 接口(不再需要 `getId()`)
- **新增** `Component caption`: 直接持有多语言文本组件
- 保留 `StringRepresentable` 和 `CODEC`

---

## 总结

### 架构变更要点

| 维度 | 1.20.1/1.21.1 | 26.1.2 |
|------|--------------|--------|
| 渲染器 | LevelRenderer 单体 | SkyRenderer / CloudRenderer / WeatherEffectRenderer 独立类 |
| 状态管理 | 内联字段 + 方法调用 | RenderState 对象(SkyRenderState/WeatherRenderState/LevelRenderState) |
| 渲染 API | RenderSystem + VertexBuffer | CommandEncoder + RenderPass + RenderPipeline |
| 缓冲 | VertexBuffer(旧 API) | GpuBuffer(新 GPU API) |
| 调度 | 顺序方法调用 | FrameGraphBuilder + FramePass |
| Fog | 静态方法 + 全局变量 | 实例 + FogEnvironment 多态 |
| 天体纹理 | 独立 PNG 文件 | TextureAtlas(CELESTIALS) |
| 维度特效 | DimensionSpecialEffects 抽象类 | DimensionType.Skybox + EnvironmentAttributes |
| 云数据 | 即时 BufferBuilder | 预处理 cells + texel buffer (GPU 端顶点拉取) |
| 扩展点 | IForgeDimensionSpecialEffects render* 回调 | LevelRenderState custom*Renderer 字段 |

### 关键数值常量(与老版本相同)

| 常量 | 值 |
|------|-----|
| 云 cell 大小 | 12.0 × 4.0 × 12.0 |
| 云移动速度 | 0.03 cell/tick |
| Sky disc 半径 | 512.0 |
| 太阳大小 | 30.0 |
| 月亮大小 | 20.0 |
| 星星数量 | 1500 |
| 星星种子 | 10842 |
| Overworld 云高度 | 192.0 |
