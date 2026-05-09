---
gsd_state_version: 1.0
milestone: v1.2
milestone_name: 真正实现 eyelib-particle 的模块分离
status: executing
stopped_at: Completed 11-01-PLAN.md; ready for Phase 11 Plan 02.
last_updated: "2026-05-09T08:18:49.319Z"
last_activity: 2026-05-09 -- Phase 11 Plan 01 runtime foundation verified
progress:
  total_phases: 7
  completed_phases: 3
  total_plans: 13
  completed_plans: 8
  percent: 62
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-09)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；粒子拆分必须形成清晰 Gradle 模块边界，同时保持现有加载、命令、网络同步、渲染行为零回归。
**Current focus:** Phase 11 — Runtime Client Core Extraction

## Current Position

Phase: 11 (runtime-client-core-extraction) — IN PROGRESS
Plan: 1 of 6 complete
Status: Completed 11-01 runtime contracts/support/boundary guards; ready for Plan 02.
Last activity: 2026-05-09 -- Phase 11 Plan 01 runtime foundation verified

Progress: [██████░░░░] 62%

## Performance Metrics

**Velocity:**

- Total plans completed: 15 historical plans (v1.0-v1.1)
- Average duration: Not tracked in STATE
- Total execution time: Not tracked in STATE

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| v1.0 Phases 1-4 | 10/10 | Not tracked | Not tracked |
| v1.1 Phases 5-7 | 5/5 | Not tracked | Not tracked |
| v1.2 Phases 8-14 | 2/2 Phase 8 plans | Phase 8 complete | Not tracked |
| Phase 09-particle-api-store-seam P01 | 4 min | 2 tasks | 10 files |
| Phase 09-particle-api-store-seam P02 | 37 min | 3 tasks | 12 files |
| Phase 09-particle-api-store-seam P03 | 20 min | 2 tasks | 5 files |
| v1.2 Phases 8-14 | 5/5 Phase 8-9 plans | Phases 8-9 complete | Not tracked |
| Phase 10-schema-runtime-ownership-adapter P01 | 11 min | 2 tasks | 6 files |
| Phase 10-schema-runtime-ownership-adapter P02 | 6min | 2 tasks | 8 files |
| Phase 11-runtime-client-core-extraction P01 | 9min | 2 tasks | 9 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table. Recent decisions affecting current work:

- [v1.2]: `:eyelib-particle` must be a real Gradle module boundary, not a cosmetic package move.
- [v1.2]: Root may depend on particle; particle must not depend back on root runtime packages or root platform wiring.
- [v1.2]: Platform bindings may live in appropriate integration layers, but pure particle core/API/schema seams must stay root- and platform-clean.
- [v1.2]: All Gradle verification must be run later through JetBrains MCP only, never shell Gradle.
- [v1.2 Phase 8]: `:eyelib-particle` starts as a Forge-visible Gradle subproject skeleton with root → particle consumption and no reverse dependency on root runtime packages.
- [v1.2 Phase 8]: Phase 8 verification used JetBrains MCP `:eyelib-particle:compileJava` and root `:compileJava`; deeper particle behavior verification remains owned by later phases.
- [Phase 09-particle-api-store-seam]: Kept particle API additive inside io.github.tt432.eyelibparticle.api with generic T contracts so :eyelib-particle remains root-clean. — Preserves Phase 8 one-way dependency boundary while providing Phase 9 API/store seams.
- [Phase 09-particle-api-store-seam]: Root particle compatibility facades now delegate to module-owned ParticleStore, ParticleLookupApi, ParticlePublisher, and ParticleSpawnApi while remaining transitional. — Preserves one-way root-to-particle dependency direction and gives later phases documented removal conditions for root facades.
- [Phase 09-particle-api-store-seam]: Plan 03 kept validation-only tests/static checks for particle API/store and transitional facades. — No runtime behavior was moved; Phase 10 can use guarded seams.
- [Phase 09-particle-api-store-seam]: Delegation documentation and forbidden-import boundaries are enforced by JUnit static source checks. — Keeps Phase 9 boundary regressions inside automated JetBrains MCP Gradle verification.
- [v1.2 Phase 9]: Post-review fixes preserved particle store insertion order, expanded particle-module forbidden import scanning, and removed obsolete root `ParticleSpawnRequest` seam risk.
- [Phase 10-schema-runtime-ownership-adapter]: `:eyelib-particle` now owns `ParticleDefinition` plus `ParticleDefinitionAdapter.fromSchema(BrParticle)` as the importer-schema to runtime-definition seam, preserving importer raw components, curves, events, render parameters, and billboard flipbook summary through `DataResult` validation.
- [Phase 10-schema-runtime-ownership-adapter]: Importer BrParticle is the canonical raw particle schema/codec owner, particle ParticleDefinition is the canonical module runtime definition owner, and root client particle bedrock BrParticle remains legacy/non-canonical until later migration. — Plan 10-02 locked docs and JUnit documentation invariants for Phase 10 ownership.
- [Phase 10-schema-runtime-ownership-adapter]: ParticleDefinitionAdapter is the only documented Phase 10 particle -> importer dependency seam; boundary tests reject duplicate BrParticle and root/MC/Forge imports. — The adapter seam preserves mapped fields without moving runtime/loading/command/network behavior.
- [v1.2 Phase 10]: Post-review fixes preserved raw particle event data in importer `BrParticle.Events`, strengthened forbidden-reference scanning, and covered adapter validation branches; Phase 10 review is clean.
- [Phase 11-runtime-client-core-extraction]: Plan 01 established pure runtime contracts and support helpers under `:eyelib-particle`, with `ParticleRuntimeDefinition` wrapping canonical `ParticleDefinition` instead of introducing a duplicate `BrParticle` owner.
- [Phase 11-runtime-client-core-extraction]: Particle runtime timing now uses a module-owned `TimeSource` port and `ParticleTimer`, keeping Minecraft tick/partial-tick access outside pure runtime until client integration binds it.
- [Phase 11-runtime-client-core-extraction]: Runtime package docs and boundary tests require pure runtime cleanliness while reserving Minecraft/Forge bindings for documented client integration outside `runtime/**`.

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 11 needs side/classloading review when moving client hooks and render code.

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Hardware Check | Windows hardware/client verification items from v1.1 checklist | Deferred to hardware verification only where ClientSmoke/static checks cannot automatically assert runtime behavior | v1.1 close |

## Session Continuity

Last session: 2026-05-09T08:18:49.308Z
Stopped at: Completed 11-01-PLAN.md
Resume file: None

## Operator Next Steps

- Continue with `/gsd-plan-phase 11` or autonomous Phase 11 planning/execution.
