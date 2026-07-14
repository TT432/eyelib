# Eyelib Molang Roadmap

## Purpose

- This file is the single current-state roadmap for `molang` package refactor work.
- Use the design drafts in `docs/molang/design/` for rationale and vocabulary.
- Use the phase plans in `docs/molang/refactor-plan/` for detailed execution gates.
- Use this roadmap to decide what is done, what is active now, what is blocked, and what must be updated when Molang work changes direction.

## Roadmap Update Rule

Update this file in the same change whenever Molang work does any of the following:

1. Adds, removes, renames, or re-scopes a Molang phase, milestone, gate, or verification command.
2. Promotes a design draft or refactor-plan item into implemented code/tests.
3. Moves a roadmap item between `Current`, `Next`, `Blocked`, `Deferred`, or `Done`.
4. Changes ownership between `molang/` (engine domain), `bridge/molang/` (MC adapter), or `molang/platform/` (currently leaked MC bindings — target: migrate to `bridge/molang/`).
5. Adds a new corpus layer, diagnostics mode behavior, binder family, host/query bridge, execution path, policy-pack behavior, cache behavior, or cutover mechanism.

If a Molang code change does not update this roadmap, it must be because the change is an implementation detail that leaves all milestones, gates, and ownership unchanged.

## Source Of Truth Order

When documents disagree, resolve them in this order:

1. `docs/molang/ROADMAP.md` for current progress and next actions.
2. `docs/molang/refactor-plan/README.md` and phase files for gates and phase-level acceptance criteria.
3. `docs/molang/design/README.md` and design drafts for rationale and candidate architecture.
4. `docs/decisions/`, `MODULES.md`, and package READMEs for navigation and ownership summaries.

Design drafts are not implementation commitments until this roadmap or the refactor plan promotes them.

## Stable Boundaries

- Engine-owned code lives under `src/main/java/io/github/tt432/eyelib/molang/`.
- Handwritten parser is the sole frontend; all parser artifacts live under handwritten packages.
- `src/main/java/io/github/tt432/eyelib/bridge/molang/MolangQuery.java` holds root-coupled query functions (animation controller, variant) — ported from the old `molang/mapping/` location per ADR-0014 + ADR-0010 Port design.
- `src/main/java/io/github/tt432/eyelib/molang/platform/**` owns Minecraft/Forge platform bindings and lifecycle hooks.

## Current Implementation Snapshot

Evidence from the current tree:

