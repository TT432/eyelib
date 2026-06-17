package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:equippable
 *
 * @param slots list of equip slots
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Equippable(List<EquipSlot> slots) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Equippable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EquipSlot.CODEC.listOf().fieldOf("slots").forGetter(Equippable::slots)
    ).apply(inst, Equippable::new));

    @Override
    public String id() {
        return "equippable";
    }

    public record EquipSlot(
            int slot,
            List<String> accepted_items,
            String interact_text,
            EventRef on_equip,
            EventRef on_unequip
    ) {
        static final Codec<EquipSlot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("slot").forGetter(EquipSlot::slot),
                Codec.STRING.listOf().fieldOf("accepted_items").forGetter(EquipSlot::accepted_items),
                Codec.STRING.fieldOf("interact_text").forGetter(EquipSlot::interact_text),
                EventRef.CODEC.optionalFieldOf("on_equip", EventRef.NONE).forGetter(EquipSlot::on_equip),
                EventRef.CODEC.optionalFieldOf("on_unequip", EventRef.NONE).forGetter(EquipSlot::on_unequip)
        ).apply(inst, EquipSlot::new));
    }
}
