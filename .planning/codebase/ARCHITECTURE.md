<!-- refreshed: 2026-05-06 -->
# Architecture

**Analysis Date:** 2026-05-06

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                       Root Runtime Module (Forge Mod)                   │
│  `src/main/java/io/github/tt432/eyelib/`                                │
├──────────────┬──────────────┬──────────────┬──────────────┬─────────────┤
│   client/    │  network/    │  capability/ │   common/    │   event/    │
│  `client/`   │ `network/`   │ `capability/`│ `common/`   │  `event/`   │
├──────────────┴──────────────┼──────────────┼──────────────┼─────────────┤
│         mc/                 │   util/      │   core/      │                  │
│    `mc/api/` (bridges)     │  `util/`     │  `core/util/`│                  │
│    `mc/impl/` (quarantine)  │              │              │                  │
└─────────────┬───────────────┴──────────────┴──────────────┴─────────────┘
              │
        Gradle `api project(...)` dependencies
              │
    ┌─────────┼────────────────────────────────────┐
    ▼         ▼                 ▼                  ▼
┌──────────┐ ┌──────────────┐ ┌────────────────┐ ┌─────────────────┐
│ eyelib-  │ │ eyelib-      │ │ eyelib-        │ │ eyelib-         │
│ molang   │ │ importer     │ │ processor      │ │ material        │
│ engine   │ │ schema/parse │ │ platform-free  │ │ Bedrock mats    │
│ no MC    │ │ `eyelib-     │ │ processing     │ │ GL/shader pipe  │
│          │ │  importer/`  │ │ `eyelib-       │ │ `eyelib-        │
│ `eyelib- │ │              │ │  processor/`   │ │  material/`     │
│  molang/`│ │              │ │                │ │                 │
└──────────┘ └──────────────┘ └────────────────┘ └─────────────────┘
    ▲              ▲
    │              │
┌───────────────────────────────────────┐
│  eyelib-attachment                    │
│  platform-type-free data attachment   │
│  `eyelib-attachment/`                 │
└───────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Bootstrap | Forge `@Mod` startup composition, capability/network registration | `mc/impl/bootstrap/EyelibMod.java` |
| Constants | Mod ID constant holder, legacy compatibility surface | `Eyelib.java` |
| Client Render System | Entity render orchestration, Forge event subscriptions, render component composition | `client/EntityRenderSystem.java` |
| Client Tick | Client tick counter and deferred task scheduling | `client/ClientTickHandler.java`, `client/ClientTaskScheduler.java` |
| Network Manager | Packet registration, transport delegation, send-to-server bridge | `network/EyelibNetworkManager.java` |
| Network Transport | Forge `SimpleChannel` wiring, side gating, packet dispatch | `mc/impl/network/EyelibNetworkTransport.java` |
| Client Packet Handlers | Domain-service routing from network packets to application logic | `network/NetClientHandlers.java` |
| Client Render Sync | Model/animation sync packet application to render state | `client/render/sync/ClientRenderSyncService.java` |
| Particle Spawn Service | Particle emitter lifecycle — spawn requests and packet-driven spawn/remove | `client/particle/ParticleSpawnService.java` |
| RenderData | Per-entity capability that owns model components, animation state, Molang scope, client entity/runtime | `capability/RenderData.java` |
| Managers | Singleton observable stores for animations, models, materials, particles, render controllers, client entities, attachables | `client/manager/*Manager.java` |
| Loaders | JSON resource reload listeners that parse assets into domain maps, then publish via registry seams | `client/loader/Br*Loader.java` |
| Registry | Domain-specific loader/tooling-to-manager publication boundaries | `client/registry/*AssetRegistry.java` |
| Molang Engine | Value wrappers, scope/compiler/type system, generated parser, mapping API, built-in mappings | `eyelib-molang/` subproject |
| Importer | Schema definitions, CODECs, Bedrock addon/pack discovery, source format parsing (bbmodel, bedrock) | `eyelib-importer/` subproject |
| Processor | Platform-free processing/batching, reload planning helpers, loader parse/translation operators | `eyelib-processor/` subproject |
| Material | Bedrock material definitions, GL state management, shader pipeline | `eyelib-material/` subproject |
| Attachment | Platform-type-free typed attachment storage, mutation contracts, codec support | `eyelib-attachment/` subproject |

