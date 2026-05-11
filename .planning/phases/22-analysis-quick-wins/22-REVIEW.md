---
phase: 22-analysis-quick-wins
reviewed: 2026-05-11T14:32:51Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/io/github/tt432/eyelib/client/animation/KeyFrame.java
  - src/main/java/io/github/tt432/eyelib/client/instrument/InstrumentConfig.java
  - src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java
  - src/main/java/io/github/tt432/eyelib/client/instrument/db/BackgroundFlushService.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/InstrumentDisabledTest.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabaseTest.java
findings:
  critical: 1
  warning: 1
  info: 0
  total: 2
status: issues_found
---

# Phase 22: Code Review Report

**Reviewed:** 2026-05-11T14:32:51Z
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Reviewed the Phase 22 source changes identified by the phase summary: deletion of the zero-reference `KeyFrame` interface and the instrumentation database path move from the project root to `.cache/eyelib_instrument`. The broad fallback scope file listed 1094 existing files, but the machine-readable phase summary and targeted diff identify the actionable Phase 22 source surface above. No issue was found with the deleted zero-reference `KeyFrame` after checking for direct fully qualified references, but the instrumentation path change introduces one runtime robustness regression and one test isolation defect.

## Critical Issues

### CR-01: BLOCKER — Cache-directory creation failures now escape the flush error boundary

**File:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java:45-49`  
**Also affected:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/BackgroundFlushService.java:139-142`

**Issue:** The new `.cache` directory creation wraps `IOException` in `UncheckedIOException`, but `BackgroundFlushService.flush()` only catches `SQLException`. If `.cache` cannot be created because a file already exists at that path, permissions deny creation, or the working directory is read-only, instrumentation no longer degrades gracefully; the unchecked exception can escape a manual flush/shutdown and can kill the scheduled flush task. This violates the class contract that DB failures must not crash the render path.

**Fix:** Keep `getConnection()` failures inside the existing checked `SQLException` path, or catch the unchecked failure at the flush boundary. Preferred fix:

```java
try {
    Files.createDirectories(DB_BASE_PATH.getParent());
} catch (java.io.IOException e) {
    throw new SQLException("Failed to create instrumentation cache directory", e);
}
```

Then the existing `catch (final SQLException ex)` in `BackgroundFlushService.flush()` continues to log and degrade safely.

## Warnings

### WR-01: WARNING — Disabled instrumentation test is order-dependent on stale `.cache` DB files

**File:** `src/test/java/io/github/tt432/eyelib/client/instrument/InstrumentDisabledTest.java:153-164`

**Issue:** `noDatabaseFilesCreated()` asserts that `.cache/eyelib_instrument.*.db` files do not exist, but cleanup runs only in `tearDown()`. Other instrumentation tests in the same suite intentionally create the shared H2 file and do not remove it. If `InstrumentDisabledTest` runs after those tests, or if a developer has a stale `.cache/eyelib_instrument.mv.db`, this assertion fails even though the disabled code path did not create a database during the test.

**Fix:** Make the test establish its own precondition before the assertion, preferably via a shared cleanup helper called from both `@BeforeEach` and `@AfterEach`:

```java
@BeforeEach
void setUp() {
    System.setProperty("eyelib.instrument.enabled", "false");
    deleteInstrumentationDatabaseFiles();
}

@AfterEach
void tearDown() {
    System.clearProperty("eyelib.instrument.enabled");
    BackgroundFlushService.getInstance().shutdown();
    deleteInstrumentationDatabaseFiles();
}

private static void deleteInstrumentationDatabaseFiles() {
    try {
        Files.deleteIfExists(Path.of(".cache", "eyelib_instrument.mv.db"));
        Files.deleteIfExists(Path.of(".cache", "eyelib_instrument.trace.db"));
    } catch (final Exception ignored) {
        // best-effort cleanup
    }
}
```

---

_Reviewed: 2026-05-11T14:32:51Z_  
_Reviewer: the agent (gsd-code-reviewer)_  
_Depth: standard_
