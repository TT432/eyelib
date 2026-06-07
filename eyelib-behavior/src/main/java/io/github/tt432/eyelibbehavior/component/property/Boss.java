package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:boss
 *
 * @param hud_range         默认 55
 * @param name              默认 ""
 * @param should_darken_sky 默认 false
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Boss(
        int hud_range,
        String name,
        boolean should_darken_sky
) implements Component {
    public static final Codec<Boss> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("hud_range", 55).forGetter(Boss::hud_range),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Boss::name),
            Codec.BOOL.optionalFieldOf("should_darken_sky", false).forGetter(Boss::should_darken_sky)
    ).apply(ins, Boss::new));

    @Override
    public String id() {
        return "boss";
    }
}
