# ADR-0029：执行时机事件化（Event 节点）

## 状态

已接受（2026-08-15）。

## 背景

ADR-0021 起的执行槽是「反向汇入槽」模型：entity.root 的
initialize/pre_animation/parent_setup 与 ac.state 的 on_entry/on_exit 都是
EXEC IN 端口，槽口接链尾，codegen 沿 exec_in 反向解析。用户指出：initialize、
pre_animation 这类字段**天然是 event**——它们指示执行时机，应建模为
UE 蓝图式的事件源节点（`{}` Event），而不是 root 上的输入槽。同时要求
AC 的 on_entry/on_exit 改成 OUT 执行端口，方向即语义。

## 决策

1. **event.* 源节点**：新增 event.initialize / event.pre_animation /
   event.parent_setup（Kind=EVENT，category=event，仅 exec_out 输出）；
   entity.root 的三个 EXEC IN 槽撤除。
2. **ac.state 端口翻转**：on_entry/on_exit 改为 EXEC OUT（id 不变）——
   状态固有时机不独立成节点，方向翻转即可。
3. **同类 event 唯一**：每张主图每类 event 节点至多 1 个（验证器
   EVENT_DUPLICATE）；event 节点仅允许出现在 CLIENT_ENTITY 主图
   （EVENT_GRAPH_KIND）。
4. **可达性模型扩展**：验证器的锚点分析从「根锚点反向」扩展为
   「根锚点反向 ∪ 时机源锚点（event.*、ac.state）正向」。
5. **codegen 源端口发射**：新增 emitStatementListFrom（EXEC OUT → 正向走链）；
   汇入槽公开入口删除（loop body、subgraph.output.exec_in 仍内部走反向）。
6. **format_version 12→13**：链式迁移，旧槽线改写为 源 → 链首 exec_in，
   导出产物逐字节等价。

## 理由

- 时机即源：事件节点把「什么时候执行」从 root 的槽名单提升为画布上的
  显式实体，符合蓝图心智；链的方向（从时机流向语句）与数据流方向一致，
  消除「槽口接链尾」的认知反转。
- ac.state 的翻转同理：on_entry/on_exit 的语义是「进入/退出此状态时
  触发」，从 state 出发的 OUT 端口直接表达。
- 链内部拓扑不变，迁移是纯端点改写，产物等价性可证明（悦灵实测对照）。

## 后果

- 好处：执行时机显式可见；孤儿链检查顺带修复了 ac.state 误报
  （旧模型下 state 的 EXEC 链无法从 ac.root 反向可达）。
- 代价：format v13 迁移；event 节点缺席即槽缺省（不再有「槽在但未连线」
  的中间态，语义更干脆）。
- 边界：loop/for_each body、subgraph.output.exec_in 保持汇入槽——
  它们是「嵌套作用域」不是「时机」，模型差异是有意的。
