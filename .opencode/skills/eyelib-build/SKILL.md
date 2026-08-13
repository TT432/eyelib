---
name: eyelib-build
description: Eyelib 构建、测试、环境——Gradle(Stonecutter 多版本) + mcmcp omp 拓展全流程。Use when building, testing, or troubleshooting Gradle builds for the eyelib project.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "2.0.0"
  tags: eyelib, build, gradle, java, stonecutter
  related-skills: eyelib, eyelib-debug
---

# eyelib-build

Eyelib 项目的 Gradle（Stonecutter 多版本）+ mcmcp omp 拓展构建、测试、环境 SOP：构建命令、IDE 约束、Windows/Stonecutter/MDG 实战坑与规避。

## When to use
- 为 eyelib 项目编译、测试、跑 NullAway/clientsmoke、启停客户端
- 排查 eyelib Gradle 构建失败、IDE 不同步、依赖或资源路径问题
- 切换 Stonecutter active version 或修改 build.gradle/settings.gradle/stonecutter.gradle
- Do NOT use when: MC 客户端调试端口与 /eval 运行时操作细节（属 mcmcp、eyelib-debug 技能）

## Rules
- [when 修改了 stonecutter.gradle 的 active version] 切 stonecutter.active 后必须在 IDEA 里 Gradle sync（reimport）并等同步完成再读 source，否则 IDE source set 还是上次的、显示错位。
- [when 修改了构建脚本] build.gradle / settings.gradle / stonecutter.gradle 改动后必须在 IDEA 里手动 Gradle sync（reimport），否则 IDE 显示的依赖图、source set、模块结构都是旧的。
- When 常用任务:
  - 任务调用对照：编译 active node 用 mcmcp_build（或 bash gradlew compileJava）；编译指定 node 用 bash gradlew :1.21.1:compileJava；测试 active node 用 mcmcp_test（等同 :1.20.1:test）；重生成模块清单用 bash gradlew :1.20.1:generateModulesMd；启动客户端用 mcmcp_launch；NullAway 检查用 mcmcp_nullaway。
  - mcmcp_launch 启动客户端前必须确认调试端口未被占用（见 mcmcp 技能）。
  - NEVER 禁止从 shell kill java 进程关停客户端；用 mcmcp_close，或 /eval → minecraft.stop()。
- When 工具链与硬约束:
  - 编译/测试/NullAway/clientsmoke/启停客户端统一走 mcmcp omp 拓展（mcmcp_build、mcmcp_test、mcmcp_nullaway、mcmcp_clientsmoke、mcmcp_launch、mcmcp_close）；其它任意 Gradle task（generateModulesMd、compileJava、各 node test、Gradle sync）通过 bash 跑 gradlew。
  - 唯一允许的 IDE 是 IntelliJ IDEA；JDTLS / VS Code / Eclipse 全部禁用。
  - build.gradle 是 Stonecutter centralScript，每个 version node（:1.20.1、:1.21.1、:26.1.2）都跑一次；版本特定代码用 //? 注释切分，放在 versions/<mc-version>/ 下。
  - active version 在 stonecutter.gradle 里（stonecutter.active '1.20.1'）；跑 task 用 node 前缀，如 :1.20.1:test、:1.20.1:generateModulesMd。
- When 关键约束:
  - FROM-CACHE / UP-TO-DATE 是否可接受取决于验证目标：回归门禁（当前输入下测试通过）可接受，Gradle 输入跟踪可靠；行为证据（新测试首次验证、性能测量、运行时诊断）不可接受——test 任务 FROM-CACHE/UP-TO-DATE 表示本次没有执行任何测试，须清掉对应模块的 build/ 目录再跑并引用真实执行输出。
  - NEVER 禁止 --no-build-cache：会强制 MC Forge artifacts 全量重建；需要清缓存时只清相关模块的 build/。
  - 结构/代码改动完成后必须 mcmcp_build 且 exit code = 0 才算完成。
  - runtime-sensitive 改动先编译，再用 dev client（eyelib-debug）做 smoke check。
- When 本地 jar 依赖：MDG remap 配置不收 files()，mavenLocal 同名替换不生效:
  - NEVER 给 modLdlibCompile/modLocalRuntime 这类 MDG createRemappingConfiguration 生成的配置加本地 jar 时不要用 files(...) 记法——直接报「Cannot convert the provided notation ... DependencyConstraint」，该链只接受字符串/map 坐标。
  - 本地 jar 正确路径：手动装进 ~/.m2/repository/<group>/<name>/<version>/（jar + 最小 pom），用坐标引用；注意 exclusiveContent 组过滤，别用被占用的 group。
  - 同名同版本替换 mavenLocal 里的 jar 内容不会生效：Gradle transform 缓存按输入哈希命中，--refresh-dependencies 也不重算（2026-08-06 实证）；必须换版本号（如 -patched1）再改依赖坐标。
  - 改完坐标后 :<node>:createClientLaunchScript 可能 UP-TO-DATE 不重生成，须确认 versions/<node>/build/moddev/clientRunClasspath.txt 里的 jar 路径已更新，必要时 --rerun-tasks。
