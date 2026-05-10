# Phase 16: Module Scaffold & Build Infrastructure - Research

**Researched:** 2026-05-10  
**Domain:** Gradle multi-project Forge module scaffolding  
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
No explicit `## Decisions` section exists; the phase context records implementation decisions under `### the agent's Discretion`. [VERIFIED: `.planning/phases/16-module-scaffold-build-infrastructure/16-CONTEXT.md`]

### the agent's Discretion
- All implementation choices are at the agent's discretion because this is a pure infrastructure/scaffolding phase.
- Preserve v1.3 locked decisions: module namespace is `io.github.tt432.eyelibutil`; `:eyelib-util` is a leaf project module with no `project(...)` dependencies; MC/Forge dependencies are allowed; no migrated utility code should be moved into the module before Phase 17+.
- Use existing sibling module build patterns from `:eyelib-material`, `:eyelib-particle`, and `:eyelib-processor` where applicable, but keep `:eyelib-util` dependency direction independent from other project modules.
- All Gradle verification must run via JetBrains MCP only, never shell Gradle.

### Deferred Ideas (OUT OF SCOPE)
- Moving root/core utility implementations into `:eyelib-util` is deferred to Phases 17-19.
- Submodule shared code centralization is deferred to Phase 20.
- Final root/core util cleanup is deferred to Phase 21.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| MOD-01 | Maintainer can build `:eyelib-util` as a standalone Forge Gradle subproject with zero `project()` dependencies. [VERIFIED: `.planning/REQUIREMENTS.md`] | Use sibling Forge subproject skeleton, add `settings.gradle` include, keep `dependencies {}` external-only, verify with JetBrains MCP `jetbrain_run_gradle_tasks` for `:eyelib-util:build`. |
| MOD-02 | Module documentation states ownership, dependency direction, package namespace `io.github.tt432.eyelibutil`, and allowed integration layers. [VERIFIED: `.planning/REQUIREMENTS.md`] | Add module README/package README and update `MODULES.md` / architecture docs with leaf dependency direction and namespace. |
</phase_requirements>

## Summary

Phase 16 should create only a buildable Forge Gradle module skeleton for `:eyelib-util`; it must not migrate utility implementations yet. The module should mirror the existing Forge subproject shape used by `:eyelib-attachment`, `:eyelib-material`, and `:eyelib-particle`: `java-library`, Lombok, `net.neoforged.moddev.legacyforge`, Java 17 toolchain, `legacyForge { mods { ... } }`, `processResources` expansion for `mods.toml`, JUnit 5 test setup, and local Maven publishing. [VERIFIED: `eyelib-attachment/build.gradle`; `eyelib-material/build.gradle`; `eyelib-particle/build.gradle`]

The critical difference from most sibling functional modules is dependency direction: `:eyelib-util` must remain a leaf module with **zero** `project(...)` dependencies. Its build file should declare only MC/Forge-provided classpath via `legacyForge` plus explicit external libraries already needed by future utility migration families: DataFixerUpper (`com.mojang:datafixerupper:6.0.8`), JOML (`org.joml:joml:1.10.5`), SLF4J (`org.slf4j:slf4j-api:2.0.7`), JSpecify compile-only, and JUnit for tests. [VERIFIED: `.planning/ROADMAP.md`; `eyelib-attachment/build.gradle`; `eyelib-molang/build.gradle`]

**Primary recommendation:** scaffold `:eyelib-util` as a Forge functional utility module with mod id `eyelibutil`, root package `io.github.tt432.eyelibutil`, a minimal `@Mod` bootstrap marker, external-only dependencies, and documentation updates proving leaf ownership before later migration phases. [VERIFIED: `.planning/ROADMAP.md`; CITED: `https://docs.minecraftforge.net/en/1.20.1/gettingstarted/modfiles/`]

## Project Constraints (from AGENTS.md)

