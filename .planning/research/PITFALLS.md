# Domain Pitfalls: v1.4 结构清理

**Domain:** Multi-module Gradle + Java 17 + Forge structural cleanup (brownfield refactoring)
**Researched:** 2026-05-11
**Confidence:** HIGH (based on 4 prior milestone post-mortems, build file inspection, IDE artifact analysis, and codebase survey)

---

## 1. Lessons From Previous Milestones

### v1.2 Particle Module Extraction (Phases 8-14)

**What worked:**
- Phase 8 established a buildable skeleton (`:eyelib-particle`) BEFORE any code moved — solo `jetbrain_build_project` passed on the empty module before migration began.
- Phase 9 used narrow API seams (lookup, spawn/remove) to decouple root from particle internals before extraction.
- Phase 10 locked schema/runtime ownership via explicit `ParticleDefinitionAdapter` — no ambiguous "who owns what."
- Phase 13 preserved observable command behavior (`/eyelib particle`) through platform adaptation layer — user-visible contract unchanged.
- Phase 14 final gate ran JetBrains MCP full build + ClientSmoke + hardware checklist.

**What broke / caused friction:**
- **PFUT-02 deferred debt:** Packet contract relocation was left as future scope — current ownership documented but not fully extracted. Packets that logically belong to `:eyelib-particle` still transit through root transport registration. Lesson: be explicit about what IS and IS NOT moving.
- **PFUT-03 deferred debt:** Independent `:eyelib-particle` artifact publication (maven publish) left as future scope — module exports but cannot be consumed standalone. Lesson: document artifact publication as a separate gate from code extraction.
- **Nyquist validation partial for Phases 11-12:** Runtime extraction and loading rewires had incomplete validation coverage at the time; only Phase 14 final gate closed the functional risk. Lesson: verify each phase independently before declaring it done.
- **Root legacy deletions were post-verified:** Root `client/particle/bedrock/**`, `ParticleLookup`, `ParticleManager`, `ParticleAssetRegistry`, and root `BrParticleRenderManager` were deleted during FM-005 but only confirmed gone in Phase 14. Lesson: run `glob` on deleted paths immediately after deletion, not at final gate.

### v1.3 Utility Module Extraction (Phases 15-21)

**What worked:**
- Phase 15 pre-migration audit was CRITICAL: every source file had a verified consumer count (0/1/N rule) and a committed routing decision before any code moved.
- Wildcard import elimination before migration: `import io.github.tt432.eyelib.util.*` → explicit class imports. This prevented silent import breakage during moves.
- Phase 16 scaffold + solo build verification BEFORE any code migration: `:eyelib-util` compiled via JetBrains MCP with exit code 0 while still empty.
- Phase 16 mods.toml with unique modId `eyelibutil` that didn't collide with 7 existing module modIds.
- Phase 17 Tier-1 migration: zero-dependency files (time, color, loader, math, search) moved atomically — they had no back-references to resolve.
- Using a separate package namespace `io.github.tt432.eyelibutil` (not `io.github.tt432.eyelib.util`) — eliminated split-package risk entirely.
- Phase 21 final static checks: `glob` for .java files in old paths, `grep` for old import patterns — both verified zero.

**What broke / caused friction:**
- **ResourceLocations.mod() circular reference to root's MOD_ID:** Had to be deleted (not parameterized) because the callers didn't need it. Lesson: identify cycle-creating references BEFORE moving classes.
- **Phase 18 Resource/texture migration had wrapper duplication:** `TexturePaths` existed in both `core/util/` and `:eyelib-util` simultaneously. Had to merge into canonical implementation and redirect callers. Lesson: don't create transitional duplicates; move atomically.
- **Single-consumer routing by locked decision:** `AnimationApplier`, `Models`, and `ModBridgeServer` were moved to functional owners despite zero current consumer evidence. Lesson: if consumers are ambiguous, flag as tech debt and verify later.
- **Deferred CENT-F01/CENT-F02:** Additional submodule duplicate centralization and root dependency-scope narrowing remain future scope — now in STATE.md deferred items.

### Cross-Milestone Patterns

| Pattern | Frequency | Severity |
|---------|-----------|----------|
| Incomplete import migration → compile failure on full build | 2/4 milestones | HIGH |
| IDE project file staleness after Gradle module changes | Every milestone | MEDIUM |
| Documentation drift (READMEs, MODULES.md not updated) | 3/4 milestones | MEDIUM |
| Tests referencing moved code with stale imports | 2/4 milestones | HIGH |
| Forgetting to update `settings.gradle` `include(...)` | 1/4 milestones | HIGH |

---

