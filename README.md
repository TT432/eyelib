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

- [Antlr-Molang](https://github.com/TT432/antlr-molang) for parse Molang.
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) for generate bytecode for Molang.
- [lombok](https://projectlombok.org/) for generate getter/setter and more.
- [Chin](https://github.com/TT432/chin) for utils.

## Repository Docs

- Repo map: [docs/index/repo-map.md](docs/index/repo-map.md)
- Refactor control spec: [docs/architecture/00-control-spec.md](docs/architecture/00-control-spec.md)
- Module boundaries: [docs/architecture/01-module-boundaries.md](docs/architecture/01-module-boundaries.md)
- Side boundaries: [docs/architecture/02-side-boundaries.md](docs/architecture/02-side-boundaries.md)
- Generated code policy: [docs/architecture/03-generated-code-policy.md](docs/architecture/03-generated-code-policy.md)
- Refactor plan: [docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md](docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md)
