# Phase 9: Particle API & Store Seam - Research

**Researched:** 2026-05-09 [VERIFIED: system date]
**Domain:** Java 17 + Forge particle module API/store seam in a multi-project Gradle codebase [VERIFIED: MODULES.md; build.gradle]
**Confidence:** HIGH for project-local architecture and existing code; MEDIUM for final API shape because Phase 10-13 ownership remains intentionally deferred [VERIFIED: 09-CONTEXT.md; ROADMAP.md]

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
## Implementation Decisions

### API Surface Shape
- New root-consumed particle API should live under `io.github.tt432.eyelibparticle.api` so the dependency direction is explicit and discoverable.
- Existing root facades may remain only as transitional compatibility facades; they must delegate to particle-module APIs and document why they exist.
- API keys should be string-first. Root/MC boundaries adapt `ResourceLocation` to strings so the existing string-keyed packet boundary remains intact.
- Phase 9 should not move Bedrock runtime/render implementation. It should extract lookup/store/spawn/publication seams only; runtime extraction remains Phase 11.

### Store And Publication Ownership
- `:eyelib-particle` should own the particle store port/API. Root `ParticleManager` becomes a transitional adapter rather than the canonical owner.
- Particle publication should route through a particle-module publisher/registry seam; root loader/registry code delegates into that seam.
- `replaceParticles` must continue keying entries by `particle_effect.description.identifier`, not resource path or incidental source key.
- Lifecycle/reset behavior should be exposed through narrow particle store/lifecycle APIs; MC/client hooks stay in root adapters until later phases explicitly move them.

### Transitional Facade And Verification Boundary
- Every root facade retained in Phase 9 must be documented as transitional via README/Javadoc or equivalent local documentation, including removal conditions.
- Verification should include compile plus boundary/static checks proving root facades contain delegation rather than business logic and particle APIs do not depend back on root runtime packages.
- Do not introduce a broad compatibility layer. Only specific, named, deletable facades are acceptable.
- Phase 9 should stabilize only lookup/store/spawn/publication contracts needed by later phases; schema/runtime internals remain deferred to Phases 10-13.

### the agent's Discretion
Implementation details not covered above are at the agent's discretion, provided they preserve Phase 8 dependency direction and avoid silent design degradation.

### Deferred Ideas (OUT OF SCOPE)
## Deferred Ideas

Schema/runtime conversion ownership is deferred to Phase 10. Bedrock runtime/render extraction is deferred to Phase 11. Loader publication rewiring is deferred to Phase 12 except for any minimal delegation seam required by Phase 9. Command/network integration behavior changes are deferred to Phase 13.
</user_constraints>

## Summary

Phase 9 should create `:eyelib-particle` owned API seams under `io.github.tt432.eyelibparticle.api` and turn current root particle surfaces into documented transitional adapters. [VERIFIED: 09-CONTEXT.md; eyelib-particle README] The safest plan is additive and delegation-first: introduce particle-module interfaces/classes for lookup, mutable store, publication, lifecycle/reset, and spawn/remove request routing, then rewire root `ParticleLookup`, `ParticleManager`, `ParticleAssetRegistry`, and `ParticleSpawnService` to delegate without moving Bedrock runtime/render internals. [VERIFIED: 09-CONTEXT.md; ParticleLookup.java; ParticleManager.java; ParticleAssetRegistry.java; ParticleSpawnService.java]

The existing store is still root-owned through `ParticleManager extends Manager<BrParticle>`, with `ManagerReadPort`/`ManagerWritePort` providing `get`, `getAllData`, `put`, `replaceAll`, and `clear`. [VERIFIED: ParticleManager.java; Manager.java; ManagerReadPort.java; ManagerWritePort.java] The existing publication seam already preserves the critical identifier behavior by flattening every particle under `particle.particleEffect().description().identifier()`, so Phase 9 must preserve that rule inside the new particle-module publisher/store API. [VERIFIED: ParticleAssetRegistry.java; 09-CONTEXT.md]

**Primary recommendation:** Plan one narrow API-store slice in `:eyelib-particle` plus root delegation adapters and boundary tests; do not move `BrParticle`, `BrParticleEmitter`, `BrParticleRenderManager`, packets, loaders, or Forge hooks in this phase. [VERIFIED: 09-CONTEXT.md; docs/architecture/01-module-boundaries.md]

