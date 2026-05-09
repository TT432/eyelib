# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Phase 10 Ownership Status
- `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner.
- `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` is the canonical module runtime definition owner, produced through `ParticleDefinitionAdapter` using the allowed particle -> importer dependency for ParticleDefinitionAdapter.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` is a legacy/non-canonical runtime adapter target kept for compatibility until later extraction phases.
- The adapter-owned mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Phase 11 moved executable runtime core in stages: emitter and particle component execution, emitter/particle lifecycle, render-manager behavior, render adapter behavior, and `Dist.CLIENT` client hook ownership now live in `:eyelib-particle`.
- Phase 12 rewires loading/publication, Phase 13 rewires command/network integration, and Phase 14 owns final broad/client verification evidence.

## Current Runtime Boundaries
- `ParticleLookup.java`: transitional read-side root facade delegating to `io.github.tt432.eyelibparticle.api.ParticleLookupApi` through the root-backed `ParticleManager` store adapter; removal condition: delete after root callers migrate directly to particle API adapters/services.
- `ParticleSpawnService.java`: transitional root runtime adapter delegating packet spawn/remove entrypoints through `io.github.tt432.eyelibparticle.api.ParticleSpawnApi` and module-owned `io.github.tt432.eyelibparticle.client.ParticleRenderManager`; removal condition: delete after packet/runtime callers migrate directly to particle API/client runtime services.
- `bedrock/BrParticleRenderManager.java`: transitional root compatibility adapter over module-owned `ParticleRenderManager`; removal condition: delete after instrumentation and remaining root callers bind directly to particle-module client services.
- Spawn request state is owned by `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`; do not add a duplicate root request type.

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.
- Spawn/remove packet application must preserve string-keyed request intent and route executable runtime/render work through the particle module runtime/client services without changing packet payloads.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
- Retained root facades are transitional compatibility adapters only; new particle API/store consumers should prefer `io.github.tt432.eyelibparticle.api` contracts where practical.
