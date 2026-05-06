---
phase: 01-module-scaffolding-config-annotation-discovery
plan: 02
subsystem: api
tags: [annotation, bytecode, asm, scan-discovery]
requires:
  - phase: 01-01
    provides: Gradle build config for annotation subproject (java-library without Forge)
provides:
  - "@ClientSmoke annotation with RetentionPolicy.CLASS, ElementType.TYPE, 3 attributes"
  - "Pure JVM annotation JAR (1588 bytes) with zero Minecraft/Forge dependencies"
affects: [01-05-scanner, 04-test-execution]
tech-stack:
  added: []
  patterns:
    - "RetentionPolicy.CLASS for ModFileScanData ASM visibility without runtime reflection"
    - "@NullMarked package-info.java for null-safety annotation (existing eyelib convention)"
key-files:
  created:
    - eyelib-clientsmoke-annotation/src/main/java/io/github/tt432/clientsmokeannotation/ClientSmoke.java
    - eyelib-clientsmoke-annotation/src/main/java/io/github/tt432/clientsmokeannotation/package-info.java
  modified: []
key-decisions:
  - "@Retention(RetentionPolicy.CLASS) per D-06 — annotation visible to ASM at bytecode, invisible to getAnnotation() at runtime"
  - "@Target(ElementType.TYPE) per D-07 — class-level only for v1, no method-level scanning"
  - "Three attributes: description (String), priority (int), modId (String) per D-08"
  - "Annotation class has zero Minecraft/Forge/NeoForge imports — pure Java annotation"
patterns-established:
  - "@ClientSmoke contract: test authors annotate classes; framework discovers via ASM; Phase 4 loads and invokes"
requirements-completed: [ANN-01, MOD-01]
duration: 4min
completed: 2026-05-06
---

# Phase 01 Plan 02: @ClientSmoke Annotation Definition Summary

**@ClientSmoke annotation defined with RetentionPolicy.CLASS (ASM-visible, reflection-invisible), ElementType.TYPE (class-level only), and three attributes — compiled to a 1.6 KB pure-JVM JAR with zero Minecraft dependencies.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-05-06T11:16:00+08:00
- **Completed:** 2026-05-06T11:20:00+08:00
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- `@ClientSmoke` annotation with RetentionPolicy.CLASS — visible to ModFileScanData ASM scanner, invisible via java.lang.reflect at runtime
- Three attributes: `description()` (human-readable test description), `priority()` (execution ordering), `modId()` (optional namespace filter)
- `@Target(ElementType.TYPE)` — class-level only, no method-level support for v1
- Package-info.java with `@NullMarked` for null-safety per eyelib convention
- Annotation JAR is 1588 bytes — contains only `ClientSmoke.class` and `package-info.class`, zero Minecraft/Forge/NeoForge entries

## Task Commits

1. **Task 1-2: @ClientSmoke Annotation** - `923372a` (feat: annotation class + package-info)

## Files Created/Modified
- `eyelib-clientsmoke-annotation/src/main/java/io/github/tt432/clientsmokeannotation/ClientSmoke.java` — @ClientSmoke annotation definition with RetentionPolicy.CLASS, ElementType.TYPE, 3 attributes
- `eyelib-clientsmoke-annotation/src/main/java/io/github/tt432/clientsmokeannotation/package-info.java` — Package documentation with @NullMarked

## Verification

- `:eyelib-clientsmoke-annotation:build` → BUILD SUCCESSFUL (exit 0)
- Output JAR: `eyelib-clientsmoke-annotation-21.1.14+1.20.1-forge.jar` (1588 bytes)
- JAR contents: only `ClientSmoke.class`, `package-info.class`, `META-INF/` — no `net/minecraft/`, `net/minecraftforge/`, `net/neoforged/`
- Zero Minecraft/Forge/NeoForge imports in any source file
- No `ElementType.METHOD`, no `RetentionPolicy.RUNTIME`, no `RetentionPolicy.SOURCE`

## Decisions Made
- Followed plan exactly — all annotation attributes, retention policy, and target match specifications

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness
- @ClientSmoke annotation JAR ready for consumption by Plan 05 (scanner) which looks for `Lio/github/tt432/clientsmokeannotation/ClientSmoke;` in ModFileScanData
- Annotation can be placed on any mod's compileOnly classpath for test authoring

---
*Plan: 01-02*
*Completed: 2026-05-06*
