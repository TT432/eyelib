# Stack Research — `:eyelib-particle` Module Separation

**Project:** Eyelib v1.2 particle module extraction  
**Researched:** 2026-05-09  
**Overall confidence:** HIGH for current repository facts; MEDIUM for exact final dependency scopes until moved code is compiled.

## Summary

Introduce `:eyelib-particle` as a normal Gradle subproject beside `:eyelib-material`, `:eyelib-importer`, `:eyelib-molang`, `:eyelib-processor`, and `:eyelib-attachment`. Do **not** add a new build system, framework, loader, or external particle library. The right stack change is a repository-local module boundary using the existing **Gradle + Java 17 + Forge 1.20.1 + ModDevGradle LegacyForge 2.0.91** pattern.

Recommended shape: make `:eyelib-particle` the owner of particle definitions, component registry, emitters, render manager, particle lookup/spawn ports, and any particle-local Forge/client binding that is needed to keep behavior intact. Root should consume it through `project(':eyelib-particle')` and keep only orchestration that still belongs to root composition, network transport, command registration, capability data, or cross-feature animation/controller integration.

The hard build invariant is one-way dependency direction: **root runtime depends on `:eyelib-particle`; `:eyelib-particle` must not depend on root runtime packages**. Any current root utility/capability/network references in particle code must be moved to a reusable module or replaced by ports/adapters, not solved with a circular project dependency.

## Current Stack Facts

| Fact | Evidence | Confidence |
|---|---|---|
| Repository already uses multi-project Gradle modules. | `settings.gradle` includes `eyelib-attachment`, `eyelib-importer`, `eyelib-material`, `eyelib-molang`, `eyelib-processor`. | HIGH |
| Root build stack is Java 17 + Forge 1.20.1 + MDGL LegacyForge 2.0.91. | Root `build.gradle` plugins and `.planning/PROJECT.md` constraints. | HIGH |
| Existing Forge-aware feature modules use `java-library`, Lombok, `net.neoforged.moddev.legacyforge`, `maven-publish`, `legacyForge.mods`, `processResources`, and `withSourcesJar()`. | `eyelib-material`, `eyelib-importer`, `eyelib-attachment` build files. | HIGH |
| Root currently consumes feature modules through `api`, `modImplementation`, and `jarJar`; plain JVM modules use `additionalRuntimeClasspath` where needed. | Root `dependencies` block. | HIGH |
| Particle runtime is currently root-owned under `src/main/java/io/github/tt432/eyelib/client/particle/`. | Particle README and package tree. | HIGH |
| Existing particle seams are already partially platform-type-free: `ParticleSpawnRequest` uses `String` ids; `SpawnParticlePacket` also carries string particle ids. | Particle README, `ParticleSpawnRequest`, `SpawnParticlePacket`. | HIGH |
| Current particle code is not pure JVM: it imports Minecraft, Forge client events, Blaze3D, material rendering, Molang, root utilities, root manager/capability, and root network packet classes. | Import scan and sampled files (`BrParticleRenderManager`, `ParticleSpawnService`, `BrParticle`, `BrParticleLoader`). | HIGH |
| Importer already owns a separate particle schema record. | `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`. | HIGH |
| Forge side boundaries matter: client-only classes must not be accidentally loaded on physical server; cross logical-side communication should use packets. | Forge 1.20.1 side docs; repo side-boundary doc. | HIGH |
| Gradle `java-library` supports `api` vs `implementation` separation; `api` leaks to consumers, `implementation` does not. | Gradle Java Library Plugin docs. | HIGH |

## Recommended Stack Changes

### 1. Add the Gradle project

```groovy
// settings.gradle
include("eyelib-particle")
```

Create:

```text
eyelib-particle/
  build.gradle
  src/main/java/io/github/tt432/eyelibparticle/...
  src/main/resources/META-INF/mods.toml
  src/test/java/...
```

Use a new package root (`io.github.tt432.eyelibparticle`) to avoid split packages with root `io.github.tt432.eyelib.*`, matching the importer/material module style.

### 2. Use the existing Forge-aware subproject template

Use the `eyelib-material`/`eyelib-importer` pattern, not a new convention plugin during this milestone:

```groovy
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base {
    archivesName = 'eyelib-particle'
}

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
        eyelibparticle {
            sourceSet(sourceSets.main)
        }
    }
}
```

Rationale: particle runtime currently needs Minecraft/Forge/Blaze3D types and can legally keep MC/Forge binding outside root per user clarification, but it still needs MDGL so those types resolve consistently with the rest of the build.

### 3. Recommended `:eyelib-particle` dependencies

