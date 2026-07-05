# 1.20.1 (Forge) Minecraft Vanilla GUI/Screen 渲染系统分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。GUI 在 1.20.1 已经使用 `GuiGraphics`，但 HUD 仍由 `Gui.render()` 单体顺序绘制。

## 目录

1. [类清单与职责](#1-类清单与职责)
2. [HUD 主流程 Gui](#2-hud-主流程-gui)
3. [GuiGraphics 绘制上下文](#3-guigraphics-绘制上下文)
4. [Font 字符渲染](#4-font-字符渲染)
5. [物品与方块图标](#5-物品与方块图标)
6. [Screen 与 Widget](#6-screen-与-widget)
7. [纹理、九宫格与 RenderType](#7-纹理九宫格与-rendertype)
8. [Tooltip 与 Scissor](#8-tooltip-与-scissor)
9. [Z-index、深度与不变量](#9-z-index深度与不变量)
10. [调用链与交互点](#10-调用链与交互点)

---

## 1. 类清单与职责

| 类/接口 | 路径 | 职责 |
|---|---|---|
| `Gui` | `net/minecraft/client/gui/Gui.java` | 游戏内 HUD 单体编排器，按固定顺序绘制 vignette、头盔覆盖、快捷栏、血量、饥饿、经验、Boss、聊天、Tab、字幕等。 |
| `GuiGraphics` | `net/minecraft/client/gui/GuiGraphics.java` | GUI 绘制上下文，包装 `PoseStack`、`MultiBufferSource.BufferSource` 和 `ScissorStack`。 |
| `Font` | `net/minecraft/client/gui/Font.java` | 文本布局与字形提交入口，最终通过 `FontSet` 获取 `BakedGlyph` 写入 `VertexConsumer`。 |
| `FontSet` | `net/minecraft/client/gui/font/FontSet.java` | 字体集与字形贴图管理，维护 glyph provider、`FontTexture`、missing/white glyph。 |
| `FontTexture` | `net/minecraft/client/gui/font/FontTexture.java` | 字形图集纹理，承载 stitch 后的 `BakedGlyph`。 |
| `GlyphRenderTypes` | `net/minecraft/client/gui/font/GlyphRenderTypes.java` | 为字体纹理选择 normal/see-through/polygon-offset `RenderType`。 |
| `Screen` | `net/minecraft/client/gui/screens/Screen.java` | 所有菜单/屏幕基类，管理子控件、焦点、叙述、背景与 tooltip。 |
| `AbstractContainerScreen` | `net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java` | 容器屏幕基类，负责 `renderBg`、slot、item tooltip、拖拽物品等。 |
| `AbstractWidget` | `net/minecraft/client/gui/components/AbstractWidget.java` | Widget 基类，定义 `renderWidget()`、鼠标命中、焦点、叙述。 |
| `Button` / `EditBox` | `net/minecraft/client/gui/components/` | 常用控件，分别处理按钮精灵和文本输入渲染。 |
| `BossHealthOverlay` | `net/minecraft/client/gui/components/BossHealthOverlay.java` | Boss 血条覆盖层，由 `Gui` 持有。 |
| `PlayerTabOverlay` | `net/minecraft/client/gui/components/PlayerTabOverlay.java` | 玩家列表覆盖层，由 `Gui` 持有。 |
| `SubtitleOverlay` | `net/minecraft/client/gui/components/SubtitleOverlay.java` | 字幕覆盖层，由 `Gui` 持有。 |
| `ClientTooltipComponent` | `net/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent.java` | tooltip 行渲染单元，支持纯文本和物品自定义 tooltip component。 |
| `TooltipRenderUtil` | `net/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil.java` | tooltip 背景/边框绘制工具。 |
| `ItemRenderer` | `net/minecraft/client/renderer/entity/ItemRenderer.java` | GUI 物品图标渲染入口，被 `GuiGraphics.renderItem()` 调用。 |
| `BlockRenderDispatcher` | `net/minecraft/client/renderer/block/BlockRenderDispatcher.java` | 方块模型渲染调度，被物品渲染器用于方块物品模型。 |

1.20.1 没有 `LayeredDraw`、没有 `GuiSpriteManager`、没有 GUI `RenderState`。所有 HUD 元素仍在 `Gui.render(GuiGraphics, float)` 中硬编码排序。

---

## 2. HUD 主流程 Gui

**文件**: `net/minecraft/client/gui/Gui.java`

### 2.1 核心字段

```java
private final Minecraft minecraft;
private final ChatComponent chat;
private final DebugScreenOverlay debugOverlay;
private final SubtitleOverlay subtitleOverlay;
private final SpectatorGui spectatorGui;
private final PlayerTabOverlay tabList;
private final BossHealthOverlay bossOverlay;
private int tickCount;
private int toolHighlightTimer;
private ItemStack lastToolHighlight = ItemStack.EMPTY;
public float vignetteBrightness = 1.0F;
```

这些字段说明 1.20.1 的 HUD 不是独立 layer 系统，而是 `Gui` 直接持有所有覆盖层对象，并在单个 `render()` 方法中串行调用。

### 2.2 `render(GuiGraphics, float)` 顺序

源码中 `render()` 的关键调用顺序：

```text
Gui.render(graphics, partialTick)
  -> renderVignette()
  -> renderSpyglassOverlay() / renderTextureOverlay(pumpkin) / powder snow
  -> renderPortalOverlay()
  -> spectatorGui.renderHotbar() 或 renderHotbar()
  -> renderCrosshair()
  -> bossOverlay.render()
  -> renderPlayerHealth()
  -> renderVehicleHealth()
  -> renderJumpMeter() 或 renderExperienceBar()
  -> renderSelectedItemName() / spectatorGui.renderTooltip()
  -> renderDemoOverlay()
  -> renderEffects()
  -> debugOverlay.render() 或 scoreboard/chat/tab/subtitles/title/saving indicator
```

关键方法定位：

| 方法 | 行为 |
|---|---|
| `renderCrosshair(GuiGraphics)` | 绘制准星；调试视图下可能调用 `RenderSystem.renderCrosshair(10)`。 |
| `renderHotbar(float, GuiGraphics)` | 绘制快捷栏底图、选中槽、副手槽、攻击指示器，并逐槽调用 `renderSlot()`。 |
| `renderPlayerHealth(GuiGraphics)` | 计算生命、吸收、闪烁、行数，委托 `renderHearts()`。 |
| `renderHearts(...)` | 每颗心按容器、吸收、当前生命、半心、闪烁状态绘制 `GUI_ICONS_LOCATION` 区域。 |
| `renderVehicleHealth(GuiGraphics)` | 玩家骑乘实体时绘制载具心。 |
| `renderExperienceBar(GuiGraphics, int)` | 绘制 182 像素经验条。 |
| `renderEffects(GuiGraphics)` | 绘制药水效果图标；Forge 扩展允许 effect renderer 覆盖图标。 |
| `renderTextureOverlay(...)` | Pumpkin、Powder Snow 等全屏纹理覆盖。 |
| `renderVignette(...)` | 根据亮度与世界边界状态绘制 vignette。 |
| `renderPortalOverlay(...)` | 传送门 overlay，alpha 至少受 `PORTAL_OVERLAY_ALPHA_MIN = 0.2F` 约束。 |
| `renderSlot(...)` | 调用 `GuiGraphics.renderItem()` 与 `renderItemDecorations()`。 |

### 2.3 HUD 常量不变量

| 常量/行为 | 值/说明 |
|---|---|
| 心图标尺寸 | `9 x 9`，`renderHeart()` 直接 blit 9 像素区域。 |
| 快捷栏槽 | 9 个主手槽，中心宽度 182 像素。 |
| 经验条宽度 | 182 像素。 |
| Boss/chat/tab/subtitle | 都由 `Gui` 字段持有，不参与统一 layer 排序。 |
| `tickCount` | HUD 动画、闪烁、消息计时统一依赖 `Gui.tick()` 增量。 |

---

## 3. GuiGraphics 绘制上下文

**文件**: `net/minecraft/client/gui/GuiGraphics.java`

### 3.1 字段结构

```java
public class GuiGraphics implements IForgeGuiGraphics {
    public static final float MAX_GUI_Z = 10000.0F;
    public static final float MIN_GUI_Z = -10000.0F;
    private final Minecraft minecraft;
    private final PoseStack pose;
    private final MultiBufferSource.BufferSource bufferSource;
    private final GuiGraphics.ScissorStack scissorStack = new ScissorStack();
    private boolean managed;
}
```

`GuiGraphics` 是 1.20.1 的关键抽象：它不拥有渲染排序，也不保存跨帧状态，只在当前帧内将调用转成顶点写入或 RenderSystem 状态调用。

### 3.2 批次与 flush

| 方法 | 行为 |
|---|---|
| `flush()` | 调用 `RenderSystem.disableDepthTest()`、`bufferSource.endBatch()`、再启用深度测试。 |
| `drawManaged(Runnable)` | 前后 `flush()`，中间设置 `managed = true`，避免每次绘制自动提交。 |
| `flushIfUnmanaged()` | 非 managed 模式时立即提交。 |

这意味着 1.20.1 的 GUI 是“即时提交 + 可选 managed 批次”。调用顺序就是视觉顺序，除非调用者通过 z 或 flush 改变局部行为。

### 3.3 基础绘制 API

| API | 源码行为 |
|---|---|
| `fill(...)` | 使用 `RenderType.gui()` 或传入 `RenderType`，写 4 个 `POSITION_COLOR` quad 顶点。 |
| `fillGradient(...)` | 写 4 个顶点，不同上下颜色。 |
| `blit(ResourceLocation, ...)` | 绑定纹理并提交 `POSITION_TEX` 或 `POSITION_COLOR_TEX` quad。 |
| `blitNineSliced(...)` | 以固定切片宽度绘制九宫格，必要时调用 repeating 路径。 |
| `blitRepeating(...)` | 对源区域进行平铺绘制。 |
| `renderItem(...)` | 设置 GUI 变换后进入 `ItemRenderer`。 |
| `renderItemDecorations(...)` | 绘制数量、耐久条、冷却等图标叠加文字/矩形。 |
| `renderTooltip(...)` | 立即布局并绘制 tooltip。 |
| `enableScissor(...)` / `disableScissor()` | 维护 `ScissorStack` 并调用 `RenderSystem.enableScissor`。 |

### 3.4 ScissorStack

`enableScissor(minX, minY, maxX, maxY)` 将 GUI 坐标经窗口 scale 转换成 framebuffer 坐标；嵌套裁剪通过 `ScreenRectangle.intersection` 收缩。栈顶为空时禁用 scissor，非空时立即设置 OpenGL scissor。

---

## 4. Font 字符渲染

**文件**: `net/minecraft/client/gui/Font.java`

### 4.1 调用链

```text
GuiGraphics.drawString(...)
  -> Font.drawInBatch(...)
    -> Font.renderText(...)
      -> StringRenderOutput.accept(codePoint, style)
        -> FontSet.getGlyphInfo(style font)
        -> FontSet.getGlyph(codePoint) 或 getRandomGlyph()
        -> Font.renderChar(BakedGlyph, ...)
          -> BakedGlyph.render(..., VertexConsumer, packedLight)
```

### 4.2 关键数据结构

| 类 | 作用 |
|---|---|
| `Font` | 字符串宽度、双向文本、阴影、下划线/删除线效果、批量绘制入口。 |
| `Font.StringRenderOutput` | `FormattedCharSink` 实现，逐 code point 解析 `Style` 并累积 x 位置。 |
| `FontSet` | 按 font `ResourceLocation` 管理 glyph provider 与图集。 |
| `BakedGlyph` | 已烘焙字形，知道 UV、边界、渲染类型。 |
| `BakedGlyph.Effect` | 下划线、删除线、背景等矩形效果。 |
| `GlyphRenderTypes` | 根据显示模式选择字体 RenderType。 |

### 4.3 行为不变量

- 普通 GUI 字体使用 packed light `15728880`，等价 full bright。
- drop shadow 会先用偏移矩阵绘制阴影，再绘制正文。
- bold 通过额外偏移绘制字形实现，italic 影响字形顶点斜切。
- underline/strikethrough 不使用特殊 glyph，而是追加 `BakedGlyph.Effect` 矩形。

---

## 5. 物品与方块图标

### 5.1 GUI 入口

`Gui.renderSlot()` 调用：

```text
renderSlot(graphics, x, y, partialTick, player, stack, seed)
  -> graphics.renderItem(player, stack, x, y, seed)
  -> graphics.renderItemDecorations(font, stack, x, y)
```

`GuiGraphics.renderItem()` 负责设置 GUI 空间下的模型姿态、捕获异常并生成 crash report，然后委托 `ItemRenderer`。在 1.20.1 中，GUI 物品仍走传统 baked model 路径；方块物品通过 `ItemRenderer` 内部使用 `BlockRenderDispatcher` 的 baked block model 能力。

### 5.2 交互点

| 子系统 | 交互 |
|---|---|
| `ItemRenderer` | 物品模型解析、foil glint、GUI transform。 |
| `BlockRenderDispatcher` | 方块物品模型渲染。 |
| `TextureAtlas` | 物品/方块 sprite 来自 atlas。 |
| `RenderType` | 物品根据模型层选择 cutout/translucent/glint 等路径。 |
| `Font` | `renderItemDecorations()` 绘制数量和冷却文本。 |

---

## 6. Screen 与 Widget

### 6.1 Screen 基类

`Screen` 是菜单系统根类，典型调用链：

```text
Minecraft.runTick/render loop
  -> 当前 Screen.render(guiGraphics, mouseX, mouseY, partialTick)
    -> renderBackground()
    -> 子类 render() / super.render()
      -> children/widgets render
    -> renderTooltip()
```

`Screen` 管理 `children`、`renderables`、`narratables`，并提供 `addRenderableWidget()` 等注册方法。焦点/导航通过 `GuiEventListener`、`ContainerEventHandler`、`ComponentPath`、`FocusNavigationEvent` 协作。

### 6.2 容器屏幕

`AbstractContainerScreen` 典型顺序：

```text
renderBackground()
renderBg(graphics, partialTick, mouseX, mouseY)
super.render(...)
renderTooltip(...)
```

其中 `renderBg()` 是箱子、熔炉、工作台等具体菜单背景的扩展点；slot 内物品仍通过 `GuiGraphics.renderItem()`。

### 6.3 Widget

`AbstractWidget.render()` 做可见性判断、hovered 状态更新，再委托 `renderWidget(GuiGraphics, mouseX, mouseY, partialTick)`。`Button`、`EditBox`、slider、selection list 都沿用这一模板。

---

## 7. 纹理、九宫格与 RenderType

### 7.1 纹理绘制

1.20.1 GUI 大量使用固定 `ResourceLocation` 与像素 UV，例如 `Gui.GUI_ICONS_LOCATION`、`WIDGETS_LOCATION`、`PUMPKIN_BLUR_LOCATION`。`GuiGraphics.blit()` 将屏幕矩形与纹理 UV 写成 quad。

### 7.2 九宫格

`GuiGraphics` 已有 `blitNineSliced()` 与 `blitRepeating()`，但没有 1.21.1 的 `GuiSpriteManager` 元数据系统。调用者必须显式提供源纹理尺寸、切片宽度、UV 偏移。

### 7.3 RenderType

GUI 基础矩形使用 `RenderType.gui()`；文字高亮、overlay、text 等使用各自 RenderType。1.20.1 的 RenderType 是状态组合对象，底层仍是 `RenderSystem.setShader()`、blend/depth/cull state shard。

---

## 8. Tooltip 与 Scissor

### 8.1 Tooltip

`GuiGraphics.renderTooltip()` 立即完成：

```text
ItemStack tooltip 数据
  -> ClientTooltipComponent.create(...)
  -> 计算宽高与屏幕内位置
  -> TooltipRenderUtil 绘制背景/边框
  -> 每个 ClientTooltipComponent.renderText / renderImage
```

tooltip 的视觉顺序依赖调用位置；没有“下一帧 tooltip”队列。

### 8.2 Scissor

裁剪直接作用于当前 RenderSystem 状态。嵌套 scissor 是栈式交集；任何需要改变 scissor 的代码都必须成对调用 `enableScissor`/`disableScissor`。

---

## 9. Z-index、深度与不变量

| 项 | 1.20.1 行为 |
|---|---|
| GUI Z 范围 | `MIN_GUI_Z=-10000.0F`，`MAX_GUI_Z=10000.0F`。 |
| Z 表达 | `PoseStack.translate(0, 0, z)` 或 blit/fill 参数中的 `z`。 |
| 提交方式 | `MultiBufferSource.BufferSource.endBatch()`。 |
| 深度状态 | `flush()` 周围显式 disable/enable depth test；不同 RenderType 可覆盖状态。 |
| 颜色 | blit overlay 常用 `RenderSystem.setShaderColor` 或顶点 color。 |
| GUI 坐标 | 使用窗口 GUI scale 后的逻辑像素，左上角为 `(0,0)`。 |

---

## 10. 调用链与交互点

### 10.1 帧级调用链

```text
GameRenderer / Minecraft render frame
  -> 创建 GuiGraphics(minecraft, renderBuffers.bufferSource())
  -> minecraft.gui.render(graphics, partialTick)
  -> 当前 Screen.render(graphics, mouseX, mouseY, partialTick)
  -> graphics.flush()
```

### 10.2 与渲染子系统交互

| 子系统 | 交互点 |
|---|---|
| Shader | `RenderType` state shard 或 blit 路径调用 `GameRenderer` shader。 |
| Buffer | `MultiBufferSource.BufferSource` 按 RenderType 聚合顶点。 |
| Texture | `TextureManager`/atlas sprite 绑定。 |
| Item | `GuiGraphics.renderItem()` 进入 `ItemRenderer`。 |
| Font | `Font.drawInBatch()` 进入 `FontSet`/`BakedGlyph`。 |
| Screen | `Screen`/`Widget` 与 HUD 共用同一个 `GuiGraphics`。 |
| Forge | `IForgeGuiGraphics`、effect icon renderer、helmet overlay 等扩展点插入原版流程。 |

1.20.1 的结论是：GUI 已有上下文对象，但没有渲染状态抽取。架构核心仍是“调用即绘制”，Eyelib 若要兼容该版本，必须保留 `PoseStack + MultiBufferSource + RenderType` 的即时路径。
