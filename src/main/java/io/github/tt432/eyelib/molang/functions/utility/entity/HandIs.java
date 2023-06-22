package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author DustW
 */
public class HandIs extends MolangFunction {
    InteractionHand hand;

    protected HandIs(InteractionHand hand, MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, -1);

        this.hand = hand;
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        LivingEntity living = scope.getDataSource().get(LivingEntity.class);

        if (living != null) {
            if (hand == InteractionHand.MAIN_HAND) {
                return Arrays.stream(args).anyMatch(v -> Objects.equals(
                        ForgeRegistries.ITEMS.getKey(living.getMainHandItem().getItem()),
                        new ResourceLocation(v.asString(scope)))) ? TRUE : FALSE;
            } else {
                return Arrays.stream(args).anyMatch(v -> Objects.equals(
                        ForgeRegistries.ITEMS.getKey(living.getOffhandItem().getItem()),
                        new ResourceLocation(v.asString(scope)))) ? TRUE : FALSE;
            }
        }

        return FALSE;
    }
}
