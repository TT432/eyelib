package io.github.tt432.eyelib.bridge.client.manager;

import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
//? if <1.20.6 {
import net.minecraftforge.common.MinecraftForge;
//?} else {
import net.neoforged.neoforge.common.NeoForge;
//?}
/**
 * @author TT432
 */
public final class ForgeManagerEventPublisher implements ManagerEventPublisher {
    @Override
    public void publishManagerEntryChanged(String managerName, String entryName, Object entryData) {
        //? if <1.20.6 {
        MinecraftForge.EVENT_BUS.post(new ManagerEntryChangedEvent(managerName, entryName, entryData));
        //?} else {
        NeoForge.EVENT_BUS.post(new ManagerEntryChangedEvent(managerName, entryName, entryData));
        //?}
    }
}
