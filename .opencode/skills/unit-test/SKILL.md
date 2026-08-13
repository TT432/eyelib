---
name: unit-test
description: Write and run JUnit 5 unit tests. Use for structural invariants, boundary enforcement, codec round-trips, and null safety checks that do NOT require a running Minecraft client.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# unit-test

Write and run JUnit 5 unit tests (structural invariants, boundary enforcement, codec round-trips, null safety) that do NOT require a running Minecraft client.

## When to use
- Writing pure unit tests for utility methods: construct inputs, assert outputs (structural invariants / null safety).
- Codec round-trip tests for datafixerupper Codec serialization/deserialization.
- Boundary enforcement: verifying production source respects module boundaries and ownership rules via structural boundary tests.
- Testing multiple inputs with the same logic via parameterized tests.
- Fixture-based integration tests loading fixtures from src/test/resources/.
- Do NOT use when: Tests that require a running Minecraft client (render output, GL state, texture correctness, full-lifecycle integration) — use the smoke-test skill instead.

## Rules
- Name test files `*Test.java`.
- Declare test classes package-private (`class FooTest`).
- Name test methods with descriptive camelCase (e.g. `firstAndLastReturnListEnds`).
- Static-import assertions from `org.junit.jupiter.api.Assertions.*`.
- Mirror the source structure under `src/test/java/`.
- Use JUnit Jupiter 5.10.2 as the test framework.
- NEVER Use custom runners, Mockito, or base classes.
- Run tests via the standard Gradle `test` task, driven through the mcmcp_test tool.
- When Pattern A: Pure unit test:
  - PREFER Test a utility method as a pure unit test by constructing inputs and asserting outputs.
- When Pattern B: Codec round-trip test:
  - PREFER Test datafixerupper Codec serialization/deserialization as a codec round-trip test.
- When Pattern C: Parameterized test:
  - PREFER Test multiple inputs with the same logic as a parameterized test.
- When Pattern D: Structural boundary test:
  - PREFER Enforce module boundaries and ownership rules with a structural boundary test: read `.java` files as text and assert allowed/forbidden imports so production code follows the declared dependency graph.
- When Pattern E: Fixture-based integration test:
  - PREFER Load test fixtures from `src/test/resources/` and process them in fixture-based integration tests.

## Workflow
1. Manually Gradle sync (reimport) in IDEA.
2. Build the project via mcmcp_build.
3. Run the relevant test via mcmcp_test.

<!-- locked residual (verbatim, do not edit) -->
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
```java
@TempDir
Path tempDir;

@Test
void loadsAddonFixture() throws Exception {
    BedrockAddon addon = BedrockAddonLoader.load(pathToFixture());
    assertNotNull(addon);
}
```
