# MC Impl Network Data-Attach Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/`
- Owns attachment packet apply/sync behavior that requires Minecraft runtime/entity/container access.

## Key Files
- `DataAttachmentSyncRuntime.java`: handles tracked sync sends, client apply by entity lookup, full-container NBT sync apply, and destroy-info server updates.

## Boundary Reminder
- Keep runtime-side attachment apply/sync transport logic in this package.
- Keep `network/dataattach/DataAttachmentSyncPayloadOps.java` limited to platform-type-free payload/state mapping helpers.
