# Minecraft Vanilla GUI/Screen 渲染系统跨版本对比 (1.20.1 / 1.21.1 / 26.1.2)

> 对比三版本 GUI/HUD/Screen 渲染系统。重点是 1.21.1 到 26.1.2 的架构革命：即时绘制被 `GuiGraphicsExtractor -> GuiRenderState -> GuiRenderer -> RenderPipeline/RenderPass` 取代。

## ① 类/包位置变化

| 维度 | 1.20.1 Forge | 1.21.1 NeoForge | 26.1.2 NeoForge |
|---|---|---|---|
| HUD 主类 | `net.minecraft.client.gui.Gui` | `net.minecraft.client.gui.Gui` | `net.minecraft.client.gui.Gui`，入口改为 `extractRenderState`。 |
| 绘制上下文 | `GuiGraphics` | `GuiGraphics` | `GuiGraphicsExtractor`。 |
| HUD layer | 无统一 layer，`Gui.render()` 硬编码顺序 | `LayeredDraw` + NeoForge `GuiLayerManager` | NeoForge `GuiLayerManager` + extractor，layer 中写状态。 |
| GUI submit | `GuiGraphics.flush()` / `BufferSource.endBatch()` | 同 1.20.1 | `net.minecraft.client.gui.render.GuiRenderer`。 |
| GUI 状态 | 无 | 无 | `net.minecraft.client.renderer.state.gui.*`。 |
| Sprite 管理 | 直接 `ResourceLocation`/UV | `GuiSpriteManager` + `GuiSpriteScaling` | `SpriteGetter`/atlas + `GuiSpriteScaling`，输出 render state。 |
| 字体 | `Font`、`FontSet`、`BakedGlyph` | 同 | 增加 `TextRenderable`、`GlyphRenderState`。 |
| 物品图标 | `GuiGraphics.renderItem()` -> `ItemRenderer` | 同 | `GuiGraphicsExtractor.item()` -> `ItemModelResolver` -> `GuiItemRenderState` -> `GuiItemAtlas/PIP`。 |
| PIP | 无统一 GUI PIP | 无统一 GUI PIP | `gui.render.pip.*` 与 `renderer.state.gui.pip.*`。 |
| Render API | `RenderType` | `RenderType` | `RenderPipeline`、`RenderPass`、`GpuBuffer`、UBO。 |

26.1.2 实际没有用户问题中假设的 `HelmetLayer`、`HeartLayer` 等独立类。源码中 HUD 元素仍由 `Gui` 的 `extract*` 方法实现；真正新增的独立类是 `contextualbar.*Renderer`、`GuiRenderer`、PIP renderer 与 GUI render state。

---

## ② 核心数据结构演进

| 数据结构 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 矩阵 | `PoseStack` 4x4 | `PoseStack` 4x4 | `Matrix3x2fStack` 2D 仿射。 |
| 批次 | `MultiBufferSource.BufferSource` | 同 | `GuiRenderState` + `ByteBufferBuilder` + `MappableRingBuffer`。 |
| 裁剪 | `GuiGraphics.ScissorStack` 立即应用 | 同 | `ScissorStack` 捕获到每个 render state，submit 阶段应用。 |
| 绘制元素 | 直接顶点 | 直接顶点 | `ColoredRectangleRenderState`、`BlitRenderState`、`TiledBlitRenderState`、`GlyphRenderState`。 |
| 文本 | `StringRenderOutput` 直接写 glyph | 同 | `GuiTextRenderState` 准备后转 `GlyphRenderState`。 |
| 物品 | 直接 `ItemRenderer` | 直接 `ItemRenderer` | `GuiItemRenderState` + `TrackingItemStackRenderState`。 |
| GUI 分层 | 调用顺序 | `LayeredDraw` 顺序 | `strata + Node(up)`，按 bounds 相交维持层级。 |
| Blur | 外部/屏幕特例 | 外部/屏幕特例 | `blurBeforeThisStratum()`，每帧最多一次。 |

---

## ③ API 差异表

