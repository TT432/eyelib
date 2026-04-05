# Eyelib Refactor Control Spec

## Scope
- Apply the staged repository review and refactor plan in `docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md`.
- Keep Eyelib as a single-module Forge project.
- Improve navigability, boundary clarity, and maintainability for both humans and AI.

## Stage Goals
- Stage 0-1: establish root guidance, boundary docs, and navigation indexes.
- Stage 2-3: declare ownership seams and isolate generated Molang code.
- Stage 4-10: extract hotspots behind narrower services, then tighten runtime boundaries and clean up.

## Non-Goals
- No full architecture rewrite.
- No module split in Gradle.
- No opportunistic renaming of broad package areas without a documented destination.
- No test-harness invention beyond targeted helper tests when pure Java seams appear.

## Execution Rules
- Update docs before broad structural moves.
- Prefer compatibility shells and facades before breaking direct callers.
- Keep commits and changes bounded to one stage or one narrow seam at a time.
- Preserve existing manager, loader, visitor, and codec patterns unless a stage explicitly replaces one usage with a clearer local boundary.

## Temporary Shims Allowed
- Thin wrapper classes are allowed only when they are part of an actively executing migration stage and have an explicit deletion point in the current plan.
- Transitional package-local delegators are allowed for runtime-sensitive code only when they reduce risk during the same stage that removes the old caller path.
- Long-lived compatibility facades are not a goal for the current breaking refactor.

## Forbidden Moves
- Editing unrelated in-flight Java changes.
- Mixing behavior changes with large package moves in one step.
- Treating generated code as a normal handwritten subsystem.
- Growing `Eyelib.java` with new singleton reach-through accessors.
- Keeping obsolete compatibility shells after all internal callers have been migrated away from them.

## Rollback Strategy
- If a stage introduces unclear ownership or breaks compile verification, revert that stage’s local changes before proceeding.
- Re-document the boundary with a simpler seam, then retry with a smaller change set.
