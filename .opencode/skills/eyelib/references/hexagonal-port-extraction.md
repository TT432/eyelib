# Hexagonal Architecture — Port 创建与提取经过验证的工作流

> 来源：eyelib 项目 2026-06-08 session 的全链路验证。
>
> ⚠️ **本文档反映 ADR-0010 时代(multi-project + 已落地 ArchUnit)的工作流**。ADR-0014 已将所有子项目合并为单 Gradle project,"Bridge 子项目创建清单"等段已过期(bridge 现在是 `io.github.tt432.eyelib.bridge` 包,不是独立子项目)。ADR-0015 计划以 freeze 模式恢复 ArchUnit 但骨架尚未落地。阅读时请结合 ADR-0014 / ADR-0015 判断哪些步骤仍适用。

## 核心教训

**测试先行，Port 在后。** 对照 Bedrock 规范写测试 → 如果测试因 MC 依赖跑不了 → 才提取 Port。不要先提取 Port 再考虑测试。

## Bridge 创建清单(ADR-0014 前;现在 bridge 是包,以下仅作历史参考)

> ADR-0014 后: bridge 是 `src/main/java/io/github/tt432/eyelib/bridge/` 包,不是独立 Gradle subproject。新建 Port 时直接在此包下加 Adapter 类即可,不需要 `mods.toml` 或 `@Mod` 注解。

1. ~~在 `settings.gradle` 加 `include("eyelib-bridge")`~~ — ADR-0014 后无子项目
2. ~~`build.gradle`：参照 eyelib-material 模板~~ — 单 project,共享 build.gradle
3. ~~`mods.toml`：必须有 `[[mods]]` 节~~ — 现共享 root mod 声明
4. ~~**必须有 `@Mod` 注解类**(如 `EyelibBridge.java`)~~ — 不需要
5. ~~Root `build.gradle`：加 `api` + `modImplementation` + `jarJar` 三条依赖~~ — 不需要
6. 编译：`jetbrain_run_gradle_tasks` 跑 `compileJava`(单 project)
7. 启动游戏验证：`eyelib_debug_launch` → `eyelib_debug_enter_world`

## 两种 Port 模式

### 纯 Port 替换（无 bridge 代码）

适用场景：MC 接口型依赖（如 `StringRepresentable` → `PortStringRepresentable`）

步骤：
1. 在端口模块创建 Port 接口（含 `fromEnum()` 静态辅助方法）
2. 所有枚举改 `implements` 声明
3. CODEC 改 `PortStringRepresentable.fromEnum(T::values)`
4. 编译验证

不需要 bridge 代码。

### Adapter 迁移

适用场景：domain 代码调用了 MC 具体方法/构造器

步骤：
1. domain 侧定义 Port 接口
2. domain 代码改为只依赖 Port
3. 原 MC 实现代码复制到 bridge，改为实现 Port
4. 删除 domain 侧原代码
5. 更新 bridge 的 build.gradle 依赖
6. 更新所有调用方 import

## Port 共享策略

被多个 domain 模块需要的 Port → 放在 `util` 包。

示例：`PortStringRepresentable` 从 `eyelib-material` 移到 `eyelib-util`，消除 `eyelib-behavior` → `eyelib-material` 的不必要依赖。

## 已验证的陷阱

- **循环依赖**：bridge → domain 是唯一合法方向。domain 不能依赖 bridge。`eyelib-material` 的 smoke 测试需要 bridge → 解决方案是移到 bridge 或删除。
- **StreamCodec 不可 Port 化**：`StreamCodec<Foo>` 接口要求精确的 `FriendlyByteBuf` 类型。model/animation 的同步包**不能**用 PortFriendlyByteBuf 替换。
- **Forge 注解扫描跨 JAR**：`@MolangMapping` 通过 Forge classpath scanner 自动发现所有 JAR 中的注解类。文件从 molang 移到 bridge 后无需任何 import 更新——scanner 自动找到。
- **`Map.of()` 10 对限制**：18 个属性 → `new HashMap<>()` + `put()`。
- **ArchUnit `@Disabled` 模式**：对于已知债务，用 `@Disabled` + Javadoc 说明阻塞条件。不阻塞 CI，但规则已就位。
