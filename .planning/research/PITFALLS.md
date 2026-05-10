# Domain Pitfalls

**Domain:** Forge Multi-Module Util Code Extraction
**Researched:** 2026-05-10
**Confidence:** HIGH

---

## Executive Summary

Extracting shared utility code from a root module into a dedicated `:eyelib-util` Forge Gradle subproject presents four classes of risk: **compile-time** failures from import-fixup lag and package shadowing; **runtime** errors from classloader changes and mods.toml ID collision; **dependency** cycles introduced by cross-module relocation; and **ownership ambiguity** when single-consumer code lands in the wrong destination.

The v1.2 particle extraction taught hard lessons: package name collisions between root and new module silently break imports, wildcard imports (`import ...util.codec.*`) resolve to the wrong package after relocation, and duplicate utility classes across submodules (`DispatchedMapCodec` in both root and eyelib-material) create post-extraction drift. The v1.3 util extraction inherits these risks and adds new ones: eyelib-util's Forge module identity (mods.toml), MC-dependent vs pure-Java code mixing, and the obligation to leave `root/util/*` completely empty.

**Key insight:** Util extraction is an import-rewiring exercise whose risk is proportional to the number of import sites changed. With 34 files in `root/util/*` referenced by dozens of consumers across root and submodules, phased, verified migration is the only safe approach.

---

## PITFALL CATEGORY 1: Compile-Time Risks

Mistakes that cause compilation failure, silent import resolution to the wrong class, or build breakage during or after migration.

### Pitfall C1: Package Shadowing — Old Source Coexisting with New Module Source

**What goes wrong:** After creating `:eyelib-util` with classes under `io.github.tt432.eyelibutil.*` (following the existing submodule naming convention), the old `io.github.tt432.eyelib.util.*` source files are not deleted from root simultaneously. Both packages coexist on the classpath during the transition. Import statements in root code may resolve to the old (root) or new (eyelib-util) location depending on classpath ordering, producing ambiguous resolution errors or silent compilation against stale code.

**Why it happens:** Migration executed as "add new module first, then gradually delete old sources" rather than atomic move-and-delete. The root module and `:eyelib-util` both appear in the Gradle build, and Gradle's classpath resolution does not guarantee which source wins.

**Consequences:** 
- `javac` error: "reference to X is ambiguous, both in module root and module eyelibutil"
- IDE code intelligence shows stale definitions, leading developers to edit the wrong copy
- Runtime behavior differs from compile-time behavior (old code ran, new code not properly wired)

**Prevention in v1.3:**
- **Atomic file operations:** For each util file being moved, in a single commit: (1) copy the file to `eyelib-util/src/main/java/`, updating the package declaration; (2) delete the file from `root/src/main/java/`; (3) update all import sites. Never leave both copies in the tree.
- **per-phase verification:** After each phase's file moves, run `jetbrain_build_project` and check for zero errors with `ide_diagnostics`.
- **Mechanical script rule:** A bash script that, for file X, confirms the old path no longer exists before proceeding to the next file.

**Detection:** Grep for `io.github.tt432.eyelib.util.` in root source after deletions. Any remaining import is a stale reference or a missed file.

**Phase mapping:** Phases 2–4 (file movement phases)

---

### Pitfall C2: Import Wildcard Collapse

**What goes wrong:** Code in root uses wildcard imports like `import io.github.tt432.eyelib.util.codec.*;` (found in `BrAnimationEntry.java`). After codec classes move to `io.github.tt432.eyelibutil.codec`, the wildcard import still resolves the old package path, now empty or containing only compatibility facades. Imports fail silently or resolve to wrong classes if facades remain.

**Why it happens:** Wildcard imports commit to a specific package name. Package rename during extraction breaks all wildcard resolution.

**Consequences:** 
- Missing imports at compile time
- If compatibility facades are left as passthrough, wildcard still works — but facade classes become long-term maintenance debt
- `BrAnimationEntry.java` currently uses `import io.github.tt432.eyelib.util.codec.*;` — this single file breaks at least 5-10 individual class references

**Prevention:**
- **Phase 0 pre-migration audit:** Scan all source files for `import io.github.tt432.eyelib.util.*` and `import io.github.tt432.eyelib.util.codec.*` and replace with explicit imports BEFORE any file movement begins.
- **Automated pre-check:** Add a verification step to the first phase that fails if wildcard imports to any `eyelib.util` subpackage exist.
- **Verified files:** `BrAnimationEntry.java` (line contains wildcard), verify no others exist in root or submodules.

**Detection:** `jetbrain_search_in_files_by_regex` with regex `import io\.github\.tt432\.eyelib\.util\.\w+\.\*` in all `*.java` files.

**Phase mapping:** Phase 1 (pre-migration audit, before file movement)

---

### Pitfall C3: Intra-Util Self-Reference Breaking After Split

**What goes wrong:** `ImmutableFloatTreeMap` (in `root/util/`) imports `io.github.tt432.eyelib.util.codec.CodecHelper` and `io.github.tt432.eyelib.util.codec.DispatchMapCodec`. If these codec classes move to eyelib-util in a different phase than `ImmutableFloatTreeMap`, the internal dependency breaks. Worse, if codec classes end up in a different package prefix (`eyelibutil.codec` vs `eyelib.util.codec`), the import chain temporarily or permanently breaks.

