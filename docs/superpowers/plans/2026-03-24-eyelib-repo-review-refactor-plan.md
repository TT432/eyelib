# Eyelib Repository Review & Boundary Refactor Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor Eyelib into clearer, documented subsystem boundaries with explicit public/internal ownership, lower context leakage for humans and AI, and safer staged migration in a single-module Forge codebase.

**Architecture:** The work proceeds in control-first stages: document current boundaries and side rules, isolate generated code, declare ownership and dependency policy, then extract high-risk hotspots behind narrow facades before moving deeper subsystems. The plan intentionally favors seam creation, local indexes, and compile-preserving extractions over broad package churn.

**Tech Stack:** Java 17, Gradle, Forge legacy moddev, Minecraft client/server runtime, Lombok, Mojang Codec, sparse JVM tests.

---

## Context

### Repository evidence
- `README.md`
- `README.cn.md`
- `build.gradle`
- `src/main/java/io/github/tt432/eyelib/Eyelib.java`
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java`
- `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`
- `src/main/java/io/github/tt432/eyelib/client/loader/BrResourcesLoader.java`
- `src/main/java/io/github/tt432/eyelib/molang/grammer/`
- `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`
- `src/main/java/io/github/tt432/eyelib/util/data_attach/DataAttachmentHelper.java`
- `src/main/java/io/github/tt432/eyelib/util/client/Textures.java`

### Current-state summary
- The repository is a single Gradle/Forge module and should remain so for this refactor.
- `client/` currently mixes rendering, animation, particles, GUI, loaders, managers, and tooling.
- `molang/grammer/` contains generated parser artifacts mixed into normal source reading paths.
- `EyelibManagerScreen.java` mixes screen composition, keybind opening, file dialogs, folder watching, resource import, and texture operations.
- `network/`, `capability/`, and `util/data_attach/` participate in shared runtime sync behavior and need explicit side and ownership rules before structural work.
- Documentation is minimal; there is no `docs/`, no `AGENTS.md`, and effectively no `src/test` tree.

### Refactor principles
- Preserve proven patterns already present in the repo: manager, loader, visitor, codec.
- Prefer documentation-backed boundaries before code moves.
- Do not rename packages unless the destination responsibility is explicit.
- Separate generated code from handwritten logic before broader Molang cleanup.
- In a Forge codebase, side boundaries matter as much as package boundaries.

## Target ownership model

This model is a destination guide, not a mandate to move everything immediately.

| Target zone | Intended responsibility | Current anchors |
|---|---|---|
| `bootstrap` | composition root, startup wiring, registration entrypoints | `Eyelib.java`, selected registration hooks |
| `api` | stable external or cross-subsystem contracts | currently sparse; to be carved from `Eyelib.java`/facades |
| `client.asset` | resource parsing, reload orchestration, import planning | `client/loader/` |
| `client.registry` | runtime storage and lookup for loaded assets | `client/manager/` |
| `client.render` | render pipeline, visitors, render params, material application | `client/render/`, parts of `util/client/` |
| `client.model` | model domain, bake flow, locator data | `client/model/` |
| `client.animation` | animation runtime and controllers | `client/animation/` |
| `client.particle` | particle runtime and component taxonomy | `client/particle/` |
| `client.tools` | developer/debug/editor UI, file import/watch tooling | `client/gui/manager/` |
| `molang.generated` | generated parser artifacts only | `molang/grammer/` |
| `molang.compiler` | compile/cache/class generation | `molang/compiler/` |
| `molang.runtime` | Molang runtime concepts and evaluation | remaining handwritten `molang/` code |
| `sync` | packet registration, sync services, client/server apply boundaries | `network/` |
| `dataattach` | typed attachment ownership and mutation helpers | `capability/`, `util/data_attach/` |
| `behavior` | behavior domain and logic | `common/behavior/` |
| `shared` | codecs, math, immutable or truly cross-cutting helpers | selected `util/` classes |

## Boundary controls

### Dependency rules
- Each subsystem must document allowed inbound and outbound dependencies before structural refactoring starts.
- New code must not be added to ambiguous catch-all zones such as `util.client` without a documented reason.
- Generated and handwritten code must not share the same ownership zone.
- Broad package renames are forbidden unless the destination responsibility and dependency policy are already documented.

### Side boundary matrix

| Side | May depend on | Must not depend on |
|---|---|---|
| common/shared | codecs, math, immutable data, pure helpers | client-only rendering/GUI classes |
| client-only | render/model/animation/particle/tooling, client handlers | server-only execution paths |
| sync layer | packet codecs, side gates, narrow client/server apply services | direct GUI or loader implementation details |
| data attachment | typed attachment mutation and read helpers | direct client rendering concerns |

### AI/human reading order
1. `AGENTS.md`
2. `docs/index/repo-map.md`
3. relevant `docs/architecture/*.md`
4. local package `README.md`
5. only then touched code files

## Current → target mapping checklist

| Current package/file | Target owner | Main rule |
|---|---|---|
| `src/main/java/io/github/tt432/eyelib/Eyelib.java` | `bootstrap` + temporary compatibility facade | keep as shell; stop growing direct singleton exposure |
| `src/main/java/io/github/tt432/eyelib/client/loader/` | `client.asset` | loaders parse/reload; publication belongs elsewhere |
| `src/main/java/io/github/tt432/eyelib/client/manager/` | `client.registry` | manager/event publication remains centralized |
| `src/main/java/io/github/tt432/eyelib/client/gui/manager/` | `client.tools` | screen composes UI; IO/watch/import move to services |
| `src/main/java/io/github/tt432/eyelib/molang/grammer/` | `molang.generated` | generated zone, excluded from normal manual edits |
| `src/main/java/io/github/tt432/eyelib/network/` | `sync` | packet routing and side application only |
| `src/main/java/io/github/tt432/eyelib/util/data_attach/` | `dataattach` | attachment mutation rules explicit and documented |
| `src/main/java/io/github/tt432/eyelib/util/client/` | destination-driven split | each class must earn a named home before moving |

## Stage order

```text
Stage 0 -> Stage 1 -> Stage 2 -> Stage 3 -> Stage 4 -> Stage 5 -> Stage 6 -> Stage 7 -> Stage 8 -> Stage 9

Hard dependencies:
- Stage 0 before all code refactors
- Stage 1 before all subsystem-local work
- Stage 2 before loader/manager or sync/dataattach reshaping
- Stage 3 before broader Molang cleanup
- Stage 4 before trimming EyelibManagerScreen aggressively
- Stage 5 before particle boundary hardening
- Stage 8 after side and runtime-storage rules are already documented
```

## Tasks

### Task 0: Write control docs and repo map

**Files:**
- Create: `AGENTS.md`
- Create: `docs/architecture/00-control-spec.md`
- Create: `docs/architecture/01-module-boundaries.md`
- Create: `docs/architecture/02-side-boundaries.md`
- Create: `docs/index/repo-map.md`
- Modify: `README.md`
- Modify: `README.cn.md`

- [ ] Add a root `AGENTS.md` that tells humans and AI to read the nearest package README before editing, treat `molang/grammer/` as generated, and verify with compile/build before claiming completion.
- [ ] Write `docs/architecture/00-control-spec.md` with stage goals, non-goals, rollback policy, allowed temporary shims, and forbidden broad rewrites.
- [ ] Write `docs/architecture/01-module-boundaries.md` with current subsystem responsibilities and the current→target mapping table.
- [ ] Write `docs/architecture/02-side-boundaries.md` defining common/client/sync/dataattach boundaries and side-gated entrypoints.
- [ ] Write `docs/index/repo-map.md` as the root navigation page for humans and AI.
- [ ] Update both root READMEs to point at the new docs.
- [ ] Validation:
  - Command/tool: open `README.md`, `README.cn.md`, `AGENTS.md`, `docs/architecture/00-control-spec.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and `docs/index/repo-map.md` in the editor.
  - Steps: verify every referenced file/path in those documents exists and every “start here” link resolves to an existing file.
  - Expected result: all referenced files open successfully and no path in the control docs points to a missing document.

### Task 1: Add subsystem indexes next to code

**Files:**
- Create: `docs/index/client.md`
- Create: `docs/index/molang.md`
- Create: `docs/index/network.md`
- Create: `docs/index/util.md`
- Create: `src/main/java/io/github/tt432/eyelib/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/client/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/client/gui/manager/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/client/loader/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/molang/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/molang/grammer/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/network/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/util/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/util/data_attach/README.md`

- [ ] Document each subsystem’s responsibility, entrypoints, hotspots, and “do not read unless” guidance.
- [ ] In `client/gui/manager/README.md`, point to `EyelibManagerScreen.java`, `EntitiesListPanel.java`, and `DragTargetWidget.java` and describe the intended extraction seams.
- [ ] In `client/loader/README.md`, document the loader pattern and current publication side effects.
- [ ] In `molang/grammer/README.md`, explicitly mark the package generated/read-only.
- [ ] In `network/README.md` and `util/data_attach/README.md`, document the packet-to-attachment flow.
- [ ] Validation:
  - Command/tool: open each subsystem index and local package README created in this stage in the editor.
  - Steps: verify every referenced Java file or directory exists and each README points back to a valid root/index document.
  - Expected result: all subsystem indexes resolve to real files and form a consistent two-way navigation path from root docs to local package docs.

### Task 2: Isolate generated Molang code first

**Files:**
- Modify or move: `src/main/java/io/github/tt432/eyelib/molang/grammer/`
- Modify: `src/main/java/io/github/tt432/eyelib/molang/compiler/MolangCompileHandler.java`
- Modify: `src/main/java/io/github/tt432/eyelib/molang/compiler/MolangCompileVisitor.java`
- Create: `docs/architecture/03-generated-code-policy.md`
- Create: `src/main/java/io/github/tt432/eyelib/molang/generated/README.md`
- Modify: `src/main/java/io/github/tt432/eyelib/molang/README.md`

- [ ] Document the generated-code policy before any file moves.
- [ ] Choose the lowest-risk isolation strategy: either move parser artifacts into a clearly generated namespace or keep them in place temporarily with an explicit adapter seam and strict docs.
- [ ] Reduce direct compiler imports of raw generated classes where a local parser-entry seam can narrow context.
- [ ] Validation:
  - Command: `./gradlew compileJava`
  - Expected result: exit code `0` and no Java compilation errors after the generated-code isolation step.

### Task 3: Declare public/internal seams without broad moves

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/Eyelib.java`
- Create: `src/main/java/io/github/tt432/eyelib/package-info.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/package-info.java`
- Create: `src/main/java/io/github/tt432/eyelib/network/package-info.java`
- Create: `src/main/java/io/github/tt432/eyelib/util/data_attach/package-info.java`
- Create: `src/main/java/io/github/tt432/eyelib/api/README.md`
- Create: `src/main/java/io/github/tt432/eyelib/internal/README.md`
- Modify: `docs/architecture/01-module-boundaries.md`

- [ ] Inventory every symbol surfaced through `Eyelib.java` and classify it as public facade, transitional bridge, or internal leak.
- [ ] Keep `Eyelib.java` as a compatibility shell for this stage; do not do broad package moves yet.
- [ ] Add package-level docs that mark client/network/dataattach internals explicitly.
- [ ] Introduce narrow facades only where they reduce singleton reach-through meaningfully.
- [ ] Validation:
  - Command: `./gradlew compileJava`
  - Expected result: exit code `0` and no compilation errors after public/internal seam declarations.

### Task 4: Define storage/import/watch seams before splitting the manager screen

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/gui/manager/io/FileDialogService.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceFolderWatcher.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java`
- Update: `src/main/java/io/github/tt432/eyelib/client/gui/manager/README.md`
- Optional test: `src/test/java/io/github/tt432/eyelib/client/gui/manager/ManagerResourceImportPlannerTest.java`

- [ ] Extract the folder-watch/import-planning seam before extracting UI composition.
- [ ] Move file dialogs and watch logic into services, but keep screen composition centralized in `EyelibManagerScreen.java`.
- [ ] Only add tests for pure import-planning or path-filter logic if the extraction yields plain Java helpers.
- [ ] Validation:
  - Command 1: `./gradlew compileJava`
  - Expected result 1: exit code `0` and no compilation errors after introducing import/watch services.
  - Command 2: `./gradlew runClient`
  - Steps: launch the dev client, open the manager screen using the existing keybind/event path, trigger the relevant file dialog or watched-folder flow for this stage.
  - Expected result 2: the manager screen opens without exceptions, and the extracted import/watch flow runs through the new service boundary instead of failing inside the screen class.

### Task 5: Separate asset parsing from runtime storage

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrResourcesLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrAttachableLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrAnimationLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrMaterialLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrModelLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/manager/Manager.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/registry/ClientAssetRegistry.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/registry/README.md`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/README.md`

- [ ] Document the rule: loaders parse/reload; registries/managers own publication and lookup.
- [ ] Move direct manager mutation out of loader paths where feasible.
- [ ] Preserve the manager/event pattern centered on `Manager.java`.
- [ ] Validation:
  - Command: `./gradlew classes`
  - Expected result: exit code `0` and no compilation/resource-processing errors after separating loader parsing from registry publication.

### Task 6: Finish trimming `EyelibManagerScreen`

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/EntitiesListPanel.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/DragTargetWidget.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/gui/manager/hotkey/ManagerScreenKeybinds.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/gui/manager/hotkey/ManagerScreenOpenEvents.java`

- [ ] Extract hotkey opening and remaining non-UI orchestration out of the screen.
- [ ] Leave the screen responsible for widget composition and delegating actions only.
- [ ] Validation:
  - Command 1: `./gradlew compileJava`
  - Expected result 1: exit code `0` and no compilation errors after trimming remaining screen responsibilities.
  - Command 2: `./gradlew runClient`
  - Steps: launch the dev client, open the manager screen through the configured keybind/event path, and confirm the screen still renders and responds to its primary interactions.
  - Expected result 2: the screen opens and remains interactive without exceptions after hotkey/open-event extraction.

### Task 7: Decompose `util.client` only into named destinations

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/util/client/Textures.java`
- Modify: `src/main/java/io/github/tt432/eyelib/util/client/PoseHelper.java`
- Modify: `src/main/java/io/github/tt432/eyelib/util/client/ModelResourceLocationHelper.java`
- Create: `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java`
- Create: `src/main/java/io/github/tt432/eyelib/util/client/texture/TextureLayerMerger.java`
- Create: `src/main/java/io/github/tt432/eyelib/util/client/render/PoseCopies.java`
- Create: `src/main/java/io/github/tt432/eyelib/util/client/model/ModelTexturePacking.java`
- Optional tests: `src/test/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelperTest.java`

- [ ] Move only deterministic, destination-clear logic first.
- [ ] Use `Textures.java` as a temporary facade until callers are migrated.
- [ ] Add JVM tests only for pure helpers.
- [ ] Validation:
  - Command 1: `./gradlew compileJava`
  - Expected result 1: exit code `0` and no compilation errors after destination-driven `util.client` extraction.
  - Command 2: `./gradlew test` (only if tests were added in this stage)
  - Expected result 2: exit code `0` and the new helper-focused test classes pass.

### Task 8: Harden particle runtime boundaries

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/particle/README.md`

- [ ] Replace networking’s direct dependence on loader implementation details with lookup/service boundaries.
- [ ] Make runtime particle lookup come from manager/service read-side boundaries.
- [ ] Validation:
  - Command 1: `./gradlew compileJava`
  - Expected result 1: exit code `0` and no compilation errors after introducing particle lookup/service boundaries.
  - Command 2: `./gradlew runClient`
  - Steps: launch the dev client, execute the existing particle/dev flow available in the repo, and confirm the particle path resolves through the new lookup/service boundary.
  - Expected result 2: particle spawning still works without exceptions and no runtime path depends directly on loader implementation classes.

### Task 9: Align sync and data-attachment boundaries

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`
- Modify: `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java`
- Modify: `src/main/java/io/github/tt432/eyelib/network/UniDataUpdatePacket.java`
- Modify: `src/main/java/io/github/tt432/eyelib/util/data_attach/DataAttachmentHelper.java`
- Create: `src/main/java/io/github/tt432/eyelib/network/dataattach/DataAttachmentSyncService.java`
- Create: `src/main/java/io/github/tt432/eyelib/network/dataattach/README.md`
- Modify: `docs/architecture/02-side-boundaries.md`

- [ ] Write down authoritative write rules and client-apply rules before moving code.
- [ ] Separate generic attachment sync flow from unrelated packet concerns.
- [ ] Avoid renaming churn unless already touching a file for a boundary reason.
- [ ] Validation:
  - Command: `./gradlew compileJava`
  - Expected result: exit code `0` and no compilation errors after sync/data-attachment boundary alignment.

### Task 10: Final cleanup and doc synchronization

**Files:**
- Modify: `README.md`
- Modify: `README.cn.md`
- Modify: all touched docs under `docs/architecture/` and `docs/index/`
- Modify: local package `README.md` files created above

- [ ] Remove obsolete temporary bridges only after imports are migrated.
- [ ] Sync docs to final paths and rules.
- [ ] Re-run compile and any added unit tests.
- [ ] Add a short migration note describing what moved and what intentionally stayed stable.
- [ ] Validation:
  - Command 1: `./gradlew compileJava`
  - Expected result 1: exit code `0` and no compilation errors after final cleanup.
  - Command 2: `./gradlew test` (if tests were added in earlier stages)
  - Expected result 2: exit code `0` and all added helper-focused tests pass.

## Validation strategy

- Docs-only stages: manual path/link review.
- Structure-changing stages: `./gradlew compileJava` minimum.
- Resource/loader stages: `./gradlew classes` preferred.
- GUI/runtime-sensitive stages: compile plus targeted manual smoke through existing client run flow.
- Tests should be added only where refactoring extracts plain or mostly plain Java helpers; do not fabricate a broad Forge runtime harness.

## Optional commit slicing

Commits are optional because no commit was requested. If implementation later proceeds with commits, prefer one bounded stage per commit.

## Success criteria

- Public/internal/generated/side boundaries are documented before broad code moves.
- Humans and AI can navigate from root docs to subsystem-local docs without reading unrelated packages.
- `EyelibManagerScreen.java`, loader/publication flow, `util.client`, and sync/data-attach flow all have narrower responsibilities.
- The plan preserves current working patterns instead of replacing them with architecture for architecture’s sake.
