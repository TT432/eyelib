# Project Research Summary

**Project:** eyelib ClientSmoke v1.1 — One-Click Gradle Automation
**Domain:** Gradle task automation for Minecraft mod client-side smoke testing
**Researched:** 2026-05-08
**Confidence:** HIGH

## Executive Summary

The v1.1 goal is to replace a 3-step manual process (set gradle property, edit TOML, run specific task) with a single `./gradlew runClientSmoke` command. Research across all four domains converges on the same core insight: **MDGL's `legacyForge.runs { }` DSL does the heavy lifting; we just declare a `clientSmoke` run config and bridge config values via JVM system properties.**

The recommended approach introduces **zero new dependencies** — everything is MDGL built-in (`systemProperty()`, `ideName`, auto-generated tasks), JDK built-in (`System.getProperty()`, `javax.xml.stream`), or already in the project (Gson, SLF4J, ForgeConfigSpec). System properties (`-Dclientsmoke.enabled=true`) are the transport layer between Gradle and the Minecraft JVM process: they are temporary (die with the process), testable (settable in unit tests), and backward-compatible (fall back to existing TOML config when absent).

The primary risk is bypassing the application-level override in favor of hacking Forge's internal `ForgeConfigSpec` state — this is the #1 critical pitfall identified across both ARCHITECTURE and PITFALLS research. Prevention is simple: add `isEnabled()` / `shouldExitAfterSmoke()` wrapper methods in `ClientSmokeConfig` that check `System.getProperty()` before falling back to `ForgeConfigSpec.get()`. Secondary risks include Windows exit code capture with `Runtime.halt()` (needs real hardware verification) and the "no tests found → IDLE → task hangs forever" edge case (requires force-exit mode to redirect to EXIT state).

## Key Findings

### Recommended Stack

No new dependencies. The v1.1 automation layer uses exclusively technologies already present or JDK built-in:

- **MDGL (LegacyForge) 2.0.91** — `legacyForge.runs { }` DSL provides `systemProperty()`, `ideName`, auto-generated Gradle tasks and IDE run configs. This is the project's existing build plugin; no alternative needed.
- **JVM System Properties (`-D` flags)** — Simplest possible cross-process configuration bridge. Set by MDGL via `systemProperty()` in the run config; read by the mod via `System.getProperty()`. Process-scoped (no cleanup), transparent (visible in process listing), backward-compatible (fallback to TOML).
- **`javax.xml.stream` (JDK 17 built-in)** — For JUnit XML report generation. No external dependency. Consumable by Jenkins, GitHub Actions, TeamCity.
- **Runtime classpath change**: `eyelib-clientsmoke` moves from conditionally-gated `localRuntime` to always-on `localRuntime`. Runtime gating shifts to `ClientSmokeConfig.isEnabled()` returning `false` by default. This avoids the "class not found" pitfall and keeps the dependency graph unchanged.

### Expected Features

**Table stakes (must ship in v1.1):**
- Single `./gradlew runClientSmoke` command launches all smoke tests — core deliverable
- Auto-enable smoke testing (no manual TOML editing) — via system property override
- Isolated game directory (`run-smoke/`) — don't pollute normal dev world
- Auto-exit after tests complete — `Runtime.halt()` with correct pass/fail exit code
- Zero impact on normal `runClient` — separate run config, smoke mod idle by default

**Differentiators (ships in v1.1):**
- IDE run configuration auto-generated (`ideName = "Run Client Smoke Tests"`)
- Config always fresh on each run (system properties, not persisted)
- Isolated config scope (smoke config lives in `run-smoke/config/`, never touches `run/config/`)
- Elevated logging for CI debugging

**Defer to v1.2+:**
- CI timeout/cleanup handling
- Multi-mod smoke testing (consumer mods)
- Auto-enable of `enableSmokeTest` Gradle property (settings-level change)

### Architecture Approach