## Pattern Overview

**Overall:** Multi-project Forge mod with layered separation — platform-free subprojects feed schema and engine logic into a root runtime module that handles all Minecraft/Forge integration in a quarantined `mc/impl/` package tree.

**Key Characteristics:**
- **Manager pattern**: Abstract `Manager<T>` class (`client/manager/Manager.java`) provides observable singleton stores with read/write port interfaces. All runtime data access goes through domain-specific manager subclasses (e.g., `ModelManager`, `AnimationManager`).
- **Loader pattern**: `BrResourcesLoader` base class with JSON-suffix-aware reload listeners. Concrete loaders (`BrAnimationLoader`, `BrModelLoader`, etc.) parse resources and publish to managers via registry seams.
- **Visitor pattern**: `ModelVisitor` hierarchy in `client/render/visitor/` for traversing model geometry during rendering. Includes `RenderModelVisitor`, `HighSpeedRenderModelVisitor`, `CollectLocatorModelVisitor`.
- **Codec approach**: Heavy use of `io.netty.buffer.ByteBuf` codecs across model, animation, particle, and Molang types. Importer owns schema codecs in `eyelib-importer/`.
- **Lookup seams**: Domain-local read facades (`AnimationLookup`, `ModelLookup`, `ClientEntityLookup`, `RenderControllerLookup`, `ParticleLookup`) narrow runtime queries instead of singleton reach-through.
- **Separation of concerns via `mc/api` and `mc/impl`**: Minecraft/Forge types are quarantined in `mc/impl/`, with platform-free API bridges in `mc/api/` where cross-boundary publication is needed.

## Layers

### Gradle Subprojects (Platform-Free Engine)

- **Purpose**: Own reusable engine logic with zero Minecraft/Forge dependencies
- **Location**: `eyelib-molang/`, `eyelib-importer/`, `eyelib-processor/`, `eyelib-material/`, `eyelib-attachment/`
- **Contains**: Molang compiler/type/runtime, Bedrock schema and CODEC definitions, addon/pack discovery, platform-free processing helpers, material definitions, data attachment contracts
- **Depends on**: Plain Java/JDK, codec libraries, each other (`importer → molang`, `processor → molang`)
- **Used by**: Root runtime module via Gradle `api project(...)` dependencies
- **Key constraint**: Must not import Minecraft, Forge, Blaze3D, or LWJGL types

### Root Runtime Module (Forge Mod)

- **Purpose**: Minecraft/Forge integration, client rendering, animation execution, particle systems, GUI tooling, network sync
- **Location**: `src/main/java/io/github/tt432/eyelib/`
- **Contains**: All runtime execution, Forge event subscriptions, Minecraft entity integration, render pipeline, UI
- **Depends on**: All Gradle subprojects, MinecraftForge, Minecraft, Blaze3D, LWJGL, FastUtil
- **Used by**: Forge mod loader (entry via `@Mod` annotation)

### `client/` — Client-Only Runtime and Tooling

- **Purpose**: Rendering, animation, particle emitters, model bake/import, GUI, tooling/debug screens
- **Location**: `src/main/java/io/github/tt432/eyelib/client/`
- **Contains**: `animation/`, `model/`, `render/`, `particle/`, `gui/`, `loader/`, `manager/`, `registry/`, `entity/`, `compat/`, `cursor/`, `gl/`, `instrument/`
- **Depends on**: `mc/impl/` for platform integration, `network/` for sync, `capability/` for component state, subprojects for schema/engine
- **Key constraint**: Must stay out of `common/` shared zone; client-only code must not execute on server

### `mc/impl/` — Platform Integration Quarantine Zone

- **Purpose**: Sole long-term home for direct Minecraft/Forge imports and lifecycle wiring
- **Location**: `src/main/java/io/github/tt432/eyelib/mc/impl/`
- **Contains**: `bootstrap/`, `client/loader/`, `common/command/`, `data_attach/`, `mixin/`, `modbridge/`, `molang/`, `network/`, `util/`, `capability/`
- **Depends on**: Everything else — this is the integration layer that binds Eyelib abstractions to Minecraft/Forge runtimes
- **Used by**: `client/`, `network/`, `capability/`

