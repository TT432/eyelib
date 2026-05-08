# Phase 1: Module Scaffolding + Config + Annotation Discovery - Context

**Gathered:** 2026-05-06
**Status:** Ready for planning

## Phase Boundary

Build the foundation for the client smoke testing framework — create two Gradle subprojects (`eyelib-clientsmoke-annotation` for the `@ClientSmoke` annotation definition, `eyelib-clientsmoke` for the Forge 1.20.1 runtime engine), implement `ModFileScanData`-based bytecode-level annotation scanning (zero class loading), wire up `ModConfigSpec` configuration with a global `enabled` toggle, and integrate the subprojects into the root module via `compileOnly` + `localRuntime`.

## Implementation Decisions

### Module Structure

- **D-01:** Annotation subproject name: `eyelib-clientsmoke-annotation` (Gradle `settings.gradle` include name)
- **D-02:** Runtime subproject name: `eyelib-clientsmoke` (Gradle `settings.gradle` include name)
- **D-03:** Java package convention follows existing pattern — annotation module uses `io.github.tt432.clientsmokeannotation`, runtime module uses `io.github.tt432.clientsmoke`

### Build Configuration

- **D-04:** Annotation module uses `java-library` Gradle plugin — zero Minecraft/Forge dependencies, produces a plain JVM JAR. Reference: `eyelib-attachment/build.gradle` and `eyelib-processor/build.gradle` use this exact pattern.
- **D-05:** Runtime module uses `net.neoforged.gradle.userdev` with `legacyForge` 2.0.91 — same as root `build.gradle`. Forge 1.20.1 + Java 17 matching existing eyelib platform.

### @ClientSmoke Annotation

- **D-06:** `@Retention(RetentionPolicy.CLASS)` — visible to FML's `ModFileScanData` ASM scanner at classload time, but `getAnnotation()` returns `null` at runtime. This prevents any reflection-based discovery from triggering class initialization.
- **D-07:** `@Target(ElementType.TYPE)` — class-level only for v1. Test methods are discovered through a convention (e.g., implementing a `ClientSmokeRunnable` interface or having a specific method signature), not via method-level annotation scanning.
- **D-08:** Annotation attributes:
  - `String description() default ""` — human-readable test description for report
  - `int priority() default 0` — execution order, lower = first
  - `String modId() default ""` — optional namespacing, empty = global

### ModFileScanData Scanner

- **D-09:** Scanner class lives in the runtime module and is called from the `@Mod` constructor (specifically in the constructor body, not in a static block or event listener). This ensures scanning happens at mod construction time, after FML has finished its own scans.
- **D-10:** Scanning uses `net.minecraftforge.fml.ModList.get().getAllScanData()` filtering for `@ClientSmoke` annotation data entries. Each `ModFileScanData.AnnotationData` record provides `annotationType()`, `clazz()` (target class), `memberName()` (target member if applicable), and `modFile()` (source JAR).
- **D-11:** Must validate that annotated classes do not trigger class loading — add a test with an annotated class containing `static { throw new RuntimeException("loaded!"); }` and confirm the scan completes without that exception surfacing.

### Configuration System

- **D-12:** Config file: `clientsmoke-common.toml`, registered as `ModConfig.Type.COMMON` in the `@Mod` constructor via `modContainer.registerConfig(...)`.
- **D-13:** Config entries:
  - `enabled` (boolean, default `false`) — **master switch**: when `false`, the entire framework is silent (no scanning, no tick handler, no state machine)
  - `screenshotDelay` (int, default `5`) — seconds to wait after world load before first screenshot
  - `reloadStabilizeTicks` (int, default `40`) — render stabilization ticks after player spawn
  - `exitAfterSmoke` (boolean, default `true`) — auto-exit after all tests complete

### Root Module Integration

- **D-14:** Root `build.gradle` adds `compileOnly project(':eyelib-clientsmoke-annotation')` — allows root code to reference `@ClientSmoke` in test classes, but the annotation is not included in the production build output.
- **D-15:** Root `build.gradle` adds `localRuntime project(':eyelib-clientsmoke')` gated behind a Gradle property `enableSmokeTest` (default `false`). This loads the runtime mod at dev time without coupling the build artifacts.
- **D-16:** `settings.gradle` includes both new subprojects after existing entries.

### the agent's Discretion

