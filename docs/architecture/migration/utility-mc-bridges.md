# utility-mc-bridges

## Scope
- Utilities that currently mix pure helpers with Minecraft-facing helpers.
- Main paths: `util/client/`, `util/codec/`, `util/modbridge/`, selected `util/math/`, selected top-level `util/*.java`

## Why it is MC-facing
- Contains MC codecs, render helpers, resource-location helpers, and bridge events alongside pure helpers.

## Final isolation status
- First-wave seam status: complete.
- Hard-quarantine wave 2 status (timer + modbridge event): complete.
- Final `mc/api + mc/impl` isolation status: in progress.
- Expected final state for this module: only truly platform-free helpers remain in `core`; MC codecs, resource-location helpers, render helpers, bridge events, and other Minecraft/Forge-aware utility behavior must be moved or confined to allowed `mc/impl` packages.

## Target seam
- Move pure helpers toward `core`.
- Keep MC codec primitives, render helpers, and bridge/event integration in `mc/impl` unless a real seam emerges.
- Prior hard-import slice: isolate `FixedTimer` and Forge `modbridge` event ownership into `mc/impl`, while extracting fixed-step arithmetic into a plain-JVM `core` seam.
- Current hard-import slice: drain legacy `util/client` helper shims by moving owned runtime helpers to their destination owners and deleting unused bridge facades that keep platform imports in `util/client`.

## First-wave ownership design (completed)
- Added platform-free seam package: `src/main/java/io/github/tt432/eyelib/core/util/`.
- Extracted pure helpers into core owners:
  - `core/util/texture/TexturePaths` (deterministic texture path transformation)
  - `core/util/color/ColorEncodings` (ARGB↔channel packing logic)
  - `core/util/collection/ListAccessors` (first/last list access)
  - `core/util/codec/Eithers` (`Either` unwrap helper)
- Kept Minecraft/Forge-facing ownership in-place:
  - `util/client/*`, `util/modbridge/*`, and `util/codec/stream/*` remain MC-oriented.
  - Existing `util/*` entrypoints now act as compatibility adapters where extraction happened.

## Implementation changes
- Added new core package documentation: `src/main/java/io/github/tt432/eyelib/core/README.md`.
- Updated adapter-style utility classes to delegate into core seams:
  - `util/ListHelper` → `core/util/collection/ListAccessors`
  - `util/client/texture/TexturePathHelper` → `core/util/texture/TexturePaths`
  - `util/math/FastColorHelper` → `core/util/color/ColorEncodings`
  - `util/codec/EitherHelper` → `core/util/codec/Eithers`
- Migrated direct internal callers where low-risk:
  - `client/render/controller/RenderControllerEntry` now uses `TexturePaths` directly.
  - `client/render/texture/NativeImageIO` now uses `ColorEncodings` directly.

## Hard-quarantine wave 2 implementation (this change)
- Added plain-JVM fixed-step seam: `src/main/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerState.java`.
- Added Minecraft-backed runtime adapter: `src/main/java/io/github/tt432/eyelib/mc/impl/util/time/FixedTimer.java`.
- Moved Forge event ownership to quarantine zone: `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeModelUpdateEvent.java`.
- Removed legacy MC-aware utility classes from non-`mc/impl` paths:
  - deleted `src/main/java/io/github/tt432/eyelib/util/FixedTimer.java`
  - deleted `src/main/java/io/github/tt432/eyelib/util/modbridge/ModBridgeModelUpdateEvent.java`
- Updated callers:
  - `client/particle/bedrock/BrParticleEmitter` and `BrParticleParticle` now import `mc/impl/util/time/FixedTimer`.
  - `client/gui/ModelPreviewScreen` now imports `mc/impl/modbridge/ModBridgeModelUpdateEvent`.
  - `client/ClientTickHandler` now exposes explicit `getTick()` (manual method) to avoid lombok-only accessor dependence in the new adapter.

## Hard-quarantine wave 3 implementation (this change)
- Moved active pose-copy helper out of legacy util bridge surface into its render owner:
  - added `src/main/java/io/github/tt432/eyelib/client/render/PoseCopies.java`
  - updated `client/render/RenderParams.java` and `client/render/visitor/ModelVisitor.java` to import the render-owned helper
  - deleted legacy shim owner `src/main/java/io/github/tt432/eyelib/util/client/render/PoseCopies.java`
