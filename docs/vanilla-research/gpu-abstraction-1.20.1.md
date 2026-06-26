# GPU 抽象层 — 1.20.1 (Forge)

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [RenderSystem 静态门面](#2-rendersystem-静态门面)
3. [GlStateManager 状态缓存](#3-glstatemanager-状态缓存)
4. [线程模型](#4-线程模型)
5. [渲染录制队列](#5-渲染录制队列)
6. [纹理绑定](#6-纹理绑定)
7. [关键不变量与约束](#7-关键不变量与约束)

---

## 1. 类位置与职责

| 类名 | 包路径 | 文件 | 职责 |
|---|---|---|---|
| `RenderSystem` | `com.mojang.blaze3d.systems` | `RenderSystem.java` (1082行) | 静态门面，渲染状态入口 |
| `GlStateManager` | `com.mojang.blaze3d.platform` | `GlStateManager.java` (962行) | GL 状态管理，带缓存避免冗余 GL 调用 |
| `RenderCall` | `com.mojang.blaze3d.pipeline` | `RenderCall.java` | 渲染录制队列中的单个操作 |

**核心架构**: RenderSystem 是调用方 API，GlStateManager 是 GL 封装。没有 GPU 抽象层，所有调用直达 LWJGL OpenGL。

---

## 2. RenderSystem 静态门面

### 2.1 线程管理 (10 方法)

| 方法 | 线程要求 | 说明 |
|---|---|---|
| `initRenderThread()` | 调用线程即渲染线程 | 记录当前线程为渲染线程，仅能调用一次 |
| `isOnRenderThread()` | 任意 | `Thread.currentThread() == renderThread` |
| `isOnRenderThreadOrInit()` | 任意 | 初始化阶段或渲染线程 |
| `initGameThread(boolean)` | 渲染线程已存在 | 记录游戏主线程 |
| `isOnGameThread()` | 任意 | **永远返回 `true`**（断言被禁用） |
| `assertInInitPhase()` | — | 检查 `isInInitPhase()` |
| `assertOnGameThreadOrInit()` | — | 检查 `!isInInit && !isOnGameThread()` |
| `assertOnRenderThreadOrInit()` | — | 检查 `!isInInit && !isOnRenderThread()` |
| `assertOnRenderThread()` | — | 检查 `!isOnRenderThread()` |
| `assertOnGameThread()` | — | 检查 `!isOnGameThread()`（因 isOnGameThread 永真，永不抛异常） |

**关键设计**: `isOnGameThread()` 永远返回 `true`（第 127 行），意味着 gameThread 断言在 1.20.1 实际上是禁用的。渲染线程断言才是真正的守卫。

### 2.2 帧生命周期 (5 方法)

| 方法 | 说明 |
|---|---|
| `flipFrame(long)` | pollEvents → replayQueue → Tesselator.clear → glfwSwapBuffers → pollEvents |
| `replayQueue()` | 消费 `recordingQueue` 中的所有 RenderCall 并 execute |
| `recordRenderCall(RenderCall)` | 将 RenderCall 入队 `recordingQueue`（ConcurrentLinkedQueue） |
| `beginInitialization()` | 设置 `isInInit = true` |
| `finishInitialization()` | 设置 `isInInit = false`，replay 残留队列，若仍有残留则抛异常 |

### 2.3 深度/模板状态 (6 方法)

| 方法 | 线程 |
|---|---|
| `disableDepthTest()` | assertOnRenderThread → `GlStateManager._disableDepthTest()` |
| `enableDepthTest()` | assertOnGameThreadOrInit → `GlStateManager._enableDepthTest()` |
| `depthFunc(int)` | assertOnRenderThread → `GlStateManager._depthFunc(depthFunc)` |
| `depthMask(boolean)` | assertOnRenderThread → `GlStateManager._depthMask(flag)` |
| `enableScissor(int,int,int,int)` | assertOnGameThreadOrInit → enableScissorTest + scissorBox |
| `disableScissor()` | assertOnGameThreadOrInit → `GlStateManager._disableScissorTest()` |

### 2.4 混合状态 (6 方法)

| 方法 | 线程 |
|---|---|
| `enableBlend()` | assertOnRenderThread → `GlStateManager._enableBlend()` |
| `disableBlend()` | assertOnRenderThread → `GlStateManager._disableBlend()` |
| `blendFunc(SourceFactor,DestFactor)` | assertOnRenderThread → `GlStateManager._blendFunc(src,dst)` |
| `blendFunc(int,int)` | assertOnRenderThread → `GlStateManager._blendFunc(int,int)` |
| `blendFuncSeparate(SourceFactor,DestFactor,SourceFactor,DestFactor)` | assertOnRenderThread → `GlStateManager._blendFuncSeparate` |
| `blendFuncSeparate(int,int,int,int)` | assertOnRenderThread → `GlStateManager._blendFuncSeparate` |
| `blendEquation(int)` | assertOnRenderThread → `GlStateManager._blendEquation(mode)` |
| `defaultBlendFunc()` | 调用 `blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO)` |

### 2.5 剔除/多边形状态 (6 方法)

| 方法 | 线程 |
|---|---|
| `enableCull()` | assertOnRenderThread → `GlStateManager._enableCull()` |
| `disableCull()` | assertOnRenderThread → `GlStateManager._disableCull()` |
| `polygonMode(int,int)` | assertOnRenderThread → `GlStateManager._polygonMode(face,mode)` |
| `enablePolygonOffset()` | assertOnRenderThread → `GlStateManager._enablePolygonOffset()` |
| `disablePolygonOffset()` | assertOnRenderThread → `GlStateManager._disablePolygonOffset()` |
| `polygonOffset(float,float)` | assertOnRenderThread → `GlStateManager._polygonOffset(factor,units)` |

### 2.6 颜色逻辑操作 (3 方法)

| 方法 | 线程 |
|---|---|
| `enableColorLogicOp()` | assertOnRenderThread |
| `disableColorLogicOp()` | assertOnRenderThread |
| `logicOp(LogicOp)` | assertOnRenderThread |

### 2.7 纹理操作 (9 方法)

| 方法 | 线程 | 说明 |
|---|---|---|
| `activeTexture(int)` | assertOnRenderThread | 激活纹理单元 |
| `texParameter(int,int,int)` | 无断言 | 设置纹理参数 |
| `deleteTexture(int)` | assertOnGameThreadOrInit | 删除 GL 纹理 |
| `bindTextureForSetup(int)` | — | 别名，调用 `bindTexture(int)` |
| `bindTexture(int)` | 无断言 | 直接调 `GlStateManager._bindTexture(texture)` |
| `setShaderTexture(int,ResourceLocation)` | isOnRenderThread 检查 + recordingQueue | 通过 TextureManager 查纹理 ID |
| `setShaderTexture(int,int)` | isOnRenderThread 检查 + recordingQueue | 直接设置纹理 ID |
| `getShaderTexture(int)` | assertOnRenderThread | 从 `shaderTextures[12]` 数组读取 |
| `setupOverlayColor(IntSupplier,int)` | assertOnRenderThread | 设置叠加纹理 `setShaderTexture(1, id)` |
| `teardownOverlayColor()` | assertOnRenderThread | 清理 `setShaderTexture(1, 0)` |

### 2.8 Viewport/颜色掩码 (3 方法)

| 方法 | 线程 |
|---|---|
| `viewport(int,int,int,int)` | assertOnGameThreadOrInit → `GlStateManager._viewport` |
| `colorMask(boolean,boolean,boolean,boolean)` | assertOnRenderThread → `GlStateManager._colorMask` |
| `lineWidth(float)` | isOnRenderThread 检查 + recordingQueue |

### 2.9 模板操作 (3 方法)

| 方法 | 线程 |
|---|---|
| `stencilFunc(int,int,int)` | assertOnRenderThread |
| `stencilMask(int)` | assertOnRenderThread |
| `stencilOp(int,int,int)` | assertOnRenderThread |

### 2.10 清除操作 (4 方法)

| 方法 | 线程 |
|---|---|
| `clearDepth(double)` | assertOnGameThreadOrInit |
| `clearColor(float,float,float,float)` | assertOnGameThreadOrInit |
| `clearStencil(int)` | assertOnRenderThread |
| `clear(int,boolean)` | assertOnGameThreadOrInit → `GlStateManager._clear(mask, checkError)` |

### 2.11 Shader 管理 (4 方法)

| 方法 | 说明 |
|---|---|
| `setShader(Supplier<ShaderInstance>)` | isOnRenderThread 检查 + recordingQueue，**延迟绑定**——shaderSupplier.get() 在录制或执行时调用 |
| `getShader()` | assertOnRenderThread，返回 `@Nullable ShaderInstance` |

**延迟绑定机制**: 非渲染线程调用时，shader 的 Supplier.get() 被推迟到 `replayQueue()` 执行时调用（渲染线程上）。这允许在主线程记录着色器切换命令。

### 2.11b Fog/Light/Color 着色器参数 (16 方法)

| 方法 | 说明 |
|---|---|
| `setShaderFogStart(float)` | 设置雾起始距离 |
| `getShaderFogStart()` | 获取雾起始距离 |
| `setShaderFogEnd(float)` | 设置雾结束距离 |
| `getShaderFogEnd()` | 获取雾结束距离 |
| `setShaderFogColor(float,float,float,float)` | 设置雾颜色 RGBA |
| `setShaderFogColor(float,float,float)` | 设置雾颜色 RGB(alpha=1.0) |
| `getShaderFogColor()` | 返回 `float[4]` |
| `setShaderFogShape(FogShape)` | SPHERE 或 CYLINDER |
| `getShaderFogShape()` | 返回 FogShape |
| `setShaderGlintAlpha(float/double)` | 设置闪烁透明度 |
| `getShaderGlintAlpha()` | 获取闪烁透明度 |
| `setShaderLights(Vector3f,Vector3f)` | 设置两个光照方向 |
| `setupShaderLights(ShaderInstance)` | 将 Lights 写入 ShaderInstance 的 LIGHT0/LIGHT1 uniform |
| `setShaderColor(float,float,float,float)` | isOnRenderThread 检查 + recordingQueue |
| `getShaderColor()` | 返回 `float[4]` |
| `setShaderGameTime(long,float)` | isOnRenderThread 检查 + recordingQueue，归一化到 [0,1] |
| `getShaderGameTime()` | assertOnRenderThread |

### 2.12 矩阵操作 (14 方法)

| 方法 | 说明 |
|---|---|
| `setProjectionMatrix(Matrix4f,VertexSorting)` | isOnRenderThread 检查 + recordingQueue，深拷贝 matrix |
| `getProjectionMatrix()` | assertOnRenderThread |
| `setInverseViewRotationMatrix(Matrix3f)` | isOnRenderThread 检查 + recordingQueue |
| `getInverseViewRotationMatrix()` | assertOnRenderThread |
| `setTextureMatrix(Matrix4f)` | isOnRenderThread 检查 + recordingQueue |
| `resetTextureMatrix()` | isOnRenderThread 检查 + recordingQueue，设为单位矩阵 |
| `getTextureMatrix()` | assertOnRenderThread |
| `applyModelViewMatrix()` | isOnRenderThread 检查 + recordingQueue，从 modelViewStack 深拷贝 |
| `getModelViewMatrix()` | assertOnRenderThread |
| `getModelViewStack()` | 无断言，直接返回 PoseStack |
| `backupProjectionMatrix()` | isOnRenderThread 检查 + recordingQueue |
| `restoreProjectionMatrix()` | isOnRenderThread 检查 + recordingQueue |
| `getVertexSorting()` | assertOnRenderThread |

### 2.13 后端初始化 (5 方法)

| 方法 | 说明 |
|---|---|
| `initBackendSystem()` | assertInInitPhase → `GLX._initGlfw()`，返回 NanoTimeSource |
| `initRenderer(int,boolean)` | assertInInitPhase → `GLX._init(debugVerbosity, synchronous)` |
| `setErrorCallback(GLFWErrorCallbackI)` | assertInInitPhase → `GLX._setGlfwErrorCallback` |
| `setupDefaultState(int,int,int,int)` | assertInInitPhase: clearDepth(1.0), enableDepthTest, depthFunc(515=LEQUAL), 矩阵 identity, viewport |
| `maxSupportedTextureSize()` | 探测实际最大纹理尺寸，从 max(32768, reported) 向下二分，GL 探测失败时 fallback |

### 2.14 GL 缓冲操作 (7 方法)

| 方法 | 说明 |
|---|---|
| `glBindBuffer(int,IntSupplier)` | 无断言，直接调 `GlStateManager._glBindBuffer` |
| `glBindVertexArray(Supplier<Integer>)` | 无断言，直接调 `GlStateManager._glBindVertexArray` |
| `glBufferData(int,ByteBuffer,int)` | assertOnRenderThreadOrInit |
| `glDeleteBuffers(int)` | assertOnRenderThread |
| `glDeleteVertexArrays(int)` | assertOnRenderThread |
| `glGenBuffers(Consumer<Integer>)` | isOnRenderThread 检查 + recordingQueue |
| `glGenVertexArrays(Consumer<Integer>)` | isOnRenderThread 检查 + recordingQueue |

### 2.15 Uniform 操作 (11 方法)

| 方法 | 说明 |
|---|---|
| `glUniform1i(int,int)` | assertOnRenderThread |
| `glUniform1/2/3/4(int,IntBuffer)` | assertOnRenderThread |
| `glUniform1/2/3/4(int,FloatBuffer)` | assertOnRenderThread |
| `glUniformMatrix2/3/4(int,boolean,FloatBuffer)` | assertOnRenderThread |

### 2.16 绘制操作 (3 方法)

| 方法 | 说明 |
|---|---|
| `drawElements(int,int,int)` | assertOnRenderThread → `GlStateManager._drawElements(mode,count,type,0L)` |
| `renderThreadTesselator()` | 返回渲染线程专用 Tessellator 单例 |
| `getSequentialBuffer(VertexFormat.Mode)` | 返回共享顺序索引缓冲（Triangles/Lines/Quads） |

### 2.17 其他 (9 方法)

| 方法 | 说明 |
|---|---|
| `pixelStore(int,int)` | assertOnGameThreadOrInit |
| `readPixels(...)` | assertOnRenderThread |
| `getString(int,Consumer<String>)` | assertOnRenderThread |
| `getBackendDescription()` | assertInInitPhase |
| `getApiDescription()` | 无断言，返回 OpenGL 版本字符串 |
| `renderCrosshair(int)` | assertOnRenderThread |
| `getCapsString()` | assertOnRenderThread，硬编码返回 "Using framebuffer using OpenGL 3.2" |
| `runAsFancy(Runnable)` | @Deprecated |
| `setupLevelDiffuseLighting(Vector3f,Vector3f,Matrix4f)` | assertOnRenderThread → GlStateManager |
| `setupGuiFlatDiffuseLighting(Vector3f,Vector3f)` | assertOnRenderThread → GlStateManager |
| `setupGui3DDiffuseLighting(Vector3f,Vector3f)` | assertOnRenderThread → GlStateManager |

---

## 3. GlStateManager 状态缓存

### 3.1 内部状态类设计

GlStateManager 使用"状态对象 + BooleanState 门面"模式缓存 GL 状态，避免冗余 GL 调用：

```java
// 每个 GL 能力用 BooleanState 包装(GL11.glEnable/glDisable 封装)
static class BooleanState {
    private final int state;  // GL enum (e.g. GL11.GL_BLEND = 3042)
    private boolean enabled;
    void enable()  { setEnabled(true); }
    void disable() { setEnabled(false); }
    void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            if (enabled) GL11.glEnable(state);
            else GL11.glDisable(state);
        }
    }
}
```

### 3.2 状态缓存项清单

| 状态对象 | 涉及的缓存字段 | 缓存逻辑 |
|---|---|---|
| `BLEND` (BlendState) | mode (BooleanState, GL_BLEND=3042), srcRgb=1, dstRgb=0, srcAlpha=1, dstAlpha=0 | `_blendFunc`: 检查 `srcRgb != BLEND.srcRgb \|\| dstRgb != BLEND.dstRgb` |
| `DEPTH` (DepthState) | mode (BooleanState, GL_DEPTH_TEST=2929), func=513(LEQUAL), mask=true | `_depthFunc`: 检查 `func != DEPTH.func`; `_depthMask`: 检查 `flag != DEPTH.mask` |
| `CULL` (CullState) | enable (BooleanState, GL_CULL_FACE=2884), mode=1029(BACK) | BooleanState 自身缓存 enable/disable |
| `POLY_OFFSET` (PolygonOffsetState) | fill (BooleanState, GL_POLYGON_OFFSET_FILL=32823), line (BooleanState, 10754), factor, units | `_polygonOffset`: 检查 `factor != POLY_OFFSET.factor \|\| units != POLY_OFFSET.units` |
| `COLOR_LOGIC` (ColorLogicState) | enable (BooleanState, GL_COLOR_LOGIC_OP=3058), op=5379(COPY) | `_logicOp`: 检查 `logicOperation != COLOR_LOGIC.op` |
| `STENCIL` (StencilState) | func (StencilFunc: func=519(ALWAYS), ref, mask=-1), mask=-1, fail/zfail/zpass=7680(KEEP) | `_stencilFunc`: 三字段全等检查; `_stencilMask`: mask 检查; `_stencilOp`: fail/zfail/zpass 全等检查 |
| `SCISSOR` (ScissorState) | mode (BooleanState, GL_SCISSOR_TEST=3089) | BooleanState 自身缓存 |
| `COLOR_MASK` (ColorMask) | red/green/blue/alpha (全 true) | `_colorMask`: 四字段全等检查 |
| `TEXTURES[12]` (TextureState[]) | binding (每纹理单元当前绑定) | `_bindTexture`: 检查 `texture != TEXTURES[activeTexture].binding` |
| `activeTexture` | 当前激活纹理单元 | `_activeTexture`: 检查 `activeTexture != texture - GL_TEXTURE0` |

### 3.3 纹理激活缓存

```java
// _activeTexture 带偏移检查
public static void _activeTexture(int texture) {
    if (activeTexture != texture - '\u84c0') {  // '\u84c0' = 33984 = GL_TEXTURE0
        activeTexture = texture - '\u84c0';
        glActiveTexture(texture);  // GL13.glActiveTexture
    }
}
```

纹理单元号 `GL_TEXTURE0 + n` 被转换为 0-based 索引 `n` 用于内部缓存。

### 3.4 Blit/FrameBuffer 操作（无状态缓存）

以下 GL 操作直接透传，无缓存：

| 方法 | GL 调用 |
|---|---|
| `_glBindFramebuffer(int,int)` | `GL30.glBindFramebuffer(target, framebuffer)` |
| `_glBlitFrameBuffer(...)` | `GL30.glBlitFramebuffer(...)` |
| `_glBindRenderbuffer(int,int)` | `GL30.glBindRenderbuffer(target, renderBuffer)` |
| `_glDeleteRenderbuffers(int)` | `GL30.glDeleteRenderbuffers(renderBuffer)` |
| `_glDeleteFramebuffers(int)` | `GL30.glDeleteFramebuffers(frameBuffer)` |
| `glGenFramebuffers()` | `GL30.glGenFramebuffers()` |
| `glGenRenderbuffers()` | `GL30.glGenRenderbuffers()` |
| `_glRenderbufferStorage(...)` | `GL30.glRenderbufferStorage(...)` |
| `_glFramebufferRenderbuffer(...)` | `GL30.glFramebufferRenderbuffer(...)` |
| `glCheckFramebufferStatus(int)` | `GL30.glCheckFramebufferStatus(target)` |
| `_glFramebufferTexture2D(...)` | `GL30.glFramebufferTexture2D(...)` |

### 3.5 缓冲操作

| 方法 | 说明 |
|---|---|
| `_glGenBuffers()` | `GL15.glGenBuffers()` |
| `_glGenVertexArrays()` | `GL30.glGenVertexArrays()` |
| `_glBindBuffer(int,int)` | `GL15.glBindBuffer(target, buffer)` |
| `_glBindVertexArray(int)` | `GL30.glBindVertexArray(array)` |
| `_glBufferData(int,ByteBuffer,int)` | `GL15.glBufferData(target, data, usage)` |
| `_glBufferData(int,long,int)` | `GL15.glBufferData(target, size, usage)` - 仅大小，用于 orphan |
| `_glMapBuffer(int,int)` | `GL15.glMapBuffer(target, access)` - 返回 `@Nullable ByteBuffer` |
| `_glUnmapBuffer(int)` | `GL15.glUnmapBuffer(target)` |
| `_glDeleteBuffers(int)` | Linux 下先 orphan 再 delete，其他平台直接 delete |

### 3.6 枚举类型

**SourceFactor (15 值)**: CONSTANT_ALPHA(32771), CONSTANT_COLOR(32769), DST_ALPHA(772), DST_COLOR(774), ONE(1), ONE_MINUS_CONSTANT_ALPHA(32772), ONE_MINUS_CONSTANT_COLOR(32770), ONE_MINUS_DST_ALPHA(773), ONE_MINUS_DST_COLOR(775), ONE_MINUS_SRC_ALPHA(771), ONE_MINUS_SRC_COLOR(769), SRC_ALPHA(770), SRC_ALPHA_SATURATE(776), SRC_COLOR(768), ZERO(0)

**DestFactor (14 值)**: CONSTANT_ALPHA(32771), CONSTANT_COLOR(32769), DST_ALPHA(772), DST_COLOR(774), ONE(1), ONE_MINUS_CONSTANT_ALPHA(32772), ONE_MINUS_CONSTANT_COLOR(32770), ONE_MINUS_DST_ALPHA(773), ONE_MINUS_DST_COLOR(775), ONE_MINUS_SRC_ALPHA(771), ONE_MINUS_SRC_COLOR(769), SRC_ALPHA(770), SRC_COLOR(768), ZERO(0)

**LogicOp (16 值)**: AND, AND_INVERTED, AND_REVERSE, CLEAR, COPY, COPY_INVERTED, EQUIV, INVERT, NAND, NOOP, NOR, OR, OR_INVERTED, OR_REVERSE, SET, XOR

---

## 4. 线程模型

### 4.1 双线程分离

```
┌─────────────┐          ┌──────────────┐
│  gameThread │          │ renderThread │
│  (主逻辑)    │          │  (GL 操作)    │
└──────┬──────┘          └──────┬───────┘
       │ recordRenderCall()     │ replayQueue()
       │ ─────────────────────► │ execute()
       │ (ConcurrentLinkedQueue)│
       │                        │
       │ assertOnGameThreadOrInit()      │ assertOnRenderThread()
       │ (depth/clear/viewport等)        │ (blend/cull/depthFunc等)
```

**gameThread**: 
- 负责世界更新、实体逻辑
- 可调用 `enableDepthTest()`, `enableScissor()`, `viewport()`, `clearDepth()`, `clearColor()`, `clear()` 等（标记 `assertOnGameThreadOrInit`）
- `isOnGameThread()` 始终返回 true（断言实际被绕过）

**renderThread**:
- 负责所有 GL 调用
- 大部分状态修改方法标记 `assertOnRenderThread()`
- `replayQueue()` 在渲染线程消费录制队列

### 4.2 录制队列跨线程通信

非渲染线程调用以下方法时，操作被延迟到渲染线程执行：
- `setShader(Supplier)`
- `setShaderTexture(int,ResourceLocation/int)`
- `setProjectionMatrix`, `setTextureMatrix`, `resetTextureMatrix`
- `applyModelViewMatrix`, `backupProjectionMatrix`, `restoreProjectionMatrix`
- `setShaderColor`, `lineWidth`
- `setInverseViewRotationMatrix`
- `setShaderGameTime`
- `glGenBuffers`, `glGenVertexArrays`

录制队列使用 `ConcurrentLinkedQueue<RenderCall>`，线程安全。`flipFrame()` 时按序 replay。

### 4.3 初始化阶段 (isInInit)

`beginInitialization()` → `isInInit = true` 期间：
- `assertOnRenderThreadOrInit()` 和 `assertOnGameThreadOrInit()` 通过
- 初始化结束后 `finishInitialization()` replay 残留队列
- `isInInitPhase()` 也永远返回 `true`（第 165 行，被禁用），意味着几乎所有线程检查都仅依赖 `isOnRenderThread()`

---

## 5. 渲染录制队列

### 5.1 RenderCall 接口

```java
// com.mojang.blaze3d.pipeline.RenderCall
public interface RenderCall {
    void execute();
}
```

### 5.2 录制与回放流程

```
主线程/初始化:
  recordRenderCall(() -> { shader = shaderSupplier.get(); })
       │
       ▼
  recordingQueue.add(renderCall)
       │
渲染线程 (flipFrame):
  replayQueue()
       │
       ▼
  while (!recordingQueue.isEmpty())
      recordingQueue.poll().execute()
```

`isReplayingQueue` 标志在 replay 期间为 true，用于区分"正在执行录制命令"与"正常渲染线程调用"。

---

## 6. 纹理绑定

### 6.1 shaderTextures 数组

```java
private static final int[] shaderTextures = new int[12];
```

12 个纹理单元的 ID 缓存。`setShaderTexture(unit, id)` 写入，`getShaderTexture(unit)` 读取。

### 6.2 ResourceLocation 重载

`setShaderTexture(int shaderTexture, ResourceLocation textureId)`：通过 `TextureManager.getTexture(textureId)` 获取 `AbstractTexture`，再调用 `abstracttexture.getId()` 得到 GL 纹理 ID。

### 6.3 GL 纹理单元激活

`activeTexture(int texture)` 调用 `GlStateManager._activeTexture(texture)` → `GL13.glActiveTexture(texture)`，实际上纹理切换是 RenderState 层通过 shader 操作隐式处理的。

---

## 7. 关键不变量与约束

1. **渲染线程是 GL 的唯一入口**: 所有 GL 调用最终必须发生在 renderThread 上。
2. **isOnGameThread() 被禁用**: 永远返回 true，gameThread 断言不做实际检查。
3. **状态缓存优先于 GL 调用**: GlStateManager 的所有状态修改方法先检查本地缓存是否已匹配，避免冗余 GL 调用。
4. **录制队列延迟执行**: 跨线程操作通过 `ConcurrentLinkedQueue` 异步化，在 `flipFrame()` 时重放。
5. **矩阵深拷贝**: `setProjectionMatrix`、`setTextureMatrix` 等方法参数矩阵先深拷贝（`new Matrix4f(param)`），防止外部修改污染。
6. **纹理 ID 无生命周期管理**: 纹理 ID 通过 `shaderTextures[int]` 数组直接存储 int 值，无引用计数或资源追踪。
7. **无 GPU 抽象**: RenderSystem 直接调用 GlStateManager → LWJGL GLxx，没有抽象层。更换渲染 API（Vulkan/DirectX）需要重写整个 RenderSystem。