- Exact class name for the scanner utility (e.g., `ClientSmokeScanner` or `SmokeTestDiscovery`) — pick the clearest, most self-documenting name.
- Whether the scanner returns a typed result object or a raw list of `AnnotationData` entries.
- Logging patterns (use SLF4J, match existing eyelib log levels).
- Exact TOML file path under `run/client/config/` is determined by NeoForge conventions.

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Context
- `.planning/PROJECT.md` — Project description, core value, constraints, key decisions
- `.planning/REQUIREMENTS.md` — v1 requirements (MOD-01–03, ANN-01–03, CFG-01–03 mapped to Phase 1)
- `.planning/ROADMAP.md` — Phase 1 success criteria (5 criteria)
- `.planning/research/SUMMARY.md` — Synthesized research findings, platform decision, key architecture
- `.planning/research/ARCHITECTURE.md` — Full architecture specification with component boundaries and build order
- `.planning/research/STACK.md` — Technology stack (Forge 1.20.1, legacyForge 2.0.91, Java 17)
- `.planning/research/PITFALLS.md` — Class loading safety, ModFileScanData vs reflection, screenshot thread requirements

### Existing Code Reference
- `settings.gradle` — Existing subproject includes, pattern for adding new ones
- `eyelib-attachment/build.gradle` — Reference for `java-library` subproject with zero MC deps
- `eyelib-importer/build.gradle` — Reference for `legacyForge` subproject with mods.toml
- `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java` — Existing `@Mod` entrypoint, constructor pattern
- `src/main/java/io/github/tt432/eyelib/mc/impl/molang/mapping/ForgeMolangMappingDiscovery.java` — Proven `ModFileScanData` usage pattern for annotation discovery
- `.planning/codebase/STACK.md` — Full technology stack, all dependencies, versions
- `.planning/codebase/ARCHITECTURE.md` — Architecture diagram, module structure, component responsibilities
- `.planning/codebase/STRUCTURE.md` — Full directory layout, package conventions

### Reference Implementation
- `E:\____脚本\图形学教学\iris-tutorial-mod\build.gradle` — Proven NeoForge 1.21.1 mod build config (adapt for Forge 1.20.1)
- `E:\____脚本\图形学教学\iris-tutorial-mod\src\main\java\net\irisshaders\tutorial\IrisShaderTutorial.java` — `@Mod` constructor + `EventBusSubscriber` pattern
- `E:\____脚本\图形学教学\iris-tutorial-mod\src\main\java\net\irisshaders\tutorial\TutorialConfig.java` — `ModConfigSpec` configuration pattern

## Existing Code Insights

### Reusable Assets
- **ForgeMolangMappingDiscovery** (`src/main/java/io/github/tt432/eyelib/mc/impl/molang/mapping/ForgeMolangMappingDiscovery.java`): Demonstrates `ModList.get().getAllScanData()` with ASM-level annotation filtering — same pattern needed for `@ClientSmoke` discovery. Extract the filtering logic as reference.
- **EyelibMod constructor** (`src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`): Shows the standard Forge `@Mod(modid)` constructor receiving `IEventBus` and `ModContainer`. The `@ClientSmoke` runtime mod should follow this same entrypoint pattern.

### Established Patterns
- **Multi-project Gradle structure**: Subprojects are included in `settings.gradle` with `include "eyelib-xxx"` and placed at repo root. Each has its own `build.gradle`. Root references them via `project(':eyelib-xxx')`.
- **java-library vs legacyForge split**: Subprojects with no MC dependencies use `java-library` (eyelib-attachment, eyelib-processor). Subprojects that need Forge/MC use `legacyForge` (eyelib-importer, eyelib-material). This split is the exact pattern needed for annotation vs runtime module separation.
- **ModConfigSpec**: Used by iris-tutorial-mod for client config — `ModConfigSpec.Builder` → `define(...)` → `SPEC = builder.build()` → registered in `@Mod` constructor via `modContainer.registerConfig(ModConfig.Type.COMMON, SPEC)`.

### Integration Points
- `settings.gradle` — add two new `include` statements
- Root `build.gradle` — add `compileOnly` and conditional `localRuntime` dependencies
- The runtime module's `@Mod` constructor is the integration point for config registration and scanner initialization.

## Specific Ideas

- Annotation module should be as minimal as possible — literally just the `@ClientSmoke` annotation class and a `package-info.java`. No other classes, no dependencies.
- The `enabled=false` behavior must be absolute: not a single log line, not a single class loaded, not a single event subscribed. Silence is the default.
- The scanner should log discovered test classes at INFO level (e.g., "Found 3 @ClientSmoke test(s): [a.b.FooTest, a.b.BarTest, c.d.BazTest]") so the user knows what will run before any game action happens.

## Deferred Ideas

None — discussion stayed within phase scope.

---

*Phase: 1-Module Scaffolding + Config + Annotation Discovery*
*Context gathered: 2026-05-06*
