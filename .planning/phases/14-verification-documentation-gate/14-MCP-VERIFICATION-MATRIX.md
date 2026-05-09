# Phase 14 JetBrains MCP Verification Matrix

**Scope:** Final automated Gradle evidence for Phase 14 Plan 03 / PVERIFY-01 / PVERIFY-02.  
**Rule:** All Gradle work in this matrix was run through JetBrains MCP `jetbrain_run_gradle_tasks`; no shell Gradle command was used.

## Required Matrix Rows

| Row | Purpose | JetBrains MCP Invocation | External Task ID | exitCode | Result / Output Summary |
|-----|---------|--------------------------|------------------|----------|-------------------------|
| 1 | Module compile/test gate | `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]` scriptParameters=`""` | 45 | 0 | PASS — `BUILD SUCCESSFUL in 2s`; 35 actionable tasks, 1 executed, 34 up-to-date. |
| 2 | Particle module final boundary/parity/loading/client tests | `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test"]` scriptParameters=`--tests io.github.tt432.eyelibparticle.ParticleModuleFinalBoundaryTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest --tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest --tests io.github.tt432.eyelibparticle.runtime.ParticleRuntimeBoundaryTest --tests io.github.tt432.eyelibparticle.loading.ParticleLoadingBoundaryTest --tests io.github.tt432.eyelibparticle.client.ParticleClientIntegrationBoundaryTest` | 46 | 0 | PASS — `BUILD SUCCESSFUL in 4s`; final module boundary, adapter parity, publication, runtime, loading, and client-integration filters passed. |
| 3 | Root final documentation, adapter, command, and network tests | `jetbrain_run_gradle_tasks` taskNames=`[":test"]` scriptParameters=`--tests io.github.tt432.eyelib.docs.ParticleFinalDocumentationGateTest --tests io.github.tt432.eyelib.client.particle.ParticleFinalSplitBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest --tests io.github.tt432.eyelib.client.loader.BrParticleLoaderPublicationTest --tests io.github.tt432.eyelib.common.runtime.ParticleCommandRuntimeTest --tests io.github.tt432.eyelib.mc.impl.common.command.EyelibParticleCommandBoundaryTest --tests io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacketTest --tests io.github.tt432.eyelib.mc.impl.network.packet.RemoveParticlePacketTest --tests io.github.tt432.eyelib.network.ParticleNetworkDelegationBoundaryTest --tests io.github.tt432.eyelib.docs.ParticleCommandNetworkDocumentationTest` | 47 | 0 | PASS — `BUILD SUCCESSFUL in 4s`; final stable-doc, root adapter/delegation, loading publication, command runtime, packet codec, and network delegation filters passed. |

## Optional Broad Root `:test` Row

| Row | Purpose | JetBrains MCP Invocation | External Task ID | exitCode | Result / Triage |
|-----|---------|--------------------------|------------------|----------|-----------------|
| 4a | Broad root suite pre-triage spot check | `jetbrain_run_gradle_tasks` taskNames=`[":test"]` scriptParameters=`""` | 48 | 1 | Found 4 failures: one stale particle lookup facade test plus three pre-existing geometry fixture `NoSuchFileException` failures. The stale particle test was fixed in Plan 03 as a Rule 1 broad-suite correctness fix. |
| 4b | Targeted verification for the stale broad-suite particle test after fix | `jetbrain_run_gradle_tasks` taskNames=`[":test"]` scriptParameters=`--tests io.github.tt432.eyelib.client.lookup.ClientLookupFacadeTest` | 49 | 0 | PASS — `BUILD SUCCESSFUL in 6s`; lookup facade test now publishes through the active module registry before asserting `ParticleLookup.names()`. |
| 4c | Broad root suite after stale particle-test fix | `jetbrain_run_gradle_tasks` taskNames=`[":test"]` scriptParameters=`""` | 50 | 1 | Non-blocking residual — 196 tests ran, 3 failed. All remaining failures are unrelated geometry/importer fixture path residuals: `BedrockGeometryImporterTest.skeletonFixtureIndependentReferenceMatchesImporterOutput`, `BedrockGeometryImporterTest.importsSkeletonFixtureKeepsBedrockCorneraairingOnRealModelFaces`, and `RenderGeometryDumpParityTest.skeletonRenderVisitorOutputCanBeReconstructedByGeometryCsvStyleSegmentation`, each with `NoSuchFileException`. They do not prevent required particle-gate rows 1-3 from running or passing. |

## Broad Failure Classification

| Failure | Classification | Blocks Particle Gate? | Rationale |
|---------|----------------|-----------------------|-----------|
| `ClientLookupFacadeTest.particleLookupExposesNamesAndGetThroughLookupSeam` from task 48 | stale particle broad-suite invariant, fixed during Plan 03 | No after fix | Test was still publishing only into the legacy `ParticleManager` map while final Phase 12/14 docs and source require `ParticleLookup.names()` to expose active module registry names. Fixed by publishing through `ParticleAssetRegistry.publishParticle` and clearing `ParticleDefinitionRegistry` in teardown. |
| `BedrockGeometryImporterTest.skeletonFixtureIndependentReferenceMatchesImporterOutput` | unrelated fixture residual | No | Importer/model geometry fixture `NoSuchFileException`; outside particle module split gate and unchanged by Phase 14 Plan 03. |
| `BedrockGeometryImporterTest.importsSkeletonFixtureKeepsBedrockCorneraairingOnRealModelFaces` | unrelated fixture residual | No | Same missing geometry fixture family; not a particle gate regression. |
| `RenderGeometryDumpParityTest.skeletonRenderVisitorOutputCanBeReconstructedByGeometryCsvStyleSegmentation` | unrelated fixture residual | No | Render geometry parity fixture `NoSuchFileException`; unrelated to particle loading, command/network, runtime/client, or final documentation gates. |

## NullAway Decision

`nullawayMain` was not run. Phase 14 Plan 03 changed evidence files and one JUnit test; it did not change null-safety-sensitive production code or annotations, matching D-12.

## Final Automated Gate Status

- Required rows 1-3: **PASS** (`exitCode 0`).
- Optional broad root row: **non-blocking residual after triage**; required particle-gate filters run and pass independently.
- Maintainer rerun path: use the exact `jetbrain_run_gradle_tasks` taskNames and scriptParameters recorded above.
