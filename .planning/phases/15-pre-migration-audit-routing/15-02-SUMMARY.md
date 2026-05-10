---
phase: 15-pre-migration-audit-routing
plan: 02
subsystem: code-quality
tags: [java, imports, codec, audit]

requires:
  - phase: 15-pre-migration-audit-routing
    provides: Utility migration audit context and explicit-import requirement for AUDIT-02
provides:
  - Explicit util/codec imports in BrAnimationEntry.java
  - Explicit Tuple nested imports and Pair import in TupleCodec.java
affects: [phase-15, phase-19-codec-migration, eyelib-util-routing]

tech-stack:
  added: []
  patterns: [explicit-java-imports, jetbrains-diagnostics-verification]

key-files:
  created:
    - .planning/phases/15-pre-migration-audit-routing/15-02-SUMMARY.md
  modified:
    - src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java
    - src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java

key-decisions:
  - "Preserved codec/runtime logic and limited edits to import declarations only."
  - "Used explicit Tuple nested imports rather than qualifying each tuple reference to keep TupleCodec signatures readable."

patterns-established:
  - "Replace pre-migration util wildcard imports with explicit owner imports before package moves."

requirements-completed: [AUDIT-02]

duration: "~15 min"
completed: 2026-05-10
---

# Phase 15 Plan 02: Replace Util Wildcard Imports Summary

**Bedrock animation and tuple codec imports are explicit while preserving existing codec behavior for the upcoming eyelib-util migration.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-05-10T16:20:00Z
- **Completed:** 2026-05-10T16:36:30Z
- **Tasks:** 2/2
- **Files modified:** 3

## Accomplishments

- Replaced `BrAnimationEntry.java` wildcard imports from `com.mojang.serialization`, `client.animation`, `client.model`, `eyelib.util`, `eyelib.util.codec`, importer animation, Molang, fastutil, jspecify, and `java.util` with explicit imports.
- Replaced `TupleCodec.java` wildcard imports with `Pair` plus explicit `Tuple.T2`-`Tuple.T16` and `Tuple.Function3`-`Tuple.Function16` nested imports.
- Verified the Phase 15 util wildcard regex returns zero production-source matches and both touched Java files have no IDE-reported errors.

## Task Commits

No commits were created, per user instruction: "Do not commit changes."

## Files Created/Modified

- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` - import declarations are explicit; codec bodies and runtime methods are unchanged.
- `src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java` - `Pair` and Tuple nested type/function dependencies are explicit; tuple arity behavior is unchanged.
- `.planning/phases/15-pre-migration-audit-routing/15-02-SUMMARY.md` - execution evidence and completion summary.

## Decisions Made

- Used explicit nested imports in `TupleCodec.java` instead of qualifying every `Tuple.Tn`/`Tuple.FunctionN` reference, preserving readable generic method signatures.
- Did not run shell Gradle and did not modify state/roadmap files because the user explicitly prohibited commits and requested scoped execution output.

## Deviations from Plan

None - plan executed exactly as written, with the exception that commits/state updates were intentionally skipped to satisfy the user's explicit requirements.

## Issues Encountered

- The worktree already contains many unrelated uncommitted modifications/deletions and untracked files. They were left untouched.

## Verification Evidence

| Check | Tool | Result |
|------|------|--------|
| `BrAnimationEntry.java` util wildcard scan: `import\\s+io\\.github\\.tt432\\.eyelib\\.util(?:\\.[A-Za-z0-9_]+)*\\.\\*;` | `jetbrain_search_regex` | PASS — zero matches |
| `TupleCodec.java` `Tuple.*` wildcard scan: `import\\s+io\\.github\\.tt432\\.eyelib\\.util\\.codec\\.Tuple\\.\\*;` | `jetbrain_search_regex` | PASS — zero matches |
| Production source util wildcard scan: `import\\s+io\\.github\\.tt432\\.eyelib\\.util(?:\\.[A-Za-z0-9_]+)*\\.\\*;` over `src/main/java/**/*.java` | `jetbrain_search_regex` | PASS — zero matches |
| `BrAnimationEntry.java` diagnostics | `ide_ide_diagnostics` with `severity=errors` | PASS — `problemCount: 0` |
| `TupleCodec.java` diagnostics | `ide_ide_diagnostics` with `severity=errors` | PASS — `problemCount: 0` |
| Targeted Java build of the two touched files | `jetbrain_build_project(filesToRebuild=[...])` | PASS — `isSuccess: true`; IDE reported limited build message collection only |
| Explicit `ChinExtraCodecs` import | `jetbrain_search_regex` | PASS — line 9 match found |
| Explicit `CodecHelper` import | `jetbrain_search_regex` | PASS — line 10 match found |
| Explicit DataFixers `Pair` import and no `com.mojang.datafixers.util.*` | `jetbrain_search_regex` | PASS — `Pair` found; wildcard zero matches |

## Known Stubs

None.

## Threat Flags

None.

## Next Phase Readiness

- Ready for Phase 15 Plan 03 single-consumer routing work.
- `BrAnimationEntry.java` and `TupleCodec.java` no longer hide util dependencies behind wildcard imports.

## Self-Check: PASSED

- Modified source files exist at the planned paths.
- Summary file created at `.planning/phases/15-pre-migration-audit-routing/15-02-SUMMARY.md`.
- All planned automated verification checks above passed.

---
*Phase: 15-pre-migration-audit-routing*
*Completed: 2026-05-10*
