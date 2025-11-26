package io.github.tt432.eyelib.util.data_attach;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.network.DataAttachmentSyncPacket;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
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
            var data = c.serializeNBT();
            EyelibNetworkManager.sendToTrackedAndSelf(entity, new DataAttachmentSyncPacket(entity.getId(), data));
        });
    }
}
