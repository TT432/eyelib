# Eyelib Processor Module

## Scope
- Path: `eyelib-processor/src/main/java/io/github/tt432/eyelibprocessor/`
- Owns platform-free processing seams extracted from root runtime/tooling flows.

## Current Responsibilities
- Path/file classification helpers for manager reload planning.
- Plain-JVM parsing helpers shared by loaders.
- Batch assembly/file collection helpers for manager import planning.
- Particle flipbook summary helpers derived from importer-owned particle schema, using `:eyelib-molang` for shared compile-time Molang analysis when numeric summaries need constant folding.

## Boundary Rules
- May depend on plain JVM APIs, codecs, importer-owned schema modules, and engine-side plain-JVM Molang analysis from `:eyelib-molang`.
- Must not depend on root runtime ownership concerns (Forge event posting, NativeImage upload, UI/session lifecycle, RenderSystem hooks).

## Current Consumers
- Root runtime/tooling module (`:`) depends on this module for processing seams.
- Stage-1 extraction keeps this module independent from root runtime packages and free of reverse dependencies back into `:`.