## Project Constraints (from AGENTS.md)

- Read `docs/index/repo-map.md`, `MODULES.md`, relevant architecture docs, and nearest package README before code work. [VERIFIED: AGENTS.md]
- Preserve existing manager, loader, visitor, and codec patterns. [VERIFIED: AGENTS.md; docs/architecture/01-module-boundaries.md]
- Prefer narrow, stage-scoped edits and do not touch unrelated uncommitted changes. [VERIFIED: AGENTS.md]
- Before each change, identify affected modules in `MODULES.md`; update `MODULES.md` if responsibilities, paths, or interactions change. [VERIFIED: AGENTS.md; MODULES.md]
- If module boundaries change, update `docs/architecture/01-module-boundaries.md` and relevant package README files in the same change. [VERIFIED: AGENTS.md; MODULES.md]
- IntelliJ IDEA is the sole IDE; do not add VS Code/Eclipse/JDTLS artifacts. [VERIFIED: AGENTS.md]
- All Gradle commands must run through JetBrains MCP Gradle tools, never shell Gradle. [VERIFIED: AGENTS.md; eyelib-particle README]
- `:eyelib-particle` must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes. [VERIFIED: eyelib-particle README; package-info.java]

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PAPI-01 | Root runtime can access particle lookup, spawn/remove, store/publication, and initialization behavior through narrow particle-module APIs instead of owning particle internals directly. [VERIFIED: REQUIREMENTS.md] | Introduce `eyelibparticle.api` seams for read/store/write/publication/lifecycle/spawn routing; root delegates from current facades. [VERIFIED: 09-CONTEXT.md; ParticleLookup.java; ParticleSpawnService.java; ParticleAssetRegistry.java] |
| PAPI-03 | Any temporary root compatibility facade delegates to particle-module APIs and is documented as transitional. [VERIFIED: REQUIREMENTS.md] | Document retained root facades in local README/Javadoc and test or statically inspect delegation-only behavior. [VERIFIED: 09-CONTEXT.md; AGENTS.md] |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Particle API contracts | Particle module (`:eyelib-particle`) | Root runtime adapters | New root-consumed APIs are locked to `io.github.tt432.eyelibparticle.api`, while root may consume the module one-way. [VERIFIED: 09-CONTEXT.md; eyelib-particle README] |
| Particle lookup | Particle module API/store | Root transitional `ParticleLookup` | Current lookup reads `ParticleManager.readPort()` directly; target is module-owned read seam with root facade delegating. [VERIFIED: ParticleLookup.java; 09-CONTEXT.md] |
| Store and publication | Particle module store/publisher API | Root `ParticleManager`/`ParticleAssetRegistry` adapter | Current manager and registry own storage/publication; Phase 9 must invert canonical ownership without moving loader/runtime internals. [VERIFIED: ParticleManager.java; ParticleAssetRegistry.java; 09-CONTEXT.md] |
| Spawn/remove orchestration | Particle module API request seam | Root client/MC runtime adapter | `ParticleSpawnService` currently touches packet, Minecraft, capability, render manager, and emitter classes, so platform/runtime behavior must remain in root adapters while API seam is string-first. [VERIFIED: ParticleSpawnService.java; docs/architecture/02-side-boundaries.md] |
| Loader reload parsing | Root loader for now | Particle publication seam | `BrParticleLoader` parses resources and calls `ParticleAssetRegistry.replaceParticles`; full loading rewire is deferred to Phase 12. [VERIFIED: BrParticleLoader.java; 09-CONTEXT.md; ROADMAP.md] |
| Bedrock runtime/render internals | Root client particle runtime for now | Future particle module runtime | `BrParticle`, `BrParticleEmitter`, and `BrParticleRenderManager` remain root-owned until later phases. [VERIFIED: docs/architecture/01-module-boundaries.md; BrParticle.java; BrParticleEmitter.java; BrParticleRenderManager.java] |

## Standard Stack