## 2. Project-Specific Constraints (Must Not Violate)

### Hard Blocks (No Exceptions)

| Constraint | Why | Violation Consequence |
|-----------|-----|----------------------|
| **No shell Gradle** — all Gradle commands via JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`) | Project policy; shell Gradle may use wrong JDK, miss IDE integration | Build undefined behavior; possible JDK mismatch |
| **No JDTLS / Eclipse artifacts** — `.vscode/`, `.eclipse/`, `.project`, `.classpath`, `.factorypath`, `.settings/`, `bin/` must never exist | Project policy; IntelliJ-only tooling | IDE conflict; rejected at PR review |
| **No behavior changes during moves** — module extraction must be pure refactoring unless a requirement explicitly changes behavior | 00-control-spec.md Execution Rules | Regression risk; complicates verification |
| **No touching unrelated uncommitted changes** | AGENTS.md Editing Rules | Merge conflicts; audit trail contamination |
| **No growing `Eyelib.java` with new reach-through accessors** | 00-control-spec.md Forbidden Moves | Anti-pattern regression; breaks module boundaries |
| **Generated code is read-only** — `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` | AGENTS.md Generated Code | Parser regeneration will overwrite; merge conflicts |
| **Preserve manager, loader, visitor, codec patterns** | 00-control-spec.md Execution Rules | Inconsistency with established architecture |

### Soft Constraints (Must Justify Deviation)

| Constraint | Why |
|-----------|-----|
| No further Gradle module split beyond current functional needs unless a human explicitly asks | 00-control-spec.md Non-Goals |
| No opportunistic renaming of broad package areas without a documented destination | 00-control-spec.md Non-Goals |
| Update MODULES.md on any module change | MODULES.md Update Rules |
| Update docs/architecture/01-module-boundaries.md on boundary changes | MODULES.md Update Rules |
| Update readers via AGENTS.md nav when index docs change | AGENTS.md |

---

## 3. Verification Gates (Must Pass)

Every cleanup operation must pass these gates in this order:

| Gate | Tool | What It Verifies | Failure Pattern |
|------|------|-----------------|-----------------|
| **G1: Solo Module Build** | `jetbrain_build_project` with `filesToRebuild` on the affected module | Module compiles independently | Missing dependencies, broken classpath |
| **G2: Full Project Build** | `jetbrain_build_project` with `rebuild=true` | ALL modules + root compile with zero errors | Stale imports in other modules, transitive breakage |
| **G3: NullAway** | `jetbrain_run_gradle_tasks` with `["nullawayMain"]` | Null safety invariants preserved | Moved code violates null contracts |
| **G4: Test Suite** | `jetbrain_run_gradle_tasks` with `["test"]` | Existing tests still pass | Moved/deleted code breaks test assertions |
| **G5: Import Purge** | `jetbrain_search_in_files_by_text` for old import patterns | Zero references to old paths | Missed import updates (especially in root, submodules) |
| **G6: File Purge** | `jetbrain_find_files_by_glob` for old file paths | Old source locations are empty | Files left behind, split-package risk |
| **G7: ClientSmoke (optional)** | `jetbrain_execute_run_configuration` with `"runClientSmoke"` | Runtime behavior preserved | Move breaks runtime wiring (not compilation) |
| **G8: Docs Alignment** | Manual review or `jetbrain_search_text` | MODULES.md, boundary docs, READMEs match new topology | Documentation drift |

---

## 4. Goal-by-Goal Risk Assessment

### Goal 1: capability 包内容迁移至 eyelib-attachment

**Current state:** The `capability/` package at root level contains:
- `EyelibAttachableData.java` — typed attachment storage
- `RenderData.java` — per-entity render data capability
- `ExtraEntityData.java` / `ExtraEntityUpdateData.java` — data-only codec records
- `EntityBehaviorData.java` — behavior state
- `EntityStatistics.java` — statistics holder
- `ItemInHandRenderData.java` — item rendering state
- `component/ModelComponent.java`, `AnimationComponent.java`, `RenderControllerComponent.java`, `ClientEntityComponent.java` — runtime capability components with Forge event subscriptions

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R1 | **Capability components have Forge event-bus subscriptions in root `mc/impl/capability/CapabilityComponentRuntimeHooks.java`** — moving capability code may break the event wiring | HIGH | HIGH | Audit `CapabilityComponentRuntimeHooks` before any move; ensure root MC event wiring stays in root |
| R2 | **Network packets reference capability types** — `DataAttachmentUpdatePacket`, `UniDataUpdatePacket`, `ExtraEntityDataPacket` in `mc/impl/network/packet/` decode through capability types | HIGH | HIGH | Catalog all packet-codec dependencies on capability types BEFORE moving |
| R3 | **Root `mc/impl/data_attach/` transitional wiring depends on capability types** — Forge capability/provider/event wiring in `mc/impl/data_attach/` binds to `EyelibAttachableData` | HIGH | MEDIUM | Document that MC wiring stays in root; only pure data/codec/contract types move to attachment |
| R4 | **Split-package risk if capability code moves to `io.github.tt432.eyelibattachment` namespace** — current root capability is `io.github.tt432.eyelib.capability` | HIGH | HIGH | Follow v1.3 pattern: use distinct namespace (`io.github.tt432.eyelibattachment.capability`) for moved code |
| R5 | **`EyelibAttachableData` has bidirectional runtime coupling** — it's both a data type AND a capability provider | MEDIUM | HIGH | Split definition from runtime wiring: move data/codec portion; keep Forge capability registration in root |
| R6 | **Tests reference capability types directly** — `AnimationComponentSerializableInfoTest`, `RenderControllerComponentTextureStateTest`, `AnimationComponentRuntimeInvalidationTest`, `CommonRuntimeUpdaterTest` | HIGH | HIGH | Update test imports as part of the same commit; run G4 immediately after move |

**Verification gates for Goal 1:**
- G2 (Full build): all modules + root compile
- G5 (Import purge): zero `import io.github.tt432.eyelib.capability.` in eyelib-attachment source (the moved code should use new namespace)
- G4 (Tests): all capability-related tests pass

---

### Goal 2: 重命名 eyelib-processor → eyelib-preprocessing

**Current state:** `:eyelib-processor` is a plain-JVM (no Forge) subproject with 8 Java source files. It depends on `:eyelib-importer` (compileOnly) and `:eyelib-molang` (implementation).

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R7 | **settings.gradle `include("eyelib-processor")` must change** — forgetting this breaks the entire Gradle project resolution | HIGH | CRITICAL | This is step 1. Verify with `jetbrain_list_gradle_projects_detail` after rename. |
| R8 | **Root `build.gradle` has 4 references to `eyelib-processor`:** `api project(':eyelib-processor')`, `additionalRuntimeClasspath project(':eyelib-processor')`, `jarJar project(':eyelib-processor')` | HIGH | HIGH | Grep for all occurrences first; update all 4 in one atomic edit |
| R9 | **`.idea/modules.xml` references `eyelib-processor`** — 2 module entries with paths like `.idea/modules/eyelib-processor/eyelib.eyelib-processor.main.iml` | CERTAIN | HIGH | Delete old .iml files; IDE will regenerate on next sync |
| R10 | **`.idea/gradle.xml` references `$PROJECT_DIR$/eyelib-processor`** — module must be re-imported in IDE | CERTAIN | HIGH | Update the path to `$PROJECT_DIR$/eyelib-preprocessing` |
| R11 | **`.idea/compiler.xml` references `eyelib.eyelib-processor.main` and `eyelib.eyelib-processor.test`** — annotation processor profiles reference these module names | CERTAIN | HIGH | Update module names in annotation processing profiles |
| R12 | **Module directory must be renamed:** `eyelib-processor/` → `eyelib-preprocessing/` | CERTAIN | HIGH | Use IDE rename (`ide_move_file`) for directory to preserve IDE tracking |
| R13 | **`eyelib-importer` depends on `:eyelib-processor`** — its `build.gradle` has `compileOnly project(':eyelib-processor')` | HIGH | HIGH | Update `eyelib-importer/build.gradle` in same commit |
| R14 | **`base.archivesName = 'eyelib-processor'`** in build.gradle — must update to `'eyelib-preprocessing'` | HIGH | MEDIUM | Update in same commit; affects published artifact name |
| R15 | **IDE sync will fail between settings.gradle update and directory rename** — Gradle can't find the old directory | CERTAIN | HIGH | Do ALL changes (settings.gradle, all build.gradle files, directory rename) in one atomic operation, then run `jetbrain_sync_gradle_projects` |
| R16 | **Run configurations might reference the old module name** — `jetbrain_get_run_configurations` | LOW | LOW | Check for any run configs that explicitly target `eyelib-processor` |

**Order of operations for safe rename (Goal 2):**
1. `jetbrain_search_in_files_by_text` for `eyelib-processor` across ALL files (`.gradle`, `.xml`, `.iml`, `.java`, `.toml`)
2. Update `settings.gradle`: `include("eyelib-preprocessing")`
3. Update root `build.gradle`: all 4 `eyelib-processor` → `eyelib-preprocessing`
4. Update `eyelib-importer/build.gradle`: `compileOnly project(':eyelib-preprocessing')`
5. Update `eyelib-processor/build.gradle`: `archivesName = 'eyelib-preprocessing'`
6. Update `.idea/gradle.xml`: path to `$PROJECT_DIR$/eyelib-preprocessing`
7. Rename directory: `eyelib-processor/` → `eyelib-preprocessing/` via `ide_move_file`
8. Run `jetbrain_sync_gradle_projects`
9. Delete regenerated `.idea/modules/eyelib-processor/` directory if IDE creates stale entries
10. Run G1 (solo build on `:eyelib-preprocessing`)
11. Run G2 (full rebuild)
12. Run G4 (tests)

**Verification gates for Goal 2:**
- G2 (Full rebuild): must pass with exit code 0
- G5 (Import purge): zero occurrences of `eyelib-processor` in any `.gradle`, `.xml`, or `.java` file
- G4 (Tests): all existing tests pass
- IDE module list shows `eyelib-preprocessing` not `eyelib-processor`

---

### Goal 3: 纯数据类归位到正确模块

**Current state examples:** `BrAcParticleEffectDefinition` (in root `client/animation/bedrock/controller/`) is a pure data record that wraps an importer schema type.

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R17 | **Ambiguous "纯数据类" definition** — what qualifies? Record types only? All DTOs? All codec-bearing records? | HIGH | MEDIUM | Define criteria BEFORE any moves: (a) Java `record`, (b) zero Forge/Minecraft runtime imports, (c) codec-only behavior, (d) no side effects |
| R18 | **Moving a data class may break its consumer's compilation** — if the consumer is in a module that doesn't depend on the target module | MEDIUM | HIGH | Audit ALL consumers before moving. Use `ide_find_references` on each candidate. |
| R19 | **`BrAcParticleEffectDefinition` depends on both `eyelib-importer` and `eyelib-molang`** — it imports `BrAcParticleEffect` (importer) and `MolangValue` (molang) | CERTAIN | HIGH | Target module must have both importer and molang as dependencies. If moving to `eyelib-importer`, molang may need to become a dependency. |
| R20 | **Data class may be "pure" NOW but could grow runtime behavior** — a record with only `fromSchema()/toSchema()` might get new methods later | LOW | LOW | Document the "owns data/codec only" boundary in the target module's README |
| R21 | **There may be more such data classes not yet identified** — the audit in Goal 3 needs to find ALL candidates | MEDIUM | MEDIUM | Run `ide_find_class` with pattern `*Definition` and manually classify each |
| R22 | **Records with `fromSchema()` methods technically have behavior** — moving them means consumers need import updates | HIGH | MEDIUM | Verify via G2 (full build) and G5 (import purge) |

**Verification gates for Goal 3:**
- Pre-move: `ide_find_references` on each candidate — confirm zero root-runtime dependency
- G2 (Full build): all consumers compile
- G5 (Import purge): zero old imports remaining

---

### Goal 4: 清理 client/animation 下无效接口

**Current state:** `client/animation/` has 40+ Java files. The "无效接口" needs definition.

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R23 | **"Invalid" needs concrete criteria** — unused? deprecated? empty? superseded by importer-owned schema? | HIGH | MEDIUM | Define criteria in plan BEFORE deletion. Candidates to audit: interfaces with zero implementations, interfaces with zero references outside their package, interfaces that duplicate importer schema. |
| R24 | **Interface might be used via reflection** — Forge event systems, Mixin targeting, or annotation-based discovery | MEDIUM | HIGH | Use `ide_find_implementations` AND `ide_search_text` for the interface name in ALL file types (`.java`, `.json`, `.toml`). Check `eyelib.mixins.json` for interface references. |
| R25 | **Deleting an interface that has a single implementation in `mc/impl/`** — the implementation is in a different package and might not be found by simple glob | MEDIUM | HIGH | Always run `ide_find_implementations` with scope `project_and_libraries` before deleting any interface |
| R26 | **Importer-owned schema interfaces in `eyelib-importer` might be shadowed** — root has `BrAnimationControllerDefinition` but importer has `BrAnimationController` schema | LOW | MEDIUM | Check for name collisions before declaring something "invalid" |
| R27 | **Legacy animation runtime ports** — `AnimationExecutionPort`, `AnimationStatePort`, `AnimationIdentityPort` in `AnimationRuntimePortSet` exist as part of a recent architecture change | LOW | HIGH | Do NOT delete recently-added port interfaces; they are NOT invalid |

**Verification gates for Goal 4:**
- Per candidate: `ide_find_implementations` with `scope=project_and_libraries`
- Per candidate: `ide_find_references` with `scope=project_and_libraries`
- Per candidate: `jetbrain_search_text` for interface name in `eyelib.mixins.json` and all `.toml` files
- G2 (Full build): after deletions
- G4 (Tests): after deletions

---

### Goal 5: 删除根目录数据库文件创建代码

**Current state:** `client/instrument/db/InstrumentDatabase.java` and `BackgroundFlushService.java` create an H2 database at `./eyelib_instrument`. The instrumentation subsystem has 19 Java files across `collector/`, `db/`, `event/`, plus `InstrumentConfig.java`, `InstrumentLifecycleHooks.java`.

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R28 | **The database code is part of a larger instrumentation subsystem** — deleting `InstrumentDatabase` alone leaves broken references in `BackgroundFlushService`, `InstrumentLifecycleHooks`, and all collectors | CERTAIN | HIGH | Assess: are we deleting JUST the database code, or the ENTIRE instrumentation subsystem? Clarify scope in plan. |
| R29 | **Active tests exist** — `InstrumentDatabaseTest`, `BackgroundFlushServiceTest`, `EventRingBufferTest`, `InstrumentConfigTest`, `InstrumentDisabledTest`, `InstrumentMolangIntegrationTest`, and 3 collector tests | CERTAIN | HIGH | Tests must be deleted or updated in the same commit. Account for all 9 test files. |
| R30 | **`InstrumentLifecycleHooks` registers with Forge event bus** — deleting this class may leave orphaned event registrations or cause runtime errors | MEDIUM | HIGH | If deleting the subsystem, also remove `InstrumentLifecycleHooks` registration from bootstrap. Check `EyelibMod.java` for `InstrumentLifecycleHooks` references. |
| R31 | **H2 database dialect is a Gradle dependency** — `com.h2database:h2:2.4.240` in root `build.gradle` is used by tests AND by the instrumentation code | MEDIUM | LOW | If removing instrumentation, verify H2 is still needed by tests (`testImplementation 'com.h2database:h2:2.4.240'`). The `implementation('com.h2database:h2:2.4.240')` via `jarJar` can be removed if instrumentation is deleted. |
| R32 | **Scope creep** — Goal 5 says "删除根目录数据库文件创建代码" which literally means "delete the database file creation code at root" but may also mean the subsystem that creates it | MEDIUM | MEDIUM | Clarify in plan: delete just `InstrumentDatabase.java` and `BackgroundFlushService.java`, or delete all of `client/instrument/`? |

**Verification gates for Goal 5:**
- Pre-deletion: `ide_find_references` on `InstrumentDatabase`, `BackgroundFlushService`
- Post-deletion: G2 (Full build), G4 (Tests — all existing non-instrument tests still pass)
- Post-deletion: G6 (File purge) — confirm deleted files are gone

---

### Goal 6: client/model/bake 移入 eyelib-preprocessing

**Current state:** `client/model/bake/` contains 5 files: `BakedModel.java`, `BakedModels.java`, `ModelBakeInfo.java`, `EmissiveModelBakeInfo.java`, `TwoSideModelBakeInfo.java`. These are in root runtime (Forge-dependent).

**Target module:** `eyelib-preprocessing` (currently `eyelib-processor`) — which is PLAIN-JVM, no Forge plugin.

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R33 | **CRITICAL: eyelib-processor is plain-JVM; bake code uses Forge/Minecraft types** — `BakedModel` likely references `ModelPart`, `PoseStack`, or `com.mojang.blaze3d.vertex.*` | HIGH | CRITICAL | FIRST audit bake code for Minecraft imports. If Minecraft types exist, either: (a) make eyelib-preprocessing a Forge module (adds `legacyForge` plugin), OR (b) extract platform-free portions only |
| R34 | **Making eyelib-preprocessing a Forge module changes its nature** — it was designed as a plain-JVM processing module | MEDIUM | HIGH | If going route (a), update `build.gradle` to add `legacyForge` plugin, `mods.toml`, and Forge dependency. This is a significant change. |
| R35 | **Bake code consumers are in root runtime** — `BakedModels` is consumed by model rendering pipeline. Moving it to preprocessing creates a dependency direction: root → preprocessing (Forge) | MEDIUM | MEDIUM | This is fine IF preprocessing is a Forge module. Currently root already depends on `:eyelib-processor` via `api project(...)`. |
| R36 | **The rename Goal 2 and this Goal 6 are coupled** — if the rename happens first, the target module is `:eyelib-preprocessing`; if bake moves first, it moves to `:eyelib-processor` which is later renamed | LOW | LOW | Always do Goal 2 (rename) before Goal 6 (bake migration) to avoid moving code twice |

**Verification gates for Goal 6:**
- Pre-move: audit each bake file for Minecraft/FORGE imports via `jetbrain_read_file`
- Pre-move: `ide_find_references` on each bake file to catalog consumers
- Post-move: G2 (Full build) — root consumers must resolve to new location
- Post-move: G6 (File purge) — `client/model/bake/` empty after move

---

### Goal 7: render/controller 基岩版 controller 分析/拆分

**Current state:** `client/render/controller/` contains 4 files: `RenderControllerEntry.java`, `RenderControllers.java`, `RenderControllerLookup.java`, `package-info.java`. These are runtime controller definitions and user-facing annotation-driven controllers.

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R37 | **"分析/拆分" is an analysis goal, not a move goal** — this needs a design phase BEFORE implementation, unlike Goals 1-6 | CERTAIN | HIGH | Treat Goal 7 as a two-part task: (Part A) analyze and document findings, (Part B) implement based on findings. Do NOT commit to a split direction before analysis. |
| R38 | **Bedrock controller runtime code is spread across multiple packages** — `client/animation/bedrock/controller/` (schema adaptation, execution, state ownership) AND `client/render/controller/` (entry definition, lookup) AND `eyelib-importer` (raw schema) | HIGH | HIGH | Map ALL controller-related code before deciding what to split. At minimum: 22 files in `client/animation/bedrock/controller/` + 4 files in `client/render/controller/` + importer schema |
| R39 | **`RenderControllers.java` uses annotation-based registration** — `@SubscribeEvent` and other Forge annotations make it a runtime concern | HIGH | MEDIUM | Controller entry definitions belong in root runtime; only analysis may move schema/codec portions |
| R40 | **Over-splitting creates complexity** — a controller has schema (importer), definition (root), execution (root animation), and render (root render controller). Adding another module boundary may not help. | MEDIUM | HIGH | The analysis must justify ANY split. "Keep in place" is a valid outcome. |

**Verification gates for Goal 7:**
- Part A (analysis): produce a documented map of controller code locations, responsibilities, and dependencies
- Part B (if split): G2 (Full build), G4 (Tests), G7 (ClientSmoke — verify animations/controllers still work)

---

### Goal 8: 重构过时的 README.md

**Current state:** 45+ README.md files across the repository. Some may reference pre-v1.3 module topology.

**Risks:**

| # | Risk | Likelihood | Impact | Prevention |
|---|------|-----------|--------|------------|
| R41 | **README updates may reference modules that don't exist yet** — if Goals 1-7 change module topology, READMEs updated before Goals 1-7 will be stale | MEDIUM | MEDIUM | Do Goal 8 LAST in the milestone, after all structural changes are complete |
| R42 | **Empty package directories may still have READMEs** — the PROJECT.md says "无内容的文件夹不保留 README" but determining "no content" requires checking for Java files | LOW | LOW | Use `glob` on each package directory; if only `package-info.java` + `README.md`, the README may be removable |
| R43 | **READMEs reference module names that may change** — `eyelib-processor` → `eyelib-preprocessing` | HIGH | MEDIUM | Run `jetbrain_search_text` for old module names across ALL README.md files after renames |
| R44 | **Root README.md documentation drift** — the root `README.md` may describe pre-v1.2 or pre-v1.3 module structure | MEDIUM | MEDIUM | Audit root README against current MODULES.md topology; rewrite if stale |
| R45 | **Package READMEs reference moved classes** — e.g., `eyelib-util/README.md` lists packages that now need updates | LOW | LOW | Verify each README lists correct current packages after all Goal 1-7 changes |

**Verification gates for Goal 8:**
- G8 (Docs alignment): Every README path matches actual filesystem state
- `jetbrain_search_text` for `eyelib-processor` in all `**/README.md` — zero results after rename
- Manual review: top-level `README.md` describes v1.4 module topology

---

## 5. Recommended Safe Execution Order

Based on dependency analysis, risk coupling, and the principle of "move before rename, simple before complex":

```
Phase A: Analysis & Audit (GOALS 4, 7)
├── Goal 4: Audit client/animation for invalid interfaces
│   - Identify candidates, verify zero references
│   - Run G4 (tests) after each deletion batch
│
└── Goal 7: Analyze render/controller bedrock controllers
    - Map all controller code locations
    - Document responsibility split (or decision NOT to split)
    - Part B (if applicable): execute split

