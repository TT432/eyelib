---
phase: 11-runtime-client-core-extraction
fixed_at: 2026-05-09T18:50:00Z
review_path: .planning/phases/11-runtime-client-core-extraction/11-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 11: Code Review Fix Report

**Fixed at:** 2026-05-09T18:50:00Z  
**Source review:** `.planning/phases/11-runtime-client-core-extraction/11-REVIEW.md`  
**Iteration:** 1

**Summary:**
- Findings in scope: 3
- Fixed: 3
- Skipped: 0

## Fixed Issues

### CR-01: BLOCKER — Optional `max_lifetime` removes particles immediately

**Files modified:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleLifetimeExpression.java`, `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentRuntimeTest.java`  
**Commit:** `e5b7ea5`  
**Status:** fixed: requires human verification  
**Applied fix:** Particle lifetime timeout now only expires when `lifetime > 0`; behavioral coverage verifies omitted/non-positive max lifetime does not remove the particle immediately.

### CR-02: BLOCKER — `particle_kill_plane` decodes but never expires particles

**Files modified:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/lifetime/ParticleLifetimeKillPlane.java`, `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentRuntimeTest.java`  
**Commit:** `35b225b`  
**Status:** fixed: requires human verification  
**Applied fix:** Kill-plane particles now evaluate signed plane distance every frame and remove particles on the negative side; tests cover both retained and removed sides.

### WR-01: WARNING — Legacy root particle bridge silently drops emitted particles

**Files modified:** `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java`, `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java`  
**Commit:** `ae62399`  
**Status:** fixed  
**Applied fix:** The legacy `BrParticleParticle` overload now fails loudly with a migration message instead of silently dropping particles; boundary coverage asserts the failure path remains explicit.

## Verification

- PASS — JetBrains MCP Gradle `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentRuntimeTest`, `:test --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest`, `:eyelib-particle:compileJava`, and `:compileJava` exited 0.

---

_Fixed: 2026-05-09T18:50:00Z_  
_Fixer: the agent (gsd-code-fixer)_  
_Iteration: 1_
