# Phase 24 Summary 01

## Result

Phase 24 已完成。

## Changes

- `client/model/bake` 的 5 个 Java 文件已迁入 `:eyelib-preprocessing` 的 `model/bake`。
- root 使用方已改为依赖 `io.github.tt432.eyelibpreprocessing.model.bake.*`。
- controller 纯定义记录 `BrAc*Definition`、`BrAnimationControllerDefinition` 已迁入 `:eyelib-importer`。
- 纯定义接口 `NamedTrackDefinition`、`NamedTrackContainerDefinition`、`StateMachineAnimationDefinition` 已迁入 `:eyelib-importer`。
- root 新增 `ModelBakeInvalidationHooks`，保留 `ModelManager` 事件驱动缓存失效，不让 preprocessing 反向依赖 root。

## Notes

- `BrAnimationController`、`BrControllerExecutor`、`BrControllerStateOwner` 仍留在 root，继续承担 runtime 执行职责。
