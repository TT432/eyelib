# Resources Importer Animation/Entity Migration Plan

## Goal

Refactor the repository so that resource-import-only parts of animation, animation-controller, client-entity, and adjacent import flows move into `:eyelib-importer`, while runtime execution remains in root runtime packages or `mc/impl`.

## Outcome We Want

- `:eyelib-importer` owns schema/definition records, codecs, parsing, normalization, and importer-only intermediate image data.
- Root runtime owns managers, lookups, ticking, playback execution, particle/entity/runtime interaction, texture upload, preview UI, and Forge/Minecraft lifecycle integration.
- `NativeImage` no longer appears in importer-owned code.
- Loader → registry → manager patterns remain intact.

## Affected Modules

- `resources-importer`
- `src/main/java/io/github/tt432/eyelib/client/model/importer/`
- `src/main/java/io/github/tt432/eyelib/client/animation/`
- `src/main/java/io/github/tt432/eyelib/client/entity/`
- `src/main/java/io/github/tt432/eyelib/client/loader/`
- `src/main/java/io/github/tt432/eyelib/client/registry/`
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/`
- `src/main/java/io/github/tt432/eyelib/client/render/texture/`
- `src/main/java/io/github/tt432/eyelib/mc/impl/`
- `MODULES.md`
- `docs/index/repo-map.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md`
- package README files under `client/model`, `client/animation`, `client/entity`, `client/loader`, `client/registry`, `client/gui/manager`

## Observed Facts Driving The Plan

- `resources-importer/src/main/java/io/github/tt432/eyelib/client/model/bbmodel/Texture.java` directly decodes `NativeImage`.
- `resources-importer/src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelData.java` stores `NativeImage` in importer-owned DTOs and repacks atlas pixels directly.
- Root `ModelImporter` and `BlockbenchModelImporter` still expose `NativeImage` through `ImportResult.atlasNativeImage`.
- `ManagerResourceImportPlanner` directly loads and uploads `NativeImage` in runtime import flows.
- `BrClientEntity` is definition-heavy and codec-oriented.
- `BrAnimationController` is runtime-heavy and depends on runtime services such as `AnimationLookup`, `ParticleLookup`, `ParticleSpawnService`, `Entity`, `BrClientEntity`, and `ModelRuntimeData`.
- Existing architecture docs already require importer/model core in `:eyelib-importer` and runtime integration in root.

## Boundary Decisions

### Move into `:eyelib-importer`

- schema/definition records
- codec trees
- parser entrypoints
- normalization/repacking helpers
- importer-only image representation
- pure parser facades called by root loaders/tooling

### Keep in root runtime

- `Animation<?>` runtime executors
- playback state and runtime effect application
- runtime lookup seams
- manager-backed publication
- runtime model/application adapters
- `ClientEntityRuntimeData`
- `BrAnimator`
- `AnimationEffects`
- `RuntimeParticlePlayData`

### Keep in root or `mc/impl` only

- `Entity`
- `Minecraft`
- `NativeImage` upload/download
- Forge event posting
- platform identifier adaptation/validation at runtime boundaries

### Must not move as-is into `:eyelib-importer`

- `BrAnimationController`
- `BrAnimationEntry`
- `ParticleSpawnService`
- `AnimationLookup`
- `ParticleLookup`
- `ModelRuntimeData`
- bake/render visitors
- GUI import planner runtime orchestration

## Core Pattern To Preserve

Reuse the same shape already used by the model importer split:

`source file -> importer-side parsed/normalized DTOs -> root runtime adapter/build step -> manager publication/runtime use`

Apply that pattern to:

- animations
- animation controllers
- client entities
- attachables that currently duplicate client-entity parsing shape

## Proposed Importer-Owned Image Representation

Introduce a plain-JVM image type, recommended name: `ImportedImageData`.

### Recommended shape

- `int width`
- `int height`
- `int[] rgbaPixels`

### Required operations

- `getPixelRgba(int x, int y)`
- `setPixelRgba(int x, int y, int rgba)`
- `blit(...)`
- `empty(width, height)`
- decode helper from PNG bytes or equivalent importer-side source

### Why this shape

- importer atlas repacking only needs dimensions and pixel copy
- it is plain JVM data
- it removes Blaze3D/Minecraft runtime types from importer code
- root can convert it to `NativeImage` only at the runtime upload boundary

### Root adapter

Add a runtime-only adapter such as `NativeImageAdapters.toNativeImage(ImportedImageData)` and keep upload inside existing root runtime utilities.

## TDD Rule

Each phase begins with a failing test in the destination owner before code is moved.

- importer/schema behavior belongs in `:eyelib-importer`
- runtime adaptation/publication behavior belongs in root tests

Important existing tests to extend or relocate:

- `resources-importer/.../ImportedModelTextureRepackerTest`
- `resources-importer/.../BedrockImportedModelDataTest`
- `src/test/.../BlockbenchModelImporterTest`
- `src/test/.../BedrockGeometryImporterTest`
- `src/test/.../ClientEntityAssetRegistryTest`
- `src/test/.../ClientEntityRuntimeDataTest`
- `src/test/.../LoaderParsingOpsTest`
- `src/test/.../BrAnimationPlaybackStateTest`

## Staged Plan

### Phase 0 - Baseline And Boundary Freeze

#### Goal

Lock the ownership map before code moves.

#### Work

- re-baseline touched modules in `MODULES.md`
- document the target owner split in `docs/architecture/01-module-boundaries.md`
- confirm whether any side-boundary wording must change in `docs/architecture/02-side-boundaries.md`
- verify no extra subproject split is needed

#### Validation

- `./gradlew :eyelib-importer:test`
- `./gradlew test`

#### Ordering constraint

Must land before animation/entity extraction begins.

### Phase 1 - Extract Importer-Safe Prerequisites

#### Goal

Remove root-only utility and runtime assumptions that would block schema movement.

#### Work

- add `:eyelib-molang` as a dependency of `:eyelib-importer` if animation/entity schema needs Molang-owned types
- replace root-only helper dependencies used by schema candidates
- eliminate Minecraft-only utility usage from schema candidates, especially enum/codec helpers that cannot live in importer code
- prefer tiny importer-local replacements before broad utility churn

#### Known blockers to resolve here

- `BrLoopType`
- `BrAcState`
- `BrBoneAnimation`
- `BrBoneKeyFrame` and its Minecraft enum helper usage

#### Validation

- `./gradlew :eyelib-importer:compileJava`
- `./gradlew :eyelib-importer:test`
- `./gradlew test`

#### Ordering constraint

Animation schema must not move before this phase is green.

### Phase 2 - Replace Importer-Side `NativeImage`

#### Goal

Remove `NativeImage` from importer-owned model parsing and texture repacking.

#### Primary file targets

- `resources-importer/.../client/model/bbmodel/Texture.java`
- `resources-importer/.../client/model/importer/ImportedModelData.java`
- `resources-importer/.../client/model/importer/ImportedModelTextureRepacker.java`
- `src/main/java/.../client/model/importer/ModelImporter.java`
- `src/main/java/.../client/model/importer/BlockbenchModelImporter.java`
- `src/main/java/.../client/gui/manager/reload/ManagerResourceImportPlanner.java`

#### Work

- replace importer-side texture/image fields from `NativeImage` to `ImportedImageData`
- keep atlas repacking in `:eyelib-importer`, but operate on the importer image type
- change root importer result types so importer-facing results expose importer-owned image data instead of `NativeImage`
- convert to `NativeImage` only at runtime upload edges in root
- do not widen this phase into bake/render/runtime texture download code

#### Validation

- `./gradlew :eyelib-importer:test --tests "io.github.tt432.eyelib.client.model.importer.ImportedModelTextureRepackerTest"`
- `./gradlew :eyelib-importer:test --tests "io.github.tt432.eyelib.client.model.importer.BedrockImportedModelDataTest"`
- `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest"`
- `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BedrockGeometryImporterTest"`

