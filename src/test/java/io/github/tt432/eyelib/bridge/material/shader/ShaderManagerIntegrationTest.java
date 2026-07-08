package io.github.tt432.eyelib.bridge.material.shader;

import io.github.tt432.eyelib.bridge.material.shader.adapter.ShaderManager;

import com.mojang.blaze3d.systems.RenderSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TT432
 */
class ShaderManagerIntegrationTest {

    private static final String VERT_RESOURCE = "assets/eyelibmaterial/shaders/pass_through.vert";
    private static final String FRAG_RESOURCE = "assets/eyelibmaterial/shaders/pass_through.frag";

    private static final String PASSTHROUGH_VERT =
            "void main() {\n" +
            "    gl_Position = ftransform();\n" +
            "}\n";

    private static final String PASSTHROUGH_FRAG =
            "void main() {\n" +
            "    gl_FragColor = vec4(1.0);\n" +
            "}\n";

    private static final String INVALID_FRAG =
            "void main() {\n" +
            "    gl_FragColor = nonexistentVariable;\n" +
            "}\n";

    private static Method injectDefinesMethod;
    private static Method buildCacheKeyMethod;

    @BeforeAll
    static void reflectPrivateHelpers() throws Exception {
        injectDefinesMethod = ShaderManager.class.getDeclaredMethod("injectDefines", String.class, List.class);
        injectDefinesMethod.setAccessible(true);

        buildCacheKeyMethod = ShaderManager.class.getDeclaredMethod("buildCacheKey", String.class, String.class, List.class);
        buildCacheKeyMethod.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        if (RenderSystem.isOnRenderThread()) {
            ShaderManager.release();
        }
    }

    @Test
    @DisplayName("loadFromResource loads .vert file from test resources and returns non-empty source")
    void loadFromResource_validVertexShader_returnsSource() {
        String source = ShaderManager.loadFromResource(VERT_RESOURCE);
        assertTrue(source != null && !source.isBlank(),
                "Vertex shader source should be non-empty");
        assertTrue(source.contains("ftransform"),
                "Should contain expected GLSL content");
    }

    @Test
    @DisplayName("loadFromResource loads .frag file from test resources and returns non-empty source")
    void loadFromResource_validFragmentShader_returnsSource() {
        String source = ShaderManager.loadFromResource(FRAG_RESOURCE);
        assertTrue(source != null && !source.isBlank(),
                "Fragment shader source should be non-empty");
        assertTrue(source.contains("gl_FragColor"),
                "Should contain expected GLSL content");
    }

