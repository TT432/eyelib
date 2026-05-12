# Technology Stack

**Project:** Eyelib v1.5 深度结构清理
**Researched:** 2026-05-12
**Overall confidence:** HIGH

## Recommended Stack

本次清理工作不需要引入新的技术栈。所有操作在现有工具链内完成。

### Core Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Java | 17 | 语言运行时 | 项目既定约束，Mojang 对 1.20.1 终端用户的要求 |
| Forge | 1.20.1 | Mod 平台 | 项目既定约束 |
| MDGL (ModDevGradleLegacy) | 2.0.91 | Gradle 构建系统 | 当前项目使用的构建插件，不可更改 |

### Existing Modules (Unchanged)

| Module | Gradle Path | Namespace | Role |
|--------|------------|-----------|------|
| Root | `:` | `io.github.tt432.eyelib` | 运行时消费中心、Forge 注册、manager/loader/render 协调 |
| Attachment | `:eyelib-attachment` | `io.github.tt432.eyelibattachment` | 数据/codec 类型所有者、stream codec 工具 |
| Preprocessing | `:eyelib-preprocessing` | `io.github.tt432.eyelibpreprocessing` | Bake helpers、loader parsing、reload planning |
| Importer | `:eyelib-importer` | `io.github.tt432.eyelibimporter` | Bedrock schema/codec 定义、MoLang 兼容值类型 |
| MoLang | `:eyelib-molang` | `io.github.tt432.eyelibmolang` | MoLang 编译/运行时/类型系统 |
| Material | `:eyelib-material` | `io.github.tt432.eyelibmaterial` | Bedrock 材质定义、GL 状态管理 |
| Particle | `:eyelib-particle` | `io.github.tt432.eyelibparticle` | Particle 模块 API、runtime、render manager |
| Utility | `:eyelib-util` | `io.github.tt432.eyelibutil` | 共享工具（codec、streamcodec、collection、math 等） |

### Tools for This Cleanup

| Tool | Purpose | When to Use |
|------|---------|-------------|
| JetBrains MCP `ide_find_references` | 验证某类的所有引用 | ANIM-01 零引用检测 |
| JetBrains MCP `jetbrain_build_project` | 编译验证 | 每阶段完成后 |
| JetBrains MCP `jetbrain_run_gradle_tasks` | 运行 nullawayMain/test | 回归验证 |
| IDE text search (`ide_search_text`) | 搜索字符串/引用 | DOCS-01 旧名引用检测 |
| IDE glob search (`jetbrain_search_file`) | 文件模式搜索 | PREP-01 模式扫描 |
| Context7 / WebSearch | 验证 Forge API 行为 | 仅在需要确认 Forge 契约时 |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Build tool | JetBrains MCP Gradle | Shell `./gradlew` | 项目规则禁止 shell gradle 命令 |
| Code analysis | JetBrains IDE tools | GitNexus | GitNexus 未索引此仓库 |
| Dependency injection | None (manual wiring) | Spring/Guice | 完全不适合 Forge mod 环境 |
| New module creation | NONE (this cleanup) | 新 Gradle 子项目 | v1.5 目标是清理现有结构，不增加新模块 |

## Installation

本阶段不需要新的依赖安装。所有清理操作在现有代码基础上进行。

```bash
# 编译验证（通过 JetBrains MCP，非 shell）
# jetbrain_build_project

# Null-safety 验证
# jetbrain_run_gradle_tasks taskNames: [":nullawayMain"]

# 测试验证
# jetbrain_run_gradle_tasks taskNames: [":test"]
```

## Sources

- `build.gradle` — Root dependency declarations (lines 148-170)
- `settings.gradle` — Module inclusion (lines 16-22)
- `MODULES.md` — 规范模块清单
- `PROJECT.md` — 项目约束（Java 17, Forge 1.20.1, MDGL）
- 所有 confidence: HIGH（来自源码检查）
