# Phase 12: Loading & Publication Rewire - Research

**Researched:** 2026-05-09  
**Status:** Ready for planning  
**Phase goal:** Particle resource reload and publication semantics move behind the module boundary without changing observable registry behavior.

## Executive Summary

Phase 12 should move the canonical particle reload/publication path into `:eyelib-particle` while keeping root as a Forge/resource integration adapter. The safest route is to introduce a module-owned loading/publication service that accepts source metadata as strings, parses raw JSON through `io.github.tt432.eyelibimporter.particle.BrParticle.CODEC`, converts through `ParticleDefinitionAdapter`, and publishes `ParticleDefinition` values by `ParticleDefinition.identifier()` through the existing `ParticlePublisher` semantics.

Root code should remain only as explicit adapters:

- `BrParticleLoader` keeps the existing Forge reload listener shape and `particles/*.json` resource scanning, but delegates parsing/publication into `:eyelib-particle`.
- `ParticleAssetRegistry`, `ParticleManager`, `ParticleLookup`, and `ParticleSpawnService` bridge current root callers while delegating active lookup/publication to module-owned store APIs.
- `ManagerResourceImportPlanner` keeps tooling-side file IO/upload ownership in root, but particle JSON publication should use the same module-owned conversion/publication seam as reload.

## Current Code Findings

| Area | Current state | Planning consequence |
|------|---------------|----------------------|
| Reload discovery | `SimpleJsonWithSuffixResourceReloadListener.scanDirectory(...)` uses `FileToIdConverter("particles", ".json")`; `BrParticleLoader` calls `super("particles", "json")`. | Preserve the exact resource path semantics; only change what happens after prepared JSON is available. |
| Particle loader | `BrParticleLoader` parses root legacy `client.particle.bedrock.BrParticle.CODEC`, stores a source-keyed `Map<ResourceLocation, BrParticle>`, then calls `ParticleAssetRegistry.replaceParticles(...)`. | Replace canonical parsing with importer schema + `ParticleDefinitionAdapter`; source `ResourceLocation` remains metadata/logging only. |
| Publication seam | `ParticlePublisher<T>` already replaces via `LinkedHashMap` and keys by extracted identifier; root `ParticleAssetRegistry` extracts `particle.particleEffect().description().identifier()`. | Reuse `ParticlePublisher<ParticleDefinition>` with `ParticleDefinition::identifier`; keep root facade as delegating compatibility only. |
| Store seam | `ParticleStore<T>` and `ParticleLookupApi<T>` are generic and string-keyed. Root `ParticleManager` currently implements `ParticleStore<BrParticle>`. | Add a module-owned active `ParticleStore<ParticleDefinition>`; root compatibility can bridge legacy `BrParticle` callers without owning active publication. |
| Spawn runtime | `ParticleSpawnService` currently converts legacy root `BrParticle` to importer `BrParticle` and then to `ParticleDefinition` at spawn time. | After publication stores `ParticleDefinition`, spawn can consume `ParticleDefinition` directly and remove late conversion from the hot path while preserving packet shape. |
| Boundary tests | Existing tests cover publisher identifier keys, stale removal, replacement order, root facade delegation, and module import cleanliness. | Extend rather than weaken these assertions; add loader/publication tests before or with implementation. |

## Recommended Architecture

### Module-owned active registry

Create a module-owned registry/service under `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/` or `.../api/`:

- `ParticleDefinitionRegistry` or equivalent active store owner backed by `LinkedHashMap<String, ParticleDefinition>`.
- `ParticleResourcePublication` or equivalent service that:
  - accepts source-keyed JSON as `Map<String, JsonElement>` or `Iterable<ParticleJsonResource>`;
  - parses with importer `BrParticle.CODEC.parse(JsonOps.INSTANCE, json)`;
  - converts with `ParticleDefinitionAdapter.fromSchema(schema)`;
  - logs/skips invalid resources while preserving reload-listener resilience;
  - publishes valid definitions through `ParticlePublisher<ParticleDefinition>` keyed by `ParticleDefinition.identifier()`.

### Root adapter responsibilities

Root should own only these pieces:

- Convert `ResourceLocation` source ids from Forge reload scanning into string source metadata.
- Register the reload listener through `mc/impl/client/loader/ClientLoaderLifecycleHooks` or keep registering a root adapter listener there.
- Maintain named compatibility adapters for current root callers that still request legacy `BrParticle`, with documented removal conditions.
- Keep command/network user behavior outside Phase 12; only preserve existing lookup names and spawn compatibility used by current paths.

### Error behavior

The conversion seam already exposes `DataResult<ParticleDefinition>` errors for missing particle effect, missing description, blank identifier, or missing render parameters. Reload behavior should:

1. fail loudly at the conversion seam via `DataResult.error`;
2. log source id + error message;
3. skip the invalid resource;
4. still replace the active registry with the valid definitions from the reload set.

This matches existing loader resilience, which logs parse failures instead of aborting all resource reload work.

## Validation Architecture

Automated validation must use JetBrains MCP Gradle tasks only. Shell Gradle is prohibited.

Required test evidence:

1. Module publication tests prove source keys are not active keys, stale entries are removed, valid entries publish in deterministic iteration order, and duplicate identifiers have explicit last-write-wins replacement behavior if touched.
2. Module loading tests prove importer schema parsing + `ParticleDefinitionAdapter` conversion is the canonical path and invalid resources are logged/skipped without silently dropping parity-critical fields.
3. Root adapter tests prove `BrParticleLoader`, `ParticleAssetRegistry`, `ParticleManager`, `ParticleLookup`, and `ParticleSpawnService` delegate into module-owned registry/store APIs and do not parse root `BrParticle` as the canonical loader schema.
4. Boundary scans prove pure particle module `api/**`, `loading/**`, and `runtime/**` remain free of root, Minecraft, and Forge imports; Minecraft/Forge reload registration stays in root `mc/impl` or documented client integration.

Recommended JetBrains MCP verification gates:

- `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.* --tests io.github.tt432.eyelibparticle.api.* --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest"`
- `jetbrain_run_gradle_tasks taskNames=[":test"] scriptParameters="--tests io.github.tt432.eyelib.client.loader.*Particle* --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistry* --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.Particle*BoundaryTest"`
- `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:compileJava", ":compileJava"]`

## Risks And Mitigations

| Risk | Mitigation |
|------|------------|
| Active registry accidentally keyed by source `ResourceLocation` | Tests must assert `source_id` entries are absent and description identifiers are present after reload/publication. |
| Root legacy `BrParticle` becomes canonical again | Loader tests must scan `BrParticleLoader` for importer-codec delegation and reject `client.particle.bedrock.BrParticle.CODEC` as the reload parse owner. |
| Module core imports Minecraft/Forge while accepting reload data | Use `String` source ids and `JsonElement` input in module loading service; keep `ResourceLocation` only in root adapter. |
| Spawn path loses compatibility for current packet/animation callers | Bridge `ParticleLookup`/`ParticleSpawnService` through module definitions and retain current public method signatures where callers still use them. |
| Invalid resource handling silently hides data loss | Conversion failures must expose `DataResult` errors and loader tests must verify invalid resources do not prevent valid replacement entries. |

## Out Of Scope For Phase 12

- `/eyelib particle` command syntax, suggestions, validation, spawn position, success message, and packet integration rewires are Phase 13.
- Broad root test-suite cleanup, visual/client smoke proof, hardware rendering evidence, and final documentation gate are Phase 14.
- Publishing `:eyelib-particle` as an independent external artifact is outside the current milestone.
