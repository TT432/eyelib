package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.chestplate_is")
public class ChestplateIs extends SlotGetter {
    public ChestplateIs(MolangValue[] values, String name) throws IllegalArgumentException {
        super(EquipmentSlot.CHEST, values, name);
    }
}
