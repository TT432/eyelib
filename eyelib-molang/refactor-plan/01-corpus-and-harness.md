# Phase 1 - Corpus And Harness

## Goal
- Define the executable corpus, lint/runner split, and initial validation workflow before replacing any engine internals.

## Source Docs
- `eyelib-molang/design/molang-syntax-baseline.md`
- `eyelib-molang/design/parser-acceptance-corpus.md`
- `eyelib-molang/design/executable-corpus-format-draft.md`
- `eyelib-molang/design/corpus-linter-runner-draft.md`
- `eyelib-molang/design/corpus-reporter-output-format-draft.md`
- `eyelib-molang/design/compatibility-semantics-matrix.md`

## Current Anchors
- `eyelib-molang/build.gradle`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalyzerTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/MolangValueConstantFoldingTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangMcAdapterSeamTest.java`

## In Scope
- Corpus file layout and metadata model
- Separation of corpus loading, linting, execution, and reporting
- Initial parse/bind/runtime case taxonomy
- Characterization coverage for current parser/analyzer behavior where useful

## Out Of Scope
- Replacing the parser
- Binding host roles or runtime specialization behavior
- Final reporter UX polish beyond the minimum structured result shape

## Deliverables
- A chosen on-disk corpus layout under `:eyelib-molang`
- A linter contract that rejects malformed corpus metadata early
- A runner contract that can execute phase-specific assertions
- A minimum structured report shape for CI/local use

## Acceptance Criteria
- Corpus metadata has a documented required-field set, duplicate-case-ID rejection rule, and stable machine-readable identity independent of filenames.
- The runner contract documents default diagnostics mode and default policy-pack behavior for early parse-first slices instead of leaving them implicit.
- Expected result shapes default to shallow, phase-local assertions unless a case is intentionally promoted to a locked deeper golden shape.
- The plan documents whether parser acceptance and binder-normalization expectations live in one adjacent format or two neighboring formats before parser implementation broadens beyond characterization coverage and before binder work begins. The initial executable slice chooses a single `.molangcase` container with optional adjacent goldens rather than split file formats.

## Non-Negotiable Corpus And Reporting Contracts
- Corpus cases must use stable case IDs as the machine-readable identity instead of relying on filenames alone.
- Duplicate case IDs and malformed or missing required corpus metadata are linter errors that must be rejected before runner execution starts.
- The minimum structured report shape must preserve distinct result classes for `PASS`, `CORPUS_ERROR`, `ENGINE_FAILURE`, `ASSERTION_FAILURE`, and `SKIPPED`.
- Every executed case report must record the effective phase set, diagnostics mode, and policy-pack selection, even if early slices only exercise parse-first workflows.
- Corpus metadata and runner assertions must stay phase-aware so later binder/specialization expectations can extend the same format instead of replacing it.

## Default And Placeholder Semantics
- Parse-first corpus slices may use documented default mode/pack markers, but they must not invent final binder or specialization semantics early.
- Missing mode or policy-pack data is a schema/defaulting rule owned by the corpus contract, not an invitation for ad hoc runtime guessing.
- Corpus assertions for compatibility behavior must point back to the canonical rows in the v1 compatibility scope matrix so `Required`, `Targeted`, and `Deferred` cases stay distinguishable.

## Entry Gates For Phase 2 / Phase 3
- **Phase 2 may not start** until the team has decided whether parser acceptance and binder normalization share one corpus-adjacent expectation format or intentionally split into two linked layers.
- **Phase 3 may not start** until the team has decided how much expected-shape richness is required before the first stable runner surface becomes too brittle for the rewrite.

## TDD Slices
1. Add characterization tests for current parse/analyze/folding behavior.
2. Add failing corpus-shape/linter tests.
3. Add the minimal loader + linter surface.
4. Add parse-runner support before any binder/runtime runner expansion.

## Verification Gate
- `./gradlew :eyelib-molang:test`
- Before implementation starts, document the exact corpus runner invocation if a new task or test entrypoint is introduced.
- Before any corpus row is treated as cutover evidence, it must reference a concrete row from the canonical v1 compatibility scope matrix.

## Exit Criteria
- Corpus cases can be discovered, linted, and executed through a documented workflow.
- Parse-accept/reject expectations have a stable format.
- The rewrite has a shared test surface that later parser and binder phases can build on.
- The reporting baseline already distinguishes corpus-data problems from engine failures and assertion mismatches instead of collapsing them into one generic failure bucket.
