# v1.2 Particle Module Split Milestone Closure

**Milestone:** v1.2 真正实现 eyelib-particle 的模块分离  
**Closure date:** 2026-05-09  
**Closure basis:** Phase 8-14 verification files, Phase 14 final JUnit gates, and Plan 03 JetBrains MCP matrix evidence.

## Requirements Status

| Requirement | Status | Closure Evidence |
|-------------|--------|------------------|
| PGRAD-01 | Complete | Phase 8 verification proves `:eyelib-particle` exists as a first-class Gradle subproject with build metadata, source sets, resources, and root dependency wiring. |
| PGRAD-02 | Complete | Phase 8 verification plus stable docs state module responsibility, dependency direction, allowed integration layers, and JetBrains MCP-only verification. |
| PAPI-01 | Complete | Phase 9 verification proves root lookup, store/publication, lifecycle, and spawn/remove entrypoints route through narrow module APIs. |
| PAPI-02 | Complete | Phase 8/9/14 boundary checks prove `:eyelib-particle` has no root runtime, root manager, root registry, root packet, capability helper, or root `mc/impl` dependency in pure module areas. |
| PAPI-03 | Complete | Phase 9/12/14 docs and tests identify root compatibility facades as transitional adapters that delegate to module APIs/services. |
| PSCHEMA-01 | Complete | Phase 10 verification names importer `io.github.tt432.eyelibimporter.particle.BrParticle` as raw schema owner and particle `ParticleDefinition` as runtime definition owner. |
| PSCHEMA-02 | Complete | Phase 10/14 adapter tests prove `ParticleDefinitionAdapter.fromSchema(BrParticle)` preserves parity-critical mapped fields. |
| PSCHEMA-03 | Complete | Phase 10/14 boundary and documentation tests prevent duplicate particle-module `BrParticle` drift and lock schema/runtime ownership wording. |
| PLOAD-01 | Complete | Phase 12 verification proves `BrParticleLoader` still scans `particles/*.json` and delegates replacement to module publication. |
| PLOAD-02 | Complete | Phase 12/14 tests prove active publication keys are `ParticleDefinition.identifier()`, not source `ResourceLocation` or resource path metadata. |
| PLOAD-03 | Complete | Phase 12/14 evidence proves module-owned `ParticleDefinitionRegistry`/`ParticleResourcePublication` own active loading/publication while root classes remain adapters. |
| PNET-01 | Complete | Phase 13 verification proves `/eyelib particle` syntax, suggestions, validation, position fallback, packet dispatch, and success message remain compatible. |
| PNET-02 | Complete | Phase 13/14 tests prove spawn/remove packets remain string-keyed and `NetClientHandlers` delegates to `ParticleSpawnService` without exposing render internals. |
| PNET-03 | Complete | Phase 13/14 docs/tests keep Brigadier/player/channel/buffer/identifier concerns in explicit root/MC adapters, outside pure particle APIs. |
| PRENDER-01 | Complete | Phase 11 verification proves emitter, component, render manager, material/texture, Molang scope, lifetime/remove, tick/render lifecycle, and cleanup behavior moved behind module runtime/client services with targeted tests. |
| PRENDER-02 | Complete | Phase 11/14 boundary tests prove client-only hooks are `Dist.CLIENT`-guarded and pure runtime packages remain free of platform bindings. |
| PVERIFY-01 | Complete | Phase 14 Plan 02 added final stable-doc/root/module boundary tests; Plan 03 `14-MCP-VERIFICATION-MATRIX.md` rows 1-3 reran the required targeted matrix with exitCode 0. |
| PVERIFY-02 | Complete | Phase 14 Plan 03 records exact JetBrains MCP taskNames/scriptParameters/task ids/exit codes, ClientSmoke applicability, hardware/manual checklist status, residual risks, and closure rationale. |

## Phase 8-13 Verified Truths Carried Forward

