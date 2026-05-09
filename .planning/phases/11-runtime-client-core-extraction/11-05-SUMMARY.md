---
phase: 11-runtime-client-core-extraction
plan: 05
subsystem: particle-client-integration
tags: [java, gradle, forge, particle, render-manager, client-integration, tdd]

# Dependency graph
requires:
  - phase: 11-runtime-client-core-extraction
    provides: Module-owned Bedrock emitter and particle lifecycle from Plan 04
provides:
  - Module-owned ParticleRenderManager collections and lifecycle methods
  - Side-safe Dist.CLIENT Forge hook wrapper for render/client tick, render level, and logout cleanup
  - BedrockParticleRenderer adapter for material resolution, texture suffixing, render buffers, tint, billboard, and light output
affects: [11-runtime-client-core-extraction, 12-loading-publication-rewire, 13-command-network-integration-rewire]

# Tech tracking
tech-stack:
  added: [project(':eyelib-material') dependency for particle client renderer]
  patterns: [tdd-red-green, side-safe-forge-hook-wrapper, client-integration-adapter, render-manager-service]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleClientRuntimeServices.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderManager.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderHooks.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/BedrockParticleRenderer.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/package-info.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/client/ParticleRenderManagerLifecycleTest.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/client/ParticleClientIntegrationBoundaryTest.java
  modified:
    - eyelib-particle/build.gradle
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java
    - MODULES.md
    - docs/architecture/01-module-boundaries.md
    - docs/architecture/02-side-boundaries.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
    - src/main/java/io/github/tt432/eyelib/client/particle/README.md

key-decisions:
  - "ParticleRenderManager owns module-side emitter and particle collections while Forge event subscription lives only in ParticleRenderHooks."
  - "Minecraft render types, ResourceLocation texture suffixing, render buffers, camera transforms, tint, billboard, and light output are quarantined in BedrockParticleRenderer under the documented client integration package."
  - "Pure runtime boundary tests now reject Minecraft/Forge imports under runtime/** while allowing the explicit client/** adapter layer required by Phase 11."

patterns-established:
  - "Client runtime service seam: tests use immediate submission while production integration uses Minecraft.getInstance().submit(...)."
  - "Hook wrapper pattern: @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = FORGE) delegates to testable lifecycle methods."

requirements-completed: [PRENDER-01, PRENDER-02]

# Metrics
duration: 20min
completed: 2026-05-09
---

# Phase 11 Plan 05: Particle Client Integration Summary

**Particle render-manager lifecycle, side-safe Forge hooks, and Minecraft render-buffer adaptation moved into the `:eyelib-particle` client integration layer.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-05-09T09:17:00Z
- **Completed:** 2026-05-09T09:36:48Z
- **Tasks:** 2 completed
- **Files modified:** 14

## Accomplishments

- Added `ParticleRenderManager` and `ParticleClientRuntimeServices` so emitter/particle collections, duplicate/no-op operations, render tick cleanup, client tick advancement, render delegation, and logout clear behavior are module-owned and unit-testable without Forge event instances.
- Added `ParticleRenderHooks` as the only Forge subscriber for this plan, with `Dist.CLIENT` + `Bus.FORGE` side gating and START/AFTER_ENTITIES/logout delegation to the render manager.
- Added `BedrockParticleRenderer` to preserve material resolution through `RenderTypeResolver`, `.png` texture suffixing, Minecraft buffer-source access, billboard/tint/lighting output, and camera/pose adaptation at render time.
- Updated boundary/docs to record that Minecraft/Forge client rendering is allowed only in `io.github.tt432.eyelibparticle.client`, while `runtime/**` remains root/MC/Forge-clean.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement render manager lifecycle service**
   - `41dec6b` test(11-05): add failing particle render manager lifecycle tests
   - `7cea137` feat(11-05): implement particle render manager lifecycle
2. **Task 2: Implement side-safe Forge hooks and renderer adapter**
   - `7a8cc8a` test(11-05): add failing particle client integration boundary tests
   - `59ee1a8` feat(11-05): add side-safe particle client integration

**Plan metadata:** committed in final docs commit

## Files Created/Modified

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleClientRuntimeServices.java` - Client-thread submission seam with immediate test execution and Minecraft client submission adapter.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderManager.java` - Module-owned emitter/particle collections and lifecycle methods.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderHooks.java` - Side-safe Forge event subscriber delegating tick/render/logout events to the manager.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/BedrockParticleRenderer.java` - Minecraft render adapter for material, texture, buffer, pose, billboard, tint, and lighting behavior.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/package-info.java` - Documents the explicit client integration exception.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/client/ParticleRenderManagerLifecycleTest.java` - Lifecycle tests for manager collection behavior, tick ordering, and clear behavior.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/client/ParticleClientIntegrationBoundaryTest.java` - Static tests for side-safe hooks, renderer adaptation, and client package documentation.
- `eyelib-particle/build.gradle` - Adds `:eyelib-material` dependency for `RenderTypeResolver` in the particle client renderer.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` - Narrows Minecraft/Forge forbidden-import checks to pure `runtime/**` while retaining root-runtime forbidden checks for all particle module sources.
- `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/.../README.md`, `src/main/java/.../client/particle/README.md` - Record the new client integration ownership and dependency exception.

