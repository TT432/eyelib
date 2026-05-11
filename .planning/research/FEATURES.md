# Feature Landscape

**Domain:** Multi-module Gradle structural cleanup (brownfield refactoring)
**Researched:** 2026-05-11

## Table Stakes

Features users expect from structural cleanup. Missing = cleanup feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Zero stale imports after any move | History: v1.3 Phase 15 established wildcard import elimination as gate; Phase 21 proved glob+grep zero-results pattern | Medium | Each move must include `grep` for old import patterns across ALL source files |
| Full project rebuild passes after each goal | History: Every v1.2/v1.3 phase gate required `jetbrain_build_project` with exit code 0 | Medium | Run G2 after EACH goal, not just at final gate |
| All 54 existing tests continue to pass | Regression prevention; tests are the safety net for module boundary correctness | Medium | Tests reference capability types, particle types, animation types — moves break test imports |
| MODULES.md updated within same commit | MODULES.md Update Rules: any module change requires same-commit docs update | Low | Applies to Goals 1, 2, 5, 6 |
| IDE project files (.idea/) consistent with Gradle | Every prior milestone required manual .idea file updates after settings.gradle changes | Medium | Goals 1, 2, 6 trigger IDE file updates |
| No split packages introduced | v1.3 established the `io.github.tt432.eyelibutil` namespace pattern — distinct namespace per module | Medium | Goal 1 (capability → attachment) is highest risk for this |

## Differentiators

Features that set this cleanup apart from previous milestones. Not expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Controller code dependency map (Goal 7 Part A) | First-time systematic mapping of bedrock controller code spread across 4 packages | High | 22+ files in animation/bedrock/controller/ + 4 in render/controller/ + importer schema |
| Plain-JVM → Forge module conversion (Goal 6) | eyelib-preprocessing may need to become Forge-aware to accept bake code | Medium | Requires adding `legacyForge` plugin, `mods.toml`, Forge dependency — unprecedented in prior milestones |
| Instrumentation subsystem full audit (Goal 5) | Understanding what breaks if instrument/db/ is deleted — 19 source files, 9 test files | Medium | First deletion of a whole subsystem; prior milestones only deleted individual classes |
| Documentation topology verification (Goal 8) | All 45+ READMEs verified against actual filesystem state | Medium | Prior milestones (v1.3 Phase 21) proved README drift is a recurring issue |

## Anti-Features

Features to explicitly NOT build.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| New Gradle module creation | 00-control-spec.md Non-Goals: "No further Gradle module split beyond current functional needs" | Move code to existing modules only (eyelib-attachment, eyelib-preprocessing) |
| Behavior changes during moves | 00-control-spec.md Forbidden Moves: "Mixing behavior changes with large package moves in one step" | Pure refactoring only; separate any necessary behavior changes into their own milestone |
| Opportunistic renaming of packages | 00-control-spec.md Non-Goals: "No opportunistic renaming of broad package areas without a documented destination" | Only rename if the destination is explicitly documented as a cleanup goal |
| Growing `Eyelib.java` with new accessors | 00-control-spec.md Forbidden Moves: explicitly listed | Use existing lookup seams (AnimationLookup, ModelLookup, etc.) |
| Keeping obsolete compatibility shells | 00-control-spec.md Forbidden Moves: explicitly listed | Delete after all internal callers migrated |
| Moving capability runtime wiring to attachment | CapabilityRuntimeHooks, Forge event subscriptions belong in root `mc/impl/` | Move only data/codec types to attachment; keep Forge wiring in root |
| Partial package moves (same package in two modules) | Split-package risk; classloader picks wrong one at runtime | Move entire packages or use new namespace in target module |

## Feature Dependencies

```
Goal 2 (rename processor → preprocessing) → Goal 6 (bake → preprocessing)
                                              Goal 3 (data classes → maybe preprocessing)

Goal 1 (capability → attachment) ← needs: capability audit first
                                     ← dep: Goal 7 Part A completed (controller analysis may reveal capability coupling)

Goal 3 (data classes) ← needs: criteria definition first
                       ← needs: per-class consumer audit

Goal 4 (invalid interfaces) ← needs: criteria definition first
                             ← needs: per-interface reference verification

Goal 5 (delete database code) ← needs: scope clarification (db/ only or instrument/?)
                                ← needs: confirmation no production code depends on instrument/

Goal 7 Part B (controller split) ← dep: Goal 7 Part A analysis complete

Goal 8 (README rewrite) ← dep: ALL other goals complete (describes final state)
```

## MVP Recommendation

Prioritize:
1. **Goal 2 (rename):** Unblocks Goal 6 and Goal 3; simplest structural change with highest cascading impact
2. **Goal 4 (invalid interfaces):** Quick win — deletion after verification; zero dependency on other goals
3. **Goal 6 (bake → preprocessing):** Middle complexity; tests the rename + move pattern end-to-end
4. **Goal 1 (capability → attachment):** Highest value cleanup (largest scope of moved code) but highest risk

Defer:
- Goal 7 Part B (controller split): Possible outcome is "keep in place" — don't invest in split unless Part A proves it's necessary
- Goal 5 decisions on scope: Need user input on whether to delete entire instrument subsystem or just database code

## Sources

- `PROJECT.md` v1.4 结构清理 goals (8 items)
- `MODULES.md` dependency graph (root → 7 submodules)
- `00-control-spec.md` Forbidden Moves and Non-Goals
- v1.2 MILESTONE-AUDIT.md: deferred PFUT-02, PFUT-03
- v1.3 MILESTONE-AUDIT.md: deferred CENT-F01, CENT-F02, AUDT-F01
- `src/test/` directory: 54 test files catalogued
- `src/main/java/io/github/tt432/eyelib/client/instrument/`: 19 source files catalogued
- `src/main/java/io/github/tt432/eyelib/client/animation/`: 40+ files catalogued
- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/`: 10+ files catalogued
