# Architecture Patterns — ClientSmoke v1.1 Gradle Automation

**Domain:** Gradle task automation layer over Minecraft mod smoke testing  
**Researched:** 2026-05-08  
**Confidence:** HIGH

## Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        DEVELOPER / CI                           │
│                                                                 │
│  ./gradlew runClientSmoke                                       │
│  ./gradlew runClientSmoke -Pclientsmoke.timeoutMinutes=5        │
│  ./gradlew runClientSmoke -Dclientsmoke.testName=LoginScreen    │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GRADLE PROCESS (root project)                 │
│                                                                 │
│  legacyForge.runs.clientSmoke {                                 │
│      client()                                                   │
│      systemProperty 'clientsmoke.enabled', 'true'               │
│      systemProperty 'clientsmoke.exitAfterSmoke', 'true'        │
│  }                                                              │
│                                                                 │
│  MDGL resolves:                                                 │
│  • Minecraft + Forge jars (mapped to parchment names)           │
│  • All subproject classpaths                                    │
│  • AT files, mixin configs                                      │
│  • Launches forked JVM with -D flags                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │ fork
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                MINECRAFT JVM (forked process)                    │
│                                                                 │
│  System.getProperty("clientsmoke.enabled") → "true"             │
│  System.getProperty("clientsmoke.exitAfterSmoke") → "true"      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ClientSmokeMod (@Mod constructor)                        │   │
│  │   ClientSmokeConfig.isEnabled() → reads system property  │   │
│  │   ClientSmokeScanner.scan() → discovers @ClientSmoke     │   │
│  │   → passes results to state machine                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ClientSmokeStateMachine (tick-driven)                    │   │
│  │   INIT → CONFIG_LOAD → SCAN → WORLD_CREATE →            │   │
│  │   WORLD_WAIT → STABILIZE → TEST_EXEC →                  │   │
│  │   REPOSITION → HUD_HIDE → SCREENSHOT → REPORT → EXIT    │   │
│  │                                                          │   │
│  │   handleExit():                                          │   │
│  │     reads testResults → determines exit code             │   │
│  │     Runtime.getRuntime().halt(pass ? 0 : 1)              │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────────┘
                               │ JVM exit code
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GRADLE PROCESS (result)                       │
│                                                                 │
│  exit code 0 → BUILD SUCCESSFUL                                 │
│  exit code 1 → BUILD FAILED (test failures)                     │
│  exit code 124 → timeout (if D2 implemented)                    │
└─────────────────────────────────────────────────────────────────┘
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `legacyForge.runs.clientSmoke` (root `build.gradle`) | Declares run configuration: JVM args, system properties, loaded mods, IDE name | MDGL plugin (via DSL) |
| MDGL `legacyForge` plugin (v2.0.91) | Resolves Minecraft/Forge classpath, launches forked JVM, generates IDE run configs | Gradle → forked JVM process |
| `ClientSmokeConfig` (modified v1.1) | Single source of truth for enabled/exit decisions. Checks system property first, falls back to ForgeConfigSpec | Read by `ClientSmokeMod`, `ClientSmokeStateMachine` |
| `ClientSmokeMod` (@Mod constructor) | unchanged v1.0 — calls `isEnabled()`, runs scanner, feeds state machine | `ClientSmokeScanner`, `ClientSmokeStateMachine` |
| `ClientSmokeStateMachine` (unchanged core, modified `handleExit`) | Tick-driven lifecycle. Modified: `handleExit()` reads aggregated test results to determine `halt()` exit code | Render thread (screenshot), test classes (via `Class.forName`) |
| `TestResult` record (unchanged) | Immutable record of each test execution: class name, status, duration, error | Written by `handleTestExec()`, read by `handleReport()` and `handleExit()` |
| Output filesystem | Screenshots (PNG) + JSON report + JUnit XML (v1.1 new) | Written by state machine, consumed by developer/CI |

### Data Flow

