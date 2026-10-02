# Snowstorm UI 像素指标 — 02 Sidebar（侧栏与输入区）

> 基准：Snowstorm v3.2.2 克隆源码 `build/_snowstorm_src/`（下文所有 file:line 均相对该目录）。
> CSS 变量统一解析为 hex；字体渲染差异豁免，但字号/行高数值仍记录。

## 区域结构 ASCII 示意图

```
#app (grid: 列 = var(--sidebar) | 剩余)
┌────────────────────────────┬───────────────┐
│ content#sidebar            │ header(74px)  │  sidebar 宽 = --sidebar (默认
│ ┌────────────────────────┐ │               │  clamp(w/2,160,clamp(180+w*0.2,
│ │ Logo (svg +12px pad)   │ │               │  160,660))，拖拽 240..w-200)
│ ├────────────────────────┤ │               │
│ │ #sidebar_tab_bar h=45  │ │  (sticky,     │
│ │ [ico][ico]...[ico] ×9  │ │   z-index 6)  │
│ ├────────────────────────┤ │               │
│ │ h3 SUBJECT 标题         │ │               │
│ ├────────────────────────┤ ├───────────────┤
│ │ .input_group           │ │               │
│ │ ┌──────────────────────┤ │   preview     │
│ │ │ h4 组头 [label]  [?] │ │               │
│ │ ├──────────────────────┤ │               │
│ │ │ ul (pad 8/2)         │ │               │
│ │ │ ┌────────────────────┤ │               │
│ │ │ │ li.input_wrapper   │ │               │
│ │ │ │ [label 100px][控件]│ │  行: label右对齐 │
│ │ │ │ [label     ][控件] │ │  控件高30px    │
│ │ │ └────────────────────┤ │               │
│ │ ├──────────────────────┤ │               │
│ │ │ h4 组头 …            │ │  (border-bottom│
│ │ └──────────────────────┤ │   1px 分隔组)  │
│ │ [Quick Setup 按钮]     │ │               │
│ └────────────────────────┘ │               │
│  (滚动区 padding-bottom 60)│               │
└────────────────────────────┴───────────────┘
  HelpPanel: 右侧浮层 宽482 top32 bottom33 z5
  InfoBox:   顶部居中 宽500 top20 z50
```

---

## 0. 全局 CSS 变量（`src/css/common.css`，body 上定义）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| --color-background | `#29323a` | src/css/common.css:34 | 页面底色 |
| --color-dark | `#20272d` | src/css/common.css:35 | input 底、checkerboard 偏移色 |
| --color-border | `#1a1c1f` | src/css/common.css:36 | 边框 |
| --color-interface | `#29323a` | src/css/common.css:37 | 面板底（sidebar/help 等） |
| --color-bar | `#34404a` | src/css/common.css:38 | tab 栏底、滚动条 thumb |
| --color-title | `#4b5b69` | src/css/common.css:39 | 选中 tab 底、thumb hover |
| --color-selection | `rgba(110, 142, 191, 0.3)` | src/css/common.css:40 | 选区 |
| --color-highlight | `#f7f9ff` | src/css/common.css:41 | hover 文字色 |
| --color-text | `#bcc3ca` | src/css/common.css:42 | 正文 |
| --color-text_grayed | `#939aa3` | src/css/common.css:43 | 弱化文字 |
| --color-accent | `#20ddff` | src/css/common.css:44 | 强调（range thumb、checkbox 勾、button hover） |
| --font-code | `Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace` | src/css/common.css:46 | 代码/input 字体 |
| body 字号 | `11pt`，weight 400，line-height 1.5 | src/css/common.css:246-248 | 全局基准（em 换算基准） |
| body 字体 | `'Lato', -apple-system, "Segoe UI", sans-serif` | src/css/common.css:242 | 渲染豁免但记录 |
| 全局盒模型 | `box-sizing: border-box`（含 :after/:before） | src/css/common.css:63,67 | 所有尺寸为 border-box |
| 全局 margin/padding 清零 + user-select:none | `*` | src/css/common.css:60-65 | |