### `network/` — Sync Layer

- **Purpose**: Packet registration, side-aware routing, client handler to domain-service delegation
- **Location**: `src/main/java/io/github/tt432/eyelib/network/`
- **Contains**: `EyelibNetworkManager.java`, `NetClientHandlers.java`, `dataattach/`, packet contract classes
- **Depends on**: `mc/impl/network/` for transport, domain services for packet application

### `capability/` — Entity State Layer

- **Purpose**: Per-entity attachable data types and render-related state holders
- **Location**: `src/main/java/io/github/tt432/eyelib/capability/`
- **Contains**: `RenderData.java`, `component/` (AnimationComponent, ClientEntityComponent, ModelComponent, RenderControllerComponent), extra entity data types
- **Depends on**: `eyelib-attachment/` for data contracts, Molang for scope, importer for schema types

## Data Flow

### Primary Request Path — Entity Rendering

1. **Forge event subscribed**: `EntityRenderSystem.onEvent(RenderLevelStageEvent)` in `client/EntityRenderSystem.java:85`
2. **Entity iteration**: Iterates all renderable entities, calls `setupClientEntity()` (`client/EntityRenderSystem.java:96`)
3. **Client entity resolution**: Looks up `BrClientEntity` via `ClientEntityLookup.get()` (`client/entity/ClientEntityLookup.java`)
4. **Render controller setup**: Resolves render controllers via `RenderControllerLookup.get()`, builds `ModelComponent` list (`client/EntityRenderSystem.java:384-389`)
5. **Animation ticking**: `BrAnimator.tickAnimation()` processes animation channels, applies effects (`client/animation/BrAnimator.java`)
6. **Render execution**: `renderComponents()` calls `RenderHelper` → `ModelVisitor` hierarchy to traverse model geometry and emit vertices to `VertexConsumer` (`client/render/visitor/RenderModelVisitor.java:20-36`)

### Write Lane — Asset Loading

1. **Forge reload listener registered**: `mc/impl/client/loader/ClientLoaderLifecycleHooks.java` wires `RegisterClientReloadListenersEvent`
2. **Loader parses resources**: `BrResourcesLoader` and concrete loaders (`client/loader/Br*Loader.java`) parse JSON from resource packs using importer codecs
3. **Publication via registry**: Loaders hand parsed data to domain registry seams (`client/registry/*AssetRegistry.java`)
4. **Manager storage**: Registries push data into `Manager.put()` (`client/manager/Manager.java:14`), which publishes `ManagerEntryChangedEvent` via `mc/api` bridge

### Sync Lane — Network to Client

1. **Packet arrives**: `mc/impl/network/EyelibNetworkTransport.java` routes packet to handler method
2. **Handler delegates**: `NetClientHandlers.java` routes to domain services (`ClientRenderSyncService`, `ParticleSpawnService`, `DataAttachmentSyncRuntime`)
3. **Domain service applies**: Services modify `RenderData` components or particle emitter state
4. **Render picks up**: `EntityRenderSystem` reads updated `RenderData` on next frame

### State Management

Entity render state lives in `RenderData` capability attached to each `Entity`. The `RenderData` owns:
- `MolangScope` — per-entity Molang variable scope for expression evaluation
- `ClientEntityComponent` — currently applied `BrClientEntity` and model/controller references
- `ModelComponent` list — per-controller model/texture/render-type bindings
- `AnimationComponent` — animation state, channel data, effects
- `RenderControllerComponent` — per-controller slot state

State synchronization (server→client) flows through dedicated packet types (`ModelComponentSyncPacket`, `AnimationComponentSyncPacket`) applied via `ClientRenderSyncService`.

## Key Abstractions

### Manager<T> (Observable Store)

- **Purpose**: Provides read/write port interfaces and event-backed observable singleton storage for domain assets
- **Examples**: `AnimationManager.java`, `ModelManager.java`, `MaterialManager.java`, `ParticleManager.java`, `RenderControllerManager.java`, `ClientEntityManager.java`, `AttachableManager.java`
- **Pattern**: `Manager<T>` implements both `ManagerReadPort<T>` and `ManagerWritePort<T>`, delegating storage to `ManagerStorage<T>`. Writes publish `ManagerEntryChangedEvent` through `ManagerEventPublishBridge` → `mc/api` → Forge event bus.

