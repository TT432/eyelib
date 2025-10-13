package io.github.tt432.eyelib.util;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.CodecHelper;
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
        return CodecHelper.dispatchedMap(Codec.STRING, s -> valueCodec.apply(Float.parseFloat(s)))
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

    public static <V> ImmutableFloatTreeMap<V> of(float[] sortedKeys, Float2ObjectOpenHashMap<V> data) {
        if (sortedKeys.length == 0) return empty();
        else return new ImmutableFloatTreeMap<>(sortedKeys, data);
    }

    public static <V> ImmutableFloatTreeMap<V> of(V value) {
        return new ImmutableFloatTreeMap<>(new float[]{0}, new Float2ObjectOpenHashMap<>(Map.of(0F, value)));
    }

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
