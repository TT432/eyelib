# Technology Stack: client-smoke-test

**Project:** NeoForge client-side smoke testing mod (subproject of eyelib)
**Researched:** 2026-05-06
**Overall confidence:** HIGH

---

## Critical Platform Discrepancy

**The current eyelib repository is on Minecraft 1.20.1 + Forge 47.1.3 + Java 17** (see `gradle.properties`: `minecraft_version=1.20.1`, `forge_version=47.1.3`, plugin `net.neoforged.moddev.legacyforge`). **PROJECT.md specifies NeoForge 1.21.1 + Java 21.** This STACK.md covers the NeoForge 1.21.1 stack as specified — the subproject must either:

1. **Option A (recommended):** Be an independent NeoForge 1.21.1 mod with its own Gradle build, consumed by root via `compileOnly`/`localRuntime` at dev time only.
2. **Option B:** Become part of a full eyelib migration to NeoForge 1.21.1 (scope expands beyond this subproject).

---

## Recommended Stack

### 1. NeoForge 1.21.1 Build Essentials

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| `net.neoforged.moddev` (ModDevGradle) | `2.0.141` | Gradle plugin for NeoForge mod development | **Current and maintained** successor to `net.neoforged.gradle` (NeoGradle legacy). The MDK template at `NeoForgeMDKs/MDK-1.21-ModDevGradle` uses this exact version. Supports NeoForge versions `>= 21.0.x`. NeoGradle legacy (`net.neoforged.gradle`) is in maintenance mode and should not be used for new projects. | HIGH — verified on GitHub: [MDK-1.21-ModDevGradle build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21-ModDevGradle/blob/main/build.gradle) |
| NeoForge | `21.1.x` | Mod loader for Minecraft 1.21.1 | NeoForge 1.21.1 maps to NeoForge version `21.1.xxx` (e.g., `21.1.0`). See [NeoForge project listing](https://projects.neoforged.net/neoforged/neoforge) for exact latest. The MDK uses property `neo_version` for this value. | HIGH — official NeoForge versioning convention |
| Java | **21** (not 17) | Compilation and runtime JDK | Minecraft 1.21+ ships Java 21 to end users. The MDK template sets `java.toolchain.languageVersion = JavaLanguageVersion.of(21)`. **Java 17 is incompatible with Minecraft 1.21.x.** | HIGH — [MDK-1.21-ModDevGradle build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21-ModDevGradle/blob/main/build.gradle) + [NeoForge 1.21 docs](https://docs.neoforged.net/docs/1.21.1/gettingstarted/) |
| Gradle | `>= 8.8` | Build system | ModDevGradle README states "compatible with Gradle 8.8". Current eyelib uses Gradle 8.x (wrapper). | HIGH — ModDevGradle README |
| Parchment mappings | `2024.07.28` (for 1.21) | Human-readable parameter names in Minecraft source | ParchmentMC provides community-maintained mappings. For NeoForge 1.21.1, check [ParchmentMC](https://parchmentmc.org/docs/getting-started) for the latest 1.21 release. | MEDIUM — version may need verification against ParchmentMC |

### 1a. NeoGradle (legacy) vs ModDevGradle

| Criterion | ModDevGradle (`net.neoforged.moddev`) | NeoGradle legacy (`net.neoforged.gradle`) |
|-----------|---------------------------------------|------------------------------------------|
| Status | **Active, supported** | Maintenance mode |
| First release | 2024 Q2 (post NeoForge fork) | ~2022 |
| Configuration style | `neoForge { version = "..." }` | `neoForge { version = "..." }` (similar but different plugin ID) |
| Gradle config cache | Supported | Partial |
| Latest version seen | `2.0.141` (May 2026) | N/A — legacy |
| Recommendation | **Use this** | Do not use for new projects |

---

### 2. Annotation-Based Class Discovery

The core architectural challenge: **discover `@ClientSmoke`-annotated test classes without triggering class loading.** Loading test target classes prematurely can cascade `ClassNotFoundException` due to shared dependencies in the NeoForge classpath.

#### Recommended: ASM-based Class File Scanning via `ModFileScanData`

**Approach:** Use NeoForge's `ModFileScanData` infrastructure (the same mechanism that discovers `@EventBusSubscriber` and `@Mod` annotations) to scan `.class` file bytecode without triggering JVM class initialization.

**How it works:**
1. During mod construction (`FMLConstructModEvent` or constructor), submit an `IModFileScanData` scan request.
2. NeoForge's `ModFileScanData` uses ASM (`org.objectweb.asm`) to read `.class` file bytecodes.
3. Scan looks for the `@ClientSmoke` annotation descriptor in the constant pool of each `.class` file — **no class loading occurs.**
4. Results are collected as `ClassData` objects with class names, ready for deferred instantiation only after the world is loaded.

**Reference implementation pattern:** NeoForge's own `@EventBusSubscriber` discovery in `net.neoforged.fml.javafmlmod.AutomaticEventSubscriber` uses `ModFileScanData.getAnnotations()` looking for `@EventBusSubscriber`.

**Key APIs:**
- `net.neoforged.neoforgespi.language.IModFileScanData` — the scan data interface
- `net.neoforged.fml.ModContainer.getModFileScanData()` — accessor within mod lifecycle
- `org.objectweb.asm.ClassReader` / `Type.getDescriptor()` — for custom scan predicates if needed

#### What NOT to use:

| Approach | Why Avoid |
|----------|-----------|
| `ServiceLoader` / `META-INF/services` | Requires class loading of service implementations, defeating the purpose. |
| `Class.forName()` / reflective scan | Triggers static initializers on test target classes. **Class loading cascade risk.** |
| `Reflections` library (`org.reflections`) | Runtime classpath scanner that loads classes. Same problem. |
| Annotation processor + SPI file | Technically works (compile-time scan → JSON registry → runtime read), but adds unnecessary build complexity for v1. Only consider if ASM approach proves insufficient. |

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| ASM | `9.x` (bundled with NeoForge) | Bytecode-level `.class` file reading | Already on NeoForge's classpath; used internally for `@EventBusSubscriber` discovery. No extra dependency needed. |
| `ModFileScanData` | NeoForge SPI | Annotation scanning API | NeoForge's own infrastructure; zero-class-loading `.class` file scanning. |

**Confidence: HIGH** — verified by examining iris-tutorial-mod's use of `@EventBusSubscriber` which relies on the same NeoForge internal scanning infrastructure.

---

### 3. Screenshot Capture

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| `net.minecraft.client.Screenshot` | Minecraft 1.21.1 | Framebuffer → PNG file | Vanilla Minecraft's built-in screenshot utility. The iris-tutorial-mod uses `Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), t -> {})`. It reads the OpenGL framebuffer via `glReadPixels`, encodes to PNG via `NativeImage`, and writes to disk asynchronously. | HIGH — verified in [iris-tutorial-mod TutorialClientHandler.java](file:///E:/____脚本/图形学教学/iris-tutorial-mod/src/main/java/net/irisshaders/tutorial/TutorialClientHandler.java) line 142 |
| `Minecraft.getMainRenderTarget()` | — | Obtain framebuffer to capture | Returns the main `MainTarget` (a `RenderTarget` wrapping the game's default FBO). This is the standard capture target for game screenshots. | HIGH — verified in iris-tutorial-mod |

**Key API signature:**
```java
// net.minecraft.client.Screenshot
public static void grab(
    File gameDirectory,
    String screenshotName,
    MainTarget target,          // framebuffer to capture
    Consumer<Component> handler  // callback for chat message (no-op for auto)
)
```

**Calling context:** Must be called during or after the render phase on the render thread. The iris-tutorial-mod calls it from `ClientTickEvent.Pre` after confirming world load and shader stabilization. For the smoke test framework, call from the same `ClientTickEvent.Pre` tick-based state machine after a configurable `SCREENSHOT_DELAY` post-world-load.

**Output location:** `mc.gameDirectory/screenshots/<name>.png`

**Limitations:**
- Requires an active OpenGL context (game window must be running).
- Captures the entire game window (including GUI unless hidden via `mc.options.hideGui = true`).
- For GUI-free captures, set `mc.options.hideGui = true` before calling `Screenshot.grab()`.

---

### 4. Programmatic World Creation and Teardown

#### Recommended: `WorldOpenFlows` API

| API | Method | Purpose |
|-----|--------|---------|
| `Minecraft.createWorldOpenFlows()` | Returns `WorldOpenFlows` instance | Entry point for programmatic world creation |
| `WorldOpenFlows.createFreshLevel(...)` | Creates **new** world and enters it | For first-run or disposable test worlds |
| `WorldOpenFlows.openWorld(...)` | Opens **existing** world | For reusing a shared test world across runs |

**Signature from iris-tutorial-mod (Minecraft 1.21.1):**
```java
mc.createWorldOpenFlows().createFreshLevel(
    worldName,                                    // String — unique world name
    new LevelSettings(
        worldName,                                // String
        GameType.CREATIVE,                        // creative mode (no hunger, fly)
        false,                                    // hardcore = false
        Difficulty.NORMAL,                        // peaceful/normal
        true,                                     // allowCommands
        new GameRules(),                          // default game rules
        WorldDataConfiguration.DEFAULT            // default data packs
    ),
    WorldOptions.defaultWithRandomSeed(),         // random seed
    WorldPresets::createNormalWorldDimensions,    // normal world gen
    new TitleScreen()                             // screen to show on cancel
);
```

**For a flat world** (simpler, faster generation, ideal for smoke testing):
```java
WorldPresets::createFlatWorldDimensions  // instead of createNormalWorldDimensions
```

Or for the simplest possible flat world with only one layer:
```java
mc.createWorldOpenFlows().createFreshLevel(
    name,
    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
        new GameRules(), WorldDataConfiguration.DEFAULT),
    WorldOptions.defaultWithRandomSeed(),
    registryAccess -> WorldPresets.createFlatWorld(registryAccess,
        new WorldGenSettings.GeneratorOptions(
            false, false, false,
            FlatLevelGeneratorSettings.getDefault(
                registryAccess.lookupOrThrow(Registries.BIOME),
                registryAccess.lookupOrThrow(Registries.STRUCTURE_SET),
                registryAccess.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
            )
        )
    ),
    new TitleScreen()
);
```

**Teardown:** No explicit world teardown is needed for v1. The auto-exit mechanism (see below) terminates the entire JVM process. World data persists in `runs/client/saves/` and reuse is handled by checking `mc.getLevelSource().levelExists(name)`.

**Confidence: HIGH** — verified in [iris-tutorial-mod TutorialClientHandler.java](file:///E:/____脚本/图形学教学/iris-tutorial-mod/src/main/java/net/irisshaders/tutorial/TutorialClientHandler.java) lines 70-80. The iris-tutorial-mod uses `WorldPresets::createNormalWorldDimensions` for its test worlds.

---

### 5. Auto-Exit Mechanisms

#### Recommended: Two-Phase Graceful-then-Force Exit (iris-tutorial-mod pattern)

| Phase | Method | Purpose | Delay |
|-------|--------|---------|-------|
| 1 (Graceful) | `Minecraft.getInstance().stop()` | Triggers resource cleanup, saves config, closes network connections | 3s after completion |
| 2 (Force) | `Runtime.getRuntime().halt(0)` | Force-kills JVM with exit code 0, bypasses hanging shutdown hooks | After Phase 1 |

**Implementation pattern (verified from iris-tutorial-mod):**
```java
private static void exitAfterCapture(Minecraft mc) {
    if (TutorialConfig.EXIT_AFTER_SCREENSHOT.get()) {
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            mc.stop();
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Runtime.getRuntime().halt(0);
        }, "Exit").start();
    }
}
```

**Why this pattern:**
- `mc.stop()` handles graceful shutdown — saves world data, disconnects from integrated server, closes resources.
- `Runtime.getRuntime().halt(0)` is a **hard exit** that does NOT run shutdown hooks. This is intentional: Java shutdown hooks can hang indefinitely in a modded environment (rendering threads, network threads).
- The 5s initial delay ensures the last screenshot has finished writing to disk.
- The 3s delay after `mc.stop()` gives the integrated server time to clean up.

#### What NOT to use:

| Approach | Why Avoid |
|----------|-----------|
| `System.exit(0)` | Runs JVM shutdown hooks, which may hang. Both NeoForge/Forge mod loaders and Minecraft rendering have registered hooks that can deadlock or take minutes. |
| `Runtime.getRuntime().halt(0)` alone (without `mc.stop()`) | Skips world save; test world may be corrupted for reuse. Also skips config persistence. |
| `mc.stop()` alone (without `halt()`) | In a modded environment, `stop()` rarely completes fully — render thread keeps the JVM alive. |
| `Minecraft.destroy()` | Deprecated/removed in modern Minecraft; internal API. |

**Confidence: HIGH** — verified in [iris-tutorial-mod TutorialClientHandler.java](file:///E:/____脚本/图形学教学/iris-tutorial-mod/src/main/java/net/irisshaders/tutorial/TutorialClientHandler.java) lines 548-557. This is the proven exit pattern.

---

### 6. Build Tooling

#### Gradle Plugins

| Plugin | Version | Purpose | Why |
|--------|---------|---------|-----|
| `net.neoforged.moddev` | `2.0.141` | NeoForge mod development | Core plugin providing userdev, runs, jarJar, mod metadata |
| `java-library` | Gradle built-in | Java compilation | Standard Java library plugin |
| `io.freefair.lombok` | `8.6` (match root) | Lombok annotation processing | Already used in eyelib root and all subprojects |
| `maven-publish` | Gradle built-in | Artifact publication | For publishing to local Maven for root consumption |
| `idea` | Gradle built-in | IntelliJ IDEA integration | Required explicitly as of ModDevGradle 2.x (see BREAKING_CHANGES) |

#### Dependency Configurations

| Configuration | Purpose | Example Use |
|---------------|---------|-------------|
| `modImplementation` | Core mod dependency (compiled + runtime, marked as mod) | `modImplementation project(':client-smoke-test')` in root |
| `compileOnly` | Compile-time only, not at runtime | Annotation definition shared between smoke-test and test targets |
| `localRuntime` | Runtime-only, not published | Root's `localRuntime` includes smoke-test for dev runs |
| `additionalRuntimeClasspath` | Runtime classpath injection for dev | Non-mod libraries needed at dev runtime |
| `jarJar` | Embed dependency in JAR | External libraries bundled with mod |

#### Subproject Structure

```groovy
// client-smoke-test/build.gradle
plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.141'
    id 'io.freefair.lombok' version '8.6'
    id 'idea'
}

version = rootProject.version
group = rootProject.group

base { archivesName = 'client-smoke-test' }

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version  // 21.1.x
    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', 'client_smoke_test'
        }
    }
    mods {
        "client_smoke_test" {
            sourceSet sourceSets.main
        }
    }
}

dependencies {
    // The annotation module (shared with test targets)
    // No direct dependency on root eyelib — smoke test is self-contained
    compileOnly 'org.jspecify:jspecify:1.0.0'
}
```

**Note on cross-project dependency:** The `client-smoke-test` subproject should NOT have compile-time access to the root eyelib module. It only needs:
- NeoForge (for `Dist.CLIENT`, event bus, etc.)
- Minecraft client classes (via userdev)
- Its own `@ClientSmoke` annotation definition (self-contained)
- Test target mods at `localRuntime` level (not `modImplementation`)

Root eyelib consumes `client-smoke-test` via:
```groovy
// Root build.gradle
configurations { localRuntime }
dependencies {
    compileOnly project(':client-smoke-test')  // Annotation visible at compile time
    localRuntime project(':client-smoke-test') // Full mod loaded at dev runtime
}
```

**Confidence: HIGH** — pattern from existing eyelib `compileOnly` + `localRuntime` dependency management (see root `build.gradle` line 132-138) and iris-tutorial-mod's `compileOnly + localRuntime` pattern.

---

### 7. What NOT to Use (and Why)

| Anti-Recommendation | Why Avoid | What to Use Instead |
|---------------------|-----------|---------------------|
| **`net.neoforged.gradle` (NeoGradle legacy)** | Maintenance mode, deprecated features. The 1.21.1 NeoForge docs reference NeoGradle but the newer ModDevGradle is the active plugin. | `net.neoforged.moddev` version `2.0.141` |
| **`net.neoforged.moddev.legacyforge`** | This is for **Forge 1.20.1** with NeoForge's build tooling (what eyelib currently uses). It does not support NeoForge 1.21.1. | `net.neoforged.moddev` (non-legacy) |
| **Java 17 for NeoForge 1.21.1** | Minecraft 1.21.x ships Java 21. The MDK explicitly sets `JavaLanguageVersion.of(21)`. | Java 21 |
| **`ServiceLoader` / `META-INF/services`** | Requires class loading of implementations. Would trigger premature loading of test target classes. | ASM-based `ModFileScanData` bytecode scanning |
| **`Reflections` or `ClassGraph`** | Runtime classpath scanners that load every class they find. **Guaranteed to cause class loading cascade.** | ASM-based `ModFileScanData` |
| **`System.exit(0)`** | Runs JVM shutdown hooks which hang in modded Minecraft. | `Runtime.getRuntime().halt(0)` after `mc.stop()` |
| **`Minecraft.getMinecraft()`** | Old MCP name for `Minecraft.getInstance()`. Not available in 1.21.1 mappings. | `Minecraft.getInstance()` |
| **Gradle 7.x** | ModDevGradle requires Gradle 8.8+. The MDK template specifies this. | Gradle 8.8+ (the default in MDK) |
| **`net.neoforged.fml.common.Mod` as client-only entrypoint without `dist = Dist.CLIENT`** | Can cause `ClassNotFoundException` on dedicated servers when Minecraft client classes are referenced. | `@Mod(value = "modid", dist = Dist.CLIENT)` |
| **Hardcoded world paths** | Different Gradle run configurations use different run directories. | Use `mc.gameDirectory` and `mc.getLevelSource()` |
| **Calling `Screenshot.grab()` from any thread** | Must be called on the render thread (OpenGL context required). | Call from `ClientTickEvent.Pre` or `RenderLevelStageEvent` |

---

### Installation

```bash
# In client-smoke-test/build.gradle
plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.141'
    id 'io.freefair.lombok' version '8.6'
}

# Root settings.gradle
include 'client-smoke-test'

# Root build.gradle
dependencies {
    compileOnly project(':client-smoke-test')
    localRuntime project(':client-smoke-test')
}
```

---

## Sources

### HIGH Confidence Sources
1. **[iris-tutorial-mod source code](file:///E:/____脚本/图形学教学/iris-tutorial-mod/src/main/java/)** — `TutorialClientHandler.java`, `TutorialConfig.java`, `IrisShaderTutorial.java` — Verifies the state machine pattern, world creation via `WorldOpenFlows`, screenshot via `Screenshot.grab()`, and two-phase exit (`mc.stop()` → `Runtime.halt(0)`).
2. **[MDK-1.21-ModDevGradle build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21-ModDevGradle/blob/main/build.gradle)** — Verifies plugin version `2.0.141`, Java 21, and NeoForge run configuration format.
3. **[ModDevGradle README](https://github.com/neoforged/ModDevGradle)** — Verifies plugin features, configuration options, and Gradle 8.8+ compatibility.
4. **[ModDevGradle BREAKING_CHANGES.md](https://github.com/neoforged/ModDevGradle/blob/main/BREAKING_CHANGES.md)** — Verifies ModDevGradle 2.x breaking changes.

### MEDIUM Confidence Sources
5. **[NeoForge 1.21.1 Documentation](https://docs.neoforged.net/docs/1.21.1/)** — Getting started, events, mod files, game tests. The 1.21.1 docs are marked "no longer actively maintained" but remain the canonical reference for that version.
6. **[NeoForge 1.21.1 Events Documentation](https://docs.neoforged.net/docs/1.21.1/concepts/events)** — Verifies `@EventBusSubscriber`, `@SubscribeEvent`, event bus model, and `Dist.CLIENT`.

### LOW Confidence / Gaps
7. **`ModFileScanData` API for custom annotation scanning** — The approach is sound (NeoForge uses it internally for `@EventBusSubscriber`) but the exact API surface for programmatic scan submission needs phase-specific research. The mechanism exists and works; exact invocation API may vary slightly by NeoForge version.
8. **Flat world generation via `WorldPresets.createFlatWorld`** — The API signature was derived from Minecraft 1.21.1 decompiled source patterns. The exact parameters for `WorldGenSettings.GeneratorOptions` constructor may need verification at implementation time.
9. **`WorldOpenFlows.createFreshLevel` with `WorldPresets::createFlatWorldDimensions`** — The `WorldPresets` overload that accepts a `RegistryAccess` lambda needs implementation-phase verification.

---

## Open Questions for Phase-Specific Research

1. **`ModFileScanData` programmatic API:** What is the exact invocation pattern for submitting custom annotation scan queries? Which lifecycle event is the earliest safe point to perform the scan? (Probable answer: `FMLConstructModEvent` on the mod bus, using `ModContainer.getModFileScanData()`).
2. **Flat world preset registration:** Can `WorldPresets.FLAT` be used directly with `createFreshWorldDimensions`, or does it require a separate `FlatLevelGeneratorPreset` registration?
3. **Test method invocation:** After discovering `@ClientSmoke`-annotated classes via ASM scanning, at what point should the framework call `Class.forName()` on them? (Probable answer: After `worldDetected` flag is true in the state machine, before screenshot capture).
4. **Gradle subproject isolation:** If `client-smoke-test` is a NeoForge 1.21.1 subproject but root is Forge 1.20.1, they cannot share the same Gradle classpath or build configuration. The subproject would need to reference different Gradle properties (`neo_version` vs `forge_version`, `parchment_minecraft_version` for 1.21 vs 1.20.1).

---

*Last updated: 2026-05-06 — STACK.md research phase*
