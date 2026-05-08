---
phase: 03-screenshot-capture-auto-exit
plan: 01
subsystem: client-smoke-test-runtime
tags: [forge-1.20.1, RenderLevelStageEvent, NativeImage, framebuffer-capture, png-output, state-machine]

requires:
  - phase: 02-state-machine-world-lifecycle-stabilization
    provides: "ClientSmokeState enum with INIT through ERROR, ClientSmokeStateMachine tick handler + switch dispatch + stabilization timer"
provides:
  - "HUD_HIDE/SCREENSHOT/EXIT enum values extending the state machine lifecycle"
  - "RenderLevelStageEvent.AFTER_LEVEL-based framebuffer capture pipeline"
  - "Timestamped PNG screenshot output to clientsmoke-reports/screenshots/"
  - "Phase 3 unit test scaffolding (12 tests)"
affects: ["03-02-auto-exit-implementation", "04-test-execution-reporting"]

tech-stack:
  added: []
  patterns:
    - "Multiple @SubscribeEvent methods in single @Mod.EventBusSubscriber class"
    - "Framebuffer read via NativeImage.downloadTexture() + flipY() + writeToFile()"
    - "Two-state HUD hide + capture with one-frame guard"

key-files:
  created:
    - "eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStatePhase3Test.java - 12 JUnit 5 tests for Phase 3 enum/fields/methods"
  modified:
    - "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeState.java - added HUD_HIDE, SCREENSHOT, EXIT enum values"
    - "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java - handleHudHide(), onRenderLevelStage(), new fields, switch dispatch"
    - "eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachineWorldTest.java - fixed WorldDimensions import"

key-decisions:
  - "D-01: Extended ClientSmokeState enum in-place (same file, same switch) rather than creating a separate enum"
  - "D-02: HUD_HIDE and SCREENSHOT are separate states to guarantee HUD toggle takes effect before capture frame"
  - "D-06: Custom framebuffer read (NativeImage.downloadTexture + flipY + writeToFile) — no Screenshot.grab() call"
  - "D-12: handleHudHide() sets hideGui=true and immediately transitionTo(SCREENSHOT) in the same tick handler"

patterns-established:
  - "One-frame guard: screenshotTakenThisFrame boolean prevents duplicate captures in same render pass"
  - "Source file scanning tests: resolve source from .class resource path, falls back gracefully if missing"
  - "EXIT placeholder: handleExit() transitions to IDLE pending Plan 03-02 real implementation"

requirements-completed: [CAP-01, CAP-02, CAP-03]

duration: 20min
completed: 2026-05-07
---

# Phase 3 Plan 1: Screenshot capture pipeline — enum extension, HUD toggle, and framebuffer-to-PNG output

