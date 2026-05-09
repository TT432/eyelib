---
phase: 09-particle-api-store-seam
reviewed: 2026-05-09T05:26:45Z
depth: deep
files_reviewed: 32
files_reviewed_list:
  - .planning/phases/09-particle-api-store-seam/09-01-SUMMARY.md
  - .planning/phases/09-particle-api-store-seam/09-02-SUMMARY.md
  - .planning/phases/09-particle-api-store-seam/09-03-SUMMARY.md
  - .planning/phases/09-particle-api-store-seam/09-VERIFICATION.md
  - MODULES.md
  - docs/index/repo-map.md
  - docs/architecture/01-module-boundaries.md
  - docs/architecture/02-side-boundaries.md
  - settings.gradle
  - build.gradle
  - eyelib-particle/build.gradle
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLookupApi.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLifecycle.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleIdentifier.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnRequest.java
  - eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnApi.java
  - src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java
  - src/main/java/io/github/tt432/eyelib/client/manager/Manager.java
  - src/main/java/io/github/tt432/eyelib/client/manager/ManagerStorage.java
  - src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java
  - src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java
  - src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java
  - src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java
  - src/main/java/io/github/tt432/eyelib/client/particle/README.md
  - src/main/java/io/github/tt432/eyelib/client/registry/README.md
  - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticlePublisherTest.java
  - eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/ParticleSpawnRequestTest.java
  - src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryTest.java
  - src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java
findings:
  critical: 1
  warning: 2
  info: 0
  total: 3
status: findings
---

# Phase 9: Code Review Report

**Reviewed:** 2026-05-09T05:26:45Z  
**Depth:** deep  
**Files Reviewed:** 32  
**Status:** findings

## Summary

Reviewed the Phase 9 particle API/store seam, root transitional delegation, boundary documentation, Gradle wiring, and test/static-check coverage. The module API is mostly root-clean and the delegation shape exists, but the root backing store breaks the claimed stable replacement order, the boundary tests leave a whole-module Minecraft/Forge contamination gap, and the root particle package still advertises an obsolete duplicate spawn request seam that is no longer used by the runtime adapter.

## Critical Issues

### CR-01: Root particle store discards the order that Phase 9 promises to preserve

**Classification:** BLOCKER  
**File:** `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java:40-42` and `src/main/java/io/github/tt432/eyelib/client/manager/ManagerStorage.java:10,25-28`  
**Issue:** `ParticlePublisher.replaceParticles` intentionally builds a `LinkedHashMap` and its Javadoc promises stable replacement order, but `ParticleManager.store()` delegates to `ManagerStorage`, whose backing `data` is a `HashMap`. `ManagerStorage.replaceAll` copies the ordered replacement into a `LinkedHashMap` and then immediately `putAll`s into the unordered `HashMap`, so `ParticleManager.store().all().keySet()` / `names()` are not guaranteed to preserve publication order. This contradicts Phase 9's own order-preservation claim and makes `ParticleAssetRegistryPublisherAdapterTest` a coincidental test: it can pass for the current two IDs while still failing for other hash distributions or JDK behavior.

**Fix:** Preserve insertion order in the backing storage, or make the particle manager override replacement/all-name behavior with an order-preserving store.

```java
// src/main/java/io/github/tt432/eyelib/client/manager/ManagerStorage.java
final class ManagerStorage<T> {
    private final Map<String, T> data = new LinkedHashMap<>();

    Map<String, T> getAllData() {
        return new LinkedHashMap<>(data);
    }

    void replaceAll(Map<String, ? extends T> replacement) {
        data.clear();
        data.putAll(replacement);
    }
}
```

Also add a root-level regression that proves `ParticleManager.store().replaceAll(new LinkedHashMap<>(...))` preserves a deliberately collision-prone or multi-entry insertion order.

## Warnings

### WR-01: Boundary tests do not actually guard all `:eyelib-particle` main sources against Minecraft/Forge imports

**Classification:** WARNING  
**File:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java:50-58` and `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java:29-44`  
**Issue:** The strongest Minecraft/Forge forbidden-import check is scoped only to `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api`, while the broader `eyelib-particle/src/main/java` check bans only selected root packages and does not ban `net.minecraft.*` or `net.minecraftforge.*`. Phase 9's boundary claim is module-level (`:eyelib-particle` must remain root/MC/Forge-clean), so a future non-API source under `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/...` could import Minecraft or Forge and still satisfy the current broad test.

**Fix:** Use one whole-module main-source scan with the complete forbidden set.

```java
Path sourceRoot = Path.of("eyelib-particle/src/main/java");
List<String> forbiddenFragments = List.of(
        "import io.github.tt432.eyelib.client.",
        "import io.github.tt432.eyelib.network.",
        "import io.github.tt432.eyelib.capability.",
        "import io.github.tt432.eyelib.mc.impl.",
        "import net.minecraft.",
        "import net.minecraftforge."
);
```

Then fail with the exact violating file list, as `ParticleApiDelegationBoundaryTest` already does for the API subpackage.

### WR-02: Root package still exposes and documents an obsolete duplicate `ParticleSpawnRequest`

**Classification:** WARNING  
**File:** `src/main/java/io/github/tt432/eyelib/client/particle/README.md:10` and `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java:7-12`  
**Issue:** Phase 9 moved packet spawn delegation to `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`, and `ParticleSpawnService` imports the module request. However, the root package still contains a same-named `client.particle.ParticleSpawnRequest` and the README says it is "used by runtime spawn orchestration". A project-wide text search found the root class referenced only by its stale README/tests, not by production runtime code. This creates a boundary regression trap: future root callers can accidentally choose the obsolete root seam, and the obsolete class has weaker semantics than the module request because it copies on construction but does not defensively copy on accessor return.

**Fix:** Delete the obsolete root `ParticleSpawnRequest` and its stale root tests if it is no longer part of the runtime seam, or explicitly deprecate it and update the README to direct callers to `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`.

```java
/**
 * @deprecated Use io.github.tt432.eyelibparticle.api.ParticleSpawnRequest.
 */
@Deprecated(forRemoval = true)
public record ParticleSpawnRequest(String spawnId, String particleId, Vector3f position) {
    public ParticleSpawnRequest {
        spawnId = Objects.requireNonNull(spawnId, "spawnId");
        particleId = Objects.requireNonNull(particleId, "particleId");
        position = new Vector3f(Objects.requireNonNull(position, "position"));
    }

    @Override
    public Vector3f position() {
        return new Vector3f(position);
    }
}
```

Preferred fix for Phase 9's seam goal is removal rather than keeping two same-named request types.

---

_Reviewed: 2026-05-09T05:26:45Z_  
_Reviewer: the agent (gsd-code-reviewer)_  
_Depth: deep_
