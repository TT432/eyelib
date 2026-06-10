# P3：测试扩展 — 系统化测试覆盖

**Status: ✅ Done / partially superseded** (full pipeline tests created; dual-frontend divergence tests superseded by ANTLR removal — only one frontend remains)

## 问题类型

**测试覆盖不足**：全流水线测试仅3个（全为"1+2"变体），5种AST类型零直接测试，生成解析器与手写解析器零交叉验证。

## 可证明证据

**证据 E1** — 全流水线测试仅有3个：
- 文件：`eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/MolangCompilerImplHandsOnQaTest.java`
- 方法：`compileAndEvaluateSimpleAdditionReturnsThreePointZero()`（仅"1+2"）
- 方法：`compileUnknownFunctionCompilesSuccessfully()`（"1+nonexistent()"）
- 方法：`bytecodeEmitterOutputStartsWithCafeBabeMagic()`（魔数验证）

**证据 E2** — 零比较运算符测试在字节码层面：
- `emitComparison()` 实现了 `<`、`<=`、`>`、`>=`、`==`、`!=` 六个case（`MolangBytecodeEmitter.java` 行297-315）
- 但 `MolangCompilerImplHandsOnQaTest.java` 中**零**个测试验证编译→求值后的比较结果

**证据 E3** — 零交叉验证测试：
- `grep` 搜索 "assertAstEquivalent\|compareParsers\|generatedAndHandwritten" → 无结果
- 全局搜索 "GeneratedParserBackedMolangParserFrontend" 仅在 `MolangBinderTest.java` 和 `MolangCorpusParseRunner.java` 中出现

## 业已验证的解决模式

### 模式 A：运算符表驱动的参数化测试

**来源**：Bazel Starlark `ParserTest.testPrecedence1-5`（Google生产级递归下降解析器）

核心思想：每个运算符有一个测试，构造包含不同优先级运算符混合的表达式，验证顶层AST节点类型。

```java
// 模式模板 — 每个运算符一个测试方法
@Test void testMultiplyBindsTighterThanAdd() {
    BinaryExpr e = (BinaryExpr) parse("1 + 2 * 3");
    assertEquals("+", e.operator());  // 顶层是 +
}
@Test void testComparisonBindsLowerThanAdd() {
    BinaryExpr e = (BinaryExpr) parse("1 + 2 > 3");
    assertEquals(">", e.operator());  // 顶层是 >
}
```

### 模式 B：`@CsvFileSource` 批量测试

**来源**：IrisShaders `ParserTest.java`，使用 CSV 文件批量加载接受/拒绝用例

```java
@ParameterizedTest
@CsvFileSource(resources = "/comparison_operators.csv", delimiter = ';')
void testFullPipelineEvaluation(String expression, double expected) {
    var compiled = compiler.compile(expression, CompileContext.defaults());
    float result = compiled.evaluate(new MolangScope()).asFloat();
    assertEquals(expected, result, 0.0001);
}
```

CSV 内容：
```csv
1 > 2;0.0
2 > 1;1.0
3 == 3;1.0
3 != 3;0.0
```

### 模式 C：双解析器差异测试

**来源**：差分测试文献（Crossy: Cross-language differential testing of JSON parsers）

```java
@ParameterizedTest
@ValueSource(strings = {"1+2", "a>3", "x<=5"})
void generatedAndHandwrittenAgreeOnAcceptReject(String source) {
    boolean genAccepts = generatedParserAccepts(source);
    boolean hwAccepts = handwrittenParserAccepts(source);
    assertEquals(genAccepts, hwAccepts, "Parsers disagree on: " + source);
}

private boolean generatedParserAccepts(String source) {
    return MolangParserFrontends.active().parseExprSet(source).ast().isPresent();
}

private boolean handwrittenParserAccepts(String source) {
    return HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).isPresent();
}
```

## 执行计划

### Step 1：创建 `MolangFullPipelineTest.java`

在 `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/` 下新建测试类，覆盖以下运算符族的 compile→evaluate 全流水线：

**子任务 1a**：字符串字面量
```java
@ParameterizedTest
@CsvSource({
    "'hello', hello",
    "'', ''",
})
void stringLiteralCompilesAndEvaluates(String input, String expected) { }
```

