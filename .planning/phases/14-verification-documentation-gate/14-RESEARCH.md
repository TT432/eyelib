---
phase: 14
slug: verification-documentation-gate
status: complete
created: 2026-05-09
requirements: [PVERIFY-01, PVERIFY-02]
---

# Phase 14 Research — Verification & Documentation Gate

## Research Question

What must Phase 14 know to plan a final verification/documentation gate that proves the `:eyelib-particle` split preserved behavior and leaves repository architecture documentation consistent?

## Sources Read

- `.planning/phases/14-verification-documentation-gate/14-CONTEXT.md`
- `.planning/PROJECT.md`
- `.planning/REQUIREMENTS.md`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- Phase 8-13 `*-VERIFICATION.md` evidence files
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`
- `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md`
- `src/main/java/io/github/tt432/eyelib/network/README.md`
- `.planning/codebase/TESTING.md`, `.planning/codebase/CONVENTIONS.md`, `.planning/codebase/STRUCTURE.md`

## Current Evidence Baseline

Phase 8-13 verification already proves these source-backed truths:

| Area | Evidence | Phase 14 implication |
|------|----------|----------------------|
| Gradle module skeleton and root -> particle dependency | Phase 8 verified `settings.gradle`, root dependency wiring, `:eyelib-particle:compileJava`, and root `:compileJava`. | Preserve direction checks and include final compile matrix. |
| API/store seam | Phase 9 verified module API/store/publisher/spawn seams, root facade delegation, insertion order, and forbidden-import scans. | Keep no-weakening assertions; adapt stale boundary expectations only where the final split intentionally allows `client/**` integration. |
| Schema/runtime ownership | Phase 10 verified importer `BrParticle`, particle `ParticleDefinition`, `ParticleDefinitionAdapter`, parity fields, loud validation failure, and no duplicate module `BrParticle`. | Final parity tests should reuse real codecs/fixtures and mapped-field assertions. |
| Runtime/client extraction | Phase 11 verified executable runtime, component dispatch, lifecycle, render manager, client hook side gating, and root spawn delegation. | Phase 14 should add final/broad gate coverage and ClientSmoke/manual evidence, not re-implement runtime behavior. |
| Loading/publication | Phase 12 verified `ParticleResourcePublication`, `ParticleDefinitionRegistry`, description-identifier active keys, reload adapter delegation, add-on publication, and docs. | Final tests must include reload key/publication semantics in the split-wide gate. |
| Command/network | Phase 13 verified command compatibility, string-keyed packet codecs, handler delegation, adapter ownership, and docs; review fixes removed `.planning` dependencies from normal tests. | Final docs tests must also avoid `.planning` dependencies; packet-contract relocation remains PFUT-02. |

## Technical Findings

1. **Phase 14 is evidence-first, not feature work.** The phase should create or adapt tests, docs, verification matrix artifacts, and final closure evidence. Runtime source movement is outside scope unless a test/doc gap directly proves a smallest corrective change is required.
2. **JUnit 5 source-scan tests are established and appropriate.** Existing tests use flat package-private `*Test.java` classes, `Files.walk`, hand-written `SourceCheck` helpers, real codecs/fixtures, and descriptive package-private test methods.
3. **Documentation drift tests must read stable repository docs only.** Context D-07 and Phase 13 review fixes require no normal source tests to read `.planning/` artifacts.
4. **JetBrains MCP is the only Gradle execution path.** Verification commands must be expressed as `jetbrain_run_gradle_tasks` invocations; plans must not ask executors to run `./gradlew` or shell Gradle.
5. **Broad root `:test` may still expose unrelated fixture failures.** Context D-03/D-11 allow triage: particle-gate regressions block completion; unrelated fixture failures are residual evidence unless they prevent particle-gate test execution.
6. **ClientSmoke applicability is limited by existing hooks.** No broad new smoke framework should be introduced. If there is no existing particle-specific smoke hook, Phase 14 should record the applicability decision and preserve manual/hardware visual checks separately.

## Existing Test Assets To Reuse

- Root tests: `ParticleApiDelegationBoundaryTest`, `ParticleSpawnServiceBoundaryTest`, `ParticleRuntimeDelegationBoundaryTest`, `ParticleAssetRegistryPublisherAdapterTest`, `BrParticleLoaderPublicationTest`, `ParticleCommandRuntimeTest`, `EyelibParticleCommandBoundaryTest`, `SpawnParticlePacketTest`, `RemoveParticlePacketTest`, `ParticleNetworkDelegationBoundaryTest`, `ParticleCommandNetworkDocumentationTest`.
- Particle-module tests: `ParticleDefinitionAdapterTest`, `ParticleDefinitionBoundaryTest`, `ParticleDefinitionDocumentationTest`, `ParticleRuntimeBoundaryTest`, `ParticleRuntimeSupportTest`, `ParticleRuntimeLifecycleTest`, `ParticleComponentRuntimeTest`, `EmitterComponentRuntimeTest`, `ParticleRenderManagerLifecycleTest`, `ParticleClientIntegrationBoundaryTest`, `ParticleLoadingBoundaryTest`, `ParticleResourcePublicationTest`, `ParticleStoreContractTest`, `ParticlePublisherAndSpawnApiTest`.

## Recommended Implementation Shape

### Plan 1 — Documentation convergence

Update only inconsistent stable docs and add final evidence/checklist planning artifacts. Use the final ownership story from D-05/D-06: `:eyelib-particle` owns APIs, runtime definitions, adapter, executable runtime, client integration, render manager, and loading/publication; root owns Forge/resource, command, network, and compatibility adapters; importer owns raw `BrParticle` schema.

### Plan 2 — Final split verification tests

Add final gate JUnit tests that aggregate boundary, parity, publication, delegation, side, and documentation invariants without depending on `.planning`. Adapt stale particle tests only when assertions are outdated by the final split and preserve the original observable behavior.

### Plan 3 — JetBrains MCP verification matrix and closure evidence

Run targeted module/root compile and test matrix via JetBrains MCP, capture exact task names/results, triage broad root `:test` if run, record ClientSmoke applicability, record hardware/manual visual checklist, and produce maintainer-oriented closure evidence for PVERIFY-01/PVERIFY-02.

## Validation Architecture

| Requirement | Validation mechanism | Required evidence |
|-------------|----------------------|-------------------|
| PVERIFY-01 | JUnit final gate tests plus existing targeted particle tests. | All particle-related assertions remain at least as strong; stale assertions are updated only to match final boundaries; targeted test filters exit 0. |
| PVERIFY-02 | JetBrains MCP Gradle matrix, ClientSmoke applicability decision, hardware/manual checklist. | `jetbrain_run_gradle_tasks` task names, exit codes, targeted groups, ClientSmoke/hardware status, residual risks, and milestone closure rationale. |

## Risks And Mitigations

| Risk | Mitigation |
|------|------------|
| Weakening old tests while adapting them to final boundaries. | Add acceptance criteria requiring retained observable assertions and new final aggregate tests. |
| Normal tests read `.planning` artifacts. | Add docs test acceptance criteria rejecting `.planning/` and `VALIDATION.md` dependencies. |
| Shell Gradle accidentally used. | Put JetBrains MCP invocation text in every verify step and evidence artifact. |
| Broad root failures obscure particle evidence. | Triage broad failures into particle-gate regressions vs unrelated fixtures per D-03/D-11. |
| Manual visual behavior is treated as automated blocker. | Keep ClientSmoke/hardware evidence separate and explicit per D-13/D-15. |

## Research Complete

Phase 14 can be planned as three focused execution plans: docs/evidence setup, final automated tests, and MCP verification/closure.
