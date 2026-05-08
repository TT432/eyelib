# Research Summary — Milestone v1.2 `:eyelib-particle` Module Separation

## Executive Summary

v1.2 should create a real `:eyelib-particle` Gradle subproject, not a cosmetic package rename. The particle feature is currently spread across root runtime packages, loader/manager publication, command/network integration, Forge client events, Molang scope handling, material rendering, and importer schema concerns. The recommended implementation is a Forge-capable feature module using the existing Java 17 + Forge 1.20.1 + ModDevGradle LegacyForge pattern, consumed by the root project through normal project dependencies.

The hard architectural rule is one-way dependency direction: root runtime may depend on `:eyelib-particle`, but `:eyelib-particle` must not depend back on root packages. Platform bindings do **not** always have to remain in root: particle-owned Forge/client hooks may live in an explicit particle integration layer such as `eyelib-particle/.../mc/impl`. However, pure particle core/API/schema seams must not be contaminated by Minecraft/Forge types, and shared command/network transport should remain in the appropriate root platform layer unless a phase deliberately proves a better integration boundary.

The biggest risks are circular dependencies, schema/runtime drift between importer and particle code, side/classloading regressions, and subtle behavior loss in reload keys, packet spawn semantics, Molang scope, defensive copies, or render-thread queuing. The roadmap should therefore move in vertical slices with compatibility tests and documentation updates in the same phases.

## Stack Additions

- Add `include("eyelib-particle")` and `eyelib-particle/build.gradle` as a first-class Gradle subproject.
- Use the existing Forge-aware module stack: `java-library`, Java 17 toolchain, Lombok, `net.neoforged.moddev.legacyforge` 2.0.91, `maven-publish`, sources jar, resource expansion, and `META-INF/mods.toml`.
- Use package root `io.github.tt432.eyelibparticle.*` to avoid split packages with root `io.github.tt432.eyelib.*`.
- Expected module dependencies: `api project(':eyelib-molang')` where public particle APIs expose Molang types; `implementation project(':eyelib-material')` for rendering; optional `implementation project(':eyelib-importer')` only for an explicit schema-to-runtime adapter; no dependency on root `:`.
- Root should initially consume the module in the established runtime-module style: `api`, `modImplementation`, and `jarJar project(':eyelib-particle')`, then narrow scopes only if API inventory proves safe.

## Feature Table Stakes

- A visible, buildable `:eyelib-particle` module with documented ownership and dependency direction.
- Particle runtime definitions, emitters, render manager, lookup/spawn seams, and particle-local registry/publication APIs moved or exposed from the module.
- Resource reload behavior preserved: `particles/*.json` parse, replacement, and publication by `particle_effect.description.identifier`.
- `/eyelib particle` command behavior preserved: syntax, suggestions, validation, messages, spawn/remove effects.
- Spawn/remove packet compatibility preserved with string-keyed ids and handler-to-service delegation.
- Client emitter/render output preserved, including material/texture resolution, Molang scope, lifetime behavior, remove semantics, tick/render events, and logout cleanup.
- Importer/runtime schema ownership decided explicitly; no silent duplicate `BrParticle` source of truth.
- Tests moved/adapted and boundary verification added; existing behavior tests must not be weakened to make extraction compile.

## Architecture Recommendation

