package io.github.tt432.eyelib.bridge.event.adapter;

import io.github.tt432.eyelib.bridge.event.ManagerEventPort;
import lombok.Data;
import lombok.EqualsAndHashCode;
//? if <1.20.6 {
import net.minecraftforge.eventbus.api.Event;
//?} else {
import net.neoforged.bus.api.Event;
//?}
/**
 * @author TT432
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ManagerEntryChangedEvent extends Event implements ManagerEventPort {
    private final String managerName;
    private final String entryName;
    private final Object entryData;
}

