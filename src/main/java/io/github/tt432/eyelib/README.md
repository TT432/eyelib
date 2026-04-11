# Eyelib Root Package

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/`
- This is the top-level package for the library’s client, network, Molang, utility, capability, and behavior-related subsystems.

## Start Reading Here
1. `AGENTS.md`
2. `docs/index/repo-map.md`
3. `mc/impl/bootstrap/EyelibMod.java`

## Main Entry Points
- `mc/impl/bootstrap/EyelibMod.java` is the Forge mod bootstrap composition root.
- `Eyelib.java` is a lightweight mod-id constant holder used by legacy call sites.
- Most concrete behavior lives in child packages, not in this root package itself.

## Child Areas
- `client/`: rendering, animation, particles, tooling, loaders, managers
- `molang/`: legacy Molang marker docs; active Molang runtime/compiler/generated code now lives in `eyelib-molang/`
- `network/`: packet registration and handler routing
- `util/`: helpers, including mixed client and data-attachment utility code
- `core/`: platform-free utility seams and extracted pure helpers
- `capability/`: attachment-related capability registration/data

## Read Only If Needed
- Stay in this directory only for bootstrap or top-level boundary work.
- For actual feature work, switch to the nearest child package README before opening more code.
