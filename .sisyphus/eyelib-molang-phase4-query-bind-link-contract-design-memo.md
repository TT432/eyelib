# Design Memo - Phase 4 Query-Only Bind-Link Contract Slice

## Scope
- Module: `:eyelib-molang` only.
- This memo defines the **smallest binder -> link handoff** for canonical `query.*` access.
- Deferred by design: transitional parity subset, compile/runtime adoption, and general callable linking.

## Grounding anchors (verified)
- Planning/roadmap: `.sisyphus/plans/eyelib-molang-phase4-bind-link-contract.md`, `eyelib-molang/ROADMAP.md`, `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`.
- Binder shape today: `compiler/binding/BoundMolang.java`, `BindResult.java`, `MolangBinder.java`, plus `MolangBinderTest.java`.
- Mapping/query matrix today: `mapping/api/MolangMappingTree.java`, `mapping/MolangQueryVariantSelectionMatrixContractTest.java`, `mapping/api/MolangFunction.java`.
- Context-only compile/analysis anchors (must stay untouched in this slice): `compiler/MolangCompileVisitor.java`, `compiler/MolangExpressionAnalysisVisitor.java`, `compiler/MolangCompileHandler.java`.

These anchors establish:
1. Binder already projects `query` access into `BoundQueryAccessExpr` and preserves surface kind (`PROPERTY` vs `EXPLICIT_CALL`).
2. Mapping already exposes deterministic candidate ordering metadata and query selection dimensions (`visible call-shape`, `required host roles`, `specificity`, `priority`).
3. Current compile/analysis paths still consume `findMethod/findField` directly; this slice must not adopt the new contract there.
4. Phase 4 ownership is explicit: binder projects semantics, linker resolves stable refs, specialization chooses final host-shape-aware winner.

## Smallest handoff contract (query-only)

### Binder-side handoff input (no winner selection)
For each `BoundQueryAccessExpr`, produce a link request with these fields:

- `symbolicQueryName` (`String`): canonical exported query name (`query.foo` form).
- `querySurfaceKind` (`BoundQueryAccessExpr.QueryProjectionKind`): preserve `PROPERTY` vs `EXPLICIT_CALL` exactly.
- `visibleCallShape` (`List<MolangMappingTree.VisibleArgumentKind>`): visible argument kinds in source order.
  - `PROPERTY` => empty list.
  - `EXPLICIT_CALL` => derived from visible call args for this slice.

### Link output (stable refs + candidate descriptors; still no winner)
Linking returns a query link record with these required fields:

- `candidateSetRef` (stable ref object/value): stable identity for the resolved candidate set.
- `registryVersionRef` (stable ref/version token): mapping-registry publication version used for this link.
- `symbolicQueryName` (echoed)
- `querySurfaceKind` (echoed)
- `visibleCallShape` (echoed)
- `candidates` (`List<...>`): ordered candidate descriptors for specialization, each including:
  - stable candidate identity within the set
  - required host roles (`Set<MolangFunction.ParameterRole>`, non-`VISIBLE_ARG` roles)
  - callable descriptor pointer/handle to current mapping function metadata

**Non-negotiable:** link output must not contain `selectedWinner` or equivalent final-choice field.

## Linker responsibilities and failure posture
- Resolve only canonical query symbolic names against mapping publication data.
- Materialize `candidateSetRef` + `registryVersionRef` deterministically.
- Preserve binder-projected query surface kind unchanged.
- Preserve visible call-shape and required-host-role metadata for downstream specialization.
- Fail loudly (`IllegalStateException`/`IllegalArgumentException`) for:
  - unresolved symbolic query name
  - invalid query access shape for this slice
  - invalid/incomplete visible call-shape extraction input

## Required invariants for implementation review
1. **Ownership invariant:** binder projects -> linker resolves stable refs -> specialization (later) selects winner.
2. **Stability invariant:** same mapping publication snapshot + same symbolic query input => same `candidateSetRef` under same `registryVersionRef`.
3. **Surface invariant:** `PROPERTY` vs `EXPLICIT_CALL` survives binder->link unchanged.
4. **Metadata invariant:** symbolic query name, visible call-shape, and required host roles are present in link output.
5. **No-winner invariant:** linker returns candidate set only; final winner is deferred.
6. **Boundary invariant:** compile/analysis visitors and runtime consumers remain untouched in this slice.

## Required tests (contract slice)
Add one dedicated contract test class (query-only bind-link) that proves at least:

1. `PROPERTY` query access links with empty visible call-shape and preserved surface kind.
2. `EXPLICIT_CALL` query access links with preserved visible call-shape ordering and preserved surface kind.
3. Link result includes non-null `candidateSetRef` and `registryVersionRef`.
4. Link result exposes required host roles per candidate.
5. Unresolved symbolic query name fails loudly.
6. Invalid link input/call-shape fails loudly.
7. Multi-candidate cases stay unresolved at link stage (no winner chosen in linker).

Keep `MolangQueryVariantSelectionMatrixContractTest` as context evidence for ordering semantics; do not move winner-selection logic into linker.

## Exact implementation allowlist for the follow-up subagent
Only these files may be edited in the implementation slice:

1. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BoundMolang.java`
2. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BindResult.java`
3. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
4. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/MolangQueryBindLinkContract.java` (new)
5. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/MolangQueryBindLinker.java` (new)
6. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
7. `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java` (new)
8. `eyelib-molang/ROADMAP.md` (**only if** implementation evidence/status wording changes)

No other production/test paths are allowed in this slice.

## Explicit deferrals (must remain deferred)
- Transitional parity subset
- Compile visitor / analysis visitor / compile handler adoption of this contract
- Runtime or root-module consumer adoption
- General callable linking beyond canonical query access
- Linker-side winner selection
- Phase 5 execution semantics
- Phase 6 specialization/policy/cache/cutover work
- Root `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**` changes
- Generated parser edits under `eyelib-molang/.../generated/**`
