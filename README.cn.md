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
- [jdk-classfile-backport](https://github.com/dmlloyd/jdk-classfile-backport) 用于生成 molang 字节码。
- [TwelveMonkeys](https://github.com/haraldk/TwelveMonkeys) 用于 .tga 格式的图片支持。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。
- [Chin](https://github.com/TT432/chin) 用于提供工具类。