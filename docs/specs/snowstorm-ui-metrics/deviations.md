# Snowstorm UI 复刻差距清单（原版 v3.2.2 vs Java 复刻）

> 基准原版：`build/_snowstorm_src/`（Snowstorm v3.2.2 克隆）；复刻：`src/main/java/io/github/tt432/eyelib/client/gui/snowstorm/`。
> 每条差距均双侧溯源（原版 file:line + Java file:line）；Java 侧行号已对源码逐一抽查复核（current-state-java.md 仅作线索）。
> 严重度：**P0** = 结构/层级错误（用户一眼看出）；**P1** = 尺寸/间距数值偏离；**P2** = 颜色/hover 等细节。
> 排除：字体渲染差异（用户豁免，字号数值不逐条比对）；舞台 3D 渲染（GL_LINES/深度/scissor 已修复，本节仅列页脚/覆盖层等 UI chrome 差距）。
> 复刻单位说明：Java 侧为 LDLib2 布局像素（MC gui 缩放后），部分视图按「web ÷2 等比」硬编码；下表「差距」列按原版 web px 对复刻 MC px 直记，等比意图在备注中注明。

## 1. App 骨架

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 1 | sidebar resizer（6px 拖拽条 + 收起钮 30×30@top:120） | 6px 宽、margin-left -3px、ew-resize，可拖拽调宽/收起（App.vue:319-326,213-225,134-142） | 固定宽，无 resizer、无收起钮（SnowstormEditorScreen.java:153） | 宽度不可调、不可收起；拖拽 clamp(240, w-200) 与 localStorage 持久化均缺 | P1 |
| 2 | sidebar 右边框 | border-right 1px solid #1a1c1f（Sidebar.vue:156-158） | 无边框（SidebarView.java:57-59） | 缺 1px 分隔线 | P2 |
| 3 | sidebar 滚动区底部留白 | padding-bottom 60px（Sidebar.vue:159-163） | 无（ScrollerView 直接 flex 1，SidebarView.java:75-76） | 末组贴底 | P2 |

## 2. MenuBar

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 4 | #menu_bar 高度 | 32px（MenuBar.vue:166） | 40px（SnowstormEditorScreen.java:45 MENUBAR_HEIGHT=40，:118） | +8px；并挤压 ExpressionBar（见 #9） | P1 |
| 5 | 顶层菜单按钮宽度 | padding 2px 12px（padding-top 3px），宽度随文字自适应（MenuBar.vue:172-177） | 固定 width 48（MenuBarView.java:249） | 「Examples」等长文本被裁/挤压，短文本过宽 | P1 |
| 6 | Help 图标钮 | HelpCircle lucide 图标 20px（MenuBar.vue:21-23） | 文字「?」width 24（MenuBarView.java:160,267） | 图标形态差异 | P2 |
| 7 | Share 图标钮 | Share2 20px，canShare 时显示（MenuBar.vue:24-26） | 未移植（MenuBarView.java:36-42 文档化排除，navigator.share 恒 false 分支） | 按钮缺失（文档化偏离） | P2 |
| 8 | 下拉弹层项 | 项高 34px、padding-top 5px、min-width 150px、box-shadow 1px 4px 10px rgba(0,0,0,.25)（MenuBar.vue:184-199） | LDLib2 Menu 默认行高/无阴影，width 150（MenuBarView.java:290-294） | 行高/内距/阴影不一致（min-width 150 一致） | P2 |
| 9 | （联动）header 内 ExpressionBar 实得高度 | 74 − 32 − 1(边框) ≈ 41px（App.vue:232 + MenuBar.vue:166 + ExpressionBar.vue:97） | 74 − 40 = 34px（SnowstormEditorScreen.java:43,45,135） | −7px | P1 |