- Read `docs/index/repo-map.md` before exploring code, `MODULES.md` before structural/multi-module changes, and boundary docs before boundary decisions. [VERIFIED: `AGENTS.md`]
- Preserve the multi-project `Gradle + Java 17 + Forge` repository shape and existing manager/loader/visitor/codec patterns. [VERIFIED: `AGENTS.md`]
- Do not touch unrelated uncommitted changes; prefer narrow, stage-scoped edits over broad package churn. [VERIFIED: `AGENTS.md`]
- Document ownership and dependency rules before moving code across subsystem boundaries. [VERIFIED: `AGENTS.md`]
- Before each change, identify affected modules in `MODULES.md`; when a module is added, update `MODULES.md` and impacted index/architecture docs in the same change. [VERIFIED: `AGENTS.md`]
- IntelliJ IDEA is the sole IDE; VS Code/Eclipse/JDTLS artifacts must not be created or committed. [VERIFIED: `AGENTS.md`]
- All Gradle commands must use JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`); never run Gradle from shell. [VERIFIED: `AGENTS.md`]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Gradle subproject registration | Build system / Gradle settings | IDE Gradle sync | `settings.gradle` is the central include list and Gradle includes map project paths to physical directories. [VERIFIED: `settings.gradle`; CITED: Gradle multi-project docs] |
| Forge module identity | Module resource metadata + bootstrap marker | Build script `legacyForge.mods` binding | Forge requires `mods.toml` under `src/main/resources/META-INF/` and `javafml` entrypoints use `@Mod` whose value matches a `mods.toml` mod id. [CITED: Forge docs] |
| Utility ownership documentation | Repository documentation | Package-local README | `MODULES.md` is canonical module inventory; sibling modules use package READMEs to record scope and dependency direction. [VERIFIED: `MODULES.md`; sibling READMEs] |
| Dependency direction enforcement | Build script | Module identity tests | `:eyelib-util` must be leaf: no `project(...)` dependencies in its build file; later phases may add dependencies from root/siblings to util, not reverse. [VERIFIED: `.planning/ROADMAP.md`] |
| Solo build verification | JetBrains MCP Gradle execution | IDE project sync | Shell Gradle is prohibited; verification must use JetBrains MCP, and current IDE Gradle task listing reported no tasks until sync. [VERIFIED: AGENTS.md; `jetbrain_list_gradle_tasks` result 2026-05-10] |

## Standard Stack

### Core

| Library / Tool | Version | Purpose | Why Standard |
|----------------|---------|---------|--------------|
| Gradle multi-project include | Existing build settings | Registers `:eyelib-util` as a subproject via `include("eyelib-util")`. | Current repository already uses `settings.gradle` includes for all sibling modules; Gradle docs define `settings.gradle` as the subproject declaration point. [VERIFIED: `settings.gradle`; CITED: Gradle multi-project docs] |
| `net.neoforged.moddev.legacyforge` | `2.0.91` | Forge 1.20.1 development plugin used by root and Forge subprojects. | Existing Forge modules use this exact plugin/version; cloning it avoids build-system innovation. [VERIFIED: `build.gradle`; `eyelib-attachment/build.gradle`] |
| Java toolchain | 17 | Compile target for Minecraft 1.20.1/Forge modules. | Root and all sibling modules set `java.toolchain.languageVersion = JavaLanguageVersion.of(17)`. [VERIFIED: root/sibling `build.gradle`] |
| Forge | `47.1.3` for Minecraft `1.20.1` | Provides Forge/Minecraft compile and mod metadata context. | Centralized in `gradle.properties`; sibling Forge modules derive `legacyForge.version` from these properties. [VERIFIED: `gradle.properties`; sibling `build.gradle`] |
| Forge `mods.toml` | `src/main/resources/META-INF/mods.toml` | Declares `eyelibutil` module metadata and Forge/Minecraft dependencies. | Forge docs require `mods.toml` under `META-INF` for the source set; sibling modules use the same path. [CITED: Forge docs; VERIFIED: sibling `mods.toml`] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `com.mojang:datafixerupper` | `6.0.8` | `com.mojang.serialization.Codec` support needed by future codec/resource utility migrations. | Add now because roadmap allows MC/Forge/external library dependencies and Phase 19 requires codec infrastructure to resolve inside `eyelib-util`. [VERIFIED: `eyelib-attachment/build.gradle`; `.planning/ROADMAP.md`] |
| `org.joml:joml` | `1.10.5` | Vector/math support for future MC-dependent utility code. | Existing attachment/molang modules use JOML; use the repository-pinned version rather than upgrading during scaffold. [VERIFIED: `eyelib-attachment/build.gradle`; `eyelib-molang/build.gradle`] |
| `org.slf4j:slf4j-api` | `2.0.7` | Logging API for utility code that needs logger types. | Existing sibling modules declare `2.0.7`; use the repository pattern to avoid unrelated dependency churn. [VERIFIED: `eyelib-attachment/build.gradle`; `eyelib-material/build.gradle`] |
| `org.jspecify:jspecify` | `1.0.0` | Compile-only nullness annotations. | Existing root and sibling modules declare compile-only JSpecify. [VERIFIED: root/sibling `build.gradle`] |
| JUnit Jupiter | BOM `5.10.2` | Future identity/boundary tests. | Existing subprojects use JUnit 5 with `useJUnitPlatform()`. [VERIFIED: sibling `build.gradle`] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Forge functional module skeleton | Plain Java `java-library` module like `:eyelib-processor` | Rejected for Phase 16 because v1.3 explicitly allows MC/Forge dependencies and requires `mods.toml`. [VERIFIED: `.planning/ROADMAP.md`; `eyelib-processor/build.gradle`] |
| Minimal resources-only module with no `@Mod` class | `mods.toml` only | Rejected for a `javafml` module skeleton: Forge docs state `javafml` entrypoint is a public class annotated with `@Mod` matching `mods.toml`; material/attachment/importer already follow this marker pattern. [CITED: Forge docs; VERIFIED: bootstrap marker classes] |
| Add `project(':eyelib-molang')` or `project(':eyelib-importer')` early | Project dependencies for future codec/Molang code | Rejected: Phase 16 and v1.3 lock `:eyelib-util` as a leaf with zero `project(...)` dependencies. [VERIFIED: `16-CONTEXT.md`; `.planning/ROADMAP.md`] |

**Installation:** no package-manager install is required; use existing Gradle wrapper/IDE model only. [VERIFIED: repository build files]

**Version verification:** package versions above are repository-pinned from existing build files; Maven Central reported newer JOML (`1.10.8`) and an alpha SLF4J (`2.1.0-alpha1`), but the scaffold should **not** upgrade them during infrastructure work. [VERIFIED: Maven Central API fetch 2026-05-10; VERIFIED: sibling build files]

## Exact Skeleton Files

| Path | Required Content | Source Pattern |
|------|------------------|----------------|
| `settings.gradle` | Add `include("eyelib-util")` near sibling includes. | Existing include list lines 16-21. [VERIFIED: `settings.gradle`] |
| `eyelib-util/build.gradle` | Forge subproject build script with external-only dependencies and no `project(...)` calls. | Clone `eyelib-attachment`/`eyelib-material` shape, remove project deps. [VERIFIED: sibling build files] |
| `eyelib-util/src/main/resources/META-INF/mods.toml` | `modLoader="javafml"`, `modId="eyelibutil"`, Forge/Minecraft dependencies, display name `Eyelib Util`. | Sibling Forge module `mods.toml` files. [VERIFIED: sibling `mods.toml`; CITED: Forge docs] |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/bootstrap/EyelibUtilMod.java` | Minimal `@Mod(EyelibUtilMod.MOD_ID)` class with `MOD_ID = "eyelibutil"`. | Material/attachment/importer bootstrap markers. [VERIFIED: bootstrap classes; CITED: Forge docs] |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/package-info.java` | Package-level statement that root package owns shared utilities and must not import project-internal modules. | Existing package-info usage in sibling modules. [VERIFIED: sibling package-info files via glob] |
| `eyelib-util/README.md` | Module ownership, leaf dependency direction, namespace, allowed integration layers, and no-migration-yet scope. | Phase 16 success criteria explicitly names `eyelib-util/README.md`. [VERIFIED: `.planning/ROADMAP.md`] |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md` | Package-local navigation/ownership README matching sibling package README pattern. | Existing package READMEs under attachment/particle/processor. [VERIFIED: sibling READMEs] |
| `MODULES.md` | Add an `eyelib-util` module row and update summary to include the new subproject. | Module update rule requires adding new module rows. [VERIFIED: `MODULES.md`] |
| `docs/architecture/01-module-boundaries.md` | Add `:eyelib-util/**` to current areas and target ownership map as a leaf shared utility module. | AGENTS/MODULES require architecture docs when adding module boundaries. [VERIFIED: `AGENTS.md`; `MODULES.md`] |
| Optional Phase 16 identity test | `eyelib-util/src/test/java/io/github/tt432/eyelibutil/UtilModuleIdentityTest.java` can assert docs/build/mod id/no root imports/no project deps. | Existing importer/attachment identity tests verify build/docs/bootstrap constraints. [VERIFIED: identity tests] |

