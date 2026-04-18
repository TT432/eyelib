# Client Animation Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/animation/`
- Contains runtime animation/controller execution, playback state, lookup seams, and transitional classes that currently still mix schema with runtime hooks.

## Boundary intent
- Parsed animation/controller schema and codecs are moving toward `:eyelib-importer`.
- Pure animation state and deterministic transition logic may remain outside `mc/impl` if they stay platform-type-free and plain-JVM-testable.
- Runtime animation hooks, controller execution tied to entity/particle/model runtime state, and schema-to-runtime adaptation stay root-owned and must not move into `:eyelib-importer` unchanged.

## Current split
- `BrAnimationPlaybackState` is the preferred example of a platform-free state seam.
- Heavy runtime/controller owners such as `BrAnimationEntry` and `BrAnimationController` remain transitional and are not direct move candidates.
- Importer-owned controller schema now exists in `:eyelib-importer` for state/effect/controller-set parsing (`BrAcState`, `BrAcParticleEffect`, `BrAnimationControllerSchema`, `BrAnimationControllerSet`), while root still owns runtime controller execution and schema-to-runtime adaptation.
- The remaining split is mainly on the animation-entry side, where `BrAnimationEntry`, `BrBoneAnimation`, and `BrBoneKeyFrame` still mix codec/state with runtime behavior.
- The current save/sync-facing surface is intentionally narrower than runtime state: `AnimationComponent.SerializableInfo` only carries animation bindings (`animations` + `animate`), while playback clocks, controller-local data, effect runtimes, and spawned particle handles remain runtime-only.
- Stage 1 characterization coverage for this package should lock current transitional behavior before larger refactors land, especially for `BrAnimationEntry.fromSchema(...)`, named-channel behavior in `BrBoneAnimation`, controller completion/state fallback semantics, and the current save/sync boundary around `AnimationComponent`.
- Stage 2 keeps `Animation<D>` as the public runtime surface but adds internal runtime ports (`identity/state/execution`) behind it so hotspot callers can start depending on narrower seams without changing save/sync shape or name-based lookup behavior.
- Stage 3 now introduces a pure clip-definition/sampler seam under the existing wrappers: `BrBoneAnimationDefinition`, `BrBoneKeyFrameDefinition`, and `BrBoneAnimationSampler`. `BrAnimationEntry`, `BrBoneAnimation`, and `BrBoneKeyFrame` still remain the compatibility surface for loaders, GUI preview, and runtime callers while delegating bone sampling to the new seam.
- Stage 4 now separates runtime ownership from execution inside the legacy wrappers: clip/runtime mutation lives in `BrClipStateOwner` + `BrClipExecutor`, controller/runtime mutation lives in `BrControllerStateOwner` + `BrControllerExecutor`, while `BrAnimationEntry.Data` / `BrAnimationController.Data` remain the compatibility wrappers stored by `AnimationComponent`.
- Narrow Stage 5 cleanup is complete for pure forwarding sites: internal callers that only used `AnimationRuntimes` for dispatch now call `Animation` untyped bridge methods directly. Loader/import, registry publication, GUI preview, lookup identity, and wrapper-shaped runtime ingress remain intentionally deferred because they still carry live compatibility roles.

## Editing rules
- Prefer expanding testable state seams first.
- Do not add new Minecraft/Forge dependencies to animation state/value classes unless they are explicitly transitional runtime owners.
- Do not move runtime executors into `:eyelib-importer` without first introducing importer-side schema records and root-side runtime adapters.
