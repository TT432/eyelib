# 技术栈研究 — `:eyelib-util` Forge 共享工具模块

**项目:** Eyelib v1.3 eyelib-util 模块分离
**研究时间:** 2026-05-10
**总体置信度:** HIGH — 基于全部现有子模块构建文件、mods.toml 模板、根构建脚本以及 PROJECT.md 约束的实际阅读

## 执行摘要

`:eyelib-util` 应当作为**Forge 感知**的 Gradle 子项目添加进来。之所以做出这个决定，是因为 `PROJECT.md` 中明确写道："eyelib-util scope: May depend on MC/Forge; not artificially constrained to be pure Java"。如果将其搞成一个纯 JVM 库，那它在 1.3 里程碑中的目标就无法实现——root/util/* 中的许多工具类都直接使用了 Minecraft 和 Forge 的类型。

构建模式应当直接沿用 `eyelib-attachment`、`eyelib-material`、`eyelib-particle` 和 `eyelib-importer` 中既有的、久经考验的 Forge 感知子项目模板。不要重新发明轮子，也不要引入新的 Gradle 约定插件；这个里程碑关心的是一次干净的迁移，而不是构建基础设施的改造。

## 已验证的现有构建事实

| 事实 | 证据 | 置信度 |
|------|------|--------|
| 4 个现有的 Forge 感知子模块（attachment、importer、material、particle）在插件集、legacyForge 配置、processResources 扩展以及 `mods.toml` 结构上使用了**完全相同的模式**。 | 已读取全部 4 个 `build.gradle` 文件 + `mods.toml` 文件 | HIGH |
| `eyelib-molang` 是一个纯 JVM 模块：它**没有** `legacyforge` 插件，也**没有** `mods.toml`。它使用 `java-library` + `lombok`，不带 `maven-publish`。 | `eyelib-molang/build.gradle` | HIGH |
| `eyelib-processor` 也是纯 JVM，但保留了 `maven-publish`。 | `eyelib-processor/build.gradle` | HIGH |
| 根构建脚本对于 Forge 模块使用 3 条特定的依赖声明，而对于纯 JVM 模块则不同：**Forge 模块** → `api` + `modImplementation` + `jarJar`；**纯 JVM** → `api` + `additionalRuntimeClasspath` + `jarJar`（例如 `eyelib-molang`）。 | 根 `build.gradle` 第 148-166 行 | HIGH |
| 子模块对子模块的依赖全部使用 `implementation`，而**没有**使用 `api`（例如 `eyelib-particle` 依赖 `eyelib-material`）。 | `eyelib-particle/build.gradle` 第 45-47 行 | HIGH |
| 所有子模块的 `mods.toml` 文件的依赖关系都严格限定为 `forge` 和 `minecraft`——**没有声明跨模块的 Forge 依赖**。只有根模块的 `mods.toml` 声明了 `eyelibimporter` 作为一个依赖。 | 全部 4 个 `mods.toml` 文件 + 根 `mods.toml` 模板 | HIGH |
| 属性（`mod_version`、`minecraft_version`、`forge_version` 等）在 `gradle.properties` 中进行定义，并可通过 `rootProject` 解析，使得所有子模块都可以使用变量替换机制。 | `gradle.properties`；`version = rootProject.version` | HIGH |
| 子模块 `processResources` 通过它自己的 `replaceProperties` 映射来扩展 `META-INF/mods.toml`——它们**不会**重用根模块的 `generateModMetadata` 任务。 | 子模块 `build.gradle` 文件中的 `processResources` 块 | HIGH |

## 推荐的 `eyelib-util` 堆栈

### 设置与骨架

```groovy
// settings.gradle — 在现有的 include 语句之后添加这一行
include("eyelib-util")
```

推荐的目录结构：

```
eyelib-util/
  build.gradle                        # Forge 感知的子项目构建文件
  src/main/java/io/github/tt432/eyelibutil/
    package-info.java                 # 模块命名空间声明
    README.md                         # 模块责任 + 依赖规则
  src/main/resources/META-INF/
    mods.toml                         # 遵循既有子模块模式的 Forge 元数据
  src/test/java/io/github/tt432/eyelibutil/
    # 在代码迁移时，相关测试也一并移入
```

**包根路径决定：** 使用 `io.github.tt432.eyelibutil`（和 `eyelibparticle` 与 `eyelibmaterial` 保持一致），**不**使用 `io.github.tt432.eyelib.util`。这可以防止与根模块的 `io.github.tt432.eyelib.*` 包空间产生分裂包问题，并遵循 v1.2 中为 `eyelib-particle` 确立的先例。

### 子模块 build.gradle（完整模板）

```groovy
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base {
    archivesName = 'eyelib-util'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

sourceSets {
    test {
        compileClasspath += sourceSets.main.output + sourceSets.main.compileClasspath
        runtimeClasspath += sourceSets.main.output + sourceSets.main.runtimeClasspath
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

legacyForge {
    // 和根模块以及其他 Forge 感知的子模块使用相同的 Forge 版本
    version = project.minecraft_version + '-' + project.forge_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    mods {
        // mod id 必须匹配 mods.toml 中的 modId
        eyelibutil {
            sourceSet(sourceSets.main)
        }
    }
}

dependencies {
    // ============================================================
    // 外部依赖 — 仅添加从 root/util/* 迁移过来的工具代码
    // 实际需要的东西。从根构建中的既有声明开始。
    // ============================================================

    compileOnly 'org.jspecify:jspecify:1.0.0'

    // 仅当迁移过来的工具代码引用了以下内容时才取消注释：
    // implementation 'com.mojang:datafixerupper:6.0.8'      // 用于流式编解码器
    // implementation 'org.joml:joml:1.10.5'                 // 用于 Vector3f / Matrix4f
    // implementation 'org.slf4j:slf4j-api:2.0.7'            // 用于日志

    // ============================================================
    // 内部项目依赖 — 无！
    // eyelib-util 是一个**叶子**模块。它不能依赖于
    // 根模块或其他任何子模块。如果迁移的代码引用了
    // 其他模块中的类型，那就说明这些类型需要被移入
    // eyelib-util 本身，或者这段代码根本就不属于这里。
    // ============================================================

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test').configure {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

// 遵循和 eyelib-attachment / eyelib-particle 一样的 processResources 模式
tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
            minecraft_version_range: minecraft_version_range,
            forge_version_range    : forge_version_range,
            loader_version_range   : loader_version_range,
            mod_version            : mod_version,
            mod_license            : mod_license,
            mod_authors            : mod_authors,
            mod_description        : 'Shared utility library for Eyelib — codecs, math, collection helpers, and Minecraft-facing adapters.'
    ]
    inputs.properties(replaceProperties)
    filesMatching('META-INF/mods.toml') {
        expand(replaceProperties)
    }
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }
    repositories {
        mavenLocal()
    }
}
```

### mods.toml 模板

`eyelib-util/src/main/resources/META-INF/mods.toml`：

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="eyelibutil"
version="${mod_version}"
displayName="Eyelib Util"
authors="${mod_authors}"
description='''Shared utility library for Eyelib — codecs, math, collection helpers, and Minecraft-facing adapters.'''

[[dependencies.eyelibutil]]
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.eyelibutil]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

`modId` 值 `"eyelibutil"` 遵循了该仓库中对所有子模块 id 使用**不带连字符的驼峰命名**这一既有惯例：`eyelibattachment`、`eyelibimporter`、`eyelibmaterial`、`eyelibparticle`。

**在 mods.toml 中，不要声明对 `eyelib`（根模块）的依赖**——eyelib-util 是一个独立的库，根模块消费它，而不是反过来。子模块的 `mods.toml` 文件中只应该保留 Forge + Minecraft 的依赖。

### 依赖配置决策

| 配置 | 何时在 eyelib-util 中使用 | 理由 |
|------|---------------------------|------|
| `api`（内部依赖） | **绝不使用。** eyelib-util 没有内部的项目依赖。 | 使用 `api` 会把一个库的符号泄漏给所有传递消费者。对于一个叶子工具模块，它唯一可能使用的 `api` 依赖应该是第三方库，并且只有在 eyelib-util 的公共类型中暴露了这些库的类型时才可以。 |
| `implementation`（内部依赖） | **绝不使用**（没有需要依赖的项目）。 | 即使将来 eyelib-util 需要某个第三方库，也应优先使用 `implementation`，除非其公共 API 会暴露该库的类型。 |
| `compileOnly`（jspecify） | **是** — 对 `org.jspecify:jspecify:1.0.0` 使用 `compileOnly`。 | 每个现有的子模块都使用了这种依赖方式来提供 `@NullMarked` / `@Nullable`，而且都是在仅编译时使用——因为 JSpecify 注解不是运行时依赖。 |
| `compileOnly`（其他模块） | **不适用。** eyelib-util 不依赖于其他子模块。 | 如果迁移的代码引用了 `eyelib-importer` 或 `eyelib-molang` 中的类型，那说明这些类型要么需要被提升到 `eyelib-util`，要么这段代码根本就不属于 `eyelib-util`。 |
| `testImplementation` | 标准 JUnit 5 配置。 | 与所有子模块的模式保持一致。 |

### 消费者应该如何依赖 eyelib-util

#### 根模块（`:eyelib` — 根构建）

```groovy
dependencies {
    // eyelib-util 是一个 Forge 感知的模块 => 使用和 attachment/importer/material/particle
    // 一模一样的三种依赖声明
    api project(':eyelib-util')
    modImplementation project(':eyelib-util')
    jarJar project(':eyelib-util')
}
```

**为什么是 `api`？** 根模块作为最终的集成点向外暴露 eyelib 库。如果在根模块的公共 API 中，任何类型引用了 eyelib-util 中的工具类型，那么消费者就需要在编译期能够访问到这些类型。如果真的想缩小范围，可以在此里程碑过后重新评估，但从 `api` 开始和既有模块的使用惯例保持一致。

**为什么是 `modImplementation`？** 这使得该模块在 `legacyForge` 管理的运行时类路径上可用——这对于任何提供 Forge 侧可见代码的 Forge 感知模块来说都是必需的。

**为什么是 `jarJar`？** 将 eyelib-util 打包进最终的 JAR 产物中。这和每一个 Forge 感知的子模块的处理方式都是一致的。

#### 其他子模块（`:eyelib-particle`、`:eyelib-importer` 等）

```groovy
dependencies {
    // eyelib-util 提供了内部使用的工具代码 —— 不要重新暴露
    implementation project(':eyelib-util')
}
```

**为什么是 `implementation`？** 对于子模块来说，eyelib-util 的符号不太可能出现在其公共 API 中（比如 `ParticleDefinition` 并不太会去暴露 `StreamCodecHelper`）。使用 `implementation` 可以将编译类路径隔离开来，从而加快构建速度并保持 API 边界清晰。如果某个特定子模块确实需要在公共 API 中暴露 eyelib-util 的类型，那么它可以将配置升级为 `api`，并应为这一升级给出充分的理由。

### 反模式：需要避免的模式

| 反模式 | 为什么它很危险 | 替代方案 |
|---------|----------------|----------|
| `eyelib-util` 依赖 `project(':')` 或任意子模块 | 形成循环依赖或产生反向依赖，违背模块分离的目的。 | eyeelib-util 必须保持叶子地位。如果代码需要来自其他模块的类型，那么要么就将这段代码留在原地，要么就将共享类型提升到 eyelib-util 中。 |
| 在 eyelib-util 中使用 `api` 来声明对另一个子模块的依赖 | 使得所有 eyelib-util 的消费者都被强制传递依赖了它本不需要知道的东西。 | 对于内部依赖，使用 `implementation` 或者完全避免。 |
| 省略 `mods.toml` / 将 eyelib-util 做成纯 JVM | 违背 PROJECT.md 中的明确约束，即“可以与 MC/Forge 存在依赖关系”。同时也会导致无法编译 Minecraft 相关的工具代码——这些代码正构成了 root/util/* 的绝大部分内容。 | 始终使用 Forge 感知的构建模式。 |
| `eyelib-util` 声明的依赖中包含了无法从 `root/util/*` 的迁移代码中得到证实的外部库 | 造成了不必要的依赖膨胀，并为 `jarJar` 打包带来了潜在的运行时冲突。 | 仅添加那些在根模块构建脚本中已被 `root/util/*` 代码所使用并已经显式声明了的依赖。 |
| 在 `mods.toml` 中将 `clientSideOnly=true` 设置为捷径 | 工具库在逻辑上是两边通用的。只有在有证据表明 eyelib-util 的所有代码都严格仅限客户端时，才可使用此选项。 | 保持 `side="BOTH"`，这是所有子模块 mods.toml 文件中的标准做法。 |

## 整合与验证

### 根模块 settings.gradle 需要做的改动

```groovy
include("eyelib-attachment")
include("eyelib-importer")
include("eyelib-material")
include("eyelib-molang")
include("eyelib-particle")
include("eyelib-processor")
include("eyelib-util")    // <-- 在 processor 之后新增这一行
```

### 根模块 build.gradle 中需要添加的完整依赖声明（依影响范围而定）

**模式 A — 最安全（从 api 开始，之后可视情况收紧）：**
```groovy
api project(':eyelib-util')
modImplementation project(':eyelib-util')
jarJar project(':eyelib-util')
```

如果后续的 API 审计表明，根模块的公开导出符号中没有任何一个暴露了 eyelib-util 里的类型，那么后续的里程碑可以安全地将 `api` 降级为 `implementation`。但本里程碑阶段无需冒着破坏编译的风险去过早收紧；沿用既有模块的成熟模式反而是一笔更安全的赌注。

**模式 B — 仅在内部使用（仅在完全确认没有公共 API 泄漏时才使用）：**
```groovy
implementation project(':eyelib-util')
modImplementation project(':eyelib-util')
jarJar project(':eyelib-util')
```

对于 Plan-Phase 阶段，请使用**模式 A**。

### Gradle 验证任务（仅通过 JetBrains MCP 运行）

验证步骤：

1. **一次性同步：** `jetbrain_sync_gradle_projects` — 确保 IntelliJ 能识别新添加的模块
2. **编译 eyelib-util：** `jetbrain_run_gradle_tasks`，任务为 `[:eyelib-util:compileJava]`
3. **运行 eyelib-util 测试：** `jetbrain_run_gradle_tasks`，任务为 `[:eyelib-util:test]`
4. **在引入依赖后重新编译整个项目：** `jetbrain_build_project` — 验证所有消费模块能否解析 `project(':eyelib-util')`
5. **运行整个测试套件：** `jetbrain_run_gradle_tasks`，任务为 `[test]`
6. **检查 NullAway（如果触及相关代码）：** `jetbrain_run_gradle_tasks`，任务为 `[nullawayMain]`

## 需要避免的堆栈陷阱总结

1. **不要** 在 eyelib-util 内部声明对任何子模块或对根模块 `:` 的项目依赖——它是一个叶模块。
2. **不要** 创建成纯 JVM 模块（不带 `legacyforge`）——这会导致无法在 eyelib-util 中编译任何 Minecraft 或 Forge 相关的工具代码。
3. **不要** 在 `mods.toml` 中声明跨模块的依赖——让它保持子模块的标准做法，只保留 forge + minecraft。
4. **不要** 添加那些当前 `root/util/*` 代码尚未使用的第三方库依赖——仅仅声明那些迁移后的工具类实际 `import` 了的库。
5. **不要** 为了“整洁”而去引入 Gradle 约定插件或是集中式的 buildSrc 逻辑——这个里程碑交付的是一个模块，而不是一次构建系统的改造。

## 来源

- 仓库文件：`settings.gradle`、根 `build.gradle`、`eyelib-attachment/build.gradle`、`eyelib-importer/build.gradle`、`eyelib-material/build.gradle`、`eyelib-particle/build.gradle`、`eyelib-molang/build.gradle`、`eyelib-processor/build.gradle`、所有 `mods.toml` 文件、`gradle.properties`、`MODULES.md`、`.planning/PROJECT.md`。**置信度：HIGH。**
- Forge 1.20.1 mods.toml 规范：https://docs.minecraftforge.net/en/1.20.1/gettingstarted/structuring/#the-modstoml-file。**置信度：HIGH。**
- Gradle Java 库插件 API 与实现分离：https://docs.gradle.org/current/userguide/java_library_plugin.html。**置信度：HIGH。**
- MDGL LegacyForge 文档：https://github.com/neoforged/ModDevGradle。**置信度：HIGH。**
