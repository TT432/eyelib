package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibCodec {
    public static final Codec<Float> STR_FLOAT_CODEC =
            Codec.withAlternative(Codec.FLOAT, Codec.STRING.xmap(Float::parseFloat, String::valueOf));

    public static <A> Codec<List<A>> singleOrList(Codec<A> codec) {
        return Codec.either(codec.xmap(List::of, List::getFirst), codec.listOf()).xmap(Either::unwrap, Either::right);
    }

    public static <A> Codec<A> check(Codec<A> sourceCodec, Function<A, DataResult<A>> checker) {
        return sourceCodec.flatXmap(checker, checker);
    }

    public static <K, V> Codec<TreeMap<K, V>> treeMap(final Codec<K> keyCodec,
                                                      final Codec<V> elementCodec,
                                                      Comparator<K> comparator) {
        return Codec.unboundedMap(keyCodec, elementCodec).xmap(map -> {
            TreeMap<K, V> result = new TreeMap<>(comparator);
            result.putAll(map);
            return result;
        }, Function.identity());
    }

    public static <T> MapCodec<T> withAlternative(final MapCodec<T> primary,
                                                  final MapCodec<? extends T> alternative) {
        return Codec.mapEither(primary, alternative).xmap(Either::unwrap, Either::left);
    }
}