## Recommended `build.gradle` Pattern

```groovy
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base { archivesName = 'eyelib-util' }
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

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
        eyelibutil { sourceSet(sourceSets.main) }
    }
}

dependencies {
    implementation 'com.mojang:datafixerupper:6.0.8'
    implementation 'org.joml:joml:1.10.5'
    implementation 'org.slf4j:slf4j-api:2.0.7'
    compileOnly 'org.jspecify:jspecify:1.0.0'

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
tasks.named('test').configure { useJUnitPlatform() }
java { withSourcesJar() }

tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
            minecraft_version_range: minecraft_version_range,
            forge_version_range    : forge_version_range,
            loader_version_range   : loader_version_range,
            mod_version            : mod_version,
            mod_license            : mod_license,
            mod_authors            : mod_authors,
            mod_description        : 'Shared utility module for Eyelib leaf utilities.'
    ]
    inputs.properties(replaceProperties)
    filesMatching('META-INF/mods.toml') { expand(replaceProperties) }
}

publishing {
    publications { mavenJava(MavenPublication) { from components.java } }
    repositories { mavenLocal() }
}
```

This pattern intentionally contains no `project(...)` calls. [VERIFIED: `.planning/ROADMAP.md`; sibling build files]

## Architecture Patterns

### System Architecture Diagram

