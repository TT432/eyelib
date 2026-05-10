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
- Root `network/` owns shared channel entrypoints, transport delegation, and context-free handler dispatch only; feature-specific protocol contracts live in feature modules where dependencies allow.
- Root-independent attachment packet contracts now live in `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/network/`; root transport only registers and dispatches them.
- `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/DataAttachmentHelper.java` now separates local mutation from tracked sync via `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/DataAttachmentSyncRuntime.java` and pure payload mapping in `src/main/java/io/github/tt432/eyelib/network/dataattach/DataAttachmentSyncPayloadOps.java`.
- `src/main/java/io/github/tt432/eyelib/mc/impl/capability/CapabilityComponentRuntimeHooks.java` now owns Forge client event-bus subscriptions that drive capability runtime-component invalidation (`ManagerEntryChangedEvent` and `TextureChangedEvent`).
- `src/main/java/io/github/tt432/eyelib/mc/impl/capability/ExtraEntityUpdateDataRuntimeHooks.java` owns Forge living event observation, attachment mutation, and root-coupled extra-entity update packet dispatch for the data-only `ExtraEntityUpdateData` attachment.
- `src/main/java/io/github/tt432/eyelib/mc/impl/common/entity/EntityExtraDataRuntimeHooks.java` owns Forge entity event subscriptions, mob-goal observation, local mutation, and attachment sync dispatch for `ExtraEntityData`; pure goal-flag update policy remains in `common/runtime/ExtraEntityDataUpdater.java`.
- `src/main/java/io/github/tt432/eyelib/client/` contains client-only rendering and tooling concerns and must stay out of common/shared zones.
- `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest` is the platform-type-free spawn request seam (`String` ids + state), and `io.github.tt432.eyelibparticle.network` owns string-keyed `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` plus `RemoveParticlePacket(String removeId)`; Minecraft identifier validation/adaptation stays in `mc/impl` command/network runtime wiring.
- `:eyelib-processor` is a processor zone for platform-free parsing/classification/batching seams (`io.github.tt432.eyelibprocessor.*`): it may depend on plain JVM/codec/importer modules and engine-side plain-JVM Molang analysis from `:eyelib-molang`, but it must not own runtime publication/upload/event/UI or other Minecraft/Forge lifecycle bindings.
- `:eyelib-importer` is currently an importer/schema Forge functional module: it owns codecs, parsed definitions, Molang-compatible value types, normalization logic, importer-only image/data representations, and the `eyelibimporter` Forge bootstrap in `mc/impl/bootstrap`; GUI/runtime execution and `NativeImage` upload/download boundaries still belong to the consuming runtime feature. A plain importer library split is future debt, not the current module identity.
- `:eyelib-particle` is a particle module zone: it owns particle module contracts, `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` as the canonical module runtime definition owner, `ParticleDefinitionAdapter`, executable particle runtime, spawn/runtime adapter behavior, render-manager behavior, the documented `io.github.tt432.eyelibparticle.client` client adapters, packet contracts in `io.github.tt432.eyelibparticle.network`, and Phase 12 loading/publication services (`ParticleDefinitionRegistry`, `ParticleResourcePublication`, active `ParticleStore<ParticleDefinition>`). Particle-specific Minecraft/Forge APIs are allowed in the particle module when side-gated with `Dist.CLIENT` and functionally owned; root reverse dependencies remain debt.
- ResourceLocation adaptation for particle loading remains at root Forge/resource integration boundaries: `BrParticleLoader` receives `ResourceLocation` source ids from reload scanning, converts them to diagnostic strings, and delegates module publication by `ParticleDefinition.identifier()` without allowing `ResourceLocation` into the active particle store or pure module loading/core packages.
- `:eyelib-attachment` is an attachment Minecraft/Forge functional module: `FriendlyByteBuf` and NBT stream codecs are allowed in `io.github.tt432.eyelibattachment.codec.stream`, root-independent attachment packet contracts are allowed in `io.github.tt432.eyelibattachment.network`, and plain storage contracts under `io.github.tt432.eyelibattachment.dataattach` should avoid direct Minecraft/Forge imports and must not depend on root runtime packages.

