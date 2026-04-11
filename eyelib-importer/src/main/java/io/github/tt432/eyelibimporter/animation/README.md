# Eyelib Importer Animation Schema Guide

## Scope
- Path: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/animation/`
- Owns importer-side animation/controller schema fragments and codecs that do not require runtime execution services.

## Current ownership
- Controller state/schema codecs such as `BrAcState`, `BrAcParticleEffect`, and `BrAnimationControllerSchema` belong here.
- Root runtime still owns animation/controller execution, playback state, entity/particle/model integration, and runtime adapters.

## Editing rules
- Do not add runtime tick logic, particle spawning, or manager publication here.
- If a type starts depending on `Animation<?>`, `Entity`, `Minecraft`, or runtime lookup/services, keep that part in root and adapt from importer-owned schema.
