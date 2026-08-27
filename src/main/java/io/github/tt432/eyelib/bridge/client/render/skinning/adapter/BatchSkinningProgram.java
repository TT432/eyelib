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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * C1 跨实体合批 compute 蒙皮程序（&le;26.1，GL&ge;4.6，ADR-0032）：skin_batch.comp 的编译/链接，
 * 全局 palette/meta/output SSBO 与合批 VAO 的生命周期，以及一次 drain 的 dispatch 编排。
 *
 * <p>缓冲策略：
 * <ul>
 *   <li>palette/meta：每 drain orphan（glBufferData 同容量）+ subData 上传，容量倍增增长；</li>
 *   <li>output：仅容量保障（compute 全覆盖使用区，无 CPU 读回，屏障保序），重建时 VAO 重指属性；</li>
 *   <li>VAO：属性 0-5 指向 output SSBO（Position/UV0/Normal/Tint/LightUV/OverlayUV，stride 80）。</li>
 * </ul>
 *
 * <p>能力探测：vanilla GLFW hints 请求 3.2 core，但桌面驱动实际返回最高可用 core 版本
 * （本机 NVIDIA 实测 4.6）；以 {@code GL_VERSION} 字符串解析 + LWJGL 函数指针双重判定。
 * 阈值按需求收紧为 4.6（compute shader 自 4.3 起为核心特性）。
 */
