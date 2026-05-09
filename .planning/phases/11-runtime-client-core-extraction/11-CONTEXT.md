# Phase 11: Runtime Client Core Extraction - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 11 moves the existing executable particle runtime core, emitter/particle lifecycle, render manager behavior, and particle-specific client hook ownership under `:eyelib-particle` while preserving the existing observable client rendering behavior. This phase must keep pure particle core root/MC/Forge-clean, place any Minecraft/Forge-facing client integration in explicit side-safe adapters, and maintain the one-way dependency direction from root integration code into the particle module without a reverse dependency back to root runtime packages.

</domain>

<decisions>
## Implementation Decisions

### Runtime Extraction Boundary
- **D-01:** Phase 11 should move executable runtime ownership, not loader/publication, command syntax, or packet contract behavior. Loading/publication remains Phase 12, command/network integration remains Phase 13, and final broad verification remains Phase 14.
- **D-02:** The migration target is the existing behavior in `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/`: `BrParticleEmitter`, `BrParticleParticle`, render-manager lifecycle, and component execution semantics. Preserve Molang scope setup, curve registration, emitter age/lifetime/random variables, local-space pose behavior, emit count/remove behavior, billboard/tint/lighting handling, texture suffixing, and render-stage timing.
- **D-03:** `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` and `ParticleDefinitionAdapter` remain the canonical schema-to-runtime seam from Phase 10. Do not reintroduce a particle-module `BrParticle` duplicate or treat root `client/particle/bedrock/BrParticle` as canonical schema.
- **D-04:** Use the smallest staged extraction that keeps behavior traceable. If legacy root runtime classes must remain temporarily, they should become thin transitional adapters or compatibility targets with documented removal conditions, not long-term owners of particle business logic.

### Side And Classloading Safety
- **D-05:** Pure particle core packages in `:eyelib-particle` must remain free of root runtime, Minecraft, Forge, and `mc/impl` imports. Existing Phase 10 boundary tests establish this pattern and should be preserved or strengthened as runtime code moves.
- **D-06:** Minecraft/Forge-facing concerns introduced by this phase, including `Minecraft.getInstance()`, `Level`, `PoseStack`, `VertexConsumer`, `RenderLevelStageEvent`, `TickEvent`, `ClientPlayerNetworkEvent`, and `@Mod.EventBusSubscriber`, require explicit client integration ownership rather than placement in pure runtime/API packages.
- **D-07:** Client-only hook classes must be side-safe with `Dist.CLIENT` and must not create dedicated-server classloading regressions. Downstream planning should verify that any Forge subscriber or client adapter lives in a documented integration layer and that common/root bootstrap does not eagerly load client-only particle classes on the wrong side.
- **D-08:** Platform type adaptation remains at integration boundaries. Runtime contracts should prefer string identifiers and particle-module request/state types where practical; conversion to `ResourceLocation`, material render types, textures, and Minecraft render buffers belongs at the client rendering adapter boundary.

### Root Adapter Compatibility
- **D-09:** Existing root facades such as `ParticleSpawnService` and `ParticleLookup` remain compatibility entrypoints only while callers still depend on root paths. They must delegate into particle-module APIs/services and should not regain particle business logic during Phase 11.
- **D-10:** Packet-driven spawn/remove behavior must remain behavior-compatible through the existing string-keyed spawn id and particle id seams. Phase 11 may move the runtime implementation behind the service, but it must not change packet payload shape, command behavior, validation, or user-visible messages.
- **D-11:** `ParticleSpawnService.spawnFromPacket`, `spawnEmitter`, and `removeEmitter` compatibility should continue to work for existing callers while the canonical implementation moves toward particle-module-owned runtime services. Any retained root class must document that it is transitional and what later phase removes it.

### Render Hook Ownership
- **D-12:** The current hook behavior in `BrParticleRenderManager.ForgeEvents` is the behavior contract: render tick START removes dead emitters/particles and advances render-frame logic; client tick START advances emitter ticks; render level AFTER_ENTITIES renders particles; client logout clears emitters and particles.
- **D-13:** Render hook ownership should move out of mixed runtime classes into an explicit particle client integration layer when practical. The render manager/service may own emitter and particle collections, but Forge event subscription should be isolated so core lifecycle operations can be tested without requiring Forge event loading.
- **D-14:** Material/texture resolution must preserve the existing behavior: particle material resolves through `RenderTypeResolver`, particle texture receives `.png` suffixing, and the render buffer comes from the Minecraft buffer source at render time. Do not silently change render type selection or texture key semantics in this phase.

### Verification Expectations
- **D-15:** Verification planning for Phase 11 should include JetBrains MCP Gradle checks only. Do not run Gradle through shell.
- **D-16:** Automated checks should cover boundary regressions, side/classloading risk, adapter delegation, and behavior-preserving runtime semantics where static/unit tests can assert them. Existing particle tests must not be weakened or deleted to make extraction compile.
- **D-17:** Runtime-sensitive behavior that cannot be automatically asserted should be compiled first and deferred to the existing client smoke/dev-client or hardware checklist path in later verification, without making Phase 11 depend on manual hardware evidence before planning.

### Claude's Discretion
Implementation details not covered above are at the planner/executor's discretion, provided they preserve Phase 8-10 ownership decisions, avoid broad compatibility layers, keep side boundaries explicit, and do not degrade observable particle rendering behavior.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project And Phase Scope
- `.planning/PROJECT.md` — v1.2 milestone goal, active requirements, particle split constraints, and JetBrains MCP-only Gradle rule.
- `.planning/REQUIREMENTS.md` — PRENDER-01/PRENDER-02 requirements for Phase 11 and future-phase boundaries for loading, command/network, and final verification.
- `.planning/ROADMAP.md` — Phase 11 goal, dependency on Phase 10, success criteria, and adjacent Phase 12-14 deferrals.
- `.planning/STATE.md` — current state, accumulated Phase 8-10 decisions, and explicit Phase 11 side/classloading concern.

