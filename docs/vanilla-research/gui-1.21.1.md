# 1.21.1 (NeoForge) Minecraft Vanilla GUI/Screen 渲染系统分析报告

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 1.21.1 仍是 `GuiGraphics + PoseStack + MultiBufferSource` 即时渲染，但 HUD 分层、GUI sprite 元数据和 `DeltaTracker` 已经为后续 26.1.2 重构铺路。

## 目录

1. [类清单与职责](#1-类清单与职责)
2. [HUD LayeredDraw 与 NeoForge GuiLayerManager](#2-hud-layereddraw-与-neoforge-guilayermanager)
3. [GuiGraphics API 与 Sprite 缩放](#3-guigraphics-api-与-sprite-缩放)
4. [Font 与 glyph 流程](#4-font-与-glyph-流程)
5. [ItemRenderer 与 GUI 图标](#5-itemrenderer-与-gui-图标)
6. [Screen、Widget 与布局](#6-screenwidget-与布局)
7. [Tooltip、Scissor、Z 与 RenderType](#7-tooltipscissorz-与-rendertype)
8. [调用链、交互点与不变量](#8-调用链交互点与不变量)

---

## 1. 类清单与职责

| 类/接口 | 路径 | 职责 |
|---|---|---|
| `Gui` | `net/minecraft/client/gui/Gui.java` | HUD 编排器；内部创建 `LayeredDraw` 并交给 NeoForge `GuiLayerManager` 渲染。 |
| `LayeredDraw` | `net/minecraft/client/gui/LayeredDraw.java` | 原版 HUD 分层容器，按加入顺序执行 layer。 |
| `GuiGraphics` | `net/minecraft/client/gui/GuiGraphics.java` | 即时 GUI 绘制上下文，包装 `PoseStack`、`BufferSource`、`ScissorStack` 与 `GuiSpriteManager`。 |
| `GuiSpriteManager` | `net/minecraft/client/gui/GuiSpriteManager.java` | GUI atlas sprite 与 `GuiSpriteScaling` 元数据管理。 |
| `GuiSpriteScaling` | `net/minecraft/client/resources/metadata/gui/GuiSpriteScaling.java` | GUI sprite 缩放策略：`Stretch`、`Tile`、`NineSlice`。 |
| `Font` | `net/minecraft/client/gui/Font.java` | 文本绘制入口，仍通过 `FontSet`、`BakedGlyph`、`GlyphRenderTypes`。 |
| `FontSet` / `FontTexture` | `net/minecraft/client/gui/font/` | 字形 provider、图集和 baked glyph 管理。 |
| `Screen` | `net/minecraft/client/gui/screens/Screen.java` | 菜单/屏幕基类，管理 renderables、children、narration、tooltip。 |
| `AbstractWidget` | `net/minecraft/client/gui/components/AbstractWidget.java` | Widget 基类，`renderWidget()` 仍是具体控件扩展点。 |
| `WidgetSprites` | `net/minecraft/client/gui/components/WidgetSprites.java` | Widget 常态/hover/focus sprite 选择。 |
| `Button` / `EditBox` / `ImageButton` | `net/minecraft/client/gui/components/` | 常用控件，使用 `GuiGraphics.blitSprite()` 绘制。 |
| `BossHealthOverlay` | `net/minecraft/client/gui/components/BossHealthOverlay.java` | Boss 血条 layer。 |
| `PlayerTabOverlay` | `net/minecraft/client/gui/components/PlayerTabOverlay.java` | Tab 玩家列表 layer。 |
| `SubtitleOverlay` | `net/minecraft/client/gui/components/SubtitleOverlay.java` | 字幕 layer。 |
| `ClientTooltipComponent` | `net/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent.java` | tooltip 文本/图片行。 |
| `TooltipRenderUtil` | `net/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil.java` | tooltip 背景和边框绘制。 |

与 1.20.1 相比，1.21.1 的核心变化不在底层 GPU 提交，而在 HUD 组织和资源描述：硬编码顺序开始转为 layer，直接像素 UV 开始转为 sprite 元数据。

---

## 2. HUD LayeredDraw 与 NeoForge GuiLayerManager

**文件**: `net/minecraft/client/gui/Gui.java`

### 2.1 核心字段

```java
private final LayeredDraw layers = new LayeredDraw();
private final DebugScreenOverlay debugOverlay;
private final SubtitleOverlay subtitleOverlay;
private final SpectatorGui spectatorGui;
private final PlayerTabOverlay tabList;
private final BossHealthOverlay bossOverlay;
private final net.neoforged.neoforge.client.gui.GuiLayerManager layerManager;
public int leftHeight;
public int rightHeight;
```

`leftHeight` / `rightHeight` 是 NeoForge 为 hotbar 左右侧 overlay 提供的高度协调变量，健康、护甲、饥饿、空气等 layer 会递增它们。

### 2.2 构造期 layer 注册

`Gui` 构造函数附近将原版 HUD 拆成多个 `LayeredDraw` 条目：

```text
CAMERA_OVERLAYS
CROSSHAIR
HOTBAR
JUMP_METER
EXPERIENCE_BAR
PLAYER_HEALTH / ARMOR_LEVEL / FOOD_LEVEL
VEHICLE_HEALTH
AIR_LEVEL
SELECTED_ITEM_NAME
SPECTATOR_TOOLTIP
EXPERIENCE_LEVEL
EFFECTS
BOSS_OVERLAY
DEMO_OVERLAY
DEBUG_OVERLAY
SCOREBOARD_SIDEBAR
OVERLAY_MESSAGE
TITLE
CHAT
TAB_LIST
SUBTITLE_OVERLAY
SAVING_INDICATOR
```

`this.layerManager.add(layereddraw, () -> !minecraft.options.hideGui).add(SLEEP_OVERLAY, this::renderSleepOverlay).add(layereddraw1, () -> !minecraft.options.hideGui);` 表明 NeoForge 在原版 `LayeredDraw` 外再包一层可注册/可条件控制的 manager。

### 2.3 渲染入口

```java
public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
    if (!(this.minecraft.screen instanceof LevelLoadingScreen)) {
        this.leftHeight = 39;
        this.rightHeight = 39;
        this.layerManager.render(guiGraphics, deltaTracker);
    }
}
```

`DeltaTracker` 替代 1.20.1 的裸 `float partialTick`，HUD 动画、attack indicator、portal overlay 等从对象中读取帧内时间。

### 2.4 仍未拆成独立 Renderer 类

1.21.1 虽有 layer 注册，但 `renderHealthLevel()`、`renderArmorLevel()`、`renderFoodLevel()`、`renderHotbar()`、`renderEffects()` 等仍是 `Gui` 内部方法。`BossHealthOverlay`、`SubtitleOverlay`、`PlayerTabOverlay` 仍是少数独立 overlay 类。

---

## 3. GuiGraphics API 与 Sprite 缩放

**文件**: `net/minecraft/client/gui/GuiGraphics.java`

### 3.1 数据结构

```java
private final Minecraft minecraft;
private final PoseStack pose;
private final MultiBufferSource.BufferSource bufferSource;
private final GuiGraphics.ScissorStack scissorStack = new ScissorStack();
private final GuiSpriteManager sprites;
private boolean managed;
```

1.21.1 仍保留 `MAX_GUI_Z = 10000.0F`、`MIN_GUI_Z = -10000.0F` 和 `flush()/drawManaged()`。

### 3.2 `blitSprite()` 与 `GuiSpriteScaling`

1.21.1 新增 `blitSprite(ResourceLocation sprite, ...)`：

```text
GuiGraphics.blitSprite(location, x, y, w, h)
  -> GuiSpriteManager.getSprite(location)
  -> GuiSpriteManager.getSpriteScaling(sprite)
  -> Stretch: blitSprite(TextureAtlasSprite)
  -> Tile: blitTiledSprite(...)
  -> NineSlice: blitNineSlicedSprite(...)
```

`GuiSpriteScaling` 的三种策略：

| 类型 | 字段/含义 | 行为 |
|---|---|---|
| `Stretch` | 无额外字段 | 拉伸整个 sprite 到目标矩形。 |
| `Tile(int width, int height)` | 源 tile 尺寸 | 在目标区域内重复平铺。 |
| `NineSlice(Border border, int width, int height, boolean stretchInner)` | 九宫格边界与原始尺寸 | 四角保持，边和中心拉伸或平铺。 |

这让 widget、HUD 图标从“硬编码 widgets.png UV”转向“atlas sprite + metadata”。

### 3.3 基础 API 清单

| API | 行为 |
|---|---|
| `fill(...)` | 与 1.20.1 一样写 `RenderType.gui()` quad。 |
| `fillGradient(...)` | 渐变矩形。 |
| `fillRenderType(RenderType, ...)` | 1.21.1 新增的显式 RenderType 填充便捷入口。 |
| `blit(...)` | 传统 texture UV blit 仍保留。 |
| `blitSprite(...)` | 新 sprite 元数据入口。 |
| `renderItem(...)` | 进入 `ItemRenderer`，仍是即时绘制。 |
| `renderItemDecorations(...)` | 数量、耐久、冷却等叠加。 |
| `enableScissor` / `disableScissor` | 仍直接调用 `RenderSystem.enableScissor/disableScissor`。 |
| `renderTooltipForNextFrame(...)` | NeoForge/原版演进为下一帧 tooltip 风格 API，但底层仍使用 `GuiGraphics` 即时绘制。 |

---

## 4. Font 与 glyph 流程

1.21.1 字体渲染的主体仍与 1.20.1 同构：

```text
GuiGraphics.drawString(...)
  -> Font.drawInBatch(...)
    -> renderText(...)
      -> StringRenderOutput.accept(...)
        -> FontSet.getGlyphInfo/getGlyph
        -> BakedGlyph.render(...)
```

核心类仍位于：

| 类 | 路径 | 说明 |
|---|---|---|
| `Font` | `net/minecraft/client/gui/Font.java` | 文本布局、阴影、outline、effect。 |
| `FontSet` | `net/minecraft/client/gui/font/FontSet.java` | 字形集和图集。 |
| `FontTexture` | `net/minecraft/client/gui/font/FontTexture.java` | 字形贴图页。 |
| `GlyphRenderTypes` | `net/minecraft/client/gui/font/GlyphRenderTypes.java` | 字体 RenderType 选择。 |
| `BakedGlyph` | `net/minecraft/client/gui/font/glyphs/BakedGlyph.java` | 字形顶点构造。 |

尚未出现 26.1.2 的 `GlyphRenderState` 或 `TextRenderable`。文字仍是 `MultiBufferSource` 即时写入。

---

## 5. ItemRenderer 与 GUI 图标

`Gui.renderSlot()` 在 1.21.1 中签名改用 `DeltaTracker`：

```text
renderSlot(guiGraphics, x, y, deltaTracker, player, stack, seed)
  -> guiGraphics.renderItem(player, stack, x, y, seed)
  -> guiGraphics.renderItemDecorations(font, stack, x, y)
```

物品图标仍使用 `ItemRenderer` 的 baked model 路径。1.21.1 尚未引入 26.1.2 的 `ItemModelResolver`、`ItemStackRenderState`、`GuiItemAtlas`。因此 GUI 物品仍是“在 GUI 绘制期间直接渲染模型”，不是先抽取 item render state。

与方块图标交互仍通过 `ItemRenderer` 内部的 block/item baked model 体系和 `BlockRenderDispatcher`。

---

## 6. Screen、Widget 与布局

### 6.1 Screen

`Screen.render(GuiGraphics, int mouseX, int mouseY, float partialTick)` 模板仍存在，但调用方时间信息在 HUD 侧已转成 `DeltaTracker`。Screen 管理：

- `children()`：输入事件目标。
- `renderables`：可绘制 widget。
- `narratables`：旁白条目。
- `setFocused()` / `ComponentPath`：焦点导航。
- `renderBackground()` / `renderDirtBackground()` / `renderTooltip()`：常用绘制辅助。

### 6.2 布局系统

1.21.1 保留并扩展布局包：

| 类 | 说明 |
|---|---|
| `Layout` / `LayoutElement` | 布局接口与元素接口。 |
| `LayoutSettings` | padding、对齐等布局参数。 |
| `FrameLayout` | 单区域 frame 布局。 |
| `GridLayout` | 网格布局。 |
| `LinearLayout` | 线性排列。 |
| `HeaderAndFooterLayout` | 标题/内容/底部按钮常用结构。 |
| `EqualSpacingLayout` / `CommonLayouts` | 1.21.1 新增/扩展的常用布局工具。 |

这些布局仍只影响 widget 位置，不参与 GPU 渲染调度。

### 6.3 Widget

`AbstractWidget.renderWidget()` 仍是具体控件绘制扩展点。`WidgetSprites` 使按钮等控件使用 sprite 状态表，从 `GuiGraphics.blitSprite()` 受益。

---

## 7. Tooltip、Scissor、Z 与 RenderType

### 7.1 Tooltip

1.21.1 同时保留直接 tooltip 绘制和 “for next frame” API。物品 tooltip 仍通过：

```text
ItemStack.getTooltipLines / Optional<TooltipComponent>
  -> ClientTooltipComponent
  -> TooltipRenderUtil 背景/边框
  -> renderText/renderImage
```

差异是 Screen/Widget 可以把 tooltip 排到下一帧或稍后绘制，从而避免被后续 widget 覆盖。

### 7.2 Scissor

`enableScissor(minX, minY, maxX, maxY)` 与 1.20.1 一样立即设置 RenderSystem scissor。与 26.1.2 不同，scissor 不是 `GuiElementRenderState` 的排序键。

### 7.3 Z 与 RenderType

| 项 | 1.21.1 行为 |
|---|---|
| Z 范围 | `[-10000, 10000]`。 |
| Z 表达 | `PoseStack` z translate 或 blit/fill 的 z/blitOffset 参数。 |
| GUI 矩形 | `RenderType.gui()`。 |
| GUI sprite | 仍通过 `RenderType` 和 `TextureAtlasSprite` 即时绘制。 |
| 文字 | `Font.drawInBatch()` 选择 glyph RenderType。 |

---

## 8. 调用链、交互点与不变量

### 8.1 帧级调用链

```text
GameRenderer / Minecraft frame
  -> 创建 GuiGraphics
  -> Gui.render(guiGraphics, deltaTracker)
    -> GuiLayerManager.render(...)
      -> LayeredDraw entries
  -> 当前 Screen.render(guiGraphics, mouseX, mouseY, partialTick)
  -> GuiGraphics.flush()
```

### 8.2 重要不变量

| 不变量 | 说明 |
|---|---|
| HUD 顺序 | 由 `LayeredDraw`/`GuiLayerManager` 注册顺序控制。 |
| 物品槽尺寸 | GUI item 默认仍按 16x16 绘制。 |
| 心/空气图标 | 仍是 9x9。 |
| 经验条 | 仍是 182 像素。 |
| 绘制模型 | 仍是即时模式，不存在 `GuiRenderState`。 |
| Sprite 元数据 | `Stretch`/`Tile`/`NineSlice` 是 1.21.1 的关键新增抽象。 |

### 8.3 对后续版本的意义

1.21.1 是过渡版本：它没有 GPU 抽象层和 extract-submit，但已经把 GUI 资源从固定 UV 推向 sprite 元数据，把 HUD 顺序从单体方法推向 layer 注册，把时间参数从裸 float 推向 `DeltaTracker`。这些都是 26.1.2 GUI 重构能落地的前置形态。
