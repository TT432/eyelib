package io.github.tt432.eyelib.common.behavior.event.filter.base;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.common.behavior.event.filter.Filter;
import io.github.tt432.eyelib.common.behavior.event.filter.Operator;
import io.github.tt432.eyelib.common.behavior.event.filter.Subject;
import lombok.AllArgsConstructor;

/**
 * @author TT432
 */
@AllArgsConstructor
public abstract sealed class BaseFilter<T> implements Filter permits ActorHealth {
    protected final T value;
    protected final Subject subject;
    protected final Operator operator;
    protected final String domain;

    public static final Codec<BaseFilter<?>> CODEC = Codec.STRING.dispatch("test",
            v -> switch (v) {
                case ActorHealth ah -> "actor_health";
                default -> throw new IllegalStateException("Unexpected value: " + v);
            },
            s -> switch (s) {
                case "actor_health" -> ActorHealth.CODEC;
                default -> throw new IllegalStateException("Unexpected value: " + s);
            });
}
