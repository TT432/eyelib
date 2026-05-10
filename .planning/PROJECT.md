# Eyelib Module Separation

**Status:** v1.0 shipped (2026-05-07) | v1.1 shipped (2026-05-08) | v1.2 shipped (2026-05-09) | v1.3 shipped (2026-05-10) | awaiting next milestone

## What This Is

Eyelib is a multi-project Forge rendering library whose runtime, importer, processor, Molang, material, attachment, particle, client-smoke, and shared utility seams are being separated into explicit Gradle modules.

## Core Value

Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。

## Current State

v1.3 shipped the `:eyelib-util` extraction on 2026-05-10.

- `:eyelib-util` is an active Forge leaf module under `io.github.tt432.eyelibutil`.
- root and approved sibling modules consume shared utilities through explicit Gradle dependencies.
- root `src/main/java/io/github/tt432/eyelib/util/**/*.java` and core `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` contain no Java source after extraction.
- v1.3 audit passed with 15/15 requirements, 7/7 phases, 13/13 integration checks, and 4/4 E2E flows.
- Deferred scope is documented in `.planning/STATE.md`: extended submodule centralization, root dependency-scope narrowing, and SharedLibraryLoader native loading audit.

## Requirements

### Validated

- ✓ All v1.0 requirements validated (2026-05-07). See `.planning/milestones/v1.0-REQUIREMENTS.md`.
- ✓ All v1.1 requirements validated (2026-05-08). See `.planning/milestones/v1.1-REQUIREMENTS.md`.
- ✓ All v1.2 requirements validated (2026-05-09). See `.planning/milestones/v1.2-REQUIREMENTS.md` and `.planning/milestones/v1.2-MILESTONE-AUDIT.md`.
- ✓ All v1.3 requirements validated (2026-05-10). See `.planning/milestones/v1.3-REQUIREMENTS.md` and `.planning/milestones/v1.3-MILESTONE-AUDIT.md`.

### Active

Fresh requirements should be defined by the next `/gsd-new-milestone` run.

### Out of Scope

- Rewriting existing implementations during module extraction.
- Adding new utility features without a milestone requirement.
- Collapsing extracted modules back into root runtime ownership.

## Next Milestone Goals

Define the next milestone from current project state rather than continuing v1.3 requirements in place.

Candidate inputs for `/gsd-new-milestone`:

- Address deferred v1.3 scope if it becomes priority: CENT-F01, CENT-F02, AUDT-F01.
- Continue module-boundary cleanup around remaining root-owned transitional adapters.
- Choose a new product/runtime slice with explicit requirements before planning phases.

## Constraints

- **Tech stack:** Java 17, Forge 1.20.1, MDGL (ModDevGradleLegacy).
- **Gradle policy:** all Gradle commands must run through JetBrains MCP, never shell Gradle.
- **Module structure:** root runtime plus Forge/Java subprojects; preserve extracted module ownership unless a human requests a boundary change.
- **Regression policy:** module moves must preserve behavior unless a requirement explicitly changes it.
- **Utility ownership:** shared helpers belong in `:eyelib-util`; domain-specific helpers belong to the nearest functional owner.

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
| `eyelib-util` as Forge module | May depend on MC/Forge; not artificially constrained to be pure Java | ✓ Validated — v1.3 |
| `io.github.tt432.eyelibutil` namespace | Avoid split packages with root and sibling modules | ✓ Validated — v1.3 |
| Single-consumer utility routing | Domain-specific code belongs to functional owners, not the shared util module | ✓ Validated — v1.3 |

## Archived v1.3 Planning Context

<details>
<summary>v1.3 planning snapshot before archive</summary>

**Goal:** 将 root 和 core 下的工具代码及子模块中可中央化的共享代码提升为独立的 `:eyelib-util` Forge Gradle 模块，root/util/* 不留残留。

**Target features:**

- 创建 `:eyelib-util` Forge Gradle 子项目，含 build metadata、mods.toml、source sets。
- root/util/* + core/util/* 全量并入 eyelib-util，root/util/* 不留——单一消费者工具代码移至对应功能 owner。
- 主动扫描子模块（attachment/importer/molang/particle/material/processor），发现可中央化到 eyelib-util 的共享代码（如 attachment 的 streamcodec）。
- 调研受影响模块，由易到难按 phase 推进消解 root 和各子模块对 util 的直接耦合。

</details>

## Evolution

This document evolves at phase transitions and milestone boundaries.

---

*Last updated: 2026-05-10 after v1.3 milestone completion*
