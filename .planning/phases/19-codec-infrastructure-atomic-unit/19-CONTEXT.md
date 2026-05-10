# Phase 19 Context: Codec Infrastructure Atomic Unit

## Goal
- Move root codec infrastructure and `ImmutableFloatTreeMap` into `:eyelib-util` as one atomic unit.
- Move canonical `Eithers` into `:eyelib-util` and delete the legacy `EitherHelper` shim.
- Keep material duplicate `DispatchedMapCodec` copies and attachment StreamCodec centralization deferred to Phase 20.

## Affected Modules
- `:eyelib-util` shared utility leaf module.
- Root animation and behavior callers that import codec helpers or `ImmutableFloatTreeMap`.
- Root/core util package cleanup.
- Documentation modules that describe active `:eyelib-util` package ownership.

## Migration Unit
- Root codec files:
  - `ChinExtraCodecs`
  - `CodecHelper`
  - `DispatchedMapCodec`
  - `EitherHelper` (delete)
  - `EyelibCodec`
  - `KeyDispatchMapCodec`
  - `Tuple`
  - `TupleCodec`
  - `package-info.java`
- Root non-codec companion:
  - `ImmutableFloatTreeMap`
- Core codec canonical helper:
  - `Eithers`

## Decisions
- Use `io.github.tt432.eyelibutil.codec` as the canonical codec package.
- Place `ImmutableFloatTreeMap` in `io.github.tt432.eyelibutil.collection` because it is a collection-like indexed map that depends on codec helpers.
- Delete `EitherHelper` during this phase and update internal codec code to use `Eithers.unwrap` directly.
- Do not touch `eyelib-material` duplicate `DispatchedMapCodec` files in this phase; Phase 20 owns submodule dedupe.

## Verification Gates
- Old root/core codec imports return zero Java matches.
- `EitherHelper` returns zero Java matches.
- `:eyelib-util:build` via JetBrains MCP passes.
- Full project rebuild via JetBrains MCP passes.
- `eyelib-util/build.gradle` remains free of `project(...)` dependencies.
