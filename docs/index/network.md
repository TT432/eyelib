# Network And Sync Index

## Scope
- Root path: `src/main/java/io/github/tt432/eyelib/network/`
- Cross-cutting sync behavior also touches `src/main/java/io/github/tt432/eyelib/util/data_attach/` and `src/main/java/io/github/tt432/eyelib/capability/`.

## Start Reading Here
1. `docs/architecture/02-side-boundaries.md`
2. `src/main/java/io/github/tt432/eyelib/network/README.md`
3. `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md`
4. `src/main/java/io/github/tt432/eyelib/network/dataattach/README.md`

## Hotspots
- `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/data_attach/DataAttachmentHelper.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java`
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/dataattach/DataAttachmentSyncRuntime.java`

## Read Only If Needed
- Do not enter client rendering/tooling packages when the task is only about packet routing or attachment sync.
