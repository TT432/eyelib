package io.github.tt432.eyelibmaterial.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TT432
 */
@NullMarked
class ShaderManagerTest {

    /** Minimal ARB-compatible vertex shader that passes through vertices. */
    private static final String PASSTHROUGH_VERT = """
            void main() {
                gl_Position = ftransform();
            }
            """;

    /** Minimal ARB-compatible fragment shader that outputs solid white. */
    private static final String PASSTHROUGH_FRAG = """
            void main() {
                gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
            }
            """;

    /** Invalid shader source guaranteed to fail compilation. */
    private static final String INVALID_FRAG = """
            void main() {
                gl_FragColor = nonexistentVariable;
            }
            """;

    @AfterEach
    void tearDown() {
        if (RenderSystem.isOnRenderThread()) {
            ShaderManager.release();
        }
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("Compile valid pass-through shaders → non-zero program, cache hit on second call")
    void compileAndLink_validShaders_returnsProgramAndCaches() {
        RenderSystem.assertOnRenderThread();

        List<String> defines = Collections.emptyList();

        int program1 = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, defines);
        assertTrue(program1 > 0, "Expected positive program ID, got " + program1);

        int program2 = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, defines);
        assertEquals(program1, program2, "Second call should return cached program ID");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("Compile with defines → different defines produce different programs")
    void compileAndLink_withDefines_differentDefinesProduceDifferentPrograms() {
        RenderSystem.assertOnRenderThread();

        List<String> definesA = List.of("FOO");
        List<String> definesB = List.of("BAR");

        int progA = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, definesA);
        int progB = ShaderManager.compileAndLink(PASSTHROUGH_VERT, PASSTHROUGH_FRAG, definesB);

        assertTrue(progA > 0);
        assertTrue(progB > 0);
        assertTrue(progA != progB, "Different defines should produce different cached programs");
    }

    @Test
    @Disabled("Requires GL context — enable when running with a Forge client")
    @DisplayName("Compile invalid shader → RuntimeException with non-empty message")
    void compileAndLink_invalidFragmentShader_throwsRuntimeException() {
        RenderSystem.assertOnRenderThread();

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> ShaderManager.compileAndLink(PASSTHROUGH_VERT, INVALID_FRAG, Collections.emptyList())
        );

        assertTrue(ex.getMessage() != null && !ex.getMessage().isBlank(),
                "Exception message should contain the GLSL error log, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("loadFromResource with valid path returns non-empty string")
    void loadFromResource_validPath_returnsContent() {
        // mods.toml is guaranteed to exist in the eyelib-material resource tree
        String content = ShaderManager.loadFromResource("META-INF/mods.toml");
        assertTrue(!content.isBlank(), "Should load mods.toml content");
    }

    @Test
    @DisplayName("loadFromResource with nonexistent path throws RuntimeException")
    void loadFromResource_nonexistentPath_throwsRuntimeException() {
        assertThrows(
                RuntimeException.class,
                () -> ShaderManager.loadFromResource("nonexistent/shader.vert")
        );
    }
}