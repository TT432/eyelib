# Eyelib Importer Addon Loading Guide

## Scope
- Path: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/addon/`
- Owns importer-side Bedrock addon/pack discovery, manifest parsing, archive extraction, asset indexing, and aggregate loading for importer-owned schemas plus raw resource-side payload capture.

## Boundary intent
- Keep pack discovery, `manifest.json` parsing, importer-owned schema loading, texture decoding, and raw resource file collection here.
- Keep runtime adaptation, manager publication, texture upload, render-controller execution, particle runtime behavior, and Minecraft/Forge lifecycle wiring in root runtime packages.

## Editing rules
- Do not add Minecraft/Forge runtime types here.
- Prefer returning plain importer-side data structures that root can adapt later.
- When a resource family is still root-owned at runtime (for example render controllers or particles), capture the raw loaded payload here instead of pulling runtime execution code into importer.
