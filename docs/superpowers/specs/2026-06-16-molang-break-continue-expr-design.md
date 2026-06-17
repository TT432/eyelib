# Spec: break/continue 在表达式语境中

## 背景

基岩版 molang 文档（`syntax-guide.md:282,303`）确认以下语法合法：

```text
loop(10, {t.x = v.x + v.y; v.x = v.y; v.y = t.x; (v.y > 20) ? break;});
loop(10, {(v.x > 5) ? continue; v.x = v.x + 1;});
```

`break`/`continue` 出现在 `BinaryConditionalExpr` 的 `whenTrue` 位置（`condition ? break`），说明它们是**表达式**。

eyelib 当前：parser `parsePrimary`（第 314-320 行）把表达式位置的 `BREAK`/`CONTINUE` 创建为 `UnknownExpr`（fallback），binder 标记 deferred → 返回 `MolangNull`（无控制流效果）。

## 目标

让 `break`/`continue` 在表达式位置产生正确的控制流效果（goto break/continue target）。

## 设计

### AST

新增两个表达式节点（不含 valueExpr，因为基岩版文档中表达式位置的 break/continue 都不带值）：

```java
// 在 MolangAst.Expr 层级
public record BreakExpr(SourceSpan span) implements MolangAst.Expr {}
public record ContinueExpr(SourceSpan span) implements MolangAst.Expr {}
```

### Parser

修改 `parsePrimary` 第 314-320 行：

```java
// 之前：
if (match(TokenKind.BREAK)) { return new UnknownExpr(...); }
if (match(TokenKind.CONTINUE)) { return new UnknownExpr(...); }

// 之后：
if (match(TokenKind.BREAK)) { return new BreakExpr(span(previous())); }
if (match(TokenKind.CONTINUE)) { return new ContinueExpr(span(previous())); }
```

**关键**：`parseStatements`（第 84-93 行）中 `match(BREAK)`/`match(CONTINUE)` 先于 `parseExpression` 执行，会消费 token 创建 BreakStmt/ContinueStmt（语句位置）。所以 parsePrimary 的 BREAK/CONTINUE 只在**表达式位置**触发，两者不冲突。

### Binder

新增两个 BoundExpr 子类：

```java
public record BoundBreakExpr(SourceSpan span) implements BoundExpr {}
public record BoundContinueExpr(SourceSpan span) implements BoundExpr {}
```

绑定逻辑（`bindExpr` switch 新增 case）：
- `BreakExpr` → 检查 `loopScopes` 栈非空（与 BreakStmt 相同的 ERROR diagnostic），返回 `BoundBreakExpr`
- `ContinueExpr` → 同上，返回 `BoundContinueExpr`

### BytecodeEmitter

`emitExpr` switch 新增两个 case：

```java
case BoundBreakExpr _ -> {
    // goto loopEnd (break target)
    // 后续不可达，push null 保证栈平衡（dead code）
    var ctx = loopContexts.peek();
    assert ctx != null : "break in expr without loop context (binder should have caught this)";
    builder.goto_(ctx.loopEndLabel());
    builder.aconst_null(); // dead code: verifier needs stack-balanced unreachable
}
case BoundContinueExpr _ -> {
    // goto continueTarget
    // 后续不可达，push null 保证栈平衡
    var ctx = loopContexts.peek();
    assert ctx != null;
    builder.goto_(ctx.continueTargetLabel());
    builder.aconst_null(); // dead code
}
```

> 注意：goto 之后的代码不可达，但 JVM bytecode verifier 可能要求栈平衡。`aconst_null` 确保不可达路径也能通过验证。如果 verifier 不要求（goto 无 fallthrough），可省略 aconst_null——实现时按实际 verifier 行为准。

### BinaryConditionalExpr 的交互

`condition ? break` 编译时：
1. 求 condition
2. if condition == 0（false）→ goto skipTrue（跳过 whenTrue）
3. whenTrue = BoundBreakExpr → goto loopEnd
4. skipTrue: push MolangNull（BinaryConditionalExpr 的 false 分支值）
5. merge: BinaryConditionalExpr 的值在操作数栈上

如果作为语句使用（`condition ? break;`），BinaryConditionalExpr 的值会被 ExprStmt 丢弃（pop），所以栈平衡。

## 测试用例

```java
// 基岩版文档示例：break 在表达式语境
// loop(10, {t.x = v.x + v.y; v.x = v.y; v.y = t.x; (v.y > 20) ? break;});
// 验证：循环在 v.y > 20 时终止

// 基岩版文档示例：continue 在表达式语境
// loop(10, {(v.x > 5) ? continue; v.x = v.x + 1;});
// 验证：v.x 达到 6.0 后不再增加

// break 不在循环中 → binder ERROR
// 验证：编译报错，不允许 break/continue 在循环外
```

## 验收标准

1. 编译通过（`jetbrain_build_project`）
2. 单元测试全部通过（`:eyelib-molang:test`）
3. 新增测试覆盖表达式位置的 break/continue（至少 2 个测试：一个 break、一个 continue）
4. break/continue 在循环外使用时 binder 报 ERROR（与语句位置行为一致）
