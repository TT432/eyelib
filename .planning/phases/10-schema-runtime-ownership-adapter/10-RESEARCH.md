# Phase 10: Schema/Runtime Ownership & Adapter - Research

**Researched:** 2026-05-09  
**Domain:** Gradle/Java 17 Forge particle schema-to-runtime module boundary  
**Confidence:** HIGH for project boundaries and current code shape; MEDIUM for exact adapter field list until implemented parity tests run

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
## Implementation Decisions

### Canonical Ownership
- `:eyelib-importer` remains the canonical owner for raw Bedrock particle schema/codecs, including `io.github.tt432.eyelibimporter.particle.BrParticle`.
- `:eyelib-particle` owns the canonical module-level runtime particle definition model. Root Bedrock runtime classes remain adapter/legacy targets until Phase 11.
- Root `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` should no longer be treated as canonical schema; Phase 10 may keep it as a legacy/runtime adapter target but must document that status.
- Avoid introducing another `BrParticle` in `:eyelib-particle`. Use a distinct runtime name such as `ParticleDefinition` or `RuntimeParticleDefinition` to reduce drift risk.

### Conversion Seam Scope
- `:eyelib-particle` should provide the named adapter from importer schema to particle runtime definition. A dependency from particle module to importer is acceptable for this seam; importer must not depend on particle runtime.
- Adapter coverage should include identifier, basic render parameters, curves, events, raw components map, billboard flipbook summary, and other parity-critical fields needed by loading, rendering, Molang, lifetime, and remove behavior.
- Molang values/expressions should preserve existing `MolangValue`/expression data; Phase 10 must not rewrite the Molang runtime or expression model.
- Conversion should fail loudly through a clear failure channel (`DataResult`, explicit exception, or equivalent) for invalid/missing required data. It must not silently drop fields that affect runtime parity.

### Drift Prevention And Tests
- Prevent schema/runtime drift with adapter parity tests plus documentation invariants that name each mapped field and owner.
- Adapter/parity tests should use large real addon/particle fixtures where practical, not only minimal inline JSON or mocked records, so real Bedrock particle shapes are represented.
- Phase 10 may connect the adapter seam only where needed to prove conversion; full resource loading/publication rewire remains Phase 12.
- Keep the legacy root runtime CODEC temporarily for compatibility and comparison tests, but mark it as non-canonical; Phase 11/12 can remove or collapse it when runtime/loading ownership moves.

### the agent's Discretion
Implementation details not covered above are at the agent's discretion, provided the result keeps one-way module dependency direction, avoids broad compatibility layers, and does not degrade field parity coverage.

### Deferred Ideas (OUT OF SCOPE)
## Deferred Ideas

