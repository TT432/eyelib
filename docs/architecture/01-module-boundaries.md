# Eyelib Module Boundaries

## Current Major Areas
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/model/`: model definitions, locators, source formats (`bbmodel`, `bedrock`), and importer normalization/support data.
- `src/main/java/io/github/tt432/eyelib/client/`: client rendering, animation, particles, GUI, loaders, managers, tooling.
- `src/main/java/io/github/tt432/eyelib/molang/`: legacy Molang marker/docs handoff path.
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`: engine-owned Molang value/runtime wrappers, scope/compiler/type/mapping-api/built-in mappings, plus generated grammar artifacts.
- `src/main/java/io/github/tt432/eyelib/network/`: packet registration and client/server packet handling.
- `src/main/java/io/github/tt432/eyelib/capability/`: attachment-related capability registration and data holders.
- `src/main/java/io/github/tt432/eyelib/common/`: shared behavior logic.
- `src/main/java/io/github/tt432/eyelib/util/`: shared helpers plus mixed client/data-attachment utilities.
- `src/main/java/io/github/tt432/eyelib/mc/`: Minecraft/Forge-facing implementation tree being introduced as the hard import quarantine zone.

## Existing Patterns To Preserve
- Manager pattern around `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`.
- Loader pattern in `src/main/java/io/github/tt432/eyelib/client/loader/`.
- Visitor pattern in `src/main/java/io/github/tt432/eyelib/client/render/visitor/`.
- Codec-heavy serialization approach across model, animation, particle, and Molang-related types.

