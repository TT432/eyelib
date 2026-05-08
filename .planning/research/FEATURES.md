# Feature Landscape — `eyelib-particle` Module Separation

**Domain:** Eyelib particle Gradle module extraction and boundary cleanup  
**Researched:** 2026-05-09  
**Downstream consumer:** Requirements definition  
**Overall confidence:** HIGH for repository-derived requirements; MEDIUM for final packaging details until Gradle wiring is designed.

## Table Stakes

Capabilities required for the milestone to count as real `eyelib-particle` separation. Missing any of these would silently weaken the user's Gradle-module goal or risk behavior regression.

| Feature | Observable Capability | Complexity | Dependencies / Existing Code | Zero-Regression Expectation |
|---------|------------------------|------------|------------------------------|-----------------------------|
| Independent `:eyelib-particle` Gradle subproject | Maintainers can see, build, and reason about particle code as a named Gradle module rather than a root package cluster. | High | Current root particle runtime at `src/main/java/io/github/tt432/eyelib/client/particle/`; module inventory in `MODULES.md`; boundary rules in `docs/architecture/01-module-boundaries.md`. | Root runtime still launches with particles available through project dependency; no consumer-facing command/resource/packet breakage. |
| Documented module ownership and dependency direction | `MODULES.md`, boundary docs, and particle README identify what belongs in `:eyelib-particle` vs root `mc/impl`, network, importer, material, Molang, and processor modules. | Medium | Existing module docs already require updates when module boundaries change. | No ambiguous new catch-all location; future maintainers can move particle code without re-opening ownership debates. |
| Particle runtime API/seams moved behind narrow ports | Root callers consume particle lookup/spawn/manager capabilities through stable seams, not concrete root internals. | High | `ParticleLookup`, `ParticleSpawnService`, `ParticleSpawnRequest`, `ParticleManager`, `ParticleAssetRegistry`. | Existing runtime reads, packet-driven spawn/remove, and renderer access still resolve the same particle identifiers. |
| Loader-to-registry publication preserved | Resource reload still parses `particles/*.json`, replaces particle store contents, and publishes by Bedrock particle identifier. | Medium | `BrParticleLoader.apply()` currently parses with `LoaderParsingOps.parseBySourceKey(...)`, stores `Map<ResourceLocation, BrParticle>`, then calls `ParticleAssetRegistry.replaceParticles(...)`. | Reloaded particle definitions remain available under `particle_effect.description.identifier`; old resources continue loading. |
| Particle schema/importer boundary clarified | Importer-facing particle data ownership is explicit: parsed Bedrock schema/codec data is not duplicated in runtime packages. | High | Existing importer schema `eyelib-importer/.../particle/BrParticle.java`; current root runtime still imports `client.particle.bedrock.BrParticle` in loader/manager/registry. | Codec behavior for format version, description, curves, events, components, flipbook extraction, and Molang values remains equivalent. |
| Platform-type quarantine maintained | Minecraft/Forge identifiers, reload events, Brigadier command wiring, and transport runtime stay at platform integration boundaries instead of contaminating pure particle core. | High | Current rules: `ParticleSpawnRequest` is string-keyed; command lives in `mc/impl/common/command`; loader lifecycle in `mc/impl/client/loader`; packets use string-keyed particle id seam. | No reintroduction of `ResourceLocation`/Forge event types into platform-free request/schema/core APIs. |
| `/eyelib particle` command compatibility | Users can continue spawning/removing particles through the existing command flow. | Medium | `EyelibParticleCommand`; `ParticleCommandRuntime`; `SpawnParticlePacket` / `RemoveParticlePacket`; command-side identifier validation/adaptation. | Command syntax, suggestions, messages, and spawn/remove effects do not regress. |
| Spawn/remove packet compatibility | Network packet contracts continue to deliver particle spawn/remove requests to the client runtime service. | Medium | `network/SpawnParticlePacket`, `network/RemoveParticlePacket`, `NetClientHandlers`, `ParticleSpawnService`. | Existing packet payload semantics remain string-keyed and client application behavior is unchanged. |
| Client emitter/render behavior preserved | Existing Bedrock particle emitters, render manager, material/render integration, and animation-triggered particle effects continue to render the same visual output. | High | `client/particle` emitter/render classes, `eyelib-material` dependency, render level scope, animation effect callers. | No visual degradation: same particles, lifetime behavior, texture/material resolution, and emitter remove semantics. |
| Tests/static verification updated to module boundary | Existing particle-related tests compile against the new module layout, and new boundary tests prevent regressions in schema/runtime/platform separation. | Medium | Current test suites plus v1.1 static verification patterns; Gradle verification must run via JetBrains MCP only. | Build/test green with zero behavior-oriented test deletions; tests should be moved/adapted, not weakened. |

