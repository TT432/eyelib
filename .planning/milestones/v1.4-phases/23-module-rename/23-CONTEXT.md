# Phase 23 Context

## Goal

将 `:eyelib-processor` 原子重命名为 `:eyelib-preprocessing`，同步完成 namespace、IDEA 元数据和 Forge 模块骨架调整，为后续 bake 代码迁移解除阻塞。

## Inputs

- `.planning/ROADMAP.md` — Phase 23 目标与成功标准
- `.planning/STATE.md`
- `MODULES.md`
- `docs/index/repo-map.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md`
- `eyelib-preprocessing/src/main/java/io/github/tt432/eyelibpreprocessing/README.md`

## Findings

- 旧模块源码规模较小，适合一次性原子重命名。
- 包名 `io.github.tt432.eyelibprocessor` 已通过 IDE 语义重命名切换为 `io.github.tt432.eyelibpreprocessing`。
- 为了承接 Phase 24 的 bake 迁移，模块必须从 plain-JVM 骨架升级为 Forge 模块骨架，并补齐 `mods.toml`。

## Affected Modules

- `client.processing` / `:eyelib-preprocessing`
- root runtime (`build.gradle`, source imports)
- IntelliJ project metadata (`.idea/`)

## Non-Goals

- 本阶段不迁移 bake 源码
- 本阶段不迁移 controller/capability 数据类
