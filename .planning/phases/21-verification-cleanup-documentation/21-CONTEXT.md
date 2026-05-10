# Phase 21 Context: Verification, Cleanup & Documentation

## Goal
- Prove the v1.3 `:eyelib-util` split is complete.
- Confirm root `util/**` and `core/util/**` Java source paths are empty.
- Update final topology docs and record verification evidence.

## Required Checks
- `src/main/java/io/github/tt432/eyelib/util/**/*.java` returns no files.
- `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` returns no files.
- Java source search for old root/core util imports returns zero matches.
- `:eyelib-util` remains leaf-only with zero `project(...)` dependencies.
- Full project rebuild passes through JetBrains MCP.
- Relevant module identity/build tests pass through JetBrains MCP.

## Documentation Scope
- `MODULES.md`
- `docs/index/repo-map.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/ARCHITECTURE-BLUEPRINT.md`
- `docs/architecture/migration/utility-routing-manifest.md`
- `src/main/java/io/github/tt432/eyelib/util/README.md`
- `eyelib-util/README.md`

## Completion Criteria
- Phase 21 verification report records all final evidence.
- `.planning/ROADMAP.md` marks Phase 21 complete and v1.3 shipped.
- `.planning/STATE.md` records v1.3 completion readiness.
