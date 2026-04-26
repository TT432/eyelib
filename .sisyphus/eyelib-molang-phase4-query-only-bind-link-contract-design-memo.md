# Design Memo - Phase 4 Query-Only Bind-Link Contract Slice

## Scope (exact)
- Module: `:eyelib-molang` only.
- Slice: **query-only canonical access** (`BoundQueryAccessExpr`) binder->link handoff.
- Keep compile/analysis/runtime consumers unchanged in this slice (`MolangCompileVisitor`, `MolangExpressionAnalysisVisitor`, runtime bridge).
- Keep generated parser files read-only.

## Grounding anchors used for this memo
- `.sisyphus/plans/eyelib-molang-phase4-bind-link-contract.md`
- `eyelib-molang/ROADMAP.md`
- `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BoundMolang.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BindResult.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`
- Context-only read anchors (no adoption in this slice):
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalysisVisitor.java`

## Ownership contract (non-negotiable)
1. Binder projects query semantics and preserves surface kind (`PROPERTY` vs `EXPLICIT_CALL`).
2. Linker resolves symbolic query name to stable refs (`candidate-set ref`, `registry version ref`) and emits query link metadata.
3. **Linker does not choose final winner**. Final host-shape-aware winner selection remains deferred to specialization.

## Smallest binder->link handoff shape

### Binder output shape (query-only request)
For each `BoundQueryAccessExpr`, hand off one request record with:
- `SourceSpan span`
- `QuerySurfaceKind surfaceKind` (1:1 projection of `BoundQueryAccessExpr.QueryProjectionKind`)
- `String symbolicQueryName` (canonical qualified name, e.g. `query.life_time` / `query.foo`)
- `VisibleCallShape visibleCallShape`

`VisibleCallShape` (must be explicit, stable, and serialized in tests):
- `int visibleArity`
- `List<VisibleArgumentKind> visibleKinds` (ordered; may include `UNKNOWN` for non-literal/dynamic argument expressions)

Required query-only normalization rules:
- Root aliases are already canonicalized by binder (`q` -> `query`); preserve canonical root in `symbolicQueryName`.
- `PROPERTY` projection means `visibleArity=0`, `visibleKinds=[]`.
- `EXPLICIT_CALL` projection derives `visibleArity`/`visibleKinds` from call arguments in source order.

### Link output shape (resolved query link)
Linker returns one linked query contract record with:
- `SourceSpan span`
- `QuerySurfaceKind surfaceKind` (unchanged)
- `String symbolicQueryName` (unchanged)
- `VisibleCallShape visibleCallShape` (unchanged)
- `CandidateSetRef candidateSetRef` (stable opaque ref for all candidates matching symbolic name)
- `RegistryVersionRef registryVersionRef` (stable opaque ref for mapping publication snapshot)
- `Set<MolangFunction.ParameterRole> requiredHostRoles` (deterministic set required by linked candidate set; excludes `VISIBLE_ARG`)
- `int candidateCount` (>= 1)

`CandidateSetRef` and `RegistryVersionRef` are opaque IDs in this slice (no external serialization/version-policy work).

## Resolution behavior and failure posture
1. Linker resolves by symbolic query name against `MolangMappingTree` publication snapshot.
2. If symbolic query name is unresolved, fail loudly with `IllegalStateException` containing the qualified query name.
3. If linker input is invalid for query-only contract (non-query projection payload, malformed qualified name, negative arity), fail loudly with `IllegalArgumentException`.
4. Linker must never collapse candidate set to a single winner, even when one candidate currently exists.

## Required invariants (implementation must satisfy)
1. `surfaceKind` is preserved exactly (`PROPERTY`/`EXPLICIT_CALL`) from binder request to linked output.
2. `symbolicQueryName` is canonical query-qualified and stable (`query.*`).
3. `visibleCallShape` ordering is stable and deterministic.
4. `candidateSetRef` is deterministic for same registry version + same symbolic query name.
5. `registryVersionRef` changes when mapping publication snapshot changes.
6. `requiredHostRoles` contains only non-visible roles from candidate set (`RECEIVER`, `INJECTED_HOST`, `SPECIAL_ENGINE_ARG` as applicable).
7. No winner field exists in query link output; no winner-selection side effects occur in linker.

## Required test coverage (new dedicated contract tests)
Add one dedicated test class for this slice and cover at least:

1. **Property projection handoff**
   - `q.life_time` binds to `surfaceKind=PROPERTY`, `symbolicQueryName=query.life_time`, empty visible shape.
2. **Explicit call projection handoff**
   - `q.foo(1, 's')` binds to `surfaceKind=EXPLICIT_CALL`, `symbolicQueryName=query.foo`, ordered visible shape.
3. **Stable refs emitted**
   - Linked output contains non-null `candidateSetRef` + `registryVersionRef` and deterministic value across repeated link on unchanged mapping tree.
4. **Unresolved symbolic query fails loudly**
   - Missing `query.*` name throws `IllegalStateException` with qualified name in message.
5. **No winner selection in linker**
   - For a multi-candidate query name, linked output preserves candidate-set-level metadata without choosing a single function.

Existing contract tests that must remain green:
- `MolangHostPublicationDeterminismConflictTest`
- `MolangCallablePublicationSignatureRoleTest`
- `MolangCallableDiscoveryRoleContractTest`
- `MolangQueryVariantSelectionMatrixContractTest`

## Exact implementation allowlist for this slice
Only these files may be edited/added:

1. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BoundMolang.java`
2. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BindResult.java`
3. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
4. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/QueryBindLinkContract.java` (new)
5. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/QueryBindLinker.java` (new)
6. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
7. `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java` (new)
8. `eyelib-molang/ROADMAP.md` (**only if** evidence/status wording changes)

No other production/test files are in scope.

## Explicit deferrals (must remain deferred)
- transitional parity subset logic
- compile/runtime consumer adoption (compile visitor, analysis visitor, runtime bridge)
- general callable linking beyond canonical query access
- linker-side final winner selection
- Phase 5/6 execution/specialization/cutover behavior
- root `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**` changes
- generated parser changes

## Verification requirement for implementation subagent
- Required command after implementation: `:eyelib-molang:test` (JetBrains Gradle tooling).
- If this slice adds evidence or changes Phase 4 status wording, update `eyelib-molang/ROADMAP.md` in the same change.
