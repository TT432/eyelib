# Coding Conventions

**Analysis Date:** 2026-05-06

## Naming Patterns

**Files:**
- PascalCase for all class/interface/record/enum source files: `MolangCompilerImpl.java`, `EyelibNetworkManager.java`
- Test files suffixed `Test` (not `Spec` or `Tests`): `ManagerStorageTest.java`, `BedrockGeometryImporterTest.java`
- `package-info.java` used in most non-trivial packages for package-level annotations

**Functions:**
- camelCase method names, often highly descriptive
- Test methods use full-sentence camelCase describing expected behavior:
  ```java
  void mapsMinimalFixtureVisibleBoxFromBedrockDescription()
  void callableVariantSelectionFailsLoudlyOnEqualSpecificityEqualPriorityAmbiguity()
  void putAndGetAllDataReturnsSnapshot()
  ```
- Accessor methods follow record-style naming (e.g., `sourceExpression()`, `toplevelBones()`) vs JavaBean `get*()` — records are the preferred data-carrier pattern

**Variables:**
- camelCase for locals and instance fields
- `static final` constants use SCREAMING_SNAKE_CASE: `EsSILON`, `DEFAULT_MODE`, `LOGGER`, `GSON`
- Logger field names: `LOGGER` (SLF4J via Lombok or explicit), `LOG` (in `java.util.logging` instrument package only)
- No Hungarian notation or type-prefixing observed

**Types:**
- PascalCase for all types
- Implementation suffix `Impl`: `MolangCompilerImpl`, `HiddenMolangExpression`
- Data carriers overwhelmingly use Java `record`: `BrBoneAnimation`, `RenderParams`, `ModelPartModel`, `InstrumentEvent`
- Sealed types use `permits` clause: `sealed interface BrAnimationEntryTrackDefinition permits BrAnimationEntryEffectTrackDefinition, BrAnimationEntryBoneTrackDefinition`
- Custom exceptions extend `RuntimeException`: `ExpressionCompileException`, `MolangUncompilableException`
- Ops/Helper/Service suffix for stateless utility facades: `DataAttachmentSyncPayloadOps`, `RenderHelper`, `ClientRenderSyncService`

## Code Style

**Formatting:**
- No external formatter config detected (no `.editorconfig`, no `checkstyle.xml`, no `prettier`)
- Formatting relies on IntelliJ IDEA defaults and the IDE's built-in formatter
- 4-space indentation using spaces (not tabs)
- Opening braces on same line (K&R style)
- Line comments use `//`, block comments use `/* ... */`

**Linting:**
- **Error Prone** (`com.google.errorprone:error_prone_core:2.42.0`) — enabled for `nullawayMain` task only, disabled for regular compilation
- **NullAway** (`com.uber.nullaway:nullaway:0.12.10`) — configured via `nullawayMain` task with `AnnotatedPackages=io.github.tt432.eyelib`, `CheckOptionalEmptiness=true`
- **JSpecify** (`org.jspecify:jspecify:1.0.0`) — compile-only dependency for nullability annotations
- Regular compilation runs with `options.errorprone.enabled = false`; null-safety checking is opt-in via the `nullawayMain` Gradle task
- Root `build.gradle` mandates `nullawayMain` as a dependency of `check`

**Lombok:**
- **Heavy Lombok usage** (`io.freefair.lombok` plugin, version 8.6, applied to all subprojects)
- Common annotations:
  - `@NoArgsConstructor(access = AccessLevel.PRIVATE)` — for utility/static-only classes (singletons, managers, registries)
  - `@Getter` — class-level on data holders (e.g., `RenderData`, `ModelComponent`)
  - `@Setter` — sparingly, on mutable runtime state fields
  - `@With` — on immutable record-like payload types (`ExtraEntityData`, `EntityStatistics`, `ModelComponent.SerializableInfo`)
  - `@Slf4j` / `lombok.extern.slf4j.Slf4j` — on some loader classes (provides `log` field; some files additionally declare explicit `LOGGER`)
- Records preferred over `@Data`/`@Value` in most modern code
- No `lombok.config` file detected — default Lombok behavior

**Null-Safety:**
- JSpecify `@NullMarked` on `package-info.java` files for packages with strict null contracts:
  - `eyelib-molang/compiler/binding/`, `eyelib-molang/compiler/frontend/`, `eyelib-molang/compiler/frontend/ast/`
  - `eyelib-attachment/dataattach/`
  - `eyelib-processor/animation/baked/`
- JSpecify `@Nullable` on individual fields/parameters/methods (439+ occurrences across the codebase)
- Null-safety annotations consistently used on public API surfaces

## Import Organization

**Order (observed pattern):**
1. Java standard library (`java.*`, `javax.*`)
2. Third-party libraries (alphabetical by root package):
   - `com.google.gson.*`, `com.mojang.*`
   - `lombok.*`
   - `org.joml.*`, `org.jspecify.*`, `org.junit.*`
   - `org.slf4j.*`
3. Project-internal imports (alphabetical):
   - Other subproject packages (`io.github.tt432.eyelib.*`)
   - Same-subpackage classes

**Static imports:**
- Assertion methods: `import static org.junit.jupiter.api.Assertions.*`
- Constants: `import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_MODE`
- No wildcard static imports in production code; tests use `Assertions.*` wildcard

**No star imports** observed in production code.

**Path Aliases:**
- No path aliases (Gradle project references use `project(':eyelib-molang')` etc.)

## Error Handling

