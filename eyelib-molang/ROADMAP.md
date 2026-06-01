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
4. `docs/decisions/`, `MODULES.md`, and package READMEs for navigation and ownership summaries.

Design drafts are not implementation commitments until this roadmap or the refactor plan promotes them.

## Stable Boundaries

- Engine-owned code lives under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`.
- Generated parser artifacts live under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` and are read-only during normal work.
- Root `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` is the only file remaining in the root `molang/` path — it holds root-coupled query functions (animation controller, variant) that cannot move to `eyelib-molang`.
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/platform/**` owns Minecraft/Forge platform bindings and lifecycle hooks.
- The old generated-parser-backed compile path must remain available until a cutover checklist and rollback point are documented.

## Current Implementation Snapshot

Evidence from the current tree:

- Runtime/value core exists: `MolangValue*.java`, `MolangScope.java`, and `type/` value objects.
- Unified compile-then-execute pipeline exists: `compiler/MolangCompilerImpl.java` orchestrates compilation from AST to bytecode using `MethodHandles.Lookup.defineHiddenClass()` (Java 15+), `compiler/MolangBytecodeEmitter.java` generates JVM bytecode for ALL 24 BoundMolang expression/statement types, `compiler/MolangRuntimeSupport.java` provides static helpers for mapping-tree-dependent runtime logic (member access, function calls, index resolution), and `compiler/MolangConstantExpressionEvaluator.java` folds compile-time constants.
- **Bytecode emitter** (`MolangBytecodeEmitter.java`, 457 lines) handles all bound expression types: literals (number/string), unary/binary/logical/comparison operators, identifiers, assignments, member access, function calls (via `MolangRuntimeSupport`), arrow/query/index access, blocks, loops, null coalesce, deferred/unknown nodes, and all statement types. No interpreter fallback — the old `BoundMolangEvaluator.java` has been removed.
- **`MolangValue.resolveFunction()`** is fully reconnected: constant folding → bytecode compile → wrap as `CompiledMolangExpression`. No evaluator fallback.
- **Compilation cache** (`MolangCompileCache.java`) provides single-tier L1 in-memory caching with `ConcurrentHashMap`, composite key (expression + registryVersionRef) for automatic staleness detection, `computeIfAbsent` for atomic compilation, and size-bound eviction (MAX_L1_SIZE=1000). L2 disk cache (`MolangDiskCache`) has been removed as it was never populated in production.
- **MolangScope** fixes: `get()` recursively traverses full parent scope chain (not just one level); `parent` is `volatile` for thread-safe publication.
- **MolangFloat** optimization: `valueOf(float)` returns cached ZERO/ONE constants for 0f/1f to reduce per-frame allocations.
- **MolangDiskCache** (`computeFileName`) uses SHA-256 instead of 32-bit `String.hashCode()` for collision-resistant file naming.
- Old `MolangCompileHandler.java` (ANTLR-based bytecode compiler with file-based caching) has been removed.
- Generated parser path exists: `generated/` plus `compiler/frontend/GeneratedMolangParserFrontend.java`.
- Additive AST frontend work exists: `compiler/frontend/ast/`, `GeneratedParserBackedAstMolangParserFrontend.java`, and `HandwrittenMolangAstParserFrontend.java`.
- Corpus/harness work exists in tests: `src/test/java/io/github/tt432/eyelibmolang/compiler/corpus/` and `src/test/resources/io/github/tt432/eyelibmolang/compiler/corpus/phase1/`. Phase1 starter corpus now has 33 expression rows (≥30 KR met), covering unary, comparison, for_each, return, member dot-chain, grouping, strings, array-literal reject, and binary-conditional syntax baseline families.
- Binder work exists: `compiler/binding/` with alias normalization, query projection, invalid-write diagnostics, typed deferred loop break/continue nodes, deferred notes, and normal/strict/debug diagnostic modes. Normal mode now emits `BIND_DEFERRED_UNSUPPORTED` warnings for deferred unsupported constructs, while strict/debug keep their additional overlay diagnostics. Deferred reason taxonomy now has 5 distinct types: `UNSUPPORTED_IN_THIS_SLICE`, `HOST_SHAPE_DEPENDENT`, `QUERY_VARIANT_SELECTION_DEPENDENT`, `COMPATIBILITY_POLICY_DEPENDENT`, and `DIAGNOSTICS_OVERLAY_OWNED_FOLLOWUP` (≥3 distinct reason types KR met).
- Mapping ports exist: `mapping/api/` plus built-in mappings in `mapping/MolangMath.java` and `mapping/MolangToplevel.java`; `HostRole<T>` is now the canonical typed host lookup key while deprecated `Class<?>` lookup remains for transitional callers.

`this` semantics are handled by the bytecode emitter (emits zero constant). Class loading uses `MethodHandles.Lookup.defineHiddenClass()` — no ClassLoader, no class name collisions, per-class GC eligibility.

## Phase Status

| Phase | Status | Evidence | Current rule |
|---|---|---|---|
| Phase 0 - Overview and boundaries | `Done / maintain` | `refactor-plan/00-overview-and-boundaries.md`, `docs/decisions/0002-module-boundaries.md`, generated-code policy | Keep boundary docs aligned when ownership changes. |
| Phase 1 - Corpus and harness | `Current / partial` | corpus loader, linter, harness, parse runner, phase1 resources, corpus tests | Continue using `./gradlew :eyelib-molang:test`; add dedicated runner command only when it exists. |
| Phase 2 - Parser and AST | `Current / partial` | `compiler/frontend/ast/`, generated-backed AST frontend, handwritten frontend, frontend tests | Keep generated parser path active; parser work must be additive and corpus-backed. |
| Phase 3 - Binder and diagnostics | `Current / partial` | `compiler/binding/`, `src/test/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinderTest.java` for typed deferred loop break/continue coverage in normal/strict/debug modes, normal deferred unsupported warnings, alias canonicalization coverage for all four roots (`q/t/v/c`), bind-shape/diagnostics/debug-trace corpus support, 5 deferred reason types (`UNSUPPORTED_IN_THIS_SLICE`, `HOST_SHAPE_DEPENDENT`, `QUERY_VARIANT_SELECTION_DEPENDENT`, `COMPATIBILITY_POLICY_DEPENDENT`, `DIAGNOSTICS_OVERLAY_OWNED_FOLLOWUP`) | Widen binder families through tests; keep unsupported semantics explicit via deferred notes plus normal warnings, and keep the typed deferred break/continue lane narrow until broader Phase 3 widening is separately planned. |
| Phase 4 - Host and query bridge | `Blocked by recorded decisions, contract test slices partial` | current `mapping/api/` ports exist, the Phase 4 decision set is recorded, `mapping/MolangHostPublicationDeterminismConflictTest.java` covers host publication determinism plus equal-tie conflict failure, `mapping/MolangCallableDiscoveryRoleContractTest.java` covers callable discovery roles, `mapping/MolangQueryVariantSelectionMatrixContractTest.java` covers query-variant matrix ordering, `mapping/MolangCallableVariantSelectionAmbiguityContractTest.java` covers variant ambiguity, `mapping/MolangCallablePublicationSignatureRoleTest.java` covers callable publication roles; bind-link and animation-clock transitional parity tests (`MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`, `MolangAnimationClockTransitionalParityContractTest`) are NOT YET IMPLEMENTED (⬜); root runtime `mc/impl/molang/mapping/MolangQueryAnimationClockRuntimeParityTest.java` and `client/animation/bedrock/BrAnimationCodecTest.java` cover animation-clock parity. | Keep broad implementation blocked; bind-link contract tests remain deferred. |
| Phase 5 - Execution and runtime semantics | `Superseded — replaced by unified bytecode compiler` | Unified compile-then-execute architecture replaces separate planning/execution phases | All execution semantics now handled by the bytecode compiler pipeline. |
| Phase 6 - Policy, specialization, cache, reporting, cutover | `Superseded — removed as over-engineered` | Policy packs, specialization, and compatibility layers removed in favor of direct bytecode compilation | Unified architecture eliminates need for separate policy/specialization layer. |
| Phase 7 - Unified Compile-Then-Execute Architecture | `Done` | `MolangCompilerImpl`, `MolangBytecodeEmitter`, `BoundMolangEvaluator` (full expression coverage), `MolangConstantExpressionEvaluator`, `MolangCompileCache` (in-memory), `MolangValue.resolveFunction()` connected | New compile pipeline handles all phases from parse to evaluation; evaluator covers all expression types; bytecode emitter handles simple arithmetic for performance. |

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
- Emit normal-mode warnings for deferred unsupported constructs, expand strict/debug coverage as overlays, and do not create a second hidden semantic system through diagnostics mode behavior.

### M4 - Resolve Phase 4 host/query gates before implementation

Before host/query bridge implementation starts, keep this roadmap and `refactor-plan/04-host-and-query-bridge.md` aligned with the recorded Phase 4 decisions and required pre-implementation test surfaces:

- Bounded callable parameter-role inference, with explicit metadata required for injected and special engine arguments.
- One unified host publication registry with internal adapter categories, not separate service-specific registries.
- A narrow bind-to-link pass between binder and specialization that resolves symbolic names to stable candidate-set refs and registry version refs.
- Required test surfaces: host publication determinism and conflict handling, callable discovery role coverage, query variant selection matrix, bind-link contract, and transitional parity subset.
- Contract test evidence landed for host publication determinism and equal-tie conflict failure in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java`, callable discovery role coverage in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableDiscoveryRoleContractTest.java`, query variant selection matrix coverage in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`, callable variant ambiguity in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableVariantSelectionAmbiguityContractTest.java`, and callable publication signature roles in `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallablePublicationSignatureRoleTest.java`. Bind-link contract tests (`MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`) and animation-clock transitional parity test (`MolangAnimationClockTransitionalParityContractTest`) are NOT YET IMPLEMENTED (⬜). Root runtime parity assertions in `src/test/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQueryAnimationClockRuntimeParityTest.java` and codec default-expression coverage in `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java` exist for animation-clock parity.

### M5 - Define execution ownership before runtime replacement

- Name the replacement execution/lowering owner before implementing cutover-sensitive runtime behavior.
- Define required v1 execution rows for short-circuit/null-propagation, control flow, and access/index behavior.
- Keep the current `this -> 0` compatibility fallback explicit and test-backed until Phase 5 promotes concrete `this` runtime semantics under a named execution owner.

## Blocked / Deferred Decisions

- Phase 4 MolangOwnerSet has been removed. HostContext exists in `mapping/api/HostContext.java` and now supports canonical `HostRole<T>` lookup alongside deprecated raw `Class<?>` lookup for transitional callers. Migration status: file deleted, semantic model partially migrated (type-safe HostRole<T> operational, downstream Class<?> callers still pending migration).
- Final cutover and deletion of generated-parser-backed compile path (deferred until downstream parity evidence is green).
- All Phase 5/6 items resolved by the unified compile-then-execute refactor — see Phase 7 status above.

## Phase 4 Recorded Decisions And Required Test Surfaces

- Callable parameter-role inference is bounded. Explicit metadata is required for injected host and special engine arguments. The first non-special host parameter may infer `RECEIVER` only when the declaration is otherwise unambiguous. Discovery fails loudly on ambiguity.
- Host publication uses one unified registry with internal adapter categories. Separate service-specific registries are not the target model.
- Phase 4 includes a narrow bind-to-link pass before specialization. Binder projects semantics, the linker resolves symbolic query and callable names to stable candidate-set refs and registry version refs, and specialization chooses the final host-shape winner.
- Required pre-implementation test surfaces: host publication determinism and conflicts, callable discovery roles, query variant selection matrix, bind-link contract, and a transitional parity subset.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java` verifies deterministic publication order for the same mapping set and loud failure for unresolved equal-tie callable conflicts.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableDiscoveryRoleContractTest.java` verifies explicit special-role metadata publication, bounded first non-special receiver inference, and loud deterministic failure for ambiguous receiver inference.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java` verifies query selection ordering (name, visible arity, visible compatibility, required host roles, specificity, priority), explicit lowest-specificity default-variant behavior, and loud failure for equal-specificity/equal-priority ambiguity.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableVariantSelectionAmbiguityContractTest.java` verifies variant-selection ambiguity handling, and `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallablePublicationSignatureRoleTest.java` verifies callable publication signature role stability. Bind-link contract tests (`MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`) and animation-clock transitional parity test (`MolangAnimationClockTransitionalParityContractTest`) are NOT YET IMPLEMENTED (⬜).

## Refactor Plan Execution Record (2026-05-03)

Evidence from refactor-plan execution:

- **P1 — Operator completeness** (✅): `<`, `<=`, `>=` operators added to handwritten tokenizer (`TokenKind.LESS`, `LESS_EQUAL`, `GREATER_EQUAL`) and parser (`parseComparison()` now matches all 6 comparison operators). Six `@ParameterizedTest`/`@CsvSource` cases in `HandwrittenMolangAstParserFrontendTest.parsesAllSixComparisonOperatorsIntoBinaryExpr()`.
- **P2 — Frontend consolidation** (✅): `MolangCompilerImpl.java` line 26 now uses `MolangParserFrontends.active()` unified entry point instead of direct `HandwrittenMolangAstParserFrontend.INSTANCE`. Arrow access bytecode stub annotated with `TODO(Phase 4)`.
- **P3 — Test expansion** (✅): New `src/test/java/io/github/tt432/eyelibmolang/compiler/MolangFullPipelineTest.java` (7 families: binary arithmetic, comparison, logical, null coalesce, strings, this, return-in-block). New `src/test/java/io/github/tt432/eyelibmolang/compiler/frontend/MolangParserFrontendDivergenceTest.java` (20+ parameterized divergence cases). Total: ~40 new test cases.
- **P4 — Documentation fix** (✅): ROADMAP file paths corrected (3 compiler files now at `compiler/` subpackage). Three missing contract test references replaced with actual file names. `compiler/diagnostic/` dead reference removed from `docs/index/molang.md` and `eyelib-molang/README.md`. MolangOwnerSet migration status accurately documented.
- **P5 — Deferred semantics diagnostics** (✅): `MolangBinder.addDeferredNote()` now emits `BIND_DEFERRED_UNSUPPORTED` WARNING in ALL modes (NORMAL/STRICT/DEBUG). Previously only STRICT mode warned. Corresponding `MolangBinderTest` assertions updated.
- **P6 — HostRole<T> implementation** (✅): New `mapping/api/HostRole.java` (type-safe key pattern per Effective Java Item 33). `HostContext` interface extended with `get/put/remove(HostRole<T>)`. Existing `Class<T>` API deprecated. `MolangScope` inline `HostContext` implementation updated with `Map<HostRole<?>, Object> hostRoleStore`.

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

### Phase 5 — Execution and Runtime Semantics (Superseded)

| KR | Threshold | Status |
|---|---|---|
| All execution KRs | Superseded by Phase 7 unified compile-then-execute | ✅ Superseded |

### Phase 6 — Policy, Specialization, Cache, Cutover (Superseded)

| KR | Threshold | Status |
|---|---|---|
| All policy/specialization KRs | Superseded — removed as over-engineered | ✅ Superseded |

## Verification Gates

- Docs-only roadmap changes: verify every referenced path still exists.
- **ALL Gradle commands must use JetBrains MCP** (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.
- Phase 1-4 implementation slices: `jetbrain_run_gradle_tasks :eyelib-molang:test`.
- Phase 7 and beyond: `jetbrain_run_gradle_tasks :eyelib-molang:test :eyelib-importer:test
 :eyelib-preprocessing:test :test`.
- Generated parser changes: require a task-specific regeneration/isolation plan and update `docs/decisions/0004-generated-code-policy.md` if the generated zone moves or changes ownership.

## Anti-Drift Checklist

Before ending any Molang refactor task, answer these in the change itself:

1. Did the task change the current phase status or next milestone? If yes, update this file.
2. Did the task implement a design draft or phase-plan promise? If yes, update the corresponding roadmap row and evidence.
3. Did the task defer a previously planned behavior? If yes, add it to `Blocked / Deferred Decisions` or the relevant phase plan.
4. Did the task change verification commands? If yes, update `Verification Gates` here and the phase plan.
5. Did the task touch generated parser output or root platform bindings? If yes, update the boundary notes that point to those paths.
