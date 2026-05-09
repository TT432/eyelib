---
phase: 11-runtime-client-core-extraction
created: 2026-05-09
status: complete
requirements: [PRENDER-01, PRENDER-02]
---

# Phase 11 Research — Runtime Client Core Extraction

## Research Complete

Phase 11 is an in-repository boundary extraction, not a new-library selection. Discovery level is **Level 0/1**: all implementation patterns already exist in the codebase, and the only external APIs involved are the existing Forge/Minecraft client render/tick APIs already used by the current runtime.

## Locked Decisions Applied

- D-01: Keep loading/publication for Phase 12, command/network rewire for Phase 13, and broad final verification for Phase 14.
- D-02: Preserve the current behavior in `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/`, including Molang scope setup, curves, emitter/particle random variables, local-space pose behavior, emit/remove behavior, billboard/tint/lighting, texture suffixing, and render-stage timing.
- D-03: Use `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` and `ParticleDefinitionAdapter` as the schema-to-runtime seam; do not add a particle-module `BrParticle` duplicate.
- D-05/D-06/D-07/D-08: Keep pure particle core packages root/MC/Forge-clean and put Minecraft/Forge-facing hooks/render bindings in explicit side-safe client integration packages.
- D-09/D-10/D-11: Root facades remain transitional compatibility entrypoints and continue packet-driven string-keyed spawn/remove behavior.
- D-12/D-13/D-14: Preserve render hook lifecycle and material/texture resolution semantics while isolating Forge event subscription from testable lifecycle operations.
- D-15/D-16/D-17: Verification must use JetBrains MCP Gradle tasks only and include boundary, side/classloading, adapter delegation, and static/runtime-semantics tests.

## Existing Runtime Surface

Current executable behavior is concentrated in:

- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleEmitter.java`
  - Owns emitter Molang scope, curve registration, age/lifetime/random variables, local-space pose, emit count, direction application, and particle creation.
  - Current root-only dependencies to replace or quarantine: `io.github.tt432.eyelib.mc.impl.util.time.FixedTimer`, `io.github.tt432.eyelib.util.Blackboard`, `io.github.tt432.eyelib.util.ResourceLocations`, `io.github.tt432.eyelib.util.math.EyeMath`, plus Minecraft `Minecraft`, `Level`, `Entity`, and `ResourceLocation`.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleParticle.java`
  - Owns particle Molang scope, particle lifetime/random variables, appearance component lookup, velocity/state, remove callback, billboard transform/render, tinting, and lighting.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java`
  - Owns global emitter/particle collections and static lifecycle methods.
  - Nested `ForgeEvents` currently handles render tick START cleanup/render-frame, client tick START emitter tick, render level AFTER_ENTITIES rendering, and logout cleanup.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java`
  - Transitional root facade converts packet data to module `ParticleSpawnRequest`, looks up root legacy `BrParticle`, obtains `RenderData` Molang scope, constructs `BrParticleEmitter`, and delegates to `BrParticleRenderManager`.

## Recommended Extraction Shape

Use a staged extraction that creates module-owned runtime services without weakening side boundaries:

1. **Module runtime contracts and support utilities**: create particle-module-owned timer/blackboard/math/key helpers and service contracts so moved code stops importing root utility or `mc/impl` classes.
2. **Component execution port**: move emitter/particle component execution into `:eyelib-particle` using `ParticleDefinition.rawComponents()` and importer `BedrockResourceValue` as the source data. Decode into typed component classes without introducing a module `BrParticle` type.
3. **Lifecycle runtime**: port emitter/particle state and Molang scope setup to module-owned classes that consume `ParticleDefinition` and the module component registry.
4. **Client integration adapter**: place Minecraft/Forge render bindings under an explicit client integration package, with `Dist.CLIENT` event subscribers and no event subscription in pure runtime classes.
5. **Root compatibility**: keep `ParticleSpawnService` and any root `BrParticleRenderManager` path as thin transitional adapters, delegating to particle-module runtime services and documenting removal conditions.

## Validation Architecture

Nyquist validation should sample every boundary where behavior can regress:

- **Boundary static tests**: scan `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/**` for root/MC/Forge imports while allowing Minecraft/Forge only under `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/**` or equivalent documented integration package.
- **No duplicate schema owner test**: reject `record BrParticle` and `class BrParticle` in `:eyelib-particle` sources; runtime classes must consume `ParticleDefinition`.
- **Lifecycle unit tests**: assert emitter and particle Molang variable registration, random variables, age/lifetime, emit count/remove callback, local-space flags, and component dispatch without Forge event loading.
- **Render adapter static tests**: assert `RenderTypeResolver.resolve(new ResourceLocation(material))`, `.withSuffix(".png")`, `RenderLevelStageEvent.Stage.AFTER_ENTITIES`, and `TickEvent.Phase.START` remain present in the client integration layer.
- **Delegation tests**: assert root `ParticleSpawnService` delegates to module services and packet payload shape remains string-keyed.
- **Compile/test gates**: use JetBrains MCP `jetbrain_run_gradle_tasks`, never shell Gradle.

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Moving runtime mechanically imports root utilities into `:eyelib-particle` | Create module-owned support helpers first and strengthen boundary tests before moving runtime code. |
| Forge event subscriber remains mixed into lifecycle manager | Split lifecycle service from event hook class; event hook delegates to service methods. |
| Particle module gains a duplicate `BrParticle` schema owner | Runtime classes consume `ParticleDefinition`; tests reject `BrParticle` declarations in module sources. |
| Packet behavior changes accidentally | Preserve `ParticleSpawnRequest(spawnId, particleId, position)` and keep Phase 13 packet/command rewire deferred. |
| Runtime-only visual behavior cannot be fully unit-tested | Compile and static/behavior tests cover deterministic seams; dev-client/hardware evidence remains Phase 14 per D-17. |

## Out of Scope for Phase 11

- Resource reload and registry publication rewiring (Phase 12).
- `/eyelib particle` command and spawn/remove packet contract rewiring (Phase 13).
- Final broad test relocation/adaptation and hardware checklist decisions (Phase 14).
