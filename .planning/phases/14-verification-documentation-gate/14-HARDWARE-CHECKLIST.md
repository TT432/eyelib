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
| Particle resource reload | `particles/*.json` definitions load and publish by `ParticleDefinition.identifier()` rather than source `ResourceLocation`. | ⬜ Not run | Fill during Plan 03 or manual session. |
| `/eyelib particle` spawn | Command suggestions, validation, position fallback, and success message remain compatible. | ⬜ Not run | Fill during Plan 03 or manual session. |
| Spawn packet visual result | String-keyed spawn path produces the expected emitter/render behavior through `ParticleSpawnService`. | ⬜ Not run | Fill during Plan 03 or manual session. |
| Remove packet visual result | String-keyed remove path removes the intended emitter without exposing render internals in handlers. | ⬜ Not run | Fill during Plan 03 or manual session. |
| Logout / cleanup | Module `ParticleRenderManager` cleanup leaves no stale emitters after client logout. | ⬜ Not run | Fill during Plan 03 or manual session. |

## ClientSmoke Applicability

Phase 14 may use automated ClientSmoke only where existing hooks provide meaningful particle-module confidence without building a new smoke framework.

| Item | Status | Notes |
|------|--------|-------|
| Existing particle-specific ClientSmoke hook | ⬜ To be determined in Plan 03 | If none exists, record `not applicable because no existing particle-specific hook exists`. |
| Existing material/client smoke hook relevant to particle render path | ⬜ To be determined in Plan 03 | Use only as supporting evidence, not as direct proof unless it exercises particle behavior. |
| New broad smoke framework work | Not in scope | Do not expand Phase 14 into feature work. |

## Windows Hardware Exit-Code Capture

Windows hardware exit-code capture is manual/deferred per D-15 and the project-level out-of-scope note. Do not make it a mandatory automated v1.2 gate.

| Capture | Status | Notes |
|---------|--------|-------|
| Windows hardware exit code | Manual/deferred | Capture manually if available; absence does not block automated JetBrains MCP gates. |
| Manual screenshot/log archive | ⬜ Optional | Link screenshots/log paths if collected. |

## Result Log

| Date/Time | Operator | Environment | Result | Evidence |
|-----------|----------|-------------|--------|----------|
| ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

## Non-Blocking Deferrals

| Deferral | Why Non-Blocking | Follow-Up |
|----------|------------------|-----------|
| PFUT-02 packet-contract relocation | Current v1.2 ownership intentionally keeps packet DTO/codecs under `mc/impl/network/packet`. | Future packet contract relocation decision. |
| PFUT-03 independent particle artifact publication | v1.2 proves module boundary inside this multi-project build; independent external publication is packaging strategy work. | Future publication milestone. |
| Unrelated root fixture cleanup | D-03/D-11 classify unrelated broad-suite fixture failures as residual unless they block particle-gate evidence. | Separate cleanup task if needed. |
| Manual visual proof | Manual/runtime evidence is separate from automated Gradle gates and may be unavailable in headless sessions. | Record observations when hardware/client session is available. |
| Windows hardware exit-code capture | Explicitly manual/deferred per D-15. | Manual hardware checklist evidence only. |
