---
gsd_state_version: 1.0
milestone: v1.4
milestone_name: 结构清理
status: planning
stopped_at: —
last_updated: "2026-05-11T00:00:00.000Z"
last_activity: 2026-05-11 — Milestone v1.4 started
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-11)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。
**Current focus:** v1.4 结构清理 — 消除非模块化残留、纠正命名语义、清理无效接口和过时文档

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-05-11 — Milestone v1.4 started

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

- v1.2 Phase 8: `eyelib-particle` as real module boundary with Forge-aware Gradle skeleton
- v1.3 Key Decision: `eyelib-util` as Forge module — may depend on MC/Forge, not artificially constrained to be pure Java
- v1.3 Key Decision: Package namespace `io.github.tt432.eyelibutil` — no split packages with root
- v1.3 Key Decision: Single-consumer code moves to functional owner, not eyelib-util
- v1.3 Phase 15: `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink` were moved to functional owner packages rather than deleted, even where consumer evidence was zero.
- v1.3 Phase 15: `docs/architecture/migration/utility-routing-manifest.md` is the routing contract for remaining util/core-util migrations.
- v1.3 Phase 16: `:eyelib-util` exists as a Forge leaf module with namespace `io.github.tt432.eyelibutil`, mod id `eyelibutil`, zero `project(...)` dependencies, and no root consumer dependency yet.
- v1.3 Phase 17: root now consumes `:eyelib-util`; time, color, loader, math, search, and collection utility categories live in `io.github.tt432.eyelibutil`, with `ListHelper` deleted and FastUtil/JOML exposed as public API dependencies where needed.
- v1.3 Phase 18: `ResourceLocations` moved to `io.github.tt432.eyelibutil.resource` without the unused root-coupled `mod(String)` method; `TexturePaths` moved to `io.github.tt432.eyelibutil.texture`; root `TexturePathHelper` and core texture wrapper were deleted.
- v1.3 Phase 19: codec infrastructure moved to `io.github.tt432.eyelibutil.codec`, `ImmutableFloatTreeMap` moved to `io.github.tt432.eyelibutil.collection`, `EitherHelper` was deleted, and root/core util Java sources are empty.
- v1.3 Phase 20: stream codec helpers moved to `io.github.tt432.eyelibutil.streamcodec`; `eyelib-attachment` and `eyelib-material` now consume `:eyelib-util`; material duplicate `DispatchedMapCodec` copies were deleted.
- v1.3 Phase 21: final static checks proved root/core util Java sources and old imports are gone; `:eyelib-util` remains leaf-only; JetBrains MCP `build` and full project rebuild passed.
- v1.3 Phase 21: obsolete root tests depending on missing local-only `test_resources/eyelib/models/skeleton.geo.json` were removed with maintainer approval.

### Pending Todos

None yet.

### Blockers/Concerns

- None.

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Extended Centralization | Additional submodule-duplicated code beyond StreamCodec and DispatchedMapCodec (CENT-F01) | Deferred | v1.3 |
| Dependency Scope Audit | Narrow root connection from broad `api` wiring to `implementation` for internal-only consumers (CENT-F02) | Deferred | v1.3 |
| SharedLibraryLoader Audit | Native library loading path validation after class relocation (AUDT-F01) | Deferred | v1.3 |

## Session Continuity

Last session: 2026-05-10
Stopped at: v1.3 complete; ready for milestone audit/completion cleanup if desired
Resume file: None

## Operator Next Steps

- Start the next milestone with /gsd-new-milestone
