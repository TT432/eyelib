---
phase: 10-schema-runtime-ownership-adapter
plan: 02
subsystem: particle-runtime-ownership-docs
tags: [java, gradle, particle, schema, documentation, junit, boundary-tests]

requires:
  - phase: 10-schema-runtime-ownership-adapter
    provides: ParticleDefinition and ParticleDefinitionAdapter runtime schema seam
provides:
  - Canonical owner documentation for importer BrParticle, particle ParticleDefinition, and legacy root BrParticle
  - Documentation invariant test for owner, mapped-field, and deferred-phase drift
  - Boundary source-scan test preventing duplicate particle-module BrParticle and root/MC/Forge imports
affects: [phase-11-runtime-client-core-extraction, phase-12-loading-publication-rewire, phase-13-command-network-integration-rewire, phase-14-verification-documentation-gate]

tech-stack:
  added: []
  patterns: [JUnit static source scan, documentation invariant tests, root-clean particle module boundary]

key-files:
  created:
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionDocumentationTest.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java
  modified:
    - MODULES.md
    - docs/architecture/01-module-boundaries.md
    - docs/architecture/02-side-boundaries.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java
    - src/main/java/io/github/tt432/eyelib/client/particle/README.md

key-decisions:
  - "Document importer BrParticle as canonical raw schema/codec owner and particle ParticleDefinition as canonical module runtime definition owner."
  - "Keep root client particle bedrock BrParticle documented as legacy/non-canonical until Phase 11/12 migration."
  - "Use JUnit source scans, not shell grep gates, to prevent duplicate BrParticle and forbidden import drift."

patterns-established:
  - "Documentation invariant tests read repository docs from the discovered project root and assert exact ownership/mapped-field phrases."
  - "Particle boundary tests scan all :eyelib-particle main Java sources for duplicate BrParticle declarations and root/MC/Forge imports."

requirements-completed: [PSCHEMA-01, PSCHEMA-02, PSCHEMA-03]

duration: 6min
completed: 2026-05-09
---

# Phase 10 Plan 02: Ownership Documentation and Drift Tests Summary

**Particle schema/runtime ownership is now documented across module and architecture docs, with JUnit drift tests preventing duplicate BrParticle ownership and root/MC/Forge contamination.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-05-09T06:50:28Z
- **Completed:** 2026-05-09T06:56:22Z
- **Tasks:** 2/2 completed
- **Files modified:** 8 plan files

## Accomplishments

- Updated module, architecture, particle-module, runtime-package, and root-particle documentation to name importer `BrParticle` as canonical raw schema/codec owner, particle `ParticleDefinition` as canonical module runtime definition owner, and root `client/particle/bedrock/BrParticle` as legacy/non-canonical.
- Documented the allowed particle -> importer dependency for `ParticleDefinitionAdapter`, mapped parity fields, and explicit Phase 11/12/13 deferrals so Phase 10 cannot be mistaken for runtime/loading/command/network migration.
- Added `ParticleDefinitionDocumentationTest` to lock owner, mapped-field, and deferral wording.
- Added `ParticleDefinitionBoundaryTest` to scan all `:eyelib-particle` main Java sources for duplicate `record BrParticle` / `class BrParticle` declarations and forbidden root, Minecraft, or Forge imports.

## Task Commits

1. **Task 1: Document canonical owners and legacy root status** — `8ffd223` (`docs`)
2. **Task 2: Add boundary and documentation drift tests** — `4ca5918` (`test`)

**Plan metadata:** committed after summary creation.

## Files Created/Modified

- `MODULES.md` — names importer/raw schema ownership, particle runtime definition ownership, root legacy status, and deferred later phases.
- `docs/architecture/01-module-boundaries.md` — updates particle/importer ownership map and mapped-field notes.
- `docs/architecture/02-side-boundaries.md` — records the allowed adapter dependency and root-clean particle side rule.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` — documents current Phase 10 particle-module responsibility and deferrals.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` — expands runtime package docs with mapped fields and deferrals.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` — marks root `BrParticle` as legacy/non-canonical and names canonical owners.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionDocumentationTest.java` — asserts documentation invariants.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` — asserts boundary and duplicate-ownership invariants.

## Decisions Made

- Kept Phase 10 limited to docs and boundary tests; no runtime, loader, command, or network behavior was moved.
- Locked exact documentation phrases for canonical owners, mapped fields, and Phase 11/12/13 deferrals so future drift fails in JUnit.
- Allowed importer schema imports only in the documented adapter/runtime seam while continuing to reject root runtime, Minecraft, and Forge imports in `:eyelib-particle` main sources.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed documentation invariant wording mismatch**
- **Found during:** Task 1 (`ParticleDefinitionDocumentationTest` verification)
- **Issue:** The first targeted documentation test run failed because the side-boundary wording used “allows” instead of the asserted “allowed” phrase, and two docs split or varied the exact mapped-field phrase.
- **Fix:** Standardized the exact “allowed particle -> importer dependency for ParticleDefinitionAdapter” and mapped-field wording across the side-boundary doc, runtime package docs, and root particle README.
- **Files modified:** `docs/architecture/02-side-boundaries.md`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`
- **Verification:** JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionDocumentationTest` exited 0 after the fix.
- **Committed in:** `8ffd223`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** The fix only aligned documentation/test invariant wording; scope remained docs and boundary tests.

## Issues Encountered

- Task 1’s first documentation invariant run failed on an exact phrase mismatch; the docs were updated and the targeted test passed.
- Task 2 was marked `tdd="true"`, but the static boundary invariant was already satisfied by existing main sources once the new test was added. The test still provides the required future drift-prevention gate.

## TDD Gate Compliance

- `test(10-02)` commit `4ca5918` exists for the Task 2 boundary test.
- No separate RED commit exists because this task adds a static drift-prevention test over already-compliant source files; forcing a failure would require intentionally corrupting source or test expectations outside the plan’s docs/boundary-test scope.

## Verification

- JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionDocumentationTest` — exitCode 0 after Task 1.
- JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionBoundaryTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionDocumentationTest` — exitCode 0 after Task 2.
- JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :compileJava` — exitCode 0 for the Plan 01-02 wave gate.

## Known Stubs

None. This plan added documentation and static tests only; no UI/data stubs were introduced.

## Threat Flags

None. The documentation drift and particle-module source-boundary trust surfaces were already listed in the plan threat model and are mitigated by JUnit checks.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 10 is complete: importer/raw schema ownership, particle runtime definition ownership, adapter field expectations, and root legacy status are documented and tested.
- Ready for Phase 11 runtime client core extraction planning; the boundary tests will fail if extraction contaminates pure `:eyelib-particle` sources with root, Minecraft, or Forge imports.

## Self-Check: PASSED

- Found created test files on disk: `ParticleDefinitionDocumentationTest.java` and `ParticleDefinitionBoundaryTest.java`.
- Found task commits `8ffd223` and `4ca5918` in git history.
- Final JetBrains MCP targeted tests and wave compile/test gate passed.

---
*Phase: 10-schema-runtime-ownership-adapter*
*Completed: 2026-05-09*