```text
Developer / IDE
   |
   v
settings.gradle include("eyelib-util")
   |
   v
Gradle project :eyelib-util
   |-- build.gradle: legacyForge + Java 17 + external-only deps
   |-- mods.toml: Forge metadata, unique modId eyelibutil
   |-- bootstrap/EyelibUtilMod.java: javafml @Mod marker
   |-- README.md + package README: ownership/dependency contract
   v
JetBrains MCP Gradle verification
   |-- :eyelib-util:build must exit 0
   |-- build.gradle text audit: zero project(...) dependencies
   v
Phase 17+ utility migrations may consume this module
```

### Recommended Project Structure

```text
eyelib-util/
├── build.gradle
├── README.md
└── src/
    ├── main/
    │   ├── java/io/github/tt432/eyelibutil/
    │   │   ├── README.md
    │   │   ├── package-info.java
    │   │   └── bootstrap/EyelibUtilMod.java
    │   └── resources/META-INF/mods.toml
    └── test/java/io/github/tt432/eyelibutil/
        └── UtilModuleIdentityTest.java   # optional but recommended for MOD-01/MOD-02 guardrails
```

### Pattern 1: Forge Metadata + Bootstrap Marker
**What:** Use `javafml` `mods.toml` plus a public `@Mod` class with matching mod id. [CITED: Forge docs]  
**When to use:** For Forge functional modules that should load as mods, including `:eyelib-util`. [VERIFIED: Phase 16 context]  
**Example:**

