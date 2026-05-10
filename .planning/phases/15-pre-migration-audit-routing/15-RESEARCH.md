# Phase 15: Pre-Migration Audit & Routing - Research

**Researched:** 2026-05-10  
**Domain:** Java/Forge utility-boundary migration audit and IDE-assisted routing  
**Confidence:** HIGH for inventory and wildcard findings; MEDIUM for consumer counts that require implementation-time IDE reference confirmation

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
No explicit `## Decisions` section exists in `15-CONTEXT.md`; the phase context instead defines implementation decisions under `## Implementation Decisions`. [VERIFIED: `.planning/phases/15-pre-migration-audit-routing/15-CONTEXT.md`]

### the agent's Discretion
- All implementation choices are at the agent's discretion because this is a pure infrastructure/migration phase.
- Preserve the milestone's fixed decisions: `:eyelib-util` will use package namespace `io.github.tt432.eyelibutil`; MC/Forge-dependent utilities are allowed in `:eyelib-util`; single-consumer code moves to its functional owner instead of the util module.
- Keep Phase 15 limited to audit, explicit import cleanup, single-consumer relocations, and routing documentation. Do not scaffold `:eyelib-util` or migrate multi-consumer utility categories before Phase 16+.
- Use IDE-aware refactoring or verified references for package moves where possible; do not hand-edit generated Molang parser files.