### Lookup Seam (Read Facade)

- **Purpose**: Narrow runtime read access through domain-local static methods, avoiding singleton reach-through
- **Examples**: `client/animation/AnimationLookup.java`, `client/model/ModelLookup.java`, `client/entity/ClientEntityLookup.java`, `client/entity/AttachableLookup.java`, `client/render/controller/RenderControllerLookup.java`, `client/particle/ParticleLookup.java`
- **Pattern**: Each lookup is a `@NoArgsConstructor(access = PRIVATE)` final class with static methods delegating to the corresponding manager's `readPort()`.

### BrResourcesLoader (Resource Reload Pattern)

- **Purpose**: Base class for JSON-suffix-based resource reload listeners
- **Example**: `client/loader/BrResourcesLoader.java`, with subclasses `BrAnimationLoader`, `BrModelLoader`, `BrParticleLoader`, `BrRenderControllerLoader`, `BrClientEntityLoader`, `BrAttachableLoader`, `BrMaterialLoader`, `BrAnimationControllerLoader`
- **Pattern**: Abstract base provides suffix filtering, JSON parsing, and map accumulation; concrete loaders define the target manager and parsing logic via template method `load()`.

### ModelVisitor (Render Traversal)

- **Purpose**: Visitor-pattern traversal of model geometry during rendering
- **Examples**: `client/render/visitor/ModelVisitor.java` (abstract base), `RenderModelVisitor.java` (vertex emission), `HighSpeedRenderModelVisitor.java` (optimized), `CollectLocatorModelVisitor.java` (bone locator collection), `BuiltInBrModelRenderVisitors.java` (factory)
- **Pattern**: Visitor receives render params, visit context, model cube, and per-vertex data (position, UV, normal). `ModelVisitContext` carries mutable per-visit state (bone transforms, locators).

### RenderData (Capability Component Host)

- **Purpose**: Per-entity Forge capability that aggregates all Eyelib render state
- **Example**: `capability/RenderData.java`
- **Pattern**: Implements Forge `ICapabilityProvider`, owns component objects (animation, model, client entity, render controller), provides `getComponent()` static accessor, and holds `MolangScope`.

### MolangScope / MolangValue (Engine Runtime)

- **Purpose**: Molang variable scope for expression evaluation, numeric/vector value wrappers
- **Examples**: `eyelib-molang/.../MolangScope.java`, `eyelib-molang/.../MolangValue.java`, `MolangValue2.java`, `MolangValue3.java`, `MolangValue4.java`
- **Pattern**: Scope is a key-value store for Molang variables; values carry type information and support arithmetic/comparison operations. `MolangCompiler` transforms expression strings into compiled functions.

## Entry Points

### Forge Mod Startup
- **Location**: `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`
- **Triggers**: Forge `@Mod` annotation discovers this class during mod loading
- **Responsibilities**: Register capabilities via `EyelibAttachableData.DATA_ATTACHMENTS.register(bus)`, initialize network via `EyelibNetworkManager.register()`

### Client Tick and Render
- **Location**: `src/main/java/io/github/tt432/eyelib/client/ClientTickHandler.java`, `src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java`
- **Triggers**: `@Mod.EventBusSubscriber(value = Dist.CLIENT)` — Forge `TickEvent.ClientTickEvent`, `RenderLevelStageEvent`, `RenderLivingEvent.Pre`, `LivingEvent.LivingTickEvent`, `EntityJoinLevelEvent`
- **Responsibilities**: Global tick counter, entity render orchestration, animation ticking, render model component composition

### Resource Reload
- **Location**: `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java`
- **Triggers**: `RegisterClientReloadListenersEvent`
- **Responsibilities**: Register all `Br*Loader` instances as Forge reload listeners

### Network Channel
- **Location**: `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`
- **Triggers**: Called during `EyelibMod` constructor via `EyelibNetworkManager.register()`
- **Responsibilities**: Create Forge `SimpleChannel`, register all packet types with codecs, handle side-aware dispatch and packet context gating

### Manager Screen (Debug/Dev UI)
- **Location**: `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java`
- **Triggers**: Keybind via `client/gui/manager/hotkey/`
- **Responsibilities**: Import/watch Bedrock addons and resources, delegate to reload planners, publish to managers via registry seams

