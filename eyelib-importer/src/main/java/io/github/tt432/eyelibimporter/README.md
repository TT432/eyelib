# Eyelib Importer Module Guide

## Current Identity
- Path: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/`
- `:eyelib-importer` is currently an importer/schema Forge functional module, not a plain JVM library.
- Its Gradle artifact is registered as the `eyelibimporter` Forge mod through `META-INF/mods.toml` and `EyelibResourcesImporterMod.java`.
- Root and feature modules consume it for importer-owned schemas, codecs, source parsing, addon discovery, normalization, and importer-side image/data representations.

## Boundary Intent
- Keep Bedrock/imported schemas, addon discovery, parsed model/entity/animation/particle source data, and importer-only normalization here.
- Keep Minecraft/Forge bootstrap isolated to the root package unless a future importer-owned runtime integration explicitly needs more Forge wiring.
- Keep root runtime execution, managers, texture upload, GUI publication, particle execution, and other runtime adaptation in consuming runtime modules.
- Do not split or migrate code only to remove Minecraft/Forge imports. A separate plain importer library remains future debt until there is a concrete product requirement.

## Editing Rules
- Do not import root runtime packages (`io.github.tt432.eyelib.*`) from importer main sources.
- Prefer plain data structures in schema and addon packages.
- If new Minecraft/Forge imports are needed outside the root package, document why they are importer-owned functional behavior before adding them.
