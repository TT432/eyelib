package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.leggings_is")
public class LeggingsIs extends SlotGetter {
    public LeggingsIs(MolangValue[] values, String name) throws IllegalArgumentException {
        super(EquipmentSlot.LEGS, values, name);
    }
}