- Quarantined inventory model-resource helper ownership under `mc/impl`:
  - added `src/main/java/io/github/tt432/eyelib/mc/impl/util/model/InventoryModelResourceLocations.java`
  - deleted legacy util owner `src/main/java/io/github/tt432/eyelib/util/client/model/InventoryModelResourceLocations.java`
- Removed unused legacy util facade shims that exposed platform types in non-`mc/impl` utility surface:
  - deleted `src/main/java/io/github/tt432/eyelib/util/client/PoseHelper.java`
  - deleted `src/main/java/io/github/tt432/eyelib/util/client/ModelResourceLocationHelper.java`
- Post-change utility re-baseline (`util/**` direct MC/Forge/Blaze imports):
  - reduced from prior utility helper hotspot files (`PoseHelper`, `ModelResourceLocationHelper`, `util/client/model/InventoryModelResourceLocations`, `util/client/render/PoseCopies`) to **only** `util/codec/EyelibCodec.java` in the narrow helper subset touched by this slice
  - remaining runtime-heavy utility hotspots were reduced further in the next follow-up slice by deleting unused util/client bridge wrappers

## Hard-quarantine wave 4 implementation (this change)
- Removed unused legacy util bridge wrappers that still carried direct MC/Forge/Blaze imports inside `util/client`:
  - deleted `src/main/java/io/github/tt432/eyelib/util/client/BakedModels.java`
  - deleted `src/main/java/io/github/tt432/eyelib/util/client/BlitCall.java`
  - deleted `src/main/java/io/github/tt432/eyelib/util/client/BufferBuilders.java`
- Re-baseline result for direct non-`mc/impl` MC/Forge/Blaze imports in `util/**` now points at:
  - `src/main/java/io/github/tt432/eyelib/util/codec/*` (codec runtime types)
  - `src/main/java/io/github/tt432/eyelib/util/ResourceLocations.java` (resource-location helper)
  - `src/main/java/io/github/tt432/eyelib/util/math/Shapes.java` (`RandomSource` helper)
- No surviving `util/client/**` seam in this slice exposes direct MC/Forge/Blaze imports after the wave-4 cleanup.

## Targeted tests for extracted pure helpers
- Added `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java` covering:
  - texture emissive path derivation
  - ARGB→ABGR channel conversion
  - list first/last helper behavior
  - `Either` unwrap behavior for both left/right branches
- Added `src/test/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerStateTest.java` covering:
  - immediate first-step behavior after `start`
  - rate-based stepped-second conversion
  - one-step-per-call catch-up behavior under elapsed-time jumps
  - tick + partial-tick real-seconds conversion

## JetBrains MCP verification (explicit)
- `jetbrain_get_file_problems filePath="src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java"` → **no errors**.
- `jetbrain_run_gradle_tasks taskNames=["test"] scriptParameters="--tests io.github.tt432.eyelib.core.util.time.FixedStepTimerStateTest"` → **exitCode 0**.
- `jetbrain_build_project` (project build) → **success** (`isSuccess=true`, no reported problems).
- Rule scans for this affected slice:
  - `jetbrain_search_regex` over `util/client/**` for `net.minecraft|net.minecraftforge|com.mojang.blaze3d` imports → **no matches** after deleting legacy wrappers.
  - `jetbrain_search_regex` over `util/**` for same imports → remaining matches are explicitly tracked in `util/codec/*`, `util/ResourceLocations.java`, and `util/math/Shapes.java`.

## Verification status update
- This retry slice now has fresh JetBrains MCP confirmation: targeted test task and project build both pass in-session.
- The wave-4 helper cleanup leaves no direct MC/Forge/Blaze imports under `util/client/**`; remaining utility blockers are limited to codec/resource/math helpers called out above.

## Notes
- This slice now has concrete helper-surface reduction evidence (legacy util helper files removed/moved/deleted) with fresh JetBrains MCP confirmation in this session.

## Deliverables
- Design the utility split and ownership map.
- Add tests for pure helper extraction.
- Implement minimal, low-risk relocations and adapters.

## Dependencies
- First wave after `molang-mc-adapters`.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [x] Re-baseline remaining Minecraft/Forge references for this slice (`util/modbridge/*`, selected top-level `util/*`).
- [x] Move or confine this slice’s bridge/event and timer runtime ownership to allowed `mc/impl` packages.
- [x] Keep surviving `core` helpers in this slice fully free of Minecraft/Forge types.
- [x] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass full-module rule-based boundary scan for this module.
