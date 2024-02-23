package io.github.tt432.eyelib.molang.function;

import io.github.tt432.eyelib.molang.MolangMapping;
import io.github.tt432.eyelib.molang.MolangScope;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * @author TT432
 */
@MolangMapping("query")
public class MolangQuery {
    public static float slot_getter(MolangScope scope, EquipmentSlot slot, Object... objects) {
        if (!(scope.getOwner().getOwner() instanceof LivingEntity livingEntity))
            return FALSE;

        for (Object object : objects) {
            if (new ResourceLocation(object.toString())
                    .equals(BuiltInRegistries.ITEM.getKey(livingEntity.getItemBySlot(slot).getItem()))) {
                return TRUE;
            }
        }

        return FALSE;
    }

    public static float off_hand_is(MolangScope scope, Object... objects) {
        return slot_getter(scope, EquipmentSlot.OFFHAND, objects);
    }

    public static float main_hand_is(MolangScope scope, Object... objects) {
        return slot_getter(scope, EquipmentSlot.MAINHAND, objects);
    }

    public static float leggings_is(MolangScope scope, Object... objects) {
        return slot_getter(scope, EquipmentSlot.LEGS, objects);
    }

    public static float helmet_is(MolangScope scope, Object... objects) {
        return slot_getter(scope, EquipmentSlot.HEAD, objects);
    }

    public static float chestplate_is(MolangScope scope, Object... objects) {
        return slot_getter(scope, EquipmentSlot.CHEST, objects);
    }

    public static float boots_is(MolangScope scope, Object... objects) {
        return slot_getter(scope, EquipmentSlot.FEET, objects);
    }

    public static float hand_is(MolangScope scope, InteractionHand hand, Object... objects) {
        if (!(scope.getOwner().getOwner() instanceof LivingEntity living))
            return FALSE;

        if (hand == InteractionHand.MAIN_HAND) {
            for (Object object : objects) {
                if (Objects.equals(
                        BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()),
                        new ResourceLocation(object.toString()))) {
                    return TRUE;
                }
            }
        } else {
            for (Object object : objects) {
                if (Objects.equals(
                        BuiltInRegistries.ITEM.getKey(living.getOffhandItem().getItem()),
                        new ResourceLocation(object.toString()))) {
                    return TRUE;
                }
            }
        }

        return FALSE;
    }

    public static float is_item_equipped(MolangScope scope, Object hand) {
        String arg = hand.toString();

        if (!(scope.getOwner().getOwner() instanceof LivingEntity living))
            return FALSE;

        if (arg.equals("main_hand") || arg.equals("0")) {
            return living.getMainHandItem().isEmpty() ? FALSE : TRUE;
        } else if (arg.equals("off_hand") || arg.equals("1")) {
            return living.getOffhandItem().isEmpty() ? FALSE : TRUE;
        }

        return FALSE;
    }
}