Full runtime client extraction remains Phase 11. Full loader/publication rewire remains Phase 12. Command/network integration remains Phase 13. Final broad regression and documentation gate remains Phase 14.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PSCHEMA-01 | Maintainer can identify the canonical owner for importer/raw particle schema and the canonical owner for executable runtime particle definitions. | Ownership must be documented in `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `eyelib-particle` README/package docs, and root particle README. [VERIFIED: `.planning/REQUIREMENTS.md`, `10-CONTEXT.md`, `MODULES.md`, `docs/architecture/01-module-boundaries.md`] |
| PSCHEMA-02 | Runtime particle definitions are created from importer/raw schema through a named adapter or equivalent explicit conversion seam with parity coverage. | Add a distinct module runtime definition type plus named adapter in `:eyelib-particle`; update `eyelib-particle/build.gradle` to consume `:eyelib-importer` and likely `:eyelib-molang`. [VERIFIED: `10-CONTEXT.md`, `eyelib-particle/build.gradle`, `eyelib-importer/build.gradle`] |
| PSCHEMA-03 | Duplicate `BrParticle` ownership cannot drift silently because codec/schema behavior and runtime conversion expectations are covered by tests or documented invariants. | Use adapter parity tests, large fixture-backed tests, and static boundary/drift documentation tests; root legacy `BrParticle.CODEC` remains temporary comparison surface. [VERIFIED: `10-CONTEXT.md`, `09-VERIFICATION.md`, root/importer `BrParticle.java`] |
</phase_requirements>

## Summary

Phase 10 should not move particle rendering/runtime execution yet; it should make ownership explicit and install a narrow, tested schema-to-runtime conversion seam. [VERIFIED: `10-CONTEXT.md`, `.planning/ROADMAP.md`] The canonical raw Bedrock schema is already in `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`, where components are preserved as `BedrockResourceValue`, curves use `MolangValue`, and `billboardFlipbook()` extracts a typed flipbook summary from raw components. [VERIFIED: `eyelib-importer/.../particle/BrParticle.java`]

The root `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` is currently both a codec and executable runtime carrier: its components are typed root `ParticleComponent`s keyed by `ResourceLocation`, its curves have `calculate(MolangScope)`, and runtime emitter/particle classes call `getComponent(...)` during emission/render. [VERIFIED: root `BrParticle.java`, `BrParticleEmitter.java`, `BrParticleParticle.java`] That makes it unsuitable as the long-term canonical schema while `:eyelib-particle` must remain root-clean. [VERIFIED: `AGENTS.md`, `MODULES.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/README.md`]

**Primary recommendation:** add `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` plus `io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter` (or `ParticleSchemaAdapter`) in `:eyelib-particle`, backed by importer schema types and tests; keep root loader/runtime rewiring out of scope except for comparison/proof tests and documentation. [VERIFIED: `10-CONTEXT.md`, `MODULES.md`, `BrParticleLoader.java`]

## Project Constraints (from AGENTS.md)

- Read `docs/index/repo-map.md`, `MODULES.md`, relevant architecture docs, and nearest package README before planning structural or boundary changes. [VERIFIED: `AGENTS.md`]
- Preserve the current multi-project `Gradle + Java 17 + Forge` shape and do not collapse the importer/resource seam. [VERIFIED: `AGENTS.md`, `MODULES.md`]
- Preserve existing manager, loader, visitor, and codec patterns. [VERIFIED: `AGENTS.md`, `docs/architecture/01-module-boundaries.md`]
- Do not touch unrelated uncommitted changes; prefer narrow, stage-scoped edits over broad package churn. [VERIFIED: `AGENTS.md`]
- Document ownership and dependency rules before moving code across subsystem boundaries. [VERIFIED: `AGENTS.md`]
- Before each change, identify affected modules in `MODULES.md`; update `MODULES.md` and architecture docs when responsibility, paths, modules, or interactions change. [VERIFIED: `AGENTS.md`, `MODULES.md`]
- `:eyelib-particle` must remain free of root runtime packages, root managers, root registries, root packets, root capability helpers, Minecraft/Forge, and root `mc/impl` unless an explicitly documented adapter is introduced. [VERIFIED: `eyelib-particle/README.md`, `docs/architecture/02-side-boundaries.md`, `09-VERIFICATION.md`]
- All Gradle verification must use JetBrains MCP; never run Gradle from shell. [VERIFIED: global AGENTS, project `AGENTS.md`, `.planning/STATE.md`]
- JDTLS, VS Code, and Eclipse artifacts are prohibited. [VERIFIED: `AGENTS.md`]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Raw Bedrock particle schema/codecs | Importer schema module (`:eyelib-importer`) | — | Importer already owns `io.github.tt432.eyelibimporter.particle.BrParticle` and addon particle files. [VERIFIED: importer `BrParticle.java`, addon README] |
| Canonical runtime particle definition model | Particle module (`:eyelib-particle`) | Root legacy runtime as adapter target | User decision says `:eyelib-particle` owns module-level runtime definitions; root `BrParticle` is non-canonical legacy target until Phase 11/12. [VERIFIED: `10-CONTEXT.md`] |
| Schema-to-runtime conversion | Particle module adapter | Importer data as input only | One-way dependency particle -> importer is allowed; importer must not depend on particle runtime. [VERIFIED: `10-CONTEXT.md`] |
| Resource reload/publication | Root loader/registry for now | Particle API/store seam | `BrParticleLoader` still parses root `BrParticle.CODEC` and publishes via `ParticleAssetRegistry`; full rewire is Phase 12. [VERIFIED: `BrParticleLoader.java`, `.planning/ROADMAP.md`] |
| Runtime emission/render behavior | Root client particle runtime until Phase 11 | Future particle runtime | `BrParticleEmitter` and `BrParticleParticle` own current Molang scope setup, component iteration, lifetime, spawn, texture, UV, lighting, and tint behavior. [VERIFIED: `BrParticleEmitter.java`, `BrParticleParticle.java`] |
| Command/network behavior | Root/platform integration | Particle API request seams | Command/network rewire is explicitly Phase 13; Phase 9 already made spawn requests string-keyed. [VERIFIED: `.planning/ROADMAP.md`, `09-VERIFICATION.md`] |

## Standard Stack

### Core
| Library / Module | Version | Purpose | Why Standard |
|------------------|---------|---------|--------------|
| Java toolchain | 17 target | Main source and subproject compilation target | Root, importer, and particle Gradle files all set `JavaLanguageVersion.of(17)`. [VERIFIED: `build.gradle`, `eyelib-importer/build.gradle`, `eyelib-particle/build.gradle`] |
| Forge/NeoForge ModDev LegacyForge plugin | `2.0.91` | Forge-aware Gradle project setup | Already used by root, importer, and particle subprojects. [VERIFIED: Gradle files] |
| Mojang serialization `Codec` / `DataResult` | Existing dependency from Minecraft/Forge toolchain | Schema decode and loud conversion/validation channel | Current root/importer particle records use `Codec`; importer `ChainNode.decode` uses `DataResult.error` for invalid raw data. [VERIFIED: importer/root `BrParticle.java`] |
| `:eyelib-importer` | Project module | Canonical raw Bedrock particle schema input | Owns `io.github.tt432.eyelibimporter.particle.BrParticle` and addon particle file maps. [VERIFIED: importer `BrParticle.java`, addon code search] |
| `:eyelib-molang` | Project module | Preserve `MolangValue` and expression data | Importer particle curves use `MolangValue`; root runtime curve evaluation uses `MolangScope` + `MolangValue`. [VERIFIED: importer/root `BrParticle.java`] |
| `:eyelib-particle` | Project module | Particle API/store and new canonical runtime definition/adapter | Current module is root-clean API boundary and intended owner of future core/runtime definitions. [VERIFIED: `eyelib-particle/README.md`, `MODULES.md`] |

### Supporting
| Library / Module | Version | Purpose | When to Use |
|------------------|---------|---------|-------------|
| JUnit Jupiter | BOM `5.10.2` | Unit/static/parity tests | Use for adapter parity tests and forbidden-import/static boundary checks. [VERIFIED: root/importer/particle Gradle files, existing tests list] |
| JSpecify | `1.0.0` compileOnly | Nullness package annotations | Keep package-level `@NullMarked` patterns in new particle packages. [VERIFIED: `eyelib-particle/build.gradle`, `package-info.java`] |
| JOML | via Forge/Minecraft/root classpath; importer test has `1.10.5` | Existing particle vectors in root runtime; avoid in pure schema unless already needed | Prefer string/Molang/raw values in new definition; only introduce vectors if module dependencies support it and root-clean tests pass. [VERIFIED: root particle runtime files, importer Gradle test dependency] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `ParticleDefinition` in `:eyelib-particle` | Another `BrParticle` record in `:eyelib-particle` | Rejected by user decision because another `BrParticle` increases drift risk. [VERIFIED: `10-CONTEXT.md`] |
| `DataResult<ParticleDefinition>` adapter | Throwing adapter method only | Exceptions are acceptable by decision, but `DataResult` fits existing codec-style validation and testable error messages. [VERIFIED: `10-CONTEXT.md`, importer `BrParticle.java`] |
| Preserve only typed root components | Preserve raw component map plus derived summaries | Raw map is required because root typed components are root/Minecraft/Forge-contaminated and cannot be imported into pure `:eyelib-particle`. [VERIFIED: root component manager imports, `eyelib-particle/README.md`] |

**Installation:** no npm packages or new external libraries are needed. Add project dependencies in `eyelib-particle/build.gradle` instead of introducing a new dependency manager. [VERIFIED: Gradle files]

**Version verification:** package versions were read from project Gradle files rather than npm because this is a Java/Gradle phase. [VERIFIED: Gradle files]

## Architecture Patterns

### System Architecture Diagram

```text
Bedrock particle JSON / addon particle file
        |
        v
