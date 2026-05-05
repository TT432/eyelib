# Feature Landscape — Client Smoke Test

**Domain:** NeoForge mod client-side smoke testing framework
**Researched:** 2026-05-06
**Confidence:** HIGH (verified against NeoForge docs, reference implementation, and ecosystem survey)

## Executive Summary

The Minecraft modding ecosystem has exactly one structured testing framework: NeoForge GameTest. It's server-side, structure-template-based, and designed for block/interaction logic verification. There is **no existing annotation-driven client smoke testing framework** for visual/rendering/GUI validation. This project fills a genuine gap.

The reference implementation (iris-tutorial-mod) demonstrates the proven pattern: config-driven enabling → ClientTickEvent.Pre state machine → auto-world-create → render stabilize → screenshot capture → auto-exit. The key innovation is using `@ClientSmoke` annotation for discovery, which separates test definitions from the framework's classloading domain and prevents accidental class loading of test targets during mod construction.

---

## Table Stakes

Features every client smoke testing tool MUST have. Missing any of these = product feels broken.

| # | Feature | Why Expected | Complexity | Dependencies | Notes |
|---|---------|--------------|------------|--------------|-------|
| TS1 | **Annotation-driven test discovery** | Annotation is the core decoupling mechanism. Without it, test code and framework code intermingle, causing class loading issues at mod construction time. | **Medium** | None | Must scan at `FMLClientSetupEvent` or later to respect NeoForge classloading boundaries. Annotations live in a shared-api submodule; framework reads them without loading annotated classes. |
| TS2 | **Auto-world create & join** | Users expect "set config, launch, see results." Manual world creation defeats the "smoke test" automation promise. | **Medium** | TS1 (test metadata drives world params) | Must handle: world name generation, GameType selection (creative for visual tests), world already-exists vs fresh-create branching. Reference: `Minecraft.createWorldOpenFlows().createFreshLevel(...)` |
| TS3 | **Screenshot/grab output** | The primary artifact of a client smoke test. Without screenshots, there's no way to verify visual correctness. | **Low** | TS2 (need world loaded) | Use vanilla `Screenshot.grab()` with configurable filename patterns (timestamp, test name, modid). Output path must be deterministic and documented. |
| TS4 | **Config-driven enable/disable** | Off-by-default safety. Test framework must not interfere with normal gameplay. A config toggle gates the entire automation pipeline. | **Low** | None | Follow iris-tutorial-mod pattern: simple string/boolean in mod config. Empty/disabled = normal gameplay. Set to test identifier = automation mode. NeoForge `ModConfigSpec` is standard. |
| TS5 | **Auto-close/exit after completion** | After screenshots are captured, the game must exit so results can be inspected. Manual exit breaks any CI pipeline integration. | **Low** | TS3 | Use `Runtime.getRuntime().halt(0)` after a grace period (5s delay for file writes to flush). Graceful `Minecraft.stop()` first, then `halt(0)`. |
| TS6 | **Deterministic render stabilization** | Screenshots captured mid-frame or before shader pipelines compile are useless. Must wait for render pipeline stability. | **Medium** | TS2 | Minimum: world-join delay (configurable seconds). Advanced: tick-count-based stabilize counter. Must account for shader compilation (NeoForge/Iris pipeline readiness). Reference: iris-tutorial-mod `reloadStabilizeTicks`. |
| TS7 | **F1 (HUD hidden) for clean captures** | Screenshots with HUD, crosshair, and debug text are unprofessional and hard to compare. | **Low** | TS3 | `Minecraft.options.hideGui = true` before capture. Restore on exit (if not auto-exiting). Must also prevent mouse-grab interference via mixin (reference: iris-tutorial-mod `MixinMouseHandler`). |

---

## Differentiators

Features that set this tool apart from anything else in the ecosystem. Not expected, but valued.