**Why it happens:** The `util/` family has internal coupling — codec helpers are consumed by util-level classes like `ImmutableFloatTreeMap`. Phased migration that separates co-dependent files causes intermediate breakage.

**Consequences:** 
- Compilation failure in intermediate phases
- Friction if phases are split across codec vs. data-structure utility families

**Prevention:**
- **Atomic codec family migration:** All classes in `root/util/codec/` and `ImmutableFloatTreeMap` (which depends on them) must move in the SAME phase. Identify transitive intra-util dependencies before planning phase boundaries.
- **Dependency graph per phase:** For each move candidate, run `ide_ide_find_references` within the `util/` subtree. If A references B and both are in root/util, they must move together.

**Detection:** Parse all `.java` files in `root/util/` for `import io.github.tt432.eyelib.util.*` references. Any self-references within root/util must be tracked.

**Intra-util dependencies discovered:**
| File | Depends on (in util) |
|------|---------------------|
| `ImmutableFloatTreeMap.java` | `util.codec.CodecHelper` |
| `util.codec.*` (all) | `util.codec.Tuple`, `util.codec.EitherHelper`, `util.codec.ChinExtraCodecs` |

All 9 codec files + `ImmutableFloatTreeMap` form an atomic migration unit.

**Phase mapping:** Phase 2 (codec family must move as one unit)

---

### Pitfall C4: Import-Heavy File Churn Exceeding IDE Auto-Fix Capacity

**What goes wrong:** 34 files in `root/util/*` are referenced by dozens of import sites across root (`client/animation/`, `client/loader/`, `common/`, `capability/`, `client/render/` etc.) and submodules. Manually fixing each import risks human error (wrong package name, missed file, stale import). IDE auto-refactor tools (`ide_refactor_move_file`) may not handle the cross-module + package-rename combination safely.

**Why it happens:** The v1.2 particle extraction faced similar import-hell. The root module has ~60+ import sites referencing `io.github.tt432.eyelib.util.*` classes. Each must change to `io.github.tt432.eyelibutil.*`.

**Consequences:** 
- Missed import sites → compile errors
- Incorrect package names (typos like `eyelibutil` vs `eyelib-util` in package) → permanent debt
- Manual rename taking hours with high error rate

**Prevention:**
- **IDE refactoring per file:** For each util file, use `ide_ide_find_references` to discover ALL import sites, then use `ide_ide_refactor_move_file` to the new module directory after package rename.
- **Package rename before move:** First rename the package declaration in the source file (edit), then move the file to the new module directory using IDE tools. IDE handles import-fixup automatically.
- **Phase grouping:** Group files by consumer domain (codec family → one phase, math family → one phase, search/modbridge → one phase) so each phase is a tractable set of import fixes.
- **Post-phase verification:** After each phase, `jetbrain_build_project` + `ide_diagnostics` to catch any missed imports.

**Phase mapping:** Phases 2, 3, 4 (each phase = one util domain family)

---

### Pitfall C5: EyelibCodec MC Dependency — Unexpected Incompatibility

**What goes wrong:** `EyelibCodec.java` imports `net.minecraft.util.ExtraCodecs` and `net.minecraft.world.phys.AABB`. If `:eyelib-util` is created but the Gradle dependency configuration does not include the Minecraft dependency that provides these classes, `EyelibCodec` fails to compile in the new module.

**Why it happens:** Assumption that "most codec code is pure Mojang DataFixerUpper" — but `EyelibCodec` specifically depends on MC `ExtraCodecs.VECTOR3F` and `AABB`. MC classes are only available when the Forge/MDGL plugin provides the Minecraft dependency.

**Consequences:** 
- Compile failure in eyelib-util for any file that uses `EyelibCodec`
- Surprise: a "codec" file is not datafixerupper-only

**Prevention:**
- **Forge module declaration confirmed:** eyelib-util MUST use the `net.neoforged.moddev.legacyforge` plugin (like all other modules) to get MC/Forge classpath.
- **Pre-move audit:** For each file in `root/util/*`, run `jetbrain_search_in_files_by_regex` for `import net\.minecraft` and `import net\.minecraftforge`. Any file with MC/Forge imports is identified as requiring the Forge module dependency. Currently: `ResourceLocations.java` (ResourceLocation), `EyelibCodec.java` (ExtraCodecs, AABB), `Shapes.java` (RandomSource) — 3 files requiring MC.
- **Design decision in STACK.md:** eyelib-util is declared as a Forge module (per Key Decision in PROJECT.md), so MC dependencies are valid. But files without MC imports remain pure Java.

**Phase mapping:** Phase 1 (module skeleton creation, Gradle config)

---

## PITFALL CATEGORY 2: Runtime Risks

Mistakes that cause runtime failures after successful compilation, including classloading, Forge lifecycle, and mod identity issues.

### Pitfall R1: mods.toml Identity Collision

**What goes wrong:** `:eyelib-util` gets a `mods.toml` with `modId = "eyelib"` (accidentally matching root) or reusing an existing submodule ID. Forge discovers two `@Mod` annotations with the same ID during scan and rejects the mod at startup.

**Why it happens:** The root module's modId is `eyelib`. Submodules use unique IDs: `eyelibattachment`, `eyelibimporter`, `eyelibmaterial`, `eyelibmolang`, `eyelibparticle`, `eyelibprocessor`. The new module must follow this pattern with `eyelibutil` (no hyphen — consistent with existing naming). Copy-paste from another module's mods.toml without changing the ID creates collision.

