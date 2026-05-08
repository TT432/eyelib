package io.github.tt432.clientsmoke.runtime;

import io.github.tt432.clientsmoke.config.ClientSmokeConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Static verification tests for the system property override bridge
 * ({@link ClientSmokeConfig#isEnabled()} and {@link ClientSmokeConfig#shouldExitAfterSmoke()}).
 *
 * <p>Verifies method existence (reflection), system property behavior (set prop → call → assert),
 * and source file content assertions (fallback patterns). Per CORR-03.</p>
 */
class ClientSmokeConfigBridgeTest {

    // ── Lifecycle: system property cleanup ──

    @BeforeEach
    void clearSystemPropertiesBefore() {
        System.clearProperty("clientsmoke.enabled");
        System.clearProperty("clientsmoke.autoExit");
    }

    @AfterEach
    void clearSystemPropertiesAfter() {
        System.clearProperty("clientsmoke.enabled");
        System.clearProperty("clientsmoke.autoExit");
    }

    // ── Method existence (reflection) ──

    @Test
    @DisplayName("isEnabled() method exists — public static returns boolean, zero params")
    void isEnabled_methodExists_publicStaticReturnsBoolean() throws Exception {
        Method method = getDeclaredMethod(ClientSmokeConfig.class, "isEnabled");
        assertNotNull(method, "ClientSmokeConfig.isEnabled() should exist");
        assertEquals(boolean.class, method.getReturnType(),
                "isEnabled() should return boolean");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "isEnabled() should be public");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "isEnabled() should be static");
        assertEquals(0, method.getParameterCount(),
                "isEnabled() should have zero parameters");
    }

    @Test
    @DisplayName("shouldExitAfterSmoke() method exists — public static returns boolean, zero params")
    void shouldExitAfterSmoke_methodExists_publicStaticReturnsBoolean() throws Exception {
        Method method = getDeclaredMethod(ClientSmokeConfig.class, "shouldExitAfterSmoke");
        assertNotNull(method, "ClientSmokeConfig.shouldExitAfterSmoke() should exist");
        assertEquals(boolean.class, method.getReturnType(),
                "shouldExitAfterSmoke() should return boolean");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "shouldExitAfterSmoke() should be public");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "shouldExitAfterSmoke() should be static");
        assertEquals(0, method.getParameterCount(),
                "shouldExitAfterSmoke() should have zero parameters");
    }

    // ── System property behavior ──

    @Test
    @DisplayName("isEnabled() returns true when clientsmoke.enabled=true")
    void isEnabled_returnsTrue_whenSystemPropertySetToTrue() {
        System.setProperty("clientsmoke.enabled", "true");
        try {
            assertTrue(ClientSmokeConfig.isEnabled(),
                    "isEnabled() should return true when system property is 'true'");
        } finally {
            System.clearProperty("clientsmoke.enabled");
        }
    }

    @Test
    @DisplayName("isEnabled() returns false when clientsmoke.enabled=false")
    void isEnabled_returnsFalse_whenSystemPropertySetToFalse() {
        System.setProperty("clientsmoke.enabled", "false");
        try {
            assertFalse(ClientSmokeConfig.isEnabled(),
                    "isEnabled() should return false when system property is 'false'");
        } finally {
            System.clearProperty("clientsmoke.enabled");
        }
    }

    @Test
    @DisplayName("shouldExitAfterSmoke() returns true when clientsmoke.autoExit=true")
    void shouldExitAfterSmoke_returnsTrue_whenSystemPropertySetToTrue() {
        System.setProperty("clientsmoke.autoExit", "true");
        try {
            assertTrue(ClientSmokeConfig.shouldExitAfterSmoke(),
                    "shouldExitAfterSmoke() should return true when system property is 'true'");
        } finally {
            System.clearProperty("clientsmoke.autoExit");
        }
    }

    @Test
    @DisplayName("shouldExitAfterSmoke() returns false when clientsmoke.autoExit=false")
    void shouldExitAfterSmoke_returnsFalse_whenSystemPropertySetToFalse() {
        System.setProperty("clientsmoke.autoExit", "false");
        try {
            assertFalse(ClientSmokeConfig.shouldExitAfterSmoke(),
                    "shouldExitAfterSmoke() should return false when system property is 'false'");
        } finally {
            System.clearProperty("clientsmoke.autoExit");
        }
    }

    // ── Source file content assertions ──

    @Test
    @DisplayName("Source contains System.getProperty(\"clientsmoke.enabled\")")
    void source_containsSystemGetProperty_clientsmokeEnabled() throws Exception {
        String src = readConfigSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("System.getProperty(\"clientsmoke.enabled\")"),
                "Source should contain System.getProperty(\"clientsmoke.enabled\") for the system property bridge");
    }

    @Test
    @DisplayName("Source contains System.getProperty(\"clientsmoke.autoExit\")")
    void source_containsSystemGetProperty_clientsmokeAutoExit() throws Exception {
        String src = readConfigSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("System.getProperty(\"clientsmoke.autoExit\")"),
                "Source should contain System.getProperty(\"clientsmoke.autoExit\") for the system property bridge");
    }

    @Test
    @DisplayName("Source contains Boolean.parseBoolean for system property parsing")
    void source_containsBooleanParseBoolean() throws Exception {
        String src = readConfigSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("Boolean.parseBoolean"),
                "Source should contain Boolean.parseBoolean for parsing system property values");
    }

    @Test
    @DisplayName("Source contains ENABLED.get() fallback")
    void source_containsENABLED_get_fallback() throws Exception {
        String src = readConfigSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("ENABLED.get()"),
                "Source should contain ENABLED.get() as the ForgeConfigSpec fallback in isEnabled()");
    }

    @Test
    @DisplayName("Source contains EXIT_AFTER_SMOKE.get() fallback")
    void source_containsEXIT_AFTER_SMOKE_get_fallback() throws Exception {
        String src = readConfigSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("EXIT_AFTER_SMOKE.get()"),
                "Source should contain EXIT_AFTER_SMOKE.get() as the ForgeConfigSpec fallback in shouldExitAfterSmoke()");
    }

    // ── Helpers ──

    private static Method getDeclaredMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private static String readConfigSourceFile() throws Exception {
        String path = "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/config/ClientSmokeConfig.java";
        Path file = Paths.get(path);
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        // Fallback: resolve from classpath via settings.gradle marker
        var sourceUrl = ClientSmokeConfig.class.getResource("ClientSmokeConfig.class");
        if (sourceUrl != null) {
            var classDir = Path.of(sourceUrl.toURI()).getParent();
            var projectRoot = classDir;
            while (projectRoot != null && !Files.exists(projectRoot.resolve("settings.gradle"))) {
                projectRoot = projectRoot.getParent();
            }
            if (projectRoot != null) {
                file = projectRoot.resolve(
                        "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/config/ClientSmokeConfig.java");
                if (Files.exists(file)) return Files.readString(file);
            }
        }
        return null;
    }
}
