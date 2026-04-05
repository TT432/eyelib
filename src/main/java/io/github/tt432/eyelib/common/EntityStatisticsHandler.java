package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.capability.EntityStatistics;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.dataattach.DataAttachmentSyncService;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
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
        EntityStatistics data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.ENTITY_STATISTICS.get(), entity);
        EntityStatistics updated = data.withDistanceWalked(data.distanceWalked() + (float) Math.sqrt(x * x + z * z));
        DataAttachmentHelper.setLocal(EyelibAttachableData.ENTITY_STATISTICS.get(), entity, updated);
        if (!entity.level().isClientSide()) {
            DataAttachmentSyncService.syncTrackedAndSelf(EyelibAttachableData.ENTITY_STATISTICS.get(), entity, updated);
        }
    }
}
