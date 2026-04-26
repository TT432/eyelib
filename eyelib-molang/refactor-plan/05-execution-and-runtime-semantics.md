# Phase 5 - Execution And Runtime Semantics

## Goal
- Make runtime execution behavior, lowering/execution-plan ownership, and first-cut parity targets explicit before policy-driven specialization and cutover work begins.

## Source Docs
- `eyelib-molang/design/molang-ast-and-semantics-draft.md`
- `eyelib-molang/design/compatibility-semantics-matrix.md`
- `eyelib-molang/design/shared-vocabulary-and-phase-ownership-draft.md`
- `eyelib-molang/design/strict-debug-diagnostics-mode-draft.md`

## Current Anchors
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangCompiledFunction.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`

## Current Deferred Compatibility Posture (`this`)
- The current generated-parser-backed compile path intentionally lowers `this` to `MolangFloat.ZERO` in `MolangCompileVisitor.visitThis()` as a compatibility fallback.
- This is not treated as completed runtime semantics; it is an explicit Phase 5 deferred posture.
- Evidence is covered by tests:
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalyzerTest.java` asserts analyzer blocker `this_not_foldable`.
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java` asserts binder preserves `BoundThisExpr` without binder diagnostics or deferred notes.
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/MolangValueConstantFoldingTest.java` asserts runtime compile-path evaluation of `new MolangValue("this")` returns zero for a default scope.

## Revisit Gate For `this`
- Do not replace the compatibility fallback until Phase 5 names the replacement execution/lowering owner and introduces explicit `this` runtime-semantics rows with parity assertions against the current compile path.

## In Scope
- Execution-plan / lowering ownership after binder and host/query contracts exist
- Runtime semantics for the first cutover slice, including short-circuit, control-flow, and access/runtime evaluation rules that must be explicit before policy specialization
- Interpreter-first or additive execution posture for the replacement path
- Parity targets for the chosen v1 execution slice against the current engine path

## Out Of Scope
- Policy-pack selection precedence
- Specialization cache semantics
- Final cutover / deletion of the old compile path

## Deliverables
- An explicit execution-result / lowering ownership contract
- A documented first-cut runtime semantics matrix for the chosen v1 slice
- Tests or corpus assertions proving parity targets for the chosen execution slice
- A clear statement of which runtime behaviors remain deferred to later policy/specialization phases

## Acceptance Criteria
- The canonical v1 compatibility scope matrix contains concrete Phase 5 rows for the execution behaviors that are `Required`, not just posture labels.
- Phase 5 runtime assertions identify the exact current-engine parity target for each required execution row before widening the slice.
- Phase 5 verification includes both module-local evidence and downstream consumer parity evidence suitable for handing work into Phase 6.

## Non-Negotiable Execution Contracts
- Execution semantics must have a named owner before cutover. They may not remain hidden inside ad hoc compile-visitor behavior.
- The first replacement posture is additive and observable: introduce the new execution path without deleting the generated-parser-driven path first.
- Runtime behavior that is compatibility-neutral for the chosen slice must be implemented and tested here rather than postponed into policy-pack wiring.
- The v1 compatibility scope matrix must mark which execution behaviors are `Required`, `Targeted`, `Deferred`, or `Avoid baking in` before specialization work begins.
- Execution-path parity is defined at the contract level for the chosen slice. It must name what is expected to match the current engine path and what is intentionally deferred.

## TDD Slices
1. Add failing runtime-semantics assertions for the chosen required slice.
2. Add failing parity assertions against the current engine path for the same slice.
3. Implement the minimal execution/lowering behavior needed for that slice.
4. Expand only after required-slice parity is green and explicitly documented.

## Verification Gate
- `./gradlew :eyelib-molang:test`
- `./gradlew :eyelib-molang:test :eyelib-importer:test :eyelib-processor:test :test`

## Downstream Consumer Parity Gate
- Phase 5 may not hand work to Phase 6 until root runtime, `:eyelib-importer`, and `:eyelib-processor` all compile/test against the chosen execution slice without regressions.

## Exit Criteria
- The rewrite has a named execution/lowering owner instead of leaving runtime behavior implicit between binder and cutover.
- The chosen v1 execution slice has explicit parity evidence against the current engine path.
- Required-vs-deferred runtime behaviors are documented well enough that policy/specialization work does not absorb core execution semantics by accident.
