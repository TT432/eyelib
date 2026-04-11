# MC Impl Data Attachment Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/`
- Minecraft/Forge-specific capability wiring, entity event handlers, and NBT persistence for data attachments.

## Key Files
- `DataAttachmentContainerCapability.java`: capability registration + attach/clone wiring
- `DataAttachmentContainerProvider.java`: Forge capability provider bridge
- `DataAttachmentEventHandlers.java`: event-driven full-container sync trigger
- `McDataAttachmentContainer.java`: NBT serialization/deserialization implementation owner
- `DataAttachmentHelper.java`: entity-bound access helper backed by Forge capability lookup

## Boundary Reminder
- Keep direct `net.minecraft.*` and `net.minecraftforge.*` references in this package.
- Keep `util/data_attach/` limited to platform-type-free contracts/storage semantics.
