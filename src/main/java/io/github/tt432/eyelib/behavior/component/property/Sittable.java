package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:sittable
 *
 * @param sit_event   event when sitting (default target "self")
 * @param stand_event event when standing (default target "self")
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Sittable(
        EventRef sit_event,
        EventRef stand_event
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Sittable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EventRef.CODEC.optionalFieldOf("sit_event", EventRef.NONE).forGetter(Sittable::sit_event),
            EventRef.CODEC.optionalFieldOf("stand_event", EventRef.NONE).forGetter(Sittable::stand_event)
    ).apply(inst, Sittable::new));

    @Override
    public String id() {
        return "sittable";
    }
}
