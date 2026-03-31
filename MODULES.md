# Eyelib Module Inventory

## Purpose
- This file is the canonical module inventory for the repository.
- A "module" here means any maintained responsibility unit, regardless of size: package subtree, boundary seam, documentation subsystem, or narrow support area that has its own reason to exist.
- Use this file when deciding scope, ownership, affected areas, and summary regeneration requirements.

## Summary
- Eyelib is a single-module `Gradle + Java 17 + Forge` repository with one runtime codebase and a growing documentation layer.
- The repository currently centers on six large code domains: `bootstrap`, `client`, `molang`, `network/sync`, `dataattach/capability`, and `shared util/common`.
- Recent refactor work introduced several narrow seam modules to reduce context leakage: `client/registry`, `client/gui/manager/io`, `client/gui/manager/reload`, `client/gui/manager/hotkey`, `client/particle` lookup/spawn seams, `network/dataattach`, and destination-driven helpers under `util/client/*`.
- Documentation is also modularized: root guidance, architecture docs, index docs, plan docs, and package-local README files all act as maintained repository modules.

## Inventory

### Root And Documentation Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Root bootstrap guide | repository-wide working rules for humans and AI | `AGENTS.md` | points to repo map, architecture docs, package READMEs |
| Root package index | top-level code package overview | `src/main/java/io/github/tt432/eyelib/README.md` | routes readers into child modules |
| Repo map | root navigation index | `docs/index/repo-map.md` | entrypoint into architecture docs and package docs |
| Architecture control spec | refactor-stage rules, non-goals, rollback guidance | `docs/architecture/00-control-spec.md` | governs structural edits |
| Module boundaries doc | current→target ownership map and boundary notes | `docs/architecture/01-module-boundaries.md` | used to classify affected modules |
| Side boundaries doc | client/common/sync/dataattach side rules | `docs/architecture/02-side-boundaries.md` | governs packet/runtime edits |
| Generated code policy | generated-vs-handwritten Molang rule set | `docs/architecture/03-generated-code-policy.md` | governs parser regeneration/isolation |
| Architecture blueprint | target communication model and execution priorities | `ARCHITECTURE-BLUEPRINT.md` | governs communication-lane refactor work |
| Client index | navigation for client runtime/tooling modules | `docs/index/client.md` | points to client package READMEs |
| Molang index | navigation for compiler/runtime/generated parser modules | `docs/index/molang.md` | points to `molang/` subareas |
| Network index | navigation for packets and sync modules | `docs/index/network.md` | points to `network/`, `util/data_attach/`, `network/dataattach/` |
| Util index | navigation for shared helper modules | `docs/index/util.md` | points to util subtree and split destinations |
| Refactor plan | staged implementation plan for current boundary work | `docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md` | planning/maintenance reference |

### Bootstrap And API Surface Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Bootstrap entrypoint | mod startup, capability registration, network registration, compatibility bridge | `src/main/java/io/github/tt432/eyelib/Eyelib.java`, `src/main/java/io/github/tt432/eyelib/package-info.java` | touches capabilities, network, and a reduced compatibility surface |
| API marker | future stable external API landing zone | `src/main/java/io/github/tt432/eyelib/api/README.md` | documents intended public surface |
| Internal marker | explicit default-internal policy | `src/main/java/io/github/tt432/eyelib/internal/README.md` | marks implementation packages as internal |

### Capability And Data Attachment Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Capability registry/data types | defines attachable data types and render-related data holders | `src/main/java/io/github/tt432/eyelib/capability/` | used by rendering, network, data attachment helpers |
| Capability component models | render/model component payload types | `src/main/java/io/github/tt432/eyelib/capability/component/` | synced by network packets, consumed by client renderers |
| Data attachment container layer | typed attachment storage, providers, local mutation helpers | `src/main/java/io/github/tt432/eyelib/util/data_attach/`, `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md`, `src/main/java/io/github/tt432/eyelib/util/data_attach/package-info.java` | used by entity state, network sync service, event handlers |
| Data attachment event sync | sends full attachment snapshots when tracking begins | `src/main/java/io/github/tt432/eyelib/util/data_attach/DataAttachmentEventHandlers.java` | now calls `network/dataattach` service |

