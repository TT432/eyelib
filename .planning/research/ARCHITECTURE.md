# Architecture: `:eyelib-util` Module Extraction

**Domain:** Forge mod multi-project utility extraction
**Researched:** 2026-05-10
**Confidence:** HIGH

## Executive Summary

The extraction of `:eyelib-util` is significantly simpler than initial estimates suggested. A critical finding: **none of the 6 existing submodules (attachment, importer, molang, material, particle, processor) currently import from `io.github.tt432.eyelib.util.*`**. The identity tests in `eyelib-*/*/IdentityTest.java` actively enforce this boundary. All 32+ util imports exist solely within the root (:) module itself. This means the migration impacts root internals only — zero submodule import changes are required unless submodules opt into the new shared utility module for optional centralized code (e.g., attachment's streamcodec).

The 34 root/util/* files plus 5 core/util/* files consolidate into `:eyelib-util` under a new namespace `io.github.tt432.eyelibutil`. The root build.gradle adds a one-way dependency on `:eyelib-util`, and ~38 root import statements are mechanically updated. The dependency direction is strictly leaf-to-root: `:eyelib-util` may depend on MC/Forge and external libraries (DFU, JOML, SLF4J) but MUST NOT depend on any other Eyelib module.

## Dependency Direction Diagram

### Before Extraction (Current State)
```
root (:) — owns io.github.tt432.eyelib.util.* and core/util/* internally
  ├── eyelib-attachment  ──X── (NO import from root util)
  ├── eyelib-importer    ──X── (NO import from root util)
  ├── eyelib-material    ──X── (NO import from root util)
  ├── eyelib-molang      ──X── (NO import from root util, pure java-library)
  ├── eyelib-particle    ──X── (NO import from root util)
  └── eyelib-processor   ──X── (NO import from root util, pure java-library)
```

### After Extraction (Target State)
```
eyelib-util (Forge module, leaf — no Eyelib dependencies)
  │  depends on: MC/Forge, com.mojang:datafixerupper, org.joml:joml, org.slf4j:slf4j-api
  │  owns: io.github.tt432.eyelibutil.*
  │
  ├── root (:) ── depends on eyelib-util ── imports io.github.tt432.eyelibutil.*
  │     ├── eyelib-attachment  (no util imports, may optionally add eyelib-util dependency)
  │     ├── eyelib-importer    (no util imports, may optionally add eyelib-util dependency)
  │     ├── eyelib-material    (no util imports, may optionally add eyelib-util dependency)
  │     ├── eyelib-molang      (no util imports, may optionally add eyelib-util dependency)
  │     ├── eyelib-particle    (no util imports, may optionally add eyelib-util dependency)
  │     └── eyelib-processor   (no util imports, may optionally add eyelib-util dependency)
  │
  └── (optional consumers)
        └── eyelib-attachment ── if streamcodec centralized into eyelib-util
```

### Dependency Direction Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| **D-1** | `:eyelib-util` MUST NOT depend on root or any submodule | Build-time: no `project(':')` or `project(':eyelib-*')` in eyelib-util/build.gradle |
| **D-2** | `:eyelib-util` MAY depend on MC/Forge, DFU, JOML, SLF4J | Allowed: `legacyForge` plugin, `com.mojang:datafixerupper`, `org.joml:joml`, `org.slf4j:slf4j-api` |
| **D-3** | Root MUST add `implementation project(':eyelib-util')` | Build-time: in root build.gradle dependencies block |
| **D-4** | Submodules OPTIONALLY add `implementation project(':eyelib-util')` | Per submodule build.gradle |
| **D-5** | eyelib-util namespace: `io.github.tt432.eyelibutil.*` | No split packages with root (`io.github.tt432.eyelib.util.*`) |
| **D-6** | root/util/* directory MUST be empty after extraction | Verification: `glob("src/main/java/io/github/tt432/eyelib/util/**/*.java")` returns empty |
| **D-7** | core/util/* directory MUST be empty after extraction | Verification: `glob("src/main/java/io/github/tt432/eyelib/core/util/**/*.java")` returns empty |

---

## Integration Points

### Current Import Site Map (root module only)

| Root Package | Files | Util Imports Used | Purpose |
|---|---|---|---|
| `client/animation/bedrock/` | 8 files | `ImmutableFloatTreeMap`, `CodecHelper`, `EyeMath`, `Curves`, `MathHelper`, `ChinExtraCodecs`, `ListHelper`, `ResourceLocations`, wildcard imports (`util.*`, `codec.*`) | Animation codec definitions, floating-point keyframe interpolation, bone math |
| `client/animation/bedrock/controller/` | 1 file | `CodecHelper` | Controller codec serialization |
| `client/render/` | 2 files | `TexturePathHelper`, `ResourceLocations` | Render params texture path, render sync service |
| `client/render/visitor/` | 1 file | `EyeMath` | Model rendering transforms |
| `client/model/` | 1 file | `EntryStreams` | Model-part iteration |
| `client/loader/` | 1 file | `Searchable` | Attachable resource search |
| `client/gui/manager/` | 2 files | `EyeMath`, `MathHelper` | GUI animation rendering, transform math |
| `common/behavior/` | 3 files | `EyelibCodec`, `KeyDispatchMapCodec` | Entity behavior codec serialization |
| `mc/impl/molang/mapping/` | 1 file | `ResourceLocations` | Molang MC-side query implementation |
| `util/*` (internal) | 6 files | `ListAccessors`, `Eithers`, `ColorEncodings`, `TexturePaths`, `CodecHelper`, `Tuple.*` | Internal utility cross-references within util subtree |

### Submodule Integration Points (post-extraction candidates)

| Submodule | Current State | Post-Extraction | Rationale |
|---|---|---|---|
| **eyelib-attachment** | Zero util imports. Owns StreamCodec/StreamEncoder/StreamDecoder/EyelibStreamCodecs with MC dependencies | **HIGH priority**: move streamcodec to eyelib-util, add eyelib-util dependency | StreamCodec is a general-purpose FriendlyByteBuf abstraction needed by network code across modules; currently siloed in attachment |
| **eyelib-importer** | Zero util imports. Has identity test enforcing boundary | Optional: add eyelib-util for generic helpers | Only if importer code needs shared collection/math helpers |
| **eyelib-material** | Zero util imports | Optional: add eyelib-util for generic helpers | Low urgency; material has few external dependencies |
| **eyelib-molang** | Zero util imports. Pure java-library | Optional: add eyelib-util for generic helpers | Currently depends only on DFU, JOML, SLF4J, ANTLR, jdk-classfile-backport |
| **eyelib-particle** | Zero util imports. Has boundary test for client.* | Optional: add eyelib-util for generic helpers | May benefit from shared codec/math helpers |
| **eyelib-processor** | Zero util imports. Pure java-library | Optional: add eyelib-util if it needs platform-free helpers | Currently compileOnly-depends on importer and implementation-depends on molang |

---

## Package Namespace Strategy

**Decision:** `io.github.tt432.eyelibutil` — new namespace, no split packages.

**Rationale:**
- Java classpath model: two Gradle modules sharing `io.github.tt432.eyelib.util` creates an unreliable split package (first-on-classpath wins, non-deterministic in multi-module builds)
- Java module system (jpms): split packages across modules are a hard error
- The existing submodules already use distinct namespaces (`eyelibattachment`, `eyelibimporter`, `eyelibmolang`, `eyelibmaterial`, `eyelibparticle`, `eyelibprocessor`) — `eyelibutil` follows the same convention
- Mechanical import rewriting is straightforward: `s/\bio\.github\.tt432\.eyelib\.util\./io.github.tt432.eyelibutil./g` across root source

**Migration mapping:**

| Source Package | → | Target Package (eyelib-util) |
|---|---|---|
| `io.github.tt432.eyelib.util` | → | `io.github.tt432.eyelibutil` |
| `io.github.tt432.eyelib.util.codec` | → | `io.github.tt432.eyelibutil.codec` |
| `io.github.tt432.eyelib.util.math` | → | `io.github.tt432.eyelibutil.math` |
| `io.github.tt432.eyelib.util.client` | → | `io.github.tt432.eyelibutil.client` |
| `io.github.tt432.eyelib.util.client.texture` | → | `io.github.tt432.eyelibutil.client.texture` |
| `io.github.tt432.eyelib.util.search` | → | `io.github.tt432.eyelibutil.search` |
| `io.github.tt432.eyelib.util.modbridge` | → | `io.github.tt432.eyelibutil.modbridge` |
| `io.github.tt432.eyelib.core.util.collection` | → | `io.github.tt432.eyelibutil.collection` |
| `io.github.tt432.eyelib.core.util.texture` | → | `io.github.tt432.eyelibutil.texture` |
| `io.github.tt432.eyelib.core.util.color` | → | `io.github.tt432.eyelibutil.color` |
| `io.github.tt432.eyelib.core.util.codec` | → | `io.github.tt432.eyelibutil.codec` |
| `io.github.tt432.eyelib.core.util.time` | → | `io.github.tt432.eyelibutil.time` |
| (new) attachment streamcodec | → | `io.github.tt432.eyelibutil.streamcodec` |

---

## New vs Modified Components

### New Components (Created)

| Component | Location | Type | Description |
|---|---|---|---|
| `eyelib-util/` | Workspace root | Gradle subproject | New Forge module, `java-library` + `legacyForge` plugins |
| `eyelib-util/build.gradle` | `eyelib-util/` | Build file | Dependencies: Forge, DFU, JOML, SLF4J, jdk-classfile-backport; NO project dependencies |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/` | Source root | Package tree | ~39 files from root/util/* + core/util/*, package-declared to `eyelibutil` |
| `eyelib-util/src/main/resources/META-INF/mods.toml` | Resource | Forge metadata | Mod ID: `eyelibutil`, description identifying it as shared utility module |
| `eyelib-util/src/test/` | Test root | Test infrastructure | JUnit 5 platform tests, one identity test verifying no reverse deps |
| `settings.gradle` | Workspace root | Modified | Add: `include("eyelib-util")` |

### Modified Components (Updated)

| Component | File/Metric | Change |
|---|---|---|
| Root build.gradle | `E:\_ideaProjects\qylEyelib\build.gradle` | Add `implementation project(':eyelib-util')` + `jarJar project(':eyelib-util')` |
| Root source files | ~32 files | Update `import io.github.tt432.eyelib.util.*` → `io.github.tt432.eyelibutil.*` |
| Root source files | ~6 files | Update `import io.github.tt432.eyelib.core.util.*` → `io.github.tt432.eyelibutil.*` |
| util internal cross-refs | ~6 files (within util) | Package declaration + cross-import updates during file move |

### Deleted Components

| Component | Location | Rationale |
|---|---|---|
| `src/main/java/io/github/tt432/eyelib/util/` | Entire directory | All 34 files moved to eyelib-util |
| `src/main/java/io/github/tt432/eyelib/core/util/` | Entire directory | All 5 files merged into eyelib-util |
| Root `util/README.md` | `src/main/java/io/github/tt432/eyelib/util/README.md` | Replaced by eyelib-util/README.md |
| Root `core/README.md` util section | `src/main/java/io/github/tt432/eyelib/core/README.md` | Updated to remove util references |

### Unchanged Components

| Component | Rationale |
|---|---|
| All 6 submodule build.gradle files | Only modified if submodule opts into eyelib-util dependency |
| Submodule source files | Zero util imports = no changes needed |
| `MODULES.md` | Updated in same change per Module Update Rule #3 |
| `docs/architecture/01-module-boundaries.md` | Updated in same change per Module Update Rule #4 |
| `docs/architecture/ARCHITECTURE-BLUEPRINT.md` | Updated to add eyelib-util node |

---

## Recommended Build Order

### Phase 1: Module Scaffold (no consumer changes)
**Rationale:** eyelib-util has zero dependencies on other Eyelib modules — it can be built and compiled independently from day one.

1. Create `eyelib-util/` directory structure
2. Write `eyelib-util/build.gradle` (Forge module, depends on MC/Forge + DFU + JOML + SLF4J)
3. Write `eyelib-util/src/main/resources/META-INF/mods.toml`
4. Add `include("eyelib-util")` to `settings.gradle`
5. Update root `settings.gradle` to include new module
6. Build-only verification: `jetbrain_build_project` with `filesToRebuild` scoped to new module

### Phase 2: Code Migration (mechanical)
**Rationale:** Move code first, update imports second, verify third. Keep each wave atomic.

7. Move all 34 root/util/* files to eyelib-util with updated package declarations (`io.github.tt432.eyelibutil.*`)
8. Merge all 5 core/util/* files into eyelib-util with consolidated package hierarchy
9. Fix internal cross-references within eyelib-util (util classes that import from other util classes)
10. Write eyelib-util identity test: verify no imports from `io.github.tt432.eyelib.`

### Phase 3: Root Rewire
**Rationale:** Root is the ONLY consumer that needs import updates.

11. Add `implementation project(':eyelib-util')` and `jarJar project(':eyelib-util')` to root build.gradle
12. Update all ~32 import statements in root from `io.github.tt432.eyelib.util.*` → `io.github.tt432.eyelibutil.*`
13. Update all ~6 import statements in root from `io.github.tt432.eyelib.core.util.*` → `io.github.tt432.eyelibutil.*`
14. Build verification: `jetbrain_build_project` full build (root + eyelib-util + all submodules)

### Phase 4: Submodule Centralization (opportunistic)
**Rationale:** Centralize code currently siloed in individual submodules that other modules could benefit from.

15. Move attachment `StreamCodec`, `StreamEncoder`, `StreamDecoder`, `EyelibStreamCodecs` → `eyelib-util/src/main/java/io/github/tt432/eyelibutil/streamcodec/`
16. Add `implementation project(':eyelib-util')` to attachment's build.gradle
17. Update attachment imports to use `io.github.tt432.eyelibutil.streamcodec.*`
18. (Optional) Add eyelib-util as dependency to other submodules that benefit from shared helpers

### Phase 5: Cleanup & Documentation
19. Delete empty `src/main/java/io/github/tt432/eyelib/util/` directory
20. Delete empty `src/main/java/io/github/tt432/eyelib/core/util/` directory
21. Write `eyelib-util/src/main/java/io/github/tt432/eyelibutil/README.md`
22. Update `MODULES.md` — add eyelib-util entry, update root util rows, update count
23. Update `docs/architecture/01-module-boundaries.md` — add eyelib-util boundary rules
24. Update `docs/architecture/ARCHITECTURE-BLUEPRINT.md` — add eyelib-util to diagram
25. Final build + test verification: all modules compile, nullawayMain passes, submodule identity tests pass

---

## Patterns to Follow

### Pattern: One-Way Dependency Leaf Module
**What:** eyelib-util is a leaf in the dependency graph — it is depended upon but depends on nothing from the project.
**When:** Any shared code module that serves multiple consumers.
**Example (build.gradle):**
```groovy
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

dependencies {
    // External only — NO project(':...') dependencies allowed
    implementation 'com.mojang:datafixerupper:6.0.8'
    implementation 'org.joml:joml:1.10.5'
    implementation 'org.slf4j:slf4j-api:2.0.7'
    compileOnly 'org.jspecify:jspecify:1.0.0'
}
```

### Pattern: Mechanical Import Rewriting
**What:** All root import changes are `s/eyelib.util./eyelibutil./g` and `s/eyelib.core.util./eyelibutil./g` transformations.
**When:** Moving an entire package tree from one module to another with namespace change.
**Verification:** After rewriting, search for any remaining `import io.github.tt432.eyelib.util.` in root source — count must be zero.

### Pattern: Identity Test
**What:** Each subproject has a test that verifies it does not import from root packages.
**When:** Adding a new subproject.
**Example (eyelib-util/IdentityTest.java):**
```java
@Test
void eyutilModule_mustNotImportRoot() {
    // All source files under eyelib-util/src/main/java/
    for (Path p : sourceFiles) {
        String content = Files.readString(p);
        assertFalse(content.contains("import io.github.tt432.eyelib."),
            p + " must not import root runtime packages");
    }
}
```

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Split Packages
**What:** Keeping `io.github.tt432.eyelib.util` as the package name in both root and eyelib-util.
**Why bad:** Non-deterministic class resolution on classpath; hard error in jpms; breaks IDE tooling.
**Instead:** Use `io.github.tt432.eyelibutil` — clean namespace for eyelib-util module.

### Anti-Pattern 2: Circular Dependencies
**What:** eyelib-util importing from root or any submodule.
**Why bad:** Defeats the purpose of extraction; creates build cycle.
**Instead:** All types needed by eyelib-util come from MC/Forge or external libraries.

### Anti-Pattern 3: Partial Migration
**What:** Moving some util files but leaving others in root.
**Why bad:** Creates confusion about where to find/utilize utility code; two sources of truth.
**Instead:** All-or-nothing: every root/util/* file moves; root/util/* directory is deleted.

### Anti-Pattern 4: API Without Contract
**What:** Exposing all eyelib-util classes as public API without documenting which are internal vs stable.
**Why bad:** Callers outside the project could depend on unstable internal helpers.
**Instead:** Default internal bias; document any intentionally stable public utility APIs in README.

---

## Util File Inventory & Classification

### Category: Platform-Free (no MC/Forge/DFU imports)
**Destination:** eyelib-util, can be used by any consumer.

| File | Dependencies | Consumers (root) |
|---|---|---|
| `Blackboard.java` | java.util.* | None found (utility class) |
| `Collectors.java` | java.util.stream.* | None found directly |
| `EntryStreams.java` | java.util.* | `client/model/ModelPartModel.java` |
| `Lists.java` | java.util.* | Internal |
| `ListHelper.java` | core/util/collection/ListAccessors | `client/animation/bedrock/BrBoneKeyFrame.java` |
| `SimpleTimer.java` | System.currentTimeMillis | None found directly |
| `Tuple.java` | Java records | Internal (TupleCodec) |
| `Curves.java` | org.joml.Vector3f | `client/animation/bedrock/BrBoneKeyFrame.java` |
| `EyeMath.java` | org.joml.Vector3f, java.lang.Math | `client/animation/bedrock/BrClipExecutor.java`, `BrBoneAnimation.java`, `BrBoneKeyFrame.java`, `BrBoneAnimationSampler.java`, `client/render/visitor/ModelVisitor.java`, `client/gui/manager/AnimationView.java` |
| `MathHelper.java` | java.lang.Math | `client/animation/bedrock/BrClipExecutor.java`, `client/gui/manager/EyelibManagerScreen.java` |
| `FastColorHelper.java` | core/util/color/ColorEncodings | Internal |
| `AnimationApplier.java` | com.mojang.datafixers.util.Either (DFU) | None found |
| `Models.java` | None external | None found |
| `TexturePathHelper.java` | core/util/texture/TexturePaths | `client/render/RenderParams.java` |
| `Searchable.java` | Interface only | `client/loader/BrAttachableLoader.java` |
| `SearchResults.java` | java.util.* | None found directly |
| `BBModelSink.java` | Interface only | Bridge |
| `ModBridgeServer.java` | com.google.gson.*, java.net.* | Bridge |
| `SharedLibraryLoader.java` | java.io.*, java.util.zip.* | None found directly |

### Category: DFU-Dependent (com.mojang.serialization.*)
**Destination:** eyelib-util (DFU is already a dependency of eyelib-util)

| File | Dependencies | Consumers (root) |
|---|---|---|
| `ImmutableFloatTreeMap.java` | com.mojang.serialization.Codec | `client/animation/bedrock/BrBoneAnimation.java`, `BrBoneAnimationSampler.java`, `BrBoneAnimationDefinition.java`, `BrAnimationChannel.java`, `client/animation/AnimationChannelDefinition.java` |
| `ChinExtraCodecs.java` | com.mojang.serialization.Codec, DataResult, MapCodec | `client/animation/bedrock/BrBoneKeyFrame.java` |
| `CodecHelper.java` | com.mojang.datafixers.util.Either, com.mojang.serialization.Codec | `client/animation/bedrock/BrBoneAnimation.java`, `BrBoneKeyFrame.java`, `client/animation/bedrock/controller/BrAnimationControllers.java` |
| `DispatchedMapCodec.java` | com.mojang.serialization.* | None found directly |
| `EitherHelper.java` | com.mojang.datafixers.util.Either, core/util/codec/Eithers | Internal |
| `KeyDispatchMapCodec.java` | com.mojang.serialization.* | `common/behavior/component/group/ComponentGroup.java` |
| `TupleCodec.java` | com.mojang.serialization.* | Internal |

### Category: MC + DFU Dependent
**Destination:** eyelib-util (MC/Forge are allowed dependencies)

| File | Dependencies | Consumers (root) |
|---|---|---|
| `EyelibCodec.java` | net.minecraft.util.ExtraCodecs, net.minecraft.world.phys.AABB, com.mojang.serialization.* | `common/behavior/event/logic/LogicNode.java`, `common/behavior/event/filter/ComplexFilter.java` |
| `ResourceLocations.java` | net.minecraft.resources.ResourceLocation | `client/animation/bedrock/BrAnimationEntryDefinition.java`, `mc/impl/molang/mapping/MolangQuery.java`, `client/render/sync/ClientRenderSyncService.java` |
| `Shapes.java` | net.minecraft.util.RandomSource | None found directly (utility class) |

### Category: core/util/* — Platform-Free (merge into eyelib-util)

| File | Consumers in root |
|---|---|
| `collection/ListAccessors.java` | `util/ListHelper.java` (internal chain) |
| `texture/TexturePaths.java` | `util/client/texture/TexturePathHelper.java`, `client/render/controller/RenderControllerEntry.java` |
| `color/ColorEncodings.java` | `util/math/FastColorHelper.java`, `client/render/texture/NativeImageIO.java` |
| `codec/Eithers.java` | `util/codec/EitherHelper.java` (internal chain) |
| `time/FixedStepTimerState.java` | None found directly |

---

## Submodule Shared Code Centralization Candidates

### eyelib-attachment → eyelib-util: StreamCodec Suite
**Files:** `StreamCodec.java`, `StreamEncoder.java`, `StreamDecoder.java`, `EyelibStreamCodecs.java`
**Why centralize:** StreamCodec is a generic `FriendlyByteBuf` serialization abstraction. It is currently siloed in the attachment module but is needed by any module that sends/receives network data (particle packets, render sync, etc.). Centralizing it into eyelib-util makes it available to all modules without forcing them to depend on attachment.
**Dependencies:** `net.minecraft.network.FriendlyByteBuf`, `net.minecraft.nbt.*`, `net.minecraft.resources.ResourceLocation`, `com.mojang.serialization.Codec`, `com.mojang.logging.LogUtils`, `org.joml.Vector3f`
**Impact:** Attachment adds `implementation project(':eyelib-util')` and updates its own StreamCodec imports to use eyelib-util. Root's attachment network processing also updates imports.

### Other Submodules: No Identified Candidates
eyelib-importer, eyelib-material, eyelib-molang, eyelib-particle, and eyelib-processor have no siloed utility code that other modules need. Their current architecture is clean — they define domain-specific types consumed through their own module boundaries. No forced centralization needed.

---

## Scalability Considerations

| Concern | At Project Scale (7 modules) | Future (15+ modules) |
|---|---|---|
| Import churn | 38 root import changes (manageable) | N/A — root split reduces future import churn |
| eyelib-util build time | Instant (leaf, no project deps) | Grows with util code volume, not module count |
| eyelib-util scope creep | Controlled by single-consumer rule (D-6) | Single-consumer code must move to functional owner, NOT stay in eyelib-util |
| Identity test coverage | 1 test in eyelib-util + existing tests in attachment/importer | Each future module gets identity test verifying no new reverse deps |
| Cross-module dependency web | eyelib-util is leaf, all modules depend on it | eyelib-util stays leaf; no module depends on another through eyelib-util |

---

## Verification Strategy

### Build Gates
| Gate | Command | Expected |
|---|---|---|
| eyelib-util solo build | `jetbrain_build_project` scoped to `eyelib-util/` | exit code 0 |
| Full project build after migration | `jetbrain_build_project` (full) | exit code 0, no errors |
| NullAway on root | `jetbrain_run_gradle_tasks` task `:nullawayMain` | exit code 0 |
| All tests | `jetbrain_run_gradle_tasks` task `test` | exit code 0 |

### Code Quality Gates
| Gate | Verification |
|---|---|
| No residual util imports | `grep -r "import io.github.tt432.eyelib.util\." src/main/java/` returns zero (except within eyelib-util itself during migration) |
| No residual core/util imports | `grep -r "import io.github.tt432.eyelib.core.util\." src/main/java/` returns zero |
| root/util/* empty | `glob("src/main/java/io/github/tt432/eyelib/util/**/*.java")` returns empty |
| core/util/* empty | `glob("src/main/java/io/github/tt432/eyelib/core/util/**/*.java")` returns empty |
| eyelib-util no root imports | Identity test passes: zero files contain `import io.github.tt432.eyelib.` |
| Submodule boundary intact | Existing identity tests (attachment/ImporterModuleIdentityTest, particle/ParticleDefinitionBoundaryTest) continue to pass |
| jarJar packaging | Root jar contains eyelib-util classes (verified in JAR manifest) |

---

## Sources

| Source | Type | Confidence |
|---|---|---|
| Root build.gradle (353 lines, analyzed in full) | Project code | HIGH |
| settings.gradle (6 includes + clientsmoke) | Project code | HIGH |
| All 6 submodule build.gradle (analyzed for existing dependencies) | Project code | HIGH |
| grep across all submodules for `import io.github.tt432.eyelib.util.` — zero results | Verification | HIGH |
| grep across root `src/main/java/` for `import io.github.tt432.eyelib.util.` — 32 results | Verification | HIGH |
| grep across root `src/main/java/` for `import io.github.tt432.eyelib.core.util.` — 6 results | Verification | HIGH |
| Attachment identity test: `AttachmentModuleIdentityTest.java` | Code enforcement | HIGH |
| Importer identity test: `ImporterModuleIdentityTest.java` | Code enforcement | HIGH |
| Particle boundary test: `ParticleDefinitionBoundaryTest.java` | Code enforcement | HIGH |
| PROJECT.md v1.3 milestone definition | Requirements | HIGH |
| MODULES.md full inventory | Documentation | HIGH |
| 01-module-boundaries.md (137 lines) | Architecture docs | HIGH |
| ARCHITECTURE-BLUEPRINT.md (102 lines) | Architecture docs | HIGH |
| All 34 util Java files (analyzed for MC/DFU dependencies) | Project code | HIGH |
| All 5 core/util Java files (analyzed for purity) | Project code | HIGH |
| Attachment StreamCodec suite (4 files, analyzed) | Project code | HIGH |

---

## Open Questions

1. **StreamCodec dependency chain:** If StreamCodec moves to eyelib-util, does attachment still need to own `DataAttachmentSyncPacket` and `UpdateDestroyInfoPacket` directly, or should they also use the centralized StreamCodec? These packets currently live in the attachment subproject and use attachment's own StreamCodec — moving StreamCodec means attachment's own packet codecs would import from eyelib-util.

2. **jarJar strategy:** Root currently uses `jarJar project(':eyelib-*')` for all submodules. Should eyelib-util follow the same pattern (jarJar for runtime bundling) or be a compile-time-only dependency? The PROJECT.md mentions "May depend on MC/Forge" but doesn't specify jarJar vs implementation.

3. **`ImmutableFloatTreeMap` scope:** This is the single most-imported util class (5 animation files + 2 definition files). It depends on `com.mojang.serialization.Codec`. Should it stay in eyelib-util or move closer to the animated-values domain? Current analysis suggests eyelib-util is appropriate since its sole dependency (DFU) is already a shared dependency.

4. **`Searchable` interface:** This is a single-method interface used only by `BrAttachableLoader`. Per the single-consumer rule, it could move to the loader's functional owner instead of eyelib-util. Verdict: keep in eyelib-util since it's a general-purpose search contract that other loaders may adopt.
