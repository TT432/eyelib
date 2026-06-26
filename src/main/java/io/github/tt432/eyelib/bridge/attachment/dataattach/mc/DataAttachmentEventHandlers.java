package io.github.tt432.eyelib.bridge.attachment.dataattach.mc;

import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentSyncRuntime;
//? if <1.20.6 {
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//?}

/**
 * 数据附属相关的事件处理器。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = "eyelib")
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
public class DataAttachmentEventHandlers {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        var entity = event.getTarget();
        if (entity.level().isClientSide()) {
            return;
        }

        //? if <1.20.6 {
        var container = entity.getCapability(DataAttachmentContainerCapability.INSTANCE);
        container.ifPresent(c -> {
        //?} else {
        entity.getExistingData(DataAttachmentContainerCapability.ATTACHMENT).ifPresent(c -> {
        //?}
            if (c instanceof McDataAttachmentContainer mcContainer) {
                //? if <1.20.6 {
                DataAttachmentSyncRuntime.syncContainer(entity, mcContainer.serializeNBT());
                //?} else {
                DataAttachmentSyncRuntime.syncContainer(entity, mcContainer.serializeNBT(entity.level().registryAccess()));
                //?}
            }
        });
    }
}
