# Eyelib

基于《Minecraft》的渲染 mod。

[English](README.md) | [中文](README.cn.md)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.tt432/eyelib)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/TT432/eyelib)

## 快速上手

该库已发布至 Maven Central。

```groovy
dependencies {
    implementation "io.github.tt432:eyelib:${eyelib_version}"
}
```

## 依赖

- [Antlr-Molang](https://github.com/TT432/antlr-molang) 用于解析 Molang。
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) 用于生成 molang 字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。
- [Chin](https://github.com/TT432/chin) 用于提供工具类。

## 仓库文档

- 仓库索引： [docs/index/repo-map.md](docs/index/repo-map.md)
- 重构控制规范： [docs/architecture/00-control-spec.md](docs/architecture/00-control-spec.md)
- 模块边界： [docs/architecture/01-module-boundaries.md](docs/architecture/01-module-boundaries.md)
- Side 边界： [docs/architecture/02-side-boundaries.md](docs/architecture/02-side-boundaries.md)
- 生成代码策略： [docs/architecture/03-generated-code-policy.md](docs/architecture/03-generated-code-policy.md)
- 重构计划： [docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md](docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md)
