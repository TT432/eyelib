# Eyelib Molang Roadmap

## Purpose

- This file is the single current-state roadmap for `:eyelib-molang` refactor work.
- Use the design drafts in `eyelib-molang/design/` for rationale and vocabulary.
- Use the phase plans in `eyelib-molang/refactor-plan/` for detailed execution gates.
- Use this roadmap to decide what is done, what is active now, what is blocked, and what must be updated when Molang work changes direction.

## Roadmap Update Rule

Update this file in the same change whenever Molang work does any of the following:

1. Adds, removes, renames, or re-scopes a Molang phase, milestone, gate, or verification command.
2. Promotes a design draft or refactor-plan item into implemented code/tests.
3. Moves a roadmap item between `Current`, `Next`, `Blocked`, `Deferred`, or `Done`.
4. Changes ownership between `:eyelib-molang`, generated parser code, root legacy marker docs, or root `mc/impl/molang/**` platform bindings.
5. Adds a new corpus layer, diagnostics mode behavior, binder family, host/query bridge, execution path, policy-pack behavior, cache behavior, or cutover mechanism.

If a Molang code change does not update this roadmap, it must be because the change is an implementation detail that leaves all milestones, gates, and ownership unchanged.

## Source Of Truth Order

When documents disagree, resolve them in this order:

1. `eyelib-molang/ROADMAP.md` for current progress and next actions.
2. `eyelib-molang/refactor-plan/README.md` and phase files for gates and phase-level acceptance criteria.
3. `eyelib-molang/design/README.md` and design drafts for rationale and candidate architecture.
4. `docs/index/molang.md`, `MODULES.md`, and package READMEs for navigation and ownership summaries.

Design drafts are not implementation commitments until this roadmap or the refactor plan promotes them.

## Stable Boundaries

- Engine-owned code lives under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`.
- Generated parser artifacts live under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` and are read-only during normal work.
- Root `src/main/java/io/github/tt432/eyelib/molang/` is a legacy marker/handoff path only.
- Root `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**` owns Minecraft/Forge platform bindings and lifecycle hooks.
- The old generated-parser-backed compile path must remain available until a cutover checklist and rollback point are documented.

## Current Implementation Snapshot

Evidence from the current tree:

- Runtime/value core exists: `MolangValue*.java`, `MolangScope.java`, `MolangOwnerSet.java`, and `type/` value objects.
- Current compile path exists: `compiler/MolangCompileHandler.java`, `compiler/MolangCompileVisitor.java`, `compiler/MolangCompileCache.java`, and `compiler/MolangCompilorCacheHandler.java`.
- Generated parser path exists and remains active: `generated/` plus `compiler/frontend/GeneratedMolangParserFrontend.java`.
- Additive AST frontend work exists: `compiler/frontend/ast/`, `GeneratedParserBackedAstMolangParserFrontend.java`, and `HandwrittenMolangAstParserFrontend.java`.
- Corpus/harness work exists in tests: `src/test/java/io/github/tt432/eyelibmolang/compiler/corpus/` and `src/test/resources/io/github/tt432/eyelibmolang/compiler/corpus/phase1/`. Phase1 starter corpus now has 33 expression rows (≥30 KR met), covering unary, comparison, for_each, return, member dot-chain, grouping, strings, array-literal reject, and binary-conditional syntax baseline families.
- Binder work exists: `compiler/binding/` with alias normalization, query projection, invalid-write diagnostics, typed deferred loop break/continue nodes, deferred notes, and normal/strict/debug diagnostic modes. Deferred reason taxonomy now has 5 distinct types: `UNSUPPORTED_IN_THIS_SLICE`, `HOST_SHAPE_DEPENDENT`, `QUERY_VARIANT_SELECTION_DEPENDENT`, `COMPATIBILITY_POLICY_DEPENDENT`, and `DIAGNOSTICS_OVERLAY_OWNED_FOLLOWUP` (≥3 distinct reason types KR met).
- Mapping ports exist: `mapping/api/` plus built-in mappings in `mapping/MolangMath.java` and `mapping/MolangToplevel.java`.

Known current deferred compatibility fallback:

