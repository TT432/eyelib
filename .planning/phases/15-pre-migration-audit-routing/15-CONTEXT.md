# Phase 15: Pre-Migration Audit & Routing - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase delivers the pre-migration audit and routing baseline for extracting `:eyelib-util`: every existing `root/util/*` and `core/util/*` source file must have a verified 0/1/N consumer count and a committed destination decision, wildcard imports to old util packages must be eliminated, single-consumer utility classes must move to their functional owner packages, and compatibility shims must be cataloged for later deletion.

</domain>

<decisions>
## Implementation Decisions

### the agent's Discretion
- All implementation choices are at the agent's discretion because this is a pure infrastructure/migration phase.
- Preserve the milestone's fixed decisions: `:eyelib-util` will use package namespace `io.github.tt432.eyelibutil`; MC/Forge-dependent utilities are allowed in `:eyelib-util`; single-consumer code moves to its functional owner instead of the util module.
- Keep Phase 15 limited to audit, explicit import cleanup, single-consumer relocations, and routing documentation. Do not scaffold `:eyelib-util` or migrate multi-consumer utility categories before Phase 16+.
- Use IDE-aware refactoring or verified references for package moves where possible; do not hand-edit generated Molang parser files.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `.planning/codebase/STRUCTURE.md` identifies `src/main/java/io/github/tt432/eyelib/util/` as transitional shared helpers with `client/`, `codec/`, `math/`, `modbridge/`, and `search/` subareas.
- `.planning/codebase/STRUCTURE.md` identifies `src/main/java/io/github/tt432/eyelib/core/util/` as platform-free utility seams with `codec/`, `collection/`, `color/`, `texture/`, and `time/` categories.
- `MODULES.md` already records Phase 15 routing targets: client-specific helpers should move under functional client owners, MC/Forge integration bridges under `mc/impl`, and central shared utilities eventually under `:eyelib-util`.

### Established Patterns
- The repository uses explicit Gradle subproject package roots such as `io.github.tt432.eyelibmolang`, `io.github.tt432.eyelibimporter`, and the planned `io.github.tt432.eyelibutil`; avoid split packages with root.
- Import conventions prefer explicit class-level imports; production wildcard imports are not expected.
- Significant package trees should carry local `README.md` guidance, and module responsibility changes must update `MODULES.md` plus relevant architecture docs when boundaries change.

### Integration Points
- `src/main/java/io/github/tt432/eyelib/util/client/AnimationApplier.java` should route to `client/animation`.
- `src/main/java/io/github/tt432/eyelib/util/client/Models.java` should route to `client/model`.
- `src/main/java/io/github/tt432/eyelib/util/modbridge/ModBridgeServer.java` and `BBModelSink.java` should route to `mc/impl/modbridge` per roadmap success criteria.
- `ResourceLocations.mod()` needs caller verification before Phase 18 decides delete vs parameterize.
- `ListHelper` and `EitherHelper` are compatibility shims to catalog with consumer counts and deletion timing.

</code_context>

<specifics>
## Specific Ideas

No user-facing preferences are required for this infrastructure phase. Follow ROADMAP success criteria, `MODULES.md` boundaries, and existing codebase conventions.

</specifics>

<deferred>
## Deferred Ideas

- `:eyelib-util` Gradle module scaffolding is deferred to Phase 16.
- Multi-consumer utility category migration is deferred to Phases 17-19.
- Submodule centralization is deferred to Phase 20.

</deferred>
