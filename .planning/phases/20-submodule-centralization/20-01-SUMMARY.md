---
phase: 20-submodule-centralization
plan: 01
status: complete
requirements-completed: [CENT-01]
completed: 2026-05-10
---

# 20-01 Summary: Attachment StreamCodec Suite Moved

## Status
- Complete.

## Changes
- Added `implementation project(':eyelib-util')` to `eyelib-attachment`.
- Moved `StreamCodec`, `StreamEncoder`, `StreamDecoder`, and `EyelibStreamCodecs` to `io.github.tt432.eyelibutil.streamcodec` using IDE semantic move.
- Rewired root and attachment imports to the util streamcodec package.
- Deleted drained attachment `codec/stream` package metadata.
- Updated attachment README and identity test for the new boundary.

## Evidence
- Search for `io.github.tt432.eyelibattachment.codec.stream`: zero Java matches.
