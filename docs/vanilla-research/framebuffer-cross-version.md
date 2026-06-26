# Minecraft RenderTarget / Framebuffer / 后处理链 跨版本比较

> 比较版本: 1.20.1 (Forge) / 1.21.1 (NeoForge) / 26.1.2 (NeoForge)
> 基于源码分析,不确定处标注 "未确认"。

---

## 目录

1. [RenderTarget API 演进](#1-rendertarget-api-演进)
2. [MainTarget 演进](#2-maintarget-演进)
3. [后处理链架构演进](#3-后处理链架构演进)
4. [Shader/Program 模型演进](#4-shaderprogram-模型演进)
5. [Texture 管理演进](#5-texture-管理演进)
6. [最终输出 (blitToScreen) 演进](#6-最终输出-bitto屏演进)
7. [多 Target 输出系统演进](#7-多-target-输出系统演进)
8. [GameRenderer 中的 PostChain 实例](#8-gamerenderer-中的-postchain-实例)
9. [关键架构决策总结](#9-关键架构决策总结)

---

## 1. RenderTarget API 演进

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 底层后端 | OpenGL (FBO + GL Texture ID) | OpenGL (FBO + GL Texture ID) | GpuDevice (GpuTexture 句柄) |
| FBO ID | int frameBufferId | int frameBufferId | ❌ 不存在 |
| Color 纹理 | int colorTextureId | int colorTextureId | `GpuTexture colorTexture` |
| Depth 纹理 | int depthBufferId | int depthBufferId | `GpuTexture depthTexture` |
| Stencil | Forge patch `enableStencil()` | Neo patch `enableStencil()` | 构造参数 `useStencil` |
| bindRead() | ✓ | ✓ | ❌ 移除 |
| bindWrite() | ✓ | ✓ | ❌ 移除 |
| clear() | ✓ | ✓ | ❌ 移除 (由 Descriptor.prepare 替代) |
| blitToScreen | 全屏四边形 + Tesselator | 全屏四边形(归一化坐标) + Tesselator | `CommandEncoder.presentTexture()` |
| setFilterMode | public one param | private two params (force) | ❌ 移除 |
| setClearColor | ✓ | ✓ | ❌ 移除 (由 Descriptor.clearColor 替代) |
| copyDepthFrom | glBlitFrameBuffer | glBlitFrameBuffer | CommandEncoder.copyTextureToTexture |
| 标签(label) | ❌ | ❌ | String label (调试用) |
| checkStatus | glCheckFramebufferStatus | glCheckFramebufferStatus | ❌ 移除 |
| 行数 | 328 | 294 | 138 |

### 1.1 关键变化

**1.20.1 → 1.21.1**: 微小重构
- Forge → NeoForge 命名空间
- blitToScreen 简化(归一化坐标)
- `setFilterMode` 添加 force 参数防止冗余 GL 调用
- 纹理参数 `null` 替代 `(IntBuffer)null`

**1.21.1 → 26.1.2**: 架构革命
- **废弃直接 GL 调用**,所有 GPU 操作通过 GpuDevice 抽象。
- RenderTarget 从"GL FBO 包装器"变为"GPU 纹理对(color+depth)"。
- 纹理管理从 `int` ID 变为不透明 GpuTexture 句柄。
- bind/unbind/clear/checkStatus 全部移除 — 这些是 GL 概念,在现代 GPU API 中无直接对应。

---

## 2. MainTarget 演进

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 构造线程检查 | recordRenderCall | 直接调用 | 直接调用 |
| FBO 创建 | 手动 glGenFramebuffer + 绑定 | 手动 glGenFramebuffer + 绑定 | GpuDevice.createTexture |
| 渐进分配检测 | GL_OUT_OF_MEMORY (1285) | GL_OUT_OF_MEMORY (1285) | GpuOutOfMemoryException |
| color 纹理格式 | GL_RGBA8 + GL_UNSIGNED_BYTE | GL_RGBA8 + GL_UNSIGNED_BYTE | TextureFormat.RGBA8 |
| depth 纹理格式 | GL_DEPTH_COMPONENT + GL_FLOAT | GL_DEPTH_COMPONENT + GL_FLOAT | TextureFormat.DEPTH32 |
| Stencil 支持 | 无构造参数 | 无构造参数 | 构造参数 `enableStencil` |
| DEFAULT 尺寸 | 854×480 | 854×480 | 854×480 |
| TextureView | ❌ | ❌ | ✓ (通过 createTextureView) |

---

## 3. 后处理链架构演进

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 配置解析 | Gson JSON 手动解析 | Gson JSON 手动解析 | Codec 系统 |
| 配置位置 | shaders/post/*.json | shaders/post/*.json | post_effect/*.json |
| target 定义 | 字符串 + JSON object | 字符串 + JSON object | InternalTarget record (width/height/persistent/clearColor) |
| pass 输入 | intarget + outtarget + auxtargets | intarget + outtarget + auxtargets | inputs 列表 (sealed) + output |
| 执行模式 | 立即执行(GL 调用) | 立即执行(GL 调用) | 延迟执行(FrameGraph 录制) |
| 资源管理 | 手动 new TextureTarget | 手动 new TextureTarget | FrameGraph internal targets |
| 跨帧资源 | ❌ 每帧新建 | ❌ 每帧新建 | persistent targets 选项 |
| 时间管理 | 自己累积 time | 自己累积 time | 移入 PostChain(被废弃的 process) |
| filterMode | 无管理 | process 中自动切换 | ❌ (由 sampler 控制) |
| 效果链 | 24 个硬编码效果路径 | 3 个实体触发 + blur | 由 ShaderManager 懒加载 |
| addPass 签名 | (String, RT, RT) | (String, RT, RT, boolean) | 由 createPass 处理 |

### 3.1 EffectInstance 命运

- 1.20.1/1.21.1: `EffectInstance` 是核心,管理 GL program + uniforms + samplers。
- 26.1.2: **`EffectInstance` 完全移除**,功能分散到:
  - `RenderPipeline`: shader 编译和绑定
  - `GpuBuffer` (UBO): uniform 数据
  - `CommandEncoder` / `RenderPass`: 命令录制

---

## 4. Shader/Program 模型演进

| 特性 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| Program 类型 | EffectInstance (GL program) | RenderPipeline (GPU pipeline) |
| Shader 源 | .vsh / .fsh 文件 | .glsl 文件 |
| Sampler 声明 | JSON "samplers" 数组 | Pipeline builder "withSampler" |
| Uniform 声明 | JSON "uniforms" 数组 | Pipeline builder "withUniform" (UBO) |
| Uniform 更新 | Uniform.set() → upload | Std140Builder → GpuBuffer.write |
| Blend 控制 | BlendMode (JSON + GL调用) | ColorTargetState (Pipeline 内建) |
| Vertex 格式 | DefaultVertexFormat.POSITION | DefaultVertexFormat.EMPTY (TRIANGLES) |
| 编译时机 | 资源加载时 | 预编译 (device.precompilePipeline) |
| Shader 导入 | ❌ | GLSL `#include` + GlslPreprocessor |

### 4.1 后处理 Shader 的 Vertex 输入

- 1.20.1/1.21.1: PostPass 手动构造全屏四边形顶点(POSITION 格式),z=500。
- 26.1.2: `POST_PROCESSING_SNIPPET` 使用 `DefaultVertexFormat.EMPTY` + `VertexFormat.Mode.TRIANGLES`,通过 `renderPass.draw(0, 3)` 绘制 3 个顶点。顶点数据由 GPU 内置生成(全屏三角形)。

---

## 5. Texture 管理演进

| 特性 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| 纹理标识 | int (GL texture name) | `GpuTexture` 句柄 |
| 纹理视图 | 无 (直接 bind) | `GpuTextureView` |
| 纹理创建 | TextureUtil.generateTextureId | GpuDevice.createTexture |
| 纹理释放 | TextureUtil.releaseTextureId | GpuTexture.close() |
| Sampler 对象 | 无 (texParameter) | `GpuSampler` (创建时指定过滤) |
| 纹理复制 | glBlitFrameBuffer | CommandEncoder.copyTextureToTexture |
| 纹理清空 | clear (glClear) | CommandEncoder.clearColorTexture |
| MRT | ❌ (单 color attachment) | 未确认是否支持多 color attachment |

---

## 6. 最终输出 (blitToScreen) 演进

### 6.1 1.20.1

```java
// 全屏四边形,Tesselator,手动 Matrix4f 正交投影
// UV 缩放: viewWidth/width, viewHeight/height
// blitShader (DiffuseSampler = colorTextureId)
// 顶点: (0,height), (width,height), (width,0), (0,0)
```

### 6.2 1.21.1

```java
// 归一化坐标 (0,0) → (1,1)
// DefaultVertexFormat.BLIT_SCREEN
// 不再手动构建 Matrix4f
```

### 6.3 26.1.2

```java
RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTextureView);
```

- 直接提交纹理到 swapchain,无全屏四边形绘制。
- EntityOutline 使用 `blitAndBlendToTexture()` — 通过 RenderPass 录制到指定输出纹理。

---

## 7. 多 Target 输出系统演进

### 7.1 1.20.1 / 1.21.1 (LevelRenderer)

```java
// Target 来源: transparency.json PostChain 的 getTempTarget()
entityTarget      // entity_outline.json → "final"
translucentTarget // transparency.json → "translucent"
itemEntityTarget  // transparency.json → "itemEntity"
particlesTarget   // transparency.json → "particles"
weatherTarget     // transparency.json → "weather"
cloudsTarget      // transparency.json → "clouds"
```

- Target 生命周期: 绑在 LevelRenderer 上,resize 时重建。
- 渲染流程: 各 target 独立渲染 → transparencyChain.process() 合成 → entityTarget.blitToScreen() 叠加 → postEffect.process() 滤镜。

### 7.2 26.1.2 (LevelTargetBundle)

```java
MAIN_TARGET_ID       = "minecraft:main"
TRANSLUCENT_TARGET_ID = "minecraft:translucent"
ITEM_ENTITY_TARGET_ID = "minecraft:item_entity"
PARTICLES_TARGET_ID   = "minecraft:particles"
WEATHER_TARGET_ID     = "minecraft:weather"
CLOUDS_TARGET_ID      = "minecraft:clouds"
ENTITY_OUTLINE_TARGET_ID = "minecraft:entity_outline"
```

- Target 是 FrameGraph 管理的 ResourceHandle<RenderTarget>,而非裸 RenderTarget 引用。
- LevelTargetBundle 实现 `PostChain.TargetBundle`,供 PostChain.addToFrame 访问。
- 不再有独立的 PostChain (transparencyChain/entityEffect) 在 LevelRenderer,而是通过 FrameGraph 统一管理。

---

## 8. GameRenderer 中的 PostChain 实例

| 版本 | PostChain 实例 | 管理方式 |
|---|---|---|
| 1.20.1 | postEffect (F4 循环+实体触发,24个效果) | GameRenderer 直接持有 |
| 1.21.1 | postEffect (实体触发,3个效果) + blurEffect | GameRenderer 直接持有 |
| 26.1.2 | 按需(实体触发+blur) | ShaderManager 懒加载,仅存 postEffectId |

### 8.1 效果列表演变

**1.20.1**: 24 个 F4 循环效果 (notch, fxaa, art, bumpy, blobs2, pencil, color_convolve, deconverge, flip, invert, ntsc, outline, phosphor, scan_pincushion, sobel, bits, desaturate, green, blur, wobble, blobs, antialias, creeper, spider)

**1.21.1**: 仅实体触发 (creeper → creeper.json, spider → spider.json, enderman → invert.json) + 新 blurEffect (菜单背景模糊)

**26.1.2**: 同 1.21.1,但通过 ShaderManager 统一管理

### 8.2 ShaderInstance 演变

| 版本 | ShaderInstance 管理 |
|---|---|
| 1.20.1 | GameRenderer 持有 60+ static ShaderInstance 字段 |
| 1.21.1 | 减少,部分改为按需获取 |
| 26.1.2 | 全部移除,由 RenderPipeline + ShaderManager 替代 |

---

## 9. 关键架构决策总结

### 9.1 从 GL 直接调用到 GPU 抽象层

26.1.2 最根本的变化: 代码不再直接调用 GL 函数。所有 GPU 操作通过 `GpuDevice` → `CommandEncoder` → `RenderPass` 链条。这使得:
- 后端可切换 (理论上可支持 Vulkan/Metal/DirectX)。
- 命令录制和提交分离 (类似现代 API 的 command buffer 模式)。
- 资源生命周期由 FrameGraph 管理,自动复用。

### 9.2 从即时执行到 FrameGraph 延迟执行

1.20.1/1.21.1: PostChain.process() 立即执行 GL 调用。
26.1.2: PostChain.addToFrame() 声明资源依赖和命令,由 FrameGraphBuilder.execute() 统一调度执行。

### 9.3 从 Gson 到 Codec

配置解析从 `GsonHelper` 手动解析 JSON 变为 `PostChainConfig.CODEC` (Mojang DataFixerUpper Codec 系统)。这意味着:
- 类型安全 (编译期检查)
- 更好的错误信息
- 支持 schema 演进 (datafixer)

### 9.4 Shader 从 Program → Pipeline

不再有 Effect + EffectProgram 的分层,统一为 RenderPipeline。Pipeline 包含完整的渲染状态 (blend, depth/stencil, vertex format, shader stages, uniforms, samplers)。

### 9.5 Uniform 从直接上传 → UBO

Uniform 不再逐个 `glUniform*()` 上传,而是预编译为 `GpuBuffer` (UniformBufferObject),通过 `Std140Builder` 写入。

### 9.6 纹理过滤从 Target 级 → Sampler 级

1.20.1/1.21.1: `RenderTarget.setFilterMode` 控制纹理过滤。
26.1.2: 纹理过滤由 `GpuSampler` 控制 (创建时指定,如 `FilterMode.NEAREST` / `FilterMode.LINEAR`)。
- PostPass 的 inputs 通过 `samplerCache.getClampToEdge(bilinear ? LINEAR : NEAREST)` 获取对应 GpuSampler。
- 1.21.1 的 `PostChain.setFilterMode` / `PostPass.getFilterMode` 模式在 26.1.2 消失。

---

## 附录: 跨版本不变项

1. **MainTarget 默认尺寸**: 854×480 (三版本相同)。
2. **Depth buffer**: MainTarget 始终带 depth buffer。
3. **渐进式分配**: MainTarget 始终先尝试用户尺寸,失败回退 DEFAULT。
4. **后处理 Pass 顺序执行**: 始终按 JSON/Codec 定义顺序。
5. **entity_outline + transparency 双链模式**: 1.20.1/1.21.1 存在;26.1.2 演进为 FrameGraph,但逻辑等价。
6. **"minecraft:main" target ID**: 三版本均作为主 framebuffer 标识。
