# Snowstorm UI 像素指标 03：曲线编辑器 / 渐变编辑器 / 事件系统

基准：原版 Snowstorm v3.2.2 克隆源码 `build/_snowstorm_src/`（下文 file:line 均相对该目录）。
字体渲染差异豁免，但字号/字重数值仍记录。CSS 变量来自 `src/css/common.css` body 上的定义（common.css:33-47），本文统一解析为 hex。

## 区域结构 ASCII 示意图

```
┌─ .curve (padding-top:12, padding-bottom:8) ──────────────────────────┐
│ <input-group> Name/Mode/Input/Range 行（标准 select 30px 高）         │
│ ┌─ .curve_display (bg=#34404a, height=150px, margin-top:10) ───────┐ │
│ │ <svg 100%×100%>  curve_path(2px,#657c86)  虚线 guides(2px)       │ │
│ │ ul.curve_controls (flex, 每节点 flex-grow:1 列)                   │ │
│ │   ● curve_point 8×8 圆 (selected 10×10, #20ddff)                  │ │
│ │   ○ curve_handle_point 8×8 (+16×16 热区 ::before)                 │ │
│ │ 1.0 ← curve_max_num (top:0,left:2)   0 ← curve_min_num(bottom:0) │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ .curve_point_options (flex: label + number inputs + 删除工具)         │
│ .curve_footer (h:30, flex: fill_line 拖拽调高度 + "Remove Curve")     │
└──────────────────────────────────────────────────────────────────────┘

┌─ .color_gradient ────────────────────────────────────────────┐
│ ┌ .gradient_container (h:20, w:calc(100%-10px), 棋盘格) ───┐ │
│ │ ▓▓ gradient_inner (linear-gradient 预览)                 │ │
│ │ ▮ gradient_point 10×16 (top:10, margin-left:-5)          │ │
│ └──────────────────────────────────────────────────────────┘ │
│ [＋][⨯] 浮动按钮(.tool)   vc-chrome 取色器 宽 310px           │
└──────────────────────────────────────────────────────────────┘

#event_list (margin-block:12)
└─ li.event (padding:20px 6px, border-top 2px #34404a)
   ├─ .event_header_bar (flex, gap:4, bg:#34404a, padding:5)
   │   [≡] Event ID [input...]            Remove Event
   └─ .event_subpart (border-left:5px #34404a, padding-left:12, pt/pb:8)
       ├─ .header_bar (flex, gap:8): ≡  #n ─ fill_line ── [✕]
       ├─ .section_bar (mt:8, mb:4): 小节标题
       ├─ li.input_wrapper (flex, gap:8, m:2 0): label 95px 右对齐 + 控件
       └─ ul.create_bar (flex, gap:6, padding:4 6): + 各节创建按钮

event_timeline (ul, padding:0 10, margin-left:20)
   │ ← 竖线 ::before w:6 r:3 bg:#34404a (left:-11, top:-4, bottom:-2)
 ●─├─ li (flex, gap:6, m:2 0): 节点 ::before 16×16 圆 #4b5b69 (left:-26,top:7)
   │   [time input 69px] [事件 chip h:30 r:5 ...]            [✕]
```

## 共享 CSS 变量（src/css/common.css:33-47，body 作用域）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| --color-background | `#29323a` | src/css/common.css:34 | |
| --color-dark | `#20272d` | src/css/common.css:35 | EventPicker 弹层背景、input 背景 |
| --color-border | `#1a1c1f` | src/css/common.css:36 | |
| --color-interface | `#29323a` | src/css/common.css:37 | 取色器 body 背景、棋盘格底色 |
| --color-bar | `#34404a` | src/css/common.css:38 | 曲线画布背景、事件头条背景、时间轴竖线、棋盘格交错色 |
| --color-title | `#4b5b69` | src/css/common.css:39 | 时间轴节点圆点 |
| --color-selection | `rgba(110, 142, 191, 0.3)` | src/css/common.css:40 | 曲线辅助虚线、贝塞尔控制线、hover 左边框 |
| --color-highlight | `#f7f9ff` | src/css/common.css:41 | hover 高亮文字 |
| --color-text | `#bcc3ca` | src/css/common.css:42 | 曲线节点点、正文 |
| --color-text_grayed | `#939aa3` | src/css/common.css:43 | 控制柄点、create_bar 按钮、弹层默认文字 |
| --color-accent | `#20ddff` | src/css/common.css:44 | 选中曲线点、排序插入指示线 |
| --font-code | Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace | src/css/common.css:46 | 等宽字体（事件 id/数值） |

