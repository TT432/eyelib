# Roadmap: client-smoke-test

## Overview

Build a standalone NeoForge 1.20.1 mod that automates client-side smoke testing for Minecraft mods. Tests are discovered via `@ClientSmoke` annotation using bytecode-level ASM scanning (zero class loading), executed inside an auto-created creative flat world, captured as clean screenshots on the render thread, and reported as JSON — all driven by a config-gated tick state machine. The journey progresses from Gradle module scaffolding through runtime backbone to output pipeline and closes with test execution and reporting.

## Phases

- [ ] **Phase 1: Module Scaffolding + Config + Annotation Discovery** — Foundation: two Gradle subprojects, @ClientSmoke annotation, ModFileScanData scanning, ModConfigSpec configuration, root module wiring
- [x] **Phase 2: State Machine + World Lifecycle + Stabilization** — Runtime backbone: tick-driven state machine, automatic world creation, multi-stage readiness checks
- [ ] **Phase 3: Screenshot Capture + Auto-Exit** — Output pipeline: render-thread screenshot capture with HUD hiding, timestamped file output, graceful two-phase JVM exit
- [ ] **Phase 4: Test Execution + Report Generation** — Close the loop: safe test class loading and invocation, failure isolation, priority ordering, JSON report output

## Phase Details

### Phase 1: Module Scaffolding + Config + Annotation Discovery
**Goal**: Foundation layer established — two Gradle subprojects initialized, @ClientSmoke annotation defined, bytecode-level scanning via ModFileScanData working with zero class loading side-effects, NeoForge ModConfigSpec configuration ready with global enable/disable toggle, and root module compileOnly dependency wired.
**Depends on**: Nothing (first phase)
**Requirements**: MOD-01, MOD-02, MOD-03, ANN-01, ANN-02, ANN-03, CFG-01, CFG-02, CFG-03
**Success Criteria** (what must be TRUE):
  1. Running `./gradlew :eyelib-clientsmoke-annotation:build` produces a JAR containing only the `@ClientSmoke` annotation class, with zero Minecraft dependencies on its compile or runtime classpath.
  2. Running `./gradlew :eyelib-clientsmoke:build` produces a Forge 1.20.1 mod JAR that loads in the eyelib development client without errors at mod construction time.
  3. Placing the annotation JAR on a test mod's `compileOnly` classpath allows `@ClientSmoke`-annotated classes to compile successfully; removing the annotation JAR from the runtime classpath leaves the annotation absent but prevents no class-loading errors.
  4. `ModFileScanData`-based scanning discovers `@ClientSmoke`-annotated classes from any JAR on the classpath (including third-party mods) without triggering JVM class initialization — verifiable by placing a test class with an intentionally broken static initializer (`static { throw new RuntimeException("loaded!"); }`) and confirming the scan completes without that error surfacing.
  5. Toggling `clientsmoke-common.toml`'s `enabled=false` silences the entire framework (no scanning, no state machine, no events); setting `enabled=true` triggers annotation scanning and config parsing at mod construction time with visible log output.
**Plans**: 5 plans in 4 waves

Plans:
- [x] 01-01-PLAN.md — Gradle Build Configuration: both subprojects, root wiring (MOD-01, MOD-02, MOD-03)
- [x] 01-02-PLAN.md — @ClientSmoke Annotation Definition (ANN-01, MOD-01)
- [x] 01-03-PLAN.md — Runtime Mod Entrypoint + Forge Metadata (MOD-02)
- [x] 01-04-PLAN.md — Config System: ForgeConfigSpec with 4 entries (CFG-01, CFG-02, CFG-03)
- [x] 01-05-PLAN.md — Scanner: ModFileScanData zero-class-loading discovery (ANN-02, ANN-03)

### Phase 2: State Machine + World Lifecycle + Stabilization
**Goal**: Runtime backbone operational — a single `@EventBusSubscriber` handler on `TickEvent.ClientTickEvent` drives the full state machine through all states (INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE → ...), automatically creates a creative flat world without user interaction, and multi-stage readiness checks confirm the world is fully loaded and the player is spawned before proceeding to test execution.
**Depends on**: Phase 1
**Requirements**: ENG-01, ENG-02, ENG-03, ENG-04
**Success Criteria** (what must be TRUE):
  1. Launching the development client with `enabled=true` causes a creative superflat world to be created and joined automatically — no user interaction with the world creation screen or title screen is required.
  2. The state machine passes through all defined states in the correct order (INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE), with each transition logged at INFO level and no mandatory state ever bypassed.
  3. The stabilization phase waits a configurable number of ticks (`reloadStabilizeTicks`, default 40) after player spawn before declaring readiness; at stabilization completion, the player entity is non-null and the world's chunk rendering is complete.
  4. If world creation fails (e.g., incompatible world data), the state machine transitions to a safe error state with a clear log message rather than hanging or crashing silently.
