# Molang Refactor Subagent Handoff

## Scope

- Target: `:eyelib-molang` refactor work only.
- Current source of truth: `eyelib-molang/ROADMAP.md`.
- Detailed gates: `eyelib-molang/refactor-plan/README.md` and phase files.
- Supervisor role: split work, assign subagents, enforce design -> implementation -> review, and verify results.

## Baseline

- `./gradlew :eyelib-molang:test` passed before new slice assignment.
- Current roadmap status: phases 1 and 2 are partial, phase 3 has a minimal slice, phases 4 through 6 remain blocked by documented gates.
- Visible concrete implementation gap: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java` still documents incomplete `this` semantics.

## Assignment Protocol

1. Design subagent identifies affected files, pass/fail criteria, required tests, and documentation impact.
2. Implementation subagent makes only the scoped change and runs `./gradlew :eyelib-molang:test`.
3. Review subagent checks code, tests, roadmap impact, generated-code boundaries, and verification evidence.
4. Supervisor verifies modified files and Gradle evidence before assigning the next slice.

## Next Reviewable Slices

### Slice A - `this` semantics gap

- Status: implemented, reviewed, and verified as explicit Phase 5-deferred compatibility fallback.
- Affected starting path: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`.
- Required gate: keep generated parser artifacts read-only.
- Verification: `./gradlew :eyelib-molang:test`.
- Documentation: update `eyelib-molang/ROADMAP.md` if the gap is resolved or explicitly deferred.

### Slice B - Phase 3 binder widening

- Status: pending after Slice A or separate design approval.
- Starting paths: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/`, `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/`, and corpus resources.
- Verification: `./gradlew :eyelib-molang:test`.
- Documentation: update roadmap/phase plan if supported families, diagnostics modes, or deferred reasons change.

### Slice C - Phase 4 gate decisions

- Status: decision recorded; first host-publication determinism/conflict contract test slice implemented, reviewed, and verified. Additional Phase 4 test/implementation slices remain pending.
- Starting path: `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`.
- Verification: referenced paths resolve; no code verification unless implementation starts.
- Documentation: update `eyelib-molang/ROADMAP.md` if any blocked decision moves to current/decided.
