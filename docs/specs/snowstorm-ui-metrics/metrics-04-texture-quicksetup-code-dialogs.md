# Snowstorm UI 像素指标 04 — TextureInput / 贴图编辑器 / QuickSetup / CodeViewer / WarningDialog

> 基准：`build/_snowstorm_src/`（Snowstorm v3.2.2 克隆源码）。所有路径相对该目录。
> 共享 CSS 变量（`src/css/common.css:33-47`）：
> `--color-background: #29323a`、`--color-dark: #20272d`、`--color-border: #1a1c1f`、`--color-interface: #29323a`、`--color-bar: #34404a`、`--color-title: #4b5b69`、`--color-selection: rgba(110,142,191,0.3)`、`--color-highlight: #f7f9ff`、`--color-text: #bcc3ca`、`--color-text_grayed: #939aa3`、`--color-accent: #20ddff`、`--font-code: Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace`。
> 全局正文：`font-size: 11pt; font-weight: 400; line-height: 1.5; color: #bcc3ca`（`src/css/common.css:237-251`）。

## 区域结构 ASCII 示意图

```
TextureInput（侧边栏内，width:100%，margin-top:8px）
┌──────────────────────────────────────────────┐
│ toolbar (h=34px, mt=6px, flex)               │
│ [🖱][🖌][⌫][🪣][💧]  [↶][↷]      ◯(34×34 色板)│  每个 .tool w=40px padding 4px
│ #color_picker_overlay (absolute, right:0, z=4)│  vc-chrome 宽 310px
├──────────────────────────────────────────────┤
│ texture_viewport (bg #20272d, h=outer+2px,    │
│   ml=-12px mr=-6px, 默认 256+2=258px 高)      │
│   ┌────────────────────────┐                 │
│   │ input_texture_wrapper  │ w=256·zoom px   │
│   │ (checkerboard 16px格,  │ border 1px      │
│   │  canvas 100%×100%)     │ #1a1c1f         │
│   │  UV预览框 + 10px手柄   │                 │
│   └────────────────────────┘                 │
│   滚动条 12px(竖屏16px) 圆角8px               │
├──────────────────────────────────────────────┤
│ texture_info_bar (flex)                      │
│  [256 x 256 px] [x,y] [100%] [◀][▶][⛶20px]   │  info flex:1 1 80px, pt=5px
├─ resize_line h=12px (内含 2px 线 @top:5px) ──┤
│ meta.toolbar (h=34px) [✕][📄][➕][💾]         │
└──────────────────────────────────────────────┘
  └ dialog#new_texture_dialog (.modal_dialog, padding 12px, 圆角4px)

QuickSetup（.quick_setup > 4×.input_group，组间 1px 底边框 #1a1c1f）
┌──────────────────────────────────────────────┐
│ h4 "Shape & Motion" (padding 10px 10px 10px 12px, 1.2em)│
│ ┌─preset_option_list (flex, gap 2px, 横向滚动)─┐│
│ │ [li 80px宽 padding8px] [li] [li] [li]      ││  图标 38×38 stroke 1
│ └────────────────────────────────────────────┘│
│ input_bar (m 10px 14px, gap 8px):            │
│   Speed [═══════○──── range] [46px 数值]      │  thumb 20px / track h4px
├──────────────────────────────────────────────┤
│ h4 "Sprite" [CC图标20px]                     │
│ sprite li: <img height=45, 1:1, cover>       │
└──────────────────────────────────────────────┘

CodeViewer（main#code, flex column）
┌──────────────────────────────────────────────┐
│ .menu (flex 0 0 42px, padding 5px)           │
│   [button 100px宽: ⧉(20px)+Copy]             │
├──────────────────────────────────────────────┤
│ pre (bg #20272d, w 100% max 1000px 居中,     │
│      overflow-y auto, 无边框圆角)             │
│   prism-json 语法高亮（无行号）               │
└──────────────────────────────────────────────┘

WarningDialog（dialog#warnings，全屏遮罩之上）
┌#dialog_blackout 全屏 z=49 rgba(0,0,0,0.31)──┐
│   ┌─dialog (z=50, w=800px, 居中,──────────┐  │
│   │  top/bottom 20px, padding 20px 28px,   │  │
│   │  圆角4px, border 1px #34404a,          │  │
│   │  shadow 0 1px 12px rgba(0,0,0,.4)      │  │
│   │  h2 Warnings              [✕ 30px@6,6] │  │
│   │  .scrollable (overflow-y scroll)       │  │
│   │   • li.warning padding 10px #ffc107    │  │
│   └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

---

## 1. TextureInput 行与贴图编辑器（`src/components/Sidebar/TextureInput.vue`）

### 1.1 容器与工具栏

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `.texture_input` width / margin-top | `100%` / `8px` | `src/components/Sidebar/TextureInput.vue:599-603` | `position: relative` |
| `.toolbar` 高度 / margin-top | `34px` / `6px` | `src/components/Sidebar/TextureInput.vue:715-721` | flex，`align-items: center` |
| `.tool` 宽度 / padding / 圆角 | `40px` / `4px 4px` / `2px` | `src/components/Sidebar/TextureInput.vue:722-728` | `text-align: center` |
| `.tool.selected` 背景 / 文字色 | `#4b5b69`(`--color-title`) / `#f7f9ff`(`--color-highlight`) | `src/components/Sidebar/TextureInput.vue:729-732` | |
| `.tool:hover` 描边色 | `#f7f9ff`(`--color-highlight`) outline | `src/components/Sidebar/TextureInput.vue:733-735` | |
| 工具图标尺寸（Select/Brush/Eraser/Bucket/Pipette/Undo/Redo/X/Refresh/New/Save） | 24×24px（lucide 默认，未传 size） | `src/components/Sidebar/TextureInput.vue:5-26,83-93` | |
| `.undo_controls` | `margin: auto`（水平居中） | `src/components/Sidebar/TextureInput.vue:736-738` | |
| `.color_preview` 宽高 / 边框 / 圆角 | `34px × 34px` / `1px solid #1a1c1f`(`--color-border`) / `50%` | `src/components/Sidebar/TextureInput.vue:739-750` | 圆形取色按钮，棋盘格 base64 背景，`margin-left: auto` |
| `#color_picker_overlay` 定位 / z-index | `absolute; right: 0` / `4` | `src/components/Sidebar/TextureInput.vue:758-762` | |
| 取色器弹层宽度（vc-chrome） | `310px`，`margin: 2px 0` | `src/css/common.css:286-290` | body 背景 `#29323a`(`--color-interface`，common.css:291-293)；字段输入框高 `24px`、边框 `1px solid #1a1c1f`（common.css:294-300） |

