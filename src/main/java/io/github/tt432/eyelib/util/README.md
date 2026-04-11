# Util Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/util/`
- Shared helpers, data-attachment support, codec/math utilities, search/modbridge helpers, and a reduced set of deterministic client-side utility code.

## Important Areas
- `data_attach/`: typed data-attachment helpers and containers
- `codec/`: shared codec utilities
- `math/`: math helpers
- `client/`: reduced deterministic helper area, not a default destination for new work

## Current Named Destinations
- `client/texture/TexturePathHelper.java`: deterministic texture-path helpers
- `../client/render/PoseCopies.java`: `PoseStack.Pose` copy helper now owned by the render domain
- `../mc/impl/util/model/InventoryModelResourceLocations.java`: inventory `ModelResourceLocation` helper now quarantined under `mc/impl`
- `../client/render/RenderTypeResolver.java`: render-type resolution now owned by the render domain
- `../client/render/texture/NativeImageIO.java`: native-image load/upload now owned by render texture support
- `../client/render/texture/TextureLayerMerger.java`: texture composition now owned by render texture support
- `../client/ClientTaskScheduler.java`: next-tick client scheduling now owned by the client runtime
- `../client/gui/preview/ModelPreviewAsset.java`: preview-only model/atlas pairing now owned by GUI preview flow

## First-Wave Core Extractions
- `../core/util/texture/TexturePaths.java`: platform-free texture-path derivation
- `../core/util/color/ColorEncodings.java`: platform-free color channel transforms
- `../core/util/collection/ListAccessors.java`: platform-free list edge accessors
- `../core/util/codec/Eithers.java`: platform-free `Either` unwrap helper
- `../core/util/time/FixedStepTimerState.java`: platform-free fixed-step timer state math
- Existing `util/*` classes remain as compatibility adapters while callers migrate incrementally.

## Historical Note
- The old `client/Textures.java`, `client/NativeImages.java`, `client/RenderTypeSerializations.java`, and `client/ClientScheduler.java` shims have been removed.
- The legacy `client/PoseHelper.java` and `client/ModelResourceLocationHelper.java` facade shims have been removed; callers should use owned destinations directly.
- Unused legacy runtime-heavy utility shims `client/BakedModels.java`, `client/BlitCall.java`, and `client/BufferBuilders.java` have been removed from `util/client`.
- Minecraft-backed fixed timer runtime access moved to `../mc/impl/util/time/FixedTimer.java`.
- Forge `modbridge` model update event ownership moved to `../mc/impl/modbridge/ModBridgeModelUpdateEvent.java`.

## Boundary Reminder
- Keep only truly cross-cutting helpers in `util/`.
- Do not add unrelated new code to `util/client/` without a documented destination responsibility.
