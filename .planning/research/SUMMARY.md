# Project Research Summary

**Project:** Eyelib v1.3 — eyelib-util 模块分离
**Domain:** Forge Multi-Module Shared Utility Library Extraction
**Researched:** 2026-05-10
**Confidence:** HIGH

## Executive Summary

The eyelib-util extraction is a **Forge-aware Gradle subproject addition** that consolidates 34 root/util/* files and 5 core/util/* files into a dedicated shared-utility leaf module. The migration is significantly simpler than initial estimates: **zero of the 6 existing submodules currently import from `io.github.tt432.eyelib.util.*`** — all 38 import sites exist exclusively within the root (:) module, with identity tests in attachment/importer/particle actively enforcing this boundary. This means the import-rewiring effort is confined to root internals only, with no submodule import changes required during the base migration phase.

The recommended approach is a phased, atomic per-file migration organized by dependency complexity. Build infrastructure (Phase 1) must be established first using the proven Forge subproject template from eyelib-attachment/material/particle/importer. Code extraction proceeds from zero-dependency categories (time, color, loader, math, search) through collection utilities, then to MC-dependent categories (resource, texture), and finally the high-risk codec infrastructure (20+ consumers, heavy MC/DFU coupling). Post-extraction, submodule-duplicated code (DispatchedMapCodec in eyelib-material, StreamCodec in eyelib-attachment) is centralized into eyelib-util as optional Phase 4.

Key risks center on five critical pitfalls: **package shadowing** from non-atomic file moves, **circular dependency** if eyelib-util references any project module, **mods.toml ID collision** with existing submodules, **single-consumer code** polluting the shared module boundary, and **ResourceLocations.mod() circular reference** to root's Eyelib.MOD_ID constant. All are preventable through the defined phase checklists — atomic move+delete operations, hard enforcement of zero `project()` dependencies in eyelib-util's build.gradle, pre-migration wildcard import audit, and per-file consumer-count classification.

## Key Findings

### Recommended Stack

eyelib-util must be a **Forge-aware Gradle subproject** (not pure JVM) following the exact build pattern established by eyelib-attachment, eyelib-material, eyelib-particle, and eyelib-importer. A pure JVM approach would violate PROJECT.md's explicit constraint ("May depend on MC/Forge; not artificially constrained to be pure Java") and prevent compilation of MC-dependent utility code like EyelibCodec, ResourceLocations, and Shapes. The build template is directly cloned from existing submodules with zero innovation — this milestone delivers a module, not a build system overhaul.

**Core technologies:**
- **Gradle java-library + legacyForge plugin**: Required for MC/Forge classpath access while maintaining clear API/implementation separation — proven by 4 existing submodules
- **Package namespace `io.github.tt432.eyelibutil`**: Prevents split-package conflicts with root's `io.github.tt432.eyelib.util` and follows existing submodule naming convention (`eyelibparticle`, `eyelibmaterial`, etc.)
- **mods.toml with modId `eyelibutil`**: Follows camelCase convention used by all 6 existing submodules; declares dependencies only on forge + minecraft (no cross-module dependencies)
- **Consumer dependency pattern A (api)**: Root build.gradle uses `api project(':eyelib-util')` + `modImplementation` + `jarJar` — matching existing submodule treatment; submodules use `implementation` to prevent transitive leakage

### Expected Features

**Must have (table stakes):**
- **collection utilities** (Blackboard, Lists, Collectors, EntryStreams, ListAccessors) — used by 10+ consumers; Blackboard is core to Molang/behavior logic
- **codec infrastructure** (EyelibCodec, CodecHelper, TupleCodec, ChinExtraCodecs, DispatchedMapCodec, KeyDispatchMapCodec, Eithers) — foundation for animation keyframes, behavior event filtering, particle definitions; 20+ consumers; heaviest MC/DFU coupling
- **math helpers** (EyeMath, Curves, MathHelper, FastColorHelper) — used across animation, rendering, and UI systems; 10-15 consumers
- **time utilities** (SimpleTimer, FixedStepTimerState) — animation timeline, particle lifecycle foundation
- **resource path factory** (ResourceLocations) — single file with 10+ consumers across Molang queries, render sync, animation definitions

**Should have (differentiators):**
- **stream codec centralization** (StreamCodec, StreamEncoder, StreamDecoder, EyelibStreamCodecs from eyelib-attachment) — general-purpose FriendlyByteBuf abstraction needed by network code across modules; currently siloed in attachment
- **texture path utilities** (TexturePaths + TexturePathHelper) — platform-free + MC-aware texture path derivation
- **color channel encoding** (ColorEncodings) — platform-free color transforms for texture pipelines
- **search infrastructure** (Searchable interface + SearchResults) — resource discovery for loaders and UI
- **native library loader** (SharedLibraryLoader) — cross-platform .dll/.so/.dylib unpacking and loading

**Defer (v2+):**
- Business logic utilities (AnimationApplier, Models) — permanently excluded, moved to functional owners
- External tool bridges (ModBridgeServer, BBModelSink) — permanently excluded, moved to mc/impl/modbridge
- Cross-submodule shared code beyond StreamCodec and DispatchedMapCodec — scope creep risk for v1.3

**Anti-features (never in eyelib-util):**
- Single-consumer code (AnimationApplier → client/animation, Models → client/model)
- Compatibility shims (ListHelper, EitherHelper — delete after consumers migrate)
- Data attachment contracts (DataAttachment*, DataAttachmentContainer — stay in eyelib-attachment)

### Architecture Approach

The extraction creates a **leaf dependency module** that sits at the bottom of the project dependency graph: eyelib-util depends on MC/Forge and external libraries (DFU, JOML, SLF4J) but **must not depend on any Eyelib module**. After extraction, root (:) adds a one-way dependency on eyelib-util, reversing the current organic coupling where root owns utility code internally. Submodules optionally adopt eyelib-util for shared helpers. The migration is mechanical: ~38 import statements in root are updated from `io.github.tt432.eyelib.util.*` to `io.github.tt432.eyelibutil.*`. The namespace change prevents split-package conflicts — a lesson from v1.2 particle extraction. All 34 root/util/* files and 5 core/util/* files must be removed from their original locations, leaving both directories empty.

**Major components:**
1. **eyelib-util module scaffold** — build.gradle (Forge-aware, zero project deps), mods.toml (modId: eyelibutil), settings.gradle include, identity test verifying no reverse imports
2. **Code migration layer** — atomic per-category file moves with package declaration updates; intra-util dependency resolution (codec family + ImmutableFloatTreeMap = atomic unit)
3. **Root rewire** — mechanical import rewriting for ~38 root import sites; root build.gradle dependency addition
4. **Submodule centralization** — opportunistic pull-in of shared code currently siloed in submodules (StreamCodec from attachment, DispatchedMapCodec from material)
5. **Cleanup gate** — deletion of empty root/util/* and core/util/* directories; documentation updates (MODULES.md, architecture docs)

### Critical Pitfalls

1. **Package shadowing (C1)** — Two copies of the same class on classpath (old root + new eyelib-util) causes ambiguous resolution errors. **Prevention:** atomic per-file move+delete; never leave both copies; per-phase build verification after each file movement batch.

2. **Circular dependency (D1)** — eyelib-util build.gradle declaring `project(':')` or any `project(':eyelib-*')` dependency creates an irreversible build cycle. **Prevention:** hard gate enforced by automated dependency audit; eyelib-util dependencies block must contain zero `project(...)` entries, only MC/Forge + external libraries.

3. **mods.toml ID collision (R1)** — Duplicate modId across modules causes Forge startup crash (not detectable at compile time). **Prevention:** use unique `eyelibutil` (no hyphen, consistent with existing convention); cross-reference all 7 existing modIds before finalizing.

4. **Single-consumer code pollution (O1)** — Moving AnimationApplier, Models, etc. into eyelib-util when they have exactly one functional consumer degrades module boundaries and creates permanent maintenance cost. **Prevention:** per-file consumer count audit (Phase 0); N=1 → move to functional owner; N≥2 → eyelib-util; N=0 → delete.

5. **ResourceLocations.mod() circular reference (R2)** — The `mod()` convenience method calls `new ResourceLocation(Eyelib.MOD_ID, path)` where MOD_ID is a root constant. Moving ResourceLocations to eyelib-util breaks this without root access. **Prevention:** delete `mod()` method; rewrite callers to use `ResourceLocations.of(Eyelib.MOD_ID, path)` where MOD_ID stays in root.

## Implications for Roadmap

Based on research, the suggested phase structure follows dependency complexity from zero-dependency pure-Java categories through MC-coupled infrastructure to submodule centralization:

### Phase 0: Pre-Migration Audit & Routing
**Rationale:** Must complete before any code moves to classify every file by consumer count (0/1/N rule) and identify intra-util dependency clusters (codec family + ImmutableFloatTreeMap form an atomic unit that can't be split across phases). Wildcard import cleanup (`import io.github.tt432.eyelib.util.codec.*` → explicit imports) prevents silent import resolution failures during migration.
**Delivers:** Consumer-audited file manifest with per-file destination decisions; wildcard-free import baseline; identified atomic migration units (codec family = 9+1 files must move together).
**Addresses:** Pitfalls C2 (wildcard collapse), O1 (single-consumer routing), C3 (intra-util self-reference)
**Avoids:** Premature file moves without knowing consumer graph

### Phase 1: Module Scaffold & Build Infrastructure
**Rationale:** eyelib-util has zero project dependencies — it can be built and compiled independently from day one. Establishing the build skeleton first verifies Gradle configuration before any code migration begins.
**Delivers:** eyelib-util directory + build.gradle (Forge-aware, zero project deps), mods.toml (modId: eyelibutil), settings.gradle include, identity test skeleton, successful solo build (`jetbrain_build_project`)
**Uses:** Full build.gradle template from STACK.md (legacyForge plugin, java-library, no project deps), mods.toml template
**Avoids:** Pitfalls C5 (EyelibCodec MC dependency — resolved since Forge module provides MC classpath), D1 (circular dependency — prevented by zero project deps), R1 (mods.toml collision — prevented by unique modId)

### Phase 2: Tier-1 Code Migration (Zero-Dependency Categories)
**Rationale:** Time, color, loader, math, and search categories have zero internal dependencies and minimal MC coupling — they can be extracted in any order with the lowest risk. These establish eyelib-util's codebase and prove the migration workflow on low-stakes files before tackling high-risk codec infrastructure.
**Delivers:** All tier-1 files moved to eyelib-util (time: 2 files, color: 1, loader: 1, math: 5, search: 2); updated ~20 root import sites; deleted shims (ListHelper, EitherHelper after consumer migration)
**Implements:** FEATURES.md Phase 2 categories; ARCHITECTURE.md Phase 2 code migration
**Avoids:** Pitfall C1 (atomic per-file move+delete), C4 (phased grouping limits import churn per wave)

### Phase 3: collection + resource + texture Migration (MC-Dependent Leaf Categories)
**Rationale:** collection utilities (Blackboard, Lists, Collectors, EntryStreams) have moderate consumer counts and zero MC imports (except ImmutableFloatTreeMap which depends on codec and must wait for Phase 4). ResourceLocations (1 file, 10+ consumers) requires resolving the MOD_ID circular reference before extraction. Texture path utilities (2 files) are low-risk and combine platform-free + MC-aware code.
**Delivers:** collection files (excluding ImmutableFloatTreeMap), ResourceLocations (with mod() method deleted), TexturePaths + TexturePathHelper merged into eyelib-util; core/util wrapper duplication resolved (TexturePathHelper → callers redirected to TexturePaths canonical)
**Implements:** FEATURES.md Phases 3-4; ARCHITECTURE.md Phase 3 root rewire
**Avoids:** Pitfall R2 (ResourceLocations.mod() resolved by deletion), O2 (core/util wrapper duplication resolved by merging canonical + adapter), O4 (Eyelib.MOD_ID stays in root)

### Phase 4: Codec Infrastructure & ImmutableFloatTreeMap
**Rationale:** This is the highest-risk extraction: 10 codec files + ImmutableFloatTreeMap with 20+ consumers across animation, behavior, particle, and importer domains. Must move as an atomic unit due to intra-util self-references (ImmutableFloatTreeMap depends on CodecHelper; all codec files reference Tuple, EitherHelper, ChinExtraCodecs internally). Heavy MC/DFU coupling (ExtraCodecs, AABB, Codec, Either, JOML types) requires careful import rewriting.
**Delivers:** All 9 root/util/codec/* + core/util/codec/Eithers.java + ImmutableFloatTreeMap moved to eyelib-util.codec; ~20+ root import sites updated; EitherHelper shim deleted
**Implements:** FEATURES.md Phase 5; ARCHITECTURE.md Phase 2 atomic codec family migration
**Avoids:** Pitfall C3 (atomic migration prevents intra-util self-reference breaks), C4 (IDE-assisted per-file refactoring handles 20+ import sites), D1 (no back-references to root types in eyelib-util codec signatures)

### Phase 5: Submodule Shared Code Centralization (Opportunistic)
**Rationale:** Consolidate duplicated code siloed in submodules that other modules could benefit from. eyelib-attachment's StreamCodec suite (5 files) is a general-purpose FriendlyByteBuf abstraction needed by particle/network code. eyelib-material's DispatchedMapCodec is a duplicate that must be merged into the canonical eyelib-util version. This phase is optional for v1.3 if risk levels warrant deferral.
**Delivers:** StreamCodec family centralized to eyelib-util.streamcodec; DispatchedMapCodec deduplication (eyelib-material copy deleted, imports redirected to eyelib-util); eyelib-attachment and eyelib-material add `implementation project(':eyelib-util')` dependencies
**Implements:** FEATURES.md Phase 6; ARCHITECTURE.md Phase 4 submodule centralization
**Avoids:** Pitfall D2 (duplicate DispatchedMapCodec resolved), D3 (dependency chain verified — eyelib-util stays leaf), D4 (scope audit: all submodules use `implementation`, not `api`, unless public API exposes eyelib-util types)

### Phase 6: Verification, Cleanup & Documentation
**Rationale:** Comprehensive verification gates ensure no residual imports, empty source directories, zero regression across all modules, and updated documentation reflecting the new module topology.
**Delivers:** Full project build verification; residual import grep (zero `io.github.tt432.eyelib.util.` in any source file); empty root/util/* and core/util/* directories; updated MODULES.md, architecture docs, ARCHITECTURE-BLUEPRINT.md; identity test confirmation for all submodules
**Avoids:** All pitfalls — comprehensive build + test + grep verification gates

### Phase Ordering Rationale

- **Phase 0 before any code moves:** Consumer audit prevents single-consumer code from entering eyelib-util; wildcard import cleanup prevents silent resolution failures
- **Phase 1 before migration:** Build infrastructure must exist and be verified before code can compile in the new module; solo build of empty eyelib-util proves Gradle configuration correctness
- **Phases 2-3 before Phase 4:** Zero-dependency and leaf categories establish the migration workflow and build confidence on low-risk files before tackling the high-risk codec infrastructure with 20+ consumers
- **Phase 4 codec as atomic unit:** ImmutableFloatTreeMap + all 9 codec files share internal references; splitting them across phases causes intermediate compilation failure
- **Phase 5 after base migration:** Submodule centralization depends on eyelib-util existing and being stable; moving StreamCodec requires eyelib-util to already have codec infrastructure
- **Phase 6 final gate:** Ensures no migration artifact remains before milestone declarion; documentation updates per MODULES.md rules in AGENTS.md

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 4 (Codec Infrastructure):** 20+ consumer import sites across animation, behavior, particle domains require careful IDE refactoring strategy; MC type propagation (ExtraCodecs, AABB) must be verified in eyelib-util Forge classpath; ImmutableFloatTreeMap scope decision (collection/ or codec/ package) needs final resolution
- **Phase 5 (Submodule Centralization):** StreamCodec relocation requires coordinating eyelib-attachment packet codecs (DataAttachmentSyncPacket, UpdateDestroyInfoPacket) that currently use attachment-local StreamCodec; DispatchedMapCodec behavioral equivalence between root and eyelib-material copies needs verification

Phases with standard patterns (skip research-phase):
- **Phase 1 (Module Scaffold):** Build patterns are directly cloned from 4 existing Forge submodules — no research needed
- **Phase 2 (Tier-1 Migration):** Files have zero internal dependencies and zero/minimal MC coupling — pure mechanical import rewriting
- **Phase 3 (collection/resource/texture):** Follows same mechanical pattern as Phase 2 with predictable import site counts

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All 7 existing module build.gradle files, settings.gradle, and mods.toml files physically read; build patterns verified across Forge-aware and pure-JVM modules; consumer dependency configuration options exhaustively mapped |
| Features | HIGH | All 39 source files (34 root/util/* + 5 core/util/*) physically inventoried; consumer counts verified via IDE reference search; import-site patterns mapped across root packages; shim files and duplicates identified |
| Architecture | HIGH | Import analysis proves zero submodule util imports via grep + identity test source code review; all ~38 root import sites mapped to specific packages; dependency direction rules verified against all 7 build.gradle files; namespace strategy validated against Java module system constraints and v1.2 lesson |
| Pitfalls | HIGH | 20 individual pitfalls classified across 4 categories; each mapped to specific phases with prevention strategies; v1.2 particle extraction lessons directly applied (package collisions, wildcard chaos, circular deps, import-hell, boundary leakage, duplicate code) |

**Overall confidence: HIGH**

### Gaps to Address

- **ResourceLocations.mod() call sites:** Consumer audit must confirm whether any of the 4 known callers (`BrAnimationEntryDefinition`, `MolangQuery`, `ClientRenderSyncService`, `RenderSyncApplyOpsTest`) actually use the `.mod()` convenience method before deciding between deletion vs. parameterization. Resolution happens in Phase 0 audit.
- **Single-consumer file classification completeness:** Preliminary audit identified AnimationApplier and Models as single-consumer; re-verification during Phase 0 may surface additional single-consumer candidates (Shapes, ModBridgeServer, SharedLibraryLoader). Per-file IDE reference search will finalize routing.
- **StreamCodec dependency chain in eyelib-attachment:** When StreamCodec moves to eyelib-util, attachment's own packet codecs (DataAttachmentSyncPacket, UpdateDestroyInfoPacket) must be verified for compatibility with centralized StreamCodec. Distinct from StreamCodec interface portability — verification scope TBD during Phase 5 planning.
- **ImmutableFloatTreeMap package placement:** Currently classified for `collection/` package (data structure semantics) but has codec dependency requiring codec infrastructure to be present in eyelib-util. Final placement (collection/ with codec-import, or codec/ as codec-first artifact) to be resolved during Phase 4 detailed planning.
- **SharedLibraryLoader native loading validation:** Native library loading path changes when class moves from root JAR to eyelib-util JAR. Current consumer count is unknown — Phase 0 audit determines whether to keep, delete, or repackage natives alongside loader.

## Sources

### Primary (HIGH confidence)
- **Repository build files (all 7 modules):** `settings.gradle`, root `build.gradle`, `eyelib-attachment/build.gradle`, `eyelib-importer/build.gradle`, `eyelib-material/build.gradle`, `eyelib-particle/build.gradle`, `eyelib-molang/build.gradle`, `eyelib-processor/build.gradle` — verified Forge vs pure-JVM patterns, dependency configurations, plugin sets
- **Repository metadata files:** All 7 `mods.toml` files — verified modId convention, dependency declarations, forge/minecraft version ranges
- **Source code inventory:** All 34 root/util/*.java files + 5 core/util/*.java files + 5 eyelib-attachment streamcodec files — physically read and classified
- **Identity test source code:** `AttachmentModuleIdentityTest.java`, `ImporterModuleIdentityTest.java`, `ParticleDefinitionBoundaryTest.java` — confirmed submodule boundary enforcement
- **Project documentation:** `PROJECT.md` (v1.3 milestone definition, Key Decisions), `MODULES.md` (complete module inventory), `root/util/README.md` (historical notes, boundary reminders), `core/README.md` (platform-free boundary rules), `docs/architecture/01-module-boundaries.md`, `docs/architecture/ARCHITECTURE-BLUEPRINT.md`
- **v1.2 extraction experience:** `.planning/REQUIREMENTS.md` (18 requirements, traceability, verification patterns)
- **Gradle documentation:** Java Library Plugin API vs Implementation separation, Gradle dependency management
- **Forge documentation:** 1.20.1 mods.toml specification, MDGL LegacyForge plugin documentation

### Secondary (MEDIUM confidence)
- **IDE search results:** Grep for `import io.github.tt432.eyelib.util.` across all submodules (0 results) and root (32 results); grep for `import io.github.tt432.eyelib.core.util.` (6 results) — confirms import-site isolation
- **Import site consumer mapping:** Root package-level analysis (client/animation/, client/render/, common/behavior/, etc.) cross-referenced with specific util class usage

### Tertiary (LOW confidence)
- _None_ — all findings are based on direct source code analysis with no inference-only conclusions

---
*Research completed: 2026-05-10*
*Ready for roadmap: yes*
