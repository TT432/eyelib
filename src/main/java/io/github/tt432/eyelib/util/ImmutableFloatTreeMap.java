package io.github.tt432.eyelib.util;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author TT432
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public sealed class ImmutableFloatTreeMap<V> {

    public static final class Empty<V> extends ImmutableFloatTreeMap<V> {

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        private static class S {
            private static final Empty<?> INSTANCE = new Empty<>();
        }

        private Empty() {
            super(new float[0], new Float2ObjectOpenHashMap<>());
        }

        @Override
        public V floorEntry(float currentTick) {
            return null;
        }

        @Override
        public V lowerEntry(float tick) {
            return null;
        }

        @Override
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
                    map.data.forEach((k, v) -> newMap.put(k.toString(), v));
                    return newMap;
                });
    }

    public static <V> Codec<ImmutableFloatTreeMap<V>> dispatched(Function<Float, Codec<V>> valueCodec) {
        return Codec.dispatchedMap(Codec.STRING, s -> valueCodec.apply(Float.parseFloat(s)))
                .xmap(map -> {
                    Map<Float, V> newMap = new HashMap<>();
                    map.forEach((k, v) -> newMap.put(Float.parseFloat(k), v));
                    float[] floatArray = new FloatArrayList(newMap.keySet()).toFloatArray();
                    Arrays.sort(floatArray);
                    return of(floatArray, new Float2ObjectOpenHashMap<>(newMap));
                }, map -> {
                    Map<String, V> newMap = new HashMap<>();
                    map.data.forEach((k, v) -> newMap.put(k.toString(), v));
                    return newMap;
                });
    }

    private final float[] sortedKeys;
    private final Float2ObjectOpenHashMap<V> data;
    // .01s 索引参数与缓存（直接缓存 V[] 结果）
    private static final int INDEX_SCALE = 100;
    private final int indexStartSlot;
    private final int indexEndSlot;
    private final V[] floorValueBySlot;
    private final V[] lowerValueBySlot;
    private final V[] higherValueBySlot;
    private final V firstValue;
    private final V lastValue;

    private static int toSlot(float value) {
        return (int) (value * INDEX_SCALE);
    }

    @SuppressWarnings("unchecked")
    private ImmutableFloatTreeMap(float[] sortedKeys, Float2ObjectOpenHashMap<V> data) {
        this.sortedKeys = sortedKeys;
        this.data = data;

        if (sortedKeys.length == 0) {
            this.indexStartSlot = 0;
            this.indexEndSlot = -1;
            this.floorValueBySlot = null;
            this.lowerValueBySlot = null;
            this.higherValueBySlot = null;
            this.firstValue = null;
            this.lastValue = null;
            return;
        }

        this.indexStartSlot = toSlot(sortedKeys[0]);
        this.indexEndSlot = toSlot(sortedKeys[sortedKeys.length - 1]);
        int len = indexEndSlot - indexStartSlot + 1;
        this.floorValueBySlot = (V[]) new Object[len];
        this.lowerValueBySlot = (V[]) new Object[len];
        this.higherValueBySlot = (V[]) new Object[len];
        this.firstValue = data.get(sortedKeys[0]);
        this.lastValue = data.get(sortedKeys[sortedKeys.length - 1]);

        int fi = -1;
        int keyIdx = 0;
        for (int s = indexStartSlot; s <= indexEndSlot; s++) {
            float threshold = s / (float) INDEX_SCALE;
            while (keyIdx < sortedKeys.length && sortedKeys[keyIdx] <= threshold) {
                fi = keyIdx;
                keyIdx++;
            }
            int arrIndex = s - indexStartSlot;
            if (fi >= 0) {
                V floorVal = data.get(sortedKeys[fi]);
                this.floorValueBySlot[arrIndex] = floorVal;
                int lowerIdx = (toSlot(sortedKeys[fi]) == s) ? (fi - 1) : fi;
                this.lowerValueBySlot[arrIndex] = lowerIdx >= 0 ? data.get(sortedKeys[lowerIdx]) : null;
                int hi = fi + 1;
                this.higherValueBySlot[arrIndex] = hi < sortedKeys.length ? data.get(sortedKeys[hi]) : null;
            } else {
                this.floorValueBySlot[arrIndex] = null;
                this.lowerValueBySlot[arrIndex] = null;
                this.higherValueBySlot[arrIndex] = sortedKeys.length > 0 ? data.get(sortedKeys[0]) : null;
            }
        }
    }

    public static <V> ImmutableFloatTreeMap<V> of(float[] sortedKeys, Float2ObjectOpenHashMap<V> data) {
        if (sortedKeys.length == 0) return empty();
        else return new ImmutableFloatTreeMap<>(sortedKeys, data);
    }

    public static <V> ImmutableFloatTreeMap<V> of(V value) {
        return new ImmutableFloatTreeMap<>(new float[]{0}, new Float2ObjectOpenHashMap<>(Map.of(0F, value)));
    }

    public V floorEntry(float currentTick) {
        if (sortedKeys.length == 0) return null;
        int slot = toSlot(currentTick);
        if (slot < indexStartSlot) return null;
        if (slot > indexEndSlot) return lastValue;
        return floorValueBySlot[slot - indexStartSlot];
    }

    public V lowerEntry(float tick) {
        if (sortedKeys.length == 0) return null;
        int slot = toSlot(tick);
        if (slot < indexStartSlot) return null;
        if (slot > indexEndSlot) return lastValue;
        return lowerValueBySlot[slot - indexStartSlot];
    }

    public V higherEntry(float currentTick) {
        if (sortedKeys.length == 0) return null;
        int slot = toSlot(currentTick);
        if (slot < indexStartSlot) return firstValue;
        if (slot > indexEndSlot) return null;
        return higherValueBySlot[slot - indexStartSlot];
    }
}
