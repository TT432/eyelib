# Client GUI Manager Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/gui/manager/`
- Developer/debug screen code for importing and monitoring client resources, including Bedrock models.

## Files To Know
- `EyelibManagerScreen.java`: main hotspot; currently mixes UI composition, file dialog flow, watch/reload flow, and import orchestration.
- `EntitiesListPanel.java`: entity listing support UI.
- `DragTargetWidget.java`: drag/drop UI support.
- `EntitiesScreen.java`: related screen flow.

## Planned Extraction Seams
- file dialog service
- folder watch/import planning service
- keybind/open-event wiring
- keeping the screen itself focused on widget composition and action delegation

## Current Extracted Helpers
- `io/FileDialogService.java`: asynchronous file and folder dialog handling
- `reload/ManagerResourceFolderWatcher.java`: file monitor lifecycle
- `reload/ManagerResourceImportPlanner.java`: folder import and single-file reload orchestration for animations, controllers, particles, entities, Bedrock models, and textures
- `hotkey/ManagerScreenKeybinds.java`: dedicated keybind registration
- `hotkey/ManagerScreenOpenEvents.java`: dedicated screen-open tick handling

## Read Only If Needed
- If the task is only about resource parsing, do not start here; go to `../../loader/README.md` instead.
- If the task is only about runtime asset lookup, go to `../../manager/` and its direct code instead.
