# Eyelib Agent Guide

## Start Here
- Read :docs/index/repo-map.md: before exploring code.
- Read :MODULES.md: before planning structural or multi-module changes.
- Read the nearest package :README.md: before editing files in that subtree.
- For boundary decisions, read :docs/architecture/01-module-boundaries.md: and :docs/architecture/02-side-boundaries.md:.

## Repository Shape
- This repository is now a multi-project :Gradle + Java 17 + Forge: codebase with root runtime module :::, importer/model subproject ::eyelib-importer:, and engine Molang subproject ::eyelib-molang:.
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
- Treat :src/main/java/io/github/tt432/eyelib/molang/grammer/: as a legacy marker, not a destination for new handwritten logic.
- Do not hand-edit generated parser files unless the current task is explicitly about generated-code isolation or regeneration.

## Reading Order
1. :AGENTS.md:
2. :MODULES.md:
3. :docs/index/repo-map.md:
4. Relevant :docs/architecture/*.md:
5. Nearest package :README.md:
6. Only then the code files you need to change

## Verification
- Docs-only changes: verify every referenced file path resolves.
- Structure/code changes: run the stage-specific Gradle command from the plan and require exit code :0:.
- Runtime-sensitive changes: compile first, then use the existing dev client flow for smoke checks.
- Null-safety changes: run :./gradlew nullawayMain: and require exit code :0: before claiming completion.
