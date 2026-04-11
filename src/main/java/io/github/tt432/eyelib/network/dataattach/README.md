# Network Data-Attach Sync Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/network/dataattach/`
- Pure payload/state sync seam for data-attachment packet mapping.

## Current Role
- `DataAttachmentSyncPayloadOps.java` owns transport-independent attachment update mapping and small state transitions.
- Runtime packet apply/sync behavior now lives in `../../mc/impl/network/dataattach/DataAttachmentSyncRuntime.java`.

## Boundary Reminder
- Packet registration and transport-side handling stay under `../../mc/impl/network/`.
- Attachment ownership stays in `util/data_attach/`.
- Cross-zone payload/state mapping should pass through this package.
