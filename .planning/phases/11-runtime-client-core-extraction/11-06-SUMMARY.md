---
phase: 11-runtime-client-core-extraction
plan: 06
subsystem: particle-client-integration
tags: [java, gradle, forge, particle, runtime-delegation, client-integration, tdd]

# Dependency graph
requires:
  - phase: 11-runtime-client-core-extraction
    provides: Module-owned Bedrock particle lifecycle, render manager, renderer adapter, and client hooks from Plans 04-05
provides:
  - Root particle spawn/remove compatibility paths delegated to module-owned runtime/client services
  - Transitional root BrParticleRenderManager adapter over io.github.tt432.eyelibparticle.client.ParticleRenderManager
  - Packet-shape and root-to-module delegation boundary tests for Phase 11 completion
affects: [12-loading-publication-rewire, 13-command-network-integration-rewire, 14-final-verification]

# Tech tracking
tech-stack:
  added: []
  patterns: [tdd-red-green, transitional-root-adapter, module-runtime-delegation, jetbrains-mcp-verification]

key-files:
  created:
    - src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java
  modified:
    - src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java
    - src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java
    - src/main/java/io/github/tt432/eyelib/client/animation/RuntimeParticlePlayData.java
    - src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntryDefinition.java
    - src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrControllerExecutor.java
    - src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java
    - src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java
    - MODULES.md
    - docs/index/repo-map.md
    - docs/architecture/01-module-boundaries.md
    - docs/architecture/02-side-boundaries.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
    - src/main/java/io/github/tt432/eyelib/client/particle/README.md

key-decisions:
  - "Root ParticleSpawnService now constructs module BedrockParticleRuntime emitters and registers them with module ParticleRenderManager while preserving string-keyed packet entrypoints."
  - "Root BrParticleRenderManager is retained only as a transitional adapter for instrumentation and legacy callers; module ParticleRenderManager owns counts, collections, and lifecycle behavior."
  - "Phase 11 final ownership docs explicitly defer loading/publication to Phase 12, command/network integration to Phase 13, and broad/client verification evidence to Phase 14."

patterns-established:
  - "Root compatibility adapters may translate legacy root BrParticle inputs into module ParticleDefinition at the boundary, then delegate executable behavior to :eyelib-particle."
  - "Static root boundary tests guard both module runtime delegation and unchanged string-keyed packet fields."

requirements-completed: [PRENDER-01, PRENDER-02]

# Metrics
duration: 16min
completed: 2026-05-09
---

# Phase 11 Plan 06: Runtime Client Core Extraction Summary

**Root particle compatibility facades now delegate packet and animation spawns into the module-owned Bedrock runtime, render manager, and client integration layer.**

## Performance

- **Duration:** 16 min
- **Started:** 2026-05-09T09:41:39Z
- **Completed:** 2026-05-09T09:57:58Z
- **Tasks:** 2 completed
- **Files modified:** 14

## Accomplishments

- Rewired `ParticleSpawnService` so packet spawns and animation/controller spawns create `BedrockParticleRuntime` emitters and register them with `io.github.tt432.eyelibparticle.client.ParticleRenderManager`.
- Converted root `BrParticleRenderManager` into a thin compatibility adapter over module render-manager counts/spawn/remove methods, leaving root render-manager event/business logic out of the ownership path.
- Updated animation particle play data and render locator positioning to carry module `BedrockParticleEmitter` instances while preserving root-side locator pose adaptation.
- Added root delegation/static tests that preserve `SpawnParticlePacket`/`RemoveParticlePacket` string-keyed fields and assert root-to-module runtime delegation.
- Updated module inventory, repo map, architecture docs, and particle READMEs to record Phase 11 final ownership and Phase 12/13/14 deferrals.

## Task Commits

Each task was committed atomically:

1. **Task 1: Delegate root spawn/render compatibility paths to module runtime**
   - `f4246e9` test(11-06): add failing particle delegation boundary tests
   - `d72acbb` feat(11-06): delegate root particle runtime to module manager
2. **Task 2: Update ownership docs and final verification gates**
   - `010c56b` docs(11-06): document final particle runtime ownership

**Plan metadata:** committed in final docs commit

## Files Created/Modified

