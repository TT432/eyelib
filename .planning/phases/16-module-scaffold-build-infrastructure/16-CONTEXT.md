# Phase 16: Module Scaffold & Build Infrastructure - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase creates the `:eyelib-util` Forge Gradle module skeleton and proves it can build by itself before utility code migration begins. It must add the build metadata, unique `eyelibutil` mod identity, package namespace documentation, and dependency-direction rules needed for later phases, without migrating existing utility implementations yet.

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
- All implementation choices are at the agent's discretion because this is a pure infrastructure/scaffolding phase.
- Preserve v1.3 locked decisions: module namespace is `io.github.tt432.eyelibutil`; `:eyelib-util` is a leaf project module with no `project(...)` dependencies; MC/Forge dependencies are allowed; no migrated utility code should be moved into the module before Phase 17+.
- Use existing sibling module build patterns from `:eyelib-material`, `:eyelib-particle`, and `:eyelib-processor` where applicable, but keep `:eyelib-util` dependency direction independent from other project modules.
- All Gradle verification must run via JetBrains MCP only, never shell Gradle.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- Existing subproject skeletons (`eyelib-material`, `eyelib-particle`, `eyelib-processor`, `eyelib-attachment`) provide build.gradle, mods.toml, package README, and source-set conventions.
- Phase 15 produced `docs/architecture/migration/utility-routing-manifest.md`, which defines what will migrate later and should not be implemented in this phase.
- `settings.gradle` is the central include list for Gradle subprojects.

### Established Patterns
- Subprojects use distinct root packages (`io.github.tt432.eyelibmaterial`, `io.github.tt432.eyelibparticle`, etc.) to avoid split packages with root.
- Forge module metadata lives under `src/main/resources/META-INF/mods.toml` for Forge functional modules.
- Significant package trees and modules document ownership and dependency direction through README files and `MODULES.md`.

### Integration Points
- `settings.gradle` must include `eyelib-util`.
- `eyelib-util/build.gradle` must declare only allowed MC/Forge/external dependencies and zero `project(...)` dependencies.
- `eyelib-util/src/main/resources/META-INF/mods.toml` must use modId `eyelibutil` without colliding with existing module ids.
- `eyelib-util/README.md` and module inventory/docs must document leaf dependency direction and allowed integration layers.

</code_context>

<specifics>
## Specific Ideas

No user-facing preferences are required for this infrastructure phase. Follow ROADMAP success criteria, Phase 15 routing constraints, and existing subproject conventions.

</specifics>

<deferred>
## Deferred Ideas

- Moving root/core utility implementations into `:eyelib-util` is deferred to Phases 17-19.
- Submodule shared code centralization is deferred to Phase 20.
- Final root/core util cleanup is deferred to Phase 21.

</deferred>
