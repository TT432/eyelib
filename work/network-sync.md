# network-sync

## Scope
- Packet registration, transport, and sync/apply services.
- Main paths: `network/`, `network/dataattach/`, `mc/impl/network/`

## Why it is MC-facing
- Uses Forge `SimpleChannel`, packet context, side gating, entity/player transport.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: in progress (hard-import slice advanced).
- Expected final state for this module: packet transport, side gating, and player/entity-backed dispatch remain in `mc/impl`; only transport-independent contracts, payload DTOs, and apply ports may survive outside `mc/impl`.

## Target seam
- Separate pure payload/state transitions from MC transport and handler context.
- Keep channel registration and packet dispatch in `mc/impl`.

## Deliverables
- Design ports around sync/apply boundaries.
- Add tests for extracted payload/state logic.
- Implement split and verify client/server compile paths.

## Dependencies
- After `capability-dataattach` and `client-render` seam decisions.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [x] Re-baseline remaining Minecraft/Forge references for this module.
- [x] Confine transport, packet context, and side-gated implementation code to allowed `mc/impl` packages.
- [ ] Keep payload/apply contracts free of Minecraft/Forge types.
- [x] Re-run JetBrains MCP verification for the final package layout.
- [x] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- Before this slice, `network/` still directly owned channel/context/side-gating and data-attach runtime apply behavior.
- Packet contract classes no longer remain under legacy `network/`; they now live under `mc/impl/network/packet/`. Final hard-import quarantine still has implementation-side codec work left because some relocated packet DTOs continue to use `FriendlyByteBuf` / `CompoundTag` inside `mc/impl`.

## Progress notes
- Moved channel registration, packet context handling, side gating, and player/entity dispatch from `network/EyelibNetworkManager` to `mc/impl/network/EyelibNetworkTransport`.
- Reduced `network/EyelibNetworkManager` to a minimal entrypoint (`register` + `sendToServer`) with no direct MC/Forge transport imports.
- Removed `NetworkEvent.Context` exposure from `network/NetClientHandlers`; handlers are now context-free delegates.
- Replaced legacy `network/dataattach/DataAttachmentSyncService` with `mc/impl/network/dataattach/DataAttachmentSyncRuntime` for runtime entity lookup/apply/sync behavior.
- `client/render/sync/RenderSyncApplyOps` now owns pure render payload/state helpers used by `ClientRenderSyncService` for both send and apply paths.
- `network/dataattach/DataAttachmentSyncPayloadOps` remains the pure attachment payload/state seam and is now consumed by `DataAttachmentSyncRuntime`.
- Narrowed packet/apply contracts for string-id attachment flow by removing `RegistryObject<DataAttachmentType<T>>` ownership from `UniDataUpdatePacket`.
- Narrowed particle packet contract ownership: `network/SpawnParticlePacket` now carries a platform-type-free `String particleId` instead of `ResourceLocation`, with conversion no longer needed at command send/runtime apply call sites.
- Relocated legacy packet DTO classes from `network/` to `mc/impl/network/packet/`, so sync transport/codec ownership no longer lives in a non-`mc/impl` package.
- Updated MC-facing call sites to use `mc/impl/network` transport/runtime owners directly (`ClientRenderSyncService`, `EntityExtraDataHandler`, `EntityStatisticsHandler`, `ExtraEntityUpdateData`, `EyelibParticleCommand`, `DataAttachmentEventHandlers`).
- Added targeted seam tests:
  - `src/test/java/io/github/tt432/eyelib/client/render/sync/RenderSyncApplyOpsTest.java`
  - `src/test/java/io/github/tt432/eyelib/network/dataattach/DataAttachmentSyncPayloadOpsTest.java`
  - `src/test/java/io/github/tt432/eyelib/network/SpawnParticlePacketTest.java`

## Verification
- JetBrains MCP file inspections (`jetbrain_get_file_problems`) on touched Java files: no errors (warnings only in pre-existing MC-facing runtime files).
- JetBrains MCP targeted Gradle tests:
  - `test --tests io.github.tt432.eyelib.client.render.sync.RenderSyncApplyOpsTest --tests io.github.tt432.eyelib.network.dataattach.DataAttachmentSyncPayloadOpsTest --tests io.github.tt432.eyelib.network.UniDataUpdatePacketTest` ✅
- JetBrains MCP compile/build checks:
  - `jetbrain_build_project` ✅
  - `build` ✅

## Rule-scan snapshot (post-slice)
- `network/dataattach/**`: no direct `net.minecraft.*` / `net.minecraftforge.*` imports.
- `network/**`: transport/context APIs (`SimpleChannel`, `NetworkEvent.Context`, `DistExecutor`, `PacketDistributor`, `NetworkRegistry`) no longer appear, and packet DTO classes no longer live in this package.
- Remaining sync blockers are now implementation-side packet payload concerns under `mc/impl/network/packet/**`:
  - `FriendlyByteBuf` in `AnimationComponentSyncPacket`, `DataAttachmentUpdatePacket`, `DataAttachmentSyncPacket`, `ExtraEntityDataPacket`, `ExtraEntityUpdateDataPacket`, `ModelComponentSyncPacket`, `RemoveParticlePacket`, `SpawnParticlePacket`, `UniDataUpdatePacket`, `UpdateDestroyInfoPacket`.
  - `CompoundTag` payload contract in `DataAttachmentSyncPacket` (full-container NBT sync path used by `mc/impl/network/dataattach/DataAttachmentSyncRuntime`).
