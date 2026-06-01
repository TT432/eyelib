# ADR-0001: Modular Architecture Control Spec

**Status:** Accepted  
**Context:** Eyelib is a bounded multi-project Forge project with one runtime root module plus focused functional subprojects. The codebase needs clear rules to govern refactoring work without causing architectural drift.  
**Decision:** Adopt a refactor control spec defining scope, non-goals, execution rules, and forbidden moves.  
**Consequences:** All structural changes must stay within the defined scope. Non-goals (full rewrite, opportunistic renaming, test-harness invention) are explicitly out of bounds.

---

# Eyelib Refactor Control Spec

## Scope
- Keep Eyelib as a bounded multi-project Forge project with one runtime root module plus focused functional subprojects.
- Improve navigability, boundary clarity, and maintainability for both humans and AI.

## Non-Goals
- No full architecture rewrite.
- No further Gradle module split beyond current functional needs unless a human explicitly asks for it.
- No opportunistic renaming of broad package areas without a documented destination.
- No test-harness invention beyond targeted helper tests when pure Java seams appear.

## Execution Rules
- Prefer compatibility shells and facades before breaking direct callers.
- Keep commits bounded to one narrow seam at a time.
- Preserve existing manager, loader, visitor, and codec patterns.

## Forbidden Moves
- Editing unrelated in-flight Java changes.
- Mixing behavior changes with large package moves in one step.
- Treating generated code as a normal handwritten subsystem.
- Growing `Eyelib.java` with new singleton reach-through accessors.
