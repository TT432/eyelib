# metrics-05 · 3D 预览舞台（Preview.vue）与页脚工具栏

基准：Snowstorm v3.2.2 源码 `build/_snowstorm_src/`。所有 `src/css/common.css` 的变量定义在 `body` 选择器下（common.css:33-47），引用处均已解析为 hex。

## 区域结构 ASCII 示意图

```
main#preview (position:relative; --footer-height:34px)
┌──────────────────────────────────────────────────────────┐
│ #canvas_wrapper  height: calc(100% - 34px)               │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ [overlay_timestamp] ← 绝对定位 top:0 left:0          │ │
│ │   padding 4px 10px, opacity 0.5, Consolas 1.1em      │ │
│ │                                                      │ │
│ │   <canvas id="canvas"> 100% × 100%（Three.js WebGL） │ │
│ │     背景色 #29323a  GridHelper 64×64 (#3d4954)       │ │
│ │     轴线 1m RGB       参考方块 1m³ @ y=-0.51         │ │
│ │                                                      │ │
│ │ ┌─ .placeholder_bar (可显隐) bottom:34px ──────────┐ │ │
│ │ │ min-height:35px, blur(4px), 90% 背景色, 顶部1px线 │ │ │
│ │ └──────────────────────────────────────────────────┘ │ │
│ └──────────────────────────────────────────────────────┘ │
├─ footer  height:34px, 背景 #34404a, 顶边框 1px #1a1c1f ──┤
│ [Loop▼][Mode▼][碰撞][#] ··· [▶][⏸] ··· [⚠n][123 P][60 FPS]│
└──────────────────────────────────────────────────────────┘
   footer 子项 padding 4px 8px；select 高度100%、margin-left:4px
```

---

## 1. 画布 / 3D 场景

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| `main#preview` position | `relative` | src/components/Preview.vue:420 | 作为 overlay 定位基准 |
| `--footer-height` 局部变量 | `34px` | src/components/Preview.vue:421 | footer 与 canvas_wrapper 共用 |
| `#canvas_wrapper` 尺寸 | `height: calc(100% - 34px)`; `width: 100%` | src/components/Preview.vue:423-426 | |
| `canvas` 尺寸 | `height: 100%`; `width: 100%`; `outline: none` | src/components/Preview.vue:427-431 | |
| 画布背景色 (clear color) | `#29323a`（=`--color-background`） | src/components/Preview.vue:94（`BACKGROUND_COLOR = 0x29323a`）、191 | 变量定义 common.css:34 |
| WebGLRenderer 参数 | `antialias: true, alpha: true, preserveDrawingBuffer: true` | src/components/Preview.vue:185-190 | |
| renderer 尺寸 | `setSize(wrapper.clientWidth, wrapper.clientHeight)`; `setPixelRatio(devicePixelRatio)` | src/components/Preview.vue:272-282 | 随窗口 resize / orientationchange（延迟 150ms）更新（Preview.vue:283-286） |
| GridHelper 尺寸 | `new THREE.GridHelper(64, 64, …)` → 64×64 世界单位、64 格 | src/components/Preview.vue:201 | 1 格 = 1m |
| GridHelper 颜色 | `#3d4954`（中心线与网格线同色） | src/components/Preview.vue:129（`grid: 0x3d4954`）、201 | |
| GridHelper 位置偏移 | `position.y -= 0.0005` | src/components/Preview.vue:202 | 防止 z-fighting |
| GridHelper 透明度 | 未设置（默认不透明，three.js 默认 `transparent:false, opacity:1`） | src/components/Preview.vue:201 | [INFERENCE] three.js 默认 |
| GridHelper 初始可见性 | `OptionValues.grid_visible` | src/components/Preview.vue:206 | 菜单 View → Grid 切换（src/components/MenuBar.vue:77-80） |
| 坐标轴 CustomAxesHelper 尺寸 | `size = 1`（1m，原点向 +X/+Y/+Z 三条线段） | src/components/Preview.vue:132-138、200 | LineSegments + LineBasicMaterial vertexColors |
| 轴色 X（红） | `#fd3043` | src/components/Preview.vue:126 | |
| 轴色 Y（绿） | `#26ec45` | src/components/Preview.vue:127 | |
| 轴色 Z（蓝） | `#2d5ee8` | src/components/Preview.vue:128 | |
| 轴线宽 | 未设置（LineBasicMaterial 默认 1px） | src/components/Preview.vue:148 | WebGL 线宽恒 1px [INFERENCE] |
| 轴初始可见性 | `OptionValues.axis_helper_visible` | src/components/Preview.vue:205 | 菜单 View → Axis Helper（src/components/MenuBar.vue:85-88） |

