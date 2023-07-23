package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.leggings_is")
public class LeggingsIs extends SlotGetter {
    public LeggingsIs() throws IllegalArgumentException {
        super(EquipmentSlot.LEGS);
    }
}
