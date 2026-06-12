# Eyelib

the renderer lib for _Minecraft_.

[English](README.md) | [中文](README.cn.md)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.tt432/eyelib)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/TT432/eyelib)

## Getting Started

The lib is published to Maven Central.

```groovy
dependencies {
    implementation "io.github.tt432:eyelib:${eyelib_version}"
}
```

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
