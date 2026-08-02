# 规格：节点图工作台（Nodegraph Workbench）

状态：已批准（2026-08-03 用户拍板）。关联：ADR-0021、nodegraph-visual-molang.md、json-view-molang-debug.md。

**用户决策（2026-08-03）**：
- 导入 = **反编译为可编辑图**（非仅查看）。
- 调试 = **画布节点值徽标 + 调试面板**（UE 蓝图式）。
- 此前交付的独立 JsonViewScreen / MolangDebugScreen **删除**（能力收编进工作台；
  service 层 EntityJsonService / MolangDebugService 保留共享）。
- AC（动画控制器）导入：运行时注册表不保留 BrAnimationControllerSet（装载后即拆分），
  故 AC 导入来源为 **JSON 文件/文本**（CE/RC 可按注册 id 导入）。

## 1. 目标

把节点图编辑器升级为**统一工作台**：一个界面内完成
**参考（JSON 资产）→ 编辑（图）→ 构建（ClientEntity）→ 调试（live molang）** 闭环。
对应 UE 蓝图编辑器：Content Browser（资产检查器）、Graph（画布）、Debugger（调试器）。

此前的独立 JsonViewScreen / MolangDebugScreen ~~保留为无 LDLib 环境的兜底~~
**已按用户决策删除**；service 层（EntityJsonService / MolangDebugService）保留并被
工作台与（潜在的）其他消费端共享。

## 2. 功能构成

### W1. 资产检查器（编辑器内嵌侧栏）

- 分类浏览 ClientEntity / 行为实体 / RC / AC / 动画 / 模型 / 纹理（各 Manager），
  选中显示 JSON（复用 EntityJsonService；语法着色渲染从 JsonTextPanel 抽为可复用组件）。
- REF 节点（ref.geometry/texture/material/animation/ac/rc）快捷跳转对应资产定义；
  模型/纹理沿用 NodeAssetPreview 预览。

### W2. JSON → 图 导入（反编译）

入口：编辑器工具栏「导入」→ 选已注册 id（ClientEntity/RC/AC）→ 生成新图库。

**结构镜像**（ClientEntityAssembler / RcAssembler / AcAssembler 的逆）：
JSON 字段 → assembly 根节点 + ref.* 节点 + animate/rc 条目 + scripts。

**molang 反编译**（核心，domain 纯函数）：
molang 字符串 → MolangAst（现有手写解析器）→ 图 IR：

| molang AST | 图节点 |
|---|---|
| 数字/字符串/bool 字面量 | const.number / const.string / const.bool（整数 → const.int） |
| `a op b` / `op a` / `c ? a : b` / `a ?? b` | op.binary / op.unary / op.ternary / op.null_coalesce |
| `variable.x` / `temp.x` / `context.x` | var.get / temp.get / **context.get（新增节点）** |
| `query.f(...)` / `math.f(...)` | query.call / math.call（含 arg_count 与参数连线） |
| `variable.x = e;` | exec.set_var |
| `loop(n, {...})` / `for_each(v, arr, {...})` | exec.loop / exec.for_each |
| `break/continue/return` | exec.break / exec.continue / exec.return |
| arrow `a->b`、下标 `a[i]`、member access | **不支持** → UNSUPPORTED_IMPORT 诊断 + sticky note 原文 + 占位 const |

- **context.get 新增节点**：补齐图语言表达力（现状 var.get 经 withRoot 会把
  `context.x` 错拼成 `variable.context.x`）。
- **布局**：分层布局（到汇最长路径定 x 层，DFS 序定 y），坐标遵循编辑器现有约定。
- **往返性质**（写入规格的明确承诺）：
  - `build(import(json))`：语义等价（molang 文本可能重排/全括号化；数值格式归一）。
  - `import(build(graph))`：结构等价（布局坐标与 temp 命名不保证还原）。
- 行为实体 JSON：**不作为图导入**（behavior components/events 超出图语言表达域），
  仅在 W1 检查器查看。

### W3. 图内 Molang 调试（编辑器内嵌）

- **调试面板**（右侧可折叠侧栏）：目标实体选择（MolangDebugService.listTargets）、
  watch 表达式列表、scope 变量表。
- **节点值徽标（UE 蓝图式画布调试）**：
  - 来源：`MolangGenerator.emitExpressionFor(graph, nodeUid, port)` 按值输出端口逐槽发射。
  - 缓存：图内容哈希变化才重新发射；每帧只做 evaluate（编译缓存去重）。
  - 显示：类型着色 + `MolangType.format`；求值错误红字；无目标/无 scope 显示「—」。
  - 上限：节点数 > N（默认 64）时仅渲染可见区域徽标。
  - 子图：v1 潜入子图时子图内节点不求值（temp.sg 隔离语义），显示「子图」标记；
    主图节点正常求值。

## 3. 版本策略

- 重逻辑全部版本无关共享：decompiler（domain）、importer（domain）、
  MolangGenerator per-slot 发射（已有）、EntityJsonService / MolangDebugService（已有）。
- 两版编辑器各做薄 UI 层：
  - ldlib1（1.20.1）：EditorRoot 工具栏加按钮 + WidgetGroup 侧栏 + 徽标 overlay 绘制。
  - ldlib2（1.21.1/26.1.2）：ModularUI 容器加侧栏 UIElement + 徽标 overlay。

## 4. 前置条件 / 后置条件 / 不变量

- 前置：编辑器已打开（LDLib 在场）；导入来源已注册于对应 Manager；调试目标在世界中。
- 后置：导入产生新 GraphLibrary（注册进 GraphLibraryManager，不落盘直到保存）；
  调试只读，不写 scope、不改实体。
- 不变量：反编译不改变既有 codegen/验证器行为；context.get 为纯增量；
  独立屏幕与工作台面板共享 service，行为一致。
- 异常：导入遇不支持结构 → 诊断 + 占位（不静默丢语义，不中断其余部分）；
  调试求值异常 → 徽标红字。

## 5. 验收

1. ~~导入 vanilla `minecraft:zombie`（ClientEntity）→ 图结构完整（root+refs+scripts），~~
   ~~未修改直接 build → 注入后实体渲染与原版一致（语义等价）。~~
   **已验证（2026-08-03，1.20.1 冒烟）**：导入 zombie→952 节点/862 连线/153 变量/24 便签，
   验证器 0 ERROR，构建注入成功，实体正常渲染（几何/纹理/粒子）。
2. ~~导入含 `context.*` 的表达式 → context.get 节点，build 回原文本（语义）。~~
   **已由 domain 单测覆盖**（MolangDecompilerTest 映射表黄金 + 往返语义）。
3. ~~导入含 arrow/下标的表达式 → UNSUPPORTED_IMPORT 诊断 + 原文便签，其余节点正常。~~
   **已由 domain 单测覆盖**；zombie 导入实产 24 张便签。
4. ~~画布节点值徽标：选定目标后 const/op 节点显示实时值；改动图后徽标随表达式重发射更新。~~
   **已验证（1.20.1 冒烟）**：选定 slime 目标后画布徽标实时值
   （'geometry.oreville_ans.slpata' / textures/... 路径 / 材质名），
   assembly-only refs 正确跳过，>64 节点截断提示显示。
5. ~~调试面板 watch/scope 表与独立屏幕行为一致。~~
   **已验证**（独立屏幕已删，侧栏=同一 MolangDebugService：目标选择→scope 表实时值）。
6. ~~三版本编译绿；domain 单测覆盖反编译映射表全行与往返语义。~~
   **已验证**：1.20.1/1.21.1/26.1.2 编译绿；1.20.1 全量测试绿（含 decompile 44 测试）。
