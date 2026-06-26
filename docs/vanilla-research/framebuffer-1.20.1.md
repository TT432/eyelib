# 1.20.1 (Forge) Minecraft RenderTarget / Framebuffer / 后处理链 分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [RenderTarget.java — Framebuffer 抽象](#1-rendertargetjava--framebuffer-抽象)
2. [MainTarget.java — 主 Framebuffer](#2-maintargetjava--主-framebuffer)
3. [TextureTarget.java — 临时 Texture Target](#3-texturetargetjava--临时-texture-target)
4. [EffectInstance.java — 后处理 Shader Program](#4-effectinstancejava--后处理-shader-program)
5. [PostPass.java — 单个后处理 Pass](#5-postpassjava--单个后处理-pass)
6. [PostChain.java — 后处理链](#6-postchainjava--后处理链)
7. [GameRenderer 中的 PostChain 实例](#7-gamerenderer-中的-postchain-实例)
8. [LevelRenderer 中的多 Target 输出](#8-levelrenderer-中的多-target-输出)
9. [完整渲染流程](#9-完整渲染流程)

---

## 1. RenderTarget.java — Framebuffer 抽象

**文件**: `com/mojang/blaze3d/pipeline/RenderTarget.java` (328 行)

### 1.1 类层级

```java
public abstract class RenderTarget {
    // 子类: MainTarget, TextureTarget, OutlineBufferTarget
}
```

### 1.2 字段 (第 20–37 行)

```java
public int width;             // 纹理宽度
public int height;            // 纹理高度
public int viewWidth;         // viewport 宽度
public int viewHeight;        // viewport 高度
public final boolean useDepth; // 是否使用深度缓冲
public int frameBufferId;     // GL FBO ID
protected int colorTextureId; // 颜色纹理 GL ID
protected int depthBufferId;  // 深度纹理 GL ID
private final float[] clearChannels = {1.0F, 1.0F, 1.0F, 0.0F}; // 清屏颜色 RGBA
public int filterMode;        // 纹理过滤模式
```

- `viewWidth`/`viewHeight` 可能与 `width`/`height` 不同(例如 MainTarget 有 fallback 维度时)。
- `filterMode` 默认 9728 (GL_NEAREST)。
- Forge 追加 `stencilEnabled` 字段 (第 307 行)。

### 1.3 createBuffers — FBO 创建核心 (第 98–146 行)

```java
this.frameBufferId = GlStateManager.glGenFramebuffers();        // glGenFramebuffers
this.colorTextureId = TextureUtil.generateTextureId();           // glGenTextures
// 若有 depth:
this.depthBufferId = TextureUtil.generateTextureId();
// depth 纹理参数: 10241/10240 = NEAREST, 34892 = 0(GL_TEXTURE_COMPARE_MODE=NONE)
//                 10242/10243 = CLAMP_TO_EDGE
// depth 格式: 6402(GL_DEPTH_COMPONENT) 或 Forge stencil 时 GL_DEPTH32F_STENCIL8
// color 纹理参数: 10242/10243 = CLAMP_TO_EDGE
// color 格式: 32856(GL_RGBA8) + 6408(GL_RGBA) + 5121(GL_UNSIGNED_BYTE)
GlStateManager._glBindFramebuffer(36160, this.frameBufferId);    // GL_FRAMEBUFFER
GlStateManager._glFramebufferTexture2D(36160, 36064, ...);     // GL_COLOR_ATTACHMENT0
// depth 附件: GL_DEPTH_ATTACHMENT(36096) 或 Forge GL_DEPTH_STENCIL_ATTACHMENT
this.checkStatus();                                              // glCheckFramebufferStatus
this.clear(clearError);
```

**GL 常量映射**:
| Java 字面量 | GL 名称 | 含义 |
|---|---|---|
| 36160 | GL_FRAMEBUFFER | 绑定目标 |
| 36064 | GL_COLOR_ATTACHMENT0 | 颜色附件 |
| 36096 | GL_DEPTH_ATTACHMENT | 深度附件 |
| 36053 | GL_FRAMEBUFFER_COMPLETE | 完整 |
| 3553 | GL_TEXTURE_2D | 纹理目标 |
| 32856 | GL_RGBA8 | 颜色内部格式 |
| 6408 | GL_RGBA | 颜色像素格式 |
| 5121 | GL_UNSIGNED_BYTE | 颜色类型 |
| 6402 | GL_DEPTH_COMPONENT | 深度格式 |
| 5126 | GL_FLOAT | 深度类型 |
| 9728 | GL_NEAREST | 邻近过滤 |
| 9729 | GL_LINEAR | 线性过滤 |
| 33071 | GL_CLAMP_TO_EDGE | 边缘钳制 |
| 34892 | GL_TEXTURE_COMPARE_MODE | 比较模式 |

### 1.4 bindRead / bindWrite / unbindRead / unbindWrite

```java
bindRead()    → GlStateManager._bindTexture(this.colorTextureId);         // 绑定颜色纹理
unbindRead()  → GlStateManager._bindTexture(0);
bindWrite(f)  → GlStateManager._glBindFramebuffer(36160, this.frameBufferId); // 绑定 FBO
                 if (setViewport) GlStateManager._viewport(0,0,viewWidth,viewHeight);
unbindWrite() → GlStateManager._glBindFramebuffer(36160, 0);             // 解绑回默认 FBO
```

- `bindRead` 仅绑定颜色纹理(用于 shader 采样)。
- `bindWrite` 绑定 FBO(用于渲染目标)。
- 渲染线程检查: 非渲染线程通过 `RenderSystem.recordRenderCall` 延迟执行。

### 1.5 blitToScreen — 最终输出到屏幕 (第 243–282 行)

流程:
1. `_colorMask(true, true, true, false)` — 禁止 alpha 写入
2. `_disableDepthTest()` + `_depthMask(false)` — 关闭深度
3. 视口设为屏幕尺寸
4. 可选 `_disableBlend()`
5. 使用 `blitShader` (DiffuseSampler = `this.colorTextureId`)
6. 设置正交投影矩阵
7. 通过 Tesselator 绘制两个三角形(全屏四边形),使用 `POSITION_TEX_COLOR` 格式
8. UV 范围根据 `viewWidth/width` 和 `viewHeight/height` 计算 (支持 Mipmap View 缩放)

### 1.6 copyDepthFrom — 深度复制 (第 90–96 行)

```java
GlStateManager._glBindFramebuffer(36008, otherTarget.frameBufferId); // GL_READ_FRAMEBUFFER
GlStateManager._glBindFramebuffer(36009, this.frameBufferId);        // GL_DRAW_FRAMEBUFFER
GlStateManager._glBlitFrameBuffer(..., 256, 9728);                   // GL_DEPTH_BUFFER_BIT, NEAREST
```

### 1.7 clear (第 284–296 行)

- 绑定写入 → 设置清除颜色 → `_clear(16384 | 256)` (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
- 若 `!useDepth` 则只有 GL_COLOR_BUFFER_BIT

### 1.8 Forge Stencil 扩展

Forge patch 注入 `stencilEnabled` 字段和 `enableStencil()` 方法。启用后 depth 纹理格式使用 `GL_DEPTH32F_STENCIL8`,附件使用 `GL_DEPTH_STENCIL_ATTACHMENT`。通过 `ForgeConfig.CLIENT.useCombinedDepthStencilAttachment` 控制是否合并深度模板附件。

---

## 2. MainTarget.java — 主 Framebuffer

**文件**: `com/mojang/blaze3d/pipeline/MainTarget.java` (148 行)

### 2.1 特性

- 继承 `RenderTarget`,构造时 `useDepth=true`。
- 默认尺寸: 854×480 (DEFAULT_WIDTH, DEFAULT_HEIGHT)。
- **渐进式分配 (Progressive Allocation)**: 先尝试请求尺寸,失败则回退到 854×480。

### 2.2 分配策略 (第 59–81 行)

```java
private MainTarget.Dimension allocateAttachments(int width, int height) {
    // 1. 尝试请求尺寸
    // 2. color 分配失败? → 尝试 DEFAULT
    // 3. depth 分配失败? → 尝试 DEFAULT
    // 4. 两者都成功 → 返回该尺寸
    // 5. 否则 → 抛出 RuntimeException "Unrecoverable GL_OUT_OF_MEMORY"
}
```

`listWithFallback` 方法 (第 123–127 行):
- 若请求尺寸在最大纹理尺寸范围内 → `[请求尺寸, DEFAULT]`
- 否则 → `[DEFAULT]`

### 2.3 与 RenderTarget.createBuffers 的区别

MainTarget **覆盖了** `createFrameBuffer` 逻辑:
- 不调用 `super.createBuffers()`,自己管理纹理生成和 FBO 配置。
- 不调用 `super.resize()` (实际上根本不需要 resize,MainTarget 生命周期内尺寸固定)。
- Attachments 分配失败时静默回退,而非立即抛异常。

---

## 3. TextureTarget.java — 临时 Texture Target

**文件**: `com/mojang/blaze3d/pipeline/TextureTarget.java` (14 行)

```java
public class TextureTarget extends RenderTarget {
    public TextureTarget(int width, int height, boolean useDepth, boolean clearError) {
        super(useDepth);
        RenderSystem.assertOnRenderThreadOrInit();
        this.resize(width, height, clearError);
    }
}
```

- 构造时立即调用 `super.resize()`,从而触发 `createBuffers`。
- 用于 PostChain 的临时 target (translucent, itemEntity, particles, weather, clouds, final 等)。

---

## 4. EffectInstance.java — 后处理 Shader Program

**文件**: `net/minecraft/client/renderer/EffectInstance.java` (414 行)

### 4.1 职责

- 代表一个后处理 shader program (顶点+片段)。
- 从 `shaders/program/<name>.json` 加载配置。
- 管理 samplers (纹理绑定) 和 uniforms。
- 代表单个 PostPass 的 GPU Program。

### 4.2 关键字段

```java
private final Map<String, IntSupplier> samplerMap;   // sampler名 → 纹理ID供应商
private final List<Uniform> uniforms;                // uniform列表
private final int programId;                         // GL program ID
private final BlendMode blend;                       // 混合模式
private final EffectProgram vertexProgram;           // 顶点shader
private final EffectProgram fragmentProgram;         // 片段shader
```

### 4.3 JSON 格式

```json
{
  "vertex": "<namespace>:shaders/program/xxx.vsh",
  "fragment": "<namespace>:shaders/program/xxx.fsh",
  "samplers": [{"name": "DiffuseSampler"}],
  "uniforms": [{"name": "InSize", "type": "float", "count": 2, "values": [1.0, 1.0]}],
  "attributes": ["Position"],
  "blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"}
}
```

### 4.4 apply() / clear() (第 250–277 行 / 第 235–248 行)

```java
apply() {
    this.blend.apply();                    // 设置GL混合
    ProgramManager.glUseProgram(programId); // 激活shader
    // 遍历samplerMap,绑定纹理到对应纹理单元,上传sampler uniform
    // 遍历uniforms上传
}
clear() {
    ProgramManager.glUseProgram(0);        // 解绑shader
    // 解绑所有sampler纹理
}
```

- `lastAppliedEffect` + `lastProgramId` 做短期缓存,避免重复 glUseProgram。

### 4.5 setSampler (第 340–347 行)

```java
public void setSampler(String name, IntSupplier textureId) {
    this.samplerMap.put(name, textureId);
    this.markDirty();
}
```

- `IntSupplier` 模式: 纹理 ID 在 apply 时延迟获取(可能因为 resize 而改变)。

---

## 5. PostPass.java — 单个后处理 Pass

**文件**: `net/minecraft/client/renderer/PostPass.java` (101 行)

### 5.1 构造

```java
public PostPass(ResourceManager, String name, RenderTarget inTarget, RenderTarget outTarget)
```

- 创建 EffectInstance (从 `shaders/program/<name>.json` 加载)
- inTarget/outTarget 指定输入/输出 framebuffer

### 5.2 process — 单个 Pass 执行 (第 56–96 行)

```java
public void process(float partialTicks) {
    this.inTarget.unbindWrite();          // 1. 解绑输入target(确保不写入)
    
    // 2. 设置viewport为输出target尺寸
    RenderSystem.viewport(0, 0, outTarget.width, outTarget.height);
    
    // 3. 设置DiffuseSampler = 输入target的color texture
    this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);
    
    // 4. 绑定辅助纹理(auxAssets)
    for aux : auxAssets {
        effect.setSampler(auxName, auxFramebuffer);
        effect.safeGetUniform("AuxSize" + i).set(width, height);
    }
    
    // 5. 上传标准uniform: ProjMat, InSize, OutSize, Time, ScreenSize
    effect.safeGetUniform("ProjMat").set(shaderOrthoMatrix);
    effect.safeGetUniform("InSize").set(inTarget.width, inTarget.height);
    effect.safeGetUniform("OutSize").set(outTarget.width, outTarget.height);
    effect.safeGetUniform("Time").set(partialTicks);
    effect.safeGetUniform("ScreenSize").set(windowWidth, windowHeight);
    
    // 6. apply shader → clear输出target → bindWrite输出target
    effect.apply();
    outTarget.clear(ON_OSX);
    outTarget.bindWrite(false);
    
    // 7. depthFunc(519)=GL_LEQUAL,绘制全屏四边形 → depthFunc(515)=GL_LEQUAL
    RenderSystem.depthFunc(519);
    // Tesselator绘制 POSITION 格式的QUADS,顶点z=500
    BufferUploader.draw(...);
    RenderSystem.depthFunc(515);
    
    // 8. 清理状态
    effect.clear();
    outTarget.unbindWrite();
    inTarget.unbindRead();
    for aux : auxAssets { if(RenderTarget) unbindRead(); }
}
```

**全屏四边形顶点**:
```
(0, 0, 500), (f, 0, 500), (f, f1, 500), (0, f1, 500)
```
其中 f=outTarget.width, f1=outTarget.height。

### 5.3 auxAssets (第 45–49 行)

辅助输入包括:
- 其他 RenderTarget 的 color/depth 纹理
- 外部纹理 (textures/effect/xxx.png)

---

## 6. PostChain.java — 后处理链

**文件**: `net/minecraft/client/renderer/PostChain.java` (338 行)

### 6.1 构造和 JSON 格式

```java
public PostChain(TextureManager, ResourceManager, RenderTarget screenTarget, ResourceLocation name)
```

JSON 结构:
```json
{
  "targets": ["final", {"name": "bloom", "width": 256, "height": 256}],
  "passes": [
    {
      "name": "blur",
      "intarget": "minecraft:main",
      "outtarget": "bloom",
      "auxtargets": [{"name": "DepthSampler", "id": "minecraft:main:depth"}],
      "uniforms": [{"name": "Radius", "values": [5.0]}]
    }
  ]
}
```

- `"minecraft:main"` → screenTarget (MainTarget)
- string 形式的 target 默认尺寸 = screenWidth × screenHeight
- object 形式的 target 可指定 width/height

### 6.2 addTempTarget (第 261–270 行)

```java
public void addTempTarget(String name, int width, int height) {
    RenderTarget rendertarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
    rendertarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);  // 透明黑色
    if (screenTarget.isStencilEnabled()) rendertarget.enableStencil();
    this.customRenderTargets.put(name, rendertarget);
    if (width == screenWidth && height == screenHeight)
        fullSizedTargets.add(rendertarget);
}
```

- 所有临时 target 都是 TextureTarget,带 depth buffer。
- `fullSizedTargets` 在窗口 resize 时同步更新。

### 6.3 process — 链执行 (第 309–323 行)

```java
public void process(float partialTicks) {
    // 累积时间(处理帧间跳跃)
    time += ...;  while(time > 20.0F) time -= 20.0F;
    
    for (PostPass postpass : passes) {
        postpass.process(this.time / 20.0F);
    }
}
```

- `time` 模 20 秒循环,作为 post 效果的归一化时间。
- Pass 按 JSON 定义顺序执行,每个 pass 的输出是下一个 pass 的输入 (由 JSON 中的 intarget/outtarget 决定)。

### 6.4 resize (第 294–307 行)

```java
public void resize(int width, int height) {
    this.screenWidth = screenTarget.width;
    this.screenHeight = screenTarget.height;
    updateOrthoMatrix();
    for pass : setOrthoMatrix(...)
    for fullSizedTargets : resize(width, height, ON_OSX)
}
```

### 6.5 parsePassNode — auxTargets 解析 (第 122–213 行)

- `id` 后缀 `:depth` → 引用 depth texture
- 否则 → 引用 color texture
- 若 target 不存在 → 尝试作为 `textures/effect/xxx.png` 加载

---

## 7. GameRenderer 中的 PostChain 实例

**文件**: `net/minecraft/client/renderer/GameRenderer.java`

### 7.1 postEffect (第 122 行)

```java
@Nullable PostChain postEffect;
```

仅在按 F4 循环切换或实体触发时加载:
- EFFECTS 数组 (第 123 行): notch, fxaa, art, bumpy, blobs2, pencil, color_convolve, deconverge, flip, invert, ntsc, outline, phosphor, scan_pincushion, sobel, bits, desaturate, green, blur, wobble, blobs, antialias, creeper, spider
- `index == EFFECTS.length` (24) → 无效果
- 实体触发: Creeper → creeper, Spider → spider, Enderman → invert

### 7.2 调用位置 (第 918–923 行)

```java
if (this.postEffect != null && this.effectActive) {
    RenderSystem.disableBlend();
    RenderSystem.disableDepthTest();
    RenderSystem.resetTextureMatrix();
    this.postEffect.process(partialTicks);
}
```

### 7.3 blitShader (第 128 行)

```java
public ShaderInstance blitShader;
```

第 407 行加载: `new ShaderInstance(resourceProvider, "blit_screen", DefaultVertexFormat.BLIT_SCREEN)`
用于 `RenderTarget.blitToScreen` 和 `LevelRenderer.doEntityOutline`。

---

## 8. LevelRenderer 中的多 Target 输出

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 8.1 字段 (第 199–211 行)

```java
private PostChain entityEffect;          // entity_outline.json
private PostChain transparencyChain;     // transparency.json
private RenderTarget entityTarget;       // "final"
private RenderTarget translucentTarget;  // "translucent"
private RenderTarget itemEntityTarget;   // "itemEntity"
private RenderTarget particlesTarget;    // "particles"
private RenderTarget weatherTarget;      // "weather"
private RenderTarget cloudsTarget;       // "clouds"
```

### 8.2 渲染流程中的 Target 使用

```
1. MainTarget (= screenTarget, "minecraft:main") 绑定
2. [Solid/Cutout] → 直接写入 MainTarget
3. [Translucent] → 写入 translucentTarget (有copyDepthFrom MainTarget)
4. [ItemEntity] → 写入 itemEntityTarget (有copyDepthFrom MainTarget)
5. [Particles] → 写入 particlesTarget (有copyDepthFrom MainTarget)
6. [Weather] → 写入 weatherTarget (clear only)
7. [Clouds] → 写入 cloudsTarget
8. transparencyChain.process() → 将 6 个输入合并回 MainTarget
9. [Entity Outline] → entityEffect.process() → entityTarget
10. entityTarget.blitToScreen() → 最终输出到 MainTarget
```

### 8.3 entity_outline.json

- PostChain: entityEffect
- 输出: "final" target (= entityTarget)
- 调用 `entityTarget.blitToScreen()` 而非 `MainTarget.blitToScreen()`,使用 alpha 混合叠加。

### 8.4 transparency.json

- PostChain: transparencyChain
- 将 translucent/itemEntity/particles/weather/clouds 等独立 target 合成回 MainTarget。
- 加载时通过 `postchain.getTempTarget(...)` 获取各 target 引用。

---

## 9. 完整渲染流程

```
GameRenderer.render()
├── renderLevel()
│   ├── MainTarget.bindWrite(true)
│   ├── [Solid/Cutout] → MainTarget
│   ├── translucentTarget: clear + copyDepthFrom(MainTarget)
│   ├── [Translucent blocks] → translucentTarget
│   ├── itemEntityTarget: clear + copyDepthFrom(MainTarget)
│   ├── [Item entities] → itemEntityTarget
│   ├── particlesTarget: clear + copyDepthFrom(MainTarget)
│   ├── [Particles] → particlesTarget
│   ├── weatherTarget: clear
│   ├── [Weather] → weatherTarget
│   ├── [Clouds] → cloudsTarget
│   ├── MainTarget.bindWrite(false)
│   ├── transparencyChain.process()   → 合成回 MainTarget
│   ├── [Entity Outline] 渲染 → entityEffect.process() → entityTarget
│   └── entityTarget.blitToScreen()    → 叠加到 MainTarget
├── doEntityOutline()
│   └── entityTarget.blitToScreen()
├── if (postEffect != null)
│   ├── disableBlend/disableDepthTest/resetTextureMatrix
│   └── postEffect.process(partialTicks)  → 滤镜处理
├── MainTarget.bindWrite(true)
├── [GUI渲染]
└── MainTarget.blitToScreen()            → 最终输出
```
