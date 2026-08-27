//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;

//? if <1.20.6 {
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//?} else {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
//?}

/**
 * 一份烘焙模型的 GPU 驻留静态几何（C1，&le;26.1，ADR-0032）：vanilla {@link VertexBuffer}（STATIC），
 * 生命周期跟随资源重载整体失效。
 *
 * <p>顶点格式 {@link #FORMAT}（27B stride）：Position(3f) / UV0(2f) / Normal(3×byte 归一化)
 * / BoneIndex（复用 vanilla UV1 元素：SHORT×1 + 0，Usage.UV 非 FLOAT → glVertexAttribIPointer，
 * 着色器侧 {@code in ivec2 BoneIndex}，取 .x）。
 * 顶点按三角形展开（每 quad 6 顶点，与 CPU 索引展开同序），配合 RenderSystem 序列索引缓冲
 * （AutoStorageIndexBuffer 按顶点数自动 SHORT/INT），绕开 BufferBuilder 无显式索引 public API 的限制。
 *
 * <p>顶点写入走 vanilla VertexConsumer API，字节级打包（法线量化等）与经典 CPU 路径天然一致。
 */
public final class LegacySkinnedGeometry implements AutoCloseable {
    //? if <1.20.6 {
    public static final VertexFormat FORMAT = new VertexFormat(ImmutableMap.of(
            "Position", DefaultVertexFormat.ELEMENT_POSITION,
            "UV0", DefaultVertexFormat.ELEMENT_UV0,
            "Normal", DefaultVertexFormat.ELEMENT_NORMAL,
            "BoneIndex", DefaultVertexFormat.ELEMENT_UV1));
    //?} else {
    public static final VertexFormat FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("BoneIndex", VertexFormatElement.UV1)
            .build();
    //?}

    private final Int2IntMap boneToSlot;
    private final int slotCount;
    private final VertexBuffer vertexBuffer;

    private LegacySkinnedGeometry(SkinningGeometryPacker.SlotPlan plan, VertexBuffer vertexBuffer) {
        this.boneToSlot = plan.boneToSlot();
        this.slotCount = plan.slotCount();
        this.vertexBuffer = vertexBuffer;
    }

    /**
     * 由烘焙模型创建 GPU 几何；骨骼数超 {@link SkinningLayout#MAX_BONES} 或全零顶点时返回 null
     * （调用方回退经典 CPU 路径）。必须在渲染线程调用（VertexBuffer 创建/上传断言）。
     */
    public static @Nullable LegacySkinnedGeometry create(BakedModel model) {
        SkinningGeometryPacker.SlotPlan plan = SkinningGeometryPacker.planSlots(model);
        if (plan.slotCount() == 0 || !SkinningLayout.skinnable(plan.slotCount())) {
            return null;
        }
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        // VAO 必须先绑定再 upload：属性指针与 ELEMENT_ARRAY 绑定都记录在 VAO 状态里
        // （vanilla 同模式：ChunkRenderDispatcher.uploadChunkLayer = bind → upload → unbind；
        // 缺了这步，draw 时 VAO 无索引缓冲绑定 → glDrawElements 读空指针驱动崩溃，2026-08-27 实证）
        buffer.bind();
        upload(buffer, model, plan);
        VertexBuffer.unbind();
        return new LegacySkinnedGeometry(plan, buffer);
    }

    /** 展开三角形写入：每 quad 发 6 顶点（0,1,2, 2,3,0），骨骼分段连续（slot 升序）。 */
    private static void upload(VertexBuffer buffer, BakedModel model, SkinningGeometryPacker.SlotPlan plan) {
        int expandedVertices = plan.totalVertices() / 4 * 6;
        //? if <1.20.6 {
        BufferBuilder builder = new BufferBuilder(expandedVertices * FORMAT.getVertexSize());
        builder.begin(VertexFormat.Mode.TRIANGLES, FORMAT);
        //?} else {
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(expandedVertices * FORMAT.getVertexSize());
        BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, FORMAT);
        //?}

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
                emitVertex(builder, slot, pos, nrm, u, v, b);
                emitVertex(builder, slot, pos, nrm, u, v, b + 1);
                emitVertex(builder, slot, pos, nrm, u, v, b + 2);
                emitVertex(builder, slot, pos, nrm, u, v, b + 2);
                emitVertex(builder, slot, pos, nrm, u, v, b + 3);
                emitVertex(builder, slot, pos, nrm, u, v, b);
            }
        }

        //? if <1.20.6 {
        buffer.upload(builder.end());
        //?} else {
        MeshData mesh = builder.buildOrThrow();
        buffer.upload(mesh);
        byteBuffer.close();
        //?}
    }

    private static void emitVertex(BufferBuilder builder, int slot,
                                   float[] pos, float[] nrm, float[] u, float[] v, int i) {
        //? if <1.20.6 {
        builder.vertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2])
                .uv(u[i], v[i])
                .normal(nrm[i * 3], nrm[i * 3 + 1], nrm[i * 3 + 2])
                .overlayCoords(slot, 0)
                .endVertex();
        //?} else {
        builder.addVertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2])
                .setUv(u[i], v[i])
                .setNormal(nrm[i * 3], nrm[i * 3 + 1], nrm[i * 3 + 2])
                .setUv1(slot, 0);
        //?}
    }

    /** 骨骼 id → palette slot；无几何骨骼返回 -1。 */
    public int slotOf(int boneId) {
        return boneToSlot.getOrDefault(boneId, -1);
    }

    public int slotCount() {
        return slotCount;
    }

    public VertexBuffer vertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void close() {
        vertexBuffer.close();
    }
}
//?}
