# Domain Pitfalls

**Domain:** NeoForge/Forge mod client-side automation and smoke testing
**Researched:** 2026-05-06
**Target platform:** MinecraftForge 1.20.1 via `legacyForge` (NeoGradle `net.neoforged.moddev.legacyforge` 2.0.91)
**Confidence:** HIGH (backed by official docs, iris-tutorial-mod reference implementation, and eyelib codebase patterns)

---

## Overview

Building a `@ClientSmoke` annotation-driven client testing framework for NeoForge/Forge mods involves navigating four high-risk zones: class loading safety, NeoForge's lifecycle model, the legacyForge build system, and OpenGL framebuffer capture. Each zone has pitfalls that can cause silent failures, `NoClassDefFoundError` crashes, blank screenshots, or non-reproducible runs.

The iris-tutorial-mod reference (NeoForge 1.21.1, `Dist.CLIENT`, `ClientTickEvent.Pre` state machine) provides a validated pattern for auto-world-creation → screenshot → exit. However, this project's target platform (Forge 1.20.1 via legacyForge) introduces additional constraints not present in the reference.

---

## Critical Pitfalls

Mistakes that cause rewrites, undebuggable crashes, or fundamentally broken test output.

### Pitfall 1: Reflection-Based Annotation Scanning Triggers Class Loading

**What goes wrong:** Using `ClassGraph`, `org.reflections:reflections`, or even manual `Class.forName()` to discover `@ClientSmoke`-annotated test classes during mod construction will trigger the JVM to load, verify, and execute the static initializers (`<clinit>`) of every discovered class. If a test class references Minecraft registries, client-only classes, or mod-internal state, that code executes before NeoForge has finished its initialization lifecycle — causing `NullPointerException`, `NoClassDefFoundError`, or deadlocked loading.

**Why it happens:** Standard reflection libraries call `Class.forName(className, true, classLoader)` — the `true` parameter means "initialize the class." In the Forge 1.20.1 `TransformingClassLoader` environment, class initialization may trigger cascade loading of transformed Minecraft classes that aren't safe to load before `FMLCommonSetupEvent` or `FMLClientSetupEvent`.

**Consequences:**
- Crash during `@Mod` constructor before any test runs
- Non-deterministic failures depending on classpath order (scan order affects which class triggers initialization first)
- Hard-to-debug `ExceptionInInitializerError` wrapping unrelated exceptions

**Prevention:**
1. **Use ASM bytecode-level scanning** (not reflection). ASM reads `.class` file bytes via `ClassReader` without triggering `Class.forName()`:
   ```java
   // SAFE: ASM reads bytecode, never initializes classes
   ClassReader reader = new ClassReader(inputStream);
   reader.accept(new ClassVisitor(Opcodes.ASM9) {
       @Override
       public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
           if (desc.equals("Lyour/pkg/ClientSmoke;")) {
               // Record class name, DON'T load it
           }
           return null;
       }
   }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
   ```
2. **Deploy annotation scanning to compile time** via an annotation processor (the `eyelib-processor` subproject already exists). Generate a `META-INF/services` file or a JSON registry of annotated classes at build time. This completely eliminates runtime scanning risk.
3. **If runtime scanning is unavoidable**, use only NeoForge-provided discovery mechanisms. On Forge 1.20.1, `ModList.get().getAllScanData()` provides `AnnotationData` from the `@Mod` annotation scanning phase — but this ONLY works for annotations declared on `@Mod`-annotated classes themselves. Test target classes (without `@Mod`) won't be found.

**Detection:** `NoClassDefFoundError` or `ExceptionInInitializerError` in mod constructor with `ClassGraph` or `Reflections` in the stack trace = class loading triggered during scan.

**Phase to address:** Phase 1 (annotation definition and scan mechanism design). This is the foundational decision — getting it wrong requires restructuring the entire test discovery system.

---

### Pitfall 2: Wrong Tick Event Phase for World State Machine

**What goes wrong:** The state machine that polls for world load, waits for render stabilization, and triggers screenshots fires on the wrong event phase. On Forge 1.20.1, `TickEvent.ClientTickEvent` has `Phase.START` (before tick logic) and `Phase.END` (after tick logic). Using `Phase.END` for world interaction can cause the state machine to observe stale world state, while rendering-related checks (chunk visibility, entity rendering) require `Phase.END` or a render-specific event.

