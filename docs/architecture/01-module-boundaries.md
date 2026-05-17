# Eyelib Module Boundaries

## Current Major Areas
- `eyelib-model/src/main/java/io/github/tt432/eyelibmodel/`: canonical model data types (Model, Bone, Cube, Face, Vertex, TextureMesh, GlobalBoneIdHandler, VisibleBox, locator tree).
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/`: importer/schema Forge functional module under `io.github.tt432.eyelibimporter.*`.
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`: Molang engine with Forge platform bindings under `platform/`.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`: particle module boundary (APIs, runtime, packets, loading).
- `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`: Bedrock material definitions and GL state.
- `eyelib-animation/src/main/java/io/github/tt432/eyelibanimation/`: animation runtime (clips, controllers, keyframes, state machines).
- `eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/`: Bedrock entity behavior component model.
- `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`: data attachment contracts and Forge capability wiring.
- `eyelib-network/src/main/java/io/github/tt432/eyelibnetwork/`: Forge SimpleChannel transport layer.
- `eyelib-util/src/main/java/io/github/tt432/eyelibutil/`: shared utility leaf module.
- `src/main/java/io/github/tt432/eyelib/client/`: client rendering, GUI, loaders, managers, tooling.
- `src/main/java/io/github/tt432/eyelib/capability/`: attachment registration and runtime hooks.
- `src/main/java/io/github/tt432/eyelib/common/`: shared behavior logic and commands.
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
- Root owns cross-module orchestration: `EntityRenderSystem`, capability wiring, network registration, command registration, loader/reload lifecycle, manager store infrastructure.
- Feature-specific packets belong in feature modules where dependency edges allow.
- Runtime executors (animation, controller, particle) are runtime implementation details that stay with their owning module.
- Importer owns schema/codec definitions; root owns runtime adaptation and `NativeImage`/texture upload.

## Breaking Refactor Rule
- Do not add new singleton reach-through methods to top-level `Eyelib`.
- Migrate remaining callers to domain-local read seams, then delete the legacy reach-through methods.
