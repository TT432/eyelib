# Phase 24 Context

## Goal

将 `client/model/bake` 迁入 `:eyelib-preprocessing`，并把 controller 纯数据定义连同其依赖的纯定义接口迁入 `:eyelib-importer`，消除已识别的错位纯数据类。

## Inputs

- `.planning/ROADMAP.md` — Phase 24 目标与成功标准
- `src/main/java/io/github/tt432/eyelib/client/model/README.md`
- `src/main/java/io/github/tt432/eyelib/client/animation/README.md`
- `eyelib-preprocessing/src/main/java/io/github/tt432/eyelibpreprocessing/README.md`
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/animation/README.md`

## Findings

- `client/model/bake/*` 使用 Minecraft/Forge 类型，必须依赖 Phase 23 的 Forge 化 `:eyelib-preprocessing`。
- `BrAc*Definition` 是一组互相依赖的纯 controller 定义，单独搬一个会留下边界残缺。
- 这组 controller 定义还依赖纯定义接口 `NamedTrackDefinition`、`NamedTrackContainerDefinition`、`StateMachineAnimationDefinition`，必须一并迁入 importer 才能保持边界完整。
- `ModelBakeInfo` 在迁移后暴露出 root 事件耦合；已通过 root 侧 `ModelBakeInvalidationHooks` 保留事件失效，而不让 `:eyelib-preprocessing` 反向依赖 root。

## Identified Relocations

- `client/model/bake/*` -> `eyelib-preprocessing/.../model/bake/`
- `client/animation/NamedTrackDefinition.java` -> `eyelib-importer/.../animation/`
- `client/animation/NamedTrackContainerDefinition.java` -> `eyelib-importer/.../animation/`
- `client/animation/StateMachineAnimationDefinition.java` -> `eyelib-importer/.../animation/`
- `client/animation/bedrock/controller/BrAc*Definition` + `BrAnimationControllerDefinition` -> `eyelib-importer/.../animation/bedrock/controller/`

## Affected Modules

- `:eyelib-preprocessing`
- `:eyelib-importer`
- `client.animation.runtime`
- `client.model`
