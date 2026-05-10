# Phase 19 Verification

## Status
- passed

## Success Criteria
1. All codec infrastructure files and `ImmutableFloatTreeMap` exist in `:eyelib-util` and no longer exist under root/core util: passed.
2. Full project rebuild completes through JetBrains MCP: passed.
3. `EitherHelper.java` is deleted and callers use `Eithers`: passed.
4. Old `io.github.tt432.eyelib.util.codec.` imports return zero Java matches: passed.
5. `ImmutableFloatTreeMap` codec dependency resolves in `:eyelib-util`: passed by `:eyelib-util:build`.

## Evidence
- New canonical codec package: `eyelib-util/src/main/java/io/github/tt432/eyelibutil/codec/`.
- New `ImmutableFloatTreeMap` package: `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/ImmutableFloatTreeMap.java`.
- Root/core util Java glob checks returned no files.
- Old util/core util imports and `EitherHelper` search returned zero Java matches.
- JetBrains MCP `:eyelib-util:build`: exit code 0.
- JetBrains MCP full project rebuild: `isSuccess=true`, `problems=[]`.

## Deferred Scope
- `eyelib-material` duplicate `DispatchedMapCodec` files remain for Phase 20 submodule centralization.
