# Eyelib Agent 指南

## 从这里开始

- 阅读 :docs/README.md: 了解完整的文档系统导航。
- 在规划结构性或多模块变更前，阅读 :MODULES.md:。（从 `*/package-info.java` 自动生成；通过 `:generateModulesMd` 重新生成）
- 阅读最近的 `package-info.java` 了解包级别的定位。
- 对于边界决策，阅读 :docs/decisions/0002-module-boundaries.md:。

## 仓库结构

- **实际架构**: 六边形(Ports & Adapters) + 包边界模块化单体 + ECS + Stonecutter 多版本。**不是 DDD** —— 无
  Aggregate/Repository/Domain Event 等战术模式;是 Bedrock 规范复刻,战略 DDD 的限界上下文思想体现在包边界上。详见 :
  docs/decisions/0002-module-boundaries.md:。
- **构建模型**: 单 Gradle project(:docs/decisions/0014-flat-merge.md:),通过 :Stonecutter: `centralScript` 同时维护多版本
  MC(注册 `1.20.1`(legacyforge,active)、`1.21.1`(neoforge)、`26.1.2` 三节点,详见 :
  docs/decisions/0015-stonecutter-multi-version.md:)。`clientsmoke/` 是 composite build(烟雾测试框架),不属于主 project。
- **源码布局**: 全部源码在 `src/main/java/io/github/tt432/eyelib/<module>/`,包边界(`io.github.tt432.eyelib.<module>`)
  定义真实架构。模块包不得依赖 root 编排包(`io.github.tt432.eyelib.client`、`io.github.tt432.eyelib.common`)。
- **模块清单**: :MODULES.md: 由 `:generateModulesMd` 从 `src/main/java/io/github/tt432/eyelib/*/package-info.java` *
  *自动生成**,不得手编。
- **核心模式**: 保留现有的 manager / loader / visitor / codec 模式。

## 编辑规则

- 不要碰无关的未提交变更。
- 优先做窄幅编辑，避免大范围的包结构调整。
- 在未先文档化目标职责前，不要向模糊区域添加代码。
- 每次变更前，确认会影响 :MODULES.md: 中的哪些模块。
- 模块职责定义在每个 `package-info.java` 的第一个 Javadoc 段落中。要更新 MODULES.md，编辑相关 `package-info.java` 并运行
  `:1.20.1:generateModulesMd`(bash `gradlew :1.20.1:generateModulesMd`)。永远不要手动编辑 MODULES.md。
- 如果新增或删除了模块（`eyelib/` 下新的顶层包），必须提供其 `package-info.java` —— `:generateModulesMd`
  在缺少文件时会失败。在同一个变更中更新所有受影响的文档。

## 注释规则

### 基本原则

- **统一使用中文**。
- 注释的价值在于**信息增量** — 删掉后别人会误解或犯错时才需要。

### `@NullMarked`

- **只在 `package-info.java` 加 `@NullMarked`**。类/接口/枚举/record 声明前**不加**（`package-info.java` 已覆盖整个包，类级是冗余）。
- NullAway 通过 `OnlyNullMarked=true` + `RequireExplicitNullMarking` checker 强制此约定。新包必须先建 `package-info.java`
  ，否则该包内的类会被 NullAway 报错。

### package-info.java

- 每个包含 `.java` 文件的包都必须有一份。统一格式：

```java
/**
 * 一句话说明包的职责（中文）。
 */
@NullMarked
package io.github.tt432.eyelib.<module>;

import org.jspecify.annotations.NullMarked;
```

- **关键**：`import` 必须在 `package` 声明之后（javac 强制要求），Javadoc 必须在最顶部。

- 不再单独维护包级 README.md。包的知识分层如下：
    - **公共 API 的 Javadoc** → 类/方法签名自带
    - **包定位** → `package-info.java` 一句话
    - **架构决策** → `docs/decisions/`（为什么这么设计）
    - **工作导航** → `AGENTS.md`（怎么看这个项目）

### 注释规则

- 对内部实现：注释记录**设计决策**而不是**规格文档**。
- 对外部接口：注释记录**规格文档**而不是**设计决策**。

### TODO

- 统一格式：`// TODO: 中文描述`
- 当执行过程中需要延迟实现则使用 TODO 标记。
- 禁止：`todo`（小写无冒号）、`TODO:fix`、`TODO(Phase N)`、Javadoc 中嵌 TODO。

### 自检清单

写注释前依次确认：

1. 删掉后别人会误解吗？→ 不会 → 删
2. 信息已经存在于类型签名/命名里吗？→ 是 → 删
3. 半年后我自己还需要这个信息吗？→ 不需要 → 删
4. 能用一句话说清吗？→ 不能 → 先重构代码，代码本身比注释好