### Deferred Ideas (OUT OF SCOPE)
- `:eyelib-util` Gradle module scaffolding is deferred to Phase 16.
- Multi-consumer utility category migration is deferred to Phases 17-19.
- Submodule centralization is deferred to Phase 20.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUDIT-01 | Every root/util/* and core/util/* file has a destination routing decision based on 0/1/N consumer classification. | Inventory below enumerates 32 root util Java files and 5 core util Java files from current filesystem glob output. [VERIFIED: Glob `src/main/java/io/github/tt432/eyelib/util/**/*.java`, Glob `src/main/java/io/github/tt432/eyelib/core/util/**/*.java`] |
| AUDIT-02 | All wildcard imports (`import io.github.tt432.eyelib.util.*`) in root are replaced with explicit imports. | Current wildcard scan found two broad wildcard imports in `BrAnimationEntry.java` and one nested `Tuple.*` wildcard in `TupleCodec.java`. [VERIFIED: JetBrains regex search `import\s+io\.github\.tt432\.eyelib\.util(?:\.[A-Za-z0-9_]+)*\.\*;`] |
| ROUTE-01 | Single-consumer utility classes are moved to their functional owner. | Roadmap names `AnimationApplier -> client/animation`, `Models -> client/model`, and `ModBridgeServer/BBModelSink -> mc/impl/modbridge`; package READMEs confirm those owners. [VERIFIED: `.planning/ROADMAP.md`; `client/animation/README.md`; `client/model/README.md`; `mc/README.md`] |
| ROUTE-02 | Compatibility shims are cataloged with consumer counts and deletion timing. | `ListHelper` delegates to `core/util/collection/ListAccessors`; `EitherHelper` delegates to `core/util/codec/Eithers`; deletion is tied to Phase 17 and Phase 19 respectively. [VERIFIED: source reads of `ListHelper.java`, `EitherHelper.java`; `.planning/ROADMAP.md`] |
</phase_requirements>

## Summary

Phase 15 is an audit-and-routing phase, not the `:eyelib-util` module creation phase. The planner should create one committed routing manifest and perform only low-risk cleanup/moves that make later phases deterministic: remove wildcard util imports, relocate or explicitly route the named single-consumer utilities, and catalog shims with deletion timing. [VERIFIED: `.planning/ROADMAP.md`; `.planning/phases/15-pre-migration-audit-routing/15-CONTEXT.md`]

Current inventory contains 32 Java files under `src/main/java/io/github/tt432/eyelib/util/` and 5 Java files under `src/main/java/io/github/tt432/eyelib/core/util/`. This inventory must be the manifest baseline; do not rely on stale earlier research that mentioned a different root-util count. [VERIFIED: Glob results in this session]

Current wildcard import debt is small and localized: `BrAnimationEntry.java` imports `io.github.tt432.eyelib.util.*` and `io.github.tt432.eyelib.util.codec.*`; `TupleCodec.java` imports `io.github.tt432.eyelib.util.codec.Tuple.*`. The success criteria explicitly target util package wildcard imports, so the planner should remove all three to avoid ambiguity. [VERIFIED: JetBrains regex search]

**Primary recommendation:** Plan Phase 15 as four narrow work packages: (1) write `docs/architecture/migration/utility-routing-manifest.md`, (2) replace wildcard imports with explicit imports, (3) physically relocate the four roadmap-named single-consumer utility files to their ROADMAP functional owner packages via IDE-aware moves while recording consumer-count evidence, and (4) add verification scans/tests using JetBrains MCP only. [VERIFIED: AGENTS.md; `.planning/ROADMAP.md`; JetBrains MCP availability; Phase 15 locked revision decision 2026-05-10]

## Project Constraints (from AGENTS.md)

- Read `docs/index/repo-map.md` before exploring code, and read `MODULES.md` before structural or multi-module planning. [VERIFIED: `AGENTS.md`]
- The repository is a multi-project Gradle + Java 17 + Forge codebase; the root runtime module coexists with multiple feature subprojects. [VERIFIED: `AGENTS.md`; `MODULES.md`]
- Preserve manager, loader, visitor, and codec patterns. [VERIFIED: `AGENTS.md`; `docs/architecture/01-module-boundaries.md`]
- Do not touch unrelated uncommitted changes and prefer narrow, stage-scoped edits. [VERIFIED: `AGENTS.md`]
- Document ownership and dependency rules before moving code across subsystem boundaries. [VERIFIED: `AGENTS.md`]
- Do not add new code to ambiguous catch-all `util/client` destinations without documenting responsibility. [VERIFIED: `AGENTS.md`; `src/main/java/io/github/tt432/eyelib/util/README.md`]
- Before each change, identify affected modules in `MODULES.md`; update `MODULES.md` if responsibilities, paths, or interactions change. [VERIFIED: `AGENTS.md`; `MODULES.md`]
- Treat `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` as generated/read-only during normal work. [VERIFIED: `AGENTS.md`]
- Use IntelliJ/JetBrains tooling; JDTLS is prohibited. [VERIFIED: `AGENTS.md`]
- All Gradle commands must run through JetBrains MCP (`jetbrain_build_project` or `jetbrain_run_gradle_tasks`); shell Gradle is forbidden. [VERIFIED: `AGENTS.md`]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Utility inventory and consumer classification | Documentation / source audit | IDE semantic index | The phase output is a routing manifest, and consumer counts must be verified from Java references rather than inferred from package names. [VERIFIED: `.planning/ROADMAP.md`; IDE Index skill loaded] |
| Wildcard import cleanup | Root Java source | IDE import optimizer/manual explicit imports | Wildcard imports exist in root source and must become class-level imports before later package moves. [VERIFIED: JetBrains regex search] |
| AnimationApplier relocation | `client/animation` | root util deletion/redirect | The class works on animation/model runtime data and the animation README owns runtime execution. [VERIFIED: `AnimationApplier.java`; `client/animation/README.md`] |
| Models relocation | `client/model` | root util deletion/redirect | The class merges importer `Model` definitions and the model README owns root-side model helpers/adapters. [VERIFIED: `Models.java`; `client/model/README.md`] |
| ModBridgeServer/BBModelSink relocation | `mc/impl/modbridge` | documentation update | Existing modbridge event ownership is already under `mc/impl/modbridge`, and roadmap routes the remaining bridge pair there. [VERIFIED: `utility-mc-bridges.md`; `.planning/ROADMAP.md`] |
| Shim catalog and deletion plan | Migration documentation | later migration phases | `ListHelper` and `EitherHelper` are compatibility delegators to core seams and are deleted after callers migrate. [VERIFIED: source reads; `.planning/ROADMAP.md`] |

## Standard Stack

### Core

| Library / Tool | Version | Purpose | Why Standard |
|----------------|---------|---------|--------------|
| Java | 17 | Main source language/runtime target | Project states Java 17 as the repository runtime baseline. [VERIFIED: `AGENTS.md`; `MODULES.md`] |
| Forge / Minecraft Gradle project | Project-managed | Provides MC/Forge types used by current util classes such as `ResourceLocations`, `EyelibCodec`, and `Shapes` | Milestone explicitly allows MC/Forge-dependent utilities in future `:eyelib-util`. [VERIFIED: `.planning/REQUIREMENTS.md`; `.planning/STATE.md`] |
| JetBrains IDE MCP / IDE Index MCP | Available, index ready | Semantic navigation, refactor-safe moves, diagnostics, and Gradle verification | Project rules require IntelliJ/JetBrains tooling and the IDE index was not in dumb mode. [VERIFIED: `AGENTS.md`; `ide_index_status`] |

### Supporting

| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| `jetbrain_search_regex` | MCP-provided | Regex scans for wildcard imports and residual old util imports | Use for AUDIT-02 and post-move residual checks. [VERIFIED: successful wildcard scan] |
| `ide_ide_search_text` / IDE reference tools | MCP-provided | Consumer discovery and class-name usage checks | Use before every routing decision; prefer semantic references when the tool accepts the target. [VERIFIED: IDE Index skill; current text searches] |
| `jetbrain_build_project` / `jetbrain_run_gradle_tasks` | MCP-provided | Compile/test verification | Use only through JetBrains MCP; do not run Gradle in shell. [VERIFIED: `AGENTS.md`] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| IDE semantic references | Text-only grep/import scans | Text scans are useful for wildcard imports but can miss semantic usages and produce false positives; use them only as supporting evidence. [VERIFIED: IDE Index skill guidance] |
| Moving all util files to `:eyelib-util` | Category-by-category migration in Phases 17-19 | Moving single-consumer code to the shared module violates milestone decisions and creates permanent boundary debt. [VERIFIED: `.planning/STATE.md`; `15-CONTEXT.md`] |
| Shell Gradle | JetBrains MCP Gradle/build tools | Shell Gradle violates project rules. [VERIFIED: `AGENTS.md`] |

**Installation:** No new packages or libraries should be installed in Phase 15. [VERIFIED: `.planning/ROADMAP.md` scope]

**Version verification:** No npm or external package versions are introduced by this phase. [VERIFIED: phase scope]

## Architecture Patterns

### System Architecture Diagram

```text
Inventory glob + IDE index
        |
        v
