# Eyelib Architecture Blueprint

## Goal
- Reorganize module communication so Eyelib keeps its working core patterns while reducing direct cross-module reach-through.
- Standardize communication into explicit lanes: write, read, sync, and notification.

## Communication Lanes
- **Write lane**: loaders and tooling parse domain data, then publish through registry or domain service.
- **Read lane**: runtime modules query narrow lookup facades instead of pulling managers through `Eyelib.java`.
- **Sync lane**: packet handlers route to domain apply services; packet decoding stays in `network/`.
- **Notification lane**: `MinecraftForge.EVENT_BUS` stays for coarse invalidation and lifecycle changes only.

## Target Roles
- `bootstrap`: composition root only.
- `client/loader`: parse-only resource loading pipeline.
- `client/registry`: publication boundary from parsed data into runtime stores.
- `client/manager`: observable runtime stores.
- `client/* runtime`: lookup- and service-driven readers.
- `network/*`: routing and packet registration only.
- `network/dataattach`: sync service seam between packets and local attachment state.

## Execution Priorities
1. Normalize all asset publication through `client/registry`.
2. Move packet application logic into domain services and keep `NetClientHandlers` shallow.
3. Introduce lookup facades for core runtime reads to reduce `Eyelib.getXManager()` reach-through.
4. Keep `EyelibManagerScreen` as a view/composition class while IO/reload/publication stay in helpers.

## Rules
- New cross-module writes should not go directly from UI or loader code into manager singletons.
- New cross-module reads should prefer lookup facades inside the target domain.
- New packet handlers should route immediately into a domain apply service.
- If a module adds a new communication seam, update `MODULES.md` and the relevant package README in the same change.
- During the current breaking refactor, once a legacy read/write path is replaced, delete the old path in the same stage instead of keeping it as a compatibility shim.
