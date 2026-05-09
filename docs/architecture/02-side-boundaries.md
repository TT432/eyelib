# Eyelib Side Boundaries

## Why Side Rules Matter Here
Eyelib is a Forge codebase with client-only rendering/tooling paths and shared runtime-sync paths. Package boundaries alone are not enough; side boundaries must stay explicit.

## Side Matrix
| Zone | May depend on | Must not depend on |
|---|---|---|
| common/shared | codecs, math, immutable data, pure helpers | client GUI, rendering, client-only runtime classes |
| client-only | render/model/animation/particle/tooling and client handlers | server-only execution paths |
| sync | packet codecs, side gates, narrow apply services | direct GUI code, loader implementation details |
| data attachment | typed attachment helpers and mutation flow | rendering or screen logic |

## Current Anchors
- `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java` now delegates transport ownership to `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`.
- Forge channel registration, packet context handling, side gating, and send transport now live in `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`.
- `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/DataAttachmentHelper.java` now separates local mutation from tracked sync via `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/DataAttachmentSyncRuntime.java` and pure payload mapping in `src/main/java/io/github/tt432/eyelib/network/dataattach/DataAttachmentSyncPayloadOps.java`.
- `src/main/java/io/github/tt432/eyelib/mc/impl/capability/CapabilityComponentRuntimeHooks.java` now owns Forge client event-bus subscriptions that drive capability runtime-component invalidation (`ManagerEntryChangedEvent` and `TextureChangedEvent`).
- `src/main/java/io/github/tt432/eyelib/client/` contains client-only rendering and tooling concerns and must stay out of common/shared zones.
- `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest` is the platform-type-free spawn request seam (`String` ids + state), and Phase 13 keeps `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` plus `RemoveParticlePacket(String removeId)` string-keyed in `mc/impl/network/packet`; Minecraft identifier validation/adaptation stays in `mc/impl` command/network runtime wiring.
- `:eyelib-processor` is a processor zone for platform-free parsing/classification/batching seams (`io.github.tt432.eyelibprocessor.*`): it may depend on plain JVM/codec/importer modules and engine-side plain-JVM Molang analysis from `:eyelib-molang`, but it must not own runtime publication/upload/event/UI or other Minecraft/Forge lifecycle bindings.
- `:eyelib-importer` is an importer/schema zone: it may own codecs, parsed definitions, Molang-compatible value types, normalization logic, and importer-only image/data representations, but it must not own GUI/runtime execution, `NativeImage` upload/download boundaries, or Forge/Minecraft lifecycle wiring.
- `:eyelib-particle` is a particle module zone: it owns particle module contracts, `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` as the canonical module runtime definition owner, `ParticleDefinitionAdapter`, executable particle runtime, render-manager behavior, the documented `io.github.tt432.eyelibparticle.client` adapter layer, and Phase 12 loading/publication services (`ParticleDefinitionRegistry`, `ParticleResourcePublication`, active `ParticleStore<ParticleDefinition>`); it must not depend on root runtime packages or `mc/impl`, and Minecraft/Forge-facing render integration is allowed only through `Dist.CLIENT`-guarded client adapters. Phase 13 command/network integration remains outside pure module APIs: `ParticleCommandRuntime` is platform-free root command shaping, `mc/impl/common/command` owns Brigadier/ResourceLocation conversion, `mc/impl/network/packet` owns packet DTO/codecs, and `ParticleSpawnService` adapts packet fields into module `ParticleSpawnRequest`.
- ResourceLocation adaptation for particle loading remains at root Forge/resource integration boundaries: `BrParticleLoader` receives `ResourceLocation` source ids from reload scanning, converts them to diagnostic strings, and delegates module publication by `ParticleDefinition.identifier()` without allowing `ResourceLocation` into the active particle store or pure module loading/core packages.

## Rules For This Refactor
- Packet registration and routing belong in the sync layer, not in GUI/tooling or loader code.
- Client-only smoke/test flows may depend on `runClient`; common/shared code must not.
- Importer/schema code may depend on plain JVM data, codecs, and engine modules such as `:eyelib-molang`, but new Minecraft/Forge runtime dependencies require explicit boundary documentation before introduction.
- Processor code may depend on plain JVM data and, when needed, importer/schema modules plus `:eyelib-molang` analysis helpers, but it must remain one-way with no dependency from `:eyelib-processor` back to root runtime packages; the current stage keeps root runtime explicit about any direct importer dependencies it still owns.
- Particle module code may define particle API/core contracts, Phase 11 executable runtime behavior, and Phase 12 pure loading/publication services that accept platform-free source metadata, but new Minecraft/Forge lifecycle, command, network transport, or client-hook dependencies require explicit adapter documentation before they are introduced; Phase 11 client hook/render dependencies are confined to `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/` and must remain `Dist.CLIENT` side-gated.
- Pure particle core in :eyelib-particle must remain root/MC/Forge-clean; platform-specific concerns require documented adapters before any Minecraft/Forge-facing dependency is introduced.
- Phase 10 documents the allowed particle -> importer dependency for ParticleDefinitionAdapter because `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner; root `client/particle/bedrock/BrParticle` remains a legacy/non-canonical runtime adapter target, Phase 11 owns executable runtime/client integration through `:eyelib-particle`, Phase 12 owns loading/publication via `ParticleResourcePublication` and `ParticleDefinitionRegistry`, Phase 13 rewires command/network integration, and Phase 14 owns broad/client verification evidence.
- Particle runtime documentation and tests must keep mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Data-attachment mutation rules must be written down before packet/data-attachment restructuring starts.
- New cross-zone dependencies need a written reason in the relevant architecture doc before they are introduced.
- Client packet handlers should prefer dedicated client runtime services such as `client/particle/ParticleSpawnService.java` over direct loader access.
- Phase 13 particle client packet handlers must remain context-free: `NetClientHandlers` delegates only to `ParticleSpawnService`, while `ParticleSpawnService` delegates executable runtime work into particle module APIs/client services.
- Client packet handlers should also route render-state application through dedicated apply services such as `client/render/sync/ClientRenderSyncService.java`.
- Client packet handlers should not call `BrParticleRenderManager` directly; emitter spawn/remove stays inside `ParticleSpawnService.java`.
- JetBrains MCP Gradle tasks are the only approved verification path for command/network checks; broad ClientSmoke/hardware visual evidence remains Phase 14 scope, and PFUT-02 packet-contract relocation remains deferred.

## Reading Guidance
- If a task involves packets or network handlers, read this file before editing `network/` or `util/data_attach/`.
- If a task involves rendering, GUI, models, or particles, stay inside client-local docs and do not assume server/shared applicability.
