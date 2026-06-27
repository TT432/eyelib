package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.molang.port.PortEntity;
import net.minecraft.world.entity.Entity;
//? if <26.1 {
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
//?} else {
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.npc.villager.Villager;
//?}
import java.util.HashMap;
import java.util.Map;

/**
 * 将 MC Entity 适配为 PortEntity，提供 Bedrock Molang 查询属性。
 *
 * @author TT432
 */
public final class EntityPortAdapter {

    private EntityPortAdapter() {}

    public static PortEntity from(Entity entity) {
        return new PortEntityImpl(entity);
    }

    private record PortEntityImpl(Entity entity) implements PortEntity {
        @Override
        public Map<String, Object> getQueryProperties() {
            Map<String, Object> props = new HashMap<>();
            // 实体类型标记
            props.put("is_sheep", entity instanceof Sheep);
            props.put("is_wolf", entity instanceof Wolf);
            props.put("is_creeper", entity instanceof net.minecraft.world.entity.monster.Creeper);
            props.put("is_vex", entity instanceof net.minecraft.world.entity.monster.Vex);
            props.put("is_warden", entity instanceof net.minecraft.world.entity.monster.warden.Warden);
            props.put("is_villager", entity instanceof Villager);
            props.put("is_camel", entity instanceof net.minecraft.world.entity.animal.camel.Camel);
            props.put("is_wither", entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss);
            props.put("is_enderman", entity instanceof net.minecraft.world.entity.monster.EnderMan);
            props.put("is_player", entity instanceof net.minecraft.world.entity.player.Player);
            // LivingEntity 属性
            boolean living = entity instanceof net.minecraft.world.entity.LivingEntity;
            props.put("is_baby", living && ((net.minecraft.world.entity.LivingEntity) entity).isBaby());
            props.put("is_sleeping", living && ((net.minecraft.world.entity.LivingEntity) entity).isSleeping());
            props.put("is_sprinting", living && ((net.minecraft.world.entity.LivingEntity) entity).isSprinting());
            // 通用实体属性
            props.put("on_fire", entity.isOnFire());
            props.put("is_on_ground", entity.onGround());
            props.put("is_in_water", entity.isInWater());
            props.put("is_riding", entity.isPassenger());
            // 实体特定行为属性
            if (living) {
                var le = (net.minecraft.world.entity.LivingEntity) entity;
                props.put("is_sheared", le instanceof Sheep s && s.isSheared());
                props.put("is_angry", le instanceof net.minecraft.world.entity.NeutralMob nm && nm.isAngry());
                props.put("is_saddled", le instanceof AbstractHorse ah && ah.isSaddled());
                props.put("is_carrying_block", le instanceof net.minecraft.world.entity.monster.EnderMan em && em.getCarriedBlock() != null);
                props.put("is_chested", le instanceof AbstractChestedHorse ach && ach.hasChest());
                props.put("is_powered", isPowered(le));
                props.put("is_standing", le instanceof AbstractHorse ah && ah.isStanding());
                props.put("is_charging", le instanceof net.minecraft.world.entity.monster.Vex v && v.isCharging());
                props.put("is_tamed", le instanceof net.minecraft.world.entity.TamableAnimal ta && ta.isTame());
            }
            // 位置
            props.put("pos_x", (float) entity.getX());
            props.put("pos_y", (float) entity.getY());
            props.put("pos_z", (float) entity.getZ());
            return props;
        }

        @Override
        public float getX() {
            return (float) entity.getX();
        }

        @Override
        public float getY() {
            return (float) entity.getY();
        }

        @Override
        public float getZ() {
            return (float) entity.getZ();
        }

        private static boolean isPowered(net.minecraft.world.entity.LivingEntity entity) {
            //? if <26.1 {
            return entity instanceof net.minecraft.world.entity.PowerableMob powerableMob && powerableMob.isPowered();
            //?} else {
            return false;
            //?}
        }
    }
}
