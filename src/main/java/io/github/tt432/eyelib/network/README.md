# Network Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/network/`
- Packet registration, packet types, and side-aware client/server routing.

## Start Reading Here
1. `docs/index/network.md`
2. `docs/architecture/02-side-boundaries.md`
3. `EyelibNetworkManager.java`

## Key Files
- `EyelibNetworkManager.java`: packet registration and side-aware dispatch
- `NetClientHandlers.java`: client application path
- `UniDataUpdatePacket.java`, `DataAttachmentUpdatePacket.java`, `DataAttachmentSyncPacket.java`: sync-related packet types

## Boundary Reminder
- Packet routing belongs here; data-attachment state ownership is shared with `../util/data_attach/` and should be read together when sync work is involved.
