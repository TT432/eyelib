# Eyelib Module Inventory

## Purpose
- This file is the canonical module inventory for the repository.
- A "module" here means any maintained responsibility unit, regardless of size: package subtree, boundary seam, documentation subsystem, or narrow support area that has its own reason to exist.
- Use this file when deciding scope, ownership, affected areas, and summary regeneration requirements.

## Summary
- Eyelib is a multi-project `Gradle + Java 17 + Forge` repository: root runtime module plus importer/model subproject `eyelib-importer` and engine Molang subproject `eyelib-molang`.
- The repository currently centers on six large code domains: `bootstrap`, `client`, `molang`, `network/sync`, `dataattach/capability`, and `shared util/common`.
- Recent refactor work introduced several narrow seam modules to reduce context leakage: domain-specific `client/registry` writers, `client/gui/manager/io`, `client/gui/manager/reload`, `client/gui/manager/hotkey`, `client/particle` lookup/spawn seams, `network/dataattach`, and named client helper owners under `client/render/*`, `client/gui/preview/*`, and `client/*`.
- Utility split work now also includes additive platform-free seams under `core/util/*`, with adapter-style compatibility preserved in existing `util/*` callers, and selected Minecraft-facing utility bridges now quarantined under `mc/impl/*`.
- Documentation is also modularized: root guidance, architecture docs, index docs, plan docs, and package-local README files all act as maintained repository modules.

## Inventory

### Root And Documentation Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Root bootstrap guide | repository-wide working rules for humans and AI | `AGENTS.md` | points to repo map, architecture docs, package READMEs |
| Root package index | top-level code package overview | `src/main/java/io/github/tt432/eyelib/README.md` | routes readers into child modules |
| Core package index | top-level index for platform-free core helpers | `src/main/java/io/github/tt432/eyelib/core/README.md` | defines no-Minecraft/no-Forge rules for extracted helpers |
| MC package index | top-level index for Minecraft/Forge-facing boundary packages | `src/main/java/io/github/tt432/eyelib/mc/README.md` | defines `mc/api` vs `mc/impl` intent during the split |
| Repo map | root navigation index | `docs/index/repo-map.md` | entrypoint into architecture docs and package docs |
| Architecture control spec | refactor-stage rules, non-goals, rollback guidance | `docs/architecture/00-control-spec.md` | governs structural edits |
| Module boundaries doc | current→target ownership map and boundary notes | `docs/architecture/01-module-boundaries.md` | used to classify affected modules |
| Side boundaries doc | client/common/sync/dataattach side rules | `docs/architecture/02-side-boundaries.md` | governs packet/runtime edits |
| Generated code policy | generated-vs-handwritten Molang rule set | `docs/architecture/03-generated-code-policy.md` | governs parser regeneration/isolation |
| Architecture blueprint | target communication model and execution priorities | `ARCHITECTURE-BLUEPRINT.md` | governs communication-lane refactor work |
| Blockbench export reference | reference notes for Blockbench-exported Bedrock geometry fields and constraints | `docs/blockbench/bedrock-geometry-export-fields-reference.md` | supports Bedrock importer/render debugging and schema comparisons |
| External reference docs | source-backed Chinese reference notes for external Bedrock addon/container/pack facts and structures | `docs/ref/` | records official docs, community references, and real-sample findings for addon research |
| Client index | navigation for client runtime/tooling modules | `docs/index/client.md` | points to client package READMEs |
| Molang index | navigation for compiler/runtime/generated parser modules | `docs/index/molang.md` | points to `molang/` subareas |
| Network index | navigation for packets and sync modules | `docs/index/network.md` | points to `network/`, `util/data_attach/`, `network/dataattach/` |
| Util index | navigation for shared helper modules | `docs/index/util.md` | points to util subtree and split destinations |
| Refactor tracker | active implementation tracker for current boundary work | `work/main.md` | planning/maintenance reference |
| Resources importer subproject | Independently consumable importer mod/artifact for importer-owned resource definitions, source parsing, normalization, Bedrock addon/pack discovery, importer image/data representations, and importer-focused tests for model plus client-entity/animation-controller schema | `eyelib-importer/build.gradle`, `eyelib-importer/src/main/java/`, `eyelib-importer/src/main/resources/META-INF/mods.toml`, `eyelib-importer/src/test/` | consumed by root via `modImplementation project(':eyelib-importer')`; importer-owned code now lives under `io.github.tt432.eyelibimporter.*`, including addon/pack discovery and raw resource-side aggregation, while runtime execution/managers/upload stay in root |
| Molang engine subproject | Gradle subproject for Molang-owned value/runtime wrappers, scope/compiler/generated parser, built-in mappings, mapping-api/type code, and related tests | `eyelib-molang/build.gradle`, `eyelib-molang/src/main/java/`, `eyelib-molang/src/test/` | consumed by root via `modImplementation project(':eyelib-molang')`; code now lives under `io.github.tt432.eyelibmolang.*`, while root keeps only `mc/impl/molang/**` platform bindings |