**Plans**: 2 plans in 2 waves

Plans:
- [x] 02-01-PLAN.md — State Machine Core: enum, @EventBusSubscriber, state transitions, scanner wiring (ENG-01, ENG-02)
- [x] 02-02-PLAN.md — World Creation + Stabilization: auto-join creative flat world, multi-stage readiness, stabilization timer (ENG-03, ENG-04)

### Phase 3: Screenshot Capture + Auto-Exit
**Goal**: Output pipeline works reliably — screenshots are captured on the render thread via `RenderLevelStageEvent.AFTER_LEVEL`, HUD is automatically hidden (F1) one frame before capture and restored afterward, output PNG files are saved with test-class + timestamp naming under a consistent directory, and the client auto-exits after all tests complete using a graceful two-phase shutdown.
**Depends on**: Phase 2
**Requirements**: CAP-01, CAP-02, CAP-03, EXIT-01, EXIT-02
**Success Criteria** (what must be TRUE):
  1. When the state machine reaches the SCREENSHOT state, a valid PNG file is written to `clientsmoke-reports/screenshots/` with a filename containing the test class name and an ISO-8601 timestamp.
  2. The captured screenshot is free of HUD elements (hotbar, crosshair, chat overlay, debug screen) — verifiable by inspecting the output PNG file in any image viewer.
  3. After screenshot capture completes, the HUD visibility is restored to its pre-capture state — the F1 toggle is undone so subsequent frames render normally if tests continue.
  4. When `exitAfterSmoke=true`, after all tests complete, the Minecraft client window closes automatically within 5 seconds — the JVM process terminates and does not hang.
  5. The exit sequence logs confirm a graceful `mc.stop()` call followed by `Runtime.getRuntime().halt(0)` after a 3-second delay — no stuck shutdown hooks or deadlocked threads.
**Plans**: 2 plans in 2 waves

Plans:
- [x] 03-01-PLAN.md — Enum Extension + HUD_HIDE + Screenshot Capture Pipeline (CAP-01, CAP-02, CAP-03)
- [x] 03-02-PLAN.md — EXIT State Implementation + STABILIZE Handoff (EXIT-01, EXIT-02)

### Phase 4: Test Execution + Report Generation
**Goal**: End-to-end pipeline closes — `@ClientSmoke`-annotated test classes are loaded via `Class.forName()` only at the TEST_EXEC state (after world is fully stabilized), instantiated, and invoked; any exception is captured and recorded without interrupting subsequent tests; tests execute in priority order; and a JSON report summarizing all results is written before auto-exit triggers.
**Depends on**: Phase 2 (world must be loaded and stabilized)
**Requirements**: EXEC-01, EXEC-02, EXEC-03, RPT-01, RPT-02
**Success Criteria** (what must be TRUE):
  1. After world stabilization completes, `@ClientSmoke`-annotated test classes are loaded via `Class.forName()`, instantiated via default constructor, and their execution entry point invoked — the test method runs inside a fully loaded Minecraft world with the player entity accessible.
  2. When a test method throws an exception (checked or unchecked), the exception is captured with its stack trace and recorded in the report as a failure; subsequent queued tests continue executing without interruption or cascade failure.
  3. Tests execute in order of their `priority` attribute — a test annotated with `@ClientSmoke(priority = 0)` runs before a test with `@ClientSmoke(priority = 10)`; tests with equal priority execute in discovery order.
  4. A valid JSON report file is written to `clientsmoke-reports/report-{timestamp}.json` containing: `totalTests`, `passed`, `failed` counts, and an `entries` array where each entry includes `className`, `status` ("passed" or "failed"), `durationMs`, and `error` (message + stack trace on failure).
  5. The report file is fully written and flushed to disk before the auto-exit sequence begins — the file survives `Runtime.getRuntime().halt(0)` without truncation or corruption.
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Module Scaffolding + Config + Annotation Discovery | 5/5 | Complete | 2026-05-06 |
| 2. State Machine + World Lifecycle + Stabilization | 2/2 | Complete | 2026-05-06 |
| 3. Screenshot Capture + Auto-Exit | 0/2 | Planned (2 waves) | 2026-05-07 |
| 4. Test Execution + Report Generation | 0/5 | Not started | - |
