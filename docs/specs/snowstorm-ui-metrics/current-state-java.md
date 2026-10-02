# Snowstorm UI 现状指标（Java 侧 eyelib 实现）

> 用途：与原版 Snowstorm v3.2.2 像素指标（`docs/specs/snowstorm-ui-metrics/` 其余区域文档）
> 对比的现状基线。所有数值为 LDLib2 布局像素（gui 缩放后的 MC 像素，非 web CSS px；
> 多数视图按「web 值 ÷ 2 等比」或独立近似值硬编码）。
>
> 路径基准：`src/main/java/io/github/tt432/eyelib/client/gui/snowstorm/`（下表 file:line 相对该目录）。
> 字号为 LDLib2 `fontSize`（MC 字体渲染，非 web px，渲染差异豁免但数值记录）。

## 区域结构 ASCII 示意图

```
SnowstormEditorScreen (100%×100%, ROW)                                [根]
├─ SidebarView (宽=initialSidebarWidth, 100%高, COLUMN)                [左，全高]
│  ├─ logo                h=16
│  ├─ tabBar              h=22 (ROW, 9 tab 各 flex 1, 图标 12)
│  ├─ subjectTitle        h=14, marginV=4
│  └─ groupList (Scroller, flex 1)
│     └─ SsGroupSection × N
│        ├─ header h=10*2+12=32, paddingH=12, paddingV=2
│        │  ├─ fold 按钮 flex 1   └─ help「?」 w=14
│        ├─ content paddingL=8, paddingR=2, paddingV=8
│        │  └─ input 行 × N: h=12 (expanded 44), paddingH=2, gap=2
│        │     [label w=62][expand 钮 w=12][控件 flex 1]
│        └─ divider h=1 (BORDER 色)
└─ 右列 (flex 1, COLUMN)
   ├─ header h=74 (COLUMN)
   │  ├─ MenuBarView  h=40 (ROW; 菜单钮 w=48, 右: help w=24 / Preview w=48 / Code w=48)
   │  └─ ExpressionBarView (flex 1; 内 TextField h=14, margin=3)
   └─ 内容区 (flex 1)：ParticleStageView 或 CodeViewerView
      ├─ ParticleStageView: StageCanvas(flex 1) + StageFooterBar(h=22)
      └─ CodeViewerView: menuRow(h=22, Copy 48×14) + Scroller(flex 1, 行 h=10)
```

---

