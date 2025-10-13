package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.capability.ExtraEntityUpdateData;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.common.behavior.component.MarkVariant;
import io.github.tt432.eyelib.common.behavior.component.Variant;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    @MolangFunction(value = "is_moving", description = "正在移动")
    public static float isMoving(MolangScope scope) {
        return entityBool(scope, entity -> entity.xo != entity.getX() || entity.yo != entity.getY() || entity.zo != entity.getZ());
    }

    @MolangFunction(value = "has_target", description = "拥有目标")
    public static float hasTarget(MolangScope scope) {
        return scope.getOwner().ownerAs(Targeting.class).map(m -> m.getTarget() != null ? TRUE : FALSE).orElse(FALSE);
    }

    @MolangFunction(value = "target_x_rotation", description = "指向当前目标所需的 X 轴角度")
    public static float targetXRotation(MolangScope scope) {
        return scope.getOwner().onHiveOwners(Entity.class, Targeting.class, (entity, targeting) -> {
            if (targeting.getTarget() != null) {
                Vec3 targetPos = targeting.getTarget().position();
                var x = targetPos.x - entity.getX();
                var y = targetPos.y - entity.getY();
                var z = targetPos.z - entity.getZ();
                return (float) Math.atan2(y, Math.sqrt(x * x + z * z));
            }

            return pitch(scope);
        }).orElse(FALSE);
    }

    @MolangFunction(value = "target_y_rotation", description = "指向当前目标所需的 Y 轴角度")
    public static float targetYRotation(MolangScope scope) {
        return xHeadYaw(scope, entity -> {
            if (entity instanceof Targeting targeting && targeting.getTarget() != null) {
                Vec3 targetPos = targeting.getTarget().position();

                // 计算方向向量
                double dx = targetPos.x - entity.getX();
                double dz = targetPos.z - entity.getZ();

                // 计算 Y 轴旋转角度 (水平旋转)
                return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f; // 转换为角度并调整基准
            }

            return headYaw(scope) + Mth.rotLerp(partialTicks(scope), entity.yBodyRotO, entity.yBodyRot);
        });
    }

    @MolangFunction(value = "is_charging", description = "充能")
    public static float isCharging(MolangScope scope) {
        return scope.getOwner().ownerAs(Vex.class).map(v -> v.isCharging() ? TRUE : FALSE)
                .orElse(livingBool(scope, LivingEntity::isUsingItem));
    }

    private static ItemStack getItemBySlot(LivingEntity entity, String slotName, int slotId) {
        return switch (slotName) {
            case "slot.weapon.mainhand" -> entity.getMainHandItem();
            case "slot.weapon.offhand" -> entity.getOffhandItem();
            case "slot.armor.head" -> entity.getItemBySlot(EquipmentSlot.HEAD);
            case "slot.armor.chest" -> entity.getItemBySlot(EquipmentSlot.CHEST);
            case "slot.armor.legs" -> entity.getItemBySlot(EquipmentSlot.LEGS);
            case "slot.armor.feet" -> entity.getItemBySlot(EquipmentSlot.FEET);
            case "slot.hotbar" ->
                    entity instanceof Player p ? p.inventoryMenu.getSlot(slotId + 36).getItem() : ItemStack.EMPTY;
            case "slot.inventory" ->
                    entity instanceof Player p ? p.inventoryMenu.getSlot(slotId).getItem() : ItemStack.EMPTY;
            case "slot.enderchest" ->
                    entity instanceof Player p ? p.getEnderChestInventory().getItem(slotId) : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    @MolangFunction(value = "is_item_name_any", description = "在手上的物品")
    public static float isItemNameAny(MolangScope scope, Object hand, Object... items) {
        return isItemNameAny(scope, hand, 0, items);
    }

    public static float isItemNameAny(MolangScope scope, Object hand, float index, Object... items) {
        return livingBool(scope, l -> {
            var itemKey = BuiltInRegistries.ITEM.getKey(getItemBySlot(l, hand.toString(), (int) index).getItem());

            for (Object item : items) {
                if (new ResourceLocation(item.toString()).equals(itemKey)) {
                    return true;
                }
            }

            return false;
        });
    }

    @MolangFunction(value = "is_gliding", description = "滑翔")
    public static float isGliding(MolangScope scope) {
        return livingBool(scope, le -> le.getFallFlyingTicks() > 0);
    }

    @MolangFunction(value = "is_sleeping", description = "睡觉")
    public static float isSleeping(MolangScope scope) {
        return livingBool(scope, LivingEntity::isSleeping);
    }

    @MolangFunction(value = "item_is_charged", description = "物品正在充能")
    public static float itemIsCharged(MolangScope scope) {
        return itemIsCharged(scope, "0");
    }

    public static float itemIsCharged(MolangScope scope, Object hand) {
        return livingBool(scope, living -> living.getUsedItemHand() == (switch (hand.toString()) {
            case "main_hand", "0" -> InteractionHand.MAIN_HAND;
            case "off_hand", "1" -> InteractionHand.OFF_HAND;
            default -> InteractionHand.MAIN_HAND;
        }));
    }

    @MolangFunction(value = "main_hand_item_use_duration", description = "主手物品使用时间")
    public static float mainHandItemUseDuration(MolangScope scope) {
        return livingFloat(scope, living -> (float) living.getTicksUsingItem() / living.getUseItem().getUseDuration());
    }

    @MolangFunction(value = "is_tamed", description = "驯服了")
    public static float isTamed(MolangScope scope) {
        return scope.getOwner().ownerAs(TamableAnimal.class).map(TamableAnimal::isTame).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "get_name", description = "获取名称")
    public static String getName(MolangScope scope) {
        return scope.getOwner().ownerAs(Entity.class).map(Entity::getName).orElse(Component.empty()).getString();
    }

    @MolangFunction(value = "item_slot_to_bone_name", description = "从 item slot 获取 bone 的名称")
    public static String itemSlotToBoneName(MolangScope scope, Object slot) {
        return switch (slot.toString()) {
            case "main_hand" -> "rightitem";
            case "off_hand" -> "leftitem";
            default -> slot.toString();
        };
    }

    @MolangFunction(value = "variant", description = "变体")
    public static float variant(MolangScope scope) {
        return livingFloat(scope, l -> {
            Variant component = DataAttachmentHelper.getOrCreate(EyelibAttachableData.ENTITY_BEHAVIOR_DATA.get(), l).component(Variant.class);
            return component != null ? (float) component.value() : 0;
        });
    }

    @MolangFunction(value = "mark_variant", description = "变体")
    public static float markVariant(MolangScope scope) {
        return livingFloat(scope, l -> {
            MarkVariant component = DataAttachmentHelper.getOrCreate(EyelibAttachableData.ENTITY_BEHAVIOR_DATA.get(), l).component(MarkVariant.class);
            return component != null ? (float) component.value() : 0;
        });
    }

    @MolangFunction(value = "damage_x", description = "受伤来源方向 x")
    public static float damageX(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), living).lastHurtX());
    }

    @MolangFunction(value = "damage_y", description = "受伤来源方向 x")
    public static float damageY(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), living).lastHurtY());
    }

    @MolangFunction(value = "damage_z", description = "受伤来源方向 x")
    public static float damageZ(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), living).lastHurtZ());
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

    public static float isItemEquipped(MolangScope scope) {
        return ((int) isItemEquipped(scope, "0")) | ((int) isItemEquipped(scope, "1"));
    }

    @MolangFunction(value = "actor_count", description = "世界中实体的数量")
    public static float actorCount(MolangScope scope) {
        if (Minecraft.getInstance().level != null)
            return Minecraft.getInstance().level.getEntityCount();
        return 0;
    }

    @MolangFunction(value = "delta_time", description = "距离上一帧的秒数")
    public static float deltaTime(MolangScope scope) {
        return scope.getOwner().ownerAs(BrAnimationEntry.Data.class).map(data -> data.deltaTime).orElse(0F);
    }

    @MolangFunction(value = "anim_time", alias = "life_time", description = "动画播放秒数")
    public static float animTime(MolangScope scope) {
        return scope.getOwner().ownerAs(BrAnimationEntry.Data.class).map(data -> data.animTime).orElse(0F);
    }

    @MolangFunction(value = "is_sniffing", description = "正在嗅")
    public static float isSniffing(MolangScope scope) {
        return scope.getOwner().ownerAs(LivingEntity.class).map(living -> living.getBrain().isActive(Activity.SNIFF)).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_roaring", description = "正在咆哮")
    public static float isRoaring(MolangScope scope) {
        return scope.getOwner().ownerAs(LivingEntity.class).map(living -> living.getBrain().isActive(Activity.ROAR)).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_sonic_boom", description = "唢呐爆炸")
    public static float isSonicBoom(MolangScope scope) {
        return scope.getOwner().ownerAs(Warden.class).map(w -> w.getBrain().isActive(Activity.FIGHT)).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_emerging", description = "正在出生")
    public static float isEmerging(MolangScope scope) {
        return scope.getOwner().ownerAs(Warden.class).map(w -> w.hasPose(Pose.EMERGING)).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "trade_tier", description = "村民交易等级")
    public static float tradeTier(MolangScope scope) {
        return scope.getOwner().ownerAs(Villager.class).map(v -> v.getVillagerData().getLevel()).orElse(0);
    }

    @MolangFunction(value = "dash_cooldown_progress", description = "冲刺冷却")
    public static float dashCooldownProgress(MolangScope scope) {
        return scope.getOwner().ownerAs(Camel.class).map(c -> c.getJumpCooldown() / 55F).orElse(0F);
    }

    @MolangFunction(value = "is_saddled", description = "正在背负")
    public static float isSaddled(MolangScope scope) {
        return scope.getOwner().ownerAs(AbstractHorse.class).map(AbstractHorse::isSaddled).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_sheared", description = "被剪了")
    public static float isSheared(MolangScope scope) {
        return scope.getOwner().ownerAs(Sheep.class).map(Sheep::isSheared).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_angry", description = "生气")
    public static float isAngry(MolangScope scope) {
        return scope.getOwner().ownerAs(NeutralMob.class).map(NeutralMob::isAngry).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_carrying_block", description = "抱方块")
    public static float isCarryingBlock(MolangScope scope) {
        return scope.getOwner().ownerAs(EnderMan.class).map(e -> e.getCarriedBlock() != null).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_chested", description = "")
    public static float isChested(MolangScope scope) {
        return scope.getOwner().ownerAs(AbstractChestedHorse.class).map(AbstractChestedHorse::hasChest).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "facing_target_to_range_attack", description = "正在进行远程攻击")
    public static float facingTargetToRageAttack(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), e).facing_target_to_range_attack());
    }

    @MolangFunction(value = "is_avoiding_mobs", description = "正在从怪物逃离")
    public static float isAvoidingMobs(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), e).is_avoiding_mobs());
    }

    @MolangFunction(value = "is_grazing", description = "正在吃草")
    public static float isGrazing(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), e).is_grazing());
    }

    @MolangFunction(value = "time_since_last_vibration_detection", description = "自最后一次检测到声波的时间（监守者）")
    public static float timeSinceLastVibrationDetection(MolangScope scope) {
        return scope.getOwner().ownerAs(Warden.class).map(w -> (40L - w.getBrain().getTimeUntilExpiry(MemoryModuleType.VIBRATION_COOLDOWN)) / 20F).orElse(0F);
    }

    @MolangFunction(value = "heartbeat_phase", description = "心跳（监守者）")
    public static float heartbeatPhase(MolangScope scope) {
        return scope.getOwner().ownerAs(Warden.class).map(w -> w.getHeartAnimation(partialTicks(scope))).orElse(0F);
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
    public static float partialTicks(MolangScope scope) {
        if (scope.contains("variable.partial_tick")) return scope.get("variable.partial_tick").asFloat();
        return Minecraft.getInstance().timer.partialTick;
    }

    @MolangFunction(value = "any_animation_finished", description = "任意动画播放完毕（动画控制器）")
    public static float anyAnimationFinished(MolangScope scope) {
        return scope.getOwner().onHiveOwners(BrAnimationController.class, BrAnimationController.Data.class,
                BrAnimationController::anyAnimationFinished).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "all_animations_finished", description = "所有动画播放完毕（动画控制器）")
    public static float allAnimationsFinished(MolangScope scope) {
        return scope.getOwner().onHiveOwners(BrAnimationController.class, BrAnimationController.Data.class,
                BrAnimationController::allAnimationFinished).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_invisible", description = "不可见")
    public static float isInvisible(MolangScope scope) {
        return entityBool(scope, Entity::isInvisible);
    }

    @MolangFunction(value = "is_alive", description = "判断实体是否存活")
    public static float isAlive(MolangScope scope) {
        return livingBool(scope, LivingEntity::isAlive);
    }

    @MolangFunction(value = "distance_from_camera", description = "距离摄像头的距离")
    public static float distanceFromCamera(MolangScope scope) {
        return entityFloat(scope, e -> {
            if (Minecraft.getInstance().cameraEntity == null) return 0F;

            return Minecraft.getInstance().cameraEntity.distanceTo(e);
        });
    }

    @MolangFunction(value = "modified_distance_moved", description = "移动过的距离")
    public static float modifiedDistanceMoved(MolangScope scope) {
        return scope.getOwner().ownerAs(Entity.class).map(e -> DataAttachmentHelper.getOrCreate(EyelibAttachableData.ENTITY_STATISTICS.get(), e).distanceWalked()).orElse(0F);
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
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), e).is_dig());
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
        return entityFloat(scope, e -> e.getViewYRot(0));
    }

    @MolangFunction(value = "yaw_speed", description = "yaw 轴旋转速度")
    public static float yawSpeed(MolangScope scope) {
        return entityFloat(scope, e -> yaw(scope) - e.getViewYRot(partialTicks(scope) - 0.1F));
    }

    @MolangFunction(value = "eye_target_x_rotation", alias = "pitch", description = "pitch 角度（x rot）")
    public static float pitch(MolangScope scope) {
        return entityFloat(scope, e -> e.getViewXRot(0));
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
        return livingFloat(scope, e -> e.onClimbable()
                ? e.getLastClimbablePos()
                .map(p -> {
                    VoxelShape shape = e.level().getBlockState(p).getShape(e.level(), p);
                    return shape.isEmpty() ? 0F : new Vec3(p.getX(), p.getY(), p.getZ())
                            .add(shape.bounds().getCenter())
                            .subtract(e.position())
                            .x;
                })
                .orElse(0D)
                .floatValue()
                : 0F);
    }

    @MolangFunction(value = "climbing_y", description = "实体位置指向可攀爬方块中心的向量的 y 分量")
    public static float climbingY(MolangScope scope) {
        return livingFloat(scope, e -> e.onClimbable()
                ? e.getLastClimbablePos()
                .map(p -> {
                    VoxelShape shape = e.level().getBlockState(p).getShape(e.level(), p);
                    return shape.isEmpty() ? 0F : new Vec3(p.getX(), p.getY(), p.getZ())
                            .add(shape.bounds().getCenter())
                            .subtract(e.position())
                            .y;
                })
                .orElse(0D)
                .floatValue()
                : 0F);
    }

    @MolangFunction(value = "climbing_z", description = "实体位置指向可攀爬方块中心的向量的 z 分量")
    public static float climbingZ(MolangScope scope) {
        return livingFloat(scope, e -> e.onClimbable()
                ? e.getLastClimbablePos()
                .map(p -> {
                    VoxelShape shape = e.level().getBlockState(p).getShape(e.level(), p);
                    return shape.isEmpty() ? 0F : new Vec3(p.getX(), p.getY(), p.getZ())
                            .add(shape.bounds().getCenter())
                            .subtract(e.position())
                            .z;
                })
                .orElse(0D)
                .floatValue()
                : 0F);
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
                .map(m -> m.isAggressive() ? TRUE : FALSE)
                .orElse(0F);
    }

    @MolangFunction(value = "attack_time", description = "攻击时间")
    public static float attackTime(MolangScope scope) {
        return livingFloat(scope, l -> l.getAttackAnim(partialTicks(scope)));
    }

    @MolangFunction(value = "is_attacking", description = "是否正在攻击")
    public static float isAttacking(MolangScope scope) {
        return attackTime(scope) > 0 ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_powered", description = "正在充能（类似苦力怕或凋灵）")
    public static float isPowered(MolangScope scope) {
        return scope.getOwner().ownerAs(PowerableMob.class).map(c -> c.isPowered() ? TRUE : FALSE).orElse(FALSE);
    }

    @MolangFunction(value = "creeper_swell", alias = "swell_amount", description = "苦力怕爆炸计时")
    public static float creeperSwell(MolangScope scope) {
        return scope.getOwner().ownerAs(Creeper.class).map(c -> c.swell / 30F)
                .orElse(Float.valueOf(scope.getOwner().ownerAs(WitherBoss.class).map(WitherBoss::getInvulnerableTicks).orElse(0)));
    }

    @MolangFunction(value = "swelling_dir", description = "苦力怕爆炸方向")
    public static float swellingDir(MolangScope scope) {
        return scope.getOwner().ownerAs(Creeper.class).map(Creeper::getSwellDir).orElse(0);
    }

    @MolangFunction(value = "invulnerable_ticks", description = "")
    public static float invulnerableTicks(MolangScope scope) {
        return scope.getOwner().ownerAs(WitherBoss.class).map(WitherBoss::getInvulnerableTicks).orElse(0);
    }

    @MolangFunction(value = "is_avoid", description = "正在逃离(比如苦力怕逃离猫)")
    public static float isAvoid(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), e).is_avoid());
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
        return livingFloat(scope, e -> {
            var xo = e.xo;
            var zo = e.zo;

            var x = e.getX();
            var z = e.getZ();

            return 20 * (float) Math.sqrt((x - xo) * (x - xo) + (z - zo) * (z - zo));
        });
    }

    @MolangFunction(value = "modified_move_speed", description = "移动速度")
    public static float modifiedMoveSpeed(MolangScope scope) {
        return livingFloat(scope, e -> {
            var xo = e.xo;
            var zo = e.zo;

            var x = e.getX();
            var z = e.getZ();

            return ((float) Math.sqrt((x - xo) * (x - xo) + (z - zo) * (z - zo)))
                    / ((float) e.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) *
                    /* 疾跑乘数，原版没有，只能硬编码 */1.3F);
        });
    }

    @MolangFunction(value = "is_damage", description = "正在受伤")
    public static float isDamage(MolangScope scope) {
        return livingBool(scope, living -> living.hurtTime > 0);
    }

    @MolangFunction(value = "hurt_time", description = "受伤时间")
    public static float hurtTime(MolangScope scope) {
        return livingFloat(scope, living -> (float) living.hurtTime / 20);
    }

    @MolangFunction(value = "is_riding", alias = "has_rider", description = "正在骑乘")
    public static float isRiding(MolangScope scope) {
        return livingBool(scope, e -> e.getVehicle() != null);
    }

    @MolangFunction(value = "vertical_speed", description = "垂直速度")
    public static float verticalSpeed(MolangScope scope) {
        return livingFloat(scope, living -> living.onGround() ? 0 : (float) (living.position().y - living.yo) * 20);
    }

    @MolangFunction(value = "head_yaw", description = "头部的 yaw 旋转角度")
    public static float headYaw(MolangScope scope) {
        return xHeadYaw(scope, l -> Mth.rotLerp(partialTicks(scope), l.yHeadRotO, l.yHeadRot));
    }

    @MolangFunction(value = "head_x_rotation", description = "返回第 N 个头的旋转")
    public static float headXRotation(MolangScope scope, float head) {
        return scope.getOwner().ownerAs(WitherBoss.class).map(w -> w.getHeadXRot((int) head)).orElse(0F);
    }

    @MolangFunction(value = "head_y_rotation", description = "返回第 N 个头的旋转")
    public static float headYRotation(MolangScope scope, float headIndex) {
        return xHeadYaw(scope, living -> {
            if (living instanceof WitherBoss wither) {
                return wither.getHeadYRot((int) headIndex) - wither.yBodyRot;
            } else {
                return Mth.rotLerp(partialTicks(scope), living.yHeadRotO, living.yHeadRot);
            }
        });
    }

    private static float xHeadYaw(MolangScope scope, Function<LivingEntity, Float> function) {
        return livingFloat(scope, livingEntity -> {
            var partialTicks = partialTicks(scope);
            var lerpBodyRot = Mth.rotLerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            var lerpHeadRot = function.apply(livingEntity);
            float netHeadYaw = lerpHeadRot - lerpBodyRot;
            boolean shouldSit = livingEntity.isPassenger() && (livingEntity.getVehicle() != null && livingEntity.getVehicle().shouldRiderSit());
            if (shouldSit && livingEntity.getVehicle() instanceof LivingEntity vehicle) {
                lerpBodyRot = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
                netHeadYaw = lerpHeadRot - lerpBodyRot;
                float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
                lerpBodyRot = lerpHeadRot - clampedHeadYaw;
                if (clampedHeadYaw * clampedHeadYaw > 2500f) {
                    lerpBodyRot += clampedHeadYaw * 0.2f;
                }
                netHeadYaw = lerpHeadRot - lerpBodyRot;
            }
            return Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
        });
    }

    @MolangFunction(value = "head_yaw_speed", description = "头部 yaw 旋转速度")
    public static float headYawSpeed(MolangScope scope) {
        return livingFloat(scope, e -> (e.getYHeadRot() - e.yHeadRotO) / 20);
    }

    @MolangFunction(value = "is_standing", description = "站立")
    public static float isStanding(MolangScope scope) {
        return scope.getOwner().ownerAs(AbstractHorse.class).map(AbstractHorse::isStanding).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "shake_angle", description = "头摇晃角度（狼）")
    public static float shakeAngle(MolangScope scope) {
        return scope.getOwner().ownerAs(Wolf.class).map(w -> w.getHeadRollAngle(partialTicks(scope))).orElse(0F);
    }

    @MolangFunction(value = "body_yaw", description = "身体 yaw 旋转角度")
    public static float bodyYaw(MolangScope scope) {
        return scope.getOwner().ownerAs(LivingEntity.class)
                .map(living -> Mth.wrapDegrees(Mth.rotLerp(partialTicks(scope),
                        living.yBodyRotO, living.yBodyRot)))
                .orElse(scope.getOwner().ownerAs(Entity.class)
                        .map(e -> Mth.wrapDegrees(e.getYRot()))
                        .orElse(0F));
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
        return livingFloat(scope, living -> -Mth.lerp(partialTicks(scope), living.xRotO, living.getXRot()));
    }

    @MolangFunction(value = "body_y_rotation", description = "身体 pitch 偏移")
    public static float bodyXRotation(MolangScope scope) {
        return livingFloat(scope, living -> Mth.lerp(partialTicks(scope), living.yBodyRotO, living.yBodyRot));
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
                        ResourceLocations.of(object.toString()))) {
                    return TRUE;
                }
            }

            return FALSE;
        }).orElse(FALSE);
    }

    private static float slotGetter(MolangScope scope, EquipmentSlot slot, Object... objects) {
        return scope.getOwner().ownerAs(LivingEntity.class).map(livingEntity -> {
            for (Object object : objects) {
                if (ResourceLocations.of(object.toString())
                        .equals(BuiltInRegistries.ITEM.getKey(livingEntity.getItemBySlot(slot).getItem()))) {
                    return TRUE;
                }
            }

            return FALSE;
        }).orElse(FALSE);
    }
}