**子任务 1b**：二元算术（+ - * / 含安全除零）
```java
@ParameterizedTest
@CsvSource({
    "1+2, 3.0",
    "5-3, 2.0",
    "4*3, 12.0",
    "8/2, 4.0",
    "5/0, 0.0",  // 安全除零返回0
})
void binaryArithmeticCompilesAndEvaluates(String input, double expected) { }
```

**子任务 1c**：六种比较运算符
```java
@ParameterizedTest
@CsvSource({
    "1 < 2, 1.0",
    "2 < 1, 0.0",
    "2 <= 2, 1.0",
    "3 >= 3, 1.0",
    "3 == 3, 1.0",
    "3 != 3, 0.0",
})
void comparisonOperatorsCompileAndEvaluate(String input, double expected) { }
```

**子任务 1d**：逻辑运算符短路
```java
@ParameterizedTest
@CsvSource({
    "1 && 2, 1.0",
    "0 && 2, 0.0",
    "1 || 0, 1.0",
    "0 || 0, 0.0",
})
void logicalOperatorsShortCircuit(String input, double expected) { }
```

**子任务 1e**：null合并
```java
@ParameterizedTest
@CsvSource({
    "1 ?? 2, 1.0",
    "does_not_exist ?? 42, 42.0",
})
void nullCoalesceWorks(String input, double expected) { }
```

**子任务 1f**：this → 0.0
```java
@Test
void thisEvaluatesToZero() {
    var compiled = compiler.compile("this", CompileContext.defaults());
    assertEquals(0.0f, compiled.evaluate(scope).asFloat(), 0.0001);
}
```

**子任务 1g**：return 在块中
```java
@Test
void returnInBlockEvaluatesToReturnedValue() {
    var compiled = compiler.compile("{ a = 1; return a + 2; }", CompileContext.defaults());
    assertEquals(3.0f, compiled.evaluate(scope).asFloat(), 0.0001);
}
```

### Step 2：创建 `MolangParserFrontendDivergenceTest.java`

```java
class MolangParserFrontendDivergenceTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "1+2", "a.b", "q.foo(1)", "a??b", "v.x=1",
        "1>2", "1<2", "1<=2", "1>=2", "1==2", "1!=2",
        "!a", "-b", "a&&b", "a||b", "a?b:c", "a?b",
        "return 1", "loop(3,{a=1;})", "for_each(t.x,arr,{})",
    })
    void bothFrontendsAgreeOnAcceptReject(String source) {
        boolean genAccepts = parseWithGenerated(source).isPresent();
        boolean hwAccepts = parseWithHandwritten(source).isPresent();
        assertEquals(genAccepts, hwAccepts,
            "Frontend disagreement on: " + source);
    }
}
```

### Step 3：运行验证

```bash
jetbrain_run_gradle_tasks :eyelib-molang:test
```

## 覆盖率目标

| 运算符族 | 修复前测试数 | 修复后测试数 |
|---|---|---|
| 二元算术 | 1个（仅"1+2"） | 6个（+ - * / 除零 取反） |
| 比较 | 0 | 12个（6种 × accept+eval各一） |
| 逻辑 | 0 | 4个（&& || 各二） |
| null合并 | 0 | 2个 |
| this | 0 | 1个 |
| return | 0 | 1个 |
| 字符串 | 0 | 2个 |
| 双前端差异 | 0 | 20+个参数化用例 |

## Check-list

- [ ] Step 1：创建 `MolangFullPipelineTest.java` 并执行全部7个子任务
- [ ] Step 1a：字符串字面量（2个参数化用例）
- [ ] Step 1b：二元算术（5个参数化用例）
- [ ] Step 1c：六种比较运算符（6个参数化用例）
- [ ] Step 1d：逻辑运算符短路（4个参数化用例）
- [ ] Step 1e：null合并（2个参数化用例）
- [ ] Step 1f：this → 0.0（1个测试）
- [ ] Step 1g：return 在块中（1个测试）
- [ ] Step 2：创建 `MolangParserFrontendDivergenceTest.java`（20+个参数化用例）
- [ ] Step 3：全部参数化用例通过
- [ ] `jetbrain_run_gradle_tasks :eyelib-molang:test` 通过
- [ ] 更新 ROADMAP.md Phase 2/3 KRs