全局控件基准（被本区组件复用）：

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| input/select 高度 | 30px | src/css/common.css:83 | 背景 #20272d，border 1px #1a1c1f，padding 4px 1px |
| text/number input padding | 0 4px | src/css/common.css:89 | 字体 var(--font-code) |
| .tool 按钮 | padding 2px 8px（top 1px），inline-block | src/components/App.vue:183-186 | hover 文字变 #f7f9ff（App.vue:192-193） |
| .unicode_icon | 24×24px，font-size 14pt，居中 | src/css/common.css:196-204 | ＋/⨯ 图标；.plus 变体 18pt、margin-top -4px（:212-215） |
| .checkerboard 棋盘格 | 16×16 格，色 = #29323a / #34404a | src/css/common.css:223-229 | 渐变条底纹 |
| .highlighting_button hover | color → #f7f9ff | src/css/common.css:253-258 | |

## 1. Curve 曲线编辑器（src/components/Sidebar/Curve.vue）

模式说明：模式切换不在 Curve.vue 内，而是其内嵌 InputGroup 的标准 select（"Mode"，选项 catmull_rom / linear / bezier / bezier_chain，src/curves.js:25-35），select 高 30px（common.css:83,231-232）。

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| .curve 容器 padding | top 12px，bottom 8px | src/components/Sidebar/Curve.vue:422-423 | 局部变量 --color-curve: #657c86（:424） |
| 画布高度（数据层） | height = 140px | src/components/Sidebar/Curve.vue:154 | 数据项 `height`，拖拽 fill_line 可调 |
| .curve_display 实际高度 | 150px（height+10 内联） | src/components/Sidebar/Curve.vue:5 | 比逻辑高多 10px |
| .curve_display 背景 | #34404a（var(--color-bar)） | src/components/Sidebar/Curve.vue:428 | margin-top 10px（:429），position:relative（:427） |
| 内嵌 svg | 100%×100% | src/components/Sidebar/Curve.vue:431-434 | |
| 曲线 stroke | 2px，#657c86（--color-curve），fill none | src/components/Sidebar/Curve.vue:447-451 | .curve_path |
| 贝塞尔控制线 stroke | 2px，rgba(110,142,191,0.3)（--color-selection），fill none | src/components/Sidebar/Curve.vue:452-456 | .curve_handles |
| 垂直辅助虚线 | 2px，--color-selection，dasharray 8 | src/components/Sidebar/Curve.vue:457-462 | .vertical_line_path |
| 水平辅助虚线 | 2px，--color-selection，dasharray 6 | src/components/Sidebar/Curve.vue:463-468 | .horizontal_line_path |
| min/max 角标 | absolute，left 2px；max top:0 / min bottom:0；pointer-events none | src/components/Sidebar/Curve.vue:435-446 | 无显式字号，继承 body |
| 控制层 ul.curve_controls | absolute 铺满，display flex | src/components/Sidebar/Curve.vue:469-476 | bezier_chain 模式改 display:block（:489-491）、cursor copy（:477-479） |
| 节点列 .curve_node / 间隙列 .curve_add | height 100%，flex-grow 1 | src/components/Sidebar/Curve.vue:480-484 | 首尾 .curve_add max-width 20px（:564-566） |
| 节点列光标 | ns-resize（bezier_chain 为 move） | src/components/Sidebar/Curve.vue:486,493 | |
| 节点列 hover 背景 | rgba(174, 217, 255, 0.1) | src/components/Sidebar/Curve.vue:502-507 | .curve_add hover 同色 |
| 控制点 .curve_point | 8×8px 圆（radius 50%），bg #bcc3ca（--color-text），margin-bottom 1px，水平居中 | src/components/Sidebar/Curve.vue:508-519 | hover 变白 #fff（:520-522） |
| 选中控制点 | 10×10px，bg #20ddff（--color-accent），margin-bottom 0 | src/components/Sidebar/Curve.vue:533-539 | bezier_chain 选中 margin-top 3px（:540-542） |
| 点数值标签 label | 默认 display:none，hover 显示；margin-left 12px，margin-top -8.4px，nowrap | src/components/Sidebar/Curve.vue:523-532 | |
| bezier_chain 节点热区 | 20×20px，absolute，margin-bottom -6px | src/components/Sidebar/Curve.vue:492-498 | 位置 = (width-16)*time（:29），纵向按值映射（:18） |
| 控制柄点 .curve_handle_point | 8×8px 圆，bg #939aa3（--color-text_grayed），margin 4px 6px，cursor row-resize | src/components/Sidebar/Curve.vue:543-551 | ::before 扩热区到 16×16（top/left -4）（:552-560） |
| 控制柄臂长 | handle_offset = 24px | src/components/Sidebar/Curve.vue:153 | 位置按 slope 三角函数偏移（:41-47） |
| .curve_point_options 行 | display flex | src/components/Sidebar/Curve.vue:567-569 | |
| 选项行 label | padding 4px 8px | src/components/Sidebar/Curve.vue:570-573 | 非首个 label 与删除工具 margin-left auto（:574-576） |
| 选项行 number input | width 0 / min-width 40px / flex-grow 1 | src/components/Sidebar/Curve.vue:577-581 | 高度继承全局 30px |
| 连接工具 .tool.slim | width 24px，padding 1px 0，居中 | src/components/Sidebar/Curve.vue:582-586 | "=" / "≠" 图标 |
| 删除点工具图标 | unicode ⨯（\u2A09），24×24 @14pt | src/components/Sidebar/Curve.vue:77-79 + common.css:196-204 | |
| .curve_footer | height 30px，display flex | src/components/Sidebar/Curve.vue:587-590 | |
| footer 拖拽条 .fill_line | flex-grow 1，cursor ns-resize；::after 高 2px、宽 100%、bg #34404a | src/components/Sidebar/Curve.vue:591-603 | hover 变 --color-selection rgba(110,142,191,0.3)（:604-606） |
| "Remove Curve" 按钮 | .tool（padding 2px 8px）width:auto 内联 | src/components/Sidebar/Curve.vue:83 | |
| 画布宽度回退 | 300px（未挂载时 getWidth 默认） | src/components/Sidebar/Curve.vue:157-159 | |

