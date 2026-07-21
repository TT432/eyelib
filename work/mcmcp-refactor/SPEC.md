# mcmcp 重构规格：AIDebugServer 端口参数化 + 迁入 clientsmoke + MCP → omp 拓展

## 需求分解

### R1 AIDebugServer 端口参数化
- 读取系统属性 `ai_debug_port`（JVM `-Dai_debug_port=<port>`）指定监听端口。
- 读不到（未设置）→ **默认不开启**（不绑定任何端口，仅日志说明）。
- 兼容通道：环境变量 `AI_DEBUG_PORT` 作为 fallback（omp 拓展通过 env 向子进程传端口；
  属性优先于 env）。
- 端口值非法（非数字/越界）→ 日志报错且不开启，不杀游戏进程。
- 旧行为 `PORT = 25999` 硬编码 + dev 自动开启 + 启动失败杀客户端，全部移除。

### R2 AIDebugServer 迁入 clientsmoke mod
- 迁移 `io.github.tt432.eyelib.common.debug.{AIDebugServer, ScriptEvalService, RenderDocCapturer}`
  与 `io.github.tt432.eyelib.bridge.client.{DebugServerPort, ClientTaskPort}`
  → `io.github.tt432.clientsmoke.debug`。
- ClientSmokeMod 构造尾调用 `AIDebugServer.startIfConfigured()`。
- eyelib 侧删除：`common/debug/` 整包、bridge/client 两个 Port、Eyelib.java 反射启动块
  （含不再使用的 FMLLoader import）。
- `ClientTaskPort`/`DebugServerPort` 经 grep 确认仅 debug 类使用，随包迁移。
- `RenderDocCapturer` 经 /eval 反射使用（renderdoc skill），必须随迁，FQCN 变更需同步文档。
- `EnvironmentPort` 保留在 eyelib（bridge 通用 Port，非 debug 脚手架）。

### R3 MCP → omp 拓展（mcmcp）
- 删除 `scripts/eyelib_debug_mcp.py`、`scripts/eyelib_debug_mcp.bat`、`.omp/mcp.json`。
- 新建**用户级全局**拓展 `~/.omp/agent/extensions/mcmcp/`（native 自动发现 +
  `omp plugin link` 注册 package root，使同包 `skills/` 被 `omp-plugins` 发现）：
  - `package.json`：`omp.extensions: ["./index.ts"]`，name `omp-mcmcp`
  - `index.ts`：功能对齐旧 MCP 的 10 个工具，命名 `mcmcp_*`：
    launch / close / status / enter_world / execute / send_command / build / test / nullaway / clientsmoke
  - `src/omp.d.ts`：ExtensionAPI 最小子集类型声明
  - `skills/mcmcp/SKILL.md`：同包 skill（clientsmoke 依赖、`.mcmcp` 格式、端口、工具表）
- 拓展读取**当前项目**根目录 `.mcmcp` 文件，JSON 格式 `{ "<name>": ["cmd", "arg1", ...] }`。
  - launch 按 name（如 `"1.20.1"`）取 launch_cmd 启动客户端。
  - 拓展注入 `AI_DEBUG_PORT` 环境变量给子进程；launch_cmd 中 `{port}` 占位符替换为端口。
  - 端口参数：每个工具可选 `port`，默认 25999（与历史文档/fallback 兼容）。
- gradle 系工具（build/test/nullaway/clientsmoke）依赖 Stonecutter+ModDevGradle 布局，
  非此类项目时报清晰错误；保持 `_mcp_gradle_out/_err.txt` 落盘约定。

### R4 skill 与文档
- skill 放在拓展包内 `skills/mcmcp/SKILL.md`（不放项目 `.opencode/skills`）。
- 全仓引用同步：`eyelib_debug_*` → `mcmcp_*`；固定端口 25999 描述 → 参数化端口语义；
  `RenderDocCapturer` FQCN → `io.github.tt432.clientsmoke.debug.RenderDocCapturer`；
  涉及 AGENTS.md、eyelib-debug/eyelib-build/eyelib-clientsmoke/eyelib-renderdoc/
  progressive-exploration/smoke-test/unit-test/mixin-testing 等 skill。
## 非功能性需求
- clientsmoke 三版本节点（1.20.1/1.21.1/26.1.2）源码用既有 `//?` 机制（legacy/modern/raw 谓词）。
- 不引入第二套约定：客户端主线程调度沿用 ClientTaskPort 模式；系统属性读取沿用
  ClientSmokeConfig 的 `System.getProperty` 模式。
- 主项目编译零错误 + NullAway 通过；clientsmoke 编译零错误。

## 前置/后置条件与不变量
- 前置：clientsmoke 已作为主项目 dev runtime mod 加载（localRuntime/copyClientsmokeToMods）。
- 后置：无 `ai_debug_port` 时游戏客户端行为与无 debug server 完全一致（零端口绑定）。
- 不变量：AIDebugServer HTTP 端点契约不变（/ping /loaded /version /eval /command
  /enterworld /enterdworld /close），仅端口来源改变。
- 副作用：clientsmoke jar 内容变化 → 需 `publishToMavenLocal` 后主项目重新解析。

## 验证
1. clientsmoke `:1.20.1:publishToMavenLocal` 成功；主项目 `gradlew compileJava` 0 错误。
2. 拓展 harness（bun 直接 import 工厂 + mock pi）验证 .mcmcp 解析、端口注入、工具注册。
3. E2E：harness 调 mcmcp_launch → /ping /loaded → enter_world → execute → close，
   证明 clientsmoke 内 AIDebugServer 经 env 端口通道工作。
4. 无 `ai_debug_port` 启动 → 25999 无监听（默认不开启）。
5. MODULES.md 重生成（包结构变更）；单元测试全绿。
