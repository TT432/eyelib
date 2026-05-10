# Phase 18 Context: Resource, Texture & MC-Dependent Migration

## Goal
- Move resource-location and texture-path utilities into `:eyelib-util` without preserving root utility wrappers.
- Resolve the `ResourceLocations.mod()` root-cycle by deleting it because Phase 15 and current scans found no source callers.
- Keep `:eyelib-util` a leaf module with zero `project(...)` dependencies.

## Affected Modules
- `:eyelib-util` shared utility leaf module.
- Root client animation, render, render sync, and Molang MC mapping callers that consume resource-location or texture-path helpers.
- Root/core util package cleanup for migrated resource and texture files.
- Documentation modules: `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, and `docs/architecture/migration/utility-routing-manifest.md`.

## Current Evidence
- `src/main/java/io/github/tt432/eyelib/util/ResourceLocations.java` imports `io.github.tt432.eyelib.Eyelib` only for unused `mod(String)`.
- Current `ResourceLocations` production callers use only `of(...)`:
  - `client/animation/bedrock/BrAnimationEntryDefinition.java`
  - `client/render/sync/ClientRenderSyncService.java`
  - `mc/impl/molang/mapping/MolangQuery.java`
- Current test caller:
  - `src/test/java/io/github/tt432/eyelib/client/render/sync/RenderSyncApplyOpsTest.java`
- `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java` is a root wrapper over `core/util/texture/TexturePaths`.
- Current texture-path production callers:
  - `client/render/RenderParams.java` via `TexturePathHelper`
  - `client/render/controller/RenderControllerEntry.java` via `core.util.texture.TexturePaths`
- Current texture-path test caller:
  - `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java`

## Decisions
- Delete `ResourceLocations.mod(String)` rather than parameterize it because there are no source callers and keeping it would require a root mod-id dependency or extra compatibility surface.
- Move callers directly to `io.github.tt432.eyelibutil.resource.ResourceLocations` and `io.github.tt432.eyelibutil.texture.TexturePaths`.
- Do not keep `TexturePathHelper` as a compatibility shim; Phase 18 success criteria requires no duplicate wrapper in root.

## Verification Gates
- IDE diagnostics on touched Java files.
- `:eyelib-util:build` via JetBrains MCP.
- Full project rebuild via JetBrains MCP.
- Text searches proving no imports of `io.github.tt432.eyelib.util.ResourceLocations`, `io.github.tt432.eyelib.util.client.texture`, or `io.github.tt432.eyelib.core.util.texture` remain in Java source.
- `eyelib-util/build.gradle` still contains zero `project(...)` dependencies.
