# Architecture: client-smoke-test

**Domain:** NeoForge/Forge client-side smoke testing framework
**Researched:** 2026-05-06
**Confidence:** HIGH

## Table of Contents

1. [Module Structure](#module-structure)
2. [Annotation Design](#annotation-design)
3. [State Machine Design](#state-machine-design)
4. [Dependency Flow](#dependency-flow)
5. [Annotation Scanning — Avoiding Class Loading](#annotation-scanning--avoiding-class-loading)
6. [Component Boundaries](#component-boundaries)
7. [Build Order](#build-order)
8. [Integration with Existing eyelib](#integration-with-existing-eyelib)
9. [Sources](#sources)

---

## Module Structure

### Decision: Two Gradle subprojects

| Subproject | Type | MC Dep | Plugin | Publish |
|---|---|---|---|---|
| `eyelib-clientsmoke-annotation` | Plain Java library | **None** | `java-library` | Maven artifact |
| `eyelib-clientsmoke` | NeoForge mod | Yes (Forge 1.20.1 userdev) | `net.neoforged.moddev.legacyforge` | Optional mod jar |

**Why two modules:**
The annotation must be usable at **compile time** by any mod that wants `@ClientSmoke`, without pulling in Minecraft or Forge classes. This mirrors the established eyelib pattern: `eyelib-molang` (plain Java, no MC) vs `eyelib-importer` (NeoForge mod). Two modules also enforce the `compileOnly` + `runtimeOnly` dependency pattern referenced in PROJECT.md.

**Internal package structure of `eyelib-clientsmoke`:**

```
io.github.tt432.clientsmoke/
├── annotation/          (re-export of the annotation for convenience)
├── scan/                (annotation discovery)
│   ├── SmokeTestScanner.java        — interface
│   └── ForgeModFileSmokeScanner.java — Forge ModFileScanData impl
├── model/
│   └── SmokeTestDescriptor.java     — discovered test metadata POJO
├── config/
│   └── ClientSmokeConfig.java       — NeoForge ModConfigSpec wrapper
├── runtime/
│   ├── ClientSmokeState.java        — state enum
│   ├── ClientSmokeStateMachine.java — tick-driven FSM (ClientTickEvent.Pre)
│   ├── ClientSmokeEngine.java       — orchestrator (test loop, world mgmt)
│   └── ClientSmokeMod.java          — @Mod entry + EventBusSubscriber
└── report/
    ├── ScreenshotCapture.java       — wraps Minecraft Screenshot.grab()
    └── SmokeReportGenerator.java    — HTML/text summary
```

---

## Annotation Design

### Definition

```java
// In: eyelib-clientsmoke-annotation/src/main/java/io/github/tt432/clientsmoke/annotation/ClientSmoke.java

package io.github.tt432.clientsmoke.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a client smoke test.
 *
 * <p>The annotated class must:
 * <ul>
 *   <li>Be a top-level class (not inner, not anonymous)</li>
 *   <li>Have a public no-arg constructor</li>
 *   <li>Implement {@code Runnable} OR define any method matching
 *       {@code void run*(ClientSmokeContext)} — the framework invokes
 *       {@code run()} by default, or discovers a named method via
 *       {@link #method()}.</li>
 * </ul>
 *
 * <p>Discovery is performed by Forge's {@code ModFileScanData} mechanism
 * (bytecode-level scanning, no class loading).  Class loading for the test
 * class only happens when the state machine reaches {@code TEST_EXEC}.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClientSmoke {

    /**
     * Execution order within the test suite.
     * Lower values run first.  Tests with equal priority run in
     * discovery order (non-deterministic; use {@link #dependsOn()}
     * for ordering guarantees).
     */
    int priority() default 0;

    /**
     * Human-readable description for the test report.
     * Optional — defaults to the simple class name.
     */
    String description() default "";

    /**
     * Extra ticks to wait after world stabilisation before taking
     * the screenshot.  -1 means use the global config default.
     *
     * <p>Useful when a test needs extra time for rendering to
     * settle (e.g. shader compilation, animation startup).
     */
    int delayTicks() default -1;

    /**
     * Name of the method to invoke on the test instance.
     *
     * <p>If empty (default), the framework calls the zero-arg
     * {@code run()} method (the test class should implement
     * {@code Runnable}).  If non-empty, the framework reflects
     * the named method and calls it with a single
     * {@code ClientSmokeContext} parameter.
     */
    String method() default "";

    /**
     * Names of test classes (as returned by
     * {@link SmokeTestDescriptor#name()}) that must complete
     * successfully before this test executes.
     *
     * <p>Used for sequencing (e.g. "setupWorld" → "testRender").
     * Circular dependencies cause the test to be skipped with a
     * warning.
     */
    String[] dependsOn() default {};
}
```

### Retention: Why `CLASS`

| Retention | Visible in .class? | Visible via reflection? | Discoverable by FML scanning? | Class loading risk |
|---|---|---|---|---|
| `SOURCE` | No | No | **No** — needs AP | Zero (but needs AP) |
| **`CLASS`** | **Yes** | **No** | **Yes** ✓ | **Zero** (bytecode scan) |
| `RUNTIME` | Yes | Yes | Yes | Medium (accidental getAnnotation) |

`CLASS` is the sweet spot:
- FML's `ModFileScanData` works at the bytecode level → discovers `CLASS` retention annotations
- Reflection-based `getAnnotation()` returns `null` → prevents accidental class loading through reflection
- No annotation processor infrastructure needed

### Evidence

The existing eyelib codebase uses exactly this pattern for two custom annotations already:
- `io.github.tt432.eyelibmolang.mapping.api.MolangMapping` → discovered via `ForgeMolangMappingDiscovery` (see `eyelib-molang` source)
- `RegisterParticleComponent` → discovered via `ParticleComponentManager.loadParticleComponents()`

Both use `ModList.get().getAllScanData()` + `ModFileScanData.AnnotationData.memberName()` to read annotation data **without loading the annotated class**. Class loading happens later, at a controlled moment, via explicit `Class.forName()`.

---

## State Machine Design

### Pattern origin: iris-tutorial-mod `TutorialClientHandler`

The reference project uses a flat set of `static boolean` flags driven by `ClientTickEvent.Pre`. Its flow:
```
autoJoinTriggered → worldDetected → IrisReloadComplete → reloadStabilized
→ [capture GIF frames] → primaryCaptureComplete → [capture presets] → done → exit
```

Our design formalises this into an explicit `enum`-based state machine that supports **multiple test methods** (the reference only ever runs one "chapter").

### States

```
                    config.enabled == false
INIT ──────────────────────────────────────────────────► IDLE (no-op)
  │                                                       
  │ config.enabled == true                                 
  ▼                                                       
CONFIG_LOAD                                               
  │                                                       
  │ config loaded                                          
  ▼                                                       
ANNOTATION_SCAN                                           
  │                                                       
  │ scan complete, tests found                             
  ▼                                                       
WORLD_PREP ──(world exists)──► WORLD_OPEN                 
  │                               │                       
  │ world created                 │ world opened           
  ▼                               ▼                       
WORLD_WAIT ◄──────────────────────┘                       
  │                                                       
  │ player != null && stabilised                           
  ▼                                                       
TEST_EXEC                                                 
  │                                                       
  │ test method returned                                   
  ▼                                                       
SCREENSHOT                                                
  │                                                       
  │ screenshot saved                                       
  ▼                                                       
NEXT_TEST ──(more tests)──► TEST_EXEC                     
  │                                                       
  │ (no more tests)                                       
  ▼                                                       
REPORT                                                    
  │                                                       
  │ report written                                        
  ▼                                                       
EXIT                                                       
  halt(0)                                                  
```

### State enum

```java
public enum ClientSmokeState {
    INIT,
    IDLE,
    CONFIG_LOAD,
    ANNOTATION_SCAN,
    WORLD_PREP,
    WORLD_OPEN,
    WORLD_WAIT,
    TEST_EXEC,
    SCREENSHOT,
    NEXT_TEST,
    REPORT,
    EXIT,
    ERROR   // terminal error state — log, report, exit
}
```

### State Machine Implementation

The state machine lives in `ClientSmokeStateMachine` (one class, single responsibility):

```java
@EventBusSubscriber(modid = ClientSmokeMod.MODID, value = Dist.CLIENT)
public class ClientSmokeStateMachine {
    private static ClientSmokeState state = ClientSmokeState.INIT;
    private static ClientSmokeEngine engine;
    private static long stabiliseTickStart = -1L;
    private static int currentTestIndex = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (state == ClientSmokeState.IDLE) return;  // short-circuit
        Minecraft mc = Minecraft.getInstance();

        try {
            switch (state) {
                case INIT -> transitionIfConfigEnabled();
                case CONFIG_LOAD -> loadConfig();
                case ANNOTATION_SCAN -> scanAndBuildQueue();
                case WORLD_PREP -> createOrOpenWorld(mc);
                case WORLD_OPEN -> openExistingWorld(mc);
                case WORLD_WAIT -> waitForStabilise(mc);
                case TEST_EXEC -> executeCurrentTest(mc);
                case SCREENSHOT -> takeScreenshot(mc);
                case NEXT_TEST -> advanceOrReport();
                case REPORT -> generateReport(mc);
                case EXIT -> halt();
            }
        } catch (Exception e) {
            LOGGER.error("State {} failed", state, e);
            state = ClientSmokeState.ERROR;
        }
    }
    // ... private helper methods
}
```

**Key design decisions:**

1. **Single tick handler** — one `@SubscribeEvent` method, not one per state. The `switch` is simple, debuggable, and avoids FML event bus overhead for 10+ event handlers.

2. **Static fields are OK here** — the state machine is naturally a singleton; there is only one client in a JVM. This matches the iris-tutorial-mod pattern exactly.

3. **ERROR state** — terminal state that still calls `generateReport()` so partial results are saved before `halt()`.

### State transition details

#### ANNOTATION_SCAN → WORLD_PREP

The annotation scanner queries `ModList.get().getAllScanData()`, filters for `@ClientSmoke`, builds a sorted `List<SmokeTestDescriptor>`. The descriptor holds:
- `className` (fully qualified, for `Class.forName()` later)
- `priority`, `description`, `delayTicks`, `method`, `dependsOn`
- `modId` (source mod, for reporting)
- All fields except `className` come from `ModFileScanData.AnnotationData.annotationData()` map — **no class loading**.

If no tests are found, transition directly to `IDLE` (non-error — a mod with the framework installed but no tests configured is valid).

#### WORLD_PREP / WORLD_OPEN

Follows iris-tutorial-mod pattern. Creates a fresh creative flat world or opens an existing one:
```java
String worldName = config.getWorldName();  // "ClientSmokeTest" by default
if (!mc.getLevelSource().levelExists(worldName)) {
    mc.createWorldOpenFlows().createFreshLevel(worldName, ...);
} else {
    mc.createWorldOpenFlows().openWorld(worldName, ...);
}
```

#### WORLD_WAIT → TEST_EXEC

Two conditions must be true:
1. `mc.player != null && mc.level != null` — world loaded
2. `mc.level.getGameTime() - stabiliseTickStart >= config.getStabiliseTicks()` — shaders/rendering stabilised

The stabilise delay is configurable (default 40 ticks, matching iris-tutorial-mod's `RELOAD_STABILIZE_TICKS`).

#### TEST_EXEC → SCREENSHOT

1. Pick the next `SmokeTestDescriptor` from the sorted queue
2. `Class<?> testClass = Class.forName(descriptor.className)` — **first and only** class load
3. `Object instance = testClass.getDeclaredConstructor().newInstance()`
4. Invoke the test method (either `Runnable.run()` or named method with `ClientSmokeContext`)
5. Return → transition to SCREENSHOT

The `ClientSmokeContext` object passed to test methods provides:
```java
public record ClientSmokeContext(Minecraft minecraft, ClientLevel level, LocalPlayer player) {}
```

This gives test methods access to the Minecraft client without reaching into statics.

#### SCREENSHOT → NEXT_TEST

1. Compute per-test delay: `descriptor.delayTicks() >= 0 ? descriptor.delayTicks() : config.getDefaultDelayTicks()`
2. Wait that many ticks after `TEST_EXEC` completed (can reuse the stabilise timer pattern)
3. Call `Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), callback)`
4. Record result in report model

Screenshot filenames follow the pattern:
`clientsmoke/{modId}/{testClassName}_{timestamp}.png`

#### EXIT

1. `mc.stop()` — clean client shutdown
2. Wait 3 seconds for async operations
3. `Runtime.getRuntime().halt(0)` — hard exit (necessary because some mods hold non-daemon threads)

---

## Dependency Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Test Mod (consumer)                      │
│  compileOnly project(':eyelib-clientsmoke-annotation')      │
│  runtimeOnly project(':eyelib-clientsmoke')                 │
│                                                              │
│  @ClientSmoke class MyTest implements Runnable { ... }       │
└──────────────┬───────────────────────────────┬──────────────┘
               │ compileOnly                    │ runtimeOnly
               ▼                                ▼
┌──────────────────────┐          ┌───────────────────────────┐
│ clientsmoke-annotation│  impl    │   clientsmoke-runtime      │
│                      │◄─────────│                            │
│ Plain Java library   │          │ NeoForge userdev mod       │
│ Zero dependencies    │          │                            │
│                      │          │ depends on:                │
│ @ClientSmoke         │          │  - clientsmoke-annotation  │
│                      │          │  - jdk-classfile-backport  │
└──────────────────────┘          │  - NeoForge/FML API        │
                                  │  - Minecraft client API    │
                                  └────────────────────────────┘
```

### Why `compileOnly` not `implementation` for test mods

If a test mod used `implementation project(':eyelib-clientsmoke')`, the runtime engine's classes would be on the compile classpath of the test mod → the test mod's bytecode could reference Minecraft classes through the engine → the FML class transformer would need to process them during mod loading → potential class loading conflicts.

With `compileOnly` for the annotation (lightweight, no MC deps) and `runtimeOnly` for the engine (not on compile classpath), the test mod's compilation is clean — it only sees `@ClientSmoke` and `ClientSmokeContext` (if it chooses to depend on the annotation module).

### Dependency matrix

| Module | What it depends on | Why |
|---|---|---|
| `eyelib-clientsmoke-annotation` | Nothing | Pure annotation, no runtime code |
| `eyelib-clientsmoke` | `eyelib-clientsmoke-annotation` | Needs `@ClientSmoke` class for `ModFileScanData` type comparison |
| `eyelib-clientsmoke` | `jdk-classfile-backport:24.0` | Optional — only if doing secondary class file scanning beyond FML's scan; FML scan alone is sufficient |
| `eyelib-clientsmoke` | NeoForge/FML API | `@Mod`, `@EventBusSubscriber`, `ModList`, `ModFileScanData`, `Dist.CLIENT` |
| `eyelib-clientsmoke` | Minecraft client API | `Minecraft`, `Screenshot`, `ClientLevel`, `LocalPlayer`, `ClientTickEvent` |
| Test mod | `eyelib-clientsmoke-annotation` (`compileOnly`) | Needs `@ClientSmoke` to compile |
| Test mod | `eyelib-clientsmoke` (`runtimeOnly`) | Engine runs alongside, never in compile classpath |

---

## Annotation Scanning — Avoiding Class Loading

### How it works

Forge/FML's `ModFileScanData` mechanism works as follows:

1. **During mod discovery** — FML scans every mod jar using ASM (bytecode analysis). It walks all `.class` files and records:
   - Class name (fully qualified, as a `String`)
   - All annotations on the class (type descriptor + member values)
   - This happens **without loading any class** — pure bytecode analysis.

2. **After mod construction** — The scan data is available via `ModList.get().getAllScanData()`.

3. **Our scanner** — At state `ANNOTATION_SCAN`, we query this data:
```java
Type annotationType = Type.getType(ClientSmoke.class);
for (ModFileScanData scanData : ModList.get().getAllScanData()) {
    for (ModFileScanData.AnnotationData ad : scanData.getAnnotations()) {
        if (Objects.equals(ad.annotationType(), annotationType)) {
            String className = ad.memberName();            // String, NOT a class
            Map<String, Object> data = ad.annotationData(); // annotation members
            descriptors.add(SmokeTestDescriptor.from(className, data));
        }
    }
}
```

4. **Class loading happens only at TEST_EXEC** — when the state machine is ready:
```java
Class<?> testClass = Class.forName(descriptor.className());
// ... instantiate and run
```

### Why this is safe

| Stage | What happens | Class loading |
|---|---|---|
| FML mod discovery | ASM scans bytecode for annotations | **None** |
| ANNOTATION_SCAN | Reads `ModFileScanData`, builds descriptor list from strings | **None** |
| TEST_EXEC | `Class.forName(descriptor.className())` | **Yes, controlled** |
| All other states | World creation, screenshots, etc. | MC classes only |

The critical property: **the test class is never referenced by name in any compiled code path except the explicit `Class.forName()` call**. There is no `import` of the test class, no static reference, no reflection-based access pattern — nothing that the JVM could trigger eager loading for.

### Contrast with alternatives

| Approach | Pros | Cons |
|---|---|---|
| **Annotation processor** | Zero runtime footprint, `SOURCE` retention | Complex AP setup, build system integration, generated files must be in test mod jar |
| **ServiceLoader** | Standard Java, no AP | Requires class instantiation during service loading (loads the class); test class needs `META-INF/services/` entry |
| **Classpath scan (reflection)** | Simple code | Loads all classes in the jar — exactly what we want to avoid |
| **Forge ModFileScanData** | **Zero class loading, already built into FML, proven in eyelib** | Requires `RUNTIME` or `CLASS` retention (not `SOURCE`); tied to Forge/NeoForge |

**Recommendation: Forge ModFileScanData** — it's already the eyelib project's standard pattern, requires zero additional infrastructure, and is proven in production code.

---

## Component Boundaries

### Component Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                      ClientSmokeMod                            │
│  (@Mod entry point, Dist.CLIENT)                               │
│                                                                 │
│  onCreate(IEventBus, ModContainer):                             │
│    1. Register ClientSmokeConfig (NeoForge config)              │
│    2. Set up ClientSmokeEngine singleton                        │
│                                                                 │
│  Owns: module lifecycle                                         │
└──────────────┬──────────────────────────────┬──────────────────┘
               │ creates                      │ creates
               ▼                              ▼
┌──────────────────────────┐    ┌──────────────────────────────┐
│ ClientSmokeStateMachine  │    │ ClientSmokeConfig            │
│                          │    │                              │
│ @EventBusSubscriber      │    │ NeoForge ModConfigSpec:      │
│ ClientTickEvent.Pre      │    │  - enabled (boolean)         │
│                          │    │  - worldName (string)        │
│ Drives FSM states        │    │  - defaultDelayTicks (int)   │
│ Delegates work to engine │    │  - stabiliseTicks (int)      │
│ Reads config             │    │  - screenshotDir (string)    │
│                          │    │  - exitAfterReport (boolean) │
│ Talks to: engine, config │    │  - reportFormat (enum)       │
└──────────┬───────────────┘    └──────────────────────────────┘
           │ delegates
           ▼
┌──────────────────────────────────────────────────────────────┐
│                   ClientSmokeEngine                           │
│                                                               │
│ Responsible for:                                              │
│  - Test queue management (sorted by priority + dependency)    │
│  - World creation/open orchestration                          │
│  - Test instance lifecycle (load → instantiate → invoke → destroy)
│  - Screenshot scheduling                                      │
│  - Report data aggregation                                    │
│                                                               │
│ Talks to: SmokeTestScanner, ScreenshotCapture,                │
│           SmokeReportGenerator, Minecraft API                 │
└──┬───────────────┬────────────────┬──────────────────────────┘
   │               │                │
   ▼               ▼                ▼
┌──────────┐ ┌─────────────┐ ┌──────────────────┐
│ Scanner  │ │ Screenshot  │ │ ReportGenerator  │
│          │ │ Capture     │ │                  │
│ Query    │ │             │ │ Aggregate:       │
│ ModList  │ │ Screenshot  │ │  - passed/failed │
│ → descr- │ │  .grab()    │ │  - screenshots   │
│  iptors  │ │ File naming │ │  - timing data   │
│          │ │ Timestamp   │ │  - errors         │
│ No class │ │ management  │ │                  │
│ loading  │ │             │ │ Output:          │
│          │ │ Talks to:   │ │  - HTML report   │
│          │ │ Minecraft   │ │  - JSON summary  │
│          │ │ client API  │ │  - text log      │
└──────────┘ └─────────────┘ └──────────────────┘
```

### Communication rules

| Component A | → Component B | What passes | Direction |
|---|---|---|---|
| `ClientSmokeMod` | `ClientSmokeStateMachine` | Config reference (constructor injection) | Unidirectional |
| `ClientSmokeMod` | `ClientSmokeEngine` | Creates and holds reference | Unidirectional |
| `ClientSmokeStateMachine` | `ClientSmokeEngine` | Method calls (`engine.startScan()`, `engine.executeTest()`, etc.) | Unidirectional |
| `ClientSmokeStateMachine` | `ClientSmokeConfig` | Read config values | Read-only |
| `ClientSmokeEngine` | `SmokeTestScanner` | Returns `List<SmokeTestDescriptor>` | Unidirectional |
| `ClientSmokeEngine` | `ScreenshotCapture` | Returns `Path` to saved screenshot | Unidirectional |
| `ClientSmokeEngine` | `SmokeReportGenerator` | Passes `List<SmokeTestResult>` | Unidirectional |
| `ScreenshotCapture` | Minecraft `Screenshot` | Calls `Screenshot.grab()` directly | Unidirectional |
| Test class (user code) | `ClientSmokeContext` | Receives context as method parameter | Injection |

**No component talks back to `ClientSmokeStateMachine`** — the FSM is the sole driver, all other components are passive services.

### Separation of concerns

| Concern | Owner | Why |
|---|---|---|
| When to transition states | `ClientSmokeStateMachine` | Single source of truth for execution timeline |
| How to discover tests | `SmokeTestScanner` | Encapsulate FML API access |
| How to run a test | `ClientSmokeEngine` | Test lifecycle, error handling, class loading |
| How to capture screenshots | `ScreenshotCapture` | File naming, directory creation, Minecraft API |
| How to format output | `SmokeReportGenerator` | HTML/JSON/text, no knowledge of MC |
| What the test does | User's test class | Framework only invokes, never interprets |

---

## Build Order

### Dependency order (must build in this sequence)

```
Step 1: eyelib-clientsmoke-annotation
        ↓ (Gradle project dependency)
Step 2: eyelib-clientsmoke
        ↓ (compileOnly + runtimeOnly)
Step 3: Test mods
```

### Step-by-step build plan

| Step | Module | To build | Gradle task |
|---|---|---|---|
| 1 | `eyelib-clientsmoke-annotation` | Pure Java library | `:eyelib-clientsmoke-annotation:build` |
| 2 | `eyelib-clientsmoke` | NeoForge mod | `:eyelib-clientsmoke:build` |
| 3 | Test mods | Consuming mods | Their own build |

### Adding to settings.gradle

```gradle
// In settings.gradle, ADD:
include("eyelib-clientsmoke-annotation")
include("eyelib-clientsmoke")
```

Position them after existing subprojects (alphabetical ordering or at end — either is fine).

### build.gradle for eyelib-clientsmoke-annotation

```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base { archivesName = 'eyelib-clientsmoke-annotation' }

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

// ZERO dependencies — pure annotation jar
dependencies {
    compileOnly 'org.jspecify:jspecify:1.0.0'
}

java { withSourcesJar() }

publishing {
    publications {
        mavenJava(MavenPublication) { from components.java }
    }
    repositories { mavenLocal() }
}
```

### build.gradle for eyelib-clientsmoke

```gradle
plugins {
    id 'java-library'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base { archivesName = 'eyelib-clientsmoke' }

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

legacyForge {
    version = project.minecraft_version + '-' + project.forge_version
    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }
    mods {
        clientsmoke { sourceSet(sourceSets.main) }
    }
}

dependencies {
    implementation project(':eyelib-clientsmoke-annotation')
    implementation 'io.github.dmlloyd:jdk-classfile-backport:24.0'
    compileOnly 'org.jspecify:jspecify:1.0.0'
}

tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
        minecraft_version_range: minecraft_version_range,
        forge_version_range    : forge_version_range,
        loader_version_range   : loader_version_range,
        mod_version            : mod_version,
        mod_license            : mod_license,
        mod_authors            : mod_authors,
        mod_description        : 'Client smoke testing framework for NeoForge mods — @ClientSmoke annotation-driven'
    ]
    inputs.properties(replaceProperties)
    filesMatching('META-INF/mods.toml') { expand(replaceProperties) }
}

java { withSourcesJar() }

publishing {
    publications {
        mavenJava(MavenPublication) { from components.java }
    }
    repositories { mavenLocal() }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

### What root build.gradle needs

The root project does **not** need to declare dependencies on either subproject. The subprojects are independent. Test mods that want the framework declare their own dependencies:

```gradle
// In a test mod's build.gradle:
dependencies {
    compileOnly 'io.github.tt432:eyelib-clientsmoke-annotation:21.1.14'
    runtimeOnly 'io.github.tt432:eyelib-clientsmoke:21.1.14+1.20.1-forge'
}
```

Or during development:

```gradle
compileOnly project(':eyelib-clientsmoke-annotation')
runtimeOnly project(':eyelib-clientsmoke')
```

---

## Integration with Existing eyelib

### Relationship to root module

`eyelib-clientsmoke` is **independent** of the root `eyelib` mod. The root mod does not depend on it, and it does not depend on the root mod. This is intentional:
- The smoke test framework is a tool, not a library feature
- It should be usable by mods that don't use eyelib at all
- Keeping them decoupled avoids dependency cycles

### Coexistence in the same Gradle build

Both subprojects coexist in the `settings.gradle` include list. They share:
- Java 17 toolchain
- NeoGradle version (via root `settings.gradle` pluginManagement)
- `gradle.properties` values (minecraft_version, forge_version, mod_version)

They do **not** share:
- Compile classpath
- Runtime classpath (unless a run configuration specifically includes both)
- Mod loading (they are separate mods with separate mod ids)

### Mod ID

Recommended mod id: `clientsmoke` (lowercase, fits the `[a-z][a-z0-9_]{1,63}` regex).

The `mods.toml` declares:
```toml
modId = "clientsmoke"
displayName = "Client Smoke Test Framework"
description = "@ClientSmoke annotation-driven client testing for NeoForge mods"
```

---

## Sources

| Source | Topic | Confidence |
|---|---|---|
| iris-tutorial-mod `TutorialClientHandler.java` — state machine pattern | State machine design, world creation, screenshot capture, exit sequence | HIGH (direct reference code) |
| iris-tutorial-mod `IrisShaderTutorial.java` — `@Mod(dist = Dist.CLIENT)` entry | Mod entry pattern, `compileOnly + localRuntime` dependency model | HIGH (direct reference code) |
| iris-tutorial-mod `TutorialConfig.java` — `ModConfigSpec` | Configuration pattern | HIGH (direct reference code) |
| eyelib `ForgeMolangMappingDiscovery.java` — uses `ModFileScanData` for custom annotation discovery | Annotation scanning without class loading | HIGH (existing code in this project) |
| eyelib `ParticleComponentManager.java` — uses `ModFileScanData` + deferred `Class.forName()` | Same pattern, validates approach | HIGH (existing code in this project) |
| eyelib `build.gradle` — `net.neoforged.moddev.legacyforge`, `localRuntime` config, `jdk-classfile-backport` dependency | Build infrastructure, available libraries | HIGH (existing build files) |
| Forge docs — GameTest framework (`@GameTest`, `@GameTestHolder`, `RegisterGameTestsEvent`) | Reference test framework for annotation patterns | HIGH (official docs, fetched 2026-05-06) |
| eyelib `MODULES.md` — module inventory and dependency rules | Repository module conventions | HIGH (canonical project documentation) |

---

## Open Questions (for Phase 7 — Design)

1. **Single test class vs test class + per-method annotation:** Should `@ClientSmoke` be on the class level only (current design) or also support method-level annotations (`@ClientSmokeTest`)? Method-level would allow multiple test scenarios per class but complicates discovery and lifecycle.

2. **World reuse between tests:** Should each test get a fresh world, or should tests share the same world? Sharing is faster but risks test pollution. Recommendation: default to fresh world per test, with a `@ClientSmoke(reuseWorld = true)` opt-in.

3. **Report output directory:** The iris-tutorial-mod writes to `screenshots/`. Should the smoke framework write to `clientsmoke-reports/` under the game directory, or somewhere configurable? Recommendation: `gameDir/clientsmoke-reports/{timestamp}/` with config override.

4. **Parallel test execution:** The current design is sequential (one test at a time). Could be parallelised in future but adds complexity in Minecraft's single-threaded client model.

5. **IDE run configuration:** Whether to provide a dedicated `runClientSmoke` Gradle task or rely on the standard `runClient` with config-driven enable.
