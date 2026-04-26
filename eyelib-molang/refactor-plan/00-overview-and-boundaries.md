# Phase 0 - Overview And Boundaries

## Goal
- Establish the rewrite guardrails before any structural code changes begin.
- Make phase ownership explicit so the implementation plan stays dependency-ordered instead of collapsing into a feature wishlist.

## Source Docs
- `docs/index/molang.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/03-generated-code-policy.md`
- `eyelib-molang/design/README.md`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md`

## In Scope
- Restating module ownership for `:eyelib-molang`
- Documenting current hotspots and migration anchors
- Declaring the phase order, verification baseline, and cutover rules

## Out Of Scope
- Editing generated parser artifacts
- Moving platform bindings out of root `mc/impl/molang/**`
- Freeform package churn not justified by a later phase deliverable

## Current Anchors
- `eyelib-molang/build.gradle`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangOwnerSet.java`

## Global Rewrite Strategy
1. Lock corpus and verification surfaces first.
2. Add a new handwritten frontend in parallel with the current generated-parser path.
3. Make binder and diagnostics testable before host/query replacement.
4. Bridge host/query semantics before runtime execution behavior is rewritten around them.
5. Make execution/lowering/runtime semantics observable before policy-driven specialization and cache work.
6. Cut over only after specialization, cache semantics, and reporting are observable.

## Verification Baseline
- Minimum gate for every phase: `./gradlew :eyelib-molang:test`
- If a phase introduces new phase-local test fixtures or runner entrypoints, the phase must document the exact command before execution begins.
- Phase 5 and Phase 6 must also document downstream consumer parity gates for root runtime, `:eyelib-importer`, and `:eyelib-processor`; cutover work does not begin on module-local evidence alone.
- Docs/planning changes must keep all referenced paths valid.

## Rollback Rule
- Every cutover step must name the compatibility shim or old path it relies on.
- No deletion of the current generated-parser-driven compile path until the later cutover checklist says the replacement is proven.

## Open Questions Carried Forward
- Phase 0 does not resolve all design questions. It records named entry gates that block later phases until the required decision is written down in the plan.
- **Phase 2 gate resolved**: parser implementation should target `BlockExpr` as the canonical expression-valued block node and treat `loop` / `for_each` as dedicated control-form productions.
- **Phase 4 entry gate**: resolve parameter-role annotation vs bounded inference and unified vs service-specific adapter publication before host/query bridge work starts.
- **Phase 6 entry gate**: resolve flat vs layered pack composition, typed pack options, and debug-mode cache identity before policy/specialization/cutover work starts.

## Done Condition For This Phase
- The team has a shared phase order, boundary statement, verification baseline, and cutover rule set to follow during execution.
