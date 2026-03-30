# Molang Subsystem Index

## Scope
- Root path: `src/main/java/io/github/tt432/eyelib/molang/`
- Covers Molang runtime values, compiler flow, mappings, types, and generated parser artifacts.

## Start Reading Here
1. `src/main/java/io/github/tt432/eyelib/molang/README.md`
2. `docs/architecture/01-module-boundaries.md`
3. Only then specific compiler/runtime classes

## Critical Rule
- Treat `src/main/java/io/github/tt432/eyelib/molang/generated/` as generated/read-only unless the current task is explicitly about generated-code isolation.

## Hotspots
- `src/main/java/io/github/tt432/eyelib/molang/generated/`
- `src/main/java/io/github/tt432/eyelib/molang/compiler/`