## 0.1 Sidebar 整体容器

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| sidebar 列宽 | `var(--sidebar)`（内联 `--sidebar: Npx`） | src/components/App.vue:2,233 | grid 布局 `"sidebar header"/"sidebar preview"` |
| 初始宽度算法 | `clamp(body_w/2, 160, clamp(180+body_w*0.2, 160, 660))` | src/components/App.vue:85-86 | 1280px 窗口 → 436px |
| 拖拽范围 | `clamp(size, 240, body_w-200)` | src/components/App.vue:136 | localStorage `snowstorm_sidebar_width` 持久化（App.vue:145） |
| content#sidebar 背景 | `--color-interface` = `#29323a` | src/components/App.vue:248-250 | grid-area: sidebar |
| 右边框 | `border-right: 1px solid var(--color-border)` = `#1a1c1f` | src/components/Sidebar.vue:156-158 | |
| 内滚动容器 | `overflow-y: auto; height: 100%; padding-bottom: 60px` | src/components/Sidebar.vue:159-163 | `#sidebar > div`（#sidebar_content） |
| Logo svg | `height: auto; padding: 12px; fill: var(--color-text)`=`#bcc3ca` | src/components/Sidebar/Logo.vue:75-80 | viewBox `90.866723×10.762064`（Logo.vue:14），按容器宽拉伸 |
| Logo 版本号 span | `float:right; margin-right:12px; margin-top:21px; color: var(--color-title)`=`#4b5b69` | src/components/Sidebar/Logo.vue:81-86 | |

## 1. Tab 栏（`#sidebar_tab_bar`，9 个 tab）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 栏高度 | `45px` | src/components/Sidebar.vue:165 | |
| 布局 | `display: flex` | src/components/Sidebar.vue:166 | 9 个 `.sidebar_tab` |
| 背景 | `--color-bar` = `#34404a` | src/components/Sidebar.vue:167 | |
| 定位 | `position: sticky; top: 0; z-index: 6` | src/components/Sidebar.vue:168-170 | |
| 阴影 | `box-shadow: 0 1px 12px rgba(0,0,0,0.34)` | src/components/Sidebar.vue:171 | |
| portrait 模式 | 绝对定位 `bottom: 36px; left/right:0`，无阴影，`border-bottom: 1px solid #1a1c1f` | src/components/Sidebar.vue:173-181 | |
| tab 宽度分配 | `flex: 1 0.5 45px`（等分，基准 45px） | src/components/Sidebar.vue:183 | 436px 侧栏 ≈ 48.4px/tab |
| tab padding | `10px 4px` | src/components/Sidebar.vue:186 | 内容区 45-20=25px 高 |
| tab 对齐 | `text-align: center; cursor: pointer` | src/components/Sidebar.vue:184-185 | |
| tab 图标 | lucide-vue 组件（Wand/File/PartyPopper/Sparkles/Image/Feather/Clock8/Tangent/Zap），未传 size → 默认 `24×24px` | src/components/Sidebar.vue:11-19,72-84 | lucide-vue 默认值（库约定） |
| tab 图标对齐 | svg `vertical-align: middle` | src/css/common.css:69-71 | |
| 选中态 | 背景 `--color-title` = `#4b5b69`；文字 `--color-highlight` = `#f7f9ff` | src/components/Sidebar.vue:190-193 | `.sidebar_tab.selected` |
| hover 态 | `color: var(--color-highlight)` = `#f7f9ff` | src/components/Sidebar.vue:194-196 | |
| tooltip | 高 `28px`，`min-width:100%`，padding `2px 8px`，`bottom:-28px`，背景继承 tab 底 `#34404a`，默认 `display:none` | src/components/Sidebar.vue:197-207 | hover 时 block（:211-213） |
| tooltip（最后 tab） | `right:0; left:unset` | src/components/Sidebar.vue:214-217 | 防溢出 |
| tooltip（portrait） | `top:-28px` | src/components/Sidebar.vue:208-210 | |
| tab 顺序/图标映射 | setup=Wand, effect=File, emitter=PartyPopper, appearance=Sparkles, texture=Image, motion=Feather, lifetime=Clock8, variables=Tangent, events=Zap | src/components/Sidebar.vue:11-19 | |

## 2. Subject 标题（`<h3>`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 字号 | `1.1em`（11pt 基准 → ≈16.1px） | src/components/Sidebar.vue:234 | |
| 外边距 | `margin: 8px; margin-bottom: -2px` | src/components/Sidebar.vue:232-233 | |
| 对齐 | `text-align: center; text-transform: uppercase` | src/components/Sidebar.vue:235-236 | |
| 左 padding | `12px` | src/components/Sidebar.vue:237 | |
| 颜色 | `--color-text_grayed` = `#939aa3` | src/components/Sidebar.vue:238 | |
| cursor | `pointer` | src/components/Sidebar.vue:239 | |
| 行高/字重 | `line-height: 1.2; font-weight: inherit(400)` | src/css/common.css:50-54 | h1-h6 通用 |

