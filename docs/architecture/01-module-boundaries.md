# Eyelib Module Boundaries

## Current Major Areas
- `src/main/java/io/github/tt432/eyelib/client/`: client rendering, animation, particles, GUI, loaders, managers, tooling.
- `src/main/java/io/github/tt432/eyelib/molang/`: Molang runtime/compiler plus generated grammar artifacts.
- `src/main/java/io/github/tt432/eyelib/network/`: packet registration and client/server packet handling.
- `src/main/java/io/github/tt432/eyelib/capability/`: attachment-related capability registration and data holders.
- `src/main/java/io/github/tt432/eyelib/common/`: shared behavior logic.
- `src/main/java/io/github/tt432/eyelib/util/`: shared helpers plus mixed client/data-attachment utilities.

## Existing Patterns To Preserve
- Manager pattern around `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`.
- Loader pattern in `src/main/java/io/github/tt432/eyelib/client/loader/`.
- Visitor pattern in `src/main/java/io/github/tt432/eyelib/client/render/visitor/`.
- Codec-heavy serialization approach across model, animation, particle, and Molang-related types.

## Current Boundary Problems
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java` is still a hotspot, but file dialogs, folder watching, import orchestration, and hotkey wiring now have dedicated helper seams.
- `src/main/java/io/github/tt432/eyelib/molang/grammer/` is a legacy name that historically made generated parser artifacts look like normal source files.
- `src/main/java/io/github/tt432/eyelib/client/loader/` now parses non-model assets into local maps first, and client-entity publication now also lands in manager/store semantics through `client/registry/`.
- `src/main/java/io/github/tt432/eyelib/util/client/` is no longer the only home for deterministic helpers; named destinations now exist for texture path, pose copy, and inventory model resource helpers.
- `src/main/java/io/github/tt432/eyelib/network/` and `src/main/java/io/github/tt432/eyelib/util/data_attach/` now share a narrower sync seam via `network/dataattach/DataAttachmentSyncService.java`, but the packet surface is still broad.
- Core runtime reads are starting to move away from `Eyelib.java` reach-through and into domain lookup seams such as `client/animation/AnimationLookup.java`, `client/model/ModelLookup.java`, `client/entity/ClientEntityLookup.java`, and `client/render/controller/RenderControllerLookup.java`.

## Current To Target Ownership Map
| Current area | Target owner | Boundary intent |
|---|---|---|
| `Eyelib.java` | `bootstrap` + temporary compatibility facade | Keep startup/composition here, reduce the remaining direct singleton exposure over time |
| `client/loader/` | `client.asset` | Parse and reload resources, but avoid owning runtime publication |
| `client/manager/` | `client.registry` | Keep runtime lookup and event-backed storage centralized, including client entities |
| `client/gui/manager/` | `client.tools` | Development/debug UI only; move import/watch/IO into helpers/services |
| `client/registry/` | `client.registry` | Centralize loader-to-manager publication seams |
| `client/* lookup seams` | domain-local read ports | Narrow runtime queries instead of bootstrap reach-through |
| `molang/generated/` | `molang.generated` | Treat as generated and isolate from normal handwritten work |
| `network/` | `sync` | Own packet registration and side-aware routing |
| `network/dataattach/` | `sync` + `dataattach` seam | Centralize attachment sync send/apply flow |
| `capability/` + `util/data_attach/` | `dataattach` | Own attachment state and mutation rules |
| selected `util/` classes | `shared` | Keep only truly cross-cutting helpers here |

## Public vs Internal Bias
- Default assumption: most packages are internal unless intentionally surfaced through a stable facade.
- `Eyelib.java` is currently the main compatibility entrypoint and should shrink, not grow.
- Generated zones, tooling zones, and runtime implementation details should not be treated as public API.

## Current `Eyelib.java` Surface Inventory
| Symbol | Current role | Current classification |
|---|---|---|
| `MOD_ID` | mod identifier constant | public bootstrap constant |
| constructor `Eyelib()` | startup wiring for capability registration and network registration | bootstrap-only |
| `getRenderHelper()` | entrypoint into current render helper runtime | transitional bridge |
| `getAnimationManager()` | direct singleton access to animation storage/runtime | internal leak to be reduced later |
| `getMaterialManager()` | direct singleton access to material storage/runtime | internal leak to be reduced later |
| `getModelManager()` | direct singleton access to model storage/runtime | internal leak to be reduced later |
| `getRenderControllerManager()` | direct singleton access to render-controller storage/runtime | internal leak to be reduced later |
| `getParticleManager()` | direct singleton access to particle storage/runtime | internal leak to be reduced later |

## Stage 3 Rule
- Do not add new singleton reach-through methods to `Eyelib.java`.
- Prefer documenting or introducing narrower facades before exposing more runtime internals.