## 2. 参考方块（minecraft_block）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 几何体 | `BoxGeometry(1, 1, 1)`（1m³） | src/components/Preview.vue:208 | |
| 位置 | `(0, -0.51, 0)` | src/components/Preview.vue:242 | 顶面在 y=-0.01，略低于网格 |
| 贴图 | `assets/minecraft_block.png`，mag/minFilter 均为 `NearestFilter` | src/components/Preview.vue:78、234-236 | 像素风采样 |
| 材质 | `MeshBasicMaterial({ vertexColors: true, map })` | src/components/Preview.vue:237-240 | 顶点色做面着色 |
| 面着色（顶点色 shade） | 上 1.0 / 下 0.5 / 东西 0.64 / 南北 0.8 | src/components/Preview.vue:226-231 | 模拟 Minecraft 光照 |
| UV 收缩 | 顶点 UV 向对边中点 `lerp(·, ·, 0.002)` | src/components/Preview.vue:210-213 | 防贴图出血 |
| 初始可见性 | `OptionValues.minecraft_block_visible` | src/components/Preview.vue:244 | 菜单 View → Reference Block（src/components/MenuBar.vue:81-84） |

> 注：网格 / 参考方块 / 坐标轴的切换入口在原版位于顶部菜单栏 View 菜单（MenuBar.vue:77-89），**不在页脚**；页脚只有「碰撞预览」与「占位变量条」两个切换按钮。

## 3. 轨道相机（OrbitControls）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 相机类型 | `PerspectiveCamera` | src/components/Preview.vue:183 | |
| FOV | `45`（度） | src/components/Preview.vue:183 | |
| 初始 aspect | `16/9` | src/components/Preview.vue:183 | 随后 resize 时改为 `width/height`（Preview.vue:277-278） |
| near / far | `0.1` / `3000` | src/components/Preview.vue:183 | |
| 初始位置 | `(-6, 3, -6)` | src/components/Preview.vue:184 | |
| controls.target | `(0, 0.8, 0)` | src/components/Preview.vue:194 | |
| screenSpacePanning | `true` | src/components/Preview.vue:195 | |
| zoomSpeed | `1.4` | src/components/Preview.vue:196 | |
| minDistance / maxDistance | 未设置（three.js 默认 `0` / `Infinity`） | src/components/Preview.vue:193-196 | 即无缩放距离限制 [INFERENCE：three.js OrbitControls 默认] |
| min/maxPolarAngle、enableDamping | 未设置（默认 `0`/`π`、`false`） | src/components/Preview.vue:193-196 | [INFERENCE] |

## 4. 页脚栏（footer）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| footer 高度 | `34px`（`var(--footer-height)`） | src/components/Preview.vue:480、421 | |
| footer 宽度 | `100%` | src/components/Preview.vue:478 | |
| footer 背景色 | `#34404a`（`--color-bar`） | src/components/Preview.vue:481；变量 common.css:38 | |
| footer 顶边框 | `1px solid #1a1c1f`（`--color-border`） | src/components/Preview.vue:482；变量 common.css:36 | |
| footer 布局 | `display: flex`；`overflow-x: auto`；`overflow-y: hidden`；`scrollbar-width: none`；webkit 滚动条 `height: 0` | src/components/Preview.vue:483-490 | 横向可溢出滚动但隐藏滚动条 |
| footer 字号 | `1.1em` | src/components/Preview.vue:479 | 相对全局默认（body 16px ≈ 17.6px，body 字号未显式设置 [INFERENCE]） |
| footer 文字颜色 | `#bcc3ca`（`--color-text`，继承自 body） | src/css/common.css:249；变量 common.css:42 | |
| footer 行高 | `1.5`（继承 body） | src/css/common.css:248 | |
| footer 字体 | `'Lato', -apple-system, "Segoe UI", sans-serif`（继承 body） | src/css/common.css:242 | 渲染豁免，数值记录 |
| footer 直接子项 padding | `4px 8px`（padding-top 重申 4px） | src/components/Preview.vue:491-495 | 子项背景同为 `#34404a` |
| footer `.tool` 子项 padding-top | `2px`（覆盖上面的 4px） | src/components/Preview.vue:496-498 | |
| `.spacing` 弹性间隔 | `flex: 1 1 auto`; padding/margin 0 | src/components/Preview.vue:499-503 | 共 2 处：播放按钮前、播放按钮后 |

