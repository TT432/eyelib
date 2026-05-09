# Phase 9: Particle API & Store Seam - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 9 creates the narrow particle-module API/store seams that root runtime can consume for lookup, spawn/remove, store/publication, and initialization behavior. It must convert any remaining root compatibility facade into explicit delegation to `:eyelib-particle` APIs, without moving Bedrock runtime/render internals that belong to later phases.

</domain>

<decisions>
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

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java` currently exposes read access but imports root `ParticleManager`, root `BrParticle`, and `ResourceLocation`.
- `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` is currently the canonical root manager with `readPort()` and `writePort()`.
- `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` currently flattens particles by `particle.particleEffect().description().identifier()` before writing to `ParticleManager`.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` currently owns packet-driven spawn/remove delegation and still imports root capability, MC impl, packet, Minecraft, and Bedrock runtime types.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` already defines dependency direction and integration constraints from Phase 8.

### Established Patterns
- Existing lookup seams are narrow static facades, but Phase 9 should make particle-module APIs canonical and root facades transitional.
- Existing manager stores use `ManagerReadPort`/`ManagerWritePort` in root; Phase 9 should avoid making `:eyelib-particle` depend on those root interfaces.
- Existing particle publication semantics use particle description identifiers as stable keys.

### Integration Points
- `:eyelib-particle` can expose platform-light API interfaces/classes consumed by root via the existing root → particle Gradle dependency.
- Root particle facade, manager, registry, and spawn service are the likely adapter points.
- Later phases depend on Phase 9 seams for schema/runtime ownership, runtime extraction, loading rewire, and command/network integration.

</code_context>

<specifics>
## Specific Ideas

Prefer the smallest correct API seam that future phases can extend without adding a broad compatibility layer. Keep behavior-compatible delegation and document all transitional root surfaces locally.

</specifics>

<deferred>
## Deferred Ideas

Schema/runtime conversion ownership is deferred to Phase 10. Bedrock runtime/render extraction is deferred to Phase 11. Loader publication rewiring is deferred to Phase 12 except for any minimal delegation seam required by Phase 9. Command/network integration behavior changes are deferred to Phase 13.

</deferred>