## 0. SnowstormTheme 色值总表（SnowstormTheme.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| BACKGROUND | `0xFF29323A` (#29323a) | SnowstormTheme.java:8 | --color-background，root 背景 |
| DARK | `0xFF20272D` (#20272d) | SnowstormTheme.java:10 | --color-dark |
| BORDER | `0xFF1A1C1F` (#1a1c1f) | SnowstormTheme.java:12 | --color-border |
| INTERFACE | `0xFF29323A` (#29323a) | SnowstormTheme.java:14 | --color-interface（与 BACKGROUND 同值） |
| BAR | `0xFF34404A` (#34404a) | SnowstormTheme.java:16 | --color-bar |
| TITLE | `0xFF4B5B69` (#4b5b69) | SnowstormTheme.java:18 | --color-title |
| SELECTION | `0x4D6E8EBF` (rgba(110,142,191,0.3)) | SnowstormTheme.java:20 | --color-selection，半透明 hover |
| HIGHLIGHT | `0xFFF7F9FF` (#f7f9ff) | SnowstormTheme.java:22 | --color-highlight |
| TEXT | `0xFFBCC3CA` (#bcc3ca) | SnowstormTheme.java:24 | --color-text |
| TEXT_GRAYED | `0xFF939AA3` (#939aa3) | SnowstormTheme.java:26 | --color-text_grayed |
| ACCENT | `0xFF20DDFF` (#20ddff) | SnowstormTheme.java:28 | --color-accent |

## 0b. kit/SsMetrics 共享度量（kit/SsMetrics.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| TAB_BAR_HEIGHT | 45 | kit/SsMetrics.java:13 | web px 值；SidebarView 实际用本地常量 22（见 §4） |
| TAB_PADDING_V / TAB_PADDING_H | 10 / 4 | kit/SsMetrics.java:15-16 | .sidebar_tab padding；SidebarView 未引用 |
| SUBJECT_TITLE_PADDING / _LEFT | 4 / 12 | kit/SsMetrics.java:18-19 | h3 padding；SidebarView 未引用 |
| GROUP_HEADER_PADDING / _LEFT | 10 / 12 | kit/SsMetrics.java:21-22 | h4 padding；SsGroupSection 引用 |
| HELP_BUTTON_WIDTH / HEIGHT | 30 / 32 | kit/SsMetrics.java:24-25 | .help_button；SsGroupSection 实际用 w=14 |
| GROUP_BODY_PADDING | 8 | kit/SsMetrics.java:27 | .input_group>ul padding（右 2） |
| INPUT_HEIGHT | 24 | kit/SsMetrics.java:29 | common.css 30px 记注；实际行高走 InputViewFactory.ROW_HEIGHT=12 |
| BUTTON_PADDING_V / _H | 8 / 12 | kit/SsMetrics.java:31-32 | button padding；SsButton 未引用 |
| FOLDED_INDICATOR_HEIGHT | 32 | kit/SsMetrics.java:34 | SsGroupSection 引用 |
| SLIDER_TRACK_HEIGHT / THUMB_SIZE | 4 / 20 | kit/SsMetrics.java:36-37 | SsSlider 引用 |
| ICON_SIZE | 12 | kit/SsMetrics.java:39 | lucide 24px 网格 → MC 12px 显示 |

> 注：SsMetrics 多个常量（TAB_BAR_HEIGHT、TAB_PADDING_*、SUBJECT_TITLE_*、HELP_BUTTON_*、
> INPUT_HEIGHT、BUTTON_PADDING_*）定义后未被对应视图实际使用，视图各自硬编码——与原版
> 对齐时需先消除这种双事实源。

---

## 1. 骨架（SnowstormEditorScreen.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| root 布局 | 100%×100%，ROW，BACKGROUND 底 | SnowstormEditorScreen.java:72-76 | App.vue grid 改 flex |
| HEADER_HEIGHT | 74 | SnowstormEditorScreen.java:43 | App.vue grid-template-rows:74px as-is |
| MENUBAR_HEIGHT | 40 | SnowstormEditorScreen.java:45 | 剩余 34 给 ExpressionBar |
| sidebar 宽度 | clamp(body/2, 160, clamp(180+0.2·body,160,660)) | SnowstormEditorScreen.java:82-84 | getInitialSidebarWidth 非 portrait as-is |
| sidebar 布局 | width=sidebarWidth, heightPercent(100) | SnowstormEditorScreen.java:153 | 左置全高（实证修正） |
| header 容器 | widthPercent(100).height(74).COLUMN | SnowstormEditorScreen.java:111-114 | |
| ExpressionBar | widthPercent(100).flex(1) | SnowstormEditorScreen.java:135 | |
| 舞台/CodeViewer | widthPercent/heightPercent 100 | SnowstormEditorScreen.java:146,148 | |
| 确认对话框文字 | fontSize 10, TEXT 色 | SnowstormEditorScreen.java:173 | LDLib2 Dialog 默认尺寸 |
| 拖入悬停遮罩 | `0xA0000000` 全屏；文字 y=center-4 | SnowstormEditorScreen.java:200-202 | 颜色硬编码内联 |
| SnowstormEditorGate | 无 UI 数值（仅反射入口） | SnowstormEditorGate.java:13-19 | 覆盖确认 |

## 2. 菜单栏（menu/MenuBarView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 菜单栏容器 | 100%×100% ROW，INTERFACE 底 | menu/MenuBarView.java:100-101 | |
| 顶层菜单钮（File/Examples/View/Help） | width 48，heightPercent 100，fontSize 10，TEXT 色 | menu/MenuBarView.java:243,249 | base EMPTY，hover/pressed=BAR |
| mode 钮（Preview/Code） | width 48（help「?」w=24），fontSize 10 | menu/MenuBarView.java:258,267 | selected：DARK 底+TEXT_GRAYED 字；hover=INTERFACE |
| 右侧 spacer | flex 1，heightPercent 100 | menu/MenuBarView.java:155-157 | float:right 等价 |
| 下拉弹层 | ABSOLUTE，left/top=锚点左/底，width 150 | menu/MenuBarView.java:291-294 | JS CSS hover 下拉 → 点击开合（偏离已记录） |

## 3. 表达式栏（bar/ExpressionBarView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 栏容器 | widthPercent 100，DARK 底 | bar/ExpressionBarView.java:70-71 | 高度由 Screen flex(1) 给（74-40=34） |
| 内嵌 TextField | widthPercent 100，height 14，marginAll 3 | bar/ExpressionBarView.java:74 | 未设 fontSize（LDLib2 默认 9） |

## 4. 侧栏骨架（SidebarView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 侧栏容器 | COLUMN，INTERFACE 底 | SidebarView.java:58-59 | |
| tab 栏高度 TAB_BAR_HEIGHT | 22 | SidebarView.java:46 | 注释：web 45px → MC 22px 等比；与 SsMetrics.TAB_BAR_HEIGHT=45 不一致（双事实源） |
| TAB_ICON_SIZE | 12 | SidebarView.java:47 | SsIcon.of(name, 12)，调用 :124-125 |
| GROUP_HEADER_HEIGHT | 14 | SidebarView.java:48 | 定义后未被使用（死常量） |
| tabBar 布局 | widthPercent 100，height 22，ROW，BAR 底 | SidebarView.java:61-65 | |
| tab 按钮 | flex 1，heightPercent 100，居中 | SidebarView.java:126-127 | selected=TITLE 底，hover/pressed=SELECTION |
| subjectTitle | height 14，marginVertical 4，fontSize 11，TEXT_GRAYED，TITLE 底，居中 | SidebarView.java:67-73 | h3 大写 |
| logo | widthPercent 100，height 16 | SidebarView.java:78-83 | SpriteTexture eyelib:snowstorm/logo.png |
| groupList | widthPercent 100，flex 1 | SidebarView.java:75-76 | ScrollerView |
| effect 尾部 Quick Setup 钮 | width 90，height 14，marginTop 8，alignSelf CENTER | SidebarView.java:150-154 | SsButton.of（fontSize 10） |

## 5. Group 区块（kit/SsGroupSection.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| header 高度 | GROUP_HEADER_PADDING*2+12 = 32 | kit/SsGroupSection.java:32 | h4 as-is |
| header padding | horizontal 12，vertical 2 | kit/SsGroupSection.java:34-35 | |
| fold 按钮 | flex 1，heightPercent 100（ghost，fontSize 9，TEXT_GRAYED） | kit/SsGroupSection.java:36-39 | |
| help「?」按钮 | width 14，heightPercent 100 | kit/SsGroupSection.java:43 | 与 SsMetrics.HELP_BUTTON 30×32 不一致 |
| content padding | left 8，right 2，vertical 8 | kit/SsGroupSection.java:51-53 | |
| 折叠指示条 | widthPercent 100，height 32（FOLDED_INDICATOR_HEIGHT） | kit/SsGroupSection.java:61 | |
| 组间分隔线 | widthPercent 100，height 1，BORDER 色 | kit/SsGroupSection.java:66-67 | |

## 6. 输入行与各 input 控件（inputs/）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| ROW_HEIGHT | 12 | inputs/InputViewFactory.java:35 | |
| EXPANDED_HEIGHT | 44 | inputs/InputViewFactory.java:37 | .expanded 近似 |
| LABEL_WIDTH | 62 | inputs/InputViewFactory.java:38 | |
| 行容器 | widthPercent 100，height 12/44，ROW，paddingH 2，gap 2 | inputs/InputViewFactory.java:55-59 | |
| label | width 62，heightPercent 100，fontSize 9，TEXT | inputs/InputViewFactory.java:65-66 | |
| expand 钮（chevron） | width 12，heightPercent 100，图标 10 | inputs/InputViewFactory.java:71-76 | |
| 控件槽 | flex 1，heightPercent 100 | inputs/InputViewFactory.java:80 | |
| number/molang/text 控件容器 | ROW，gap 2 | inputs/NumberInputView.java:29-30；inputs/MolangTextInputView.java:32-33 | 多轴按 axis_count 分框，各 flex 1 |
| number/molang TextField | fontSize 9，flex 1，heightPercent 100 | inputs/NumberInputView.java:53,65；inputs/MolangTextInputView.java:58,83 | molang 带 placeholder |
| checkbox | 图标 10（check-square/square），行 ROW，alignItems CENTER | inputs/CheckboxInputView.java:26,39 | |
| select Selector | flex 1，heightPercent 100；候选文字 fontSize 9 左对齐 | inputs/SelectInputView.java:44-46,63 | 容器 gap 2（:36） |
| select_custom 附加框 | fontSize 9，flex 1，heightPercent 100 | inputs/SelectInputView.java:68,78 | |
| color | LDLib2 ColorConfigurator，widthPercent 100 | inputs/ColorInputView.java:24,36 | 默认色 0xFFFFFFFF（:30）；偏离 Chrome 取色器 |
| list 容器 | COLUMN，gap 2 | inputs/ListInputView.java:35-36 | |
| list 添加钮「＋」 | widthPercent 100，height 10，fontSize 9，TEXT_GRAYED | inputs/ListInputView.java:50-57 | |
| list 行 | height ROW_HEIGHT(12)，ROW，gap 2 | inputs/ListInputView.java:65-68 | |
| list 删除钮「✕」 | width 10，heightPercent 100，fontSize 8，TEXT_GRAYED | inputs/ListInputView.java:75-83 | |
| InputUiEvents | 无布局数值（事件对象工厂） | inputs/InputUiEvents.java | 覆盖确认 |

## 7. kit 通用控件（kit/）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| SsButton.of | fontSize 10，TEXT 字；base=BAR，hover/pressed=ACCENT | kit/SsButton.java:22-25 | 注释记 padding 8px 12px / radius 1px，未实现 |
| SsButton.ghost | fontSize 9，TEXT_GRAYED 字；base EMPTY，hover/pressed=SELECTION | kit/SsButton.java:35-38 | |
| SsIconButton | 图标居中，ghost/bar 两形态（bar 底 hover SELECTION） | kit/SsIconButton.java:21-31,34-46 | 尺寸由调用方给 |
| SsIcon | width=height=size；资产 48×48 png | kit/SsIcon.java:22-25 | |
| SsTextField | fontSize 9，DARK 底 | kit/SsTextField.java:21,24-25 | 1px border 未实现（LDLib2 无） |
| SsListAddRow | height 16，marginAll 2，居中「＋」 | kit/SsListAddRow.java:21-28 | dashed 边框未画（偏离记录） |
| SsSlider 容器 | widthPercent 100，height 20 | kit/SsSlider.java:29 | |
| SsSlider 轨道 | widthPercent 100，height 4（SLIDER_TRACK_HEIGHT），BAR 色 | kit/SsSlider.java:33-35 | |
| SsSlider thumb | 20×20（SLIDER_THUMB_SIZE），ACCENT 外框 + inset 2 的 INTERFACE 内心 | kit/SsSlider.java:88-95 | 圆形 → 同心方块近似 |
| SsJsonColors | PUNCTUATION `0xFF5BA8C5`、STRING `0xFF94E400`、NUMBER `0xFFB99CFF`、TEXT `0xFFBCC3CA` | kit/SsJsonColors.java:19-22 | Prism token 色 |

## 8. 曲线编辑器（curve/）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| COLOR_BAR | `0xFF34404A` | curve/CurveEditorView.java:55 | 本地复制 SnowstormTheme.BAR |
| COLOR_CURVE | `0xFF657C86` | curve/CurveEditorView.java:56 | --color-curve（Theme 中缺失，本地常量） |
| COLOR_SELECTION | `0x4C6E8EBF` | curve/CurveEditorView.java:57 | 与 Theme.SELECTION(0x4D…) 差 1 alpha |
| COLOR_TEXT / GRAYED / ACCENT | `0xFFBCC3CA` / `0xFF939AA3` / `0xFF20DDFF` | curve/CurveEditorView.java:58-60 | 本地复制 |
| COLOR_HOVER_BG | `0x19AED9FF` (rgba(174,217,255,.1)) | curve/CurveEditorView.java:61 | .curve_node:hover |
| DEFAULT_HEIGHT | 140 | curve/CurveEditorView.java:73 | JS data height as-is |
| HANDLE_OFFSET | 24 | curve/CurveEditorView.java:75 | JS handle_offset |
| ADD_CAP | 20 | curve/CurveEditorView.java:77 | 首尾 add 帽宽（CSS max-width 20px） |
| display 高度 | height+10（初值 150） | curve/CurveEditorView.java:120,184 | JS CSS height+padding |
| footer | widthPercent 100，height 30，ROW | curve/CurveEditorView.java:132 | fill_line + Remove Curve |
| 选项栏 | height 14 | curve/CurveEditorView.java:314 | |
| 选项栏数值框 | fontSize 9，flex 1，heightPercent 100 | curve/CurveEditorView.java:222,225,326,329 | |
| Connect 钮「=/≠」 | width 24，heightPercent 100 | curve/CurveEditorView.java:271,303 | |
| 删除节点钮「x」 | width 24，图标 9 | curve/CurveEditorView.java:310-311 | |
| 节点点半径 | 选中 5 / 普通 4（直径 10/8） | curve/CurveEditorView.java:782-783,811 | 方块近似圆 |
| slope 手柄 | 8×8 方块，HANDLE_OFFSET 距离 | curve/CurveEditorView.java:788-797 | |
| 命中盒 | 手柄 ±8；chain 节点 ±10 | curve/CurveEditorView.java:414,427,433 | |
| chain 节点 x | 8+(W-16)·time | curve/CurveEditorView.java:591-593 | 路径对齐（修正 JS 2px 错位） |
| 参考虚线 dasharray | 水平 6 / 垂直 8；线宽 2 | curve/CurveEditorView.java:655-657,881-891 | |
| 线段绘制 | 2×2 采样方块，步长 1px；bezier 24 段 | curve/CurveEditorView.java:845-868 | |
| min/max 文本 | x+2 / y+h-9，COLOR_TEXT | curve/CurveEditorView.java:662-663 | |
| 节点值标签偏移 | +12/-8 | curve/CurveEditorView.java:804,821 | |
| CurveAddButton | widthPercent 100，height 24 | curve/CurveAddButton.java:28 | |
| CurveAddButton 边框 | `0xFF34404A` 1px 虚线（dash 4/空 4，步进 8） | curve/CurveAddButton.java:24,66-75 | |
| CurveAddButton hover 底 | `0xFF232B32`（--color-dark 近似，非精确） | curve/CurveAddButton.java:25 | |
| CurveAddButton「+」 | `0xCCBCC3CA`（text 色 0.8 alpha），字宽近似 7，垂直居中用 h-9 | curve/CurveAddButton.java:60-63 | |

## 9. 渐变编辑器（gradient/GradientEditorView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| BAR_HEIGHT | 20 | gradient/GradientEditorView.java:59 | |
| CONTAINER_HEIGHT | 26 | gradient/GradientEditorView.java:60 | 20+6 手柄下探 |
| HANDLE_WIDTH/HEIGHT/TOP | 10 / 16 / 10 | gradient/GradientEditorView.java:61-63 | margin-left:-5px 等价于居中减 5 |
| DRAG_UNLOCK_PX | 4 | gradient/GradientEditorView.java:65 | JS distance>4 as-is |
| 棋盘格双色 | CHECKER_A=INTERFACE，CHECKER_B=BAR | gradient/GradientEditorView.java:67-68 | |
| 渐变条边框 | `0xFF000000` 1px 四边 | gradient/GradientEditorView.java:69,382-387 | |
| 手柄边框 | `0xFF212529`；选中 `0xFFFFFFFF` | gradient/GradientEditorView.java:70-71 | 内填充 inset 1（:143） |
| 容器外边距 | marginHorizontal 5，marginBottom 10 | gradient/GradientEditorView.java:103-104 | .gradient_container margin 0 5px 10px 5px |
| 工具行 | height 14，marginHorizontal 5 | gradient/GradientEditorView.java:109-111 | |
| 工具钮（plus/x） | width 14，heightPercent 100，图标 10 | gradient/GradientEditorView.java:267-270 | |
| 手柄定位 | ABSOLUTE top 10，left=percent%·W − 5 | gradient/GradientEditorView.java:135-137,156 | |
| 棋盘格 tile | 8px 双色格 | gradient/GradientEditorView.java:361-368 | 注释引 common.css 16px tile/8px 格 |
| 取色器 | LDLib2 ColorConfigurator widthPercent 100 | gradient/GradientEditorView.java:293-295 | 偏离 Chrome 取色器 |

## 10. 事件编辑器（events/）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| EventListView 添加钮 | widthPercent 100，height 16 | events/EventListView.java:73 | BAR 底 hover INTERFACE（:68-72） |
| EventListView 行 header | height 16，ROW，gap 2，BAR 底 | events/EventListView.java:158-159 | |
| grip 钮 | width 12，图标 10 | events/EventListView.java:160-163 | 拖拽排序 |
| 「Event ID」label | width 52，fontSize 9，TEXT | events/EventListView.java:175-177 | |
| rename 框 | flex 1，fontSize 9 | events/EventListView.java:182,185 | |
| Remove Event 钮 | fontSize 9，TEXT_GRAYED，hover SELECTION | events/EventListView.java:189-193 | |
| EventListInputView 行 | height 14，ROW | events/EventListInputView.java:66 | label flex 1（fontSize 9，:69-70） |
| EventListInputView 删除钮 | width 16（图标 x） | events/EventListInputView.java:72-77 | 容器 gap 2（:34） |
| EventTimelineView 容器 | COLUMN，gap 2 | events/EventTimelineView.java:44,90 | |
| timeline 时间框 | width 48，height 14，fontSize 9 | events/EventTimelineView.java:97,104 | |
| timeline 行 | ROW，gap 2；事件行 height 14 | events/EventTimelineView.java:93,113 | |
| timeline 删除钮 | width 16（行内 heightPercent 100 / 条目 height 14） | events/EventTimelineView.java:124,140 | 图标 x size 8（:120） |
| EventSpeedListView 行 | height 14，ROW，gap 2 | events/EventSpeedListView.java:78 | 容器 gap 2（:44） |
| speed 框 | width 48，fontSize 9 | events/EventSpeedListView.java:101,106 | 「Min Speed」label fontSize 9 GRAYED（:97） |
| speed 删除钮 | width 16 | events/EventSpeedListView.java:91 | |
| EventPickerView 触发钮 | width 22，height 14，图标 10 | events/EventPickerView.java:52-53 | zap/plus |
| EventPickerView 项/空提示 | widthPercent 100，height 14，fontSize 9 | events/EventPickerView.java:73,85 | 项 DARK 底 hover INTERFACE |
| EventSubpartView 各 section | widthPercent 100，COLUMN | events/EventSubpartView.java:118,134,161,183,303 | 行 ROW gap 2（:188,221,241,248,306） |
| EventSubpartView label | width 52，fontSize 9，TEXT | events/EventSubpartView.java:470-472 | |
| EventSubpartView 输入框 | flex 1，fontSize 9 | events/EventSubpartView.java:194,225,254,312,479-480 | Expression section 框 height 14（:168） |
| descriptorBar | height 14，BAR 底；label fontSize 9 TEXT_GRAYED | events/EventSubpartView.java:381-386 | |
| optionHeader | height 14，ROW gap 2；grip width 12 图标 10 | events/EventSubpartView.java:398-402 | |
| Weight 框 | width 48 | events/EventSubpartView.java:142 | |
| type dropdown 当前值/项 | height 14，fontSize 9；DARK 底 | events/EventSubpartView.java:266-270,279-291 | |
| create 钮「+ X」 | height 14，fontSize 9，TEXT_GRAYED，DARK 底 | events/EventSubpartView.java:425-431 | |
| 尾部「＋ name」钮 | height 14，fontSize 9，BAR 底 | events/EventSubpartView.java:438-444 | |
| iconButton（disable/remove） | width 16，图标 9 | events/EventSubpartView.java:450-451 | |
| textButton | height 14，fontSize 9，BAR 底 | events/EventSubpartView.java:458-464 | |
| EventUiEvents | 无布局数值 | events/EventUiEvents.java | 覆盖确认 |

## 11. 贴图编辑器（texture/TextureEditorView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 根容器 | widthPercent 100，COLUMN，gap 2 | texture/TextureEditorView.java:125 | |
| 工具栏 | height 20，ROW，alignItems CENTER，gap 1 | texture/TextureEditorView.java:129-130 | |
| 工具钮 5 枚 | width 18，height 14，图标 10 | texture/TextureEditorView.java:230-232 | 选中 TITLE 底+HIGHLIGHT 字（:240-246） |
| undo/redo 等小钮 | width max(14, len*5+6)，height 14，fontSize 8 | texture/TextureEditorView.java:253,257 | |
| 图标小钮 | width 18，height 14，图标 10，BAR 底 hover TITLE | texture/TextureEditorView.java:264-268 | |
| 颜色预览块 | width 18，height 18，marginAll 1 | texture/TextureEditorView.java:141 | 初值 0xFFFFFFFF |
| 取色器 overlay | ABSOLUTE right 0，top 22，width 120，INTERFACE 底 | texture/TextureEditorView.java:153-154 | |
| viewport | widthPercent 100，height VIEWPORT_SIZE+2 = 258 | texture/TextureEditorView.java:74,182 | VIEWPORT_SIZE=256（resize_line 未移植） |
| 信息栏 | height 12，ROW，gap 2 | texture/TextureEditorView.java:161-162 | 3 文本各 flex 1 |
| 信息文本 | height 10，fontSize 8，TEXT_GRAYED | texture/TextureEditorView.java:275-276 | |
| meta 工具栏 | height 16，ROW，gap 1 | texture/TextureEditorView.java:173 | x/refresh-ccw/file-plus-2/save |
| 新建对话框宽高框 | width 48，height 14；form gap 2 paddingAll 2 | texture/TextureEditorView.java:328,332,334 | setNumbersOnlyInt(1,4096)（:330） |
| wrapper 边框 | 1px BORDER 色外扩 | texture/TextureEditorView.java:873-874 | |
| 棋盘格双色 | `0xFF6B6B6B` / `0xFF454545`，8px 格 | texture/TextureEditorView.java:847-848,876-877 | JS .checkerboard |
| UV 周界框 | TEXT 色描边 | texture/TextureEditorView.java:885-886 | drawOutline |
| UV 采样框 | ACCENT 色描边 | texture/TextureEditorView.java:890-891 | |
| UV 尺寸手柄 | 10×10，TEXT 填充 + BORDER 描边；命中盒 10px | texture/TextureEditorView.java:629-630,896-897 | JS -10px 偏移 10×10 |
| brush outline | `0xFFFFFFFF` 单像素白描边 | texture/TextureEditorView.java:908 | difference 混合 → 单层近似 |
| 缩放 | ctrl+滚轮 ×/÷1.1；shift+滚轮平移 ±50 | texture/TextureEditorView.java:711-715 | 行为常量 |
| TextureBridge | 无布局数值（编解码接缝） | texture/TextureBridge.java | 覆盖确认 |

## 12. QuickSetup（quicksetup/QuickSetupView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 根容器 | widthPercent 100，COLUMN，INTERFACE 底 | quicksetup/QuickSetupView.java:43-46 | |
| group 标题 | height 12，paddingLeft 12，paddingTop 10，fontSize 10，TEXT_GRAYED | quicksetup/QuickSetupView.java:174-175 | |
| 选项列表 | flexWrap WRAP，gap 2 | quicksetup/QuickSetupView.java:185-186 | |
| 选项格（shape/timing/physics） | width 80，height 34，COLUMN，居中，gap 1；选中 BAR 底 | quicksetup/QuickSetupView.java:191-199 | |
| sprite 格 | width 80，height 64，paddingAll 8 | quicksetup/QuickSetupView.java:218 | |
| sprite 图 | ABSOLUTE left 17，top 2，45×45 | quicksetup/QuickSetupView.java:230-231 | |
| sprite 标签 | ABSOLUTE left 0，right 0，bottom 2 | quicksetup/QuickSetupView.java:238-239 | |
| 许可开关「©」 | ABSOLUTE right 8，top 4，14×12，fontSize 9 TEXT_GRAYED | quicksetup/QuickSetupView.java:123,133-134 | |
| 许可文本 | paddingHorizontal 12，paddingTop 2，fontSize 9，TEXT_GRAYED | quicksetup/QuickSetupView.java:137-140 | |
| 滑杆行 | height 14，ROW，alignItems CENTER，paddingHorizontal 14，gap 8，marginVertical 5 | quicksetup/QuickSetupView.java:258-264 | |
| 滑杆数值 label | width 46，居中 | quicksetup/QuickSetupView.java:269-270 | |
| 滑杆 | flex 1，height 12（LDLib2 Slider.Horizontal） | quicksetup/QuickSetupView.java:272,281 | 非 kit SsSlider |
| toggle 行 | height 14，ROW，paddingHorizontal 14，gap 8 | quicksetup/QuickSetupView.java:291-296 | |
| SPRITE_FRAMES | {16,1}×3, {16,8}, {8,8}, {16,4}, {16,8} | quicksetup/QuickSetupView.java:33-36 | 资产尺寸 |
| ClasspathSpriteTextures | 无布局数值 | quicksetup/ClasspathSpriteTextures.java | 覆盖确认 |

## 13. CodeViewer（menu/CodeViewerView.java）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 容器 | 100%×100%，COLUMN，BACKGROUND 底 | menu/CodeViewerView.java:41-42 | |
| MENU_HEIGHT | 22 | menu/CodeViewerView.java:33 | menuRow height（:57） |
| Copy 钮 | width 48，height 14，marginAll 4，fontSize 10，BAR 底 hover SELECTION | menu/CodeViewerView.java:48-55 | |
| 代码区 | ScrollerView flex 1 | menu/CodeViewerView.java:59-60 | |
| 代码行 | widthPercent 100，height 10，fontSize 9，TEXT | menu/CodeViewerView.java:108-109 | SsJsonColors 着色 |

## 14. 对话框（dialog/）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| blackout 遮罩 | `0x50000000`（#00000050），全屏 ABSOLUTE 居中 | dialog/PlaceholderBakeDialogView.java:39,49-50；dialog/WarningDialogView.java:32,37-38 | |
| Bake 面板 | width 308，COLUMN，paddingAll 10，gap 4，INTERFACE 底 | dialog/PlaceholderBakeDialogView.java:56-61 | JS max-width 308px |
| Bake 列表 | height min(140, max(18, n·16)) | dialog/PlaceholderBakeDialogView.java:99 | |
| Bake 行 | height 16，ROW，alignItems CENTER，gap 4 | dialog/PlaceholderBakeDialogView.java:106-110 | label flex 1 fontSize 9（:112-113） |
| Bake 值框 | width 60，height 14 | dialog/PlaceholderBakeDialogView.java:122 | |
| Bake 钮 | width 30，height 14，fontSize 9，BAR 底 hover SELECTION | dialog/PlaceholderBakeDialogView.java:126-136 | |
| Bake 底部确认行 | height 16，ROW，gap 6 | dialog/PlaceholderBakeDialogView.java:160-163 | |
| Bake 关闭钮 | widthPercent 100，height 16 | dialog/PlaceholderBakeDialogView.java:195-196 | |
| Warning 面板 | width 280，COLUMN，paddingAll 10，gap 4，INTERFACE 底 | dialog/WarningDialogView.java:46-51 | |
| Warning 标题行 | height 14；标题 fontSize 12 HIGHLIGHT；关闭钮 width 14 | dialog/WarningDialogView.java:55-68 | |
| WARNING_COLOR | `0xFFFFC107`（#ffc107） | dialog/WarningDialogView.java:31 | |
| Warning 列表 | height min(160, max(20, n·14)) | dialog/WarningDialogView.java:78 | |
| Warning 行 | widthPercent 100，paddingVertical 4，fontSize 9 | dialog/WarningDialogView.java:80-82 | |

## 15. 舞台与页脚（stage/）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| ParticleStageView 容器 | COLUMN；canvas flex 1；footer widthPercent 100 | stage/ParticleStageView.java:112,115,127 | |
| 时间戳 overlay | ABSOLUTE left 4，top 2，fontSize 9，TEXT_GRAYED | stage/ParticleStageView.java:120-123 | #overlay_timestamp |
| TICK_GATE_MS / FPS / PARTICLES / WARNINGS 周期 | 32 / 1000 / 200 / 500 ms | stage/ParticleStageView.java:87-91 | 行为常量（非布局） |
| gizmo 轴色 | R `0xFD3043`，G `0x26EC45`，B `0x2D5EE8` | stage/ParticleStageView.java:185-187 | 无 alpha 字节的 RGB 常量 |
| 网格色 | `0x3D4954` | stage/ParticleStageView.java:188 | |
| 网格范围 | i ∈ [-32, 32]，y=-0.0005 | stage/ParticleStageView.java:313,318 | |
| 26.1 降级提示 | 文字 `0xFF939AA3` | stage/ParticleStageView.java:680 | |
| OrbitCamera | FOV 45，aspect 16/9，near 0.1，far 3000；初始位置 (-6,3,-6)，目标 (0,0.8,0) | stage/OrbitCamera.java:42-49 | three 相机常量（非布局） |
| 页脚高度 HEIGHT | 22 | stage/StageFooterBar.java:36 | |
| CONTROL_HEIGHT | 14 | stage/StageFooterBar.java:37 | |
| 页脚 bar | height 22，ROW，alignItems CENTER，paddingHorizontal 4，gap 4，DARK 底 | stage/StageFooterBar.java:61-63,75-81 | |
| placeholder 栏 | widthPercent 100，ROW，alignItems CENTER，paddingH 4，gap 4，BAR 底（默认 NONE） | stage/StageFooterBar.java:66-73 | |
| loop/parent 选择器 | width 56，height 14 | stage/StageFooterBar.java:241 | |
| 工具钮（collision/hash/play/pause） | width 16，height 14，图标 10 | stage/StageFooterBar.java:249-250 | |
| 弹性分隔 | flex 1，height 1 ×2 | stage/StageFooterBar.java:102,107 | |
| placeholder 字段 | label height 14；输入框 width 50，height 14 | stage/StageFooterBar.java:175,180 | |
| Bake 钮 | width 36，height 14，fontSize 9 | stage/StageFooterBar.java:190-194 | |
| fps label | width 40，fontSize 9 | stage/StageFooterBar.java:119-120,266-267 | |
| warning label | fontSize 9，ACCENT 色 | stage/StageFooterBar.java:109,266-267 | |
| PlaybackController | 无布局数值（播放逻辑） | stage/PlaybackController.java | 覆盖确认 |

## 16. IO 与其他（io/、package-info、Gate）

| 元素/属性 | 当前值 | 来源(file:line) | 备注 |
|---|---|---|---|
| io/EditorFileActions、io/SubEffectEditorActions | 无布局数值（文件 IO / 子效果栈逻辑） | io/EditorFileActions.java；io/SubEffectEditorActions.java | 覆盖确认 |
| SnowstormEditorGate | 无布局数值 | SnowstormEditorGate.java:13-19 | 反射入口 |
| 各 package-info.java ×9 | 无数值 | 各子包 | 覆盖确认 |

---

## 附：与原版对齐前的已知双事实源/近似（汇总）

1. `SsMetrics.TAB_BAR_HEIGHT=45` vs `SidebarView.TAB_BAR_HEIGHT=22`（两处定义，后者生效）。
2. `SsMetrics.HELP_BUTTON 30×32` vs `SsGroupSection` 实际 width 14。
3. `SsMetrics.INPUT_HEIGHT=24` vs `InputViewFactory.ROW_HEIGHT=12`。
4. `SsMetrics.TAB_PADDING_*`/`SUBJECT_TITLE_*`/`BUTTON_PADDING_*` 定义未被使用。
5. `SidebarView.GROUP_HEADER_HEIGHT=14` 死常量。
6. `CurveEditorView` 颜色本地复制 7 份（含 `--color-curve #657c86` Theme 缺失、SELECTION alpha 0x4C vs 0x4D）。
7. `CurveAddButton` hover 色 `0xFF232B32` 为 --color-dark(#20272d) 的「近似」，非精确值。
8. 贴图编辑器棋盘格 `0xFF6B6B6B`/`0xFF454545` 为自绘灰，非 common.css 变量。