### 4.1 左下角两个下拉（loop_mode / parent_mode）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| select#loop_mode 选项 | `auto`=Auto / `looping`=Looping / `once`=Once（默认 `auto`） | src/components/Preview.vue:24-28、307 | |
| select#parent_mode 选项 | `world`=World / `entity`=Entity / `locator`=Locator（默认 `world`） | src/components/Preview.vue:29-33、308 | |
| select appearance | `none`（去掉原生箭头） | src/components/Preview.vue:505 | |
| select 背景色 | `#20272d`（`--color-dark`） | src/components/Preview.vue:506；变量 common.css:35 | |
| select border | 全局 `1px solid #1a1c1f`（`--color-border`），footer 内 `border-top: none` | src/css/common.css:81；src/components/Preview.vue:507 | portrait_view 下再去掉底边框（Preview.vue:512-514） |
| select 高度 | `100%`（即 34px，覆盖全局 30px） | src/components/Preview.vue:508；全局 src/css/common.css:83 | |
| select margin-left | `4px` | src/components/Preview.vue:509 | |
| select padding | `2px 6px`（覆盖全局 `4px 1px`） | src/components/Preview.vue:510；全局 src/css/common.css:84 | |
| select 文字颜色 | `#bcc3ca`（`--color-text`） | src/css/common.css:82；变量 common.css:42 | |
| select 字号 | 继承 footer `1.1em` | src/css/common.css:72-77；src/components/Preview.vue:479 | |

### 4.2 图标按钮（.tool，lucide 图标）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 通用 `.tool` 尺寸 | `display: inline-block; padding: 2px 8px（top 1px）; width: 35px; height: 100%; max-height: 32px; cursor: pointer` | src/components/App.vue:183-191 | 定义在 App.vue 非 scoped 样式 |
| `.tool` hover 颜色 | `#f7f9ff`（`--color-highlight`） | src/components/App.vue:192-194；变量 common.css:41 | 只改文字/图标色，不改背景 |
| `.tool.toggle_enabled` 背景 | `#29323a`（`--color-background`） | src/components/Preview.vue:542-544；变量 common.css:34 | 碰撞/占位条开启态 |
| footer 内 `.tool` padding | `4px 8px`，`padding-top: 2px` | src/components/Preview.vue:491-498 | |
| 碰撞切换按钮 | `.tool.ground_collision`；图标 `FlipVertical2` size=20（开启）/ `Minus` size=20（关闭）；title="Preview Collisions"；默认 `collision: true` | src/components/Preview.vue:34-37、311 | |
| 占位变量条按钮 | 图标 `Hash` size=22；title="Show Variable Placeholder Bar" | src/components/Preview.vue:38-40 | |
| 播放按钮 | 图标 `Play` size=22；title="Play"；`Emitter.stop(true)` 后 `start()` | src/components/Preview.vue:44-46、172-174 | |
| 暂停按钮 | 图标 `Pause` size=22；title="Pause"；切换 `Emitter.paused` | src/components/Preview.vue:47-49、175-177 | 快捷键：空格=播放、Ctrl+空格=暂停（Preview.vue:288-297） |

