package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:insomnia
 *
 * @param days_until_insomnia days until insomnia (default 3.0f)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Insomnia(float days_until_insomnia) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Insomnia> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("days_until_insomnia", 3.0f).forGetter(Insomnia::days_until_insomnia)
    ).apply(inst, Insomnia::new));

    @Override
    public String id() {
        return "insomnia";
    }
}
