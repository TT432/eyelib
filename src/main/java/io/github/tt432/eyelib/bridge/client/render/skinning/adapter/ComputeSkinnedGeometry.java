//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * C1 跨实体合批 compute 蒙皮几何（&le;26.1，GL&ge;4.6）：输入 SSBO（绑定姿态展开三角形，
 * 48B/顶点，STATIC）。蒙皮输出写入 {@link BatchSkinningProgram} 的全局 output SSBO，
 * 绘制用其全局合批 VAO——几何自身不再持有输出缓冲/VAO。
 *
 * <p>输入顶点布局（std430，与 skin_batch.comp 的 VertIn 逐字段对应）：
 * <pre>
 *   0  vec4 posSlot（xyz=绑定姿态位置，w=slot 的 int 位模式）
 *   16 vec4 nrm（xyz=绑定姿态法线，字节量化精度，与 VS 路径上传一致）
 *   32 vec2 uv + 8B 填充
 * </pre>
 *
 * <p>VAO 属性位置 0-5 = Position/UV0/Normal/Tint/LightUV/OverlayUV：vanilla {@code ShaderInstance}
 * 按构造传入 VertexFormat 的元素顺序 {@code glBindAttribLocation}（链接前顺序绑定，源码实证），
 * 合批绘制用 {@link #BATCH_FORMAT} 即按此顺序声明。
 */
final class ComputeSkinnedGeometry implements LegacyGpuGeometry {
    static final int STRIDE = 48;

    /** 合批直通管线的顶点格式：Normal/Tint FLOAT 自定义元素 + LightUV/OverlayUV FLOAT UV 元素。 */
    public static final VertexFormat BATCH_FORMAT;

    //? if <1.20.6 {
    static {
        BATCH_FORMAT = new VertexFormat(com.google.common.collect.ImmutableMap.of(
                "Position", com.mojang.blaze3d.vertex.DefaultVertexFormat.ELEMENT_POSITION,
                "UV0", com.mojang.blaze3d.vertex.DefaultVertexFormat.ELEMENT_UV0,
                "Normal", new com.mojang.blaze3d.vertex.VertexFormatElement(0,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.NORMAL, 3),
                "Tint", new com.mojang.blaze3d.vertex.VertexFormatElement(0,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.COLOR, 4),
                "LightUV", new com.mojang.blaze3d.vertex.VertexFormatElement(3,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.UV, 2),
                "OverlayUV", new com.mojang.blaze3d.vertex.VertexFormatElement(4,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.UV, 2)));
    }
    //?} else {
    static {
        BATCH_FORMAT = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("UV0", com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("Normal", new com.mojang.blaze3d.vertex.VertexFormatElement(7, 0,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.NORMAL, 3))
                .add("Tint", new com.mojang.blaze3d.vertex.VertexFormatElement(8, 0,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.COLOR, 4))
                .add("LightUV", new com.mojang.blaze3d.vertex.VertexFormatElement(9, 3,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.UV, 2))
                .add("OverlayUV", new com.mojang.blaze3d.vertex.VertexFormatElement(10, 4,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT,
                        com.mojang.blaze3d.vertex.VertexFormatElement.Usage.UV, 2))
                .build();
    }
    //?}

    private final Int2IntMap boneToSlot;
    private final int slotCount;
    private final int inputBuffer;
    private final int vertexCount;

    private ComputeSkinnedGeometry(SkinningGeometryPacker.SlotPlan plan,
                                   int inputBuffer, int vertexCount) {
        this.boneToSlot = plan.boneToSlot();
        this.slotCount = plan.slotCount();
        this.inputBuffer = inputBuffer;
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
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        return new ComputeSkinnedGeometry(plan, input, expandedVertices);
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

    int vertexCount() {
        return vertexCount;
    }

    @Override
    public void close() {
        RenderSystem.glDeleteBuffers(inputBuffer);
    }
}
//?}
