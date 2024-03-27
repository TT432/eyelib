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

then, you need to set up dependencies:

```groovy
dependencies {
    // 如果你需要 jarJar，用 'implementation "io.github.tt432:eyelib:0.1.0:all"'
    implementation "io.github.tt432:eyelib:0.1.0"
}
```

## 功能

该 mod 的结构基于 `Capability`。

该 mod 的 `Capability` 被附加到大多数游戏对象上，所以你可以通过修改 `Capability` 的内容来修改游戏对象的渲染。

你可以在 `io.github.tt432.eyelib.capability` 包下找到该 mod 的 `Capability`。

## 依赖

- [Antlr-Molang](https://github.com/TT432/antlr-molang) 用于解析 Molang。
- [javassist](http://www.javassist.org/) 用于生成 Molang 的字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。