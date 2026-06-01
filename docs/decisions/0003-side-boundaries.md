# ADR-0003: Side Boundaries (Client/Server Separation)

**Status:** Accepted  
**Context:** Minecraft's client/server split requires clear side ownership to prevent classloader issues and accidental server-side rendering references.  
**Decision:** Establish explicit client/server separation rules. All rendering, GUI, and model loading lives under `client/`. Common/shared logic under `common/`. Network transport under `network/`.  
**Consequences:** Subprojects must not depend on root. Root may depend on subprojects for feature-owned side-specific logic.

---

# Side Boundaries

## Client/Server Separation
- All rendering, GUI, and model loading lives under `src/main/java/io/github/tt432/eyelib/client/`.
- Common/shared logic lives under `src/main/java/io/github/tt432/eyelib/common/`.
- Network transport lives under `src/main/java/io/github/tt432/eyelib/network/` and `eyelib-network/`.

## Module Client Integration
- `io.github.tt432.eyelibparticle.network` contains particle-owned packet codecs as particle protocol contracts.
- Particle client integration uses `Dist.CLIENT` Forge hook delegation via client adapters.
- root `client/particle/bedrock/BrParticle` has been deleted.
- The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation. The importer module owns raw `io.github.tt432.eyelibimporter.particle.BrParticle` schema/codec.
- ResourceLocation adaptation for particle loading remains at root Forge/resource integration boundaries.

## Importer Module
- The importer module is currently an importer/schema Forge functional module.

## Particle Module
- `:eyelib-particle` owns module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable runtime, client integration, render manager.
- `ParticleDefinitionRegistry` is the module-owned active store and `ParticleResourcePublication` handles loading.
- Active keys use `ParticleDefinition.identifier()`.
- Phase 14 deferred scope includes ClientSmoke, hardware evidence, and PFUT-03 independent particle artifact publication.

## Cross-Module Dependency Rules
- Subproject `build.gradle` `project(:)` edges define the real architecture.
- A subproject must never depend on root (`io.github.tt432.eyelib.*`).
- Root may depend on subprojects.