final class BatchSkinningProgram implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSkinningProgram.class);

    static final int MIN_MAJOR = 4;
    static final int MIN_MINOR = 6;

    /** VertOut 步长（与 skin_batch.comp 的 VertOut std430 布局逐字段对应）。 */
    static final int OUT_STRIDE = 80;
    /** Meta 步长（std430 数组 stride：48B 有效 + 16B 对齐）。 */
    static final int META_STRIDE = 64;

    private static final ResourceLocation SHADER_ID =
            ResourceLocationBridge.fromParts("eyelib", "shaders/skinning/skin_batch.comp");
    private static final int WORKGROUP_SIZE = 64; // 与 skin_batch.comp local_size_x 一致
    private static final int INITIAL_VERTEX_CAPACITY = 1 << 16;
    private static final int INITIAL_MAT_CAPACITY = 1 << 12;
    private static final int INITIAL_META_CAPACITY = 256;

    private final int programId;
    private final int paletteBuffer;
    private final int metaBuffer;
    private final int vertexCountLocation;
    private final int entityBaseLocation;

    private int outputBuffer;
    private int vaoId;
    private int vertexCapacity;
    private int matCapacity;
    private int metaCapacity;
    private boolean vaoDirty = true;

    private BatchSkinningProgram(int programId, int paletteBuffer, int metaBuffer, int outputBuffer, int vaoId,
                                 int vertexCountLocation, int entityBaseLocation) {
        this.programId = programId;
        this.paletteBuffer = paletteBuffer;
        this.metaBuffer = metaBuffer;
        this.outputBuffer = outputBuffer;
        this.vaoId = vaoId;
        this.vertexCountLocation = vertexCountLocation;
        this.entityBaseLocation = entityBaseLocation;
    }

    /** 一次性能力探测（渲染线程，GL 上下文已就绪）。 */
    static boolean supported() {
        String version = GlStateManager._getString(GL11.GL_VERSION);
        int[] parsed = parseVersion(version);
        boolean versionOk = parsed[0] > MIN_MAJOR || (parsed[0] == MIN_MAJOR && parsed[1] >= MIN_MINOR);
        GLCapabilities caps = GL.getCapabilities();
        boolean pointerOk = caps != null && caps.glDispatchCompute != 0;
        LOGGER.info("[skinning] 合批 compute 能力探测：GL_VERSION = \"{}\"（需要 ≥ {}.{}），函数指针 = {}",
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

    /** 编译/链接并创建全局缓冲与 VAO；失败抛 IOException（调用方整体禁用 compute 路径）。 */
    static BatchSkinningProgram create(ResourceProvider provider) throws IOException {
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
            throw new IOException("[skinning] skin_batch.comp 编译失败: " + log);
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(shader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new IOException("[skinning] skin_batch.comp 链接失败: " + log);
        }

        int palette = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, palette);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) INITIAL_MAT_CAPACITY * 64L, GL15.GL_STREAM_DRAW);

        int meta = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, meta);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) INITIAL_META_CAPACITY * META_STRIDE, GL15.GL_STREAM_DRAW);

        int output = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, output);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) INITIAL_VERTEX_CAPACITY * OUT_STRIDE, GL15.GL_STREAM_DRAW);
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        int vao = GlStateManager._glGenVertexArrays();

        BatchSkinningProgram result = new BatchSkinningProgram(program, palette, meta, output, vao,
                GL20.glGetUniformLocation(program, "VertexCount"),
                GL20.glGetUniformLocation(program, "EntityBase"));
        result.matCapacity = INITIAL_MAT_CAPACITY;
        result.metaCapacity = INITIAL_META_CAPACITY;
        result.vertexCapacity = INITIAL_VERTEX_CAPACITY;
        return result;
    }

    /** 容量保障：不足时倍增重建（output 重建标记 VAO 重指）。渲染线程调用。 */
    void ensureCapacities(int vertexCount, int matCount, int entityCount) {
        if (matCount > matCapacity) {
            matCapacity = grow(matCapacity, matCount);
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, paletteBuffer);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) matCapacity * 64L, GL15.GL_STREAM_DRAW);
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (entityCount > metaCapacity) {
            metaCapacity = grow(metaCapacity, entityCount);
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, metaBuffer);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) metaCapacity * META_STRIDE, GL15.GL_STREAM_DRAW);
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (vertexCount > vertexCapacity) {
            vertexCapacity = grow(vertexCapacity, vertexCount);
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) vertexCapacity * OUT_STRIDE, GL15.GL_STREAM_DRAW);
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            vaoDirty = true;
        }
    }

    private static int grow(int capacity, int needed) {
        while (capacity < needed) {
            capacity *= 2;
        }
        return capacity;
    }

    /** orphan + subData 上传本 drain 的 palette/meta 暂存（flip 后的 readonly 视图）。 */
    void upload(ByteBuffer paletteData, ByteBuffer metaData) {
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, paletteBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) matCapacity * 64L, GL15.GL_STREAM_DRAW);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, paletteData);

        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, metaBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) metaCapacity * META_STRIDE, GL15.GL_STREAM_DRAW);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, metaData);

        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    /** 计算相开始：绑定 compute program 与 output/palette/meta 的 SSBO 基址（输入按子批绑定）。 */
    void beginDispatch() {
        GL20.glUseProgram(programId);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, outputBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, paletteBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, metaBuffer);
    }

    /** 一个子批（同几何的连续实体段）：绑定输入 SSBO，2D dispatch（顶点 × 实体）。 */
    void dispatchSubbatch(ComputeSkinnedGeometry geometry, int entityBase, int entityCount) {
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, geometry.inputBuffer());
        GL20.glUniform1i(vertexCountLocation, geometry.vertexCount());
        GL20.glUniform1i(entityBaseLocation, entityBase);
        GL43.glDispatchCompute((geometry.vertexCount() + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE, entityCount, 1);
    }

    /** 计算相结束：SSBO + 顶点属性双屏障，保证随后 draw 读到完整结果。 */
    void endDispatch() {
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        GL20.glUseProgram(0);
    }

    /** 绑定合批 VAO（output 重建后重指属性指针）。 */
    void bindVao() {
        if (vaoDirty) {
            vaoDirty = false;
            GlStateManager._glBindVertexArray(vaoId);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, outputBuffer);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, OUT_STRIDE, 0L);   // Position
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, OUT_STRIDE, 32L);  // UV0
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, OUT_STRIDE, 16L);  // Normal
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, OUT_STRIDE, 64L);  // Tint
            GL20.glVertexAttribPointer(4, 2, GL11.GL_FLOAT, false, OUT_STRIDE, 40L);  // LightUV
            GL20.glVertexAttribPointer(5, 2, GL11.GL_FLOAT, false, OUT_STRIDE, 48L);  // OverlayUV
            for (int i = 0; i < 6; i++) {
                GL20.glEnableVertexAttribArray(i);
            }
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
        GlStateManager._glBindVertexArray(vaoId);
    }

    static void unbindVao() {
        GlStateManager._glBindVertexArray(0);
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(programId);
        RenderSystem.glDeleteBuffers(paletteBuffer);
        RenderSystem.glDeleteBuffers(metaBuffer);
        RenderSystem.glDeleteBuffers(outputBuffer);
        GlStateManager._glDeleteVertexArrays(vaoId);
    }
}
//?}
