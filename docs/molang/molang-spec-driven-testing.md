# MoLang 规范驱动测试方法

> 2026-06-09 session 教训记录

## 问题

对 molang 模块做 ANTLR 移除时，仅依赖现有的 148 个测试（corpus/binder/full-pipeline）声称 "all pass"，但实际遗漏了：
- 比较 vs 相等运算符优先级错误（`<` 和 `==` 同级而非分两级）
- BinaryConditionalExpr 被 binder 标记 UNSUPPORTED 导致静默返回 null
- 空白输入被错误拒绝（Bedrock 动画 JSON 中合法）
- 数值尾缀 `f` 不被识别（vanilla .mcpack 大量使用）

这些 bug 在现有的 148 个测试中全部漏过。

## 正确方法

从 **Mojang 官方 Bedrock 规范**（bedrock-dev-docs 1.21.130.26 MoLang.html）推导测试矩阵：

### 1. 提取形式语法

```
Primary:  NUM | STR | ID | true | false | this | loop(...) | for_each(...) | (E) | {S*}
Unary:    ('!' | '-') Unary | Postfix
Multiply: Unary (('*' | '/') Unary)*
Add:      Multiply (('+' | '-') Multiply)*
Comparison: Add (('<' | '<=' | '>' | '>=') Add)*
Equality: Comparison (('==' | '!=') Comparison)*    ← 官方 spec 比较高于相等！
And:      Equality ('&&' Equality)*
Or:       And ('||' And)*
Ternary:  Or ('?' E (':' E)?)?
NullCo:   Ternary ('??' Ternary)*
Assign:   NullCo ('=' Assign)?
```

### 2. 计算分支 × 覆盖率

每条产生式的每个分支一个测试，13 级优先级每对相邻级别一个交叉验证测试。

### 3. 验证语义规则

从官方 spec 提取语义规则表，逐条对照：
- 错误→0.0（除零、未定义变量、null 引用）
- 简单表达式返回值，复杂表达式无 return→0.0
- 字符串仅支持 `==` `!=`
- 三元嵌套右结合（1.18.10+）
- `&&` 高于 `||`（1.18.20+）
- 比较高于相等（1.18.20+）
- 空白/空输入 = 0
- loop 最大 1024
- 数组索引：负数→0，超限→wrap

### 4. Oracle 优先级

1. **Mojang Creator 文档**（bedrock-dev-docs MoLang.html）— 最权威
2. **.mcpack 真实数据** — 实际行为
3. **Bedrock Wiki** — 社区文档，可能过时
4. **项目内部 pitfall/ADR** — 二次加工

## 教训

- Wiki 和官方 spec 不一致时，官方 spec 为准。本 session 发现的比较 vs 相等优先级错误就是因为 Wiki（advanced-molang.md）和官方 spec（MoLang.html）的优先级表不同，而 parser 同时参考了两者
- 仅依赖现有测试 = 盲区。必须从规范重新推导
- 语法覆盖 ≠ 语义覆盖。每条产生式还要验证具体的 Bound 节点类型和求值结果
- **测试只覆盖 parser 不够，语义 bug 在编译器层（字节码生成）。** 见 `references/molang-bytecode-semantics.md`——`==`/`!=` 在 parser 生成正确的 AST，但字节码发射器用了 `asFloat()+fcmpg` 导致字符串比较全等。必须逐操作符对照 Bedrock 语义规则验证字节码生成逻辑。
