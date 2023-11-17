# Eyelib

the renderer lib for _Minecraft_.

[English](README.md) | [中文](README.cn.md)

## Setup

If you want to test the mod in the development environment, you need to commit `sourceSets.main.resources { exclude 'assets' }` in the `build.gradle` file, because the mod needs to be built without test resources.

## Features

The mod uses a `Capability`-based structure.

`Capability` is attached to a most game elements in the mod, allowing you to easily modify the Capability content in your mod to alter the rendering.

You can find the `Capability` in the `io.github.tt432.eyelib.capability` package.