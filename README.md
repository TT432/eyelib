# Eyelib

the renderer lib for _Minecraft_.

[English](README.md) | [中文](README.cn.md)

## Getting Started

The lib is published to [Maven Central](https://repo1.maven.org/maven2/io/github/tt432/eyelib/).

Pick the one that matches your target:

| Minecraft | Loader | Java | Artifact version |
|---|---|---|---|
| 1.20.1 | Forge 47.x | 17 | ![1.20.1-forge latest](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Ftt432%2Feyelib%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bcontains%28.%2C%271.20.1-forge%27%29%5D%5Blast%28%29%5D&label=&color=blue) |
| 1.21.1 | NeoForge 21.1.x | 21 | ![1.21.1-neoforge latest](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Ftt432%2Feyelib%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bcontains%28.%2C%271.21.1-neoforge%27%29%5D%5Blast%28%29%5D&label=&color=blue) |
| 26.1.2 | NeoForge 26.1.x | 25 | ![26.1.2-neoforge latest](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Ftt432%2Feyelib%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bcontains%28.%2C%2726.1.2-neoforge%27%29%5D%5Blast%28%29%5D&label=&color=blue) |

> Replace `{mod_version}` with the artifact version listed above. Check [Maven Central](https://repo1.maven.org/maven2/io/github/tt432/eyelib/) for the latest published version of each target.

### Gradle

<details open>
<summary><b>Groovy DSL</b> (<code>build.gradle</code>)</summary>

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
<summary><b>Kotlin DSL</b> (<code>build.gradle.kts</code>)</summary>

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.tt432:eyelib:{mod_version}")
}
```

</details>

> If your mod will bundle eyelib inside its own jar instead of requiring users to install it separately, use `jarJar` (Forge / NeoForge) in addition to `compileOnly`.

### Maven

<details>
<summary><b>Maven</b> (<code>pom.xml</code>)</summary>

```xml
<dependency>
    <groupId>io.github.tt432</groupId>
    <artifactId>eyelib</artifactId>
    <version>{mod_version}</version>
</dependency>
```

</details>

### Declaring a mod dependency

Add eyelib as a dependency in your mod metadata so the loader enforces it at runtime.

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

The `versionRange` uses the mod version — the artifact version with its `+mc-loader` suffix stripped (e.g. `21.1.23` for `21.1.23+1.20.1-forge`) — because that is the version the mod reports in-game. Set `side` to `"CLIENT"` (eyelib is a client-side rendering library) or `"BOTH"` depending on your needs.

## Dependencies

- Handwritten recursive-descent parser for Molang (ANTLR removed).
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) for generate bytecode for Molang.
- [lombok](https://projectlombok.org/) for generate getter/setter and more.
- [Chin](https://github.com/TT432/chin) for utils.

## Repository Docs

- Architecture decisions: `docs/decisions/`
- Module boundaries: [docs/decisions/0002-module-boundaries.md](docs/decisions/0002-module-boundaries.md)
- Side boundaries: [docs/decisions/0003-side-boundaries.md](docs/decisions/0003-side-boundaries.md)
- Generated code policy: [docs/decisions/0004-generated-code-policy.md](docs/decisions/0004-generated-code-policy.md)
- Functional module debt ledger: [docs/decisions/0005-mc-debt-ledger.md](docs/decisions/0005-mc-debt-ledger.md)
