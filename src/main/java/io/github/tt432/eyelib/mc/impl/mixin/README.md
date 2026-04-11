# MC Impl Mixin Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/`
- Owns Sponge Mixin-based Minecraft client integration hooks.

## Boundary intent
- Mixins are direct Minecraft integration and belong under `mc/impl`.
- This package is implementation-only and must not be treated as a platform-free seam.

## Current ownership
- `HumanoidModelMixin`
- `LivingEntityRendererAccessor`
- `MultiPlayerGameModeMixin`

## Configuration linkage
- `src/main/resources/eyelib.mixins.json` now points to `"package": "io.github.tt432.eyelib.mc.impl.mixin"`.

## Editing rules
- If adding/removing/moving mixin classes, update `eyelib.mixins.json` in the same change.
- Do not add `core` abstractions here.
