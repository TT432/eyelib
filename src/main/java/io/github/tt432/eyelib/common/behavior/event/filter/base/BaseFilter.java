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
            v -> {
                if (v instanceof ActorHealth) {
                    return "actor_health";
                } else {
                    throw new IllegalStateException("Unexpected value: " + v);
                }
            },
            s -> {
                if (s.equals("actor_health")) {
                    return ActorHealth.CODEC.codec();
                } else {
                    throw new IllegalStateException("Unexpected value: " + s);
                }
            });
}
