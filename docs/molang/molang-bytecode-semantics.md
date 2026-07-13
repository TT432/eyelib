# Molang 字节码语义验证

> 2026-06-10 formal proof session

## 全栈验证模型

Molang 模块的正确性需要分 4 层逐一证明，**不能只证 parser**：

```
Layer 1 [Parser]:   String → MolangAst         [结构归纳法，13级优先级]
Layer 2 [Binder]:   MolangAst → BoundMolang    [逐节点映射 + 别名规范化]
Layer 3 [Compiler]: BoundMolang → JVM bytecode [逐操作符语义对照 Bedrock 规范]
Layer 4 [Runtime]:  bytecode × Scope → Value   [resolveMemberAccess 全链路]
```

三层验证方法：
1. **前提（规范）**：从 Bedrock MoLang.html 提取形式操作语义 `⟦e⟧σ`
2. **事实（实现）**：逐操作符检查字节码序列是否实现对应语义规则
3. **矛盾（结论）**：若产生式预期 V1、实现产出 V2 且 V1≠V2，定位到具体操作符

## 已验证的语义规则表

| 操作符 | Bedrock 语义 | 正确实现 | 已发现的问题 |
|---|---|---|---|
| `+` `-` `*` | Float 四则运算 | `asFloat` ×2, `fadd/fsub/fmul`, `MolangFloat.valueOf` | - |
| `/` | 除数0 → 0.0 | `emitSafeDiv` | - |
| `<` `<=` `>` `>=` | Float 比较 | `fcmpg` + branch | - |
| `!` | `asFloat==0 → 1.0` | `fcmpg(0); ifeq→ONE else ZERO` | - |
| `&&` `\|\|` | Short-circuit | 正确 | - |
| `?:` `?` | Conditional | 正确 | - |
| `??` | Null coalesce | `asFloat()!=0?left:right` | 语义争议：0??42→42 |
| `==` `!=` | **字符串按内容比较，数值按 float 比较** | ✅ 已修复：`equalsF/nEqualsF` | **曾用 `asFloat+fcmpg`，字符串全等** |
| `{...}` | 无 return → 0.0 | pop + MolangNull | - |
| `loop` | 执行 n 次 → Null | for 循环 | - |

## `==`/`!=` Bug 详情

**错误代码**（2026-06-10 之前）：
```java
// 所有二元运算符统一用 asFloat + fcmpg
emitExpr(code, binary.left());
code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
emitExpr(code, binary.right());
code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
case "==", "!=", "<", "<=", ">", ">=" -> emitComparison(code, operator);
```

`MolangString.asFloat()` 永远返回 `0` → `'a'=='b'` 变成 `0.0==0.0` → `true` ❌

**正确代码**：
```java
// == 和 != 使用类型感知的 equalsF/nEqualsF
if ("==".equals(operator)) {
    emitExpr(code, binary.left());
    emitExpr(code, binary.right());
    code.invokeinterface(CD_MOLANG_OBJECT, "equalsF", MethodTypeDesc.of(CD_FLOAT, CD_MOLANG_OBJECT));
    code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
    return;
}
```

`MolangObject.equalsF` → `equals()` → record 自动按字段比较 → 类型不同返回 false ✅

## 运行时验证方法

用 `/eval` 编译并求值表达式验证修复：
```java
io.github.tt432.eyelib.molang.compiler.MolangCompilerImpl compiler = new ...();
compiler.compile("'a' == 'b'", CompileContext.defaults()).evaluate(new MolangScope()).asFloat();
// 预期: 0.0
```

**注意**：`scope.get("query.is_sheared")` 返回 null 是预期行为——query 函数通过 `resolveMemberAccess` 动态解析，不缓存在 scope map 中。
正确查询方式：`MolangRuntimeSupport.resolveMemberAccess(scope, "query.is_sheared")`