Phase B: Module Rename (GOAL 2 first — unblocks Goal 6)
└── Goal 2: Rename eyelib-processor → eyelib-preprocessing
    - Atomic operation (settings.gradle + all build.gradle + IDE files + directory)
    - Run G2 (full rebuild) immediately
    - Run G5 (import purge) to verify zero old references

Phase C: Code Relocation (GOALS 1, 3, 6)
├── Goal 6: Move client/model/bake → eyelib-preprocessing
│   - Depends on Goal 2 (rename) completion
│   - Audit Minecraft imports before committing to move
│
├── Goal 3: Move pure data classes to correct modules
│   - Define criteria first
│   - Audit all candidates with ide_find_references
│   - Move atomically per class
│
└── Goal 1: Move capability code → eyelib-attachment
    - Most complex move (runtime coupling, network packets, tests)
    - Must split data/codec from runtime wiring
    - Keep Forge event wiring in root mc/impl

Phase D: Deletion (GOAL 5)
└── Goal 5: Delete instrumentation database code
    - Clarify scope (just db/ or entire instrument/ subsystem?)
    - Delete tests in same commit
    - Run G2 + G4

Phase E: Documentation (GOAL 8 — ALWAYS LAST)
└── Goal 8: Rewrite stale READMEs
    - After ALL structural changes are complete
    - Verify every path reference resolves
    - Update top-level README to v1.4 topology

