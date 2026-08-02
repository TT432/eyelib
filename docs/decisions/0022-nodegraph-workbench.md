# ADR-0022：节点图工作台——JSON 反编译导入与图内 Molang 调试

日期：2026-08-03　状态：已接受（用户拍板）　规格：docs/specs/nodegraph-workbench.md

## 背景

ADR-0021 交付了节点图编辑器（图 → molang → ClientEntity 正向管线）后，用户提出两点修正：
molang 调试必须**在节点图同一界面**（而非独立屏幕）；编辑器需要**导入 Entity/JSON 的按钮**
（而非另开查看器）。经确认三项决策：导入=反编译为可编辑图；调试=画布节点值徽标+面板
（UE 蓝图式）；此前交付的独立 JsonViewScreen/MolangDebugScreen 删除，能力收编工作台。

## 决策

### D1. 工作台单一界面

编辑器升级为工作台：画布 + 工具栏（导入/资产/调试）+ 两个可折叠侧栏（资产检查器、
调试面板）+ 画布徽标 overlay。参考/编辑/构建/调试在同一界面闭环。
独立屏幕删除；service 层（EntityJsonService/MolangDebugService）保留并被工作台共享。

### D2. JSON → 图 反编译器在 domain（nodegraph.decompile）

- 输入为**文件形态 JsonObject**（与 assembler 输出对称），domain 不依赖 importer 层 record；
  注册表 → JSON 的桥接在 client 侧（EntityJsonService，CODEC encode）。
- molang 反编译 = MolangAst（现有手写解析器）→ 图 IR 的机械映射（规格 §W2 表格）；
  不支持结构（arrow/下标/member access）→ UNSUPPORTED_IMPORT 诊断 + StickyNote 原文
  + 占位常量，**不静默丢语义、不中断其余部分**。
- 反编译补齐了图语言表达力缺口：新增 **context.get 节点**（var.get 经 withRoot 无法表达
  context.*）与 **emitNodeOutput codegen 入口**（徽标的生产者视角发射）。
- 往返承诺：build(import(json)) 语义等价；import(build(graph)) 结构等价（布局/temp 名不保证）。
- AC 导入来源为 JSON 文件/文本（运行时注册表不保留 BrAnimationControllerSet）；
  行为实体 JSON 不导入（超出图语言表达域），仅检查器查看。

### D3. 画布徽标走「生产者输出发射 + 共享模型」

- 徽标表达式来源是新增的 `MolangGenerator.emitNodeOutput`（与构建同一 codegen，
  语义一致性由构造保证，非另写求值路径）。
- `client.nodegraph.workbench.NodeDebugOverlayModel` 为版本无关共享模型：COW 引用比较
  触发重发射、每帧仅 evaluate（编译缓存去重）、上限 64 节点、
  跳过 subgraph.*（temp.sg 隔离语义）与 assembly-only refs（无 molang 语义）。
- 两版编辑器只做薄壳：ldlib1=EditorRoot 工具栏+WidgetGroup 侧栏+FreeGraphView 末层
  BadgeLayer（1/scale 反向缩放文字）；ldlib2=Ldlib2Workbench 容器（Taffy 布局）+
  canvas 顶层被动 overlay。着色/流程/命名两版逐值对齐（IRC 协商）。

### D4. 调试只读，渲染线程执行

目标实体 scope 经 RenderData 附件只读解析（owner 守卫收敛于
`RenderData.scopeIfOwnedBy`，IQF Q-4）；求值全在渲染线程（屏幕 render 路径），
与 /eval HTTP 线程的已知限制相区别（query.* 宿主查询可用）。

## 后果

- 正向（图→JSON）与反向（JSON→图）管线同域共存，assembler 与 decompiler 互逆约束
  由单测往返语义守护（44 个 decompile 测试）。
- context.get / emitNodeOutput 为纯增量，既有图与构建行为不变。
- 导入不自动落盘（编辑器未保存语义）；保存走既有 writeToDisk。
- 无 LDLib 环境失去查看/调试入口（用户接受；D7 门控语义不变：图仍可构建渲染）。

## 验证

- domain：decompile 44 测试绿（映射表/往返/诊断）；codegen emitNodeOutput 黄金串绿。
- 1.20.1 冒烟：UI 导入 minecraft:zombie→952 节点 0 ERROR→构建注入→实体正常渲染；
  调试侧栏选 slime→scope 表实时值；画布徽标 'geometry.oreville_ans.slpata' 等实时值
  （assembly refs 正确跳过，>64 截断提示）；资产检查器 JSON 着色显示。
- 三版本编译绿；1.20.1 全量测试绿（含架构门禁）。
