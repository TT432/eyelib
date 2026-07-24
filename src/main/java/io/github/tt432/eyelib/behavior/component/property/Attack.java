package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * minecraft:attack — 近战攻击伤害与命中附加状态效果。
 *
 * @author TT432
 */
public record Attack(
        String damage,
        float effect_duration,
        String effect_name,
        int effect_amplifier
) implements Component {
    /**
     * damage 兼容 Bedrock 三种形态：数字、字符串（Molang）、[min, max] 范围数组。
     * 范围数组统一序列化为 "[min, max]" 字符串；encode 始终写回字符串形态（Bedrock 合法）。
     */
    private static final Codec<String> DAMAGE_CODEC = Codec.either(
            Codec.either(Codec.INT, Codec.STRING),
            Codec.DOUBLE.listOf()
    ).xmap(
            e -> e.map(
                    inner -> inner.map(Object::toString, s -> s),
                    Attack::formatRange
            ),
            s -> Either.left(Either.right(s))
    );

    private static String formatRange(List<Double> range) {
        return range.stream()
                .map(Attack::formatNumber)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) && !Double.isInfinite(value)
                ? Long.toString((long) value)
                : Double.toString(value);
    }

    public static final Codec<Attack> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            DAMAGE_CODEC.fieldOf("damage").forGetter(Attack::damage),
            Codec.FLOAT.optionalFieldOf("effect_duration", 0f).forGetter(Attack::effect_duration),
            Codec.STRING.optionalFieldOf("effect_name", "").forGetter(Attack::effect_name),
            Codec.INT.optionalFieldOf("effect_amplifier", 0).forGetter(Attack::effect_amplifier)
    ).apply(ins, Attack::new));

    @Override
    public String id() {
        return "attack";
    }
}
