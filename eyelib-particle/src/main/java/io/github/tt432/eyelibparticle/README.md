# Eyelib Particle Module

## Scope
- Path: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`
- Owns the particle module boundary for particle-module APIs, `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` as the canonical module runtime definition owner, `ParticleDefinitionAdapter`, executable runtime, client integration, spawn/runtime adapters, render manager, and loading/publication through `ParticleDefinitionRegistry` plus `ParticleResourcePublication`.

## Current Responsibilities
- Phase 8 owns build metadata, source/resource layout, and boundary documentation for `:eyelib-particle`.
- Phase 9 owns root-consumed API/store/publication contracts under `io.github.tt432.eyelibparticle.api`; retained root facades are transitional and must delegate here.
- Phase 10 owns `ParticleDefinition` and `ParticleDefinitionAdapter`; `io.github.tt432.eyelibimporter.particle.BrParticle` remains the canonical raw Bedrock particle schema/codec owner.
- The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Phase 11 owns module-side executable component and lifecycle behavior under `runtime/bedrock/**`, including emitter components, particle appearance/lifetime/initial/motion semantics, and module-owned emitter/particle lifecycle classes.
- Phase 11 also owns the explicit client integration layer under `client/**` for render-manager collections, render-buffer/material adapters, and `Dist.CLIENT` Forge hook delegation; root particle render-manager paths are transitional adapters only until later compatibility rewires complete.
- Phase 12 owns active loading/publication: `ParticleDefinitionRegistry` is the module-owned active `ParticleStore<ParticleDefinition>` and `ParticleResourcePublication` parses canonical importer schema `io.github.tt432.eyelibimporter.particle.BrParticle`, converts through `ParticleDefinitionAdapter`, and publishes by `ParticleDefinition.identifier()`.
- Source ids from Forge reload scanning are diagnostics/report metadata only; they must not become active store keys or replace description-identifier publication semantics.
- Particle packet contracts live under `io.github.tt432.eyelibparticle.network`: `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)` own their codecs here, while `ParticleCommandRuntime` owns platform-free command shaping in root, `mc/impl/common/command` owns Brigadier and `ResourceLocation` conversion, root transport registers the packets, root `ParticleSpawnService` converts packets into module `ParticleSpawnRequest`, and `ParticleSpawnRuntimeAdapter` owns particle-only definition lookup, emitter creation, spawn, and remove delegation.

## Dependency Direction
- Root runtime may depend on :eyelib-particle, but :eyelib-particle must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
- Minecraft/Forge-facing integration must live in explicitly documented adapters before introduction; particle-owned packet codecs are allowed in `network/**` because they are particle protocol contracts.
- Existing particle loading, command, and network behavior must not be moved into pure runtime packages; spawn/runtime and render adapter behavior belongs only in the documented `client/**` integration layer.
- Phase 11 moved executable runtime core and client integration into this module; Phase 12 moved loading/publication ownership here through pure loading services, Phase 13 rewires command/network integration, and Phase 14 owns final broad/client verification evidence.
- Particle packet contracts have been relocated into this module; broad ClientSmoke/hardware evidence remains Phase 14 scope.
- Root `ResourceLocation` adaptation remains outside pure particle packages; module loading/publication accepts platform-free source metadata and publishes active entries by `ParticleDefinition.identifier()`.

## Current Consumers
- Root runtime `:` consumes the module through Gradle project dependency wiring.
- Root runtime still consumes this module through `BrParticleLoader`, `ParticleSpawnService`, and command/network transport adapters. Legacy root `ParticleLookup`, `ParticleAssetRegistry`, `ParticleManager`, `BrParticleRenderManager`, and `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/**` have been deleted after production callers moved to module definitions/publication/runtime services.
- `ParticleSpawnService` remains only to adapt root packet callers, module-definition runtime callers, and root Minecraft/capability context into `ParticleSpawnRuntimeAdapter`; delete it after callers bind directly to `io.github.tt432.eyelibparticle.api` and `io.github.tt432.eyelibparticle.client` adapters/services.

## Verification Rule
- Gradle verification for this repository must be executed through JetBrains MCP Gradle tools only, never through shell Gradle commands.
- Normal source tests must not read `.planning/` files. ClientSmoke and hardware/manual visual evidence are recorded separately from automated Gradle gates; PFUT-03 independent particle artifact publication, unrelated fixture cleanup, and manual visual proof are non-blocking scope boundaries for the v1.2 final gate.
