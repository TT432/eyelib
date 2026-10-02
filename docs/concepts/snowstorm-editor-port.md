# Snowstorm 粒子编辑器游戏内复刻——设计规格

> 状态：已批准（用户三项决策 2026-09-29 当场落盘，见决策记录）
> 关联：ADR-0036、ADR-0034（wintersky as-is 移植）、ADR-0035（wintersky 融入设计）、`wintersky-integration.md` §8（验证方法学）

## 0. 目标与现状盘点

将 Snowstorm（JannisX11/snowstorm v3.2.2，基岩版粒子可视化编辑器，GPL-3.0）完整复刻为
eyelib 游戏内粒子编辑器：表单编辑 → wintersky 实时预览 → 导入/导出基岩版 particle JSON 的完整闭环。

现状基线（已存在，全部复用）：

- `io.github.tt432.eyelib.wintersky`：wintersky 1.3.3 as-is 移植（Scene/Config/Emitter/Particle，零 MC 依赖，
  Node golden oracle 全绿）。编辑器数据层直接驱动 `Config.set(id, value)` 与 `Config.setFromJSON`。
- 游戏内渲染闭环（wu7）：`WinterskySceneManager`/`WinterskyParticleRenderer`/`WinterskyRenderTypes`/
  `WinterskyParticleFileLoader`/`WinterskyParticlePort`，管理界面（I 键）已有粒子预览入口
  `WinterskyParticleScreen`。
- LDLib2 UI 工具包（可选前置，三版本齐备：1.20.1 fork.6 / 1.21.1 / 26.1.2.33）：
  retained-mode UIElement + taffy flexbox + 事件系统 + TextField/Selector/Button/ColorConfigurator。
  nodegraph 编辑器（`client/nodegraph/editor/ldlib2`）已建立全部使用模式与版本守卫；
  `gradle/ldlib2-uitest.gradle` 提供 in-client UI 测试框架（`-PldTest`）。
- `FileDialogService`（client/gui/io）：文件选择对话框。
- `AddonTextureRegistry`：运行时纹理注册。

Snowstorm 源（已全量盘点，克隆于 `build/_snowstorm_src`，~200KB）：

- 数据层（vue 无关，可 as-is 移植）：`input_structure.js`（9 subject tab 表单骨架 Data）、
  `input.js`（Input 类：value setter→Config.set 回写、条件可见性、event_timeline）、
  `curves.js`+Curve.vue（4 模式曲线）、`gradient.js`（渐变 stops）、events
  （EventList/EventSubpart 递归模型/EventPicker）、`event_sub_effects.js`（子效果注册表）、
  `export.js`（generateFile→基岩 JSON）、`import.js`（setFromJSON+回填）、
  `texture_edit.js`（画布绘制：brush/eraser/fill/undo/redo）、`variable_placeholders.js`、
  `options.js`、`edits.js`、`util.js`、`emitter.js`（编辑器侧 wintersky 装配）。
- UI 层（Vue，**不作为移植源，仅作行为/布局参照**）：App.vue/Sidebar.vue/Preview.vue/Curve.vue/
  Gradient.vue/TextureInput.vue/QuickSetup.vue/MenuBar/CodeViewer/ExpressionBar/WarningDialog。
- 资产：`assets/` 9 PNG（7 sprite + minecraft_block + spark）、`examples/` 9 个粒子 JSON。

## 1. 用户决策记录（2026-09-29，ask 工具当场确认）

