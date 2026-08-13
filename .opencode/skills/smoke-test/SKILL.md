---
name: smoke-test
description: Write and run ClientSmoke tests that verify visual behavior in a real Minecraft client. Use for render output, GL state, texture correctness, and full-lifecycle integration checks.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# smoke-test

Write and run ClientSmoke tests that verify visual behavior in a real Minecraft client (render output, GL state, texture correctness, full-lifecycle integration).

## When to use
- Visual correctness: does the rendered output have the right pixel colors?
- Full-lifecycle: does the mod work from load through world join to render?
- GL state: are shaders compiling and applying correctly?
- Integration: does component A correctly feed into component B during a real tick?
- Do NOT use when: Structural invariants — use unit tests instead
- Do NOT use when: Quick runtime checks — use progressive exploration instead

## Rules
- NEVER write smoke tests as JUnit or rely on any base class, interface, or method contract — the no-arg constructor body IS the test
- give the test class a public no-arg constructor whose body performs the assertions
- annotate the test class with @ClientSmoke
- PREFER register visual hooks in the constructor for per-test render verification
- place the test in a Gradle module that depends on the annotation via compileOnly; the annotation is discovered from any mod on the classpath
- treat @ClientSmoke as RetentionPolicy.CLASS: ASM-visible but not reflectively accessible at runtime, which prevents accidental class loading during discovery
- access Minecraft singletons via Minecraft.getInstance()
- the framework runs as a tick-driven state machine on ClientTickEvent
- tests run in a deterministic creative superflat world (ClientSmokeTest, seed 12345L)
- use the dedicated clientSmoke Gradle run configuration; the smoke client uses a separate game directory (run/clientsmoke/) and auto-exits, so it does not block
- RenderHook fires before framebuffer capture and can draw debug geometry
- CaptureVerifier fires after capture and receives a NativeImage for pixel-level assertions
- treat hooks as one-shot: they are cleared after each test
- ClientSmoke is a standalone framework that discovers @ClientSmoke-annotated classes via ASM scanning, creates a deterministic superflat world, executes test constructors in priority order, captures screenshots, runs pixel assertions via visual hooks, and produces JSON + JUnit XML reports.

## Workflow
1. Create a class in the relevant module's src/main/java/ under a smoke/ package
2. Annotate with @ClientSmoke (no JUnit — just the annotation)
3. Implement a no-arg constructor that performs assertions
4. Register visual hooks if needed
5. Sync the Gradle project if dependencies changed
6. Build the project
7. Run the smoke tests
8. Check run/clientsmoke/clientsmoke-reports/ for results

## Output
- Type: clientsmoke-report
- screenshots (required) — written to run/clientsmoke/clientsmoke-reports/screenshots/
- json-report (required) — report-{timestamp}.json
- junit-xml (required) — junit-{timestamp}.xml
- Stop when: mc.stop() then 60-tick grace, then Runtime.halt(0) on all-pass, halt(1) on failure

<!-- locked residual (verbatim, do not edit) -->
```java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClientSmoke {
    String description() default "";  // human-readable, appears in reports
    int priority() default 0;         // lower = executes first
    String modId() default "";        // optional namespace gating
}
```
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
```java
public MaterialPipelineSmoke() {
    // Register render hook: draws custom geometry before screenshot
    ClientSmokeVisualHooks.set(
        mc -> { /* draw custom GL */ },       // RenderHook
        image -> { /* assert pixel colors */ } // CaptureVerifier
    );
}
```
```
INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE
  → TEST_EXEC (foreach test by priority) → HUD_HIDE → SCREENSHOT
  → REPORT → EXIT
```
Screenshots: written to `run/clientsmoke/clientsmoke-reports/screenshots/`
Reports: JSON (`report-{timestamp}.json`) + JUnit XML (`junit-{timestamp}.xml`)
