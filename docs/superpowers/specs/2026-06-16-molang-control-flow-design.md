# Molang 控制流语法对齐 — 实现规格

**Date:** 2026-06-16
**Scope:** eyelib-molang 模块语法修缮，Phase 1（控制流）
**Mode:** 净室实现（本文档是唯一信息源，实现者不参考反编译产物）

## 目标

eyelib-molang 的 Parser 已支持 `loop` / `for_each` / `break` / `continue` 语法，但 Binder 标记为 deferred、BytecodeEmitter 为空操作。本次修缮将这 4 个控制流特性完整实现到字节码级别。

## 不在范围内

- 箭头访问 `->` 的跨实体语义（数据层，后续阶段）
- `math.ease_*` 函数库（独立于语法）
- 三元运算符结合性修改（eyelib 当前右结合已与基岩版新版本一致，无需改）

## 语义规格（确定性，非推测）

### loop 表达式

语法：`loop(count_expr, body_block)`

| 场景 | 行为 | 返回值 |
|---|---|---|
| `count_expr` 求值后截断为 int | 运行时求值，`(int) asFloat()` | — |
| `count <= 0` | 不迭代 | `MolangNull` |
| 正常完成（所有迭代执行完） | — | `MolangNull` |
| `break expr` 出现在 body 中 | 终止循环 | `expr` 的求值结果 |
| `break`（无值）出现在 body 中 | 终止循环 | `MolangNull` |
| `continue` / `continue expr` | 执行 expr（如有，仅取副作用），跳到下一次迭代 | 不直接影响 loop 返回值 |

**count_expr 必须在运行时求值**，不再要求编译期常量。

### for_each 表达式

语法：`for_each(variable_name, collection_expr, body_block)`

| 场景 | 行为 | 返回值 |
|---|---|---|
| `collection_expr` 求值结果为 `MolangArray` | 遍历每个元素，绑定到 `variable_name` | — |
| `collection_expr` 求值结果非 `MolangArray` | 不迭代 | `MolangNull` |
| 正常完成 | — | `MolangNull` |
| `break expr` | 终止遍历 | `expr` 的求值结果 |
| `break`（无值） | 终止遍历 | `MolangNull` |
| `continue` / `continue expr` | 执行 expr（如有，仅取副作用），跳到下一个元素 | 不直接影响返回值 |

**variable_name 绑定**：每次迭代将当前元素写入 scope 的 `variable.<name>` 键（与赋值语义一致）。

### break / continue 作用域约束

- 只在 loop / for_each 的 body 内有效
- 在循环外使用 → Binder 报 `BindDiagnostic.Severity.ERROR`
- 嵌套循环时，break / continue 只影响最内层循环

## 数据结构变更

### BoundLoopExpr

```
// 变更前
record BoundLoopExpr(String iterationCountRawText, BoundBlockExpr body, ...)

// 变更后
record BoundLoopExpr(BoundExpr countExpr, BoundBlockExpr body, ...)
```

### BoundForEachExpr

```
record BoundForEachExpr(
    String variableName,           // 绑定的变量名（不含 "variable." 前缀）
    BoundExpr collectionExpr,
    BoundBlockExpr body
    // 移除 deferredReason
)
```

### BoundBreakStmt

```
record BoundBreakStmt(
    BoundExpr valueExpr   // 可为 null（无值 break）
    // 移除 deferredReason
)
```

### BoundContinueStmt

```
record BoundContinueStmt(
    BoundExpr valueExpr   // 可为 null（无值 continue）
    // 移除 deferredReason
)
```

## Binder 变更

### 1. 解除 deferred

`bindForEachExpr` / `bindBreakStmt` / `bindContinueStmt` 不再设置 `deferredReason = UNSUPPORTED_IN_THIS_SLICE`，正常绑定各子表达式。

### 2. loop count 改为表达式

`bindLoopExpr` 不再从原始文本解析 iteration count，而是递归绑定 count 子表达式为 `BoundExpr`。

### 3. 作用域检查

Binder 维护循环作用域栈（`Deque<LoopScope>` 或等效机制）：

- 进入 `bindLoopExpr` / `bindForEachExpr` 的 body 绑定前 push
- body 绑定完成后 pop
- `bindBreakStmt` / `bindContinueStmt` 检查栈非空
  - 栈空 → `BindDiagnostic(ERROR, "break/continue outside of loop")`
  - 栈非空 → 正常绑定

