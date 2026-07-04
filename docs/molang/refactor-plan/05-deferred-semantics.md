# P5：延迟语义 — 处理解析但未实现的构造

## 问题类型

**语义完整性缺陷**：6种AST构造在手写前端正确解析、在绑定层被标记为 `UNSUPPORTED_IN_THIS_SLICE`、在字节码层静默返回 `MolangNull.INSTANCE`，且无运行时测试。

## 可证明证据

### 证据 E1 — 绑定层标记延迟

文件：`src/main/java/io/github/tt432/eyelib/molang/compiler/binding/MolangBinder.java`

| 行号 | AST输入 | 绑定输出 | 延迟原因 |
|---|---|---|---|
| 117-118 | `TernaryConditionalExpr` | `BoundDeferredExpr(UNSUPPORTED_IN_THIS_SLICE)` | 三元条件未实现 |
| 119-120 | `BinaryConditionalExpr` | `BoundDeferredExpr(UNSUPPORTED_IN_THIS_SLICE)` | 二元条件未实现 |
| 166-177 | `LoopExpr` | `BoundLoopExpr(deferredReason=UNSUPPORTED)` | 循环未实现 |
| 180-194 | `ForEachExpr` | `BoundForEachExpr(deferredReason=UNSUPPORTED)` | for_each未实现 |
| 137-139 | `BreakStmt` | `BoundBreakStmt(deferredReason=UNSUPPORTED)` | break未实现 |
| 141-143 | `ContinueStmt` | `BoundContinueStmt(deferredReason=UNSUPPORTED)` | continue未实现 |

### 证据 E2 — 字节码层静默返回null

文件：`src/main/java/io/github/tt432/eyelib/molang/compiler/MolangBytecodeEmitter.java`

行187-188：
```java
} else if (expr instanceof BoundMolang.BoundUnknownExpr || expr instanceof BoundMolang.BoundDeferredExpr) {
    code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
}
```

行177-184（循环）：
```java
} else if (expr instanceof BoundMolang.BoundLoopExpr loopExpr) {
    if (loopExpr.deferredReason() != null) {
        code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);  // 静默null
```

### 证据 E3 — NORMAL 模式下零诊断

行211-230：`addDeferredNote()` 仅在 STRICT 模式产生 WARNING，在 DEBUG 模式产生 INFO。NORMAL 模式下**零诊断**。

**后果**：用户写 `a > b ? 1 : 0` → 编译通过 → 求值得到 `null`（而非1或0）→ 无任何提示。

### 证据 E4 — 箭头访问字节码存根

行161-164：
```java
emitExpr(code, arrowAccessExpr.left());
code.pop();           // 左侧值被丢弃
emitExpr(code, arrowAccessExpr.right());  // 仅右侧求值
```

`v.other->v.x` 的语义是"跨实体访问"，当前实现是"丢弃左侧，返回右侧"。

## 问题分类

这个问题不能通过简单的代码补丁解决，因为每个延迟构造都需要设计决策。

### 短期（本计划范围内）：提高诊断可见性

当前行为：延迟构造静默失败。修复：在 NORMAL 模式下至少产生 INFO 诊断，让开发者知道他们的表达式被静默替换为 null。

### 长期（需要设计决策）：决定每个构造的去留

| 构造 | 选项A：实现语义 | 选项B：移除解析器支持 |
|---|---|---|
| 三元 `?:` | 实现条件分支字节码 | 从 handWritten parser 中移除，产生解析错误 |
| 二元 `?` | 实现短格式条件字节码 | 移除 |
| `loop()` | 实现循环字节码 | 移除 |
| `for_each()` | 实现迭代字节码 | 移除 |
| `break`/`continue` | 实现控制流字节码 | 移除 |
| `->` 箭头 | 实现跨实体访问 | 移除，产生解析错误 |

## 业已验证的解决模式

### 模式：功能能力注册表（Capability Registry）

**核心思想**：不是"解析所有，绑定延迟，字节码返回null"，而是显式注册系统支持哪些构造。不支持的构造在绑定阶段产生 ERROR（而非延迟到运行时静默null）。