### Core
| Library / Pattern | Version | Purpose | Why Standard |
|-------------------|---------|---------|--------------|
| Java toolchain | 17 | Source/target runtime for root and particle module | Root and particle builds set `JavaLanguageVersion.of(17)`. [VERIFIED: build.gradle; eyelib-particle/build.gradle] |
| Gradle multi-project module | current repo wiring | One-way root consumption of `:eyelib-particle` | Phase 8 registered and wired `:eyelib-particle`; root depends through `api`, `modImplementation`, and `jarJar`. [VERIFIED: 08-01-SUMMARY.md; build.gradle] |
| LegacyForge Gradle plugin | 2.0.91 | Forge-visible module metadata/build integration | Root and particle module both apply `net.neoforged.moddev.legacyforge` 2.0.91. [VERIFIED: build.gradle; eyelib-particle/build.gradle] |
| JSpecify | 1.0.0 | Nullness annotations and `@NullMarked` packages | Particle module declares `compileOnly 'org.jspecify:jspecify:1.0.0'` and package marker. [VERIFIED: eyelib-particle/build.gradle; package-info.java] |
| JUnit Jupiter | 5.10.2 BOM | Unit and boundary tests | Root and particle build files declare JUnit Jupiter; existing tests cover lookup, packets, managers, and request seams. [VERIFIED: build.gradle; eyelib-particle/build.gradle; test file search] |

### Supporting
| Library / Pattern | Version | Purpose | When to Use |
|-------------------|---------|---------|-------------|
| Lombok | 8.6 plugin | Existing no-args/private constructor pattern for static facades | Use only where existing project pattern already does, e.g. static utility facades. [VERIFIED: build.gradle; ParticleLookup.java; ParticleSpawnService.java; ParticleAssetRegistry.java] |
| Manager-style ports | project-local | Current root store read/write behavior | Reuse behavior but do not make `:eyelib-particle` import root `ManagerReadPort`/`ManagerWritePort`. [VERIFIED: ManagerReadPort.java; ManagerWritePort.java; 09-CONTEXT.md] |
| String-keyed IDs | project-local | Particle API and packet boundary | Preserve `String` ids and adapt `ResourceLocation` only in root/MC adapters. [VERIFIED: 09-CONTEXT.md; SpawnParticlePacketTest.java; ParticleSpawnRequest.java] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| New particle-module store interfaces | Reuse root `ManagerReadPort`/`ManagerWritePort` | Not acceptable because particle module must not depend back on root runtime manager packages. [VERIFIED: eyelib-particle README; 09-CONTEXT.md] |
| String-first API | `ResourceLocation`-first API | Not acceptable for Phase 9 because locked decisions require string-first API and MC boundary adaptation. [VERIFIED: 09-CONTEXT.md] |
| Delegation facades | Broad compatibility layer | Not acceptable because Phase 9 permits only specific, named, deletable facades. [VERIFIED: 09-CONTEXT.md; REQUIREMENTS.md] |

**Installation:** No dependency installation is required for Phase 9; `:eyelib-particle` already exists and is consumed by root. [VERIFIED: 08-01-SUMMARY.md; build.gradle]

**Version verification:** Versions were verified from checked-in Gradle files rather than package registries because this is a Gradle/Java project, not an npm package phase. [VERIFIED: build.gradle; eyelib-particle/build.gradle]

## Architecture Patterns

### System Architecture Diagram

```text
Resource reload / tooling import
        |
        v
Root loader/tooling parses BrParticle maps [root, unchanged in Phase 9]
        |
        v
Root transitional ParticleAssetRegistry facade
        |
        v
:eyelib-particle api publisher/store seam (string-keyed, identifier-flattening)
        |
        v
Particle store read/write/lifecycle API
        |
        +--> Root ParticleLookup facade delegates reads
        |
        +--> Command suggestions / animation runtime reads names/get

Network SpawnParticlePacket / RemoveParticlePacket (string ids)
        |
        v
Root NetClientHandlers
        |
        v
Root transitional ParticleSpawnService adapter
        |
        v
:eyelib-particle spawn/remove API request seam
        |
        v
Root runtime adapter still constructs BrParticleEmitter and calls BrParticleRenderManager
```

All root/MC/Forge types remain outside pure particle-module API contracts in Phase 9. [VERIFIED: 09-CONTEXT.md; docs/architecture/02-side-boundaries.md]

### Recommended Project Structure

