---
phase: 12-loading-publication-rewire
verified: 2026-05-09T13:12:51Z
status: passed
score: 13/13 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: passed
  previous_score: 13/13
  gaps_closed: []
  gaps_remaining: []
  regressions: []
---

# Phase 12: Loading & Publication Rewire Verification Report

**Phase Goal:** Particle resource reload and publication semantics move behind the module boundary without changing observable registry behavior.  
**Verified:** 2026-05-09T13:12:51Z  
**Status:** passed  
**Re-verification:** Yes — after code review fixes (`447d7a5`, `d1fb60f`, `ac8b1e3`, `6a9828e`)

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Resource reload still parses `particles/*.json` and replaces the active particle registry with the same observable reload behavior. | ✓ VERIFIED | `BrParticleLoader` still calls `super("particles", "json")` and receives `Map<ResourceLocation, JsonElement>` in `apply` (`BrParticleLoader.java:24-32`). It converts source ids to strings and delegates replacement to `ParticleResourcePublication.replaceFromJsonResources`. `ParticleResourcePublicationTest.fullReplacementRemovesStaleEntries` proves full replacement removes stale entries and preserves active keys. |
| 2 | Particle publication continues to key entries by `particle_effect.description.identifier`, not by JSON resource path or other incidental source keys. | ✓ VERIFIED | `ParticleDefinitionRegistry` constructs `ParticlePublisher<>(STORE, ParticleDefinition::identifier)` (`ParticleDefinitionRegistry.java:16-18`). `ParticleResourcePublication` parses importer `BrParticle` and stores definitions by `definition.identifier()` (`ParticleResourcePublication.java:37-48`). Tests assert source keys are absent and description identifiers are present. |
| 3 | Loader, registry, and manager responsibilities are owned by the particle module or explicit root adapters without reintroducing root-owned particle internals. | ✓ VERIFIED | Module owns `ParticleDefinitionRegistry`, `ParticleResourcePublication`, and `ParticleLoadReport`. Root `ParticleAssetRegistry`, `ParticleManager`, `ParticleLookup`, and `ParticleSpawnService` are documented transitional adapters and delegate active publication/lookup to module APIs. |
| 4 | Maintainer can trace the reload path from root/Forge integration into particle-module registry publication without hidden ownership duplication. | ✓ VERIFIED | `ClientLoaderLifecycleHooks` registers `BrParticleLoader.INSTANCE` (`ClientLoaderLifecycleHooks.java:21-25`); `BrParticleLoader.apply` converts `ResourceLocation` to string and calls `ParticleResourcePublication`; `ParticleResourcePublication` publishes through `ParticleDefinitionRegistry.publisher()`. Docs in `MODULES.md`, repo map, architecture docs, side-boundary docs, and package READMEs describe the same path. |
| 5 | Module publication accepts string source metadata, uses `ParticlePublisher`, and active keys are `ParticleDefinition.identifier()` only. | ✓ VERIFIED | `ParticleResourcePublication.replaceFromJsonResources(Map<String, JsonElement>, Logger)` accepts string source ids; `ParticleDefinitionRegistry.publisher()` returns `ParticlePublisher<ParticleDefinition>` keyed by `ParticleDefinition::identifier`. |
| 6 | Raw particle JSON is parsed through importer `BrParticle.CODEC` and converted through `ParticleDefinitionAdapter` with failures logged/skipped. | ✓ VERIFIED | `ParticleResourcePublication.java:37-45` uses `io.github.tt432.eyelibimporter.particle.BrParticle.CODEC.parse(...).flatMap(ParticleDefinitionAdapter::fromSchema)`. `recordFailure` logs source id and message and records a report failure (`ParticleResourcePublication.java:79-85`). |
| 7 | Full replacement removes stale entries and preserves valid replacement iteration order. | ✓ VERIFIED | `ParticleResourcePublication` accumulates valid definitions in `LinkedHashMap` and calls `replaceParticles(definitions.values())`; `ParticlePublisher.replaceParticles` builds a `LinkedHashMap` and calls `store.replaceAll`. `ParticleResourcePublicationTest` covers stale removal and deterministic order. |
| 8 | Root loader/registry/lookup/spawn classes are named compatibility adapters and root `ParticleAssetRegistry` delegates to particle-module publisher/store API. | ✓ VERIFIED | `ParticleAssetRegistry.publisher()` returns `ParticleDefinitionRegistry.publisher()`; `ParticleLookup.names()` reads `ParticleDefinitionRegistry.store().names()`; `ParticleSpawnService.RootParticleSpawnApi.spawn` reads `ParticleDefinitionRegistry.store().get(request.particleId())`. Javadocs/READMEs mark these as transitional. |
| 9 | Forge reload registration and `particles/*.json` scanning stay behavior-compatible and side-safe. | ✓ VERIFIED | Forge registration remains in `mc/impl/client/loader/ClientLoaderLifecycleHooks` with `@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = MOD)`. `BrParticleLoader` retains `super("particles", "json")`. |
| 10 | Current animation particle effects and packet-driven spawn paths remain behavior-compatible through explicit adapters. | ✓ VERIFIED | Review-fix source now resolves animation/controller particle effects through module definitions: `BrAnimationEntryDefinition.java:98-103` uses `ParticleLookup.definition(s)` and `ParticleSpawnService.spawnEmitter(uuid, definition, ...)`; `BrControllerExecutor.java:79-89` does the same for controller particle effects. Packet path uses `ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position())` and `ParticleDefinitionRegistry.store().get(request.particleId())` without `BrParticle.CODEC.encodeStart` in `ParticleSpawnService`. Legacy `spawnEmitter(String, BrParticle, ...)` remains only as an explicit compatibility path. |
| 11 | Verification uses JetBrains MCP Gradle tasks only and preserves existing particle assertions. | ✓ VERIFIED | Re-ran JetBrains MCP tasks during re-verification: external task id 17 `:eyelib-particle:test :eyelib-particle:compileJava :compileJava` exited 0; external task id 18 targeted `:eyelib-particle:test :test` with Phase 12/review-fix tests exited 0. No shell Gradle was used. Existing and review-fix tests for publication, add-on loading, runtime delegation, asset registry, manager adapter, loader, boundary, adapter, and documentation passed. |
| 12 | Maintainer can trace ownership from Forge reload adapter into module loading/publication without hidden root business ownership. | ✓ VERIFIED | Source and documentation align: root owns `ResourceLocation` adaptation and Forge listener registration; module owns parse/convert/publish and active store; root registry/manager are compatibility only. |
| 13 | Module, architecture, side-boundary, loader, registry, and particle READMEs state the new loading/publication responsibilities. | ✓ VERIFIED | Verified docs contain `ParticleDefinitionRegistry`, `ParticleResourcePublication`, `ParticleDefinition.identifier()`, root `ResourceLocation` adaptation, and compatibility-adapter language. `ParticleApiDelegationBoundaryTest` and `ParticleDefinitionDocumentationTest` assert these anchors. |

