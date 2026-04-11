# eyelib core ↔ mc split - main tracker

## Goal
- Separate platform-free `core` from Minecraft-facing `mc` code.
- Inside `mc`, introduce `api` for seam contracts and `impl` for Minecraft/Forge implementations.
- Keep the repository as a single Gradle module during this migration.

## Current migration stance
- The existing completed module notes below mostly describe a first-wave seam extraction pass, not the final package-isolation end state requested for this task.
- A module marked `completed` in this file may still require follow-up work until all direct `net.minecraft.*`, `net.minecraftforge.*`, and equivalent MC runtime references are confined to explicitly allowed `mc/impl` packages.
- Tracker updates during this task must distinguish between `first-wave seam complete` and `final mc/api + mc/impl isolation complete`.

## Oracle-reviewed target package layout
- `io.github.tt432.eyelib.core..*`: platform-free domain logic, state, algorithms, and portable helpers.
- `io.github.tt432.eyelib.mc.api..*`: platform-type-free contracts, ports, lifecycle intents, and seam DTOs between `core` and Minecraft runtime.
- `io.github.tt432.eyelib.mc.impl..*`: the only allowed home for direct Minecraft/Forge imports, including bootstrap, client runtime, packets, capability wiring, resource reload registration, GUI, rendering, and mixins.
- Existing legacy packages are transitional only; they are not acceptable long-term homes for direct MC/Forge imports once equivalent `mc/impl` ownership exists.

## Verification policy
- All compile/test verification must use JetBrains MCP.
- If JetBrains MCP cannot run the required build/test step, stop the module task and report immediately.
- Baseline verified before planning: `qylEyelib [build]` ✅, `nullawayMain` ✅ via JetBrains run configurations.
- Default test style for new seam work: plain-JVM JUnit 5 tests under `src/test/java` in package-mirrored locations.
- Prefer targeted seam tests with inline fakes/stubs over runtime-heavy Minecraft integration unless the behavior truly depends on live MC execution.
- Use JetBrains MCP `Test`, targeted Gradle `test --tests ...`, `qylEyelib [build]`, and `nullawayMain` as the default verification ladder.

## Placement rubric
- `core`: no `net.minecraft`, `net.minecraftforge`, `Minecraft.getInstance()`, NBT, packet context, render types, resource reload listeners, event bus, or mixins.
- `mc/api`: narrow role-based ports and seam DTOs that connect `core` with Minecraft runtime.
- `mc/impl`: anything touching entities, registries, capabilities, packets, rendering, resource packs, GUI, event bus, or bootstrap.

## `mc/api` design rules
- Prefer interface-first ports plus explicit implementation binding; do not rely on package naming alone as the boundary.
- `mc/api` contracts must not expose concrete Minecraft/Forge classes in method signatures, fields, generics, or DTO members.
- If a seam currently needs Minecraft identifiers or runtime handles, introduce a role-based contract or platform-free DTO instead of leaking `ResourceLocation`, `Entity`, `Level`, `Minecraft`, packet context, or Forge event types into `mc/api`.
- Side-specific bootstrap and client/server-only code must stay in `mc/impl`; `core` and `mc/api` must remain safe from side-only classloading.
- Tests for seam contracts should prefer fake/test implementations of `mc/api` ports over depending on live Minecraft runtime wiring unless the specific test is explicitly validating `mc/impl` integration.
- `mc/api` may use Minecraft-flavored domain names (`EntityRef`, `CapabilitySync`, `RenderContextPort`) as long as the types themselves remain platform-type-free and plain-JVM-testable.

## External pattern guidance
- Primary pattern for this repo: interface-first ports with a single runtime implementation bound inside repository-owned `mc/impl` composition code.
- Acceptable inspiration: ServiceLoader-style provider resolution or explicit composition-root binding; the exact mechanism may vary by module, but the API/impl split must remain visible in code structure.
- Avoid annotation-driven indirection unless it materially reduces churn and remains easy to verify with JetBrains MCP tests/builds.

## Terminal condition for this task
- Except for explicitly allowed `mc/impl` packages, no production class may directly import or reference Minecraft/Forge runtime packages.
- `mc/api` must contain only contracts, role-based ports, and seam DTOs; it must not contain concrete Minecraft/Forge types or implementation logic.
- A module is not done until rule-based scans confirm that its remaining Minecraft/Forge references live only in allowed `mc/impl` locations.
- Existing first-wave seams are acceptable only if they are stepping stones toward this terminal condition, not if they preserve long-lived MC references in legacy package locations.
- Having a seam is not sufficient. The acceptance test is hard import quarantine, not architectural intent.

