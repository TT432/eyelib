---
phase: 01-module-scaffolding-config-annotation-discovery
plan: 05
subsystem: scanner
tags: [modfilescandata, asm, bytecode, zero-class-loading, annotation-discovery]
requires:
  - phase: 01-02
    provides: "@ClientSmoke annotation with RetentionPolicy.CLASS for ASM visibility"
  - phase: 01-04
    provides: ClientSmokeConfig.ENABLED gate for scanner activation"
provides:
  - "ModFileScanData-based bytecode scanner — discovers @ClientSmoke classes with zero JVM class loading"
  - "DiscoveredTest record with className, description, priority, modId"
  - "Scanner wired into @Mod constructor inside enabled-gate"
affects: [04-test-execution, 02-state-machine]
tech-stack:
  added:
    - net.minecraftforge.fml.ModList (getAllScanData)
    - net.minecraftforge.forgespi.language.ModFileScanData
    - org.objectweb.asm.Type (annotation descriptor)
  patterns:
    - "ModFileScanData.AnnotationData iteration — proven pattern from ForgeMolangMappingDiscovery"
    - "Type-safe extractString/extractInt from untyped annotationData Map"
    - "Utility class pattern: final class, private constructor, static scan() method"
key-files:
  created:
    - eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/scanner/ClientSmokeScanner.java
  modified:
    - eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/ClientSmokeMod.java
key-decisions:
  - "ZERO Class.forName(), ClassLoader.loadClass(), or java.lang.reflect — class names stored as strings, loading deferred to Phase 4"
  - "Two-layer enabled-gate: (1) constructor checks ENABLED before calling scan(), (2) scan() itself checks ENABLED as first operation"
  - "Type.getType(ClientSmoke.class) for ASM annotation type — compares descriptors, not class objects"
  - "extractString/extractInt with instanceof checks and defaults — handles null/unexpected annotation data safely"
patterns-established:
  - "Zero-class-loading annotation scan: read Metadata from ModFileScanData.AnnotationData, defer Class.forName() to execution phase"
  - "Double-gated pattern: external guard (constructor) + internal guard (scan method) for safety"
requirements-completed: [ANN-02, ANN-03]
duration: 5min
completed: 2026-05-06
---

# Phase 01 Plan 05: Scanner — ModFileScanData Zero-Class-Loading Discovery Summary

**Bytecode-level @ClientSmoke scanner using Forge's ModFileScanData infrastructure — discovers annotated classes from ALL JARs on the classpath with zero JVM class initialization. No Class.forName(), no reflection, no class loading until Phase 4 test execution.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-05-06T11:29:00+08:00
- **Completed:** 2026-05-06T11:34:00+08:00
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- `ClientSmokeScanner` utility class with `scan()` method that iterates `ModList.get().getAllScanData()` for `@ClientSmoke` annotations
- `DiscoveredTest` record stores className, description, priority, modId — pure data, no references to actual class objects
- Annotation type matching via `Type.getType(ClientSmoke.class)` — compares ASM descriptors, not class instances
- Safe attribute extraction: `extractString` and `extractInt` with `instanceof` checks and default values
- INFO-level logging for discovered test count and per-test details (className, priority, description)
- Double-gated: constructor if-block AND internal scan() method both check `ClientSmokeConfig.ENABLED`
- Scanner wired into `ClientSmokeMod` constructor inside the enabled-if block

## Task Commits

1. **Task 1-2: Scanner + Constructor Wiring** - `e204aab` (feat: zero-class-loading ModFileScanData scanner)

## Files Created/Modified
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/scanner/ClientSmokeScanner.java` — ModFileScanData scanner with DiscoveredTest record, zero class loading
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/ClientSmokeMod.java` — Added scanner import and wired `ClientSmokeScanner.scan()` inside enabled-gate

## Verification

- `:eyelib-clientsmoke:build` → BUILD SUCCESSFUL (exit 0)
- **Class-loading safety:** ZERO calls to Class.forName() in scanner code (verified by grep; `loadClass` appears only in Javadoc explaining the safety guarantee)
- **Class-loading safety:** ZERO imports of java.lang.reflect
- `ModList.get().getAllScanData()` iteration pattern present (line 80)
- `Type.getType(ClientSmoke.class)` annotation type descriptor (line 77)
- `ClientSmokeConfig.ENABLED.get()` internal gate (line 72)
- Scanner call is INSIDE the `if (ClientSmokeConfig.ENABLED.get())` block in constructor

## Decisions Made
- Followed the proven ForgeMolangMappingDiscovery iteration pattern — same ModList.get().getAllScanData() → AnnotationData loop
- Key deviation from reference: stored className String instead of calling Class.forName() — this IS the core architectural invariant

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness
- Phase 1 complete — all 5 plans delivered, all 9 requirements covered
- Foundation ready for Phase 2 (State Machine + World Lifecycle)
- Scanner produces `List<DiscoveredTest>` ready for state machine to consume
- Phase 1 deliverables: 2 Gradle subprojects, @ClientSmoke annotation, runtime mod entrypoint, ForgeConfigSpec config, ModFileScanData scanner

---
*Plan: 01-05*
*Completed: 2026-05-06*