```java
// Source: Forge docs + existing material/attachment marker pattern
package io.github.tt432.eyelibutil.bootstrap;

import net.minecraftforge.fml.common.Mod;

@Mod(EyelibUtilMod.MOD_ID)
public class EyelibUtilMod {
    public static final String MOD_ID = "eyelibutil";
}
```

### Pattern 2: Leaf Module Build Contract
**What:** External dependencies only; no `implementation project(...)`, `api project(...)`, `compileOnly project(...)`, or `testImplementation project(...)`. [VERIFIED: Phase 16 context]  
**When to use:** During Phase 16 and all later phases unless a human changes the locked decision. [VERIFIED: `16-CONTEXT.md`]  
**Example validation:** scan `eyelib-util/build.gradle` for `project(` and fail the phase if found. [VERIFIED: `.planning/ROADMAP.md`]

### Anti-Patterns to Avoid
- **Adding utility implementations in Phase 16:** migration is explicitly deferred to Phases 17-19. [VERIFIED: `16-CONTEXT.md`]
- **Depending on root or sibling project modules from `:eyelib-util`:** violates the leaf-module locked decision. [VERIFIED: `16-CONTEXT.md`; `.planning/ROADMAP.md`]
- **Using root package `io.github.tt432.eyelib.util`:** creates split-package confusion; use `io.github.tt432.eyelibutil`. [VERIFIED: `16-CONTEXT.md`; `MODULES.md`]
- **Running `./gradlew` in shell for verification:** prohibited by global/project rules. [VERIFIED: `AGENTS.md`]
- **Creating VS Code/Eclipse/JDTLS artifacts during scaffold:** prohibited by tooling restrictions. [VERIFIED: `AGENTS.md`]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Forge module setup | Custom classpath/source-set wiring | Existing `legacyForge` sibling module pattern | Proven in current repository and matches ModDevGradle/Forge workflow. [VERIFIED: sibling build files] |
| Mod metadata generation | Custom resource copy task | `processResources` `filesMatching('META-INF/mods.toml') { expand(...) }` | Existing modules already expand version/license/authors consistently. [VERIFIED: sibling build files] |
| Dependency boundary enforcement | Manual memory of allowed imports only | Build-file text audit + optional identity test | Existing identity tests enforce module boundary docs/import rules. [VERIFIED: identity tests] |
| Gradle verification | Shell Gradle invocation | JetBrains MCP `jetbrain_run_gradle_tasks` | Shell Gradle is prohibited by project rules. [VERIFIED: `AGENTS.md`] |

**Key insight:** this phase is infrastructure cloning, not build-system design; reuse existing subproject patterns and make only the dependency-direction contract stricter. [VERIFIED: `.planning/REQUIREMENTS.md`; sibling build files]

## Common Pitfalls

### Pitfall 1: Forgetting `settings.gradle`
**What goes wrong:** `eyelib-util/` exists but Gradle does not know project `:eyelib-util`.  
**Why it happens:** Files are added without updating the central include list.  
**How to avoid:** Add `include("eyelib-util")` in `settings.gradle`. [VERIFIED: `settings.gradle`; CITED: Gradle multi-project docs]  
**Warning signs:** JetBrains MCP cannot resolve or run `:eyelib-util:build` after sync.

### Pitfall 2: Mod id mismatch
**What goes wrong:** Forge metadata and bootstrap class disagree, causing load/discovery failure.  
**Why it happens:** `mods.toml`, `legacyForge.mods`, and `@Mod` marker use different identifiers.  
**How to avoid:** Use `eyelibutil` exactly in `legacyForge.mods { eyelibutil { ... } }`, `mods.toml`, and `EyelibUtilMod.MOD_ID`. [CITED: Forge docs; VERIFIED: sibling module pattern]  
**Warning signs:** Build may pass but Forge mod discovery/runtime load fails.

