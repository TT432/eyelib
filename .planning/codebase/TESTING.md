# Testing Patterns

**Analysis Date:** 2026-05-06

## Test Framework

**Runner:**
- JUnit Jupiter (JUnit 5) — `org.junit.jupiter:junit-jupiter` via BOM `org.junit:junit-bom:5.10.2`
- Platform: `org.junit.platform:junit-platform-launcher` (test runtime only)
- Config: `tasks.named('test').configure { useJUnitPlatform() }` in every subproject `build.gradle`

**Assertion Library:**
- JUnit Jupiter built-in assertions: `Assertions.assertEquals`, `Assertions.assertTrue`, `Assertions.assertFalse`, `Assertions.assertNotNull`, `Assertions.assertNull`, `Assertions.assertThrows`, `Assertions.assertSame`

**Run Commands:**
```bash
# All tests (via JetBrains MCP only — never ./gradlew directly):
jetbrain_run_gradle_tasks taskNames=['test']

# Specific subproject tests:
jetbrain_run_gradle_tasks taskNames=[':eyelib-molang:test']

# NullAway verification:
jetbrain_run_gradle_tasks taskNames=['nullawayMain']
```
No watch-mode or coverage commands configured in the build scripts.

## Test File Organization

**Location:**
- `src/test/java/` mirroring the main source package structure
- Root project tests: `src/test/java/io/github/tt432/eyelib/`
- Subproject tests:
  - `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/`
  - `eyelib-material/src/test/java/io/github/tt432/eyelibmaterial/`
  - `eyelib-importer/src/test/java/io/github/tt432/eyelibimporter/`
  - `eyelib-processor/src/test/java/io/github/tt432/eyelibprocessor/`
  - `eyelib-attachment/src/test/java/io/github/tt432/eyelibattachment/`

**Naming:**
- Test class: `*Test.java` (e.g., `ManagerStorageTest.java`, `BrMaterialCodecRegressionTest.java`, `DataAttachmentStorageTest.java`)
- No `*Spec`, `*Tests`, or `*IT.java` patterns observed

**Structure:**
```
src/test/
├── java/
│   └── io/github/tt432/eyelib/
│       ├── client/
│       │   ├── animation/bedrock/  (BrAnimationCodecTest, BrBoneAnimationChannelTest, ...)
│       │   ├── entity/            (ClientEntityRuntimeDataTest, ClientEntityLookupTest)
│       │   ├── gui/manager/reload/ (ManagerResourceImportPlannerAddonBridgeTest)
│       │   ├── instrument/        (InstrumentConfigTest, EventRingBufferTest, ...)
│       │   ├── loader/            (BedrockAddonRuntimeBridgeTest)
│       │   ├── manager/           (ManagerStorageTest, ManagerEventPublishBridgeTest)
│       │   ├── model/importer/    (BedrockGeometryImporterTest, BlockbenchModelImporterTest)
│       │   ├── particle/          (ParticleSpawnRequestTest)
│       │   ├── registry/          (ClientAssetRegistryTest, AttachableAssetRegistryTest, ...)
│       │   └── render/            (RenderGeometryDumpParityTest, RenderParamsTest, ...)
│       ├── capability/component/  (AnimationComponentSerializableInfoTest, ...)
│       ├── core/util/time/        (FixedStepTimerStateTest)
│       └── network/               (SpawnParticlePacketTest, DataAttachmentSyncPayloadOpsTest, ...)
└── resources/
    └── io/github/tt432/eyelib/    (test fixtures: JSON files, model data, etc.)
```

**Test resource fixtures:**
- Located under `src/test/resources/` matching the package path
- Example fixture path pattern: `eyelib-importer/src/test/resources/io/github/tt432/eyelib/client/model/importer/bedrock/*.json`
- Corpus test data: `eyelib-molang/src/test/resources/io/github/tt432/eyelibmolang/compiler/corpus/phase1/`
- Real-world fixtures: `eyelib-importer/src/test/resources/io/github/tt432/eyelibimporter/addon/fixtures/`

## Test Structure