```text
eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/
├── api/                    # root-consumed particle API/store/spawn contracts [VERIFIED: 09-CONTEXT.md]
│   ├── ParticleApi.java     # optional service facade/composition entrypoint [ASSUMED]
│   ├── ParticleLookupApi.java
│   ├── ParticleStore.java
│   ├── ParticlePublisher.java
│   └── ParticleLifecycle.java
└── README.md               # update with Phase 9 ownership/delegation rules [VERIFIED: AGENTS.md]

src/main/java/io/github/tt432/eyelib/client/particle/
├── ParticleLookup.java       # transitional facade delegating into particle API [VERIFIED: 09-CONTEXT.md]
├── ParticleSpawnService.java # root client/runtime adapter; delegates seam and keeps MC/runtime internals [VERIFIED: ParticleSpawnService.java]
└── README.md                 # document transitional root facades [VERIFIED: 09-CONTEXT.md]

└── ParticleAssetRegistry.java # transitional publication facade delegating into particle publisher [VERIFIED: ParticleAssetRegistry.java]

└── ParticleManager.java       # transitional store adapter, not canonical owner [VERIFIED: 09-CONTEXT.md]
```

### Pattern 1: Module-owned store ports, root-backed implementation
**What:** Define particle-module read/write/lifecycle interfaces that use `String` keys and no root package types. [VERIFIED: 09-CONTEXT.md; eyelib-particle README]
**When to use:** Use for lookup names/get, publication replacement, single publish, and lifecycle reset/clear. [VERIFIED: REQUIREMENTS.md; ManagerReadPort.java; ManagerWritePort.java]
**Example:**
```java
// Source: project-local pattern from ManagerReadPort/ManagerWritePort, adapted to particle module constraints.
// [VERIFIED: ManagerReadPort.java; ManagerWritePort.java; eyelib-particle README]
public interface ParticleStore<T> {
    @Nullable T get(String id);
    Map<String, T> all();
    void put(String id, T particle);
    void replaceAll(Map<String, ? extends T> replacement);
    void clear();
}
```

### Pattern 2: Publication seam owns identifier flattening
**What:** Put `description.identifier` flattening behind a named publisher method so loaders/tooling do not duplicate key selection. [VERIFIED: ParticleAssetRegistry.java; 09-CONTEXT.md]
**When to use:** Use whenever replacing or publishing parsed particle definitions. [VERIFIED: BrParticleLoader.java; ManagerResourceImportPlanner references]
**Example:**
```java
// Source: current ParticleAssetRegistry behavior, moved behind module-owned publisher seam.
// [VERIFIED: ParticleAssetRegistry.java]
particles.forEach((ignored, particle) ->
        flattened.put(particle.particleEffect().description().identifier(), particle));
store.replaceAll(flattened);
```

### Pattern 3: Transitional root facades are deletion-ready adapters
**What:** Keep root static classes only if they delegate into `eyelibparticle.api` and document removal conditions. [VERIFIED: 09-CONTEXT.md]
**When to use:** Use for existing callers of `ParticleLookup`, `ParticleAssetRegistry`, `ParticleManager`, and `ParticleSpawnService`. [VERIFIED: text search results]
**Example:**
```java
// Source: required Phase 9 delegation pattern; exact class names should be finalized in plan.
// [VERIFIED: 09-CONTEXT.md] [ASSUMED: class names]
/** Transitional facade. Remove after root callers migrate to io.github.tt432.eyelibparticle.api. */
public final class ParticleLookup {
    public static Collection<String> names() {
        return ParticleApis.lookup().names();
    }
}
```