## Differentiators

Valuable maintainer-observable improvements that make the separation robust, not merely a file move.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Particle module facade package | Gives root and future consumers a small, intentional surface for lookup, spawn, registry publication, and module initialization. | Medium | Prefer facade/ports over exposing all `bedrock` runtime classes as de facto API. |
| Boundary smoke/static tests for forbidden dependencies | Prevents future regressions such as `eyelib-particle` depending back on root runtime or pure schema APIs importing MC/Forge types. | Medium | Should assert dependency direction and package import rules; do not replace runtime behavior tests. |
| Schema adapter layer between importer and particle runtime | Makes it explicit when importer `BrParticle` data is reused directly versus adapted into runtime definitions. | High | Especially useful because importer already has `io.github.tt432.eyelibimporter.particle.BrParticle` while root has particle runtime Bedrock classes. |
| Stable particle fixture corpus | Captures representative resource reload, flipbook, curve, event, and component cases before moving code. | Medium | Enables zero-regression confidence for codec/schema behavior and rendering setup. |
| Lifecycle registration composition in root bootstrap | Keeps `EyelibMod` as Forge startup composition root while delegating particle module registration through a narrow hook. | Medium | Avoids making `:eyelib-particle` a second uncontrolled bootstrap owner. |
| Migration notes for maintainers | Documents old path → new module mapping so future refactors do not reintroduce mixed ownership. | Low | Update `MODULES.md`, `docs/architecture/01-module-boundaries.md`, and particle README in same change. |

## Anti-features

Features to explicitly avoid because they undermine true module separation or zero-regression constraints.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Cosmetic package rename without Gradle subproject | Fails the user's clarified goal of an independent `eyelib-particle` module. | Add a real Gradle subproject and make root consume it through project dependency. |
| Keeping root as the owner of particle internals | Leaves manager/loader/packet/runtime coupling essentially unchanged. | Move particle-owned runtime/schema-adapter responsibilities behind module seams; root keeps only integration wiring where appropriate. |
| Duplicating particle schema in both importer and particle module | Creates drift in codecs, flipbook extraction, Molang defaults, and component interpretation. | Choose one schema owner and introduce adapters if runtime needs a different representation. |
| Moving Forge/MC command, packet transport, or reload lifecycle wholesale into pure particle core | Contaminates the module with platform integration and violates existing hard-quarantine direction. | Keep platform binding in `mc/impl`/network lifecycle; expose string-keyed module services. |
| Changing particle identifiers during migration | Breaks resource lookup, command spawn, packet payloads, and registry publication. | Preserve `particle_effect.description.identifier` as the manager/publication key. |
| Weakening or deleting tests to make extraction compile | Hides behavior regression. | Move tests with their owned code and add boundary tests for new dependency rules. |
| Introducing top-level `Eyelib` particle reach-through | Contradicts current boundary rule against new singleton reach-through methods. | Use domain-local read/write ports and module facades. |
| Expanding scope to new particle authoring features | Distracts from separation and increases regression risk. | Defer authoring/editor features until after module boundary stabilizes. |

