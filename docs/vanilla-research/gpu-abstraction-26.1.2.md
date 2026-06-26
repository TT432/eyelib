# GPU 抽象层 — 26.1.2 (NeoForge)

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [架构总览：Facade-Backend 抽象模式](#2-架构总览facade-backend-抽象模式)
3. [GpuDevice 抽象与 GlDevice 实现](#3-gpudevice-抽象与-gldevice-实现)
4. [CommandEncoder 命令编码器](#4-commandencoder-命令编码器)
5. [RenderPass 渲染通道](#5-renderpass-渲染通道)
6. [RenderPipeline 渲染管线](#6-renderpipeline-渲染管线)
7. [GpuBuffer 资源管理](#7-gpubuffer-资源管理)
8. [GpuTexture 纹理抽象](#8-gputexture-纹理抽象)
9. [RenderSystem 瘦身](#9-rendersystem-瘦身)
10. [GlStateManager 26.1.2](#10-glstatemanager-2612)
11. [线程模型](#11-线程模型)
12. [关键不变量与约束](#12-关键不变量与约束)

---

## 1. 类位置与职责

### 1.1 新增包

| 包 | 职责 |
|---|---|
| `com.mojang.blaze3d.buffers` | GPU 缓冲抽象层（GpuBuffer, GpuBufferSlice, GpuFence, Std140Builder） |
| `com.mojang.blaze3d.textures` | GPU 纹理抽象层（GpuTexture, GpuTextureView, GpuSampler, TextureFormat 等） |
| `com.mojang.blaze3d.opengl` | **OpenGL 后端实现**（GlDevice, GlRenderPass, GlCommandEncoder, GlBuffer, GlTexture 等） |
| `com.mojang.blaze3d.framegraph` | 帧图（未深入研究） |
| `com.mojang.blaze3d.resource` | GPU 资源管理（未深入研究） |

### 1.2 核心类清单

| 类名 | 包 | 角色 |
|---|---|---|
| `GpuDevice` | `systems` | **Facade** - 渲染设备外观 |
| `GpuDeviceBackend` | `systems` | **Interface** - 后端抽象接口 |
| `GlDevice` | `opengl` | OpenGL 后端实现 |
| `CommandEncoder` | `systems` | **Facade** - 命令编码器外观 |
| `CommandEncoderBackend` | `systems` | **Interface** - 编码器后端接口 |
| `GlCommandEncoder` | `opengl` | OpenGL 编码器实现 |
| `RenderPass` | `systems` | **Facade** - 渲染通道外观 |
| `RenderPassBackend` | `systems` | **Interface** - 通道后端接口 |
| `GlRenderPass` | `opengl` | OpenGL 渲染通道实现 |
| `RenderPipeline` | `pipeline` | 管线描述符（不可变） |
| `CompiledRenderPipeline` | `pipeline` | 编译后的管线接口 |
| `GlRenderPipeline` | `opengl` | GL 编译管线 |
| `GpuBuffer` | `buffers` | GPU 缓冲抽象类 |
| `GpuBufferSlice` | `buffers` | 缓冲切片 record |
| `GlBuffer` | `opengl` | GL 缓冲实现 |
| `GpuTexture` | `textures` | GPU 纹理抽象类 |
| `GpuTextureView` | `textures` | 纹理视图抽象类 |
| `GlTexture` | `opengl` | GL 纹理实现 |
| `GlTextureView` | `opengl` | GL 纹理视图实现 |
| `MainTarget` | `pipeline` | 主 framebuffer（继承 RenderTarget） |
| `RenderTarget` | `pipeline` | Framebuffer 基类 |
| `DynamicUniforms` | `client.renderer` | UBO 动态 uniform 管理 |
| `GlStateManager` | `opengl` | **OpenGL 后端专用状态管理** |
| `RenderSystem` | `systems` | **极度瘦身的静态门面**（418 行 vs 1.20.1 的 1082 行） |

---

## 2. 架构总览：Facade-Backend 抽象模式

### 2.1 三层 Facade 模式

每个 GPU 抽象概念使用相同的三层架构：

```
调用方代码
    │
    ▼
┌─────────────┐  验证参数、线程检查
│   Facade    │  (GpuDevice / CommandEncoder / RenderPass)
└──────┬──────┘
       │ 委托
       ▼
┌─────────────┐  接口定义
│  Backend    │  (GpuDeviceBackend / CommandEncoderBackend / RenderPassBackend)
│  Interface  │
└──────┬──────┘
       │ 实现
       ▼
┌─────────────┐  OpenGL 具体实现
│ GlBackend   │  (GlDevice / GlCommandEncoder / GlRenderPass)
└─────────────┘
```

### 2.2 设计目标

1. **跨 API 可移植**: 将 Facade 和 Backend Interface 与 OpenGL 解耦。理论上替换 GlDevice 即可切换渲染后端（Vulkan/DirectX）。
2. **参数验证集中**: Facade 层负责 usage flag 检查、尺寸验证、关闭状态检查。Backend 层只做 GL 调用。
3. **资源生命周期明确**: AutoCloseable 模式，texture/buffer 关闭后标记 `closed`，后续操作抛出异常。
4. **Pipeline 状态封装**: blend/depth/cull/stencil 不再由 RenderSystem 逐个设置，而是打成 RenderPipeline 不可变对象。

---

## 3. GpuDevice 抽象与 GlDevice 实现

### 3.1 GpuDevice (Facade, 213行)

```java
// com.mojang.blaze3d.systems.GpuDevice
public class GpuDevice {
    private final GpuDeviceBackend backend;

    // 构造
    public GpuDevice(GpuDeviceBackend backend)

    // 资源创建
    CommandEncoder createCommandEncoder()
    GpuSampler createSampler(AddressMode, AddressMode, FilterMode, FilterMode, int, OptionalDouble)
    GpuTexture createTexture(Supplier<String>/String, int usage, TextureFormat, int w, int h, int depthLayers, int mips)
    GpuTextureView createTextureView(GpuTexture) / createTextureView(GpuTexture, int baseMip, int mips)
    GpuBuffer createBuffer(Supplier<String>, int usage, long size)
    GpuBuffer createBuffer(Supplier<String>, int usage, ByteBuffer data)

    // 查询
    String getImplementationInformation()
    List<String> getLastDebugMessages()
    boolean isDebuggingEnabled()
    String getVendor() / getBackendName() / getVersion() / getRenderer()
    int getMaxTextureSize()
    int getUniformOffsetAlignment()
    int getMaxSupportedAnisotropy()
    List<String> getEnabledExtensions()

    // Pipeline 编译
    CompiledRenderPipeline precompilePipeline(RenderPipeline)
    CompiledRenderPipeline precompilePipeline(RenderPipeline, ShaderSource)
    void clearPipelineCache()

    // 生命周期
    void close()
    void setVsync(boolean)
    void presentFrame()
    boolean isZZeroToOne()
}
```

**参数验证**: `createTexture` 验证 mipLevels、depthOrLayers、Cubemap 正方性、数组纹理支持等。`createBuffer` 验证 size > 0。`createSampler` 验证 maxAnisotropy 范围。

### 3.2 GpuDeviceBackend (Interface, 78行)

```java
public interface GpuDeviceBackend {
    CommandEncoderBackend createCommandEncoder();
    GpuSampler createSampler(...);
    GpuTexture createTexture(Supplier<String>/String, int usage, TextureFormat, int, int, int, int);
    GpuTextureView createTextureView(GpuTexture) / (GpuTexture, int, int);
    GpuBuffer createBuffer(Supplier<String>, int usage, long) / (Supplier<String>, int usage, ByteBuffer);
    String getImplementationInformation();
    List<String> getLastDebugMessages();
    boolean isDebuggingEnabled();
    String getVendor() / getBackendName() / getVersion() / getRenderer();
    int getMaxTextureSize() / getUniformOffsetAlignment() / getMaxSupportedAnisotropy();
    CompiledRenderPipeline precompilePipeline(RenderPipeline, ShaderSource);
    void clearPipelineCache();
    List<String> getEnabledExtensions();
    void close();
    void setVsync(boolean);
    void presentFrame();
    boolean isZZeroToOne();
}
```

### 3.3 GlDevice (OpenGL 实现, 429行)

```java
// com.mojang.blaze3d.opengl.GlDevice
class GlDevice implements GpuDeviceBackend {
    // GL 扩展能力检测
    protected static boolean USE_GL_ARB_vertex_attrib_binding;
    protected static boolean USE_GL_KHR_debug;
    protected static boolean USE_GL_ARB_direct_state_access;  // DSA
    protected static boolean USE_GL_ARB_buffer_storage;       // Immutable storage

    // 核心组件
    private final CommandEncoderBackend encoder;          // GlCommandEncoder（共享单例）
    private final GlDebug debugLog;
    private final GlDebugLabel debugLabels;
    private final DirectStateAccess directStateAccess;    // DSA 抽象
    private final BufferStorage bufferStorage;            // 缓冲存储策略
    private final VertexArrayCache vertexArrayCache;      // VAO 缓存
    private final int maxSupportedTextureSize;
    private final int uniformOffsetAlignment;             // UBO offset alignment
    private final int maxSupportedAnisotropy;

    // Pipeline 编译缓存
    private final Map<RenderPipeline, GlRenderPipeline> pipelineCache;      // IdentityHashMap
    private final Map<ShaderCompilationKey, GlShaderModule> shaderCache;    // HashMap

    // 构造: GLFW context → GLCapabilities → 扩展检测 → 各组件初始化
    GlDevice(long windowHandle, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions)
}
```

**构造流程**:
1. `GLFW.glfwMakeContextCurrent(windowHandle)` — 绑定 GL 上下文
2. `GL.createCapabilities()` — 获取 GLCapabilities
3. 探测最大纹理尺寸（与 1.20.1 相同的二分探测算法）
4. 创建各组件: GlDebug(debug callback) → GlDebugLabel → VertexArrayCache → BufferStorage → DirectStateAccess
5. 初始化 GL 全局状态: `GL11.glEnable(GL_PROGRAM_POINT_SIZE)`, `GL11.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS)`
6. 检测各向异性过滤: `GL_EXT_texture_filter_anisotropic`

**Pipeline 编译缓存**:
- `getOrCompilePipeline(RenderPipeline)`: `pipelineCache.computeIfAbsent(pipeline, this::compilePipeline)`
- `compilePipeline`: 编译 vertex + fragment shader → `GlProgram.link()` → 设置 uniforms/samplers
- `clearPipelineCache()`: 关闭所有 program + shader，AMD 显卡额外执行 "献祭" shader（编译后立即删除以清理驱动状态）

**纹理创建**(`createTexture`):
1. `GlStateManager._genTexture()` → 绑定到 GL_TEXTURE_2D 或 GL_TEXTURE_CUBE_MAP
2. 设置 mip 参数: GL_TEXTURE_BASE_LEVEL, GL_TEXTURE_MAX_LEVEL, GL_TEXTURE_MAX_LOD
3. 为每个 mip level 调用 `_texImage2D` 分配存储（初始数据 null）
4. 错误检查: GL_OUT_OF_MEMORY → GpuOutOfMemoryException

**缓冲创建**(`createBuffer`):
1. 委托 `BufferStorage.createBuffer()` → `GlStateManager._glGenBuffers()`
2. 基于 `GL_ARB_buffer_storage` 或 fallback `glBufferData` 分配
3. 错误检查: GL_OUT_OF_MEMORY → GpuOutOfMemoryException

---

## 4. CommandEncoder 命令编码器

### 4.1 CommandEncoder (Facade, 524行)

```java
// com.mojang.blaze3d.systems.CommandEncoder
public class CommandEncoder {
    private final GpuDeviceBackend device;
    private final CommandEncoderBackend backend;

    // RenderPass 创建
    RenderPass createRenderPass(Supplier<String>, GpuTextureView, OptionalInt clearColor)
    RenderPass createRenderPass(Supplier<String>, GpuTextureView, OptionalInt, GpuTextureView depth, OptionalDouble clearDepth)

    // 离屏操作
    void clearColorTexture(GpuTexture, int clearColor)
    void clearColorAndDepthTextures(GpuTexture, int, GpuTexture, double)
    void clearColorAndDepthTextures(GpuTexture, int, GpuTexture, double, int x, int y, int w, int h)  // 区域清除
    void clearDepthTexture(GpuTexture, double)
    void clearStencilTexture(GpuTexture, int)

    // 缓冲操作
    void writeToBuffer(GpuBufferSlice, ByteBuffer)
    GpuBuffer.MappedView mapBuffer(GpuBuffer/buffer, boolean read, boolean write)
    GpuBuffer.MappedView mapBuffer(GpuBufferSlice, boolean read, boolean write)
    void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target)

    // 纹理操作
    void writeToTexture(GpuTexture, NativeImage)
    void writeToTexture(GpuTexture, NativeImage, int mip, int layer, int dx, int dy, int w, int h, int sx, int sy)
    void writeToTexture(GpuTexture, ByteBuffer, Format, int mip, int layer, int dx, int dy, int w, int h)
    void copyTextureToBuffer(GpuTexture, GpuBuffer, long offset, Runnable callback, int mip)
    void copyTextureToBuffer(GpuTexture, GpuBuffer, long offset, Runnable callback, int mip, int x, int y, int w, int h)
    void copyTextureToTexture(GpuTexture src, GpuTexture dst, int mip, int dx, int dy, int sx, int sy, int w, int h)

    // 呈现
    void presentTexture(GpuTextureView)

    // 同步
    GpuFence createFence()
    GpuQuery timerQueryBegin()
    void timerQueryEnd(GpuQuery)
}
```

**核心约束**: 除 `createRenderPass` 外，所有命令要求不在 render pass 内（`!backend.isInRenderPass()`）。即：RenderPass 是互斥状态——要么在 pass 内渲染，要么在 pass 外执行数据搬运/清除。

### 4.2 CommandEncoderBackend (Interface, 66行)

与 CommandEncoder 一一对应的后端接口方法。每个方法签名与 Facade 层略有简化（无验证逻辑）。

### 4.3 GlCommandEncoder (OpenGL 实现, 745行)

关键实现细节:

**FBO 管理**: 构造时创建 `readFbo` 和 `drawFbo` 两个持久 FBO，用于 `glBlitFramebuffer` 操作。避免临时创建/销毁 FBO。

**createRenderPass**:
1. 获取 `GlTextureView.getFbo()` 配置好的 FBO（由 GlTexture 内部缓存）
2. `GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fbo)`
3. 按需 clear color/depth（设置 clearColor/clearDepth → clear mask）
4. 设置初始 viewport 到纹理尺寸
5. 返回 `new GlRenderPass(this, device, hasDepth)`

**applyPipelineState** (第 672-714 行):
- 仅当 `lastPipeline != pipeline` 时执行（状态缓存）
- 从 RenderPipeline 提取 DepthStencilState → 设置 depthTest, depthFunc, depthMask, polygonOffset
- 设置 cull (enable/disable)
- 设置 blend (从 ColorTargetState.blendFunction)
- 设置 polygonMode + colorMask(writeMask)

**drawFromBuffers** (第 440-484 行):
1. `vertexArrayCache.bindVertexArray(format, vertexBuffer)` — VAO 绑定
2. `GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer)` — 索引缓冲绑定
3. 分发到: `glDrawElementsInstancedBaseVertex` / `glDrawElementsInstanced` / `glDrawElementsBaseVertex` / `glDrawElements` / `glDrawArraysInstanced` / `glDrawArrays`

**Uniform 绑定** (第 487-670 行):
- UBO: `GL32.glBindBufferRange(GL_UNIFORM_BUFFER, blockBinding, handle, offset, length)`
- UTB (Uniform Texel Buffer): `glUniform1i(location, samplerIdx)` + `glActiveTexture` + `glBindTexture(GL_TEXTURE_BUFFER)` + `glTexBuffer`
- Sampler: `glUniform1i(location, samplerIndex)` + `glActiveTexture` + `glBindTexture` + `glBindSampler`
- `dirtyUniforms` 集合跟踪哪些 uniform 在本帧被修改，仅绑定脏 uniform

**Stencil 支持**: 通过 NeoForge `StencilTest` Optional. 独立前后表面设置(`_stencilFuncFront`/`_stencilFuncBack`)。

---

## 5. RenderPass 渲染通道

### 5.1 RenderPass (Facade, 152行)

```java
// com.mojang.blaze3d.systems.RenderPass
public class RenderPass implements AutoCloseable {
    private final RenderPassBackend backend;
    private final GpuDeviceBackend device;

    // Debug
    void pushDebugGroup(Supplier<String>)
    void popDebugGroup()

    // 管线
    void setPipeline(RenderPipeline)

    // 纹理/Uniform
    void bindTexture(String name, GpuTextureView, GpuSampler)
    void setUniform(String name, GpuBuffer)
    void setUniform(String name, GpuBufferSlice)  // 验证 offset alignment

    // Viewport/Scissor
    void setViewport(int x, int y, int w, int h)
    void enableScissor(int x, int y, int w, int h)
    void disableScissor()

    // 缓冲绑定
    void setVertexBuffer(int slot, GpuBuffer)
    void setIndexBuffer(GpuBuffer, IndexType)

    // 绘制
    void drawIndexed(int baseVertex, int firstIndex, int indexCount, int instanceCount)
    <T> void drawMultipleIndexed(Collection<Draw<T>>, GpuBuffer defaultIB, IndexType, Collection<String> dynamicUniforms, T)
    void draw(int firstVertex, int vertexCount)

    // 生命周期
    void close()  // 验证 debug group 平衡 → backend.close()
}
```

**RenderPass.Draw<T>**: 多绘制批次记录，支持 per-draw uniform upload callback。

### 5.2 RenderPassBackend (Interface, 55行)

```java
public interface RenderPassBackend extends AutoCloseable {
    void pushDebugGroup(Supplier<String>);
    void popDebugGroup();
    void setPipeline(RenderPipeline);
    void bindTexture(String, GpuTextureView, GpuSampler);
    void setUniform(String, GpuBuffer);
    void setUniform(String, GpuBufferSlice);
    void setViewport(int, int, int, int);
    void enableScissor(int, int, int, int);
    void disableScissor();
    void setVertexBuffer(int, GpuBuffer);
    void setIndexBuffer(GpuBuffer, IndexType);
    void drawIndexed(int, int, int, int);
    <T> void drawMultipleIndexed(Collection<Draw<T>>, GpuBuffer, IndexType, Collection<String>, T);
    void draw(int, int);
    void close();
    boolean isClosed();
}
```

### 5.3 GlRenderPass (OpenGL 实现, 180行)

**状态缓存**:
```java
protected GlRenderPipeline pipeline;                       // 当前管线
protected GpuBuffer[] vertexBuffers = new GpuBuffer[1];    // 仅 1 个 VB slot (MAX_VERTEX_BUFFERS = 1)
protected GpuBuffer indexBuffer;
protected VertexFormat.IndexType indexType;
protected HashMap<String, GpuBufferSlice> uniforms;        // UBO uniform
protected HashMap<String, TextureViewAndSampler> samplers; // 纹理采样器
protected Set<String> dirtyUniforms;                       // 脏标记集合
private final ScissorState scissorState;
```

**setPipeline**: 调用 `RenderSystem.applyPipelineModifiers(pipeline)` 应用 NeoForge 管线修改器，然后通过 `device.getOrCompilePipeline()` 获取 `GlRenderPipeline`。切换管线时所有 uniform 标记为 dirty。

**Uniform 上传惰性化**: `setUniform` 仅更新 `uniforms` map 和 `dirtyUniforms` 集合。实际的 GL 绑定推迟到 `executeDraw` 中 `trySetup()` 时——仅绑定脏 uniform。

**Validation 模式**: `VALIDATION = IS_RUNNING_IN_IDE && !neoforge.disableGlValidation`。在 IDE 运行且未显式禁用时，每次 draw 前验证:
- pipeline 不为 null 且 program 有效
- 所有 required uniform 被设置
- uniform buffer 未被关闭且有 USAGE_UNIFORM
- sampler 对应的 texture view 未被关闭且有 USAGE_TEXTURE_BINDING
- vertex/index buffer 未被关闭且有正确的 usage flag

---

## 6. RenderPipeline 渲染管线

### 6.1 RenderPipeline (不可变描述符, 520行)

```java
// com.mojang.blaze3d.pipeline.RenderPipeline
public class RenderPipeline {
    private final Identifier location;                    // 管线标识
    private final Identifier vertexShader;                // VS shader ID
    private final Identifier fragmentShader;              // FS shader ID
    private final ShaderDefines shaderDefines;            // 着色器宏定义
    private final List<String> samplers;                  // 采样器名称列表
    private final List<UniformDescription> uniforms;      // Uniform 声明
    private final DepthStencilState depthStencilState;    // 深度模板状态 (nullable)
    private final PolygonMode polygonMode;                // FILL/LINE/POINT
    private final boolean cull;                           // 是否剔除
    private final ColorTargetState colorTargetState;      // 颜色目标状态(blend + writeMask)
    private final VertexFormat vertexFormat;              // 顶点格式
    private final VertexFormat.Mode vertexFormatMode;     // 图元类型(TRIANGLES等)
    private final Optional<StencilTest> stencilTest;      // NeoForge 模板测试
}
```

**UniformDescription**: `name + UniformType(UNIFORM_BUFFER/TEXEL_BUFFER) + TextureFormat(UTB时)`

**Builder 模式**: `RenderPipeline.builder(snippets...).withLocation().withVertexShader().withFragmentShader().withUniform().withDepthStencilState()...build()`

### 6.2 GlRenderPipeline (GL 编译管线)

```java
// com.mojang.blaze3d.opengl.GlRenderPipeline
record GlRenderPipeline(RenderPipeline info, GlProgram program) implements CompiledRenderPipeline {
    static final GlRenderPipeline INVALID = new GlRenderPipeline(null, GlProgram.INVALID_PROGRAM);
}
```

### 6.3 Pipeline 编译流程

```
RenderPipeline (不可变描述符)
    │
    ▼ GlDevice.compilePipeline()
    │
    ├── getOrCompileShader(Identifier, ShaderType, ShaderDefines, ShaderSource)
    │   ├── shaderCache.computeIfAbsent(key, compileShader)
    │   └── GlslPreprocessor.injectDefines(source, defines) → glCreateShader → glShaderSource → glCompileShader
    │
    ├── GlProgram.link(vertexShader, fragmentShader, vertexFormat, location)
    │   ├── glCreateProgram → glAttachShader → glBindAttribLocation → glLinkProgram
    │   └── 提取 uniforms (glGetActiveUniform → Uniform.Ubo / Uniform.Sampler / Uniform.Utb)
    │
    └── new GlRenderPipeline(pipeline, compiledProgram)
```

---

## 7. GpuBuffer 资源管理

### 7.1 GpuBuffer (抽象基类, 72行)

```java
// com.mojang.blaze3d.buffers.GpuBuffer
public abstract class GpuBuffer implements AutoCloseable {
    // Usage flags (位掩码)
    public static final int USAGE_MAP_READ        = 1;   // 可映射读取
    public static final int USAGE_MAP_WRITE       = 2;   // 可映射写入
    public static final int USAGE_HINT_CLIENT_STORAGE = 4;  // 客户端存储提示
    public static final int USAGE_COPY_DST        = 8;   // 可作为复制目标
    public static final int USAGE_COPY_SRC        = 16;  // 可作为复制源
    public static final int USAGE_VERTEX          = 32;  // 顶点缓冲
    public static final int USAGE_INDEX           = 64;  // 索引缓冲
    public static final int USAGE_UNIFORM         = 128; // Uniform 缓冲
    public static final int USAGE_UNIFORM_TEXEL_BUFFER = 256; // Texel 缓冲

    private final int usage;
    private final long size;

    GpuBufferSlice slice(long offset, long length);  // 创建切片
    GpuBufferSlice slice();                          // 全缓冲切片

    // MappedView: 映射到 CPU 可访问的 ByteBuffer
    interface MappedView extends AutoCloseable {
        ByteBuffer data();
        void close();  // unmap
    }
}
```

### 7.2 GpuBufferSlice (Record, 25行)

```java
public record GpuBufferSlice(GpuBuffer buffer, long offset, long length) {
    GpuBufferSlice slice(long offset, long length);  // 切片内切片
}
```

### 7.3 GlBuffer (GL 实现, 78行)

```java
// com.mojang.blaze3d.opengl.GlBuffer
public class GlBuffer extends GpuBuffer {
    protected boolean closed;
    protected final Supplier<String> label;
    private final DirectStateAccess dsa;
    protected final int handle;             // GL buffer name
    protected ByteBuffer persistentBuffer;  // 持久映射的 buffer (ARB_buffer_storage)

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (persistentBuffer != null) {
                dsa.unmapBuffer(handle, usage());  // 先 unmap
            }
            GlStateManager._glDeleteBuffers(handle);
            MEMORY_POOL.free(handle);  // Tracy 内存追踪
        }
    }
}
```

**持久映射**: 如果 BufferStorage 使用 `GL_ARB_buffer_storage` + `GL_MAP_PERSISTENT_BIT`，`persistentBuffer` 持有持久映射的 ByteBuffer，避免重复 map/unmap。

---

## 8. GpuTexture 纹理抽象

### 8.1 GpuTexture (抽象基类, 75行)

```java
// com.mojang.blaze3d.textures.GpuTexture
public abstract class GpuTexture implements AutoCloseable {
    public static final int USAGE_COPY_DST          = 1;   // 纹理写入目标
    public static final int USAGE_COPY_SRC          = 2;   // 纹理复制源
    public static final int USAGE_TEXTURE_BINDING   = 4;   // 着色器纹理绑定
    public static final int USAGE_RENDER_ATTACHMENT = 8;   // Framebuffer 附件
    public static final int USAGE_CUBEMAP_COMPATIBLE = 16; // Cubemap 兼容

    private final TextureFormat format;
    private final int width, height, depthOrLayers, mipLevels;
    private final int usage;
    private final String label;

    int getWidth(int mipLevel);   // width >> mipLevel
    int getHeight(int mipLevel);  // height >> mipLevel
}
```

### 8.2 GpuTextureView (抽象基类, 42行)

```java
// com.mojang.blaze3d.textures.GpuTextureView
public abstract class GpuTextureView implements AutoCloseable {
    private final GpuTexture texture;     // 底层纹理
    private final int baseMipLevel;       // 起始 mip
    private final int mipLevels;          // mip 层数

    int getWidth(int mipLevel);   // texture.getWidth(mip + baseMip)
    int getHeight(int mipLevel);  // texture.getHeight(mip + baseMip)
}
```

**设计意图**: TextureView 允许对同一纹理的不同 mip 范围创建不同视图，每个视图可独立绑定到不同 sampler slot。

### 8.3 GlTexture (GL 实现, 99行)

```java
// com.mojang.blaze3d.opengl.GlTexture
public class GlTexture extends GpuTexture {
    protected final int id;              // GL texture name
    private int firstFboId;             // 首次分配的 FBO (缓存)
    private int firstFboDepthId;        // 首次 FBO 的深度附件 ID
    private Int2IntMap fboCache;        // (depthId → fboId) 缓存
    private int views;                  // 活跃 TextureView 计数

    int getFbo(DirectStateAccess dsa, GpuTexture depth);  // 获取/创建 FBO

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (views == 0) destroyImmediately();  // 延迟删除：等所有 View 关闭
        }
    }
}
```

**FBO 缓存**: `getFbo()` 按深度附件 ID 缓存 FBO，避免每次 RenderPass 创建都分配/销毁 FBO。

**延迟删除**: `close()` 不立即销毁 GL 纹理，等所有 GpuTextureView 关闭（`views == 0`）后才调用 `destroyImmediately()`。

### 8.4 MainTarget (138行)

```java
// com.mojang.blaze3d.pipeline.MainTarget extends RenderTarget
public class MainTarget extends RenderTarget {
    // 创建 color + depth GpuTexture (usage=15 = COPY_DST|COPY_SRC|TEXTURE_BINDING|RENDER_ATTACHMENT)
    // RGBA8 颜色附件 + DEPTH32(或 NeoForge 的 StencilFormat) 深度附件
    // 分配失败时降级尝试更小尺寸
}
```

**分配策略**: 从请求尺寸向下尝试多个备选尺寸（`Dimension.listWithFallback`），直到分配成功或全部失败。

---

## 9. RenderSystem 瘦身

### 9.1 26.1.2 RenderSystem (418行 vs 1.20.1 1082行, 缩减 61%)

所有 GL 状态操作（blend/depth/cull/stencil/texture/clear/etc.）全部移除。转而依赖:

| 职责 | 1.20.1 | 26.1.2 |
|---|---|---|
| Blend/Depth/Cull/Stencil | RenderSystem.blendFunc/depthFunc/enableCull... | **RenderPipeline 状态对象** |
| 纹理绑定 (setShaderTexture) | RenderSystem.setShaderTexture(unit, id) | **RenderPass.bindTexture(name, view, sampler)** |
| Shader 绑定 | RenderSystem.setShader(Supplier<ShaderInstance>) | **RenderPass.setPipeline(RenderPipeline)** |
| Uniform 设置 | RenderSystem.glUniform*(...) | **RenderPass.setUniform(name, GpuBufferSlice)** |
| 绘制调用 | RenderSystem.drawElements(mode, count, type) | **RenderPass.drawIndexed(baseVertex, firstIndex, indexCount, instanceCount)** |
| Viewport | RenderSystem.viewport(x,y,w,h) | **RenderPass.setViewport(x,y,w,h)** |
| Scissor | RenderSystem.enableScissor/disableScissor | **RenderPass.enableScissor/disableScissor** |
| 矩阵设置 | RenderSystem.setProjectionMatrix/setTextureMatrix | **作为 UBO 传入 (getProjectionMatrixBuffer)** |
| Draw 调用 | RenderSystem.drawElements | **RenderPass.drawIndexed / drawMultipleIndexed** |

### 9.2 26.1.2 RenderSystem 保留的方法

| 方法 | 职责 |
|---|---|
| `initRenderThread()` | 记录渲染线程 |
| `isOnRenderThread()` | 线程检查 |
| `assertOnRenderThread()` | 线程断言 |
| `flipFrame(TracyFrameCapture)` | Tesselator.clear() → device.presentFrame() → dynamicUniforms.reset() → levelRenderer.endFrame() |
| `initRenderer(GpuDevice)` | 设置 DEVICE，创建 DynamicUniforms |
| `getDevice()` / `tryGetDevice()` | 获取 GpuDevice |
| `getDynamicUniforms()` | 获取 DynamicUniforms |
| `bindDefaultUniforms(RenderPass)` | 绑定 Projection/Fog/Globals/Lighting UBO |
| `setProjectionMatrix(GpuBufferSlice, ProjectionType)` | 设置投影矩阵 UBO slice |
| `backupProjectionMatrix()` / `restoreProjectionMatrix()` | 投影矩阵备份/恢复 |
| `getProjectionMatrixBuffer()` | 获取投影矩阵 UBO slice |
| `setShaderFog(GpuBufferSlice)` / `getShaderFog()` | Fog UBO slice |
| `setShaderLights(GpuBufferSlice)` / `getShaderLights()` | Lighting UBO slice |
| `setGlobalSettingsUniform(GpuBuffer)` / `getGlobalSettingsUniform()` | 全局设置 UBO |
| `getModelViewMatrix()` / `getModelViewStack()` | 模型视图矩阵栈(Matrix4fStack) |
| `getSequentialBuffer(VertexFormat.Mode)` | 共享顺序索引缓冲 |
| `enableScissorForRenderTypeDraws()` / `disableScissorForRenderTypeDraws()` | RenderType 专用 scissor |
| `getScissorStateForRenderTypeDraws()` | ScissorState 获取 |
| `pollEvents()` / `isFrozenAtPollEvents()` | GLFW 事件轮询 |
| `getBackendDescription()` / `getApiDescription()` | 后端信息 |
| `setErrorCallback()` / `initBackendSystem(BackendOptions)` | 初始化 |
| `queueFencedTask(Runnable)` / `executePendingTasks()` | GPU fence 异步任务 |
| `getSamplerCache()` | SamplerCache 获取器 |
| `getProjectionType()` | 投影类型 (PERSPECTIVE/ORTHOGONAL) |
| **NeoForge**: `pushPipelineModifier()` / `popPipelineModifier()` / `renderWithPipelineModifier()` / `applyPipelineModifiers()` / `ensurePipelineModifiersEmpty()` | 管线修改器栈 |

### 9.3 26.1.2 RenderSystem 移除的方法（较 1.20.1）

**全部移除（已抽象到 RenderPipeline/RenderPass）**:
- 深度/模板: `disableDepthTest`, `enableDepthTest`, `depthFunc`, `depthMask`, `stencilFunc`, `stencilMask`, `stencilOp`
- 混合: `enableBlend`, `disableBlend`, `blendFunc`, `blendFuncSeparate`, `blendEquation`, `defaultBlendFunc`
- 剔除/多边形: `enableCull`, `disableCull`, `polygonMode`, `enablePolygonOffset`, `disablePolygonOffset`, `polygonOffset`
- 颜色逻辑: `enableColorLogicOp`, `disableColorLogicOp`, `logicOp`
- 纹理: `activeTexture`, `texParameter`, `deleteTexture`, `bindTexture`, `bindTextureForSetup`
- Viewport/颜色掩码: `viewport`, `colorMask`, `lineWidth`
- 清除: `clearDepth`, `clearColor`, `clearStencil`, `clear`
- Shader: `setShader`, `getShader`, `setShaderTexture`, `getShaderTexture`
- Fog/Light/Color: `setShaderFogStart`, `getShaderFogStart`, `setShaderFogEnd`, `getShaderFogEnd`, `setShaderFogColor`, `getShaderFogColor`, `setShaderFogShape`, `getShaderFogShape`, `setShaderGlintAlpha`, `getShaderGlintAlpha`, `setupShaderLights`, `setShaderColor`, `getShaderColor`, `setShaderGameTime`, `getShaderGameTime`
- 纹理矩阵: `setTextureMatrix`, `resetTextureMatrix`, `getTextureMatrix`, `setInverseViewRotationMatrix`, `getInverseViewRotationMatrix`
- 混合: `setupOverlayColor`, `teardownOverlayColor`
- Draw: `drawElements`, `renderThreadTesselator`
- Uniform: 全部 11 个 `glUniform*` 方法
- 缓冲: `glGenBuffers`, `glGenVertexArrays`, `glBindBuffer`, `glBindVertexArray`, `glBufferData`, `glDeleteBuffers`, `glDeleteVertexArrays`
- 其他: `pixelStore`, `readPixels`, `getString`, `renderCrosshair`, `getCapsString`, `setupDefaultState`, `maxSupportedTextureSize`, `runAsFancy`, `setupLevelDiffuseLighting`, `setupGuiFlatDiffuseLighting`, `setupGui3DDiffuseLighting`, `recordRenderCall`, `replayQueue`, `beginInitialization`, `finishInitialization`, `limitDisplayFPS`

**新增方法**:
- `getDevice()`, `tryGetDevice()`, `getDynamicUniforms()`, `bindDefaultUniforms()`
- `queueFencedTask()`, `executePendingTasks()`
- `setGlobalSettingsUniform()`, `getGlobalSettingsUniform()`
- NeoForge PipelineModifier 系列 (5 个方法)
- `getSamplerCache()`, `getProjectionType()`

---

## 10. GlStateManager 26.1.2

### 10.1 包迁移

从 `com.mojang.blaze3d.platform.GlStateManager` → **`com.mojang.blaze3d.opengl.GlStateManager`**

这体现了其角色变化：不再是通用平台状态管理器，而是 **OpenGL 后端专用** 的内部实现。调用方不应直接使用 GlStateManager，应通过 GpuDevice/CommandEncoder/RenderPass。

### 10.2 与 1.20.1 GlStateManager 的关键差异

| 差异 | 1.20.1 | 26.1.2 |
|---|---|---|
| 包路径 | `com.mojang.blaze3d.platform` | `com.mojang.blaze3d.opengl` |
| 文件长度 | 962 行 | 708 行 (减少 26%) |
| 线程断言 | `assertOnRenderThreadOrInit` | `assertOnRenderThread` (统一) |
| COLOR_MASK | ColorMask 类 | `@ColorTargetState.WriteMask int COLOR_MASK = 15` (int 位掩码) |
| FBO 缓存 | 无 | `readFbo` / `writeFbo` 缓存（`_glBindFramebuffer` 去重） |
| 缓冲管理 | ON_LINUX 特殊处理, glMapBuffer | BufferStorage 抽象, 无 ON_LINUX hack |
| Tracy 集成 | 无 | `numTextures`/`numBuffers` Tracy Plot |
| 移除的方法 | — | `_blendFunc` (仅 int 重载), `_drawArrays`, `_vertexAttribPointer`(DSA), `_vertexAttribIPointer`(DSA), `_enableVertexAttribArray`(VAO), `_disableVertexAttribArray`(VAO), `glActiveTexture`, `_getTexLevelParameter`(仅 initPhase), `upload`, `_genTextures`, `_deleteTextures`, `_getTexImage`, `_getActiveTexture`, `lastBrightnessX/Y` |
| `glShaderSource` | `List<String>` → 拼接 → ByteBuffer | `String` → ByteBuffer (简化) |
| `_readPixels(long)` | 存在 | 移除（只用 ByteBuffer 重载） |
| `_glDeleteBuffers` | Linux 先 orphan | 直接 delete (Tracy 追踪) |
| `_glBindFramebuffer` | 直接 bind | **带缓存**: readFbo/writeFbo 去重 |
| `getFrameBuffer(target)` | 不存在 | 新增：返回缓存的 FBO |
| `_glDeleteFramebuffers` | 直接 delete | 同步清除 readFbo/writeFbo 缓存 |

### 10.3 新增方法

| 方法 | 说明 |
|---|---|
| `_glBufferSubData(int, long, ByteBuffer)` | 缓冲子区域更新 |
| `_glMapBufferRange(int, long, long, int)` | 范围映射 (替代 glMapBuffer) |
| `_stencilFuncFront(int, int, int)` | 前表面模板函数 |
| `_stencilFuncBack(int, int, int)` | 后表面模板函数 |
| `_stencilOpFront(int, int, int)` | 前表面模板操作 |
| `_stencilOpBack(int, int, int)` | 后表面模板操作 |
| `_enableStencilTest()` / `_disableStencilTest()` | 模板测试开关 |
| `_colorMask(int)` | 整型颜色掩码重载 |
| `clearGlErrors()` | `while(glGetError()!=0)` 清除残留错误 |
| `_drawArrays(int, int, int)` | `GL11.glDrawArrays` |

---

## 11. 线程模型

### 11.1 简化为单渲染线程

与 1.21.1 相同，仅保留 `renderThread` 概念。所有 `assertOnRenderThread()` 断言。

### 11.2 录制队列移除

**不再有 `recordRenderCall()` / `replayQueue()` / `ConcurrentLinkedQueue<RenderCall>`**。

录制队列机制被以下模式替代：
- **GPU Fence**: `queueFencedTask(Runnable)` 创建 GpuFence，异步任务在 fence 通过后由 `executePendingTasks()` 执行。
- **CommandEncoder 互斥**: RenderPass 内外操作互斥（`isInRenderPass()` 检查），天然串行化 GPU 命令。

### 11.3 GPU Fence 异步

```java
// RenderSystem
public static void queueFencedTask(Runnable task) {
    PENDING_FENCES.addLast(new GpuAsyncTask(task, getDevice().createCommandEncoder().createFence()));
}

public static void executePendingTasks() {
    for (GpuAsyncTask task = PENDING_FENCES.peekFirst(); task != null; task = PENDING_FENCES.peekFirst()) {
        if (!task.fence.awaitCompletion(0L)) return;  // 非阻塞轮询
        task.callback.run();
        task.fence.close();
        PENDING_FENCES.removeFirst();
    }
}
```

用于异步读取（`copyTextureToBuffer` 的回调）。

### 11.4 flipFrame 流程变化

**1.20.1**: `pollEvents → replayQueue → Tesselator.clear → glfwSwapBuffers → pollEvents`

**26.1.2**: `Tesselator.clear → getDevice().presentFrame() → Tracy endFrame → dynamicUniforms.reset() → levelRenderer.endFrame() → NeoForge FlipFrameEvent`

glfwSwapBuffers 和 pollEvents 封装到 `device.presentFrame()` 和 `pollEvents()` 中。

---

## 12. 关键不变量与约束

1. **Facade-Backend 分离**: 调用方永远通过 GpuDevice/CommandEncoder/RenderPass Facade 操作，不直接调用 GlStateManager。
2. **RenderPass 互斥**: 创建 RenderPass 后必须先 close 才能执行其他 CommandEncoder 命令。
3. **Usage Flag 强制验证**: 每个资源操作前检查 usage flag（如 vertexBuffer 必须有 USAGE_VERTEX）。
4. **Pipeline 不可变**: RenderPipeline 创建后不可修改，通过 Builder/Snippet 模式构建。
5. **Pipeline 编译缓存**: GlDevice 维护 IdentityHashMap pipelineCache 和 HashMap shaderCache，同一管线描述符只编译一次。
6. **Uniform 惰性绑定**: RenderPass 设置 uniform 不立即调 GL，仅在 draw 时绑定脏 uniform。
7. **纹理 FBO 缓存**: GlTexture 的 `getFbo()` 按 depth ID 缓存 FBO，避免重复创建。
8. **纹理延迟删除**: GlTexture.close() 等所有 GpuTextureView 关闭后才销毁 GL 资源。
9. **单 VB Slot**: `MAX_VERTEX_BUFFERS = 1`，当前实现仅支持 1 个顶点缓冲槽位。
10. **矩阵通过 UBO 传递**: Projection/ModelView 不再存储于 RenderSystem 静态字段，而是通过 GpuBufferSlice (UBO) 传入着色器。
11. **NeoForge 管线修改器**: PipelineModifierStack 允许运行时注入管线修改（如着色器替换）。
12. **无 gameThread 概念**: 与 1.21.1 相同，所有操作在渲染线程或通过 fence 异步化。
