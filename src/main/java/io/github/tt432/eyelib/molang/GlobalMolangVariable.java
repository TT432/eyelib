package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author TT432
 */
public class GlobalMolangVariable {
    static final Map<String, MolangVariable> MAP = new HashMap<>();

    static {
        common();
        entity();
        living();
    }

    public static void common() {
        MAP.put("math.pi", new MolangVariable(EyeMath.PI));
        MAP.put("PI", new MolangVariable(EyeMath.PI));
        MAP.put("math.e", new MolangVariable(EyeMath.E));
        MAP.put("E", new MolangVariable(EyeMath.E));

        setVariable("query.actor_count", s -> (float) Minecraft.getInstance().level.getEntityCount());
        setVariable("query.time_of_day", s -> Minecraft.getInstance().level.getDayTime() / 24000F);
        setVariable("query.moon_phase", s -> (float) Minecraft.getInstance().level.getMoonPhase());
        setVariable("query.partial_tick", s -> Minecraft.getInstance().getPartialTick());
    }

    public static void entity() {
        setVariable("query.distance_from_camera", s -> entityDouble(s, entity -> (float) Minecraft.getInstance()
                .gameRenderer.getMainCamera().getPosition().distanceTo(entity.position())));
        setVariable("query.is_on_ground", s -> entityBool(s, Entity::onGround));
        setVariable("query.is_in_water", s -> entityBool(s, Entity::isInWater));
        setVariable("query.is_in_water_or_rain", s -> entityBool(s, Entity::isInWaterRainOrBubble));

        setVariable("query.yaw", s -> entityDouble(s, e -> e.getViewYRot(s.get("query.partial_tick"))));
        setVariable("query.yaw_speed", s -> entityDouble(s, e -> e.getViewYRot(s.get("query.partial_tick"))
                - e.getViewYRot(s.get("query.partial_tick") - 0.1F)));
        setVariable("query.pitch", s -> entityDouble(s, e -> e.getViewXRot(s.get("query.partial_tick"))));

        setVariable("query.sitting", s -> entityBool(s, e -> e.isPassenger() &&
                (e.getVehicle() != null && e.getVehicle().shouldRiderSit())));
    }

    public static void living() {
        setVariable("query.is_stalking", s -> livingBool(s, living -> living instanceof Mob mob && mob.isAggressive()));
        setVariable("query.has_helmet", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()));
        setVariable("query.has_chestplate", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty()));
        setVariable("query.has_leggings", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty()));
        setVariable("query.has_boots", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.FEET).isEmpty()));

        setVariable("query.health", s -> livingDouble(s, LivingEntity::getHealth));
        setVariable("query.max_health", s -> livingDouble(s, LivingEntity::getMaxHealth));
        setVariable("query.is_on_fire", s -> livingBool(s, LivingEntity::isOnFire));
        setVariable("query.ground_speed", s -> livingDouble(s, livingEntity -> {
            Vec3 velocity = livingEntity.getDeltaMovement();
            return Mth.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z))) * 20;
        }));
        setVariable("query.vertical_speed", s -> livingDouble(s, living ->
                living.onGround() ? 0 : (float) (living.getDeltaMovement().y * 20)));

        setVariable("query.head_yaw", s -> livingDouble(s, e ->
                Mth.lerp(s.get("query.partial_tick"), e.yHeadRotO, e.yHeadRot)));
        setVariable("query.head_yaw_speed", s -> livingDouble(s, e -> (e.getYHeadRot() - e.yHeadRotO) / 20));
        setVariable("query.body_yaw", s -> livingDouble(s, e ->
                Mth.lerp(s.get("query.partial_tick"), e.yBodyRotO, e.yBodyRot)));
        setVariable("query.body_yaw_speed", s -> livingDouble(s, e -> (e.yBodyRot - e.yBodyRotO) / 20));
        setVariable("query.head_yaw_offset", s -> livingDouble(s, living -> {
            float hRot = s.get("query.head_yaw");
            float bRot = s.get("query.body_yaw");
            float netHeadYaw = hRot - bRot;

            if (s.getBool("query.sitting") && living.getVehicle() instanceof LivingEntity) {
                float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
                bRot = hRot - clampedHeadYaw;

                if (clampedHeadYaw > 500f)
                    bRot += clampedHeadYaw * 0.2f;

                netHeadYaw = hRot - bRot;
            }

            return Mth.wrapDegrees(-netHeadYaw);
        }));
        setVariable("query.head_pitch_offset", s -> livingDouble(s, living ->
                -Mth.lerp(s.get("query.partial_tick"), living.xRotO, living.getXRot())));

        setVariable("query.baby", s -> livingBool(s, LivingEntity::isBaby));
    }

    static void setVariable(String var, Function<MolangScope, Float> mapper) {
        MAP.put(var, new MolangVariable(mapper));
    }

    private static float entityBool(MolangScope s, Function<Entity, Boolean> mapper) {
        return s.owner.getOwner() instanceof Entity e
                ? mapper.apply(e) ? MolangValue.TRUE : MolangValue.FALSE
                : MolangValue.FALSE;
    }

    private static float entityDouble(MolangScope s, Function<Entity, Float> mapper) {
        return s.owner.getOwner() instanceof Entity e ? mapper.apply(e) : 0;
    }

    private static float livingBool(MolangScope s, Function<LivingEntity, Boolean> mapper) {
        return s.owner.getOwner() instanceof LivingEntity e
                ? mapper.apply(e) ? MolangValue.TRUE : MolangValue.FALSE
                : MolangValue.FALSE;
    }

    private static Float livingDouble(MolangScope s, Function<LivingEntity, Float> mapper) {
        return s.owner.getOwner() instanceof LivingEntity e ? mapper.apply(e) : 0;
    }

    public static MolangVariable get(String var) {
        return MAP.get(var);
    }

    public static boolean contains(String var) {
        return MAP.containsKey(var);
    }
}
