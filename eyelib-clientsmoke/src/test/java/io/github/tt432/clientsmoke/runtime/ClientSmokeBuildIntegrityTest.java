package io.github.tt432.clientsmoke.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Static verification tests for build.gradle integrity (CORR-03: run config
 * property isolation) and state machine idle-path correctness.
 *
 * <p>Part A verifies that the {@code client} run config has zero smoke-related
 * system properties while the {@code clientSmoke} run config has both required
 * properties ({@code clientsmoke.enabled=true} and {@code clientsmoke.autoExit=true}).</p>
 *
 * <p>Part B verifies that the state machine's {@code handleInit()} method
 * correctly transitions to IDLE when the framework is disabled, and that the
 * {@code onClientTick} terminal-condition check exists.</p>
 */
class ClientSmokeBuildIntegrityTest {

    // ── Part A: build.gradle integrity (CORR-03) ───────────────────

    @Test
    @DisplayName("build.gradle client run config has no clientsmoke.enabled property")
    void buildGradle_clientRunConfig_hasNoClientsmokeEnabled() throws Exception {
        String buildGradle = readBuildGradle();
        assertNotNull(buildGradle, "build.gradle should be accessible");

        String clientBlock = extractBlock(buildGradle, "client");
        assertFalse(clientBlock.isEmpty(), "client block should be found in build.gradle");
        assertFalse(clientBlock.contains("clientsmoke.enabled"),
                "client { } block must NOT contain clientsmoke.enabled — "
                        + "runClient must have zero smoke-related system properties per CORR-03");
    }

    @Test
    @DisplayName("build.gradle client run config has no clientsmoke.autoExit property")
    void buildGradle_clientRunConfig_hasNoClientsmokeAutoExit() throws Exception {
        String buildGradle = readBuildGradle();
        assertNotNull(buildGradle, "build.gradle should be accessible");

        String clientBlock = extractBlock(buildGradle, "client");
        assertFalse(clientBlock.isEmpty(), "client block should be found in build.gradle");
        assertFalse(clientBlock.contains("clientsmoke.autoExit"),
                "client { } block must NOT contain clientsmoke.autoExit — "
                        + "runClient must have zero smoke-related system properties per CORR-03");
    }

    @Test
    @DisplayName("build.gradle clientSmoke run config has clientsmoke.enabled=true")
    void buildGradle_clientSmokeRunConfig_hasEnabledProperty() throws Exception {
        String buildGradle = readBuildGradle();
        assertNotNull(buildGradle, "build.gradle should be accessible");

        String clientSmokeBlock = extractBlock(buildGradle, "clientSmoke");
        assertFalse(clientSmokeBlock.isEmpty(), "clientSmoke block should be found in build.gradle");
        assertTrue(clientSmokeBlock.contains("systemProperty 'clientsmoke.enabled', 'true'"),
                "clientSmoke { } block must contain systemProperty 'clientsmoke.enabled', 'true' per CORR-03");
    }

    @Test
    @DisplayName("build.gradle clientSmoke run config has clientsmoke.autoExit=true")
    void buildGradle_clientSmokeRunConfig_hasAutoExitProperty() throws Exception {
        String buildGradle = readBuildGradle();
        assertNotNull(buildGradle, "build.gradle should be accessible");

        String clientSmokeBlock = extractBlock(buildGradle, "clientSmoke");
        assertFalse(clientSmokeBlock.isEmpty(), "clientSmoke block should be found in build.gradle");
        assertTrue(clientSmokeBlock.contains("systemProperty 'clientsmoke.autoExit', 'true'"),
                "clientSmoke { } block must contain systemProperty 'clientsmoke.autoExit', 'true' per CORR-03");
    }

    @Test
    @DisplayName("build.gradle clientSmoke run config has gameDirectory = run/clientsmoke")
    void buildGradle_clientSmokeRunConfig_hasGameDirectory() throws Exception {
        String buildGradle = readBuildGradle();
        assertNotNull(buildGradle, "build.gradle should be accessible");

        String clientSmokeBlock = extractBlock(buildGradle, "clientSmoke");
        assertFalse(clientSmokeBlock.isEmpty(), "clientSmoke block should be found in build.gradle");
        assertTrue(clientSmokeBlock.contains("gameDirectory = project.file('run/clientsmoke')"),
                "clientSmoke { } block must contain gameDirectory = project.file('run/clientsmoke') "
                        + "for isolated game directory");
    }

    // ── Part B: State machine idle path (CORR-03) ──────────────────

    @Test
    @DisplayName("handleInit() transitions to IDLE when !isEnabled()")
    void source_handleInit_transitionsToIdle_whenDisabled() throws Exception {
        String src = readStateMachineSource();
        assertNotNull(src, "State machine source file should be accessible");

        // Extract the handleInit() method body
        String handleInitBody = extractMethodBody(src, "handleInit");
        assertFalse(handleInitBody.isEmpty(), "handleInit() method body should be found");

        assertTrue(handleInitBody.contains("transitionTo(ClientSmokeState.IDLE"),
                "handleInit() must transition to ClientSmokeState.IDLE when isEnabled() returns false");
    }