- Runtime/value core exists: `MolangValue*.java`, `MolangScope.java`, and `type/` value objects.
- Unified compile-then-execute pipeline exists: `compiler/MolangCompilerImpl.java` orchestrates compilation from AST to bytecode using `MethodHandles.Lookup.defineHiddenClass()` (Java 15+), `compiler/MolangBytecodeEmitter.java` generates JVM bytecode for ALL 24 BoundMolang expression/statement types, `compiler/MolangRuntimeSupport.java` provides static helpers for mapping-tree-dependent runtime logic (member access, function calls, index resolution), and `compiler/MolangConstantExpressionEvaluator.java` folds compile-time constants.
- **Bytecode emitter** (`MolangBytecodeEmitter.java`) handles all bound expression types: literals (number/string), unary/binary/logical/comparison operators, identifiers, assignments, member access, function calls (via `MolangRuntimeSupport`), arrow/query/index access, blocks, loop/for_each control flow, break/continue, null coalesce, deferred/unknown nodes, and all statement types. No interpreter fallback — the old `BoundMolangEvaluator.java` has been removed.
- **`MolangValue.resolveFunction()`** is fully reconnected: constant folding → bytecode compile → wrap as `CompiledMolangExpression`. No evaluator fallback.
- **Compilation cache** (`MolangCompileCache.java`) provides single-tier L1 in-memory caching with `ConcurrentHashMap`, composite key (expression + registryVersionRef) for automatic staleness detection, `computeIfAbsent` for atomic compilation, and size-bound eviction (MAX_L1_SIZE=1000). L2 disk cache (`MolangDiskCache`) has been removed as it was never populated in production.
- **MolangScope** fixes: `get()` recursively traverses full parent scope chain (not just one level); `parent` is `volatile` for thread-safe publication.
- **MolangFloat** optimization: `valueOf(float)` returns cached ZERO/ONE constants for 0f/1f to reduce per-frame allocations.
- **MolangDiskCache** (`computeFileName`) uses SHA-256 instead of 32-bit `String.hashCode()` for collision-resistant file naming.
- Old `MolangCompileHandler.java` (ANTLR-based bytecode compiler with file-based caching) has been removed.
- Handwritten parser is the sole frontend; `generated/` directory and `GeneratedMolangParserFrontend.java` have been removed.
- Handwritten parser frontend is the sole frontend: `compiler/frontend/ast/` and `HandwrittenMolangAstParserFrontend.java`.
- Corpus/harness work exists in tests: `src/test/java/io/github/tt432/eyelib/molang/compiler/corpus/` and `src/test/resources/io/github/tt432/eyelib/molang/compiler/corpus/phase1/`. Phase1 starter corpus now has 33 expression rows (≥30 KR met), covering unary, comparison, for_each, return, member dot-chain, grouping, strings, array-literal reject, and binary-conditional syntax baseline families.
- Binder work exists: `compiler/binding/` with alias normalization, query projection, invalid-write diagnostics, loop-scope validation for break/continue, deferred notes, and normal/strict/debug diagnostic modes. Normal mode emits `BIND_DEFERRED_UNSUPPORTED` warnings for remaining deferred unsupported constructs, while strict/debug keep their additional overlay diagnostics. Deferred reason taxonomy now has 5 distinct types: `UNSUPPORTED_IN_THIS_SLICE`, `HOST_SHAPE_DEPENDENT`, `QUERY_VARIANT_SELECTION_DEPENDENT`, `COMPATIBILITY_POLICY_DEPENDENT`, and `DIAGNOSTICS_OVERLAY_OWNED_FOLLOWUP` (≥3 distinct reason types KR met).
- Mapping ports exist: `mapping/api/` plus built-in mappings in `mapping/MolangMath.java` and `mapping/MolangToplevel.java`; `HostRole<T>` is now the canonical typed host lookup key while deprecated `Class<?>` lookup remains for transitional callers.

`this` semantics are handled by the bytecode emitter (emits zero constant). Class loading uses `MethodHandles.Lookup.defineHiddenClass()` — no ClassLoader, no class name collisions, per-class GC eligibility.

## Phase Status

| Phase | Status | Evidence | Current rule |
|---|---|---|---|
| Phase 0 - Overview and boundaries | `Done / maintain` | `docs/decisions/0002-module-boundaries.md`, generated-code policy | Keep boundary docs aligned when ownership changes. |
| Phase 1 - Corpus and harness | `Done / maintain` | All 4 KR ✅: corpus case IDs stable, linter zero errors, parse runner 100% pass (MolangCorpusHarnessTest green), 33 starter rows. | Continue using `eyelib_debug_test`; add dedicated runner command only when it exists. |
| Phase 2 - Parser and AST | `Done` | `compiler/frontend/ast/`, handwritten frontend, frontend tests; ANTLR-generated parser removed | Handwritten parser is the sole frontend; all parser work is AST-driven. |
| Phase 3 - Binder and diagnostics | `Done / maintain` | All 5 KR ✅: alias canonicalization (q/t/v/c), 5 deferred reason types, write-target coverage (query/context/temp/variable), strict/debug ≥5 families, ≤1 alias impl. | Keep unsupported semantics explicit via deferred notes; widen binder families only with corpus-backed slices. |
| Phase 4 - Host and query bridge | `Done / maintain` | All 6 KR ✅: HostRole<T> migration complete (98ed1de9), 6 contract tests green (host publication, callable discovery, query variant, callable ambiguity, callable signature, animation-clock parity). Bind-link superseded by Phase 7. | Maintain mapping registry stability; add query/callable registrations through documented API. |
| Phase 5 - Execution and runtime semantics | `Superseded — replaced by unified bytecode compiler` | Unified compile-then-execute architecture replaces separate planning/execution phases | All execution semantics now handled by the bytecode compiler pipeline. |
| Phase 6 - Policy, specialization, cache, reporting, cutover | `Superseded — removed as over-engineered` | Policy packs, specialization, and compatibility layers removed in favor of direct bytecode compilation | Unified architecture eliminates need for separate policy/specialization layer. |
| Phase 7 - Unified Compile-Then-Execute Architecture | `Done` | `MolangCompilerImpl`, `MolangBytecodeEmitter`, `BoundMolangEvaluator` (full expression coverage), `MolangConstantExpressionEvaluator`, `MolangCompileCache` (in-memory), `MolangValue.resolveFunction()` connected | New compile pipeline handles all phases from parse to evaluation; evaluator covers all expression types; bytecode emitter handles simple arithmetic for performance. |

