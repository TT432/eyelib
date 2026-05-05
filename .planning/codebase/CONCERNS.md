# Codebase Concerns

**Analysis Date:** 2026-05-06

## Tech Debt

### Pre-existing Root Build Failure (Blocking)
- Issue: Root `jetbrain_build_project` fails on `-Xlint:removal` warnings treated as errors, all related to `MolangOwnerSet` being `@Deprecated(forRemoval=true)` and still used by `MolangScope.java` and `MolangOwnerSetHostContextAdapter.java`. This is NOT caused by any recent change — it is a pre-existing condition from other workspace changes.
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/HostContext.java`
- Impact: Full project build is broken. Only module-scoped compilation (`:eyelib-molang:compileJava`, `:eyelib-importer:compileJava`, etc.) works.
- Fix approach: Remove all remaining deprecated `MolangOwnerSet` references and finalize `HostContext` migration, or suppress the `removal` lint temporarily until the migration is complete.

### eyelib-importer Compile Failure
- Issue: `:eyelib-importer:build` fails with 41 compile errors — `MolangValue` and `MolangScope` symbols from `io.github.tt432.eyelibmolang` are not found during compilation.
- Files: Multiple files under `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/`
- Impact: The importer subproject cannot be built or tested. Blocked from any importer-boundary expansion work.
- Fix approach: Verify the dependency configuration between `:eyelib-importer` and `:eyelib-molang` (current `implementation project(':eyelib-molang')` should resolve), check classpath ordering, and ensure `io.github.tt432.eyelibmolang` package is properly exported.

### H2 Dependency Risk
- Issue: `com.h2database:h2:2.4.240` is present in root `build.gradle` (line 160) for `jarJar` bundling into the mod artifact and in test scope (line 180). This was flagged as a violation of removal guardrails in a previous audit but persists.
- Files: `build.gradle` lines 160, 180
- Impact: Unnecessary database dependency being shipped in a Minecraft rendering library. The disk cache (`MolangDiskCache`) that used H2 for persistence has been removed from the compiler pipeline; the only current H2 consumer is the performance instrumentation subsystem.
- Fix approach: Either remove H2 entirely (if instrumentation can use a simpler persistence layer) or narrow the dependency to instrumentation-only scope instead of bundling it into the mod JAR.

### Generated ANTLR Parser (1098+205 Lines of Opaque Code)
- Issue: `MolangParser.java` (1098 lines) and `MolangLexer.java` (205 lines) are ANTLR 4.9.1 generated artifacts that are checked into source control. They contain serialized ATN state (`_serializedATN`) that is effectively binary data stored as a Java string literal.
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/MolangParser.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/MolangLexer.java`
- Impact: Zero readability. Any change to the Molang grammar requires ANTLR regeneration. The generated parser is being phased out in favor of a handwritten AST frontend, but still exists as a compatibility path.
- Fix approach: Continue the parser migration. Once `GeneratedParserBackedAstMolangParserFrontend` cutover is complete and parity evidence is green, remove the generated ANTLR artifacts.

