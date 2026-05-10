# Phase 8: Boundary Contract & Gradle Module Skeleton - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase establishes `:eyelib-particle` as a first-class Gradle module boundary with explicit ownership, documented one-way root-to-particle dependency direction, module build metadata, source/resource layout, and verification guidance that uses JetBrains MCP Gradle execution only.

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
All implementation choices are at the agent's discretion because this is a pure infrastructure phase. Preserve repository module-boundary rules, avoid root runtime dependencies from `:eyelib-particle`, and do not endorse shell Gradle commands.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MODULES.md` is the canonical module inventory and already documents particle/module ownership expectations.
- `docs/index/repo-map.md` and architecture docs provide navigation and boundary constraints for module split work.

### Established Patterns
- The repository uses multi-project Gradle modules with module-local README files and `META-INF/mods.toml` metadata.
- Gradle verification must be run through JetBrains MCP, not shell commands.

### Integration Points
- Root build settings and module documentation define the visible boundary for `:eyelib-particle`.
- Root runtime may depend on `:eyelib-particle`; the particle module must not depend back on root runtime packages or root `mc/impl` implementation classes.

</code_context>

<specifics>
## Specific Ideas

No specific requirements - infrastructure phase.

</specifics>

<deferred>
## Deferred Ideas

None.

</deferred>