## Active Milestones

### M1 - Keep phase 1 corpus harness reliable

- Preserve stable corpus case IDs, lint errors, result classes, effective diagnostics mode, and effective policy-pack reporting.
- Keep phase1 corpus resources valid and runnable through `eyelib_debug_test`. Starter corpus: 33 rows (≥30 KR ✅), covering unary, comparison, for_each, return, member dot-chain, grouping, strings, array-literal reject, and binary-conditional.
- Do not promote corpus rows to cutover evidence unless they reference the V1 compatibility matrix in `docs/molang/refactor-plan/README.md`.

### M2 - Widen parser/AST only with corpus-backed slices

- Add parser acceptance/rejection cases before adding new handwritten parser behavior.
- Keep `BlockExpr` as the canonical expression-valued block node.
- Keep `loop` and `for_each` as dedicated control-form productions, not generic call-like forms.

### M3 - Widen binder diagnostics without hiding semantics

- Keep alias canonicalization, invalid-write rejection, access-family preservation, and query projection as binder-owned responsibilities.
- Represent unsupported or later-phase semantics as typed deferred nodes plus reason tokens, including the current typed deferred loop break/continue lane.
- Emit normal-mode warnings for deferred unsupported constructs, expand strict/debug coverage as overlays, and do not create a second hidden semantic system through diagnostics mode behavior.

### M4 - Resolve Phase 4 host/query gates before implementation

Before host/query bridge implementation starts, keep this roadmap and `refactor-plan/06-host-context-alignment.md` aligned with the recorded Phase 4 decisions and required pre-implementation test surfaces:

- Bounded callable parameter-role inference, with explicit metadata required for injected and special engine arguments.
- One unified host publication registry with internal adapter categories, not separate service-specific registries.
- A narrow bind-to-link pass between binder and specialization that resolves symbolic names to stable candidate-set refs and registry version refs.
- Required test surfaces: host publication determinism and conflict handling, callable discovery role coverage, query variant selection matrix, bind-link contract, and transitional parity subset.
- Contract test evidence landed for host publication determinism and equal-tie conflict failure in `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangHostPublicationDeterminismConflictTest.java`, callable discovery role coverage in `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangCallableDiscoveryRoleContractTest.java`, query variant selection matrix coverage in `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`, callable variant ambiguity in `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangCallableVariantSelectionAmbiguityContractTest.java`, and callable publication signature roles in `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangCallablePublicationSignatureRoleTest.java`. Bind-link contract tests (`MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`) and animation-clock transitional parity test (`MolangAnimationClockTransitionalParityContractTest`) are NOT YET IMPLEMENTED (⬜). Root runtime parity assertions in `src/test/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQueryAnimationClockRuntimeParityTest.java` and codec default-expression coverage in `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java` exist for animation-clock parity.

