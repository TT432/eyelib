# Eyelib Side Boundaries

## Why Side Rules Matter Here
Eyelib is a Forge codebase with client-only rendering/tooling paths and shared runtime-sync paths. Package boundaries alone are not enough; side boundaries must stay explicit.

## Side Matrix
| Zone | May depend on | Must not depend on |
|---|---|---|
| common/shared | codecs, math, immutable data, pure helpers | client GUI, rendering, client-only runtime classes |
| client-only | render/model/animation/particle/tooling and client handlers | server-only execution paths |
| sync | packet codecs, side gates, narrow apply services | direct GUI code, loader implementation details |
| data attachment | typed attachment helpers and mutation flow | rendering or screen logic |

## Current Anchors
- `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java` already uses side gating around client handlers.
- `src/main/java/io/github/tt432/eyelib/util/data_attach/DataAttachmentHelper.java` now separates local mutation from tracked sync via `src/main/java/io/github/tt432/eyelib/network/dataattach/DataAttachmentSyncService.java`.
- `src/main/java/io/github/tt432/eyelib/client/` contains client-only rendering and tooling concerns and must stay out of common/shared zones.

## Rules For This Refactor
- Packet registration and routing belong in the sync layer, not in GUI/tooling or loader code.
- Client-only smoke/test flows may depend on `runClient`; common/shared code must not.
- Data-attachment mutation rules must be written down before packet/data-attachment restructuring starts.
- New cross-zone dependencies need a written reason in the relevant architecture doc before they are introduced.
- Client packet handlers should prefer dedicated client runtime services such as `client/particle/ParticleSpawnService.java` over direct loader access.
- Client packet handlers should also route render-state application through dedicated apply services such as `client/render/sync/ClientRenderSyncService.java`.
- Client packet handlers should not call `BrParticleRenderManager` directly; emitter spawn/remove stays inside `ParticleSpawnService.java`.

## Reading Guidance
- If a task involves packets or network handlers, read this file before editing `network/` or `util/data_attach/`.
- If a task involves rendering, GUI, models, or particles, stay inside client-local docs and do not assume server/shared applicability.
