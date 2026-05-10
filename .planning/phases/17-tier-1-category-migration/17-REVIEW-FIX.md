---
phase: 17
fixed_at: 2026-05-10T00:00:00Z
review_path: .planning/phases/17-tier-1-category-migration/17-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 17: Code Review Fix Report

**Fixed at:** 2026-05-10T00:00:00Z
**Source review:** `.planning/phases/17-tier-1-category-migration/17-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 4
- Fixed: 4
- Skipped: 0

## Fixed Issues

### WR-01: Public util APIs leak non-exported dependencies

**Files modified:** `eyelib-util/build.gradle`
**Commit:** Not committed per user constraint.
**Applied fix:** Promoted JOML to `api` and declared FastUtil `api` while preserving zero `project(...)` dependencies.

### WR-02: Validation artifact still marks Phase 17 requirements as pending

**Files modified:** `.planning/phases/17-tier-1-category-migration/17-VALIDATION.md`
**Commit:** Not committed per user constraint.
**Applied fix:** Marked the Phase 17 per-task verification map and Wave 0 checklist complete using the existing final gate evidence in the validation artifact.

### WR-03: Active util package documentation still says Phase 16 scaffold must not receive implementations

**Files modified:** `eyelib-util/src/main/java/io/github/tt432/eyelibutil/package-info.java`, `eyelib-util/src/test/java/io/github/tt432/eyelibutil/UtilModuleIdentityTest.java`
**Commit:** Not committed per user constraint.
**Applied fix:** Replaced scaffold-only wording with active Phase 17 ownership wording for `time`, `color`, `loader`, `math`, `search`, and `collection` packages, and updated the module identity test to assert the active Phase 17 package contract.

### WR-04: SharedLibraryLoader silently ignores checksum read failures

**Files modified:** `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java`
**Commit:** Not committed per user constraint.
**Applied fix:** Changed checksum calculation to propagate read failures as a contextual unchecked exception so native extraction/loading fails closed instead of comparing a partial CRC.

## Verification

- Re-read all modified sections after patching.
- `ide_ide_diagnostics` on `SharedLibraryLoader.java` reported no errors.
- `jetbrain_run_gradle_tasks` with `:eyelib-util:build` completed successfully with exit code `0`.

---

_Fixed: 2026-05-10T00:00:00Z_
_Fixer: the agent (gsd-code-fixer)_
_Iteration: 1_