## 2. Gradient 渐变编辑器（src/components/Sidebar/Gradient.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| .color_gradient 宽 | 100% | src/components/Sidebar/Gradient.vue:80-82 | |
| .gradient_container 尺寸 | width calc(100% - 10px)，height 20px | src/components/Sidebar/Gradient.vue:83-86 | margin 0 5px 10px 5px（:87），border 1px solid #000（:88），棋盘格底纹（common.css:223-229，16px 格） |
| .gradient_inner | 100%×100%，background 为 linear-gradient(to right, …) | src/components/Sidebar/Gradient.vue:105-108, 39-45 | 站点位 `color percent%` |
| 色标节点 .gradient_point | 10×16px，top 10px，margin-left -5px（水平居中于百分比点），border 1px solid #212529 | src/components/Sidebar/Gradient.vue:90-99 | left = point.percent%（模板 :10），cursor pointer，overflow hidden |
| 选中色标 | border-color #fff，box-shadow 0 0 2px #000，z-index 4 | src/components/Sidebar/Gradient.vue:100-104 | |
| 拖拽解锁阈值 | 4px（|Δx|>4 才开始拖动） | src/components/Sidebar/Gradient.vue:54 | percent = round(clamp 0-100)（:57-58） |
| ＋/⨯ 按钮 | .tool，float:right 内联，图标 \uFF0B / \u2A09 24×24 | src/components/Sidebar/Gradient.vue:15-16, 20 + common.css:196-215 | |
| 取色器 vc-chrome 宽度 | 310px，margin 2px 0 | src/css/common.css:286-290 | float:left（Gradient.vue:109-111）；body 背景 #29323a（common.css:291-293） |
| 取色器字段 input | height 24px，border 1px #1a1c1f，color #bcc3ca | src/css/common.css:294-300 | |
| 取色器饱和度圆点 | margin-top -5px，margin-left -4px | src/css/common.css:304-307 | 圆点尺寸由 vue-color 库自身决定（非本项目 CSS） |

