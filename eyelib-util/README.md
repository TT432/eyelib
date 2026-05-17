# Eyelib Util Module

## Scope
- Gradle path: `:eyelib-util`
- Forge mod id: `eyelibutil`
- Root package namespace: `io.github.tt432.eyelibutil`
- Current active migrated packages are `time`, `color`, `loader`, `math`, `search`, `collection`, `resource`, `texture`, `codec`, and `streamcodec` under `io.github.tt432.eyelibutil`.

## Ownership
- Shared cross-cutting utility code may live here when that code has module-level utility ownership.
- Single-consumer helper code belongs with its functional owner instead of this module.
- New utility packages must stay under `io.github.tt432.eyelibutil` and must not create split packages with root `io.github.tt432.eyelib.util`.

## Dependency Direction
- `:eyelib-util` is a leaf project module and must contain zero `project(...)` dependencies.
- Root and sibling project modules must not be imported or depended on from this module.
- Root consumes `:eyelib-util` through explicit Gradle dependency edges; `:eyelib-attachment` and `:eyelib-material` also consume it through explicit dependency edges.

## Allowed Integration Layers
- Minecraft and Forge APIs are allowed when a utility contract is intentionally platform-facing.
- External libraries already declared in `eyelib-util/build.gradle` are allowed integration inputs.
- Project-internal dependencies on `:`, `:eyelib-attachment`, `:eyelib-importer`, `:eyelib-material`, `:eyelib-molang`, `:eyelib-particle`, or `:eyelib-preprocessing` are rejected.

## Verification Rule
- Gradle verification for this repository must be executed through JetBrains MCP Gradle tools only, never through shell Gradle commands.
