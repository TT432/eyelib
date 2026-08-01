package io.github.tt432.eyelib.bridge.molang.adapter;

import io.github.tt432.eyelib.molang.port.PortEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.compiler.MolangRuntimeSupport;
import io.github.tt432.eyelib.molang.mapping.api.HostContext;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.molang.port.ArrowHostInstaller;
import io.github.tt432.eyelib.molang.type.MolangEntityRef;
import io.github.tt432.eyelib.molang.type.MolangObject;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
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

    /**
     * 将 {@link PortEntity} 还原为 MC Entity；非本适配器包装的 PortEntity 返回 {@code null}。
     */
    public static @Nullable Entity toEntity(PortEntity portEntity) {
        return portEntity instanceof PortEntityImpl impl ? impl.entity() : null;
    }

    /**
     * 注册箭头访问（{@code ->}）宿主安装器：把箭头左侧的实体引用
     * （{@link MolangEntityRef} 包装的 PortEntity）翻译为 MC Entity 并注入宿主上下文，
     * 右式的 {@code query.*} 查询即可在目标实体上求值；求值结束后恢复原宿主。
     * <p>
     * 非实体引用（数字/字符串）不触发切换，箭头退化为仅求值右式。
     */
    public static void installArrowHostBridge() {
        HostRole<Entity> entityRole = HostRole.of("arrow_entity", Entity.class);
        MolangRuntimeSupport.setArrowHostInstaller(new ArrowHostInstaller() {
            @Override
            public @Nullable Object install(MolangScope scope, MolangObject host) {
                PortEntity portEntity = host instanceof MolangEntityRef ref && ref.entity() instanceof PortEntity pe
                        ? pe : null;
                if (portEntity == null) return null;
                Entity entity = toEntity(portEntity);
                if (entity == null) return null;
                Object previousEntity = scope.getHostContext().get(entityRole).orElse(null);
                Object previousPortEntity = scope.getHostContext().get(HostRoles.PORT_ENTITY).orElse(null);
                scope.getHostContext().put(entityRole, entity);
                scope.getHostContext().put(HostRoles.PORT_ENTITY, portEntity);
                return new ArrowHostToken(entityRole, previousEntity, previousPortEntity);
            }

            @Override
            public void restore(MolangScope scope, @Nullable Object previous) {
                if (!(previous instanceof ArrowHostToken token)) return;
                putHost(scope.getHostContext(), token.entityRole(), token.previousEntity());
                putHost(scope.getHostContext(), HostRoles.PORT_ENTITY, token.previousPortEntity());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void putHost(HostContext context, HostRole<T> role, @Nullable Object value) {
        if (value == null) {
            context.remove(role);
        } else {
            context.put(role, (T) value);
        }
    }

    private record ArrowHostToken(HostRole<Entity> entityRole,
                                  @Nullable Object previousEntity,
                                  @Nullable Object previousPortEntity) {
    }

    /**
     * Bedrock q.is_on_ground 语义。JE 客户端实体的 onGround 仅随移动包同步，
     * 静止实体（如 NoAI）永远为 false；用 1mm 下移碰撞检测兜底。
     */
    public static boolean isOnGround(Entity entity) {
        return entity.onGround()
                || !entity.level().noCollision(entity, entity.getBoundingBox().move(0.0, -1.0E-3, 0.0));
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
            props.put("is_on_ground", isOnGround(entity));
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