## 3. Input Group 折叠区（`.input_group`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 组间分隔 | 非末组 `border-bottom: 1px solid var(--color-border)` = `#1a1c1f` | src/components/Sidebar.vue:270-272 | 全局样式块 |
| 组头 h4 padding | `10px`，左 `12px` | src/components/Sidebar.vue:273-276 | |
| 组头字号 | `1.2em`（≈17.6px） | src/components/Sidebar.vue:275 | |
| 组头颜色 | `--color-text_grayed` = `#939aa3` | src/components/Sidebar.vue:277 | |
| 组头 cursor | `pointer` | src/components/Sidebar.vue:278 | 点击整行折叠 |
| 组头 hover | `filter: brightness(1.1)` | src/components/Sidebar.vue:280-282 | |
| 折叠箭头 | 无独立箭头字符；折叠态显示 `.input_group_folded_indicator`（"..."） | src/components/Sidebar.vue:45 | 展开/折叠由 h4 与 indicator 触发 |
| 折叠指示符 | 高 `32px`，居中，字号 `30px`，`line-height: 6px`，色 `#939aa3` | src/components/Sidebar.vue:290-297 | |
| 折叠指示符 hover | `color: var(--color-text)` = `#bcc3ca` | src/components/Sidebar.vue:298-300 | |
| 帮助按钮 `.help_button`（"?"） | `float: right`；宽 `30px`，高 `32px`，`margin: -5px -9px`，`padding-top: 4px`，居中，字体 arial | src/components/Sidebar.vue:283-285（float）、218-225（尺寸） | class 含 `highlighting_button` |
| 帮助按钮 hover | `color: var(--color-highlight)` = `#f7f9ff` | src/css/common.css:256-258 | `.highlighting_button:hover` |
| 组内容 ul padding | `8px`，右 `2px` | src/components/Sidebar.vue:286-289 | `.input_group > ul` |
| 曲线组 ul | 同上（结构一致） | src/components/Sidebar.vue:31-39 | |

## 4. 通用控件基线（`src/css/common.css`，所有 input 行共享）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| input/select 高 | `30px` | src/css/common.css:83 | |
| input/select 背景 | `--color-dark` = `#20272d` | src/css/common.css:80 | |
| input/select 边框 | `1px solid var(--color-border)` = `#1a1c1f` | src/css/common.css:81 | |
| input/select 文字色 | `--color-text` = `#bcc3ca` | src/css/common.css:82 | |
| input/select padding | `4px 1px`；text/number 另 `padding: 0 4px` | src/css/common.css:84,89 | |
| text/number 字体 | `--font-code`（Consolas…） | src/css/common.css:88 | |
| input 字号/字重/行高 | inherit（11pt/400/1.5） | src/css/common.css:72-77 | |
| outline | `none`（input/select） | src/css/common.css:85 | 无 focus 高亮样式（未定义 focus 规则） |
| disabled | 无通用 disabled 样式；仅 range thumb disabled 背景 `--color-button`（未定义变量，回退无效）[INFERENCE] | src/css/common.css:148-153 | |
| select 高/外观 | `30px`，`-webkit-appearance:none; appearance:auto; border-radius:0` | src/css/common.css:231-236 | |
| number 文字色 | `#b99cff` | src/css/common.css:366-368 | prism token 色联动 |
| button 基线 | 背景 `--color-bar`=`#34404a`，padding `8px 12px`，`border-radius:1px`，无边框 | src/css/common.css:179-188 | |
| button hover | 背景 `--color-accent`=`#20ddff`，文字 `black` | src/css/common.css:189-192 | |
| `.tool` 基线 | inline-block，`padding:2px 8px / top 1px`，宽 `35px`，`height:100%; max-height:32px` | src/components/App.vue:183-190 | 全局（App.vue 非 scoped） |
| `.tool:hover` | `color: var(--color-highlight)` = `#f7f9ff` | src/components/App.vue:192-194 | |
| label | `display: inline-block` | src/css/common.css:193-195 | |
| `.unicode_icon` | `24×24px`，居中，字号 `14pt`，sans-serif | src/css/common.css:196-204 | ✕/＋ 等字符图标 |
| `.unicode_icon.plus` | 字号 `18pt`，`margin-top:-4px` | src/css/common.css:212-215 | |

