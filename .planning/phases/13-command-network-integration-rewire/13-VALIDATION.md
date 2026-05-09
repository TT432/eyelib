---
phase: 13
slug: command-network-integration-rewire
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-09
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for command/network adapter rewiring.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 5.10.2 through Gradle |
| **Config file** | Root `build.gradle` / subproject Gradle config |
| **Quick run command** | JetBrains MCP `jetbrain_run_gradle_tasks(projectPath="E:/_ideaProjects/qylEyelib", taskNames=[":test"], scriptParameters="--tests io.github.tt432.eyelib.common.runtime.ParticleCommandRuntimeTest --tests io.github.tt432.eyelib.mc.impl.common.command.EyelibParticleCommandBoundaryTest --tests io.github.tt432.eyelib.network.SpawnParticlePacketTest --tests io.github.tt432.eyelib.network.RemoveParticlePacketTest --tests io.github.tt432.eyelib.network.ParticleNetworkDelegationBoundaryTest --tests io.github.tt432.eyelib.docs.ParticleCommandNetworkDocumentationTest")` |
| **Full suite command** | JetBrains MCP `jetbrain_run_gradle_tasks(projectPath="E:/_ideaProjects/qylEyelib", taskNames=[":test", ":eyelib-particle:test", ":compileJava", ":eyelib-particle:compileJava"], scriptParameters="")` |
| **Estimated runtime** | Targeted tests under 60 seconds; full compile/test depends on Gradle daemon state |

## Sampling Rate

- **After every task commit:** Run the targeted JetBrains MCP root `:test` filters for the touched test classes.
- **After every plan wave:** Run the full JetBrains MCP compile/test command above.
- **Before `/gsd-verify-work`:** Full suite must be green, or any Gradle/MCP blocker must be recorded explicitly.
- **Max feedback latency:** One task; no more than two implementation tasks may occur without a targeted automated test run.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | PNET-01, PNET-03 | T-13-01 | Invalid suggestions are filtered and command inputs remain MC-adapter-only | unit/static | JetBrains MCP `:test --tests io.github.tt432.eyelib.common.runtime.ParticleCommandRuntimeTest --tests io.github.tt432.eyelib.mc.impl.common.command.EyelibParticleCommandBoundaryTest` | ✅ | ⬜ pending |
| 13-01-02 | 01 | 1 | PNET-01, PNET-03 | T-13-01 | Command sends string-keyed spawn packet and compatible success message | unit/static | JetBrains MCP command-runtime + command-boundary test filters | ✅ | ⬜ pending |
| 13-02-01 | 02 | 1 | PNET-02, PNET-03 | T-13-02 | Packets stay string-keyed and codecs remain MC/network-owned | unit/static | JetBrains MCP `:test --tests io.github.tt432.eyelib.network.SpawnParticlePacketTest --tests io.github.tt432.eyelib.network.RemoveParticlePacketTest --tests io.github.tt432.eyelib.network.ParticleNetworkDelegationBoundaryTest` | ✅ / W0 creates missing tests | ⬜ pending |
| 13-02-02 | 02 | 1 | PNET-02, PNET-03 | T-13-02 | Handler delegates only to particle service and no-ops missing runtime state | unit/static | JetBrains MCP network delegation test filters | ✅ / W0 creates missing tests | ⬜ pending |
| 13-03-01 | 03 | 2 | PNET-03 | T-13-03 | Documentation matches adapter ownership and deferred scope | static/docs | JetBrains MCP `:test --tests io.github.tt432.eyelib.docs.ParticleCommandNetworkDocumentationTest` | ✅ / W0 creates missing test | ⬜ pending |
| 13-03-02 | 03 | 2 | PNET-01, PNET-02, PNET-03 | T-13-04 | Final compile/test evidence uses JetBrains MCP only | Gradle MCP | JetBrains MCP `:test :eyelib-particle:test :compileJava :eyelib-particle:compileJava` | ✅ | ⬜ pending |

## Wave 0 Requirements

- Existing infrastructure covers command runtime tests and spawn packet tests.
- Wave 1 tasks create missing targeted test files before implementation changes:
  - `src/test/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommandBoundaryTest.java`
  - `src/test/java/io/github/tt432/eyelib/network/RemoveParticlePacketTest.java`
  - `src/test/java/io/github/tt432/eyelib/network/ParticleNetworkDelegationBoundaryTest.java`
  - `src/test/java/io/github/tt432/eyelib/docs/ParticleCommandNetworkDocumentationTest.java`

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| In-game visual particle render after command | Phase 14 / PVERIFY-02 | Hardware/client visual rendering evidence is explicitly deferred by D-23 | Do not require in Phase 13 unless a direct command/network regression appears. |

## Validation Sign-Off

- [x] All tasks have automated JetBrains MCP verify commands or Wave 0 test creation dependencies.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all missing test references.
- [x] No watch-mode flags.
- [x] Feedback latency target documented.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** pending execution
