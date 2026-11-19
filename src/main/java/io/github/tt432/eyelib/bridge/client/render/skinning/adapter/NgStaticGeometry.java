//? if >=26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;

/**
 * 静态实例几何（26.1.2，ADR-0048）：bind 姿态摊平后的模型空间顶点驻留 GPU，
 * 逐实例 root 位姿/法线阵/tint/light/overlay 经 `InstanceTransform` UBO（std140，160B）传入。
 *
 * <p>blaze3d 的 {@code VertexArrayCache} 不设置顶点属性除数，因此 26.1 不做顶点属性实例化；
 * 改为「同一静态几何 + 逐 draw uniform」：几何仍只驻留一份、无 compute、每实体上传 160B。
 */
public final class NgStaticGeometry implements AutoCloseable {
    /**
     * 摊平几何的 Normal 是 float×3（与 CPU 路径同精度）；vanilla {@code VertexFormatElement.NORMAL}
     * 是 byte×3，用它会让 26.1 的 VAO 步长变成 23（blaze3d 要求 4 字节对齐）。
     */
    private static final VertexFormatElement NORMAL_FLOAT =
            VertexFormatElement.register(VertexFormatElement.findNextId(), 0,
                    VertexFormatElement.Type.FLOAT, false, 3);

    public static final VertexFormat FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", NORMAL_FLOAT)
            .build();

    /** std140 UBO block 大小与字段偏移（vec4 tint / ivec2 overlay / ivec2 light / mat4 pose / mat4 normal）。 */
    static final int BLOCK_SIZE = 160;
    static final int OFF_TINT = 0;
    static final int OFF_OVERLAY = 16;
    static final int OFF_LIGHT = 24;
    static final int OFF_POSE = 32;
    static final int OFF_NORMAL = 96;

    private final Int2IntMap boneToSlot;
    private final int[] boneIds;
    private final int[] firstVertex;
    private final int[] vertexCount;
    private final int slotCount;
    private final GpuBuffer vertexBuffer;

    private NgStaticGeometry(StaticGeometryPacker.Packed packed) {
        this.boneToSlot = packed.boneToSlot();
        this.boneIds = packed.boneIds();
        this.firstVertex = packed.firstVertex();
        this.vertexCount = packed.vertexCount();
        this.slotCount = packed.boneIds().length;
        this.vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "eyelib-static-vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, packed.vertices());
    }

    /** 由 bind 骨骼树与烘焙几何创建；全零顶点返回 null（调用方回退常规路径）。渲染线程调用。 */
    static @Nullable NgStaticGeometry create(Model model, BakedModel baked) {
        StaticGeometryPacker.Packed packed = StaticGeometryPacker.pack(model, baked);
        return packed == null ? null : new NgStaticGeometry(packed);
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

    GpuBuffer vertexBuffer() {
        return vertexBuffer;
    }

    /** 可见 slot → 连续顶点区间（[firstVertex, count, ...]）。 */
    int[] ranges(BitSet visible) {
        return StaticGeometryPacker.coalesceVisible(visible, firstVertex, vertexCount, slotCount);
    }

    @Override
    public void close() {
        vertexBuffer.close();
    }
}
//?}
