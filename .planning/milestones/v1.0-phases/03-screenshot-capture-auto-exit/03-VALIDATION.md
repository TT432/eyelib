---
phase: 3
slug: screenshot-capture-auto-exit
status: verified
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-07
updated: 2026-05-07
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter, as configured in Phase 1 build.gradle) |
| **Config file** | `eyelib-clientsmoke/build.gradle` — `testImplementation` dependencies |
| **Quick run command** | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` |
| **Full suite command** | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}`
- **After every plan wave:** Run `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | CAP-01 | T-03-01 / — | Framebuffer read only on render thread via RenderLevelStageEvent | unit | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | CAP-02 | T-03-02 / — | HUD toggle via options.hideGui, restored after capture | unit | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` | ❌ W0 | ⬜ pending |
| 03-01-03 | 01 | 1 | CAP-03 | T-03-03 / — | Path resolution uses FMLPaths.GAMEDIR, no path traversal | unit | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | EXIT-01 | T-03-04 / — | exitAfterSmoke=false → IDLE, true → orderly exit | unit | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 2 | EXIT-02 | T-03-05 / — | Two-phase: mc.stop() graceful + halt(0) force, no hanging hooks | unit | `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/` — stubs for CAP-01 through EXIT-02
- [ ] Framework install: `jetbrain_run_gradle_tasks` with `{":eyelib-clientsmoke:test"}` — JUnit 5 should be already configured from Phase 1

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Screenshot PNG is free of HUD elements | CAP-02 | Visual inspection of output PNG file — automated pixel analysis is fragile and out of scope | Open `clientsmoke-reports/screenshots/{test}-{timestamp}.png` in any image viewer; verify no hotbar, crosshair, chat overlay, or debug screen |
| JVM exits within 5 seconds | EXIT-01 | Timing measurement requires real Minecraft client launch — unit tests can verify state transitions but not wall-clock exit duration | Launch dev client with `enabled=true` and `exitAfterSmoke=true`, observe client window closes within 5 seconds of stabilization |
| Exit logs show mc.stop() + halt(0) sequence | EXIT-02 | Log verification in running client — unit tests can check logic but not actual JVM lifecycle | Check log output for "Initiating exit — calling mc.stop()" followed by "Exit complete — halting JVM" |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