## 3. EventList 事件列表（src/components/Sidebar/EventList.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| #event_list 外边距 | margin-block 12px | src/components/Sidebar/EventList.vue:95-97 | |
| 事件项 li.event padding | 20px 6px | src/components/Sidebar/EventList.vue:98-99 | border-top 2px #34404a（:100），border-bottom 2px transparent（:101） |
| 首个事件项 | border-top 透明，padding-top 4px，margin-top 0 | src/components/Sidebar/EventList.vue:103-107 | |
| 排序插入指示 | border-top / border-bottom 2px #20ddff（--color-accent） | src/components/Sidebar/EventList.vue:108-113 | .sort_before / .sort_after |
| .event_header_bar | display flex，gap 4px，bg #34404a（--color-bar），padding 5px | src/components/Sidebar/EventList.vue:114-119 | |
| 头条 label "Event ID" | padding 4px，min-width 80px，右对齐 | src/components/Sidebar/EventList.vue:120-124 | |
| 头条 input | margin-right auto（其余靠右） | src/components/Sidebar/EventList.vue:125-127 | 高 30px（common.css:83），等宽字体（:87-89） |
| 排序手柄 .event_sort_handle | cursor grab，padding-top 2px | src/components/Sidebar/EventList.vue:128-131 | GripVertical 图标（lucide，默认 24px，模板 :10 未传 size） |
| Remove Event 按钮 | padding 4px，hover 文字 #f7f9ff（--color-highlight） | src/components/Sidebar/EventList.vue:133-139 | |

## 4. EventSubpart 递归子部件（src/components/Sidebar/EventSubpart.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| .event_subpart 容器 | border-left 5px #34404a（--color-bar），padding-left 12px，padding-top/bottom 8px | src/components/Sidebar/EventSubpart.vue:290-295 | hover 时左边框变 --color-selection rgba(110,142,191,0.3)（:296-298） |
| 选项头行 .header_bar | display flex，gap 8px | src/components/Sidebar/EventSubpart.vue:334-337 | label padding-top 4px（:338-340）；拖拽 svg cursor grab、margin-left -2px、margin-top 2px（:341-345） |
| 头行分隔线 .fill_line::after | 高 2px，宽 100%，bg #34404a | src/components/Sidebar/EventSubpart.vue:346-357 | |
| 小节标题 .section_bar | margin-top 8px，margin-bottom 4px | src/components/Sidebar/EventSubpart.vue:366-369 | |
| .descriptor_label | color #939aa3（--color-text_grayed），padding-right 4px | src/components/Sidebar/EventSubpart.vue:370-374 | |
| 嵌套列表 ul.nested_list | margin-left 12px | src/components/Sidebar/EventSubpart.vue:318-320 | |
| 可排序 li | border-top/bottom 2px transparent；排序指示 2px #20ddff | src/components/Sidebar/EventSubpart.vue:321-329 | |
| .list_add_button | margin-left 26px | src/components/Sidebar/EventSubpart.vue:331-333 | ListAddButton 组件本体样式在其自身文件（Form/ListAddButton.vue，非本区） |
| 创建按钮行 ul.create_bar | display flex，justify-content center，gap 6px，padding 4px 6px，flex-wrap wrap | src/components/Sidebar/EventSubpart.vue:299-305 | |
| create_bar 项 | 字号 0.96em，color #939aa3，hover 变 #bcc3ca（--color-text），nowrap | src/components/Sidebar/EventSubpart.vue:306-314 | svg vertical-align sub（:315-317），图标为 lucide 默认 24px |
| 输入行 .input_wrapper | display flex，gap 8px，margin 2px 0 | src/components/Sidebar/EventSubpart.vue:375-379 | |
| 输入行 label | width 95px，右对齐，margin 3px 0，flex-shrink 0 | src/components/Sidebar/EventSubpart.vue:380-386 | |
| 输入行 select | flex-grow 1，高 30px | src/components/Sidebar/EventSubpart.vue:387-389 + common.css:83 | |

