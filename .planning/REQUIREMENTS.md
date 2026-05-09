# Requirements: Eyelib Module Separation

**Defined:** 2026-05-09
**Milestone:** v1.2 真正实现 eyelib-particle 的模块分离
**Core Value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；粒子拆分必须形成清晰 Gradle 模块边界，同时保持现有加载、命令、网络同步、渲染行为零回归。

## v1.2 Requirements

Requirements for this milestone. Each maps to exactly one roadmap phase.

### Gradle Module

- [ ] **PGRAD-01**: Maintainer can build and consume a real `:eyelib-particle` Gradle subproject with its own build metadata, source sets, resources, and root project dependency wiring.
- [ ] **PGRAD-02**: Maintainer can read module documentation that states `:eyelib-particle` ownership, dependency direction, and allowed integration layers.

### Boundary API

- [x] **PAPI-01**: Root runtime can access particle lookup, spawn/remove, store/publication, and initialization behavior through narrow particle-module APIs instead of owning particle internals directly.
- [ ] **PAPI-02**: `:eyelib-particle` has no dependency on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.
- [x] **PAPI-03**: Any temporary root compatibility facade delegates to particle-module APIs and is documented as transitional.

### Schema And Runtime Ownership

- [x] **PSCHEMA-01**: Maintainer can identify the canonical owner for importer/raw particle schema and the canonical owner for executable runtime particle definitions.
- [x] **PSCHEMA-02**: Runtime particle definitions are created from importer/raw schema through a named adapter or equivalent explicit conversion seam with parity coverage.
- [x] **PSCHEMA-03**: Duplicate `BrParticle` ownership cannot drift silently because codec/schema behavior and runtime conversion expectations are covered by tests or documented invariants.

### Loading And Publication

- [x] **PLOAD-01**: Resource reload still parses `particles/*.json` and replaces the active particle registry without changing observable reload behavior.
- [x] **PLOAD-02**: Particle publication continues to key entries by `particle_effect.description.identifier`, not by JSON resource path or other incidental source keys.
- [x] **PLOAD-03**: Loader, registry, and manager responsibilities are owned by the particle module or by explicit root adapters without reintroducing root-owned particle internals.

### Command And Network Integration

- [ ] **PNET-01**: User can run `/eyelib particle` with the same syntax, suggestions, validation, spawn position behavior, and success message as before extraction.
- [ ] **PNET-02**: Spawn/remove packet behavior remains string-keyed and continues to delegate from network handlers into particle services without exposing render internals.
- [ ] **PNET-03**: Platform-specific command, player, packet channel, and identifier validation concerns stay in explicit integration adapters and do not contaminate pure particle core APIs.

### Rendering And Verification

- [x] **PRENDER-01**: Existing client particle emitter, render manager, material/texture resolution, Molang scope, lifetime, remove semantics, tick/render lifecycle, and logout cleanup behavior are preserved.
- [x] **PRENDER-02**: Client-only hooks and platform integrations are side-safe after extraction and do not introduce dedicated-server classloading regressions.
- [ ] **PVERIFY-01**: Existing particle-related tests are moved or adapted without weakening assertions, and new boundary/parity/regression tests cover the module split.
- [ ] **PVERIFY-02**: Maintainer can verify the extracted module through planned JetBrains MCP Gradle checks, automated ClientSmoke flow where applicable, and a separate hardware checklist only for runtime behavior that cannot be automatically asserted.

## Future Requirements

Deferred to future milestones; not required for v1.2 completion.

### Publication And Ownership Polish

- **PFUT-01**: Maintainer can narrow root dependency scopes from broad `api`/`modImplementation`/`jarJar` wiring after API inventory proves which particle surfaces are public.
- **PFUT-02**: Maintainer can decide whether packet contracts should permanently remain root-owned or move into a dedicated cross-module transport contract after the v1.2 adapter boundary is stable.
- **PFUT-03**: Maintainer can publish `:eyelib-particle` as a separately documented external artifact if the broader Eyelib packaging strategy requires independent consumption.

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Cosmetic package rename without ownership transfer | Fails the user's goal of true module separation |
| New external particle engine/library | Not needed for boundary extraction and would add unnecessary risk |
| Replacing Molang engine or material pipeline | Existing modules already own these concerns; v1.2 should integrate, not rewrite them |
| Converting pure request/packet seams back to `ResourceLocation` | Would undo existing string-keyed boundary cleanup |
| Deleting or weakening tests to make extraction compile | Violates zero behavior regression requirement |
| Broad root compatibility layer with hidden long-term ownership | Would preserve root-owned particle internals under a new facade name |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PGRAD-01 | Phase 8: Boundary Contract & Gradle Module Skeleton | Complete |
| PGRAD-02 | Phase 8: Boundary Contract & Gradle Module Skeleton | Complete |
| PAPI-01 | Phase 9: Particle API & Store Seam | Complete |
| PAPI-02 | Phase 8: Boundary Contract & Gradle Module Skeleton | Complete |
| PAPI-03 | Phase 9: Particle API & Store Seam | Complete |
| PSCHEMA-01 | Phase 10: Schema/Runtime Ownership & Adapter | Complete |
| PSCHEMA-02 | Phase 10: Schema/Runtime Ownership & Adapter | Complete |
| PSCHEMA-03 | Phase 10: Schema/Runtime Ownership & Adapter | Complete |
| PLOAD-01 | Phase 12: Loading & Publication Rewire | Complete |
| PLOAD-02 | Phase 12: Loading & Publication Rewire | Complete |
| PLOAD-03 | Phase 12: Loading & Publication Rewire | Complete |
| PNET-01 | Phase 13: Command & Network Integration Rewire | Pending |
| PNET-02 | Phase 13: Command & Network Integration Rewire | Pending |
| PNET-03 | Phase 13: Command & Network Integration Rewire | Pending |
| PRENDER-01 | Phase 11: Runtime Client Core Extraction | Complete |
| PRENDER-02 | Phase 11: Runtime Client Core Extraction | Complete |
| PVERIFY-01 | Phase 14: Verification & Documentation Gate | Pending |
| PVERIFY-02 | Phase 14: Verification & Documentation Gate | Pending |

**Coverage:**
- v1.2 requirements: 18 total
- Mapped to phases: 18
- Unmapped: 0 ✓
- Duplicate mappings: 0 ✓
- Coverage status: 100% mapped to exactly one phase ✓

---
*Requirements defined: 2026-05-09*
*Last updated: 2026-05-09 after Phase 9 verification*
