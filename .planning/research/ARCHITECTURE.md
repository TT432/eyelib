# Architecture Patterns

**Domain:** Multi-module Gradle structural cleanup (brownfield refactoring)
**Researched:** 2026-05-11

## Recommended Architecture: Phase-Based Structural Refactoring

```
┌─────────────────────────────────────────────────────────────────┐
│                     v1.4 结构清理 Phases                         │
├───────────────┬───────────────┬───────────────┬─────────────────┤
│ Phase A       │ Phase B       │ Phase C       │ Phase D+E       │
│ Analysis      │ Rename        │ Relocation    │ Cleanup+Docs    │
├───────────────┼───────────────┼───────────────┼─────────────────┤
│ Goal 4 ───────│ Goal 2 ───────│ Goal 6 ───────│ Goal 5 ─────────│
│ (audit intf)  │ (rename mod)  │ (bake→prep)   │ (delete db)     │
│               │               │               │                 │
│ Goal 7(PtA)───│               │ Goal 3 ───────│ Goal 7(PtB)─────│
│ (analyze ctl) │               │ (data class)  │ (split ctl?)    │
│               │               │               │                 │
│               │               │ Goal 1 ───────│ Goal 8 ─────────│
│               │               │ (cap→attach)  │ (READMEs)       │
└───────────────┴───────────────┴───────────────┴─────────────────┘
                          │
                    ┌─────▼─────┐
                    │  Phase F  │
                    │ Final Gate│
                    │ G2,G3,G4,G7
                    └───────────┘
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| **Analysis layer** (Goals 4, 7 Part A) | Identifies candidates for deletion/split WITHOUT modifying code | IDE refactoring tools (`ide_find_references`, `ide_find_implementations`); produces audit documents for downstream phases |
| **Rename layer** (Goal 2) | Atomically renames a Gradle module | `settings.gradle` (include), all `build.gradle` files (dependency declarations), `.idea/` XML files (module registry), filesystem (directory rename) |
| **Relocation layer** (Goals 1, 3, 6) | Moves source code between modules | IDE refactoring tools (`ide_move_file`), import rewriters, test files in same/other modules |
| **Deletion layer** (Goal 5) | Removes code and tests | IDE refactoring tools (`ide_refactor_safe_delete`), import checkers (to verify no orphaned references) |
| **Documentation layer** (Goal 8) | Rewrites READMEs to reflect final state | Filesystem verifier (`glob` for consistency), text search (`jetbrain_search_text` for stale references) |
| **Verification layer** (Phase F) | Runs all gates in sequence | JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`), ClientSmoke run configuration |

### Data Flow

```
1. IDE Index → ide_find_references → consumer catalog
2. Consumer catalog → plan document → defines move boundaries
3. Move boundaries → ide_move_file / ide_refactor_safe_delete → code changes
4. Code changes → jetbrain_sync_gradle_projects → IDE consistency
5. IDE consistency → jetbrain_build_project → compilation verification
6. Compilation OK → jetbrain_run_gradle_tasks(["test"]) → regression check
7. Tests OK → next goal / final gate
```

## Patterns to Follow

### Pattern 1: Audit-Before-Move (from v1.3 Phase 15)

**What:** Before moving any class, run `ide_find_references` with scope `project_and_libraries` to catalog ALL consumers. Document the consumer list. If any consumer is in a module that doesn't (and shouldn't) depend on the target module, abort the move.

**When:** Every class move in Goals 1, 3, 6.

**Example:**
```
1. ide_find_references on BrAcParticleEffectDefinition
2. Result: 4 consumers in root (BrControllerExecutor, BrAcStateDefinition, 
   BrAcStateTracksDefinition, BrAcStateParticleEffectsTrackDefinition)
3. Decision: All consumers are in root, target module is eyelib-*preprocessing
4. CHECK: Can root depend on eyelib-preprocessing? YES (already does via api)
5. PROCEED with move
```

### Pattern 2: Atomic Module Rename (synthesized from v1.2 Phase 8 + v1.3 Phase 16)

**What:** A Gradle module rename touches 5+ locations that must all be updated before the next Gradle sync. Perform all edits in one logical operation, then run `jetbrain_sync_gradle_projects`.

**When:** Goal 2 (eyelib-processor → eyelib-preprocessing).

**Example:**
```
Step 1: jetbrain_search_in_files_by_text for old name → catalog ALL references
Step 2: Update settings.gradle (include)
Step 3: Update root build.gradle (all 4 dependency declarations)
Step 4: Update all submodule build.gradle files (eyelib-importer)
Step 5: Update own build.gradle (archivesName)
Step 6: Update .idea/gradle.xml (module path)
Step 7: Update .idea/modules.xml (module entries)
Step 8: ide_move_file for directory rename
Step 9: jetbrain_sync_gradle_projects
Step 10: Verify with jetbrain_list_gradle_projects_detail
```

### Pattern 3: Gate-by-Gate Verification (from v1.3 Phase 21)

**What:** After each goal, run verification gates in a specific order: solo build → full build → NullAway → tests → import purge → file purge. A failure at any gate blocks progression to the next goal.

**When:** After EACH goal completion (not just at final gate).

**Example:**
```
Post-Goal-N:
  [ ] G2: jetbrain_build_project with rebuild=true → exit code 0?
  [ ] G5: jetbrain_search_text for old patterns → zero results?
  [ ] G6: jetbrain_find_files_by_glob for old paths → empty?
  [ ] G4: jetbrain_run_gradle_tasks ["test"] → exit code 0?
  [ ] If all pass → commit and proceed to Goal N+1
  [ ] If any fail → fix before proceeding
```

### Pattern 4: Namespace Separation for Module Boundaries (from v1.3)

