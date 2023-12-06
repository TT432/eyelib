package io.github.tt432.eyelib.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.neoforged.bus.api.Event;

/**
 * @author TT432
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InitComponentEvent extends Event {
    public final Object entity;
    public final Object componentObject;
}
