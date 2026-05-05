# Codebase Structure

**Analysis Date:** 2026-05-06

## Directory Layout

```
qylEyelib/                              # Project root
├── build.gradle                        # Root build script (Forge mod + legacyForge plugin)
├── settings.gradle                     # Gradle settings, includes all subprojects
├── gradle.properties                   # Build properties (versions, mod metadata)
├── src/
│   ├── main/
│   │   ├── java/io/github/tt432/eyelib/  # Root runtime module (Forge mod)
│   │   │   ├── Eyelib.java             # MOD_ID constant holder
│   │   │   ├── api/                    # Future stable API surface (placeholder)
│   │   │   ├── capability/             # Per-entity capability types + components
│   │   │   │   └── component/          # AnimationComponent, ClientEntityComponent,
│   │   │   │                            # ModelComponent, RenderControllerComponent
│   │   │   ├── client/                 # Client-only rendering, animation, UI
│   │   │   │   ├── EntityRenderSystem.java  # Main render orchestration
│   │   │   │   ├── ClientTickHandler.java   # Client tick counter
│   │   │   │   ├── ClientTaskScheduler.java # Deferred task scheduling
│   │   │   │   ├── animation/          # Animation runtime, lookups, effects
│   │   │   │   │   └── bedrock/        # Bedrock animation adapters
│   │   │   │   ├── model/              # Model runtime, bake, import
│   │   │   │   │   ├── bake/           # BakedModel, EmissiveModelBakeInfo
│   │   │   │   │   ├── bbmodel/        # Blockbench model runtime
│   │   │   │   │   └── importer/       # Runtime importer facades
│   │   │   │   ├── render/             # Render pipeline, visitors, textures
│   │   │   │   │   ├── controller/     # Render controller runtime
│   │   │   │   │   ├── sync/           # Client render sync service
│   │   │   │   │   ├── texture/        # NativeImageIO, TextureLayerMerger
│   │   │   │   │   └── visitor/        # ModelVisitor, RenderModelVisitor, etc.
│   │   │   │   ├── particle/           # Particle emitters, spawn/remove services
│   │   │   │   │   └── bedrock/        # Bedrock particle runtime
│   │   │   │   ├── entity/             # Client entity/attachable runtime
│   │   │   │   ├── gui/                # GUI screens and tooling
│   │   │   │   │   ├── manager/        # EyelibManagerScreen + sub-seams
│   │   │   │   │   └── preview/        # ModelPreviewScreen, ModelPreviewAsset
│   │   │   │   ├── loader/             # Resource reload listeners
│   │   │   │   ├── manager/            # Singleton manager stores
│   │   │   │   ├── registry/           # Loader/tooling-to-manager publication
│   │   │   │   ├── compat/             # External client compatibility
│   │   │   │   ├── cursor/             # Cursor helpers
│   │   │   │   ├── gl/                 # GL-specific client support
│   │   │   │   ├── material/           # Material entries
│   │   │   │   └── instrument/         # Rendering instrumentation
│   │   │   ├── common/                 # Shared server/client behavior
│   │   │   │   ├── behavior/           # Entity behavior logic
│   │   │   │   └── runtime/            # Platform-free command runtime
│   │   │   ├── core/                   # Platform-free utility seams
│   │   │   │   └── util/               # codec/, collection/, color/, texture/, time/
│   │   │   ├── event/                  # Custom Eyelib events
│   │   │   ├── internal/               # Internal implementation marker
│   │   │   ├── mc/                     # Minecraft/Forge integration boundary
│   │   │   │   ├── api/                # Platform-type-free API bridges
│   │   │   │   │   └── client/
│   │   │   │   │       └── manager/    # Manager event publish bridge
│   │   │   │   └── impl/               # Hard quarantine zone — MC/Forge types
│   │   │   │       ├── bootstrap/      # EyelibMod.java (Forge @Mod entry)
│   │   │   │       ├── capability/     # Forge component invalidation hooks
│   │   │   │       ├── client/         # loader/, manager/
│   │   │   │       ├── common/         # command/
│   │   │   │       ├── data_attach/    # Forge capability/provider/NBT wiring
│   │   │   │       ├── mixin/          # Minecraft mixin classes
│   │   │   │       ├── modbridge/      # Forge modbridge event wiring
│   │   │   │       ├── molang/         # Molang MC query/lifecycle bindings
│   │   │   │       ├── network/        # Forge SimpleChannel, packet impl, transport
│   │   │   │       └── util/           # MC/Forge utility adapters
│   │   │   ├── molang/                 # Legacy Molang marker/handoff path
│   │   │   ├── network/                # Packet registration + client handlers
│   │   │   │   └── dataattach/         # Attachment sync payload ops
│   │   │   └── util/                   # Shared helpers (transitional)
│   │   │       ├── client/             # Client utility (texture helpers, models)
│   │   │       ├── codec/              # Stream/codec helpers
│   │   │       ├── math/               # Math/transform utilities
│   │   │       ├── modbridge/          # Integration bridge helpers
│   │   │       └── search/             # Search/index result helpers
│   │   └── resources/
│   │       ├── eyelib.mixins.json      # Mixin configuration
│   │       └── META-INF/mods.toml      # Forge mod metadata
│   └── test/java/                      # JUnit 5 tests
├── eyelib-molang/                      # Molang engine subproject
│   ├── build.gradle
│   └── src/main/java/.../eyelibmolang/
│       ├── MolangValue*.java           # Numeric/vector value wrappers
│       ├── MolangScope.java            # Variable scope
│       ├── compiler/                   # Compiler, frontend, cache, bytecode
│       │   ├── bound/                  # Bound compiler inputs
│       │   ├── cache/                  # MolangDiskCache
│       │   ├── common/                 # CompileContext
│       │   └── frontend/               # Compiler frontend
│       ├── generated/                  # READ-ONLY — generated lexer/parser
│       ├── mapping/                    # Molang built-in mappings, mapping API
│       │   └── api/                    # MappingDiscovery, QueryRuntime ports
│       ├── type/                       # Molang type system
│       └── util/                       # Molang-specific utilities
├── eyelib-importer/                    # Importer/schema subproject
│   ├── build.gradle
│   └── src/main/java/.../eyelibimporter/
│       ├── addon/                      # Bedrock addon/pack discovery
│       ├── animation/                  # Animation schema definitions
│       ├── entity/                     # Client-entity/attachable schema
│       ├── material/                   # Material schema
│       ├── mc/                         # (Minecraft-facing, transitional)
│       ├── model/                      # Model definitions, locators, tree
│       │   ├── bbmodel/                # Blockbench model format
│       │   ├── bedrock/                # Bedrock geometry format
│       │   ├── importer/               # Importer data/repacker
│       │   └── locator/                # Model locator support
│       ├── particle/                   # Particle schema
│       ├── render/                     # Render-related schema
│       └── util/                       # Importer-specific utilities
├── eyelib-processor/                   # Processing/batching subproject
│   ├── build.gradle
│   └── src/main/java/.../eyelibprocessor/
│       ├── animation/                  # Animation processing helpers
│       ├── loader/                     # Loader parsing operators
│       ├── manager/                    # Manager reload planning
│       └── particle/                   # Particle processing helpers
├── eyelib-material/                    # Material subproject
│   ├── build.gradle
│   └── src/main/java/.../eyelibmaterial/
│       ├── bootstrap/                  # Material bootstrap
│       ├── gl/                         # GL state management
│       ├── material/                   # Material definitions
│       ├── render/                     # Material render helpers
│       ├── shader/                     # Shader pipeline
│       ├── shared/                     # Shared pure-data types
│       └── util/                       # Material utilities
├── eyelib-attachment/                  # Data attachment subproject
│   ├── build.gradle
│   └── src/main/java/.../eyelibattachment/
│       ├── bootstrap/                  # Attachment bootstrap
│       ├── codec/                      # Attachment codecs
│       └── dataattach/                 # Storage, container, type contracts
├── docs/                               # Documentation
│   ├── architecture/                   # Boundary docs, control spec
│   ├── blockbench/                     # Blockbench reference
│   ├── index/                          # Navigation indexes
│   └── reference/                      # External reference docs
├── work/                               # Refactor tracker
└── .planning/                          # GSD planning artifacts
    └── codebase/                       # Codebase map documents
```

