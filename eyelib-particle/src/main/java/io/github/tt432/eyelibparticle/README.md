# Eyelib Particle Module

## Scope
- Path: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`
- Owns the particle module boundary for particle-module APIs, `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` as the canonical module runtime definition owner, and explicit integration seams.

## Current Responsibilities
- Phase 8 owns build metadata, source/resource layout, and boundary documentation for `:eyelib-particle`.
- Phase 9 owns root-consumed API/store/publication contracts under `io.github.tt432.eyelibparticle.api`; retained root facades are transitional and must delegate here.
- Phase 10 owns `ParticleDefinition` and `ParticleDefinitionAdapter`; `io.github.tt432.eyelibimporter.particle.BrParticle` remains the canonical raw Bedrock particle schema/codec owner.
- The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Phase 11 owns module-side executable component and lifecycle behavior under `runtime/bedrock/**`, including emitter components, particle appearance/lifetime/initial/motion semantics, and module-owned emitter/particle lifecycle classes.
- Phase 11 also owns the explicit client integration layer under `client/**` for render-manager collections, render-buffer/material adapters, and `Dist.CLIENT` Forge hook delegation; root particle render-manager paths are transitional until later compatibility rewires complete.

## Dependency Direction
- Root runtime may depend on :eyelib-particle, but :eyelib-particle must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
- Minecraft/Forge-facing integration must live in explicitly documented adapters before introduction.
- Existing particle loading, command, and network behavior must not be moved into pure runtime packages; render adapter behavior belongs only in the documented `client/**` integration layer.
- Phase 11 moves executable runtime core, Phase 12 rewires loading/publication, and Phase 13 rewires command/network integration.

## Current Consumers
- Root runtime `:` consumes the module through Gradle project dependency wiring.
- Transitional root facades such as `ParticleLookup`, `ParticleAssetRegistry`, and `ParticleSpawnService` delegate to this module's API contracts; their removal condition is direct migration of root callers to `io.github.tt432.eyelibparticle.api` adapters/services.
- Root `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` is a legacy/non-canonical runtime adapter target, not the canonical raw schema.

## Verification Rule
- Gradle verification for this repository must be executed through JetBrains MCP Gradle tools only, never through shell Gradle commands.
