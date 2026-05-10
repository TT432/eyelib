---
phase: 17-tier-1-category-migration
plan: 01
subsystem: build-utility-migration
tags: [gradle, forge, eyelib-util, color, time, native-loader]
status: complete

requires:
  - phase: 16-module-scaffold-build-infrastructure
    provides: ":eyelib-util scaffold and leaf-module build identity"
provides:
  - "Root dependency edge to :eyelib-util"
  - "Migrated time, color, and native loader utilities under io.github.tt432.eyelibutil"
  - "Util-module behavior tests for FixedStepTimerState and ColorEncodings"
affects: [phase-17, phase-18, phase-19, phase-20, eyelib-util]

tech-stack:
  added: []
  patterns:
    - "IDE-aware Java file move from root/core utility paths into :eyelib-util package namespace"
    - "Root Forge-module consumption via api + modImplementation + jarJar project(':eyelib-util')"

key-files:
  created:
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/SimpleTimer.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/FixedStepTimerState.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/color/ColorEncodings.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java
    - eyelib-util/src/test/java/io/github/tt432/eyelibutil/time/FixedStepTimerStateTest.java
    - eyelib-util/src/test/java/io/github/tt432/eyelibutil/color/ColorEncodingsTest.java
  modified:
    - build.gradle
    - eyelib-util/README.md
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md
    - src/main/java/io/github/tt432/eyelib/client/render/texture/NativeImageIO.java
    - src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java
    - src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java

key-decisions:
  - "Followed D-01 by placing SharedLibraryLoader in io.github.tt432.eyelibutil.loader."
  - "Did not migrate Phase 18/19/20 resource, texture, codec, or submodule-centralization files."
  - "Did not commit changes because the user explicitly requested no commits."

patterns-established:
  - "Move utility implementation classes into category packages below io.github.tt432.eyelibutil without algorithm rewrites."
  - "Use JetBrains MCP Gradle tasks only for build verification."

requirements-completed: [MIGR-01]
duration: unknown
completed: 2026-05-10
---

# Phase 17 Plan 01: Tier-1 Utility Consumption and Time/Color/Loader Migration Summary

**Root now consumes `:eyelib-util`, and the first time/color/native-loader utilities live under `io.github.tt432.eyelibutil` with util-module behavior tests.**

## Performance

- **Duration:** not measured in-shell
- **Started:** 2026-05-10
- **Completed:** 2026-05-10
- **Tasks completed:** 3/3 verified; targeted IDE build now passes after the unrelated `TupleCodec.java` import fix.
- **Files modified/created:** 12 plan-relevant files

## Accomplishments

- Added the root dependency edge for `:eyelib-util` using the established Forge submodule pattern: `api`, `modImplementation`, and `jarJar`.
- Used IDE-aware moves for `SimpleTimer`, `FixedStepTimerState`, `ColorEncodings`, `SharedLibraryLoader`, and `FixedStepTimerStateTest` into `eyelib-util` category packages.
- Added `ColorEncodingsTest` in the util module and preserved the fixed-step timer behavior tests under the owning module.
- Rewired `NativeImageIO` and the root seam test away from the old core `ColorEncodings` import.
- Updated util module README files from scaffold-only wording to active migrated package documentation for `time`, `color`, and `loader`.

## Task Commits

No commits were created. The user explicitly required: **Do not commit.**

## Files Created/Modified

- `build.gradle` — root now consumes `:eyelib-util` through `api`, `modImplementation`, and `jarJar`.
- `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/SimpleTimer.java` — migrated timer utility with package-only change.
- `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/FixedStepTimerState.java` — migrated fixed-step timer state with `rate > 0` guard preserved.
- `eyelib-util/src/main/java/io/github/tt432/eyelibutil/color/ColorEncodings.java` — migrated ARGB/ABGR encoding helper.
- `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` — migrated shared/native library loader under the locked `loader` package.
- `eyelib-util/src/test/java/io/github/tt432/eyelibutil/time/FixedStepTimerStateTest.java` — moved timer behavior coverage into the owning module.
- `eyelib-util/src/test/java/io/github/tt432/eyelibutil/color/ColorEncodingsTest.java` — added color channel behavior coverage.
- `src/main/java/io/github/tt432/eyelib/client/render/texture/NativeImageIO.java` — imports `io.github.tt432.eyelibutil.color.ColorEncodings`.
- `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java` — no longer imports the old core color path; Phase 18/19 assertions remain on old owners.
- `src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java` — IDE move updated its delegated `ColorEncodings` import so the remaining shim compiles against the moved color owner.
- `eyelib-util/README.md` and `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md` — document active migrated packages.

