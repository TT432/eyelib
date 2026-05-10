# Network And Sync Index

## Scope
- Root path: `src/main/java/io/github/tt432/eyelib/network/`
- MC transport and remaining root-coupled packet DTO/codecs: `src/main/java/io/github/tt432/eyelib/mc/impl/network/` and `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/`.
- Cross-cutting sync behavior also touches `src/main/java/io/github/tt432/eyelib/util/data_attach/` and `src/main/java/io/github/tt432/eyelib/capability/`.
- Particle packet payloads stay string-keyed as `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)`, with packet contracts owned by `:eyelib-particle` under `io.github.tt432.eyelibparticle.network`.
- Root-independent attachment packet contracts are owned by `:eyelib-attachment` under `io.github.tt432.eyelibattachment.network`; remaining root packet classes are blocker-bound to root render/model/capability/data owners.

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
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/SpawnParticlePacket.java`
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/RemoveParticlePacket.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/DataAttachmentSyncRuntime.java`

## Phase 13 Particle Routing Notes
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` stays context-free and delegates particle spawn/remove handling to `ParticleSpawnService`.
- `ParticleSpawnService` converts string-keyed packet fields into module `ParticleSpawnRequest` and delegates particle-only spawn/runtime work into `ParticleSpawnRuntimeAdapter`.
- JetBrains MCP Gradle tasks are the only approved verification path for these command/network checks; do not run Gradle through shell.

## FM-014 Network Ownership Notes
- Root `network/` owns shared channel entrypoints, transport delegation, and context-free dispatch only.
- `mc/impl/network/` owns Forge `SimpleChannel`, packet context, side gating, and player/entity send transport.
- Feature-specific protocol contracts live in feature modules where dependencies allow; root-coupled packet classes remain only when their payloads still decode through root data/capability/render owners.

## Read Only If Needed
- Do not enter client rendering/tooling packages when the task is only about packet routing or attachment sync.
