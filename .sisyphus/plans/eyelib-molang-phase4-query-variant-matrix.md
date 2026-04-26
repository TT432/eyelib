# Eyelib Molang Phase 4 - Query Variant Selection Matrix Slice

## Goal

Advance `:eyelib-molang` Phase 4 with the smallest reviewable slice that is still meaningful: add the **query variant selection matrix** contract surface before bind-link and transitional parity.

## Why this slice

- `eyelib-molang/ROADMAP.md` still marks Phase 4 as blocked pending three remaining test surfaces: `query variant selection matrix`, `bind-link contract`, and `transitional parity subset`.
- `eyelib-molang/refactor-plan/04-host-and-query-bridge.md` orders the TDD slices as:
  1. host publication determinism/conflicts
  2. callable discovery roles
  3. query variant selection
  4. bind-link contract
  5. transitional parity subset
- Existing tests already cover the first two slices:
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java`
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallablePublicationSignatureRoleTest.java`
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableDiscoveryRoleContractTest.java`
- Adjacent binder/query anchors already exist without bind-link implementation:
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BoundMolang.java`
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java`
- Oracle recommendation: do the query variant selection matrix first; do **not** pull bind-link or specialization forward.

## Non-goals

- Do not implement bind-link refs, candidate-set refs, or registry version refs in this slice.
- Do not widen to transitional parity yet.
- Do not touch generated parser files.
- Do not rewrite root `mc/impl/molang/**` bindings.
- Do not start Phase 5 execution semantics or Phase 6 policy/cache/cutover work.

## Expected deliverable

A narrow design->implementation->review loop that lands a dedicated Phase 4 **contract test class** for query variant selection, plus only the minimum engine code needed to make the tests meaningful and green.

## Required contract coverage

The slice must express the ordering described by `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`:

1. exported name
2. visible arity
3. visible argument compatibility
4. required host-role availability
5. specificity
6. explicit priority tie-break

And it must include:

- at least one explicit default-variant path modeled as a real lowest-specificity variant
- at least one loud failure case for equal-specificity + equal-priority ambiguity
- no hidden fallback semantics

## Likely touched files

Primary anchors:

- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangFunction.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` (new dedicated query-selection test class)

Context-only anchors:

- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallablePublicationSignatureRoleTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableDiscoveryRoleContractTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java`
- `eyelib-molang/ROADMAP.md` (must be updated if this slice changes evidence/status)

## Subtasks

### Task A - Design

Produce a compact design note for the query variant matrix slice.

Must answer:

- What is the smallest public/engine-local selection contract needed for the tests?
- Which metadata already exists in `MolangMappingTree.FunctionInfo` and which metadata, if any, must be added?
- How will visible args, host-role availability, specificity, and explicit priority be represented without dragging in bind-link?
- What should remain intentionally deferred to the later bind-link slice?

Output:

- short design memo written to a file under `.sisyphus/`
- list of exact files the implementation subtask may edit
- list of invariants the review subtask must verify

Verification:

- Tooling: read the design memo, `eyelib-molang/ROADMAP.md`, `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`, and `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangFunction.java`
- Steps:
  1. verify the memo names the exact ordering to encode: exported name -> visible arity -> visible argument compatibility -> required host-role availability -> specificity -> explicit priority tie-break
  2. verify the memo names at least one explicit default-variant case and one equal-specificity/equal-priority loud failure case
  3. verify the memo explicitly marks bind-link refs, transitional parity, and specialization as deferred
  4. verify the memo lists the exact implementation files allowed to change
- Expected result: all four checks are satisfied in the memo; if any are missing, implementation must not start

### Task B - Implementation

Implement the slice from the approved design.

Must do:

- add a dedicated query-selection contract test class under `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/`
- keep the implementation additive and Phase-4-local
- keep bind-link, parity, and specialization blocked
- update `eyelib-molang/ROADMAP.md` in the same change if the evidence/status changes

Must not do:

- no generated parser edits
- no root module edits unless strictly required by documentation only
- no speculative refactors outside the slice

Verification:

- `:eyelib-molang:test` via IDE/JetBrains Gradle tooling

### Task C - Review

Review the implementation against the design and roadmap.

Must verify:

- the tests encode the documented ordering rather than accidental current behavior
- at least one default-variant case exists and is explicit
- loud ambiguity failure is covered
- no bind-link concepts leaked into this slice
- documentation/evidence updates are correct
- `:eyelib-molang:test` passes

Verification:

- Tooling:
  - read the approved design memo
  - read the new query-selection test class and any changed engine files
  - read `eyelib-molang/ROADMAP.md`
  - run `:eyelib-molang:test` via JetBrains Gradle tooling
- Steps:
  1. compare the design memo against the landed test names/assertions and verify every required ordering dimension appears in the tests
  2. verify one test covers an explicit default-variant path and asserts it wins only as the lowest-specificity fallback
  3. verify one test covers equal-specificity + equal-priority ambiguity and expects loud failure
  4. verify changed engine code does not introduce bind-link refs, candidate-set refs, registry version refs, or specialization logic
  5. verify `eyelib-molang/ROADMAP.md` evidence/status was updated if the slice changed roadmap evidence
  6. run `:eyelib-molang:test` and require success
- Expected result: all checks pass and the Gradle test task exits with code `0`; otherwise the slice is rejected for follow-up fixes

## Delegation rules

- Use fresh subagents where practical to avoid context pollution.
- Keep prompts atomic and reviewable.
- Prefer file-based communication for design/review artifacts.
- Supervisor does not edit business logic directly; subagents own design and implementation work.

## Initial sequencing decision

1. Momus reviews this plan.
2. Design subagent writes the contract memo.
3. Implementation subagent executes the memo.
4. Review subagent validates the landed slice.
