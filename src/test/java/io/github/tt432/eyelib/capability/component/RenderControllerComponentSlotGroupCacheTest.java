package io.github.tt432.eyelib.capability.component;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link RenderControllerComponent.Slot#materialGroups} 值相等键缓存契约：
 * 值不变零重建、任一键分量变化重建、modelVersion 变化失效、baseVis 与分组对齐回填。
 */
class RenderControllerComponentSlotGroupCacheTest {

    private static LinkedHashMap<String, Set<Integer>> groups(String... names) {
        LinkedHashMap<String, Set<Integer>> result = new LinkedHashMap<>();
        for (String name : names) {
            result.put(name, Set.of(1));
        }
        return result;
    }

    @Test
    void hitOnEqualValuesReturnsSameInstanceWithoutRebuild() {
        RenderControllerComponent.Slot slot = new RenderControllerComponent().syncSlot(0, null);
        AtomicInteger rebuilds = new AtomicInteger();
        LinkedHashMap<String, Set<Integer>> first = groups("a");

        assertSame(first, slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return first;
        }));
        // 值相等（新 List 实例）→ 命中，零重建
        assertSame(first, slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("x");
        }));
        assertEquals(1, rebuilds.get());
    }

    @Test
    void colorComparedByValueNotIdentity() {
        RenderControllerComponent.Slot slot = new RenderControllerComponent().syncSlot(0, null);
        AtomicInteger rebuilds = new AtomicInteger();
        LinkedHashMap<String, Set<Integer>> first = groups("a");

        slot.materialGroups("geo", List.of("a"), new float[]{1, 0, 0, 1}, () -> {
            rebuilds.incrementAndGet();
            return first;
        });
        // 等值新数组 → 命中
        assertSame(first, slot.materialGroups("geo", List.of("a"), new float[]{1, 0, 0, 1}, () -> {
            rebuilds.incrementAndGet();
            return groups("x");
        }));
        // null → 非 null → 重建
        slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        slot.materialGroups("geo", List.of("a"), new float[]{1, 0, 0, 1}, () -> {
            rebuilds.incrementAndGet();
            return first;
        });
        assertEquals(3, rebuilds.get());
    }

    @Test
    void anyKeyComponentChangeRebuilds() {
        RenderControllerComponent.Slot slot = new RenderControllerComponent().syncSlot(0, null);
        AtomicInteger rebuilds = new AtomicInteger();

        slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        // geometry 变
        slot.materialGroups("geo2", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        // 材质值变
        slot.materialGroups("geo2", List.of("b"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        // 材质数量变
        slot.materialGroups("geo2", List.of("b", "c"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        assertEquals(4, rebuilds.get());
    }

    @Test
    void modelVersionChangeInvalidates() {
        RenderControllerComponent.Slot slot = new RenderControllerComponent().syncSlot(0, null);
        AtomicInteger rebuilds = new AtomicInteger();

        // 生产时序：allBoneIds/matchBones 恒先于 materialGroups，先稳定 modelVersion
        slot.matchBones("*", List.of(), 7);
        slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        // 同 version 命中
        slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("x");
        });
        assertEquals(1, rebuilds.get());
        // modelVersion 变 → 缓存失效 → 重建
        slot.matchBones("*", List.of(), 8);
        slot.materialGroups("geo", List.of("a"), null, () -> {
            rebuilds.incrementAndGet();
            return groups("a");
        });
        assertEquals(2, rebuilds.get());
    }

    @Test
    void baseVisStoredOnRebuildAndAligned() {
        RenderControllerComponent.Slot slot = new RenderControllerComponent().syncSlot(0, null);

        slot.materialGroups("geo", List.of("a"), null, () -> {
            LinkedHashMap<String, Set<Integer>> groups = groups("a", "b");
            Int2BooleanOpenHashMap v0 = new Int2BooleanOpenHashMap();
            v0.put(1, true);
            Int2BooleanOpenHashMap v1 = new Int2BooleanOpenHashMap();
            v1.put(2, false);
            slot.storeBaseVis(List.of(v0, v1));
            return groups;
        });

        List<Int2BooleanOpenHashMap> baseVis = slot.baseVis();
        assertEquals(2, baseVis.size());
        assertEquals(true, baseVis.get(0).get(1));
        assertEquals(false, baseVis.get(1).get(2));
        // 命中路径不清空 baseVis
        slot.materialGroups("geo", List.of("a"), null, () -> groups("z"));
        assertEquals(2, slot.baseVis().size());
    }
}
