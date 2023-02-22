package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.helmet_is")
public class HelmetIs extends SlotGetter {
    public HelmetIs(MolangValue[] values, String name) throws IllegalArgumentException {
        super(EquipmentSlot.HEAD, values, name);
    }
}
