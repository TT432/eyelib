# Core Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/core/`
- Platform-free helpers and logic intended to remain independent from Minecraft/Forge runtime classes.

## Boundary Rules
- `core` must not depend on `net.minecraft.*` or `net.minecraftforge.*`.
- Minecraft-facing adapters remain in existing `util/*` or domain-specific MC packages.
- During migration waves, prefer additive extraction into `core` plus compatibility adapters over broad callsite churn.

## First-Wave Utility Seams
- `core/util/collection/ListAccessors.java`
- `core/util/texture/TexturePaths.java`
- `core/util/color/ColorEncodings.java`
- `core/util/codec/Eithers.java`
- `core/util/time/FixedStepTimerState.java`
