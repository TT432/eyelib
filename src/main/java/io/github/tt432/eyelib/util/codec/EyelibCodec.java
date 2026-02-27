package io.github.tt432.eyelib.util.codec;

import com.mojang.serialization.*;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.chin.util.Tuple;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.ExtraCodecs;
import org.joml.*;

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
            Codec.withAlternative(Codec.FLOAT, Codec.STRING.xmap(Float::parseFloat, String::valueOf));

    public record CodecInfo<T>(Class<T> aClass, Codec<T> codec) {
    }

    public static <V> Codec<Int2ObjectMap<V>> int2ObjectMap(final Codec<V> elementCodec) {
        return Codec.unboundedMap(Codec.INT, elementCodec)
                .xmap(Int2ObjectOpenHashMap::new, Function.identity());
    }

    public static <S> MapCodec<S> list(Supplier<Map<String, CodecInfo<? extends S>>> codecs) {
        return new MapCodec<S>() {
            private Map<String, MapCodec<S>> codecCache;
            private Map<Class<? extends S>, MapCodec<S>> classCache;

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

            Map<Class<? extends S>, MapCodec<S>> getCache() {
                get();
                return classCache;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return get().keySet().stream().map(ops::createString);
            }

            @Override
            public <T> DataResult<S> decode(DynamicOps<T> ops, MapLike<T> input) {
                return input.entries()
                        .map(p -> p.mapFirst(k -> ops.getStringValue(k).getOrThrow()))
                        .filter(p -> get().containsKey(p.getFirst()))
                        .findFirst()
                        .map(p -> get().get(p.getFirst()).fieldOf(p.getFirst()).decode(ops, input))
                        .orElse(DataResult.error(() -> "can't parse " + input + " with " + this));
            }

            @Override
            public <T> RecordBuilder<T> encode(S input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return getCache().entrySet().stream()
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
}