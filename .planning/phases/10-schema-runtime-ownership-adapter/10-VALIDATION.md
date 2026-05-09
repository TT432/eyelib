---
phase: 10
slug: schema-runtime-ownership-adapter
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-09
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter plus JetBrains MCP Gradle compile/test checks |
| **Config file** | `eyelib-particle/build.gradle`, `eyelib-importer/build.gradle`, root `build.gradle` |
| **Quick run command** | JetBrains MCP targeted `:eyelib-particle:test` for adapter/boundary tests |
| **Full suite command** | JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :compileJava` plus targeted root tests if root legacy docs/tests change |
| **Estimated runtime** | Project-dependent; use MCP timeouts appropriate for Forge compile/test |

---

## Sampling Rate

- **After every task commit:** Run adapter/boundary tests for touched files and static file checks for ownership docs.
- **After every plan wave:** Run the planned JetBrains MCP compile/test subset.
- **Before `/gsd-verify-work`:** Adapter parity, forbidden import, and documentation invariant checks must be green.
- **Max feedback latency:** One task/wave.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | PSCHEMA-01 | T-10-01 | N/A | docs/static | Verify owner docs name importer schema, particle runtime definition, and root legacy status | W0 | pending |
| 10-01-02 | 01 | 1 | PSCHEMA-02 | T-10-02 | N/A | unit/fixture | JetBrains MCP `:eyelib-particle:test` adapter tests using real fixture | W0 | pending |
| 10-02-01 | 02 | 2 | PSCHEMA-03 | T-10-03 | N/A | boundary/static | Verify no `BrParticle` duplicate in particle module and no forbidden root/Minecraft/Forge imports | W0 | pending |
| 10-02-02 | 02 | 2 | PSCHEMA-02/03 | T-10-04 | N/A | parity | Verify large real fixture preserves identifier, render params, curves, events, raw components, and flipbook summary | W0 | pending |

*Status: pending · green · red · flaky*

---

## Wave 0 Requirements

Existing JUnit/Gradle infrastructure covers this phase. Plans may copy or reference a real particle fixture with explicit source notes if direct cross-module fixture access is impractical.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Ownership documentation clarity | PSCHEMA-01 | Documentation clarity is partly qualitative | Read module/root docs and confirm canonical owners and legacy status are unambiguous. |

---

## Validation Sign-Off

- [x] All tasks have automated verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency is bounded to each task/wave
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-05-09