Three patterns form the architecture backbone:

1. **System Property Override (Config Resolution Chain)** — Application-level `isEnabled()` wrapper that checks `System.getProperty("clientsmoke.enabled")` first, falls back to `ForgeConfigSpec.ENABLED.get()`. This is the bridge between Gradle (which sets `-D` flags) and the Minecraft runtime (which reads them). No reflection, no Forge internals, no persistent file mutation.

2. **MDGL Run Config Isolation** — Separate `legacyForge.runs.clientSmoke` entry with its own system properties, IDE name, and game directory. Shares MDGL infrastructure (classpath assembly, AT processing) but is independently configured. No cross-contamination with `runClient`.

3. **Immutable Result Accumulation → Exit Code** — `testResults` list is the single source of truth. `handleExit()` performs a single reduction (`allMatch`) to determine aggregate exit code. No mutable "hasFailures" boolean that can drift out of sync.

**Anti-patterns to actively avoid:** mutating persistent TOML files from Gradle, reflective hacking of `ForgeConfigSpec` internals, subclassing/shadowing MDGL-generated tasks.

### Critical Pitfalls

1. **Hacking Forge config internals instead of adding application-level override** — Use `System.getProperty()` wrapper in `ClientSmokeConfig`. Never touch `ForgeConfigSpec` internal `BitSet` or use reflection on Forge config. Detection: any reference to Forge internal config classes is a red flag.

2. **Dependency classpath gate not removed** — Remove the `if (enableSmokeTest == 'true')` guard on `localRuntime project(':eyelib-clientsmoke')`. Smoke mod must always be on classpath. Runtime gating is via `isEnabled()` returning false by default. Detection: `runClientSmoke` must work after `clean` build with no manual config.

3. **Exit code not propagated** — Current v1.0 calls `Runtime.halt(0)` unconditionally. Must aggregate `testResults` to compute pass/fail before `halt()`. Detection: deliberately failing test must produce `BUILD FAILED` in Gradle.

4. **No tests found → IDLE → task hangs forever** — When in force-exit mode, empty scan results must transition to REPORT (generate empty report) → EXIT, not IDLE. Detection: `runClientSmoke` with no annotated tests must complete (not hang).

5. **Windows `Runtime.halt()` exit code capture** — May not be reliably detected by Gradle on Windows. Mitigation: test on real Windows; have `System.exit()` fallback or exit code marker file ready. Detection: verify `%ERRORLEVEL%` on Windows CMD after both pass and fail scenarios.

## Implications for Roadmap

### Suggested Phase Structure

**Phase 1: Gradle Run Configuration & Classpath**
- **Rationale:** Everything depends on the `runClientSmoke` task existing and the smoke mod being on the classpath. This is the foundation — nothing else works without it.
- **Delivers:** `legacyForge.runs.clientSmoke` block in root `build.gradle`, unconditional `localRuntime` dependency, `.gitignore` entries for smoke artifacts, IDE run configuration auto-generated.
- **Implements:** MDGL Run Config Isolation pattern. Removes Pitfall #2 (classpath gate).
- **Research flag:** Standard MDGL DSL — well-documented, skip research-phase.

**Phase 2: Config Override Bridge & State Machine Fixes**
- **Rationale:** Once the task launches Minecraft, the system property override must actually be read by the mod. Also fix the two correctness bugs blocking CI integration.
- **Delivers:** `ClientSmokeConfig.isEnabled()` and `shouldExitAfterSmoke()` wrapper methods, all `ENABLED.get()` call sites updated, "no tests" IDLE→EXIT redirect, correct exit code aggregation in `handleExit()`, JUnit XML report generation.
- **Implements:** System Property Override pattern and Immutable Result Accumulation pattern. Avoids Pitfalls #1, #3, #4, #6.
- **Research flag:** Needs `/gsd-research-phase` — the no-tests IDLE fix requires understanding the full state machine transitions.

