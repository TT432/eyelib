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

- Handwritten recursive-descent parser for Molang (ANTLR removed).
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) 用于生成 molang 字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。
- [Chin](https://github.com/TT432/chin) 用于提供工具类。

## 仓库文档

|- 架构决策： `docs/decisions/`
|- 模块边界： [docs/decisions/0002-module-boundaries.md](docs/decisions/0002-module-boundaries.md)
|- Side 边界： [docs/decisions/0003-side-boundaries.md](docs/decisions/0003-side-boundaries.md)
|- 生成代码策略： [docs/decisions/0004-generated-code-policy.md](docs/decisions/0004-generated-code-policy.md)
|- 功能模块债务清单： [docs/decisions/0005-mc-debt-ledger.md](docs/decisions/0005-mc-debt-ledger.md)
