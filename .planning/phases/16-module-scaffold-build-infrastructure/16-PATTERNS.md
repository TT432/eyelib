# Phase 16: Module Scaffold & Build Infrastructure - Pattern Map

**Mapped:** 2026-05-10  
**Files analyzed:** 10  
**Analogs found:** 10 / 10

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `settings.gradle` | config | build graph / batch | `settings.gradle` sibling include block | exact |
| `eyelib-util/build.gradle` | config | build / batch | `eyelib-attachment/build.gradle` | exact-minus-deps |
| `eyelib-util/src/main/resources/META-INF/mods.toml` | config | Forge metadata / request-response runtime discovery | `eyelib-attachment/src/main/resources/META-INF/mods.toml` | exact |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/bootstrap/EyelibUtilMod.java` | bootstrap | event-driven Forge load | `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/bootstrap/EyelibAttachmentMod.java` | exact |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/package-info.java` | documentation/config | boundary metadata | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java` | role-match |
| `eyelib-util/README.md` | documentation | ownership/boundary | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` + `eyelib-attachment/.../README.md` | role-match |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md` | documentation | package navigation | `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/README.md` | exact |
| `eyelib-util/src/test/java/io/github/tt432/eyelibutil/UtilModuleIdentityTest.java` | test | batch/static validation | `eyelib-attachment/src/test/java/io/github/tt432/eyelibattachment/AttachmentModuleIdentityTest.java` | role-match |
| `MODULES.md` | documentation | module inventory update | `MODULES.md` module rows and update rules | exact |
| `docs/architecture/01-module-boundaries.md` / `docs/index/repo-map.md` | documentation | architecture/index update | existing boundary and repo-map module summaries | role-match |

## Pattern Assignments

### `settings.gradle` (config, build graph)

**Analog:** `settings.gradle`

**Include-list pattern** (lines 14-21):
```groovy
rootProject.name = "eyelib"

include("eyelib-attachment")
include("eyelib-importer")
include("eyelib-material")
include("eyelib-molang")
include("eyelib-particle")
include("eyelib-processor")
```

**Apply:** add `include("eyelib-util")` adjacent to sibling subprojects, before `includeBuild("clientsmoke")`.

---

### `eyelib-util/build.gradle` (config, build/batch)

**Primary analog:** `eyelib-attachment/build.gradle`  
**Secondary analogs:** `eyelib-material/build.gradle`, `eyelib-particle/build.gradle`, `eyelib-processor/build.gradle`

**Plugin/version/base pattern** (`eyelib-attachment/build.gradle` lines 1-15):
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
    archivesName = 'eyelib-attachment'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)
```

**Forge source-set binding pattern** (`eyelib-attachment/build.gradle` lines 30-43):
```groovy
legacyForge {
    version = project.minecraft_version + '-' + project.forge_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    mods {
        eyelibattachment {
            sourceSet(sourceSets.main)
        }
    }
}
```

**External dependency pattern to copy and adapt** (`eyelib-attachment/build.gradle` lines 45-57):
```groovy
dependencies {
    // com.mojang.serialization.* — standalone DataFixerUpper library
    implementation 'com.mojang:datafixerupper:6.0.8'

    implementation 'org.joml:joml:1.10.5'
    implementation 'org.slf4j:slf4j-api:2.0.7'

    compileOnly 'org.jspecify:jspecify:1.0.0'

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

**Do not copy project dependencies** (`eyelib-particle/build.gradle` lines 45-49 and `eyelib-processor/build.gradle` lines 30-33):
```groovy
implementation project(':eyelib-importer')
implementation project(':eyelib-molang')
implementation project(':eyelib-material')
```
```groovy
compileOnly project(':eyelib-importer')
implementation project(':eyelib-molang')
```

**Resource expansion pattern** (`eyelib-attachment/build.gradle` lines 71-85):
```groovy
tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
            minecraft_version_range: minecraft_version_range,
            forge_version_range    : forge_version_range,
            loader_version_range   : loader_version_range,
            mod_version            : mod_version,
            mod_license            : mod_license,
            mod_authors            : mod_authors,
            mod_description        : 'Typed data attachment contracts and stream codec utilities for Eyelib.'
    ]
    inputs.properties(replaceProperties)
    filesMatching('META-INF/mods.toml') {
        expand(replaceProperties)
    }
}
```

**Publishing/test conventions** (`eyelib-attachment/build.gradle` lines 59-69 and 87-96):
```groovy
tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test').configure {
    useJUnitPlatform()
}

