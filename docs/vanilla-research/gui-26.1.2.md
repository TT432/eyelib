# 26.1.2 (NeoForge) Minecraft Vanilla GUI/Screen 渲染系统分析报告

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 26.1.2 的 GUI 是渲染重构周期的一部分：HUD/Screen 调用不再直接写 GPU 顶点，而是通过 `GuiGraphicsExtractor` 收集 `GuiRenderState`，最后由 `GuiRenderer` 排序、合批、提交到 `RenderPipeline`/`RenderPass`。

## 目录

1. [类清单与职责](#1-类清单与职责)
2. [HUD 编排 Gui 与 layer 清单](#2-hud-编排-gui-与-layer-清单)
3. [GuiGraphicsExtractor](#3-guigraphicsextractor)
4. [GuiRenderState 与 RenderState 类型](#4-guirenderstate-与-renderstate-类型)
5. [GuiRenderer 提交流程](#5-guirenderer-提交流程)
6. [Font 与 GlyphRenderState](#6-font-与-glyphrenderstate)
7. [物品、方块图标与 ItemModelResolver](#7-物品方块图标与-itemmodelresolver)
8. [Screen、Widget、布局与 PIP](#8-screenwidget布局与-pip)
9. [纹理、九宫格、Tooltip、Scissor、Z](#9-纹理九宫格tooltipscissorz)
10. [RenderPipelines 与不变量](#10-renderpipelines-与不变量)

---

## 1. 类清单与职责

### 1.1 GUI 主干

| 类/接口 | 路径 | 职责 |
|---|---|---|
| `Gui` | `net/minecraft/client/gui/Gui.java` | HUD 状态抽取编排器，注册 vanilla/NeoForge GUI layers，调用 `GuiGraphicsExtractor`。 |
| `GuiGraphicsExtractor` | `net/minecraft/client/gui/GuiGraphicsExtractor.java` | GUI “绘制” API 表面；实际创建 render state 并写入 `GuiRenderState`。 |
| `GuiRenderer` | `net/minecraft/client/gui/render/GuiRenderer.java` | GUI submit 阶段执行器：prepare text/item/PIP，排序，合批，创建 `RenderPass` 绘制。 |
| `GuiRenderState` | `net/minecraft/client/renderer/state/gui/GuiRenderState.java` | GUI 帧状态容器，按 stratum/node 保存 element、glyph、text、item、PIP。 |
| `GuiElementRenderState` | `net/minecraft/client/renderer/state/gui/GuiElementRenderState.java` | 可直接构建 GUI quad 顶点的状态接口。 |
| `TextureSetup` | `net/minecraft/client/gui/render/TextureSetup.java` | GUI draw 的纹理/sampler/lightmap 组合与排序 key。 |
| `GuiItemAtlas` | `net/minecraft/client/gui/render/GuiItemAtlas.java` | GUI 物品图标 atlas，配合 item render state 缓存/拷贝。 |
| `DynamicAtlasAllocator` | `net/minecraft/client/gui/render/DynamicAtlasAllocator.java` | 动态 atlas 分配器。 |

### 1.2 HUD 专用 Renderer

26.1.2 没有 `HelmetLayer`、`HeartLayer` 这类独立 HUD layer 类；源码中实际新增的是 contextual bar renderer：

| 类 | 路径 | 职责 |
|---|---|---|
| `ContextualBarRenderer` | `net/minecraft/client/gui/contextualbar/ContextualBarRenderer.java` | 经验/定位/可跳跃载具条的统一接口。 |
| `ExperienceBarRenderer` | `net/minecraft/client/gui/contextualbar/ExperienceBarRenderer.java` | 经验条实现。 |
| `LocatorBarRenderer` | `net/minecraft/client/gui/contextualbar/LocatorBarRenderer.java` | locator bar 实现。 |
| `JumpableVehicleBarRenderer` | `net/minecraft/client/gui/contextualbar/JumpableVehicleBarRenderer.java` | 马等可蓄力跳跃载具条实现。 |
| `BossHealthOverlay` | `net/minecraft/client/gui/components/BossHealthOverlay.java` | Boss 血条 overlay，仍由 `Gui` 调用。 |
| `SubtitleOverlay` | `net/minecraft/client/gui/components/SubtitleOverlay.java` | 字幕 overlay。 |
| `PlayerTabOverlay` | `net/minecraft/client/gui/components/PlayerTabOverlay.java` | 玩家列表 overlay。 |

### 1.3 PIP Renderer

| 类 | 路径 | 职责 |
|---|---|---|
| `PictureInPictureRenderer<T>` | `net/minecraft/client/gui/render/pip/PictureInPictureRenderer.java` | GUI 内 3D/PIP 内容渲染器基类。 |
| `OversizedItemRenderer` | `net/minecraft/client/gui/render/pip/OversizedItemRenderer.java` | GUI 中超过 16x16 bounds 的物品渲染。 |
| `GuiEntityRenderer` | `net/minecraft/client/gui/render/pip/GuiEntityRenderer.java` | GUI 内实体预览。 |
| `GuiSkinRenderer` | `net/minecraft/client/gui/render/pip/GuiSkinRenderer.java` | 皮肤/玩家模型预览。 |
| `GuiSignRenderer` | `net/minecraft/client/gui/render/pip/GuiSignRenderer.java` | 告示牌 GUI 预览。 |
| `GuiBookModelRenderer` | `net/minecraft/client/gui/render/pip/GuiBookModelRenderer.java` | 书模型 GUI 预览。 |
| `GuiBannerResultRenderer` | `net/minecraft/client/gui/render/pip/GuiBannerResultRenderer.java` | 旗帜结果预览。 |
| `GuiProfilerChartRenderer` | `net/minecraft/client/gui/render/pip/GuiProfilerChartRenderer.java` | profiler chart PIP。 |

---

## 2. HUD 编排 Gui 与 layer 清单

**文件**: `net/minecraft/client/gui/Gui.java`

### 2.1 字段结构

```java
private final DebugScreenOverlay debugOverlay;
private final SubtitleOverlay subtitleOverlay;
private final SpectatorGui spectatorGui;
private final PlayerTabOverlay tabList;
private final BossHealthOverlay bossOverlay;
private @Nullable Runnable deferredSubtitles;
private Pair<Gui.ContextualInfo, ContextualBarRenderer> contextualInfoBar;
private final Map<Gui.ContextualInfo, Supplier<ContextualBarRenderer>> contextualInfoBarRenderers;
private final net.neoforged.neoforge.client.gui.GuiLayerManager layerManager;
public int leftHeight;
public int rightHeight;
```

### 2.2 抽取入口

```java
public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    if (!(this.minecraft.screen instanceof LevelLoadingScreen)) {
        leftHeight = 39;
        rightHeight = 39;
        updateContextualBarRenderer();
        layerManager.render(graphics, deltaTracker);
    }
}
```

`render()` 命名已变为 `extractRenderState()`，说明 HUD 阶段只收集状态，不提交 GPU。

### 2.3 `registerVanillaLayers()` 完整 layer 顺序

源码注册顺序如下：

```text
CAMERA_OVERLAYS
CROSSHAIR
AFTER_CAMERA_DECORATIONS -> graphics.nextStratum()
HOTBAR
PLAYER_HEALTH
ARMOR_LEVEL
FOOD_LEVEL
VEHICLE_HEALTH
AIR_LEVEL
CONTEXTUAL_INFO_BAR_BACKGROUND
EXPERIENCE_LEVEL
CONTEXTUAL_INFO_BAR
SELECTED_ITEM_NAME
SPECTATOR_TOOLTIP
EFFECTS
BOSS_OVERLAY
SLEEP_OVERLAY
DEMO_OVERLAY
SCOREBOARD_SIDEBAR
OVERLAY_MESSAGE
TITLE
CHAT
TAB_LIST
SUBTITLE_OVERLAY
```

其中 `AFTER_CAMERA_DECORATIONS` 显式调用 `nextStratum()`，它不是视觉元素，而是分层边界。健康/护甲/饥饿/空气仍是 `Gui` 内部 `extract*` 方法，不是独立 `HeartLayer` 类。

---

## 3. GuiGraphicsExtractor

**文件**: `net/minecraft/client/gui/GuiGraphicsExtractor.java`

### 3.1 字段结构

```java
private final Minecraft minecraft;
private final Matrix3x2fStack pose;
private final GuiGraphicsExtractor.ScissorStack scissorStack;
private final SpriteGetter sprites;
private final TextureAtlas guiSprites;
private final GuiRenderState guiRenderState;
private CursorType pendingCursor;
private final int mouseX;
private final int mouseY;
private @Nullable Runnable deferredTooltip;
private ItemStack tooltipStack = ItemStack.EMPTY;
```

关键变化：`PoseStack` 的 4x4 矩阵替换为 `Matrix3x2fStack`。GUI 常规 2D 操作只需要仿射变换，Z 由 renderer 统一映射。

### 3.2 基础 API 与状态类型

| API | 创建/修改的状态 |
|---|---|
| `fill(RenderPipeline, x0, y0, x1, y1, color)` | `ColoredRectangleRenderState`。 |
| `fillGradient(...)` | `ColoredRectangleRenderState`，上下颜色不同。 |
| `text(Font, ..., x, y, color, dropShadow)` | `GuiTextRenderState`。 |
| `blit(RenderPipeline, Identifier, ...)` | `BlitRenderState`。 |
| `blitSprite(RenderPipeline, Identifier, ...)` | 根据 `GuiSpriteScaling` 创建 blit/tiled/nine-slice 状态。 |
| `item(ItemStack, x, y, seed)` | `GuiItemRenderState`。 |
| `setTooltipForNextFrame(...)` | 保存 `deferredTooltip`，稍后调用 `tooltip(...)` 写入 GUI 状态。 |
| `enableScissor(x0, y0, x1, y1)` | 将当前 `Matrix3x2fStack` 变换后的 `ScreenRectangle` 压入 scissor 栈。 |
| `nextStratum()` | 调用 `GuiRenderState.nextStratum()`。 |
| `blurBeforeThisStratum()` | 标记后续 stratum 在 blur 之后绘制。 |

### 3.3 物品 API 调用链

```text
GuiGraphicsExtractor.item(stack, x, y, seed)
  -> new TrackingItemStackRenderState()
  -> minecraft.getItemModelResolver().updateForTopItem(..., ItemDisplayContext.GUI, ...)
  -> new GuiItemRenderState(pose, itemStackRenderState, x, y, scissor)
  -> guiRenderState.addItem(...)
```

这与 1.20.1/1.21.1 直接调用 `ItemRenderer.render()` 完全不同。

---

## 4. GuiRenderState 与 RenderState 类型

**包**: `net/minecraft/client/renderer/state/gui/`

### 4.1 类清单

| 类型 | 职责 |
|---|---|
| `GuiRenderState` | 帧级 GUI 状态容器，维护 `strata`、当前 node、item model identity、panorama state、clear color override。 |
| `ScreenArea` | 暴露 `bounds()`，供 layering/scissor/相交判断。 |
| `GuiElementRenderState` | 可构建顶点的 GUI 元素接口：`buildVertices`、`pipeline`、`textureSetup`。 |
| `ColoredRectangleRenderState` | 纯色/渐变矩形。 |
| `BlitRenderState` | 单纹理 blit。 |
| `TiledBlitRenderState` | 平铺 blit。 |
| `GlyphRenderState` | 字形贴图元素，包装 `TextRenderable`。 |
| `GuiTextRenderState` | 未准备文本；`GuiRenderer.prepareText()` 会转为 glyph element。 |
| `GuiItemRenderState` | GUI 物品状态，包含 `TrackingItemStackRenderState`、位置、scissor、bounds、oversized bounds。 |
| `PanoramaRenderState` | 标题背景 panorama 旋转状态。 |

### 4.2 PIP RenderState 清单

| 类型 | 对应 Renderer |
|---|---|
| `PictureInPictureRenderState` | `PictureInPictureRenderer<T>`。 |
| `OversizedItemRenderState` | `OversizedItemRenderer`。 |
| `GuiEntityRenderState` | `GuiEntityRenderer`。 |
| `GuiSkinRenderState` | `GuiSkinRenderer`。 |
| `GuiSignRenderState` | `GuiSignRenderer`。 |
| `GuiBookModelRenderState` | `GuiBookModelRenderer`。 |
| `GuiBannerResultRenderState` | `GuiBannerResultRenderer`。 |
| `GuiProfilerChartRenderState` | `GuiProfilerChartRenderer`。 |

### 4.3 Layering 算法

`GuiRenderState` 不是简单列表。它维护 `List<Node> strata`，每个 `Node` 可有 `up` 子节点。新增元素时：

```text
addGuiElement/addText/addItem/addPicturesInPictureState
  -> findAppropriateNode(bounds)
    -> 如果上一元素 bounds 包含当前 bounds，则 up()
    -> 否则从当前 stratum 顶部向下找与当前 bounds 相交的最高 node
    -> 若相交，则 up() 到其上方
  -> 加入当前 node
```

这样可以在保留大部分调用顺序语义的同时，把不相交元素放到同层供排序/合批。

---

## 5. GuiRenderer 提交流程

**文件**: `net/minecraft/client/gui/render/GuiRenderer.java`

### 5.1 字段结构

```java
private static final float MAX_GUI_Z = 10000.0F;
public static final float MIN_GUI_Z = 0.0F;
private static final float GUI_Z_NEAR = 1000.0F;
public static final int GUI_3D_Z_FAR = 1000;
public static final int GUI_3D_Z_NEAR = -1000;
public static final int DEFAULT_ITEM_SIZE = 16;
private final GuiRenderState renderState;
private final List<Draw> draws;
private final List<MeshToDraw> meshesToDraw;
private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(786432);
private final MultiBufferSource.BufferSource bufferSource;
private final SubmitNodeCollector submitNodeCollector;
private final FeatureRenderDispatcher featureRenderDispatcher;
private @Nullable GuiItemAtlas itemAtlas;
```

### 5.2 `render(GpuBufferSlice fogBuffer)`

```text
GuiRenderer.render(fogBuffer)
  -> panoramaRenderState != null ? cubeMap.render(...)
  -> prepare()
  -> draw(fogBuffer)
  -> rotate vertex ring buffers
  -> clear draws/meshes
  -> renderState.reset()
  -> cleanup oversized item/PIP renderer pools
```

### 5.3 `prepare()`

```text
prepare()
  -> bufferSource.endBatch()
  -> preparePictureInPicture()
  -> prepareItemElements()
  -> prepareText()
  -> renderState.sortElements(ELEMENT_SORT_COMPARATOR)
  -> addElementsToMeshes(BEFORE_BLUR)
  -> firstDrawIndexAfterBlur = meshesToDraw.size()
  -> addElementsToMeshes(AFTER_BLUR)
  -> recordDraws()
```

排序键是 `scissorArea -> RenderPipeline.getSortKey() -> TextureSetup.getSortKey()`。这正是 GUI 从“调用顺序提交”转向“状态排序合批”的证据。

### 5.4 `draw()` 与 RenderPass

`draw()` 设置正交投影：

```java
setupOrtho(1000.0F, 11000.0F, width/guiScale, height/guiScale, true);
writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F), ...);
```

若存在 blur 分界，先执行 `GUI before blur`，清深度并调用 `minecraft.gameRenderer.processBlurEffect()`，再执行 `GUI after blur`。实际 draw range 使用 `CommandEncoder`/`RenderPass`、`GpuBuffer`、索引缓冲和动态 uniform。

---

## 6. Font 与 GlyphRenderState

### 6.1 字体相关类

| 类 | 路径 | 变化 |
|---|---|---|
| `Font` | `net/minecraft/client/gui/Font.java` | API 仍负责测量/布局，但 GUI 路径可产出 text render state。 |
| `TextRenderable` | `net/minecraft/client/gui/font/TextRenderable.java` | 26.1.2 新增，用于延迟 glyph 顶点构建。 |
| `PlainTextRenderable` | `net/minecraft/client/gui/font/PlainTextRenderable.java` | 文本可渲染对象实现。 |
| `FontSet` | `net/minecraft/client/gui/font/FontSet.java` | 字形集管理。 |
| `FontTexture` | `net/minecraft/client/gui/font/FontTexture.java` | 字形图集。 |
| `GlyphRenderState` | `net/minecraft/client/renderer/state/gui/GlyphRenderState.java` | 将 `TextRenderable` 转为 GUI element。 |

### 6.2 调用链

```text
GuiGraphicsExtractor.text(font, sequence, x, y, color, shadow)
  -> new GuiTextRenderState(font, sequence, pose, x, y, color, ..., scissor)
  -> GuiRenderState.addText(...)
  -> GuiRenderer.prepareText()
    -> Font/TextRenderable 准备 glyph
    -> GuiRenderState.addGlyphToCurrentLayer(new GlyphRenderState(...))
  -> GlyphRenderState.buildVertices(vertexConsumer)
```

`GlyphRenderState.pipeline()` 返回 `renderable.guiPipeline()`；`textureSetup()` 返回字形 texture view + lightmap。相比旧版本，字形不再在 `Font.drawInBatch()` 调用时立即写入最终 buffer，而是在 submit 阶段合批。

---

## 7. 物品、方块图标与 ItemModelResolver

### 7.1 ItemModelResolver

**文件**: `net/minecraft/client/renderer/item/ItemModelResolver.java`

```java
public void updateForTopItem(ItemStackRenderState output, ItemStack item,
    ItemDisplayContext displayContext, @Nullable Level level,
    @Nullable ItemOwner owner, int seed)
```

流程：

```text
output.clear()
-> item.get(DataComponents.ITEM_MODEL)
-> ModelManager.getItemProperties(modelId).oversizedInGui()
-> ModelManager.getItemModel(modelId).update(output, item, resolver, GUI, level, owner, seed)
```

### 7.2 GUI item state

`GuiItemRenderState` 字段：

```java
private final Matrix3x2f pose;
private final TrackingItemStackRenderState itemStackRenderState;
private final int x;
private final int y;
private final @Nullable ScreenRectangle scissorArea;
private final @Nullable ScreenRectangle oversizedItemBounds;
private final @Nullable ScreenRectangle bounds;
```

若 item model 标记 `oversizedInGui`，它根据模型 AABB 计算真实屏幕 bounds；普通物品仍以 16x16 为默认尺寸。

### 7.3 方块图标

方块物品不再由 GUI 立即进入 `BlockRenderDispatcher`，而是经 item model 更新进入 `ItemStackRenderState.LayerRenderState`。最终由 GUI item 准备阶段写入 atlas 或走 oversized/PIP 路径。方块模型仍依赖客户端模型系统，但 GUI 与模型渲染之间多了一层 `ItemStackRenderState`。

---

## 8. Screen、Widget、布局与 PIP

### 8.1 Screen

26.1.2 的 `Screen` 方法签名已改用 `GuiGraphicsExtractor`。菜单渲染仍是子类覆盖 `render(...)`、调用 `children()`/renderables，但绘制调用只是收集状态。

### 8.2 布局系统

布局包仍包括：

| 类 | 说明 |
|---|---|
| `LayoutSettings` | padding/align 参数。 |
| `FrameLayout` | frame 布局。 |
| `GridLayout` | 网格布局。 |
| `LinearLayout` | 线性布局。 |
| `HeaderAndFooterLayout` | 常用三段式屏幕布局。 |
| `EqualSpacingLayout` / `CommonLayouts` | 常用间距工具。 |

这些布局不直接接触 `RenderPipeline`，只决定后续 extractor API 的坐标。

### 8.3 Widget

`AbstractWidget`、`Button`、`EditBox` 等继续存在，但 `renderWidget()` 接收 extractor。控件通过 `blitSprite(RenderPipelines.GUI_TEXTURED, ...)`、`text(...)`、`fill(...)` 写入 render state。

### 8.4 PIP

GUI 内 3D 内容通过 `PictureInPictureRenderState` 与 `PictureInPictureRenderer` 池执行。`GuiRenderer.preparePictureInPicture()` 先准备 PIP，再把结果作为 GUI 元素参与后续 draw range。

---

## 9. 纹理、九宫格、Tooltip、Scissor、Z

### 9.1 纹理与九宫格

`GuiGraphicsExtractor.blitSprite(RenderPipeline, Identifier, ...)` 使用 atlas `SpriteGetter` 和 `GuiSpriteScaling`。`Stretch` 生成普通 blit；`Tile` 生成 `TiledBlitRenderState`；`NineSlice` 会拆成边角/边/中心多段 blit，必要时启用 scissor 限制目标区域。

### 9.2 Tooltip

tooltip API 统一为 `setTooltipForNextFrame(...)` 与 `tooltip(...)`：

```text
setTooltipForNextFrame(...)
  -> 保存 deferredTooltip
  -> 本帧后段执行 tooltip(...)
  -> ClientTooltipComponent 列表
  -> TooltipRenderUtil / fill / text / image 写入 GuiRenderState
```

26.1.2 没有单独名为 `GuiTooltipRenderer` 的源码类；tooltip 仍在 `GuiGraphicsExtractor` 内完成布局和状态收集。

### 9.3 Scissor

`enableScissor(x0, y0, x1, y1)` 不再立即调用 GL scissor。它把当前 pose 变换后的 `ScreenRectangle` 放入栈；每个 render state 捕获当时的 scissor。`GuiRenderer` 排序和 `RenderPass` 绘制时再按 draw 设置 scissor。

### 9.4 Z

GUI 常规 2D API 没有旧版 `blitOffset` 语义。`GuiRenderer` 使用正交投影和统一 transform 将 GUI 映射到 `MIN_GUI_Z = 0.0F`、`MAX_GUI_Z = 10000.0F` 范围。PIP/3D GUI 内容使用 `GUI_3D_Z_NEAR = -1000`、`GUI_3D_Z_FAR = 1000`。

---

## 10. RenderPipelines 与不变量

**文件**: `net/minecraft/client/renderer/RenderPipelines.java`

### 10.1 GUI pipeline snippets

| Snippet | 配置 |
|---|---|
| `GUI_SNIPPET` | `core/gui` vertex/fragment，`POSITION_COLOR`，`QUADS`，translucent color target。 |
| `GUI_TEXTURED_SNIPPET` | `core/position_tex_color`，`Sampler0`，`POSITION_TEX_COLOR`。 |
| `GUI_TEXT_SNIPPET` | 基于 `TEXT_SNIPPET`，移除 depth stencil。 |

### 10.2 GUI pipelines

| Pipeline | 用途 |
|---|---|
| `GUI` | 纯色矩形。 |
| `GUI_INVERT` | 文本选择反色等。 |
| `GUI_TEXT_HIGHLIGHT` | 文本高亮。 |
| `GUI_TEXTURED` | 普通 GUI 纹理/sprite。 |
| `GUI_TEXTURED_PREMULTIPLIED_ALPHA` | premultiplied alpha GUI 纹理。 |
| `BLOCK_SCREEN_EFFECT` | 方块遮挡屏幕效果。 |
| `FIRE_SCREEN_EFFECT` | 火焰屏幕效果。 |
| `GUI_OPAQUE_TEXTURED_BACKGROUND` | 不透明背景。 |
| `GUI_NAUSEA_OVERLAY` | 反胃 overlay。 |
| `VIGNETTE` | vignette。 |
| `CROSSHAIR` | 准星特殊混合。 |
| `GUI_TEXT` / `GUI_TEXT_INTENSITY` | 字体。 |

### 10.3 不变量

| 不变量 | 说明 |
|---|---|
| 默认 item 尺寸 | `GuiRenderer.DEFAULT_ITEM_SIZE = 16`。 |
| GUI Z | 常规 GUI `[0, 10000]`，3D GUI `[-1000, 1000]`。 |
| byte buffer 初始大小 | `ByteBufferBuilder(786432)`。 |
| 排序键 | scissor -> pipeline sort key -> texture setup sort key。 |
| Blur | 每帧最多一个 `blurBeforeThisStratum()`，重复调用抛异常。 |
| HUD layer | 原版健康/护甲/饥饿仍在 `Gui` 中，不是独立 layer 类。 |

26.1.2 的 GUI 结论：这是从 immediate mode 到 extract-submit 的完整迁移。`GuiGraphicsExtractor` 保留调用方熟悉的 2D API，`GuiRenderState` 负责保序/分层，`GuiRenderer` 才真正触碰 GPU。Eyelib 的跨版本适配必须把“绘制 API 表面”和“提交后端”分开建模。
