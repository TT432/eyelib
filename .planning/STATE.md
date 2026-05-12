---
gsd_state_version: 1.0
milestone: v1.5
milestone_name: 深度结构清理
status: planning
stopped_at: —
last_updated: "2026-05-12T00:00:00.000Z"
last_activity: 2026-05-12 — Roadmap created (Phases 27-30)
progress:
  total_phases: 4
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-12)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。
**Current focus:** v1.5 深度结构清理 — roadmap created, ready for phase planning

## Current Position

Phase: 27 of 30 (DOCS — 文档审计与修正)
Plan: Not yet planned
Status: Ready to plan
Last activity: 2026-05-12 — Roadmap created, phases 27-30 defined

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 66 across v1.0–v1.4
- Average duration: N/A (not tracked)
- Total execution time: N/A (not tracked)

**By Phase:**
*(See ROADMAP.md progress table for per-phase plan counts)*

**Recent Trend:**
- v1.4 shipped 5 phases (5 plans) in ~1 day
- v1.4 audit passed with 9/9 requirements, 5/5 phases, 25/25 integration checks
- Trend: Stable

*Updated after each plan completion*

## Accumulated Context

### Decisions

Recent decisions affecting current work:

- v1.5 Phase ordering: DOCS → ANIM → PREP+DUP → CAP (research-backed, strict dependency chain)
- DOCS first: zero risk, no compilation impact, establishes accurate baseline
- ANIM second: dead code deletion changes file landscape, must finish before scans
- PREP+DUP combined: both read-only analysis, avoid redundant traversal, inform CAP
- CAP last: most complex, requires full context from all prior phases
- PIT-01: Clean build before ANIM to purge stale .class files (prevents reference false positives)
- PIT-02: Per-file IDE Find References on bedrock/ before any deletion (BrClipExecutor/BrControllerExecutor are active)
- PIT-03: EyelibAttachableData stays in root — it's a Forge registry hub, not a data type
- PIT-04: EntityBehaviorData codec extraction deferred to post-v1.5 (MolangQuery coupling risk)

### Pending Todos

- None.

### Blockers/Concerns

- **CAP-01 Risk (HIGH):** EntityBehaviorData codec boundary needs careful analysis — MolangQuery coupling may block extraction. Mitigation: defer to post-v1.5 spike.
- **ANIM-01 Risk (MEDIUM):** `client/animation/` has 22+ items including bedrock/ runtime. Must verify each file before deletion. Mitigation: per-file IDE Find References.
- **Stale .class Risk (LOW):** bin/ may contain classes from already-deleted sources. Mitigation: clean build before Phase 28.

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Capability | EntityBehaviorData codec extraction (CAP-F01) | Deferred | v1.5 |
| Capability | Capability runtime owner 完全分离 (CAP-F02) | Deferred | v1.5 |
| Extended Cleanup | root util/ 残留工具代码最终清理确认 (CLEAN-F01) | Deferred | v1.5 |
| Extended Cleanup | grammer/ 和 generated/ 标记包生命周期决策 (CLEAN-F02) | Deferred | v1.5 |
| Extended Centralization | Additional submodule-duplicated code (CENT-F01) | Deferred | v1.3 |
| Dependency Scope Audit | Narrow root connection to implementation (CENT-F02) | Deferred | v1.3 |
| SharedLibraryLoader Audit | Native library loading path validation (AUDT-F01) | Deferred | v1.3 |

## Session Continuity

Last session: 2026-05-12
Stopped at: Roadmap creation complete (Phases 27-30)
Resume file: None

## Operator Next Steps

- `/gsd-plan-phase 27` — Plan Phase 27 (DOCS: 文档审计与修正)
