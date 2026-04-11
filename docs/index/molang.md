# Molang Subsystem Index

## Scope
- Engine-owned Molang core path: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`
- Root legacy marker path: `src/main/java/io/github/tt432/eyelib/molang/`
- Covers Molang value/runtime wrappers, scope/compiler/mapping/built-in mapping-api/type/generated parser artifacts in `:eyelib-molang`, with root keeping only legacy marker docs and `mc/impl/molang/**` platform wiring.

## Start Reading Here
1. `docs/architecture/01-module-boundaries.md`
2. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/README.md`
3. `src/main/java/io/github/tt432/eyelib/molang/README.md`
4. Only then specific compiler/runtime classes

## Critical Rule
- Treat `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` as generated/read-only unless the current task is explicitly about generated-code isolation.

## Hotspots
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`
