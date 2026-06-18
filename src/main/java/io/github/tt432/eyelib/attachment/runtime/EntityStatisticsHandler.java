package io.github.tt432.eyelib.attachment.runtime;

import io.github.tt432.eyelib.attachment.capability.EntityStatistics;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.attachment.network.DataAttachmentSyncRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
//? if <1.20.6 {
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}

/**
 * 实体统计数据的事件处理器。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber
//?}
public class EntityStatisticsHandler {
    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(LivingEvent.LivingTickEvent event) {
    //?} else {
    public static void onEvent(EntityTickEvent event) {
    //?}
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
