# 初始化赋值折叠为变量默认值（导入期自动）

> 状态：已实现并实机验证（2026-08-07）。判据与等价性论证见
> `InitDefaultFolder` 类文档（代码为权威参考）。

## 1. 需求

导入的 Bedrock 实体脚本里大量 `variable.x = 0;` 这类 initialize 链上的常量初始化
语句（悦灵 89 条，每条 = set_var + variable 两节点），本质是声明初始值，应当
折叠为变量声明的默认值，而不是以执行语句形态留在图里。

## 2. 判据（全部满足才折叠，宁漏勿错）

1. 恰好一次写入：variable 节点唯一出边 → `set_var.target`；零读取（无其它出边）；
2. 写入值为常量：value 端口未连线（端口默认值）或连线自 `const.*` 节点；
3. exec 路径只经过 `exec.set_var`/`exec.set_temp`，终末落在 `entity.root.initialize`
   （pre_animation/动画/dead-end 不折）；
4. 变量名未以文本形式出现在任何行内 molang 常量（`variable.x`/`v.x` 词边界匹配）；
5. 同图无同名第二个 variable 节点；声明已有**不同**默认值时不折。

## 3. 等价性论证

折叠把写入从「链中位置」提前到「initialize 最前」（默认值导出为前置语句，
见 nodegraph-variable-table §默认值导出）。重排只在被跨越语句能读取该变量时
才可观测：图内读取被判据 1 排除、行内文本读取被判据 4 排除、外部文件无法在
initialize 执行途中插入读取。故产物语义等价（同进 scripts.initialize）。

## 4. 前置条件 / 后置条件 / 不变量

- 前置：导入流水线内联（VariableInliner，若启用）之后、布局之前；无条件启用。
- 后置：每个折叠变量产生带默认值的 VariableDecl（类型按常量推断：
  整数→INT、小数→FLOAT、布尔→BOOL、字符串→STRING）；set_var 与 variable
  节点删除、exec 链旁路；值源 const 节点成孤儿时一并删除。
- 不变量：不新增/删除任何 molang 语句的可观测效果；导出 initialize 语句集不变
  （顺序可能不同——常量写入间无依赖，顺序不承载语义）。
- 异常行为：条件不满足 = 原样保留（静默，无诊断）；有折叠时产出一条
  `INIT_DEFAULT_FOLD` info 汇总（不逐条刷屏）。

## 5. 非目标

- pre_animation 上的常量赋值不折（导出位置会变，严格等价的例外）；
- 表达式值不折（inline_demo 的 hp/x/y 反例：1 写 0 读但跨文件通道）；
- 编辑器内实时折叠不做（用户可能在构造中，自动删节点是敌意行为）；
- TEMP 作用域不折（temp 无默认值概念，不导出 initialize）。

## 6. 验证

- 单测 12 例（正例×3：未连线/链式串联/const 连线+孤儿删除+类型推断；
  负例×8：有读取/表达式值/pre_animation/夹 exec.call/文本引用/同名歧义/
  dead-end/声明冲突）；
- 实机：悦灵重导入折叠 89 个，变量表默认值落位，构建导出 initialize 语句集
  与折叠前一致。
