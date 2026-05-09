---
phase: 09-particle-api-store-seam
plan: 02
subsystem: particle-api
tags: [java17, gradle, forge, particle, api, store, facade, tdd]

requires:
  - phase: 09-particle-api-store-seam
    provides: String-keyed particle API/store/publication/spawn contracts from Plan 01
provides:
  - Root ParticleManager backing adapter for module-owned ParticleStore
  - Transitional ParticleLookup and ParticleAssetRegistry facades delegating to particle API seams
  - Transitional ParticleSpawnService adapter delegating packet spawn/remove through ParticleSpawnApi
affects: [phase-09-particle-api-store-seam, phase-10-schema-runtime-ownership, phase-11-runtime-client-core-extraction, phase-12-loading-publication-rewire, phase-13-command-network-integration-rewire]

tech-stack:
  added: []
  patterns: [root backing adapter, transitional facade delegation, identifier-based publisher seam, source-boundary tests]

key-files:
  created:
    - src/test/java/io/github/tt432/eyelib/client/manager/ParticleManagerStoreAdapterTest.java
    - src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryPublisherAdapterTest.java
    - src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java
  modified:
    - MODULES.md
    - src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java
    - src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java
    - src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java
    - src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java
    - src/main/java/io/github/tt432/eyelib/client/particle/README.md
    - src/main/java/io/github/tt432/eyelib/client/registry/README.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
    - src/test/java/io/github/tt432/eyelib/client/lookup/ClientLookupFacadeTest.java

key-decisions:
  - "Kept root compatibility classes as named transitional adapters instead of adding a broad compatibility layer."
  - "Bound root runtime BrParticle storage to generic particle-module APIs through ParticleManager, preserving one-way root -> particle dependency direction."
  - "Kept Minecraft, capability, packet, and render-manager behavior in root while using module ParticleSpawnRequest/ParticleSpawnApi as the request seam."

patterns-established:
  - "Root facades expose/consume module API seams while preserving existing static call signatures for current callers."
  - "Particle publication ignores source map keys and routes replacement through ParticlePublisher identifier extraction."
  - "Boundary tests combine behavior checks with source/static checks where runtime Minecraft internals should not be moved."

requirements-completed: [PAPI-01, PAPI-03]

duration: 37 min
completed: 2026-05-09
---

# Phase 09 Plan 02: Particle API & Store Seam Summary

**Root particle manager, lookup, publication, and spawn facades now delegate to module-owned particle API seams while keeping runtime internals in root**

## Performance

- **Duration:** 37 min
- **Started:** 2026-05-09T12:27:00+08:00
- **Completed:** 2026-05-09T13:04:15+08:00
- **Tasks:** 3 completed
- **Files modified:** 12

## Accomplishments

- Made `ParticleManager` implement `ParticleStore<BrParticle>` and added `store()` as the module API backing adapter while preserving existing `readPort()`/`writePort()` compatibility.
- Delegated `ParticleLookup` through `ParticleLookupApi` and `ParticleAssetRegistry` through `ParticlePublisher`, preserving `particle_effect.description.identifier` key semantics.
- Delegated packet spawn/remove entrypoints through `ParticleSpawnApi` and module `ParticleSpawnRequest` while keeping `Minecraft`, `DataAttachmentHelper`, `BrParticleEmitter`, and `BrParticleRenderManager` in root runtime code.
- Documented retained root facades as transitional adapters with delegation targets and removal conditions in local READMEs/Javadocs.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Make ParticleManager the root backing adapter for module store APIs** - `9ad7c18` (test)
2. **Task 1 GREEN: Make ParticleManager the root backing adapter for module store APIs** - `afc1a14` (feat)
3. **Task 2 RED: Delegate lookup and publication facades to particle APIs** - `c2d1a07` (test)
4. **Task 2 GREEN: Delegate lookup and publication facades to particle APIs** - `1143f54` (feat)
5. **Task 3 RED: Delegate spawn/remove through module request API without moving runtime internals** - `a6749e2` (test)
6. **Task 3 GREEN: Delegate spawn/remove through module request API without moving runtime internals** - `41b23b8` (feat)

**Plan metadata:** committed after this summary in the final docs commit.

## Files Created/Modified

