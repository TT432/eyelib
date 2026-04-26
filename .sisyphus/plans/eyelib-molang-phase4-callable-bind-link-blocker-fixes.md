# Eyelib Molang Slice Plan - Phase 4 Callable Bind-Link Blocker Fixes

## Goal
- Repair the reviewed Phase 4 callable bind-link slice so it passes independent review without widening scope.
- Keep the fix limited to binder emission gating, one binder regression test path, and roadmap evidence alignment.

## Why This Slice Exists
- Review found that `MolangBinder` can enqueue invalid callable bind-link requests when symbolic callable-name extraction fails.
- Review also found missing binder-level regression coverage for that path.
- Review/context mining found `eyelib-molang/ROADMAP.md` evidence drift because Phase 4 bind-link evidence still reads as query-only after callable bind-link contract coverage landed.

## Scope

### Allowed paths
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java`
- `eyelib-molang/ROADMAP.md`

### Forbidden paths
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/**`
- `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**`
- Any Phase 5/6 execution, specialization, cache, or cutover files

## Required Outcome
1. `MolangBinder` must not enqueue a callable bind-link request when symbolic callable-name extraction returns blank.
2. Existing linker validation remains unchanged as the hard malformed-input guard.
3. `MolangCallableBindLinkContractTest` gains a binder regression proving malformed non-query callee shapes stay out of `callableBindLinkRequests`.
4. `ROADMAP.md` updates Phase 4 evidence text anywhere it still claims query-only bind-link coverage.

## Design Constraints
- Do not redesign the callable bind-link contract or linker.
- Do not add new diagnostics behavior unless absolutely required by the existing binder style; default is silent skip of malformed callable bind-link emission.
- Do not widen callable subset coverage.
- Do not change verification commands.

## Subtasks

### Task A - Design
- Produce a concise design brief for the blocker-fix slice.
- Name the exact binder guard change, the regression-test shape, and the exact `ROADMAP.md` sections to edit.

#### Task A QA Scenario
- Tool: `Read`.
- Steps:
  1. Read the design brief file.
  2. Confirm it limits code edits to the three allowed files.
  3. Confirm it explicitly keeps linker validation unchanged.
  4. Confirm it names the roadmap sections that need evidence-text updates.
- Expected results:
  - The brief is readable and concrete.
  - The brief does not authorize changes outside the allowed files.

### Task B - Implement
- Add the binder emission guard.
- Add the binder regression test in `MolangCallableBindLinkContractTest`.
- Update `ROADMAP.md` evidence text in all relevant Phase 4 sections.

#### Task B QA Scenario
- Tool: JetBrains Gradle tooling (`jetbrain_run_gradle_tasks`) plus file inspection.
- Steps:
  1. Run `:eyelib-molang:test` via IDE Gradle tooling.
  2. Confirm the new binder regression test passes.
  3. Read the changed `ROADMAP.md` sections and confirm they now mention callable bind-link evidence without changing unrelated phase status or rules.
- Expected results:
  - Gradle exits `0`.
  - Binder regression coverage is present and passing.
  - Roadmap wording reflects actual callable Phase 4 evidence and nothing broader.

### Task C - Review
- Verify the fix resolves the original blockers and does not reopen phase-boundary leakage.

#### Task C QA Scenario
- Tools: `Read`, `Glob`, git diff/status via shell if needed for review evidence.
- Steps:
  1. Confirm only the three allowed files changed in this fix slice.
  2. Confirm `MolangBinder` now skips callable-request emission when symbolic extraction is blank.
  3. Confirm linker files remain untouched.
  4. Confirm `ROADMAP.md` updates are limited to Phase 4 evidence wording.
- Expected results:
  - Diff stays within allowed paths.
  - Binder no longer feeds malformed requests into the callable bind-link seam.
  - Roadmap drift is resolved without expanding scope.

## Verification
- Required command: `:eyelib-molang:test` via IDE/JetBrains Gradle tooling only.

## Hidden Failure Mode To Watch
- Accidentally converting the binder guard into a new silent semantic classifier that starts owning more callable-shape policy than intended. This slice should only prevent malformed bind-link requests, not reinterpret unsupported syntax.

## Handoff Rules
- Fresh subagents only; do not reuse task sessions.
- Keep reports concise and file-backed where possible.
