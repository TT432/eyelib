package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.molang.MolangScope;

import java.util.List;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * @author TT432
 */
public record AnimationEffect<V>(
        TreeMap<Float, List<V>> data,
        BiConsumer<MolangScope, V> action
) {
    public Runtime<V> runtime() {
        return new Runtime<>(new TreeMap<>(data), action);
    }

    public record Runtime<V>(
            TreeMap<Float, List<V>> data,
            BiConsumer<MolangScope, V> action
    ) {
        public static <V> void processEffect(Runtime<V> runtime, float ticks, MolangScope scope) {
            if (runtime != null && !runtime.data().isEmpty() && runtime.data().firstKey() < ticks) {
                runtime.data().pollFirstEntry().getValue().forEach(v -> runtime.action().accept(scope, v));
            }
        }
    }

    public static final AnimationEffect<?> EMPTY = new AnimationEffect<>(new TreeMap<>(), (s, v) -> {
    });

    @SuppressWarnings("unchecked")
    public static <V> AnimationEffect<V> empty() {
        return (AnimationEffect<V>) EMPTY;
    }
}
