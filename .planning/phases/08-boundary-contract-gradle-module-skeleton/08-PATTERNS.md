# Phase 08: Boundary Contract & Gradle Module Skeleton - Pattern Map

**Mapped:** 2026-05-09  
**Files analyzed:** 11 new/modified files  
**Analogs found:** 11 / 11

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `settings.gradle` | config | build graph / request-response to Gradle | `settings.gradle` existing include block | exact |
| `build.gradle` | config | dependency wiring / build graph | root `build.gradle` existing subproject dependency block | exact |
| `eyelib-particle/build.gradle` | config | module build / batch | `eyelib-material/build.gradle` + `eyelib-attachment/build.gradle` | exact |
| `eyelib-particle/src/main/resources/META-INF/mods.toml` | config | resource metadata transform | `eyelib-material/src/main/resources/META-INF/mods.toml` | exact |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` | documentation | boundary contract | `eyelib-processor/.../README.md` + `client/particle/README.md` | role-match |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java` | model/documentation | compile-time nullness metadata | `eyelib-attachment/.../dataattach/package-info.java` | exact |
| `MODULES.md` | documentation | module inventory update | `MODULES.md` subproject rows | exact |
| `docs/index/repo-map.md` | documentation | navigation/index update | `docs/index/repo-map.md` repository summary/topic bullets | exact |
| `docs/architecture/01-module-boundaries.md` | documentation | boundary ownership map | `docs/architecture/01-module-boundaries.md` subproject boundary notes | exact |
| `docs/architecture/02-side-boundaries.md` | documentation | side/dependency rules | `docs/architecture/02-side-boundaries.md` subproject zone rules | exact |
| `eyelib-particle/src/test/**` / `src/test/resources/**` placeholders if created | test/config | batch verification scaffold | `eyelib-material/build.gradle` test sourceSet + JUnit config | role-match |

## Pattern Assignments

### `settings.gradle` (config, build graph)

**Analog:** `settings.gradle`

**Include pattern** (lines 14-20):
```groovy
rootProject.name = "eyelib"

include("eyelib-attachment")
include("eyelib-importer")
include("eyelib-material")
include("eyelib-molang")
include("eyelib-processor")
```

**Apply:** add `include("eyelib-particle")` beside the other first-class subprojects; keep `includeBuild("clientsmoke")` separate (line 22).

---

### `build.gradle` (config, root dependency wiring)

**Analog:** root `build.gradle`

**Forge-visible subproject dependency pattern** (lines 148-163):
```groovy
dependencies {
    api project(':eyelib-attachment')
    modImplementation project(':eyelib-attachment')
    jarJar project(':eyelib-attachment')
    api project(':eyelib-material')
    modImplementation project(':eyelib-material')
    jarJar project(':eyelib-material')
    api project(':eyelib-processor')
    additionalRuntimeClasspath project(':eyelib-processor')
    jarJar project(':eyelib-processor')
    api project(':eyelib-importer')
    modImplementation project(':eyelib-importer')
    jarJar project(':eyelib-importer')
    api project(':eyelib-molang')
    additionalRuntimeClasspath project(':eyelib-molang')
    jarJar project(':eyelib-molang')
```

**Apply:** for a Forge-visible particle module, follow the material/attachment/importer pattern: `api`, `modImplementation`, and `jarJar` from root to `:eyelib-particle`. Do **not** add any dependency from `:eyelib-particle` back to root.

---

### `eyelib-particle/build.gradle` (config, module build)

**Analog:** `eyelib-material/build.gradle`

**Plugin/version/toolchain pattern** (lines 1-15):
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
    archivesName = 'eyelib-material'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)
