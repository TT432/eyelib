package io.github.tt432.eyelib.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraftforge.eventbus.api.Event;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NullMarked
public class ManagerEntryChangedEvent extends Event {
    private final String managerName;
    private final String entryName;
    private final Object entryData;
}