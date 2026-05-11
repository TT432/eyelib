---
gsd_state_version: 1.0
milestone: v1.4
milestone_name: 结构清理
status: ready_to_plan
stopped_at: —
last_updated: "2026-05-11T00:00:00.000Z"
last_activity: 2026-05-11 — ROADMAP.md created; Phase 22 ready to plan
progress:
  total_phases: 5
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-11)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。
**Current focus:** v1.4 结构清理 — Phase 22 Analysis & Quick Wins

## Current Position

Phase: 22 of 26 (Analysis & Quick Wins)
Plan: — (not yet planned)
Status: Ready to plan
Last activity: 2026-05-11 — ROADMAP.md created for v1.4 milestone (5 phases, 9 requirements)

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 61 across v1.0–v1.3
- Average duration: N/A (not tracked)
- Total execution time: N/A (not tracked)

**By Phase:**
*(See ROADMAP.md progress table for per-phase plan counts)*

**Recent Trend:**
- v1.3 shipped 7 phases (24 plans) in ~1 day
- Trend: Stable

*Updated after each plan completion*

## Accumulated Context

### Decisions

Recent decisions affecting current work:

- v1.4 Phase ordering: Analysis → Rename → Data Relocation → Capability Migration → Documentation & Final Verification (research-backed dependency chain)
- v1.4 CODEQ-01: Interface deletion must verify zero references via ide_find_references + ide_find_implementations with project_and_libraries scope
- v1.4 MOD-01: Module rename must be atomic — settings.gradle + all build.gradle + .idea/ XML + directory rename in one operation before Gradle sync
- v1.4 MOD-02: eyelib-preprocessing must become Forge module (legacyForge plugin, mods.toml) before accepting bake code with Minecraft imports
- v1.4 MOD-03: Capability migration must split data/codec from Forge runtime wiring; use distinct namespace io.github.tt432.eyelibattachment to prevent split packages
- v1.4 Phase dependency: Phase 23 (rename) must precede Phase 24 (data relocation) because bake code targets the renamed module
- v1.4 DOCS-01: Documentation always last — describes final state after all structural changes complete

### Pending Todos

None yet.

### Blockers/Concerns

- **MOD-03 Risk (HIGH):** Capability migration has bidirectional runtime coupling between EyelibAttachableData, Forge event wiring, and network packets. Per-class audit required before any move.
- **DATA-01 Risk (HIGH):** Bake code must be audited for Minecraft/FORGE imports before deciding whether eyelib-preprocessing Forge conversion is sufficient or code must be split.
- **CODEQ-02 Scope:** Confirmed — change only database path to .cache, do not delete entire instrument/ subsystem.

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Extended Centralization | Additional submodule-duplicated code beyond StreamCodec and DispatchedMapCodec (CENT-F01) | Deferred | v1.3 |
| Dependency Scope Audit | Narrow root connection from broad `api` wiring to `implementation` for internal-only consumers (CENT-F02) | Deferred | v1.3 |
| SharedLibraryLoader Audit | Native library loading path validation after class relocation (AUDT-F01) | Deferred | v1.3 |

## Session Continuity

Last session: 2026-05-10
Stopped at: v1.3 complete; v1.4 requirements defined
Resume file: None

## Operator Next Steps

- `/gsd-plan-phase 22` — Plan Phase 22: Analysis & Quick Wins