| 功能 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| HUD 入口 | `Gui.render(GuiGraphics, float)` | `Gui.render(GuiGraphics, DeltaTracker)` | `Gui.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` |
| 纯色矩形 | `GuiGraphics.fill(RenderType, ...)` | 同 | `GuiGraphicsExtractor.fill(RenderPipeline, ...)` |
| 纹理 blit | `blit(ResourceLocation, ...)` | `blit` + `blitSprite` | `blit(RenderPipeline, Identifier/GpuTextureView, ...)` |
| Sprite 缩放 | 手写九宫格/平铺 | `GuiSpriteScaling` 自动选择 | `GuiSpriteScaling` 生成 render state |
| 文字 | `drawString` -> `Font.drawInBatch` | 同 | `text` -> `GuiTextRenderState` -> `GlyphRenderState` |
| 物品 | `renderItem` | `renderItem` | `item` -> `ItemModelResolver` |
| Tooltip | `renderTooltip` 立即绘制 | `renderTooltipForNextFrame` 过渡 | `setTooltipForNextFrame` 延迟状态收集 |
| Scissor | 立即 `RenderSystem.enableScissor` | 同 | 捕获 `ScreenRectangle`，draw 时应用 |
| Z | `[-10000,10000]` | `[-10000,10000]` | 常规 GUI `[0,10000]`，PIP `[-1000,1000]` |
| 提交 | `flush()` | `flush()` | `GuiRenderer.render(GpuBufferSlice)` |

### 关键调用形态对比

| 场景 | 旧版调用者看到的模型 | 26.1.2 调用者看到的模型 | 真实后端差异 |
|---|---|---|---|
| HUD 绘制 | `Gui.render` 中直接调用 `renderHotbar/renderHearts` | `Gui.extractRenderState` 中调用 `extractHotbar/extractHealthLevel` | 方法命名从 render 改为 extract，表示不再直接提交。 |
| Widget 绘制 | `AbstractWidget.renderWidget(GuiGraphics, ...)` | `AbstractWidget.renderWidget(GuiGraphicsExtractor, ...)` | 控件源码看起来仍是 fill/blit/text，但对象变成状态收集器。 |
| 文本阴影 | `Font.drawInBatch` 立即写两个 glyph pass | `GuiTextRenderState` 保存 shadow 标志 | shadow 的实际 glyph 拆分发生在 `GuiRenderer.prepareText()`。 |
| 物品装饰 | `renderItemDecorations` 立即绘制数字/耐久 | `itemDecorations` 继续收集 text/fill | item 本体与装饰文本进入不同 state 类型。 |
| 九宫格 | `blitNineSliced` 直接拆 quad | `blitSprite` 根据 metadata 生成多个 state | 26.1.2 的拆分结果还要参与 scissor/pipeline/texture 排序。 |
| Blur | Screen/后处理特例 | `blurBeforeThisStratum()` | GUI state 内记录 blur 分界，submit 阶段拆成 before/after draw range。 |

---

## ④ 数值/常量不变量

| 常量/行为 | 跨版本结论 |
|---|---|
| GUI item 默认尺寸 | 三版本均以 16x16 为普通物品图标基准；26.1.2 额外处理 oversized item bounds。 |
| 心/空气图标 | 仍以 9x9 为基本 HUD 图标尺寸。 |
| 经验条宽度 | 旧版 `Gui` 中为 182 像素；26.1.2 迁到 contextual bar 体系但视觉基准延续。 |
| GUI 坐标 | 三版本均使用 GUI scale 后的逻辑像素，左上角为原点。 |
| 文本光照 | GUI 文本保持 full-bright 语义；26.1.2 通过 glyph texture/lightmap setup 表达。 |
| Tooltip 数据 | 三版本都使用 `ClientTooltipComponent` 表示 tooltip 行。 |
| HUD 持有 overlay | `BossHealthOverlay`、`PlayerTabOverlay`、`SubtitleOverlay` 三版本持续存在。 |

---

## ⑤ 内部机制演进

### 5.1 1.20.1：上下文封装，但仍是单体即时绘制

`GuiGraphics` 已经统一了 fill/blit/item/text/tooltip/scissor API，但 `Gui.render()` 仍是一个顺序方法。调用一个绘制 API 通常立即写入 `BufferSource` 或改变 `RenderSystem` 状态。

