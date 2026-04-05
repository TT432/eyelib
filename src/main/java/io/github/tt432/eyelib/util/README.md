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
- `client/render/PoseCopies.java`: `PoseStack.Pose` copy helper
- `client/model/InventoryModelResourceLocations.java`: inventory `ModelResourceLocation` helper
- `../client/render/RenderTypeResolver.java`: render-type resolution now owned by the render domain
- `../client/render/texture/NativeImageIO.java`: native-image load/upload now owned by render texture support
- `../client/render/texture/TextureLayerMerger.java`: texture composition now owned by render texture support
- `../client/ClientTaskScheduler.java`: next-tick client scheduling now owned by the client runtime
- `../client/gui/preview/ModelPreviewAsset.java`: preview-only model/atlas pairing now owned by GUI preview flow

## Historical Note
- The old `client/Textures.java`, `client/NativeImages.java`, `client/RenderTypeSerializations.java`, and `client/ClientScheduler.java` shims have been removed.
- `client/PoseHelper.java` and `client/ModelResourceLocationHelper.java` still exist, but new work should prefer direct named owners instead of growing facade-style helpers.

## Boundary Reminder
- Keep only truly cross-cutting helpers in `util/`.
- Do not add unrelated new code to `util/client/` without a documented destination responsibility.
