# Phase 09: Particle API & Store Seam - Pattern Map

**Mapped:** 2026-05-09
**Files analyzed:** 16 likely new/modified files
**Analogs found:** 16 / 16

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java` | service | CRUD | `src/main/java/io/github/tt432/eyelib/client/manager/ManagerReadPort.java` + `ManagerWritePort.java` | role-match |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLookupApi.java` | service | request-response | `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` | exact |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java` | service | transform | `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` | exact |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLifecycle.java` | service | CRUD | `src/main/java/io/github/tt432/eyelib/client/manager/ManagerWritePort.java` | role-match |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnApi.java` | service | request-response | `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | role-match |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleApis.java` | provider | request-response | `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` | partial |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java` | config | request-response | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java` | exact |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` | config | request-response | existing same file | exact |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` | service | request-response | `src/main/java/io/github/tt432/eyelib/client/model/ModelLookup.java` | exact |
| `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` | store | CRUD | `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java` | exact |
| `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` | service | transform | `src/main/java/io/github/tt432/eyelib/client/registry/ClientEntityAssetRegistry.java` | exact |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | service | request-response | `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` + current `ParticleSpawnService.java` | role-match |
| `src/main/java/io/github/tt432/eyelib/client/particle/README.md` | config | request-response | existing same file | exact |
| `src/main/java/io/github/tt432/eyelib/client/registry/README.md` | config | request-response | existing same file | exact |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/*Test.java` | test | CRUD/request-response | `src/test/java/io/github/tt432/eyelib/client/manager/ManagerStorageTest.java` | role-match |
| `src/test/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistryTest.java` | test | transform | `src/test/java/io/github/tt432/eyelib/client/registry/ClientEntityAssetRegistryTest.java` | exact |

## Pattern Assignments

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java` (service, CRUD)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/manager/ManagerReadPort.java` + `ManagerWritePort.java`

**Imports / nullability pattern** (`ManagerReadPort.java` lines 1-5):
```java
package io.github.tt432.eyelib.client.manager;

import org.jspecify.annotations.Nullable;

import java.util.Map;
```

**Read-port method shape** (`ManagerReadPort.java` lines 7-14):
```java
public interface ManagerReadPort<T> {
    @Nullable
    T get(String name);

    Map<String, T> getAllData();

    String getManagerName();
}
```

**Write/lifecycle method shape** (`ManagerWritePort.java` lines 5-10):
```java
public interface ManagerWritePort<T> {
    void put(String name, T value);

    void replaceAll(Map<String, ? extends T> replacement);

    void clear();
}
```

**Copy with changes:** keep `String` keys and `Map<String, ? extends T>` replacement semantics, but do **not** import `io.github.tt432.eyelib.client.manager.*` into `:eyelib-particle`.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLookupApi.java` (service, request-response)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java`

**Static lookup behavior to preserve** (lines 14-24):
```java
public static @Nullable BrParticle get(ResourceLocation id) {
    return ParticleManager.readPort().get(id.toString());
}

public static @Nullable BrParticle get(String id) {
    return ParticleManager.readPort().get(id);
}

public static Collection<String> names() {
    return ParticleManager.readPort().getAllData().keySet();
}
```

