# Phase 25 Context

## Goal

在不允许 `:eyelib-attachment` 反向依赖 root runtime 的前提下，提取 attachment-side payload/data/codec 类型；runtime owner 类继续保留在 root。

## Inputs

- `.planning/ROADMAP.md` — Phase 25 目标与成功标准
- `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/README.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md`

## Findings

- `ExtraEntityData`、`ExtraEntityUpdateData`、`EntityStatistics` 是独立 attachment payload/data records，可直接迁入 `:eyelib-attachment`。
- `AnimationComponent.SerializableInfo` 与 `ModelComponent.SerializableInfo` 是纯 sync/persistence payload，适合提取为 attachment-side payload 类型。
- `RenderData`、`AnimationComponent`、`ModelComponent`、`RenderControllerComponent`、`ClientEntityComponent`、`ItemInHandRenderData`、`EntityBehaviorData` 仍然承担 runtime owner 职责，直接整体迁出会导致 attachment 反向依赖 root。
- `ModelBakeInvalidationHooks` 之外，本阶段未新增新的 root <- attachment 反向依赖。

## Extracted Types

- `io.github.tt432.eyelibattachment.capability.ExtraEntityData`
- `io.github.tt432.eyelibattachment.capability.ExtraEntityUpdateData`
- `io.github.tt432.eyelibattachment.capability.EntityStatistics`
- `io.github.tt432.eyelibattachment.capability.AnimationComponentInfo`
- `io.github.tt432.eyelibattachment.capability.ModelComponentInfo`

## Root Owners Retained

- `io.github.tt432.eyelib.capability.RenderData`
- `io.github.tt432.eyelib.capability.EyelibAttachableData`
- `io.github.tt432.eyelib.capability.EntityBehaviorData`
- `io.github.tt432.eyelib.capability.ItemInHandRenderData`
- `io.github.tt432.eyelib.capability.component.*`
