# Client Package Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/`
- Covers runtime rendering, models, animations, particles, loaders, managers, and dev-oriented screens.

## Start Reading Here
1. `docs/index/client.md`
2. `loader/README.md` for resource ingestion work
3. `gui/manager/README.md` for tooling-screen work

## Important Areas
- `loader/`: resource reload and parse entrypoints
- `manager/`: runtime storage and lookup of loaded assets
- `gui/manager/`: development/debug UI, currently including a major hotspot
- `render/`, `model/`, `animation/`, `particle/`: core client runtime domains

## Hotspots
- `gui/manager/EyelibManagerScreen.java`
- `loader/`
- `manager/`

## Do Not Read Unless
- Only enter `gui/manager/` when the task touches dev tooling.
- Only read all of `client/` when a task genuinely spans multiple runtime domains.
