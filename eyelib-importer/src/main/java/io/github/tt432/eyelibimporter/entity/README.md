# Eyelib Importer Client Entity Schema Guide

## Scope
- Path: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/entity/`
- Owns importer-side client-entity and attachable schema/codecs shared by root loaders and manager import tooling.

## Boundary intent
- Keep parsed definition/codecs here.
- Keep runtime lookup, manager publication, RenderData integration, and entity/model runtime behavior in root `client/entity` and neighboring runtime packages.

## Editing rules
- Do not add runtime manager, event, or Minecraft upload logic here.
- If a type starts evaluating runtime state instead of just describing schema, split the runtime part back into root.
