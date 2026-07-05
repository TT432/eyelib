# RenderType / RenderPipeline System — 26.1.2 (NeoForge 重大重构)

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。所有路径相对于该目录。 源码树由 `scripts/extract-mc-source.py` 重建。

## 目录

1. [类位置与继承层次](#1-类位置与继承层次)
2. [核心数据结构](#2-核心数据结构)
3. [机制详解](#3-机制详解)
4. [静态工厂方法清单](#4-静态工厂方法清单)
5. [RenderPipeline 完整常量清单](#5-renderpipeline-完整常量清单)
6. [与其他子系统的交互](#6-与其他子系统的交互)
7. [关键不变量与约束](#7-关键不变量与约束)

---

## 1. 类位置与继承层次

### 1.1 核心类表

| 类名 | 完整包路径 | 文件 |
|---|---|---|
| `RenderType` | `net.minecraft.client.renderer.rendertype` | `RenderType.java` (171行) |
| `RenderTypes` | `net.minecraft.client.renderer.rendertype` | `RenderTypes.java` (664行) |
| `RenderSetup` | `net.minecraft.client.renderer.rendertype` | `RenderSetup.java` (238行) |
| `RenderSetup.RenderSetupBuilder` | 同上(内部类) | 同上 |
| `OutputTarget` | `net.minecraft.client.renderer.rendertype` | `OutputTarget.java` (36行) |
| `TextureTransform` | `net.minecraft.client.renderer.rendertype` | `TextureTransform.java` (50行) |
| `LayeringTransform` | `net.minecraft.client.renderer.rendertype` | `LayeringTransform.java` (36行) |
| `RenderPipelines` | `net.minecraft.client.renderer` | `RenderPipelines.java` (788行) |
| `RenderPipeline` | `com.mojang.blaze3d.pipeline` | `RenderPipeline.java` (521行) |
| `RenderPipeline.Builder` | 同上(内部类) | 同上 |
| `RenderPipeline.Snippet` | 同上(内部 record) | 同上 |
| `RenderPipeline.UniformDescription` | 同上(内部 record) | 同上 |
| `ColorTargetState` | `com.mojang.blaze3d.pipeline` | `ColorTargetState.java` (48行) |
| `BlendFunction` | `com.mojang.blaze3d.pipeline` | `BlendFunction.java` (31行) |
| `DepthStencilState` | `com.mojang.blaze3d.pipeline` | `DepthStencilState.java` (15行) |
| `StencilTest` | `net.neoforged.neoforge.client.stencil` | NeoForge 扩展 |

### 1.2 继承层次(完全扁平化)

```
RenderType (非抽象, final 语义)
├── name: String
├── state: RenderSetup (不可变, 持有 RenderPipeline)
├── outline: Optional<RenderType>
├── draw(MeshData): void — 完整渲染调度
│
RenderSetup (final, 不可变)
├── pipeline: RenderPipeline
├── textures: Map<String, TextureBinding>
├── textureTransform: TextureTransform
├── outputTarget: OutputTarget
├── outlineProperty: OutlineProperty
├── useLightmap: boolean
├── useOverlay: boolean
├── affectsCrumbling: boolean
├── sortOnUpload: boolean
├── bufferSize: int
├── layeringTransform: LayeringTransform
│
RenderPipeline (final, 不可变)
├── location: Identifier (唯一标识)
├── vertexShader: Identifier
├── fragmentShader: Identifier
├── shaderDefines: ShaderDefines
├── samplers: List<String>
├── uniforms: List<UniformDescription>
├── depthStencilState: @Nullable DepthStencilState
├── polygonMode: PolygonMode
├── cull: boolean
├── colorTargetState: ColorTargetState
├── vertexFormat: VertexFormat
├── vertexFormatMode: VertexFormat.Mode
├── sortKey: int
└── stencilTest: Optional<StencilTest> (NeoForge 扩展)
```

> **关键架构变更**: 1.20.1/1.21.1 的 `RenderType extends RenderStateShard` 层次完全移除。26.1.2 的 RenderType 是一个**扁平 final 类**，组合持有 `RenderSetup`(包含 RenderPipeline)。不再有 CompositeState/RenderStateShard 树。

---

## 2. 核心数据结构

### 2.1 RenderType (扁平化)

```java
// RenderType.java:26-33
public class RenderType {
    public static final int BIG_BUFFER_SIZE = 4194304;     // 4MB
    public static final int SMALL_BUFFER_SIZE = 786432;    // 768KB
    public static final int TRANSIENT_BUFFER_SIZE = 1536;

    private final RenderSetup state;
    private final Optional<RenderType> outline;
    protected final String name;
```

**构造函数** (`RenderType.java:34-40`):
```java
private RenderType(String name, RenderSetup state) {
    this.name = name;
    this.state = state;
    this.outline = state.outlineProperty == RenderSetup.OutlineProperty.AFFECTS_OUTLINE
        ? state.textures.values().stream().findFirst()
            .map(texture -> RenderTypes.OUTLINE.apply(texture.location(), state.pipeline.isCull()))
        : Optional.empty();
}
```

**唯一工厂方法** (`RenderType.java:42-44`):
```java
public static RenderType create(String name, RenderSetup state) {
    return new RenderType(name, state);
}
```

对比 1.20.1 的 7 参版 `create(name, format, mode, bufSize, affectsCrumbling, sortOnUpload, compositeState)`，26.1.2 简化为仅 2 参：`create(name, RenderSetup)`。所有状态维度已收敛到 RenderSetup + RenderPipeline。

### 2.2 RenderSetup (状态聚合体)

```java
// RenderSetup.java:23-35
public final class RenderSetup {
    final RenderPipeline pipeline;
    final Map<String, TextureBinding> textures;
    final TextureTransform textureTransform;
    final OutputTarget outputTarget;
    final RenderSetup.OutlineProperty outlineProperty;
    final boolean useLightmap;
    final boolean useOverlay;
    final boolean affectsCrumbling;
    final boolean sortOnUpload;
    final int bufferSize;
    final LayeringTransform layeringTransform;
```

全部字段 `final`，不可变。

### 2.3 RenderSetup.RenderSetupBuilder

```java
// RenderSetup.java:148-166 — 默认值
public static class RenderSetupBuilder {
    private final RenderPipeline pipeline;               // 构造时必须提供
    private boolean useLightmap = false;
    private boolean useOverlay = false;
    private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
    private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
    private TextureTransform textureTransform = TextureTransform.DEFAULT_TEXTURING;
    private boolean affectsCrumbling = false;
    private boolean sortOnUpload = false;
    private int bufferSize = 1536;
    private OutlineProperty outlineProperty = OutlineProperty.NONE;
    private final Map<String, TextureBinding> textures = new HashMap<>();
```

Builder 全部 API:
| 方法 | 说明 |
|---|---|
| `withTexture(name, Identifier)` | 绑定简单纹理 |
| `withTexture(name, Identifier, Supplier<GpuSampler>)` | 绑定可自定义采样器的纹理 |
| `useLightmap()` | 启用 lightmap 绑定 |
| `useOverlay()` | 启用 overlay 绑定 |
| `affectsCrumbling()` | 启用破坏动画影响 |
| `sortOnUpload()` | 启用上传排序 |
| `bufferSize(int)` | 设置缓冲区大小 |
| `setLayeringTransform(LayeringTransform)` | 设置分层变换 |
| `setOutputTarget(OutputTarget)` | 设置渲染目标 |
| `setTextureTransform(TextureTransform)` | 设置纹理矩阵变换 |
| `setOutline(OutlineProperty)` | 设置 outline 属性 |
| `createRenderSetup()` | 构建不可变 RenderSetup |

### 2.4 RenderPipeline (完整 GPU Pipeline 描述)

```java
// RenderPipeline.java:22-40
public class RenderPipeline {
    private final Identifier location;
    private final Identifier vertexShader;
    private final Identifier fragmentShader;
    private final ShaderDefines shaderDefines;
    private final List<String> samplers;
    private final List<UniformDescription> uniforms;
    private final @Nullable DepthStencilState depthStencilState;
    private final PolygonMode polygonMode;
    private final boolean cull;
    private final ColorTargetState colorTargetState;
    private final VertexFormat vertexFormat;
    private final VertexFormat.Mode vertexFormatMode;
    private final int sortKey;
    private final Optional<StencilTest> stencilTest;  // NeoForge 扩展
```

所有字段 `final` — Pipeline 构造后不可变。

**RenderPipeline.Snippet** (可组合片段):
```java
public record Snippet(
    Optional<Identifier> vertexShader,
    Optional<Identifier> fragmentShader,
    Optional<ShaderDefines> shaderDefines,
    Optional<List<String>> samplers,
    Optional<List<UniformDescription>> uniforms,
    Optional<ColorTargetState> colorTargetState,
    Optional<DepthStencilState> depthStencilState,
    Optional<PolygonMode> polygonMode,
    Optional<Boolean> cull,
    Optional<VertexFormat> vertexFormat,
    Optional<VertexFormat.Mode> vertexFormatMode,
    Optional<StencilTest> stencilTest
)
```

Snippet 合并语义: `Builder.withSnippet()` 仅在 snippet 字段 `isPresent()` 时覆盖 builder 相应字段。

### 2.5 ColorTargetState (color blend + write mask)

```java
// ColorTargetState.java:9
public record ColorTargetState(Optional<BlendFunction> blendFunction,
                              @WriteMask int writeMask) {
    public static final int WRITE_RED = 1;
    public static final int WRITE_GREEN = 2;
    public static final int WRITE_BLUE = 4;
    public static final int WRITE_ALPHA = 8;
    public static final int WRITE_ALL = 15;          // 0b1111
    public static final int WRITE_NONE = 0;
    public static final ColorTargetState DEFAULT = new ColorTargetState(Optional.empty(), 15);

    public ColorTargetState(BlendFunction blendFunction) {
        this(Optional.of(blendFunction), 15);
    }
}
```

### 2.6 BlendFunction (record)

```java
// BlendFunction.java:7-8
public record BlendFunction(SourceFactor sourceColor, DestFactor destColor,
                            SourceFactor sourceAlpha, DestFactor destAlpha) {
    public BlendFunction(SourceFactor source, DestFactor dest) {
        this(source, dest, source, dest);  // RGB 和 Alpha 相同时的简写
    }
```

| 常量 | sourceColor | destColor | sourceAlpha | destAlpha |
|---|---|---|---|---|
| `TRANSLUCENT` | SRC_ALPHA | ONE_MINUS_SRC_ALPHA | ONE | ONE_MINUS_SRC_ALPHA |
| `ADDITIVE` | ONE | ONE | ONE | ONE |
| `LIGHTNING` | SRC_ALPHA | ONE | SRC_ALPHA | ONE |
| `GLINT` | SRC_COLOR | ONE | ZERO | ONE |
| `OVERLAY` | SRC_ALPHA | ONE | ONE | ZERO |
| `INVERT` | ONE_MINUS_DST_COLOR | ONE_MINUS_SRC_COLOR | ONE | ZERO |
| `TRANSLUCENT_PREMULTIPLIED_ALPHA` | ONE | ONE_MINUS_SRC_ALPHA | ONE | ONE_MINUS_SRC_ALPHA |
| `ENTITY_OUTLINE_BLIT` | SRC_ALPHA | ONE_MINUS_SRC_ALPHA | ZERO | ONE |

### 2.7 DepthStencilState

```java
// DepthStencilState.java:5-6
public record DepthStencilState(CompareOp depthTest, boolean writeDepth,
                               float depthBiasScaleFactor, float depthBiasConstant) {
    public static final DepthStencilState DEFAULT =
        new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);

    public DepthStencilState(CompareOp depthTest, boolean depthWrite) {
        this(depthTest, depthWrite, 0.0F, 0.0F);
    }
}
```

### 2.8 OutputTarget

```java
// OutputTarget.java:14-18
public class OutputTarget {
    public static final OutputTarget MAIN_TARGET =
        new OutputTarget("main_target", () -> Minecraft.getInstance().getMainRenderTarget());
    public static final OutputTarget OUTLINE_TARGET =
        new OutputTarget("outline_target", () -> levelRenderer.entityOutlineTarget());
    public static final OutputTarget WEATHER_TARGET =
        new OutputTarget("weather_target", () -> levelRenderer.getWeatherTarget());
    public static final OutputTarget ITEM_ENTITY_TARGET =
        new OutputTarget("item_entity_target", () -> levelRenderer.getItemEntityTarget());
```

通过 `Supplier<@Nullable RenderTarget>` 懒绑定，`getRenderTarget()` fallback 到主目标。

### 2.9 TextureTransform（替代 TexturingStateShard)

```java
public class TextureTransform {
    public static final TextureTransform DEFAULT_TEXTURING =
        new TextureTransform("default_texturing", Matrix4f::new);
    public static final TextureTransform GLINT_TEXTURING =
        new TextureTransform("glint_texturing", () -> setupGlintTexturing(8.0F));
    public static final TextureTransform ENTITY_GLINT_TEXTURING =
        new TextureTransform("entity_glint_texturing", () -> setupGlintTexturing(0.5F));
    public static final TextureTransform ARMOR_ENTITY_GLINT_TEXTURING =
        new TextureTransform("armor_entity_glint_texturing", () -> setupGlintTexturing(0.16F));

    public Matrix4f getMatrix() { return this.supplier.get(); }
```

持有 `Supplier<Matrix4f>`，每次调用 `getMatrix()` 生成当前帧纹理矩阵（如光灵动画面）。

子类 `OffsetTextureTransform(float u, float v)` 提供固定偏移的纹理矩阵。

### 2.10 LayeringTransform（替代 LayeringStateShard)

```java
public class LayeringTransform {
    public static final LayeringTransform NO_LAYERING =
        new LayeringTransform("no_layering", null);
    public static final LayeringTransform VIEW_OFFSET_Z_LAYERING =
        new LayeringTransform("view_offset_z_layering",
            modelViewStack -> projectionType.applyLayeringTransform(modelViewStack, 1.0F));
    public static final LayeringTransform VIEW_OFFSET_Z_LAYERING_FORWARD =
        new LayeringTransform("view_offset_z_layering_forward",
            modelViewStack -> projectionType.applyLayeringTransform(modelViewStack, -1.0F));

    public @Nullable Consumer<Matrix4fStack> getModifier() { return this.modifier; }
```

`null` modifier 表示无操作。非 null 时在 `RenderType.draw()` 中执行 pushMatrix → modifier → draw → popMatrix。

---

## 3. 机制详解

### 3.1 `RenderType.draw(MeshData)` 完整渲染流程

```java
// RenderType.java:62-140
public void draw(MeshData mesh) {
    // 1. 可选分层变换 (push modelView)
    Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
    Consumer<Matrix4fStack> modelViewModifier = this.state.layeringTransform.getModifier();
    if (modelViewModifier != null) {
        modelViewStack.pushMatrix();
        modelViewModifier.accept(modelViewStack);
    }

    // 2. 写入 DynamicTransforms UBO (transform + color + texture transform)
    GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
        .writeTransform(RenderSystem.getModelViewMatrix(),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),  // color modulate
            new Vector3f(),                           // line width etc
            this.state.textureTransform.getMatrix());

    // 3. 解析纹理绑定
    Map<String, RenderSetup.TextureAndSampler> textures = this.state.getTextures();

    // 4. 上传顶点/索引缓冲
    GpuBuffer vertices = this.state.pipeline.getVertexFormat()
        .uploadImmediateVertexBuffer(mesh.vertexBuffer());
    // ... 索引缓冲处理 ...

    // 5. 解析 RenderTarget
    RenderTarget renderTarget = this.state.outputTarget.getRenderTarget();
    GpuTextureView colorTexture = renderTarget.getColorTextureView();
    GpuTextureView depthTexture = renderTarget.useDepth
        ? renderTarget.getDepthTextureView() : null;

    // 6. 创建 RenderPass + 设置 Pipeline
    try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Immediate draw for " + this.name,
                colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
        renderPass.setPipeline(this.state.pipeline);

        // 7. 绑定 Uniform / Vertex / Texture / Index
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", dynamicTransforms);
        renderPass.setVertexBuffer(0, vertices);
        for (entry : textures) {
            renderPass.bindTexture(entry.getKey(), entry.getValue().textureView(),
                                   entry.getValue().sampler());
        }
        renderPass.setIndexBuffer(indices, indexType);
        renderPass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
    }

    // 8. 恢复 modelView
    if (modelViewModifier != null) { modelViewStack.popMatrix(); }
}
```

流程步骤:
1. **Layering** — 如需要，推入 modelView 变换
2. **DynamicTransforms UBO** — 写入每帧动态数据
3. **纹理解析** — `getTextures()` 合并手动绑定的纹理 + lightmap(Sampler2) + overlay(Sampler1)
4. **Buffer 上传** — `uploadImmediateVertexBuffer` / `uploadImmediateIndexBuffer`
5. **RenderTarget 解析** — 通过 OutputTarget 获取 color/depth 纹理
6. **RenderPass 创建** — `createCommandEncoder().createRenderPass()`
7. **Pipeline 绑定** — `setPipeline` + uniforms + vertex + textures + index → `drawIndexed`
8. **清理** — pop modelView

> 关键：26.1.2 不再有 `setupRenderState()` / `clearRenderState()` 机制。所有状态在**构建时**固化到 `RenderPipeline`，运行时通过 `renderPass.setPipeline()` 一次设置。

### 3.2 Pipeline 注册与懒编译

```java
// RenderPipelines.java:736-739
private static RenderPipeline register(RenderPipeline pipeline) {
    PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
    return pipeline;
}
```

所有 pipeline 在静态初始化时注册到 `PIPELINES_BY_LOCATION` (HashMap<Identifier, RenderPipeline>)。

**懒编译** (通过 GlDevice):
- Pipeline 对象是 metadata 描述，不是编译后的 GPU pipeline
- 实际 GPU pipeline 在 `getOrCompilePipeline(pipeline)` 时编译
- 编译后的 pipeline 被缓存 (pipelineCache)

### 3.3 `Util.memoize` 缓存

与 1.20.1/1.21.1 相同 — `RenderTypes` 中的 `Function<Identifier, RenderType>` 工厂仍使用 `Util.memoize()` 懒缓存。

### 3.4 Snippet 组合模式

RenderPipelines 定义了一组 Snippet 常量作为**可组合构建块**:

```java
// 示例: ENTITY_SNIPPET 组合了多个子 snippet
public static final RenderPipeline.Snippet ENTITY_SNIPPET =
    RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withSampler("Sampler0")
        .withSampler("Sampler2")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
```

Pipeline 通过多层 Snippet 叠加构建:
```
MATRICES_PROJECTION_SNIPPET
    ↓
MATRICES_FOG_SNIPPET = MATRICES_PROJECTION_SNIPPET + FOG uniform
    ↓
MATRICES_FOG_LIGHT_DIR_SNIPPET = MATRICES_FOG_SNIPPET + Lighting uniform
    ↓
ENTITY_SNIPPET = MATRICES_FOG_LIGHT_DIR_SNIPPET + shader + samplers + format + depth
    ↓
ENTITY_TRANSLUCENT = ENTITY_SNIPPET + ALPHA_CUTOUT define + Translucent blend + cull=false
```

### 3.5 `RenderSetup.getTextures()` 纹理解析

```java
// RenderSetup.java:82-118
public Map<String, TextureAndSampler> getTextures() {
    if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap) {
        return Collections.emptyMap();
    }
    Map<String, TextureAndSampler> result = new HashMap<>();
    if (this.useOverlay) {
        result.put("Sampler1", new TextureAndSampler(
            overlayTexture().getTextureView(),
            getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
    }
    if (this.useLightmap) {
        result.put("Sampler2", new TextureAndSampler(
            lightmap(), getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
    }
    for (entry : this.textures.entrySet()) {
        AbstractTexture texture = textureManager.getTexture(entry.getValue().location);
        // ... 应用 sampler override
    }
    return result;
}
```

- Lightmap 总是绑定到 `Sampler2`
- Overlay 总是绑定到 `Sampler1`
- 用户纹理按 name 映射，支持 sampler override

### 3.6 Outline 自动派生机制

```java
// RenderType constructor
this.outline = state.outlineProperty == RenderSetup.OutlineProperty.AFFECTS_OUTLINE
    ? state.textures.values().stream().findFirst()
        .map(texture -> RenderTypes.OUTLINE.apply(texture.location(), state.pipeline.isCull()))
    : Optional.empty();
```

- `AFFECTS_OUTLINE` 时，取第一个纹理自动生成 outline RenderType
- Outline RenderType 使用 `OUTLINE_CULL` 或 `OUTLINE_NO_CULL` pipeline（取决于父 pipeline 是否 cull）

### 3.7 NeoForge RegisterRenderPipelinesEvent

```java
// RenderPipelines.java:743-751
public static void registerCustomPipelines() {
    var event = new RegisterRenderPipelinesEvent(pipeline -> {
        if (PIPELINES_BY_LOCATION.putIfAbsent(pipeline.getLocation(), pipeline) != null) {
            throw new IllegalStateException("Duplicate RenderPipeline registration");
        }
    });
    ModLoader.postEvent(event);
}
```

模组可通过 `RegisterRenderPipelinesEvent` 注册自定义 pipeline。

---

## 4. 静态工厂方法清单 (RenderTypes)

### 4.1 无参数

| 静态方法 | Pipeline | 关键特征 |
|---|---|---|
| `solidMovingBlock()` | SOLID_BLOCK | lightmap + BLOCK_SHEET sampler |
| `cutoutMovingBlock()` | CUTOUT_BLOCK | lightmap + BLOCK_SHEET sampler |
| `translucentMovingBlock()` | TRANSLUCENT_BLOCK | lightmap + sortOnUpload + ITEM_ENTITY_TARGET |
| `leash()` | LEASH | lightmap |
| `waterMask()` | WATER_MASK | 无纹理 |
| `armorEntityGlint()` | GLINT | ENCHANTED_GLINT_ARMOR + ENTITY_GLINT_TEXTURING + VIEW_OFFSET_Z_LAYERING |
| `glintTranslucent()` | GLINT | ENCHANTED_GLINT_ITEM + GLINT_TEXTURING + ITEM_ENTITY_TARGET |
| `glint()` | GLINT | ENCHANTED_GLINT_ITEM + GLINT_TEXTURING |
| `entityGlint()` | GLINT | ENCHANTED_GLINT_ITEM + ENTITY_GLINT_TEXTURING |
| `lightning()` | LIGHTNING | WEATHER_TARGET + sortOnUpload |
| `dragonRays()` | DRAGON_RAYS | 无纹理 |
| `dragonRaysDepth()` | DRAGON_RAYS_DEPTH | depth-only |
| `endPortal()` | END_PORTAL | 双纹理(END_SKY + END_PORTAL) |
| `endGateway()` | END_GATEWAY | 双纹理(END_SKY + END_PORTAL) |
| `lines()` | LINES | VIEW_OFFSET_Z_LAYERING + ITEM_ENTITY_TARGET |
| `linesTranslucent()` | LINES_TRANSLUCENT | VIEW_OFFSET_Z_LAYERING + ITEM_ENTITY_TARGET |
| `secondaryBlockOutline()` | SECONDARY_BLOCK_OUTLINE | VIEW_OFFSET_Z_LAYERING + ITEM_ENTITY_TARGET |
| `debugFilledBox()` | DEBUG_FILLED_BOX | sortOnUpload + VIEW_OFFSET_Z_LAYERING |
| `debugPoint()` | DEBUG_POINTS | 点图元 |
| `debugQuads()` | DEBUG_QUADS | sortOnUpload |
| `debugTriangleFan()` | DEBUG_TRIANGLE_FAN | sortOnUpload |
| `textBackground()` | TEXT_BACKGROUND | lightmap + sortOnUpload |
| `textBackgroundSeeThrough()` | TEXT_BACKGROUND_SEE_THROUGH | lightmap + sortOnUpload |

### 4.2 Function<Identifier, RenderType>

| 静态方法 | Pipeline | 关键特征 |
|---|---|---|
| `armorCutoutNoCull(id)` | ARMOR_CUTOUT_NO_CULL | lightmap + overlay + VIEW_OFFSET_Z + affectsCrumbling + AFFECTS_OUTLINE |
| `armorTranslucent(id)` | ARMOR_TRANSLUCENT | lightmap + overlay + VIEW_OFFSET_Z + affectsCrumbling + AFFECTS_OUTLINE |
| `entitySolid(id)` | ENTITY_SOLID | lightmap + overlay + affectsCrumbling + AFFECTS_OUTLINE |
| `entitySolidZOffsetForward(id)` | ENTITY_SOLID_Z_OFFSET_FORWARD | lightmap + overlay + VIEW_OFFSET_Z_FORWARD + affectsCrumbling |
| `entityCutoutCull(id)` | ENTITY_CUTOUT_CULL | lightmap + overlay + affectsCrumbling + AFFECTS_OUTLINE (cull ON) |
| `entityTranslucentCullItemTarget(id)` | ENTITY_TRANSLUCENT_CULL | ITEM_ENTITY_TARGET + lightmap + overlay |
| `itemCutout(id)` | ITEM_CUTOUT | lightmap + affectsCrumbling (无 overlay) |
| `itemTranslucent(id)` | ITEM_TRANSLUCENT | ITEM_ENTITY_TARGET + lightmap + affectsCrumbling + sortOnUpload |
| `endCrystalBeam(id)` | END_CRYSTAL_BEAM | lightmap (无 overlay) |
| `bannerPattern(id)` | BANNER_PATTERN | lightmap + sortOnUpload |
| `entityShadow(id)` | ENTITY_SHADOW | lightmap + overlay + VIEW_OFFSET_Z |
| `eyes(id)` | EYES | sortOnUpload |
| `crumbling(id)` | CRUMBLING | sortOnUpload |
| `text(id)` | TEXT | lightmap + 786432 bufferSize |
| `textIntensity(id)` | TEXT_INTENSITY | lightmap + 786432 bufferSize |
| `textPolygonOffset(id)` | TEXT_POLYGON_OFFSET | lightmap + sortOnUpload |
| `textIntensityPolygonOffset(id)` | TEXT_INTENSITY | lightmap + sortOnUpload |
| `textSeeThrough(id)` | TEXT_SEE_THROUGH | lightmap |
| `textIntensitySeeThrough(id)` | TEXT_INTENSITY_SEE_THROUGH | lightmap + sortOnUpload |
| `blockScreenEffect(id)` | BLOCK_SCREEN_EFFECT | GUI_TEXTURED |
| `fireScreenEffect(id)` | FIRE_SCREEN_EFFECT | GUI_TEXTURED |

### 4.3 BiFunction<Identifier, Boolean, RenderType>

| 静态方法 | Boolean 参数 | Pipeline |
|---|---|---|
| `entityCutout(id, affectsOutline)` | affectsOutline → AFFECTS_OUTLINE or NONE | ENTITY_CUTOUT |
| `entityCutoutZOffset(id, affectsOutline)` | 同上 + VIEW_OFFSET_Z | ENTITY_CUTOUT_Z_OFFSET |
| `entityTranslucent(id, affectsOutline)` | 同上 | ENTITY_TRANSLUCENT |
| `entityTranslucentEmissive(id, affectsOutline)` | 同上 (无 lightmap) | ENTITY_TRANSLUCENT_EMISSIVE |
| `beaconBeam(id, translucent)` | translucent → BEACON_BEAM_TRANSLUCENT or OPAQUE | BEACON_BEAM_TRANSLUCENT / BEACON_BEAM_OPAQUE |

### 4.4 特殊参数工厂

| 静态方法 | 额外参数 | 特征 |
|---|---|---|
| `entityCutoutDissolve(id, maskId)` | Identifier maskTexture | DISSOLVE shader define |
| `breezeWind(id, u, v)` | float u, float v | OffsetTextureTransform + BREEZE_WIND pipeline |
| `energySwirl(id, u, v)` | float u, float v | OffsetTextureTransform + ENERGY_SWIRL pipeline |
| `outline(id)` | — | OUTLINE pipeline (OUTLINE_NO_CULL) |
| `breezeEyes(id)` | — | entityTranslucentEmissive(id, false) |

### 4.5 createArmorDecalCutoutNoCull (公开扩展方法)

```java
public static RenderType createArmorDecalCutoutNoCull(Identifier texture) {
    return RenderType.create("armor_decal_cutout_no_cull",
        RenderSetup.builder(RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL)
            .withTexture("Sampler0", texture)
            .useLightmap().useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup());
}
```

---

## 5. RenderPipeline 完整常量清单

### 5.1 SNIPPET 常量 (构建块)

| Snippet | 包含的 Uniforms / 状态 |
|---|---|
| `MATRICES_PROJECTION_SNIPPET` | DynamicTransforms + Projection UBO |
| `FOG_SNIPPET` | Fog UBO |
| `GLOBALS_SNIPPET` | Globals UBO |
| `MATRICES_FOG_SNIPPET` | MATRICES_PROJECTION + FOG |
| `MATRICES_FOG_LIGHT_DIR_SNIPPET` | MATRICES_FOG + Lighting UBO |
| `GENERIC_BLOCKS_SNIPPET` | FOG + Sampler0/Sampler2 + BLOCK format + DEFAULT depth |
| `TERRAIN_SNIPPET` | GENERIC_BLOCKS + Projection + ChunkSection + core/terrain shaders |
| `BLOCK_SNIPPET` | GENERIC_BLOCKS + MATRICES_PROJECTION + core/block shaders |
| `ENTITY_SNIPPET` | MATRICES_FOG_LIGHT_DIR + core/entity shaders + Sampler0/2 + ENTITY format |
| `ENTITY_EMISSIVE_SNIPPET` | 类似 ENTITY + EMISSIVE define, 无 Sampler2 |
| `BEACON_BEAM_SNIPPET` | MATRICES_FOG + core/rendertype_beacon_beam + BLOCK format |
| `ITEM_SNIPPET` | MATRICES_FOG_LIGHT_DIR + core/item shaders + ENTITY format |
| `TEXT_SNIPPET` | MATRICES_PROJECTION + TRANSLUCENT blend + POSITION_COLOR_TEX_LIGHTMAP format |
| `END_PORTAL_SNIPPET` | MATRICES_PROJECTION + FOG + GLOBALS + core/rendertype_end_portal + POSITION format |
| `CLOUDS_SNIPPET` | MATRICES_FOG + core/rendertype_clouds + CloudInfo/CloudFaces uniforms |
| `LINES_SNIPPET` | MATRICES_FOG + GLOBALS + core/rendertype_lines + POSITION_COLOR_NORMAL_LINE_WIDTH + TRANSLUCENT blend + cull=false |
| `DEBUG_FILLED_SNIPPET` | MATRICES_PROJECTION + core/position_color + TRANSLUCENT + POSITION_COLOR format |
| `PARTICLE_SNIPPET` | MATRICES_FOG + core/particle + PARTICLE format |
| `WEATHER_SNIPPET` | PARTICLE + TRANSLUCENT blend + cull=false |
| `GUI_SNIPPET` | MATRICES_PROJECTION + core/gui + TRANSLUCENT + POSITION_COLOR format |
| `GUI_TEXTURED_SNIPPET` | MATRICES_PROJECTION + core/position_tex_color + TRANSLUCENT + POSITION_TEX_COLOR format |
| `GUI_TEXT_SNIPPET` | TEXT_SNIPPET + no depthState |
| `OUTLINE_SNIPPET` | MATRICES_PROJECTION + core/rendertype_outline + POSITION_TEX_COLOR format |
| `POST_PROCESSING_SNIPPET` | EMPTY format + TRIANGLES mode |
| `ANIMATE_SPRITE_SNIPPET` | core/animate_sprite + SpriteAnimationInfo UBO + EMPTY format |

### 5.2 ENTITY_* Pipelines

| Pipeline 常量 | 位置 (location) | 基于 Snippet | 特殊 define |
|---|---|---|---|
| `ENTITY_SOLID` | pipeline/entity_solid | ENTITY_SNIPPET + Sampler1 | — |
| `ENTITY_SOLID_Z_OFFSET_FORWARD` | pipeline/entity_solid_offset_forward | ENTITY_SNIPPET + Sampler1 | — |
| `ENTITY_CUTOUT_CULL` | pipeline/entity_cutout_cull | ENTITY_SNIPPET + Sampler1 | ALPHA_CUTOUT(0.1) |
| `ENTITY_CUTOUT` | pipeline/entity_cutout | ENTITY_SNIPPET + Sampler1 | ALPHA_CUTOUT(0.1), PER_FACE_LIGHTING, cull=false |
| `ENTITY_CUTOUT_Z_OFFSET` | pipeline/entity_cutout_z_offset | ENTITY_SNIPPET + Sampler1 | ALPHA_CUTOUT(0.1), PER_FACE_LIGHTING, cull=false |
| `ENTITY_CUTOUT_DISSOLVE` | pipeline/entity_cutout_dissolve | ENTITY_SNIPPET + Sampler1 + DissolveMaskSampler | ALPHA_CUTOUT(0.1), PER_FACE_LIGHTING, DISSOLVE, cull=false |
| `ENTITY_TRANSLUCENT` | pipeline/entity_translucent | ENTITY_SNIPPET + Sampler1 | ALPHA_CUTOUT(0.1), PER_FACE_LIGHTING, TRANSLUCENT blend, cull=false |
| `ENTITY_TRANSLUCENT_EMISSIVE` | pipeline/entity_translucent_emissive | ENTITY_EMISSIVE_SNIPPET + Sampler1 | ALPHA_CUTOUT(0.1), PER_FACE_LIGHTING, TRANSLUCENT, cull=false, LEQUAL depth |
| `ENTITY_TRANSLUCENT_CULL` | pipeline/entity_translucent_cull | ENTITY_SNIPPET + Sampler1 | ALPHA_CUTOUT(0.1), TRANSLUCENT blend |
| `ENTITY_SHADOW` | pipeline/entity_shadow | MATRICES_FOG + core/rendertype_entity_shadow | TRANSLUCENT blend, ENTITY format, LEQUAL depth(no write) |

### 5.3 ARMOR_* Pipelines

| Pipeline | 位置 | 特殊 define |
|---|---|---|
| `ARMOR_CUTOUT_NO_CULL` | pipeline/armor_cutout_no_cull | ALPHA_CUTOUT(0.1), NO_OVERLAY, PER_FACE_LIGHTING, cull=false |
| `ARMOR_DECAL_CUTOUT_NO_CULL` | pipeline/armor_decal_cutout_no_cull | ALPHA_CUTOUT(0.1), NO_OVERLAY, PER_FACE_LIGHTING, cull=false, EQUAL depth(no write) |
| `ARMOR_TRANSLUCENT` | pipeline/armor_translucent | ALPHA_CUTOUT(0.1), NO_OVERLAY, PER_FACE_LIGHTING, TRANSLUCENT, cull=false |

### 5.4 BLOCK / TERRAIN Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `SOLID_BLOCK` | pipeline/solid_block | BLOCK_SNIPPET |
| `SOLID_TERRAIN` | pipeline/solid_terrain | TERRAIN_SNIPPET |
| `WIREFRAME` | pipeline/wireframe | TERRAIN + WIREFRAME polygonMode |
| `CUTOUT_BLOCK` | pipeline/cutout_block | BLOCK + ALPHA_CUTOUT(0.5) |
| `CUTOUT_TERRAIN` | pipeline/cutout_terrain | TERRAIN + ALPHA_CUTOUT(0.5) |
| `TRANSLUCENT_TERRAIN` | pipeline/translucent_terrain | TERRAIN + TRANSLUCENT + ALPHA_CUTOUT(0.01) |
| `TRANSLUCENT_BLOCK` | pipeline/translucent_block | BLOCK + TRANSLUCENT + ALPHA_CUTOUT(0.01) |

### 5.5 BEACON / BANNER / ENERGY / EYES Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `BEACON_BEAM_OPAQUE` | pipeline/beacon_beam_opaque | BEACON_BEAM_SNIPPET |
| `BEACON_BEAM_TRANSLUCENT` | pipeline/beacon_beam_translucent | BEACON_BEAM + TRANSLUCENT + LEQUAL depth(no write) |
| `BANNER_PATTERN` | pipeline/banner_pattern | ENTITY + NO_OVERLAY + TRANSLUCENT + LEQUAL depth(no write) |
| `ENERGY_SWIRL` | pipeline/energy_swirl | MATRICES_FOG + core/entity + EMISSIVE + NO_OVERLAY + NO_CARDINAL_LIGHTING + ADDITIVE + cull=false |
| `EYES` | pipeline/eyes | MATRICES_FOG + core/entity + EMISSIVE + NO_OVERLAY + NO_CARDINAL_LIGHTING + TRANSLUCENT + LEQUAL(no write) |
| `BREEZE_WIND` | pipeline/breeze_wind | ENTITY + ALPHA_CUTOUT(0.1) + APPLY_TEXTURE_MATRIX + NO_OVERLAY + NO_CARDINAL_LIGHTING + TRANSLUCENT + cull=false |
| `END_CRYSTAL_BEAM` | pipeline/end_crystal_beam | ENTITY + ALPHA_CUTOUT(0.1) + NO_OVERLAY + cull=false |

### 5.6 LINES / LIGHTNING / DRAGON_RAYS Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `LINES` | pipeline/lines | LINES_SNIPPET |
| `LINES_TRANSLUCENT` | pipeline/lines_translucent | LINES + LEQUAL depth(no write) |
| `LINES_DEPTH_BIAS` | pipeline/lines_depth_bias | LINES + LEQUAL + depthBias(-1,-1) |
| `SECONDARY_BLOCK_OUTLINE` | pipeline/secondary_block_outline | LINES + TRANSLUCENT + LEQUAL(no write) |
| `LIGHTNING` | pipeline/lightning | MATRICES_FOG + core/rendertype_lightning + LIGHTNING blend + POSITION_COLOR format |
| `DRAGON_RAYS` | pipeline/dragon_rays | MATRICES_FOG + LIGHTNING blend + POSITION_COLOR + TRIANGLES + LEQUAL(no write) |
| `DRAGON_RAYS_DEPTH` | pipeline/dragon_rays_depth | MATRICES_FOG + core/position + no blend + POSITION format + TRIANGLES |

### 5.7 TEXT / GUI Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `TEXT` | pipeline/text | TEXT_SNIPPET + FOG + core/rendertype_text + Sampler0/2 |
| `GUI_TEXT` | pipeline/gui_text | GUI_TEXT_SNIPPET + FOG + core/rendertype_text |
| `TEXT_BACKGROUND` | pipeline/text_background | TEXT_SNIPPET + FOG + core/rendertype_text_background + POSITION_COLOR_LIGHTMAP |
| `TEXT_INTENSITY` | pipeline/text_intensity | TEXT_SNIPPET + FOG + core/rendertype_text_intensity + LEQUAL+depthBias(-1,-10) |
| `GUI_TEXT_INTENSITY` | pipeline/gui_text_intensity | GUI_TEXT_SNIPPET + FOG + core/rendertype_text_intensity |
| `TEXT_POLYGON_OFFSET` | pipeline/text_polygon_offset | TEXT_SNIPPET + FOG + core/rendertype_text + LEQUAL+depthBias(-1,-10) |
| `TEXT_SEE_THROUGH` | pipeline/text_see_through | TEXT_SNIPPET + core/rendertype_text_see_through + no depth |
| `TEXT_BACKGROUND_SEE_THROUGH` | pipeline/text_background_see_through | TEXT_SNIPPET + no depth |
| `TEXT_INTENSITY_SEE_THROUGH` | pipeline/text_intensity_see_through | TEXT_SNIPPET + no depth |
| `GUI` | pipeline/gui | GUI_SNIPPET |
| `GUI_INVERT` | pipeline/gui_invert | GUI + INVERT blend |
| `GUI_TEXT_HIGHLIGHT` | pipeline/gui_text_highlight | GUI + ADDITIVE blend |
| `GUI_TEXTURED` | pipeline/gui_textured | GUI_TEXTURED_SNIPPET |
| `GUI_TEXTURED_PREMULTIPLIED_ALPHA` | pipeline/gui_textured_premultiplied_alpha | GUI_TEXTURED + TRANSLUCENT_PREMULTIPLIED |

### 5.8 EFFECT / SCREEN Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `BLOCK_SCREEN_EFFECT` | pipeline/block_screen_effect | GUI_TEXTURED |
| `FIRE_SCREEN_EFFECT` | pipeline/fire_screen_effect | GUI_TEXTURED |
| `GUI_OPAQUE_TEXTURED_BACKGROUND` | pipeline/gui_opaque_textured_background | GUI_TEXTURED + no blend + writeMask=15 |
| `GUI_NAUSEA_OVERLAY` | pipeline/gui_nausea_overlay | GUI_TEXTURED + ADDITIVE |
| `VIGNETTE` | pipeline/vignette | GUI_TEXTURED + VIGNETTE blend(ZERO, 1-SRC_COLOR, ZERO, ONE) |
| `CROSSHAIR` | pipeline/crosshair | GUI_TEXTURED + INVERT |
| `MOJANG_LOGO` | pipeline/mojang_logo | GUI_TEXTURED + SRC_ALPHA/ONE blend |
| `ENTITY_OUTLINE_BLIT` | pipeline/entity_outline_blit | screenquad + blit_screen + ENTITY_OUTLINE_BLIT blend |
| `TRACY_BLIT` | pipeline/tracy_blit | screenquad + blit_screen |

### 5.9 GLINT / CRUMBLING / OUTLINE / LEASH Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `GLINT` | pipeline/glint | MATRICES_PROJECTION + FOG + GLOBALS + core/glint + GLINT blend + cull=false + POSITION_TEX + EQUAL depth(no write) |
| `CRUMBLING` | pipeline/crumbling | MATRICES_PROJECTION + core/rendertype_crumbling + DST_COLOR/SRC_COLOR blend + BLOCK format + LEQUAL+depthBias(-1,-10) |
| `OUTLINE_CULL` | pipeline/outline_cull | OUTLINE_SNIPPET + cull=true |
| `OUTLINE_NO_CULL` | pipeline/outline_no_cull | OUTLINE_SNIPPET + cull=false |
| `LEASH` | pipeline/leash | MATRICES_FOG + core/rendertype_leash + Sampler2 + cull=false + POSITION_COLOR_LIGHTMAP + TRIANGLE_STRIP |
| `WATER_MASK` | pipeline/water_mask | MATRICES_PROJECTION + core/rendertype_water_mask + TRANSLUCENT blend(writeMask=0) + POSITION format |

### 5.10 SKY / WEATHER / PARTICLE / CLOUDS / WORLD_BORDER Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `SKY` | pipeline/sky | MATRICES_FOG + core/sky + POSITION + TRIANGLE_FAN |
| `END_SKY` | pipeline/end_sky | MATRICES_PROJECTION + core/position_tex_color + TRANSLUCENT + POSITION_TEX_COLOR |
| `SUNRISE_SUNSET` | pipeline/sunrise_sunset | MATRICES_PROJECTION + core/position_color + TRANSLUCENT + POSITION_COLOR + TRIANGLE_FAN |
| `STARS` | pipeline/stars | MATRICES_PROJECTION + core/stars + OVERLAY blend + POSITION |
| `CELESTIAL` | pipeline/celestial | MATRICES_PROJECTION + core/position_tex + OVERLAY + POSITION_TEX |
| `OPAQUE_PARTICLE` | pipeline/opaque_particle | PARTICLE_SNIPPET |
| `TRANSLUCENT_PARTICLE` | pipeline/translucent_particle | PARTICLE + TRANSLUCENT blend |
| `WEATHER_DEPTH_WRITE` | pipeline/weather_depth_write | WEATHER_SNIPPET |
| `WEATHER_NO_DEPTH_WRITE` | pipeline/weather_no_depth_write | WEATHER + LEQUAL(no write) |
| `FLAT_CLOUDS` | pipeline/flat_clouds | CLOUDS + cull=false |
| `CLOUDS` | pipeline/clouds | CLOUDS_SNIPPET |
| `WORLD_BORDER` | pipeline/world_border | MATRICES_PROJECTION + core/rendertype_world_border + OVERLAY blend + cull=false + LEQUAL+depthBias(-3,-3) |

### 5.11 DEBUG Pipelines

| Pipeline | 位置 | 特征 |
|---|---|---|
| `DEBUG_POINTS` | pipeline/debug_points | MATRICES_PROJECTION + core/debug_point + cull=false + POINTS |
| `DEBUG_FILLED_BOX` | pipeline/debug_filled_box | DEBUG_FILLED_SNIPPET |
| `DEBUG_QUADS` | pipeline/debug_quads | DEBUG_FILLED + cull=false |
| `DEBUG_TRIANGLE_FAN` | pipeline/debug_triangle_fan | DEBUG_FILLED + cull=false + TRIANGLE_FAN |

### 5.12 特殊 Pipelines

| Pipeline | 位置 | 用途 |
|---|---|---|
| `PANORAMA` | pipeline/panorama | MATRICES_PROJECTION + core/panorama + POSITION |
| `LIGHTMAP` | pipeline/lightmap | screenquad + core/lightmap + LightmapInfo UBO |
| `ANIMATE_SPRITE_BLIT` | pipeline/animate_sprite_blit | ANIMATE_SPRITE + core/animate_sprite_blit |
| `ANIMATE_SPRITE_INTERPOLATE` | pipeline/animate_sprite_interpolate | ANIMATE_SPRITE + core/animate_sprite_interpolate + 双纹理 |

### 5.13 Pipeline 注册与查询

```java
// 获取所有静态 pipeline
public static List<RenderPipeline> getStaticPipelines() {
    return PIPELINES_BY_LOCATION.values().stream().toList();
}

// 模组注册自定义 pipeline (通过 NeoForge Event)
public static void registerCustomPipelines() { ... }
```

---

## 6. 与其他子系统的交互

### 6.1 GlDevice (Pipeline 编译)

- Pipeline 对象是 metadata 描述
- `GlDevice.getOrCompilePipeline(pipeline)` 执行实际 GPU pipeline 编译
- 编译结果缓存在 `pipelineCache` 中
- Pipeline 按 `location` (Identifier) 索引

### 6.2 RenderPass

- `RenderType.draw()` 通过 `RenderSystem.getDevice().createCommandEncoder().createRenderPass()` 创建 RenderPass
- `renderPass.setPipeline(this.state.pipeline)` 一次性设置全部 GPU 状态
- Uniform 通过 `setUniform(name, bufferSlice)` 动态绑定

### 6.3 DynamicUniforms

- `RenderSystem.getDynamicUniforms()` 返回每帧动态 UBO 写入器
- `writeTransform(modelView, colorFactor, lineInfo, textureMatrix)` 组合写入
- 结果作为 `GpuBufferSlice` 传给 RenderPass

### 6.4 纹理系统

- `TextureManager` 管理纹理生命周期
- `RenderSetup.getTextures()` 在每次 `draw()` 调用时解析纹理绑定
- Lightmap 和 Overlay 纹理通过约定名称 (`Sampler2`, `Sampler1`) 绑定
- 支持 `Supplier<GpuSampler>` 覆写采样器配置

### 6.5 RenderTarget 系统

- `OutputTarget` 通过 `Supplier<RenderTarget>` 懒获取目标 FBO
- `getRenderTarget()` 有 fallback：若 supplier 返回 null，回退到 `getMainRenderTarget()`
- `draw()` 中从 `renderTarget` 提取 `colorTexture` 和 `depthTexture` 传给 `RenderPass`

---

## 7. 关键不变量与约束

1. **RenderType 不可变**: 构造后所有状态通过 `RenderSetup` + `RenderPipeline` 固化
2. **RenderPipeline 不可变**: 所有字段 `final`，在 `build()` 时验证强制字段完整
3. **Snippet 不可变 + 单向合并**: `Snippet` 是 record，`withSnippet()` 只覆盖非空字段
4. **Pipeline 全局注册**: 所有 pipeline 通过 `PIPELINES_BY_LOCATION` 全局 HashMap 索引，location 唯一
5. **Builder → Snippet → Builder 可逆**: `toBuilder()` 可将已构建的 Pipeline 转为 Builder 重新修改
6. **setupRenderState/clearRenderState 消失**: 状态在构建时固化到 pipeline，运行时通过 setPipeline 一次设置
7. **State shard 维度被重新分配**: 13 个 CompositeState 维度 → RenderPipeline (GPU 固定状态) + RenderSetup (CPU 侧可动态选择)
8. **Stencil 由 NeoForge 扩展**: `StencilTest` 通过 NeoForge 的 `withStencilTest()` / `withoutStencilTest()` Builder API 支持
9. **sortKey 种子随机化**: `updateSortKeySeed()` 在 `DEBUG_SHUFFLE_UI_RENDERING_ORDER` 时随机化排序
10. **Pipeline 构建强制验证**: `build()` 检查 location/vertexShader/fragmentShader/vertexFormat/vertexFormatMode 非空
11. **纹理绑定使用约定名称**: `Sampler0` = 主纹理, `Sampler1` = overlay, `Sampler2` = lightmap
