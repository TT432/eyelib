# bootstrap

## Scope
- Mod bootstrap and registration wiring.
- Main paths:
  - `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`
  - `src/main/java/io/github/tt432/eyelib/Eyelib.java`

## Why it is MC-facing
- Forge startup ownership requires `@Mod` and `FMLJavaModLoadingContext`.
- Startup composition remains responsible for attachment registration and network registration.

## Final isolation status
- First-wave seam status: complete.
- Final `mc/api + mc/impl` isolation status: **final isolation complete** for bootstrap ownership.
- Bootstrap entrypoint is now quarantined under allowed `mc/impl` package layout.

## Implemented slice
- Moved Forge `@Mod` composition root from legacy top-level package to `mc/impl/bootstrap/EyelibMod`.
- Kept top-level `Eyelib` as a constant-only compatibility holder for `MOD_ID` so existing call sites do not need extra indirection.
- Preserved startup composition behavior with no new bootstrap abstraction:
  - `EyelibAttachableData.DATA_ATTACHMENTS.register(bus)`
  - `EyelibNetworkManager.register()`

## Verification (JetBrains MCP only)
- `jetbrain_get_file_problems` on changed Java files (`Eyelib.java`, `mc/impl/bootstrap/EyelibMod.java`, `package-info.java`) returned no issues.
- `jetbrain_build_project` project compile: success.
- `jetbrain_run_gradle_tasks` with `build` task: exit code `0` (tests + `nullawayMain` included in task graph).

## Checklist
- [x] Interface design (no new port needed)
- [x] Tests (no extracted pure helper introduced)
- [x] Implementation
- [x] JetBrains MCP verification

## Notes
- No bootstrap-facing port was introduced because this slice was a pure package/layout quarantine move.
- Any future bootstrap wiring should remain in `mc/impl/bootstrap` rather than returning to top-level package ownership.