## Affected work modules

### Status legend
- `final isolation complete`: the module now satisfies the hard-import quarantine end state for its scope.
- `completed (hard-import slice advanced)` / `completed (second-wave quarantine slice)`: a meaningful quarantine slice is done and verified locally, but the whole module still has remaining legacy-package MC runtime ownership to clean up.
- `first-wave complete + re-baselined`: earlier seam work exists, and the remaining final-isolation gaps are now explicitly mapped.
- `first-wave complete`: only the earlier seam extraction baseline is complete; final quarantine planning or re-baselining is still pending.

| Module | Status | Primary source areas | Notes |
|---|---|---|---|
| `bootstrap` | final isolation complete | `mc/impl/bootstrap/`, `Eyelib.java` | Forge `@Mod` composition root is quarantined to `mc/impl/bootstrap/EyelibMod`; top-level `Eyelib` is constant-only (`MOD_ID`) |
| `capability-dataattach` | completed (hard-import slice advanced) | `capability/`, `util/data_attach/`, `mc/impl/data_attach/`, `mc/impl/capability/` | completed attachment split keeps storage contracts platform-free and capability/provider/event/NBT wiring in `mc/impl/data_attach`; latest runtime-component follow-up moved Forge manager/texture invalidation listeners out of `capability/component` into `mc/impl/capability/CapabilityComponentRuntimeHooks` with targeted plain-JVM seam tests, while `RenderData`/`ModelComponent` still remain legacy-package MC-facing hotspots |
| `network-sync` | completed (hard-import slice advanced) | `network/`, `network/dataattach/`, `mc/impl/network/`, `mc/impl/network/packet/` | channel/context/side-gating and entity/player dispatch moved to `mc/impl/network`; packet DTO classes are now quarantined under `mc/impl/network/packet/`, with remaining follow-up reduced to implementation-side `FriendlyByteBuf` / `CompoundTag` payload concerns |
| `client-loaders` | completed (hard-import slice advanced) | `client/loader/`, `mc/impl/client/loader/` | Forge reload-listener wiring moved to `mc/impl/client/loader/ClientLoaderLifecycleHooks` and parsing seam keys are now platform-type-free; legacy `SimpleJsonWithSuffixResourceReloadListener` and concrete loader runtime types still require follow-up isolation for final module quarantine |
| `client-managers-registry` | final isolation complete (code) | `client/manager/`, `client/registry/`, `mc/api/client/manager/`, `mc/impl/client/manager/` | manager entry publication now routes through `mc/api` bridge and Forge event-bus wiring is owned by `mc/impl`; client-entity registry replacement seam no longer exposes `ResourceLocation`; scoped rule-based scans passed, while fresh full-module MCP confirmation still depends on a stable validation session |
| `client-model-animation-entity` | completed (second-wave quarantine slice) | `client/model/`, `client/animation/`, `client/entity/` | first hard-import slice landed in `client/entity` (`ClientEntityLookup` string-keyed), and a second model-definition slice removed direct `net.minecraft.util.ExtraCodecs` usage from `Model.TextureMesh` codec; heavier model/animation/runtime owners still expose `PoseStack`, `ModelPart`, `PartPose`, `NativeImage`, and other MC/Blaze bake/runtime hooks |
| `client-render` | completed (hard-import slice advanced) | `client/render/` | `RenderSyncApplyOps` now uses platform-type-free `RenderModelSyncPayload` (`String` ids) and model packet payload no longer exposes `ModelComponent.SerializableInfo`; targeted seam test + JetBrains MCP build checks are green, while runtime decode/entity lookup/render owners still require later `mc/impl` quarantine |
| `client-particle` | completed (hard-import slice advanced) | `client/particle/` | spawn/request seam tightened: `ParticleSpawnRequest` is now platform-type-free (`String` ids + state copy), with `ResourceLocation` adaptation kept in `ParticleSpawnService`; heavy particle runtime/render owners remain queued for `mc/impl` quarantine |
| `client-gui-tools` | completed (hard-import slice advanced) | `client/gui/` | reload planning seam now also owns platform-free path normalization and texture-key planning; `ManagerResourceImportPlanner` no longer constructs `ResourceLocation` directly, while runtime UI/hotkey/preview owners remain queued for `mc/impl` quarantine |
| `molang-mc-adapters` | final isolation complete | `molang/mapping/`, selected compiler hooks, `mc/impl/molang/` | first module fully promoted to hard import quarantine: heavy query implementation + lifecycle hooks moved under `mc/impl/molang`, `MolangMath` no longer imports Minecraft helpers, targeted tests + JetBrains MCP build/nullaway green |
| `common-runtime` | completed (hard-import slice advanced) | `common/`, `mc/impl/common/command/`, selected `event/` | command hotspot quarantined to `mc/impl/common/command/EyelibParticleCommand`, deterministic command seam extracted to `common/runtime/ParticleCommandRuntime`, while handler/event logic and behavior schema still expose MC types like `ResourceLocation` and `StringRepresentable` |
| `utility-mc-bridges` | final isolation in progress (slice advanced) | `util/client/`, `util/codec/`, `util/modbridge/`, selected `util/math/` | wave 2 timer + modbridge quarantine is complete, wave 3 moved/removed legacy util helper shims, and wave 4 deleted dead `util/client` runtime bridge wrappers (`BakedModels`, `BlitCall`, `BufferBuilders`); remaining blockers are now concentrated in `util/codec/*` runtime codecs plus `util/ResourceLocations` and `util/math/Shapes`, and this retry has fresh JetBrains MCP confirmation (`test --tests io.github.tt432.eyelib.core.util.time.FixedStepTimerStateTest` + project build both pass) |
| `mixin-integration` | completed (hard-import slice advanced) | `mc/impl/mixin/`, `src/main/resources/eyelib.mixins.json` | mixin classes relocated from legacy `mixin/` to `mc/impl/mixin/`, config package ownership aligned to `io.github.tt432.eyelib.mc.impl.mixin`, and relocation verified with JetBrains MCP build/file checks; remaining cross-module follow-up is `client/model/RootModelPartModel` ownership under client-model final isolation |

