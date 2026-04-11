# Data Attachment Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/util/data_attach/`
- Platform-type-free typed data-attachment contracts and storage/mutation helpers.

## Start Reading Here
1. `docs/index/network.md`
2. `docs/architecture/02-side-boundaries.md`
3. `DataAttachmentStorage.java`

## Key Files
- `DataAttachmentStorage.java`: platform-free storage/mutation contract
- `DataAttachmentType.java`: attachment type + identifier/codec contract without Minecraft types
- `DataAttachmentMapStorage.java`, `DataAttachmentContainer.java`: map-backed storage implementation
- `../../mc/impl/data_attach/`: Minecraft/Forge capability, provider, event, and NBT wiring owners
- `../../network/dataattach/DataAttachmentSyncPayloadOps.java`: attachment payload/state mapping seam used by runtime sync owners
- `../../mc/impl/network/dataattach/`: Minecraft runtime apply/sync owner for attachment packets

## Boundary Reminder
- Attachment state ownership and portable mutation rules belong here.
- Minecraft/Forge capability wiring, entity event handling, and NBT serialization belong in `../../mc/impl/data_attach/`.
- Any refactor in this area must keep client rendering/tooling dependencies out.
