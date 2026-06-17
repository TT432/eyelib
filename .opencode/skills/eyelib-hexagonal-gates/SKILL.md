---
name: eyelib-hexagonal-gates
description: Verify a hexagonal architecture refactoring batch — run ArchUnit isolation, spec-based tests, and RenderDoc integration checks per the G1→G2→G3 gate pipeline.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, hexagonal, architecture, gates, acceptance
  related-skills: eyelib-domain-extraction, eyelib-build, eyelib-debug, eyelib-renderdoc
---

# Eyelib 六边形架构验收闸门

子代理和架构师共用的完成标准。每一批次必须通过 G1→G2→G3 闸门。

## 闸门体系

```
G1: 编译隔离 ──→ G2: 行为正确 ──→ G3: 集成不退化
   (自动化)        (JUnit)          (RenderDoc)
```

| 闸门 | 验证什么 | 工具 | 需要 MC |
|------|----------|------|--------|
| G1 | domain 模块零 MC import | ArchUnit + gradle compile | ❌ |
| G2 | 行为对照 Bedrock 规范 | JUnit spec-based 测试 | ❌ |
| G3 | 重构后渲染输出不变 | RenderDoc 截帧对比 | ✅（仅大接线后） |

## G1：编译隔离

### ArchUnit 规则

在 domain 模块 test 中添加，排除 @Mod bootstrap 类和已知 MC 依赖：

```java
class ArchitectureRules {
    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelibmaterial");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibMaterialMod")
                                || c.getSimpleName().equals("BrShaderMapping"))))
                .and().resideInAPackage("io.github.tt432.eyelibmaterial..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
```

### 编译验证

```bash
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:compileJava --no-configuration-cache"
```

判定：exit code 0 且无 `import net.minecraft` 错误。

## G2：行为正确（Spec-Based 测试）

### 编写规范

测试 oracle 必须来自外部规范（Bedrock 文档、.mcpack 数据），不能来自当前代码输出：

```java
// ✅ oracle 来自 Bedrock 文档
@Test
void inheritOrder_lastWins() {
    // Bedrock 规范：materials 数组中后面覆盖前面
    Map<String, BrMaterialEntry> chain = buildChain(
        "entity",          // base
        "entity_nocull",   // +DisableCulling
        "entity_alphatest" // +ALPHA_TEST
    );
    ResolvedBrMaterial resolved = BrMaterialResolver.resolve(chain);
    assertTrue(resolved.hasDefine("ALPHA_TEST"),
        "entity_alphatest 加了 ALPHA_TEST，应保留");
}
```

### 必须覆盖的测试类别

| 类别 | 示例 | 适用模块 |
|------|------|---------|
| CODEC 往返 | JSON → parse → encode → JSON 等值 | material, behavior, particle |
| 继承链 | 3 层 base→add→sub 的最终状态 | material |
| 状态机 | RC 的 transition → new_state 逻辑 | animation |
| Molang 求值 | 对照 .mcpack 真实表达式验证 | molang |
| 有效边界 | 空 map、单元素、无 base、无 add | 所有 |
| 异常/边界 | 循环继承、缺失 base、未知状态 | material, animation |

### 验证命令

```bash
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:test --no-configuration-cache"
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:test --no-configuration-cache --tests \"*BrMaterialResolverTest\""
```

判定：全部 GREEN。`UP-TO-DATE` 可接受（源码未变时 Gradle 跳过）。

## G3：集成不退化

只在所有 domain 提取完成后跑一次。

### RenderDoc 截帧对比流程

1. 重构前：启动 MC → 进世界 → 召唤测试实体 → 截帧 → `before.rdc`
2. 重构后：同样操作 → `after.rdc`
3. 对比：Draw call 数 ±2，最终 RT 逐像素对比
4. 不一致 → `diff_draw_calls()` 定位

### 快速验证（/eval 替代）

```java
var target = mc.level.getEntity(250);
Object cap = RenderData.getComponent(target);
List comps = (List) cap.getClass().getMethod("getModelComponents").invoke(cap);
// 验证 comps.size()、RenderType 等
```

## 闸门通过表

| 批次 | 模块 | G1 隔离 | G2 spec-test | G3 集成 |
|------|------|---------|-------------|---------|
| 1 | eyelib-material | ✅ ArchUnit（3 排除） | ✅ 28 tests | ✅ |
| 1 | eyelib-molang | ✅ ArchUnit（4 排除） | ✅ 21 tests | — |
| 2 | eyelib-model | ✅ ArchUnit（2 排除） | — | — |
| 3 | eyelib-animation | ✅ ArchUnit（4 排除） | ✅ 7 tests | — |
| 3 | eyelib-behavior | ✅ ArchUnit（1 排除） | ✅ 9 tests | — |
| 4 | eyelib-particle | ✅ ArchUnit（8 排除） | ✅ 6 tests | — |

## Common Pitfalls

1. **ArchUnit 规则未在 build.gradle 加依赖** — 先确认 `testImplementation 'com.tngtech.archunit:archunit-junit5'`
2. **@Mod bootstrap 类触发误报** — 在 ArchUnit 规则中用 `DescribedPredicate` 排除
3. **WSL 下误用 `./gradlew`** — 必须用 Windows 侧 `gradlew.bat`，否则极慢
4. **Spec-based 测试 oracle 来自当前实现** — 这不是测试，是 pin 了 bug。oracle 必须来自规范
5. **G3 在每批次都跑** — G3 只在最终集成时跑一次，日常不跑
