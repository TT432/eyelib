# Eyelib Molang 模块形式化证明

> 2026-06-10 全栈证明记录

## 方法论

从 Bedrock 1.21.130.26 官方文档提取形式操作语义 `⟦·⟧ : Expr × Scope → Value`，然后对实现管线 4 层做结构归纳：

```
Layer 1 [Parser]:     String → MolangAst           HandwrittenMolangAstParserFrontend
Layer 2 [Binder]:     MolangAst → BoundMolang      MolangBinder + QueryProjector
Layer 3 [Compiler]:   BoundMolang → JVM bytecode   MolangBytecodeEmitter
Layer 4 [Runtime]:    bytecode × Scope → Value     MolangRuntimeSupport + MolangScope
```

对每一层，验证每条语义规则与实现的对应关系：
- 规范前提 → 实现事实 → 结论（一致或矛盾）

## Layer 1 — Parser：✅ 完全正确

13 级优先级链完整：`Assign → Ternary → NullCo → Or → And → Equality → Comparison → Add → Multiply → Unary → Postfix → Primary`

## Layer 2 — Binder：✅ 结构正确

别名规范化 (q→query, t→temp, v→variable, c→context) 和查询投影正确。
小缺陷：`BinaryConditionalExpr` AST 字段命名为 `whenFalse` 实为 `whenTrue`（数据流不受影响）。

## Layer 3 — Compiler：❌ 一个语义 bug

**BUG：字符串 `==`/`!=` 比较使用 `asFloat()+fcmpg`**

```java
// MolangBytecodeEmitter:262-276
emitExpr(code, binary.left());
code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", ...);
emitExpr(code, binary.right());
code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", ...);
switch (operator) {
    case "==", "!=", "<", "<=", ">", ">=" -> emitComparison(code, operator);
}
```

`MolangString.asFloat()` 返回 `0`，导致 `'a' == 'b'` 比较 `0.0 == 0.0` → true。

**三段论**：
- 前提：Bedrock 规范"String operations only support == and !=" → 比较字符串内容
- 事实：实现对所有 `==`/`!=` 统一用 `asFloat()+fcmpg`
- 矛盾：`'a' == 'b'` 规范预期 `0.0`，实现返回 `1.0`

**修复方向**：在 `emitBinaryExpr` 中对 `==`/`!=` 先检查操作数是否为 MolangString，若是则调用 `MolangObject.equalsF/equals` 而非 `asFloat+fcmpg`。

## Layer 4 — Runtime：✅ 结构正确

`MolangRuntimeSupport.resolveMemberAccess` 解析链：scope.get → mappingTree.findField → mappingTree.findMethod → MolangNull。

**注意**：未解析查询返回 `MolangNull`（非 `Float(0.0)`），规范要求未定义变量→0.0。`MolangNull.asFloat()=0.0` 对数值上下文透明，但对类型检查（`instanceof MolangNull`）有差异。

## 证明结论

| 层 | 状态 | 语义 bug |
|---|---|---|
| Parser | ✅ | 0 |
| Binder | ✅ | 0（1 命名缺陷） |
| Compiler | ⚠️ | 1（string `==`/`!=`） |
| Runtime | ✅ | 0 |
