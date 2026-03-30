# Util Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/util/`
- Shared helpers, data-attachment support, codec/math utilities, search/modbridge helpers, and mixed client-side utility code.

## Important Areas
- `data_attach/`: typed data-attachment helpers and containers
- `codec/`: shared codec utilities
- `math/`: math helpers
- `client/`: mixed client helper area; current refactor hotspot, not a default destination for new work

## Current Named Destinations
- `client/texture/TexturePathHelper.java`: deterministic texture-path helpers
- `client/render/PoseCopies.java`: `PoseStack.Pose` copy helper
- `client/model/InventoryModelResourceLocations.java`: inventory `ModelResourceLocation` helper

## Compatibility Facades
- `client/Textures.java`, `client/PoseHelper.java`, and `client/ModelResourceLocationHelper.java` remain as transitional facades while callers migrate.

## Boundary Reminder
- Keep only truly cross-cutting helpers in `util/`.
- Do not add unrelated new code to `util/client/` without a documented destination responsibility.
