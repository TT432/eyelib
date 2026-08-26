//? if >=26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;

/**
 * 一份烘焙模型的 GPU 驻留静态几何（C1，26.1.2）：
 * 顶点缓冲（绑定姿态 + palette slot）与索引缓冲各一，生命周期跟随资源重载整体失效。
 *
 * <p>顶点格式 {@link #FORMAT}（28B stride）：Position(3f) / UV0(2f) / Normal(3×byte 归一化 + 1B pad)
 * / BoneIndex(uint)。非归一化整数元素由 GL 后端走 glVertexAttribIPointer
 * （VertexArrayCache），着色器侧为 {@code in uint BoneIndex}。
 */
public final class SkinnedGeometry implements AutoCloseable {
    public static final VertexFormatElement BONE_INDEX =
            VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.UINT, false, 1);
    public static final VertexFormat FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .add("BoneIndex", BONE_INDEX)
            .build();

    private final int[] boneIds;
    private final Int2IntMap boneToSlot;
    private final int[] firstIndex;
    private final int[] indexCount;
    private final GpuBuffer vertexBuffer;
    private final GpuBuffer indexBuffer;
    private final int slotCount;
    private final int paletteBlockSize;

    private SkinnedGeometry(SkinningGeometryPacker.Packed packed) {
        this.boneIds = packed.boneIds();
        this.boneToSlot = packed.boneToSlot();
        this.firstIndex = packed.firstIndex();
        this.indexCount = packed.indexCount();
        this.slotCount = boneIds.length;
        this.paletteBlockSize = SkinningLayout.blockSize(slotCount);
        var device = RenderSystem.getDevice();
        this.vertexBuffer = device.createBuffer(() -> "eyelib-skinned-vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, packed.vertices());
        this.indexBuffer = device.createBuffer(() -> "eyelib-skinned-indices",
                GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, packed.indices());
    }

    /**
     * 由烘焙模型创建 GPU 几何；骨骼数超 {@link SkinningLayout#MAX_BONES} 或全零顶点时返回 null
     * （调用方回退经典 CPU 路径）。
     */
    public static @Nullable SkinnedGeometry create(BakedModel model) {
        SkinningGeometryPacker.Packed packed = SkinningGeometryPacker.pack(model);
        if (packed == null || !SkinningLayout.skinnable(packed.boneIds().length)) {
            return null;
        }
        return new SkinnedGeometry(packed);
    }

    /** 骨骼 id → palette slot；无几何骨骼返回 -1。 */
    public int slotOf(int boneId) {
        return boneToSlot.getOrDefault(boneId, -1);
    }

    public int slotCount() {
        return slotCount;
    }

    /** 本几何一帧一实体的 palette UBO block 尺寸（未对齐）。 */
    public int paletteBlockSize() {
        return paletteBlockSize;
    }

    public GpuBuffer vertexBuffer() {
        return vertexBuffer;
    }

    public GpuBuffer indexBuffer() {
        return indexBuffer;
    }

    /** 合并可见 slot 为 draw 区间对 [firstIndex, count, ...]。 */
    public int[] coalesceVisible(java.util.BitSet visible) {
        return SkinningGeometryPacker.coalesceRanges(visible, firstIndex, indexCount, slotCount);
    }

    @Override
    public void close() {
        vertexBuffer.close();
        indexBuffer.close();
    }
}
//?}