| # | Feature | Value Proposition | Complexity | Dependencies | Notes |
|---|---------|-------------------|------------|--------------|-------|
| D1 | **Test ordering dependencies** (`@ClientSmoke(dependsOn = "...")`, `priority = N`) | Multiple tests in sequence: "load world → render entity → take screenshot → apply resource pack → screenshot again." Without ordering, tests run in arbitrary JVM order. | **Medium** | TS1 | Can be expressed as DAG: framework computes topological sort, runs tests in dependency order. Unordered tests can run in parallel if engine supports it (v2 consideration). |
| D2 | **Multi-mod configuration support** | A mod that uses this framework should be able to define its own test config TOGETHER with other mods' tests. Each mod's `@ClientSmoke` methods get their own config namespace. | **Medium** | TS1, TS4 | Namespace config by `modid` (extracted from annotation or class package). Each mod's tests have independent enable/disable, delay, world params. Avoids config collision between consumer mods. |
| D3 | **Test metadata in screenshot filenames** | Screenshots named `modid_testname_timestamp.png` are self-documenting. Screenshots named `2026-05-06_14.30.22.png` require cross-referencing logs. | **Low** | TS3 | Format: `{modid}_{testName}_{timestamp}.png`. Also write a `test_report.json` or `test_report.md` alongside screenshots summarizing pass/fail/metadata. |
| D4 | **Per-test world configuration** | Different tests need different worlds: flat world for entity rendering, normal world for terrain mods, specific seed for reproducible scenes. | **Medium** | TS2 | `@ClientSmoke` annotation parameters: `worldPreset = FLAT`, `gameType = CREATIVE`, `seed = 12345L`, `difficulty = PEACEFUL`. Falls back to sensible defaults. |
| D5 | **Frame sequence / GIF capture** (v2) | Static screenshots miss animation bugs. Frame sequence capture enables visual diff of animated effects. | **High** | TS3, TS6 | Configurable frame count + interval. Saves numbered PNG frames + metadata file for external GIF composition. Reference: iris-tutorial-mod GIF mode. Complexity is HIGH due to timing stability and frame-accurate capture requirements. |
| D6 | **Human-readable test report** | After tests finish, generate a markdown or JSON report: which tests ran, screenshot paths, timestamps, any errors. | **Low** | TS3, TS4 | Write to `run/screenshots/smoke_test_report.md` (or `.json`). Includes: test name, modid, timestamp, screenshot path, duration, status (success/timeout/error). |

### Differentiators Considered but Not Recommended for v1

| Feature | Why Deferred | When to Revisit |
|---------|-------------|-----------------|
| **Hot-reloadable test definitions** | Requires file watcher + dynamic class reloading — enormous complexity for marginal gain. Mod code changes already require restart. | v3+ — only if user demand proves restart avoidance is critical |
| **Screenshot diff/comparison** | Pixel-perfect comparison across GPU/driver/OS combinations is a research-grade problem. Reference images age instantly with any rendering change. | v2 — introduce basic checksum/hash comparison of screenshots with tolerance thresholds |
| **CI integration scripts** | The framework should be CI-friendly (exit code, deterministic output paths), but CI orchestration (GitHub Actions, Jenkins) is out of scope per PROJECT.md. | v2 — provide example CI configs, not built-in scripts |

---

## Anti-Features

Features to explicitly NOT build. Adding these would increase maintenance burden without proportional value.

| # | Anti-Feature | Why Avoid | What to Do Instead |
|---|-------------|-----------|-------------------|
| A1 | **In-game GUI for test management** | GUI framework in Minecraft is complex (screens, widgets, rendering), creates its own rendering interference, and duplicates what a config file does more cleanly. | Config file (`client_smoke_test-common.toml`) is the single source of truth. Users edit text, not click buttons. |
| A2 | **Network/server-side testing** | Out of scope per PROJECT.md. Server testing is NeoForge GameTest's domain. This is a CLIENT smoke testing framework. | Document the boundary clearly. If a mod needs server tests, direct users to NeoForge GameTest. |
| A3 | **Performance benchmarking** | FPS measurement, tick timing, memory profiling — these are different problems with different tooling (Spark, Observable, Minecraft debug profiler). Mixing concerns bloats the framework. | Recommend external profiling tools. Smoke testing = visual correctness, not performance. |
| A4 | **Automated assertion / regression testing** | "Does this screenshot match the reference?" is a pixel-comparison problem. GPU/driver/os differences make this fragile. v1 scope is screenshots for human review. | v2 could add checksum-based regression detection (D2 revisit), but never claim "automated pass/fail from screenshots" without explicit tolerance configuration. |
| A5 | **Runtime test registration** | Allowing `/smoketest run MyTest` from in-game chat adds a secondary test discovery path that can bypass classloading safety. | Tests discovered ONLY at mod construction time via annotation scanning. No command-based test registration. |
| A6 | **Modpack-level test orchestration** | Running tests across multiple mods with dependencies between them is a different problem (modpack validation). | Each mod runs its own smoke tests independently. The framework provides per-mod config isolation; modpack-level orchestration is the modpack author's responsibility. |

---

## Feature Dependencies

```
TS1 (Annotation Discovery)
 ├── TS2 (Auto-World) ── requires TS1 (know what world to create)
 │    ├── TS3 (Screenshot) ── requires TS2 (need loaded world)
 │    │    ├── TS5 (Auto-Exit) ── requires TS3 (exit after capture)
 │    │    └── D3 (Metadata Filenames) ── requires TS3 (naming convention)
 │    └── TS6 (Render Stabilize) ── requires TS2 (need world to wait in)
 │         └── D5 (Frame/GIF Capture) ── requires TS6 (stable frames)
 ├── TS4 (Config) ── independent (mod config system)
 │    └── D2 (Multi-Mod Config) ── requires TS4 (config namespace per mod)
 ├── D1 (Test Ordering) ── requires TS1 (know what to order)
 ├── D4 (Per-Test World Config) ── requires TS1, TS2 (annotation params feed world creation)
 └── D6 (Test Report) ── requires TS3, TS4 (screenshot paths + config context)
```

