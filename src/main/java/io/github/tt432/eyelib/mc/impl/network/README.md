# MC Impl Network Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/network/`
- Owns Forge/Minecraft packet channel transport, side-gating, packet-context handling, and registration of feature-owned packet contracts.

## Key Files
- `EyelibNetworkTransport.java`: `SimpleChannel` registration, packet dispatch, and send-to-player/tracked transport wiring.
- `io.github.tt432.eyelibparticle.network.SpawnParticlePacket`: string-keyed `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` contract and `FriendlyByteBuf` codec.
- `io.github.tt432.eyelibparticle.network.RemoveParticlePacket`: string-keyed `RemoveParticlePacket(String removeId)` contract and `FriendlyByteBuf` codec.
- `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/network/`: attachment-owned root-independent packet contracts registered by this transport.
- `packet/`: remaining root-coupled packet contracts that still decode through root render/model/capability/data owners.
- `dataattach/DataAttachmentSyncRuntime.java`: Minecraft runtime apply/sync behavior for attachment packets.

## Boundary Reminder
- Keep direct packet transport/context/runtime imports in this package.
- Keep shared channel/context transport here, while feature-owned packet DTO/codecs stay under their functional modules and root `network/` stays focused on entrypoint and context-free handler delegation.
- Do not add new feature protocols to `packet/` when their dependencies already fit an owning feature module.
- Particle packet handlers route through `NetClientHandlers` into `ParticleSpawnService`, which converts packets into module `ParticleSpawnRequest` and delegates particle-only spawn/runtime work into `ParticleSpawnRuntimeAdapter`.
- Broad ClientSmoke/hardware visual evidence remains Phase 14 scope, and verification must use JetBrains MCP Gradle tasks only.
