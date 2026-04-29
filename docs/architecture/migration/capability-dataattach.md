# capability-dataattach

## Scope
- Forge capability layer and typed data-attachment storage/mutation.
- Main paths: `capability/`, `util/data_attach/`

## Why it is MC-facing
- Uses Forge capabilities, `AttachCapabilitiesEvent`, entity attachment, NBT serialization.

## Final isolation status
- First-wave seam status: complete.
- Hard-import-quarantine slice status: second-wave attachment contract + wiring isolation complete.
- Final `mc/api + mc/impl` isolation status: pending remaining capability payload/package moves outside this slice.
- Expected final state for this module: storage and mutation contracts may live in `core` or `mc/api`, but all Forge capability registration, provider wiring, entity events, and NBT-bound implementation paths must be confined to allowed `mc/impl` packages.

## Target seam
- Keep storage semantics and mutation rules portable where possible.
- Move Forge registration/provider wiring to `mc/impl`.
- Define narrow ports for attachment access and sync triggering.

## Seam design (current slice)
- Pure storage/mutation port: `DataAttachmentStorage`.
- Map-backed storage semantics owner: `DataAttachmentMapStorage` now stores by attachment id string, not `ResourceLocation`.
- `DataAttachmentType` now exposes platform-free `String id` for storage/mutation contracts.
- `IDataAttachmentContainer` is now storage-only (no `INBTSerializable<CompoundTag>` inheritance).
- `DataAttachment` is now a platform-free typed value holder; NBT encode/decode moved to mc-facing implementation.
- Forge capability registration/provider/entity event integration moved to `mc/impl/data_attach`: `DataAttachmentContainerCapability`, `DataAttachmentContainerProvider`, `DataAttachmentEventHandlers`.
- NBT serialization implementation moved to `mc/impl/data_attach/McDataAttachmentContainer`.

## Deliverables
- Design seam interfaces and placement.
- Add seam-focused tests for storage/mutation logic.
- Implement split and migrate local callers.

## Dependencies
- After `client-managers-registry` and `utility-mc-bridges`.

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
- [x] Move or confine capability/provider/event/NBT implementation code to allowed `mc/impl` packages.
- [x] Keep storage/mutation contracts free of Minecraft/Forge types.
- [x] Re-run JetBrains MCP verification for the final package layout.
- [x] Pass rule-based boundary scan for this module.

## Verification log
- ✅ JetBrains MCP Gradle test: `test --tests io.github.tt432.eyelib.util.data_attach.DataAttachmentStorageTest`
- ✅ JetBrains MCP Gradle build: `build`
- ✅ JetBrains MCP Gradle test: `test --tests io.github.tt432.eyelib.util.data_attach.DataAttachmentStorageTest --tests io.github.tt432.eyelib.network.dataattach.DataAttachmentSyncPayloadOpsTest`
- ✅ JetBrains MCP file problems: zero errors across modified attachment + wiring files
- ✅ Rule scan: no `net.minecraft.*` / `net.minecraftforge.*` / `com.mojang.blaze3d.*` imports remain under `util/data_attach/**`; MC imports now appear in `mc/impl/data_attach/**`
- ℹ️ Historical note: an earlier session reported unrelated `FixedStepTimerStateTest` failures, but current JetBrains MCP `build` runs in this slice are passing.
- ✅ JetBrains MCP Gradle test (runtime follow-up slice): `test --tests io.github.tt432.eyelib.capability.component.AnimationComponentRuntimeInvalidationTest --tests io.github.tt432.eyelib.capability.component.RenderControllerComponentTextureStateTest`
- ✅ JetBrains MCP build (runtime follow-up slice): `build`
- ✅ JetBrains MCP file problems: no errors in modified runtime-slice Java files (`AnimationComponent`, `RenderControllerComponent`, `CapabilityComponentRuntimeHooks`, new tests)
- ⚠️ Java `lsp_diagnostics` (`jdtls`) unavailable in this session (`Command not found: jdtls`); compile/test/build verification was completed through JetBrains MCP instead.

## Re-baseline notes for final isolation
- The contract layer has already moved significantly toward platform-free ownership: `DataAttachmentType` is string-id based, `DataAttachment` no longer owns NBT behavior, and `IDataAttachmentContainer` no longer extends Minecraft/Forge serialization types.
- `mc/impl/data_attach/` is now the concrete quarantine zone for capability registration, provider bridging, entity event handling, entity-bound access helpers, and NBT serialization.
- Rule-scan snapshot: `util/data_attach/**` no longer carries direct `net.minecraft.*` / `net.minecraftforge.*` imports, while `mc/impl/data_attach/**` now correctly owns those imports.
- Remaining validation work is no longer about whether these responsibilities moved, but whether all legacy callers/tests/docs were updated consistently, whether `capability/**` residual MC-facing types (`RenderData`, `ModelComponent`, `RenderControllerComponent`, etc.) should remain in that package or move in later slices, and whether full JetBrains MCP verification stays green after the background task finishes.

## Runtime component follow-up slice (post attachment split)
- Scope of this slice: move Forge client event wiring for remaining `capability/**` runtime components without broad render/runtime rewrites.
- Landed move: `AnimationComponent` and `RenderControllerComponent` no longer subscribe to Forge event bus directly; component-local invalidation seams are now platform-free methods (`AnimationComponent.onManagerEntryChanged(...)`, `RenderControllerComponent.onTextureStateChanged()`).
- New `mc/impl` owner: `src/main/java/io/github/tt432/eyelib/mc/impl/capability/CapabilityComponentRuntimeHooks.java` now owns `ManagerEntryChangedEvent`/`TextureChangedEvent` subscriptions and forwards invalidation to capability runtime components.
- Plain-JVM seam coverage added: `AnimationComponentRuntimeInvalidationTest` and `RenderControllerComponentTextureStateTest`.

## Remaining blockers after this slice
- `capability/RenderData.java` still directly depends on MC runtime (`Entity`) and `mc/impl` helper access for attachment lookup/sync boundary wiring.
- `capability/component/ModelComponent.java` still exposes MC runtime transport/render types in its contract (`ResourceLocation`, `FriendlyByteBuf`, `RenderType`) and remains a primary next-cut hotspot.
- `capability/component/AnimationComponent.java` still carries MC transport type (`FriendlyByteBuf`) in stream codec wiring; this slice only moved Forge event ownership.
