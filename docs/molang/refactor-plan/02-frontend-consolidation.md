# P2：前端统一 — 消除 active() 与生产路径的脱钩

**Status: ✅ Superseded** (ANTLR entirely removed; `GeneratedParserBackedMolangParserFrontend` deleted; handwritten parser is the sole frontend)

## 问题类型

**架构耦合缺陷**：`MolangParserFrontends.active()` 返回 `GeneratedParserBackedMolangParserFrontend`，但生产编译器 `MolangCompilerImpl` 直接调用 `HandwrittenMolangAstParserFrontend.INSTANCE`，完全绕过 `active()`。

## 可证明证据

**证据链 E1** — `active()` 定义：
- 文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/MolangParserFrontends.java`
- 行4：`private static final MolangParserFrontend ACTIVE = GeneratedParserBackedMolangParserFrontend.INSTANCE;`

**证据链 E2** — 生产编译器绕过 `active()`：
- 文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompilerImpl.java`
- 行26：`HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(expression)`

**证据链 E3** — 测试代码使用 `active()`：
- 文件：`eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java`
- 行288：`MolangParserFrontends.active().parseExprSet(source)`

**后果**：
- 修改 `MolangParserFrontends.ACTIVE` 对生产行为零影响
- 测试使用 `active()` 但生产不使用 — 测试与生产使用不同代码路径
- ANTLR 生成的 `ExprSetContext` 在 `GeneratedParserBackedMolangParserFrontend` 中被计算但永不消费 ~~（历史：该前端已随 ANTLR 移除而删除）~~

## 业已验证的解决模式

### 模式：单一事实来源（Single Source of Truth）

**核心思想**：所有调用方通过同一个入口获取解析器实例，不存在"有时用这个、有时用那个"的路径。

**Java 实现**：
```java
public final class MolangParserFrontends {
    // 只有一个静态字段，没有"备用路径"
    private static final MolangParserFrontend ACTIVE = 
        HandwrittenMolangAstParserFrontend.INSTANCE;
    
    public static MolangParserFrontend active() {
        return ACTIVE;
    }
}
```

所有调用方（编译器 + 测试 + 常量化器）统一通过 `MolangParserFrontends.active()`。

### 模式：架构测试（ArchUnit）

**核心思想**：用自动化测试强制执行架构约束，例如"`MolangCompilerImpl` 不得直接引用 `HandwrittenMolangAstParserFrontend`"。

```java
@Test
void compilerMustUseParserFrontendsActive() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("io.github.tt432.eyelibmolang.compiler");
    
    archRule.check(classes);
}
```

## 执行计划

### Step 1：将所有调用方统一到 `active()`

逐一检查并修改所有直接调用 `HandwrittenMolangAstParserFrontend.INSTANCE` 的位置：

**生产代码**：
- `MolangCompilerImpl.java` 第26行：改为 `MolangParserFrontends.active().parseExprSet(source).ast()`

**测试代码**：
- `MolangBinderTest.java`：已使用 `active()` ✅ 无需修改
- `HandwrittenMolangAstParserFrontendTest.java`：保留直接调用（这是手写前端的单元测试，合理）

### Step 2：评估 `active()` 的值

当前 `active()` 指向 `GeneratedParserBackedMolangParserFrontend`，但由于第31行的实现：
```java
return new MolangParserFrontendResult(
    parser, exprSet,
    HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source)  // ← 间接调用手写前端
);
```

它实际上也是通过手写前端产生 AST。因此：
- **选项A**：将 `active()` 直接指向 `HandwrittenMolangAstParserFrontend.INSTANCE`，删除 `GeneratedParserBackedMolangParserFrontend` 中的重复 AST 生成
- **选项B**：保留 `GeneratedParserBackedMolangParserFrontend` 用于语料测试中的 parse shape 收集

**推荐选项B**：语料测试需要 ANTLR parse tree 做 shape verification。保留双前端但通过统一入口访问。
~~（历史：ANTLR 已删除，该建议不再适用；语料测试仅使用手写前端）~~

### Step 3：修改 `MolangCompilerImpl` 使用 `active()`

```java
// 修改前：
MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE
    .parseExprSetAst(expression)
    .orElseThrow(...);

// 修改后：
MolangParserFrontendResult result = MolangParserFrontends.active()
    .parseExprSet(expression);
MolangAst.ExprSet ast = result.ast()
    .orElseThrow(...);
```

### Step 4：验证

运行：`jetbrain_run_gradle_tasks :eyelib-molang:test`
预期：所有现有测试通过，生产编译器通过统一入口获取解析器。

## Check-list

- [ ] Step 1：审计所有 `HandwrittenMolangAstParserFrontend.INSTANCE` 的直接引用
- [ ] Step 2：评估 `active()` 应指向哪个实现（推荐选项B）
- [ ] Step 3：修改 `MolangCompilerImpl.java` 第26行使用 `active()`
- [ ] Step 3：修改 `MolangConstantExpressionEvaluator.java`（如果存在同样问题）
- [ ] Step 4：`jetbrain_run_gradle_tasks :eyelib-molang:test` 通过
- [ ] 更新 ROADMAP.md 相关条目
