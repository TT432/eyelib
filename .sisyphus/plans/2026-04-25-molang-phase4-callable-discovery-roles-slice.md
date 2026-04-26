# Phase 4 Callable Discovery Roles Contract Slice

## Scope

Implement the smallest `:eyelib-molang` slice that locks callable discovery roles through the existing `mapping/api` and `mapping` test surfaces. Keep it centered on contract tests for role assignment, bounded inference, and loud ambiguity failure, with `MolangHostPublicationDeterminismConflictTest` as the adjacent evidence anchor.

## Non-goals

- No generated parser edits.
- No root `mc/impl` edits.
- No broad `HostRole` / `HostContext` / `HostShape` architecture.
- No work outside `:eyelib-molang`.
- No `eyelib-molang/ROADMAP.md` update unless phase evidence or status actually changes.

## Affected Paths

- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingTree.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingDiscovery.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMapping.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangFunction.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangHostPublicationDeterminismConflictTest.java`

## Design Constraints

- Preserve the current roadmap order. Phase 4 stays a contract slice, not a jump into broader host or query work.
- Keep callable role rules explicit, bounded, and test first.
- Reuse the current discovery and publication ordering rules instead of inventing a second path.
- Keep scope local to mapping annotations, discovery, and mapping tests.

## Binary Acceptance Criteria

- PASS: new or updated tests prove callable discovery role assignment on the existing mapping API surface, including explicit special role metadata and the first unambiguous non-special receiver inference.
- PASS: ambiguous callable discovery fails loudly and deterministically.
- PASS: the slice stays inside `:eyelib-molang`, with no generated parser edits and no root `mc/impl` edits.
- FAIL: the work needs broad `HostRole` / `HostContext` / `HostShape` redesign, parser regeneration, or root platform binding changes.

## Subtasks

### Design

- Read the current mapping API and the existing host publication conflict test.
- Define the smallest callable-discovery-role contract the current surface can prove.
- Pick the exact test cases for explicit roles, inferred receiver, and ambiguity failure.

### Implement

- Add the minimal production support only if the tests need it.
- Add or extend `mapping` tests to pin the callable-discovery-role contract.
- Keep the change set inside `:eyelib-molang`.

### Review

- Check that the slice did not expand into root `mc/impl`, generated parser code, or broad host architecture.
- Confirm the tests are deterministic and read like contract evidence.
- Update `eyelib-molang/ROADMAP.md` only if the phase evidence or status changes.

## Verification Commands

- `./gradlew :eyelib-molang:test`

## QA

- Scenario 1 — Explicit role metadata contract
  - Command: `./gradlew :eyelib-molang:test --tests io.github.tt432.eyelibmolang.mapping.MolangCallableDiscoveryRoleContractTest`
  - Coverage target: a mapping/api test that asserts explicit role metadata is preserved for callable discovery entries.
  - Expected result: PASS when the discovered role matches the annotation metadata exactly.

- Scenario 2 — Receiver inference contract
  - Command: `./gradlew :eyelib-molang:test --tests io.github.tt432.eyelibmolang.mapping.MolangCallableDiscoveryRoleContractTest`
  - Coverage target: a mapping test that asserts the first unambiguous non-special host parameter is inferred as the receiver.
  - Expected result: PASS when receiver inference is accepted only for the unambiguous case and remains stable across runs.

- Scenario 3 — Ambiguity-failure contract
  - Command: `./gradlew :eyelib-molang:test --tests io.github.tt432.eyelibmolang.mapping.MolangHostPublicationDeterminismConflictTest`
  - Coverage target: the existing conflict anchor plus any new callable-discovery ambiguity case in `mapping/`.
  - Expected result: PASS when ambiguous callable discovery fails loudly and deterministically.