- Treat `:eyelib-particle` as a feature runtime module, not pure JVM-only. It may own particle runtime/client/render code and particle-specific Forge hooks when those hooks are isolated in integration packages.
- Keep pure particle APIs and request seams platform-light: `ParticleSpawnRequest`, packet-facing DTOs, lookup names, and publication keys should remain string-keyed and root-independent.
- Keep unified command registration and network channel ownership in root `mc/impl` / `network`; root adapters should convert platform packets, `ResourceLocation` validation, player/render data, and Molang scope into particle-module requests/context objects.
- Move particle-specific manager/store ownership out of root. If temporary root facades are needed, they should delegate to module APIs and be documented as compatibility shims.
- Preserve importer as raw/source schema owner. Particle owns runtime executable definitions and may add a named, tested adapter from importer schema to runtime definitions.
- Update `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `docs/index/repo-map.md`, and a new particle module README alongside implementation.

## Watch Out For

1. **Cosmetic module only:** adding Gradle scaffolding without moving ownership fails the milestone.
2. **Root ↔ particle cycles:** any `:eyelib-particle` import of root manager, registry, packets, capability helpers, `mc/impl`, or root utilities is a red flag.
3. **Platform contamination:** MC/Forge bindings may live in integration layers, but pure particle core must not import `ResourceLocation`, Forge events, or root platform wiring.
4. **Schema drift:** importer `BrParticle` and runtime particle definitions need a declared contract plus adapter/parity tests if both remain.
5. **Lookup key regression:** preserve publication under `particle_effect.description.identifier`, not JSON resource paths.
6. **String-id regression:** do not convert request/packet/common seams back to `ResourceLocation`; command validation can adapt at the platform boundary.
7. **Client/server side issues:** Forge event subscribers and client classes need `Dist.CLIENT` posture and classloading review.
8. **Subtle runtime regressions:** preserve Molang parent scope, defensive `Vector3f` copies, and `Minecraft.getInstance().submit(...)` queued render-manager mutations.
9. **Verification shortcuts:** Gradle verification must use JetBrains MCP only; do not run Gradle from shell.

## Requirements Inputs

- `GRAD-PARTICLE-01`: add `:eyelib-particle` as a real Gradle subproject with Forge-capable build metadata and root runtime consumption.
- `BOUNDARY-PARTICLE-01`: document particle/runtime/importer/platform ownership and enforce one-way root → particle dependency direction.
- `API-PARTICLE-01`: expose lookup, spawn/remove, manager publication, and module initialization through narrow particle-module ports/facades.
- `SCHEMA-PARTICLE-01`: choose canonical importer/raw schema ownership and add named adapters/tests for runtime conversion.
- `LOADER-PARTICLE-01`: preserve resource reload parsing and registry replacement by description identifier.
- `COMMAND-PARTICLE-01`: preserve `/eyelib particle` user behavior while keeping Brigadier/validation in platform integration.
- `NETWORK-PARTICLE-01`: preserve string-keyed spawn/remove packet semantics and handler delegation into particle services.
- `RENDER-PARTICLE-01`: preserve visual output, material integration, Molang scope, tick/render lifecycle, and logout cleanup.
- `QUARANTINE-PARTICLE-01`: forbid pure particle core imports of root/MC/Forge platform types except in explicitly named integration adapters.
- `VERIFY-PARTICLE-01`: move/adapt tests, add boundary tests, and run planned Gradle/client verification through JetBrains MCP.

## Roadmap Inputs

1. **Boundary contract and Gradle skeleton** — Add the module, build metadata, root dependencies, README/docs, and explicit dependency/side policy before moving behavior. Controls cosmetic-module and wrong-build-pattern risk.
2. **Particle API/store seam** — Introduce module-owned lookup, spawn request/context, read/write store, and publication APIs; add temporary root delegating facades only if needed. Controls circular dependency risk before bulk moves.
3. **Schema/runtime ownership decision** — Lock importer raw schema vs particle runtime contract, add adapter/parity tests, and prevent duplicate codec drift. Needs focused planning/research because current importer/runtime models differ.
4. **Runtime/client move** — Move Bedrock runtime definitions, components, emitters, render manager, and particle-specific client hooks into `:eyelib-particle`, keeping pure core separate from `mc/impl`. Controls behavior and side-boundary risk.
5. **Loader/manager publication rewire** — Move or adapt `BrParticleLoader`, `ParticleAssetRegistry`, and manager publication while preserving description-identifier keys and reload replacement semantics.
6. **Command/network integration rewire** — Keep root command/channel ownership, convert packets/platform data into module requests, and preserve handler → service delegation without exposing render internals.
7. **Regression and documentation gate** — Verify compile/tests/client smoke via JetBrains MCP, compare reload/command/packet/render behavior, remove or document compatibility facades, and ensure all architecture docs match the new boundary.

**Research flags:** Phase 3 and Phase 4 need deeper phase-level design because schema ownership and loader/store migration have the highest ambiguity. Phase 6 can mostly use standard adapter patterns if Phase 2 APIs are clean. Phase 1 is well documented and should not need additional research.

## Confidence

| Area | Confidence | Notes |
|---|---|---|
| Stack | HIGH | Current multi-project Gradle and Forge-aware module patterns are well evidenced; exact dependency scopes should be finalized after compile/API inventory. |
| Features | HIGH | Table stakes come directly from repository behavior and user constraints. |
| Architecture | HIGH | Integration points are clear; exact phase sizing is MEDIUM until move/compile failures expose hidden coupling. |
| Pitfalls | HIGH | Risks are based on inspected current particle loader/runtime/command/network/test seams. |

**Open gaps for planning:** final `api` vs `implementation` scopes; exact placement of particle reload listener; whether packet contracts remain root-owned permanently; adapter shape between importer schema and particle runtime; dedicated-server classloading proof after moving client hooks.

## Sources

- `.planning/research/STACK.md`
- `.planning/research/FEATURES.md`
- `.planning/research/ARCHITECTURE.md`
- `.planning/research/PITFALLS.md`
