# Eyelib

the renderer lib for _Minecraft_.

[English](README.md) | [中文](README.cn.md)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.tt432/eyelib)

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
- [TwelveMonkeys](https://github.com/haraldk/TwelveMonkeys) for .tga image support.
- [lombok](https://projectlombok.org/) for generate getter/setter and more.
- [Chin](https://github.com/TT432/chin) for utils.