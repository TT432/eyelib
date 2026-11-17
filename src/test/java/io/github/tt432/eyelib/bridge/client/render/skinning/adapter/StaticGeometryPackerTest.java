package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 静态实例几何打包（ADR-0048）的不变量：bind 骨骼链 + 模型 180° 的摊平结果与手算值一致、
 * slot 分配按骨骼 id 升序且跳过零顶点骨骼、可见分段合并、空几何返回 null。
 */
class StaticGeometryPackerTest {

    private static Model.Bone bone(int id, int parent, float x, float y, float z) {
        return Model.Bone.of(id, parent, new Vector3f(), new Vector3f(), new Vector3f(x, y, z),
                new Vector3f(1, 1, 1), null, new Int2ObjectOpenHashMap<>(), List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of()));
    }

    private static BakedModel.BakedBone quad(float[][] positions, float[][] normals) {
        int n = positions.length;
        float[] x = new float[n], y = new float[n], z = new float[n];
        float[] nx = new float[n], ny = new float[n], nz = new float[n];
        for (int i = 0; i < n; i++) {
            x[i] = positions[i][0];
            y[i] = positions[i][1];
            z[i] = positions[i][2];
            nx[i] = normals[i][0];
            ny[i] = normals[i][1];
            nz[i] = normals[i][2];
        }
        return new BakedModel.BakedBone(x, y, z, nx, ny, nz,
                new float[n * 3], new float[n * 3], new float[n * 3],
                new float[n * 3], new float[n * 3], new float[n * 3],
                new float[]{0, 1, 1, 0}, new float[]{0, 0, 1, 1});
    }

    /** 父骨骼 0 在原点，子骨骼 1 偏移 (1,0,0)。 */
    private static Model model() {
        Int2ObjectMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        bones.put(0, bone(0, -1, 0, 0, 0));
        bones.put(1, bone(1, 0, 1, 0, 0));
        return Model.of("test", bones);
    }

    @Test
    void flattensBindPoseChainWithModelYaw() {
        Int2ObjectMap<BakedModel.BakedBone> bakedBones = new Int2ObjectOpenHashMap<>();
        bakedBones.put(1, quad(
                new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}},
                new float[][]{{0, 0, 2}, {0, 0, 2}, {0, 0, 2}, {0, 0, 2}}));

        var packed = StaticGeometryPacker.pack(model(), new BakedModel(bakedBones));
        assertNotNull(packed);
        assertEquals(1, packed.vertexCount().length);
        assertEquals(0, packed.firstVertex()[0]);
        assertEquals(6, packed.vertexCount()[0]);

        var buf = packed.vertices().order(ByteOrder.LITTLE_ENDIAN);
        // 顶点 0（32B）：pos(0,4,8) / uv(12,16) / normal(20,24,28)
        // (0,0,0) → 父链 T(1,0,0) → rotY(π) → (-1,0,0)；法线 (0,0,2) 归一化后 rotY(π) → (0,0,-1)
        assertEquals(-1.0f, buf.getFloat(0), 1e-6f);
        assertEquals(0.0f, buf.getFloat(4), 1e-6f);
        assertEquals(0.0f, buf.getFloat(8), 1e-6f);
        assertEquals(0.0f, buf.getFloat(12), 1e-6f);
        assertEquals(0.0f, buf.getFloat(16), 1e-6f);
        assertEquals(0.0f, buf.getFloat(20), 1e-6f);
        assertEquals(0.0f, buf.getFloat(24), 1e-6f);
        assertEquals(-1.0f, buf.getFloat(28), 1e-6f);
        // 顶点 1：(1,0,0) → (2,0,0) → (-2,0,0)
        assertEquals(-2.0f, buf.getFloat(StaticGeometryPacker.VERTEX_STRIDE), 1e-6f);
    }

    @Test
    void slotsAscendingSkipEmptyBones() {
        Int2ObjectMap<BakedModel.BakedBone> bakedBones = new Int2ObjectOpenHashMap<>();
        bakedBones.put(5, quad(new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}},
                new float[][]{{0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}}));
        bakedBones.put(2, quad(new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}},
                new float[][]{{0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}}));

        var packed = StaticGeometryPacker.pack(model(), new BakedModel(bakedBones));
        assertNotNull(packed);
        assertArrayEquals(new int[]{2, 5}, packed.boneIds());
        assertEquals(0, packed.boneToSlot().get(2));
        assertEquals(1, packed.boneToSlot().get(5));
        // slot 0 占 0-5，slot 1 从 6 开始
        assertArrayEquals(new int[]{0, 6}, packed.firstVertex());
        assertArrayEquals(new int[]{6, 6}, packed.vertexCount());
    }

    @Test
    void coalescesOnlyContiguousVisibleSlots() {
        var ranges = StaticGeometryPacker.coalesceVisible(bits(0, 1, 3), new int[]{0, 6, 12, 18}, new int[]{6, 6, 6, 6}, 4);
        assertArrayEquals(new int[]{0, 12, 18, 6}, ranges);
    }

    @Test
    void emptyGeometryReturnsNull() {
        Int2ObjectMap<BakedModel.BakedBone> bakedBones = new Int2ObjectOpenHashMap<>();
        bakedBones.put(1, new BakedModel.BakedBone(0, new float[0], new float[0],
                new float[0], new float[0], new float[0], new float[0]));
        assertNull(StaticGeometryPacker.pack(model(), new BakedModel(bakedBones)));
    }

    private static BitSet bits(int... slots) {
        BitSet bits = new BitSet();
        for (int slot : slots) {
            bits.set(slot);
        }
        return bits;
    }
}
