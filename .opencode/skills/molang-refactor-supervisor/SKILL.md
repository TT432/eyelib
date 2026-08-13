---
name: molang-refactor-supervisor
description: Coordinate eyelib `molang` package refactor slices through subagent design, implementation, review, roadmap alignment, and Gradle verification without directly editing business logic.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# molang-refactor-supervisor

Coordinate eyelib `molang` package refactor slices through subagent design, implementation, review, roadmap alignment, and Gradle verification without directly editing business logic.

## When to use
- supervising `io.github.tt432.eyelib.molang` package refactor work that must be split into reviewable subagent tasks

## Rules
- PREFER Each subagent task should include affected paths, pass/fail acceptance criteria, must-not-touch paths, verification commands, and documentation impact.
- Read `docs/molang/ROADMAP.md`, `docs/molang/refactor-plan/README.md`, the relevant phase plan, and `MODULES.md` before assigning a slice.
- Split work into design, implementation, and review subtasks; use separate fresh subagents.
- NEVER Revive the ANTLR path when refactoring (ANTLR generated code was fully removed on 2026-06-09, ADR-0004 Superseded; the handwritten recursive-descent parser under `src/main/java/io/github/tt432/eyelib/molang/compiler/` is the only frontend).
- [when when semantics change / for phase 1-4 slices] Require tests before implementation when semantics change, and require `mcmcp_test` for phase 1-4 slices.
- [when when phase status, gates, evidence, ownership, or verification posture changes] Update `docs/molang/ROADMAP.md` in the same slice.
- [when when module responsibilities change] Edit the corresponding `package-info.java` (e.g. `src/main/java/io/github/tt432/eyelib/molang/package-info.java`), then run `:generateModulesMd` to regenerate `MODULES.md`.
- NEVER Hand-edit `MODULES.md` (AGENTS.md Editing Rules).

## Workflow
1. Read roadmap, refactor-plan README, relevant phase plan, and MODULES.md before assigning a slice
2. Split work into design, implementation, and review subtasks assigned to separate fresh subagents
3. Require tests before implementation when semantics change; require mcmcp_test for phase 1-4 slices
4. Update ROADMAP.md in the same slice on posture changes; on module responsibility changes edit package-info.java and run :generateModulesMd to regenerate MODULES.md
5. Shape each subagent handoff with affected paths, acceptance criteria, must-not-touch paths, verification commands, and documentation impact

## Output
- Stop when: all assigned refactor slices verified and documented

<!-- locked residual (verbatim, do not edit) -->
ANTLR 生成代码已于 2026-06-09 整体移除(见 ADR-0004 Superseded)。当前手写 recursive-descent parser 是唯一前端,在 `src/main/java/io/github/tt432/eyelib/molang/compiler/` 下。重构时不要复活 ANTLR 路径。