```

**Source set/repository/Forge mod pattern** (lines 17-43):
```groovy
sourceSets {
    test {
        compileClasspath += sourceSets.main.output + sourceSets.main.compileClasspath
        runtimeClasspath += sourceSets.main.output + sourceSets.main.runtimeClasspath
    }
}

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
        eyelibmaterial {
            sourceSet(sourceSets.main)
        }
    }
}
```

**Dependencies/test/publishing pattern** (lines 45-93):
```groovy
dependencies {
    implementation 'org.slf4j:slf4j-api:2.0.7'

    compileOnly "io.github.tt432:clientsmoke:${project.version}"
    compileOnly 'org.jspecify:jspecify:1.0.0'

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test').configure {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
            minecraft_version_range: minecraft_version_range,
            forge_version_range    : forge_version_range,
            loader_version_range   : loader_version_range,
            mod_version            : mod_version,
            mod_license            : mod_license,
            mod_authors            : mod_authors,
            mod_description        : 'Bedrock material definitions and GL state management for Eyelib.'
    ]
    inputs.properties(replaceProperties)
    filesMatching('META-INF/mods.toml') {
        expand(replaceProperties)
    }
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }
    repositories {
        mavenLocal()
    }
}
```

**Apply:** replace archive/mod ids/descriptions with `eyelib-particle` / `eyelibparticle`. Keep dependencies minimal: `compileOnly 'org.jspecify:jspecify:1.0.0'` and JUnit are enough unless the skeleton adds code that exposes other module types. Avoid `implementation project(':')`.

---

### `eyelib-particle/src/main/resources/META-INF/mods.toml` (config, resource metadata)

**Analog:** `eyelib-material/src/main/resources/META-INF/mods.toml`

**Forge metadata pattern** (lines 1-24):
```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="eyelibmaterial"
version="${mod_version}"
displayName="Eyelib Material"
authors="${mod_authors}"
description='''Bedrock material definitions and GL state management for Eyelib.'''

[[dependencies.eyelibmaterial]]
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.eyelibmaterial]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

**Apply:** use `modId="eyelibparticle"`, display name `Eyelib Particle`, and a short description about particle module APIs/core boundary. Keep only Forge and Minecraft dependencies unless a real code dependency is added.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` (documentation, boundary contract)

**Analogs:** `eyelib-processor/.../README.md`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`, `eyelib-importer/.../addon/README.md`

**Module README scope pattern** (`eyelib-processor/.../README.md`, lines 1-19):
```markdown
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
```

**Particle-specific runtime boundary wording** (`client/particle/README.md`, lines 3-17):
```markdown
## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Current Runtime Boundaries
- `ParticleLookup.java`: read-side access to particle definitions through the runtime manager boundary
- `ParticleSpawnService.java`: packet-driven spawn/remove orchestration on the client side
- `ParticleSpawnRequest.java`: platform-type-free spawn request state (`String` ids + position) used by runtime spawn orchestration

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
```

**Importer boundary wording** (`eyelib-importer/.../addon/README.md`, lines 7-14):
```markdown
## Boundary intent
- Keep pack discovery, `manifest.json` parsing, importer-owned schema loading, texture decoding, and raw resource file collection here.
- Keep runtime adaptation, manager publication, texture upload, render-controller execution, particle runtime behavior, and Minecraft/Forge lifecycle wiring in root runtime packages.

## Editing rules
- Do not add Minecraft/Forge runtime types here.
- Prefer returning plain importer-side data structures that root can adapt later.
- When a resource family is still root-owned at runtime (for example render controllers or particles), capture the raw loaded payload here instead of pulling runtime execution code into importer.
```

**Apply:** README must state root may consume `:eyelib-particle`; `:eyelib-particle` must not depend on root runtime packages, managers, registries, packets, capability helpers, or `mc/impl`; MC/Forge integration needs explicit adapter documentation before introduction.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java` (model/documentation, compile-time metadata)

**Analog:** `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/dataattach/package-info.java`

**Boundary + NullMarked pattern** (lines 1-11):
```java
/**
 * Platform-type-free data-attachment contracts and mutation helpers.
 * <p>
 * This package owns typed attachment state concerns without direct Minecraft/Forge types.
 * Packet routing belongs in the network layer, and capability/event/NBT runtime wiring belongs in
 * {@code io.github.tt432.eyelib.mc.impl.data_attach}.
 */
@NullMarked
package io.github.tt432.eyelibattachment.dataattach;

