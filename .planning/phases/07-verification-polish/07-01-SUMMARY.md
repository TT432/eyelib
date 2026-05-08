---
phase: 07-verification-polish
plan: 01
subsystem: testing
tags: [junit-jupiter, reflection, source-assertions, forge, gradle]

# Dependency graph
requires:
  - phase: 06-config-override-bridge-state-machine-fixes
    provides: "System property bridge (isEnabled, shouldExitAfterSmoke), state machine config bridge wiring, build.gradle run configs"
provides:
  - "Static verification tests for CORR-03 system property bridge and build.gradle integrity"
  - "Regression guard against accidental smoke property leakage into runClient config"
  - "Source-level assertion that handleInit() transitions to IDLE when disabled"
affects: [07-02-hardware-smoke-checklist, CORR-03, CORR-04]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Reflection-based method existence tests (Modifier.isPublic .isStatic, getReturnType, getParameterCount)"
    - "Source file content assertions via classpath → settings.gradle marker resolution"
    - "Block extraction from build.gradle with brace-counting parser"
    - "System property lifecycle management via @BeforeEach/@AfterEach"
    - "Package-private test classes following Phase 3/4 convention"

key-files:
  created:
    - "eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeConfigBridgeTest.java"
    - "eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeBuildIntegrityTest.java"
  modified: []

key-decisions:
  - "Used classpath-based settings.gradle marker resolution for root build.gradle to avoid subproject CWD issue"
  - "Used naive brace-counting block extractor (not regex) for build.gradle block verification — simple and sufficient for this file's structure"
  - "System property behavior tests set property before call → never hit ForgeConfigSpec fallback → safe without Forge runtime init"

patterns-established:
  - "System property bridge test pattern: setProperty → call method → assert result → clearProperty in finally"
  - "Source file assertion pattern: resolveSourceFile via classpath walk → Files.readString → assert contains/doesNotContain"
  - "Build integrity test pattern: extract named Gradle block → assert contains required/excluded properties"

requirements-completed: [CORR-03, CORR-04]

# Metrics
duration: 20 min
completed: 2026-05-08
---

# Phase 7 Plan 01: Automated Static Verification Tests Summary

**Two JUnit Jupiter test classes with 21 tests statically verifying CORR-03 system property bridge behavior and build.gradle run config isolation — zero production code changes.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-05-08T15:17:00Z
- **Completed:** 2026-05-08T15:37:39Z
- **Tasks:** 2
- **Files created:** 2
- **Files modified:** 0

## Accomplishments
- 11 system property bridge verification tests covering method existence, behavioral correctness, and source file fallback patterns (CORR-03)
- 10 build integrity tests verifying client run config has zero smoke props, clientSmoke has both required props, and state machine idle path is intact (CORR-03)
- All 21 new tests pass; full `:eyelib-clientsmoke:test` suite: 84 tests, 3 pre-existing failures (out of scope), 81 pass
- Tests follow established Phase 3/4 patterns: package-private class, reflection + source assertions, no Minecraft runtime required

## Task Commits

Each task was committed atomically:

1. **Task 1: ClientSmokeConfigBridgeTest — system property bridge verification** - `99da26c` (test)
2. **Task 2: ClientSmokeBuildIntegrityTest — build.gradle & state machine idle path** - `2ef65b2` (test)

## Files Created

- `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeConfigBridgeTest.java` — 11 tests: method existence (reflection), system property behavior (set/assert), source file assertions (ENABLED.get() and EXIT_AFTER_SMOKE.get() fallback verification)
- `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeBuildIntegrityTest.java` — 10 tests: Part A (build.gradle run config isolation via block extraction), Part B (state machine idle path and config bridge reference assertions)

## Decisions Made
- Used classpath-based `settings.gradle` marker resolution for root `build.gradle` to avoid subproject CWD issue inherent in Gradle test task execution
- Used naive brace-counting block extractor (not regex) for build.gradle block verification — simple and sufficient for this file's well-structured format
- System property behavior tests set property before calling bridge methods, ensuring the code path never hits `ForgeConfigSpec.get()` — safe without Minecraft runtime initialization

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed `extractBlock()` brace-start index off-by-one**
- **Found during:** Task 2 (ClientSmokeBuildIntegrityTest compilation/execution)
- **Issue:** `extractBlock()` method started its iteration at `start + blockName.length() + 1` which placed the cursor AT the opening brace instead of after it. This caused the opening `{` to be counted as `braceCount=1`, causing the extraction to bleed past the intended closing brace.
- **Fix:** Changed to `start + search.length()` which correctly skips the block name, the space, and the opening brace.
- **Files modified:** `ClientSmokeBuildIntegrityTest.java`
- **Verification:** All 10 tests pass after fix; extracted blocks contain correct content
- **Committed in:** `2ef65b2` (Task 2 commit — fix applied before commit)

**2. [Rule 1 - Bug] Fixed `readBuildGradle()` resolving subproject build.gradle instead of root**
- **Found during:** Task 2 (ClientSmokeBuildIntegrityTest execution)
- **Issue:** `readBuildGradle()` used `Paths.get("build.gradle")` as its first resolution attempt. The Gradle `:eyelib-clientsmoke:test` task runs with CWD at the subproject, so the relative path resolved to `eyelib-clientsmoke/build.gradle` which has a `client { }` block but no `clientSmoke { }` block. The root `build.gradle` was never reached because the relative path succeeded.
- **Fix:** Removed the relative path fallback. The method now always uses classpath-based resolution (walk up from test class location via `settings.gradle` marker), which correctly resolves the project root `build.gradle`.
- **Files modified:** `ClientSmokeBuildIntegrityTest.java`
- **Verification:** All Part A build.gradle tests pass; root `build.gradle` with `clientSmoke { }` block is correctly read
- **Committed in:** `2ef65b2` (Task 2 commit — fix applied before commit)

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both bugs were in the test helper code (not production code). No scope creep. Fixes were necessary for test correctness.

## Issues Encountered

### Pre-existing Test Failures (Out of Scope — Not Caused by Plan 07-01)

The full `:eyelib-clientsmoke:test` suite (84 tests) has 3 pre-existing failures unrelated to this plan's changes:

| Test | Failure |
|------|---------|
| `ClientSmokeExitCodeTest > buildJUnitXml contains XML declaration` | Pre-existing |
| `ClientSmokeExitCodeTest > buildJUnitXml contains testsuite element` | Pre-existing |
| `ClientSmokeStatePhase3Test > handleExit: source file contains EXIT_AFTER_SMOKE` | Pre-existing |

These 3 failures existed before this plan executed and were not introduced or affected by the new test files. All 21 new tests (ConfigBridgeTest: 11, BuildIntegrityTest: 10) pass cleanly. These pre-existing failures are logged to `deferred-items.md` for resolution in a future phase.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness
- CORR-03 static verification complete: 21 automated regression tests guard the system property bridge and build.gradle integrity
- Ready for 07-02 hardware smoke checklist (semi-automated verification)
- The 3 pre-existing test failures in `ClientSmokeExitCodeTest` and `ClientSmokeStatePhase3Test` should be addressed in a future phase; they are logged in `deferred-items.md`

---
*Phase: 07-verification-polish*
*Completed: 2026-05-08*
