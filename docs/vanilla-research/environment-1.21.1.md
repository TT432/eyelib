# 1.21.1 (NeoForge) Minecraft Vanilla Environment Rendering 分析报告

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [相对于 1.20.1 的关键变更](#1-相对于-1201-的关键变更)
2. [renderSky 全分析](#2-rendersky-全分析)
3. [renderClouds 全分析](#3-renderclouds-全分析)
4. [renderSnowAndRain](#4-rendersnowandrain)
5. [CloudStatus 变更](#5-cloudstatus-变更)
6. [DimensionSpecialEffects](#6-dimensionspecialeffects)
7. [FogRenderer 在环境渲染中的角色](#7-fogrenderer-在环境渲染中的角色)

---

## 1. 相对于 1.20.1 的关键变更

| 方面 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| PoseStack 传递 | `PoseStack poseStack` 由外部传入 | `PoseStack poseStack = new PoseStack()` 内部构建,接受 `Matrix4f frustumMatrix` |
| 顶点 API | `bufferbuilder.vertex(x,y,z).uv(u,v).color(r,g,b,a).endVertex()` | `bufferbuilder.addVertex(x,y,z).setUv(u,v).setColor(r,g,b,a)` |
| BufferBuilder begin | `bufferbuilder.begin(mode, format)` 返回 void | `Tesselator.begin(mode, format)` 返回 BufferBuilder |
| MeshData | `BufferBuilder.RenderedBuffer` (end+upload) | `MeshData` (buildOrThrow+upload) |
| 云着色器 | 手动 `RenderSystem.setShader()` + 双 pass colorMask | `RenderType.cloudsDepthOnly()` + `RenderType.clouds()` |
| Shader 类 | `ShaderInstance` | 未确认(Shader Program API 内部可能不变) |
| 光照 | `Lighting.setupLevel(pose)` 需要 PoseStack | `Lighting.setupLevel()` 静态方法(无参) |
| CloudStatus | `implements OptionEnum` | `implements OptionEnum, StringRepresentable` |
| NeoForge events | `ForgeHooksClient.dispatchRenderStage` | `ClientHooks.dispatchRenderStage` |
| Rendering schedule | 行 1397–1430 | 行 1213–1246 (结构基本不变) |

---

## 2. renderSky 全分析

**文件**: `LevelRenderer.java`, 行 1598–1715

### 2.1 方法签名变化

```java
// 1.20.1
public void renderSky(PoseStack poseStack, Matrix4f projectionMatrix, ...)

// 1.21.1
public void renderSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, ...)
```

- 1.21.1 内部创建 PoseStack:`PoseStack poseStack = new PoseStack(); poseStack.mulPose(frustumMatrix);`
- 这意味着 frustum matrix 在内部被应用而非依赖外部 PoseStack

### 2.2 渲染流程(不变的核心逻辑)

与 1.20.1 完全相同的步骤:
1. Sky disc → `skyBuffer.drawWithShader()` 调制 skyColor
2. Sunrise/sunset fan → TRIANGLE_FAN, 16 steps
3. Blend mode: `SRC_ALPHA/ONE, ONE/ZERO`
4. 太阳 quad → 30×30, `SUN_LOCATION`
5. 月亮 quad → 20×20, `MOON_LOCATION`, 月相 UV
6. 星空 → `starBuffer.drawWithShader()`
7. Dark disc → `darkBuffer`

### 2.3 顶点构建 API 变化

```java
// 1.20.1
bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
bufferbuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(f4, f5, f6, afloat[3]).endVertex();

// 1.21.1
BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
bufferbuilder.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(f4, f5, f6, afloat[3]);
```

### 2.4 Tesselator 返回值变化

```java
// 1.20.1
bufferbuilder.end();            // → BufferBuilder.RenderedBuffer
BufferUploader.drawWithShader(bufferbuilder.end());

// 1.21.1
bufferbuilder.buildOrThrow();   // → MeshData
BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
```

### 2.5 renderEndSky 不变

与 1.20.1 完全相同的 6 面天空盒逻辑(行 1555–1596)。

---

## 3. renderClouds 全分析

**文件**: `LevelRenderer.java`, 行 1723–1791

### 3.1 方法签名变化

```java
// 1.20.1
public void renderClouds(PoseStack poseStack, Matrix4f projectionMatrix, ...)

// 1.21.1
public void renderClouds(PoseStack poseStack, Matrix4f frustumMatrix,
    Matrix4f projectionMatrix, ...)
```

新增 `frustumMatrix` 参数,在内部应用:
```java
poseStack.mulPose(frustumMatrix);
```

### 3.2 云渲染使用 RenderType

这是 **1.21.1 最大的云渲染变更**:

```java
// 1.20.1: 手动 colorMask + setShader
for(int i1 = l; i1 < 2; ++i1) {
    if (i1 == 0) {
        RenderSystem.colorMask(false, false, false, false);  // 深度 pass
    } else {
        RenderSystem.colorMask(true, true, true, true);      // 颜色 pass
    }
    ShaderInstance shaderinstance = RenderSystem.getShader();
    this.cloudBuffer.drawWithShader(pose, projection, shaderinstance);
}

// 1.21.1: 使用 RenderType 封装
for (int i1 = l; i1 < 2; i1++) {
    RenderType rendertype = i1 == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
    rendertype.setupRenderState();
    ShaderInstance shaderinstance = RenderSystem.getShader();
    this.cloudBuffer.drawWithShader(pose, projection, shaderinstance);
    rendertype.clearRenderState();
}
```

### 3.3 MeshData 替代 RenderedBuffer

```java
// 1.20.1
BufferBuilder.RenderedBuffer renderedBuffer = this.buildClouds(builder, ...);
this.cloudBuffer.upload(renderedBuffer);

// 1.21.1
this.cloudBuffer.upload(this.buildClouds(Tesselator.getInstance(), ...));
// buildClouds 返回 MeshData
```

### 3.4 buildClouds 顶点 API 变化

```java
// 1.20.1
builder.vertex(f18+0, f17+0, f19+8).uv(...).color(...).normal(...).endVertex();

// 1.21.1
bufferbuilder.addVertex(f18+0, f17+0, f19+8)
    .setUv(...).setColor(...).setNormal(...);
```

### 3.5 云状态管理不变

- 云位置和移动速度: 与 1.20.1 完全相同
- 重建触发条件: 与 1.20.1 完全相同
- FANCY (3D 盒体) vs FAST (单层平面): 几何体构建逻辑不变

---

## 4. renderSnowAndRain

**文件**: `LevelRenderer.java`, 行 266–428

### 4.1 关键变更

```java
// 1.20.1: 每个 biome-precipitation 块内手动管理 BufferBuilder
bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
// ... 逐顶点构建 ...

// 1.21.1: Tesselator.begin 返回 BufferBuilder, draw 用 buildOrThrow
bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
// ... 逐顶点构建 ...
BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
```

### 4.2 雨动画种子变更

```java
// 1.20.1
int i3 = this.ticks + k1*k1*3121 + k1*45238971 + j1*j1*418711 + j1*13761 & 31;

// 1.21.1
int i3 = this.ticks & 131071;
int j3 = k1*k1*3121 + k1*45238971 + j1*j1*418711 + j1*13761 & 0xFF;
float f2 = 3.0F + randomsource.nextFloat();
float f3 = -((float)(i3 + j3) + partialTick) / 32.0F * f2;
float f4 = f3 % 32.0F;
```

- 1.21.1 的雨滴下落速度包含随机组件(`f2 = 3.0F + random.nextFloat()`),使雨滴速度有轻微变化
- `ticks` 掩码改为 `131071`(17 bits),种子合并方式不同
- 最终 UV 使用 `f4 = f3 % 32.0F` 卷绕

### 4.3 雨/雪光照处理

```java
// 1.20.1
int j3 = getLightColor(level, blockpos$mutableblockpos);
bufferbuilder.vertex(...).uv2(j3).endVertex();

// 1.21.1
int k3 = getLightColor(level, blockpos$mutableblockpos);
bufferbuilder.addVertex(...).setLight(k3);  // 新 API: setLight 替代 uv2
```

- 1.21.1 使用 `setLight(packedLight)` 方法统一处理光照
- 雪粒子的光照处理也改为统一 API

### 4.4 tickRain 不变

与 1.20.1 完全相同的粒子生成和雨声逻辑。

---

## 5. CloudStatus 变更

**文件**: `net/minecraft/client/CloudStatus.java` (40 行 vs 1.20.1 的 28 行)

```java
// 1.20.1
public enum CloudStatus implements OptionEnum {
    OFF(0, "options.off"),
    FAST(1, "options.clouds.fast"),
    FANCY(2, "options.clouds.fancy");
}

// 1.21.1
public enum CloudStatus implements OptionEnum, StringRepresentable {
    OFF(0, "false", "options.off"),
    FAST(1, "fast", "options.clouds.fast"),
    FANCY(2, "true", "options.clouds.fancy");

    public static final Codec<CloudStatus> CODEC = StringRepresentable.fromEnum(CloudStatus::values);
    private final String legacyName;  // "false"/"fast"/"true"

    public String getSerializedName() { return this.legacyName; }
}
```

- 新增 `StringRepresentable` 接口,支持序列化
- 新增 `CODEC` 用于数据驱动
- 新增 `legacyName` 字段: `"false"/"fast"/"true"`

---

## 6. DimensionSpecialEffects

**文件**: `net/minecraft/client/renderer/DimensionSpecialEffects.java` (151 行,几乎不变)

与 1.20.1 相比:
- 包名变更: `net.minecraftforge` → `net.neoforged.neoforge`
- 实现接口: `IForgeDimensionSpecialEffects` → `IDimensionSpecialEffectsExtension`
- 核心逻辑(三个维度特效、SkyType、getSunriseColor、getCloudHeight、Fog 方法)完全相同

---

## 7. FogRenderer 在环境渲染中的角色

**文件**: `FogRenderer.java`

与 1.20.1 相比 **几乎完全不变**:
- `setupColor`、`setupFog`、`levelFogColor`、`setupNoFog` 逻辑完全一致
- Forge hook 变更为 NeoForge hook
- `FogMode`: 仍为 `FOG_SKY` / `FOG_TERRAIN`

---

## 总结

### 1.21.1 环境渲染演进特点

| 类别 | 变更程度 |
|------|---------|
| 天空渲染(太阳/月亮/星星/sky disc) | **微调**(PoseStack/frustum 参数) |
| 云渲染 | **中度**(RenderType 封装 + MeshData) |
| 天气渲染(雨/雪) | **中度**(BufferBuilder API + 随机变化) |
| DimensionSpecialEffects | **不变**(仅 forge → neoforge 包名) |
| FogRenderer | **不变**(仅 hook 包名变更) |
| CloudStatus | **扩展**(+StringRepresentable, +CODEC, +legacyName) |

### 核心未变内容

- 所有着色器程序不变
- 所有纹理资源路径不变
- 所有几何体构建算法(cloud cells, sky disc, stars, sunrise fan)完全不变
- 所有数值常量(尺寸、速度、半径)不变
- DimensionSpecialEffects 的三个维度特效行为不变
