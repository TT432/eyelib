# Eyelib Animation Runtime Refactor Plan

## Goal

Refactor Eyelib’s animation runtime into a clearer split between identity, definition, state, and execution without breaking the current intermediate implementation.

This plan explicitly reconciles the current bridge state:
- `BrBoneAnimation` now has `Channel -> KeyFrame` internally.
- importer-owned animation/controller schema already exists in `:eyelib-importer`.
- root runtime still owns animation/controller execution.
- `Animation.java` still mixes too many roles and remains the main design knot.

The target is a Java-friendly composite runtime that preserves nested controllers without spreading `Either`/union-style branching across the codebase.

## Current State

- `src/main/java/io/github/tt432/eyelib/client/animation/Animation.java` mixes identity (`name()`), lifecycle (`onFinish`), state factory (`createData()`), completion queries, and execution (`tickAnimation(...)`).
- `BrAnimationEntry` is still the most overloaded type. It adapts importer schema, owns runtime effect logic, owns playback data shape, applies bone transforms, and touches particles/sounds directly.
- `BrBoneAnimation` now has named channels internally, but it still mixes definition-like storage, schema conversion, legacy accessors, and sampling/interpolation behavior.
- `BrBoneKeyFrame` still mixes keyframe data, codec factory behavior, and interpolation helpers.
- `BrAnimationController` already sits on top of importer-owned controller schema, but still owns runtime state-machine execution and nested animation dispatch through `AnimationLookup`.
- `BrAnimationPlaybackState` is the cleanest existing state seam and should be treated as the template for the rest of the split.
- `AnimationComponent.SerializableInfo` is the current save/sync-facing surface, but runtime state actually lives elsewhere in `animationData`, `AnimationEffects`, and `RuntimeParticlePlayData`.
- `BrAnimation` and `BrAnimationControllers` are already acting like transitional schema-set wrappers/builders more than durable runtime architecture.

## Target Architecture

- Identity is small and stable. Candidate type: `AnimationId`.
- Definition is immutable and runtime-owned, but built from importer-owned schema. Candidate types: `AnimationDefinition`, `AnimationNodeDefinition`, `ClipAnimationDefinition`, `ControllerAnimationDefinition`.
- State is per-instance mutable runtime data only. Candidate types: `AnimationNodeState`, `ClipAnimationState`, `ControllerAnimationState`, `AnimationEffectState`.
- Execution is stateless service logic over `definition + state + context`. Candidate types: `AnimationExecutor`, `ClipAnimationExecutor`, `ControllerAnimationExecutor`, `AnimationRuntimeContext`.
- Nested controllers stay composite. A controller should execute child animation nodes through one node contract plus executor dispatch, not through repeated `instanceof` or `Either<Clip, Controller>` branching at call sites.
- Importer keeps codecs and raw schema normalization. Root runtime keeps compiled definitions, state, execution, manager publication, and runtime integration.
- Save/sync-facing data stays narrow. The serializable surface should describe animation bindings and inputs, not transient runtime playback/effect state.

## Save/Sync Rule

- `AnimationComponent.SerializableInfo` or its staged successor should represent only definition references and binding inputs such as `animations` and `animate`.
- Playback clocks, loop counters, current controller state, effect cursors, blend progress, and spawned particle handles are runtime-only and should not be persisted or treated as durable wire state.
- Every stage that changes this split must update the matching documentation in `MODULES.md`, `docs/architecture/01-module-boundaries.md`, and `src/main/java/io/github/tt432/eyelib/client/animation/README.md`.

## Compatibility Shims To Carry Temporarily

- Keep `Animation<D>` as a legacy facade until the new definition/state/executor split is fully wired.
- Keep `BrBoneAnimation.rotation()`, `position()`, and `scale()` as wrappers over named channels until all callers move to channel-based or definition-based access.
- Keep `AnimationLookup` string-keyed during migration even if the backing runtime becomes definition/executor based.
- Keep `BrAnimation` and `BrAnimationControllers` as loader-facing wrappers until loaders and manager tooling publish compiled definitions directly.
- Keep `AnimationComponent.SerializableInfo` stable until a replacement adapter exists and render/network callers are migrated.

## Candidate Types And Interfaces To Introduce

These are plan items, not implementation code.

- `AnimationId`: stable runtime identity wrapper over current string keys.
- `AnimationDefinition`: immutable root contract for runtime-known animation assets.
- `AnimationNodeDefinition`: composite node contract shared by clips and controllers.
- `ClipAnimationDefinition`: compiled root definition for former `BrAnimationEntry` behavior.
- `ControllerAnimationDefinition`: compiled root definition for former `BrAnimationController` behavior.
- `BoneChannelDefinition`: compiled channel definition built from current `BrBoneAnimation.channels()`.
- `BoneKeyframeDefinition`: pure keyframe value definition split from runtime sampling behavior.
- `AnimationNodeState`: common runtime state marker/owner.
- `ClipAnimationState`: playback state, effect cursors, transient clip-local data.
- `ControllerAnimationState`: current state node, start tick, nested child state references.
- `AnimationRuntimeContext`: Molang scope, model runtime data, effect sinks, particle services.
- `AnimationExecutor`: stateless executor contract for ticking and applying a node.
- `LegacyAnimationAdapter`: temporary adapter implementing current `Animation<D>` over new definitions and executors.
- `AnimationBindingInfo`: successor or alias for `AnimationComponent.SerializableInfo`.

