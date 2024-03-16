package io.github.tt432.eyelib.util;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

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

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public float lastKey() {
            return 0;
        }
    }

    public static <V> ImmutableFloatTreeMap<V> empty() {
        return (ImmutableFloatTreeMap<V>) Empty.S.INSTANCE;
    }

    private final float[] sortedKeys;
    private final Float2ObjectOpenHashMap<V> data;

    public static <V> ImmutableFloatTreeMap<V> of(float[] sortedKeys, Float2ObjectOpenHashMap<V> data) {
        if (sortedKeys.length == 0) return empty();
        else return new ImmutableFloatTreeMap<>(sortedKeys, data);
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

    public boolean isEmpty() {
        return sortedKeys.length > 0;
    }

    public float lastKey() {
        return sortedKeys[sortedKeys.length - 1];
    }
}