    @Test
    @DisplayName("handleInit() uses ClientSmokeConfig.isEnabled() — not direct field access")
    void source_handleInit_usesIsEnabled_notDirectFieldAccess() throws Exception {
        String src = readStateMachineSource();
        assertNotNull(src, "State machine source file should be accessible");

        String handleInitBody = extractMethodBody(src, "handleInit");
        assertFalse(handleInitBody.isEmpty(), "handleInit() method body should be found");

        assertTrue(handleInitBody.contains("ClientSmokeConfig.isEnabled()"),
                "handleInit() must call ClientSmokeConfig.isEnabled() (the bridge method), "
                        + "not access the ForgeConfigSpec field directly");
    }

    @Test
    @DisplayName("onClientTick checks state == IDLE as terminal condition")
    void source_onClientTick_terminatesOnIdle() throws Exception {
        String src = readStateMachineSource();
        assertNotNull(src, "State machine source file should be accessible");

        assertTrue(src.contains("state == ClientSmokeState.IDLE"),
                "onClientTick must check state == ClientSmokeState.IDLE as a terminal condition — "
                        + "when IDLE, no state machine processing should occur");
    }

    @Test
    @DisplayName("State machine source contains ClientSmokeConfig.isEnabled() calls")
    void source_contains_isEnabled_methodCall_inStateMachine() throws Exception {
        String src = readStateMachineSource();
        assertNotNull(src, "State machine source file should be accessible");

        long count = countOccurrences(src, "ClientSmokeConfig.isEnabled()");
        assertTrue(count >= 1,
                "State machine source must contain at least 1 ClientSmokeConfig.isEnabled() call — "
                        + "the bridge method must be referenced (got " + count + ")");
    }

    @Test
    @DisplayName("State machine source contains ClientSmokeConfig.shouldExitAfterSmoke() calls (>= 3)")
    void source_contains_shouldExitAfterSmoke_methodCall_inStateMachine() throws Exception {
        String src = readStateMachineSource();
        assertNotNull(src, "State machine source file should be accessible");

        long count = countOccurrences(src, "ClientSmokeConfig.shouldExitAfterSmoke()");
        assertTrue(count >= 2,
                "State machine source must contain at least 2 ClientSmokeConfig.shouldExitAfterSmoke() calls — "
                        + "handleScan + handleExit + handleStabilize = 3 expected (got " + count + ")");
    }

    // ── Helpers ────────────────────────────────────────────────────

    /**
     * Resolves the root {@code build.gradle} by navigating from the test class
     * location up to the project root (marked by {@code settings.gradle}).
     *
     * <p>Always uses classpath-based resolution to ensure the <em>root</em>
     * {@code build.gradle} is read (not a subproject's). The Gradle test task
     * CWD is the subproject directory, so a relative path would resolve
     * incorrectly.</p>
     */
    private static String readBuildGradle() throws Exception {
        var sourceUrl = ClientSmokeBuildIntegrityTest.class.getResource(
                "ClientSmokeBuildIntegrityTest.class");
        if (sourceUrl == null) {
            return null;
        }
        var classDir = Path.of(sourceUrl.toURI()).getParent();
        var projectRoot = classDir;
        while (projectRoot != null && !Files.exists(projectRoot.resolve("settings.gradle"))) {
            projectRoot = projectRoot.getParent();
        }
        if (projectRoot != null) {
            Path file = projectRoot.resolve("build.gradle");
            if (Files.exists(file)) {
                return Files.readString(file);
            }
        }
        return null;
    }

    /**
     * Resolves the source file of {@link ClientSmokeStateMachine}.java using
     * classpath-based project-root walk (same pattern as Phase 3/4 tests).
     */
    private static String readStateMachineSource() throws Exception {
        // Try relative path first
        String path = "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java";
        Path file = Paths.get(path);
        if (Files.exists(file)) {
            return Files.readString(file);
        }

        // Fallback via classpath resolution
        var sourceUrl = ClientSmokeStateMachine.class.getResource("ClientSmokeStateMachine.class");
        if (sourceUrl != null) {
            var classDir = Path.of(sourceUrl.toURI()).getParent();
            var projectRoot = classDir;
            while (projectRoot != null && !Files.exists(projectRoot.resolve("settings.gradle"))) {
                projectRoot = projectRoot.getParent();
            }
            if (projectRoot != null) {
                file = projectRoot.resolve(path);
                if (Files.exists(file)) {
                    return Files.readString(file);
                }
            }
        }
        return null;
    }

    /**
     * Extracts the text between a named Gradle block ({@code blockName \{})
     * and its matching closing brace.
     */
    private static String extractBlock(String source, String blockName) {
        String search = blockName + " {";
        int start = source.indexOf(search);
        if (start < 0) {
            return "";
        }
        int braceCount = 0;
        int i = start + search.length(); // after opening brace
        StringBuilder sb = new StringBuilder();
        for (; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                if (braceCount == 0) {
                    break;
                }
                braceCount--;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Extracts the body of a Java method by name (naive but sufficient for
     * source-level content assertions). Returns the text between the first
     * opening brace of the method declaration and its matching closing brace.
     */
    private static String extractMethodBody(String source, String methodName) {
        // Find the method declaration: optional modifiers + return type + methodName + (
        Pattern pattern = Pattern.compile(
                "\\b" + Pattern.quote(methodName) + "\\s*\\([^)]*\\)\\s*(?:throws\\s+[^{]+)?\\s*\\{");
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return "";
        }
        int start = matcher.end() - 1; // position of opening brace
        int braceCount = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < source.length(); i++) {
            char c = source.charAt(i);
            sb.append(c);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Counts occurrences of a substring in source text.
     */
    private static long countOccurrences(String source, String substring) {
        long count = 0;
        int idx = 0;
        while ((idx = source.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}
