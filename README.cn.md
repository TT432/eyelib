# Eyelib

基于《Minecraft》的渲染 mod。

[English](README.md) | [中文](README.cn.md)

## 快速上手

该库已发布至 [Maven Central](https://repo1.maven.org/maven2/io/github/tt432/eyelib/)。

按目标版本选择：

| Minecraft | Loader | Java | 坐标版本 |
|---|---|---|---|
| 1.20.1 | Forge 47.x | 17 | ![1.20.1-forge latest](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Ftt432%2Feyelib%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bcontains%28.%2C%271.20.1-forge%27%29%5D%5Blast%28%29%5D&label=&color=blue) |
| 1.21.1 | NeoForge 21.1.x | 21 | ![1.21.1-neoforge latest](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Ftt432%2Feyelib%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bcontains%28.%2C%271.21.1-neoforge%27%29%5D%5Blast%28%29%5D&label=&color=blue) |
| 26.1.2 | NeoForge 26.1.x | 25 | ![26.1.2-neoforge latest](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Ftt432%2Feyelib%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bcontains%28.%2C%2726.1.2-neoforge%27%29%5D%5Blast%28%29%5D&label=&color=blue) |

> 把 `{mod_version}` 替换为上方表格中对应的坐标版本。各目标的已发布版本见 [Maven Central](https://repo1.maven.org/maven2/io/github/tt432/eyelib/)。

### Gradle

<details open>
<summary><b>Groovy DSL</b>（<code>build.gradle</code>）</summary>

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "io.github.tt432:eyelib:{mod_version}"
}
```

</details>

<details>
<summary><b>Kotlin DSL</b>（<code>build.gradle.kts</code>）</summary>

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.tt432:eyelib:{mod_version}")
}
```

</details>

> 如果你打算把 eyelib 打包进自己的 jar（而不是要求用户单独安装），除 `compileOnly` 外还需使用 `jarJar`（Forge / NeoForge）。

### Maven

<details>
<summary><b>Maven</b>（<code>pom.xml</code>）</summary>

```xml
<dependency>
    <groupId>io.github.tt432</groupId>
    <artifactId>eyelib</artifactId>
    <version>{mod_version}</version>
</dependency>
```

</details>

### 声明 mod 依赖

在 mod 元数据里声明对 eyelib 的依赖，让加载器在运行时强制检查。

<details>
<summary><b>Forge</b> — <code>src/main/resources/META-INF/mods.toml</code></summary>

```toml
[[dependencies."your_mod_id"]]
    modId="eyelib"
    mandatory=true
    versionRange="[{mod_version},)"
    ordering="AFTER"
    side="CLIENT"
```

</details>

<details>
<summary><b>NeoForge</b> — <code>src/main/resources/META-INF/neoforge.mods.toml</code></summary>

```toml
[[dependencies."your_mod_id"]]
    modId="eyelib"
    mandatory=true
    versionRange="[{mod_version},)"
    ordering="AFTER"
    side="CLIENT"
```

</details>

`versionRange` 用的是 mod 版本号——即坐标版本去掉 `+mc-loader` 后缀后的部分（例如 `21.1.23+1.20.1-forge` 对应 `21.1.23`），因为那才是 mod 在游戏内上报的版本。`side` 按需设为 `"CLIENT"`（eyelib 是客户端渲染库）或 `"BOTH"`。

## 依赖

- Handwritten recursive-descent parser for Molang (ANTLR removed).
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) 用于生成 molang 字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。
- [Chin](https://github.com/TT432/chin) 用于提供工具类。

## 仓库文档

- 架构决策： `docs/decisions/`
- 模块边界： [docs/decisions/0002-module-boundaries.md](docs/decisions/0002-module-boundaries.md)
- Side 边界： [docs/decisions/0003-side-boundaries.md](docs/decisions/0003-side-boundaries.md)
- 生成代码策略： [docs/decisions/0004-generated-code-policy.md](docs/decisions/0004-generated-code-policy.md)
- 功能模块债务清单： [docs/decisions/0005-mc-debt-ledger.md](docs/decisions/0005-mc-debt-ledger.md)
