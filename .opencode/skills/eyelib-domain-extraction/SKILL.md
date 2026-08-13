---
name: eyelib-domain-extraction
description: Extract a Port from a domain module in the hexagonal architecture refactoring — locate MC contact points, create Port interfaces, migrate glue code to bridge, and verify with ArchUnit.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, hexagonal, domain, port, extraction, refactoring
  related-skills: eyelib-hexagonal-gates, eyelib-build
---

# eyelib-domain-extraction

六边形架构重构中从 domain 模块提取一个 Port：定位 MC 接触点 → 创建 Port 接口 → 修改 domain 代码 → 迁移 glue 到 bridge → 更新调用方 → 编译与 ArchUnit 验证。

## When to use
- Extract a Port from a domain module in the hexagonal architecture refactoring — locate MC contact points, create Port interfaces, migrate glue code to bridge, and verify with ArchUnit.

## Rules
- 子代理每次只提取一个模块的一个 Port；同一 Port 涉及多个模块时按模块拆分多次执行（如 PortStringRepresentable 涉及 3 模块 → 分 3 次）。
- 编译统一用 mcmcp_build 或 bash 跑 gradlew compileJava（ADR-0014 后单 project，无 :eyelib-material: 子项目前缀）。
- material 模块 gl/ 与 shared/ 两处同名枚举（BlendFactor、GLStates、DepthFunc）都要改 implements：shared/ 是抽象版（CODEC 用），gl/ 映射 GL 常量，不能漏改。
- NEVER Port 接口不得 import 任何 net.minecraft.*；用 grep "import net.minecraft" 验证。
- When 前置条件:
  - 开始前已读完 ADR-0010（六边形架构，为什么拆）和 docs/architecture/domain-module-map.md（当前模块需要哪些 Port）。
  - bridge 包已存在（ADR-0014 后为包非子项目）。
- When Common Pitfalls:
  - 枚举改了 implements 必须同步改 codec：StringRepresentable.fromEnum() → PortStringRepresentable.fromEnum()。
  - NEVER bridge 中不得重新发明 Port：bridge 只能实现 domain 已有 Port，不能定义新的。
- When Step 2：定位所有 MC 接触点:
  - 用 grep -rn "import net.minecraft" 定位全部 MC 接触点并分类映射处理策略：枚举序列化（implements StringRepresentable）→ 改实现 PortStringRepresentable；资源路径（new ResourceLocation(ns, path)）→ 改用 PortResourceLocation；渲染类型（RenderType, RenderStateShard）→ 改用 Port 接口；其他 → 判断新建 Port 或直接迁移 bridge。
- When Step 3：创建 Port 接口:
  - 在目标模块 src/main/java/io/github/tt432/<模块包>/port/ 下创建 Port 接口。
  - NEVER Port 接口不加 public 修饰符。
  - NEVER Port 接口不加类级 @NullMarked（依赖包的 package-info.java）。
- When Step 4：修改 domain 代码:
  - 枚举类 implements StringRepresentable 改为 implements PortStringRepresentable。
  - new ResourceLocation(...) 改为 PortResourceLocation.of(ns, path)。
  - RenderType → 语义 Port：domain 输出 Port 语义（如 PortRenderPass），不输出 MC 类型。
- When Step 5：编译验证:
  - 编译零错误，警告也要修。
- When Step 6：ArchUnit（如已配置）:
  - [when ADR-0015 ArchUnit freeze 模式骨架落地后才执行本步骤；骨架未恢复则跳过。] ArchUnit 已配置时通过 mcmcp_test 或 bash 跑 gradlew test 验证隔离，可加 --tests "*ArchUnit*" 指定测试类。
- When Step 7：迁移胶水代码到 bridge:
  - 纯 Port 替换（如 PortStringRepresentable 只改 implements）不需要迁移 glue；仅当 domain 调用了 MC 类具体方法（如 RenderType.m_...()）或创建了 MC 类型实例（如 new BufferBuilder(...)）时才迁移到 bridge。
  - bridge 中实现类实现 domain 的 Port 接口，内部可自由 import MC 类型。
