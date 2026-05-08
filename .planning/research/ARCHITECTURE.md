# Architecture Research — `eyelib-particle` Module Separation

**Project:** Eyelib v1.2 particle module split  
**Researched:** 2026-05-09  
**Confidence:** HIGH for current-code integration points; MEDIUM for exact phase sizing until implementation inventory/test failures are known.

## Current Architecture

- Particle runtime is currently root-owned under `src/main/java/io/github/tt432/eyelib/client/particle/`.
  - `ParticleLookup` reads through root `client/manager/ParticleManager`.
  - `ParticleSpawnService` applies packet-driven spawns and directly reaches root capability/data-attach state plus `SpawnParticlePacket`.
  - `BrParticleRenderManager` owns emitter/particle stores and Forge client events for render tick, client tick, level render, and logout cleanup.
  - Bedrock runtime component classes mix data codecs, Molang evaluation, Minecraft rendering/world types, and Forge annotation scanning.
- Importer already has a separate particle schema at `eyelib-importer/.../eyelibimporter/particle/BrParticle`; it is raw/importer-facing and keeps component bodies as `BedrockResourceValue`, not runtime components.
- Root integration points already route through seams:
  - `/eyelib particle` command: `mc/impl/common/command/EyelibParticleCommand` validates `ResourceLocation`, suggests via `ParticleLookup.names()`, sends `SpawnParticlePacket`.
  - Network: `EyelibNetworkTransport` registers spawn/remove packets; `NetClientHandlers` delegates to `ParticleSpawnService`.
  - Reload: `ClientLoaderLifecycleHooks` registers `BrParticleLoader`; loader publishes through `ParticleAssetRegistry`.
  - Tooling: `ManagerResourceImportPlanner` parses/publishes particles and currently skips addon particle bridge because importer particle components are plain-data only.
  - Animation/controller runtime calls `ParticleLookup` and `ParticleSpawnService` for effect spawning/removal.

## Target Boundary

Recommended shape: create `:eyelib-particle` as a feature runtime module with a clean internal split, not as a pure-JVM-only module.

| Boundary | Target owner | Rationale |
|---|---|---|
| Particle runtime API/store | `:eyelib-particle` under `io.github.tt432.eyelibparticle.*` | This is the actual reusable particle capability. Root should consume it, not own it. Use a new namespace to avoid split packages. |
| Pure-ish particle model/request seams | `:eyelib-particle` core/runtime packages | `ParticleSpawnRequest`, read/write ports, definition ids, and non-Forge request shaping should be root-independent. |
| Bedrock runtime components/emitter/particle renderer | `:eyelib-particle` runtime/client packages | They are particle-owned even when they depend on Minecraft client/render classes. Keeping them in the module makes root depend on the feature, not the inverse. |
| Particle-owned platform hooks | `:eyelib-particle/.../mc/impl` | Render tick, client tick, logout cleanup, component annotation scanning, and particle reload-listener registration are particle-specific platform bindings. Keeping them next to the module is acceptable as long as pure core packages do not import them. |
| Shared command/network transport | root `mc/impl` / `network` | The root already owns the unified `/eyelib` command and Forge channel. Keep transport registration centralized, but make it call particle module APIs only. |
| Importer particle schema | `:eyelib-importer` | Do not duplicate ownership. Importer schema remains raw/source-facing; particle module may consume it through an adapter when addon particle support is enabled. |
| Root manager/registry publication | move particle-specific store to `:eyelib-particle`; root tooling calls module write port | Avoid `:eyelib-particle -> root` dependency through `ParticleManager extends Manager`. If necessary, keep temporary root facade adapters that forward into the module while callers migrate. |

Dependency direction should be:

```text
:eyelib-importer ─┐
:eyelib-molang   ├──> :eyelib-particle ───> root runtime (:)
:eyelib-material ┘                         (root consumes particle APIs)

Forbidden: :eyelib-particle -> root (:)
Forbidden: :eyelib-importer -> :eyelib-particle
Forbidden: pure particle core -> :eyelib-particle/mc/impl
```

Practical dependency note: `:eyelib-particle` should depend on `:eyelib-molang` for expressions/scopes and on `:eyelib-material` for render type resolution. It should only depend on `:eyelib-importer` if/when a source-schema-to-runtime adapter is introduced. Root should add `api/modImplementation/jarJar project(':eyelib-particle')` in the same style as `:eyelib-material`.

## Integration Points

### New components

