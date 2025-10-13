package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import io.github.tt432.eyelib.network.UniDataUpdatePacket;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
public class EntityExtraDataHandler {

    @SubscribeEvent
    public static void onEvent(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            EyelibNetworkManager.sendToPlayer(sp,
                    UniDataUpdatePacket.crate(event.getTarget(), EyelibAttachableData.EXTRA_ENTITY_DATA));
        }
    }

    @SubscribeEvent
    public static void onEvent(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) {
            if (event.getEntity() instanceof Mob r) {
                ExtraEntityData data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), event.getEntity());
                ExtraEntityData oldData = data;
                var facing_target_to_range_attack = false;
                var is_avoiding_mobs = false;
                var is_avoid = false;
                var is_grazing = false;

                for (WrappedGoal availableGoal : r.goalSelector.getAvailableGoals()) {
                    if (availableGoal.isRunning() &&
                            ((availableGoal.getGoal() instanceof RangedAttackGoal rag && rag.attackTime > 0)
                                    || (availableGoal.getGoal() instanceof RangedBowAttackGoal && r.getTicksUsingItem() < 19))
                    ) {
                        facing_target_to_range_attack = true;
                    }
                    if (availableGoal.getGoal() instanceof AvoidEntityGoal<?> avoid) {
                        if (avoid.toAvoid instanceof Mob)
                            is_avoiding_mobs = true;
                        if (avoid.toAvoid != null)
                            is_avoid = true;
                    }
                    if (availableGoal.getGoal() instanceof EatBlockGoal && availableGoal.isRunning()) {
                        is_grazing = true;
                    }
                }

                if (data.facing_target_to_range_attack() != facing_target_to_range_attack)
                    data = data.withFacing_target_to_range_attack(facing_target_to_range_attack);
                if (data.is_avoiding_mobs() != is_avoiding_mobs)
                    data = data.with_avoiding_mobs(is_avoiding_mobs);
                if (data.is_grazing() != is_grazing)
                    data = data.with_grazing(is_grazing);
                if (data.is_avoid() != is_avoid)
                    data = data.with_avoid(is_avoid);

                DataAttachmentHelper.set(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), event.getEntity(), data);

                if (oldData != data)
                    EyelibNetworkManager.sendToTrackedAndSelf(r,
                            UniDataUpdatePacket.crate(r, EyelibAttachableData.EXTRA_ENTITY_DATA));
            }
        }
    }
}
