# Common Package Guide

## Scope

- Path: `src/main/java/io/github/tt432/eyelib/common/`
- Contains shared behavior/runtime logic and deterministic runtime update helpers.

## Boundary intent

- Deterministic update logic and platform-free state helpers may remain outside `mc/impl`.
- Forge subscribers, command wiring, packet sends, mob-goal inspection, and Minecraft-backed entity/event logic live
  under explicit runtime owners such as `mc/impl/common/entity/`.

## Current split

- `common/runtime/` is the preferred home for extracted pure update logic.
- Legacy handlers in `common/` should not accumulate new Minecraft/Forge responsibilities; prefer a named runtime
  observer package for event/entity wiring.

## Editing rules

- Do not add new direct Minecraft/Forge dependencies to shared helpers that could live in `common/runtime/` or another
  platform-free seam.
- If a class mixes deterministic logic with event/entity/runtime access, split the deterministic part first and document
  the remaining MC-facing ownership.