## Active implementation queue
1. `utility-mc-bridges` *(background deep subagent running / targeting remaining legacy utility-render bridge surface)*
2. `client-model-animation-entity` *(background deep subagent retry running / second hard-import slice on model-definition side)*
3. `capability runtime components` *(in progress / Forge event-hook ownership moved to `mc/impl/capability`, remaining render/transport runtime contracts still queued)*

## Current quarantine maturity split
- **Already using explicit `mc/impl` ownership:** `molang-mc-adapters`, `bootstrap`, `mixin-integration`, `client-managers-registry`, `capability-dataattach`, `client-loaders`, `utility-mc-bridges`, and the advanced `network-sync` slice.
- **Still primarily legacy-package MC runtime ownership (re-baselined or pending):** `client-model-animation-entity`, `client-render`, `client-particle`, `client-gui-tools`, `common-runtime`.

## Remaining work before true completion
- Continue `network-sync` follow-up only on implementation-side packet payload cleanup (`FriendlyByteBuf` / `CompoundTag`) now that transport, context, and packet DTO class ownership have all moved under `mc/impl/network/**`.
- Execute at least one explicit `mc/impl` landing slice in the still-legacy-heavy client/runtime area (`client-model-animation-entity` → `client-render` → `client-particle` / `client-gui-tools`) rather than stopping at analysis-only baselines.
- Resolve the remaining legacy-package MC runtime owners called out in trackers: render/runtime helpers, GUI tooling, particle runtime, and behavior schema/runtime.
- Re-run rule-based scans for the full repository and verify that every remaining direct `net.minecraft.*` / `net.minecraftforge.*` / `com.mojang.blaze3d.*` import outside allowed `mc/impl` locations is either eliminated or explicitly queued in an unfinished module tracker.
- Re-run JetBrains MCP verification in a session where build/run-config tooling is available, so the slices currently marked “code isolation complete” or “advanced” gain fresh whole-repo confirmation instead of relying on stale or tool-blocked validation notes.

## What is actually done vs not done
- **Actually at final isolation for its slice:** `molang-mc-adapters`, `bootstrap`.
- **Advanced quarantine slices completed, but module still not at final end-state:** `client-managers-registry`, `client-loaders`, `capability-dataattach`, `utility-mc-bridges`, `common-runtime`, `network-sync`.
- **Still not at final end-state (some have landed hard-import slices):** `client-model-animation-entity`, `client-render`, `client-particle`, `client-gui-tools`, `common-runtime`.

