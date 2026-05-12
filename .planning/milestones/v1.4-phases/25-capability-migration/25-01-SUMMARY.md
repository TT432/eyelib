# Phase 25 Summary 01

## Result

Phase 25 已完成。

## Changes

- `ExtraEntityData`、`ExtraEntityUpdateData`、`EntityStatistics` 已迁入 `:eyelib-attachment` 的 `capability/`。
- 新增 attachment-side payload：`AnimationComponentInfo`、`ModelComponentInfo`。
- root runtime owner `AnimationComponent`、`ModelComponent`、`RenderData`、network sync packet 与 render sync apply 流程均已改为依赖新 payload 类型。
- `:eyelib-attachment` 新增对 `:eyelib-molang` 的依赖，用于承载 animation payload 中的 `MolangValue`。
- 运行时 owner 仍保留在 root，未引入 attachment -> root 的反向依赖。
