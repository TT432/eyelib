---
gsd_state_version: 1.0
milestone: v1.5
milestone_name: 深度结构清理
status: planning
stopped_at: —
last_updated: "2026-05-12T00:00:00.000Z"
last_activity: 2026-05-12 — Milestone v1.5 started
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-12)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。
**Current focus:** v1.5 深度结构清理

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-05-12 — Milestone v1.5 started

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 66 across v1.0–v1.4
- Average duration: N/A (not tracked)
- Total execution time: N/A (not tracked)

**By Phase:**
*(See ROADMAP.md progress table for per-phase plan counts)*

**Recent Trend:**
- v1.3 shipped 7 phases (24 plans) in ~1 day
- v1.4 started with Phase 22 complete (1 plan)
- v1.4 Phase 23 completed after atomic module rename + Forge conversion
- v1.4 Phase 24 completed after bake relocation plus controller definition migration
- v1.4 Phase 25 completed after attachment-side payload/data/codec extraction with runtime owners retained in root
- v1.4 Phase 26 completed with docs alignment plus final `test` / `nullawayMain` / rebuild verification
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

- None.

### Blockers/Concerns

- **CAP-01 Risk (HIGH):** Root `capability/` 目录仍存在，需确认 v1.4 迁移后哪些内容应保留在 root、哪些应继续迁移。
- **ANIM-01 Risk (MEDIUM):** `client/animation/` 下有 22 项文件（含 `bedrock/` 子目录），需逐项审计引用状态后删除。
- **DOCS-01 Risk (LOW):** 部分 README.md 已过时或位于空目录，需按实际状态增删改查。

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Extended Centralization | Additional submodule-duplicated code beyond StreamCodec and DispatchedMapCodec (CENT-F01) | Deferred | v1.3 |
| Dependency Scope Audit | Narrow root connection from broad `api` wiring to `implementation` for internal-only consumers (CENT-F02) | Deferred | v1.3 |
| SharedLibraryLoader Audit | Native library loading path validation after class relocation (AUDT-F01) | Deferred | v1.3 |

## Session Continuity

Last session: 2026-05-12
Stopped at: Milestone v1.5 initialization
Resume file: None

## Operator Next Steps

- Define requirements and create roadmap for v1.5 深度结构清理
