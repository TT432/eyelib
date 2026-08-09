# 导入期单用 const 常量自动内联

> 状态：已实现并实机验证（2026-08-07）。用户决策：「类似这种 const.int 也应该自动内联」
> （无条件，不加开关）。

## 1. 需求

导入产物中大量 `const.*` 节点只有单一用途（如 animate.entry 的 weight=1）。
这类常量用端口行内值表达等价且更干净：节点图少一层间接，与手敲行内值同一存储
路径（node.constants）。

## 2. 判据（全部满足才内联）

1. const 节点类型 ∈ {const.number, const.int, const.bool, const.string}
   （const.color 不动——COLOR 端口无行内字面值编辑器）；
2. 出边**恰好 1 条**（共享常量内联会复制值且失去单一改点）；
3. 目标端口类型有行内编辑器（INT/FLOAT/BOOL/STRING/ANY，同 EvmInlinePortFields
   覆盖范围）且（带默认值 **或类型为 ANY**）——ldlib2 对 ANY 输入无条件挂行内
   文本编辑器（v8 起放开，call 的 argN 即此类）；其余无默认值端口无行内编辑器，
   内联后值会隐身，不动）。

## 3. 语义保持

- 常量以 JsonElement 原样写入目标节点 `constants[端口id]`——不经过
  InlineLiteral.parse 的智能解析，`const.string("5")` 不会退化为 int 5；
- 导出路径与端口默认值/行内常量一致，产物不变；
- 顺序：VariableInliner（若启用）→ InitDefaultFolder → **ConstNodeInliner** → 布局，
  无条件启用（ImportGraphBuilder.build）。

## 4. 前置条件 / 后置条件 / 不变量 / 异常

- 前置：折叠之后、布局之前（删除的节点不占位）。
- 后置：const 节点与连线删除；目标节点 constants 增加端口值；产出一条
  `CONST_INLINE` info 汇总（数量）。
- 不变量：导出产物不变；多用途/孤儿 const、无行内编辑器端口目标不受影响。
- 异常行为：条件不满足 = 原样保留（静默）。

## 5. 验证

- 单测 ConstNodeInlinerTest 6 例：单用内联（常量落 constants + 节点线删除）、
  共享保留、字符串型保持、无行内编辑器端口不动、const.color 不动、孤儿不动；
  三节点编译、1.20.1/1.21.1 test+nullaway 全绿。
- 实机：悦灵重导后 animate.entry weight 等单用常量不再产生 const 节点。
