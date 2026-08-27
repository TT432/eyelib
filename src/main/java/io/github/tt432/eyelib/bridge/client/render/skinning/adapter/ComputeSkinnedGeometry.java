//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * C1 compute 蒙皮几何（&le;26.1，GL&ge;4.6）：输入 SSBO（绑定姿态展开三角形，48B/顶点，STATIC）
 * + 输出 SSBO（蒙皮结果，STREAM_DRAW，每帧被 skin.comp 覆写）+ 直通绘制 VAO。
 *
 * <p>顶点布局（std430，与 skin.comp 的 VertIn/VertOut 逐字段对应）：
 * <pre>
 *   0  vec4 posSlot（xyz=绑定姿态位置，w=slot 的 int 位模式）
 *   16 vec4 nrm（xyz=绑定姿态法线，字节量化精度，与 VS 路径上传一致）
 *   32 vec2 uv + 8B 填充
 * </pre>
 *
 * <p>VAO 属性位置 0/1/2 = Position/UV0/Normal：vanilla {@code ShaderInstance} 按构造传入
 * VertexFormat 的元素顺序 {@code glBindAttribLocation}（链接前顺序绑定，源码实证），
 * compute 绘制用 {@link #COMPUTE_FORMAT} 即按此顺序声明。
 *
 * <p>多实体共享同一几何的输出缓冲：flush 内 draw 在渲染线程严格串行且 GL 命令保序，无竞争。
 */
final class ComputeSkinnedGeometry implements LegacyGpuGeometry {
    static final int STRIDE = 48;

    /** 直通管线的顶点格式：Normal 为 FLOAT×3（非 vanilla 的 BYTE 归一化元素）。 */
    public static final VertexFormat COMPUTE_FORMAT;

    //? if <1.20.6 {
    static {
        COMPUTE_FORMAT = new VertexFormat(com.google.common.collect.ImmutableMap.of(
                "Position", com.mojang.blaze3d.vertex.DefaultVertexFormat.ELEMENT_POSITION,
                "UV0", com.mojang.blaze3d.vertex.DefaultVertexFormat.ELEMENT_UV0,
                "Normal", new com.mojang.blaze3d.vertex.VertexFormatElement(0,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.NORMAL, 3)));
    }
    //?} else {
    static {
        COMPUTE_FORMAT = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("UV0", com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("Normal", new com.mojang.blaze3d.vertex.VertexFormatElement(7, 0,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.NORMAL, 3))
                .build();
    }
    //?}

    private final Int2IntMap boneToSlot;
    private final int slotCount;
    private final int inputBuffer;
    private final int outputBuffer;
    private final int vaoId;
    private final int vertexCount;

    private ComputeSkinnedGeometry(SkinningGeometryPacker.SlotPlan plan,
                                   int inputBuffer, int outputBuffer, int vaoId, int vertexCount) {
        this.boneToSlot = plan.boneToSlot();
        this.slotCount = plan.slotCount();
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;
        this.vaoId = vaoId;
        this.vertexCount = vertexCount;
    }

    /** 同 {@link LegacySkinnedGeometry#create} 的前置条件；渲染线程调用。 */
    static @Nullable ComputeSkinnedGeometry create(BakedModel model) {
        RenderSystem.assertOnRenderThread();
        SkinningGeometryPacker.SlotPlan plan = SkinningGeometryPacker.planSlots(model);
        if (plan.slotCount() == 0 || !SkinningLayout.skinnable(plan.slotCount())) {
            return null;
        }

        int expandedVertices = plan.totalVertices() / 4 * 6;
        ByteBuffer data = ByteBuffer.allocateDirect(expandedVertices * STRIDE).order(ByteOrder.LITTLE_ENDIAN);
        for (int slot = 0; slot < plan.slotCount(); slot++) {
            BakedModel.BakedBone bone = model.bones().get(plan.slotBoneIds()[slot]);
            int n = bone.vertexSize();
            float[] pos = bone.position();
            float[] nrm = bone.normal();
            float[] u = bone.u();
            float[] v = bone.v();
            int quads = n / 4;
            for (int q = 0; q < quads; q++) {
                int b = q * 4;
                emitVertex(data, slot, pos, nrm, u, v, b);
                emitVertex(data, slot, pos, nrm, u, v, b + 1);
                emitVertex(data, slot, pos, nrm, u, v, b + 2);
                emitVertex(data, slot, pos, nrm, u, v, b + 2);
                emitVertex(data, slot, pos, nrm, u, v, b + 3);
                emitVertex(data, slot, pos, nrm, u, v, b);
            }
        }
        data.flip();

        int input = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, input);
        RenderSystem.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, GL15.GL_STATIC_DRAW);

        int output = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, output);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) expandedVertices * STRIDE, GL15.GL_STREAM_DRAW);
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        // VAO：属性指针记录 output SSBO（glVertexAttribPointer 捕获当前 ARRAY_BUFFER 绑定）
        int vao = GlStateManager._glGenVertexArrays();
        GlStateManager._glBindVertexArray(vao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, output);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0L);   // Position
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, STRIDE, 32L);  // UV0
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, STRIDE, 16L);  // Normal
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager._glBindVertexArray(0);

        return new ComputeSkinnedGeometry(plan, input, output, vao, expandedVertices);
    }

    private static void emitVertex(ByteBuffer data, int slot,
                                   float[] pos, float[] nrm, float[] u, float[] v, int i) {
        data.putFloat(pos[i * 3]).putFloat(pos[i * 3 + 1]).putFloat(pos[i * 3 + 2]);
        data.putInt(slot); // w = slot 的 int 位模式，着色器侧 floatBitsToUint
        // 法线量化与 BufferBuilder.normal()（byte×127 截断 / GL 归一化 /127）逐位一致
        data.putFloat(quantizeNormal(nrm[i * 3]));
        data.putFloat(quantizeNormal(nrm[i * 3 + 1]));
        data.putFloat(quantizeNormal(nrm[i * 3 + 2]));
        data.putFloat(0.0F);
        data.putFloat(u[i]).putFloat(v[i]);
        data.putFloat(0.0F).putFloat(0.0F);
    }

    private static float quantizeNormal(float c) {
        return (byte) (int) (Math.max(-1.0F, Math.min(1.0F, c)) * 127.0F) / 127.0F;
    }

    @Override
    public int slotOf(int boneId) {
        return boneToSlot.getOrDefault(boneId, -1);
    }

    @Override
    public int slotCount() {
        return slotCount;
    }

    int inputBuffer() {
        return inputBuffer;
    }

    int outputBuffer() {
        return outputBuffer;
    }

    int vaoId() {
        return vaoId;
    }

    int vertexCount() {
        return vertexCount;
    }

    @Override
    public void close() {
        RenderSystem.glDeleteBuffers(inputBuffer);
        RenderSystem.glDeleteBuffers(outputBuffer);
        GlStateManager._glDeleteVertexArrays(vaoId);
    }
}
//?}
