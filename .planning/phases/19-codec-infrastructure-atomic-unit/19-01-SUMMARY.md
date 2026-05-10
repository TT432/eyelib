---
phase: 19-codec-infrastructure-atomic-unit
plan: 01
status: complete
requirements-completed: [MIGR-04]
completed: 2026-05-10
---

# 19-01 Summary: Codec Sources Moved

## Status
- Complete.

## Changes
- Moved codec infrastructure into `eyelib-util/src/main/java/io/github/tt432/eyelibutil/codec/` using IDE semantic move:
  - `ChinExtraCodecs`
  - `CodecHelper`
  - `DispatchedMapCodec`
  - `EyelibCodec`
  - `KeyDispatchMapCodec`
  - `Tuple`
  - `TupleCodec`
  - `Eithers`
- Replaced `EitherHelper` usage with canonical `Eithers.unwrap`.
- Deleted old `EitherHelper` and root codec package metadata.

## Evidence
- Old root/core codec import search returned zero Java matches.
- `EitherHelper` search returned zero Java matches.
