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
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java` is now a platform-type-free client request seam (`String` ids + state), and `network/SpawnParticlePacket` now also uses a string-keyed particle id contract; Minecraft identifier validation/adaptation stays in `mc/impl` command/network runtime wiring.
- `:eyelib-importer` is an importer/schema zone: it may own codecs, parsed definitions, Molang-compatible value types, normalization logic, and importer-only image/data representations, but it must not own GUI/runtime execution, `NativeImage` upload/download boundaries, or Forge/Minecraft lifecycle wiring.

## Rules For This Refactor
- Packet registration and routing belong in the sync layer, not in GUI/tooling or loader code.
- Client-only smoke/test flows may depend on `runClient`; common/shared code must not.
- Importer/schema code may depend on plain JVM data, codecs, and engine modules such as `:eyelib-molang`, but new Minecraft/Forge runtime dependencies require explicit boundary documentation before introduction.
- Data-attachment mutation rules must be written down before packet/data-attachment restructuring starts.
- New cross-zone dependencies need a written reason in the relevant architecture doc before they are introduced.
- Client packet handlers should prefer dedicated client runtime services such as `client/particle/ParticleSpawnService.java` over direct loader access.
- Client packet handlers should also route render-state application through dedicated apply services such as `client/render/sync/ClientRenderSyncService.java`.
- Client packet handlers should not call `BrParticleRenderManager` directly; emitter spawn/remove stays inside `ParticleSpawnService.java`.

## Reading Guidance
- If a task involves packets or network handlers, read this file before editing `network/` or `util/data_attach/`.
- If a task involves rendering, GUI, models, or particles, stay inside client-local docs and do not assume server/shared applicability.
