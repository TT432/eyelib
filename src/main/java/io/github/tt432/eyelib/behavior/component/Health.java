package io.github.tt432.eyelib.behavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:health — 实体生命值定义。
 * Bedrock 规范: { "value": int, "max": int }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Health(
        int value,
        int max
) implements Component {
    public static final Codec<Health> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("value").forGetter(Health::value),
            Codec.INT.optionalFieldOf("max", 20).forGetter(Health::max)
    ).apply(ins, Health::new));

    @Override
    public String id() {
        return "health";
    }
}
