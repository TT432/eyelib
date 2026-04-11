# Utility Index

## Scope
- Root path: `src/main/java/io/github/tt432/eyelib/util/`
- Includes shared helpers, data-attachment helpers, codec/math utilities, and mixed client-side helpers.

## Start Reading Here
1. `src/main/java/io/github/tt432/eyelib/util/README.md`
2. `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md` for attachment flow
3. Relevant architecture doc before touching `util/client/`

## Hotspots
- `src/main/java/io/github/tt432/eyelib/util/client/`
- `src/main/java/io/github/tt432/eyelib/util/data_attach/`
- `src/main/java/io/github/tt432/eyelib/util/SharedLibraryLoader.java`

## Current Split Targets
- `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java`
- `src/main/java/io/github/tt432/eyelib/client/render/PoseCopies.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/util/model/InventoryModelResourceLocations.java`
- `src/main/java/io/github/tt432/eyelib/core/util/`
- `src/main/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerState.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/util/time/FixedTimer.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeModelUpdateEvent.java`

## Boundary Reminder
- `util/client/` is not a default destination for new code during the refactor.
- Only truly cross-cutting helpers should remain in `util/` long term.
