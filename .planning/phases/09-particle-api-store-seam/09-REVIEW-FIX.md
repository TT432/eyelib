---
phase: 09-particle-api-store-seam
fixed_at: 2026-05-09T05:42:03Z
review_path: .planning/phases/09-particle-api-store-seam/09-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 09: Code Review Fix Report

**Fixed at:** 2026-05-09T05:42:03Z  
**Source review:** `.planning/phases/09-particle-api-store-seam/09-REVIEW.md`  
**Iteration:** 1

**Summary:**
- Findings in scope: 3
- Fixed: 3
- Skipped: 0

## Fixed Issues

### CR-01: Root particle store discards the order that Phase 9 promises to preserve

**Files modified:** `src/main/java/io/github/tt432/eyelib/client/manager/ManagerStorage.java`, `src/test/java/io/github/tt432/eyelib/client/manager/ParticleManagerStoreAdapterTest.java`  
**Commit:** `481f098`  
**Applied fix:** Changed manager backing storage and snapshots to `LinkedHashMap` and added a particle store regression asserting `replaceAll`, `all().keySet()`, and `names()` preserve insertion order.

### WR-01: Boundary tests do not actually guard all `:eyelib-particle` main sources against Minecraft/Forge imports

**Files modified:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`, `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java`  
**Commit:** `26a4d55`  
**Applied fix:** Expanded boundary scans to cover all Java sources under `eyelib-particle/src/main/java` with root, Minecraft, and Forge forbidden import fragments, reporting exact violating files.

### WR-02: Root package still exposes and documents an obsolete duplicate `ParticleSpawnRequest`

**Files modified:** `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java`, `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequestTest.java`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`, `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `docs/architecture/migration/client-particle.md`, `docs/architecture/migration/main.md`  
**Commit:** `36863b3`  
**Applied fix:** Removed the obsolete root request record and stale root test, redirected docs to the module-owned `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`, and added a boundary assertion that the root duplicate is not reintroduced.

## Skipped Issues

None.

## Verification

- JetBrains MCP Gradle `:eyelib-particle:test :eyelib-particle:compileJava :compileJava` — PASS (`exitCode=0`).
- JetBrains MCP Gradle `:test --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleSpawnServiceBoundaryTest --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryPublisherAdapterTest` — PASS (`exitCode=0`).
- Earlier combined Gradle invocation failed because root `--tests` filters were accidentally applied to subproject test tasks with no matching tests; it was rerun with module and root tests separated, and the relevant checks passed.

---

_Fixed: 2026-05-09T05:42:03Z_  
_Fixer: the agent (gsd-code-fixer)_  
_Iteration: 1_
