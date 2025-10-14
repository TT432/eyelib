package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.capability.EntityStatistics;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * @author TT432
 */
@EventBusSubscriber
public class EntityStatisticsHandler {
    @SubscribeEvent
    public static void onEvent(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Vec3 pos = entity.position();
        var x = pos.x - entity.xo;
        var z = pos.z - entity.zo;
        EntityStatistics data = entity.getData(EyelibAttachableData.ENTITY_STATISTICS);
        entity.setData(EyelibAttachableData.ENTITY_STATISTICS,
                data.withPreDistanceWalked(data.distanceWalked())
                        .withDistanceWalked(data.distanceWalked() + (float) Math.sqrt(x * x + z * z)));
    }
}