- `compiler/MolangCompileVisitor.java` now explicitly documents that `visitThis()` lowers `this` to zero as an intentional compatibility fallback while Phase 5 execution semantics are still deferred.
- Test evidence for the deferred posture is now explicit in `src/test/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalyzerTest.java` (`this_not_foldable`), `src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java` (`BoundThisExpr` with no binder diagnostics/deferred note), and `src/test/java/io/github/tt432/eyelibmolang/compiler/MolangValueConstantFoldingTest.java` (current compile-path `this` evaluates to zero).
- Revisit gate: replace the fallback only after Phase 5 names the replacement execution/lowering owner and adds an explicit `this` runtime-semantics row with parity assertions.

## Phase Status

| Phase | Status | Evidence | Current rule |
|---|---|---|---|
| Phase 0 - Overview and boundaries | `Done / maintain` | `refactor-plan/00-overview-and-boundaries.md`, `docs/index/molang.md`, generated-code policy | Keep boundary docs aligned when ownership changes. |
| Phase 1 - Corpus and harness | `Current / partial` | corpus loader, linter, harness, parse runner, phase1 resources, corpus tests | Continue using `./gradlew :eyelib-molang:test`; add dedicated runner command only when it exists. |
| Phase 2 - Parser and AST | `Current / partial` | `compiler/frontend/ast/`, generated-backed AST frontend, handwritten frontend, frontend tests | Keep generated parser path active; parser work must be additive and corpus-backed. |
| Phase 3 - Binder and diagnostics | `Current / partial` | `compiler/binding/`, `src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java` for typed deferred loop break/continue coverage in normal/strict/debug modes, alias canonicalization coverage for all four roots (`q/t/v/c`), bind-shape/diagnostics/debug-trace corpus support, 5 deferred reason types (`UNSUPPORTED_IN_THIS_SLICE`, `HOST_SHAPE_DEPENDENT`, `QUERY_VARIANT_SELECTION_DEPENDENT`, `COMPATIBILITY_POLICY_DEPENDENT`, `DIAGNOSTICS_OVERLAY_OWNED_FOLLOWUP`) | Widen binder families through tests; keep unsupported semantics explicit via deferred notes, and keep the typed deferred break/continue lane narrow until broader Phase 3 widening is separately planned. |
| Phase 4 - Host and query bridge | `Blocked by recorded decisions, contract test slices green` | current `mapping/api/` ports exist, the Phase 4 decision set is recorded, `mapping/MolangHostPublicationDeterminismConflictTest.java` covers host publication determinism plus equal-tie conflict failure, `mapping/MolangCallableDiscoveryRoleContractTest.java` covers callable discovery roles with bounded receiver inference and loud ambiguity failure, `mapping/MolangQueryVariantSelectionMatrixContractTest.java` covers query-variant matrix ordering including explicit default-variant fallback plus equal-specificity/equal-priority loud ambiguity failure, `mapping/MolangQueryBindLinkContractTest.java` and `mapping/MolangCallableBindLinkContractTest.java` cover query and callable bind-link contracts (surface/call-shape preservation, stable ref exposure, required host-role exposure, loud unresolved/invalid failures, and explicit no-winner behavior), and `mapping/MolangAnimationClockTransitionalParityContractTest.java` plus root runtime assertions in `src/test/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQueryAnimationClockRuntimeParityTest.java` + `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java` cover the animation-clock transitional parity subset (`query.anim_time`, alias `query.life_time`, `query.delta_time`, and default expression path). | Keep broad implementation blocked while winner-selection/specialization and later-phase runtime replacement remain deferred. |
| Phase 5 - Execution and runtime semantics | `Blocked by Phase 3/4 readiness` | current bytecode compile path exists, replacement execution owner not introduced | Do not let execution semantics hide in cache/cutover work; define required v1 execution rows first. |
| Phase 6 - Policy, specialization, cache, reporting, cutover | `Blocked / future` | phase plan exists, no cutover evidence yet | Do not remove old compile path before policy, specialization, cache, reporting, downstream parity, and rollback evidence are green. |

## Active Milestones

### M1 - Keep phase 1 corpus harness reliable

- Preserve stable corpus case IDs, lint errors, result classes, effective diagnostics mode, and effective policy-pack reporting.
- Keep phase1 corpus resources valid and runnable through `jetbrain_run_gradle_tasks :eyelib-molang:test`. Starter corpus: 33 rows (≥30 KR ✅), covering unary, comparison, for_each, return, member dot-chain, grouping, strings, array-literal reject, and binary-conditional.
- Do not promote corpus rows to cutover evidence unless they reference the V1 compatibility matrix in `eyelib-molang/refactor-plan/README.md`.