**Score:** 13/13 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleDefinitionRegistry.java` | Module-owned active `ParticleStore<ParticleDefinition>` and publisher access | ✓ VERIFIED | Exists, substantive, exposes `store()` and `publisher()`, uses `ParticlePublisher<ParticleDefinition>` and `ParticleDefinition::identifier`. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java` | Importer-schema JSON parse/convert/publish service | ✓ VERIFIED | Exists, substantive, uses importer `BrParticle.CODEC`, `ParticleDefinitionAdapter::fromSchema`, failure reporting, duplicate reporting, and `ParticleDefinitionRegistry.publisher()`. `gsd-sdk verify.artifacts` missed the method-reference form, but source evidence verifies the link. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleLoadReport.java` | Processed source, published id, failure, duplicate report | ✓ VERIFIED | Exists and exposes immutable lists plus `failedSourceIds()`. |
| `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` | Forge reload adapter delegating JSON resources to module publication | ✓ VERIFIED | Retains `super("particles", "json")`, converts `ResourceLocation` keys to strings, delegates to `ParticleResourcePublication.replaceFromJsonResources`. |
| `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` | Transitional root publication facade over module publisher | ✓ VERIFIED | `publisher()` returns `ParticleDefinitionRegistry.publisher()`; `replaceParticles` converts legacy root particles to module definitions and publishes by identifier. |
| `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` | Legacy compatibility map only | ✓ VERIFIED | Javadoc explicitly names it as compatibility and active registry as `ParticleDefinitionRegistry`; store behavior preserved by tests. |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` | Transitional lookup facade | ✓ VERIFIED | Active `names()` comes from `ParticleDefinitionRegistry.store().names()`; legacy object reads remain compatibility-only. |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | Root packet/runtime compatibility over module `ParticleDefinition` lookup | ✓ VERIFIED | Packet path looks up `ParticleDefinition` directly and constructs `BedrockParticleRuntime`; no packet-path root `BrParticle` codec round-trip. Compatibility `BrParticle` spawn path publishes legacy particles into module definitions first, then creates module runtime emitters. |
| `MODULES.md` and architecture/package docs | Phase 12 ownership documentation | ✓ VERIFIED | Docs state module-owned active loading/publication and root compatibility adapter boundaries. `gsd-sdk verify.artifacts` missed an exact string in `MODULES.md`, but equivalent specific ownership text is present. |
| Targeted tests | Regression/boundary coverage | ✓ VERIFIED | Loader, publication, registry, manager, runtime delegation, boundary, adapter, and documentation tests exist and pass. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ClientLoaderLifecycleHooks` | `BrParticleLoader.INSTANCE` | Forge client reload listener registration | ✓ WIRED | Side-safe `Dist.CLIENT` registration remains in root `mc/impl`; `event.registerReloadListener(BrParticleLoader.INSTANCE)` is present. |
| `BrParticleLoader.apply` | `ParticleResourcePublication.replaceFromJsonResources` | `ResourceLocation` source ids converted to strings | ✓ WIRED | `entry.getKey().toString()` feeds ordered `Map<String, JsonElement>` into module publication. |
| `ParticleResourcePublication` | importer schema + `ParticleDefinitionAdapter` | DataResult parse/convert chain | ✓ WIRED | `BrParticle.CODEC.parse(JsonOps.INSTANCE, json).flatMap(ParticleDefinitionAdapter::fromSchema)`. |
| `ParticleResourcePublication` | `ParticleDefinitionRegistry.publisher()` | Replace valid definitions by identifier | ✓ WIRED | Calls `ParticleDefinitionRegistry.publisher().replaceParticles(definitions.values())`; publisher keys by `ParticleDefinition::identifier`. |
| `ParticleAssetRegistry` | `ParticleDefinitionRegistry` | Transitional root facade | ✓ WIRED | Publisher accessor and replacement/publish methods delegate to module registry while maintaining legacy map compatibility. |
| `ParticleLookup` | `ParticleDefinitionRegistry.store()` | String-keyed module lookup | ✓ WIRED | Active names come from module store. |
| `ParticleSpawnService` | `BedrockParticleRuntime` | Module `ParticleDefinition` without root legacy parse in packet path | ✓ WIRED | Packet path uses module definition lookup then `new BedrockParticleRuntime(definition, ...)`. |
| `ParticleLoadingBoundaryTest` | module loading package | Forbidden import scan | ✓ WIRED | Scans `eyelibparticle/loading/**` imports for root/Minecraft/Forge contamination. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `BrParticleLoader` | `resources: Map<String, JsonElement>` | Forge reload-prepared `Map<ResourceLocation, JsonElement>` from `SimpleJsonWithSuffixResourceReloadListener` | Yes | ✓ FLOWING |
| `ParticleResourcePublication` | `definitions: LinkedHashMap<String, ParticleDefinition>` | Importer `BrParticle.CODEC` parse + `ParticleDefinitionAdapter::fromSchema` | Yes | ✓ FLOWING |
| `ParticleDefinitionRegistry` | active store `particles` | `ParticlePublisher.replaceParticles` / `publishParticle` | Yes | ✓ FLOWING |
| `ParticleLookup.names()` | collection of active identifiers | `ParticleDefinitionRegistry.store().names()` | Yes | ✓ FLOWING |
| `ParticleSpawnService.RootParticleSpawnApi.spawn` | `ParticleDefinition definition` | `ParticleDefinitionRegistry.store().get(request.particleId())` | Yes | ✓ FLOWING |
| `ManagerResourceImportPlanner` | particle JSON/schema resources | add-on `BedrockAddon.aggregate().resourcePack().particleFiles()` via `ParticleResourcePublication.replaceFromSchemas`, legacy folder `particles/*.json` via `replaceFromJsonResources`, single-file import via `publishFromJsonResource` | Yes | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Targeted Phase 12 + review-fix unit/static tests pass | JetBrains MCP `:eyelib-particle:test :test` with filtered publication/loading/boundary/runtime/add-on/adapter/documentation tests | External task id 18, exitCode 0, BUILD SUCCESSFUL | ✓ PASS |
| Compile gates and particle module tests pass | JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :compileJava` | External task id 17, exitCode 0, BUILD SUCCESSFUL | ✓ PASS |
| Loader keeps scan contract and delegates publication | Source inspection of `BrParticleLoader.java` and `BrParticleLoaderPublicationTest` | `super("particles", "json")`, string source conversion, module publication call present; root legacy codec absent | ✓ PASS |
| Module loading package remains root/MC/Forge-clean | Source inspection plus `ParticleLoadingBoundaryTest` | No forbidden imports in `loading/**`; test passed | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| PLOAD-01 | 12-01, 12-02, 12-03 | Resource reload still parses `particles/*.json` and replaces active particle registry without changing observable reload behavior. | ✓ SATISFIED | `BrParticleLoader` scan contract preserved; module `replaceFromJsonResources` performs replacement; stale-removal tests pass. |
| PLOAD-02 | 12-01, 12-02, 12-03 | Particle publication continues to key by `particle_effect.description.identifier`, not source path/key. | ✓ SATISFIED | `ParticleDefinition::identifier` publisher; tests assert source keys absent and description identifiers present. |
| PLOAD-03 | 12-02, 12-03 | Loader, registry, and manager responsibilities are module-owned or explicit root adapters without root-owned internals. | ✓ SATISFIED | Module owns active registry/publication; root classes are named transitional adapters; docs and boundary tests enforce ownership. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| Relevant Phase 12 production files | - | TODO/FIXME/placeholder/stub scan | ℹ️ None | No blocker or warning anti-patterns found in `eyelibparticle/loading/**`, `ManagerResourceImportPlanner`, or touched root adapter/runtime files. `ParticleSpawnService.java:127` returns `null` only for failed legacy compatibility conversion and does not affect active module publication; unrelated pre-existing TODO/null patterns exist outside Phase 12 scope. |

### Human Verification Required

None. Phase 12 loading/publication semantics are covered by automated unit/static tests and compile checks. Visual/client smoke and broad verification are explicitly Phase 14 scope, not required to decide this phase goal.

### Gaps Summary

No blocking gaps found. The phase goal remains achieved after review fixes: reload scanning remains root/Forge-side and behavior-compatible, add-on and legacy folder particle publication now both flow into module-owned loading/publication, animation/controller particle effects resolve active module definitions, active registry keys are description identifiers, root classes are explicit adapters, and documentation/tests lock the ownership boundary.

---

_Verified: 2026-05-09T13:12:51Z_  
_Verifier: the agent (gsd-verifier)_
