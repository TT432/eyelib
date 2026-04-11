# Client Render Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/render/`
- Owns render-time helpers, render visitors, renderer integration, texture upload/merge logic, and packet-apply helpers for client rendering.

## Boundary intent
- Pure mutation/state helpers may remain outside `mc/impl` only if they do not expose Minecraft/Forge/Blaze3D runtime types.
- Runtime render execution, renderer integration, render-type binding, texture upload, and client singleton access should end up under `mc/impl` during final quarantine.

## Current split
- Lower-risk seam already exists in `sync/RenderSyncApplyOps`.
- Model sync payload at this seam is now `sync/RenderModelSyncPayload` (`String`-keyed model/texture/renderType); MC `ResourceLocation` decoding is kept at runtime apply wiring.
- Heavy runtime owners such as `RenderParams`, `RenderTypeResolver`, visitors, renderers, and texture merge/upload remain transitional and are expected to move toward `mc/impl`.

## Editing rules
- Do not introduce new public contracts here that expose `RenderType`, `ResourceLocation`, `Minecraft`, or Blaze3D types unless the code is clearly implementation-only and intended for later `mc/impl` ownership.
- Prefer narrowing payload/state contracts first, then relocating runtime-heavy classes.