Per-file consumer classification (0 / 1 / N)
        |
        +--> 0 consumers -> route = delete, except roadmap-named Phase 15 relocation targets still move per locked decision
        |
        +--> 1 functional consumer -> route = functional owner package
        |
        +--> N consumers -> route = later :eyelib-util migration phase
        |
        v
Committed routing manifest
        |
        +--> wildcard import cleanup -> explicit imports -> residual scan = zero
        |
        +--> named single-consumer moves -> IDE-safe import/package update -> build/diagnostics
        |
        v
Phase 16+ uses manifest as migration contract
```

This flow reflects the 0/1/N rule and the roadmap's requirement for a maintainable routing manifest before module scaffolding. [VERIFIED: `.planning/ROADMAP.md`; `.planning/REQUIREMENTS.md`]

### Recommended Project Structure / Artifacts

```text
docs/architecture/migration/
├── utility-routing-manifest.md   # new Phase 15 manifest: every util/core-util source file, count, route, later phase
└── utility-mc-bridges.md          # update only if modbridge/util bridge status changes

src/main/java/io/github/tt432/eyelib/
├── client/animation/              # AnimationApplier target; must be physically moved in Phase 15
├── client/model/                  # Models target; must be physically moved in Phase 15
└── mc/impl/modbridge/             # ModBridgeServer + BBModelSink target; both must be physically moved in Phase 15
```

The new manifest should live under `docs/architecture/migration/` because existing migration boundary docs are committed there and AGENTS.md requires documenting ownership before boundary moves. [VERIFIED: Glob `docs/architecture/migration/*.md`; `AGENTS.md`]

### Pattern 1: Routing Manifest as Contract

**What:** One table row per Java source file under `root/util/**` and `core/util/**`, including path, primary symbol, verified consumer count, 0/1/N classification, target destination, phase owner, and verification evidence. [VERIFIED: `.planning/ROADMAP.md`]

**When to use:** Use before any move so later phases do not rediscover ownership. [VERIFIED: Phase 15 success criteria]

**Example:**
```markdown
| Source file | Consumers | Class | Route | Phase | Evidence |
|-------------|-----------|-------|-------|-------|----------|
| `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` | 1 production file (`BrBoneKeyFrame`) | 1 | delete after direct `ListAccessors` migration | 17 | IDE/text search 2026-05-10 |
```

### Pattern 2: IDE-Aware Move Before Text Cleanup

**What:** For classes that remain source files, use IDE-aware move/refactor tools where possible, then run residual import/package scans. [VERIFIED: `15-CONTEXT.md`; IDE Index skill]

**When to use:** Use for `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink`; these four roadmap-named files must physically relocate to their ROADMAP target functional owner packages in Phase 15 even if semantic reference checks find zero consumers. The manifest must still record consumer count and evidence, but Phase 15 must not autonomously delete these four classes. [VERIFIED: `.planning/ROADMAP.md`; current text searches; Phase 15 locked revision decision 2026-05-10]

### Anti-Patterns to Avoid

- **Hand-moving Java classes and hoping imports resolve:** breaks package declarations and imports; use IDE-aware moves when available. [VERIFIED: IDE Index skill]
- **Treating `mc/impl` as the default home for every MC import:** project docs say `mc/impl` is transitional, and feature-owned MC/Forge code should live with the feature owner. [VERIFIED: `mc/README.md`; `docs/architecture/01-module-boundaries.md`]
- **Scaffolding `:eyelib-util` in this phase:** explicitly deferred to Phase 16. [VERIFIED: `15-CONTEXT.md`; `.planning/ROADMAP.md`]
- **Migrating multi-consumer utility categories early:** Phases 17-19 own those migrations. [VERIFIED: `.planning/ROADMAP.md`]

## Existing Utility Inventory for Manifest Baseline

### Root util Java files (32)

| Category | Files | Preliminary route |
|----------|-------|-------------------|
| package metadata | `util/package-info.java`, `util/client/package-info.java`, `util/codec/package-info.java` | Update/delete as packages drain; include in manifest so empty-package cleanup is explicit. [VERIFIED: Glob] |
| top-level collection / misc | `Blackboard.java`, `Collectors.java`, `EntryStreams.java`, `Lists.java`, `ListHelper.java`, `ImmutableFloatTreeMap.java`, `SharedLibraryLoader.java`, `SimpleTimer.java`, `ResourceLocations.java` | Later `:eyelib-util` migrations except `ListHelper` shim deletion and `ResourceLocations.mod()` audit. [VERIFIED: Glob; `.planning/ROADMAP.md`] |
| codec | `ChinExtraCodecs.java`, `CodecHelper.java`, `DispatchedMapCodec.java`, `EitherHelper.java`, `EyelibCodec.java`, `KeyDispatchMapCodec.java`, `Tuple.java`, `TupleCodec.java` | Atomic codec migration in Phase 19; `EitherHelper` shim deletion after callers use `Eithers`. [VERIFIED: Glob; `.planning/ROADMAP.md`] |
| client util | `AnimationApplier.java`, `Models.java`, `client/texture/TexturePathHelper.java` | `AnimationApplier -> client/animation`; `Models -> client/model`; `TexturePathHelper` later texture/resource migration. [VERIFIED: Glob; `15-CONTEXT.md`; `.planning/ROADMAP.md`] |
| math | `Curves.java`, `EyeMath.java`, `FastColorHelper.java`, `MathHelper.java`, `Shapes.java` | Later math/color migration; `FastColorHelper` is a compatibility adapter to `ColorEncodings`. [VERIFIED: Glob; `FastColorHelper` import scan; `.planning/ROADMAP.md`] |
| search | `Searchable.java`, `SearchResults.java` | Later Tier-1 migration. [VERIFIED: Glob; `.planning/ROADMAP.md`] |
| modbridge | `BBModelSink.java`, `ModBridgeServer.java` | `mc/impl/modbridge` per roadmap. Current source scan shows no external production construction/use, but the locked Phase 15 revision decision requires physical migration rather than autonomous deletion; manifest rows still record consumer count/evidence. [VERIFIED: Glob; JetBrains regex search for `ModBridgeServer`/`BBModelSink`; `.planning/ROADMAP.md`; Phase 15 locked revision decision 2026-05-10] |

### Core util Java files (5)

| Category | Files | Preliminary route |
|----------|-------|-------------------|
| collection | `core/util/collection/ListAccessors.java` | Later collection migration; canonical replacement for `ListHelper`. [VERIFIED: Glob; `ListHelper.java`] |
| codec | `core/util/codec/Eithers.java` | Later codec migration; canonical replacement for `EitherHelper`. [VERIFIED: Glob; `EitherHelper.java`] |
| color | `core/util/color/ColorEncodings.java` | Later Tier-1 color migration. [VERIFIED: Glob; `.planning/ROADMAP.md`] |
| texture | `core/util/texture/TexturePaths.java` | Later texture/resource migration; canonical backend for `TexturePathHelper`. [VERIFIED: Glob; `TexturePathHelper` import scan; `.planning/ROADMAP.md`] |
| time | `core/util/time/FixedStepTimerState.java` | Later Tier-1 time migration. [VERIFIED: Glob; `.planning/ROADMAP.md`] |

## Single-Consumer / Shim Findings

| File | Current evidence | Routing recommendation |
|------|------------------|------------------------|
| `util/client/AnimationApplier.java` | Class exists and imports animation/model runtime types; current code text search found class declaration but no production call to `AnimationApplier.apply`. [VERIFIED: source read; IDE text search] | Physically migrate to `src/main/java/io/github/tt432/eyelib/client/animation/AnimationApplier.java` with package `io.github.tt432.eyelib.client.animation`; record consumer count/evidence in the manifest even if the count is `0`. Do not autonomously delete. [VERIFIED: `.planning/ROADMAP.md`; Phase 15 locked revision decision 2026-05-10] |
| `util/client/Models.java` | Class exists and defines `merge/add/sub`; current regex search found no direct `Models.merge/add/sub` calls in source/test. [VERIFIED: source read; JetBrains regex search] | Physically migrate to `src/main/java/io/github/tt432/eyelib/client/model/Models.java` with package `io.github.tt432.eyelib.client.model`; record consumer count/evidence in the manifest even if the count is `0`. Do not autonomously delete. [VERIFIED: `client/model/README.md`; `.planning/ROADMAP.md`; Phase 15 locked revision decision 2026-05-10] |
| `util/modbridge/ModBridgeServer.java` | Class exists; current regex search found only its own declaration/constructor and `BBModelSink` field/parameter, with no `new ModBridgeServer(...)` call in source/test. [VERIFIED: source read; JetBrains regex search] | Physically migrate to `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeServer.java` with package `io.github.tt432.eyelib.mc.impl.modbridge`; record consumer count/evidence in the manifest even if the count is `0`. Do not autonomously delete. [VERIFIED: `.planning/ROADMAP.md`; `mc/README.md`; Phase 15 locked revision decision 2026-05-10] |
| `util/modbridge/BBModelSink.java` | Interface is referenced only by `ModBridgeServer`. [VERIFIED: source read; JetBrains regex search] | Physically migrate with `ModBridgeServer` to `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/BBModelSink.java` with package `io.github.tt432.eyelib.mc.impl.modbridge`; record consumer count/evidence in the manifest even if the count is `0`. Do not autonomously delete or leave it as a standalone util interface. [VERIFIED: source read; Phase 15 locked revision decision 2026-05-10] |
| `util/ListHelper.java` | Delegates to `ListAccessors.first/last`; production consumer evidence is `BrBoneKeyFrame.java`. [VERIFIED: source read; IDE text search] | Catalog as shim; deletion tied to Phase 17 after caller(s) directly use `ListAccessors`/future `eyelibutil` equivalent. [VERIFIED: `.planning/ROADMAP.md`] |
| `util/codec/EitherHelper.java` | Delegates to `Eithers.unwrap`; current production consumers are `CodecHelper.java` and `ChinExtraCodecs.java`. [VERIFIED: source read; IDE text search] | Catalog as shim; deletion tied to Phase 19 atomic codec migration. [VERIFIED: `.planning/ROADMAP.md`] |
| `util/ResourceLocations.java` | `mod(String)` calls root `Eyelib.MOD_ID`; current scan found zero `ResourceLocations.mod(...)` calls. [VERIFIED: source read; JetBrains regex search] | Manifest should record `mod()` as likely deletable in Phase 18, but Phase 15 should not migrate the class. [VERIFIED: `.planning/STATE.md`; `.planning/ROADMAP.md`] |

## Wildcard Import Findings

| File | Wildcard import | Required action |
|------|-----------------|-----------------|
| `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` | `import io.github.tt432.eyelib.util.*;` | Replace with explicit imports used by the file, likely `ResourceLocations` is not in this file and the wildcard may cover only legacy utilities; verify with IDE optimize imports. [VERIFIED: source read; JetBrains regex search] |
| `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` | `import io.github.tt432.eyelib.util.codec.*;` | Replace with explicit codec imports, at minimum `ChinExtraCodecs` and `CodecHelper` based on source use. [VERIFIED: source read] |
| `src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java` | `import io.github.tt432.eyelib.util.codec.Tuple.*;` | Replace with explicit nested-type imports or qualify nested symbols; include in cleanup despite the roadmap examples naming broader wildcard packages. [VERIFIED: JetBrains regex search] |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Java reference counts | Custom parser over Java text | IDE semantic references plus supporting regex scans | Java imports, same-package usages, static calls, and wildcard imports make text-only counts unreliable. [VERIFIED: IDE Index skill] |
| Package moves | Manual filesystem move + search/replace | `ide_move_file` / IDE refactor where possible | IDE moves update package declarations and imports semantically. [VERIFIED: IDE Index skill] |
| Gradle verification | Shell `gradlew` | `jetbrain_build_project`, JetBrains run config, or `jetbrain_run_gradle_tasks` after sync | Project rules forbid shell Gradle. [VERIFIED: `AGENTS.md`] |
| Routing manifest generation | Implicit notes in PLAN.md only | Committed `docs/architecture/migration/utility-routing-manifest.md` | Success criteria require maintainers to inspect a committed destination routing decision. [VERIFIED: `.planning/ROADMAP.md`] |

**Key insight:** The hard part is not moving files; it is making future phases deterministic by proving each file's route before `:eyelib-util` exists. [VERIFIED: `.planning/ROADMAP.md`; `.planning/STATE.md`]

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|-----------------|
| Stored data | None identified for Phase 15; scope is Java source package/import routing, not persisted identifiers. [VERIFIED: `.planning/ROADMAP.md`] | No data migration. |
| Live service config | `ModBridgeServer` opens local TCP server code, but no source construction call was found in current scan. [VERIFIED: source read; regex search] | It must move to `mc/impl/modbridge` per locked Phase 15 decision; verify any non-git startup wiring before runtime smoke if such wiring is discovered. |
| OS-registered state | None identified. [ASSUMED] | No OS registration task unless user reports external ModBridge startup registration. |
| Secrets/env vars | None identified. [ASSUMED] | No secrets/env migration. |
| Build artifacts | Package moves may leave stale build output, but Gradle/IDE rebuild handles source correctness. [ASSUMED] | Use JetBrains build/rebuild gate after moves. |

## Common Pitfalls

### Pitfall 1: Stale consumer assumptions
**What goes wrong:** Roadmap names several single-consumer utilities, but current text scans show some may now be zero-consumer; treating that as deletion permission would violate the locked Phase 15 revision decision. [VERIFIED: `.planning/ROADMAP.md`; current regex/text searches; Phase 15 locked revision decision 2026-05-10]  
**Why it happens:** Prior cleanup phases may have removed callers after the roadmap/research assumptions were written. [ASSUMED]  
**How to avoid:** Treat Phase 15 as the source of truth: run IDE references and record 0/1/N evidence in the manifest. For `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink`, always move to the ROADMAP target packages and never autonomously delete, even when the recorded count is `0`. [VERIFIED: Phase 15 goal; Phase 15 locked revision decision 2026-05-10]  
**Warning signs:** A class appears only in its own declaration and planning docs. [VERIFIED: current searches for `AnimationApplier`, `ModBridgeServer`]

### Pitfall 2: Wildcard import hides actual dependency set
**What goes wrong:** Moving files before eliminating wildcard imports can leave ambiguous or stale imports. [VERIFIED: current wildcard scan]  
**Why it happens:** `BrAnimationEntry.java` currently imports broad util and util.codec packages. [VERIFIED: source read]  
**How to avoid:** Cleanup wildcard imports first, then re-run scan to zero. [VERIFIED: AUDIT-02 success criteria]  
**Warning signs:** `jetbrain_search_regex` still finds `import io.github.tt432.eyelib.util...*;`. [VERIFIED: JetBrains regex search]

### Pitfall 3: Treating compatibility shims as permanent module API
**What goes wrong:** `ListHelper` and `EitherHelper` move into `:eyelib-util` and become permanent duplicate APIs. [VERIFIED: source reads show they are delegators]  
**Why it happens:** They look like normal utility classes unless the core seam delegation is checked. [VERIFIED: `ListHelper.java`; `EitherHelper.java`]  
**How to avoid:** Catalog them with deletion timing and canonical replacements in the manifest. [VERIFIED: ROUTE-02]  
**Warning signs:** New code imports `ListHelper` or `EitherHelper` instead of canonical `ListAccessors`/`Eithers`. [VERIFIED: source reads]

### Pitfall 4: Updating docs without source-validating paths
**What goes wrong:** The manifest lists files that no longer exist or misses `package-info.java`. [VERIFIED: current glob count differs from older `.planning/research/SUMMARY.md` count]  
**Why it happens:** Utility cleanup has already happened across prior phases. [VERIFIED: `utility-mc-bridges.md`]  
**How to avoid:** Generate manifest from fresh glob/IDE file search during implementation and verify every path resolves. [VERIFIED: AGENTS docs-only verification rule]

## Code Examples

### Wildcard residual scan
```text
Tool: jetbrain_search_regex
Paths: ["src/main/java/**/*.java"]
Regex: import\s+io\.github\.tt432\.eyelib\.util(?:\.[A-Za-z0-9_]+)*\.\*;
Expected after cleanup: zero items
```
Source: current successful JetBrains regex search. [VERIFIED: JetBrains MCP]

### Manifest row shape
```markdown
| Source | Symbol | Consumers | 0/1/N | Route | Phase | Evidence |
|--------|--------|-----------|-------|-------|-------|----------|
| `src/main/java/io/github/tt432/eyelib/util/codec/EitherHelper.java` | `EitherHelper` | `CodecHelper`, `ChinExtraCodecs` | N-internal codec | delete after callers use `Eithers` | 19 | IDE/text search 2026-05-10 |
```
Source: source reads and IDE text search. [VERIFIED: `EitherHelper.java`; IDE text search]

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Mixed root `util/*` packages with catch-all helpers | Functional owner for 1-consumer code; `:eyelib-util` for shared utilities after Phase 16 | v1.3 roadmap | Phase 15 must prevent pollution of the future util module. [VERIFIED: `.planning/ROADMAP.md`; `.planning/STATE.md`] |
| `util/client` as a default helper destination | Named client owners such as `client/render`, `client/model`, `client/animation` | prior utility cleanup waves | New moves should not add ambiguous `util/client` code. [VERIFIED: `util/README.md`; `utility-mc-bridges.md`] |
| Core util compatibility adapters | Canonical core seams with legacy root adapters | first-wave utility seams | Shims must be deleted once callers migrate. [VERIFIED: `core/README.md`; source reads] |

**Deprecated/outdated:** Earlier planning research says there were 34 root util Java files; current fresh glob found 32 root util Java files. Use current glob as source of truth. [VERIFIED: Glob; `.planning/research/SUMMARY.md`]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | No OS-registered state embeds the util package/class names. | Runtime State Inventory | Low; if external ModBridge startup exists, moving package names could require tooling startup updates. |
| A2 | No secrets/env vars reference the renamed/moved util classes. | Runtime State Inventory | Low; Java package/class moves rarely affect secrets, but external tooling could be unusual. |
| A3 | Build artifacts are safely refreshed by IDE/Gradle rebuild. | Runtime State Inventory | Low; stale IDE/build caches could cause confusing diagnostics until rebuild/sync. |

## Open Questions (RESOLVED)

1. **RESOLVED — zero-consumer roadmap-named classes must still move, not be deleted in Phase 15.**
   - What we know: current scans found no production call to `AnimationApplier.apply`, `Models.merge/add/sub`, or `new ModBridgeServer`. [VERIFIED: JetBrains regex searches]
   - Locked decision: `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink` must be physically migrated to the ROADMAP target functional owner packages in Phase 15 even if semantic reference checks find zero consumers. The manifest must still record consumer count and evidence. Phase 15 must not autonomously delete these four classes. [VERIFIED: `.planning/ROADMAP.md`; Phase 15 locked revision decision 2026-05-10]
   - Target paths: `AnimationApplier -> src/main/java/io/github/tt432/eyelib/client/animation/AnimationApplier.java`; `Models -> src/main/java/io/github/tt432/eyelib/client/model/Models.java`; `ModBridgeServer -> src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeServer.java`; `BBModelSink -> src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/BBModelSink.java`. [VERIFIED: required Phase 15 revision]

2. **RESOLVED — package-info files are routed in the manifest and deleted only when their package has no source peers.**
   - What we know: three root util `package-info.java` files exist. [VERIFIED: Glob]
   - Decision: include every `package-info.java` in the manifest with route `package cleanup`; delete only when implementation proves the package has no remaining source peers. [VERIFIED: AGENTS docs/path verification]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| IDE Index MCP | Semantic source/reference checks | ✓ | MCP tool available; index not dumb | Use JetBrains text/regex searches plus manual review if specific reference tool parameter handling fails. [VERIFIED: `ide_index_status`] |
| JetBrains run configurations | Build verification | ✓ | `qylEyelib [build]`, `nullawayMain`, `Test`, client configs listed | Use `jetbrain_execute_run_configuration` when task list is not synced. [VERIFIED: `jetbrain_get_run_configurations`] |
| Gradle task list via JetBrains MCP | Task-specific verification | ⚠ | `jetbrain_list_gradle_tasks` returned no tasks; IDE says sync Gradle first | Run `jetbrain_sync_gradle_projects`, then use `jetbrain_run_gradle_tasks`; do not use shell Gradle. [VERIFIED: `jetbrain_list_gradle_tasks`; `AGENTS.md`] |

**Missing dependencies with no fallback:** None currently blocking research. [VERIFIED: available MCP tools]

**Missing dependencies with fallback:** JetBrains Gradle task list is not currently loaded; fallback is IDE Gradle sync or the existing `qylEyelib [build]` run configuration. [VERIFIED: JetBrains MCP outputs]

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit/Gradle test infrastructure present via many `*Test.java` files. [VERIFIED: JetBrains file search] |
| Config file | Gradle build files exist at root and subprojects. [VERIFIED: JetBrains file search] |
| Quick run command | `jetbrain_search_regex` residual scans + targeted `jetbrain_run_gradle_tasks` after Gradle sync. [VERIFIED: MCP availability] |
| Full suite command | `jetbrain_build_project` or `jetbrain_execute_run_configuration` `qylEyelib [build]`; do not run shell Gradle. [VERIFIED: `AGENTS.md`; run configuration list] |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command / Tool | File Exists? |
|--------|----------|-----------|--------------------------|--------------|
| AUDIT-01 | Routing manifest lists every root/core util source file with 0/1/N and route | docs/static verification | Glob current util/core-util files, compare against manifest rows; docs-only path verification | ❌ Wave 0 create manifest |
| AUDIT-02 | Wildcard util imports are zero | static scan | `jetbrain_search_regex` with util wildcard regex | ✅ tool available |
| ROUTE-01 | Named files reside in functional owner packages and old paths have no residual references | compile + static scan | IDE move/ref search; `jetbrain_build_project` or build run config | ✅ target package READMEs exist except no `mc/impl/modbridge/README.md` |
| ROUTE-02 | Shim counts and deletion phases are cataloged | docs/static verification | Manifest contains `ListHelper` and `EitherHelper` rows with replacements/deletion phase | ❌ Wave 0 create manifest |

### Sampling Rate
- **Per task commit:** residual regex scan for wildcard imports and old moved-class imports. [VERIFIED: success criteria]
- **Per wave merge:** JetBrains build/project diagnostics after Gradle sync or existing build run configuration. [VERIFIED: AGENTS.md; run config list]
- **Phase gate:** Full JetBrains build green plus manifest path-count verification. [VERIFIED: AGENTS.md; `.planning/ROADMAP.md`]

### Wave 0 Gaps
- [ ] `docs/architecture/migration/utility-routing-manifest.md` — covers AUDIT-01 and ROUTE-02. [VERIFIED: no existing manifest found by migration docs glob]
- [ ] Optional `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/README.md` — document modbridge destination if moving `ModBridgeServer`/`BBModelSink`. [VERIFIED: no README glob result]
- [ ] IDE Gradle sync before task-specific Gradle runs, because current task list is empty. [VERIFIED: `jetbrain_list_gradle_tasks`]

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | no | No auth/session behavior in scope. [VERIFIED: phase scope] |
| V3 Session Management | no | No session behavior in scope. [VERIFIED: phase scope] |
| V4 Access Control | no | No access-control behavior in scope. [VERIFIED: phase scope] |
| V5 Input Validation | yes, narrowly | Preserve existing validation in `ModBridgeServer` if moved; it bounds payload length to `50_000_000`. [VERIFIED: `ModBridgeServer.java`] |
| V6 Cryptography | no | No crypto behavior in scope. [VERIFIED: phase scope] |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| ModBridge TCP listener accidentally preserved without lifecycle ownership | Denial of Service / Tampering | If kept, route it to `mc/impl/modbridge`, document lifecycle owner, preserve payload bounds, and compile-test. [VERIFIED: `ModBridgeServer.java`; `mc/README.md`] |
| Package move creates stale imports and hidden classpath ambiguity | Tampering (build integrity) | Use IDE-aware moves, explicit imports, residual scans, and JetBrains build. [VERIFIED: IDE Index skill; `AGENTS.md`] |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/15-pre-migration-audit-routing/15-CONTEXT.md` — scope, decisions, deferred ideas. [VERIFIED]
- `.planning/ROADMAP.md` — Phase 15 goal and success criteria. [VERIFIED]
- `.planning/REQUIREMENTS.md` — AUDIT/ROUTE requirements. [VERIFIED]
- `.planning/STATE.md` — current focus and key v1.3 decisions. [VERIFIED]
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md` — project constraints and ownership. [VERIFIED]
- Source globs for `src/main/java/io/github/tt432/eyelib/util/**/*.java` and `src/main/java/io/github/tt432/eyelib/core/util/**/*.java`. [VERIFIED]
- JetBrains MCP searches for wildcard imports, utility imports, selected class usages, run configurations, and Gradle task availability. [VERIFIED]

### Secondary (MEDIUM confidence)
- `.planning/research/*.md` — useful historical migration research, but some counts are stale against current glob results. [VERIFIED]

### Tertiary (LOW confidence)
- Assumptions about no OS/env/runtime registrations for moved utility classes. [ASSUMED]

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — project docs and MCP tools verified. [VERIFIED]
- Architecture: HIGH — roadmap/AGENTS/MODULES/source docs align. [VERIFIED]
- Inventory: HIGH — fresh glob results produced the baseline. [VERIFIED]
- Consumer counts: MEDIUM — text/regex findings are current, but implementation should use IDE semantic references per file where possible before final route decisions. [VERIFIED: IDE Index skill]
- Pitfalls: MEDIUM — based on current scans plus project history; zero-consumer named classes require maintainer/semantic confirmation. [VERIFIED/ASSUMED as marked]

**Research date:** 2026-05-10  
**Valid until:** 2026-05-17, or earlier if util/core-util files change before planning. [ASSUMED]
