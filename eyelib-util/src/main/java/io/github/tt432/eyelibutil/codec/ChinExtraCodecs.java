package io.github.tt432.eyelibutil.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import io.github.tt432.eyelibutil.codec.TupleCodec.*;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author TT432
 * Copy from tt432/chin (https://github.com/TT432/chin)
 * As you wish!
 * 提供单个/列表兼容器、校验、树映射和元组 Codec 工厂方法。
 */
public class ChinExtraCodecs {
    /**
     * Example: <br/>
     * var codec = singleOrList(Codec.INT); <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("1")); // return List(1) <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("[1, 2]")); // return List(1, 2) <br/>
     */
    public static <A> Codec<List<A>> singleOrList(Codec<A> codec) {
        return Codec.either(codec.xmap(List::of, l -> l.get(0)), codec.listOf()).xmap(Eithers::unwrap, Either::right);
    }

    /**
     * Example: <br/>
     * var codec = check(Codec.STRING, s -> s.equals("1.23") ? DataResult.success(s) : DataResult.error(() -> "Not equals 1.23")); <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("\"1.23\"")); // return "1.23" <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("\"2.23\"")); // throw <br/>
     *
     * @see Codec#intRange(int, int)
     */
    public static <A> Codec<A> check(Codec<A> sourceCodec, Function<A, DataResult<A>> checker) {
        return sourceCodec.flatXmap(checker, checker);
    }

    /**
     * @see Codec#unboundedMap(Codec, Codec)
     */
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
        return Codec.mapEither(primary, alternative).xmap(Eithers::unwrap, Either::left);
    }

    public static <A> T1Codec<A> tuple(Codec<A> codec1) {
        return new T1Codec<>(codec1);
    }

    public static <A, B> T2Codec<A, B> tuple(Codec<A> codec1, Codec<B> codec2) {
        return new T2Codec<>(codec1, codec2);
    }

    public static <A, B, C> T3Codec<A, B, C> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3) {
        return new T3Codec<>(codec1, codec2, codec3);
    }

    public static <A, B, C, D> T4Codec<A, B, C, D> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4) {
        return new T4Codec<>(codec1, codec2, codec3, codec4);
    }

    public static <A, B, C, D, E> T5Codec<A, B, C, D, E> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5) {
        return new T5Codec<>(codec1, codec2, codec3, codec4, codec5);
    }

    public static <A, B, C, D, E, F> T6Codec<A, B, C, D, E, F> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6) {
        return new T6Codec<>(codec1, codec2, codec3, codec4, codec5, codec6);
    }

    public static <A, B, C, D, E, F, G> T7Codec<A, B, C, D, E, F, G> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7) {
        return new T7Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7);
    }

    public static <A, B, C, D, E, F, G, H> T8Codec<A, B, C, D, E, F, G, H> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8) {
        return new T8Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8);
    }

    public static <A, B, C, D, E, F, G, H, I> T9Codec<A, B, C, D, E, F, G, H, I> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9) {
        return new T9Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9);
    }

    public static <A, B, C, D, E, F, G, H, I, J> T10Codec<A, B, C, D, E, F, G, H, I, J> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10) {
        return new T10Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10);
    }

    public static <A, B, C, D, E, F, G, H, I, J, K> T11Codec<A, B, C, D, E, F, G, H, I, J, K> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10, Codec<K> codec11) {
        return new T11Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10, codec11);
    }

    public static <A, B, C, D, E, F, G, H, I, J, K, L> T12Codec<A, B, C, D, E, F, G, H, I, J, K, L> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10, Codec<K> codec11, Codec<L> codec12) {
        return new T12Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10, codec11, codec12);
    }

    public static <A, B, C, D, E, F, G, H, I, J, K, L, M> T13Codec<A, B, C, D, E, F, G, H, I, J, K, L, M> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10, Codec<K> codec11, Codec<L> codec12, Codec<M> codec13) {
        return new T13Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10, codec11, codec12, codec13);
    }

    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N> T14Codec<A, B, C, D, E, F, G, H, I, J, K, L, M, N> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10, Codec<K> codec11, Codec<L> codec12, Codec<M> codec13, Codec<N> codec14) {
        return new T14Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10, codec11, codec12, codec13, codec14);
    }

    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> T15Codec<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10, Codec<K> codec11, Codec<L> codec12, Codec<M> codec13, Codec<N> codec14, Codec<O> codec15) {
        return new T15Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10, codec11, codec12, codec13, codec14, codec15);
    }

    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> T16Codec<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> tuple(Codec<A> codec1, Codec<B> codec2, Codec<C> codec3, Codec<D> codec4, Codec<E> codec5, Codec<F> codec6, Codec<G> codec7, Codec<H> codec8, Codec<I> codec9, Codec<J> codec10, Codec<K> codec11, Codec<L> codec12, Codec<M> codec13, Codec<N> codec14, Codec<O> codec15, Codec<P> codec16) {
        return new T16Codec<>(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9, codec10, codec11, codec12, codec13, codec14, codec15, codec16);
    }
}