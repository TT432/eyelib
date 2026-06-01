# ADR-0004: Generated Code Policy

**Status:** Accepted  
**Context:** The Molang parser generates code that should be visually and structurally distinct from handwritten compiler/runtime code to prevent accidental edits.  
**Decision:** Isolate generated parser artifacts under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`. Do not hand-edit generated files during normal work. Regeneration requires a clearly scoped task.  
**Consequences:** Contributors and AI assistants can distinguish generated from handwritten code at a glance. The root `MolangQuery.java` stays root-coupled.

---

# Eyelib Generated Code Policy

## Current Generated Zone
- Generated Molang parser artifacts now live under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`.
- Root `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` is the only file remaining in the root `molang/` path — it holds root-coupled query functions that cannot move to `eyelib-molang`.

## Rules
- Do not hand-edit generated parser files during normal feature or refactor work.
- If parser output needs to change, treat it as a regeneration task with clearly scoped review.
- Handwritten compiler/runtime code should depend on the generated package through the smallest surface practical for the current stage.

## This Stage's Goal
- Make generated parser artifacts visually and structurally distinct from handwritten Molang compiler/runtime code.
- Reduce the chance that contributors or AI assistants open generated files by default when investigating normal Molang logic.