### Bootstrap And API Surface Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Bootstrap entrypoint | mod startup, capability registration, network registration | `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`, `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/README.md`, `src/main/java/io/github/tt432/eyelib/Eyelib.java` | `EyelibMod` owns Forge startup composition; top-level `Eyelib` remains a constant holder for legacy call sites |
| API marker | future stable external API landing zone | `src/main/java/io/github/tt432/eyelib/api/README.md` | documents intended public surface |
| Internal marker | explicit default-internal policy | `src/main/java/io/github/tt432/eyelib/internal/README.md` | marks implementation packages as internal |

### Capability And Data Attachment Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Capability registry/data types | defines attachable data types and render-related data holders | `src/main/java/io/github/tt432/eyelib/capability/` | used by rendering, network, data attachment helpers |
| Capability component models | render/model component payload types plus RenderData-owned client/runtime component state; component-local invalidation state stays in `capability/component` while Forge client event hooks move to `mc/impl` | `src/main/java/io/github/tt432/eyelib/capability/component/`, `src/main/java/io/github/tt432/eyelib/mc/impl/capability/` | synced by network packets and consumed by client render/runtime orchestration; Forge event-bus subscription for manager/texture invalidation is owned by `mc/impl/capability` |
| Data attachment container layer | platform-type-free typed attachment storage and mutation contracts | `src/main/java/io/github/tt432/eyelib/util/data_attach/`, `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md`, `src/main/java/io/github/tt432/eyelib/util/data_attach/package-info.java` | used by capability registry, entity state, and network sync payload seams |
| MC attachment capability wiring | Forge capability/provider/event/NBT implementation for attachment containers | `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/`, `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/README.md` | binds Minecraft entity capabilities to `util/data_attach` storage and calls `network/dataattach` sync service |