### Phase 4 Molang Refactor — Incomplete Migration
- Issue: The `MolangOwnerSet` → `HostContext` migration is partially complete. `MolangOwnerSet.java` has been deleted, but downstream `Class<?>` callers are still pending migration to the canonical `HostRole<T>` lookup. Bind-link contract tests are not yet implemented.
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/HostContext.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`, `eyelib-molang/refactor-plan/04-host-and-query-bridge.md`
- Impact: Blocked Phase 4 — broad host/query bridge implementation cannot proceed. The `@Deprecated(forRemoval=true)` annotations in `HostContext` and `MolangScope` are triggering build failures.
- Fix approach: Complete the `HostRole<T>` migration in all downstream callers. Implement the missing bind-link contract tests (`MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`, `MolangAnimationClockTransitionalParityContractTest`). Then remove the deprecated `Class<?>` lookup variants.

### Catch-All Area — `util/client/` Pending Destination Cleanup
- Issue: AGENTS.md explicitly warns against adding new code to `util/client/`, but the directory still contains 3 source files (`Models.java`, `AnimationApplier.java`, `TexturePathHelper.java`) that are transitional utility code awaiting final destination assignment.
- Files: `src/main/java/io/github/tt432/eyelib/util/client/Models.java`, `src/main/java/io/github/tt432/eyelib/util/client/AnimationApplier.java`, `src/main/java/io/github/tt432/eyelib/util/client/texture/TexturePathHelper.java`
- Impact: Ambiguous code placement. New contributors may add utils here instead of proper domain owners. The documented boundary says this area "has been drained" but residual code remains.
- Fix approach: Move `Models.java` and `AnimationApplier.java` to a proper client module (render or animation domain). `TexturePathHelper.java` should migrate to `core/util/texture` per the utility split plan.

### Broad Exception Catch Blocks
- Issue: 17+ locations use `catch (Exception e)` or `catch (Exception ignored)` with either silent swallowing or inadequate logging. This masks real failures during resource loading, addon bridging, and import planning.
- Files (representative): `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java` (lines 77, 247), `src/main/java/io/github/tt432/eyelib/client/loader/BedrockAddonRuntimeBridge.java` (line 116), `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java` (line 62), `src/main/java/io/github/tt432/eyelib/util/modbridge/ModBridgeServer.java` (line 85), `src/main/java/io/github/tt432/eyelib/client/instrument/InstrumentLifecycleHooks.java` (lines 35, 92, 124)
- Impact: Silent failures are extremely hard to debug. Resource import failures, addon loading errors, and particle command errors produce no actionable diagnostics.
- Fix approach: Replace broad `catch (Exception)` with specific exception types. Where broad catch is truly necessary, ensure the exception is logged at WARN or ERROR level with sufficient context (file path, resource name, operation).

### XXX/Known-Bug Markers in Codec Utilities
- Issue: Two `// XXX` comments in codec utility code suggest known logic bugs: `isError -> error().isPresent() ?` question marks an incomplete refactoring or unconfirmed edge case in both `DispatchedMapCodec` and `KeyDispatchMapCodec`.
- Files: `src/main/java/io/github/tt432/eyelib/util/codec/DispatchedMapCodec.java` (line 70), `src/main/java/io/github/tt432/eyelib/util/codec/KeyDispatchMapCodec.java` (line 61)
- Impact: Unknown — could be a real bug in codec dispatch or just a stylistic TODO. Either way, codec bugs in core utilities would corrupt serialization for the entire pipeline.
- Fix approach: Review the `isError` / `error().isPresent()` logic, confirm the correct semantics, and either fix the code or remove the XXX marker.

### @SuppressWarnings Proliferation
- Issue: 30+ `@SuppressWarnings` annotations across the codebase, predominantly `"unchecked"` and `"NullAway"`. Many suppress type-safety and null-safety warnings in generics-adjacent code, particularly in codec and capability layers.
- Files (representative): `src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java`, `src/main/java/io/github/tt432/eyelib/capability/RenderData.java`, `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/component/ParticleComponentManager.java`, `src/main/java/io/github/tt432/eyelib/util/ImmutableFloatTreeMap.java`
- Impact: Suppressed warnings hide real potential issues. The `"unchecked"` casts inside codec/capability layers could produce `ClassCastException` at runtime if the type parameters are wrong.
- Fix approach: Audit each `@SuppressWarnings("unchecked")` site. For codec usage, introduce type-safe wrapper methods or explicit `@SuppressWarnings` with comments justifying each case. For NullAway suppressions, either fix the nullability annotations or document why the suppression is necessary.

