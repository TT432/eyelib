# Utility Index

## Scope
- Root path: `src/main/java/io/github/tt432/eyelib/util/`
- Includes shared helpers, data-attachment helpers, codec/math utilities, and mixed client-side helpers.

## Start Reading Here
1. `src/main/java/io/github/tt432/eyelib/util/README.md`
2. `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md` for attachment flow
3. Relevant architecture doc before touching `util/client/`

## Hotspots
- `src/main/java/io/github/tt432/eyelib/util/data_attach/`

## Current Utility Ownership
- Shared utility code lives in `:eyelib-util` under `io.github.tt432.eyelibutil` (packages: `time`, `color`, `loader`, `math`, `search`, `collection`, `resource`, `texture`, `codec`, `streamcodec`).
- Platform-free render helpers (`PoseCopies`) live in `client/render/`.
- Minecraft-facing adapter helpers live in `mc/impl/util/`.
- Root `util/` is drained: no Java source remains.
- Root `core/util/` is drained: no Java source remains.

## Boundary Reminder
- `util/client/` is drained; do not add new code here.
- Only truly cross-cutting helpers should use `:eyelib-util`; domain-specific helpers belong with their functional owner.
