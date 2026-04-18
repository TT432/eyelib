package io.github.tt432.eyelib.util.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.AABB;
import org.joml.*;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibCodec {
    public static final Codec<Vector2f> VEC2F = ChinExtraCodecs.tuple(Codec.FLOAT, Codec.FLOAT)
            .bmap(Vector2f::new, v -> Tuple.of(v.x, v.y));

    public static final Codec<Vector2fc> VEC2FC = VEC2F.xmap(v -> v, Vector2f::new);
    public static final Codec<Vector3fc> VEC3FC = ExtraCodecs.VECTOR3F.xmap(v -> v, Vector3f::new);
    public static final Codec<AABB> AABB_CODEC = Codec.DOUBLE.listOf().comapFlatMap(
            l -> l.size() == 6
                    ? DataResult.success(new AABB(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5)))
                    : DataResult.error(() -> "expected 6 values for AABB, got " + l.size()),
            aabb -> List.of(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)
    );

    public static final Codec<Vector2f> FLOATS2VEC2F_CODEC = Codec.FLOAT.listOf().xmap(
            l -> new Vector2f(l.get(0), l.get(1)),
            v -> List.of(v.x, v.y)
    );

    public static final Codec<Vector3f> FLOATS2VEC3F_CODEC = Codec.FLOAT.listOf().xmap(
            l -> new Vector3f(l.get(0), l.get(1), l.get(2)),
            v -> List.of(v.x, v.y, v.z)
    );

    public static final Codec<Vector4f> FLOATS2VEC4F_CODEC = Codec.FLOAT.listOf().xmap(
            l -> new Vector4f(l.get(0), l.get(1), l.get(2), l.get(3)),
            v -> List.of(v.x, v.y, v.z, v.w)
    );

    public static final Codec<Float> STR_FLOAT_CODEC =
            CodecHelper.withAlternative(Codec.FLOAT, Codec.STRING.xmap(Float::parseFloat, String::valueOf));

    public record CodecInfo<T>(Class<T> aClass, Codec<T> codec) {
    }

    public static <V> Codec<Int2ObjectMap<V>> int2ObjectMap(final Codec<V> elementCodec) {
        return Codec.unboundedMap(Codec.INT, elementCodec)
                .xmap(Int2ObjectOpenHashMap::new, Function.identity());
    }

    public static <S> MapCodec<S> list(Supplier<Map<String, CodecInfo<? extends S>>> codecs) {
        return new MapCodec<S>() {
            private @Nullable Map<String, MapCodec<S>> codecCache;
            private @Nullable Map<Class<? extends S>, MapCodec<S>> classCache;

            @SuppressWarnings("unchecked")
            Map<String, MapCodec<S>> get() {
                if (codecCache == null) {
                    codecCache = new HashMap<>();
                    classCache = new HashMap<>();
                    var stringCodecMap = codecs.get();
                    stringCodecMap.forEach((k, v) -> {
                        MapCodec<? extends S> mapCodec = v.codec.fieldOf(k);
                        codecCache.put(k, (MapCodec<S>) mapCodec);
                        classCache.put(v.aClass, (MapCodec<S>) mapCodec);
                    });
                }

                return codecCache;
            }

            @Nullable Map<Class<? extends S>, MapCodec<S>> getCache() {
                get();
                return classCache;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return get().keySet().stream().map(ops::createString);
            }

            @Override
            public <T> DataResult<S> decode(DynamicOps<T> ops, MapLike<T> input) {
                var result = input.entries()
                        .map(p -> p.mapFirst(k -> ops.getStringValue(k).getOrThrow(false, IllegalArgumentException::new)))
                        .filter(p -> get().containsKey(p.getFirst()))
                        .findFirst()
                        .map(p -> {
                            MapCodec<S> codec = get().get(p.getFirst());
                            if (codec == null) {
                                return DataResult.<S>error(() -> "no codec for " + p.getFirst());
                            }
                            return codec.fieldOf(p.getFirst()).decode(ops, input);
                        });

                if (result.isPresent()) {
                    return result.get();
                }

                return DataResult.error(() -> "can't parse " + input + " with " + this);
            }

            @Override
            public <T> RecordBuilder<T> encode(S input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                var cache = getCache();
                if (cache == null) return prefix;
                return cache.entrySet().stream()
                        .filter(e -> e.getKey().isInstance(input))
                        .findFirst()
                        .map(e -> e.getValue().encode(input, ops, prefix))
                        .orElse(prefix);
            }
        };
    }

    public static <A> MapCodec<Optional<A>> optionalMapCodec(MapCodec<A> mapCodec) {
        return new MapCodec<>() {
            @Override
            public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.map(p -> mapCodec.encode(p, ops, prefix)).orElse(prefix);
            }

            @Override
            public <T> Stream<T> keys(final DynamicOps<T> ops) {
                return mapCodec.keys(ops);
            }

            @Override
            public <T> DataResult<Optional<A>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
                return DataResult.success(mapCodec.decode(ops, input).result());
            }

            @Override
            public String toString() {
                return mapCodec + "[mapResult Optional]";
            }
        };
    }

    public static <A> Codec<A> recursive(final String name, final Function<Codec<A>, Codec<A>> wrapped) {
        return new RecursiveCodec<>(name, wrapped);
    }

    public static <T> Codec<T> withAlternative(final Codec<T> primary, final Codec<? extends T> alternative) {
        return Codec.either(
                primary,
                alternative
        ).xmap(
                tEither -> tEither.map(Function.identity(), Function.identity()),
                Either::left
        );
    }

    static class RecursiveCodec<T> implements Codec<T> {
        private final String name;
        private final Supplier<Codec<T>> wrapped;

        private RecursiveCodec(final String name, final Function<Codec<T>, Codec<T>> wrapped) {
            this.name = name;
            this.wrapped = Suppliers.memoize(() -> wrapped.apply(this));
        }

        @Override
        public <S> DataResult<Pair<T, S>> decode(final DynamicOps<S> ops, final S input) {
            return wrapped.get().decode(ops, input);
        }

        @Override
        public <S> DataResult<S> encode(final T input, final DynamicOps<S> ops, final S prefix) {
            return wrapped.get().encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "RecursiveCodec[" + name + ']';
        }
    }
}

