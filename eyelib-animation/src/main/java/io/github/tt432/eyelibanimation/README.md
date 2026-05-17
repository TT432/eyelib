# Eyelib Animation Module
## Scope
Animation runtime, Bedrock clip/controller state machines, keyframe interpolation.
## Domain Boundaries
- Owns: `Animation`, `BrAnimationEntry`, `BrAnimationController`, `ModelRuntimeData`, `AnimationComponent`, `AnimationManager`, `AnimationLookup`.
- Depends on: `:eyelib-molang`, `:eyelib-importer`, `:eyelib-util`, `:eyelib-particle`, `:eyelib-preprocessing`, `:eyelib-attachment`.
- Must not depend on root runtime packages.