### M5 - Define execution ownership before runtime replacement

- Name the replacement execution/lowering owner before implementing cutover-sensitive runtime behavior.
- Define required v1 execution rows for short-circuit/null-propagation, control flow, and access/index behavior.
- Keep the current `this -> 0` compatibility fallback explicit and test-backed until Phase 5 promotes concrete `this` runtime semantics under a named execution owner.

## Blocked / Deferred Decisions

- Phase 4 MolangOwnerSet has been removed. HostContext exists in `mapping/api/HostContext.java` and supports canonical `HostRole<T>` lookup. **Migration completed** (commit `98ed1de9`): all static `Class<?>` callers migrated to `HostRole<T>` across 14 files. Remaining `Class<?>` API is intentionally retained for two dynamic-resolution paths: `MolangRuntimeSupport.get(role.parameterType())` (callable parameter resolution) and `RenderData.put((Class<T>) owner.getClass(), owner)` (runtime owner type). `MolangScope.HostContext` bridges `get(Class<?>)` to also search `hostRoleStore`, ensuring interop.
- All Phase 5/6 items resolved by the unified compile-then-execute refactor — see Phase 7 status above.

## Phase 4 Recorded Decisions And Required Test Surfaces

- Callable parameter-role inference is bounded. Explicit metadata is required for injected host and special engine arguments. The first non-special host parameter may infer `RECEIVER` only when the declaration is otherwise unambiguous. Discovery fails loudly on ambiguity.
- Host publication uses one unified registry with internal adapter categories. Separate service-specific registries are not the target model.
- Phase 4 includes a narrow bind-to-link pass before specialization. Binder projects semantics, the linker resolves symbolic query and callable names to stable candidate-set refs and registry version refs, and specialization chooses the final host-shape winner.
- Required pre-implementation test surfaces: host publication determinism and conflicts, callable discovery roles, query variant selection matrix, bind-link contract, and a transitional parity subset.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangHostPublicationDeterminismConflictTest.java` verifies deterministic publication order for the same mapping set and loud failure for unresolved equal-tie callable conflicts.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangCallableDiscoveryRoleContractTest.java` verifies explicit special-role metadata publication, bounded first non-special receiver inference, and loud deterministic failure for ambiguous receiver inference.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangQueryVariantSelectionMatrixContractTest.java` verifies query selection ordering (name, visible arity, visible compatibility, required host roles, specificity, priority), explicit lowest-specificity default-variant behavior, and loud failure for equal-specificity/equal-priority ambiguity.
- Completed contract evidence: `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangCallableVariantSelectionAmbiguityContractTest.java` verifies variant-selection ambiguity handling, and `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangCallablePublicationSignatureRoleTest.java` verifies callable publication signature role stability. Animation-clock transitional parity test (`MolangAnimationClockTransitionalParityContractTest`) is COMPLETED (✅), verifying HostRole put ↔ Class<?> get bridge interop. Bind-link contract tests (`MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`) are NOT YET IMPLEMENTED (⬜).

## Refactor Plan Execution Record (2026-05-03)

Evidence from refactor-plan execution:

- **P1 — Operator completeness** (✅): `<`, `<=`, `>=` operators added to handwritten tokenizer (`TokenKind.LESS`, `LESS_EQUAL`, `GREATER_EQUAL`) and parser (`parseComparison()` now matches all 6 comparison operators). Six `@ParameterizedTest`/`@CsvSource` cases in `HandwrittenMolangAstParserFrontendTest.parsesAllSixComparisonOperatorsIntoBinaryExpr()`.
- **P2 — Frontend consolidation** (✅): `MolangCompilerImpl.java` line 26 now uses `MolangParserFrontends.active()` unified entry point instead of direct `HandwrittenMolangAstParserFrontend.INSTANCE`. Arrow access bytecode stub annotated with `TODO(Phase 4)`.
- **P3 — Test expansion** (✅): New `src/test/java/io/github/tt432/eyelib/molang/compiler/MolangFullPipelineTest.java` (7 families: binary arithmetic, comparison, logical, null coalesce, strings, this, return-in-block). New `src/test/java/io/github/tt432/eyelib/molang/compiler/frontend/MolangParserFrontendDivergenceTest.java` (20+ parameterized divergence cases). Total: ~40 new test cases.
- **P4 — Documentation fix** (✅): ROADMAP file paths corrected (3 compiler files now at `compiler/` subpackage). Three missing contract test references replaced with actual file names. `compiler/diagnostic/` dead reference removed from `docs/index/molang.md` and `README.md`. MolangOwnerSet migration status accurately documented.
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
| Corpus parse runner | 100% phase1 cases pass parse (MolangCorpusHarnessTest green) | ✅ |
| Phase1 corpus rows | ≥30 expression rows covering syntax baseline | ✅ |

### Phase 2 — Parser and AST

| KR | Threshold | Status |
|---|---|---|
| AST node coverage | All syntax-baseline checklist nodes have explicit AST types (26 types in MolangAst.java) | ✅ |
| ForEachExpr binder branch | Explicit handler in MolangBinder (not generic else) | ✅ |
| Handwritten frontend coverage | ≥20 acceptance/rejection tests | ✅ |
| ANTLR-generated parser removed | All ANTLR artifacts deleted; handwritten parser is sole frontend | ✅ |

### Phase 3 — Binder and Diagnostics

| KR | Threshold | Status |
|---|---|---|
| Alias canonicalization coverage | All 4 alias roots (q/t/v/c) → canonical tested | ✅ |
| Deferred reason granularity | ≥3 distinct reason types beyond UNSUPPORTED_IN_THIS_SLICE | ✅ |
| Invalid-write diagnostics | All write-target errors tested (query/context/temp/variable) | ✅ |
| Strict/debug mode coverage | ≥5 distinct binder families tested per mode | ✅ |
| Alias logic deduplication | ≤1 implementation location (shared module) | ✅ |

### Phase 4 — Host and Query Bridge

| KR | Threshold | Status |
|---|---|---|
| Contract test count | ≥5 contract test classes green | ✅ (6 green) |
| Host publication determinism | Equal-tie conflict fails loudly (tested) | ✅ |
| Callable discovery roles | Bounded inference + loud ambiguity (tested) | ✅ |
| Query variant selection matrix | Explicit default-variant + equal-tie failure (tested) | ✅ |
| Bind-link contract | Stable candidateSetRef + registryVersionRef (tested) | ✅ Superseded — Phase 7 unified compile-then-execute resolves names at runtime via MolangRuntimeSupport; specialization layer removed |
| MolangOwnerSet→HostContext migration | All static Class<?> callers migrated to HostRole<T> (dynamic resolution paths intentionally retained) | ✅ |

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
- **通过 eyelib-debug MCP 或 bash 跑 `gradlew`** (`eyelib_debug_build`, `eyelib_debug_test` 等) 执行所有 Gradle 命令。
- 所有 phase 验证：`eyelib_debug_test`（单 Gradle project，ADR-0014 后无 `:eyelib-importer` / `:eyelib-preprocessing` 等子项目 task）。

## Anti-Drift Checklist

Before ending any Molang refactor task, answer these in the change itself:

1. Did the task change the current phase status or next milestone? If yes, update this file.
2. Did the task implement a design draft or phase-plan promise? If yes, update the corresponding roadmap row and evidence.
3. Did the task defer a previously planned behavior? If yes, add it to `Blocked / Deferred Decisions` or the relevant phase plan.
4. Did the task change verification commands? If yes, update `Verification Gates` here and the phase plan.
5. Did the task touch root platform bindings? If yes, update the boundary notes that point to those paths.
