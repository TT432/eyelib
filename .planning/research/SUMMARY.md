# Project Research Summary

**Project:** Client Smoke Test (eyelib subproject)
**Domain:** Minecraft Forge 1.20.1 client-side smoke testing framework
**Researched:** 2026-05-06
**Confidence:** HIGH

## Executive Summary

This project fills a genuine gap in the Minecraft modding ecosystem: there is no existing annotation-driven client smoke testing framework for visual/rendering/GUI validation. NeoForge GameTest covers server-side structural testing only. The iris-tutorial-mod provides a proven reference implementation of the end-to-end pattern—config-driven enabling → state machine → auto-world → render-stabilize → screenshot → auto-exit—but it is hardcoded to a single test flow. This project generalizes that pattern into a reusable framework driven by `@ClientSmoke` annotations.

**The recommended approach** is two Gradle subprojects within the existing eyelib repository: `eyelib-clientsmoke-annotation` (a plain Java library exposing only the `@ClientSmoke` annotation) and `eyelib-clientsmoke` (a Forge 1.20.1 mod using the `legacyForge` plugin). Discovery of annotated test classes uses Forge's `ModFileScanData`—a bytecode-level ASM scan that avoids class loading entirely. This mechanism is already proven in eyelib's own `ForgeMolangMappingDiscovery` and `ParticleComponentManager`. The runtime is a tick-driven state machine (`ClientTickEvent` + `RenderLevelStageEvent`) that creates a flat creative world, waits for render stabilization, invokes test methods, captures screenshots via vanilla `Screenshot.grab()`, generates a report, and exits the JVM.

**The single biggest risk** is accidentally triggering class loading of test target classes during annotation discovery—using reflection-based scanning (`Reflections`, `Class.forName()`) instead of bytecode-level ASM scanning would cause `NoClassDefFoundError` or `ExceptionInInitializerError` during mod construction. The second critical risk is capturing screenshots on the wrong thread or at the wrong render stage, producing blank or partial framebuffers. Both risks are well-understood and have proven mitigations.

## Platform Decision: Forge 1.20.1 + Java 17

**The STACK researcher recommended NeoForge 1.21.1 + Java 21 + ModDevGradle 2.0.141.** This recommendation is **REJECTED** in favor of matching the existing eyelib platform. The synthesizer resolves this conflict as follows:

| Aspect | STACK.md Recommendation | **Adopted Decision** | Rationale |
|--------|------------------------|---------------------|-----------|
| Mod loader | NeoForge 1.21.1 | **Forge 1.20.1 (47.1.3)** | Matches existing eyelib root; migration to NeoForge 1.21.1 is out of scope |
| Java version | 21 | **17** | Minecraft 1.20.1 ships Java 17; changing would break the root project |
| Gradle plugin | `net.neoforged.moddev` 2.0.141 | **`net.neoforged.moddev.legacyforge` 2.0.91** | Already used by eyelib root; supports Forge 1.20.1 |
| Annotation scan | `ModFileScanData` (NeoForge SPI) | **`ModFileScanData` (Forge)** ✓ | Same mechanism exists in Forge 1.20.1; proven in eyelib's `ForgeMolangMappingDiscovery` and `ParticleComponentManager` |
| World creation | `WorldOpenFlows` (1.21.1 API) | **Forge 1.20.1 equivalent** — needs phase-specific research | API surface may differ; the pattern (createFreshLevel with creative flat world) transfers conceptually |
| Screenshot | `Screenshot.grab()` (1.21.1) | **`Screenshot.grab()` (1.20.1)** | Vanilla API exists in both versions |
| Exit | `mc.stop()` + `halt(0)` | **Same pattern** | Works identically in Forge 1.20.1 |

**What from STACK.md is retained:**
- ASM-based `ModFileScanData` approach for annotation discovery (the key architectural insight)
- Two-phase exit pattern (`mc.stop()` → `Runtime.halt(0)`)
- `Screenshot.grab()` as the capture mechanism
- Flat/creative world for deterministic test environments
- `compileOnly` + `runtimeOnly` dependency pattern between modules

## Key Findings

### Recommended Stack

The framework lives as two Gradle subprojects within the existing eyelib multi-project build. The annotation module is a zero-dependency Java library; the runtime module is a Forge 1.20.1 client-side mod.