Start conservative and tighten after compile feedback:

```groovy
dependencies {
    // Public particle definitions currently expose Molang types in record fields/method signatures.
    api project(':eyelib-molang')

    // Particle rendering uses material render helpers, but do not expose material types in particle API unless necessary.
    implementation project(':eyelib-material')

    // Use only for importer-schema-to-runtime adaptation; do not duplicate importer normalization logic.
    implementation project(':eyelib-importer')

    // Use only if the particle loader moves into this module and continues to call LoaderParsingOps.
    implementation project(':eyelib-processor')

    // Add only if packet/request stream codecs move into particle-owned contracts.
    implementation project(':eyelib-attachment')

    implementation 'org.slf4j:slf4j-api:2.0.7'
    compileOnly 'org.jspecify:jspecify:1.0.0'
    compileOnly "io.github.tt432:clientsmoke:${project.version}"

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Dependency direction rules:

- `:eyelib-particle -> :eyelib-molang` is expected: particle curves/runtime evaluate Molang values.
- `:eyelib-particle -> :eyelib-material` is expected for render type/material resolution.
- `:eyelib-particle -> :eyelib-importer` is acceptable only for schema adaptation; importer must remain schema/normalization owner.
- `:eyelib-particle -> :eyelib-processor` is acceptable only for plain parsing/batching helpers; processor must not gain runtime publication/render ownership.
- `:eyelib-particle -> root project` is **not allowed**; it defeats the module separation.

### 4. Root build dependency change

Add root consumption consistent with current feature modules:

```groovy
dependencies {
    api project(':eyelib-particle')
    modImplementation project(':eyelib-particle')
    jarJar project(':eyelib-particle')
}
```

Reason: existing root modules use this pattern for Forge-aware feature artifacts that must be visible to root consumers and present in dev/runtime packaging. If later the public root API no longer exposes particle types, `api` can be revisited, but starting with the repository’s current module pattern minimizes packaging regression risk.

### 5. `mods.toml` metadata

Add `eyelib-particle/src/main/resources/META-INF/mods.toml` modeled after `eyelib-material`:

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="eyelibparticle"
version="${mod_version}"
displayName="Eyelib Particle"
authors="${mod_authors}"
description='''Particle definitions, emitters, render manager, and particle runtime seams for Eyelib.'''

[[dependencies.eyelibparticle]]
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.eyelibparticle]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

Do not set `clientSideOnly=true` as a shortcut unless phase-specific research proves the separated artifact is intentionally absent/ignored on servers. Current Eyelib integration still has command/network/server-to-client spawn behavior, so the safer milestone stance is: keep server-visible contracts in root/network, keep client-only particle rendering guarded by Forge side/event boundaries.

### 6. What NOT to add

- No new Gradle plugin or build convention layer for v1.2; copy the established module pattern first.
- No external particle engine/library.
- No new Molang implementation inside particle; depend on `:eyelib-molang`.
- No duplicate Bedrock particle schema normalization in particle; use/import from `:eyelib-importer` where schema ownership applies.
- No dependency from `:eyelib-particle` back to the root project.
- No new top-level `Eyelib` singleton reach-through for particle access.
- No direct network/command calls into `BrParticleRenderManager`; keep `ParticleSpawnService`/ports as the boundary.
- No Gradle execution from shell; verification must use JetBrains MCP when performed.

## Integration Points

| Area | Current owner | Recommended v1.2 integration |
|---|---|---|
| Resource reload registration | Root `mc/impl/client/loader/ClientLoaderLifecycleHooks` registers `BrParticleLoader.INSTANCE`. | Either register a particle-module loader from root lifecycle, or move the lifecycle hook into `:eyelib-particle` with `Dist.CLIENT` guard. Do not leave root importing old root particle packages. |
| Particle loader parsing | Root `client/loader/BrParticleLoader` parses runtime `BrParticle.CODEC` and publishes via `ParticleAssetRegistry`. | Move loader/publication adapter to particle module, or keep a root adapter that calls particle module ports. Prefer particle-owned loader if it can avoid root-only loader base dependency. |
| Manager publication | Root `ParticleManager` + `ParticleAssetRegistry`. | Move particle store/read-write ports to `:eyelib-particle`; root consumers call `ParticleLookup` from particle module. Preserve manager pattern semantics. |
| Spawn/remove packet apply | `NetClientHandlers` delegates to `ParticleSpawnService`. | Keep network transport/packet registration in root `mc/impl/network`; delegate to particle module `ParticleSpawnService` or an adapter interface. Packet contracts may remain root-owned unless moving them reduces coupling without widening side risk. |
| `/eyelib particle` command | Root `mc/impl/common/command/EyelibParticleCommand`. | Keep command registration in root/platform integration unless deliberately moving command binding; command should ask particle module for names via a narrow lookup and send root network packet. |
| Animation/controller particle effects | Root animation classes import `ParticleLookup`/`ParticleSpawnService`. | Update imports to `io.github.tt432.eyelibparticle...` ports. Do not let animation depend on particle internals beyond lookup/spawn seams. |
| Rendering and ticking | `BrParticleRenderManager` owns Forge client events and render/tick caches. | May move into `:eyelib-particle`; ensure event subscribers remain `Dist.CLIENT` and no dedicated-server classloading path is introduced. |
| Capability/Molang scope bridge | `ParticleSpawnService` currently reaches root capability/data-attach and `RenderData`. | Replace root reach-through with a root-provided spawn context/scope provider or keep the capability adapter in root while module owns emitter/render services. This is the main anti-cycle seam. |

## Risks

| Risk | Why it matters | Mitigation / requirement input |
|---|---|---|
| Circular dependency pressure | Current particle code imports root utilities, manager, capability, packet, and `mc/impl` classes; root must depend on particle after extraction. | Require `:eyelib-particle` to have zero root-project dependency. Move small utilities to reusable modules or add ports/adapters. |
| Client classloading regression | Particle renderer uses `Minecraft`, Blaze3D, Forge client events, and `net.minecraft.client` classes. | Keep client-only code under `Dist.CLIENT` event registration and avoid loading render classes from common/server paths. Verify with compile/static inspection and existing client smoke flow. |
| Schema/runtime duplication | Root runtime `BrParticle` and importer `BrParticle` already both exist. | Requirement should force a documented adapter decision: importer owns parsed/raw schema; particle owns runtime executable definition. No silent duplicate normalization. |
| Dependency leakage through `api` | `api` dependencies become consumer compile classpath. | Use `api` only where public particle signatures expose types; otherwise prefer `implementation`, per Gradle Java Library guidance. Revisit after compile/API inventory. |
| Packaging metadata mismatch | Separate Forge-aware modules need metadata and runtime availability. | Add `mods.toml`, `processResources`, and root `modImplementation`/`jarJar` following existing feature modules. |
| Behavior regression through moved lifecycle hooks | Loader registration, manager publication, command suggestions, packet apply, tick/render events all interact. | Preserve the same integration call graph initially; move ownership with adapters before deleting root paths. |

## Requirement Inputs

Suggested requirements for roadmap/planning:

1. **Gradle project exists:** `settings.gradle` includes `eyelib-particle`; module has `build.gradle`, Java 17 toolchain, MDGL LegacyForge config, `mods.toml`, sources jar, tests, and resource expansion.
2. **Root consumes particle:** root `build.gradle` adds `api`, `modImplementation`, and `jarJar` on `project(':eyelib-particle')` unless implementation proves a narrower scope is safe.
3. **No reverse root dependency:** `:eyelib-particle` compiles without depending on root project packages. Any needed root capability/network/bootstrap access goes through root adapters or extracted reusable modules.
4. **Particle ownership documented:** `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, and the particle README describe `:eyelib-particle` responsibility, package root, dependencies, and side rules.
5. **Integration points preserved:** resource reload, `ParticleManager` publication, `ParticleLookup`, `ParticleSpawnService`, `/eyelib particle`, spawn/remove packet handlers, animation/controller particle effects, and render/tick event behavior remain behavior-compatible.
6. **Importer boundary explicit:** particle schema/runtime split is documented; importer-owned schema is not reimplemented silently inside particle.
7. **Verification tasks to plan, not run here:** via JetBrains MCP only, plan `:eyelib-particle:compileJava`, `:eyelib-particle:test`, root `compileJava`/`test` or repository `check`, `nullawayMain` if null-safety touched, and existing `clientSmoke`/manual visual flow for particle spawn/render regression.

## Sources

- Repo files: `.planning/PROJECT.md`, `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `settings.gradle`, root `build.gradle`, `eyelib-material/build.gradle`, `eyelib-importer/build.gradle`, `eyelib-processor/build.gradle`, particle README and sampled particle/network/loader files. **Confidence: HIGH**.
- Gradle Java Library Plugin docs, API vs implementation separation: https://docs.gradle.org/current/userguide/java_library_plugin.html. **Confidence: HIGH**.
- Forge 1.20.1 side docs: https://docs.minecraftforge.net/en/1.20.1/concepts/sides/. **Confidence: HIGH**.
