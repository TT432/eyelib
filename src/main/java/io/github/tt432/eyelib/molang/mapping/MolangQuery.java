package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
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
    @MolangFunction(value = "has_main_hand", description = "主手有物品")
    public static float hasMainHand(MolangScope scope) {
        return livingBool(scope, e -> !e.getMainHandItem().isEmpty());
    }

    @MolangFunction(value = "has_off_hand", description = "副手有物品")
    public static float hasOffHand(MolangScope scope) {
        return livingBool(scope, e -> !e.getOffhandItem().isEmpty());
    }

    @MolangFunction(value = "has_helmet", description = "已装备头盔")
    public static float hasHelmet(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
    }

    @MolangFunction(value = "has_chestplate", description = "已装备胸甲")
    public static float hasChestplate(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty());
    }

    @MolangFunction(value = "has_leggings", description = "已装备护腿")
    public static float hasLeggings(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty());
    }

    @MolangFunction(value = "has_boots", description = "已装备靴子")
    public static float hasBoots(MolangScope scope) {
        return livingBool(scope, living -> !living.getItemBySlot(EquipmentSlot.FEET).isEmpty());
    }

    @MolangFunction(value = "off_hand_is", description = "副手是指定物品之一")
    public static float offHandIs(MolangScope scope, Object... objects) {
        return slotGetter(scope, EquipmentSlot.OFFHAND, objects);
    }

    @MolangFunction(value = "main_hand_is", description = "主手是指定物品之一")
    public static float mainHandIs(MolangScope scope, Object... objects) {
        return slotGetter(scope, EquipmentSlot.MAINHAND, objects);
    }

    @MolangFunction(value = "leggings_is", description = "护腿是指定物品之一")
    public static float leggingsIs(MolangScope scope, Object... objects) {
        return slotGetter(scope, EquipmentSlot.LEGS, objects);
    }

    @MolangFunction(value = "helmet_is", description = "头盔是指定物品之一")
    public static float helmetIs(MolangScope scope, Object... objects) {
        return slotGetter(scope, EquipmentSlot.HEAD, objects);
    }

    @MolangFunction(value = "chestplate_is", description = "胸甲是指定物品之一")
    public static float chestplateIs(MolangScope scope, Object... objects) {
        return slotGetter(scope, EquipmentSlot.CHEST, objects);
    }

    @MolangFunction(value = "boots_is", description = "鞋子是指定物品之一")
    public static float bootsIs(MolangScope scope, Object... objects) {
        return slotGetter(scope, EquipmentSlot.FEET, objects);
    }

    @MolangFunction(value = "is_item_equipped", description = "指定手上是否有物品 (有效参数 主手='main_hand' 或 0; 副手= 'off_hand' 或 1)")
    public static float isItemEquipped(MolangScope scope, Object hand) {
        return scope.getOwner().ownerAs(LivingEntity.class).map(living -> (switch (hand.toString()) {
            case "main_hand", "0" -> living.getMainHandItem();
            case "off_hand", "1" -> living.getOffhandItem();
            default -> ItemStack.EMPTY;
        }).isEmpty() ? FALSE : TRUE).orElse(FALSE);
    }

    @MolangFunction(value = "actor_count", description = "世界中实体的数量")
    public static float actorCount(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getEntityCount();
        return 0;
    }

    @MolangFunction(value = "time_of_day", description = "一天中的时间")
    public static float timeOfDay(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getDayTime() / 24000F;
        return 0;
    }

    @MolangFunction(value = "moon_phase", description = "月相")
    public static float moonPhase(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getMoonPhase();
        return 0;
    }

    @MolangFunction(value = "partial_tick", description = "距离上一帧的时间")
    public static float partialTick(MolangScope scope) {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
    }

    @MolangFunction(value = "any_animation_finished", description = "任意动画播放完毕（动画控制器）")
    public static float anyAnimationFinished(MolangScope scope) {
        return scope.getOwner().ownerAs(RenderData.class).map(data ->
                        data.getAnimationComponent().anyAnimationFinished(ClientTickHandler.getTick() + partialTick(scope))
                                ? MolangValue.TRUE
                                : MolangValue.FALSE)
                .orElse(FALSE);
    }

    @MolangFunction(value = "all_animations_finished", description = "所有动画播放完毕（动画控制器）")
    public static float allAnimationsFinished(MolangScope scope) {
        return scope.getOwner().ownerAs(RenderData.class).map(data ->
                        data.getAnimationComponent().allAnimationsFinished(ClientTickHandler.getTick() + partialTick(scope))
                                ? MolangValue.TRUE
                                : MolangValue.FALSE)
                .orElse(FALSE);
    }

    @MolangFunction(value = "distance_from_camera", description = "距离摄像头的距离")
    public static float distanceFromCamera(MolangScope scope) {
        return entityFloat(scope, e -> {
            if (Minecraft.getInstance().cameraEntity == null) return 0F;

            return Minecraft.getInstance().cameraEntity.distanceTo(e);
        });
    }

    @MolangFunction(value = "is_on_ground", description = "正处于地面上")
    public static float isOnGround(MolangScope scope) {
        return entityBool(scope, Entity::onGround);
    }

    @MolangFunction(value = "fall_distance", description = "摔落的距离")
    public static float fallDistance(MolangScope scope) {
        return entityFloat(scope, e -> e.fallDistance);
    }

    @MolangFunction(value = "is_jumping", description = "正在跳跃")
    public static float isJumping(MolangScope scope) {
        return livingBool(scope, entity -> entity.jumping);
    }

    @MolangFunction(value = "is_sprinting", description = "正在冲刺（疾跑）")
    public static float isSprinting(MolangScope scope) {
        return entityBool(scope, Entity::isSprinting);
    }

    @MolangFunction(value = "is_crouching", alias = "is_sneaking", description = "正在蹲下")
    public static float isCrouching(MolangScope scope) {
        return entityBool(scope, Entity::isCrouching);
    }

    @MolangFunction(value = "is_on_climbable", alias = "is_wall_climbing", description = "正处于可以攀爬的方块内（比如梯子或藤蔓）")
    public static float isOnClimbable(MolangScope scope) {
        return livingBool(scope, LivingEntity::onClimbable);
    }

    @MolangFunction(value = "is_swimming", description = "正在游泳")
    public static float isSwimming(MolangScope scope) {
        return entityBool(scope, Entity::isSwimming);
    }

    @MolangFunction(value = "is_digging", description = "正在挖掘（玩家）")
    public static float isDigging(MolangScope scope) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        return (gameMode != null && gameMode.isDestroying()) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_eating", description = "正在食用物品")
    public static float isEating(MolangScope scope) {
        return livingBool(scope, p -> p.isUsingItem() && p.getUseItem().getUseAnimation() == UseAnim.EAT);
    }

    @MolangFunction(value = "is_drinking", description = "正在饮用物品")
    public static float isDrinking(MolangScope scope) {
        return livingBool(scope, p -> p.isUsingItem() && p.getUseItem().getUseAnimation() == UseAnim.DRINK);
    }

    @MolangFunction(value = "is_main_hand_swing", description = "主手正在挥动")
    public static float isMainHandSwing(MolangScope scope) {
        return livingBool(scope, e -> e.swinging && e.swingingArm == InteractionHand.MAIN_HAND);
    }

    @MolangFunction(value = "is_off_hand_swing", description = "副手正在挥动")
    public static float isOffHandSwing(MolangScope scope) {
        return livingBool(scope, e -> e.swinging && e.swingingArm == InteractionHand.OFF_HAND);
    }

    @MolangFunction(value = "is_in_water", description = "正在水中")
    public static float isInWater(MolangScope scope) {
        return entityBool(scope, Entity::isInWater);
    }

    @MolangFunction(value = "is_in_water_or_rain", description = "正在水中或在雨中")
    public static float isInWaterOrRain(MolangScope scope) {
        return entityBool(scope, Entity::isInWaterRainOrBubble);
    }

    @MolangFunction(value = "eye_target_y_rotation", alias = "yaw", description = "yaw 角度（y rot）")
    public static float yaw(MolangScope scope) {
        return entityFloat(scope, e -> e.getViewYRot(partialTick(scope)));
    }

    @MolangFunction(value = "yaw_speed", description = "yaw 轴旋转速度")
    public static float yawSpeed(MolangScope scope) {
        return entityFloat(scope, e -> yaw(scope) - e.getViewYRot(partialTick(scope) - 0.1F));
    }

    @MolangFunction(value = "eye_target_x_rotation", alias = "pitch", description = "pitch 角度（x rot）")
    public static float pitch(MolangScope scope) {
        return entityFloat(scope, e -> e.getViewXRot(partialTick(scope)));
    }

    @MolangFunction(value = "sitting", alias = "is_sitting", description = "正在坐")
    public static float sitting(MolangScope scope) {
        return entityBool(scope, e -> e.isPassenger() && (e.getVehicle() != null && e.getVehicle().shouldRiderSit()));
    }

    @MolangFunction(value = "pos_x", description = "实体 x 位置")
    public static float posX(MolangScope scope) {
        return entityFloat(scope, e -> (float) e.position().x);
    }

    @MolangFunction(value = "pos_y", description = "实体 y 位置")
    public static float posY(MolangScope scope) {
        return entityFloat(scope, e -> (float) e.position().y);
    }

    @MolangFunction(value = "pos_z", description = "实体 z 位置")
    public static float posZ(MolangScope scope) {
        return entityFloat(scope, e -> (float) e.position().z);
    }

    @MolangFunction(value = "climbing_x", description = "实体位置指向可攀爬方块中心的向量的 x 分量")
    public static float climbingX(MolangScope scope) {
        return livingFloat(scope, e -> e.getLastClimbablePos()
                .map(p ->
                        new Vec3(p.getX(), p.getY(), p.getZ())
                                .add(e.level().getBlockState(p).getCollisionShape(e.level(), p).bounds().getCenter())
                                .subtract(e.position())
                                .x)
                .orElse(0D)
                .floatValue());
    }

    @MolangFunction(value = "climbing_y", description = "实体位置指向可攀爬方块中心的向量的 y 分量")
    public static float climbingY(MolangScope scope) {
        return livingFloat(scope, e -> e.getLastClimbablePos()
                .map(p ->
                        new Vec3(p.getX(), p.getY(), p.getZ())
                                .add(e.level().getBlockState(p).getCollisionShape(e.level(), p).bounds().getCenter())
                                .subtract(e.position())
                                .y)
                .orElse(0D)
                .floatValue());
    }

    @MolangFunction(value = "climbing_z", description = "实体位置指向可攀爬方块中心的向量的 z 分量")
    public static float climbingZ(MolangScope scope) {
        return livingFloat(scope, e -> e.getLastClimbablePos()
                .map(p -> new Vec3(p.getX(), p.getY(), p.getZ())
                        .add(e.level().getBlockState(p).getCollisionShape(e.level(), p).bounds().getCenter())
                        .subtract(e.position())
                        .z)
                .orElse(0D)
                .floatValue());
    }

    @MolangFunction(value = "is_crawling", description = "爬行（在陆地上被活板门等方块挤压导致趴下的动作）")
    public static float isCrawling(MolangScope scope) {
        return entityBool(scope, Entity::isVisuallyCrawling);
    }

    @MolangFunction(value = "is_damage_by", description = "判断生物是否被指定的伤害类型所伤害")
    public static float isDamageBy(MolangScope scope, Object... damageTypes) {
        return livingBool(scope, e -> {
            if (e.getLastDamageSource() != null) {
                return e.level().registryAccess()
                        .registry(Registries.DAMAGE_TYPE)
                        .map(r -> r.getKey(e.getLastDamageSource().type()))
                        .map(rl -> Arrays.stream(damageTypes).anyMatch(t -> rl.toString().equals(t.toString())))
                        .orElse(false);
            }

            return false;
        });
    }

    @MolangFunction(value = "is_stalking", description = "判断生物是否正在追踪其他实体")
    public static float isStalking(MolangScope scope) {
        return scope.getOwner().ownerAs(Mob.class)
                .map(m->m.isAggressive()?TRUE:FALSE)
                .orElse(0F);
    }

    @MolangFunction(value = "attack_time", description = "攻击时间")
    public static float attackTime(MolangScope scope) {
        return livingFloat(scope, l -> l.getAttackAnim(partialTick(scope)));
    }

    @MolangFunction(value = "is_attacking", description = "是否正在攻击")
    public static float isAttacking(MolangScope scope) {
        return attackTime(scope) > 0 ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_powered", description = "正在充能（类似苦力怕或凋灵）")
    public static float isPowered(MolangScope scope) {
        return scope.getOwner().ownerAs(PowerableMob.class).map(c -> c.isPowered() ? TRUE : FALSE).orElse(FALSE);
    }

    @MolangFunction(value = "health", description = "生物血量")
    public static float health(MolangScope scope) {
        return livingFloat(scope, LivingEntity::getHealth);
    }

    @MolangFunction(value = "max_health", description = "生物最大血量")
    public static float maxHealth(MolangScope scope) {
        return livingFloat(scope, LivingEntity::getMaxHealth);
    }

    @MolangFunction(value = "is_on_fire", description = "正在着火")
    public static float isOnFire(MolangScope scope) {
        return livingBool(scope, LivingEntity::isOnFire);
    }

    @MolangFunction(value = "ground_speed", description = "地面速度")
    public static float groundSpeed(MolangScope scope) {
        return livingFloat(scope, livingEntity -> {
            Vec3 velocity = livingEntity.getDeltaMovement();
            return Mth.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z))) * 20;
        });
    }

    @MolangFunction(value = "vertical_speed", description = "垂直速度")
    public static float verticalSpeed(MolangScope scope) {
        return livingFloat(scope, living -> living.onGround() ? 0 : (float) (living.getDeltaMovement().y * 20));
    }

    @MolangFunction(value = "head_yaw", description = "头部的 yaw 旋转角度")
    public static float headYaw(MolangScope scope) {
        return livingFloat(scope, e -> Mth.lerp(partialTick(scope), e.yHeadRotO, e.yHeadRot));
    }

    @MolangFunction(value = "head_yaw_speed", description = "头部 yaw 旋转速度")
    public static float headYawSpeed(MolangScope scope) {
        return livingFloat(scope, e -> (e.getYHeadRot() - e.yHeadRotO) / 20);
    }

    @MolangFunction(value = "body_yaw", description = "身体 yaw 旋转角度")
    public static float bodyYaw(MolangScope scope) {
        return livingFloat(scope, e -> Mth.lerp(partialTick(scope), e.yBodyRotO, e.yBodyRot));
    }

    @MolangFunction(value = "body_yaw_speed", description = "身体 yaw 旋转角度")
    public static float bodyYawSpeed(MolangScope scope) {
        return livingFloat(scope, e -> (e.yBodyRot - e.yBodyRotO) / 20);
    }

    @MolangFunction(value = "head_yaw_offset", description = "头部 yaw 偏移")
    public static float headYawOffset(MolangScope scope) {
        return livingFloat(scope, living -> {
            float hRot = headYaw(scope);
            float bRot = bodyYaw(scope);
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

    @MolangFunction(value = "head_pitch_offset", description = "头部 pitch 偏移")
    public static float headPitchOffset(MolangScope scope) {
        return livingFloat(scope, living -> -Mth.lerp(partialTick(scope), living.xRotO, living.getXRot()));
    }

    @MolangFunction(value = "baby", alias = "is_baby", description = "幼年体")
    public static float baby(MolangScope scope) {
        return livingBool(scope, LivingEntity::isBaby);
    }

    @FunctionalInterface
    interface ToBooleanFunction<K> {
        boolean apply(K key);
    }

    private static float entityBool(MolangScope scope, ToBooleanFunction<Entity> function) {
        return scope.getOwner().ownerAs(Entity.class).map(l -> function.apply(l) ? TRUE : FALSE).orElse(0F);
    }

    private static float entityFloat(MolangScope scope, Function<Entity, Float> function) {
        return scope.getOwner().ownerAs(Entity.class).map(function).orElse(0F);
    }

    private static float livingBool(MolangScope scope, ToBooleanFunction<LivingEntity> function) {
        return scope.getOwner().ownerAs(LivingEntity.class)
                .map(l -> function.apply(l) ? TRUE : FALSE)
                .orElse(0F);
    }

    private static float livingFloat(MolangScope scope, Function<LivingEntity, Float> function) {
        return scope.getOwner().ownerAs(LivingEntity.class)
                .map(function)
                .orElse(0F);
    }

    private static float handIs(MolangScope scope, InteractionHand hand, Object... objects) {
        return scope.getOwner().ownerAs(LivingEntity.class).map(living -> {
            ItemStack handItem = hand == InteractionHand.MAIN_HAND
                    ? living.getMainHandItem()
                    : living.getOffhandItem();

            for (Object object : objects) {
                if (Objects.equals(
                        BuiltInRegistries.ITEM.getKey(handItem.getItem()),
                        ResourceLocation.parse(object.toString()))) {
                    return TRUE;
                }
            }

            return FALSE;
        }).orElse(FALSE);
    }

    private static float slotGetter(MolangScope scope, EquipmentSlot slot, Object... objects) {
        return scope.getOwner().ownerAs(LivingEntity.class).map(livingEntity -> {
            for (Object object : objects) {
                if (ResourceLocation.parse(object.toString())
                        .equals(BuiltInRegistries.ITEM.getKey(livingEntity.getItemBySlot(slot).getItem()))) {
                    return TRUE;
                }
            }

            return FALSE;
        }).orElse(FALSE);
    }
}
