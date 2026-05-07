# client-smoke-test

**Status:** v1.0 shipped (2026-05-07) | v1.1 planning

## What This Is

A standalone NeoForge mod subproject that provides automated client-side smoke testing for Minecraft mods. Tests are discovered via a `@ClientSmoke` annotation, keeping test infrastructure decoupled from mod business code. The framework auto-starts Minecraft, loads a test world, executes annotated test constructors, captures screenshots, generates JSON reports, and exits — all config-driven. While built within the eyelib repository, it is an independent module intended for use by any NeoForge mod.

## Current Milestone: v1.1 ClientSmoke 全自动化

**Goal:** 将 clientsmoke 从"需手动配置参数后启动"变为一个 Gradle 任务即可一键运行。

**Target features:**
- 注册 Gradle 任务（如 `runClientSmoke`），一行命令完成全流程
- 基于现有 MDGL (`net.neoforged.moddev.legacyforge`) 机制扩展
- Root 项目可直接调用
- 自动注入启动参数（强制启用 clientsmoke、自动退出）
- 不影响现有 `runClient` 的正常行为

## Core Value

`@ClientSmoke` 注解驱动的客户端冒烟测试，通过编译时/类加载时的注解扫描分离测试基础设施与模组运行时，杜绝意外的类加载问题。

## Requirements

### Validated

All v1 requirements validated (2026-05-07). See `.planning/milestones/v1.0-REQUIREMENTS.md`.

### Active

- [ ] Gradle 任务一键启动 clientsmoke（无需手动设 enableSmokeTest 和 config）
- [ ] 任务自动注入 JVM 参数（强制启用、自动退出）
- [ ] Root 项目可直接调用，不限于子项目
- [ ] 不影响现有 runClient 行为

### Out of Scope

- 服务端冒烟测试 — 专注于客户端场景
- 自动断言/回归比对 — v1 仅截图，人工验证
- CI 集成脚本 — v1 仅本地运行

## Context

本项目基于 eyelib 仓库（Gradle + Java 17 + NeoForge 1.21.1 多项目结构）。仓库已有 `eyelib-molang`、`eyelib-importer`、`eyelib-processor`、`eyelib-material`、`eyelib-attachment` 五个子项目。`client-smoke-test` 作为第六个子项目加入。

参考 iris-tutorial-mod (`E:\____脚本\图形学教学\iris-tutorial-mod`) 的自动启动模式：
- `@Mod(dist = Dist.CLIENT)` 入口 + 构造注入 `IEventBus` / `ModContainer`
- `@EventBusSubscriber` 订阅 `ClientTickEvent.Pre` 驱动状态机
- 配置文件的 `chapter` 字段作为自动模式开关
- 状态机流程：自动创建世界 → 等待加载 → 延迟稳定 → 截图 → 多预设循环 → `Runtime.getRuntime().halt(0)` 退出
- `compileOnly + localRuntime` 依赖管理模式（编译时可见，运行时拉取但不打包）

关键设计决策（来自用户）：
- 使用类似 `@ClientSmoke` 注解实现解耦，避免测试代码与业务代码的类加载交错
- 注解放在独立模块中，测试类通过注解标记，框架扫描发现

## Constraints

- **Tech stack**: Java 17, NeoForge 1.21.1, NeoGradle userdev 插件
- **Module structure**: 标准 NeoForge Gradle 子项目，与现有 `settings.gradle` 的子项目平级
- **Compatibility**: 必须与 eyelib root 模块共存于同一 Gradle 构建（不影响主构建，可选择性加载）
- **Class loading safety**: 注解扫描必须在安全边界内进行，不得导致目标模组的类被提前加载
- **Runtime**: 仅客户端（`Dist.CLIENT`），无服务端组件

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 使用 `@ClientSmoke` 注解 | 解耦测试与业务代码，注解作为 classpath 扫描标记 | — Pending |
| 参考 iris-tutorial-mod 自动启动 | 已验证的模式，成熟可靠 | — Pending |
| 作为独立 Gradle 子项目 | 保持构建隔离，不污染 root 模块的编译类路径 | — Pending |
| Runtime 级依赖而非 compile | 防止测试框架泄漏到主模组的编译产物 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-07 after v1.1 milestone start*