### eyelib-material Shared/GL Type Duplication
- Issue: The `eyelib-material` module maintains parallel type trees: pure-data copies in the `shared/` package (e.g., `shared/BrMaterialEntry`) and runtime/GL-bounded copies in `material/` and `gl/` packages. Enum-by-name conversion (`valueOf(name)`) between layers is verbose and fragile at runtime.
- Files: `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/shared/BrMaterialEntry.java` ↔ `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrMaterialEntry.java`, plus matching pairs for `BlendFactor`, `DepthFunc`, `GLStates`, `BrMaterial`
- Impact: Name mismatch between shared and GL enum variants would cause runtime failures with confusing error messages. Maintenance requires updating both copies for any schema change.
- Fix approach: Establish a single source of truth with a conversion layer that is tested for completeness. Consider a code-generation step to produce one from the other.

## Known Bugs

### eyelib-material Test Failures (5/22 failing)
- Symptoms: `BrMaterialEntryRenderTypeTest` (3 failures: ExceptionInInitializerError/NoClassDefFoundError), `MaterialEndToEndTest` (2 failures: CODEC parsing returned incorrect result, inheritance resolution returned null).
- Files: `eyelib-material/src/test/java/io/github/tt432/eyelibmaterial/BrMaterialEntryRenderTypeTest.java`, `eyelib-material/src/test/java/io/github/tt432/eyelibmaterial/MaterialEndToEndTest.java`
- Trigger: `BrMaterialEntryRenderTypeTest` fails because MC bootstrap is not initialized in plain Gradle test runner — tests hit `Bootstrap.checkBootstrapCalled()`. `MaterialEndToEndTest` failures appear to be logic bugs in CODEC parsing and inheritance resolution.
- Workaround: For `RenderType` tests, use `@ExtendWith(MinecraftExtension.class)` or equivalent MC bootstrapper. For CODEC/inheritance failures, debug the actual parsing and resolution logic.

### Root Test Failures (3 NoSuchFileException cases)
- Symptoms: `BedrockGeometryImporterTest` (2 failures at line 403), `RenderGeometryDumpParityTest` (1 failure at line 132) — all `NoSuchFileException`.
- Files: `src/test/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporterTest.java`, `src/test/java/io/github/tt432/eyelib/client/render/RenderGeometryDumpParityTest.java`
- Trigger: Test fixture files are missing or the test working directory is not correctly resolving relative paths.
- Workaround: Verify test resource directories (`src/test/resources/`) contain the expected fixture files at the paths the tests reference.

### Instrument Test Compile Errors (2 pre-existing failures)
- Symptoms: `CacheSizeObserverTest.java` — `MolangValue` symbol not found at lines 183-185. `InstrumentMolangIntegrationTest.java` — lambda captures non-final loop variable at line 44.
- Files: `src/test/java/io/github/tt432/eyelib/client/instrument/collector/CacheSizeObserverTest.java`, `src/test/java/io/github/tt432/eyelib/client/instrument/InstrumentMolangIntegrationTest.java`
- Trigger: Compile errors prevent test execution. Unrelated to any recent instrumentation changes — pre-existing condition.
- Workaround: Fix the MolangValue import path and the non-final variable capture in the integration test.

## Security Considerations

### SharedLibraryLoader — Native Code Loading
- Risk: The `SharedLibraryLoader` class dynamically extracts and loads native libraries from JARs at runtime. It uses `synchronized` blocks on a class-level lock and writes to the system temp directory.
- Files: `src/main/java/io/github/tt432/eyelib/util/SharedLibraryLoader.java` (290 lines)
- Current mitigation: Libraries are extracted from trusted JAR sources. The temp file naming uses `libraryName`.
- Recommendations: Verify that the extracted library path is validated against path traversal. The class-level `synchronized` lock could cause contention if multiple native libraries are loaded concurrently.

