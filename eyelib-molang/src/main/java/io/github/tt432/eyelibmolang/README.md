# Molang Engine Package Index

## Scope
- Path: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`
- Engine-owned Molang value/runtime wrappers, scope/owner-set/compiler/cache/mapping-api/built-in mappings/type, and generated parser artifacts.

## Start Reading Here
1. `docs/index/molang.md`
2. `docs/architecture/01-module-boundaries.md`
3. Specific compiler/runtime code needed by the task

## Key Areas
- `MolangScope.java` + `MolangOwnerSet.java`: shared evaluation scope/owner state
- `MolangValue*.java`: canonical Molang value/runtime wrappers used by root consumers via the subproject dependency
- `MolangCompiledFunction.java`: engine seam type consumed by `MolangValue`
- `compiler/`: compile/cache/class generation flow
- `mapping/`: built-in mappings and plain-JVM mapping support
- `mapping/api/`: mapping annotations/discovery/query runtime bridge
- `type/`: Molang typing support
- `generated/`: generated parser artifacts

## Critical Rule
- Treat `generated/` as the active generated/read-only zone for normal work.