- When Step 8：更新调用方:
  - 更新调用方 import：Port 接口引用保持不变（在 domain 模块中）；MC 实现引用改为 bridge 中 Adapter 类。
- When 返回值类型:
  - Port 方法返回值只能用 Java 标准库、util 包类型或同 domain 模块类型；禁止返回 Object，用 sealed 类型或泛型替代。
- When 禁止项:
  - NEVER Port 不得放在 bridge 模块；Port 由 domain 定义。
  - NEVER 一个 Port 不得超过 10 个方法；超出拆成多个窄接口。
- When 粒度假说:
  - Port 只在被至少两个 MC 代码路径调用时才创建；单一 use case 直接暴露具体类型给 bridge。
- When 命名规范:
  - 命名约定：包路径 <模块>/port/；接口名 Port<语义>（如 PortRenderPass, PortEntity）；实现类 <语义>Adapter（如 RenderPassAdapter，在 bridge）。
- When 语义驱动:
  - Port 表达 domain 的需求语义，不是 MC API 镜像；按语义分组，不是每个 MC 类建一个 Port。
- When fromEnum() 辅助方法:
  - Port 替代 StringRepresentable 时必须提供 fromEnum() 辅助方法。

## Workflow
1. 从 domain-module-map.md 取出待处理 Port，确认目标任务（例：为 material 模块创建 PortStringRepresentable，替换所有 net.minecraft.util.StringRepresentable）。
2. grep 定位所有 MC 接触点，按枚举序列化/资源路径/渲染类型/其他分类映射处理策略，并做重复枚举检查。
3. 在目标模块 <模块包>/port/ 下创建 Port 接口（不加 public、不加类级 @NullMarked、不 import net.minecraft.*）。
4. 修改 domain 代码：枚举改 implements PortStringRepresentable；ResourceLocation 改 PortResourceLocation.of；RenderType 改输出语义 Port。
5. 编译验证：零错误，警告也要修。
6. ArchUnit 骨架已配置（ADR-0015 freeze 骨架落地）则跑隔离测试；未落地则跳过本步。 [decision]
7. 判断是否迁移 glue：纯 Port 替换不迁移；domain 调 MC 具体方法或创建 MC 实例时在 bridge 写实现 domain Port 接口的 Adapter（内部可自由 import MC 类型）。 [decision]
8. 更新调用方 import：Port 接口引用不变，MC 实现引用改为 bridge 中 Adapter 类。
9. 全局编译通过即完成本次提取。 [stop]

## Output
- compile_clean (required) — 零错误，警告也要修
- archunit_passed — ADR-0015 骨架落地后隔离测试通过
- Stop when: Step 9 全局编译通过后结束本次提取。

<!-- locked residual (verbatim, do not edit) -->
```java
// 改后：domain 输出 Port 语义，不输出 MC 类型
public PortRenderPass getRenderPass() {
    return new PortRenderPass() {
        public boolean requiresAlphaTest() { return hasDefine("ALPHA_TEST"); }
        public boolean requiresBlending() { return hasBlending(); }
        public boolean requiresCulling() { return !hasDefine("DISABLE_CULLING"); }
    };
}
```
```java
/** @author TT432 */
interface PortXxx {
    ReturnType someProperty();
    @Nullable ReturnType optionalProperty();
    void doSomething(InputType input);
    static PortXxx of(Args args) { ... }
}
```
```java
public interface PortStringRepresentable {
    String getSerializedName();

    static <T extends Enum<T> & PortStringRepresentable> Codec<T> fromEnum(Supplier<T[]> values) {
        return Codec.STRING.xmap(
                name -> Arrays.stream(values.get())
                        .filter(e -> e.getSerializedName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown: " + name)),
                PortStringRepresentable::getSerializedName
        );
    }
}
```
~~**bridge 的 build.gradle 缺 domain 依赖** — `implementation project(':eyelib-material')`~~（ADR-0014 前子项目时代踩坑，当前单 project 无此问题）
分类：
Step 6：ArchUnit（如已配置）
零错误，警告也要修。
> 注: ADR-0015 ArchUnit freeze 模式骨架尚未落地,本步骤在骨架恢复后才执行。