## Decisions Made

- Kept event subscription out of `ParticleRenderManager`; hooks delegate to lifecycle methods so tests can verify behavior without Forge event loading.
- Kept render backend details in `BedrockParticleRenderer` instead of pure runtime classes; runtime lifecycle state remains platform-clean and rendering adapts at the client boundary.
- Added a direct `:eyelib-material` dependency to `:eyelib-particle` because material/render type resolution is now part of the module-owned client renderer.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected render-manager cleanup test fixture to avoid extra emitter emission**
- **Found during:** Task 1 GREEN
- **Issue:** The initial cleanup-order test registered an active instant emitter in the manager, so `onRenderTickStart()` correctly spawned an additional particle while the test expected only the previously spawned particle.
- **Fix:** Changed the fixture to create the particle through an emitter that was not itself registered for render-frame advancement, isolating the cleanup-order assertion.
- **Files modified:** `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/client/ParticleRenderManagerLifecycleTest.java`
- **Verification:** JetBrains MCP targeted `ParticleRenderManagerLifecycleTest` plus `:eyelib-particle:compileJava` passed.
- **Committed in:** `7cea137`

**2. [Rule 2 - Missing Critical] Documented and enforced the client integration exception**
- **Found during:** Task 2 implementation
- **Issue:** Introducing Minecraft/Forge client rendering inside `:eyelib-particle` changed the module dependency/side boundary and required explicit documentation plus a boundary-test adjustment so pure runtime remains protected without rejecting the planned client adapter.
- **Fix:** Updated module/architecture/package docs, added `:eyelib-material` as the renderer dependency, and split the boundary test into all-source root-runtime checks plus runtime-only Minecraft/Forge checks.
- **Files modified:** `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/build.gradle`, `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java`, particle READMEs
- **Verification:** JetBrains MCP targeted client integration test plus plan-level `:eyelib-particle:test`, `:eyelib-particle:compileJava`, and `:compileJava` passed.
- **Committed in:** `59ee1a8`

---

**Total deviations:** 2 auto-fixed (1 bug, 1 missing critical documentation/boundary update)
**Impact on plan:** No scope creep; fixes preserved the planned side-safe client integration and kept pure runtime protections intact.

## Issues Encountered

- Task 1 GREEN initially failed one assertion because the test fixture allowed an active emitter to spawn a second particle during the same render tick. The fixture was corrected and the targeted gate passed.
- Task 2 GREEN initially failed the package-info documentation assertion due to case-sensitive wording. The package documentation was updated to contain the exact integration phrase and the targeted gate passed.

## Known Stubs

None.

## Threat Flags

None - the planned Forge/Minecraft client boundary is documented under `client/**`, guarded by `Dist.CLIENT`, and pure runtime sources remain covered by runtime-only forbidden-import tests.

## Authentication Gates

None.

## Verification

- PASS — RED Task 1 JetBrains MCP targeted `ParticleRenderManagerLifecycleTest` failed on missing `ParticleRenderManager` before implementation.
- PASS — Task 1 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.client.ParticleRenderManagerLifecycleTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — RED Task 2 JetBrains MCP targeted `ParticleClientIntegrationBoundaryTest` failed on missing client hook/renderer/package-info files before implementation.
- PASS — Task 2 JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.client.ParticleClientIntegrationBoundaryTest", ":eyelib-particle:compileJava"]` exited 0.
- PASS — Plan-level JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]` exited 0.
- PASS — Acceptance checks confirmed `ParticleRenderManager.java` contains `getEmitterCount`, `getParticleCount`, `onRenderTickStart`, `onClientTickStart`, and `clear`.
- PASS — Acceptance checks confirmed `ParticleRenderHooks.java` contains `Dist.CLIENT`, `TickEvent.Phase.START`, `RenderLevelStageEvent.Stage.AFTER_ENTITIES`, and `ClientPlayerNetworkEvent.LoggingOut`.
- PASS — Acceptance checks confirmed `BedrockParticleRenderer.java` contains `RenderTypeResolver.resolve(new ResourceLocation(material))` and `withSuffix(".png")`.

## TDD Gate Compliance

- PASS — RED commits exist for both TDD tasks (`41dec6b`, `7a8cc8a`).
- PASS — GREEN commits exist after each RED commit (`7cea137`, `59ee1a8`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for Plan 11-06 to rewire root compatibility facades around the module-owned client integration.
- No blockers; targeted and plan-level JetBrains MCP Gradle verification passed.

## Self-Check: PASSED

- Verified key client integration files exist on disk.
- Verified task commits `41dec6b`, `7cea137`, `7a8cc8a`, and `59ee1a8` exist in git history.

---
*Phase: 11-runtime-client-core-extraction*
*Completed: 2026-05-09*