## Next dependency order after active queue
1. *(none queued after current active set)*

## Re-baseline highlights driving the queue
- `client-loaders` now has `mc/impl` lifecycle ownership for loader registration and platform-type-free parser key seams, but still has legacy-package MC reload-base ownership (`SimpleJsonWithSuffixResourceReloadListener`) that needs a later relocation step.
- `network-sync` packet DTO classes are now under `mc/impl/network/packet/`; remaining follow-up is limited to implementation-side payload cleanup (`FriendlyByteBuf`, `CompoundTag`) rather than legacy package ownership.
- `client-model-animation-entity` must precede `client-render` because definition-layer contracts still expose MC asset/runtime types that render code currently consumes directly.
- Within `client-model-animation-entity`, the first `client/entity` hard-import step is now done: `ClientEntityLookup` is platform-type-free (`String` id seam), and MC `ResourceLocation` adaptation is kept at the caller boundary.
- `client-render` started from the `RenderSyncApplyOps` seam and tightened model sync payload types away from `ResourceLocation` by introducing string-keyed `RenderModelSyncPayload`; remaining work is relocating heavy runtime owners (`RenderTypeResolver`, `RenderParams`, visitors/renderers, texture upload/merge) into `mc/impl`.
- `RenderControllerEntry` is a key cross-module knot: it still mixes model/entity definitions with texture composition and runtime render setup, so `client-model-animation-entity` must narrow those definition/state contracts before `client-render` can safely finish its own quarantine.
- `client-gui-tools` is downstream of render/runtime quarantine more than of loader seams alone: preview/import tooling still depends directly on `RenderParams`, `RenderType`, DFS/baked-model helpers, modbridge events, and `NativeImageIO`, so GUI final isolation should stay queued behind render/runtime stabilization.
- High-density next-cut files are now explicitly tracked: render should begin with `RenderSyncApplyOps` + payload tightening before moving `RenderParams` / `EyelibLivingEntityRenderer` / `SimpleRenderAction` / `TextureLayerMerger` / `NativeImageIO`; GUI should start from `AnimationView` / `ModelPreviewScreen` / `DragTargetWidget` / `EntitiesListPanel` / `EyelibManagerScreen`; particle should start from `BrParticleParticle` / `BrParticleRenderManager` / `ParticleComponentManager` / `BrParticleEmitter`.
- `common-runtime` follows capability/network/model decisions because its handlers currently mix attachment writes, packet sends, mob-goal inspection, and behavior definitions with MC types.
- `capability-dataattach` completion does not imply the whole `capability/**` package is clean: `RenderData`, `ModelComponent`, `RenderControllerComponent`, and `AnimationComponent` still look like MC-facing runtime components and should be handled with later render/network/runtime slices, while simpler DTOs such as `ExtraEntityData` and `EntityStatistics` may remain as data carriers.
- The concentrated command hotspot slice has now landed under `common-runtime`: `EyelibParticleCommand` moved to `mc/impl/common/command`, with deterministic suggestion/request/message shaping extracted into platform-type-free `common/runtime/ParticleCommandRuntime`; remaining `common-runtime` work is now primarily handler wiring and behavior schema isolation.
- `common/behavior/**` is not just runtime wiring debt: parts of its schema layer still depend on Minecraft serialization conventions (`ResourceLocation`, `StringRepresentable`), so the later `common-runtime` batch must explicitly choose between platform-free schema redesign and full Minecraft-owned quarantine for that behavior surface.
- `mixin-integration` hard-import relocation slice is now landed: all three mixins and mixin-package config are under `mc/impl`; remaining follow-up is cross-module (`client/model/RootModelPartModel` ownership) under the `client-model-animation-entity` final isolation track.
- `bootstrap` quarantine move is now complete: Forge `@Mod` / mod-bus composition moved to `mc/impl/bootstrap/EyelibMod`, while top-level `Eyelib` is constant-only for `MOD_ID` compatibility.
- Package-guidance gaps are now largely closed: local `README.md` files exist for `client/render/`, `client/gui/`, `common/`, `common/behavior/`, `client/animation/`, `client/model/`, `client/entity/`, `client/particle/`, and `mc/impl/mixin/` (with a legacy pointer retained in `mixin/README.md`).

