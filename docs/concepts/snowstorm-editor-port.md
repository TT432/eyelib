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
  `Scene.updateFacingRotation`；帧渲染驱动 `emitter.tick(dt)`，dt 节流规则 as-is（帧间隔
  >32ms 时按 Snowstorm Preview.vue 同规则处理）。
- 页脚控制条 as-is：loop_mode/parent_mode 下拉、ground_collision 开关、placeholder 栏（# 键）、
  play/pause（空格/Ctrl+空格）、警告计数、粒子数/FPS、placeholder bake 对话框。

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
- 导入：FileDialogService 选文件 + `Screen.onFilesDrop` 拖拽（**核对点 C3**：1.20.1 Screen 是否有
  onFilesDrop，无则仅对话框）；VSCode 消息通道不适用，剔除。
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

```
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
- R3 Snowstorm 数据层对 Vue 响应式的隐性依赖（Input.value setter 触发 UI 刷新）→ 移植时以显式
  listener 替代，oracle 只覆盖数据语义不覆盖 UI 刷新。
- R4 贴图编辑器 flood fill/插值连线精度 → 纯 int[] 实现与 canvas 2D 语义差（抗锯齿）：
  Snowstorm brush 依赖 canvas 抗锯齿，Java 侧 as-is 到「硬边像素」语义并记录偏离；**核对点 C2**。
- R5 工程规模（~200KB 源，UI 占比大）→ 分期切片 + 子代理并行，每期独立验收。
- C3（核对点）：1.20.1 `Screen.onFilesDrop` 存在性；不存在则导入仅走 FileDialogService。