| # | 问题 | 决策 |
|---|---|---|
| U1 | 复刻范围 | **完整复刻**：表单+曲线编辑器+事件编辑器+渐变+贴图绘制+QuickSetup+CodeViewer+子效果，Snowstorm 全部功能 |
| U2 | 宿主形态 | **游戏内全屏 Screen**：预览用 3D 舞台（自由轨道相机，脱离世界）；管理界面加入口 |
| U3 | 贴图编辑器 | **V1 就复刻**：brush/eraser/fill/undo 全套（texture_edit.js 移植） |
| U4 | 1:1 对齐验收标准（2026-10-01 ask 确认） | **布局像素级，字体豁免**：逐区域从 v3.2.2 CSS/Vue 提取像素尺寸/间距/颜色/图标 1:1 复刻；MC 位图字体与 Web 字体的渲染差异记为已知偏离豁免。实机截图并排比对验收 |
| U5 | 1:1 对齐参考基准（2026-10-01 ask 确认） | **克隆的 v3.2.2 源码**（`build/_snowstorm_src`），不以 snowstorm.app 在线新版为准 |
| U6 | 1:1 对齐范围（2026-10-01 ask 确认） | **全编辑器所有界面**：主界面骨架 + 9 subject tab 表单 + 曲线/渐变/事件/贴图编辑器 + QuickSetup/CodeViewer/对话框/菜单/页脚/舞台 |
| U7 | portrait 模式（2026-10-01 ask 确认） | **只复刻横向模式**：原版 body 宽 100~720px 时切换 portrait 纵向布局（header 124px + 底部 38px Config/Code/Help/Preview 选择器，App.vue:74），不移植；记入保留偏离。验证截图统一用 ≥720 GUI 宽度 |

## 2. 架构决策

### D1 双层模块划分

- **`io.github.tt432.eyelib.snowstorm`（顶层新模块，零 MC 依赖，同 wintersky 先例）**：
  Snowstorm 数据层 as-is 移植。只依赖 `eyelib.wintersky`（Config/Emitter/Scene）与 Gson/jspecify。
  包内结构镜像 Snowstorm 文件：`input/`（Input、InputStructure=Data）、`curve/`、`gradient/`、
  `event/`（EventList 模型、EventSubpart、EventSubEffects）、`texture/`（TextureEdit 纯栅格）、
  `io/`（Export、Import）、`editor/`（EditorRuntime=emitter.js 装配逻辑、VariablePlaceholders、
  Options、Edits）、`util/`。
- **`io.github.tt432.eyelib.client.gui.snowstorm`（MC 侧 UI 适配）**：LDLib2 界面、3D 舞台、
  纹理桥接、文件对话框。遵循 ADR-0002：MC API 只出现在 bridge/client 侧；
  ldlib2 import 限本包（同 nodegraph 编辑器 D7 先例）。

### D2 UI 栈：LDLib2

表单密集型编辑器（~80 个输入控件、tab、折叠组、拖拽排序、取色器）用 retained-mode 工具包实现；
eyelib.ui 手写 canvas 控件不承载此规模。LDLib2 保持**可选前置**语义：缺失时管理界面粒子编辑器
入口置灰/隐藏（同 nodegraph 编辑器门槛）。26.1 版本差异经 `//?` 守卫收敛在本包。

### D3 数据流（as-is 语义）

```
UI Input 变更 → Input.value setter → Config.set(id, value)（wintersky 扁平 key-value）
              → updatePreview（Emitter.config 已关联，下一 tick 生效；结构变化时重启发射器）
              → registerEdit（typing 600ms 合并，as-is）
导出：generateFile() 从 Config + curves + events 重建基岩 JSON（format_version 1.10.0）
导入：Config.setFromJSON(json) → updateInputsFromConfig 回填 Input（molang 多行展开）
```

关键 as-is 行为（移植必须保留）：select 经 meta_value 反查 key；event_timeline 维护
`timeline[{uuid,time,event[]}]`；bezier_chain 导出时 nodes 对象化 time 键、slope 拆左右；
static 颜色 hex→rgb 数组、gradient 导出 `{time:#AARRGGBB}`、expression clamp01；
pre_effect_expression 导出补分号；unsupported_fields 捕获。

### D4 预览舞台（游戏内 3D 视口）

- 编辑器 Screen 内嵌舞台区域（左侧约 60% 宽），独立透视投影（FOV 45，对应 Snowstorm
  PerspectiveCamera45）+ 轨道相机（旋转/缩放/平移，对应 OrbitControls target=(0,0.8,0)、
  zoomSpeed=1.4）。OrbitControls 数学 as-is 移植（球坐标+阻尼关闭）。
