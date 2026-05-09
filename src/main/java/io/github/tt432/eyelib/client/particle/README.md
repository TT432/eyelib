# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Current Runtime Boundaries
- `ParticleLookup.java`: transitional read-side root facade delegating to `io.github.tt432.eyelibparticle.api.ParticleLookupApi` through the root-backed `ParticleManager` store adapter; removal condition: delete after root callers migrate directly to particle API adapters/services.
- `ParticleSpawnService.java`: transitional root runtime adapter delegating packet spawn/remove entrypoints to `io.github.tt432.eyelibparticle.api.ParticleSpawnApi` while keeping Minecraft/capability/render-manager internals in root; removal condition: delete after packet/runtime callers migrate directly to particle API adapters/services.
- `ParticleSpawnRequest.java`: platform-type-free spawn request state (`String` ids + position) used by runtime spawn orchestration

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.
- Spawn/remove packet application must keep root runtime/render work out of `:eyelib-particle`; the module API carries request intent only.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
- Retained root facades are transitional compatibility adapters only; new particle API/store consumers should prefer `io.github.tt432.eyelibparticle.api` contracts where practical.
