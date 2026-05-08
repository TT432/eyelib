# Pitfalls — `eyelib-particle` Module Separation

**Research type:** Project-specific extraction pitfalls  
**Milestone:** v1.2 true `:eyelib-particle` Gradle module separation  
**Researched:** 2026-05-09  
**Confidence:** HIGH — based on current project docs and inspected particle loader/runtime/command/network/tests.

## High-risk Pitfalls

### 1. Creating a Gradle module without moving ownership

**What goes wrong:** `include("eyelib-particle")` is added, but the real owners remain in root: `client/particle/**`, `client/manager/ParticleManager`, `client/registry/ParticleAssetRegistry`, `client/loader/BrParticleLoader`, command/network handlers, and Forge render/tick event subscribers. The result is a cosmetic module that root still owns through split responsibilities.

**Why this repo is exposed:** Current docs require a real module boundary, but particle behavior is spread across root runtime, `mc/impl`, network packets, manager publication, and importer schema.

**Consequences:** Later phases cannot tell whether particle schema/runtime/Forge binding belongs to root or `:eyelib-particle`; circular dependencies become tempting; zero-regression checks pass while architecture goal fails.

**Prevention:** Define `:eyelib-particle` ownership before moving code: particle runtime definitions, emitters, render manager, lookup/spawn service, and particle-local tests move together unless a file is explicitly documented as root/MC integration. Update `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and the particle README in the same phase.

---

### 2. Introducing root ↔ particle circular dependencies

**What goes wrong:** `:eyelib-particle` depends on root for `ParticleManager`, `DataAttachmentHelper`, packets, util classes, or bootstrap constants while root also depends on `:eyelib-particle` to consume particle behavior.

**Why this repo is exposed:** `ParticleSpawnService` currently imports root capability/data-attachment helpers and `SpawnParticlePacket`; `BrParticleEmitter` imports root MC timer/util helpers; `BrParticle` imports root codec/math utilities.

**Consequences:** Gradle module cannot build independently, or extraction stalls into broad utility churn. Worst case: particle code is copied instead of moved, causing divergent behavior.

**Prevention:** Use one-way dependency direction: root/MC integration consumes `:eyelib-particle`; `:eyelib-particle` may depend on already extracted modules (`:eyelib-molang`, `:eyelib-material`, attachment/utility modules if truly available as subprojects), but not root runtime packages. Any root-only helper needed by particles must either be moved to a true lower module first or wrapped behind a narrow port owned by root.

---

### 3. Schema/runtime duplication drift between importer and particle runtime

**What goes wrong:** The existing `eyelib-importer/.../particle/BrParticle` and runtime `client/particle/bedrock/BrParticle` continue evolving independently after extraction. Codec fixes, flipbook support, curve parsing, component names, or texture/material identifiers land in one copy only.

**Why this repo is exposed:** Importer `BrParticle` already stores generic component values and string texture fields; runtime `BrParticle` decodes dispatched runtime components and uses `ResourceLocation` for texture/component keys.

**Consequences:** Resource reload may parse one shape, manager import may parse another, and tests can miss real add-on cases. Runtime particles may render differently from importer-derived metadata.

**Prevention:** Make the phase explicitly decide the contract: importer owns raw/schema parsing; `:eyelib-particle` owns runtime component decoding and execution; adapters between them must be named and tested. Do not silently maintain two “source of truth” codecs. Add parity tests for identifier, curve-node alternatives, basic render parameters, and billboard flipbook metadata if both representations remain.

---

### 4. Breaking manager publication and lookup semantics

**What goes wrong:** Loader relocation changes keys or publication order. `BrParticleLoader` currently parses by resource key, then `ParticleAssetRegistry.replaceParticles` flattens by `particle_effect.description.identifier`; `ParticleLookup.names()` exposes manager keys for command suggestions.

**Why this repo is exposed:** The resource path key and particle description identifier can differ. Command suggestions and packet spawn lookup use the flattened string id, not necessarily the JSON file path.

**Consequences:** `/eyelib particle` suggestions disappear, a valid packet `particleId` no longer resolves, or duplicate identifiers overwrite unpredictably.

**Prevention:** Preserve the current publication contract unless a separate compatibility phase changes it: loaded particles are published under `particle_effect.description.identifier`, lookup accepts string ids, and suggestions reflect manager keys. Add/keep tests around `ParticleAssetRegistry.replaceParticles`, `ParticleLookup.names()`, and reload replacement behavior.

---

### 5. Re-contaminating platform-free seams with Minecraft identifiers

**What goes wrong:** Extraction “cleans up” string ids back to `ResourceLocation` in packet/request/common runtime because the particle module is Minecraft-facing anyway.

**Why this repo is exposed:** Existing boundary docs and tests intentionally made `ParticleSpawnRequest` and `SpawnParticlePacket` string-keyed. `SpawnParticlePacketTest` asserts non-ResourceLocation strings are carried unchanged.

**Consequences:** Current hard-quarantine progress is reversed; common/runtime tests fail or, worse, compile but reject previously preserved packet payloads. Command validation leaks into packet contracts instead of staying in `mc/impl`.

**Prevention:** Keep request/packet/common runtime seams string-keyed. ResourceLocation validation/adaptation remains in `mc/impl/common/command` and any MC transport boundary. Do not weaken `SpawnParticlePacketTest`, `ParticleSpawnRequestTest`, or `ParticleCommandRuntimeTest` to make extraction compile.

---

### 6. Moving Forge/client-only event subscribers into the wrong side

**What goes wrong:** `BrParticleRenderManager.ForgeEvents` is moved into `:eyelib-particle` without clear client-only Gradle/source-set and mod metadata handling, or particle runtime classes become reachable from common/server paths.

**Why this repo is exposed:** The render manager subscribes to `RenderTickEvent`, `ClientTickEvent`, `RenderLevelStageEvent`, and `ClientPlayerNetworkEvent` and directly uses `Minecraft`, `PoseStack`, render buffers, and material render types.

**Consequences:** Dedicated-server classloading crashes, Forge subscriber discovery misses the class, particles stop ticking/rendering, or logout cleanup no longer clears emitters/particles.

**Prevention:** Treat event wiring as client integration. Either keep Forge subscriber binding in root `mc/impl` and call particle-module services, or document `:eyelib-particle` as a client mod artifact with safe `Dist.CLIENT` subscription and resources. Verify tick, render, and logout cleanup after relocation.

---

### 7. Breaking network spawn/remove flow while moving packet ownership

**What goes wrong:** Packet classes, `NetClientHandlers`, or `ParticleSpawnService` move across modules in a way that makes network depend on loader internals or render manager directly.

**Why this repo is exposed:** Side-boundary docs explicitly require client packet handlers to call `ParticleSpawnService` and not `BrParticleRenderManager` directly. Spawn currently depends on player render data scope, client level, and packet payload.

**Consequences:** Spawn packets decode but do nothing, remove packets target the wrong manager instance, or client handlers gain forbidden rendering/loader dependencies.

**Prevention:** Preserve this chain: command builds string request → transport sends `SpawnParticlePacket` → client handler delegates to particle service → service resolves `ParticleLookup` and owns emitter spawn/remove. If packet classes stay in root `mc/impl/network/packet`, particle service should accept a module-owned request DTO or a tiny adapter should live in root to avoid making `:eyelib-particle` depend on root packet packages.

---

### 8. Losing Molang scope and RenderData parent-scope behavior

**What goes wrong:** `ParticleSpawnService` is simplified during extraction and no longer obtains `RenderData` from the client player, or `BrParticleEmitter` no longer sets the parent Molang scope.

**Why this repo is exposed:** Spawn currently creates `BrParticleEmitter(particle, data.getScope(), level, position)`. Emitter curves and variables rely on `MolangScope` and host context.

**Consequences:** Particles still appear in simple cases but expressions tied to entity/render data, curves, or emitter variables regress subtly.

**Prevention:** Keep a `ParticleSpawnContext`/port that provides parent Molang scope and level without making the particle module import root capability helpers. Add a test seam for spawn context construction and require manual/client smoke verification for Molang-driven particle samples.

---

### 9. Weakening defensive-copy and threading behavior

**What goes wrong:** Request/packet relocation changes `Vector3f` copying, or render manager state is mutated directly instead of through `Minecraft.getInstance().submit(...)`.

**Why this repo is exposed:** `ParticleSpawnRequestTest` asserts defensive copies. `BrParticleRenderManager.spawnEmitter/removeEmitter/spawnParticle` enqueue client-thread mutations.

**Consequences:** Packet/request positions can be mutated by callers; emitters/particles can race with render tick iteration; intermittent client crashes appear only under network load.

**Prevention:** Preserve defensive-copy tests and add thread-affinity review for every moved spawn/remove method. Do not replace queued mutations with direct map/list edits unless a stronger client-thread invariant is proven and documented.

---

### 10. Missing Gradle/resource metadata for a Forge subproject

**What goes wrong:** `:eyelib-particle` compiles as plain Java but lacks correct Forge/MDGL configuration, `mods.toml`, source/resource inclusion, or dependency declarations required for runtime discovery.

**Why this repo is exposed:** Existing subprojects have different roles (`:eyelib-importer`, `:eyelib-molang`, `:eyelib-material`, `:eyelib-processor`). Particle includes runtime client event behavior, so copying the wrong build pattern can produce a module that compiles but does not load.

**Consequences:** IntelliJ/Gradle compile passes; client run lacks particle subscriber or assets; root runtime has classpath-only behavior that differs from production packaging.

**Prevention:** Choose the build pattern deliberately: if `:eyelib-particle` is a Forge runtime module, mirror a runtime subproject pattern and verify it is on root runtime classpath; if it is a library module, keep Forge event binding in root `mc/impl`. Document the choice in `MODULES.md` and architecture docs.

## Warning Signs

- `:eyelib-particle` imports `io.github.tt432.eyelib.*` root runtime packages after extraction, especially `client.manager`, `client.registry`, `mc.impl`, `network`, or `capability`.
- Root code still imports `io.github.tt432.eyelib.client.particle.bedrock.*` after the module boundary is supposed to be complete, except through documented integration adapters.
- Both importer and particle modules define similarly named particle schema records/codecs with no adapter/parity tests.
- `SpawnParticlePacket`, `ParticleSpawnRequest`, or `ParticleCommandRuntime` switch from `String particleId` to `ResourceLocation`.
- `NetClientHandlers` calls `BrParticleRenderManager` or loader classes directly.
- Command suggestions use JSON resource paths rather than `particle_effect.description.identifier` manager keys.
- Forge `@Mod.EventBusSubscriber` particle hooks move without `Dist.CLIENT` and client classloading review.
- Any extraction phase deletes or relaxes the three existing regression tests instead of preserving their intent.
- `settings.gradle` includes the module, but no architecture/module docs identify ownership and dependency direction.

## Prevention Strategy

1. **Start with boundary contracts, not file moves.** Define what lives in `:eyelib-particle`, what remains root `mc/impl`, and how root consumes particle services.
2. **Move in vertical behavior slices.** Keep loader → registry/manager → lookup → command/network spawn → render manager behavior green after each slice.
3. **Prefer adapters over reverse dependencies.** If particle runtime needs player `RenderData`, level, or transport packet details, pass a context/request object from root instead of importing root capability/network packages.
4. **Freeze existing compatibility tests.** Treat `SpawnParticlePacketTest`, `ParticleSpawnRequestTest`, and `ParticleCommandRuntimeTest` as boundary sentinels; add manager publication and render cleanup tests rather than weakening them.
5. **Document every boundary change in the same phase.** `MODULES.md`, module-boundary docs, side-boundary docs, and particle README must change with code moves.
6. **Use JetBrains/Gradle verification only.** Compile and Gradle tasks must run through JetBrains MCP per repo rules; do not run Gradle from shell.

## Phase/Roadmap Inputs

| Phase | Must address | Risk controlled |
|---|---|---|
| Phase 1 — Boundary contract and Gradle skeleton | Add `:eyelib-particle`, choose Forge-runtime vs library-module posture, document dependency direction and client-side policy before moving behavior. | Cosmetic module, circular dependencies, wrong Gradle pattern. |
| Phase 2 — Schema/runtime ownership | Decide importer `BrParticle` vs runtime `BrParticle` contract, add adapter/parity tests, prevent duplicate codec drift. | Schema/runtime duplication drift. |
| Phase 3 — Runtime core move | Move particle definitions/components/emitters/render manager behind module services; preserve Molang scope, defensive copies, client-thread queued mutations. | Runtime behavior regressions, threading bugs, Molang regressions. |
| Phase 4 — Loader/manager publication seam | Relocate or adapt `BrParticleLoader`, `ParticleAssetRegistry`, `ParticleManager`, and `ParticleLookup` while preserving description-identifier keys and reload replacement. | Lost command suggestions, broken lookup/spawn ids. |
| Phase 5 — Command/network integration | Keep `mc/impl` validation/transport boundaries; preserve string-keyed packet/request contracts and handler → service delegation. | Platform leakage, broken spawn/remove flow. |
| Phase 6 — Client rendering and lifecycle verification | Verify render tick, client tick, render stage, logout cleanup, material render type use, and client-only classloading posture. | Missing Forge subscriber, server classloading crash, render regression. |
| Phase 7 — Regression/documentation gate | Run compile/tests/client smoke plan through JetBrains MCP, update all docs, and explicitly compare old vs new behavior for reload, manager publication, command, packet spawn/remove, and rendering. | Silent behavior regression and incomplete roadmap evidence. |

## Sources

- `.planning/PROJECT.md` — v1.2 goals, active requirements, constraints. HIGH.
- `MODULES.md` — current module inventory and update rules. HIGH.
- `docs/architecture/01-module-boundaries.md` — boundary map and current particle hard-quarantine progress. HIGH.
- `docs/architecture/02-side-boundaries.md` — side rules for packet/client particle services. HIGH.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` — current particle lookup/spawn communication rule. HIGH.
- Inspected current code/tests: `BrParticleLoader`, `ParticleSpawnService`, `EyelibParticleCommand`, `SpawnParticlePacket`, `NetClientHandlers`, runtime/importer `BrParticle`, `BrParticleRenderManager`, `BrParticleEmitter`, and listed regression tests. HIGH.
