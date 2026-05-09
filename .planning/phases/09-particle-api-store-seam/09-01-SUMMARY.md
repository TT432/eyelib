---
phase: 09-particle-api-store-seam
plan: 01
subsystem: particle-api
tags: [java17, gradle, forge, particle, api, store, tdd]

requires:
  - phase: 08-boundary-contract-gradle-module-skeleton
    provides: :eyelib-particle Gradle module skeleton and one-way root-to-particle dependency boundary
provides:
  - String-keyed particle lookup/store/lifecycle API contracts under :eyelib-particle
  - Identifier-flattening particle publisher seam backed by ParticleStore
  - String-keyed spawn/remove request API with defensive position copies
affects: [phase-09-particle-api-store-seam, phase-10-schema-runtime-ownership, phase-11-runtime-client-core-extraction, phase-12-loading-publication-rewire, phase-13-command-network-integration-rewire]

tech-stack:
  added: []
  patterns: [generic particle store port, identifier publisher seam, defensive-copy request record, TDD red-green commits]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLookupApi.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLifecycle.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleIdentifier.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnRequest.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnApi.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticleStoreContractTest.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticlePublisherAndSpawnApiTest.java
  modified: []

key-decisions:
  - "Kept Phase 9 plan 01 additive inside io.github.tt432.eyelibparticle.api; root runtime/render internals were not moved."
  - "Used generic T parameters so :eyelib-particle APIs stay root-clean while later phases bind root runtime particle definitions through adapters."
  - "Preserved publication order with LinkedHashMap and keyed publication by ParticleIdentifier output rather than loader/source keys."

patterns-established:
  - "Particle API package remains string-first and root/MC/Forge-clean."
  - "ParticleSpawnRequest defensively copies Vector3f on construction and accessor calls."
  - "TDD tests cover store lifecycle, publication flattening, and spawn request invariants before implementation commits."

requirements-completed: [PAPI-01]

duration: 4 min
completed: 2026-05-09
---

# Phase 09 Plan 01: Particle API & Store Seam Summary

**String-keyed particle module API contracts with generic store/publication seams and defensive-copy spawn requests**

## Performance

- **Duration:** 4 min
- **Started:** 2026-05-09T12:47:40+08:00
- **Completed:** 2026-05-09T12:51:49+08:00
- **Tasks:** 2 completed
- **Files modified:** 10 created, 0 modified

## Accomplishments

- Created `io.github.tt432.eyelibparticle.api` with root-clean lookup, store, lifecycle, publisher, identifier, spawn request, and spawn/remove contracts.
- Added TDD coverage proving string-keyed store behavior, identifier-based replacement order, single-particle publication, null guards, and defensive `Vector3f` copies.
- Verified the particle API compiles through JetBrains MCP Gradle and passes forbidden-import/static shape checks without shell Gradle.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Define root-clean lookup/store/lifecycle API contracts** - `40873b3` (test)
2. **Task 1 GREEN: Define root-clean lookup/store/lifecycle API contracts** - `c78b191` (feat)
3. **Task 2 RED: Define publication and spawn/remove API seams** - `d70523d` (test)
4. **Task 2 GREEN: Define publication and spawn/remove API seams** - `95c04a2` (feat)

**Plan metadata:** `be5f7aa` (docs: complete plan)

## Files Created/Modified

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java` - API package Javadoc and `@NullMarked` boundary marker.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLookupApi.java` - String-keyed nullable lookup/read contract.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java` - Mutable store port extending lookup and lifecycle behavior.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLifecycle.java` - Narrow `clear()` lifecycle contract.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleIdentifier.java` - Functional identifier extractor used by the publisher seam.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java` - Identifier-flattening publisher that writes through `ParticleStore` using `LinkedHashMap`.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnRequest.java` - String-keyed spawn request with non-null guards and defensive `Vector3f` copies.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnApi.java` - Spawn/remove request port.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticleStoreContractTest.java` - Store lifecycle and string-key behavior test.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticlePublisherAndSpawnApiTest.java` - Publisher flattening and spawn request test coverage.

## Decisions Made

- Kept this plan additive and module-local: no Bedrock runtime/render internals, loader code, packets, or MC/client hooks moved.
- Used generic API contracts instead of exposing root `BrParticle`, preserving the one-way root -> particle dependency boundary.
- Kept ID validation/adaptation out of `:eyelib-particle`; the API accepts strings and later root/MC adapters remain responsible for platform validation.

## TDD Gate Compliance

- RED gate commits exist: `40873b3`, `d70523d`.
- GREEN gate commits exist after each RED gate: `c78b191`, `95c04a2`.
- No refactor commit was needed after GREEN because the minimal implementation already matched the plan shape and tests.

## Verification

- **RED Task 1:** JetBrains MCP `:eyelib-particle:test` failed as expected because `ParticleStore` did not exist.
- **GREEN Task 1:** JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava` exited 0.
- **Task 1 static check:** PowerShell `Select-String` forbidden-import scan over `eyelibparticle/api/*.java` passed.
- **RED Task 2:** JetBrains MCP `:eyelib-particle:test` failed as expected because `ParticlePublisher` and `ParticleSpawnRequest` did not exist.
- **GREEN Task 2:** JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava` exited 0.
- **Task 2 static check:** PowerShell content check confirmed `ParticlePublisher.java` contains `LinkedHashMap` and `store.replaceAll`, and `ParticleSpawnRequest.java` contains `String particleId` with no `ResourceLocation`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed a forbidden literal package reference from API package Javadoc**
- **Found during:** Task 1 (Define root-clean lookup/store/lifecycle API contracts)
- **Issue:** The package Javadoc initially named the exact root platform implementation package, causing the required static forbidden-import/content check to fail even though it was documentation text.
- **Fix:** Reworded the Javadoc to say "root platform implementation classes" while preserving the one-way dependency rule.
- **Files modified:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java`
- **Verification:** Re-ran the forbidden-import scan; it passed.
- **Committed in:** `c78b191`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Static boundary enforcement was strengthened without changing API scope or behavior.

## Issues Encountered

None beyond expected TDD RED failures and the auto-fixed static boundary wording issue documented above.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Threat Flags

None - this plan introduced pure Java API contracts only; no new network endpoint, auth path, file access pattern, or trust-boundary schema migration was added beyond the planned string-key request seam.

## Next Phase Readiness

- Phase 9 Plan 02 can wire root compatibility facades/adapters to these module-owned API seams.
- Later Phase 10-13 work can bind concrete root/importer/runtime particle definitions without making `:eyelib-particle` depend back on root packages.

## Self-Check: PASSED

- Created API and test files exist on disk.
- Task commits `40873b3`, `c78b191`, `d70523d`, and `95c04a2` exist in git history.
- Verification commands recorded above passed after GREEN implementations.

---
*Phase: 09-particle-api-store-seam*
*Completed: 2026-05-09*
