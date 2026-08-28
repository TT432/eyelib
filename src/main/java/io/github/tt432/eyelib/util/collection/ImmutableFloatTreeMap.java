package io.github.tt432.eyelib.util.collection;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 不可变的浮点键树形映射，支持 floor、lower、higher 查询。
 *
 * @author TT432
 */
@Getter
public sealed class ImmutableFloatTreeMap<V> {
    private ImmutableFloatTreeMap(float[] sortedKeys, Float2ObjectOpenHashMap<V> data) {
        this.sortedKeys = sortedKeys;
        this.data = data;
        // 与 sortedKeys 平行的值数组：采样热路径（floorHigherIndices/valueAt）免除
        // Float2ObjectOpenHashMap 的哈希+探针，一次二分后直接索引取值。
        this.values = new Object[sortedKeys.length];
        for (int i = 0; i < sortedKeys.length; i++) {
            this.values[i] = data.get(sortedKeys[i]);
        }
    }

    public static final class Empty<V> extends ImmutableFloatTreeMap<V> {

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        private static class S {
            private static final Empty<?> INSTANCE = new Empty<>();
        }

        private Empty() {
            super(new float[0], new Float2ObjectOpenHashMap<>());
        }

        @Override
        @Nullable
        public V floorEntry(float currentTick) {
            return null;
        }

        @Override
        @Nullable
        public V lowerEntry(float tick) {
            return null;
        }

        @Override
        @Nullable
        public V higherEntry(float currentTick) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <V> ImmutableFloatTreeMap<V> empty() {
        return (ImmutableFloatTreeMap<V>) Empty.S.INSTANCE;
    }

    public static <V> Codec<ImmutableFloatTreeMap<V>> codec(Codec<V> valueCodec) {
        return Codec.unboundedMap(Codec.STRING, valueCodec)
                .xmap(map -> {
                    Map<Float, V> newMap = new HashMap<>();
                    map.forEach((k, v) -> newMap.put(Float.parseFloat(k), v));
                    float[] floatArray = new FloatArrayList(newMap.keySet()).toFloatArray();
                    Arrays.sort(floatArray);
                    return of(floatArray, new Float2ObjectOpenHashMap<>(newMap));
                }, map -> {
                    Map<String, V> newMap = new HashMap<>();
                    map.data.forEach((k, v) -> newMap.put(String.valueOf(k), v));
                    return newMap;
                });
    }

    public static <V> Codec<ImmutableFloatTreeMap<V>> dispatched(Function<Float, Codec<V>> valueCodec) {
        return CodecHelper.dispatchedMap(Codec.STRING, s -> valueCodec.apply(Float.parseFloat(s)))
                .xmap(map -> {
                    Map<Float, V> newMap = new HashMap<>();
                    map.forEach((k, v) -> newMap.put(Float.parseFloat(k), v));
                    float[] floatArray = new FloatArrayList(newMap.keySet()).toFloatArray();
                    Arrays.sort(floatArray);
                    return of(floatArray, new Float2ObjectOpenHashMap<>(newMap));
                }, map -> {
                    Map<String, V> newMap = new HashMap<>();
                    map.data.forEach((k, v) -> newMap.put(String.valueOf(k), v));
                    return newMap;
                });
    }

    private final float[] sortedKeys;
    @Getter
    private final Float2ObjectOpenHashMap<V> data;
    @Getter(AccessLevel.NONE)
    private final Object[] values;

    public static <V> ImmutableFloatTreeMap<V> of(float[] sortedKeys, Float2ObjectOpenHashMap<V> data) {
        if (sortedKeys.length == 0) return empty();
        else return new ImmutableFloatTreeMap<>(sortedKeys, data);
    }

    public static <V> ImmutableFloatTreeMap<V> of(V value) {
        return new ImmutableFloatTreeMap<>(new float[]{0}, new Float2ObjectOpenHashMap<>(Map.of(0F, value)));
    }

