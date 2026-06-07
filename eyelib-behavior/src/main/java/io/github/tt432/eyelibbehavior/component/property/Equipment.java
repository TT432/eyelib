package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:equipment
 *
 * @param table             loot table path
 * @param slot_drop_chance  list of slot drop chances
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Equipment(String table, List<SlotDrop> slot_drop_chance) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Equipment> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("table").forGetter(Equipment::table),
            SlotDrop.CODEC.listOf().fieldOf("slot_drop_chance").forGetter(Equipment::slot_drop_chance)
    ).apply(inst, Equipment::new));

    @Override
    public String id() {
        return "equipment";
    }

    public record SlotDrop(String slot, float drop_chance) {
        static final Codec<SlotDrop> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("slot").forGetter(SlotDrop::slot),
                Codec.FLOAT.optionalFieldOf("drop_chance", 1.0f).forGetter(SlotDrop::drop_chance)
        ).apply(inst, SlotDrop::new));
    }
}