### Pitfall 3: Accidental project dependency
**What goes wrong:** `:eyelib-util` starts depending on root/importer/molang/material before migrations, creating dependency cycles or blocking later centralization.  
**Why it happens:** Copying `eyelib-particle` or `eyelib-importer` dependencies verbatim.  
**How to avoid:** Clone structural build blocks but replace `dependencies {}` with external-only dependencies. [VERIFIED: `eyelib-particle/build.gradle`; `16-CONTEXT.md`]  
**Warning signs:** `build.gradle` contains `project(`.

### Pitfall 4: Treating `eyelib-util` as pure Java
**What goes wrong:** Future MC-dependent utility migrations (`ResourceLocations`, `Shapes`, codec infrastructure) cannot compile in the new module.  
**Why it happens:** Over-applying `:eyelib-processor` plain-JVM pattern.  
**How to avoid:** Use Forge functional module pattern with `legacyForge`; MC/Forge dependencies are allowed. [VERIFIED: `16-CONTEXT.md`; `.planning/REQUIREMENTS.md`]

## Code Examples

### `mods.toml` for `eyelibutil`

```toml
# Source: sibling Forge mods.toml files + Forge docs
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="eyelibutil"
version="${mod_version}"
displayName="Eyelib Util"
authors="${mod_authors}"
description='''Shared utility module for Eyelib leaf utilities.'''

[[dependencies.eyelibutil]]
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.eyelibutil]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

### Optional identity test checks

```java
// Source: adapted from AttachmentModuleIdentityTest and ImporterModuleIdentityTest patterns
assertTrue(build.contains("id 'net.neoforged.moddev.legacyforge'"));
assertTrue(build.contains("eyelibutil"));
assertFalse(build.contains("project("));
assertTrue(modsToml.contains("modId=\"eyelibutil\""));
assertTrue(bootstrap.contains("@Mod(EyelibUtilMod.MOD_ID)"));
assertTrue(readme.contains("io.github.tt432.eyelibutil"));
assertTrue(readme.contains("leaf"));
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Root `src/main/java/io/github/tt432/eyelib/util/` as mixed utility bucket | New `:eyelib-util` Forge utility module with package `io.github.tt432.eyelibutil` | v1.3 Phase 16 begins after Phase 15 audit | Planning must scaffold module before moving code. [VERIFIED: ROADMAP/STATE] |
| Single-consumer helper cleanup by deletion or util migration | Route single-consumer helpers to functional owners | Phase 15 | Phase 16 must not reintroduce those helpers into util. [VERIFIED: utility routing manifest] |
| Project modules with functional dependencies (`:eyelib-particle` -> importer/molang/material) | `:eyelib-util` as project-dependency leaf | v1.3 locked decision | Build file dependency block must be stricter than particle/importer. [VERIFIED: `16-CONTEXT.md`] |

**Deprecated/outdated:** Treating `src/main/java/io/github/tt432/eyelib/util/client/` as a destination for new utility code is outdated; AGENTS forbids adding to ambiguous catch-all util/client areas without documented responsibility. [VERIFIED: `AGENTS.md`; `docs/architecture/01-module-boundaries.md`]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The minimal identity test should be included in Phase 16 rather than deferred when it has no project-internal dependencies and only validates module/mod/package identity. [RESOLVED] | Exact Skeleton Files / Validation Architecture | This is allowed and preferred because it strengthens MOD-01/MOD-02 automated guardrails without migrating utility implementation code or adding project dependencies. |

## Open Questions (RESOLVED)

1. **(RESOLVED) Should `eyelib-util` also be wired into root `build.gradle` as a dependency in Phase 16?**
   - What we know: Phase 16 success criteria only requires solo `:eyelib-util` build and zero `project(...)` dependencies inside util. [VERIFIED: `.planning/ROADMAP.md`]
   - Resolution: Root `build.gradle` must **not** add a dependency on `:eyelib-util` in Phase 16. Root/sibling consumption starts in Phase 17+ when code actually migrates into the module; Phase 16 remains skeleton-only.
