# Research Summary: v1.4 结构清理

**Domain:** Multi-module Gradle brownfield structural cleanup
**Researched:** 2026-05-11
**Overall confidence:** HIGH

## Executive Summary

The v1.4 "结构清理" milestone targets 8 structural cleanup operations on a codebase that has already undergone 4 successful module extraction milestones (v1.0-v1.3). The project is a mature multi-module Gradle + Java 17 + Forge repository with 7 submodules plus a composite build. All previous milestones established repeatable patterns: pre-migration audit, scaffold verification, atomic class moves with import updates, and a final verification gate (full build + NullAway + tests + ClientSmoke).

The primary research finding is that **the 8 goals are not independent** — they form a dependency chain where Goal 2 (module rename) must precede Goal 6 (bake migration), and Goal 8 (README rewrite) must be last. Goal 7 (controller analysis) is a two-part design+implementation task that differs fundamentally from the other move/delete goals. Goal 1 (capability migration) carries the highest risk due to bidirectional runtime coupling between capability types and Forge event wiring, network packets, and tests.

The recommended safe execution order separates goals into phases: Analysis (Goals 4, 7) → Rename (Goal 2) → Relocation (Goals 1, 3, 6) → Deletion (Goal 5) → Documentation (Goal 8) → Final Verification. This ordering resolves all known inter-goal dependencies and progressively de-risks the codebase.

## Key Findings

**Stack:** Java 17 + Forge 1.20.1 (LegacyForge/MDGL); Gradle subprojects with varying Forge/plain-JVM profiles; JetBrains MCP for all Gradle operations.

**Architecture:** Root runtime + 7 submodules (attachment, importer, material, molang, particle, processor/util, and composite clientsmoke). Dependency direction is always root → submodule; submodules may consume other submodules but never root.

**Critical pitfall:** Module rename (Goal 2) breaks IDE sync, Gradle resolution, and all cross-module dependency declarations simultaneously — must be performed as a single atomic operation touching settings.gradle, all build.gradle files, .idea XML files, and the directory itself.

## Implications for Roadmap

Based on research, suggested phase structure:

1. **Phase: Analysis & Audit** — Goals 4, 7 (Part A)
   - Addresses: interface cleanup candidates, controller code map
   - Avoids: premature deletion (verify zero references first), committing to split before analysis

2. **Phase: Module Rename** — Goal 2
   - Addresses: eyelib-processor → eyelib-preprocessing rename
   - Avoids: IDE project file staleness (update all .idea XML in same commit), broken Gradle resolution (atomic settings.gradle + all build.gradle edits)

3. **Phase: Code Relocation** — Goals 6, 3, 1
   - Addresses: bake → preprocessing, data classes → correct modules, capability → attachment
   - Avoids: moving into module that will be renamed (do Goal 2 first), capability runtime coupling (split data/codec from Forge wiring)

4. **Phase: Deletion & Cleanup** — Goal 5, Goal 7 (Part B)
   - Addresses: instrumentation database deletion, any controller split execution
   - Avoids: leaving orphaned test files, deleting code with active references

5. **Phase: Documentation & Final Gate** — Goal 8
   - Addresses: README rewrites, MODULES.md updates, boundary doc alignment
   - Avoids: documentation that references stale module topology (always do last)

**Phase ordering rationale:**
- Analysis before action (avoids deleting or moving code without full understanding)
- Rename before relocation (target module names are stable before code moves)
- Simple moves before complex (data classes before capability, which has runtime coupling)
- Deletion after relocation (instrumentation may reference capability code; delete after moves settle)
- Documentation always last (describes final state)

**Research flags for phases:**
- Phase Analysis: Goal 7 (Part A) needs deeper research — controller code is spread across 4 packages and has implicit runtime coupling. A spike may be warranted.
- Phase Relocation: Goal 1 (capability → attachment) needs deeper research on `EyelibAttachableData` bidirectional coupling and network packet dependencies.
- Phase Deletion: Goal 5 needs scope clarification — delete just `db/` or entire `instrument/` subsystem?
- Standard patterns: Goals 2, 3, 6, 4, 8 use established v1.2/v1.3 patterns, unlikely to need additional research.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Based on direct inspection of all `build.gradle`, `settings.gradle`, and `mods.toml` files |
| Features | HIGH | All 8 goals verified against actual source tree and file inventories |
| Architecture | HIGH | Based on MODULES.md, boundary docs, and actual code structure survey |
| Pitfalls | HIGH | Based on 4 prior milestone post-mortems, IDE artifact analysis, and codebase survey. Only Goal 7 (controller analysis) and Goal 1 (capability coupling depth) have residual uncertainty. |

## Gaps to Address

- **Goal 1 scope clarity:** Exactly which capability classes move to `eyelib-attachment`? The PROJECT.md says "capability 包内容迁移" but distinguishing data types from Forge runtime wiring requires per-class analysis.
- **Goal 5 scope clarity:** "删除根目录数据库文件创建代码" — just `InstrumentDatabase.java` + `BackgroundFlushService.java`, or the entire `client/instrument/` subsystem (19 files, 9 tests)?
- **Goal 7 (Part B) feasibility:** Can bedrock controllers be meaningfully split, or is "keep in place" the correct outcome? Analysis phase must answer this before committing to a split.
- **Goal 6 Minecraft dependency audit:** The `client/model/bake/` code has not been fully audited for Minecraft/Forge imports. If present, eyelib-preprocessing must become a Forge module (adding `legacyForge` plugin, `mods.toml`).

**Topics needing phase-specific research later:**
- Per-class capability audit for Goal 1 (which classes are "pure data" vs "runtime coupled")
- Controller code dependency graph for Goal 7 (Part A)
- Bake code Minecraft import audit for Goal 6