### Client Runtime Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Client root | root index for all client-only runtime code | `src/main/java/io/github/tt432/eyelib/client/README.md`, `src/main/java/io/github/tt432/eyelib/client/package-info.java` | routes into render/model/loader/tooling submodules |
| Client animation runtime | runtime animation/controller execution, playback state, lookup seams, root-side adapters that compile importer-owned animation/controller schema into runtime executors, the current binding-only animation save/sync surface (`AnimationComponent.SerializableInfo`), transitional clip definition/sampler seams under existing Bedrock wrappers, and owner/executor splits that keep mutable runtime state inside root-only implementation classes | `src/main/java/io/github/tt432/eyelib/client/animation/` | depends on Molang, managers, render pipeline, capability/render-sync integration, and importer-owned controller schema plus transitional animation schema fragments from `:eyelib-importer` |
| Animation lookup seam | narrow runtime read access to animations | `src/main/java/io/github/tt432/eyelib/client/animation/AnimationLookup.java` | shields consumers from bootstrap reach-through |
| Client entity runtime | runtime-facing client-entity lookup/helpers and RenderData-side client-entity component support over importer-owned client-entity schema | `src/main/java/io/github/tt432/eyelib/client/entity/`, `src/main/java/io/github/tt432/eyelib/client/entity/README.md` | interacts with importer-owned parsing/schema, client-entity manager store, RenderData capability components, render controllers, and particles |
| Client entity lookup seam | narrow runtime read access to client entities through platform-type-free identifiers | `src/main/java/io/github/tt432/eyelib/client/entity/ClientEntityLookup.java` | used by render/runtime setup, with Minecraft id adaptation kept at MC-facing call sites |
| Client render pipeline | render parameters, visitors, targets, render helpers, render type resolution, texture IO/merging | `src/main/java/io/github/tt432/eyelib/client/render/` | depends on models, materials, and render-owned helper classes |
| Client render sync seam | applies model/animation sync packets into render state via a string-keyed model payload seam (`RenderModelSyncPayload`) and runtime-only decode wiring | `src/main/java/io/github/tt432/eyelib/client/render/sync/ClientRenderSyncService.java`, `src/main/java/io/github/tt432/eyelib/client/render/sync/RenderSyncApplyOps.java`, `src/main/java/io/github/tt432/eyelib/client/render/sync/RenderModelSyncPayload.java` | called from network client handlers; keeps `ResourceLocation` decode at client runtime apply boundary |
| Client model domain | runtime-facing model helpers, bake/runtime data, and root-side traversal/render adapters for imported models | `src/main/java/io/github/tt432/eyelib/client/model/` | consumes `eyelib-importer` model definitions and feeds render/runtime systems |
| Model importer seam | runtime importer facades, importer-image-to-runtime adaptation, and source-to-runtime integration | `src/main/java/io/github/tt432/eyelib/client/model/importer/` | delegates source parsing/model definitions and importer image data to `eyelib-importer`, keeps root-side runtime integration, NativeImage conversion/upload boundaries, and planner entrypoints |
| Model lookup seam | narrow runtime read access to models | `src/main/java/io/github/tt432/eyelib/client/model/ModelLookup.java` | used by components and runtime setup |
| Client material domain | material definitions and entries | `src/main/java/io/github/tt432/eyelib/client/material/` | used by render controllers and material manager |
| Client particle runtime | particle definitions, emitters, render manager, and spawn/remove services | `src/main/java/io/github/tt432/eyelib/client/particle/`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md` | depends on render level scope and service-routed emitter control; spawn request and packet particle-id seams are string-keyed, while MC identifier validation/adaptation remains in MC runtime command/network boundaries |
| Render-controller lookup seam | narrow runtime read access to render controllers | `src/main/java/io/github/tt432/eyelib/client/render/controller/RenderControllerLookup.java` | used by entity render setup |
| Client compatibility adapters | external client compatibility integrations | `src/main/java/io/github/tt432/eyelib/client/compat/` | bridges to surrounding mod/client systems |
| Client cursor/gl helpers | cursor and GL-specific client support | `src/main/java/io/github/tt432/eyelib/client/cursor/`, `src/main/java/io/github/tt432/eyelib/client/gl/` | low-level client support for rendering/tooling |
| Client tick/runtime hooks | client-side periodic runtime orchestration and deferred task scheduling | `src/main/java/io/github/tt432/eyelib/client/ClientTickHandler.java`, `src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java`, `src/main/java/io/github/tt432/eyelib/client/ClientTaskScheduler.java` | feeds render/tooling animation loops and next-tick actions |

### Client Loader, Manager, Registry, And Tooling Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Resource loader base | common resource loading pattern used by client loaders | `src/main/java/io/github/tt432/eyelib/client/loader/BrResourcesLoader.java`, `src/main/java/io/github/tt432/eyelib/client/loader/SimpleJsonWithSuffixResourceReloadListener.java` | base for JSON/suffix loading behavior while lifecycle ownership continues migrating toward `mc/impl` |
| Asset loaders | own reload orchestration and root-side adaptation for animations, materials, particles, entities, attachables, and render controllers while importer-owned schema parsing migrates into `:eyelib-importer` | `src/main/java/io/github/tt432/eyelib/client/loader/`, `src/main/java/io/github/tt432/eyelib/client/loader/README.md` | publish into domain-specific registry writers; loader registration lifecycle is bound in `mc/impl/client/loader/`; client-entity/attachable codecs and controller-set parsing now come from `:eyelib-importer`, while animation-entry parsing is still transitional |
| Client loader lifecycle hook | Forge client reload-listener registration for all `Br*Loader` listeners | `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/` | owns `RegisterClientReloadListenersEvent` wiring and registers concrete loader instances |
| Runtime managers | singleton manager stores for animations, models, materials, particles, render controllers, and client entities | `src/main/java/io/github/tt432/eyelib/client/manager/` | consumed through lookup seams and domain-specific writer classes; manager entry-change publication routes through `mc/api` bridge |
| Client registry seam | domain-specific loader/tooling→store publication boundaries | `src/main/java/io/github/tt432/eyelib/client/registry/`, `src/main/java/io/github/tt432/eyelib/client/registry/README.md` | used by loaders, manager import planner, and manager import actions; replacement seams are string-keyed where possible to avoid platform identifier leakage |
| Manager screen UI | developer/debug UI for importing and watching non-model client resources | `src/main/java/io/github/tt432/eyelib/client/gui/manager/`, `src/main/java/io/github/tt432/eyelib/client/gui/manager/README.md` | interacts with entities screen, import actions, and folder session helpers |
| Manager screen IO seam | async file/folder dialog handling | `src/main/java/io/github/tt432/eyelib/client/gui/manager/io/` | used only by manager screen UI |
| Manager screen reload seam | resource-folder import and file watch lifecycle for animations, controllers, particles, entities, Bedrock models, and textures, plus platform-free route/texture-key planning helpers and root-side runtime publication/upload orchestration over importer parsers | `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/` | publishes through domain-specific registry seams, holds monitored folder session state, and should avoid owning importer normalization logic |
| Manager screen hotkey seam | keybind registration and open-screen tick event | `src/main/java/io/github/tt432/eyelib/client/gui/manager/hotkey/` | opens `EyelibManagerScreen` without embedding event logic in UI class |
| Auxiliary client screens | extra screen tooling outside manager screen | `src/main/java/io/github/tt432/eyelib/client/gui/` | uses client/model/util helpers |

### Molang Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Molang runtime core | Molang value wrappers, expression entrypoints, and vector helpers (`MolangValue*`) | `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangValue*.java` | consumed by animation/render/controller/particle logic through `project(':eyelib-molang')` |
| Molang engine scope/compiler/type | Engine-owned scope, owner sets, compilation/cache/class generation, and Molang type system | `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangOwnerSet.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/type/` | exposed to root through `project(':eyelib-molang')`; no reverse dependency back into root runtime |
| Molang generated parser | generated lexer/parser/visitor artifacts | `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/README.md` | read-only generated code used by compiler |
| Molang legacy grammar marker | legacy handoff/doc marker for old parser location | `src/main/java/io/github/tt432/eyelib/molang/grammer/`, `src/main/java/io/github/tt432/eyelib/molang/grammer/README.md` | documentation compatibility only |
| Molang mappings | named Molang mapping support and built-in plain-JVM mapping entries without direct Minecraft/Forge imports | `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/`, `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` | supports compiler/runtime lookup inside `:eyelib-molang`; root platform bindings populate extra mappings through discovery ports |
| Molang MC adapter seams | isolates Forge scan and Minecraft query/runtime lifecycle wiring behind ports with concrete bindings under `mc/impl` | `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangMappingDiscovery.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/MolangQueryRuntime*.java`, `src/main/java/io/github/tt432/eyelib/mc/impl/molang/mapping/`, `src/main/java/io/github/tt432/eyelib/mc/impl/molang/compiler/` | engine mapping/compile code consumes ports from `:eyelib-molang`; `mc/impl` hook classes remain in root and bind Forge/client runtime |
| Manager event publish seam | isolates manager entry-change event publication behind platform-type-free bridge with Forge implementation in `mc/impl` | `src/main/java/io/github/tt432/eyelib/mc/api/client/manager/`, `src/main/java/io/github/tt432/eyelib/mc/impl/client/manager/` | `client/manager/Manager.java` publishes via `mc/api`; Forge `EVENT_BUS` posting is bound in `mc/impl` lifecycle hook |
| Molang type system | Molang object/array/primitive type layer | `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/type/` | used by runtime and render controllers via `:eyelib-molang` |
| Molang package index | local package guidance for Molang work plus root-side legacy marker handoff | `src/main/java/io/github/tt432/eyelib/molang/README.md`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md` | directs readers into engine/runtime areas and the root legacy marker |

