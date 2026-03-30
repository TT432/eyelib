# Network Data-Attach Sync Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/network/dataattach/`
- Synchronization boundary for data-attachment update packets and full-container sync flow.

## Current Role
- `DataAttachmentSyncService.java` centralizes packet send/apply logic so packet handlers and attachment helpers do not each embed their own sync wiring.
- This includes generic attachment updates and extra-entity attachment apply paths on the client side.

## Boundary Reminder
- Packet registration stays in `network/`.
- Attachment ownership stays in `util/data_attach/`.
- Cross-zone send/apply logic should pass through this service.