## Verification

### Passed

- `jetbrain_search_in_files_by_text(... searchText="project(':eyelib-util')")` found root `build.gradle` entries for `api`, `modImplementation`, and `jarJar`.
- `jetbrain_search_regex(paths=["eyelib-util/build.gradle"], q="project\\(")` returned zero results, preserving `:eyelib-util` as a leaf module.
- `jetbrain_run_gradle_tasks(taskNames=[":eyelib-util:build"])` completed with exit code `0`.
- Residual old color import scan returned zero results for `import io.github.tt432.eyelib.core.util.color.ColorEncodings;` under root source/test paths.
- Residual old timer import scan returned zero results for `import io.github.tt432.eyelib.core.util.time.FixedStepTimerState;` under root source/test paths.
- Old source path glob checks found no root/core copies of `SimpleTimer.java`, `FixedStepTimerState.java`, `ColorEncodings.java`, or `SharedLibraryLoader.java`.
- IDE diagnostics on `NativeImageIO.java`, `CoreUtilitySeamTest.java`, and `ColorEncodings.java` reported zero file-local problems.
- Re-run after the `TupleCodec.java` import fix: `jetbrain_build_project(filesToRebuild=[NativeImageIO.java, CoreUtilitySeamTest.java])` succeeded with no problems.
- Re-run after the `TupleCodec.java` import fix: IDE diagnostics on `TupleCodec.java` reported zero file-local errors and zero build errors.

### Previously Blocked, Now Resolved

- The earlier `jetbrain_build_project(filesToRebuild=[NativeImageIO.java, CoreUtilitySeamTest.java])` blocker was caused by unrelated missing function imports in `src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java`.
- After the import fix, the same targeted IDE build gate passes, confirming the unrelated codec blocker no longer prevents Plan 17-01 verification.

## Deviations from Plan

### Auto-fixed / Required Adjustments

**1. [Rule 3 - Blocking] Updated `FastColorHelper` delegated color import**
- **Found during:** Task 2 IDE move of `ColorEncodings`.
- **Issue:** The remaining root `FastColorHelper` shim imported the moved old core `ColorEncodings` owner.
- **Fix:** IDE move rewired it to `io.github.tt432.eyelibutil.color.ColorEncodings` without migrating the math utility itself.
- **Files modified:** `src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java`
- **Verification:** `:eyelib-util:build` passed; file-local diagnostics passed for moved color class.

**2. [User Constraint] Skipped commits**
- **Found during:** execution requirements.
- **Issue:** Executor defaults call for commits, but the user explicitly required no commits.
- **Fix:** No task or metadata commits were created.
- **Files modified:** none by this adjustment.

## Known Stubs

None found in files created or modified by this plan.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: native-loader-relocation | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` | Existing filesystem/native library loading surface moved package only as planned; no algorithm rewrite. Future `AUDT-F01` audit remains deferred. |

## Deferred Issues

- None. The previously deferred `TupleCodec.java` import blocker is resolved.

## Self-Check: PASSED

- Summary file created at `.planning/phases/17-tier-1-category-migration/17-01-SUMMARY.md`.
- Key created files exist under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/{time,color,loader}` and util-module tests exist under `eyelib-util/src/test/java/io/github/tt432/eyelibutil/{time,color}`.
- No commits were expected or checked because the user required no commits.
- Overall plan status is complete because all Plan 17-01 verification gates now pass.

## Next Phase Readiness

- Plan 02+ should be able to consume the root `:eyelib-util` edge and the migrated `time`, `color`, and `loader` packages.
- Plan 01 is fully verified; the previously blocking `TupleCodec` compile errors are resolved.

---
*Phase: 17-tier-1-category-migration*
*Plan: 01*
*Completed: 2026-05-10 with targeted build verification passing*
