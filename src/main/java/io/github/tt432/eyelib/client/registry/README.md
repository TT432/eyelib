# Client Registry Index

## Scope

- Path: `src/main/java/io/github/tt432/eyelib/client/registry/`
- Runtime publication and lookup-facing boundary for parsed client assets.

## Current Role

- This package now contains domain-specific publication seams instead of one central static facade.
- `AnimationAssetRegistry.java`, `MaterialAssetRegistry.java`, `RenderControllerAssetRegistry.java`,
  `ClientEntityAssetRegistry.java`, `AttachableAssetRegistry.java`, and `ModelAssetRegistry.java` each own one
  write-side publication lane into manager-backed runtime storage.
- Loaders and tooling should call importer parsers and runtime adapters as needed, then hand publication off to the
  matching domain registry instead of pushing directly into managers or a shared god-facade.
- `ClientEntityAssetRegistry` publishes by `BrClientEntity.identifier()` and keeps its replacement seam free of
  `ResourceLocation`/Forge types.
- `ParticleAssetRegistry` has been deleted. Particle loading/publication callers should use
  `io.github.tt432.eyelibparticle.loading.ParticleResourcePublication` and `ParticleDefinitionRegistry.publisher()`
  directly.

## Boundary Direction

- Importer-only schema/codecs for client entities, animations, controllers, and model image data are moving toward
  `:eyelib-importer`.
- This package remains the write-side runtime publication boundary and must not absorb importer normalization logic.
- Particle publication API and active loading ownership now live in `io.github.tt432.eyelibparticle.api` plus
  `io.github.tt432.eyelibparticle.loading`; this package no longer keeps a particle compatibility adapter.
- Do not add canonical particle loading/publication logic here. `ParticleResourcePublication` parses importer-schema
  JSON and `ParticleDefinitionRegistry` owns the active `ParticleStore<ParticleDefinition>` keyed by
  `ParticleDefinition.identifier()`.

## Read Only If Needed

- Start here when a task is about manager publication, runtime lookup, or reducing direct manager writes from loaders.
