# 变量表面板与 molang 作用域（VariableDecl.scope）

> 状态：已实现。用户决策 2026-08-06：独立变量表面板；作用域 = molang 层 temp/variable；
> 声明默认值导出为初始化语句。
> 修订关系：扩展 nodegraph-visual-molang §2.3「变量」段——黑板变量从「恒 variable.* 根」
> 改为「按声明作用域选根」。

## 1. 需求

1. 编辑器提供**真正的变量表**：表格视图（名字/类型/作用域/默认值/引用数），支持
   自建变量、行内改型/改作用域/改默认值、删除、筛选——替代「黑板右键菜单 +
   检查器逐个展开」的管理方式（allay 级 123 个变量的库不可用）。
2. 变量带 **molang 作用域**：`VARIABLE`（实体级 `variable.*`，跨图跨文件可见）/
   `TEMP`（求值级 `temp.*`，仅当次求值内有效）。
3. 声明的**默认值要生效**：导出时生成初始化语句（Bedrock 无变量声明段，未初始化
   默认为 0）。

## 2. 设计

### 2.1 域模型（VariableDecl.scope）

- `VariableDecl` 新增第 5 分量 `scope: Scope`；`Scope = VARIABLE | TEMP`
  （序列化 `"variable"`/`"temp"`，codec `optionalFieldOf("scope", VARIABLE)`）。
- **兼容性**：v6 文件无 scope 字段 → 全部 VARIABLE，行为不变；属兼容扩展，
  不升 format 版本。4 分量构造保留（默认 VARIABLE），既有调用点零改动。
- `GraphVariableOps.rename/retype` 保留 scope；新增 `setScope`。

### 2.2 codegen：按声明选根

- `EmitSession` 的 `VARIABLE` 节点发射：按当前帧图（`Frame.graph`）的变量声明
  查 scope——`TEMP` 发 `temp.<name>`，`VARIABLE`/未声明发 `variable.<name>`
  （未声明维持现状 + 既有 UNDECLARED_VARIABLE 警告）。
- `exec.set_var` 的 target 走同一发射路径，写身份自动跟随作用域。
- 行内 molang 文本里的 `variable.x`/`temp.x` 是用户原文，不做改写。
- 子图帧查子图自己的声明表（黑板声明按图隔离，与编辑器翻译一致）。

### 2.3 导出初始化语句

- `ClientEntityAssembler.assembleScripts`：主图 `VARIABLE` 作用域且带默认值的声明，
  按声明序生成 `variable.<name> = <字面量>;`（`MolangLiterals.literal`），
  **前置**到 `scripts.initialize`：initialize 槽有连线则拼在用户语句之前
  （用户语句可覆盖初始化值），无连线但有初始化语句时也输出 initialize。
- `TEMP` 作用域默认值不导出（temp 当次求值即清，初始化无意义）。

### 2.4 编辑器

- **作用域侧表**：LDLib2 变量模型无作用域字段，eyelib 侧在
  `EvmGraph.LibraryContext` 持 `Map<UUID, VariableDecl.Scope>`（键 = 变量模型 uid）。
  加载时 `EvmGraphTranslator.populateVariables` 登记；保存翻译按 `var.getUid()`
  读回（缺失 = VARIABLE）。会话内改名 uid 不变，作用域跟随；保存重开后由域重建。
- **变量表面板**（`VariablesPanel`，工作台右侧标签页的「变量」页，默认活动页）:
  - 表头行：名字 | 类型 | 作用域 | 默认值 | 读 | 写；顶部「+ 新建」与筛选框。
  - 行内编辑：改名（`setName`，同内建检查器）、改型（直连
    `setDataTypeHandle`，同内建黑板属性面板语义）、改作用域（写侧表）、改默认值
    （initialization model `setValue`）。
  - 可声明类型收窄为 molang 值类型（FLOAT/INT/BOOL/STRING/OBJECT；
    2026-08-14 起 OBJECT 是独立 PortType，不再是 ANY 别名——见
    nodegraph-object-and-typed-ports.md §2.1；ANY 不可声明，未决用 UNKNOWN）——
    EXECUTION_FLOW/SLOT/VARIABLE 身份与 *_REF/COLOR 不是可存储的值
    （`EvmTypeHandles.allVariableDeclTypes`，黑板类型选择器同源）。
  - 读/写引用数（2026-08-10 起两列，闭包级）：写入边（→variable.in）计写、
    读取边（variable.out→）计读。当前潜入图取活模型（编辑即时反映）；
    本库其余图与本库 ref.ac 引用的 AC 图库取域快照按名统计——variable.* 是
    实体级作用域，AC 图里的读写与实体库同源（hizljo 实证：AC transition 的读
    在实体库内不可见，单库计数漏报）。
  - 面板跟随当前潜入图（每 tick 对比模型身份 + 变量数增量重建）。
  - 子图接口变量（INPUT/OUTPUT）在表内只读展示（接口编辑走既有子图机制），
    不显示作用域选择器。