## 文档规则

- **路径必须可解析。** 文档中引用的每个文件路径都必须存在。如果引用的文件被删除或移动，更新或删除该引用。
- **不要在活跃文档中保留历史。** 已完成的任务、已解决的问题和历史中间状态属于 git 历史，不属于当前状态文档。
- **架构决策放在 `docs/decisions/`。** 每个文件是一个 ADR：上下文 → 决策 → 后果。不包含实现细节。
- **代码是权威参考。** 包结构、类名和方法签名是"有什么"的真相来源。Bedrock 标准和外部格式规范直接引用其规范来源（E 盘
  Bedrock 文档、Mojang Creator 文档）。
- **关于依赖的说法必须与 `build.gradle` 一致。** Gradle 依赖图是唯一的真相来源。
- **纯文档变更：** 提交前验证每个引用的文件路径都能解析。
- **结构/代码变更：** 通过 `eyelib_debug_build` 构建，要求退出码 :0: 才能声称完成。
- **运行时敏感变更：** 先编译，然后用现有的开发客户端流程进行冒烟检查。

## 文档同步规则

代码改动类型 → 必须同步的文档:

| 代码改动                                                        | 必须同步检查                                                                                                         |
|-------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| 新增/删除/重命名包                                                  | 该包 `package-info.java`、`:MODULES.md:`(跑 `:generateModulesMd` 重生成)、`:docs/decisions/0002-module-boundaries.md:` |
| 新增/删除/重命名顶层模块                                               | `:MODULES.md:`、`:docs/README.md:`、AGENTS.md Repository Shape                                                   |
| 新增 ADR                                                      | `:docs/README.md:` ADR 索引、被修订的旧 ADR 头部加 `amended/superseded by` 标注                                             |
| 改 `build.gradle` 依赖图 / `settings.gradle` / Stonecutter node | AGENTS.md Tooling Restrictions、`:docs/README.md:`(若结构变)                                                        |
| Molang 阶段/里程碑/闸门变化                                          | `:docs/molang/ROADMAP.md:`                                                                                     |
| 删除/重命名文件被 docs 引用                                           | grep 全 `docs/` + `AGENTS.md` + `MODULES.md` + 所有 `SKILL.md`                                                    |
| 新增/删除 Skill                                                 | `:docs/README.md:` Skill 索引、AGENTS.md Skill Usage                                                              |

提交前自检:

1. 改动是否触发了上表任何一行? → 是 → 必须同步
2. 同步后,grep 全仓库验证没有旧路径残留
3. 文档-only PR:必须 grep 验证每个引用路径存在

## 工具限制

- IntelliJ IDEA 是唯一的 IDE。严禁提交 VS Code 和 Eclipse 产物。
- **JDTLS 被明确禁止。**
- **JetBrains MCP / ide-index MCP 已废弃。** 构建、测试、调试、客户端启停统一通过 `eyelib-debug` MCP
  （`eyelib_debug_build` / `eyelib_debug_test` / `eyelib_debug_nullaway` / `eyelib_debug_clientsmoke` /
  `eyelib_debug_launch` / `eyelib_debug_close` / `eyelib_debug_execute` / `eyelib_debug_send_command`）。
- **Gradle 执行**: `eyelib-debug` MCP 覆盖编译/测试/clientsmoke；任意其它 Gradle task（如 `:1.20.1:generateModulesMd`、
  `compileJava`、各 node test）及 Gradle sync 通过 bash 跑 `gradlew`。
- **Stonecutter 多版本**:
    - `build.gradle` 是 `centralScript`,每个 version node(`:1.20.1`、`:1.21.1`、`:26.1.2`)都跑一次。版本特定代码用 `//?`
      注释切分,放在 `versions/<mc-version>/` 下。
    - active version 在 `stonecutter.gradle` 里(`stonecutter.active '1.20.1'`)。跑 task 用 node 前缀:`:1.20.1:test`、
      `:1.20.1:generateModulesMd` 等。
    - 切 active version 后必须在 IDEA 里 Gradle sync(reimport),否则 source set 显示错位。
- **游戏重启**: 通过 debug HTTP `/eval` → `minecraft.stop()` 关闭运行中的客户端,切勿从 shell `kill` java 进程。
- **游戏启动**: `eyelib_debug_launch`。启动前检查端口 25999 是否空闲；若被占用，先 `eyelib_debug_close` 关闭旧实例。

## 构建与测试验证

