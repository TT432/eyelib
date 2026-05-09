---
phase: 12-loading-publication-rewire
plan: 02
subsystem: particle-loading
tags: [java, gradle, junit5, particle, loading, publication, compatibility]

requires:
  - phase: 12-loading-publication-rewire
    provides: module-owned ParticleDefinition registry and JSON publication seam from Plan 01
provides:
  - root reload adapter delegation from ResourceLocation-keyed JSON resources into ParticleResourcePublication
  - root registry/manager/lookup compatibility over module-owned active ParticleDefinition publication
  - packet spawn path lookup of module ParticleDefinition without root legacy BrParticle round-trip conversion
  - targeted root tests for loader, registry, manager, lookup, boundary, and spawn compatibility
affects: [phase-12-loading-publication-rewire, phase-13-command-network-integration-rewire, phase-14-verification-documentation-gate]

tech-stack:
  added: []
  patterns: [root compatibility adapter, module-owned ParticleDefinition publication, JSON publication seam reuse, string-keyed active lookup]

key-files:
  created:
    - src/test/java/io/github/tt432/eyelib/client/loader/BrParticleLoaderPublicationTest.java
  modified:
    - src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java
    - src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java
    - src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java
    - src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java
    - src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java
    - src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java
    - src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryTest.java
    - src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryPublisherAdapterTest.java
    - src/test/java/io/github/tt432/eyelib/client/manager/ParticleManagerStoreAdapterTest.java
    - src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java
    - src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java

key-decisions:
  - "Root reload now converts ResourceLocation source ids to strings and delegates parse/convert/publish ownership to ParticleResourcePublication."
  - "ParticleAssetRegistry remains only as a legacy root compatibility adapter while active publication uses ParticleDefinitionRegistry.publisher()."
  - "Packet-driven spawn now looks up ParticleDefinition from the module active registry directly; legacy BrParticle conversion remains only for current root compatibility callers."

patterns-established:
  - "Root compatibility maps may retain legacy BrParticle values, but names and packet spawn definitions come from ParticleDefinitionRegistry.store()."
  - "Single-file tooling imports use ParticleResourcePublication.publishFromJsonResource so they do not accidentally clear the active registry."

requirements-completed: [PLOAD-01, PLOAD-02, PLOAD-03]

duration: 20min
completed: 2026-05-09
---

# Phase 12 Plan 02: Root Publication Adapter Rewire Summary

**Root particle reload, registry, lookup, tooling, and packet spawn adapters now publish and read module-owned ParticleDefinition entries while retaining narrow legacy compatibility maps**

## Performance

- **Duration:** 20 min
- **Started:** 2026-05-09T11:33:57Z
- **Completed:** 2026-05-09T11:52:50Z
- **Tasks:** 2
- **Files modified:** 13

## Accomplishments
- Added RED coverage for `BrParticleLoader` preserving `particles/*.json` scanning while delegating publication to `ParticleResourcePublication` instead of the root legacy particle codec.
- Rewired `BrParticleLoader`, `ParticleAssetRegistry`, `ParticleLookup`, `ParticleManager`, `ParticleSpawnService`, and manager tooling import paths to use module-owned active `ParticleDefinitionRegistry` publication and lookup semantics.
- Preserved current root compatibility callers by keeping legacy `BrParticle` access in named adapters while ensuring active names and packet spawn definitions come from the module store.

## Task Commits

Each task was committed atomically:

1. **Task 1: Specify root adapter delegation and compatibility behavior** - `56adc82` (test)
2. **Task 2: Rewire root reload, registry, lookup, tooling, and spawn adapters** - `2bb047c` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD tasks used separate RED and GREEN commits._

## Files Created/Modified
- `src/test/java/io/github/tt432/eyelib/client/loader/BrParticleLoaderPublicationTest.java` - Static root reload adapter coverage for resource scan contract and module publication delegation.
- `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` - Converts `ResourceLocation` source ids to strings and delegates JSON publication to `ParticleResourcePublication`.
- `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` - Publishes active module `ParticleDefinition` values while maintaining the root legacy compatibility map.
- `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` - Documents the manager as a legacy compatibility map, not active publication ownership.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` - Reads active names from `ParticleDefinitionRegistry.store()` while legacy `get(...)` remains bridged.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` - Packet spawn path looks up module `ParticleDefinition` and builds module runtime emitters directly.
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java` - Routes particle JSON tooling imports through module publication services.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java` - Adds single-resource publish support for tooling imports without full registry replacement.
- Root registry/manager/particle tests - Updated assertions to lock module active publication and legacy compatibility boundaries.