:eyelib-importer BrParticle.CODEC
  - format_version
  - description identifier/basic render params
  - curves/events
  - raw component map
  - billboard flipbook summary helper
        |
        v
:eyelib-particle ParticleDefinitionAdapter
  - validates required identifier/render params
  - copies MolangValue curve expressions
  - preserves raw components and derived summaries
  - returns DataResult or explicit failure
        |
        v
:eyelib-particle ParticleDefinition (canonical module runtime definition)
        |
        +--> adapter/parity tests compare importer schema, definition, and legacy root shape
        |
        +--> Phase 11/12 future root-runtime/loading rewire

Current root loader path remains unchanged in Phase 10:
particles/*.json -> BrParticleLoader -> root legacy BrParticle.CODEC -> ParticleAssetRegistry
```

### Recommended Project Structure

```text
eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/
├── api/                 # existing Phase 9 store/lookup/spawn/publication contracts
└── runtime/             # new canonical particle runtime definition + schema adapter
    ├── ParticleDefinition.java
    ├── ParticleDefinitionAdapter.java
    └── package-info.java

eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/
├── ParticleDefinitionAdapterTest.java
├── ParticleDefinitionBoundaryTest.java
└── ParticleDefinitionDocumentationTest.java

eyelib-particle/src/test/resources/io/github/tt432/eyelibparticle/runtime/fixtures/
└── ... optional copied/minimized real particle fixtures if cross-module test resources are awkward
```

### Pattern 1: Distinct Runtime Definition, Not Duplicate Schema
**What:** create a module-owned `ParticleDefinition` that stores executable-definition inputs without root runtime classes. [VERIFIED: `10-CONTEXT.md`, `eyelib-particle/README.md`]  
**When to use:** whenever code needs the canonical module-level definition produced from raw importer schema. [VERIFIED: `.planning/REQUIREMENTS.md`]  
**Example:**

```java
// Source: project pattern from records/codecs in importer BrParticle.java and root BrParticle.java
public record ParticleDefinition(
        String formatVersion,
        String identifier,
        BasicRenderParameters basicRenderParameters,
        Map<String, ParticleCurve> curves,
        ParticleEvents events,
        Map<String, BedrockResourceValue> rawComponents,
        Optional<BillboardFlipbook> billboardFlipbook
) {}
```

### Pattern 2: Named Adapter With Loud Failure
**What:** expose a named adapter method such as `ParticleDefinitionAdapter.fromSchema(BrParticle schema)` returning `DataResult<ParticleDefinition>`. [VERIFIED: `10-CONTEXT.md`; `DataResult` usage verified in importer `BrParticle.java`]  
**When to use:** Phase 10 conversion tests and later Phase 12 loader rewire. [VERIFIED: `.planning/ROADMAP.md`]  
**Example:**

```java
// Source: DataResult pattern from importer BrParticle.Curve.ChainNode.decode
public static DataResult<ParticleDefinition> fromSchema(BrParticle schema) {
    if (schema.particleEffect().description().identifier().isBlank()) {
        return DataResult.error(() -> "Particle identifier is required");
    }
    return DataResult.success(/* mapped ParticleDefinition */);
}
```

### Pattern 3: Preserve Raw Components Plus Derived Summaries
**What:** keep `Map<String, BedrockResourceValue>` in the module definition and derive only stable summaries needed for planning parity, such as billboard flipbook. [VERIFIED: importer `BrParticle.java`, `10-CONTEXT.md`]  
**When to use:** components cannot be decoded to root `ParticleComponent` in `:eyelib-particle` because those classes import root runtime, Minecraft, Forge, and annotation-scanned registries. [VERIFIED: root `ParticleComponentManager.java`, root component files, `eyelib-particle/README.md`]

### Anti-Patterns to Avoid
- **Adding `io.github.tt432.eyelibparticle.runtime.BrParticle`:** violates the decision to avoid another `BrParticle` and makes drift harder to see. [VERIFIED: `10-CONTEXT.md`]
- **Importing root particle components into `:eyelib-particle`:** would violate root-clean/module boundary rules. [VERIFIED: `eyelib-particle/README.md`, `ParticleComponentManager.java`]
- **Rewiring `BrParticleLoader` to the new adapter in Phase 10:** full loading/publication rewire is Phase 12; Phase 10 may only connect enough to prove conversion. [VERIFIED: `10-CONTEXT.md`, `.planning/ROADMAP.md`]
- **Dropping unknown/raw components:** runtime parity depends on component data; raw component preservation is explicitly required. [VERIFIED: `10-CONTEXT.md`, importer `BrParticle.java`]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON decoding | Custom Gson field walking for whole schema | Existing importer `BrParticle.CODEC` | Existing codec already handles format version, description, curves, events, and raw components. [VERIFIED: importer `BrParticle.java`] |
| Error channel | Silent defaults for missing required fields | `DataResult.error` or explicit exception | Context requires loud failure; project codecs already use `DataResult`. [VERIFIED: `10-CONTEXT.md`, importer `BrParticle.java`] |
| Component type registry in particle module | Recreate root `ParticleComponentManager` scanning | Preserve raw `BedrockResourceValue` and derived summaries | Root manager depends on Forge scan and root/Minecraft types, forbidden in pure particle module. [VERIFIED: `ParticleComponentManager.java`, `eyelib-particle/README.md`] |
| Molang conversion | String parsing/recompilation of Molang expressions | Preserve existing `MolangValue` objects | Context explicitly forbids rewriting Molang runtime/expression model. [VERIFIED: `10-CONTEXT.md`, importer/root `BrParticle.java`] |
| Fixture strategy | Only tiny mocked JSON | Large real addon fixture where practical | User decision and context require real Bedrock particle shapes where practical. [VERIFIED: user additional context, `10-CONTEXT.md`, `microsoft-shapeshifter/.../particles/witchspell.json` found] |

**Key insight:** the adapter is an ownership seam, not a new engine. It should make existing importer data consumable by a root-clean particle definition while preserving enough raw data for Phase 11/12 runtime migration. [VERIFIED: `10-CONTEXT.md`, `.planning/ROADMAP.md`]

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — Phase 10 changes code/docs/tests around in-memory particle definitions; no database/datastore holding renamed particle schema keys was found in the provided scope. [VERIFIED: provided files, `.planning/STATE.md`] | None. |
| Live service config | None — no external service/UI configuration is involved in the phase scope. [VERIFIED: `.planning/ROADMAP.md`, `10-CONTEXT.md`] | None. |
| OS-registered state | None — no scheduler/service registration is involved. [VERIFIED: `.planning/ROADMAP.md`, `10-CONTEXT.md`] | None. |
| Secrets/env vars | None — no secret/env var name change is required. [VERIFIED: phase context and requirements] | None. |
| Build artifacts / installed packages | Gradle/IDE compile outputs may contain old classes after package additions, but no installed package rename is required. [ASSUMED] | Use JetBrains MCP Gradle compile/test; IDE sync if generated task list is stale. |

## Common Pitfalls

### Pitfall 1: Treating Root `BrParticle` as Canonical Because Loader Uses It
**What goes wrong:** planner rewires around the current loader and accidentally preserves root schema ownership. [VERIFIED: `BrParticleLoader.java`, `10-CONTEXT.md`]  
**Why it happens:** `BrParticleLoader` still parses `src/main/.../client/particle/bedrock/BrParticle.CODEC`. [VERIFIED: `BrParticleLoader.java`]  
**How to avoid:** document root `BrParticle` as legacy/non-canonical and keep Phase 10 adapter tests centered on importer `BrParticle` -> particle `ParticleDefinition`. [VERIFIED: `10-CONTEXT.md`]  
**Warning signs:** new code in `:eyelib-particle` imports `io.github.tt432.eyelib.client.particle...` or creates another `BrParticle`. [VERIFIED: `eyelib-particle/README.md`, `10-CONTEXT.md`]

### Pitfall 2: Component Parity Loss
**What goes wrong:** adapter maps identifier/render params but drops raw components that drive emission, motion, appearance, lifetime, and removal. [VERIFIED: `BrParticleEmitter.java`, `BrParticleParticle.java`, component files]  
**Why it happens:** root runtime decodes components into typed `ParticleComponent`s, but importer stores components as `BedrockResourceValue`. [VERIFIED: importer/root `BrParticle.java`]  
**How to avoid:** preserve raw component map and assert known keys/values survive for real fixtures; derive summaries only when they are stable and root-clean. [VERIFIED: `10-CONTEXT.md`, importer `billboardFlipbook()`]  
**Warning signs:** tests only assert identifier or map size. [ASSUMED]

### Pitfall 3: Namespace Mismatch for Component Keys
**What goes wrong:** root lookups use unnamespaced `ResourceLocation("particle_appearance_billboard")`, while importer checks both `minecraft:particle_appearance_billboard` and `particle_appearance_billboard` for billboard extraction. [VERIFIED: importer `BrParticle.java`, `BrParticleParticle.java`]  
**Why it happens:** Bedrock source files may use `minecraft:` prefixes while legacy runtime code may use unprefixed keys. [VERIFIED: importer code handles both forms]  
**How to avoid:** adapter should normalize or preserve original keys and document lookup invariants; parity tests should include prefixed and unprefixed component keys. [VERIFIED: importer `BrParticle.java`; test need inferred from context]  
**Warning signs:** adapter uses only one spelling without a test. [ASSUMED]

### Pitfall 4: Gradle Dependency Direction Regression
**What goes wrong:** `:eyelib-particle` imports root runtime classes to reuse component parsing. [VERIFIED: `eyelib-particle/README.md`]  
**Why it happens:** root component codecs already exist and are tempting to reuse. [VERIFIED: root component files]  
**How to avoid:** allow `:eyelib-particle -> :eyelib-importer` for schema input, but never `:eyelib-particle -> root`; add static source checks over all `eyelib-particle/src/main/java`. [VERIFIED: `10-CONTEXT.md`, `09-VERIFICATION.md`]  
**Warning signs:** `project(':')`, `io.github.tt432.eyelib.client`, `net.minecraft`, or `net.minecraftforge` in particle core packages. [VERIFIED: `eyelib-particle/README.md`, `09-VERIFICATION.md`]

## Code Examples

### Adapter Parity Test Shape

```java
// Source: JUnit 5 usage and large fixture requirement from project tests/context
@Test
void realAddonParticlePreservesParityCriticalFields() {
    BrParticle schema = decodeImporterFixture("microsoft-shapeshifter/.../particles/witchspell.json");

    ParticleDefinition definition = ParticleDefinitionAdapter.fromSchema(schema)
            .getOrThrow(false, Assertions::fail);

    assertEquals(schema.particleEffect().description().identifier(), definition.identifier());
    assertEquals(schema.particleEffect().description().basicRenderParameters().material(), definition.basicRenderParameters().material());
    assertEquals(schema.particleEffect().curves().keySet(), definition.curves().keySet());
    assertEquals(schema.particleEffect().components().keySet(), definition.rawComponents().keySet());
}
```

### Boundary Static Check Shape

```java
// Source: Phase 9 verification describes existing source-scan boundary tests
assertNoForbiddenImports(
    Path.of("eyelib-particle/src/main/java"),
    "io.github.tt432.eyelib.client",
    "io.github.tt432.eyelib.network",
    "io.github.tt432.eyelib.capability",
    "io.github.tt432.eyelib.mc.impl",
    "net.minecraft",
    "net.minecraftforge"
);
```

## State of the Art

| Old Approach | Current Phase 10 Approach | When Changed | Impact |
|--------------|---------------------------|--------------|--------|
| Root `BrParticle` mixes schema codec and executable runtime component model. [VERIFIED: root `BrParticle.java`] | Importer `BrParticle` is raw canonical schema; particle `ParticleDefinition` is canonical module runtime definition; root `BrParticle` is legacy adapter target. [VERIFIED: `10-CONTEXT.md`] | Phase 10 planning, 2026-05-09 | Prevents silent drift and prepares Phase 11/12 moves. |
| Loader directly parses root `BrParticle.CODEC`. [VERIFIED: `BrParticleLoader.java`] | Keep loader unchanged for Phase 10 except tests/proof; full rewire in Phase 12. [VERIFIED: `.planning/ROADMAP.md`] | Deferred until Phase 12 | Reduces blast radius. |
| Particle module only has API/store/spawn contracts. [VERIFIED: `eyelib-particle/README.md`, file search] | Add runtime definition/adapter package while preserving root-clean boundary. [VERIFIED: `10-CONTEXT.md`] | Phase 10 | Moves ownership without moving renderer yet. |

**Deprecated/outdated:**
- Treating root `client/particle/bedrock/BrParticle.java` as canonical schema is outdated for v1.2 Phase 10. [VERIFIED: `10-CONTEXT.md`]
- Creating another `BrParticle` in `:eyelib-particle` is disallowed for this phase. [VERIFIED: `10-CONTEXT.md`]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Gradle/IDE compile outputs may need a clean-ish compile after adding packages, but no installed package rename is required. | Runtime State Inventory | Low — planner can still use normal JetBrains MCP compile/test validation. |
| A2 | Tests that only assert identifier/map size are too weak for parity. | Common Pitfalls | Medium — planner should define explicit parity field assertions. |
| A3 | Adapter tests should cover both prefixed and unprefixed component keys. | Common Pitfalls | Medium — if Bedrock inputs never use one spelling, test may be less urgent; importer code suggests both are relevant. |

## Open Questions

1. **Should `ParticleDefinition` keep `BedrockResourceValue` in its public API or wrap it in particle-owned raw component records?**
   - What we know: raw preservation is required and importer already uses `BedrockResourceValue`. [VERIFIED: `10-CONTEXT.md`, importer `BrParticle.java`]
   - What's unclear: whether exposing importer raw value types from particle runtime definition is acceptable long-term API design. [ASSUMED]
   - Recommendation: for Phase 10, use importer raw value types to avoid hand-rolling; document that the dependency is intentional and reassess in Phase 11/12 if public API stability becomes a concern. [VERIFIED: allowed particle -> importer dependency in `10-CONTEXT.md`]

2. **Should Phase 10 add a root legacy comparison adapter from `ParticleDefinition` to root `BrParticle`?**
   - What we know: Phase 10 may connect the seam only enough to prove conversion, but full loader/publication rewire is deferred. [VERIFIED: `10-CONTEXT.md`]
   - What's unclear: whether comparing against root `BrParticle.CODEC` for all real fixtures will be possible without component registry setup. [ASSUMED]
   - Recommendation: prioritize importer -> particle definition parity; add root comparison only for fields that do not require Forge annotation component registry. [VERIFIED: root component manager requires Forge scan]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JetBrains MCP / IDE MCP | Required Gradle verification and Java semantic search | ✓ | Tools available; IDE index not in dumb mode | None; required by project rules. [VERIFIED: `ide_index_status`, AGENTS] |
| Linked Gradle project in JetBrains | Gradle task execution via MCP | △ | Sync exited 0, but `jetbrain_list_gradle_tasks` still returned no tasks | Planner should call JetBrains Gradle task execution directly or resync before validation; do not use shell Gradle. [VERIFIED: `jetbrain_sync_gradle_projects`, `jetbrain_list_gradle_tasks`] |
| Java runtime | Local compile/test runtime | ✓ | OpenJDK 21.0.10 installed; project target is Java 17 | Gradle toolchain may supply Java 17; verify through JetBrains compile. [VERIFIED: `java -version`, Gradle files] |
| Large real particle fixture | Adapter parity tests | ✓ | `microsoft-shapeshifter/.../particles/witchspell.json` | If cross-module resource access is awkward, copy a fixture subset with SOURCE note. [VERIFIED: file glob] |
| Knowledge graph | Optional relationship discovery | ✗ | `.planning/graphs/graph.json` absent | Use source/docs/IDE search. [VERIFIED: graph status check] |

**Missing dependencies with no fallback:** none for research. [VERIFIED: environment probes]

**Missing dependencies with fallback:** knowledge graph absent; source/docs/IDE search used instead. [VERIFIED: graph status check]

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter via `org.junit:junit-bom:5.10.2` [VERIFIED: Gradle files] |
| Config file | Gradle `tasks.named('test').configure { useJUnitPlatform() }` in root/importer/particle builds [VERIFIED: Gradle files] |
| Quick run command | JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionBoundaryTest` [VERIFIED: project Gradle/JUnit pattern; command proposed] |
| Full suite command | JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :eyelib-importer:test :compileJava` [VERIFIED: Phase 9 verification command style; command proposed] |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PSCHEMA-01 | Canonical owner docs name importer schema, particle runtime definition, and root legacy status. | static/doc unit | JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionDocumentationTest` | ❌ Wave 0 |
| PSCHEMA-02 | Adapter creates `ParticleDefinition` from importer `BrParticle` and preserves identifier/render params/curves/events/components/flipbook. | unit + fixture parity | JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest` | ❌ Wave 0 |
| PSCHEMA-03 | Duplicate ownership cannot drift: no `BrParticle` in particle module, no forbidden imports, mapped-field invariant documented/tested. | static + parity | JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionBoundaryTest` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** JetBrains MCP targeted `:eyelib-particle:test --tests ...` for the changed adapter/boundary test. [VERIFIED: project MCP rule]
- **Per wave merge:** JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :compileJava`. [VERIFIED: Phase 9 verification style]
- **Phase gate:** JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :eyelib-importer:test :compileJava`; do not run shell Gradle. [VERIFIED: AGENTS]

