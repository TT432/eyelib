package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.boots_is")
public class BootsIs extends SlotGetter {
    public BootsIs() throws IllegalArgumentException {
        super(EquipmentSlot.FEET);
    }
}
