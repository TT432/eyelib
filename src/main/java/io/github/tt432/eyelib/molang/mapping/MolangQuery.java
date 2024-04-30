package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Function;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * @author TT432
 */
@MolangMapping(value = "query", pureFunction = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public final class MolangQuery {
    private static float slot_getter(MolangScope scope, EquipmentSlot slot, Object... objects) {
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

    public static float actor_count(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getEntityCount();
        return 0;
    }

    public static float time_of_day(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getDayTime() / 24000F;
        return 0;
    }

    public static float moon_phase(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getMoonPhase();
        return 0;
    }

    public static float partial_tick(MolangScope scope) {
        return Minecraft.getInstance().getPartialTick();
    }

    public static float any_animation_finished(MolangScope scope) {
        return scope.getOwner().getAnimationComponent().anyAnimationFinished(partial_tick(scope))
                ? MolangValue.TRUE
                : MolangValue.FALSE;
    }

    public static float all_animation_finished(MolangScope scope) {
        return scope.getOwner().getAnimationComponent().allAnimationFinished(partial_tick(scope))
                ? MolangValue.TRUE
                : MolangValue.FALSE;
    }

    private static float entityBool(MolangScope scope, Function<Entity, Boolean> function) {
        return scope.getOwner().ownerAs(Entity.class).map(function.andThen(b -> b ? TRUE : FALSE)).orElse(0F);
    }

    private static float entityFloat(MolangScope scope, Function<Entity, Float> function) {
        return scope.getOwner().ownerAs(Entity.class).map(function).orElse(0F);
    }

    public static float distance_from_camera(MolangScope scope) {
        return entityFloat(scope, e -> {
            if (Minecraft.getInstance().cameraEntity == null) return 0F;

            return Minecraft.getInstance().cameraEntity.distanceTo(e);
        });
    }

    public static float is_on_ground(MolangScope scope) {
        return entityBool(scope, Entity::onGround);
    }

    public static float is_in_water(MolangScope scope) {
        return entityBool(scope, Entity::isInWater);
    }

    public static float is_in_water_or_rain(MolangScope scope) {
        return entityBool(scope, Entity::isInWaterRainOrBubble);
    }

    public static float yaw(MolangScope scope) {
        return entityFloat(scope, e -> e.getViewYRot(partial_tick(scope)));
    }

    public static float yaw_speed(MolangScope scope) {
        return entityFloat(scope, e -> yaw(scope) - e.getViewYRot(partial_tick(scope) - 0.1F));
    }

    public static float pitch(MolangScope scope) {
        return entityFloat(scope, e -> e.getViewXRot(partial_tick(scope)));
    }

    public static float eye_target_y_rotation(MolangScope scope) {
        return yaw(scope);
    }

    public static float eye_target_x_rotation(MolangScope scope) {
        return pitch(scope);
    }

    public static float sitting(MolangScope scope) {
        return entityBool(scope, e -> e.isPassenger() && (e.getVehicle() != null && e.getVehicle().shouldRiderSit()));
    }

    public static float is_stalking(MolangScope scope) {
        return scope.getOwner().ownerAs(Mob.class)
                .map(Mob::isAggressive)
                .map(b -> b ? TRUE : FALSE)
                .orElse(0F);
    }

    private static float livingBool(MolangScope scope, Function<LivingEntity, Boolean> function) {
        return scope.getOwner().ownerAs(LivingEntity.class)
                .map(function.andThen(b -> b ? TRUE : FALSE))
                .orElse(0F);
    }

    private static float livingFloat(MolangScope scope, Function<LivingEntity, Float> function) {
        return scope.getOwner().ownerAs(LivingEntity.class)
                .map(function)
                .orElse(0F);
    }

    public static float attack_time(MolangScope scope) {
        return livingFloat(scope, l -> l.getAttackAnim(partial_tick(scope)));
    }

    public static float is_attacking(MolangScope scope) {
        return attack_time(scope) > 0 ? TRUE : FALSE;
    }

    public static float is_powered(MolangScope scope) {
        return scope.getOwner().ownerAs(PowerableMob.class).map(c -> c.isPowered() ? TRUE : FALSE).orElse(FALSE);
    }

    public static float has_helmet(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
    }

    public static float has_chestplate(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty());
    }

    public static float has_leggings(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty());
    }

    public static float has_boots(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.FEET).isEmpty());
    }

    public static float health(MolangScope scope) {
        return livingFloat(scope, LivingEntity::getHealth);
    }

    public static float max_health(MolangScope scope) {
        return livingFloat(scope, LivingEntity::getMaxHealth);
    }

    public static float is_on_fire(MolangScope scope) {
        return livingBool(scope, LivingEntity::isOnFire);
    }

    public static float ground_speed(MolangScope scope) {
        return livingFloat(scope, livingEntity -> {
            Vec3 velocity = livingEntity.getDeltaMovement();
            return Mth.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z))) * 20;
        });
    }

    public static float vertical_speed(MolangScope scope) {
        return livingFloat(scope, living -> living.onGround() ? 0 : (float) (living.getDeltaMovement().y * 20));
    }

    public static float head_yaw(MolangScope scope) {
        return livingFloat(scope, e -> Mth.lerp(partial_tick(scope), e.yHeadRotO, e.yHeadRot));
    }

    public static float head_yaw_speed(MolangScope scope) {
        return livingFloat(scope, e -> (e.getYHeadRot() - e.yHeadRotO) / 20);
    }

    public static float body_yaw(MolangScope scope) {
        return livingFloat(scope, e -> Mth.lerp(partial_tick(scope), e.yBodyRotO, e.yBodyRot));
    }

    public static float body_yaw_speed(MolangScope scope) {
        return livingFloat(scope, e -> (e.yBodyRot - e.yBodyRotO) / 20);
    }

    public static float head_yaw_offset(MolangScope scope) {
        return livingFloat(scope, living -> {
            float hRot = head_yaw(scope);
            float bRot = body_yaw(scope);
            float netHeadYaw = hRot - bRot;

            if (sitting(scope) == TRUE && living.getVehicle() instanceof LivingEntity) {
                float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
                bRot = hRot - clampedHeadYaw;

                if (clampedHeadYaw > 500f)
                    bRot += clampedHeadYaw * 0.2f;

                netHeadYaw = hRot - bRot;
            }

            return Mth.wrapDegrees(-netHeadYaw);
        });
    }

    public static float head_pitch_offset(MolangScope scope) {
        return livingFloat(scope, living -> -Mth.lerp(partial_tick(scope), living.xRotO, living.getXRot()));
    }

    public static float baby(MolangScope scope) {
        return livingBool(scope, LivingEntity::isBaby);
    }

    public static float is_baby(MolangScope scope) {
        return livingBool(scope, LivingEntity::isBaby);
    }
}
