package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.boots_is")
public class BootsIs extends SlotGetter {
    public BootsIs(MolangValue[] values, String name) throws IllegalArgumentException {
        super(EquipmentSlot.FEET, values, name);
    }
}