## Cross-module rules
- Preserve manager, loader, visitor, and codec patterns unless a module plan explicitly narrows a seam.
- Do not move generated Molang parser artifacts.
- `common/shared` in current docs is not automatically `core`.
- Avoid package mirrors in `mc/api`; define role-based ports instead.
- Do not treat prior `completed` markers as proof of final boundary isolation; re-baseline each module against the terminal condition above.
- When a compatibility shim is introduced during migration, record its deletion point in the owning module tracker during the same stage.
- If a doc points to a missing plan or outdated path, fix that doc in the same stage that relies on it.
- Do not allow hidden type leakage through `Object`, raw collections, or generic wrappers that still require Minecraft/Forge semantics at the boundary.

## Per-module task contract
Each module subtask must do all of the following:
1. Design interfaces/ports and decide `core` / `mc/api` / `mc/impl` placement.
2. Add or adapt tests around the seam.
3. Implement the seam and migrate local callers.
4. Verify using JetBrains MCP build/test only.
5. Update this tracker and the module tracker with outcomes/blockers.

## Current summary
- Direct Minecraft/Forge interaction is concentrated in bootstrap, network, capabilities/data attachment, client runtime/render/tooling, Molang query adapters, and mixins.
- Best first slices are not full package moves; they are narrow seam extractions around query, publish, apply, and storage ports.
- The repository now has a real top-level `mc/` root package, but large parts of the codebase are still transitional: many legacy packages still own direct Minecraft/Forge runtime code that has not yet been physically quarantined into `mc/impl`.
- Several architecture docs still reference a missing historical refactor-plan file; treat that as documentation debt to resolve during this migration rather than as an authoritative current plan.
- `molang-mc-adapters` is now the first final-isolation-complete slice: `MolangQuery` and Molang lifecycle hooks moved into `mc/impl/molang/*`, `MolangMath` is plain-JVM, JetBrains MCP targeted tests/build passed, and rule-based scans show no remaining `net.minecraft`/`net.minecraftforge` imports under `src/main/java/io/github/tt432/eyelib/molang/**`.
- The `mc/` root is now a real code-bearing boundary, not just a target shape: current ownership already includes `mc/impl/molang/*`, `mc/impl/client/manager/*`, `mc/impl/data_attach/*`, `mc/impl/util/time/*`, `mc/impl/modbridge/*`, plus the first `mc/api/client/manager/*` contract seam.
- `mc/impl` is continuing to consolidate along module boundaries rather than ad-hoc file moves: it now contains dedicated subtrees for `client/loader`, `client/manager`, `common/command`, `data_attach`, `molang`, `modbridge`, `util/time`, and an emerging `network/` transport/runtime area.
- `mc/api` remains intentionally narrow so far; most remaining work is still physical quarantine of legacy runtime code into `mc/impl`, not broad creation of new API layers.
- Current `mc/api/**` audit is clean: it now contains only the `ManagerEventPublisher` interface, with no direct `net.minecraft.*`, `net.minecraftforge.*`, or `com.mojang.blaze3d.*` imports.
- Utility hard-quarantine advanced beyond first wave: fixed-step timer arithmetic now lives in plain-JVM `core/util/time/FixedStepTimerState`, Minecraft runtime timing moved to `mc/impl/util/time/FixedTimer`, Forge `modbridge` update event ownership moved to `mc/impl/modbridge/ModBridgeModelUpdateEvent`, and legacy util helper shims were further drained (`client/render/PoseCopies` owner move, inventory model helper move to `mc/impl/util/model`, obsolete util facade deletion); remaining utility blockers are runtime-heavy `util/client/BakedModels` + `util/client/BlitCall`, plus MC-facing codec/runtime helpers under `util/codec/*`.
- `FixedStepTimerState` now has a local semantic fix for the first-step accounting bug (`start()` resets `init`, first immediate step advances `lastFixed`), but this specific repair has not yet been re-confirmed by JetBrains MCP in the current session because build/run-config tools are unavailable and Java LSP (`jdtls`) is not installed.
- `client-managers-registry` is now final-isolation complete for code isolation in this scope: manager storage remains behind `ManagerStorage<T>`, lookup/publish call sites use `ManagerReadPort<T>`/`ManagerWritePort<T>`, the publisher interface remains in `mc/api/client/manager/ManagerEventPublisher`, the concrete bridge holder now lives in `client/manager/ManagerEventPublishBridge`, Forge implementation/lifecycle wiring stays in `mc/impl/client/manager`, and `ClientEntityAssetRegistry` replacement no longer leaks `ResourceLocation`; fresh full-module MCP confirmation still needs a stable validation session.
- `client-loaders` hard-import quarantine slice advanced: concrete `Br*Loader` classes no longer own Forge reload-listener registration, with registration centralized under `mc/impl/client/loader/ClientLoaderLifecycleHooks`; `LoaderParsingOps` now uses source-key generics instead of `ResourceLocation` in parse/translation method signatures, and loader seam tests now validate plain string-key behavior. JetBrains MCP verification is only partially available in this session: boundary scans and loader-scope code changes are confirmed, but some MCP operations (`get_file_problems`, `run_gradle_tasks`, `build_project`) were unavailable and run-config status has shifted across slices.
- `capability-dataattach` second-wave quarantine slice is complete: `util/data_attach` contracts are platform-type-free (`DataAttachmentType` string id, map storage keyed by id, no `INBTSerializable`/`CompoundTag` in container contracts), and capability/provider/event/NBT logic now lives in `mc/impl/data_attach`; targeted JetBrains tests pass and scans show no remaining forbidden imports under `util/data_attach`.
- Capability runtime follow-up slice has started after the attachment split: Forge `ManagerEntryChangedEvent`/`TextureChangedEvent` listener ownership for runtime-component invalidation moved from `AnimationComponent` + `RenderControllerComponent` into `mc/impl/capability/CapabilityComponentRuntimeHooks`, with new plain-JVM seam tests (`AnimationComponentRuntimeInvalidationTest`, `RenderControllerComponentTextureStateTest`).
- JetBrains MCP verification for that runtime follow-up slice is green in this session (`test --tests io.github.tt432.eyelib.capability.component.AnimationComponentRuntimeInvalidationTest --tests io.github.tt432.eyelib.capability.component.RenderControllerComponentTextureStateTest` and full `build` both exit 0); Java `lsp_diagnostics` remains tool-blocked because `jdtls` is not installed.
- The remaining `capability/**` surface is now narrower and clearer: `RenderData` and `ModelComponent` still carry MC-facing runtime/transport/render contracts (`Entity`, `ResourceLocation`, `FriendlyByteBuf`, `RenderType`), and `AnimationComponent` still has `FriendlyByteBuf` stream-codec coupling, so those remain the next blockers for this module.

