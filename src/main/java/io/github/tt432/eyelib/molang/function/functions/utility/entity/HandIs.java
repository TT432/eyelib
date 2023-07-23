package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * @author DustW
 */
public class HandIs extends MolangFunction {
    InteractionHand hand;

    protected HandIs(InteractionHand hand) throws IllegalArgumentException {
        this.hand = hand;
    }

    @Override
    public float invoke(MolangFunctionParameters params) {
        if (!(params.scope().getOwner().instance() instanceof LivingEntity living))
            return FALSE;

        if (hand == InteractionHand.MAIN_HAND) {
            return params.svalues().anyMatch(v -> Objects.equals(
                    ForgeRegistries.ITEMS.getKey(living.getMainHandItem().getItem()),
                    new ResourceLocation(v))) ? TRUE : FALSE;
        } else {
            return params.svalues().anyMatch(v -> Objects.equals(
                    ForgeRegistries.ITEMS.getKey(living.getOffhandItem().getItem()),
                    new ResourceLocation(v))) ? TRUE : FALSE;
        }
    }
}