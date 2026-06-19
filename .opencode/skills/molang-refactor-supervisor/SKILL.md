---
name: molang-refactor-supervisor
description: Coordinate eyelib `molang` package refactor slices through subagent design, implementation, review, roadmap alignment, and Gradle verification without directly editing business logic.
---

## When to use

Use this skill when supervising `io.github.tt432.eyelib.molang` package refactor work that must be split into reviewable subagent tasks.

## Protocol

1. Read `docs/molang/ROADMAP.md`, `docs/molang/refactor-plan/README.md`, the relevant phase plan, and `MODULES.md` before assigning a slice.
2. Split work into design, implementation, and review subtasks. Use separate fresh subagents unless continuity is required.
3. ANTLR 生成代码已于 2026-06-09 整体移除(见 ADR-0004 Superseded)。当前手写 recursive-descent parser 是唯一前端,在 `src/main/java/io/github/tt432/eyelib/molang/compiler/` 下。重构时不要复活 ANTLR 路径。
4. Require tests before implementation when semantics change, and require `jetbrain_run_gradle_tasks(["test"])` for phase 1-4 slices. **禁止 shell gradlew**(AGENTS.md Tooling Restrictions)。
5. Update `docs/molang/ROADMAP.md` in the same slice when phase status, gates, evidence, ownership, or verification posture changes.
6. 模块职责变化时改对应 `package-info.java`(在 `src/main/java/io/github/tt432/eyelib/molang/package-info.java` 等),然后跑 `:generateModulesMd` 重生成 `MODULES.md`。**禁止手编 MODULES.md**(AGENTS.md Editing Rules)。

## Handoff shape

Each subagent task should include affected paths, pass/fail acceptance criteria, must-not-touch paths, verification commands, and documentation impact.