### 1.2 贴图画布视口（texture_viewport）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 默认视口尺寸 `viewport_size` | `256`(px) | `src/components/Sidebar/TextureInput.vue:177` | data 初始值；`--size: 256px` 见 :612 |
| 视口高度 | `calc(var(--outer-height) + 2px)` | `src/components/Sidebar/TextureInput.vue:615` | 默认 258px |
| 视口外边距 | `margin-left: -12px; margin-right: -6px; margin-top: 8px` | `src/components/Sidebar/TextureInput.vue:616,621-623` | 顶破侧边栏 padding |
| 视口背景 | `#20272d`(`--color-dark`) | `src/components/Sidebar/TextureInput.vue:620` | |
| 画布 wrapper 宽度 / 边框 | `var(--size)` = 256·zoom px / `1px solid #1a1c1f`(`--color-border`) | `src/components/Sidebar/TextureInput.vue:628-631` | `box-sizing: content-box`，`cursor: crosshair`（select 工具时 default，:636-638） |
| 画布元素 | `width/height: 100%`，`background-size: contain` | `src/components/Sidebar/TextureInput.vue:789-795` | 非 scoped 样式 |
| 棋盘格单元 | `16px × 16px`（位置 0 0 / 0 8px / 8px -8px / -8px 0px），底色 `#29323a`(`--color-interface`)，偏移色 `#34404a`(`--color-bar`) | `src/css/common.css:223-230` | `.checkerboard` |
| 默认 zoom / 缩放范围 | `1`；clamp `[min(0.5, ratio), 8]`，1.0 附近有吸附 | `src/components/Sidebar/TextureInput.vue:176,327-330` | 滚轮步进 ×1.1 / ÷1.1（:311-317） |
| 视口拖拽高度范围 | clamp `64px` ~ `window.innerHeight - 120` | `src/components/Sidebar/TextureInput.vue:548` | resize_line 拖动 |
| 新建贴图默认尺寸 | `16 × 16` px | `src/components/Sidebar/TextureInput.vue:181`；`src/texture_edit.js:25,45-46,77-79` | 空画布初始 16×16 |
| 网格 | 无独立网格线层；像素网格 = canvas 像素化渲染（body `image-rendering: pixelated`） | `src/css/common.css:238` | 画笔轮廓 `#brush_outline` 充当 1 像素格指示 |

