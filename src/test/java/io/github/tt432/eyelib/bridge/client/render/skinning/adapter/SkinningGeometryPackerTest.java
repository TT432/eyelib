package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * C1 蒙皮几何打包与 std140 布局的结构性不变量：
 * slot 分配（id 升序、零顶点跳过）、顶点/索引内容、可见区间合并、UBO 布局偏移。
 */
class SkinningGeometryPackerTest {

    private static BakedModel.BakedBone bone(float[][] positions, float[][] normals, float[] u, float[] v) {
        int n = positions.length;
        float[] x = new float[n], y = new float[n], z = new float[n];
        float[] nx = new float[n], ny = new float[n], nz = new float[n];
        for (int i = 0; i < n; i++) {
            x[i] = positions[i][0]; y[i] = positions[i][1]; z[i] = positions[i][2];
            nx[i] = normals[i][0]; ny[i] = normals[i][1]; nz[i] = normals[i][2];
        }
        return new BakedModel.BakedBone(x, y, z, nx, ny, nz,
                new float[n * 3], new float[n * 3], new float[n * 3],
                new float[n * 3], new float[n * 3], new float[n * 3], u, v);
    }

    /** 骨骼 5：2 quad（8 顶点）；骨骼 2：1 quad；骨骼 9：0 顶点（应无 slot）。 */
    private static BakedModel model() {
        Int2ObjectMap<BakedModel.BakedBone> bones = new Int2ObjectOpenHashMap<>();
        bones.put(5, bone(new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
                                        {2, 0, 0}, {3, 0, 0}, {3, 1, 0}, {2, 1, 0}},
                          new float[][]{{0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1},
                                        {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}},
                          new float[]{0, 1, 1, 0, 0, 1, 1, 0}, new float[]{0, 0, 1, 1, 0, 0, 1, 1}));
        bones.put(2, bone(new float[][]{{10, 0, 0}, {11, 0, 0}, {11, 1, 0}, {10, 1, 0}},
                          new float[][]{{0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1}},
                          new float[]{0, 1, 1, 0}, new float[]{0, 0, 1, 1}));
        bones.put(9, bone(new float[][]{}, new float[][]{}, new float[]{}, new float[]{}));
        return new BakedModel(bones);
    }

    @Test
    void slotsAssignedByBoneIdAscendingSkippingEmpty() {
        var packed = SkinningGeometryPacker.pack(model());
        assertNotNull(packed);
        assertArrayEquals(new int[]{2, 5}, packed.boneIds());
        assertEquals(0, packed.boneToSlot().get(2));
        assertEquals(1, packed.boneToSlot().get(5));
        assertFalse(packed.boneToSlot().containsKey(9));
    }

    @Test
    void vertexAndIndexContents() {
        var packed = SkinningGeometryPacker.pack(model());
        var vertices = packed.vertices().order(ByteOrder.LITTLE_ENDIAN);
        var indices = packed.indices().order(ByteOrder.LITTLE_ENDIAN);

        // 骨骼 2（slot 0）先来：4 顶点 × 28B
        assertEquals((4 + 8) * SkinningGeometryPacker.VERTEX_SIZE, vertices.remaining());
        // slot0 顶点 0：pos(10,0,0) uv(0,0) normal(0,0,-127) pad boneIndex=0
        assertEquals(10.0f, vertices.getFloat(0));
        assertEquals(0.0f, vertices.getFloat(12));
        assertEquals((byte) 0, vertices.get(20));
        assertEquals((byte) -127, vertices.get(22));
        assertEquals(0, vertices.getInt(24));
        // slot1 顶点 0（骨骼 5）：boneIndex=1
        int slot1 = 4 * SkinningGeometryPacker.VERTEX_SIZE;
        assertEquals(1, vertices.getInt(slot1 + 24));
        assertEquals((byte) 127, vertices.get(slot1 + 22)); // normal z=+1

        // 索引：slot0 = [0,1,2,2,3,0]，slot1 两个 quad 紧随其后
        assertArrayEquals(new int[]{0, 6}, packed.firstIndex());
        assertArrayEquals(new int[]{6, 12}, packed.indexCount());
        assertEquals(18, indices.remaining() / 4);
        int[] expected = {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4, 8, 9, 10, 10, 11, 8};
        for (int i : expected) {
            assertEquals(i, indices.getInt());
        }
    }

    @Test
    void coalesceMergesOnlyPhysicallyContiguous() {
        var packed = SkinningGeometryPacker.pack(model());
        BitSet both = new BitSet();
        both.set(0);
        both.set(1);
        // slot0 [0,6) + slot1 [6,18) 连续 → 合并为一次 draw
        assertArrayEquals(new int[]{0, 18},
                SkinningGeometryPacker.coalesceRanges(both, packed.firstIndex(), packed.indexCount(), 2));

        BitSet onlySecond = new BitSet();
        onlySecond.set(1);
        assertArrayEquals(new int[]{6, 12},
                SkinningGeometryPacker.coalesceRanges(onlySecond, packed.firstIndex(), packed.indexCount(), 2));

        assertArrayEquals(new int[0],
                SkinningGeometryPacker.coalesceRanges(new BitSet(), packed.firstIndex(), packed.indexCount(), 2));
    }

    @Test
    void layoutOffsetsMatchStd140() {
        assertEquals(32, SkinningLayout.poseOffset(0));
        assertEquals(96, SkinningLayout.poseOffset(1));
        assertEquals(32 + 64 * 3, SkinningLayout.normalArrayOffset(3));
        assertEquals(32 + 64 * 3 + 64, SkinningLayout.normalOffset(1, 3));
        assertEquals(32 + 128 * 96, SkinningLayout.blockSize(SkinningLayout.MAX_BONES));
        // GL 3.3 保证 MAX_UNIFORM_BLOCK_SIZE ≥ 16384
        assertTrue(SkinningLayout.blockSize(SkinningLayout.MAX_BONES) <= 16384);
        assertTrue(SkinningLayout.skinnable(1));
        assertTrue(SkinningLayout.skinnable(96));
        assertFalse(SkinningLayout.skinnable(97));
        assertFalse(SkinningLayout.skinnable(0));
    }

    @Test
    void emptyModelPacksToNull() {
        assertNull(SkinningGeometryPacker.pack(new BakedModel(new Int2ObjectOpenHashMap<>())));
    }
}
