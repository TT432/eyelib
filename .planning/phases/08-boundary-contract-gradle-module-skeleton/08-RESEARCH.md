# Phase 8: Boundary Contract & Gradle Module Skeleton - Research

**Researched:** 2026-05-09  
**Domain:** Gradle multi-project module boundary, Java 17 Forge/ModDevGradle subproject skeleton, repository boundary documentation  
**Confidence:** HIGH for repository-local conventions; MEDIUM-HIGH for external Gradle/ModDevGradle guidance

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
This phase establishes `:eyelib-particle` as a real Gradle module boundary with explicit build metadata, source/resource layout, root dependency wiring, and ownership documentation. It must prove the one-way dependency direction: root runtime may consume the particle module, but `:eyelib-particle` must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes. [VERIFIED: .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md]

### the agent's Discretion
All implementation choices are at the agent's discretion because this is a pure infrastructure phase. Use the ROADMAP goal, requirements PGRAD-01/PGRAD-02/PAPI-02, existing Gradle subproject conventions, and repository module-boundary documentation rules to guide decisions. [VERIFIED: .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md]

### Deferred Ideas (OUT OF SCOPE)
Moving particle APIs, schema/runtime adapters, loader publication, command/network integration, and verification coverage is deferred to Phases 9-14. [VERIFIED: .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md]
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PGRAD-01 | Maintainer can build and consume a real `:eyelib-particle` Gradle subproject with its own build metadata, source sets, resources, and root project dependency wiring. [VERIFIED: .planning/REQUIREMENTS.md] | Use `settings.gradle` `include("eyelib-particle")`, an `eyelib-particle/build.gradle` patterned after `:eyelib-material`/`:eyelib-attachment`, conventional `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`, `META-INF/mods.toml`, and root `dependencies` wiring. [VERIFIED: settings.gradle; build.gradle; eyelib-material/build.gradle; eyelib-attachment/build.gradle] |
| PGRAD-02 | Maintainer can read module documentation that states `:eyelib-particle` ownership, dependency direction, and allowed integration layers. [VERIFIED: .planning/REQUIREMENTS.md] | Update `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and add `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` to record ownership and allowed dependencies. [VERIFIED: AGENTS.md; MODULES.md; docs/index/repo-map.md; docs/architecture/01-module-boundaries.md; docs/architecture/02-side-boundaries.md] |
| PAPI-02 | `:eyelib-particle` has no dependency on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes. [VERIFIED: .planning/REQUIREMENTS.md] | Keep the Phase 8 skeleton minimal and do not move current particle runtime code yet; add boundary documentation and optional compile-only/structural tests later only if they do not require root dependencies. [VERIFIED: .planning/ROADMAP.md; .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md] |
</phase_requirements>

## Summary

Phase 8 should create the Gradle and documentation boundary before moving particle behavior. Existing repository conventions show that first-class subprojects are included in `settings.gradle`, use their own `build.gradle`, inherit `rootProject.version`/`rootProject.group`, target Java 17, use `java-library`, usually use `io.freefair.lombok` 8.6, use JUnit 5, and for Forge-visible modules use `net.neoforged.moddev.legacyforge` 2.0.91 with a module-local `META-INF/mods.toml`. [VERIFIED: settings.gradle; build.gradle; eyelib-attachment/build.gradle; eyelib-importer/build.gradle; eyelib-material/build.gradle; eyelib-molang/build.gradle; eyelib-processor/build.gradle]

The safest plan is intentionally a skeleton: add `:eyelib-particle` as a buildable/consumable subproject and root dependency, document ownership and one-way dependency direction, but do not move particle APIs, schema/runtime adapters, loader publication, command/network integration, or render/runtime code because those are explicitly deferred to Phases 9-14. [VERIFIED: .planning/ROADMAP.md; .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md]

**Primary recommendation:** Implement a minimal Forge-aware `:eyelib-particle` skeleton patterned after `:eyelib-material`/`:eyelib-attachment`, wire root → particle only, and make documentation plus a JetBrains-MCP-only verification plan the main deliverable. [VERIFIED: eyelib-material/build.gradle; eyelib-attachment/build.gradle; AGENTS.md; docs/conventions.md]

## Project Constraints (from AGENTS.md)

- Read `docs/index/repo-map.md` before code exploration, `MODULES.md` before structural/multi-module planning, and boundary docs before boundary decisions. [VERIFIED: AGENTS.md]
- The repo is a multi-project `Gradle + Java 17 + Forge` codebase with root runtime plus subprojects including `:eyelib-processor`, `:eyelib-importer`, `:eyelib-molang`, and `:eyelib-material`; the existing module shape must be preserved unless a human asks to collapse it. [VERIFIED: AGENTS.md; MODULES.md; docs/index/repo-map.md]
- Do not touch unrelated uncommitted changes; use narrow, stage-scoped edits. [VERIFIED: AGENTS.md]
- Before each change, identify affected `MODULES.md` modules; update `MODULES.md` and impacted index/architecture docs when adding/removing modules or changing boundaries. [VERIFIED: AGENTS.md; MODULES.md]
- Do not add new code to ambiguous catch-all areas like `src/main/java/io/github/tt432/eyelib/util/client/` without documenting destination responsibility. [VERIFIED: AGENTS.md]
- Treat `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` as generated/read-only during normal work. [VERIFIED: AGENTS.md]
- Use IntelliJ/JetBrains MCP for Gradle checks; never run `./gradlew` or other shell Gradle commands. If MCP is unavailable, stop and ask the user to re-enable MCP. [VERIFIED: AGENTS.md; docs/conventions.md]
- VS Code/Eclipse/JDTLS artifacts and tooling are forbidden. [VERIFIED: AGENTS.md]
- Docs-only changes verify referenced paths; structure/code changes use stage-specific Gradle checks via JetBrains MCP and require exit code 0. [VERIFIED: AGENTS.md]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Gradle subproject inclusion | Build System / Gradle | Documentation | `settings.gradle` owns included subprojects and Gradle docs state multi-project builds are defined in `settings.gradle(.kts)` via `include()`. [VERIFIED: settings.gradle] [CITED: https://docs.gradle.org/current/userguide/multi_project_builds.html] |
| Module build metadata | Build System / Gradle | Forge tooling | Existing Forge-visible subprojects use module-local `build.gradle`, Java 17, `legacyForge`, `mods { ... sourceSet(sourceSets.main) }`, resources, and publication metadata. [VERIFIED: eyelib-attachment/build.gradle; eyelib-material/build.gradle; eyelib-importer/build.gradle] |
| Root → particle consumption | Root runtime build | Particle module artifact | Root currently consumes module artifacts through `api`, runtime classpath (`modImplementation` or `additionalRuntimeClasspath`), and `jarJar` depending on subproject type. [VERIFIED: build.gradle] |
| Particle core ownership contract | Documentation / Architecture | Particle module package README | Boundary rules are maintained in `MODULES.md`, architecture docs, repo map, and package-local README files. [VERIFIED: AGENTS.md; MODULES.md; docs/index/repo-map.md] |
| Root/runtime contamination prevention | Particle module build + docs | Later boundary tests | Phase 8 can guarantee no reverse dependency by not declaring a dependency from `:eyelib-particle` to root and by keeping moved code out of scope. [VERIFIED: .planning/ROADMAP.md; .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md] |
| Verification execution | JetBrains MCP | IDE Gradle sync | Project conventions require Gradle execution through `jetbrain_run_gradle_tasks`, `jetbrain_build_project`, or `jetbrain_sync_gradle_projects` only. [VERIFIED: docs/conventions.md] |

## Standard Stack

### Core
| Library / Tool | Version | Purpose | Why Standard |
|----------------|---------|---------|--------------|
| Gradle multi-project build | Current docs fetched version 9.5.0; wrapper version not executed in shell due project rule | Subproject inclusion, project dependencies, source sets, lifecycle tasks | Official Gradle docs define subprojects via `settings.gradle` `include()` and project dependencies via `project(':module')`. [CITED: https://docs.gradle.org/current/userguide/multi_project_builds.html] [CITED: https://docs.gradle.org/current/userguide/declaring_dependencies_basics.html] |
| `java-library` Gradle plugin | Built-in Gradle plugin | Library source sets, `api`/`implementation`, JAR/test tasks | Existing subprojects use it; official docs state it exposes `api` and `implementation` for API/implementation separation. [VERIFIED: eyelib-attachment/build.gradle; eyelib-material/build.gradle; eyelib-molang/build.gradle] [CITED: https://docs.gradle.org/current/userguide/java_library_plugin.html] |
| Java toolchain | 17 | Compile/test toolchain for Minecraft 1.20.1 Forge project | Root and subprojects set `java.toolchain.languageVersion = JavaLanguageVersion.of(17)`; Gradle docs recommend toolchains for reproducible Java version selection. [VERIFIED: build.gradle; eyelib-attachment/build.gradle; eyelib-material/build.gradle] [CITED: https://docs.gradle.org/current/userguide/toolchains.html] |
| `net.neoforged.moddev.legacyforge` | 2.0.91 | Forge/ModDevGradle integration for Forge-visible modules | Root and Forge-visible subprojects already use this plugin; NeoForged docs describe ModDevGradle as creating artifacts to compile Minecraft mods and run from Gradle/IntelliJ. [VERIFIED: build.gradle; eyelib-attachment/build.gradle; eyelib-material/build.gradle; eyelib-importer/build.gradle] [CITED: https://docs.neoforged.net/toolchain/docs/plugins/mdg/] |
| `io.freefair.lombok` | 8.6 | Lombok support in Java modules | Existing subprojects consistently apply it. [VERIFIED: eyelib-attachment/build.gradle; eyelib-importer/build.gradle; eyelib-material/build.gradle; eyelib-molang/build.gradle; eyelib-processor/build.gradle] |

### Supporting
| Library / Tool | Version | Purpose | When to Use |
|----------------|---------|---------|-------------|
| JUnit Jupiter | BOM 5.10.2 | Unit tests for module skeleton/boundary checks | Existing subprojects use `testImplementation platform('org.junit:junit-bom:5.10.2')`, `org.junit.jupiter`, and `useJUnitPlatform()`. [VERIFIED: build.gradle; eyelib-attachment/build.gradle; eyelib-material/build.gradle] |
| `maven-publish` | Built-in Gradle plugin | Local Maven publication metadata | Existing independently consumable subprojects apply it and publish `components.java`. [VERIFIED: eyelib-attachment/build.gradle; eyelib-material/build.gradle; eyelib-importer/build.gradle; eyelib-processor/build.gradle] |
| `org.jspecify:jspecify` | 1.0.0 | Compile-only nullness annotations | Existing modules use it as `compileOnly`. [VERIFIED: build.gradle; eyelib-attachment/build.gradle; eyelib-material/build.gradle] |
| `:eyelib-molang` | root version | Molang value/scope API for later particle runtime phases | Current particle code imports Molang types; for Phase 8 skeleton only declare if a placeholder public API actually exposes Molang types. [VERIFIED: JetBrains text search in `src/main/java/io/github/tt432/eyelib/client/particle/**`; eyelib-molang/build.gradle] |
| `:eyelib-material` | root version | Render/material support for later runtime phases | Current particle render manager imports `io.github.tt432.eyelibmaterial.render.RenderTypeResolver`; do not add unless Phase 8 creates code that compiles against it. [VERIFIED: JetBrains text search in particle package; eyelib-material/build.gradle] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Forge-aware `legacyForge` subproject | Plain `java-library` only | Plain Java is simpler and used by `:eyelib-molang`/`:eyelib-processor`, but Phase success explicitly requires source sets/resources/root runtime consumption in a Forge project and existing Forge-visible modules have `META-INF/mods.toml`. Use Forge-aware skeleton unless the planner intentionally creates a pure-core-only module first. [VERIFIED: .planning/ROADMAP.md; eyelib-material/build.gradle; eyelib-molang/build.gradle] |
| Empty skeleton module | Move particle classes immediately | Moving code would collide with deferred Phases 9-14 and current particle package has many MC/Forge/root imports that would violate PAPI-02 if copied naively. [VERIFIED: .planning/ROADMAP.md; JetBrains text search in particle package] |
| Root `api` only | `api` + `modImplementation` + `jarJar` | Existing root uses broad triple wiring for Forge-visible subprojects; narrower scopes are explicitly deferred by PFUT-01. [VERIFIED: build.gradle; .planning/REQUIREMENTS.md] |

**Installation:** No npm/package installation is needed. Add/edit Gradle build files and module directories only. [VERIFIED: .planning/ROADMAP.md; settings.gradle; build.gradle]

**Version verification:** Recommended versions are repository-pinned rather than fetched from a package registry: `legacyForge` 2.0.91, `io.freefair.lombok` 8.6, JUnit BOM 5.10.2, Forge 47.1.3, Minecraft 1.20.1, Java toolchain 17. [VERIFIED: build.gradle; gradle.properties; subproject build.gradle files]

## Architecture Patterns

### System Architecture Diagram

```text
Maintainer / IDE
    |
    v
settings.gradle include("eyelib-particle")
    |
    v
:eyelib-particle Gradle subproject
    |-- build.gradle (java-library + Lombok + optional legacyForge + publishing)
    |-- src/main/java/io/github/tt432/eyelibparticle/README.md
    |-- src/main/resources/META-INF/mods.toml (if Forge-visible skeleton)
    |-- src/test/java + src/test/resources placeholders as needed
    |
    v
Root build.gradle dependencies
    |
    +--> root runtime consumes particle artifact
    |
    X  no dependency back to root runtime / mc/impl / managers / packets
```

The diagram follows current repo subproject conventions and the one-way root → particle boundary decision. [VERIFIED: settings.gradle; build.gradle; .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md]

### Recommended Project Structure

```text
eyelib-particle/
├── build.gradle                                      # subproject metadata and dependencies [VERIFIED pattern: eyelib-material/build.gradle]
├── src/main/java/io/github/tt432/eyelibparticle/
│   ├── README.md                                     # ownership + dependency contract [VERIFIED rule: MODULES.md]
│   └── package-info.java                             # optional package-level boundary note
├── src/main/resources/
│   ├── META-INF/mods.toml                            # if Forge-visible module, like material/attachment/importer
│   └── pack.mcmeta                                   # optional only if matching existing Forge module resource convention
├── src/test/java/io/github/tt432/eyelibparticle/
└── src/test/resources/
```

Use package root `io.github.tt432.eyelibparticle` to avoid split packages with root `io.github.tt432.eyelib.*`, matching existing independent namespaces `io.github.tt432.eyelibmaterial`, `io.github.tt432.eyelibimporter`, and `io.github.tt432.eyelibmolang`. [VERIFIED: MODULES.md; subproject source paths]

### Pattern 1: Gradle subproject inclusion
**What:** Add `include("eyelib-particle")` in `settings.gradle`. [CITED: https://docs.gradle.org/current/userguide/multi_project_builds.html]  
**When to use:** Required for a first-class subproject under the root build. [VERIFIED: settings.gradle]  
**Example:**

```groovy
rootProject.name = "eyelib"

include("eyelib-attachment")
include("eyelib-importer")
include("eyelib-material")
include("eyelib-molang")
include("eyelib-particle")
include("eyelib-processor")
```

### Pattern 2: Forge-visible module skeleton
**What:** Mirror `:eyelib-material`/`:eyelib-attachment` build metadata: `java-library`, Lombok, `legacyForge`, `maven-publish`, root version/group, Java 17, resource token expansion, JUnit 5. [VERIFIED: eyelib-material/build.gradle; eyelib-attachment/build.gradle]  
**When to use:** Use for `:eyelib-particle` if it is meant to be a mod/artifact visible to Forge runtime and jar-in-jar consumption. [VERIFIED: build.gradle; eyelib-material/src/main/resources/META-INF/mods.toml]

```groovy
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base { archivesName = 'eyelib-particle' }
java.toolchain.languageVersion = JavaLanguageVersion.of(17)
```

### Pattern 3: Root consumes module; module does not consume root
**What:** Add root project dependency on `:eyelib-particle`; do not add any `project(':')` or root package dependency from particle back to root. [VERIFIED: build.gradle; .planning/REQUIREMENTS.md]  
**When to use:** Required by one-way root → particle direction and PAPI-02. [VERIFIED: .planning/STATE.md]

```groovy
dependencies {
    api project(':eyelib-particle')
    modImplementation project(':eyelib-particle')
    jarJar project(':eyelib-particle')
}
```

### Anti-Patterns to Avoid
- **Moving current particle runtime classes in Phase 8:** Current particle code imports `net.minecraft`, `net.minecraftforge`, root `mc/impl`, and root `client.manager.ParticleManager`; copying it into `:eyelib-particle` during this skeleton phase would violate PAPI-02 unless adapters are designed first. [VERIFIED: JetBrains text search in `src/main/java/io/github/tt432/eyelib/client/particle/**`; .planning/ROADMAP.md]
- **Declaring `implementation project(':')` from `:eyelib-particle`:** That creates the forbidden reverse dependency. [VERIFIED: .planning/REQUIREMENTS.md]
- **Using shell Gradle commands in docs:** Project rules forbid `./gradlew`; verification docs must name JetBrains MCP tools/tasks only. [VERIFIED: AGENTS.md; docs/conventions.md]
- **Adding particle code under root catch-all/legacy packages:** Phase intent is a real module boundary, not more root-owned internals. [VERIFIED: .planning/REQUIREMENTS.md; AGENTS.md]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Multi-project inclusion | Custom build discovery or ad-hoc folder conventions | Gradle `settings.gradle include("eyelib-particle")` | Gradle officially maps included project paths to subproject directories and project dependencies. [CITED: https://docs.gradle.org/current/userguide/multi_project_builds.html] |
| Java source/resource layout | Custom source-set paths without need | Default `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources` | Gradle Java plugin conventions automatically wire compile, resources, test, and jar tasks. [CITED: https://docs.gradle.org/current/userguide/building_java_projects.html] |
| API/implementation classpath | Manual jar/file dependencies | `java-library` `api`/`implementation` and `project(':module')` dependencies | Gradle docs state `api` exposes ABI dependencies and `implementation` keeps internals off consumer compile classpaths. [CITED: https://docs.gradle.org/current/userguide/java_library_plugin.html] |
| Forge mod metadata processing | Custom resource copy script | Existing `processResources` token expansion pattern | Existing Forge-visible modules expand `META-INF/mods.toml` from Gradle properties. [VERIFIED: eyelib-attachment/build.gradle; eyelib-material/build.gradle] |
| Verification runner | Shell `./gradlew` commands | `jetbrain_run_gradle_tasks`, `jetbrain_build_project`, `jetbrain_sync_gradle_projects` | Project conventions explicitly restrict Gradle execution to JetBrains/IDE MCP. [VERIFIED: docs/conventions.md] |

**Key insight:** This phase is infrastructure and contract work; custom compatibility shims or runtime movers would add debt before the API/store/schema/render phases establish the actual seams. [VERIFIED: .planning/ROADMAP.md]

## Common Pitfalls

### Pitfall 1: Skeleton accidentally becomes a runtime extraction
**What goes wrong:** Planner moves current root particle classes into `:eyelib-particle` to make the module “real.” [VERIFIED: current particle package exists under root from CONTEXT.md and Glob]  
**Why it happens:** PGRAD-01 says buildable/consumable, but later phases own API, schema, runtime, loader, command, network, and render moves. [VERIFIED: .planning/ROADMAP.md]  
**How to avoid:** Keep Phase 8 to build metadata, empty/marker package, resources, docs, and root dependency wiring only. [VERIFIED: .planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md]  
**Warning signs:** Plan tasks mention moving `ParticleSpawnService`, `BrParticleRenderManager`, loaders, packets, or `BrParticle` runtime definitions. [VERIFIED: .planning/ROADMAP.md]

### Pitfall 2: Reverse dependency hidden through project dependencies
**What goes wrong:** `:eyelib-particle` declares `implementation project(':')`, imports root packages, or uses root managers/registries/packets/capability helpers. [VERIFIED: .planning/REQUIREMENTS.md]  
**Why it happens:** Current root particle implementation already reaches `ParticleManager`, `mc/impl`, Minecraft, Forge, Molang, and material classes. [VERIFIED: JetBrains text search in particle package]  
**How to avoid:** In Phase 8, do not move those classes; in later phases, use explicit adapters/ports. [VERIFIED: .planning/ROADMAP.md]

### Pitfall 3: Wrong dependency scope leaks module internals
**What goes wrong:** Everything is declared as `api`, causing unnecessary consumer compile-classpath exposure. [CITED: https://docs.gradle.org/current/userguide/java_library_plugin.html]  
**Why it happens:** Existing root uses broad dependency wiring for jar-in-jar/runtime convenience. [VERIFIED: build.gradle]  
**How to avoid:** For `:eyelib-particle` internal dependencies, prefer `implementation`; use `api` only for types exposed in public ABI. [CITED: https://docs.gradle.org/current/userguide/java_library_plugin.html]

### Pitfall 4: Forgetting documentation fan-out
**What goes wrong:** Build works but maintainers cannot discover ownership or boundary rules. [VERIFIED: PGRAD-02 in .planning/REQUIREMENTS.md]  
**Why it happens:** New module rows and architecture docs are required by project rules, not by Gradle. [VERIFIED: AGENTS.md; MODULES.md]  
**How to avoid:** Update `MODULES.md`, repo map, both boundary docs, and module-local README in the same slice. [VERIFIED: AGENTS.md; MODULES.md]

## Code Examples

### Minimal `eyelib-particle/build.gradle` skeleton

```groovy
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base { archivesName = 'eyelib-particle' }
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

repositories {
    mavenCentral()
    mavenLocal()
    maven { url 'https://libraries.minecraft.net/' }
}

legacyForge {
    version = project.minecraft_version + '-' + project.forge_version
    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }
    mods {
        eyelibparticle { sourceSet(sourceSets.main) }
    }
}

dependencies {
    compileOnly 'org.jspecify:jspecify:1.0.0'
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
tasks.named('test').configure { useJUnitPlatform() }
java { withSourcesJar() }
```

Source basis: existing `:eyelib-material` and `:eyelib-attachment` build scripts plus Gradle Java Library and ModDevGradle docs. [VERIFIED: eyelib-material/build.gradle; eyelib-attachment/build.gradle] [CITED: https://docs.gradle.org/current/userguide/java_library_plugin.html] [CITED: https://docs.neoforged.net/toolchain/docs/plugins/mdg/]

### Module README boundary contract

```markdown
# Eyelib Particle Module

## Scope
- Path: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`
- Owns future particle-module APIs, particle core/runtime definitions, and allowed particle integration seams.

## Dependency Direction
- Root runtime may depend on `:eyelib-particle`.
- `:eyelib-particle` must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
- Minecraft/Forge integration must live in explicit integration/adapters documented before introduction.
```

Source basis: PGRAD-02/PAPI-02 and existing module README practices. [VERIFIED: .planning/REQUIREMENTS.md; src/main/java/io/github/tt432/eyelib/client/particle/README.md]

## State of the Art

| Old Approach | Current Approach | When Changed / Source | Impact |
|--------------|------------------|------------------------|--------|
| Root-owned mixed particle package under `src/main/java/io/github/tt432/eyelib/client/particle/` | First-class `:eyelib-particle` boundary planned, with root consuming particle one-way | v1.2 Phase 8 roadmap/context. [VERIFIED: .planning/ROADMAP.md; 08-CONTEXT.md] | Planner should establish module/documentation contract first, then move behavior in later phases. |
| Ad-hoc root reach-through | Domain-local read seams and explicit adapters | Boundary docs identify lookup seams and no new `Eyelib` singleton methods. [VERIFIED: docs/architecture/01-module-boundaries.md] | Particle future APIs should be narrow seams, not global bootstrap accessors. |
| Direct platform types in shared seams | String/platform-free seams where possible, MC adaptation at boundaries | Particle spawn and packet ids are string-keyed today. [VERIFIED: docs/architecture/01-module-boundaries.md; docs/architecture/02-side-boundaries.md] | New module docs should preserve platform-free core rule. |

**Deprecated/outdated:** Shell Gradle commands in docs are forbidden for this project even when examples from official docs show `./gradlew`; use JetBrains MCP task names instead. [VERIFIED: AGENTS.md; docs/conventions.md] [CITED: Gradle docs examples]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `:eyelib-particle` should be Forge-visible and use `legacyForge` immediately, rather than starting as pure `java-library` only. | Standard Stack / Architecture Patterns | If maintainer wanted pure core first, the module would carry unnecessary Forge metadata; planner can choose plain Java only if Phase 8 goal is narrowed to pure core. |
| A2 | Compile checks should be planned now, while deeper architecture tests can be deferred unless trivial. | Open Questions / Validation Architecture | If user expects boundary tests immediately, Phase 8 plan may under-specify automated PAPI-02 enforcement. |
| A3 | Research validity estimate is 30 days for repository-local conventions and shorter for external plugin-version decisions. | Metadata | If external Gradle/ModDevGradle behavior changes sooner, planner may rely on stale docs. |

## Open Questions (RESOLVED)

1. **Should `:eyelib-particle` initially be Forge-visible or pure Java-only?**
   - What we know: Existing Forge-visible feature subprojects `:eyelib-material`, `:eyelib-attachment`, and `:eyelib-importer` use `legacyForge` and `mods.toml`; pure engine/processor modules do not. [VERIFIED: subproject build.gradle files]
   - What's unclear: Phase 8 does not explicitly say whether `:eyelib-particle` must load as its own mod during dev/runtime. [VERIFIED: .planning/ROADMAP.md]
   - Recommendation: Use Forge-visible skeleton if root runtime will jarJar/load it like material/attachment; otherwise document a pure-core-first decision before planning. [ASSUMED]
   - RESOLVED: Use a Forge-visible skeleton now, matching the existing feature-module pattern used by `:eyelib-material`, `:eyelib-attachment`, and `:eyelib-importer`. This gives Phase 8 first-class Gradle/source/resource metadata while later phases can keep pure particle core packages free of root/MC/Forge contamination.

2. **Should Phase 8 add automated dependency-boundary tests immediately?**
   - What we know: Phase 14 owns verification coverage broadly, while Phase 8 success criteria require boundary documentation and module skeleton. [VERIFIED: .planning/ROADMAP.md]
   - What's unclear: Whether a lightweight `:eyelib-particle:compileJava` plus root compile is enough for this phase gate. [VERIFIED: .planning/ROADMAP.md]
   - Recommendation: Plan compile checks now; defer deeper architecture tests unless they are trivial and do not require moving code. [ASSUMED]
   - RESOLVED: Phase 8 must include compile/static checks for the skeleton and forbidden dependency/import absence. Deeper regression and parity tests remain Phase 14 unless a trivial boundary test naturally fits without moving runtime code.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JetBrains MCP Gradle tools | Verification guidance and future compile/test execution | ✓ | Tools available in session; Gradle tasks list currently reports “Sync the Gradle project in IDE first.” [VERIFIED: jetbrain_list_linked_gradle_projects; jetbrain_list_gradle_tasks] | Use `jetbrain_sync_gradle_projects` before `jetbrain_run_gradle_tasks`; if MCP unavailable, stop and ask user. [VERIFIED: docs/conventions.md] |
| IDE Index MCP | Java/source navigation and boundary searches | ✓ | Smart mode, not indexing. [VERIFIED: ide_index_status] | Use GitNexus/text search only as secondary; do not use JDTLS. [VERIFIED: AGENTS.md] |
| Java runtime | Gradle JVM/toolchain environment | ✓ | Shell `java -version` reports Temurin OpenJDK 21.0.10; `where.exe java` also shows a JDK 17 path. [VERIFIED: shell `java -version`; `where.exe java`] | Project Gradle toolchains target Java 17 and Foojay resolver is configured. [VERIFIED: settings.gradle; build.gradle] |

**Missing dependencies with no fallback:** None identified for planning. [VERIFIED: tool probes above]

**Missing dependencies with fallback:** Gradle task list is not visible until IDE sync; planner should include an initial JetBrains MCP sync step if verification task discovery is needed. [VERIFIED: jetbrain_list_gradle_tasks]

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter via JUnit BOM 5.10.2. [VERIFIED: build.gradle; subproject build.gradle files] |
| Config file | No standalone test config; Gradle `tasks.named('test').configure { useJUnitPlatform() }` in root/subprojects. [VERIFIED: build.gradle; eyelib-attachment/build.gradle; eyelib-material/build.gradle] |
| Quick run command | JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames: [":eyelib-particle:compileJava"]` after module exists. [VERIFIED: docs/conventions.md] |
| Full suite command | JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames: [":eyelib-particle:test", ":compileJava"]` or stage-specific planner command; no shell Gradle. [VERIFIED: docs/conventions.md] |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| PGRAD-01 | `:eyelib-particle` exists as a Gradle subproject with source/resource layout and root dependency wiring. | Gradle compile/sync + docs path check | JetBrains MCP `jetbrain_sync_gradle_projects`, then `jetbrain_run_gradle_tasks` task `:eyelib-particle:compileJava`. [VERIFIED: docs/conventions.md] | ❌ Wave 0 creates module files |
| PGRAD-02 | Docs state ownership, dependency direction, and allowed integration layers. | Docs path/content review | Docs-only path verification; no Gradle required unless combined with build edits. [VERIFIED: AGENTS.md] | ❌ Wave 0 updates docs |
| PAPI-02 | Particle module has no dependency on root runtime packages/root managers/root registries/root packets/root capability helpers/root `mc/impl`. | Static dependency/build review | Compile `:eyelib-particle:compileJava`; optional text/IDE search in `eyelib-particle/**` for forbidden imports. [VERIFIED: .planning/REQUIREMENTS.md; IDE search capability] | ❌ Wave 0 can add a small test or checklist |

### Sampling Rate
- **Per task commit:** Use JetBrains MCP compile for changed build/module slice; docs-only commits verify referenced paths. [VERIFIED: AGENTS.md; docs/conventions.md]
- **Per wave merge:** Use JetBrains MCP `:eyelib-particle:compileJava` and root `:compileJava` after dependency wiring. [VERIFIED: docs/conventions.md]
- **Phase gate:** JetBrains MCP Gradle checks green; no shell Gradle commands in plan or docs. [VERIFIED: AGENTS.md]

### Wave 0 Gaps
- [ ] `eyelib-particle/build.gradle` — covers PGRAD-01. [VERIFIED: PGRAD-01]
- [ ] `eyelib-particle/src/main/resources/META-INF/mods.toml` — covers Forge-visible resource metadata if chosen. [VERIFIED pattern: eyelib-material/src/main/resources/META-INF/mods.toml]
- [ ] `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` — covers PGRAD-02/PAPI-02. [VERIFIED: PGRAD-02/PAPI-02]
- [ ] `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md` updates — covers project documentation rules. [VERIFIED: AGENTS.md; MODULES.md]

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | no | No authentication/session behavior is in Phase 8. [VERIFIED: .planning/ROADMAP.md] |
| V3 Session Management | no | No sessions or user state are in Phase 8. [VERIFIED: .planning/ROADMAP.md] |
| V4 Access Control | no | No runtime authorization boundary changes are in Phase 8. [VERIFIED: .planning/ROADMAP.md] |
| V5 / ASVS v5 Encoding and Sanitization / injection-related controls | limited | Avoid executable shell Gradle invocation in docs; use JetBrains MCP tools. OWASP ASVS provides technical security verification guidance; this phase primarily uses build/process controls. [CITED: https://owasp.org/www-project-application-security-verification-standard/] [VERIFIED: docs/conventions.md] |
| V6 Cryptography | no | No cryptographic behavior is in Phase 8. [VERIFIED: .planning/ROADMAP.md] |

### Known Threat Patterns for Build/Module Boundary Work

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Supply-chain/build contamination through ad-hoc file dependencies | Tampering | Use Gradle project dependencies and repository-backed dependencies, not raw file dependencies. [CITED: https://docs.gradle.org/current/userguide/declaring_dependencies_basics.html] |
| Runtime classpath confusion from reverse/root dependency | Tampering / Elevation of privilege by boundary bypass | Keep `:eyelib-particle` free of `project(':')` and forbidden root imports; compile module independently. [VERIFIED: .planning/REQUIREMENTS.md] |
| Unsafe verification instruction execution | Tampering | Do not document shell `./gradlew`; use JetBrains MCP only. [VERIFIED: AGENTS.md; docs/conventions.md] |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/08-boundary-contract-gradle-module-skeleton/08-CONTEXT.md` — phase boundary, discretion, deferred ideas. [VERIFIED]
- `.planning/REQUIREMENTS.md` — PGRAD-01/PGRAD-02/PAPI-02 and traceability. [VERIFIED]
- `.planning/ROADMAP.md` and `.planning/STATE.md` — Phase 8 goal/success criteria and milestone decisions. [VERIFIED]
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `docs/conventions.md` — project constraints and documentation/update rules. [VERIFIED]
- `settings.gradle`, root `build.gradle`, `gradle.properties`, and existing subproject `build.gradle` files — local Gradle conventions and versions. [VERIFIED]
- JetBrains MCP searches in current particle package — current root particle package has MC/Forge/root dependencies, so Phase 8 should not naively move it. [VERIFIED]

### Secondary (MEDIUM-HIGH confidence)
- Gradle Multi-Project Builds docs — `settings.gradle include()` and project dependencies. [CITED: https://docs.gradle.org/current/userguide/multi_project_builds.html]
- Gradle Java Library Plugin docs — `api`/`implementation` separation and library conventions. [CITED: https://docs.gradle.org/current/userguide/java_library_plugin.html]
- Gradle Java/JVM project docs — source sets/resources/toolchains/testing conventions. [CITED: https://docs.gradle.org/current/userguide/building_java_projects.html]
- Gradle Toolchains docs — Java toolchain best practice. [CITED: https://docs.gradle.org/current/userguide/toolchains.html]
- NeoForged ModDevGradle docs — ModDevGradle/IntelliJ/mod metadata/run support. [CITED: https://docs.neoforged.net/toolchain/docs/plugins/mdg/]
- OWASP ASVS project page — security verification framing. [CITED: https://owasp.org/www-project-application-security-verification-standard/]

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions and patterns are repository-pinned in existing Gradle files; external docs confirm Gradle concepts. [VERIFIED: build files] [CITED: Gradle docs]
- Architecture: HIGH — phase context, requirements, roadmap, and module-boundary docs align on one-way root → particle dependency. [VERIFIED: planning/docs files]
- Pitfalls: MEDIUM-HIGH — contamination findings are verified by IDE text search, but exact future extraction design belongs to later phases. [VERIFIED: IDE search; .planning/ROADMAP.md]

**Research date:** 2026-05-09  
**Valid until:** 2026-06-08 for repository-local conventions; re-check external Gradle/ModDevGradle docs before changing plugin versions. [ASSUMED]
