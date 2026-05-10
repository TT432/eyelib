---
phase: 17-tier-1-category-migration
verified: 2026-05-10T13:04:22Z
status: passed
score: 5/5 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: passed
  previous_score: 5/5
  gaps_closed: []
  gaps_remaining: []
  regressions: []
---

# Phase 17: Tier-1 Category Migration Verification Report

**Phase Goal:** Zero-dependency utility categories (time, color, loader, math, search — 11 files) and collection utilities (Blackboard, Lists, Collectors, EntryStreams) reside entirely in `:eyelib-util` with all root consumers compiling against the new module without regression.
**Verified:** 2026-05-10T13:04:22Z
**Status:** passed
**Re-verification:** Yes — review-fix revalidation after `17-REVIEW-FIX.md` changes to `eyelib-util/build.gradle`, `17-VALIDATION.md`, `package-info.java`, `SharedLibraryLoader.java`, and `UtilModuleIdentityTest.java`.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | All 11 zero-dependency files exist in `:eyelib-util` and old root/core source copies are gone. | ✓ VERIFIED | `glob` found `SimpleTimer`, `FixedStepTimerState`, `ColorEncodings`, `SharedLibraryLoader`, `Curves`, `EyeMath`, `MathHelper`, `FastColorHelper`, `Shapes`, `Searchable`, and `SearchResults` under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/**`; exact old root/core path globs returned no files. |
| 2 | All collection utility files exist in `:eyelib-util` and old root/core source copies are gone. | ✓ VERIFIED | `glob` found `Blackboard`, `Lists`, `Collectors`, `EntryStreams`, and `ListAccessors` under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/`; old `src/main/java/io/github/tt432/eyelib/util/{Blackboard,Lists,Collectors,EntryStreams,ListHelper}.java` and old core `ListAccessors.java` globs returned no files. |
| 3 | JetBrains MCP full project build completed successfully. | ✓ VERIFIED | Re-run for review-fix revalidation: `jetbrain_build_project(projectPath="E:\\_ideaProjects\\qylEyelib", filesToRebuild=[], rebuild=true, timeout=600000)` returned `isSuccess=true`, `problems=[]`. |
| 4 | Old Phase 17 util imports across source/test are zero. | ✓ VERIFIED | `jetbrain_search_regex` for old Phase 17 import packages and `ListHelper` over `src/main/java/**`, `src/test/java/**`, and `eyelib-util/src/test/java/**` returned `items: []`; follow-up grep found no old imports. |
| 5 | `ListHelper.java` is deleted and former consumers compile using `ListAccessors`. | ✓ VERIFIED | `ListHelper.java` old path absent; `BrBoneKeyFrame.java` imports `io.github.tt432.eyelibutil.collection.ListAccessors` and calls `ListAccessors.first/last`; targeted residual scan for source/test `ListHelper` references returned no code hits; full build passed. |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/SimpleTimer.java` | migrated timer utility | ✓ VERIFIED | Present in target package. |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/FixedStepTimerState.java` | migrated fixed-step timer state | ✓ VERIFIED | Present in target package; old core path absent. |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/color/ColorEncodings.java` | migrated color helper | ✓ VERIFIED | Present in target package; old core path absent. |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` | migrated loader utility | ✓ VERIFIED | Present in locked `loader` package; old root path absent. Existing `return null` paths are documented control flow, not a stub. |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/{Curves,EyeMath,MathHelper,FastColorHelper,Shapes}.java` | migrated math helpers | ✓ VERIFIED | All five target files present; old root `util/math` copies absent. `FastColorHelper` delegates to `eyelibutil.color.ColorEncodings`. |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/{Searchable,SearchResults}.java` | migrated search helpers | ✓ VERIFIED | Both target files present; old root `util/search` copies absent. |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/{Blackboard,Lists,Collectors,EntryStreams,ListAccessors}.java` | migrated collection helpers and canonical list accessors | ✓ VERIFIED | All five target files present; old root/core copies absent. `Lists.java` retains `Int2ObjectFunction` API and `:eyelib-util:build` passed. |
| `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` | deleted shim | ✓ VERIFIED | File absent; former consumer rewired to `ListAccessors`. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| root `build.gradle` | `:eyelib-util` | `api`, `modImplementation`, `jarJar project(':eyelib-util')` | ✓ WIRED | Lines 167-169 declare all three dependency edges. |
| Root math/search/collection consumers | `io.github.tt432.eyelibutil.*` | Java imports | ✓ WIRED | Old Phase 17 import regex returned zero; positive evidence includes `BrBoneKeyFrame` and `BrAttachableLoader` imports. |
| `BrBoneKeyFrame.java` | `ListAccessors` | direct import and `first/last` calls | ✓ WIRED | Lines 17 and 195/199/203/215/216/233 use `ListAccessors`; no `ListHelper` import remains. |
| `:eyelib-util` | project-internal dependencies | leaf-module build file check | ✓ WIRED | Review-fix revalidation confirmed `jetbrain_search_regex` for `\bproject\s*\(` in `eyelib-util/build.gradle` returned `items: []`; the build file exposes JOML and FastUtil as `api` while keeping zero `project(...)` dependencies. |

### Data-Flow Trace (Level 4)

Not applicable: Phase 17 migrates Java utility classes and import/build wiring, not dynamic UI/API data rendering.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| FastUtil-backed util module compiles/tests after review fixes | `jetbrain_run_gradle_tasks(... taskNames=[":eyelib-util:build"], timeoutMillis=240000)` | exitCode `0`, `BUILD SUCCESSFUL in 116ms`; `:eyelib-util:test UP-TO-DATE` and `:eyelib-util:build UP-TO-DATE` | ✓ PASS |
| Full project compiles after review fixes | `jetbrain_build_project(... rebuild=true, timeout=600000)` | `isSuccess=true`, `problems=[]` | ✓ PASS |
| Old Phase 17 imports are absent | `jetbrain_search_regex` over source/test old import patterns | `items: []` | ✓ PASS |
| Util module remains a leaf after dependency exposure fix | `jetbrain_search_regex(paths=["eyelib-util/build.gradle"], q="\\bproject\\s*\\(")` | `items: []` | ✓ PASS |

### Review-Fix Revalidation

| Changed Area | Revalidation Evidence | Status |
|---|---|---|
| `eyelib-util/build.gradle` | Lines 49-50 declare JOML and FastUtil as `api`; `jetbrain_search_regex` found zero `project(...)` calls. | ✓ VERIFIED |
| `17-VALIDATION.md` | Frontmatter is `status: complete`, `nyquist_compliant: true`, `wave_0_complete: true`; per-task map and Wave 0 checklist are checked complete. | ✓ VERIFIED |
| `package-info.java` + `UtilModuleIdentityTest.java` | Package docs now record active Phase 17 utility ownership; identity test asserts `active Phase 17 utility packages` and leaf boundary wording. IDE diagnostics returned `problemCount=0` for the test file. | ✓ VERIFIED |
| `SharedLibraryLoader.java` | `crc(InputStream)` now throws `RuntimeException("Unable to calculate native library checksum", ex)` on read failure instead of returning a partial checksum; IDE diagnostics returned `problemCount=0`. | ✓ VERIFIED |
| JetBrains MCP build gates | `:eyelib-util:build` exitCode `0`; full `jetbrain_build_project` returned `isSuccess=true`, `problems=[]`. | ✓ VERIFIED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| MIGR-01 | 17-01 through 17-04, 17-06 | Zero-dependency utility categories migrated into `:eyelib-util` with updated root import sites. | ✓ SATISFIED | 11 target files present, old source paths absent, old import regex zero, full build passed. |
| MIGR-02 | 17-05, 17-06 | Collection utilities migrated into `:eyelib-util`. | ✓ SATISFIED | Collection targets plus `ListAccessors` present, old sources and `ListHelper` absent, `BrBoneKeyFrame` uses `ListAccessors`, full build passed. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` | 188, 290 | `return null` | ℹ️ Info | Existing documented sentinel/control-flow behavior for native-library loading path resolution; not placeholder behavior and not blocking Phase 17. |
| `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java` | 28 | method name mentions legacy `ListHelper` | ℹ️ Info | Test name documents legacy equivalence; no old import or production dependency remains. |

### Human Verification Required

None. All Phase 17 success criteria are static/build-verifiable and were checked through JetBrains MCP or source scans.

### Gaps Summary

No blocking gaps found. Phase 17's required utility migration, old-source removal, import rewiring, `ListHelper` deletion, and JetBrains MCP build gates remain verified after the review-fix changes.

---

_Verified: 2026-05-10T13:04:22Z_
_Verifier: the agent (gsd-verifier)_