- **`FROM-CACHE` / `UP-TO-DATE` 是可接受的。** 如果 Gradle `test` 任务显示 `UP-TO-DATE`
  ，表示测试已在相同源码上运行过——构建系统正确地跟踪了输入变更。仅在你故意更改了测试源码而 Gradle 忽略此变更时，才清理受影响模块的
  `build/` 目录。
- **切勿向 Gradle 传递 `--no-build-cache`。** 它会强制完全重建 MC Forge 产物。应使用有针对性的缓存清理。
- `eyelib_debug_build` 编译源码。对于依赖重混淆 JAR 的运行时验证，使用 `eyelib_debug_launch` 启动客户端。

## 完整验证流程

按改动类型选择对应闸门。"声称完成"前必须通过对应闸门，未通过的改动不算完成。工具调用细节见 `eyelib-build` SKILL。

### 闸门矩阵

| 改动类型                         | 编译 | NullAway | 单元测试 | MODULES.md | 文档同步 | clientsmoke |
|--------------------------------|:---:|:---:|:---:|:---:|:---:|:---:|
| 纯文档                          | — | — | — | — | ✓ grep | — |
| 代码（不动包结构）                 | ✓ | ✓ | ✓ | — | 触发则同步 | — |
| 代码（新增/删除/重命名 包或顶层模块） | ✓ | ✓ | ✓ | ✓ 重生成 | ✓ | — |
| 运行时敏感（渲染/资源/网络/Mixin） | ✓ | ✓ | ✓ | 触发则同步 | ✓ | ✓ |

### 各闸门执行方式

1. **编译**: `eyelib_debug_build`。构建失败时直接读 `build/_mcp_gradle_out.txt` / `build/_mcp_gradle_err.txt`
   拿完整错误(见 `eyelib-build` SKILL 的"报错被截断"陷阱)。
2. **NullAway / Error Prone**: `eyelib_debug_nullaway`。强制 `@NullMarked` 约定(只在 `package-info.java` 加)与
   null safety，不通过说明包边界/可空性出了问题，**禁止用加注解的方式静默消除报错**，先查根因。
3. **单元测试**: `eyelib_debug_test`(等同 `:1.20.1:test`)。跨版本改动时按各 node 分别跑(`:1.20.1:test`、
   `:1.21.1:test`)。测试失败先 `git stash` 复现，排除预存失败(见 `eyelib` SKILL 跨域约束)。
4. **MODULES.md**: 仅在包/模块结构变更时执行 `:1.20.1:generateModulesMd`(bash `gradlew :1.20.1:generateModulesMd`)
   重生成，**禁止手编**，产物需随改动一起提交。
5. **文档同步**: 见上文"文档同步规则"，grep 全仓库验证无旧路径残留、所有引用路径可解析。
6. **clientsmoke**: `eyelib_debug_clientsmoke`。验证 MC 客户端加载后的接线行为(Bridge/接线层)，报告输出到
   `run/clientsmoke-reports/`。写法见 `eyelib-clientsmoke` SKILL。

### 提交前 Checklist（代码/结构/运行时变更）

- [ ] `eyelib_debug_build` 退出码 0
- [ ] `eyelib_debug_nullaway` 无报错
- [ ] `eyelib_debug_test` 全绿(跨版本改动则各 node 分别全绿)
- [ ] 包/模块结构变更 → `:1.20.1:generateModulesMd` 已重生成且随改动提交
- [ ] 触发"文档同步规则"任一行 → grep 验证无旧路径残留
- [ ] 运行时敏感 → `eyelib_debug_clientsmoke` 全绿

## Skill 使用

- Skill 是模块化且聚焦的——一个 skill 涵盖一个工作流领域。不要把所有内容塞进一个 skill。
- 当发现重复出现的陷阱或工作流时，决定它属于哪个 skill 领域（构建、调试、renderdoc、clientsmoke）。如果没有现有的 skill 适合，创建一个新的聚焦 skill。
- Skill 必须与 AGENTS.md 保持同步：如果此处的规则有变更，检查 skill 文档是否需要同样的变更。

## 陷阱记录

- 操作级排错知识存放在相关 skill 中（例如构建问题放 `eyelib-build`，渲染问题放 `eyelib-debug`）。
- 每个 skill 的 "Common Pitfalls" 部分每个条目覆盖一类问题。
- 遇到新问题时，将其添加到相关 skill 的陷阱部分——如果没有现有 skill 覆盖该领域，则创建一个新的聚焦 skill。

## 阅读顺序

1. :AGENTS.md:（本文件）— 规则和约定
2. :docs/README.md: — 文档系统导航
3. :MODULES.md: — 模块清单和归属
4. 最近的 `package-info.java` 了解包级别定位
5. 然后才是你需要修改的代码文件
