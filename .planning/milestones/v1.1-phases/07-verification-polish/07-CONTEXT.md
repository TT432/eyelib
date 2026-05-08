# Phase 7: Verification & Polish - Context

**Gathered:** 2026-05-08
**Status:** Ready for planning
**Mode:** Auto-generated (autonomous fallback)

<domain>
## Phase Boundary

一键启动承诺在真实硬件上验证通过；正常开发流程零回归

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
All implementation choices are at the agent's discretion. Use ROADMAP phase goal, success criteria, and codebase conventions.

**Key constraints from ROADMAP:**
- CORR-03: `runClient` must behave identically to pre-v1.1 — smoke mod idle, no interference
- CORR-04: Windows exit code capture verification (exit 0 = pass, exit 1 = fail)

**Key context from prior phases:**
- Phase 5: `runClientSmoke` task declared with unconditional `localRuntime` for `eyelib-clientsmoke`, gameDirectory isolated
- Phase 6: `ClientSmokeConfig.isEnabled()` system-property-first, will be false for `runClient` (no sys props); state machine idle when disabled; exit code properly propagated

</decisions>

<code_context>
## Existing Code Insights

Key areas to verify:
- `runClient` task — must still work, smoke mod is on classpath but idle (isEnabled() returns false without system properties)
- `runClientSmoke` task — full pipeline with system property injection
- Exit code behavior — `halt(0)` vs `halt(1)`

This phase is primarily verification, not new feature work. Any code changes should be minimal bug fixes.
</code_context>

<specifics>
## Specific Ideas

- Verify `runClient` with `isEnabled()` returning false → state machine stays in INIT/IDLE
- Verify `runClientSmoke` task listing via Gradle dry-run
- Verify exit code handling: `halt(0)` for pass, `halt(1)` for fail
- Check no regression in existing Forge runtime (smoke mod on classpath but inactive)

</specifics>

<deferred>
## Deferred Ideas

Deferred manual tests (from v1.0):
- HUD-free screenshot visual verification
- JVM exit timing verification
- Windows Runtime.halt() exit code capture (target for this phase)

</deferred>
