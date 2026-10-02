# Metrics 01 — App 骨架 / 顶栏 / 表达式栏 / Logo

> 基准：Snowstorm v3.2.2 克隆源码 `build/_snowstorm_src/`（下文所有 file:line 均相对该目录）。
> 注意：源码中 CSS 变量**不是**定义在 `:root`，而是定义在 `body` 选择器上（`src/css/common.css:33-47`），语义等同全局变量。
> 字体渲染差异豁免；字号/行高数值照常记录。

## 区域结构 ASCII 示意图

```
横向（默认，body.clientWidth >= 720 或 <= 100）:
┌──────────────┬───────────────────────────────────────────────────────────┐
│              │ header (行高 74px, grid-area: header, position: relative) │
│              │ ┌───────────────────────────────────────────────────────┐ │
│   sidebar    │ │ #menu_bar 高32px  [菜单a...]   [Help][Share][Code|Pre]│ │
│  (grid-area: │ ├───────────────────────────────────────────────────────┤ │
│   sidebar,   │ │ #expression_bar (absolute, min-height 20px,           │ │
│   宽=        │ │   padding 5px 8px, 底边框1px)  => 74-32-1-10 ≈ 31px   │ │
│   --sidebar) │ └───────────────────────────────────────────────────────┘ │
│   ▲          ├───────────────────────────────────────────────────────────┤
│   │ 6px      │                                                           │
│ resizer      │ main / preview (grid-area: preview)                       │
│ (margin-left │                                                           │
│  -3px, 按钮  │                                                           │
│  top:120px)  │                                                           │
└──────────────┴───────────────────────────────────────────────────────────┘
grid: rows 74px / calc(100%-74px); cols var(--sidebar) / calc(100%-var(--sidebar))
areas: "sidebar header" / "sidebar preview"

纵向 portrait（100px < body.clientWidth < 720px）:
┌───────────────────────────────┐
│ header (124px): Logo svg 40px │
│   + menu_bar 32px + expr bar  │
├───────────────────────────────┤
│ main (calc(100% - 162px))     │
├───────────────────────────────┤
│ #portrait_mode_selector 38px  │
│  [Config][Code][Help][Preview]│ 每格 flex:1, 顶边框1px
└───────────────────────────────┘
grid: rows 124px / calc(100%-162px) / 38px; cols 100%
```

