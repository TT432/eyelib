package io.github.tt432.eyelib.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.neoforged.bus.api.Event;

/**
 * @author TT432
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ManagerEntryChangedEvent extends Event {
    private final String managerName;
    private final String entryName;
    private final Object entryData;
}
