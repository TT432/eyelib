# Eyelib Module Separation

**Status:** v1.0–v1.4 shipped | v1.5 in planning (2026-05-12)

## What This Is

Eyelib is a multi-project Forge rendering library whose runtime, importer, processor, Molang, material, attachment, particle, client-smoke, and shared utility seams are being separated into explicit Gradle modules.

## Core Value

Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。

## Current Milestone: v1.5 深度结构清理

**Goal:** 完成上一轮未闭合的结构清理任务——capability 残留审计与终结迁移、animation 无效接口全量清除、preprocessing 归属扫描、重复代码排查、结构文档现代化。

**Target features:**
- capability 残留内容归属审计与最终迁移（root → eyelib-attachment）
- 扫描确认哪些代码应移入 `:eyelib-preprocessing`
- 排查重复职责或意外复制的类/方法
- `client/animation` 下无效接口的彻底清理（含 `bedrock/` 子目录）
- 结构文档（README.md 等）审视与增删改查

## Historical State (v1.4)

v1.4 shipped the 结构清理 milestone on 2026-05-12.

- `eyelib-processor` has been atomically renamed to `:eyelib-preprocessing` (Forge module, `io.github.tt432.eyelibpreprocessing` namespace).
- Bake helpers (5 files) reside in `:eyelib-preprocessing`; controller pure definitions moved to `:eyelib-importer`.
- Capability data/codec types live at `io.github.tt432.eyelibattachment.capability`; runtime owners remain in root with no reverse dependency.
- Dead animation interface (`KeyFrame.java`) removed; instrumentation DB output redirected to `.cache/`.
- All documentation updated to v1.4 topology.
- v1.4 audit passed with 9/9 requirements, 5/5 phases, 25/25 integration checks, and 4/4 E2E flows.
- Deferred scope includes capability runtime owner splitting and stale build artifact cleanup.

## Requirements

### Validated

- ✓ All v1.0 requirements validated (2026-05-07). See `.planning/milestones/v1.0-REQUIREMENTS.md`.
- ✓ All v1.1 requirements validated (2026-05-08). See `.planning/milestones/v1.1-REQUIREMENTS.md`.
- ✓ All v1.2 requirements validated (2026-05-09). See `.planning/milestones/v1.2-REQUIREMENTS.md` and `.planning/milestones/v1.2-MILESTONE-AUDIT.md`.
- ✓ All v1.3 requirements validated (2026-05-10). See `.planning/milestones/v1.3-REQUIREMENTS.md` and `.planning/milestones/v1.3-MILESTONE-AUDIT.md`.
- ✓ All v1.4 requirements validated (2026-05-12). See `.planning/milestones/v1.4-REQUIREMENTS.md` and `.planning/milestones/v1.4-MILESTONE-AUDIT.md`.

### Active

- [ ] **CAP-01**: 审计 root `capability/` 目录残留内容，完成归属决策与最终迁移
- [ ] **ANIM-01**: 清理 `client/animation/` 下无效接口（含 `bedrock/` 子目录）
- [ ] **PREP-01**: 扫描确认哪些代码应移入 `:eyelib-preprocessing`
- [ ] **DUP-01**: 排查重复职责或意外复制的类/方法
- [ ] **DOCS-01**: 审视并更新所有结构文档（README.md 等），删除无效文档

### Out of Scope

- Rewriting existing implementations during module extraction.
- Adding new utility features without a milestone requirement.
- Collapsing extracted modules back into root runtime ownership.

## Archived Milestone Context

<details>
<summary>v1.4 planning context</summary>

**Goal:** 消除残留的非模块化代码放置、纠正命名语义、清理无效接口和过时文档，使模块边界与代码实际归属一致。

**Target features:**
- capability 包内容迁移至 eyelib-attachment
- 重命名 eyelib-processor → eyelib-preprocessing
- 纯数据类（如 BrAcParticleEffectDefinition）归位到正确模块
- 清理 client/animation 下无效接口
- 删除根目录数据库文件创建代码
- client/model/bake 移入预处理模块
- render/controller 基岩版独立控制器的结构分析/拆分
- 重构过时的 README.md（改写为现状描述，无内容的文件夹不保留 README）

</details>

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
| v1.4 Phase ordering | Analysis → Rename → Data → Capability → Docs (research-backed dependency chain) | ✓ Validated — v1.4 |
| Atomic module rename | settings.gradle + build.gradle + .idea/ + directory in one operation before Gradle sync | ✓ Validated — v1.4 |
| Capability split strategy | Data/codec types move to attachment; runtime owners stay in root with distinct namespace | ✓ Validated — v1.4 |
| ModelBakeInvalidationHooks bridge | Prevents preprocessing → root reverse dependency | ✓ Validated — v1.4 |
| Full-suite verification | test + nullawayMain + rebuild replaces standalone Nyquist per phase | ✓ Validated — v1.4 |

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

*Last updated: 2026-05-12 after v1.5 milestone initialization*
