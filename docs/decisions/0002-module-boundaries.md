# ADR-0002: Module Boundaries

**Status:** Accepted (amended by [ADR-0014](0014-flat-merge.md))  
**Context:** As Eyelib grew, clear module boundaries were needed to prevent dependency cycles and ownership ambiguity.  
**Decision:** Establish explicit ownership boundaries by package, with root owning cross-module orchestration.  
**Consequences:** Module boundaries are enforced by package naming convention and review, not by Gradle project isolation. All boundary-crossing changes require updating this document.

---

# Eyelib Module Boundaries

> ADR-0014 取消了 12 个 Gradle 子项目，所有源码合并到 root 单 Gradle 项目，统一到 `io.github.tt432.eyelib.<module>` 命名空间。模块边界现在由包名约定 + review 维护，不再由 Gradle project 隔离。

## Current Major Areas
- `src/main/java/io/github/tt432/eyelib/model/`: canonical model data types (Model, Bone, Cube, Face, Vertex, TextureMesh, GlobalBoneIdHandler, VisibleBox, locator tree).
- `src/main/java/io/github/tt432/eyelib/importer/`: importer/schema functional module. `io.github.tt432.eyelib.importer.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner.
- `src/main/java/io/github/tt432/eyelib/molang/`: Molang engine with Forge platform bindings under `platform/`.
- `src/main/java/io/github/tt432/eyelib/particle/`: particle module boundary (APIs, runtime, packets, loading). `io.github.tt432.eyelib.particle.runtime.ParticleDefinition` is the canonical module runtime definition owner. `ParticleDefinitionAdapter` is the schema adapter seam. The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- `src/main/java/io/github/tt432/eyelib/material/`: Bedrock material definitions and GL state.
- `src/main/java/io/github/tt432/eyelib/animation/`: animation runtime (clips, controllers, keyframes, state machines).
- `src/main/java/io/github/tt432/eyelib/behavior/`: Bedrock entity behavior component model.
- `src/main/java/io/github/tt432/eyelib/attachment/`: data attachment contracts and Forge capability wiring.
- `src/main/java/io/github/tt432/eyelib/network/`: Forge SimpleChannel transport layer (merged: root channel entrypoints + transport delegation).
- `src/main/java/io/github/tt432/eyelib/util/`: shared utility leaf module.
- `src/main/java/io/github/tt432/eyelib/track/`: ItemStack tracking module.
- `src/main/java/io/github/tt432/eyelib/bridge/`: hexagonal architecture adapter layer — implements domain Port interfaces for MC/Forge runtime.
- `src/main/java/io/github/tt432/eyelib/client/`: client rendering, GUI, loaders, managers, tooling.
- `src/main/java/io/github/tt432/eyelib/capability/`: attachment registration and runtime hooks.
- `src/main/java/io/github/tt432/eyelib/common/`: shared commands plus server-side Bedrock behavior pack loading, publication, runtime registries, and behavior-state synchronization orchestration.
- `src/main/java/io/github/tt432/eyelib/molang/mapping/`: root-coupled `MolangQuery`.
- `clientsmoke/`: external client smoke framework (composite build).

## Existing Patterns To Preserve
- Manager pattern around `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`.
- Loader pattern in `src/main/java/io/github/tt432/eyelib/client/loader/`.
- Visitor pattern in `src/main/java/io/github/tt432/eyelib/client/render/visitor/`.
- Codec-heavy serialization approach across model, animation, particle, and Molang-related types.

## Boundary Rules
- Package boundaries define the real architecture. A module package (e.g. `io.github.tt432.eyelib.util`) must not depend on root orchestration packages (`io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.common`).
- Root owns cross-module orchestration: `EntityRenderSystem`, capability wiring, network registration, command registration, loader/reload lifecycle, manager store infrastructure, and server-side behavior-pack publication/synchronization.
- Bedrock behavior packs are server-side runtime data: root common loads and publishes them on server lifecycle events, while the client receives only network-synchronized behavior state needed for rendering/Molang queries.
- Feature-specific packets belong in feature packages where dependency edges allow.
- Runtime executors (animation, controller, particle) are runtime implementation details that stay with their owning package.
- Importer owns schema/codec definitions; root owns runtime adaptation and `NativeImage`/texture upload.
- Particle module loading/publication is owned by `ParticleDefinitionRegistry` and `ParticleResourcePublication`, keyed by `ParticleDefinition.identifier()`. Particle packet contracts live under `io.github.tt432.eyelib.particle.network`.
- Particle command/network integration routes through `mc/impl/common/command` with platform-free shaping in `ParticleCommandRuntime`.
- Domain isolation (no `net.minecraft.*` imports in domain packages) is a convention enforced by review, not ArchUnit (see [ADR-0010](0010-hexagonal-architecture.md) and [ADR-0014](0014-flat-merge.md)).

## Breaking Refactor Rule
- Do not add new singleton reach-through methods to top-level `Eyelib`.
- Migrate remaining callers to domain-local read seams, then delete the legacy reach-through methods.