2. **(RESOLVED) Is a minimal identity test allowed in Phase 16?**
   - What we know: The phase must not migrate utility implementation code and `eyelib-util/build.gradle` must keep zero `project(...)` dependencies. [VERIFIED: `16-CONTEXT.md`; `.planning/ROADMAP.md`]
   - Resolution: A minimal `UtilModuleIdentityTest` is allowed and preferred if it has no project-internal dependencies and only validates module/mod/package identity: build script plugin/mod id/no `project(`, `mods.toml` mod id, bootstrap `@Mod` marker, and `io.github.tt432.eyelibutil` package namespace. It must not reference or require root utility implementation classes.
3. **(RESOLVED) Should docs update `docs/index/repo-map.md` now or wait until Phase 21?**
   - What we know: AGENTS and MODULES say adding a module requires impacted index/architecture docs in the same change. [VERIFIED: `AGENTS.md`; `MODULES.md`]
   - Resolution: Update `MODULES.md`, `docs/architecture/01-module-boundaries.md`, and `docs/index/repo-map.md` in Phase 16 because the new module changes the repository inventory and navigation map. Do not update `docs/architecture/02-side-boundaries.md` unless execution finds a concrete side-boundary wording mismatch.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JetBrains MCP Gradle tools | Required verification path | ✓ | Tools available; Gradle tasks currently not listed before sync | Run JetBrains MCP Gradle sync first; do not use shell Gradle. [VERIFIED: `jetbrain_list_linked_gradle_projects`; `jetbrain_list_gradle_tasks`] |
| Java runtime | Gradle/compile environment | ✓ | Temurin OpenJDK 21.0.10 installed; build itself targets Java 17 toolchain | Gradle toolchain may provision/use Java 17 as configured. [VERIFIED: `java -version`; build files] |
| Shell Gradle | Verification | ✗ by policy | Prohibited | JetBrains MCP only. [VERIFIED: `AGENTS.md`] |

**Missing dependencies with no fallback:** none identified, but IDE Gradle tasks were unavailable until sync; planner should include a JetBrains MCP sync/check step before solo build. [VERIFIED: tool result 2026-05-10]

**Missing dependencies with fallback:** Gradle task listing unavailable before sync → use `jetbrain_sync_gradle_projects`, then `jetbrain_run_gradle_tasks`. [VERIFIED: JetBrains MCP tool availability]

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter via BOM `5.10.2`, `useJUnitPlatform()` [VERIFIED: sibling build files] |
| Config file | Per-subproject `build.gradle`; no separate JUnit config found for these identity tests. [VERIFIED: sibling build files] |
| Quick run command | JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames: [":eyelib-util:test"]` after scaffold and sync. |
| Full suite command | JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames: [":eyelib-util:build"]`; optionally `jetbrain_build_project` full rebuild after settings sync. |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| MOD-01 | `:eyelib-util` solo build succeeds and build file contains no `project(...)` dependencies. | Gradle build + identity test | JetBrains MCP `:eyelib-util:build` | ❌ Wave 0 |
| MOD-02 | README/docs state ownership, leaf direction, namespace, and allowed integration layers. | Unit identity/doc test | JetBrains MCP `:eyelib-util:test` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** JetBrains MCP `:eyelib-util:test` once test exists; otherwise `:eyelib-util:compileJava`/`:eyelib-util:processResources` through `jetbrain_run_gradle_tasks`.
- **Per wave merge:** JetBrains MCP `:eyelib-util:build`.
- **Phase gate:** JetBrains MCP `:eyelib-util:build` exit code 0 and text audit confirms zero `project(...)` in `eyelib-util/build.gradle`.

