package io.github.tt432.eyelibmaterial.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

/**
 * 使用ARB扩展加载、编译、链接并缓存GLSL顶点+片段着色器程序。
 * 所有入口方法都断言在渲染线程上执行。着色器程序按顶点/片段源码哈希和defines组成的键来缓存。
 *
 * @author TT432
 */
@NullMarked
public final class ShaderManager {

    private static final Map<String, Integer> PROGRAM_CACHE = new HashMap<>();

    private ShaderManager() {
        // 工具类，禁止实例化
    }

    /**
     * Compiles and links a vertex+fragment shader program with optional preprocessor defines.
     *
     * @param vertSource GLSL vertex shader source
     * @param fragSource GLSL fragment shader source
     * @param defines    list of define tokens (e.g. {@code "USE_NORMAL_MAP"}, {@code "MAX_LIGHTS 4"});
     *                   each token is prepended as {@code #define <token>}
     * @return OpenGL program ID (&gt; 0)
     * @throws RuntimeException if compilation or linking fails
     */
    public static int compileAndLink(String vertSource, String fragSource, List<String> defines) {
        RenderSystem.assertOnRenderThread();

        String key = buildCacheKey(vertSource, fragSource, defines);
        Integer cached = PROGRAM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        String processedVert = injectDefines(vertSource, defines);
        String processedFrag = injectDefines(fragSource, defines);

        // 编译顶点着色器
        int vertShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
        ARBShaderObjects.glShaderSourceARB(vertShader, processedVert);
        ARBShaderObjects.glCompileShaderARB(vertShader);
        checkCompileStatus(vertShader, "vertex");

        // 编译片元着色器
        int fragShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        ARBShaderObjects.glShaderSourceARB(fragShader, processedFrag);
        ARBShaderObjects.glCompileShaderARB(fragShader);
        checkCompileStatus(fragShader, "fragment");

        // 链接着色器程序
        int program = ARBShaderObjects.glCreateProgramObjectARB();
        ARBShaderObjects.glAttachObjectARB(program, vertShader);
        ARBShaderObjects.glAttachObjectARB(program, fragShader);
        ARBShaderObjects.glLinkProgramARB(program);
        checkLinkStatus(program);

        // 清理着色器对象—链接后不再需要
        ARBShaderObjects.glDeleteObjectARB(vertShader);
        ARBShaderObjects.glDeleteObjectARB(fragShader);

        PROGRAM_CACHE.put(key, program);
        return program;
    }

    /**
     * Releases a specific shader program, deleting it from the GPU and removing it from the cache.
     *
     * @param program the OpenGL program ID to release
     */
    public static void releaseProgram(int program) {
        RenderSystem.assertOnRenderThread();
        ARBShaderObjects.glDeleteObjectARB(program);
        PROGRAM_CACHE.values().removeIf(v -> v == program);
    }

    /**
     * Releases all cached shader programs, deleting them from the GPU and clearing the cache.
     */
    public static void release() {
        RenderSystem.assertOnRenderThread();
        PROGRAM_CACHE.values().forEach(ARBShaderObjects::glDeleteObjectARB);
        PROGRAM_CACHE.clear();
    }

    /**
     * Reads a GLSL source file from the classpath.
     *
     * @param path resource path (e.g. {@code "shaders/test.vert"})
     * @return the full file content as a string
     * @throws RuntimeException if the resource is not found or cannot be read
     */
    public static String loadFromResource(String path) {
        try (InputStream is = ShaderManager.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader resource: " + path, e);
        }
    }

    private static String injectDefines(String source, List<String> defines) {
        if (defines == null || defines.isEmpty()) {
            return source;
        }
        StringBuilder sb = new StringBuilder();
        for (String def : defines) {
            sb.append("#define ").append(def).append('\n');
        }
        String defineBlock = sb.toString();
        if (source.startsWith("#version")) {
            int lineEnd = source.indexOf('\n');
            if (lineEnd >= 0) {
                return source.substring(0, lineEnd + 1) + defineBlock + source.substring(lineEnd + 1);
            }
        }
        return defineBlock + source;
    }

    private static String buildCacheKey(String vertSource, String fragSource, List<String> defines) {
        String definePart = defines == null || defines.isEmpty()
                ? ""
                : String.join(",", defines);
        return vertSource.hashCode() + "|" + fragSource.hashCode() + ":" + definePart;
    }

    private static void checkCompileStatus(int shader, String type) {
        if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
            String log = ARBShaderObjects.glGetInfoLogARB(shader);
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw new RuntimeException("Failed to compile " + type + " shader:\n" + log);
        }
    }

    private static void checkLinkStatus(int program) {
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            String log = ARBShaderObjects.glGetInfoLogARB(program);
            ARBShaderObjects.glDeleteObjectARB(program);
            throw new RuntimeException("Failed to link shader program:\n" + log);
        }
    }
}