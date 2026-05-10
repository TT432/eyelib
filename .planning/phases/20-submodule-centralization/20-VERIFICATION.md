# Phase 20 Verification

## Status
- passed

## Success Criteria
1. StreamCodec suite exists in `:eyelib-util` and no longer exists in `eyelib-attachment`: passed.
2. Material duplicate `DispatchedMapCodec` implementations are deleted and callers use util canonical codec: passed.
3. `eyelib-attachment` and `eyelib-material` depend on `:eyelib-util` and their builds pass: passed.
4. Full project rebuild completes through JetBrains MCP: passed.
5. `:eyelib-util` remains leaf-only with zero `project(...)` dependencies: passed.

## Evidence
- New streamcodec package: `eyelib-util/src/main/java/io/github/tt432/eyelibutil/streamcodec/`.
- Deleted attachment `codec/stream` Java sources.
- Deleted material duplicate `DispatchedMapCodec` Java sources.
- JetBrains MCP `:eyelib-attachment:build :eyelib-material:build`: exit code 0.
- JetBrains MCP full project rebuild: `isSuccess=true`, `problems=[]`.
