# Project Research Summary

**Project:** Eyelib v1.5 深度结构清理
**Domain:** Multi-module Forge rendering library structural cleanup
**Researched:** 2026-05-12
**Confidence:** HIGH

## Executive Summary

This is a **structural cleanup milestone** for a mature multi-module Forge rendering library (Java 17, Forge 1.20.1, 7 submodules + root). The v1.4 milestone moved 5 data/codec types from root to `:eyelib-attachment` and deleted `KeyFrame.java`, completing the major module extraction phase. v1.5 is **boundary hardening and residue removal** — no new modules, no API breaks, no dependency direction changes, no new technology introduction. All work happens within the existing toolchain (JetBrains MCP Gradle, IDE reference analysis).

**Overall architecture health is GOOD.** The codebase follows three well-established patterns: Root-as-Consumer (submodule data types referenced one-way from root), Bridge Event Hooks (root `mc/impl/` bridges Forge events to submodule code), and Schema→Runtime Adaptation (importer owns canonical schemas, root hosts compiled runtime adapters). The primary risk is not structural instability but **precision** during cleanup — misidentifying active runtime code as dead (especially in `client/animation/bedrock/`) or moving the wrong code across boundaries could silently break the animation system.

**Recommended approach:** Execute five cleanup tasks in strict dependency order: documentation audit first (zero risk), dead code deletion second (with per-file IDE reference verification), preprocessing+DUP scans third (read-only analysis), capability audit last (most complex, requires full context from prior phases). Each phase gates on `jetbrain_build_project` passing with exit code 0. Stale `.class` files must be purged via `clean build` before the deletion phases to prevent IDE reference false positives.

## Key Findings

### Confirmed Across All Researchers

These findings received consensus from all 4 researcher agents (STACK, FEATURES, ARCHITECTURE, PITFALLS):

1. **7-module + root topology is correct and must not change.** No new Gradle subprojects, no removal of existing modules. The current split (root runtime, attachment data, preprocessing bake/parse, importer schema, Molang compiler, material GL, particle engine, utility shared) is the intentional v1.4 target state.

2. **`EyelibAttachableData.java` must stay in root.** It is a Forge registry hub (`@Mod.EventBusSubscriber`, `DeferredRegister`, `RegistryObject<DataAttachmentType<...>>`), not a data type. Moving it to attachment would break Forge bootstrap and cause a startup crash.

3. **`client/animation/bedrock/` runtime types are ACTIVE, not dead code.** `BrClipExecutor`, `BrControllerExecutor`, `BrBoneAnimation`, `BrBoneKeyFrame`, and all `*Definition` types are actively consumed by the `EntityRenderSystem → BrAnimator → Animation.tickAnimationUntyped()` rendering pipeline. Only per-file IDE Find References can distinguish dead from alive.

4. **Definition types are NOT Schema duplicates.** Root `Br*Definition` types (e.g., `BrBoneAnimationDefinition`) are runtime-compiled adaptations with pre-computed state (`ImmutableFloatTreeMap`, Catmull-Rom `setupCurvePoints`). Importer `Br*Schema` types are raw Codec-parsed schemas. Same-name fields do not equal duplicate code — the consumer determines the layer.

5. **No new technology stack needed.** All operations use JetBrains MCP Gradle (no shell `./gradlew`), IDE reference analysis, and text search. GitNexus is not indexed on this repo.

6. **Clean build before deletion phases is mandatory.** Stale `.class` files in `bin/` from already-deleted sources (like `KeyFrame.java`) can cause IDE reference false positives. A clean build ensures the bin/ tree matches src/.

### Contested or Uncertain Areas

| Area | Discrepancy | Resolution |
|------|-------------|------------|
| **Phase order: PREP-01 vs DUP-01** | FEATURES puts PREP→DUP, ARCHITECTURE puts DUP→PREP | Both are read-only scan phases with no side effects. **Combine them into a single phase or run in parallel.** Running DUP first is slightly preferred since it's a pure analysis (comparing signatures/fields across modules) that doesn't depend on PREP's scan results. |
| **EntityBehaviorData codec extraction** | PITFALLS warns against mechanical extraction; ARCHITECTURE flags it as "needs audit." FEATURES lists it as a Differentiator (nice-to-have), not a Table Stake. | **Defer to post-v1.5.** The codec is tightly coupled to `MolangQuery` behavior semantics. Extraction without understanding this binding risks breaking variant/markVariant serialization. Treat as a separate investigation phase. |
| **Root vs attachment boundary for ItemInHandRenderData** | Not flagged by any researcher — it's confirmed as root runtime owner, parallel to `RenderData`. | No action needed. Confirmed correct. |