### Anti-Patterns to Avoid
- **Root-owned canonical store:** `ParticleManager` must not remain the canonical owner after Phase 9; it should be a transitional adapter. [VERIFIED: 09-CONTEXT.md]
- **Root imports in `:eyelib-particle`:** Any import from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` violates the module contract. [VERIFIED: eyelib-particle README]
- **Moving Bedrock runtime/render early:** Moving `BrParticleRenderManager`, emitters, loader behavior, or packet behavior belongs to later phases, not Phase 9. [VERIFIED: 09-CONTEXT.md; ROADMAP.md]
- **ResourceLocation in pure API:** `ResourceLocation` belongs at MC/root boundary adapters, not the new string-first API. [VERIFIED: 09-CONTEXT.md]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Generic manager/event system | A new global registry/event bus | Narrow particle store/publisher API backed by existing manager behavior | Existing `Manager` already handles storage and publish-on-put behavior; Phase 9 only needs ownership seam inversion. [VERIFIED: Manager.java; 09-CONTEXT.md] |
| ID parsing/validation | New parser in particle core | `String` ids in API; `ResourceLocation` validation only in root/MC adapters | Locked decision keeps API string-first and packet boundary string-keyed. [VERIFIED: 09-CONTEXT.md; EyelibParticleCommand.java] |
| Runtime renderer abstraction | New particle engine/render manager | Existing `BrParticleRenderManager` via root adapter | Runtime/render extraction is Phase 11 and current manager has Forge/Minecraft hooks. [VERIFIED: BrParticleRenderManager.java; ROADMAP.md] |
| Broad compatibility layer | Catch-all facade package | Specific documented transitional facades | Requirement PAPI-03 requires named delegation facades and documentation. [VERIFIED: REQUIREMENTS.md; 09-CONTEXT.md] |

**Key insight:** The complex part is not inventing new behavior; it is moving canonical ownership of API/store/publication seams while preserving existing root runtime behavior and dependency direction. [VERIFIED: 09-CONTEXT.md; existing code files]

## Common Pitfalls

### Pitfall 1: Accidentally importing root packages into `:eyelib-particle`
**What goes wrong:** The module compiles only because root classes leak into API design, violating PAPI-02 and Phase 8 boundary contract. [VERIFIED: REQUIREMENTS.md; eyelib-particle README]
**Why it happens:** Current useful types (`BrParticle`, `ManagerReadPort`, `SpawnParticlePacket`, capability helpers) live in root packages. [VERIFIED: ParticleLookup.java; ParticleSpawnService.java; ParticleManager.java]
**How to avoid:** Keep particle API generic or platform-light and bind root runtime types only in root adapters. [VERIFIED: 09-CONTEXT.md]
**Warning signs:** Any `io.github.tt432.eyelib.client`, `network`, `capability`, or `mc.impl` import under `eyelib-particle/src/main/java`. [VERIFIED: eyelib-particle README]

### Pitfall 2: Losing `description.identifier` publication keys
**What goes wrong:** Particles are keyed by resource path/source key instead of Bedrock `particle_effect.description.identifier`. [VERIFIED: ParticleAssetRegistry.java; 09-CONTEXT.md]
**Why it happens:** Loader maps are currently keyed by `ResourceLocation`, but registry publication intentionally ignores source keys. [VERIFIED: BrParticleLoader.java; ParticleAssetRegistry.java]
**How to avoid:** Make identifier extraction a named publisher responsibility and test replacement with mismatched source key vs description identifier. [VERIFIED: 09-CONTEXT.md]
**Warning signs:** New code uses loader map keys or `ResourceLocation.toString()` as publication key. [VERIFIED: BrParticleLoader.java; ParticleAssetRegistry.java]

### Pitfall 3: Moving runtime/render concerns before their phase
**What goes wrong:** API/store work pulls `Minecraft`, `Level`, `RenderLevelStageEvent`, capability helpers, or emitter construction into particle module core. [VERIFIED: ParticleSpawnService.java; BrParticleRenderManager.java; BrParticleEmitter.java]
**Why it happens:** Spawn/remove behavior currently mixes API request handling with client runtime wiring. [VERIFIED: ParticleSpawnService.java]
**How to avoid:** Define spawn/remove API as request/port seams and keep concrete emitter construction in root adapter until Phase 11. [VERIFIED: 09-CONTEXT.md; ROADMAP.md]
**Warning signs:** `:eyelib-particle` imports Minecraft/Forge runtime classes or root capability helpers. [VERIFIED: eyelib-particle README]

### Pitfall 4: Undocumented compatibility facades
**What goes wrong:** Future maintainers cannot tell which root facades are temporary or when to remove them. [VERIFIED: PAPI-03 in REQUIREMENTS.md]
**Why it happens:** Static root facades already look canonical. [VERIFIED: ParticleLookup.java; ParticleSpawnService.java; ParticleAssetRegistry.java]
**How to avoid:** Add Javadoc and package README entries for each retained facade with purpose and removal condition. [VERIFIED: 09-CONTEXT.md]
**Warning signs:** Root facade contains business logic or lacks "transitional" documentation. [VERIFIED: 09-CONTEXT.md]

## Code Examples

### Current lookup seam to preserve behavior through delegation
```java
// Source: src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java [VERIFIED: file read]
public static @Nullable BrParticle get(String id) {
    return ParticleManager.readPort().get(id);
}

