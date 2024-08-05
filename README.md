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
- [janino](https://janino-compiler.github.io/janino/) for generate bytecode for Molang.
- [lombok](https://projectlombok.org/) for generate getter/setter and more.
- [EffekseerForMultiLanguages](https://github.com/effekseer/EffekseerForMultiLanguages) for effekseer support.
- [Chin](https://github.com/TT432/chin) for utils.