### Recommended Stack

No new technology introduction. All work done with existing tools:

| Tool | Purpose | Constraint |
|------|---------|------------|
| **JetBrains MCP** (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`) | Compilation and Gradle task execution | Shell `./gradlew` is PROHIBITED by project rules |
| **IDE Find References** (`ide_find_references`) | Per-file reference analysis for ANIM-01 dead code detection | Scope: `project_production_files` |
| **IDE Text/Symbol Search** | Full-text search for DOCS-01 old name detection, PREP-01 pattern scanning | `eyelib-processor` → `eyelib-preprocessing` migration |
| **IDE Glob Search** (`jetbrain_search_file`) | File pattern matching for PREP-01 residual scan | `*.java` with Codec+JsonElement import patterns |

**Core platform (unchanged):** Java 17 (Mojang 1.20.1 requirement), Forge 1.20.1 (mod platform), MDGL 2.0.91 (Gradle build plugin).

### Expected Features (Cleanup Tasks)

**Table stakes (must complete — milestone incomplete without them):**

| ID | Task | Complexity | Key Risk |
|----|------|------------|----------|
| **DOCS-01** | README/文档审视: 搜索并替换 `eyelib-processor` 旧名，验证所有 README 反映当前结构 | Low | Pitfall #8: 误删 `mixin/README.md` compatibility pointer |
| **ANIM-01** | `client/animation/` 无效接口清理: 逐文件 IDE Find References，删除零引用接口 | Medium | Pitfall #2: 误删活跃的 `Br*Executor` |
| **CAP-01** | capability 残留审计: 确认 root `capability/` 中无遗漏的纯 data/codec 类型 | Low-Medium | Pitfall #3: 移动 `EyelibAttachableData` registry hub |
| **PREP-01** | preprocessing 归属扫描: 确保所有 preprocessing 职责代码已在 `:eyelib-preprocessing` | Low | Pitfall #5: 移动仍有 root runtime 依赖的代码 |
| **DUP-01** | 重复代码排查: 搜索 root capacity/ 中 attachment 已拥有的数据类型的影子版本 | Medium | Pitfall #1: 误判 Definition→Schema adaptation layer 为重复 |

**Differentiators (nice-to-have, improve codebase health):**

- `bin/` stale `.class` 清理 — 运行 `clean build`（建议在 ANIM-01 前作为前置步骤）
- Type hierarchy audit — 验证 Br*Definition → Br*Schema → Runtime 三层无断裂

**Defer (post-v1.5):**

- `EntityBehaviorData` codec 提取到 attachment — 需要专门分析 codec↔MolangQuery 耦合
- 新 Gradle 子项目创建 — 明确拒绝（anti-feature #1）
- 将 `Br*Definition` 运行时类型合并到 Importer — 明确拒绝（anti-feature #2）
- 删除 `mixin/README.md` — 明确拒绝（anti-feature #5）

### Architecture Approach

The codebase follows a **Root-as-Consumer, Submodule-as-Data-Owner** pattern with three reinforcing mechanisms:

1. **Pattern 1: Root-as-Consumer.** Root runtime code imports and references submodule-owned data/codec types as generic parameters (e.g., `DataAttachmentType<ExtraEntityData>`). Submodules never import root runtime packages. This creates a strict one-way dependency: root → submodules.

2. **Pattern 2: Bridge Event Hooks.** Root `mc/impl/` bridges Forge lifecycle events to submodule code (`ModelBakeInvalidationHooks`, `EntityExtraDataRuntimeHooks`). Submodules register handlers without importing root, preventing reverse dependencies.

3. **Pattern 3: Schema→Runtime Adaptation.** Importer (`:eyelib-importer`) owns canonical Bedrock JSON schema/codec types. Root hosts compiled runtime adaptation types with pre-computed state (Catmull-Rom interpolation setup, sorted key maps). The bridge is `BoneAnimationBaker` in preprocessing.

**Component architecture diagram (simplified):**

```
Root (::)
├── capability/          ← Registry hub + runtime component state
│   ├── EyelibAttachableData  (Forge registry, STAYS)
│   ├── RenderData            (runtime owner, STAYS)
│   └── component/            (runtime state, STAYS)
├── client/animation/    ← Runtime animation execution
│   ├── bedrock/             (active executors — verify, don't blindly delete)
│   └── Animation<T>         (port system, STAYS)
└── mc/impl/             ← Bridge hooks (STAYS)

:eyelib-attachment     ← Data/codec types (AnimationComponentInfo, etc.)
:eyelib-preprocessing  ← Bake helpers + loader parsing
:eyelib-importer       ← Bedrock schema/codec definitions
:eyelib-molang         ← MoLang compiler/runtime
:eyelib-material       ← Bedrock material + GL state
:eyelib-particle       ← Particle module API
:eyelib-util           ← Shared utilities
```

**Full build order and component taxonomy:** See [ARCHITECTURE.md](./ARCHITECTURE.md) for complete namespace mapping (which types stay/move/delete) and data flow diagrams (capability sync, animation sync, animation execution, schema load).

### Critical Pitfalls

Ranked by severity and likelihood:

| # | Pitfall | Phase | Severity | Prevention |
|---|---------|-------|----------|------------|
| 1 | **误删活跃的 `Br*Executor`** — 将 `BrClipExecutor`/`BrControllerExecutor` 判定为无效代码并删除 | ANIM-01 | **CRITICAL** (动画系统停止工作) | 对 `bedrock/` 中**每个** .java 文件运行 IDE Find References，仅删除零引用文件 |
| 2 | **移动 `EyelibAttachableData` registry hub** — 将其视为 data type 移入 attachment | CAP-01 | **CRITICAL** (Forge 启动崩溃) | 检查 `@Mod.EventBusSubscriber`、`DeferredRegister`、`RegistryObject` 标记 — registry hub 留在 root |
| 3 | **误判 Definition 为 Schema duplicate** — 看到同名字段就认为是重复代码 | DUP-01 | **HIGH** (破坏运行时预编译缓存) | 比较消费者 — Schema→Codec (importer), Definition→Sampler/Executor (root)。消费者不同 = 不是 duplicate |
| 4 | **机械提取 EntityBehaviorData codec** — 不加分析就把 codec 部分移到 attachment | CAP-01 | **MEDIUM** (可能破坏 MolangQuery 序列化) | 分析 codec 的 `RecordCodecBuilder` 字段消费者。如果深度绑定 root Molang 语义，不能提取 |
| 5 | **误移 root runtime 依赖代码到 preprocessing** — 移动的代码 import root runtime 包 | PREP-01 | **MEDIUM** (反向依赖 + 编译失败) | 硬性规则：如果文件 import `io.github.tt432.eyelib.capability` 或 `io.github.tt432.eyelib.client.animation` → 不能移动 |
| 6 | **stale .class 导致 IDE 引用误报** — bin/ 中残留已删除源文件的 .class | ALL | **LOW** (影响引用分析准确性) | ANIM-01 前运行 clean build |
| 7 | **旧模块名 `eyelib-processor` 残留** — README 中仍有 v1.4 前的旧名 | DOCS-01 | **LOW** (开发者困惑) | 全文搜索 `eyelib-processor`，替换为 `eyelib-preprocessing` |
| 8 | **误删 legacy compatibility pointer** — 删除 `mixin/README.md` | DOCS-01 | **LOW** (破坏重定向链) | 检查 README 内容，包含 "legacy"、"compatibility pointer" 的不删除 |

## Implications for Roadmap

Based on cross-dimensional research, the v1.5 milestone should execute in **4 sequential phases** with strict ordering based on dependency analysis:

### Phase 1: DOCS-01 — Documentation Audit & Remediation

**Rationale:** Zero compilation impact, no risk to runtime, can start immediately. Establishes accurate documentation baseline before code changes begin. The `eyelib-processor` → `eyelib-preprocessing` rename search validates that grep-style operations work correctly before they're needed for code changes.

**Delivers:** Updated README files reflecting v1.4 target state. All `eyelib-processor` references replaced with `eyelib-preprocessing`. Verification that no README exists in drained/deleted directories.

**Addresses:** DOCS-01 (FEATURES table stake)

**Avoids:** Pitfall #7 (old name `eyelib-processor` in README), Pitfall #8 (deleting `mixin/README.md` compatibility pointer)

**Verification:** `jetbrain_search_in_files_by_text` for `eyelib-processor` returns 0 production results.

### Phase 2: ANIM-01 — Dead Animation Interface Cleanup

**Rationale:** Depends only on DOCS-01 (no code dependency, but needs documentation context). Must complete before CAP-01 because animation status knowledge informs the capability audit. Per-file IDE Find References is the sole gating mechanism — no file is deleted without verification.

**Delivers:** Deleted zero-reference interfaces from `client/animation/` (including `bedrock/` subdirectory). Clean build with no broken compilation. Documentation of which files were deleted and why (reference counts).

**Addresses:** ANIM-01 (FEATURES table stake)

**Uses:** JetBrains MCP IDE Find References (scope: `project_production_files`), `jetbrain_build_project` for verification

**Avoids:** Pitfall #2 (deleting active `Br*Executor`), Pitfall #6 (stale .class causing false positives — run clean build first)

**Pre-step (mandatory):** `jetbrain_run_gradle_tasks taskNames: [":clean"]` followed by `jetbrain_build_project` to purge stale .class files before reference analysis.

### Phase 3: PREP-01 + DUP-01 — Combined Structural Scan

**Rationale:** Both are read-only analysis phases with no code modifications. Running them together avoids redundant module traversal. PREP-01 scans root for residual preprocessing patterns (Codec+JsonElement imports); DUP-01 scans for duplicated data types across root capacity/ and attachment capability/. Combined results inform Phase 4 migration decisions.

**Delivers:** Two reports: (1) PREP-01 report listing root files that may belong in preprocessing (with import analysis showing why they can/cannot move). (2) DUP-01 report listing potential duplicates with distinction between intentional adaptation layers (Definition→Schema) and genuine copy-paste.

**Addresses:** PREP-01, DUP-01 (FEATURES table stakes)

**Uses:** `jetbrain_search_in_files_by_regex` for Codec+JsonElement import patterns, `ide_search_text` for duplicate signature detection

**Avoids:** Pitfall #1 (misidentifying Definition as Schema duplicate), Pitfall #5 (flagging root-runtime-dependent code for moving to preprocessing)

### Phase 4: CAP-01 — Capability Residual Audit & Final Migration

**Rationale:** Most complex phase — requires complete picture from prior phases to make correct keep/move decisions. ANIM-01 may have changed the animation fileset; PREP-01+DUP-01 results identify migration candidates. The gate criteria is: after this phase, no root `capability/*.java` file is a "data/codec-only" type that belongs in attachment.

**Delivers:** Audit report confirming all 5 v1.4-moved capability types are correctly in attachment, all 8 intentionally-retained root capability types have valid stay rationale. Any residual migrations identified and executed (with build verification after each move). EntityBehaviorData audit with explicit deferral decision for codec extraction.

**Addresses:** CAP-01 (FEATURES table stake) + Optional differentiator: EntityBehaviorData codec assessment

**Avoids:** Pitfall #3 (moving EyelibAttachableData), Pitfall #4 (mechanically extracting EntityBehaviorData codec)

### Phase Ordering Rationale

1. **DOCS first** — zero risk, no compilation impact, establishes documentation accuracy before code changes
2. **ANIM second** — dead code deletion changes the file landscape; must complete before PREP/DUP scans to avoid analyzing code that will be deleted
3. **PREP+DUP third** — read-only analysis; results inform CAP decisions but don't block compilation
4. **CAP last** — complex decisions require all prior phase findings; serves as the final boundary hardening gate

**Why not parallelize ANIM and PREP+DUP?** ANIM deletes files. If PREP/DUP scanner runs in parallel, it may flag files that ANIM concurrently deletes, producing phantom results. Sequential execution ensures PREP/DUP only scans the final ANIM-processed codebase.

**Critical gate after every phase:** `jetbrain_build_project` must pass with exit code 0. For phases involving code movement (CAP-01), also run `jetbrain_run_gradle_tasks taskNames: [":nullawayMain"]`.

### Research Flags

**Phases needing deeper research during planning (`/gsd-research-phase`):**

- **Phase 4 (CAP-01):** `EntityBehaviorData` codec extraction boundary analysis. Requires understanding the MolangQuery coupling — can the codec be extracted to attachment without breaking variant/markVariant serialization? Also needs Clarify on the extent of codec field consumers across root vs. attachment. **Mitigation:** Defer this to post-v1.5 if analysis reveals tight coupling; treat as a separate investigation spike.

**Phases with well-documented patterns (skip research-phase):**

- **Phase 1 (DOCS-01):** Pure text search-and-replace — well-understood, no domain research needed.
- **Phase 2 (ANIM-01):** IDE Find References is a mechanical operation — the research files provide the exact file list and integration points to verify. Pattern: "run references, check against known-active list, delete if zero."
- **Phase 3 (PREP-01 + DUP-01):** Pattern scanning with documented import signatures — PREP searches for `Codec`+`JsonElement` co-imports, DUP compares type signatures across modules. Both are mechanical with clear criteria.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **HIGH** | All 4 researchers confirmed from `build.gradle`, `settings.gradle`, `MODULES.md`, and `PROJECT.md`. Java 17, Forge 1.20.1, MDGL 2.0.91 are project constraints verified by source inspection. |
| Features | **HIGH** | All 5 cleanup tasks directly mapped to PROJECT.md v1.5 Active Requirements. Complexity estimates validated against ARCHITECTURE's integration point analysis and PITFALLS' phase-specific warnings. |
| Architecture | **HIGH** | Component boundaries, data flows, and namespace mappings verified via IDE reference analysis on all 38 integration points. 3 architectural patterns confirmed consistent across root and all 7 submodules. Namespace mapping table in ARCHITECTURE.md provides itemized keep/move/delete decisions. |
| Pitfalls | **HIGH** | All 8 pitfalls derived from v1.0–v1.4 milestone patterns (documented in MODULES.md, boundary docs, and PROJECT.md). Each pitfall has a concrete prevention rule, detection heuristic, and phase mapping. Verified against IDE reference analysis on active integration points. |

**Overall confidence:** **HIGH** — All findings are source-verified (IDE reference analysis, code inspection, architecture docs). No external documentation dependency. No areas received LOW confidence from any researcher.

### Gaps to Address

These areas couldn't be conclusively resolved during research and need attention during planning or execution:

| Gap | How to Handle | Phase |
|-----|---------------|-------|
| **EntityBehaviorData codec boundary** | Codec contains MolangQuery-coupled fields. ARCHITECTURE and PITFALLS agree the extraction decision requires dedicated analysis. **Recommendation:** CAP-01 produces an audit report with explicit deferral. Post-v1.5 spike (`/gsd-spike`) investigates codec extraction feasibility. | CAP-01 (defer decision) |
| **Exact count of dead interfaces in `client/animation/`** | Research confirmed the methodology (per-file IDE Find References) and the list of known-active files. The actual count of zero-reference files can only be determined by running the references during ANIM-01 execution. Research provides the approach, execution provides the answer. | ANIM-01 (execution-time discovery) |
| **Potential residual code in root `util/`** | The `util/README.md` reports "no Java source remains after Phase 19." ARCHITECTURE flags `client/particle/bedrock/` as drained. Verify these assertions hold in the current codebase state — they may have drifted since the README was written. | PREP-01 (scan during execution) |

## Sources

All sources are internal to the repository (no external documentation dependence):

### Primary (HIGH confidence)
- `PROJECT.md` — v1.5 milestone scope, active requirements (CAP-01, ANIM-01, PREP-01, DUP-01, DOCS-01)
- `MODULES.md` — canonical module inventory, boundary rules, v1.0–v1.4 separation history
- `docs/architecture/01-module-boundaries.md` — current→target ownership mapping, module dependency graph
- `docs/architecture/02-side-boundaries.md` — side constraints (no new submodules, no reverse deps)
- `build.gradle` + `settings.gradle` — dependency declarations, module topology (lines 148-170, 16-22)
- IDE reference analysis — 38 integration point verifications across all modules

### Secondary (MEDIUM confidence)
- `eyelib-molang/ROADMAP.md` — MoLang refactor phase status (cross-referenced to verify no overlap with v1.5)
- Package-level README.md files — documentation accuracy verification for DOCS-01
- v1.0–v1.4 milestone patterns — pitfall provenance documented in PITFALLS.md sources

### Tertiary (LOW confidence)
- None — all findings are source-verified; no external documentation or inference-based conclusions.

---

*Research completed: 2026-05-12*
*Ready for roadmap: yes — all 5 cleanup tasks have clear execution paths, phase ordering, and pitfall mitigations.*
