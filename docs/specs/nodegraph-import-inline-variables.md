# 导入期变量内联

> 状态：已实现并实机验证（2026-08-06）。
> 入口：导入对话框「导入时内联可内联的变量」复选框（会话级状态，默认关）。

## 1. 需求

从 JSON 导入的图里， Bedrock 包常见的「写一次、读一次」临时变量模式
（`temp.t0 = <expr>; … temp.t0 …`）会产生 set/get 节点对 + variable 节点，
图面噪音大。勾选复选框后，导入时把**可内联**的变量值直接接到读取点，
删除 set/get 节点。

## 2. 可内联条件（全部满足；任一不满足即保留原样，宁漏勿错）

1. **恰好一次写入、恰好一次读取**（同图内）：
   - variable 形态：`variable` 节点 V 的出边中，恰一条接 `exec.set_var.target`（写），
     恰一条接非 target 端口（读）；
   - temp 形态：全图恰一个同名 `exec.set_temp`，恰一个同名 `temp.get`，
     且 get 恰一条出边；
2. **先写后读**：读取点的 exec 上下文（值链下游首个 exec 节点，必须唯一）
   在 exec 流上可从 set 到达；
3. **不跨循环边界**：set 与读取上下文同处 `exec.loop`/`exec.for_each` 的 body
   链内或同处链外；
4. **无文本引用**：变量名（`variable.x`/`v.x`/`temp.x`/`t.x` 四种写法，词边界匹配）
   不出现在任何行内 molang 常量中（文本引用图结构不可见）；
5. variable 名在同图无第二个 variable 节点（绑定无歧义）。

## 3. 不变量与语义

- 值子图在本节点系统的值侧是纯表达式（副作用只走 exec 链），单读场景下移动
  求值位置不改变结果；
- **variable.* 的写是实体可观测副作用，内联即移除该副作用**——这是复选框
  默认关、需显式勾选的原因；
- set 节点删除时 exec 链旁路（上游 × 下游重连），语句顺序不变；
- 写入值未连线时，内联为消费端口的端口默认值常量（0）；
- 嵌套候选（`set a = f(temp.b)`）由不动点迭代处理：每趟只内联一个候选并重算
  资格，收敛（每趟净删 2 节点），上限 256 趟防御。

## 4. 实现位置

- `decompile/VariableInliner`：纯图变换（节点/连线列表 → 变换后列表 + 诊断），
  不依赖 MC；
- `ImportGraphBuilder.build`：**布局前**应用（删除的节点不占位）；
- 旗标链路：ImportDialogs 复选框 → `ImportClosure.importWithClosure(…, flag)` /
  `JsonGraphImporters.import{ClientEntity,RenderController,AnimationControllers}(…, flag)`
  ——实体闭包（含 AC 库）与 RC/AC 直接导入全覆盖；旧签名委托 `false`。

## 5. 验证

- 单测 `VariableInlinerTest` 9 例：两种形态内联、exec 旁路、负例（多读/先读后写/
  文本引用/循环边界/多写）、未连线值默认常量、importer 端到端旗标；
- 实机：勾选复选框导入悦灵闭包，诊断含 INLINED_VARIABLE 计数，图面无单写单读
  set/get 对。
