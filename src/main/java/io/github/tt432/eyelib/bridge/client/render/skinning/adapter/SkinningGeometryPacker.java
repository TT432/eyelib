package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 静态蒙皮几何的 CPU 打包：{@link BakedModel}（按骨骼分组的绑定姿态数组）
 * → 交错顶点缓冲（含 palette slot）+ TRIANGLES 索引 + per-slot 索引区间。
 *
 * <p>纯 CPU 无 GL 依赖（三版本可编译、可单测）；GL 上传在 {@code SkinnedGeometry}。
 *
 * <p>约定：
 * <ul>
 *   <li>骨骼按 id 升序分配 slot（确定性）；零顶点骨骼不占 slot。</li>
 *   <li>同一骨骼的顶点与索引连续 → partVisibility 按 slot 区间裁剪 draw。</li>
 *   <li>法线打包与 {@code BufferBuilder.setNormal} 一致：clamp[-1,1]×127 截断为 byte。</li>
 *   <li>索引为 uint（texture_mesh 体素化模型可超 65535 顶点）。</li>
 * </ul>
 */
final class SkinningGeometryPacker {
    /** pos(3f) + uv(2f) + normal(3b) + pad(1b) + boneIndex(1×uint)。 */
    static final int VERTEX_SIZE = 28;

    private SkinningGeometryPacker() {
    }

    record Packed(
            /** slot → 骨骼 id（按 id 升序）。 */
            int[] boneIds,
            /** 骨骼 id → slot（零顶点骨骼无条目）。 */
            Int2IntMap boneToSlot,
            /** slot → 索引缓冲起始（元素数，非字节）。 */
            int[] firstIndex,
            /** slot → 索引数。 */
            int[] indexCount,
            ByteBuffer vertices,
            ByteBuffer indices
    ) {
    }

    /** slot 分配计划（按骨骼 id 升序、跳过零顶点骨骼；NG 与 Legacy 几何共用此唯一事实源）。 */
    record SlotPlan(
            /** 骨骼 id → slot（零顶点骨骼无条目）。 */
            Int2IntMap boneToSlot,
            /** slot → 骨骼 id（按 id 升序）。 */
            int[] slotBoneIds,
            int slotCount,
            int totalVertices
    ) {
    }

    static SlotPlan planSlots(BakedModel model) {
        int[] ids = model.bones().keySet().toIntArray();
        Arrays.sort(ids);

        Int2IntMap boneToSlot = new Int2IntOpenHashMap(ids.length);
        int[] slotBoneIds = new int[ids.length];
        int slotCount = 0;
        int totalVertices = 0;
        for (int id : ids) {
            BakedModel.BakedBone bone = model.bones().get(id);
            if (bone == null || bone.vertexSize() == 0) {
                continue;
            }
            boneToSlot.put(id, slotCount);
            slotBoneIds[slotCount++] = id;
            totalVertices += bone.vertexSize();
        }
        return new SlotPlan(boneToSlot, slotBoneIds, slotCount, totalVertices);
    }

    static Packed pack(BakedModel model) {
        SlotPlan plan = planSlots(model);
        Int2IntMap boneToSlot = plan.boneToSlot();
        int[] slotBoneIds = plan.slotBoneIds();
        int slotCount = plan.slotCount();
        int totalVertices = plan.totalVertices();
        if (slotCount == 0) {
            return null;
        }

        ByteBuffer vertices = ByteBuffer.allocateDirect(totalVertices * VERTEX_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        // 上界：每 4 顶点 6 索引；顶点数恒为 4 的倍数（face/体素均以 4 顶点发射）
        ByteBuffer indices = ByteBuffer.allocateDirect(totalVertices / 4 * 6 * 4).order(ByteOrder.LITTLE_ENDIAN);
        int[] firstIndex = new int[slotCount];
        int[] indexCount = new int[slotCount];

        int vertexBase = 0;
        int indexBase = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            BakedModel.BakedBone bone = model.bones().get(slotBoneIds[slot]);
            int n = bone.vertexSize();
            float[] pos = bone.position();
            float[] nrm = bone.normal();
            float[] u = bone.u();
            float[] v = bone.v();

            for (int i = 0; i < n; i++) {
                vertices.putFloat(pos[i * 3]).putFloat(pos[i * 3 + 1]).putFloat(pos[i * 3 + 2]);
                vertices.putFloat(u[i]).putFloat(v[i]);
                vertices.put(packNormal(nrm[i * 3])).put(packNormal(nrm[i * 3 + 1])).put(packNormal(nrm[i * 3 + 2]));
                vertices.put((byte) 0);
                vertices.putInt(slot);
            }

            firstIndex[slot] = indexBase;
            int quads = n / 4;
            for (int q = 0; q < quads; q++) {
                int b = vertexBase + q * 4;
                indices.putInt(b).putInt(b + 1).putInt(b + 2);
                indices.putInt(b + 2).putInt(b + 3).putInt(b);
            }
            int slotIndices = quads * 6;
            indexCount[slot] = slotIndices;
            indexBase += slotIndices;
            vertexBase += n;
        }

        vertices.flip();
        indices.flip();
        return new Packed(Arrays.copyOf(slotBoneIds, slotCount), boneToSlot, firstIndex, indexCount, vertices, indices);
    }

    private static byte packNormal(float c) {
        return (byte) ((int) (Math.max(-1.0F, Math.min(1.0F, c)) * 127.0F) & 0xFF);
    }

    /** 把可见 slot 集合合并为连续索引区间（[firstIndex0, count0, firstIndex1, count1, ...]）。 */
    static int[] coalesceRanges(java.util.BitSet visible, int[] firstIndex, int[] indexCount, int slotCount) {
        int[] tmp = new int[slotCount * 2];
        int n = 0;
        int slot = visible.nextSetBit(0);
        while (slot >= 0) {
            int start = firstIndex[slot];
            int count = indexCount[slot];
            int next = visible.nextSetBit(slot + 1);
            // 相邻可见 slot 且索引区间物理连续 → 合并为一次 draw
            while (next >= 0 && firstIndex[next] == start + count) {
                count += indexCount[next];
                next = visible.nextSetBit(next + 1);
            }
            tmp[n++] = start;
            tmp[n++] = count;
            slot = next;
        }
        return Arrays.copyOf(tmp, n);
    }
}
