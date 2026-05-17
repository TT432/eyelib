package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelibutil.manager.ManagerEventPublisher;
import net.minecraftforge.common.MinecraftForge;

public final class ForgeManagerEventPublisher implements ManagerEventPublisher {
    @Override
    public void publishManagerEntryChanged(String managerName, String entryName, Object entryData) {
        MinecraftForge.EVENT_BUS.post(new ManagerEntryChangedEvent(managerName, entryName, entryData));
    }
}
