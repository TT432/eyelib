package io.github.tt432.eyelibbridge.molang;

import io.github.tt432.eyelibmolang.port.PortEntity;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;

/**
 * 将 MC Entity 适配为 PortEntity，提供 Bedrock Molang 查询属性。
 *
 * @author TT432
 */
@NullMarked
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
            props.put("is_sheep", entity instanceof net.minecraft.world.entity.animal.Sheep);
            props.put("is_wolf", entity instanceof net.minecraft.world.entity.animal.Wolf);
            props.put("is_creeper", entity instanceof net.minecraft.world.entity.monster.Creeper);
            props.put("is_vex", entity instanceof net.minecraft.world.entity.monster.Vex);
            props.put("is_warden", entity instanceof net.minecraft.world.entity.monster.warden.Warden);
            props.put("is_villager", entity instanceof net.minecraft.world.entity.npc.Villager);
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
                props.put("is_sheared", le instanceof net.minecraft.world.entity.animal.Sheep s && s.isSheared());
                props.put("is_angry", le instanceof net.minecraft.world.entity.NeutralMob nm && nm.isAngry());
                props.put("is_saddled", le instanceof net.minecraft.world.entity.animal.horse.AbstractHorse ah && ah.isSaddled());
                props.put("is_carrying_block", le instanceof net.minecraft.world.entity.monster.EnderMan em && em.getCarriedBlock() != null);
                props.put("is_chested", le instanceof net.minecraft.world.entity.animal.horse.AbstractChestedHorse ach && ach.hasChest());
                props.put("is_powered", le instanceof net.minecraft.world.entity.PowerableMob pm && pm.isPowered());
                props.put("is_standing", le instanceof net.minecraft.world.entity.animal.horse.AbstractHorse ah && ah.isStanding());
                props.put("is_charging", le instanceof net.minecraft.world.entity.monster.Vex v && v.isCharging());
                props.put("is_tamed", le instanceof net.minecraft.world.entity.TamableAnimal ta && ta.isTame());
            }
            // 位置
            props.put("pos_x", (float) entity.getX());
            props.put("pos_y", (float) entity.getY());
            props.put("pos_z", (float) entity.getZ());
            return props;
        }
    }
}
