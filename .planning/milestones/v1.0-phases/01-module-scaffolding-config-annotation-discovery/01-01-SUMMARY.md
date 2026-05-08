---
phase: 01-module-scaffolding-config-annotation-discovery
plan: 01
subsystem: build
tags: [gradle, legacyforge, java-library, subproject]
requires: []
provides:
  - Gradle build infrastructure for annotation subproject (pure JVM JAR, zero Minecraft deps)
  - Gradle build infrastructure for runtime subproject (legacyForge 2.0.91, Forge 1.20.1)
  - Root module wiring via compileOnly (annotation) and conditional localRuntime (runtime, gated by enableSmokeTest)
affects: [all-phase-1-plans, build-system]
tech-stack:
  added: []
  patterns:
    - "compileOnly + conditional localRuntime pattern for optional runtime mod loading"
    - "java-library subproject pattern without legacyForge for pure JVM annotation JARs"
key-files:
  created:
    - eyelib-clientsmoke-annotation/build.gradle
    - eyelib-clientsmoke/build.gradle
  modified:
    - settings.gradle
    - build.gradle (root)
    - gradle.properties
key-decisions:
  - "Annotation module uses only java-library + maven-publish plugins — zero Minecraft/Forge dependencies per D-04"
  - "Runtime module uses legacyForge 2.0.91 with modId 'clientsmoke' and processResources for mods.toml variable expansion"
  - "Root build.gradle: compileOnly on annotation, conditional localRuntime on runtime (gated by enableSmokeTest=false default)"
  - "enableSmokeTest property defaults to false — runtime mod is opt-in, not loaded in standard builds"
patterns-established:
  - "compileOnly project + conditional localRuntime for optional subprojects"
  - "java-library subproject without legacyForge for pure JVM annotation modules"
requirements-completed: [MOD-01, MOD-02, MOD-03]
duration: 8min
completed: 2026-05-06
---

# Phase 01 Plan 01: Gradle Build Configuration Summary

**Two new Gradle subprojects created — annotation (pure JVM JAR, zero MC deps) and runtime (legacyForge Forge 1.20.1 mod) — wired into root build with compileOnly + gated localRuntime pattern.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-05-06T11:09:00+08:00
- **Completed:** 2026-05-06T11:17:00+08:00
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments
- Annotation subproject (`eyelib-clientsmoke-annotation`) build.gradle with java-library + maven-publish only — zero Minecraft/Forge/Lombok plugins
- Runtime subproject (`eyelib-clientsmoke`) build.gradle with legacyForge 2.0.91, Forge 1.20.1 config, modId "clientsmoke", processResources for mods.toml expansion
- Root wiring: compileOnly on annotation, conditional localRuntime on runtime gated by `enableSmokeTest=false` default in gradle.properties

## Task Commits

All three tasks committed as one build configuration unit:

1. **Task 1-3: Gradle Build Configuration** - `eb26ecc` (build: all subproject build.gradle files + root wiring changes)

## Files Created/Modified
- `eyelib-clientsmoke-annotation/build.gradle` — Pure JVM JAR subproject, only java-library + maven-publish plugins
- `eyelib-clientsmoke/build.gradle` — Forge 1.20.1 runtime mod subproject with legacyForge 2.0.91, compileOnly on annotation, processResources expansion
- `settings.gradle` — Added `include("eyelib-clientsmoke-annotation")` and `include("eyelib-clientsmoke")`
- `build.gradle` (root) — Added compileOnly on annotation and conditional localRuntime on runtime (gated by `enableSmokeTest`)
- `gradle.properties` — Added `enableSmokeTest=false`

## Verification

- `:eyelib-clientsmoke-annotation:build` → BUILD SUCCESSFUL (exit 0)
- `:eyelib-clientsmoke:build` → BUILD SUCCESSFUL (exit 0)
- Annotation JAR produced at `eyelib-clientsmoke-annotation/build/libs/` (261 bytes — skeleton, no Java sources yet)
- Runtime JAR produced at `eyelib-clientsmoke/build/libs/` (311 bytes — skeleton, no Java sources yet)
- Both JARs are minimal skeletons — they exist and build cleanly, ready for Java sources in Plans 02-05

## Decisions Made
- Followed plan exactly — all build.gradle content, wiring patterns, and dependency configurations match the plan specifications
- Used `java-library` plugin (not `java`) for annotation subproject to match existing eyelib subproject convention
- Conditional localRuntime guarded by `project.findProperty('enableSmokeTest')` — runtime mod only loads when opt-in

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness
- Both subprojects compile successfully with `./gradlew build`
- Ready for Plan 02 (@ClientSmoke annotation) and Plan 03 (runtime mod entrypoint + mods.toml)
- All subsequent Phase 1 plans depend on these build files to compile their Java sources

---
*Plan: 01-01*
*Completed: 2026-05-06*
