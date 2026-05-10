# Eyelib Module Separation

**Status:** v1.0 shipped (2026-05-07) | v1.1 shipped (2026-05-08) | v1.2 shipped (2026-05-09) | v1.3 planning

## What This Is

Eyelib is a multi-project Forge rendering library whose runtime, importer, processor, Molang, material, attachment, particle, and smoke-test seams are being separated into explicit Gradle modules. This milestone focuses on extracting shared utility code into a dedicated `:eyelib-util` Forge Gradle module, while also discovering shareable code currently siloed in individual submodules that should be centrally available.

## Core Value

Eyelib 的功能模块必须能被独立理解、构建、验证和消费；工具代码共享必须形成清晰 Gradle 模块边界，消除 root util 包集群和子模块间重复的共享代码。

## Current Milestone: v1.3 分离 eyelib-util 模块

**Goal:** 将 root 和 core 下的工具代码及子模块中可中央化的共享代码提升为独立的 `:eyelib-util` Forge Gradle 模块，root/util/* 不留残留。

**Target features:**
- 创建 `:eyelib-util` Forge Gradle 子项目，含 build metadata、mods.toml、source sets。
- root/util/* + core/util/* 全量并入 eyelib-util，root/util/* 不留——单一消费者工具代码移至对应功能 owner。
- 主动扫描子模块（attachment/importer/molang/particle/material/processor），发现可中央化到 eyelib-util 的共享代码（如 attachment 的 streamcodec）。
- 调研受影响模块，由易到难按 phase 推进消解 root 和各子模块对 util 的直接耦合。

## Requirements

### Validated

All v1.0 requirements validated (2026-05-07). See `.planning/milestones/v1.0-REQUIREMENTS.md`.

All v1.1 requirements validated (2026-05-08). See `.planning/milestones/v1.1-REQUIREMENTS.md`.

All v1.2 requirements validated (2026-05-09). See `.planning/REQUIREMENTS.md` (v1.2 section, 18 requirements across 7 phases).

### Active

- [ ] `:eyelib-util` exists as a Forge Gradle module with documented ownership, dependency direction, and build metadata.
- [ ] root/util/* code fully migrated into `:eyelib-util` with compatibility facades where necessary.
- [ ] core/util/* code merged into `:eyelib-util` without duplication.
- [ ] Submodule shared code (e.g. attachment streamcodec) centralized into `:eyelib-util` where appropriate.
- [ ] Single-consumer utility code moved to its functional owner instead of staying in eyelib-util.
- [ ] root and affected submodules consume eyelib-util through explicit project dependencies.
- [ ] root/util/* directory is empty after extraction (no residual code).

### Out of Scope

- Replacing or rewriting existing utility implementations.
- Adding new utility features beyond what already exists.
- Removing MC/Forge dependency from eyelib-util — MC-dependent utilities are valid in this module.

## Context

v1.2 shipped 2026-05-09 with 7 phases (22 plans, 18 requirements):
- **Phase 8:** Boundary Contract & Gradle Module Skeleton for `:eyelib-particle`
- **Phase 9:** Particle API & Store Seam
- **Phase 10:** Schema/Runtime Ownership & Adapter
- **Phase 11:** Runtime Client Core Extraction
- **Phase 12:** Loading & Publication Rewire
- **Phase 13:** Command & Network Integration Rewire
- **Phase 14:** Verification & Documentation Gate

Build system: Gradle + Java 17 + Forge 1.20.1 + MDGL (ModDevGradleLegacy).
All Gradle commands via JetBrains MCP.

v1.3 starts from the current Eyelib module split:
- Existing Gradle subprojects: `:eyelib-attachment`, `:eyelib-importer`, `:eyelib-material`, `:eyelib-molang`, `:eyelib-particle`, `:eyelib-processor`.
- root/util/* contains 34 files across codec, math, search, modbridge, client subareas.
- core/util/* contains 6 platform-free helpers (codec, collection, color, texture, time).
- All submodules and root consume root/util/* directly — extraction requires changing every import site.
- eyelib-attachment's streamcodec helpers are an identified candidate for centralization.

## Constraints

- **Tech stack**: Java 17, Forge 1.20.1, MDGL (ModDevGradleLegacy)
- **Module structure**: Forge Gradle subproject alongside root and other modules
- **Compatibility**: Must coexist with eyelib root module and all submodules in same Gradle build
- **Regression policy**: No functional behavior change — pure ownership transfer and import path rewiring
- **eyelib-util scope**: May depend on MC/Forge; single-consumer code goes to functional owner, not util module

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| @ClientSmoke annotation | Decouple test from business code | ✓ Validated — v1.0 |
| iris-tutorial-mod auto-start pattern | Proven reference implementation | ✓ Validated — v1.0 |
| Independent Gradle subproject | Build isolation | ✓ Validated — v1.0 |
| Runtime dependency, not compile | Prevent framework leakage | ✓ Validated — v1.0 |
| Unconditional localRuntime | No Gradle property required; runtime control via isEnabled() | ✓ Validated — v1.1 |
| System property override bridge | isEnabled()/shouldExitAfterSmoke() check System.getProperty first, ForgeConfigSpec fallback | ✓ Validated — v1.1 |
| JUnit XML alongside JSON | Standard CI integration format | ✓ Validated — v1.1 |
| Conditional halt(0)/halt(1) | Gradle exit code propagation | ✓ Validated — v1.1 |
| `eyelib-particle` as real module boundary | Particle responsibilities spread across root runtime, importer schema, command/network integration, manager publication | ✓ Validated — v1.2 |
| `eyelib-util` as Forge module | May depend on MC/Forge; not artificially constrained to be pure Java | — Pending — v1.3 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---

*Last updated: 2026-05-10 after starting v1.3 milestone*