## 5. EventPicker 事件选择弹层（src/components/Sidebar/EventPicker.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 触发按钮 .highlighting_button | Plus / Zap 图标 size 22px | src/components/Sidebar/EventPicker.vue:3-6 | hover 文字 #f7f9ff（common.css:253-258） |
| 弹层 ul.list | width 200px，min-height 20px，max-height 200px，overflow auto，absolute | src/components/Sidebar/EventPicker.vue:77-82 | |
| 弹层配色 | bg #20272d（--color-dark），文字 #939aa3（--color-text_grayed），border 1px #1a1c1f（--color-border），radius 2px | src/components/Sidebar/EventPicker.vue:83-87 | z-index 4（:86），text-align center（:88） |
| 弹层选项 li | height 32px，padding 4px 9px，color #bcc3ca（--color-text），text-align initial | src/components/Sidebar/EventPicker.vue:90-96 | Zap 图标 size 20px（模板 :9），svg margin-right 2px（:101-103） |
| 选项 hover | bg #29323a（--color-interface），color #f7f9ff（--color-highlight） | src/components/Sidebar/EventPicker.vue:97-100 | |

## 6. event_timeline / event_list / event_speed_list（src/components/Sidebar/InputGroup.vue）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 事件 chip li.event_list_event | height 30px，padding 4px 13px，margin 1px 2px，bg #34404a（--color-bar），radius 5px，box-shadow 0 1px 14px rgba(0,0,0,0.18) | src/components/Sidebar/InputGroup.vue:341-349 | display inline-block；label 等宽字体（:350-352）；X 图标 18px、margin-top -2px（模板 :110，样式 :353-355） |
| chip 后 EventPicker 容器 | inline-block，margin-top 4px | src/components/Sidebar/InputGroup.vue:356-359 | |
| timeline 内事件列表 .timeline_event_list | margin-top -2px，margin-bottom 0 | src/components/Sidebar/InputGroup.vue:360-363 | |
| ul.event_timeline | padding 0 10px，margin-left 20px，relative | src/components/Sidebar/InputGroup.vue:385-389 | |
| 时间轴竖线 ::before | 宽 6px，radius 3px，bg #34404a（--color-bar），left -11px，top -4px，bottom -2px | src/components/Sidebar/InputGroup.vue:390-400 | 仅 has_entries 时显示 |
| 时间轴行 li | display flex，gap 6px，margin 2px 0，padding-left 2px | src/components/Sidebar/InputGroup.vue:401-408 | |
| 关键帧节点 ::before（播放点） | 16×16px 圆（radius 50%），bg #4b5b69（--color-title），top 7px，left -26px | src/components/Sidebar/InputGroup.vue:409-419 | hover 时 filter brightness(1.2)（:420-422） |
| 行 hover 才显示切换按钮 | .event_switch_button 非 hover 时 display:none | src/components/Sidebar/InputGroup.vue:423-425 | 当前模板中已注释（:155），行为保留于 CSS |
| 时间数值 input | width 69px，step 0.05，min 0 | src/components/Sidebar/InputGroup.vue:426-428 + 模板 :151 | 高 30px 继承全局 |
| 行内 label | 等宽字体，padding 3px | src/components/Sidebar/InputGroup.vue:429-432 | |
| 行尾删除 .timeline_remove_button | margin-left auto，margin-top 2px，flex-shrink 0，X 图标 18px | src/components/Sidebar/InputGroup.vue:433-437 + 模板 :164 | |
| event_speed_list chip | padding 5px（覆盖 chip 的 4px 13px） | src/components/Sidebar/InputGroup.vue:364-367 | X 按钮 float right、margin-top 1px（:368-371） |
| .event_min_speed 行 | width 100%，display flex，gap 5px，border-top 2px #29323a（--color-interface），padding-top 4px | src/components/Sidebar/InputGroup.vue:372-378 | label padding-top 3px（:379-381） |
| Min Speed input | width 56px | src/components/Sidebar/InputGroup.vue:382-384 | |
