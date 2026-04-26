# Design Memo - Phase 4 Animation Clock Transitional Parity Slice

## Scope (exact)
- This slice is only for the animation-clock property-query group:
  - `query.anim_time`
  - alias `query.life_time`
  - `query.delta_time`
- Transitional parity only. No explicit-call parity, no role-aware parity, no MC runtime-bridge query parity, no wider runtime replacement.

## Grounding anchors used
- Plan/phase docs:
  - `.sisyphus/plans/eyelib-molang-phase4-animation-clock-parity.md`
  - `eyelib-molang/ROADMAP.md`
  - `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`
- Runtime anchors:
  - `src/main/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQuery.java`
  - `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java`
  - `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrClipExecutor.java`
  - `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrClipStateOwner.java`
  - `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java`
- Existing Phase 4 contract tests (context to preserve):
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java`
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`

## Current shipped behavior to treat as parity baseline
1. `query.delta_time` returns `BrAnimationEntry.Data#deltaTime()` and falls back to `0F` when no `BrAnimationEntry.Data` owner is present.
2. `query.anim_time` returns `BrAnimationEntry.Data#animTime()` and falls back to `0F` when no `BrAnimationEntry.Data` owner is present.
3. `query.life_time` is an alias of `query.anim_time` (same mapped method behavior).
4. The animation runtime state consumed by these queries is the clip state copied into `BrAnimationEntry.Data` via `BrClipStateOwner#syncStateFields()` from playback state (`animTime`, `deltaTime`, plus other counters).
5. During clip tick, `BrClipExecutor` injects `BrAnimationEntry.Data` into scope owners before evaluating `anim_time_update`, then syncs state fields after playback tick.
6. Default expression path exists in codec: missing `anim_time_update` defaults to `new MolangValue("query.anim_time + query.delta_time")`.

## Smallest parity contract for this slice
The implementation subagent must prove all items below, and nothing broader:

1. **Property-query path only**
   - All three names are treated as property-style query access (`PROPERTY`) with empty visible call-shape.
   - No explicit-call query assertions in this slice.

2. **Runtime-backed value parity**
   - With a `BrAnimationEntry.Data` owner populated from clip state, `query.anim_time` and `query.life_time` both read exactly `data.animTime()`.
   - With that same owner, `query.delta_time` reads exactly `data.deltaTime()`.
   - Without `BrAnimationEntry.Data` owner, all three read `0F`.

3. **Alias parity rule (critical)**
   - Alias parity is behavioral, not symbolic-ref identity based.
   - Do **not** require equal symbolic-name-based refs between `query.anim_time` and `query.life_time`.
   - Acceptable assertions:
     - both names resolve on the same property/empty-shape path,
     - both names produce equal runtime values for the same scope state,
     - candidate metadata is valid for each name independently.

4. **Default expression path coverage**
   - The shipped default `query.anim_time + query.delta_time` must be covered explicitly (not implied).
   - Coverage must prove that missing `anim_time_update` in codec input yields that exact default expression context string.

## Required tests/invariants for implementation review
1. A dedicated transitional parity test proves property-style parity for the three names and enforces the alias behavioral rule above.
2. Runtime state parity assertions are direct (`animTime`/`deltaTime`) and not inferred through unrelated queries.
3. Default `anim_time_update` expression is asserted explicitly as `query.anim_time + query.delta_time`.
4. Existing Phase 4 query matrix and bind-link contract tests remain intact; this slice must not rewrite their semantics.
5. No test in this slice introduces winner-selection, role-aware specialization, or explicit-call query parity requirements.

## Exact implementation allowlist for the follow-up subagent
Expected minimal allowlist (test-first, no production edits expected):

1. `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangAnimationClockTransitionalParityContractTest.java` (new)
2. `src/test/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQueryAnimationClockRuntimeParityTest.java` (new)
3. `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java` (edit for default-expression assertion)
4. `eyelib-molang/ROADMAP.md` (**only if** Phase 4 evidence/status wording changes)

Conditional-only fallback (use only if parity tests prove existing behavior is broken and include justification in review notes):

5. `src/main/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQuery.java`
6. `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java`
7. `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrClipExecutor.java`
8. `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrClipStateOwner.java`

No other files are in scope.

## Explicit deferrals (must remain deferred after this slice)
- explicit-call parity
- role-aware parity / role-aware specialization behavior
- MC runtime-bridge query parity (`partial_tick`, `distance_from_camera`, `actor_count`, `time_of_day`, `moon_phase`, etc.)
- winner selection / specialization pipeline replacement
- broader compile/runtime replacement and Phase 5 execution semantics
- Phase 6 policy/specialization-cache/cutover work
- generated parser changes
