# Eyelib Behavior Module
## Scope
- Path: `eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/`
- Bedrock entity behavior component model.
## Domain Boundaries
- Owns: `BehaviorEntity`, `EntityBehaviorData`, all component types, event filter/logic nodes.
- Depends on: `:eyelib-util`, `com.mojang:datafixerupper`.
- Must not depend on root runtime packages (`io.github.tt432.eyelib.*`).