**Suite Organization:**
```java
package io.github.tt432.eyelib.client.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManagerStorageTest {
    @Test
    void putAndGetAllDataReturnsSnapshot() {
        ManagerStorage<String> storage = new ManagerStorage<>();
        storage.put("first", "value");

        Map<String, String> snapshot = storage.getAllData();
        snapshot.put("second", "other");

        assertEquals("value", storage.get("first"));
        assertNull(storage.get("second"));
    }
    // ... more @Test methods
}
```

**Key Patterns:**
- Test classes are **package-private** (no `public` modifier): `class ManagerStorageTest`
- Test methods are **package-private** `void` methods annotated `@Test`
- **No `@Nested` inner classes** observed — tests are flat within each test class
- Test method names are **highly descriptive camelCase sentences** describing the expected behavior:
  ```java
  void callableVariantSelectionFailsLoudlyOnEqualSpecificityEqualPriorityAmbiguity()
  void importsRealFixturePerFaceUvBoxesForRotatedAndZeroDepthCubes()
  void starterCorpusRunsParseOnlyAssertionsAgainstGeneratedParserPath()
  ```
- **Setup/Teardown:**
  - `@BeforeEach` used for per-test state initialization (e.g., `InstrumentConfigTest`, `MolangDiskCacheTest`)
  - `@AfterEach` used pervasively for teardown/cleanup — especially in mapping tests that clear `MolangMappingTree.INSTANCE.clear()` and registry tests that reset manager state
  - `@BeforeAll` observed once in `ShaderManagerIntegrationTest` for shared GPU resource setup
- **Parameterized tests:** `@ParameterizedTest` with `@ValueSource` in codec/parsing tests:
  ```java
  @ParameterizedTest
  @ValueSource(strings = {"1+2", "1+2+3", "1*2+3", "(1+2)*3"})
  void simpleExpressionCompiles(String expression) { ... }
  ```
- **Temp directories:** `@TempDir Path tempDir` used in addon/integration tests for file-system isolation
- **Display names:** `@DisplayName("Roundtrip regression: cutout material entry")` used in `BrMaterialCodecRegressionTest`
- **`@NullMarked` on test package-info** in `eyelib-attachment/src/test/`

**Smoke/Infrastructure Test:**
```java
class MaterialTestInfrastructureTest {
    @Test
    void infrastructureWorks() {
        assertTrue(true);
    }
    @Test
    void basicAssertions() {
        assertEquals(4, 2 + 2);
    }
}
```

## Mocking

**Framework:** No mocking framework detected — no Mockito, EasyMock, or JMock dependencies in any `build.gradle`.

**Patterns:**
Test doubles are **hand-written** by implementing the interface or extending the class under test:
```java
// From DataAttachmentStorageTest.java
private static final class NullWhenReadStorage implements DataAttachmentStorage {
    private Integer stored;

    @Override
    public <T> boolean has(DataAttachmentType<T> attachment) { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void set(DataAttachmentType<T> attachment, T value) {
        stored = (Integer) value;
    }
    // ...
}
```

**What to Mock:**
- Pluggable interfaces/storage backends where the real implementation has external dependencies (e.g., `DataAttachmentStorage`)
- Test doubles kept as `private static final` inner classes at the bottom of the test class

**What NOT to Mock:**
- Codecs, parsers, compilers — tested directly with real implementations
- Managers, registries — tested via direct instantiation and method calls
- Data carriers (records) — tested via construction and field access

## Fixtures and Factories

**Test Data (embedded JSON):**
Java 17+ text blocks used for inline test fixtures:
```java
private static final String VANILLA_MATERIAL_JSON = """
        {
          "materials": {
            "cutout": {
              "vertexShader": "a",
              "fragmentShader": "b",
              "defines": [],
              "samplerStates": [],
              "states": [],
              "variants": []
            }
          }
        }
        """;
```

**Factory Methods:**
```java
private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
    MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
    assertNotNull(mapping);
    return new MolangMappingDiscovery.MolangMappingClassEntry(
            mapping.value(), mappingClass, mapping.pureFunction());
}
```

**Location:**
- Inline JSON text blocks within test classes for small fixtures
- Test resource files (`src/test/resources/`) for real-world/external fixture data
- Fixture path resolution: `fixturePath("bedrock/minimal.geometry.json")` resolving to `eyelib-importer/src/test/resources/io/github/tt432/eyelib/client/model/importer/bedrock/`