## 5. Input 行（`.input_wrapper`，InputGroup.vue）

### 5.1 行骨架

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 行间距 | `margin: 2px 0` | src/components/Sidebar/InputGroup.vue:264-266 | 每行上下 2px |
| label 宽 | `100px` | src/components/Sidebar/InputGroup.vue:267-268 | |
| label 对齐 | `text-align: right; vertical-align: middle` | src/components/Sidebar/InputGroup.vue:269-270 | |
| label 上下 margin | `3px 0` | src/components/Sidebar/InputGroup.vue:271 | 行高 = 30px 控件 + 2×2px 行距 ≈ 34px |
| 控件容器 `.input_right` | `display: inline-flex; gap: 2px; vertical-align: middle` | src/components/Sidebar/InputGroup.vue:273-276 | |
| `.input_right` 宽 | `calc(100% - 110px)`，`margin-left: 4px` | src/components/Sidebar/InputGroup.vue:277-278 | 100 label + 4 margin + 余量 |
| 无 label 时 `.full_width` | `calc(100% - 8px)` | src/components/Sidebar/InputGroup.vue:280-282 | |
| 可展开 `.expandable` | `calc(100% - 134px)` | src/components/Sidebar/InputGroup.vue:283-285 | 让出 24px 展开钮 |
| 展开态 `.expanded` | `display:block; width: calc(100% - 7px)`；内部 input/vector `width:100%!important; display:block` | src/components/Sidebar/InputGroup.vue:289-297 | |
| 展开按钮 `.tool.input_expand_button` | `float:right; width:22px; padding-left:0; padding-top:2px` | src/components/Sidebar/InputGroup.vue:298-303 | 图标 ChevronDown/Up `:size="20"`（:14-15） |
| 单轴控件宽 | axes=1 时 input/select `width:100%` | src/components/Sidebar/InputGroup.vue:286-288 | |

### 5.2 text / molang（PrismEditor）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| wrapper 背景 | `--color-dark` = `#20272d` | src/css/common.css:313 | |
| wrapper padding | `4px`，底 `2px` | src/css/common.css:318-319 | |
| 字号 | `15px`，字体 `--font-code` | src/css/common.css:320,316 | |
| 宽度 | `width: fit-content; min-width: 100%` | src/css/common.css:321-322 | |
| z-index | `3` | src/css/common.css:315 | |
| 外框 | `.prism-editor-component` `border: 1px solid var(--color-border)` = `#1a1c1f`，宽 100% | src/css/common.css:328-331 | 控件总高 = 15×1.5 行高+padding+border ≈ 30px（与 input 对齐）[INFERENCE] |
| 文字色 | pre `--color-text` = `#bcc3ca` | src/css/common.css:335-337 | |
| token 配色 | punctuation `#5ba8c5`、operator/keyword `#fc2f40`、number/boolean `#b99cff`、function `#94e400`、selector `#92dcff` | src/css/common.css:360-373 | |
| 自动补全框 | 背景 `#29323a`（--color-interface），边框 `#1a1c1f`，`max-height:220px`，padding-bottom 10px；项 `padding:3px 8px`；hover/selected 背景 `#34404a`、文字 `#f7f9ff` | src/css/common.css:341-358 | |

### 5.3 number

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 高/底/边框/字色 | 30px / `#20272d` / `1px #1a1c1f` / `#b99cff` | src/css/common.css:78-90,366-368 | 见 §4 |
| step/min/max | 来自 input 定义（`:step/:min/:max` 绑定） | src/components/Sidebar/InputGroup.vue:57-58 | |

### 5.4 vector（多轴 text/number）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `.input_vector` 宽 | `40px`，`flex-grow: 1`（flex 容器 gap 2px 均分） | src/components/Sidebar/InputGroup.vue:323-326,275 | |
| 首个无左 margin | `.input_vector:first-child { margin-left: 0 }` | src/components/Sidebar/InputGroup.vue:327-329 | |
| text 型外包 | `.prism_editor_outer_wrapper.input_vector` | src/components/Sidebar/InputGroup.vue:66 | |

### 5.5 checkbox

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 结构 | 隐藏原生 input（`width/height:0; opacity:0; appearance:none`），显示 lucide 图标 | src/components/Form/Checkbox.vue:3-5,39-46 | |
| 图标尺寸 | `Square`/`CheckSquare` `:size="21"` → 21×21px | src/components/Form/Checkbox.vue:4-5 | |
| 勾选色 | `svg path:first-child` `color: var(--color-accent)` = `#20ddff`，`stroke-width: 3px` | src/components/Form/Checkbox.vue:47-50 | |
| 容器 padding | `2px` | src/components/Form/Checkbox.vue:36-38 | 行内总高 ≈ 25px |

