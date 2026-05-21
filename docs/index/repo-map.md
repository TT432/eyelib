# Eyelib Repo Map

## Start Here
- Root guidance: `AGENTS.md`
- Module inventory: `MODULES.md`

## What This Repository Is
- Eyelib is a multi-project `Gradle + Java 17 + Forge` rendering library: root runtime plus 10 Gradle subprojects (`eyelib-animation`, `eyelib-attachment`, `eyelib-behavior`, `eyelib-importer`, `eyelib-material`, `eyelib-molang`, `eyelib-network`, `eyelib-particle`, `eyelib-preprocessing`, `eyelib-util`).

## Where To Read By Topic
- Client rendering/runtime: `src/main/java/io/github/tt432/eyelib/client/`
- Importer/schema: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/`
- Molang engine: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`
- Particles: `:eyelib-particle` owns module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, `ParticleDefinitionRegistry`, `ParticleResourcePublication`, keyed by `ParticleDefinition.identifier()`. Raw schema: `io.github.tt432.eyelibimporter.particle.BrParticle`. Network: `io.github.tt432.eyelibparticle.network`.
- Animation: `eyelib-animation/src/main/java/io/github/tt432/eyelibanimation/`
- Behavior: `eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/`
- Materials: `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`
- Data attachment: `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`
- Network: `eyelib-network/src/main/java/io/github/tt432/eyelibnetwork/` + `src/main/java/io/github/tt432/eyelib/network/`
- Shared utilities: `eyelib-util/src/main/java/io/github/tt432/eyelibutil/`
- Auth dependency graph: each subproject's `build.gradle` `project(:)` edges
- Phase 14 deferred scopes include ClientSmoke, hardware evidence, and PFUT-03 independent particle artifact publication.

## Read In This Order
1. `AGENTS.md`
2. Nearest package `README.md`
3. Only the code files required by the current task
