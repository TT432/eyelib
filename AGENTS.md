# Eyelib Agent Guide

## Start Here
- Read :docs/README.md: for the full documentation system navigation.
- Read :MODULES.md: before planning structural or multi-module changes.
- Read the nearest `package-info.java` for package-level orientation.
- For boundary decisions, read :docs/decisions/0002-module-boundaries.md:.

## Repository Shape
- Single-project :Gradle + Java 17 + Forge: codebase. All source under `src/main/java/io/github/tt432/eyelib/<module>/`, modules separated by package convention (see :docs/decisions/0014-flat-merge.md:). `clientsmoke/` is a composite build (smoke test framework), not part of the main project.
- The authoritative module inventory is :MODULES.md:. Package boundaries (`io.github.tt432.eyelib.<module>`) define the real architecture.
- Preserve existing core patterns: manager, loader, visitor, and codec.

## Editing Rules
- Do not touch unrelated uncommitted changes.
- Prefer narrow edits over broad package churn.
- Do not add code to ambiguous areas without first documenting the destination responsibility.
- Before each change, identify which modules in :MODULES.md: are affected. Update :MODULES.md: in the same change if responsibility, paths, or interactions change.
- If a module is added or removed, update :MODULES.md: and any impacted docs in the same change.
- Package boundaries (`io.github.tt432.eyelib.<module>`) define the real architecture. A module package must not depend on root orchestration packages (`io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.common`).

## Comment Rules

### 基本原则
- **统一使用中文**。
- 注释的价值在于**信息增量** — 删掉后别人会误解或犯错时才需要。

### `@author TT432`
- 每个 `.java` 文件的类/接口声明前加 `/** @author TT432 */`。

### `@NullMarked`
- **全局强制**。所有类、所有 `package-info.java` 必须加 `@NullMarked`。

### package-info.java
- 统一格式，用一段话简述包职责：
```java
@NullMarked
package ...;

/**
 * 一句话说明包的职责。
 */
```

- 不再单独维护包级 README.md。包的知识分层如下：
  - **公共 API 的 Javadoc** → 类/方法签名自带
  - **包定位** → `package-info.java` 一句话
  - **架构决策** → `docs/decisions/`（为什么这么设计）
  - **工作导航** → `AGENTS.md`（怎么看这个项目）

### Javadoc（类级）
- 所有 `public` 类/接口必须有，不超过两句话简述职责。
- `private` 内部类、纯数据 record、简单 DTO 可不加。

### Javadoc（方法级）
- `@param` / `@return` **不强制**。仅在行为非显而易见时写，重点关注：
  - **边界条件**：null 约定、线程要求、副作用、前置/后置条件。
  - **性能特征**：O(n) 还是 O(1)，会阻塞吗。
  - **幂等性**：重复调用是否安全。
- 简单 getter/setter、`equals`/`hashCode`/`toString`、框架 override 方法不加。

### TODO
- 统一格式：`// TODO: 中文描述`
- 禁止：`todo`（小写无冒号）、`TODO:fix`、`TODO(Phase N)`、Javadoc 中嵌 TODO。

### 行内注释（`//`）
- **默认不写**。仅当逻辑确实复杂且一眼看不懂时才加，且必须回答 **"为什么这样做"** 而非 **"代码在做什么"**。

### 禁止项
- **段分隔装饰符**：`// ----`、`// ───`、`// ═══` 等一律禁止。
- **变量名译本**：`// samplerStates — convert each BrSamplerState` 这类注释禁止。
- **重复注解**：`@DisplayName("...")` 下方再写相同内容的 Javadoc。
- **模板复制**：同一段话出现在多个文件的注释中。
- **HTML 标签**：`<ul>`、`<ol>`、`<li>`、`<p>` 等禁止出现在注释中。

### 各代码类型差异化标准

| 代码类型 | 类级 Javadoc | 方法级 | 行内 |
|---|---|---|---|
| 公开 API（api/、接口） | 必须 | 边界条件必写 | 按需（层次 3） |
| 实现类 | 必须 | 边界条件必写 | 按需（层次 3） |
| 测试 | 不加 | 不加 | 按需（层次 3） |
| Forge 注册/样板 | 不加 | 不加 | 不加 |
| 生成代码 | 不加 | 不加 | 不加 |

### 注释深度分层

