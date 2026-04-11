# Client Loader Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/loader/`
- Resource reload listeners and parse/reload entrypoints for animations, materials, particles, render controllers, entities, and related assets.

## Pattern To Preserve
- Loader classes in this package follow the existing resource loader pattern rooted in `BrResourcesLoader.java`.
- Future refactor work should keep reload orchestration and root-side runtime adaptation here while moving importer-only parsing/normalization toward `:eyelib-importer` and pushing runtime publication responsibility toward narrow domain registry owners.
- Current publication seam: parsed asset maps are now handed to domain-specific classes under `../registry/` instead of each loader writing directly into managers inline.
- Client reload-listener registration lifecycle is now owned by `mc/impl/client/loader/ClientLoaderLifecycleHooks.java`.

## Key Files
- `BrResourcesLoader.java`
- `BrAnimationLoader.java`
- `BrParticleLoader.java`
- `BrMaterialLoader.java`
- `BrRenderControllerLoader.java`
- `BrModelLoader.java`

## Current Boundary Concern
- Some loader paths still blend importer parsing work with runtime publication or root-owned adaptation concerns.

## Current Boundary Improvement
- Manager publication for animations, materials, particles, render controllers, client entities, and models is now split across domain-specific classes in `client/registry/`.
- `LoaderParsingOps` parsing/translation seams no longer require `ResourceLocation` in method signatures.
- Concrete Forge reload listener registration (`RegisterClientReloadListenersEvent`) has moved out of concrete `Br*Loader` classes and into `mc/impl/client/loader/ClientLoaderLifecycleHooks.java`.
- Client-entity and attachable codecs now come from `:eyelib-importer`, and animation-controller reload paths now parse importer-owned controller schema before adapting into root runtime controllers.
- Target end state: loaders invoke importer-owned parsers/codecs, adapt results into runtime objects where necessary, then publish through `client/registry/`.

## Read Only If Needed
- If the task is only about runtime asset lookup or events, do not stay in this package longer than necessary.
