package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * @author DustW
 */
public class SlotGetter extends MolangFunction {
    EquipmentSlot slot;

    protected SlotGetter(EquipmentSlot slot, MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, -1);

        this.slot = slot;
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        LivingEntity livingEntity = scope.getDataSource().get(LivingEntity.class);

        if (livingEntity == null)
            return FALSE;

        for (int i = 0; i < args.length; i++) {
            String item = getArgAsString(i, scope);

            if (new ResourceLocation(item).equals(livingEntity.getItemBySlot(slot).getItem().getRegistryName())) {
                return TRUE;
            }
        }

        return FALSE;
    }
}
