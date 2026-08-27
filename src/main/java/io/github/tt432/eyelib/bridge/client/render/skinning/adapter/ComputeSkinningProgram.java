//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * C1 compute 蒙皮程序（&le;26.1，ADR-0032）：GLSL 430 蒙皮 compute shader 的编译/链接、
 * 全局 palette SSBO（{@code 96×2 mat4 = 12KB}，STREAM_DRAW，逐 draw 两次 subData），
 * 以及单次 dispatch + 内存屏障。
 *
 * <p>能力探测：vanilla GLFW hints 请求 3.2 core，但桌面驱动实际返回最高可用 core 版本
 * （本机 NVIDIA 实测 4.6）；以 {@code GL_VERSION} 字符串解析 + LWJGL 函数指针双重判定。
 * 阈值按需求收紧为 4.6（compute shader 自 4.3 起为核心特性）。
 */
final class ComputeSkinningProgram implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeSkinningProgram.class);

    static final int MIN_MAJOR = 4;
    static final int MIN_MINOR = 6;

    private static final ResourceLocation SHADER_ID =
            ResourceLocationBridge.fromParts("eyelib", "shaders/skinning/skin.comp");
    private static final int PALETTE_BYTES = SkinningLayout.MAX_BONES * SkinningLayout.MAT4_SIZE * 2;
    private static final int NORMAL_OFFSET = SkinningLayout.MAX_BONES * SkinningLayout.MAT4_SIZE;
    private static final int WORKGROUP_SIZE = 64; // 与 skin.comp local_size_x 一致

    private final int programId;
    private final int paletteBuffer;
    private final int vertexCountLocation;

    private ComputeSkinningProgram(int programId, int paletteBuffer, int vertexCountLocation) {
        this.programId = programId;
        this.paletteBuffer = paletteBuffer;
        this.vertexCountLocation = vertexCountLocation;
    }

    /** 一次性能力探测（渲染线程，GL 上下文已就绪）。 */
    static boolean supported() {
        String version = GlStateManager._getString(GL11.GL_VERSION);
        int[] parsed = parseVersion(version);
        boolean versionOk = parsed[0] > MIN_MAJOR || (parsed[0] == MIN_MAJOR && parsed[1] >= MIN_MINOR);
        GLCapabilities caps = GL.getCapabilities();
        boolean pointerOk = caps != null && caps.glDispatchCompute != 0;
        LOGGER.info("[skinning] compute 能力探测：GL_VERSION = \"{}\"（需要 ≥ {}.{}），函数指针 = {}",
                version, MIN_MAJOR, MIN_MINOR, pointerOk);
        return versionOk && pointerOk;
    }

    /** "4.6.0 NVIDIA 572.83" → [4, 6]；解析失败 → [0, 0]。 */
    private static int[] parseVersion(String version) {
        if (version == null) {
            return new int[]{0, 0};
        }
        try {
            String[] head = version.trim().split(" ")[0].split("\\.");
            return new int[]{Integer.parseInt(head[0]), head.length > 1 ? Integer.parseInt(head[1]) : 0};
        } catch (RuntimeException e) {
            return new int[]{0, 0};
        }
    }

    /** 编译/链接并创建 palette SSBO；失败抛 IOException（调用方整体禁用 compute 路径）。 */
    static ComputeSkinningProgram create(ResourceProvider provider) throws IOException {
        String source;
        try (var stream = provider.getResourceOrThrow(SHADER_ID).open()) {
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IOException("[skinning] skin.comp 编译失败: " + log);
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(shader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new IOException("[skinning] skin.comp 链接失败: " + log);
        }

        int palette = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, palette);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, PALETTE_BYTES, GL15.GL_STREAM_DRAW);
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        return new ComputeSkinningProgram(program, palette,
                GL20.glGetUniformLocation(program, "VertexCount"));
    }

    /**
     * 上传 palette 并对一份几何执行蒙皮：输入/输出/palette 分别绑到 SSBO binding 0/1/2，
     * dispatch 后以 SSBO + 顶点属性双屏障保证随后 draw 读到完整结果。
     */
    void skin(ComputeSkinnedGeometry geometry, float[] pose, float[] normals) {
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, paletteBuffer);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, pose);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, NORMAL_OFFSET, normals);

        GL20.glUseProgram(programId);
        GL20.glUniform1i(vertexCountLocation, geometry.vertexCount());
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, geometry.inputBuffer());
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, geometry.outputBuffer());
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, paletteBuffer);
        GL43.glDispatchCompute((geometry.vertexCount() + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE, 1, 1);
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        GL20.glUseProgram(0);
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(programId);
        RenderSystem.glDeleteBuffers(paletteBuffer);
    }
}
//?}
