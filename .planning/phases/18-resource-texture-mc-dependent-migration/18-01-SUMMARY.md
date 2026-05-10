---
phase: 18-resource-texture-mc-dependent-migration
plan: 01
status: complete
requirements-completed: [MIGR-03]
completed: 2026-05-10
---

# 18-01 Summary: ResourceLocations Migration

## Status
- Complete.

## Changes
- Added `eyelib-util/src/main/java/io/github/tt432/eyelibutil/resource/ResourceLocations.java`.
- Rewired all current callers to `io.github.tt432.eyelibutil.resource.ResourceLocations`.
- Deleted `src/main/java/io/github/tt432/eyelib/util/ResourceLocations.java`.
- Removed the unused `ResourceLocations.mod(String)` root mod-id coupling instead of preserving a compatibility method.

## Evidence
- Java source search for `io.github.tt432.eyelib.util.ResourceLocations` returned zero matches.
- Java source search for `ResourceLocations.mod` returned zero matches.
