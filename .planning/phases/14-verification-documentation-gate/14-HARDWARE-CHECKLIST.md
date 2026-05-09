# Phase 14 Hardware And Manual Evidence Checklist

## Scope

This checklist captures manual/runtime evidence that cannot be proven by the automated JetBrains MCP Gradle gates. It is evidence for PVERIFY-02 only and must not be treated as a replacement for `:eyelib-particle:test`, targeted root `:test` filters, or compile checks.

- Automated gates: recorded later in `14-FINAL-GATE-EVIDENCE.md` and Plan 03 matrix rows.
- Manual evidence: real Minecraft particle spawn/remove/render observations, screenshots, notes, and operator sign-off.
- Non-blocking scope boundaries: PFUT-02 packet-contract relocation, PFUT-03 independent particle artifact publication, unrelated fixture cleanup, and manual visual proof when no automated ClientSmoke hook applies.

## Manual Visual Checks

Record observations from a real client session or existing smoke flow when available.

| Check | Expected Behavior | Status | Evidence / Notes |
|-------|-------------------|--------|------------------|
| Particle resource reload | `particles/*.json` definitions load and publish by `ParticleDefinition.identifier()` rather than source `ResourceLocation`. | Manual/deferred | Automated evidence exists in `14-MCP-VERIFICATION-MATRIX.md` rows 1-3 for loader/publication tests; real client resource reload observation was not run in this automation-only session. |
| `/eyelib particle` spawn | Command suggestions, validation, position fallback, and success message remain compatible. | Manual/deferred | Automated evidence exists in row 3 for command/runtime/boundary tests; real in-game command use was not run in this automation-only session. |
| Spawn packet visual result | String-keyed spawn path produces the expected emitter/render behavior through `ParticleSpawnService`. | Manual/deferred | Automated packet/delegation tests passed in row 3; visual render proof remains hardware/manual evidence. |
| Remove packet visual result | String-keyed remove path removes the intended emitter without exposing render internals in handlers. | Manual/deferred | Automated remove packet and network delegation tests passed in row 3; visual remove behavior remains hardware/manual evidence. |
| Logout / cleanup | Module `ParticleRenderManager` cleanup leaves no stale emitters after client logout. | Manual/deferred | Module lifecycle/client integration tests passed in rows 1-2; real logout observation remains hardware/manual evidence. |

## ClientSmoke Applicability

Phase 14 may use automated ClientSmoke only where existing hooks provide meaningful particle-module confidence without building a new smoke framework.

| Item | Status | Notes |
|------|--------|-------|
| Existing particle-specific ClientSmoke hook | not applicable because no existing particle-specific hook exists | Repository search found ClientSmoke framework code and `eyelib-material` material smoke coverage, but no particle-specific `@ClientSmoke` hook. Adding one would be new smoke framework/feature work, outside Plan 03. |
| Existing material/client smoke hook relevant to particle render path | supporting-only, not direct proof | `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/smoke/MaterialPipelineSmoke.java` exercises material/client smoke plumbing but does not directly prove particle spawn/remove/render behavior. |
| New broad smoke framework work | Not in scope | Do not expand Phase 14 into feature work. |

## Windows Hardware Exit-Code Capture

Windows hardware exit-code capture is manual/deferred per D-15 and the project-level out-of-scope note. Do not make it a mandatory automated v1.2 gate.

| Capture | Status | Notes |
|---------|--------|-------|
| Windows hardware exit code | Manual/deferred | Not captured in Plan 03; absence does not block automated JetBrains MCP rows 1-3. |
| Manual screenshot/log archive | Optional / not collected | No screenshot or hardware log archive was produced in this automation-only session. |

## Result Log

| Date/Time | Operator | Environment | Result | Evidence |
|-----------|----------|-------------|--------|----------|
| 2026-05-09 | gsd-executor | JetBrains MCP Gradle automation on Windows; no live Minecraft client hardware session | Automated matrix passed rows 1-3; ClientSmoke direct particle hook not applicable; manual visual/hardware evidence deferred | `14-MCP-VERIFICATION-MATRIX.md`, `14-FINAL-GATE-EVIDENCE.md`, `14-MILESTONE-CLOSURE.md` |

## Non-Blocking Deferrals

| Deferral | Why Non-Blocking | Follow-Up |
|----------|------------------|-----------|
| PFUT-02 packet-contract relocation | Current v1.2 ownership intentionally keeps packet DTO/codecs under `mc/impl/network/packet`. | Future packet contract relocation decision. |
| PFUT-03 independent particle artifact publication | v1.2 proves module boundary inside this multi-project build; independent external publication is packaging strategy work. | Future publication milestone. |
| Unrelated root fixture cleanup | D-03/D-11 classify unrelated broad-suite fixture failures as residual unless they block particle-gate evidence. | Separate cleanup task if needed. |
| Manual visual proof | Manual/runtime evidence is separate from automated Gradle gates and may be unavailable in headless sessions. | Record observations when hardware/client session is available. |
| Windows hardware exit-code capture | Explicitly manual/deferred per D-15. | Manual hardware checklist evidence only. |