### 5.2 1.21.1：资源与 HUD 分层过渡

`LayeredDraw` 把 HUD 顺序显式化；`GuiSpriteManager`/`GuiSpriteScaling` 把 GUI 纹理从像素 UV 提升为 atlas sprite + 元数据。底层仍即时绘制，但调用方开始依赖更稳定的语义层。

### 5.3 26.1.2：extract-submit 架构

26.1.2 将 GUI 分成三段：

```text
extract: Gui / Screen / Widget 调用 GuiGraphicsExtractor
state: GuiRenderState 保存 element/text/item/PIP/strata/scissor/bounds
submit: GuiRenderer prepare -> sort -> batch -> RenderPass draw
```

此变化带来四个机制差异：

- 绘制调用不再等于 GPU 提交。
- scissor、pipeline、texture 成为排序键。
- 文本和物品需要 prepare 阶段。
- GUI 可以自然插入 blur、PIP、动态 atlas、renderer pool。

### 5.4 26.1.2 的保序策略

26.1.2 并不是把所有 GUI 元素全局重排。`GuiRenderState` 先用 `strata` 保存显式层，再用 `Node.up` 保存与已有 bounds 相交的局部覆盖关系。只有同一可安全排序范围内的 `GuiElementRenderState` 才按 `scissor -> pipeline -> texture` 合批。这个设计避免了“为了合批破坏 HUD/Widget 覆盖顺序”的问题。

### 5.5 资源模型迁移路径

1.20.1 中 GUI 纹理常见形态是 `ResourceLocation + u/v + textureWidth/textureHeight`。1.21.1 引入 `GuiSpriteManager` 后，调用者可以只给 sprite id，由 metadata 决定 stretch/tile/nine-slice。26.1.2 沿用这个语义，但结果不再是直接绘制，而是生成 `BlitRenderState` 或 `TiledBlitRenderState`。因此资源侧演进是连续的，渲染提交侧演进是断裂的。

### 5.6 Tooltip 机制迁移

三版本 tooltip 都保留 `ClientTooltipComponent`，但绘制时机不同。1.20.1 主要是 `renderTooltip` 立即绘制；1.21.1 出现 “for next frame” 风格 API，用来避免被后续控件覆盖；26.1.2 则把 tooltip 正式纳入 deferred state，tooltip 背景、文本、图片最终都拆成 GUI render state 参与同一 submit 流程。

---

## ⑥ 对 eyelib 的影响清单(按优先级)

| 优先级 | 影响 | 建议 |
|---|---|---|
| P0 | 1.20.1/1.21.1 与 26.1.2 的 GUI 后端模型完全不同 | Eyelib GUI bridge 必须拆成 `ImmediateGuiPort` 与 `ExtractedGuiPort` 或等价适配层。 |
| P0 | `GuiGraphics` 与 `GuiGraphicsExtractor` 类型不兼容 | 公共渲染代码不要暴露 vanilla GUI 上下文类型，应封装 fill/blit/text/item/scissor。 |
| P0 | `RenderType` 到 `RenderPipeline` 不是一一替换 | 材质/GUI pipeline 映射应按语义枚举，不要在业务层硬编码 vanilla 类型。 |
| P1 | 26.1.2 物品 GUI 走 `ItemModelResolver` 和 render state | 自定义物品预览不能只假设 `ItemRenderer.render` 可同步调用。 |
| P1 | 26.1.2 scissor 延迟应用 | 旧版“enable 后立即影响 GL 状态”的调试假设失效。 |
| P1 | 字体在 26.1.2 有 `GlyphRenderState` | 文本特效若依赖即时 `VertexConsumer`，需要 submit 阶段适配。 |
| P2 | 1.21.1 已有 sprite scaling | 跨版本 GUI 纹理最好使用 sprite 语义，1.20.1 再降级为显式九宫格/UV。 |
| P2 | HUD layer 名称来自 NeoForge `VanillaGuiLayers` | 插入 HUD 元素应优先用 layer key，而不是 patch `Gui` 方法顺序。 |

