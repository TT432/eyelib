# client-render

## Scope
- Render pipeline, render params, sync application, textures, visitors.
- Main paths: `client/render/`

## Why it is MC-facing
- Uses `Minecraft`, render types, texture systems, pose stack, event bus, and client runtime state.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: in progress (hard-import slice advanced).
- Expected final state for this module: rendering, texture upload, render-type binding, visitor execution, and live client runtime lookup stay in `mc/impl`; only stable render-facing contracts or pure state/application seams may remain outside it.

## Target seam
- Keep actual rendering in `mc/impl`.
- Extract only stable data/decision interfaces needed by `core` and higher-level runtime logic.

## Deliverables
- Design minimal render-facing ports.
- Add tests for extracted pure decision/state code where feasible.
- Implement seam without broad render rewrite.

## Dependencies
- After `client-model-animation-entity`.

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
- [ ] Confine rendering, texture upload, render-type binding, visitors, and live client lookup code to allowed `mc/impl` packages.
- [ ] Keep render-facing contracts and pure apply/state seams free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- Large portions of `client/render/**` still directly import Minecraft/Forge/Blaze3D runtime types, including `RenderParams`, `RenderHelper`, `SimpleRenderAction`, `EyelibLivingEntityRenderer`, `visitor/**`, `texture/NativeImageIO`, and `texture/TextureLayerMerger`; these remain legacy MC-facing package residents until moved under allowed `mc/impl` ownership.
- `RenderTypeResolver` is not yet a platform-free seam: its public contract still exposes `RenderType` and `ResourceLocation`, so any surviving non-`mc/impl` render decision layer will need a new platform-type-free contract instead of this current shape.
- `ClientRenderSyncService` still performs live client entity lookup through `Minecraft.getInstance()` and therefore remains implementation-side runtime wiring, not a portable contract.
- `RenderControllerEntry` still embeds `MissingTextureAtlasSprite` and `ResourceLocation`, which means render-controller definitions are not yet cleanly separated from MC client asset/runtime types.
- `RenderSyncApplyOps` model payload seam is now platform-type-free (`RenderModelSyncPayload` with string identifiers) and no longer exposes `ModelComponent.SerializableInfo`/`ResourceLocation` in collect/replace signatures. Runtime decode back to `ModelComponent.SerializableInfo` now lives in `ClientRenderSyncService`.
- Next render quarantine steps should move runtime-heavy owners such as `RenderTypeResolver`, `RenderParams`, visitors, renderers, and texture upload/merge into `mc/impl`.
- Local package guidance now exists at `src/main/java/io/github/tt432/eyelib/client/render/README.md`; keep it updated as runtime owners move toward `mc/impl`.

## Priority hotspot files for next slice
- Highest import-density runtime owners currently worth attacking first:
  - `client/render/RenderParams.java`
  - `client/render/EyelibLivingEntityRenderer.java`
  - `client/render/SimpleRenderAction.java`
  - `client/render/texture/TextureLayerMerger.java`
  - `client/render/texture/NativeImageIO.java`
- Lower-risk seam-first starting point remains:
  - `client/render/sync/RenderSyncApplyOps.java`
  - payload types consumed by `RenderSyncApplyOpsTest`

## Progress notes
- Added a narrow apply seam at `client/render/sync/RenderSyncApplyOps.java` to isolate stable render-state mutation from Minecraft runtime lookup/wiring.
- Tightened model sync payload at this seam to `client/render/sync/RenderModelSyncPayload.java` (`String` model/texture/renderType keys), and updated `ModelComponentSyncPacket` to transport this platform-type-free payload instead of `ModelComponent.SerializableInfo`.
- Kept MC-facing behavior in `ClientRenderSyncService`: entity resolution (`Minecraft.getInstance().level.getEntity`), capability lookup, and packet wiring remain unchanged.
- Extracted apply operations:
  - `collectSerializableModelInfo(List<ModelComponent>) -> List<RenderModelSyncPayload>`
  - `replaceModelComponents(List<ModelComponent>, List<RenderModelSyncPayload>, decoder)`
  - `applyAnimationInfo(RenderData<?>, AnimationComponent.SerializableInfo)`
- Added targeted seam tests in `src/test/java/io/github/tt432/eyelib/client/render/sync/RenderSyncApplyOpsTest.java` for model payload collection/replacement semantics and animation info application.

## Boundary check (Oracle guidance)
- Kept MC-only: `RenderParams`, visitors/renderers, texture upload/merge (`NativeImageIO`, `TextureLayerMerger`), render-type resolution (`RenderTypeResolver`), event wiring, and direct `Minecraft` entity lookup.
- Extracted only decision/apply seam logic needed by higher-level packet-apply flow.

## Verification
- JetBrains MCP Gradle test (targeted): `test --tests io.github.tt432.eyelib.client.render.sync.RenderSyncApplyOpsTest` ✅
- JetBrains MCP project file checks (`jetbrain_get_file_problems`) on changed Java/test files: ✅ (no errors/warnings)
- JetBrains MCP build (`jetbrain_build_project`): ✅
- JetBrains MCP Gradle build: `build` ✅
- Java LSP diagnostics (`lsp_diagnostics`): unavailable in this session because `jdtls` is not installed (tooling blocker, unrelated to compile/test outcomes).