## Directory Purposes

### `src/main/java/io/github/tt432/eyelib/` — Root Runtime Module
- **Purpose:** Forge mod runtime — all Minecraft/Forge integration, client rendering, animation, particles, GUI, and sync logic
- **Contains:** Java source files organized into domain packages
- **Key files:** `Eyelib.java` (MOD_ID constant)

### `src/main/java/io/github/tt432/eyelib/client/` — Client Runtime
- **Purpose:** All client-only rendering, animation execution, particle systems, model import/bake, GUI tooling
- **Contains:** 18 sub-packages covering the full client runtime surface
- **Key files:** `EntityRenderSystem.java`, `ClientTickHandler.java`, `ClientTaskScheduler.java`

### `src/main/java/io/github/tt432/eyelib/mc/impl/` — Platform Integration Quarantine
- **Purpose:** Sole home for direct Minecraft, Forge, Blaze3D, and LWJGL imports
- **Contains:** `bootstrap/`, `client/`, `common/`, `data_attach/`, `mixin/`, `modbridge/`, `molang/`, `network/`, `util/`, `capability/`
- **Key files:** `bootstrap/EyelibMod.java`, `network/EyelibNetworkTransport.java`, `mixin/` classes

### `eyelib-molang/` — Molang Engine Subproject
- **Purpose:** Platform-free Molang compiler, type system, value wrappers, scope management, generated parser, mapping API
- **Contains:** `compiler/`, `generated/` (read-only), `mapping/`, `type/`, `util/`
- **Key files:** `MolangScope.java`, `MolangCompiler.java`, `MolangValue.java`, `MolangBuiltInMappings.java`

