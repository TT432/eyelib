# Network Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/network/`
- Minimal network entrypoint and context-free handler delegation for sync routing.

## Start Reading Here
1. `docs/index/network.md`
2. `docs/architecture/02-side-boundaries.md`
3. `EyelibNetworkManager.java`

## Key Files
- `EyelibNetworkManager.java`: minimal network entrypoint delegating transport to `mc/impl/network/`
- `NetClientHandlers.java`: context-free client apply delegation
- Packet contract classes now live under `../mc/impl/network/packet/`

## Boundary Reminder
- Packet transport/context ownership lives under `../mc/impl/network/`; this package should stay transport-agnostic.
- Packet codec/DTO ownership now also lives under `../mc/impl/network/packet/`; do not grow new `FriendlyByteBuf` / NBT-backed packet contracts here.
- Data-attachment state ownership is shared with `../util/data_attach/` and should be read together when sync work is involved.
