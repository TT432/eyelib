---
phase: 17-tier-1-category-migration
plan: 05
subsystem: eyelib-util-collection-migration
tags: [eyelib-util, collection, fastutil, listhelper, migration]
status: complete

requires:
  - phase: 17-tier-1-category-migration
    plan: 01
    provides: "Root dependency edge to :eyelib-util"
  - phase: 17-tier-1-category-migration
    plan: 04
    provides: "BrBoneKeyFrame math imports already rewired while ListHelper remained staged"
provides:
  - "Collection utilities under io.github.tt432.eyelibutil.collection"
  - "FastUtil-backed Lists.asList verified by :eyelib-util:build"
  - "ListHelper deleted after BrBoneKeyFrame rewired to ListAccessors.first/last"
affects: [phase-17, eyelib-util, client-animation, client-model, core-util-tests]

tech-stack:
  added: []
  patterns:
    - "Behavior-preserving package migration into :eyelib-util collection namespace"
    - "Shim deletion after direct canonical accessor rewiring"
    - "JetBrains MCP-only Gradle verification"

key-files:
  created:
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Blackboard.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Lists.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Collectors.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/EntryStreams.java
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/ListAccessors.java
    - eyelib-util/src/test/java/io/github/tt432/eyelibutil/collection/ListAccessorsTest.java
  modified:
    - eyelib-util/README.md
    - eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md
    - src/main/java/io/github/tt432/eyelib/client/model/ModelPartModel.java
    - src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrBoneKeyFrame.java
    - src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java
  deleted:
    - src/main/java/io/github/tt432/eyelib/util/Blackboard.java
    - src/main/java/io/github/tt432/eyelib/util/Lists.java
    - src/main/java/io/github/tt432/eyelib/util/Collectors.java
    - src/main/java/io/github/tt432/eyelib/util/EntryStreams.java
    - src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java
    - src/main/java/io/github/tt432/eyelib/util/ListHelper.java

key-decisions:
  - "Migrated only Phase 17 collection files; Phase 18/19/20 resource, texture, codec, and submodule-centralization files were not migrated."
  - "Kept Lists.java's FastUtil Int2ObjectFunction API intact and verified it through :eyelib-util:build."
  - "Deleted ListHelper only after BrBoneKeyFrame and the root seam test imported io.github.tt432.eyelibutil.collection.ListAccessors."
  - "Did not commit changes because the user explicitly requested no commits."

requirements-completed: [MIGR-02]
duration: not measured
completed: 2026-05-10
---

# Phase 17 Plan 05: Collection Utility Migration Summary

**Collection utilities now live in `io.github.tt432.eyelibutil.collection`; `BrBoneKeyFrame` uses `ListAccessors.first/last`; `ListHelper` is deleted after verified rewiring.**

## Performance

- **Duration:** not measured in-shell
- **Completed:** 2026-05-10
- **Tasks completed:** 3/3
- **Files modified/created/deleted:** 18 plan-relevant paths plus this summary

## Accomplishments

- Moved `Blackboard`, `Lists`, `Collectors`, `EntryStreams`, and `ListAccessors` into `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/` with implementation behavior preserved.
- Added util-module collection tests covering `ListAccessors.first/last`, `Lists.asList(...)` lazy FastUtil-backed view behavior, and `EntryStreams.collectSequenced()`.
- Rewired `ModelPartModel` to import `io.github.tt432.eyelibutil.collection.EntryStreams`.
- Rewired `BrBoneKeyFrame` from `ListHelper.getFirst/getLast` to `ListAccessors.first/last` without changing interpolation, codec, or keyframe behavior.
- Rewired `CoreUtilitySeamTest` to the new collection package while leaving Phase 18/19 assertions (`TexturePaths`, `Eithers`) on their old owners.
- Deleted old root/core collection source files and the `ListHelper` shim.
- Updated util module README files to document `collection/` as an active migrated package.

## Task Commits

No commits were created. The user explicitly required: **Do not commit.**

## Verification

### Passed

- `jetbrain_run_gradle_tasks(projectPath="E:\\_ideaProjects\\qylEyelib", externalProjectPath="E:\\_ideaProjects\\qylEyelib", taskNames=[":eyelib-util:build"], scriptParameters="", timeoutMillis=240000)` completed with exit code `0` and `BUILD SUCCESSFUL`, proving the FastUtil-dependent `Lists.java` classpath per D-02.
- `jetbrain_build_project(projectPath="E:\\_ideaProjects\\qylEyelib", filesToRebuild=["src/main/java/io/github/tt432/eyelib/client/model/ModelPartModel.java", "src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrBoneKeyFrame.java", "src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java"], rebuild=false, timeout=120000)` returned `isSuccess=true` with no problems.
- Residual `ListHelper` scan returned zero results under `src/main/java/**` and `src/test/java/**` for `io.github.tt432.eyelib.util.ListHelper|\bListHelper\b`.
- Residual old collection import scan returned zero results for `import io.github.tt432.eyelib.(core.)?util.(collection.ListAccessors|Blackboard|Lists|Collectors|EntryStreams);`.
- Positive import scan found expected new imports in `ModelPartModel`, `BrBoneKeyFrame`, and `CoreUtilitySeamTest`.
- `EntryStreams.java` still imports `java.util.stream.Collectors`, preserving the JDK collector dependency and avoiding the Eyelib `Collectors` collision.
- `Lists.java` still contains `it.unimi.dsi.fastutil.ints.Int2ObjectFunction`, proving the API was not simplified or degraded.
- File presence/absence checks found all new collection files and `ListAccessorsTest`, and confirmed old root/core collection paths plus `ListHelper.java` are absent.
- IDE diagnostics reported zero problems for `Lists.java`, `ListAccessors.java`, `BrBoneKeyFrame.java`, and `ModelPartModel.java`.

## Deviations from Plan

### Required Adjustments

**1. [User Constraint] Skipped commits**
- **Found during:** execution requirements.
- **Issue:** Executor defaults call for per-task commits, but the user explicitly required no commits.
- **Fix:** No task or metadata commits were created.
- **Files modified:** none by this adjustment.

**2. [Execution Constraint] Used narrow apply_patch moves instead of IDE move refactor**
- **Found during:** source migration.
- **Issue:** The repository already contained extensive unrelated uncommitted changes from prior phases; broad IDE move/refactor risked touching unrelated imports beyond Plan 05 scope.
- **Fix:** Used `apply_patch` for narrow file moves/import rewires and used IDE-aware sync, diagnostics, searches, and JetBrains MCP builds for semantic verification.
- **Files modified:** plan-listed files only.

## Known Stubs

None found in files created or modified by this plan. Stub-pattern scans on plan-touched collection/test files returned no placeholder/TODO/FIXME content; a broader package scan found pre-existing TODO text in `BrAnimationEntry.java`, which was not modified by this plan.

## Threat Flags

None. The planned trust boundaries were handled by package/import migration only; no new network endpoint, auth path, file-access behavior, or schema trust boundary was introduced. T-17-10/T-17-11/T-17-12 mitigations were verified by caller rewiring, `:eyelib-util:build`, and collision-aware import scans.

## Deferred Issues

- None for Plan 05.

## Self-Check: PASSED

- Summary file created at `.planning/phases/17-tier-1-category-migration/17-05-SUMMARY.md`.
- All collection target files exist under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/`.
- `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` and old collection source owners are absent.
- Required JetBrains MCP Gradle/build/search gates passed.
- No commits were expected or checked because the user required no commits.

---
*Phase: 17-tier-1-category-migration*  
*Plan: 05*  
*Completed: 2026-05-10 with JetBrains MCP verification passing*
