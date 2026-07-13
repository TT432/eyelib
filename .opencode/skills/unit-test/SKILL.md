---
name: unit-test
description: Write and run JUnit 5 unit tests. Use for structural invariants, boundary enforcement, codec round-trips, and null safety checks that do NOT require a running Minecraft client.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

## Test Framework

JUnit Jupiter 5.10.2. No custom runners, no Mockito, no base classes.

Tests are run via Gradle `test` tasks using `eyelib_debug_test`:
```
eyelib_debug_test              # 全 project
eyelib_debug_test version="1.20.1"  # Stonecutter active node
```

## Test File Conventions

| Convention | Rule |
|-----------|------|
| File name | `*Test.java` |
| Class visibility | Package-private (`class FooTest`) |
| Method naming | Descriptive camelCase (`firstAndLastReturnListEnds`) |
| Assertions | Static import from `org.junit.jupiter.api.Assertions.*` |
| Location | Mirror source structure under `src/test/java/` |

## Common Test Patterns

### Pattern A: Pure unit test

Test a utility method by constructing inputs and asserting outputs.

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListAccessorsTest {
    @Test
    void firstAndLastReturnListEnds() {
        List<String> values = List.of("first", "middle", "last");
        assertEquals("first", ListAccessors.first(values));
        assertEquals("last", ListAccessors.last(values));
    }
}
```

### Pattern B: Codec round-trip test

Test datafixerupper Codec serialization/deserialization.

```java
@Test
void parsesAnimationFromCodec() {
    MyType obj = MyType.CODEC.parse(JsonOps.INSTANCE,
        JsonParser.parseString("{ ... }")
    ).getOrThrow(false, AssertionError::new);
    assertNotNull(obj);
    assertEquals(expected, obj.someField());
}
```

### Pattern C: Parameterized test

Test multiple inputs with the same logic.

```java
@ParameterizedTest
@CsvSource(value = {
    "1+2~3.0",
    "5-3~2.0",
    "5/0~0.0"
}, delimiter = '~')
void binaryArithmetic(String expression, float expected) {
    assertEquals(expected, evaluate(expression), 0.0001);
}
```

### Pattern D: Structural boundary test

Assert that source file contents respect module boundaries. Read `.java` files as text and check for allowed/forbidden imports.

```java
@Test
void spawnServiceDoesNotImportRootParticleTypes() throws IOException {
    String source = Files.readString(Path.of(
        "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
    ));
    assertTrue(source.contains("import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;"));
    assertFalse(source.contains("import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;"));
}
```

This pattern is unique to this project. It enforces module boundaries and ownership rules by verifying that production source code follows the declared dependency graph.

### Pattern E: Fixture-based integration test

Load test fixtures from `src/test/resources/` and process them.

```java
@TempDir
Path tempDir;

@Test
void loadsAddonFixture() throws Exception {
    BedrockAddon addon = BedrockAddonLoader.load(pathToFixture());
    assertNotNull(addon);
}
```

## Running Tests

All modules run JUnit via the standard Gradle `test` task. To verify changes:

1. 在 IDEA 里手动 Gradle sync(reimport)（如果修改了 `build.gradle`）
2. Build the project via `eyelib_debug_build`
3. Run the relevant test via `eyelib_debug_test`