### Wave 0 Gaps
- [ ] `eyelib-util/src/test/java/io/github/tt432/eyelibutil/UtilModuleIdentityTest.java` — covers MOD-01/MOD-02 guardrails with no project-internal dependencies and only module/mod/package identity assertions. [RESOLVED]
- [ ] `eyelib-util/build.gradle` — needed before any test command can run.
- [ ] `eyelib-util/src/main/resources/META-INF/mods.toml` — needed for `processResources`/Forge metadata.
- [ ] JetBrains MCP Gradle sync — current task listing returned “No Gradle tasks found. Sync the Gradle project in IDE first.” [VERIFIED: tool result]

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | no | No auth surface in module scaffold. [VERIFIED: Phase scope] |
| V3 Session Management | no | No session surface in module scaffold. [VERIFIED: Phase scope] |
| V4 Access Control | no | No runtime authorization surface in module scaffold. [VERIFIED: Phase scope] |
| V5 Input Validation | yes | Validate metadata identifiers through Forge mod id regex and tests/text audit. [CITED: Forge docs] |
| V6 Cryptography | no | No crypto surface in module scaffold. [VERIFIED: Phase scope] |

### Known Threat Patterns for Gradle/Forge Scaffold

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Dependency confusion / unplanned project coupling | Tampering | Use explicit external coordinates only and assert no `project(...)` in `eyelib-util/build.gradle`. [VERIFIED: Phase requirements] |
| Mod id collision | Spoofing | Use unique `eyelibutil`; verify against roadmap-listed existing ids: `eyelib`, `eyelibattachment`, `eyelibimporter`, `eyelibmaterial`, `eyelibmolang`, `eyelibparticle`, `eyelibprocessor`. [VERIFIED: `.planning/ROADMAP.md`] |
| Unauthorized tool artifacts | Tampering | Do not create VS Code/Eclipse/JDTLS artifacts; use IntelliJ/JetBrains MCP only. [VERIFIED: `AGENTS.md`] |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/16-module-scaffold-build-infrastructure/16-CONTEXT.md` — phase boundaries, locked/discretion decisions, deferred scope.
- `.planning/ROADMAP.md` — Phase 16 goal, MOD-01/MOD-02 mapping, success criteria, mod id list.
- `.planning/REQUIREMENTS.md` — module infrastructure requirements and out-of-scope constraints.
- `.planning/STATE.md` — v1.3 current focus and accumulated decisions.
- `docs/architecture/migration/utility-routing-manifest.md` — Phase 15 routing contract and no-migration boundary.
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md` — repository constraints and module-boundary rules.
- Sibling module build/resource/doc files: `eyelib-attachment`, `eyelib-material`, `eyelib-particle`, `eyelib-processor`, `eyelib-importer`, `eyelib-molang`.
- Forge 1.20.1 docs: `https://docs.minecraftforge.net/en/1.20.1/gettingstarted/modfiles/` — `mods.toml`, mod id regex, `javafml` `@Mod` entrypoint.
- Gradle docs: `https://docs.gradle.org/current/userguide/multi_project_builds.html` — `settings.gradle` includes and multi-project structure.

### Secondary (MEDIUM confidence)
- NeoForged ModDevGradle GitHub README — confirms ModDevGradle supports IntelliJ runs, Forge/NeoForge mod development, JUnit tests, and mod source-set binding; project uses legacyForge variant already. [CITED: `https://github.com/neoforged/ModDevGradle`]
- Maven Central API fetches for JOML/SLF4J latest versions — used only to avoid accidental upgrades, not to change pinned versions.

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — based on existing sibling modules and locked roadmap/context.
- Architecture: HIGH — module-boundary requirements are explicit in ROADMAP/CONTEXT/MODULES.
- Pitfalls: HIGH — derived from observed sibling differences and explicit constraints.

**Research date:** 2026-05-10  
**Valid until:** 2026-06-09 for repository-local scaffold guidance; re-check if Gradle plugin or Forge version changes.
