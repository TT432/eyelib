# Eyelib

the renderer lib for _Minecraft_.

[English](README.md) | [中文](README.cn.md)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.tt432/eyelib)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/TT432/eyelib)

## Getting Started

The lib is published to [Maven Central](https://repo1.maven.org/maven2/io/github/tt432/eyelib/).

### Version scheme

Eyelib supports multiple Minecraft / loader combinations. Each combination is a separate artifact with a version following the format:

```
{mod_version}+{minecraft_version}-{loader}
```

Pick the one that matches your target:

| Minecraft | Loader | Java | Artifact version |
|---|---|---|---|
| 1.20.1 | Forge 47.x | 17 | `21.1.14+1.20.1-forge` |
| 1.21.1 | NeoForge 21.1.x | 21 | `21.1.14+1.21.1-neoforge` |
| 26.1.2 | NeoForge 26.1.x | 25 | `21.1.14+26.1.2-neoforge` |

> Check [Maven Central](https://repo1.maven.org/maven2/io/github/tt432/eyelib/) for the latest available `mod_version`.

### Gradle

**Groovy DSL** (`build.gradle`):

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "io.github.tt432:eyelib:21.1.14+1.20.1-forge"
}
```

**Kotlin DSL** (`build.gradle.kts`):

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.tt432:eyelib:21.1.14+1.20.1-forge")
}
```

> If your mod will bundle eyelib inside its own jar instead of requiring users to install it separately, use `jarJar` (Forge / NeoForge) in addition to `compileOnly`.

### Maven

```xml
<dependency>
    <groupId>io.github.tt432</groupId>
    <artifactId>eyelib</artifactId>
    <version>21.1.14+1.20.1-forge</version>
</dependency>
```

### Declaring a mod dependency

Add eyelib as a dependency in your mod metadata so the loader enforces it at runtime.

**Forge** — `src/main/resources/META-INF/mods.toml`:

```toml
[[dependencies."your_mod_id"]]
    modId="eyelib"
    mandatory=true
    versionRange="[21.1.14,)"
    ordering="AFTER"
    side="CLIENT"
```

**NeoForge** — `src/main/resources/META-INF/neoforge.mods.toml`:

```toml
[[dependencies."your_mod_id"]]
    modId="eyelib"
    mandatory=true
    versionRange="[21.1.14,)"
    ordering="AFTER"
    side="CLIENT"
```

The `versionRange` uses the mod version (`21.1.14`) without the `+mc-loader` suffix, because that is the version the mod reports in-game. Set `side` to `"CLIENT"` (eyelib is a client-side rendering library) or `"BOTH"` depending on your needs.

## Dependencies

- Handwritten recursive-descent parser for Molang (ANTLR removed).
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) for generate bytecode for Molang.
- [lombok](https://projectlombok.org/) for generate getter/setter and more.
- [Chin](https://github.com/TT432/chin) for utils.

## Repository Docs

|- Architecture decisions: `docs/decisions/`
|- Module boundaries: [docs/decisions/0002-module-boundaries.md](docs/decisions/0002-module-boundaries.md)
|- Side boundaries: [docs/decisions/0003-side-boundaries.md](docs/decisions/0003-side-boundaries.md)
|- Generated code policy: [docs/decisions/0004-generated-code-policy.md](docs/decisions/0004-generated-code-policy.md)
|- Functional module debt ledger: [docs/decisions/0005-mc-debt-ledger.md](docs/decisions/0005-mc-debt-ledger.md)