    public boolean isEmpty() {
        return sortedKeys.length == 0;
    }
    /**
     * 单次二分同时定位 floor/higher 索引：高 32 位 = floorIndex，低 32 位 = higherIndex，
     * 不存在的一侧为 -1。与 {@link #floorEntry}/{@link #higherEntry} 逐次调用语义等价
     * （命中：floor=s, higher=s+1；未命中：floor=ins-1, higher=ins，均越界即 -1），
     * 供采样热路径用一次二分替代两次。
     */
    /**
     * 线性扫描阈值：小数组下顺序循环（{@link Float#compare} 为 intrinsic，分支可预测）
     * 优于 {@code Arrays.binarySearch} 的调用与分支误测开销；超过阈值仍走二分。
     * JFR 实证二分为采样热路径可辨识成本（world n384 渲染线程 ~3.4%，Opt14）。
     * 诊断：-Deyelib.anim.linearScanThreshold=0 禁用线性路径（回退恒二分）。
     */
    private static final int LINEAR_SCAN_THRESHOLD =
            Integer.getInteger("eyelib.anim.linearScanThreshold", 16);

    public long floorHigherIndices(float tick) {
        float[] keys = sortedKeys;
        int n = keys.length;
        if (n <= LINEAR_SCAN_THRESHOLD) {
            // Float.compare 与 Arrays.binarySearch(float[],float) 使用同一全序
            // （-0.0 < 0.0、NaN 最大、位级相等才算命中；构造期 keys 按同序排序），
            // 故与下方二分分支结果逐位等价——含 NaN/-0.0 tick 的病态输入
            int floor = -1;
            for (int i = 0; i < n; i++) {
                if (Float.compare(keys[i], tick) <= 0) {
                    floor = i;
                } else {
                    return ((long) floor << 32) | (i & 0xFFFFFFFFL);
                }
            }
            return ((long) floor << 32) | 0xFFFFFFFFL;
        }
        int s = Arrays.binarySearch(keys, tick);
        int floor;
        int higher;
        if (s >= 0) {
            floor = s;
            higher = s + 1 < sortedKeys.length ? s + 1 : -1;
        } else {
            int insertion = -s - 1;
            floor = insertion - 1;
            higher = insertion < sortedKeys.length ? insertion : -1;
        }
        return ((long) floor << 32) | (higher & 0xFFFFFFFFL);
    }

    /** 按 {@link #floorHigherIndices} 返回的索引直接取值；索引必须来自本实例的定位结果。 */
    @Nullable
    @SuppressWarnings("unchecked")
    public V valueAt(int index) {
        return (V) values[index];
    }

    @Nullable
    public V floorEntry(float currentTick) {
        int index = Arrays.binarySearch(sortedKeys, currentTick);

        if (index >= 0) {
            return data.get(sortedKeys[index]);
        } else {
            int closestBeforeIndex = -index - 2;

            if (closestBeforeIndex >= 0) {
                return data.get(sortedKeys[closestBeforeIndex]);
            }
        }

        return null;
    }

    @Nullable
    public V lowerEntry(float tick) {
        int index = Arrays.binarySearch(sortedKeys, tick);

        if (index >= 0) {
            if (index - 1 >= 0)
                return data.get(sortedKeys[index - 1]);
        } else {
            int closestBeforeIndex = -index - 2;

            if (closestBeforeIndex >= 0) {
                return data.get(sortedKeys[closestBeforeIndex]);
            }
        }

        return null;
    }

    @Nullable
    public V higherEntry(float currentTick) {
        int index = Arrays.binarySearch(sortedKeys, currentTick);

        if (index >= 0) {
            if (index + 1 < data.size())
                return data.get(sortedKeys[index + 1]);
        } else {
            index = -index - 1;

            if (index < data.size())
                return data.get(sortedKeys[index]);
        }

        return null;
    }
}