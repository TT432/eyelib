---
phase: 10-schema-runtime-ownership-adapter
plan: 01
subsystem: particle-runtime-schema-adapter
tags: [java, gradle, particle, importer, molang, dataresult, junit]

requires:
  - phase: 09-particle-api-store-seam
    provides: particle module API/store boundary and root-clean dependency direction
provides:
  - ParticleDefinition runtime definition contract in :eyelib-particle
  - ParticleDefinitionAdapter importer BrParticle to runtime definition seam
  - Real witchspell fixture parity coverage for adapter field preservation
affects: [phase-10-plan-02, phase-11-runtime-client-core-extraction, phase-12-loading-publication-rewire]

tech-stack:
  added: [project(':eyelib-importer'), project(':eyelib-molang')]
  patterns: [DataResult loud-failure adapter, root-clean particle runtime definition, real fixture parity test]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java
    - eyelib-particle/src/test/resources/io/github/tt432/eyelibparticle/runtime/fixtures/witchspell.json
  modified:
    - eyelib-particle/build.gradle

key-decisions:
  - "Use ParticleDefinition, not another BrParticle, as the particle-module runtime definition name."
  - "Allow :eyelib-particle -> :eyelib-importer and :eyelib-particle -> :eyelib-molang only for the schema adapter seam."
  - "Preserve importer BrParticle curve/event/raw component data instead of reparsing or normalizing it in Phase 10."

patterns-established:
  - "Adapter seam: ParticleDefinitionAdapter.fromSchema(BrParticle) returns DataResult<ParticleDefinition> with clear validation errors."
  - "Parity fixture: copied witchspell.json is decoded through importer BrParticle.CODEC before adaptation."

requirements-completed: [PSCHEMA-01, PSCHEMA-02, PSCHEMA-03]

duration: 11min
completed: 2026-05-09
---

# Phase 10 Plan 01: Runtime Definition Adapter Summary

**Importer-owned Bedrock particle schema now converts into a root-clean particle-module ParticleDefinition through a named DataResult adapter with real witchspell fixture parity tests.**

## Performance

- **Duration:** 11 min
- **Started:** 2026-05-09T14:30:54Z
- **Completed:** 2026-05-09T14:41:48Z
- **Tasks:** 2/2 completed
- **Files modified:** 6 plan files

## Accomplishments

- Added `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` as the distinct particle-module runtime definition owner.
- Added `ParticleDefinitionAdapter.fromSchema(BrParticle)` with `DataResult` validation for null schema, missing effect/description/render parameters, blank identifier, blank material, and blank texture.
- Wired `:eyelib-particle` to consume `:eyelib-importer` and `:eyelib-molang` for the explicit adapter seam without root/MC/Forge imports.
- Added real `witchspell.json` fixture coverage proving identifier, format version, render parameters, component keys, selected nested raw values, events identity, curve keys, and billboard flipbook summary are preserved.

## Task Commits

1. **Task 1: Add runtime definition contract and adapter dependency wiring** — `1a0a4c9` (`feat`)
2. **Task 2: Prove adapter parity with a large real particle fixture** — `3487254` (`test`)

**Plan metadata:** committed after summary creation.

## Files Created/Modified

- `eyelib-particle/build.gradle` — adds intentional importer and Molang project dependencies for the schema adapter seam.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` — documents importer raw schema ownership, particle runtime definition ownership, and root legacy `BrParticle` status.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` — canonical module runtime definition preserving render parameters, curves, events, raw components, and billboard flipbook summary.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` — named importer schema adapter returning `DataResult<ParticleDefinition>`.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java` — real fixture parity and loud-failure coverage.
- `eyelib-particle/src/test/resources/io/github/tt432/eyelibparticle/runtime/fixtures/witchspell.json` — copied real particle fixture source for adapter parity tests.

## Decisions Made

- Kept importer `BrParticle` as the raw schema input and did not introduce a particle-module `BrParticle` duplicate.
- Stored `BrParticle.Curve`, `BrParticle.Events`, and `BedrockResourceValue` values directly in `ParticleDefinition` for Phase 10 to preserve Molang/raw component parity without reparsing.
- Added a direct `:eyelib-molang` dependency because public importer particle curve types expose Molang values through the adapter/runtime definition surface.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed test fixture lookup path**
- **Found during:** Task 2 (adapter parity test verification)
- **Issue:** The first parity test read the fixture using a project-root-relative filesystem path, but the Gradle test task executes from the subproject working directory and raised `NoSuchFileException`.
- **Fix:** Switched fixture loading to the test classpath resource path via `ClassLoader.getResourceAsStream`.
- **Files modified:** `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java`
- **Verification:** JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest` exited 0.
- **Committed in:** `3487254`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** The fix only corrected test resource loading; adapter scope and runtime boundary design were unchanged.

## Issues Encountered

- Initial targeted adapter test run failed because the fixture path assumed the root working directory. The classpath-based loader resolved it and the rerun passed.

## TDD Gate Compliance

- A `test(10-01)` commit exists for the fixture-backed parity coverage, but it was committed after the Task 1 implementation commit because Task 1's scoped files were production/config contracts and its verification gate was compile-only. No behavior implementation was accepted without the plan-level targeted test passing before summary creation.

## Verification

- JetBrains MCP `:eyelib-particle:compileJava` — exitCode 0 after Task 1 and again during final verification.
- JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest` — exitCode 0 after Task 2 and again during final verification.
- Source checks: no `class BrParticle` or `record BrParticle` declarations found under `eyelib-particle/src/main/java`; no root/Minecraft/Forge forbidden imports found under particle main sources.

## Known Stubs

None. Null checks in `ParticleDefinitionAdapter` are validation paths, not UI/data stubs.

## Threat Flags

None. The new trust boundary was already listed in the plan threat model and mitigated with `DataResult.error` validation plus bounded fixture testing.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for `10-02-PLAN.md`, which should update broader ownership docs and add static drift-prevention tests around this new runtime package.
- No auth gates or manual verification are required for this plan.

## Self-Check: PASSED

- Found all created/modified plan files on disk.
- Found task commits `1a0a4c9` and `3487254` in git history.
- Final JetBrains MCP compile/test verification passed.

---
*Phase: 10-schema-runtime-ownership-adapter*
*Completed: 2026-05-09*
