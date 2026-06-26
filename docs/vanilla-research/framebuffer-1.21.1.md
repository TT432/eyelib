# 1.21.1 (NeoForge) Minecraft RenderTarget / Framebuffer / 后处理链 分析报告

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [RenderTarget.java — 与 1.20.1 的差异](#1-rendertargetjava--与-1201-的差异)
2. [MainTarget.java — 与 1.20.1 的差异](#2-maintargetjava--与-1201-的差异)
3. [EffectInstance.java — 与 1.20.1 的差异](#3-effectinstancejava--与-1201-的差异)
4. [PostPass.java — 新增 filterMode](#4-postpassjava--新增-filtermode)
5. [PostChain.java — 新增 blurEffect 和 filterMode 管理](#5-postchainjava--新增-blureffect-和-filtermode-管理)
6. [GameRenderer — 新增 blurEffect 和重构](#6-gamerenderer--新增-blureffect-和重构)
7. [LevelRenderer — 微小变更](#7-levelrenderer--微小变更)

---

## 1. RenderTarget.java — 与 1.20.1 的差异

**文件**: `com/mojang/blaze3d/pipeline/RenderTarget.java` (294 行,减少 34 行)

### 1.1 主要变更

| 变更项 | 1.20.1 | 1.21.1 |
|---|---|---|
| OnlyIn 注解 | `net.minecraftforge.api.distmarker` | `net.neoforged.api.distmarker` |
| 纹理参数 null | `(IntBuffer)null` 显式转型 | `null` 直接传入 |
| setFilterMode | 单参数 | 双参数(含 force 参数,第 143 行) |
| blitToScreen | GameThread 检查 + recordRenderCall | 直接调用(第 222 行) |
| blitToScreen 渲染 | Tesselator+Matrix4f 手动构建 | BufferBuilder 简化(使用 `DefaultVertexFormat.BLIT_SCREEN`) |
| createBuffers depth null | `(IntBuffer)null` | `null` |
| Stencil 配置 | `ForgeConfig.CLIENT` | `NeoForgeConfig.CLIENT` |

### 1.2 blitToScreen 简化 (第 225–248 行)

1.21.1 的 blitToScreen 显著简化:
- 去掉了 `RenderSystem.assertOnGameThreadOrInit()` 和 `recordRenderCall`。
- 去掉了手动 Matrix4f 构建,改用 `DefaultVertexFormat.BLIT_SCREEN`。
- 全屏四边形的顶点从绝对坐标变为 0~1 归一化坐标:
```java
bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);   // 左下
bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);   // 右下
bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);   // 右上
bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);   // 左上
```
- Tesselator 调用改为新的 builder 模式: `RenderSystem.renderThreadTesselator().begin(...)`

### 1.3 setFilterMode 新增 force 参数 (第 143–152 行)

```java
private void setFilterMode(int filterMode, boolean force) {
    if (force || filterMode != this.filterMode) {
        this.filterMode = filterMode;
        GlStateManager._bindTexture(this.colorTextureId);
        GlStateManager._texParameter(3553, 10241, filterMode);
        GlStateManager._texParameter(3553, 10240, filterMode);
        GlStateManager._bindTexture(0);
    }
}
```

---

## 2. MainTarget.java — 与 1.20.1 的差异

**文件**: `com/mojang/blaze3d/pipeline/MainTarget.java` (143 行)

### 2.1 构造简化

```java
// 1.20.1: 检查渲染线程,非渲染线程 recordRenderCall
public MainTarget(int width, int height) {
    super(true);
    RenderSystem.assertOnRenderThreadOrInit();
    if (!RenderSystem.isOnRenderThread())
        RenderSystem.recordRenderCall(() -> this.createFrameBuffer(width, height));
    else
        this.createFrameBuffer(width, height);
}

// 1.21.1: 直接执行
public MainTarget(int width, int height) {
    super(true);
    this.createFrameBuffer(width, height);
}
```

### 1.21.1 其他:

- `@Override` 注解添加了 `equals`/`hashCode`/`toString`。
- 纹理参数 `(IntBuffer)null` → `null`。
- 功能逻辑不变。

---

## 3. EffectInstance.java — 与 1.20.1 的差异

**文件**: `net/minecraft/client/renderer/EffectInstance.java` (415 行)

### 3.1 关键变更

| 变更项 | 1.20.1 | 1.21.1 |
|---|---|---|
| ResourceManager | `ResourceManager` | `ResourceProvider` |
| ResourceLocation 构造 | `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` |
| apply() 断言 | `RenderSystem.assertOnGameThread()` | 无断言 |
| 纹理单元常量 | `'\u84c0' + i` (字面量) | `33984 + i` (GL_TEXTURE0 常量) |
| activeTexture | `GlStateManager._activeTexture` | `RenderSystem.activeTexture` |

### 3.2 apply 的线程断言

1.20.1 版 `apply()` 开头 `RenderSystem.assertOnGameThread()`,1.21.1 删除该断言,使得 `apply()` 可在渲染线程调用。

---

## 4. PostPass.java — 新增 filterMode

**文件**: `net/minecraft/client/renderer/PostPass.java` (106 行)

### 4.1 新增 filterMode 字段 (第 31、36 行)

```java
private final int filterMode;  // 新增

public PostPass(ResourceProvider resourceProvider, String name,
                RenderTarget inTarget, RenderTarget outTarget,
                boolean useLinearFilter) throws IOException {  // 新增参数
    this.effect = new EffectInstance(resourceProvider, name);
    this.inTarget = inTarget;
    this.outTarget = outTarget;
    this.filterMode = useLinearFilter ? 9729 : 9728;  // GL_LINEAR : GL_NEAREST
}
```

### 4.2 process 变更

- `Tesselator.getInstance().getBuilder()` → `Tesselator.getInstance().begin(...)` (builder 模式)
- `bufferbuilder.end()` → `bufferbuilder.buildOrThrow()`
- 其余逻辑不变

### 4.3 getFilterMode (第 103 行)

```java
public int getFilterMode() {
    return this.filterMode;
}
```

---

## 5. PostChain.java — 新增 blurEffect 和 filterMode 管理

**文件**: `net/minecraft/client/renderer/PostChain.java` (353 行)

### 5.1 关键变更

| 变更项 | 1.20.1 | 1.21.1 |
|---|---|---|
| ResourceManager | `ResourceManager` | `ResourceProvider` |
| addPass 签名 | `(String, RT, RT)` | `(String, RT, RT, boolean useLinearFilter)` |
| process | 简单遍历 | 增加 filterMode 自动切换 |
| 新增方法 | 无 | `setFilterMode(int)`, `setUniform(String, float)` |
| `"minecraft:main"` | String | String (不变) |
| parsePassNode | 无 use_linear_filter | 新增 `use_linear_filter` 字段解析 |

### 5.2 process — filterMode 自动切换 (第 313–333 行)

```java
public void process(float partialTicks) {
    this.time += partialTicks;
    while (this.time > 20.0F) this.time -= 20.0F;
    
    int i = 9728;  // GL_NEAREST
    for (PostPass postpass : this.passes) {
        int j = postpass.getFilterMode();
        if (i != j) {
            this.setFilterMode(j);  // 切换所有 target 的过滤模式
            i = j;
        }
        postpass.process(this.time / 20.0F);
    }
    this.setFilterMode(9728);  // 恢复默认
}
```

`setFilterMode` 对 screenTarget 和所有 customRenderTargets 调用 `setFilterMode(filterMode)`。

### 5.3 setUniform (第 335 行)

```java
public void setUniform(String name, float value) {
    for (PostPass postpass : this.passes) {
        postpass.getEffect().safeGetUniform(name).set(value);
    }
}
```

用于 blurEffect 的 "Radius" uniform。

### 5.4 addPass — useLinearFilter 参数 (第 281 行)

```java
public PostPass addPass(String name, RenderTarget inTarget, RenderTarget outTarget, boolean useLinearFilter)
```

---

## 6. GameRenderer — 新增 blurEffect 和重构

**文件**: `net/minecraft/client/renderer/GameRenderer.java` (1691 行)

### 6.1 新增 blurEffect PostChain (第 127 行)

```java
@Nullable
private PostChain blurEffect;
```

BLUR_LOCATION = `namespace:shaders/post/blur.json`

### 6.2 移除字段

| 移除项 | 说明 |
|---|---|
| EFFECTS 数组 | 不再有硬编码的 F4 效果列表 |
| effectIndex / cycleEffect() | 不再支持 F4 切换效果 |

### 6.3 新增方法

```java
public void processBlurEffect(float partialTick)   // 第 355 行
private void loadBlurEffect(ResourceProvider)       // 第 340 行
```

### 6.4 checkEntityPostEffect (第 297–320 行)

```java
// 1.20.1: checkEntityPostEffect 使用 EFFECTS 数组索引
// 1.21.1: if(entity instanceof Creeper) loadEffect("shaders/post/creeper.json");
//          if(entity instanceof Spider)  loadEffect("shaders/post/spider.json");
//          if(entity instanceof EnderMan) loadEffect("shaders/post/invert.json");
//          else loadEntityShader(entity, this)  // Forge 钩子
```

### 6.5 processBlurEffect 调用时机

```java
// GameRenderer.render() 中:
if (this.blurEffect != null && f >= 1.0F) {
    this.blurEffect.setUniform("Radius", f);
    this.blurEffect.process(partialTick);
}
```

- `f` = `options.getMenuBackgroundBlurriness()`
- 仅在值 ≥ 1.0 时激活(菜单背景模糊效果)

### 6.6 ShaderInstance 简化

1.21.1 移除了大量 `static ShaderInstance` 字段。shader 实例现在通过 `RenderStateShard`/Lazy 按需获取,不再作为 GameRenderer 的公共静态字段。

### 6.7 渲染流程 diff (与 1.20.1 相比)

```
1.20.1: postEffect.process() 在 LevelRenderer.doEntityOutline() 之后
           postEffect.process(partialTicks); → MainTarget.bindWrite(true); → GUI
1.21.1: 相同顺序,但 postEffect.process() 使用 deltaTracker.getGameTimeDeltaTicks()
```

---

## 7. LevelRenderer — 微小变更

**文件**: `net/minecraft/client/renderer/LevelRenderer.java` (3706 行)

### 7.1 变更

| 变更项 | 说明 |
|---|---|
| ResourceLocation | `new ResourceLocation(...)` → `ResourceLocation.withDefaultNamespace(...)` |
| Crash 处理 | `Minecraft.crash()` → `minecraft.emergencySaveAndCrash()` |
| addPass 调用 | 不变(仍为 旧 3 参数,但内部新增 useLinearFilter=false 默认) |

### 7.2 架构不变

- entity_outline.json → entityEffect → entityTarget (blitToScreen)
- transparency.json → transparencyChain → translucent/itemEntity/particles/weather/clouds → 合成回 MainTarget
