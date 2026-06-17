package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:pushable
 *
 * @param is_pushable          whether pushable (default true)
 * @param is_pushable_by_piston whether pushable by piston (default true)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Pushable(
        boolean is_pushable,
        boolean is_pushable_by_piston
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Pushable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("is_pushable", true).forGetter(Pushable::is_pushable),
            Codec.BOOL.optionalFieldOf("is_pushable_by_piston", true).forGetter(Pushable::is_pushable_by_piston)
    ).apply(inst, Pushable::new));

    @Override
    public String id() {
        return "pushable";
    }
}
