# client-model-animation-entity

## Scope
- Model, animation, and client-entity definitions/runtime seams.
- Main paths: `client/model/`, `client/animation/`, `client/entity/`

## Why it is MC-facing
- Many mixed types use MC codecs, asset identifiers, rendering helpers, and Molang runtime queries.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: pending re-baseline (second hard-import slice on model-definition side landed).
- Expected final state for this module: pure definition/state types and role-based ports may live outside `mc/impl`, but codecs, runtime lookup binding, rendering hooks, and any Minecraft-backed identifiers or helpers must be isolated to allowed `mc/impl` packages unless converted to platform-free contracts.

## Target seam
- Separate pure definition/state types from MC serialization, runtime lookup, and rendering/application hooks.
- Preserve codec-heavy existing patterns.

## Deliverables
- Identify interface boundaries per subdomain.
- Add tests around extracted pure definitions/state transitions.
- Implement split with minimal churn.

## Dependencies
- After `molang-mc-adapters`, `client-managers-registry`, and `client-loaders`.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [ ] Re-baseline remaining Minecraft/Forge references for this module.
- [ ] Confine MC codecs, runtime binding, and render/application hooks to allowed `mc/impl` packages.
- [ ] Keep definition/state ports and DTOs free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- This module still has direct MC runtime dependencies in implementation-heavy classes such as `BrAnimationEntry`, `BrAnimationController`, `ModelBakeInfo`, and model bake/runtime helpers; these remain legacy MC-facing package residents until moved under allowed `mc/impl` ownership.
- The remaining gap is not limited to runtime hooks: several definition/runtime-adjacent types still expose Minecraft/Blaze classes directly, including `Model` traversal/runtime hooks (`PoseStack`), selected bake/runtime classes (`ResourceLocation`), importer/runtime texture flow (`NativeImage`), `BrBoneKeyFrame` (`StringRepresentable`), and model-part wrappers (`ModelPart`, `PartPose`).
- Final isolation for this module therefore likely needs two coordinated tracks: platform-type-free definition/state contracts for surviving non-`mc/impl` assets, and separate `mc/impl` ownership for bake/runtime/render/application logic.
- Current safest implementation order is now clearer from code/test shape: keep using already plain-JVM-covered state seams such as `BrAnimationPlaybackState` as the first expansion point, then relocate bake/runtime-heavy owners (`ModelBakeInfo`, `Model`, importer/runtime helpers) into `mc/impl` slices instead of attempting a single whole-module rewrite.
- `client/entity` low-risk lookup slice is now partially landed: `ClientEntityLookup` no longer imports `ResourceLocation`, and id adaptation now happens in MC-facing callers.
- Local package README guidance now exists for `client/animation/`, `client/model/`, and `client/entity/`.

## Documentation follow-up
- When this module enters active implementation, add package-local `README.md` guidance for:
  - `src/main/java/io/github/tt432/eyelib/client/animation/`
  - `src/main/java/io/github/tt432/eyelib/client/model/`
  - `src/main/java/io/github/tt432/eyelib/client/entity/`
- Those README files should explicitly split platform-free definition/state ownership from bake/runtime/render ownership that must move into `mc/impl`.

### Documentation follow-up status
- [x] `src/main/java/io/github/tt432/eyelib/client/animation/README.md`
- [x] `src/main/java/io/github/tt432/eyelib/client/model/README.md`
- [x] `src/main/java/io/github/tt432/eyelib/client/entity/README.md`

## Progress notes
- Added animation playback seam `BrAnimationPlaybackState` to isolate pure loop/time transition logic from `BrAnimationEntry` runtime hooks.
- Added client-entity model lookup seam `ClientEntityModelLookup` and routed `ClientEntityRuntimeData` through the port (default bound to `ModelLookup::get`).
- Converted `ClientEntityLookup` to a platform-type-free string-id seam (`get(String entityId)`), removing the direct `ResourceLocation` import from `client/entity`.
- Updated `EntityRenderSystem.setupClientEntity(ResourceLocation, RenderData<?>)` to perform local id adaptation (`entityId.toString()`) at the MC-facing caller boundary.
- Added targeted pure/state tests:
  - `BrAnimationPlaybackStateTest`
  - `ClientEntityRuntimeDataTest`
  - `ClientEntityLookupTest`
- Added Stage-1 characterization coverage for the current animation refactor seam:
  - `BrAnimationEntryCharacterizationTest`
  - `BrAnimationControllerBehaviorTest`
  - `AnimationComponentSerializableInfoTest`
- Added Stage-4 seam coverage for internal runtime-owner extraction:
  - `BrAnimationEntryLifecycleTest`
  - `BrAnimationControllerStateOwnerTest`
- Narrow Stage-5 cleanup completed: pure runtime forwarding sites no longer depend on `AnimationRuntimes`, while wrapper-backed ingress/publish/GUI seams remain deferred by design.
- Landed model-definition follow-up seam: `Model.TextureMesh.CODEC` now uses Eyelib float-list vector codecs, removing direct `net.minecraft.util.ExtraCodecs` import from `client/model/Model.java` while keeping JSON shape compatibility for list-based vector fields.
- Added targeted plain-JVM seam test coverage for the extracted definition contract:
  - `ModelTextureMeshCodecTest`
- Remaining blockers after this slice are explicit: heavy model/runtime owners still expose MC/Blaze types (`PoseStack` in `Model`, `ModelPart`/`PartPose` in part adapters, and `NativeImage` in importer/runtime texture flow) and require later quarantine/move decisions.
- JetBrains MCP verification:
  - `:test --tests io.github.tt432.eyelib.client.entity.ClientEntityLookupTest --tests io.github.tt432.eyelib.client.entity.ClientEntityRuntimeDataTest` (via `jetbrain_run_gradle_tasks`) ✅
  - IDE build check (via `jetbrain_build_project`) ✅
  - `:test --tests io.github.tt432.eyelib.client.model.ModelTextureMeshCodecTest` (via `jetbrain_run_gradle_tasks`) ✅ *(this retry)*
  - IDE build check (via `jetbrain_build_project`) ✅ *(this retry)*
