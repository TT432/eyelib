---
phase: 09
slug: particle-api-store-seam
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-09
---

# Phase 09 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter plus JetBrains MCP Gradle compile/test checks |
| **Config file** | root `build.gradle`, `eyelib-particle/build.gradle` |
| **Quick run command** | JetBrains MCP `jetbrain_run_gradle_tasks` for `:eyelib-particle:compileJava` and affected root compile/test task |
| **Full suite command** | JetBrains MCP Gradle tasks selected by the plan, expected to include particle module compile/tests and root compile/tests for touched adapters |
| **Estimated runtime** | Project-dependent; use MCP timeouts appropriate for Forge compile/test |

---

## Sampling Rate

- **After every task commit:** Run the task's file/content/static checks; run MCP compile when Java/build files changed.
- **After every plan wave:** Run the planned JetBrains MCP Gradle checks for particle module and touched root adapters.
- **Before `/gsd-verify-work`:** All planned MCP Gradle checks and boundary/static checks must be green.
- **Max feedback latency:** One task/wave; do not defer API contract or reverse-dependency failures.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | PAPI-01 | T-09-01 | N/A | compile/static | Verify new `eyelibparticle.api` surfaces compile and are consumed by root adapters | W0 | pending |
| 09-01-02 | 01 | 1 | PAPI-03 | T-09-02 | N/A | file/content | Verify retained root facades document transitional delegation and removal conditions | W0 | pending |
| 09-02-01 | 02 | 2 | PAPI-01 | T-09-03 | N/A | behavior/unit/static | Verify lookup/store/publication/spawn/remove seams preserve existing string-keyed behavior | W0 | pending |
| 09-02-02 | 02 | 2 | PAPI-03 | T-09-04 | N/A | static | Verify root facades delegate to particle API and do not duplicate particle business logic | W0 | pending |

*Status: pending · green · red · flaky*

---

## Wave 0 Requirements

Existing JUnit/Gradle infrastructure covers this phase. Plans may add focused boundary/static tests, but no new test framework is required.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Maintainer can identify transitional root facades | PAPI-03 | Documentation readability is partly qualitative | Read local README/Javadoc notes for retained root facades and confirm each explains delegation target and removal condition. |

---

## Validation Sign-Off

- [x] All tasks have automated verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency is bounded to each task/wave
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-05-09
