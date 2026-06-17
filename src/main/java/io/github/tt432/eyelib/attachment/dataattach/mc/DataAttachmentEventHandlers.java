package io.github.tt432.eyelib.attachment.dataattach.mc;

import io.github.tt432.eyelib.attachment.network.DataAttachmentSyncRuntime;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 数据附属相关的事件处理器。
 *
 * @author TT432
 */
@Mod.EventBusSubscriber(modid = "eyelibattachment")
public class DataAttachmentEventHandlers {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        var entity = event.getTarget();
        if (entity.level().isClientSide()) {
            return;
        }

        var container = entity.getCapability(DataAttachmentContainerCapability.INSTANCE);
        container.ifPresent(c -> {
            if (c instanceof McDataAttachmentContainer mcContainer) {
                DataAttachmentSyncRuntime.syncContainer(entity, mcContainer.serializeNBT());
            }
        });
    }
}