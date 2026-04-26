# Eyelib Molang Refactor Plan

## Purpose
- Turn the design drafts under `eyelib-molang/design/` into a dependency-ordered implementation plan for `:eyelib-molang`.
- Keep the plan scoped to the Molang engine subproject. Root `src/main/java/io/github/tt432/eyelib/mc/impl/molang/` remains a boundary reminder, not an implementation target for this plan.
- Keep `eyelib-molang/ROADMAP.md` as the current-state source of truth; this directory owns detailed phase gates and acceptance criteria.

## Scope And Boundaries
- Engine code lives under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`.
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` is generated/read-only during normal work.
- Root `src/main/java/io/github/tt432/eyelib/molang/` is a legacy marker only.
- Platform bindings stay outside this plan unless a later phase explicitly documents a boundary change.

## Source Inputs
1. `docs/index/molang.md`
2. `eyelib-molang/ROADMAP.md`
3. `docs/architecture/01-module-boundaries.md`
4. `docs/architecture/03-generated-code-policy.md`
5. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md`
6. `eyelib-molang/design/README.md`
7. The phase-specific design drafts listed below

## Current Hotspots
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangOwnerSet.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangQueryRuntimeBridge.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/`

## Phase Map
1. `00-overview-and-boundaries.md`
2. `01-corpus-and-harness.md`
3. `02-parser-and-ast.md`
4. `03-binder-and-diagnostics.md`
5. `04-host-and-query-bridge.md`
6. `05-execution-and-runtime-semantics.md`
7. `06-policy-specialization-cache-reporting-cutover.md`

## Dependency Chain
- Baseline and terminology first: `molang-syntax-baseline.md`, `shared-vocabulary-and-phase-ownership-draft.md`
- Frontend next: `molang-ast-and-semantics-draft.md`, `parser-strategy-draft.md`
- Binder before runtime policy: `binder-normalization-contract-draft.md`, `strict-debug-diagnostics-mode-draft.md`
- Host/query bridge before specialization: `host-injection-api-draft.md`, `host-adapter-registry-draft.md`, `callable-discovery-annotation-draft.md`, `query-variant-registry-draft.md`
- Execution semantics after binder + host/query and before cutover: `molang-ast-and-semantics-draft.md`, `compatibility-semantics-matrix.md`
- Compatibility and specialization after execution semantics: `compatibility-policy-pack-draft.md`, `policy-pack-selection-configuration-draft.md`, `runtime-specialization-contract-draft.md`
- Cache/reporting/cutover last: `specialization-cache-contract-draft.md`, `corpus-linter-runner-draft.md`, `corpus-reporter-output-format-draft.md`

