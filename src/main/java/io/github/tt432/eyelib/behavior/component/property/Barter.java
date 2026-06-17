package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:barter
 *
 * @param barter_table                  barter loot table path
 * @param cooldown_after_being_attacked cooldown after being attacked (default 0)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Barter(
        String barter_table,
        int cooldown_after_being_attacked
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Barter> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("barter_table").forGetter(Barter::barter_table),
            Codec.INT.optionalFieldOf("cooldown_after_being_attacked", 0).forGetter(Barter::cooldown_after_being_attacked)
    ).apply(inst, Barter::new));

    @Override
    public String id() {
        return "barter";
    }
}
