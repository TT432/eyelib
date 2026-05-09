---
phase: 12
slug: loading-publication-rewire
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-05-09
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for loading/publication rewiring. All automated execution commands below are JetBrains MCP Gradle tasks; do not run Gradle through shell.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 through Gradle test tasks |
| **Config file** | `build.gradle`, `eyelib-particle/build.gradle` |
| **Quick run command** | `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.* --tests io.github.tt432.eyelibparticle.api.*"` |
| **Full suite command** | `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":test", ":eyelib-particle:compileJava", ":compileJava"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.* --tests io.github.tt432.eyelibparticle.api.* --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest --tests io.github.tt432.eyelib.client.loader.*Particle* --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistry* --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.Particle*BoundaryTest"` |
| **Estimated runtime** | Repository-local targeted Gradle tasks; executor records actual MCP task ids and durations. |

## Sampling Rate

- **After every task:** Run the task-specific JetBrains MCP Gradle check from the plan.
- **After every wave:** Run the wave-level targeted Gradle check for the touched module(s).
- **Before verification:** Run the full suite command above through JetBrains MCP.
- **Max feedback latency:** One targeted test run between behavior-changing tasks.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | PLOAD-01/PLOAD-02 | T-12-01 | malformed JSON/conversion data logs and skips without publishing attacker-controlled source keys | unit | `JetBrains MCP jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest"` | ❌ W0 | ⬜ pending |
| 12-01-02 | 01 | 1 | PLOAD-01/PLOAD-02 | T-12-02 | active store replacement removes stale entries and remains string-keyed by definition identifier | unit | `JetBrains MCP jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest --tests io.github.tt432.eyelibparticle.api.ParticlePublisherTest"` | ❌ W0 | ⬜ pending |
| 12-02-01 | 02 | 2 | PLOAD-01/PLOAD-03 | T-12-03 | root reload adapter keeps ResourceLocation at integration boundary only | unit/static | `JetBrains MCP jetbrain_run_gradle_tasks taskNames=[":test"] scriptParameters="--tests io.github.tt432.eyelib.client.loader.BrParticleLoaderPublicationTest"` | ❌ W0 | ⬜ pending |
| 12-02-02 | 02 | 2 | PLOAD-02/PLOAD-03 | T-12-04 | root compatibility adapters delegate to module registry and preserve lookup/spawn behavior | unit/static | `JetBrains MCP jetbrain_run_gradle_tasks taskNames=[":test"] scriptParameters="--tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistry* --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.Particle*BoundaryTest"` | ✅/❌ W0 additions | ⬜ pending |
| 12-03-01 | 03 | 3 | PLOAD-03 | T-12-05 | docs and scans make ownership traceable and reject hidden root ownership | static/docs | `JetBrains MCP jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":test"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.ParticleLoadingBoundaryTest --tests io.github.tt432.eyelib.client.particle.ParticleApiDelegationBoundaryTest"` | ❌ W0 | ⬜ pending |
| 12-03-02 | 03 | 3 | PLOAD-01/PLOAD-02/PLOAD-03 | T-12-06 | compile and targeted regression gates prove loader/publication compatibility | compile/unit | `JetBrains MCP jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":test", ":eyelib-particle:compileJava", ":compileJava"] scriptParameters="--tests io.github.tt432.eyelibparticle.loading.* --tests io.github.tt432.eyelibparticle.api.* --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest --tests io.github.tt432.eyelib.client.loader.*Particle* --tests io.github.tt432.eyelib.client.registry.ParticleAssetRegistry* --tests io.github.tt432.eyelib.client.manager.ParticleManagerStoreAdapterTest --tests io.github.tt432.eyelib.client.particle.Particle*BoundaryTest"` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

## Wave 0 Requirements

- [ ] `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublicationTest.java` — failing behavior tests for module-owned JSON parse/convert/publish semantics.
- [ ] `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleLoadingBoundaryTest.java` — static boundary tests for root/MC/Forge cleanliness in pure particle loading/publication packages.
- [ ] `src/test/java/io/github/tt432/eyelib/client/loader/BrParticleLoaderPublicationTest.java` — root adapter tests for unchanged `particles/*.json` scanning contract and module delegation.
- [ ] Update existing root adapter tests instead of deleting or weakening: `ParticleAssetRegistryTest`, `ParticleAssetRegistryPublisherAdapterTest`, `ParticleManagerStoreAdapterTest`, `ParticleApiDelegationBoundaryTest`, `ParticleRuntimeDelegationBoundaryTest`.

## Manual-Only Verifications

All Phase 12 behaviors have automated verification. Visual/client smoke evidence is intentionally Phase 14 scope.

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all MISSING references.
- [x] No watch-mode flags.
- [x] Feedback latency constrained to targeted JetBrains MCP Gradle checks.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** pending execution
