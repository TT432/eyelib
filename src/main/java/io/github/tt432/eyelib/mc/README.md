# MC Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/`
- Transitional home for root-level Minecraft/Forge wiring that has not yet been assigned to a functional module.
- This package is not a boundary rule. Feature-owned Minecraft/Forge code should live with its feature module.

## Layout
- `impl/`: remaining root mod startup, shared technical wiring, and compatibility adapters that still need functional ownership decisions.
- `api/`: legacy transitional namespace; do not add new contracts here.

## Current State
- This package is being drained as the refactor moves code to functional owners.
- Existing legacy packages remain transitional until their classes are assigned to particle, material, importer, attachment, render, manager, network, or another functional owner.
- Forge `@Mod` composition-root ownership now lives in `mc/impl/bootstrap/EyelibMod.java`.
- Current transitional areas include Molang Minecraft bindings, client loader lifecycle hooks, capability runtime-component hooks, sync transport/runtime ownership, data-attachment capability wiring, selected utility adapters, and mixin wiring.
- Manager event publication has moved to the `client/manager` functional owner.

## Ownership Rule
- Minecraft/Forge imports are allowed wherever the owning functional module needs them.
- Use this package only when code genuinely coordinates multiple functional modules or has not yet received a better owner.
