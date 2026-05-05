# External Integrations

**Analysis Date:** 2026-05-06

## APIs & External Services

**Minecraft Forge Runtime:**
- MinecraftForge 47.1.3 - Mod loader providing event bus (`@SubscribeEvent`, `IEventBus`), network (`SimpleChannel`), rendering pipeline, and capability system
  - SDK/Client: Forge classes resolved via `legacyForge` Gradle plugin (NeoForged ModDev)
  - Auth: Not applicable (mod runs within Minecraft process)

**Minecraft Vanilla (Access Transformers + Mixins):**
- Access Transformers: 49 entries in `src/main/resources/META-INF/accesstransformer.cfg` widening access to Minecraft internals (ModelPart, BufferBuilder, PoseStack, RenderType, NativeImage, etc.)
- Mixins: 3 client-only mixins registered in `src/main/resources/eyelib.mixins.json`:
  - `HumanoidModelMixin` - humanoid model hooks
  - `LivingEntityRendererAccessor` - renderer access
  - `MultiPlayerGameModeMixin` - game mode hooks
- Mixin package root: `io.github.tt432.eyelib.mc.impl.mixin`

**Accelerated Rendering Compatibility (Optional):**
- Modrinth mod `acceleratedrendering:1.0.5-1.20.1-alpha` - Optional rendering acceleration
  - Dependency scope: `compileOnly` (not bundled, runtime detection via `LoadingModList`)
  - Integration files: `src/main/java/io/github/tt432/eyelib/client/compat/ar/ARCompat.java`, `ARCompatImpl.java`, `AcceleratedBakedBoneRenderer.java`
  - Detection: Static boolean `ARCompat.AR_INSTALLED` checked at runtime
  - Used for accelerated bone mesh rendering when the mod is present

**Sonatype OSSRH (Maven Central Publishing):**
- Publishing repository: `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/`
  - Credentials: `ossrhUsername` / `ossrhPassword` from Gradle properties
  - Artifact signing via `signing` plugin
  - Group ID: `io.github.tt432`
  - Module archive names: `eyelib`, `eyelib-importer`, `eyelib-material`, `eyelib-attachment`, `eyelib-processor`, `eyelib-molang`

## Data Storage

**Databases:**
- H2 Database (embedded, file-based) - Client-side performance instrumentation
  - JDBC URL: `jdbc:h2:file:./eyelib_instrument;ACCESS_MODE_DATA=rws;WRITE_DELAY=1000`
  - Database file: `./eyelib_instrument.mv.db` (created at runtime in working directory; existing file at project root is a leftover)
  - Implementation: `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java`
  - Used by: `InstrumentLifecycleHooks`, `BackgroundFlushService`, `InstrumentConfig`, performance event collectors
  - Schema: `performance_events` table with fields for event time, type, source, metric name/value/unit, thread name, extra JSON

**File Storage:**
- Local filesystem only - Minecraft resource packs, manager import folders, config files
- Manager screen imports from local folders (`io.github.tt432.eyelib.client.gui.manager.reload`)
- Bedrock addon loading from filesystem (`io.github.tt432.eyelib.client.loader`)

**Caching:**
- Molang compilation disk cache (`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/cache/MolangDiskCache.java`) - persistent compiled bytecode cache
- Gradle build cache (`org.gradle.caching=true`) and configuration cache (`org.gradle.configuration-cache=true`)

## Authentication & Identity

**Auth Provider:**
- No external authentication providers used by the mod
- Mod identity handled by Forge's built-in mod system: mod ID `eyelib`, version `21.1.14+1.20.1-forge`
- Publishing auth: Sonatype OSSRH username/password for Maven Central deployment

## Monitoring & Observability

**Error Tracking:**
- No external error tracking service detected

**Logs:**
- SLF4J API 2.0.7 - Standard logging facade used by subprojects
- Runtime implementation: Log4j (provided by Minecraft Forge runtime)
- Logger usage in `InstrumentDatabase.java` via `java.util.logging.Logger` (legacy); most subprojects use SLF4J

