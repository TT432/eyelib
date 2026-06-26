# GPU 抽象层 — 1.21.1 (NeoForge)

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [与 1.20.1 的关键差异](#2-与-1201-的关键差异)
3. [RenderSystem 静态门面](#3-rendersystem-静态门面)
4. [GlStateManager 状态缓存](#4-glstatemanager-状态缓存)
5. [线程模型](#5-线程模型)
6. [关键不变量与约束](#6-关键不变量与约束)

---

## 1. 类位置与职责

| 类名 | 包路径 | 文件 | 职责 |
|---|---|---|---|
| `RenderSystem` | `com.mojang.blaze3d.systems` | `RenderSystem.java` (945行) | 静态门面（较 1.20.1 减少 137 行） |
| `GlStateManager` | `com.mojang.blaze3d.platform` | `GlStateManager.java` (1005行) | GL 状态管理（较 1.20.1 增加 43 行） |
| `RenderCall` | `com.mojang.blaze3d.pipeline` | `RenderCall.java` | 不变 |

**与 1.20.1 相同的包结构**: `audio`, `font`, `pipeline`, `platform`, `preprocessor`, `shaders`, `systems`, `vertex`。无 GPU 抽象层，架构与 1.20.1 基本相同。

---

## 2. 与 1.20.1 的关键差异

### 2.1 线程模型简化

| 差异项 | 1.20.1 | 1.21.1 |
|---|---|---|
| gameThread 字段 | 存在（但断言被禁用） | **完全移除** |
| `initGameThread(boolean)` | 存在 | **移除** |
| `isOnGameThread()` | 永远返回 true | **移除** |
| `assertOnGameThread()` | 存在（无实际作用） | **移除** |
| `assertOnGameThreadOrInit()` | 存在 | **移除** |
| `assertInInitPhase()` | 存在 | **移除** |
| `isInInitPhase()` | 永远返回 true | **移除** |
| `beginInitialization()` | 存在 | **移除** |
| `finishInitialization()` | 存在 | **移除** |
| `isInInit` 字段 | 存在 | **移除** |
| `isOnRenderThreadOrInit()` | 存在 | 更名为 `isOnRenderThread()` 的单一路径 |

线程断言方法只剩下**两种**:
- `assertOnRenderThread()` — 核心断言
- `assertOnRenderThreadOrInit()` — 初始化期间也用

### 2.2 矩阵栈变化

| 差异项 | 1.20.1 | 1.21.1 |
|---|---|---|
| modelViewStack 类型 | `PoseStack` | **`Matrix4fStack`** |
| modelViewMatrix | 独立 `Matrix4f` 副本 | 直接使用 `modelViewStack`（`Matrix4fStack` 本身是 Matrix4f 子类） |
| `applyModelViewMatrix()` | 深拷贝 `modelViewStack.last().pose()` → `modelViewMatrix` | **移除**，直接使用 `modelViewStack` |
| `getModelViewStack()` | 返回 `PoseStack` | 返回 `Matrix4fStack` |
| `getModelViewMatrix()` | 返回独立副本 | 返回 `modelViewStack`（`Matrix4fStack` = `Matrix4f` 子类） |

### 2.3 API 变更

| 差异项 | 1.20.1 | 1.21.1 |
|---|---|---|
| Forge/NeoForge | `net.minecraftforge.api.distmarker` | `net.neoforged.api.distmarker` |
| JOML 类型 | `Matrix3f` 仍存在 | `Matrix3f` 相关方法可能移除（inverseViewRotationMatrix 检查中） |
| Tesselator 构造 | `new Tesselator()` (1536 隐式) | `new Tesselator(1536)` 显式传参 |
| `samplerCache` | 无 | **不存在**（26.1.2 才有） |

### 2.4 GlStateManager 差异

| 差异项 | 1.20.1 | 1.21.1 |
|---|---|---|
| `lastBrightnessX/Y` 字段 | 存在（Forge 补丁） | 移除（使用其他方式跟踪） |
| `_texParameter` float 重载 | Forge 自定义亮度跟踪 | 标准 `GL11.glTexParameterf` |
| 文件长度 | 962 行 | 1005 行 |

---

## 3. RenderSystem 静态门面

### 3.1 方法清单变化对照

以下标记 [同1.20.1] 的方法行为与 1.20.1 完全相同。

**线程管理**: 仅保留 `initRenderThread()`, `isOnRenderThread()`, `isOnRenderThreadOrInit()`, `assertOnRenderThread()`, `assertOnRenderThreadOrInit()`, `constructThreadException()`。移除所有 gameThread/init 相关方法。

**帧生命周期**: `flipFrame(long)`, `replayQueue()`, `recordRenderCall(RenderCall)`, `limitDisplayFPS(int)`, `pollEvents()`, `isFrozenAtPollEvents()` [同1.20.1]

**深度/模板**: `disableDepthTest()`, `enableDepthTest()`, `depthFunc(int)`, `depthMask(boolean)`, `enableScissor(int,int,int,int)`, `disableScissor()` [同1.20.1，线程断言简化为 assertOnRenderThreadOrInit 或 assertOnRenderThread]

**混合**: `enableBlend()`, `disableBlend()`, `blendFunc(SourceFactor,DestFactor)`, `blendFunc(int,int)`, `blendFuncSeparate(4 参数)`, `blendFuncSeparate(4 int)`, `blendEquation(int)`, `defaultBlendFunc()` [同1.20.1]

**剔除/多边形**: `enableCull()`, `disableCull()`, `polygonMode(int,int)`, `enablePolygonOffset()`, `disablePolygonOffset()`, `polygonOffset(float,float)` [同1.20.1]

**颜色逻辑**: `enableColorLogicOp()`, `disableColorLogicOp()`, `logicOp(LogicOp)` [同1.20.1]

**纹理**: `activeTexture(int)`, `texParameter(int,int,int)`, `deleteTexture(int)`, `bindTextureForSetup(int)`, `bindTexture(int)` [同1.20.1]

**Viewport/颜色掩码**: `viewport(int,int,int,int)`, `colorMask(boolean,boolean,boolean,boolean)`, `lineWidth(float)` [同1.20.1]

**模板**: `stencilFunc(int,int,int)`, `stencilMask(int)`, `stencilOp(int,int,int)` [同1.20.1]

**清除**: `clearDepth(double)`, `clearColor(float,float,float,float)`, `clearStencil(int)`, `clear(int,boolean)` [同1.20.1]

**Shader 管理**: `setShader(Supplier<ShaderInstance>)`, `getShader()` [同1.20.1]

**Fog/Light/Color**: `setShaderFogStart/getShaderFogStart`, `setShaderFogEnd/getShaderFogEnd`, `setShaderFogColor/getShaderFogColor`, `setShaderFogShape/getShaderFogShape`, `setShaderGlintAlpha/getShaderGlintAlpha`, `setShaderLights/setupShaderLights`, `setShaderColor/getShaderColor`, `setShaderGameTime/getShaderGameTime` [同1.20.1]

**矩阵操作**: 
- `setProjectionMatrix(Matrix4f,VertexSorting)` [同1.20.1]
- `getProjectionMatrix()` [同1.20.1]
- `setTextureMatrix(Matrix4f)`, `resetTextureMatrix()`, `getTextureMatrix()` [同1.20.1]
- `getModelViewMatrix()` — **改为直接返回 modelViewStack**（Matrix4fStack 本身就是 Matrix4f）
- `getModelViewStack()` — 返回 `Matrix4fStack`
- `applyModelViewMatrix()` — **移除**，不再需要显式同步副本
- `backupProjectionMatrix()`, `restoreProjectionMatrix()` [同1.20.1]
- `getVertexSorting()` [同1.20.1]
- `setInverseViewRotationMatrix`, `getInverseViewRotationMatrix` — **可能移除**（PoseStack 消失导致）

**后端初始化**: `initBackendSystem()`, `initRenderer(int,boolean)`, `setErrorCallback(GLFWErrorCallbackI)`, `setupDefaultState(int,int,int,int)`, `maxSupportedTextureSize()` [同1.20.1]

**GL 缓冲**: `glBindBuffer(int,IntSupplier)`, `glBindVertexArray(Supplier<Integer>)`, `glBufferData(int,ByteBuffer,int)`, `glDeleteBuffers(int)`, `glDeleteVertexArrays(int)`, `glGenBuffers(Consumer<Integer>)`, `glGenVertexArrays(Consumer<Integer>)` [同1.20.1]

**Uniform**: 11 个 `glUniform*` 方法 [同1.20.1]

**绘制**: `drawElements(int,int,int)`, `renderThreadTesselator()`, `getSequentialBuffer(VertexFormat.Mode)` [同1.20.1]

**其他**: `pixelStore`, `readPixels`, `getString`, `getBackendDescription`, `getApiDescription`, `renderCrosshair`, `getCapsString`, `setupLevelDiffuseLighting`, `setupGuiFlatDiffuseLighting`, `setupGui3DDiffuseLighting`, `setupOverlayColor`, `teardownOverlayColor`, `runAsFancy` [同1.20.1]

### 3.2 移除的方法清单

| 方法 | 原因 |
|---|---|
| `initGameThread(boolean)` | gameThread 概念移除 |
| `isOnGameThread()` | gameThread 概念移除 |
| `assertOnGameThread()` | gameThread 概念移除 |
| `assertOnGameThreadOrInit()` | 统一到 renderThread 断言 |
| `assertInInitPhase()` | isInInit 移除 |
| `isInInitPhase()` | isInInit 移除 |
| `beginInitialization()` | 录制队列始终活跃 |
| `finishInitialization()` | 录制队列始终活跃 |
| `applyModelViewMatrix()` | Matrix4fStack 不需要显式同步 |
| `setInverseViewRotationMatrix(Matrix3f)` | 未确认（待查 matrix3f 是否保留） |
| `getInverseViewRotationMatrix()` | 未确认 |

---

## 4. GlStateManager 状态缓存

与 1.20.1 相同的缓存结构:

| 状态对象 | 缓存内容 | 变化 |
|---|---|---|
| `BLEND` (BlendState) | mode, srcRgb, dstRgb, srcAlpha, dstAlpha | 无变化 |
| `DEPTH` (DepthState) | mode, func=513, mask=true | 无变化 |
| `CULL` (CullState) | enable, mode=1029 | 无变化 |
| `POLY_OFFSET` (PolygonOffsetState) | fill, line, factor, units | 无变化 |
| `COLOR_LOGIC` (ColorLogicState) | enable, op=5379 | 无变化 |
| `STENCIL` (StencilState) | func, mask, fail/zfail/zpass | 无变化 |
| `SCISSOR` (ScissorState) | mode | 无变化 |
| `COLOR_MASK` (ColorMask) | red/green/blue/alpha | 无变化 |
| `TEXTURES[12]` | binding per unit | 无变化 |
| `activeTexture` | 当前纹理单元 | 无变化 |

**移除了 Forge 扩展**: `lastBrightnessX`, `lastBrightnessY` 字段和 `_texParameter(float)` 中的亮度跟踪逻辑被移除，改用标准方式。

---

## 5. 线程模型

### 5.1 简化为单线程概念

```
┌──────────────┐
│ renderThread │  ← 唯一的"渲染线程"概念
│  (GL 操作)    │
└──────┬───────┘
       │ replayQueue() 消费 recordingQueue
       │
       │ recordRenderCall() 从任意线程录制
       ▼
  (ConcurrentLinkedQueue<RenderCall>)
```

**关键变化**:
- `assertOnRenderThreadOrInit()` 覆盖了原先的 "gameThread 可调用" 和 "init 期间可调用" 场景
- 录制队列机制保持不变，但不再区分 gameThread vs renderThread
- 部分方法（`enableDepthTest`, `enableScissor`, `viewport`, `clearColor`, `clearDepth`, `clear`）的线程断言从 `assertOnGameThreadOrInit` 改为 `assertOnRenderThreadOrInit`

### 5.2 录制队列

与 1.20.1 相同: `ConcurrentLinkedQueue<RenderCall>`, `recordRenderCall()`, `replayQueue()` 在 `flipFrame()` 时消费。

---

## 6. 关键不变量与约束

1. **与 1.20.1 共享相同约束**（线程安全、状态缓存、矩阵深拷贝）。
2. **gameThread 概念消失**: 所有操作对渲染线程的依赖更加明确。
3. **Matrix4fStack 替代 PoseStack**: 消除了 `applyModelViewMatrix()` 的手动同步步骤，`Matrix4fStack` 直接充当 modelView 矩阵。
4. **Tesselator 显式容量**: `new Tesselator(1536)` 使初始缓冲区大小显式化。
5. **录制队列始终活跃**: 不再有 `isInInit` 阶段概念，录制队列在初始化期间也在工作。