**Cleaner model lookup analog** (`ModelLookup.java` lines 13-22):
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelLookup {
    @Nullable
    public static Model get(String name) {
        return ModelManager.readPort().get(name);
    }

    public static Map<String, Model> all() {
        return ModelManager.readPort().getAllData();
    }
}
```

**Copy with changes:** expose string-first lookup in particle API; leave `ResourceLocation` overload only in root transitional facade.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java` (service, transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java`

**Identifier-flattening rule** (lines 13-21):
```java
public static void replaceParticles(Map<?, BrParticle> particles) {
    LinkedHashMap<String, BrParticle> flattened = new LinkedHashMap<>();
    particles.forEach((ignored, particle) -> flattened.put(particle.particleEffect().description().identifier(), particle));
    ParticleManager.writePort().replaceAll(flattened);
}

public static void publishParticle(BrParticle particle) {
    ParticleManager.writePort().put(particle.particleEffect().description().identifier(), particle);
}
```

**Same-domain registry publication pattern** (`ClientEntityAssetRegistry.java` lines 16-20):
```java
public static void replaceClientEntities(Iterable<BrClientEntity> entities) {
    LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
    entities.forEach(entity -> flattened.put(entity.identifier(), entity));
    ClientEntityManager.writePort().replaceAll(flattened);
}
```

**Copy with changes:** make identifier extraction a named publisher/store responsibility; never key replacement by loader map key or `ResourceLocation.toString()`.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleLifecycle.java` (service, CRUD)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`

**Storage lifecycle behavior** (lines 31-37):
```java
public void replaceAll(Map<String, ? extends T> replacement) {
    storage.replaceAll(replacement);
}

public void clear() {
    storage.clear();
}
```

**Copy with changes:** expose narrow reset/clear behavior through particle API while keeping MC/client hook ownership in root adapters.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleSpawnApi.java` (service, request-response)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java` and `ParticleSpawnService.java`

**Platform-type-free request record** (`ParticleSpawnRequest.java` lines 7-12):
```java
public record ParticleSpawnRequest(String spawnId, String particleId, Vector3f position) {
    public ParticleSpawnRequest {
        spawnId = Objects.requireNonNull(spawnId, "spawnId");
        particleId = Objects.requireNonNull(particleId, "particleId");
        position = new Vector3f(Objects.requireNonNull(position, "position"));
    }
}
```

**Current root runtime adapter behavior** (`ParticleSpawnService.java` lines 16-32):
```java
public static void spawnFromPacket(SpawnParticlePacket packet) {
    ParticleSpawnRequest request = new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position());
    BrParticle particle = ParticleLookup.get(request.particleId());
    if (particle == null || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
        return;
    }

    RenderData<?> data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
    BrParticleRenderManager.spawnEmitter(
            request.spawnId(),
            new BrParticleEmitter(
                    particle,
                    data.getScope(),
                    Minecraft.getInstance().level,
                    request.position()
            )
    );
}
```

**Copy with changes:** API seam should carry `spawnId`/`particleId` request intent; concrete `Minecraft`, capability, emitter, and render-manager work remains in root `ParticleSpawnService`.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleApis.java` (provider, request-response)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java`

**Singleton access pattern** (lines 10-20):
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticleManager extends Manager<BrParticle> {
    public static final ParticleManager INSTANCE = new ParticleManager();

    public static ManagerReadPort<BrParticle> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<BrParticle> writePort() {
        return INSTANCE;
    }
}
```

**Copy with changes:** if a service holder is introduced, keep it narrow and explicit. Avoid a broad compatibility layer; expose only lookup/store/publisher/lifecycle/spawn seams needed by Phase 9.

---

### `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` (service, request-response)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/animation/AnimationLookup.java`

