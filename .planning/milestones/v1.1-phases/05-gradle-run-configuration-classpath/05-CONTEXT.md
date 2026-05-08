# Phase 5: Gradle Run Configuration & Classpath - Context

**Gathered:** 2026-05-08
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped — autonomous fallback)

<domain>
## Phase Boundary

`./gradlew runClientSmoke` 任务存在且可启动 Minecraft，smoke mod 始终在 classpath 上

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
All implementation choices are at the agent's discretion — discuss phase was skipped per autonomous fallback mode. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

**Key constraints from ROADMAP:**
- GRAD-01, GRAD-02, GRAD-03, GRAD-04 requirements
- Smoke mod must always be on runClientSmoke classpath (no Gradle property)
- gameDirectory must be isolated: `run/clientsmoke/` vs `run/`
- IntelliJ run configuration should be auto-generated
- Output dirs must be gitignored

</decisions>

<code_context>
## Existing Code Insights

Codebase context will be gathered during plan-phase research. Key areas to explore:
- Root `build.gradle` — where the `runClientSmoke` task will be declared
- `eyelib-clientsmoke` mod's `build.gradle` — current configuration
- Existing `runClient` task and run config declarations in the Gradle build

</code_context>

<specifics>
## Specific Ideas

- Unconditional `localRuntime` dependency on `eyelib-clientsmoke` for the `runClientSmoke` task
- Dedicated `gameDirectory` set to `run/clientsmoke/`
- `.gitignore` entries for `run/clientsmoke/` and `clientsmoke-reports/`

</specifics>

<deferred>
## Deferred Ideas

None — discuss phase skipped.

</deferred>
