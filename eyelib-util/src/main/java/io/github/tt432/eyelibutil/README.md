# `io.github.tt432.eyelibutil`

## Scope
- Package root: `eyelib-util/src/main/java/io/github/tt432/eyelibutil/`
- Module path: `:eyelib-util`
- Forge mod id: `eyelibutil`
- This package is the leaf namespace for shared utility code moved by migration phases.

## Current Packages
- `bootstrap/` — Forge marker package for `EyelibUtilMod` and the `eyelibutil` mod identity.
- `time/` — migrated timer utilities (`SimpleTimer`, `FixedStepTimerState`).
- `color/` — migrated color channel encoding helpers (`ColorEncodings`).
- `loader/` — migrated native/shared library loading utility (`SharedLibraryLoader`).
- `math/` — migrated math helpers (`Curves`, `EyeMath`, `MathHelper`, `FastColorHelper`, `Shapes`).
- `search/` — migrated search helper interface/results pair (`Searchable`, `SearchResults`).
- `collection/` — migrated collection helpers (`Blackboard`, `Lists`, `Collectors`, `EntryStreams`, `ListAccessors`).
- `resource/` — migrated ResourceLocation construction helpers without root mod-id coupling (`ResourceLocations`).
- `texture/` — migrated deterministic texture path helpers (`TexturePaths`).
- `codec/` — migrated codec infrastructure (`ChinExtraCodecs`, `CodecHelper`, `DispatchedMapCodec`, `Eithers`, `EyelibCodec`, `KeyDispatchMapCodec`, `Tuple`, `TupleCodec`).
- `streamcodec/` — migrated FriendlyByteBuf/NBT stream codec helpers (`StreamCodec`, `StreamEncoder`, `StreamDecoder`, `EyelibStreamCodecs`).

## Boundary Rules
- Migrated utility packages must remain under `io.github.tt432.eyelibutil`.
- Do not create split packages against root `io.github.tt432.eyelib.util`.
- Do not import root or sibling project module packages from this leaf module.
- Keep `project(...)` dependencies out of `eyelib-util/build.gradle` unless a later approved phase changes the boundary contract.
