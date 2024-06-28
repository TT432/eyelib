# Eyelib

the renderer lib for _Minecraft_.

[English](README.md) | [中文](README.cn.md)

## Getting Started

the first step is set up github packages maven.[Github Docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry).

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
    implementation "io.github.tt432:eyelib:1.21-0.1.2"
}
```

## Dependencies

- [Antlr-Molang](https://github.com/TT432/antlr-molang) for parse Molang.
- [javassist](http://www.javassist.org/) for generate bytecode for Molang.
- [lombok](https://projectlombok.org/) for generate getter/setter and more.