---
phase: 11
slug: runtime-client-core-extraction
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-05-09
---

# Phase 11 — Validation Strategy

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter via Gradle/Forge multi-project build |
| **Config file** | root `build.gradle`, `eyelib-particle/build.gradle` |
| **Quick run command** | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava"]` |
| **Full suite command** | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]` |
| **Estimated runtime** | Under normal local compile/test time; use targeted tests after each task and full command after each wave |

## Sampling Rate

- **After every task commit:** run the task's targeted JetBrains MCP Gradle command.
- **After every plan wave:** run JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]`.
- **Before `/gsd-verify-work`:** the full suite command above must be green.
- **Max feedback latency:** no more than one task without an automated targeted command.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | PRENDER-01/PRENDER-02 | T-11-01 | runtime contracts exist before movement | boundary/unit | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava"]` | ✅ | ⬜ pending |
| 11-01-02 | 01 | 1 | PRENDER-02 | T-11-02 | pure runtime forbidden imports blocked | static | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleRuntimeBoundaryTest"]` | ✅ | ⬜ pending |
| 11-02-01 | 02 | 2 | PRENDER-01 | — | emitter component behavior preserved | unit | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.EmitterComponentRuntimeTest"]` | ✅ | ⬜ pending |
| 11-03-01 | 03 | 2 | PRENDER-01 | — | particle component behavior preserved | unit | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentRuntimeTest"]` | ✅ | ⬜ pending |
| 11-04-01 | 04 | 3 | PRENDER-01 | T-11-03 | emitter/particle lifecycle testable without Forge event loading | unit | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeLifecycleTest"]` | ✅ | ⬜ pending |
| 11-05-01 | 05 | 4 | PRENDER-01/PRENDER-02 | T-11-04 | client hooks are Dist.CLIENT and delegate into service | static/unit | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test --tests io.github.tt432.eyelibparticle.client.ParticleClientIntegrationBoundaryTest", ":eyelib-particle:compileJava"]` | ✅ | ⬜ pending |
| 11-06-01 | 06 | 5 | PRENDER-01/PRENDER-02 | T-11-05 | root facades delegate and no packet shape changes | static/integration | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":test --tests io.github.tt432.eyelib.client.particle.ParticleSpawnServiceBoundaryTest", ":compileJava"]` | ✅ | ⬜ pending |

## Wave 0 Requirements

- Existing JUnit infrastructure is present in root and `:eyelib-particle`.
- Wave 1 plan creates/strengthens the Phase 11 boundary and lifecycle test scaffolds before runtime movement.

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real client visual particle rendering | PRENDER-01 | Requires Minecraft client rendering/device state and is deferred by D-17/Phase 14 | Compile first; Phase 14 will use existing dev-client or hardware checklist path for final visual evidence. |

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 1 scaffold dependencies.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 1 covers boundary/lifecycle scaffolding.
- [x] No watch-mode flags.
- [x] Gradle verification is specified through JetBrains MCP only.

**Approval:** draft 2026-05-09
