# Eyelib Util Module

## Scope
- Gradle path: `:eyelib-util`
- Forge mod id: `eyelibutil`
- Root package namespace: `io.github.tt432.eyelibutil`
- Phase 16 established the scaffold, build metadata, documentation boundary, and module identity.
- Phase 17 Plan 01 starts implementation migration with time, color, and native-loader utilities.
- Phase 17 Plan 02 adds math utilities under `io.github.tt432.eyelibutil.math`.
- Phase 17 Plan 03 adds search utilities under `io.github.tt432.eyelibutil.search`.
- Phase 17 Plan 05 adds collection utilities under `io.github.tt432.eyelibutil.collection`.
- Phase 18 adds resource-location helpers under `io.github.tt432.eyelibutil.resource` and texture-path helpers under `io.github.tt432.eyelibutil.texture`.
- Phase 19 adds codec infrastructure under `io.github.tt432.eyelibutil.codec` and moves `ImmutableFloatTreeMap` into `io.github.tt432.eyelibutil.collection`.
- Phase 20 adds FriendlyByteBuf/NBT stream codec helpers under `io.github.tt432.eyelibutil.streamcodec` for root and submodule packet contracts.

## Ownership
- Shared cross-cutting utility code may live here when that code has module-level utility ownership.
- Current active migrated packages are `time`, `color`, `loader`, `math`, `search`, `collection`, `resource`, `texture`, `codec`, and `streamcodec`.
- Single-consumer helper code belongs with its functional owner instead of this module.
- New utility packages added in later phases must stay under `io.github.tt432.eyelibutil` and must not create split packages with root `io.github.tt432.eyelib.util`.

## Dependency Direction
- `:eyelib-util` is a leaf project module and must contain zero `project(...)` dependencies.
- Root and sibling project modules must not be imported or depended on from this module.
- Root consumes `:eyelib-util` through explicit Gradle dependency edges added for Phase 17; `:eyelib-attachment` and `:eyelib-material` consume it through explicit Phase 20 dependency edges.

## Allowed Integration Layers
- Minecraft and Forge APIs are allowed when a utility contract is intentionally platform-facing.
- External libraries already declared in `eyelib-util/build.gradle` are allowed integration inputs.
- Project-internal dependencies on `:`, `:eyelib-attachment`, `:eyelib-importer`, `:eyelib-material`, `:eyelib-molang`, `:eyelib-particle`, or `:eyelib-processor` are rejected.

## Verification Rule
- Gradle verification for this repository must be executed through JetBrains MCP Gradle tools only, never through shell Gradle commands.
