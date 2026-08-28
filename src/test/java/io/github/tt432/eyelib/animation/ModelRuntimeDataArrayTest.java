package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Opt18-C ModelRuntimeData 数组化契约：稠密骨骼 id 数组索引与旧 map 语义逐项对齐——
 * getData 懒建/扩容、getOrDefault 零值共享、position/rotation/scale 的 bind 偏移、
 * 负 id 兜底、set 的 Entry 引用共享、reset 归零。
 */
class ModelRuntimeDataArrayTest {

    private static Model.Bone bone(int id) {
        // of(id, parent, pivot, rotation, position, scale, ...)
        return Model.Bone.of(id, -1, new Vector3f(), new Vector3f(4, 5, 6), new Vector3f(1, 2, 3),
                new Vector3f(2), null, new Int2ObjectOpenHashMap<>(), List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of()));
    }

    @Test
    void getDataCreatesAndGrowsBeyondInitialCapacity() {
        ModelRuntimeData data = new ModelRuntimeData();
        ModelRuntimeData.Entry low = data.getData(1);
        ModelRuntimeData.Entry high = data.getData(500); // 超初始容量 64 → 扩容
        assertNotSame(low, high);
        assertSame(low, data.getData(1));
        assertSame(high, data.getData(500));
        high.position.set(7, 8, 9);
        assertEquals(7F, data.getData(500).position.x);
    }

    @Test
    void getOrDefaultReturnsSharedIdentityForUnsetBones() {
        ModelRuntimeData data = new ModelRuntimeData();
        ModelRuntimeData.Entry unset = data.getOrDefault(42);
        assertSame(unset, data.getOrDefault(10000));
        assertEquals(0F, unset.position.x);
        assertEquals(1F, unset.scale.x);
    }

    @Test
    void bindOffsetsApplyOnlyWhenEntryExists() {
        ModelRuntimeData data = new ModelRuntimeData();
        Model.Bone bone = bone(3);
        // 无 Entry → bind pose 原值
        assertEquals(new Vector3f(1, 2, 3), data.position(bone));
        assertEquals(new Vector3f(4, 5, 6), data.rotation(bone));
        assertEquals(new Vector3f(2), data.scale(bone));
        // 有 Entry → init + offset
        data.position(bone, 10, 20, 30);
        assertEquals(new Vector3f(11, 22, 33), data.position(bone));
    }

    @Test
    void negativeIdFallsBackToDedicatedSlot() {
        ModelRuntimeData data = new ModelRuntimeData();
        ModelRuntimeData.Entry negative = data.getData(-1);
        assertSame(negative, data.getData(-1));
        negative.rotation.set(1, 0, 0);
        assertSame(negative, data.getOrDefault(-1));
        assertEquals(1F, data.getOrDefault(-1).rotation.x);
    }

    @Test
    void setSharesEntryInstancesLikeMapPutAll() {
        ModelRuntimeData source = new ModelRuntimeData();
        source.getData(5).position.set(1, 1, 1);
        ModelRuntimeData target = new ModelRuntimeData();
        target.getData(3).position.set(9, 9, 9);
        target.set(source);
        // putAll 语义：Entry 实例引用共享
        assertSame(source.getData(5), target.getData(5));
        // reset 语义：未覆盖的旧 Entry 归零而非移除
        assertEquals(0F, target.getData(3).position.x);
    }

    @Test
    void resetZeroesAllEntriesInPlace() {
        ModelRuntimeData data = new ModelRuntimeData();
        data.getData(2).position.set(5, 5, 5);
        data.getData(-1).position.set(6, 6, 6);
        data.reset();
        assertEquals(0F, data.getData(2).position.x);
        assertEquals(0F, data.getData(-1).position.x);
    }

    @Test
    void bindBonesIndexedByBoneId() {
        ModelRuntimeData data = new ModelRuntimeData();
        Int2ObjectMap<Model.Bone> binds = new Int2ObjectOpenHashMap<>();
        Model.Bone bone = bone(7);
        binds.put(7, bone);
        data.bindBones(binds);
        assertSame(bone, data.bindBone(7));
        assertEquals(null, data.bindBone(8));
        assertEquals(null, data.bindBone(-1));
        data.bindBones(null);
        assertEquals(null, data.bindBone(7));
    }
}