### M2 - Widen parser/AST only with corpus-backed slices

- Add parser acceptance/rejection cases before adding new handwritten parser behavior.
- Keep `BlockExpr` as the canonical expression-valued block node.
- Keep `loop` and `for_each` as dedicated control-form productions, not generic call-like forms.
- Do not edit generated parser artifacts unless the task is explicitly a regeneration/isolation task.

### M3 - Widen binder diagnostics without hiding semantics

- Keep alias canonicalization, invalid-write rejection, access-family preservation, and query projection as binder-owned responsibilities.
- Represent unsupported or later-phase semantics as typed deferred nodes plus reason tokens, including the current typed deferred loop break/continue lane.
- Expand strict/debug coverage as overlays; broader Phase 3 widening stays open, and do not create a second hidden semantic system through diagnostics mode behavior.

### M4 - Resolve Phase 4 host/query gates before implementation

Before host/query bridge implementation starts, keep this roadmap and `refactor-plan/04-host-and-query-bridge.md` aligned with the recorded Phase 4 decisions and required pre-implementation test surfaces:

- Bounded callable parameter-role inference, with explicit metadata required for injected and special engine arguments.
- One unified host publication registry with internal adapter categories, not separate service-specific registries.
- A narrow bind-to-link pass between binder and specialization that resolves symbolic names to stable candidate-set refs and registry version refs.
- Required test surfaces: host publication determinism and conflict handling, callable discovery role coverage, query variant selection matrix, bind-link contract, and transitional parity subset.
- Contract test evidence landed for host publication determinism and equal-tie conflict failure in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java`, callable discovery role coverage in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableDiscoveryRoleContractTest.java`, query variant selection matrix coverage in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`, query and callable bind-link contract coverage in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java` and `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java`, and animation-clock transitional parity coverage in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangAnimationClockTransitionalParityContractTest.java` plus root runtime parity assertions in `src/test/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQueryAnimationClockRuntimeParityTest.java` and codec default-expression coverage in `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java`.

### M5 - Define execution ownership before runtime replacement

- Name the replacement execution/lowering owner before implementing cutover-sensitive runtime behavior.
- Define required v1 execution rows for short-circuit/null-propagation, control flow, and access/index behavior.
- Keep the current `this -> 0` compatibility fallback explicit and test-backed until Phase 5 promotes concrete `this` runtime semantics under a named execution owner.

## Blocked / Deferred Decisions

- Phase 5 replacement execution/lowering owner and first required execution slice.
- Phase 5 concrete `this` runtime semantics beyond the documented compatibility fallback (`this -> 0`) in the current compile path.
- Phase 6 flat vs layered policy packs.
- Phase 6 typed policy-pack option representation.
- Phase 6 debug-vs-normal specialization cache identity.
- Final cutover and deletion of generated-parser-backed compile path.

## Phase 4 Recorded Decisions And Required Test Surfaces

- Callable parameter-role inference is bounded. Explicit metadata is required for injected host and special engine arguments. The first non-special host parameter may infer `RECEIVER` only when the declaration is otherwise unambiguous. Discovery fails loudly on ambiguity.
- Host publication uses one unified registry with internal adapter categories. Separate service-specific registries are not the target model.
- Phase 4 includes a narrow bind-to-link pass before specialization. Binder projects semantics, the linker resolves symbolic query and callable names to stable candidate-set refs and registry version refs, and specialization chooses the final host-shape winner.
- Required pre-implementation test surfaces: host publication determinism and conflicts, callable discovery roles, query variant selection matrix, bind-link contract, and a transitional parity subset.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java` verifies deterministic publication order for the same mapping set and loud failure for unresolved equal-tie callable conflicts.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableDiscoveryRoleContractTest.java` verifies explicit special-role metadata publication, bounded first non-special receiver inference, and loud deterministic failure for ambiguous receiver inference.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java` verifies query selection ordering (name, visible arity, visible compatibility, required host roles, specificity, priority), explicit lowest-specificity default-variant behavior, and loud failure for equal-specificity/equal-priority ambiguity.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java` and `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java` verify query and callable bind-link handoff output preserves symbolic name/surface/call-shape, exposes stable `candidateSetRef` and `registryVersionRef`, exposes required host roles per candidate, fails loudly for unresolved symbolic names and invalid call-shape input, and keeps winner selection deferred.

## OKR — Key Results Per Phase

Each Phase (Objective) has quantified Key Results.
KR status uses ✅ (done) / ⬜ (not started) / 🔶 (partial).
Target thresholds establish what "done" means before phase promotion.

### Phase 1 — Corpus and Harness

| KR | Threshold | Status |
|---|---|---|
| Stable corpus case IDs | 100% phase1 cases have fixed IDs | ✅ |
| Corpus linter zero errors | 0 lint errors on phase1 resources (valid corpus only; invalid*/ intentional test fixtures excluded) | ✅ |
| Corpus parse runner | 100% phase1 cases pass parse | ⬜ |
| Phase1 corpus rows | ≥30 expression rows covering syntax baseline | ✅ |

### Phase 2 — Parser and AST

| KR | Threshold | Status |
|---|---|---|
| AST node coverage | All syntax-baseline checklist nodes have explicit AST types | 🔶 |
| ForEachExpr binder branch | Explicit handler in MolangBinder (not generic else) | ✅ |
| Generated parser parity | 0 regression failures on old compile path | 🔶 |
| Handwritten frontend coverage | ≥20 acceptance/rejection tests | ✅ |

### Phase 3 — Binder and Diagnostics

| KR | Threshold | Status |
|---|---|---|
| Alias canonicalization coverage | All 4 alias roots (q/t/v/c) → canonical tested | ✅ |
| Deferred reason granularity | ≥3 distinct reason types beyond UNSUPPORTED_IN_THIS_SLICE | ✅ |
| Invalid-write diagnostics | All write-target errors tested (query/context/unknown) | 🔶 |
| Strict/debug mode coverage | ≥5 distinct binder families tested per mode | ✅ |
| Alias logic deduplication | ≤1 implementation location (shared module) | ✅ |

### Phase 4 — Host and Query Bridge

| KR | Threshold | Status |
|---|---|---|
| Contract test count | ≥5 contract test classes green | 🔶 |
| Host publication determinism | Equal-tie conflict fails loudly (tested) | 🔶 |
| Callable discovery roles | Bounded inference + loud ambiguity (tested) | 🔶 |
| Query variant selection matrix | Explicit default-variant + equal-tie failure (tested) | 🔶 |
| Bind-link contract | Stable candidateSetRef + registryVersionRef (tested) | 🔶 |
| MolangOwnerSet→HostContext migration | migration plan documented (not yet performed) | ⬜ |

### Phase 5 — Execution and Runtime Semantics

| KR | Threshold | Status |
|---|---|---|
| Replacement execution owner named | Documented in ROADMAP + phase plan | ⬜ |
| V1 execution rows defined | ≥5 rows (short-circuit, null-prop, control-flow, access/index, this) | ⬜ |
| This→0 fallback replaced | Explicit `this` runtime semantics under named owner | ⬜ |
| All UNSUPPORTED_IN_THIS_SLICE replaced | 0 remaining defer notes with generic reason | ⬜ |

### Phase 6 — Policy, Specialization, Cache, Cutover

| KR | Threshold | Status |
|---|---|---|
| Old compile path deletion | All Phase 6 gates green + rollback point documented | ⬜ |
| Downstream parity | All 4 modules test suites pass after cutover | ⬜ |
| Cache identity model | Debug-vs-normal specialization cache identity documented | ⬜ |

## Verification Gates

- Docs-only roadmap changes: verify every referenced path still exists.
- **ALL Gradle commands must use JetBrains MCP** (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.
- Phase 1-4 implementation slices: `jetbrain_run_gradle_tasks :eyelib-molang:test`.
- Phase 5 and Phase 6 execution/cutover slices: `jetbrain_run_gradle_tasks :eyelib-molang:test :eyelib-importer:test :eyelib-processor:test :test`.
- Generated parser changes: require a task-specific regeneration/isolation plan and update `docs/architecture/03-generated-code-policy.md` if the generated zone moves or changes ownership.

## Anti-Drift Checklist

Before ending any Molang refactor task, answer these in the change itself:

1. Did the task change the current phase status or next milestone? If yes, update this file.
2. Did the task implement a design draft or phase-plan promise? If yes, update the corresponding roadmap row and evidence.
3. Did the task defer a previously planned behavior? If yes, add it to `Blocked / Deferred Decisions` or the relevant phase plan.
4. Did the task change verification commands? If yes, update `Verification Gates` here and the phase plan.
5. Did the task touch generated parser output or root platform bindings? If yes, update the boundary notes that point to those paths.
