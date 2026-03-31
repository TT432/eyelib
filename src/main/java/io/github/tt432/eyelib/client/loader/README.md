# Client Loader Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/loader/`
- Resource reload listeners and parse/reload entrypoints for animations, materials, particles, render controllers, entities, and related assets.

## Pattern To Preserve
- Loader classes in this package follow the existing resource loader pattern rooted in `BrResourcesLoader.java`.
- Future refactor work should keep parsing/reload flow here while pushing runtime publication responsibility toward a narrower registry/manager boundary.
- Current publication seam: parsed asset maps are now handed to `../registry/ClientAssetRegistry.java` instead of each loader writing directly into managers inline.

## Key Files
- `BrResourcesLoader.java`
- `BrAnimationLoader.java`
- `BrParticleLoader.java`
- `BrMaterialLoader.java`
- `BrRenderControllerLoader.java`
- `BrModelLoader.java`

## Current Boundary Concern
- Some loader paths currently blend parsing/reload work with publication side effects into runtime managers.

## Current Boundary Improvement
- Manager publication for animations, materials, particles, render controllers, and client entities is now centralized behind `client/registry/ClientAssetRegistry.java`.

## Read Only If Needed
- If the task is only about runtime asset lookup or events, do not stay in this package longer than necessary.