### Maven Publishing Credentials
- Risk: The root `build.gradle` (lines 269-272) references OSS Sonatype credentials (`ossrhUsername`, `ossrhPassword`) from Gradle properties. These are typically in `~/.gradle/gradle.properties` and not in the repo — standard practice.
- Files: `build.gradle` lines 267-275
- Current mitigation: Credentials are in local-only Gradle properties, not committed.
- Recommendations: No change needed — this is standard.

### ModBridgeServer — External Socket Communication
- Risk: The `ModBridgeServer` utility opens a server socket with broad `catch (Exception e)` swallowing all connection errors.
- Files: `src/main/java/io/github/tt432/eyelib/util/modbridge/ModBridgeServer.java`
- Current mitigation: Unknown — insufficient logging.
- Recommendations: Add proper error logging, ensure the server binds only to localhost unless remote access is explicitly needed, and add a configurable port range.

## Performance Bottlenecks

### Texture Layer Merging with Compute Shaders
- Problem: `TextureLayerMerger.java` creates and compiles OpenGL compute shaders at runtime with `throw new RuntimeException` on any GL failure. No caching of compiled shader programs.
- Files: `src/main/java/io/github/tt432/eyelib/client/render/texture/TextureLayerMerger.java`
- Cause: Shader compilation and linking happens each time texture layers are merged — potentially per-frame if textures change frequently.
- Improvement path: Cache compiled compute shader programs keyed by shader source hash. Add a fallback path if compute shaders are not supported (instead of crashing).

### ImmutableFloatTreeMap — Naive Immutability
- Problem: `ImmutableFloatTreeMap` converts `float` keys to `int` via `Float.floatToIntBits()` for ordering, but uses raw `TreeMap` internally with repeated `return null` returns for edge cases. No specialized float range optimizations.
- Files: `src/main/java/io/github/tt432/eyelib/util/ImmutableFloatTreeMap.java` (152 lines)
- Cause: Generic map approach with boxing/unboxing overhead per lookup.
- Improvement path: Consider `fastutil` `Float2ObjectOpenHashMap` or specialized float-keyed tree structures. The `return null` fallbacks could use `Optional` to make absent values explicit.

### Compilation Cache Size Cap
- Problem: `MolangCompileCache` has a hard-coded `MAX_L1_SIZE=1000` with no eviction policy beyond the `ConcurrentHashMap` size check. Once full, new entries are silently rejected.
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/cache/MolangCompileCache.java`
- Cause: Simple size guard to prevent memory leaks.
- Improvement path: Implement LRU eviction. Consider making the cache size configurable. Add metrics for cache hit/miss rates.

### L2 Disk Cache Removed
- Problem: The L2 disk cache (`MolangDiskCache`) has been removed. `collectSlowMetrics()` cannot emit `MolangDiskCacheObserver` telemetry because no disk cache instance is exposed.
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/cache/MolangCompileCache.java`
- Cause: Intentional removal — the disk cache was never populated in production and added complexity.
- Impact: All Molang expressions are recompiled on cold start. For large animation packs, this could cause a noticeable startup delay.
- Improvement path: If startup performance becomes an issue, re-introduce a disk cache with proper serialization (ProtoBuf or simple binary format) rather than H2.

## Fragile Areas

