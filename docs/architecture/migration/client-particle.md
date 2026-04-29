# client-particle

## Scope
- Particle definitions, emitters, render manager, spawn/remove services.
- Main paths: `client/particle/`

## Why it is MC-facing
- Depends on client level, render-stage events, render types, and world runtime.

## Final isolation status
- Hard-import slice status: advanced (spawn/request seam tightened).
- Final `mc/api + mc/impl` isolation status: still pending.
- Expected final state for this module: particle runtime execution, emitter/render hooks, level access, and render-type integration stay in `mc/impl`; only stable spawn contracts or pure request/state helpers may remain outside it.

## Target seam
- Keep most runtime/render execution in `mc/impl`.
- Extract only stable definition or controller-facing ports if they genuinely reduce coupling.

## Deliverables
- Confirm what should remain fully in `mc/impl`.
- Add tests only for extracted pure helpers/definitions.
- Implement minimal seam; avoid fake abstractions.

## Dependencies
- After `client-render` and `network-sync`.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [ ] Re-baseline remaining Minecraft/Forge references for this module.
- [ ] Confine runtime emitters, render hooks, level access, and render-type integration to allowed `mc/impl` packages.
- [ ] Keep spawn contracts and pure helper/state code free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- `BrParticleRenderManager` still directly owns `Minecraft` client submission, Forge client events, `RenderLevelStageEvent`, and `RenderTypeResolver`-driven render-type binding in the legacy `client/particle/bedrock` package; this runtime/render wiring remains implementation-side work that must end up in allowed `mc/impl` ownership.
- `ParticleSpawnService` still directly depends on `Minecraft`, player/level access, and attachment-backed render data lookup (`DataAttachmentHelper`, `EyelibAttachableData`, `RenderData`), so it cannot remain outside `mc/impl` in the final state.
- The current seam isolates packet-to-request mapping with a platform-type-free request contract: `ParticleSpawnRequest` now stores `particleId` as `String`, and Minecraft identifier adaptation is kept inside `ParticleSpawnService`.
- This module is therefore still downstream of `client-render`, `network-sync`, and `capability-dataattach`: the render payload seam is now narrower (`RenderModelSyncPayload` is string-keyed), but particle runtime still depends on heavier render owners (`RenderTypeResolver`, render runtime lookup) plus packet transport and attachment-backed scope access.

## Priority hotspot files for next slice
- Highest import-density particle runtime owners:
  - `client/particle/bedrock/BrParticleParticle.java`
  - `client/particle/bedrock/BrParticleRenderManager.java`
  - `client/particle/bedrock/component/ParticleComponentManager.java`
  - `client/particle/bedrock/BrParticleEmitter.java`
  - `client/particle/bedrock/component/particle/appearance/ParticleAppearanceBillboard.java`
- Contract follow-up once upstream render/network seams settle:
  - `client/particle/ParticleSpawnService.java`
  - `client/particle/ParticleSpawnRequest.java`

## Progress notes
- Minimal seam decision: keep `BrParticleRenderManager`, emitter/particle runtime, render hooks, level/camera access, and render-type binding fully MC-facing; do not introduce a new abstraction layer around runtime rendering.
- Tightened `client/particle/ParticleSpawnRequest` into a platform-type-free seam (`String` spawn id + `String` particle id + defensive-copied `Vector3f`) by removing direct `ResourceLocation` and packet type coupling.
- Updated `ParticleSpawnService` to adapt packet `ResourceLocation` to string at the runtime boundary (`packet.particleId().toString()`), then use string-keyed lookup while keeping emitter construction/spawn/remove logic in existing MC-facing services.
- Updated targeted plain-JVM seam tests in `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequestTest.java` to cover request-state copy behavior without Minecraft type imports.

## JetBrains MCP verification results
- Targeted test (`jetbrain_run_gradle_tasks`: `test --tests io.github.tt432.eyelib.client.particle.ParticleSpawnRequestTest`): **PASS** (`exitCode=0`).
- Build verification (`jetbrain_build_project`): **PASS** (`isSuccess=true`).
