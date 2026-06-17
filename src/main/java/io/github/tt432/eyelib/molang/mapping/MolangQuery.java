package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.behavior.component.MarkVariant;
import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
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
    @MolangFunction(value = "anim_time", alias = "life_time", description = "动画播放秒数")
    public static float animTime(MolangScope scope) {
        return scope.getHostContext().get(BrAnimationEntry.Data.class).map(BrAnimationEntry.Data::animTime)
                    .orElseGet(() -> scope.getHostContext().get(BrAnimationController.Data.class)
                                          .map(BrAnimationController.Data::animTime).orElse(0F));
    }

    @MolangFunction(value = "delta_time", description = "距离上一帧的秒数")
    public static float deltaTime(MolangScope scope) {
        return scope.getHostContext().get(BrAnimationEntry.Data.class).map(BrAnimationEntry.Data::deltaTime).orElse(0F);
    }

    @MolangFunction(value = "any_animation_finished", description = "任意动画播放完毕（动画控制器）")
    public static float anyAnimationFinished(MolangScope scope) {
        return scope.getHostContext().get(BrAnimationController.class)
                    .flatMap(c -> scope.getHostContext().get(BrAnimationController.Data.class)
                                       .map(c::anyAnimationFinished)
                    ).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "all_animations_finished", description = "所有动画播放完毕（动画控制器）")
    public static float allAnimationsFinished(MolangScope scope) {
        return scope.getHostContext().get(BrAnimationController.class)
                    .flatMap(c -> scope.getHostContext().get(BrAnimationController.Data.class)
                                       .map(c::allAnimationFinished)
                    ).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "variant", description = "变体")
    public static float variant(MolangScope scope) {
        return livingFloat(scope, l -> {
            SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                    EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(), l);
            if (synced != null) {
                return (float) synced.variant();
            }
            Variant component = DataAttachmentHelper.getOrCreate(EyelibAttachableData.ENTITY_BEHAVIOR_DATA.get(), l)
                                                    .component(Variant.class);
            return component != null ? (float) component.value() : 0;
        });
    }

    @MolangFunction(value = "mark_variant", description = "变体")
    public static float markVariant(MolangScope scope) {
        return livingFloat(scope, l -> {
            SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                    EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(), l);
            if (synced != null) {
                return (float) synced.markVariant();
            }
            MarkVariant component = DataAttachmentHelper.getOrCreate(EyelibAttachableData.ENTITY_BEHAVIOR_DATA.get(), l)
                                                        .component(MarkVariant.class);
            return component != null ? (float) component.value() : 0;
        });
    }

    @MolangFunction(value = "damage_x", description = "受伤来源方向 x")
    public static float damageX(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), living)
                                            .lastHurtX());
    }

    @MolangFunction(value = "damage_y", description = "受伤来源方向 x")
    public static float damageY(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), living)
                                            .lastHurtY());
    }

    @MolangFunction(value = "damage_z", description = "受伤来源方向 x")
    public static float damageZ(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), living)
                                            .lastHurtZ());
    }

    @MolangFunction(value = "facing_target_to_range_attack", description = "正在进行远程攻击")
    public static float facingTargetToRageAttack(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .facing_target_to_range_attack());
    }

    @MolangFunction(value = "is_avoiding_mobs", description = "正在从怪物逃离")
    public static float isAvoidingMobs(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_avoiding_mobs());
    }

    @MolangFunction(value = "is_grazing", description = "正在吃草")
    public static float isGrazing(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_grazing());
    }

    @MolangFunction(value = "modified_distance_moved", description = "移动过的距离")
    public static float modifiedDistanceMoved(MolangScope scope) {
        return scope.getHostContext()
                    .get(Entity.class)
                    .map(e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), e)
                                                  .distanceWalked())
                    .orElse(0F);
    }

    @MolangFunction(value = "is_digging", description = "正在挖掘（玩家）")
    public static float isDigging(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_dig());
    }

    @MolangFunction(value = "is_avoid", description = "正在逃离(比如苦力怕逃离猫)")
    public static float isAvoid(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_avoid());
    }

    @MolangFunction(value = "is_jumping", description = "正在跳跃")
    public static float isJumping(MolangScope scope) {
        return livingBool(scope, entity -> entity.jumping);
    }

    @MolangFunction(value = "creeper_swell", alias = "swell_amount", description = "苦力怕爆炸计时")
    public static float creeperSwell(MolangScope scope) {
        return scope.getHostContext().get(Creeper.class).map(c -> c.swell / 30F)
                    .orElse(Float.valueOf(scope.getHostContext()
                                               .get(WitherBoss.class)
                                               .map(WitherBoss::getInvulnerableTicks)
                                               .orElse(0)));
    }

    private static float livingBool(MolangScope scope, ToBooleanFunction<LivingEntity> function) {
        return scope.getHostContext().get(LivingEntity.class)
                    .map(l -> function.apply(l) ? TRUE : FALSE)
                    .orElse(0F);
    }

    private static float entityBool(MolangScope scope, ToBooleanFunction<Entity> function) {
        return scope.getHostContext().get(Entity.class).map(l -> function.apply(l) ? TRUE : FALSE).orElse(0F);
    }

    private static float livingFloat(MolangScope scope, Function<LivingEntity, Float> function) {
        return scope.getHostContext().get(LivingEntity.class)
                    .map(function)
                    .orElse(0F);
    }

    @MolangFunction(value = "equipped_item_is_attachable", description = "手持物品为 attachable")
    public static float equippedItemIsAttachable(MolangScope scope) {
        return scope.getHostContext().get(LivingEntity.class)
                    .map(entity -> {
                        if (AttachableResolver.resolve(entity, entity.getMainHandItem()) != null
                                || AttachableResolver.resolve(entity, entity.getOffhandItem()) != null) {
                            return TRUE;
                        }
                        return FALSE;
                    }).orElse(FALSE);
    }

    @FunctionalInterface
    interface ToBooleanFunction<K> {
        boolean apply(K key);
    }
}
