# Molang Scope 在 /eval 线程中的调试限制

## 核心机制

### 标识符 canonicalization

手写词法分析器的 `isIdentifierPart` 不含 `.`（点），因此 `q.variant` 被 token 化为三部分：`q` `.` `variant`。Parser 的 `parsePostfix` 将其组装为 `MemberAccessExpr(Identifier("q"), "variant")`。

Binder 的 `normalizeIdentifier` 通过 `MolangRootAliasCanonicalizer` 将短根展开：
- `q` → `query`
- `v` → `variable`
- `t` → `temp`
- `c` → `context`

最终 `memberAccessName()` 用 `.` 拼接成完整名如 `"query.variant"`。

### MemberAccessExpr 的运行时求值

编译后调用 `MolangRuntimeSupport.resolveMemberAccess(scope, "query.variant")`，分三步：

1. **scope.get("query.variant")** — 优先检查 scope 缓存（脚本赋值过的变量在这里）
2. **mappingTree.findField("query.variant")** — 查找 `@MolangQuery` 静态字段
3. **mappingTree.selectQueryVariant("query.variant", ...)** — 查找 `@MolangQuery` 方法并调用

### /eval 线程的限制

第三步（query 方法调用）需要 `HostContext` 中有实体/EntityRenderer 等宿主对象。`/eval` 线程是 Janino 编译线程，**没有 render 线程的宿主上下文**。

结果：
- `v.xxx`（变量赋值）→ 步骤 1 命中 scope 缓存 → 正确返回 ✅
- `q.xxx`（query 函数）→ 步骤 1 未命中（无 render 线程写入）→ 步骤 3 失败（无 host context）→ 返回 `MolangNull(0)` ❌

### 正确的 /eval 测试姿势

```java
// ✅ 正确：查脚本赋值的变量
scope.get("variable.cdrzno");   // = scope 中 canonical key
scope.get("variable.aybhly");

// ❌ 错误：查 query 函数
scope.get("q.variant");         // 原始 key，不会命中
new MolangValue("q.variant").method().apply(scope);  // 走 resolveMemberAccess，无 host context → 0
```

### 赋值操作的 key

`v.cdrzno = 1` 编译后调用 `scope.set("variable.cdrzno", value)`，key 已经过 canonicalization。**读取时必须用同样的 canonical key**。

## 相关代码路径

- `HandwrittenMolangAstParserFrontend.Tokenizer.isIdentifierPart()` — 不含 `.`
- `HandwrittenMolangAstParserFrontend.Parser.parsePostfix()` — 组装 MemberAccessExpr
- `MolangRootAliasCanonicalizer.canonicalizeRoot()` — q→query, v→variable
- `MolangRuntimeSupport.resolveMemberAccess()` — 三步求值链路
- `MolangBytecodeEmitter.memberAccessName()` — 拼接 canonical 完整名
