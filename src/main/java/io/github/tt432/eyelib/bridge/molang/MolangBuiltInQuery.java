package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangQueryRuntimeBridge;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
//? if <26.1
import net.minecraft.world.entity.PowerableMob;
//? if <26.1 {
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//?} else {
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
//?}
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
//? if <26.1 {
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.monster.SpellcasterIllager;
//?} else {
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
//?}
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
//? if <26.1 {
import net.minecraft.world.entity.npc.Villager;
//?} else {
import net.minecraft.world.entity.npc.villager.Villager;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
import net.minecraft.world.item.UseAnim;
//?} else {
import net.minecraft.world.item.ItemUseAnimation;
//?}
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.github.tt432.eyelib.bridge.molang.adapter.ComponentStore;
import io.github.tt432.eyelib.bridge.molang.adapter.MolangEntityContext;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * Molang 平台映射（内置查询函数）。
 *
 * @author TT432
 */
@MolangMapping(value = "query", pureFunction = false)
@SuppressWarnings("unused")
public interface MolangBuiltInQuery {

    // MC 实体子类 HostRole（未集中在 HostRoles 中，按需创建）
    HostRole<Entity> ENTITY = HostRole.of("Entity", Entity.class);
    HostRole<LivingEntity> LIVING_ENTITY = HostRole.of("LivingEntity", LivingEntity.class);
    HostRole<Mob> MOB = HostRole.of("Mob", Mob.class);
    HostRole<Targeting> TARGETING = HostRole.of("Targeting", Targeting.class);
    HostRole<Vex> VEX = HostRole.of("Vex", Vex.class);
    HostRole<TamableAnimal> TAMABLE_ANIMAL = HostRole.of("TamableAnimal", TamableAnimal.class);
    HostRole<Warden> WARDEN = HostRole.of("Warden", Warden.class);
    HostRole<Villager> VILLAGER = HostRole.of("Villager", Villager.class);
    HostRole<Camel> CAMEL = HostRole.of("Camel", Camel.class);
    HostRole<AbstractHorse> ABSTRACT_HORSE = HostRole.of("AbstractHorse", AbstractHorse.class);
    HostRole<AbstractChestedHorse> ABSTRACT_CHESTED_HORSE = HostRole.of("AbstractChestedHorse", AbstractChestedHorse.class);
    HostRole<Sheep> SHEEP = HostRole.of("Sheep", Sheep.class);
    HostRole<NeutralMob> NEUTRAL_MOB = HostRole.of("NeutralMob", NeutralMob.class);
    HostRole<EnderMan> ENDER_MAN = HostRole.of("EnderMan", EnderMan.class);
    HostRole<Creeper> CREEPER = HostRole.of("Creeper", Creeper.class);
    //? if <26.1
    HostRole<PowerableMob> POWERABLE_MOB = HostRole.of("PowerableMob", PowerableMob.class);
    HostRole<WitherBoss> WITHER_BOSS = HostRole.of("WitherBoss", WitherBoss.class);
    HostRole<Wolf> WOLF = HostRole.of("Wolf", Wolf.class);
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

    @MolangFunction(value = "any", description = "对第一个参数求值后，若后续任意参数与第一个参数值相等则返回 1.0。至少需要 3 个参数")
    public static float any(MolangScope scope, float value, float... candidates) {
        for (float candidate : candidates) {
            if (Float.compare(value, candidate) == 0) {
                return TRUE;
            }
        }
        return FALSE;
    }

    @MolangFunction(value = "in_range", description = "若第一个参数在最小值和最大值（含）之间则返回 1.0")
    public static float inRange(MolangScope scope, float value, float min, float max) {
        return value >= min && value <= max ? TRUE : FALSE;
    }

    @MolangFunction(value = "position", description = "返回实体的绝对位置。参数为轴索引 (0=x, 1=y, 2=z)")
    public static float position(MolangScope scope, float axis) {
        return switch ((int) axis) {
            case 0 -> posX(scope);
            case 1 -> posY(scope);
            case 2 -> posZ(scope);
            default -> 0F;
        };
    }

    @MolangFunction(value = "camera_distance_range_lerp", description = "根据相机距离在两个距离范围之间插值返回 0~1")
    public static float cameraDistanceRangeLerp(MolangScope scope, float d1, float d2) {
        float dist = distanceFromCamera(scope);
        float min = Math.min(d1, d2);
        float max = Math.max(d1, d2);
        if (dist <= min) return 0F;
        if (dist >= max) return TRUE;
        return (dist - min) / (max - min);
    }

    @MolangFunction(value = "has_target", description = "拥有目标")
    public static float hasTarget(MolangScope scope) {
        return scope.getHostContext().get(TARGETING).map(m -> m.getTarget() != null ? TRUE : FALSE).orElse(FALSE);
    }

    @MolangFunction(value = "target_x_rotation", description = "指向当前目标所需的 X 轴角度")
    public static float targetXRotation(MolangScope scope) {
        return scope.getHostContext().get(ENTITY)
                    .flatMap(entity -> scope.getHostContext().get(TARGETING)
                                            .map(targeting -> {
                                                if (targeting.getTarget() != null) {
                                                    Vec3 targetPos = targeting.getTarget().position();
                                                    var x = targetPos.x - entity.getX();
                                                    var y = targetPos.y - entity.getY();
                                                    var z = targetPos.z - entity.getZ();
                                                    return (float) Math.atan2(y, Math.sqrt(x * x + z * z));
                                                }

                                                return pitch(scope);
                                            })
                    ).orElse(FALSE);
    }

    @MolangFunction(value = "target_y_rotation", description = "指向当前目标所需的 Y 轴角度")
    public static float targetYRotation(MolangScope scope) {
        return xHeadYaw(scope, entity -> {
            if (entity instanceof Targeting targeting && targeting.getTarget() != null) {
                Vec3 targetPos = targeting.getTarget().position();

                double dx = targetPos.x - entity.getX();
                double dz = targetPos.z - entity.getZ();

                return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            }

            return headYaw(scope) + Mth.rotLerp(partialTicks(scope), entity.yBodyRotO, entity.yBodyRot);
        });
    }