### 2.5 验证器

- 新增 `TEMP_NEVER_WRITTEN`（warning）：TEMP 作用域变量在同图被读（variable 节点
  接值端口）但无任何 `exec.set_var` 写入——temp 跨求值不存活，读到恒 0。
- 写检测：`exec.set_var` 的 target 连线源为同名 variable 节点。死写（只写不读）
  不报警。

### 2.6 未选类型 = unknown

- `PortType.UNKNOWN`：变量声明的占位过渡态。codec 序列化 `"unknown"`；连线兼容
  矩阵按 ANY 放行（仅 COLOR 恒不互通）；`EvmTypeHandles` 双向映射 LDLib2 内建
  `TypeHandles.UNKNOWN`。
- 新建变量默认 unknown（用户显式选型前不猜测语义）；类型列显示编辑器类型名
  （float/int/bool/string/object/unknown），不显示 handle 原文。
- 验证器新增 `VARIABLE_TYPE_UNKNOWN`（warning）：声明类型为 UNKNOWN 的变量。

### 2.7 变量表操作 undo 与 blackboard 合并

- `EvmUndo`：图模型 NBT 快照 + 作用域侧表快照双份留底，undo/redo 同时恢复
  （LDLib2 内建 UndoableGraphCommand 只覆盖图 NBT，侧表是 eyelib 域概念）。
  改名/改型/改作用域/改默认值/删除全部接线；连续编辑（逐键）靠 HistoryStack 的
  `source` 合并为单条历史。新建走内建 dispatchCommand（本身可 undo）。
  26.1 序列化 API 改版（ValueOutput/ValueInput），按版本门控。
- blackboard 合并：LDLib2 内建黑板面板（GraphPanel）隐藏，变量 UI 统一为变量表。
  右侧为标签页结构（用户决策 2026-08-07）：标签条 [变量][调试] 常显于标签体之上
  （不随页显隐消失），点击互斥切换变量表/调试侧栏，活动标签半透明白底高亮；
  「+ 新建」/筛选属变量页，随页显隐。黑板能力
  无损失：创建/改名/删除/改型变量表全覆盖，分组是 LDLib2 会话态（域不往返）。

### 2.8 面板调宽与引用高亮

- 左缘 4px 拖拽手柄调宽（180..640，会话内保持）。
- 点击变量行 → 图上高亮该变量的全部引用节点（复用内建选择态：clearAllSelected
  + addSelected 绑定该声明的 VariableNodeModel）。

## 3. 前置条件 / 后置条件 / 不变量 / 异常行为

- 前置：图数据合法（既有验证器全绿不强制，warning 级不阻断）。
- 后置：TEMP 变量的全部读写发射为 `temp.*`；初始化语句进入导出 JSON 的
  `description.scripts.initialize`；变量表编辑保存后重开不丢（域往返）。
- 不变量：未声明变量行为不变（variable.* 根 + 警告）；旧格式文件产物逐字节等价；
  codegen 确定性（§2.4）不受影响。
- 异常行为：TEMP 变量跨求值读取是用户责任（规格明示 temp 语义；
  TEMP_NEVER_WRITTEN 只兜同图有读无写）。

## 4. 非目标

- 不做变量引用跳转/重命名重构（rename 已由声明引用自动同步变量节点）。
- 不做批量多选操作（v1 单行删除）。
- 不改 temp.get/temp.set 自由命名节点（不进黑板的既有路径不变）。

## 5. 验证

- 单测：codec scope 往返/缺省；codegen TEMP 根发射（读+写）；initialize 初始化
  合并（有/无用户 initialize 连线）；TEMP_NEVER_WRITTEN 触发与不触发；
  rename/retype 保 scope；VARIABLE_TYPE_UNKNOWN 触发与不触发；UNKNOWN 兼容矩阵
  与 codec 往返。
- 实机：变量表新建 TEMP 变量 → 连线读写 → 保存重开 → 导出 JSON：
  temp 读写为 `temp.*` 根；VARIABLE 默认值出现在 initialize。
  U2 批次实机：blackboard 隐藏 + 变量表默认展开；新建变量 unknown；删除后 undo
  恢复；行点击选中引用节点（数与引用数一致）；拖拽调宽 300→400；折叠隔帧生效。
