# 动画/AC 的 molang 变量引用声明（decl_variables）

> 状态：已实现并实机验证（2026-08-07）。
> 动机：nodegraph-init-default-fold §3「变量作用域注意」——variable.* 是实体级
> 作用域，但引用点分散在动画与 AC 文件里，实体库图内不可见。本特性把这些
> 跨文件引用显式建模进实体库图。

## 1. 需求

1. `ref.animation` 节点加一个输入端口，声明该动画引用的全部 molang 变量。
2. `ref.ac` 做与 `ref.rc`（v6 decl_* 通道）类似的处理：同样获得变量引用声明端口。
3. 导入时自动从动画/AC 文档提取变量名并接线，使用户在实体库里就能看到
   闭包级的变量引用面（变量表引用数、图上高亮随之覆盖跨文件引用）。

## 2. 设计

### 2.1 端口

- `ref.animation` / `ref.ac` 新增 `decl_variables`（`PortType.VARIABLE`，inMulti）。
  命名镜像 ref.rc 的 `decl_*` 前缀（声明通道，不承载 molang 值）。
- 每个（变量名, ref 节点）对实例化一个 `variable` 节点接入——变量表的
  「引用数」（按绑定节点计数）因此等于「该变量被多少个动画/AC 引用 + 图内使用」。
- VARIABLE→VARIABLE 同型直连，兼容矩阵无需修改（PortType.isAssignableTo
  同型恒真）。

### 2.2 变量提取（MolangVariableRefs，decompile 包）

对原始 JSON 文档递归收集字符串值，逐个经 MolangTokenizer 切词，匹配
`variable.<name>` 与别名 `v.<name>`（IDENT DOT IDENT 三连）收集名字。
语法残缺的字符串跳过（不阻断导入）。`temp.`/`t.` 不收集（表达式局部，
非实体级作用域）。统一从**原始文档**提取（不依赖图翻译保真度——AC 的
on_exit、transition 条件、动画 keyframe molang 全部覆盖）。

### 2.3 导入接线（AnimationVariableDecls，decompile 包）

`ImportClosure.importWithClosure` 后处理：

1. 实体库主图中每个 `ref.animation`（identifier 非空）→ LazyScan 动画文档 → 提取变量集；
2. 每个 `ref.ac` → LazyScan AC 文档 → 提取变量集（不再依赖 AC 图库已导入，
   未找到的 AC 照常有声明）；
3. `AnimationVariableDecls.wire`：按（名, ref）对新增 variable 节点 + 连线
   （节点竖排于 ref 节点下方），新变量名并入图声明（type UNKNOWN、scope
   VARIABLE、无默认值），产出新 GraphLibrary 重新注册。

### 2.4 与折叠/内联的关系（结构独立，无需代码豁免）

decl 接线在导入**后处理**（折叠/内联完成之后），且每个（名, ref）对使用**独立的
variable 节点实例**——写入节点与声明节点不共享。因此折叠/内联的读写判定天然
看不见声明边：折叠只删写入节点，声明节点与连线原样保留（有回归测试
foldLeavesDeclNodesIntact 守护）。注意顺序依赖：若未来把 decl 接线移进导入流水线，
同名声明节点会触发折叠的「同名第二个 variable 节点 → 绑定歧义」保护。

## 3. 前置 / 后置 / 不变量 / 异常

- 前置：闭包导入完成（实体库已注册）；包文档经资源桥可扫（包未挂载 = 提取为空，静默）。
- 后置：实体库新增 variable 节点与 decl_variables 连线；图声明补齐新变量名。
- 不变量：**不改变导出产物**——decl_variables 是纯元数据，组装器不读取这些端口；
  ref.animation/ref.ac 的导出语义（identifier/short_name）不变。
- 异常：文档缺失/语法残缺 = 该 ref 无声明（静默）；变量节点重名复用图声明合并去重。

## 4. 非目标

- temp.* 不声明（表达式局部）；
- 不做引用方向（读/写）区分（v1 只要「触及」面）；
- 不反向约束动画内容（不验证动画是否真的用了声明的变量——提取即真相）。

## 5. 验证

- 单测：变量提取（v./variable. 别名、temp 排除、残缺语法跳过、嵌套 JSON 递归）；
  接线（节点/边/声明合并/布局位置）；折叠与内联对 decl 边的豁免。
- 实机：悦灵闭包重导——edfatt 接线到引用它的 AC/动画 ref 节点、变量表引用数
  覆盖跨文件引用、INIT_DEFAULT_FOLD 折叠数不减少、构建产物与折叠前一致。
