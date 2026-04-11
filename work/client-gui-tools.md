# client-gui-tools

## Scope
- Manager screen UI, previews, hotkeys, and client tooling helpers.
- Main paths: `client/gui/`

## Why it is MC-facing
- Uses `Screen`, `GuiGraphics`, keymapping, client tick events, and preview rendering.

## Final isolation status
- Hard-import slice status: completed (hard-import slice advanced).
- Final `mc/api + mc/impl` isolation status: pending re-baseline.
- Expected final state for this module: screens, previews, hotkeys, dialogs, watcher-triggered runtime calls, and GUI rendering stay in `mc/impl`; only non-UI planning/service contracts may remain outside it.

## Target seam
- Keep UI in `mc/impl`.
- Extract only non-UI planning/service ports when they can be tested outside Minecraft.

## Seam decision (this slice)
- Keep screens/previews/hotkeys/file dialogs/watcher-triggered MC calls in-place.
- Advance `ManagerResourceReloadPlan` from path classification-only into a broader non-UI planning seam: path normalization, route classification, and texture-key shaping.
- `ManagerResourceImportPlanner` now delegates both single-file route selection and texture-key derivation to that helper; runtime registry publication, texture upload, importer calls, and Forge texture-change event posting remain MC-facing in the planner.

## Implementation notes
- Expanded `client/gui/manager/reload/ManagerResourceReloadPlan.java` with `Path`-based `classifySingleFile(basePath, file)`, normalized `classifySingleFile(String)`, and string-key `toTextureKey(basePath, textureFile)` planning helpers.
- `ManagerResourceImportPlanner` now routes texture upload via `ManagerResourceReloadPlan.toTextureKey(...)`, removing direct `ResourceLocation` construction from the planner and keeping runtime upload wiring in `NativeImageIO`.
- Added a narrow `NativeImageIO.upload(String, NativeImage)` overload so planner runtime code can stay string-keyed at the planning seam boundary.
- No UI flow rewrites and no hotkey/screen/file-dialog behavior changes.

## Tests
- Added targeted seam test: `src/test/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceReloadPlanTest.java`.
- Coverage now includes known JSON folder routes, model/texture routes, unsupported paths, backslash normalization, `Path`-based relative classification, and lowercase texture-key shaping.

## Verification (JetBrains MCP)
- ✅ `test --tests io.github.tt432.eyelib.client.gui.manager.reload.ManagerResourceReloadPlanTest`
- ✅ `build`
- ✅ `jetbrain_build_project` (project compile/build problem scan)
- ⚠️ Java `lsp_diagnostics` is tool-blocked in this environment (`jdtls` not installed); verification used JetBrains MCP inspections/Gradle build instead.

## Deliverables
- Identify non-UI seams worth extracting.
- Add tests around extracted planning/service logic.
- Implement minimal split without destabilizing tooling flows.

## Dependencies
- After `client-loaders`, `client-render`, and `client-managers-registry`.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [ ] Re-baseline remaining Minecraft/Forge references for this module.
- [ ] Confine screens, previews, hotkeys, dialogs, and GUI rendering/runtime calls to allowed `mc/impl` packages.
- [ ] Keep non-UI planning/service seams free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- `ModelPreviewScreen` still directly depends on Minecraft GUI/rendering/runtime types (`Screen`, `GuiGraphics`, `Minecraft`, `RenderType`, `ResourceLocation`, Blaze3D pose/buffer types) and Forge event bus registration; it should be treated as implementation-only UI code for eventual `mc/impl` ownership rather than as a seam candidate.
- `ManagerScreenOpenEvents` and `ManagerScreenKeybinds` still directly own Forge client event/keybind wiring and must end up under allowed `mc/impl` ownership in the final layout.
- `ManagerResourceImportPlanner` still directly handles `NativeImage`, `MinecraftForge.EVENT_BUS`, `TextureChangedEvent`, importer calls, and texture upload, so the planner itself remains MC-facing runtime tooling even though it now delegates route/texture-key planning to a pure helper seam.
- The clearest platform-type-free seam in this module is now `ManagerResourceReloadPlan` (route + texture-key planning); broader final isolation should favor moving GUI/runtime tooling into `mc/impl` rather than inventing additional abstractions around screen behavior.
- This module is also downstream of render/runtime quarantine more than it first appears: `ModelPreviewScreen` directly consumes `RenderParams`, `RenderType`, DFS/baked-model runtime helpers, and modbridge update events, while `ManagerResourceImportPlanner` directly depends on `NativeImageIO`. The render payload seam is now narrower (`RenderModelSyncPayload`), but final GUI quarantine still depends on the heavier render/runtime owners stabilizing first.
- Local package guidance now exists at `src/main/java/io/github/tt432/eyelib/client/gui/README.md` plus the more specific `client/gui/manager/README.md`; keep both aligned as GUI runtime owners move toward `mc/impl`.

## Priority hotspot files for next slice
- Highest import-density GUI/runtime owners currently worth moving as implementation-only UI code:
  - `client/gui/manager/AnimationView.java`
  - `client/gui/ModelPreviewScreen.java`
  - `client/gui/manager/DragTargetWidget.java`
  - `client/gui/manager/EntitiesListPanel.java`
  - `client/gui/manager/EyelibManagerScreen.java`
- Runtime-tooling follow-up after render stabilization:
  - `client/gui/manager/reload/ManagerResourceImportPlanner.java`
  - `client/gui/manager/hotkey/ManagerScreenKeybinds.java`
  - `client/gui/manager/hotkey/ManagerScreenOpenEvents.java`
