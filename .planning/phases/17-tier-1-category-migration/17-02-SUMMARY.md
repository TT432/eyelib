---
phase: 17-tier-1-category-migration
plan: 02
subsystem: eyelib-util-math
tags: [eyelib-util, math, migration, gradle, forge]
status: complete

requires:
  - phase: 17-tier-1-category-migration
    plan: 01
    provides: "ColorEncodings under io.github.tt432.eyelibutil.color and root :eyelib-util dependency edge"
provides:
  - "Five math utility sources under io.github.tt432.eyelibutil.math"
  - "Old root util/math copies removed for the five migrated math helpers"
affects: [phase-17, eyelib-util, math-utilities]

tech-stack:
  added: []
  patterns:
    - "Package-only migration into io.github.tt432.eyelibutil.math"
    - "FastColorHelper delegates to migrated io.github.tt432.eyelibutil.color.ColorEncodings"

key-files:
  created:
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Curves.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/EyeMath.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/MathHelper.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/FastColorHelper.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Shapes.java
  modified:
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/SearchResults.java
  deleted:
    - src/main/java/io/github/tt432/eyelib/util/math/Curves.java
    - src/main/java/io/github/tt432/eyelib/util/math/EyeMath.java
    - src/main/java/io/github/tt432/eyelib/util/math/MathHelper.java
    - src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java
    - src/main/java/io/github/tt432/eyelib/util/math/Shapes.java

key-decisions:
  - "Preserved valid interrupted partial work where the math files had already been moved to :eyelib-util."
  - "Did not migrate Phase 18/19/20 resource, texture, codec, or submodule-centralization files."
  - "Did not commit changes because the user explicitly requested no commits."

requirements-completed: [MIGR-01]
duration: unknown
completed: 2026-05-10
---

# Phase 17 Plan 02: Math Utility Migration Summary

**The five Phase 17 math helpers now live under `io.github.tt432.eyelibutil.math`, with old root math source copies absent and `:eyelib-util:build` passing through JetBrains MCP.**

## Performance

- **Duration:** not measured in-shell
- **Completed:** 2026-05-10
- **Tasks completed:** 2/2
- **Files modified/created/deleted:** 12 plan-relevant or verification-unblocking files

## Accomplishments

- Preserved the interrupted partial move of `Curves`, `EyeMath`, `MathHelper`, `FastColorHelper`, and `Shapes` into `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/`.
- Verified all five moved files declare `package io.github.tt432.eyelibutil.math;`.
- Verified `FastColorHelper` imports `io.github.tt432.eyelibutil.color.ColorEncodings`.
- Verified the old root copies under `src/main/java/io/github/tt432/eyelib/util/math/` are absent.
- Left Phase 18/19/20-owned resource, texture, codec, and submodule-centralization files unmigrated.

## Task Commits

No commits were created. The user explicitly required: **Do not commit.**

## Verification

### Passed

- `jetbrain_run_gradle_tasks(projectPath="E:\\_ideaProjects\\qylEyelib", externalProjectPath="E:\\_ideaProjects\\qylEyelib", taskNames=[":eyelib-util:build"], scriptParameters="", timeoutMillis=240000)` exited `0`.
- `jetbrain_search_file(paths=["eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/"], q="*.java")` returned exactly the five target math files: `Curves.java`, `EyeMath.java`, `MathHelper.java`, `FastColorHelper.java`, and `Shapes.java`.
- `jetbrain_search_file(paths=["src/main/java/io/github/tt432/eyelib/util/math/"], q="*.java")` returned no files for the old root math source directory.
- `jetbrain_search_regex(paths=["eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/*.java"], q="package\\s+io\\.github\\.tt432\\.eyelibutil\\.math;")` found package declarations in all five target files.
- `jetbrain_search_regex(paths=["eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/FastColorHelper.java"], q="import\\s+io\\.github\\.tt432\\.eyelibutil\\.color\\.ColorEncodings;")` found the required direct import.
- IDE diagnostics reported zero file-local errors for `FastColorHelper.java`, `Shapes.java`, and the verification-unblocking `SearchResults.java`; the post-build diagnostics query also reported zero build errors.

### Initial Blocker Resolved

- The first `:eyelib-util:build` run failed before math compilation could be accepted because interrupted Phase 17 search partial work left `SearchResults.java` importing the old deleted `io.github.tt432.eyelib.util.search.Searchable` package.
- The import was removed because `SearchResults` and `Searchable` are now in the same `io.github.tt432.eyelibutil.search` package; the same JetBrains MCP build gate then passed.

## Deviations from Plan

### Auto-fixed / Required Adjustments

**1. [Rule 3 - Blocking] Fixed stale same-package search import from interrupted partial work**
- **Found during:** Task 1 verification with `:eyelib-util:build`.
- **Issue:** `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/SearchResults.java` imported deleted root `io.github.tt432.eyelib.util.search.Searchable`, preventing the required util-module build gate from running cleanly.
- **Fix:** Removed the stale import; the class now resolves the same-package `Searchable` type.
- **Files modified:** `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/SearchResults.java`
- **Verification:** Re-ran `jetbrain_run_gradle_tasks(... taskNames=[":eyelib-util:build"] ...)`; exit code `0`.

**2. [User Constraint] Skipped commits**
- **Found during:** execution requirements.
- **Issue:** Executor defaults call for commits, but the user explicitly required no commits.
- **Fix:** No task or metadata commits were created.
- **Files modified:** none by this adjustment.

## Known Stubs

None found in files created or modified by this plan.

## Threat Flags

None. The planned trust boundary (`root runtime -> :eyelib-util math`) was handled as package relocation only; no new network, auth, file-access, or schema trust surface was introduced by the math move.

## Deferred Issues

- Root production consumers still contain old `io.github.tt432.eyelib.util.math.*` imports. This was observed during residual inspection but not changed because Plan 02's file list and action are limited to moving the five math utility sources and verifying `:eyelib-util`; root consumer rewiring belongs to a later Phase 17 plan.

## Self-Check: PASSED

- Summary file created at `.planning/phases/17-tier-1-category-migration/17-02-SUMMARY.md`.
- Key target files exist under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/`.
- Old root math files are absent under `src/main/java/io/github/tt432/eyelib/util/math/`.
- Required JetBrains MCP `:eyelib-util:build` gate passed with exit code `0`.
- No commits were expected or checked because the user required no commits.

---
*Phase: 17-tier-1-category-migration*  
*Plan: 02*  
*Completed: 2026-05-10 with targeted JetBrains MCP build verification passing*
