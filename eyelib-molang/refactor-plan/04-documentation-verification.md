# P4：文档验证 — 消除文档漂移

## 问题类型

**文档-代码漂移**：ROADMAP.md 声称存在的文件实际不存在，路径不准确，状态描述与代码现实矛盾。

## 可证明证据

### 证据 E1 — ROADMAP 声称但缺失的3个文件

文件：`eyelib-molang/ROADMAP.md`，第122-128行

```markdown
| 声称存在的文件 | 实际状态 | 严重性 |
|---|---|---|
| `MolangQueryBindLinkContractTest.java` | 磁盘上不存在 | HIGH |
| `MolangCallableBindLinkContractTest.java` | 磁盘上不存在 | HIGH |
| `MolangAnimationClockTransitionalParityContractTest.java` | 磁盘上不存在 | HIGH |
```

验证命令：
```bash
# 在 E:\_ideaProjects\qylEyelib 下执行
ls eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java
# → File not found
```

### 证据 E2 — 路径漂移

ROADMAP 提到以下文件在 `eyelibmolang/` 根包，实际在 `eyelibmolang/compiler/` 下：

| ROADMAP 声称路径 | 实际路径 |
|---|---|
| `eyelibmolang/MolangBytecodeEmitter.java` | `eyelibmolang/compiler/MolangBytecodeEmitter.java` |
| `eyelibmolang/MolangCompilerImpl.java` | `eyelibmolang/compiler/MolangCompilerImpl.java` |
| `eyelibmolang/MolangRuntimeSupport.java` | `eyelibmolang/compiler/MolangRuntimeSupport.java` |

### 证据 E3 — 状态矛盾

ROADMAP 第113行：`"MolangOwnerSet→HostContext migration (deferred, not yet performed)"`

但 `MolangOwnerSet.java` 已被删除。迁移已部分完成，但文档称"未开始"。

### 证据 E4 — 死文档指针

`docs/index/molang.md` 和 `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md` 引用了不存在的 `compiler/diagnostic/` 目录。

### 证据 E5 — `compiler/diagnostic/` 目录不存在

```bash
ls eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/diagnostic/
# → 目录不存在
```

## 业已验证的解决模式

### 模式 A：文档路径自动化交叉验证（最简单、最有效）

**来源**：Koru 项目的 `TEST_DOCUMENTATION_MAP.md` + 文件存在性测试

**核心思想**：从 Markdown 文档中提取所有文件路径引用（匹配反引号内的 `.java` 模式），逐个验证文件是否存在。

