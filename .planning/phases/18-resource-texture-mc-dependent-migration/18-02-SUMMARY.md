---
phase: 18-resource-texture-mc-dependent-migration
plan: 02
status: complete
requirements-completed: [MIGR-03]
completed: 2026-05-10
---

# 18-02 Summary: TexturePaths Migration

## Status
- Complete.

## Changes
- Added `eyelib-util/src/main/java/io/github/tt432/eyelibutil/texture/TexturePaths.java`.
- Rewired `RenderParams`, `RenderControllerEntry`, and `CoreUtilitySeamTest` to the util module package.
- Deleted `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java`.
- Deleted `src/main/java/io/github/tt432/eyelib/core/util/texture/TexturePaths.java`.
- Deleted the drained `src/main/java/io/github/tt432/eyelib/util/client/package-info.java`.

## Evidence
- Java source search for `io.github.tt432.eyelib.util.client.texture`, `io.github.tt432.eyelib.core.util.texture`, and `TexturePathHelper` returned zero matches.
