---
phase: 20-submodule-centralization
plan: 02
status: complete
requirements-completed: [CENT-02]
completed: 2026-05-10
---

# 20-02 Summary: Material DispatchedMapCodec Deduplicated

## Status
- Complete.

## Changes
- Added `implementation project(':eyelib-util')` to `eyelib-material`.
- Rewired material main/test code to `io.github.tt432.eyelibutil.codec.DispatchedMapCodec`.
- Deleted both material duplicate `DispatchedMapCodec` implementations and drained package metadata.

## Evidence
- Search for material duplicate `DispatchedMapCodec` imports: zero Java matches.
- Material duplicate file glob returned no files.