## Current Boundary Problems
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java` is now narrowed to UI composition and action delegation, with folder session state and import actions extracted into dedicated helpers.
- `src/main/java/io/github/tt432/eyelib/molang/grammer/` is a legacy name that historically made generated parser artifacts look like normal source files.
- `src/main/java/io/github/tt432/eyelib/client/loader/` now parses non-model assets into local maps first, then hands publication to domain-specific registry owners in `client/registry/`.
- `src/main/java/io/github/tt432/eyelib/util/client/` has been drained of its major historical shims; render type resolution, native image IO, texture merging, preview assets, and client tick scheduling now live under named client owners.
- First-wave utility split now has additive core seams under `src/main/java/io/github/tt432/eyelib/core/util/`, while legacy `util/` helpers remain as compatibility adapters where needed.
- `src/main/java/io/github/tt432/eyelib/network/` and `src/main/java/io/github/tt432/eyelib/util/data_attach/` now share a narrower sync seam via `network/dataattach/DataAttachmentSyncPayloadOps.java`, while transport/context/apply runtime ownership moved under `mc/impl/network/`; attachment writes no longer auto-trigger sync implicitly through `DataAttachmentHelper`.
- `src/main/java/io/github/tt432/eyelib/util/data_attach/` now owns platform-type-free attachment contracts/storage only; Forge capability provider/event wiring plus NBT persistence moved under `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/`.
- Core runtime reads are starting to move away from `Eyelib.java` reach-through and into domain lookup seams such as `client/animation/AnimationLookup.java`, `client/model/ModelLookup.java`, `client/entity/ClientEntityLookup.java`, and `client/render/controller/RenderControllerLookup.java`.
- First hard-quarantine move is now in place for Molang: Minecraft query/runtime lifecycle hooks and query implementation moved under `src/main/java/io/github/tt432/eyelib/mc/impl/molang/`, while the root `src/main/java/io/github/tt432/eyelib/molang/` path is now only a legacy marker/docs handoff.
- Next hard-quarantine move is in place for manager/registry publication seams: `client/manager/Manager.java` now publishes through `mc/api` bridge contracts, Forge event-bus wiring moved to `src/main/java/io/github/tt432/eyelib/mc/impl/client/manager/`, and `client/registry/ClientEntityAssetRegistry.java` no longer exposes `ResourceLocation` in its replacement seam.
- Utility hard-quarantine follow-up is in progress: fixed-step timer Minecraft runtime access and Forge `modbridge` update event ownership moved to `src/main/java/io/github/tt432/eyelib/mc/impl/util/time/` and `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/`, while deterministic timer state moved to `src/main/java/io/github/tt432/eyelib/core/util/time/`.
- Utility hard-quarantine follow-up advanced on legacy helper cleanup: `util/client/render/PoseCopies` moved to render ownership at `src/main/java/io/github/tt432/eyelib/client/render/PoseCopies.java`, inventory `ModelResourceLocation` helper ownership moved from `util/client/model` to `src/main/java/io/github/tt432/eyelib/mc/impl/util/model/InventoryModelResourceLocations.java`, unused facade shims `util/client/PoseHelper` + `util/client/ModelResourceLocationHelper` were removed, and dead bridge wrappers `util/client/BakedModels` + `util/client/BlitCall` + `util/client/BufferBuilders` were deleted; remaining utility non-`mc/impl` runtime imports are now concentrated in `util/codec/*`, `util/ResourceLocations`, and `util/math/Shapes`.
- Client-loader hard-quarantine follow-up is in progress: Forge `RegisterClientReloadListenersEvent` wiring for concrete `Br*Loader` classes is now owned by `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java`, and `client/loader/LoaderParsingOps` no longer exposes `ResourceLocation` in parse/translation method signatures.
- Client-render hard-quarantine follow-up has started from the sync seam: `client/render/sync/RenderSyncApplyOps` now uses string-keyed `RenderModelSyncPayload` for model payload collection/replacement, while `ResourceLocation` decode stays in `client/render/sync/ClientRenderSyncService` runtime wiring.
- Client-particle hard-quarantine follow-up has started from the spawn/request seam: `client/particle/ParticleSpawnRequest` now uses platform-type-free string ids and request state only, and `network/SpawnParticlePacket` now uses a string-keyed particle id contract; Minecraft identifier validation/adaptation stays in `mc/impl/common/command` and transport runtime boundaries.
- Common-runtime hard-quarantine follow-up has started from the command hotspot: command registration/runtime wiring moved to `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java`, while deterministic suggestion/request/message shaping now lives in platform-type-free `src/main/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntime.java`.
- Client-gui-tools hard-quarantine follow-up has started from reload planning state: `client/gui/manager/reload/ManagerResourceReloadPlan` now owns platform-free path normalization, single-file route classification, and texture-key planning (`Path`/`String` only), while `ManagerResourceImportPlanner` keeps Minecraft/Forge texture upload and event posting runtime wiring.
- Client-model hard-quarantine follow-up has started from definition visible-bounds state: `Model` and importer-side `ImportedModelData` now use platform-free `VisibleBox` instead of `AABB`; remaining MC runtime ownership in this area is concentrated in bake/model-part/render helpers (`ModelPart`, `PartPose`, `PoseStack`, and texture/runtime bake paths).
- Client-model hard-quarantine follow-up advanced with a second definition-side seam: `Model.TextureMesh` vector codec fields no longer use direct `net.minecraft.util.ExtraCodecs` from `client/model/Model.java` and now route through Eyelib float-list codecs; remaining MC runtime ownership in this area is still concentrated in bake/model-part/render helpers (`ModelPart`, `PartPose`, `PoseStack`, texture/runtime bake paths) plus importer/runtime native image flow.
- Capability runtime-component hard-quarantine follow-up has started: Forge `ManagerEntryChangedEvent` and `TextureChangedEvent` listener ownership for `AnimationComponent`/`RenderControllerComponent` invalidation now lives in `src/main/java/io/github/tt432/eyelib/mc/impl/capability/CapabilityComponentRuntimeHooks.java`, while component-local invalidation state/methods stay in `capability/component/`.
- Mixin integration hard-quarantine relocation has now landed: mixin class/package ownership moved from legacy `io.github.tt432.eyelib.mixin` to `src/main/java/io/github/tt432/eyelib/mc/impl/mixin/`, and `src/main/resources/eyelib.mixins.json` now points at `io.github.tt432.eyelib.mc.impl.mixin`.
- Importer/model split has landed as a Gradle subproject: `:eyelib-importer` now owns `client/model/Model`, locator/tree support, `client/model/bbmodel`, `client/model/bedrock`, and importer-side data/repacker/support classes; root `client/model/importer` keeps runtime adapter/build steps and tooling entrypoints, while render traversal stays in root visitors.
- Next importer-boundary expansion is now materially landed: `eyelib-importer` owns client-entity/attachable schema codecs, importer image data (`ImportedImageData`), controller-side schema records/sets, and importer-side model/entity/animation schema under the dedicated `io.github.tt432.eyelibimporter.*` namespace, while root `client/animation`, `client/entity`, `client/loader`, and `client/gui/manager/reload` retain runtime execution, manager publication, texture upload, and remaining runtime adaptation.
- Full Molang extraction has landed as a Gradle subproject: `eyelib-molang` now owns Molang value/runtime wrappers, scope/owner-set/compiler/cache/generated parser/mapping-api/type code, plus built-in mapping entries (`MolangMath`, `MolangToplevel`); root keeps only `mc/impl/molang/**` platform/query/lifecycle bindings and the legacy `molang/` marker path.

## Current To Target Ownership Map
| Current area | Target owner | Boundary intent |
|---|---|---|
| `mc/impl/bootstrap/EyelibMod.java` | `bootstrap` | Own Forge `@Mod` startup composition in allowed `mc/impl` quarantine, while top-level `Eyelib` stays constant-only |
| `client/loader/` | `client.asset` | Parse and reload resources, avoid owning runtime publication, and keep parser seams free of platform identifier types |
| `mc/impl/client/loader/` | platform integration zone | Own Forge reload-listener lifecycle registration for client loader listeners |
| `mc/impl/common/command/` | platform integration zone | Own Forge command registration and Brigadier runtime wiring |
| `:eyelib-importer/**` | `client.model.importer.core` | Own model definitions plus source-format parsing/normalization/import support, addon/pack discovery, and raw resource-side aggregation without runtime manager/event/upload ownership |
| importer-owned client-entity / animation-controller schema under `:eyelib-importer/**` | `client.importer.schema` | Own codec trees, parsed definitions, importer-only normalization, and platform-free image/data representations for import flows; must not own runtime execution, manager publication, texture upload, or Forge/Minecraft lifecycle wiring |
| `client/model/importer/` | `client.model.importer` | Adapt `eyelib-importer` outputs into root runtime flows without leaking source-format behavior into tooling, managers, or render execution |
| `client/animation/` | `client.animation.runtime` | Keep runtime animation/controller execution, playback state, runtime lookups, and schema-to-runtime adapter/build logic in root even as parsed schema moves into `eyelib-importer` |
| `client/entity/` | `client.entity.runtime` | Keep runtime-facing lookup/helpers and RenderData integration in root while parsed client-entity/attachable schema migrates into `eyelib-importer` |
| `client/manager/` | `client.registry` | Keep runtime lookup and event-backed storage centralized, including client entities |
| `client/gui/manager/` | `client.tools` | Development/debug UI only; move import/watch/IO into helpers/services |
| `client/registry/` | `client.registry` | Hold domain-specific loader/tooling-to-manager publication seams |
| `client/* lookup seams` | domain-local read ports | Narrow runtime queries instead of bootstrap reach-through |
| `mc/impl/**` | platform integration zone | Sole long-term home for direct Minecraft/Forge imports and lifecycle wiring |
| `mc/impl/mixin/` + `eyelib.mixins.json` | platform integration zone | Own Minecraft mixin classes and mixin package/config wiring |
| `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` | `molang.generated` | Treat as generated and isolate from normal handwritten work |
| `eyelib-molang/**` | `molang.engine` | Own Molang value/runtime, compile/type/scope/mapping-api, and built-in mappings without depending on root runtime packages |
| `network/` | `sync` | Own packet registration and side-aware routing |
| `network/dataattach/` | `sync` + `dataattach` seam | Centralize attachment sync send/apply flow |
| `capability/` + `util/data_attach/` | `dataattach` | Own attachment state, ids, and mutation rules without direct MC/Forge types |
| `mc/impl/data_attach/` | platform integration zone | Own Forge capability/provider/event and NBT serialization wiring for attachment containers |
| `mc/impl/capability/` | platform integration zone | Own Forge client event-hook wiring for capability runtime-component invalidation |
| selected `util/` classes | `shared` | Keep only truly cross-cutting helpers here |

## Boundary Notes For Client Entity And Render Controller Runtime
- `BrClientEntity` is a parsed client-entity definition, not a runtime-state owner.
- Per-entity client-entity runtime belongs on `RenderData`-owned capability components, with `ClientEntityComponent` as the owner for the currently applied client-entity reference and its derived runtime caches.
- `RenderControllerEntry` stays definition-only. Render-controller runtime may cache controller-local derived state, but it must not own or bootstrap client-entity runtime.
- `EntityRenderSystem` is the composition seam that applies client-entity context to render-controller definitions and produces render/model components.

## Boundary Notes For Importer Expansion
- `:eyelib-importer` now uses its own importer namespace (`io.github.tt432.eyelibimporter.*`) so it can be consumed as an independent mod/artifact without split packages against root runtime packages.
- Client-entity/attachable schema ownership has already moved to `:eyelib-importer`, and controller reload/planner paths now parse importer-owned controller schema before adapting to root runtime controllers.
- Bedrock addon container handling (`manifest.json`, folder/archive pack discovery, texture decode, and importer-owned aggregate loading) belongs in `:eyelib-importer`; root should consume importer results instead of re-implementing pack traversal.
- Runtime executors such as `Animation<?>`, playback state owners, particle spawning, and entity/model runtime integration remain root-owned even when their source JSON codecs/definitions move out.
- Importer-owned image handling is now platform-free via `ImportedImageData`. `NativeImage` creation, upload, and other Minecraft/Blaze3D runtime wiring stay root-owned.
- Root loaders and manager tooling may call importer parsers, but they should not duplicate importer normalization logic once migration completes.
- The current animation save/sync boundary is intentionally binding-only: `AnimationComponent.SerializableInfo` carries selected animation names and Molang inputs, while per-instance playback/controller/effect state stays runtime-local and should be characterized before any refactor widens or relocates it.
- Runtime state extraction stays root-owned even after internal splits: owner/executor seams such as `BrClipStateOwner`/`BrClipExecutor` and `BrControllerStateOwner`/`BrControllerExecutor` are runtime implementation details, not importer concerns and not sync payload types.

## Public vs Internal Bias
- Default assumption: most packages are internal unless intentionally surfaced through a stable facade.
- `EyelibMod` should remain the only Forge `@Mod` composition root, while top-level `Eyelib` stays constant-only and not a runtime gateway.
- Generated zones, tooling zones, and runtime implementation details should not be treated as public API.

## Current bootstrap surface inventory
| Symbol | Current role | Current classification |
|---|---|---|
| `MOD_ID` | mod identifier constant | public bootstrap constant |
| constructor `EyelibMod()` | startup wiring for capability registration and network registration | `mc/impl` bootstrap-only |

## Current Breaking-Refactor State
- Direct manager reach-through accessors have been removed from top-level bootstrap access points.
- Runtime reads now go through lookup seams such as `AnimationLookup`, `ModelLookup`, and `ParticleLookup`.
- Loader/tooling publication now routes through domain-specific registry owners instead of `ClientAssetRegistry`.

## Breaking Refactor Rule
- Do not add new singleton reach-through methods to top-level `Eyelib`.
- Migrate remaining callers to domain-local read seams, then delete the legacy reach-through methods instead of preserving them for compatibility.
