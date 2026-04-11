package io.github.tt432.eyelib.mc.impl.data_attach;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.mc.impl.network.dataattach.DataAttachmentSyncRuntime;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID)
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
