# ADR-0020: 调试服务器归属与 mcmcp 拓展

**Status:** Accepted  
**Date:** 2026-07-22  
**Author:** @TT432

## Context

客户端调试 HTTP 服务只在 clientsmoke 注入的客户端环境中有意义。旧的项目内 Python MCP 同时承担客户端启动、Gradle 操作和有状态会话管理，固定端口、自动启动和错误时杀客户端等行为把调试脚手架耦合进主模组，也使项目配置和工具发现不一致。

调试端点本身需要保持稳定，但服务的生命周期、端口来源和工具调用应能独立于 eyelib 主模组演进。

## Decision

1. `AIDebugServer`、`ScriptEvalService`、`RenderDocCapturer` 及其客户端任务 Port 归属 `clientsmoke` mod，包名为 `io.github.tt432.clientsmoke.debug`；clientsmoke 构造完成后调用 `startIfConfigured()`。
2. AIDebugServer 读取 JVM 系统属性 `ai_debug_port`；若未设置，再读取环境变量 `AI_DEBUG_PORT`。两者都没有时不启动、不绑定端口；非法或越界值只记录错误并保持关闭。
3. `/ping`、`/loaded`、`/version`、`/eval`、`/command`、`/enterworld`、`/enterdworld`、`/close` 端点契约保持不变，只改变服务归属和端口来源。
4. 客户端启动、构建、测试、NullAway 和 clientsmoke 编排由用户级 `mcmcp` omp 拓展提供。拓展从当前项目根目录读取 `.mcmcp` JSON：`{ "<version>": ["command", "arg", ...] }`；启动时以 `AI_DEBUG_PORT` 注入子进程，并替换命令中的 `{port}`。
5. 项目内 Skill 只描述如何使用该拓展和如何诊断客户端；不得在主项目恢复第二套 Python MCP、固定端口状态机或 debug server 实现。

## Consequences

- eyelib 主模组不再持有调试服务实现或启动反射块；普通客户端没有调试端口时行为与无 debug server 相同。
- clientsmoke 成为调试端点的运行时依赖；其 jar 更新后主项目需要重新解析本地发布物。
- mcmcp 拓展是用户级工具，不随项目源码发布；项目只保留 `.mcmcp` 格式和使用约定。
- 端口默认值由 mcmcp 工具层提供（当前为 25999），服务端本身在无配置时保持关闭；这避免服务端因历史默认值被意外暴露。

## Verification

- 工具和调试流程：[`eyelib-debug`](../../.opencode/skills/eyelib-debug/SKILL.md)、[`eyelib-build`](../../.opencode/skills/eyelib-build/SKILL.md)。
- clientsmoke 端口和测试流程：[`eyelib-clientsmoke`](../../.opencode/skills/eyelib-clientsmoke/SKILL.md)。
- RenderDoc 调试入口：[`eyelib-renderdoc`](../../.opencode/skills/eyelib-renderdoc/SKILL.md)。
