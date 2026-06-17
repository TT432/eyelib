package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:push_through
 *
 * @param value push-through value (default 0.0f)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record PushThrough(float value) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<PushThrough> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("value", 0.0f).forGetter(PushThrough::value)
    ).apply(inst, PushThrough::new));

    @Override
    public String id() {
        return "push_through";
    }
}