    @Test
    @DisplayName("loadFromResource with nonexistent path throws RuntimeException")
    void loadFromResource_invalidPath_throwsRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> ShaderManager.loadFromResource("assets/eyelibmaterial/shaders/nonexistent.vert"));
    }

    @Test
    @DisplayName("injectDefines with no defines returns source unchanged")
    void injectDefines_noDefines_returnsSourceUnchanged() throws Exception {
        String result = (String) injectDefinesMethod.invoke(null, PASSTHROUGH_VERT, Collections.emptyList());
        assertEquals(PASSTHROUGH_VERT, result, "Source without defines should be returned as-is");
    }

    @Test
    @DisplayName("injectDefines with null defines returns source unchanged")
    void injectDefines_nullDefines_returnsSourceUnchanged() throws Exception {
        String result = (String) injectDefinesMethod.invoke(null, PASSTHROUGH_VERT, (List<String>) null);
        assertEquals(PASSTHROUGH_VERT, result, "Source with null defines should be returned as-is");
    }

    @Test
    @DisplayName("injectDefines prepends #define directives before source without #version")
    void injectDefines_withDefines_prependsDirectives() throws Exception {
        List<String> defines = List.of("USE_NORMAL_MAP", "MAX_LIGHTS 4");
        String result = (String) injectDefinesMethod.invoke(null, PASSTHROUGH_VERT, defines);

        assertTrue(result.startsWith("#define USE_NORMAL_MAP\n#define MAX_LIGHTS 4\n"),
                "Should prepend #define lines: " + result);
        assertTrue(result.endsWith(PASSTHROUGH_VERT),
                "Original source should appear after #define lines");
    }

    @Test
    @DisplayName("injectDefines inserts directives after #version line")
    void injectDefines_withVersion_insertsAfterVersion() throws Exception {
        String source = "#version 150\nvoid main() {}\n";
        String result = (String) injectDefinesMethod.invoke(null, source, List.of("USE_COLOR"));

        assertTrue(result.startsWith("#version 150\n#define USE_COLOR\n"),
                "Defines must appear after #version: " + result);
    }

    @Test
    @DisplayName("buildCacheKey differs when defines differ")
    void buildCacheKey_differentDefines_differentKeys() throws Exception {
        String keyA = (String) buildCacheKeyMethod.invoke(null, PASSTHROUGH_VERT, PASSTHROUGH_FRAG, List.of("FOO"));
        String keyB = (String) buildCacheKeyMethod.invoke(null, PASSTHROUGH_VERT, PASSTHROUGH_FRAG, List.of("BAR"));

        assertNotEquals(keyA, keyB, "Different defines should produce different cache keys");
    }

    @Test
    @DisplayName("buildCacheKey is same when sources and defines match")
    void buildCacheKey_sameInputs_sameKey() throws Exception {
        String keyA = (String) buildCacheKeyMethod.invoke(null, PASSTHROUGH_VERT, PASSTHROUGH_FRAG, List.of("FOO"));
        String keyB = (String) buildCacheKeyMethod.invoke(null, PASSTHROUGH_VERT, PASSTHROUGH_FRAG, List.of("FOO"));

        assertEquals(keyA, keyB, "Same inputs should produce identical cache keys");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("compileAndLink with same sources returns cached program on second call")
    void compileAndLink_sameInputs_secondCallReturnsCached() {
        RenderSystem.assertOnRenderThread();

        int first = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, Collections.emptyList());
        assertTrue(first > 0, "First compile should return valid program ID");

        int second = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, Collections.emptyList());
        assertEquals(first, second, "Second call should return cached program ID");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("compileAndLink with invalid fragment shader throws RuntimeException with error log")
    void compileAndLink_invalidShader_throwsRuntimeException() {
        RenderSystem.assertOnRenderThread();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ShaderManager.compileAndLink(PASSTHROUGH_VERT, INVALID_FRAG, Collections.emptyList()));

        assertTrue(ex.getMessage() != null && !ex.getMessage().isBlank(),
                "Exception message should contain GLSL error log");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("compile → cache → release → recompile produces new program ID")
    void compileAndLink_programLifecycle_releaseAndRecompile() {
        RenderSystem.assertOnRenderThread();

        int first = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, Collections.emptyList());
        assertTrue(first > 0, "First compile should return valid program ID");

        ShaderManager.releaseProgram(first);

        int recompiled = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, Collections.emptyList());
        assertTrue(recompiled > 0, "Recompiled program should be valid");
        assertNotEquals(first, recompiled,
                "After release + recompile, program ID should be different (cache cleared for that entry)");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("release() clears all cached programs, next compile produces new ID")
    void compileAndLink_fullRelease_clearsAllCache() {
        RenderSystem.assertOnRenderThread();

        int first = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, Collections.emptyList());
        assertTrue(first > 0, "First compile should return valid program ID");

        ShaderManager.release();

        int afterRelease = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, Collections.emptyList());
        assertTrue(afterRelease > 0, "Recompile after full release should return valid program ID");
        assertNotEquals(first, afterRelease,
                "After release(), cache is empty so next compile should produce a new program");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("Same vertex/fragment source with different defines produces different programs")
    void compileAndLink_sameSourceDifferentDefines_differentPrograms() {
        RenderSystem.assertOnRenderThread();

        int progA = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, List.of("FOO"));
        int progB = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, List.of("BAR"));

        assertTrue(progA > 0);
        assertTrue(progB > 0);
        assertNotEquals(progA, progB,
                "Different defines should produce distinct cached programs");
    }
}
