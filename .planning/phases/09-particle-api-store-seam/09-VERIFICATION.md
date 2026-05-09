---
phase: 09-particle-api-store-seam
verified: 2026-05-09T05:20:46Z
status: passed
score: 8/8 must-haves verified
overrides_applied: 0
---

# Phase 9: Particle API & Store Seam Verification Report

**Phase Goal:** Root runtime can use particle capabilities through narrow module-owned APIs instead of owning particle internals directly.
**Verified:** 2026-05-09T05:20:46Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Root runtime can access particle lookup, spawn/remove, store/publication, and initialization behavior through particle-module API seams. | ✓ VERIFIED | `ParticleManager` implements `ParticleStore<BrParticle>` and exposes `store()` (`ParticleManager.java:18-23`); `ParticleLookup` returns `ParticleLookupApi<BrParticle>` via `ParticleManager.store()` (`ParticleLookup.java:21-35`); `ParticleAssetRegistry` delegates publish/replace through `ParticlePublisher` (`ParticleAssetRegistry.java:19-34`); `ParticleSpawnService` delegates packet spawn/remove through `ParticleSpawnApi` + module `ParticleSpawnRequest` (`ParticleSpawnService.java:24-40`). Lifecycle/reset is the `ParticleLifecycle.clear()` contract implemented via `ParticleStore`/`Manager.clear()` inheritance. |
| 2 | Any root compatibility facade delegates to particle-module APIs instead of containing particle business logic. | ✓ VERIFIED | Retained facades are narrow: `ParticleLookup` delegates all reads through `api().get`/`api().names`; `ParticleAssetRegistry` delegates to `publisher().replaceParticles`/`publishParticle`; `ParticleSpawnService.spawnFromPacket/removeEmitter` delegate to `api().spawn/remove`. Runtime emitter construction remains in the root adapter implementation, not in module API. |
| 3 | Maintainer can identify every temporary compatibility facade and read why it exists and when it can be removed. | ✓ VERIFIED | Javadocs on `ParticleManager`, `ParticleLookup`, `ParticleAssetRegistry`, and `ParticleSpawnService` state transitional/removal conditions. Local docs list `ParticleLookup`, `ParticleSpawnService`, and `ParticleAssetRegistry` as transitional and name `io.github.tt432.eyelibparticle.api` delegation/removal conditions (`client/particle/README.md:7-19`, `client/registry/README.md:12-17`, `eyelibparticle/README.md:21-23`). |
| 4 | New particle API contracts are string-keyed and root/MC/Forge-clean. | ✓ VERIFIED | API package uses `String` IDs in `ParticleLookupApi.get`, `ParticleStore.put/replaceAll`, `ParticleIdentifier.identify`, `ParticleSpawnRequest(String spawnId, String particleId, Vector3f)`, and `ParticleSpawnApi.spawn/remove`. Forbidden-import scan over `eyelib-particle/src/main/java` found no root runtime, network, capability, `mc.impl`, Minecraft, or Forge imports. |
| 5 | Particle publication still keys entries by `particle_effect.description.identifier`, not source/resource keys. | ✓ VERIFIED | `ParticleAssetRegistry` constructs `ParticlePublisher` with extractor `particle -> particle.particleEffect().description().identifier()` and passes `particles.values()` so source keys are ignored (`ParticleAssetRegistry.java:19-30`). `ParticleAssetRegistryTest` and `ParticleAssetRegistryPublisherAdapterTest` use mismatched source keys and assert only description identifiers are stored. |
| 6 | Spawn/remove request seam exists without moving runtime/render internals into `:eyelib-particle`. | ✓ VERIFIED | `ParticleSpawnApi` is a pure request port (`ParticleSpawnApi.java:6-20`). `ParticleSpawnRequest` carries string ids and defensive-copy `Vector3f` (`ParticleSpawnRequest.java:14-27`). `ParticleSpawnService` keeps `Minecraft`, `DataAttachmentHelper`, `BrParticleEmitter`, and `BrParticleRenderManager` imports in root (`ParticleSpawnService.java:3-14, 42-67`). |
| 7 | Automated tests prove publication identifiers, spawn request semantics, delegation docs, and root-clean API boundaries. | ✓ VERIFIED | Tests exist and are substantive: `ParticlePublisherTest`, `ParticleSpawnRequestTest`, `ParticleAssetRegistryTest`, `ParticleApiDelegationBoundaryTest`, plus adapter tests from earlier plans. Orchestrator evidence: `:eyelib-particle:test`, `:eyelib-particle:compileJava`, `:compileJava`, and targeted root tests exited 0. |
| 8 | No broad compatibility layer or runtime move occurred in Phase 9. | ✓ VERIFIED | Search found no new `ParticleCompatibility`/catch-all compatibility package. Runtime classes remain in root (`src/main/java/.../client/particle/`), while `:eyelib-particle` contains API contracts only. Later runtime/schema/loading/command moves remain explicitly deferred to Phases 10-13 in `ROADMAP.md`. |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java` | API package boundary docs and null-marking | ✓ VERIFIED | Documents root-consumed particle API contracts and one-way dependency rule; `@NullMarked` present. |
| `ParticleStore.java` | String-keyed mutable store port | ✓ VERIFIED | Extends `ParticleLookupApi<T>` and `ParticleLifecycle`; defines `put(String, T)` and `replaceAll(Map<String, ? extends T>)`. |
| `ParticleLookupApi.java` | String-keyed read API | ✓ VERIFIED | Defines nullable `get(String)`, `all()`, and default `names()` from map keys. |
| `ParticleLifecycle.java` | Narrow initialization/reset/lifecycle seam | ✓ VERIFIED | Defines `clear()` only; implemented by `ParticleManager` via `ParticleStore`. |
| `ParticleIdentifier.java` | Publication identifier extractor | ✓ VERIFIED | Functional interface returning string id. |
| `ParticlePublisher.java` | Identifier-flattening publication seam | ✓ VERIFIED | Uses `LinkedHashMap`, `Objects.requireNonNull`, `identifier.identify`, `store.put`, and `store.replaceAll`. |
| `ParticleSpawnRequest.java` | String-keyed spawn request | ✓ VERIFIED | Record carries `spawnId`, `particleId`, defensive-copy `Vector3f`; rejects null fields. |
| `ParticleSpawnApi.java` | Spawn/remove request port | ✓ VERIFIED | Defines `spawn(ParticleSpawnRequest)` and `remove(String)`. |
| `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` | Root backing adapter implementing particle-module store API | ✓ VERIFIED | `extends Manager<BrParticle> implements ParticleStore<BrParticle>`; exposes `store()` and transitional `readPort`/`writePort`. |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` | Transitional lookup facade delegating to API | ✓ VERIFIED | Delegates `get(ResourceLocation)`, `get(String)`, and `names()` through `ParticleLookupApi`. |
| `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` | Transitional publication facade delegating to publisher | ✓ VERIFIED | Uses module `ParticlePublisher` and description identifier extractor. |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | Transitional spawn/remove adapter through `ParticleSpawnApi` | ✓ VERIFIED | Public methods delegate to module API; root runtime implementation stays nested/root-local. |
| `src/main/java/io/github/tt432/eyelib/client/particle/README.md` | Transitional facade documentation | ✓ VERIFIED | Names `ParticleLookup` and `ParticleSpawnService`, their delegation targets, and removal conditions. |
| `src/main/java/io/github/tt432/eyelib/client/registry/README.md` | Transitional registry documentation | ✓ VERIFIED | Names `ParticleAssetRegistry` as transitional facade and its removal condition. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ParticleManager` | `ParticleStore<BrParticle>` | implements module API and returns `store()` | ✓ WIRED | Root runtime storage is now consumable as particle-module store API while preserving manager behavior. |
| `ParticleLookup` | `ParticleLookupApi` | `api()` returns `ParticleManager.store()` | ✓ WIRED | Lookup overloads call `api().get(...)`; names call `api().names()`. |
| `ParticleAssetRegistry` | `ParticlePublisher` | static `PUBLISHER` with description-identifier extractor | ✓ WIRED | `replaceParticles` passes only map values, preventing source-key usage. |
| `ParticlePublisher` | `ParticleStore.replaceAll` | `LinkedHashMap` replacement keyed by `identifier.identify` | ✓ WIRED | `ParticlePublisher.java:43-50` builds replacement and calls `store.replaceAll(replacement)`. |
| `NetClientHandlers` | `ParticleSpawnService` | packet handlers call service methods | ✓ WIRED | `NetClientHandlers.java:30-35` calls `removeEmitter(packet.removeId())` and `spawnFromPacket(packet)`. |
| `ParticleSpawnService` | `ParticleSpawnApi` | `api().spawn(new ParticleSpawnRequest(...))`, `api().remove(...)` | ✓ WIRED | Packet data is converted to module request before root runtime adapter work. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `ParticleAssetRegistry` | `Map<?, BrParticle> particles` | Loader/tooling call sites pass parsed particles; facade uses `particles.values()` | Yes | ✓ FLOWING — replacement reaches `ParticlePublisher` and then `ParticleManager.store().replaceAll`. |
| `ParticleLookup` | String id / `ResourceLocation` id | Runtime callers (`ParticleSpawnService`, animation/controller code) | Yes | ✓ FLOWING — id goes through `ParticleLookupApi.get` to manager-backed store. |
| `ParticleSpawnService` | `SpawnParticlePacket` fields | Network handlers pass packet; service builds module `ParticleSpawnRequest` | Yes | ✓ FLOWING — request drives root lookup and render-manager spawn/remove adapter. |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Particle module API contracts compile and tests pass | JetBrains MCP `:eyelib-particle:test`, `:eyelib-particle:compileJava` | Orchestrator evidence: exitCode 0 | ✓ PASS |
| Root adapters compile | JetBrains MCP `:compileJava` | Orchestrator evidence: exitCode 0 | ✓ PASS |
| Root identifier/delegation boundary tests pass | JetBrains MCP `:test --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistryTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest` | Orchestrator evidence: exitCode 0 | ✓ PASS |
| Broad root `:test` | JetBrains MCP `:test` | Failed in unrelated Bedrock/geometry fixture tests with `NoSuchFileException`; no evidence links this to Phase 9 particle API/store changes | ℹ️ RESIDUAL RISK, not a Phase 9 gap |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| PAPI-01 | 09-01, 09-02, 09-03 | Root runtime can access particle lookup, spawn/remove, store/publication, and initialization behavior through narrow particle-module APIs instead of owning particle internals directly. | ✓ SATISFIED | Module APIs exist; `ParticleManager`, `ParticleLookup`, `ParticleAssetRegistry`, and `ParticleSpawnService` consume/delegate through them; tests and compile checks pass. |
| PAPI-03 | 09-02, 09-03 | Any temporary root compatibility facade delegates to particle-module APIs and is documented as transitional. | ✓ SATISFIED | Javadocs/READMEs document transitional status/removal conditions; `ParticleApiDelegationBoundaryTest` checks API imports and documentation wording. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` | 238 | Existing `//TODO` in particle Bedrock runtime | ℹ️ Info | Pre-existing runtime/schema concern outside Phase 9 API/store seam; schema/runtime ownership is explicitly Phase 10/11 scope. |
| Broad root tests | N/A | Fixture `NoSuchFileException` failures | ℹ️ Residual risk | Full root suite still has unrelated Bedrock/geometry fixture failures; targeted Phase 9 tests and compile checks passed. Track separately if full-suite green is required for later gates. |

### Human Verification Required

None. Phase 9 is API/store/documentation/test-boundary work; no visual, hardware, external-service, or real-time user-flow check is required for the phase goal.

### Gaps Summary

No blocking gaps found. The phase goal is achieved: root particle lookup, store/publication, lifecycle/reset, and spawn/remove entrypoints now pass through narrow `io.github.tt432.eyelibparticle.api` seams; retained root facades are specific, documented transitional adapters; no broad compatibility layer or premature runtime move was found.

---

_Verified: 2026-05-09T05:20:46Z_
_Verifier: the agent (gsd-verifier)_
