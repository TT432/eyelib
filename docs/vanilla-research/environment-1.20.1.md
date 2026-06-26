# 1.20.1 (Forge) Minecraft Vanilla Environment Rendering 分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [渲染调度顺序](#1-渲染调度顺序)
2. [renderSky 全分析](#2-rendersky-全分析)
3. [renderClouds 全分析](#3-renderclouds-全分析)
4. [renderSnowAndRain + tickRain](#4-rendersnowandrain--tickrain)
5. [天空几何体构建](#5-天空几何体构建)
6. [DimensionSpecialEffects](#6-dimensionspecialeffects)
7. [FogRenderer 在环境渲染中的角色](#7-fogrenderer-在环境渲染中的角色)
8. [CloudStatus 云状态枚举](#8-cloudstatus-云状态枚举)
9. [纹理资源清单](#9-纹理资源清单)

---

## 1. 渲染调度顺序

**文件**: `LevelRenderer.java`

### 1.1 `renderLevel()` 调度流 (行 1160–1439)

```
1. FogRenderer.setupColor(...)       → 计算雾颜色
2. Clear + FogRenderer.levelFogColor()
3. FogRenderer.setupFog(FOG_SKY)    → 天空雾
4. renderSky(...)                   → 天空渲染
5. FogRenderer.setupFog(FOG_TERRAIN)→ 地形雾
6. Chunk layers (solid/cutoutMipped/cutout)
7. Lighting setup (Nether或普通)
8. Entities + BlockEntities
9. Translucent chunks
10. Particles
11. Clouds (单独 target 或直接渲染)
12. Weather (snow/rain)
13. WorldBorder
14. Debug
```

### 1.2 云和天气渲染调度 (行 1397–1430)

```java
// 云渲染: 如果 transparencyChain 存在则渲染到独立 targets
if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
    if (this.transparencyChain != null) {
        this.cloudsTarget.clear(Minecraft.ON_OSX);
        RenderStateShard.CLOUDS_TARGET.setupRenderState();
        this.renderClouds(...);
        RenderStateShard.CLOUDS_TARGET.clearRenderState();
    } else {
        this.renderClouds(...);  // 直接渲染到主 target
    }
}

// 天气渲染: 类似的两路分支
if (this.transparencyChain != null) {
    RenderStateShard.WEATHER_TARGET.setupRenderState();
    this.renderSnowAndRain(...);
    this.renderWorldBorder(camera);
    RenderStateShard.WEATHER_TARGET.clearRenderState();
    this.transparencyChain.process(partialTick);
} else {
    RenderSystem.depthMask(false);
    this.renderSnowAndRain(...);
    this.renderWorldBorder(camera);
    RenderSystem.depthMask(true);
}
```

---

## 2. renderSky 全分析

**文件**: `LevelRenderer.java`, 行 1789–1901

### 2.1 方法签名与前置检查

```java
public void renderSky(PoseStack poseStack, Matrix4f projectionMatrix,
    float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup)
```

- `isFoggy`: 由 `DimensionSpecialEffects.isFoggyAt(camX, camY)` 决定
- `skyFogSetup`: 重新设置天空雾的 Runnable

### 2.2 早期退出条件 (行 1790–1793)

1. `level.effects().renderSky(...)` 返回 true → DimensionSpecialEffects 拦截(如 Nether 的天空类型 NONE 默认由插件处理)
2. `skyFogSetup.run()` 后检查:
   - `isFoggy` = true → 跳过
   - `FluidInCamera` = POWDER_SNOW / LAVA → 跳过
   - `doesMobEffectBlockSky()` (Blindness/Darkness) → 跳过

### 2.3 天空类型分发 (行 1796–1798)

```java
if (effects().skyType() == SkyType.END) {
    this.renderEndSky(poseStack);           // End 维度
} else if (effects().skyType() == SkyType.NORMAL) {
    // 主世界天空渲染
}
// SkyType.NONE (Nether) → 不渲染天空
```

### 2.4 NORMAL 天空渲染流程 (行 1799–1901)

#### 步骤 1: Sky Disc (行 1799–1810)

```java
Vec3 vec3 = this.level.getSkyColor(cameraPosition, partialTick);
float f = (float)vec3.x;  // R
float f1 = (float)vec3.y; // G
float f2 = (float)vec3.z; // B
FogRenderer.levelFogColor();
RenderSystem.depthMask(false);
RenderSystem.setShaderColor(f, f1, f2, 1.0F);
this.skyBuffer.bind();
this.skyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
VertexBuffer.unbind();
```

- 天空颜色从 `ClientLevel.getSkyColor()` 获取,受时间/天气影响
- Sky buffer 是预构建的 TRIANGLE_FAN(半径 512),y=+16
- 通过 `setShaderColor` 调制颜色,着色器输出 = vertexPosition(无纹理)

#### 步骤 2: Sunrise/Sunset (行 1812–1838)

```java
float[] afloat = this.level.effects().getSunriseColor(timeOfDay, partialTick);
if (afloat != null) {
    // TRIANGLE_FAN: 中心(0,100,0) → 16 等分圆,半径 120
    // 颜色: 中心=afloat[0..3](带alpha), 外圈=afloat[0..2]+alpha=0
    // 旋转: 太阳位于地平线以下时旋转 180°
}
```

- `getSunriseColor()` 仅在 cos(timeOfDay*2π) ∈ [-0.4, 0.4] 时返回非 null(即日出日落时段)
- 使用 `POSITION_COLOR` 顶点格式, `GameRenderer::getPositionColorShader`

#### 步骤 3: 混合模式设置 (行 1840)

```java
RenderSystem.blendFuncSeparate(
    SRC_ALPHA, ONE,    // RGB: additive blending
    ONE, ZERO           // Alpha: source only
);
```

Sun/Moon/Stars 阶段使用加法混合,使天体叠加在天空色之上。

#### 步骤 4: 太阳渲染 (行 1841–1855)

```java
poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
poseStack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0F));
// 太阳: 30×30 quad, y=100, 纹理 SUN_LOCATION
// 颜色调制: alpha = 1 - rainLevel (雨天太阳变透明)
```

- 太阳是一个 30×30 的正方形 quad,在距离 100 处
- 雨天透明度 = `1.0F - rainLevel`

#### 步骤 5: 月亮渲染 (行 1856–1870)

```java
// 月亮: 20×20 quad, y=-100
// 纹理: MOON_LOCATION, 根据月相 UV 偏移
int k = this.level.getMoonPhase();
int l = k % 4;       // 列 (0-3)
int i1 = k / 4 % 2;  // 行 (0-1)
// UV = [l/4, i1/2] → [(l+1)/4, (i1+1)/2]
```

- 月亮纹理为 4×2 的月相图集
- 逆时针旋转(与太阳差 180°)

#### 步骤 6: 星空渲染 (行 1871–1879)

```java
float f10 = this.level.getStarBrightness(partialTick) * f11;
if (f10 > 0.0F) {
    RenderSystem.setShaderColor(f10, f10, f10, f10);
    FogRenderer.setupNoFog();       // 星空无雾
    this.starBuffer.bind();
    this.starBuffer.drawWithShader(pose, projection, GameRenderer.getPositionShader());
    VertexBuffer.unbind();
    skyFogSetup.run();              // 恢复天空雾
}
```

- `starBrightness` 受时间 + 雨天影响
- 星空使用 `setShaderColor` 调制整体亮度
- 渲染期间关闭雾

#### 步骤 7: 重置与 Dark Disc (行 1881–1896)

```java
RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
RenderSystem.disableBlend();
RenderSystem.defaultBlendFunc();
// Dark disc: 当玩家眼睛位置低于地平线时渲染
double d0 = playerEyeY - level.getLevelData().getHorizonHeight(level);
if (d0 < 0.0D) {
    poseStack.translate(0.0F, 12.0F, 0.0F);
    this.darkBuffer.bind();
    this.darkBuffer.drawWithShader(pose, projection, shader);
}
RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
RenderSystem.depthMask(true);
```

- Dark disc 颜色 = `setShaderColor(0,0,0,1)`,即在低处时用黑色 disc 覆盖天空下方
- 向上偏移 12 单位以避免 z-fighting

### 2.5 renderEndSky (行 1745–1787)

```java
private void renderEndSky(PoseStack poseStack) {
    RenderSystem.enableBlend();
    RenderSystem.depthMask(false);
    RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
    for(int i = 0; i < 6; ++i) {
        // 6 个面: 默认, X+90°, X-90°, X+180°, Z+90°, Z-90°
        // 每面: 100×100 quad, 颜色(40,40,40,255)
        bufferbuilder.begin(QUADS, POSITION_TEX_COLOR);
        bufferbuilder.vertex(-100, -100, -100).uv(0,0).color(40,40,40,255).endVertex();
        bufferbuilder.vertex(-100, -100,  100).uv(0,16).color(40,40,40,255).endVertex();
        bufferbuilder.vertex( 100, -100,  100).uv(16,16).color(40,40,40,255).endVertex();
        bufferbuilder.vertex( 100, -100, -100).uv(16,0).color(40,40,40,255).endVertex();
        tesselator.end();
    }
    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
}
```

- 立方体包围盒的 6 个内面
- `END_SKY_LOCATION` = `textures/environment/end_sky.png`
- 暗灰色 (40,40,40) 为基底,纹理叠加

---

## 3. renderClouds 全分析

**文件**: `LevelRenderer.java`, 行 1912–1990

### 3.1 方法签名与前置检查

```java
public void renderClouds(PoseStack poseStack, Matrix4f projectionMatrix,
    float partialTick, double camX, double camY, double camZ)
```

- 先调用 `level.effects().renderClouds(...)` 检查 DimensionSpecialEffects 拦截
- `effects().getCloudHeight()` 返回 NaN → 跳过(如 Nether/End)

### 3.2 渲染状态

```java
RenderSystem.disableCull();
RenderSystem.enableBlend();
RenderSystem.enableDepthTest();
RenderSystem.blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ONE_MINUS_SRC_ALPHA);
RenderSystem.depthMask(true);
```

- 标准半透明混合,无面剔除,深度写入

### 3.3 云位置计算 (行 1922–1933)

```java
float cellSize = 12.0F;    // 每个云 cell 12 方块
float layerHeight = 4.0F;  // 云层厚度 4 方块

// 云移动速度
double d1 = (ticks + partialTick) * 0.03;
double d2 = (camX + d1) / 12.0;   // XZ 平面"原点"随时间平移
double d3 = (cloudHeight - camY + 0.33);  // 高度(相对相机)
double d4 = camZ / 12.0 + 0.33;

// 周期性 Wrap (2048 cells 后重复)
d2 -= floor(d2 / 2048.0) * 2048;
d4 -= floor(d4 / 2048.0) * 2048;

// 小数部分用于 sub-cell 平移
float f3 = (float)(d2 - floor(d2));       // X 偏移
float f4 = (float)(d3/4.0 - floor(d3/4.0)) * 4.0F; // Y 偏移
float f5 = (float)(d4 - floor(d4));       // Z 偏移
```

- 移动速度: 每秒 0.03 cell (即 0.36 方块/s)
- 2048 cells 后 wrap,防止浮点精度问题

### 3.4 云几何体重建触发 (行 1938–1945)

```java
if (i != prevCloudX || j != prevCloudY || k != prevCloudZ
    || options.getCloudsType() != prevCloudsType
    || prevCloudColor.distanceToSqr(vec3) > 2.0E-4) {
    // 触发重建
    this.generateClouds = true;
}
```

触发条件:相机位置跨 cell 边界、云设置变更(FAST↔FANCY)、颜色变化超过阈值。

### 3.5 着色器与纹理 (行 1961–1962)

```java
RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
```

- `CLOUDS_LOCATION` = `textures/environment/clouds.png`
- `POSITION_TEX_COLOR_NORMAL` 顶点格式
- 着色器包含雾计算(通过 `FogRenderer.levelFogColor()`)

### 3.6 Pose 变换 (行 1964–1966)

```java
poseStack.scale(12.0F, 1.0F, 12.0F);   // 将 cell 网格映射到世界空间
poseStack.translate(-f3, f4, -f5);      // sub-cell 偏移
```

### 3.7 双 Pass 渲染 (行 1969–1983)

```java
int l = (prevCloudsType == CloudStatus.FANCY) ? 0 : 1;
for(int i1 = l; i1 < 2; ++i1) {
    if (i1 == 0) {
        RenderSystem.colorMask(false, false, false, false);  // 仅写深度
    } else {
        RenderSystem.colorMask(true, true, true, true);      // 写颜色
    }
    this.cloudBuffer.drawWithShader(pose, projection, shader);
}
```

- **FANCY 模式**(`i1=0`): 双 pass。Pass 1 写深度(不写颜色)确保正确的深度排序,Pass 2 渲染颜色
- **FAST 模式**(`i1=1`): 单 pass,直接写颜色(已预烘焙为单层平面)

### 3.8 buildClouds 几何体构建 (行 1992–2086)

#### FANCY 模式 (3D 盒体)
```
遍历 8×8 cell 网格:
  每个 cell = 8×4×8 方块盒体
  底部面 (y=0):  颜色 * 0.7 (暗)
  顶部面 (y=4):  颜色 * 1.0 (亮)
  侧面 (X 向):   颜色 * 0.9
  侧面 (Z 向):   颜色 * 0.8
```

- 材质坐标: `f3 = floor(cellX) * 0.00390625` (= 1/256)
- 颜色从 `level.getCloudColor(partialTick)` 获取
- 色彩分层(底部暗/顶部亮/侧面中等)模拟体积光照

#### FAST 模式 (单层平面)
```
4 个 32×32 的大 plane, 共 16 个 quad
全部朝下(normal = 0,-1,0), 单色
```

### 3.9 云颜色

```java
Vec3 vec3 = this.level.getCloudColor(partialTick);
```

通过 `ClientLevel.getCloudColor()` 获取,受时间/天气影响。不同朝向面使用不同强度(0.7/0.8/0.9/1.0)模拟光照。

---

## 4. renderSnowAndRain + tickRain

**文件**: `LevelRenderer.java`, 行 273–441

### 4.1 renderSnowAndRain (行 273–392)

```java
private void renderSnowAndRain(LightTexture lightTexture, float partialTick,
    double camX, double camY, double camZ)
```

#### 前置检查
- `level.effects().renderSnowAndRain(...)` 拦截
- `rainLevel <= 0.0F` → 跳过

#### 渲染状态
```java
lightTexture.turnOnLightLayer();  // 激活 lightmap
RenderSystem.disableCull();
RenderSystem.enableBlend();
RenderSystem.enableDepthTest();
int radius = Minecraft.useFancyGraphics() ? 10 : 5;
RenderSystem.depthMask(Minecraft.useShaderTransparency());
RenderSystem.setShader(GameRenderer::getParticleShader);
```

#### 遍历相机周围区域 (行 299–382)
```
for z: camZ-radius .. camZ+radius
  for x: camX-radius .. camX+radius
    1. 取 biome, 检查 hasPrecipitation()
    2. 取 MOTION_BLOCKING 高度
    3. 裁剪到相机垂直范围 [camY-radius, camY+radius]
    4. 根据 biome 降水类型生成粒子:
```

#### 雨粒子 (行 327–349)
```java
// 纹理: RAIN_LOCATION = "textures/environment/rain.png"
// 每列多个垂直 quad, 通过 UV.y 滚动实现下落动画
int i3 = ticks + x*3121 + x*45238971 + z*418711 + z*13761 & 31;
float f2 = -((float)i3 + partialTick) / 32.0F * (3.0F + random.nextFloat());
// UV: (0, y0*0.25+f2) → (1, y1*0.25+f2)
// 使用 lightmap: getLightColor(level, pos) → uv2(j3)
// alpha = ((1-dist²)*0.5+0.5) * rainLevel
```

- 使用 32×32 的 `rainSizeX[]`/`rainSizeZ[]` 预计算表给每列雨倾斜方向
- `VertexFormat`: PARTICLE (POSITION+UV0+COLOR+UV2)
- 每列 2 个 quad = 4 个顶点

#### 雪粒子 (行 350–377)
```java
// 纹理: SNOW_LOCATION = "textures/environment/snow.png"
// 水平漂移 + 垂直下落
float f5 = -((float)(ticks & 511) + partialTick) / 512.0F;  // 垂直速度
float f6 = random.nextDouble() + ticks*0.01*random.nextGaussian(); // X 漂移
float f7 = random.nextDouble() + ticks*0.001*random.nextGaussian(); // Z 漂移
// UV: (0+f6, y0*0.25+f5+f7) → (1+f6, y1*0.25+f5+f7)
// lightmap: 手动分拆 packed light 为两个 short
int k3 = getLightColor(level, pos);
int l3 = k3 >> 16 & 0xFFFF;  // sky light
int i4 = k3 & 0xFFFF;         // block light
int j4 = (l3*3 + 240) / 4;   // sky light 亮度混合
int k4 = (i4*3 + 240) / 4;   // block light 亮度混合
// uv2(k4, j4) — 注意: 雪用短整数直接传入而非 packed int
```

- 雪粒子更大(alpha 因子: `(1-dist²)*0.3+0.5`)
- 雪有随机高斯偏移模拟飘动

### 4.2 tickRain — 雨音与地面粒子 (行 394–441)

```java
public void tickRain(Camera camera)
```

- 在相机周围 20×20 区域内随机生成 `RAIN` / `SMOKE` 粒子
- 粒子数量: `100 * rainLevel²` (粒子设为 DECREASED 时减半)
- 岩浆块/营火上方生成 `SMOKE` 粒子
- 雨声: `SOUND_EVENTS.WEATHER_RAIN` 或 `WEATHER_RAIN_ABOVE`

---

## 5. 天空几何体构建

### 5.1 createStars (行 600–657)

```java
private void createStars()
```

- **星数**: 1500 颗
- **分布**: 均匀分布的单位球面 (`random.nextFloat()*2-1`),拒绝内球半径 <0.01 或 >1.0 的点
- **星大小**: `0.15 + random.nextFloat() * 0.1`
- **朝向**: 每颗星计算球面朝向,构建 camera-facing quad
- **顶点格式**: `POSITION`, `VertexFormat.Mode.QUADS`
- **种子**: 固定 10842L(线程安全 RandomSource)

### 5.2 createLightSky / createDarkSky (行 558–598)

```java
private void createLightSky()  // skyBuffer, y=+16
private void createDarkSky()   // darkBuffer, y=-16
```

- 两者共用的 `buildSkyDisc()`:
```java
float f = Math.signum(y) * 512.0F;  // 半径
builder.begin(TRIANGLE_FAN, POSITION);
builder.vertex(0, y, 0).endVertex();          // 圆心
for(int i = -180; i <= 180; i += 45) {        // 9 个顶点(含起点 = 10 vertices)
    builder.vertex(f*cos(i°), y, 512*sin(i°)).endVertex();
}
```

- **Light Sky**: y=+16, 颜色通过 `setShaderColor(skyColor)` 调制
- **Dark Sky**: y=-16, 颜色通过 `setShaderColor(0,0,0,1)` 调制

---

## 6. DimensionSpecialEffects

**文件**: `net/minecraft/client/renderer/DimensionSpecialEffects.java` (144 行)

### 6.1 基类结构

```java
public abstract class DimensionSpecialEffects {
    private final float cloudLevel;          // 云高度 (NaN=无云)
    private final boolean hasGround;         // 是否有地面
    private final SkyType skyType;           // NONE/NORMAL/END
    private final boolean forceBrightLightmap;
    private final boolean constantAmbientLight;
}
```

### 6.2 三个内置维度

| 维度 | cloudLevel | hasGround | skyType | forceBrightLightmap | constantAmbientLight |
|------|-----------|-----------|---------|---------------------|---------------------|
| Overworld | 192.0 | true | NORMAL | false | false |
| Nether | NaN | true | NONE | false | true |
| End | NaN | false | END | true | false |

### 6.3 SkyType 枚举

```java
public static enum SkyType {
    NONE,    // Nether: 无天空渲染
    NORMAL,  // Overworld: 完整天空(太阳/月亮/星星/sky disc)
    END;     // End: 末地天空(6面天空盒)
}
```

### 6.4 getSunriseColor (行 43–60)

```java
public float[] getSunriseColor(float timeOfDay, float partialTicks) {
    float cosTime = cos(timeOfDay * 2π) - 0.0F;
    if (cosTime >= -0.4F && cosTime <= 0.4F) {
        float t = (cosTime + 0.4F) / 0.4F * 0.5F + 0.5F;
        float alpha = 1.0F - (1.0F - Mth.sin(t * π)) * 0.99F;
        alpha *= alpha;
        sunriseCol[0] = t * 0.3F + 0.7F;     // R: 0.7→1.0
        sunriseCol[1] = t * t * 0.7F + 0.2F;  // G: 0.2→0.9
        sunriseCol[2] = t * t * 0.0F + 0.2F;  // B: 0.2
        sunriseCol[3] = alpha;
        return sunriseCol;
    }
    return null;
}
```

- 仅在 `cos(timeOfDay*2π) ∈ [-0.4, 0.4]` 时返回非 null
- 返回 RGBA 数组,alpha 控制 sunrise fan 的透明度渐变
- End 维度 override 返回 `null`(无日出效果)

### 6.5 Fog 颜色方法

```java
// Overworld
public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
    return fogColor.multiply(
        brightness*0.94+0.06,   // R: 最小 6% 的天空色
        brightness*0.94+0.06,   // G: 最小 6%
        brightness*0.91+0.09    // B: 最小 9%
    );
}
public boolean isFoggyAt(int x, int y) { return false; }

// Nether
public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
    return fogColor;  // 不变
}
public boolean isFoggyAt(int x, int y) { return true; }

// End
public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
    return fogColor.scale(0.15);  // 极暗雾
}
public boolean isFoggyAt(int x, int y) { return false; }
```

### 6.6 IForgeDimensionSpecialEffects 扩展接口

Forge 扩展了 `DimensionSpecialEffects` 允许模组注册自定义维度特效。关键回调:
- `renderSky(...)` — 完全接管天空渲染
- `renderClouds(...)` — 接管云渲染
- `renderSnowAndRain(...)` — 接管天气渲染
- `tickRain(...)` — 接管雨天逻辑
- `adjustLightmapColors(...)` — 调整 lightmap 颜色

默认实现:
```java
// 所有 render* 方法默认返回 false (让原版处理)
// Nether: skyType=NONE → 天空渲染直接 return
```

---

## 7. FogRenderer 在环境渲染中的角色

**文件**: `FogRenderer.java` (366 行)

### 7.1 FogMode 枚举

```java
public static enum FogMode {
    FOG_SKY,      // 天空雾: start=0, end=renderDistance, shape=CYLINDER
    FOG_TERRAIN;  // 地形雾: start=distance-lerp, end=distance, shape=CYLINDER
}
```

### 7.2 setupFog (行 217–281)

根据 `FluidInCamera` 和 FogMode 计算雾起止距离:
- **LAVA**: start=0.25, end=1.0(非旁观者)
- **POWDER_SNOW**: start=0, end=2.0
- **WATER**: start=-8, end=96(受 WaterVision 影响)
- **Blindness/Darkness**: 自定义雾距
- **FOG_SKY(默认)**: start=0, end=renderDistance, CYLINDER
- **FOG_TERRAIN**: start=renderDistance-lerp, end=renderDistance, CYLINDER

### 7.3 setupColor (行 42–200)

计算雾颜色流程:
```
1. 根据流体确定基础色
2. skyColor 取样的天空色
3. CubicSampler 8点取样 biome fog color → DimensionSpecialEffects 调制
4. 日出日落颜色混合
5. 天空色 25%-75% 融合(depends on render distance)
6. 雨天/雷暴衰减
7. void 暗化(低于最低建造高度)
8. 夜视增亮
```

### 7.4 levelFogColor / setupNoFog

```java
public static void levelFogColor() {
    RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue);
}
public static void setupNoFog() {
    RenderSystem.setShaderFogStart(Float.MAX_VALUE);
}
```

- `levelFogColor()` 在天空 disc、云渲染前调用
- `setupNoFog()` 在星空渲染和最终清理时调用

---

## 8. CloudStatus 云状态枚举

**文件**: `net/minecraft/client/CloudStatus.java` (28 行)

```java
public enum CloudStatus implements OptionEnum {
    OFF(0, "options.off"),
    FAST(1, "options.clouds.fast"),
    FANCY(2, "options.clouds.fancy");
}
```

- 存储在 `Options.getCloudsType()`
- **OFF**: 完全不渲染云
- **FAST**: 单层平面(32×32 quad),单 pass
- **FANCY**: 3D 盒体(8×8×8 cells),双 pass(深度预写 + 颜色)

---

## 9. 纹理资源清单

| 常量 | 路径 | 用途 |
|------|------|------|
| `SUN_LOCATION` | `textures/environment/sun.png` | 太阳纹理 |
| `MOON_LOCATION` | `textures/environment/moon_phases.png` | 月相纹理(4×2 图集) |
| `CLOUDS_LOCATION` | `textures/environment/clouds.png` | 云纹理 |
| `RAIN_LOCATION` | `textures/environment/rain.png` | 雨纹理 |
| `SNOW_LOCATION` | `textures/environment/snow.png` | 雪纹理 |
| `END_SKY_LOCATION` | `textures/environment/end_sky.png` | 末地天空纹理 |

---

## 总结

### 关键常量速查

| 常量 | 值 | 含义 |
|------|-----|------|
| Overworld 云高度 | 192.0 | 云层 Y 坐标 |
| Sky disc 半径 | 512.0 | 天空穹顶半径 |
| 太阳大小 | 30.0 | 太阳 quad 半边长 |
| 月亮大小 | 20.0 | 月亮 quad 半边长 |
| 星星数量 | 1500 | 预生成星数 |
| 云 cell 大小 | 12.0×4.0×12.0 | 单个云 cell 尺寸 |
| 云网格 | 8×8 cells | FANCY 模式渲染网格 |
| 雨半径 | 5 (fast) / 10 (fancy) | 相机周围渲染列数 |
| 云移动速度 | 0.03 cell/tick | ~0.36 方块/秒 |
| 2048 | 云 wrap 周期 | 约 24576 方块后重复 |

### 架构特点

- **单片架构**: 所有环境渲染逻辑集中在 `LevelRenderer`
- **VertexBuffer 预构建**: sky disc/star 在构造时生成,渲染时仅 bind+draw
- **云动态重建**: 仅在相机跨 cell 边界或设置变更时重建 VBO
- **Forge 扩展点**: DimensionSpecialEffects 通过 `IForgeDimensionSpecialEffects` 提供 renderSky/renderClouds/renderSnowAndRain 回调
- **双 target 路径**: transparencyChain 将云/天气渲染到独立 framebuffer,最后合成