    @MolangFunction(value = "is_charging", description = "充能")
    public static float isCharging(MolangScope scope) {
        return portBool(scope, "is_charging")
                || scope.getHostContext().get(VEX).map(Vex::isCharging).orElse(false)
                ? TRUE : FALSE;
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
    public static float isItemNameAny(MolangScope scope, String hand, String... items) {
        return isItemNameAny(scope, hand, 0, items);
    }

    public static float isItemNameAny(MolangScope scope, String hand, float index, String... items) {
        return livingBool(scope, l -> {
            var itemKey = BuiltInRegistries.ITEM.getKey(getItemBySlot(l, hand.toString(), (int) index).getItem());

            for (Object item : items) {
                //? if <1.20.6 {
                if (new ResourceLocation(item.toString()).equals(itemKey)) {
                //?} elif <26.1 {
                if (ResourceLocation.parse(item.toString()).equals(itemKey)) {
                //?} else {
                if (Identifier.parse(item.toString()).equals(itemKey)) {

                //?}
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

    public static float itemIsCharged(MolangScope scope, String hand) {
        return livingBool(scope, living -> living.getUsedItemHand() == (switch (hand.toString()) {
            case "main_hand", "0" -> InteractionHand.MAIN_HAND;
            case "off_hand", "1" -> InteractionHand.OFF_HAND;
            default -> InteractionHand.MAIN_HAND;
        }));
    }

    @MolangFunction(value = "main_hand_item_use_duration", description = "主手物品使用时间")
    public static float mainHandItemUseDuration(MolangScope scope) {
        return livingFloat(scope, living -> {
            //? if <1.20.6 {
            int useDuration = living.getUseItem().getUseDuration();
            //?} elif <26.1 {
            int useDuration = living.getUseItem().getUseDuration(living);
            //?} else {
            int useDuration = 1;
            //?}
            // 未在使用物品时 useDuration=0，0/0 会产生 NaN；BE 语义下未使用即返回 0
            if (useDuration <= 0) {
                return 0F;
            }
            return (float) living.getTicksUsingItem() / useDuration;
        });
    }

    @MolangFunction(value = "is_tamed", description = "驯服了")
    public static float isTamed(MolangScope scope) {
        return portBool(scope, "is_tamed")
                || scope.getHostContext().get(TAMABLE_ANIMAL).map(TamableAnimal::isTame).orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "get_name", description = "获取名称")
    public static String getName(MolangScope scope) {
        return scope.getHostContext().get(ENTITY).map(Entity::getName).orElse(Component.empty()).getString();
    }

    @MolangFunction(value = "is_name_any", description = "实体名称是否与给定名称之一精确相等（区分大小写）")
    public static float isNameAny(MolangScope scope, String... names) {
        return scope.getHostContext().get(ENTITY).map(entity -> {
            Component customName = entity.getCustomName();
            String entityName = (customName != null ? customName : entity.getName()).getString();
            for (String name : names) {
                if (entityName.equals(name)) {
                    return TRUE;
                }
            }
            return FALSE;
        }).orElse(FALSE);
    }

    @MolangFunction(value = "item_slot_to_bone_name", description = "从 item slot 获取 bone 的名称")
    public static String itemSlotToBoneName(MolangScope scope, String slot) {
        return switch (slot.toString()) {
            case "main_hand" -> "rightitem";
            case "off_hand" -> "leftitem";
            default -> slot.toString();
        };
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
    public static float offHandIs(MolangScope scope, String... objects) {
        return slotGetter(scope, EquipmentSlot.OFFHAND, objects);
    }

    @MolangFunction(value = "main_hand_is", description = "主手是指定物品之一")
    public static float mainHandIs(MolangScope scope, String... objects) {
        return slotGetter(scope, EquipmentSlot.MAINHAND, objects);
    }

    @MolangFunction(value = "leggings_is", description = "护腿是指定物品之一")
    public static float leggingsIs(MolangScope scope, String... objects) {
        return slotGetter(scope, EquipmentSlot.LEGS, objects);
    }

    @MolangFunction(value = "helmet_is", description = "头盔是指定物品之一")
    public static float helmetIs(MolangScope scope, String... objects) {
        return slotGetter(scope, EquipmentSlot.HEAD, objects);
    }

    @MolangFunction(value = "chestplate_is", description = "胸甲是指定物品之一")
    public static float chestplateIs(MolangScope scope, String... objects) {
        return slotGetter(scope, EquipmentSlot.CHEST, objects);
    }

    @MolangFunction(value = "boots_is", description = "鞋子是指定物品之一")
    public static float bootsIs(MolangScope scope, String... objects) {
        return slotGetter(scope, EquipmentSlot.FEET, objects);
    }

    @MolangFunction(value = "is_item_equipped", description = "指定手上是否有物品 (有效参数 主手='main_hand' 或 0; 副手= 'off_hand' 或 1)")
    public static float isItemEquipped(MolangScope scope, String hand) {
        return scope.getHostContext().get(LIVING_ENTITY).map(living -> (switch (hand.toString()) {
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
        return MolangQueryRuntimeBridge.actorCount();
    }

    @MolangFunction(value = "is_sniffing", description = "正在嗅")
    public static float isSniffing(MolangScope scope) {
        return scope.getHostContext()
                    .get(LIVING_ENTITY)
                    .map(living -> living.getBrain().isActive(Activity.SNIFF))
                    .orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_roaring", description = "正在咆哮")
    public static float isRoaring(MolangScope scope) {
        return scope.getHostContext()
                    .get(LIVING_ENTITY)
                    .map(living -> living.getBrain().isActive(Activity.ROAR))
                    .orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_sonic_boom", description = "唢呐爆炸")
    public static float isSonicBoom(MolangScope scope) {
        return scope.getHostContext()
                    .get(WARDEN)
                    .map(w -> w.getBrain().isActive(Activity.FIGHT))
                    .orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_emerging", description = "正在出生")
    public static float isEmerging(MolangScope scope) {
        return scope.getHostContext().get(WARDEN).map(w -> w.hasPose(Pose.EMERGING)).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "trade_tier", description = "村民交易等级")
    public static float tradeTier(MolangScope scope) {
        //? if <26.1 {
        return scope.getHostContext().get(VILLAGER).map(v -> v.getVillagerData().getLevel()).orElse(0);
        //?} else {
        return 0;
        //?}
    }

    @MolangFunction(value = "dash_cooldown_progress", description = "冲刺冷却")
    public static float dashCooldownProgress(MolangScope scope) {
        return scope.getHostContext().get(CAMEL).map(c -> c.getJumpCooldown() / 55F).orElse(0F);
    }

    @MolangFunction(value = "is_saddled", description = "正在背负")
    public static float isSaddled(MolangScope scope) {
        return portBool(scope, "is_saddled")
                || scope.getHostContext().get(ABSTRACT_HORSE).map(AbstractHorse::isSaddled).orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_sheared", description = "被剪了")
    public static float isSheared(MolangScope scope) {
        return portBool(scope, "is_sheared")
                || scope.getHostContext().get(SHEEP).map(Sheep::isSheared).orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_angry", description = "生气")
    public static float isAngry(MolangScope scope) {
        return portBool(scope, "is_angry")
                || scope.getHostContext().get(NEUTRAL_MOB).map(NeutralMob::isAngry).orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_carrying_block", description = "抱方块")
    public static float isCarryingBlock(MolangScope scope) {
        return portBool(scope, "is_carrying_block")
                || scope.getHostContext().get(ENDER_MAN).map(e -> e.getCarriedBlock() != null).orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "is_chested", description = "")
    public static float isChested(MolangScope scope) {
        return portBool(scope, "is_chested")
                || scope.getHostContext()
                        .get(ABSTRACT_CHESTED_HORSE)
                        .map(AbstractChestedHorse::hasChest)
                        .orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "time_since_last_vibration_detection", description = "自最后一次检测到声波的时间（监守者）")
    public static float timeSinceLastVibrationDetection(MolangScope scope) {
        return portFloat(scope, "time_since_last_vibration_detection")
                .orElse(scope.getHostContext()
                             .get(WARDEN)
                             .map(w -> (40L - w.getBrain()
                                               .getTimeUntilExpiry(MemoryModuleType.VIBRATION_COOLDOWN)) / 20F)
                             .orElse(0F));
    }

    @MolangFunction(value = "heartbeat_phase", description = "心跳（监守者）")
    public static float heartbeatPhase(MolangScope scope) {
        return portFloat(scope, "heartbeat_phase")
                .orElse(scope.getHostContext()
                             .get(WARDEN)
                             .map(w -> w.getHeartAnimation(partialTicks(scope)))
                             .orElse(0F));
    }

    @MolangFunction(value = "time_of_day", description = "一天中的时间")
    public static float timeOfDay(MolangScope scope) {
        return MolangQueryRuntimeBridge.timeOfDay();
    }

    @MolangFunction(value = "moon_phase", description = "月相")
    public static float moonPhase(MolangScope scope) {
        return MolangQueryRuntimeBridge.moonPhase();
    }

    @MolangFunction(value = "partial_tick", description = "距离上一帧的时间")
    public static float partialTicks(MolangScope scope) {
        return MolangQueryRuntimeBridge.resolvePartialTick(scope);
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
        return entityFloat(scope, MolangQueryRuntimeBridge::distanceFromCamera);
    }

    @MolangFunction(value = "is_on_ground", description = "正处于地面上")
    public static float isOnGround(MolangScope scope) {
        return entityBool(scope, io.github.tt432.eyelib.bridge.molang.adapter.EntityPortAdapter::isOnGround);
    }

    @MolangFunction(value = "is_in_ui", description = "是否在 UI 中渲染（eyelib 目前只有世界内渲染路径，恒为 0）")
    public static float isInUi(MolangScope scope) {
        return FALSE;
    }

    io.github.tt432.eyelib.molang.mapping.api.HostRole<Boolean> ATTACHED =
            io.github.tt432.eyelib.molang.mapping.api.HostRole.of("is_attached", Boolean.class);

    @MolangFunction(value = "is_attached", description = "attachable 是否附着于实体渲染（由 AttachableItemRenderSetup 置位）")
    public static float isAttached(MolangScope scope) {
        return scope.getHostContext().get(ATTACHED).map(b -> b ? TRUE : FALSE).orElse(FALSE);
    }

    /** 供 AttachableItemRenderSetup 将 scope 标记为“附着渲染”。 */
    public static void markAttached(MolangScope scope) {
        scope.getHostContext().put(ATTACHED, Boolean.TRUE);
    }

    @MolangFunction(value = "fall_distance", description = "摔落的距离")
    public static float fallDistance(MolangScope scope) {
        //? if <26.1 {
        return entityFloat(scope, e -> e.fallDistance);
        //?} else {
        return entityFloat(scope, e -> (float) e.fallDistance);
        //?}
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

    @MolangFunction(value = "is_eating", description = "正在食用物品")
    public static float isEating(MolangScope scope) {
        //? if <26.1 {
        return livingBool(scope, p -> p.isUsingItem() && p.getUseItem().getUseAnimation() == UseAnim.EAT);
        //?} else {
        return livingBool(scope, p -> p.isUsingItem() && p.getUseItem().getUseAnimation() == ItemUseAnimation.EAT);
        //?}
    }

    @MolangFunction(value = "is_drinking", description = "正在饮用物品")
    public static float isDrinking(MolangScope scope) {
        //? if <26.1 {
        return livingBool(scope, p -> p.isUsingItem() && p.getUseItem().getUseAnimation() == UseAnim.DRINK);
        //?} else {
        return livingBool(scope, p -> p.isUsingItem() && p.getUseItem().getUseAnimation() == ItemUseAnimation.DRINK);
        //?}
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
        //? if <26.1 {
        return entityBool(scope, Entity::isInWaterRainOrBubble);
        //?} else {
        return entityBool(scope, Entity::isInWater);
        //?}
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
    public static float isDamageBy(MolangScope scope, String... damageTypes) {
        return livingBool(scope, e -> {
            net.minecraft.world.damagesource.DamageSource lastDamage = e.getLastDamageSource();
            if (lastDamage != null) {
                return e.level().registryAccess()
                        //? if <26.1 {
                        .registry(Registries.DAMAGE_TYPE)
                        //?} else {
                        .lookup(Registries.DAMAGE_TYPE)
                        //?}
                        .map(r -> r.getKey(lastDamage.type()))
                        .map(rl -> Arrays.stream(damageTypes).anyMatch(t -> rl.toString().equals(t.toString())))
                        .orElse(false);
            }

            return false;
        });
    }

    @MolangFunction(value = "is_stalking", description = "判断生物是否正在追踪其他实体")
    public static float isStalking(MolangScope scope) {
        return scope.getHostContext().get(MOB)
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
        return portBool(scope, "is_powered") || poweredMob(scope) ? TRUE : FALSE;
    }

    private static boolean poweredMob(MolangScope scope) {
        //? if <26.1 {
        return scope.getHostContext().get(POWERABLE_MOB).map(PowerableMob::isPowered).orElse(false);
        //?} else {
        return false;
        //?}
    }

    @MolangFunction(value = "swelling_dir", description = "苦力怕爆炸方向")
    public static float swellingDir(MolangScope scope) {
        return portFloat(scope, "swelling_dir")
                .orElse(scope.getHostContext().get(CREEPER).map(Creeper::getSwellDir).orElse(0).floatValue());
    }

    @MolangFunction(value = "invulnerable_ticks", description = "")
    public static float invulnerableTicks(MolangScope scope) {
        return portFloat(scope, "invulnerable_ticks")
                .orElse(scope.getHostContext()
                             .get(WITHER_BOSS)
                             .map(WitherBoss::getInvulnerableTicks)
                             .orElse(0)
                             .floatValue());
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

            float speed = (float) e.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.3F;
            // 属性移速为 0 时避免 0/0=NaN（NaN 会沿动画链扩散致整模型消失）
            if (speed <= 1.0E-6F) {
                return 0F;
            }
            return ((float) Math.sqrt((x - xo) * (x - xo) + (z - zo) * (z - zo))) / speed;
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
        //? if <26.1 {
        return portFloat(scope, "head_x_rotation")
                .orElse(scope.getHostContext().get(WITHER_BOSS).map(w -> w.getHeadXRot((int) head)).orElse(0F));
        //?} else {
        return 0F;
        //?}
    }

    @MolangFunction(value = "head_y_rotation", description = "返回第 N 个头的旋转")
    public static float headYRotation(MolangScope scope, float headIndex) {
        return xHeadYaw(scope, living -> {
            if (living instanceof WitherBoss wither) {
                //? if <26.1 {
                return wither.getHeadYRot((int) headIndex) - living.yBodyRot;
                //?} else {
                return 0F;
                //?}
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
            boolean shouldSit = livingEntity.isPassenger() && (livingEntity.getVehicle() != null && livingEntity.getVehicle()
                                                                                                                .shouldRiderSit());
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
        return portBool(scope, "is_standing")
                || scope.getHostContext().get(ABSTRACT_HORSE).map(AbstractHorse::isStanding).orElse(false)
                ? TRUE : FALSE;
    }

    @MolangFunction(value = "shake_angle", description = "头摇晃角度（狼）")
    public static float shakeAngle(MolangScope scope) {
        return portFloat(scope, "shake_angle")
                .orElse(scope.getHostContext()
                              .get(WOLF)
                              //? if <26.1 {
                              .map(w -> w.getHeadRollAngle(partialTicks(scope)))
                              //?} else {
                              .map(w -> 0F)
                              //?}
                              .orElse(0F));
    }

    @MolangFunction(value = "body_yaw", description = "身体 yaw 旋转角度")
    public static float bodyYaw(MolangScope scope) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(living -> Mth.wrapDegrees(Mth.rotLerp(partialTicks(scope),
                                                               living.yBodyRotO, living.yBodyRot)))
                    .orElse(scope.getHostContext().get(ENTITY)
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

    @MolangFunction(value = "is_riding_any_entity_of_type", description = "检查实体是否骑乘任意指定类型的实体")
    public static float isRidingAnyEntityOfType(MolangScope scope, String... types) {
        return scope.getHostContext().get(ENTITY).map(entity -> {
            Entity vehicle = entity.getVehicle();
            if (vehicle == null) return FALSE;
            //? if <26.1 {
            ResourceLocation vehicleKey = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
            //?} else {
            Identifier vehicleKey = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
            //?}
            for (String type : types) {
                if (ResourceLocationBridge.parseMc(type).equals(vehicleKey)) {
                    return TRUE;
                }
            }
            return FALSE;
        }).orElse(FALSE);
    }

    @MolangFunction(value = "equipped_item_any_tag", description = "检查指定槽位物品是否拥有任意指定的物品标签")
    public static float equippedItemAnyTag(MolangScope scope, String slot, String... tags) {
        return livingBool(scope, l -> {
            ItemStack item = getItemBySlot(l, slot, 0);
            for (String tag : tags) {
                var tagKey = net.minecraft.tags.TagKey.create(Registries.ITEM, ResourceLocationBridge.parseMc(tag));
                if (item.is(tagKey)) {
                    return true;
                }
            }
            return false;
        });
    }

    @MolangFunction(value = "get_equipped_item_name", description = "获取指定槽位的物品名称（已弃用，建议使用 is_item_name_any）")
    public static String getEquippedItemName(MolangScope scope) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(l -> BuiltInRegistries.ITEM.getKey(getItemBySlot(l, "0", 0).getItem()).toString())
                    .orElse("");
    }

    @MolangFunction(value = "relative_block_has_any_tag", description = "检查实体相对位置的方块是否拥有任意指定的方块标签")
    public static float relativeBlockHasAnyTag(MolangScope scope, float relX, float relY, float relZ, String... tags) {
        return entityBool(scope, entity -> {
            var level = entity.level();
            var pos = entity.blockPosition().offset((int) relX, (int) relY, (int) relZ);
            var blockState = level.getBlockState(pos);
            for (String tag : tags) {
                var tagKey = net.minecraft.tags.TagKey.create(Registries.BLOCK, ResourceLocationBridge.parseMc(tag));
                if (blockState.is(tagKey)) {
                    return true;
                }
            }
            return false;
        });
    }

    @MolangFunction(value = "armor_texture_slot", description = "盔甲纹理类型槽（Bedrock 专属，Java 无等效数据源，始终返回 0）")
    public static float armorTextureSlot(MolangScope scope, float slot) {
        return 0F;
    }

    @MolangFunction(value = "bone_orientation_trs", description = "骨骼 TRS 分解（Bedrock .geo 模型数据，Java 运行时无等效数据源，始终返回 0）")
    public static float boneOrientationTrs(MolangScope scope, String boneName) {
        return 0F;
    }

    @MolangFunction(value = "bone_origin", description = "骨骼初始枢轴点（Bedrock .geo 模型数据，Java 运行时无等效数据源，始终返回 0）")
    public static float boneOrigin(MolangScope scope, String boneName) {
        return 0F;
    }

    @MolangFunction(value = "get_root_locator_offset", description = "根模型定位器偏移（Bedrock .geo 模型数据，Java 运行时无等效数据源，始终返回 0）")
    public static float getRootLocatorOffset(MolangScope scope, String locator, String axis) {
        return 0F;
    }

    @MolangFunction(value = "graphics_mode_is_any", description = "图形模式检查（Bedrock 客户端特性，Java 无等效数据源，始终返回 1.0 代表 fancy）")
    public static float graphicsModeIsAny(MolangScope scope, String... modes) {
        return TRUE;
    }

    @MolangFunction(value = "is_pack_setting_enabled", description = "资源包设置开关（Bedrock 客户端特性，Java 无等效数据源，始终返回 0）")
    public static float isPackSettingEnabled(MolangScope scope, String settingName) {
        return FALSE;
    }

    @MolangFunction(value = "is_pack_setting_selected", description = "资源包设置下拉选择（Bedrock 客户端特性，Java 无等效数据源，始终返回 0）")
    public static float isPackSettingSelected(MolangScope scope, String settingName, String selection) {
        return FALSE;
    }

    @MolangFunction(value = "has_property", description = "实体属性存在检查（Bedrock 实体属性系统，Java 无等效数据源，始终返回 0）")
    public static float hasProperty(MolangScope scope, String propertyName) {
        return FALSE;
    }

    @MolangFunction(value = "property", description = "实体属性值（Bedrock 实体属性系统，Java 无等效数据源，始终返回 0）")
    public static float property(MolangScope scope, String propertyName) {
        return 0F;
    }

    @MolangFunction(value = "is_owner_identifier_any", description = "检查根实体标识符是否与任一指定字符串匹配")
    public static float isOwnerIdentifierAny(MolangScope scope, String... identifiers) {
        return scope.getHostContext().get(ENTITY).map(entity -> {
            //? if <26.1 {
            ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            //?} else {
            Identifier entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            //?}
            for (String id : identifiers) {
                if (ResourceLocationBridge.parseMc(id).equals(entityKey)) {
                    return TRUE;
                }
            }
            return FALSE;
        }).orElse(FALSE);
    }

    @MolangFunction(value = "variant", description = "变体")
    public static float variant(@Nullable MolangEntityContext ctx) {
        if (ctx == null) return 0f;
        ComponentStore store = ctx.componentStore();
        Number v = store.get("minecraft:variant");
        return v != null ? v.floatValue() : 0f;
    }

    @MolangFunction(value = "mark_variant", description = "标记变体")
    public static float markVariant(@Nullable MolangEntityContext ctx) {
        if (ctx == null) return 0f;
        ComponentStore store = ctx.componentStore();
        Number v = store.get("minecraft:mark_variant");
        return v != null ? v.floatValue() : 0f;
    }

    @MolangFunction(value = "scale", description = "行为包缩放")
    public static float scale(@Nullable MolangEntityContext ctx) {
        if (ctx == null) return 1f;
        ComponentStore store = ctx.componentStore();
        Number v = store.get("minecraft:scale");
        return v != null ? v.floatValue() : 1f;
    }

    // ==================== 2026-08-16 补齐：包数据实扫（113 实体 + 2560 动画 + 650 AC）中未实现的 query ====================
    // 语义来源：Microsoft 官方 Molang Query Functions 文档（bedrock.dev 镜像逐条核对）。
    // JE 无对应数据源的按官方 "else returns 0" 语义给中性值，description 逐一注明，不做静默近似。

    @MolangFunction(value = "blocking", description = "正在格挡（盾）")
    public static float blocking(MolangScope scope) {
        return livingBool(scope, LivingEntity::isBlocking);
    }

    @MolangFunction(value = "death_ticks", description = "死亡动画已进行的 tick 数")
    public static float deathTicks(MolangScope scope) {
        return livingFloat(scope, l -> (float) l.deathTime);
    }

    @MolangFunction(value = "hurt_direction", description = "受伤方向")
    public static float hurtDirection(MolangScope scope) {
        return livingFloat(scope, LivingEntity::getHurtDir);
    }

    @MolangFunction(value = "has_head_gear", description = "头部盔甲槽有物品")
    public static float hasHeadGear(MolangScope scope) {
        return livingBool(scope, l -> !l.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
    }

    @MolangFunction(value = "has_armor_slot", description = "指定盔甲槽有物品（0头1胸2腿3脚4身体）")
    public static float hasArmorSlot(MolangScope scope, float slot) {
        return livingBool(scope, l -> !armorStack(l, (int) slot).isEmpty());
    }

    @MolangFunction(value = "equipment_count", description = "已装备盔甲件数（不含手部；含动物身体槽/马铠）")
    public static float equipmentCount(MolangScope scope) {
        return livingFloat(scope, l -> {
            int count = 0;
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                if (!l.getItemBySlot(s).isEmpty()) {
                    count++;
                }
            }
            if (!bodyArmor(l).isEmpty()) {
                count++;
            }
            return (float) count;
        });
    }

    @MolangFunction(value = "armor_color_slot", description = "指定盔甲槽护甲染色（打包 RGB int 以 float 返回；未染色/不可染色 → -1）")
    public static float armorColorSlot(MolangScope scope, float slot) {
        return livingFloat(scope, l -> {
            ItemStack stack = armorStack(l, (int) slot);
            if (stack.isEmpty()) {
                return -1F;
            }
            //? if <1.20.6 {
            return stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable
                    ? (float) dyeable.getColor(stack) : -1F;
            //?} else {
            return (float) net.minecraft.world.item.component.DyedItemColor.getOrDefault(stack, -1);
            //?}
        });
    }

    @MolangFunction(value = "is_attached_to_entity", description = "被拴绳拴在实体/栅栏上（BE attach ≈ JE leash）")
    public static float isAttachedToEntity(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Mob mob && mob.isLeashed());
    }

    @MolangFunction(value = "is_admiring", description = "猪灵正在端详物品")
    public static float isAdmiring(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Piglin piglin
                && piglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM));
    }

    @MolangFunction(value = "is_casting", description = "灾厄施法者正在施法")
    public static float isCasting(MolangScope scope) {
        return entityBool(scope, e -> e instanceof SpellcasterIllager caster && caster.isCastingSpell());
    }

    @MolangFunction(value = "is_charged", description = "苦力怕已充能")
    public static float isCharged(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Creeper creeper && creeper.isPowered());
    }

    @MolangFunction(value = "is_croaking", description = "青蛙正在鸣叫")
    public static float isCroaking(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Frog frog && frog.croakAnimationState.isStarted());
    }

    @MolangFunction(value = "is_dancing", description = "悦灵正在跳舞")
    public static float isDancing(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Allay allay && allay.isDancing());
    }

    @MolangFunction(value = "is_in_contact_with_water", description = "接触水（含雨/气泡柱；喷溅水瓶 JE 无信号不计）")
    public static float isInContactWithWater(MolangScope scope) {
        //? if <26.1 {
        return entityBool(scope, e -> e.isInWaterOrRain() || e.isInWaterOrBubble());
        //?} else {
        return entityBool(scope, e -> e.isInWaterOrRain() || e.isInWater());
        //?}
    }

    @MolangFunction(value = "is_in_lava", description = "在岩浆中")
    public static float isInLava(MolangScope scope) {
        return entityBool(scope, Entity::isInLava);
    }

    @MolangFunction(value = "is_interested", description = "狼正对食物/玩家歪头（interested）")
    public static float isInterested(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Wolf wolf && wolf.isInterested());
    }

    @MolangFunction(value = "is_jump_goal_jumping", description = "跳跃目标跳跃中（JE 无 goal 区分，取生物跳跃标志，同 is_jumping）")
    public static float isJumpGoalJumping(MolangScope scope) {
        return livingBool(scope, EntityStatePort::isJumping);
    }

    @MolangFunction(value = "is_playing_dead", description = "美西螈正在装死")
    public static float isPlayingDead(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Axolotl axolotl && axolotl.isPlayingDead());
    }

    @MolangFunction(value = "is_resting", description = "蝙蝠正在倒吊休息")
    public static float isResting(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Bat bat && bat.isResting());
    }

    @MolangFunction(value = "is_searching", description = "嗅探兽正在搜寻")
    public static float isSearching(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Sniffer sniffer && sniffer.isSearching());
    }

    @MolangFunction(value = "is_shaking", description = "发抖（官方文档描述缺失；JE 近似：完全冰冻）")
    public static float isShaking(MolangScope scope) {
        return entityBool(scope, Entity::isFullyFrozen);
    }

    @MolangFunction(value = "is_shaking_wetness", description = "狼正在抖掉身上的水")
    public static float isShakingWetness(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Wolf wolf && EntityStatePort.wolfShakingWetness(wolf));
    }

    @MolangFunction(value = "is_spectator", description = "旁观模式")
    public static float isSpectator(MolangScope scope) {
        return entityBool(scope, Entity::isSpectator);
    }

    @MolangFunction(value = "is_using_item", description = "正在使用物品（拉弓/进食/举盾等）")
    public static float isUsingItem(MolangScope scope) {
        return livingBool(scope, LivingEntity::isUsingItem);
    }

    @MolangFunction(value = "item_in_use_duration", description = "当前物品已使用秒数")
    public static float itemInUseDuration(MolangScope scope) {
        return livingFloat(scope, l -> {
            if (!l.isUsingItem()) {
                return 0F;
            }
            ItemStack stack = l.getUseItem();
            //? if <1.20.6 {
            int max = stack.getUseDuration();
            //?} else {
            int max = stack.getUseDuration(l);
            //?}
            return (max - l.getUseItemRemainingTicks()) / 20F;
        });
    }

    @MolangFunction(value = "main_hand_item_max_duration", description = "主手物品使用时长的最大秒数")
    public static float mainHandItemMaxDuration(MolangScope scope) {
        return livingFloat(scope, l -> {
            ItemStack stack = l.getMainHandItem();
            if (stack.isEmpty()) {
                return 0F;
            }
            //? if <1.20.6 {
            return stack.getUseDuration() / 20F;
            //?} else {
            return stack.getUseDuration(l) / 20F;
            //?}
        });
    }

    @MolangFunction(value = "position_delta", description = "本 tick 位移分量（0=x,1=y,2=z）")
    public static float positionDelta(MolangScope scope, float axis) {
        return entityFloat(scope, e -> switch ((int) axis) {
            case 0 -> (float) (e.getX() - e.xo);
            case 1 -> (float) (e.getY() - e.yo);
            case 2 -> (float) (e.getZ() - e.zo);
            default -> 0F;
        });
    }

    @MolangFunction(value = "relative_block_has_all_tags", description = "实体相对位置的方块拥有全部给定标签")
    public static float relativeBlockHasAllTags(MolangScope scope, float relX, float relY, float relZ, String... tags) {
        return entityBool(scope, entity -> {
            var level = entity.level();
            var pos = entity.blockPosition().offset((int) relX, (int) relY, (int) relZ);
            var blockState = level.getBlockState(pos);
            for (String tag : tags) {
                var tagKey = net.minecraft.tags.TagKey.create(Registries.BLOCK, ResourceLocationBridge.parseMc(tag));
                if (!blockState.is(tagKey)) {
                    return false;
                }
            }
            return true;
        });
    }

    @MolangFunction(value = "ride_body_y_rotation", description = "坐骑的身体 yaw 旋转")
    public static float rideBodyYRotation(MolangScope scope) {
        return entityFloat(scope, e -> e.getVehicle() instanceof LivingEntity vehicle
                ? Mth.wrapDegrees(vehicle.yBodyRot) : 0F);
    }

    @MolangFunction(value = "sit_amount", description = "坐下程度（JE：熊猫坐姿插值；骆驼/驯养动物退化为 0/1）")
    public static float sitAmount(MolangScope scope) {
        return livingFloat(scope, l -> {
            if (l instanceof Panda panda) {
                return panda.getSitAmount(1.0F);
            }
            if (l instanceof Camel camel) {
                return camel.isCamelVisuallySitting() ? 1F : 0F;
            }
            if (l instanceof TamableAnimal tamable) {
                return tamable.isInSittingPose() ? 1F : 0F;
            }
            return 0F;
        });
    }

    @MolangFunction(value = "sneeze_counter", description = "熊猫喷嚏计数器")
    public static float sneezeCounter(MolangScope scope) {
        return livingFloat(scope, l -> l instanceof Panda panda ? (float) panda.getSneezeCounter() : 0F);
    }

    @MolangFunction(value = "standing_scale", description = "直立程度（马扬起前蹄动画量）")
    public static float standingScale(MolangScope scope) {
        return livingFloat(scope, l -> l instanceof AbstractHorse horse ? horse.getStandAnim(1.0F) : 0F);
    }

    @MolangFunction(value = "tail_angle", description = "尾巴角度（狼）")
    public static float tailAngle(MolangScope scope) {
        return livingFloat(scope, l -> l instanceof Wolf wolf ? wolf.getTailAngle() : 0F);
    }

    @MolangFunction(value = "texture_frame_index", description = "经验球图标索引")
    public static float textureFrameIndex(MolangScope scope) {
        return entityFloat(scope, e -> e instanceof ExperienceOrb orb ? (float) orb.getIcon() : 0F);
    }

    @MolangFunction(value = "walk_distance", description = "累计行走距离（JE walkDist：着地累计；潜行不剔除，与 BE 略有差异）")
    public static float walkDistance(MolangScope scope) {
        //? if <26.1 {
        return entityFloat(scope, e -> e.walkDist);
        //?} else {
        // 26.1：walkDist 迁到 AbstractClientPlayer.avatarState()（仅玩家、仅客户端），非玩家无等价信号 → 0
        return entityFloat(scope, e -> e instanceof net.minecraft.client.player.AbstractClientPlayer player
                ? player.avatarState().getInterpolatedWalkDistance(1.0F) : 0F);
        //?}
    }

    @MolangFunction(value = "time_stamp", description = "世界时间戳（JE 取 level gameTime，单位 tick）")
    public static float timeStamp(MolangScope scope) {
        return entityFloat(scope, e -> (float) e.level().getGameTime());
    }

    @MolangFunction(value = "has_dash_cooldown", description = "骆驼冲刺处于冷却")
    public static float hasDashCooldown(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Camel camel && EntityStatePort.camelHasDashCooldown(camel));
    }

    @MolangFunction(value = "has_player_rider", description = "有玩家骑乘（任意座位）")
    public static float hasPlayerRider(MolangScope scope) {
        return entityBool(scope, e -> e.getPassengers().stream().anyMatch(p -> p instanceof Player));
    }

    @MolangFunction(value = "body_x_rotation", description = "身体 pitch 旋转（JE 无独立身体 pitch，取实体 pitch 近似）")
    public static float bodyXPitch(MolangScope scope) {
        return entityFloat(scope, Entity::getXRot);
    }

    @MolangFunction(value = "model_scale", description = "实体缩放（<1.20.6 无缩放属性恒 1；>=1.20.6 取 generic.scale）")
    public static float modelScale(MolangScope scope) {
        //? if <1.20.6 {
        return 1F;
        //?} else {
        return livingFloat(scope, l -> (float) l.getAttributeValue(Attributes.SCALE));
        //?}
    }

    // ---------- JE 无对应数据源，按官方 "else 0/-1" 语义给中性值（非静默：description 均注明） ----------

    @MolangFunction(value = "is_avoiding_block", description = "正在逃离方块（JE 无该 goal 信号，恒 0）")
    public static float isAvoidingBlock(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "is_delayed_attacking", description = "延迟攻击中（BE 专用攻击 goal，JE 无信号，恒 0）")
    public static float isDelayedAttacking(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "is_eating_mob", description = "正在吞食生物（JE 无该 goal 信号，恒 0）")
    public static float isEatingMob(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "is_emoting", description = "正在做表情动作（JE 无表情系统，恒 0）")
    public static float isEmoting(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "is_ghost", description = "是幽灵实体（JE 1.20.1 无对应实体类型，恒 0）")
    public static float isGhost(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "is_shield_powered", description = "持激活的充能盾（JE 1.20.1 无充能盾，恒 0）")
    public static float isShieldPowered(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "is_stunned", description = "处于眩晕（JE 无眩晕状态，恒 0）")
    public static float isStunned(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "roll_counter", description = "翻滚计数（JE 无翻滚机制，恒 0）")
    public static float rollCounter(MolangScope scope) {
        return 0F;
    }

    @MolangFunction(value = "lie_amount", description = "趴下程度（JE 无连续趴下动画量，恒 0）")
    public static float lieAmount(MolangScope scope) {
        return 0F;
    }

    @MolangFunction(value = "structural_integrity", description = "结构完整度（BE 劫掠兽机制，JE 无对应，恒 0）")
    public static float structuralIntegrity(MolangScope scope) {
        return 0F;
    }

    @MolangFunction(value = "get_animation_frame", description = "物品图标动画帧（JE 物品模型谓词体系无帧索引，恒 0）")
    public static float getAnimationFrame(MolangScope scope) {
        return 0F;
    }

    @MolangFunction(value = "is_persona_or_premium_skin", description = "玩家使用 persona/付费皮肤（JE 无 persona 体系，恒 0）")
    public static float isPersonaOrPremiumSkin(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "timer_flag_2", description = "behavior.timer_flag_2 运行中（行为包计时器未同步到渲染侧，恒 0）")
    public static float timerFlag2(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "timer_flag_3", description = "behavior.timer_flag_3 运行中（行为包计时器未同步到渲染侧，恒 0）")
    public static float timerFlag3(MolangScope scope) {
        return FALSE;
    }

    @MolangFunction(value = "cooldown_time", description = "物品冷却总时长（JE 玩家冷却仅记录剩余比例、无槽位语义，恒 0）")
    public static float cooldownTime(MolangScope scope, String... slots) {
        return 0F;
    }

    @MolangFunction(value = "cooldown_time_remaining", description = "物品冷却剩余秒数（JE 无槽位冷却语义，恒 0）")
    public static float cooldownTimeRemaining(MolangScope scope, String... slots) {
        return 0F;
    }

    @MolangFunction(value = "life_span", description = "有限寿命实体的寿命（官方：永生实体返回 0；JE 实体普遍永生，恒 0）")
    public static float lifeSpan(MolangScope scope) {
        return 0F;
    }

    @MolangFunction(value = "ticks_since_last_kinetic_weapon_hit", description = "动能武器命中距今 tick 数（JE 1.20.1 无动能武器；官方语义：未使用 → -1）")
    public static float ticksSinceLastKineticWeaponHit(MolangScope scope) {
        return -1F;
    }

    /** BE 盔甲槽位 → JE 物品栈（0头1胸2腿3脚4身体）。 */
    private static ItemStack armorStack(LivingEntity living, int slot) {
        return switch (slot) {
            case 0 -> living.getItemBySlot(EquipmentSlot.HEAD);
            case 1 -> living.getItemBySlot(EquipmentSlot.CHEST);
            case 2 -> living.getItemBySlot(EquipmentSlot.LEGS);
            case 3 -> living.getItemBySlot(EquipmentSlot.FEET);
            default -> bodyArmor(living);
        };
    }

    /** 动物身体槽护甲（<1.20.6 马铠独立栏位；>=1.20.6 EquipmentSlot.BODY）。 */
    private static ItemStack bodyArmor(LivingEntity living) {
        //? if <1.20.6 {
        return living instanceof net.minecraft.world.entity.animal.horse.Horse horse
                ? horse.getArmor() : ItemStack.EMPTY;
        //?} else {
        return living.getItemBySlot(EquipmentSlot.BODY);
        //?}
    }

    @FunctionalInterface
    interface ToBooleanFunction<K> {
        boolean apply(K key);
    }

    private static float entityBool(MolangScope scope, ToBooleanFunction<Entity> function) {
        return scope.getHostContext().get(ENTITY).map(l -> function.apply(l) ? TRUE : FALSE).orElse(0F);
    }

    private static float entityFloat(MolangScope scope, Function<Entity, Float> function) {
        return scope.getHostContext().get(ENTITY).map(function).orElse(0F);
    }

    private static float livingBool(MolangScope scope, ToBooleanFunction<LivingEntity> function) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(l -> function.apply(l) ? TRUE : FALSE)
                    .orElse(0F);
    }

    private static float livingFloat(MolangScope scope, Function<LivingEntity, Float> function) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(function)
                    .orElse(0F);
    }

    private static float handIs(MolangScope scope, InteractionHand hand, String... objects) {
        return scope.getHostContext().get(LIVING_ENTITY).map(living -> {
            ItemStack handItem = hand == InteractionHand.MAIN_HAND
                    ? living.getMainHandItem()
                    : living.getOffhandItem();

            for (String object : objects) {
                if (Objects.equals(
                        BuiltInRegistries.ITEM.getKey(handItem.getItem()),
                        ResourceLocationBridge.parseMc(object))) {
                    return TRUE;
                }
            }

            return FALSE;
        }).orElse(FALSE);
    }

    private static float slotGetter(MolangScope scope, EquipmentSlot slot, String... objects) {
        return scope.getHostContext().get(LIVING_ENTITY).map(livingEntity -> {
            for (String object : objects) {
                if (ResourceLocationBridge.parseMc(object)
                                     .equals(BuiltInRegistries.ITEM.getKey(livingEntity.getItemBySlot(slot)
                                                                                       .getItem()))) {
                    return TRUE;
                }
            }

            return FALSE;
        }).orElse(FALSE);
    }

    private static boolean portBool(MolangScope scope, String key) {
        return scope.getHostContext().get(HostRoles.PORT_ENTITY)
                    .map(pe -> {
                        Object val = pe.getQueryProperties().get(key);
                        return val instanceof Boolean b && b;
                    })
                    .orElse(false);
    }

    private static Optional<Float> portFloat(MolangScope scope, String key) {
        return scope.getHostContext().get(HostRoles.PORT_ENTITY)
                    .map(pe -> pe.getQueryProperties().get(key))
                    .filter(val -> val instanceof Number)
                    .map(val -> ((Number) val).floatValue());
    }
}