- 舞台内容：GridHelper 64 格网格、CustomAxesHelper 坐标轴、minecraft_block 参考方块
  （纹理 `assets/minecraft_block.png` 移植进 eyelib resources），可见性由 Options 控制
  （localStorage→eyelib 配置文件持久化）。
- 粒子渲染：复用 `WinterskyParticleRenderer`/`WinterskyRenderTypes`，舞台相机四元数喂给
  `Scene.updateFacingRotation`。**tick 节流 as-is（Preview.vue:253-262 实证）**：每 rAF 帧检查
  `timestamp - last_frame_time > 32` 才调 `Emitter.tick()`（无参数，wintersky 内部固定步长
  1/tick_rate）——即 tick 频率上限 ~30Hz 与 rAF 解耦；渲染每帧都做
  （controls.update→updateFacingRotation→render）。MC 侧：Screen 渲染帧 = rAF 帧，
  用系统纳秒戳实现同一 32ms 门。
  **实证参数（Preview.vue 2026-09-29 全读）**：camera=PerspectiveCamera(45, 16/9, 0.1, 3000)
  初始位置 (-6,3,-6)；OrbitControls target=(0,0.8,0)、screenSpacePanning=true、zoomSpeed=1.4；
  GridHelper(64,64) 下沉 y-=0.0005；CustomAxesHelper(1)；minecraft_block Mesh 位于 (0,-0.51,0)；
  渲染循环：controls.update()→Scene.updateFacingRotation(camera)→render；`View.placeholder_variables`
  挂在 View 上（页脚 placeholder 栏数据源）；View.screenshot=renderer 截图（游戏内改 Screenshot.grab）。
- 页脚控制条 as-is：loop_mode/parent_mode 下拉、ground_collision 开关、placeholder 栏（# 键）、
  警告计数、粒子数/FPS、placeholder bake 对话框。键盘（Preview.vue:288-297 实证）：**空格=重新开始
  （startAnimation=Emitter restart），Ctrl+空格=暂停切换**；焦点在输入框时快捷键不生效。

### D5 贴图编辑器（U3）

- domain 侧 `TextureEdit`：纯 `int[]` ABGR 栅格 + 工具逻辑 as-is（brush/eraser 插值连线、
  shift 直线、flood fill、pickColor、history undo/redo、createEmpty/save/reload），默认 16×16。
- MC 侧桥：栅格 → NativeImage → DynamicTexture 注册进 AddonTextureRegistry；
  `wintersky` 侧 `Scene.fetchTexture` 钩子已支持运行时注入（wu7 接缝）。
- UI as-is：工具栏 5 工具 + undo/redo + 颜色预览 + 取色 overlay；viewport 滚轮缩放/右键平移；
  UV 框拖拽（周界移动+采样+尺寸手柄）；动画帧步进；信息栏；新建对话框。

### D6 导入/导出/文件

- 导出：generateFile as-is → 写入 `run/snowstorm_exports/<identifier>.particle.json` + 复制到剪贴板
  （Screen 环境无浏览器下载）。
- 导入：FileDialogService 选文件 + `Screen.onFilesDrop` 拖拽双通道（C3 已验证存在，见 §6）；
  VSCode 消息通道不适用，剔除。
- 子效果（EventSubEffects）：`fetchParticleFile` 钩子接子效果注册表（编辑器内闭环，as-is）；
  子效果编辑用编辑器 Screen 堆叠新实例（对应 Snowstorm 新标签页）。

### D7 警告与校验

`validate()` 3 条规则 as-is：不透明材质用 alpha / direction 无速度 / steady 速率低于寿命。
WarningDialog 以 LDLib2 弹层实现。

### D8 验证策略（三层）

1. **Node golden oracle**（同 wintersky 先例）：Snowstorm 数据层模块在 Node 以最小 stub
   （localStorage/窗口）直接运行，导出/导入/曲线/渐变/贴图操作生成 golden JSON 冻结于
   `src/test/resources/snowstorm/`；Java 移植逐值比对。脚本归 `scripts/snowstorm-oracle/`。
