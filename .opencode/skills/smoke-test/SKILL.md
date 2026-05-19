---
name: smoke-test
description: Write and run ClientSmoke tests that verify visual behavior in a real Minecraft client. Use for render output, GL state, texture correctness, and full-lifecycle integration checks.
---

## What ClientSmoke Is

A standalone framework that discovers `@ClientSmoke`-annotated classes via ASM scanning, creates a deterministic superflat world, executes test constructors in priority order, captures screenshots, runs pixel assertions via visual hooks, and produces JSON + JUnit XML reports.

Tests are NOT JUnit — they run inside a real Minecraft client instance.

## Annotation API

```java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClientSmoke {
    String description() default "";  // human-readable, appears in reports
    int priority() default 0;         // lower = executes first
    String modId() default "";        // optional namespace gating
}
```

`RetentionPolicy.CLASS` means the annotation is ASM-visible but not reflectively accessible at runtime. This prevents accidental class loading during discovery.

## Constructor-as-Test Pattern

There is **no base class, no interface, no method contract**. The no-arg constructor body IS the test:

```java
@ClientSmoke(description = "Validates login screen renders", priority = 0)
public class LoginScreenTest {
    public LoginScreenTest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null)
            throw new AssertionError("Expected a screen to be open");
    }
}
```

The only requirements:
- A public no-arg constructor
- `@ClientSmoke` annotation on the class
- Access Minecraft singletons via `Minecraft.getInstance()`

## Visual Hooks

For per-test render verification, set hooks in the constructor:

```java
public MaterialPipelineSmoke() {
    // Register render hook: draws custom geometry before screenshot
    ClientSmokeVisualHooks.set(
        mc -> { /* draw custom GL */ },       // RenderHook
        image -> { /* assert pixel colors */ } // CaptureVerifier
    );
}
```

- `RenderHook`: fires before framebuffer capture. Can draw debug geometry.
- `CaptureVerifier`: fires after capture. Receives a `NativeImage` for pixel-level assertions.
- Hooks are one-shot: cleared after each test.

## Execution Flow

The framework runs as a tick-driven state machine on `ClientTickEvent`:

```
INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE
  → TEST_EXEC (foreach test by priority) → HUD_HIDE → SCREENSHOT
  → REPORT → EXIT
```

- World: deterministic creative superflat (`ClientSmokeTest`, seed `12345L`)
- Screenshots: written to `run/clientsmoke/clientsmoke-reports/screenshots/`
- Reports: JSON (`report-{timestamp}.json`) + JUnit XML (`junit-{timestamp}.xml`)
- Exit: `mc.stop()` → 60-tick grace → `Runtime.halt(0)` on all-pass, `halt(1)` on failure

## Adding a Smoke Test

1. Create a class in the relevant module's `src/main/java/` under a `smoke/` package
2. Annotate with `@ClientSmoke` (no JUnit — just the annotation)
3. Implement a no-arg constructor that performs assertions
4. Register visual hooks if needed

The test must go in a Gradle module that depends on the annotation (via `compileOnly`). The annotation is discovered from any mod on the classpath.

## Running Smoke Tests

There is a dedicated `clientSmoke` Gradle run configuration. To execute:

1. Sync the Gradle project if dependencies changed
2. Build the project
3. Run via `jetbrain_execute_run_configuration` with configuration name `"ClientSmoke"`
4. Check `run/clientsmoke/clientsmoke-reports/` for results

The smoke client uses a separate game directory (`run/clientsmoke/`) and auto-exits, so it doesn't block.

## When to Use

- Visual correctness: does the rendered output have the right pixel colors?
- Full-lifecycle: does the mod work from load through world join to render?
- GL state: are shaders compiling and applying correctly?
- Integration: does component A correctly feed into component B during a real tick?

Not for: structural invariants (use unit tests), quick runtime checks (use progressive exploration).