**Exception Design:**
- All custom exceptions extend `RuntimeException` (unchecked) — no checked exceptions in public APIs
- Exceptions carry contextual information beyond a message string:
  ```java
  public class ExpressionCompileException extends RuntimeException {
      private final String sourceExpression;
      private final List<String> diagnostics;
  }
  ```

**Patterns:**
- **Mojang DataResult pattern**: `result.getOrThrow(false, msg -> new AssertionError(msg))` or `result.getOrThrow(false, LOGGER::warn)` — used extensively with `com.mojang.serialization` codecs
- **Fail-fast**: Throw immediately on invalid state with descriptive messages
- **Context preservation**: Wrapping/forwarding exception details to higher-level exceptions
- **Test error verification**: `assertThrows(ExpectedException.class, () -> { ... })` with message content checks via `assertTrue(exception.getMessage().contains("..."))`
- Try-catch in compilation: catches generic `Throwable` and wraps in domain exception:
  ```java
  try { ... } catch (Throwable t) {
      throw new ExpressionCompileException(expression, "Failed to compile...", t);
  }
  ```

**Return Null Patterns:**
- `@Nullable` return types documented via JSpecify annotations
- `Optional<T>` return types used in some APIs (e.g., parser frontends: `Optional<MolangAst.ExprSet>`)
- `getOrNull()` helper method naming for nullable return variants (e.g., `DataAttachmentHelper.getOrNull()`)

## Logging

**Framework:**
- **Primary**: SLF4J (`org.slf4j`) — 90%+ of the codebase
  - Used via explicit `LoggerFactory.getLogger(ClassName.class)` with field name `LOGGER`
  - Also via Lombok `@Slf4j` annotation (provides `log` field)
- **Secondary**: `java.util.logging` (`java.util.logging.Logger`) — isolated to `client/instrument/` package (`BackgroundFlushService`, `InstrumentDatabase`, `EventRingBuffer`, `InstrumentLifecycleHooks`, `MolangDiskCacheObserver`, `InstrumentConfig`)
  - Field name: `LOG`

**Patterns:**
- Logging at appropriate levels: `.error()` for exceptions, `.warn()` for fallback/graceful degradation, `.debug()` for diagnostic detail
- Log parameters passed directly (SLF4J parameterized messages): `LOGGER.error("can't load addon source {}, fallback to plain folder mode.", basePath, exception)`
- Logger instance field:
  ```java
  private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceImportPlanner.class);
  ```

## Comments

**When to Comment:**
- Javadoc on most public classes and methods, especially in molang compiler and mapping packages
- `@author TT432` on some network/bootstrap classes
- Inline comments for multi-step algorithms (e.g., `// Step 1: Parse source into AST...`)
- Markup: `TODO`, `FIXME`, `XXX` present in limited quantities (7 hits in root `src/main/java`):
  - `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` — `// TODO 不确定` (Chinese: "uncertain")
  - `src/main/java/io/github/tt432/eyelib/util/codec/KeyDispatchMapCodec.java` — `// XXX: isError -> error().isPresent() ?`
  - `src/main/java/io/github/tt432/eyelib/util/codec/DispatchedMapCodec.java` — `// XXX: isError -> error().isPresent() ?`
  - `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` — `//TODO`
  - `src/main/java/io/github/tt432/eyelib/client/AttachableBlockEntityWithoutLevelRenderer.java` — `// TODO:fix`

**JSDoc/TSDoc:**
- Not applicable (Java codebase)

**Javadoc:**
- Used on public API classes and interfaces
- `@param` and `@return` used in method documentation
- TDD markers in some tests: `TDD RED phase: These tests may currently fail...`

## Function Design

**Size:** Methods generally stay focused and small. Test classes may have larger helper methods (e.g., `independentCorners()` in `BedrockGeometryImporterTest` at ~60 lines).

**Parameters:**
- Constructor parameters map directly to record components or fields
- Builder-like patterns via `@With` annotations on records
- Factory methods (`static of(...)`, `static from*(...)`) preferred over complex constructors
- `@Nullable` annotations on optional parameters

**Return Values:**
- Prefer concrete types; use interfaces where multiple implementations exist
- `List.copyOf()` for defensive copies in public getters
- `Optional<T>` for parse/maybe-return semantics
- Void methods for side-effecting operations (fire-and-forget)

## Module Design

**Exports:**
- Public API via `public` classes and interfaces
- Implementation classes may be `public` (not package-private) even when intended as internal — the `internal/` package serves as a documentation marker for non-public-API code
- `sealed interface` restricts inheritance to known subtypes (molang compiler/type system)

**package-info.java:**
- Used pervasively (20+ files detected in root, 6+ in `eyelib-molang`)
- Carries `@NullMarked` annotations for NullAway enforcement boundaries
- Carries package-level Javadoc

**README.md per package:**
- Documented in `MODULES.md` as a convention: every significant package subtree has a `README.md`
- Examples: `client/README.md`, `client/loader/README.md`, `client/registry/README.md`, `util/README.md`, `mc/README.md`

**Barrel Files:**
- Not a Java pattern, but analogous facade classes exist:
  - `*Ops` classes (stateless utility methods): `DataAttachmentSyncPayloadOps`, `LoaderParsingOps`
  - `*Helper` classes: `RenderHelper`, `DataAttachmentHelper`
  - `*Lookup` seam classes (narrow read-only access): `ModelLookup`, `AnimationLookup`, `ClientEntityLookup`

---

*Convention analysis: 2026-05-06*
