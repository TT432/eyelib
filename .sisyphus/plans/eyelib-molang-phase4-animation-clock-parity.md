# Eyelib Molang Phase 4 - Animation Clock Transitional Parity Slice

## Goal

Finish the last pending Phase 4 test surface by landing the smallest real **transitional parity subset**: the animation clock property-query group consisting of `query.anim_time`, alias `query.life_time`, and `query.delta_time`.

## Why this slice

- `eyelib-molang/ROADMAP.md` now shows the first four Phase 4 surfaces as landed: host publication determinism, callable discovery roles, query variant matrix, and query-only bind-link contract. The only remaining pending Phase 4 surface is the **transitional parity subset**.
- The narrowed Oracle recommendation, code exploration, and doc-gate exploration all converge on the same smallest safe subset:
  - `query.anim_time`
  - alias `query.life_time`
  - `query.delta_time`
- This subset is the smallest one that still exercises a **real shipped runtime path**:
  - property-style access
  - zero visible args
  - alias compatibility (`life_time` vs `anim_time`)
  - live animation clock state already consumed through the default clip expression `query.anim_time + query.delta_time`
- It intentionally avoids the larger risks still deferred to later work: role-aware selection, explicit-call parity, MC runtime-bridge queries, and broader execution replacement.

## Non-goals

- Do not widen beyond the animation clock property-query slice.
- Do not include explicit-call query parity.
- Do not include receiver-role or injected-host parity.
- Do not include MC runtime-bridge-heavy queries such as `partial_tick`, `distance_from_camera`, `actor_count`, `time_of_day`, or `moon_phase`.
- Do not integrate broader compile/runtime replacement or Phase 5 semantics.
- Do not change generated parser files.
- Do not edit unrelated root runtime bindings outside what is strictly necessary for this parity slice.

## Required parity acceptance

This slice must prove all of the following:

1. `query.anim_time`, alias `query.life_time`, and `query.delta_time` still read the same animation clock state that current runtime code exposes.
2. The subset remains on the **property-style / empty visible-call-shape** path already exercised by the bind-link contract.
3. Alias parity is explicit:
   - `query.life_time` remains behaviorally equivalent to `query.anim_time`
   - parity comparisons must not require identical `candidateSetRef` values when symbolic names differ
4. The shipped default expression path remains valid: `anim_time_update = query.anim_time + query.delta_time`.
5. The slice remains transitional parity only; it must not broaden into winner-selection, role-aware specialization, or explicit-call parity.

## Expected deliverable

A design->implementation->review loop that lands a narrow parity test slice proving the animation clock property-query group matches current shipped behavior while reusing the already-landed query matrix and bind-link contract work.

## Likely touched files

Primary test/runtime anchors:

- `src/main/java/io/github/tt432/eyelib/mc/impl/molang/mapping/MolangQuery.java`
- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java`
- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrClipExecutor.java`
- `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrClipStateOwner.java`
- `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationCodecTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` (new parity-focused test class or clearly justified adjacent location)
- `eyelib-molang/ROADMAP.md` (if evidence/status wording changes)

Context-only anchors:

- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryVariantSelectionMatrixContractTest.java`
- `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`

## Explicit deferrals

The implementation must keep all of these deferred:

- explicit-call parity
- role-aware query parity
- MC runtime-bridge query parity
- compile/analyzer cutover to broader new query-selection machinery beyond this subset
- winner-selection/specialization work
- Phase 5 execution replacement
- Phase 6 policy/cache/cutover work
- generated parser edits

## Subtasks

### Task A - Design

Produce a compact design memo for the animation clock transitional parity slice.

Must answer:

- What exact runtime state is the current shipped behavior for `anim_time`, `life_time`, and `delta_time`?
- Which assertions prove behavioral parity without overreaching into specialization or broader runtime replacement?
- How should alias parity be tested given that symbolic-name-based refs may differ?
- Which exact files are allowed for the implementation slice?
- What remains explicitly blocked after this slice lands?

Output:

- a memo file under `.sisyphus/`
- exact implementation allowlist
- invariant checklist for review

Verification:

- Tooling: read the design memo, `eyelib-molang/ROADMAP.md`, `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`, current animation runtime anchors, and existing query bind-link/matrix tests
- Steps:
  1. verify the memo limits the slice to `query.anim_time`, alias `query.life_time`, and `query.delta_time`
  2. verify the memo explicitly preserves property-style / empty visible-call-shape assumptions
  3. verify the memo explains alias parity without requiring identical symbolic-name-based refs
  4. verify the memo includes the shipped default expression path `query.anim_time + query.delta_time`
  5. verify the memo explicitly defers explicit-call, role-aware, and MC runtime-bridge queries
- Expected result: all five checks are satisfied before implementation starts

### Task B - Implementation

Implement the approved animation clock transitional parity slice.

Must do:

- add a dedicated parity-focused test slice for the animation clock property-query group
- prove runtime-backed parity for `query.anim_time`, alias `query.life_time`, and `query.delta_time`
- prove the shipped default expression path remains valid
- keep the implementation narrow and additive
- update `eyelib-molang/ROADMAP.md` in the same change if evidence/status wording changes
- run the smallest correct verification set through IDE/JetBrains tooling

Must not do:

- no widening to explicit-call parity
- no widening to MC runtime-bridge queries
- no winner-selection/specialization logic
- no broad root-runtime refactor
- no generated parser edits

Verification:

- Tooling: IDE diagnostics plus JetBrains Gradle/test tooling
- Steps:
  1. run diagnostics on all changed files and require zero errors
  2. run the relevant `:eyelib-molang:test` verification and any directly required root test scope justified by touched files
  3. confirm changed files remain within the approved allowlist
- Expected result: diagnostics show zero errors and all justified verification tasks exit with code `0`

### Task C - Review

Review the landed slice against the approved design memo and roadmap.

Must verify:

- the parity slice is limited to the animation clock property-query group
- `anim_time`, `life_time`, and `delta_time` parity is actually asserted, not inferred indirectly
- alias parity is checked behaviorally rather than by forcing identical symbolic-name-based refs
- the default expression path remains covered
- explicit-call / role-aware / MC runtime-bridge queries remain untouched
- `ROADMAP.md` evidence/status wording is correct

Verification:

- Tooling:
  - read the approved design memo
  - read all changed production/test files
  - read `eyelib-molang/ROADMAP.md`
  - inspect the verification task results via JetBrains tooling
- Steps:
  1. compare the memo against the landed tests and touched runtime anchors
  2. verify the tests cover the three intended queries and the default expression path
  3. verify unchanged deferred areas were not pulled into this slice
  4. verify changed files stay within the allowlist
  5. verify roadmap evidence was updated if the slice changed Phase 4 evidence/status
  6. require successful verification commands
- Expected result: all checks pass and Phase 4 has no remaining undocumented pending surface

## Initial sequencing decision

1. Momus reviews this parity plan.
2. Design subagent writes the parity memo.
3. Implementation subagent lands the parity slice and tests.
4. Review subagent validates the landed slice.