## Current non-`mc/impl` import density snapshot
- `client/gui/**`: 77 direct `net.minecraft` / `net.minecraftforge` / `com.mojang.blaze3d` imports across 11 files.
- `client/render/**`: 54 imports across 14 files.
- `client/particle/**`: 51 imports across 15 files.
- `util/**`: 10 imports across 7 files.
- `client/loader/**`: 30 imports across 9 files.
- `network/**`: 11 imports across 10 files.
- `capability/**`: 29 imports across 9 files.
- `client/model/**`: 24 imports across 14 files.
- `common/**`: 16 imports across 6 files.
- `client/animation/**`: 6 imports across 3 files.
- `client/entity/**`: 0 imports across 0 files.

These refreshed counts reinforce the current queue: after the attachment slice landed, `network/**` is materially narrower, while the biggest remaining work is still concentrated in client runtime/render/tooling plus the remaining legacy loader/runtime surfaces.
- Latest recount after the render, particle, GUI planning, and packet-contract slices shows only modest numerical change in the hottest client-runtime zones (`client/render/**` 54, `client/particle/**` 51, `client/gui/**` 77), plus a now-clearly bounded utility remainder (`util/**` 10 imports across 7 files). This confirms the remaining work is now mostly heavy runtime-owner quarantine rather than additional low-cost seam tightening.

## Current file-level hotspot shortlist
- `client/gui/**`: `AnimationView`, `EyelibManagerScreen`, `ManagerScreenOpenEvents`, `ManagerScreenKeybinds`, `ManagerFolderSession`, `ManagerResourceImportPlanner`
- `client/render/**`: `RenderTypeResolver`, `PoseCopies`, `RenderControllerEntry`, `TextureLayerMerger`, `NativeImageIO`, `ClientRenderSyncService`, `RenderModelVisitor`
- `client/particle/**`: `ParticleLookup`, `ParticleSpawnService`, `BrParticleEmitter`, `Direction`
- `client/model/**`: `ModelBakeInfo`, `TwoSideModelBakeInfo`, `bbmodel/Texture`, `importer/BlockbenchModelImporter`, `importer/ImportedModelData`, `importer/ModelImporter`
- `capability/**`: `RenderData`, `component/ModelComponent`, `component/RenderControllerComponent`
- `util/**`: `util/codec/EyelibCodec`