### Network And Sync Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Network registration/channel | sync network entrypoint and transport delegation seam | `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`, `src/main/java/io/github/tt432/eyelib/network/package-info.java`, `src/main/java/io/github/tt432/eyelib/network/README.md` | delegates packet channel registration and side/context transport to `mc/impl/network/` |
| MC sync transport/channel wiring | Forge channel setup, packet context handling, side gating, and player/entity dispatch | `src/main/java/io/github/tt432/eyelib/mc/impl/network/`, `src/main/java/io/github/tt432/eyelib/mc/impl/network/README.md` | owns direct `SimpleChannel`, `NetworkEvent.Context`, `DistExecutor`, and packet distributor wiring |
| Client packet handlers | client application of sync packets | `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` | now context-free and delegates particle/data-attachment applies to dedicated runtime services |
| Model/animation sync packets | client synchronization of render/model components | `ModelComponentSyncPacket.java`, `AnimationComponentSyncPacket.java` | apply into render data on client |
| Particle packet layer | remove/spawn particle packet contracts with string-keyed particle id payload seam | `RemoveParticlePacket.java`, `SpawnParticlePacket.java` | client side delegates into particle service; packet id adaptation no longer requires `ResourceLocation` in `network/**` contract |
| Attachment update packets | generic and specialized attachment packet contracts | `DataAttachmentUpdatePacket.java`, `DataAttachmentSyncPacket.java`, `UniDataUpdatePacket.java`, `ExtraEntityDataPacket.java`, `ExtraEntityUpdateDataPacket.java`, `UpdateDestroyInfoPacket.java` | packet contracts for attachment and entity state sync |
| Network data-attach seam | transport-independent attachment payload/state mapping | `src/main/java/io/github/tt432/eyelib/network/dataattach/`, `src/main/java/io/github/tt432/eyelib/network/dataattach/README.md` | shared by runtime sync owners under `mc/impl/network/dataattach/` |

