# Molang 模块形式化模型

> 从 Mojang 官方 Bedrock 文档推导（bedrock-dev-docs 1.21.130.26 + Creator syntax-guide）

## 1. 输入空间 `S`

### 1.1 字符集 `Σ`

```
Σ = ASCII 可打印字符 ∪ { \t, \r, \n }
数字      D = [0-9]
字母      L = [a-zA-Z_]
字母数字  A = [a-zA-Z0-9_]
```

### 1.2 Token 类型 `T`

```
T ::= NUM | STR | ID | KW | OP | PUNCT | EOF

NUM  ::= D+ ('.' D+)? ([eE] [+-]? D+)? ('f'|'F')?
STR  ::= "'" [^']* "'"
ID   ::= L A*
KW   ::= 'return' | 'break' | 'continue' | 'loop' | 'for_each' | 'this' | 'true' | 'false'
OP   ::= '+' | '-' | '*' | '/' | '!' | '&&' | '||'
       | '<' | '<=' | '>' | '>=' | '==' | '!='
       | '?' | ':' | '??' | '=' | '->'
PUNCT ::= '(' | ')' | '{' | '}' | '[' | ']' | ',' | ';' | '.'
```

### 1.3 语法 `G`

```
ExprSet  ::= Stmt (';' Stmt)* ';'?

Stmt     ::= ReturnStmt | BreakStmt | ContinueStmt | ExprStmt
ReturnStmt ::= 'return' Expr
BreakStmt ::= 'break'
ContinueStmt ::= 'continue'
ExprStmt ::= Expr

Expr     ::= Assignment

Assignment ::= Conditional ('=' Assignment)?

Conditional ::= NullCoalesce ( ('?' Expr (':' Expr)? )? )

NullCoalesce ::= Or ('??' Or)*

Or       ::= And ('||' And)*

And      ::= Comparison ('&&' Comparison)*

Comparison ::= Add (CMP_OP Add)*
CMP_OP  ::= '<' | '<=' | '>' | '>='

Equality ::= Comparison (EQ_OP Comparison)*
EQ_OP   ::= '==' | '!='

Add      ::= Multiply (ADD_OP Multiply)*
ADD_OP  ::= '+' | '-'

Multiply ::= Unary (MUL_OP Unary)*
MUL_OP  ::= '*' | '/'

Unary    ::= ('!' | '-') Unary | Postfix

Postfix  ::= Primary ( PostfixOp )*
PostfixOp ::= '.' ID
            | '->' ID '.' ID
            | '(' Args? ')'
            | '[' Expr ']'

Primary  ::= NUM | STR | ID | 'true' | 'false' | 'this'
           | 'loop' '(' Expr ',' Block ')'
           | 'for_each' '(' ID ',' Expr ',' Block ')'
           | '(' Expr ')'
           | '{' Stmt* '}'

Block    ::= '{' Stmt* '}'
Args     ::= Expr (',' Expr)*
```

**注**：根据 Mojang 官方 spec，`!` 优先级最高（高于 `*` `/`），`-`（一元）与 `!` 同级。`&&` 和 `||` 分属不同级别（`&&` 高于 `||`）。`==` `!=` 与 `<` `<=` `>` `>=` 也分属不同级别（比较高于相等）。官方 spec 未将 `=`、`return`、`->` 列入优先级表——它们有独立语义。

### 1.4 语义规则

| 规则 | 官方依据 |
|------|---------|
| 所有数值为单精度浮点 | "All numerical values are floats" |
| 布尔值 0.0=false, ≠0.0=true | "float value equivalent to 0.0 is false" |
| 错误（除零、未定义变量、null引用）返回 0.0 | "Errors generally return a value of 0.0" |
| 简单表达式（无 `;`）返回表达式值 | "A simple expression... the value of which is returned" |
| 复杂表达式无 return 时返回 0.0 | "if you don't return... will evaluate to 0.0" |
| 字符串仅支持 `==` `!=` | "String operations only support == and !=" |
| 三元嵌套右结合（1.18.10+） | "A?B:C?D:E = A?B:(C?D:E)" |
| `&&` 高于 `||`（1.18.20+） | "Logical AND to evaluate before Logical OR" |
| 比较高于相等（1.18.20+） | "comparison operators to evaluate before equality operators" |
| loop 最大 1024 次 | "maximum loop counter is 1024" |
| 数组索引：负数→0，超限→wrap | "clamped at zero...wrapped by the array size" |
| 大小写不敏感（字符串除外） | "All things in Molang are case-INsensitive, with the exception of strings" |
| 空白输入：求值为 0 | 简单表达式规则推论——无 token 即无表达式，返回 0 |

## 2. 管线函数 `f: S → O`

### 2.1 Tokenizer `tok: String → List⟨T⟩`

```
tok(s) = 顺序扫描字符，按最长匹配原则生成 token 序列
```

**不变量**：
- 任意合法输入产生唯一 token 序列
- 非法字符 → ParseException

### 2.2 Parser `parse: List⟨T⟩ → AST`

递归下降，每个非终结符对应一个解析函数。AST 节点携带 `SourceSpan`。

**不变量**：
- 任意合法 token 序列产生唯一 AST
- 空 token 序列 → `NumberLiteralExpr(0)`（等价于空白输入求值为 0）

