# 26.1.2 (NeoForge) Minecraft RenderTarget / Framebuffer / 后处理链 分析报告

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [架构概览: GL → GpuDevice 抽象层](#1-架构概览-gl--gpudevice-抽象层)
2. [RenderTarget.java — GpuTexture 化](#2-rendertargetjava--gputexture-化)
3. [MainTarget.java — GpuDevice 分配](#3-maintargetjava--gpudevice-分配)
4. [RenderTargetDescriptor — 资源描述符](#4-rendertargetdescriptor--资源描述符)
5. [RenderPipeline 和 RenderPipelines — Shader 管线](#5-renderpipeline-和-renderpipelines--shader-管线)
6. [PostChainConfig — Codec 驱动配置](#6-postchainconfig--codec-驱动配置)
7. [PostPass.java — RenderPass 驱动](#7-postpassjava--renderpass-驱动)
8. [PostChain.java — FrameGraph 驱动](#8-postchainjava--framegraph-驱动)
9. [GameRenderer — PostChain 实例和流程](#9-gamerenderer--postchain-实例和流程)
10. [LevelTargetBundle — 多 Target 标识](#10-leveltargetbundle--多-target-标识)
11. [ShaderManager — PostChain 管理](#11-shadermanager--postchain-管理)
12. [FrameGraphBuilder — 帧图调度](#12-framegraphbuilder--帧图调度)

---

## 1. 架构概览: GL → GpuDevice 抽象层

26.1.2 是最重大的一次重构。1.20.1/1.21.1 基于 **OpenGL 直接调用** (GlStateManager + 硬编码 GL 常量),26.1.2 引入了基于 **GpuDevice** 的现代 GPU 抽象层:

| 概念 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| Framebuffer | RenderTarget (FBO + GL Texture) | RenderTarget (GpuTexture + GpuTextureView) |
| Shader Program | EffectInstance (Effect+Program) | 不再存在,由 RenderPipeline 替代 |
| Render Pass | PostPass.process() 直接 GL 调用 | PostPass 通过 RenderPass 录制命令 |
| 资源生命周期 | 手动管理 (destroyBuffers) | FrameGraph + GraphicsResourceAllocator |
| 后处理链执行 | PostChain.process() 立即执行 | PostChain.addToFrame() 构建 FrameGraph |
| 最终输出 | MainTarget.blitToScreen() | CommandEncoder.presentTexture() |
| Shader 配置 | JSON Gson 解析 | Codec (DataFixerUpper) |
| 纹理 | GL texture ID (int) | GpuTexture / GpuTextureView 句柄 |
| Uniform | Uniform (GL location) | UniformBuffer (GpuBuffer) |

**关键: `EffectInstance` 类在 26.1.2 中不存在。**

---

## 2. RenderTarget.java — GpuTexture 化

**文件**: `com/mojang/blaze3d/pipeline/RenderTarget.java` (138 行,比 1.21.1 减少 156 行)

### 2.1 字段完全重写

```java
// 1.21.1 (GL):
public int frameBufferId;           // FBO ID
protected int colorTextureId;       // GL texture ID
protected int depthBufferId;        // GL texture ID
public int filterMode;              // GL_NEAREST/GL_LINEAR
private final float[] clearChannels;// 清屏颜色

// 26.1.2 (GpuDevice):
protected @Nullable GpuTexture colorTexture;        // GPU 纹理句柄
protected @Nullable GpuTextureView colorTextureView; // GPU 纹理视图
protected @Nullable GpuTexture depthTexture;        // 深度纹理句柄
protected @Nullable GpuTextureView depthTextureView; // 深度纹理视图
protected final String label;                       // 调试标签
public final boolean useStencil;                   // 模板支持(构造时指定)
```

### 2.2 构造

```java
// 两个构造器:
public RenderTarget(@Nullable String label, boolean useDepth)
public RenderTarget(@Nullable String label, boolean useDepth, boolean useStencil)
```

- `label` 空时自动生成 "FBO N"。
- `useStencil && !useDepth` → 抛 IllegalArgumentException("Stencil can only be enabled if depth is enabled")。
- 不再有 `frameBufferId/colorTextureId/depthBufferId` 初始化为 -1 的逻辑。

### 2.3 createBuffers — GpuDevice 创建纹理 (第 84–101 行)

```java
public void createBuffers(int width, int height) {
    RenderSystem.assertOnRenderThread();
    GpuDevice device = RenderSystem.getDevice();
    int maxTextureSize = device.getMaxTextureSize();
    // 尺寸检查...

    if (this.useDepth) {
        var format = this.useStencil ? ClientHooks.getStencilFormat() : TextureFormat.DEPTH32;
        this.depthTexture = device.createTexture(() -> this.label + " / Depth",
            15,         // 未知标志位(类似 usage flags)
            format, width, height, 1, 1);
        this.depthTextureView = device.createTextureView(this.depthTexture);
    }

    this.colorTexture = device.createTexture(() -> this.label + " / Color",
        15, TextureFormat.RGBA8, width, height, 1, 1);
    this.colorTextureView = device.createTextureView(this.colorTexture);
}
```

- `createTexture` 参数: label 供应商、标志位(15)、格式、宽、高、mip 层级、array 层级。

### 2.4 blitToScreen → presentTexture (第 104–110 行)

```java
public void blitToScreen() {
    if (this.colorTexture == null) {
        throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
    } else {
        RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTextureView);
    }
}
```

- 不再手动绘制全屏四边形。直接通过 `CommandEncoder.presentTexture` 将纹理提交到 swapchain。

### 2.5 新增 blitAndBlendToTexture (第 112–121 行)

```java
public void blitAndBlendToTexture(GpuTextureView output) {
    RenderSystem.assertOnRenderThread();
    try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Blit render target", output, OptionalInt.empty())) {
        renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture("InSampler", this.colorTextureView,
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        renderPass.draw(0, 3);
    }
}
```

- 用于 entity outline blit,通过 RenderPass 录制。

### 2.6 访问器

```java
public @Nullable GpuTexture getColorTexture()        // → colorTexture
public @Nullable GpuTextureView getColorTextureView() // → colorTextureView
public @Nullable GpuTexture getDepthTexture()        // → depthTexture
public @Nullable GpuTextureView getDepthTextureView() // → depthTextureView
```

- 移除了 `getColorTextureId()` / `getDepthTextureId()`。

### 2.7 移除的方法

- `bindRead()` / `unbindRead()` / `bindWrite()` / `unbindWrite()`
- `clear()` / `setClearColor()` / `setFilterMode()` / `checkStatus()`
- `blitToScreen(int width, int height)` / `blitToScreen(int width, int height, boolean)`
- `enableStencil()` / `isStencilEnabled()` (stencil 现在构造时指定)

### 2.8 copyDepthFrom (第 71–82 行)

```java
public void copyDepthFrom(RenderTarget source) {
    RenderSystem.getDevice()
        .createCommandEncoder()
        .copyTextureToTexture(source.depthTexture, this.depthTexture, 0, 0, 0, 0, 0,
            this.width, this.height);
}
```

- 通过 `CommandEncoder.copyTextureToTexture` 替代 GL `glBlitFrameBuffer`。

---

## 3. MainTarget.java — GpuDevice 分配

**文件**: `com/mojang/blaze3d/pipeline/MainTarget.java` (138 行)

### 3.1 构造支持 stencil

```java
public MainTarget(int desiredWidth, int desiredHeight)                  // useStencil=false
public MainTarget(int desiredWidth, int desiredHeight, boolean enableStencil)
```

### 3.2 allocateAttachments 重写 (第 39–79 行)

- 不再使用 GL 错误码 (`GL_OUT_OF_MEMORY = 1285`)。
- 尝试分配 color + depth,GpuOutOfMemoryException 异常 → 回退 DEFAULT → 再试。
- 分配成功后创建 `GpuTextureView`:
```java
this.colorTextureView = RenderSystem.getDevice().createTextureView(this.colorTexture);
this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
```

### 3.3 allocateColorAttachment / allocateDepthAttachment

```java
private @Nullable GpuTexture allocateColorAttachment(MainTarget.Dimension dimension) {
    try {
        return RenderSystem.getDevice().createTexture(
            () -> this.label + " / Color", 15, TextureFormat.RGBA8,
            dimension.width, dimension.height, 1, 1);
    } catch (GpuOutOfMemoryException var3) {
        return null;  // 回退
    }
}

private @Nullable GpuTexture allocateDepthAttachment(MainTarget.Dimension dimension) {
    try {
        var format = this.useStencil ? ClientHooks.getStencilFormat() : TextureFormat.DEPTH32;
        return RenderSystem.getDevice().createTexture(
            () -> this.label + " / Depth", 15, format,
            dimension.width, dimension.height, 1, 1);
    } catch (GpuOutOfMemoryException var3) {
        return null;
    }
}
```

---

## 4. RenderTargetDescriptor — 资源描述符

**文件**: `com/mojang/blaze3d/resource/RenderTargetDescriptor.java` (42 行)

```java
public record RenderTargetDescriptor(
    int width, int height, boolean useDepth, int clearColor, boolean useStencil
) implements ResourceDescriptor<RenderTarget>
```

- 实现 `ResourceDescriptor<RenderTarget>`,供 FrameGraph 管理 RenderTarget 资源池。
- `allocate()`: 创建 `TextureTarget(null, width, height, useDepth, useStencil)`。
- `prepare()`: 通过 CommandEncoder 清理纹理。
- `free()`: 调用 `destroyBuffers()`。
- `canUsePhysicalResource()`: 检查尺寸/depth/stencil 匹配。

---

## 5. RenderPipeline 和 RenderPipelines — Shader 管线

**文件**: `net/minecraft/client/renderer/RenderPipelines.java` (787 行)

### 5.1 POST_PROCESSING_SNIPPET (第 152–154 行)

```java
public static final RenderPipeline.Snippet POST_PROCESSING_SNIPPET = RenderPipeline.builder()
    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
    .buildSnippet();
```

- 后处理管线使用 `EMPTY` 顶点格式 + `TRIANGLES` 模式 (后处理是全屏三角形,无顶点数据需显式传入)。
- 与普通渲染不同: 无矩阵投影 snippet,无 fog,无光照。

### 5.2 ENTITY_OUTLINE_BLIT

```java
public static final RenderPipeline ENTITY_OUTLINE_BLIT = register(
    RenderPipeline.builder(OUTLINE_SNIPPET)
        .withLocation("pipeline/entity_outline_blit")
        .build()
);
```

### 5.3 RenderPipeline 系统

- **Snippet**: 可组合的管线片段(Uniforms、Samplers、VertexFormat、DepthStencil、ColorTarget)。
- **Pipeline**: 由 Snippet 组装 + vertex/fragment shader + location。
- **Builder**: 支持 `withUniform()`, `withSampler()`, `withShaderDefine()`, `withVertexShader()`, `withFragmentShader()` 等。
- Uniform 现在是 `UniformType.UNIFORM_BUFFER` 类型 (UBO),不再基于 GL uniform location。

---

## 6. PostChainConfig — Codec 驱动配置

**文件**: `net/minecraft/client/renderer/PostChainConfig.java` (133 行)

### 6.1 结构

```java
public record PostChainConfig(
    Map<Identifier, InternalTarget> internalTargets,
    List<Pass> passes
)
```

- 使用 Mojang Codec 系统 (DataFixerUpper) 替代 Gson JSON 手动解析。
- `InternalTarget`: width, height, persistent(跨帧复用), clearColor(ARGB)。
- `Pass`: vertexShaderId, fragmentShaderId, inputs, outputTarget, uniforms。

### 6.2 Input — sealed interface

```java
public sealed interface Input permits PostChainConfig.TextureInput, PostChainConfig.TargetInput
```

- `TextureInput`: samplerName, location(texture), width, height, bilinear。
- `TargetInput`: samplerName, targetId, useDepthBuffer, bilinear。

### 6.3 与 1.20.1/1.21.1 JSON 格式差异

| 字段 | 1.20.1/1.21.1 JSON | 26.1.2 Codec |
|---|---|---|
| pass name | `"name"` | 不再存在 |
| shader 引用 | `"vertex"/"fragment"` → External shader | `"vertex_shader"/"fragment_shader"` → Identifier |
| sampler | effect JSON 的 `"samplers"` | pass 的 `"inputs"` |
| intarget/outtarget | 字符串 | inputs + output |
| auxtargets | JSON 数组 | inputs 数组(区分 TextureInput/TargetInput) |
| uniform values | PostChain JSON 的 `"uniforms"` | Pass 的 `"uniforms"` map |

**重要**: 26.1.2 不再有 "auxtargets" 概念。所有输入统一为 `inputs` 列表,通过 sealed type 区分 TargetInput 还是 TextureInput。

---

## 7. PostPass.java — RenderPass 驱动

**文件**: `net/minecraft/client/renderer/PostPass.java` (213 行)

### 7.1 不再有 EffectInstance

```java
// 1.21.1: PostPass持有 EffectInstance
private final EffectInstance effect;

// 26.1.2: PostPass持有 RenderPipeline
private final RenderPipeline pipeline;
```

### 7.2 构造 (第 42–73 行)

```java
public PostPass(RenderPipeline pipeline, Identifier outputTargetId,
                Map<String, List<UniformValue>> uniformGroups,
                List<Input> inputs)
```

- Uniform 按 group 分组,每组编译为独立的 GpuBuffer (UBO)。
- 使用 `Std140Builder` 按 std140 布局计算和写入 uniform 数据。
- 创建 `infoUbo` (MappableRingBuffer): 存储输出尺寸和每个输入纹理的尺寸。

### 7.3 addToFrame (第 75–144 行)

核心处理逻辑:

```java
public void addToFrame(FrameGraphBuilder frame,
                       Map<Identifier, ResourceHandle<RenderTarget>> targets,
                       GpuBufferSlice shaderOrthoMatrix) {
    FramePass pass = frame.addPass(this.name);
    // 1. 声明输入资源依赖
    for (input : inputs) { input.addToPass(pass, targets); }
    // 2. 声明输出资源
    ResourceHandle<RenderTarget> outputHandle = ...;
    pass.readsAndWrites(outputHandle);
    // 3. 录制执行代码
    pass.executes(() -> {
        RenderTarget outputTarget = outputHandle.get();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(shaderOrthoMatrix, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        // 写入 SamplerInfo UBO
        try (GpuBuffer.MappedView view = commandEncoder.mapBuffer(infoUbo.currentBuffer(), false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec2(outputTarget.width, outputTarget.height);
            for (input : inputs) builder.putVec2(input.texture.width, input.texture.height);
        }
        // 创建 RenderPass
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Post pass " + this.name,
                outputTarget.getColorTextureView(),
                OptionalInt.empty(),
                outputTarget.useDepth ? outputTarget.getDepthTextureView() : null,
                OptionalDouble.empty())) {
            renderPass.setPipeline(this.pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", this.infoUbo.currentBuffer());
            for (customUniforms) renderPass.setUniform(...);
            for (input) renderPass.bindTexture(samplerName + "Sampler", view, sampler);
            renderPass.draw(0, 3);
        }
        this.infoUbo.rotate();
        RenderSystem.restoreProjectionMatrix();
    });
}
```

**关键变化**:
- `renderPass.draw(0, 3)` 绘制 3 个顶点 (全屏三角形而非四边形)。
- 统一 uniform buffers 使用 `CommandEncoder.mapBuffer` + `Std140Builder`。
- 纹理通过 `renderPass.bindTexture()` 绑定。

### 7.4 Input sealed interface

```java
public interface Input {
    void addToPass(FramePass pass, Map<Identifier, ResourceHandle<RenderTarget>> targets);
    GpuTextureView texture(Map<...> targets);
    String samplerName();
    boolean bilinear();
}
```

实现:
- `TargetInput(targetId, useDepthBuffer, bilinear)`: 从 target map 获取 RenderTarget 的 color/depth textureView。
- `TextureInput(texture, width, height, bilinear)`: 绑定外部纹理。

---

## 8. PostChain.java — FrameGraph 驱动

**文件**: `net/minecraft/client/renderer/PostChain.java` (224 行)

### 8.1 构造 (第 41–53 行)

```java
private PostChain(
    List<PostPass> passes,
    Map<Identifier, PostChainConfig.InternalTarget> internalTargets,
    Set<Identifier> externalTargets,
    Projection projection,
    ProjectionMatrixBuffer projectionMatrixBuffer
)
```

- 通过静态工厂 `load()` 创建。
- 不再持有 `screenTarget` (传入的 MainTarget) —— 目标通过 FrameGraph 资源系统传递。

### 8.2 静态工厂 load (第 55–79 行)

```java
public static PostChain load(
    PostChainConfig config, TextureManager textureManager,
    Set<Identifier> allowedExternalTargets, Identifier id,
    Projection projection, ProjectionMatrixBuffer projectionMatrixBuffer
)
```

1. 收集所有引用的 external targets (不在 internalTargets 中的目标)。
2. 验证它们都在 `allowedExternalTargets` 中。
3. 遍历 config.passes,为每个创建 PostPass:
```java
RenderPipeline.Builder pipelineBuilder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
    .withFragmentShader(config.fragmentShaderId())
    .withVertexShader(config.vertexShaderId())
    .withLocation(id);
// 为每个 input 添加 sampler: pipelineBuilder.withSampler(input.samplerName() + "Sampler");
pipelineBuilder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);
// 为每个 uniformGroup 添加 uniform block
pipelineBuilder.withUniform(groupName, UniformType.UNIFORM_BUFFER);
RenderPipeline pipeline = pipelineBuilder.build();
```

### 8.3 addToFrame (第 124–154 行)

```java
public void addToFrame(FrameGraphBuilder frame, int screenWidth, int screenHeight,
                       PostChain.TargetBundle providedTargets)
```

1. 设置投影矩阵。
2. 创建 `targets` map: external targets 从 TargetBundle 获取; internal targets 通过 frame.createInternal 或 frame.importExternal (persistent) 创建。
3. 遍历 passes,调用 `pass.addToFrame(frame, targets, projectionBuffer)`。
4. 将 external targets 的更新 handle 写回 TargetBundle。

### 8.4 兼容层 process (第 156–162 行)

```java
@Deprecated
public void process(RenderTarget mainTarget, GraphicsResourceAllocator resourceAllocator) {
    FrameGraphBuilder frame = new FrameGraphBuilder();
    PostChain.TargetBundle targets = PostChain.TargetBundle.of(MAIN_TARGET_ID,
        frame.importExternal("main", mainTarget));
    this.addToFrame(frame, mainTarget.width, mainTarget.height, targets);
    frame.execute(resourceAllocator);
}
```

- `@Deprecated` — 推荐直接使用 `addToFrame` + FrameGraph。
- 临时构造 FrameGraphBuilder 并立即执行。

### 8.5 persistent targets (第 164–177 行)

```java
private RenderTarget getOrCreatePersistentTarget(Identifier id, RenderTargetDescriptor descriptor) {
    // 缓存: 相同尺寸复用,不同尺寸重建
    if (target == null || sizeChanged) { ... descriptor.allocate(); descriptor.prepare(); ... }
    return target;
}
```

- `InternalTarget.persistent = true` 的目标跨帧复用,避免每帧重新分配。

### 8.6 TargetBundle 接口 (第 190–223 行)

```java
public interface TargetBundle {
    void replace(Identifier id, ResourceHandle<RenderTarget> handle);
    @Nullable ResourceHandle<RenderTarget> get(Identifier id);
    default ResourceHandle<RenderTarget> getOrThrow(Identifier id) { ... }
}
```

---

## 9. GameRenderer — PostChain 实例和流程

**文件**: `net/minecraft/client/renderer/GameRenderer.java` (893 行,比 1.21.1 减少约 800 行)

### 9.1 PostChain 实例

26.1.2 GameRenderer **不再直接持有 PostChain 实例**。PostChain 通过 `ShaderManager` 管理:

```java
// 字段:
private @Nullable Identifier postEffectId;  // 仅存 ID
// 不再有 PostChain postEffect 字段
// 不再有 ShaderInstance blitShader 字段
// 不再有 Map<String, ShaderInstance> shaders 字段
// 不再有所有 static ShaderInstance 字段
```

### 9.2 效果触发

```java
public void checkEntityPostEffect(@Nullable Entity cameraEntity) {
    switch (cameraEntity) {
        case Creeper → setPostEffect(Identifier.withDefaultNamespace("creeper"));
        case Spider  → setPostEffect(Identifier.withDefaultNamespace("spider"));
        case EnderMan → setPostEffect(Identifier.withDefaultNamespace("invert"));
        default → clearPostEffect() or loadEntityShader(entity, this);
    }
}
```

### 9.3 渲染流程 (第 453–466 行)

```java
if (shouldRenderLevel) {
    this.lightmap.render(...);
    profiler.push("world");
    this.renderLevel(deltaTracker);
    this.tryTakeScreenshotIfNeeded();
    this.minecraft.levelRenderer.doEntityOutline();
    if (this.postEffectId != null && this.effectActive) {
        PostChain postChain = this.minecraft.getShaderManager()
            .getPostChain(this.postEffectId, LevelTargetBundle.MAIN_TARGETS);
        if (postChain != null) {
            postChain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
        }
    }
    profiler.pop();
}
```

- PostChain 通过 ShaderManager 按需加载(懒加载+缓存)。
- 使用 `@Deprecated` 的 `process()` 兼容路径。
- LevelTargetBundle.MAIN_TARGETS 指定允许的 external target。

### 9.4 processBlurEffect (第 243–247 行)

```java
public void processBlurEffect() {
    PostChain postChain = this.minecraft.getShaderManager()
        .getPostChain(BLUR_POST_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
    if (postChain != null) {
        postChain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
    }
}
```

- `BLUR_POST_CHAIN_ID = Identifier.withDefaultNamespace("blur")` (第 106 行)

### 9.5 resourcePool

```java
private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
```

- 跨帧资源池,缓存 3 帧的资源。提供给 `postChain.process()` 的 `GraphicsResourceAllocator`。

---

## 10. LevelTargetBundle — 多 Target 标识

**文件**: `net/minecraft/client/renderer/LevelTargetBundle.java` (84 行)

### 10.1 Target ID 定义

```java
public static final Identifier MAIN_TARGET_ID       = PostChain.MAIN_TARGET_ID; // "minecraft:main"
public static final Identifier TRANSLUCENT_TARGET_ID = Identifier.withDefaultNamespace("translucent");
public static final Identifier ITEM_ENTITY_TARGET_ID = Identifier.withDefaultNamespace("item_entity");
public static final Identifier PARTICLES_TARGET_ID   = Identifier.withDefaultNamespace("particles");
public static final Identifier WEATHER_TARGET_ID     = Identifier.withDefaultNamespace("weather");
public static final Identifier CLOUDS_TARGET_ID      = Identifier.withDefaultNamespace("clouds");
public static final Identifier ENTITY_OUTLINE_TARGET_ID = Identifier.withDefaultNamespace("entity_outline");
```

### 10.2 TargetSet 定义

```java
MAIN_TARGETS    = Set.of(MAIN_TARGET_ID);                           // 仅主画面
OUTLINE_TARGETS = Set.of(MAIN_TARGET_ID, ENTITY_OUTLINE_TARGET_ID); // 轮廓
SORTING_TARGETS = Set.of(MAIN_TARGET_ID, TRANSLUCENT_TARGET_ID, ITEM_ENTITY_TARGET_ID,
                         PARTICLES_TARGET_ID, WEATHER_TARGET_ID, CLOUDS_TARGET_ID);
```

- 这些 Set 传给 `ShaderManager.getPostChain(id, allowedTargets)` 作为 `allowedExternalTargets`。

### 10.3 实现 TargetBundle

```java
public class LevelTargetBundle implements PostChain.TargetBundle {
    public ResourceHandle<RenderTarget> main;
    public @Nullable ResourceHandle<RenderTarget> translucent;
    public @Nullable ResourceHandle<RenderTarget> itemEntity;
    // ... etc
}
```

---

## 11. ShaderManager — PostChain 管理

**文件**: `net/minecraft/client/renderer/ShaderManager.java` (281 行)

### 11.1 PostChain 加载

```java
// 第 138-147 行
private static void loadPostChain(Identifier location, Resource resource,
                                   Builder<Identifier, PostChainConfig> output) {
    // 从 post_effect/<name>.json 加载,使用 PostChainConfig.CODEC 解析
    output.put(id, PostChainConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(...));
}
```

- 配置文件路径: `shaders/post/<name>.json` → `PostChainConfig` Codec 解析。

### 11.2 getPostChain (第 190–199 行)

```java
public @Nullable PostChain getPostChain(Identifier id, Set<Identifier> allowedTargets) {
    try {
        return this.compilationCache.getOrLoadPostChain(id, allowedTargets);
    } catch (CompilationException var4) {
        LOGGER.error("Failed to load post chain: {}", id, var4);
        this.compilationCache.postChains.put(id, Optional.empty());  // 缓存失败
        this.tryTriggerRecovery(var4);
        return null;
    }
}
```

- 懒加载: 首次请求时加载编译,失败后缓存 Optional.empty。

### 11.3 Pipeline 预编译

```java
// apply 方法
Set<RenderPipeline> pipelinesToPreload = new HashSet<>(RenderPipelines.getStaticPipelines());
for (RenderPipeline pipeline : pipelinesToPreload) {
    CompiledRenderPipeline compiled = device.precompilePipeline(pipeline, compilationCache::getShaderSource);
    if (!compiled.isValid()) failedLoads.add(pipeline.getLocation());
}
```

---

## 12. FrameGraphBuilder — 帧图调度

**文件**: `com/mojang/blaze3d/framegraph/FrameGraphBuilder.java` (369 行)

### 12.1 核心概念

FrameGraph 是现代渲染引擎常见模式:
1. **声明阶段**: 声明 pass 及其输入/输出资源。
2. **编译阶段**: 分析资源依赖,确定生命周期(alias 分析,资源复用)。
3. **执行阶段**: 按顺序执行 pass,自动分配/释放临时资源。

### 12.2 关键 API

```java
FramePass addPass(String name);                              // 添加pass
<T> ResourceHandle<T> importExternal(String name, T resource); // 导入外部资源
<T> ResourceHandle<T> createInternal(String name, ResourceDescriptor<T> descriptor); // 创建内部资源
void execute(GraphicsResourceAllocator resourceAllocator);   // 执行帧图
```

### 12.3 ResourceHandle

- `reads(handle)`: 声明 pass 读取该资源。
- `readsAndWrites(handle)`: 声明 pass 读写该资源(如 output target)。
- 通过 `handle.get()` 在执行阶段获取实际资源。

### 12.4 资源生命周期

```java
public void execute(GraphicsResourceAllocator resourceAllocator, Inspector inspector) {
    // 1. identifyPassesToKeep() — 去除死pass
    // 2. resolvePassOrder() — 拓扑排序确定执行顺序
    // 3. assignResourceLifetimes() — 确定资源首次使用和最后使用的pass
    for (pass in passesInOrder) {
        // acquire resources (首次使用)
        for (resource : pass.resourcesToAcquire) resource.acquire(resourceAllocator);
        // execute pass
        pass.task.run();
        // release resources (最后使用)
        for (resource : pass.resourcesToRelease) resource.release(resourceAllocator);
    }
}
```

---

## 附录: 26.1.2 完整渲染流程中的 PostChain

```
GameRenderer.render()
├── renderLevel(deltaTracker)
│   ├── [各种pass → LevelTargetBundle targets]
│   └── levelRenderer.doEntityOutline()
│       └── entityTarget.blitAndBlendToTexture(mainTarget.colorTextureView)
├── if (postEffectId != null && effectActive)
│   └── ShaderManager.getPostChain(postEffectId, MAIN_TARGETS)
│       └── postChain.process(mainTarget, resourcePool)
│           ├── FrameGraphBuilder → addToFrame → execute
│           │   ├── pass: createInternal targets
│           │   ├── pass: PostPass.addToFrame → RenderPass draw
│           │   └── export: write back to mainTarget
├── fogRenderer.endFrame()
├── CommandEncoder.clearDepthTexture(mainTarget.depthTexture, 1.0)
├── [GUI渲染: guiRenderer.render()]
└── [present: mainTarget.blitToScreen() → presentTexture]
```