### 1.3 UV 预览框 / 画笔轮廓 / 视口滚动条

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `.uv_preview` 描边 | `outline: 1px solid #1a1c1f`(`--color-border`) | `src/components/Sidebar/TextureInput.vue:642-648` | `cursor: move` |
| UV sample 框边框 | `1px solid #20ddff`(`--color-accent`) | `src/components/Sidebar/TextureInput.vue:655-657` | |
| UV perimeter 框边框 | `1px solid #bcc3ca`(`--color-text`) | `src/components/Sidebar/TextureInput.vue:658-660` | |
| UV hover 描边（select 工具） | `#f7f9ff`(`--color-highlight`) | `src/components/Sidebar/TextureInput.vue:652-654` | |
| `.uv_preview_size_handle` 尺寸 / 偏移 | `10px × 10px`，`bottom/right: -10px` | `src/components/Sidebar/TextureInput.vue:661-672` | outline 1px `#1a1c1f`，背景 `#bcc3ca`，hover 视口时才显示（:673-675） |
| `#brush_outline` | `border: 1px solid #fff` + `outline: 1px solid #1a1c1f`，`mix-blend-mode: difference` | `src/components/Sidebar/TextureInput.vue:763-768` | 尺寸=单像素格 |
| `.viewport_scrollbar` 宽高 / 圆角 / 背景 | `12px × 12px` / `8px` / `#34404a`(`--color-bar`) | `src/components/Sidebar/TextureInput.vue:676-685` | border `1px solid #1a1c1f` |
| 竖屏（portrait）滚动条 | `16px × 16px` | `src/components/Sidebar/TextureInput.vue:686-689` | |
| 滚动条 hover/active | `#20ddff`(`--color-accent`) | `src/components/Sidebar/TextureInput.vue:690-692` | |
| 水平滚动条位置 / 长度限制 | `bottom: 4px`，`min-width: 25px; max-width: 100px` | `src/components/Sidebar/TextureInput.vue:693-699` | 宽度 `(30/zoom)%`（:61） |
| 垂直滚动条位置 / 长度限制 | `right: 3px`，`min-height: 25px; max-height: 100px` | `src/components/Sidebar/TextureInput.vue:700-706` | 高度 `(30/(zoom/ratio))%`（:62） |

### 1.4 信息栏 / 高度拖拽条 / meta 工具栏

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `.texture_info_bar` | flex；`.info` `flex: 1 1 80px`、`text-align: center`、`padding-top: 5px` | `src/components/Sidebar/TextureInput.vue:707-714` | 显示 宽x高 px / 光标 x y / 缩放 % |
| 帧切换/居中图标（ArrowBigLeft/Right、Maximize） | 20×20px（`:size="20"`） | `src/components/Sidebar/TextureInput.vue:69-78` | 其余 meta 图标默认 24px |
| `.resize_line` 高度 / 光标 | `12px` / `ns-resize` | `src/components/Sidebar/TextureInput.vue:769-773` | |
| resize_line 可视线 | `height: 2px; top: 5px; width: 100%`，色 `#34404a`(`--color-bar`)；hover `#6e8ebf4d`(`--color-selection`) | `src/components/Sidebar/TextureInput.vue:774-786` | |
| 文件选择输入 | `margin-top: 5px` | `src/components/Sidebar/TextureInput.vue:84` | `accept=".png"` |
| `#new_texture_dialog`（.modal_dialog） | padding `12px`，圆角 `4px`，border `1px solid #34404a`，背景 `#29323a`，阴影 `0 1px 12px rgba(0,0,0,0.4)` | `src/css/common.css:261-269` | button_bar：flex，mt `10px`，gap `4px`，右对齐（:270-275）；form_bar：flex 两端对齐，gap `8px`，mt `2px`（:276-282） |

---

