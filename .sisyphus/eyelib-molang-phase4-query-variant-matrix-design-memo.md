# Design Memo - Phase 4 Query Variant Selection Matrix (Engine-Local Slice)

## Scope and ownership
- Affected module: `:eyelib-molang` mapping API/test surface only.
- This slice defines only the **engine-local query variant selection contract** needed by Phase 4 matrix tests.
- Keep generated parser files read-only (`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/**`).

## Required ordering contract (must be encoded exactly)
Selection order is fixed and test-owned:

1. exported name
2. visible arity
3. visible argument compatibility
4. required host-role availability
5. specificity
6. explicit priority tie-break

If multiple candidates remain after step 6 with equal specificity and equal priority, fail loudly with `IllegalStateException` (no silent winner).

## Smallest engine-local contract to implement
Grounded in existing `MolangMappingTree.FunctionInfo` and `MolangFunction.ParameterRole` behavior:

1. Keep using `MolangMappingTree` as the publication/discovery owner.
2. Add a selector API on `MolangMappingTree` that chooses one `FunctionInfo` from one exported query name using call-shape inputs only (no bind-link refs):
   - exported/qualified method name (`query.foo` form, same lookup style as `findMethod`)
   - visible argument call-shape (ordered)
   - available host roles at call site (`RECEIVER`/`INJECTED_HOST`/`SPECIAL_ENGINE_ARG` availability set)
3. Add minimal variant metadata on callable declaration (`MolangFunction`) for ordering dimensions not already represented:
   - `specificity` (integer, higher wins)
   - `priority` (integer, higher wins; used only after specificity)
4. Derive required-host-role and visible-arg descriptors from existing `FunctionInfo.parameterRoles` + method signature:
   - visible arity: count of `VISIBLE_ARG` roles (existing behavior)
   - visible arg compatibility: compare call-shape to ordered visible parameters
   - required host roles: non-`VISIBLE_ARG` roles present on the candidate

No hidden fallback path is allowed. If no candidate survives filters, return `null` (or equivalent explicit no-match result) and let callers handle it.

## Representation rules per ordering dimension
1. **Exported name**: existing `Node.actualFunctions` key (`String`) from `MolangMappingTree`.
2. **Visible arity**: existing publication signature concept (including varargs visible-base semantics).
3. **Visible arg compatibility**: deterministic comparator over visible argument positions only.
   - Minimum contract for this slice: positional compatibility over runtime-visible types/categories; do not inspect binder/link structures.
4. **Required host-role availability**: candidate-required role set must be subset of available role set from selector input.
5. **Specificity**: explicit integer metadata on variant declaration; higher is more specific.
6. **Priority tie-break**: explicit integer metadata; applied only among equal-specificity survivors.

## Mandatory test scenarios for the new dedicated matrix test class
Create one dedicated class under mapping tests (see allowlist below). It must include at least:

1. **Default variant is explicit and lowest-specificity**
   - Use one exported query name with:
     - one or more specific variants
     - one explicit default variant declared with lowest specificity (e.g., `specificity = Integer.MIN_VALUE` or another clearly lowest constant used consistently in tests)
   - Assert default variant wins **only** when higher-specificity candidates are filtered out by compatibility/role availability.
   - Assert no implicit fallback path exists outside this explicit variant.

2. **Equal-specificity + equal-priority ambiguity fails loudly**
   - Same exported name, same visible arity/compatibility/required roles, equal specificity, equal priority, different methods.
   - Assert `IllegalStateException` with message containing the qualified query name and ambiguity context.

3. Matrix ordering proof (non-ambiguous)
   - At least one test proving earlier dimensions dominate later ones (e.g., compatibility/role filter happens before specificity/priority comparison).

## Exact implementation files allowed to change in this slice
Implementation subagent may edit **only**:

1. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
2. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangFunction.java`
3. `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java` (new)
4. `eyelib-molang/ROADMAP.md` (**only if** evidence/status text changes after this slice lands)

No other production/test files are part of this slice.

## Explicit deferrals (must remain deferred)
- bind-link refs
- candidate-set refs
- registry version refs
- transitional parity subset implementation
- specialization or Phase 5/6 semantics
- root `mc/impl/molang/**` changes
- generated parser edits

## Verification requirements for the implementation slice
- Required verification command: `:eyelib-molang:test` (via IDE/JetBrains Gradle tooling).
- Existing completed Phase 4 tests must stay green:
  - `MolangHostPublicationDeterminismConflictTest`
  - `MolangCallablePublicationSignatureRoleTest`
  - `MolangCallableDiscoveryRoleContractTest`
- If this slice adds evidence or changes Phase 4 status wording, update `eyelib-molang/ROADMAP.md` in the same change.