**Why it happens:** The logical order matters:
- `Phase.START`: World hasn't ticked yet this frame. Safe for submitting world mutations/commands.
- `Phase.END`: World has ticked. Entity positions, chunk loads, and rendering are up-to-date for this frame.
- `RenderLevelStageEvent` (Forge 1.20.1): Fires during the render pass, AFTER the tick. This is when framebuffer contents are actually valid.

The iris-tutorial-mod uses `ClientTickEvent.Pre` (NeoForge 1.21), which is equivalent to Forge 1.20.1's `TickEvent.ClientTickEvent` with `Phase.START`. Following this pattern exactly (Phase.START for everything) works for world creation but can cause screenshots to capture an empty framebuffer if called before the render pass.

**Consequences:**
- Blank or partially rendered screenshots (framebuffer not yet populated)
- State machine advances before world is truly loaded (player entity not yet spawned)
- Race condition: chunk rendering completes between Phase.START and Phase.END, so Phase.START screenshots miss chunks

**Prevention:**
1. **Split the state machine:** Use `TickEvent.ClientTickEvent` with `Phase.START` for world lifecycle checks (world loaded? player spawned?). Use `RenderLevelStageEvent` (or `Phase.END` + a one-tick delay) for actual screenshot capture.
2. **The iris-tutorial-mod "wait ticks" pattern is correct but needs adaptation.** The reference waits `RELOAD_STABILIZE_TICKS` (40 ticks default) after world detection before capturing. On Forge 1.20.1, capture should happen in `Phase.END` after the stabilize counter elapses, not `Phase.START`.
3. **Explicitly check `Minecraft.getInstance().levelRenderer` state** before screenshot — `levelRenderer.allChanged()` returns false until the first full render pass completes.

**Detection:** Screenshots with missing chunks, black sky, or missing entities despite `mc.player != null && mc.level != null` being true = wrong event phase.

**Phase to address:** Phase 2 (state machine implementation). Must be validated with actual screenshot output against the reference mod.

---

### Pitfall 3: OpenGL Calls on Non-Render Thread

**What goes wrong:** Calling `Screenshot.grab()` or any OpenGL operation from a tick event handler that fires on the game thread (not the render thread). In Minecraft, OpenGL context is bound to the Render thread. Cross-thread GL calls produce undefined behavior: crashes (`1282 INVALID_OPERATION`), black screenshots, or GL state corruption.

**Why it happens:** `TickEvent.ClientTickEvent` handlers run on the game thread. `RenderLevelStageEvent` handlers run on the render thread. On Forge 1.20.1, `Screenshot.grab()` internally calls `glReadPixels()` and other GL functions — it MUST be called from the render thread. The `Screenshot.grab()` callback (`Consumer<Component>`) fires asynchronously after the screenshot is saved; the caller doesn't need to worry about that callback, but the `grab()` call itself is synchronous in terms of GL operations.

**Consequences:**
- `GL_INVALID_OPERATION` crash
- Silent failure: screenshot saved as empty/black PNG
- Hard-to-reproduce: works in dev environment (single-threaded rendering), fails in production (multi-threaded)

**Prevention:**
1. **Capture screenshots only in `RenderLevelStageEvent`** (Forge 1.20.1) or the equivalent render-phase callback. Never in `TickEvent` directly.
2. If the state machine must trigger capture from a tick handler, set a volatile flag and let the render event handler pick it up:
   ```java
   // Tick handler (Phase.START or Phase.END)
   private static volatile boolean captureRequested = false;
   
   @SubscribeEvent
   public static void onTick(TickEvent.ClientTickEvent event) {
       if (shouldCapture() && event.phase == TickEvent.Phase.END) {
           captureRequested = true;
       }
   }
   
   // Render handler (RenderLevelStageEvent)
   @SubscribeEvent
   public static void onRender(RenderLevelStageEvent event) {
       if (captureRequested && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
           captureRequested = false;
           Screenshot.grab(gameDirectory, filename, mc.getMainRenderTarget(), cb);
       }
   }
   ```
3. **Verify with `RenderSystem.assertOnRenderThread()`** during development to catch thread violations early.

**Detection:** `GL_INVALID_OPERATION` crash with `Screenshot.grab` in stack trace = GL call on wrong thread.

**Phase to address:** Phase 2 (screenshot capture integration). If using `RenderLevelStageEvent`, verify the event is available in the Forge 1.20.1 API.

---

