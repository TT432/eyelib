---
phase: 11-runtime-client-core-extraction
plan: 01
subsystem: particle-runtime
tags: [java, gradle, forge, particle, runtime-boundary, tdd]

# Dependency graph
requires:
  - phase: 10-schema-runtime-ownership-adapter
    provides: Canonical ParticleDefinition and ParticleDefinitionAdapter schema seam
provides:
  - Pure particle runtime definition/context/service contracts
  - Module-owned particle timer, blackboard, and math support helpers
  - Runtime boundary tests rejecting root, Minecraft, Forge, and duplicate BrParticle ownership drift
affects: [11-runtime-client-core-extraction, 12-loading-publication-rewire, 13-command-network-integration-rewire]

# Tech tracking
tech-stack:
  added: []
  patterns: [tdd-red-green, source-boundary-scan, platform-free-runtime-ports]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeDefinition.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeContext.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeServices.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleBlackboard.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleTimer.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleMath.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeSupportTest.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeBoundaryTest.java
  modified:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java

key-decisions:
  - "ParticleRuntimeDefinition wraps the Phase 10 canonical ParticleDefinition rather than introducing any BrParticle duplicate."
  - "ParticleTimer consumes a module-owned TimeSource port so runtime timing stays platform-free until client integration binds Minecraft time."
  - "Runtime package docs explicitly reserve Minecraft/Forge bindings for a documented client integration package outside runtime/**."

patterns-established:
  - "Pure runtime ports: platform objects stay behind ParticleRuntimeServices interfaces."
  - "Boundary tests strip comments/string literals before rejecting forbidden references."

requirements-completed: [PRENDER-01, PRENDER-02]

# Metrics
duration: 9min
completed: 2026-05-09
---

# Phase 11 Plan 01: Runtime Foundation Summary

**Particle runtime contracts with platform-free service ports, support helpers, and boundary guards for Phase 11 extraction.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-05-09T08:08:50Z
- **Completed:** 2026-05-09T08:17:40Z
- **Tasks:** 2 completed
- **Files modified:** 9

## Accomplishments

- Added pure runtime contracts that wrap `ParticleDefinition`, carry `MolangScope` context, and define narrow runtime service ports.
- Added module-owned `ParticleTimer`, `ParticleBlackboard`, and `ParticleMath` helpers so later moved runtime code does not import root timer/blackboard/math utilities.
- Added boundary tests that reject root/MC/Forge contamination under `runtime/**` and reject any particle-module `BrParticle` duplicate.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define pure runtime extraction contracts and support helpers**
   - `5f27a6e` test(11-01): add failing particle runtime support tests
   - `f3bac7b` feat(11-01): add particle runtime support contracts
2. **Task 2: Strengthen runtime boundary and duplicate-owner tests**
   - `032aba6` test(11-01): add failing runtime boundary tests
   - `cf07ef4` feat(11-01): document pure runtime boundary

**Plan metadata:** committed in final docs commit

## Files Created/Modified

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeDefinition.java` - Typed runtime view over the canonical Phase 10 particle definition.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeContext.java` - Runtime context carrying parent Molang scope, definition, and service ports.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeServices.java` - Platform-free time, spawn, and environment service contracts.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleBlackboard.java` - Typed key/value runtime storage.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleTimer.java` - Platform-free fixed-step timer equivalent for later emitter/particle runtime movement.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleMath.java` - Particle-module-owned math helpers replacing root math imports.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` - Documents pure runtime ownership and client integration exception path.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeSupportTest.java` - Unit coverage for definition wrapper, timer, blackboard, context, and math helper behavior.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeBoundaryTest.java` - Static boundary coverage for D-03/D-05/D-06/D-07 invariants.

## Decisions Made

- Kept `ParticleRuntimeDefinition` as a wrapper over `ParticleDefinition` to preserve D-03 canonical ownership and avoid a second module `BrParticle` type.
- Modeled timing through `ParticleRuntimeServices.TimeSource` so Minecraft tick/partial-tick access can be bound later in client integration without contaminating pure runtime.
- Documented `client integration` as the explicit exception path for future Minecraft/Forge-facing adapters outside `runtime/**`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- During Task 1 GREEN, the initial support test fixture directly constructed importer `BrParticle.Events`, which exposed importer codec initialization ordering. The test was corrected to build the fixture through `BrParticle.CODEC` and `ParticleDefinitionAdapter`, matching the Phase 10 seam under test.

## Known Stubs

None.

## Authentication Gates

None.

## Verification

- PASS — JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleRuntimeSupportTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleRuntimeBoundaryTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava"]` exited 0.

## TDD Gate Compliance

- PASS — RED commits exist for both TDD tasks (`5f27a6e`, `032aba6`).
- PASS — GREEN commits exist after each RED commit (`f3bac7b`, `cf07ef4`).

## Next Phase Readiness

- Ready for Plan 11-02 to move component execution onto these pure runtime contracts.
- No blockers; pure runtime boundary tests now guard the extraction path.

## Self-Check: PASSED

- Verified all created/modified files exist on disk.
- Verified task commits `5f27a6e`, `f3bac7b`, `032aba6`, and `cf07ef4` exist in git history.

---
*Phase: 11-runtime-client-core-extraction*
*Completed: 2026-05-09*
