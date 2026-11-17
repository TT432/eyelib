package io.github.tt432.eyelib.animation;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 静态实例判定的逐帧口径（ADR-0048）：{@link ModelRuntimeData#hasAnyEntry(IntSet)} 只在骨骼
 * 真正被动画写入时返回 true；空集、越界 id 与负 id 都不得误报。
 */
class ModelRuntimeDataEntryTest {

    @Test
    void noEntriesMeansStaticPose() {
        ModelRuntimeData data = new ModelRuntimeData();
        assertFalse(data.hasAnyEntry(ids(0, 1, 2)));
    }

    @Test
    void anyWrittenBoneBreaksStaticPose() {
        ModelRuntimeData data = new ModelRuntimeData();
        data.getData(42);
        assertTrue(data.hasAnyEntry(ids(41, 42, 43)));
        assertFalse(data.hasAnyEntry(ids(40, 41)));
    }

    @Test
    void negativeAndOutOfRangeIdsAreNotEntries() {
        ModelRuntimeData data = new ModelRuntimeData();
        data.getData(-1);
        assertTrue(data.hasAnyEntry(ids(-1)), "负 id 走 negativeEntry 槽，确实是本帧动画写入");
        assertFalse(data.hasAnyEntry(ids(900)), "越界 id 无 entry 槽");
    }

    private static IntSet ids(int... values) {
        return new IntOpenHashSet(values);
    }
}