## 3. ExpressionBar

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 10 | 底边框 | 1px solid #1a1c1f（ExpressionBar.vue:97） | 无（ExpressionBarView.java:69-77，仅 DARK 底） | 与下方舞台无分隔线 | P2 |
| 11 | 内边距 | padding 5px 8px（ExpressionBar.vue:103） | TextField marginAll 3、高 14（ExpressionBarView.java:74） | 垂直 −4px、水平 −5px | P1 |
| 12 | 语法高亮/自动补全 | Prism token 五色 + 补全弹层（max-height 220 等）（common.css:341-374） | 纯文本 TextField，无高亮无补全（ExpressionBarView.java:22-27 文档化排除） | token 着色与补全弹层整体缺失（文档化偏离） | P2 |

## 4. Sidebar（骨架 / tab 栏 / 标题 / Logo / 浮层）

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 13 | HelpPanel 文档面板 | 右浮层宽 482px、top 32 bottom 33、z5，含目录/搜索/类型徽标（HelpPanel.vue:82-153） | 未移植：help「?」回调为空占位（SidebarView.java:163-165）；MenuBar 的 openHelpPage 亦未接线（SnowstormEditorScreen.java:116-132 无 setOnOpenHelpPage） | 整个 482px 面板缺失，入口全部无效 | **P0** |
| 14 | InfoBox 顶部信息条 | 宽 500px、top 20、z50、阴影 0 0 15px（InfoBox.vue:16-35） | 无对应组件（全包无 InfoBox 引用） | 组件缺失 | **P0** |
| 15 | tab 栏高度 | 45px（Sidebar.vue:165） | 22px（SidebarView.java:46,63；注释自述 web45→MC22 等比） | −23px（等比意图；与像素级对齐目标冲突） | P1 |
| 16 | tab 图标尺寸 | lucide 默认 24×24（Sidebar.vue:11-19） | SsIcon 12px（SidebarView.java:47,124-125） | ½ | P1 |
| 17 | tab hover 样式 | 仅字色 → #f7f9ff，背景不变（Sidebar.vue:194-196） | SELECTION 半透明背景块（SidebarView.java:119-120） | hover 反馈形式不同 | P2 |
| 18 | tab tooltip | 高 28px、bottom -28px、默认隐藏 hover 显示（Sidebar.vue:197-217） | 无 tooltip（SidebarView.java:109-130） | 缺失 | P2 |
| 19 | Logo 显示 | svg height:auto + padding 12px，按容器宽等比伸缩（436px 侧栏 ≈ 73px 总高）（Logo.vue:77-82,11-13） | SpriteTexture 固定 height 16、宽 100%（SidebarView.java:79-83） | 高度 −57px 且宽高比失真（拉伸变形） | P1 |
| 20 | Logo 版本号 | span 右浮、margin-right 12 / margin-top 21、色 #4b5b69（Logo.vue:83-88） | 无版本号（SidebarView.java:79-83） | 缺失 | P2 |
| 21 | subject 标题背景 | 无背景（仅灰字居中，Sidebar.vue:232-239） | 整条 TITLE(#4b5b69) 实色背景带（SidebarView.java:73） | 多出原版不存在的高亮色带，极醒目 | **P0** |
| 22 | subject 标题间距 | margin 8px、margin-bottom -2px（Sidebar.vue:232-233） | height 14 + marginVertical 4（SidebarView.java:69-72） | 上 −4 / 下 +6 | P1 |
| 23 | effect 页 Quick Setup 入口钮 | display:block margin:auto margin-top:16px；button 基线 padding 8px 12px + Wand 24px 图标（Sidebar.vue:48-53,226-230；common.css:179-188） | 固定 90×14、marginTop 8、「✦ Quick Setup」文本（SidebarView.java:148-155） | 高度 ≈40→14；图标变字符；上距减半 | P1 |

## 5. Group 区块与 input 行

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 24 | group 头对齐与折叠符 | h4 文本左对齐、padding 10px（左 12），**无折叠箭头字符**（Sidebar.vue:273-282,45） | LDLib2 Button 文本居中 + 自加「▾/▸ 」前缀（SsGroupSection.java:30-40） | 居中 vs 左对齐；多出原版没有的箭头 glyph | **P0** |
| 25 | group 头高度/内距 | padding 10px → 行高 ≈ 17.6×1.2+20 ≈ 41px（Sidebar.vue:273-276） | 高 32（10×2+12）、paddingVertical 仅 2（SsGroupSection.java:32-35） | −9px；上下内距 10→2 | P1 |
| 26 | group 头 help「?」钮 | 30×32、margin -5px -9px、float right（Sidebar.vue:218-225,283-285） | width 14（SsGroupSection.java:43） | 不足一半，点击热区小 | P1 |
| 27 | 组间分隔线 | 仅非末组：:not(:last-of-type) border-bottom 1px #1a1c1f（Sidebar.vue:270-272） | 每组（含末组）都画 1px 线（SsGroupSection.java:65-68） | 末组多一条线 | P2 |
| 28 | input 行高 | 控件 30px + 行距 margin 2px 0 → ≈34px/行（common.css:83；InputGroup.vue:264-266） | ROW_HEIGHT 12（InputViewFactory.java:35,54-59） | 约 1/3 密度过高 | P1 |
| 29 | input 行 label | 宽 100px、text-align right、margin 3px 0（InputGroup.vue:267-271） | 宽 62、未设右对齐（InputViewFactory.java:38,65-66） | −38px；左对齐 vs 右对齐 | P1 |
| 30 | expand 展开钮 | 宽 22px、Chevron 图标 20px、float right（InputGroup.vue:298-303,14-15） | 宽 12、图标 10（InputViewFactory.java:71-76） | ≈½ | P2 |
| 31 | 文本/数字输入框边框 | 1px solid #1a1c1f（common.css:81；prism 外框 common.css:328-331） | 无 1px 边框（SsTextField.java:24-25 注释自述 LDLib2 无等价；NumberInputView.java:51-67 仅 DARK 底） | 输入框无描边 | P2 |
| 32 | number 文字色 | #b99cff（common.css:366-368） | 未设置（TextField 默认色，NumberInputView.java:53） | 紫色数字 → 默认色 | P2 |
| 33 | checkbox | Square/CheckSquare 21×21，勾选 path 色 #20ddff、stroke-width 3（Checkbox.vue:4-5,47-50） | 图标 10px，无 accent 着色（CheckboxInputView.java:39） | ½；勾选态无青色 | P1 |
| 34 | 取色器（color input / gradient / texture overlay 三处） | vue-color Chrome 面板宽 310px、字段高 24px（common.css:286-300；TextureInput.vue:758-762） | LDLib2 ColorConfigurator（ColorInputView.java:27-37；GradientEditorView.java:268-273；TextureEditorView.java:153-157 宽 120） | 面板形态/宽度完全不同（文档化偏离） | P1 |
| 35 | list 添加钮「＋」 | 内联 .list_add_tool、unicode ＋ 18pt（InputGroup.vue:19,330-332） | 全宽独立行、高 10（ListInputView.java:49-57） | 内联小钮 → 整行按钮 | P2 |
| 36 | list 行删除钮「✕」 | 宽 24px、高 30px（InputGroup.vue:317-321） | 宽 10（ListInputView.java:83） | 热区 ≈½ 不到 | P2 |

## 6. Curve 曲线编辑器

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 37 | .curve 容器内距 | padding-top 12px、bottom 8px（Curve.vue:421-423） | 无 padding（CurveEditorView.java:118） | 曲线块上下少 12/8px | P2 |
| 38 | 点选项行（.curve_point_options） | label padding 4px 8px + number input 30px 高 → 行 ≈30-38px（Curve.vue:567-581） | 行高 14（CurveEditorView.java:314,326-329） | ≈½ | P1 |
| 39 | Add Curve 钮 | 高 ≈26（padding 2 + 图标 20 + 边框），hover 底 #20272d（ListAddButton.vue:22-34；Sidebar.vue:251-259） | 高 24，hover 底 0xFF232B32（CurveAddButton.java:25,28） | 高 −2；hover 色为 #20272d 的非精确近似 | P2 |
| 40 | 节点/手柄形状 | CSS 圆形（radius 50%），8×8 / 选中 10×10（Curve.vue:508-519,533-539） | 方块近似（半径 4/5 方点）（CurveEditorView.java:782-783,811） | 圆 → 方（渲染细节） | P2 |

## 7. Gradient 渐变编辑器

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 41 | ＋/⨯ 工具钮 | .tool 基线宽 35px、unicode_icon 24×24（Gradient.vue:15-20；App.vue:183-190；common.css:196-204） | 宽 14、图标 10（GradientEditorView.java:258-263） | ≈½ | P2 |
| 42 | 取色器 | 见 #34（vc-chrome 310px） | GradientColorConfigurator（GradientEditorView.java:268-273） | 同 #34 | P1 |

## 8. Events 事件系统

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 43 | Add Event 钮 | 虚线框 ListAddButton（1px dashed #34404a，hover 底 #20272d）（ListAddButton.vue:22-34；EventList.vue:20） | 实心 DARK 底 + plus 图标、高 16（EventListView.java:65-73） | 虚线框 → 实心块 | P1 |
| 44 | event 项间距/分隔 | padding 20px 6px；border-top 2px #34404a（首项透明）（EventList.vue:98-107） | 无 padding、无分隔边框（EventListView.java:153-159，COLUMN 直排） | 事件块粘连 | P1 |
| 45 | event 头条高度 | padding 5px + input 30px → ≈40px（EventList.vue:114-119；common.css:83） | 高 16（EventListView.java:158） | −24px | P1 |
| 46 | 「Event ID」label | min-width 80px、padding 4px、右对齐（EventList.vue:120-124） | width 52（EventListView.java:175-177） | −28px | P1 |
| 47 | event_subpart 层级左边框 | border-left 5px #34404a + padding-left 12px（hover 变 selection 色）（EventSubpart.vue:290-298） | 无左边框、无缩进（EventSubpartView.java 全文无 border/paddingLeft，见 :115-145） | 嵌套层级视觉指示整体缺失 | **P0** |
| 48 | section 标题条 | .section_bar 仅 margin 8/4，无背景；.descriptor_label 灰字（EventSubpart.vue:366-374） | descriptorBar 整条 BAR(#34404a) 实底（EventSubpartView.java:381-386） | 多出实色背景条 | P2 |
| 49 | subpart 输入行 label | 宽 95px、右对齐（EventSubpart.vue:380-386） | 宽 52（EventSubpartView.java:189,222,242,249,307） | −43px | P1 |
| 50 | event_timeline 竖线与节点 | 竖线 6px 宽 #34404a radius 3（left -11）；节点 16×16 圆 #4b5b69（InputGroup.vue:385-422） | 均无（EventTimelineView.java:88-145 纯行布局） | 时间轴可视化缺失 | P1 |
| 51 | timeline 时间输入框 | 宽 69px（InputGroup.vue:426-428,151） | 宽 48（EventTimelineView.java:104） | −21px | P2 |
| 52 | 事件 chip | 高 30、padding 4px 13px、radius 5、阴影 0 1px 14px、底 #34404a（InputGroup.vue:341-349） | 纯文本行高 14，无 chip 底/圆角/阴影（EventTimelineView.java:113-118；EventListInputView.java:66） | chip 形态退化为文本行 | P1 |
| 53 | EventPicker 弹层 | absolute 弹层宽 200px、项高 32px、radius 2（EventPicker.vue:77-100） | 行内展开列表、项高 14（EventPickerView.java:51-56,73,85） | 弹层 → 内联；项高 ≈½ 弱 | P1 |

## 9. Texture 贴图编辑器

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 54 | 棋盘格颜色 | #29323a / #34404a（common.css:223-229） | 0xFF6B6B6B / 0xFF454545 自绘亮灰（TextureEditorView.java:847-848） | 色系完全偏离主题（明显偏亮） | P1 |
| 55 | 工具栏 | 高 34px、margin-top 6px；.tool 宽 40px padding 4px、图标 24px（TextureInput.vue:715-728,5-26） | 高 20；钮 18×14、图标 10（TextureEditorView.java:128-130,225-235） | ≈½ | P1 |
| 56 | 颜色预览钮 | 34×34 圆形、1px border、棋盘格底（TextureInput.vue:739-750） | 18×18 方块（TextureEditorView.java:140-141） | 尺寸 ½、圆 → 方、无棋盘格底 | P1 |
| 57 | resize_line 高度拖拽条 | 高 12px、ns-resize，可视 2px 线（TextureInput.vue:769-786） | 未移植，viewport 固定 258（TextureEditorView.java:73-74,182） | 拖拽条缺失、视口高不可调（文档化偏离） | P1 |
| 58 | viewport 外扩 | margin-left -12px / right -6px 顶破侧栏 padding（TextureInput.vue:616,621-623） | 无外扩（根容器 gap 2，TextureEditorView.java:125,182） | 视口比原版窄且不破边 | P2 |
| 59 | 帧切换/居中钮 | ArrowBigLeft/Right、Maximize 图标 20px（TextureInput.vue:69-78） | 文字「<」「>」+ maximize 图标 10（TextureEditorView.java:166-169） | 图标形态差异 | P2 |
| 60 | 信息栏 | .info flex 1 1 80px、padding-top 5px、居中（TextureInput.vue:707-714） | 高 12、fontSize 8（TextureEditorView.java:160-162,272-277） | 行高约 ½ | P2 |
| 61 | 取色器 overlay | 见 #34（vc-chrome 310px，absolute right 0）（TextureInput.vue:758-762；common.css:286-290） | 宽 120 ColorConfigurator（TextureEditorView.java:153-157） | 同 #34 | P1 |

## 10. QuickSetup 面板

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 62 | 组标题 h4 | padding 10px（左 12）、1.2em → ≈41px 高（Sidebar.vue:273-279；QuickSetup.vue 各组复用 .input_group） | height 12 + paddingTop 10（QuickSetupView.java:174-175） | ≈½ | P1 |
| 63 | 组间分隔线 | 非末组 border-bottom 1px #1a1c1f（Sidebar.vue:270-272） | 无分隔线（QuickSetupView.java:169-178 groupBlock 无 divider） | 缺失 | P2 |
| 64 | 预设图标 | 38×38、stroke-width 1（QuickSetup.vue:7 等 11 处；:533-536） | SsIcon 16（QuickSetupView.java:202） | ≈40% | P1 |
| 65 | 预设格尺寸 | 宽 80、padding 8px，高 ≈ 38 图标 + 文字 + 16 内距 ≈ 62-70px（QuickSetup.vue:521-526） | 固定 80×34（QuickSetupView.java:191-192） | 高度 ≈½，图标文字挤压 | P1 |
| 66 | 预设列表排布 | flex 横向 overflow-x auto 滚动（QuickSetup.vue:516-520） | flexWrap WRAP 换行（QuickSetupView.java:180-187，文档化偏离） | 横滚 → 换行 | P2 |
| 67 | 滑杆外观 | track 高 4px #34404a；thumb 20×20 圆、2px #20ddff 环 + #29323a 心（common.css:99-166） | LDLib2 Slider.Horizontal 原生样式、高 12（QuickSetupView.java:272,281；非 kit SsSlider） | 轨道/thumb 形态完全不同 | P1 |
| 68 | input_bar 外距 | margin 10px 14px（QuickSetup.vue:595-600） | paddingH 14 + marginV 5（QuickSetupView.java:257-264） | 垂直 10→5 | P2 |
| 69 | 预设格 hover | hover 字色 #f7f9ff（QuickSetup.vue:527-532） | 无 hover 样式（QuickSetupView.java:197-201 仅 selected 底） | hover 反馈缺失 | P2 |
| 70 | CC 许可图标 | CreativeCommons 图标 20px（QuickSetup.vue:83） | 文字「©」14×12（QuickSetupView.java:121-134） | 图标 → 字符 | P2 |
| 71 | sprite 格高度 | padding 8 + img 45 + 标签 → ≈77px（QuickSetup.vue:90-116,537-542） | 固定 64（QuickSetupView.java:218） | −13px | P2 |

## 11. CodeViewer

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 72 | .menu 高度 | flex 0 0 42px、padding 5px（CodeViewer.vue:76-79） | 高 22（CodeViewerView.java:33,56-57） | −20px | P1 |
| 73 | Copy 钮 | 宽 100px、Copy 图标 20px + 文字、padding 8px 12px（CodeViewer.vue:4,80-85；common.css:179-188） | 48×14、纯文字（CodeViewerView.java:47-55） | 宽 −52、高 −26、无图标 | P1 |
| 74 | pre 背景 | #20272d（CodeViewer.vue:59-71） | 无背景（透出根容器 BACKGROUND #29323a）（CodeViewerView.java:43-44,59-60） | 代码区底色错误 | P2 |
| 75 | pre 宽度约束 | 宽 100%、max-width 1000px 居中（CodeViewer.vue:59-71） | 全宽无约束（CodeViewerView.java:60） | 宽屏下代码通栏 | P2 |
| 76 | 语法高亮主题 | prism-okaidia + 覆盖（property #7bcbf0、boolean/number #ff6868 等）（CodeViewer.vue:12,92-100） | SsJsonColors 用表达式栏 token 色（#5ba8c5/#94e400/#b99cff）（SsJsonColors.java:19-22；CodeViewerView.java:106-107） | 用错配色体系 | P2 |

## 12. 对话框

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 77 | Warning 面板宽 | 800px、max-width 100%（App.vue:297-301） | 280px（WarningDialogView.java:46-47） | −520px | P1 |
| 78 | Warning 面板内距 | padding 20px 28px（App.vue:305） | paddingAll 10（WarningDialogView.java:49） | 显著偏紧 | P1 |
| 79 | Warning 面板边框/圆角/阴影 | 1px solid #34404a、radius 4px、shadow 0 1px 12px rgba(0,0,0,.4)（App.vue:304,313-315） | 均无（WarningDialogView.java:51 仅 INTERFACE 底） | 缺失 | P2 |
| 80 | Warning 面板位置 | top 20 / bottom 20（顶对齐非居中）（App.vue:302-308） | 全屏居中（WarningDialogView.java:40-41） | 垂直位置不同 | P2 |
| 81 | Warning 关闭钮 | absolute top 6 right 6、高 30、unicode ✕ 24×24（App.vue:333-338；common.css:196-204） | 标题行内 width 14「⤫」（WarningDialogView.java:60-68） | 位置/尺寸差异 | P2 |
| 82 | warning 条目 | list-style inside、padding 10px、#ffc107（WarningDialog.vue:66-70） | paddingVertical 4 + 手动「• 」前缀（WarningDialogView.java:80-82） | 内距 −6 | P2 |
| 83 | 新建贴图对话框内距 | .modal_dialog padding 12px、radius 4、border 1px #34404a（common.css:261-269） | form gap 2 paddingAll 2（TextureEditorView.java:328-334） | 内距/边框/圆角缺失 | P2 |

## 13. stage-footer（页脚与画布覆盖层；舞台 3D 渲染已修复，不在此列）

| # | 组件 | 原版值（源） | 复刻值（源） | 差距 | 严重度 |
|---|---|---|---|---|---|
| 84 | footer 高度 | 34px（Preview.vue:421,480） | 22px（StageFooterBar.java:36,77） | −12px | P1 |
| 85 | footer 背景 | #34404a（--color-bar）（Preview.vue:481） | DARK #20272d（StageFooterBar.java:63） | 整bar 底色错误，与原版明暗颠倒 | P1 |
| 86 | footer 顶边框 | 1px solid #1a1c1f（Preview.vue:482） | 无（StageFooterBar.java:61-63） | 与画布无分隔线 | P2 |
| 87 | loop/parent 下拉 | 高 100%（=34）、padding 2px 6px、margin-left 4（Preview.vue:508-510） | 56×14（StageFooterBar.java:241,37） | 高 −20 | P1 |
| 88 | footer 图标钮 | 图标 20/22px、.tool 宽 35 padding 2 8（Preview.vue:34-49；App.vue:183-190） | 16×14、图标 10（StageFooterBar.java:245-250） | ≈½ | P1 |
| 89 | 警告计数颜色 | #ffc107（hover #ffe060）（Preview.vue:521-529） | ACCENT #20ddff（StageFooterBar.java:109） | 琥珀 → 青，语义色错误 | P1 |
| 90 | 粒子/FPS 统计样式 | 色 #bcc3ca、min-width 72、右对齐（Preview.vue:515-520） | TEXT_GRAYED、fps 宽 40（StageFooterBar.java:117-120,263-270） | 色偏暗、宽 −32 | P2 |
| 91 | placeholder 占位变量条 | absolute bottom 34 覆盖画布下缘、min-height 35、90% 透明底 + blur(4px) + 顶边框（Preview.vue:442-450） | 收进页脚流内独立一行、不透明 BAR 底（StageFooterBar.java:65-73） | 位置结构不同；无半透明/模糊 | P1 |
| 92 | placeholder 变量输入框 | 宽 70px（Preview.vue:467-469） | 宽 50（StageFooterBar.java:180，经 current-state-java.md §15 复核） | −20px | P2 |
| 93 | 时间戳 overlay | padding 4px 10px、opacity 0.5、色 #bcc3ca（Preview.vue:432-440） | left 4 top 2、无 padding、TEXT_GRAYED 无半透明（ParticleStageView.java:118-123） | 无半透明、无内距 | P2 |

## 假警报 / 已排除项（疑似但复核后不计入差距）

| 项 | 复核结论 |
|---|---|
| Curve 画布 SELECTION 色 0x4C6E8EBF vs Theme 0x4D6E8EBF | rgba(110,142,191,0.3) 的 alpha 0.3×255=76.5，0x4C=76 / 0x4D=77 仅 1/255 取整差，不可感知；原版源 common.css:40 vs CurveEditorView.java:57 + SnowstormTheme.java:20。不计 |
| 渐变/贴图棋盘格单元尺寸 8px | 原版 background-size 16px 但交错偏移 8px（common.css:223-229），单元格即 8px；Java 8px 格（GradientEditorView.java:361-368）一致。仅贴图棋盘格「颜色」偏离（已列 #54） |
| PlaceholderBake 对话框宽 308px | 原版即内联 max-width 308px（Preview.vue:58 → metrics-05 §6），Java width 308（PlaceholderBakeDialogView.java:57）一致 |
| MenuBar 下拉 min-width 150px | Java width 150（MenuBarView.java:290-294）与 MenuBar.vue:191 一致 |
| 折叠指示条高 32px | Sidebar.vue:290-297 与 SsGroupSection.java:61（FOLDED_INDICATOR_HEIGHT=32）一致 |
| 组内容 ul padding 8/右 2 | Sidebar.vue:286-289 与 SsGroupSection.java:51-53 一致 |
| 舞台网格色 #3d4954 / 轴色 RGB / 参考方块 | 舞台渲染差距已修复验证（GL_LINES/深度/scissor），色值经 ParticleStageView.java:185-188 复核一致 |
| 菜单 hover 展开 → 点击展开 | 交互方式偏离（MenuBarView.java:271-300 文档化），非像素/颜色差距，不计入本表 |
| SsMetrics 死常量双事实源（TAB_BAR_HEIGHT=45 vs 22 等） | 代码卫生问题（未生效常量），渲染以视图硬编码为准——真正差距已按生效值列入上表 |

## 统计

- 差距总数：**93** 条（P0 5 / P1 45 / P2 43）
- **P0 清单（5）**：
  1. #13 HelpPanel 文档面板整体缺失（入口未接线）
  2. #14 InfoBox 顶部信息条组件缺失
  3. #21 subject 标题多出 TITLE(#4b5b69) 实色背景带
  4. #24 group 头文本居中 + 自加「▾/▸」折叠箭头（原版左对齐无箭头）
  5. #47 event_subpart 5px 层级左边框整体缺失
- P1 主诉：尺寸链整体按「web÷2 等比」缩放（tab 栏 45→22、input 行 34→12、event 头条 40→16、footer 34→22 等），与像素级对齐目标系统性冲突；另含 footer 底色 #34404a→#20272d、贴图棋盘格色系偏离、Warning 面板 800→280 等单点错误。

## P3 修复状态（2026-10-01，commit 后回填）

- **已修（P0）**：#13 HelpPanel 新增（`snowstorm/help/HelpData.java` domain 加载 `/snowstorm/help_data.json`，
  UI `client/gui/snowstorm/help/HelpPanelView.java`，入口=MenuBar HelpCircle + group「?」，实测打开正常）；
  #14 InfoBox **不移植**——实证 v3.2.2 App.vue 模板从未渲染 InfoBox（仅 import/注册，死组件）；
  #21/#24/#47 结构修复（标题去实底色带、group 头左对齐无箭头、event_subpart 5px 左边框）。
- **已修（P1/P2）**：尺寸链全改 web px 原值（tab 45、input 行 34、event 头条 40、footer 34、工具钮 35/图标 20-24 等）；
  footer 底色 #34404a + 顶边框；警告色 #ffc107；贴图棋盘格改 #29323a/#34404a；Warning 面板 800px 顶对齐；
  CodeViewer menu 42px + pre 深色底 + okaidia 配色（key #7bcbf0/number #ff6868）+ key 识别；
  输入框 1px border（GuiTextureGroup(ColorRect+ColorBorderTexture(-1))）；checkbox 21px 勾选 ACCENT；
  曲线节点/手柄方→圆（disc 五条带近似）；QuickSetup 格 80px/图标 38/sprite 77px/CC 图标/滑杆 SsSlider；
  sidebar resizer 拖拽+收起钮（MOUSE_DOWN/MOUSE_MOVE/MOUSE_UP，localStorage 持久化降级为会话静态）；
  logo 按 6:1 比例居中 + 版本号（logo.png 重烘焙为 #bcc3ca 填充——原资产 rgb 全 0，SpriteTexture.setColor
  为乘法无法点亮黑色，教训固化）。
- **保留偏离（LDLib2 无等价）**：#12 Prism 语法高亮/补全（文档化）；#34/#61/#42 vc-chrome 取色器（用 LDLib2
  ColorConfigurator，宽已对齐 310）；#57 resize_line；#66 横滚→换行；#7 Share 按钮；dashed 边框→实线近似；
  backdrop blur→半透明近似；菜单 hover 展开→点击展开。
- **验证**：`:1.20.1:compileJava` BUILD SUCCESSFUL；实机截图 `build/_shots/p3_final4.png`（主界面）、
  `p3_help2.png`（HelpPanel）；`:1.20.1:test` BUILD/TEST SUCCESSFUL。
