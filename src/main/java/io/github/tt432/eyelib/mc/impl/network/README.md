# MC Network Implementation

## Scope
- Path: `mc/impl/network/`
- Forge `SimpleChannel` transport wiring, packet context handling, and side gating.

## Key Responsibilities
- Transport registers feature-owned packet contracts from `io.github.tt432.eyelibparticle.network`.
- Particle packet registration: `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)`.
- Delegates packet application to `NetClientHandlers` and `ParticleSpawnService`.

## Dependency Direction
- Transport owns channel/context/distributor wiring.
- Feature module packet contracts must not depend on `mc/impl/network/` transport details.