### Pitfall 4: Main Framebuffer Not Ready at Capture Time

**What goes wrong:** `mc.getMainRenderTarget()` returns a `RenderTarget` object even when its backing framebuffer hasn't been cleared or rendered to this frame. Capturing before the main render pass completes produces a screenshot of the PREVIOUS frame's contents (or black screen if it's the first frame).

**Why it happens:** The main framebuffer lifecycle:
1. Frame start: framebuffer cleared (optional, depends on render config)
2. Terrain rendered to framebuffer
3. Entities, particles, etc. rendered to framebuffer
4. UI/HUD rendered to framebuffer
5. Framebuffer blitted to screen

`Screenshot.grab()` calls `glReadPixels()` on the currently bound framebuffer. If called between steps 1 and 2, the screenshot gets empty terrain. If called during step 4, the screenshot may include UI elements (even with `hideGui=true` if called before the next frame).

**Consequences:**
- Empty sky with no terrain in screenshot
- Previous frame's content (visibly different from current state)
- Ghost artifacts from uncleared framebuffer areas
- Flickering: different results on different runs (race with render pipeline)

**Prevention:**
1. **Capture in `RenderLevelStageEvent.Stage.AFTER_LEVEL`** (for Forge 1.20.1) — this guarantees terrain + entities have been rendered.
2. **Use `mc.options.hideGui = true` at least one frame before capture** — the HUD toggle takes effect on the NEXT frame, not the current one. Set it, wait one tick, then capture.
3. **Verify framebuffer is bound:** `RenderSystem.getModelViewStack()` and `RenderSystem.applyModelViewMatrix()` are reliable indicators that GL state is set up for rendering.
4. **Add a configurable post-stabilize delay** (iris-tutorial-mod pattern: `RELOAD_STABILIZE_TICKS`). The default of 40 ticks (2 seconds) accounts for chunk loading + first render.
5. **For absolute safety**, wait for `mc.getFramerate()` to stabilize (non-zero, above a threshold) — first few frames after world load can have 0 FPS counter.

**Detection:** Screenshot is blank, all-one-color, or shows terrain from a clearly wrong position = framebuffer timing issue.

**Phase to address:** Phase 2-3 (screenshot capture + stabilization tuning).

---

### Pitfall 5: `compileOnly` Dependency on Test Annotation Causes Runtime Discovery Failure

**What goes wrong:** The `@ClientSmoke` annotation class is declared as a `compileOnly` dependency so that mods under test can reference it without bundling the test framework. However, the test framework ITSELF needs the annotation at runtime to discover annotated classes. If the annotation jar is only on the compile-time classpath of the test framework, annotation class literals (e.g., `ClientSmoke.class`) throw `NoClassDefFoundError` at runtime.

**Why it happens:** The project context specifies "与 eyelib root 模块通过 compileOnly 或 runtimeOnly 依赖，不引入编译时耦合." For the annotation DEFINITION module:
- Target mods depend on it via `compileOnly` (correct — they need the annotation type to compile, not at runtime)
- The test framework must depend on it via `implementation` or `runtimeOnly` (it needs the annotation at runtime to discover)
- If BOTH use `compileOnly`, the framework can't load the annotation class

**Consequences:**
- `NoClassDefFoundError: your/pkg/ClientSmoke` at test framework startup
- ASM-based scanning fails because the scanned descriptor string doesn't match any loaded class
- Silent failure: zero tests discovered because annotation descriptor comparison always fails

**Prevention:**
1. **Three-module architecture:**
   - `client-smoke-annotation` — annotation definition only, NO Minecraft dependencies. Target mods use `compileOnly project(':client-smoke-annotation')`.
   - `client-smoke-framework` — scanner, state machine, screenshot, config. Depends on `client-smoke-annotation` via `implementation`.
   - `client-smoke-runtime` — the `@Mod` entrypoint that bootstraps the framework. Depends on `client-smoke-framework` via `implementation`.
2. The annotation module must be a plain-JVM project (no `legacyForge` plugin), similar to `eyelib-molang` or `eyelib-processor`.
3. **Verify at build time:** A test in the framework module should successfully load the annotation class via `Class.forName()`.

**Detection:** `NoClassDefFoundError` referencing the annotation class, or zero tests discovered despite annotated classes on classpath.

**Phase to address:** Phase 1 (module structure and Gradle dependency configuration).

---

### Pitfall 6: Multiple @Mod Classes in Same ClassLoader With Same modId

**What goes wrong:** The client-smoke-test framework is a NeoForge mod itself (needs `@Mod` to bootstrap). If a target mod under test also has a `@Mod` class with the same `modId`, or if the framework's modId conflicts with any loaded mod, NeoForge rejects the duplicate and crashes.

**Why it happens:** NeoForge/Forge enforces unique `modId` across all loaded mods. On Forge 1.20.1, the `ModDiscoverer` collects all `@Mod` annotations across the classpath. Two `@Mod("same_id")` → `DuplicateModException`.

**Consequences:**
- `net.minecraftforge.fml.ModLoadingException` with "Duplicate mod" message
- Framework can't coexist with mods that use its modId
- Running `runClient` with both the framework and a target mod in the same workspace may crash

**Prevention:**
1. **Use a unique, unlikely-to-collide modId** for the framework — e.g., `client_smoke_test`. The 2-64 char lowercase constraint applies.
2. **Make the framework's `@Mod` `dist = Dist.CLIENT`** to ensure it's only loaded on client runs, not dedicated servers.
3. **Add dependency ordering** in `neoforge.mods.toml` (or `mods.toml` for Forge 1.20.1): mark the framework as `ordering="AFTER"` for target mods so the framework loads last and can discover already-loaded mods.
4. **If using a shared annotation module across framework and target mods**, the annotation module itself should NOT have a `@Mod` — it should be a plain library.

**Detection:** `ModLoadingException` during startup with "duplicate" in message = modId collision.

**Phase to address:** Phase 1 (modId and `neoforge.mods.toml` / `mods.toml` configuration).

---

### Pitfall 7: Mixin Scope Bleeding Into Target Mods

**What goes wrong:** The client-smoke-test framework applies mixins to vanilla Minecraft classes (e.g., mixing into `Minecraft` or `Screenshot` to add hooks). If target mods also mix into the same classes, the transformations can conflict — `@Overwrite` collisions, injection point mismatches, or unexpected bytecode after both mixins apply.

**Why it happens:** Mixin uses a config-file-based system. Multiple mods can each have their own mixin config targeting the same class. The order of application depends on `priority` values and load order. Without explicit `requiredMods` constraints, a framework mixin may apply even when the target mod isn't present, breaking vanilla behavior.

**Consequences:**
- `MixinApplyError` at startup if two `@Overwrite` annotations target the same method
- Subtle bugs: injection works but interacts badly with another mod's mixin
- Framework's mixins persist in the classpath even when framework is "disabled" via config

**Prevention:**
1. **Minimize framework mixins.** Prefer event-driven integration over mixin injection whenever possible. The framework should primarily use:
   - `@EventBusSubscriber` for lifecycle hooks
   - `ModConfigSpec` for configuration
   - Existing vanilla `Screenshot.grab()` (no mixin needed)
2. **If mixins are unavoidable**, declare them in a separate mixin config (`client_smoke_test.mixins.json`) with:
   - High `priority` value (e.g., 2000) so framework mixins apply last, after target mods
   - `"required": false` so framework mixins don't crash if target classes aren't present
3. **Use `@Inject` with cancellable = false, NEVER `@Overwrite`** — Overwrite is the most conflict-prone mixin operation.
4. **Test with common mods** (Sodium, Iris/Oculus, etc.) to verify no mixin conflicts.

**Detection:** `MixinApplyError` stack trace during startup, or target mod behavior changes when framework is present.

**Phase to address:** Phase 2 (if mixins are introduced; otherwise address in Phase 1 architectural decision to avoid mixins).

---

### Pitfall 8: World Save/Load Race Condition

**What goes wrong:** Calling `mc.createWorldOpenFlows().createFreshLevel()` and immediately checking `mc.level != null` in the next tick — without accounting for the asynchronous world creation pipeline. World creation involves chunk generation, lighting calculation, and spawn point determination. These happen across multiple ticks.

**Why it happens:** On Forge 1.20.1, `createFreshLevel` returns immediately after initiating the world creation flow. The actual loading happens over several ticks:
1. World creation flow starts (tick N)
2. Level data initialized (tick N+? depending on world size/seed)
3. Player entity spawned in world (tick after level data ready)
4. Chunks around spawn generated (ticks N+2 to N+20+)
5. First render of loaded chunks

Checking `mc.player != null` only confirms step 3. Chunks may still be absent from the framebuffer.

**Consequences:**
- Screenshot of empty void or partially loaded terrain
- `NullPointerException` if accessing `mc.level` before it's fully set
- Inconsistent: works on fast machines, fails on slow CI runners

**Prevention:**
1. **Multi-stage readiness check** (iris-tutorial-mod pattern):
   ```java
   // Stage 1: World created
   boolean worldCreated = mc.getLevelSource().levelExists(worldName);
   
   // Stage 2: Player spawned in world
   boolean playerSpawned = mc.player != null && mc.level != null;
   
   // Stage 3: Enough chunks loaded (stabilization period)
   boolean chunksReady = System.currentTimeMillis() - joinTime >= MIN_TICKS * 50L;
   // AND optional: check render distance chunks
   ```
2. **Add a configurable stabilization delay** (default 2-5 seconds). This is the iris-tutorial-mod `SCREENSHOT_DELAY` pattern.
3. **Check `mc.levelRenderer` state:** `mc.levelRenderer.countRenderedChunks()` (if accessible) or `mc.levelRenderer.getLastViewEntity()` can indicate render readiness.
4. **Pre-create the world once** (saves time on subsequent runs). The iris-tutorial-mod reuses the world if it already exists (`createFreshLevel` only if `!levelExists`).

**Detection:** Partially loaded terrain or black chunks in screenshot, especially when CI vs local dev produce different results.

**Phase to address:** Phase 2 (state machine implementation and stabilization tuning).

---

## Moderate Pitfalls

### Pitfall 9: Working Directory Variation Changes Screenshot Output Path

**What goes wrong:** `mc.gameDirectory` resolves to different paths depending on the run configuration:
- IDE run via `runClient`: `<project>/runs/client/`
- `gradlew runClient`: `<project>/runs/client/`
- Production install: `.minecraft/`
- CI environment: may be overridden by `--gameDir` argument

Screenshots saved to `gameDirectory/screenshots/` end up in different locations, making CI artifact collection or manual review unpredictable.

**Prevention:**
1. **Override the screenshot output path via system property or config.** For example, output to `System.getProperty("user.dir") + "/smoke-test-output/"` so CI always picks it up from the project root.
2. **Log the absolute path** on startup: `LOGGER.info("Screenshots will be saved to: {}", outputDir.toAbsolutePath())`.
3. **Use `FMLPaths.GAMEDIR.get()`** for the canonical game directory, not `mc.gameDirectory.toPath()` (they differ in some run configs).

**Detection:** Screenshots appear in different directories on different runs; CI can't find them.

**Phase to address:** Phase 3 (output and reporting configuration).

---

### Pitfall 10: MSAA Framebuffer Captures Are Unresolved or Downscaled

**What goes wrong:** If the game is configured with MSAA (multisample anti-aliasing), `mc.getMainRenderTarget()` uses a multisample framebuffer. `glReadPixels()` on a multisample framebuffer returns raw sample data — either unresolved (garbage pixels) or the driver auto-resolves (quality depends on driver implementation).

**Why it happens:** Minecraft 1.20.1 defaults to no MSAA on the main framebuffer. However, mods like Sodium or OptiFine can enable MSAA, and the Iris/Oculus shader pipeline adds custom framebuffers. When MSAA is active, `Screenshot.grab()` (which calls `glReadPixels` on the bound FBO) operates on the multisample texture.

**Consequences:**
- Garbled pixel data in screenshots (unresolved MSAA)
- Lower-resolution screenshots (driver auto-downsamples)
- Inconsistent between vanilla and modded environments

**Prevention:**
1. **Check framebuffer format before capture:** `mc.getMainRenderTarget().isStencilEnabled()` can hint at format complexity.
2. **Force non-MSAA for screenshots:** Bind a temporary non-multisampled FBO, blit the main framebuffer to it (which resolves MSAA), then capture from the temp FBO.
3. **Configure the smoke test run to disable graphics enhancements** — set MSAA to 0, disable custom shaders for capture runs.
4. **Document this limitation** in v1: "Screenshots assume default rendering pipeline; custom shader packs and MSAA may produce unexpected output."

**Detection:** Stripe patterns, wrong colors, or lower resolution in areas with geometry edges = MSAA issue.

**Phase to address:** Phase 3-4 (screenshot verification + edge case handling). Not critical for v1 if MSAA is disabled by default.

---

### Pitfall 11: `localRuntime` Config Scope Confusion With legacyForge

**What goes wrong:** The eyelib root project defines a custom `localRuntime` configuration. If the `client-smoke-test` subproject uses a different scope pattern, dependencies may be available at compile time but missing at runtime (or vice versa). This is especially confusing because:
- The `legacyForge` plugin remaps certain configurations (e.g., `implementation` → `modImplementation` semantics).
- Subproject dependencies need `modImplementation project(':subproject')` rather than plain `implementation` for the run classpath.
- The `additionalRuntimeClasspath` configuration (used by existing subprojects) differs from `localRuntime`.

**Prevention:**
1. **Follow the root project's established pattern:**
   ```groovy
   // In client-smoke-test/build.gradle
   dependencies {
       // For runtime-only presence (not pulled by dependents):
       additionalRuntimeClasspath project(':client-smoke-annotation')
       // For compile + runtime (as a mod dependency):
       modImplementation project(':client-smoke-framework')
   }
   ```
2. **Test the classpath visibility explicitly:** Add a startup check that verifies key classes are loadable:
   ```java
   try {
       Class.forName("your.pkg.ClientSmoke");
   } catch (ClassNotFoundException e) {
       throw new IllegalStateException("ClientSmoke annotation not on runtime classpath", e);
   }
   ```
3. **Use `jetbrain_build_project` to verify the full build succeeds** with the subproject included.

**Detection:** `ClassNotFoundException` or `NoClassDefFoundError` at framework startup despite correct source code.

**Phase to address:** Phase 1 (Gradle build configuration).

---

### Pitfall 12: UI Overlays Appear in Screenshots Despite `hideGui = true`

**What goes wrong:** Setting `mc.options.hideGui = true` suppresses the in-game HUD (hotbar, health, crosshair) but does NOT suppress:
- Debug screen (F3) — must also set `mc.options.renderDebug = false` or check `mc.getDebugOverlay().reset()`
- Toast notifications (advancements, recipes) — cannot be easily suppressed without mixin
- Chat messages — remain in chat overlay
- Mod overlays (minimaps, JEI, etc.) — each mod handles `hideGui` differently

The iris-tutorial-mod sets `hideGui = true` on the tick AFTER world detection, then captures on subsequent ticks. This gives the UI one frame to disappear before capture.

**Prevention:**
1. Set `hideGui = true` ONE FRAME before capture (not the same frame).
2. Additionally disable common overlays: `mc.options.renderDebug = false`, clear chat via `mc.gui.getChat().clearMessages()`.
3. **Document known limitations:** "Screenshots may include third-party mod overlays that don't respect `hideGui`. Run smoke tests in a minimal mod environment for clean output."
4. **Consider using `Framebuffer` capture at a lower render stage** (before UI pass) — but this requires mixin into the render pipeline.

**Detection:** Unwanted UI elements in test screenshots.

**Phase to address:** Phase 3 (screenshot quality verification).

---

## Minor Pitfalls

### Pitfall 13: FMLEnvironment.dist Check Required for Client-Only Mod

**What goes wrong:** A `@Mod` class annotated with `dist = Dist.CLIENT` will ONLY load on a physical client. However, if there are other entry points (e.g., `@EventBusSubscriber` without a `value = Dist.CLIENT`), those handlers may attempt to execute on a dedicated server and crash with `NoClassDefFoundError`.

**Prevention:**
1. Always annotate `@EventBusSubscriber` with `value = Dist.CLIENT` when subscribing to client-only events (`ClientTickEvent`, `RenderLevelStageEvent`, etc.).
2. Add a guard in any common-side methods:
   ```java
   if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return;
   ```

**Detection:** Crash on dedicated server run when trying to use client-only classes.

**Phase to address:** Phase 1 (mod entrypoint setup).

---

### Pitfall 14: Gradle `jarJar` With Mod Dependencies

**What goes wrong:** Using `jarJar project(':subproject')` on a subproject that is itself a Forge mod (has `@Mod`) can embed the subproject's mod metadata into the parent JAR. This may cause NeoForge to detect multiple mods in a single JAR or lead to classpath conflicts.

**Prevention:**
1. The annotation module should be a plain JVM library (not a Forge mod, no `@Mod`).
2. Use `additionalRuntimeClasspath` (not `jarJar`) for dependencies that should be present at runtime but not bundled.
3. Only use `jarJar` for self-contained library JARs that don't have their own mod metadata.

**Detection:** `DuplicateModException` or "Multiple mods in same JAR" warnings at startup.

**Phase to address:** Phase 1 (Gradle dependency configuration).

---

### Pitfall 15: Non-Deterministic World Generation Affects Screenshot Comparisons

**What goes wrong:** Creating a world with a random seed means terrain, entity positions, and weather differ every run. When comparing screenshots across runs (even manually), different terrain makes it impossible to distinguish "mod behavior changed" from "world generated differently."

**Prevention:**
1. **Use a fixed seed** in the test world configuration:
   ```java
   WorldOptions options = new WorldOptions(12345L, true, false);
   ```
2. **Use `WorldPresets.FLAT`** (superflat) for deterministic terrain without variable elevation.
3. **Document that v1 is manual inspection only**, not automated comparison. Add regression comparison in a later phase.

**Detection:** Different terrain/lighting in screenshots between runs with same config.

**Phase to address:** Phase 3 (screenshot stabilization). Critical if automated comparison is added later.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| **Phase 1: Annotation + module setup** | Pitfall 1 (class loading during scan), Pitfall 5 (compileOnly trap), Pitfall 6 (modId collision), Pitfall 11 (config scope confusion) | Use ASM bytecode scanning; three-module split (annotation/plain lib → framework → runtime mod); verify classpath at startup; follow root project `additionalRuntimeClasspath` pattern |
| **Phase 2: State machine + world lifecycle** | Pitfall 2 (wrong tick phase), Pitfall 3 (GL on wrong thread), Pitfall 7 (mixin scope), Pitfall 8 (world save/load race) | Split state machine across tick (world checks) and render (capture) events; validate thread with `assertOnRenderThread`; minimize mixins; multi-stage readiness check; configurable stabilization delay |
| **Phase 3: Screenshot + output** | Pitfall 4 (framebuffer timing), Pitfall 9 (working dir variation), Pitfall 10 (MSAA), Pitfall 12 (UI overlays), Pitfall 15 (non-deterministic seed) | Capture in `RenderLevelStageEvent.AFTER_LEVEL`; use fixed output path; document MSAA limitation; set hideGui early; use fixed seed + flat world |
| **Phase 4: Config + robustness** | Pitfall 13 (side guard), Pitfall 14 (jarJar scope) | Add FMLEnvironment.dist checks; verify jarJar only for plain libs; run dedicated-server smoke test to verify client-only code doesn't crash |
| **Build integration** | Pitfall 11 (dependency scope between subprojects) | Follow root project's `additionalRuntimeClasspath` + `modImplementation` patterns; verify with `jetbrain_build_project` |

---

## Pre-Implementation Checklist

Before writing Phase 1 code, verify:

- [ ] Annotation class is in a module WITHOUT the `legacyForge` plugin (plain `java-library` like `eyelib-molang`)
- [ ] Annotation scanning plan uses ASM, not reflection — confirmed by design doc
- [ ] modId is unique and doesn't conflict with any existing eyelib mod or target mod
- [ ] `@Mod` annotation uses `dist = Dist.CLIENT`
- [ ] `@EventBusSubscriber` annotations specify `value = Dist.CLIENT`
- [ ] Dependency configuration in build.gradle matches root project pattern (not plain `implementation`)
- [ ] No `@Overwrite` mixins planned; mixin use minimized or zero
- [ ] Config file `chapter`/`enabled` field allows disabling the smoke test entirely

---

## Sources

- NeoForge Documentation: Sides, Events, Mod Files, Structuring, Registries (docs.neoforged.net, version 26.1 + 1.21.1 archives)
- SpongePowered Mixin Wiki: "Introduction to Mixins — The Mixin Environment" (github.com/SpongePowered/Mixin/wiki)
- NeoGradle README (github.com/neoforged/NeoGradle, NG_7.1 branch) — configuration scopes, run management
- iris-tutorial-mod reference implementation (`E:\____脚本\图形学教学\iris-tutorial-mod`) — state machine pattern, screenshot timing, config system
- eyelib codebase: root `build.gradle` (dependency config patterns, `legacyForge` plugin usage), `EyelibMod.java` (@Mod entrypoint), `ClientTickHandler.java` (tick event pattern), `eyelib.mixins.json` (mixin config structure)
- Project context from `.planning/PROJECT.md` — design decisions, constraint list
