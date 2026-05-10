# Phase 17: Tier-1 Category Migration - Research

**Researched:** 2026-05-10  
**Domain:** Gradle multi-project Java/Forge utility-module migration  
**Confidence:** HIGH for local routing/import/build guidance; MEDIUM for runtime native-library behavior because `SharedLibraryLoader` has no active production consumer evidence in this session.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- D-01: `SharedLibraryLoader` target package is `io.github.tt432.eyelibutil.loader`. [VERIFIED: `.planning/phases/17-tier-1-category-migration/17-CONTEXT.md`]
- D-02: Migrate FastUtil-dependent `Lists.java` fully; prove classpath with JetBrains MCP `:eyelib-util:build`, no design degradation. [VERIFIED: `.planning/phases/17-tier-1-category-migration/17-CONTEXT.md`]
- D-03: Delete `ListHelper` after callers are rewired to `io.github.tt432.eyelibutil.collection.ListAccessors`. [VERIFIED: `.planning/phases/17-tier-1-category-migration/17-CONTEXT.md`]

### the agent's Discretion
- All implementation choices are at the agent's discretion within the ROADMAP success criteria and Phase 15 routing manifest.
- Migrate only Phase 17-owned files: time, color, loader/misc, math, search, and collection helpers named in ROADMAP. Do not migrate resource/texture utilities, codec infrastructure, or submodule-centralized helpers before later phases.
- Preserve package namespace `io.github.tt432.eyelibutil` and keep `:eyelib-util` as a leaf module with no project-internal dependencies.
- Use IDE-aware moves/refactors where possible; validate all Gradle/build operations through JetBrains MCP only.

### Deferred Ideas (OUT OF SCOPE)
- Resource/texture migration and `ResourceLocations.mod()` handling are Phase 18-owned.
- Codec infrastructure and `EitherHelper` deletion are Phase 19-owned.
- Submodule shared-code centralization is Phase 20-owned.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| MIGR-01 | Zero-dependency utility categories (time, color, loader, math, search — 11 files) are migrated into `:eyelib-util` with updated root import sites. [VERIFIED: `.planning/REQUIREMENTS.md`] | Target package table, dependency audit, consumer import matrix, and JetBrains-only verification commands below. |
| MIGR-02 | Collection utilities (`Blackboard`, `Lists`, `Collectors`, `EntryStreams`) are migrated into `:eyelib-util`. [VERIFIED: `.planning/REQUIREMENTS.md`] | Collection target package table, `ListHelper` deletion path, and consumer/test rewiring guidance below. |
</phase_requirements>

## Summary

Phase 17 should be a behavior-preserving source ownership transfer: move only the 11 ROADMAP Tier-1 utility files and the 4 collection utility files into `:eyelib-util`, add root consumption of `:eyelib-util`, update imports, move/adjust relevant tests, then delete old root/core source copies plus `ListHelper`. [VERIFIED: `.planning/ROADMAP.md`, `docs/architecture/migration/utility-routing-manifest.md`, IDE indexed searches]

Use category subpackages under the non-split namespace `io.github.tt432.eyelibutil`: `time`, `color`, `loader`, `math`, `search`, and `collection`. [VERIFIED: `17-CONTEXT.md` namespace decision; inferred package grouping from ROADMAP category names and current source categories] This avoids recreating root `io.github.tt432.eyelib.util` split packages and keeps future Phase 18/19/20 migrations cleanly separated. [VERIFIED: `eyelib-util/README.md`, `docs/architecture/01-module-boundaries.md`]

**Primary recommendation:** migrate with IDE-aware move/refactor operations per category, add `api/modImplementation/jarJar project(':eyelib-util')` to root dependencies before import rewiring, replace `ListHelper` calls with `ListAccessors.first/last`, then verify with JetBrains MCP full build and IDE residual-import scans. [VERIFIED: root `build.gradle` dependency patterns, `AGENTS.md` Gradle restriction, IDE search evidence]

## Project Constraints (from AGENTS.md)

- Read `docs/index/repo-map.md`, `MODULES.md`, relevant architecture docs, package READMEs, then code before structural/multi-module work. [VERIFIED: `AGENTS.md`]
- This is a multi-project Gradle + Java 17 + Forge codebase; preserve the existing module split unless a human requests collapse. [VERIFIED: `AGENTS.md`, `MODULES.md`]
- Prefer narrow, stage-scoped edits; do not touch unrelated uncommitted changes. [VERIFIED: `AGENTS.md`]
- Before each change, identify affected modules in `MODULES.md`; update `MODULES.md` if responsibility, paths, or interactions change. [VERIFIED: `AGENTS.md`, `MODULES.md`]
- Do not add new code to ambiguous catch-all areas such as `src/main/java/io/github/tt432/eyelib/util/client/` without documenting destination responsibility. [VERIFIED: `AGENTS.md`]
- IntelliJ IDEA is the sole IDE; JDTLS/Eclipse tooling and VS Code/Eclipse artifacts are prohibited. [VERIFIED: `AGENTS.md`]
- Never run Gradle in shell; all Gradle verification must use JetBrains MCP (`jetbrain_build_project` or `jetbrain_run_gradle_tasks`). [VERIFIED: `AGENTS.md`]
- Structure/code changes require the stage-specific Gradle command to exit `0`; null-safety changes require the NullAway gate. [VERIFIED: `AGENTS.md`]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Utility source ownership transfer | Build/module boundary (`:eyelib-util`) | Root runtime consumers | `:eyelib-util` owns shared utility code under `io.github.tt432.eyelibutil`; root/sibling modules may consume it only via explicit Gradle edges. [VERIFIED: `eyelib-util/README.md`, `docs/architecture/01-module-boundaries.md`] |
| Consumer import rewiring | Java source layer | Build/module boundary | Root code must import new `eyelibutil` classes once old root/core source files are deleted. [VERIFIED: Phase 17 ROADMAP success criteria] |
| `ListHelper` deletion | Java source layer | Test layer | Manifest states `ListHelper` is a shim over `ListAccessors` and should be deleted after its `BrBoneKeyFrame` caller migrates. [VERIFIED: `utility-routing-manifest.md`, IDE `ListHelper` search] |
| Verification | JetBrains Gradle/tooling | IDE indexed search | Project rules forbid shell Gradle and require JetBrains MCP for Gradle gates. [VERIFIED: `AGENTS.md`] |