### Common, Command, Event, And Mixin Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Common behavior runtime | shared non-client entity behavior logic | `src/main/java/io/github/tt432/eyelib/common/`, `src/main/java/io/github/tt432/eyelib/common/behavior/` | server/common-side entity state updates |
| Entity extra-data handlers | entity extra-data lifecycle and sync triggers | `src/main/java/io/github/tt432/eyelib/common/EntityExtraDataHandler.java`, `EntityStatisticsHandler.java` | uses attachment helpers and network packets |
| Command module | particle-related command entrypoints | `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java` | registers Brigadier command wiring and dispatches particle sync packets via MC transport |
| Event module | custom Eyelib events for init, manager entry changes, texture changes | `src/main/java/io/github/tt432/eyelib/event/` | consumed across client runtime and tooling |
| Mixin module | targeted Minecraft integration hooks | `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/`, `src/main/resources/eyelib.mixins.json` | runtime integration with vanilla classes and mixin config ownership under `mc/impl` namespace |

### Shared Utility Modules
| Module | Responsibility | Main paths | Interactions |
|---|---|---|---|
| Core utility seams | platform-free helper ownership for extracted utility logic | `src/main/java/io/github/tt432/eyelib/core/util/` | consumed directly by callers that do not require Minecraft/Forge types |
| Util root | local index and rules for utility code | `src/main/java/io/github/tt432/eyelib/util/README.md` | routes into client/data_attach/codec/math/search |
| Util client deterministic helpers | reduced narrow helper area (`TexturePathHelper`) plus legacy model/animation pure helpers pending destination cleanup | `src/main/java/io/github/tt432/eyelib/util/client/` | consumed by render/model code during transitional utility split; no remaining direct MC/Forge/Blaze imports in this subtree after unused blit/bake helper cleanup |
| Texture path helper | compatibility adapter to core texture-path transform | `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java` | keeps existing util/client callers while migrating to `core/util/texture` |
| Render pose copy helper | `PoseStack.Pose` copy helper under render ownership | `src/main/java/io/github/tt432/eyelib/client/render/PoseCopies.java` | used by render params and visitors |
| Native image IO | image load/upload helpers | `src/main/java/io/github/tt432/eyelib/client/render/texture/NativeImageIO.java` | used by render controller textures, cursor loading, and manager import planner |
| Texture layer merger | merged texture atlas generation for layered render-controller textures | `src/main/java/io/github/tt432/eyelib/client/render/texture/TextureLayerMerger.java` | used by render controller texture composition |
| Render type resolver | render-type id to runtime factory resolution | `src/main/java/io/github/tt432/eyelib/client/render/RenderTypeResolver.java` | used by materials, model components, and particles |
| Model preview asset | preview-only model/atlas pair for GUI preview flow | `src/main/java/io/github/tt432/eyelib/client/gui/preview/ModelPreviewAsset.java` | used by `ModelPreviewScreen` |
| Codec utilities | custom stream/codec helpers plus core-wrapper adapters | `src/main/java/io/github/tt432/eyelib/util/codec/` | used by network packets and serialization; selected pure helpers route through `core/util/codec` |
| Math utilities | math and transform helpers plus core-wrapper adapters | `src/main/java/io/github/tt432/eyelib/util/math/` | used by render/model/UI helpers; selected channel transforms route through `core/util/color` |
| Search utilities | search/index result helpers | `src/main/java/io/github/tt432/eyelib/util/search/` | used by loaders and tooling UI |
| Modbridge utilities | integration bridge helpers without direct Forge event inheritance | `src/main/java/io/github/tt432/eyelib/util/modbridge/` | external/runtime integration glue and plain bridge IO |
| MC impl utility bridges | Minecraft/Forge-aware utility adapters and events moved out of legacy `util/*` | `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/`, `src/main/java/io/github/tt432/eyelib/mc/impl/util/time/`, `src/main/java/io/github/tt432/eyelib/mc/impl/util/model/` | consumed by client preview/event wiring, particle runtime, and model resource-location runtime helpers |
| Generic util helpers | lists, blackboards, resource locations, library loading, simple timer | `src/main/java/io/github/tt432/eyelib/util/*.java` | shared support across modules, with selected pure list helpers routed through `core/util/collection` |

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