- When 非 Mod 库运行时 ClassNotFoundException:
  - implementation 'group:artifact:version' 添加的第三方库仅加入编译 classpath，Forge 运行时 classpath 机制不同，运行时会 ClassNotFoundException；正确做法是同时加入编译和运行时 classpath：additionalRuntimeClasspath(implementation('group:artifact:version'))；缺了里层 implementation() 只能上运行时 classpath，编译报包不存在。
- When 直跑 gradlew --tests 带引号 pattern 报 No tests found:
  - git-bash 里 cmd /c 'gradlew.bat :26.1.2:test --tests "*Foo*"' 时 cmd 不剥离双引号，Gradle 收到带字面引号的 pattern 报 No tests found，极易误判为测试发现机制损坏；规避：用 mcmcp_test（内部传不带引号的 pattern），或直跑时写 --tests *Foo* 不带引号。
- When 资源路径硬编码 "eyelib/" 前缀:
  - BrResourcesLoader 构造函数硬编码 "eyelib/" 前缀，资源必须放在 assets/<namespace>/eyelib/<type>/ 路径下（模板 assets/<namespace>/eyelib/<resource_type>/<filename>.json）；本 mod 内部资源放 assets/eyelib/eyelib/<type>/，外部资源放 assets/<your-namespace>/eyelib/<type>/。
- When mcmcp_build 报错被截断拿不到编译错误:
  - mcmcp_build 失败时只返回状态且错误行被截断到 20 行、按字符串匹配 error: 过滤，中文/非标准输出会漏掉；完整 Gradle 输出每次 build 落盘覆盖到 build/_mcp_gradle_out.txt（stdout：任务日志+编译错误）、build/_mcp_gradle_err.txt（stderr：JVM warning+错误回显）、build/_mcp_gradle.log（mcmcp 拓展运行日志）；build 失败时直接 read 这些文件拿完整错误（mcmcp 拓展 runGradle 写盘实现细节）。
  - 若用 bash 跑 gradlew，输出直接在终端，不会落盘到 _mcp_gradle_*.txt 文件——两者执行链路不同。
- When Stonecutter `//?` 注释语法踩坑:
  - //? 块条件的闭合标记必须 //?}（如 //? if <1.20.6 { ... //?} else { ... //?}），不能用 //}。
  - //? 行条件不支持 else（//? if <1.20.6 后换行再 //?} else 是错误语法），需要 else 时必须用块条件。
  - NEVER //? 块内禁放纯注释：else 分支激活时 Stonecutter 会剥离注释包裹，纯注释变成裸文本导致编译错误；块内需要占位时用 throw new UnsupportedOperationException("...")。
  - Stonecutter 0.7.x centralScript 不自动替换 sourceSet，需在 build.gradle 手动 sourceSets.main.java.srcDirs = [stonecutterGenerated]。

## Workflow
1. 编译 active node：mcmcp_build（或 bash gradlew compileJava）；编译指定 node：bash gradlew :<node>:compileJava。
2. mcmcp_build 失败且错误被截断时，直接 read build/_mcp_gradle_out.txt 与 build/_mcp_gradle_err.txt（必要时 build/_mcp_gradle.log）拿完整编译错误。 [fallback]
3. 测试 active node：mcmcp_test（等同 :1.20.1:test）；NullAway：mcmcp_nullaway；重生成模块清单：bash gradlew :1.20.1:generateModulesMd。
4. 切 active version 流程：1) 改 stonecutter.gradle 里的 active；2) 在 IDEA 里 Gradle sync（reimport）；3) 等同步完成再读 source。
5. 结构/代码改动的完成判定：mcmcp_build exit code = 0；runtime-sensitive 改动另需 dev client smoke check 通过。 [stop]

## Output
- build 结果 (required) — mcmcp_build exit code = 0 为结构/代码改动完成的必要条件
- Stop when: 结构/代码改动以 mcmcp_build exit 0 收尾；runtime-sensitive 改动以 dev client smoke check 通过收尾。

<!-- evidence background: sole source of r-central, r-nodeprefix -->
项目根 `E:\_ideaProjects\qylEyelib`。单 Gradle project(ADR-0014 flat-merge),Stonecutter 0.9.6 多版本(节点 `1.20.1`/`1.21.1`/`26.1.2`),active = `1.20.1`(legacyforge,Java 17)。node `1.21.1`/`26.1.2` 用 NeoForge 21.1.x / 26.1.2.x。

<!-- locked residual (verbatim, do not edit) -->
```groovy
// 正确做法:同时加入编译和运行时 classpath
additionalRuntimeClasspath(implementation('group:artifact:version'))
```
**结构/代码改动**: 完成后必须 `mcmcp_build`,exit code = 0 才算完成。