**Phase 1 bootstrap order** (respecting dependencies):
1. TS4 (Config) + TS1 (Annotation) — can develop in parallel
2. TS2 (Auto-World) + TS6 (Render Stabilize) — TS1 must be done, can overlap
3. TS3 (Screenshot) + TS5 (Auto-Exit) + D3 (Metadata) — TS2 must be done, these are tightly coupled
4. D6 (Test Report) — TS3 must be done

---

## MVP Recommendation

### Must Have (Phase 1 — Ship to Validate)

Prioritize in this order:
1. **TS4 — Config-driven enable/disable** (first, because everything else gates on it)
2. **TS1 — Annotation-driven test discovery** (first, core architecture)
3. **TS2 — Auto-world create & join** (second, needs TS1 metadata)
4. **TS6 — Render stabilization** (second, tightly coupled with TS2)
5. **TS3 — Screenshot/grab output** (third, needs TS2 + TS6)
6. **TS5 — Auto-close/exit** (third, needs TS3)
7. **D3 — Metadata filenames** (bundled with TS3, low effort)
8. **TS7 — F1 HUD hiding** (bundled with TS3, low effort)

**This gets a working end-to-end pipeline**: set config → launch → world loads → stabilize → screenshot → exit.

### Defer to Phase 2

- **D1 — Test ordering** (complexity spike, not needed for single-test workflows)
- **D2 — Multi-mod config** (value grows with adoption, not needed for first user)
- **D4 — Per-test world config** (nice but Sensible Defaults covers 80% of cases)
- **D6 — Test report** (screenshot filenames + timestamps are sufficient for v1)

### Defer to v2+

- **D5 — Frame/GIF capture** (high complexity, specialized use case)
- Screenshot diff/comparison
- CI example configs
- Hot-reloadable test definitions

---

## Feature Interaction Matrix

|  | TS4 Config | TS1 Annotation | TS2 World | TS3 Screenshot | TS5 Exit | TS6 Stabilize | TS7 F1 |
|--|-----------|---------------|-----------|---------------|----------|---------------|--------|
| **TS4 Config** | - | Config gates annotation scope | Config provides world params | Config provides filename template | Config provides exit delay | Config provides stabilize duration | Config provides F1 toggle |
| **TS1 Annotation** | Reads config.enabled | - | Test method drives world creation | Test name into filename | - | - | - |
| **TS2 World** | - | - | - | World must be loaded | - | World tick counter needed | - |
| **TS3 Screenshot** | - | - | - | - | Exit triggered post-capture | Must wait for stabilize | HUD hidden pre-capture |
| **D1 Ordering** | - | Depends on annotations discovered | Ordered tests may share world | Ordered tests may all screenshot | Exit after last test only | Each test may need own stabilize | - |

---

## Complexity Calibration

| Complexity | What It Means | Examples |
|-----------|---------------|----------|
| **Low** | Single-class implementation, few edge cases, well-understood Minecraft API | Config file, HUD hiding, auto-exit, screenshot naming |
| **Medium** | Requires careful state machine design, multiple Minecraft subsystem interactions, or non-trivial edge case handling | Annotation scanning, world creation lifecycle, render stabilization, test ordering DAG |
| **High** | Requires deep Minecraft internals knowledge, platform-specific timing hacks, or API surface that may change across versions | Frame-accurate GIF capture (timing drift, frame duplication, vsync interaction), pixel-level screenshot comparison |

---

## Sources

| Source | Confidence | What It Provides |
|--------|-----------|-----------------|
| NeoForge GameTest docs (docs.neoforged.net) | HIGH | Confirms ecosystem gap: GameTest is server-side structural testing, no client smoke testing exists |
| iris-tutorial-mod source (local reference) | HIGH | Proven auto-start pattern: ClientTickEvent.Pre state machine, world creation, screenshot, exit |
| NeoForge sides/concepts docs (docs.neoforged.net) | HIGH | Classloading safety patterns: `@Mod(dist = Dist.CLIENT)`, `FMLEnvironment.dist` |
| NeoForge 1.21.1 API docs (Context7 verified) | HIGH | Minecraft API surface: `Screenshot.grab()`, `createWorldOpenFlows()`, `Minecraft.stop()`, `Runtime.getRuntime().halt()` |
| MinecraftForge AggregateTest (GitHub) | MEDIUM | Aggregate test runner pattern — Groovy-based, not annotation-driven, confirms novelty of annotation approach |
| WebSearch: "Minecraft mod client smoke test screenshot automation" | MEDIUM (no direct matches found) | Negative finding: no existing annotation-driven client smoke test framework exists — validates this project fills a gap |

