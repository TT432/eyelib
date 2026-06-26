# RenderType System — 1.20.1 (Forge)

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [类位置与继承层次](#1-类位置与继承层次)
2. [核心数据结构](#2-核心数据结构)
3. [机制详解](#3-机制详解)
4. [静态工厂方法清单](#4-静态工厂方法清单)
5. [与其他子系统的交互](#5-与其他子系统的交互)
6. [关键不变量与约束](#6-关键不变量与约束)

---

## 1. 类位置与继承层次

| 类名 | 包路径 | 文件 |
|---|---|---|
| `RenderStateShard` | `net.minecraft.client.renderer` | `RenderStateShard.java` (586行) |
| `RenderType` | `net.minecraft.client.renderer` | `RenderType.java` (686行) |
| `RenderType.CompositeRenderType` | `net.minecraft.client.renderer.RenderType` | 内部静态类 |
| `RenderType.CompositeState` | `net.minecraft.client.renderer.RenderType` | 内部静态类 |
| `RenderType.CompositeState.CompositeStateBuilder` | 同上 | 内部静态类 |
| `RenderType.OutlineProperty` | 同上 | 内部枚举 |
| `ForgeRenderTypes` | `net.minecraftforge.client` | Forge 扩展 |

### 继承层次

```
RenderStateShard (abstract)
├── name: String
├── setupState: Runnable
├── clearState: Runnable
├── setupRenderState() / clearRenderState()
│
├── RenderStateShard.BooleanStateShard
│   ├── CullStateShard
│   ├── LightmapStateShard
│   └── OverlayStateShard
│
├── RenderStateShard.ShaderStateShard
├── RenderStateShard.EmptyTextureStateShard
│   ├── TextureStateShard
│   └── MultiTextureStateShard
├── RenderStateShard.TransparencyStateShard
├── RenderStateShard.DepthTestStateShard
├── RenderStateShard.WriteMaskStateShard
├── RenderStateShard.LayeringStateShard
├── RenderStateShard.OutputStateShard
├── RenderStateShard.TexturingStateShard
│   └── OffsetTexturingStateShard
├── RenderStateShard.LineStateShard
├── RenderStateShard.ColorLogicStateShard
│
└── RenderType (abstract, extends RenderStateShard)
    ├── format: VertexFormat
    ├── mode: VertexFormat.Mode
    ├── bufferSize: int
    ├── affectsCrumbling: boolean
    ├── sortOnUpload: boolean
    └── RenderType.CompositeRenderType (extends RenderType)
```

---

## 2. 核心数据结构

### 2.1 RenderStateShard (基类)

```java
// RenderStateShard.java:22-26
public abstract class RenderStateShard {
    private static final float VIEW_SCALE_Z_EPSILON = 0.99975586F;
    protected final String name;
    protected Runnable setupState;
    private final Runnable clearState;
```

构造函数 (`RenderStateShard.java:223-227`):
```java
public RenderStateShard(String name, Runnable setupState, Runnable clearState) {
    this.name = name;
    this.setupState = setupState;
    this.clearState = clearState;
}
```

状态切换接口 (`RenderStateShard.java:229-235`):
```java
public void setupRenderState() { this.setupState.run(); }
public void clearRenderState() { this.clearState.run(); }
```

### 2.2 13个状态维度 (CompositeState 的 shard)

| 维度 | 类型 | 默认值 | 职责 |
|---|---|---|---|
| textureState | `EmptyTextureStateShard` | `NO_TEXTURE` | 绑定纹理 + 设置过滤模式 |
| shaderState | `ShaderStateShard` | `NO_SHADER` | 激活 ShaderInstance |
| transparencyState | `TransparencyStateShard` | `NO_TRANSPARENCY` | 控制 blend enable/func |
| depthTestState | `DepthTestStateShard` | `LEQUAL_DEPTH_TEST` | 控制 glDepthFunc |
| cullState | `CullStateShard` | `CULL` | 控制背面剔除 |
| lightmapState | `LightmapStateShard` | `NO_LIGHTMAP` | 绑定 lightmap 纹理层 |
| overlayState | `OverlayStateShard` | `NO_OVERLAY` | 绑定 overlay 纹理层 |
| layeringState | `LayeringStateShard` | `NO_LAYERING` | polygon offset / view scale |
| outputState | `OutputStateShard` | `MAIN_TARGET` | 切换 FBO 渲染目标 |
| texturingState | `TexturingStateShard` | `DEFAULT_TEXTURING` | 设置纹理矩阵 |
| writeMaskState | `WriteMaskStateShard` | `COLOR_DEPTH_WRITE` | 控制 color/depth mask |
| lineState | `LineStateShard` | `DEFAULT_LINE` | 控制线宽 |
| colorLogicState | `ColorLogicStateShard` | `NO_COLOR_LOGIC` | 控制颜色逻辑操作 |

### 2.3 RenderType (抽象基类)

```java
// RenderType.java:32
public abstract class RenderType extends RenderStateShard {
    // 缓冲区大小常量
    public static final int BIG_BUFFER_SIZE = 2097152;   // 2MB
    public static final int MEDIUM_BUFFER_SIZE = 262144;  // 256KB
    public static final int SMALL_BUFFER_SIZE = 131072;   // 128KB
    public static final int TRANSIENT_BUFFER_SIZE = 256;

    // 核心字段
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final int bufferSize;
    private final boolean affectsCrumbling;
    private final boolean sortOnUpload;
    private final Optional<RenderType> asOptional;
```

构造函数 (`RenderType.java:496-504`):
```java
public RenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                  int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                  Runnable setupState, Runnable clearState) {
    super(name, setupState, clearState);
    this.format = format;
    this.mode = mode;
    this.bufferSize = bufferSize;
    this.affectsCrumbling = affectsCrumbling;
    this.sortOnUpload = sortOnUpload;
    this.asOptional = Optional.of(this);
}
```

### 2.4 `RenderType.create()` 两个重载

```java
// 5参版(内部用) — RenderType.java:506-508
static RenderType.CompositeRenderType create(String name, VertexFormat format,
    VertexFormat.Mode mode, int bufferSize, RenderType.CompositeState state) {
    return create(name, format, mode, bufferSize, false, false, state);
}

// 7参版(公开) — RenderType.java:510-512
public static RenderType.CompositeRenderType create(String name, VertexFormat format,
    VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
    boolean sortOnUpload, RenderType.CompositeState state) {
    return new RenderType.CompositeRenderType(name, format, mode, bufferSize,
        affectsCrumbling, sortOnUpload, state);
}
```

### 2.5 CompositeRenderType (内部类)

```java
// RenderType.java:557-591
public static final class CompositeRenderType extends RenderType {
    static final BiFunction<ResourceLocation, CullStateShard, RenderType> OUTLINE =
        Util.memoize((p_286176_, p_286177_) -> RenderType.create("outline",
            DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256,
            CompositeState.builder()
                .setShaderState(RENDERTYPE_OUTLINE_SHADER)
                .setTextureState(new TextureStateShard(p_286176_, false, false))
                .setCullState(p_286177_)
                .setDepthTestState(NO_DEPTH_TEST)
                .setOutputState(OUTLINE_TARGET)
                .createCompositeState(OutlineProperty.IS_OUTLINE)));

    private final RenderType.CompositeState state;
    private final Optional<RenderType> outline;
    private final boolean isOutline;

    CompositeRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
            int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
            RenderType.CompositeState state) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload,
            () -> state.states.forEach(RenderStateShard::setupRenderState),
            () -> state.states.forEach(RenderStateShard::clearRenderState));
        this.state = state;
        this.outline = state.outlineProperty == OutlineProperty.AFFECTS_OUTLINE
            ? state.textureState.cutoutTexture()
                .map(p_173270_ -> OUTLINE.apply(p_173270_, state.cullState))
            : Optional.empty();
        this.isOutline = state.outlineProperty == OutlineProperty.IS_OUTLINE;
    }
}
```

关键：`CompositeRenderType` 将 `setupRenderState`/`clearRenderState` 代理为遍历 `state.states` 的 `ImmutableList<RenderStateShard>`，逐项调用 `setupRenderState()`/`clearRenderState()`，形成**Runnable 串联链**。

### 2.6 CompositeState + CompositeStateBuilder

CompositeState 字段 (`RenderType.java:606-620`):
```java
public static final class CompositeState {
    final EmptyTextureStateShard textureState;
    private final ShaderStateShard shaderState;
    private final TransparencyStateShard transparencyState;
    private final DepthTestStateShard depthTestState;
    final CullStateShard cullState;
    private final LightmapStateShard lightmapState;
    private final OverlayStateShard overlayState;
    private final LayeringStateShard layeringState;
    private final OutputStateShard outputState;
    private final TexturingStateShard texturingState;
    private final WriteMaskStateShard writeMaskState;
    private final LineStateShard lineState;
    private final ColorLogicStateShard colorLogicState;
    final OutlineProperty outlineProperty;
    final ImmutableList<RenderStateShard> states;
```

states 列表构造顺序 (`RenderType.java:638`):
```java
this.states = ImmutableList.of(
    this.textureState, this.shaderState, this.transparencyState,
    this.depthTestState, this.cullState, this.lightmapState,
    this.overlayState, this.layeringState, this.outputState,
    this.texturingState, this.writeMaskState, this.colorLogicState,
    this.lineState);
```

Builder 默认值 (`RenderType.java:647-658`):
```java
public static class CompositeStateBuilder {
    private EmptyTextureStateShard textureState = NO_TEXTURE;
    private ShaderStateShard shaderState = NO_SHADER;
    private TransparencyStateShard transparencyState = NO_TRANSPARENCY;
    private DepthTestStateShard depthTestState = LEQUAL_DEPTH_TEST;
    private CullStateShard cullState = CULL;
    private LightmapStateShard lightmapState = NO_LIGHTMAP;
    private OverlayStateShard overlayState = NO_OVERLAY;
    private LayeringStateShard layeringState = NO_LAYERING;
    private OutputStateShard outputState = MAIN_TARGET;
    private TexturingStateShard texturingState = DEFAULT_TEXTURING;
    private WriteMaskStateShard writeMaskState = COLOR_DEPTH_WRITE;
    private LineStateShard lineState = DEFAULT_LINE;
    private ColorLogicStateShard colorLogicState = NO_COLOR_LOGIC;
```

Builder 终结方法:
```java
// 布尔 overlap 参数版
public CompositeState createCompositeState(boolean outline) {
    return this.createCompositeState(outline
        ? OutlineProperty.AFFECTS_OUTLINE : OutlineProperty.NONE);
}

// OutlineProperty 枚举版
public CompositeState createCompositeState(OutlineProperty outlineState) {
    return new CompositeState(this.textureState, this.shaderState,
        this.transparencyState, this.depthTestState, this.cullState,
        this.lightmapState, this.overlayState, this.layeringState,
        this.outputState, this.texturingState, this.writeMaskState,
        this.lineState, this.colorLogicState, outlineState);
}
```

### 2.7 OutlineProperty 枚举

```java
static enum OutlineProperty {
    NONE("none"),
    IS_OUTLINE("is_outline"),
    AFFECTS_OUTLINE("affects_outline");
}
```

### 2.8 关键 RenderStateShard 常量定义

**TransparencyState 预设**:
| 常量 | blendFunc |
|---|---|
| `NO_TRANSPARENCY` | disable blend |
| `ADDITIVE_TRANSPARENCY` | ONE, ONE |
| `LIGHTNING_TRANSPARENCY` | SRC_ALPHA, ONE |
| `GLINT_TRANSPARENCY` | SRC_COLOR, ONE / ZERO, ONE (separate) |
| `CRUMBLING_TRANSPARENCY` | DST_COLOR, SRC_COLOR / ONE, ZERO (separate) |
| `TRANSLUCENT_TRANSPARENCY` | SRC_ALPHA, 1-SRC_ALPHA / ONE, 1-SRC_ALPHA (separate) |

**DepthTestState 预设**:
| 常量 | GL 函数 | GL 常量值 |
|---|---|---|
| `NO_DEPTH_TEST` | always | 519 (GL_ALWAYS) |
| `EQUAL_DEPTH_TEST` | == | 514 (GL_EQUAL) |
| `LEQUAL_DEPTH_TEST` | <= | 515 (GL_LEQUAL) |
| `GREATER_DEPTH_TEST` | > | 516 (GL_GREATER) |

**WriteMaskState 预设**:
| 常量 | color mask | depth mask |
|---|---|---|
| `COLOR_DEPTH_WRITE` | true | true |
| `COLOR_WRITE` | true | false |
| `DEPTH_WRITE` | false | true |

**OutputTarget 预设** (均为 OutputStateShard 实例):
| 常量 | 绑定的 FBO |
|---|---|
| `MAIN_TARGET` | 无操作(已是主目标) |
| `OUTLINE_TARGET` | levelRenderer.entityTarget() |
| `TRANSLUCENT_TARGET` | levelRenderer.getTranslucentTarget() |
| `PARTICLES_TARGET` | levelRenderer.getParticlesTarget() |
| `WEATHER_TARGET` | levelRenderer.getWeatherTarget() |
| `CLOUDS_TARGET` | levelRenderer.getCloudsTarget() |
| `ITEM_ENTITY_TARGET` | levelRenderer.getItemEntityTarget() |

所有 OutputTarget(除 MAIN_TARGET) 有条件判断 `Minecraft.useShaderTransparency()` — 仅在 fabulous 画质下切换。

**LayeringState 预设**:
| 常量 | 操作 |
|---|---|
| `NO_LAYERING` | 无操作 |
| `POLYGON_OFFSET_LAYERING` | polygonOffset(-1, -10) + enable |
| `VIEW_OFFSET_Z_LAYERING` | modelViewStack.scale(0.99975586) |

**TexturingState 预设**:
| 常量 | 操作 |
|---|---|
| `DEFAULT_TEXTURING` | 无操作 |
| `GLINT_TEXTURING` | 8.0F 速度光灵纹理矩阵动画 |
| `ENTITY_GLINT_TEXTURING` | 0.16F 速度光灵纹理矩阵动画 |

---

## 3. 机制详解

### 3.1 `setupRenderState` / `clearRenderState` 机制

1. `CompositeRenderType` 构造时，将 setup/clear 回调定义为 `state.states.forEach(RenderStateShard::setupRenderState)` 和 `state.states.forEach(RenderStateShard::clearRenderState)`
2. 调用 `setupRenderState()` 时，按 `ImmutableList` 顺序依次执行 13 个 shard 的 setup Runnable
3. 调用 `clearRenderState()` 时，按同样顺序依次执行 13 个 shard 的 clear Runnable
4. 每个 shard 的 setup/clear 成对定义：setup 设置 GL 状态，clear 恢复默认 GL 状态
5. **无状态栈机制** — 不存在 push/pop，完全依赖 clear 恢复

### 3.2 `end()` 渲染提交流程

```java
// RenderType.java:514-524
public void end(BufferBuilder bufferBuilder, VertexSorting quadSorting) {
    if (bufferBuilder.building()) {
        if (this.sortOnUpload) {
            bufferBuilder.setQuadSorting(quadSorting);
        }
        BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();
        this.setupRenderState();
        BufferUploader.drawWithShader(renderedBuffer);
        this.clearRenderState();
    }
}
```

流程: 检查是否在构建 → 可选设置排序 → `BufferBuilder.end()` → `setupRenderState()` → `BufferUploader.drawWithShader()` → `clearRenderState()`

### 3.3 `Util.memoize` 缓存机制

所有带 `ResourceLocation` 参数的工厂方法使用 `Util.memoize()` 包裹 lambda，实现**懒加载 + 单例缓存**：

```java
private static final Function<ResourceLocation, RenderType> ENTITY_SOLID =
    Util.memoize((p_286159_) -> {
        CompositeState state = CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
            .setTextureState(new TextureStateShard(p_286159_, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
        return create("entity_solid", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 256, true, false, state);
    });
```

- `Util.memoize` 内部使用 `AtomicReference` + `synchronized` 保证线程安全
- 首次 `.apply(location)` 调用时计算并缓存，后续返回同一实例
- 对于 `BiFunction` 工厂（如 `entityCutoutNoCull`），以 `(ResourceLocation, Boolean)` 对为 key

### 3.4 状态绑定细节

**Shader 绑定** (`ShaderStateShard`): setup 通过 `RenderSystem.setShader(Supplier<ShaderInstance>)` 激活 shader，clear 无操作（shader 在下一次 setShader 时被替换）。

**纹理绑定** (`TextureStateShard`): setup 通过 `TextureManager.getTexture(texture).setFilter(blur, mipmap)` 设置过滤，然后 `RenderSystem.setShaderTexture(0, texture)` 绑定到纹理单元 0。clear 无操作。

**Lightmap 绑定** (`LightmapStateShard`): setup 调用 `LightTexture.turnOnLightLayer()` 绑定 lightmap 纹理到 GL 纹理单元，clear 调用 `turnOffLightLayer()`。

**Overlay 绑定** (`OverlayStateShard`): setup 调用 `OverlayTexture.setupOverlayColor()`，clear 调用 `teardownOverlayColor()`。

---

## 4. 静态工厂方法清单

### 4.1 无参数(返回单例常量)

| 静态方法 | 内部常量名 | 语义 |
|---|---|---|
| `solid()` | `SOLID` | 方块实心 |
| `cutoutMipped()` | `CUTOUT_MIPPED` | 方块镂空+mipmap |
| `cutout()` | `CUTOUT` | 方块镂空 |
| `translucent()` | `TRANSLUCENT` | 方块半透明 |
| `translucentMovingBlock()` | `TRANSLUCENT_MOVING_BLOCK` | 移动方块半透明 |
| `translucentNoCrumbling()` | `TRANSLUCENT_NO_CRUMBLING` | 无破坏动画半透明 |
| `leash()` | `LEASH` | 拴绳 |
| `waterMask()` | `WATER_MASK` | 水面遮罩 |
| `armorGlint()` | `ARMOR_GLINT` | 盔甲光灵(物品) |
| `armorEntityGlint()` | `ARMOR_ENTITY_GLINT` | 盔甲光灵(实体) |
| `glintTranslucent()` | `GLINT_TRANSLUCENT` | 半透明光灵(item target) |
| `glint()` | `GLINT` | 普通光灵 |
| `glintDirect()` | `GLINT_DIRECT` | 直接光灵 |
| `entityGlint()` | `ENTITY_GLINT` | 实体光灵(item target) |
| `entityGlintDirect()` | `ENTITY_GLINT_DIRECT` | 实体直接光灵 |
| `lightning()` | `LIGHTNING` | 闪电 |
| `tripwire()` | `TRIPWIRE` | 绊线 |
| `endPortal()` | `END_PORTAL` | 末地传送门 |
| `endGateway()` | `END_GATEWAY` | 末地折跃门 |
| `lines()` | `LINES` | 线框(LINES mode) |
| `lineStrip()` | `LINE_STRIP` | 线带 |
| `debugFilledBox()` | `DEBUG_FILLED_BOX` | 调试填充盒 |
| `debugQuads()` | `DEBUG_QUADS` | 调试四边形 |
| `debugSectionQuads()` | `DEBUG_SECTION_QUADS` | 调试区块四边形 |
| `gui()` | `GUI` | GUI |
| `guiOverlay()` | `GUI_OVERLAY` | GUI 叠加层 |
| `guiTextHighlight()` | `GUI_TEXT_HIGHLIGHT` | GUI 文本高亮 |
| `guiGhostRecipeOverlay()` | `GUI_GHOST_RECIPE_OVERLAY` | GUI 配方透明覆盖 |
| `textBackground()` | `TEXT_BACKGROUND` | 文本背景 |
| `textBackgroundSeeThrough()` | `TEXT_BACKGROUND_SEE_THROUGH` | 穿透文本背景 |

### 4.2 Function<ResourceLocation, RenderType> (缓存工厂)

| 静态方法 | 内部 Lambda 名 | 纹理参数 | VertexFormat | affectCrumbling | sortOnUpload | 关键 CompositeState 特征 |
|---|---|---|---|---|---|---|
| `armorCutoutNoCull(loc)` | `ARMOR_CUTOUT_NO_CULL` | location | NEW_ENTITY | true | false | NO_CULL + VIEW_OFFSET_Z_LAYERING |
| `entitySolid(loc)` | `ENTITY_SOLID` | location | NEW_ENTITY | true | false | NO_TRANSPARENCY + 影响 outline |
| `entityCutout(loc)` | `ENTITY_CUTOUT` | location | NEW_ENTITY | true | false | NO_TRANSPARENCY |
| `itemEntityTranslucentCull(loc)` | `ITEM_ENTITY_TRANSLUCENT_CULL` | location | NEW_ENTITY | true | true | TRANSLUCENT + ITEM_ENTITY_TARGET + COLOR_DEPTH_WRITE |
| `entityTranslucentCull(loc)` | `ENTITY_TRANSLUCENT_CULL` | location | NEW_ENTITY | true | true | TRANSLUCENT (cull ON) |
| `entitySmoothCutout(loc)` | `ENTITY_SMOOTH_CUTOUT` | location | NEW_ENTITY | false(默认) | false(默认) | NO_CULL, 无 transparency |
| `entityDecal(loc)` | `ENTITY_DECAL` | location | NEW_ENTITY | false | false | EQUAL_DEPTH_TEST + NO_CULL, outline=NONE |
| `entityNoOutline(loc)` | `ENTITY_NO_OUTLINE` | location | NEW_ENTITY | false | true | TRANSLUCENT + NO_CULL + COLOR_WRITE, outline=NONE |
| `entityShadow(loc)` | `ENTITY_SHADOW` | location | NEW_ENTITY | false | false | TRANSLUCENT + COLOR_WRITE + LEQUAL_DEPTH_TEST + VIEW_OFFSET_Z_LAYERING, outline=NONE |
| `dragonExplosionAlpha(loc)` | `DRAGON_EXPLOSION_ALPHA` | location | NEW_ENTITY | false(默认) | false(默认) | NO_CULL, 仅 shader+texture, outline=AFFECTS |
| `eyes(loc)` | `EYES` | location | NEW_ENTITY | false | true | ADDITIVE_TRANSPARENCY + COLOR_WRITE, outline=NONE |
| `crumbling(loc)` | `CRUMBLING` | location | BLOCK | false | true | CRUMBLING_TRANSPARENCY + COLOR_WRITE + POLYGON_OFFSET_LAYERING, outline=NONE |
| `text(loc)` | `TEXT` | location | POSITION_COLOR_TEX_LIGHTMAP | false | true | TRANSLUCENT (Forge 代理) |
| `textIntensity(loc)` | `TEXT_INTENSITY` | location | POSITION_COLOR_TEX_LIGHTMAP | false | true | TRANSLUCENT (Forge 代理) |
| `textPolygonOffset(loc)` | `TEXT_POLYGON_OFFSET` | location | POSITION_COLOR_TEX_LIGHTMAP | false | true | TRANSLUCENT + POLYGON_OFFSET (Forge 代理) |
| `textIntensityPolygonOffset(loc)` | `TEXT_INTENSITY_POLYGON_OFFSET` | location | POSITION_COLOR_TEX_LIGHTMAP | false | true | TRANSLUCENT + POLYGON_OFFSET (Forge 代理) |
| `textSeeThrough(loc)` | `TEXT_SEE_THROUGH` | location | POSITION_COLOR_TEX_LIGHTMAP | false | true | TRANSLUCENT + NO_DEPTH_TEST + COLOR_WRITE (Forge 代理) |
| `textIntensitySeeThrough(loc)` | `TEXT_INTENSITY_SEE_THROUGH` | location | POSITION_COLOR_TEX_LIGHTMAP | false | true | TRANSLUCENT + NO_DEPTH_TEST + COLOR_WRITE (Forge 代理) |

### 4.3 BiFunction<ResourceLocation, Boolean, RenderType> (缓存工厂)

| 静态方法 | 内部 Lambda 名 | Boolean 参数含义 |
|---|---|---|
| `entityCutoutNoCull(loc, outline)` | `ENTITY_CUTOUT_NO_CULL` | outline → `createCompositeState(outline)` |
| `entityCutoutNoCullZOffset(loc, outline)` | `ENTITY_CUTOUT_NO_CULL_Z_OFFSET` | outline + VIEW_OFFSET_Z_LAYERING |
| `entityTranslucent(loc, outline)` | `ENTITY_TRANSLUCENT` | outline + TRANSLUCENT_TRANSPARENCY + NO_CULL |
| `entityTranslucentEmissive(loc, outline)` | `ENTITY_TRANSLUCENT_EMISSIVE` | outline + NO_CULL + COLOR_WRITE (no lightmap) |
| `beaconBeam(loc, colorFlag)` | `BEACON_BEAM` | colorFlag → TRANSLUCENT vs NO_TRANSPARENCY + COLOR_WRITE vs COLOR_DEPTH_WRITE |

### 4.4 特殊参数工厂

| 静态方法 | 额外参数 | 关键特征 |
|---|---|---|
| `energySwirl(loc, u, v)` | float u, float v | OffsetTexturingStateShard + ADDITIVE_TRANSPARENCY + NO_CULL |
| `debugLineStrip(width)` | double width | DEBUG_LINE_STRIP mode + POSITION_COLOR_SHADER |
| `outline(loc)` | — | 委托 `CompositeRenderType.OUTLINE.apply(loc, NO_CULL)` |

### 4.5 chunkBufferLayers

```java
private static final ImmutableList<RenderType> CHUNK_BUFFER_LAYERS =
    ImmutableList.of(solid(), cutoutMipped(), cutout(), translucent(), tripwire());
```

在 static 初始化块中，每个 layer 被赋予递增的 `chunkLayerId`（Forge 扩展）。

---

## 5. 与其他子系统的交互

### 5.1 BufferUploader

`RenderType.end()` 调用 `BufferUploader.drawWithShader(BufferBuilder.RenderedBuffer)`：
- 将 BufferBuilder 产出的顶点缓冲上传到 GPU
- 使用当前 RenderSystem 设置的 shader 执行 draw call
- 单次绘制提交后释放临时缓冲

### 5.2 ShaderInstance (通过 GameRenderer)

所有 `RENDERTYPE_*` shader 常量通过 `GameRenderer::getRendertype*Shader` 方法引用懒加载：
- `GameRenderer` 持有所有 shader 实例
- `ShaderStateShard` 的 setup 调用 `RenderSystem.setShader(Supplier<ShaderInstance>)`
- shader 对象在资源重载时刷新

### 5.3 RenderTarget (FBO 切换)

`OutputStateShard` 控制渲染目标：
- setup: 切换到特定 FBO (如 translucent target)
- clear: 切回主 FBO
- 条件：仅在 `Minecraft.useShaderTransparency()` 为 true 时生效

### 5.4 TextureManager

`TextureStateShard.setup` 通过 `Minecraft.getInstance().getTextureManager()` 获取纹理实例并设置过滤模式。

### 5.5 ForgeRenderTypes (Forge 扩展层)

1.20.1 Forge 通过 `ForgeRenderTypes` 类拦截 `text()`、`textIntensity()` 等工厂方法，允许模组替换这些 RenderType 实现：
```java
public static RenderType text(ResourceLocation location) {
    return net.minecraftforge.client.ForgeRenderTypes.getText(location);
}
```

---

## 6. 关键不变量与约束

1. **不可变性**: 所有 RenderType 实例构造后不可变 — `final` 字段，无 setter
2. **线程安全**: `Util.memoize` 使用 `AtomicReference` + `synchronized`，适合多线程并发首次访问
3. **缓存语义**: 相同 `(ResourceLocation)` 或 `(ResourceLocation, Boolean)` 参数组合永远返回同一实例
4. **状态恢复**: 每个 shard 的 setup/clear 必须成对 — setup 修改的 GL 状态必须在 clear 中完全恢复
5. **CompositeRenderType 不可被外部继承**: 构造为 `public static final class`，只能用 `create()` 工厂构建
6. **setupRenderState 执行顺序固定**: 按 `ImmutableList` 序执行(纹理→着色器→透明度→深度→剔除→光照→覆盖→分层→输出→纹理矩阵→写掩码→颜色逻辑→线宽)
7. **Forge chunkLayerId**: Forge 通过 `getChunkLayerId()` 为每个 chunk layer 赋予唯一 ID(0-4)
8. **VertexSorting 影响**: 仅 `sortOnUpload=true` 时 BufferBuilder 才应用 `quadSorting`
