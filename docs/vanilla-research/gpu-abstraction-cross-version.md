# GPU 抽象层 — 跨版本对比 (1.20.1 / 1.21.1 / 26.1.2)

> 三版本完整对比，聚焦架构演进。

## 目录

1. [类/包变化表](#1-类包变化表)
2. [GL 直调 → GPU 抽象的演进](#2-gl-直调--gpu-抽象的演进)
3. [RenderSystem 瘦身对照](#3-rendersystem-瘦身对照)
4. [RenderPass 模型 vs 即时 GL 调用](#4-renderpass-模型-vs-即时-gl-调用)
5. [资源生命周期演进](#5-资源生命周期演进)
6. [线程模型演进](#6-线程模型演进)
7. [矩阵传递演进](#7-矩阵传递演进)

---

## 1. 类/包变化表

### 1.1 包结构演进

| 包 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| `com.mojang.blaze3d.systems` | RenderSystem, TimerQuery | RenderSystem, TimerQuery | RenderSystem(瘦身), GpuDevice, CommandEncoder, RenderPass, GpuDeviceBackend, CommandEncoderBackend, RenderPassBackend, GpuQuery, ScissorState, SamplerCache |
| `com.mojang.blaze3d.platform` | GlStateManager, GLX, Window, ... | GlStateManager, GLX, Window, ... | GLX, Window, BackendOptions, ... (GlStateManager **迁移**) |
| `com.mojang.blaze3d.opengl` | **不存在** | **不存在** | **GlStateManager, GlDevice, GlRenderPass, GlCommandEncoder, GlBuffer, GlTexture, GlTextureView, GlRenderPipeline, GlProgram, GlShaderModule, GlSampler, GlFence, GlDebug, GlDebugLabel, DirectStateAccess, BufferStorage, VertexArrayCache, GlConst, Uniform, GlTimerQuery, GlBackend** |
| `com.mojang.blaze3d.buffers` | **不存在** | **不存在** | **GpuBuffer, GpuBufferSlice, GpuFence, Std140Builder, Std140SizeCalculator** |
| `com.mojang.blaze3d.textures` | **不存在** | **不存在** | **GpuTexture, GpuTextureView, GpuSampler, TextureFormat, AddressMode, FilterMode** |
| `com.mojang.blaze3d.pipeline` | RenderCall, RenderTarget, MainTarget, ... | RenderCall, RenderTarget, MainTarget, ... | **RenderPipeline, CompiledRenderPipeline, BlendFunction, ColorTargetState, DepthStencilState**, RenderTarget, MainTarget, TextureTarget |
| `com.mojang.blaze3d.framegraph` | **不存在** | **不存在** | **新增** |
| `com.mojang.blaze3d.resource` | **不存在** | **不存在** | **新增** |

### 1.2 核心类生命周期

| 概念 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 渲染门面 | `RenderSystem` (1082行, 静态方法) | `RenderSystem` (945行) | `RenderSystem` (418行, **-61%**) |
| GL 状态 | `GlStateManager` (platform, 962行) | `GlStateManager` (platform, 1005行) | `GlStateManager` (opengl, 708行, **-30%** vs 1.21.1) |
| 设备抽象 | 无 | 无 | `GpuDevice` + `GpuDeviceBackend` + `GlDevice` |
| 命令编码 | 无 | 无 | `CommandEncoder` + `CommandEncoderBackend` + `GlCommandEncoder` |
| 渲染通道 | 无 (即时 GL 调用) | 无 | `RenderPass` + `RenderPassBackend` + `GlRenderPass` |
| 管线描述 | `RenderStateShard` (碎片化) | `RenderStateShard` | `RenderPipeline` (不可变, Builder) |
| 缓冲抽象 | `VertexBuffer` (直接 GL) | `VertexBuffer` | `GpuBuffer` + `GlBuffer` |
| 纹理抽象 | `AbstractTexture` (直接 GL) | `AbstractTexture` | `GpuTexture` + `GlTexture` + `GpuTextureView` + `GlTextureView` |
| 录制队列 | `ConcurrentLinkedQueue<RenderCall>` | 同 1.20.1 | **移除**，用 `GpuFence` 异步 |
| 矩阵 | `Matrix4f` 静态字段 | `Matrix4fStack` 直接 | `GpuBufferSlice` (UBO) |

---

## 2. GL 直调 → GPU 抽象的演进

### 2.1 演进路径

```
1.20.1/1.21.1:               26.1.2:
┌──────────────┐             ┌─────────────────┐
│  RenderType  │             │  RenderType     │
│ (RenderState │             │  (RenderState   │
│  Shard 列表)  │             │   Shard 列表)    │
└──────┬───────┘             └───────┬─────────┘
       │ setupState                   │
       ▼                              ▼
┌──────────────┐             ┌─────────────────┐
│ RenderSystem │             │  RenderPipeline │
│ .depthFunc(  │             │  (不可变描述符)   │
│  GL_LEQUAL)  │             └───────┬─────────┘
│ .enableBlend │                     │ setPipeline
│ .blendFunc(  │                     ▼
│  SRC_ALPHA, │             ┌─────────────────┐
│  ONE_MINUS..)│             │   RenderPass    │
│ .setShader() │             │ .setPipeline()  │
│ .drawElements│             │ .setUniform()   │
└──────┬───────┘             │ .bindTexture()  │
       │ 直接 GL                  │ .drawIndexed()  │
       ▼                     └───────┬─────────┘
┌──────────────┐                     │
│ GlStateManager│                    ▼
│._depthFunc() │             ┌─────────────────┐
│._blendFunc() │             │ GlCommandEncoder│
│._drawElements│             │.applyPipeline() │
└──────┬───────┘             │.trySetup()      │
       │ __glXxx                   │.drawFromBuffers│
       ▼                     └───────┬─────────┘
   [LWJGL GL11/GL15/GL30]          │
                                    ▼
                              [LWJGL GL11/GL15/GL30/GL32]
```

### 2.2 核心差异

| 维度 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| **调用模式** | 即时 GL 状态设置，每帧大量 glEnable/glBlendFunc/glDepthFunc | RenderPipeline 预编译，切换时一次性 applyPipelineState |
| **状态封装** | RenderStateShard 碎片（每个状态一个 Shard 类） | RenderPipeline 整体（不可变，所有状态打包） |
| **跨帧缓存** | GlStateManager BooleanState（本地 boolean 去重） | GlCommandEncoder.lastPipeline（单指针比较去重） |
| **可移植性** | 零——所有调用直接绑定 LWJGL/GL | 理论可移植——Facade/Backend 分离 |
| **Uniform 绑定** | 每个 glUniform 调用即时执行 | 惰性：setUniform 写 map，draw 时仅绑定脏项 |
| **错误检查** | GlStateManager._getError() 手动 | VALIDATION 模式（IDE 内自动验证所有资源状态） |

---

## 3. RenderSystem 瘦身对照

### 3.1 方法分类统计

| 类别 | 1.20.1 | 1.21.1 | 26.1.2 | 去向 |
|---|---|---|---|---|
| 线程管理 | 10 | 6 | 3 | 简化 |
| 帧生命周期 | 5 | 5 | 2 | `flipFrame` 逻辑简化 |
| 深度/模板 | 9 | 9 | 0 | → RenderPipeline.depthStencilState + stencilTest |
| 混合 | 8 | 8 | 0 | → RenderPipeline.colorTargetState.blendFunction |
| 剔除/多边形 | 6 | 6 | 0 | → RenderPipeline.cull + polygonMode |
| 颜色逻辑 | 3 | 3 | 0 | → 移除 |
| 纹理操作 | 10 | 10 | 0 | → RenderPass.bindTexture / CommandEncoder.writeToTexture |
| Viewport/掩码 | 3 | 3 | 0 | → RenderPass.setViewport |
| 清除 | 4 | 4 | 0 | → CommandEncoder.clearColorTexture 等 |
| Shader/Fog/Light | 20 | 20 | 4 | Fog/Light → GpuBufferSlice UBO; setShader → RenderPipeline |
| 矩阵 | 14 | 13 | 4 | → GpuBufferSlice UBO |
| 后端初始化 | 5 | 5 | 3 | `initRenderer(GpuDevice)` 替代 |
| GL 缓冲 | 7 | 7 | 0 | → GpuDevice.createBuffer / CommandEncoder |
| Uniform | 11 | 11 | 0 | → RenderPass.setUniform |
| 绘制 | 3 | 3 | 0 | → RenderPass.drawIndexed |
| 录制队列 | 3 | 3 | 0 | → 移除，Fence 替代 |
| **新增: Device/Uniforms** | 0 | 0 | 10 | getDevice, getDynamicUniforms, bindDefaultUniforms, Fence, PipelineModifier |
| **总计** | ~121 | **~116** | **~26** (-78%) | |

### 3.2 完全移除的方法（部分列表）

| 1.20.1 方法 | 26.1.2 替代 |
|---|---|
| `enableBlend()` / `disableBlend()` | RenderPipeline.colorTargetState.blendFunction |
| `blendFunc(src, dst)` / `blendFuncSeparate(...)` | RenderPipeline.colorTargetState.blendFunction |
| `enableCull()` / `disableCull()` | RenderPipeline.cull |
| `depthFunc(GL_LEQUAL)` / `depthMask(true/false)` | RenderPipeline.depthStencilState |
| `enableDepthTest()` / `disableDepthTest()` | RenderPipeline.depthStencilState (nullable) |
| `setShader(Supplier<ShaderInstance>)` | RenderPass.setPipeline(RenderPipeline) |
| `setShaderTexture(unit, id)` | RenderPass.bindTexture(name, view, sampler) |
| `glUniform*(location, value)` | RenderPass.setUniform(name, GpuBufferSlice) |
| `drawElements(mode, count, type)` | RenderPass.drawIndexed(baseVertex, firstIndex, indexCount, instanceCount) |
| `viewport(x,y,w,h)` | RenderPass.setViewport(x,y,w,h) |
| `clearColor/clearDepth/clear(mask)` | CommandEncoder.clearColorTexture/clearDepthTexture |
| `glGenBuffers/glDeleteBuffers/glBufferData` | GpuDevice.createBuffer / buffer.close() |
| `setProjectionMatrix(Matrix4f)` | setProjectionMatrix(GpuBufferSlice, ProjectionType) |
| `recordRenderCall(RenderCall)` | 移除，Fence 替代 |
| `replayQueue()` | 移除 |
| `isOnGameThread()` / `assertOnGameThread()` | 移除 |

---

## 4. RenderPass 模型 vs 即时 GL 调用

### 4.1 执行模型对比

**1.20.1/1.21.1 — 即时 GL 调用**:
```java
// 每帧渲染：碎片化状态设置
RenderSystem.setShader(() -> shaderInstance);
RenderSystem.enableBlend();
RenderSystem.blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA);
RenderSystem.disableCull();
RenderSystem.depthFunc(GL_LEQUAL);
RenderSystem.depthMask(true);
RenderSystem.setShaderTexture(0, textureId);
shaderInstance.safeGetUniform("ModelViewMat").set(matrix);
RenderSystem.drawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT);

// 每个状态改变都触发一次 GL 调用（即使 GlStateManager 缓存了）
```

**26.1.2 — RenderPass + Pipeline**:
```java
// 管线预定义（启动时一次性）
RenderPipeline pipeline = RenderPipeline.builder()
    .withLocation("my_pipeline")
    .withVertexShader("core/position_tex")
    .withFragmentShader("core/position_tex")
    .withUniform("ModelViewProjMat", UNIFORM_BUFFER)
    .withSampler("Sampler0")
    .withDepthStencilState(new DepthStencilState(true, LEQUAL))
    .withColorTargetState(new ColorTargetState(
        Optional.of(new BlendFunction(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO)),
        ColorTargetState.WRITE_MASK_ALL))
    .withCull(false)
    .withVertexFormat(POSITION_TEX, TRIANGLES)
    .build();

// 每帧渲染：简洁
CommandEncoder encoder = device.createCommandEncoder();
RenderPass pass = encoder.createRenderPass(
    () -> "my pass", colorView, OptionalInt.empty(), depthView, OptionalDouble.of(1.0));
pass.setPipeline(pipeline);
pass.setUniform("ModelViewProjMat", uboSlice);
pass.bindTexture("Sampler0", textureView, sampler);
pass.setVertexBuffer(0, vertexBuffer);
pass.setIndexBuffer(indexBuffer, INT);
pass.drawIndexed(0, 0, indexCount, 1);
pass.close();
```

### 4.2 性能优势

| 优势 | 说明 |
|---|---|
| **状态打包** | 所有 blend/depth/cull/stencil 在一次 `applyPipelineState` 中设置，且仅在 lastPipeline 变化时执行 |
| **Uniform 惰性** | `setUniform` 不调 GL，draw 时仅绑定 `dirtyUniforms` |
| **管线编译缓存** | 同一 RenderPipeline 只编译一次 GL program |
| **FBO 缓存** | GlTexture 的 `getFbo()` 缓存 FBO，避免每帧创建/销毁 |
| **VertexArray 缓存** | GlDevice 的 `vertexArrayCache` 缓存 VAO 配置 |

### 4.3 drawMultipleIndexed — 批量多绘制

26.1.2 新增的 `drawMultipleIndexed` 允许在一次 RenderPass 中执行多个不同的绘制调用，每个调用可指定不同的 vertexBuffer/indexBuffer/uniform:

```java
List<RenderPass.Draw<T>> draws = List.of(
    new Draw<>(0, vb1, ib1, SHORT, 0, count1, 0, (arg, uploader) -> {
        uploader.upload("Tint", tintSlice);
    }),
    new Draw<>(0, vb2, ib2, SHORT, 0, count2, 0)
);
pass.drawMultipleIndexed(draws, null, null, collections.emptyList(), null);
```

---

## 5. 资源生命周期演进

### 5.1 缓冲

| 版本 | 创建 | 上传 | 绑定 | 销毁 |
|---|---|---|---|---|
| 1.20.1/1.21.1 | `GlStateManager._glGenBuffers()` → int | `_glBufferData` / `_glMapBuffer` / VertexBuffer.upload | `_glBindBuffer(GL_ARRAY_BUFFER, id)` (VAO 内) | `_glDeleteBuffers(int)` |
| 26.1.2 | `GpuDevice.createBuffer(label, usage, size)` → GpuBuffer | `CommandEncoder.writeToBuffer(slice, data)` / `mapBuffer` | `RenderPass.setVertexBuffer(slot, buffer)` / `setIndexBuffer` | `buffer.close()` |

**关键变化**:
- 从裸 int 句柄 → 类型安全对象（GpuBuffer, GlBuffer）
- Usage flag 在创建时声明并验证
- `close()` 负责完整清理（unmap persistent buffer → glDeleteBuffers → Tracy free）
- 仅 USAGE_COPY_DST 的缓冲才能执行 writeToBuffer

### 5.2 纹理

| 版本 | 创建 | 上传 | 视图 | 作为 RenderTarget | 销毁 |
|---|---|---|---|---|---|
| 1.20.1/1.21.1 | `TextureManager` / `AbstractTexture` | `NativeImage.upload` / `GlStateManager._texSubImage2D` | 无（直接 bind texture id） | `RenderTarget.createBuffers` → framebuffer | `TextureManager.release` |
| 26.1.2 | `GpuDevice.createTexture(label, usage, format, w, h, layers, mips)` → GpuTexture | `CommandEncoder.writeToTexture(GpuTexture, NativeImage)` | `GpuTextureView` (mip 范围视图) | `RenderTarget` 持有 GpuTexture + GpuTextureView 对 | `texture.close()` (延迟到所有 View 关闭) |

**关键变化**:
- GpuTextureView 分离纹理本身与 mip 范围/访问方式
- 延迟删除：`texture.close()` 不立即销毁，等所有 View 关闭
- FBO 缓存：GlTexture 内部缓存 FBO，避免重复分配
- Usage flag 独立控制：COPY_DST(写入) / COPY_SRC(读取) / TEXTURE_BINDING(采样) / RENDER_ATTACHMENT(FBO) / CUBEMAP

### 5.3 着色器

| 版本 | 编译 | 绑定 | Uniform |
|---|---|---|---|
| 1.20.1/1.21.1 | `ShaderInstance` 加载时编译(glCreateShader → glCompileShader → glLinkProgram) | `RenderSystem.setShader(Supplier<ShaderInstance>)` → `GlStateManager._glUseProgram` | `glUniform*` 逐个设置 |
| 26.1.2 | `RenderPipeline` 不可变描述符 → GlDevice 编译缓存(IdentityHashMap) | `RenderPass.setPipeline(RenderPipeline)` | `RenderPass.setUniform(name, GpuBufferSlice)` UBO + Sampler 名绑定 |

**关键变化**:
- 从 Supplier 延迟绑定 → RenderPipeline 不可变描述符
- 从手动编译 → 自动编译缓存（pipelineCache + shaderCache）
- 从逐个 glUniform → GpuBufferSlice UBO（Std140 布局）

---

## 6. 线程模型演进

| 维度 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| gameThread 概念 | 存在但禁用(`isOnGameThread→true`) | **移除** | **移除** |
| renderThread 概念 | 存在 | 存在 | 存在 |
| 录制队列 | `ConcurrentLinkedQueue<RenderCall>` | 同 1.20.1 | **移除** |
| 跨线程操作 | 非渲染线程 → recordRenderCall → 渲染线程 replay | 同 1.20.1 | GPU Fence (`queueFencedTask`) |
| 初始化阶段 | `beginInitialization()/finishInitialization()` | **移除** | **移除** |
| 线程断言方法 | 6 种 | 2 种 | 1 种(`assertOnRenderThread`) |

**Fence 替代录制队列**: 1.20.1 的录制队列用于延迟 shader/matrix/texture 设置到渲染线程。26.1.2 中这些操作已移到 RenderPass/CommandEncoder（天然在渲染线程上），录制队列不再需要。异步 GPU 回读通过 `GpuFence` + `queueFencedTask` 实现。

---

## 7. 矩阵传递演进

| 版本 | 投影矩阵 | 模型视图 | 纹理矩阵 | 传递方式 |
|---|---|---|---|---|
| 1.20.1 | `Matrix4f projectionMatrix` 静态字段 | `PoseStack modelViewStack` → `Matrix4f modelViewMatrix` 副本 | `Matrix4f textureMatrix` | ShaderInstance.safeGetUniform → `glUniformMatrix4` |
| 1.21.1 | 同 1.20.1 | `Matrix4fStack modelViewStack`（直接，无副本） | 同 1.20.1 | 同 1.20.1 |
| 26.1.2 | `GpuBufferSlice projectionMatrixBuffer` (UBO) | `Matrix4fStack modelViewStack` | **移除**（未确认纹理矩阵去向） | `RenderSystem.bindDefaultUniforms(RenderPass)` → UBO slot "Projection" |

**关键变化**: 矩阵从 RenderSystem 静态字段 + 逐个 `glUniform` → UBO Buffer + RenderPass Uniform slot。`bindDefaultUniforms()` 自动绑定 Projection/Fog/Globals/Lighting 四个标准 UBO。

---

## 总结：最关键的 5 条跨版本发现

1. **GL 直调 → 完整 GPU 抽象**: 26.1.2 引入了 Facade-Backend 模式(GpuDevice/CommandEncoder/RenderPass + Backend interface + GlDevice 实现)，从零可移植性跃升为理论跨 API 架构。

2. **状态碎片化 → 管线整体化**: 1.20.1 的 RenderStateShard 碎片 + RenderSystem 逐个方法设置，在 26.1.2 被 ReplacePipeline(不可变描述符) 替代。所有 blend/depth/cull/stencil 状态打包为一个不可变对象，切换时一次性应用。

3. **RenderSystem 从 121 方法缩减到 26 方法(-78%)**: 所有 GL 状态操作、纹理绑定、shader 设置、uniform 上传、绘制调用全部迁移到 RenderPass/CommandEncoder/RenderPipeline。

4. **资源管理从裸 int 句柄变为类型安全对象**: GpuBuffer/GpuTexture 带 usage flag + AutoCloseable + 生命周期验证。纹理支持延迟删除（等所有 View 关闭），FBO 缓存避免每帧分配。

5. **线程模型简化**: 1.20.1 的 gameThread(禁用)+renderThread+录制队列+isInInit 阶段，经 1.21.1 移除 gameThread/isInInit，最终在 26.1.2 完全移除录制队列，仅保留 renderThread + GpuFence 异步机制。
