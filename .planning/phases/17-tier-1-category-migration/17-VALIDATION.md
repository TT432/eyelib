---
phase: 17
slug: tier-1-category-migration
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-10
---

# Phase 17 — Validation Strategy

> Per-phase validation contract for Tier-1 utility migration into `:eyelib-util`.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Java/Gradle via JetBrains MCP plus residual import scans |
| **Config file** | `build.gradle`, `eyelib-util/build.gradle`, `settings.gradle` |
| **Quick run command** | JetBrains regex scans for old Phase 17 util imports and old source paths |
| **Full suite command** | JetBrains MCP `:eyelib-util:build` and full project build/rebuild |
| **Estimated runtime** | ~2-5 minutes depending on Gradle sync |

## Sampling Rate

- After each migration group: IDE diagnostics on moved files and direct consumers.
- After each wave: `:eyelib-util:build` via JetBrains MCP.
- Final gate: full project build via JetBrains MCP and residual import scan = zero.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 17-01-01 | dependency/import prep | 0 | MIGR-01, MIGR-02 | static | root depends on `project(':eyelib-util')` only after consumer imports require it | ✅ complete |
| 17-02-01 | zero-dependency utilities | 1 | MIGR-01 | build/static | moved files exist under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/`; old root copies absent | ✅ complete |
| 17-03-01 | collection utilities and ListHelper deletion | 1 | MIGR-02 | build/static | `ListHelper.java` absent; former consumers import `eyelibutil.collection.ListAccessors` | ✅ complete |
| 17-04-01 | final verification | 2 | MIGR-01, MIGR-02 | build | JetBrains MCP full project build succeeds; residual old imports zero | ✅ complete |

## Plan 06 Final Gate Evidence

Recorded: 2026-05-10.

### Exact Target Presence Checks

All target checks were verified twice: by exact JetBrains MCP `jetbrain_search_file(... paths=["<exact target path>"], q="<exact filename>")` returning one item, and by explicit local `Test-Path -PathType Leaf` returning present.

| Trace | Target path | Evidence |
|-------|-------------|----------|
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/SimpleTimer.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/FixedStepTimerState.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/color/ColorEncodings.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 / D-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Curves.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/EyeMath.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/MathHelper.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/FastColorHelper.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Shapes.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/Searchable.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-01 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/SearchResults.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-02 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Blackboard.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-02 / D-02 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Lists.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-02 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Collectors.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-02 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/EntryStreams.java` | PASS: JetBrains search returned exact path; explicit check present |
| MIGR-02 / D-03 | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/ListAccessors.java` | PASS: JetBrains search returned exact path; explicit check present |

### Exact Old Source Absence Checks

Every old path was verified by exact JetBrains MCP `jetbrain_search_file(... paths=["<exact old path>"], q="<exact filename>")` returning `items: []`, and by explicit local `Test-Path` returning absent.

| Trace | Old path | Evidence |
|-------|----------|----------|
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/SimpleTimer.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerState.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/core/util/color/ColorEncodings.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 / D-01 | `src/main/java/io/github/tt432/eyelib/util/SharedLibraryLoader.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/math/Curves.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/math/EyeMath.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/math/MathHelper.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/math/Shapes.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/search/Searchable.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-01 | `src/main/java/io/github/tt432/eyelib/util/search/SearchResults.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-02 | `src/main/java/io/github/tt432/eyelib/util/Blackboard.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-02 / D-02 | `src/main/java/io/github/tt432/eyelib/util/Lists.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-02 | `src/main/java/io/github/tt432/eyelib/util/Collectors.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-02 | `src/main/java/io/github/tt432/eyelib/util/EntryStreams.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-02 / D-03 | `src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java` | PASS: JetBrains search zero; explicit check absent |
| MIGR-02 / D-03 deletion | `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` | PASS: JetBrains search zero; explicit check absent |

### Residual Import and Reference Scans

| Scope | Pattern | Evidence |
|-------|---------|----------|
| `src/main/java/**`, `src/test/java/**`, `eyelib-util/src/test/java/**` | `import\s+io\.github\.tt432\.eyelib\.(?:core\.)?util\.(?:time|math|search|collection|color|Blackboard|Lists|Collectors|EntryStreams|SharedLibraryLoader|ListHelper)|\bListHelper\b` | PASS: `jetbrain_search_regex` returned `items: []` |

### JetBrains MCP Build Gates

| Gate | Tool invocation | Evidence |
|------|-----------------|----------|
| Util module solo build / D-02 FastUtil classpath | `jetbrain_run_gradle_tasks(projectPath="E:\\_ideaProjects\\qylEyelib", externalProjectPath="E:\\_ideaProjects\\qylEyelib", taskNames=[":eyelib-util:build"], scriptParameters="", timeoutMillis=240000)` | PASS: exitCode `0`; output includes `:eyelib-util:compileJava UP-TO-DATE`, `:eyelib-util:test UP-TO-DATE`, and `BUILD SUCCESSFUL in 1s` |
| Full project rebuild | `jetbrain_build_project(projectPath="E:\\_ideaProjects\\qylEyelib", filesToRebuild=[], rebuild=true, timeout=600000)` | PASS: `isSuccess=true`, `problems=[]`. Initial 300000ms invocation timed out, then the same JetBrains MCP build succeeded with the longer timeout. |

### Documentation Gate

| Check | Evidence |
|-------|----------|
| Stale scaffold-only claim removed from the five touched docs | PASS: the JetBrains text search for the plan-specified stale scaffold phrase returned only the plan's verification instruction, not `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `eyelib-util/README.md`, or `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md`. |
| Active util namespace documented | PASS: `io.github.tt432.eyelibutil` appears in the touched maintainer docs, including `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `eyelib-util/README.md`, and `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md`. |

## Plan 06 Sign-Off

- [x] MIGR-01: Tier-1 time/color/loader/math/search targets are present under `:eyelib-util`; old root/core source paths are absent; residual old imports are zero.
- [x] MIGR-02: Collection targets are present under `:eyelib-util`; old collection paths and `ListHelper.java` are absent; residual `ListHelper` references are zero.
- [x] D-01: `SharedLibraryLoader` is verified at `io.github.tt432.eyelibutil.loader` target path.
- [x] D-02: `Lists.java` is verified at `io.github.tt432.eyelibutil.collection` target path and `:eyelib-util:build` succeeds through JetBrains MCP.
- [x] D-03: `ListAccessors` is verified as the replacement target and `ListHelper.java` is verified absent.
- [x] No Phase 18 resource/texture, Phase 19 codec, or Phase 20 submodule-centralized files were migrated by Plan 06.

## Wave 0 Requirements

- [x] Root build dependency on `:eyelib-util` when migrated imports are introduced.
- [x] Target packages under `io.github.tt432.eyelibutil`: `time`, `color`, `loader`, `math`, `search`, `collection`.
- [x] No Phase 18/19/20 files migrated early.

## Manual-Only Verifications

All Phase 17 criteria should be automated/static-verifiable. No manual validation expected.

## Validation Sign-Off

- [x] All tasks have automated verification
- [x] No shell Gradle commands
- [x] `nyquist_compliant: true` set

**Approval:** approved 2026-05-10
