package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.chestplate_is")
public class ChestplateIs extends SlotGetter {
    public ChestplateIs() throws IllegalArgumentException {
        super(EquipmentSlot.CHEST);
    }
}
