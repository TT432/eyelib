package io.github.tt432.eyelibbehavior.event.filter;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.EntityBehaviorData;
import io.github.tt432.eyelibbehavior.event.filter.base.BaseFilter;

/**
 * 过滤器接口，支持基础过滤与复合过滤的编解码。
 *
 * @author TT432
 */
public interface Filter {
    Codec<Filter> CODEC = Codec.either(BaseFilter.CODEC, ComplexFilter.CODEC.codec()).xmap(
            e -> e.left().map(b -> (Filter) b).orElse(e.right().orElseThrow()),
            v -> {
                if (v instanceof BaseFilter<?> baseFilter) {
                    return Either.left(baseFilter);
                } else if (v instanceof ComplexFilter complexFilter) {
                    return Either.right(complexFilter);
                } else {
                    throw new IllegalStateException("Unexpected value: " + v);
                }
            });

    /**
     * 评估过滤器是否通过。
     *
     * @param data 实体行为数据
     * @return 过滤器评估结果
     */
    boolean eval(EntityBehaviorData data);
}