public static Collection<String> names() {
    return ParticleManager.readPort().getAllData().keySet();
}
```

### Current identifier-flattening publication rule
```java
// Source: src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java [VERIFIED: file read]
particles.forEach((ignored, particle) ->
        flattened.put(particle.particleEffect().description().identifier(), particle));
ParticleManager.writePort().replaceAll(flattened);
```

### Current network-to-spawn service delegation
```java
// Source: src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java [VERIFIED: file read]
public static void onRemoveParticlePacket(RemoveParticlePacket packet) {
    ParticleSpawnService.removeEmitter(packet.removeId());
}

public static void onSpawnParticlePacket(SpawnParticlePacket packet) {
    ParticleSpawnService.spawnFromPacket(packet);
}
```

## State of the Art

| Old Approach | Current / Phase 9 Approach | When Changed | Impact |
|--------------|----------------------------|--------------|--------|
| Root bootstrap/manager reach-through | Domain-local lookup seams such as `ParticleLookup` | Already in current boundary docs | Phase 9 should make particle-module API canonical instead of adding bootstrap reach-through. [VERIFIED: docs/architecture/01-module-boundaries.md; ParticleLookup.java] |
| Root-only particle storage | Particle-module store/publisher API with root adapter | Phase 9 target | Enables root to consume particle capabilities without owning internals directly. [VERIFIED: ROADMAP.md; 09-CONTEXT.md] |
| `ResourceLocation` packet particle id | String-keyed packet/request seam | Already landed before Phase 9 | New API should preserve string-first boundary. [VERIFIED: docs/architecture/02-side-boundaries.md; SpawnParticlePacketTest.java; ParticleSpawnRequest.java] |
| Particle module as skeleton only | Particle module owns API/store seams | Phase 9 target | Starts true module ownership without moving runtime/render internals. [VERIFIED: 08-02-SUMMARY.md; 09-CONTEXT.md] |

**Deprecated/outdated:** Treating `ParticleManager`/`ParticleAssetRegistry` as the long-term canonical particle store/publication owner is outdated for Phase 9; they should become transitional adapters. [VERIFIED: 09-CONTEXT.md]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `ParticleApis` / `ParticleApi.java` may be a useful service-composition entrypoint name. [ASSUMED] | Recommended Project Structure / Pattern 3 | Low: planner can choose different names while preserving package and seam responsibilities. |

## Open Questions

1. **Should particle API be generic over particle definition type or expose root runtime `BrParticle` through an adapter-only binding?** [VERIFIED: current `BrParticle` is root-owned]
   - What we know: `:eyelib-particle` cannot import root `BrParticle`, but lookup/store currently stores root `client.particle.bedrock.BrParticle`. [VERIFIED: ParticleManager.java; eyelib-particle README]
   - What's unclear: Whether Phase 9 should use generic interfaces (`ParticleStore<T>`) or introduce a platform-light particle definition view before Phase 10. [ASSUMED]
   - Recommendation: Prefer generic/store port contracts for Phase 9 and defer canonical schema/runtime type ownership to Phase 10. [VERIFIED: 09-CONTEXT.md; ROADMAP.md]

2. **How should service instances be wired?** [ASSUMED]
   - What we know: Existing root facades are static utility-style classes. [VERIFIED: ParticleLookup.java; ParticleSpawnService.java; ParticleAssetRegistry.java]
   - What's unclear: Whether to use static module API holder, explicit service instance, or dependency injection-like binder. [ASSUMED]
   - Recommendation: Use the smallest static or final service holder needed to preserve existing call sites, but keep root-specific binding outside pure API contracts. [VERIFIED: existing static facade pattern]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JetBrains MCP Gradle tools | Required compile/test verification path | ✓ | MCP available; Gradle sync exited 0 | None; shell Gradle is forbidden. [VERIFIED: jetbrain_sync_gradle_projects; AGENTS.md] |
| Java runtime | Gradle/IDE compile toolchain | ✓ | OpenJDK 21.0.10 installed; project targets Java 17 via toolchain | Use Gradle toolchain resolution through JetBrains MCP. [VERIFIED: `java -version`; build.gradle] |
| Gradle task index in JetBrains MCP | Task discovery | ⚠ | Sync succeeded, but task detail lookup returned no matches | Planner should use known JetBrains MCP task invocations from Phase 8 summaries and verify task availability at execution start. [VERIFIED: jetbrain_get_gradle_task_detail; 08-01-SUMMARY.md] |

**Missing dependencies with no fallback:** None identified for planning; shell Gradle remains prohibited rather than a fallback. [VERIFIED: AGENTS.md]

**Missing dependencies with fallback:** Gradle task discovery through MCP returned no matches after sync; known task names from Phase 8 can still be invoked via `jetbrain_run_gradle_tasks` during execution. [VERIFIED: 08-01-SUMMARY.md; jetbrain_get_gradle_task_detail]

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter with BOM 5.10.2 for root and particle module tests. [VERIFIED: build.gradle; eyelib-particle/build.gradle] |
| Config file | Gradle build files configure `useJUnitPlatform()`; no separate JUnit config file was identified in supplied/read files. [VERIFIED: build.gradle; eyelib-particle/build.gradle] |
| Quick run command | Use JetBrains MCP `jetbrain_run_gradle_tasks` with targeted test task(s), not shell Gradle. [VERIFIED: AGENTS.md] |
| Full suite command | Use JetBrains MCP `jetbrain_run_gradle_tasks` for `:eyelib-particle:compileJava`, `:compileJava`, and relevant tests. [VERIFIED: 08-01-SUMMARY.md; AGENTS.md] |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PAPI-01 | Particle lookup/store/publication API is root-consumable and string-keyed. [VERIFIED: REQUIREMENTS.md] | unit/boundary | JetBrains MCP Gradle run for root tests covering new seam. [VERIFIED: AGENTS.md] | ❌ Wave 0: add focused seam test(s). |
| PAPI-01 | `replaceParticles` continues to key by `particle_effect.description.identifier`. [VERIFIED: 09-CONTEXT.md] | unit/regression | JetBrains MCP Gradle run for registry/publisher tests. [VERIFIED: AGENTS.md] | ❌ Wave 0: add particle publisher/store test. |
| PAPI-01 | Spawn/remove packet path delegates through particle API seam while runtime stays root adapter. [VERIFIED: NetClientHandlers.java; ParticleSpawnService.java] | unit/static boundary | JetBrains MCP Gradle run for root tests plus static import checks. [VERIFIED: AGENTS.md] | ⚠ Existing `SpawnParticlePacketTest` and `ParticleSpawnRequestTest`; add delegation test or static check. [VERIFIED: test files] |
| PAPI-03 | Root compatibility facades are documented and delegation-only. [VERIFIED: REQUIREMENTS.md] | docs/static boundary | Content/static checks plus compile through JetBrains MCP. [VERIFIED: 09-CONTEXT.md] | ❌ Wave 0: add or script boundary checks in plan. |

### Sampling Rate
- **Per task commit:** JetBrains MCP compile for touched module when code changes; docs-only tasks verify referenced paths. [VERIFIED: AGENTS.md]
- **Per wave merge:** JetBrains MCP `:eyelib-particle:compileJava` and root `:compileJava` equivalents, matching Phase 8 precedent. [VERIFIED: 08-01-SUMMARY.md]
- **Phase gate:** Compile plus targeted unit/boundary tests and static checks proving no reverse particle dependency and documented transitional facades. [VERIFIED: 09-CONTEXT.md]

### Wave 0 Gaps
- [ ] `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/api/...` — covers module API/store contracts for PAPI-01. [VERIFIED: no particle-module tests found in test file search]
- [ ] Root test for `ParticleAssetRegistry` or new publisher adapter — covers identifier-key replacement for PAPI-01. [VERIFIED: ParticleAssetRegistry.java]
- [ ] Static boundary check for forbidden imports under `eyelib-particle/src/main/java` — covers PAPI-02 preservation while planning PAPI-01/PAPI-03. [VERIFIED: eyelib-particle README]
- [ ] Documentation/static check that every retained root facade says transitional and names removal condition — covers PAPI-03. [VERIFIED: 09-CONTEXT.md]

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No authentication behavior is in Phase 9 scope. [VERIFIED: 09-CONTEXT.md; REQUIREMENTS.md] |
| V3 Session Management | no | No session behavior is in Phase 9 scope. [VERIFIED: 09-CONTEXT.md; REQUIREMENTS.md] |
| V4 Access Control | no | No authorization boundary is introduced; root/MC command behavior remains later-phase/root adapter scope. [VERIFIED: ROADMAP.md] |
| V5 Input Validation | yes | Preserve string-keyed IDs and keep `ResourceLocation` validation/adaptation in MC/root adapters. [VERIFIED: 09-CONTEXT.md; EyelibParticleCommand.java] |
| V6 Cryptography | no | No cryptography behavior is in Phase 9 scope. [VERIFIED: 09-CONTEXT.md; REQUIREMENTS.md] |

### Known Threat Patterns for This Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Identifier spoofing or malformed IDs crossing MC boundary | Tampering | Keep API string-first but validate `ResourceLocation` only in root/MC command/network adapters. [VERIFIED: 09-CONTEXT.md; EyelibParticleCommand.java] |
| Client-only classloading leakage into shared/module code | Denial of Service | Keep pure particle API root/MC/Forge-clean and leave runtime classes in root adapters. [VERIFIED: docs/architecture/02-side-boundaries.md; eyelib-particle README] |
| Hidden broad compatibility facade keeps stale business logic alive | Maintainability/security drift | Require named transitional documentation and delegation-only static checks. [VERIFIED: 09-CONTEXT.md; REQUIREMENTS.md] |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/09-particle-api-store-seam/09-CONTEXT.md` — locked decisions, deferred scope, existing code insights. [VERIFIED: file read]
- `.planning/REQUIREMENTS.md` — PAPI-01/PAPI-03 and milestone constraints. [VERIFIED: file read]
- `.planning/ROADMAP.md` and `.planning/STATE.md` — Phase 9 placement and later phase boundaries. [VERIFIED: file read]
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md` — project constraints and module/side rules. [VERIFIED: file read]
- Phase 8 summaries `08-01-SUMMARY.md` and `08-02-SUMMARY.md` — established Gradle skeleton and boundary documentation. [VERIFIED: file read]
- Code files: `ParticleLookup.java`, `ParticleSpawnService.java`, `ParticleManager.java`, `ParticleAssetRegistry.java`, `Manager*.java`, `BrParticleLoader.java`, `NetClientHandlers.java`, `EyelibParticleCommand.java`, particle runtime classes, and current tests. [VERIFIED: file read; JetBrains search]
- JetBrains MCP availability and Gradle sync result. [VERIFIED: jetbrain_sync_gradle_projects]

### Secondary (MEDIUM confidence)
- JetBrains text search for usages of `ParticleLookup`, `ParticleSpawnService`, `ParticleAssetRegistry`, `ParticleManager`, `replaceParticles`, and `spawnFromPacket`; results are index-backed but not semantic references due IDE reference tool parameter limitations in this session. [VERIFIED: JetBrains search]

### Tertiary (LOW confidence)
- API class naming suggestions such as `ParticleApis` are design assumptions, not verified project decisions. [ASSUMED]

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — checked build files and Phase 8 summaries. [VERIFIED: build.gradle; eyelib-particle/build.gradle; 08 summaries]
- Architecture: HIGH — locked decisions and architecture docs are explicit; source files confirm current seams. [VERIFIED: 09-CONTEXT.md; docs/architecture; code files]
- Pitfalls: HIGH — derived from explicit constraints and current import/dependency hotspots. [VERIFIED: eyelib-particle README; ParticleSpawnService.java; ParticleAssetRegistry.java]
- Final API naming: LOW — only package and responsibilities are locked; exact class names remain planner discretion. [ASSUMED]

**Research date:** 2026-05-09 [VERIFIED: system date]
**Valid until:** 2026-06-08 unless Phase 10-13 decisions change particle ownership earlier. [ASSUMED]
