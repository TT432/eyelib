# Eyelib Agent Guide

## Start Here
- Read :docs/index/repo-map.md: before exploring code.
- Read :MODULES.md: before planning structural or multi-module changes.
- Read the nearest package :README.md: before editing files in that subtree.
- For boundary decisions, read :docs/architecture/01-module-boundaries.md: and :docs/architecture/02-side-boundaries.md:.

## Repository Shape
- This repository is now a multi-project :Gradle + Java 17 + Forge: codebase with root runtime module :::, processor subproject ::eyelib-preprocessing:, importer/model subproject ::eyelib-importer:, and engine Molang subproject ::eyelib-molang:.
- The current module split exists because a human explicitly requested extraction of the resources importer/model seam; keep that shape unless a human asks to collapse it again.
- Preserve existing core patterns already used in the codebase: manager, loader, visitor, and codec.

## Editing Rules
- Do not touch unrelated uncommitted changes.
- Prefer narrow, stage-scoped edits over broad package churn.
- Document ownership and dependency rules before moving code across subsystem boundaries.
- Do not add new code to ambiguous catch-all areas like :src/main/java/io/github/tt432/eyelib/util/client/: without first documenting the destination responsibility.
- Before each change, identify which modules in :MODULES.md: are affected.
- If an affected module changes responsibility, main paths, or interactions, update :MODULES.md: in the same change.
- If a module is added or removed, update :MODULES.md: and any impacted index/architecture docs in the same change.

## Generated Code
- Treat :eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/: as generated/read-only during normal work.
- Root `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` is the only file remaining in the root `molang/` path — it holds root-coupled query functions (animation controller, variant, DataAttachment) that cannot move to `eyelib-molang`.
- Do not hand-edit generated parser files unless the current task is explicitly about generated-code isolation or regeneration.

## Tooling Restrictions
- This project uses IntelliJ IDEA as its sole IDE. VS Code and Eclipse artifacts (.vscode/, .eclipse/, .project, .classpath, .factorypath, .settings/, bin/) must never be committed or recreated.
- **JDTLS (Eclipse Java Language Server) is explicitly prohibited.** Do not use, install, or configure JDTLS-based tooling for this project. The project relies on IntelliJ's own language server and JetBrains MCP for all tooling integration.
- All Gradle commands must use JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.

## Molang Roadmap
- Read :eyelib-molang/ROADMAP.md: before planning or implementing Molang refactor work.
- Update :eyelib-molang/ROADMAP.md: in the same change whenever Molang phase status, milestones, gates, ownership, verification commands, corpus layers, binder/runtime semantics, host/query behavior, policy/specialization/cache behavior, or cutover posture changes.
- Treat :eyelib-molang/ROADMAP.md: as the current-state source of truth; design drafts remain discussion artifacts until the roadmap or refactor plan promotes them.

## Reading Order
1. :AGENTS.md:
2. :MODULES.md:
3. :docs/index/repo-map.md:
4. Relevant :docs/architecture/*.md:
5. Nearest package :README.md:
6. Only then the code files you need to change

## Layer Priority Rules

When multiple instruction sources conflict, resolve in this order:

1. **Global Safety Rules** — AGENTS.md Hard Blocks and constraints (never violated)
2. **Sisyphus Identity** — Behavior parameters from `<identity>` and `<style>` sections (cross-session consistency)
3. **Project Rules** — AGENTS.md, docs/conventions.md, ROADMAP.md (project-specific constraints)
4. **Active Skill** — Currently loaded skill workflow and constraints (override general defaults)
5. **Task Context** — Current task scope and user instructions (override only where safe)

Lower-numbered layers always win in conflict. A skill may not override global safety rules; a task context may not override identity parameters.

## Verification
- Docs-only changes: verify every referenced file path resolves.
- Structure/code changes: run the stage-specific Gradle command from the plan and require exit code :0:.
- Runtime-sensitive changes: compile first, then use the existing dev client flow for smoke checks.
- Null-safety changes: run :./gradlew nullawayMain: and require exit code :0: before claiming completion.
- See `docs/conventions.md` for unified temp file, commit message, and KPI recording practices.
