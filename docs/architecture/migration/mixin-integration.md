# mixin-integration

## Scope
- Direct Minecraft integration hooks via Mixin.
- Main paths: `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/`
- Config: `src/main/resources/eyelib.mixins.json`

## Why it is MC-facing
- All mixins target Minecraft classes directly with `@Mixin`, `@Inject`, or `@Invoker`.
- All mixin classes are registered in the `client` section of `eyelib.mixins.json`.
- Mixin dependencies are Minecraft/Forge-facing runtime types and must stay in `mc/impl`.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: hard-import relocation slice advanced.
- Expected final state for this module: mixin classes/config stay under `mc/impl` ownership and do not introduce new non-`mc/impl` MC runtime leakage.

## Inventory

### HumanoidModelMixin
- **File**: `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/HumanoidModelMixin.java`
- **Target**: `net.minecraft.client.model.HumanoidModel`
- **What it does**: Injects into constructor flow and captures root `ModelPart` via `RootModelPartModel.getRootPart()`.
- **Coupling note**: still implements `io.github.tt432.eyelib.client.model.RootModelPartModel`.

### LivingEntityRendererAccessor
- **File**: `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/LivingEntityRendererAccessor.java`
- **Target**: `net.minecraft.client.renderer.entity.LivingEntityRenderer`
- **What it does**: Exposes `callGetWhiteOverlayProgress` via `@Invoker` for render overlay calculations.
- **Direct consumer**: `src/main/java/io/github/tt432/eyelib/client/render/SimpleRenderAction.java`.

### MultiPlayerGameModeMixin
- **File**: `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/MultiPlayerGameModeMixin.java`
- **Target**: `net.minecraft.client.multiplayer.MultiPlayerGameMode`
- **What it does**: Injects block-break lifecycle points to send `UpdateDestroyInfoPacket`.
- **Dependencies**: `EyelibNetworkManager`, `UpdateDestroyInfoPacket`, `ClientTaskScheduler`.

## Concrete re-check evidence from this stage

### `RootModelPartModel` coupling
- Search confirms `RootModelPartModel` is referenced by the relocated mixin and its own declaration under `client/model`.
- The interface remains outside `mc/impl`, with Minecraft `ModelPart` in its signature.
- This is not a blocker for moving mixin ownership itself, but remains a concrete downstream item for `client-model-animation-entity` final isolation.

### Network-hook coupling
- `MultiPlayerGameModeMixin` still sends `UpdateDestroyInfoPacket` through `EyelibNetworkManager.sendToServer(...)`.
- Transport/context handling for that packet is already owned by:
  - `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`
  - `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/DataAttachmentSyncRuntime.java`
- Therefore legacy-network ownership is no longer a blocker for package relocation.

## Landed relocation step
- Moved all mixin classes from `io.github.tt432.eyelib.mixin` to `io.github.tt432.eyelib.mc.impl.mixin`.
- Updated `eyelib.mixins.json` package field to `io.github.tt432.eyelib.mc.impl.mixin`.
- Updated direct import consumer `SimpleRenderAction` to the relocated accessor package.

## Status
- **Module status**: completed (hard-import slice advanced)
- **Code changes**: relocation + config alignment landed in this stage.
- **Docs/tracker updates**: updated in same change.
- **Remaining blocker**: cross-module ownership of `client/model/RootModelPartModel` (outside mixin package relocation scope).

## Final isolation checklist
- [x] Relocate mixin classes to allowed `mc/impl` package layout.
- [x] Update mixin config package ownership in `eyelib.mixins.json`.
- [x] Re-check `RootModelPartModel` and network-hook couplings with concrete file evidence.
- [ ] Finish broader client-model final isolation decisions for `RootModelPartModel` ownership.
- [ ] Pass full module/repo final isolation audit.
