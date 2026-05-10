---
phase: 15-pre-migration-audit-routing
plan: 01
subsystem: docs
tags: [migration, utility-routing, ide-index, jetbrains-mcp]

requires:
  - phase: 14-verification-documentation-gate
    provides: v1.2 particle split completion baseline
provides:
  - Phase 15 utility/core-util routing manifest with inventory counts and evidence
  - Compatibility shim deletion timing for ListHelper and EitherHelper
  - ResourceLocations.mod() caller evidence for Phase 18 decision handling
affects: [phase-15, phase-17, phase-18, phase-19, phase-21, eyelib-util]

tech-stack:
  added: []
  patterns: [routing manifest, 0-1-N consumer classification, shim deletion plan]

key-files:
  created:
    - docs/architecture/migration/utility-routing-manifest.md
    - .planning/phases/15-pre-migration-audit-routing/15-01-SUMMARY.md
  modified: []

key-decisions:
  - "Kept Phase 15 as documentation-only for Plan 01: no source moves, no util deletions, and no Gradle shell usage."
  - "Recorded named roadmap classes as functional-owner routes even when current consumer evidence is zero/internal-only, matching the locked Phase 15 decision."

patterns-established:
  - "Every current root util/core-util Java source row records consumers, class, route, target phase, and evidence."
  - "Compatibility shims are routed to delete only after their canonical replacements are adopted in later phases."

requirements-completed: [AUDIT-01, ROUTE-02]

duration: ~45min
completed: 2026-05-10
---

# Phase 15 Plan 01: Utility Routing Manifest Summary

**Fresh root/core utility routing manifest with 32 root util rows, 5 core util rows, shim deletion timing, and ResourceLocations.mod() evidence.**

## Performance

- **Duration:** ~45 min
- **Started:** 2026-05-10
- **Completed:** 2026-05-10
- **Tasks:** 2/2
- **Files modified:** 2 created

## Accomplishments
- Created `docs/architecture/migration/utility-routing-manifest.md` with required sections and exact route table columns.
- Re-baselined current inventory with JetBrains file search: 32 root util Java files and 5 core util Java files.
- Cataloged `ListHelper` and `EitherHelper` as compatibility shims with canonical replacements and Phase 17/19 deletion timing.
- Recorded `ResourceLocations.mod()` as having zero source caller evidence and assigned final delete-vs-parameterize handling to Phase 18.
- Routed `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink` to functional owners rather than deletion, per locked Phase 15 decision.

## Task Commits

No commits were created because the user explicitly requested: “Do not commit changes.”

## Files Created/Modified
- `docs/architecture/migration/utility-routing-manifest.md` - Phase 15 routing contract for every current root/core util Java source file.
- `.planning/phases/15-pre-migration-audit-routing/15-01-SUMMARY.md` - Execution summary and verification evidence.

## Decisions Made
- Used JetBrains/IDE indexed file and text searches as evidence because `ide_ide_find_references` rejected available parameter combinations in this session.
- Preserved Plan 01 scope: no `:eyelib-util` scaffold, no source relocation, no source import cleanup, and no Gradle build needed for docs-only output.

## Deviations from Plan

None - plan executed as documentation-only routing work. The only note is evidence-tool fallback: semantic reference tooling was attempted but rejected parameters, so IDE indexed text/search evidence was used and documented in the manifest.

## Issues Encountered
- The worktree already contained extensive unrelated uncommitted changes before execution. I did not modify or stage unrelated files.
- `ide_ide_find_references` returned `Cannot specify both language+symbol and file+line+column` for attempted semantic reference calls. The manifest records this and instructs later move plans to re-run semantic references if the tool contract is corrected.

## Verification Evidence
- `jetbrain_search_file` for `src/main/java/io/github/tt432/eyelib/util/**/*.java` returned 32 current root util Java files.
- `jetbrain_search_file` for `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` returned 5 current core util Java files.
- Read-back verification confirmed required headings `## Route table` and `## Compatibility shim deletion plan` are present.
- JetBrains regex verification confirmed the manifest contains literal evidence strings for `ListHelper`, `EitherHelper`, `ResourceLocations.mod()`, `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink`.
- No Gradle command was run from shell; no Gradle build was necessary for docs-only changes.

## Known Stubs

None.

## Threat Flags

None.

## User Setup Required

None.

## Next Phase Readiness
- Plan 02 can use the manifest and existing wildcard-import findings to replace util wildcard imports with explicit imports.
- Plan 03 can use the functional-owner rows for the four named roadmap classes and should re-run semantic references before moving source files.

## Self-Check: PASSED

- Created files exist and were read back.
- Required manifest headings and named routing evidence strings were verified.
- Fresh inventory counts match the manifest baseline: 32 root util Java files + 5 core util Java files.

---
*Phase: 15-pre-migration-audit-routing*
*Completed: 2026-05-10*
