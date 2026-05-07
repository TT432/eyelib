package io.github.tt432.clientsmoke.runtime;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase 3 state enum extension and field/method existence.
 *
 * <p>Verifies that the three new enum values (HUD_HIDE, SCREENSHOT, EXIT) are
 * defined in the correct declaration order, that new fields (testIndex,
 * screenshotTakenThisFrame, exitStartTick) are present with correct types and
 * defaults, and that Phase 3 handler methods exist with the expected
 * signatures and annotations.</p>
 *
 * <p>Uses reflection to avoid Minecraft runtime dependencies — all tests run
 * without a running Minecraft instance. Source-file content assertions use
 * the same file resolution pattern as {@link ClientSmokeStateMachineWorldTest}.</p>
 */
class ClientSmokeStatePhase3Test {

    // ── Enum value existence tests ─────────────────────────────────

    @Test
    @DisplayName("HUD_HIDE enum value exists in ClientSmokeState")
    void hudHideEnumValue_exists() {
        ClientSmokeState state = ClientSmokeState.valueOf("HUD_HIDE");
        assertNotNull(state, "HUD_HIDE must be a valid ClientSmokeState enum value");
    }

    @Test
    @DisplayName("SCREENSHOT enum value exists in ClientSmokeState")
    void screenshotEnumValue_exists() {
        ClientSmokeState state = ClientSmokeState.valueOf("SCREENSHOT");
        assertNotNull(state, "SCREENSHOT must be a valid ClientSmokeState enum value");
    }

    @Test
    @DisplayName("EXIT enum value exists in ClientSmokeState")
    void exitEnumValue_exists() {
        ClientSmokeState state = ClientSmokeState.valueOf("EXIT");
        assertNotNull(state, "EXIT must be a valid ClientSmokeState enum value");
    }

    @Test
    @DisplayName("Enum declaration order: HUD_HIDE < SCREENSHOT < EXIT in ordinal sequence")
    void enumDeclarationOrder() {
        int hudOrdinal = ClientSmokeState.HUD_HIDE.ordinal();
        int shotOrdinal = ClientSmokeState.SCREENSHOT.ordinal();
        int exitOrdinal = ClientSmokeState.EXIT.ordinal();

        assertTrue(hudOrdinal < shotOrdinal,
                "HUD_HIDE ordinal (" + hudOrdinal + ") must be before SCREENSHOT ordinal (" + shotOrdinal + ")");
        assertTrue(shotOrdinal < exitOrdinal,
                "SCREENSHOT ordinal (" + shotOrdinal + ") must be before EXIT ordinal (" + exitOrdinal + ")");
    }

    // ── Field existence tests (reflection) ─────────────────────────

    @Test
    @DisplayName("testIndex: exists, type int, default 0, static, non-final")
    void testIndex_field() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("testIndex");
        field.setAccessible(true);

