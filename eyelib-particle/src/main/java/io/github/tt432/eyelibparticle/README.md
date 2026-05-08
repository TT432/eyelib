# Eyelib Particle Module

## Scope
- Path: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`
- Owns the new particle-module API and core contract boundary for future particle extraction work.
- Phase 8 creates only the Gradle/source/resource skeleton; current runtime behavior remains under `src/main/java/io/github/tt432/eyelib/client/particle/` until later phases move it through explicit seams.

## Dependency Direction
- Root runtime may consume `:eyelib-particle` through declared Gradle project dependencies.
- `:eyelib-particle` must not depend back on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core should remain root-independent and platform-free unless a future phase documents a narrower adapter boundary.
- Minecraft/Forge lifecycle wiring, command registration, transport handling, and client hooks must live in explicit integration layers before they are introduced.
- Existing particle loading, command, network, and render behavior must not be moved into this module during the skeleton phase.
