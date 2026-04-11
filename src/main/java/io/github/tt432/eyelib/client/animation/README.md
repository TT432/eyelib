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

## Editing rules
- Prefer expanding testable state seams first.
- Do not add new Minecraft/Forge dependencies to animation state/value classes unless they are explicitly transitional runtime owners.
- Do not move runtime executors into `:eyelib-importer` without first introducing importer-side schema records and root-side runtime adapters.