```java
@Test
void allRoadmapFilePathsExist() throws Exception {
    Path roadmap = Path.of("eyelib-molang/ROADMAP.md");
    String content = Files.readString(roadmap);
    
    // 提取反引号中的文件路径
    Pattern pattern = Pattern.compile("`([^`]+\\.java)`");
    Matcher matcher = pattern.matcher(content);
    
    List<String> failures = new ArrayList<>();
    while (matcher.find()) {
        String claimed = matcher.group(1);
        if (claimed.contains("(")) continue;  // 跳过方法引用
        Path resolved = Path.of(claimed);
        if (!Files.exists(resolved)) {
            failures.add("ROADMAP.md 声称存在但缺失: " + claimed);
        }
    }
    
    assertThat(failures).isEmpty();
}
```

### 模式 B：ArchUnit 包结构验证

**来源**：ArchUnit 官方示例，Société Générale 生产使用

```java
@ArchTest
static final ArchRule mapping_package_contains_contract_tests =
    classes()
        .that().haveSimpleNameEndingWith("ContractTest")
        .should().resideInAPackage("..mapping..");
```

### 模式 C：文档状态与代码一致性的 Gradle 任务

**来源**：verhas/jamal 文档断言模块的自定义构建集成

```kotlin
// build.gradle.kts — 文档验证任务
tasks.register("verifyDocs") {
    doLast {
        // 1. 提取 ROADMAP 文件路径
        // 2. 验证每个路径存在
        // 3. 验证 ROADMAP 中引用的 class 名在模块中实际存在
        // 4. 任一失败 → 构建中断
    }
}
```

## 执行计划

### Step 1：修复 ROADMAP.md 中的路径（3处）

将以下3个文件路径从根包修正为 `compiler/` 子包：
```markdown
- `MolangBytecodeEmitter.java` → `compiler/MolangBytecodeEmitter.java`
- `MolangCompilerImpl.java` → `compiler/MolangCompilerImpl.java`
- `MolangRuntimeSupport.java` → `compiler/MolangRuntimeSupport.java`
```

### Step 2：修复 ROADMAP.md 中缺失的3个文件引用

选项：
- **A**：如果这些测试已被替代方案覆盖，删除ROADMAP中的引用
- **B**：如果这些测试仍需要但尚未编写，保留引用但将状态改为 ⬜
- **C**：如果实际存在不同名称的等价测试，更新引用为新文件名

**推荐选项C**：将以下3个缺失引用替换为实际存在的测试类：
```markdown
- `MolangQueryBindLinkContractTest.java` → `MolangCallableVariantSelectionAmbiguityContractTest.java`
- `MolangCallableBindLinkContractTest.java` → `MolangCallablePublicationSignatureRoleTest.java`
- `MolangAnimationClockTransitionalParityContractTest.java` → ⬜（标记为未开始）
```

### Step 3：更新 MolangOwnerSet 迁移状态

```markdown
# 修改前：
Phase 4 MolangOwnerSet→HostContext migration (deferred, not yet performed)

# 修改后：
Phase 4 MolangOwnerSet has been removed. HostContext exists but uses raw Class<?> 
lookup — the HostRole<T> design from shared-vocabulary-and-phase-ownership-draft.md 
is not yet implemented. Partial migration: file deleted, semantic model pending.
```

### Step 4：删除死文档指针

- 从 `docs/index/molang.md` 中移除 `compiler/diagnostic/` 引用
- 从 `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md` 中移除相应行

### Step 5：创建 `RoadmapDocVerificationTest.java`

新增测试类，自动验证 ROADMAP.md 中的所有文件路径引用：

```java
// 路径：eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/doc/RoadmapDocVerificationTest.java
class RoadmapDocVerificationTest {
    @Test
    void allRoadmapFileClaimsExist() throws Exception {
        // 实现模式 A 的逻辑
    }
    
    @Test
    void roadmapDoesNotReferenceMolangOwnerSet() {
        String content = Files.readString(Path.of("eyelib-molang/ROADMAP.md"));
        assertThat(content).doesNotContain("MolangOwnerSet.java");
    }
    
    @Test
    void compilerDiagnosticDirectoryDoesNotHaveDeadRefs() {
        // 验证 docs/index/molang.md 不再引用不存在的目录
    }
}
```

### Step 6：运行验证

```bash
jetbrain_run_gradle_tasks :eyelib-molang:test
```

## 防漂移机制

完成修复后，`RoadmapDocVerificationTest` 将作为 CI 门控运行，确保：
1. ROADMAP 中引用的所有 `.java` 文件实际存在
2. 不会重新引入已删除的类引用
3. 不会重新引入已移除的死目录引用

## Check-list

- [ ] Step 1：修复 ROADMAP.md 中3处路径（根包→compiler子包）
- [ ] Step 2：修复 ROADMAP.md 中3个缺失文件引用（选项C）
- [ ] Step 3：更新 MolangOwnerSet 迁移状态描述
- [ ] Step 4：从 docs/index/molang.md 移除 compiler/diagnostic/ 引用
- [ ] Step 4：从 eyelib-molang README.md 移除 compiler/diagnostic/ 引用
- [ ] Step 5：创建 `RoadmapDocVerificationTest.java`
- [ ] Step 6：`jetbrain_run_gradle_tasks :eyelib-molang:test` 通过
- [ ] 确认 ROADMAP.md 不再引用不存在的文件
