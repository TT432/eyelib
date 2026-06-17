# Molang ANTLR 移除记录

## 日期

2026-06-09

## 变更内容

### 删除的文件
- `eyelib-molang/.../generated/MolangLexer.java`
- `eyelib-molang/.../generated/MolangParser.java`
- `eyelib-molang/.../generated/MolangVisitor.java`
- `eyelib-molang/.../generated/MolangBaseVisitor.java`
- `eyelib-molang/.../generated/package-info.java`
- `eyelib-molang/.../frontend/GeneratedParserBackedMolangParserFrontend.java`

### 修改的文件
- `MolangParserFrontend.java` — 接口简化为 `parseExprSet(String)`
- `MolangParserFrontendResult.java` — 移除 ANTLR 字段，仅保留 `Optional<MolangAst.ExprSet>`
- `HandwrittenMolangAstParserFrontend.java` — 移除 ANTLR import
- `MolangCorpusParseRunner.java` — 移除 ANTLR 依赖
- `build.gradle` — 移除 `antlr4-runtime:4.9.1`

### 手写解析器架构

```
Tokenizer → Parser → AST nodes (record types) → Binder → Bytecode Emitter → HiddenClass
```

- Tokenizer: ~200 行，字符级扫描
- Parser: ~300 行，递归下降 + 优先级爬升
- AST: ~100 行，所有节点为 Java record
- Binder: ~260 行，别名规范化、标识符解析
- Bytecode Emitter: ~500 行，直接生成 JVM 字节码

### 语法覆盖

手写解析器覆盖全部 MoLang 语法（20+ 产生式分支）。

## 发现的 Bug

| Bug | 根因 | 影响 |
|-----|------|------|
| `A?B` binder 标记 UNSUPPORTED | BinaryConditionalExpr deferred | 静默返回 null |
| `A?B` 常量求值器用错分支 | `binaryCond.condition()` 而非 `binaryCond.whenFalse()` | 常量表达式求值错误 |
| `1.0f` 解析失败 | Tokenizer 不处理尾缀 `f` | .mcpack 数据中常见 |
| `{...}` 无 return 不返 0 | emitBlockExpr 缺默认值 | 块表达式返回值不确定 |
| 空输入返回有效 AST | parseExprSetAst 无 blank 检查 | 边界行为不符合规范 |

## 后续发现的 Bug（2026-06-10）

### 运算符优先级错误（导致史莱姆不可见 + 羊变黑）

**问题**：`parseAnd()` 直接调用 `parseComparison()`，而 `parseComparison()` 把 `<`, `<=`, `>`, `>=`, `==`, `!=` 全部混在同一层级处理。

这意味着 `&&` 实际比 `==` 绑定得更紧。例如 `q.variant == 0 && q.skin_id == 1` 被解析为：
```
(q.variant == (0 && q.skin_id)) == 1   // ❌ 错误
```
而非：
```
(q.variant == 0) && (q.skin_id == 1)   // ✅ 正确
```

这导致 RC 状态机中的复合条件表达式被错误求值，进而选错 state → 选错材质/动画 → 渲染异常。

**Bedrock Molang 正确优先级链**：比较符（`< <= > >=`）高于相等符（`== !=`），`&&` 高于 `||` 但低于相等符。正确 parse 链应为 `parseOr → parseAnd → parseEquality → parseComparison → parseAdd → ...`

**修复**：新增 `parseEquality()` 方法单独处理 `==`/`!=`，`parseAnd()` 改为调用 `parseEquality()` 而非 `parseComparison()`。

**教训**：molang 解析器变更后，必须先用**优先级覆盖矩阵**（按 Bedrock 官方 MoLang.html 规范）穷举验证所有运算符组合，不能用肉眼观察渲染结果来判断正确性。运算符优先级错误会导致渲染 bug（选错 RC state），而不是编译错误——这类 bug 极难通过视觉调试定位。

## 后续发现的 Bug（2026-06-10）

### 运算符优先级错误（导致史莱姆不可见 + 羊非羊毛部分变黑）

**问题**：`parseAnd()` 直接调用 `parseComparison()`，而 `parseComparison()` 把 `<`, `<=`, `>`, `>=`, `==`, `!=` 全部混在同一层级处理——`&&` 实际比 `==` 绑定得更紧。

例如 `q.variant == 0 && q.skin_id == 1` 被解析为 `(q.variant == (0 && q.skin_id)) == 1` ❌，而非正确的 `(q.variant == 0) && (q.skin_id == 1)` ✅。

这导致 RC 状态机中的复合条件被错误求值 → 选错 state → 选中错误的材质/动画 → 渲染异常。

**Bedrock 正确优先级**（由高到低）：`! -` → `* /` → `+ -` → `< <= > >=` → `== !=` → `&&` → `||`

**正确 parse 链**：`parseExpression → parseAssignment → parseTernary → parseNullCoalesce → parseOr → parseAnd → parseEquality → parseComparison → parseAdd → parseMultiply → parseUnary → parsePostfix → parsePrimary`

**修复**（commit `17d9fad1`）：新增 `parseEquality()`，`parseAnd()` 改为调用 `parseEquality()`。

**教训**：molang 解析器变更后，必须用**优先级覆盖矩阵**穷举验证所有运算符组合。优先级错误不会产生编译错误——只会产生静默的逻辑 bug——极难通过视觉观察定位。

## 注意事项

- 手写解析器已是活跃前端（`MolangParserFrontends.active()` 返回 `HandwrittenMolangAstParserFrontend.INSTANCE`）
- 接口变更后必须 `:eyelib-molang:jar createLaunchScripts`（仅 `compileJava` 不够）
- `BinaryConditionalExpr` record 字段名 `whenFalse` 实际存储的是 when-true 值（命名误导）
- loop 已从 deferred 改为完全支持
- for_each 仍为 deferred
