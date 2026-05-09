---
phase: 14
slug: verification-documentation-gate
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-09
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 via Gradle `test` tasks |
| **Config file** | Root `build.gradle`, subproject `eyelib-particle/build.gradle` |
| **Quick run command** | JetBrains MCP `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test", ":test"]` scriptParameters with targeted `--tests` filters |
| **Full suite command** | JetBrains MCP `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]`, plus targeted root `:test` filters |
| **Estimated runtime** | Targeted matrix under normal local IDE Gradle runtime; broad root `:test` may be longer and requires triage |

## Sampling Rate

- **After every task commit:** Run that task's targeted JetBrains MCP `jetbrain_run_gradle_tasks` command.
- **After every plan wave:** Run all commands listed for the wave in the plan verification section.
- **Before `/gsd-verify-work`:** `:eyelib-particle:test`, `:eyelib-particle:compileJava`, `:compileJava`, and the targeted root particle/documentation test filter must be green or have a documented blocker.
- **Max feedback latency:** immediate after each task; no three consecutive tasks may omit automated verification.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 14-01-01 | 01 | 1 | PVERIFY-02 | T-14-01-01 | Stable docs do not expose misleading ownership claims | docs path check | JetBrains MCP-free path/existence check plus targeted docs grep by executor | ✅ | ⬜ pending |
| 14-01-02 | 01 | 1 | PVERIFY-02 | T-14-01-02 | Manual evidence remains separate from automated gates | artifact check | JetBrains MCP-free path/existence check | ✅ | ⬜ pending |
| 14-02-01 | 02 | 2 | PVERIFY-01 | T-14-02-01 | Tests do not depend on `.planning` artifacts | JUnit source/static | JetBrains MCP `jetbrain_run_gradle_tasks` taskNames=`[":test"]` scriptParameters=`--tests io.github.tt432.eyelib.docs.ParticleFinalDocumentationGateTest --tests io.github.tt432.eyelib.client.particle.ParticleFinalSplitBoundaryTest` | ✅ | ⬜ pending |
| 14-02-02 | 02 | 2 | PVERIFY-01 | T-14-02-02 | Particle module forbidden imports remain scoped correctly | JUnit source/static | JetBrains MCP `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test"]` scriptParameters=`--tests io.github.tt432.eyelibparticle.ParticleModuleFinalBoundaryTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest --tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest` | ✅ | ⬜ pending |
| 14-03-01 | 03 | 3 | PVERIFY-02 | T-14-03-01 | Verification evidence uses JetBrains MCP task results only | Gradle MCP | JetBrains MCP `jetbrain_run_gradle_tasks` matrix in `14-MCP-VERIFICATION-MATRIX.md` | ✅ | ⬜ pending |
| 14-03-02 | 03 | 3 | PVERIFY-01, PVERIFY-02 | T-14-03-02 | Closure distinguishes regressions from unrelated residuals | evidence review | JetBrains MCP targeted matrix and artifact checklist | ✅ | ⬜ pending |

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test framework install, Gradle shell command, or new external service setup is required.

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real Minecraft particle rendering visual confidence | PVERIFY-02 | In-game visual output and Windows hardware exit-code capture are explicitly manual/deferred where ClientSmoke cannot assert them automatically. | Use `14-HARDWARE-CHECKLIST.md`; record whether visual particle spawn/remove/render behavior was manually observed, not as an automated Gradle blocker. |

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or existing infrastructure coverage.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all MISSING references: none needed.
- [x] No watch-mode flags.
- [x] Gradle verification path is JetBrains MCP only.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** pending execution