## 2. QuickSetup 面板（`src/components/Sidebar/QuickSetup.vue`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 分组 `.input_group` 分隔 | 非末组 `border-bottom: 1px solid #1a1c1f`(`--color-border`) | `src/components/Sidebar.vue:270-272` | 全局样式 |
| 组标题 `h4` padding / 字号 | `10px`（左 `12px`） / `1.2em`（≈13.2pt） | `src/components/Sidebar.vue:273-279` | hover `filter: brightness(1.1)`（:280-282） |
| `.input_group > ul` padding | `8px`（右 `2px`） | `src/components/Sidebar.vue:286-289` | |
| 预设列表 `.preset_option_list` | flex，`gap: 2px`，`overflow-x: auto`（横向滚动条） | `src/components/Sidebar/QuickSetup.vue:516-520` | |
| 预设卡片 `li` 宽度 / padding | `80px` / `8px` | `src/components/Sidebar/QuickSetup.vue:521-526` | `text-align: center` |
| 卡片 hover / selected | hover 文字 `#f7f9ff`(`--color-highlight`)；selected 背景 `#34404a`(`--color-bar`) | `src/components/Sidebar/QuickSetup.vue:527-532` | |
| 预设图标（lucide）尺寸 / 线宽 | `38×38px` / `stroke-width: 1` | `src/components/Sidebar/QuickSetup.vue:7,11,15,19,34,38,58,62,66,70,74` | `svg { display:block; margin:auto }`（:533-536） |
| Sprite 缩略图 `<img>` | `height="45"`，`aspect-ratio: 1/1`，`object-fit: cover`，`object-position: bottom` | `src/components/Sidebar/QuickSetup.vue:90-116,537-542` | 45×45px 裁切显示 |
| 动图缩略图帧动画 | 8 帧 `1s step-end infinite`（`thumb_animation`）；4 帧 `300ms`（`thumb_animation_4`） | `src/components/Sidebar/QuickSetup.vue:543-594` | 仅 hover 时播放 |
| Sprite 标题 CC 图标 | `20×20px`（`:size="20"`） | `src/components/Sidebar/QuickSetup.vue:83` | 点击切换许可说明 |
| 许可说明段落 padding | `2px 12px` | `src/components/Sidebar/QuickSetup.vue:85` | 内联样式 |
| `.input_bar` 布局 | flex，`align-items: center`，`margin: 10px 14px`，`gap: 8px` | `src/components/Sidebar/QuickSetup.vue:595-600` | |
| 滑杆数值标签 `.range_number_label` | 宽 `46px`，居中 | `src/components/Sidebar/QuickSetup.vue:609-613` | |
| range 滑杆 | 高 `30px`；thumb `20×20px` 圆形、填充 `#29323a`(`--color-center`=`--color-interface`)、描边 `2px solid #20ddff`(`--color-thumb`=`--color-accent`)、`margin-top: -9px`；轨道高 `4px`（webkit，moz 为 2px+radius 3px），色 `#34404a`(`--color-bar`) | `src/css/common.css:99-166` | hover thumb 实心 `#20ddff`（:140-147） |
| range 范围参数 | Speed 0–20 step 0.5；Amount 1–120 step 1；Lifetime 0.1–10 step 0.1 | `src/components/Sidebar/QuickSetup.vue:25,44,49` | |

---