**Codec roundtrip helper:**
```java
private static BrMaterial roundtripVanillaMaterial(String entryKey) {
    var jsonElement = JsonParser.parseString(VANILLA_MATERIAL_JSON);
    var firstResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonElement.getAsJsonObject());
    var first = firstResult.getOrThrow(false, msg -> new AssertionError("First decode failed: " + msg));
    var encodedResult = BrMaterial.CODEC.encodeStart(JsonOps.INSTANCE, first);
    var encoded = encodedResult.getOrThrow(false, msg -> new AssertionError("Re-encode failed: " + msg));
    var secondResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, encoded);
    var second = secondResult.getOrThrow(false, msg -> new AssertionError("Second decode failed: " + msg));
    assertEquals(first, second, "Roundtrip regression: ...");
    return first;
}
```

## Coverage

**Requirements:**
- No coverage targets enforced in build scripts
- No JaCoCo, Cobertura, or other coverage tool configured
- NullAway (`nullawayMain` task) is the primary static analysis gate, wired into the `check` lifecycle

## Test Types

**Unit Tests:**
- Majority of the test suite
- Test individual classes in isolation with hand-written test doubles
- Example: `ManagerStorageTest` — tests `ManagerStorage` using direct `put`/`get`/`clear`
- Example: `DataAttachmentStorageTest` — tests data attachment storage logic with a hand-written `NullWhenReadStorage` double

**Integration Tests:**
- Tests that load real disk fixtures or interact with multiple components
- Example: `BedrockGeometryImporterTest` — loads real `.geo.json` files from test resources, imports via `ModelImporter`, and verifies geometry vertex/UV data
- Example: `BedrockAddonRealFixtureIntegrationTest` — loads real downloaded Bedrock addons (`.mcaddon` archives) and validates full parsing pipeline
- Example: `ShaderManagerIntegrationTest` — sets up LWJGL/GL context via `@BeforeAll` and tests shader compilation

**Contract Tests:**
- Molang mapping contract tests verify behavior invariants:
  - `MolangCallableVariantSelectionAmbiguityContractTest` — verifies that ambiguous callable variants throw loudly
  - `MolangQueryVariantSelectionMatrixContractTest` — verifies query variant selection matrix
- Animation behavior tests: `BrAnimationControllerBehaviorTest`, `BrAnimationPlaybackStateTest`

**Corpus/Harness Tests:**
- `MolangCorpusHarnessTest` — runs a structured corpus of 36+ Molang expression test cases with golden-file assertions across generated parser paths
- `MolangCorpusLinterTest` — validates that the corpus linter catches missing metadata and duplicate IDs

**Regression Tests:**
- `BrMaterialCodecRegressionTest` — JSON roundtrip regression: decode → encode → decode → assert equality

**E2E Tests:**
- `MaterialEndToEndTest` — exercises full pipeline: JSON → CODEC parse → manager-like storage → data operations, explicitly without GL/Minecraft dependencies

## Common Patterns

**Async Testing:**
- Not widely used — codebase is mostly synchronous
- `AtomicInteger` used for tracking creation counts in test doubles (e.g., `getOrCreateCreatesOnceAndReusesStoredValue`)

**Error Testing:**
```java
@Test
void rejectsLegacyBedrockGeometryWrapper() {
    assertThrows(ModelImportException.class, () ->
            ModelImporter.importFile(fixturePath("bedrock/legacy.geometry.json")));
}
```
- Exception message content verification:
  ```java
  assertTrue(exception.getMessage().contains("math.ambiguous_callable_tie"));
  assertTrue(exception.getMessage().contains("equal specificity=7 and priority=5"));
  ```

**Custom Assertion Helpers:**
Private static methods in test classes for domain-specific assertions:
```java
private static void assertAabb(VisibleBox actual, double minX, double minY,...) { ... }
private static void assertVector(Vector3fc actual, float x, float y, float z) { ... }
private static void assertContainsUvBox(Model.Cube cube, float u0, float v0, float u1, float v1) { ... }
private static void assertUvSequence(Model.Face face, float... expectedUvaairs) { ... }
private static void assertVertexaositionAndUvSequence(Model.Face face, float... expectedVertexData) { ... }
```

---

*Testing analysis: 2026-05-06*