## Standard Stack

### Core
| Library / Module | Version | Purpose | Why Standard |
|------------------|---------|---------|--------------|
| Java toolchain | 17 | Compile root and subprojects. | Root and `eyelib-util` set `java.toolchain.languageVersion = JavaLanguageVersion.of(17)`. [VERIFIED: `build.gradle`, `eyelib-util/build.gradle`] |
| Forge Gradle plugin | `net.neoforged.moddev.legacyforge` 2.0.91 | Forge module classpath and mod source-set wiring. | Root and `eyelib-util` both apply this plugin. [VERIFIED: `build.gradle`, `eyelib-util/build.gradle`] |
| `:eyelib-util` | local Gradle project | Canonical shared utility owner for this phase. | Phase 16 created it as leaf module with `eyelibutil` mod id and `io.github.tt432.eyelibutil` namespace. [VERIFIED: `16-VERIFICATION.md`, `eyelib-util/README.md`] |
| Lombok | FreeFair plugin 8.6 | Supports existing annotations such as `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@Getter`, `@Slf4j`. | Existing Phase 17 targets use Lombok annotations and `eyelib-util` already applies Lombok. [VERIFIED: target source reads, `eyelib-util/build.gradle`] |
| JSpecify | 1.0.0 compile-only | Supports `@Nullable` in `Blackboard` and `SharedLibraryLoader`. | `eyelib-util` already declares `compileOnly 'org.jspecify:jspecify:1.0.0'`. [VERIFIED: target source reads, `eyelib-util/build.gradle`] |
| JOML | 1.10.5 | Supports `Vector2f`/`Vector3f` in math helpers. | `Curves` and `Shapes` import JOML; `eyelib-util` already declares JOML. [VERIFIED: target source reads, `eyelib-util/build.gradle`] |
| SLF4J API | 2.0.7 | Supports `@Slf4j` generated logger type in `SharedLibraryLoader`. | `SharedLibraryLoader` uses `@Slf4j`; `eyelib-util` already declares SLF4J. [VERIFIED: source read, `eyelib-util/build.gradle`] |

### Supporting
| Library / Module | Version | Purpose | When to Use |
|------------------|---------|---------|-------------|
| Minecraft/Forge classpath | `minecraft_version` + `forge_version` project properties | Allows `net.minecraft.util.RandomSource` in `Shapes`. | Keep because `:eyelib-util` is an allowed Forge utility module, not a pure-Java-only module. [VERIFIED: `eyelib-util/build.gradle`, `eyelib-util/README.md`, `Shapes.java`] |
| FastUtil | supplied by Minecraft/Forge classpath | Supports `it.unimi.dsi.fastutil.ints.Int2ObjectFunction` in `Lists`. | No explicit dependency exists in `eyelib-util`; compile is expected through Forge/Minecraft dependency graph and must be verified by `:eyelib-util:build`. [VERIFIED: `Lists.java`, `eyelib-util/build.gradle`; confidence MEDIUM until build] |
| JUnit Jupiter | BOM 5.10.2 | Module/root unit tests after move. | `eyelib-util` and root already declare JUnit Jupiter test dependencies. [VERIFIED: `build.gradle`, `eyelib-util/build.gradle`] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Category packages under `io.github.tt432.eyelibutil` | Flat `io.github.tt432.eyelibutil.*` package | Flat package is closer to ROADMAP wording but loses category ownership clarity; category packages align with phase categories and avoid mixing future Phase 18/19 code. [VERIFIED: ROADMAP category wording; recommendation inferred] |
| `ListHelper` compatibility wrapper | Keep `ListHelper` in `eyelib-util` | ROADMAP and manifest require deletion after caller migration; keeping it fails success criterion 5. [VERIFIED: `.planning/ROADMAP.md`, `utility-routing-manifest.md`] |
| Shell `./gradlew build` | JetBrains MCP Gradle task | Shell Gradle is forbidden by project/global rules. [VERIFIED: `AGENTS.md`] |

**Installation:** No npm or external install step is required. Use existing Gradle dependencies in `eyelib-util/build.gradle`, and add only the root project dependency edge to `build.gradle`. [VERIFIED: local Gradle files]

**Version verification:** Package versions above were verified from local Gradle build files, not external registries. [VERIFIED: `build.gradle`, `eyelib-util/build.gradle`]

## Exact Phase 17 File Moves

