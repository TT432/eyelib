---
name: molang-refactor-supervisor
description: Coordinate eyelib-molang refactor slices through subagent design, implementation, review, roadmap alignment, and Gradle verification without directly editing business logic.
---

## When to use

Use this skill when supervising `:eyelib-molang` refactor work that must be split into reviewable subagent tasks.

## Protocol

1. Read `eyelib-molang/ROADMAP.md`, `eyelib-molang/refactor-plan/README.md`, the relevant phase plan, `MODULES.md`, and `docs/index/repo-map.md` before assigning a slice.
2. Split work into design, implementation, and review subtasks. Use separate fresh subagents unless continuity is required.
3. Keep generated parser files under `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` read-only unless the task explicitly covers regeneration/isolation.
4. Require tests before implementation when semantics change, and require `./gradlew :eyelib-molang:test` for phase 1-4 slices.
5. Update `eyelib-molang/ROADMAP.md` in the same slice when phase status, gates, evidence, ownership, or verification posture changes.
6. Update `MODULES.md` only when module responsibility, paths, or interactions change.

## Handoff shape

Each subagent task should include affected paths, pass/fail acceptance criteria, must-not-touch paths, verification commands, and documentation impact.
