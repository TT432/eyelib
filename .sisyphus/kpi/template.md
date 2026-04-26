# KPI Session Record: YYYY-MM-DD

## Session Context
- **Date**: YYYY-MM-DD
- **Session type**: [implementation / analysis / bugfix / review]
- **Phases touched**: [Phase N]
- **Files changed**: [count]

## D1 — Build Health

| Check | Result | Exit Code | Notes |
|---|---|---|---|
| `lsp_diagnostics` (changed files) | [pass/fail] | — | |
| `jetbrain_build_project` | [pass/fail] | [0/N] | |
| `jetbrain_run_gradle_tasks :eyelib-molang:test` | [pass/fail] | [0/N] | |

## D2 — ROADMAP Alignment

| Check | Answer |
|---|---|
| Anti-Drift Checklist all 5 addressed? | [yes/no] |
| ROADMAP mismatch count | [N] |
| ROADMAP updated in same change? | [yes/no/N/A] |

## D3 — Debt Clearance

| Item | Status |
|---|---|
| Debt items resolved this session | [N] |
| Debt items identified this session | [N] |
| Running total open debt | [N] |

Resolved items:
- [item from debt audit]

## D4 — Migration Progress

| Metric | Value | Trend |
|---|---|---|
| Binder defer rate (UNSUPPORTED_IN_THIS_SLICE) | [N/N estimated] | [stable/improving/worsening] |
| New corpus rows added | [N] | |
| Phase1 corpus pass rate | [N/N] | |

## D5 — Repetition Elimination

| Metric | Current | Target |
|---|---|---|
| Alias logic copies | 3 | 1 |
| Mapping lookup copies | 3 | 1 |
| Root alias map copies | [N] | 1 |

## Notes & Follow-up

- [any observations, blockers, or next-session TODOs]
