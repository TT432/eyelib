---
phase: 11-runtime-client-core-extraction
plan: 04
subsystem: particle-runtime
tags: [java, gradle, particle, runtime-boundary, tdd]

# Dependency graph
requires:
  - phase: 11-runtime-client-core-extraction
    provides: Pure runtime contracts plus emitter/particle component execution from Plans 01-03
provides:
  - Module-owned Bedrock particle runtime factory, environment, and spawner ports
  - Module-owned emitter lifecycle with Molang state, curves, randoms, emit count, and spawn delegation
  - Module-owned particle instance lifecycle with Molang state, component dispatch, age/lifetime, and idempotent removal callbacks
affects: [11-runtime-client-core-extraction, 12-loading-publication-rewire, 13-command-network-integration-rewire]

# Tech tracking
tech-stack:
  added: []
  patterns: [tdd-red-green, platform-free-lifecycle-ports, module-owned-runtime-lifecycle]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleRuntime.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleEmitter.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleInstance.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeEnvironment.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeSpawner.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeLifecycleTest.java
  modified:
    - MODULES.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
    - src/main/java/io/github/tt432/eyelib/client/particle/README.md

key-decisions:
  - "BedrockParticleRuntime creates module-owned emitters from canonical ParticleDefinition while keeping environment and spawn side effects behind pure ports."
  - "BedrockParticleEmitter delegates particle creation through ParticleRuntimeSpawner instead of root BrParticleRenderManager."
  - "BedrockParticleInstance implements the particle component access port directly so component dispatch remains root/MC/Forge-clean."

patterns-established:
  - "Lifecycle port pattern: runtime state depends on ParticleRuntimeEnvironment and ParticleRuntimeSpawner instead of Minecraft/client singletons."
  - "Runtime host context pattern: module lifecycle classes register themselves in MolangScope host context and expose Bedrock variable names from the moved root behavior."

requirements-completed: [PRENDER-01, PRENDER-02]

# Metrics
duration: 52min
completed: 2026-05-09
---

# Phase 11 Plan 04: Runtime Lifecycle Summary

**Bedrock emitter and particle instance lifecycle moved into `:eyelib-particle` with pure environment/spawner ports and lifecycle parity tests.**

## Performance

- **Duration:** 52 min
- **Started:** 2026-05-09T08:22:00Z
- **Completed:** 2026-05-09T09:14:09Z
- **Tasks:** 2 completed
- **Files modified:** 9

## Accomplishments

- Added `BedrockParticleRuntime`, `ParticleRuntimeEnvironment`, and `ParticleRuntimeSpawner` as the pure lifecycle factory and side-effect ports for module-owned runtime execution.
- Ported emitter Molang scope registration, curves, `variable.emitter_*` values, randoms, loop/tick/render-frame dispatch, emit count tracking, and spawn delegation into `BedrockParticleEmitter`.
- Ported particle instance Molang scope registration, `variable.particle_*` values, randoms, lifetime/age behavior, particle component frame dispatch, and idempotent remove callback semantics into `BedrockParticleInstance`.
- Updated module and particle package docs to record that Phase 11 lifecycle ownership now lives in `:eyelib-particle`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement module-owned emitter lifecycle**
   - `0c69127` test(11-04): add failing particle lifecycle tests
   - `5aed615` feat(11-04): implement bedrock emitter lifecycle
2. **Task 2: Implement module-owned particle instance lifecycle**
   - `52155f7` test(11-04): add failing particle instance lifecycle tests
   - `b9282cd` feat(11-04): implement bedrock particle instance lifecycle

**Documentation fix:** `fc3262e` docs(11-04): document particle lifecycle ownership
**Plan metadata:** committed in final docs commit

## Files Created/Modified

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleRuntime.java` - Factory for creating module-owned Bedrock emitters from canonical particle definitions.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleEmitter.java` - Emitter lifecycle state, Molang variables, component dispatch, and spawn delegation.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleInstance.java` - Particle instance state, Molang variables, particle component dispatch, and idempotent removal.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeEnvironment.java` - Platform-free time/environment lookup port.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeSpawner.java` - Platform-free spawned-particle handoff port.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeLifecycleTest.java` - TDD lifecycle coverage for emitter and particle behavior.
- `MODULES.md`, `eyelib-particle/.../README.md`, `src/main/java/.../client/particle/README.md` - Ownership documentation for moved lifecycle behavior.

## Decisions Made

- Kept lifecycle construction behind `BedrockParticleRuntime` instead of adding global managers or root reach-through from `:eyelib-particle`.
- Used `ParticleRuntimeSpawner` for emitted instances so Plan 05 can bind render-manager/client integration without reintroducing root dependencies into pure runtime.
- Used `ParticleRuntimeEnvironment` for time, entity bounds, and block lookup seams so lifecycle tests run without Forge event subscribers or Minecraft classes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Updated lifecycle ownership documentation**
- **Found during:** Plan-level verification after Task 2
- **Issue:** Moving lifecycle ownership changed module responsibilities, and existing documentation invariant tests required the Phase 11 deferral phrase to remain present in module docs.
- **Fix:** Updated `MODULES.md` plus particle package READMEs to document module-owned lifecycle behavior and preserve phase deferral wording.
- **Files modified:** `MODULES.md`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`
- **Verification:** JetBrains MCP `:eyelib-particle:test` and `:eyelib-particle:compileJava` passed.
- **Committed in:** `fc3262e`

---

**Total deviations:** 1 auto-fixed (1 missing critical documentation update)
**Impact on plan:** No behavior scope expansion; documentation now matches the moved lifecycle ownership boundary.

## Issues Encountered

- Plan-level `:eyelib-particle:test` initially failed `ParticleDefinitionDocumentationTest` because the docs update needed exact phase deferral wording. The documentation was corrected and the full particle test/compile gate passed.

## Known Stubs

None.

## Threat Flags

None - the new lifecycle surface uses platform-free environment/spawner ports and introduces no network, auth, file, or schema trust boundary.

## Authentication Gates

None.

## Verification

- PASS — RED Task 1 JetBrains MCP targeted lifecycle test failed on missing runtime lifecycle classes before implementation.
- PASS — Task 1 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeLifecycleTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — RED Task 2 JetBrains MCP targeted lifecycle test failed on missing particle instance lifecycle methods before implementation.
- PASS — Task 2 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeLifecycleTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — Plan-level JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava"]` exited 0 after documentation fix.
- PASS — Acceptance checks confirmed `BedrockParticleEmitter.java` contains `variable.emitter_age`, `variable.emitter_lifetime`, and `variable.emitter_random_4` without `BrParticleRenderManager`.
- PASS — Acceptance checks confirmed `BedrockParticleInstance.java` contains `variable.particle_age`, `variable.particle_lifetime`, and `variable.particle_random_4`, and `remove()` guards against double decrement.

## TDD Gate Compliance

- PASS — RED commits exist for both TDD tasks (`0c69127`, `52155f7`).
- PASS — GREEN commits exist after each RED commit (`5aed615`, `b9282cd`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for Plan 11-05 to bind render-manager/client integration to the module-owned runtime lifecycle ports.
- No blockers; lifecycle classes compile/test inside `:eyelib-particle` without root runtime or Forge hook loading.

## Self-Check: PASSED

- Verified key created/modified files exist on disk.
- Verified task commits `0c69127`, `5aed615`, `52155f7`, `b9282cd`, and documentation fix `fc3262e` exist in git history.

---
*Phase: 11-runtime-client-core-extraction*
*Completed: 2026-05-09*
