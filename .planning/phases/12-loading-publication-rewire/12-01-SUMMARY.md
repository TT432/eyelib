---
phase: 12-loading-publication-rewire
plan: 01
subsystem: particle-loading
tags: [java, gradle, junit5, particle, loading, publication]

requires:
  - phase: 11-runtime-client-core-extraction
    provides: module-owned particle runtime definitions and executable runtime/client integration
provides:
  - module-owned active ParticleDefinition registry and ParticlePublisher access
  - importer-schema JSON publication service keyed by ParticleDefinition.identifier()
  - load report with processed sources, published identifiers, failures, and duplicates
affects: [phase-12-loading-publication-rewire, phase-13-command-network-integration-rewire, phase-14-verification-documentation-gate]

tech-stack:
  added: []
  patterns: [ParticlePublisher publication seam, importer BrParticle codec parsing, DataResult conversion reporting, LinkedHashMap replacement order]

key-files:
  created:
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleDefinitionRegistry.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java
    - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleLoadReport.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublicationTest.java
    - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleLoadingBoundaryTest.java
  modified: []

key-decisions:
  - "Module loading publication stores active entries by ParticleDefinition.identifier(), while source ids remain diagnostics/report metadata."
  - "Invalid resource JSON/schema conversion failures are logged and reported without blocking valid replacement entries."

patterns-established:
  - "Source-keyed JSON maps are converted to ordered ParticleDefinition values before publication through ParticleDefinitionRegistry.publisher()."
  - "Pure loading package tests scan actual import declarations to keep loading/** root/MC/Forge-clean."

requirements-completed: [PLOAD-01, PLOAD-02]

duration: 7min
completed: 2026-05-09
---

# Phase 12 Plan 01: Module-Owned Particle Loading Publication Summary

**Importer-schema particle JSON publication into a module-owned active registry keyed only by description identifiers**

## Performance

- **Duration:** 7 min
- **Started:** 2026-05-09T11:33:57Z
- **Completed:** 2026-05-09T11:40:13Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Added RED tests for source-key rejection, full replacement, valid replacement order, duplicate identifier reporting, invalid-resource resilience, and loading package boundary cleanliness.
- Added `ParticleDefinitionRegistry` with module-owned `ParticleStore<ParticleDefinition>` and `ParticlePublisher<ParticleDefinition>` keyed by `ParticleDefinition::identifier`.
- Added `ParticleResourcePublication.replaceFromJsonResources(...)` to parse importer `BrParticle.CODEC`, convert through `ParticleDefinitionAdapter`, log/report failures, and publish valid runtime definitions.

## Task Commits

Each task was committed atomically:

1. **Task 1: Specify module-owned publication behavior** - `9f844b8` (test)
2. **Task 2: Implement module registry and publication service** - `48c7ae3` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD tasks used separate RED and GREEN commits._

## Files Created/Modified
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleDefinitionRegistry.java` - Module-owned active particle definition store and publisher access.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java` - Source-keyed JSON parse/convert/report/publish service.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleLoadReport.java` - Immutable reload publication report with failures and duplicate identifiers.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublicationTest.java` - Publication behavior coverage for PLOAD-01/PLOAD-02.
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleLoadingBoundaryTest.java` - Static boundary guard for loading package imports.

## Verification

- **RED gate:** JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest --tests io.github.tt432.eyelibparticle.loading.ParticleLoadingBoundaryTest` failed before implementation because `ParticleDefinitionRegistry`, `ParticleResourcePublication`, and `ParticleLoadReport` did not exist (External task id 306).
- **GREEN gate:** JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest --tests io.github.tt432.eyelibparticle.loading.ParticleLoadingBoundaryTest --tests io.github.tt432.eyelibparticle.api.ParticlePublisherTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest` exited 0 (External task id 309).
- **Acceptance checks:** Grep/file checks confirmed required test names, registry publisher patterns, importer `BrParticle` usage, `ParticleDefinitionAdapter::fromSchema`, absence of root legacy/MC/Forge imports in `ParticleResourcePublication`, and report accessors for published/failed identifiers.

## Decisions Made
- Active registry ownership for Plan 01 lives in `eyelibparticle.loading.ParticleDefinitionRegistry`, not root manager/registry code.
- Source ids are retained only in `ParticleLoadReport.processedSourceIds()` and failure logging/reporting; active store keys come exclusively from converted runtime definitions.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Initial GREEN compile failed because the local DataFixerUpper `DataResult` error type is not named `DataResult.Error`; fixed by reading the error object via a lambda and re-running the targeted JetBrains MCP test successfully.
- Initial boundary test path assumed Gradle ran from the repository root; fixed by locating `MODULES.md` from `user.dir`, matching existing particle boundary test patterns.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for `12-02-PLAN.md`: root reload/registry/lookup/tooling/spawn compatibility adapters can now delegate loading publication into the module-owned registry and publication service.

## Self-Check: PASSED

- Created files exist: `ParticleDefinitionRegistry.java`, `ParticleResourcePublication.java`, `ParticleLoadReport.java`, `ParticleResourcePublicationTest.java`, `ParticleLoadingBoundaryTest.java`, and this summary.
- Task commits exist: `9f844b8` and `48c7ae3`.
- Targeted JetBrains MCP verification passed in External task id 309.

---
*Phase: 12-loading-publication-rewire*
*Completed: 2026-05-09*
