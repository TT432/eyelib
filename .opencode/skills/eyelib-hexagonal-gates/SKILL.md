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

# eyelib-hexagonal-gates

验证六边形架构重构批次的完成标准：按 G1→G2→G3 门禁流水线运行 ArchUnit 隔离、spec-based 测试与 RenderDoc 集成检查。子代理和架构师共用。

## When to use
- 验证六边形架构重构批次 / 验收批次完成标准 / 运行 G1→G2→G3 门禁
- Do NOT use when: 非六边形架构重构场景；G3 不适合日常批次（仅最终集成）

## Rules
- 每一批次必须按 G1（编译隔离，ArchUnit+gradle compile）→ G2（行为正确，JUnit spec-based）→ G3（集成不退化，RenderDoc 截帧对比）顺序通过全部闸门；G1/G2 不需要 MC，G3 需要 MC
- When G1：编译隔离:
  - domain 模块必须零 MC import：在 domain 模块 test 中添加 ArchUnit 规则验证其不依赖 net.minecraft.. 包
  - ArchUnit 规则必须用 DescribedPredicate 排除 @Mod bootstrap 类和已知 MC 依赖，避免误报
  - 先在 build.gradle 确认 testImplementation 'com.tngtech.archunit:archunit-junit5' 依赖，否则 ArchUnit 规则无法运行
- When G2：行为正确（Spec-Based 测试）:
  - spec-based 测试 oracle 必须来自外部规范（Bedrock 文档、.mcpack 数据）
  - NEVER oracle 不得来自当前代码输出——那不是测试，是 pin 了 bug
  - 必须覆盖测试类别：CODEC 往返（JSON→parse→encode→JSON 等值）、继承链（3 层 base→add→sub 最终状态）、状态机（RC transition→new_state）、Molang 求值（对照 .mcpack 真实表达式）、有效边界（空 map、单元素、无 base、无 add）、异常/边界（循环继承、缺失 base、未知状态）
- When G3：集成不退化:
  - [when 仅最终集成/大接线后] G3 只在所有 domain 提取完成、最终集成（大接线后）时跑一次，日常批次不跑
- When 编译验证:
  - 编译验证判定：exit code 0 且无 import net.minecraft 错误

## Workflow
1. G1 编译验证：通过 mcmcp_build 或 bash 跑 gradlew compileJava（ADR-0014 后单 project，无 :eyelib-material: 子项目前缀）
2. 判定 exit code 0 且无 import net.minecraft 错误 → 进入 G2；否则修复后重跑 [decision]
3. G2 测试验证：通过 mcmcp_test 或 bash 跑 gradlew test；指定测试类可加 --tests "*BrMaterialResolverTest" 脚本参数
4. 判定全部 GREEN；UP-TO-DATE 可接受（源码未变时 Gradle 跳过）→ 所有 domain 提取完成后进入 G3 [decision]
5. 重构前：启动 MC → 进世界 → 召唤测试实体 → 截帧 → before.rdc
6. 重构后：同样操作 → after.rdc
7. 对比：Draw call 数 ±2，最终 RT 逐像素对比 [decision]
8. 不一致 → diff_draw_calls() 定位 [fallback]

## Output
- Type: gate-verdict
- Stop when: G1→G2→G3 全部通过（G3 仅最终集成时）即批次验收完成

<!-- locked residual (verbatim, do not edit) -->
```java
class ArchitectureRules {
    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelib.material");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibMaterialMod")
                                || c.getSimpleName().equals("BrShaderMapping"))))
                .and().resideInAPackage("io.github.tt432.eyelib.material..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
```
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
```java
var target = mc.level.getEntity(250);
Object cap = RenderData.getComponent(target);
List comps = (List) cap.getClass().getMethod("getModelComponents").invoke(cap);
// 验证 comps.size()、RenderType 等
```
通过 `mcmcp_test` 或 bash 跑 `gradlew test`,若要指定测试类可加 `--tests "*BrMaterialResolverTest"` 脚本参数。
