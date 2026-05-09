---
phase: 11-runtime-client-core-extraction
plan: 02
subsystem: particle-runtime
tags: [java, gradle, particle, runtime-boundary, tdd]

# Dependency graph
requires:
  - phase: 11-runtime-client-core-extraction
    provides: Pure particle runtime contracts, support helpers, and boundary guards from Plan 01
provides:
  - Module-owned emitter component dispatch from ParticleDefinition rawComponents
  - Emitter rate, lifetime, local-space, and shape component behavior under :eyelib-particle
  - Component parity tests for raw dispatch, rate/lifetime lifecycle, and shape position evaluation
affects: [11-runtime-client-core-extraction, 12-loading-publication-rewire, 13-command-network-integration-rewire]

# Tech tracking
tech-stack:
  added: []
  patterns: [tdd-red-green, raw-component-dispatch, platform-free-runtime-access-port]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ComponentTarget.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponent.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentManager.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/EmitterParticleComponent.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/EmitterInitialization.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/EmitterLocalSpace.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/rate/EmitterRateInstant.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/rate/EmitterRateManual.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/rate/EmitterRateSteady.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/lifetime/EmitterLifetimeExpression.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/lifetime/EmitterLifetimeLooping.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/lifetime/EmitterLifetimeOnce.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/lifetime/EmitterLifetimeEvents.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/Direction.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/EmitterDisc.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/EmitterShapeBox.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/EmitterShapeCustom.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/EmitterShapeEntityAABB.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/EmitterShapePoint.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/EmitterShapeSphere.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/EmitterComponentRuntimeTest.java
  modified:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentManager.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/EmitterParticleComponent.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/EmitterComponentRuntimeTest.java

key-decisions:
  - "ParticleComponentManager decodes executable components from ParticleDefinition.rawComponents() instead of introducing a particle-module BrParticle schema owner."
  - "Emitter components operate on a module-owned EmitterAccess port so pure runtime code does not import root particle runtime, Minecraft, or Forge types."
  - "Entity-AABB shape data is routed through an optional bounds port, leaving Minecraft entity adaptation for later client integration."

patterns-established:
  - "Raw component dispatch: normalize Bedrock component string keys and decode importer BedrockResourceValue payloads through component codecs."
  - "Runtime access port: component behavior depends on MolangScope, ParticleBlackboard, random source, and optional environment bounds via EmitterAccess."

requirements-completed: [PRENDER-01, PRENDER-02]

# Metrics
duration: 17min
completed: 2026-05-09
---

# Phase 11 Plan 02: Emitter Component Runtime Summary

**Emitter component dispatch, rate/lifetime lifecycle behavior, and local-space/shape position evaluation moved into the pure particle module.**

## Performance

- **Duration:** 17 min
- **Started:** 2026-05-09T08:13:30Z
- **Completed:** 2026-05-09T08:30:35Z
- **Tasks:** 2 completed
- **Files modified:** 21

## Accomplishments

- Added module-owned emitter component interfaces and `ParticleComponentManager` dispatch that reads canonical `ParticleDefinition.rawComponents()`.
- Ported emitter rate and lifetime behavior with parity coverage for instant/manual/steady gating and once/looping/expression lifecycle effects.
- Ported local-space and shape components with platform-free direction, random position evaluation, and entity-AABB bounds through a runtime access port.

## Task Commits

Each task was committed atomically:

1. **Task 1: Port emitter component interfaces, registry, rate, and lifetime behavior**
   - `b7d0565` test(11-02): add failing emitter component runtime tests
   - `253ab07` feat(11-02): port emitter rate and lifetime components
2. **Task 2: Port local-space and shape behavior**
   - `3d0dcc1` test(11-02): add failing shape component runtime tests
   - `bb2e9f2` feat(11-02): port emitter local space and shapes

**Plan metadata:** committed in final docs commit

## Files Created/Modified

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentManager.java` - Static component registry and raw component decoder for module-owned particle components.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/EmitterParticleComponent.java` - Emitter component contract plus pure-runtime `EmitterAccess` port.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/rate/*` - Instant, manual, and steady emitter rate behavior.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/lifetime/*` - Once, looping, expression, and event lifetime component definitions.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/EmitterLocalSpace.java` - Local-space flags with `EMPTY` default.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/emitter/shape/*` - Direction plus point, box, sphere, disc, custom, and entity-AABB shape behavior.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/EmitterComponentRuntimeTest.java` - TDD parity tests for dispatch, rate/lifetime, local-space, and shape behavior.

## Decisions Made

- Kept component decoding keyed by strings from `ParticleDefinition.rawComponents()` to maintain the Phase 10 canonical schema seam.
- Used `EmitterParticleComponent.EmitterAccess` as the temporary pure-runtime component context until later lifecycle plans wire full emitter/particle runtime classes.
- Modeled entity bounds as optional platform-free vectors so Minecraft entity dimensions can be adapted later without contaminating `runtime/**`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected initial parity-test expectations for existing root steady/looping behavior**
- **Found during:** Task 1 GREEN
- **Issue:** The first GREEN run showed the new test expected `EmitterRateSteady` not to emit on the second tick when its timestamp was still zero, and expected looping lifetime to start at exact age zero. The existing root behavior emits again while timestamp remains zero and starts looping only after age exceeds the current loop time.
- **Fix:** Adjusted the assertions to match the current root runtime semantics before committing the GREEN implementation.
- **Files modified:** `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/EmitterComponentRuntimeTest.java`
- **Verification:** JetBrains MCP targeted `EmitterComponentRuntimeTest` plus `:eyelib-particle:compileJava` passed.
- **Committed in:** `253ab07`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** No scope expansion; the fix made tests more faithful to the behavior being preserved.

## Issues Encountered

- Task 1 GREEN initially failed two assertions due to the test parity mismatch documented above; implementation was then verified green.

## Known Stubs

None.

## Threat Flags

None - the planned importer raw components → runtime component registry boundary was implemented with canonical `ParticleDefinition.rawComponents()` and no new network/auth/file trust surface was introduced.

## Authentication Gates

None.

## Verification

- PASS — JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.EmitterComponentRuntimeTest", ":eyelib-particle:compileJava"]` exited 0 after Task 1.
- PASS — JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.EmitterComponentRuntimeTest", ":eyelib-particle:compileJava"]` exited 0 after Task 2.
- PASS — JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava"]` exited 0 for plan-level verification.
- PASS — Acceptance checks confirmed `ParticleComponentManager.java` contains `rawComponents`, emitter component files contain no root `io.github.tt432.eyelib.client.particle` imports, `EmitterLocalSpace.java` contains `EMPTY`, and tests assert rate/lifetime plus point/box shape behavior.

## TDD Gate Compliance

- PASS — RED commits exist for both TDD tasks (`b7d0565`, `3d0dcc1`).
- PASS — GREEN commits exist after each RED commit (`253ab07`, `bb2e9f2`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for Plan 11-03 to port particle component behavior onto the same component/runtime access patterns.
- No blockers; pure runtime component sources remain root/MC/Forge-clean.

## Self-Check: PASSED

- Verified key created/modified files exist on disk.
- Verified task commits `b7d0565`, `253ab07`, `3d0dcc1`, and `bb2e9f2` exist in git history.

---
*Phase: 11-runtime-client-core-extraction*
*Completed: 2026-05-09*
