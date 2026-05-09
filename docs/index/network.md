# Network And Sync Index

## Scope
- Root path: `src/main/java/io/github/tt432/eyelib/network/`
- MC transport and packet DTO/codecs: `src/main/java/io/github/tt432/eyelib/mc/impl/network/` and `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/`.
- Cross-cutting sync behavior also touches `src/main/java/io/github/tt432/eyelib/util/data_attach/` and `src/main/java/io/github/tt432/eyelib/capability/`.
- Phase 13 command/network integration keeps particle packet payloads string-keyed as `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)`; PFUT-02 packet-contract relocation remains deferred.

## Start Reading Here
1. `docs/architecture/02-side-boundaries.md`
2. `src/main/java/io/github/tt432/eyelib/network/README.md`
3. `src/main/java/io/github/tt432/eyelib/mc/impl/network/README.md`
4. `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md`
5. `src/main/java/io/github/tt432/eyelib/network/dataattach/README.md`

## Hotspots
- `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/DataAttachmentHelper.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/RemoveParticlePacket.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/DataAttachmentSyncRuntime.java`

## Phase 13 Particle Routing Notes
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` stays context-free and delegates particle spawn/remove handling to `ParticleSpawnService`.
- `ParticleSpawnService` converts string-keyed packet fields into module `ParticleSpawnRequest` and delegates runtime work into particle module APIs/client services.
- JetBrains MCP Gradle tasks are the only approved verification path for these command/network checks; do not run Gradle through shell.

## Read Only If Needed
- Do not enter client rendering/tooling packages when the task is only about packet routing or attachment sync.
