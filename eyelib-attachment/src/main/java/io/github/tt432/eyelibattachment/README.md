# Eyelib Attachment Module

## Scope
- Path: `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`
- Owns typed attachment storage contracts and attachment packet contracts.
- This is a Minecraft/Forge functional module, not a platform-free core library.
- Shared `FriendlyByteBuf` and NBT stream codec helpers now come from `:eyelib-util`.

## Layout
- `dataattach/`: attachment type, storage, and container contracts.
- `network/`: attachment-owned packet contracts that do not depend on root capability registries or runtime data types.
- `bootstrap/`: Forge module bootstrap marker.

## Ownership Rule
- Keep attachment storage and attachment protocol helpers here when they are attachment-specific.
- Keep plain attachment storage/data contracts free of root runtime package dependencies; direct
  Minecraft/Forge types should remain in `network/` or bootstrap wiring unless a
  follow-up explicitly documents a broader attachment-runtime owner.
- Runtime observers that inspect entity behavior, render state, or user input should stay with their owning feature until they can be moved intentionally.
