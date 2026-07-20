---
name: mcmcp
description: MC 客户端调试 omp 拓展——读取项目 .mcmcp 启动配置，驱动 clientsmoke mod 内的 AIDebugServer。Use when launching, controlling, or debugging a Minecraft client via mcmcp_* tools, or when setting up .mcmcp in a new project.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: minecraft, debug, omp-extension, mcmcp, clientsmoke
  related-skills: eyelib-debug, eyelib-build
---

# mcmcp — MC 客户端调试 omp 拓展

位置：`.omp/extensions/mcmcp/`（项目级，omp 自动发现 `.omp/extensions`）。
功能：替代旧 `eyelib-debug` Python MCP，工具以 `mcmcp_*` 前缀注册。

## 依赖（使用前必须满足）

1. **clientsmoke mod**：被调试的客户端必须加载含 AIDebugServer 的 clientsmoke mod
   （`io.github.tt432.clientsmoke.debug.AIDebugServer`）。HTTP 端点契约：
   `/ping` `/loaded` `/version` `/eval` `/command` `/enterworld` `/enterdworld` `/close`。
2. **`.mcmcp` 文件**：项目根目录，JSON 对象 `{ "<name>": ["launch_cmd", ...] }`。

```json
{
  "1.20.1": ["cmd.exe", "/c", "versions\\1.20.1\\build\\moddev\\runClient.cmd"],
  "1.21.1": ["cmd.exe", "/c", "versions\\1.21.1\\build\\moddev\\runClient.cmd"],
  "26.1.2": ["cmd.exe", "/c", "versions\\26.1.2\\build\\moddev\\runClient.cmd"]
}
```

- name 任意（版本名、变体名均可）；`mcmcp_launch(version=...)` 按键选择，缺省取第一个键。
- launch_cmd 中 `{port}` 占位符会被替换为实际调试端口。

## 端口机制

- AIDebugServer 读取 JVM 系统属性 `ai_debug_port`（fallback 环境变量 `AI_DEBUG_PORT`）；
  **两者都未配置时默认不开启**（不绑定端口）。
- `mcmcp_launch` 启动子进程时自动注入 `AI_DEBUG_PORT`（默认 25999，可用 `port` 参数覆盖），
  并替换 launch_cmd 里的 `{port}`。
- 手动启动（IDEA、renderdoccmd capture）必须自己传 `-Dai_debug_port=<port>` 或设环境变量，
  否则所有 HTTP 工具不可达。
- 每个工具都有可选 `port` 参数；缺省用最近一次 launch 的端口（初始 25999）。

## 工具

| 工具 | 用途 |
|---|---|
| `mcmcp_launch(version?, timeout=120, port?)` | 按 .mcmcp 启动客户端 → 轮询 /ping /loaded 就绪 |
| `mcmcp_close(port?)` | /close 关停 → 等待端口释放 → 兜底按端口 kill |
| `mcmcp_status(info?, port?)` | 实时查询会话状态（无内部状态机） |
| `mcmcp_enter_world(world_name?, timeout?, port?)` | 进/建超平坦单人世界 → 自动取消暂停 |
| `mcmcp_execute(code, port?)` | /eval 执行 Java 方法体（注入 minecraft/player/level） |
| `mcmcp_send_command(side, command_text, port?)` | slash 命令（client 走网络包 / server 直接执行） |
| `mcmcp_build(version?)` | 编译 + 刷新 ModDevGradle run 产物 ⬥ |
| `mcmcp_test(version?, test_filter?)` | `:{version}:test` ⬥ |
| `mcmcp_nullaway(module?, version?)` | NullAway 检查 ⬥ |
| `mcmcp_clientsmoke(timeout?, version?, port?)` | 重建 → 注入 JVM 参数 → smoke run → 解析报告 ⬥ |

⬥ = 额外要求 Stonecutter + ModDevGradle 布局（项目根 `gradlew.bat`、`versions/<name>/`），
非此类项目会报清晰错误。Gradle 完整输出落盘 `build/_mcp_gradle_out.txt` / `_mcp_gradle_err.txt`。

## 典型流程

```
mcmcp_status(info="all")
mcmcp_launch()                      # 缺省 .mcmcp 第一个键
mcmcp_enter_world()
mcmcp_execute(code='return "hello";')
mcmcp_close()
```

## 跨项目使用

拓展是通用的：把 `.omp/extensions/mcmcp/` 复制到目标项目的 `.omp/extensions/`（项目级），
或放到 `~/.omp/agent/extensions/mcmcp/`（用户级，全项目生效），再在目标项目根写 `.mcmcp`、
确保客户端含 clientsmoke mod 即可。

## 故障排查

- **launch 超时** → 端口被僵尸进程占用：`mcmcp_close()` 后重试。
- **ping 不通、端口空闲** → 客户端启动时没带 `ai_debug_port`（AIDebugServer 默认不开启）。
- **缺 run 产物** → 先 `mcmcp_build`。
- 详见 eyelib-debug SKILL 的 fallback shell 与 mcp-state-recovery 参考文档。
