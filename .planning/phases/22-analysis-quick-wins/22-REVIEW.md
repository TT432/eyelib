---
phase: 22-analysis-quick-wins
reviewed: 2026-05-11T00:00:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java
  - src/main/java/io/github/tt432/eyelib/client/instrument/InstrumentConfig.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabaseTest.java
  - src/test/java/io/github/tt432/eyelib/client/instrument/InstrumentDisabledTest.java
findings:
  critical: 2
  warning: 3
  info: 3
  total: 8
status: issues_found
---

# Phase 22: Code Review Report

**Reviewed:** 2026-05-11
**Depth:** standard
**Files Reviewed:** 4
**Status:** issues_found

## Summary

Reviewed four files from Phase 22 (analysis-quick-wins): the production `InstrumentDatabase` and `InstrumentConfig` classes plus two test classes. The `.cache/eyelib_instrument` path migration is correctly implemented and consistent across all files.

However, the review uncovered **two critical thread-safety defects** in `InstrumentDatabase` that affect correctness under concurrent access — a scenario confirmed by the `BackgroundFlushService` which calls `getConnection()` from a background daemon thread every second. The design also has a subtle resource-management conflict where `BackgroundFlushService` closes the singleton's shared connection via try-with-resources on every flush cycle. Three warnings cover exception-safety ordering, atomicity gaps, and test-cleanup fragility.

---

## Critical Issues

### CR-01: Race condition in `InstrumentDatabase.getConnection()` — unsafe publication of `connection` field

**File:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java:22,42-56`

**Issue:**
The `connection` field (line 22) is read and written without any synchronization or `volatile` modifier. The `getConnection()` method (lines 42–56) implements a **check-then-act** pattern that is not atomic:

```java
// line 43-44: read
Connection existing = connection;
if (existing == null || existing.isClosed()) {
    // ... create new connection ...
    // line 51: write
    connection = existing;
}
```

Under concurrent access (proven real: `BackgroundFlushService.flush()` at `BackgroundFlushService.java:110` calls this from daemon thread `eyelib-instrument-flush`), two threads can **both** pass the null/closed check and create separate connections. The second write overwrites the first, **leaking** the first connection permanently. Additionally, a thread that has already read `existing` may later find that connection closed by another thread, causing `SQLException` at point of use.

In Java, without `volatile` or `synchronized`, writes to `connection` by one thread are not guaranteed to be visible to another. Combined with the non-atomic check-then-act, this is a classic double-checked locking defect.

**Fix:**
Synchronize `getConnection()` and `close()` on the same lock, or make the `connection` field `volatile` and use a synchronized block for the create path:

```java
private volatile @Nullable Connection connection;

public synchronized Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        try {
            Files.createDirectories(DB_BASE_PATH.getParent());
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Failed to create instrumentation cache directory", e);
        }
        connection = DriverManager.getConnection(DB_URL);
        LOG.info("H2 database connection opened: " + DB_URL);
        ensureSchema();
    }
    return connection;
}