**Consequences:** 
- Forge startup crash: "Duplicate mod ID" or "Mod ID already registered"
- Error only detectable at runtime, not at compile time

**Prevention:**
- **Template with guard:** Create `mods.toml` from scratch using `eyelibutil` as `modId`, not by copying another module's file.
- **Unique ID verification:** Cross-reference all existing `mods.toml` files:
  - root: `eyelib`
  - eyelib-attachment: `eyelibattachment`
  - eyelib-importer: `eyelibimporter`
  - eyelib-material: `eyelibmaterial`
  - eyelib-molang: `eyelibmolang`
  - eyelib-particle: `eyelibparticle`
  - eyelib-processor: `eyelibprocessor`
  - eyelib-util (new): `eyelibutil`
- **Mod annotation check:** If eyelib-util does NOT register a `@Mod` class (it's purely a library, no Forge bootstrap), the mods.toml `modId` only needs to not conflict. But if it has a `@Mod` entrypoint, the modId must be globally unique across ALL Forge mods in the environment.

**Detection:** Grep all `mods.toml` files for `modId` values. Ensure `eyelibutil` is unique. Note: eyelib-util is a library module — it may NOT need `@Mod` or `mods.toml` at all, if it has no Forge lifecycle. Per PROJECT.md Key Decision: "eyelib-util as Forge module: May depend on MC/Forge." But dependency on MC/Forge ≠ needing a `@Mod` annotation. The build.gradle must use the legacyForge plugin, but a `mods.toml` may be optional if no `@Mod` class exists.

**Phase mapping:** Phase 1 (module skeleton creation)

---

### Pitfall R2: ResourceLocations — MOD_ID Circular Dependency

**What goes wrong:** `ResourceLocations.mod(String path)` calls `new ResourceLocation(Eyelib.MOD_ID, path)`. `Eyelib.MOD_ID` is defined in root (`Eyelib.java`). If `ResourceLocations.java` moves to `:eyelib-util`, and eyelib-util cannot depend on root (circular dependency violation), the `mod()` method fails to compile.

**Why it happens:** The `mod()` convenience method embeds a reference to the root module constant. Moving `ResourceLocations` to a submodule breaks this reference because submodules cannot depend upward on root.

**Consequences:** 
- Compile error in `ResourceLocations.mod()`
- Either the method is deleted (breaking callers) or the MOD_ID must be duplicated (DRY violation)

**Prevention options (ranked):**
1. **Remove `mod()` method, keep callers working:** Delete the `mod()` convenience method and make callers use `ResourceLocations.of(Eyelib.MOD_ID, path)` — the MOD_ID stays in root, ResourceLocations only provides the `of()` factory.
2. **Parameterize:** Change `mod(String path)` to `mod(String modId, String path)`, making callers supply MOD_ID.
3. **Move MOD_ID to eyelib-util:** Extract `Eyelib.MOD_ID` constant to eyelib-util. But this changes the package of a legacy constant holder, potentially breaking other reference sites.

**Recommendation:** Option 1 (remove `mod()` method). 4 call sites currently use `ResourceLocations`: `BrAnimationEntryDefinition`, `MolangQuery`, `ClientRenderSyncService`, and `RenderSyncApplyOpsTest`. Audit whether any of them specifically use `ResourceLocations.mod()`. If none do after source analysis, deletion is safe.

**Detection:** Run `ide_ide_find_references` on `ResourceLocations.mod()` to confirm call sites. If zero callers → safe deletion. If callers exist → rewrite them before moving.

**Phase mapping:** Phase 2 (ResourceLocations migration)

---

### Pitfall R3: SharedLibraryLoader Classpath Change

**What goes wrong:** `SharedLibraryLoader` uses `getResourceAsStream("/" + path)` and `new ZipFile(nativesJar)` to load native libraries from JAR classpath. After moving to `:eyelib-util`, the classloader that loads SharedLibraryLoader belongs to the `eyelib-util` JAR, not the root Eyelib JAR. The `getResourceAsStream` path resolves relative to the eyelib-util JAR, potentially failing to find embedded native libraries.

**Why it happens:** Classpath-relative resource loading depends on which JAR the class belongs to. After module extraction, the class moves to a different JAR with a different resource root.

**Consequences:** 
- `RuntimeException: "Unable to read file for extraction: ..."` at startup
- Native library extraction fails quietly, downstream native-dependent features break

**Prevention:**
- **Audit resource dependencies:** Check if `SharedLibraryLoader` is currently used by root to load natives from the root JAR. If so, the natives must either stay in root (with a compatibility bridge) or move to eyelib-util alongside SharedLibraryLoader.
- **Verification test:** After migration, run a test that invokes `SharedLibraryLoader.load()` with a known native and verifies success.
- **Risk assessment:** SharedLibraryLoader is a libGDX-origin utility. It may only be used for loading specific native DLLs that ship in the root JAR. If true, keeping it in eyelib-util requires repackaging natives. If the native loading is unused in practice, consider removing SharedLibraryLoader entirely.

**Detection:** Run `ide_ide_find_references` on `SharedLibraryLoader` to find all callers. If zero callers → safe to delete. If callers exist → test the native path after migration.

**Phase mapping:** Phase 3 (after initial code movement, dedicated verification)

---

### Pitfall R4: ModBridgeServer Lifecycle Timing

**What goes wrong:** `ModBridgeServer` starts a TCP server using `new ServerSocket(port)` in `start()`. If this server's lifecycle was previously controlled by root Forge events (e.g., started during `FMLCommonSetupEvent`), after moving to eyelib-util, the startup trigger remains in root but the server class is in a different module. Classloading order or event timing may cause `start()` to be called before the class is loaded.

**Why it happens:** Module classloading in Forge multi-module builds is not deterministic by default. The eyelib-util module may load after root attempts to call `ModBridgeServer.start()`.

**Consequences:** 
- `NoClassDefFoundError` or `ClassNotFoundException` at startup
- Silent failure if timing-dependent

**Prevention:**
- **Dependency declaration ensures load order:** If root depends on `:eyelib-util` via `modImplementation project(':eyelib-util')`, the eyelib-util classes are guaranteed to be on the classpath when root initializes.
- **Lazy initialization:** If `ModBridgeServer.start()` is called from a `@SubscribeEvent` handler in root, the class is loaded by the time Forge fires the event.
- **Verification:** After migration, run a full Forge client startup and verify no ClassNotFound errors for eyelib-util classes.

**Detection:** Search for `ModBridgeServer` in root event handlers. Confirm it's loaded through a Forge event, not a static initializer.

**Phase mapping:** Phase 4 (runtime verification)

---

## PITFALL CATEGORY 3: Dependency Risks

Mistakes in Gradle dependency configuration that create circular dependencies, wrong scope choices, or transitive leakage.

### Pitfall D1: Circular Dependency Through EyelibCodec

**What goes wrong:** Root depends on `:eyelib-util` for codec utilities. But `EyelibCodec.list()` returns a `MapCodec` that root animation code (`BrAnimationEntry`, `BrBoneKeyFrame`) uses with domain-specific codec registries provided by root. If the domain-specific registries reference classes in eyelib-util AND eyelib-util references classes in root, a cycle forms.

**Why it happens:** `EyelibCodec.list(Supplier<Map<String, CodecInfo<? extends S>>> codecs)` is a generic codec factory. Root callers pass root-owned CodecInfo suppliers. The signature itself is generic and doesn't introduce a cycle — but if eyelib-util were to import root classes for any reason, the cycle would manifest.

**Consequences:** 
- Gradle build failure: "Circular dependency detected"
- Cannot build eyelib-util independently

**Prevention:**
- **Dependency direction enforcement:** eyelib-util MUST have zero `project(':eyelib-*')` or root dependencies in its `build.gradle`. It depends only on MC/Forge/Mojang DataFixerUpper/JOML.
- **Dependency audit:** After each phase, run `jetbrain_get_project_dependencies` on eyelib-util and verify zero project dependencies.
- **Generic code pattern:** Codec factories in eyelib-util must accept generic types (Supplier, Function, Codec) and never reference concrete root domain types.

**Detection:** In eyelib-util `build.gradle`, confirm `dependencies { }` block contains no `project(...)` entries for eyelib submodules.

**Phase mapping:** Phase 1 (Gradle config), verified in every subsequent phase

---

### Pitfall D2: Duplicate DispatchedMapCodec — Source of Truth Ambiguity

**What goes wrong:** `DispatchedMapCodec` exists in TWO locations:
1. `root/src/main/java/io/github/tt432/eyelib/util/codec/DispatchedMapCodec.java`
2. `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/util/codec/DispatchedMapCodec.java`

Both are the same concept (dispatched map codec) with slightly different implementations. After extracting to `eyelib-util`, only ONE copy should exist. But if the consumer migration for the eyelib-material version is missed, two implementations drift independently.

**Why it happens:** During v1.2 particle extraction, eyelib-material was created as an independent module and duplicated utility code rather than depending on root util. This is exactly the pattern v1.3 aims to fix.

**Consequences:** 
- Two implementations with different behavior → subtle bugs
- Maintenance burden doubled
- eyelib-material continues to use its own copy after centralization, defeating the purpose

**Prevention:**
- **Post-centralization verification:** After moving `DispatchedMapCodec` to eyelib-util, run `ide_ide_find_references` on both the new AND old locations. The old eyelib-material copy's usages should be zero or replaced.
- **Delete atomically:** When the eyelib-util version is introduced, ADD the `project(':eyelib-util')` dependency to eyelib-material, UPDATE its imports, and DELETE the eyelib-material copy in the same commit.
- **Duplicate detection:** Before starting, scan ALL submodules for `DispatchedMapCodec`, `CodecHelper`, `KeyDispatchMapCodec`, `ChinExtraCodecs`, `TupleCodec`, `EyelibCodec` — any codec class name that might be duplicated.

**Duplicate candidates discovered:**
| Class | Root Location | Duplicate Location | Action |
|-------|---------------|-------------------|--------|
| `DispatchedMapCodec` | `root/util/codec/` | `eyelib-material/util/codec/` | Centralize to eyelib-util, delete eyelib-material copy |
| `KeyDispatchMapCodec` | `root/util/codec/` | Only in root | Move to eyelib-util |
| `StreamCodec` family | `eyelib-attachment/codec/stream/` | Only in attachment | Centralize to eyelib-util |

**Phase mapping:** Phase 3 (submodule shared code discovery and centralization)

---

### Pitfall D3: Attachment StreamCodec Relocation — Dependency Chain

**What goes wrong:** `EyelibStreamCodecs` and `StreamCodec` live in `:eyelib-attachment`. They are MC-dependent (FriendlyByteBuf, ResourceLocation, NBT) but are generic stream codec utilities, not attachment-specific logic. If they move to `:eyelib-util`, eyelib-attachment must add a dependency on `:eyelib-util`. But eyelib-util must NOT depend on eyelib-attachment.

**Current dependency chain:**
- root → `:eyelib-attachment`
- root → `:eyelib-molang`
- root → `:eyelib-importer`
- `:eyelib-particle` → `:eyelib-importer`, `:eyelib-molang`, `:eyelib-material`
- All submodules are independent of each other (except particle → importer/molang/material)

**After centralization:**
- `:eyelib-util` depends on nothing (MC/Forge only)
- root → `:eyelib-util`, `:eyelib-attachment`, etc.
- `:eyelib-attachment` → `:eyelib-util` (for stream codecs)
- `:eyelib-material` → `:eyelib-util` (for DispatchedMapCodec)

**Why it happens:** Failure to plan the dependency graph before moving code. Adding project dependencies without checking for cycles.

**Consequences:** 
- Circular dependency if eyelib-util depends back on any submodule
- Gradle build failure

**Prevention:**
- **Dependency graph diagram:** Before starting any code movement, draw the dependency graph and confirm eyelib-util is a LEAF node (nothing depends on it upward, it depends on none sideways).
- **Incremental addition:** Add `project(':eyelib-util')` dependency to ONE submodule at a time, verify build after each addition.
- **No back-reference rule:** eyelib-util's `build.gradle` dependencies block MUST NOT contain any `project(...)` entries. This is a hard gate checked by automated audit.

**Target dependency graph after v1.3:**
```
eyelib-util (leaf — no project deps)
    ↑
    ├── root (was: direct util access; now: project(':eyelib-util'))
    ├── eyelib-attachment (now: project(':eyelib-util') for streamcodecs)
    ├── eyelib-material (now: project(':eyelib-util') for dispatched map codec)
    ├── eyelib-molang (possibly: project(':eyelib-util') if any util code used)
    ├── eyelib-importer (possibly: project(':eyelib-util') if any util code used)
    ├── eyelib-particle (possibly: project(':eyelib-util') if any util code used)
    └── eyelib-processor (possibly: project(':eyelib-util') if any util code used)
```

**Phase mapping:** Phase 1 (dependency planning), Phase 3 (attachment/material dependency rewiring)

---

### Pitfall D4: Gradle Dependency Scope Mismatch

**What goes wrong:** Submodules use `implementation project(':eyelib-util')` but eyelib-util types appear in their public API signatures (method return types, constructor parameters). Gradle's `implementation` scope hides these types from transitive consumers, causing compile failures in downstream consumers of the submodule.

**Why it happens:** Defaulting to `implementation` because "it's just a utility library." But if a submodule's public API exposes `ImmutableFloatTreeMap` or `EyelibCodec` types in its interface, downstream consumers cannot see those types.

**Consequences:** 
- Root module compiles against `:eyelib-particle`, which exposes `ImmutableFloatTreeMap` in its public API, but `:eyelib-particle` uses `implementation project(':eyelib-util')` → root cannot resolve `ImmutableFloatTreeMap`

**Prevention:**
- **Scope audit rule:** For each submodule adding `:eyelib-util` dependency:
  - Use `api project(':eyelib-util')` IF and ONLY IF eyelib-util types appear in the submodule's public API (return types, parameter types, thrown exceptions, superclass, implemented interfaces).
  - Use `implementation project(':eyelib-util')` if eyelib-util types are only used internally.
- **Root module special case:** Root consumed `util` code internally; after extraction, root → `:eyelib-util` should almost always be `implementation` because root is the top-level assembly module.
- **Verification:** After configuring dependencies, build the full project and confirm no "cannot find symbol" errors for eyelibutil types in downstream modules.

**Phase mapping:** Phase 4 (submodule integration phase)

---

## PITFALL CATEGORY 4: Ownership Ambiguity Risks

Mistakes in deciding WHAT goes to eyelib-util vs. functional owners vs. stays in root, causing maintenance confusion and degradation of the module boundary.

### Pitfall O1: Single-Consumer Utility Ends Up in eyelib-util

**What goes wrong:** `AnimationApplier` and `Models` in `util/client/` are used ONLY by the client animation domain (`BrBoneAnimation`, `BrBoneKeyFrame`). They have exactly one functional consumer. Per the milestone rules, "Single-consumer utility code moved to its functional owner instead of staying in eyelib-util." But during migration, the laziest path is "move everything in root/util/* to eyelib-util" — violating the boundary rule.

**Why it happens:** Unfamiliarity with the consumer graph. Default assumption "if it's in util/, it must be shared." The `util/client/` directory name is misleading — some code there pre-dates the module extraction and was placed in util/ before the current boundary discipline existed.

**Consequences:** 
- eyelib-util becomes a dumping ground for animation-specific code, defeating the purpose
- Animation module cannot be understood independently
- Future animation refactoring must touch eyelib-util (violates separation of concerns)

**Prevention:**
- **Consumer count audit (Phase 1):** For every file in `root/util/*`, run `ide_ide_find_references`. If exactly ONE consuming module exists → move to that module, NOT eyelib-util.
- **Apply the rule mechanically:** The "one consumer" rule is not negotiable. If a file has N consumers:
  - N = 0: Delete it (unused code)
  - N = 1: Move to consumer's module (not eyelib-util)
  - N ≥ 2: Move to eyelib-util (truly shared)
- **Per-file ownership decisions:**

| File | Consumers | Decision |
|------|-----------|----------|
| `AnimationApplier.java` | Animation only (1 consumer) | → Move to `client/animation/` in root |
| `Models.java` | Animation loading only (1 consumer) | → Move to `client/animation/` in root |
| `ResourceLocations.java` | 4 consumers across root | → eyelib-util (after resolving MOD_ID) |
| `ImmutableFloatTreeMap.java` | 5 animation consumers | → eyelib-util |
| `EyelibCodec.java` | 10+ consumers across root and submodules | → eyelib-util |
| `Lists.java` | 0 consumers (verified) | → DELETE or eyelib-util if core/util equivalent needed |
| `Collectors.java` | 0 consumers | → DELETE or eyelib-util |
| `EntryStreams.java` | Needs audit | → TBD |
| `ListHelper.java` | Needs audit | → TBD |
| `SimpleTimer.java` | Needs audit | → TBD |
| `Blackboard.java` | 0 consumers | → DELETE or eyelib-util |
| `BBModelSink.java` | ModBridgeServer only (1) | → eyelib-util (it's part of ModBridgeServer's interface) |
| `ModBridgeServer.java` | Root GUI manager (1+) | → Needs consumer audit |
| `Shapes.java` | Particle module (1?) | → If 1 consumer, move to eyelib-particle. If 2+, eyelib-util. |

**Phase mapping:** Phase 1 (consumer audit), Phase 2 (per-file routing decisions)

---

### Pitfall O2: Core/Util Hybrid — The Two-Source Problem

**What goes wrong:** `core/util/*` is a separate subtree of platform-free helpers extracted during an earlier utility split wave (v1.1 era). `root/util/*` has compatibility adapters that wrap core/util functionality (e.g., `TexturePathHelper` wrapping `TexturePaths`). After both merge into eyelib-util, the adapter double-layer becomes permanent technical debt.

**Why it happens:** The core/util extraction was an incremental migration that left compatibility facades in root/util. If these facades are simply moved to eyelib-util alongside the core code, the dual-layer persists.

**Consequences:** 
- Two classes doing the same thing in the same module (e.g., eyelib-util has both `TexturePathHelper` and `TexturePaths`)
- Future developers confused about which to use
- Import sites reference the wrapper instead of the canonical implementation

**Prevention:**
- **Merge duplicates during extraction:** When core/util and root/util equivalents exist:
  1. Identify the canonical version (core/util is platform-free, that's the source of truth)
  2. AUDIT all callers of the root/util wrapper
  3. Rewrite callers to use the core version directly
  4. DELETE the root/util wrapper (do not move it to eyelib-util)
  5. Move the core version to eyelib-util
- **File pairs to merge:**
  | Core/Util Canonical | Root/Util Wrapper | Action |
  |---------------------|-------------------|--------|
  | `core/util/texture/TexturePaths.java` | `util/client/texture/TexturePathHelper.java` | Audit TexturePathHelper callers, redirect to TexturePaths, delete TexturePathHelper |
  | `core/util/collection/ListAccessors.java` | Possibly `util/Lists.java` or `util/ListHelper.java` | Needs audit; may be different interfaces |
  | `core/util/codec/Eithers.java` | `util/codec/EitherHelper.java` | Compare APIs; if equivalent, unify |
  | `core/util/time/FixedStepTimerState.java` | `util/SimpleTimer.java` | Different abstractions; audit consumer overlap |

**Phase mapping:** Phase 2 (during root/util → eyelib-util migration)

---

### Pitfall O3: Package Name Convention Violation

**What goes wrong:** Existing submodules use the naming pattern `io.github.tt432.eyelib<modulename>.*`:
- eyelib-particle → `io.github.tt432.eyelibparticle.*`
- eyelib-attachment → `io.github.tt432.eyelibattachment.*`
- eyelib-molang → `io.github.tt432.eyelibmolang.*`
- eyelib-material → `io.github.tt432.eyelibmaterial.*`
- eyelib-importer → `io.github.tt432.eyelibimporter.*`
- eyelib-processor → `io.github.tt432.eyelibprocessor.*`

If `:eyelib-util` uses a different pattern (e.g., `io.github.tt432.eyelib.util` or `io.github.tt432.eyelibutil.shared`), it breaks the convention and creates inconsistency.

**Why it happens:** The hyphen in the module name `eyelib-util` does not map cleanly to Java package names. Natural inclination might be `eyelibutil` (consistent) or `eyelib.util` (ambiguous, overlaps with root package).

**Consequences:** 
- Inconsistency with 6 existing modules
- IDE auto-import suggestions may conflict with root package if `eyelib.util` is reused
- Mental tax for developers navigating packages

**Prevention:**
- **Use `io.github.tt432.eyelibutil`** (no hyphen, no dot separator — consistent with ALL existing submodule naming).
- **Enforce with checkstyle or grep:** After Phase 1 module creation, verify ALL new source files in eyelib-util use `package io.github.tt432.eyelibutil.*`.
- **Sub-package mapping:**
  - `root/util/codec/` → `io.github.tt432.eyelibutil.codec`
  - `root/util/math/` → `io.github.tt432.eyelibutil.math`
  - `root/util/search/` → `io.github.tt432.eyelibutil.search`
  - `root/util/modbridge/` → `io.github.tt432.eyelibutil.modbridge`
  - `root/util/client/` → Not moving (single-consumer → functional owner)
  - `core/util/*` → `io.github.tt432.eyelibutil.core` (or flatten — core distinction is about MC-free vs MC-dep, not a submodule boundary)

**Phase mapping:** Phase 1 (package naming decision, module skeleton)

---

### Pitfall O4: Legacy Eyelib.java MOD_ID Constant Relocation

**What goes wrong:** `Eyelib.java` in root (`io.github.tt432.eyelib.Eyelib`) holds `MOD_ID = "eyelib"` used by `ResourceLocations.mod()`. If `ResourceLocations` moves to eyelib-util, and the `mod()` method is preserved, eyelib-util needs access to MOD_ID. Moving the entire `Eyelib` class to eyelib-util (or just MOD_ID) breaks root references to `Eyelib` itself.

**Why it happens:** `Eyelib` is referenced by root bootstrap, packet registration, and various other root-level code. It cannot simply move to a submodule.

**Consequences:** 
- Root code referencing `Eyelib.MOD_ID` breaks
- `Eyelib` is a legacy constant holder — moving it disrupts dozens of root call sites

**Prevention:**
- **Do NOT move `Eyelib.java`.** It stays in root.
- **Resolve the ResourceLocations.mod() issue (see Pitfall R2):** Either delete `mod()` or parameterize it to accept `modId` from caller.
- **Alternative:** Add `MOD_ID` as a duplicate constant in eyelib-util (worst option — DRY violation, versioning risk).
- **Recommended:** Delete `ResourceLocations.mod()` method. Check its 4 callers (`BrAnimationEntryDefinition`, `MolangQuery`, `ClientRenderSyncService`, `RenderSyncApplyOpsTest`) — audit whether any of them use `.mod()`. If not, safe deletion. If yes, rewrite callers to use `ResourceLocations.of(Eyelib.MOD_ID, path)`.

**Phase mapping:** Phase 2 (ResourceLocations migration)

---

## v1.2 Particle Extraction Lessons Applied to v1.3

The v1.2 particle extraction provides direct analogues for v1.3 util extraction. The following are lessons learned and how they apply:

| v1.2 Issue | How It Manifested | v1.3 Analogue | Prevention |
|-----------|-------------------|---------------|------------|
| Package name collision | Root and particle module both had `BrParticle`-named classes in different packages; confusing imports | Root util classes moving to eyelib-util with new package names; old imports break | Atomic move+delete per file, package rename verified |
| Wildcard import chaos | `import io.github.tt432.eyelib.client.particle.bedrock.*` broke when package moved | `import io.github.tt432.eyelib.util.codec.*` in BrAnimationEntry.java | Pre-migration wildcard audit (Pitfall C2) |
| Circular dependency risk | Phase 8 detected eyelib-particle must not depend back on root | eyelib-util must not depend on any submodule | Hard gate: zero-`project()` deps rule (Pitfall D1) |
| Duplicate code across modules | `BrParticle` existed in root (legacy) and particle module (new), two owners | `DispatchedMapCodec` in root and eyelib-material | Deduplicate during centralization (Pitfall D2) |
| Import hell | 60+ import sites changed across 7 phases | 34 files × many consumers = potentially 100+ import fixes | IDE refactor per file, grouped by domain family (Pitfall C4) |
| Boundary leakage | Root compatibility adapters survived as permanent facades | core/util wrappers in root/util could become permanent if moved to eyelib-util | Merge canonical + wrapper during extraction (Pitfall O2) |
| Verification granularity | Phase 14 verification gate caught residual issues | Each phase in v1.3 should have its own verification gate | Per-phase `jetbrain_build_project` + diagnostics (all pitfalls) |

---

## Phase-Specific Checklists

### Phase 1: Module Skeleton & Audit

- [ ] Create `eyelib-util/build.gradle` with `legacyForge` plugin, java-library, no `project()` deps
- [ ] Confirm package prefix `io.github.tt432.eyelibutil` — no dot after eyelib, no hyphen
- [ ] Create `mods.toml` with `modId = "eyelibutil"` (verify no collision with 7 existing modIds)
- [ ] Register `:eyelib-util` in `settings.gradle` with `include("eyelib-util")`
- [ ] Replace ALL wildcard imports to `io.github.tt432.eyelib.util.*` with explicit imports
- [ ] Run consumer audit: for every file in `root/util/*`, run `ide_ide_find_references` and classify (0/1/N consumers)
- [ ] For each file, assign destination: eyelib-util (N≥2), functional owner (1), delete (0)
- [ ] Build project (`jetbrain_build_project`) — should succeed (module exists but is empty/not yet consumed)
- [ ] Identify intra-util dependency clusters (files that import each other; all must move together)
- [ ] Identify MC/Forge-dependent files → confirm eyelib-util Forge module provides MC classpath

### Phase 2: Root Util → eyelib-util Migration

- [ ] Move codec family (9 files + ImmutableFloatTreeMap) as atomic unit
- [ ] Move math family (EyeMath, Curves, MathHelper, FastColorHelper, Shapes) as unit — but confirm Shapes consumer count first
- [ ] Move search family (Searchable, SearchResults) after confirming consumer count > 1
- [ ] Move modbridge family (BBModelSink, ModBridgeServer) — pure Java, safe to move together
- [ ] Move top-level files (ResourceLocations, Lists, Collectors, EntryStreams, ListHelper, SimpleTimer, Blackboard) — each individually after consumer audit
- [ ] Resolve ResourceLocations.mod() — delete method or parameterize
- [ ] Resolve core/util + root/util wrapper duplication (TexturePathHelper → TexturePaths merge, etc.)
- [ ] Delete single-consumer files (AnimationApplier, Models) → move to animation domain in root
- [ ] After EACH file move: update imports, then `jetbrain_build_project` + verify zero errors
- [ ] After ALL files moved: confirm `root/util/*` directory is EMPTY (grep for any remaining `.java` files)

### Phase 3: Submodule Shared Code Centralization

- [ ] Audit all 6 existing submodules for shared utility code candidates:
  - [ ] eyelib-attachment: streamcodec family → move to eyelib-util
  - [ ] eyelib-material: DispatchedMapCodec → verify it's the same class; if different, merge or rename
  - [ ] eyelib-importer: check for duplicate codec/math helpers
  - [ ] eyelib-molang: check for duplicate utility code
  - [ ] eyelib-particle: check for duplicate utility code
  - [ ] eyelib-processor: check for duplicate utility code
- [ ] For each centralized piece: add `project(':eyelib-util')` dep to the submodule, update imports, delete local copy
- [ ] After each submodule rewiring: `jetbrain_build_project` + verify zero errors
- [ ] Add root `project(':eyelib-util')` dependency (root was the original util consumer)

### Phase 4: Verification & Cleanup

- [ ] Full project build: `jetbrain_build_project` with `rebuild = true`
- [ ] IDE diagnostics on all modules: `ide_diagnostics` with `includeBuildErrors: true`
- [ ] Grep for `io.github.tt432.eyelib.util.` in ALL source files — should return zero results
- [ ] Grep for `root/util/*` directory — confirm empty
- [ ] Verify eyelib-util build.gradle has zero `project(...)` deps
- [ ] Verify no two modules have the same `modId`
- [ ] Verify deprecation markers on any temporary facades (if any needed)
- [ ] Run existing tests — confirm zero regression
- [ ] Update MODULES.md with eyelib-util entry
- [ ] Update PROJECT.md Key Decisions with v1.3 outcome
- [ ] Run full ClientSmoke flow if available

---

## Sources

| Source | Confidence | Relevance |
|--------|------------|-----------|
| PROJECT.md (v1.3 milestone context, Key Decisions) | HIGH | Defines eyelib-util as Forge module, single-consumer rule, root/util/* must be empty |
| MODULES.md (complete module inventory, ownership boundaries) | HIGH | Documents existing module boundaries, package prefixes, dependency directions |
| root/util/README.md (historical notes, boundary reminders) | HIGH | Documents prior util split, deleted shims, "no new code in util/client/" rule |
| core/README.md (platform-free boundary rules) | HIGH | Defines core's MC-free constraint, first-wave extractions |
| .planning/REQUIREMENTS.md (v1.2 18 requirements, traceability) | HIGH | Documents v1.2 particle extraction requirements and verification patterns |
| Source code analysis: 34 files in root/util/*, 5 in core/util/*, 6 submodule build.gradle files | HIGH | Direct evidence of dependency patterns, import counts, duplicate code |
| v1.2 particle extraction experience (Phase 8-14, 7 phases, 22 plans) | HIGH | Lessons on package collisions, circular deps, import-hell, boundary leakage |
| eyelib-attachment build.gradle (legacyForge configuration) | HIGH | Template for eyelib-util Gradle configuration |
| eyelib-material/DispatchedMapCodec (duplicate with root) | HIGH | Concrete evidence of submodule code duplication |
| EyelibStreamCodecs.java (MC-dependent stream codec utilities in attachment) | HIGH | Candidate for centralization with documented MC dependencies |

---

## Summary of Critical vs Moderate vs Minor Pitfalls

### Critical (must prevent — rewrites required if missed):
1. **C1: Package shadowing** — atomic move+delete only; two copies on classpath = broken build
2. **D1: Circular dependency** — eyelib-util with project deps creates irreversible cycle
3. **R1: mods.toml ID collision** — duplicate modId → Forge startup crash
4. **O1: Single-consumer code in eyelib-util** — degrades module boundary, long-term maintenance cost
5. **R2: ResourceLocations.mod() MOD_ID circular reference** — compile failure blocking Phase 2

### Moderate (avoid if possible — workarounds exist):
1. **C2: Wildcard import collapse** — fixable with pre-migration audit
2. **D2: Duplicate DispatchedMapCodec** — fixable with careful centralization
3. **C4: Import-heavy file churn** — manageable with phase grouping and IDE tools
4. **O2: Core/util wrapper duplication** — fixable during merge
5. **R4: ModBridgeServer lifecycle** — likely unaffected if root controls startup

### Minor (low risk — easy to fix if noticed):
1. **C3: Intra-util self-reference** — fixed by atomic codec family migration
2. **C5: EyelibCodec MC dependency** — trivially resolved since eyelib-util IS a Forge module
3. **D3: StreamCodec relocation dependency** — simple dependency addition
4. **D4: Gradle scope mismatch** — `implementation` vs `api` is easy to flip
5. **O3: Package name convention** — documented rule, easy to verify
6. **O4: Legacy Eyelib MOD_ID** — resolved by deleting `mod()` method
7. **R3: SharedLibraryLoader classpath** — only relevant if native loading is actually used
