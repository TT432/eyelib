# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-06)

**Core value:** @ClientSmoke 注解驱动的客户端冒烟测试，通过编译时/类加载时的注解扫描分离测试基础设施与模组运行时，杜绝意外的类加载问题。
**Current focus:** Phase 1 — Module Scaffolding + Config + Annotation Discovery

## Current Position

Phase: 1 of 4 (Module Scaffolding + Config + Annotation Discovery)
Plan: 5 written in current phase (pending execution)
Status: Planned — 5 PLAN.md files ready for execution
Last activity: 2026-05-06 — Phase 1 planned: 5 plans, 4 waves, 9 requirements covered

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: —
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| — | — | — | — |

**Recent Trend:**
- No plans executed yet.

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- **Phase structure**: 4 phases derived from research — Foundation (Phase 1) → State Machine (Phase 2) → Screenshot/Exit (Phase 3) → Test Exec/Report (Phase 4). MOD-03 placed in Phase 1 for wiring coherence.
- **Platform**: Forge 1.20.1 + legacyForge 2.0.91 + Java 17 — matches existing eyelib platform.
- **Annotation scanning**: ModFileScanData (bytecode-level ASM), never reflection-based. Proven pattern in eyelib's `ForgeMolangMappingDiscovery`.
- **Screenshot capture**: Must use `RenderLevelStageEvent.AFTER_LEVEL` (render thread), never `ClientTickEvent`.

### Pending Todos

None yet.

### Blockers/Concerns

- **Phase 2 — Forge 1.20.1 world creation API**: Exact API surface differs from 1.21.1's `WorldOpenFlows`. Needs implementation-phase verification (MEDIUM risk, pattern is proven).
- **Phase 3 — RenderLevelStageEvent availability on Forge 1.20.1**: Event exists but stage constants need verification (LOW risk, well-known event).
- **Phase 1 — Annotation scan API**: Exact invocation pattern for filtering `@ClientSmoke` from `ModList.get().getAllScanData()` needs verification during implementation (LOW risk, pattern proven in eyelib).

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-05-06
Stopped at: Phase 1 context gathered — 01-CONTEXT.md with 16 decisions
Resume file: .planning/phases/01-module-scaffolding-config-annotation-discovery/01-CONTEXT.md