public synchronized void close() {
    if (connection != null) {
        try {
            if (!connection.isClosed()) {
                connection.close();
                LOG.info("H2 database connection closed");
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error closing database connection", e);
        } finally {
            connection = null;
        }
    }
}
```

Alternatively, use `ReentrantReadWriteLock` to allow concurrent reads while serializing writes.

---

### CR-02: `BackgroundFlushService.flush()` closes the singleton's shared connection every cycle

**Files:**
- `src/main/java/io/github/tt432/eyelib/client/instrument/db/BackgroundFlushService.java:110`
- `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java:42-56`

**Issue:**
`BackgroundFlushService.flush()` uses try-with-resources on the connection borrowed from `InstrumentDatabase`:

```java
try (Connection conn = database.getConnection()) {   // line 110
    // ... batch insert ...
}  // conn.close() called here!
```

`java.sql.Connection` implements `AutoCloseable`, so at the end of the block `conn.close()` is invoked. But `conn` is the same object stored in `InstrumentDatabase.connection`. This means **every flush cycle (every 1 second) closes the shared connection**. While `getConnection()` detects the closed state and creates a new one, this is wasteful and introduces a window where the connection is closed but the field still points to it — any other thread that read the field before the close will receive a dead connection.

The `close()` call should be owned exclusively by `InstrumentDatabase.close()`, not by every caller.

**Fix:**
Remove the try-with-resources from `BackgroundFlushService.flush()`. The connection lifecycle should be managed by `InstrumentDatabase` alone:

```java
// In BackgroundFlushService.flush(), replace:
try (Connection conn = database.getConnection()) {

// With:
Connection conn = database.getConnection();
try {
```

Add a `finally` block to restore auto-commit but do NOT close:

```java
Connection conn = database.getConnection();
try {
    conn.setAutoCommit(false);
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        // ... batch insert ...
        conn.commit();
    } catch (SQLException ex) {
        try { conn.rollback(); } catch (final SQLException ignored) {}
        throw ex;
    }
} catch (final SQLException ex) {
    LOG.log(Level.WARNING, "Failed to flush " + batch.size() + " events", ex);
} finally {
    try { conn.setAutoCommit(true); } catch (final SQLException ignored) {}
}
```

This pairs with CR-01: once `getConnection()` is synchronized, the connection will be safely shared.

---

## Warnings

### WR-01: Connection assigned to field before `ensureSchema()` succeeds

**File:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java:50-53`

**Issue:**
The connection is stored in the field (`connection = existing;` at line 51) **before** `ensureSchema()` is called (line 53). If `ensureSchema()` throws (e.g., schema DDL fails), the field holds a connection whose schema may be incomplete or inconsistent. Subsequent callers of `getConnection()` will bypass the null/closed check and receive this bad connection.

**Fix:**
Reorder so that `ensureSchema()` succeeds before the field assignment:

```java
existing = DriverManager.getConnection(DB_URL);
ensureSchema();              // validate first
connection = existing;       // publish only after success
LOG.info("H2 database connection opened: " + DB_URL);
```

Also, remove the redundant `ensureSchema()` call at line 59 inside `ensureSchema()` itself (which calls `getConnection()`) — this is a recursive call that could deadlock once synchronization is added.

---

### WR-02: `InstrumentDatabase.close()` races with `getConnection()` on `isClosed()` check

**File:** `src/main/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabase.java:78-89`

**Issue:**
The `close()` method checks `connection.isClosed()` (line 81) and then calls `connection.close()` (line 82). Between these two calls, another thread could call `getConnection()`, see the connection is "open," and return it — only for it to be closed a moment later. This is a time-of-check-to-time-of-use (TOCTOU) race.

**Fix:**
Synchronizing both `getConnection()` and `close()` (as recommended in CR-01) eliminates this race entirely.

---

### WR-03: `InstrumentDatabaseTest.testInsert10kEventsAndQuery()` finally block can leak auto-commit state

**File:** `src/test/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabaseTest.java:133-141`

**Issue:**
The `finally` block has a structure where `conn.setAutoCommit(originalAutoCommit)` (line 140) is **after** the try-with-resources cleanup statement (lines 134-136). If the cleanup DELETE fails (e.g., table missing), the exception propagates from the finally block and **skips** `conn.setAutoCommit(originalAutoCommit)`. Since `InstrumentDatabase` is a singleton, the connection is shared across tests — leaving auto-commit `false` can break subsequent tests that expect auto-commit behavior.

**Fix:**
Wrap the auto-commit restore in its own `finally` or use a nested try-finally:

```java
} finally {
    try {
        try (Statement cleanup = conn.createStatement()) {
            cleanup.executeUpdate("DELETE FROM performance_events WHERE event_type = 'PERF_TEST'");
        }
    } finally {
        if (!conn.getAutoCommit()) {
            conn.commit();
        }
        conn.setAutoCommit(originalAutoCommit);
    }
}
```

---

## Info

### IN-01: `testConcurrentAccessSafety` does not verify thread safety of `connection` field

**File:** `src/test/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabaseTest.java:145-178`

**Issue:**
The test launches 4 threads that all call `getConnection()` and run `SELECT 1`, but it never asserts that all threads received the **same** Connection object, nor does it detect if multiple connections were created (i.e., connection leaks). It only verifies queries work. This test would pass even under the race conditions described in CR-01 and CR-02, providing a false sense of safety.

**Suggestion:** After the concurrent phase, call `getConnection()` once more and verify it's not closed. Optionally track the returned connection objects in a `Set` and assert `size() == 1`.

---

### IN-02: `testCorruptionRecovery` misleading name — tests deletion, not corruption

**File:** `src/test/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabaseTest.java:181-200`

**Issue:**
The test deletes the `.mv.db` and `.trace.db` files (lines 186-187) and verifies the database is recreated. This tests **recovery from file deletion**, not corruption (where files exist but contain invalid data). The name is misleading.

**Suggestion:** Rename to `testRecoveryAfterFileDeletion` or add a second test that writes garbage bytes to the file and verifies H2's built-in corruption detection.

---

### IN-03: `ensureSchema()` called redundantly in `testSchemaCreation`

**File:** `src/test/java/io/github/tt432/eyelib/client/instrument/db/InstrumentDatabaseTest.java:41-63`

**Issue:**
Line 42 calls `getConnection()`, which internally calls `ensureSchema()` (InstrumentDatabase.java:53). Line 43 then calls `ensureSchema()` again explicitly. This is redundant and masks the fact that `getConnection()` already ensures the schema.

**Suggestion:** Remove the explicit `ensureSchema()` call at line 43; the test already verifies schema correctness through metadata inspection.

---

_Reviewed: 2026-05-11_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
