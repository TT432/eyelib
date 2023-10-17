package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * @author DustW
 */
public class SlotGetter extends MolangFunction {
    EquipmentSlot slot;

    protected SlotGetter(EquipmentSlot slot) throws IllegalArgumentException {
        this.slot = slot;
    }

    @Override
    public float invoke(MolangFunctionParameters params) {
        if (!(params.scope().getOwner().getOwner() instanceof LivingEntity livingEntity))
            return FALSE;

        return params.svalues().anyMatch(item -> new ResourceLocation(item)
                .equals(ForgeRegistries.ITEMS.getKey(livingEntity.getItemBySlot(slot).getItem())))
                ? TRUE
                : FALSE;
    }
}
