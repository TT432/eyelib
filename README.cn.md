# Eyelib

基于《Minecraft》的渲染 mod。

[English](README.md) | [中文](README.cn.md)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.tt432/eyelib)

## 快速上手

该库已发布至 Maven Central。

```groovy
dependencies {
    implementation "io.github.tt432:eyelib:${eyelib_version}"
}
```

## 依赖

- [Antlr-Molang](https://github.com/TT432/antlr-molang) 用于解析 Molang。
- [janino](https://janino-compiler.github.io/janino/) 用于生成 Molang 的字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。
- [EffekseerForMultiLanguages](https://github.com/effekseer/EffekseerForMultiLanguages) 用于 effekseer 支持。
- [Chin](https://github.com/TT432/chin) 用于提供工具类。