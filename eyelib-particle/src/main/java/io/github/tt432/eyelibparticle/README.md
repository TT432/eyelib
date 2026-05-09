# Eyelib Particle Module

## Scope
- Path: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`
- Owns the particle module boundary for future particle-module APIs, core/runtime definitions, and explicit integration seams.

## Current Responsibilities
- Phase 8 owns build metadata, source/resource layout, and boundary documentation for `:eyelib-particle`.
- Phase 9 owns root-consumed API/store/publication contracts under `io.github.tt432.eyelibparticle.api`; retained root facades are transitional and must delegate here.
- Current executable particle runtime remains in `src/main/java/io/github/tt432/eyelib/client/particle/` until later phase plans move it through explicit seams.

## Dependency Direction
- Root runtime may depend on :eyelib-particle, but :eyelib-particle must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
- Minecraft/Forge-facing integration must live in explicitly documented adapters before introduction.
- Existing particle loading, command, network, and render behavior must not be moved into this module during the skeleton phase.

## Current Consumers
- Root runtime `:` consumes the module through Gradle project dependency wiring.
- Transitional root facades such as `ParticleLookup`, `ParticleAssetRegistry`, and `ParticleSpawnService` delegate to this module's API contracts; their removal condition is direct migration of root callers to `io.github.tt432.eyelibparticle.api` adapters/services.

## Verification Rule
- Gradle verification for this repository must be executed through JetBrains MCP Gradle tools only, never through shell Gradle commands.
