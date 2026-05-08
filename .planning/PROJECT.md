# Eyelib Module Separation

**Status:** v1.0 shipped (2026-05-07) | v1.1 shipped (2026-05-08) | v1.2 planning

## What This Is

Eyelib is a multi-project Forge rendering library whose runtime, importer, processor, Molang, material, attachment, and smoke-test seams are being separated into explicit Gradle modules. This milestone focuses on making the particle system a real module boundary instead of a root-runtime package cluster with mixed schema, runtime, registry, networking, and platform integration concerns.

## Core Value

Eyelib 的功能模块必须能被独立理解、构建、验证和消费；粒子拆分必须形成清晰 Gradle 模块边界，同时保持现有加载、命令、网络同步、渲染行为零回归。

## Current Milestone: v1.2 真正实现 eyelib-particle 的模块分离

**Goal:** 将粒子相关能力从 root runtime 的混合包结构中提升为清晰的 `eyelib-particle` Gradle 模块边界，同时保持现有粒子加载、命令、网络 spawn/remove、渲染行为零回归。

**Target features:**
- 新增并接入 `:eyelib-particle` Gradle 模块，root 通过项目依赖消费粒子能力。
- 梳理粒子 schema/importer/runtime 的职责边界，避免把纯粒子核心、导入模型、MC/Forge 绑定混在同一层。
- 清理当前粒子路径中的边界泄漏：平台类型、loader/manager 直接耦合、packet/command/runtime 互相穿透。
- 保持现有行为兼容：资源重载、粒子管理器、`/eyelib particle` 命令、Spawn/Remove packet、客户端发射器渲染不退化。

## Requirements

### Validated

All v1.0 requirements validated (2026-05-07). See `.planning/milestones/v1.0-REQUIREMENTS.md`.

All v1.1 requirements validated (2026-05-08). See `.planning/milestones/v1.1-REQUIREMENTS.md`.

- ✓ Gradle 任务一键启动 clientsmoke — v1.1 (GRAD-01)
- ✓ 任务自动注入 JVM 参数（强制启用、自动退出） — v1.1 (OVRD-03)
- ✓ System property override bridge with ForgeConfigSpec fallback — v1.1 (OVRD-01, OVRD-02)
- ✓ State machine handles empty test sets + JUnit XML + conditional exit code — v1.1 (CORR-01, CORR-02, OVRD-04)
- ✓ runClient zero regression, static verification — v1.1 (CORR-03, CORR-04)
- ✓ Isolated game directory and unconditional classpath — v1.1 (GRAD-02, GRAD-03, GRAD-04)

### Active

- [ ] `:eyelib-particle` exists as a Gradle module with documented responsibility and dependency direction.
- [ ] Particle schema/importer-facing data, particle runtime, and platform integration boundaries are explicit and do not duplicate ownership silently.
- [ ] Root runtime consumes particle capabilities through narrow module seams instead of owning particle internals directly.
- [ ] Existing particle loading, manager publication, `/eyelib particle` command, spawn/remove packets, and client rendering behavior continue to work.

### Out of Scope

- 服务端冒烟测试 — 专注于客户端场景
- 自动断言/回归比对 — v1 仅截图，人工验证
- CI 集成脚本 — v1 仅本地运行
- Windows hardware exit code capture — deferred to manual verification (see `.planning/phases/07-verification-polish/07-02-HARDWARE-CHECKLIST.md`)

## Context

v1.1 shipped 2026-05-08 with 3 phases (5 plans, 8 tasks):
- **Phase 5:** Gradle run config with unconditional localRuntime, isolated gameDirectory, .gitignore
- **Phase 6:** System property override bridge, state machine fixes, JUnit XML, conditional exit code
- **Phase 7:** 33 static verification tests, hardware checklist

Build system: Gradle + Java 17 + Forge 1.20.1 + MDGL (ModDevGradleLegacy).
All Gradle commands via JetBrains MCP.

3325 total lines across 33 static regression tests. Clean build (zero errors).

Deferred hardware verification items (CORR-03, CORR-04) available via:
`.planning/phases/07-verification-polish/07-02-HARDWARE-CHECKLIST.md`

v1.2 starts from the current Eyelib module split:
- Existing Gradle subprojects include `:eyelib-attachment`, `:eyelib-importer`, `:eyelib-material`, `:eyelib-molang`, and `:eyelib-processor`.
- Current particle runtime lives under `src/main/java/io/github/tt432/eyelib/client/particle/` with lookup/spawn seams and Bedrock runtime classes.
- Importer already owns a particle schema record at `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`, creating a visible schema/runtime split pressure point.
- Existing particle command and packet integration lives under `mc/impl` packages and must remain behavior-compatible while module ownership is clarified.

## Constraints

- **Tech stack**: Java 17, Forge 1.20.1, MDGL (ModDevGradleLegacy)
- **Module structure**: Standard Forge Gradle subproject alongside root and other modules
- **Compatibility**: Must coexist with eyelib root module in same Gradle build
- **Class loading safety**: Annotation scanning within safe boundaries
- **Runtime**: Client only (no server components)
- **Particle split**: Platform bindings may live in the most appropriate integration layer, but must not contaminate pure particle core ownership.
- **Regression policy**: Existing particle resource reload, command, packet, spawn/remove, and rendering behavior must not regress during extraction.

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
| `eyelib-particle` as real module boundary | Particle responsibilities are currently spread across root runtime, importer schema, command/network integration, and manager publication | — Pending — v1.2 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---

*Last updated: 2026-05-09 after starting v1.2 milestone*
