package io.github.tt432.eyelib.common;

import io.github.tt432.eyelibattachment.capability.EntityStatistics;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibattachment.network.DataAttachmentSyncRuntime;
import io.github.tt432.eyelibattachment.runtime.EntityStatisticsUpdater;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
public class EntityStatisticsHandler {
    @SubscribeEvent
    public static void onEvent(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        Vec3 pos = entity.position();
        var x = pos.x - entity.xo;
        var z = pos.z - entity.zo;
        EntityStatistics data = DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), entity);
        EntityStatistics updated = EntityStatisticsUpdater.updateDistanceWalked(data, x, z);
        DataAttachmentHelper.setLocal(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), entity, updated);
        if (!entity.level().isClientSide()) {
            DataAttachmentSyncRuntime.syncTrackedAndSelf(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), entity, updated);
        }
    }
}
