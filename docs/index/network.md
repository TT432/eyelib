# Network Index

## Scope
- Network transport, packet contracts, and channel registration guide.

## Key Responsibilities
- FM-014 designates shared channel entrypoints and context-free handler dispatch as root network ownership.
- Feature-specific protocol contracts live in `io.github.tt432.eyelibparticle.network` and `io.github.tt432.eyelibattachment.network`.
- Phase 13 command/network integration routes through `mc/impl/common/command` and root adapters.
- `ParticleCommandRuntime` owns deterministic platform-free suggestion/request/message shaping.
- Particle packet contracts: `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)`.
- `ParticleSpawnService` is the transitional root facade for packet-to-module delegation.
- JetBrains MCP provides the automated verification path.

## Dependency Direction
- Root packet transports register feature-owned packet contracts from subproject modules.
- Subprojects must not depend on root network registration or `mc/impl` transport wiring.