#### Ordering constraint

This must land before broader parser migration so importer code has a clean image seam.

### Phase 3 - Move Client-Entity Schema Into `:eyelib-importer`

#### Goal

Move pure client-entity definitions first because they are the cleanest non-model candidate.

#### Primary file targets

- `src/main/java/.../client/entity/BrClientEntity.java`
- `src/main/java/.../client/entity/BrClientEntityScripts.java`
- `src/main/java/.../client/loader/BrClientEntityLoader.java`
- `src/main/java/.../client/loader/BrAttachableLoader.java`
- `src/main/java/.../client/gui/manager/reload/ManagerResourceImportPlanner.java`
- `src/main/java/.../client/registry/ClientEntityAssetRegistry.java`

#### Work

- move or recreate `BrClientEntity` schema in `:eyelib-importer`
- split `BrClientEntityScripts` into importer-side schema fields/codecs and root runtime helpers if evaluation logic still needs runtime support
- create importer parser entrypoints for both `minecraft:client_entity` and `minecraft:attachable`
- keep root loaders in place to preserve the loader pattern
- rewire loaders, planner, and registries to call importer parse functions and then publish through the existing registry layer
- keep `ClientEntityRuntimeData`, `ClientEntityLookup`, and render/runtime consumers in root

#### Validation

