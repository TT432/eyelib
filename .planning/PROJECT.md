# client-smoke-test

## What This Is

A standalone NeoForge mod subproject that provides automated client-side smoke testing for Minecraft mods. Tests are discovered via a `@ClientSmoke` annotation, keeping test infrastructure decoupled from mod business code. The framework auto-starts Minecraft, loads a test world, executes annotated test methods, captures screenshots, and exits — all config-driven. While built within the eyelib repository, it is an independent module intended for use by any NeoForge mod.

## Core Value

`@ClientSmoke` 注解驱动的客户端冒烟测试，通过编译时/类加载时的注解扫描分离测试基础设施与模组运行时，杜绝意外的类加载问题。

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] 独立的 `client-smoke-test` Gradle 子项目，使用 NeoForge userdev 插件
- [ ] `@ClientSmoke` 注解定义，标记测试方法或测试类
- [ ] 注解扫描机制，在 NeoForge 类加载安全边界内发现被标记的测试
- [ ] 参考 iris-tutorial-mod 的自动启动机制：配置驱动入口、ClientTickEvent.Pre 状态机、自动进世界截图退出
- [ ] 配置系统（是否启用自动测试、截图延迟、世界名称等）
- [ ] 截图输出与报告生成
- [ ] 与 eyelib root 模块通过 `compileOnly` 或 `runtimeOnly` 依赖，不引入编译时耦合

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
*Last updated: 2026-05-06 after initialization*
