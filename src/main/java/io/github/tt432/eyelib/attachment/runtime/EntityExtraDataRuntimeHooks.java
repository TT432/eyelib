package io.github.tt432.eyelibattachment.runtime;

import io.github.tt432.eyelibattachment.capability.ExtraEntityData;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibattachment.network.DataAttachmentSyncRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 额外实体数据运行时的 Forge 事件钩子。
 *
 * @author TT432
 */
@Mod.EventBusSubscriber
public final class EntityExtraDataRuntimeHooks {
    private EntityExtraDataRuntimeHooks() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var attachment = DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get();
            var data = DataAttachmentHelper.getOrCreate(attachment, event.getTarget());
            DataAttachmentSyncRuntime.syncToPlayer(attachment, event.getTarget(), data, player);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        var attachment = DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get();
        ExtraEntityData data = DataAttachmentHelper.getOrCreate(attachment, mob);
        ExtraEntityData updated = ExtraEntityDataUpdater.update(data, observeGoalFlags(mob));

        DataAttachmentHelper.setLocal(attachment, mob, updated);
        if (data != updated) {
            DataAttachmentSyncRuntime.syncTrackedAndSelf(attachment, mob, updated);
        }
    }

    private static ExtraEntityDataUpdater.ObservedGoalFlags observeGoalFlags(Mob mob) {
        boolean facingTargetToRangeAttack = false;
        boolean isAvoidingMobs = false;
        boolean isAvoid = false;
        boolean isGrazing = false;

        for (WrappedGoal availableGoal : mob.goalSelector.getAvailableGoals()) {
            if (availableGoal.isRunning()
                    && ((availableGoal.getGoal() instanceof RangedAttackGoal rangedAttackGoal
                    && rangedAttackGoal.attackTime > 0)
                    || (availableGoal.getGoal() instanceof RangedBowAttackGoal && mob.getTicksUsingItem() < 19))) {
                facingTargetToRangeAttack = true;
            }
            if (availableGoal.getGoal() instanceof AvoidEntityGoal<?> avoid) {
                if (avoid.toAvoid instanceof Mob) {
                    isAvoidingMobs = true;
                }
                if (avoid.toAvoid != null) {
                    isAvoid = true;
                }
            }
            if (availableGoal.getGoal() instanceof EatBlockGoal && availableGoal.isRunning()) {
                isGrazing = true;
            }
        }

        return new ExtraEntityDataUpdater.ObservedGoalFlags(
                facingTargetToRangeAttack,
                isAvoidingMobs,
                isGrazing,
                isAvoid
        );
    }
}