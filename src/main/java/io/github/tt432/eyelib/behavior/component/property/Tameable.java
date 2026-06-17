package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:tameable
 *
 * @param probability tame probability (default 1.0f)
 * @param tame_items  list of tame items (empty by default)
 * @param tame_event  event fired when tamed (default target "self")
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Tameable(
        float probability,
        List<String> tame_items,
        EventRef tame_event
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Tameable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("probability", 1.0f).forGetter(Tameable::probability),
            Codec.STRING.listOf().optionalFieldOf("tame_items", List.of()).forGetter(Tameable::tame_items),
            EventRef.CODEC.optionalFieldOf("tame_event", EventRef.NONE).forGetter(Tameable::tame_event)
    ).apply(inst, Tameable::new));

    @Override
    public String id() {
        return "tameable";
    }
}
