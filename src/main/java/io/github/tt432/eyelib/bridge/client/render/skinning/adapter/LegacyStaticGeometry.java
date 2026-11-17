//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.BitSet;

//? if <1.20.6 {
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//?}

/**
 * 静态实例几何（&le;26.1，ADR-0048）：bind 姿态摊平后的模型空间顶点（Position/UV0/Normal，
 * 32B stride，按骨骼分段）驻留 GPU，实例数据走 SSBO（`gl_InstanceID` 索引），
 * 绘制用 `glDrawArraysInstanced`。
 *
 * <p>为什么用 SSBO 而不是逐实例顶点属性：1.20.1 的 {@code VertexFormatElement} 只允许
 * 非 UV usage 的 index 为 0（其余元素只能占用 8 个 UV 槽位），13 个属性无法在单个
 * {@code VertexFormat} 里表达；而静态通道与 compute 合批同门控（GL&ge;4.6），SSBO 无额外门槛。
 *
 * <p>实例布局（std430，160B）：mat4 pose / mat4 normal（左上 3×3）/ vec4 tint /
 * vec2 light(light&amp;0xFFFF, light&gt;&gt;&gt;16) / vec2 overlay(overlay&amp;0xFFFF, overlay&gt;&gt;&gt;16)。
 */
final class LegacyStaticGeometry implements AutoCloseable {
    static final VertexFormat FORMAT;
    /** 实例记录 stride（std430 struct 对齐：最大成员 mat4 → 16B，尺寸 160B）。 */
    static final int INSTANCE_STRIDE = 160;
    static final int OFF_POSE = 0;
    static final int OFF_NORMAL = 64;
    static final int OFF_TINT = 128;
    static final int OFF_LIGHT = 144;
    static final int OFF_OVERLAY = 152;

    //? if <1.20.6 {
    static {
        FORMAT = new VertexFormat(ImmutableMap.of(
                "Position", DefaultVertexFormat.ELEMENT_POSITION,
                "UV0", DefaultVertexFormat.ELEMENT_UV0,
                "Normal", DefaultVertexFormat.ELEMENT_NORMAL));
    }
    //?} else {
    static {
        FORMAT = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("UV0", com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("Normal", com.mojang.blaze3d.vertex.VertexFormatElement.NORMAL)
                .build();
    }
    //?}

    private final Int2IntMap boneToSlot;
    private final int[] boneIds;
    private final int slotCount;
    private final int[] firstVertex;
    private final int[] vertexCount;
    private final int vao;
    private final int vertexBuffer;
    private int totalVertices;

    private LegacyStaticGeometry(StaticGeometryPacker.Packed packed, int vao, int vertexBuffer) {
        this.boneToSlot = packed.boneToSlot();
        this.boneIds = packed.boneIds();
        this.slotCount = packed.boneIds().length;
        this.firstVertex = packed.firstVertex();
        this.vertexCount = packed.vertexCount();
        this.vao = vao;
        this.vertexBuffer = vertexBuffer;
        int total = 0;
        for (int count : vertexCount) {
            total += count;
        }
        this.totalVertices = total;
    }

    /** 由 bind 骨骼树与烘焙几何创建；全零顶点返回 null（调用方回退常规路径）。渲染线程调用。 */
    static @Nullable LegacyStaticGeometry create(Model model, BakedModel baked) {
        StaticGeometryPacker.Packed packed = StaticGeometryPacker.pack(model, baked);
        if (packed == null) {
            return null;
        }
        int vao = GlStateManager._glGenVertexArrays();
        int buffer = GlStateManager._glGenBuffers();
        int previousVao = GlStateManager._getInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousBuffer = GlStateManager._getInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, packed.vertices(), GL15.GL_STATIC_DRAW);
            int stride = StaticGeometryPacker.VERTEX_STRIDE;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 12L);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 20L);
            for (int i = 0; i < 3; i++) {
                GL20.glEnableVertexAttribArray(i);
            }
        } finally {
            GlStateManager._glBindVertexArray(previousVao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
        return new LegacyStaticGeometry(packed, vao, buffer);
    }

    /** 骨骼 id → slot；无几何骨骼返回 -1。 */
    int slotOf(int boneId) {
        return boneToSlot.getOrDefault(boneId, -1);
    }

    int slotCount() {
        return slotCount;
    }

    /** slot → 骨骼 id（按 id 升序）。 */
    int boneId(int slot) {
        return boneIds[slot];
    }

    int totalVertices() {
        return totalVertices;
    }

    int vao() {
        return vao;
    }

    /** 可见 slot → 连续顶点区间（[firstVertex, count, ...]）。 */
    int[] ranges(BitSet visible) {
        return StaticGeometryPacker.coalesceVisible(visible, firstVertex, vertexCount, slotCount);
    }

    @Override
    public void close() {
        GlStateManager._glDeleteVertexArrays(vao);
        com.mojang.blaze3d.systems.RenderSystem.glDeleteBuffers(vertexBuffer);
    }
}
//?}