**Static facade style** (lines 10-18):
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationLookup {
    @Nullable
    public static Animation<?> get(String name) {
        return AnimationManager.readPort().get(name);
    }

    public static Collection<String> names() {
```

**Manager metadata pass-through** (`AnimationLookup.java` lines 21-27):
```java
public static int size() {
    return AnimationManager.readPort().getAllData().size();
}

public static String managerName() {
    return AnimationManager.readPort().getManagerName();
}
```

**Copy with changes:** retain Lombok private constructor and static compatibility methods, but make body delegate into `io.github.tt432.eyelibparticle.api`; document as transitional and keep ResourceLocation adaptation at this root boundary only.

---

### `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` (store, CRUD)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`

**Core manager storage behavior** (lines 10-20, 27-37):
```java
public abstract class Manager<T> implements ManagerReadPort<T>, ManagerWritePort<T> {
    private final ManagerStorage<T> storage = new ManagerStorage<>();

    public void put(String name, T value) {
        storage.put(name, value);
        ManagerEventPublishBridge.publishManagerEntryChanged(getManagerName(), name, value);
    }

    @Nullable
    public T get(String name) {
        return storage.get(name);
    }
```
```java
public Map<String, T> getAllData() {
    return storage.getAllData();
}

public void replaceAll(Map<String, ? extends T> replacement) {
    storage.replaceAll(replacement);
}

public void clear() {
    storage.clear();
}
```

**Copy with changes:** keep existing manager behavior/event publication but reclassify it as root backing adapter for particle-module store APIs, not canonical particle owner.

---

### `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` (service, transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/registry/ModelAssetRegistry.java`

**Write-side registry pattern** (lines 11-19):
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelAssetRegistry {
    public static void publishModels(Map<String, Model> models) {
        models.forEach(ModelManager.writePort()::put);
    }

    public static void replaceModels(Map<String, Model> models) {
        ModelManager.writePort().replaceAll(new LinkedHashMap<>(models));
    }
}
```

**Copy with changes:** keep static registry facade shape for root callers, but delegate into particle publisher API and preserve particle description identifier flattening.

---

### `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` (service, request-response)

**Analog:** `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`

**Network handler delegates to runtime service** (lines 30-36):
```java
public static void onRemoveParticlePacket(RemoveParticlePacket packet) {
    ParticleSpawnService.removeEmitter(packet.removeId());
}

public static void onSpawnParticlePacket(SpawnParticlePacket packet) {
    ParticleSpawnService.spawnFromPacket(packet);
}
```

**Spawn/remove runtime methods** (`ParticleSpawnService.java` lines 35-40):
```java
public static void spawnEmitter(String spawnId, BrParticleEmitter emitter) {
    BrParticleRenderManager.spawnEmitter(spawnId, emitter);
}

public static void removeEmitter(String removeId) {
    BrParticleRenderManager.removeEmitter(removeId);
}
```

**Copy with changes:** introduce particle API request/delegation seam without moving root runtime logic. `NetClientHandlers` should continue avoiding direct `BrParticleRenderManager` calls.

---

### Documentation files (config, request-response)

**Analogs:** `eyelib-particle/README.md`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`, `src/main/java/io/github/tt432/eyelib/client/registry/README.md`

**Particle module boundary wording** (`eyelib-particle README` lines 11-18):
```markdown
## Dependency Direction
- Root runtime may depend on :eyelib-particle, but :eyelib-particle must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
```

**Root particle communication rule** (`client/particle/README.md` lines 7-17):
```markdown
## Current Runtime Boundaries
- `ParticleLookup.java`: read-side access to particle definitions through the runtime manager boundary
- `ParticleSpawnService.java`: packet-driven spawn/remove orchestration on the client side
- `ParticleSpawnRequest.java`: platform-type-free spawn request state (`String` ids + position) used by runtime spawn orchestration

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
```

**Registry boundary wording** (`client/registry/README.md` lines 7-15):
```markdown
## Current Role
- This package now contains domain-specific publication seams instead of one central static facade.
- `AnimationAssetRegistry.java`, `MaterialAssetRegistry.java`, `ParticleAssetRegistry.java`, `RenderControllerAssetRegistry.java`, `ClientEntityAssetRegistry.java`, and `ModelAssetRegistry.java` each own one write-side publication lane into manager-backed runtime storage.
- Loaders and tooling should call importer parsers and runtime adapters as needed, then hand publication off to the matching domain registry instead of pushing directly into managers or a shared god-facade.
```

**Copy with changes:** update documentation to call retained root classes transitional, name their particle API delegation targets, and state removal conditions.

---

### Tests (test, CRUD / transform / request-response)

**Analogs:** `ManagerStorageTest.java`, `ClientEntityAssetRegistryTest.java`, `ClientLookupFacadeTest.java`, `SpawnParticlePacketTest.java`

**Store replacement lifecycle test** (`ManagerStorageTest.java` lines 24-35):
```java
@Test
void replaceAllOverwritesExistingEntries() {
    ManagerStorage<String> storage = new ManagerStorage<>();
    storage.put("stale", "old");

    LinkedHashMap<String, String> replacement = new LinkedHashMap<>();
    replacement.put("fresh", "new");
    storage.replaceAll(replacement);

    assertNull(storage.get("stale"));
    assertEquals("new", storage.get("fresh"));
}
```

**Identifier-key registry test pattern** (`ClientEntityAssetRegistryTest.java` lines 16-33):
```java
@AfterEach
void tearDown() {
    ClientEntityManager.writePort().clear();
}

@Test
void replaceClientEntitiesUsesEntityIdentifierAsStorageKey() {
    BrClientEntity stale = testEntity("eyelib:stale");
    ClientEntityManager.writePort().put(stale.identifier(), stale);

    BrClientEntity first = testEntity("eyelib:first");
    BrClientEntity second = testEntity("eyelib:second");
    ClientEntityAssetRegistry.replaceClientEntities(List.of(first, second));

    assertNull(ClientEntityManager.readPort().get("eyelib:stale"));
    assertEquals(first, ClientEntityManager.readPort().get("eyelib:first"));
    assertEquals(second, ClientEntityManager.readPort().get("eyelib:second"));
}
```

**Lookup facade test pattern** (`ClientLookupFacadeTest.java` lines 54-74):
```java
@Test
void particleLookupExposesNamesAndGetThroughLookupSeam() {
    BrParticle particle = BrParticle.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("""
            {
              "format_version": "1.10.0",
              "particle_effect": {
                "description": {
                  "identifier": "eyelib:test_particle",
                  "basic_render_parameters": {
                    "material": "particles_alpha",
                    "texture": "eyelib:test_particle"
                  }
                }
              }
            }
            """)).getOrThrow(false, AssertionError::new);
    ParticleManager.INSTANCE.put("eyelib:test_particle", particle);

    assertEquals(Set.of("eyelib:test_particle"), Set.copyOf(ParticleLookup.names()));
    assertSame(particle, ParticleLookup.get("eyelib:test_particle"));
}
```

**String-id packet/request test pattern** (`SpawnParticlePacketTest.java` lines 10-20):
```java
@Test
void packetCarriesStringParticleIdContract() {
    SpawnParticlePacket packet = new SpawnParticlePacket(
            "spawn-id",
            "not-a-resource-location",
            new Vector3f(1F, 2F, 3F)
    );

    assertEquals("spawn-id", packet.spawnId());
    assertEquals("not-a-resource-location", packet.particleId());
    assertEquals(new Vector3f(1F, 2F, 3F), packet.position());
}
```

## Shared Patterns

### Boundary / forbidden imports
**Source:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java` lines 1-8
**Apply to:** all new `eyelibparticle.api` files
```java
/**
 * Particle module API and core contract boundary for Eyelib.
 * <p>
 * The root runtime may consume this module, but this package must not depend back on root runtime
 * packages, root managers, root registries, root packets, root capability helpers, or
 * {@code io.github.tt432.eyelib.mc.impl} classes. Minecraft/Forge lifecycle wiring and other
 * platform bindings require explicit adapter documentation before introduction.
 */
```

### Static facade private-constructor style
**Source:** `ParticleLookup.java` lines 12-13; `ParticleAssetRegistry.java` lines 11-12
**Apply to:** retained root compatibility facades and optional narrow API holder
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleLookup {
```

### Store method vocabulary
**Source:** `ManagerReadPort.java` lines 7-14 and `ManagerWritePort.java` lines 5-10
**Apply to:** particle store/read/write/lifecycle APIs
```java
@Nullable
T get(String name);
Map<String, T> getAllData();
void put(String name, T value);
void replaceAll(Map<String, ? extends T> replacement);
void clear();
```

### Publication key selection
**Source:** `ParticleAssetRegistry.java` lines 13-20
**Apply to:** particle publisher/store replacement tests and root registry adapter
```java
particles.forEach((ignored, particle) -> flattened.put(particle.particleEffect().description().identifier(), particle));
ParticleManager.writePort().replaceAll(flattened);
```

### Request defensive copy / validation
**Source:** `ParticleSpawnRequest.java` lines 7-12
**Apply to:** spawn request seam tests and any new request payload class
```java
public ParticleSpawnRequest {
    spawnId = Objects.requireNonNull(spawnId, "spawnId");
    particleId = Objects.requireNonNull(particleId, "particleId");
    position = new Vector3f(Objects.requireNonNull(position, "position"));
}
```

### No auth/guard pattern
**Source:** phase scope and existing particle code
**Apply to:** all files
```text
No authentication/authorization pattern applies. Validation concern is identifier boundary placement: string-first API, ResourceLocation adaptation only in root/MC adapters.
```

## No Analog Found

All likely files have at least a role-match analog in the current codebase. Exact API class names remain planner discretion, but the store, lookup, publisher, lifecycle, spawn, documentation, and test patterns are covered above.

## Metadata

**Analog search scope:** supplied phase files; `src/main/java/io/github/tt432/eyelib/client/{particle,manager,registry,model,animation,render}`; `src/main/java/io/github/tt432/eyelib/network`; `src/test/java/io/github/tt432/eyelib/**`; `eyelib-particle/src/main/java/**`.
**Files scanned/read:** 34
**Pattern extraction date:** 2026-05-09
