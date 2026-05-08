# client-smoke-test

**Status:** v1.0 shipped (2026-05-07) | v1.1 shipped (2026-05-08)

## What This Is

A standalone NeoForge mod subproject that provides automated client-side smoke testing for Minecraft mods. Tests are discovered via a `@ClientSmoke` annotation, keeping test infrastructure decoupled from mod business code. v1.1 delivers one-command execution: `./gradlew runClientSmoke` launches Minecraft with smoke testing auto-enabled and auto-exits after report generation. While built within the eyelib repository, it is an independent module intended for use by any NeoForge mod.

## Core Value

`./gradlew runClientSmoke` 一键启动全流程，零手动配置。`@ClientSmoke` 注解驱动的客户端冒烟测试，通过编译时/类加载时的注解扫描分离测试基础设施与模组运行时，杜绝意外的类加载问题。

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

None — all requirements shipped. Run `/gsd-new-milestone` to define next milestone.

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

## Constraints

- **Tech stack**: Java 17, Forge 1.20.1, MDGL (ModDevGradleLegacy)
- **Module structure**: Standard Forge Gradle subproject alongside root and other modules
- **Compatibility**: Must coexist with eyelib root module in same Gradle build
- **Class loading safety**: Annotation scanning within safe boundaries
- **Runtime**: Client only (no server components)

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

## Evolution

This document evolves at phase transitions and milestone boundaries.

---

*Last updated: 2026-05-08 after v1.1 milestone*