**Framebuffer capture on render thread via RenderLevelStageEvent.AFTER_LEVEL with NativeImage downloadTexture + flipY + PNG output to clientsmoke-reports/screenshots/**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-05-07T11:13:58Z
- **Completed:** 2026-05-07T11:33:00Z
- **Tasks:** 3
- **Files modified:** 4 (1 created, 3 modified)

## Accomplishments
- Extended `ClientSmokeState` enum with HUD_HIDE, SCREENSHOT, EXIT values in correct declaration order after STABILIZE
- Implemented `handleHudHide()` — sets `hideGui=true` and transitions to SCREENSHOT in the same tick per D-02/D-12
- Implemented `onRenderLevelStage()` @SubscribeEvent — captures main framebuffer at AFTER_LEVEL stage into NativeImage, flips Y-axis, writes timestamped PNG to `clientsmoke-reports/screenshots/`, restores `hideGui=false`, advances testIndex
- Added 3 new static fields: `testIndex` (int, 0), `screenshotTakenThisFrame` (boolean, false), `exitStartTick` (long, -1L)
- Created 12 JUnit 5 unit tests covering enum existence, field reflection, method signatures, and source file content assertions

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend enum + add fields + update switch dispatch** — `0636461` (feat)
2. **Task 2: Implement handleHudHide() + onRenderLevelStage() screenshot capture** — `412275f` (feat)
3. **Task 3: Create unit tests for Phase 3 state enum and field/method existence** — `fb255aa` (test)

_Note: Task 3 commit also includes a Rule 3 fix for pre-existing WorldDimensions import and test reference._

## Files Created/Modified
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeState.java` — Added HUD_HIDE (line ~82), SCREENSHOT (line ~94), EXIT (line ~106) enum values with Javadoc
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java` — Added 6 new imports, 3 new fields, 3 new switch case arms, onRenderLevelStage() @SubscribeEvent handler (~110 lines), handleHudHide(), handleScreenshot(), handleExit() placeholders, updated terminal guard comment
- `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStatePhase3Test.java` — New: 12 tests (4 enum value, 3 reflection field, 2 reflection method, 3 source content assertions)
- `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachineWorldTest.java` — Fixed WorldDimensions import (dimension → levelgen)

## Decisions Made
- Followed all locked decisions D-01 through D-16 from CONTEXT.md without modification
- Used custom framebuffer read pattern (`NativeImage.downloadTexture(0, true)` + `flipY()`) rather than `Screenshot.grab()` per D-06
- Screenshot output directory: `FMLPaths.GAMEDIR.get().resolve("clientsmoke-reports").resolve("screenshots")` per D-13
- Filename format: `{SimpleClassName}-{yyyyMMdd-HHmmss}.png` per D-14/D-15
- EXIT is not in the terminal guard (IDLE || ERROR) — EXIT handler must continue receiving ticks for countdown

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed wrong WorldDimensions import in Phase 2 code**
- **Found during:** Task 3 (test execution)
- **Issue:** `import net.minecraft.world.level.dimension.WorldDimensions;` was wrong — Forge 1.20.1 mojang mappings place `WorldDimensions` at `net.minecraft.world.level.levelgen.WorldDimensions`. Pre-existing from Phase 2 but blocked Task 3 test compilation.
- **Fix:** Changed import in `ClientSmokeStateMachine.java` from `net.minecraft.world.level.dimension.WorldDimensions` to `net.minecraft.world.level.levelgen.WorldDimensions`. Also fixed the class reference in `ClientSmokeStateMachineWorldTest.java` (line 95) from fully-qualified `net.minecraft.world.level.dimension.WorldDimensions.class` to `WorldDimensions.class`.
- **Files modified:** `ClientSmokeStateMachine.java` (import line), `ClientSmokeStateMachineWorldTest.java` (import added + class reference)
- **Verification:** `:eyelib-clientsmoke:test` exits 0, all tests pass
- **Committed in:** `fb255aa` (Task 3 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** One pre-existing import error fixed to unblock test execution. No plan scope changes.

## Known Stubs

| Stub | File | Line | Reason |
|------|------|------|--------|
| `handleExit()` transitions to IDLE | `ClientSmokeStateMachine.java` | ~393 | Placeholder — real two-phase exit (mc.stop() + halt(0)) implemented in Plan 03-02 per D-04 |

## Issues Encountered
- Pre-existing `WorldDimensions` import error surfaced during `compileTestJava` — fixed via Rule 3 (described in Deviations)
- All 12 Phase 3 tests pass on first successful build after import fix

## Next Phase Readiness
- Phase 3 Plan 01 complete — state machine has full HUD_HIDE → SCREENSHOT → EXIT flow
- Ready for Plan 03-02: replace placeholder `handleExit()` with real two-phase JVM exit (mc.stop() + 3s countdown + halt(0))
- Phase 4 will insert TEST_EXEC and NEXT_TEST between STABILIZE and HUD_HIDE to complete the full test execution loop

---
*Phase: 03-screenshot-capture-auto-exit*
## Self-Check: PASSED

All 4 key files exist on disk. All 3 task commits verified in git history.

---
*Completed: 2026-05-07*