### Client Runtime Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Client root | root index for all client-only runtime code | `src/main/java/io/github/tt432/eyelib/client/README.md`, `src/main/java/io/github/tt432/eyelib/client/package-info.java` | routes into render/model/loader/tooling submodules |
| Client animation runtime | animation definitions, runtime state, controller handling | `src/main/java/io/github/tt432/eyelib/client/animation/` | depends on Molang, managers, render pipeline |
| Animation lookup seam | narrow runtime read access to animations | `src/main/java/io/github/tt432/eyelib/client/animation/AnimationLookup.java` | shields consumers from bootstrap reach-through |
| Client entity runtime | client entity definitions and runtime helpers | `src/main/java/io/github/tt432/eyelib/client/entity/` | interacts with loader parsing, client-entity manager store, render controllers, and particles |
| Client entity lookup seam | narrow runtime read access to client entities | `src/main/java/io/github/tt432/eyelib/client/entity/ClientEntityLookup.java` | used by render/runtime setup |
| Client render pipeline | render parameters, visitors, targets, render helpers | `src/main/java/io/github/tt432/eyelib/client/render/` | depends on models, materials, util client helpers |
| Client render sync seam | applies model/animation sync packets into render state | `src/main/java/io/github/tt432/eyelib/client/render/sync/ClientRenderSyncService.java` | called from network client handlers |
| Client model domain | model structures, bake/runtime data, locators, and source-format parsers for model inputs | `src/main/java/io/github/tt432/eyelib/client/model/` | used by render pipeline, loaders, and importer seam |
| Model importer seam | importer entrypoints and source-to-runtime model conversion | `src/main/java/io/github/tt432/eyelib/client/model/importer/` | consumes `client/model/bbmodel` and later `client/model/bedrock`, produces runtime `Model` instances |
| Model lookup seam | narrow runtime read access to models | `src/main/java/io/github/tt432/eyelib/client/model/ModelLookup.java` | used by components and runtime setup |
| Client material domain | material definitions and entries | `src/main/java/io/github/tt432/eyelib/client/material/` | used by render controllers and material manager |
| Client particle runtime | particle definitions, emitters, render manager, and spawn/remove services | `src/main/java/io/github/tt432/eyelib/client/particle/`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md` | depends on render level scope, network packet handlers, and service-routed emitter control |
| Render-controller lookup seam | narrow runtime read access to render controllers | `src/main/java/io/github/tt432/eyelib/client/render/controller/RenderControllerLookup.java` | used by entity render setup |
| Client compatibility adapters | external client compatibility integrations | `src/main/java/io/github/tt432/eyelib/client/compat/` | bridges to surrounding mod/client systems |
| Client cursor/gl helpers | cursor and GL-specific client support | `src/main/java/io/github/tt432/eyelib/client/cursor/`, `src/main/java/io/github/tt432/eyelib/client/gl/` | low-level client support for rendering/tooling |
| Client tick/runtime hooks | client-side periodic runtime orchestration | `src/main/java/io/github/tt432/eyelib/client/ClientTickHandler.java`, `src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java` | feeds render/tooling animation loops |

### Client Loader, Manager, Registry, And Tooling Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Resource loader base | common reload-listener base and resource loading pattern | `src/main/java/io/github/tt432/eyelib/client/loader/BrResourcesLoader.java` | base for all JSON/suffix loaders |
| Asset loaders | parse/reload animations, materials, particles, entities, attachables, and render controllers | `src/main/java/io/github/tt432/eyelib/client/loader/`, `src/main/java/io/github/tt432/eyelib/client/loader/README.md` | publish into registry seam and some legacy loaders |
| Runtime managers | singleton manager stores for animations, models, materials, particles, render controllers, and client entities | `src/main/java/io/github/tt432/eyelib/client/manager/` | partially exposed via `Eyelib.java` transitional bridge |
| Client registry seam | centralized loader/tooling→store publication boundary for non-model client assets | `src/main/java/io/github/tt432/eyelib/client/registry/`, `src/main/java/io/github/tt432/eyelib/client/registry/README.md` | used by loaders, manager import planner, and client-entity publication into manager/store |
| Manager screen UI | developer/debug UI for importing and watching non-model client resources | `src/main/java/io/github/tt432/eyelib/client/gui/manager/`, `src/main/java/io/github/tt432/eyelib/client/gui/manager/README.md` | interacts with entities screen, import planner, folder watcher |
| Manager screen IO seam | async file/folder dialog handling | `src/main/java/io/github/tt432/eyelib/client/gui/manager/io/` | used only by manager screen UI |
| Manager screen reload seam | resource-folder import and file watch lifecycle for animations, controllers, particles, entities, Bedrock models, and textures | `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/` | publishes through client registry seam |
| Manager screen hotkey seam | keybind registration and open-screen tick event | `src/main/java/io/github/tt432/eyelib/client/gui/manager/hotkey/` | opens `EyelibManagerScreen` without embedding event logic in UI class |
| Auxiliary client screens | extra screen tooling outside manager screen | `src/main/java/io/github/tt432/eyelib/client/gui/` | uses client/model/util helpers |

### Molang Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Molang runtime core | Molang value objects, scope handling, owner sets | `src/main/java/io/github/tt432/eyelib/molang/MolangScope.java`, `MolangValue*.java`, `MolangOwnerSet.java` | consumed by animation/render/controller logic |
| Molang compiler | compile/cache/class generation for Molang expressions | `src/main/java/io/github/tt432/eyelib/molang/compiler/` | depends on generated parser and cache policy |
| Molang generated parser | generated lexer/parser/visitor artifacts | `src/main/java/io/github/tt432/eyelib/molang/generated/`, `src/main/java/io/github/tt432/eyelib/molang/generated/README.md` | read-only generated code used by compiler |
| Molang legacy grammar marker | legacy handoff/doc marker for old parser location | `src/main/java/io/github/tt432/eyelib/molang/grammer/`, `src/main/java/io/github/tt432/eyelib/molang/grammer/README.md` | documentation compatibility only |
| Molang mappings | named Molang mapping support | `src/main/java/io/github/tt432/eyelib/molang/mapping/` | supports compiler/runtime lookup |
| Molang type system | Molang object/array/primitive type layer | `src/main/java/io/github/tt432/eyelib/molang/type/` | used by runtime and render controllers |
| Molang package index | local package guidance for Molang work | `src/main/java/io/github/tt432/eyelib/molang/README.md` | directs readers into compiler/generated/runtime areas |

### Network And Sync Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Network registration/channel | packet channel setup and side-aware dispatch | `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`, `src/main/java/io/github/tt432/eyelib/network/package-info.java`, `src/main/java/io/github/tt432/eyelib/network/README.md` | central entrypoint for all packet types |
| Client packet handlers | client application of sync packets | `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` | now delegates particle and data-attachment flows to dedicated services |
| Model/animation sync packets | client synchronization of render/model components | `ModelComponentSyncPacket.java`, `AnimationComponentSyncPacket.java` | apply into render data on client |
| Particle packet layer | remove/spawn particle packet contracts | `RemoveParticlePacket.java`, `SpawnParticlePacket.java` | client side delegates into particle service |
| Attachment update packets | generic and specialized attachment packet contracts | `DataAttachmentUpdatePacket.java`, `DataAttachmentSyncPacket.java`, `UniDataUpdatePacket.java`, `ExtraEntityDataPacket.java`, `ExtraEntityUpdateDataPacket.java`, `UpdateDestroyInfoPacket.java` | packet contracts for attachment and entity state sync |
| Network data-attach seam | dedicated attachment sync send/apply service | `src/main/java/io/github/tt432/eyelib/network/dataattach/`, `src/main/java/io/github/tt432/eyelib/network/dataattach/README.md` | bridges `network/` and `util/data_attach/` |

### Common, Command, Event, And Mixin Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Common behavior runtime | shared non-client entity behavior logic | `src/main/java/io/github/tt432/eyelib/common/`, `src/main/java/io/github/tt432/eyelib/common/behavior/` | server/common-side entity state updates |
| Entity extra-data handlers | entity extra-data lifecycle and sync triggers | `src/main/java/io/github/tt432/eyelib/common/EntityExtraDataHandler.java`, `EntityStatisticsHandler.java` | uses attachment helpers and network packets |
| Command module | particle-related command entrypoints | `src/main/java/io/github/tt432/eyelib/command/EyelibParticleCommand.java` | sends particle/network packets |
| Event module | custom Eyelib events for init, manager entry changes, texture changes | `src/main/java/io/github/tt432/eyelib/event/` | consumed across client runtime and tooling |
| Mixin module | targeted Minecraft integration hooks | `src/main/java/io/github/tt432/eyelib/mixin/` | runtime integration with vanilla classes |

### Shared Utility Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Util root | local index and rules for utility code | `src/main/java/io/github/tt432/eyelib/util/README.md` | routes into client/data_attach/codec/math/search |
| Util client facade layer | legacy/transitional client helper area | `src/main/java/io/github/tt432/eyelib/util/client/` | some callers still use compatibility facades |
| Texture path helper | deterministic texture path transformation | `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java` | used by rendering and texture composition |
| Pose copy helper | `PoseStack.Pose` copy helper | `src/main/java/io/github/tt432/eyelib/util/client/render/PoseCopies.java` | used by render params and visitors |
| Inventory model resource helper | inventory `ModelResourceLocation` helper | `src/main/java/io/github/tt432/eyelib/util/client/model/InventoryModelResourceLocations.java` | used by image/model preview flow |
| Native image utilities | image load/upload helpers | `src/main/java/io/github/tt432/eyelib/util/client/NativeImages.java` | used by render controller textures and manager import planner |
| Texture composition facade | texture layer merging and emissive path facade | `src/main/java/io/github/tt432/eyelib/util/client/Textures.java` | still used by render controller texture composition |
| Codec utilities | custom stream/codec helpers | `src/main/java/io/github/tt432/eyelib/util/codec/` | used by network packets and serialization |
| Math utilities | math and transform helpers | `src/main/java/io/github/tt432/eyelib/util/math/` | used by render/model/UI helpers |
| Search utilities | search/index result helpers | `src/main/java/io/github/tt432/eyelib/util/search/` | used by loaders and tooling UI |
| Modbridge utilities | integration bridge helpers | `src/main/java/io/github/tt432/eyelib/util/modbridge/` | external/runtime integration glue |
| Generic util helpers | lists, timers, blackboards, resource locations, library loading | `src/main/java/io/github/tt432/eyelib/util/*.java` | shared support across modules |

## Module Update Rules
1. Before any change, identify every affected module in this file.
2. If a change modifies code or docs inside a listed module, update that module’s summary if its responsibility, paths, or interactions changed.
3. If a change adds a new module or removes an existing module, update this file in the same change.
4. If a change alters module boundaries, also update `docs/architecture/01-module-boundaries.md` and any relevant package README files.
5. If a change affects packet/data-attachment/client-side applicability, also re-check `docs/architecture/02-side-boundaries.md`.

## Regeneration Checklist
- Re-read the affected package README files.
- Re-check the current package tree under `src/main/java/io/github/tt432/eyelib/`.
- Update the affected module rows in this file.
- Add/remove rows for newly created or deleted modules.
- Re-check links from `AGENTS.md`, `docs/index/repo-map.md`, and any touched package README files.