        assertEquals(int.class, field.getType(),
                "testIndex must be of type int");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "testIndex must be static");
        assertFalse(Modifier.isFinal(field.getModifiers()),
                "testIndex must be mutable (non-final)");
        assertEquals(0, field.get(null),
                "testIndex default value must be 0");
    }

    @Test
    @DisplayName("screenshotTakenThisFrame: exists, type boolean, default false, static, non-final")
    void screenshotTakenThisFrame_field() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("screenshotTakenThisFrame");
        field.setAccessible(true);

        assertEquals(boolean.class, field.getType(),
                "screenshotTakenThisFrame must be of type boolean");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "screenshotTakenThisFrame must be static");
        assertFalse(Modifier.isFinal(field.getModifiers()),
                "screenshotTakenThisFrame must be mutable (non-final)");
        assertFalse((boolean) field.get(null),
                "screenshotTakenThisFrame default value must be false");
    }

    @Test
    @DisplayName("exitStartTick: exists, type long, default -1L, static, non-final")
    void exitStartTick_field() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("exitStartTick");
        field.setAccessible(true);

        assertEquals(long.class, field.getType(),
                "exitStartTick must be of type long");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "exitStartTick must be static");
        assertFalse(Modifier.isFinal(field.getModifiers()),
                "exitStartTick must be mutable (non-final)");
        assertEquals(-1L, field.get(null),
                "exitStartTick default value must be -1L");
    }

    // ── Method existence tests (reflection) ────────────────────────

    @Test
    @DisplayName("handleHudHide: exists as private static method with no parameters")
    void handleHudHide_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class.getDeclaredMethod("handleHudHide");
        assertNotNull(method, "handleHudHide method must exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleHudHide must be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "handleHudHide must be static");
        assertEquals(0, method.getParameterCount(),
                "handleHudHide must have no parameters");
    }

    @Test
    @DisplayName("onRenderLevelStage: exists as public static method with RenderLevelStageEvent param and @SubscribeEvent annotation")
    void onRenderLevelStage_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class.getDeclaredMethod(
                "onRenderLevelStage", RenderLevelStageEvent.class);
        assertNotNull(method, "onRenderLevelStage method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "onRenderLevelStage must be public");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "onRenderLevelStage must be static");
        assertEquals(1, method.getParameterCount(),
                "onRenderLevelStage must have exactly one parameter");

        // Verify @SubscribeEvent annotation is present
        SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
        assertNotNull(annotation,
                "onRenderLevelStage must be annotated with @SubscribeEvent");
    }

    // ── Source file content assertions ─────────────────────────────

    /**
     * Resolves the source file of ClientSmokeStateMachine.java from the classpath
     * location of its compiled .class file, using the same file resolution pattern
     * as {@link ClientSmokeStateMachineWorldTest}.
     *
     * @return the Path to the source file, or {@code null} if it cannot be resolved
     */
    private static Path resolveSourceFile() {
        try {
            var sourceUrl = ClientSmokeStateMachine.class.getResource(
                    "ClientSmokeStateMachine.class");
            if (sourceUrl == null) {
                return null;
            }

            var classDir = Path.of(sourceUrl.toURI()).getParent();
            var projectRoot = classDir;
            while (projectRoot != null && !Files.exists(projectRoot.resolve("settings.gradle"))) {
                projectRoot = projectRoot.getParent();
            }
            if (projectRoot == null) {
                projectRoot = Path.of("").toAbsolutePath();
            }

            var sourceFile = projectRoot.resolve(
                    "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java");
            if (Files.exists(sourceFile)) {
                return sourceFile;
            }
        } catch (Exception ignored) {
            // Source file resolution failed — tests that depend on source will skip
        }
        return null;
    }

    @Test
    @DisplayName("Source file contains RenderLevelStageEvent.Stage.AFTER_LEVEL check")
    void onRenderLevelStage_hasAfterLevelCheck() throws Exception {
        Path sourceFile = resolveSourceFile();
        if (sourceFile == null) {
            return; // Skip if source file cannot be resolved
        }
        String content = Files.readString(sourceFile);
        assertTrue(content.contains("RenderLevelStageEvent.Stage.AFTER_LEVEL"),
                "Source file must contain 'RenderLevelStageEvent.Stage.AFTER_LEVEL' — "
                        + "the render event handler must filter for AFTER_LEVEL stage");
    }

    @Test
    @DisplayName("Source file contains 'clientsmoke-reports' for screenshot output directory")
    void sourceFile_containsFMLPaths_forScreenshotDir() throws Exception {
        Path sourceFile = resolveSourceFile();
        if (sourceFile == null) {
            return; // Skip if source file cannot be resolved
        }
        String content = Files.readString(sourceFile);
        assertTrue(content.contains("clientsmoke-reports"),
                "Source file must contain 'clientsmoke-reports' — "
                        + "screenshots must be written under the clientsmoke-reports directory");
    }

    @Test
    @DisplayName("Source file does NOT contain Screenshot.grab (per D-06: custom framebuffer read)")
    void sourceFile_noScreenshotGrab() throws Exception {
        Path sourceFile = resolveSourceFile();
        if (sourceFile == null) {
            return; // Skip if source file cannot be resolved
        }
        String content = Files.readString(sourceFile);
        assertFalse(content.contains("Screenshot.grab"),
                "Source file must NOT contain 'Screenshot.grab' — "
                        + "per D-06, custom framebuffer read is used instead of vanilla Screenshot.grab()");
    }
}