- add importer-side tests for client-entity and attachable parsing
- `./gradlew :eyelib-importer:test`
- `./gradlew test --tests "io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistryTest"`
- `./gradlew test --tests "io.github.tt432.eyelib.client.entity.ClientEntityRuntimeDataTest"`
- `./gradlew test --tests "io.github.tt432.eyelib.client.entity.ClientEntityLookupTest"`

#### Ordering constraint

Entity migration should precede animation/controller migration.

### Phase 4 - Split Animation And Controller Schema From Runtime Execution

#### Goal

Move parsed schema and codecs into `:eyelib-importer` without moving runtime executors.

#### Primary file targets

- `src/main/java/.../client/animation/bedrock/BrAnimation.java`
- `src/main/java/.../client/animation/bedrock/BrAnimationEntry.java`
- `src/main/java/.../client/animation/bedrock/controller/BrAnimationControllers.java`
- `src/main/java/.../client/animation/bedrock/controller/BrAnimationController.java`
- `src/main/java/.../client/animation/bedrock/controller/BrAcState.java`
- `src/main/java/.../client/animation/bedrock/controller/BrAcParticleEffect.java`
- `src/main/java/.../client/animation/bedrock/BrBoneAnimation.java`
- `src/main/java/.../client/animation/bedrock/BrBoneKeyFrame.java`
- `src/main/java/.../client/loader/BrAnimationLoader.java`
- `src/main/java/.../client/loader/BrAnimationControllerLoader.java`
- `src/main/java/.../client/gui/manager/reload/ManagerImportActions.java`
- `src/main/java/.../client/gui/manager/reload/ManagerResourceImportPlanner.java`
- `src/main/java/.../client/registry/AnimationAssetRegistry.java`

#### Work

- do not move `BrAnimationEntry` or `BrAnimationController` unchanged
- introduce importer-owned animation schema records and controller schema records
- keep runtime classes that implement `Animation<?>` and perform execution/state transitions in root
- add root-side builders/adapters that convert importer schema into runtime animation/controller executors
- keep `BrAnimationPlaybackState`, `AnimationLookup`, `ParticleLookup`, `ParticleSpawnService`, `ModelRuntimeData`, and `Entity`-touching logic in root
- rewire loaders and manager tooling to parse importer schema, build runtime objects, and publish them through `AnimationAssetRegistry`

#### Validation

- add importer-side parser/schema tests for animation/controller JSON
- `./gradlew :eyelib-importer:test`
- `./gradlew test --tests "io.github.tt432.eyelib.client.animation.bedrock.BrAnimationPlaybackStateTest"`
- `./gradlew test --tests "io.github.tt432.eyelib.client.registry.ClientAssetRegistryTest"`
- `./gradlew test`

#### Ordering constraint

Root runtime adapters must exist before loader output types change.

### Phase 5 - Rewire Mixed Import Hotspots

#### Goal

Make root import flows orchestration-only and keep parsing/normalization in `:eyelib-importer`.

#### Primary file targets

- `src/main/java/.../client/gui/manager/reload/ManagerResourceImportPlanner.java`
- `src/main/java/.../client/gui/manager/reload/ManagerImportActions.java`
- `src/main/java/.../client/model/importer/ModelImporter.java`
- `src/main/java/.../client/model/importer/BlockbenchModelImporter.java`
- `src/main/java/.../client/loader/LoaderParsingOps.java`