### Wave 0 Gaps
- [ ] `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` — covers PSCHEMA-01/02.
- [ ] `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` — covers PSCHEMA-02.
- [ ] `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` — documents ownership/boundary invariants for PSCHEMA-01/03.
- [ ] `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java` — real fixture parity for PSCHEMA-02.
- [ ] `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` — forbidden imports/no duplicate `BrParticle` for PSCHEMA-03.
- [ ] `eyelib-particle/build.gradle` dependency update — add `implementation project(':eyelib-importer')` if adapter uses importer types; verify no root dependency. [VERIFIED: current particle Gradle lacks importer dependency]

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth/session surface in phase. [VERIFIED: phase requirements/context] |
| V3 Session Management | no | No session surface in phase. [VERIFIED: phase requirements/context] |
| V4 Access Control | no | No user authorization surface in phase. [VERIFIED: phase requirements/context] |
| V5 Input Validation | yes | Use importer `Codec` and adapter `DataResult`/explicit exception for invalid/missing required fields. [VERIFIED: `10-CONTEXT.md`, importer `BrParticle.java`] |
| V6 Cryptography | no | No crypto surface in phase. [VERIFIED: phase requirements/context] |

### Known Threat Patterns for Java/Codec Schema Adapter

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Malformed particle JSON causes silent partial runtime definition | Tampering | Fail loudly with `DataResult.error`/exception and tests for missing required fields. [VERIFIED: `10-CONTEXT.md`] |
| Unbounded fixture/addon parsing in unit tests becomes flaky/slow | Denial of Service | Use selected real fixture(s), not whole pack traversal, for adapter unit tests. [ASSUMED] |
| Platform class leakage into pure module causes dedicated-server classloading risk later | Elevation/DoS | Static forbidden-import tests for root/MC/Forge imports in `:eyelib-particle`. [VERIFIED: `09-VERIFICATION.md`, `docs/architecture/02-side-boundaries.md`] |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/10-schema-runtime-ownership-adapter/10-CONTEXT.md` — locked Phase 10 ownership, adapter scope, drift-prevention decisions.
- `.planning/REQUIREMENTS.md` — PSCHEMA-01/02/03 requirements and traceability.
- `.planning/ROADMAP.md` / `.planning/STATE.md` — Phase 10 position and later Phase 11/12/13 deferrals.
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md` — project constraints and module/side boundaries.
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` — canonical importer/raw particle schema and codec behavior.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java`, `BrParticleEmitter.java`, `BrParticleParticle.java`, component manager/files — root legacy runtime/schema behavior.
- `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` — current loader path.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticlePublisher.java`, package docs, README — Phase 9 particle module API/store seam.
- `eyelib-importer/src/test/resources/.../microsoft-shapeshifter/.../particles/witchspell.json` — available large real particle fixture.

### Secondary (MEDIUM confidence)
- Existing Phase 9 verification report — verified patterns for boundary/source-scan tests and JetBrains MCP validation command style.

### Tertiary (LOW confidence)
- None used as authoritative external research; all architecture findings came from repository docs/source.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — project Gradle files and source were read directly.
- Architecture: HIGH — locked context, requirements, MODULES, and architecture docs agree.
- Pitfalls: MEDIUM-HIGH — root/importer code verifies most pitfalls; exact final parity assertions need implementation feedback.

**Research date:** 2026-05-09  
**Valid until:** 2026-06-08 for project-local boundaries; re-check immediately if Phase 11/12 changes land first.