**Core technologies:**
- **Forge 1.20.1 + `legacyForge` 2.0.91**: Matches existing eyelib platform; `ModFileScanData` already proven in this codebase for annotation discovery without class loading
- **Java 17**: Required by Minecraft 1.20.1; consistent with all existing eyelib subprojects
- **ASM 9.x (bundled with Forge)**: Bytecode-level `.class` file reading for `@ClientSmoke` annotation discovery; zero class loading during scan
- **`Screenshot.grab()` (vanilla Minecraft)**: Reads OpenGL framebuffer → PNG; no extra dependencies needed; proven in iris-tutorial-mod
- **`ModFileScanData` (Forge SPI)**: Annotation scanning API that reads bytecode without triggering JVM class initialization; same mechanism Forge uses internally for `@Mod` discovery
- **Lombok 8.6**: Already used across all eyelib subprojects for boilerplate reduction
- **`jdk-classfile-backport` 24.0**: Optional secondary bytecode scanning (only if `ModFileScanData` proves insufficient); already a dependency in eyelib root

**Explicitly NOT used:**
- ~~NeoForge 1.21.1 / ModDevGradle~~ — rejected in favor of existing Forge 1.20.1 platform
- ~~`ServiceLoader` / `META-INF/services`~~ — requires class loading of implementations
- ~~`Reflections` / `ClassGraph`~~ — runtime classpath scanners that trigger class loading
- ~~`System.exit(0)`~~ — runs JVM shutdown hooks that hang in modded Minecraft
- ~~`@Overwrite` mixins~~ — most conflict-prone mixin operation; prefer event-driven integration

### Expected Features

**Must have (MVP — Phase 1):**
- **TS4 — Config-driven enable/disable**: Off-by-default toggle gates the entire automation pipeline; prevents interference with normal gameplay
- **TS1 — Annotation-driven test discovery**: `@ClientSmoke` annotation scanned via `ModFileScanData` (bytecode-level, zero class loading) at mod construction time
- **TS2 — Auto-world create & join**: Programmatic flat creative world creation via `createFreshLevel`; world reuse if already exists
- **TS6 — Deterministic render stabilization**: Configurable tick delay (default 40 ticks) after world load to wait for chunk rendering and shader compilation
- **TS3 — Screenshot/grab output**: Vanilla `Screenshot.grab()` with metadata-rich filenames (`{modId}/{testClassName}_{timestamp}.png`)
- **TS5 — Auto-close/exit after completion**: Two-phase exit: `mc.stop()` (graceful) → 3s delay → `Runtime.halt(0)` (hard exit)
- **TS7 — F1 HUD hiding**: `mc.options.hideGui = true` one frame before capture for clean screenshots

**Should have (Phase 2):**
- **D1 — Test ordering dependencies**: `@ClientSmoke(priority = N, dependsOn = "...")` for sequenced multi-test runs
- **D2 — Multi-mod configuration**: Per-mod config namespaces isolated by `modId`; independent enable/disable and world params per consumer mod
- **D4 — Per-test world configuration**: Annotation parameters for world preset, game type, seed, difficulty
- **D6 — Human-readable test report**: Markdown/JSON report summarizing test results, screenshot paths, timing, errors

**Defer to v2+:**
- **D5 — Frame sequence / GIF capture**: High complexity due to timing stability and frame-accurate capture requirements
- Screenshot diff/comparison: Pixel-perfect comparison across GPU/driver/OS is a research-grade problem
- CI integration example configs: Framework is CI-friendly by design; example configs can come later

### Architecture Approach

Two Gradle subprojects with strict dependency isolation. The annotation module (`eyelib-clientsmoke-annotation`) is a plain `java-library` with zero Minecraft dependencies—test target mods depend on it via `compileOnly`. The runtime module (`eyelib-clientsmoke`) is a Forge 1.20.1 `legacyForge` mod that implements the state machine, scanner, screenshot capture, and reporting. The runtime module depends on the annotation module via `implementation`.

Annotation discovery uses Forge's `ModFileScanData` at the bytecode level—the only code path that loads a test class is the explicit `Class.forName()` call at the `TEST_EXEC` state, after the world is fully loaded and stabilized. The state machine has 10 states (`INIT → CONFIG_LOAD → ANNOTATION_SCAN → WORLD_PREP → WORLD_OPEN → WORLD_WAIT → TEST_EXEC → SCREENSHOT → NEXT_TEST → REPORT → EXIT`) driven by a single `@EventBusSubscriber` handler on `ClientTickEvent`. Screenshot capture delegates to `RenderLevelStageEvent` to guarantee OpenGL context availability.

