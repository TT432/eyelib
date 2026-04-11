# Client GUI Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/gui/`
- Contains GUI screens, preview flows, manager tooling UI, hotkey entrypoints, and import/reload tooling used by client-facing editor features.

## Boundary intent
- GUI screens, previews, hotkeys, dialogs, and runtime rendering/tooling should ultimately live in `mc/impl` because they directly depend on Minecraft client UI and render runtime.
- Only narrow planning/classification helpers that are fully platform-type-free should remain outside `mc/impl`.

## Current split
- `manager/reload/ManagerResourceReloadPlan` is the clearest existing platform-free seam (single-file route classification + texture key shaping).
- `ModelPreviewScreen`, hotkey wiring, and resource import tooling remain transitional MC-facing runtime owners.

## Editing rules
- Do not add new platform-facing UI/runtime code here unless it is clearly transitional and intended for later `mc/impl` relocation.
- Prefer extracting platform-free planning/state helpers first, then move the remaining GUI/runtime owners into `mc/impl`.
- Keep this top-level guide aligned with the more specific `client/gui/manager/README.md` package notes.
