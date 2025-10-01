package io.github.tt432.eyelib.util.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author TT432
 */
public record KeyDispatchMapCodec<K, V>(
        Codec<K> keyCodec,
        Function<K, @NotNull Codec<? extends V>> elementCodec
) implements Codec<Map<K, V>> {

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
                    // XXX: isError -> error().isPresent() ?
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
            prefix.add(keyCodec().encodeStart(ops, entry.getKey()), ((Codec<V>) elementCodec.apply(entry.getKey())).encodeStart(ops, entry.getValue()));
        }
        return prefix;
    }
}

