# Core Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/core/`
- Historical platform-free helper seam drained by the v1.3 `:eyelib-util` migration.

## Boundary Rules
- Do not add new utility code here; shared helpers belong in `:eyelib-util` or a functional owner package.
- Minecraft-facing adapters belong in domain-specific runtime packages, not in this historical seam.

## v1.3 Outcome
- No Java source remains under `core/util/` after Phase 19.
- Former first-wave helpers now live under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/` in `collection`, `texture`, `color`, `codec`, and `time` packages.
