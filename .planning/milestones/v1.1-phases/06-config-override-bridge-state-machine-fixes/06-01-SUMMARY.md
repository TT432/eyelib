---
phase: 06-config-override-bridge-state-machine-fixes
plan: 01
subsystem: build
tags: [gradle, system-property, forge-config, jogamp, bridge, config-override]

# Dependency graph
requires: []
provides:
  - "ClientSmokeConfig.isEnabled() — system-property-first, TOML-fallback static method (OVRD-01)"
  - "ClientSmokeConfig.shouldExitAfterSmoke() — system-property-first, TOML-fallback static method (OVRD-02)"
  - "Gradle clientSmoke run config injects clientsmoke.enabled=true and clientsmoke.autoExit=true as JVM -D flags (OVRD-03)"
affects: ["06-state-machine-fixes", "06-junit-xml-exit-code"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "System.getProperty() → Boolean.parseBoolean() → ForgeConfigSpec fallback pattern for config overrides"
    - "MDGL systemProperty() DSL as JVM -D flag injection bridge"

key-files:
  created: []
  modified:
    - eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/config/ClientSmokeConfig.java
    - build.gradle

key-decisions:
  - "System-property-first, TOML-fallback override pattern — no new dependency or annotation framework needed"
  - "Used Boolean.parseBoolean() for system property parsing — 'true' (case-insensitive) → true, anything else → false"
  - "Both override methods placed after SPEC field and before constructor, with a section comment header"

patterns-established:
  - "System-property-first config bridge: System.getProperty() returns non-null → use it; null → fall back to ForgeConfigSpec.get()"

requirements-completed: [OVRD-01, OVRD-02, OVRD-03]

# Metrics
duration: 13min
completed: 2026-05-08
---

# Phase 6 Plan 01: System Property Override Bridge & Gradle Injection Summary

**System-property-first config override methods in ClientSmokeConfig with Gradle `systemProperty()` JVM flag injection for smoke test auto-enable and auto-exit**

## Performance

- **Duration:** 13 min
- **Started:** 2026-05-08T06:29:55Z
- **Completed:** 2026-05-08T06:42:58Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Added `ClientSmokeConfig.isEnabled()` — system-property-first (`clientsmoke.enabled`) with ForgeConfigSpec fallback (OVRD-01)
- Added `ClientSmokeConfig.shouldExitAfterSmoke()` — system-property-first (`clientsmoke.autoExit`) with ForgeConfigSpec fallback (OVRD-02)
- Injected `systemProperty 'clientsmoke.enabled', 'true'` and `systemProperty 'clientsmoke.autoExit', 'true'` into the `clientSmoke` Gradle run config (OVRD-03)

## Task Commits

Each task was committed atomically:

| Task | Description | Commit |
|------|-------------|--------|
| 1 | Add system property override bridge methods to ClientSmokeConfig | `efc04ed` (feat) |
| 2 | Inject system properties into clientSmoke Gradle run config | `5e9bb10` (feat) |

## Files Modified
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/config/ClientSmokeConfig.java` — Added `isEnabled()` (lines 102-108) and `shouldExitAfterSmoke()` (lines 123-129) with system-property-first, ForgeConfigSpec-fallback pattern; all existing fields unchanged
- `build.gradle` — Added two `systemProperty` lines (72-73) inside `clientSmoke` block injecting `clientsmoke.enabled=true` and `clientsmoke.autoExit=true`; no other run configs affected

## Verification Results

### Task 1 Acceptance Criteria
| # | Criterion | Result |
|---|-----------|--------|
| 1 | `isEnabled()` method count → 1 | ✅ PASS |
| 2 | `shouldExitAfterSmoke()` method count → 1 | ✅ PASS |
| 3 | `System.getProperty("clientsmoke.enabled")` count → 1 | ✅ PASS |
| 4 | `System.getProperty("clientsmoke.autoExit")` count → 1 | ✅ PASS |
| 5 | `isEnabled()` after SPEC line, before constructor | ✅ PASS |
| 6 | Existing fields (ENABLED, EXIT_AFTER_SMOKE, SPEC, SCREENSHOT_DELAY, RELOAD_STABILIZE_TICKS) unchanged | ✅ PASS |
| 7 | Constructor not duplicated (count → 1) | ✅ PASS |

### Task 2 Acceptance Criteria
| # | Criterion | Result |
|---|-----------|--------|
| 1 | `systemProperty 'clientsmoke.enabled', 'true'` in build.gradle → 1 | ✅ PASS |
| 2 | `systemProperty 'clientsmoke.autoExit', 'true'` in build.gradle → 1 | ✅ PASS |
| 3 | Both lines within `clientSmoke { }` block | ✅ PASS |
| 4 | `client()`, `gameDirectory`, `forge.enabledGameTestNamespaces` all preserved | ✅ PASS |
| 5 | `clientSmoke {` count → 1 (no duplicate block) | ✅ PASS |
| 6 | No double injection (each systemProperty → 1) | ✅ PASS |

### Overall Verification
- **Compilation:** `jetbrain_build_project` on `ClientSmokeConfig.java` — 0 errors ✅
- **Config file structure:** 3 refs to `isEnabled`/`shouldExitAfterSmoke` (≥2 each required) ✅
- **Gradle config integrity:** Both `systemProperty` injections present exactly once ✅

## Decisions Made
- Used `Boolean.parseBoolean()` for system property parsing — `"true"` (case-insensitive) → true, anything else → false; null → fallback to ForgeConfigSpec
- Placed override methods between `SPEC` field and private constructor with a dedicated section comment header for clarity
- Used Groovy string literals `'true'` for `systemProperty()` values — MDGL passes these as JVM `-D` string flags
- No new imports needed — `System.getProperty()` and `Boolean.parseBoolean()` are `java.lang` members

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — both tasks completed on first attempt with all acceptance criteria passing.

## Threat Mitigations

All three threats in the plan's STRIDE register were classified as `accept`:
- **T-06-01 (Tampering):** Build scripts are version-controlled; no runtime exposure to untrusted input
- **T-06-02 (Information Disclosure):** Property values (`true`/`false`) carry no secrets
- **T-06-03 (Elevation of Privilege):** System property set by Gradle (trusted); if attacker can set JVM properties, they already own the process

No new threat surface beyond what the plan anticipated.

## Known Stubs

None — both methods are fully wired with real system property reads and ForgeConfigSpec fallbacks.

## Next Phase Readiness
- `runClientSmoke` now passes `-Dclientsmoke.enabled=true -Dclientsmoke.autoExit=true` to the JVM, which `ClientSmokeConfig.isEnabled()` and `shouldExitAfterSmoke()` will pick up
- `runClient` does NOT inject these properties — `System.getProperty()` returns `null`, so the mod falls back to ForgeConfigSpec defaults (`enabled=false`), keeping the mod idle
- Ready for Phase 6 Plan 02 (state machine fixes, JUnit XML, exit code propagation)

---
*Phase: 06-config-override-bridge-state-machine-fixes*
*Completed: 2026-05-08*

## Self-Check: PASSED

- **Files on disk:** `06-01-SUMMARY.md` ✅, `ClientSmokeConfig.java` ✅, `build.gradle` ✅
- **Commits present:** `efc04ed` (Task 1) ✅, `5e9bb10` (Task 2) ✅
- **Compilation:** `jetbrain_build_project` on modified files → 0 errors ✅
