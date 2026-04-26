# Phase 4 - Host And Query Bridge

## Goal
- Replace the current mixed owner-bag model with explicit host publication, callable discovery, a narrow bind-link handoff, and query variant dispatch seams.

## Source Docs
- `eyelib-molang/design/host-injection-api-draft.md`
- `eyelib-molang/design/host-adapter-registry-draft.md`
- `eyelib-molang/design/callable-discovery-annotation-draft.md`
- `eyelib-molang/design/query-variant-registry-draft.md`
- `eyelib-molang/design/shared-vocabulary-and-phase-ownership-draft.md`

## Current Anchors
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangOwnerSet.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingDiscovery.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangQueryRuntimeBridge.java`

## In Scope
- Host-role vocabulary and publication model
- Adapter registry and callable discovery normalization
- Narrow bind-to-link handoff before specialization
- Query variant registration and shape-aware dispatch inputs
- Transitional compatibility with the current mapping discovery surface

## Out Of Scope
- Minecraft-side runtime publication changes in root `mc/impl/molang/**`
- Final policy-pack-driven specialization
- Broad cutover of all existing queries in one step

## Deliverables
- Engine-local host publication contracts
- Discovery output model for callables/query variants
- Bind-link contract with stable candidate-set refs and registry version refs
- A migration bridge that lets current mappings feed the new contracts incrementally
- Initial high-value query migrations proving the model

## Acceptance Criteria
- Publication determinism is proven: the same inputs produce the same published roles and selection order.
- Exclusive-role conflicts and equal-specificity/equal-priority ties fail loudly in tests instead of falling through hidden runtime behavior.
- Callable discovery proves the bounded inference rule: explicit metadata is required for injected host and special engine arguments, and receiver inference only applies to the first non-special host parameter when it is unambiguous.
- The bind-link handoff resolves symbolic query and callable names to stable candidate-set refs and registry version refs before specialization.
- The initial migrated mappings prove contract-level parity with the current mapping discovery surface for the chosen slice before widening coverage.

## Non-Negotiable Host / Query Contracts
- `HostRole`, `HostContext`, and `HostShape` remain the canonical semantic model. Raw-class lookup and `MolangOwnerSet` scanning may survive only as transitional compatibility seams, not as the new design center.
- Host publication uses one unified registry with internal adapter categories. Inheritance is only consulted during publication, publication-site roles such as `SELF_ENTITY` or `TARGET_ENTITY` must be materialized here, and conflicting exclusive-role publication fails loudly.
- Discovery produces canonical descriptors rather than runtime policy: exported names are explicit, every parameter resolves to exactly one semantic role, injected host and special engine arguments require explicit metadata, and the first non-special host parameter may infer `RECEIVER` only when the declaration is otherwise unambiguous. Narrow inference failure beats silent guessing.
- Query-registry descriptor shapes are derived from discovery output and role normalization. They are projections of the declaration model, not a second competing declaration source.
- Query/callable selection order stays explicit: binder projection -> link resolution -> exported name -> visible arity -> visible argument compatibility -> required host-role availability -> specificity -> explicit priority tie-break. Equal specificity plus equal priority is a registration/configuration error.
- If a query needs a neutral fallback, model it as an explicit lowest-specificity default variant instead of hiding fallback semantics in ad hoc runtime code.

## Transitional Parity Gate
- Before widening migrations, document which current mapping/discovery behaviors are expected to match the new host/query contracts exactly for the chosen slice.
- Any current behavior that is intentionally deferred must be marked through the v1 compatibility scope matrix instead of silently changing under the migration bridge.
- Pre-implementation test surfaces for this phase are: host publication determinism and conflicts, callable discovery roles, query variant selection matrix, bind-link contract, and a transitional parity subset.

## Entry Gates
- **Phase 4 entry gate**: implementation remains blocked until the recorded Phase 4 decisions are backed by assigned test slices.
- **Phase 4 entry gate**: callable discovery may only use the bounded inference rule described above.
- **Phase 4 entry gate**: host publication uses the unified registry model with internal adapter categories.
- **Phase 4 entry gate**: the bind-link handoff exists as a narrow projection step, not as a second competing semantic dispatcher.

## TDD Slices
1. Add tests for host publication determinism and conflict handling.
2. Add tests for callable discovery roles, including bounded receiver inference and explicit metadata for injected and special args.
3. Add tests for query variant selection against small host-shape matrices.
4. Add tests for the bind-link contract, including stable candidate-set refs and registry version refs.
5. Add a transitional parity subset before widening coverage.

## Verification Gate
- Docs-only changes: verify referenced paths resolve before any implementation slice begins.
- Implementation slices: `./gradlew :eyelib-molang:test`

## Exit Criteria
- Host roles, adapters, and query variants are explicit engine contracts.
- `MolangOwnerSet` is demoted to a compatibility seam instead of remaining the primary design center.
- Binder output can hand off enough structure for later specialization without bespoke runtime reach-through.
- The binder-to-link handoff is explicit, stable, and tested.
- Tests cover ambiguity handling, deterministic publication/query selection, and at least one explicit default-variant path where fallback semantics matter.
