# Molang Subsystem Index

## Scope
- Engine-owned Molang core path: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`
- Root legacy marker path: `src/main/java/io/github/tt432/eyelib/molang/`
- Covers Molang value/runtime wrappers, scope/compiler/mapping/built-in mapping-api/type/generated parser artifacts in `:eyelib-molang`, with Forge platform bindings under `io.github.tt432.eyelibmolang.platform` and root keeping only root-coupled `MolangQuery` in `molang/mapping/`.

## Start Reading Here
1. `docs/architecture/01-module-boundaries.md`
2. `eyelib-molang/ROADMAP.md` for current phase status, next milestones, gates, and update rules
3. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md`
5. `eyelib-molang/design/README.md` for rewrite discussion drafts
6. `eyelib-molang/refactor-plan/README.md` for the dependency-ordered execution plan and cutover gates
7. Only then specific compiler/runtime classes

## Critical Rule
- Treat `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` as generated/read-only unless the current task is explicitly about generated-code isolation.
- Treat `eyelib-molang/ROADMAP.md` as the current-state source of truth for Molang progress. Update it in the same change when Molang milestones, gates, ownership, verification, corpus, binder/runtime, host/query, policy/cache, or cutover posture changes.

## Hotspots
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/cache/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompilerImpl.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangBytecodeEmitter.java`
