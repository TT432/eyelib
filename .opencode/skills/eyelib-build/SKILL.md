---
name: eyelib-build
description: Eyelib 构建、测试、环境——Gradle(Stonecutter 多版本) + eyelib-debug MCP 全流程。Use when building, testing, or troubleshooting Gradle builds for the eyelib project.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "2.0.0"
  tags: eyelib, build, gradle, java, stonecutter
  related-skills: eyelib, eyelib-debug
---

# Eyelib 构建与测试

项目根 `E:\_ideaProjects\qylEyelib`。单 Gradle project(ADR-0014 flat-merge),Stonecutter 0.9.6 多版本(节点 `1.20.1`/`1.21.1`/`26.1.2`),active = `1.20.1`(legacyforge,Java 17)。node `1.21.1`/`26.1.2` 用 NeoForge 21.1.x / 26.1.2.x。

## 工具链与硬约束

- **Gradle 调用方式**: 编译/测试/NullAway/clientsmoke/启停客户端统一走 `eyelib-debug` MCP(`eyelib_debug_build`、`eyelib_debug_test`、`eyelib_debug_nullaway`、`eyelib_debug_clientsmoke`、`eyelib_debug_launch`、`eyelib_debug_close`)；任意其它 Gradle task(generateModulesMd、compileJava、各 node test、Gradle sync)通过 bash 跑 `gradlew`。
- **唯一允许的 IDE**: IntelliJ IDEA。JDTLS / VS Code / Eclipse 全部禁用。
- **Stonecutter 多版本**:
  - `build.gradle` 是 `centralScript`,每个 version node(`:1.20.1`、`:1.21.1`、`:26.1.2`)都跑一次。版本特定代码用 `//?` 注释切分,放在 `versions/<mc-version>/` 下。
  - active version 在 `stonecutter.gradle` 里(`stonecutter.active '1.20.1'`)。跑 task 用 node 前缀:`:1.20.1:test`、`:1.20.1:generateModulesMd` 等。
  - **切 active version 后必须在 IDEA 里 Gradle sync(reimport)**,否则 source set 显示错位。

## 常用任务

| 任务 | 调用 |
|---|---|
| 编译 active node | `eyelib_debug_build`(或 bash `gradlew compileJava`) |
| 编译指定 node | bash `gradlew :1.21.1:compileJava` |
| 测试 active node | `eyelib_debug_test`(等同 `:1.20.1:test`) |
| 重生成模块清单 | bash `gradlew :1.20.1:generateModulesMd`(详见 AGENTS.md 文档同步规则) |
| 启动客户端 | `eyelib_debug_launch` —— **启动前必须确认端口 25999 未被占用**(eyelib-debug SKILL) |
| 关停客户端 | `eyelib_debug_close`,或 `/eval` → `minecraft.stop()`。**禁止从 shell `kill` java 进程** |
| NullAway 检查 | `eyelib_debug_nullaway` |

## 关键约束

- **`FROM-CACHE` / `UP-TO-DATE` 是可接受的**: Gradle 输入跟踪可靠。仅当你改了测试源但 Gradle 报 UP-TO-DATE 时,清掉对应模块的 `build/` 目录再跑。
- **禁止 `--no-build-cache`**: 会强制 MC Forge artifacts 全量重建。需要清缓存时只清相关模块的 `build/`。
- **结构/代码改动**: 完成后必须 `eyelib_debug_build`,exit code = 0 才算完成。
- **runtime-sensitive 改动**: 先编译,再用 dev client(eyelib-debug)做 smoke check。
- **`build.gradle` 改动后**: 必须在 IDEA 里 Gradle sync(reimport) 让 IDE 重新感知。

## Common Pitfalls

### 修改 build.gradle 后 IDE 不感知

`build.gradle` / `settings.gradle` / `stonecutter.gradle` 改动后,IDE 不会自动重读。必须在 IDEA 里手动 Gradle sync(reimport),否则 IDE 显示的依赖图、source set、模块结构都是旧的。

### 切 Stonecutter active version 后看不到新源码

`stonecutter.active` 改了之后,IDE source set 还是上次的。流程:
1. 改 `stonecutter.gradle` 里的 active。
2. 在 IDEA 里 Gradle sync(reimport)。
3. 等同步完成再读 source。

### 非 Mod 库运行时 ClassNotFoundException

`implementation 'group:artifact:version'` 添加的第三方库编译通过但运行时找不到。Forge 运行时 classpath 机制不同,`implementation` 仅加入编译 classpath。

```groovy
// 正确做法:同时加入编译和运行时 classpath
additionalRuntimeClasspath(implementation('group:artifact:version'))
```

缺了里层 `implementation()` 只能上运行时 classpath,编译报包不存在。参考 `build.gradle` dependencies 段。

### 资源路径硬编码 "eyelib/" 前缀

`BrResourcesLoader` 构造函数硬编码 `"eyelib/"` 前缀,导致资源必须放在 `assets/<namespace>/eyelib/<type>/` 路径下。路径模板 `assets/<namespace>/eyelib/<resource_type>/<filename>.json`。本 mod 内部资源放 `assets/eyelib/eyelib/<type>/`,外部资源放 `assets/<your-namespace>/eyelib/<type>/`。

### eyelib_debug_build 报错被截断拿不到编译错误

`eyelib_debug_build` MCP 工具失败时只返回状态(如 "BUILD FAILED in 6m 6s"),错误行被截断到 20 行且按字符串匹配 `error:` 过滤,中文/非标准输出会漏掉。

实际完整 Gradle 输出已落盘到三处(每次 build 覆盖):
- `build/_mcp_gradle_out.txt` — Gradle stdout(任务执行日志 + 编译错误)
- `build/_mcp_gradle_err.txt` — Gradle stderr(JVM warning + 编译错误回显)
- `build/_mcp_gradle.log` — MCP 自己的运行日志(每次调用的 task 列表 + 耗时)

`build` 失败时直接 `read` 这两个文件即可拿到完整错误。这是 eyelib-debug mcp(`scripts/eyelib_debug_mcp.py` `_run_gradle_sync`)写盘的实现细节。

> 若用 bash 跑 `gradlew`,输出直接在终端,不会落盘到上述文件——两者执行链路不同。

### Stonecutter `//?` 注释语法踩坑

centralScript 模式下 `//?` 版本条件注释的实战限制(官方语法文档未明确):

- **闭合标记必须 `//?}`**:块条件 `//? if <1.20.6 { ... //?} else { ... //?}`,闭合不能用 `//}`。
- **行条件不支持 else**:`//? if <1.20.6\n<代码>\n//?} else` 是错误语法,需要 else 时必须用块条件。
- **`//?` 块内禁放纯注释**:Stonecutter 在 else 分支激活时会剥离注释包裹,纯注释变成裸文本导致编译错误。块内需要占位时用 `throw new UnsupportedOperationException("...")`。
- **sourceSet 需手动替换**:Stonecutter 0.7.x centralScript 不自动替换 sourceSet,需在 `build.gradle` 手动 `sourceSets.main.java.srcDirs = [stonecutterGenerated]`。
