package io.github.tt432.eyelib.common.behavior.event.filter;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.common.behavior.event.filter.base.BaseFilter;

/**
 * @author TT432
 */
public interface Filter {
    Codec<Filter> CODEC = Codec.either(BaseFilter.CODEC, ComplexFilter.CODEC.codec()).xmap(
            e -> e.left().map(b -> (Filter) b).orElse(e.right().orElseThrow()),
            v -> switch (v) {
                case BaseFilter<?> baseFilter -> Either.left(baseFilter);
                case ComplexFilter complexFilter -> Either.right(complexFilter);
                default -> throw new IllegalStateException("Unexpected value: " + v);
            });
}
