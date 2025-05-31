package io.github.tt432.eyelib.common.behavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Allows an entity to ignore attackable targets for a given duration.
 *
 * @param cooldown_after_being_attacked Duration, in seconds, for which mob won't admire items if it was hurt
 * @param duration                      Duration, in seconds, that the mob is pacified.
 * @author TT432
 */
public record AdmireItem(
        int cooldown_after_being_attacked,
        int duration
) implements Component {
    public static final Codec<AdmireItem> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("cooldown_after_being_attacked", 0).forGetter(AdmireItem::cooldown_after_being_attacked),
            Codec.INT.optionalFieldOf("duration", 10).forGetter(AdmireItem::duration)
    ).apply(ins, AdmireItem::new));

    @Override
    public String id() {
        return "admire_item";
    }
}
