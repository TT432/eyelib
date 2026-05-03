# Molang Engine Package Index

## Scope
- Path: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`
- Engine-owned Molang value/runtime wrappers, scope/owner-set/compiler/cache/mapping-api/built-in mappings/type, and generated parser artifacts.

## Start Reading Here
1. `docs/index/molang.md`
2. `eyelib-molang/ROADMAP.md`
3. `docs/architecture/01-module-boundaries.md`
4. Specific compiler/runtime code needed by the task

## Key Areas
- `MolangScope.java`: shared evaluation scope state
- `MolangValue*.java`: canonical Molang value/runtime wrappers used by root consumers via the subproject dependency
- `MolangCompiledFunction.java`: engine seam type consumed by `MolangValue`
- `compiler/`: compile/cache/binding/bytecode emission flow plus `MolangCompilerImpl`, `MolangBytecodeEmitter`, shared expression analysis, and compile-time constant folding helpers consumed by plain-JVM processor code
- `compiler/cache/`: compiler cache layer
- `compiler/binding/`: compiler binding and diagnostic infrastructure
- `mapping/`: built-in mappings and plain-JVM mapping support
- `mapping/api/`: mapping annotations/discovery/query runtime bridge
- `type/`: Molang typing support
- `generated/`: generated parser artifacts

## Critical Rule
- Treat `generated/` as the active generated/read-only zone for normal work.
- Treat `eyelib-molang/ROADMAP.md` as the current-state source of truth for Molang phase status, milestones, gates, and verification posture. Update it in the same change when Molang refactor progress changes.