import org.jspecify.annotations.NullMarked;
```

**Apply:** use `package io.github.tt432.eyelibparticle;`, keep `@NullMarked`, and adapt the Javadoc to particle boundary ownership and forbidden root dependency direction.

---

### `MODULES.md` (documentation, module inventory)

**Analog:** `MODULES.md`

**Subproject inventory rows** (lines 38-42):
```markdown
| Resource processor subproject | Plain-JVM processing/batching seams for manager reload planning, loader parsing helpers, and importer-backed particle flipbook numeric summary helpers, without runtime publication/upload/event/UI ownership | `eyelib-processor/build.gradle`, `eyelib-processor/src/main/java/`, `eyelib-processor/src/main/resources/META-INF/mods.toml`, `eyelib-processor/src/test/` | consumed directly by root; code lives under `io.github.tt432.eyelibprocessor.*`, may depend on importer-owned schema modules plus plain-JVM Molang analysis from `:eyelib-molang`, and has no dependency back into root runtime packages |
| Resources importer subproject | Independently consumable importer mod/artifact for importer-owned resource definitions, source parsing, normalization, Bedrock addon/pack discovery, importer image/data representations, typed particle flipbook extraction, and importer-focused tests for model plus client-entity/animation-controller/particle schema slices | `eyelib-importer/build.gradle`, `eyelib-importer/src/main/java/`, `eyelib-importer/src/main/resources/META-INF/mods.toml`, `eyelib-importer/src/test/` | consumed directly by root for importer-owned schemas/runtime adapters; importer-owned code now lives under `io.github.tt432.eyelibimporter.*`, including addon/pack discovery and raw resource-side aggregation, while runtime execution/managers/upload stay in root |
| Molang engine subproject | Gradle subproject for Molang-owned value/runtime wrappers, scope/compiler/generated parser, built-in mappings, mapping-api/type code, and related tests | `eyelib-molang/build.gradle`, `eyelib-molang/src/main/java/`, `eyelib-molang/src/test/` | consumed by root via `modImplementation project(':eyelib-molang')`; code now lives under `io.github.tt432.eyelibmolang.*`, while root keeps only `mc/impl/molang/**` platform bindings |
| eyelib-material subproject | Bedrock material definitions, GL state management, shader pipeline, and material-specific ClientSmoke fixtures | `eyelib-material/build.gradle`, `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`, `eyelib-material/src/main/resources/assets/eyelibmaterial/shaders/smoke.*` | consumed by root render controllers and particle renderer; contains `@ClientSmoke` material checks discovered when the external ClientSmoke submodule is present |
```

**Update rule pattern** (lines 145-150):
```markdown
1. Before any change, identify every affected module in this file.
2. If a change modifies code or docs inside a listed module, update that module’s summary if its responsibility, paths, or interactions changed.
3. If a change adds a new module or removes an existing module, update this file in the same change.
4. If a change alters module boundaries, also update `docs/architecture/01-module-boundaries.md` and any relevant package README files.
5. If a change affects packet/data-attachment/client-side applicability, also re-check `docs/architecture/02-side-boundaries.md`.
```

**Apply:** add a Root/Documentation module row for `eyelib-particle` with paths `eyelib-particle/build.gradle`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`, `eyelib-particle/src/main/resources/META-INF/mods.toml`, `eyelib-particle/src/test/`; interactions should explicitly say root consumes it and it has no reverse dependency to root runtime.

---

### `docs/index/repo-map.md` (documentation, navigation/index)

**Analog:** `docs/index/repo-map.md`

**Repository shape summary pattern** (lines 10-14):
```markdown
## What This Repository Is
- Eyelib is a multi-project `Gradle + Java 17 + Forge` rendering library for Minecraft (`:` root runtime + `:eyelib-processor` processing/batching core + `:eyelib-importer` importer/model core + `:eyelib-molang` engine Molang core + `:eyelib-material` material core + composite-build `clientsmoke` submodule).
- Forge bootstrap entrypoint: `src/main/java/io/github/tt432/eyelib/mc/impl/bootstrap/EyelibMod.java`
- Compatibility constant holder: `src/main/java/io/github/tt432/eyelib/Eyelib.java`
- Current codebase pressure points are client tooling, generated Molang grammar files, loader/publication flow, and sync/data-attachment boundaries.
```