### `eyelib-importer/` — Importer/Schema Subproject
- **Purpose:** Bedrock addon/pack discovery, schema definitions (model, animation, entity, controller), CODEC trees, source-format parsing
- **Contains:** `addon/`, `animation/`, `entity/`, `material/`, `model/`, `particle/`, `render/`, `util/`
- **Key files:** `model/Model.java`, `addon/BedrockAddon.java`, `model/bbmodel/`, `model/bedrock/`

### `eyelib-processor/` — Processing Subproject
- **Purpose:** Platform-free processing, batching, reload planning helpers for loader and manager seams
- **Contains:** `animation/`, `loader/`, `manager/`, `particle/`
- **Key files:** `loader/LoaderParsingOps.java`, `manager/ManagerResourceReloadPlan.java`

### `eyelib-material/` — Material Subproject
- **Purpose:** Bedrock material definitions, GL state management, shader pipeline
- **Contains:** `bootstrap/`, `gl/`, `material/`, `render/`, `shader/`, `shared/`, `util/`
- **Key files:** Material definition and GL state classes

### `eyelib-attachment/` — Data Attachment Subproject
- **Purpose:** Platform-type-free typed attachment storage, container contracts, mutation interfaces
- **Contains:** `bootstrap/`, `codec/`, `dataattach/`
- **Key files:** `DataAttachment.java`, `DataAttachmentContainer.java`, `DataAttachmentType.java`

### `docs/` — Documentation
- **Purpose:** Architecture specs, boundary rules, navigation indexes, Blockbench/Bedrock references
- **Key files:** `architecture/00-control-spec.md`, `architecture/01-module-boundaries.md`, `architecture/02-side-boundaries.md`, `architecture/ARCHITECTURE-BLUEPRINT.md`, `index/repo-map.md`

### `.planning/codebase/` — GSD Codebase Maps
- **Purpose:** Generated codebase analysis documents consumed by GSD planning/execution commands
- **Contains:** `STACK.md`, `INTEGRATIONS.md`, `ARCHITECTURE.md`, `STRUCTURE.md`, `CONVENTIONS.md`, `TESTING.md`, `CONCERNS.md`
- **Generated:** Yes (by `/gsd-map-codebase` command)
- **Committed:** No (generated artifacts)

## Key File Locations

### Entry Points
- `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`: Forge `@Mod` composition root — capability and network registration
- `src/main/java/io/github/tt432/eyelib/Eyelib.java`: `MOD_ID` constant holder, legacy compatibility surface

### Configuration
- `build.gradle`: Root build — Forge legacyForge plugin, dependencies on all subprojects, NullAway, Lombok, Mixin
- `settings.gradle`: Subproject includes (`eyelib-attachment`, `eyelib-importer`, `eyelib-material`, `eyelib-molang`, `eyelib-processor`)
- `gradle.properties`: Version properties (`mod_version`, `minecraft_version`, `forge_version`, `mappings`)
- `src/main/resources/eyelib.mixins.json`: Mixin config — points to `io.github.tt432.eyelib.mc.impl.mixin`
- `src/main/resources/META-INF/mods.toml`: Forge mod metadata

