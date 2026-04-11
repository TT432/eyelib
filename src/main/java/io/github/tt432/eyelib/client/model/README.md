# Client Model Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/model/`
- Contains root-side runtime model helpers, bake helpers, model-part-facing abstractions, and adapters that consume model definitions from `:eyelib-importer`.

## Boundary intent
- Platform-free definition/state structures may remain outside `mc/impl` only if they do not expose Minecraft model/runtime types.
- Bake/runtime/render integration, texture-backed model processing, and model-part-facing implementation code should move toward `mc/impl` during final quarantine.

## Current split
- Canonical model definitions (`Model`, locators, visible bounds, source-format importer data) now live in `:eyelib-importer`.
- Root-side runtime owners (`ModelBakeInfo`, bake helpers, render-facing adapters, `ModelRuntimeData`) remain transitional.
- This package still contains MC-bound runtime/details such as `ModelPart`, `PartPose`, `PoseStack`, and related bake/render helpers.
- Importer-side texture/image handling now uses `ImportedImageData` inside `:eyelib-importer`; `NativeImage` conversion/upload remains root-owned.

## Editing rules
- Do not treat this package as platform-free by default.
- Prefer keeping definition/state contracts in `:eyelib-importer` and runtime/render helpers in root unless a human explicitly asks to re-merge them.
- Keep `client/model/importer/` focused on adapting importer outputs into runtime objects instead of reclaiming importer normalization logic.
