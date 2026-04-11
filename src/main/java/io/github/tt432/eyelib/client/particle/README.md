# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Current Runtime Boundaries
- `ParticleLookup.java`: read-side access to particle definitions through the runtime manager boundary
- `ParticleSpawnService.java`: packet-driven spawn/remove orchestration on the client side
- `ParticleSpawnRequest.java`: platform-type-free spawn request state (`String` ids + position) used by runtime spawn orchestration

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