**Performance Instrumentation (Built-in):**
- Custom client-side instrumentation system under `src/main/java/io/github/tt432/eyelib/client/instrument/`
  - `InstrumentDatabase.java` - H2-backed SQL event store
  - `InstrumentConfig.java` - Runtime enable/disable
  - `InstrumentLifecycleHooks.java` - Forge event-based lifecycle
  - `collector/JvmMetricCollector.java` - JVM memory/GC metrics
  - `collector/CacheSizeObserver.java` - Render cache observation
  - `event/EventRingBuffer.java` - In-memory event buffer before DB flush
  - `db/BackgroundFlushService.java` - Async DB write service
- Dedicated tests: `InstrumentDatabaseTest`, `InstrumentConfigTest`, `InstrumentDisabledTest`, etc.

**RenderDoc Integration:**
- Custom Gradle task `runWithRenderDoc` in `build.gradle` lines 317-338
- Launches Minecraft client via RenderDoc's `renderdoccmd.exe` for GPU frame capture
- Configured for `E:\RenderDoc\renderdoccmd.exe`

## CI/CD & Deployment

**Hosting:**
- Maven Central (`io.github.tt432:eyelib`) - primary artifact distribution
- Modrinth (referenced in `settings.gradle` plugin repositories for dependency resolution: `maven.modrinth` group)

**CI Pipeline:**
- No GitHub Actions detected (no `.github/workflows/` directory)
- No CI configuration files found in repository

**Development Run Configurations:**
- Managed by NeoForged ModDev Gradle plugin (`legacyForge.runs`):
  - `client` - Minecraft client with Forge for mod development
  - `server` - Dedicated server with `--nogui`
  - `gameTestServer` - Headless game test server
  - `data` - Data generation run for resource generation

## Environment Configuration

**Required env vars:**
- None for runtime mod operation; all configuration in `gradle.properties`
- No `.env` files present in repository

**Secrets location:**
- Maven Central publishing credentials via Gradle properties (typically `~/.gradle/gradle.properties`):
  - `ossrhUsername`
  - `ossrhPassword`
- Not stored in repository

**Key Gradle Properties (`gradle.properties`):**
| Property | Value | Purpose |
|----------|-------|---------|
| `minecraft_version` | `1.20.1` | Target Minecraft version |
| `forge_version` | `47.1.3` | Target Forge version |
| `mod_id` | `eyelib` | Mod identifier |
| `mod_version` | `21.1.14` | Mod version (SemVer) |
| `mod_group_id` | `io.github.tt432` | Maven group |
| `parchment_minecraft_version` | `1.20.1` | Parchment mappings MC version |
| `parchment_mappings_version` | `2023.09.03` | Parchment mappings date |

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

## Mod Dependency Declarations (mods.toml)

The root mod declares the following runtime dependencies:

| Mod ID | Requirement | Version Range | Side |
|--------|-------------|---------------|------|
| `forge` | mandatory | `[47.1.3,)` | BOTH |
| `minecraft` | mandatory | `[1.20.1, 1.21)` | BOTH |
| `eyelibimporter` | mandatory | `[21.1.14,)` | BOTH |

Subproject `eyelib-importer` is bundled into the root mod via `jarJar` and declared as a runtime dependency.

## Subproject Inter-Module Dependencies

```
root (eyelib)
 ├── :eyelib-attachment    (api, jarJar bundled)
 ├── :eyelib-material      (api, jarJar bundled)
 ├── :eyelib-processor     (api, jarJar bundled)
 │    ├── :eyelib-importer (compileOnly)
 │    └── :eyelib-molang    (implementation)
 ├── :eyelib-importer      (api, jarJar bundled)
 │    ├── :eyelib-molang    (implementation)
 │    └── :eyelib-material  (implementation)
 └── :eyelib-molang        (api, jarJar bundled)
```

All subproject inter-dependencies flow from root outward. `eyelib-molang` and `eyelib-processor` have no dependency back into root runtime packages. The `eyelib-particle/` directory exists but is not included in `settings.gradle` and appears dormant.

---

*Integration audit: 2026-05-06*
