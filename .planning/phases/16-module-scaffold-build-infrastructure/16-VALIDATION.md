---
phase: 16
slug: module-scaffold-build-infrastructure
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-10
---

# Phase 16 — Validation Strategy

> Per-phase validation contract for `:eyelib-util` module scaffolding.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Gradle/Java module build via JetBrains MCP, optional JUnit identity test |
| **Config file** | `settings.gradle`, `eyelib-util/build.gradle`, `eyelib-util/src/main/resources/META-INF/mods.toml` |
| **Quick run command** | JetBrains file/regex checks for module include, zero `project(...)`, and `eyelibutil` mod id |
| **Full suite command** | `jetbrain_run_gradle_tasks` for `:eyelib-util:build` after JetBrains Gradle sync, or `jetbrain_build_project` if task execution is unavailable |
| **Estimated runtime** | ~60-240 seconds depending on Gradle sync state |

---

## Sampling Rate

- **After every task commit:** Run static checks for the files touched by that task.
- **After every plan wave:** Run IDE diagnostics/file checks for the module skeleton.
- **Before `/gsd-verify-work`:** `:eyelib-util` solo build must pass through JetBrains MCP.
- **Max feedback latency:** 240 seconds when Gradle sync is current.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 16-01-01 | scaffold | 0 | MOD-01, MOD-02 | T-16-01 | N/A | static | Verify `settings.gradle` includes `eyelib-util`, `eyelib-util/build.gradle` exists and contains no `project(` | ✅ | ✅ green |
| 16-02-01 | metadata | 1 | MOD-01, MOD-02 | T-16-02 | Unique mod identity prevents loader collision | static/diagnostic | Verify `mods.toml` contains `modId="eyelibutil"` and package `io.github.tt432.eyelibutil` exists | ✅ | ✅ green |
| 16-03-01 | docs | 1 | MOD-02 | — | N/A | docs/static | Verify `eyelib-util/README.md`, package README, `MODULES.md`, and architecture docs document leaf dependency direction | ✅ | ✅ green |
| 16-04-01 | build | 2 | MOD-01 | T-16-03 | Build integrity proves scaffold correctness | build | JetBrains MCP Gradle sync then `:eyelib-util:build` exits 0 | ✅ tool | ✅ green |

---

## Wave 0 Requirements

- [x] `eyelib-util/build.gradle` — Forge Gradle module skeleton with zero `project(...)` dependencies.
- [x] `eyelib-util/src/main/resources/META-INF/mods.toml` — unique `eyelibutil` mod id.
- [x] `eyelib-util/src/main/java/io/github/tt432/eyelibutil/` — package root and minimal bootstrap/metadata class.
- [x] `eyelib-util/README.md` and package-local README/package-info files.

---

## Manual-Only Verifications

All Phase 16 success criteria should be automatically verifiable through static checks and JetBrains MCP Gradle build. No manual validation is expected.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all missing scaffold files
- [x] No watch-mode flags
- [x] Feedback latency < 240s when Gradle is synced
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-05-10
