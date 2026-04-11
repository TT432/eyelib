# client-loaders

## Scope
- Resource reload listeners and asset parsing.
- Main paths: `client/loader/`

## Why it is MC-facing
- Uses client reload listener registration and `ResourceManager` lifecycle.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: pending re-baseline.
- Expected final state for this module: reload listener registration and resource-manager lifecycle stay in `mc/impl`; parsing contracts or translation seams may live outside `mc/impl` only if they remain free of Minecraft/Forge types.

## Target seam
- Separate parsing/translation logic from resource reload registration and publication wiring.
- Preserve existing loader pattern.

## Implemented seam design
- Added `client/loader/LoaderParsingOps` as a pure decode/translation helper for codec-based loader parsing.
- Moved Forge reload-listener lifecycle wiring into `mc/impl` via `mc/impl/client/loader/ClientLoaderLifecycleHooks`.
- Kept publication wiring in loader `apply(...)` methods routed to existing domain registry seams (`client/registry/*`), with parsing now delegated.
- Reused first-wave manager/registry seams by not introducing any competing abstraction layer.

## Implementation summary
- Refactored codec-based loaders to call `LoaderParsingOps` for parsing/translation:
  - `BrAnimationLoader`
  - `BrAnimationControllerLoader`
  - `BrParticleLoader`
  - `BrMaterialLoader`
  - `BrRenderControllerLoader`
  - `BrClientEntityLoader`
  - `BrAttachableLoader`
- Extracted model parsing within `BrModelLoader` into `parseLoadedModels(...)` so parse/translation is distinct from publication (`ModelAssetRegistry.replaceModels(...)`).
- Removed per-loader Forge event annotations/imports from concrete `Br*Loader` classes; registration now happens only in `ClientLoaderLifecycleHooks` under `mc/impl`.
- Updated `LoaderParsingOps` contract from `ResourceLocation`-keyed methods to source-key-generic methods (`parseBySourceKey`, generic `parseAndTranslate`) so parsing/translation seams outside `mc/impl` no longer expose Minecraft identifier types.
- Added targeted seam test: `src/test/java/io/github/tt432/eyelib/client/loader/LoaderParsingOpsTest.java`.

## JetBrains MCP verification results
- Targeted test run (`jetbrain_run_gradle_tasks`, `test --tests io.github.tt432.eyelib.client.loader.LoaderParsingOpsTest`): **PASS** (`exitCode=0`).
- Full build verification (`jetbrain_execute_run_configuration`, `qylEyelib [build]`): **PASS** (`exitCode=0`, `BUILD SUCCESSFUL`).
- Current hard-import slice verification attempt: **PARTIAL (JetBrains MCP run-config path available, some MCP operations unavailable)**.
  - `jetbrain_execute_run_configuration` (`qylEyelib [build]`): **FAIL (unrelated repository-wide tests)** — `FixedStepTimerStateTest` failures in `core/util/time`, outside `client-loaders` scope.
  - `jetbrain_get_file_problems`: unavailable (`Tool get_file_problems not found`).
  - `jetbrain_run_gradle_tasks`: unavailable (`Tool run_gradle_tasks not found`).
  - `jetbrain_build_project`: unavailable (`Tool build_project not found`).
  - Rule-based boundary scans for this slice were run with repository grep tools and captured in this tracker update.

## Deliverables
- Design parser-facing interfaces/ports.
- Add tests for extracted parsing/translation code.
- Implement split and migrate registry publication callers.

## Dependencies
- After `client-managers-registry` and `utility-mc-bridges`.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [ ] JetBrains MCP verification

## Final isolation checklist
- [x] Re-baseline remaining Minecraft/Forge references for this module.
- [ ] Confine reload-listener registration and resource-manager lifecycle code to allowed `mc/impl` packages.
- [x] Keep parser/translation seams free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- Forge reload-listener registration ownership is now under `mc/impl` (`ClientLoaderLifecycleHooks`); concrete `Br*Loader` classes no longer own `RegisterClientReloadListenersEvent` lifecycle wiring.
- `SimpleJsonWithSuffixResourceReloadListener` remains a Minecraft-backed reload abstraction (`FileToIdConverter`, `ResourceManager`, `SimplePreparableReloadListener`) in `client/loader` and still needs relocation/isolation for full final state.
- `LoaderParsingOps` and its tests are now platform-type-free for key translation contracts (no `ResourceLocation` in method signatures).
- `BrClientEntityLoader` and `BrAttachableLoader` still materialize `ResourceLocation` when translating parsed identifiers for current publication flow; follow-up is needed if these loaders are further decomposed toward full module quarantine.