java {
    withSourcesJar()
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

---

### `eyelib-util/src/main/resources/META-INF/mods.toml` (config, Forge metadata)

**Analog:** `eyelib-attachment/src/main/resources/META-INF/mods.toml`

**Metadata pattern** (lines 1-24):
```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="eyelibattachment"
version="${mod_version}"
displayName="Eyelib Attachment"
authors="${mod_authors}"
description='''Typed data attachment contracts and stream codec utilities for Eyelib.'''

[[dependencies.eyelibattachment]]
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.eyelibattachment]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

**Apply:** replace `eyelibattachment` with `eyelibutil`; display name should be `Eyelib Util`.

---

### `EyelibUtilMod.java` (bootstrap, event-driven Forge load)

**Analog:** `eyelib-attachment/.../bootstrap/EyelibAttachmentMod.java`

**Bootstrap marker pattern** (lines 1-8):
```java
package io.github.tt432.eyelibattachment.bootstrap;

import net.minecraftforge.fml.common.Mod;

@Mod(EyelibAttachmentMod.MOD_ID)
public class EyelibAttachmentMod {
    public static final String MOD_ID = "eyelibattachment";
}
```

**Apply:** package `io.github.tt432.eyelibutil.bootstrap`, class `EyelibUtilMod`, `MOD_ID = "eyelibutil"`.

---

### `package-info.java` (documentation/config, boundary metadata)

**Analog:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java`

**NullMarked + boundary contract pattern** (lines 1-12):
```java
/**
 * Particle module API and core contract boundary for Eyelib.
 * <p>
 * The root runtime may consume this module, but this package must not depend back on root runtime
 * packages, root managers, root registries, root packets, root capability helpers, or
 * {@code io.github.tt432.eyelib.mc.impl} classes. Minecraft/Forge lifecycle wiring and other
 * platform bindings require explicit adapter documentation before introduction.
 */
@NullMarked
package io.github.tt432.eyelibparticle;

import org.jspecify.annotations.NullMarked;
```

**Apply:** state `:eyelib-util` is a leaf shared utility module; no imports from root/sibling project modules; Minecraft/Forge APIs only where explicitly documented for utility ownership.

---

### `eyelib-util/README.md` and package README (documentation, ownership/boundary)

**Analogs:** `eyelib-attachment/.../README.md`, `eyelib-particle/.../README.md`, `eyelib-processor/.../README.md`

**Module scope/layout pattern** (`eyelib-attachment/.../README.md` lines 1-21):
```markdown
# Eyelib Attachment Module

## Scope
- Path: `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`
- Owns typed attachment storage contracts and attachment-oriented stream codec utilities.
- This is a Minecraft/Forge functional module, not a platform-free core library.

## Layout
- `dataattach/`: attachment type, storage, and container contracts.
- `codec/stream/`: `FriendlyByteBuf`/NBT stream codec helpers used by attachment and network payloads.
- `network/`: attachment-owned packet contracts that do not depend on root capability registries or runtime data types.
- `bootstrap/`: Forge module bootstrap marker.

## Ownership Rule
- Keep attachment storage and attachment protocol helpers here when they are attachment-specific.
```

**Dependency direction pattern** (`eyelib-particle/.../README.md` lines 18-24):
```markdown
## Dependency Direction
- Root runtime may depend on :eyelib-particle, but :eyelib-particle must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
```

**Plain-JVM boundary phrasing to adapt** (`eyelib-processor/.../README.md` lines 13-19):
```markdown
## Boundary Rules
- May depend on plain JVM APIs, codecs, importer-owned schema modules, and engine-side plain-JVM Molang analysis from `:eyelib-molang`.
- Must not depend on root runtime ownership concerns (Forge event posting, NativeImage upload, UI/session lifecycle, RenderSystem hooks).

## Current Consumers
- Root runtime/tooling module (`:`) depends on this module for processing seams.
- Stage-1 extraction keeps this module independent from root runtime packages and free of reverse dependencies back into `:`.
```

**Apply:** for util, explicitly document `io.github.tt432.eyelibutil`, leaf dependency direction, allowed MC/Forge/external libraries, and “no utility implementation migration in Phase 16”.

---

### `UtilModuleIdentityTest.java` (test, batch/static validation)

**Analog:** `eyelib-attachment/src/test/java/io/github/tt432/eyelibattachment/AttachmentModuleIdentityTest.java`

**Imports/assertion pattern** (lines 1-14):
```java
package io.github.tt432.eyelibattachment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

**Docs/static identity assertion pattern** (lines 18-30):
```java
@Test
void moduleDocsDeclareMinecraftFunctionalIdentityAndCodecScope() throws IOException {
    String readme = Files.readString(MODULE_ROOT.resolve("README.md"));
    String codecPackage = Files.readString(MODULE_ROOT.resolve("codec/stream/package-info.java"));

    assertAll(
            () -> assertTrue(readme.contains("Minecraft/Forge functional module")),
            () -> assertTrue(readme.contains("FriendlyByteBuf")),
            () -> assertTrue(readme.contains("codec/stream/")),
            () -> assertTrue(readme.contains("network/")),
            () -> assertTrue(codecPackage.contains("Minecraft/Forge functional module")),
            () -> assertTrue(codecPackage.contains("Plain attachment storage contracts"))
    );
}
```

**No-root-import and allowed path scan pattern** (lines 33-65):
```java
@Test
void attachmentModuleDoesNotDependOnRootRuntimePackages() throws IOException {
    assertAll(javaSources().map(path -> () -> {
        String source = Files.readString(path);
        assertFalse(source.contains("import io.github.tt432.eyelib."), path + " must not import root runtime packages");
    }));
}

private static boolean isAllowedMinecraftFacingPath(String path) {
    return path.startsWith("codec/stream/")
            || path.startsWith("network/")
            || path.startsWith("bootstrap/");
}
```

**Apply:** include checks for `build.gradle` containing `net.neoforged.moddev.legacyforge`, `eyelibutil`, no `project(`, `mods.toml` modId, bootstrap `@Mod(EyelibUtilMod.MOD_ID)`, README namespace, and leaf wording.

---

### `MODULES.md` (documentation, module inventory)

**Analog:** `MODULES.md`

**Summary pattern** (lines 8-16):
```markdown
## Summary
- Eyelib is a multi-project `Gradle + Java 17 + Forge` repository: root runtime module plus attachment subproject `eyelib-attachment`, processor subproject `eyelib-processor`, importer/model subproject `eyelib-importer`, engine Molang subproject `eyelib-molang`, material subproject `eyelib-material`, and particle boundary subproject `eyelib-particle`.
```

**Subproject row pattern** (lines 40-45):
```markdown
| Attachment subproject | Typed data attachment contracts, storage, Minecraft stream codec utilities, and root-independent attachment packet contracts owned by the attachment functional module | `eyelib-attachment/build.gradle`, `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`, `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/README.md`, `eyelib-attachment/src/main/resources/META-INF/mods.toml`, `eyelib-attachment/src/test/` | consumed directly by root; current module is a Forge functional submodule ... |
```

**Update rule pattern** (lines 151-156):
```markdown
## Module Update Rules
1. Before any change, identify every affected module in this file.
2. If a change modifies code or docs inside a listed module, update that module’s summary if its responsibility, paths, or interactions changed.
3. If a change adds a new module or removes an existing module, update this file in the same change.
4. If a change alters module boundaries, also update `docs/architecture/01-module-boundaries.md` and any relevant package README files.
```

**Apply:** add `eyelib-util` to summary and inventory, with main paths `eyelib-util/build.gradle`, `eyelib-util/src/main/java/io/github/tt432/eyelibutil/`, `eyelib-util/src/main/resources/META-INF/mods.toml`, `eyelib-util/src/test/`.

---

### Architecture/index docs (documentation, architecture/index update)

**Analogs:** `docs/architecture/01-module-boundaries.md`, `docs/index/repo-map.md`

**Current area pattern** (`docs/architecture/01-module-boundaries.md` lines 3-18):
```markdown
## Current Major Areas
- `eyelib-processor/src/main/java/io/github/tt432/eyelibprocessor/`: platform-free processing and batching helpers for loader and manager reload planning seams.
- `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`: Bedrock material definitions, GL state management, shader pipeline, and shared pure-data types under `shared/` package.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`: first-class particle module boundary, build/package contract, module-owned particle APIs, ...
- `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/`: attachment functional module for typed attachment storage/contracts and Minecraft stream codec utilities.
```

**Target map pattern** (`docs/architecture/01-module-boundaries.md` lines 53-83):
```markdown
## Current To Target Ownership Map
| Current area | Target owner | Boundary intent |
|---|---|---|
| `:eyelib-processor/**` | `client.processing` | Own platform-free processing and batching helpers ... |
| `:eyelib-particle/**` | `client.particle.module` | Own particle module contracts ... root may consume it, but it must not depend on root runtime packages ... |
| selected `util/` classes | `shared` | Keep only truly cross-cutting helpers here |
```

**Repo-map summary pattern** (`docs/index/repo-map.md` lines 10-14):
```markdown
## What This Repository Is
- Eyelib is a multi-project `Gradle + Java 17 + Forge` rendering library for Minecraft (`:` root runtime + `:eyelib-processor` processing/batching core + `:eyelib-importer` importer/schema Forge functional module + `:eyelib-molang` engine Molang core + `:eyelib-material` material core + `:eyelib-particle` particle module boundary + composite-build `clientsmoke` submodule).
```

**Apply:** add util as a leaf shared utility Forge module; do not claim migrated utility ownership before Phase 17+.

## Shared Patterns

### Forge Functional Module Skeleton
**Source:** `eyelib-attachment/build.gradle` lines 1-43, 71-85; `eyelib-attachment/mods.toml` lines 1-24; `EyelibAttachmentMod.java` lines 1-8.  
**Apply to:** `eyelib-util/build.gradle`, `mods.toml`, bootstrap class.

### Leaf Dependency Boundary
**Source:** `16-CONTEXT.md` lines 16-20; `eyelib-particle/README.md` lines 18-24; `AttachmentModuleIdentityTest.java` lines 33-39.  
**Apply to:** build.gradle, README, package-info, identity test.  
**Rule:** `eyelib-util/build.gradle` must contain no `project(` and source must not import `io.github.tt432.eyelib.` root runtime packages.

### Documentation Update Coupling
**Source:** `AGENTS.md` lines 19-21; `MODULES.md` lines 151-156.  
**Apply to:** `MODULES.md`, `docs/architecture/01-module-boundaries.md`, and likely `docs/index/repo-map.md`.

### Verification Path
**Source:** `AGENTS.md` lines 28-31; `16-VALIDATION.md` lines 20-24, 39-44.  
**Apply to:** plan verification steps only.  
**Rule:** run Gradle only through JetBrains MCP sync/build tools; never shell Gradle.

## No Analog Found

None. Every expected Phase 16 file has a same-role or sibling-module analog. The only adaptation is stricter dependency direction for `:eyelib-util` than `:eyelib-particle` or `:eyelib-processor`.

## Metadata

**Analog search scope:** `settings.gradle`, root `build.gradle`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, sibling modules `eyelib-attachment`, `eyelib-material`, `eyelib-particle`, `eyelib-processor`.  
**Files scanned:** 21  
**Pattern extraction date:** 2026-05-10
