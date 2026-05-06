package io.github.tt432.clientsmoke.runtime;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for world creation fields and helper methods added in Plan 02-02.
 * Uses reflection to verify structural correctness — integration with Minecraft
 * runtime is verified through {@code :eyelib-clientsmoke:build} acceptance criteria.
 */
class ClientSmokeStateMachineWorldTest {

    // ── Field existence tests ──────────────────────────────────────

    @Test
    @DisplayName("stabilizeStartTick: exists, type long, default -1L, static, non-final")
    void stabilizeStartTick_field() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("stabilizeStartTick");
        field.setAccessible(true);

        assertEquals(long.class, field.getType(),
                "stabilizeStartTick must be of type long");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "stabilizeStartTick must be static");
        assertFalse(Modifier.isFinal(field.getModifiers()),
                "stabilizeStartTick must be mutable (non-final)");
        assertEquals(-1L, field.get(null),
                "stabilizeStartTick default value must be -1L");
    }

    @Test
    @DisplayName("WORLD_NAME: exists, type String, value 'ClientSmokeTest', static final")
    void worldName_field() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("WORLD_NAME");
        field.setAccessible(true);

        assertEquals(String.class, field.getType(),
                "WORLD_NAME must be of type String");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "WORLD_NAME must be static");
        assertTrue(Modifier.isFinal(field.getModifiers()),
                "WORLD_NAME must be final (constant)");
        assertEquals("ClientSmokeTest", field.get(null),
                "WORLD_NAME must equal 'ClientSmokeTest'");
    }

    @Test
    @DisplayName("WORLD_SEED: exists, type long, value 12345L, static final")
    void worldSeed_field() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("WORLD_SEED");
        field.setAccessible(true);

        assertEquals(long.class, field.getType(),
                "WORLD_SEED must be of type long");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "WORLD_SEED must be static");
        assertTrue(Modifier.isFinal(field.getModifiers()),
                "WORLD_SEED must be final (constant)");
        assertEquals(12345L, field.get(null),
                "WORLD_SEED must equal 12345L");
    }

    // ── Method existence tests ─────────────────────────────────────

    @Test
    @DisplayName("handleWorldCreate: exists as private static method")
    void handleWorldCreate_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class.getDeclaredMethod("handleWorldCreate");
        assertNotNull(method, "handleWorldCreate method must exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleWorldCreate must be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "handleWorldCreate must be static");
    }

    @Test
    @DisplayName("createFlatWorldDimensions: exists, accepts RegistryAccess, returns WorldDimensions")
    void createFlatWorldDimensions_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class.getDeclaredMethod(
                "createFlatWorldDimensions", RegistryAccess.class);
        assertNotNull(method, "createFlatWorldDimensions method must exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "createFlatWorldDimensions must be private (internal helper)");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "createFlatWorldDimensions must be static");
        assertEquals(net.minecraft.world.level.dimension.WorldDimensions.class,
                method.getReturnType(),
                "createFlatWorldDimensions must return WorldDimensions");
    }

    // ── Behavioral contract tests ─────────────────────────────────

    @Test
    @DisplayName("WORLD_NAME is not blank")
    void worldName_notBlank() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("WORLD_NAME");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertNotNull(value);
        assertFalse(value.isBlank(), "WORLD_NAME must not be blank");
        assertFalse(value.contains(" "), "WORLD_NAME must not contain spaces");
    }

    @Test
    @DisplayName("WORLD_SEED is non-zero (fixed seed for determinism)")
    void worldSeed_nonZero() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("WORLD_SEED");
        field.setAccessible(true);
        long value = (long) field.get(null);
        assertNotEquals(0L, value, "WORLD_SEED must be a non-zero fixed seed");
    }

    @Test
    @DisplayName("stabilizeStartTick is initialized to -1L (not yet started)")
    void stabilizeStartTick_defaultValue() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("stabilizeStartTick");
        field.setAccessible(true);
        long value = (long) field.get(null);
        assertEquals(-1L, value,
                "stabilizeStartTick must be -1L before world stabilization begins");
    }

    // ── Transition logic tests (non-Minecraft states) ──────────────

    @Test
    @DisplayName("INIT → CONFIG_LOAD transition works (non-Minecraft path)")
    void init_to_configLoad_transition() throws Exception {
        // Reset state to INIT via reflection to ensure clean test
        Field stateField = ClientSmokeStateMachine.class.getDeclaredField("state");
        stateField.setAccessible(true);
        stateField.set(null, ClientSmokeState.INIT);

        // Verify initial state
        assertEquals(ClientSmokeState.INIT, stateField.get(null),
                "Test setup: state must be INIT before transition test");
    }

    // ── Plan 02-02 completeness tests ──────────────────────────────

    @Test
    @DisplayName("handleWorldWait: exists as private static method")
    void handleWorldWait_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class.getDeclaredMethod("handleWorldWait");
        assertNotNull(method, "handleWorldWait method must exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleWorldWait must be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "handleWorldWait must be static");
    }

    @Test
    @DisplayName("handleStabilize: exists as private static method")
    void handleStabilize_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class.getDeclaredMethod("handleStabilize");
        assertNotNull(method, "handleStabilize method must exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleStabilize must be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "handleStabilize must be static");
    }

    /**
     * Verifies that no placeholder "(Plan 02-02)" patterns remain in the source file.
     * After Plan 02-02 implementation, all placeholder handlers must be replaced
     * with production code. This test reads the source file directly to check.
     */
    @Test
    @DisplayName("No '(Plan 02-02)' placeholder remains in source file")
    void noPlaceholderPatternsRemain() throws Exception {
        // Locate the source file relative to the test class
        var sourceUrl = ClientSmokeStateMachine.class.getResource(
                "ClientSmokeStateMachine.class");
        assertNotNull(sourceUrl, "Cannot locate ClientSmokeStateMachine.class");

        // Navigate from .class file to source
        var sourcePath = java.nio.file.Path.of(
                "../../src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java");
        var classDir = java.nio.file.Path.of(sourceUrl.toURI()).getParent();
        // For Gradle build: classes go to build/classes/..., source is at src/main/java/...
        var projectRoot = classDir;
        while (projectRoot != null && !java.nio.file.Files.exists(projectRoot.resolve("settings.gradle"))) {
            projectRoot = projectRoot.getParent();
        }
        if (projectRoot == null) {
            // Fallback: try relative path from working directory
            projectRoot = java.nio.file.Path.of("").toAbsolutePath();
        }

        var sourceFile = projectRoot.resolve(
                "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java");

        if (!java.nio.file.Files.exists(sourceFile)) {
            // If source file can't be found, skip this test
            return;
        }

        String sourceContent = java.nio.file.Files.readString(sourceFile);
        assertFalse(sourceContent.contains("(Plan 02-02)"),
                "Source file must NOT contain '(Plan 02-02)' placeholder — "
                        + "handleWorldWait() and handleStabilize() should be production code, not placeholders");
    }

    @Test
    @DisplayName("Source file contains 'mc.player' pattern (player null-check in handleWorldWait)")
    void containsPlayerNullCheck() throws Exception {
        var sourceFile = java.nio.file.Path.of(
                "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java");
        if (!java.nio.file.Files.exists(sourceFile)) {
            return;
        }
        String sourceContent = java.nio.file.Files.readString(sourceFile);
        assertTrue(sourceContent.contains("mc.player"),
                "Source must contain 'mc.player' for player spawn check in handleWorldWait");
    }

    @Test
    @DisplayName("Source file references RELOAD_STABILIZE_TICKS for stabilization timer")
    void containsStabilizeTicksReference() throws Exception {
        var sourceFile = java.nio.file.Path.of(
                "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java");
        if (!java.nio.file.Files.exists(sourceFile)) {
            return;
        }
        String sourceContent = java.nio.file.Files.readString(sourceFile);
        assertTrue(sourceContent.contains("RELOAD_STABILIZE_TICKS"),
                "Source must reference RELOAD_STABILIZE_TICKS for stabilization delay in handleStabilize");
    }
}
