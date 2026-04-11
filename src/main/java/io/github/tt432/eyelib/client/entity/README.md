# Client Entity Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/entity/`
- Contains runtime-facing client-entity lookup/helpers, RenderData integration, and transitional schema-adjacent types used by render/model systems.

## Boundary intent
- Parsed client-entity and attachable schema are moving toward `:eyelib-importer` as importer-owned definitions/codecs.
- Root `client/entity` should keep runtime-facing lookup/helpers and RenderData integration outside `mc/impl` only as long as they stay platform-type-free.

## Current split
- Canonical `BrClientEntity` / `BrClientEntityScripts` schema and codecs now live in `:eyelib-importer`.
- `ClientEntityRuntimeData` remains root-owned runtime state.
- `ClientEntityLookup` is the narrow runtime read seam and should stay separate from importer parsing.

## Editing rules
- Treat `client/entity` as the lowest-risk early sub-slice within the broader `client-model-animation-entity` module.
- Prefer moving schema/codecs out first, then keep runtime lookup/contracts here before expanding into heavier model/render runtime owners.
