# Eyelib Molang Slice Plan - Phase 4 Callable Bind-Link Bridge

## Goal
- Add an additive callable-side bind-link bridge for engine-local built-ins in `:eyelib-molang`.
- First migrated subset is limited to engine-local built-ins under `MolangMath` and `MolangToplevel`.
- Keep the current generated-parser-backed compile path and root `mc/impl/molang/**` runtime bindings untouched.

## Why This Slice
- Current production execution still flows through `MolangValue -> MolangCompileHandler -> MolangCompileVisitor`.
- Query-side bind-link contract/linker work already exists as additive refactor scaffolding.
- The next safe architectural step is to mirror that query-side work for callable discovery/link payloads without selecting winners or altering runtime execution ownership.

## Roadmap / Phase
- Primary phase: Phase 4 - Host and query bridge.
- Acceptance must remain inside additive migration rules from `eyelib-molang/ROADMAP.md` and `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`.

## Slice Boundaries

### Allowed paths
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/**`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/**`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/**`
- `eyelib-molang/ROADMAP.md` only if evidence, phase wording, or implementation posture actually changes

### Forbidden paths
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/**`
- `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**`
- Any Phase 6 policy/specialization/cache/reporting destinations

## Non-Negotiable Constraints
- No winner selection in the linker.
- No fallback semantics hidden in the linker.
- No runtime execution change.
- No cache identity or cutover behavior.
- `MolangOwnerSet` remains a compatibility seam; do not remove or demote through runtime rewiring in this slice.
- If roadmap wording does not materially change, leave `ROADMAP.md` untouched.

## Expected Deliverables
1. Design brief identifying the callable-side contract shape and the minimal migrated callable subset.
2. Implementation that extends binder/link outputs for callable link requests/results.
3. Contract tests proving:
   - stable candidate-set ref and registry-version ref exposure,
   - preserved symbolic callable name and visible call shape,
   - required host-role metadata exposure,
   - loud failure for unresolved callable names,
   - loud failure for ambiguous equal-specificity/equal-priority ties.
4. Review evidence confirming no runtime/cutover leakage.

## Design -> Implement -> Review Subtasks

### Task A - Design
- Produce a concise design note for callable-side bind-link shape.
- Name touched files, invariants, and exact failure modes.
- Decide whether to extend existing query-side contracts or introduce parallel callable-specific contract types.
- Prefer the shape that keeps query and callable contracts symmetrical without conflating them.

#### Task A QA Scenario
- Tool: `Read`.
- Steps:
  1. Read the produced design note file.
  2. Confirm it explicitly names the migrated callable subset (`MolangMath`, `MolangToplevel`).
  3. Confirm it names allowed paths, forbidden paths, invariants, and loud-failure cases.
  4. Confirm it states that linker winner selection, runtime execution changes, and cutover behavior are out of scope.
- Expected results:
  - A single design note exists and is readable.
  - The note contains the callable subset, contract shape decision, touched files, forbidden files, invariants, and verification command.
  - The note does not authorize edits to `generated/**`, `MolangCompileHandler.java`, `MolangCompileVisitor.java`, or root `mc/impl/molang/**`.

### Task B - Implement
- Add the minimal binder/link data path for callable requests/results.
- Limit migrated coverage to engine-local built-ins (`MolangMath`, `MolangToplevel`).
- Update or add tests before/with implementation.
- Update roadmap only if implementation posture/evidence truly advances the recorded Phase 4 state.

#### Task B QA Scenario
- Tool: JetBrains Gradle tooling (`jetbrain_run_gradle_tasks`).
- Steps:
  1. Run `:eyelib-molang:test` through JetBrains/IDE Gradle tooling.
  2. Inspect test output for callable bind-link contract coverage and ensure the new/updated callable bind-link tests pass.
  3. If `ROADMAP.md` changed, read the changed section and confirm the update only records actual evidence/posture changes from this slice.
- Expected results:
  - Gradle task exits with code `0`.
  - Callable-side bind-link tests pass alongside pre-existing `:eyelib-molang` tests.
  - No broader runtime, cutover, or root-module verification is required for this Phase 4 additive slice.

### Task C - Review
- Check allowed-path compliance.
- Check that compile/runtime entrypoints are untouched.
- Check that the linker is not selecting winners.
- Check that verification ran via IDE/JetBrains Gradle tooling.
- Check whether roadmap impact was handled correctly.

#### Task C QA Scenario
- Tools: `Read`, `Glob`, and git diff/status via shell if needed only for review evidence.
- Steps:
  1. Inspect changed files and confirm every modified path is inside the allowed path list.
  2. Confirm no changes landed in `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/**`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`, or `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**`.
  3. Read the linker/contract changes and confirm they expose candidate-set metadata without selecting winners or adding fallback runtime semantics.
  4. Confirm verification evidence shows JetBrains/IDE Gradle execution with exit code `0`.
- Expected results:
  - Changed files stay within allowed paths.
  - Forbidden runtime/generated/root binding paths remain untouched.
  - The linker remains a bind-link handoff layer rather than a specialization/runtime dispatcher.
  - Verification evidence is present and green.

## Verification
- Required command: `:eyelib-molang:test` via IDE/JetBrains Gradle tooling.
- Do not run Gradle from shell.

## Hidden Failure Mode To Watch
- The callable linker quietly starts doing specialization or runtime winner selection, creating a shadow execution system before Phase 5 defines execution ownership.

## Handoff Rules
- Fresh subagents only; do not reuse task sessions.
- Subagents should keep outputs concise and prefer file-backed handoff notes over large pasted code blocks.