### 4. Parser 变更

`parseLoop` 中 count 部分从保存原始文本改为递归调用 `parseExpression()`，得到 AST 表达式节点。

`parsePrimary` 中 `break` / `continue` 后可选地解析一个表达式作为值（遇到 `;` / `}` / `)` 时停止，表示无值形式）。

## BytecodeEmitter 变更

### LoopContext 栈

Emitter 维护 `Deque<LoopContext>`：

```java
record LoopContext(
    Label continueLabel,    // continue 跳转目标
    Label breakLabel,       // break 跳转目标
    int resultLocalSlot     // 存储 break 带回值的 local 变量槽（-1 = 无带值）
)
```

### loop 编译模式

```
// emit(BoundLoopExpr)
emit(countExpr)                        // 栈: float
f2i
istore counterSlot                     // counterSlot = (int)count

iload counterSlot
ifle endLabel                          // count <= 0 → 结束，返回 null

// 声明 resultLocalSlot（初始 null）
aconst_null
astore resultLocalSlot

push LoopContext(continueLabel=continueTarget, breakLabel=endLabel, resultLocalSlot)

loopStart:
// body
emit(body)
fpop                                   // 丢弃 body 正常返回值

continueTarget:
iinc counterSlot -1
iload counterSlot
ifgt loopStart

pop LoopContext

endLabel:
// 加载结果：如果 break 设置了 resultLocalSlot 则返回它，否则 null
aload resultLocalSlot                  // 初始 null，break 覆盖
areturn
```

> **注意**：resultLocalSlot 初始化为 null（MolangNull.INSTANCE）。如果 break expr 设置了它，则返回该值；如果正常完成或无值 break，它仍是 null。这天然满足语义。

### for_each 编译模式

```
// emit(BoundForEachExpr)
emit(collectionExpr)                   // 栈: MolangObject
astore arraySlot

// 类型检查：非 MolangArray → 返回 null
aload arraySlot
instanceof MolangArray
ifeq endLabel

// 初始化迭代器
aload arraySlot
invokevirtual MolangArray.iterator() / 或直接用 List 的迭代
astore iterSlot

// 声明 resultLocalSlot
aconst_null
astore resultLocalSlot

push LoopContext(continueLabel=continueTarget, breakLabel=endLabel, resultLocalSlot)

loopStart:
aload iterSlot
invokeinterface Iterator.hasNext()
ifeq endLabel

aload iterSlot
invokeinterface Iterator.next()
astore varSlot                         // 绑定到 variable

// body
emit(body)
fpop

continueTarget:
goto loopStart

pop LoopContext

endLabel:
aload resultLocalSlot
areturn
```

### break 编译模式

```
// emit(BoundBreakStmt)
if (valueExpr != null) {
    emit(valueExpr)                    // 栈: value
    astore loopContext.resultLocalSlot // 存入结果槽
}
goto loopContext.breakLabel            // 跳到循环出口
```

### continue 编译模式

```
// emit(BoundContinueStmt)
if (valueExpr != null) {
    emit(valueExpr)                    // 栈: value
    fpop                               // 丢弃（仅为副作用执行）
}
goto loopContext.continueLabel         // 跳到循环头部/迭代步进
```

### 作用域安全

如果 Binder 已正确检查作用域，Emitter 不应遇到循环外的 break/continue。但作为防御，Emitter 可在 LoopContext 栈为空时抛出 `IllegalStateException`。

## 值类型约定

- `MolangNull.INSTANCE` — 循环正常完成的返回值
- `break expr` 的值 — 任意 `MolangObject`，存入 resultLocalSlot
- `body` 的正常返回值 — 丢弃（fpop），不影响循环结果

## 测试规格

### Parser 测试

| 用例 | 预期 AST |
|---|---|
| `loop(3, { break })` | LoopExpr[count=NumberLiteral(3), body=Block[BreakStmt(value=null)]] |
| `loop(t.n, { break 1.0 })` | LoopExpr[count=Identifier(t.n), body=Block[BreakStmt(value=NumberLiteral(1.0))]] |
| `for_each(v, q.arr, { continue v })` | ForEachExpr[var="v", col=QueryAccess(arr), body=Block[ContinueStmt(value=Identifier(v))]] |

