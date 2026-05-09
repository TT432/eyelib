---
phase: 11-runtime-client-core-extraction
plan: 03
subsystem: particle-runtime
tags: [java, gradle, particle, runtime-boundary, tdd]

# Dependency graph
requires:
  - phase: 11-runtime-client-core-extraction
    provides: Pure runtime contracts plus emitter component execution from Plans 01-02
provides:
  - Module-owned particle component execution interface and access port
  - Particle appearance, tinting, lighting, initial speed/spin, lifetime, block-expiration, and motion components under :eyelib-particle
  - Particle component behavior tests for billboard UV/size, tinting colors, lifetime removal, block expiration, and motion updates
affects: [11-runtime-client-core-extraction, 12-loading-publication-rewire, 13-command-network-integration-rewire]

# Tech tracking
tech-stack:
  added: []
  patterns: [tdd-red-green, platform-free-particle-access-port, raw-component-dispatch]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/ParticleParticleComponent.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/appearance/ParticleAppearanceBillboard.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/appearance/ParticleAppearanceLighting.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/appearance/ParticleAppearanceTinting.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/initial/ParticleInitialSpeed.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/initial/ParticleInitialSpin.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleLifetimeExpression.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleLifetimeEvents.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleLifetimeKillPlane.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleExpireIfInBlocks.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleExpireIfNotInBlocks.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/motion/ParticleMotionCollision.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/motion/ParticleMotionDynamic.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/motion/ParticleMotionParametric.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentRuntimeTest.java
  modified:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentManager.java
    - MODULES.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
    - src/main/java/io/github/tt432/eyelib/client/particle/README.md

key-decisions:
  - "ParticleParticleComponent uses a module-owned ParticleAccess port so executable particle components stay root/MC/Forge-clean."
  - "ParticleComponentManager now registers particle-side component codecs alongside emitter components and exposes particleComponents(ParticleDefinition)."
  - "Block-presence checks consume an optional string block-id port instead of Minecraft Level/BuiltInRegistries."

patterns-established:
  - "Particle access port: component behavior depends on MolangScope, ParticleBlackboard, mutable particle state, and optional environment strings only."
  - "Pure transform access: billboard rotation math exposes Quaternionf output without PoseStack, Camera, or Minecraft imports."

requirements-completed: [PRENDER-01, PRENDER-02]

# Metrics
duration: 36min
completed: 2026-05-09
---

# Phase 11 Plan 03: Particle Component Runtime Summary

**Particle appearance, lifetime, initial-state, and motion component execution moved into the pure particle module with behavior parity tests.**

## Performance

- **Duration:** 36 min
- **Started:** 2026-05-09T08:16:00Z
- **Completed:** 2026-05-09T08:52:09Z
- **Tasks:** 2 completed
- **Files modified:** 19

## Accomplishments

- Added `ParticleParticleComponent` with a pure `ParticleAccess` port for module-owned particle-side component execution.
- Ported billboard, lighting, tinting, initial speed/spin, lifetime, block-expiration, and motion component classes into `:eyelib-particle` without root particle imports.
- Expanded targeted tests to cover billboard UV/size, tinting color interpolation, lifetime removal, block expiration, and dynamic/parametric motion updates.

## Task Commits

Each task was committed atomically:

1. **Task 1: Port appearance and initial particle components**
   - `7e4d876` test(11-03): add failing particle appearance component tests
   - `70686c2` feat(11-03): port particle appearance components
2. **Task 2: Port particle lifetime and motion components**
   - `bacaa3a` test(11-03): add failing particle lifetime motion tests
   - `ea5840e` feat(11-03): port particle lifetime and motion components

**Plan metadata:** committed in final docs commit

## Files Created/Modified

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/ParticleParticleComponent.java` - Particle component interface plus pure state/environment access port.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/appearance/*` - Billboard, lighting, and tinting behavior moved into the module.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/initial/*` - Initial speed and spin behavior moved into the module.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/*` - Lifetime expression, event data, kill-plane data, and block-expiration components moved into the module.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/motion/*` - Collision data, dynamic motion, and parametric motion components moved into the module.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentManager.java` - Registers particle-side codecs and exposes `particleComponents` dispatch from raw components.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentRuntimeTest.java` - TDD parity coverage for particle component behavior.
- `MODULES.md`, `eyelib-particle/.../README.md`, `src/main/java/.../client/particle/README.md` - Updated module ownership documentation for moved component execution.

## Decisions Made

- Kept particle component behavior behind `ParticleParticleComponent.ParticleAccess` rather than importing root `BrParticleParticle`, Minecraft `Level`, or Forge types.
- Preserved billboard UV/size and tinting ARGB semantics while exposing pure quaternion rotation math for later render adapters.
- Routed block expiration through `ParticleAccess.blockAtPosition()` string ids so client integration can bind Minecraft block lookup later.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Documented particle module responsibility update**
- **Found during:** Task 2 completion review
- **Issue:** Moving particle component execution changed module responsibilities, and AGENTS.md requires affected module docs to be updated in the same change.
- **Fix:** Updated `MODULES.md` and particle package READMEs to record module-owned component execution and remaining transitional root runtime ownership.
- **Files modified:** `MODULES.md`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`
- **Verification:** Docs paths resolve and final metadata commit includes the updates.
- **Committed in:** final docs commit

---

**Total deviations:** 1 auto-fixed (1 missing critical documentation update)
**Impact on plan:** No implementation scope expansion; documentation now matches the changed ownership boundary.

## Issues Encountered

- None beyond expected RED failures for missing particle component packages/classes during TDD.

## Known Stubs

- `ParticleLifetimeEvents` remains data-only, matching the current root component behavior until later lifecycle/event wiring consumes event payloads.
- `ParticleMotionCollision` remains data-only, matching the current root component behavior; collision simulation requires later environment/client integration beyond this plan's pure component port.

## Threat Flags

None - no new network, auth, file, or schema trust boundary was introduced; block checks are exposed through a string-id runtime access port.

## Authentication Gates

None.

## Verification

- PASS — RED Task 1 JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentRuntimeTest` failed on missing target particle component packages/classes before implementation.
- PASS — Task 1 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentRuntimeTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — RED Task 2 JetBrains MCP targeted test failed on missing lifetime/motion component packages/classes before implementation.
- PASS — Task 2 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentRuntimeTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — Plan-level JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava"]` exited 0.
- PASS — Acceptance checks confirmed particle component files exist and `eyelib-particle/.../runtime/bedrock/component/particle/**/*.java` contains no `io.github.tt432.eyelib.client.particle` imports.

## TDD Gate Compliance

- PASS — RED commits exist for both TDD tasks (`7e4d876`, `bacaa3a`).
- PASS — GREEN commits exist after each RED commit (`70686c2`, `ea5840e`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for Plan 11-04 to assemble module-owned particle lifecycle classes using these particle component ports.
- No blockers; pure component sources remain root/MC/Forge-clean, with render/client integration still deferred to the explicit later plans.

## Self-Check: PASSED

- Verified key created/modified files exist on disk.
- Verified task commits `7e4d876`, `70686c2`, `bacaa3a`, and `ea5840e` exist in git history.

---
*Phase: 11-runtime-client-core-extraction*
*Completed: 2026-05-09*
