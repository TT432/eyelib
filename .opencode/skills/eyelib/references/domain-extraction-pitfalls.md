# Domain 提取实战陷阱

> 基于 2026-06-08 全批次提取的经验教训。

## Port 设计

### 1. Port 必须用 public，放在 eyelib-util

包级可见（不加 public）的 Port 接口无法跨模块使用。eyelib-behavior 依赖 eyelib-material 但看不到 material/port/ 的包私有接口 → 编译失败。**所有 Port 接口/record 必须声明为 `public`。** `port-design-template.md` 中"不加 public"的规则是错误的——已在 pitfall 中记录。

**正确做法：** 被多个模块需要的 Port 放在 `eyelib-util` 中，声明为 `public`。eyelib-util 零依赖，所有模块都可引用。

迁移到 util 的 Port：`PortStringRepresentable`、`PortResourceLocation`。

### 2. 不要为 Mojang 契约类型创建 Port

`FriendlyByteBuf` 被 Mojang 的 `StreamCodec` 接口强制为参数类型——Port 化会导致 `@Override` 签名不匹配，编译失败。

**规则：** 如果 MC 类型出现在你无法控制的接口契约中（如 Mojang 的 StreamCodec、Forge 的 IEventBus），不要 Port 化。保持 MC 引用。

### 3. 不需要的类型包装

`PortMolangValue` sealed 类型被证明是多余的。`Map<String, Object>` 已经可以表达 Molang 查询属性的值。不要为每种返回值创建包装类型——Java 标准库足够。

### 4. 不需要独立的 bridge 模块的 Port

`PortFriendlyByteBuf` 创建了但从未被使用（因为 StreamCodec 限制）。不要预创建"未来可能用到"的 Port。

## Bridge 模块

> ⚠️ 本节描述 ADR-0014 前的 multi-project 时代。ADR-0014 后 bridge 是 `io.github.tt432.eyelib.bridge` 包,共享 root 的 mod 声明,无独立 `mods.toml` 或 `@Mod` 注解类。

### Forge dev 环境要求(历史)

~~`eyelib-bridge` 是一个完整的 Forge 子项目,不是纯库 JAR。必须在 `mods.toml` 中声明 `[[mods]]` 部分,并提供一个 `@Mod("eyelibbridge")` 标注的空类。~~ ADR-0014 后此要求已不适用。

### 依赖方向(仍适用)

- bridge → domain ✅
- domain → bridge ❌ （循环依赖,包之间也禁止）
- domain 内部通过 util 包共享 Port ✅

如果 domain 或下游模块（particle）需要桥接功能，在 domain 层保留使用 Port 类型的 RenderTypeResolver，在 bridge 层保留使用 MC 类型的版本。两套并行。

## 编译

### 必须经 JetBrains MCP 跑 Gradle

历史教训: WSL 下 `./gradlew` / `java -cp ... GradleWrapperMain` 经常超时(>120s); Windows 侧 `cmd.exe /c gradlew.bat` 虽然快(5-20s)但已禁止。

现行规则(AGENTS.md Tooling Restrictions): 唯一允许的 Gradle 入口是 JetBrains MCP。

```
jetbrain_run_gradle_tasks(taskNames=["compileJava"])
```

(ADR-0014 后是单 Gradle project,不再有 `:eyelib-material:compileJava` 这种子项目 task 路径。)

## 提取顺序（按风险递增）

| 批次 | 模块 | MC import | 策略 |
|------|------|-----------|------|
| 1 | material | 33 | Port 接口替换枚举 + ResourceLocation + RenderType |
| 2 | behavior | 4 | 纯 Port 替换，简单 |
| 3 | animation | 4 | 2 个可 Port，2 个需保留 |
| 4 | model | 1 | FriendlyByteBuf 保留（Mojang 契约） |
| - | particle | 15 | 未处理，风险最高 |
| - | molang | 28 | Port 接口已创建，MolangBuiltInQuery 迁移待定 |