## 1. CSS 变量全量解析（common.css `body` 块）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| --color-background | #29323a | src/css/common.css:34 | body 背景 |
| --color-dark | #20272d | src/css/common.css:35 | 输入框/表达式栏底 |
| --color-border | #1a1c1f | src/css/common.css:36 | 边框色 |
| --color-interface | #29323a | src/css/common.css:37 | 面板底色（与 background 同值） |
| --color-bar | #34404a | src/css/common.css:38 | 菜单悬停/滚动条 |
| --color-title | #4b5b69 | src/css/common.css:39 | 标题灰蓝 |
| --color-selection | rgba(110,142,191,0.3) | src/css/common.css:40 | 选区 |
| --color-highlight | #f7f9ff | src/css/common.css:41 | 悬停高亮字色 |
| --color-text | #bcc3ca | src/css/common.css:42 | 正文 |
| --color-text_grayed | #939aa3 | src/css/common.css:43 | 灰化文字 |
| --color-accent | #20ddff | src/css/common.css:44 | 按钮 hover 底 |
| --font-code | Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace | src/css/common.css:46 | 代码字体栈 |
| body font-family | 'Lato', -apple-system, "Segoe UI", sans-serif | src/css/common.css:242 | UI 字体 |
| body font-size | 11pt | src/css/common.css:246 | 基准字号 |
| body font-weight | 400 | src/css/common.css:247 | |
| body line-height | 1.5 | src/css/common.css:248 | |
| body color | var(--color-text) → #bcc3ca | src/css/common.css:249 | |
| 全局 box-sizing | border-box | src/css/common.css:63,67 | `*` 与 `*,:after,:before` 双声明 |
| 全局 margin/padding | 0 / 0 | src/css/common.css:61-62 | `*` 重置 |
| 全局 user-select | none | src/css/common.css:64,240 | |
| 滚动条 | thin；thumb=--color-bar(#34404a)，hover=--color-title(#4b5b69)，track=--color-interface(#29323a) | src/css/common.css:4-21,29-30 | webkit+firefox |

## 2. App 整体 Grid（横向/默认）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| #app display | grid | src/components/App.vue:231 | |
| grid-template-rows | 74px calc(100% - 74px) | App.vue:232 | header 固定 74px |
| grid-template-columns | var(--sidebar) calc(100% - var(--sidebar)) | App.vue:233 | --sidebar 由内联 style 注入 |
| grid-template-areas | "sidebar header" / "sidebar preview" | App.vue:234 | sidebar 占两行 |
| #app 尺寸/定位 | height 100%; width 100%; position: fixed | App.vue:235-237 | |
| #app font-weight | 400 | App.vue:238 | |
| --sidebar 注入 | `:style="{'--sidebar': getEffectiveSidebarWidth()+'px'}"` | App.vue:2 | 内联 CSS 变量 |
| main grid-area | preview | App.vue:241 | |
| header grid-area / font-size / position | header / 1.1em / relative | App.vue:243-246 | 1.1em ≈ 16.13px（基准 11pt≈14.67px） |
| content grid-area / 背景 | sidebar / var(--color-interface)=#29323a | App.vue:248-251 | sidebar 容器 |
| #dialog_blackout | position:absolute, inset 0, z-index 49, 背景 #00000050 | App.vue:288-296 | 模态遮罩 |

### 2.1 sidebar 宽度计算逻辑（JS）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| portrait 判定 | 100px < body.clientWidth < 720px | App.vue:74 | `portrait_view` |
| VSCode 兜底 | body_width < 100 时按 1280 计 | App.vue:79 | |
| portrait 初始宽 | = body_width（全宽） | App.vue:81-82 | |
| 有 localStorage `snowstorm_sidebar_width` | clamp(parseInt(存储值), 100, body_width-200) | App.vue:83-84 | 单位 px |
| 无存储默认 | clamp(body_width/2, 160, clamp(180 + body_width*0.2, 160, 660)) | App.vue:85-87 | 即 min(半屏, max(160, 180+20%屏宽, ≤660)) |
| 拖拽实时宽 | clamp(size, 240, body_width-200)；size ≤ 80 视为收起手势 | App.vue:134-142 | 收起时记住原宽 |
| 有效宽 = 开关×宽度 | `is_sidebar_open * sidebar_width` | App.vue:162-164 | 关闭时 --sidebar = 0px |
| 持久化 | localStorage `snowstorm_sidebar_width` / `snowstorm_is_sidebar_open` | App.vue:145-146,171 | |
| 初始开关 | localStorage 有值按其值，否则 true | App.vue:90-96 | |
| 默认 tab | portrait→'config'，否则 'preview' | App.vue:106 | |

### 2.2 Resizer（分隔条）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| .resizer 定位 | absolute, top:0, bottom:0, width 6px, margin-left -3px | App.vue:319-326 | 中心对齐 sidebar 右缘 |
| .resizer z-index | 12 | App.vue:199-201 | 非 scoped 块 |
| .resizer left | = getEffectiveSidebarWidth() px（内联） | App.vue:29 | |
| 光标 | ew-resize；sidebar 关闭时 default | App.vue:29,325 | |
| 方向变体 | vertical 宽6px/ew-resize；horizontal 高6px/ns-resize | App.vue:202-209 | |
| .resizer.disabled | pointer-events: none | App.vue:210-212 | |
| .resizer_toggle_button | absolute, left 50%, top 120px, translateX(-50%), 30px×30px, flex 居中, padding 0, border-radius 10% | App.vue:213-225 | 仅 sidebar 关闭时显示（v-show） |
| 按钮内图标 | PanelLeftOpen（lucide，默认 24px） | App.vue:31-32,61 | lucide 默认 size=24 |

### 2.3 Portrait 模式 grid

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| grid-template-rows | 124px calc(100% - 162px) 38px | App.vue:255 | 124+38=162 |
| grid-template-columns | 100% | App.vue:256 | |
| grid-template-areas | "header" "main" "mode_selector" | App.vue:257 | |
| header > svg（portrait logo） | height 40px, padding 12px, width 100% | App.vue:259-263 | |
| #portrait_mode_selector | grid-area mode_selector, display flex | App.vue:270-273 | |
| li.mode_selector（portrait） | flex: 1 0 0, text-align center, padding-top 6px, border-top 1px solid var(--color-border)=#1a1c1f | App.vue:274-279 | 图标 lucide size=22（App.vue:39-42） |
| li.mode_selector.selected（portrait） | background var(--color-dark)=#20272d | App.vue:280-282 | |
| portrait dialog | margin auto, padding 12px | App.vue:283-286 | |

## 3. Header 内部（74px = menu_bar 32px + expression_bar 余量）

### 3.1 MenuBar（#menu_bar）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| ul#menu_bar height | 32px | src/components/MenuBar.vue:166 | |
| font-weight | normal | MenuBar.vue:167 | |
| padding | 0 8px | MenuBar.vue:168 | |
| 背景 | var(--color-interface) = #29323a | MenuBar.vue:169 | |
| white-space | nowrap | MenuBar.vue:170 | |
| 顶层 li | display inline-block | MenuBar.vue:181-183 | |
| 菜单项 a | display block, padding 2px 12px（padding-top 3px）, color inherit | MenuBar.vue:172-177 | 实际行高约 32px |
| a:hover 背景 | var(--color-interface) = #29323a | MenuBar.vue:178-180 | 与底同色（视觉无变化） |
| li:hover > a 背景 | var(--color-bar) = #34404a | MenuBar.vue:200-202 | 展开下拉时父项高亮 |
| 下拉 ul.menu_dropdown | display none→hover block, position absolute, padding 0, z-index 8, min-width 150px, 背景 var(--color-bar)=#34404a, box-shadow 1px 4px 10px rgba(0,0,0,0.25) | MenuBar.vue:184-195 | |
| 下拉项 a | height 34px, padding-top 5px | MenuBar.vue:196-199 | |

### 3.2 右侧 Preview/Code 切换与图标按钮（.mode_selector）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| .mode_selector | float right, height 100%（=32px）, padding 2px 8px（padding-top 3px）, cursor pointer, margin-right 2px | MenuBar.vue:203-210 | 含 Code/Preview 文字按钮与 Help/Share 图标按钮 |
| .mode_selector:hover 背景 | var(--color-interface) = #29323a | MenuBar.vue:211-213 | |
| .mode_selector.selected | 背景 var(--color-dark)=#20272d，字色 var(--color-text_grayed)=#939aa3 | MenuBar.vue:214-217 | 当前 tab 指示 |
| Code/Preview 按钮 | `<li class="mode_selector code/preview">`，文字标签 | MenuBar.vue:17-18 | 仅非 portrait 且非 VSCode 扩展 |
| Help 按钮 | div.mode_selector.highlighting_button，HelpCircle size=20 | MenuBar.vue:21-23 | 文档面板开关，选中态同 .selected |
| Share 按钮 | div.mode_selector.highlighting_button，Share2 size=20，仅 canShare | MenuBar.vue:24-26 | |
| .highlighting_button:hover | color var(--color-highlight)=#f7f9ff | src/css/common.css:256-258 | |
| VSCode 扩展模式按钮 | "Switch to Code" 文字 + split 图标（⎅, .unicode_icon.split 22pt, margin-top -12px） | MenuBar.vue:12-15; common.css:216-219 | 仅 isVSCExtension |
| .unicode_icon 基础 | display block, 24px×24px, text-align center, font-size 14pt, font-family sans-serif | common.css:196-204 | |

## 4. ExpressionBar（表达式栏）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| #expression_bar 宽度 | 100% | src/components/ExpressionBar.vue:94 | |
| min-height | 20px | ExpressionBar.vue:95 | 内容增高时 height:auto（:99） |
| 背景 | var(--color-dark) = #20272d | ExpressionBar.vue:96 | |
| 底边框 | 1px solid var(--color-border) = #1a1c1f | ExpressionBar.vue:97 | |
| 定位 | position absolute, z-index 3 | ExpressionBar.vue:98,100 | 叠在 header 下部 |
| 字体 | var(--font-code)（Consolas…monospace） | ExpressionBar.vue:101 | |
| outline | none | ExpressionBar.vue:102 | |
| padding | 5px 8px | ExpressionBar.vue:103 | |
| 内含编辑器 | vue-prism-editor，无行号 | ExpressionBar.vue:3-10 | 右侧**无按钮**（此版本模板只有编辑器） |
| .prism-editor-component border | none（栏内覆盖；全局默认 1px solid var(--color-border)） | ExpressionBar.vue:105-107; common.css:328-331 | |
| .prism-editor-wrapper | 背景 var(--color-dark)=#20272d, padding 4px（bottom 2px）, font-size 15px, z-index 3, width fit-content, min-width 100% | common.css:312-324 | 编辑器实际文本字号 15px |
| 编辑器 textarea outline | none | common.css:332-334 | |
| 自动完成弹层 | 背景 var(--color-interface)=#29323a, 边框 1px var(--color-border), max-height 220px, padding-bottom 10px；li padding 3px 8px, font var(--font-code)；hover/selected: 字 #f7f9ff 底 #34404a | common.css:341-358 | |
| 语法高亮色 | punctuation #5ba8c5；operator/keyword #fc2f40；number/boolean #b99cff；function-name #94e400；selector #92dcff | common.css:360-374 | |
| 实际高度推算 | 20(min 内容) + 10(padding) + 1(border) ≥ 31px；74 − 32 = 42px 可用，超出部分为空隙 | App.vue:232 + 上表 | [INFERENCE] 由 grid 行高与 menu_bar 高度推得 |

## 5. Logo 与版本号（Sidebar/Logo.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| svg 固有尺寸 | width 54.0mm, height 8.9683867mm | src/components/Sidebar/Logo.vue:11-12 | ≈ 204.1px × 33.9px @96dpi [INFERENCE] |
| viewBox | -0.1 0 90.866723 10.762064 | Logo.vue:13 | |
| svg 渲染样式 | height auto, padding 12px, fill var(--color-text)=#bcc3ca, overflow hidden | Logo.vue:77-82 | 宽度随 sidebar 容器 |
| 版本号 span | float right, margin-right 12px, margin-top 21px, color var(--color-title)=#4b5b69 | Logo.vue:83-88 | 内容 = 全局 VERSION（Logo.vue:70） |
| portrait 下 logo 位置 | header 顶部（`<logo v-if="portrait_view"/>`），svg 高 40px padding 12px width 100% | App.vue:8, 259-263 | 横向下 logo 在 sidebar 内部 |

## 6. 窗口缩放 / resize 行为

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 布局自适应 | #app position fixed + 100% 尺寸，grid 第二行/列 calc(100%−…) | App.vue:232-237 | 窗口缩放自动重排 |
| portrait 切换时机 | 仅启动时读取 body.clientWidth（无 resize 监听） | App.vue:74,112 | 运行中改窗宽不切换横竖版式 [INFERENCE：未见 resize listener] |
| sidebar 拖拽 | mousedown→document mousemove/mouseup，宽 = 原宽 + dx | App.vue:148-161 | 松手即写 localStorage |
| 拖拽上/下限 | min 240px, max body_width−200px；≤80px 触发收起 | App.vue:135-142 | |
| iOS 输入缩放修复 | iPhone 时 viewport 加 maximum-scale=1 | App.vue:68-71 | 非 vscode 环境 |
| Preview 尺寸同步 | setTab/toggleSidebar/setSidebarSize 后调 preview.updateSize() | App.vue:118-120,143-144,167-170 | |

## 7. 通用控件（与顶栏相关的全局样式）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| button | 背景 var(--color-bar)=#34404a, padding 8px 12px, border-radius 1px, border none | common.css:179-188 | resizer_toggle_button 继承 |
| button:hover | 背景 var(--color-accent)=#20ddff, 字色 black | common.css:189-192 | |
| input/select | 高 30px, 背景 var(--color-dark), 边框 1px var(--color-border), padding 4px 1px, outline none | common.css:78-86 | |
| input 文本类 | font var(--font-code), padding 0 4px | common.css:87-90 | |
| ::selection | 背景 var(--color-selection)=rgba(110,142,191,0.3) | common.css:22-24 | |
| ::placeholder | opacity 0.6 | common.css:25-27 | |
