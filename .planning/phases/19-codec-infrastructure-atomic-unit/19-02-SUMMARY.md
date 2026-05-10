---
phase: 19-codec-infrastructure-atomic-unit
plan: 02
status: complete
requirements-completed: [MIGR-04, VERIFY-01]
completed: 2026-05-10
---

# 19-02 Summary: ImmutableFloatTreeMap Moved

## Status
- Complete.

## Changes
- Moved `ImmutableFloatTreeMap` to `io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap` using IDE semantic move.
- Rewired animation/channel callers and fully-qualified references.
- Deleted drained root/core util Java package metadata after source migration.

## Evidence
- `src/main/java/io/github/tt432/eyelib/util/**/*.java` returned no files.
- `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` returned no files.
- `eyelib-util/src/main/java` has zero imports from `io.github.tt432.eyelib.*`.