- **层次 0（禁止）**：删掉后无任何信息损失 → 不写。
- **层次 1（标识）**：一句话说清职责或非显而易见的含义 → 类级 Javadoc、sentinel 值、API 枚举常量。
- **层次 2（边界）**：调用方不知就可能会写 bug → null 约定、线程安全、副作用、前置条件。
- **层次 3（解释）**：熟练同事 code review 会问「为什么不用 xxx？」→ 绕过已知问题、对齐外部规范、设计取舍。

### 自检清单
写注释前依次确认：
1. 删掉后别人会误解吗？→ 不会 → 删
2. 信息已经存在于类型签名/命名里吗？→ 是 → 删
3. 半年后我自己还需要这个信息吗？→ 不需要 → 删
4. 能用一句话说清吗？→ 不能 → 先重构代码，代码本身比注释好

## Documentation Rules
- **Paths must resolve.** Every file path reference in docs must exist. If a referenced file is deleted or moved, update or delete the reference.
- **Don't keep history in active docs.** Completed tasks, resolved problems, and historical intermediate states belong in git history, not in current-state documents.
- **Architecture decisions go in `docs/decisions/`.** Each file is one ADR: context → decision → consequences. No implementation details.
- **Code is the authoritative reference.** Package structure, class names, and method signatures are the source of truth for "what exists." Bedrock standards and external format specs are referenced directly from their canonical sources (E盘 Bedrock 文档、Mojang Creator 文档).
- **Claims about dependencies must match `build.gradle`.** The Gradle dependency graph is the single source of truth.
- **Docs-only changes:** verify every referenced file path resolves before committing.
- **Structure/code changes:** build via JetBrains MCP and require exit code :0: before claiming completion.
- **Runtime-sensitive changes:** compile first, then use the existing dev client flow for smoke checks.

## Generated Code (Historical — ANTLR Removed)
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` has been removed along with all ANTLR-generated parser artifacts. The handwritten recursive-descent parser is the sole frontend.
- `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` holds root-coupled query functions (animation controller, variant) that cannot move to `eyelib-molang`.

## Tooling Restrictions
- IntelliJ IDEA is the sole IDE. VS Code and Eclipse artifacts must never be committed.
- **JDTLS is explicitly prohibited.** All tooling integration uses JetBrains MCP.
- All Gradle commands must use JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.
- **Game restarts**: close running clients via debug HTTP `/eval` → `minecraft.stop()`, never `kill` java processes from shell.
- **Game startup**: use `jetbrain_run_gradle_tasks` with `["runClient"]`. Before starting, check port 25999 is free; if occupied, close the old instance first.

## Build & Test Verification

- **`FROM-CACHE` / `UP-TO-DATE` is acceptable.** If a Gradle `test` task shows `UP-TO-DATE`, it means the tests were already run on the same sources — the build system tracks input changes correctly. Only clear the affected module's `build/` directory when you intentionally changed test sources and Gradle is ignoring the change.
- **Never pass `--no-build-cache` to Gradle.** It forces a full rebuild of MC Forge artifacts. Use targeted cache clearing instead.
- `jetbrain_build_project` compiles via IntelliJ. For runtime verification that depends on reobfuscated JARs, use `jetbrain_run_gradle_tasks` with `runClient`.

## Skill Usage
- Skills are modular and focused — one skill covers one workflow domain. Don't dump everything into a single skill.
- When a recurring pitfall or workflow is discovered, decide which skill domain it belongs to (build, debug, renderdoc, clientsmoke). If no existing skill fits, create a new focused one.
- Skills must be kept in sync with AGENTS.md: if a rule changes here, check whether skill docs need the same change.

## Molang Roadmap
- Read :docs/molang/ROADMAP.md: before planning or implementing Molang refactor work.
- Update :docs/molang/ROADMAP.md: in the same change when Molang phase status, milestones, gates, ownership, verification commands, corpus layers, binder/runtime semantics, host/query behavior, policy/specialization/cache behavior, or cutover posture changes.

## Pitfall Records
- Operational troubleshooting knowledge lives in the relevant skill (e.g. `eyelib-build` for build issues, `eyelib-debug` for rendering issues).
- Each skill's "Common Pitfalls" section covers one class of problem per entry.
- When encountering a new issue, add it to the relevant skill's pitfalls section — or create a new focused skill if no existing skill covers that domain.

## Reading Order
1. :AGENTS.md: (this file) — rules and conventions
2. :docs/README.md: — documentation system navigation
3. :MODULES.md: — module inventory and ownership
4. Nearest package :README.md: (if exists)
5. Only then the code files you need to change
