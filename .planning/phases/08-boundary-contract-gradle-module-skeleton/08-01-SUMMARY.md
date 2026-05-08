---
phase: 08-boundary-contract-gradle-module-skeleton
plan: 01
subsystem: infra
tags: [gradle, forge, java17, module-boundary, eyelib-particle]

requires:
  - phase: 07-verification-polish
    provides: v1.1 verified baseline before particle module separation
provides:
  - Forge-visible `:eyelib-particle` Gradle subproject skeleton
  - One-way root-to-particle dependency wiring
  - Particle module package boundary and nullness metadata
affects: [phase-08, phase-09, phase-10, phase-11, phase-12, phase-13, phase-14, particle-module]

tech-stack:
  added: [net.neoforged.moddev.legacyforge, java-library, maven-publish, jspecify, junit-jupiter]
  patterns: [one-way Gradle project dependency, Forge-visible subproject metadata, NullMarked module package]

key-files:
  created:
    - eyelib-particle/build.gradle
    - eyelib-particle/src/main/resources/META-INF/mods.toml
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
  modified:
    - settings.gradle
    - build.gradle
    - MODULES.md
    - docs/index/repo-map.md
    - docs/architecture/01-module-boundaries.md
    - docs/architecture/02-side-boundaries.md

key-decisions:
  - "Use a Forge-visible `:eyelib-particle` skeleton matching existing feature-module patterns."
  - "Keep Phase 8 free of particle runtime moves; root consumes the new module one-way."
  - "Document module inventory and boundary docs immediately because AGENTS.md requires docs fan-out when adding modules."

patterns-established:
  - "Root dependency cluster uses `api`, `modImplementation`, and `jarJar` for Forge-visible particle consumption."
  - "Particle module build metadata must not declare `project(':')` or root runtime package dependencies."

requirements-completed: [PGRAD-01, PAPI-02]

duration: 35 min
completed: 2026-05-09
---

# Phase 08 Plan 01: Boundary Contract & Gradle Module Skeleton Summary

**Forge-visible `:eyelib-particle` Gradle skeleton with one-way root consumption and documented root-independent particle boundary**

## Performance

- **Duration:** 35 min
- **Started:** 2026-05-09T00:00:00Z
- **Completed:** 2026-05-09T00:35:00Z
- **Tasks:** 2/2 completed
- **Files modified:** 10

## Accomplishments

- Registered `:eyelib-particle` as a first-class Gradle subproject in `settings.gradle`.
- Added root `api`, `modImplementation`, and `jarJar` wiring so root consumes the particle module without reverse dependency wiring.
- Created `eyelib-particle` build metadata, Forge `mods.toml`, `@NullMarked` package marker, and module README boundary contract.
- Updated module inventory and architecture/navigation docs required by project rules for a newly added module.

## Task Commits

Each task was committed atomically:

1. **Task 1: Register `:eyelib-particle` and wire one-way root consumption** - `98109f1` (feat)
2. **Task 2: Create particle module build metadata and package boundary marker** - `12ef340` (feat)

**Plan metadata:** committed separately in the docs commit that adds this summary.

## Files Created/Modified

- `settings.gradle` - Includes `eyelib-particle` beside other first-class subprojects.
- `build.gradle` - Adds root `api`, `modImplementation`, and `jarJar` dependencies on `:eyelib-particle`.
- `eyelib-particle/build.gradle` - Defines Java 17, LegacyForge, Lombok, JUnit, resources expansion, sources jar, and Maven publication metadata.
- `eyelib-particle/src/main/resources/META-INF/mods.toml` - Provides Forge-visible `eyelibparticle` module metadata.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java` - Marks the package `@NullMarked` and states forbidden root dependency categories.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` - Documents module scope, dependency direction, and integration rules.
- `MODULES.md` - Adds the `eyelib-particle` module inventory row and summary mention.
- `docs/index/repo-map.md` - Adds particle module to repository shape and topic routing.
- `docs/architecture/01-module-boundaries.md` - Adds particle module current area and target owner mapping.
- `docs/architecture/02-side-boundaries.md` - Adds particle module side/dependency rules.

## Decisions Made

- Followed the existing Forge-visible feature module pattern (`:eyelib-material`) rather than making a pure Java-only placeholder, because the plan requires Forge metadata and root runtime consumption.
- Did not move existing particle runtime classes; later phases own API/store/schema/runtime/loading/network/render extraction.
- Added module documentation fan-out during this plan even though detailed ownership docs are also planned later, because project `AGENTS.md` requires module inventory and impacted architecture/index docs when adding a module.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added required documentation fan-out for the new module**
- **Found during:** Task 2 (Create particle module build metadata and package boundary marker)
- **Issue:** The plan's task file list focused on build/resource/package metadata, but `AGENTS.md` requires `MODULES.md` and impacted index/architecture docs to be updated when adding a module.
- **Fix:** Added the particle subproject to `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and created a module-local README.
- **Files modified:** `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`
- **Verification:** Referenced paths exist; JetBrains MCP `:eyelib-particle:compileJava` passed; added docs contain no shell Gradle invocation lines.
- **Committed in:** `12ef340`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Documentation fan-out was required for correctness with project rules and did not move particle runtime behavior or alter planned dependency direction.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Verification

- PASS: PowerShell content checks confirmed exactly one `include("eyelib-particle")` and exactly one root `api`, `modImplementation`, and `jarJar` dependency for `:eyelib-particle`.
- PASS: PowerShell content checks confirmed `archivesName = 'eyelib-particle'`, no `implementation project(':')` in `eyelib-particle/build.gradle`, `modId="eyelibparticle"`, and `@NullMarked` package metadata.
- PASS: JetBrains MCP `jetbrain_sync_gradle_projects` for `E:\_ideaProjects\qylEyelib` exited 0.
- PASS: JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames: [":eyelib-particle:compileJava"]` exited 0.
- PASS: Committed diffs introduced no shell Gradle invocation lines.

## Self-Check: PASSED

- Verified key created files exist on disk.
- Verified task commits `98109f1` and `12ef340` exist in recent git history.
- Verified acceptance criteria and plan-level compile/content checks passed.

## Next Phase Readiness

Ready for Plan 08-02 to expand module ownership documentation without needing to re-create the build skeleton. Later phases can add API/store/schema/runtime seams against the now-buildable `:eyelib-particle` module.

---
*Phase: 08-boundary-contract-gradle-module-skeleton*
*Completed: 2026-05-09*