**Topic routing pattern** (lines 16-28):
```markdown
## Where To Read By Topic
- Client rendering/runtime: start in `src/main/java/io/github/tt432/eyelib/client/`
- Loader and resource ingestion: start in `src/main/java/io/github/tt432/eyelib/client/loader/` for root-side reload orchestration and runtime adaptation flow; platform-free parsing/planning helpers should move into `:eyelib-processor`, and importer-only parsing/normalization should move into `:eyelib-importer`
- Importer/model/schema core data and fixtures: start in `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/model/` and nearby importer-owned schema packages as client-entity/animation-controller parsing expands into the subproject; importer fixtures live under `eyelib-importer/src/test/resources/io/github/tt432/eyelib/client/model/importer/`
```

**Apply:** extend summary with `:eyelib-particle` and add a particle topic bullet pointing to `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/` for the new module contract and root `src/main/java/io/github/tt432/eyelib/client/particle/` for current runtime until later phases move code.

---

### `docs/architecture/01-module-boundaries.md` (documentation, boundary ownership)

**Analog:** `docs/architecture/01-module-boundaries.md`

**Current major subproject pattern** (lines 3-10):
```markdown
## Current Major Areas
- `eyelib-processor/src/main/java/io/github/tt432/eyelibprocessor/`: platform-free processing and batching helpers for loader and manager reload planning seams.
- `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/model/`: model definitions, locators, source formats (`bbmodel`, `bedrock`), and importer normalization/support data.
- `src/main/java/io/github/tt432/eyelib/client/`: client rendering, animation, particles, GUI, loaders, managers, tooling.
- `src/main/java/io/github/tt432/eyelib/molang/`: legacy Molang marker/docs handoff path.
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/`: engine-owned Molang value/runtime wrappers, scope/compiler/type/mapping-api/built-in mappings, plus generated grammar artifacts.
- `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`: Bedrock material definitions, GL state management, shader pipeline, and shared pure-data types under `shared/` package.
- `clientsmoke/`: external standalone client smoke framework and annotation API, consumed through a Gradle composite build and kept independent from feature modules.
```

**Ownership map pattern** (lines 49-70):
```markdown
## Current To Target Ownership Map
| Current area | Target owner | Boundary intent |
|---|---|---|
| `:eyelib-processor/**` | `client.processing` | Own platform-free processing and batching helpers for loader/reload planning; may consume importer schema and plain-JVM Molang analysis, but must not own runtime manager publication, texture upload, Forge events, RenderSystem/session lifecycle, or UI |
| `:eyelib-importer/**` | `client.model.importer.core` | Own model definitions plus source-format parsing/normalization/import support, addon/pack discovery, and raw resource-side aggregation without runtime manager/event/upload ownership |
| `eyelib-molang/**` | `molang.engine` | Own Molang value/runtime, compile/type/scope/mapping-api, and built-in mappings without depending on root runtime packages |
```

**Apply:** add `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/` as current major area and `:eyelib-particle/**` as target owner for particle module core/contracts. State Phase 8 is a skeleton only and current root particle runtime remains in `src/main/java/io/github/tt432/eyelib/client/particle/` until later phases.

---

### `docs/architecture/02-side-boundaries.md` (documentation, side/dependency rules)

**Analog:** `docs/architecture/02-side-boundaries.md`

**Subproject side-rule pattern** (lines 20-23):
```markdown
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java` is now a platform-type-free client request seam (`String` ids + state), and `network/SpawnParticlePacket` now also uses a string-keyed particle id contract; Minecraft identifier validation/adaptation stays in `mc/impl` command/network runtime wiring.
- `:eyelib-processor` is a processor zone for platform-free parsing/classification/batching seams (`io.github.tt432.eyelibprocessor.*`): it may depend on plain JVM/codec/importer modules and engine-side plain-JVM Molang analysis from `:eyelib-molang`, but it must not own runtime publication/upload/event/UI or other Minecraft/Forge lifecycle bindings.
- `:eyelib-importer` is an importer/schema zone: it may own codecs, parsed definitions, Molang-compatible value types, normalization logic, and importer-only image/data representations, but it must not own GUI/runtime execution, `NativeImage` upload/download boundaries, or Forge/Minecraft lifecycle wiring.
```

**Cross-zone dependency rule pattern** (lines 24-33):
```markdown
- Importer/schema code may depend on plain JVM data, codecs, and engine modules such as `:eyelib-molang`, but new Minecraft/Forge runtime dependencies require explicit boundary documentation before introduction.
- Processor code may depend on plain JVM data and, when needed, importer/schema modules plus `:eyelib-molang` analysis helpers, but it must remain one-way with no dependency from `:eyelib-processor` back to root runtime packages; the current stage keeps root runtime explicit about any direct importer dependencies it still owns.
- New cross-zone dependencies need a written reason in the relevant architecture doc before they are introduced.
```

**Apply:** add `:eyelib-particle` as a particle module zone. State it must not depend on root runtime packages or `mc/impl`; pure particle core should remain platform-free unless future adapter docs explicitly allow Minecraft/Forge-facing integration.

---

### `eyelib-particle/src/test/**` placeholders if created (test/config, batch verification scaffold)

**Analog:** `eyelib-material/build.gradle`

**JUnit sourceSet and test task pattern** (lines 17-21, 51-61):
```groovy
sourceSets {
    test {
        compileClasspath += sourceSets.main.output + sourceSets.main.compileClasspath
        runtimeClasspath += sourceSets.main.output + sourceSets.main.runtimeClasspath
    }
}

dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test').configure {
    useJUnitPlatform()
}
```

**Apply:** only add tests/placeholders if they are meaningful and do not require root runtime imports. For Phase 8, `:eyelib-particle:compileJava` via JetBrains MCP is the primary structural check.

## Shared Patterns

### One-way dependency boundary
**Source:** `08-CONTEXT.md` lines 7-10; `08-RESEARCH.md` lines 171-180  
**Apply to:** `build.gradle`, `eyelib-particle/build.gradle`, docs
```markdown
Root runtime may consume `:eyelib-particle`, but `:eyelib-particle` must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.
```

### No Phase 8 runtime moves
**Source:** `08-CONTEXT.md` lines 48-52; `08-RESEARCH.md` lines 183-187  
**Apply to:** all implementation planning
```markdown
Moving particle APIs, schema/runtime adapters, loader publication, command/network integration, and verification coverage is deferred to Phases 9-14.
```

### JetBrains MCP-only Gradle verification
**Source:** `AGENTS.md` lines 28-31; `docs/conventions.md` lines 11-16  
**Apply to:** plan verification sections and docs
```markdown
- ALL Gradle commands must use JetBrains MCP (`jetbrain_run_gradle_tasks`) or IDE MCP tools.
- Never run `./gradlew ...` directly in shell.
- If MCP is unavailable: stop and ask the user to re-enable MCP before continuing.
```

### Documentation fan-out for new modules
**Source:** `AGENTS.md` lines 14-21; `MODULES.md` lines 145-150  
**Apply to:** `MODULES.md`, repo map, architecture docs, module README
```markdown
- If a module is added or removed, update `MODULES.md` and any impacted index/architecture docs in the same change.
- If a change alters module boundaries, also update `docs/architecture/01-module-boundaries.md` and any relevant package README files.
```

### Nullness package metadata
**Source:** `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/package-info.java` lines 1-4  
**Apply to:** new Java package roots
```java
@NullMarked
package io.github.tt432.eyelibmolang;

import org.jspecify.annotations.NullMarked;
```

## No Analog Found

None. Every planned file has an existing build/config/documentation analog. The only caution is that there is no existing `eyelib-particle` subproject yet; use material/attachment/importer for Gradle/resource metadata and current `client/particle` docs only for particle-domain wording, not for moving runtime code.

## Metadata

**Analog search scope:** root Gradle files, existing subproject Gradle files, subproject `mods.toml`, module/package READMEs, `MODULES.md`, repo map, architecture docs, package-info files  
**Files scanned:** 21  
**Pattern extraction date:** 2026-05-09