### 5.6 select / select_custom

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| select 基线 | 见 §4（高 30px，`appearance:auto`，radius 0） | src/css/common.css:78-86,231-236 | |
| select_custom 的 select | `width: 140px; flex-grow: 1` | src/components/Sidebar/InputGroup.vue:305-308 | 选中 custom 时追加一个 text input（:102） |

### 5.7 color（vue-color Chrome）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 弹出面板宽 | `310px`，`margin: 2px 0` | src/css/common.css:286-290 | `div.vc-chrome` |
| 面板体背景 | `--color-interface` = `#29323a` | src/css/common.css:291-293 | |
| 字段 input | 色 `#bcc3ca`，边框 `1px #1a1c1f`，高 `24px`，无阴影 | src/css/common.css:294-300 | |
| toggle 图标 fill | `--color-text` = `#bcc3ca` | src/css/common.css:301-303 | |
| 饱和度圆点偏移 | `margin-top:-5px; margin-left:-4px` | src/css/common.css:304-307 | |

### 5.8 range（Quick Setup 等用）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 轨道高 | `4px`（webkit）/ `2px`+radius 3px（moz） | src/css/common.css:154-166 | 色 `--color-track`=`--color-bar`=`#34404a` |
| 控件总高 | `30px`，透明背景、无边框 | src/css/common.css:99-111 | |
| thumb | `20×20px`，圆，`margin-top:-9px`，背景 `--color-center`=`--color-interface`=`#29323a`，边框 `2px solid --color-thumb`=`--color-accent`=`#20ddff` | src/css/common.css:109-139 | |
| thumb hover | 背景变 `#20ddff`，无边框 | src/css/common.css:140-147 | |

### 5.9 列表型（axis_count = -1，`ul.input_list`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 列表项行距 | `margin: 2px 0` | src/components/Sidebar/InputGroup.vue:309-311 | |
| 项内控件宽 | `calc(100% - 80px)`，`margin-left: 52px`，`float: left` | src/components/Sidebar/InputGroup.vue:312-316 | |
| 项内删除 .tool | `padding: 2px 0; width: 24px; height: 30px` | src/components/Sidebar/InputGroup.vue:317-321 | 图标 `\u2A09`（✕） |
| 添加按钮 `.list_add_tool` | `vertical-align: sub`，图标 `\uFF0B`（＋，`.unicode_icon.plus` 18pt） | src/components/Sidebar/InputGroup.vue:19,330-332 | |

### 5.10 event_list / event_timeline 行

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `.event_list` | `min-height:30px; flex-grow:1; padding:0 1px` | src/components/Sidebar/InputGroup.vue:334-340 | |
| 事件 chip `li.event_list_event` | 高 `30px`，`padding: 4px 13px`，`margin: 1px 2px`，背景 `#34404a`（--color-bar），`border-radius:5px`，阴影 `0 1px 14px rgba(0,0,0,0.18)` | src/components/Sidebar/InputGroup.vue:341-349 | |
| chip label 字体 | `--font-code` | src/components/Sidebar/InputGroup.vue:350-352 | |
| chip 删除图标 | lucide `X :size="18"`，`margin-top:-2px` | src/components/Sidebar/InputGroup.vue:110,353-355 | class highlighting_button，hover `#f7f9ff` |
| event_speed_list chip | `padding: 5px`；X 按钮 `float:right; margin-top:1px` | src/components/Sidebar/InputGroup.vue:364-371 | |
| `.event_min_speed` | `width:100%; display:flex; gap:5px; border-top:2px solid #29323a; padding-top:4px` | src/components/Sidebar/InputGroup.vue:372-378 | label `padding-top:3px`；input 宽 `56px`（:379-384） |
| `ul.event_timeline` | `padding: 0 10px; margin-left: 20px` | src/components/Sidebar/InputGroup.vue:385-389 | |
| 时间轴竖线（有项时 :before） | 宽 `6px`，`left:-11px`，`top:-4px/bottom:-2px`，背景 `#34404a`，radius 3px | src/components/Sidebar/InputGroup.vue:390-400 | |
| 时间轴行 | `display:flex; gap:6px; margin:2px 0; padding-left:2px` | src/components/Sidebar/InputGroup.vue:401-408 | |
| 行节点圆点（::before） | `16×16px` 圆，`top:7px; left:-26px`，背景 `--color-title`=`#4b5b69`，hover `brightness(1.2)` | src/components/Sidebar/InputGroup.vue:409-422 | |
| 时间 input 宽 | `69px`（step 0.05，min 0） | src/components/Sidebar/InputGroup.vue:426-428,151 | |
| 时间轴 label | `--font-code`，`padding:3px` | src/components/Sidebar/InputGroup.vue:429-432 | |
| `.timeline_remove_button` | `margin-left:auto; margin-top:2px`，X 18px | src/components/Sidebar/InputGroup.vue:164,433-437 | |

