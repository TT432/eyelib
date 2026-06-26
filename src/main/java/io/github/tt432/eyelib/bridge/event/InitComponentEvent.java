package io.github.tt432.eyelib.bridge.event;

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
@EqualsAndHashCode(callSuper = true)
@Data
public class InitComponentEvent extends Event {
    public final Object entity;
    public final Object componentObject;
}
