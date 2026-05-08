---
phase: 05-gradle-run-configuration-classpath
plan: 01
subsystem: build
tags: [gradle, mdgl, legacyforge, run-config, classpath, gitignore]

# Dependency graph
requires: []
provides:
  - "clientSmoke Gradle run config in root build.gradle"
  - "Unconditional localRuntime dependency on eyelib-clientsmoke"
  - "Isolated gameDirectory (run/clientsmoke/) for smoke tests"
  - "Gitignore entries for runtime smoke artifacts"
affects: ["06-system-property-injection", "07-smoke-test-verification"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "MDGL legacyForge.runs DSL for custom run configurations"
    - "localRuntime configuration for optional runtime mods (unconditional inclusion)"
    - "gameDirectory isolation pattern for separate run configs"

key-files:
  created: []
  modified:
    - build.gradle
    - .gitignore

key-decisions:
  - "Placed clientSmoke run config between client and server blocks in legacyForge.runs for logical grouping"
  - "Removed enableSmokeTest Gradle property gate — mod behavior controlled at runtime by ClientSmokeConfig.isEnabled()"
  - "Kept compileOnly for annotation module, localRuntime for runtime mod — preserves standard MDGL dependency management pattern"

patterns-established:
  - "Unconditional localRuntime: compile-time annotation visibility + always-on runtime mod, behavior gated by system property at launch"
  - "Run config isolation: each run target gets its own gameDirectory to prevent state bleed"

requirements-completed: [GRAD-01, GRAD-02, GRAD-03, GRAD-04]

# Metrics
duration: 5min
completed: 2026-05-08
---

# Phase 5 Plan 01: Gradle Run Configuration & Classpath Summary

**MDGL `clientSmoke` run config with isolated `run/clientsmoke/` game directory, unconditional `eyelib-clientsmoke` localRuntime dependency, and `.gitignore` entries for smoke test artifacts**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-05-08T06:04:00Z
- **Completed:** 2026-05-08T06:09:05Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Declared `clientSmoke` run configuration in `legacyForge.runs` DSL with isolated `gameDirectory = project.file('run/clientsmoke')` (GRAD-01, GRAD-02)
- Removed `enableSmokeTest` Gradle property conditional gate — `eyelib-clientsmoke` now unconditionally on `localRuntime` classpath (GRAD-03)
- Added `run/clientsmoke/` and `clientsmoke-reports/` to `.gitignore` to prevent runtime artifact leakage into version control (GRAD-04)
- Verified `runClientSmoke --dry-run` resolves task graph successfully (exit code 0) via JetBrains MCP Gradle runner

## Task Commits

Each task was committed atomically:

| Task | Description | Commit |
|------|-------------|--------|
| 1 | Declare clientSmoke run config + unconditional localRuntime | `61611e6` (feat) |
| 2 | Add smoke artifact directories to .gitignore | `7244cd6` (chore) |

**Plan metadata:** to be committed separately

## Files Modified
- `build.gradle` — Added `clientSmoke` run config (lines 68-72), removed conditional `enableSmokeTest` gate (line 167, now unconditional `localRuntime`)
- `.gitignore` — Added `run/clientsmoke/` and `clientsmoke-reports/` entries (lines 55-56) with documentation comment

## Verification Results

### Acceptance Criteria (Task 1)
| # | Criterion | Result |
|---|-----------|--------|
| 1 | `grep "clientSmoke {" build.gradle` → 1 match | ✅ PASS |
| 2 | `grep "gameDirectory = project.file('run/clientsmoke')" build.gradle` → 1 match | ✅ PASS |
| 3 | `grep "forge.enabledGameTestNamespaces.*clientsmoke" build.gradle` → 1 match (root) | ✅ PASS |
| 4 | `grep "enableSmokeTest" build.gradle` → 0 matches | ✅ PASS |
| 5 | `grep "localRuntime project(':eyelib-clientsmoke')" build.gradle` → 1 match (no `if` wrapping) | ✅ PASS |
| 6 | `grep "compileOnly project(':eyelib-clientsmoke-annotation')" build.gradle` → 1 match (unchanged) | ✅ PASS |
| 7 | `clientSmoke` block between `client {` and `server {` | ✅ PASS |

### Acceptance Criteria (Task 2)
| # | Criterion | Result |
|---|-----------|--------|
| 1 | `grep "run/clientsmoke/" .gitignore` → 1 match | ✅ PASS |
| 2 | `grep "clientsmoke-reports/" .gitignore` → 1 match | ✅ PASS |
| 3 | Both entries on distinct lines | ✅ PASS |
| 4 | `git check-ignore run/clientsmoke/somefile.log` → success | ✅ PASS |
| 5 | `git check-ignore clientsmoke-reports/somefile.xml` → success | ✅ PASS |

### Overall Verification
- **`runClientSmoke --dry-run`:** `BUILD SUCCESSFUL in 15s` via JetBrains MCP — task graph resolves with `writeClientSmokeLegacyClasspath`, `prepareClientSmokeRun`, and `runClientSmoke` tasks ✅
- **`eyelib-clientsmoke` dependency:** Present in task graph dependency chain (compilation task included) ✅

## Decisions Made
- Placed `clientSmoke` run config between `client` and `server` blocks for logical grouping within the existing `runs` structure
- Used `client()` factory method to inherit default client JVM args and main class behavior from `configureEach`
- Set `gameTestNamespaces` to `'clientsmoke'` (literal, not `project.mod_id`) to scope game test scanning to the smoke test mod namespace
- Kept `compileOnly project(':eyelib-clientsmoke-annotation')` unchanged — annotation visibility at compile time remains conditional-free

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — both tasks completed on first attempt with all acceptance criteria passing.

## Threat Mitigations Implemented
- **T-05-02 (Information Disclosure):** `.gitignore` `run/clientsmoke/` entry prevents accidental commit of smoke test runtime artifacts (screenshots, logs, config)
- **T-05-03 (Information Disclosure):** `.gitignore` `clientsmoke-reports/` entry prevents accidental commit of test report XML/JSON

## Next Phase Readiness
- Ready for Phase 6 (System Property Injection) — `runClientSmoke` task is available as a Gradle target
- `eyelib-clientsmoke` is now unconditionally on classpath; Phase 6 will inject JVM system properties to toggle its runtime behavior per run config
- The IntelliJ "Run Client Smoke Tests" IDE run configuration should auto-generate after the next Gradle sync in the IDE

---
*Phase: 05-gradle-run-configuration-classpath*
*Completed: 2026-05-08*