## 6. Quick Setup 按钮（`#test_quick_setup_button`，effect 标签页底部）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 位置 | 独立 `.input_group` 内，`display:block; margin:auto; margin-top:16px` | src/components/Sidebar.vue:48-53,226-230 | 水平居中 |
| 尺寸 | button 基线：padding `8px 12px`，`border-radius:1px`，无边框；高 = 30px 左右（icon24+8×2=40？icon svg 24px 行高 1.5 基线，实际 ≈ 24+16=40px）[INFERENCE 高度未显式定义] | src/css/common.css:179-188 | |
| 背景/文字 | `#34404a` / 继承 `#bcc3ca` | src/css/common.css:182-183 | |
| hover | 背景 `#20ddff`，文字 black | src/css/common.css:189-192 | |
| 图标 | Wand（lucide 默认 24px），后随文字 "Quick Setup" | src/components/Sidebar.vue:50-51 | svg `vertical-align:middle`（common.css:69-71） |

> Quick Setup 面板（setup tab 的 `<quick-setup>`）：预设列表 `.preset_option_list` flex `gap:2px`、项宽 `80px` padding `8px`、选中背景 `#34404a`、hover 文字 `#f7f9ff`；`.input_bar` flex `margin:10px 14px; gap:8px`，range 数字 label 宽 `46px`。来源：src/components/Sidebar/QuickSetup.vue:515-613（style 块起 515 行）。

## 7. 列表项（Curves / Events 占位行）

### 7.1 Add Curve / Add Event 按钮（ListAddButton / `#add_curve_button`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 边框 | `1px dashed var(--color-bar)` = `#34404a` 虚线 | src/components/Sidebar.vue:251-253；src/components/Form/ListAddButton.vue:23 | |
| padding | `2px` | src/components/Sidebar.vue:254；ListAddButton.vue:24 | |
| 宽 | `#add_curve_button` `width:100%`；ListAddButton `width:auto` | src/components/Sidebar.vue:252；ListAddButton.vue:22 | |
| 图标 | Plus `:size="20"`，居中 block，`opacity:0.8` | src/components/Form/ListAddButton.vue:3,30-34 | |
| hover | 背景 `--color-dark` = `#20272d` | src/components/Sidebar.vue:257-259；ListAddButton.vue:27-29 | |
| 总高 | 2+2 padding + 20 icon + 2×1 边框 ≈ 26px [INFERENCE] | — | |

### 7.2 Curve 组件行（`.curve`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 容器 padding | 上 `12px`，下 `8px` | src/components/Sidebar/Curve.vue:421-423 | 变量 `--color-curve:#657c86`（:424） |
| 图形区 `.curve_display` | 高 `height+10`（默认 height=140 → 150px），背景 `#34404a`，`margin-top:10px` | src/components/Sidebar/Curve.vue:5,154,426-430 | 内嵌 input-group（mode/min/max 等，:3） |
| 曲线路径 | 描边 2px `#657c86` | src/components/Sidebar/Curve.vue:447-451 | |
| 辅助线 | 2px 虚线 `--color-selection`（dasharray 8 / 6） | src/components/Sidebar/Curve.vue:457-468 | |
| 节点圆点 | `8×8px` 圆 `#bcc3ca`；选中 `10×10px` `#20ddff`；hover 白 | src/components/Sidebar/Curve.vue:508-522,533-539 | |
| hover 行背景 | `rgba(174,217,255,0.1)` | src/components/Sidebar/Curve.vue:502-507 | |
| bezier 手柄点 | `8×8px` `#939aa3`，热区 ::before 16×16 | src/components/Sidebar/Curve.vue:543-560 | |
| `.curve_point_options` | flex；label `padding:4px 8px`；input `min-width:40px; flex-grow:1` | src/components/Sidebar/Curve.vue:567-581 | |
| `.tool.slim` | 宽 `24px`，`padding:1px 0` | src/components/Sidebar/Curve.vue:582-586 | |
| `.curve_footer` | flex，高 `30px`；拖拽线 2px `#34404a`（hover 变 selection 色） | src/components/Sidebar/Curve.vue:587-606 | Remove Curve `.tool` 宽 auto（:83） |