| Phase | Verified Truth |
|-------|----------------|
| 8 | `:eyelib-particle` is a real Gradle subproject consumed one-way by root; module docs and compile checks passed through JetBrains MCP. |
| 9 | Root particle lookup/store/publication/spawn facades delegate through module API/store/request ports, with insertion order and duplicate request-type risks fixed. |
| 10 | Importer raw schema, particle runtime definition, and adapter seam are explicit and covered by real fixture parity plus forbidden-reference tests. |
| 11 | Executable runtime, component dispatch, lifecycle, render manager, client hooks, and root spawn delegation live behind module/runtime/client boundaries. |
| 12 | Active loading/publication is module-owned through `ParticleDefinitionRegistry` and `ParticleResourcePublication`, keyed by description identifier. |
| 13 | Command/network integration remains user-compatible while platform concerns stay in root/MC adapters and packets remain string-keyed. |
| 14 | Final source tests read stable docs/source only, required JetBrains MCP rows pass, and manual/hardware evidence is recorded separately from automated gates. |

## Explicit Deferrals

| Deferral | Status | Why Non-Blocking For v1.2 | Follow-Up |
|----------|--------|----------------------------|-----------|
| PFUT-02 | Future requirement | v1.2 intentionally keeps packet DTO/codecs under `mc/impl/network/packet`; Phase 13/14 tests document and verify that adapter boundary. | Future packet-contract relocation decision after the adapter boundary stabilizes. |
| PFUT-03 | Future requirement | v1.2 proves the in-repository Gradle module boundary; independent external publication is packaging strategy work. | Future publication/packaging milestone if needed. |
| Windows hardware exit-code capture | Manual/deferred | D-15 and project scope exclude mandatory automated Windows hardware exit-code capture from v1.2 closure. | Manual hardware checklist evidence when a hardware/client session is available. |
| Unrelated root fixture cleanup | Residual | Optional broad `:test` still fails three geometry/importer fixture `NoSuchFileException` tests; required particle-gate rows run and pass. | Separate fixture cleanup task outside the particle split gate. |
| Manual visual proof | Manual/deferred | No existing particle-specific ClientSmoke hook was available; creating one would be new smoke framework/feature work. Automated JUnit/static gates cover the final module split, while visual proof remains manual/hardware evidence. | Record screenshots/logs in `14-HARDWARE-CHECKLIST.md` when a real client session is available. |

## Residual Risks

| Risk | Current Treatment | Blocking? |
|------|-------------------|-----------|
| Broad root `:test` is not fully green because of geometry/importer fixture `NoSuchFileException` failures. | Documented in `14-MCP-VERIFICATION-MATRIX.md` row 4c as unrelated fixture residuals; targeted particle rows 1-3 pass. | No |
| Direct in-game particle visual proof is unavailable in this automation-only execution. | Recorded as manual/deferred in `14-HARDWARE-CHECKLIST.md`; not represented as automated proof. | No |
| Packet contract and external artifact polish remain open future questions. | Tracked as PFUT-02 and PFUT-03; stable docs explicitly mark them future/non-blocking. | No |

## Closure Decision

**Close v1.2 as complete for the in-repository `:eyelib-particle` module split gate.**

Rationale:

1. All v1.2 requirements from `PGRAD-01` through `PVERIFY-02` have source-backed evidence or final Plan 03 matrix evidence.
2. Required JetBrains MCP rows 1-3 in `14-MCP-VERIFICATION-MATRIX.md` exited 0 and provide exact maintainer rerun commands.
3. The only remaining broad-suite failures are unrelated geometry/importer fixture residuals, not particle module split regressions.
4. ClientSmoke/manual/hardware status is explicitly separated from automated Gradle success, preserving D-13 through D-15 without inventing unsupported automated visual proof.
5. PFUT-02 and PFUT-03 remain future requirements rather than hidden blockers for the v1.2 boundary extraction.
