# Client Registry Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/registry/`
- Runtime publication and lookup-facing boundary for parsed client assets.

## Current Role
- This package now contains domain-specific publication seams instead of one central static facade.
- `AnimationAssetRegistry.java`, `MaterialAssetRegistry.java`, `ParticleAssetRegistry.java`, `RenderControllerAssetRegistry.java`, `ClientEntityAssetRegistry.java`, and `ModelAssetRegistry.java` each own one write-side publication lane into manager-backed runtime storage.
- Loaders and tooling should call importer parsers and runtime adapters as needed, then hand publication off to the matching domain registry instead of pushing directly into managers or a shared god-facade.
- `ClientEntityAssetRegistry` publishes by `BrClientEntity.identifier()` and keeps its replacement seam free of `ResourceLocation`/Forge types.

## Boundary Direction
- Importer-only schema/codecs for client entities, animations, controllers, and model image data are moving toward `:eyelib-importer`.
- This package remains the write-side runtime publication boundary and must not absorb importer normalization logic.

## Read Only If Needed
- Start here when a task is about manager publication, runtime lookup, or reducing direct manager writes from loaders.
