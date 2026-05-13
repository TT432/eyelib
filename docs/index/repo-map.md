# Eyelib Repo Map

## Start Here
- Root guidance: `AGENTS.md`
- Communication blueprint: `docs/architecture/ARCHITECTURE-BLUEPRINT.md`
- Boundary overview: `docs/architecture/01-module-boundaries.md`
- Side rules: `docs/architecture/02-side-boundaries.md`
- MC debt ledger: `docs/architecture/04-mc-debt-ledger.md`

## What This Repository Is
- Eyelib is a multi-project `Gradle + Java 17 + Forge` rendering library for Minecraft (`:` root runtime + `:eyelib-preprocessing` processing/batching core + `:eyelib-importer` importer/schema Forge functional module + `:eyelib-molang` engine Molang core + `:eyelib-material` material core + `:eyelib-particle` particle module boundary + active leaf utility module `:eyelib-util` + composite-build `clientsmoke` submodule).
- Forge bootstrap entrypoint: `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`
- Compatibility constant holder: `src/main/java/io/github/tt432/eyelib/Eyelib.java`
- Current codebase pressure points are client tooling, generated Molang grammar files, loader/publication flow, and sync/data-attachment boundaries.

## Where To Read By Topic
- Client rendering/runtime: start in `src/main/java/io/github/tt432/eyelib/client/`
- Loader and resource ingestion: start in `src/main/java/io/github/tt432/eyelib/client/loader/` for root-side reload orchestration and runtime adaptation flow; platform-free parsing/planning helpers should move into `:eyelib-preprocessing`, and importer-only parsing/normalization should move into `:eyelib-importer`
- Importer/model/schema functional module data and fixtures: start in `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/README.md`, then `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/model/` and nearby importer-owned schema packages; the current artifact is the `eyelibimporter` Forge mod, while a separate plain importer library remains future debt. Importer fixtures live under `eyelib-importer/src/test/resources/io/github/tt432/eyelib/client/model/importer/`
- Runtime asset storage: start in `src/main/java/io/github/tt432/eyelib/client/manager/`
- Molang: start in `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/` for value/runtime wrappers, scope/compiler/type/generated code; root `src/main/java/io/github/tt432/eyelib/molang/` is now a legacy marker/handoff path only
- Molang rewrite planning: read `eyelib-molang/ROADMAP.md` first for current progress and update rules, then use `eyelib-molang/refactor-plan/README.md` when the task is about rewrite sequencing, entry gates, or cutover parity
- Particle module boundary: start in `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/` for the `:eyelib-particle` module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable particle runtime, lifecycle, spawn/runtime adapters, render-manager behavior, `Dist.CLIENT` client integration, particle packet contracts under `io.github.tt432.eyelibparticle.network`, and loading/publication through `ParticleDefinitionRegistry` plus `ParticleResourcePublication` with active keys from `ParticleDefinition.identifier()`. Root owns Forge/resource adapter `BrParticleLoader`, temporary packet/root Minecraft/capability context adapter `ParticleSpawnService`, `mc/impl/common/command`, transport registration, and `NetClientHandlers` delegation; legacy root `client/particle/bedrock/**`, `ParticleLookup`, `ParticleManager`, `ParticleAssetRegistry`, and root `BrParticleRenderManager` have been deleted; importer owns raw `io.github.tt432.eyelibimporter.particle.BrParticle` schema/codec. Source `ResourceLocation` values are diagnostics metadata only, normal source tests must not read `.planning/` files, and Phase 14 keeps JetBrains MCP checks separate from ClientSmoke/hardware evidence while treating PFUT-03, unrelated fixture cleanup, and manual visual proof as non-blocking boundaries.
- Sync and packets: start in `src/main/java/io/github/tt432/eyelib/network/`; particle packet DTO/codecs for `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)` live under `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/`, while root `mc/impl/network` owns channel registration and packet context dispatch.
- Data attachment flow: start in `src/main/java/io/github/tt432/eyelib/util/data_attach/` and `src/main/java/io/github/tt432/eyelib/capability/`
- Shared utility boundaries: start in `eyelib-util/README.md` for the `:eyelib-util` Forge shared utility leaf module; active packages now include `io.github.tt432.eyelibutil.time`, `.color`, `.loader`, `.math`, `.search`, `.collection`, `.resource`, `.texture`, `.codec`, and `.streamcodec` consumed by root and approved sibling modules through explicit Gradle dependencies.
- Blockbench Bedrock export reference: `docs/blockbench/bedrock-geometry-export-fields-reference.md`
- External Bedrock reference docs: `docs/reference/`
- Client smoke framework: start in the `clientsmoke/` Git submodule; material-specific smoke tests live under `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/smoke/` so the framework does not depend on feature modules.

## Read In This Order
1. This file
2. Relevant architecture doc under `docs/architecture/`
3. The nearest package `README.md`
4. Only the code files required by the current task

## Hotspots
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- `src/main/java/io/github/tt432/eyelib/client/loader/`
- `src/main/java/io/github/tt432/eyelib/util/client/`
- `src/main/java/io/github/tt432/eyelib/network/` + `src/main/java/io/github/tt432/eyelib/util/data_attach/`