#### Work

- keep `ManagerResourceImportPlanner` in root, but reduce it to file classification, importer parse invocation, runtime adaptation, registry publication, texture upload, and runtime event posting
- keep model importer as a root runtime adapter over importer outputs
- preserve the existing loader → registry → manager flow
- remove duplicated parsing/normalization logic between loaders and manager tooling paths

#### Validation

- `./gradlew test --tests "io.github.tt432.eyelib.client.loader.LoaderParsingOpsTest"`
- `./gradlew test`
- execute the JetBrains run configuration `Client`
- smoke steps inside the launched client:
  1. open the existing manager screen flow used for resource import
  2. import or reload a folder that contains at least one model, one animation/controller resource, one client-entity resource, and one texture
  3. verify the import completes without runtime exceptions in the client log
  4. verify imported models/entities/animations appear in the same manager/runtime paths as before the refactor
  5. verify texture upload still succeeds after the importer image IR conversion
- expected result: the `Client` run launches successfully, the import flow completes without crashes or missing-resource regressions, and the affected assets remain visible/usable through the existing runtime and manager paths

#### Ordering constraint

Do this after schema movement, not before.

### Phase 6 - Documentation And Cleanup

#### Goal

Update ownership docs and package guidance in the same change that finalizes the new boundaries.

#### Required documentation updates

- `MODULES.md`
- `docs/index/repo-map.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md` if runtime/import wording changes
- `src/main/java/.../client/model/README.md`
- `src/main/java/.../client/animation/README.md`
- `src/main/java/.../client/entity/README.md`
- `src/main/java/.../client/loader/README.md`
- `src/main/java/.../client/registry/README.md`
- `src/main/java/.../client/gui/manager/README.md`
- add a package README under the new importer-owned animation/entity schema area

#### Validation

- verify every referenced path resolves
- `./gradlew :eyelib-importer:test`
- `./gradlew test`
- `./gradlew nullawayMain`

## Ordering Constraints Summary

- no dependency from `:eyelib-importer` back into root
- image seam before wider parser migration
- client-entity schema before animation/controller schema
- runtime adapters before loader output type flips
- docs updated in the same change that changes ownership
- no direct move of runtime executors into `:eyelib-importer`

## Main Risks

- moving `BrAnimationEntry` or `BrAnimationController` directly would drag runtime services and `Entity` into importer code
- animation schema classes currently depend on root utilities and need prerequisite cleanup first
- `BrBoneKeyFrame` currently relies on Minecraft enum helpers and cannot move as-is
- `BrClientEntityScripts` likely needs a split rather than a raw move
- `BrAttachableLoader` duplicates client-entity parsing shape and must be included to avoid split ownership
- `ManagerResourceImportPlanner` is a mixed hotspot; if not cleaned carefully, parser logic will remain duplicated
- image IR migration can subtly break atlas size and UV remapping behavior

## Recommended Atomic Commit Strategy

1. `prep resources-importer prerequisites for animation/entity schemas`
2. `replace importer NativeImage usage with importer-owned image data`
3. `move client-entity and attachable schema parsing into resources-importer`
4. `split animation/controller schemas from root runtime executors`
5. `rewire loaders and manager import planner to importer parsers`
6. `update module boundary docs and package READMEs`

Each commit must leave tests green and the module boundary coherent.

## Default Decisions To Use Unless The Human Says Otherwise

- keep canonical schema ownership in `:eyelib-importer`
- introduce root runtime adapter/executor names only where current classes mix schema and execution
- prefer importer-local helper extraction before broader utility churn

## Open Decisions

- whether moved schema classes should keep their current public names or whether root should keep thin compatibility wrappers
- whether shared pure helpers discovered during animation migration should remain importer-local or be promoted into `core/` or `:eyelib-molang`

## Definition Of Done

- `:eyelib-importer` owns importer-only schema/parsing/image representation for model + entity + animation/controller resources
- root owns runtime adapters, managers, lookups, and execution logic
- importer-owned code contains no `NativeImage`
- loaders and manager tooling use importer parsers rather than duplicating parse logic
- touched docs reflect the new ownership map
- required Gradle verification commands pass
