---
phase: 09-particle-api-store-seam
plan: 03
subsystem: particle-api-validation
tags: [java17, gradle, forge, particle, api, boundary, tests]

requires:
  - phase: 09-particle-api-store-seam
    provides: String-keyed particle API contracts and transitional root facade delegation from Plans 01-02
provides:
  - Focused particle-module publisher and spawn request contract tests
  - Root registry regression coverage for description-identifier publication keys
  - Static delegation, transitional documentation, and forbidden-import boundary checks
  - Updated Phase 9 validation map with concrete green checks
affects: [phase-09-particle-api-store-seam, phase-10-schema-runtime-ownership, phase-11-runtime-client-core-extraction, phase-12-loading-publication-rewire, phase-13-command-network-integration-rewire]

tech-stack:
  added: []
  patterns: [JUnit boundary/static checks, source-file delegation assertions, identifier-key regression fixtures]

key-files:
  created:
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticlePublisherTest.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticleSpawnRequestTest.java
    - src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryTest.java
    - src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java
  modified:
    - .planning/phases/09-particle-api-store-seam/09-VALIDATION.md

key-decisions:
  - "Kept Plan 03 validation-only: no particle runtime, command, network, or render behavior was moved."
  - "Used source-file JUnit checks to make delegation docs and forbidden imports part of automated verification."
  - "Recorded only checks actually run through JetBrains MCP as green in 09-VALIDATION.md."

patterns-established:
  - "Particle module API behavior gets module-local tests that avoid root runtime imports."
  - "Root transitional facades are guarded by static tests requiring API delegation and removal-condition wording."
  - "Registry publication regressions use mismatched source-key versus description-identifier fixtures."

requirements-completed: [PAPI-01, PAPI-03]

duration: 20 min
completed: 2026-05-09
---

# Phase 09 Plan 03: Particle API & Store Seam Validation Summary

**Automated boundary tests now lock particle API/store publication keys, root facade delegation docs, and root-clean particle module imports**

## Performance

- **Duration:** 20 min
- **Started:** 2026-05-09T12:52:00+08:00
- **Completed:** 2026-05-09T13:12:37+08:00
- **Tasks:** 2 completed
- **Files modified:** 4 created, 1 modified

## Accomplishments

- Added module-local `ParticlePublisherTest` and `ParticleSpawnRequestTest` proving extracted identifier publication, stale replacement removal, string IDs, null guards, and defensive `Vector3f` copies without root imports.
- Added root `ParticleAssetRegistryTest` proving source/loader keys are ignored in favor of `particle_effect.description.identifier`.
- Added `ParticleApiDelegationBoundaryTest` to assert root facades delegate to `io.github.tt432.eyelibparticle.api`, transitional removal wording remains documented, and particle API sources stay free of root/Minecraft/Forge imports.
- Updated `09-VALIDATION.md` rows to green only after JetBrains MCP test/compile checks passed.

## Task Commits

Each task was committed atomically:

1. **Task 1: Test particle module publisher and spawn request contracts** - `4c64ff7` (test)
2. **Task 2: Test root facade delegation, identifier-key publication, docs, and boundary invariants** - `fecff8e` (test)

**Plan metadata:** committed after this summary in the final docs commit.

_Note: These were validation TDD tasks for behavior implemented by prior Phase 9 plans, so commits are test/static-check commits only; no production GREEN implementation was required._

## Files Created/Modified

- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticlePublisherTest.java` - In-memory `ParticleStore<String>` tests for extracted identifier publication and replacement overwrite behavior.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticleSpawnRequestTest.java` - Spawn request tests for string IDs, null rejection, and defensive position copies.
- `src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryTest.java` - Root registry regression test with mismatched source key and description identifier.
- `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java` - Static source checks for API delegation, transitional docs, and forbidden particle-module imports.
- `.planning/phases/09-particle-api-store-seam/09-VALIDATION.md` - Updated Phase 9 validation rows with concrete green tests/checks.

## Decisions Made

- Kept this plan as a validation hardening slice only; production particle API/facade behavior from Plans 01-02 was not expanded.
- Chose JUnit static source checks instead of ad-hoc scripts so delegation and no-reverse-dependency checks are portable and run through Gradle/MCP.
- Scoped forbidden import checks to `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api` for this plan’s API seam while existing broader boundary tests continue to cover module main sources.

## TDD Gate Compliance

- The plan tasks were marked `tdd="true"`, but this plan intentionally adds validation for behavior already implemented in Plans 01-02.
- RED failures were therefore not produced for Task 1/Task 2 without weakening existing correct production code; the resulting commits are test-only validation commits.
- GREEN verification was satisfied by JetBrains MCP `:eyelib-particle:test`, targeted root `:test`, `:eyelib-particle:compileJava`, and `:compileJava` passing after the tests were added.

## Verification

- **Task 1:** JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava` exited 0.
- **Task 1 static acceptance:** Content search found no root runtime imports in `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api`.
- **Task 2:** JetBrains MCP root `:test --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest` exited 0.
- **Task 2 compile:** JetBrains MCP `:compileJava :eyelib-particle:compileJava` exited 0.
- **Plan-level verification:** Re-ran JetBrains MCP checks separately after commits: `:eyelib-particle:test :eyelib-particle:compileJava`, targeted root `:test`, and `:compileJava :eyelib-particle:compileJava` all exited 0.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Test Bug] Corrected case-sensitive transitional documentation assertion**
- **Found during:** Task 2 (root facade delegation/static boundary checks)
- **Issue:** The new static test searched for lowercase `transitional` in Javadocs, while source Javadocs use sentence-case `Transitional`; this made the check fail despite documentation being present.
- **Fix:** Updated the test expectations to match the actual source wording while preserving README lowercase checks.
- **Files modified:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`
- **Verification:** Re-ran targeted JetBrains MCP root test task; it exited 0.
- **Committed in:** `fecff8e`

---

**Total deviations:** 1 auto-fixed (1 test bug)
**Impact on plan:** The fix kept the intended static documentation gate and did not change production scope.

## Issues Encountered

- A combined JetBrains MCP Gradle invocation using `--tests` together with compile tasks failed because Gradle applies `--tests` only to test tasks. The plan-level gate was rerun as separate MCP invocations for particle tests, targeted root tests, and compile tasks; all passed.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Threat Flags

None - this plan added tests/static checks and validation documentation only; it introduced no new network endpoint, auth path, runtime file access surface, or schema trust boundary.

## Next Phase Readiness

- Phase 9 API/store seam is now covered by automated module tests, root regression tests, static delegation checks, and validation status updates.
- Phase 10 can proceed with schema/runtime ownership decisions using the guarded root-clean particle API seam and documented transitional facades.

## Self-Check: PASSED

- Created test files exist on disk.
- Task commits `4c64ff7` and `fecff8e` exist in git history.
- JetBrains MCP compile/test verification passed after task commits.
- Existing untracked `.planning/v1.0-MILESTONE-AUDIT.md` was left untouched as unrelated work.

---
*Phase: 09-particle-api-store-seam*
*Completed: 2026-05-09*
