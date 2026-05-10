---
phase: 15
slug: pre-migration-audit-routing
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-10
---

# Phase 15 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Java source/static verification plus JetBrains MCP Gradle build |
| **Config file** | `build.gradle`, `settings.gradle`, `.planning/config.json` |
| **Quick run command** | `jetbrain_search_regex` for residual util wildcard imports and moved-class old imports |
| **Full suite command** | `jetbrain_build_project` with `rebuild=false` or the existing JetBrains build run configuration |
| **Estimated runtime** | ~30-180 seconds depending on IDE Gradle sync state |

---

## Sampling Rate

- **After every task commit:** Run the relevant residual static scan for that task's touched import/package paths.
- **After every plan wave:** Run JetBrains diagnostics/build verification for touched Java packages.
- **Before `/gsd-verify-work`:** Full JetBrains build or equivalent existing build run configuration must be green.
- **Max feedback latency:** 180 seconds when Gradle is already synced.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 15-01-01 | routing manifest | 0 | AUDIT-01, ROUTE-02 | — | N/A | docs/static | Compare current `src/main/java/io/github/tt432/eyelib/util/**/*.java` and `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` inventory against `docs/architecture/migration/utility-routing-manifest.md` rows | ✅ | ✅ green |
| 15-02-01 | wildcard cleanup | 1 | AUDIT-02 | T-15-01 | Build integrity preserved by explicit imports | static | `jetbrain_search_regex` pattern `import\s+io\.github\.tt432\.eyelib\.util(?:\.[A-Za-z0-9_]+)*\.\*;` returns zero results | ✅ | ✅ green |
| 15-03-01 | single-consumer routing | 1 | ROUTE-01 | T-15-02 | Moved modbridge code preserves payload bound because retained classes moved to `mc/impl/modbridge` | static/build | IDE diagnostics for `AnimationApplier`, `Models`, `ModBridgeServer`, `BBModelSink`; residual old path import scan returns zero | ✅ | ✅ green |
| 15-04-01 | phase verification | 2 | AUDIT-01, AUDIT-02, ROUTE-01, ROUTE-02 | T-15-10, T-15-11, T-15-12 | Build integrity and retained ModBridge ownership verified | build/docs | `jetbrain_build_project(rebuild=false)` succeeded; manifest includes `ListHelper`/`EitherHelper` deletion plans; docs updated for actual moved owners | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `docs/architecture/migration/utility-routing-manifest.md` — manifest rows cover the current utility/core utility inventory; after Plan 03 moves, JetBrains file search found 28 current root util Java files and 5 current core util Java files, all represented exactly once by current-path rows.
- [x] Optional local modbridge README not required; `MODULES.md` and `docs/architecture/01-module-boundaries.md` record `ModBridgeServer`/`BBModelSink` ownership under `mc/impl/modbridge`.
- [x] JetBrains MCP build gate completed through `jetbrain_build_project(rebuild=false)` with `isSuccess=true`; no shell Gradle command was used.

---

## Execution Evidence

| Check | Tool | Result |
|-------|------|--------|
| Current root util Java inventory | `jetbrain_search_file` | PASS — 28 current `src/main/java/io/github/tt432/eyelib/util/**/*.java` files after the four Plan 03 moves. |
| Current core util Java inventory | `jetbrain_search_file` | PASS — 5 current `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` files. |
| Manifest current-path coverage | read-back comparison against current JetBrains file-search inventory | PASS — every current util/core-util Java path appears once in `docs/architecture/migration/utility-routing-manifest.md`. |
| Util wildcard residual scan | `jetbrain_search_regex` pattern `import\s+io\.github\.tt432\.eyelib\.util(?:\.[A-Za-z0-9_]+)*\.\*;` over `src/main/java/**/*.java` | PASS — zero matches. |
| Old moved-class package residual scan | `jetbrain_search_regex` pattern `io\.github\.tt432\.eyelib\.util\.(client|modbridge)\.(AnimationApplier|Models|ModBridgeServer|BBModelSink)` over `src/main/java/**/*.java` | PASS — zero matches. |
| Touched Java diagnostics | `ide_ide_diagnostics(severity=errors)` | PASS — `problemCount=0` for `BrAnimationEntry`, `TupleCodec`, `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink`. |
| Project build gate | `jetbrain_build_project(projectPath="E:\\_ideaProjects\\qylEyelib", rebuild=false)` | PASS — `isSuccess=true`; IDE reported limited build diagnostics collection only. |

---

## Manual-Only Verifications

All phase behaviors have automated or static verification. Human intervention is only required if semantic reference checks prove a roadmap-named class has zero consumers and the executor cannot decide move vs delete without accepting a scope interpretation.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s when Gradle is synced
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-05-10
