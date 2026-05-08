---
phase: 01-module-scaffolding-config-annotation-discovery
plan: 03
subsystem: runtime
tags: [forge, mod-entrypoint, mods-toml, client-only]
requires:
  - phase: 01-01
    provides: Gradle build config for runtime subproject (legacyForge 2.0.91)
provides:
  - "@Mod entrypoint class (ClientSmokeMod) with Forge 1.20.1 composition root"
  - "mods.toml metadata with modId=clientsmoke, side=CLIENT, Forge+Minecraft dependencies"
affects: [01-04-config, 01-05-scanner, 02-state-machine]
tech-stack:
  added:
    - net.minecraftforge.fml.common.Mod
    - net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
    - org.slf4j (SLF4J logging)
  patterns:
    - "@Mod constructor pattern with FMLJavaModLoadingContext.get().getModEventBus()"
    - "Comment placeholders for downstream plan wiring (config, scanner, events)"
key-files:
  created:
    - eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/ClientSmokeMod.java
    - eyelib-clientsmoke/src/main/resources/META-INF/mods.toml
  modified: []
key-decisions:
  - "MOD_ID = \"clientsmoke\" — lowercase, matches mods.toml [[mods]].modId exactly (per Pitfall 6)"
  - "Constructor takes no args — standard Forge 1.20.1 @Mod pattern with FMLJavaModLoadingContext for bus access"
  - "mods.toml side=\"CLIENT\" on ALL dependency blocks — client-only mod, prevents dedicated server loading (per Pitfall 13)"
  - "SLF4J LoggerFactory pattern — matches existing eyelib logging convention"
patterns-established:
  - "@Mod entrypoint with comment placeholders for future wiring: config registration, scanner init, event bus subscriber"
requirements-completed: [MOD-02]
duration: 5min
completed: 2026-05-06
---

# Phase 01 Plan 03: Runtime Mod Entrypoint + Forge Metadata Summary

**Forge 1.20.1 @Mod entrypoint created with SLF4J logger, FMLJavaModLoadingContext bus access, and comment placeholders for downstream plans — mods.toml declares modId=clientsmoke as client-only with all variables expanded at build time.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-05-06T11:20:00+08:00
- **Completed:** 2026-05-06T11:25:00+08:00
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- `ClientSmokeMod` class with `@Mod("clientsmoke")`, MOD_ID constant, SLF4J logger, and constructor with FMLJavaModLoadingContext bus access
- Comment placeholders for Plan 04 (config registration), Plan 05 (scanner init), and Phase 2 (event bus subscriber)
- `mods.toml` with modId=clientsmoke, displayName="Client Smoke Test", side="CLIENT" on all dependencies
- Variable expansion via processResources: forge_version_range=[47.1.3,), minecraft_version_range=[1.20.1,1.21), loader_version_range=[47,)
- No unresolved `${...}` placeholders in output JAR mods.toml

## Task Commits

1. **Task 1-2: @Mod Entrypoint + mods.toml** - `9429440` (feat: runtime entrypoint)

## Files Created/Modified
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/ClientSmokeMod.java` — @Mod entrypoint with MOD_ID, SLF4J logger, constructor bus access, downstream wiring placeholders
- `eyelib-clientsmoke/src/main/resources/META-INF/mods.toml` — Forge 1.20.1 mod metadata (modId=clientsmoke, side=CLIENT, Forge+Minecraft deps)

## Verification

- `:eyelib-clientsmoke:build` → BUILD SUCCESSFUL (exit 0)
- Output JAR: `eyelib-clientsmoke-21.1.14+1.20.1-forge.jar` (2281 bytes)
- JAR contains `META-INF/mods.toml` with ALL variables expanded — NO `${...}` placeholders
- mods.toml `side="CLIENT"` on both dependency blocks (exactly 2 `side=` entries, both CLIENT)
- No `side="BOTH"` or `side="SERVER"` anywhere in mods.toml
- `ClientSmokeMod.class` present in JAR

## Decisions Made
- Followed plan exactly — all patterns match existing eyelib conventions (EyelibMod constructor pattern, mods.toml variable expansion)

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness
- Runtime mod JAR builds and produces valid Forge 1.20.1 mod with correct metadata
- Ready for Plan 04 (config registration into @Mod constructor) and Plan 05 (scanner initialization)
- @Mod constructor has clearly marked comment placeholders for downstream wiring

---
*Plan: 01-03*
*Completed: 2026-05-06*
