package io.github.tt432.eyelib.bridge.attachment.runtime;

import io.github.tt432.eyelib.util.entitydata.ExtraEntityDataUpdater;
import io.github.tt432.eyelib.util.entitydata.ExtraEntityData;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentSyncRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
//? if <1.20.6 {
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//?}

/**
 * 额外实体数据运行时的 Forge 事件钩子。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
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
    //? if <1.20.6 {
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    //?} else {
    public static void onLivingTick(EntityTickEvent.Pre event) {
    //?}
        //? if <26.1 {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob)) {
        //?} else {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
        //?}
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
            if (availableGoal.isRunning()) {
                boolean rangedAttackGoalActive = false;
                if (availableGoal.getGoal() instanceof RangedAttackGoal rangedAttackGoal) {
                    //? if <1.20.6 {
                    rangedAttackGoalActive = rangedAttackGoal.attackTime > 0;
                    //?} elif <26.1 {
                    rangedAttackGoalActive = ((io.github.tt432.eyelib.mixin.RangedAttackGoalAccessor) rangedAttackGoal).eyelib$getAttackTime() > 0;
                    //?} else {
                    rangedAttackGoalActive = false;
                    //?}
                }
                if (rangedAttackGoalActive
                        || (availableGoal.getGoal() instanceof RangedBowAttackGoal && mob.getTicksUsingItem() < 19)) {
                    facingTargetToRangeAttack = true;
                }
            }
            if (availableGoal.getGoal() instanceof AvoidEntityGoal<?> avoid) {
                //? if <26.1 {
                //? if <1.20.6 {
                var toAvoid = avoid.toAvoid;
                //?} else {
                var toAvoid = ((io.github.tt432.eyelib.mixin.AvoidEntityGoalAccessor) avoid).eyelib$getToAvoid();
                //?}
                //?} else {
                var toAvoid = (Mob) null;
                //?}
                if (toAvoid instanceof Mob) {
                    isAvoidingMobs = true;
                }
                if (toAvoid != null) {
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
