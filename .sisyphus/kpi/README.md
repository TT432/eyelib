# Per-Session KPI Tracking

## Purpose

Track eyelib-molang refactor quality and throughput per working session.
Each session must record metrics at session end. This file explains the framework;
the actual recording uses the `template.md` in this directory.

## Metric Dimensions

### D1 - Build Health
- **Diagnostic errors** (lsp_diagnostics on all changed files): target 0
- **Build exit code** (jetbrain_build_project): target 0
- **Test exit code** (`./gradlew :eyelib-molang:test`): target 0

### D2 - ROADMAP Alignment
- **Phase status accuracy**: number of known mismatches between ROADMAP claim and actual code (target 0)
- **Anti-Drift Checklist pass**: all 5 items addressed before session end (target yes)

### D3 - Debt Clearance
- **Debt items resolved this session**: count (target ≥1 per session in Phase 3/4)
- **Accumulated open debt**: running count from the debt audit

### D4 - Migration Progress
- **Binder defer rate**: `UNSUPPORTED_IN_THIS_SLICE` count / total bound nodes, trend downward
- **Corpus coverage**: new phase1 corpus rows added in session

### D5 - Repetition Elimination
- **Alias logic copies**: target 1 (single shared module), current 3
- **Mapping lookup copies**: target 1, current 3

## Recording Protocol

1. At session start: copy `template.md` as `kpi-YYYY-MM-DD-session-N.md`
2. After each implementation block: update relevant metrics
3. At session end: fill remaining D1 values, mark template complete
4. Enter summary row into `history.csv` in this directory

## History

See `history.csv` for cumulative records.
