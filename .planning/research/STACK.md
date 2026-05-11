# Technology Stack

**Project:** v1.4 结构清理 (multi-module Gradle structural cleanup)
**Researched:** 2026-05-11

## Recommended Stack

This is a brownfield refactoring milestone — the technology stack is inherited from v1.3. No new technologies are introduced. The research focuses on the existing stack constraints that affect structural cleanups.

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Java | 17 | Compilation target | Mojang ships Java 17 to end users in 1.20.1 |
| Forge (LegacyForge) | 1.20.1-50.1.32 | Mod platform | Project foundation; `net.neoforged.moddev.legacyforge` 2.0.91 |
| Gradle | 8.x (via MDGL) | Build system | Managed by ModDevGradleLegacy plugin |

### Database
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| H2 Database | 2.4.240 | Instrumentation persistence (to be deleted in Goal 5) | Currently used by `client/instrument/db/InstrumentDatabase.java`; will be removed from `implementation` but may remain in `testImplementation` |

### Infrastructure
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| JetBrains MCP | IDE plugin | Gradle task execution | **REQUIRED** — project policy prohibits shell Gradle. All builds use `jetbrain_build_project` and `jetbrain_run_gradle_tasks`. |
| IntelliJ IDEA | Latest | Primary IDE | Only supported IDE; `.idea/` checked in, no JDTLS/VS Code/Eclipse artifacts allowed |
| ClientSmoke | Git submodule | Automated visual testing | Composite build `includeBuild("clientsmoke")`; used as optional verification gate G7 |
| GitHub Actions (implied) | N/A | CI | Build verification in CI must use JetBrains MCP-equivalent Gradle invocation |

### Static Analysis
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| NullAway | 0.12.10 | Null safety verification | Configured on root via `nullawayMain` task; checks `io.github.tt432.eyelib` packages |
| Error Prone | 2.42.0 | Compile-time bug detection | Used with NullAway; `nullawayMain` task has isolated processor path |
| JSpecify | 1.0.0 | Nullability annotations | `@Nullable` annotations used across codebase; NullAway reads them |

### Testing
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| JUnit | 5.10.2 (platform) | Unit testing | All 54 existing tests use JUnit Jupiter |
| JUnit Platform Launcher | 5.x | Test runner | Configured via `tasks.named('test').configure { useJUnitPlatform() }` |

## Module Build Profiles

Each module has a distinct build profile affecting what cleanup operations are safe:

| Module | Forge Plugin | Plain JVM? | Key Constraint |
|--------|-------------|------------|----------------|
| `:` (root) | YES (legacyForge) | No | Has NullAway, Mixin, ClientSmoke; depends on all submodules |
| `:eyelib-attachment` | YES (legacyForge) | No | Has mods.toml (modId: `eyelibattachment`) |
| `:eyelib-importer` | YES (legacyForge) | No | Has mods.toml (modId: `eyelibimporter`) |
| `:eyelib-material` | YES (legacyForge) | No | Has mods.toml (modId: `eyelibmaterial`) |
| `:eyelib-molang` | YES (legacyForge) | No | Generated code under `generated/` is read-only |
| `:eyelib-particle` | YES (legacyForge) | No | Has mods.toml (modId: `eyelibparticle`) |
| `:eyelib-processor` | **NO** | **YES** | **Plain JVM** — no Forge, no mods.toml. CRITICAL for Goal 6: if bake code has Minecraft imports, this module must become Forge. |
| `:eyelib-util` | YES (legacyForge) | No | Leaf module (zero `project(...)` dependencies); has mods.toml (modId: `eyelibutil`) |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Module rename approach | Atomic settings.gradle + all build.gradle + directory rename in one operation | Two-step (create new, migrate code, delete old) | Two-step risks having both modules in the project simultaneously, causing classpath conflicts and confusing IDE |
| Directory rename tool | `ide_move_file` (IDE refactoring engine) | Shell `mv` or `ren` command | IDE refactoring preserves references; shell rename leaves IDE with stale paths |
| Gradle execution | JetBrains MCP (`jetbrain_build_project`) | Shell `./gradlew build` | **PROHIBITED** by project policy |
| Capability migration namespace | New namespace `io.github.tt432.eyelibattachment.capability` | Same namespace `io.github.tt432.eyelib.capability` | Split-package risk; follow v1.3 pattern of using distinct namespace |

## Sources

- `build.gradle` (root): All 356 lines inspected — dependency declarations, NullAway config, Mixin setup
- `settings.gradle`: All 24 lines inspected — 7 includes + composite build
- `eyelib-processor/build.gradle`: Confirmed plain-JVM (no `legacyForge` plugin)
- `eyelib-attachment/build.gradle`: Confirmed Forge + mods.toml
- `eyelib-importer/build.gradle`: Confirmed Forge + depends on `:eyelib-molang` and `:eyelib-material`
- `.idea/compiler.xml`: Annotation processor profiles for all modules
- Prior milestone build.gradle changes (v1.2, v1.3): Established patterns for adding module dependencies
