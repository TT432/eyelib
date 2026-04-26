# molang-mc-adapters

## Scope
- Minecraft-facing Molang mappings and runtime query bridges.
- Main paths: `molang/mapping/`, selected compiler/runtime hooks touching Forge lifecycle

## Why it is MC-facing
- `MolangQuery` is one of the heaviest MC-coupled classes in the repo.
- Some compiler cache lifecycle hooks also depend on Forge events.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: completed for the current Molang slice.
- Final state achieved in this slice: compiler, values, mapping tree, and type-system logic remain outside `mc/impl`; Forge lifecycle hooks, Minecraft query runtime implementations, and the heavy query mapping implementation now live under allowed `mc/impl` packages.

## Target seam
- Keep compiler, value runtime, and type system in `core`.
- Move Minecraft-backed query adapters and lifecycle hooks behind narrow ports.

## Deliverables
- Design query/provider interfaces.
- Add plain-JVM tests around extracted query-independent logic.
- Implement the split and migrate Molang call sites.

## Dependencies
- First wave.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [x] Re-baseline remaining Minecraft/Forge references for this module.
- [x] Confine Forge lifecycle hooks, query runtime implementations, and MC-backed scans/bridges to allowed `mc/impl` packages.
- [x] Keep compiler/value/type-system contracts free of Minecraft/Forge types.
- [x] Re-run JetBrains MCP verification for the final package layout.
- [x] Pass rule-based boundary scan for this module.

## First-wave implementation (completed)

### Interface / port design
- Added `MolangMappingDiscovery` (`molang/mapping/api`) as a platform discovery port for mapping class registration.
- Added `MolangQueryRuntime` + `MolangQueryRuntimeBridge` (`molang/mapping/api`) as a narrow runtime port for singleton-backed query values (`actor_count`, `time_of_day`, `moon_phase`, `partial_tick`, `distance_from_camera`).
- Kept compiler/runtime/type logic in handwritten Molang core packages; moved Forge lifecycle/event coupling into mc-facing hook classes.

### Implementation changes
- Refactored `MolangMappingTree` to consume `MolangMappingDiscovery` and removed direct Forge event + scan dependencies from the core tree class.
- Added mc implementation hooks (now relocated under `mc/impl`):
  - `mc/impl/molang/mapping/ForgeMolangMappingDiscovery`
  - `mc/impl/molang/mapping/MolangMappingTreeLifecycleHooks`
  - `mc/impl/molang/mapping/MolangQueryRuntimeLifecycleHooks`
  - `mc/impl/molang/mapping/MinecraftMolangQueryRuntime`
  - `mc/impl/molang/compiler/MolangCompileLifecycleHooks`
- Removed Forge shutdown subscriber from `MolangCompileHandler`; lifecycle now routes through mc hook class.
- Relocated the heavy `MolangQuery` implementation to `mc/impl/molang/mapping/MolangQuery` so non-`mc/impl` Molang packages no longer directly import Minecraft/Forge runtime types.
- Updated `MolangQuery` to resolve singleton-backed values through `MolangQueryRuntimeBridge` instead of direct `Minecraft.getInstance()` calls for the extracted seam surface.
- Removed the last direct Minecraft dependency from `molang/mapping/MolangMath` by replacing `Mth` usage with plain-JVM `Math` equivalents.

### Targeted tests
- Added `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangMcAdapterSeamTest.java`.
- Added `src/test/java/io/github/tt432/eyelib/molang/mapping/MolangMathTest.java`.
- Test coverage includes:
  - Mapping tree setup through discovery port, alias resolution, and setup reset behavior.
  - Query runtime bridge behavior: scope `variable.partial_tick` precedence and runtime-port forwarding.
  - Plain-JVM parity checks for `MolangMath` trig/clamp behavior after removing `Mth`.

### Verification (JetBrains MCP only)
- `jetbrain_run_gradle_tasks`: `test --tests io.github.tt432.eyelibmolang.mapping.MolangMcAdapterSeamTest --tests io.github.tt432.eyelibmolang.mapping.MolangMathTest` ✅ (exit code 0).
- `jetbrain_run_gradle_tasks`: `build` ✅ (exit code 0, includes `nullawayMain`).
- Rule-based scans: `jetbrain_search_text` over `src/main/java/io/github/tt432/eyelib/molang/**` for `net.minecraft` and `net.minecraftforge` returned no remaining matches ✅.

## Notes
- LSP diagnostics tool (`lsp_diagnostics`) is blocked in this environment because `jdtls` is not installed; compile/test verification completed through JetBrains MCP as required.
