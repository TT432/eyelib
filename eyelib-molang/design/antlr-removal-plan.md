# ANTLR 去除计划：Molang 模块完全手写化

## 状态

**Phase**: Implementation  
**Date**: 2026-06-09  
**Author**: TT432 (via agent)

## 1. 现状分析

### 1.1 当前架构

```
eyelib-molang/
├── compiler/
│   ├── frontend/
│   │   ├── MolangParserFrontend.java          ← 接口（含 ANTLR 类型签名）
│   │   ├── MolangParserFrontendResult.java     ← 结果（含 ANTLR 字段）
│   │   ├── MolangParserFrontends.java          ← 选择器（返回手写前端）
│   │   ├── HandwrittenMolangAstParserFrontend.java ← ✅ 手写解析器（已激活）
│   │   ├── GeneratedParserBackedMolangParserFrontend.java ← ❌ ANTLR 包装（未激活）
│   │   └── ast/
│   │       ├── MolangAst.java                  ← AST 节点定义
│   │       └── SourceSpan.java                 ← 源码位置
│   ├── binding/                                ← 语义绑定
│   ├── MolangCompilerImpl.java                 ← 编译管线
│   ├── MolangBytecodeEmitter.java              ← JVM 字节码生成
│   └── cache/                                  ← 编译缓存
└── generated/                                  ← ❌ ANTLR 生成代码（未激活）
    ├── MolangLexer.java
    ├── MolangParser.java
    ├── MolangVisitor.java
    ├── MolangBaseVisitor.java
    └── package-info.java
```

### 1.2 激活路径

`MolangParserFrontends.active()` → `HandwrittenMolangAstParserFrontend.INSTANCE`

ANTLR 生成代码**未被任何活跃路径使用**。`GeneratedParserBackedMolangParserFrontend` 存在但未被注入。

### 1.3 手写解析器能力矩阵

手写 `HandwrittenMolangAstParserFrontend`（~670 行）已覆盖 MoLang 全部语法：

| 语法特性 | 状态 | 实现 |
|---------|------|------|
| 数字字面量（含科学计数法） | ✅ | `Tokenizer.readNumber()` |
| 字符串字面量 | ✅ | `Tokenizer.readString()` |
| 标识符（含关键字识别） | ✅ | `Tokenizer.readIdentifier()` |
| 一元运算符 `-` `!` | ✅ | `Parser.parseUnary()` |
| 二元运算符 `+ - * /` | ✅ | `Parser.parseAdd()`, `parseMultiply()` |
| 比较运算符 `< <= > >= == !=` | ✅ | `Parser.parseComparison()` |
| 逻辑运算符 `&&` `\|\|` | ✅ | `Parser.parseAnd()`, `parseOr()` |
| 三元条件 `? :` | ✅ | `Parser.parseTernary()` |
| 二元条件 `?` | ✅ | `Parser.parseTernary()` |
| 空值合并 `??` | ✅ | `Parser.parseNullCoalesce()` |
| 赋值 `=` | ✅ | `Parser.parseAssignment()` |
| 成员访问 `.` | ✅ | `Parser.parsePostfix()` |
| 箭头访问 `->` | ✅ | `Parser.parsePostfix()` |
| 函数调用 `()` | ✅ | `Parser.parsePostfix()` |
| 索引访问 `[]` | ✅ | `Parser.parsePostfix()` |
| 分组 `()` | ✅ | `Parser.parsePrimary()` |
| 块表达式 `{}` | ✅ | `Parser.parsePrimary()` |
| `this` | ✅ | `Parser.parsePrimary()` |
| `return` | ✅ | `Parser.parseStatements()` |
| `break` / `continue` | ✅ | `Parser.parseStatements()` |
| `loop()` | ✅ | `Parser.parseLoopControlForm()` |
| `for_each()` | ✅ | `Parser.parseForEachControlForm()` |
| 语句分隔 `;` | ✅ | `Parser.parseStatements()` |

## 2. MoLang 语言特征分析

基于 Bedrock 官方文档和 .mcpack 数据：

### 2.1 语言分类

- **范式**：表达式优先的弱类型脚本语言
- **类型系统**：动态类型，核心为单精度浮点数。布尔值 = 0.0/1.0。支持字符串、结构体、实体引用。
- **语法族**：类 C 语法，区分大小写不敏感（字符串除外）
- **求值模型**：严格求值，默认返回 0.0

### 2.2 语法特点

1. **表达式优先**：单行简单表达式是主要使用方式（`math.sin(q.anim_time * 1.23)`）
2. **复杂表达式**：支持多语句 + `return`（`t.a = ...; t.b = ...; return t.b + 1;`）
3. **作用域根**：4 个命名空间根（`query/q`、`variable/v`、`temp/t`、`context/c`），带别名解析
4. **无类型声明**：结构体隐式创建（`v.location.x = 1`）
5. **低歧义语法**：运算符优先级固定，无用户自定义运算符/类型
6. **控制流受限**：仅 `loop`/`for_each`/`break`/`continue`，无 `if`（用 `?` 替代）

### 2.3 适合手写解析的原因

- 语法规模小（~15 条产生式规则）
- 无歧义（固定优先级，无悬空 else 问题）
- 无左递归（自然适合递归下降）
- 运算符优先级固定且层次清晰（13 级）
- Bedrock 语法非正式文法驱动，而是示例驱动——手写解析器更易演进

## 3. 编译器架构（已实现）

### 3.1 管线

```
Source Text → Tokenizer → Parser → AST → Binder → Bytecode Emitter → JVM Class
                (手写)    (递归下降)  (记录类型) (语义解析)  (ASM-free)    (HiddenClass)
```

### 3.2 各阶段职责