- `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java` - Static guard for root spawn/runtime delegation, root render-manager adapter shape, and unchanged packet fields.
- `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java` - Updated boundary scan to allow documented module client integration while keeping pure runtime/root-import guards.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` - Converts retained root `BrParticle` inputs to module `ParticleDefinition`, creates module `BedrockParticleRuntime` emitters, and delegates spawn/remove to module `ParticleRenderManager`.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java` - Transitional adapter over module `ParticleRenderManager`.
- `src/main/java/io/github/tt432/eyelib/client/animation/RuntimeParticlePlayData.java` - Stores module `BedrockParticleEmitter` for animation-driven particles.
- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntryDefinition.java` - Routes animation particle effects through `ParticleSpawnService.spawnEmitter`.
- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrControllerExecutor.java` - Routes controller particle effects through `ParticleSpawnService.spawnEmitter`.
- `src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java` - Applies locator pose updates through the root compatibility service for module emitters.
- `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and both particle READMEs - Document final Phase 11 ownership and later-phase deferrals.

## Decisions Made

- Kept root packet and animation call sites compatibility-shaped, but moved executable emitter registration and render-manager behavior to module services.
- Retained root `BrParticleRenderManager` as an adapter rather than deleting it because instrumentation and legacy sources still reference the root path.
- Kept loading/publication and command/network rewires deferred per plan; the compatibility adapter translates the current legacy root `BrParticle` at spawn time until Phase 12 moves publication to module definitions.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated animation particle play data for module emitter type**
- **Found during:** Task 1 GREEN verification
- **Issue:** `RuntimeParticlePlayData` still declared the legacy root `BrParticleEmitter` type after animation/controller spawn paths started returning module `BedrockParticleEmitter`, causing `:compileJava` to fail.
- **Fix:** Changed the play-data record to store `BedrockParticleEmitter` and routed locator pose adaptation through `ParticleSpawnService.initPose`.
- **Files modified:** `RuntimeParticlePlayData.java`, `EntityRenderSystem.java`, `BrAnimationEntryDefinition.java`, `BrControllerExecutor.java`
- **Verification:** JetBrains MCP targeted root delegation tests plus `:compileJava` exited 0.
- **Committed in:** `d72acbb`

---

**Total deviations:** 1 auto-fixed (1 blocking issue)
**Impact on plan:** The fix was required for the planned root-to-module delegation to compile and did not widen scope beyond compatibility rewiring.

## Issues Encountered

- RED verification failed as expected on missing module-runtime delegation strings before implementation.
- The first GREEN verification exposed the stale `RuntimeParticlePlayData` emitter type; it was fixed before committing the implementation.

## Known Stubs

None.

## Threat Flags

None - packet shape was preserved by static tests, no new auth/file/network surface was introduced, and retained root classes remain documented transitional adapters.

## Authentication Gates

None.

## Verification

- PASS — RED JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":test --tests io.github.tt432.eyelib.client.particle.ParticleSpawnServiceBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest"]` failed on missing module runtime delegation before implementation.
- PASS — Task 1 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":test --tests io.github.tt432.eyelib.client.particle.ParticleSpawnServiceBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest", ":compileJava"]` exited 0 after implementation.
- PASS — Task 2 / plan-level JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]` exited 0.
- PASS — Acceptance checks: `ParticleSpawnService.java` imports module `ParticleSpawnRequest`; root `BrParticleRenderManager.java` imports module `ParticleRenderManager`; tests assert `SpawnParticlePacket` and `RemoveParticlePacket` fields/codecs remain string-keyed; docs include Phase 12, Phase 13, Phase 14, `executable runtime`, and `Dist.CLIENT`.

## TDD Gate Compliance

- PASS — RED commit exists for the TDD task (`f4246e9`).
- PASS — GREEN commit exists after RED (`d72acbb`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 11 is complete: runtime/client core extraction now has root compatibility adapters delegating to module-owned runtime/client services.
- Ready for Phase 12 loading/publication rewire to stop spawn-time legacy root `BrParticle` conversion and publish module definitions directly.
- Phase 13 should migrate command/network callers off transitional root adapters where practical; Phase 14 should gather broad/client verification evidence.

## Self-Check: PASSED

- Verified key created/modified files exist on disk.
- Verified task commits `f4246e9`, `d72acbb`, and `010c56b` exist in git history.
- Verified final JetBrains MCP compile/test gates passed.

---
*Phase: 11-runtime-client-core-extraction*
*Completed: 2026-05-09*
