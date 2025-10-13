package io.github.tt432.eyelib.util.codec;

import com.mojang.serialization.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibCodec {
    public static final Codec<Float> STR_FLOAT_CODEC =
            CodecHelper.withAlternative(Codec.FLOAT, Codec.STRING.xmap(Float::parseFloat, String::valueOf));

    public record CodecInfo<T>(Class<T> aClass, Codec<T> codec) {
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
                        .map(p -> p.mapFirst(k -> ops.getStringValue(k).getOrThrow(false, IllegalArgumentException::new)))
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
}