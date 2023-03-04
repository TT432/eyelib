package io.github.tt432.eyelib.molang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.MinecraftForgeClient;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangVariableControl {
    public static void registerAll(MolangVariableScope scope) {
        common(scope);
        entity(scope);
        living(scope);
    }

    public static void common(MolangVariableScope scope) {
        scope.setVariable("query.actor_count", s -> (double) Minecraft.getInstance().level.getEntityCount());
        scope.setVariable("query.time_of_day", s -> (double) MolangValue.normalizeTime(Minecraft.getInstance().level.getDayTime()));
        scope.setVariable("query.moon_phase", s -> (double) Minecraft.getInstance().level.getMoonPhase());
    }

    public static void entity(MolangVariableScope scope) {
        scope.setVariable("query.distance_from_camera", s -> entityDouble(s, entity -> Minecraft.getInstance()
                .gameRenderer.getMainCamera().getPosition().distanceTo(entity.position())));
        scope.setVariable("query.is_on_ground", s -> entityBool(s, Entity::isOnGround));
        scope.setVariable("query.is_in_water", s -> entityBool(s, Entity::isInWater));
        scope.setVariable("query.is_in_water_or_rain", s -> entityBool(s, Entity::isInWaterRainOrBubble));

        scope.setVariable("query.yaw", s -> entityDouble(s, e -> e.getViewYRot(MinecraftForgeClient.getPartialTick())));
        scope.setVariable("query.yaw_speed", s -> entityDouble(s, e ->
                e.getViewYRot(MinecraftForgeClient.getPartialTick()) -
                        e.getViewYRot(MinecraftForgeClient.getPartialTick() - 0.1F)));
        scope.setVariable("query.pitch", s -> entityDouble(s, e ->
                e.getViewXRot(MinecraftForgeClient.getPartialTick())));

        scope.setVariable("query.sitting", s -> entityBool(s, e -> e.isPassenger() &&
                (e.getVehicle() != null && e.getVehicle().shouldRiderSit())));
    }

    public static void living(MolangVariableScope scope) {
        scope.setVariable("query.has_helmet", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()));
        scope.setVariable("query.has_chestplate", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty()));
        scope.setVariable("query.has_leggings", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty()));
        scope.setVariable("query.has_boots", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.FEET).isEmpty()));

        scope.setVariable("query.health", s -> livingDouble(s, LivingEntity::getHealth));
        scope.setVariable("query.max_health", s -> livingDouble(s, LivingEntity::getMaxHealth));
        scope.setVariable("query.is_on_fire", s -> livingBool(s, LivingEntity::isOnFire));
        scope.setVariable("query.ground_speed", s -> livingDouble(s, livingEntity -> {
            Vec3 velocity = livingEntity.getDeltaMovement();
            return Mth.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z))) * 20;
        }));
        scope.setVariable("query.vertical_speed", s -> livingDouble(s, living -> living.getDeltaMovement().y * 20));

        scope.setVariable("query.head_yaw", s -> livingDouble(s, LivingEntity::getYHeadRot));
        scope.setVariable("query.head_yaw_speed", s -> livingDouble(s, e -> (e.getYHeadRot() - e.yHeadRotO) / 20));
        scope.setVariable("query.body_yaw", s -> livingDouble(s, e -> e.yBodyRot));
        scope.setVariable("query.body_yaw_speed", s -> livingDouble(s, e -> (e.yBodyRot - e.yBodyRotO) / 20));
        scope.setVariable("query.head_yaw_offset", s -> livingDouble(s, living -> {
            float partialTick = MinecraftForgeClient.getPartialTick();
            float bRot = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            float hRot = Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot);
            float netHeadYaw = hRot - bRot;

            if (s.getAsBool("query.sitting") && living.getVehicle() instanceof LivingEntity) {
                float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
                bRot = hRot - clampedHeadYaw;

                if (clampedHeadYaw * clampedHeadYaw > 2500f)
                    bRot += clampedHeadYaw * 0.2f;

                netHeadYaw = hRot - bRot;
            }
            return -netHeadYaw;
        }));

        scope.setVariable("query.baby", s -> livingBool(s, LivingEntity::isBaby));
    }

    static Double entityDouble(MolangVariableScope scope, ToDoubleFunction<Entity> func) {
        Entity entity = scope.getDataSource().get(Entity.class);

        if (entity != null) {
            return func.applyAsDouble(entity);
        }

        return 0.;
    }

    static Double entityBool(MolangVariableScope scope, Predicate<Entity> predicate) {
        return entityDouble(scope, entity -> predicate.test(entity) ? MolangValue.TRUE : MolangValue.FALSE);
    }

    static Double livingDouble(MolangVariableScope scope, ToDoubleFunction<LivingEntity> func) {
        LivingEntity livingEntity = scope.getDataSource().get(LivingEntity.class);

        if (livingEntity != null) {
            return func.applyAsDouble(livingEntity);
        }

        return 0.;
    }

    static Double livingBool(MolangVariableScope scope, Predicate<LivingEntity> predicate) {
        return livingDouble(scope, living -> predicate.test(living) ? MolangValue.TRUE : MolangValue.FALSE);
    }
}
