# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Current Runtime Boundaries
- `ParticleLookup.java`: read-side access to particle definitions through the runtime manager boundary
- `ParticleSpawnService.java`: packet-driven spawn/remove orchestration on the client side

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
