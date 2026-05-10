# Util Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/util/`
- Historical utility package drained by the `:eyelib-util` migration.

## Important Areas
- No Java source remains under this package after Phase 19.

## Current Named Destinations
- `../client/render/PoseCopies.java`: `PoseStack.Pose` copy helper now owned by the render domain
- `../mc/impl/util/model/InventoryModelResourceLocations.java`: inventory `ModelResourceLocation` helper now quarantined under `mc/impl`
- `../client/render/RenderTypeResolver.java`: render-type resolution now owned by the render domain
- `../client/render/texture/NativeImageIO.java`: native-image load/upload now owned by render texture support
- `../client/render/texture/TextureLayerMerger.java`: texture composition now owned by render texture support
- `../client/ClientTaskScheduler.java`: next-tick client scheduling now owned by the client runtime
- `../client/gui/preview/ModelPreviewAsset.java`: preview-only model/atlas pairing now owned by GUI preview flow

## v1.3 Utility Module Destinations
- `io.github.tt432.eyelibutil.color.ColorEncodings`: platform-free color channel transforms.
- `io.github.tt432.eyelibutil.collection.ListAccessors`: platform-free list edge accessors.
- `io.github.tt432.eyelibutil.codec.Eithers`: platform-free `Either` unwrap helper.
- `io.github.tt432.eyelibutil.time.FixedStepTimerState`: platform-free fixed-step timer state math.
- `io.github.tt432.eyelibutil.resource.ResourceLocations` and `io.github.tt432.eyelibutil.texture.TexturePaths`: resource and texture helpers moved during Phase 18.
- `io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap` and `io.github.tt432.eyelibutil.codec.*`: codec infrastructure moved during Phase 19.

## Historical Note
- The old `client/Textures.java`, `client/NativeImages.java`, `client/RenderTypeSerializations.java`, and `client/ClientScheduler.java` shims have been removed.
- The legacy `client/PoseHelper.java` and `client/ModelResourceLocationHelper.java` facade shims have been removed; callers should use owned destinations directly.
- Unused legacy runtime-heavy utility shims `client/BakedModels.java`, `client/BlitCall.java`, and `client/BufferBuilders.java` have been removed from `util/client`.
- The unused Minecraft-backed fixed timer adapter was removed after particle runtime moved to module-owned timing support.
- Forge `modbridge` model update event ownership moved to `../mc/impl/modbridge/ModBridgeModelUpdateEvent.java`.
- `ResourceLocations`, `client/texture/TexturePathHelper`, and `core/util/texture/TexturePaths` moved to `:eyelib-util` during Phase 18; callers should use `io.github.tt432.eyelibutil.resource.ResourceLocations` and `io.github.tt432.eyelibutil.texture.TexturePaths`.
- `ImmutableFloatTreeMap`, codec helpers, and `Eithers` moved to `:eyelib-util` during Phase 19; callers should use `io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap` and `io.github.tt432.eyelibutil.codec.*`.

## Boundary Reminder
- Do not add new Java source under this drained package.
- Use `:eyelib-util` for shared utility code, or move domain-specific helpers to the nearest functional owner.
