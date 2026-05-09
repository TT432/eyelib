# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Phase 10 Ownership Status
- `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner.
- `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` is the canonical module runtime definition owner, produced through `ParticleDefinitionAdapter` using the allowed particle -> importer dependency for ParticleDefinitionAdapter.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` is a legacy/non-canonical runtime adapter target kept for compatibility until later extraction phases.
- The adapter-owned mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Phase 11 is moving executable runtime core in stages: emitter and particle component execution plus emitter/particle lifecycle now lives in `:eyelib-particle`, and render-manager/client integration now has a module-owned `io.github.tt432.eyelibparticle.client` layer while root render-manager paths remain transitional until later Phase 11 compatibility work completes.
- Phase 11 moves executable runtime core, Phase 12 rewires loading/publication, and Phase 13 rewires command/network integration.

## Current Runtime Boundaries
- `ParticleLookup.java`: transitional read-side root facade delegating to `io.github.tt432.eyelibparticle.api.ParticleLookupApi` through the root-backed `ParticleManager` store adapter; removal condition: delete after root callers migrate directly to particle API adapters/services.
- `ParticleSpawnService.java`: transitional root runtime adapter delegating packet spawn/remove entrypoints to `io.github.tt432.eyelibparticle.api.ParticleSpawnApi` while keeping Minecraft/capability/render-manager internals in root; removal condition: delete after packet/runtime callers migrate directly to particle API adapters/services.
- Spawn request state is owned by `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`; do not add a duplicate root request type.

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.
- Spawn/remove packet application must keep root runtime/render work out of `:eyelib-particle`; the module API carries request intent only.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
- Retained root facades are transitional compatibility adapters only; new particle API/store consumers should prefer `io.github.tt432.eyelibparticle.api` contracts where practical.