**What:** When moving code from root to a submodule, use the submodule's existing namespace (e.g., `io.github.tt432.eyelibattachment.*`) rather than keeping the root namespace (`io.github.tt432.eyelib.*`). This prevents split packages.

**When:** Goal 1 (capability → attachment).

**Example:**
```
WRONG: Move io.github.tt432.eyelib.capability.EyelibAttachableData
       → eyelib-attachment: io.github.tt432.eyelib.capability.EyelibAttachableData
       (split package: same package in two modules)

RIGHT: Move io.github.tt432.eyelib.capability.EyelibAttachableData
      → eyelib-attachment: io.github.tt432.eyelibattachment.capability.EyelibAttachableData
      (distinct namespace; no split package)
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Move Then Audit

**What:** Moving a class to a new module, then running `ide_find_references` to find broken imports.
**Why bad:** Broken imports cascade — a single missed reference causes compilation failure across multiple files. Finding and fixing after the fact is 3x slower than auditing first.
**Instead:** Pattern 1 — audit before move. Consumer catalog → plan → move.

### Anti-Pattern 2: Partial Settings Update

**What:** Updating `settings.gradle` but forgetting to update root `build.gradle` dependency declarations (or vice versa).
**Why bad:** Gradle resolution will fail or produce wrong classpath. IDE sync will show mysterious errors.
**Instead:** Pattern 2 — atomic rename. Edit ALL files before running Gradle sync.

### Anti-Pattern 3: Documentation Before Code

**What:** Updating READMEs and MODULES.md to reflect a new module topology BEFORE the code moves are complete.
**Why bad:** If a move is reverted or changed, docs become stale. Also, docs written against planned state may describe things that don't exist yet.
**Instead:** Goal 8 is ALWAYS the last goal in the milestone.

### Anti-Pattern 4: Deleting Without Reference Check

**What:** Deleting an interface or class without checking `ide_find_implementations` or `ide_find_references`.
**Why bad:** Deleted code may be used via reflection (Mixin configs, Forge event annotations, annotation-based discovery). Compilation succeeds but runtime breaks.
**Instead:** Goal 4 audit — check references in ALL file types (`.java`, `.json`, `.toml`) before any deletion.

### Anti-Pattern 5: Flat Sequence (No Phases)

**What:** Executing all 8 goals in a flat list, each fully completed before the next starts.
**Why bad:** Goal dependencies create cascading rework. Goal 6 depends on Goal 2 (rename). Goal 8 depends on ALL others. Flat ordering causes Goal 6 to move code into `eyelib-processor`, then Goal 2 renames it, requiring Goal 6 consumers to update again.
**Instead:** Phase ordering — rename first, then moves, then docs.

## Scalability Considerations

| Concern | At 1 goal | At 4 goals | At 8 goals |
|---------|-----------|------------|------------|
| IDE sync stability | Stable after each Gradle sync | Must batch related edits to avoid sync failures | Phase-based batching: sync only at phase boundaries, not after each goal |
| Import cleanup complexity | `grep` + manual fix | Automated `ide_find_references` per move | Per-phase import audit: verify ALL goals in a phase before declaring done |
| Test regression surface | 54 tests, single goal impact ~5 tests | 54 tests, multi-goal impact ~20 tests | 54 tests, full milestone impact ~30 tests. Run full suite after each phase. |
| Documentation drift | 1-2 READMEs affected | 5-10 READMEs affected | 15+ READMEs affected. Only update in Phase E (Goal 8). |
| git commit granularity | 1 commit per goal | 1 commit per phase | 1 commit per phase (phase = logical unit of work) |

## Current Module Dependency Graph

```
                       ┌──────────────┐
                       │  clientsmoke │ (composite build, independent)
                       └──────────────┘
                              ↑
                       (only at runtime via localRuntime)
                              │
┌──────────────────────────────────────────────────────────────────┐
│                         root (eyelib)                             │
│  depends on: attachment, importer, material, molang, particle,    │
│              processor, util                                      │
└──────────────────────────────────────────────────────────────────┘
         │         │         │         │         │         │
         ▼         ▼         ▼         ▼         ▼         ▼
    ┌────────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌────────┐ ┌──────┐
    │attach..│ │import│ │materl│ │molang│ │particle│ │util  │
    │        │ │      │ │      │ │      │ │        │ │(leaf)│
    └───┬────┘ └──┬───┘ └──┬───┘ └──────┘ └───┬────┘ └──────┘
        │         │         │                   │
        │    ┌────▼────┐    │                   │
        │    │importer │    │                   │
        │    │depends: │    │                   │
        │    │molang,  │    │                   │
        │    │material │    │                   │
        │    └─────────┘    │                   │
        │                   │                   │
        ▼                   ▼                   ▼
    ┌────────┐                              ┌────────┐
    │util    │                              │util    │
    └────────┘                              └────────┘

    ┌─────────────┐
    │ processor   │ (plain JVM — depends on: importer, molang)
    │ (to become  │
    │  preprocessing)
    └─────────────┘
```

## Sources

- `docs/architecture/00-control-spec.md`: Execution rules, forbidden moves, rollback strategy
- `docs/architecture/01-module-boundaries.md`: Current-to-target ownership map, boundary rules
- `MODULES.md`: Module inventory with dependency directions
- `settings.gradle`: All 7 includes + composite build
- `build.gradle` (root): All dependency declarations between modules
- v1.2 MILESTONE-AUDIT.md: Phase ordering patterns for particle extraction
- v1.3 MILESTONE-AUDIT.md: Phase ordering patterns for utility extraction
- v1.3-ROADMAP.md: Phase-by-phase gate sequences (G1→G2→G3→G4 pattern)