**Major components:**
1. **`ClientSmokeStateMachine`**: Sole driver of the execution timeline; single tick handler with `switch` over states; owns the FSM lifecycle
2. **`SmokeTestScanner`**: Queries `ModList.get().getAllScanData()` for `@ClientSmoke` annotations; returns `List<SmokeTestDescriptor>` with zero class loading
3. **`ClientSmokeEngine`**: Test queue management (priority + dependency ordering), world creation orchestration, test instance lifecycle (load → instantiate → invoke → destroy), screenshot scheduling, report data aggregation
4. **`ScreenshotCapture`**: Wraps `Screenshot.grab()` with file naming, directory creation, and timestamp management; called only from render thread
5. **`ClientSmokeConfig`**: NeoForge `ModConfigSpec` wrapper for enable/disable, world name, stabilize ticks, screenshot directory, exit behavior
6. **`SmokeReportGenerator`**: Aggregates test results into HTML/JSON/text output; no Minecraft knowledge required

### Critical Pitfalls

1. **Reflection-based scanning triggers class loading** — Using `Reflections`, `ClassGraph`, or `Class.forName()` for annotation discovery loads test classes during mod construction, causing `NoClassDefFoundError`. **Prevention:** Use ASM bytecode-level scanning via `ModFileScanData` (proven in eyelib's `ForgeMolangMappingDiscovery`). Only call `Class.forName()` at `TEST_EXEC` state after world is fully loaded.

2. **OpenGL calls on non-render thread** — `Screenshot.grab()` calls `glReadPixels()` which MUST execute on the render thread. Calling from `ClientTickEvent` produces `GL_INVALID_OPERATION` or black screenshots. **Prevention:** Capture screenshots only in `RenderLevelStageEvent.Stage.AFTER_LEVEL`. Use a volatile flag to signal capture intent from the tick handler to the render handler.

3. **Main framebuffer not ready at capture time** — Capturing before the main render pass completes produces blank screenshots or stale previous-frame content. **Prevention:** Capture in `RenderLevelStageEvent.Stage.AFTER_LEVEL`; set `hideGui = true` one frame before capture; use configurable post-stabilize delay (default 40 ticks).

4. **Wrong tick event phase for world state** — `Phase.START` may observe stale world state; `Phase.END` is needed for up-to-date entity positions and chunk rendering. **Prevention:** Split the state machine: use `ClientTickEvent` for world lifecycle checks (world loaded? player spawned?), use `RenderLevelStageEvent` for screenshot capture.

5. **`compileOnly` dependency causes runtime discovery failure** — If the framework depends on the annotation module via `compileOnly`, `Class.forName("...ClientSmoke")` fails at runtime. **Prevention:** Framework depends on annotation module via `implementation`; test target mods use `compileOnly`.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Module Scaffolding + Config + Annotation Discovery
**Rationale:** These are the foundational building blocks. Config gates everything else; annotation discovery defines the core architecture. Both have zero dependencies on each other and can be developed in parallel.

**Delivers:** Two Gradle subprojects initialized (`eyelib-clientsmoke-annotation` as plain Java library, `eyelib-clientsmoke` as Forge 1.20.1 `legacyForge` mod); `@ClientSmoke` annotation with `CLASS` retention; `ClientSmokeConfig` via `ModConfigSpec` with `enabled` toggle; `SmokeTestScanner` using `ModFileScanData` with zero class loading; `SmokeTestDescriptor` POJO.

**Addresses:** TS1 (annotation discovery), TS4 (config enable/disable)
**Avoids:** Pitfall 1 (class loading during scan), Pitfall 5 (compileOnly trap), Pitfall 6 (modId collision), Pitfall 11 (config scope confusion), Pitfall 13 (side guard)

**Research flag:** LOW — `ModFileScanData` pattern already proven in eyelib codebase. Standard annotation design.

### Phase 2: State Machine + World Lifecycle + Stabilization
**Rationale:** The state machine is the runtime backbone; world creation and stabilization are tightly coupled. Annotation discovery must be complete before this phase begins (dependency on Phase 1).

**Delivers:** `ClientSmokeStateMachine` with all 10 states; programmatic flat creative world creation using Forge 1.20.1 world creation API; multi-stage readiness check (world exists → player spawned → chunks rendered → stabilize ticks elapsed); `ClientSmokeEngine` with test queue management.

**Addresses:** TS2 (auto-world), TS6 (render stabilization), D1 (test ordering — test queue infrastructure)
**Avoids:** Pitfall 2 (wrong tick phase), Pitfall 8 (world save/load race)

**Research flag:** MEDIUM — World creation API in Forge 1.20.1 needs phase-specific research; may differ from iris-tutorial-mod's 1.21.1 `WorldOpenFlows` API. The pattern is proven but exact API surface needs verification.

### Phase 3: Screenshot Capture + Auto-Exit + HUD Hiding
**Rationale:** Tightly coupled—screenshot requires stabilization, exit requires screenshot completion. All three are low-complexity individually but require careful integration with the state machine.

**Delivers:** `ScreenshotCapture` wrapping `Screenshot.grab()` with `RenderLevelStageEvent.AFTER_LEVEL` integration; metadata-rich filename pattern; `hideGui = true` one frame before capture; two-phase exit (`mc.stop()` → `halt(0)`); configurable post-test delay.

**Addresses:** TS3 (screenshot), TS5 (auto-exit), TS7 (F1 HUD hiding), D3 (metadata filenames)
**Avoids:** Pitfall 3 (GL on wrong thread), Pitfall 4 (framebuffer timing), Pitfall 10 (MSAA — document limitation), Pitfall 12 (UI overlays)

**Research flag:** LOW — `Screenshot.grab()` and exit pattern are standard vanilla APIs; proven in iris-tutorial-mod. `RenderLevelStageEvent` availability on Forge 1.20.1 needs verification but is a well-known event.

### Phase 4: Test Execution + Report Generation
**Rationale:** Test execution depends on world being loaded and stabilized. Report generation is the final step before exit and aggregates data from all prior phases.

**Delivers:** Test class loading via `Class.forName()` at `TEST_EXEC` state; method invocation (`Runnable.run()` or named method with `ClientSmokeContext`); `SmokeReportGenerator` producing JSON summary; per-test error handling with graceful degradation.

**Addresses:** D6 (test report), test execution infrastructure
**Avoids:** Pitfall 1 (class loading only at TEST_EXEC, not earlier)

**Research flag:** LOW — Standard Java reflection for controlled class loading; JSON serialization is straightforward.

### Phase 5: Multi-Mod Config + Per-Test World Config
**Rationale:** Only meaningful after the single-test pipeline works end-to-end. These features add configuration complexity that is not needed for the first working prototype.

**Delivers:** Per-mod config namespaces; `@ClientSmoke` annotation parameters (worldPreset, gameType, seed, difficulty, reuseWorld); config isolation between consumer mods.

**Addresses:** D2 (multi-mod config), D4 (per-test world config)
**Avoids:** Pitfall 6 (modId collision — per-mod namespaces prevent it)

**Research flag:** LOW — Configuration expansion is straightforward; follows established NeoForge `ModConfigSpec` patterns.

### Phase Ordering Rationale

- **Phase 1 must come first**: Everything gates on config and annotation discovery. Without these, no other component can function.
- **Phase 2 before Phase 3**: Screenshots require a loaded, stabilized world. The state machine is the backbone that Phase 3 integrates with.
- **Phase 3 before Phase 4**: Test execution happens in a loaded world; reports summarize what happened after tests run.
- **Phase 5 is independent of Phase 4** and can be parallelized with it, but conflicts with early stages if per-test config breaks the simple single-test pipeline.
- **MVP scope = Phases 1-4**: End-to-end pipeline works (config → scan → world → stabilize → execute → screenshot → exit → report).

### Research Flags

**Phases likely needing deeper research during planning:**
- **Phase 2 (World Lifecycle):** Forge 1.20.1 world creation API. The iris-tutorial-mod uses `WorldOpenFlows` (1.21.1 API); the exact 1.20.1 equivalent needs verification. Flat world preset registration may differ.
- **Phase 3 (Screenshot Capture):** `RenderLevelStageEvent` availability in Forge 1.20.1. The event exists but stage constants may differ. Thread safety of `Screenshot.grab()` on Forge 1.20.1 needs validation.
- **General:** `ModFileScanData` programmatic API for custom annotation queries. Eyelib's existing code uses `ModList.get().getAllScanData()` and filters annotations—this needs to be verified as the exact pattern for the new subproject.

**Phases with standard patterns (skip deep research):**
- **Phase 1 (Module Scaffolding):** Standard Gradle multi-project setup; `legacyForge` plugin already configured in root; annotation design follows existing eyelib `@MolangMapping` pattern.
- **Phase 4 (Test Execution):** Controlled `Class.forName()` + reflection is standard Java; JSON report generation is straightforward.
- **Phase 5 (Multi-Mod Config):** Standard `ModConfigSpec` expansion; follows documented NeoForge patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **MEDIUM** | STACK.md provides HIGH-confidence analysis of the NeoForge 1.21.1 stack, but that has been rejected in favor of Forge 1.20.1. The adopted stack is already verified in the existing eyelib codebase, making the final conclusion HIGH. |
| Features | **HIGH** | Feature landscape confirmed by NeoForge GameTest docs (gap identified), iris-tutorial-mod reference implementation (proven pattern), and web search (no competing annotation-driven client smoke framework found). |
| Architecture | **HIGH** | State machine pattern verified in iris-tutorial-mod; `ModFileScanData` annotation scanning verified in eyelib's `ForgeMolangMappingDiscovery` and `ParticleComponentManager`; module structure follows established eyelib conventions. |
| Pitfalls | **HIGH** | Pitfalls 1-8 are backed by concrete failure modes documented in Forge/NeoForge docs, iris-tutorial-mod reference code, or eyelib codebase patterns. Pitfalls 9-15 are moderate-level practical concerns with clear mitigations. |

**Overall confidence:** HIGH — the key architectural decisions (ModFileScanData scanning, state machine, screenshot capture) are all backed by working reference implementations in this exact codebase or the iris-tutorial-mod.

### Gaps to Address

1. **Forge 1.20.1 world creation API**: The exact API surface for programmatic world creation (equivalent to NeoForge 1.21.1's `WorldOpenFlows`) needs verification at implementation time. May require `Minecraft.loadLevel()` or similar older API.

2. **`RenderLevelStageEvent` on Forge 1.20.1**: The event class and stage constants need to be verified against the 1.20.1 Forge event bus API. If unavailable, `RenderWorldLastEvent` or `TickEvent.Phase.END` with a one-tick delay are fallbacks.

3. **Screenshot output path consistency**: Different run configurations (`runClient` via IDE vs Gradle vs CI) resolve `mc.gameDirectory` differently. A fixed output path via system property or config override needs implementation-phase verification.

4. **ModId uniqueness**: `clientsmoke` needs to be confirmed as not colliding with any existing mod or future eyelib module.

5. **Annotation scan API for `ModFileScanData`**: While the pattern is proven in eyelib (both `ForgeMolangMappingDiscovery` and `ParticleComponentManager`), the exact invocation pattern for filtering `@ClientSmoke` from `ModList.get().getAllScanData()` needs verification during Phase 1 implementation.

## Sources

### Primary (HIGH confidence)
1. **[iris-tutorial-mod source](file:///E:/____脚本/图形学教学/iris-tutorial-mod/src/main/java/)** — State machine pattern, world creation via `WorldOpenFlows`, screenshot via `Screenshot.grab()`, two-phase exit (`mc.stop()` → `Runtime.halt(0)`), config system via `ModConfigSpec`
2. **[eyelib `ForgeMolangMappingDiscovery.java`](eyelib-molang source)** — Proves `ModFileScanData` works for custom annotation discovery on Forge 1.20.1; uses `ModList.get().getAllScanData()` with zero class loading
3. **[eyelib `ParticleComponentManager.java`](root source)** — Second proof of `ModFileScanData` + deferred `Class.forName()` pattern in this exact codebase
4. **[eyelib root `build.gradle`](root)** — Confirms `legacyForge` 2.0.91, Java 17, `localRuntime` config, `additionalRuntimeClasspath` pattern, `jdk-classfile-backport` dependency

### Secondary (MEDIUM confidence)
5. **NeoForge GameTest documentation** (docs.neoforged.net) — Confirms ecosystem gap: GameTest is server-side structural testing; no client smoke testing exists
6. **NeoForge sides/concepts documentation** (docs.neoforged.net) — Classloading safety patterns: `@Mod(dist = Dist.CLIENT)`, `FMLEnvironment.dist`
7. **ModDevGradle README + BREAKING_CHANGES.md** — Build infrastructure reference (platform-specific details rejected but patterns retained)

### Tertiary (LOW confidence / needs validation)
8. **Forge 1.20.1 world creation API** — Exact API surface for `createFreshLevel` equivalent needs implementation-phase verification; derived from 1.21.1 patterns
9. **`RenderLevelStageEvent` on Forge 1.20.1** — Event class and stage constants need verification against 1.20.1 Forge event bus
10. **WebSearch: "Minecraft mod client smoke test screenshot automation"** — Negative finding validates this project fills a gap; no competing framework found

---

*Research completed: 2026-05-06*
*Ready for roadmap: yes*
