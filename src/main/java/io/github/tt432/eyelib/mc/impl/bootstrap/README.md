# MC Impl Bootstrap

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/`
- Owns Forge `@Mod` composition-root startup wiring for Eyelib.

## Main Entry
- `EyelibMod.java`: registers attachment data and network transport bootstrap during mod startup.

## Boundary Rule
- Keep direct Minecraft/Forge lifecycle wiring here.
- Do not move this startup ownership back into legacy top-level packages.
