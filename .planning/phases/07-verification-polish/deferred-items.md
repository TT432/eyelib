# Deferred Items — Phase 07

Items discovered during execution that are out of scope for the current plan.
These should be addressed in a future phase.

## Pre-existing Test Failures (discovered in Plan 07-01)

The full `:eyelib-clientsmoke:test` suite (84 tests) revealed 3 failures that pre-date Plan 07-01:

| Test | Line | Issue |
|------|------|-------|
| `ClientSmokeExitCodeTest > buildJUnitXml contains XML declaration` | 147 | Assertion failure — XML declaration not found in JUnit XML output |
| `ClientSmokeExitCodeTest > buildJUnitXml contains testsuite element with all required attributes` | 160 | Assertion failure — testsuite element missing expected attributes |
| `ClientSmokeStatePhase3Test > handleExit: source file contains EXIT_AFTER_SMOKE config gating check` | 277 | Source file assertion failure — `EXIT_AFTER_SMOKE` not found in state machine source |

These failures are not caused by Plan 07-01 changes (no production code was modified). They were present before this plan executed.
