# MC Impl Mixin Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/`
- Owns Sponge Mixin-based Minecraft client integration hooks.

## Boundary intent
- This package owns technical Sponge Mixin wiring and config alignment for mixin classes registered from `src/main/resources/eyelib.mixins.json`.
- A class being physically hosted here does not make this package the feature owner; feature ownership follows the caller/behavior that needs the hook.
- This package is implementation-only and must not be treated as a platform-free seam.

## Current ownership
- `HumanoidModelMixin`: technical mixin wiring for model integration.
- `LivingEntityRendererAccessor`: technical mixin wiring for a client-render-owned overlay accessor consumed by `src/main/java/io/github/tt432/eyelib/client/render/SimpleRenderAction.java`.
- `MultiPlayerGameModeMixin`: technical mixin wiring for block-break/update-destroy integration.

## Configuration linkage
- `src/main/resources/eyelib.mixins.json` now points to `"package": "io.github.tt432.eyelib.mc.impl.mixin"`.
- FM-015 keeps `LivingEntityRendererAccessor` in this package because the current mixin config uses one shared package root; moving only this accessor would require broader mixin config/loading changes with no behavior benefit.

## Editing rules
- If adding/removing/moving mixin classes, update `eyelib.mixins.json` in the same change.
- Do not add `core` abstractions here.
- Document the feature owner for any mixin or accessor whose behavior is owned outside this technical wiring package.
