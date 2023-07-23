package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.helmet_is")
public class HelmetIs extends SlotGetter {
    public HelmetIs() throws IllegalArgumentException {
        super(EquipmentSlot.HEAD);
    }
}