### Core Logic — Client Rendering Pipeline
- `src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java`: Main entity render orchestration (409 lines)
- `src/main/java/io/github/tt432/eyelib/client/render/RenderHelper.java`: Render context and visitor orchestration
- `src/main/java/io/github/tt432/eyelib/client/render/visitor/RenderModelVisitor.java`: Vertex emission visitor
- `src/main/java/io/github/tt432/eyelib/client/animation/BrAnimator.java`: Animation ticking and playback

### Core Logic — Asset Loading
- `src/main/java/io/github/tt432/eyelib/client/loader/BrResourcesLoader.java`: Base resource reload pattern
- `src/main/java/io/github/tt432/eyelib/client/loader/BrModelLoader.java`: Model resource loader
- `src/main/java/io/github/tt432/eyelib/client/loader/BrAnimationLoader.java`: Animation resource loader
- `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java`: Forge reload-listener wiring

### Core Logic — Manager Stores
- `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`: Abstract observable store base class
- `src/main/java/io/github/tt432/eyelib/client/manager/AnimationManager.java`: Animation singleton store
- `src/main/java/io/github/tt432/eyelib/client/manager/ModelManager.java`: Model singleton store
- `src/main/java/io/github/tt432/eyelib/client/manager/ModelManager.java`: Model singleton store

### Core Logic — Network
- `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`: Packet registration + send-to-server bridge
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`: Forge SimpleChannel wiring
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`: Client packet → domain service routing

### Core Logic — Entity State
- `src/main/java/io/github/tt432/eyelib/capability/RenderData.java`: Per-entity render capability
- `src/main/java/io/github/tt432/eyelib/capability/component/AnimationComponent.java`: Animation state component
- `src/main/java/io/github/tt432/eyelib/capability/component/ModelComponent.java`: Model binding component

