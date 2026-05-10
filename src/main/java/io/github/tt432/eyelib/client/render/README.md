# Client Render Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/render/`
- Owns render-time helpers, render visitors, renderer integration, texture upload/merge logic, and packet-apply helpers for client rendering.

## Boundary intent
- Client-render is the feature owner for render execution, renderer integration, render-type binding, texture upload, and render-only helper contracts even when those classes use Minecraft/Forge/Blaze3D runtime types.
- `mc/impl/mixin` may host technical Sponge Mixin wiring when the shared mixin configuration requires a single package root; that package location does not transfer feature ownership away from client-render.

## Current split
- Lower-risk seam already exists in `sync/RenderSyncApplyOps`.
- Model sync payload at this seam is now `sync/RenderModelSyncPayload` (`String`-keyed model/texture/renderType); MC `ResourceLocation` decoding is kept at runtime apply wiring.
- Heavy runtime owners such as `RenderParams`, `RenderTypeResolver`, visitors, renderers, and texture merge/upload remain client-render-owned implementation code.
- `SimpleRenderAction` uses `LivingEntityRendererAccessor` for render overlay calculation; the accessor is feature-owned by client-render but remains physically under `mc/impl/mixin` as technical mixin wiring registered through `src/main/resources/eyelib.mixins.json`.

## Editing rules
- Do not introduce new public contracts here that expose `RenderType`, `ResourceLocation`, `Minecraft`, or Blaze3D types unless the code is clearly implementation-only and client-render-owned.
- Prefer narrowing payload/state contracts first, then keep runtime-heavy render behavior with the client-render feature owner unless a class genuinely coordinates multiple feature modules.
