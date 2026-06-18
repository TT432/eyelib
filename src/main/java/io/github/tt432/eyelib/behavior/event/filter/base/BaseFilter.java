package io.github.tt432.eyelib.behavior.event.filter.base;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.event.filter.Filter;
import io.github.tt432.eyelib.behavior.event.filter.Operator;
import io.github.tt432.eyelib.behavior.event.filter.Subject;
import lombok.AllArgsConstructor;

/**
 * 过滤器的抽象基类，封装通用过滤字段。
 *
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
                    //? if <1.20.6 {
                    return ActorHealth.CODEC.codec();
                    //?} else {
                    return ActorHealth.CODEC.fieldOf("value");
                    //?}
                } else {
                    throw new IllegalStateException("Unexpected value: " + s);
                }
            });

    /**
     * 获取要比较的实体值（由子类实现）。
     *
     * @param data 实体行为数据
     * @return 实体当前值
     */
    protected abstract float getComparisonValue(EntityBehaviorData data);

    /**
     * 获取过滤器中设定的比较值。
     *
     * @return 过滤器设定的值
     */
    protected abstract float getFilterValue();

    @Override
    public boolean eval(EntityBehaviorData data) {
        float entityValue = getComparisonValue(data);
        float filterValue = getFilterValue();
        return switch (operator) {
            case EQUALS, EQ2, EQ -> entityValue == filterValue;
            case NEQ, NEQ2 -> entityValue != filterValue;
            case LESS -> entityValue < filterValue;
            case GREATER -> entityValue > filterValue;
            case LESS_EQUAL -> entityValue <= filterValue;
            case GREATER_EQUAL -> entityValue >= filterValue;
            case NOT -> entityValue != filterValue;
        };
    }
}