### 2.3 Binder `bind: AST → BoundAST`

```
bind 职责：
1. 标识符规范化：q→query, t→temp, v→variable, c→context
2. 赋值目标验证：context.* 不可写
3. 节点类型映射：每种 AST 节点 → 确定的 Bound 节点类型
```

**不变量**：
- `BinaryConditionalExpr` → `BoundBinaryConditionalExpr`（不可为 Deferred）
- `TernaryConditionalExpr` → `BoundTernaryConditionalExpr`
- `LoopExpr` → `BoundLoopExpr`（不可为 Deferred）
- `ForEachExpr` → `BoundForEachExpr`（Deferred，因未实现）
- 赋值为 `context.*` → `BindDiagnostic(ERROR)`

### 2.4 BytecodeEmitter `emit: BoundAST → byte[]`

生成 JVM 字节码，通过 `MethodHandles.Lookup.defineHiddenClass()` 加载。

**不变量**：
- `BinaryConditionalExpr`: condition真→whenTrue，假→0.0
- `TernaryConditionalExpr`: condition真→whenTrue，假→whenFalse
- `BlockExpr` 最后非 return → 隐式 push 0.0
- `LoopExpr`: 循环 count 次执行 body，返回值 0.0
- 除零 → 0.0
- 未定义变量 → 0.0

### 2.5 Evaluate `eval: byte[] × MolangScope → MolangObject`

通过 `MethodHandle.invoke(scope)` 执行编译后的字节码。

## 3. Oracle：参考解释器

由于 molang 模块无外部依赖，我们可以构建一个**树遍历解释器**作为 oracle。它直接对 AST 求值，逻辑简单、显然正确。

```
interpret(AST, scope):
  match node:
    NumberLiteralExpr → MolangFloat(value)
    StringLiteralExpr → MolangString(rawText)
    ThisExpr → scope.getThis() ?? 0.0
    UnaryExpr(op, expr) → op == '-' ? -interpret(expr) : interpret(expr)==0 ? 1 : 0
    BinaryExpr(op, l, r) → ...
    TernaryConditionalExpr(cond, t, f) → interpret(cond)!=0 ? interpret(t) : interpret(f)
    BinaryConditionalExpr(cond, t) → interpret(cond)!=0 ? interpret(t) : 0.0
    BlockExpr(stmts) → 执行所有 stmt，最后非return则返回 0.0
    ...
```

**Oracle 正确性论证**：解释器是对语法树的直接语义映射，每步操作对应 MoLang 规范的一条语义规则。其正确性仅依赖于规范的正确理解，不依赖于编译器实现细节。

## 4. 验证策略

### 4.1 差分测试 `∀e ∈ S_const: interpret(e) = eval(emit(bind(parse(e))))`

对纯常量表达式（无标识符），解释器和编译器的求值结果必须一致。

### 4.2 类型测试 `∀e: bind(parse(e)).root 类型符合上表`

Binder 对每种 AST 节点产出的 Bound 类型是确定的。

### 4.3 崩溃测试 `∀e ∈ S: eval(e) 不抛非预期异常`

对于所有合法表达式（含标识符），编译器不应崩溃。`ExpressionCompileException` 仅在语法/语义无效时合法出现。

### 4.4 穷举测试空间

`S = Primary × PostfixOp* × BinaryOp × ...`

- `Primary` 分支：10 种（NUM|STR|ID|true|false|this|loop|for_each|(Expr)|{Stmt*}）
- `Unary` op：2 种（`!`, `-`）
- `Binary` op：10 种（`+`, `-`, `*`, `/`, `<`, `<=`, `>`, `>=`, `==`, `!=`）
- `Logical` op：2 种（`&&`, `||`）
- `Conditional` 分支：3 种（无、二元、三元）
- `Postfix` op：4 种（`.`、`->`、`()`、`[]`）

每层组合数的乘积构成完整的测试空间。通过参数化测试 + 随机模糊测试可达到高覆盖率。

## 5. 实现检查清单

对照上述模型，检查当前实现：

- [ ] Tokenizer: `f`/`F` 后缀处理 ✅ (已修复)
- [ ] Tokenizer: 科学计数法 ✅
- [ ] Parser: 空白输入 → `NumberLiteralExpr(0)` ✅ (已修复)
- [ ] Parser: `!` 与 `-` 同级，高于 `*` `/` ✅
- [ ] Parser: `==` `!=` 低于 `<` `<=` `>` `>=` ⚠️ (当前实现为同级)
- [ ] Parser: `;` 在复杂表达式中每个语句都需要 ⚠️ (当前实现宽松)
- [ ] Binder: `BinaryConditionalExpr` → `BoundBinaryConditionalExpr` ✅ (已修复)
- [ ] Binder: `LoopExpr` → `BoundLoopExpr` (非Deferred) ✅ (已修复)
- [ ] Bytecode: 除零 → 0.0 ✅
- [ ] Bytecode: 隐式 return 0.0 ✅ (已修复)
- [ ] Bytecode: loop 最大 1024 ⚠️ (未实现)
- [ ] Evaluate: 错误 → 0.0（未定义变量等）⚠️ (部分实现)