| New component | Location | Purpose |
|---|---|---|
| Particle Gradle subproject | `eyelib-particle/build.gradle`, `settings.gradle` | Forge-capable feature module; no dependency back to root. |
| Particle module namespace | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/` | Runtime definitions, emitters, particles, components, lookup/spawn APIs, store/write ports. |
| Particle platform hooks | `eyelib-particle/.../mc/impl/` | Particle-owned Forge client events, reload listener registration, component scanning if retained. |
| Packet apply adapter | root `mc/impl/network` or `network` | Converts root `SpawnParticlePacket`/`RemoveParticlePacket` into module requests; owns root capability lookup and Minecraft player context. |
| Importer-to-runtime adapter | preferably `:eyelib-particle` adapter package | Converts `eyelibimporter.particle.BrParticle` into runtime `BrParticle` only when raw addon/importer flow is ready. |

### Modified components

| Existing component | Change needed | Why |
|---|---|---|
| `settings.gradle` | include `eyelib-particle` | Establish module identity before moves. |
| root `build.gradle` | depend on and jarJar `:eyelib-particle` | Root consumes particle runtime. |
| `client/particle/**` | move/rename into `io.github.tt432.eyelibparticle.*` | Remove root ownership of particle internals. |
| `ParticleSpawnService` | stop accepting `SpawnParticlePacket`; accept `ParticleSpawnRequest` plus context/scope inputs | Prevent module from importing root packet classes and root capability helpers. |
| `NetClientHandlers` | convert packet payloads to particle module requests | Network remains central; particle module remains packet-agnostic. |
| `EyelibParticleCommand` | import particle API only for suggestions; keep Brigadier/transport in root `mc/impl` | Command is platform integration and unified under `/eyelib`. |
| `BrParticleLoader` / reload lifecycle | move loader or replace with module-owned reload listener | Avoid root loader lifecycle importing concrete particle internals long-term. |
| `ParticleManager` / `ParticleAssetRegistry` | replace root manager with module-owned store/write port or temporary forwarding facades | Prevent circular dependency through root generic manager. |
| `ManagerResourceImportPlanner` | publish through particle module write API; later use importer adapter for addon particles | Keeps tooling root-owned but particle storage module-owned. |
| Animation/controller runtime | update imports to particle module lookup/spawn API | Root animation may consume particle feature; no reverse dependency. |
| `BrParticleRenderManager` | split static store/render loop from Forge subscriber shell if needed | Pure store operations should be testable without event annotations; event class belongs in platform hook package. |

## Proposed Build Order

1. **Boundary contract and module scaffold**
   - Add `include("eyelib-particle")`, module `build.gradle`, `mods.toml`, package README.
   - Add root dependency wiring but move no behavior yet.
   - Document dependency rule: root may depend on particle; particle must not depend on root.

2. **Create particle-owned API/store seam**
   - Introduce module-local read/write ports, `ParticleLookup`, `ParticleSpawnRequest`, and publication API.
   - Keep root facades temporarily if needed, but make them delegate into module APIs.
   - This breaks the `ParticleManager extends root Manager` dependency before bulk runtime movement.

3. **Move runtime definitions/components/emitter/render manager**
   - Move `client/particle/bedrock/**` into `:eyelib-particle` namespace.
   - Keep behavior intact first; do not simultaneously redesign component semantics.
   - Split pure request/store classes from `mc/impl` event subscribers where direct Forge annotations exist.

4. **Rewire packet and command integration**
   - Keep `SpawnParticlePacket`/`RemoveParticlePacket` and channel registration in root transport.
   - Root packet handler builds `ParticleSpawnRequest`, obtains root-owned `RenderData`/Molang scope/Minecraft context, then calls particle API.
   - Command continues to own Brigadier/ResourceLocation validation and only asks particle lookup for names.

5. **Rewire reload/tooling publication**
   - Move or replace `BrParticleLoader` so particle reload registration is module-owned, or keep a short-lived root adapter with a documented removal task.
   - Change manager tooling to publish through the particle module write port.
   - Preserve legacy folder particle loading before enabling addon particle bridge.

6. **Clarify importer schema bridge**
   - Keep `eyelib-importer` particle schema as source/raw schema.
   - Add an explicit adapter from importer schema to runtime particle definitions only after runtime move is stable.
   - Then remove the current addon skip in `ManagerResourceImportPlanner` with targeted tests/verification.

7. **Compatibility cleanup and verification**
   - Remove root `client/particle` implementation classes or leave only documented compatibility facades.
   - Run stage-specific Gradle verification through JetBrains MCP only.
   - Smoke-check resource reload, `/eyelib particle`, spawn/remove packets, animation-triggered particles, logout cleanup, and render counts.

## Documentation Impact

Update these files in the same implementation phases that change ownership:

- `MODULES.md`: add `eyelib-particle` row; update Client particle runtime, Runtime managers, Asset loaders, Command module, Network particle packet layer, MC impl utility bridge interactions.
- `docs/architecture/01-module-boundaries.md`: add `:eyelib-particle/**` to current major areas and target ownership map; record the dependency direction and platform-hook exception.
- `docs/architecture/02-side-boundaries.md`: add particle module side rules: pure particle core vs particle `mc/impl`; packet handlers convert payloads at root integration boundary.
- `docs/index/repo-map.md`: add particle module as the start point for particle runtime; root `client/particle` should no longer be the primary entry once moved.
- New `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`: document module-local packages, allowed dependencies, and public vs internal APIs.
- Root `src/main/java/io/github/tt432/eyelib/client/particle/README.md`: delete or convert to a compatibility/handoff note if facades remain.
- `settings.gradle` and root/subproject `build.gradle`: document Gradle module inclusion/dependency intent if not obvious from existing patterns.

## Requirement Inputs

- Active milestone requirement: `:eyelib-particle` must exist as a documented Gradle module with explicit responsibility and dependency direction.
- Active milestone requirement: schema/importer-facing data, particle runtime, and platform integration boundaries must be explicit and not silently duplicate ownership.
- Active milestone requirement: root consumes particle through narrow seams instead of owning particle internals.
- Active milestone requirement: resource reload, manager publication, `/eyelib particle`, spawn/remove packets, and client rendering must remain behavior-compatible.
- User clarification: platform bindings may live in the most appropriate integration layer. Recommendation: keep shared command/network transport in root `mc/impl`, but move particle-owned lifecycle hooks into `:eyelib-particle/.../mc/impl`; never allow these hooks to contaminate pure particle core packages.