## Global Execution Rules
- Prefer additive slices over replacement. The new parser/frontend must coexist with the current generated-parser path until proof exists.
- Treat `generated/` as read-only unless the work is explicitly a regeneration/isolation task.
- Keep parser, binder, host/query, and compatibility concerns separated; do not let one phase absorb another.
- Use the existing JUnit test surface in `:eyelib-molang` as the first execution anchor, then add corpus-driven coverage.
- **ALL Gradle commands must use JetBrains MCP** (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.
- Keep `:eyelib-molang:test` green throughout the rewrite (via `jetbrain_run_gradle_tasks`). Add narrower phase-specific tasks only when they exist.
- Treat downstream consumer parity as part of the rewrite, not as optional follow-up. Root `:test`, `:eyelib-importer:test`, and `:eyelib-processor:test` become mandatory verification gates (via `jetbrain_run_gradle_tasks`) once execution semantics and cutover work begin.

## Cross-Phase Non-Negotiable Contracts
- Keep the canonical shared vocabulary from `shared-vocabulary-and-phase-ownership-draft.md` stable across plan slices: `HostRole`, `HostContext`, `HostShape`, `VisibleArgSpec`, `CallableTraits`, `SourceOrigin`, `BoundQueryAccess`, `CompatibilityPolicyPack`, and `DiagnosticsModeOverlay`.
- Binder and host/query bridge phases communicate through a narrow bind-link handoff that resolves symbolic names into stable candidate-set refs and registry version refs. Runtime specialization consumes that link output and still owns the final host-shape-aware winner.
- Do not let later phases reintroduce raw owner-bag or raw-class lookup as the long-term semantic contract once host publication and query dispatch work begins.
- Ambiguity remains an error across publication, discovery, callable dispatch, and query variant selection. No phase may rely on silent guessing as a compatibility shortcut.
- Keep semantic compatibility ownership in policy packs and keep strict/debug behavior as diagnostics overlays. A diagnostics mode must not become a hidden second semantic pack system.
- Corpus/reporting work must preserve stable case IDs, record effective mode and policy-pack selection for every executed case, and keep `CORPUS_ERROR`, `ENGINE_FAILURE`, and `ASSERTION_FAILURE` as distinct result classes.
- Runtime semantics must have an explicit owner before cutover. Lowering/execution-plan work, interpreter/runtime behavior, and compatibility-neutral execution rules must not be left implicit inside cache or cutover tasks.

## V1 Compatibility Scope Matrix
The table below is the canonical V1 matrix artifact. Later phases must reference these rows instead of redefining their own ad hoc slice language.

| Behavior / family | Posture | Owning phase | Assertion surface | Current-engine parity target | Notes |
|---|---|---|---|---|---|
| Parse accept/reject for the chosen syntax baseline slice | `Required` | Phase 2 | corpus parse cases + parser acceptance tests | current generated-parser path for the same accepted/rejected slice | Entry gate for widening parser coverage |
| Binder canonicalization, invalid-write rejection, and typed deferral for the first supported access/query families | `Required` | Phase 3 | binder-shape tests + corpus bind assertions | current analyzer/binder-equivalent behavior for the chosen slice | Must stay explicit before execution/specialization starts |
| Deterministic host publication and query/callable selection for the representative migrated subset | `Required` | Phase 4 | host-role/query selection tests | current mapping discovery + runtime bridge contracts for the same migrated subset | Includes explicit default-variant behavior |
| Compatibility-neutral execution semantics: `short-circuit` / null-propagation behavior for the first cutover slice | `Required` | Phase 5 | runtime-semantics assertions + parity tests | current compile path results for the chosen execution slice | Must be explicitly defined before Phase 5 starts |
| Compatibility-neutral execution semantics: control-flow behavior for the first cutover slice | `Required` | Phase 5 | runtime-semantics assertions + parity tests | current compile path results for the chosen execution slice | Must be explicitly defined before Phase 5 starts |
| Compatibility-neutral execution semantics: access/index evaluation behavior for the first cutover slice | `Required` | Phase 5 | runtime-semantics assertions + parity tests | current compile path results for the chosen execution slice | Must be explicitly defined before Phase 5 starts |
| Phase 5 downstream handoff parity for root runtime, importer, and processor | `Required` | Phase 5 | `./gradlew :eyelib-molang:test :eyelib-importer:test :eyelib-processor:test :test` | no compile/test regressions in downstream consumers for the chosen execution slice before Phase 6 handoff | Separates Phase 5 readiness from final cutover parity |
| Downstream consumer parity for root runtime, importer, and processor | `Required` | Phase 6 | `./gradlew :eyelib-molang:test :eyelib-importer:test :eyelib-processor:test :test` | no compile/test regressions in downstream consumers for the chosen cutover slice | Mandatory before cutover begins |
| Policy-pack selection precedence and diagnostics-overlay interaction for the cutover slice | `Required` | Phase 6 | policy-pack matrix tests + corpus/report assertions | current effective policy behavior for the chosen cutover slice | Must be explicitly defined before Phase 6 specialization/cutover work begins |
| Specialization cache identity/invalidation for the cutover slice | `Targeted` | Phase 6 | cache-key/invalidation tests | parity target documented per cache-sensitive row | Promote to `Required` before any cache-dependent cutover |
| Extended compatibility behaviors outside the first cutover slice | `Deferred` | follow-up | follow-up corpus rows | no parity required for v1 cutover | Keep visible so omissions are intentional |
| New engine quirks without a current-engine contract | `Avoid baking in` | none | none | none | Must not silently become part of the semantic core |

## Open Questions And Deferred Decisions Register
- **Phase 2 gate resolved**: use `BlockExpr` as the canonical expression-valued block node, keep "complex expression" as source/corpus terminology only, and parse `loop` / `for_each` as dedicated control-form productions rather than generic call-like nodes lowered later.
- **Phase 4 entry gate**: do not begin host/query bridge work until the team has resolved how much callable parameter-role inference is allowed before explicit annotations are required, and whether host publication uses one unified adapter model or narrower service-specific adapters.
- **Phase 6 entry gate**: do not begin policy/specialization/cutover work until the team has resolved whether policy packs are flat or layered, how typed pack options are represented, and whether debug-vs-normal specialization caches share identity or intentionally fork.
- Keep explicitly deferred unless a later phase promotes them: any compatibility behavior marked non-required by the v1 scope matrix, plus cutover-independent reporter polish.

## Current-To-Target Structure Map
- Phase 2 may introduce additive syntax/frontend destinations.
- Phase 3 may introduce additive semantic/binding and diagnostics destinations.
- Phase 4 may introduce additive semantic/host and query-registry destinations.
- Phase 5 may introduce additive analysis/plan/runtime execution destinations.
- Phase 6 may introduce additive policy/specialization/cache/reporting destinations and only then document cutover of old compile-path seams.

## Atomic Commit Policy
- One commit per independently verifiable slice.
- Preferred slice order: spec/tests first, implementation second, cleanup/cutover third.
- Do not mix corpus, parser, binder, host/query, and cutover work in the same commit unless the slice cannot be separated without breaking the build.

## Exit Condition For The Whole Rewrite
- The new frontend, binder, host/query bridge, execution-semantics lane, policy/specialization flow, and reporting surface are all covered by executable corpus cases and existing `:eyelib-molang` tests.
- The old compile path is only removed after an explicit cutover checklist and rollback point are documented.
