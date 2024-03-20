# Eyelib

基于《Minecraft》的渲染 mod。

[English](README.md) | [中文](README.cn.md)

## 功能

该 mod 的结构基于 `Capability`。

该 mod 的 `Capability` 被附加到大多数游戏对象上，所以你可以通过修改 `Capability` 的内容来修改游戏对象的渲染。

你可以在 `io.github.tt432.eyelib.capability` 包下找到该 mod 的 `Capability`。

## 依赖

- [Antlr-Molang](https://github.com/TT432/antlr-molang) 用于解析 Molang。
- [Manifold: String Templates](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-strings/README.md) 用于使用字符串模板。
- [javassist](http://www.javassist.org/) 用于生成 Molang 的字节码。
- [lombok](https://projectlombok.org/) 用于生成 getter/setter 等。