## Architectural Constraints

- **Threading:** Single-threaded Minecraft main thread for rendering and tick. Client-side parallel stream used for entity iteration in `EntityRenderSystem.entities()`. No custom threading primitives.
- **Global state:** Manager singletons (`AnimationManager`, `ModelManager`, etc.) hold global mutable state. `ClientTickHandler.tick` is a package-private static int. `ClientTaskScheduler.TASKS` is a static `ArrayList`.
- **Circular imports:** Not detected. Importer and processor subprojects have one-way dependency on molang. Root depends on all subprojects. Subprojects do not depend on root.
- **Mixin integration:** Mixin classes are quarantined in `mc/impl/mixin/` with config in `src/main/resources/eyelib.mixins.json`. No split-package issues.
- **Generated code:** `eyelib-molang/.../generated/` contains read-only generated parser artifacts (lexer, parser, visitor). Must not be hand-edited.

## Anti-Patterns

### Direct Manager Reach-Through

**What happens:** Historically, code accessed manager singletons directly (e.g., `Eyelib.getAnimationManager()`).
**Why it's wrong:** Creates hidden coupling, makes it harder to change storage or add validation.
**Do this instead:** Use domain-local lookup seams (`AnimationLookup.get("name")` → `AnimationManager.readPort().get("name")`). The old reach-through accessors have been removed from `Eyelib.java`.

### Importer/Minecraft Type Leakage in Processing Code

**What happens:** Platform-free code in `eyelib-processor` or `eyelib-importer` importing Minecraft types like `ResourceLocation`.
**Why it's wrong:** Breaks the one-way dependency contract; subprojects must remain consumable without Forge.
**Do this instead:** Use `String` keys in processor/importer signatures. Convert to `ResourceLocation` at root runtime boundaries only (e.g., `ClientRenderSyncService.decodeModelPayload()` calls `ResourceLocations.of()`).

### Cast Suppression Warnings

**What happens:** `EntityRenderSystem.java` contains `// todo 权宜之计` (expedient) comments with unchecked casts like `private static <T> T cast(Object obj) { return (T) obj; }`.
**Why it's wrong:** Suppresses type safety, hides generic mismatch issues between animation runtime and model types.
**Do this instead:** Design generic boundaries correctly in the animation/model type hierarchy.

### Event Bus Subscriber in Domain Class

**What happens:** `EntityRenderSystem` is both a Forge `@EventBusSubscriber` and a domain logic class.
**Why it's wrong:** Mixes platform wiring with business logic, makes the class harder to test in isolation.
**Do this instead:** The ongoing `mc/impl` quarantine refactor moves Forge event subscriptions into dedicated hook classes under `mc/impl/` (already done for capability invalidation: `mc/impl/capability/CapabilityComponentRuntimeHooks.java`). Similar extraction is in progress for `EntityRenderSystem`.

## Error Handling

**Strategy:** Defensive null-checking with `@Nullable` annotations (JSpecify). Most operations silently no-op on null input rather than throwing.

**Patterns:**
- Manager lookups return `@Nullable T`; callers null-check before use
- `getComponent()` may return null or uninitialized state; checked via `cap.getOwner() != entity` before use
- Molang scope operations (`scope.get()`, `scope.set()`) are tolerant of missing variables
- `ExpressionCompileException` in Molang compiler — caught at compile time, not in runtime hot path
- `MolangUncompilableException` for expression validation failures

## Cross-Cutting Concerns

**Logging:** Uses SLF4J via Forge-provided logging. `forge.logging.markers` configured for `REGISTRIES` level. `logLevel = org.slf4j.event.Level.DEBUG` in run configs.

**Validation:** Importer codec-based JSON parsing provides structural validation at load time. Manager entry publication includes validation through type-safe generic boundaries. No separate validation framework.

**Authentication:** Not applicable — this is a client-side rendering library for Minecraft Forge mods.

**Observability:** Manager entry changes published through `ManagerEntryChangedEvent` on `MinecraftForge.EVENT_BUS`. `TextureChangedEvent` for texture invalidation. `InitComponentEvent` for entity component initialization.

---

*Architecture analysis: 2026-05-06*
