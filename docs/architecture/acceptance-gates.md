# 验收闸门

> 架构师和子代理共用的完成标准。每一批次必须通过这些闸门才能进入下一批。

## 闸门体系

```
G1: 编译隔离 ──→ G2: 行为正确 ──→ G3: 集成不退化
   (自动化)        (JUnit)          (RenderDoc)
```

| 闸门 | 验证什么 | 工具 | 是否需要 MC |
|------|----------|------|------------|
| G1 | domain 模块零 MC import | ArchUnit + gradle compile | ❌ |
| G2 | 行为对照 Bedrock 规范 | JUnit spec-based 测试 | ❌ |
| G3 | 重构后渲染输出不变 | RenderDoc 截帧对比 | ✅（仅大接线后） |

---

## G1：编译隔离

### 1a: ArchUnit 规则

**前置步骤：在 domain 模块的 `build.gradle` 中已有 ArchUnit 依赖。**

规则使用 `.that(DescribedPredicate)` 排除 @Mod bootstrap 类和已知无法避免的 MC 依赖：

```java
@NullMarked
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

### 1b: 编译

```bash
# WSL 环境 → 必须用 Windows 侧 gradlew.bat
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:compileJava --no-configuration-cache"
```

**判定**：exit code 0 且无 `import net.minecraft` 错误。

---

## G2：行为正确（Spec-Based 测试）

### 编写规范

测试的 oracle 必须来自外部规范，不能来自当前代码的输出：

**正确 —— oracle 来自 Bedrock 文档：**
```java
@Test
void inheritOrder_lastWins() {
    // Bedrock 规范：materials 数组中后面覆盖前面
    Map<String, BrMaterialEntry> chain = buildChain(
        "entity",      // base
        "entity_nocull",  // +DisableCulling
        "entity_alphatest" // +ALPHA_TEST
    );
    ResolvedBrMaterial resolved = BrMaterialResolver.resolve(chain);

    // Oracle: Bedrock 文档说后覆盖前
    assertTrue(resolved.hasDefine("ALPHA_TEST"),
        "entity_alphatest 加了 ALPHA_TEST，应保留");
    assertFalse(resolved.hasState(GLStates.DisableCulling),
        "entity_alphatest 的 -states 移除了 entity_nocull 加的 DisableCulling");
}
```

**错误 —— oracle 来自当前实现：**
```java
@Test
void getRenderType_returnsCutout() {
    // 这个测试只是 pin 了当前代码的返回值，不是验证正确性
    assertEquals("entity_cutout_no_cull", material.getRenderType(texture).toString());
}
```

### 必须覆盖的测试类别

| 类别 | 示例 | 对哪些模块适用 |
|------|------|---------------|
| CODEC 往返 | JSON → parse → encode → JSON 等值 | material, behavior, particle |
| 继承链 | 3 层 base→add→sub 的最终状态 | material |
| 状态机 | RC 的 transition → new_state 逻辑 | animation |
| Molang 求值 | 对照 .mcpack 中真实表达式验证 | molang |
| 有效边界 | 空 map、单元素、无 base、无 add | 所有 |
| 异常/边界 | 循环继承、缺失 base、未知状态 | material, animation |

### 验证命令

```bash
# 模块测试
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :eyelib-material:test --no-configuration-cache

# 指定测试类
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :eyelib-material:test --no-configuration-cache --tests "*BrMaterialResolverTest"
```

**判定**：全部 GREEN。不接受的失败：
- `UP-TO-DATE` 可以接受（源码未变时 Gradle 跳过测试）
- 但如果有意改了测试源码 → 先清 build 目录再跑

---

## G3：集成不退化（RenderDoc 截帧对比）

> 只在所有 domain 提取完成后跑一次。不是每批次的闸门。

### 流程

1. 用重构前的代码：启动 MC → 进世界 → 召唤测试实体 → 截帧 → 保存 `before.rdc`
2. 用重构后的代码：同样操作 → 截帧 → 保存 `after.rdc`
3. 用 renderdoc-mcp 对比：
   - Draw call 数量差异 ±2 以内
   - 最终 RT 输出逐像素对比（排除时间戳/粒子随机性差异）
4. 如果不一致 → 用 `diff_draw_calls()` 定位根因

### 快速验证（替代方案）

如果 RenderDoc 不可用，用 /eval 做程序化验证：

```java
// 查实体是否正确加载了模型
var target = mc.level.getEntity(250);
Object cap = RenderData.getComponent(target);
List comps = (List) cap.getClass().getMethod("getModelComponents").invoke(cap);
assert comps.size() == expectedCount : "ModelComponent 数量不对";
assert ((Boolean) comp.getClass().getMethod("isSolid").invoke(comp)) == expectedSolid : "RenderType 不对";
```

---

## 闸门总览

| 批次 | 模块 | G1 隔离 | G2 spec-test | G3 集成 |
|------|------|---------|-------------|---------|
| 1 | eyelib-material | ✅ ArchUnit（3 排除） | ✅ 28 tests（继承链 + CODEC + RenderState） | ✅ 运行时 |
| 1 | eyelib-molang | ✅ ArchUnit（4 排除） | ✅ 21 tests（编译→求值 + Port 契约） | — |
| 2 | eyelib-model | ✅ ArchUnit（2 排除） | —（数据结构无需 spec） | — |
| 3 | eyelib-animation | ✅ ArchUnit（4 排除） | ✅ 7 tests（RC 状态机） | — |
| 3 | eyelib-behavior | ✅ ArchUnit（1 排除） | ✅ 9 tests（CODEC 往返 + 实体） | — |
| 4 | eyelib-particle | ✅ ArchUnit（8 排除） | ✅ 6 tests（发射器生命周期） | — |

## 自动化 CI

```yaml
# 建议添加的 GitHub Actions / CI 步骤
- name: ArchUnit Domain Isolation
  run: ./gradlew :eyelib-material:test --tests "*ArchitectureRules"
- name: Spec-Based Tests
  run: ./gradlew :eyelib-material:test :eyelib-molang:test
```
