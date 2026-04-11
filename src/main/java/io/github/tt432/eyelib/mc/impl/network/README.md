# MC Impl Network Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/network/`
- Owns Forge/Minecraft packet channel transport, side-gating, and packet-context handling.

## Key Files
- `EyelibNetworkTransport.java`: `SimpleChannel` registration, packet dispatch, and send-to-player/tracked transport wiring.
- `dataattach/DataAttachmentSyncRuntime.java`: Minecraft runtime apply/sync behavior for attachment packets.

## Boundary Reminder
- Keep direct packet transport/context/runtime imports in this package.
- Keep `network/` focused on packet contracts and context-free handler delegation.
