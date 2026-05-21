package io.github.tt432.eyelib.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraftforge.eventbus.api.Event;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NullMarked
/** @author TT432 */
public class InitComponentEvent extends Event {
    public final Object entity;
    public final Object componentObject;
}