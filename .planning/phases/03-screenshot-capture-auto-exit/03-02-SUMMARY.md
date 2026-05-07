---
phase: 03-screenshot-capture-auto-exit
plan: 02
subsystem: runtime
tags: [state-machine, jvm-exit, shutdown, test-infrastructure, junit5, forge]

# Dependency graph
requires:
  - phase: 03-01
    provides: "HUD_HIDE/SCREENSHOT/EXIT enum values, testIndex/exitStartTick fields, switch dispatch, screenshot capture pipeline"
provides:
  - "Two-phase JVM exit: mc.stop() + 60-tick countdown + Runtime.halt(0)"
  - "STABILIZE→HUD_HIDE transition completing the Phase 3 pipeline"
  - "EXIT_AFTER_SMOKE config gating (false → IDLE instead of halt)"
  - "6 new unit tests for exit flow and stabilization handoff"
affects: ["03-03 (Phase 4 test execution)", "end-to-end smoke test pipeline"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Two-phase JVM shutdown with countdown guard"
    - "One-shot log guard to prevent per-tick log spam"
    - "Source-scanning unit tests for production code assertions"

key-files:
  created: []
  modified:
    - "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java"
    - "eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStatePhase3Test.java"

key-decisions:
  - "T-03-09 mitigation: testIndex guard uses >= (not ==) to be resilient to overflow, plus halt() as terminal guarantee"
  - "T-03-06 mitigation: screenshot writes PNG synchronously 80+ ticks before EXIT state, widening the safe window before halt()"

patterns-established:
  - "Two-phase shutdown: mc.stop() on tick 0, countdown on subsequent ticks, halt(0) at tick 60"
  - "Source-file-content assertions in tests: grepping production code for expected string patterns"

requirements-completed: [EXIT-01, EXIT-02]

# Metrics
duration: 6 min
completed: 2026-05-07
---

# Phase 03 Plan 02: Auto-Exit + STABILIZE Handoff Summary

**Two-phase JVM exit (mc.stop() + 60-tick countdown + Runtime.halt(0)) with config gating, wired STABILIZE→HUD_HIDE transition completing the Phase 3 pipeline**

## Performance

- **Duration:** 6 min
- **Started:** 2026-05-07T11:21:35Z
- **Completed:** 2026-05-07T11:27:11Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Replaced placeholder `handleExit()` with full two-phase JVM exit: Phase 1 calls `mc.stop()` with try-catch, Phase 2 counts 60 ticks then fires `Runtime.getRuntime().halt(0)`
- Wired STABILIZE→HUD_HIDE transition in `handleStabilize()` — when stabilization completes and `testIndex >= discoveredTests.size()`, the pipeline flows into screenshot capture and auto-exit
- Added 6 new unit tests covering exit method existence, halt(0) call, mc.stop() graceful shutdown, EXIT_AFTER_SMOKE config gating, stabilization→HUD_HIDE transition, and placeholder removal verification
- All 32 tests pass (12 from Plan 03-01 + 6 new exit flow tests)

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace placeholder handleExit()** - `5799182` (feat: implement two-phase JVM exit — mc.stop() + 60-tick countdown + Runtime.halt(0))
2. **Task 2: Wire STABILIZE→HUD_HIDE transition + tests** - `4642ff4` (feat: wire STABILIZE→HUD_HIDE transition; add exit flow unit tests)

**Plan metadata:** to be committed after SUMMARY.md creation

## Files Created/Modified
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java` — `handleExit()` rewritten with two-phase shutdown; `handleStabilize()` modified to transition to HUD_HIDE
- `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStatePhase3Test.java` — 6 new exit flow tests added

## Decisions Made
- Used `System.currentTimeMillis() / 50` as wall-clock fallback when `mc.level` is null (after `mc.stop()` clears it), ensuring countdown still fires `halt()` as final guarantee
- Used `testIndex >= discoveredTests.size()` (not `==`) for the STABILIZE guard — resilient to field corruption; `halt()` provides terminal guarantee per T-03-09

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

- **Test `handleStabilize_transitionsToHudHide` initially failed**: `indexOf("transitionTo(ClientSmokeState.HUD_HIDE")` found the first occurrence in `onRenderLevelStage()` (before the "Phase 2 complete" log in `handleStabilize()`). Fixed by switching to `lastIndexOf()` to locate the handleStabilize occurrence. Trivial fix — no impact on production code.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness
- EXIT pipeline complete: STABILIZE → HUD_HIDE → SCREENSHOT → EXIT → halt(0)
- `testIndex < discoveredTests.size()` guard in `handleStabilize()` provides clean handoff to Phase 4 TEST_EXEC interleaving
- Ready for Phase 04 — test execution, next-test loop, and report generation

---
*Phase: 03-screenshot-capture-auto-exit*
*Completed: 2026-05-07*
