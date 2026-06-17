---
name: eyelib-build
description: Eyelib 构建、测试、环境——Gradle/WSL/Windows 交叉编译全流程。Use when building, testing, or troubleshooting Gradle builds for the eyelib project.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, build, gradle, java
  related-skills: eyelib, eyelib-debug
---

# Eyelib 构建与测试

项目路径 `/mnt/e/_ideaProjects/qylEyelib`，MC 1.20.1 / Forge 47.1.3 / Java 17。

## 环境

```bash
source ~/.hermes/profiles/qyleyelib/scripts/env.sh
cd /mnt/e/_ideaProjects/qylEyelib
```

- `JAVA_HOME` = `/root/localjdk`（JDK 21 Temurin）
- `GRADLE_OPTS` 含代理 `127.0.0.1:10808`
- WSL 环境 → **必须用 Windows 侧 gradlew.bat**

## 常用命令

```bash
# 全项目编译
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :compileJava --no-configuration-cache"

# 单模块编译
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:compileJava --no-configuration-cache"

# 单模块测试
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:test --no-configuration-cache"

# 生成启动脚本（供 eyelib_debug_launch 内部使用）
# 仅当 runClient.cmd 不存在时需要
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain createLaunchScripts --no-configuration-cache
# 输出: build/moddev/runClient.cmd
```

## 关键约束

- **WSL Gradle 死慢 → 永远用 Windows gradlew**：`cmd.exe /c "cd /d E:\... && gradlew.bat ..."` 5~20s vs WSL >120s
- **`--no-configuration-cache`**：Forge 构建需要
- **`UP-TO-DATE` 可接受**：Gradle 输入跟踪可靠
- **WSL → Windows 交叉编译**：`patch` 工具修改后 Gradle 可能不检测变更 → 强制 `rm -rf <module>/build/classes && :module:compileJava`
- **子模块类变更后 `compileJava` 不够** — 必须跑 `:module:jar` 或 `createLaunchScripts`。根模块 class 文件被 `runClient.cmd` 的 `MOD_CLASSES` 直接加载，但子模块 class 文件走 `clientRunClasspath.txt` 引用的 pre-built JAR。只 `compileJava` 编译了 `.class` 但没打进 JAR → 运行时 `ClassNotFoundException`
- **接口变更（record 字段增减、方法签名变化）**同样导致 JAR 不匹配——旧 JAR 里的旧接口签名和新编译的实现类不一致 → `NoSuchMethodError`。必须 `:module:jar createLaunchScripts`
- **`createLaunchScripts` 不更新 `build/devlibs/`** — 运行时 classpath 引用 `build/devlibs/` 下的 JAR，而非 `build/libs/`。`:module:jar` 只更新 `build/libs/`，不会自动同步。手动修复：`cp build/libs/<module>.jar build/devlibs/<module>.jar`。验证：对比两个目录下 JAR 的文件大小和时间戳
- **模块变更跑全量测试**：仅跑 `:eyelib-molang:test` 不够 → 根模块 `:test` 可能依赖该模块。任何子模块变更后必须跑 `:test` 全量验证 + `clientsmoke`
- **禁止 `git add -A`**：会污染 `3rdparty/rd_src` 子模块

## TODO 管理 (rusty-todo-md)

从代码中自动提取 `TODO`/`FIXME`/`HACK` 注释生成 `TODO.md`。

```bash
uv tool install rusty-todo-md
cd /mnt/e/_ideaProjects/qylEyelib
export PATH="$HOME/.hermes/profiles/qyleyelib/home/.local/bin:$PATH"
rusty-todo-md -p TODO.md -m TODO FIXME HACK -- <file1> <file2> ...
```

获取所有含 TODO 的文件：
```bash
grep -rln "TODO:" --include="*.java" src/ eyelib-*/src/main/java/ | grep -v "/build/" | grep -v "/generated/"
```

## Common Pitfalls

### WSL Gradle 必须通过 Windows cmd.exe

WSL 内直接跑 `./gradlew.bat` 会报 `syntax error`（bash 无法解析批处理文件），且 WSL Gradle 速度 >120s（vs Windows 5~20s）。**永远用**：

```bash
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :module:task --no-configuration-cache"
```

委托子代理时必须在 prompt 中指明此命令格式，否则子代理会尝试 `./gradlew.bat` 并超时。

### 非 Mod 库运行时 ClassNotFoundException

`implementation 'group:artifact:version'` 添加的第三方库编译通过但运行时找不到。Forge 运行时 classpath 机制不同，`implementation` 仅加入编译 classpath。

```groovy
// 正确做法：同时加入编译和运行时 classpath
additionalRuntimeClasspath(implementation('group:artifact:version'))
```

缺了里层 `implementation()` 只能上运行时 classpath，编译报包不存在。参考 `build.gradle:189-190`。

### 修改 build.gradle 后 Sync 不生效

修改 `build.gradle` 后 IDE 不会自动感知变更。通过 JetBrains MCP 执行 `jetbrain_sync_gradle_projects`，或手动在 IDE Gradle 工具窗口刷新。

### 资源路径硬编码 "eyelib/" 前缀

`BrResourcesLoader` 构造函数硬编码了 `"eyelib/"` 前缀，导致资源必须放在 `assets/<namespace>/eyelib/<type>/` 路径下。路径模板为 `assets/<namespace>/eyelib/<resource_type>/<filename>.json`。本 mod 内部资源放 `assets/eyelib/eyelib/<type>/`，测试或外部资源放 `assets/<your-namespace>/eyelib/<type>/`。