```
1. Gradle property / CLI arg (-Dclientsmoke.enabled=true)
                    │
2. MDGL systemProperty()  ──────►  JVM -D flag
                    │
3. ClientSmokeConfig.isEnabled() reads System.getProperty()
                    │
4. ClientSmokeMod constructor checks isEnabled() → proceeds
                    │
5. ClientSmokeScanner.scan() → List<DiscoveredTest>
                    │
6. State machine tick loop → executes tests, captures screenshots
                    │
7. handleExit() aggregates testResults → Runtime.halt(exitCode)
                    │
8. JVM exit code → Gradle reports BUILD SUCCESSFUL / BUILD FAILED
```

## Patterns to Follow

### Pattern 1: System Property Override (Config Resolution Chain)

**What:** A static method that checks JVM system property first, then falls back to ForgeConfigSpec. This is the bridge pattern between the Gradle task (which sets `-D` flags) and the Minecraft runtime (which reads them).

**When:** Whenever a config value needs to be overridden by the Gradle task without modifying persistent TOML files.

**Why this pattern:**
- **Temporary** — system properties die with the JVM process. No cleanup needed.
- **Transparent** — the override is visible in the JVM args (`jps -v` or task output).
- **Backward compatible** — when no system property exists, existing TOML behavior is preserved.
- **Testable** — can set system properties in unit tests without a running Minecraft instance.

**Example:**
```java
// ClientSmokeConfig.java (v1.1 addition)
public final class ClientSmokeConfig {
    
    private static final String PROP_PREFIX = "clientsmoke.";
    
    /**
     * Returns true if smoke testing should be enabled.
     * Resolution order: system property → ForgeConfigSpec TOML.
     */
    public static boolean isEnabled() {
        String override = System.getProperty(PROP_PREFIX + "enabled");
        if (override != null) {
            return Boolean.parseBoolean(override);
        }
        return ENABLED.get();  // ForgeConfigSpec fallback
    }
    
    /**
     * Returns true if the client should auto-exit after tests complete.
     */
    public static boolean shouldExitAfterSmoke() {
        String override = System.getProperty(PROP_PREFIX + "exitAfterSmoke");
        if (override != null) {
            return Boolean.parseBoolean(override);
        }
        return EXIT_AFTER_SMOKE.get();
    }
}
```

**Callers to update (v1.1 migration):**
- `ClientSmokeMod` constructor: `ClientSmokeConfig.ENABLED.get()` → `ClientSmokeConfig.isEnabled()`
- `ClientSmokeStateMachine.handleInit()`: same change
- `ClientSmokeStateMachine.handleExit()`: `EXIT_AFTER_SMOKE.get()` → `shouldExitAfterSmoke()`

### Pattern 2: MDGL Run Config Isolation

**What:** Use separate `legacyForge.runs` entries for different execution modes (normal dev vs smoke testing). Each run has its own system properties, loaded mods, and IDE name. They share the same MDGL infrastructure but are independent.

**When:** Adding any new Minecraft launch mode that differs in configuration from the standard `runClient`.

**Why this pattern:**
- MDGL auto-generates Gradle tasks and IDE run configs from each run entry
- No risk of cross-contamination between development and testing modes
- Each run can have different logging levels, system properties, and mod sets

**Example:**
```groovy
legacyForge {
    runs {
        // Development mode: no smoke test
        client {
            client()
            ideName = "Run Client"
        }
        
        // Smoke test mode: force-enabled smoke testing
        clientSmoke {
            client()
            ideName = "Run Client Smoke Tests"
            systemProperty 'clientsmoke.enabled', 'true'
            systemProperty 'clientsmoke.exitAfterSmoke', 'true'
        }
    }
}
```

### Pattern 3: Immutable Result Accumulation → Exit Code

**What:** The state machine accumulates `TestResult` records in an immutable list. At EXIT time, `handleExit()` performs a single reduction over the list to determine the aggregate exit code. No mutable "hasFailures" boolean that can get out of sync.

**When:** Converting accumulated test results into a process exit code.

**Why this pattern:**
- The `testResults` list is the single source of truth
- No risk of a status flag being set correctly for pass but missed for fail
- Easy to extend (e.g., count warnings separately)

