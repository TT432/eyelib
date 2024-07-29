# Eyelib

基于《Minecraft》的渲染 mod。

[English](README.md) | [中文](README.cn.md)

## 快速上手

第一步是配置 GitHub packages 仓库。[Github 文档](https://docs.github.com/zh/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)。

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/TT432/eyelib")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
   }
}
```

然后你需要设置 dependencies：

```groovy
dependencies {
    // 如果你需要 jarJar，用 'implementation "io.github.tt432:eyelib:0.1.0:all"'
    implementation "io.github.tt432:eyelib:1.21-0.1.2"
}
```

## 依赖

- [Antlr-Molang](https://github.com/TT432/antlr-molang) 用于解析 Molang。
- [janino](https://janino-compiler.github.io/janino/) 用于生成 Molang 的字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。
- [EffekseerForMultiLanguages](https://github.com/effekseer/EffekseerForMultiLanguages) 用于 effekseer 支持。
- [Chin](https://github.com/TT432/chin) 用于提供工具类。