### Zero-dependency / Tier-1 utility files
| Source file | Current package | Target file | Target package | Notes |
|-------------|-----------------|-------------|----------------|-------|
| `src/main/java/io/github/tt432/eyelib/util/SimpleTimer.java` | `io.github.tt432.eyelib.util` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/SimpleTimer.java` | `io.github.tt432.eyelibutil.time` | No production source consumers found in IDE text search. [VERIFIED: IDE `SimpleTimer` search] |
| `src/main/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerState.java` | `io.github.tt432.eyelib.core.util.time` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/FixedStepTimerState.java` | `io.github.tt432.eyelibutil.time` | Existing source test should move/update with the class. [VERIFIED: source read, IDE `FixedStepTimerState` search] |
| `src/main/java/io/github/tt432/eyelib/core/util/color/ColorEncodings.java` | `io.github.tt432.eyelib.core.util.color` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/color/ColorEncodings.java` | `io.github.tt432.eyelibutil.color` | Consumers: `FastColorHelper`, `NativeImageIO`, root seam test. [VERIFIED: IDE `ColorEncodings` search] |
| `src/main/java/io/github/tt432/eyelib/util/SharedLibraryLoader.java` | `io.github.tt432.eyelib.util` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` | `io.github.tt432.eyelibutil.loader` | No active production consumers found; native path audit remains future `AUDT-F01`. [VERIFIED: manifest, IDE `SharedLibraryLoader` search] |
| `src/main/java/io/github/tt432/eyelib/util/math/Curves.java` | `io.github.tt432.eyelib.util.math` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Curves.java` | `io.github.tt432.eyelibutil.math` | Consumer: `BrBoneKeyFrame`. [VERIFIED: IDE `Curves` search] |
| `src/main/java/io/github/tt432/eyelib/util/math/EyeMath.java` | `io.github.tt432.eyelib.util.math` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/EyeMath.java` | `io.github.tt432.eyelibutil.math` | Consumers: animation, render visitor, manager UI. [VERIFIED: IDE `EyeMath` search] |
| `src/main/java/io/github/tt432/eyelib/util/math/MathHelper.java` | `io.github.tt432.eyelib.util.math` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/MathHelper.java` | `io.github.tt432.eyelibutil.math` | Consumers: `EyelibManagerScreen`, `BrClipExecutor`. [VERIFIED: IDE `MathHelper` search] |
| `src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java` | `io.github.tt432.eyelib.util.math` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/FastColorHelper.java` | `io.github.tt432.eyelibutil.math` | Update internal import to `io.github.tt432.eyelibutil.color.ColorEncodings`. [VERIFIED: source read] |
| `src/main/java/io/github/tt432/eyelib/util/math/Shapes.java` | `io.github.tt432.eyelib.util.math` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Shapes.java` | `io.github.tt432.eyelibutil.math` | Imports `RandomSource` and `Vector3f`; Forge/JOML are allowed in util module. [VERIFIED: source read, `eyelib-util/README.md`] |
| `src/main/java/io/github/tt432/eyelib/util/search/Searchable.java` | `io.github.tt432.eyelib.util.search` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/Searchable.java` | `io.github.tt432.eyelibutil.search` | Consumer: `BrAttachableLoader`; owns default construction of `SearchResults`. [VERIFIED: IDE `Searchable` search] |
| `src/main/java/io/github/tt432/eyelib/util/search/SearchResults.java` | `io.github.tt432.eyelib.util.search` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/SearchResults.java` | `io.github.tt432.eyelibutil.search` | Internal peer of `Searchable`. [VERIFIED: source read, manifest] |

### Collection utility files
| Source file | Current package | Target file | Target package | Notes |
|-------------|-----------------|-------------|----------------|-------|
| `src/main/java/io/github/tt432/eyelib/util/Blackboard.java` | `io.github.tt432.eyelib.util` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Blackboard.java` | `io.github.tt432.eyelibutil.collection` | No production source consumers found, but ROADMAP requires migration. [VERIFIED: IDE `Blackboard` search, ROADMAP] |
| `src/main/java/io/github/tt432/eyelib/util/Lists.java` | `io.github.tt432.eyelib.util` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Lists.java` | `io.github.tt432.eyelibutil.collection` | Uses FastUtil; unrelated `com.google.common.collect.Lists` hits must not be changed. [VERIFIED: source read, IDE `Lists` search] |
| `src/main/java/io/github/tt432/eyelib/util/Collectors.java` | `io.github.tt432.eyelib.util` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Collectors.java` | `io.github.tt432.eyelibutil.collection` | No external consumer beyond collection family; avoid colliding with `java.util.stream.Collectors` imports. [VERIFIED: IDE `Collectors` search] |
| `src/main/java/io/github/tt432/eyelib/util/EntryStreams.java` | `io.github.tt432.eyelib.util` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/EntryStreams.java` | `io.github.tt432.eyelibutil.collection` | Consumer: `client/model/ModelPartModel`. [VERIFIED: IDE `EntryStreams` search] |
| `src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java` | `io.github.tt432.eyelib.core.util.collection` | `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/ListAccessors.java` | `io.github.tt432.eyelibutil.collection` | Canonical replacement for `ListHelper`. [VERIFIED: manifest, source read] |
| `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` | `io.github.tt432.eyelib.util` | **delete** | — | Do not migrate; replace former calls with `ListAccessors.first/last`. [VERIFIED: ROADMAP, manifest, IDE `ListHelper` search] |

### Explicitly out of scope in Phase 17
- Do not move `ResourceLocations.java`, `TexturePathHelper.java`, or `core/util/texture/TexturePaths.java`; these are Phase 18. [VERIFIED: `17-CONTEXT.md`, ROADMAP, manifest]
- Do not move `util/codec/**`, `ImmutableFloatTreeMap.java`, `EitherHelper.java`, or `core/util/codec/Eithers.java`; these are Phase 19. [VERIFIED: `17-CONTEXT.md`, ROADMAP, manifest]
- Do not move attachment/material duplicate shared code; this is Phase 20. [VERIFIED: `17-CONTEXT.md`, ROADMAP]

## Architecture Patterns

### System Architecture Diagram

```text
Phase 17 planner input
        |
        v
Route manifest + ROADMAP success criteria
        |
        v
Create category packages in :eyelib-util (time/color/loader/math/search/collection)
        |
        v
IDE-aware file moves / package rewrites
        |
        +--> Update intra-util imports (FastColorHelper -> ColorEncodings; Searchable -> SearchResults)
        |
        +--> Add root Gradle dependency edge to :eyelib-util
        |
        +--> Rewire root consumers and tests to io.github.tt432.eyelibutil.*
        |
        +--> Replace ListHelper calls with ListAccessors.first/last, then delete ListHelper
        |
        v
JetBrains MCP verification
        |
        +--> :eyelib-util:build
        +--> full project build/rebuild
        +--> IDE residual import scans
        v
Phase 17 success criteria satisfied
```

### Recommended Project Structure

```text
eyelib-util/src/main/java/io/github/tt432/eyelibutil/
├── bootstrap/                 # existing Forge marker package
├── collection/                # Blackboard, Lists, Collectors, EntryStreams, ListAccessors
├── color/                     # ColorEncodings
├── loader/                    # SharedLibraryLoader
├── math/                      # Curves, EyeMath, MathHelper, FastColorHelper, Shapes
├── search/                    # Searchable, SearchResults
└── time/                      # SimpleTimer, FixedStepTimerState

eyelib-util/src/test/java/io/github/tt432/eyelibutil/
├── collection/                # ListAccessors/collection behavior tests as needed
├── color/                     # ColorEncodings test coverage as needed
└── time/                      # FixedStepTimerStateTest moved from root
```

### Pattern 1: Dependency edge before consumer rewiring
**What:** Add root project dependency on `:eyelib-util` before changing imports, so IDE resolution and Gradle compile can see the new module. [VERIFIED: root `build.gradle` dependency pattern]

**Use this root dependency pattern:**
```gradle
dependencies {
    api project(':eyelib-util')
    modImplementation project(':eyelib-util')
    jarJar project(':eyelib-util')
}
```

**When to use:** Phase 17 root consumers import `io.github.tt432.eyelibutil.*`, so root must consume the util module. [VERIFIED: Phase 17 ROADMAP success criteria]

**Why this shape:** Root uses `api + modImplementation + jarJar` for Forge functional modules such as attachment/material/particle/importer; `eyelib-util` is a Forge module with `mods.toml`, so matching that consumption pattern is the least surprising default. [VERIFIED: root `build.gradle`, `eyelib-util/build.gradle`; confidence HIGH]

### Pattern 2: IDE-aware move/refactor per category
**What:** Use IDE move/refactor tools where possible to move files and update package declarations/imports; if applying patches manually, immediately run IDE searches/diagnostics before build. [VERIFIED: project constraints and IDE skill availability]

**When to use:** Every source move in this phase changes a Java package and import sites. [VERIFIED: target file table]

**Example import outcomes:**
```java
import io.github.tt432.eyelibutil.math.EyeMath;
import io.github.tt432.eyelibutil.collection.EntryStreams;
import io.github.tt432.eyelibutil.search.Searchable;
import io.github.tt432.eyelibutil.color.ColorEncodings;
```

### Pattern 3: Replace shim names with canonical names
**What:** Replace `ListHelper.getFirst(list)` with `ListAccessors.first(list)` and `ListHelper.getLast(list)` with `ListAccessors.last(list)`, then delete `ListHelper.java`. [VERIFIED: `ListHelper.java`, `ListAccessors.java`, manifest]

**When to use:** `BrBoneKeyFrame` is the only production source consumer found in IDE search. [VERIFIED: IDE `ListHelper` search]

**Example:**
```java
import io.github.tt432.eyelibutil.collection.ListAccessors;

return isPre ? ListAccessors.first(keyFrame.dataPoints()) : ListAccessors.last(keyFrame.dataPoints());
```

### Anti-Patterns to Avoid
- **Migrating Phase 18/19/20 files early:** Moving texture/resource/codec/submodule-centralization files now violates the phase boundary. [VERIFIED: `17-CONTEXT.md`, ROADMAP]
- **Keeping `ListHelper` as a compatibility facade:** This fails Phase 17 success criterion 5. [VERIFIED: `.planning/ROADMAP.md`]
- **Creating split packages under `io.github.tt432.eyelib.util`:** `:eyelib-util` owns `io.github.tt432.eyelibutil`, not root's old package. [VERIFIED: `eyelib-util/README.md`]
- **Assuming text search is enough for Java moves:** Prefer IDE-aware refactors/searches; text-only replacement can hit unrelated classes such as Guava `Lists` or `java.util.stream.Collectors`. [VERIFIED: IDE searches showing unrelated `Lists`/`Collectors` hits]
- **Running Gradle from shell:** Explicitly forbidden. [VERIFIED: `AGENTS.md`]

## Consumer / Import Rewiring Strategy

| Consumer file | Old import/use | New import/use | Notes |
|---------------|----------------|----------------|-------|
| `client/gui/manager/EyelibManagerScreen.java` | `io.github.tt432.eyelib.util.math.MathHelper` | `io.github.tt432.eyelibutil.math.MathHelper` | Two clamp calls. [VERIFIED: IDE `MathHelper` search] |
| `client/gui/manager/AnimationView.java` | `io.github.tt432.eyelib.util.math.EyeMath` | `io.github.tt432.eyelibutil.math.EyeMath` | Uses degrees/radians constant. [VERIFIED: IDE `EyeMath` search] |
| `client/render/visitor/ModelVisitor.java` | `io.github.tt432.eyelib.util.math.EyeMath` | `io.github.tt432.eyelibutil.math.EyeMath` | Uses constant. [VERIFIED: IDE `EyeMath` search] |
| `client/model/ModelPartModel.java` | `io.github.tt432.eyelib.util.EntryStreams` | `io.github.tt432.eyelibutil.collection.EntryStreams` | Two collection calls. [VERIFIED: IDE `EntryStreams` search] |
| `client/loader/BrAttachableLoader.java` | `io.github.tt432.eyelib.util.search.Searchable` | `io.github.tt432.eyelibutil.search.Searchable` | Implements interface. [VERIFIED: IDE `Searchable` search] |
| `client/animation/bedrock/BrBoneAnimation.java` | `io.github.tt432.eyelib.util.math.EyeMath` | `io.github.tt432.eyelibutil.math.EyeMath` | Weight calculation. [VERIFIED: IDE `EyeMath` search] |
| `client/animation/bedrock/BrBoneAnimationSampler.java` | `io.github.tt432.eyelib.util.math.EyeMath` | `io.github.tt432.eyelibutil.math.EyeMath` | Weight calculation. [VERIFIED: IDE `EyeMath` search] |
| `client/animation/bedrock/BrClipExecutor.java` | `EyeMath`, `MathHelper` from old util math | `io.github.tt432.eyelibutil.math.EyeMath`, `io.github.tt432.eyelibutil.math.MathHelper` | Blend/clamp and radians conversion. [VERIFIED: IDE searches] |
| `client/animation/bedrock/BrBoneKeyFrame.java` | `ListHelper`, `Curves`, `EyeMath` | `ListAccessors`, `Curves`, `EyeMath` under `eyelibutil` | Replace method names for `ListHelper` calls. [VERIFIED: IDE `ListHelper`, `Curves`, `EyeMath` searches] |
| `client/render/texture/NativeImageIO.java` | `io.github.tt432.eyelib.core.util.color.ColorEncodings` | `io.github.tt432.eyelibutil.color.ColorEncodings` | Two pixel conversion calls. [VERIFIED: IDE `ColorEncodings` search] |
| `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java` | core `ColorEncodings`, core `ListAccessors` | `eyelibutil.color.ColorEncodings`, `eyelibutil.collection.ListAccessors` | Keep `Eithers` and `TexturePaths` old until Phase 18/19. [VERIFIED: source read, phase boundaries] |
| `FixedStepTimerStateTest.java` | package-local root core test | Move to `eyelib-util/src/test/java/io/github/tt432/eyelibutil/time/FixedStepTimerStateTest.java` | Keeps timer behavior covered in owning module. [VERIFIED: source read] |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Cross-package Java moves | Manual search/replace only | IDE move/refactor plus IDE import scans | Avoid missed imports and unrelated `Lists`/`Collectors` false positives. [VERIFIED: IDE search evidence] |
| List first/last compatibility | New `ListHelper` wrapper in util module | `ListAccessors.first/last` | ROADMAP requires `ListHelper` deletion. [VERIFIED: ROADMAP, manifest] |
| Build execution | Shell Gradle wrapper | JetBrains MCP Gradle tools | Shell Gradle is forbidden. [VERIFIED: `AGENTS.md`] |
| New Gradle conventions | Custom dependency configurations | Existing root Forge-module pattern | Root already consumes Forge submodules with `api`, `modImplementation`, and `jarJar`. [VERIFIED: root `build.gradle`] |

**Key insight:** Phase 17 is not an algorithm rewrite; all risk comes from ownership/package/build-classpath rewiring. [VERIFIED: `.planning/REQUIREMENTS.md` out-of-scope table]

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|-----------------|
| Stored data | None found for these class/package names in repo-tracked source or phase docs; Phase 17 utilities are Java classes, not persisted keys. [VERIFIED: IDE searches for target symbols; local docs] | No data migration. |
| Live service config | None found; no external service configuration is part of Phase 17. [VERIFIED: CONTEXT/ROADMAP scope] | No service config update. |
| OS-registered state | `SharedLibraryLoader` may extract/load native libraries into `.natives/`, temp, or user-home locations at runtime, but IDE search found no active production consumer. [VERIFIED: `SharedLibraryLoader.java`, IDE search] | No immediate OS re-registration; keep `AUDT-F01` as future native path audit. |
| Secrets/env vars | None found; Phase 17 target sources do not read project secrets/env var names, though `SharedLibraryLoader` checks `APP_SANDBOX_CONTAINER_ID`. [VERIFIED: source read] | No secret/env migration. |
| Build artifacts | Existing compiled classes under build outputs may retain old packages until clean/rebuild. [ASSUMED] | Use JetBrains MCP full rebuild/clean-equivalent if stale IDE/build artifacts cause false failures. |

## Common Pitfalls

### Pitfall 1: Moving `ListHelper` instead of deleting it
**What goes wrong:** The phase appears to compile but fails the explicit `ListHelper.java is deleted` success criterion. [VERIFIED: ROADMAP]
**Why it happens:** `ListHelper` looks like a normal collection utility but is only a shim over `ListAccessors`. [VERIFIED: `ListHelper.java`, manifest]
**How to avoid:** Rewire `BrBoneKeyFrame` to `ListAccessors.first/last`; delete `ListHelper.java`; scan for `ListHelper`. [VERIFIED: IDE `ListHelper` search]
**Warning signs:** Any source or import under `io.github.tt432.eyelib.util.ListHelper` remains. [VERIFIED: ROADMAP success criterion]

### Pitfall 2: False-positive collection imports
**What goes wrong:** Replacing `Lists` or `Collectors` by text can alter Guava `Lists` or JDK `Collectors` usage. [VERIFIED: IDE `Lists`/`Collectors` searches]
**Why it happens:** Class names collide with common libraries. [VERIFIED: IDE search results]
**How to avoid:** Update only fully-qualified imports from `io.github.tt432.eyelib.util.*`; use IDE symbol-aware tools where possible. [VERIFIED: IDE skill guidance]
**Warning signs:** Diffs touch `com.google.common.collect.Lists` or `java.util.stream.Collectors` imports. [VERIFIED: IDE search results]

### Pitfall 3: Missing FastUtil on `:eyelib-util` compile classpath
**What goes wrong:** `Lists.java` may fail compilation if `it.unimi.dsi.fastutil.ints.Int2ObjectFunction` is not available through the Forge/Minecraft classpath. [VERIFIED: `Lists.java`; confidence MEDIUM]
**Why it happens:** `eyelib-util/build.gradle` does not explicitly declare FastUtil. [VERIFIED: `eyelib-util/build.gradle`]
**How to avoid:** Run `:eyelib-util:build` via JetBrains MCP immediately after moving `Lists.java`; if it fails only on FastUtil resolution, add the minimal external dependency or document why Forge classpath should supply it before changing. [VERIFIED: project verification constraints]
**Warning signs:** Compile error for `it.unimi.dsi.fastutil.ints.Int2ObjectFunction`. [ASSUMED]

### Pitfall 4: Moving Phase 18/19 dependencies accidentally
**What goes wrong:** A broad package move drags `ResourceLocations`, texture, codec, or `ImmutableFloatTreeMap` into Phase 17 and creates avoidable circular/dependency issues. [VERIFIED: phase boundaries]
**Why it happens:** Target files share old `util`/`core/util` roots. [VERIFIED: manifest inventory]
**How to avoid:** Move exactly the files listed in this research; leave `package-info.java` cleanup to Phase 21 unless a package becomes invalid. [VERIFIED: manifest package cleanup rows]
**Warning signs:** Diff includes `ResourceLocations.java`, `TexturePaths.java`, `TexturePathHelper.java`, `util/codec/**`, `ImmutableFloatTreeMap.java`, `EitherHelper.java`, or `Eithers.java`. [VERIFIED: manifest]

## Code Examples

### Root build dependency addition
```gradle
dependencies {
    api project(':eyelib-util')
    modImplementation project(':eyelib-util')
    jarJar project(':eyelib-util')
}
```
Source: root submodule consumption pattern in `build.gradle`; util module is Forge-module scaffold. [VERIFIED: `build.gradle`, `eyelib-util/build.gradle`]

### New `FastColorHelper` internal import
```java
package io.github.tt432.eyelibutil.math;

import io.github.tt432.eyelibutil.color.ColorEncodings;

public class FastColorHelper {
    public static int argbToAbgr(int argb32) {
        return ColorEncodings.argbToAbgr(argb32);
    }
}
```
Source: current wrapper delegates to `ColorEncodings`; package/import adjusted to target namespace. [VERIFIED: `FastColorHelper.java`, `ColorEncodings.java`]

### `ListHelper` replacement pattern
```java
import io.github.tt432.eyelibutil.collection.ListAccessors;

private static MolangValue3 getKeyFrameData(BrBoneKeyFrame keyFrame, boolean isPre) {
    return isPre ? ListAccessors.first(keyFrame.dataPoints()) : ListAccessors.last(keyFrame.dataPoints());
}
```
Source: `BrBoneKeyFrame` currently calls `ListHelper.getFirst/getLast`; `ListAccessors` provides `first/last`. [VERIFIED: source reads]

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Root/core utility source under `src/main/java/io/github/tt432/eyelib/util` and `core/util` | Dedicated `:eyelib-util` Forge leaf module under `io.github.tt432.eyelibutil` | Phase 16 scaffold; Phase 17 begins source migration. [VERIFIED: ROADMAP, `16-VERIFICATION.md`] | Root must add dependency and import new packages. |
| Compatibility shim `ListHelper` | Canonical `ListAccessors` | Planned Phase 17 deletion. [VERIFIED: manifest, ROADMAP] | `BrBoneKeyFrame` method names must change. |
| Scaffold-only `:eyelib-util` with no implementation files | Category-owned utility packages | Phase 17 plan target. [VERIFIED: ROADMAP] | Module docs may need update from “Phase 16 only creates scaffold” wording. |

**Deprecated/outdated:**
- `io.github.tt432.eyelib.util.ListHelper`: delete in Phase 17 after rewiring to `ListAccessors`. [VERIFIED: ROADMAP, manifest]
- Imports under `io.github.tt432.eyelib.util.math`, `.util.search`, `.util.EntryStreams`, `.util.ListHelper`, `.core.util.color`, `.core.util.collection`, and `.core.util.time` for Phase 17 targets: replace with `io.github.tt432.eyelibutil.*`. [VERIFIED: IDE import scans]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Build outputs may retain stale old-package classes until a rebuild/clean-equivalent runs. | Runtime State Inventory | Planner may include unnecessary rebuild advice, but it is safe. |
| A2 | A missing FastUtil compile error would specifically mention `it.unimi.dsi.fastutil.ints.Int2ObjectFunction`. | Common Pitfalls | Planner may need to inspect actual compiler wording if it differs. |

## Open Questions (RESOLVED)

1. **Should `SharedLibraryLoader` remain in `loader/` or a future `native/` package?**
   - RESOLVED: Use `io.github.tt432.eyelibutil.loader.SharedLibraryLoader` per D-01. Do not introduce a `native/` package in Phase 17.
   - What we know: ROADMAP classifies it as Phase 17 loader/misc and no active production consumer was found. [VERIFIED: ROADMAP, IDE search]
   - What's unclear: No local doc defines a final native-loader package naming convention. [VERIFIED: local docs read]
   - Recommendation: Use `io.github.tt432.eyelibutil.loader.SharedLibraryLoader` for Phase 17 because it matches the phase category and can be renamed later only if a real consumer demands it. [ASSUMED]

2. **Will `Lists.java` compile without an explicit FastUtil dependency?**
   - RESOLVED: Migrate `Lists.java` fully per D-02 and prove the FastUtil classpath with JetBrains MCP `:eyelib-util:build`. Do not simplify or omit the FastUtil-dependent API.
   - What we know: `Lists.java` imports FastUtil and `eyelib-util` does not explicitly declare FastUtil. [VERIFIED: source/build reads]
   - What's unclear: Whether the Forge/Minecraft classpath exposes FastUtil to `:eyelib-util` compilation in this project instance. [VERIFIED: no build executed during research]
   - Recommendation: Treat `:eyelib-util:build` as the first post-move gate; add no dependency unless the JetBrains MCP build proves it is missing. [VERIFIED: project verification rules]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JetBrains IDE Index MCP | Semantic file/search/refactor checks | ✓ | Index ready, not dumb mode | Use IDE search tools; if refactor tool fails, sync files and use narrow patches plus diagnostics. [VERIFIED: `ide_index_status`] |
| JetBrains Gradle MCP | Build/test verification | ✓ | Linked Gradle project path detected as `E:\_ideaProjects\qylEyelib` | No shell Gradle fallback allowed; stop and ask user if JetBrains Gradle MCP is unavailable. [VERIFIED: `jetbrain_list_linked_gradle_projects`, `AGENTS.md`] |
| Java/Gradle project model | Source migration/build | ✓ | Java 17 toolchain declared | Use existing Gradle build files. [VERIFIED: `build.gradle`, `eyelib-util/build.gradle`] |

**Missing dependencies with no fallback:** None found during research. [VERIFIED: local tool availability checks]

**Missing dependencies with fallback:** None found during research. [VERIFIED: local tool availability checks]

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter via `org.junit:junit-bom:5.10.2`. [VERIFIED: root and util `build.gradle`] |
| Config file | No standalone JUnit config file found; Gradle `tasks.named('test').configure { useJUnitPlatform() }` controls execution. [VERIFIED: root and util `build.gradle`] |
| Quick run command | JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames=[":eyelib-util:build"]` after moving utility code. [VERIFIED: `16-VERIFICATION.md` precedent] |
| Full suite command | JetBrains MCP full project build via `jetbrain_build_project(projectPath="E:\\_ideaProjects\\qylEyelib", rebuild=true)` or `jetbrain_run_gradle_tasks` for `build` if IDE project build is insufficient. [VERIFIED: `AGENTS.md`; command selection recommendation] |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| MIGR-01 | 11 Tier-1 files compile from `:eyelib-util`; root consumers import new packages. | module build + full build + IDE residual scan | JetBrains MCP `:eyelib-util:build`; then full project build/rebuild; IDE regex for old imports. | ❌ Wave 0 for moved utility behavior tests; existing `FixedStepTimerStateTest` must move. [VERIFIED: source/test scans] |
| MIGR-02 | Collection utilities compile from `:eyelib-util`; `ListHelper` deleted and former caller uses `ListAccessors`. | module build + targeted IDE search + full build | JetBrains MCP `:eyelib-util:build`; IDE search `ListHelper`; full project build. | ❌ Wave 0 for `ListAccessors` under util module; root `CoreUtilitySeamTest` currently covers legacy core path. [VERIFIED: source/test reads] |

### Sampling Rate
- **Per task commit:** Run IDE diagnostics/search for touched files plus JetBrains MCP `:eyelib-util:build` when util module sources changed. [VERIFIED: project verification rules]
- **Per wave merge:** Run JetBrains MCP full project build/rebuild and residual import scans. [VERIFIED: ROADMAP success criteria]
- **Phase gate:** Full project build/rebuild green, no old Phase 17 imports, `ListHelper.java` deleted, target files absent from root/core and present in `:eyelib-util`. [VERIFIED: ROADMAP success criteria]

### Wave 0 Gaps
- [ ] Move or recreate `FixedStepTimerStateTest` under `eyelib-util/src/test/java/io/github/tt432/eyelibutil/time/` to cover `FixedStepTimerState`. [VERIFIED: existing root test]
- [ ] Add util-module tests or adapt existing `CoreUtilitySeamTest` coverage for `ColorEncodings` and `ListAccessors`; keep Phase 18/19 assertions for `TexturePaths`/`Eithers` in root until their phases. [VERIFIED: `CoreUtilitySeamTest`, phase boundaries]
- [ ] Add/import boundary assertions to `UtilModuleIdentityTest` only if planner wants a static guard that `:eyelib-util` still has zero `project(...)` dependencies after root consumes it. Existing test already checks the util module build file. [VERIFIED: `UtilModuleIdentityTest.java`]

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | no | No auth/session behavior in Phase 17. [VERIFIED: phase scope and target sources] |
| V3 Session Management | no | No session state in Phase 17. [VERIFIED: phase scope and target sources] |
| V4 Access Control | no | No permission boundaries in Phase 17. [VERIFIED: phase scope and target sources] |
| V5 Input Validation | yes | Preserve existing validation: `FixedStepTimerState` rejects non-positive rate; do not broaden inputs while moving. [VERIFIED: `FixedStepTimerState.java`] |
| V6 Cryptography | no | No cryptographic implementation in Phase 17. [VERIFIED: target source reads] |

### Known Threat Patterns for Java utility relocation

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Native library path/extraction behavior changes in `SharedLibraryLoader` | Tampering / Elevation of Privilege | Do not rewrite extraction/loading algorithm during migration; preserve package-only behavior and keep native audit deferred. [VERIFIED: `SharedLibraryLoader.java`, `STATE.md` deferred item] |
| Utility package collision causing wrong class import | Tampering | Use fully-qualified IDE import rewiring and residual scans; avoid text-only replacements for `Lists`/`Collectors`. [VERIFIED: IDE search evidence] |
| Build dependency accidentally reversed into `:eyelib-util` | Elevation of Privilege / Integrity | Keep `eyelib-util/build.gradle` free of `project(...)` dependencies and let root depend on util, not util on root. [VERIFIED: `eyelib-util/README.md`, `UtilModuleIdentityTest.java`] |

## Verification Checklist for Planner

1. Presence checks: all target files exist under `eyelib-util/src/main/java/io/github/tt432/eyelibutil/**`. [VERIFIED: target plan from ROADMAP]
2. Absence checks: old Phase 17 source files no longer exist under `src/main/java/io/github/tt432/eyelib/util/**` or `src/main/java/io/github/tt432/eyelib/core/util/**`. [VERIFIED: ROADMAP]
3. Residual import regex: `import\s+io\.github\.tt432\.eyelib\.(?:core\.)?util\.(?:time|math|search|collection|color|Blackboard|Lists|Collectors|EntryStreams|SharedLibraryLoader|ListHelper)` returns zero production-source matches except out-of-scope Phase 18/19 files if regex is widened. [VERIFIED: initial IDE regex scan]
4. `ListHelper` search: no source hits except historical docs/planning mentions. [VERIFIED: ROADMAP success criterion]
5. Gradle gates: JetBrains MCP `:eyelib-util:build` and full project build/rebuild complete with exit code 0. [VERIFIED: ROADMAP, `AGENTS.md`]

## Sources

### Primary (HIGH confidence)
- `.planning/phases/17-tier-1-category-migration/17-CONTEXT.md` — phase boundary, discretion, deferred scope.
- `.planning/ROADMAP.md` — Phase 17 success criteria and v1.3 phase boundaries.
- `.planning/REQUIREMENTS.md` — MIGR-01/MIGR-02 definitions and out-of-scope rules.
- `docs/architecture/migration/utility-routing-manifest.md` — route table, consumer classifications, `ListHelper` deletion timing.
- `.planning/phases/16-module-scaffold-build-infrastructure/16-VERIFICATION.md` — scaffold/build precedent and util module identity evidence.
- `AGENTS.md`, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md` — project constraints and module boundary rules.
- `build.gradle`, `eyelib-util/build.gradle`, `settings.gradle`, `eyelib-util/README.md`, `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md` — build/module configuration.
- IDE Index MCP searches on 2026-05-10 — target file inventory and consumer/import evidence.

### Secondary (MEDIUM confidence)
- Local inference from existing root Forge-module dependency patterns (`api`, `modImplementation`, `jarJar`) for adding root consumption of `:eyelib-util`.

### Tertiary (LOW confidence)
- Assumption that stale build artifacts may require rebuild/clean-equivalent after package moves.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — verified from local Gradle files and Phase 16 verification.
- Architecture: HIGH — verified from ROADMAP, manifest, module docs, and AGENTS constraints.
- Pitfalls: HIGH for phase-boundary/import/`ListHelper` risks; MEDIUM for FastUtil classpath risk until `:eyelib-util:build` runs after move.

**Research date:** 2026-05-10  
**Valid until:** 2026-06-09 for local project architecture; re-run IDE searches immediately before implementation because consumer counts can drift.