## 3. CodeViewer（`src/components/CodeViewer.vue`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 容器 `main#code` | flex column | `src/components/CodeViewer.vue:55-58` | 无行号功能（模板仅 `<prism>`，:6） |
| `.menu` 高度 / padding | `flex: 0 0 42px` / `5px` | `src/components/CodeViewer.vue:76-79` | |
| Copy 按钮宽度 / 图标 | `100px` / Copy 图标 20px、`margin-right: 4px` | `src/components/CodeViewer.vue:4,80-85` | 按钮全局样式：padding `8px 12px`、圆角 `1px`、背景 `#34404a`(`--color-bar`)、hover 背景 `#20ddff`(`--color-accent`) + 文字 black（`src/css/common.css:179-192`） |
| `pre` 背景 / 宽 / 最大宽 | `#20272d`(`--color-dark`) / `100%` / `1000px` 居中 | `src/components/CodeViewer.vue:59-71` | `margin: 0 auto`、`overflow-y: auto`、`border: none`、`border-radius: 0` |
| 代码字体 | `--font-code`（Consolas, Monaco, …monospace） | `src/css/common.css:46`；prism 主题继承 | 渲染差异豁免，仅记录 |
| 代码文字色 / 选中 | `#bcc3ca`(`--color-text`)；`user-select: text`、`text-shadow: none` | `src/css/common.css:335-337`；`src/components/CodeViewer.vue:72-75` | |
| 语法高亮：property/tag/constant/symbol/deleted | `#7bcbf0` | `src/components/CodeViewer.vue:95-97` | 覆盖 okaidia |
| 语法高亮：boolean/number | `#ff6868` | `src/components/CodeViewer.vue:98-100` | 覆盖 okaidia |
| 语法高亮：operator/entity/url | `background: transparent` | `src/components/CodeViewer.vue:92-94` | 去掉 okaidia 底色 |
| 其余 token 色（comment `#8292a2`、punctuation `#f8f8f2`、string `#e6db74`、keyword/operator `#f92672`、function `#a6e22e` 等） | prism-okaidia.css 主题 | `src/components/CodeViewer.vue:12` | node_modules 未随源码克隆，无法给出 file:line；数值为该公开主题标准值 [INFERENCE]，仅对未被上述规则覆盖的 token 生效 |
| 相关共享样式：prism 编辑器 wrapper（用于表达式输入，非本视图） | 背景 `#20272d`、padding `4px`（底 `2px`）、字号 `15px`、`min-width: 100%` | `src/css/common.css:312-324` | 其 token 色：punctuation `#5ba8c5`、operator/keyword `#fc2f40`、number/boolean `#b99cff`、function-name `#94e400`、selector `#92dcff`（:360-374）——与 CodeViewer 不是同一套 |

---

## 4. WarningDialog（`src/components/WarningDialog.vue` + `src/components/App.vue`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 遮罩 `#dialog_blackout` | 全屏 absolute，`z-index: 49`，背景 `#00000050`（rgba(0,0,0,0.31)） | `src/components/App.vue:288-296` | 点击关闭 |
| `dialog` 宽度 / 定位 | `800px`，`max-width: 100%`，`margin-left: calc(50% - 400px)` | `src/components/App.vue:297-301` | 水平居中 |
| 垂直位置 / 最大高 | `top: 20px; bottom: 20px`，`max-height: calc(100% - 40px)` | `src/components/App.vue:302-308` | |
| padding / z-index | `20px 28px` / `50` | `src/components/App.vue:305,307` | flex column，`overflow: hidden` |
| 边框 / 圆角 / 阴影 / 背景 | `1px solid #34404a`(`--color-bar`) / `4px` / `0 1px 12px rgba(0,0,0,0.4)` / `#29323a`(`--color-interface`) | `src/components/App.vue:304,313-315` | |
| 竖屏 dialog | `margin: auto; padding: 12px 12px` | `src/components/App.vue:283-286` | portrait_view 覆盖 |
| 关闭按钮 `.close_button` | `position: absolute; height: 30px; top: 6px; right: 6px` | `src/components/App.vue:333-338` | 全局 dialog 样式；内部 `.unicode_icon`（✕ U+2A09）`24×24px`、字号 `14pt`（`src/css/common.css:196-204`；`src/components/WarningDialog.vue:3`） |
| 标题 `h2` | 继承全局：margin 0、行高 1.2、weight 继承 | `src/css/common.css:50-54` | 「Warnings」 |
| 列表容器 `.scrollable` | `overflow-y: scroll`（无固定高，受 dialog flex 约束） | `src/components/WarningDialog.vue:63-65` | |
| 警告条目 `li.warning` | `list-style: inside; padding: 10px; color: #ffc107` | `src/components/WarningDialog.vue:66-70` | 琥珀色警示文字 |
| 正文按钮样式（对话框通用 button） | padding `8px 12px`、圆角 `1px`、背景 `#34404a`、hover `#20ddff` + 黑字 | `src/css/common.css:179-192` | WarningDialog 本身只有关闭按钮 |

---

## 附：本区域引用的共享滚动条指标

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `::-webkit-scrollbar` | `8px × 8px` | `src/css/common.css:4-7` | Firefox `scrollbar-width: thin`（:28-31） |
| 滑块 / hover | `#34404a`(`--color-bar`) / `#4b5b69`(`--color-title`) | `src/css/common.css:16-21` | 轨道 `#29323a`(`--color-interface`，:9-14) |