## Dependency Notes

```text
Root Forge bootstrap / mc/impl
    ├── owns command registration, reload listener registration, network transport, lifecycle wiring
    └── depends on :eyelib-particle services/facades where particle behavior is needed

:eyelib-particle
    ├── owns particle runtime definitions, emitter/render manager, lookup/spawn service contracts, and registry publication seams
    ├── may depend on :eyelib-importer for importer-owned particle schema or consume adapter DTOs
    ├── may depend on :eyelib-molang for Molang particle expressions
    ├── may depend on :eyelib-material/render-facing contracts only where current rendering behavior requires it
    └── must not depend back on root runtime packages or `mc/impl` platform wiring

:eyelib-importer
    └── owns Bedrock particle schema/codec data if selected as canonical schema owner

network/** packets
    └── keep string-keyed packet contracts and delegate client application into particle spawn/remove services
```

Key dependency decisions requirements should force explicitly:

- Decide whether `eyelib-importer` remains the canonical owner of `BrParticle` schema or whether a particle-specific schema package is introduced with importer dependency direction documented. Do not allow two silent owners.
- Preserve existing manager pattern (`Manager<BrParticle>` read/write ports) or replace it with an equivalent particle-module store facade; root callers must not reach into concrete storage.
- Keep `ResourceLocation` at loader/resource and command validation boundaries; particle request and packet seams should remain string-keyed.
- Treat animation particle effects as cross-domain consumers: they should call particle module services, not instantiate particle internals directly.
- Update documentation in the same milestone: `MODULES.md`, `docs/architecture/01-module-boundaries.md`, and particle README are part of the deliverable, not optional cleanup.

## Requirement Inputs

Use these as candidate requirements for the v1.2 roadmap/spec.

1. **GRAD-PARTICLE-01:** Add `:eyelib-particle` as a first-class Gradle subproject and wire root runtime to consume it via project dependency.
2. **BOUNDARY-PARTICLE-01:** Define and document ownership for particle runtime, importer schema, loader publication, command/network integration, and render/material integration.
3. **SCHEMA-PARTICLE-01:** Establish a single canonical particle schema owner and remove/avoid duplicate `BrParticle` ownership across root/importer/particle module.
4. **RUNTIME-PARTICLE-01:** Move or expose particle lookup, manager publication, and spawn/remove orchestration through `:eyelib-particle` seams.
5. **LOADER-PARTICLE-01:** Preserve resource reload behavior from `particles/*.json` through parsing, registry replacement, and manager publication.
6. **COMMAND-PARTICLE-01:** Preserve `/eyelib particle` spawn/remove command behavior, suggestions, validation, and user-visible messages.
7. **NETWORK-PARTICLE-01:** Preserve `SpawnParticlePacket` and `RemoveParticlePacket` client application semantics with string-keyed particle ids.
8. **RENDER-PARTICLE-01:** Preserve client emitter/render output, material/texture resolution, and animation-triggered particle effects.
9. **QUARANTINE-PARTICLE-01:** Prevent pure particle schema/core APIs from importing Forge/Minecraft platform types unless the package is explicitly an integration adapter.
10. **VERIFY-PARTICLE-01:** Move/adapt tests and add boundary verification so Gradle module separation is proven without deleting behavior coverage.

## Sources

- `.planning/PROJECT.md` — v1.2 goal, active requirements, constraints, and zero-regression policy.
- `MODULES.md` — current module inventory, particle runtime row, network command rows, module update rules.
- `docs/architecture/01-module-boundaries.md` — hard-quarantine direction, particle request seam, importer boundary notes, no top-level reach-through rule.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` — current particle lookup/spawn/request communication rules.
- `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` — current reload parse and registry publication behavior.
- `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` — current identifier flattening and manager write-port publication.
- `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` — current manager read/write port pattern.
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` — existing importer particle schema/codec and flipbook/curve/component parsing behavior.
