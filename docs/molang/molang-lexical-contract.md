# Molang 词法契约：无前导零小数

## 适用范围

手写 Molang tokenizer 对十进制数字字面量的词法边界。该契约解决 Bedrock 数据中常见的 `.0`、`.5` 形式，同时保留成员访问点号语义。

## 词法规则

当当前字符是数字，或当前字符为 `.` 且下一字符是数字时，tokenizer 必须进入数字读取路径。数字读取继续复用现有十进制流程：

- 整数：`0`、`12`
- 小数：`0.5`、`.5`
- 科学计数法：`1.8e-4`、`.5e-2`
- `f/F` 尾缀：`.5f`、`1.0F`

生成的 `NumberLiteralExpr.rawText` 必须保留输入文本，数值转换沿用 `Double.parseDouble` 兼容路径。

## 点号歧义

- `.` 后紧跟数字 → 一个 `NUMBER` token，例如 `.5`。
- `.` 后不是数字 → `DOT` token，例如 `variable.name` 中的成员访问点。
- 单独的 `.` 或点号后接非法字符不通过容错改写；由后续语法层按原规则拒绝。

因此，数字规则只扩大数字 token 的起始集合，不改变成员访问、箭头访问或已有数字格式。

## 实现与验证入口

- 实现：[`HandwrittenMolangAstParserFrontend.Tokenizer`](../../src/main/java/io/github/tt432/eyelib/molang/compiler/frontend/HandwrittenMolangAstParserFrontend.java)。
- 词法回归应覆盖 `.0`、`.5`、`.5e-2`、`.5f`、`variable.name` 和单独 `.`。
- 编译回归应覆盖粒子表达式：
  `.0 + math.cos(variable.particle_random_2 * 360) * 0.3`

修复必须位于词法层；粒子解码或渲染事件不得吞掉编译异常，也不得复制或改写表达式字符串。
