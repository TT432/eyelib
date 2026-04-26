# Eyelib Molang Phase 4 - Bind-Link Contract Slice

## Goal

Advance `:eyelib-molang` Phase 4 with the next smallest reviewable slice after query variant selection: add a **query-only bind-link contract** that turns binder-projected query access into a stable engine-local link handoff, without choosing the final winner and without integrating runtime/codegen consumers yet.

## Why this slice

- `eyelib-molang/ROADMAP.md` and `eyelib-molang/refactor-plan/04-host-and-query-bridge.md` still list **bind-link contract** and **transitional parity subset** as the remaining pending Phase 4 surfaces after query variant selection landed.
- Oracle recommendation: do a **query-only bind-link slice** next, carrying stable candidate-set + registry-version refs, while keeping final host-shape-aware winner selection deferred to specialization.
- Current code anchors show a precise gap:
  - binder already projects query access through `BoundMolang.BoundQueryAccessExpr`
  - mapping now exposes `selectQueryVariant(...)` and query matrix metadata
  - but compile/analysis paths still use raw `findMethod/findField`, and there is no typed bind-link handoff object yet
- This makes bind-link the smallest additive seam that meaningfully advances Phase 4 without leaking into parity, runtime execution, or Phase 5/6 work.

## Non-goals

- Do not integrate the new bind-link contract into `MolangCompileVisitor`, `MolangExpressionAnalysisVisitor`, `MolangCompileHandler`, or root/runtime paths in this slice.
- Do not implement transitional parity subset yet.
- Do not let the linker choose the final host-shape winner.
- Do not introduce root `mc/impl/molang/**` edits.
- Do not edit generated parser files.
- Do not start Phase 5 execution semantics or Phase 6 specialization/cutover work.

## Required contract acceptance

This slice must prove all of the following:

1. A **narrow bind->link pass** exists as an explicit seam, not a second semantic dispatcher.
2. The seam resolves symbolic query access into stable **candidate-set refs** plus **registry version refs**.
3. Binder ownership stays intact:
   - binder projects semantics
   - linker resolves stable refs
   - specialization remains the future owner of final host-shape-aware winner selection
4. Query surface kind remains preserved (`PROPERTY` vs `EXPLICIT_CALL`) across the handoff.
5. Failure posture stays loud and explicit for unresolved symbolic query names or invalid link inputs.

## Expected deliverable

A design->implementation->review loop that lands a **contract-first query bind-link seam** inside `:eyelib-molang`, backed by dedicated tests. The slice should be additive, reviewable, and intentionally not yet consumed by compile/runtime code.

## Likely touched files

Primary anchors:

- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BoundMolang.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BindResult.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` (new bind-link contract test class)
- `eyelib-molang/ROADMAP.md` (if evidence/status wording changes)

Probable new contract-only area:

- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/`

Context-only anchors:

- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalysisVisitor.java`

## Explicit deferrals

The implementation must keep all of these deferred:

- compile/analysis visitor consumption of the new link contract
- transitional parity subset
- general callable linking beyond canonical query access
- bind-link driven winner selection
- specialization logic
- candidate refs serialized across registry snapshots
- root `mc/impl/molang/**` changes
- generated parser edits

## Subtasks

### Task A - Design

Produce a compact design memo for the query-only bind-link contract.

Must answer:

- What is the smallest handoff object shape from binder to linker?
- Which stable ref fields are required for the slice (`candidate-set ref`, `registry version ref`, surface kind, symbolic query name, visible call-shape, required host roles)?
- Which exact existing binder outputs are reused versus extended?
- What unresolved cases fail loudly now, and what remains deferred to parity/specialization?
- Which files are allowed for implementation?

Output:

- a memo file under `.sisyphus/`
- exact implementation allowlist
- invariant checklist for review

Verification:

- Tooling: read the design memo, `eyelib-molang/ROADMAP.md`, `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`, current binder anchors, and `MolangMappingTree.java`
- Steps:
  1. verify the memo defines binder->link ownership and explicitly says the linker does not choose the final winner
  2. verify the memo names stable `candidate-set ref` and `registry version ref` as required output fields
  3. verify the memo preserves `PROPERTY` vs `EXPLICIT_CALL` query surface kind
  4. verify the memo explicitly defers parity, compile/runtime consumer adoption, and general callable linking
  5. verify the memo names the exact implementation allowlist
- Expected result: all five checks are satisfied before implementation starts

### Task B - Implementation

Implement the approved query-only bind-link contract slice.

Must do:

- add a dedicated bind-link contract test class under `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` or another clearly justified Phase-4-local test location
- add the minimum new contract types and binder/link glue needed for tests
- keep the slice additive and query-only
- keep compile/runtime visitors untouched except for read-only context
- update `eyelib-molang/ROADMAP.md` in the same change if evidence/status changes
- run `:eyelib-molang:test` through JetBrains Gradle tooling

Must not do:

- no root module edits
- no generated parser edits
- no transitional parity logic
- no winner-selection logic in the linker
- no broad callable refactor beyond canonical query access

Verification:

- Tooling: JetBrains Gradle tooling + IDE diagnostics
- Steps:
  1. run diagnostics on all changed files and require zero errors
  2. run `:eyelib-molang:test`
  3. confirm changed files are limited to the approved allowlist
- Expected result: diagnostics show zero errors and `:eyelib-molang:test` exits with code `0`

### Task C - Review

Review the landed slice against the approved design memo and roadmap.

Must verify:

- the new contract is binder->link only and does not perform final winner selection
- candidate-set ref and registry-version ref are present in the landed contract
- query surface kind is preserved
- unresolved/invalid link cases fail loudly
- compile/runtime visitors were not pulled into this slice
- `ROADMAP.md` evidence/status wording is correct

Verification:

- Tooling:
  - read the approved design memo
  - read all changed production/test files
  - read `eyelib-molang/ROADMAP.md`
  - run or inspect `:eyelib-molang:test` result via JetBrains tooling
- Steps:
  1. compare the design memo against the landed contract types and tests
  2. verify the tests cover stable ref output and loud unresolved/failure behavior
  3. verify the tests do not encode compile/runtime adoption or specialization behavior
  4. verify changed files stay within the allowlist
  5. verify roadmap evidence was updated if the slice changed Phase 4 evidence
  6. require `:eyelib-molang:test` success
- Expected result: all checks pass and the slice remains a narrow Phase-4-local contract step

## Initial sequencing decision

1. Momus reviews this bind-link plan.
2. Design subagent writes the bind-link memo.
3. Implementation subagent lands the contract and tests.
4. Review subagent validates the landed slice.
