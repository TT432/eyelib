# Phase 10: Schema/Runtime Ownership & Adapter - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 10 makes particle schema/runtime ownership explicit and creates a tested conversion seam from importer/raw Bedrock particle schema to executable particle runtime definitions. It must prevent duplicate `BrParticle` ownership from drifting silently, while avoiding a full runtime extraction, loader rewire, or command/network behavior change.

</domain>

<decisions>
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

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- Importer schema: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` owns raw Bedrock codec records, components as `BedrockResourceValue`, curves/events, render parameters, and billboard flipbook extraction helpers.
- Root runtime schema/runtime: `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` currently mixes runtime executable concepts, Minecraft `ResourceLocation`, component dispatch, Molang evaluation, and CODEC logic.
- Loader: `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` currently parses root runtime `BrParticle.CODEC` and publishes through `ParticleAssetRegistry`.
- Phase 9 APIs under `io.github.tt432.eyelibparticle.api` already provide string-keyed store/publication/spawn seams and root transitional adapters.

### Established Patterns
- Importer owns raw/source schema and CODECs; runtime execution belongs outside importer.
- Particle module must remain root-clean: no imports from root runtime packages, root managers, root registries, root packets, root capabilities, or root `mc/impl`.
- Tests should lock boundary behavior with compile/static checks and parity assertions instead of relying only on documentation.

### Integration Points
- `:eyelib-particle` may consume `:eyelib-importer` and `:eyelib-molang` to convert importer schema into runtime definitions without depending on root.
- Root legacy runtime classes can serve as comparison/adaptation targets but should be documented as non-canonical.
- Later phases depend on the adapter seam before moving runtime code (Phase 11) and loader/publication behavior (Phase 12).

</code_context>

<specifics>
## Specific Ideas

Prefer a named adapter and distinct runtime definition type, with large real fixture-backed parity tests that prove field preservation for real particle JSON shapes.

</specifics>

<deferred>
## Deferred Ideas

Full runtime client extraction remains Phase 11. Full loader/publication rewire remains Phase 12. Command/network integration remains Phase 13. Final broad regression and documentation gate remains Phase 14.

</deferred>
