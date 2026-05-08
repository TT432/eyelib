# Phase 6: Config Override Bridge & State Machine Fixes - Context

**Gathered:** 2026-05-08
**Status:** Ready for planning
**Mode:** Auto-generated (autonomous fallback)

<domain>
## Phase Boundary

Smoke测试通过system property自动启用并自动退出；状态机正确处理空测试集和exit code

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
All implementation choices are at the agent's discretion. Use ROADMAP phase goal, success criteria, and codebase conventions.

**Key constraints from ROADMAP:**
- OVRD-01: ClientSmokeConfig.isEnabled() — system property override with ForgeConfigSpec fallback
- OVRD-02: ClientSmokeConfig.shouldExitAfterSmoke() — system property override with ForgeConfigSpec fallback
- OVRD-03: systemProperty() injection in clientSmoke run config
- OVRD-04: JUnit XML report generation alongside JSON
- CORR-01: Empty test set must complete (SCAN → REPORT → EXIT, not hang at IDLE)
- CORR-02: Exit code propagation — halt(0) for pass, halt(1) for fail

</decisions>

<code_context>
## Existing Code Insights

Key files to examine:
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeConfig.java` — config reads
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java` — state transitions
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeState.java` — state enum
- `build.gradle` — where run config with systemProperty() should be added
- Phase 5 already added the unconditional localRuntime and gameDirectory
</code_context>

<specifics>
## Specific Ideas

- System properties: `clientsmoke.enabled` and `clientsmoke.autoExit`
- State machine gap: when no @ClientSmoke tests found and autoExit=true, go SCAN→REPORT→EXIT
- Exit code: aggregate test results, halt(0) or halt(1)
- JUnit XML format alongside existing JSON report

</specifics>

<deferred>
## Deferred Ideas

None — discuss phase skipped.

</deferred>
