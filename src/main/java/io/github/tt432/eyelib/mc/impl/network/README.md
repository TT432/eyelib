# MC Impl Network Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/network/`
- Owns Forge/Minecraft packet channel transport, side-gating, packet-context handling, and Phase 13 particle packet DTO/codec ownership.

## Key Files
- `EyelibNetworkTransport.java`: `SimpleChannel` registration, packet dispatch, and send-to-player/tracked transport wiring.
- `packet/SpawnParticlePacket.java`: string-keyed `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` contract and `FriendlyByteBuf` codec.
- `packet/RemoveParticlePacket.java`: string-keyed `RemoveParticlePacket(String removeId)` contract and `FriendlyByteBuf` codec.
- `dataattach/DataAttachmentSyncRuntime.java`: Minecraft runtime apply/sync behavior for attachment packets.

## Boundary Reminder
- Keep direct packet transport/context/runtime imports in this package.
- Keep packet DTO/codecs under `mc/impl/network/packet` for Phase 13, while root `network/` stays focused on entrypoint and context-free handler delegation.
- Particle packet handlers route through `NetClientHandlers` into `ParticleSpawnService`, which converts packets into module `ParticleSpawnRequest` and delegates runtime work into particle module APIs/client services.
- PFUT-02 packet-contract relocation remains deferred, broad ClientSmoke/hardware visual evidence remains Phase 14 scope, and verification must use JetBrains MCP Gradle tasks only.