```java
public enum MolangCapability {
    TERNARY_CONDITIONAL,
    BINARY_CONDITIONAL,
    LOOP,
    FOR_EACH,
    BREAK,
    CONTINUE,
    ARROW_ACCESS
}

public final class MolangCapabilityRegistry {
    private final Set<MolangCapability> enabled = EnumSet.noneOf(MolangCapability.class);
    
    public boolean isSupported(MolangCapability capability) {
        return enabled.contains(capability);
    }
    
    // 在绑定层使用：
    // if (!capabilityRegistry.isSupported(TERNARY_CONDITIONAL)) {
    //     error("三元条件在当前版本中未实现");
    // }
}
```

### 模式：渐进式类型方法（Gradual Typing）

来自 TypeScript/Python 类型检查器的模式：明确声明"已知不支持的构造"，在编译时产生警告，运行时提供明确的错误信息而非静默null。

## 执行计划

### Step 1：NORMAL 模式至少产生 WARNING（即刻修复）

修改 `MolangBinder.addDeferredNote()` — 在 NORMAL 模式下也产生 WARNING 级诊断：

```java
// 修改后 — NORMAL 模式也产生 WARNING
private void addDeferredNote(BindingState state, SourceSpan span, 
                              BindDeferredNote.Reason reason, String sourceFamily) {
    state.deferredNotes.add(new BindDeferredNote(span, reason, sourceFamily));
    // 始终产生 WARNING，而不仅限 STRICT
    state.diagnostics.add(new BindDiagnostic(
        span,
        BindDiagnostic.Severity.WARNING,
        "BIND_DEFERRED_" + reason.name(),
        "构造 '" + sourceFamily + "' 在当前版本中未实现，求值返回 null。原因：" + reason
    ));
}
```

### Step 2：添加延迟构造的运行时求值测试

在 `MolangFullPipelineTest.java`（来自P3）中添加：

```java
@ParameterizedTest
@ValueSource(strings = {
    "a > b ? 1 : 0",       // 三元
    "a > b ? 1",            // 二元条件
    "loop(3, {a=1;})",     // 循环
    "for_each(t.x, arr, {})" // for_each
})
void deferredConstructsProduceNullAtRuntime(String source) {
    var compiled = compiler.compile(source, CompileContext.defaults());
    // 当前预期：返回 null
    assertTrue(compiled.evaluate(scope) instanceof MolangNull);
}
```

### Step 3：为箭头访问添加语义注释

在 `MolangBytecodeEmitter.emitArrowAccessExpr()` 上方添加明确的注释和TODO：

```java
/**
 * 箭头访问 (->) 当前实现为存根。
 * 
 * 预期语义：左表达式求值为宿主实体引用，
 * 右表达式在该宿主上下文中求值。
 * 
 * 当前行为：左表达式求值后丢弃，仅右表达式求值。
 * 
 * TODO(Phase 4): 实现 HostContext 切换以支持跨实体访问。
 */
```

### Step 4：运行验证

```bash
eyelib_debug_test
```

### 决策点

完成 Step 1-3 后，需要对每个延迟构造做出设计决策。建议按以下顺序处理：

1. **三元条件 `?:`** — 最常用，应优先实现
2. **null合并 `??`** — 已实现 ✅
3. **二元条件 `?`** — 使用频率低于三元
4. **`loop()`** — 需要循环控制流
5. **`for_each()`** — 需要迭代器模式
6. **`break`/`continue`** — 依赖循环实现
7. **`->` 箭头** — 需要 HostContext 基础设施

## Check-list

- [ ] Step 1：NORMAL 模式对延迟构造产生 WARNING
- [ ] Step 2：延迟构造运行时返回 null 的测试
- [ ] Step 3：箭头访问代码添加语义注释
- [ ] Step 4：`eyelib_debug_test` 通过
- [ ] 决策：三元条件 `?:` → 实现或移除解析器支持
- [ ] 决策：二元条件 `?` → 实现或移除
- [ ] 决策：`loop()` → 实现或移除
- [ ] 决策：`for_each()` → 实现或移除
- [ ] 决策：`break`/`continue` → 实现或移除
- [ ] 决策：`->` 箭头 → 实现真正的跨实体访问
- [ ] 更新 ROADMAP.md Phase 3 延迟相关 KRs
