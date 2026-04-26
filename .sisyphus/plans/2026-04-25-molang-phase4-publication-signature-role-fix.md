# Phase 4 Publication Signature Role Fix

## Scope

Fix the callable publication/conflict path in `:eyelib-molang` so `MolangMappingTree.publicationSignature(...)` and equal-tie conflict checks honor resolved `FunctionInfo.parameterRoles()` instead of relying on legacy `pureFunction` + raw parameter count. Keep existing behavior for simple callables, and add focused mapping tests for role-aware ordering and conflict detection.

## Non-goals

- No query variants.
- No bind-link work.
- No runtime invocation semantics.
- No `HostRole` / `HostContext` / `HostShape` architecture.
- No generated parser edits.
- No root `mc/impl` edits.
- No work outside `:eyelib-molang`.
- No `eyelib-molang/ROADMAP.md` update unless phase evidence or status wording truly changes.

## Affected Paths

- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java`

## Design Constraints

- Preserve the current Phase 4 ordering; this is a publication-signature fix slice, not a broader host/query slice.
- Derive visible arity from resolved parameter roles, with `VISIBLE_ARG` contributing to publication order/conflict checks and hidden roles excluded.
- Keep simple callables unchanged when no special roles are present.
- Reuse the existing mapping discovery/publication path; do not introduce a parallel role system.

## Binary Acceptance Criteria

- PASS: publication ordering changes when hidden parameter roles are present, and the order is stable across repeated discovery.
- PASS: equal-tie conflicts fail loudly when two callables collapse to the same visible signature after role resolution.
- PASS: simple callables still publish and conflict-check exactly as before.
- PASS: all changes stay inside `:eyelib-molang` and only touch `MolangMappingTree` plus mapping tests.
- FAIL: the fix requires query variants, bind-link, runtime invocation semantics, or broad host architecture changes.

## Subtasks

### Design

- Inspect the current `publicationSignature(...)` and conflict validation path in `MolangMappingTree`.
- Confirm how `FunctionInfo.parameterRoles()` encodes `VISIBLE_ARG` versus hidden roles.
- Choose the minimal test cases that prove role-aware ordering, conflict failure, and simple-callable regression safety.

### Implement

- Update publication signature calculation to count visible args from resolved parameter roles.
- Update equal-tie conflict detection to compare the role-resolved visible signature.
- Add focused mapping tests for stable ordering and equal-tie conflict cases that include hidden roles plus visible args.

### Review

- Verify the fix did not expand into query, bind-link, runtime, or `HostRole` / `HostContext` / `HostShape` work.
- Confirm the tests demonstrate deterministic ordering and loud failure for the hidden-role collision case.
- Update `eyelib-molang/ROADMAP.md` only if the evidence or phase-status wording actually changes.

## QA Scenarios

- Scenario 1 — Stable ordering with hidden roles
  - Command: `./gradlew :eyelib-molang:test --tests io.github.tt432.eyelibmolang.mapping.MolangCallablePublicationSignatureRoleTest`
  - Coverage target: a mapping test with two discoverable callables whose hidden roles differ but visible args should produce a stable publication order.
  - Expected result: PASS when repeated runs publish the same order and the visible-arg role counts drive that order.

- Scenario 2 — Equal-tie conflict with hidden roles
  - Command: `./gradlew :eyelib-molang:test --tests io.github.tt432.eyelibmolang.mapping.MolangCallablePublicationSignatureRoleTest`
  - Coverage target: a mapping test where two callables share the same visible signature after resolving hidden parameter roles.
  - Expected result: PASS when publication throws the unresolved callable publication conflict deterministically.

- Scenario 3 — Simple callable regression
  - Command: `./gradlew :eyelib-molang:test --tests io.github.tt432.eyelibmolang.mapping.MolangCallablePublicationSignatureRoleTest`
  - Coverage target: a mapping test with no special roles, proving legacy publication behavior remains intact for plain callables.
  - Expected result: PASS when publication order and conflict behavior match the pre-fix baseline.