2. **ldlib2-uitest in-client 场景**：编辑器打开、tab 切换、Input 编辑→Config 断言、
   QuickSetup 预设应用、placeholder bake。复用 `-PldTest` 框架（26.1 无框架，只跑 1.20.1/1.21.1）。
3. **实机截图**（mcmcp）：编辑器全屏、舞台渲染、贴图编辑器、与 Snowstorm 网页版并排比对。

## 3. 新增文件规划

### domain（`io.github.tt432.eyelib.snowstorm`，零 MC）

snowstorm/
  input/Input.java  InputStructure.java  InputType.java（枚举: molang/text/number/checkbox/select/
        select_custom/color/gradient/image/event_list/event_timeline/event_speed_list）
  curve/Curve.java  CurveMode.java
  gradient/Gradient.java  GradientStop.java
  event/EditorEvent.java  EventSubpart.java  EventSubEffects.java  EventTimeline.java
  texture/TextureEdit.java  TextureHistory.java  RasterCanvas.java（int[] ABGR）
  io/SnowstormExport.java  SnowstormImport.java
  editor/SnowstormEditorRuntime.java（emitter.js 装配）  VariablePlaceholders.java
        EditorOptions.java  EditHistory.java  QuickSetupPresets.java
  util/SnowstormUtil.java（bbuid/guid/clamp/roundTo/snapToValues/compileJSON/lineify）
```

### client（`io.github.tt432.eyelib.client.gui.snowstorm`，LDLib2）

```
client/gui/snowstorm/
  SnowstormEditorScreen.java（ModularUI 宿主，管理界面入口）
  layout/SidebarView.java  SubjectTabView.java  InputGroupView.java
  inputs/（MolangInput/NumberInput/ColorInput/GradientInput/ImageInput/EventListInput/
         EventTimelineInput 等 12 种控件视图）
  stage/ParticleStageView.java（3D 舞台）  OrbitCamera.java  StageFooterBar.java
  curve/CurveEditorView.java  gradient/GradientEditorView.java
  texture/TextureEditorView.java  TextureBridge.java
  menu/EditorMenuBar.java  CodeViewerView.java  ExpressionBarView.java  WarningDialogView.java
  quicksetup/QuickSetupView.java