### 7.3 Event 列表行（events tab，EventList.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `#event_list` | `margin-block: 12px` | src/components/Sidebar/EventList.vue:95-97 | |
| `.event` 项 | `padding: 20px 6px`；`border-top: 2px solid #34404a`；`border-bottom: 2px solid transparent` | src/components/Sidebar/EventList.vue:98-102 | 首项 border-top 透明、`padding-top:4px`（:103-107） |
| 排序提示线 | sort_before/after：`border-top/bottom: 2px solid #20ddff` | src/components/Sidebar/EventList.vue:108-113 | |
| `.event_header_bar` | flex，`gap:4px`，背景 `#34404a`，`padding:5px` | src/components/Sidebar/EventList.vue:114-118 | |
| header label | `padding:4px; min-width:80px; text-align:right` | src/components/Sidebar/EventList.vue:120-124 | "Event ID" |
| header input | `margin-right:auto`（占剩余宽） | src/components/Sidebar/EventList.vue:125-127 | 30px 高（§4） |
| 排序把手 | GripVertical 图标，`cursor:grab; padding-top:2px` | src/components/Sidebar/EventList.vue:10,128-131 | lucide 默认 24px |
| Remove Event 按钮 | `padding:4px`；hover 文字 `#f7f9ff` | src/components/Sidebar/EventList.vue:133-139 | |
| Add Event | ListAddButton（见 7.1） | src/components/Sidebar/EventList.vue:20 | |

## 8. HelpPanel（`#help_panel`）

```
┌─ #help_panel (绝对定位右侧, 宽482, top32 bottom33, z5) ─┐
│ .help_header: [X 22px] [‹ Back to overview (h29)]      │
├─────────────────────────────────────────────────────────┤
│ content (overflow-y:auto, padding 14)                   │
│  h1 Documentation (border-bottom 2px selection 色)      │
│  ul>li(pad 3/7) category_title(灰大写)                  │
│    ul>li(pad-left 25) clickable → hover 底 #34404a      │
│  h2/h3 节标题; p(margin-bottom 12)                      │
│  .input_help: h2+图标(22px 徽标) / .input_info_bar h24  │
└─────────────────────────────────────────────────────────┘
```

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 面板宽 | `482px`，`max-width:100%` | src/components/HelpPanel.vue:82-84 | 非 portrait |
| 定位 | `position:absolute; right:0; top:32px; bottom:33px; z-index:5` | src/components/HelpPanel.vue:86-92 | |
| 背景/边框 | `#29323a`（--color-interface）；`1px solid #1a1c1f` | src/components/HelpPanel.vue:85,93 | |
| 布局 | flex 纵向；header `flex-shrink:0` | src/components/HelpPanel.vue:78-81,100-102 | |
| 内容区 | `overflow-y:auto; padding:14px` | src/components/HelpPanel.vue:103-107 | |
| portrait 模式 | `width:100%` | src/components/HelpPanel.vue:95-99 | |
| 关闭按钮 X | lucide `:size="22"`，`margin:4px` | src/components/HelpPanel.vue:4,108-110 | highlighting_button，hover `#f7f9ff` |
| 返回按钮 | 高 `29px`，`padding:2px/右8px`，ChevronLeft 20px，svg `margin-top:-2px`；portrait 高 43 pad 10 | src/components/HelpPanel.vue:5,111-128 | hover 文字 `#f7f9ff`（:123-125） |
| 目录 li | `padding: 3px 7px`（portrait 上下 5px） | src/components/HelpPanel.vue:129-135 | |
| 子目录缩进 | `ul>li>ul padding-left:2px`；`li>ul>li padding-left:25px; margin-left:-2px` | src/components/HelpPanel.vue:136-142 | |
| 类目标题 | `--color-text_grayed`=`#939aa3`，大写 | src/components/HelpPanel.vue:143-146 | |
| 可点项 hover | 文字 `#f7f9ff`，背景 `#34404a` | src/components/HelpPanel.vue:147-153 | |
| 段落 p | `margin-bottom:12px; line-height:normal` | src/components/HelpPanel/HelpText.vue:57-60 | |
| h1 | `margin-bottom:12px; padding-bottom:2px; border-bottom:2px solid var(--color-selection)` | src/components/HelpPanel/HelpText.vue:61-65 | |
| h2 | `margin-top:22px; margin-bottom:6px` | src/components/HelpPanel/HelpText.vue:66-69 | |
| h3 | `margin-top:15px; margin-bottom:5px; font-weight:600` | src/components/HelpPanel/HelpText.vue:70-74 | |
| 代码行 code | `padding:4px 8px`，色 `#50cca7`，字号 `0.92em`，背景 `#20272d`，宽 `208px`，`margin-right:8px` | src/components/HelpPanel/HelpText.vue:42-54 | 行容器 flex、`margin-bottom:2px`（:37-41） |
| input_help 类型图标 | svg 高 `22px`，`margin-top:-5px`，背景 `#34404a`，边框 `1px #20272d`，radius `3px` | src/components/HelpPanel/HelpInputList.vue:103-109 | |
| `.input_info_bar` | 背景 `#20272d`，高 `24px`，radius `4px`，`margin-bottom:3px` | src/components/HelpPanel/HelpInputList.vue:110-115 | |
| 类型标签 | `padding:1px 8px`，radius 4px，文字 `#1a1c1f`，背景 `#20ddff`（type）/`#bcc3ca`（context） | src/components/HelpPanel/HelpInputList.vue:116-131 | |
| context 彩色徽标（内联 style） | emitter `#e98989`、particle `#f9da88`、spawned_emitter `#db57ae` | src/components/HelpPanel/HelpInputList.vue:73-77,16 | |
| select 描述缩进 | `padding-left:30px` | src/components/HelpPanel/HelpInputList.vue:132-134 | |