---

## ⑦ 验证命令(grep 清单)

以下命令只用于验证源码存在性：

```powershell
rg "class Gui|extractRenderState|registerVanillaLayers" .local_ref\mc\26.1.2\sources\net\minecraft\client\gui\Gui.java
rg "class GuiGraphics|class GuiGraphicsExtractor|enableScissor|setTooltipForNextFrame|public void item" .local_ref\mc\26.1.2\sources\net\minecraft\client\gui
rg "class GuiRenderer|ELEMENT_SORT_COMPARATOR|prepareText|prepareItemElements|RenderPass" .local_ref\mc\26.1.2\sources\net\minecraft\client\gui\render\GuiRenderer.java
rg "record .*RenderState|class GuiRenderState|interface GuiElementRenderState" .local_ref\mc\26.1.2\sources\net\minecraft\client\renderer\state\gui
rg "GUI_SNIPPET|GUI_TEXTURED_SNIPPET|GUI_TEXT|VIGNETTE|CROSSHAIR" .local_ref\mc\26.1.2\sources\net\minecraft\client\renderer\RenderPipelines.java
rg "LayeredDraw|GuiSpriteManager|GuiSpriteScaling|render\(GuiGraphics" .local_ref\mc\1.21.1\sources\net\minecraft\client\gui
rg "public void render\(GuiGraphics guiGraphics, float|renderHotbar|renderHearts|renderEffects" .local_ref\mc\1.20.1\sources\net\minecraft\client\gui\Gui.java
```

---

## ⑧ 总结表

| 问题 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| GUI 架构关键词 | Immediate context | Layered immediate context | Extract-submit render graph style |
| HUD 是否分层 | 否 | 是，`LayeredDraw` | 是，layer 写入 state/strata |
| Screen 是否改变本质 | 否 | 否 | 是，渲染参数换成 extractor |
| 文字是否延迟 | 否 | 否 | 是 |
| 物品是否延迟 | 否 | 否 | 是 |
| Scissor 是否延迟 | 否 | 否 | 是 |
| 纹理抽象 | ResourceLocation + UV | GUI sprite metadata | GUI sprite metadata + render state |
| GPU 抽象 | RenderType/RenderSystem | RenderType/RenderSystem | RenderPipeline/RenderPass/GpuBuffer |
| 对 mod 兼容难度 | 低 | 中 | 高 |

### 版本定位

| 版本 | 适配判断 | 最容易踩坑的点 |
|---|---|---|
| 1.20.1 | 以 `GuiGraphics` 为中心即可覆盖大多数 GUI 绘制 | HUD 没有 layer，插入顺序经常依赖 Forge overlay 或 patch 点。 |
| 1.21.1 | 适配应优先接入 layer 和 sprite 语义 | 不要误以为 `LayeredDraw` 已改变底层提交，它仍是即时渲染。 |
| 26.1.2 | 必须把“发出绘制命令”和“执行绘制命令”分离 | 直接持有 `VertexConsumer`、`RenderType`、`PoseStack` 的旧抽象会失效。 |

---

## ⑨ 5 条最重要的跨版本发现

1. 26.1.2 的 GUI 重构不是简单重命名：它把绘制 API、帧状态和 GPU submit 拆成三层，旧版 `GuiGraphics.flush()` 语义消失。
2. 1.21.1 是关键过渡版本：`LayeredDraw` 与 `GuiSpriteScaling` 已出现，但底层仍是即时 `RenderType` 渲染。
3. 26.1.2 没有独立 `HeartLayer`/`HelmetLayer` 类；HUD 元素仍主要在 `Gui` 内部方法中，只是方法名前缀从 `render*` 变为 `extract*`。
4. 物品 GUI 是 26.1.2 最大断点之一：`ItemModelResolver`、`ItemStackRenderState`、`GuiItemRenderState` 替代了直接 `ItemRenderer.renderItem` 假设。
5. Scissor/Z/blur 都从调用时状态转为 submit 阶段数据，Eyelib 若要跨 1.20.1、1.21.1、26.1.2，必须以自己的 GUI command/state 抽象隔离 vanilla 后端。
