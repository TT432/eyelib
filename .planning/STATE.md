---
gsd_state_version: 1.0
milestone: v1.2
milestone_name: 真正实现 eyelib-particle 的模块分离
status: in_progress
stopped_at: Phase 14 Plan 01 complete; ready for final split boundary tests.
last_updated: "2026-05-09T14:44:51Z"
last_activity: 2026-05-09 -- Phase 14 Plan 01 completed docs and evidence shells
progress:
  total_phases: 7
  completed_phases: 6
  total_plans: 22
  completed_plans: 20
  percent: 91
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-09)

**Core value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；粒子拆分必须形成清晰 Gradle 模块边界，同时保持现有加载、命令、网络同步、渲染行为零回归。
**Current focus:** Phase 14 — Verification & Documentation Gate

## Current Position

Phase: 14 (verification-documentation-gate) — IN PROGRESS
Plan: 1 of 3
Status: Phase 14 Plan 01 complete; execute final split boundary tests next.
Last activity: 2026-05-09 -- Phase 14 Plan 01 completed docs and evidence shells

Progress: [█████████░] 91%

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
| Phase 11-runtime-client-core-extraction P02 | 17min | 2 tasks | 21 files |
| Phase 11-runtime-client-core-extraction P03 | 36min | 2 tasks | 19 files |
| Phase 11-runtime-client-core-extraction P04 | 52min | 2 tasks | 9 files |
| Phase 11-runtime-client-core-extraction P05 | 20min | 2 tasks | 14 files |
| Phase 11-runtime-client-core-extraction P06 | 16min | 2 tasks | 14 files |
| Phase 12-loading-publication-rewire P01 | 7min | 2 tasks | 5 files |
| Phase 12-loading-publication-rewire P02 | 20min | 2 tasks | 13 files |
| Phase 12-loading-publication-rewire P03 | 8min | 2 tasks | 10 files |
| Phase 13-command-network-integration-rewire P03 | 8min | 3 tasks | 14 files |
| Phase 14-verification-documentation-gate P01 | 4min | 2 tasks | 9 files |

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
- [Phase 11-runtime-client-core-extraction]: ParticleComponentManager decodes executable components from ParticleDefinition.rawComponents() instead of introducing a particle-module BrParticle schema owner. — Preserves Phase 10 canonical schema ownership while moving executable component behavior.
- [Phase 11-runtime-client-core-extraction]: Entity-AABB shape data is routed through an optional bounds port, leaving Minecraft entity adaptation for later client integration. — Preserves shape behavior without violating pure runtime boundary rules.
- [Phase 11-runtime-client-core-extraction]: Emitter components operate on a module-owned EmitterAccess port so pure runtime code does not import root particle runtime, Minecraft, or Forge types. — Keeps runtime component behavior pure until later lifecycle and client integration plans bind platform state.
- [Phase ?]: [Phase 11-runtime-client-core-extraction]: ParticleComponentManager now registers particle-side component codecs alongside emitter components and exposes particleComponents(ParticleDefinition). — Keeps component dispatch rooted in canonical ParticleDefinition.rawComponents().
- [Phase ?]: [Phase 11-runtime-client-core-extraction]: Block-presence checks consume an optional string block-id port instead of Minecraft Level/BuiltInRegistries. — Keeps block checks platform-free until client integration binds the environment.
- [Phase ?]: [Phase 11-runtime-client-core-extraction]: ParticleParticleComponent uses a module-owned ParticleAccess port so executable particle components stay root/MC/Forge-clean. — Preserves pure particle runtime boundary while moving behavior.
- [Phase 11-runtime-client-core-extraction]: BedrockParticleRuntime creates module-owned emitters from canonical ParticleDefinition while keeping environment and spawn side effects behind pure ports. — Preserves Phase 10 canonical schema ownership and keeps runtime side effects behind pure ports for later client integration.
- [Phase 11-runtime-client-core-extraction]: BedrockParticleInstance implements the particle component access port directly so component dispatch remains root/MC/Forge-clean. — Preserves Plan 03 component access boundaries while moving particle lifecycle state into the module.
- [Phase 11-runtime-client-core-extraction]: BedrockParticleEmitter delegates particle creation through ParticleRuntimeSpawner instead of root BrParticleRenderManager. — Keeps pure lifecycle code decoupled from root render-manager singletons until Plan 05 binds the client adapter.
- [Phase ?]: [Phase 11-runtime-client-core-extraction]: ParticleRenderManager now owns module-side emitter and particle collections while Forge event subscription lives only in ParticleRenderHooks. — Keeps lifecycle behavior testable without Forge event loading.
- [Phase ?]: [Phase 11-runtime-client-core-extraction]: Minecraft render types, ResourceLocation texture suffixing, render buffers, camera transforms, tint, billboard, and light output are quarantined in BedrockParticleRenderer under the particle client integration package. — Preserves render behavior while keeping runtime/** root/MC/Forge-clean.
- [Phase 11-runtime-client-core-extraction]: Root ParticleSpawnService now constructs module BedrockParticleRuntime emitters and registers them with module ParticleRenderManager while preserving string-keyed packet entrypoints. — Completes Phase 11 root compatibility delegation while deferring loading/publication to Phase 12 and command/network rewires to Phase 13.
- [Phase 12-loading-publication-rewire]: Module loading publication stores active entries by ParticleDefinition.identifier(), while source ids remain diagnostics/report metadata.
- [Phase 12-loading-publication-rewire]: Invalid resource JSON/schema conversion failures are logged and reported without blocking valid replacement entries.
- [Phase 12-loading-publication-rewire]: Root reload now converts ResourceLocation source ids to strings and delegates parse/convert/publish ownership to ParticleResourcePublication.
- [Phase 12-loading-publication-rewire]: ParticleAssetRegistry remains only as a legacy root compatibility adapter while active publication uses ParticleDefinitionRegistry.publisher().
- [Phase 12-loading-publication-rewire]: Packet-driven spawn now looks up ParticleDefinition from the module active registry directly; legacy BrParticle conversion remains only for current root compatibility callers.
- [v1.2 Phase 12]: Post-review fixes publish addon particle files into the module active registry, route animation/controller particle effects through module ParticleDefinition lookup, and keep legacy root maps as compatibility-only. Phase 12 review is clean.
- [Phase 13-command-network-integration-rewire]: Planning locked command compatibility, string-keyed packet/delegation checks, documentation drift tests, and JetBrains MCP-only verification across 3 plans.
- [Phase 13-command-network-integration-rewire]: Final docs lock command/network ownership as root/MC adapter work: `ParticleCommandRuntime` shapes platform-free requests, `mc/impl/common/command` owns Brigadier/ResourceLocation conversion, `mc/impl/network/packet` owns packet DTO/codecs, and `NetClientHandlers` delegates through `ParticleSpawnService`.
- [Phase 13-command-network-integration-rewire]: PFUT-02 packet-contract relocation and broad ClientSmoke/hardware visual evidence remain deferred outside Phase 13; final Phase 13 evidence used JetBrains MCP Gradle tasks only.
- [v1.2 Phase 13]: Post-review fixes removed `.planning/` dependencies from runtime documentation tests and added real spawn/remove `STREAM_CODEC` round-trip coverage; Phase 13 review is clean.
- [Phase 14-verification-documentation-gate]: Plan 01 aligned stable ownership docs and created `14-HARDWARE-CHECKLIST.md` plus `14-FINAL-GATE-EVIDENCE.md`; Plan 03 must fill exact JetBrains MCP matrix results and manual/ClientSmoke status.

### Pending Todos

None yet.

### Blockers/Concerns

None currently.

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Hardware Check | Windows hardware/client verification items from v1.1 checklist | Deferred to hardware verification only where ClientSmoke/static checks cannot automatically assert runtime behavior | v1.1 close |

## Session Continuity

Last session: 2026-05-09T14:44:51Z
Stopped at: Completed 14-01-PLAN.md
Resume file: None

## Operator Next Steps

- Continue with `/gsd-execute-phase 14` for `14-02-PLAN.md` final split boundary tests.
