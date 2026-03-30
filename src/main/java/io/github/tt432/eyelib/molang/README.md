# Molang Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/molang/`
- Molang values, compiler/runtime flow, mappings, types, and generated grammar artifacts.

## Start Reading Here
1. `docs/index/molang.md`
2. `docs/architecture/01-module-boundaries.md`
3. Specific compiler/runtime code needed by the task

## Key Areas
- `compiler/`: compile/cache/class generation flow
- `mapping/`: mapping layer
- `type/`: Molang typing support
- `generated/`: generated parser artifacts
- `grammer/`: legacy grammar marker and documentation handoff

## Critical Rule
- Treat `generated/` as the active generated/read-only zone for normal work.
