---
phase: 08
slug: boundary-contract-gradle-module-skeleton
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-09
---

# Phase 08 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Gradle Java/Forge build checks via JetBrains MCP; file/content checks for docs and module wiring |
| **Config file** | `settings.gradle`, root `build.gradle`, `eyelib-particle/build.gradle` |
| **Quick run command** | JetBrains MCP: build/sync affected Gradle project files, plus file existence/content checks |
| **Full suite command** | JetBrains MCP Gradle task for root/module compile or build selected by executor after module skeleton exists |
| **Estimated runtime** | Project-dependent; use MCP timeout appropriate for Gradle sync/build |

---

## Sampling Rate

- **After every task commit:** Verify touched files exist and contain the exact required module/dependency/doc strings.
- **After every plan wave:** Run the planned JetBrains MCP Gradle verification for the affected root/module scope.
- **Before `/gsd-verify-work`:** Full planned MCP Gradle check must be green or a blocker must be recorded.
- **Max feedback latency:** One task/wave; do not defer Gradle/module wiring failures past phase verification.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | PGRAD-01 | T-08-01 | N/A | file/content | Check `settings.gradle` and `eyelib-particle/build.gradle` contain planned module wiring | W0 | pending |
| 08-01-02 | 01 | 1 | PGRAD-02 | T-08-02 | N/A | file/content | Check `eyelib-particle/README.md` and `MODULES.md` document ownership and dependency direction | W0 | pending |
| 08-01-03 | 01 | 1 | PAPI-02 | T-08-03 | N/A | static/build | Use JetBrains MCP Gradle verification; inspect `eyelib-particle` for no root `io.github.tt432.eyelib.*` or `mc/impl` dependency imports | W0 | pending |

*Status: pending · green · red · flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. This phase creates module skeleton and docs; no new test framework is required before implementation.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Maintainer readability of boundary documentation | PGRAD-02 | Documentation clarity is partially qualitative | Read `eyelib-particle/README.md` and `MODULES.md`; confirm they state ownership, dependency direction, allowed integration layers, and JetBrains MCP-only Gradle verification. |

---

## Validation Sign-Off

- [x] All tasks have automated verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency is bounded to each task/wave
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-05-09