- `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` - Implements `ParticleStore<BrParticle>`, adds `store()`, and documents compatibility accessors.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` - Delegates root lookup calls through `ParticleLookupApi` while keeping `ResourceLocation` adaptation at the root boundary.
- `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` - Delegates publish/replace operations through `ParticlePublisher` using particle description identifiers.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` - Delegates spawn/remove entrypoints through `ParticleSpawnApi` while retaining root runtime/render implementation.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` - Documents `ParticleLookup` and `ParticleSpawnService` as transitional facades with removal conditions.
- `src/main/java/io/github/tt432/eyelib/client/registry/README.md` - Documents `ParticleAssetRegistry` delegation and transitional status.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` - Records Phase 9 API responsibilities and root facade delegation targets.
- `MODULES.md` - Updates particle module/runtime manager interactions for the module-owned API/store contract.
- `src/test/java/io/github/tt432/eyelib/client/manager/ParticleManagerStoreAdapterTest.java` - Covers manager-backed `ParticleStore` behavior.
- `src/test/java/io/github/tt432/eyelib/client/lookup/ClientLookupFacadeTest.java` - Adds lookup API adapter coverage.
- `src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryPublisherAdapterTest.java` - Covers `ParticlePublisher` delegation and description-identifier replacement keys.
- `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java` - Guards spawn API delegation and no reverse root imports in `:eyelib-particle` sources.

## Decisions Made

- Kept root facades (`ParticleLookup`, `ParticleAssetRegistry`, `ParticleSpawnService`) as specific deletion-ready adapters instead of introducing any generic compatibility package.
- Used the generic module API contracts from Plan 01 with `BrParticle` bound only in root, preserving `:eyelib-particle` root-clean constraints.
- Preserved string-keyed packet and publication behavior; `ResourceLocation` remains only on the root lookup overload and source map keys remain ignored for particle replacement.

## TDD Gate Compliance

- RED gate commits exist for all three tasks: `9ad7c18`, `c2d1a07`, `a6749e2`.
- GREEN gate commits exist after each RED gate: `afc1a14`, `1143f54`, `41b23b8`.
- No separate refactor commit was needed; formatting of `ParticleSpawnService.java` was applied before the Task 3 GREEN commit.

## Verification

- **Task 1 RED:** JetBrains MCP `:test --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest` failed as expected because `ParticleManager.store()` did not exist.
- **Task 1 GREEN:** JetBrains MCP targeted test plus `:compileJava` exited 0; PowerShell static check confirmed `ParticleStore` import, `extends Manager<BrParticle>`, and `store()` accessor.
- **Task 2 RED:** JetBrains MCP targeted tests failed as expected because `ParticleLookup.api()` and `ParticleAssetRegistry.publisher()` did not exist.
- **Task 2 GREEN:** JetBrains MCP targeted tests plus `:compileJava` exited 0; PowerShell static checks confirmed `ParticlePublisher`, description identifier extraction, `eyelibparticle.api` lookup delegation, and transitional documentation.
- **Task 3 RED:** JetBrains MCP `ParticleSpawnServiceBoundaryTest` failed as expected because `ParticleSpawnService` did not yet import/delegate to module spawn APIs.
- **Task 3 GREEN:** JetBrains MCP targeted boundary test plus `:compileJava` exited 0; PowerShell static check confirmed `ParticleSpawnApi`, module `ParticleSpawnRequest`, root runtime imports, and no forbidden root imports under `eyelib-particle/src/main/java`.
- **Plan-level verification:** JetBrains MCP `:eyelib-particle:compileJava :compileJava :test` with the targeted Phase 9 tests exited 0; overall static boundary check passed.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None beyond expected TDD RED failures.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Threat Flags

None - this plan rewired existing root compatibility and packet/runtime adapter seams without adding a new network endpoint, auth path, external file access pattern, or schema trust boundary.

## Next Phase Readiness

- Phase 9 Plan 03 can build on root facades that now consume module store/publication/spawn API seams.
- Later schema/runtime ownership and runtime extraction phases can migrate callers away from root transitional facades using the documented removal conditions.

## Self-Check: PASSED

- Created test files exist on disk.
- Task commits `9ad7c18`, `afc1a14`, `c2d1a07`, `1143f54`, `a6749e2`, and `41b23b8` exist in git history.
- JetBrains MCP compile/test verification and PowerShell static boundary checks passed after GREEN implementations.

---
*Phase: 09-particle-api-store-seam*
*Completed: 2026-05-09*
