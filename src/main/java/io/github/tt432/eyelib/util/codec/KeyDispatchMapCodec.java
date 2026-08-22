package io.github.tt432.eyelib.util.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 按键分派的 Map Codec，使用键 Codec 解析出对应的元素 Codec。
 *
 * @author TT432
 */
public record KeyDispatchMapCodec<K, V>(
        Codec<K> keyCodec,
        Function<K, Codec<? extends V>> elementCodec,
        /**
         * 可选的编码期值分派：优先于 {@link #elementCodec} 按值实例选择编码器。
         * 用于 RawComponent 这类"键命中 typed codec、但值是原始 JSON 兜底"的场景，
         * 避免对值做强转导致 ClassCastException。
         */
        @Nullable Function<V, @Nullable Codec<? extends V>> valueCodec
) implements Codec<Map<K, V>> {

    public KeyDispatchMapCodec(Codec<K> keyCodec, Function<K, Codec<? extends V>> elementCodec) {
        this(keyCodec, elementCodec, null);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyDispatchMapCodec.class);

    @Override
    public <T> DataResult<Pair<Map<K, V>, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(map -> decode(ops, map)).map(r -> Pair.of(r, input));
    }

    @Override
    public <T> DataResult<T> encode(final Map<K, V> input, final DynamicOps<T> ops, final T prefix) {
        return encode(input, ops, ops.mapBuilder()).build(prefix);
    }

    @Override
    public String toString() {
        return "KeyDispatchMapCodec[" + keyCodec + " -> " + elementCodec + ']';
    }

    <T> DataResult<Map<K, V>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final Object2ObjectMap<K, V> read = new Object2ObjectArrayMap<>();
        final Stream.Builder<Pair<T, T>> failed = Stream.builder();

        final DataResult<Unit> result = input.entries().reduce(
                DataResult.success(Unit.INSTANCE, Lifecycle.stable()),
                (r, pair) -> {
                    final DataResult<K> key = keyCodec().parse(ops, pair.getFirst());
                    final DataResult<? extends V> value = key.flatMap(s -> elementCodec.apply(s).parse(ops, pair.getSecond()));

                    final DataResult<Pair<K, V>> entryResult = key.apply2stable(Pair::of, value);
                    final Optional<Pair<K, V>> entry = entryResult.resultOrPartial(LOGGER::warn);
                    if (entry.isPresent()) {
                        final V existingValue = read.putIfAbsent(entry.get().getFirst(), entry.get().getSecond());
                        if (existingValue != null) {
                            failed.add(pair);
                            return r.apply2stable((u, p) -> u, DataResult.error(() -> "Duplicate entry for key: '" + entry.get().getFirst() + "'"));
                        }
                    }
                    if (entryResult.error().isPresent()) {
                        failed.add(pair);
                    }

                    return r.apply2stable((u, p) -> u, entryResult);
                },
                (r1, r2) -> r1.apply2stable((u1, u2) -> u1, r2)
        );

        final Map<K, V> elements = ImmutableMap.copyOf(read);
        final T errors = ops.createMap(failed.build());

        return result.map(unit -> elements).setPartial(elements).mapError(e -> e + " missed input: " + errors);
    }

    @SuppressWarnings("unchecked")
    <T> RecordBuilder<T> encode(final Map<K, V> input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        for (final Map.Entry<K, V> entry : input.entrySet()) {
            Codec<? extends V> valueEncoder = valueCodec != null ? valueCodec.apply(entry.getValue()) : null;
            if (valueEncoder == null) {
                valueEncoder = elementCodec.apply(entry.getKey());
            }
            prefix.add(keyCodec().encodeStart(ops, entry.getKey()), ((Codec<V>) valueEncoder).encodeStart(ops, entry.getValue()));
        }
        return prefix;
    }
}
