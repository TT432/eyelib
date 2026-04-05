# Client Registry Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/registry/`
- Runtime publication and lookup-facing boundary for parsed client assets.

## Current Role
- This package now contains domain-specific publication seams instead of one central static facade.
- `AnimationAssetRegistry.java`, `MaterialAssetRegistry.java`, `ParticleAssetRegistry.java`, `RenderControllerAssetRegistry.java`, `ClientEntityAssetRegistry.java`, and `ModelAssetRegistry.java` each own one write-side publication lane into manager-backed runtime storage.
- Loaders and tooling should parse and validate data, then hand publication off to the matching domain registry instead of pushing directly into managers or a shared god-facade.

## Read Only If Needed
- Start here when a task is about manager publication, runtime lookup, or reducing direct manager writes from loaders.
