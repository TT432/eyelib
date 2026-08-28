package io.github.tt432.eyelib.util.collection;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ImmutableFloatTreeMap#floorHigherIndices} 线性扫描路径契约：
 * 对 ≤16 键数组必须与二分路径（oracle，迁移前实现）逐位等价，
 * 覆盖精确命中/键间/全下/全上/NaN/-0.0/±Inf 等病态 tick。
 *
 * @author TT432
 */
class ImmutableFloatTreeMapLinearScanTest {

    /** 迁移前实现（二分）作为 oracle。 */
    private static long oracle(float[] keys, float tick) {
        int s = java.util.Arrays.binarySearch(keys, tick);
        int floor;
        int higher;
        if (s >= 0) {
            floor = s;
            higher = s + 1 < keys.length ? s + 1 : -1;
        } else {
            int insertion = -s - 1;
            floor = insertion - 1;
            higher = insertion < keys.length ? insertion : -1;
        }
        return ((long) floor << 32) | (higher & 0xFFFFFFFFL);
    }

    private static ImmutableFloatTreeMap<String> mapOf(float[] keys) {
        Float2ObjectOpenHashMap<String> data = new Float2ObjectOpenHashMap<>();
        for (float k : keys) {
            data.put(k, "v" + Float.floatToIntBits(k));
        }
        return ImmutableFloatTreeMap.of(keys, data);
    }

    private static float[] ticksFor(float[] keys) {
        java.util.List<Float> ticks = new java.util.ArrayList<>();
        ticks.add(Float.NaN);
        ticks.add(Float.NEGATIVE_INFINITY);
        ticks.add(Float.POSITIVE_INFINITY);
        ticks.add(0.0f);
        ticks.add(-0.0f);
        ticks.add(Float.MIN_VALUE);
        ticks.add(-Float.MIN_VALUE);
        for (float k : keys) {
            ticks.add(k); // 精确命中
            ticks.add(Math.nextDown(k));
            ticks.add(Math.nextUp(k));
        }
        if (keys.length > 0) {
            ticks.add(keys[0] - 1.0f); // 全下
            ticks.add(keys[keys.length - 1] + 1.0f); // 全上
        }
        for (int i = 0; i + 1 < keys.length; i++) {
            ticks.add((keys[i] + keys[i + 1]) / 2.0f); // 键间
        }
        float[] result = new float[ticks.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ticks.get(i);
        }
        return result;
    }

    private static void assertEquivalent(float[] keys) {
        ImmutableFloatTreeMap<String> map = mapOf(keys);
        for (float tick : ticksFor(keys)) {
            long expected = oracle(keys, tick);
            long actual = map.floorHigherIndices(tick);
            assertEquals(expected, actual,
                    "keys=" + java.util.Arrays.toString(keys) + " tick=" + tick
                            + " (bits=" + Float.floatToIntBits(tick) + ")");
        }
    }

    @Test
    void smallArraysMatchBinaryOracle() {
        // 覆盖 ≤16（线性路径）与 >16（二分路径），含负值/-0.0/0.0/极端值
        float[][] cases = {
                {0.0f},
                {-0.0f},
                {-0.0f, 0.0f},
                {1.0f},
                {-5.0f, 0.0f, 5.0f},
                {0.0f, 0.5f, 1.0f, 1.5f, 2.0f},
                {-1e30f, -1.0f, -0.0f, 0.0f, Float.MIN_VALUE, 1.0f, 1e30f},
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, // 16：线性上界
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, // 17：二分路径
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                        21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31}, // 32：二分
        };
        for (float[] keys : cases) {
            assertEquivalent(keys);
        }
    }

    @Test
    void denseMonotonicKeysExhaustive() {
        // 0..15 每个整数键 × 每个半整数 tick 全枚举
        float[] keys = new float[16];
        for (int i = 0; i < 16; i++) {
            keys[i] = i;
        }
        ImmutableFloatTreeMap<String> map = mapOf(keys);
        for (int t = -10; t <= 170; t++) {
            float tick = t / 10.0f;
            assertEquals(oracle(keys, tick), map.floorHigherIndices(tick), "tick=" + tick);
        }
    }
}
