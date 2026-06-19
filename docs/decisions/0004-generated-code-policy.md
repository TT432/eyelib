# ADR-0004: Generated Code Policy

**Status:** Superseded (ANTLR removed) — **Also amended by [ADR-0014](0014-flat-merge.md)** (`eyelib-molang/` 子项目不再存在,路径 `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` 已失效)  
**Context:** The Molang parser generates code that should be visually and structurally distinct from handwritten compiler/runtime code to prevent accidental edits.  
**Decision:** Isolate generated parser artifacts under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`. Do not hand-edit generated files during normal work. Regeneration requires a clearly scoped task.  
**Consequences:** Contributors and AI assistants can distinguish generated from handwritten code at a glance. The root `MolangQuery.java` stays root-coupled.

---

# Eyelib Generated Code Policy

## Historical Status

This policy applied when ANTLR-generated parser artifacts existed under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`. As of the ANTLR removal refactor, that directory and all generated artifacts have been deleted. The handwritten recursive-descent parser is now the sole frontend.

## Rules (Historical — No Longer Applicable)

- ~~Do not hand-edit generated parser files during normal feature or refactor work.~~
- ~~If parser output needs to change, treat it as a regeneration task with clearly scoped review.~~
- ~~Handwritten compiler/runtime code should depend on the generated package through the smallest surface practical for the current stage.~~

## Current State

- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` has been removed.
- `GeneratedMolangParserFrontend.java` has been removed.
- `GeneratedParserBackedAstMolangParserFrontend.java` has been removed.
- The `antlr4-runtime` dependency has been removed from `build.gradle`.
- The handwritten parser is the sole frontend; all parser work is AST-driven.

## Remaining References

- Root `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` is the only file in the root `molang/` path — it holds root-coupled query functions that cannot move to `eyelib-molang`.
