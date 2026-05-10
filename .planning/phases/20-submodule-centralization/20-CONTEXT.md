# Phase 20 Context: Submodule Centralization

## Goal
- Centralize attachment StreamCodec helpers into `:eyelib-util`.
- Rewire attachment/root consumers to the canonical util streamcodec package.
- Delete material duplicate `DispatchedMapCodec` copies and use `eyelibutil.codec.DispatchedMapCodec`.

## Affected Modules
- `:eyelib-util` shared utility leaf module.
- `:eyelib-attachment` data attachment module.
- `:eyelib-material` material module.
- Root network/capability packet consumers that use stream codecs.
- Documentation modules describing module boundaries.

## Migration Scope
- Move from attachment to util:
  - `StreamCodec`
  - `StreamEncoder`
  - `StreamDecoder`
  - `EyelibStreamCodecs`
- Delete from material:
  - `eyelibmaterial.shared.util.DispatchedMapCodec`
  - `eyelibmaterial.util.codec.DispatchedMapCodec`
- Rewire material callers/tests to `io.github.tt432.eyelibutil.codec.DispatchedMapCodec`.

## Dependency Decisions
- Add `implementation project(':eyelib-util')` to `eyelib-attachment` and `eyelib-material`.
- Keep `:eyelib-util` leaf-only with zero `project(...)` dependencies.

## Verification Gates
- `:eyelib-attachment:build` via JetBrains MCP.
- `:eyelib-material:build` via JetBrains MCP.
- Full project rebuild via JetBrains MCP.
- Old attachment streamcodec and material duplicate imports return zero matches.
- `eyelib-util/build.gradle` still has zero `project(...)` dependencies.