**Example:**
```java
private static void handleExit() {
    if (!ClientSmokeConfig.shouldExitAfterSmoke()) {
        transitionTo(ClientSmokeState.IDLE, "exitAfterSmoke=false");
        return;
    }
    
    // ... existing mc.stop() and countdown logic ...
    
    if (elapsedTicks >= 60) {
        // Determine exit code from aggregated results
        boolean allPassed = testResults.stream()
                .allMatch(r -> "passed".equals(r.status()));
        int exitCode = allPassed ? 0 : 1;
        
        LOGGER.info("[ClientSmoke] Exit complete — halting JVM with code {}", exitCode);
        Runtime.getRuntime().halt(exitCode);
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Gradle Task Mutates Persistent Config

**What:** A Gradle task that writes to `config/clientsmoke-common.toml` or `gradle.properties` before launching Minecraft, then restores the original values afterward.

**Why bad:**
- Race conditions with concurrent Gradle invocations
- Git status pollution (`gradle.properties` is tracked)
- If Gradle is killed mid-run, the config stays in "enabled" state → next `runClient` unexpectedly activates smoke tests
- Complex cleanup logic for error recovery

**Instead:** Use system properties (Pattern 1 above). They are JVM-process-scoped and leave zero persistent trace.

### Anti-Pattern 2: Reflective Hacking of Forge Config Internals

**What:** Using reflection to modify `ForgeConfigSpec` internal `BitSet` or `ModConfig`'s `ConfigFileTypeHandler` to force config values at runtime.

**Why bad:**
- Extremely fragile — Forge internals change between versions
- Class loading order sensitivity — the hack must run before `ForgeConfigSpec` is built
- Hard to debug — silent failures when Forge's internal representation doesn't match expected
- Violates module boundaries

**Instead:** Add a simple `System.getProperty()` check in the application-level `ClientSmokeConfig` class — the class you own and control.

### Anti-Pattern 3: Subclassing or Shadowing MDGL Tasks

**What:** Creating a custom Gradle task that extends or replaces MDGL's auto-generated run tasks.

**Why bad:**
- MDGL task generation is complex (classpath assembly, AT processing, mapping wiring)
- Future MDGL updates could break the custom task
- IDE integration (run config generation) depends on MDGL's own task wiring

**Instead:** Use MDGL's `legacyForge.runs { }` DSL to declare run configs. MDGL generates the correct tasks. If additional behavior is needed (e.g., timeout), use Gradle's `doFirst`/`doLast` hooks on the generated task, not a replacement.

## Scalability Considerations

| Concern | Local dev (1 user) | Team (10 users) | CI (automated) |
|---------|-------------------|-----------------|----------------|
| Config isolation | System property override prevents TOML mutation | Same — no shared state to conflict | Same — each CI run has its own JVM process |
| Output directory | `clientsmoke-reports/` in game dir (default) | Same | Override to `build/smoke-reports/` via `-Dclientsmoke.reportDir` for CI artifact collection |
| Concurrent runs | N/A (one Minecraft instance per machine) | N/A | Each CI agent runs one instance |
| Test discovery | Scanner finds all `@ClientSmoke` on classpath | Same | Filter by `-Dclientsmoke.modId=...` to isolate specific mod |
| Timeout | Not needed (developer watching) | Not needed | `-Pclientsmoke.timeoutMinutes=N` prevents hung CI jobs |

## Sources

- **MDGL Run Configuration API** — https://github.com/neoforged/ModDevGradle/blob/main/README.md#runs (HIGH confidence — official documentation)
- **Existing `ClientSmokeStateMachine.handleExit()`** — v1.0 two-phase exit pattern (HIGH confidence — primary source, directly analyzed)
- **Forge 1.20.1 ForgeConfigSpec** — Config value resolution mechanism (HIGH confidence — primary source from project)
- **System.getProperty() JavaDoc** — JDK 17 built-in (HIGH confidence — standard Java API)
