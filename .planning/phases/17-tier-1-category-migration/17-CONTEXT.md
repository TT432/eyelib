# Phase 17: Tier-1 Category Migration - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase migrates the Tier-1 zero-dependency utility categories and collection utilities into `:eyelib-util`, rewires root consumers to `io.github.tt432.eyelibutil`, deletes the old root/core source copies, and removes the `ListHelper` shim after former callers compile against the canonical list accessor utility.

</domain>

<decisions>
## Decisions

- D-01: `SharedLibraryLoader` target package is `io.github.tt432.eyelibutil.loader`.
- D-02: Migrate FastUtil-dependent `Lists.java` fully; prove classpath with JetBrains MCP `:eyelib-util:build`, no design degradation.
- D-03: Delete `ListHelper` after callers are rewired to `io.github.tt432.eyelibutil.collection.ListAccessors`.

### the agent's Discretion
- All implementation choices not locked above are at the agent's discretion within the ROADMAP success criteria and Phase 15 routing manifest.
- Migrate only Phase 17-owned files: time, color, loader/misc, math, search, and collection helpers named in ROADMAP. Do not migrate resource/texture utilities, codec infrastructure, or submodule-centralized helpers before later phases.
- Preserve package namespace `io.github.tt432.eyelibutil` and keep `:eyelib-util` as a leaf module with no project-internal dependencies.
- Use IDE-aware moves/refactors where possible; validate all Gradle/build operations through JetBrains MCP only.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 15 manifest `docs/architecture/migration/utility-routing-manifest.md` records the source inventory, route decisions, and `ListHelper` deletion timing for Phase 17.
- Phase 16 created `:eyelib-util` with package root `io.github.tt432.eyelibutil`, module docs, mod id `eyelibutil`, and a passing solo build/test.
- Existing old sources live under `src/main/java/io/github/tt432/eyelib/util/` and `src/main/java/io/github/tt432/eyelib/core/util/`.

### Established Patterns
- Subprojects use their own root package and avoid split packages with root.
- Build verification must use JetBrains MCP; shell Gradle is forbidden.
- Utility extraction should be behavior-preserving: package/import rewiring, not algorithm rewrites.

### Integration Points
- Root build must consume `:eyelib-util` once root code imports migrated utilities.
- Root consumers of `SimpleTimer`, `FixedStepTimerState`, `ColorEncodings`, `SharedLibraryLoader`, math/search helpers, `Blackboard`, `Lists`, `Collectors`, `EntryStreams`, and `ListHelper` need import/path rewiring.
- `ListHelper` should be removed once former callers use `ListAccessors` or its `eyelibutil` equivalent.

</code_context>

<specifics>
## Specific Ideas

No user-facing preferences are required. Follow Phase 15 manifest, Phase 16 module constraints, and ROADMAP success criteria.

</specifics>

<deferred>
## Deferred Ideas

- Resource/texture migration and `ResourceLocations.mod()` handling are Phase 18-owned.
- Codec infrastructure and `EitherHelper` deletion are Phase 19-owned.
- Submodule shared-code centralization is Phase 20-owned.

</deferred>