**Phase 3: Verification & Polish**
- **Rationale:** Validate the "one-click" promise on real hardware, especially Windows exit code capture. Verify edge cases. Add developer-facing documentation.
- **Delivers:** Windows exit code verification (real hardware), deliberately-failing test CI workflow validation, elevated logging configuration, user-facing command documentation, smoke-only `runClient` verification (no regression).
- **Implements:** Quality gates for Pitfall #5 (Windows). Table stakes verification.
- **Research flag:** Standard QA — skip research-phase, use `/gsd-verify-work`.

### Phase Ordering Rationale

- Phase 1 is the hard prerequisite — the run config must exist before anything can be tested. It also addresses the most impactful pitfall (classpath gate, Pitfall #2) with a one-line change.
- Phase 2 depends on Phase 1 for a working launch target but is otherwise independent. It packs the highest-risk changes (config override, state machine modifications) together so they can be tested as a unit.
- Phase 3 is gated on Phase 2 for correctness but can proceed in parallel with documentation work. The Windows verification specifically needs Phase 2's exit code changes.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | MDGL 2.0.91 APIs verified against official README and existing project usage. JDK APIs are stable. No external dependencies to evaluate. |
| Features | HIGH | Derived directly from v1.0 code analysis (ClientSmokeConfig, ClientSmokeStateMachine, root build.gradle). Table stakes are clear. |
| Architecture | HIGH | All three research files independently converge on the same pattern: system property bridge + MDGL run isolation. Anti-patterns validated against real Forge 47.1.3 internals. |
| Pitfalls | HIGH | Pitfalls #1-#4 validated against concrete code paths in v1.0. Pitfall #5 (Windows exit code) is MEDIUM — based on community reports, needs real hardware verification. |

**Overall confidence: HIGH** — All research files independently converge. The only uncertainty is Windows `Runtime.halt()` exit code capture on real hardware.

### Gaps to Address

- **Windows exit code reliability:** Research is based on community reports of Gradle + `Runtime.halt()` issues on Windows. During Phase 3 verification, test on real Windows hardware with both `cmd` and PowerShell. Have `System.exit()` fallback or exit code marker file ready if `halt()` exit codes are not captured.
- **Empty test run UX decision:** When no `@ClientSmoke` tests exist, should the exit code be 0 (nothing to test, not a failure) or 1 (test run found nothing)? Research recommends 0 with clear logging, but this is a product decision to confirm during planning.

## Sources

### Primary (HIGH confidence)
- **MDGL README** — https://github.com/neoforged/ModDevGradle/blob/main/README.md — verified `systemProperty()`, `ideName`, `legacyForge.runs` DSL against plugin v2.0.91
- **Root `build.gradle`** — Existing run configs, dependency gating, and MDGL usage patterns — primary source, directly analyzed
- **`ClientSmokeConfig.java`** (v1.0) — ForgeConfigSpec usage, `ENABLED`/`EXIT_AFTER_SMOKE` fields, all call sites — primary source
- **`ClientSmokeStateMachine.java`** (v1.0) — `handleExit()`, `handleScan()`, state transitions — primary source
- **Forge 1.20.1 ForgeConfigSpec** — Internal BitSet tracking, config resolution mechanism — verified via Forge 47.1.3 source analysis
- **JDK 17 `Runtime.halt()` / `System.getProperty()` / `javax.xml.stream`** — Official JavaDoc — standard Java APIs

### Secondary (MEDIUM confidence)
- **Gradle `JavaExec` exit code handling on Windows** — Community discussions on StackOverflow/Gradle forums — needs local Windows verification
- **Gradle User Guide** — `JavaExec` system properties documentation — well-established Gradle API

### No LOW confidence sources used.

---
*Research completed: 2026-05-08*
*Ready for requirements: yes*
*Next: Orchestrator proceeds to requirements definition via `/gsd-discuss-phase` or directly to plan-phase*
