# 规格：执行时机事件节点（nodegraph-event-nodes，v13）

## 1. 背景与决策

v13 前，执行时机建模为「汇入槽」：entity.root 的 initialize/pre_animation/parent_setup
与 ac.state 的 on_entry/on_exit 都是 EXEC IN 端口，槽口接链尾，codegen 沿 exec_in
反向走到链首再正向发射。用户决策（2026-08-15）：执行时机应是**源**——
UE 蓝图式事件节点/输出端口，exec 链从时机锚点出发。

- entity.root 的三个 EXEC 槽撤除，改为 `event.initialize` / `event.pre_animation` /
  `event.parent_setup` 三种事件节点（category=event，无输入、无选项，仅 `exec_out`）。
- ac.state 的 on_entry/on_exit 端口方向翻转为 EXEC OUT（端口 id 不变）——
  它们是状态的固有时机，不独立成节点。
- 同类 event 节点每张主图至多 1 个（用户决策：语义最清晰，与 UE 蓝图一致）。

## 2. 图模型

### 2.1 节点类型

| 类型 id | Kind | 端口 | 语义 |
|---|---|---|---|
| event.initialize | EVENT | exec_out: EXEC OUT | scripts.initialize 执行时机 |
| event.pre_animation | EVENT | exec_out: EXEC OUT | scripts.pre_animation 执行时机 |
| event.parent_setup | EVENT | exec_out: EXEC OUT | scripts.parent_setup 执行时机 |

ac.state：`on_entry` / `on_exit` 为 EXEC OUT 端口。

### 2.2 链拓扑（不变量）

链内部拓扑不变：语句节点间 exec_out → exec_in 依序相连。
变化仅在端点：链首的 exec_in 接时机源（event 节点 exec_out / ac.state on_entry/on_exit），
链尾 exec_out 悬空（v13 前接槽口）。

### 2.3 合法性（验证器）

- `EVENT_GRAPH_KIND`（ERROR）：event.* 节点只能出现在 CLIENT_ENTITY 库的主图。
- `EVENT_DUPLICATE`（ERROR）：同类 event 节点每张主图至多 1 个。
- 可达性分析（检查 17 未连接输入 / 检查 18 孤儿链）= 根锚点反向可达 ∪
  时机源锚点（event.*、ac.state）正向可达；挂在时机源下的链不再报 ORPHAN_CHAIN，
  ac.state 自身不再是孤儿。
- event 节点 exec_out 未连线 = 该槽不输出（不是错误）。

## 3. 代码生成

新增源端口发射入口：`MolangGenerator.emitStatementListFrom(graphName, PortRef)`
（便捷形 `emitStatementListFromFor`）：从 EXEC OUT 端口正向走链发射语句序列；
空链产 "0"。汇入槽形入口 `emitStatementList(For)` 删除——loop/for_each 的 body
与 subgraph.output.exec_in 仍是 IN 槽，由会话内部 emitExecInput 处理，无公开入口。

## 4. 组装与导入

- ClientEntityAssembler：scripts.initialize/pre_animation/parent_setup = 主图中对应
  event 节点的 exec_out 链；节点缺席或未连线 → 跳过该槽。变量声明默认值仍前置于
  initialize（event 缺席但声明默认值非空时，initialize 仅由默认值组成）。
- AnimationControllerAssembler：on_entry/on_exit = state 同名 OUT 端口的出链。
- JsonGraphImporters：scripts 三槽 → event.* 节点 + 链；AC state 两槽 →
  state 源端口出链。
- InitDefaultFolder：折叠判据从「沿 exec_out 终末落在 entity.root.initialize」改为
  「沿 exec_in 反向，链首挂在 event.initialize」，其余条件与等价性论证不变。

## 5. 迁移（v12 → v13）

migrateV12ToV13：旧槽口连线（链尾 → entity.root.三槽 / ac.state.两槽）改写为
源端口 → 链首 exec_in；实体槽新建 event 节点（uid `event_<slot>`，置于链首左移 280）。
无连线的槽不产节点。链内部不动，导出产物逐字节等价。

## 6. 验证

- 单测：装配（ClientEntityAssemblerTest/AnimationControllerAssemblerTest）、
  codegen（MolangGeneratorTest 等）、验证器（EVENT_DUPLICATE/EVENT_GRAPH_KIND/
  ORPHAN_CHAIN 新形态）、迁移（GraphMigrationsTest v13 用例）。
- 实机：编辑器导入悦灵 → event 节点出现在画布；构建产物与迁移前等价。
