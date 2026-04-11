# Eyelib API Boundary Notes

## Purpose
- This directory is the future home for intentionally stable external or cross-subsystem contracts.
- At the current stage, Eyelib does not yet have a broad carved-out API package; most code remains internal by default.

## Current Public Bias
- `src/main/java/io/github/tt432/eyelib/Eyelib.java` is now a compatibility-facing mod-id constant holder.
- Forge startup entrypoint ownership is quarantined under `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`.
- New stable API should be added here only when a stage explicitly justifies exposing it.
