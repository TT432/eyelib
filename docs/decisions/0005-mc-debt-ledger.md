# ADR-0005: Functional Module Debt Ledger

**Status:** Accepted  
**Context:** After extracting functional modules (particle, material, importer, attachment, Molang, animation, behavior), remaining cross-module debt blocks full ownership extraction.  
**Decision:** Track remaining debt items (FM-004, FM-008, FM-014, FM-015) with clear status indicators. Root keeps only code that genuinely coordinates multiple feature modules or is required by the root mod entrypoint.  
**Consequences:** Future refactoring phases have a prioritized list of remaining cross-module dependencies to address.

---

# Functional Module Debt Ledger

## Purpose
- Record remaining cross-module debt that blocks further functional ownership extraction.
- Functional ownership rule: particle code → `:eyelib-particle`, material → `:eyelib-material`, importer → `:eyelib-importer`, attachment → `:eyelib-attachment`, Molang → `:eyelib-molang`, animation → `:eyelib-animation`, behavior → `:eyelib-behavior`.
- Root keeps only code that genuinely coordinates multiple feature modules or is required by the root mod entrypoint.

## Remaining Debt

| ID | Problem | Status |
|---|---|---|
| FM-004 | `ParticleSpawnService` — root compatibility facade for packet entrypoints and capability context. Particle-only spawn/runtime behavior already lives in `:eyelib-particle`. | Partial |
| FM-008 | Root-coupled attachment packets — remaining update/extra packet contracts decode through root `EyelibAttachableData` or root capability payload types, preventing full attachment protocol surface ownership by `:eyelib-attachment`. | Partial |
| FM-014 | Shared channel entrypoints and context-free handler dispatch — root network package owns shared registration/delegation, while feature-specific protocol contracts stay in subproject modules (`io.github.tt432.eyelibparticle.network`, `io.github.tt432.eyelibattachment.network`). | Stable |
| FM-015 | `LivingEntityRendererAccessor` — client-render-owned accessor mixin, physically hosted in the shared `mixin/` package root with technical mixin wiring, superseding package-name ownership. | Stable |
