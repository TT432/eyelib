# Network Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/network/`
- Minimal network entrypoint and context-free handler delegation for sync routing.
- Phase 13 command/network integration keeps particle packet DTO/codecs in `mc/impl/network/packet` while this package delegates handling only.

## Start Reading Here
1. `docs/index/network.md`
2. `docs/architecture/02-side-boundaries.md`
3. `EyelibNetworkManager.java`

## Key Files
- `EyelibNetworkManager.java`: minimal network entrypoint delegating transport to `mc/impl/network/`
- `NetClientHandlers.java`: context-free client apply delegation; particle spawn/remove calls only `ParticleSpawnService`
- Packet contract classes now live under `../mc/impl/network/packet/`, including `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)`

## Boundary Reminder
- Packet transport/context ownership lives under `../mc/impl/network/`; this package should stay transport-agnostic.
- Packet codec/DTO ownership now also lives under `../mc/impl/network/packet/`; do not grow new `FriendlyByteBuf` / NBT-backed packet contracts here.
- Phase 13 requires `NetClientHandlers` to remain context-free and route particle packets through `ParticleSpawnService`, which converts packet fields into module `ParticleSpawnRequest` and delegates runtime work into particle module APIs/client services.
- `:eyelib-particle` owns particle module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable runtime, client integration, render manager, and loading/publication through `ParticleDefinitionRegistry` plus `ParticleResourcePublication`; importer owns raw `io.github.tt432.eyelibimporter.particle.BrParticle`; this root network package owns only handler delegation and sync routing.
- PFUT-02 packet-contract relocation, PFUT-03 independent particle artifact publication, unrelated fixture cleanup, and broad ClientSmoke/hardware/manual visual proof remain deferred or non-blocking; verify this boundary with JetBrains MCP Gradle tasks only, and keep normal source tests independent from `.planning/` files.
- Data-attachment state ownership is shared with `../util/data_attach/` and should be read together when sync work is involved.
