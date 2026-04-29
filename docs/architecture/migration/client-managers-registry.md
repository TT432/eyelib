# client-managers-registry

## Scope
- Runtime stores, lookup seams, and publication boundaries.
- Main paths: `client/manager/`, `client/registry/`

## Why it is MC-facing
- Storage itself is close to core-worthy, but current managers post Forge events and are wired into MC-facing publication.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: code isolation complete for the manager/registry hard-import-quarantine scope; full module verification currently blocked by unrelated repository compile failures.
- Expected final state for this module: read/write/storage ports may live outside `mc/impl`, but event-bus publication, registration, and any direct Minecraft/Forge interaction must end up only in allowed `mc/impl` packages.

## Target seam
- Extract storage/read/write ports.
- Keep event-bus posting and runtime registration in `mc/impl`.

## Deliverables
- Design role-based ports for lookup and publication.
- Add tests for pure storage semantics.
- Implement split without breaking current manager pattern.

## Dependencies
- After `utility-mc-bridges`.

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
- [x] Confine event-bus posting and runtime registration code to allowed `mc/impl` packages.
- [x] Keep read/write/storage ports free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [x] Pass rule-based boundary scan for this module.

## Progress
- Completed next-wave seam extraction for `client/manager` + `client/registry` with minimal churn and preserved manager pattern.
- Completed hard-import-quarantine follow-up: direct Forge event-bus publication and `ResourceLocation` publication seam leakage were removed from `client/manager` and `client/registry`.

## Interface/Port Design (completed)
- Added role-based manager ports in `client/manager/`:
  - `ManagerReadPort<T>` for lookup/snapshot reads (`get`, `getAllData`, `getManagerName`).
  - `ManagerWritePort<T>` for publication/storage mutation (`put`, `replaceAll`, `clear`).
- Added pure storage owner `ManagerStorage<T>` (package-private) to hold map semantics without Forge coupling.
- Updated `Manager<T>` to implement both ports and delegate storage to `ManagerStorage<T>`.
- Moved manager event publication behind `mc/api` contract:
  - `mc/api/client/manager/ManagerEventPublisher`
- Kept the publication interface in `mc/api` and moved the concrete bridge holder to `client/manager/ManagerEventPublishBridge` so `mc/api` remains interface-only.
- Bound Forge event publication in `mc/impl`:
  - `mc/impl/client/manager/ForgeManagerEventPublisher`
  - `mc/impl/client/manager/ManagerEventLifecycleHooks`
- Exposed typed role ports from each concrete manager via `readPort()` / `writePort()` helpers.

## Implementation Changes (completed)
- Lookup seams now consume read ports:
  - `client/animation/AnimationLookup.java`
  - `client/model/ModelLookup.java`
  - `client/particle/ParticleLookup.java`
  - `client/render/controller/RenderControllerLookup.java`
  - `client/entity/ClientEntityLookup.java`
- Registry publication code now consumes write ports instead of concrete singleton writes:
  - `client/registry/AnimationAssetRegistry.java`
  - `client/registry/MaterialAssetRegistry.java`
  - `client/registry/ParticleAssetRegistry.java`
  - `client/registry/RenderControllerAssetRegistry.java`
  - `client/registry/ClientEntityAssetRegistry.java`
  - `client/registry/ModelAssetRegistry.java`
- `client/manager/Manager.java` no longer imports `MinecraftForge`; it publishes entry changes through `client/manager/ManagerEventPublishBridge` while the publisher interface remains in `mc/api`.
- `client/registry/ClientEntityAssetRegistry.java` no longer imports `ResourceLocation`; bulk replacement now accepts `Iterable<BrClientEntity>` and keys by `identifier()`.
- Updated callers (`BrClientEntityLoader`, `ManagerResourceImportPlanner`) to publish client entities through identifier-based replacement without platform identifiers in the registry seam.

## Targeted Tests (completed)
- Added `src/test/java/io/github/tt432/eyelib/client/manager/ManagerStorageTest.java` for pure storage semantics:
  - snapshot isolation (`getAllData` copy semantics)
  - replacement overwrite semantics (`replaceAll` clears stale entries)
  - clear semantics (`clear` empties storage)
- Reused existing publication seam coverage:
  - `ClientAssetRegistryTest`
  - `ClientLookupFacadeTest`
- Added `src/test/java/io/github/tt432/eyelib/client/manager/ManagerEventPublishBridgeTest.java` for manager event publish bridge behavior without Forge runtime.
- Added `src/test/java/io/github/tt432/eyelib/client/registry/ClientEntityAssetRegistryTest.java` for identifier-based client entity replacement semantics.

## JetBrains MCP Verification (current run)
- File inspections on modified/new manager/registry slice files report **no errors**.
- Targeted Gradle test invocation attempted via JetBrains MCP:
  - `test --tests io.github.tt432.eyelib.client.manager.ManagerStorageTest --tests io.github.tt432.eyelib.client.manager.ManagerEventPublishBridgeTest --tests io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistryTest --tests io.github.tt432.eyelib.client.registry.ClientAssetRegistryTest --tests io.github.tt432.eyelib.client.lookup.ClientLookupFacadeTest`
  - Result: **FAILED** before tests executed due unrelated compile errors in other modules (`DataAttachmentHelper` / `DataAttachmentContainerCapability` missing).
- JetBrains MCP file-scoped build attempt on changed files also fails for the same unrelated repository-wide compile blocker.
