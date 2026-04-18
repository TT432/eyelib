# Eyelib Repo Map

## Start Here
- Root guidance: `AGENTS.md`
- Communication blueprint: `ARCHITECTURE-BLUEPRINT.md`
- Boundary overview: `docs/architecture/01-module-boundaries.md`
- Side rules: `docs/architecture/02-side-boundaries.md`
- Active refactor tracker: `work/main.md`

## What This Repository Is
- Eyelib is a multi-project `Gradle + Java 17 + Forge` rendering library for Minecraft (`:` root runtime + `:eyelib-importer` importer/model core + `:eyelib-molang` engine Molang core).
- Forge bootstrap entrypoint: `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`
- Compatibility constant holder: `src/main/java/io/github/tt432/eyelib/Eyelib.java`
- Current codebase pressure points are client tooling, generated Molang grammar files, loader/publication flow, and sync/data-attachment boundaries.

## Where To Read By Topic
- Client rendering/runtime: start in `src/main/java/io/github/tt432/eyelib/client/`
- Loader and resource ingestion: start in `src/main/java/io/github/tt432/eyelib/client/loader/` for root-side reload orchestration and runtime adaptation flow; importer-only parsing/normalization should move into `:eyelib-importer`
- Importer/model/schema core data and fixtures: start in `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/model/` and nearby importer-owned schema packages as client-entity/animation-controller parsing expands into the subproject; importer fixtures live under `eyelib-importer/src/test/resources/io/github/tt432/eyelib/client/model/importer/`
- Runtime asset storage: start in `src/main/java/io/github/tt432/eyelib/client/manager/`
- Molang: start in `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/` for value/runtime wrappers, scope/compiler/type/generated code; root `src/main/java/io/github/tt432/eyelib/molang/` is now a legacy marker/handoff path only
- Sync and packets: start in `src/main/java/io/github/tt432/eyelib/network/`
- Data attachment flow: start in `src/main/java/io/github/tt432/eyelib/util/data_attach/` and `src/main/java/io/github/tt432/eyelib/capability/`
- Platform-free utility seams: start in `src/main/java/io/github/tt432/eyelib/core/`
- Blockbench Bedrock export reference: `docs/blockbench/bedrock-geometry-export-fields-reference.md`
- External Bedrock reference docs: `docs/ref/`

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
