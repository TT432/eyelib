# Molang Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/molang/`
- Legacy Molang marker/handoff path only.
- Handwritten Molang value/runtime wrappers, mappings, scope/compiler/type, and generated parser ownership now live in `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`.

## Start Reading Here
1. `docs/index/molang.md`
2. `docs/architecture/01-module-boundaries.md`
3. Specific compiler/runtime code needed by the task

## Key Areas
- `grammer/`: legacy grammar marker and documentation handoff
- `README.md`: redirect into `:eyelib-molang` plus boundary notes for the legacy path

## Critical Rule
- Treat `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` as the active generated/read-only zone for normal work.