### Prior Phase Decisions
- `.planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md` — one-way root -> particle dependency direction and module skeleton constraints.
- `.planning/phases/09-particle-api-store-seam/09-CONTEXT.md` — particle API/store seam, string-keyed request boundary, transitional root facade rules, and runtime extraction deferral.
- `.planning/phases/10-schema-runtime-ownership-adapter/10-CONTEXT.md` — canonical importer schema owner, particle runtime definition owner, adapter seam scope, and root legacy `BrParticle` status.
- `.planning/phases/10-schema-runtime-ownership-adapter/10-VERIFICATION.md` — verified Phase 10 truths, boundary test expectations, adapter parity coverage, and root/MC/Forge-clean particle module evidence.

### Repository Boundary Rules
- `AGENTS.md` — repository editing, reading, Gradle, module update, and verification rules.
- `MODULES.md` — module inventory, particle subproject responsibility, client particle runtime row, manager/loader/registry interactions, and module update rules.
- `docs/index/repo-map.md` — repository navigation and particle-module/current-runtime starting points.
- `docs/architecture/01-module-boundaries.md` — current-to-target ownership map, particle boundary notes, root legacy `BrParticle` status, and one-way dependency rules.
- `docs/architecture/02-side-boundaries.md` — client/common/sync/dataattach side matrix, particle side rules, and explicit client packet/render service constraints.

### Particle Package Documentation
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` — particle module scope, dependency direction, integration rule, current consumers, and verification rule.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` — current root runtime boundaries, transitional facade removal conditions, and packet/runtime adaptation rule.

### Codebase Scout Maps
- `.planning/codebase/ARCHITECTURE.md` — manager/loader/lookup patterns, sync lane, client render/tick event entrypoints, and event-bus-in-domain anti-pattern.
- `.planning/codebase/INTEGRATIONS.md` — Forge/Minecraft runtime integrations, access transformers/mixins, development run configuration context, and external integration boundaries.
- `.planning/codebase/TESTING.md` — JUnit 5 conventions, fixture patterns, boundary/static test style, and JetBrains MCP-only examples.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/` already provides root-consumed API/store/spawn contracts, including `ParticleSpawnApi` and `ParticleSpawnRequest`.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` is the canonical module runtime definition with format version, identifier, material/texture, curves, events, raw components, and billboard flipbook summary.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` is the named importer-schema-to-runtime-definition seam and already fails loudly through `DataResult` for invalid required data.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/` contains the current executable runtime surface: emitter, particle instance, render manager, component dispatch, lifetime, shape, motion, appearance, rate, and local-space logic.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` is the current transitional adapter from packet spawn/remove requests into runtime emitters.
- Existing tests such as `ParticleSpawnServiceBoundaryTest` and `ParticleDefinitionBoundaryTest` provide the static boundary-test style to extend for Phase 11.

### Established Patterns
- Root runtime may consume subprojects, but subprojects must not depend back on root runtime packages.
- Client-only Forge event hooks are supposed to live in explicit integration/quarantine layers rather than mixed domain classes.
- Runtime reads and packet application should go through domain-local lookup/spawn services, not loader internals or broad singleton reach-through.
- Existing tests favor flat package-private JUnit 5 classes with descriptive method names, direct source scans for boundary invariants, and real fixtures where behavior parity matters.

### Integration Points
- `BrParticleRenderManager.ForgeEvents` currently owns the event hooks that Phase 11 must preserve or relocate: render tick START, client tick START, render level AFTER_ENTITIES, and logout cleanup.
- `ParticleSpawnService.RootParticleSpawnApi` currently pulls particle definitions through `ParticleLookup`, obtains the player `RenderData` Molang scope, constructs `BrParticleEmitter`, and delegates to `BrParticleRenderManager`.
- Rendering currently resolves material through `RenderTypeResolver`, creates a `ResourceLocation` from the particle material string, appends `.png` to particle texture keys, and renders each `BrParticleParticle` with the current `PoseStack`/`VertexConsumer`.
- `BrParticleEmitter` and `BrParticleParticle` currently depend on Minecraft runtime types (`Minecraft`, `Level`, `Entity`, `ResourceLocation`, render/light classes) and root utility classes, so Phase 11 planning must split pure runtime state from client integration deliberately rather than moving files mechanically.

</code_context>

<specifics>
## Specific Ideas

No user-only gray area remains after applying Phase 8-10 decisions. The pragmatic default is a behavior-preserving extraction with explicit side-safe adapters: move particle runtime ownership into `:eyelib-particle`, quarantine client hooks/render bindings, keep root facades transitional, and defer loading/publication plus command/network rewires to their dedicated phases.

</specifics>

<deferred>
## Deferred Ideas

- Full resource reload and registry publication rewire is deferred to Phase 12.
- `/eyelib particle` command behavior and spawn/remove packet integration rewire is deferred to Phase 13, except for preserving compatibility through existing Phase 11 adapters.
- Final test relocation/adaptation, ClientSmoke/hardware checklist decisions, and broad documentation gate are deferred to Phase 14.

</deferred>

---

*Phase: 11-Runtime Client Core Extraction*
*Context gathered: 2026-05-09*
