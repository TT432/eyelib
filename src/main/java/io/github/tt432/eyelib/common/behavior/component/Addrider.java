package io.github.tt432.eyelib.common.behavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Adds a rider to the entity.
 *
 * @param entity_type Type of entity to acquire as a rider
 * @param spawn_event Trigger event when a rider is acquired
 * @author TT432
 */
public record Addrider(
        String entity_type,
        String spawn_event
) implements Component {
    public static final Codec<Addrider> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("entity_type", "").forGetter(o -> o.entity_type),
            Codec.STRING.optionalFieldOf("spawn_event", "").forGetter(o -> o.spawn_event)
    ).apply(ins, Addrider::new));

    @Override
    public String id() {
        return "addrider";
    }
}
