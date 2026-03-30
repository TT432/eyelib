# Eyelib Repo Map

## Start Here
- Root guidance: `AGENTS.md`
- Communication blueprint: `ARCHITECTURE-BLUEPRINT.md`
- Boundary overview: `docs/architecture/01-module-boundaries.md`
- Side rules: `docs/architecture/02-side-boundaries.md`
- Full staged plan: `docs/superpowers/plans/2026-03-24-eyelib-repo-review-refactor-plan.md`

## What This Repository Is
- Eyelib is a single-module `Gradle + Java 17 + Forge` rendering library for Minecraft.
- Current public-ish entrypoint: `src/main/java/io/github/tt432/eyelib/Eyelib.java`
- Current codebase pressure points are client tooling, generated Molang grammar files, loader/publication flow, and sync/data-attachment boundaries.

## Where To Read By Topic
- Client rendering/runtime: start in `src/main/java/io/github/tt432/eyelib/client/`
- Loader and resource ingestion: start in `src/main/java/io/github/tt432/eyelib/client/loader/`
- Runtime asset storage: start in `src/main/java/io/github/tt432/eyelib/client/manager/`
- Molang: start in `src/main/java/io/github/tt432/eyelib/molang/`, but treat `molang/generated/` as generated/read-only
- Sync and packets: start in `src/main/java/io/github/tt432/eyelib/network/`
- Data attachment flow: start in `src/main/java/io/github/tt432/eyelib/util/data_attach/` and `src/main/java/io/github/tt432/eyelib/capability/`

## Read In This Order
1. This file
2. Relevant architecture doc under `docs/architecture/`
3. The nearest package `README.md`
4. Only the code files required by the current task

## Hotspots
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/EyelibManagerScreen.java`
- `src/main/java/io/github/tt432/eyelib/molang/generated/`
- `src/main/java/io/github/tt432/eyelib/client/loader/`
- `src/main/java/io/github/tt432/eyelib/util/client/`
- `src/main/java/io/github/tt432/eyelib/network/` + `src/main/java/io/github/tt432/eyelib/util/data_attach/`