### 4.3 右侧状态区（警告 / 粒子数 / FPS）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 警告 `.tool.warning` 文字色 | `#ffc107` | src/components/Preview.vue:521-525 | 仅 `warning_count > 0` 时渲染（Preview.vue:53） |
| 警告 hover 色 | `#ffe060` | src/components/Preview.vue:526-529 | |
| 警告图标 | `⚠`（unicode），`margin-right: 4px` | src/components/Preview.vue:53、530-533 | 后跟警告数量 |
| 警告刷新频率 | 每 500ms（`validate().length`） | src/components/Preview.vue:400-404 | |
| `div.stat`（粒子数/FPS 通用） | `text-align: right; float: right; background: transparent; min-width: 72px` | src/components/Preview.vue:515-520 | padding 继承 footer 子项 `4px 8px` |
| 粒子数显示 | `{count} P`，千分位逗号格式化；每 200ms 刷新 | src/components/Preview.vue:54、380-383、397-399 | |
| FPS 显示 | `{fps} FPS`，**内联 `width: 66px`**（覆盖 min-width 72px 的生效宽度）；每 1000ms 刷新 | src/components/Preview.vue:55、393-396 | min-width 仍 72px，width 66px 时实际以 min-width 为准 [INFERENCE：CSS min-width 优先] |

## 5. 画布覆盖层

### 5.1 左上角时间显示 `#overlay_timestamp`

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 定位 | `position: absolute; top: 0; left: 0` | src/components/Preview.vue:432-435 | 相对 main#preview |
| padding | `4px 10px` | src/components/Preview.vue:437 | |
| opacity | `0.5` | src/components/Preview.vue:436 | |
| 字号 | `1.1em` | src/components/Preview.vue:438 | |
| 字体 | `Consolas, monospace` | src/components/Preview.vue:439 | 渲染豁免 |
| 颜色 | 继承 `#bcc3ca`（`--color-text`） | src/css/common.css:249 | |
| pointer-events | `none` | src/components/Preview.vue:440 | 不挡鼠标 |
| 内容格式 | `{整数秒}:{十分位}`（如 `1:3`），来自 `Emitter.age` | src/components/Preview.vue:384-389 | |

### 5.2 占位变量条 `.placeholder_bar`（"Empty" 提示所在）

> 源码中无字面「Empty」文案；占位变量条在无任何未定义变量时显示 **"No undefined variables found"**（src/components/Preview.vue:15），即该区域唯一的空态提示。

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 定位 | `position: absolute; bottom: 34px; width: 100%` | src/components/Preview.vue:442-446 | 恰好贴在 footer 上方 |
| min-height | `35px` | src/components/Preview.vue:445 | |
| 布局 | `display: flex` | src/components/Preview.vue:447 | |
| 背景色 | `color-mix(in srgb, #29323a 90%, transparent)`（`--color-background` 90%） | src/components/Preview.vue:448；变量 common.css:35→34 | 半透明 |
| 顶边框 | `1px solid #1a1c1f`（`--color-border`） | src/components/Preview.vue:449；变量 common.css:36 | |
| 毛玻璃 | `backdrop-filter: blur(4px)` | src/components/Preview.vue:450 | |
| ul 内边距/间距 | `padding: 2px 10px; gap: 2px 12px; flex-grow: 1; flex-wrap: wrap` | src/components/Preview.vue:452-458 | |
| li 布局 | `display: flex; gap: 5px; align-items: center` | src/components/Preview.vue:459-463 | |
| 空态提示文字 | "No undefined variables found"，`label` 颜色 `#939aa3`（`--color-text_grayed`） | src/components/Preview.vue:15、470-472；变量 common.css:43 | |
| label 颜色 | `#939aa3`（`--color-text_grayed`） | src/components/Preview.vue:470-472 | |
| input（变量值）宽度 | `70px`；type=number，字体 `var(--font-code)` = Consolas 等 | src/components/Preview.vue:467-469；src/css/common.css:87-90、46 | |
| li 内烘焙按钮 | `.tool` `width: 24px; padding: 2px`；图标 `CheckCheck` size=20 | src/components/Preview.vue:473-476、11-13 | |
| 关闭按钮 | `.tool` `padding-top: 4px`；图标 `X` size=22 | src/components/Preview.vue:464-466、18-20 | |

## 6. 帧率 / 更新节流（行为指标）

| 元素/属性 | 值 | 来源(file:line) | 备注 |
|---|---|---|---|
| 模拟 tick 节流 | 距上一帧 > 32ms 才 `Emitter.tick()`（≈30fps 上限） | src/components/Preview.vue:256-262 | |
| 快捷键 | Space=播放，Ctrl+Space=暂停（输入框聚焦时忽略） | src/components/Preview.vue:288-297 | |
| 烘焙确认弹窗 | `max-width: 308px`（内联） | src/components/Preview.vue:58 | modal_dialog，样式属对话框区域 |
