package io.github.tt432.eyelib.common.behavior.event.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
@AllArgsConstructor
public sealed class ComplexFilter implements Filter permits ComplexFilter.AllOf, ComplexFilter.OneOf, ComplexFilter.NoneOf {
    public final List<Filter> filters;

    public static final MapCodec<ComplexFilter> CODEC = EyelibCodec.list(() -> Map.of(
            "all_of", new EyelibCodec.CodecInfo<>(AllOf.class, AllOf.CODEC),
            "one_of", new EyelibCodec.CodecInfo<>(OneOf.class, OneOf.CODEC),
            "none_of", new EyelibCodec.CodecInfo<>(NoneOf.class, NoneOf.CODEC)
    ));

    public static final class AllOf extends ComplexFilter {
        public static final Codec<AllOf> CODEC = Filter.CODEC.listOf().xmap(AllOf::new, f -> f.filters);

        public AllOf(List<Filter> filters) {
            super(filters);
        }
    }

    public static final class OneOf extends ComplexFilter {
        public static final Codec<OneOf> CODEC = Filter.CODEC.listOf().xmap(OneOf::new, f -> f.filters);

        public OneOf(List<Filter> filters) {
            super(filters);
        }
    }

    public static final class NoneOf extends ComplexFilter {
        public static final Codec<NoneOf> CODEC = Filter.CODEC.listOf().xmap(NoneOf::new, f -> f.filters);

        public NoneOf(List<Filter> filters) {
            super(filters);
        }
    }
}
