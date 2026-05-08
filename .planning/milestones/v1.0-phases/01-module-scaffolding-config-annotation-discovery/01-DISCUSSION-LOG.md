# Phase 1: Module Scaffolding + Config + Annotation Discovery - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in 01-CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-06
**Phase:** 1-Module Scaffolding + Config + Annotation Discovery
**Areas discussed:** Subproject naming, build configuration, annotation design, ModFileScanData scanner, configuration system, root integration (all auto-selected)

---

## Subproject Naming and Package Convention

| Option | Description | Selected |
|--------|-------------|----------|
| Follow existing pattern: `eyelib-clientsmoke-annotation` / `eyelib-clientsmoke`, packages `io.github.tt432.clientsmokeannotation` / `io.github.tt432.clientsmoke` | Matches `eyelib-molang`→`eyelibmolang`, `eyelib-importer`→`eyelibimporter` pattern | ✓ |
| Shorter names: `clientsmoke-annotation` / `clientsmoke` | Deviates from project convention | |

**Auto-selected:** Follow existing eyelib naming convention. Same transformation as sibling subprojects: `eyelib-molang` → `io.github.tt432.eyelibmolang`.

---

## Build Plugin Selection

| Option | Description | Selected |
|--------|-------------|----------|
| Annotation: `java-library`, Runtime: `legacyForge` | Matches existing split (eyelib-attachment uses java-library, eyelib-importer uses legacyForge) | ✓ |
| Both use `legacyForge` | Annotation module doesn't need Forge, would add unnecessary MC deps | |
| Annotation: `java`, Runtime: `legacyForge` | `java-library` preferred (enforces API vs implementation separation) | |

**Auto-selected:** `java-library` for annotation (zero MC deps), `legacyForge` for runtime. Overrides STACK.md researcher's suggestion of two distinct build files — this maintains consistency with the existing repository.

---

## @ClientSmoke Annotation Design

| Option | Description | Selected |
|--------|-------------|----------|
| `@Retention(CLASS)`, `@Target(TYPE)`, 3 attributes (description, priority, modId) | ASM-visible but reflection-invisible; class-level only; minimal attribute set | ✓ |
| `@Retention(RUNTIME)` | Would allow reflection discovery but risks accidental class loading | |
| Method-level `@Target(METHOD)` | Per-method granularity but adds complexity (v2 consideration) | |

**Auto-selected:** RetentionPolicy.CLASS is the key safety decision — it makes the annotation visible to ASM bytecode scanning (which doesn't load classes) while making it invisible to Java reflection (which would trigger static initializers). ElementType.TYPE keeps v1 simple with class-level tests.

---

## ModFileScanData Scanner

| Option | Description | Selected |
|--------|-------------|----------|
| `ModList.get().getAllScanData()` in @Mod constructor, no Class.forName() | Proven pattern from `ForgeMolangMappingDiscovery`, safe bytecode-level scanning | ✓ |
| `ClassGraph` / `Reflections` library | Would trigger class loading on Minecraft's transformed classpath — HIGH RISK | |
| Annotation processor (compile-time) | Would require compile-time coupling, not usable for third-party mods | |

**Auto-selected:** Use the existing eyelib pattern (`ForgeMolangMappingDiscovery` as reference). Must validate with a broken-static-initializer test to prove zero class loading.

---

## Configuration System

| Option | Description | Selected |
|--------|-------------|----------|
| `ModConfig.Type.COMMON`, 4 entries: enabled, screenshotDelay, reloadStabilizeTicks, exitAfterSmoke | Standard NeoForge config, file lives at `run/client/config/clientsmoke-common.toml` | ✓ |
| `ModConfig.Type.CLIENT` | Client-only never syncs to server, but COMMON works equally well for client-side reads | |
| Separate config per test mod | Over-engineering for v1 — single global config sufficient | |

**Auto-selected:** COMMON type with 4 entries. `enabled=false` as default ensures the framework is completely silent unless explicitly activated.

---

## Root Module Integration

| Option | Description | Selected |
|--------|-------------|----------|
| `compileOnly` annotation, `localRuntime` for runtime mod, gated by `enableSmokeTest` Gradle property | No compile coupling, no production build contamination | ✓ |
| `modImplementation` for both | Would couple smoke test to production build, violates decoupling goal | |
| Separate subproject only (no root integration) | Would require manual launch setup, defeats convenience of auto-running | |

**Auto-selected:** `compileOnly` for annotation JAR ensures @ClientSmoke references compile but don't ship. `localRuntime` for the runtime mod loads it at dev time. Gradle property `enableSmokeTest` (default: false) controls the `localRuntime` inclusion.

---

## the agent's Discretion

- Exact scanner class name and API surface
- Logging patterns (use SLF4J consistently)
- TOML file path determined by NeoForge convention
- Whether scanner returns typed result object or raw `AnnotationData` list

## Deferred Ideas

None.
