# Eyelib Molang Phase 4 - Callable Bind-Link Bridge Design Brief

## Scope
- Module: `:eyelib-molang` only.
- Slice type: additive Phase 4 bind-link scaffolding for **callable-side** linking.
- Keep current runtime/compile ownership unchanged (`MolangCompileHandler` / `MolangCompileVisitor` / parser frontend / root `mc/impl/molang/**` untouched).
- Mirror existing query-side pattern (`MolangQueryBindLinkContract` + `MolangQueryBindLinker`) for callables.

## Grounding (current code)
- Planning anchors: `.sisyphus/plans/eyelib-molang-phase4-callable-bind-link.md`, `eyelib-molang/ROADMAP.md`, `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`.
- Existing query bind-link seam:  
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/MolangQueryBindLinkContract.java`  
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/MolangQueryBindLinker.java`
- Existing binder request production:  
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`  
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BindResult.java`
- Existing callable registry and ordering/roles:  
  - `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`

## Contract Shape Decision
**Decision: add parallel callable-specific contract structures (do not extend/overload query-specific records).**

Reasoning:
1. `QueryBindLinkRequest` is query-projection specific (`querySurfaceKind`, `query.*` canonical checks) and would become conflated if reused directly for general callables.
2. Parallel callable records keep query vs callable symmetry in shape while keeping ownership boundaries explicit.
3. This avoids risk to existing query contract tests while allowing additive callable evolution.

## Initial Migrated Callable Subset
- `MolangMath` subset (binder->link flow):
  - `math.sin`
  - `math.clamp`
  - `math.random`
- `MolangToplevel` subset:
  - `loop` (link-contract coverage included; binder emission from `BoundLoopExpr` stays deferred in this slice because `loop` is still a dedicated control-form path, not generic `BoundCallExpr`).

## Planned Contract Types
Add new file: `compiler/binding/link/MolangCallableBindLinkContract.java`.

Define records (mirror query contract naming/semantics):
1. `CallableBindLinkRequest(String symbolicCallableName, List<MolangMappingTree.VisibleArgumentKind> visibleCallShape)`
2. `CandidateSetRef(String value)`
3. `RegistryVersionRef(String value)`
4. `CandidateRef(String value)`
5. `CallableCandidateDescriptor(CandidateRef candidateRef, Set<MolangFunction.ParameterRole> requiredHostRoles, MolangMappingTree.FunctionInfo callableDescriptor)`
6. `CallableLinkResult(CandidateSetRef candidateSetRef, RegistryVersionRef registryVersionRef, String symbolicCallableName, List<MolangMappingTree.VisibleArgumentKind> visibleCallShape, List<CallableCandidateDescriptor> candidates)`

Immutability/null-guard behavior should match existing query contract style (`List.copyOf`, unmodifiable sets/lists, `Objects.requireNonNull`).

## Linker Behavior
Add new file: `compiler/binding/link/MolangCallableBindLinker.java`.

Required behavior:
1. Accept `BindResult` and link all callable requests (`bindResult.callableBindLinkRequests()`).
2. Resolve symbolic callable name via `MolangMappingTree.findMethod(symbolicCallableName)`.
3. Return stable `candidateSetRef` + `registryVersionRef` + ordered candidate descriptors.
4. Preserve symbolic callable name and visible call-shape exactly from request.
5. Expose candidate `requiredHostRoles` as non-`VISIBLE_ARG` roles.
6. **Do not** choose winner (`selectedWinner`-style field forbidden).

Candidate identity/fingerprint format should mirror current query linker (`index|owner#method(paramTypes)` and hash-based candidate-set ref) to keep deterministic behavior consistent.

## Binder Integration
Extend binder outputs (additive only):
- `BindResult`: add `List<MolangCallableBindLinkContract.CallableBindLinkRequest> callableBindLinkRequests`.
- `MolangBinder`: collect callable bind-link requests from canonical `BoundCallExpr` call sites that are **not** query-root calls.

Canonical callable symbolic-name extraction rules:
1. `BoundIdentifierExpr` callee -> `<name>` (toplevel form, e.g. `loop`).
2. `BoundMemberAccessExpr` chain -> `<root>.<member...>` (e.g. `math.sin`).
3. If root canonicalizes to `query`, skip callable request (query path already owns it).

Visible-call-shape derivation should reuse current binder inference behavior (`NUMBER` / `STRING` / `BOOLEAN`; unknown -> `null` leading to loud invalid-shape failure in linker).

## Failure Modes (must be loud)
1. **Unresolved symbolic callable**: linker throws `IllegalStateException` with callable name + registry version context.
2. **Invalid callable symbolic name** (blank/non-canonical for this slice): linker throws `IllegalArgumentException`.
3. **Invalid/incomplete visible call-shape** (`null` entries): linker throws `IllegalArgumentException`.
4. **Equal-specificity/equal-priority ambiguity**: must remain loud per mapping selection contract (no silent fallback).  
   This slice must keep/add callable-named test evidence for loud ambiguity behavior while still keeping winner selection out of bind-link.

## Tests
### New tests
1. `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java`
   - preserves symbolic callable name and visible call-shape (`math.sin`, `math.clamp`, `math.random`, `loop` request coverage)
   - exposes stable non-null `candidateSetRef` and `registryVersionRef`
   - exposes required host-role metadata per candidate
   - fails loudly on unresolved callable symbolic name
   - fails loudly on invalid visible call-shape
   - verifies no-winner contract (`CallableLinkResult` must not contain winner field)

2. Callable ambiguity contract assertion (new callable-focused test class or extension of existing mapping contract tests):
   - equal-specificity/equal-priority callable ambiguity fails loudly
   - no fallback behavior introduced

### Existing tests to keep green
- `MolangQueryBindLinkContractTest`
- `MolangQueryVariantSelectionMatrixContractTest`
- `MolangCallableDiscoveryRoleContractTest`
- `MolangHostPublicationDeterminismConflictTest`

## Touched Files (implementation allowlist)
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/BindResult.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/MolangCallableBindLinkContract.java` (new)
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/MolangCallableBindLinker.java` (new)
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java` (new)
- callable ambiguity test location under `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` (new or existing test extension)

## Forbidden Areas
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/**`
- `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**`
- `eyelib-molang/ROADMAP.md` (no update in this design-only slice)

## Implementation Checklist
1. Add callable bind-link contract records under `compiler/binding/link/`.
2. Add callable linker with deterministic candidate-set and registry-version refs.
3. Extend `BindResult` with callable request list.
4. Extend `MolangBinder` to emit callable requests for non-query `BoundCallExpr` sites.
5. Add contract tests for callable bind-link (`MolangMath` + `MolangToplevel` subset names above).
6. Add/extend callable ambiguity loud-failure test (equal specificity + equal priority).
7. Confirm no winner-selection field/logic appears in callable linker contract output.
8. Confirm forbidden files/paths are untouched.

## Verification
- Run via IDE Gradle tooling: `:eyelib-molang:test`
