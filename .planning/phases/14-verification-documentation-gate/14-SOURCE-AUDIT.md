# Phase 14 Multi-Source Coverage Audit

## Source Items

| Source | Item | Coverage |
|--------|------|----------|
| GOAL | Maintainer can prove the particle module split preserves behavior and leaves documented architecture consistent. | 14-01 docs/evidence, 14-02 tests, 14-03 MCP matrix/closure |
| REQ | PVERIFY-01: particle-related tests moved/adapted without weakening and new split tests cover boundary/parity/regression. | 14-02, 14-03 |
| REQ | PVERIFY-02: maintainer can verify via JetBrains MCP Gradle checks, applicable ClientSmoke flow, and separate hardware checklist. | 14-01, 14-03 |
| RESEARCH | Evidence-first planning; no broad runtime feature work. | 14-01 through 14-03 |
| RESEARCH | JUnit/source-scan/fixed docs test patterns. | 14-02 |
| RESEARCH | JetBrains MCP-only Gradle matrix and broad root triage. | 14-03 |
| CONTEXT | D-01 no weakening existing particle tests. | 14-02 |
| CONTEXT | D-02 complete split coverage categories. | 14-02, 14-03 |
| CONTEXT | D-03 broad root cleanup only for particle split regressions; unrelated failures residual. | 14-03 |
| CONTEXT | D-04 existing JUnit 5 style. | 14-02 |
| CONTEXT | D-05 final ownership story. | 14-01 |
| CONTEXT | D-06 docs checked/updated list. | 14-01, 14-02 |
| CONTEXT | D-07 source tests must not depend on `.planning`. | 14-01, 14-02 |
| CONTEXT | D-08 future/manual work remains separate. | 14-01, 14-03 |
| CONTEXT | D-09 Gradle via JetBrains MCP only. | 14-03 |
| CONTEXT | D-10 minimum automated matrix. | 14-03 |
| CONTEXT | D-11 broad root `:test` triage. | 14-03 |
| CONTEXT | D-12 NullAway only if null-safety-sensitive code changes. | 14-03 |
| CONTEXT | D-13 ClientSmoke only where existing hooks apply. | 14-01, 14-03 |
| CONTEXT | D-14 hardware/manual visual checks separate. | 14-01, 14-03 |
| CONTEXT | D-15 Windows hardware exit-code capture manual/deferred. | 14-01, 14-03 |
| CONTEXT | D-16 PVERIFY evidence. | 14-03 |
| CONTEXT | D-17 summarize Phase 8-13 verified truths. | 14-01, 14-03 |
| CONTEXT | D-18 maintainer-oriented exact results, docs, ClientSmoke/hardware, residuals, closure rationale. | 14-01, 14-03 |

## Exclusions

- PFUT-02 packet-contract relocation is documented/recorded only, not implemented.
- PFUT-03 independent particle artifact publication is documented/recorded only, not implemented.
- Windows hardware exit-code capture remains manual/deferred.
- Unrelated broad root fixture cleanup is not planned unless it blocks particle-gate evidence.

## Audit Result

All GOAL, REQ, RESEARCH, and CONTEXT source items are covered by at least one executable plan. No unplanned source items remain.
