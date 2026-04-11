# MC Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/`
- Owns Minecraft/Forge-facing integration that must stay out of `core` and out of platform-type-free contracts.

## Layout
- `api/`: future home for platform-type-free contracts between `core` and Minecraft runtime.
- `impl/`: concrete Minecraft/Forge implementations, lifecycle hooks, bootstrap wiring, rendering, packets, GUI, and other runtime integration.

## Current State
- This package is being introduced incrementally during the refactor.
- Existing legacy packages remain transitional until their direct Minecraft/Forge imports are moved behind `mc/api` ports or into `mc/impl`.
- Forge `@Mod` composition-root ownership now lives in `mc/impl/bootstrap/EyelibMod.java`.
- Data-attachment capability/provider/event/NBT wiring now lives in `mc/impl/data_attach/` as part of hard-import quarantine work.
- Current seams under this root include Molang runtime/query bindings, manager entry-change event publication bindings, client loader reload-listener lifecycle hooks, capability runtime-component event hooks under `mc/impl/capability/`, and sync transport/runtime ownership under `mc/impl/network/`.
- Utility bridge quarantine now includes `mc/impl/molang/*`, `mc/impl/util/time/FixedTimer.java`, `mc/impl/modbridge/ModBridgeModelUpdateEvent.java`, and `mc/impl/util/model/InventoryModelResourceLocations.java`.
- Mixin integration ownership now also lives under `mc/impl/mixin/`, with `eyelib.mixins.json` package routing aligned to that namespace.

## Boundary Rule
- Direct `net.minecraft.*`, `net.minecraftforge.*`, and `com.mojang.blaze3d.*` imports belong in `mc/impl` only.