### Lookup Seams (Read Facades)
- `src/main/java/io/github/tt432/eyelib/client/animation/AnimationLookup.java`
- `src/main/java/io/github/tt432/eyelib/client/model/ModelLookup.java`
- `src/main/java/io/github/tt432/eyelib/client/entity/ClientEntityLookup.java`
- `src/main/java/io/github/tt432/eyelib/client/entity/AttachableLookup.java`
- `src/main/java/io/github/tt432/eyelib/client/render/controller/RenderControllerLookup.java`
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java`

### Testing
- `src/test/`: JUnit 5 tests for root module
- `eyelib-molang/src/test/`: Molang engine tests
- `eyelib-importer/src/test/`: Importer tests including fixture resources under `src/test/resources/`

## Naming Conventions

### Files
- **Loader classes:** `Br*Loader.java` (e.g., `BrAnimationLoader`, `BrModelLoader`) — "Br" prefix for Bedrock
- **Manager classes:** `*Manager.java` (e.g., `AnimationManager`, `ModelManager`)
- **Packet classes:** `*Packet.java` or `*SyncPacket.java` in `mc/impl/network/packet/`
- **Lookup facades:** `*Lookup.java` (e.g., `AnimationLookup`, `ModelLookup`)
- **Registry seams:** `*AssetRegistry.java` (e.g., `AnimationAssetRegistry`, `ModelAssetRegistry`)
- **Visitor classes:** `*Visitor.java` (e.g., `ModelVisitor`, `RenderModelVisitor`)
- **Molang values:** `MolangValue*.java` (numbered: `MolangValue`, `MolangValue2`, `MolangValue3`, `MolangValue4`)
- **Importer types:** `Br*` prefix (e.g., `BrClientEntity`, `BrParticle`, `BrControllerState`) — Bedrock convention
- **README files:** `README.md` in most package directories

### Directories
- **Domain packages:** Lowercase descriptive names (`animation/`, `model/`, `render/`, `particle/`)
- **Importer format packages:** `bbmodel/`, `bedrock/` for format-specific code
- **Platform integration:** `mc/api/` for bridges, `mc/impl/` for quarantine zone
- **Subproject namespaces:** Each subproject uses its own root package (`eyelibmolang`, `eyelibimporter`, `eyelibprocessor`, `eyelibmaterial`, `eyelibattachment`)

### Package Naming
- **Root module:** `io.github.tt432.eyelib.*`
- **Molang subproject:** `io.github.tt432.eyelibmolang.*`
- **Importer subproject:** `io.github.tt432.eyelibimporter.*`
- **Processor subproject:** `io.github.tt432.eyelibprocessor.*`
- **Material subproject:** `io.github.tt432.eyelibmaterial.*`
- **Attachment subproject:** `io.github.tt432.eyelibattachment.*`

## Where to Add New Code

### New Feature — Render/Animation Runtime
- **Primary code:** `src/main/java/io/github/tt432/eyelib/client/{domain}/`
- **Tests:** `src/test/java/io/github/tt432/eyelib/`

### New Feature — Model/Animation Schema or Bedrock Format
- **Schema definitions (parsing, CODECs):** `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/{domain}/`
- **Runtime integration (execution, upload):** `src/main/java/io/github/tt432/eyelib/client/model/importer/` or appropriate runtime package
- **Don't put runtime execution in importer** — the importer owns definitions only

### New Feature — Molang Engine
- **Engine code:** `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`
- **Platform bindings (MC queries, lifecycle):** `src/main/java/io/github/tt432/eyelib/mc/impl/molang/`
- **Don't edit** `eyelib-molang/.../generated/` — it's read-only generated code

### New Feature — Processing/Batching Helpers
- **Platform-free helpers:** `eyelib-processor/src/main/java/io/github/tt432/eyelibprocessor/`
- **Runtime orchestration (upload, events):** `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/`
- **Don't put runtime lifecycle in processor** — it's platform-free only

### New Feature — Material/Shader
- **Material definitions:** `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/`
- **GL/Shader pipeline:** `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/gl/`, `shader/`
- **Root integration:** `src/main/java/io/github/tt432/eyelib/client/material/`

### New Network Packet
- **Packet class:** `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/`
- **Transport registration:** Add to `EyelibNetworkTransport.register()` in `mc/impl/network/EyelibNetworkTransport.java`
- **Client handler:** Add to `NetClientHandlers` in `network/NetClientHandlers.java` — delegate to domain service
- **Domain service:** Create in appropriate `client/` subpackage (e.g., `client/render/sync/`, `client/particle/`)

### New Manager Store
- **Manager class:** `src/main/java/io/github/tt432/eyelib/client/manager/` — extend `Manager<T>`
- **Registry seam (if loader publishes):** `src/main/java/io/github/tt432/eyelib/client/registry/`
- **Lookup facade (for reads):** `src/main/java/io/github/tt432/eyelib/client/{domain}/` — static methods delegating to manager `readPort()`

### Platform Integration (MC/Forge Wiring)
- **All MC/Forge imports go in:** `src/main/java/io/github/tt432/eyelib/mc/impl/{area}/`
- **API bridges (platform-free contracts):** `src/main/java/io/github/tt432/eyelib/mc/api/{area}/`

### Utilities
- **Platform-free helpers (no MC/Forge):** `src/main/java/io/github/tt432/eyelib/core/util/{area}/`
- **MC/Forge-aware utility adapters:** `src/main/java/io/github/tt432/eyelib/mc/impl/util/{area}/`
- **Transitional/mixed helpers:** `src/main/java/io/github/tt432/eyelib/util/{area}/` (being migrated)

## Special Directories

### `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- **Purpose:** Generated lexer, parser, and visitor artifacts from Molang grammar
- **Generated:** Yes (by ANTLR or custom parser generator)
- **Committed:** Yes (checked in as read-only reference)
- **Rule:** Do not hand-edit; regenerate from grammar when changes needed

### `src/main/java/io/github/tt432/eyelib/molang/grammer/`
- **Purpose:** Legacy marker/handoff path — historical location of Molang grammar
- **Generated:** No (documentation/compatibility only)
- **Rule:** Do not add new handwritten logic here; use `eyelib-molang/` subproject instead

### `src/generated/resources/`
- **Purpose:** Forge data generation output directory
- **Generated:** Yes
- **Committed:** Not committed (generated during build)
- **Mapped as:** `sourceSets.main.resources { srcDir 'src/generated/resources' }`

### `src/main/java/io/github/tt432/eyelib/util/client/`
- **Purpose:** Transitional client utility area — being drained into named owners under `client/`, `core/`, and `mc/impl/`
- **Contains:** `AnimationApplier.java`, `Models.java`, `texture/TexturePathHelper.java`
- **Rule:** Prefer `core/util/` or dedicated `client/` subpackages for new utility code; this area is narrowing

### `run/`
- **Purpose:** Minecraft Forge development client run directory (generated during `runClient`)
- **Generated:** Yes
- **Committed:** No (in `.gitignore`)

### `bin/`
- **Purpose:** IDE output directory (IDEA compilation output)
- **Generated:** Yes
- **Committed:** No (in `.gitignore`)

---

*Structure analysis: 2026-05-06*