Phase F: Final Verification
├── G2: Full project rebuild (jetbrain_build_project with rebuild=true)
├── G3: NullAway (jetbrain_run_gradle_tasks ["nullawayMain"])
├── G4: Full test suite
└── G7: ClientSmoke (jetbrain_execute_run_configuration "runClientSmoke")
```

### Rationale For This Order

1. **Analysis first (Goals 4, 7):** Avoids deleting or splitting code prematurely. Goal 4 deletions and Goal 7 analysis have zero dependency on other goals.
2. **Rename before moves (Goal 2 before Goals 6, 3):** Goal 6 (bake → preprocessing) target module depends on the rename. Renaming first avoids moving code into a module whose name will immediately change.
3. **Simple moves before complex (Goal 3 before Goal 1, Goal 6 separately):** Goal 3 (data classes) has fewer runtime coupling risks. Goal 1 (capability) has the most runtime coupling — do it last among moves when more of the codebase is stable.
4. **Deletion after moves (Goal 5):** Instrumentation subsystem may have references to capability code; deleting it after capability migration avoids orphaned references.
5. **Documentation always last (Goal 8):** READMEs describe the final state. Updating them earlier guarantees staleness.

---

## 6. Common Failure Modes (Cross-Cutting)

These apply to ALL goals:

### F1: Stale IDE Project Files After Module Changes
**Symptom:** IDE shows red errors for valid code, or Gradle sync fails silently.
**Root cause:** `.idea/modules.xml`, `.idea/gradle.xml`, `.idea/compiler.xml` reference old module names.
**Fix:** Run `jetbrain_sync_gradle_projects` after ANY settings.gradle change. If sync fails, manually update the 3 XML files.
**Detection:** Check `ide_index_status` — if `isDumbMode: true`, IDE is reindexing; wait.

### F2: Incomplete Import Migration
**Symptom:** Full project build (G2) fails with "cannot find symbol" in a module you didn't touch.
**Root cause:** Moving a class updates imports in the SAME module but forgets to update imports in dependent modules.
**Fix:** Always run `ide_find_references` BEFORE moving any class. After move, run G2 (full rebuild) — NOT just solo module build.
**Detection:** G2 failure in unexpected modules.

### F3: Gradle Dependency Edge Breakage
**Symptom:** Module compiles solo (G1 passes) but full build (G2) fails with circular dependency or missing dependency.
**Root cause:** Adding a `project(...)` dependency that creates a cycle, or forgetting to add a needed dependency when code moves.
**Fix:** Document the dependency graph BEFORE adding any edge. Check: root → all modules (root depends on everything), modules consume only what they declare.
**Detection:** `jetbrain_get_project_dependencies` to inspect the graph.

### F4: Test Breakage Without Corresponding Source Fix
**Symptom:** Tests fail after move because they reference old package paths.
**Root cause:** Moving source code but forgetting to move/update test code.
**Fix:** Use `ide_find_references` with scope `project_test_files` to find test references. Include test file updates in the same commit as the move.
**Detection:** G4 (test suite) failure.

### F5: Partial File Movement (Split Package)
**Symptom:** Two modules have the same package with different classes — classloader picks wrong one at runtime.
**Root cause:** Moving SOME but not ALL classes from a package to a new module, leaving others in the old package path.
**Fix:** Move entire packages, not individual classes. If partial move is needed, use a new package name in the target module (v1.3 pattern: `io.github.tt432.eyelibutil` not `io.github.tt432.eyelib.util`).
**Detection:** G6 (file purge) shows .java files remaining in old path. `ide_find_file` for the package name shows files in two locations.

### F6: NullAway Regression
**Symptom:** NullAway fails (G3) on code that wasn't changed.
**Root cause:** Moving a class into a module that doesn't have NullAway configured, OR the `AnnotatedPackages` option in `nullawayMain` doesn't include the new module's package.
**Fix:** After any move into a Forge module, verify that module's build.gradle has NullAway configuration. Currently only root has `nullawayMain` task — submodules may need it added.
**Detection:** G3 (NullAway) failure.

### F7: mods.toml modId Collision
**Symptom:** Forge complains about duplicate modId at startup.
**Root cause:** If a module is converted from plain-JVM to Forge (adding `legacyForge` plugin and `mods.toml`), its modId must be unique.
**Fix:** Check ALL existing mods.toml files for modId values BEFORE creating a new one. Current known modIds: `eyelib` (root), `eyelibattachment`, `eyelibimporter`, `eyelibmaterial`, `eyelibmolang`, `eyelibparticle`, `eyelibutil`.
**Detection:** G7 (ClientSmoke) or `jetbrain_execute_run_configuration` fails with modId conflict.

---

## 7. Checklist For Each Goal (Quick Reference)

Before starting any goal, verify:

```
[ ] All references to target code catalogued (ide_find_references)
[ ] All import sites identified (not just own module)
[ ] All test files that reference target code listed
[ ] All IDE project files that reference target paths known
[ ] Dependency graph updated (if adding/removing edges)
[ ] Verification gate sequence planned (which gates in which order)
[ ] Commit boundary defined (what's in one atomic commit)
```

After completing any goal, verify:

```
[ ] G2: Full rebuild passes (jetbrain_build_project with rebuild=true)
[ ] G4: Tests pass (if applicable)
[ ] G5: Zero old import references (jetbrain_search_text)
[ ] G6: Old paths empty (jetbrain_find_files_by_glob)
[ ] MODULES.md updated (if module inventory changed)
[ ] docs/architecture/01-module-boundaries.md updated (if boundary changed)
[ ] No .idea file staleness (ide_index_status shows ready)
```

---

## Sources

| Source | Confidence | What It Provided |
|--------|-----------|-----------------|
| `.planning/milestones/v1.3-MILESTONE-AUDIT.md` | HIGH | v1.3 what broke, what worked, deferred items |
| `.planning/milestones/v1.2-MILESTONE-AUDIT.md` | HIGH | v1.2 partial Nyquist, PFUT-02/03 deferred debt |
| `.planning/milestones/v1.3-ROADMAP.md` | HIGH | Phase-by-phase success criteria, dependency chains, verification patterns |
| `.planning/PROJECT.md` | HIGH | v1.4 8 cleanup goals, constraints, key decisions |
| `MODULES.md` | HIGH | Current module inventory, ownership, interaction rules |
| `docs/architecture/00-control-spec.md` | HIGH | Execution rules, forbidden moves, rollback strategy |
| `settings.gradle` | HIGH | Current module includes (7 submodules + composite) |
| `build.gradle` (root) | HIGH | All 7 submodule dependency declarations |
| `.idea/modules.xml` | HIGH | IntelliJ module entries for all submodules |
| `.idea/gradle.xml` | HIGH | Gradle-linked module paths |
| `.idea/compiler.xml` | HIGH | Annotation processor module profiles |
| `eyelib-processor/build.gradle` | HIGH | Plain-JVM nature (no Forge plugin) — critical for Goal 6 risk |
| `src/main/java/io/github/tt432/eyelib/client/instrument/` | HIGH | Full instrumentation subsystem scope for Goal 5 |
| `src/test/` directory listing | HIGH | All 54 test files, 9 instrumentation tests, capability-related tests |
| `src/main/java/io/github/tt432/eyelib/capability/` | HIGH | 13 capability files with runtime coupling to Forge events |
| `src/main/java/io/github/tt432/eyelib/client/animation/` | HIGH | 40+ animation files for Goal 4 audit |
| `src/main/java/io/github/tt432/eyelib/client/model/bake/` | HIGH | 5 bake files for Goal 6 migration audit |
| `src/main/java/io/github/tt432/eyelib/client/render/controller/` | HIGH | 4 render controller files for Goal 7 |
| `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/` | HIGH | 10+ controller runtime files for Goal 7 analysis |
| Deferred items in `.planning/STATE.md` | HIGH | CENT-F01, CENT-F02, AUDT-F01 still deferred — don't let scope creep |
