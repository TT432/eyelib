# Eyelib Agent Guide

## Start Here
- Read :MODULES.md: before planning structural or multi-module changes.
- Read the nearest package :README.md: before editing files in that subtree.
- For boundary decisions, read :docs/architecture/01-module-boundaries.md:.

## Repository Shape
- Multi-project :Gradle + Java 17 + Forge: codebase: root runtime module :::, and 10 Gradle subprojects: ::eyelib-animation:, ::eyelib-attachment:, ::eyelib-behavior:, ::eyelib-importer:, ::eyelib-material:, ::eyelib-molang:, ::eyelib-network:, ::eyelib-particle:, ::eyelib-preprocessing:, ::eyelib-util:.
- The authoritative dependency graph is each subproject's `build.gradle` `project(:)` edges. Read those before trusting any prose document.
- Preserve existing core patterns: manager, loader, visitor, and codec.

## Editing Rules
- Do not touch unrelated uncommitted changes.
- Prefer narrow edits over broad package churn.
- Do not add code to ambiguous areas without first documenting the destination responsibility.
- Before each change, identify which modules in :MODULES.md: are affected. Update :MODULES.md: in the same change if responsibility, paths, or interactions change.
- If a module is added or removed, update :MODULES.md: and any impacted docs in the same change.
- Subproject `build.gradle` `project(:)` edges define the real architecture. A subproject must never depend on root (`io.github.tt432.eyelib.*`).

## Documentation Rules
- **Paths must resolve.** Every file path reference in docs must exist. If a referenced file is deleted or moved, update or delete the reference.
- **Don't keep history in active docs.** Completed tasks, resolved problems, and historical intermediate states belong in `docs/architecture/migration/`, not in current-state documents.
- **Claims about dependencies must match `build.gradle`.** The Gradle dependency graph is the single source of truth.
- **Docs-only changes:** verify every referenced file path resolves before committing.
- **Structure/code changes:** build via JetBrains MCP and require exit code :0: before claiming completion.
- **Runtime-sensitive changes:** compile first, then use the existing dev client flow for smoke checks.

## Generated Code
- Treat :eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/: as generated/read-only during normal work.
- `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` holds root-coupled query functions (animation controller, variant) that cannot move to `eyelib-molang`.
- Do not hand-edit generated parser files unless the current task is explicitly about generated-code isolation or regeneration.

## Tooling Restrictions
- IntelliJ IDEA is the sole IDE. VS Code and Eclipse artifacts must never be committed.
- **JDTLS is explicitly prohibited.** All tooling integration uses JetBrains MCP.
- All Gradle commands must use JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.

## Molang Roadmap
- Read :eyelib-molang/ROADMAP.md: before planning or implementing Molang refactor work.
- Update :eyelib-molang/ROADMAP.md: in the same change when Molang phase status, milestones, gates, ownership, verification commands, corpus layers, binder/runtime semantics, host/query behavior, policy/specialization/cache behavior, or cutover posture changes.

## Pitfall Records
- `docs/pitfalls/` stores troubleshooting records for recurring or non-obvious issues.
- **Each file has a single responsibility** — describe one class of problem, not a collection.
- **File names must be clear and specific** (e.g. `non-mod-libs-need-additionalruntimeclasspath.md`).
- When encountering a new issue, decide whether it fits an existing record — if so, merge it in; if it's a distinct problem, create a new file.
- Each record should cover: what the symptom looks like, why it happens, and the correct fix.

## Reading Order
1. :AGENTS.md:
2. :MODULES.md:
3. Nearest package :README.md:
4. Only then the code files you need to change
