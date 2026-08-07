# 自定义 molang 函数（.emolang）

> 状态：设计定稿，实现中（2026-08-07）。
> 用户需求（2026-08-07 原话）：编辑器层面自定义 molang function；`$config/emolang`
> 下每个文件一个函数，能定义输入、输出、局部变量；导出时自动展开
> （`custom1(1, 2)` → `1 + 2`）。

## 1. 需求

1. 用户可编写 `.emolang` 文件定义自定义 molang 函数（参数、返回、局部变量）。
2. 节点图编辑器中像内置函数一样被选用（query.call / math.call / exec.call 的
   function 下拉候选），参数端口自动带名称/类型标注
   （复用 `MolangFunctionSignatures` 注册缝）。
3. 导出（codegen）时调用点**内联展开**为纯 molang——产物不依赖任何运行时
   自定义函数支持，与 Bedrock 完全兼容。
4. 局部变量展开时必须**卫生**（多次调用/嵌套调用不互相污染）。

## 2. 文件格式

位置：`$config/emolang/*.emolang`（客户端运行目录的 config/emolang）。

语法（每个文件恰好一个函数）：

```
function <name>(<param>: <type>, ...) {
    <molang 语句序列>
    return <表达式>;
}
```

- `name`：`[a-z][a-z0-9_]*`（小写蛇形）。**裸名调用**（不写 query./math. 前缀），
  如 `custom1(1, 2)`。名称语法不含 `.`，与内置函数（必有根前缀）构造上不可能冲突。
- `type`：`float | int | bool | string`——仅标注语义（端口 label / 下拉提示）；
  molang 物理类型全是 float，string 仅指标签/名称类用法。type 可省略（= float）。
- 参数引用：体内以**裸标识符**引用参数（`return arg1 + arg2`）——裸标识符不是
  合法 molang 变量引用，是 .emolang 的语法扩展，展开时按 token 替换。
- 局部变量：体内全部 `temp.*` 一律视为函数局部（不论是否先赋值），展开时重命名
  （见 §4）。`variable.*` 不隔离（函数可读写实体变量，属有意的副作用通道）。
- `return`：恰好一个，且是最后一个非空语句（v1 约束，加载校验）。
- 注释：`//` 行注释（解析器在 token 化前剥离，字符串字面量内的 `//` 不受影响）。

示例：

```
// config/emolang/custom1.emolang
function custom1(arg1: int, arg2: float) {
    temp.doubled = arg1 * 2;
    return temp.doubled + arg2;
}
```

## 3. 加载

- 时机：客户端启动（`ClientBootstrap` 阶段）+ 编辑器打开时重扫（无需重启游戏）。
- 解析：函数头手写解析；**体不做完整 molang parse**——存原文 token 流。
  加载校验 = 参数名替换为 `0` 后经 molang 前端 parse 体块（语法检查）+
  return 存在且唯一在尾 + 参数名非 molang 保留字。失败 → 日志 + 跳过该函数（不影响其它）。
- 注册：`EmolangRegistry`（domain）：name → `EmolangFunction(params, bodyTokens, source)`；
  同步 `MolangFunctionSignatures.registerCustom`（参数名/类型 → 端口签名）。
  重扫前 `clearCustoms()`。

## 4. 展开语义（codegen）

调用点：query.call / math.call / exec.call 的 function 选项值为已注册裸名时，
`EmitSession.emitCallLike` 走展开分支而非直发：

1. **实参绑定**：实参表达式在调用点帧先发射（沿用子图展开 D5 同构规则）——
   形参在体内被引用 ≥ 2 次且实参非平凡（非字面量/变量引用）时，提取
   `temp.em<K>_<param> = <arg>;` 前置语句，体内引用替换为该 temp 名；
   否则直接文本替换为 `(<实参表达式>)`。
2. **局部变量卫生重命名**：体内全部 `temp.<x>` → `temp.em<K>_<x>`
   （K = 全局展开计数，每次展开自增——与 subgraph 的 `sg<K>_` 前缀同机制）。
3. **token 级替换**：对体 token 流操作（标识符精确匹配参数名），不做字符串替换
   （避免误伤同名子串/字符串字面量）。
4. **产出**：体的非 return 语句 → 调用点 preludes；return 表达式 → 调用点值表达式。
   exec.call 语句位调用：return 表达式作为丢弃值语句并入语句序列。
5. **递归/嵌套**：函数体内可调用其它自定义函数（同样裸名 token 命中即展开）；
   展开深度上限 32（沿用子图先例），超限/自递归 → 诊断 `EMOLANG_RECURSION`。
6. 参数个数不匹配 → 诊断 `EMOLANG_ARITY`（少了补 0 占位，多了忽略），不中断导出。

## 5. 诊断

| 码 | 时机 | 级别 | 说明 |
|---|---|---|---|
| 加载失败 | 加载 | 日志 | 语法错误/return 缺失/非法名或类型 → 跳过该函数 |
| `EMOLANG_ARITY` | codegen | warning | 实参与形参数目不符 |
| `EMOLANG_RECURSION` | codegen | error | 展开深度超限/自递归 |
| `UNKNOWN_FUNCTION` | codegen | warning | function 选项为未知裸名（原样直发会产生非法 molang） |

## 6. 编辑器集成

- 下拉候选：`AssetSuggestions` 的 `molang.query` 键并入自定义函数裸名
  （`MolangFunctionSignatures.customNames` 机制，`customNames` 对无根裸名返全部）。
- 端口签名：注册时写入 `MolangFunctionSignatures` → 参数端口自动带名称/类型标注
  （string 参数 label 带 `: str`），定长函数隐藏 arg_count。
- 编辑器打开时重扫 config/emolang → 改文件后重开编辑器即生效。

## 7. 验证

- 单测：解析（头/类型省略/注释/局部变量）、加载校验（无 return、重名内置、
  语法错误跳过）、展开（文本替换卫生、实参提取、temp 重命名、嵌套展开、
  递归诊断、arity 诊断）、下拉候选并入。
- 实机：写一个 .emolang → 编辑器下拉选用 → 端口标注正确 → 构建导出 molang
  为展开产物（无裸名残留）→ 实体渲染行为符合函数语义。