## Verification

- **RED gate:** JetBrains MCP `:test --tests io.github.tt432.eyelib.client.loader.BrParticleLoaderPublicationTest --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryTest --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryPublisherAdapterTest --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest` failed before implementation because `ParticleAssetRegistry.publisher()` still returned `ParticlePublisher<BrParticle>` (External task id 310).
- **GREEN gate:** JetBrains MCP `:eyelib-particle:test :test --tests io.github.tt432.eyelibparticle.loading.* --tests io.github.tt432.eyelibparticle.api.* --tests io.github.tt432.eyelib.client.loader.BrParticleLoaderPublicationTest --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryTest --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryPublisherAdapterTest --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest` exited 0 (External task id 312).
- **Acceptance checks:** The targeted test suite confirms `BrParticleLoader` retains `super("particles", "json")`, no longer uses root `BrParticle.CODEC`, `ParticleAssetRegistry` delegates to `ParticleDefinitionRegistry`, packet spawn uses module definitions, and command/packet contract files were not modified.

## Decisions Made
- Active publication ownership now lives in `ParticleDefinitionRegistry.publisher()` for root reload, root registry, and tooling imports.
- Root `ParticleManager` stays as an explicit legacy compatibility map for callers that still require root `BrParticle` objects, while `ParticleLookup.names()` reports module active identifiers.
- Single-file particle JSON imports publish one module definition without full replacement, preserving existing tooling behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Preserved single-file particle import behavior while reusing module publication**
- **Found during:** Task 2 (tooling import rewire)
- **Issue:** Reusing full `replaceFromJsonResources(...)` for a single particle JSON file would clear unrelated active particles.
- **Fix:** Added `ParticleResourcePublication.publishFromJsonResource(...)` and used it for single-file manager tooling imports.
- **Files modified:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java`, `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java`
- **Verification:** Targeted JetBrains MCP GREEN gate exited 0 (External task id 312).
- **Committed in:** `2bb047c`

**2. [Rule 1 - Bug] Scoped root boundary scan to pure particle packages**
- **Found during:** Task 2 verification
- **Issue:** The root boundary test still scanned the documented particle `client/**` integration layer, which Phase 11 explicitly allows to import Minecraft/Forge client APIs.
- **Fix:** Updated the scan to exclude `eyelibparticle/client/**` while continuing to reject root/MC/Forge imports in pure particle module packages.
- **Files modified:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`
- **Verification:** Targeted JetBrains MCP GREEN gate exited 0 (External task id 312).
- **Committed in:** `2bb047c`

---

**Total deviations:** 2 auto-fixed (2 bug fixes)
**Impact on plan:** Both fixes preserved required observable behavior and documented module-side boundaries without adding user-facing scope.

## Issues Encountered
- Initial GREEN verification failed because legacy boundary assertions still rejected the allowed particle client integration layer and the compatibility spawn adapter still contained a local root `BrParticle` codec round-trip. The implementation was adjusted so packet spawn uses module definitions directly and the boundary scan reflects current Phase 11/12 ownership.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for `12-03-PLAN.md`: root loading/publication ownership is now traceable into module publication services, and the remaining Phase 12 work can update ownership documentation plus run final targeted verification.

## Self-Check: PASSED

- Created summary exists: `.planning/phases/12-loading-publication-rewire/12-02-SUMMARY.md`.
- Created test exists: `src/test/java/io/github/tt432/eyelib/client/loader/BrParticleLoaderPublicationTest.java`.
- Task commits exist: `56adc82` and `2bb047c`.
- Targeted JetBrains MCP verification passed in External task id 312.

---
*Phase: 12-loading-publication-rewire*
*Completed: 2026-05-09*
