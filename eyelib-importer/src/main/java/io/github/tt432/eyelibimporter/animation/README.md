# Eyelib Importer Animation Schema Guide

## Scope
- Path: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/animation/`
- Owns importer-side animation/controller schema fragments and codecs that do not require runtime execution services.

## Current ownership
- Controller state/schema codecs such as `BrAcState`, `BrAcParticleEffect`, and `BrAnimationControllerSchema` belong here.
- Clip-side schema records such as `BrAnimationEntrySchema`, `BrBoneAnimationSchema`, and `BrBoneKeyFrameSchema` also remain importer-owned, even though root runtime still compiles them into executable animation objects.
- Root runtime still owns animation/controller execution, playback state, entity/particle/model integration, and runtime adapters.

## Current migration note
- Stage-1 characterization on the root side should lock the current importer-to-runtime bridge before larger refactors land.
- In particular, root tests should treat `BrAnimationEntry.fromSchema(...)`, `BrBoneAnimation.fromSchema(...)`, and `BrAnimationController.fromSchema(...)` as the current behavior seam from importer schema to runtime execution.

## Editing rules
- Do not add runtime tick logic, particle spawning, or manager publication here.
- If a type starts depending on `Animation<?>`, `Entity`, `Minecraft`, or runtime lookup/services, keep that part in root and adapt from importer-owned schema.
