# Phase 3 - Binder And Diagnostics

## Goal
- Normalize parser output into bound semantic nodes and make deferred semantics visible before runtime specialization starts.

## Source Docs
- `eyelib-molang/design/shared-vocabulary-and-phase-ownership-draft.md`
- `eyelib-molang/design/binder-normalization-contract-draft.md`
- `eyelib-molang/design/strict-debug-diagnostics-mode-draft.md`
- `eyelib-molang/design/compatibility-semantics-matrix.md`

## Current Anchors
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalyzer.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalysisVisitor.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/compiler/`

## In Scope
- Binder input/output contract
- Alias normalization, root interpretation, query candidate projection
- Deferred-reason tagging for semantics that must wait for policy or specialization
- Structured diagnostics for normal/strict/debug modes

## Out Of Scope
- Final host-object lookup
- Policy pack selection logic
- Runtime specialization and cache invalidation

## Minimal Phase 3 Slice Status (Current)
- Implemented in this minimal binder lane:
  - Query projection coverage for both property access (`query:PROPERTY`) and query-rooted explicit calls (`query:EXPLICIT_CALL`).
  - Deferred-note assertion coverage through bind-shape corpus expectations (deferred-note reason tokens).
  - Typed deferred `break` and `continue` coverage inside `loop`, with `src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java` covering normal, strict, and debug evidence.
  - Optional adjacent `*.diagnostics.golden.yaml` support for `bind-normalize` corpus cases in `normal` diagnostics mode, with structured binder diagnostics subset matching on phase/severity/code and optional message subset.
  - Optional adjacent `*.debug-trace.golden.yaml` support for `bind-normalize` corpus cases in `debug` diagnostics mode, with debug-trace token subset matching.
- Intentionally deferred beyond this slice:
  - Full strict/debug diagnostics-overlay behavior validation matrix breadth across additional binder families, kept for later Phase 3 widening and downstream phases.

## Deliverables
- Bound node/result wrapper contract
- Binder-owned traits and preservation rules
- Mode-aware diagnostics shape
- Bind-shape assertions in the corpus/test surface

## Acceptance Criteria
- Binder assertions prove alias canonicalization, access-family preservation, query-candidate projection, invalid-write rejection, source-span lineage, and typed deferred `break` / `continue` statements for the first supported families.
- `BindResult` and diagnostic payload shape are explicit enough that later execution/specialization phases consume them without rebinding hidden semantics.
- Deferred nodes remain typed and reasoned; they do not collapse into generic "unknown" placeholders.

## Non-Negotiable Binder And Diagnostics Contracts
- Alias normalization is required, not optional: `q/t/v/c` canonicalize to `query/temp/variable/context`.
- Invalid assignment targets are binder errors. They must fail in binder diagnostics instead of remaining latent runtime surprises.
- Binder must preserve access-family and structure needed by later phases, including `.` vs `->`, call/index/member chain order, block/control-flow ordering, and source-span lineage.
- `BoundQueryAccess` stays the canonical binder-level query node. If source spelling distinction matters, binder preserves omission-style access versus explicit call spelling on that node instead of inventing a competing canonical node family.
- Deferred semantics must remain explicit and testable. Binder may defer only when later host shape, compatibility policy, query variant selection, or mode-owned behavior is genuinely required, and every deferred node must carry a reason category.
- Diagnostics overlays own severity and trace behavior on top of semantic classification. Binder should expose normalization/deferral state; it should not bury compatibility or strict/debug decisions inside generic fallback logic.

## Mode Matrix Summary
- Normal mode: baseline diagnostics for supported binder semantics without extra trace amplification.
- Strict mode: same binder ownership, but stricter severity/escalation for unsupported or ambiguity-sensitive cases.
- Debug mode: same semantic classification, with richer trace/debug payloads instead of a separate semantic fallback system.

## Deferred Reason Categories
- Host-shape-dependent
- Query-variant-selection-dependent
- Compatibility-policy-dependent
- Diagnostics-overlay-owned follow-up
- Explicitly unsupported-in-this-slice

## Downstream Entry Gates
- **Phase 4 entry gate**: do not begin host/query bridge implementation until the plan records the bounded inference rule, the unified host registry choice, the bind-link contract, and the required test surfaces.
- **Phase 5 entry gate**: do not begin execution/specialization-consuming work until the required richness of deferred-reason payloads is documented for downstream consumers.

## TDD Slices
1. Add failing binder-shape tests.
2. Add failing diagnostic-shape tests for normal/strict/debug cases.
3. Implement minimal binder normalization for the smallest query/access families.
4. Add conservative deferral behavior before any specialization-aware narrowing.

## Verification Gate
- `./gradlew :eyelib-molang:test`

## Exit Criteria
- Parser output is normalized into stable bound results.
- Binder responsibilities are separated from parser recovery and runtime specialization.
- Compatibility-sensitive behavior is represented as explicit deferred structure instead of hidden fallback logic.
- Tests prove alias canonicalization, access-family preservation, query-candidate projection, and deferred-reason tagging for the first supported families.
