# ADR-0002: Module Boundaries

**Status:** Accepted  
**Context:** As Eyelib grew from a monolithic root module into a multi-project codebase, clear module boundaries were needed to prevent dependency cycles and ownership ambiguity.  
**Decision:** Establish explicit ownership boundaries for each Gradle subproject, with root owning cross-module orchestration and subprojects owning their domain logic.  
**Consequences:** Subprojects must never depend on root. Root may depend on subprojects. All boundary-crossing changes require updating this document.

---

# Eyelib Module Boundaries

## Current Major Areas
- `eyelib-model/src/main/java/io/github/tt432/eyelibmodel/`: canonical model data types (Model, Bone, Cube, Face, Vertex, TextureMesh, GlobalBoneIdHandler, VisibleBox, locator tree).
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/`: importer/schema Forge functional module under `io.github.tt432.eyelibimporter.*`. `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner.
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`: Molang engine with Forge platform bindings under `platform/`.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`: particle module boundary (APIs, runtime, packets, loading). `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` is the canonical module runtime definition owner. `ParticleDefinitionAdapter` is the schema adapter seam. Root `client/particle/bedrock/BrParticle` has been deleted. The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation. Phase 14 deferred scopes include ClientSmoke, hardware evidence, and PFUT-03 independent particle artifact publication.
- `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`: Bedrock material definitions and GL state.
- `eyelib-animation/src/main/java/io/github/tt432/eyelibanimation/`: animation runtime (clips, controllers, keyframes, state machines).
- `eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/`: Bedrock entity behavior component model.
- `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`: data attachment contracts and Forge capability wiring.
- `eyelib-network/src/main/java/io/github/tt432/eyelibnetwork/`: Forge SimpleChannel transport layer.
- `eyelib-util/src/main/java/io/github/tt432/eyelibutil/`: shared utility leaf module.
- `src/main/java/io/github/tt432/eyelib/client/`: client rendering, GUI, loaders, managers, tooling.
- `src/main/java/io/github/tt432/eyelib/capability/`: attachment registration and runtime hooks.
- `src/main/java/io/github/tt432/eyelib/common/`: shared commands plus server-side Bedrock behavior pack loading, publication, runtime registries, and behavior-state synchronization orchestration.
- `src/main/java/io/github/tt432/eyelib/network/`: shared channel entrypoints and root-coupled packet blockers.
- `src/main/java/io/github/tt432/eyelib/molang/mapping/`: root-coupled `MolangQuery`.
- `clientsmoke/`: external client smoke framework (composite build).

## Existing Patterns To Preserve
- Manager pattern around `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`.
- Loader pattern in `src/main/java/io/github/tt432/eyelib/client/loader/`.
- Visitor pattern in `src/main/java/io/github/tt432/eyelib/client/render/visitor/`.
- Codec-heavy serialization approach across model, animation, particle, and Molang-related types.

## Boundary Rules
- Subproject `build.gradle` `project(:)` edges define the real architecture. A subproject must never depend on root (`io.github.tt432.eyelib.*`).
- Root owns cross-module orchestration: `EntityRenderSystem`, capability wiring, network registration, command registration, loader/reload lifecycle, manager store infrastructure, and server-side behavior-pack publication/synchronization.
- Bedrock behavior packs are server-side runtime data: root common loads and publishes them on server lifecycle events, while the client receives only network-synchronized behavior state needed for rendering/Molang queries.
- Feature-specific packets belong in feature modules where dependency edges allow.
- Runtime executors (animation, controller, particle) are runtime implementation details that stay with their owning module.
- Importer owns schema/codec definitions; root owns runtime adaptation and `NativeImage`/texture upload.
- Particle module loading/publication is owned by `ParticleDefinitionRegistry` and `ParticleResourcePublication`, keyed by `ParticleDefinition.identifier()`. Particle packet contracts live under `io.github.tt432.eyelibparticle.network`.
- Root legacy `client/particle/bedrock/**` schema/runtime tree has been deleted.
- FM-015 supersedes package-name ownership for accessors: `LivingEntityRendererAccessor` is client-render-owned technical mixin wiring, physically hosted in one shared package root.
- FM-014 designates shared channel entrypoints and context-free handler dispatch as root network responsibility; feature-specific protocol contracts stay in subproject modules.
- Particle command/network integration routes through `mc/impl/common/command` with platform-free shaping in `ParticleCommandRuntime`. The `:eyelib-particle` module owns particle APIs, `ParticleDefinitionAdapter`, `ParticleDefinitionRegistry`, `ParticleResourcePublication`, and `io.github.tt432.eyelibparticle.network` packet contracts.

## Breaking Refactor Rule
- Do not add new singleton reach-through methods to top-level `Eyelib`.
- Migrate remaining callers to domain-local read seams, then delete the legacy reach-through methods.