## Stage 1: Freeze Boundaries And Add Characterization Tests

- Goal: lock down current behavior and document the intended split before runtime surgery starts.
- Depends on: none.
- Affected files/modules: `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `src/main/java/io/github/tt432/eyelib/client/animation/README.md`, `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/animation/README.md`, `work/client-model-animation-entity.md`, `src/test/java/io/github/tt432/eyelib/client/animation/**`, `src/test/java/io/github/tt432/eyelib/capability/component/**`, `src/test/java/io/github/tt432/eyelib/client/render/sync/**`.
- Main refactors: document the current state versus target state versus transitional shims; add characterization tests for `BrAnimationEntry.fromSchema(...)`, named-channel behavior in `BrBoneAnimation`, controller transition behavior, and the current save/sync shape around `AnimationComponent.SerializableInfo`.
- Verification: run `:eyelib-importer:test`, then targeted root tests for animation playback, channel behavior, controller codecs, render-sync apply, and animation component invalidation.
- Rollback notes: this stage should be docs and tests only; if behavior is ambiguous, keep the old behavior and tighten the tests/documentation first.

## Stage 2: Introduce Split Runtime Ports Behind `Animation<D>`

- Goal: create the new runtime vocabulary without forcing a full caller cutover.
- Depends on: Stage 1.
- Affected files/modules: `src/main/java/io/github/tt432/eyelib/client/animation/Animation.java`, `BrAnimator.java`, `AnimationLookup.java`, `src/main/java/io/github/tt432/eyelib/client/manager/AnimationManager.java`, `src/main/java/io/github/tt432/eyelib/client/registry/AnimationAssetRegistry.java`, `src/main/java/io/github/tt432/eyelib/capability/component/AnimationComponent.java`, `src/main/java/io/github/tt432/eyelib/client/animation/README.md`.
- Main refactors: introduce `AnimationId`, `AnimationDefinition`, `AnimationNodeDefinition`, `AnimationNodeState`, `AnimationExecutor`, and `AnimationRuntimeContext`; make `Animation<D>` a compatibility facade instead of the architectural center; add `LegacyAnimationAdapter`; define the planned `AnimationBindingInfo` boundary while keeping `AnimationComponent.SerializableInfo` stable.
- Verification: add plain-JVM tests for the new adapter layer and binding-info behavior, then compile root plus importer and run targeted adapter tests.
- Rollback notes: if a caller cutover is too invasive, keep the legacy interface as the public entrypoint and only redirect internal construction through the new types.

## Stage 3: Split Clip Definitions From Clip Execution

- Goal: use the existing named-channel work as the bridge into a real definition/runtime split on the animation-entry side.
- Depends on: Stage 2.
- Affected files/modules: `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java`, `BrBoneAnimation.java`, `BrBoneKeyFrame.java`, `BrAnimation.java`, `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/animation/bedrock/BrAnimationEntrySchema.java`, `BrBoneAnimationSchema.java`, `BrBoneKeyFrameSchema.java`, `src/test/java/io/github/tt432/eyelib/client/animation/bedrock/**`.
- Main refactors: extract compiled immutable definitions for clip entry, bone channel, and keyframe data; make `BrBoneAnimation.channels()` the canonical internal bridge while keeping legacy channel accessors as shims; move interpolation and sampling behavior into dedicated runtime services such as a `BoneChannelSampler`; move codec-oriented construction fully to importer schema plus root-side builders so compiled clip definitions are codec-free.
- Verification: extend `BrBoneAnimationChannelTest`; add pure sampler/keyframe tests; run importer tests plus targeted root tests for clip-definition builders and samplers.
- Rollback notes: retain `BrAnimationEntry` and `BrBoneAnimation` as thin wrappers over the new compiled definitions if direct caller replacement proves too large for one slice.

## Stage 4: Split Mutable State And Execution For Clips And Controllers

- Goal: move runtime state out of definition holders and make nested controller execution composite and typed.
- Depends on: Stage 3.
- Affected files/modules: `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java`, `BrAnimationPlaybackState.java`, `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrAnimationController.java`, `AnimationEffect.java`, `AnimationEffects.java`, `RuntimeParticlePlayData.java`, `BrAnimator.java`, `src/main/java/io/github/tt432/eyelib/capability/component/AnimationComponent.java`, `src/main/java/io/github/tt432/eyelib/client/render/sync/RenderSyncApplyOps.java`, `ClientRenderSyncService.java`.
- Main refactors: introduce `ClipAnimationState`, `ControllerAnimationState`, and effect-state owners; move clip tick/apply logic into `ClipAnimationExecutor`; move controller state-machine logic into `ControllerAnimationExecutor`; replace untyped `Map<String, Object> animationData` growth with typed state ownership keyed by `AnimationId` or node path; explicitly define save/sync behavior so bindings serialize but runtime playback/effect state does not.
- Verification: add controller transition tests, clip loop/reset tests, binding-info round-trip tests, and nested-controller executor tests; run targeted tests first, then full root test task.
- Rollback notes: keep `createData()` and the old nested `Data` classes as adapter-backed wrappers until the new state owners are fully proven by tests.

## Stage 5: Cut Over Callers, Remove Shims, And Rebaseline Docs

- Goal: make the split the real architecture instead of an internal compatibility layer.
- Depends on: Stage 4.
- Affected files/modules: `src/main/java/io/github/tt432/eyelib/client/animation/Animation.java`, `AnimationLookup.java`, `src/main/java/io/github/tt432/eyelib/client/registry/AnimationAssetRegistry.java`, `src/main/java/io/github/tt432/eyelib/client/loader/BrAnimationLoader.java`, `BrAnimationControllerLoader.java`, `src/main/java/io/github/tt432/eyelib/client/gui/manager/AnimationView.java`, `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java`, `BrAnimation.java`, `BrAnimationControllers.java`, `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `src/main/java/io/github/tt432/eyelib/client/animation/README.md`.
- Main refactors: switch loaders, registry publication, GUI/runtime inspection, and lookup seams to compiled definitions plus executors; remove `BrAnimation` and `BrAnimationControllers` if they no longer add value; demote or delete legacy `Animation<D>` and old clip/controller wrapper behavior; remove compatibility channel accessors once all callers use definition-driven access.
- Verification:
  - Run importer tests plus root tests.
  - Run the JetBrains IDE `Client` run configuration for the final client smoke check.
  - Prepare a temporary resource folder using the exact minimal JSON fixtures already exercised by tests:
    - `animations/test.animation.json`
      ```json
      {
        "animations": {
          "animation.test.idle": {
            "loop": "true",
            "animation_length": 1.5,
            "bones": {
              "body": {
                "rotation": {
                  "0.0": [0.0, 0.0, 0.0]
                }
              }
            }
          }
        }
      }
      ```
    - `animation_controllers/test.controller.json`
      ```json
      {
        "animation_controllers": {
          "controller.animation.test": {
            "initial_state": "default",
            "states": {
              "default": {
                "animations": ["animation.test.idle"]
              }
            }
          }
        }
      }
      ```
  - Import that folder through the existing manager tooling path backed by `ManagerResourceImportPlanner.loadResourceFolder(...)`.
  - Expected smoke result:
    - client launches without animation/controller load exceptions,
    - folder import completes without codec or planner warnings,
    - both `animation.test.idle` and `controller.animation.test` become visible in the manager/runtime asset view that reflects the same publication path covered by `ClientAssetRegistryTest`,
    - controller-backed playback still starts from the imported controller without regressing the clip import path.
- Rollback notes: only remove shims after the smoke flow passes; if the final cutover fails, restore adapter-backed lookup/publication and keep the new internals hidden behind the legacy facade for one more slice.

## Atomic Commit Strategy

- Commit 1: docs and characterization tests only.
- Commit 2: new runtime ports and legacy adapter, with no intended behavior change.
- Commit 3: clip-definition split around `BrAnimationEntry`, `BrBoneAnimation`, and `BrBoneKeyFrame`, with legacy wrappers kept.
- Commit 4: state/executor split for clips and controllers plus explicit save/sync boundary.
- Commit 5: caller cutover, shim removal, and documentation rebaseline.

Each commit should stay reviewable on its own and should leave the repository in a green state.

## TDD Execution Rule

- Start each stage with a failing test that describes the destination behavior of that stage.
- Keep importer/schema assertions in `:eyelib-importer` tests.
- Keep runtime adaptation/execution assertions in root tests.
- Prefer plain-JVM seam tests first, then compile/build verification, then dev-client smoke only when runtime behavior actually changed.
- Do not delete the old path in a stage until the new path is covered by tests and the adapter is green.

## Execution Notes

- Execute stages serially; do not overlap Stage 3 and Stage 4.
- Re-read `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and the nearest package `README.md` before each implementation stage.
- If a stage changes module ownership or documentation intent, update the matching docs in the same commit.
- Do not move runtime execution into `:eyelib-importer`.
- Do not re-expand `Animation.java` during migration; every new responsibility should land in definition/state/executor types instead.
- If the repo still has no checked-in animation/controller smoke fixtures by Stage 5, create the temporary folder from the exact JSON above instead of inventing a new manual asset.

## Exit Criteria

- `Animation.java` is no longer the architectural center of the runtime.
- `BrAnimationEntry`, `BrBoneAnimation`, and `BrBoneKeyFrame` no longer mix codec/definition/state/execution responsibilities.
- Nested controllers execute through a composite node model without union-type pain at call sites.
- The save/sync surface is explicitly limited to bindings and inputs.
- Loader, registry, manager, and runtime boundaries still match the repo’s existing write/read/sync patterns.
- Docs and tests describe the final staged architecture accurately.