### Generated Parser Cutover Boundary
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/MolangParser.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/MolangLexer.java`, `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/`
- Why fragile: Three parser paths exist (generated ANTLR, generated-backed AST frontend, handwritten AST frontend). These must stay in sync. The generated parser cannot be debugged with normal IDE tooling. Changes to Molang syntax require ANTLR regeneration.
- Safe modification: Only edit the handwritten frontend. Do not edit generated files. Run corpus tests after any frontend change.
- Test coverage: Phase 1 corpus has 33 rows. Handwritten frontend has 20+ acceptance/rejection tests. Generated parser parity is only partial (🔶).

### Capability Component Sync Boundary
- Files: `src/main/java/io/github/tt432/eyelib/capability/component/AnimationComponent.java`, `src/main/java/io/github/tt432/eyelib/capability/component/ModelComponent.java`, `src/main/java/io/github/tt432/eyelib/capability/RenderData.java`
- Why fragile: AnimationComponent uses `synchronized(INSTANCES)` to guard a static `Set`. ModelComponent has three methods that `return null` when `serializableInfo == null`, risking NPE at call sites. RenderData has `@SuppressWarnings("unchecked")` casts on generic capability lookups.
- Safe modification: Never add new capability types without updating the sync packet layer. Always test both client and server sides.
- Test coverage: `AnimationComponentSerializableInfoTest.java`, `AnimationComponentRuntimeInvalidationTest.java`, `RenderControllerComponentTextureStateTest.java` exist but don't cover the full sync round-trip.

### eyelib-importer Dependency Chain
- Files: `eyelib-importer/build.gradle`, `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/`
- Why fragile: The importer is currently broken (41 compile errors). It depends on `:eyelib-molang` and `:eyelib-material`, but symbol resolution fails. Any change to Molang's public API could break the importer silently until a full build is attempted.
- Safe modification: Always run `:eyelib-molang:compileJava` before importer changes. Fix the root cause of the compile failure before adding new code to the importer.
- Test coverage: Importer test coverage exists but is blocked by the compilation failure.

### Serialized ATN in Generated Parser
- Files: `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/MolangParser.java` (line 1090 — `_serializedATN` string)
- Why fragile: The serialized ATN is a human-unreadable binary blob embedded as a Java string literal. If the grammar changes, this must be regenerated exactly. Any manual edit or merge conflict here would produce a non-obvious runtime failure.
- Safe modification: Never hand-edit. Regenerate via ANTLR tool or keep the generated files read-only as policy dictates.

## Scaling Limits

### Single-Tier Compilation Cache
- Current capacity: 1000 entries in `ConcurrentHashMap` with no eviction.
- Limit: Once full, overflow entries silently fail to cache. For large modpacks with many Molang expressions across many entities, 1000 entries may be exceeded.
- Scaling path: Increase `MAX_L1_SIZE` or implement LRU eviction. Re-introduce an optional disk cache for persistence across restarts.

### ClientTickHandler — Per-Frame Processing
- Current capacity: Ticks all animation controllers, particle systems, and render updates every client tick (20 TPS).
- Limit: As the number of animated entities and particles grows, per-frame processing could exceed the tick budget, causing frame drops.
- Scaling path: Profile the tick handler with realistic entity counts. Consider distance-based LOD culling, tick-rate scaling for distant entities, or batched Molang evaluation.

### Texture Atlas Merging
- Current capacity: `TextureLayerMerger` creates merged texture atlases in GPU memory.
- Limit: Large numbers of layered render-controller textures (many entities × many layers) could exhaust GPU memory or exceed the maximum texture size.
- Scaling path: Implement texture atlas size limits. Consider mipmap-based resolution scaling for distant entities. Add a texture memory budget monitor.

## Dependencies at Risk

### ANTLR 4.9.1 Runtime
- Risk: ANTLR 4.9.1 is 5+ years old (current is 4.13.x). The generated parser is locked to this version. The handwritten parser frontend is the migration target, but the generated path is still active.
- Impact: Security vulnerabilities in old ANTLR versions. Difficult to upgrade without regenerating the parser — which is a policy-controlled operation.
- Migration plan: Complete the handwritten parser frontend migration. Remove the ANTLR dependency entirely once the generated parser path is cut over.

### H2 Database 2.4.240
- Risk: H2 is a full SQL database being bundled into a Minecraft rendering library. It is used only by the performance instrumentation subsystem (not the core rendering pipeline).
- Impact: Unnecessary binary bloat in the mod JAR. Potential classpath conflicts with other mods using a different H2 version. H2 has a track record of CVEs.
- Migration plan: Either remove H2 entirely and use a simpler persistence format (JSON, ProtoBuf), or keep it as a `compileOnly` dependency that users optionally provide.

### com.mojang:datafixerupper:6.0.8
- Risk: This is a standalone repackaging of Minecraft's codec library. While widely used in the Forge ecosystem, its update cadence is tied to Minecraft releases.
- Impact: Breaking changes in newer versions could affect codec-based serialization across the entire eyelib pipeline.
- Migration plan: Monitor Mojang's releases. Consider whether the codec abstraction layer in `util/codec/` can shield the codebase from DFU version changes.

## Missing Critical Features

### No End-to-End Sync Test Coverage
- Problem: Capability-to-client sync is tested in isolated unit tests only. The full round-trip (server capability mutation → network packet → client render update) has no automated test.
- Blocks: Confidence in sync correctness relies on manual QA. Any refactor of the sync layer risks regressions.
- Recommendation: Add a server-side test that sets capability data, sends packets, and asserts the resulting client state. Use the existing packet contracts in `network/`.

### No Render-Controller End-to-End Test
- Problem: Render controllers orchestrate complex state machines across animations, materials, and textures. No integration test validates the full controller → render output pipeline.
- Blocks: Controller behavior changes cannot be verified without a running Minecraft client.
- Recommendation: Create a test harness that provides mock `MolangScope` and render context, allowing controller state transitions to be asserted programmatically.

### No Particle System Integration Test
- Problem: Particle emitters, spawn/remove services, and render managers are tested only in unit isolation.
- Blocks: Particle lifecycle bugs (memory leaks, orphaned emitters) can only be detected through extended manual playtesting.
- Recommendation: Add a test that spawns particles via the packet layer, runs multiple ticks, and asserts that emitters are properly cleaned up.

## Test Coverage Gaps

### eyelib-attachment Module
- What's not tested: Most of the typed data attachment contracts. Only `DataAttachmentStorageTest.java` exists.
- Files: `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/dataattach/` (multiple source files, 1 test file)
- Risk: Attachment serialization bugs could cause data loss or sync failures in production.
- Priority: Medium

### eyelib-processor Module
- What's not tested: Processing pipeline coverage is minimal — 4 test files for the entire subproject.
- Files: `eyelib-processor/src/main/java/io/github/tt432/eyelibprocessor/` (multiple source files, 4 test files)
- Risk: Batching and reload planning bugs could cause resource import failures.
- Priority: Medium

### Network/Packet Layer
- What's not tested: `ExtraEntityDataPacket`, `DataAttachmentSyncPacket`, `AnimationComponentSyncPacket`, `ModelComponentSyncPacket` have no dedicated tests.
- Files: `src/main/java/io/github/tt432/eyelib/network/`
- Risk: Packet format changes could break mod compatibility silently.
- Priority: High

### Manager/GUI/Import Flow
- What's not tested: `EyelibManagerScreen`, `ManagerImportActions`, `ManagerResourceFolderWatcher` have no tests. The import flow is only tested at the planner level.
- Files: `src/main/java/io/github/tt432/eyelib/client/gui/manager/`
- Risk: UI regressions and import bugs require manual testing.
- Priority: Low (developer tooling)

### SharedLibraryLoader
- What's not tested: Native library extraction, platform detection, and loading — no test coverage.
- Files: `src/main/java/io/github/tt432/eyelib/util/SharedLibraryLoader.java`
- Risk: Platform-specific native loading failures could silently disable features that depend on native libraries.
- Priority: Low

### Molang Compiler — Remaining Phase 4 Test Gaps
- What's not tested: Bind-link contracts for query and callable resolution. Transitional animation-clock parity.
- Files: `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/` (gaps: `MolangQueryBindLinkContractTest`, `MolangCallableBindLinkContractTest`, `MolangAnimationClockTransitionalParityContractTest`)
- Risk: Phase 4 implementation cannot proceed without these contract tests green.
- Priority: High

---

*Concerns audit: 2026-05-06*