| 阶段 | 输入 | 输出 | 关键决策 |
|------|------|------|---------|
| **Tokenizer** | `String` | `List<Token>` | 上下文无关，按字符分类 |
| **Parser** | `List<Token>` | `MolangAst.ExprSet` | 递归下降 + 优先级爬升 |
| **Binder** | `MolangAst.ExprSet` | `BindResult` | 别名规范化、标识符解析、赋值验证 |
| **Bytecode** | `BoundMolangCompilerInput` | `byte[]` | JVM 字节码直接生成，无 ASM 依赖 |
| **Loader** | `byte[]` | `CompiledMolangExpression` | `MethodHandles.Lookup.defineHiddenClass()` |

### 3.3 设计原则

1. **分层清晰**：Tokenizer → Parser → AST → Binder → Bytecode，每层独立可测试
2. **无外部依赖**：不使用 ANTLR/ASM/Javassist 等代码生成库
3. **AST 不可变**：所有节点为 Java record，线程安全
4. **源码位置保留**：每个 AST 节点携带 `SourceSpan` 用于诊断
5. **类型安全**：Expr vs Stmt 分离，编译期防止将语句放入表达式位置

## 4. 清理计划

### 4.1 删除清单

| # | 文件 | 原因 |
|---|------|------|
| 1 | `generated/MolangLexer.java` | ANTLR 生成 |
| 2 | `generated/MolangParser.java` | ANTLR 生成 |
| 3 | `generated/MolangVisitor.java` | ANTLR 生成 |
| 4 | `generated/MolangBaseVisitor.java` | ANTLR 生成 |
| 5 | `generated/package-info.java` | 随目录删除 |
| 6 | `compiler/frontend/GeneratedParserBackedMolangParserFrontend.java` | ANTLR 包装器，未激活 |

### 4.2 修改清单

| # | 文件 | 变更 |
|---|------|------|
| 7 | `build.gradle` | 移除 `org.antlr:antlr4-runtime:4.9.1` |
| 8 | `MolangParserFrontend.java` | 移除 `Consumer<MolangLexer>` 和 `Consumer<MolangParser>` 参数 |
| 9 | `MolangParserFrontendResult.java` | 移除 `MolangParser parser` 和 `ExprSetContext exprSet` 字段 |
| 10 | `HandwrittenMolangAstParserFrontend.java` | 移除 ANTLR import，简化 `parseExprSet` |
| 11 | `MolangParserFrontends.java` | 更新注释，移除对 dead code 的引用 |
| 12 | `MolangCorpusParseRunner.java` | 完全移除 ANTLR 依赖，重构为纯 AST 驱动 |
| 13 | `README.md` / `README.cn.md` | 更新 antlr-molang 引用 |
| 14 | `ROADMAP.md` | 更新 Phase 2 状态，标记 generated parser 已移除 |
| 15 | `MODULES.md` | 更新 Molang generated parser 条目 |
| 16 | `design/molang-ast-and-semantics-draft.md` | 更新状态标记 |
| 17 | `refactor-plan/*.md` | 更新对 GeneratedParserBacked 的引用 |
| 18 | `docs/decisions/0004-generated-code-policy.md` | 移除或更新 Molang 相关生成代码策略 |

### 4.3 不修改的文件

- `MolangCompilerImpl.java` — 使用 `MolangParserFrontends.active().parseExprSet(source)` 0-arg 重载，无需改
- 所有 binding/ bytecode/ cache 文件 — 不依赖 ANTLR
- 除 `MolangCorpusParseRunner` 外的所有测试文件 — 使用 0-arg 重载

## 5. 验证计划

### 5.1 编译验证

```bash
jetbrain_build_project                    # IntelliJ 编译
jetbrain_run_gradle_tasks :eyelib-molang:compileJava  # Gradle 编译
```

### 5.2 单元测试

```bash
jetbrain_run_gradle_tasks :eyelib-molang:test
```

关键测试套件：
- `HandwrittenMolangAstParserFrontendTest` — 20+ 解析器验收/拒绝用例
- `MolangParserFrontendDivergenceTest` — 前端一致性（清理后需调整）
- `MolangFullPipelineTest` — 端到端编译管线（7 族：算术、比较、逻辑、空值合并、字符串、this、return-in-block）
- `MolangBinderTest` — 语义绑定
- `MolangCorpusHarnessTest` — 语料库测试（33+ 行）
- `MolangSpecTest` — 规格测试

### 5.3 Clientsmoke 集成测试

```bash
eyelib_debug_clientsmoke(timeout=120)
```

验证 MC 运行环境中 MoLang 表达式编译和执行正确。

### 5.4 成功标准

- [ ] `:eyelib-molang:compileJava` 通过（无 ANTLR 依赖）
- [ ] `:eyelib-molang:test` 全部通过
- [ ] `jetbrain_build_project` 全项目编译通过
- [ ] Clientsmoke 测试通过
- [ ] `.jar` 中不含 `org/antlr/**` 类
- [ ] `.jar` 中不含 `generated/` 包
- [ ] 搜索全项目无 `import org.antlr` 引用
- [ ] 搜索全项目无 `import ...generated.Molang` 引用

## 6. 回滚点

- Commit 前：所有 ANTLR 文件可通过 git 恢复
- 建议在删除 generated/ 目录前先创建 commit 保存当前状态

## 7. 风险评估

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| MolangCorpusParseRunner 重构引入 bug | 中 | 低 | 有语料库测试覆盖 |
| 其他模块间接依赖 ANTLR | 低 | 中 | 全局搜索确认无引用 |
| MolangParserFrontendDivergenceTest 需要重写 | 高 | 低 | 标记为删除（两个前端对比测试，清理后无意义） |
| Gradle 缓存导致旧类残留 | 低 | 低 | clean build |