### Binder 测试

| 用例 | 预期 |
|---|---|
| 循环外 `break` | ERROR diagnostic |
| 循环外 `continue` | ERROR diagnostic |
| `loop(t.n, ...)` count 是表达式 | 正常绑定，BoundLoopExpr.countExpr 非空 |
| 嵌套循环内层 break | 正常绑定，无 ERROR |

### 集成测试（编译 + 执行）

以下断言中，`eval(expr)` 表示在空 scope（t.* 初始 0）中编译并执行表达式。

```java
// 1. loop 正常完成返回 null
assert eval("loop(3, {})").asFloat() == 0.0f;
assert eval("loop(3, {})") == MolangNull.INSTANCE;

// 2. loop break 带值
assert eval("loop(10, { break 42.0 })").asFloat() == 42.0f;

// 3. loop break 无值
assert eval("loop(10, { break })") == MolangNull.INSTANCE;

// 4. loop count=0 不迭代
assert eval("loop(0, { break 1.0 })") == MolangNull.INSTANCE;

// 5. loop count 为负数不迭代
assert eval("loop(-5, { break 1.0 })") == MolangNull.INSTANCE;

// 6. loop 动态计数（运行时求值）
assert eval("t.n = 3; loop(t.n, { t.x = t.x + 1 }); t.x").asFloat() == 3.0f;

// 7. continue 跳过后续语句
assert eval("t.x = 0; loop(5, { continue; t.x = t.x + 1 }); t.x").asFloat() == 0.0f;

// 8. continue expr 副作用生效
assert eval("t.x = 0; loop(5, { continue (t.x = t.x + 1) }); t.x").asFloat() == 5.0f;

// 9. break 在第 N 次迭代触发
assert eval("t.i = 0; loop(10, { t.i = t.i + 1; t.i > 3 ? { break t.i } : 0 }); t.i").asFloat() == 4.0f;

// 10. for_each 遍历求和（需 scope 预置数组）
// scope 预置 variable.arr = MolangArray[1.0, 2.0, 3.0]
// assert eval("t.sum = 0; for_each(v, t.arr, { t.sum = t.sum + v }); t.sum").asFloat() == 6.0f;

// 11. for_each 非数组返回 null
assert eval("for_each(v, 42.0, {})") == MolangNull.INSTANCE;

// 12. for_each break
// scope 预置 variable.arr = MolangArray[1.0, 2.0, 3.0, 4.0, 5.0]
// assert eval("for_each(v, t.arr, { v > 3 ? { break v } : 0 })").asFloat() == 4.0f;

// 13. 嵌套循环，break 只跳出内层
assert eval("t.x = 0; loop(3, { loop(3, { break }); t.x = t.x + 1 }); t.x").asFloat() == 3.0f;

// 14. 嵌套循环，外层 break 带值穿透内层
assert eval("loop(3, { loop(3, {}); break 7.0 })").asFloat() == 7.0f;
```

> 测试 10/12 需要预置 scope 数组。如果当前测试框架不支持预置 MolangArray，可跳过或用替代方式构造。

## 文件影响范围

| 文件 | 变更 |
|---|---|
| `BoundLoopExpr.java` | 字段：`iterationCountRawText: String` → `countExpr: BoundExpr` |
| `BoundForEachExpr.java` | 移除 `deferredReason` |
| `BoundBreakStmt.java` | 移除 `deferredReason`，新增 `valueExpr: BoundExpr`（nullable） |
| `BoundContinueStmt.java` | 移除 `deferredReason`，新增 `valueExpr: BoundExpr`（nullable） |
| `MolangBinder.java` | 解除 deferred、作用域检查栈、loop count 改为绑定表达式 |
| `BytecodeEmitter.java` | LoopContext 栈、4 个控制流编译模式 |
| `MolangParser.java` | loop count 改为 parseExpression()、break/continue 可选值表达式 |
| 新增测试文件 | Parser/Binder/集成测试 |

## 验收标准

1. 所有现有 molang 测试仍通过（无回归）
2. 上述集成测试用例 1-9、13-14 全部通过
3. `break` / `continue` 在循环外使用时 Binder 报 ERROR
4. `loop(t.n, ...)` 动态计数正确工作
5. 编译产物（defineHiddenClass 生成的类）无 JVM 字节码验证错误
