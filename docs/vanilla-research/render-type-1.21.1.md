# RenderType System — 1.21.1 (NeoForge)

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [类位置与继承层次](#1-类位置与继承层次)
2. [核心数据结构](#2-核心数据结构)
3. [机制详解](#3-机制详解)
4. [静态工厂方法清单](#4-静态工厂方法清单)
5. [与其他子系统的交互](#5-与其他子系统的交互)
6. [关键不变量与约束](#6-关键不变量与约束)
7. [与 1.20.1 的差异汇总](#7-与-1201-的差异汇总)

---

## 1. 类位置与继承层次

| 类名 | 包路径 | 文件 |
|---|---|---|
| `RenderStateShard` | `net.minecraft.client.renderer` | `RenderStateShard.java` (687行) |
| `RenderType` | `net.minecraft.client.renderer` | `RenderType.java` (1470行) |
| `RenderType.CompositeRenderType` | `net.minecraft.client.renderer.RenderType` | 内部静态类 |
| `RenderType.CompositeState` | `net.minecraft.client.renderer.RenderType` | 内部静态类 |
| `RenderType.CompositeState.CompositeStateBuilder` | 同上 | 内部静态类 |
| `RenderType.OutlineProperty` | 同上 | 内部枚举 |

> 注：1.21.1 已无 `ForgeRenderTypes` 外部代理；所有工厂方法直接使用 `RenderType.create()`。

### 继承层次

与 1.20.1 相同 — RenderStateShard → RenderType → CompositeRenderType。

---

## 2. 核心数据结构

### 2.1 RenderStateShard 可见性变更

1.21.1 将多个 `protected` 字段提升为 `public`：

```java
// RenderStateShard.java:24-26 — 1.21.1
public abstract class RenderStateShard {
    public static final float VIEW_SCALE_Z_EPSILON = 0.99975586F;
    public static final double MAX_ENCHANTMENT_GLINT_SPEED_MILLIS = 8.0;
    public final String name;
    public final Runnable setupState;
    public final Runnable clearState;
```

关键变更：
- `VIEW_SCALE_Z_EPSILON`: `private static` → `public static`
- `name`: `protected` → `public final`
- `setupState`: `protected` (非 final) → `public final`
- `clearState`: `private` → `public final`

`setupGlintTexturing` 也从 `private static` 变为 `public static`。

### 2.2 RenderType 缓冲区大小常量

```java
public static final int BIG_BUFFER_SIZE = 4194304;    // 4MB (1.20.1: 2MB)
public static final int SMALL_BUFFER_SIZE = 786432;    // 768KB (1.20.1: 128KB)
public static final int TRANSIENT_BUFFER_SIZE = 1536;  // (1.20.1: 256)
// MEDIUM_BUFFER_SIZE 已移除
```

### 2.3 `RenderType.end()` 签名变更

1.20.1 使用 `BufferBuilder` + `BufferUploader`:
```java
// 1.20.1
public void end(BufferBuilder bufferBuilder, VertexSorting quadSorting) {
    BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();
    this.setupRenderState();
    BufferUploader.drawWithShader(renderedBuffer);
    this.clearRenderState();
}
```

1.21.1 使用 `MeshData`:
```java
// 1.21.1
public void end(MeshData meshData, VertexSorting vertexSorting) {
    if (this.sortOnUpload) {
        meshData.sortQuads(this.format, vertexSorting);
    }
    this.setupRenderState();
    meshData.drawWithShader();
    this.clearRenderState();
}
```

### 2.4 CompositeState Builder 默认值变化

| 维度 | 1.20.1 默认 | 1.21.1 默认 |
|---|---|---|
| cullState | `CULL` | `NO_CULL` |

其他 12 个维度的默认值不变。

### 2.5 ShaderState 常量变化

**1.21.1 新增**:
| 常量 | 来源 |
|---|---|
| `RENDERTYPE_CLOUDS_SHADER` | `GameRenderer::getRendertypeCloudsShader` |
| `RENDERTYPE_BREEZE_WIND_SHADER` | `GameRenderer::getRendertypeBreezeWindShader` |

**1.21.1 移除** (相对于 1.20.1):
| 移除的常量 |
|---|
| `RENDERTYPE_ARMOR_GLINT_SHADER` |
| `RENDERTYPE_GLINT_DIRECT_SHADER` |
| `RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER` |
| `RENDERTYPE_TRANSLUCENT_NO_CRUMBLING_SHADER` |
| `POSITION_COLOR_TEX_SHADER` |

### 2.6 预设透明/深度/写掩码状态

与 1.20.1 完全相同 — `NO_TRANSPARENCY`, `ADDITIVE_TRANSPARENCY`, `LIGHTNING_TRANSPARENCY`, `GLINT_TRANSPARENCY`, `CRUMBLING_TRANSPARENCY`, `TRANSLUCENT_TRANSPARENCY` 及所有 DepthTest 和 WriteMask 预设均一致。

---

## 3. 机制详解

### 3.1 `setupRenderState` / `clearRenderState`

机制与 1.20.1 相同。唯一区别是字段可见性从 `protected`/`private` 提升为 `public final`，使得外部可直接读取 setupState/clearState Runnable 引用。

### 3.2 `Util.memoize` 缓存机制

与 1.20.1 相同 — `Function<ResourceLocation, RenderType>` 和 `BiFunction<ResourceLocation, Boolean, RenderType>` 均使用 `Util.memoize()` 懒缓存。

### 3.3 `MeshData.drawWithShader()`

1.21.1 将原本 `BufferUploader.drawWithShader()` 的逻辑内联到 `MeshData` 类中，`RenderType.end()` 直接调用 `meshData.drawWithShader()` 不再经过 BufferUploader 静态方法。

---

## 4. 静态工厂方法清单

### 4.1 无参数(返回单例常量)

| 静态方法 | 1.20.1对应 | 说明 |
|---|---|---|
| `solid()` | 同 | 方块实心 |
| `cutoutMipped()` | 同 | 方块镂空+mipmap |
| `cutout()` | 同 | 方块镂空 |
| `translucent()` | 同 | 方块半透明 |
| `translucentMovingBlock()` | 同 | 移动方块半透明 |
| `leash()` | 同 | 拴绳 |
| `waterMask()` | 同 | 水面遮罩 |
| `armorEntityGlint()` | 同 | 盔甲光灵(实体) |
| `glintTranslucent()` | 同 | 半透明光灵(item target) |
| `glint()` | 同 | 普通光灵 |
| `entityGlint()` | 同 | 实体光灵 |
| `lightning()` | 同 | 闪电 |
| `tripwire()` | 同 | 绊线 |
| `endPortal()` | 同 | 末地传送门 |
| `endGateway()` | 同 | 末地折跃门 |
| `lines()` | `lines()` | 线框(LINES mode) |
| `lineStrip()` | `lineStrip()` | 线带 |
| `debugFilledBox()` | 同 | 调试填充盒 |
| `debugQuads()` | 同 | 调试四边形 |
| `gui()` | 同 | GUI |
| `guiOverlay()` | 同 | GUI 叠加层 |
| `guiTextHighlight()` | 同 | GUI 文本高亮 |
| `guiGhostRecipeOverlay()` | 同 | GUI 配方透明覆盖 |
| `textBackground()` | 同 | 文本背景 |
| `textBackgroundSeeThrough()` | 同 | 穿透文本背景 |
| `clouds()` | **新增** | 云渲染 |
| `dragonRays()` | **新增** | 龙息射线(颜色) |
| `dragonRaysDepth()` | **新增** | 龙息射线(深度) |

**1.21.1 移除的常量工厂**:
- `armorGlint()` — 被合并/移除
- `glintDirect()` — 移除
- `entityGlintDirect()` — 移除
- `translucentNoCrumbling()` — 移除
- `debugSectionQuads()` — 移除

### 4.2 Function<ResourceLocation, RenderType>

| 静态方法 | 关键 CompositeState 特征 |
|---|---|
| `armorCutoutNoCull(loc)` | NO_CULL + VIEW_OFFSET_Z_LAYERING，影响 outline |
| `entitySolid(loc)` | NO_TRANSPARENCY，影响 outline |
| `entityCutoutCull(loc)` | **新增** — CULL(开启剔除) + ALPHA_CUTOUT shader define |
| `entityCutout(loc)` | NO_TRANSPARENCY，去除了 Boolean 参数的默认重载 |
| `itemEntityTranslucentCull(loc)` | TRANSLUCENT + ITEM_ENTITY_TARGET + COLOR_DEPTH_WRITE |
| `entityTranslucentCull(loc)` | TRANSLUCENT (cull ON) |
| `entitySmoothCutout(loc)` | NO_CULL, outline=NONE |
| `entityDecal(loc)` | EQUAL_DEPTH_TEST + NO_CULL |
| `entityNoOutline(loc)` | TRANSLUCENT + NO_CULL + COLOR_WRITE |
| `entityShadow(loc)` | TRANSLUCENT + COLOR_WRITE + LEQUAL_DEPTH + VIEW_OFFSET_Z_LAYERING |
| `dragonExplosionAlpha(loc)` | NO_CULL |
| `eyes(loc)` | ADDITIVE_TRANSPARENCY + COLOR_WRITE |
| `crumbling(loc)` | CRUMBLING_TRANSPARENCY + COLOR_WRITE + POLYGON_OFFSET_LAYERING |
| `text(loc)` | **直接 create()**，不再走 Forge 代理 |
| `textIntensity(loc)` | **直接 create()** |
| `textPolygonOffset(loc)` | **直接 create()** |
| `textIntensityPolygonOffset(loc)` | **直接 create()** |
| `textSeeThrough(loc)` | **直接 create()** |
| `textIntensitySeeThrough(loc)` | **直接 create()** |

### 4.3 BiFunction<ResourceLocation, Boolean, RenderType>

| 静态方法 | Boolean 参数含义 |
|---|---|
| `entityCutoutNoCull(loc, outline)` | outline → createCompositeState(outline) |
| `entityCutoutNoCullZOffset(loc, outline)` | outline + VIEW_OFFSET_Z_LAYERING |
| `entityTranslucent(loc, outline)` | outline + TRANSLUCENT_TRANSPARENCY + NO_CULL |
| `entityTranslucentEmissive(loc, outline)` | outline + NO_CULL + COLOR_WRITE (no lightmap) |
| `beaconBeam(loc, colorFlag)` | colorFlag → TRANSLUCENT vs NO_TRANSPARENCY |

### 4.4 特殊参数工厂

| 静态方法 | 额外参数 | 特征 |
|---|---|---|
| `energySwirl(loc, u, v)` | float u, float v | OffsetTexturingStateShard + ADDITIVE_TRANSPARENCY |
| `breezeWind(loc, u, v)` | float u, float v | **新增** — OffsetTexturingStateShard + BREEZE_WIND shader |
| `debugLineStrip(width)` | double width | DEBUG_LINE_STRIP mode |

---

## 5. 与其他子系统的交互

### 5.1 MeshData (替代 BufferBuilder)

1.21.1 引入了 `MeshData` 抽象层：
- `MeshData` 替代了 1.20.1 的 `BufferBuilder + BufferUploader` 组合
- `MeshData.drawWithShader()` 内部封装了顶点缓冲上传 + draw call
- `MeshData.sortQuads(VertexFormat, VertexSorting)` 替代了 `BufferBuilder.setQuadSorting()`

### 5.2 无 ForgeRenderTypes 代理

1.21.1直接使用 NeoForge (`net.neoforged.api.distmarker`)，不再需要通过 `ForgeRenderTypes` 外部代理创建 text 相关 RenderType。所有 TEXT_* 工厂方法直接调用 `RenderType.create()`。

### 5.3 ShaderInstance / RenderTarget / TextureManager

与 1.20.1 交互方式相同。

---

## 6. 关键不变量与约束

与 1.20.1 相同，另加以下变更：
1. **字段不可变但公开**: `setupState`/`clearState` 从 `protected`/`private` 提升为 `public final`，外部可读取但不能修改
2. **CompositeState 默认剔除关闭**: `NO_CULL` 而非 `CULL`
3. **无 Forge 外部代理层**: 工厂方法不再通过第三方类转发
4. **缓冲区大小上调**: 适应 1.21 更大的区块渲染需求

---

## 7. 与 1.20.1 的差异汇总

| 维度 | 1.20.1 | 1.21.1 |
|---|---|---|
| Mod 框架 | Forge (`@OnlyIn(Dist.CLIENT)`) | NeoForge (`@OnlyIn(Dist.CLIENT)`) |
| `RenderStateShard.setupState` 可见性 | `protected` (非 final) | `public final` |
| `RenderStateShard.clearState` 可见性 | `private` | `public final` |
| `VIEW_SCALE_Z_EPSILON` | `private static` | `public static` |
| `setupGlintTexturing` | `private static` | `public static` |
| `BIG_BUFFER_SIZE` | 2097152 (2MB) | 4194304 (4MB) |
| `SMALL_BUFFER_SIZE` | 131072 (128KB) | 786432 (768KB) |
| `TRANSIENT_BUFFER_SIZE` | 256 | 1536 |
| `MEDIUM_BUFFER_SIZE` | 262144 (256KB) | 移除 |
| CompositeState 默认 cullState | `CULL` | `NO_CULL` |
| 绘制接口 | `BufferBuilder.end()` + `BufferUploader.drawWithShader()` | `MeshData.drawWithShader()` |
| 云 shader | 无 | `RENDERTYPE_CLOUDS_SHADER` |
| Breeze Wind shader | 无 | `RENDERTYPE_BREEZE_WIND_SHADER` |
| Armor Glint shader | 有 | 移除 |
| Glint Direct shader | 有 | 移除 |
| Entity Glint Direct shader | 有 | 移除 |
| Translucent No Crumbling | 有 | 移除 |
| POSITION_COLOR_TEX_SHADER | 有 | 移除 |
| `armorGlint()` | 有 | 移除 |
| `glintDirect()` | 有 | 移除 |
| `entityGlintDirect()` | 有 | 移除 |
| `translucentNoCrumbling()` | 有 | 移除 |
| `debugSectionQuads()` | 有 | 移除 |
| `entityCutoutCull()` | 无 | **新增** |
| `clouds()` | 无 | **新增** |
| `dragonRays()` | 无 | **新增** |
| `dragonRaysDepth()` | 无 | **新增** |
| `breezeWind(loc, u, v)` | 无 | **新增** |
| TEXT_* 工厂 | 通过 `ForgeRenderTypes` 代理 | 直接 `create()` |
