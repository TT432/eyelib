# Eyelib Internal Boundary Notes

## Purpose
- This marker documents the repository-wide default: implementation packages are internal unless a narrower facade or package-level contract says otherwise.

## Applies To
- `client/`
- `network/`
- `molang/` compiler/runtime details
- `util/` implementation helpers
- tooling and generated-code zones

## Rule
- Do not treat an internal package as public API just because it is reachable from current code.