```

### 资源/脚本

- `src/main/resources/snowstorm/assets/`：Snowstorm 9 张 PNG + `examples/` 9 个 JSON（GPL 资产随源移植，
  保留许可声明）。
- `scripts/snowstorm-oracle/`：Node golden 生成脚本。
- 测试：`src/test/java/io/github/tt432/eyelib/snowstorm/**` + `src/test/resources/snowstorm/`。

## 4. 规格（前置/后置/不变量/异常/副作用）

- **前置**：LDLib2 存在（缺失→入口关闭，不崩）；wintersky 模块已注册；玩家在世界内或主菜单均可打开
  （舞台脱离世界，U2）。
- **后置**：导出文件字节级符合基岩 particle schema v1.10.0（oracle 验证）；编辑器所有 Input 变更
  实时反映到预览（下一 tick）。
- **不变量**：snowstorm 包零 MC 依赖（加入 ArchUnit 白名单守卫，同 wintersky）；
  wintersky 包不被 snowstorm 修改；编辑器关闭时发射器/纹理/监听全部释放（无泄漏到世界渲染）。
- **异常**：导入 JSON 不合法 → WarningDialog 提示 + unsupported_fields 捕获（as-is），不抛穿 Screen；
  贴图缺失 → wintersky missing 占位图（既有行为）。
- **副作用**：导出写 `run/snowstorm_exports/`；EditorOptions 持久化到 eyelib 配置文件；
  子效果注册表为 Screen 会话级，关闭即弃。

## 5. 分期

| 期 | 内容 | 验收 |
|---|---|---|
| P1 | domain 数据层：util/input/curve/gradient/event/export/import + Node oracle | oracle 全绿 |
| P2 | TextureEdit 纯栅格 + 单测 | 工具操作 golden 比对 |
| P3 | LDLib2 骨架：Screen+Sidebar+tab+12 种 Input 视图+ExpressionBar+MenuBar | ldlib2-uitest 场景绿 |
| P4 | 3D 舞台：轨道相机+网格/轴/参考方块+粒子渲染+页脚 | 实机截图比对 |
| P5 | 贴图编辑器 UI + 渐变/曲线编辑器 UI + CodeViewer + QuickSetup + 警告弹层 | 实机截图 + uitest |
| P6 | 导入/导出文件、子效果堆叠编辑、管理界面入口、文档/ModulesMd/提交 | 门禁全绿 |

## 6. 风险与核对点

- R1 LDLib2 版本差（1.20.1 fork.6 vs 26.1.2.33 API 漂移）→ 全部 `import com.lowdragmc.*` 限
  `client/gui/snowstorm`，版本守卫复制 nodegraph 编辑器先例；26.1 无 uitest 框架，测试只跑两个旧版。
- R2 舞台内透视投影与 MC GUI ortho 的矩阵切换 → 参考 GuiRenderEntity/Inventory 实体渲染先例；
  核对点 C1：RenderSystem projection matrix 在 Screen 渲染中的压栈/恢复路径。
  **实证约束（NodeAssetPreview javadoc，2026-08 实机）**：LDLib2/GUI 上下文中
  `guiGraphics.bufferSource` 批渲染（含 entitySolid）零像素，唯一实证可用路径是
  Tesselator + position_tex 直接 `drawWithShader`（同 `GuiGraphics.innerBlit`）。
  → P4 舞台粒子渲染不能直接复用 WinterskyRenderHooks 的 BufferSource 批次路径，
  须走立即模式 drawWithShader；顶点生成逻辑（wintersky quad/clr/uv）可复用，
  提交机制重写。26.1 GUI 渲染路径未迁移（`//? if <26.1` 先例），P4 仅在 1.20.1/1.21.1 验收。
  **R2 修正（2026-10-01 实机，三条舞台渲染硬结论）**：
  1. ~~禁止 enableScissor~~ **已证伪**：scissor 用设备像素坐标（`x*fbW/guiW`，y 自底向上翻转）
     即正确；不裁剪时网格经 NDC→舞台映射仍溢出全屏（实证），必须裁剪。
  2. **GL_LINES 立即模式在 GUI 上下文零像素**（quad 同管线正常；2026-09-30 截图即无网格，
     当时误判「已对齐」）→ 网格/坐标轴改细 quad 条（半径 0.01 世界单位 ≈ 1px@默认距离）。
  3. **必须每帧清深度**（three.js autoClear 语义）：共享 MC framebuffer 时世界/GUI 残留深度
     让舞台内容 LEQUAL 全灭——2026-09-30 的 fire 粒子截图其实只有天空深度区通过，
     「舞台已渲染」是深度残留造成的幸存者偏差。
- R3 Snowstorm 数据层对 Vue 响应式的隐性依赖（Input.value setter 触发 UI 刷新）→ 移植时以显式
  listener 替代，oracle 只覆盖数据语义不覆盖 UI 刷新。
- R4 贴图编辑器 flood fill/插值连线精度 → 纯 int[] 实现与 canvas 2D 语义差（抗锯齿）：
  Snowstorm brush 依赖 canvas 抗锯齿，Java 侧 as-is 到「硬边像素」语义并记录偏离；**核对点 C2**。
- R5 工程规模（~200KB 源，UI 占比大）→ 分期切片 + 子代理并行，每期独立验收。
- C3 ~~（核对点）~~ **已验证（2026-09-29，forge-1.20.1-47.1.3-sources.jar Screen.java:489）**：
  1.20.1 `Screen.onFilesDrop(List<Path>)` 存在，导入支持拖拽+对话框双通道。

## 7. 实施实况（2026-09-29，随实施回填）

**已落地（三版本编译粒度验收，逐切片报告在 git 历史）**：

- domain 数据层（`io.github.tt432.eyelib.snowstorm`）：util/input(81 输入)/curve/gradient/event/texture/
  io(export/import)/editor(EditorRuntime/Options/EditHistory/Placeholders/MolangData/QuickSetupPresets/
  Validator) 全量 as-is 移植；Node golden oracle 26 用例冻结（scripts/snowstorm-oracle/，
  loader.mjs+hooks.mjs stub 方案见 NOTES.md），JUnit `SnowstormOracleTest` 7/7 绿；
  ArchUnit DOMAIN_CLASSES 白名单已含 snowstorm。
- UI（`client.gui.snowstorm`，LDLib2）：Screen 骨架/Sidebar/12 种 Input 视图（inputs/）/
  ExpressionBar+MenuBar+CodeViewer（bar/+menu/）/曲线编辑器（curve/）/渐变编辑器（gradient/）/
  贴图编辑器（texture/）/事件控件（events/）/3D 舞台（stage/：OrbitCamera oracle ≤1e-9 对齐 three
  OrbitControls、立即模式 drawWithShader 渲染路径、页脚）/QuickSetup+弹层（quicksetup/+dialog/）/
  文件 IO（io/：导出落盘+剪贴板、对话框+onFilesDrop 导入、子效果快照栈）。
- 管理界面第 9 项「粒子编辑器」入口（EyelibManagerScreen，经 SnowstormEditorGate 反射门控）。

**实施期发现与修复**：

- **wu7 遗留 26.1.2 回归（本任务暴露）**：wu7 的 bridge/particle/adapter 四文件直接 import
  `net.minecraft.resources.ResourceLocation`（26.1 已改名 Identifier）且无版本守卫——wu7 门禁只跑了
  1.20.1。修复：四文件内部统一 PortResourceLocation，MC 边界经 ResourceLocationBridge.toMc/fromMc；
  WinterskyRenderTypes 26.1 分支从 vanilla RenderType 静态工厂（26.1 不存在）改为 RenderSetup.builder
  +RenderPipelines 基座（BrRenderTypeFactory 先例）；WinterskySceneManager 注册表 get 的
  Optional<Reference> 差异加守卫。
- jspecify TYPE_USE 注解误用于限定嵌套类型（`@Nullable java.util.function.Consumer`）会致 javac
  报错并**级联杀死 Lombok AP**（全项目数百个幻影错误）——两度实证（SidebarView、EventSubpartView）。
  正确形式：`java.util.function.@Nullable Consumer`。
- edit 工具行号漂移多次造成中途态破损（StageFooterBar/CodeViewerView/TextureBridge/GradientEditorView
  字段吞行），全部由各切片 owner 修复并复验；主代理手术编辑同受影响，修复后均经编译复验。

**验证结果（2026-09-30 实机）**：

- 门禁：三版本 compileJava、`:1.20.1:test`（1721 全绿）、`nullawayMain`（78→0 修复后零错误）。
- in-client：`snowstorm_editor` uitest 场景 PASS（结构断言 + fire 预设加载断言）。
- 实机截图验证（mcmcp）：编辑器布局、fire 预设火焰粒子舞台渲染、variables/texture/events
  tab、Code tab（fire JSON）、管理界面「粒子编辑器」入口。
- 实机验证暴露并修复：Selector null 候选 NPE（StageFooterBar/SelectInputView 双守卫）、
  OrbitCamera.update() 漏调（舞台恒等旋转，参考方块巨大偏移实证）、CodeViewer 入 tab 不刷新
  （v-if 等价 onAdded 刷新）、TextureSourceCodec 未安装（open 装 TextureBridge.install()）、
  内置纹理未进 assets/ 路径（5 张 PNG 复制到 assets/eyelib/wintersky/textures/）、
  导入/预设后 Sidebar 不刷新（notifyChanged 接线）。
- 工具坑：`-PldTest` 会把 uitest 系统属性烘进 clientRunVmArgs.txt（createClientLaunchScript
  生成），导致后续 mcmcp 启动自动跑 uitest 并退出——mcmcp_build 重生成即恢复。

**二期（2026-10-01：拖入 + 视觉对齐）**：

- 拖入文件：移植 yessteveskill client/dnd OLE 拖放基础设施（bridge/client/dnd/adapter/，
  Windows IDropTarget 接管 GLFW 窗口，hover 状态 + 落点坐标；其它平台退回 MC 原生
  onFilesDrop）；编辑器拖入悬停遮罩 + onFilesDrop → importFromDroppedFiles（实机验证
  identifier 切换生效）。
- 视觉对齐：布局修正为 App.vue 实证结构（grid "sidebar header"/"sidebar preview"——
  sidebar 左置全高，此前误置右侧）；kit 组件库（client/gui/snowstorm/kit/：SsMetrics/
  SsButton/SsTextField/SsIcon/SsIconButton/SsSlider/SsGroupSection/SsListAddRow/SsJsonColors）；
  59 个 lucide 图标光栅化进 assets/eyelib/snowstorm/icons/（scripts/snowstorm-icons/README.md
  管线记录）+ SNOWSTORM logo（Logo.vue SVG 光栅化）；全界面文字占位符退役。
- 实机暴露并修复：LDLib2 Button 默认 translation 'Button' 文本覆盖（tab/sprite 格需
  setText(Component.empty())）。
- 实机暴露并修复（2026-10-02 贴图编辑器色板）：ScrollerView 深层子树内某元素经
  style/buttonStyle/drawBackgroundAdditional 提交的纹理批次均不落屏（绘制每帧被调用、
  不在裁剪区外、剪刀包含绘制区；同帧兄弟元素正常），纹理提交后显式 `graphics.flush()`
  稳定修复（SsColorSwatch，根因未明，flush 为实证最小修复）。排查教训：mcmcp /eval
  在渲染线程执行，`Thread.sleep` 会冻结当次渲染——跨 eval 读帧计数器才有效，同 eval 内
  sleep+读数恒为 0（曾误判为"LDLib2 离屏缓存"）。
- 实机暴露并修复（2026-10-02 舞台，用户报告"画面反了+无法拖动"）：
  ① NDC→舞台映射 sy 误取负——render3D 整体替换了投影矩阵，输出走 GL 约定
  （NDC y+ = 窗口顶），MC 的 GUI y-down 正交矩阵已被替换不参与，sy 必须为正；
  负值致整个世界 Y 翻转（粒子弧倒扣网格下方、参考方块/绿色 Y 轴没入网格；
  XZ 平面对称掩盖了症状）。② 舞台拖动双断点：onMouseDown 在外层
  ParticleStageView 上调 isHover()（悬停元素是最深层 StageCanvas，恒 false），
  且 startDrag 以 this 为 dragSource（DRAG_SOURCE_UPDATE 只派发到 dragSource
  及其祖先路径，子级画布监听器收不到）；改为以 event.currentElement 为 source。
  左拖旋转/滚轮缩放/右拖平移全部实机复验（相机位置随操作变化）。③ 排查方法：
  真实输入注入——`glfwSetCursorPos` 产生真实光标回调（悬停/拖拽路径全走通），
  `MouseHandler.onPress/onScroll`（private，反射）产生真实按键事件；优于合成
  `screen.mouseClicked`（后者 hover 用的是真实光标位置，坐标对不上恒 miss）。
  另：rainbow 数值对照（原版 window.Emitter vs EditorRuntime.Emitter 同龄采样）
  证实模拟层完全一致（发射点 cos/sin·90°/s 圆轨迹吻合），差异纯在渲染映射。
- 实机暴露并修复（2026-10-02 贴图编辑器画布空白，两个断点）：
  ① `decodeExternal` 对 bedrock 风格无命名空间路径（basic_render_parameters.texture
  ="textures/particle/particles"）恒走 eyelib 命名空间且不补扩展名——ResourceManager
  按物理文件查找，vanilla 图集永不命中；修复为 minecraft → eyelib 候选 + 无扩展名补
  ".png"。② 外部 decode 只填充领域层 RasterCanvas，GPU 侧常驻 DynamicTexture 不更新
  （NativeImage 停 16×16 初值）——解码成功即 ensureImage 写像素 + upload。
  复验：vanilla 粒子图集 128×128 完整显示，UV 框定位正确。