## 9. InfoBox（`#box`）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 定位 | `position:absolute; top:20px; left/right:0; margin:auto; z-index:50` | src/components/InfoBox.vue:16,22-28 | 顶部水平居中 |
| 宽 | `500px; max-width:100%` | src/components/InfoBox.vue:20-21 | |
| 背景/边框 | `#29323a`（--color-interface）；`1px solid #1a1c1f` | src/components/InfoBox.vue:17,19 | |
| 阴影 | `0 0 15px rgba(0,0,0,0.5)` | src/components/InfoBox.vue:18 | |
| padding | `16px 20px` | src/components/InfoBox.vue:25 | |
| 字号 | `1.1em` | src/components/InfoBox.vue:29 | |
| overflow | `hidden` | src/components/InfoBox.vue:24 | |
| 关闭按钮 | `.tool` + `\u2A09`（✕，`unicode_icon` 24×24/14pt），绝对定位 `top:13px; right:10px` | src/components/InfoBox.vue:4,31-35 | `.tool` 基线见 §4 |

## 10. 滚动条（全局，作用于 sidebar/help 滚动区）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 宽/高 | `8px / 8px` | src/css/common.css:4-7 | webkit |
| track / corner 背景 | `--color-interface` = `#29323a` | src/css/common.css:9-14 | |
| thumb 背景 | `--color-bar` = `#34404a` | src/css/common.css:16-18 | |
| thumb hover | `--color-title` = `#4b5b69` | src/css/common.css:19-21 | |
| Firefox | `scrollbar-width: thin; scrollbar-color: #34404a #29323a` | src/css/common.css:28-31 | |
| prism 编辑器内 | `scrollbar-color: #34404a #20272d` | src/css/common.css:323 | |
| 文本选区 | 背景 `--color-selection` = `rgba(110,142,191,0.3)` | src/css/common.css:22-24 | |
| placeholder | `opacity: 0.6` | src/css/common.css:25-27 | |

---

## 备注与推断汇总

- `[INFERENCE]` 项：Quick Setup 按钮高度、ListAddButton 总高、Prism 编辑器控件总高均未显式定义，按盒模型推算；克隆实现时应以实际渲染测量复核。
- lucide-vue 图标未传 `size` 的一律为库默认 `24px`（tab 图标、GripVertical 等）；显式 `:size` 的已逐条标注（Chevron 20、CheckSquare/Square 21、X 18/22、Plus 20）。
- 侧栏实际像素宽由运行时窗口宽与 localStorage 决定（App.vue:76-87,136），不是固定常量。
- 字体渲染差异豁免：Lato（UI）/Consolas（代码）仅记录数值不做像素比对。
