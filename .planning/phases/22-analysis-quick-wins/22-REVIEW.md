---
phase: 22-analysis-quick-wins
reviewed: 2026-05-11T14:40:08Z
depth: standard
files_reviewed: 5
files_reviewed_list:
  - src/main/java/io/github/tt432/eyelib/client/instrument/InstrumentConfig.java
  - src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/InstrumentConfigTest.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/InstrumentDisabledTest.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/db/BackgroundFlushServiceTest.java
findings:
  critical: 1
  warning: 1
  info: 0
  total: 2
status: issues_found
---

# Phase 22: Code Review Report

**Reviewed:** 2026-05-11T14:40:08Z
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Summary

Reviewed the explicitly scoped Phase 22 instrumentation files at standard depth, including the production `.cache/eyelib_instrument` path change and the updated tests around disabled instrumentation and background flushing. The path move introduces one runtime error-boundary regression, and the disabled-instrumentation assertion remains order-dependent on stale H2 files created by other instrumentation tests.

## Critical Issues

### CR-01: BLOCKER — Cache-directory creation failures now escape the flush error boundary

**File:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java:45-49`  
**Also affected:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/BackgroundFlushService.java:139-142`

**Issue:** The new `.cache` directory creation wraps `IOException` in `UncheckedIOException`, but the flush boundary only handles `SQLException`. If `.cache` cannot be created because a file already exists at that path, permissions deny creation, or the working directory is read-only, the unchecked exception can escape a manual flush/shutdown and can terminate the scheduled flush task instead of degrading through the existing logging path. That violates the instrumentation contract that database failures must not crash or kill the render-path instrumentation service.

**Fix:** Keep `getConnection()` failures inside the checked `SQLException` path so existing callers' error handling still applies:

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

**Issue:** `noDatabaseFilesCreated()` asserts that `.cache/eyelib_instrument.*.db` files do not exist, but the test does not establish that precondition before running. `BackgroundFlushServiceTest` intentionally opens the shared H2 database and only deletes rows, not the database files, so suite order or a developer's stale `.cache/eyelib_instrument.mv.db` can make this disabled-path test fail even when the disabled code did not create a database during the test.

**Fix:** Delete the instrumentation database files before the assertion as well as after the test, or move the database tests to a per-test temporary DB location. For the current shared path, use a cleanup helper from both `@BeforeEach` and `@AfterEach`:

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

_Reviewed: 2026-05-11T14:40:08Z_  
_Reviewer: the agent (gsd-code-reviewer)_  
_Depth: standard_
