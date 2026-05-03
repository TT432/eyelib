# P1：运算符完整性 — 补全缺失的比较运算符

## 问题类型

**解析器完整性缺陷**：手写递归下降解析器实现的运算符集与参考语法（ANTLR Molang.g4）不一致。

## 可证明证据

**证据链 E1** — 手写词法分析器 `TokenKind` 枚举：
- 文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/HandwrittenMolangAstParserFrontend.java`
- 行602-633：定义25种TokenKind，包含 `GREATER`、`EQUAL_EQUAL`、`BANG_EQUAL`，**不包含 `LESS`、`LESS_EQUAL`、`GREATER_EQUAL`**

**证据链 E2** — 手写解析器 `parseComparison()` 方法：
- 文件同上，行164-172：
```java
while (match(TokenKind.GREATER, TokenKind.EQUAL_EQUAL, TokenKind.BANG_EQUAL)) {
```
仅匹配三种比较运算符。

**证据链 E3** — ANTLR语法中的比较运算符：
- 文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/MolangParser.java`
- 行19-24：`T__11='<'`, `T__12='<='`, `T__13='>='`, `T__14='>'`
- 行638-653：`ComparisonOperatorContext` 匹配全部四种运算符

**证据链 E4** — 字节码发射器已就绪：
- 文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangBytecodeEmitter.java`
- 行297-315：`emitComparison()` 方法包含 `<`、`<=`、`>`、`>=` 四个case：
```java
case "<" -> code.iflt(trueLabel);
case "<=" -> code.ifle(trueLabel);
case ">" -> code.ifgt(trueLabel);
case ">=" -> code.ifge(trueLabel);
```

**结论**：瓶颈纯粹在词法/解析层，字节码层已完备。

## 业已验证的解决模式

### 模式：运算符表驱动验证

**核心思想**：不依赖人工记忆哪些运算符已实现，而是用数据表定义"应支持的运算符 → 对应应产生的AST结构"，然后对每个表项生成参数化测试。

**通用模式（语言无关）**：
```
OperatorTable = [
    { input: "a < b",  operator: "<",  astType: BinaryExpr, precedence: comparison },
    { input: "a <= b", operator: "<=", astType: BinaryExpr, precedence: comparison },
    { input: "a > b",  operator: ">",  astType: BinaryExpr, precedence: comparison },
    { input: "a >= b", operator: ">=", astType: BinaryExpr, precedence: comparison },
    { input: "a == b", operator: "==", astType: BinaryExpr, precedence: equality },
    { input: "a != b", operator: "!=", astType: BinaryExpr, precedence: equality },
]

for each row in OperatorTable:
    test: parse(row.input) → assert ast matches row.astType
    test: parse(row.input) → assert operator == row.operator
```

### 模式：差异测试（Differential Testing）

**核心思想**：将同一输入同时送入两个解析器实现（ANTLR生成 + 手写），断言产出等价AST结构。

```java
@ParameterizedTest
@ValueSource(strings = {"1 < 2", "a <= b", "x >= 0"})
void generatedAndHandwrittenProduceEquivalentAst(String source) {
    var generatedResult = MolangParserFrontends.active().parseExprSet(source);
    var handwrittenAst = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source);
    // Assert structural equivalence
    assertAstEquivalent(generatedResult.exprSet(), handwrittenAst.orElseThrow());
}
```

## 执行计划

### Step 1：编写失败测试（参数化运算符表）

在 `HandwrittenMolangAstParserFrontendTest.java` 中添加：

```java
@ParameterizedTest
@CsvSource({
    "1 < 2,  <, BinaryExpr",
    "1 <= 2, <=, BinaryExpr",
    "1 >= 2, >=, BinaryExpr",
    "1 > 2,  >, BinaryExpr",
    "1 == 2, ==, BinaryExpr",
    "1 != 2, !=, BinaryExpr",
})
void parsesAllSixComparisonOperators(String source, String operator, String astType) {
    var result = parse(source);
    assertTrue(result.isPresent(), "Should parse: " + source);
    var exprSet = result.orElseThrow();
    // 验证产生了正确的AST类型和运算符
    // ...
}
```

运行测试 → 预期 `<`、`<=`、`>=` 三个用例失败。

### Step 2：补全词法分析器

在 `TokenKind` 枚举中添加三个新TokenKind：
```java
LESS,           // <
LESS_EQUAL,     // <=
GREATER_EQUAL,  // >=
```

在 `readPunctuationOrOperator()` 方法中添加：
```java
if (match("<=")) {
    return token(TokenKind.LESS_EQUAL, "<=", startIndex, startLine, startColumn, 2);
}
if (match(">=")) {
    return token(TokenKind.GREATER_EQUAL, ">=", startIndex, startLine, startColumn, 2);
}
```

在单字符 switch 中添加 `<`：
```java
case '<' -> token(TokenKind.LESS, "<", startIndex, startLine, startColumn, 1);
```

### Step 3：补全解析器

修改 `parseComparison()` 的 match 调用：
```java
// 修改前：
while (match(TokenKind.GREATER, TokenKind.EQUAL_EQUAL, TokenKind.BANG_EQUAL)) {

// 修改后：
while (match(TokenKind.LESS, TokenKind.LESS_EQUAL, TokenKind.GREATER, TokenKind.GREATER_EQUAL, TokenKind.EQUAL_EQUAL, TokenKind.BANG_EQUAL)) {
```

### Step 4：验证

运行：`jetbrain_run_gradle_tasks :eyelib-molang:test`
预期：六个参数化测试全部通过 + 现有测试无回归。

### Step 5：添加差异测试

新增测试类 `MolangParserFrontendDivergenceTest.java`：
- 对相同的输入同时调佣生成解析器 + 手写解析器
- 断言两个解析器的parse-accept/reject行为一致
- 对共通的输入断言AST结构等价

## Check-list

- [ ] Step 1：参数化比较运算符测试（6个用例），预期3个失败
- [ ] Step 2：TokenKind 枚举添加 LESS、LESS_EQUAL、GREATER_EQUAL
- [ ] Step 2：readPunctuationOrOperator() 添加 "<=", ">=", "<" 分支
- [ ] Step 3：parseComparison() 的 match 调用包含全部6个运算符
- [ ] Step 4：`jetbrain_run_gradle_tasks :eyelib-molang:test` 通过
- [ ] Step 5：差异测试类创建并通过
- [ ] 更新 ROADMAP.md Phase 2 KR 状态
