# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-10)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；工具代码共享必须形成清晰 Gradle 模块边界，消除 root util 包集群和子模块间重复的共享代码。
**Current focus:** Phase 15 — Pre-Migration Audit & Routing (v1.3)

## Current Position

Phase: 15 of 21 (v1.3 分离 eyelib-util 模块)
Plan: 0 of TBD in current phase
Status: Ready to plan
Last activity: 2026-05-10 — Roadmap created; v1.2 shipped; v1.3 milestones defined

Progress: [████████████░░░░] 67% (14/21 phases complete)

## Performance Metrics

**Velocity:**
- Total plans completed: 40 across v1.0–v1.2
- Average duration: N/A (not tracked)
- Total execution time: N/A (not tracked)

**By Phase:**
*(See ROADMAP.md progress table for per-phase plan counts)*

**Recent Trend:**
- v1.2 shipped 7 phases (22 plans) in ~1 day
- Trend: Stable

*Updated after each plan completion*

## Accumulated Context

### Decisions

Recent decisions affecting current work:
- v1.2 Phase 8: `eyelib-particle` as real module boundary with Forge-aware Gradle skeleton
- v1.3 Key Decision: `eyelib-util` as Forge module — may depend on MC/Forge, not artificially constrained to be pure Java
- v1.3 Key Decision: Package namespace `io.github.tt432.eyelibutil` — no split packages with root
- v1.3 Key Decision: Single-consumer code moves to functional owner, not eyelib-util

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 19 (Codec Infrastructure): 20+ consumer import sites across animation, behavior, particle domains require careful IDE refactoring strategy
- Phase 20 (Submodule Centralization): StreamCodec relocation requires coordinating eyelib-attachment packet codecs
- Phase 15 (Pre-Migration Audit): ResourceLocations.mod() caller verification must confirm whether any of 4 known callers use `.mod()` before deciding delete vs. parameterize

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Extended Centralization | Additional submodule-duplicated code beyond StreamCodec and DispatchedMapCodec (CENT-F01) | Deferred | v1.3 |
| Dependency Scope Audit | Narrow root connection from broad `api` wiring to `implementation` for internal-only consumers (CENT-F02) | Deferred | v1.3 |
| SharedLibraryLoader Audit | Native library loading path validation after class relocation (AUDT-F01) | Deferred | v1.3 |

## Session Continuity

Last session: 2026-05-10
Stopped at: Roadmap creation complete for v1.3; ready for `/gsd-plan-phase 15`
Resume file: None