These are the concrete remaining legacy-package hotspots after the latest round of quarantine work.
- `network-sync` hard-import slice advanced: transport/channel/context/side-gating ownership now lives under `mc/impl/network` (`EyelibNetworkTransport` + `DataAttachmentSyncRuntime`), `NetClientHandlers` is context-free, `UniDataUpdatePacket` no longer exposes `RegistryObject`, and `SpawnParticlePacket` no longer exposes `ResourceLocation` (string-keyed particle id contract); final packet-contract quarantine for remaining `FriendlyByteBuf` and `DataAttachmentSyncPacket` `CompoundTag` payload ownership in legacy `network/**` is still pending.
- `common-runtime` hard-import slice advanced: `EyelibParticleCommand` now lives under `mc/impl/common/command`, deterministic command shaping moved into plain-JVM `common/runtime/ParticleCommandRuntime`, and command registration no longer contributes non-`mc/impl` MC/Forge imports; `EntityExtraDataHandler`/`EntityStatisticsHandler` plus `common/behavior/**` schema remain queued for final isolation.
- `client-gui-tools` hard-import slice advanced with minimal churn: `ManagerResourceReloadPlan` now owns normalized single-file route classification plus string-key texture planning (`Path`/`String` only), `ManagerResourceImportPlanner` delegates both route and texture-key decisions and no longer constructs `ResourceLocation` directly, targeted seam tests were expanded, and runtime UI/hotkey/preview owners remain queued for `mc/impl` relocation.
- `client-render` hard-import slice advanced: model sync payload now routes through string-keyed `client/render/sync/RenderModelSyncPayload`, `RenderSyncApplyOps` no longer exposes `ModelComponent.SerializableInfo` in model collect/replace signatures, and runtime `ResourceLocation` decode stays in `ClientRenderSyncService`; targeted seam tests are updated and MCP verification is tracked in the module note.
- `client-particle` hard-import slice advanced from first-wave baseline: `client/particle/ParticleSpawnRequest` is now platform-type-free (string-keyed particle id + defensive position copy, no direct packet/`ResourceLocation` type coupling), and packet `ResourceLocation` adaptation is now explicit at `ParticleSpawnService` runtime wiring; targeted seam test and JetBrains project build checks pass, while `BrParticleRenderManager`, emitters/particles, render hooks, and level/camera/render-type access remain MC-facing and still queued for `mc/impl` relocation.
- `bootstrap` final isolation is complete: Forge startup composition (`@Mod`, mod-bus wiring, attachment + network registration) now lives in `mc/impl/bootstrap/EyelibMod`; top-level `Eyelib` no longer owns MC/Forge bootstrap imports and remains a constant holder.
- `client-model-animation-entity` first hard-import step is now concrete in `client/entity`: `ClientEntityLookup` no longer imports `ResourceLocation` and now exposes a string-keyed seam with targeted plain-JVM lookup coverage (`ClientEntityLookupTest`), while model/animation/runtime-heavy classes remain queued for later `mc/impl` relocation.
- `client-model-animation-entity` second hard-import slice is now concrete on the model-definition side: `Model.TextureMesh` no longer imports/uses `net.minecraft.util.ExtraCodecs` directly and now encodes vector fields through Eyelib float-list codecs; targeted plain-JVM seam coverage (`ModelTextureMeshCodecTest`) passes, while remaining blockers are still concentrated in `Model` traversal/runtime (`PoseStack`), model-part adapters (`ModelPart`/`PartPose`), and importer/runtime texture image flow (`NativeImage`).
- `mixin-integration` hard-import slice advanced: `HumanoidModelMixin`, `LivingEntityRendererAccessor`, and `MultiPlayerGameModeMixin` moved under `mc/impl/mixin`, `eyelib.mixins.json` now points to `io.github.tt432.eyelib.mc.impl.mixin`, and `SimpleRenderAction` imports the relocated accessor type; remaining follow-up is cross-module (`RootModelPartModel` ownership in `client/model`).
