# Network Data-Attach Sync Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/network/dataattach/`
- Pure payload/state sync seam for remaining root-coupled data-attachment packet mapping.

## Current Role
- `DataAttachmentSyncPayloadOps.java` owns transport-independent attachment update mapping and small state transitions while root update/extra packets still decode through root attachment registry/capability data.
- Runtime packet apply/sync behavior now lives in `../../mc/impl/network/dataattach/DataAttachmentSyncRuntime.java`.

## Boundary Reminder
- Packet registration and transport-side handling stay under `../../mc/impl/network/`.
- Root-independent attachment ownership stays in `:eyelib-attachment`; this seam remains only for root-coupled sync blockers until root registry/capability data move or expose attachment-owned lookup contracts.
- Cross-zone payload/state mapping should pass through this package.