## Rules For This Refactor
- Packet registration and routing belong in the sync layer, not in GUI/tooling or loader code.
- Client-only smoke/test flows may depend on `runClient`; common/shared code must not.
- Importer/schema code may depend on plain JVM data, codecs, and engine modules such as `:eyelib-molang`; Minecraft/Forge dependencies are allowed for importer-owned functional behavior because the current importer artifact is a Forge functional module, but non-bootstrap Forge expansion must be documented explicitly.
- Processor code may depend on plain JVM data and, when needed, importer/schema modules plus `:eyelib-molang` analysis helpers, but it must remain one-way with no dependency from `:eyelib-processor` back to root runtime packages; the current stage keeps root runtime explicit about any direct importer dependencies it still owns.
- Particle module code may define particle API/core contracts, executable runtime behavior, loading/publication services, and particle-owned Minecraft/Forge client integration when the code is particle-specific; client-only dependencies must remain side-gated.
- Particle code should avoid root reverse dependencies, but Minecraft/Forge APIs are not prohibited when they belong to particle functionality.
- Phase 10 documents the allowed particle -> importer dependency for ParticleDefinitionAdapter because `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner; root `client/particle/bedrock/BrParticle` has been deleted, Phase 11 owns executable runtime/client integration through `:eyelib-particle`, Phase 12 owns loading/publication via `ParticleResourcePublication` and `ParticleDefinitionRegistry`, Phase 13 rewires command/network integration, and Phase 14 owns broad/client verification evidence.
- Particle runtime documentation and tests must keep mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Data-attachment mutation rules must be written down before packet/data-attachment restructuring starts.
- Entity-derived attachment mutation keeps read/update/write/sync explicit: runtime observers read the attachment, apply deterministic policy, write with `DataAttachmentHelper.setLocal`, and dispatch sync through `DataAttachmentSyncRuntime` only when the value instance changes.
- Attachment packet contracts that still depend on root `EyelibAttachableData` or root capability payload types should remain root-coupled until those owners move; do not introduce a reverse dependency from `:eyelib-attachment` to root.
- Remaining root packet classes under `mc/impl/network/packet/` are blocker-bound to root render/model/capability/data owners and should not become the default home for new feature protocols.
- Do not split attachment stream codecs out only to isolate Minecraft buffer imports; split only if the product goal changes to a separate plain attachment contract artifact.
- New cross-feature dependencies need a written reason in the relevant architecture doc before they are introduced.
- Client packet handlers should prefer dedicated client runtime services such as `client/particle/ParticleSpawnService.java` over direct loader access.
- Phase 13 particle client packet handlers must remain context-free: `NetClientHandlers` delegates only to `ParticleSpawnService`, while `ParticleSpawnService` delegates particle-only spawn/runtime work into `ParticleSpawnRuntimeAdapter` and keeps only root Minecraft/capability context adaptation.
- Client packet handlers should also route render-state application through dedicated apply services such as `client/render/sync/ClientRenderSyncService.java`.
- Client packet handlers should not call render managers directly; emitter spawn/remove stays inside `ParticleSpawnService.java` until callers bind directly to module client services.
- JetBrains MCP Gradle tasks are the only approved verification path for command/network checks; broad ClientSmoke/hardware visual evidence remains Phase 14 scope.
- Phase 14 final gate evidence must keep automated JetBrains MCP Gradle checks separate from ClientSmoke/manual/hardware status. Windows hardware exit-code capture and purely manual visual proof are manual/deferred evidence, PFUT-03 independent particle artifact publication is future scope, and normal source tests must not read `.planning/` artifacts.

## Reading Guidance
- If a task involves packets or network handlers, read this file before editing `network/` or `util/data_attach/`.
- If a task involves rendering, GUI, models, or particles, stay inside client-local docs and do not assume server/shared applicability.
