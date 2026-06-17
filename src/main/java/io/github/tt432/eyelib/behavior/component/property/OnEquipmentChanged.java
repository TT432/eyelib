package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:on_equipment_changed — 实体装备变化时触发事件。
 *
 * @author TT432
 */
public record OnEquipmentChanged(
        String event,
        String target
) implements Component {
    public static final Codec<OnEquipmentChanged> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnEquipmentChanged::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnEquipmentChanged::target)
    ).apply(ins, OnEquipmentChanged::new));

    @Override
    public String id() {
        return "on_equipment_changed";
    }
}
