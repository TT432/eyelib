# Phase 12 Pattern Map

**Mapped:** 2026-05-09  
**Scope:** Particle reload, active registry replacement, publication key semantics, and root compatibility adapters.

## Analog Files And Required Patterns

| Role | Existing file | Pattern to preserve |
|------|---------------|---------------------|
| JSON suffix reload scanning | `src/main/java/io/github/tt432/eyelib/client/loader/SimpleJsonWithSuffixResourceReloadListener.java` | `FileToIdConverter(directory, "." + suffix)` scans resources and maps file paths to prepared ids; errors are logged with source id. |
| Particle reload adapter | `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` | Constructor uses `super("particles", "json")`; root adapter currently receives `Map<ResourceLocation, JsonElement>` in `apply(...)`. Keep this resource discovery path unchanged. |
| Forge lifecycle registration | `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java` | `@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = MOD)` registers concrete reload listeners. Keep Minecraft/Forge registration here or in a documented client integration package, not pure module API/runtime. |
| Publication seam | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java` | `replaceParticles(Iterable)` copies into `LinkedHashMap`, uses identifier extractor, then `store.replaceAll(replacement)`. This is the deterministic replacement pattern. |
| Store contract | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java` | String-keyed `put`, `replaceAll`, `get`, `all`, and lifecycle clear semantics. Do not introduce `ResourceLocation` keys. |
| Runtime definition conversion | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` | Parse importer schema, validate required description/render fields, return `DataResult<ParticleDefinition>`; do not silently invent defaults for missing parity-critical fields. |
| Root registry adapter | `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` | Transitional root facade delegates to module publisher; tests must continue proving source keys are not active keys. |
| Root lookup/spawn compatibility | `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java`, `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | Keep public root entrypoints behavior-compatible while delegating active data to module-owned APIs/services. |
| Tooling import route | `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java` | Root owns file IO, texture upload, and event posting; particle JSON publication should route through the same module-owned publication seam as reload. |

## Concrete Code Excerpts For Executors

### Existing reload scanning contract

```java
// SimpleJsonWithSuffixResourceReloadListener.scanDirectory(...)
FileToIdConverter filetoidconverter = new FileToIdConverter(name, "." + suffix);
for (Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(resourceManager).entrySet()) {
    ResourceLocation resourcelocation = entry.getKey();
    ResourceLocation resourcelocation1 = filetoidconverter.fileToId(resourcelocation);
    JsonElement jsonelement = gson.fromJson(reader, JsonElement.class);
    output.put(resourcelocation1, jsonelement);
}
```

### Existing particle loader path to preserve at the boundary

```java
private BrParticleLoader() {
    super("particles", "json");
}

@Override
protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    // Phase 12 should delegate canonical parse/convert/publish to :eyelib-particle here.
}
```

### Existing deterministic publisher contract

```java
public void replaceParticles(Iterable<? extends T> particles) {
    Objects.requireNonNull(particles, "particles");
    LinkedHashMap<String, T> replacement = new LinkedHashMap<>();
    for (T particle : particles) {
        T checkedParticle = Objects.requireNonNull(particle, "particle");
        replacement.put(identify(checkedParticle), checkedParticle);
    }
    store.replaceAll(replacement);
}
```

### Existing conversion seam

```java
return DataResult.success(new ParticleDefinition(
        schema.formatVersion(),
        identifier,
        new ParticleDefinition.BasicRenderParameters(renderParameters.material(), renderParameters.texture()),
        effect.curves(),
        effect.events(),
        effect.components(),
        effect.billboardFlipbook()
));
```

## Required New Pattern

Module-owned loading/publication should follow this shape:

```java
public static ParticleLoadReport replaceFromJsonResources(Map<String, JsonElement> resources, Logger logger) {
    LinkedHashMap<String, ParticleDefinition> definitions = new LinkedHashMap<>();
    resources.forEach((sourceId, json) -> BrParticle.CODEC.parse(JsonOps.INSTANCE, json)
            .flatMap(ParticleDefinitionAdapter::fromSchema)
            .resultOrPartial(message -> logger.error("Couldn't parse particle data file {}: {}", sourceId, message))
            .ifPresent(definition -> definitions.put(definition.identifier(), definition)));
    ParticleDefinitionRegistry.replaceParticles(definitions.values());
    return ParticleLoadReport.from(definitions, resources.keySet());
}
```

Important constraints:

- The `sourceId` above is diagnostic/source metadata only.
- Active keys are `definition.identifier()` only.
- The module service uses importer `BrParticle`, not root legacy `client.particle.bedrock.BrParticle`.
- Root `ResourceLocation` conversion to string happens before the module service call.

## Boundary Scan Targets

Pure packages that must remain root/MC/Forge-clean:

- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/`
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/`
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/`

Allowed Minecraft/Forge-facing locations:

- `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java`
- existing root adapter packages under `src/main/java/io/github/tt432/eyelib/client/loader/`, `client/registry/`, `client/particle/`
- documented particle `client/**` integration only for render/client hooks, not reload purity unless explicitly documented.
