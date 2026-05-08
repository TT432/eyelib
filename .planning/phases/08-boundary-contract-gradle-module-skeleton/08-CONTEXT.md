# Phase 8: Boundary Contract & Gradle Module Skeleton - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase establishes `:eyelib-particle` as a real Gradle module boundary with explicit build metadata, source/resource layout, root dependency wiring, and ownership documentation. It must prove the one-way dependency direction: root runtime may consume the particle module, but `:eyelib-particle` must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
All implementation choices are at the agent's discretion because this is a pure infrastructure phase. Use the ROADMAP goal, requirements PGRAD-01/PGRAD-02/PAPI-02, existing Gradle subproject conventions, and repository module-boundary documentation rules to guide decisions.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- Existing subprojects `:eyelib-attachment`, `:eyelib-importer`, `:eyelib-material`, `:eyelib-molang`, and `:eyelib-processor` provide the reference pattern for build script structure, source/resource layout, package naming, and module metadata.
- `settings.gradle`, root `build.gradle`, `gradle.properties`, `MODULES.md`, and docs under `docs/architecture/` are the main integration and documentation points for a new Gradle module.
- Current particle code starts in `src/main/java/io/github/tt432/eyelib/client/particle/`, while importer schema pressure already exists at `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`.

### Established Patterns
- Subprojects use Java 17, Groovy Gradle DSL, Forge/MDGL-compatible metadata, Lombok, JUnit 5, and package roots like `io.github.tt432.eyelibmolang`, `io.github.tt432.eyelibimporter`, and `io.github.tt432.eyelibattachment`.
- Significant package/module areas have README documentation and are listed in `MODULES.md` with responsibility, main paths, and interactions.
- Platform-free code must stay outside root `mc/impl`; direct Minecraft/Forge imports belong in explicit integration/adapters.

### Integration Points
- `settings.gradle` must include the new subproject.
- Root build wiring must allow root runtime to depend on `:eyelib-particle` without introducing a reverse dependency.
- Documentation must state particle ownership, allowed integration layers, and JetBrains MCP-only Gradle verification expectations.

</code_context>

<specifics>
## Specific Ideas

No specific requirements beyond ROADMAP and requirement traceability; prefer the smallest correct module skeleton that future phases can fill without broad compatibility shims.

</specifics>

<deferred>
## Deferred Ideas

Moving particle APIs, schema/runtime adapters, loader publication, command/network integration, and verification coverage is deferred to Phases 9-14.

</deferred>
