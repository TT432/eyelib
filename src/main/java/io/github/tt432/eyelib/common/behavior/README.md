# Common Behavior Package Guide

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/common/behavior/`
- Contains behavior schema types, filters, components, and event logic used by shared behavior features.

## Boundary warning
- This package is not platform-free today.
- Several schema-level types still depend on Minecraft serialization conventions such as `ResourceLocation` and `StringRepresentable`.

## Boundary intent
- During final quarantine, either:
  1. redesign surviving behavior schema contracts to use platform-free ids/enums, or
  2. quarantine Minecraft-owned behavior schema/runtime into `mc/impl`.

## Editing rules
- Do not add new Minecraft/Forge serialization helpers here casually.
- If introducing new behavior schema, prefer platform-free identifiers and enums unless the class is explicitly transitional and documented as Minecraft-owned.
